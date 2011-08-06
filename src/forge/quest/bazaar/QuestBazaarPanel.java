package forge.quest.bazaar;

import forge.quest.QuestMainFrame;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;

public class QuestBazaarPanel extends JPanel{

    QuestMainFrame mainFrame;
    List stallList = new ArrayList();

    public QuestBazaarPanel(QuestMainFrame mainFrame) {
        this.mainFrame = mainFrame;
        this.setLayout(new BoxLayout(this,BoxLayout.Y_AXIS));
        
    }
}
