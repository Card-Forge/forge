package forge.gui.toolbox;

import forge.gui.toolbox.FSkin.SkinnedLabel;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Calendar;

/**
 * Digital clock label that displays current time
 *
 */
@SuppressWarnings("serial")
public class FDigitalClock extends SkinnedLabel {
    private static final Calendar now = Calendar.getInstance();
    private static final DateFormat timeFormatter = DateFormat.getTimeInstance(DateFormat.SHORT);
    private static final ArrayList<FDigitalClock> clocks = new ArrayList<FDigitalClock>();
    private static Timer timer;
    private static String currentTimeDisplay;

    public FDigitalClock() {
        clocks.add(this);
        if (timer == null) {
            timer = new Timer(60000, new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    now.add(Calendar.MINUTE, 1);
                    updateTimeDisplay();
                }
            });
            updateTimeDisplay();
            //ensure timer starts when current minute ends
            timer.setInitialDelay(60000 - (now.get(Calendar.MILLISECOND) + now.get(Calendar.SECOND) * 1000));
            timer.start();
        }
        else {
            setText(currentTimeDisplay);
        }
    }

    private static void updateTimeDisplay() {
        currentTimeDisplay = timeFormatter.format(now.getTime());
        for (FDigitalClock clock : clocks) {
            clock.setText(currentTimeDisplay);
        }
    }
}
