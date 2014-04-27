package forge.toolbox;

import java.awt.event.MouseEvent;

import forge.interfaces.ITriggerEvent;

//MouseEvent wrapper used for passing trigger to input classes
public class MouseTriggerEvent implements ITriggerEvent {
    private final MouseEvent event;

    public MouseTriggerEvent(MouseEvent event0) {
        event = event0;
    }

    @Override
    public int getButton() {
        return event.getButton();
    }
}
