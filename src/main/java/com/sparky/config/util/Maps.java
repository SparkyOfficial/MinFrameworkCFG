package com.sparky.config.util;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.*;

public final class Maps {
    private Maps() {}

    @SuppressWarnings("unchecked")
    public static Map<String, Object> deepMerge(Map<String, Object> base, Map<String, Object> add) {
        Map<String, Object> out = new LinkedHashMap<>(base);
        for (var e : add.entrySet()) {
            String k = e.getKey();
            Object v = e.getValue();
            if (v instanceof Map && out.get(k) instanceof Map) {
                Map<String, Object> merged = deepMerge((Map<String, Object>) out.get(k), (Map<String, Object>) v);
                out.put(k, merged);
            } else {
                out.put(k, v);
            }
        }
        return out;
    }

    @SuppressWarnings("unchecked")
    public static Object getByPath(Map<String, Object> root, String path) {
        if (path == null || path.isEmpty()) return root;
        String[] parts = path.split("\\.");
        Object cur = root;
        for (String p : parts) {
            if (!(cur instanceof Map)) return null;
            cur = ((Map<String, Object>) cur).get(p);
            if (cur == null) return null;
        }
        return cur;
    }

    public static <T> T coerce(Object value, Class<T> type, ObjectMapper mapper) {
        if (value == null) return null;
        if (type.isInstance(value)) return type.cast(value);
        // common primitive conversions
        if (type == String.class) return type.cast(String.valueOf(value));
        try {
            return mapper.convertValue(value, type);
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Cannot convert value '" + value + "' to " + type.getSimpleName(), ex);
        }
    }
}
