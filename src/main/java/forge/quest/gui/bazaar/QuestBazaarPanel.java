package forge.quest.gui.bazaar;

import forge.quest.data.bazaar.QuestStallManager;
import forge.quest.gui.QuestAbstractPanel;
import forge.quest.gui.QuestFrame;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * <p>QuestBazaarPanel class.</p>
 *
 * @author Forge
 * @version $Id$
 */
public class QuestBazaarPanel extends QuestAbstractPanel {
    /** Constant <code>serialVersionUID=1418913010051869222L</code> */
    private static final long serialVersionUID = 1418913010051869222L;

    /** Constant <code>stallList</code> */
    static List<QuestBazaarStall> stallList = new ArrayList<QuestBazaarStall>();

    JPanel buttonPanel = new JPanel(new BorderLayout());
    JPanel buttonPanelMain = new JPanel();

    JPanel stallPanel = new JPanel();

    JToggleButton selectedStall = null;

    CardLayout stallLayout = new CardLayout();

    /**
     * <p>Constructor for QuestBazaarPanel.</p>
     *
     * @param mainFrame a {@link forge.quest.gui.QuestFrame} object.
     */
    public QuestBazaarPanel(QuestFrame mainFrame) {
        super(mainFrame);
        this.setLayout(new BorderLayout());

        stallList = new ArrayList<QuestBazaarStall>();

        for (String stallName : QuestStallManager.getStallNames()) {
            stallList.add(new QuestBazaarStall(QuestStallManager.getStall(stallName)));
        }

        buttonPanelMain.setLayout(new GridLayout(stallList.size(), 1));

        stallPanel.setLayout(stallLayout);
        List<JToggleButton> buttonList = new LinkedList<JToggleButton>();

        double maxWidth = 0;
        double maxHeight = 0;

        for (QuestBazaarStall stall : stallList) {
            JToggleButton stallButton = new JToggleButton(stall.getStallName(), stall.getStallIcon());
            stallButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {

                    if (QuestBazaarPanel.this.selectedStall == e.getSource()) {
                        QuestBazaarPanel.this.selectedStall.setSelected(true);
                        return;
                    }

                    if (QuestBazaarPanel.this.selectedStall != null) {
                        QuestBazaarPanel.this.selectedStall.setSelected(false);
                    }

                    QuestBazaarPanel.this.showStall(((JToggleButton) e.getSource()).getText());
                    QuestBazaarPanel.this.selectedStall = (JToggleButton) e.getSource();
                }
            });

            Dimension preferredSize = stallButton.getPreferredSize();

            if (preferredSize.getWidth() > maxWidth) {
                maxWidth = preferredSize.getWidth();
            }

            if (preferredSize.getHeight() > maxHeight) {
                maxHeight = preferredSize.getHeight();
            }

            buttonList.add(stallButton);

            buttonPanelMain.add(stallButton);
            stallPanel.add(stall, stall.getStallName());
        }

        buttonList.get(0).setSelected(true);
        this.selectedStall = buttonList.get(0);

        Dimension max = new Dimension((int) maxWidth, (int) maxHeight);

        for (JToggleButton button : buttonList) {
            button.setMinimumSize(max);
        }


        buttonPanel.add(buttonPanelMain, BorderLayout.NORTH);
        JButton quitButton = new JButton("Leave Bazaar");
        quitButton.setSize(max);
        quitButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                QuestBazaarPanel.this.mainFrame.showMainPane();
            }
        });

        buttonPanel.add(quitButton, BorderLayout.SOUTH);

        this.add(buttonPanel, BorderLayout.WEST);
        this.add(stallPanel, BorderLayout.CENTER);

    }

    /**
     * <p>showStall.</p>
     *
     * @param source a {@link java.lang.String} object.
     */
    private void showStall(String source) {
        stallLayout.show(stallPanel, source);
    }

    /**
     * Slightly hackish, but should work.
     */
    static void refreshLastInstance() {
        for (QuestBazaarStall stall : stallList) {
            stall.updateItems();
        }
    }

    /** {@inheritDoc} */
    @Override
    public void refreshState() {
        refreshLastInstance();
    }
}
