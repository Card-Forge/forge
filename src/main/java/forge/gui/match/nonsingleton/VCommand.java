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
package forge.gui.match.nonsingleton;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.border.MatteBorder;

import net.miginfocom.swing.MigLayout;
import forge.game.player.Player;
import forge.gui.framework.DragCell;
import forge.gui.framework.DragTab;
import forge.gui.framework.EDocID;
import forge.gui.framework.IVDoc;
import forge.gui.toolbox.FSkin;
import forge.view.arcane.PlayArea;

/** 
 * Assembles Swing components of a player field instance.
 * 
 * <br><br><i>(V at beginning of class name denotes a view class.)</i>
 */
public class VCommand implements IVDoc<CCommand> {
    // Fields used with interface IVDoc
    private final CCommand control;
    private DragCell parentCell;
    private final EDocID docID;
    private final DragTab tab = new DragTab("Command");

    // Other fields
    private Player player = null;

    // Top-level containers
    private final JScrollPane scroller = new JScrollPane();
    private final PlayArea tabletop;

    //========= Constructor
    /**
     * Assembles Swing components of a player field instance.
     * 
     * @param player0 &emsp; {@link forge.game.player.Player}
     * @param id0 &emsp; {@link forge.gui.framework.EDocID}
     */
    public VCommand(final EDocID id0, final Player player0) {
        this.docID = id0;
        id0.setDoc(this);

        this.player = player0;
        if (player0 != null) { tab.setText(player0.getName() + " Command"); }
        else { tab.setText("NO PLAYER FOR " + docID.toString()); }

        // TODO player is hard-coded into tabletop...should be dynamic
        // (haven't looked into it too deeply). Doublestrike 12-04-12
        tabletop = new PlayArea(scroller, id0 == EDocID.COMMAND_0 );

        control = new CCommand(player, this);

        tabletop.setBorder(new MatteBorder(0, 1, 0, 0,
                FSkin.getColor(FSkin.Colors.CLR_BORDERS)));
        tabletop.setOpaque(false);

        scroller.setViewportView(this.tabletop);
        scroller.setOpaque(false);
        scroller.getViewport().setOpaque(false);
        scroller.setBorder(null);

    }

    //========= Overridden methods

    /* (non-Javadoc)
     * @see forge.gui.framework.IVDoc#populate()
     */
    @Override
    public void populate() {
        final JPanel pnl = parentCell.getBody();
        pnl.setLayout(new MigLayout("insets 0, gap 0"));

        pnl.add(scroller, "w 85%!, h 100%!, span 1 2, wrap");
    }

    /* (non-Javadoc)
     * @see forge.gui.framework.IVDoc#getDocumentID()
     */
    @Override
    public EDocID getDocumentID() {
        return docID;
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
    public CCommand getLayoutControl() {
        return control;
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

    //========= Retrieval methods
    /**
     * Gets the player currently associated with this field.
     * @return {@link forge.game.player.Player}
     */
    public Player getPlayer() {
        return this.player;
    }

    /**
     * Gets the tabletop.
     *
     * @return PlayArea where cards for this field are in play
     */
    public PlayArea getTabletop() {
        return this.tabletop;
    }
}
