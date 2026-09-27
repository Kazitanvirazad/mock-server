package com.server.app.util;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.scene.Scene;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.stage.Stage;
import javafx.stage.Window;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AppUtilTest {
    @Test
    void getApplicationWindowByTitleReturnsMatchingWindow() {
        Stage stage = mock(Stage.class);
        ObservableList<Window> windows = FXCollections.observableArrayList(stage);
        when(stage.isShowing()).thenReturn(true);
        when(stage.getTitle()).thenReturn("Mock Server Settings");

        try (MockedStatic<Window> windowMock = mockStatic(Window.class)) {
            windowMock.when(Window::getWindows).thenReturn(windows);

            Window actual = AppUtil.getApplicationWindowByTitle("Settings");

            assertSame(stage, actual);
        }
    }

    @Test
    void bringExistingActiveWindowToFrontOrElseBringsWindowForward() {
        Stage stage = mock(Stage.class);
        ObservableList<Window> windows = FXCollections.observableArrayList(stage);
        when(stage.isShowing()).thenReturn(true);
        when(stage.getTitle()).thenReturn("Settings");

        try (MockedStatic<Window> windowMock = mockStatic(Window.class)) {
            windowMock.when(Window::getWindows).thenReturn(windows);

            AppUtil.bringExistingActiveWindowToFrontOrElse(() -> {
            }, "Settings");

            verify(stage).toFront();
        }
    }

    @Test
    void bringExistingActiveWindowToFrontOrElseRunsFallbackWhenMissing() {
        ObservableList<Window> windows = FXCollections.observableArrayList();
        AtomicBoolean called = new AtomicBoolean(false);

        try (MockedStatic<Window> windowMock = mockStatic(Window.class)) {
            windowMock.when(Window::getWindows).thenReturn(windows);

            AppUtil.bringExistingActiveWindowToFrontOrElse(() -> called.set(true), "Settings");

            assertTrue(called.get());
        }
    }

    @Test
    void closeWindowButtonEventClosesWindowFromEventSource() {
        Event event = mock(Event.class);
        ImageView node = mock(ImageView.class);
        Scene scene = mock(Scene.class);
        Stage stage = mock(Stage.class);
        when(event.getSource()).thenReturn(node);
        when(node.getScene()).thenReturn(scene);
        when(scene.getWindow()).thenReturn(stage);

        AppUtil.closeWindowButtonEvent(event);

        verify(stage).close();
    }

    @Test
    void closeWindowWithTitleClosesMatchingStages() {
        Stage stage = mock(Stage.class);
        Stage otherStage = mock(Stage.class);
        ObservableList<Window> windows = FXCollections.observableArrayList(stage, otherStage);
        when(stage.getTitle()).thenReturn("Settings");
        when(otherStage.getTitle()).thenReturn("Other");

        try (MockedStatic<Window> windowMock = mockStatic(Window.class)) {
            windowMock.when(Window::getWindows).thenReturn(windows);

            AppUtil.closeWindowWithTitle("Settings");

            verify(stage).close();
        }
    }

    @Test
    void exitApplicationClosesAllWindowsAndCallsPlatformExit() {
        Stage first = mock(Stage.class);
        Stage second = mock(Stage.class);
        ObservableList<Window> windows = FXCollections.observableArrayList(first, second);
        doAnswer(invocation -> {
            windows.remove(second);
            return null;
        }).when(second).close();
        doAnswer(invocation -> {
            windows.remove(first);
            return null;
        }).when(first).close();

        try (MockedStatic<Window> windowMock = mockStatic(Window.class);
             MockedStatic<Platform> platformMock = mockStatic(Platform.class)) {
            windowMock.when(Window::getWindows).thenReturn(windows);

            AppUtil.exitApplication();

            verify(second).close();
            verify(first).close();
            platformMock.verify(Platform::exit);
        }
    }

    @Test
    void setCloseWindowOnEscapeButtonPressClosesStageForEscapeKey() {
        Stage stage = mock(Stage.class);
        Scene scene = mock(Scene.class);
        KeyEvent keyEvent = mock(KeyEvent.class);
        when(keyEvent.getCode()).thenReturn(KeyCode.ESCAPE);
        ArgumentCaptor<EventHandler<KeyEvent>> captor = ArgumentCaptor.forClass(EventHandler.class);

        AppUtil.setCloseWindowOnEscapeButtonPress(stage, scene);

        verify(scene).setOnKeyPressed(captor.capture());
        captor.getValue().handle(keyEvent);
        verify(stage).close();
    }

    @Test
    void setCloseWindowOnEscapeButtonPressIgnoresOtherKeys() {
        Stage stage = mock(Stage.class);
        Scene scene = mock(Scene.class);
        KeyEvent keyEvent = mock(KeyEvent.class);
        when(keyEvent.getCode()).thenReturn(KeyCode.ENTER);
        ArgumentCaptor<EventHandler<KeyEvent>> captor = ArgumentCaptor.forClass(EventHandler.class);

        AppUtil.setCloseWindowOnEscapeButtonPress(stage, scene);

        verify(scene).setOnKeyPressed(captor.capture());
        captor.getValue().handle(keyEvent);
        verify(stage, never()).close();
    }

    @Test
    void setCloseWindowOnEscapeButtonPressDoesNothingWhenArgumentsAreNull() {
        Scene scene = mock(Scene.class);

        AppUtil.setCloseWindowOnEscapeButtonPress(null, scene);
        AppUtil.setCloseWindowOnEscapeButtonPress(mock(Stage.class), null);

        verify(scene, never()).setOnKeyPressed(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void loadEnvironmentPropertiesLoadsClasspathValues() {
        Properties properties = AppUtil.loadEnvironmentProperties();

        assertEquals("1.2", properties.getProperty("app.version"));
    }
}
