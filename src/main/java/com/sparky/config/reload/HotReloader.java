package com.sparky.config.reload;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.IOException;
import java.nio.file.*;
import java.time.Duration;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class HotReloader implements Closeable {
    private static final Logger log = LoggerFactory.getLogger(HotReloader.class);

    private final Set<Path> filesToWatch;
    private final Runnable onChange;
    private final ExecutorService exec = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "config-watcher");
        t.setDaemon(true);
        return t;
    });

    private volatile boolean running = false;

    public HotReloader(Set<Path> filesToWatch, Runnable onChange) {
        this.filesToWatch = filesToWatch;
        this.onChange = onChange;
    }

    public void start() {
        if (running) return;
        running = true;
        exec.submit(this::runLoop);
    }

    private void runLoop() {
        try (WatchService watcher = FileSystems.getDefault().newWatchService()) {
            for (Path file : filesToWatch) {
                Path dir = file.toAbsolutePath().getParent();
                if (dir == null) continue;
                dir.register(watcher, StandardWatchEventKinds.ENTRY_MODIFY, StandardWatchEventKinds.ENTRY_CREATE);
            }
            while (running) {
                WatchKey key = watcher.poll(500, TimeUnit.MILLISECONDS);
                if (key == null) continue;
                Path dir = (Path) key.watchable();
                boolean dirty = false;
                for (WatchEvent<?> event : key.pollEvents()) {
                    WatchEvent.Kind<?> kind = event.kind();
                    if (kind == StandardWatchEventKinds.OVERFLOW) continue;
                    Path changed = dir.resolve((Path) event.context());
                    for (Path f : filesToWatch) {
                        if (f.toAbsolutePath().equals(changed.toAbsolutePath())) { dirty = true; break; }
                    }
                }
                boolean valid = key.reset();
                if (!valid) log.debug("WatchKey invalidated for {}", dir);
                if (dirty) {
                    // simple debounce
                    try { Thread.sleep(Duration.ofMillis(150).toMillis()); } catch (InterruptedException ignored) {}
                    onChange.run();
                }
            }
        } catch (IOException | InterruptedException e) {
            log.warn("Watcher error", e);
        }
    }

    @Override
    public void close() {
        running = false;
        exec.shutdownNow();
    }
}
