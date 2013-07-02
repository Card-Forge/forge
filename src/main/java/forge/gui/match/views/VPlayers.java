/*
 * Forge: Play Magic: the Gathering.
 * Copyright (C) 2011  Nate
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
package forge.gui.match.views;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.MatteBorder;

import net.miginfocom.swing.MigLayout;
import forge.Card;
import forge.Singletons;
import forge.game.Game;
import forge.game.player.Player;
import forge.game.zone.ZoneType;
import forge.gui.framework.DragCell;
import forge.gui.framework.DragTab;
import forge.gui.framework.EDocID;
import forge.gui.framework.IVDoc;
import forge.gui.match.controllers.CPlayers;
import forge.gui.toolbox.FSkin;
import forge.properties.ForgePreferences.FPref;

/** 
 * Assembles Swing components of players report.
 *
 * <br><br><i>(V at beginning of class name denotes a view class.)</i>
 */
public enum VPlayers implements IVDoc<CPlayers> {
    /** */
    SINGLETON_INSTANCE;

    // Fields used with interface IVDoc
    private DragCell parentCell;
    private final DragTab tab = new DragTab("Players");

    // Other fields
    private Map<Player, JLabel[]> infoLBLs;
    private JLabel stormLabel;

    //========= Overridden methods

    /* (non-Javadoc)
     * @see forge.gui.framework.IVDoc#populate()
     */
    @Override
    public void populate() {
        final JPanel pnl = parentCell.getBody();
        pnl.setLayout(new MigLayout("insets 0, gap 0, wrap"));

        final String constraints = "w 97%!, gapleft 2%, gapbottom 1%";

        for (final Entry<Player, JLabel[]> p : infoLBLs.entrySet()) {
            for(JLabel label : p.getValue() )
                pnl.add(label, constraints);
        }

        stormLabel = new InfoLabel();
        pnl.add(stormLabel, constraints);
    }
    
    public void init(final Iterable<Player> players) {

        this.infoLBLs = new HashMap<Player, JLabel[]>();
        for (final Player p : players) {
            // Create and store labels detailing various non-critical player info.
            final InfoLabel name = new InfoLabel();
            final InfoLabel life = new InfoLabel();
            final InfoLabel hand = new InfoLabel();
            final InfoLabel draw = new InfoLabel();
            final InfoLabel prevention = new InfoLabel();
            final InfoLabel keywords = new InfoLabel();
            final InfoLabel antes = new InfoLabel();
            this.infoLBLs.put(p, new JLabel[] { name, life, hand, draw, prevention, keywords, antes });

            // Set border on bottom label, and larger font on player name
            antes.setBorder(new MatteBorder(0, 0, 1, 0, FSkin.getColor(FSkin.Colors.CLR_BORDERS)));
            name.setText(p.getName());
        }
    }

    /* (non-Javadoc)
     * @see forge.gui.framework.IVDoc#setParentCell()
     */
    @Override
    public void setParentCell(final DragCell cell0) {
        this.parentCell = cell0;
    }

    /* (non-Javadoc)
     * @see forge.gui.framework.IVDoc#getParentCell()
     */
    @Override
    public DragCell getParentCell() {
        return this.parentCell;
    }

    /* (non-Javadoc)
     * @see forge.gui.framework.IVDoc#getDocumentID()
     */
    @Override
    public EDocID getDocumentID() {
        return EDocID.REPORT_PLAYERS;
    }

    /* (non-Javadoc)
     * @see forge.gui.framework.IVDoc#getTabLabel()
     */
    @Override
    public DragTab getTabLabel() {
        return tab;
    }

    /* (non-Javadoc)
     * @see forge.gui.framework.IVDoc#getLayoutControl()
     */
    @Override
    public CPlayers getLayoutControl() {
        return CPlayers.SINGLETON_INSTANCE;
    }

    //========== Observer update methods

    /** @param p0 {@link forge.game.player.Player} */
    public void update() {
        // No need to update if this panel isn't showing
        if (parentCell == null || !this.equals(parentCell.getSelected())) { return; }

        for(Entry<Player, JLabel[]> rr : infoLBLs.entrySet()) {
            Player p0 = rr.getKey();
            final JLabel[] temp = rr.getValue();
            temp[1].setText("Life: " + String.valueOf(p0.getLife()) + "  |  Poison counters: "
                    + String.valueOf(p0.getPoisonCounters()));
            temp[2].setText("Maximum hand size: " + String.valueOf(p0.getMaxHandSize()));
            temp[3].setText("Cards drawn this turn: " + String.valueOf(p0.getNumDrawnThisTurn()));
            temp[4].setText("Damage Prevention: " + String.valueOf(p0.getPreventNextDamageTotalShields()));
            if (!p0.getKeywords().isEmpty()) {
                temp[5].setText(p0.getKeywords().toString());
            } else {
                temp[5].setText("");
            }
            if (Singletons.getModel().getPreferences().getPrefBoolean(FPref.UI_ANTE)) {
                List<Card> list = p0.getCardsIn(ZoneType.Ante);
                StringBuilder sb = new StringBuilder();
                sb.append("Ante'd: ");
                for (int i = 0; i < list.size(); i++) {
                    sb.append(list.get(i));
                    if (i < (list.size() - 1)) {
                        sb.append(", ");
                    }
                }
                temp[6].setText(sb.toString());
            }
        }
    }

    /**
     * @param game  */
    public void updateStormLabel(Game game) {
        // No need to update if this panel isn't showing
        if (!parentCell.getSelected().equals(this)) { return; }

        stormLabel.setText("Storm count: " + game.getStack().getCardsCastThisTurn().size());
    }

    //========= Custom class handling

    /** A quick JLabel for info in "players" panel, to consolidate styling. */
    @SuppressWarnings("serial")
    private class InfoLabel extends JLabel {
        public InfoLabel() {
            super();
            this.setForeground(FSkin.getColor(FSkin.Colors.CLR_TEXT));
        }
    }
}
