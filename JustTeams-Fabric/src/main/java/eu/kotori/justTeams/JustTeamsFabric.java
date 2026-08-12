package eu.kotori.justTeams;

import eu.kotori.justTeams.commands.TeamCommand;
import eu.kotori.justTeams.config.JustTeamsConfig;
import eu.kotori.justTeams.gameplay.TeamFriendlyFire;
import eu.kotori.justTeams.storage.TeamStorage;
import eu.kotori.justTeams.team.TeamManager;
import eu.kotori.justTeams.util.ChatInputEvents;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.server.MinecraftServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public final class JustTeamsFabric implements ModInitializer {
    public static final String MOD_ID = "justteams";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    private static TeamManager teamManager;
    private static TeamStorage teamStorage;
    private static JustTeamsConfig config;

    @Override
    public void onInitialize() {
        try {
            config = new JustTeamsConfig(net.fabricmc.loader.api.FabricLoader.getInstance().getConfigDir());
        } catch (IOException exception) {
            throw new RuntimeException("Unable to load JustTeams configuration", exception);
        }

        teamManager = new TeamManager();
        teamStorage = new TeamStorage();

        ServerLifecycleEvents.SERVER_STARTING.register(this::loadTeamData);
        ServerLifecycleEvents.AFTER_SAVE.register((server, flush, force) -> saveTeamData(server, false));
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> saveTeamData(server, true));

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> TeamCommand.register(dispatcher));
        ChatInputEvents.register();
        TeamFriendlyFire.register();
        LOGGER.info("JustTeams Fabric core initialized");
    }

    private void loadTeamData(MinecraftServer server) {
        try {
            teamStorage.load(teamManager);
            LOGGER.info("Loaded {} team(s) including persistent bank inventories", teamManager.size());
        } catch (IOException exception) {
            throw new RuntimeException("Unable to load JustTeams data", exception);
        }
    }

    private void saveTeamData(MinecraftServer server, boolean logSuccess) {
        try {
            teamStorage.save(teamManager);
            if (logSuccess) LOGGER.info("Saved {} team(s) including persistent bank inventories", teamManager.size());
        } catch (IOException exception) {
            LOGGER.error("Unable to save JustTeams data", exception);
        }
    }

    public static TeamManager teams() {
        if (teamManager == null) throw new IllegalStateException("JustTeams has not initialized");
        return teamManager;
    }

    public static TeamStorage storage() {
        if (teamStorage == null) throw new IllegalStateException("JustTeams has not initialized");
        return teamStorage;
    }

    public static JustTeamsConfig config() {
        if (config == null) throw new IllegalStateException("JustTeams has not initialized");
        return config;
    }
}
