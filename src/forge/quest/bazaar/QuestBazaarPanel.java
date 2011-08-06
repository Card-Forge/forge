package forge.quest.bazaar;

import forge.quest.QuestMainFrame;

import javax.swing.*;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class QuestBazaarPanel extends JPanel{
	private static final long serialVersionUID = 1418913010051869222L;
	
	QuestMainFrame mainFrame;
    static List<QuestAbstractBazaarStall> stallList = new ArrayList<QuestAbstractBazaarStall>();

    JPanel buttonPanel = new JPanel(new BorderLayout());
    JPanel buttonPanelMain = new JPanel();

    JPanel stallPanel = new JPanel();

    JToggleButton selectedStall = null;

    CardLayout stallLayout = new CardLayout();

    public QuestBazaarPanel(QuestMainFrame mainFrame) {
        this.mainFrame = mainFrame;
        this.setLayout(new BorderLayout());

        stallList = new ArrayList<QuestAbstractBazaarStall>();
        stallList.add(new QuestAlchemistStall());
        stallList.add(new QuestBankerStall());
        stallList.add(new QuestBookStall());
        stallList.add(new QuestGearStall());
        stallList.add(new QuestNurseryStall());
        stallList.add(new QuestPetStall());

        buttonPanelMain.setLayout(new GridLayout(stallList.size(),1));
        
        stallPanel.setLayout(stallLayout);
        List<JToggleButton> buttonList = new LinkedList<JToggleButton>();

        double maxWidth=0;
        double maxHeight=0;

        for(QuestAbstractBazaarStall stall:stallList){
            JToggleButton stallButton = new JToggleButton(stall.getStallName(), stall.getIcon());
            stallButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {

                    if (QuestBazaarPanel.this.selectedStall == e.getSource())
                    {
                        QuestBazaarPanel.this.selectedStall.setSelected(true);
                        return;
                    }

                    if (QuestBazaarPanel.this.selectedStall != null){
                        QuestBazaarPanel.this.selectedStall.setSelected(false);
                    }

                    QuestBazaarPanel.this.showStall(((JToggleButton) e.getSource()).getText());
                    QuestBazaarPanel.this.selectedStall = (JToggleButton) e.getSource();
                }
            });

            Dimension preferredSize = stallButton.getPreferredSize();

            if (preferredSize.getWidth() > maxWidth){
                maxWidth = preferredSize.getWidth();
            }

            if (preferredSize.getHeight() > maxHeight){
                maxHeight = preferredSize.getHeight();
            }

            buttonList.add(stallButton);

            buttonPanelMain.add(stallButton);
            stallPanel.add(stall, stall.getStallName());
        }

        buttonList.get(0).setSelected(true);
        this.selectedStall = buttonList.get(0);

        Dimension max = new Dimension((int)maxWidth, (int)maxHeight);

        for (JToggleButton button : buttonList){
            button.setMinimumSize(max);
        }


        buttonPanel.add(buttonPanelMain, BorderLayout.NORTH);
        JButton quitButton = new JButton("Leave Bazaar");
        quitButton.setSize(max);
        quitButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                QuestBazaarPanel.this.mainFrame.showPane(QuestMainFrame.MAIN_PANEL);
            }
        });

        buttonPanel.add(quitButton, BorderLayout.SOUTH);

        this.add(buttonPanel, BorderLayout.WEST);
        this.add(stallPanel, BorderLayout.CENTER);

    }

    private void showStall(String source) {
        stallLayout.show(stallPanel, source);
    }

    /**
     * Slightly hackish, but should work.
     * @return The last created instance of this object, used for updates after purchases.
     */
    static void refreshLastInstance(){
        for (QuestAbstractBazaarStall stall: stallList){
            stall.updateItems();
        }
    }

}
