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

import java.awt.Font;
import java.awt.Insets;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;

import forge.game.card.CardView;
import forge.game.player.PlayerView;
import forge.gamemodes.match.YieldMode;
import forge.gui.framework.DragCell;
import forge.gui.framework.DragTab;
import forge.gui.framework.EDocID;
import forge.gui.framework.IVDoc;
import forge.localinstance.properties.ForgePreferences;
import forge.localinstance.properties.ForgePreferences.FPref;
import forge.model.FModel;
import forge.screens.match.controllers.CPrompt;
import forge.toolbox.FButton;
import forge.toolbox.FHtmlViewer;
import forge.toolbox.FLabel;
import forge.toolbox.FScrollPane;
import forge.toolbox.FSkin;
import forge.util.Localizer;
import net.miginfocom.swing.MigLayout;

/**
 * Assembles Swing components of message report.
 * 
 * <br><br><i>(V at beginning of class name denotes a view class.)</i>
 */
public class VPrompt implements IVDoc<CPrompt> {

    // Fields used with interface IVDoc
    private DragCell parentCell;
    final Localizer localizer = Localizer.getInstance();
    private final DragTab tab = new DragTab(localizer.getMessage("lblPrompt"));

    // Various components
    private final FButton btnOK = new FButton(localizer.getMessage("lblOK"));
    private final FButton btnCancel = new FButton(localizer.getMessage("lblCancel"));
    private final FHtmlViewer tarMessage = new FHtmlViewer();
    private final FScrollPane messageScroller = new FScrollPane(tarMessage, false,
    		ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
    private final JLabel lblGames;
    private CardView card = null ;

    public void setCardView(final CardView card) {
	this.card = card ;
    }

    private KeyAdapter buttonKeyAdapter = new KeyAdapter() {
        @Override
        public void keyPressed(final KeyEvent e) {
            if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                // Try to cancel yield first if experimental options enabled
                if (FModel.getPreferences().getPrefBoolean(FPref.YIELD_EXPERIMENTAL_OPTIONS)) {
                    if (controller.getMatchUI() != null) {
                        PlayerView player = controller.getMatchUI().getCurrentPlayer();
                        if (player != null) {
                            YieldMode currentYield = controller.getMatchUI().getYieldMode(player);
                            if (currentYield != null && currentYield != YieldMode.NONE) {
                                controller.getMatchUI().clearYieldMode(player);
                                return;
                            }
                        }
                    }
                }
                // Existing ESC behavior
                if (btnCancel.isEnabled()) {
                    if (FModel.getPreferences().getPrefBoolean(FPref.UI_ALLOW_ESC_TO_END_TURN) || !btnCancel.getText().equals("End Turn")) {
                        btnCancel.doClick();
                    }
                }
            }
        }
    };

    private final CPrompt controller;

    //========= Constructor
    public VPrompt(final CPrompt controller) {
        this.controller = controller;

        lblGames = new FLabel.Builder()
        .fontSize(12)
        .fontStyle(Font.PLAIN)
        .fontAlign(SwingConstants.CENTER)
        .opaque()
        .build();

        btnOK.addKeyListener(buttonKeyAdapter);
        btnCancel.addKeyListener(buttonKeyAdapter);

        tarMessage.setForeground(FSkin.getColor(FSkin.Colors.CLR_TEXT));
        tarMessage.setMargin(new Insets(3, 3, 3, 3));
        tarMessage.getAccessibleContext().setAccessibleName("Prompt");
        tarMessage.setFocusable(true); // Allow tab to navigate to the prompt.
        messageScroller.getViewport().getView().addMouseListener(new MouseAdapter() {
        	@Override 
        	public void mouseEntered(final MouseEvent e) {
        		if ( card != null ) {
			    controller.getMatchUI().setCard(card);
        		}
        	}
        });
    }

    //========== Overridden methods

    /* (non-Javadoc)
     * @see forge.gui.framework.IVDoc#populate()
     */
    @Override
    public void populate() {
    	ForgePreferences prefs = FModel.getPreferences();
        JPanel container = parentCell.getBody();

        // wrap   : 2 columns required for btnOk and btnCancel.
        container.setLayout(new MigLayout("wrap 2, gap 0px!, insets 1px 1px 3px 1px"));
        if (prefs.getPrefBoolean(FPref.UI_COMPACT_PROMPT)) { //hide header and use smaller font if compact prompt
            tarMessage.setFont(FSkin.getFont());
        }
        else {
        	container.add(lblGames, "span 2, w 10:100%, h 22px!");
            tarMessage.setFont(FSkin.getRelativeFont(14));
        }
        lblGames.setText(localizer.getMessage("lblGameSetup"));

        container.add(messageScroller, "span 2, w 10:100%, h 0:100%");

        boolean largerButtons = prefs.getPrefBoolean(FPref.UI_FOR_TOUCHSCREN);
        String constraints = largerButtons ? "w 10:50%, h 40%:40%:60px" : "w 10:50%, hmin 24px";
        constraints += ", gaptop 2px!";

        container.add(btnOK, constraints);
        container.add(btnCancel, constraints);
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
        return EDocID.REPORT_MESSAGE;
    }

    /* (non-Javadoc)
     * @see forge.gui.framework.IVDoc#getLayoutControl()
     */
    @Override
    public CPrompt getLayoutControl() {
        return controller;
    }

    /* (non-Javadoc)
     * @see forge.gui.framework.IVDoc#getTabLabel()
     */
    @Override
    public DragTab getTabLabel() {
        return tab;
    }

    //========= Retrieval methods
    /** @return {@link javax.swing.JButton} */
    public FButton getBtnOK() {
        return this.btnOK;
    }

    /** @return {@link javax.swing.JButton} */
    public FButton getBtnCancel() {
        return this.btnCancel;
    }

    /** @return {@link javax.swing.JTextArea} */
    public FHtmlViewer getTarMessage() {
        return this.tarMessage;
    }

    /** @return {@link javax.swing.JLabel} */
    public JLabel getLblGames() {
        return this.lblGames;
    }

}
