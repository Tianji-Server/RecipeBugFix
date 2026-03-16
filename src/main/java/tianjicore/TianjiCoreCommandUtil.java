package tianjicore;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import java.util.List;

public class TianjiCoreCommandUtil {

    private static final String PREFIX = ChatColor.GOLD + "[TianjiCore] " + ChatColor.RESET;
    private static final String TOGGLE_SUB_COMMAND = "toggle";
    private static final String RELOAD_SUB_COMMAND = "reload";
    private final TianjiCoreModuleManager moduleManager;

    public TianjiCoreCommandUtil(TianjiCore plugin) {
        this.moduleManager = new TianjiCoreModuleManager(plugin);
    }

    public void bootstrap() {
        moduleManager.bootstrap();
    }

    public void shutdown() {
        moduleManager.shutdown();
    }

    public List<String> completeSubCommands(String input) {
        return moduleManager.filterByPrefix(List.of(TOGGLE_SUB_COMMAND, RELOAD_SUB_COMMAND), input);
    }

    public List<String> completeToggleTargets(String input) {
        return moduleManager.filterByPrefix(moduleManager.getToggleableModuleKeys(), input);
    }

    public List<String> completeReloadTargets(String input) {
        return moduleManager.filterByPrefix(moduleManager.getReloadTargets(), input);
    }

    public boolean handleToggleCommand(CommandSender sender, String moduleInput) {
        TianjiCoreModuleManager.ToggleResult result = moduleManager.toggle(moduleInput);
        if (result.status() == TianjiCoreModuleManager.ToggleStatus.UNKNOWN_MODULE) {
            sender.sendMessage(PREFIX + ChatColor.RED + "未知模块: " + moduleInput);
            sender.sendMessage(
                    PREFIX + ChatColor.YELLOW + "可开关模块: " + String.join(", ", moduleManager.getToggleableModuleKeys())
            );
            return true;
        }

        TianjiCoreModuleManager.ModuleInfo moduleInfo = result.moduleInfo();
        if (result.status() == TianjiCoreModuleManager.ToggleStatus.NOT_TOGGLEABLE) {
            sender.sendMessage(PREFIX + ChatColor.RED + "该模块不支持 toggle: " + moduleInfo.key());
            sender.sendMessage(
                    PREFIX + ChatColor.YELLOW + "可开关模块: " + String.join(", ", moduleManager.getToggleableModuleKeys())
            );
            return true;
        }

        if (result.status() == TianjiCoreModuleManager.ToggleStatus.FAILED) {
            sender.sendMessage(PREFIX + ChatColor.RED + "模块切换失败: " + moduleInfo.displayName());
            return true;
        }

        sender.sendMessage(
                PREFIX + ChatColor.GREEN + moduleInfo.displayName() + " 已" + (moduleInfo.enabled() ? "开启" : "关闭")
        );
        return true;
    }

    public boolean handleReloadCommand(CommandSender sender, String moduleInput) {
        TianjiCoreModuleManager.ReloadResult result = moduleManager.reload(moduleInput);
        if (result.status() == TianjiCoreModuleManager.ReloadStatus.SUCCESS_PLUGIN) {
            sender.sendMessage(PREFIX + ChatColor.GREEN + "插件与模块配置已重载");
            return true;
        }

        if (result.status() == TianjiCoreModuleManager.ReloadStatus.UNKNOWN_MODULE) {
            sender.sendMessage(PREFIX + ChatColor.RED + "未知模块: " + moduleInput);
            sender.sendMessage(PREFIX + ChatColor.YELLOW + "可用模块: " + String.join(", ", moduleManager.getModuleKeys()));
            return true;
        }

        TianjiCoreModuleManager.ModuleInfo moduleInfo = result.moduleInfo();
        if (result.status() == TianjiCoreModuleManager.ReloadStatus.FAILED) {
            sender.sendMessage(PREFIX + ChatColor.RED + "模块重载失败: " + moduleInfo.displayName());
            return true;
        }

        sender.sendMessage(
                PREFIX + ChatColor.GREEN + moduleInfo.displayName() + " 已重载，当前状态: " + (moduleInfo.enabled() ? "开启" : "关闭")
        );
        return true;
    }

    public void sendHelp(CommandSender sender, String label) {
        sender.sendMessage(PREFIX + ChatColor.YELLOW + "命令帮助:");
        sender.sendMessage(ChatColor.GRAY + "/" + label + " toggle <feature> " + ChatColor.WHITE + "开关指定模块");
        sender.sendMessage(ChatColor.GRAY + "/" + label + " reload <module> " + ChatColor.WHITE + "重载插件或指定模块");
        sender.sendMessage(
                ChatColor.GRAY + "可开关模块: " + ChatColor.AQUA + String.join(", ", moduleManager.getToggleableModuleKeys())
        );
        sender.sendMessage(ChatColor.GRAY + "可重载模块: " + ChatColor.AQUA + String.join(", ", moduleManager.getModuleKeys()));
        sender.sendMessage(ChatColor.GRAY + "插件重载参数: " + ChatColor.AQUA + moduleManager.getReloadPluginTarget());
    }
}
