package com.server.app.notification;

import com.server.app.support.JavaFxToolkitSupport;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.image.ImageView;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FXAlertNotificationTest {
    @BeforeAll
    static void startJavaFxToolkit() {
        JavaFxToolkitSupport.ensureStarted();
    }

    @AfterAll
    static void exitJavaFXToolKit() {
        JavaFxToolkitSupport.exitApplication();
    }

    @Test
    void triggerInfoNotificationShowsInformationAlert() {
        try (MockedConstruction<Alert> alertConstruction = mockConstruction(Alert.class);
             MockedConstruction<ImageView> imageViewConstruction = mockConstruction(ImageView.class)) {
            new FXAlertNotification().triggerInfoNotification("Saved", "All good");

            Alert alert = alertConstruction.constructed().get(0);
            verify(alert).setHeaderText("Saved");
            verify(alert).setContentText("All good");
            verify(alert).setGraphic(any(ImageView.class));
            verify(alert).setTitle("Info");
            verify(alert).show();
        }
    }

    @Test
    void triggerErrorNotificationShowsErrorAlert() {
        try (MockedConstruction<Alert> alertConstruction = mockConstruction(Alert.class);
             MockedConstruction<ImageView> imageViewConstruction = mockConstruction(ImageView.class)) {
            new FXAlertNotification().triggerErrorNotification("Failed", "Please retry");

            Alert alert = alertConstruction.constructed().get(0);
            verify(alert).setTitle("Error");
            verify(alert).show();
        }
    }

    @Test
    void triggerConfirmationPromptReturnsTrueForOkSelection() {
        try (MockedConstruction<Alert> alertConstruction = mockConstruction(Alert.class,
                (mock, context) -> when(mock.showAndWait()).thenReturn(Optional.of(ButtonType.OK)));
             MockedConstruction<ImageView> imageViewConstruction = mockConstruction(ImageView.class)) {
            boolean actual = new FXAlertNotification().triggerConfirmationPrompt("Confirm", "Continue?");

            assertTrue(actual);
        }
    }

    @Test
    void triggerConfirmationPromptReturnsFalseForMissingSelection() {
        try (MockedConstruction<Alert> alertConstruction = mockConstruction(Alert.class,
                (mock, context) -> when(mock.showAndWait()).thenReturn(Optional.empty()));
             MockedConstruction<ImageView> imageViewConstruction = mockConstruction(ImageView.class)) {
            boolean actual = new FXAlertNotification().triggerConfirmationPrompt("Confirm", "Continue?");

            assertFalse(actual);
        }
    }

    @Test
    void triggerConfirmationPromptReturnsFalseForCancelSelection() {
        try (MockedConstruction<Alert> alertConstruction = mockConstruction(Alert.class,
                (mock, context) -> when(mock.showAndWait()).thenReturn(Optional.of(ButtonType.CANCEL)));
             MockedConstruction<ImageView> imageViewConstruction = mockConstruction(ImageView.class)) {
            boolean actual = new FXAlertNotification().triggerConfirmationPrompt("Confirm", "Continue?");

            assertFalse(actual);
        }
    }

    @Test
    void initializeAlertSetsWarningTitle() throws Exception {
        try (MockedConstruction<Alert> alertConstruction = mockConstruction(Alert.class);
             MockedConstruction<ImageView> imageViewConstruction = mockConstruction(ImageView.class)) {
            java.lang.reflect.Method initializeAlert = FXAlertNotification.class.getDeclaredMethod(
                    "initializeAlert", String.class, String.class, Alert.AlertType.class);
            initializeAlert.setAccessible(true);

            initializeAlert.invoke(new FXAlertNotification(), "Warning", "Check this", Alert.AlertType.WARNING);

            Alert alert = alertConstruction.constructed().get(0);
            verify(alert).setTitle("Warning");
            verify(alert).show();
        }
    }
}
