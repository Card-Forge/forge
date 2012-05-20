/*
 * Forge: Play Magic: the Gathering.
 * Copyright (C) 2011  Nate
 *
 * This prog
import forge.gui.deckeditor.CDeckEditorUI;
ram is free software: you can redistribute it and/or modify
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
package forge.gui.toolbox;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.border.MatteBorder;

import net.miginfocom.swing.MigLayout;
import forge.AllZone;
import forge.Command;
import forge.Constant;
import forge.Singletons;
import forge.control.FControl;
import forge.deck.CardCollections;
import forge.deck.Deck;
import forge.deck.DeckBase;
import forge.game.GameType;
import forge.gui.deckeditor.CDeckEditorUI;
import forge.gui.deckeditor.controllers.ACEditorBase;
import forge.gui.deckeditor.controllers.CEditorConstructed;
import forge.gui.deckeditor.controllers.CEditorLimited;
import forge.gui.deckeditor.controllers.CEditorQuest;
import forge.gui.framework.ILocalRepaint;

/**
 * Creates deck list for selected decks for quick deleting, editing, and basic
 * info.
 * 
 */
@SuppressWarnings("serial")
public class DeckLister extends JPanel implements ILocalRepaint {
    private final ImageIcon icoDelete;
    private final ImageIcon icoDeleteOver;
    private final ImageIcon icoEdit;
    private final ImageIcon icoEditOver;
    private RowPanel previousSelect;
    private RowPanel[] rows;
    private final GameType gametype;
    private Command cmdDelete, cmdRowSelect;
    private final Color clrDefault, clrHover, clrActive, clrBorders;

    /**
     * Creates deck list for selected decks for quick deleting, editing, and
     * basic info. "selectable" and "editable" assumed true.
     *
     * @param gt0 the gt0
     * {@link forge.game.GameType}
     */
    public DeckLister(final GameType gt0) {
        this(gt0, null);
    }

    /**
     * Creates deck list for selected decks for quick deleting, editing, and
     * basic info. Set "selectable" and "editable" to show those buttons, or
     * not.
     *
     * @param gt0 the gt0
     * @param cmd0 the cmd0
     * {@link forge.game.GameType}
     * {@link forge.Command}, when exiting deck editor
     */
    public DeckLister(final GameType gt0, final Command cmd0) {
        super();
        this.gametype = gt0;

        this.clrDefault = new Color(0, 0, 0, 0);
        this.clrHover = FSkin.getColor(FSkin.Colors.CLR_HOVER);
        this.clrActive = FSkin.getColor(FSkin.Colors.CLR_ACTIVE);
        this.clrBorders = FSkin.getColor(FSkin.Colors.CLR_BORDERS);

        this.setOpaque(false);
        this.setLayout(new MigLayout("insets 0, gap 0, wrap"));

        this.icoDelete = FSkin.getIcon(FSkin.ForgeIcons.ICO_DELETE);
        this.icoDeleteOver = FSkin.getIcon(FSkin.ForgeIcons.ICO_DELETE_OVER);
        this.icoEdit = FSkin.getIcon(FSkin.ForgeIcons.ICO_EDIT);
        this.icoEditOver = FSkin.getIcon(FSkin.ForgeIcons.ICO_EDIT_OVER);
    }

    /**
     * Sets the decks.
     *
     * @param decks0 the new decks
     * {@link forge.deck.Deck}[]
     */
    public void setDecks(final Iterable<Deck> decks0) {
        this.removeAll();
        final List<RowPanel> tempRows = new ArrayList<RowPanel>();

        // Title row
        // Note: careful with the widths of the rows here;
        // scroll panes will have difficulty dynamically resizing if 100% width
        // is set.
        final JPanel rowTitle = new TitlePanel();
        rowTitle.setBackground(FSkin.getColor(FSkin.Colors.CLR_THEME2));
        rowTitle.setLayout(new MigLayout("insets 0, gap 0"));

        rowTitle.add(new FLabel.Builder().text("Delete").fontAlign(SwingConstants.CENTER).build(),
                "w 10%!, h 20px!, gaptop 5px");
        rowTitle.add(new FLabel.Builder().text("Edit")
                .fontScaleAuto(false).fontSize(14).fontAlign(SwingConstants.CENTER).build(),
                "w 10%!, h 20px!, gaptop 5px");
        rowTitle.add(new FLabel.Builder().text("Deck Name")
                .fontScaleAuto(false).fontSize(14).fontAlign(SwingConstants.CENTER).build(),
                "w 58%!, h 20px!, gaptop 5px");
        rowTitle.add(new FLabel.Builder().text("Main")
                .fontScaleAuto(false).fontSize(14).fontAlign(SwingConstants.CENTER).build(),
                "w 10%!, h 20px!, gaptop 5px");
        rowTitle.add(new FLabel.Builder().text("Side")
                .fontScaleAuto(false).fontSize(14).fontAlign(SwingConstants.CENTER).build(),
                "w 10%!, h 20px!, gaptop 5px");
        this.add(rowTitle, "w 98%!, h 30px!, gapleft 1%");

        RowPanel row;
        for (final Deck d : decks0) {
            if (d.getName() == null) {
                continue;
            }

            row = new RowPanel(d);
            row.add(new DeleteButton(row), "w 10%!, h 20px!, gaptop 5px");
            row.add(new EditButton(row), "w 10%!, h 20px!, gaptop 5px");
            row.add(new GenericLabel(d.getName()), "w 58%!, h 20px!, gaptop 5px");
            row.add(new MainLabel(String.valueOf(d.getMain().countAll())), "w 10%, h 20px!, gaptop 5px");
            row.add(new GenericLabel(String.valueOf(d.getSideboard().countAll())), "w 10%!, h 20px!, gaptop 5px");
            this.add(row, "w 98%!, h 30px!, gapleft 1%");
            tempRows.add(row);
        }

        this.rows = tempRows.toArray(new RowPanel[0]);
        this.revalidate();
    }

    /**
     * Gets the selected deck.
     *
     * @return {@link forge.deck.Deck}
     */
    public Deck getSelectedDeck() {
        Deck selectedDeck = null;
        for (final RowPanel r : this.rows) {
            if (r.isSelected()) {
                selectedDeck = r.getDeck();
            }
        }
        return selectedDeck;
    }

    /** Prevent panel from repainting the whole screen. */
    @Override
    public void repaintThis() {
        final Dimension d = DeckLister.this.getSize();
        this.repaint(0, 0, d.width, d.height);
    }

    private class DeleteButton extends JButton {
        public DeleteButton(final RowPanel r0) {
            super();
            this.setRolloverEnabled(true);
            this.setPressedIcon(DeckLister.this.icoDeleteOver);
            this.setRolloverIcon(DeckLister.this.icoDeleteOver);
            this.setIcon(DeckLister.this.icoDelete);
            this.setOpaque(false);
            this.setContentAreaFilled(false);
            this.setBorder(null);
            this.setBorderPainted(false);
            this.setToolTipText("Delete this deck");

            this.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(final MouseEvent e) {
                    if (!r0.selected) {
                        r0.setBackground(DeckLister.this.clrHover);
                        r0.setOpaque(true);
                    }
                }

                @Override
                public void mouseExited(final MouseEvent e) {
                    if (!r0.selected) {
                        r0.setBackground(DeckLister.this.clrDefault);
                        r0.setOpaque(false);
                    }
                }

                @Override
                public void mouseClicked(final MouseEvent e) {
                    DeckLister.this.deleteDeck(r0);
                }
            });
        }
    }

    private class EditButton extends JButton {
        public EditButton(final RowPanel r0) {
            super();
            this.setRolloverEnabled(true);
            this.setPressedIcon(DeckLister.this.icoEditOver);
            this.setRolloverIcon(DeckLister.this.icoEditOver);
            this.setIcon(DeckLister.this.icoEdit);
            this.setOpaque(false);
            this.setContentAreaFilled(false);
            this.setBorder(null);
            this.setBorderPainted(false);
            this.setToolTipText("Edit this deck");

            this.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(final MouseEvent e) {
                    if (!r0.selected) {
                        r0.setBackground(DeckLister.this.clrHover);
                        r0.setOpaque(true);
                    }
                }

                @Override
                public void mouseExited(final MouseEvent e) {
                    if (!r0.selected) {
                        r0.setBackground(DeckLister.this.clrDefault);
                        r0.setOpaque(false);
                    }
                }

                @Override
                public void mouseClicked(final MouseEvent e) {
                    DeckLister.this.editDeck(r0.getDeck());
                }
            });
        }
    }

    // Here only to prevent visual artifact problems from translucent skin
    // colors.
    private class TitlePanel extends JPanel {
        @Override
        public void paintComponent(final Graphics g) {
            g.setColor(this.getBackground());
            g.clearRect(0, 0, this.getWidth(), this.getHeight());
            g.fillRect(0, 0, this.getWidth(), this.getHeight());
            super.paintComponent(g);
        }
    }

    private class RowPanel extends JPanel {
        private boolean selected = false;
        private final Deck deck;

        public RowPanel(final Deck d0) {
            super();
            this.setOpaque(false);
            this.setBackground(new Color(0, 0, 0, 0));
            this.setLayout(new MigLayout("insets 0, gap 0"));
            this.setBorder(new MatteBorder(0, 0, 1, 0, DeckLister.this.clrBorders));
            this.deck = d0;

            this.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(final MouseEvent e) {
                    if (!RowPanel.this.selected) {
                        ((RowPanel) e.getSource()).setBackground(DeckLister.this.clrHover);
                        ((RowPanel) e.getSource()).setOpaque(true);
                    }
                }

                @Override
                public void mouseExited(final MouseEvent e) {
                    if (!RowPanel.this.selected) {
                        ((RowPanel) e.getSource()).setBackground(DeckLister.this.clrDefault);
                        ((RowPanel) e.getSource()).setOpaque(false);
                    }
                }

                @Override
                public void mousePressed(final MouseEvent e) {
                    DeckLister.this.selectHandler((RowPanel) e.getSource());
                }
            });
        }

        public void setSelected(final boolean b0) {
            this.selected = b0;
            this.setOpaque(b0);
            this.setBackground(b0 ? DeckLister.this.clrActive : DeckLister.this.clrHover);
        }

        public boolean isSelected() {
            return this.selected;
        }

        public Deck getDeck() {
            return this.deck;
        }
    }

    private class MainLabel extends JLabel {
        public MainLabel(final String txt0) {
            super(txt0);
            this.setOpaque(true);
            if (Integer.parseInt(txt0) < 40) {
                this.setBackground(Color.RED.brighter());
            } else {
                this.setBackground(Color.GREEN);
            }
            this.setHorizontalAlignment(SwingConstants.CENTER);
            this.setFont(FSkin.getBoldFont(12));
            this.setHorizontalAlignment(SwingConstants.CENTER);
        }
    }

    private class GenericLabel extends JLabel {
        public GenericLabel(final String txt0) {
            super(txt0);
            this.setHorizontalAlignment(SwingConstants.CENTER);
            this.setForeground(FSkin.getColor(FSkin.Colors.CLR_TEXT));
            this.setFont(FSkin.getBoldFont(12));
        }
    }

    /**
     * Gets the selected index.
     *
     * @return {@link java.lang.Integer}
     */
    public int getSelectedIndex() {
        for (int i = 0; i < this.rows.length; i++) {
            if (this.rows[i].isSelected()) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Selects a row programatically.
     * 
     * @param i0
     *            &emsp; int
     * @return boolean Was able to select, or not.
     */
    public boolean setSelectedIndex(final int i0) {
        if (i0 >= this.rows.length) {
            return false;
        }
        this.selectHandler(this.rows[i0]);
        return true;
    }

    /**
     * Sets the selected deck.
     *
     * @param d0 &emsp; Deck object to select (if exists in list)
     * @return boolean Found deck, or didn't.
     */
    public boolean setSelectedDeck(final Deck d0) {
        for (final RowPanel r : this.rows) {
            if (r.getDeck() == d0) {
                this.selectHandler(r);
                return true;
            }
        }
        return false;
    }

    /**
     * Sets the delete command.
     *
     * @param c0 &emsp; {@link forge.Command} command executed on delete.
     */
    public void setDeleteCommand(final Command c0) {
        this.cmdDelete = c0;
    }

    /**
     * Sets the select command.
     *
     * @param c0 &emsp; {@link forge.Command} command executed on row select.
     */
    public void setSelectCommand(final Command c0) {
        this.cmdRowSelect = c0;
    }

    private void selectHandler(final RowPanel r0) {
        if (this.previousSelect != null) {
            this.previousSelect.setSelected(false);
        }
        r0.setSelected(true);
        this.previousSelect = r0;

        if (this.cmdRowSelect != null) {
            this.cmdRowSelect.execute();
        }
    }
    @SuppressWarnings("unchecked")
    private <T extends DeckBase> void editDeck(final Deck d0) {
        switch (this.gametype) {
            case Quest:
                    Constant.Runtime.HUMAN_DECK[0] = d0;
                    final CEditorQuest qEditor = new CEditorQuest(AllZone.getQuest());
                    CDeckEditorUI.SINGLETON_INSTANCE.setCurrentEditorController(qEditor);
                    FControl.SINGLETON_INSTANCE.changeState(FControl.DECK_EDITOR_QUEST);
                break;
            case Constructed:
                    CDeckEditorUI.SINGLETON_INSTANCE.setCurrentEditorController(new CEditorConstructed());
                    FControl.SINGLETON_INSTANCE.changeState(FControl.DECK_EDITOR_CONSTRUCTED);
                    CDeckEditorUI.SINGLETON_INSTANCE.getCurrentEditorController().getDeckController().load(d0.getName());
                break;
            case Sealed:
                final ACEditorBase<?, T> sEditor = (ACEditorBase<?, T>)
                    new CEditorLimited(Singletons.getModel().getDecks().getSealed());

                CDeckEditorUI.SINGLETON_INSTANCE.setCurrentEditorController(sEditor);

                sEditor.getDeckController().load(d0.getName());
                FControl.SINGLETON_INSTANCE.changeState(FControl.DECK_EDITOR_LIMITED);
                break;
            case Draft:
                final ACEditorBase<?, T> dEditor = (ACEditorBase<?, T>)
                        new CEditorLimited(Singletons.getModel().getDecks().getDraft());
                CDeckEditorUI.SINGLETON_INSTANCE.setCurrentEditorController(dEditor);

                dEditor.getDeckController().load(d0.getName());
                FControl.SINGLETON_INSTANCE.changeState(FControl.DECK_EDITOR_LIMITED);
                break;
            default:
                break;
        }
    }

    private void deleteDeck(final RowPanel r0) {
        final Deck d0 = r0.getDeck();

        final int n = JOptionPane.showConfirmDialog(null, "Are you sure you want to delete \"" + d0.getName() + "\" ?",
                "Delete Deck", JOptionPane.YES_NO_OPTION);

        if (n == JOptionPane.NO_OPTION) {
            return;
        }

        final CardCollections deckManager = Singletons.getModel().getDecks();

        if (this.gametype.equals(GameType.Draft)) {
            deckManager.getDraft().delete(d0.getName());
        } else if (this.gametype.equals(GameType.Sealed)) {
            deckManager.getSealed().delete(d0.getName());
        } else if (this.gametype.equals(GameType.Quest)) {
            AllZone.getQuest().getMyDecks().delete(d0.getName());
            AllZone.getQuest().save();
        } else {
            deckManager.getConstructed().delete(d0.getName());
        }

        this.remove(r0);
        this.repaintThis();
        this.revalidate();

        if (this.cmdDelete != null) {
            this.cmdDelete.execute();
        }
    }
}
