package org.tianjiserver.tianjicore.fixer;

import org.bukkit.Material;
import org.bukkit.entity.Enderman;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityChangeBlockEvent;

// 修复末地蘑菇导致服务器崩溃的bug
public class EndermanMushroomBugFix implements Listener {

  @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
  public void onEndermanPutMushroom(EntityChangeBlockEvent event) {
    if (!(event.getEntity() instanceof Enderman enderman)) {
      return;
    }
    if (isMushroom(event.getTo()) || isMushroom(event.getBlockData().getMaterial())) {
      event.setCancelled(true);
      enderman.setCarriedBlock(null);
    }
  }

  private boolean isMushroom(Material type) {
    return type == Material.BROWN_MUSHROOM
        || type == Material.RED_MUSHROOM
        || type == Material.CRIMSON_FUNGUS
        || type == Material.WARPED_FUNGUS;
  }
}
