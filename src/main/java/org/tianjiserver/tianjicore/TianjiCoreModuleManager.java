package org.tianjiserver.tianjicore;

import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.tianjiserver.tianjicore.feature.PhantomSpawnBlocker;
import org.tianjiserver.tianjicore.fixer.EndermanMushroomBugFix;
import org.tianjiserver.tianjicore.fixer.RecipeBugFix;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

class TianjiCoreModuleManager {

    private static final String RELOAD_PLUGIN_TARGET = "plugin";
    private static final Set<String> RELOAD_PLUGIN_ALIASES = Set.of(
            "plugin",
            "core",
            "all",
            "tianjicore"
    );

    private final TianjiCore plugin;
    private final Map<String, ModuleState> modules = new LinkedHashMap<>();
    private final Map<String, String> moduleAliasIndex = new LinkedHashMap<>();

    TianjiCoreModuleManager(TianjiCore plugin) {
        this.plugin = plugin;
    }

    void bootstrap() {
        registerModule(
                "recipebugfix",
                "配方修复",
                false,
                RecipeBugFix::new,
                "recipe",
                "recipes"
        );
        registerModule(
                "phantomspawnblocker",
                "幻翼阻止",
                true,
                PhantomSpawnBlocker::new,
                "phantom",
                "phantomblocker"
        );
        registerModule(
                "endermanmushroombugfix",
                "末影人蘑菇修复",
                false,
                EndermanMushroomBugFix::new,
                "enderman",
                "mushroomfix"
        );

        plugin.getConfig().options().copyDefaults(true);
        modules.keySet().forEach(moduleKey ->
                plugin.getConfig().addDefault(moduleConfigPath(moduleKey), true)
        );
        plugin.saveConfig();

        applyConfigStates(false);
    }

    void shutdown() {
        modules.values().forEach(this::stopModuleIfRunning);
    }

    ToggleResult toggle(String rawModuleInput) {
        ModuleState module = findModule(rawModuleInput);
        if (module == null) {
            return new ToggleResult(ToggleStatus.UNKNOWN_MODULE, null);
        }
        if (!module.toggleable) {
            return new ToggleResult(ToggleStatus.NOT_TOGGLEABLE, module.toInfo());
        }

        boolean targetState = !module.enabled;
        boolean success = targetState ? startModule(module) : stopModule(module);
        if (!success) {
            return new ToggleResult(ToggleStatus.FAILED, module.toInfo());
        }

        plugin.getConfig().set(moduleConfigPath(module.key), targetState);
        plugin.saveConfig();
        return new ToggleResult(ToggleStatus.SUCCESS, module.toInfo());
    }

    ReloadResult reload(String rawTargetInput) {
        String target = normalize(rawTargetInput);
        if (RELOAD_PLUGIN_ALIASES.contains(target)) {
            plugin.reloadConfig();
            applyConfigStates(true);
            return new ReloadResult(ReloadStatus.SUCCESS_PLUGIN, null);
        }

        ModuleState module = findModule(rawTargetInput);
        if (module == null) {
            return new ReloadResult(ReloadStatus.UNKNOWN_MODULE, null);
        }

        plugin.reloadConfig();
        boolean shouldEnable = plugin.getConfig().getBoolean(moduleConfigPath(module.key), true);
        boolean success = shouldEnable ? restartModule(module) : stopModule(module);
        if (!success) {
            return new ReloadResult(ReloadStatus.FAILED, module.toInfo());
        }

        return new ReloadResult(ReloadStatus.SUCCESS_MODULE, module.toInfo());
    }

    String getReloadPluginTarget() {
        return RELOAD_PLUGIN_TARGET;
    }

    List<String> getReloadTargets() {
        List<String> targets = new ArrayList<>();
        targets.add(RELOAD_PLUGIN_TARGET);
        targets.addAll(getModuleKeys());
        return targets;
    }

    List<String> getModuleKeys() {
        return List.copyOf(modules.keySet());
    }

    List<String> getToggleableModuleKeys() {
        return modules.values().stream()
                .filter(module -> module.toggleable)
                .map(module -> module.key)
                .collect(Collectors.toList());
    }

    List<String> filterByPrefix(List<String> sources, String rawInput) {
        String normalizedInput = normalize(rawInput);
        return sources.stream()
                .filter(option -> option.toLowerCase(Locale.ROOT).startsWith(normalizedInput))
                .collect(Collectors.toList());
    }

    private void applyConfigStates(boolean forceRestartEnabledModule) {
        modules.values().forEach(module -> {
            boolean shouldEnable = plugin.getConfig().getBoolean(moduleConfigPath(module.key), true);
            if (shouldEnable) {
                if (forceRestartEnabledModule) {
                    restartModule(module);
                } else {
                    startModule(module);
                }
            } else {
                stopModule(module);
            }
        });
    }

    private boolean restartModule(ModuleState module) {
        if (!stopModule(module)) {
            return false;
        }
        return startModule(module);
    }

    private boolean startModule(ModuleState module) {
        if (module.enabled) {
            return true;
        }

        try {
            Listener listener = module.listenerFactory.get();
            plugin.getServer().getPluginManager().registerEvents(listener, plugin);
            module.listenerInstance = listener;
            module.enabled = true;
            plugin.getLogger().info("模块已开启: " + module.key);
            return true;
        } catch (Exception exception) {
            plugin.getLogger().severe("模块开启失败: " + module.key + "，原因: " + exception.getMessage());
            module.listenerInstance = null;
            module.enabled = false;
            return false;
        }
    }

    private boolean stopModule(ModuleState module) {
        if (!module.enabled) {
            return true;
        }

        stopModuleIfRunning(module);
        plugin.getLogger().info("模块已关闭: " + module.key);
        return true;
    }

    private void stopModuleIfRunning(ModuleState module) {
        if (module.listenerInstance != null) {
            HandlerList.unregisterAll(module.listenerInstance);
        }
        module.listenerInstance = null;
        module.enabled = false;
    }

    private void registerModule(
            String moduleKey,
            String displayName,
            boolean toggleable,
            Supplier<? extends Listener> listenerFactory,
            String... aliases
    ) {
        String normalizedKey = normalize(moduleKey);
        ModuleState module = new ModuleState(normalizedKey, displayName, toggleable, listenerFactory);
        modules.put(normalizedKey, module);
        moduleAliasIndex.put(normalizedKey, normalizedKey);

        for (String alias : aliases) {
            moduleAliasIndex.put(normalize(alias), normalizedKey);
        }
    }

    private ModuleState findModule(String rawInput) {
        String normalizedInput = normalize(rawInput);
        String moduleKey = moduleAliasIndex.get(normalizedInput);
        if (moduleKey == null) {
            return null;
        }
        return modules.get(moduleKey);
    }

    private String moduleConfigPath(String moduleKey) {
        return "modules." + moduleKey + ".enabled";
    }

    private String normalize(String input) {
        return input.toLowerCase(Locale.ROOT).trim();
    }

    enum ToggleStatus {
        SUCCESS,
        UNKNOWN_MODULE,
        NOT_TOGGLEABLE,
        FAILED
    }

    enum ReloadStatus {
        SUCCESS_PLUGIN,
        SUCCESS_MODULE,
        UNKNOWN_MODULE,
        FAILED
    }

    record ModuleInfo(String key, String displayName, boolean enabled) {
    }

    record ToggleResult(ToggleStatus status, ModuleInfo moduleInfo) {
    }

    record ReloadResult(ReloadStatus status, ModuleInfo moduleInfo) {
    }

    private static class ModuleState {
        private final String key;
        private final String displayName;
        private final boolean toggleable;
        private final Supplier<? extends Listener> listenerFactory;
        private Listener listenerInstance;
        private boolean enabled;

        private ModuleState(
                String key,
                String displayName,
                boolean toggleable,
                Supplier<? extends Listener> listenerFactory
        ) {
            this.key = key;
            this.displayName = displayName;
            this.toggleable = toggleable;
            this.listenerFactory = listenerFactory;
            this.listenerInstance = null;
            this.enabled = false;
        }

        private ModuleInfo toInfo() {
            return new ModuleInfo(key, displayName, enabled);
        }
    }
}
