package com.sparky.config.source;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Map;

public class YamlConfigSource extends FileConfigSource {
    private final ObjectMapper yaml = new ObjectMapper(new YAMLFactory());

    public YamlConfigSource(Path path) { super("yaml:" + path.getFileName(), path); }

    @Override
    public Map<String, Object> load() throws IOException {
        if (!Files.exists(path)) return Collections.emptyMap();
        try (var in = Files.newInputStream(path)) {
            @SuppressWarnings("unchecked")
            Map<String, Object> map = yaml.readValue(in, Map.class);
            return map == null ? Collections.emptyMap() : map;
        }
    }
}
