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
package forge.gui.match.views;

import java.awt.Font;
import java.awt.Insets;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;
import javax.swing.text.DefaultCaret;

import net.miginfocom.swing.MigLayout;
import forge.Singletons;
import forge.gui.framework.DragCell;
import forge.gui.framework.DragTab;
import forge.gui.framework.EDocID;
import forge.gui.framework.IVDoc;
import forge.gui.match.controllers.CMessage;
import forge.gui.toolbox.FButton;
import forge.gui.toolbox.FLabel;
import forge.gui.toolbox.FSkin;
import forge.gui.toolbox.FSkin.JTextComponentSkin;
import forge.properties.ForgePreferences.FPref;

/**
 * Assembles Swing components of message report.
 * 
 * <br><br><i>(V at beginning of class name denotes a view class.)</i>
 */
public enum VMessage implements IVDoc<CMessage> {
    /** */
    SINGLETON_INSTANCE;

    // Fields used with interface IVDoc
    private DragCell parentCell;
    private final DragTab tab = new DragTab("Prompt");

    // Various components
    private final JButton btnOK = new FButton("OK");
    private final JButton btnCancel = new FButton("Cancel");
    private final JTextArea tarMessage = new JTextArea();
    private final JLabel lblGames;

    //========= Constructor
    private VMessage() {

        lblGames = new FLabel.Builder()
        .fontSize(12)
        .fontStyle(Font.PLAIN)
        .fontAlign(SwingConstants.CENTER)
        .opaque()
        .build();

        JTextComponentSkin<JTextArea> tarMessageSkin = FSkin.get(tarMessage);
        tarMessage.setOpaque(false);
        tarMessage.setFocusable(false);
        tarMessage.setEditable(false);
        tarMessage.setLineWrap(true);
        tarMessage.setWrapStyleWord(true);
        tarMessageSkin.setForeground(FSkin.getColor(FSkin.Colors.CLR_TEXT));
        tarMessageSkin.setFont(FSkin.getFont(14));
        tarMessage.setMargin(new Insets(5, 5, 5, 5));

        // Prevent scroll-bar from automatically scrolling to bottom of JTextArea.
        DefaultCaret caret = (DefaultCaret)tarMessage.getCaret();
        caret.setUpdatePolicy(DefaultCaret.NEVER_UPDATE);

    }

    //========== Overridden methods

    /* (non-Javadoc)
     * @see forge.gui.framework.IVDoc#populate()
     */
    @Override
    public void populate() {

        JPanel container = parentCell.getBody();

        // wrap   : 2 columns required for btnOk and btnCancel.
        container.setLayout(new MigLayout("wrap 2, gap 0px!, insets 1px 1px 5px 1px"));
        container.add(lblGames, "span 2, w 10:100%, h 22px!");
        lblGames.setText("Game Setup");

        JScrollPane scrollPane = new JScrollPane(tarMessage);
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.setBorder(null);
        container.add(scrollPane, "span 2, w 10:100%, h 0:100%");

        boolean largerButtons = Singletons.getModel().getPreferences().getPrefBoolean(FPref.UI_FOR_TOUCHSCREN);
        String constraints = largerButtons ? "w 10:50%, h 40%:40%:60px" : "w 10:50%, hmin 24px";
        constraints += ", gaptop 4px!";

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
    public CMessage getLayoutControl() {
        return CMessage.SINGLETON_INSTANCE;
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
    public JButton getBtnOK() {
        return this.btnOK;
    }

    /** @return {@link javax.swing.JButton} */
    public JButton getBtnCancel() {
        return this.btnCancel;
    }

    /** @return {@link javax.swing.JTextArea} */
    public JTextArea getTarMessage() {
        return this.tarMessage;
    }

    /** @return {@link javax.swing.JLabel} */
    public JLabel getLblGames() {
        return this.lblGames;
    }
}
