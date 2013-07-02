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

import java.awt.Color;
import java.awt.Font;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;
import javax.swing.border.Border;
import javax.swing.border.LineBorder;
import javax.swing.border.MatteBorder;

import net.miginfocom.swing.MigLayout;
import forge.game.player.LobbyPlayer;
import forge.game.player.Player;
import forge.game.zone.ZoneType;
import forge.gui.framework.DragCell;
import forge.gui.framework.DragTab;
import forge.gui.framework.EDocID;
import forge.gui.framework.IVDoc;
import forge.gui.match.CMatchUI;
import forge.gui.toolbox.FLabel;
import forge.gui.toolbox.FSkin;
import forge.gui.toolbox.special.PhaseIndicator;
import forge.gui.toolbox.special.PlayerDetailsPanel;
import forge.view.arcane.PlayArea;

/** 
 * Assembles Swing components of a player field instance.
 * 
 * <br><br><i>(V at beginning of class name denotes a view class.)</i>
 */
public class VField implements IVDoc<CField> {
    // Fields used with interface IVDoc
    private final CField control;
    private DragCell parentCell;
    private final EDocID docID;
    private final DragTab tab = new DragTab("Field");

    // Other fields
    private Player player = null;

    // Top-level containers
    private final JScrollPane scroller = new JScrollPane();
    private final PlayArea tabletop;
    private final JPanel avatarArea = new JPanel();

    private final PlayerDetailsPanel detailsPanel;

    // Avatar area
    private final FLabel lblAvatar = new FLabel.Builder().fontAlign(SwingConstants.CENTER).iconScaleFactor(1.0f).build();
    private final FLabel lblLife = new FLabel.Builder().fontAlign(SwingConstants.CENTER).fontStyle(Font.BOLD).build();


    private final PhaseIndicator phaseInidicator = new PhaseIndicator();

    private final Border borderAvatarSimple = new LineBorder(new Color(0, 0, 0, 0), 1);
    private final Border borderAvatarHover = new LineBorder(FSkin.getColor(FSkin.Colors.CLR_BORDERS), 1);
    private final Border borderAvatarHighlited = new LineBorder(Color.red, 2);


    //========= Constructor
    /**
     * Assembles Swing components of a player field instance.
     * 
     * @param playerOnwer &emsp; {@link forge.game.player.Player}
     * @param id0 &emsp; {@link forge.gui.framework.EDocID}
     */
    public VField(final EDocID id0, final Player playerOnwer, LobbyPlayer playerViewer) {
        this.docID = id0;
        id0.setDoc(this);

        this.player = playerOnwer;
        if (playerOnwer != null) { tab.setText(playerOnwer.getName() + " Field"); }
        else { tab.setText("NO PLAYER FOR " + docID.toString()); }

        
        detailsPanel = new PlayerDetailsPanel(player);

        // TODO player is hard-coded into tabletop...should be dynamic
        // (haven't looked into it too deeply). Doublestrike 12-04-12
        tabletop = new PlayArea(scroller, id0 == EDocID.FIELD_1, player.getZone(ZoneType.Battlefield).getCards(false));

        control = new CField(player, this, playerViewer);

        avatarArea.setOpaque(false);
        avatarArea.setBackground(FSkin.getColor(FSkin.Colors.CLR_HOVER));
        avatarArea.setLayout(new MigLayout("insets 0, gap 0"));
        avatarArea.add(lblAvatar, "w 100%!, h 70%!, wrap, gaptop 4%");
        avatarArea.add(lblLife, "w 100%!, h 30%!, gaptop 4%");

        // Player area hover effect
        avatarArea.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(final MouseEvent e) {
                avatarArea.setOpaque(true);
                if (!isHighlited())
                    avatarArea.setBorder(borderAvatarHover);
            }

            @Override
            public void mouseExited(final MouseEvent e) {
                avatarArea.setOpaque(false);
                if (!isHighlited())
                    avatarArea.setBorder(borderAvatarSimple);
            }
        });

        tabletop.setBorder(new MatteBorder(0, 1, 0, 0,
                FSkin.getColor(FSkin.Colors.CLR_BORDERS)));
        tabletop.setOpaque(false);

        scroller.setViewportView(this.tabletop);
        scroller.setOpaque(false);
        scroller.getViewport().setOpaque(false);
        scroller.setBorder(null);
        
        updateDetails();
    }

    //========= Overridden methods

    /* (non-Javadoc)
     * @see forge.gui.framework.IVDoc#populate()
     */
    @Override
    public void populate() {
        final JPanel pnl = parentCell.getBody();
        pnl.setLayout(new MigLayout("insets 0, gap 0"));

        pnl.add(avatarArea, "w 10%!, h 30%!");
        pnl.add(phaseInidicator, "w 5%!, h 100%!, span 1 2");
        pnl.add(scroller, "w 85%!, h 100%!, span 1 2, wrap");
        pnl.add(detailsPanel, "w 10%!, h 69%!, gapleft 1px");
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
    public CField getLayoutControl() {
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

    //========= Populate helper methods


    // ========== Observer update methods

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

    /**
     * Gets the avatar area.
     *
     * @return JPanel containing avatar pic and life label
     */
    public JPanel getAvatarArea() {
        return this.avatarArea;
    }

    /** @return {@link javax.swing.JLabel} */
    public FLabel getLblAvatar() {
        return this.lblAvatar;
    }

    /** @return {@link javax.swing.JLabel} */
    public FLabel getLblLife() {
        return this.lblLife;
    }

    /**
     * TODO: Write javadoc for this method.
     * @return
     */
    public PhaseIndicator getPhaseInidicator() {
        return phaseInidicator;
    }

    /**
     * @return the detailsPanel
     */
    public PlayerDetailsPanel getDetailsPanel() {
        return detailsPanel;
    }

    public boolean isHighlited() {
        return CMatchUI.SINGLETON_INSTANCE.isHighlited(player);
    }
    
    /**
     * TODO: Write javadoc for this method.
     * @param player2
     */
    public void updateDetails() {
        detailsPanel.updateDetails();
        
        this.getLblLife().setText("" + player.getLife());
        Color lifeFg = player.getLife() <= 5 ? Color.red : FSkin.getColor(FSkin.Colors.CLR_TEXT);
        this.getLblLife().setForeground(lifeFg);

        
        boolean highlited = isHighlited(); 
        this.avatarArea.setBorder(highlited ? borderAvatarHighlited : borderAvatarSimple );
        this.avatarArea.setOpaque(highlited);

    }
}
