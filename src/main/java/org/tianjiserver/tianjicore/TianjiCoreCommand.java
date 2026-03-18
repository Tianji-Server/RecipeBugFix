package org.tianjiserver.tianjicore;

import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.CommandSender;
import revxrsal.commands.annotation.Command;
import revxrsal.commands.annotation.Subcommand;
import revxrsal.commands.bukkit.annotation.CommandPermission;


@Command({"tianjicore", "tc"})
public class TianjiCoreCommand {
  
  private final TianjiCoreModuleHelper moduleHelper;
  private final MiniMessage mini = MiniMessage.miniMessage();

  public TianjiCoreCommand(TianjiCore plugin) {
    this.moduleHelper = new TianjiCoreModuleHelper(plugin);
  }

  public void bootstrap() {
    moduleHelper.bootstrap();
  }

  public void shutdown() {
    moduleHelper.shutdown();
  }

  @CommandPermission("tianjicore.command.admin")
  @Subcommand("toggle")
  public void handleToggleCommand(CommandSender sender, String moduleInput) {
    var result = moduleHelper.toggleModule(moduleInput);
    
    switch (result.status()) {
      case UNKNOWN_MODULE:
        sender.sendMessage(mini.deserialize("<red>未知模块: " + moduleInput));
        sender.sendMessage(mini.deserialize(
            "<yellow>可开关模块: <aqua>" + String.join(", ", moduleHelper.getToggleableModuleKeys())));
        break;
        
      case NOT_TOGGLEABLE:
        var moduleInfo = result.moduleInfo();
        sender.sendMessage(mini.deserialize(
            "<red>该模块不支持 toggle: " + moduleInfo.key()));
        sender.sendMessage(mini.deserialize(
            "<yellow>可开关模块: <aqua>" + String.join(", ", moduleHelper.getToggleableModuleKeys())));
        break;
        
      case FAILED:
        moduleInfo = result.moduleInfo();
        sender.sendMessage(mini.deserialize(
            "<red>模块切换失败: " + moduleInfo.displayName()));
        break;
        
      case SUCCESS:
        moduleInfo = result.moduleInfo();
        sender.sendMessage(mini.deserialize(
            "<green>" + moduleInfo.displayName() + " 已" + (moduleInfo.enabled() ? "开启" : "关闭")));
        break;
    }
  }

  @CommandPermission("tianjicore.command.admin")
  @Subcommand("reload")
  public void handleReloadCommand(CommandSender sender, String moduleInput) {
    var result = moduleHelper.reloadModule(moduleInput);
    
    switch (result.status()) {
      case SUCCESS_PLUGIN:
        sender.sendMessage(mini.deserialize("<green>插件与模块配置已重载"));
        break;
        
      case UNKNOWN_MODULE:
        sender.sendMessage(mini.deserialize("<red>未知模块: " + moduleInput));
        sender.sendMessage(mini.deserialize(
            "<yellow>可用模块: <aqua>" + String.join(", ", moduleHelper.getModuleKeys())));
        break;
        
      case FAILED:
        var moduleInfo = result.moduleInfo();
        sender.sendMessage(mini.deserialize(
            "<red>模块重载失败: " + moduleInfo.displayName()));
        break;
        
      case SUCCESS_MODULE:
        moduleInfo = result.moduleInfo();
        sender.sendMessage(mini.deserialize(
            "<green>" + moduleInfo.displayName() + " 已重载，当前状态: "
                + (moduleInfo.enabled() ? "<green>开启" : "<red>关闭")));
        break;
    }
  }


  @Subcommand("help")
  public void handleHelpCommand(CommandSender sender) {
    sender.sendMessage(mini.deserialize("<yellow>命令帮助:"));
    sender.sendMessage(mini.deserialize("<gray>/tc toggle <module> <white>开关指定模块"));
    sender.sendMessage(mini.deserialize("<gray>/tc reload <module|plugin> <white>重载插件或指定模块"));
    sender.sendMessage(mini.deserialize("<gray>可开关模块: <aqua>" + String.join(", ", moduleHelper.getToggleableModuleKeys())));
    sender.sendMessage(mini.deserialize("<gray>可重载模块: <aqua>" + String.join(", ", moduleHelper.getModuleKeys())));
    sender.sendMessage(mini.deserialize("<gray>插件重载参数: <aqua>" + moduleHelper.getReloadPluginTarget()));
  }
}
