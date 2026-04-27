package mx.jume.aquahunger.commands;

import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import mx.jume.aquahunger.compat.SuperlativeManager;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;

import javax.annotation.Nonnull;

public class HungryStatsCommand extends CommandBase {
    public HungryStatsCommand() {
        super("stats", "Show server records for gluttony and thirst");
    }

    @Override
    protected void executeSync(@Nonnull CommandContext context) {
        if (context.sender() instanceof PlayerRef player) {
            SuperlativeManager.showSuperlatives(player);
        }
    }
}
