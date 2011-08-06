package forge.quest.gui;

import javax.swing.*;

public abstract class QuestAbstractPanel extends JPanel {
	private static final long serialVersionUID = -6378675010346615367L;
	
	public QuestFrame mainFrame;

    protected QuestAbstractPanel(QuestFrame mainFrame) {
        this.mainFrame = mainFrame;
    }

    public abstract void refreshState();
}
