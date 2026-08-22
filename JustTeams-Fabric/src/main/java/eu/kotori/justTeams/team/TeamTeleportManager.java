package eu.kotori.justTeams.team;

import eu.kotori.justTeams.JustTeamsFabric;
import eu.kotori.justTeams.permission.JustTeamsPermissions;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/** Centralized Fabric implementation of team-home and team-warp teleport mechanics. */
public final class TeamTeleportManager {
    private final Map<UUID, Instant> homeCooldowns = new HashMap<>();
    private final Map<UUID, Instant> warpCooldowns = new HashMap<>();
    private final Map<UUID, Warmup> warmups = new HashMap<>();
    private long tickCounter;

    public TeamTeleportManager() {
        ServerTickEvents.END_SERVER_TICK.register(this::tick);
    }

    public boolean requestHomeTeleport(ServerPlayerEntity player, TeamLocation location) {
        if (isOnCooldown(player, true)) return false;
        return beginWarmup(player, location, true);
    }

    public boolean requestWarpTeleport(ServerPlayerEntity player, TeamLocation location) {
        if (isOnCooldown(player, false)) return false;
        return beginWarmup(player, location, false);
    }

    public void handleDisconnect(UUID playerUuid) {
        warmups.remove(playerUuid);
    }

    public void clear() {
        homeCooldowns.clear();
        warpCooldowns.clear();
        warmups.clear();
    }

    private boolean beginWarmup(ServerPlayerEntity player, TeamLocation destination, boolean home) {
        UUID uuid = player.getUuid();
        if (warmups.containsKey(uuid)) {
            player.sendMessage(Text.literal("You already have a teleportation in progress."), true);
            return false;
        }

        int seconds = home
                ? JustTeamsFabric.config().getHomeWarmupSeconds()
                : JustTeamsFabric.config().getWarpWarmupSeconds();
        int ticks = Math.max(0, seconds * 20);

        if (ticks <= 0) {
            return completeTeleport(player, destination, home);
        }

        Warmup warmup = new Warmup(
                uuid,
                destination,
                home,
                ticks,
                player.getEntityWorld().getRegistryKey(),
                player.getX(),
                player.getY(),
                player.getZ());
        warmups.put(uuid, warmup);
        sendWarmupUpdate(player, warmup);
        return true;
    }

    private void tick(MinecraftServer server) {
        tickCounter++;
        if (tickCounter % 20 != 0) return;

        cleanupExpiredCooldowns();

        Iterator<Map.Entry<UUID, Warmup>> iterator = warmups.entrySet().iterator();
        while (iterator.hasNext()) {
            Warmup warmup = iterator.next().getValue();
            ServerPlayerEntity player = server.getPlayerManager().getPlayer(warmup.playerUuid());
            if (player == null || !player.isAlive()) {
                iterator.remove();
                continue;
            }

            if (!isStillAtStart(player, warmup)) {
                cancelWarmup(player);
                iterator.remove();
                continue;
            }

            warmup.decrementSecond();
            if (warmup.remainingTicks() <= 0) {
                iterator.remove();
                completeTeleport(player, warmup.destination(), warmup.home());
            } else {
                sendWarmupUpdate(player, warmup);
            }
        }
    }

    private boolean isStillAtStart(ServerPlayerEntity player, Warmup warmup) {
        if (!player.isAlive()) return false;
        if (!player.getEntityWorld().getRegistryKey().equals(warmup.startWorld())) return false;
        return player.squaredDistanceTo(warmup.startX(), warmup.startY(), warmup.startZ()) <= 1.0D;
    }

    private void sendWarmupUpdate(ServerPlayerEntity player, Warmup warmup) {
        long seconds = Math.max(1L, (warmup.remainingTicks() + 19L) / 20L);
        player.sendMessage(
                Text.literal("Teleporting in " + seconds + " seconds... Don't move!"),
                true);

        if (!JustTeamsFabric.config().areParticlesEnabled()) return;
        ServerWorld world = player.getEntityWorld();
        world.spawnParticles(
                player,
                resolveParticle(JustTeamsFabric.config().getWarmupParticle()),
                false,
                player.getX(),
                player.getY() + 1.0D,
                player.getZ(),
                10,
                0.5D,
                0.5D,
                0.5D,
                0.0D);
    }

    private boolean completeTeleport(ServerPlayerEntity player, TeamLocation location, boolean home) {
        Identifier identifier = Identifier.tryParse(location.getDimension());
        if (identifier == null) {
            player.sendMessage(Text.literal(
                    home ? "The saved home has an invalid dimension." : "The saved warp has an invalid dimension."), true);
            playError(player);
            return false;
        }

        RegistryKey<World> key = RegistryKey.of(RegistryKeys.WORLD, identifier);
        MinecraftServer server = player.getEntityWorld().getServer();
        ServerWorld destinationWorld = server.getWorld(key);
        if (destinationWorld == null) {
            player.sendMessage(Text.literal(
                    home ? "The saved home's dimension is not available." : "The saved warp's dimension is not available."), true);
            playError(player);
            return false;
        }

        player.teleport(
                destinationWorld,
                location.getX(),
                location.getY(),
                location.getZ(),
                java.util.Set.of(),
                location.getYaw(),
                location.getPitch(),
                true);

        player.sendMessage(Text.literal(
                home
                        ? "You have been successfully teleported to your team home."
                        : "You have been successfully teleported to your team warp."), true);
        playTeleport(player);

        if (JustTeamsFabric.config().areParticlesEnabled()) {
            destinationWorld.spawnParticles(
                    player,
                    resolveParticle(JustTeamsFabric.config().getSuccessParticle()),
                    false,
                    player.getX(),
                    player.getY(),
                    player.getZ(),
                    30,
                    0.5D,
                    0.5D,
                    0.5D,
                    0.0D);
        }

        setCooldown(player, home);
        return true;
    }

    private boolean isOnCooldown(ServerPlayerEntity player, boolean home) {
        String permission = home
                ? JustTeamsPermissions.BYPASS_HOME_COOLDOWN
                : JustTeamsPermissions.BYPASS_WARP_COOLDOWN;
        if (JustTeamsFabric.permissions().has(player, permission)) return false;

        Map<UUID, Instant> cooldowns = home ? homeCooldowns : warpCooldowns;
        Instant end = cooldowns.get(player.getUuid());
        if (end == null) return false;

        Instant now = Instant.now();
        if (!now.isBefore(end)) {
            cooldowns.remove(player.getUuid());
            return false;
        }

        long seconds = Math.max(0L, Duration.between(now, end).toSeconds());
        if (home) {
            player.sendMessage(Text.literal("Teleport cooldown: " + seconds + "s remaining."), true);
        } else {
            player.sendMessage(Text.literal("Warp cooldown: " + seconds + "s remaining."), true);
        }
        return true;
    }

    private void setCooldown(ServerPlayerEntity player, boolean home) {
        String permission = home
                ? JustTeamsPermissions.BYPASS_HOME_COOLDOWN
                : JustTeamsPermissions.BYPASS_WARP_COOLDOWN;
        if (JustTeamsFabric.permissions().has(player, permission)) return;

        int seconds = home
                ? JustTeamsFabric.config().getHomeCooldownSeconds()
                : JustTeamsFabric.config().getWarpCooldownSeconds();
        if (seconds <= 0) return;

        Map<UUID, Instant> cooldowns = home ? homeCooldowns : warpCooldowns;
        cooldowns.put(player.getUuid(), Instant.now().plusSeconds(seconds));
    }

    private void cleanupExpiredCooldowns() {
        Instant now = Instant.now();
        homeCooldowns.entrySet().removeIf(entry -> !now.isBefore(entry.getValue()));
        warpCooldowns.entrySet().removeIf(entry -> !now.isBefore(entry.getValue()));
    }

    private void cancelWarmup(ServerPlayerEntity player) {
        player.sendMessage(Text.literal("Teleportation canceled because you moved."), true);
        playError(player);
    }

    private void playTeleport(ServerPlayerEntity player) {
        if (!JustTeamsFabric.config().areSoundsEnabled()) return;
        player.playSound(resolveSound(JustTeamsFabric.config().getTeleportSound()), 1.0F, 1.0F);
    }

    private void playError(ServerPlayerEntity player) {
        if (!JustTeamsFabric.config().areSoundsEnabled()) return;
        player.playSound(resolveSound(JustTeamsFabric.config().getErrorSound()), 1.0F, 1.0F);
    }

    private static SoundEvent resolveSound(String value) {
        if (value == null) return SoundEvents.BLOCK_NOTE_BLOCK_BASS;
        return switch (value.trim().toUpperCase()) {
            case "BLOCK_BEACON_ACTIVATE" -> SoundEvents.BLOCK_BEACON_ACTIVATE;
            case "BLOCK_NOTE_BLOCK_BASS" -> SoundEvents.BLOCK_NOTE_BLOCK_BASS;
            default -> SoundEvents.BLOCK_NOTE_BLOCK_BASS;
        };
    }

    private static net.minecraft.particle.SimpleParticleType resolveParticle(String value) {
        if (value == null) return ParticleTypes.PORTAL;
        return switch (value.trim().toUpperCase()) {
            case "END_ROD" -> ParticleTypes.END_ROD;
            case "PORTAL" -> ParticleTypes.PORTAL;
            default -> ParticleTypes.PORTAL;
        };
    }

    private static final class Warmup {
        private final UUID playerUuid;
        private final TeamLocation destination;
        private final boolean home;
        private int remainingTicks;
        private final RegistryKey<World> startWorld;
        private final double startX;
        private final double startY;
        private final double startZ;

        private Warmup(UUID playerUuid, TeamLocation destination, boolean home, int remainingTicks,
                       RegistryKey<World> startWorld, double startX, double startY, double startZ) {
            this.playerUuid = playerUuid;
            this.destination = destination;
            this.home = home;
            this.remainingTicks = remainingTicks;
            this.startWorld = startWorld;
            this.startX = startX;
            this.startY = startY;
            this.startZ = startZ;
        }

        private void decrementSecond() {
            remainingTicks -= 20;
        }

        private UUID playerUuid() { return playerUuid; }
        private TeamLocation destination() { return destination; }
        private boolean home() { return home; }
        private int remainingTicks() { return remainingTicks; }
        private RegistryKey<World> startWorld() { return startWorld; }
        private double startX() { return startX; }
        private double startY() { return startY; }
        private double startZ() { return startZ; }
    }
}
