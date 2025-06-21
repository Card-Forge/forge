package forge.adventure.util;

import com.badlogic.gdx.Input;
import com.badlogic.gdx.controllers.Controller;
import com.badlogic.gdx.controllers.ControllerMapping;
import com.badlogic.gdx.controllers.Controllers;
import forge.gui.GuiBase;

// The standard button has index 0, controller binding is 1. Others can be added if needed.
public enum KeyBinding {
    Left("Left", new int[]{Input.Keys.LEFT, Input.Keys.DPAD_LEFT, Input.Keys.A}),
    Up("Up", new int[]{Input.Keys.UP, Input.Keys.DPAD_UP, Input.Keys.W}),
    Right("Right", new int[]{Input.Keys.RIGHT, Input.Keys.DPAD_RIGHT, Input.Keys.D}),
    Down("Down", new int[]{Input.Keys.DOWN, Input.Keys.DPAD_DOWN, Input.Keys.S}),
    Menu("Menu", new int[]{Input.Keys.ESCAPE, Input.Keys.BUTTON_START}),
    Inventory("Inventory", new int[]{Input.Keys.I, Input.Keys.BUTTON_X}),
    Status("Status", new int[]{Input.Keys.Q, Input.Keys.BUTTON_Y}),
    Deck("Deck", new int[]{Input.Keys.E, Input.Keys.BUTTON_A}),
    Map("Map", new int[]{Input.Keys.M, Input.Keys.BUTTON_SELECT}),
    Equip("Equip", new int[]{Input.Keys.E, Input.Keys.BUTTON_X}),
    ExitToWorldMap("ExitToWorldMap", new int[]{Input.Keys.F4, Input.Keys.BUTTON_L2}),
    Bookmark("Bookmark", new int[]{Input.Keys.B, Input.Keys.BUTTON_R2}),
    Use("Use", new int[]{Input.Keys.ENTER, Input.Keys.BUTTON_A}),
    Back("Back", new int[]{Input.Keys.ESCAPE, Input.Keys.BUTTON_B, Input.Keys.BACK}),
    ScrollUp("ScrollUp", new int[]{Input.Keys.PAGE_UP, Input.Keys.BUTTON_L1}),
    ScrollDown("ScrollDown", new int[]{Input.Keys.PAGE_DOWN, Input.Keys.BUTTON_R1}),
    ;
    final String name;
    final int[] bindings;

    KeyBinding(String name, int[] bindings) {
        this.name = name;
        this.bindings = bindings;
    }

    public boolean isPressed(int key) {
        for (int i = 0; i < bindings.length; i++) {
            if (key == bindings[i]) {
                return true;
            }
        }
        return false;
    }

    // The controller binding always has index 1.
    final static String controllerPrefix = "XBox_";

    public String getLabelText(boolean pressed) {
        if (Controllers.getCurrent() != null) {
            return "[%120][+" + controllerPrefix + Input.Keys.toString(bindings[1]).replace(" Button", "") + (pressed ? "_pressed]" : "]");
        } else {
            if (GuiBase.isAndroid())
                return "";
            return "[%120][+" + Input.Keys.toString(bindings[0]) + (pressed ? "_pressed]" : "]");
        }

    }

    public static int controllerButtonToKey(Controller controller, int key) {
        ControllerMapping map = controller.getMapping();
        if (key == map.buttonA) return Input.Keys.BUTTON_A;
        if (key == map.buttonB) return Input.Keys.BUTTON_B;
        if (key == map.buttonX) return Input.Keys.BUTTON_X;
        if (key == map.buttonY) return Input.Keys.BUTTON_Y;

        if (key == map.buttonBack) return Input.Keys.BUTTON_SELECT;
        if (key == map.buttonStart) return Input.Keys.BUTTON_START;
        if (key == map.buttonL1) return Input.Keys.BUTTON_L1;
        if (key == map.buttonL2) return Input.Keys.BUTTON_L2;
        if (key == map.buttonR1) return Input.Keys.BUTTON_R1;
        if (key == map.buttonR2) return Input.Keys.BUTTON_R2;
        if (key == map.buttonDpadUp) return Input.Keys.DPAD_UP;
        if (key == map.buttonDpadDown) return Input.Keys.DPAD_DOWN;
        if (key == map.buttonDpadLeft) return Input.Keys.DPAD_LEFT;
        if (key == map.buttonDpadRight) return Input.Keys.DPAD_RIGHT;
        if (key == map.buttonLeftStick) return Input.Keys.BUTTON_THUMBL;
        if (key == map.buttonRightStick) return Input.Keys.BUTTON_THUMBR;

        return 0;
    }
}
