package eu.kotori.justTeams.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import eu.kotori.justTeams.JustTeamsFabric;
import eu.kotori.justTeams.chat.TeamChatManager;
import eu.kotori.justTeams.gui.JoinRequestGui;
import eu.kotori.justTeams.gui.TeamGuiManager;
import eu.kotori.justTeams.permission.JustTeamsPermissions;
import eu.kotori.justTeams.team.Team;
import eu.kotori.justTeams.team.TeamLocation;
import eu.kotori.justTeams.team.TeamPlayer;
import eu.kotori.justTeams.team.TeamRole;
import eu.kotori.justTeams.team.TeamWarp;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;

import java.time.Instant;
import java.util.Set;

public final class TeamCommand {
    private TeamCommand() {}

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("team")
                .executes(c -> run(c.getSource(), () -> openGui(c.getSource())))
                .then(CommandManager.literal("create")
                        .then(CommandManager.argument("name", StringArgumentType.word())
                                .then(CommandManager.argument("tag", StringArgumentType.word())
                                        .executes(c -> run(c.getSource(), () -> create(c.getSource(),
                                                StringArgumentType.getString(c, "name"),
                                                StringArgumentType.getString(c, "tag")))))))
                .then(CommandManager.literal("gui")
                        .executes(c -> run(c.getSource(), () -> openGui(c.getSource()))))
                .then(CommandManager.literal("info")
                        .executes(c -> run(c.getSource(), () -> info(c.getSource()))))
                .then(CommandManager.literal("leave")
                        .executes(c -> run(c.getSource(), () -> leave(c.getSource()))))
                .then(CommandManager.literal("disband")
                        .executes(c -> run(c.getSource(), () -> disband(c.getSource()))))
                .then(CommandManager.literal("pvp")
                        .executes(c -> run(c.getSource(), () -> togglePvp(c.getSource()))))
                .then(CommandManager.literal("home")
                        .executes(c -> run(c.getSource(), () -> useHome(c.getSource())))
                        .then(CommandManager.literal("set")
                                .executes(c -> run(c.getSource(), () -> setHome(c.getSource()))))
                        .then(CommandManager.literal("clear")
                                .executes(c -> run(c.getSource(), () -> clearHome(c.getSource())))))
                .then(CommandManager.literal("warp")
                        .executes(c -> run(c.getSource(), () -> listWarps(c.getSource())))
                        .then(CommandManager.literal("set")
                                .then(CommandManager.argument("name", StringArgumentType.word())
                                        .executes(c -> run(c.getSource(), () -> setWarp(c.getSource(), StringArgumentType.getString(c, "name"))))))
                        .then(CommandManager.literal("remove")
                                .then(CommandManager.argument("name", StringArgumentType.word())
                                        .executes(c -> run(c.getSource(), () -> removeWarp(c.getSource(), StringArgumentType.getString(c, "name"))))))
                        .then(CommandManager.literal("list")
                                .executes(c -> run(c.getSource(), () -> listWarps(c.getSource()))))
                        .then(CommandManager.argument("name", StringArgumentType.word())
                                .executes(c -> run(c.getSource(), () -> useWarp(c.getSource(), StringArgumentType.getString(c, "name"), "")))
                                .then(CommandManager.argument("password", StringArgumentType.word())
                                        .executes(c -> run(c.getSource(), () -> useWarp(c.getSource(),
                                                StringArgumentType.getString(c, "name"),
                                                StringArgumentType.getString(c, "password")))))))
                .then(CommandManager.literal("invite")
                        .then(CommandManager.argument("player", StringArgumentType.word())
                                .executes(c -> run(c.getSource(), () -> invite(c.getSource(), StringArgumentType.getString(c, "player"))))))
                .then(CommandManager.literal("accept")
                        .then(CommandManager.argument("team", StringArgumentType.word())
                                .executes(c -> run(c.getSource(), () -> acceptInvite(c.getSource(), StringArgumentType.getString(c, "team"))))))
                .then(CommandManager.literal("deny")
                        .then(CommandManager.argument("team", StringArgumentType.word())
                                .executes(c -> run(c.getSource(), () -> denyInvite(c.getSource(), StringArgumentType.getString(c, "team"))))))
                .then(CommandManager.literal("join")
                        .then(CommandManager.argument("team", StringArgumentType.word())
                                .executes(c -> run(c.getSource(), () -> requestJoin(c.getSource(), StringArgumentType.getString(c, "team"))))))
                .then(CommandManager.literal("unjoin")
                        .then(CommandManager.argument("team", StringArgumentType.word())
                                .executes(c -> run(c.getSource(), () -> cancelJoinRequest(c.getSource(), StringArgumentType.getString(c, "team"))))))
                .then(CommandManager.literal("requests")
                        .executes(c -> run(c.getSource(), () -> openRequests(c.getSource()))))
                .then(CommandManager.literal("chat")
                        .executes(c -> run(c.getSource(), () -> toggleChat(c.getSource())))));
    }

    @FunctionalInterface
    private interface CommandAction { int run() throws Exception; }

    private static int run(ServerCommandSource source, CommandAction action) {
        try {
            return action.run();
        } catch (Exception exception) {
            source.sendError(Text.literal(exception.getMessage() == null ? "Command failed." : exception.getMessage()));
            JustTeamsFabric.LOGGER.error("JustTeams command failed", exception);
            return 0;
        }
    }

    private static int openGui(ServerCommandSource source) throws Exception { TeamGuiManager.openMain(source.getPlayerOrThrow()); return 1; }

    private static int toggleChat(ServerCommandSource source) throws Exception {
        ServerPlayerEntity player = source.getPlayerOrThrow();
        if (!JustTeamsFabric.permissions().has(player, JustTeamsPermissions.COMMAND_CHAT)) {
            source.sendError(Text.literal("You do not have permission to use team chat."));
            return 0;
        }
        Team team = JustTeamsFabric.teams().getTeam(player.getUuid());
        if (team == null) {
            source.sendError(Text.literal("You are not in a team."));
            return 0;
        }
        boolean enabled = TeamChatManager.toggle(player);
        source.sendFeedback(() -> Text.literal(enabled ? "Team chat enabled. Your chat messages will only be sent to team members." : "Team chat disabled. Your chat messages are public again."), false);
        return 1;
    }

    private static int create(ServerCommandSource s, String name, String tag) throws Exception {
        ServerPlayerEntity p = s.getPlayerOrThrow();
        if (name.length() > 16 || tag.length() > 4) { s.sendError(Text.literal("Team name must be 16 characters or fewer and tag 4 characters or fewer.")); return 0; }
        try {
            Team t = JustTeamsFabric.teams().createTeam(name, tag, p.getUuid(), true, false, false);
            JustTeamsFabric.storage().save(JustTeamsFabric.teams());
            s.sendFeedback(() -> Text.literal("Created team " + t.getName() + " [" + t.getTag() + "]"), false);
            return 1;
        } catch (IllegalStateException e) { s.sendError(Text.literal(e.getMessage())); return 0; }
    }

    private static int info(ServerCommandSource s) throws Exception {
        Team t = JustTeamsFabric.teams().getTeam(s.getPlayerOrThrow().getUuid());
        if (t == null) { s.sendError(Text.literal("You are not in a team.")); return 0; }
        s.sendFeedback(() -> Text.literal("Team: " + t.getName() + " [" + t.getTag() + "]"), false);
        s.sendFeedback(() -> Text.literal("Members: " + t.getMembers().size() + " | Friendly fire: " + (t.isPvpEnabled() ? "ON" : "OFF")), false);
        return 1;
    }

    private static int leave(ServerCommandSource s) throws Exception {
        ServerPlayerEntity p = s.getPlayerOrThrow(); Team t = JustTeamsFabric.teams().getTeam(p.getUuid());
        if (t == null) { s.sendError(Text.literal("You are not in a team.")); return 0; }
        if (t.isOwner(p.getUuid())) { s.sendError(Text.literal("The owner cannot leave the team. Use /team disband.")); return 0; }
        TeamChatManager.disable(p.getUuid());
        JustTeamsFabric.teams().removeMember(t, p.getUuid()); JustTeamsFabric.storage().save(JustTeamsFabric.teams());
        s.sendFeedback(() -> Text.literal("You left " + t.getName() + "."), false); return 1;
    }

    private static int disband(ServerCommandSource s) throws Exception {
        ServerPlayerEntity p = s.getPlayerOrThrow(); Team t = JustTeamsFabric.teams().getTeam(p.getUuid());
        if (t == null || !t.isOwner(p.getUuid())) { s.sendError(Text.literal("You do not own a team.")); return 0; }
        for (TeamPlayer member : t.getMembers()) TeamChatManager.disable(member.getPlayerUuid());
        JustTeamsFabric.teams().unregister(t); JustTeamsFabric.storage().save(JustTeamsFabric.teams());
        s.sendFeedback(() -> Text.literal("Disbanded " + t.getName() + "."), false); return 1;
    }

    private static int togglePvp(ServerCommandSource s) throws Exception {
        ServerPlayerEntity p = s.getPlayerOrThrow(); Team t = JustTeamsFabric.teams().getTeam(p.getUuid());
        if (t == null || !t.isOwner(p.getUuid())) { s.sendError(Text.literal("Only the team owner can change friendly fire.")); return 0; }
        t.setPvpEnabled(!t.isPvpEnabled()); JustTeamsFabric.storage().save(JustTeamsFabric.teams());
        s.sendFeedback(() -> Text.literal("Friendly fire is now " + (t.isPvpEnabled() ? "ON" : "OFF") + "."), false); return 1;
    }

    private static int setHome(ServerCommandSource s) throws Exception {
        ServerPlayerEntity player = s.getPlayerOrThrow();
        Team team = requireTeam(s, player);
        TeamPlayer member = team.getMember(player.getUuid());
        if (member == null || !member.canSetHome()) { s.sendError(Text.literal("You do not have permission to set the team home.")); return 0; }
        team.setHome(TeamLocation.fromPlayer(player));
        JustTeamsFabric.storage().save(JustTeamsFabric.teams());
        s.sendFeedback(() -> Text.literal("Team home set at your current location."), false);
        return 1;
    }

    private static int clearHome(ServerCommandSource s) throws Exception {
        ServerPlayerEntity player = s.getPlayerOrThrow();
        Team team = requireTeam(s, player);
        TeamPlayer member = team.getMember(player.getUuid());
        if (member == null || !member.canSetHome()) { s.sendError(Text.literal("You do not have permission to clear the team home.")); return 0; }
        if (team.getHome() == null) { s.sendError(Text.literal("Your team does not have a home set.")); return 0; }
        team.clearHome();
        JustTeamsFabric.storage().save(JustTeamsFabric.teams());
        s.sendFeedback(() -> Text.literal("Team home cleared."), false);
        return 1;
    }

    private static int useHome(ServerCommandSource s) throws Exception {
        ServerPlayerEntity player = s.getPlayerOrThrow();
        Team team = requireTeam(s, player);
        TeamPlayer member = team.getMember(player.getUuid());
        if (member == null || !member.canUseHome()) { s.sendError(Text.literal("You do not have permission to use the team home.")); return 0; }
        if (team.getHome() == null) { s.sendError(Text.literal("Your team does not have a home set.")); return 0; }
        teleport(s, player, team.getHome());
        return 1;
    }

    private static int setWarp(ServerCommandSource s, String name) throws Exception {
        ServerPlayerEntity player = s.getPlayerOrThrow();
        Team team = requireTeam(s, player);
        TeamPlayer member = team.getMember(player.getUuid());
        if (member == null || !member.canSetHome()) { s.sendError(Text.literal("You do not have permission to create team warps.")); return 0; }
        if (name.length() > 32) { s.sendError(Text.literal("Warp name must be 32 characters or fewer.")); return 0; }
        if (team.getWarp(name) != null) { s.sendError(Text.literal("A warp with that name already exists.")); return 0; }
        TeamLocation location = TeamLocation.fromPlayer(player);
        team.addWarp(new TeamWarp(name, player.getUuid(), location.getDimension(), location.getX(), location.getY(), location.getZ(), location.getYaw(), location.getPitch()));
        JustTeamsFabric.storage().save(JustTeamsFabric.teams());
        s.sendFeedback(() -> Text.literal("Team warp '" + name + "' created."), false);
        return 1;
    }

    private static int removeWarp(ServerCommandSource s, String name) throws Exception {
        ServerPlayerEntity player = s.getPlayerOrThrow();
        Team team = requireTeam(s, player);
        TeamWarp warp = team.getWarp(name);
        if (warp == null) { s.sendError(Text.literal("Warp not found.")); return 0; }
        if (!team.isOwner(player.getUuid()) && !warp.getOwner().equals(player.getUuid())) { s.sendError(Text.literal("Only the team owner or warp creator can remove this warp.")); return 0; }
        team.removeWarp(name);
        JustTeamsFabric.storage().save(JustTeamsFabric.teams());
        s.sendFeedback(() -> Text.literal("Team warp '" + name + "' removed."), false);
        return 1;
    }

    private static int listWarps(ServerCommandSource s) throws Exception {
        ServerPlayerEntity player = s.getPlayerOrThrow();
        Team team = requireTeam(s, player);
        if (team.getWarps().isEmpty()) { s.sendFeedback(() -> Text.literal("Your team has no warps."), false); return 1; }
        s.sendFeedback(() -> Text.literal("Team warps:"), false);
        for (TeamWarp warp : team.getWarps()) {
            s.sendFeedback(() -> Text.literal("- " + warp.getName() + (warp.isEnabled() ? "" : " (disabled)")), false);
        }
        return 1;
    }

    private static int useWarp(ServerCommandSource s, String name, String password) throws Exception {
        ServerPlayerEntity player = s.getPlayerOrThrow();
        Team team = requireTeam(s, player);
        TeamWarp warp = team.getWarp(name);
        if (warp == null) { s.sendError(Text.literal("Warp not found.")); return 0; }
        if (!warp.isEnabled()) { s.sendError(Text.literal("That warp is disabled.")); return 0; }
        if (!warp.isMembersCanUse() && !team.isOwner(player.getUuid())) { s.sendError(Text.literal("You do not have permission to use that warp.")); return 0; }
        if (!warp.getPassword().isEmpty() && !warp.getPassword().equals(password)) { s.sendError(Text.literal("Incorrect warp password.")); return 0; }
        TeamLocation location = new TeamLocation(warp.getWorld(), warp.getX(), warp.getY(), warp.getZ(), warp.getYaw(), warp.getPitch());
        teleport(s, player, location);
        return 1;
    }

    private static void teleport(ServerCommandSource source, ServerPlayerEntity player, TeamLocation location) throws Exception {
        Identifier identifier = Identifier.tryParse(location.getDimension());
        if (identifier == null) throw new IllegalStateException("The saved team location has an invalid dimension: " + location.getDimension());
        RegistryKey<World> key = RegistryKey.of(RegistryKeys.WORLD, identifier);
        MinecraftServer server = source.getServer();
        ServerWorld world = server.getWorld(key);
        if (world == null) throw new IllegalStateException("The saved team location's dimension is not available on this server.");
        player.teleport(world, location.getX(), location.getY(), location.getZ(), Set.of(), location.getYaw(), location.getPitch(), true);
        source.sendFeedback(() -> Text.literal("Teleported to the team location."), false);
    }

    private static Team requireTeam(ServerCommandSource source, ServerPlayerEntity player) {
        Team team = JustTeamsFabric.teams().getTeam(player.getUuid());
        if (team == null) throw new IllegalStateException("You are not in a team.");
        return team;
    }

    private static int invite(ServerCommandSource s, String name) throws Exception {
        ServerPlayerEntity owner = s.getPlayerOrThrow(); Team t = JustTeamsFabric.teams().getTeam(owner.getUuid());
        if (t == null || !t.hasElevatedPermissions(owner.getUuid())) { s.sendError(Text.literal("Only the owner or co-owners can invite players.")); return 0; }
        ServerPlayerEntity target = s.getServer().getPlayerManager().getPlayerList().stream()
                .filter(p -> p.getGameProfile().name().equalsIgnoreCase(name)).findFirst().orElse(null);
        if (target == null) { s.sendError(Text.literal("That player is not online.")); return 0; }
        if (JustTeamsFabric.teams().isInTeam(target.getUuid())) { s.sendError(Text.literal("That player is already in a team.")); return 0; }
        t.addInvite(target.getUuid()); JustTeamsFabric.storage().save(JustTeamsFabric.teams());
        target.sendMessage(Text.literal("You have been invited to join " + t.getName() + ". Use /team accept " + t.getName() + "."));
        s.sendFeedback(() -> Text.literal("Invited " + name + " to your team."), false); return 1;
    }

    private static int acceptInvite(ServerCommandSource s, String name) throws Exception {
        ServerPlayerEntity p = s.getPlayerOrThrow();
        if (JustTeamsFabric.teams().isInTeam(p.getUuid())) { s.sendError(Text.literal("You are already in a team.")); return 0; }
        Team t = findTeam(name);
        if (t == null || !t.hasInvite(p.getUuid())) { s.sendError(Text.literal("You do not have an invite to that team.")); return 0; }
        t.removeInvite(p.getUuid());
        JustTeamsFabric.teams().addMember(t, new TeamPlayer(p.getUuid(), TeamRole.MEMBER, Instant.now(), false, false, false, true));
        JustTeamsFabric.storage().save(JustTeamsFabric.teams()); s.sendFeedback(() -> Text.literal("You joined " + t.getName() + "."), false); return 1;
    }

    private static int denyInvite(ServerCommandSource s, String name) throws Exception {
        ServerPlayerEntity p = s.getPlayerOrThrow(); Team t = findTeam(name);
        if (t == null || !t.hasInvite(p.getUuid())) { s.sendError(Text.literal("You do not have an invite to that team.")); return 0; }
        t.removeInvite(p.getUuid()); JustTeamsFabric.storage().save(JustTeamsFabric.teams());
        s.sendFeedback(() -> Text.literal("Declined the invitation to " + t.getName() + "."), false); return 1;
    }

    private static int requestJoin(ServerCommandSource s, String name) throws Exception {
        ServerPlayerEntity player = s.getPlayerOrThrow();
        if (JustTeamsFabric.teams().isInTeam(player.getUuid())) { s.sendError(Text.literal("You are already in a team.")); return 0; }
        Team t = findTeam(name);
        if (t == null) { s.sendError(Text.literal("Team not found.")); return 0; }
        if (!t.isPublic()) { s.sendError(Text.literal("That team is private. Ask an owner for an invitation.")); return 0; }
        if (t.hasJoinRequest(player.getUuid())) { s.sendError(Text.literal("You already have a pending request to that team.")); return 0; }
        t.addJoinRequest(player.getUuid());
        JustTeamsFabric.storage().save(JustTeamsFabric.teams());
        s.sendFeedback(() -> Text.literal("Join request sent to " + t.getName() + "."), false);
        for (ServerPlayerEntity online : s.getServer().getPlayerManager().getPlayerList()) {
            if (t.hasElevatedPermissions(online.getUuid())) online.sendMessage(Text.literal(player.getName().getString() + " requested to join " + t.getName() + "."), false);
        }
        return 1;
    }

    private static int cancelJoinRequest(ServerCommandSource s, String name) throws Exception {
        ServerPlayerEntity player = s.getPlayerOrThrow(); Team t = findTeam(name);
        if (t == null || !t.hasJoinRequest(player.getUuid())) { s.sendError(Text.literal("You do not have a pending request to that team.")); return 0; }
        t.removeJoinRequest(player.getUuid()); JustTeamsFabric.storage().save(JustTeamsFabric.teams());
        s.sendFeedback(() -> Text.literal("Cancelled your request to " + t.getName() + "."), false); return 1;
    }

    private static int openRequests(ServerCommandSource s) throws Exception {
        ServerPlayerEntity player = s.getPlayerOrThrow(); Team t = JustTeamsFabric.teams().getTeam(player.getUuid());
        if (t == null) { s.sendError(Text.literal("You are not in a team.")); return 0; }
        JoinRequestGui.open(player, t); return 1;
    }

    private static Team findTeam(String name) {
        return JustTeamsFabric.teams().getTeams().stream().filter(t -> t.getName().equalsIgnoreCase(name)).findFirst().orElse(null);
    }
}
