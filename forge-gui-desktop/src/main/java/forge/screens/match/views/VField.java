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
package forge.screens.match.views;

import java.awt.Color;
import java.awt.Font;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.border.Border;
import javax.swing.border.LineBorder;

import net.miginfocom.swing.MigLayout;
import forge.assets.FSkinProp;
import forge.game.player.PlayerView;
import forge.game.zone.ZoneType;
import forge.gui.framework.DragCell;
import forge.gui.framework.DragTab;
import forge.gui.framework.EDocID;
import forge.gui.framework.IVDoc;
import forge.screens.match.CMatchUI;
import forge.screens.match.controllers.CField;
import forge.toolbox.FLabel;
import forge.toolbox.FScrollPane;
import forge.toolbox.FSkin;
import forge.toolbox.FSkin.SkinImage;
import forge.toolbox.FSkin.SkinnedPanel;
import forge.toolbox.special.PhaseIndicator;
import forge.toolbox.special.PlayerDetailsPanel;
import forge.view.arcane.PlayArea;

/** 
 * Assembles Swing components of a player field instance.
 * 
 * <br><br><i>(V at beginning of class name denotes a view class.)</i>
 */
public class VField implements IVDoc<CField> {
    private final static int LIFE_CRITICAL = 5;
    private final static int POISON_CRITICAL = 8;

    // Fields used with interface IVDoc
    private final CField control;
    private DragCell parentCell;
    private final EDocID docID;
    private final DragTab tab = new DragTab("Field");

    // Other fields
    private final PlayerView player;

    // Top-level containers
    private final FScrollPane scroller = new FScrollPane(false);
    private final PlayArea tabletop;
    private final SkinnedPanel avatarArea = new SkinnedPanel();

    private final PlayerDetailsPanel detailsPanel;

    // Avatar area
    private final FLabel lblAvatar = new FLabel.Builder().fontAlign(SwingConstants.CENTER).iconScaleFactor(1.0f).build();
    private final FLabel lblLife   = new FLabel.Builder().fontAlign(SwingConstants.CENTER).fontStyle(Font.BOLD).build();
    private final FLabel lblPoison = new FLabel.Builder().fontAlign(SwingConstants.CENTER).fontStyle(Font.BOLD).icon(FSkin.getImage(FSkinProp.IMG_ZONE_POISON)).iconInBackground().build();

    private final PhaseIndicator phaseIndicator = new PhaseIndicator();

    private final Border borderAvatarSimple = new LineBorder(new Color(0, 0, 0, 0), 1);
    private final Border borderAvatarHighlighted = new LineBorder(Color.red, 2);


    //========= Constructor
    /**
     * Assembles Swing components of a player field instance.
     * 
     * @param p &emsp; {@link forge.game.player.Player}
     * @param id0 &emsp; {@link forge.gui.framework.EDocID}
     */
    public VField(final CMatchUI matchUI, final EDocID id0, final PlayerView p, final boolean mirror) {
        this.docID = id0;

        this.player = p;
        if (p != null) { tab.setText(p.getName() + " Field"); }
        else { tab.setText("NO PLAYER FOR " + docID.toString()); }

        detailsPanel = new PlayerDetailsPanel(player);

        // TODO player is hard-coded into tabletop...should be dynamic
        // (haven't looked into it too deeply). Doublestrike 12-04-12
        tabletop = new PlayArea(matchUI, scroller, mirror, player, ZoneType.Battlefield);

        control = new CField(matchUI, player, this);

        lblAvatar.setFocusable(false);
        lblLife.setFocusable(false);
        lblPoison.setFocusable(false);

        avatarArea.setOpaque(false);
        avatarArea.setBackground(FSkin.getColor(FSkin.Colors.CLR_HOVER));
        avatarArea.setLayout(new MigLayout("insets 0, gap 0"));
        avatarArea.add(lblAvatar, "w 100%-6px!, h 100%-23px!, wrap, gap 3 3 3 0");
        avatarArea.add(lblLife, "w 100%!, h 20px!, wrap");

        // Player area hover effect
        avatarArea.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(final MouseEvent e) {
                avatarArea.setOpaque(true);
                if (!isHighlighted()) {
                    avatarArea.setBorder(new FSkin.LineSkinBorder(FSkin.getColor(FSkin.Colors.CLR_BORDERS)));
                }
            }

            @Override
            public void mouseExited(final MouseEvent e) {
                avatarArea.setOpaque(false);
                if (!isHighlighted()) {
                    avatarArea.setBorder(borderAvatarSimple);
                }
            }
        });

        tabletop.setBorder(new FSkin.MatteSkinBorder(0, 1, 0, 0, FSkin.getColor(FSkin.Colors.CLR_BORDERS)));
        tabletop.setOpaque(false);

        scroller.setViewportView(this.tabletop);

        updateDetails();
    }

    @Override
    public void populate() {
        final JPanel pnl = parentCell.getBody();
        pnl.setLayout(new MigLayout("insets 0, gap 0"));

        pnl.add(avatarArea, "w 10%!, h 35%!");
        pnl.add(phaseIndicator, "w 5%!, h 100%!, span 1 2");
        pnl.add(scroller, "w 85%!, h 100%!, span 1 2, wrap");
        pnl.add(detailsPanel, "w 10%!, h 64%!, gapleft 1px");
    }

    @Override
    public EDocID getDocumentID() {
        return docID;
    }

    @Override
    public DragTab getTabLabel() {
        return tab;
    }

    @Override
    public CField getLayoutControl() {
        return control;
    }

    @Override
    public void setParentCell(final DragCell cell0) {
        this.parentCell = cell0;
    }

    @Override
    public DragCell getParentCell() {
        return this.parentCell;
    }

    public PlayArea getTabletop() {
        return this.tabletop;
    }

    public JPanel getAvatarArea() {
        return this.avatarArea;
    }

    public PhaseIndicator getPhaseIndicator() {
        return phaseIndicator;
    }

    public PlayerDetailsPanel getDetailsPanel() {
        return detailsPanel;
    }

    private boolean isHighlighted() {
        return control.getMatchUI().isHighlighted(player);
    }

    public void setAvatar(final SkinImage avatar) {
        lblAvatar.setIcon(avatar);
        lblAvatar.getResizeTimer().start();
    }

    public void updateManaPool() {
        detailsPanel.updateManaPool();
    }
    public void updateZones() {
        detailsPanel.updateZones();
    }

    private void addLblPoison() {
        if (lblPoison.isShowing()) {
            return;
        }
        avatarArea.remove(lblLife);
        lblLife.setIcon(FSkin.getImage(FSkinProp.ICO_QUEST_LIFE));
        avatarArea.add(lblLife, "w 50%!, h 20px!, split 2");
        avatarArea.add(lblPoison, "w 50%!, h 20px!, wrap");
    }
    private void removeLblPoison() {
        if (!lblPoison.isShowing()) {
            return;
        }
        avatarArea.remove(lblPoison);
        avatarArea.remove(lblLife);
        avatarArea.add(lblLife, "w 100%!, h 20px!, wrap");
    }

    public void updateDetails() {
        // Update life total
        final int life = player.getLife();
        lblLife.setText(String.valueOf(life));
        if (life > LIFE_CRITICAL) {
            lblLife.setForeground(FSkin.getColor(FSkin.Colors.CLR_TEXT));
        } else {
            lblLife.setForeground(Color.RED);
        }

        // Update poison counters
        final int poison = player.getPoisonCounters();
        if (poison > 0) {
            addLblPoison();
            lblPoison.setText(String.valueOf(poison));
            if (poison < POISON_CRITICAL) {
                lblPoison.setForeground(FSkin.getColor(FSkin.Colors.CLR_TEXT));
            } else {
                lblPoison.setForeground(Color.RED);
            }
        } else {
            removeLblPoison();
        }

        final boolean highlighted = isHighlighted();
        this.avatarArea.setBorder(highlighted ? borderAvatarHighlighted : borderAvatarSimple );
        this.avatarArea.setOpaque(highlighted);
        this.avatarArea.setToolTipText(player.getDetailsHtml());
    }
}
