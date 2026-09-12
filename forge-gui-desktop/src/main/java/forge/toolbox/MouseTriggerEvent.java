package forge.toolbox;

import java.awt.event.MouseEvent;
import java.io.Serializable;

import forge.util.ITriggerEvent;

//MouseEvent wrapper used for passing trigger to input classes
public class MouseTriggerEvent implements ITriggerEvent, Serializable {
    private static final long serialVersionUID = -5440485066050000298L;

    private final int button, x, y;

    public MouseTriggerEvent(final MouseEvent event) {
        this.button = event.getButton();
        this.x = event.getX();
        this.y = event.getY();
    }

    public MouseTriggerEvent(final int button, final int x, final int y) {
        this.button = button;
        this.x = x;
        this.y = y;
    }

    @Override
    public int getButton() {
        return button;
    }

    @Override
    public int getX() {
        return x;
    }

    @Override
    public int getY() {
        return y;
    }
}
