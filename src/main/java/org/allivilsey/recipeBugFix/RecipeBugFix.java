package org.allivilsey.recipeBugFix;

import org.bukkit.Bukkit;
import org.bukkit.Keyed;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.Recipe;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public final class RecipeBugFix extends JavaPlugin implements Listener {

    List<NamespacedKey> allRecipeKeys;

    @Override
    public void onEnable() {
        allRecipeKeys = new ArrayList<>();
        Iterator<Recipe> iterator = Bukkit.recipeIterator();
        while (iterator.hasNext()) {
            Recipe recipe = iterator.next();
            if (recipe instanceof Keyed) {
                NamespacedKey key = ((Keyed) recipe).getKey();
                allRecipeKeys.add(key);
            }
        }
        getLogger().info("Recipe collected");

        Bukkit.getPluginManager().registerEvents(this,this);
    }

    @Override
    public void onDisable() {

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
