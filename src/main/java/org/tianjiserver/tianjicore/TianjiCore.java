package org.tianjiserver.tianjicore;

import org.bukkit.plugin.java.JavaPlugin;

public class TianjiCore extends JavaPlugin {

  private static TianjiCore instance;

  @Override
  public void onEnable() {
    instance = this;

    saveDefaultConfig();

    getServer().getPluginManager().registerEvents(new RecipeBugFix(), this);
    getServer().getPluginManager().registerEvents(new MobControlListener(), this);

    getLogger().info("TianjiCore is enabled！");
  }

  public static TianjiCore getInstance() {
    return instance;
  }
}
