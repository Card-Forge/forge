package forge.quest;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class QuestMainFrame extends JFrame {
    JPanel mainPanel;

    public QuestMainFrame() throws HeadlessException {
        this.setTitle("Quest Mode");

        mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBorder(new EmptyBorder(2,2,2,2));
        mainPanel.add(new Gui_Quest(), BorderLayout.CENTER);


        this.getContentPane().setLayout(new BorderLayout());
        this.getContentPane().add(mainPanel, BorderLayout.CENTER);
        this.setPreferredSize(new Dimension(1024, 768));
        this.setMinimumSize(new Dimension(800, 600));
        this.setVisible(true);
    }
}
