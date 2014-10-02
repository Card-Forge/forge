package forge.control;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.Timer;

import forge.interfaces.IGuiTimer;

@SuppressWarnings("serial")
public class GuiTimer extends Timer implements IGuiTimer {
    public GuiTimer(final Runnable proc0, int interval0) {
        super(interval0, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                proc0.run();
            }
        });
    }

    @Override
    public void setInterval(int interval0) {
        setDelay(interval0);
    }
}
