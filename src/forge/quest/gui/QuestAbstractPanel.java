package forge.quest.gui;

import javax.swing.*;

/**
 * <p>Abstract QuestAbstractPanel class.</p>
 *
 * @author Forge
 * @version $Id: $
 */
public abstract class QuestAbstractPanel extends JPanel {
    /** Constant <code>serialVersionUID=-6378675010346615367L</code> */
    private static final long serialVersionUID = -6378675010346615367L;

    public QuestFrame mainFrame;

    /**
     * <p>Constructor for QuestAbstractPanel.</p>
     *
     * @param mainFrame a {@link forge.quest.gui.QuestFrame} object.
     */
    protected QuestAbstractPanel(QuestFrame mainFrame) {
        this.mainFrame = mainFrame;
    }

    /**
     * <p>refreshState.</p>
     */
    public abstract void refreshState();
}
