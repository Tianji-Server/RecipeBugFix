package org.tianjiserver.tianjicore.feature;

import org.bukkit.Bukkit;
import org.bukkit.Keyed;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.Recipe;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.tianjiserver.tianjicore.TianjiCore;

// 修复某些情况下玩家jei模组显示需要管理员给予配方的bug
public class RecipeBugFix implements Listener {

    List<NamespacedKey> allRecipeKeys;

    public RecipeBugFix() {
        allRecipeKeys = new ArrayList<>();
        Iterator<Recipe> iterator = Bukkit.recipeIterator();
        while (iterator.hasNext()) {
            Recipe recipe = iterator.next();
            if (recipe instanceof Keyed) {
                NamespacedKey key = ((Keyed) recipe).getKey();
                allRecipeKeys.add(key);
            }
        }
        TianjiCore.getInstance().getLogger().info("Recipe collected");
        TianjiCore.getInstance().getLogger().info("RecipeBugFix feature is loaded");
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        try {
            player.discoverRecipes(allRecipeKeys);
            Bukkit.getLogger().info("[RecipeBugFix] Player " + player.getName() + " received recipe");
        } catch (Exception e) {
            Bukkit.getLogger().severe("[RecipeBugFix] " + e.getMessage());
        }
    }

}
