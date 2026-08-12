package eu.kotori.justTeams.gui;

import eu.kotori.justTeams.economy.TeamBank;
import eu.kotori.justTeams.team.Team;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.SimpleNamedScreenHandlerFactory;
import net.minecraft.text.Text;

/** Opens the team's item-backed bank as a vanilla 6-row chest interface. */
public final class TeamBankGui {
    private TeamBankGui() {}

    public static void open(PlayerEntity player, Team team) {
        TeamBank bank = team.getBank();
        player.openHandledScreen(new SimpleNamedScreenHandlerFactory(
                (syncId, playerInventory, ignored) -> GenericContainerScreenHandler.createGeneric9x6(
                        syncId, playerInventory, bank
                ),
                Text.literal("Team Bank - " + team.getName())
        ));
    }
}
