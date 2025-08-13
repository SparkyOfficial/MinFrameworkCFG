package com.sparky.config;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sparky.config.reload.HotReloader;
import com.sparky.config.source.ConfigSource;
import com.sparky.config.util.Maps;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * Central manager to load, merge, access and hot-reload configuration from multiple sources.
 * Features:
 * - YAML, JSON, ENV sources via {@link ConfigSource}
 * - Deep merge with later sources overriding earlier ones
 * - Typed getters with basic coercion
 * - Dot-path navigation for nested structures (e.g. "db.host")
 * - Optional hot reload that re-reads changed file-based sources
 */
public class ConfigManager implements Closeable {
    private static final Logger log = LoggerFactory.getLogger(ConfigManager.class);

    private final List<ConfigSource> sources;
    private final List<Consumer<Map<String, Object>>> listeners = new CopyOnWriteArrayList<>();
    private final ObjectMapper converter; // used for type coercion

    private volatile Map<String, Object> merged = Collections.emptyMap();
    private HotReloader reloader;

    private ConfigManager(List<ConfigSource> sources, boolean hotReload, ObjectMapper converter) throws IOException {
        this.sources = List.copyOf(sources);
        this.converter = converter == null ? new ObjectMapper() : converter;
        reload();
        if (hotReload) startReloader();
    }

    public static Builder builder() { return new Builder(); }

    public synchronized void reload() throws IOException {
        Map<String, Object> result = new LinkedHashMap<>();
        for (ConfigSource s : sources) {
            Map<String, Object> next = s.load();
            if (next == null) continue;
            result = Maps.deepMerge(result, next);
        }
        merged = Collections.unmodifiableMap(result);
        for (Consumer<Map<String, Object>> l : listeners) {
            try { l.accept(merged); } catch (Exception e) { log.warn("Listener error", e); }
        }
        log.debug("Config reloaded. Keys: {}", merged.keySet());
    }

    private void startReloader() {
        Set<Path> paths = new LinkedHashSet<>();
        for (ConfigSource s : sources) s.watchPath().ifPresent(paths::add);
        if (paths.isEmpty()) return;
        reloader = new HotReloader(paths, () -> {
            try { reload(); } catch (IOException e) { log.warn("Reload failed", e); }
        });
        reloader.start();
    }

    public Map<String, Object> asMap() { return merged; }

    public Optional<Object> find(String path) { return Optional.ofNullable(Maps.getByPath(merged, path)); }

    public <T> Optional<T> find(String path, Class<T> type) {
        Object v = Maps.getByPath(merged, path);
        if (v == null) return Optional.empty();
        return Optional.of(Maps.coerce(v, type, converter));
    }

    public <T> T get(String path, Class<T> type) {
        return find(path, type).orElseThrow(() -> new ValidationException("Missing config: " + path));
    }

    public String getString(String path) { return get(path, String.class); }
    public Integer getInt(String path) { return get(path, Integer.class); }
    public Long getLong(String path) { return get(path, Long.class); }
    public Boolean getBool(String path) { return get(path, Boolean.class); }

    public <T> T get(String path, TypeReference<T> ref) {
        Object v = Maps.getByPath(merged, path);
        if (v == null) throw new ValidationException("Missing config: " + path);
        return converter.convertValue(v, ref);
    }

    public void addChangeListener(Consumer<Map<String, Object>> listener) { listeners.add(listener); }
    public void removeChangeListener(Consumer<Map<String, Object>> listener) { listeners.remove(listener); }

    @Override public void close() {
        if (reloader != null) reloader.close();
    }

    public static class Builder {
        private final List<ConfigSource> sources = new ArrayList<>();
        private boolean hotReload = true;
        private ObjectMapper converter;

        public Builder addSource(ConfigSource source) { this.sources.add(source); return this; }
        public Builder addSources(Collection<ConfigSource> srcs) { this.sources.addAll(srcs); return this; }
        public Builder hotReload(boolean enabled) { this.hotReload = enabled; return this; }
        public Builder converter(ObjectMapper mapper) { this.converter = mapper; return this; }

        public ConfigManager build() throws IOException { return new ConfigManager(sources, hotReload, converter); }
    }
}
