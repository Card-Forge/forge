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
    private JProgressBar progressBar = new JProgressBar();

    /**
     * Create the dialog.
     */
    public GuiProgressBarWindow() {
        setResizable(false);
        setTitle("Some Progress");
        Dimension screen = this.getToolkit().getScreenSize();
        setBounds(screen.width / 3, 100, // position
                450, 84); // size
        getContentPane().setLayout(null);
        contentPanel.setBounds(0, 0, 442, 58);
        contentPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
        getContentPane().add(contentPanel);
        contentPanel.setLayout(null);
        progressBar.setValue(50);
        // progressBar.setBackground(Color.GRAY);
        // progressBar.setForeground(Color.BLUE);
        progressBar.setBounds(12, 12, 418, 32);
        contentPanel.add(progressBar);
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
        progressBar.setMinimum(min);
        progressBar.setMaximum(max);
    }

    /**
     * <p>
     * increment.
     * </p>
     */
    public final void increment() {
        progressBar.setValue(progressBar.getValue() + 1);

        if (progressBar.getValue() % 10 == 0) {
            contentPanel.paintImmediately(progressBar.getBounds());
        }
    }

    /**
     * Get the progressBar for fine tuning (e.g., adding text).
     * 
     * @return the progressBar
     */
    public final JProgressBar getProgressBar() {
        return progressBar;
    }

    /**
     * Set the progress back to zero units completed.
     * 
     * It is not certain whether this method actually works the way it was
     * intended.
     */
    public final void reset() {
        getProgressBar().setValue(0);
        contentPanel.paintImmediately(getProgressBar().getBounds());
    }
}
