package org.tianjiserver.tianjicore;

import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.tianjiserver.tianjicore.feature.PhantomSpawnBlocker;
import org.tianjiserver.tianjicore.fixer.EndermanMushroomBugFix;
import org.tianjiserver.tianjicore.fixer.RecipeBugFix;
import org.tianjiserver.tianjicore.itemloreandsignature.ItemLoreAndSignature;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * 模块管理器。
 * 统一管理模块注册、启停、重载与配置持久化。
 */
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
        // 在这里集中声明模块，命令层无需感知具体模块实现。
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
                "阻止幻翼生成",
                true,
                PhantomSpawnBlocker::new,
                "phantom",
                "phantomblocker"
        );
        registerModule(
                "endermanmushroombugfix",
                "末地蘑菇修复",
                false,
                EndermanMushroomBugFix::new,
                "enderman",
                "mushroomfix"
        );
        registerModule(
                "itemloreandsignature",
                "物品签名锻造",
                true,
                () -> new ItemLoreAndSignature(plugin),
                "itemsign",
                "forge",
                "lore"
        );

        plugin.getConfig().options().copyDefaults(true);
        modules.keySet().forEach(moduleKey ->
                plugin.getConfig().addDefault(moduleConfigPath(moduleKey), true)
        );
        // 保存默认值后，按配置应用模块状态。
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
        // toggle 的目标状态为“当前状态取反”。
        if (targetState && !startModule(module)) {
            return new ToggleResult(ToggleStatus.FAILED, module.toInfo());
        }
        if (!targetState) {
            stopModule(module);
        }

        plugin.getConfig().set(moduleConfigPath(module.key), targetState);
        plugin.saveConfig();
        return new ToggleResult(ToggleStatus.SUCCESS, module.toInfo());
    }

    ReloadResult reload(String rawTargetInput) {
        String target = normalize(rawTargetInput);
        if (RELOAD_PLUGIN_ALIASES.contains(target)) {
            // 插件级重载：重读配置并按新配置刷新全部模块。
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
        if (shouldEnable && !restartModule(module)) {
            return new ReloadResult(ReloadStatus.FAILED, module.toInfo());
        }
        if (!shouldEnable) {
            stopModule(module);
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

    boolean isModuleEnabled(String rawModuleInput) {
        ModuleState module = findModule(rawModuleInput);
        return module != null && module.enabled;
    }

    private void applyConfigStates(boolean forceRestartEnabledModule) {
        // 根据配置统一校准模块状态，避免内存状态与配置不一致。
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
        stopModule(module);
        return startModule(module);
    }

    private boolean startModule(ModuleState module) {
        if (module.enabled) {
            return true;
        }

        try {
            // 每次启用都创建新监听器实例，避免复用旧实例引入脏状态。
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

    private void stopModule(ModuleState module) {
        if (!module.enabled) {
            return;
        }

        stopModuleIfRunning(module);
        plugin.getLogger().info("模块已关闭: " + module.key);
    }

    private void stopModuleIfRunning(ModuleState module) {
        if (module.listenerInstance != null) {
            // 注销监听器，确保模块关闭后不再响应事件。
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
