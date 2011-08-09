package forge.quest.gui.bazaar;

import forge.AllZone;
import forge.gui.GuiUtils;
import forge.properties.NewConstants;
import forge.quest.data.QuestData;
import forge.quest.data.bazaar.QuestStallDefinition;
import forge.quest.data.bazaar.QuestStallManager;
import forge.quest.data.bazaar.QuestStallPurchasable;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.ArrayList;

/**
 * <p>QuestBazaarStall class.</p>
 *
 * @author Forge
 * @version $Id$
 */
public class QuestBazaarStall extends JPanel implements NewConstants {
    /** Constant <code>serialVersionUID=-4147745071116906043L</code> */
    private static final long serialVersionUID = -4147745071116906043L;
    String name;
    String stallName;
    String fluff;
    ImageIcon icon;


    private JLabel creditLabel = new JLabel();
    private JPanel inventoryPanel = new JPanel();

    protected QuestData questData = AllZone.getQuestData();

    /**
     * <p>Constructor for QuestBazaarStall.</p>
     *
     * @param name a {@link java.lang.String} object.
     * @param stallName a {@link java.lang.String} object.
     * @param iconName a {@link java.lang.String} object.
     * @param fluff a {@link java.lang.String} object.
     */
    protected QuestBazaarStall(String name, String stallName, String iconName, String fluff) {
        this.name = name;
        this.fluff = fluff;
        this.icon = GuiUtils.getIconFromFile(iconName);
        this.stallName = stallName;

        initUI();

    }

    /**
     * <p>Constructor for QuestBazaarStall.</p>
     *
     * @param definition a {@link forge.quest.data.bazaar.QuestStallDefinition} object.
     */
    protected QuestBazaarStall(QuestStallDefinition definition) {
        this.fluff = definition.fluff;
        this.icon = GuiUtils.getIconFromFile(definition.iconName);
        this.stallName = definition.displayName;
        this.name = definition.name;
        initUI();
    }

    /**
     * <p>initUI.</p>
     */
    private void initUI() {
        this.removeAll();

        JLabel stallNameLabel;

        GridBagLayout layout = new GridBagLayout();
        this.setLayout(layout);

        stallNameLabel = new JLabel(stallName);
        stallNameLabel.setFont(new Font("sserif", Font.BOLD, 22));
        stallNameLabel.setHorizontalAlignment(SwingConstants.CENTER);

        creditLabel.setText("Credits: " + questData.getCredits());
        creditLabel.setFont(new Font("sserif", 0, 14));

        JTextArea fluffArea = new JTextArea(fluff);
        fluffArea.setFont(new Font("sserif", Font.ITALIC, 14));
        fluffArea.setLineWrap(true);
        fluffArea.setWrapStyleWord(true);
        fluffArea.setOpaque(false);
        fluffArea.setEditable(false);
        fluffArea.setFocusable(false);
        fluffArea.setPreferredSize(new Dimension(fluffArea.getPreferredSize().width, 40));
        GridBagConstraints constraints = new GridBagConstraints(0,
                0,
                1,
                1,
                1,
                0,
                GridBagConstraints.CENTER,
                GridBagConstraints.NONE,
                new Insets(2, 2, 2, 2),
                0,
                0);
        layout.setConstraints(stallNameLabel, constraints);
        this.add(stallNameLabel);

        constraints.gridy = 1;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        layout.setConstraints(fluffArea, constraints);
        this.add(fluffArea);

        constraints.gridy = 2;
        layout.setConstraints(creditLabel, constraints);
        this.add(creditLabel);

        constraints.gridy = 3;
        constraints.anchor = GridBagConstraints.NORTHWEST;
        constraints.fill = GridBagConstraints.BOTH;
        constraints.insets = new Insets(10, 5, 10, 5);
        constraints.weighty = 1;
        constraints.weightx = GridBagConstraints.REMAINDER;

        populateInventory(populateItems());

        JScrollPane scrollPane = new JScrollPane(inventoryPanel);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setBorder(new EmptyBorder(0, 0, 0, 0));
        layout.setConstraints(scrollPane, constraints);
        this.add(scrollPane);

        this.setBorder(new EmptyBorder(0, 5, 0, 0));
    }

    /**
     * <p>populateInventory.</p>
     *
     * @param stallItems a {@link java.util.List} object.
     */
    private void populateInventory(java.util.List<QuestBazaarItem> stallItems) {
        inventoryPanel.removeAll();

        GridBagLayout innerLayout = new GridBagLayout();
        inventoryPanel.setLayout(innerLayout);
        GridBagConstraints innerConstraints =
                new GridBagConstraints(0,
                        0,
                        1,
                        1,
                        1,
                        0,
                        GridBagConstraints.NORTHWEST,
                        GridBagConstraints.HORIZONTAL,
                        new Insets(2, 2, 2, 2),
                        0,
                        0);

        JLabel purchaseLabel = new JLabel();

        if (stallItems.size() == 0) {

            purchaseLabel.setText("The merchant does not have anything useful for sale");
            inventoryPanel.add(purchaseLabel);
            innerConstraints.gridy++;
        } else {

            innerConstraints.insets = new Insets(5, 20, 5, 5);
            for (QuestBazaarItem item : stallItems) {
                JPanel itemPanel = item.getItemPanel();

                innerLayout.setConstraints(itemPanel, innerConstraints);
                inventoryPanel.add(itemPanel);
                innerConstraints.gridy++;
            }
        }
        innerConstraints.weighty = 1;
        JLabel fillLabel = new JLabel();

        innerLayout.setConstraints(fillLabel, innerConstraints);
        inventoryPanel.add(fillLabel);
    }

    /**
     * <p>populateItems.</p>
     *
     * @return a {@link java.util.List} object.
     */
    protected java.util.List<QuestBazaarItem> populateItems() {
        java.util.List<QuestBazaarItem> ret = new ArrayList<QuestBazaarItem>();
        java.util.List<QuestStallPurchasable> purchasables = QuestStallManager.getItems(name);

        for (QuestStallPurchasable purchasable : purchasables) {
            ret.add(new QuestBazaarItem(purchasable));
        }

        return ret;
    }


    /**
     * <p>getStallIcon.</p>
     *
     * @return a {@link javax.swing.ImageIcon} object.
     */
    public ImageIcon getStallIcon() {
        return icon;
    }

    /**
     * <p>Getter for the field <code>stallName</code>.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String getStallName() {
        return stallName;
    }

    /**
     * <p>updateItems.</p>
     */
    public void updateItems() {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                populateInventory(populateItems());
                creditLabel.setText("Credits: " + questData.getCredits());
                inventoryPanel.invalidate();
                inventoryPanel.repaint();
            }
        });
    }
}
