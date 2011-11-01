package forge.quest.gui.bazaar;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JToggleButton;

import forge.quest.data.bazaar.QuestStallManager;
import forge.quest.gui.QuestAbstractPanel;
import forge.quest.gui.QuestFrame;

/**
 * <p>
 * QuestBazaarPanel class.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 */
public class QuestBazaarPanel extends QuestAbstractPanel {
    /** Constant <code>serialVersionUID=1418913010051869222L</code>. */
    private static final long serialVersionUID = 1418913010051869222L;

    /** Constant <code>stallList</code>. */
    private static List<QuestBazaarStall> stallList = new ArrayList<QuestBazaarStall>();

    /** The button panel. */
    private final JPanel buttonPanel = new JPanel(new BorderLayout());

    /** The button panel main. */
    private final JPanel buttonPanelMain = new JPanel();

    /** The stall panel. */
    private final JPanel stallPanel = new JPanel();

    /** The selected stall. */
    private JToggleButton selectedStall = null;

    /** The stall layout. */
    private final CardLayout stallLayout = new CardLayout();

    /**
     * <p>
     * Constructor for QuestBazaarPanel.
     * </p>
     * 
     * @param mainFrame
     *            a {@link forge.quest.gui.QuestFrame} object.
     */
    public QuestBazaarPanel(final QuestFrame mainFrame) {
        super(mainFrame);
        this.setLayout(new BorderLayout());

        QuestBazaarPanel.stallList = new ArrayList<QuestBazaarStall>();

        for (final String stallName : QuestStallManager.getStallNames()) {
            QuestBazaarPanel.stallList.add(new QuestBazaarStall(QuestStallManager.getStall(stallName)));
        }

        this.buttonPanelMain.setLayout(new GridLayout(QuestBazaarPanel.stallList.size(), 1));

        this.stallPanel.setLayout(this.stallLayout);
        final List<JToggleButton> buttonList = new LinkedList<JToggleButton>();

        double maxWidth = 0;
        double maxHeight = 0;

        for (final QuestBazaarStall stall : QuestBazaarPanel.stallList) {
            final JToggleButton stallButton = new JToggleButton(stall.getStallName(), stall.getStallIcon());
            stallButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(final ActionEvent e) {

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

            final Dimension preferredSize = stallButton.getPreferredSize();

            if (preferredSize.getWidth() > maxWidth) {
                maxWidth = preferredSize.getWidth();
            }

            if (preferredSize.getHeight() > maxHeight) {
                maxHeight = preferredSize.getHeight();
            }

            buttonList.add(stallButton);

            this.buttonPanelMain.add(stallButton);
            this.stallPanel.add(stall, stall.getStallName());
        }

        buttonList.get(0).setSelected(true);
        this.selectedStall = buttonList.get(0);

        final Dimension max = new Dimension((int) maxWidth, (int) maxHeight);

        for (final JToggleButton button : buttonList) {
            button.setMinimumSize(max);
        }

        this.buttonPanel.add(this.buttonPanelMain, BorderLayout.NORTH);
        final JButton quitButton = new JButton("Leave Bazaar");
        quitButton.setSize(max);
        quitButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                QuestBazaarPanel.this.getMainFrame().showMainPane();
            }
        });

        this.buttonPanel.add(quitButton, BorderLayout.SOUTH);

        this.add(this.buttonPanel, BorderLayout.WEST);
        this.add(this.stallPanel, BorderLayout.CENTER);

    }

    /**
     * <p>
     * showStall.
     * </p>
     * 
     * @param source
     *            a {@link java.lang.String} object.
     */
    private void showStall(final String source) {
        this.stallLayout.show(this.stallPanel, source);
    }

    /**
     * Slightly hackish, but should work.
     */
    static void refreshLastInstance() {
        for (final QuestBazaarStall stall : QuestBazaarPanel.stallList) {
            stall.updateItems();
        }
    }

    /** {@inheritDoc} */
    @Override
    public final void refreshState() {
        QuestBazaarPanel.refreshLastInstance();
    }
}
