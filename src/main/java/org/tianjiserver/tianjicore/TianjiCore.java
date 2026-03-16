package org.tianjiserver.tianjicore;

import org.bukkit.plugin.java.JavaPlugin;
import org.tianjiserver.tianjicore.fixer.EndermanMushroomBugFix;
import org.tianjiserver.tianjicore.feature.PhantomSpawnBlocker;
import org.tianjiserver.tianjicore.fixer.RecipeBugFix;

public class TianjiCore extends JavaPlugin {

  private static TianjiCore instance;

  @Override
  public void onEnable() {
    instance = this;

    saveDefaultConfig();

    getServer().getPluginManager().registerEvents(new RecipeBugFix(), this);
    getServer().getPluginManager().registerEvents(new PhantomSpawnBlocker(), this);
    getServer().getPluginManager().registerEvents(new EndermanMushroomBugFix(), this);

    getLogger().info("TianjiCore is enabled！");
  }

  public static TianjiCore getInstance() {
    return instance;
  }
}
