package forge.quest.gui;

import javax.swing.JPanel;

/**
 * <p>
 * Abstract QuestAbstractPanel class.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 */
public abstract class QuestAbstractPanel extends JPanel {
    /** Constant <code>serialVersionUID=-6378675010346615367L</code>. */
    private static final long serialVersionUID = -6378675010346615367L;

    /** The main frame. */
    private QuestFrame mainFrame;

    /**
     * <p>
     * Constructor for QuestAbstractPanel.
     * </p>
     * 
     * @param mainFrame
     *            a {@link forge.quest.gui.QuestFrame} object.
     */
    protected QuestAbstractPanel(final QuestFrame mainFrame) {
        this.setMainFrame(mainFrame);
    }

    /**
     * <p>
     * refreshState.
     * </p>
     */
    public abstract void refreshState();

    /**
     * @return the mainFrame
     */
    public QuestFrame getMainFrame() {
        return mainFrame;
    }

    /**
     * @param mainFrame the mainFrame to set
     */
    public void setMainFrame(QuestFrame mainFrame) {
        this.mainFrame = mainFrame; // TODO: Add 0 to parameter's name.
    }
}
