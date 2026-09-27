package com.server.app;

import com.server.app.config.AppConfig;
import com.server.app.controller.MainAppController;
import com.server.app.controller.SplashScreenController;
import com.server.app.fxml.loader.MainStageLoader;
import com.server.app.fxml.loader.SplashScreenStageLoader;
import com.server.app.fxml.loader.StageLoader;
import com.server.app.notification.FXAlertNotification;
import com.server.app.service.AppService;
import com.server.app.service.ServerRestartService;
import com.server.app.service.SettingsService;
import com.server.core.config.CommonConfig;
import com.server.core.notification.Notification;
import com.server.core.server.ServerManager;
import com.server.core.service.ServerService;
import com.server.core.service.Service;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.Stage;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.PreparedStatement;
import java.util.List;
import java.util.Optional;

import static com.server.app.constants.AppConstants.SQL_DDL_APP_QUERY_FILE_PATH;
import static com.server.app.util.AppUtil.exitApplication;
import static com.server.app.util.AppUtil.loadEnvironmentProperties;
import static com.server.core.util.CommonUtil.removeLogTracer;
import static com.server.core.util.CommonUtil.setLogTracer;
import static com.server.core.util.DatabaseUtil.executeCreateQuery;
import static com.server.core.util.DatabaseUtil.readStartupSQLScript;

/**
 * @author Kazi Tanvir Azad
 */
public class MockServerApp extends Application {
    private static final Logger log = LogManager.getLogger(MockServerApp.class);
    private final ServerService serverService = Service.INSTANCE.getServerService();
    private final ServerRestartService serverRestartService = AppService.INSTANCE.getServerRestartService();
    private final SettingsService settingsService = AppService.INSTANCE.getSettingsService();

    @Override
    public void start(Stage primaryStage) {
        try {
            // Setting FX Notification implementation
            Notification notification = new FXAlertNotification();
            CommonConfig.INSTANCE.setNotification(notification);
            // Setting BouncyCastle Provider to java.security
            CommonConfig.INSTANCE.initBountyCastleProvider();
            // Setting tracer for logging
            setLogTracer();
            // Setting HostServices to Server
            AppService.INSTANCE.setHostServices(getHostServices());
            // Initializing Stage for splash screen and Main core window
            StageLoader<SplashScreenController> splashScreenStageLoader = new SplashScreenStageLoader();
            StageLoader<MainAppController> mainStageLoader = new MainStageLoader(primaryStage);
            // Loading splash screen during application startup
            splashScreenStageLoader.loadStage();

            // handling the Splash Screen Stage closure and Main App Stage loading
            Platform.runLater(() -> {
                // Loading environment properties
                AppConfig.INSTANCE.setEnvProperties(loadEnvironmentProperties());
                Stage splashScreenStage = splashScreenStageLoader.getStage();
                try {
                    // restart servers during core startup if configured in settings
                    if (AppConfig.INSTANCE.getConfiguration().isStartServerOnStartup()) {
                        serverRestartService.getAllServerRestartDataStream()
                                .map(serverService::getServerById)
                                .filter(Optional::isPresent)
                                .map(Optional::get)
                                .forEach(server -> ServerManager.INSTANCE.startServer(server, true));
                    }
                    // Keeping the Splash screen for 800 milliseconds
                    Thread.sleep(800);
                } catch (Exception exception) {
                    log.error(exception.getMessage());
                }
                // closing splash screen
                splashScreenStage.close();
                // Loading application main stage
                mainStageLoader.loadStage();
            });
        } catch (RuntimeException exception) {
            CommonConfig.INSTANCE.notification()
                    .triggerErrorNotification("Failed to load application!", """
                            Try opening the application again.
                            If the problem persists,
                            try reinstalling the application.""");
            log.error("Failed to load application!");
            log.error(exception.getMessage());
            exitApplication();
        }
    }

    @Override
    public void init() throws Exception {
        super.init();
        // Execute sql DDL queries during application initialization
        List<String> queries = readStartupSQLScript(SQL_DDL_APP_QUERY_FILE_PATH);
        for (String query : queries) {
            executeCreateQuery(connection -> {
                PreparedStatement preparedStatement = connection.prepareStatement(query);
                return preparedStatement.executeUpdate();
            });
        }
        // Initialize setting table row for first time core startup and sync configuration
        settingsService.initAndSyncSettings();
    }

    @Override
    public void stop() throws Exception {
        super.stop();
        // Stop all servers
        List<String> stoppedServerIds = ServerManager.INSTANCE.stopAllServers(true);
        // persist stopped ServerIds for restart on application startup
        serverRestartService.resetServerRestartData();
        if (CollectionUtils.isNotEmpty(stoppedServerIds)) {
            serverRestartService.putServerRestartData(stoppedServerIds);
        }
        // Save the settings in case not updated
        settingsService.updateConfig();
        // Remove the log tracer
        removeLogTracer();
    }

    public static void initiateLaunch(String[] args) {
        launch(args);
    }
}
