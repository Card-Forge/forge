package forge.gui.toolbox;

import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.JCheckBox;

import forge.Command;

/** 
 * A custom instance of JCheckBox using Forge skin properties.
 */
@SuppressWarnings("serial")
public class FCheckBox extends JCheckBox implements ItemListener, MouseListener {
    private Command cmd;
    /** */
    public FCheckBox() {
        this("");
    }

    /** @param s0 &emsp; {@link java.lang.String} */
    public FCheckBox(final String s0) {
        super(s0);
        this.setForeground(FSkin.getColor(FSkin.Colors.CLR_TEXT));
        this.setBackground(FSkin.getColor(FSkin.Colors.CLR_HOVER));
        this.setFont(FSkin.getFont(14));
        this.setOpaque(false);
        this.addMouseListener(this);
        this.addItemListener(this);
    }

    /** @param cmd0 &emsp; {@link forge.Command} */
    public void setCommand(final Command cmd0) {
        this.cmd = cmd0;
    }

    @Override
    public void mouseEntered(final MouseEvent e) {
        setOpaque(true);
    }

    @Override
    public void mouseExited(final MouseEvent e) {
        setOpaque(false);
    }

    @Override
    public void itemStateChanged(final ItemEvent e) {
        if (cmd != null) { cmd.execute(); }
    }

    @Override public void mouseClicked(MouseEvent arg0) { }
    @Override public void mousePressed(MouseEvent arg0) { }
    @Override public void mouseReleased(MouseEvent arg0) { }
}
