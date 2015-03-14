package forge.toolbox;

import java.awt.event.MouseEvent;
import java.io.Serializable;

import forge.util.ITriggerEvent;

//MouseEvent wrapper used for passing trigger to input classes
public class MouseTriggerEvent implements ITriggerEvent, Serializable {
    private static final long serialVersionUID = -4746127117012991732L;

    private final MouseEvent event;

    public MouseTriggerEvent(MouseEvent event0) {
        event = event0;
    }

    @Override
    public int getButton() {
        return event.getButton();
    }

    public MouseEvent getMouseEvent() {
        return event;
    }
}
