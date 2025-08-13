package com.sparky.config.source;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Map;

public class JsonConfigSource extends FileConfigSource {
    private final ObjectMapper json = new ObjectMapper();

    public JsonConfigSource(Path path) { super("json:" + path.getFileName(), path); }

    @Override
    public Map<String, Object> load() throws IOException {
        if (!Files.exists(path)) return Collections.emptyMap();
        try (var in = Files.newInputStream(path)) {
            @SuppressWarnings("unchecked")
            Map<String, Object> map = json.readValue(in, Map.class);
            return map == null ? Collections.emptyMap() : map;
        }
    }
}
