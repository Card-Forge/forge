/*
 * Forge: Play Magic: the Gathering.
 * Copyright (C) 2011  Forge Team
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package forge;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;

/**
 * ExternalPanel.java
 *
 * Created on 13.08.2009
 */

/**
 * The class ExternalPanel. A panel with which some other component can be shown
 * in an external window.
 * 
 * @author Clemens Koza
 * @version V0.0 13.08.2009
 */
public class ExternalPanel extends JPanel {

    /** Constant <code>serialVersionUID=9098962430872706173L</code>. */
    private static final long serialVersionUID = 9098962430872706173L;
    private final Component child, head;
    private JFrame frame;

    /**
     * <p>
     * Constructor for ExternalPanel.
     * </p>
     * 
     * @param child
     *            a {@link java.awt.Component} object.
     */
    public ExternalPanel(final Component child) {
        this(child, BorderLayout.EAST);
    }

    /**
     * <p>
     * Constructor for ExternalPanel.
     * </p>
     * 
     * @param child
     *            a {@link java.awt.Component} object.
     * @param side
     *            a {@link java.lang.String} object.
     */
    public ExternalPanel(final Component child, final String side) {
        super(new BorderLayout());
        this.child = child;
        this.add(this.child);
        final JButton b = new JButton();
        b.setPreferredSize(new Dimension(6, 6));
        b.setToolTipText("Click to move component into an extra Window");
        b.addActionListener(new ExternListener());
        this.head = b;
        this.setHeadSide(side);
    }

    /**
     * <p>
     * setHeadSide.
     * </p>
     * 
     * @param side
     *            a {@link java.lang.String} object.
     */
    public final void setHeadSide(final String side) {
        this.remove(this.head);
        this.add(this.head, side);
    }

    /** {@inheritDoc} */
    @Override
    protected final void addImpl(final Component comp, final Object constraints, final int index) {
        if ((comp != this.child) && (comp != this.head)) {
            throw new IllegalArgumentException();
        }
        super.addImpl(comp, constraints, index);
    }

    private final class ExternListener extends WindowAdapter implements ActionListener {
        private void bringOut() {
            ExternalPanel.this.frame = new JFrame();
            ExternalPanel.this.frame.addWindowListener(this);
            ExternalPanel.this.frame.addWindowStateListener(this);
            ExternalPanel.this.frame.add(ExternalPanel.this.child);
            ExternalPanel.this.frame.getRootPane().setPreferredSize(ExternalPanel.this.child.getSize());
            ExternalPanel.this.frame.pack();
            ExternalPanel.this.frame.setVisible(true);
            ExternalPanel.this.validate();
            ExternalPanel.this.repaint();
        }

        private void bringIn() {
            ExternalPanel.this.add(ExternalPanel.this.child);
            ExternalPanel.this.frame.dispose();
            ExternalPanel.this.frame = null;
            ExternalPanel.this.validate();
            ExternalPanel.this.repaint();
        }

        @Override
        public void actionPerformed(final ActionEvent e) {
            if (ExternalPanel.this.frame == null) {
                this.bringOut();
            } else {
                this.bringIn();
            }
        }

        @Override
        public void windowClosing(final WindowEvent e) {
            this.bringIn();
        }
    }
}
