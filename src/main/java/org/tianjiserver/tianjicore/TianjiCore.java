package org.tianjiserver.tianjicore;

import org.bukkit.plugin.java.JavaPlugin;
import revxrsal.commands.bukkit.BukkitLamp;

/**
 * TianjiCore 插件主入口。
 * 负责初始化配置、命令系统与模块生命周期。
 */
public class TianjiCore extends JavaPlugin {

    private static TianjiCore instance;
    private TianjiCoreCommand commandHandler;

    @Override
    public void onEnable() {
        instance = this;

        saveDefaultConfig();

        commandHandler = new TianjiCoreCommand(this);
        commandHandler.bootstrap();

        var lamp = BukkitLamp.builder(this).build();
        lamp.register(commandHandler);

        getLogger().info("TianjiCore 已启动");
    }

    @Override
    public void onDisable() {
        if (commandHandler != null) {
            commandHandler.shutdown();
        }
    }

    public static TianjiCore getInstance() {
        return instance;
    }
}
