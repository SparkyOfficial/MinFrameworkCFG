package com.sparky.config.source;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;

public interface ConfigSource {
    String name();
    Map<String, Object> load() throws IOException;
    default Optional<Path> watchPath() { return Optional.empty(); }
}
