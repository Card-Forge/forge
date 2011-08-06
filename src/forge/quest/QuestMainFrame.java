package forge.quest;

import forge.quest.bazaar.QuestBazaarPanel;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class QuestMainFrame extends JFrame {
	private static final long serialVersionUID = -2832625381531838412L;
	
	JPanel visiblePanel;
    CardLayout questLayout;

    public static final String MAIN_PANEL = "Main";
    public static final String BAZAAR_PANEL = "Bazaar";

    public QuestMainFrame() throws HeadlessException {
        this.setTitle("Quest Mode");

        visiblePanel = new JPanel(new BorderLayout());
        visiblePanel.setBorder(new EmptyBorder(2,2,2,2));
        questLayout = new CardLayout();
        visiblePanel.setLayout(questLayout);
        visiblePanel.add(new QuestMainPanel(this), MAIN_PANEL);
        visiblePanel.add(new QuestBazaarPanel(this), BAZAAR_PANEL);


        this.getContentPane().setLayout(new BorderLayout());
        this.getContentPane().add(visiblePanel, BorderLayout.CENTER);
        this.setPreferredSize(new Dimension(1024, 768));
        this.setMinimumSize(new Dimension(800, 600));

        questLayout.show(visiblePanel, MAIN_PANEL);

        this.pack();
        this.setVisible(true);
    }

    public void showPane(String paneName){
        questLayout.show(visiblePanel, paneName);
    }
}
