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
package forge.gui.bazaar;

import java.awt.Component;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JPanel;
import javax.swing.JTextPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;

import net.miginfocom.swing.MigLayout;
import forge.AllZone;
import forge.control.bazaar.ControlStall;
import forge.gui.toolbox.FLabel;
import forge.gui.toolbox.FScrollPane;
import forge.gui.toolbox.FSkin;
import forge.quest.QuestController;
import forge.quest.bazaar.IQuestBazaarItem;
import forge.quest.bazaar.QuestStallDefinition;
import forge.quest.data.QuestAssets;
import forge.view.ViewBazaarUI;

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
    private final List<ViewItem> lstItemPanels;
    private QuestStallDefinition stall;

    /**
     * @param v0
     *            {@link forge.view.ViewBazaarUI}
     */
    public ViewStall(final ViewBazaarUI v0) {
        // Final/component inits
        this.lblStallName = new FLabel.Builder().text("").fontAlign(SwingConstants.CENTER).build();
        this.lblEmpty = new FLabel.Builder().text("The merchant does not have anything useful for sale.")
                .fontAlign(SwingConstants.CENTER).build();
        this.lblStats = new FLabel.Builder().fontAlign(SwingConstants.CENTER).fontSize(12).build();

        this.tpnFluff = new JTextPane();
        this.pnlInventory = new JPanel();
        this.scrInventory = new FScrollPane(this.pnlInventory);
        this.control = new ControlStall(this);
        this.parentView = v0;
        this.lstItemPanels = new ArrayList<ViewItem>();

        // Component styling
        this.setOpaque(false);

        this.tpnFluff.setOpaque(false);
        this.tpnFluff.setForeground(FSkin.getColor(FSkin.Colors.CLR_TEXT));
        this.tpnFluff.setFont(FSkin.getItalicFont(15));
        this.tpnFluff.setFocusable(false);
        this.tpnFluff.setEditable(false);
        this.tpnFluff.setBorder(null);

        final StyledDocument doc = this.tpnFluff.getStyledDocument();
        final SimpleAttributeSet center = new SimpleAttributeSet();
        StyleConstants.setAlignment(center, StyleConstants.ALIGN_CENTER);
        doc.setParagraphAttributes(0, doc.getLength(), center, false);

        this.pnlInventory.setOpaque(false);

        this.scrInventory.setBorder(null);
        this.scrInventory.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        this.scrInventory.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

        // Layout
        this.setLayout(new MigLayout("insets 0, gap 0, wrap, ay center"));
        this.add(this.lblStallName, "w 90%!, h 40px!, gap 5% 0 10px 0");
        this.add(this.tpnFluff, "w 90%!, h 20px!, gap 5% 0 10px 40px");
        this.add(this.lblStats, "w 90%!, h 18px!, gap 5% 0 0 10px");
        this.add(this.scrInventory, "w 95%!, h 70%!");

        this.pnlInventory.setLayout(new MigLayout("insets 0, gap 0, wrap, alignx center, hidemode 3"));
        this.pnlInventory.add(this.lblEmpty, "w 90%!, h 30px!, gap 5% 0 50px 50px");
    }

    /** @return {@link forge.gui.toolbox.FLabel} */
    public FLabel getLblStallName() {
        return this.lblStallName;
    }

    /** @return {@link javax.swing.JTextPane} */
    public JTextPane getTpnFluff() {
        return this.tpnFluff;
    }

    /** @return {@link forge.gui.toolbox.FLabel} */
    public FLabel getLblStats() {
        return this.lblStats;
    }

    /** @return {@link forge.gui.toolbox.FScrollPane} */
    public FScrollPane getScrInventory() {
        return this.scrInventory;
    }

    /** @return {@link forge.control.bazaar.ControlStall} */
    public ControlStall getController() {
        return this.control;
    }

    /**
     * @param q0
     *            &emsp; {@link forge.quest.bazaar.QuestStallDefinition}
     */
    public void setStall(final QuestStallDefinition q0) {
        this.stall = q0;
    }

    /**
     * Updates/hides pre-existing panels with new inventory list, and creates
     * new panels if necessary.
     */
    public void updateStall() {
        final QuestController qData = AllZone.getQuest();
        if (qData.getAssets() == null) {
            return;
        }

        final QuestAssets qS = qData.getAssets();
        this.lblStats.setText("Credits: " + qS.getCredits() + "         Life: " + qS.getLife(qData.getMode()));

        final List<IQuestBazaarItem> items = qData.getBazaar().getItems(qData, this.stall.getName());

        this.lblStallName.setText(this.stall.getDisplayName());
        this.tpnFluff.setText(this.stall.getFluff());

        // Hide all components
        for (final Component i : this.pnlInventory.getComponents()) {
            i.setVisible(false);
        }
        // No items available to purchase?
        if (items.size() == 0) {
            this.lblEmpty.setVisible(true);
            return;
        }

        for (int i = 0; i < items.size(); i++) {
            // Add panel instances to match length of list, if necessary
            if (this.lstItemPanels.size() == i) {
                final ViewItem pnlItem = new ViewItem();
                this.lstItemPanels.add(i, pnlItem);
                this.pnlInventory.add(pnlItem, "w 90%!, gap 5% 0 0 10px");
            }

            this.lstItemPanels.get(i).setItem(items.get(i));
            this.lstItemPanels.get(i).update();
            this.lstItemPanels.get(i).setVisible(true);
        }
    }

    /** @return {@link forge.view.ViewBazaarUI} */
    public ViewBazaarUI getParentView() {
        return this.parentView;
    }
}
