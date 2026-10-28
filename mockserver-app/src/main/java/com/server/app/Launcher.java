package com.server.app;

/**
 * @author Kazi Tanvir Azad
 */
public class Launcher {
    public static void main(String[] args) {
        // Adding custom shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            // executing shutdown processes
            // run gc
            Runtime.getRuntime().gc();
        }));
        MockServerApp.initiateLaunch(args);
    }
}
