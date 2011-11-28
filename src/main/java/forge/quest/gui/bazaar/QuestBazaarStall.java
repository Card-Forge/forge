/*
 * Forge: Play Magic: the Gathering.
 * Copyright (C) 2011  Forge Team
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package forge.quest.gui.bazaar;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.ArrayList;

import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;

import forge.AllZone;
import forge.gui.GuiUtils;
import forge.quest.data.QuestData;
import forge.quest.data.bazaar.QuestStallDefinition;
import forge.quest.data.bazaar.QuestStallManager;
import forge.quest.data.bazaar.QuestStallPurchasable;

/**
 * <p>
 * QuestBazaarStall class.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 */
public class QuestBazaarStall extends JPanel {
    /** Constant <code>serialVersionUID=-4147745071116906043L</code>. */
    private static final long serialVersionUID = -4147745071116906043L;

    /** The name. */
    private final String name;

    /** The stall name. */
    private final String stallName;

    /** The fluff. */
    private final String fluff;

    /** The icon. */
    private final ImageIcon icon;

    private final JLabel creditLabel = new JLabel();
    private final JPanel inventoryPanel = new JPanel();

    /** The quest data. */
    private final QuestData questData = AllZone.getQuestData();

    /**
     * <p>
     * Constructor for QuestBazaarStall.
     * </p>
     * 
     * @param name
     *            a {@link java.lang.String} object.
     * @param stallName
     *            a {@link java.lang.String} object.
     * @param iconName
     *            a {@link java.lang.String} object.
     * @param fluff
     *            a {@link java.lang.String} object.
     */
    protected QuestBazaarStall(final String name, final String stallName, final String iconName, final String fluff) {
        this.name = name;
        this.fluff = fluff;
        this.icon = GuiUtils.getIconFromFile(iconName);
        this.stallName = stallName;

        this.initUI();

    }

    /**
     * <p>
     * Constructor for QuestBazaarStall.
     * </p>
     * 
     * @param definition
     *            a {@link forge.quest.data.bazaar.QuestStallDefinition} object.
     */
    protected QuestBazaarStall(final QuestStallDefinition definition) {
        this.fluff = definition.getFluff();
        this.icon = GuiUtils.getIconFromFile(definition.getIconName());
        this.stallName = definition.getDisplayName();
        this.name = definition.getName();
        this.initUI();
    }

    /**
     * <p>
     * initUI.
     * </p>
     */
    private void initUI() {
        this.removeAll();

        JLabel stallNameLabel;

        final GridBagLayout layout = new GridBagLayout();
        this.setLayout(layout);

        stallNameLabel = new JLabel(this.stallName);
        stallNameLabel.setFont(new Font("sserif", Font.BOLD, 22));
        stallNameLabel.setHorizontalAlignment(SwingConstants.CENTER);

        this.creditLabel.setText("Credits: " + this.questData.getCredits());
        this.creditLabel.setFont(new Font("sserif", 0, 14));

        final JTextArea fluffArea = new JTextArea(this.fluff);
        fluffArea.setFont(new Font("sserif", Font.ITALIC, 14));
        fluffArea.setLineWrap(true);
        fluffArea.setWrapStyleWord(true);
        fluffArea.setOpaque(false);
        fluffArea.setEditable(false);
        fluffArea.setFocusable(false);
        fluffArea.setPreferredSize(new Dimension(fluffArea.getPreferredSize().width, 40));
        final GridBagConstraints constraints = new GridBagConstraints(0, 0, 1, 1, 1, 0, GridBagConstraints.CENTER,
                GridBagConstraints.NONE, new Insets(2, 2, 2, 2), 0, 0);
        layout.setConstraints(stallNameLabel, constraints);
        this.add(stallNameLabel);

        constraints.gridy = 1;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        layout.setConstraints(fluffArea, constraints);
        this.add(fluffArea);

        constraints.gridy = 2;
        layout.setConstraints(this.creditLabel, constraints);
        this.add(this.creditLabel);

        constraints.gridy = 3;
        constraints.anchor = GridBagConstraints.NORTHWEST;
        constraints.fill = GridBagConstraints.BOTH;
        constraints.insets = new Insets(10, 5, 10, 5);
        constraints.weighty = 1;
        constraints.weightx = GridBagConstraints.REMAINDER;

        this.populateInventory(this.populateItems());

        final JScrollPane scrollPane = new JScrollPane(this.inventoryPanel);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setBorder(new EmptyBorder(0, 0, 0, 0));
        layout.setConstraints(scrollPane, constraints);
        this.add(scrollPane);

        this.setBorder(new EmptyBorder(0, 5, 0, 0));
    }

    /**
     * <p>
     * populateInventory.
     * </p>
     * 
     * @param stallItems
     *            a {@link java.util.List} object.
     */
    private void populateInventory(final java.util.List<QuestBazaarItem> stallItems) {
        this.inventoryPanel.removeAll();

        final GridBagLayout innerLayout = new GridBagLayout();
        this.inventoryPanel.setLayout(innerLayout);
        final GridBagConstraints innerConstraints = new GridBagConstraints(0, 0, 1, 1, 1, 0,
                GridBagConstraints.NORTHWEST, GridBagConstraints.HORIZONTAL, new Insets(2, 2, 2, 2), 0, 0);

        final JLabel purchaseLabel = new JLabel();

        if (stallItems.size() == 0) {

            purchaseLabel.setText("The merchant does not have anything useful for sale");
            this.inventoryPanel.add(purchaseLabel);
            innerConstraints.gridy++;
        } else {

            innerConstraints.insets = new Insets(5, 20, 5, 5);
            for (final QuestBazaarItem item : stallItems) {
                final JPanel itemPanel = item.getItemPanel();

                innerLayout.setConstraints(itemPanel, innerConstraints);
                this.inventoryPanel.add(itemPanel);
                innerConstraints.gridy++;
            }
        }
        innerConstraints.weighty = 1;
        final JLabel fillLabel = new JLabel();

        innerLayout.setConstraints(fillLabel, innerConstraints);
        this.inventoryPanel.add(fillLabel);
    }

    /**
     * <p>
     * populateItems.
     * </p>
     * 
     * @return a {@link java.util.List} object.
     */
    protected java.util.List<QuestBazaarItem> populateItems() {
        final java.util.List<QuestBazaarItem> ret = new ArrayList<QuestBazaarItem>();
        final java.util.List<QuestStallPurchasable> purchasables = QuestStallManager.getItems(this.name);

        for (final QuestStallPurchasable purchasable : purchasables) {
            ret.add(new QuestBazaarItem(purchasable));
        }

        return ret;
    }

    /**
     * <p>
     * getStallIcon.
     * </p>
     * 
     * @return a {@link javax.swing.ImageIcon} object.
     */
    public ImageIcon getStallIcon() {
        return this.icon;
    }

    /**
     * <p>
     * Getter for the field <code>stallName</code>.
     * </p>
     * 
     * @return a {@link java.lang.String} object.
     */
    public String getStallName() {
        return this.stallName;
    }

    /**
     * <p>
     * updateItems.
     * </p>
     */
    public void updateItems() {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                QuestBazaarStall.this.populateInventory(QuestBazaarStall.this.populateItems());
                QuestBazaarStall.this.creditLabel.setText("Credits: " + QuestBazaarStall.this.questData.getCredits());
                QuestBazaarStall.this.inventoryPanel.invalidate();
                QuestBazaarStall.this.inventoryPanel.repaint();
            }
        });
    }
}
