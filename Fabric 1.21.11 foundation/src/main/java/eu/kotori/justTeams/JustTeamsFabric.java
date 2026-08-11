package eu.kotori.justTeams;

import eu.kotori.justTeams.commands.TeamCommand;
import eu.kotori.justTeams.gameplay.TeamFriendlyFire;
import eu.kotori.justTeams.permission.LuckPermsPermissionService;
import eu.kotori.justTeams.permission.PermissionService;
import eu.kotori.justTeams.storage.TeamStorage;
import eu.kotori.justTeams.team.TeamManager;
import eu.kotori.justTeams.util.ChatInputEvents;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public final class JustTeamsFabric implements ModInitializer {
    public static final String MOD_ID = "justteams";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    private static TeamManager teamManager;
    private static TeamStorage teamStorage;
    private static PermissionService permissionService;

    @Override
    public void onInitialize() {
        teamManager = new TeamManager();
        teamStorage = new TeamStorage();
        permissionService = createPermissionService();
        try {
            teamStorage.load(teamManager);
        } catch (IOException exception) {
            throw new RuntimeException("Unable to load JustTeams data", exception);
        }
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> TeamCommand.register(dispatcher));
        ChatInputEvents.register();
        TeamFriendlyFire.register();
        LOGGER.info("JustTeams Fabric core initialized with {} team(s)", teamManager.size());
    }

    private static PermissionService createPermissionService() {
        if (FabricLoader.getInstance().isModLoaded("luckperms")) {
            try {
                LOGGER.info("LuckPerms detected; enabling JustTeams LuckPerms permissions");
                return new LuckPermsPermissionService();
            } catch (RuntimeException exception) {
                LOGGER.warn("LuckPerms was detected but its API could not be initialized; using default JustTeams permissions", exception);
            }
        }
        return PermissionService.defaults();
    }

    public static TeamManager teams() {
        if (teamManager == null) throw new IllegalStateException("JustTeams has not initialized");
        return teamManager;
    }

    public static TeamStorage storage() {
        if (teamStorage == null) throw new IllegalStateException("JustTeams has not initialized");
        return teamStorage;
    }

    public static PermissionService permissions() {
        if (permissionService == null) throw new IllegalStateException("JustTeams has not initialized");
        return permissionService;
    }
}
