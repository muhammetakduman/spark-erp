package com.electrician.tracker;

/**
 * Separate main entry point (does not extend Application) to avoid JavaFX
 * fat-jar classpath issues when launched via {@code java -jar}.
 */
public class Launcher {

    public static void main(String[] args) {
        ElectricianTrackerApp.main(args);
    }
}
