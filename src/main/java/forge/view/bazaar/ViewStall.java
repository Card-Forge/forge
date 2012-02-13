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
package forge.view.bazaar;

import java.awt.Component;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.SwingConstants;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;

import net.miginfocom.swing.MigLayout;
import forge.AllZone;
import forge.control.bazaar.ControlStall;
import forge.quest.data.bazaar.QuestStallDefinition;
import forge.quest.data.bazaar.QuestStallManager;
import forge.quest.data.bazaar.QuestStallPurchasable;
import forge.view.ViewBazaarUI;
import forge.view.toolbox.FLabel;
import forge.view.toolbox.FScrollPane;
import forge.view.toolbox.FSkin;

/**
 * <p>
 * ViewStall class.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 */
@SuppressWarnings("serial")
public class ViewStall extends JPanel {
    private final FLabel lblStallName, lblEmpty, lblStats;
    private final JTextPane tpnFluff;
    private final JPanel pnlInventory;
    private final FScrollPane scrInventory;
    private final ControlStall control;
    private final ViewBazaarUI parentView;
    private List<ViewItem> lstItemPanels;
    private QuestStallDefinition stall;

    /** @param v0 {@link forge.view.ViewBazaarUI} */
    public ViewStall(final ViewBazaarUI v0) {
        // Final/component inits
        this.lblStallName = new FLabel.Builder().text("").fontAlign(SwingConstants.CENTER).build();
        this.lblEmpty = new FLabel.Builder()
            .text("The merchant does not have anything useful for sale.")
            .fontAlign(SwingConstants.CENTER).build();
        this.lblStats = new FLabel.Builder().fontAlign(SwingConstants.CENTER)
                .fontScaleFactor(0.9).build();

        this.tpnFluff = new JTextPane();
        this.pnlInventory = new JPanel();
        this.scrInventory = new FScrollPane(pnlInventory);
        this.control = new ControlStall(this);
        this.parentView = v0;
        this.lstItemPanels = new ArrayList<ViewItem>();

        // Component styling
        this.setOpaque(false);

        tpnFluff.setOpaque(false);
        tpnFluff.setForeground(FSkin.getColor(FSkin.Colors.CLR_TEXT));
        tpnFluff.setFont(FSkin.getItalicFont(15));
        tpnFluff.setFocusable(false);
        tpnFluff.setEditable(false);
        tpnFluff.setBorder(null);

        StyledDocument doc = tpnFluff.getStyledDocument();
        SimpleAttributeSet center = new SimpleAttributeSet();
        StyleConstants.setAlignment(center, StyleConstants.ALIGN_CENTER);
        doc.setParagraphAttributes(0, doc.getLength(), center, false);

        pnlInventory.setOpaque(false);

        scrInventory.setBorder(null);
        scrInventory.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrInventory.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

        // Layout
        this.setLayout(new MigLayout("insets 0, gap 0, wrap, ay center"));
        this.add(lblStallName, "w 90%!, h 40px!, gap 5% 0 10px 0");
        this.add(tpnFluff, "w 90%!, h 20px!, gap 5% 0 10px 40px");
        this.add(lblStats, "w 90%!, h 18px!, gap 5% 0 0 10px");
        this.add(scrInventory, "w 95%!, h 70%!");

        pnlInventory.setLayout(new MigLayout("insets 0, gap 0, wrap, alignx center, hidemode 3"));
        pnlInventory.add(lblEmpty, "w 90%!, h 30px!, gap 5% 0 50px 50px");
    }

    /** @return {@link forge.view.toolbox.FLabel} */
    public FLabel getLblStallName() {
        return lblStallName;
    }

    /** @return {@link javax.swing.JTextPane} */
    public JTextPane getTpnFluff() {
        return tpnFluff;
    }

    /** @return {@link forge.view.toolbox.FLabel} */
    public FLabel getLblStats() {
        return lblStats;
    }

    /** @return {@link forge.view.toolbox.FScrollPane} */
    public FScrollPane getScrInventory() {
        return scrInventory;
    }

    /** @return {@link forge.control.bazaar.ControlStall} */
    public ControlStall getController() {
        return control;
    }

    /** @param q0 &emsp; {@link forge.quest.data.bazaar.QuestStallDefinition} */
    public void setStall(QuestStallDefinition q0) {
        this.stall = q0;
    }

    /**
     * Updates/hides pre-existing panels with new inventory list,
     * and creates new panels if necessary.
     */
    public void updateStall() {
        this.lblStats.setText(
                "Credits: " + AllZone.getQuestData().getCredits()
                + "         Life: " + AllZone.getQuestData().getLife());

        final List<QuestStallPurchasable> items =
                QuestStallManager.getItems(stall.getName());

        lblStallName.setText(stall.getDisplayName());
        tpnFluff.setText(stall.getFluff());

        // Hide all components
        for (Component i : this.pnlInventory.getComponents()) { i.setVisible(false); }
        // No items available to purchase?
        if (items.size() == 0) { lblEmpty.setVisible(true); return; }

        for (int i = 0; i < items.size(); i++) {
            // Add panel instances to match length of list, if necessary
            if (this.lstItemPanels.size() == i) {
                final ViewItem pnlItem = new ViewItem();
                lstItemPanels.add(i, pnlItem);
                pnlInventory.add(pnlItem, "w 90%!, gap 5% 0 0 10px");
            }

            lstItemPanels.get(i).setItem(items.get(i));
            lstItemPanels.get(i).update();
            lstItemPanels.get(i).setVisible(true);
        }
    }

    /** @return {@link forge.view.ViewBazaarUI} */
    public ViewBazaarUI getParentView() {
        return this.parentView;
    }
}
