package org.tianjiserver.tianjicore;

import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * TianjiCore 插件主入口。
 * 负责初始化配置、命令系统与模块生命周期。
 */
public class TianjiCore extends JavaPlugin {

    private static TianjiCore instance;
    private TianjiCoreCommandUtil commandUtil;

    @Override
    public void onEnable() {
        instance = this;

        // 首次启动时生成默认配置文件。
        saveDefaultConfig();

        // 初始化命令工具，同时触发模块注册与按配置加载。
        commandUtil = new TianjiCoreCommandUtil(this);
        commandUtil.bootstrap();

        // 绑定 /tianji 命令执行器与补全器。
        TianjiCoreCommands commandExecutor = new TianjiCoreCommands(commandUtil);
        PluginCommand tianjiCommand = getCommand("tianji");
        if (tianjiCommand == null) {
            getLogger().severe("未找到 tianji 命令，请检查 plugin.yml 配置");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        tianjiCommand.setExecutor(commandExecutor);
        tianjiCommand.setTabCompleter(commandExecutor);

        getLogger().info("TianjiCore 已启动");
    }

    @Override
    public void onDisable() {
        if (commandUtil != null) {
            // 插件关闭时统一卸载已注册的监听器，避免残留状态。
            commandUtil.shutdown();
        }
    }

    public static TianjiCore getInstance() {
        return instance;
    }
}
