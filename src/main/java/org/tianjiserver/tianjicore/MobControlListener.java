package org.tianjiserver.tianjicore;

import org.bukkit.Material;
import org.bukkit.entity.Enderman;
import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;

public class MobControlListener implements Listener {

  @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
  public void onCreatureSpawn(CreatureSpawnEvent event) {

    if (event.getEntityType() == EntityType.PHANTOM) {
      event.setCancelled(true);
    }
  }
    
  @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEndermanPutMushroom(EntityChangeBlockEvent event){
        if(!(event.getEntity() instanceof Enderman)){
            return;
        }
        if(isMushroom(event.getTo()) || isMushroom(event.getBlockData().getMaterial())){
            event.setCancelled(true);
            Enderman enderman = (Enderman) event.getEntity();
            enderman.setCarriedBlock(null);
        }
    }

    private boolean isMushroom(Material type){
        return type == Material.BROWN_MUSHROOM
                || type == Material.RED_MUSHROOM
                || type == Material.CRIMSON_FUNGUS
                || type == Material.WARPED_FUNGUS;
    }
  }

