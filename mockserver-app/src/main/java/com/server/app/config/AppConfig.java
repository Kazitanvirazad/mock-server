package com.server.app.config;

import com.server.app.model.data.Configuration;
import javafx.scene.image.Image;

import java.net.URL;
import java.util.Optional;
import java.util.Properties;

/**
 * @author Kazi Tanvir Azad
 */
public enum AppConfig {
    INSTANCE;
    private final Image appLogo;
    private final URL macDockAppLogo;
    private final Image splashScreenLogo;
    private final Configuration configuration;
    private Properties envProperties;

    public Image getAppLogo() {
        return appLogo;
    }

    public URL getMacDockAppLogo() {
        return macDockAppLogo;
    }

    public Image getSplashScreenLogo() {
        return splashScreenLogo;
    }

    public void setEnvProperties(Properties envProperties) {
        this.envProperties = envProperties;
    }

    public Configuration getConfiguration() {
        return configuration;
    }

    public Optional<String> getEnvProperty(String name) {
        return Optional.ofNullable(name)
                .map(key -> this.envProperties.getProperty(key))
                .map(String::trim);
    }

    {
        this.appLogo = new Image("/static/icons/mockface-40.png");
        this.macDockAppLogo = AppConfig.class.getResource("/static/icons/mockface-macos-dock-icon.png");
        this.splashScreenLogo = new Image("/static/icons/mockface-100.png");
        this.configuration = new Configuration();
    }
}
