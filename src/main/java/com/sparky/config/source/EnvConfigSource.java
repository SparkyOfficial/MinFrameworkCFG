package com.sparky.config.source;

import java.util.*;

/**
 * Reads environment variables into a nested map.
 * Mapping rules:
 * - Optional prefix filters variables (e.g. APP_)
 * - Double underscore "__" becomes a dot path separator (e.g. DB__HOST -> db.host)
 * - Keys are normalized to lower-case with dashes/periods preserved after expansion
 */
public class EnvConfigSource implements ConfigSource {
    private final String prefix; // may be null or empty

    public EnvConfigSource() { this(""); }
    public EnvConfigSource(String prefix) { this.prefix = prefix == null ? "" : prefix; }

    @Override public String name() { return "env" + (prefix.isEmpty() ? "" : (":" + prefix)); }

    @Override
    public Map<String, Object> load() {
        Map<String, String> env = System.getenv();
        Map<String, Object> root = new LinkedHashMap<>();
        for (var e : env.entrySet()) {
            String key = e.getKey();
            if (!prefix.isEmpty() && !key.startsWith(prefix)) continue;
            String trimmed = prefix.isEmpty() ? key : key.substring(prefix.length());
            if (trimmed.isEmpty()) continue;
            String path = trimmed.replace("__", ".").toLowerCase(Locale.ROOT);
            insert(root, path, e.getValue());
        }
        return root;
    }

    private static void insert(Map<String, Object> root, String path, String value) {
        String[] parts = path.split("\\.");
        Map<String, Object> cur = root;
        for (int i = 0; i < parts.length - 1; i++) {
            String p = parts[i];
            Object next = cur.get(p);
            if (!(next instanceof Map)) {
                Map<String, Object> m = new LinkedHashMap<>();
                cur.put(p, m);
                cur = m;
            } else {
                cur = (Map<String, Object>) next;
            }
        }
        cur.put(parts[parts.length - 1], value);
    }
}
