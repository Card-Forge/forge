package forge.adventure.util;

import com.badlogic.gdx.Input;
import com.badlogic.gdx.controllers.Controller;
import com.badlogic.gdx.controllers.ControllerMapping;
import com.badlogic.gdx.controllers.Controllers;
import forge.gui.GuiBase;

public enum KeyBinding {
    Left("Left", Input.Keys.LEFT,Input.Keys.DPAD_LEFT),
    Up("Up", Input.Keys.UP,Input.Keys.DPAD_UP),
    Right("Right", Input.Keys.RIGHT,Input.Keys.DPAD_RIGHT),
    Down("Down", Input.Keys.DOWN,Input.Keys.DPAD_DOWN),
    Menu("Menu", Input.Keys.ESCAPE,Input.Keys.BUTTON_START),
    Inventory("Inventory", Input.Keys.I,Input.Keys.BUTTON_X),
    Status("Status", Input.Keys.Q,Input.Keys.BUTTON_Y),
    Deck("Deck", Input.Keys.E,Input.Keys.BUTTON_A),
    Map("Map", Input.Keys.M,Input.Keys.BUTTON_SELECT),
    Equip("Equip", Input.Keys.E,Input.Keys.BUTTON_X),
    ExitToWorldMap("ExitToWorldMap", Input.Keys.F4,Input.Keys.BUTTON_L2),
    Bookmark("Bookmark", Input.Keys.B, Input.Keys.BUTTON_R2),
    Use("Use", Input.Keys.ENTER,Input.Keys.BUTTON_A),
    Back("Back", Input.Keys.ESCAPE,Input.Keys.BUTTON_B),
    ScrollUp("ScrollUp", Input.Keys.PAGE_UP,Input.Keys.BUTTON_L1),
    ScrollDown("ScrollDown", Input.Keys.PAGE_DOWN,Input.Keys.BUTTON_R1),
    ;
    String name;
    int binding;
    int defaultBinding;
    int bindingController;
    int defaultBindingController;

    KeyBinding(String name, int defaultBinding, int defaultBindingController)
    {
        this.name=name;
        this.defaultBinding=binding=defaultBinding;
        this.defaultBindingController=bindingController=defaultBindingController;
    }
    public boolean isPressed(int key)
    {
        return key==binding||key==bindingController;
    }

    static String controllerPrefix="XBox_";
    public String getLabelText(boolean pressed) {
        if(Controllers.getCurrent()!=null)
        {
            return "[%120][+"+controllerPrefix+Input.Keys.toString(bindingController).replace(" Button","")+(pressed?"_pressed]":"]");
        }
        else
        {
            if(GuiBase.isAndroid())
                return "";
            return "[%120][+"+Input.Keys.toString(binding)+(pressed?"_pressed]":"]");
        }

    }
    
    public static int controllerButtonToKey(Controller controller,int key)
    {
        ControllerMapping map=controller.getMapping();
        if(key==map.buttonA) return Input.Keys.BUTTON_A;
        if(key==map.buttonB) return Input.Keys.BUTTON_B;
        if(key==map.buttonX) return Input.Keys.BUTTON_X;
        if(key==map.buttonY) return Input.Keys.BUTTON_Y;

        if(key==map.buttonBack) return Input.Keys.BUTTON_SELECT;
        if(key==map.buttonStart) return Input.Keys.BUTTON_START;
        if(key==map.buttonL1) return Input.Keys.BUTTON_L1;
        if(key==map.buttonL2) return Input.Keys.BUTTON_L2;
        if(key==map.buttonR1) return Input.Keys.BUTTON_R1;
        if(key==map.buttonR2) return Input.Keys.BUTTON_R2;
        if(key==map.buttonDpadUp) return Input.Keys.DPAD_UP;
        if(key==map.buttonDpadDown) return Input.Keys.DPAD_DOWN;
        if(key==map.buttonDpadLeft) return Input.Keys.DPAD_LEFT;
        if(key==map.buttonDpadRight) return Input.Keys.DPAD_RIGHT;
        if(key==map.buttonLeftStick) return Input.Keys.BUTTON_THUMBL;
        if(key==map.buttonRightStick) return Input.Keys.BUTTON_THUMBR;

        if(key==map.buttonDpadUp) return Input.Keys.DPAD_UP;
        if(key==map.buttonDpadDown) return Input.Keys.DPAD_DOWN;
        if(key==map.buttonDpadLeft) return Input.Keys.DPAD_LEFT;
        if(key==map.buttonDpadRight) return Input.Keys.DPAD_RIGHT;
        return 0;
    }
}
