package com.server.app.support;

import javafx.application.Platform;

public final class JavaFxToolkitSupport {
    private static volatile boolean started;

    private JavaFxToolkitSupport() {
        throw new AssertionError("Initialization of this class is not allowed");
    }

    public static void ensureStarted() {
        if (started) {
            return;
        }
        synchronized (JavaFxToolkitSupport.class) {
            if (started) {
                return;
            }
            try {
                Platform.startup(() -> {
                });
            } catch (IllegalStateException ignored) {
                // Toolkit was already started by another test or the runtime.
            }
            started = true;
        }
    }

    public static void exitApplication() {
        try {
            Platform.exit();
        } catch (Exception ignore) {
        }
    }
}
