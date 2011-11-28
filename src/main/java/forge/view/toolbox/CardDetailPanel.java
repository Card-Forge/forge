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
package forge.view.toolbox;

import java.awt.Color;
import java.awt.Font;
import java.util.Iterator;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;

import net.miginfocom.swing.MigLayout;
import forge.AllZone;
import forge.Card;
import forge.CardContainer;
import forge.Constant.Zone;
import forge.Counters;
import forge.GameEntity;
import forge.GuiDisplayUtil;

/** Shows details of card if moused over. */
@SuppressWarnings("serial")
public class CardDetailPanel extends JPanel implements CardContainer {
    private Card card = null;
    private final JLabel lblCardName, lblCardCost, lblCardType, lblCardID, lblCardPT, lblCardSet, lblCardDmg1,
            lblCardDmg2;
    private final JTextArea tarInfo;
    private static final Color PURPLE = new Color(14381203);

    private final FSkin skin;
    private final Font f;
    private final Color foreground;
    private final Color zebra;

    /**
     * Instantiates a new card detail panel.
     */
    public CardDetailPanel() {
        super();
        this.setLayout(new MigLayout("wrap, insets 0, gap 0"));

        // Styles used in DetailLabel class
        this.skin = AllZone.getSkin();
        this.f = this.skin.getFont1().deriveFont(Font.PLAIN, 11);
        this.foreground = this.skin.getClrText();
        this.zebra = this.skin.getClrZebra();

        // DetailLabel instances (true = zebra stripe)
        this.lblCardName = new DetailLabel();
        this.lblCardCost = new DetailLabel();
        this.lblCardType = new DetailLabel();
        this.lblCardPT = new DetailLabel();
        this.lblCardDmg1 = new DetailLabel();
        this.lblCardDmg2 = new DetailLabel();
        this.lblCardID = new DetailLabel();
        this.lblCardSet = new DetailLabel();

        // Info text area
        this.tarInfo = new JTextArea();
        this.tarInfo.setFont(this.f);
        this.tarInfo.setLineWrap(true);
        this.tarInfo.setWrapStyleWord(true);
        this.tarInfo.setFocusable(false);

        this.add(this.lblCardName, "w 100%!");
        this.add(this.lblCardCost, "w 100%!");
        this.add(this.lblCardType, "w 100%!");
        this.add(this.lblCardPT, "w 100%!");
        this.add(this.lblCardDmg1, "w 100%!");
        this.add(this.lblCardDmg2, "w 100%!");
        this.add(this.lblCardID, "w 100%!");
        this.add(this.lblCardSet, "w 100%!");
        this.add(new JScrollPane(this.tarInfo, ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS,
                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER), "w 100%!, h 45%!, south");
        this.setCard(this.card);
    }

    /**
     * A fairly long method testing conditions determining which details to
     * show.
     * 
     * @param c
     *            &emsp; Card obj
     */
    @Override
    public void setCard(final Card c) {
        this.lblCardName.setText(" ");
        this.lblCardCost.setText(" ");
        this.lblCardType.setText(" ");
        this.lblCardPT.setText(" ");
        this.lblCardDmg1.setText(" ");
        this.lblCardDmg2.setText(" ");
        this.lblCardID.setText(" ");
        this.lblCardSet.setText(" ");
        this.lblCardSet.setOpaque(true);
        this.lblCardSet.setBorder(null);
        this.tarInfo.setText(" ");

        this.card = c;
        if (this.card == null) {
            return;
        }

        final boolean faceDown = this.card.isFaceDown() && (this.card.getController() != AllZone.getHumanPlayer());

        if (!faceDown) {
            this.lblCardName.setText(this.card.getName());
            if (!this.card.getManaCost().equals("") && !this.card.isLand()) {
                this.lblCardCost.setText(this.card.getManaCost());
            }
        } else {
            this.lblCardName.setText("Morph");
        }

        if (!faceDown) {
            this.lblCardType.setText(GuiDisplayUtil.formatCardType(this.card));
        } else {
            this.lblCardType.setText("Creature");
        }

        if (this.card.isCreature()) {
            this.lblCardPT.setText(this.card.getNetAttack() + " / " + this.card.getNetDefense());
            this.lblCardDmg1.setText("Damage: " + this.card.getDamage());
            this.lblCardDmg2.setText("Assigned Damage: " + this.card.getTotalAssignedDamage());
        }
        if (this.card.isPlaneswalker()) {
            this.lblCardDmg2.setText("Assigned Damage: " + this.card.getTotalAssignedDamage());
        }

        this.lblCardID.setText("Card ID  " + this.card.getUniqueNumber());

        // Rarity and set of a face down card should not be visible to the
        // opponent
        if (!this.card.isFaceDown() || this.card.getController().isHuman()) {
            final String s = this.card.getCurSetCode();
            if (s.equals("")) {
                this.lblCardSet.setText("---");
            } else {
                this.lblCardSet.setText(s);
            }
        }

        final String csr = this.card.getCurSetRarity();
        if (csr.equals("Common") || csr.equals("Land")) {
            this.lblCardSet.setBackground(Color.BLACK);
            this.lblCardSet.setForeground(Color.WHITE);
            // lblCardSet.setBorder(BorderFactory.createLineBorder(Color.WHITE));
        } else if (csr.equals("Uncommon")) {
            this.lblCardSet.setBackground(Color.LIGHT_GRAY);
            this.lblCardSet.setForeground(Color.BLACK);
            // lblCardSet.setBorder(BorderFactory.createLineBorder(Color.BLACK));
        } else if (csr.equals("Rare")) {
            this.lblCardSet.setBackground(Color.YELLOW);
            this.lblCardSet.setForeground(Color.BLACK);
            // lblCardSet.setBorder(BorderFactory.createLineBorder(Color.BLACK));
        } else if (csr.equals("Mythic")) {
            this.lblCardSet.setBackground(Color.RED);
            this.lblCardSet.setForeground(Color.BLACK);
            // lblCardSet.setBorder(BorderFactory.createLineBorder(Color.BLACK));
        } else if (csr.equals("Special")) {
            // "Timeshifted" or other Special Rarity Cards
            this.lblCardSet.setBackground(CardDetailPanel.PURPLE);
            this.lblCardSet.setForeground(Color.BLACK);
            // lblCardSet.setBorder(BorderFactory.createLineBorder(Color.BLACK));
        }

        // Fill the card text
        final StringBuilder str = new StringBuilder();

        // Token
        if (this.card.isToken()) {
            str.append("Token");
        }

        if (!faceDown) {
            if (str.length() != 0) {
                str.append("\n");
            }
            String text = this.card.getText();

            // LEVEL [0-9]+-[0-9]+
            // LEVEL [0-9]+\+
            String regex = "LEVEL [0-9]+-[0-9]+ ";
            text = text.replaceAll(regex, "$0\r\n");

            regex = "LEVEL [0-9]+\\+ ";
            text = text.replaceAll(regex, "\r\n$0\r\n");

            // Displays keywords that have dots in them a little better:
            regex = "\\., ";
            text = text.replaceAll(regex, ".\r\n");

            str.append(text);
        }

        // Phasing
        if (this.card.isPhasedOut()) {
            if (str.length() != 0) {
                str.append("\n");
            }
            str.append("Phased Out");
        }

        // Counter text
        final Counters[] counters = Counters.values();
        for (final Counters counter : counters) {
            if (this.card.getCounters(counter) != 0) {
                if (str.length() != 0) {
                    str.append("\n");
                }
                str.append(counter.getName() + " counters: ");
                str.append(this.card.getCounters(counter));
            }
        }

        // Regeneration Shields
        final int regenShields = this.card.getShield();
        if (regenShields > 0) {
            if (str.length() != 0) {
                str.append("\n");
            }
            str.append("Regeneration Shield(s): ").append(regenShields);
        }

        // Damage Prevention
        final int preventNextDamage = this.card.getPreventNextDamage();
        if (preventNextDamage > 0) {
            str.append("\n");
            str.append("Prevent the next ").append(preventNextDamage).append(" damage that would be dealt to ");
            str.append(this.card.getName()).append(" it this turn.");
        }

        // Top revealed
        if (this.card.hasKeyword("Play with the top card of your library revealed.")
                && (this.card.getController() != null) && !this.card.getController().getZone(Zone.Library).isEmpty()) {
            str.append("\r\nTop card: ");
            str.append(this.card.getController().getCardsIn(Zone.Library, 1));
        }

        // Chosen type
        if (this.card.getChosenType() != "") {
            if (str.length() != 0) {
                str.append("\n");
            }
            str.append("(chosen type: ");
            str.append(this.card.getChosenType());
            str.append(")");
        }

        // Chosen color
        if (!this.card.getChosenColor().isEmpty()) {
            if (str.length() != 0) {
                str.append("\n");
            }
            str.append("(chosen colors: ");
            str.append(this.card.getChosenColor());
            str.append(")");
        }

        // Named card
        if (this.card.getNamedCard() != "") {
            if (str.length() != 0) {
                str.append("\n");
            }
            str.append("(named card: ");
            str.append(this.card.getNamedCard());
            str.append(")");
        }

        // Equipping
        if (this.card.getEquipping().size() > 0) {
            if (str.length() != 0) {
                str.append("\n");
            }
            str.append("=Equipping ");
            str.append(this.card.getEquipping().get(0));
            str.append("=");
        }

        // Equipped by
        if (this.card.getEquippedBy().size() > 0) {
            if (str.length() != 0) {
                str.append("\n");
            }
            str.append("=Equipped by ");
            for (final Iterator<Card> it = this.card.getEquippedBy().iterator(); it.hasNext();) {
                str.append(it.next());
                if (it.hasNext()) {
                    str.append(", ");
                }
            }
            str.append("=");
        }

        // Enchanting
        final GameEntity entity = this.card.getEnchanting();
        if (entity != null) {
            if (str.length() != 0) {
                str.append("\n");
            }
            str.append("*Enchanting ");

            if (entity instanceof Card) {
                final Card temp = (Card) entity;
                if (temp.isFaceDown() && temp.getController().isComputer()) {
                    str.append("Morph (");
                    str.append(this.card.getUniqueNumber());
                    str.append(")");
                } else {
                    str.append(entity);
                }
            } else {
                str.append(entity);
            }
            str.append("*");
        }

        // Enchanted by
        if (this.card.getEnchantedBy().size() > 0) {
            if (str.length() != 0) {
                str.append("\n");
            }
            str.append("*Enchanted by ");
            for (final Iterator<Card> it = this.card.getEnchantedBy().iterator(); it.hasNext();) {
                str.append(it.next());
                if (it.hasNext()) {
                    str.append(", ");
                }
            }
            str.append("*");
        }

        // Controlling
        if (this.card.getGainControlTargets().size() > 0) {
            if (str.length() != 0) {
                str.append("\n");
            }
            str.append("+Controlling: ");
            for (final Iterator<Card> it = this.card.getGainControlTargets().iterator(); it.hasNext();) {
                str.append(it.next());
                if (it.hasNext()) {
                    str.append(", ");
                }
            }
            str.append("+");
        }

        // Cloned via
        if (this.card.getCloneOrigin() != null) {
            if (str.length() != 0) {
                str.append("\n");
            }
            str.append("^Cloned via: ");
            str.append(this.card.getCloneOrigin().getName());
            str.append("^");
        }

        // Imprint
        if (!this.card.getImprinted().isEmpty()) {
            if (str.length() != 0) {
                str.append("\n");
            }
            str.append("^Imprinting: ");
            for (final Iterator<Card> it = this.card.getImprinted().iterator(); it.hasNext();) {
                str.append(it.next());
                if (it.hasNext()) {
                    str.append(", ");
                }
            }
            str.append("^");
        }

        // Uncastable
        /*
         * if (card.isUnCastable()) { if (str.length() != 0) str.append("\n");
         * str.append("This card can't be cast."); }
         */

        if (this.card.hasAttachedCardsByMindsDesire()) {
            if (str.length() != 0) {
                str.append("\n");
            }
            final Card[] cards = this.card.getAttachedCardsByMindsDesire();
            str.append("=Attached: ");
            for (final Card temp : cards) {
                str.append(temp.getName());
                str.append(" ");
            }
            str.append("=");
        }

        this.tarInfo.setText(str.toString());
    }

    /**
     * Gets the card.
     * 
     * @return Card
     */
    @Override
    public Card getCard() {
        return this.card;
    }

    /** A brief JLabel to consolidate styling. */
    private class DetailLabel extends JLabel {
        public DetailLabel(final boolean zebra0) {
            super();

            if (zebra0) {
                this.setBackground(CardDetailPanel.this.zebra);
                this.setOpaque(true);
            }

            this.setFont(CardDetailPanel.this.f);
            this.setForeground(CardDetailPanel.this.foreground);
            this.setHorizontalAlignment(SwingConstants.CENTER);
        }

        public DetailLabel() {
            this(false);
        }
    }
}
