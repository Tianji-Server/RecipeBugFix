package tianjicore;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;
import java.util.Locale;

public class TianjiCoreCommands implements CommandExecutor, TabCompleter {

    private static final String TOGGLE_SUB_COMMAND = "toggle";
    private static final String RELOAD_SUB_COMMAND = "reload";
    private final TianjiCoreCommandUtil commandUtil;

    public TianjiCoreCommands(TianjiCoreCommandUtil commandUtil) {
        this.commandUtil = commandUtil;
    }

    @Override
    @ParametersAreNonnullByDefault
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            commandUtil.sendHelp(sender, label);
            return true;
        }

        String subCommand = normalize(args[0]);
        if (TOGGLE_SUB_COMMAND.equals(subCommand)) {
            if (args.length < 2) {
                sender.sendMessage("§c用法: /" + label + " toggle <feature>");
                return true;
            }
            return commandUtil.handleToggleCommand(sender, args[1]);
        }

        if (RELOAD_SUB_COMMAND.equals(subCommand)) {
            if (args.length < 2) {
                sender.sendMessage("§c用法: /" + label + " reload <module>");
                return true;
            }
            return commandUtil.handleReloadCommand(sender, args[1]);
        }

        commandUtil.sendHelp(sender, label);
        return true;
    }

    @Override
    @ParametersAreNonnullByDefault
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return commandUtil.completeSubCommands(args[0]);
        }

        if (args.length == 2) {
            String subCommand = normalize(args[0]);
            if (TOGGLE_SUB_COMMAND.equals(subCommand)) {
                return commandUtil.completeToggleTargets(args[1]);
            }
            if (RELOAD_SUB_COMMAND.equals(subCommand)) {
                return commandUtil.completeReloadTargets(args[1]);
            }
        }

        return List.of();
    }

    private String normalize(String input) {
        return input.toLowerCase(Locale.ROOT);
    }
}
