package com.sparky.config.source;

import java.nio.file.Path;
import java.util.Optional;

public abstract class FileConfigSource implements ConfigSource {
    protected final Path path;
    protected final String name;

    protected FileConfigSource(String name, Path path) {
        this.name = name;
        this.path = path;
    }

    @Override public String name() { return name; }
    @Override public Optional<Path> watchPath() { return Optional.of(path); }
}
