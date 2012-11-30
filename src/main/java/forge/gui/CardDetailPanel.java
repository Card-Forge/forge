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

package forge.gui;

import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.util.Iterator;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import javax.swing.border.EtchedBorder;

import forge.Card;
import forge.CounterType;
import forge.GameEntity;
import forge.Singletons;
import forge.game.zone.ZoneType;

/**
 * The class CardDetailPanel. Shows the details of a card.
 * 
 * @author Clemens Koza
 * @version V0.0 17.02.2010
 */
public class CardDetailPanel extends JPanel implements CardContainer {
    /** Constant <code>serialVersionUID=-8461473263764812323L</code>. */
    private static final long serialVersionUID = -8461473263764812323L;

    private static Color purple = new Color(14381203);

    private Card card;

    private final JLabel nameCostLabel;
    private final JLabel typeLabel;
    private final JLabel powerToughnessLabel;
    private final JLabel idLabel;
    private final JLabel setInfoLabel;
    private final JTextArea cdArea;
    private final JScrollPane scrArea;

    /**
     * <p>
     * Constructor for CardDetailPanel.
     * </p>
     * 
     * @param card
     *            a {@link forge.Card} object.
     */
    public CardDetailPanel(final Card card) {
        this.setLayout(new GridBagLayout());
        this.setBorder(new EtchedBorder());

        GridBagConstraints labelConstrains = new GridBagConstraints();
        labelConstrains.fill = GridBagConstraints.BOTH;
        labelConstrains.gridx = 0;
        labelConstrains.gridy = 0;
        labelConstrains.weightx = 1.0;

        final JPanel cdLabels = new JPanel(new GridLayout(0, 1, 0, 5));
        this.nameCostLabel = new JLabel();
        this.typeLabel = new JLabel();
        this.powerToughnessLabel = new JLabel();
        cdLabels.add(this.nameCostLabel);
        cdLabels.add(this.typeLabel);
        cdLabels.add(this.powerToughnessLabel);

        final JPanel idr = new JPanel(new GridBagLayout());
        final GridBagConstraints c1 = new GridBagConstraints();
        final GridBagConstraints c2 = new GridBagConstraints();

        c1.fill = GridBagConstraints.HORIZONTAL;

        c1.gridwidth = 2;
        c1.weightx = 1.0;
        this.idLabel = new JLabel();
        idr.add(this.idLabel, c1);

        c2.gridwidth = 1;
        c2.weightx = 0.3;
        c2.fill = GridBagConstraints.HORIZONTAL;
        this.setInfoLabel = new JLabel();
        idr.add(this.setInfoLabel, c2);

        cdLabels.add(idr);

        this.add(cdLabels, labelConstrains);
        this.nameCostLabel.setHorizontalAlignment(SwingConstants.CENTER);
        this.typeLabel.setHorizontalAlignment(SwingConstants.CENTER);
        this.powerToughnessLabel.setHorizontalAlignment(SwingConstants.CENTER);
        // cdLabel7.setSize(100, cdLabel7.getHeight());

        this.setInfoLabel.setHorizontalAlignment(SwingConstants.CENTER);

        this.cdArea = new JTextArea(4, 12);
        this.scrArea = new JScrollPane(this.cdArea);

        GridBagConstraints areaConstraints = new GridBagConstraints();
        areaConstraints.fill = GridBagConstraints.BOTH;
        areaConstraints.gridx = 0;
        areaConstraints.gridy = 1;
        areaConstraints.weightx = 1.0;
        areaConstraints.weighty = 1.0;
        this.add(scrArea, areaConstraints);
        this.cdArea.setLineWrap(true);
        this.cdArea.setWrapStyleWord(true);
        this.cdArea.setEditable(false);
        this.cdArea.setBorder(new EmptyBorder(4, 4, 4, 4));

        this.nameCostLabel.setFont(new java.awt.Font("Dialog", 0, 14));
        this.typeLabel.setFont(new java.awt.Font("Dialog", 0, 14));
        this.powerToughnessLabel.setFont(new java.awt.Font("Dialog", 0, 14));
        this.idLabel.setFont(new java.awt.Font("Dialog", 0, 14));

        java.awt.Font f = new java.awt.Font("Dialog", 0, 14);
        f = f.deriveFont(java.awt.Font.BOLD);
        this.setInfoLabel.setFont(f);

        this.cdArea.setFont(new java.awt.Font("Dialog", 0, 14));

        this.setCard(card);
    }

    /** {@inheritDoc} */
    @Override
    public final void setCard(final Card card) {
        this.nameCostLabel.setText("");
        this.typeLabel.setText("");
        this.powerToughnessLabel.setText("");
        this.idLabel.setText("");
        this.setInfoLabel.setText("");
        this.setInfoLabel.setOpaque(false);
        this.setInfoLabel.setBorder(null);
        this.cdArea.setText("");
        this.setBorder(GuiDisplayUtil.getBorder(card));

        this.card = card;
        if (card == null) {
            return;
        }

        final boolean faceDown = card.isFaceDown() && (card.getController() != Singletons.getControl().getPlayer());
        if (!faceDown) {
            if (card.getManaCost().toString().equals("") || card.isLand()) {
                this.nameCostLabel.setText(card.getName());
            } else {
                this.nameCostLabel.setText(card.getName() + " - " + card.getManaCost());
            }
        } else {
            this.nameCostLabel.setText("Morph");
        }

        if (!faceDown) {
            this.typeLabel.setText(GuiDisplayUtil.formatCardType(card));
        } else {
            this.typeLabel.setText("Creature");
        }
        if (card.isCreature()) {
            this.powerToughnessLabel.setText(card.getNetAttack() + " / " + card.getNetDefense());
        }

        this.idLabel.setText("Card ID  " + card.getUniqueNumber());

        // rarity and set of a face down card should not be visible to the
        // opponent
        if (!card.isFaceDown() || card.getController().isHuman()) {
            this.setInfoLabel.setText(card.getCurSetCode());
        }

        if (!this.setInfoLabel.getText().equals("")) {
            this.setInfoLabel.setOpaque(true);
            final String csr = card.getCurSetRarity();
            if (csr.equals("Common") || csr.equals("Land")) {
                this.setInfoLabel.setBackground(Color.BLACK);
                this.setInfoLabel.setForeground(Color.WHITE);
                this.setInfoLabel.setBorder(BorderFactory.createLineBorder(Color.WHITE));
            } else if (csr.equals("Uncommon")) {
                this.setInfoLabel.setBackground(Color.LIGHT_GRAY);
                this.setInfoLabel.setForeground(Color.BLACK);
                this.setInfoLabel.setBorder(BorderFactory.createLineBorder(Color.BLACK));
            } else if (csr.equals("Rare")) {
                this.setInfoLabel.setBackground(Color.YELLOW);
                this.setInfoLabel.setForeground(Color.BLACK);
                this.setInfoLabel.setBorder(BorderFactory.createLineBorder(Color.BLACK));
            } else if (csr.equals("Mythic")) {
                this.setInfoLabel.setBackground(Color.RED);
                this.setInfoLabel.setForeground(Color.BLACK);
                this.setInfoLabel.setBorder(BorderFactory.createLineBorder(Color.BLACK));
            } else if (csr.equals("Special")) {
                // "Timeshifted" or other Special Rarity Cards
                this.setInfoLabel.setBackground(CardDetailPanel.purple);
                this.setInfoLabel.setForeground(Color.BLACK);
                this.setInfoLabel.setBorder(BorderFactory.createLineBorder(Color.BLACK));
            }
            // cdLabel7.setText(card.getCurSetCode());
        }

        // fill the card text

        final StringBuilder area = new StringBuilder();

        // Token
        if (card.isToken()) {
            area.append("Token");
        }

        if (!faceDown) {
            // card text
            if (area.length() != 0) {
                area.append("\n");
            }
            String text = card.getText();
            // LEVEL [0-9]+-[0-9]+
            // LEVEL [0-9]+\+

            String regex = "LEVEL [0-9]+-[0-9]+ ";
            text = text.replaceAll(regex, "$0\r\n");

            regex = "LEVEL [0-9]+\\+ ";
            text = text.replaceAll(regex, "\r\n$0\r\n");

            // displays keywords that have dots in them a little better:
            regex = "\\., ";
            text = text.replaceAll(regex, ".\r\n");

            area.append(text);
        }

        if (card.isPhasedOut()) {
            if (area.length() != 0) {
                area.append("\n");
            }
            area.append("Phased Out");
        }

        // counter text
        final CounterType[] counters = CounterType.values();
        for (final CounterType counter : counters) {
            if (card.getCounters(counter) != 0) {
                if (area.length() != 0) {
                    area.append("\n");
                }
                area.append(counter.getName() + " counters: ");
                area.append(card.getCounters(counter));
            }
        }

        if (card.isCreature()) {
            int damage = card.getDamage();
            if (damage > 0) {
                if (area.length() != 0) {
                    area.append("\n");
                }
                area.append("Damage: " + damage);
            }
            int assigned = card.getTotalAssignedDamage();
            if (assigned > 0) {
                if (area.length() != 0) {
                    area.append("\n");
                }
                area.append("Assigned Damage: " + assigned);
            }
        }
        if (card.isPlaneswalker()) {
            int assigned = card.getTotalAssignedDamage();
            if (assigned > 0) {
                if (area.length() != 0) {
                    area.append("\n");
                }
                area.append("Assigned Damage: " + assigned);
            }
        }

        // Regeneration Shields
        final int regenShields = card.getShield();
        if (regenShields > 0) {
            if (area.length() != 0) {
                area.append("\n");
            }
            area.append("Regeneration Shield(s): ").append(regenShields);
        }

        // Damage Prevention
        final int preventNextDamage = card.getPreventNextDamage();
        if (preventNextDamage > 0) {
            area.append("\n");
            area.append("Prevent the next ").append(preventNextDamage).append(" damage that would be dealt to ");
            area.append(card.getName()).append(" this turn.");
        }

        // top revealed
        if ((card.hasKeyword("Play with the top card of your library revealed.") || card
                .hasKeyword("Players play with the top card of their libraries revealed."))
                && (card.getController() != null)
                && card.isInZone(ZoneType.Battlefield)
                && !card.getController().getZone(ZoneType.Library).isEmpty()) {
            area.append("\r\nTop card of your library: ");
            area.append(card.getController().getCardsIn(ZoneType.Library, 1));
        }
        if (card.hasKeyword("Players play with the top card of their libraries revealed.")
                && (card.getController() != null) && card.isInZone(ZoneType.Battlefield)
                && !card.getController().getOpponent().getZone(ZoneType.Library).isEmpty()) {
            area.append("\r\nTop card of your opponent's library: ");
            area.append(card.getController().getOpponent().getCardsIn(ZoneType.Library, 1));
        }

        // chosen type
        if (!card.getChosenType().equals("")) {
            if (area.length() != 0) {
                area.append("\n");
            }
            area.append("(chosen type: ");
            area.append(card.getChosenType());
            area.append(")");
        }

        // chosen color
        if (!card.getChosenColor().isEmpty()) {
            if (area.length() != 0) {
                area.append("\n");
            }
            area.append("(chosen colors: ");
            area.append(card.getChosenColor());
            area.append(")");
        }

        // named card
        if (!card.getNamedCard().equals("")) {
            if (area.length() != 0) {
                area.append("\n");
            }
            area.append("(named card: ");
            area.append(card.getNamedCard());
            area.append(")");
        }

        // equipping
        if (card.getEquipping().size() > 0) {
            if (area.length() != 0) {
                area.append("\n");
            }
            area.append("=Equipping ");
            area.append(card.getEquipping().get(0));
            area.append("=");
        }

        // equipped by
        if (card.getEquippedBy().size() > 0) {
            if (area.length() != 0) {
                area.append("\n");
            }
            area.append("=Equipped by ");
            for (final Iterator<Card> it = card.getEquippedBy().iterator(); it.hasNext();) {
                area.append(it.next());
                if (it.hasNext()) {
                    area.append(", ");
                }
            }
            area.append("=");
        }

        // enchanting
        final GameEntity entity = card.getEnchanting();
        if (entity != null) {
            if (area.length() != 0) {
                area.append("\n");
            }
            area.append("*Enchanting ");

            if (entity instanceof Card) {
                final Card c = (Card) entity;
                if (c.isFaceDown() && c.getController().isComputer()) {
                    area.append("Morph (");
                    area.append(card.getUniqueNumber());
                    area.append(")");
                } else {
                    area.append(entity);
                }
            } else {
                area.append(entity);
            }
            area.append("*");
        }

        // enchanted by
        if (card.getEnchantedBy().size() > 0) {
            if (area.length() != 0) {
                area.append("\n");
            }
            area.append("*Enchanted by ");
            for (final Iterator<Card> it = card.getEnchantedBy().iterator(); it.hasNext();) {
                area.append(it.next());
                if (it.hasNext()) {
                    area.append(", ");
                }
            }
            area.append("*");
        }

        // controlling
        if (card.getGainControlTargets().size() > 0) {
            if (area.length() != 0) {
                area.append("\n");
            }
            area.append("+Controlling: ");
            for (final Iterator<Card> it = card.getGainControlTargets().iterator(); it.hasNext();) {
                area.append(it.next());
                if (it.hasNext()) {
                    area.append(", ");
                }
            }
            area.append("+");
        }

        // cloned via
        if (card.getCloneOrigin() != null) {
            if (area.length() != 0) {
                area.append("\n");
            }
            area.append("^Cloned via: ");
            area.append(card.getCloneOrigin().getName());
            area.append("^");
        }

        // Imprint
        if (!card.getImprinted().isEmpty()) {
            if (area.length() != 0) {
                area.append("\n");
            }
            area.append("^Imprinting: ");
            for (final Iterator<Card> it = card.getImprinted().iterator(); it.hasNext();) {
                area.append(it.next());
                if (it.hasNext()) {
                    area.append(", ");
                }
            }
            area.append("^");
        }

        // must block
        if (card.getMustBlockCards() != null) {
            if (area.length() != 0) {
                area.append("\n");
            }
            area.append("Must block an attacker");
        }

        this.cdArea.setText(area.toString());

        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                scrArea.getVerticalScrollBar().setValue(scrArea.getVerticalScrollBar().getMinimum());
            }
        });
    }

    /**
     * <p>
     * Getter for the field <code>card</code>.
     * </p>
     * 
     * @return a {@link forge.Card} object.
     */
    @Override
    public final Card getCard() {
        return this.card;
    }

    /** @return JLabel */
    public JLabel getNameCostLabel() {
        return this.nameCostLabel;
    }

    /** @return JLabel */
    public JLabel getTypeLabel() {
        return this.typeLabel;
    }

    /** @return JLabel */
    public JLabel getPowerToughnessLabel() {
        return this.powerToughnessLabel;
    }

    /** @return JLabel */
    public JLabel getIDLabel() {
        return this.idLabel;
    }

    /** @return JLabel */
    public JLabel getSetInfoLabel() {
        return this.setInfoLabel;
    }

    /** @return JLabel */
    public JTextArea getCDArea() {
        return this.cdArea;
    }
}
