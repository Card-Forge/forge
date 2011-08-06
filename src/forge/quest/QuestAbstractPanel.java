package forge.quest;

import javax.swing.*;

public abstract class QuestAbstractPanel extends JPanel {
    public QuestFrame mainFrame;

    protected QuestAbstractPanel(QuestFrame mainFrame) {
        this.mainFrame = mainFrame;
    }

    public abstract void refreshState();
}
