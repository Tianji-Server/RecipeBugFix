package org.tianjiserver.tianjicore;

import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;

public class NoPhantom implements Listener {

  @EventHandler
  public void onCreatureSpawn(CreatureSpawnEvent event) {

    if (event.getEntityType() == EntityType.PHANTOM) {
      event.setCancelled(true);
    }

  }
}
