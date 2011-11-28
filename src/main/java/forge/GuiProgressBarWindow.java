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

import java.awt.Dimension;

import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.border.EmptyBorder;

/**
 * <p>
 * Gui_ProgressBarWindow class.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 * @since 1.0.15
 */
public class GuiProgressBarWindow extends JDialog {

    /**
     *
     */
    private static final long serialVersionUID = 5832740611050396643L;
    private final JPanel contentPanel = new JPanel();
    private final JProgressBar progressBar = new JProgressBar();

    /**
     * Create the dialog.
     */
    public GuiProgressBarWindow() {
        this.setResizable(false);
        this.setTitle("Some Progress");
        final Dimension screen = this.getToolkit().getScreenSize();
        this.setBounds(screen.width / 3, 100, // position
                450, 84); // size
        this.getContentPane().setLayout(null);
        this.contentPanel.setBounds(0, 0, 442, 58);
        this.contentPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
        this.getContentPane().add(this.contentPanel);
        this.contentPanel.setLayout(null);
        this.progressBar.setValue(50);
        // progressBar.setBackground(Color.GRAY);
        // progressBar.setForeground(Color.BLUE);
        this.progressBar.setBounds(12, 12, 418, 32);
        this.contentPanel.add(this.progressBar);
    }

    /**
     * <p>
     * setProgressRange.
     * </p>
     * 
     * @param min
     *            a int.
     * @param max
     *            a int.
     */
    public final void setProgressRange(final int min, final int max) {
        this.progressBar.setMinimum(min);
        this.progressBar.setMaximum(max);
    }

    /**
     * <p>
     * increment.
     * </p>
     */
    public final void increment() {
        this.progressBar.setValue(this.progressBar.getValue() + 1);

        if ((this.progressBar.getValue() % 10) == 0) {
            this.contentPanel.paintImmediately(this.progressBar.getBounds());
        }
    }

    /**
     * Get the progressBar for fine tuning (e.g., adding text).
     * 
     * @return the progressBar
     */
    public final JProgressBar getProgressBar() {
        return this.progressBar;
    }

    /**
     * Set the progress back to zero units completed.
     * 
     * It is not certain whether this method actually works the way it was
     * intended.
     */
    public final void reset() {
        this.getProgressBar().setValue(0);
        this.contentPanel.paintImmediately(this.getProgressBar().getBounds());
    }
}
