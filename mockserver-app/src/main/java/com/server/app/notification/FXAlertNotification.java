package com.server.app.notification;

import com.server.app.config.AppConfig;
import com.server.core.notification.Notification;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.image.ImageView;

import java.util.Optional;

/**
 * @author Kazi Tanvir Azad
 */
public class FXAlertNotification implements Notification {

    /**
     * Opens a JavaFX Alert window with Information {@link Alert.AlertType}<br>
     *
     * @param headerText  {@link String} Text to display in the header section of the Alert stage
     * @param contentText {@link String} Text to display in the context section of the Alert stage
     */
    @Override
    public void triggerInfoNotification(String headerText, String contentText) {
        initializeAlert(headerText, contentText, Alert.AlertType.INFORMATION);
    }

    /**
     * Opens a JavaFX Alert window with Error {@link Alert.AlertType}<br>
     *
     * @param headerText  {@link String} Text to display in the header section of the Alert stage
     * @param contentText {@link String} Text to display in the context section of the Alert stage
     */
    @Override
    public void triggerErrorNotification(String headerText, String contentText) {
        initializeAlert(headerText, contentText, Alert.AlertType.ERROR);
    }

    /**
     * Opens a JavaFX Alert Confirmation window for prompting user to choose one from OK and Cancel button<br>
     *
     * @param headerText  {@link String} Text to display in the header section of the Alert stage
     * @param contentText {@link String} Text to display in the context section of the Alert stage
     * @return The {@link ButtonType} selected by the user
     */
    @Override
    public boolean triggerConfirmationPrompt(String headerText, String contentText) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setHeaderText(headerText);
        alert.setContentText(contentText);
        alert.setGraphic(new ImageView(AppConfig.INSTANCE.getAppLogo()));
        alert.setResizable(false);
        alert.setTitle("Choose to confirm");
        Optional<ButtonType> result = alert.showAndWait();
        return result.map(buttonType -> buttonType.equals(ButtonType.OK)).orElse(false);
    }

    /**
     * Opens a JavaFX Alert window with provided {@link Alert.AlertType}<br>
     *
     * @param headerText  {@link String} Text to display in the header section of the Alert stage
     * @param contentText {@link String} Text to display in the context section of the Alert stage
     * @param alertType   The {@link Alert.AlertType} to be used
     */
    private void initializeAlert(String headerText, String contentText, Alert.AlertType alertType) {
        Alert alert = new Alert(alertType);
        alert.setHeaderText(headerText);
        alert.setContentText(contentText);
        alert.setGraphic(new ImageView(AppConfig.INSTANCE.getAppLogo()));
        alert.setResizable(false);
        switch (alertType) {
            case Alert.AlertType.WARNING -> alert.setTitle("Warning");
            case Alert.AlertType.ERROR -> alert.setTitle("Error");
            case Alert.AlertType.INFORMATION -> alert.setTitle("Info");
        }
        alert.show();
    }
}
