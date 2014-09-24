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
package forge.screens.match.views;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.swing.JLabel;
import javax.swing.ScrollPaneConstants;

import com.google.common.collect.Lists;

import net.miginfocom.swing.MigLayout;
import forge.gui.framework.DragCell;
import forge.gui.framework.DragTab;
import forge.gui.framework.EDocID;
import forge.gui.framework.IVDoc;
import forge.match.MatchUtil;
import forge.model.FModel;
import forge.properties.ForgePreferences.FPref;
import forge.screens.match.controllers.CPlayers;
import forge.toolbox.FScrollPanel;
import forge.toolbox.FSkin;
import forge.toolbox.FSkin.SkinnedLabel;
import forge.view.CardView;
import forge.view.PlayerView;

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
    private final FScrollPanel scroller = new FScrollPanel(new MigLayout("insets 0, gap 0, wrap"), false,
            ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
            ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

    // Other fields
    private Map<PlayerView, JLabel[]> infoLBLs;

    //========= Overridden methods

    /* (non-Javadoc)
     * @see forge.gui.framework.IVDoc#populate()
     */
    @Override
    public void populate() {
        scroller.removeAll();
        final String constraints = "w 97%!, gapleft 2%, gapbottom 1%";
        for (final Entry<PlayerView, JLabel[]> p : infoLBLs.entrySet()) {
            for (JLabel label : p.getValue()) {
                scroller.add(label, constraints);
            }
        }
        parentCell.getBody().setLayout(new MigLayout("insets 0, gap 0"));
        parentCell.getBody().add(scroller, "w 100%, h 100%!");
    }

    public void init(final Iterable<PlayerView> players) {
        this.infoLBLs = new HashMap<>();
        for (final PlayerView p : players) {
            // Create and store labels detailing various non-critical player info.
            final InfoLabel name = new InfoLabel();
            final InfoLabel life = new InfoLabel();
            final InfoLabel hand = new InfoLabel();
            final InfoLabel draw = new InfoLabel();
            final InfoLabel prevention = new InfoLabel();
            final InfoLabel keywords = new InfoLabel();
            final InfoLabel antes = new InfoLabel();
            final InfoLabel cmd = new InfoLabel();
            this.infoLBLs.put(p, new JLabel[] { name, life, hand, draw, prevention, keywords, antes, cmd });

            // Set border on bottom label, and larger font on player name
            name.setBorder(new FSkin.MatteSkinBorder(1, 0, 0, 0, FSkin.getColor(FSkin.Colors.CLR_BORDERS)));
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

    /** @param game {@link forge.game.player.Player} */
    public void update() {
        // No need to update if this panel isn't showing
        if (parentCell == null || !this.equals(parentCell.getSelected())) { return; }
        boolean isCommander = MatchUtil.getGameView().isCommander();

        for(final Entry<PlayerView, JLabel[]> rr : infoLBLs.entrySet()) {
            PlayerView p0 = rr.getKey();
            final JLabel[] temp = rr.getValue();
            temp[1].setText("Life: " + String.valueOf(p0.getLife()) + "  |  Poison counters: "
                    + String.valueOf(p0.getPoisonCounters()));
            temp[2].setText("Maximum hand size: " + String.valueOf(p0.getMaxHandSize()));
            temp[3].setText("Cards drawn this turn: " + String.valueOf(p0.getNumDrawnThisTurn()));
            temp[4].setText("Damage Prevention: " + String.valueOf(p0.getPreventNextDamage()));
            List<String> keywords = Lists.newArrayList(p0.getKeywords());
            while (keywords.indexOf("CanSeeOpponentsFaceDownCards") != -1) {
                keywords.remove("CanSeeOpponentsFaceDownCards");
            }
            if (!keywords.isEmpty()) {
                temp[5].setText(keywords.toString());
            } else {
                temp[5].setText("");
            }
            if (FModel.getPreferences().getPrefBoolean(FPref.UI_ANTE)) {
                final List<CardView> list = p0.getAnteCards();
                final StringBuilder sb = new StringBuilder();
                sb.append("Ante'd: ");
                for (int i = 0; i < list.size(); i++) {
                    sb.append(list.get(i));
                    if (i < (list.size() - 1)) {
                        sb.append(", ");
                    }
                }
                temp[6].setText(sb.toString());
            }
            if (isCommander) {
                temp[7].setText(p0.getCommanderInfo());
            }
        }
    }

    //========= Custom class handling

    /** A quick JLabel for info in "players" panel, to consolidate styling. */
    @SuppressWarnings("serial")
    private class InfoLabel extends SkinnedLabel {
        public InfoLabel() {
            super();
            this.setForeground(FSkin.getColor(FSkin.Colors.CLR_TEXT));
        }
    }
}
