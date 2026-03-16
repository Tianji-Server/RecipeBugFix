package org.tianjiserver.tianjicore;

import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public class TianjiCore extends JavaPlugin {

    private static TianjiCore instance;
    private TianjiCoreCommandUtil commandUtil;

    @Override
    public void onEnable() {
        instance = this;

        saveDefaultConfig();

        commandUtil = new TianjiCoreCommandUtil(this);
        commandUtil.bootstrap();

        TianjiCoreCommands commandExecutor = new TianjiCoreCommands(commandUtil);
        PluginCommand tianjiCommand = getCommand("tianji");
        if (tianjiCommand == null) {
            getLogger().severe("未找到 tianji 命令，请检查 plugin.yml 配置");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        tianjiCommand.setExecutor(commandExecutor);
        tianjiCommand.setTabCompleter(commandExecutor);

        getLogger().info("TianjiCore is enabled！");
    }

    @Override
    public void onDisable() {
        if (commandUtil != null) {
            commandUtil.shutdown();
        }
    }

    public static TianjiCore getInstance() {
        return instance;
    }
}
