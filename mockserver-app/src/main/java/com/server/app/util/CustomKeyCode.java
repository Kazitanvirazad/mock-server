package com.server.app.util;

import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import org.apache.commons.lang3.SystemUtils;

/**
 * @author Kazi Tanvir Azad
 * @apiNote Enumeration for Singleton {@link KeyCode} for perform or handle various keyboard key press events
 */
public enum CustomKeyCode {
    INSTANCE;
    private final KeyCodeCombination COPY_KEYCODE_COMBINATION;
    private final KeyCode ESCAPE_KEYCODE;
    private final KeyCode ENTER_KEYCODE;
    private final KeyCode DELETE_KEYCODE;

    public KeyCodeCombination getCopyKeycodeCombination() {
        return COPY_KEYCODE_COMBINATION;
    }

    public KeyCode getEscapeKeycode() {
        return ESCAPE_KEYCODE;
    }

    public KeyCode getEnterKeycode() {
        return ENTER_KEYCODE;
    }

    public KeyCode getDeleteKeycode() {
        return DELETE_KEYCODE;
    }

    {
        this.COPY_KEYCODE_COMBINATION =
                SystemUtils.IS_OS_MAC ? new KeyCodeCombination(KeyCode.C, KeyCombination.META_ANY)
                        : new KeyCodeCombination(KeyCode.C, KeyCombination.CONTROL_ANY);
        this.ESCAPE_KEYCODE = KeyCode.ESCAPE;
        this.ENTER_KEYCODE = KeyCode.ENTER;
        this.DELETE_KEYCODE = SystemUtils.IS_OS_MAC ? KeyCode.BACK_SPACE : KeyCode.DELETE;
    }
}
