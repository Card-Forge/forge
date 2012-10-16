package forge.gui.toolbox;

import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JLabel;
import javax.swing.SwingConstants;

import forge.Command;
import forge.gui.framework.ILocalRepaint;

/**
 * 
 * TODO: Write javadoc for this type.
 *
 */
@SuppressWarnings("serial")
public class ExperimentalLabel extends JLabel implements ILocalRepaint {

    private final Color clrMain = FSkin.getColor(FSkin.Colors.CLR_INACTIVE);
    private final Color d50 = FSkin.stepColor(clrMain, -50);
    private final Color d30 = FSkin.stepColor(clrMain, -30);
    private final Color d20 = FSkin.stepColor(clrMain, -20);
    private final Color l00 = FSkin.stepColor(clrMain, 0);
    private final Color l10 = FSkin.stepColor(clrMain, 10);
    private final Color l20 = FSkin.stepColor(clrMain, 20);
    private final Color l30 = FSkin.stepColor(clrMain, 30);
    private final Color l40 = FSkin.stepColor(clrMain, 40);

    private int w = 0;
    private int h = 0;

    private Command cmdClick;

    private boolean hover = false;
    private boolean down = false;

    /**
     * @param str0 String
     */
    public ExperimentalLabel(String str0) {
        super();
        this.setFont(FSkin.getFont(16));
        this.setForeground(FSkin.getColor(FSkin.Colors.CLR_TEXT));
        //this.setHorizontalTextPosition(SwingConstants.CENTER);
        this.setHorizontalAlignment(SwingConstants.CENTER);
        this.setText(str0);

        this.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(final MouseEvent e) {
                hover = true;
                repaintSelf();
            }

            @Override
            public void mouseExited(final MouseEvent e) {
                hover = false;
                repaintSelf();
            }

            @Override
            public void mousePressed(final MouseEvent e) {
                down = true;
                repaintSelf();
            }

            @Override
            public void mouseReleased(final MouseEvent e) {
                down = false;
                repaintSelf();
                if (cmdClick != null) { cmdClick.execute(); }
            }
        });
    }

    /** @return {@link forge.Command} */
    public Command getCommand() {
        return this.cmdClick;
    }

    /** @param c0 &emsp; {@link forge.Command} on click */
    public void setCommand(final Command c0) {
        this.cmdClick = c0;
    }

    @Override
    public void repaintSelf() {
        repaint(0, 0, getWidth(), getHeight());
    }

    @Override
    public void paintComponent(Graphics g) {
        w = getWidth();
        h = getHeight();

        if (down) { paintDown((Graphics2D) g); }
        else if (hover) { paintHover((Graphics2D) g); }
        else { paintUp((Graphics2D) g); }

        super.paintComponent(g);
    }

    private void paintHover(final Graphics2D g) {
        GradientPaint gradient = new GradientPaint(0, h, l00, 0, 0, l40);
        g.setPaint(gradient);
        g.fillRect(0, 0, getWidth(), h);

        g.setColor(l10);
        g.drawRect(0, 0, w - 2, h - 2);
        g.setColor(l30);
        g.drawRect(1, 1, w - 4, h - 4);
    }

    private void paintUp(final Graphics2D g) {
        GradientPaint gradient = new GradientPaint(0, h, d20, 0, 0, l20);
        g.setPaint(gradient);
        g.fillRect(0, 0, w, h);

        g.setColor(d50);
        g.drawRect(0, 0, w - 2, h - 2);
        g.setColor(l10);
        g.drawRect(1, 1, w - 4, h - 4);
    }

    private void paintDown(final Graphics2D g) {
        GradientPaint gradient = new GradientPaint(0, h, d30, 0, 0, l10);
        g.setPaint(gradient);
        g.fillRect(0, 0, w, h);

        g.setColor(d30);
        g.drawRect(0, 0, w - 2, h - 2);
        g.setColor(l10);
        g.drawRect(1, 1, w - 4, h - 4);
    }
}
