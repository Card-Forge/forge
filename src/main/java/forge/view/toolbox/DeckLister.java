package forge.view.toolbox;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
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
import forge.deck.Deck;
import forge.deck.DeckIO;
import forge.deck.DeckManager;
import forge.game.GameType;
import forge.gui.deckeditor.DeckEditorCommon;
import forge.gui.deckeditor.DeckEditorQuest;

/** 
 * Creates deck list for selected decks for quick deleting, editing, and basic info.
 *
 */
@SuppressWarnings("serial")
public class DeckLister extends JPanel {
    private ImageIcon icoDelete;
    private ImageIcon icoDeleteOver;
    private ImageIcon icoEdit;
    private ImageIcon icoEditOver;
    private RowPanel previousSelect;
    private RowPanel[] rows;
    private GameType gametype;
    private Command cmdEditorExit, cmdDelete, cmdRowSelect;
    private final Color clrDefault, clrHover, clrActive, clrBorders;

    /**
     * Creates deck list for selected decks for quick deleting, editing, and basic info.
     * "selectable" and "editable" assumed true.
     *
     * @param gt0 {@link forge.game.GameType}
     */
    public DeckLister(GameType gt0) {
        this(gt0, null);
    }

    /**
     * Creates deck list for selected decks for quick deleting, editing, and basic info.
     * Set "selectable" and "editable" to show those buttons, or not.
     * 
     * @param gt0 {@link forge.game.GameType}
     * @param cmd0 {@link forge.Command}, when exiting deck editor
     */
    public DeckLister(GameType gt0, Command cmd0) {
        super();
        this.gametype = gt0;
        this.cmdEditorExit = cmd0;

        this.clrDefault = new Color(0, 0, 0, 0);
        this.clrHover = FSkin.getColor(FSkin.Colors.CLR_HOVER);
        this.clrActive = FSkin.getColor(FSkin.Colors.CLR_ACTIVE);
        this.clrBorders = FSkin.getColor(FSkin.Colors.CLR_BORDERS);

        this.setOpaque(false);
        this.setLayout(new MigLayout("insets 0, gap 0, wrap"));

        icoDelete = FSkin.getIcon(FSkin.ForgeIcons.ICO_DELETE);
        icoDeleteOver = FSkin.getIcon(FSkin.ForgeIcons.ICO_DELETE_OVER);
        icoEdit = FSkin.getIcon(FSkin.ForgeIcons.ICO_EDIT);
        icoEditOver = FSkin.getIcon(FSkin.ForgeIcons.ICO_EDIT_OVER);
    }

    /** @param decks0 {@link forge.deck.Deck}[] */
    public void setDecks(Deck[] decks0) {
        this.removeAll();
        List<RowPanel> tempRows = new ArrayList<RowPanel>();

        // Title row
        // Note: careful with the widths of the rows here;
        // scroll panes will have difficulty dynamically resizing if 100% width is set.
        JPanel rowTitle = new TitlePanel();
        rowTitle.setBackground(FSkin.getColor(FSkin.Colors.CLR_ZEBRA));
        rowTitle.setLayout(new MigLayout("insets 0, gap 0"));

        rowTitle.add(new FLabel.Builder().text("Delete").fontAlign(SwingConstants.CENTER).build(),
                "w 10%!, h 20px!, gaptop 5px");
        rowTitle.add(new FLabel.Builder().text("Edit").fontAlign(SwingConstants.CENTER).build(),
                "w 10%!, h 20px!, gaptop 5px");
        rowTitle.add(new FLabel.Builder().text("Deck Name").fontAlign(SwingConstants.CENTER).build(), "w 60%!, h 20px!, gaptop 5px");
        rowTitle.add(new FLabel.Builder().text("Main").fontAlign(SwingConstants.CENTER).build(),
                "w 10%!, h 20px!, gaptop 5px");
        rowTitle.add(new FLabel.Builder().text("Side").fontAlign(SwingConstants.CENTER).build(),
                "w 10%!, h 20px!, gaptop 5px");
        this.add(rowTitle, "w 98%!, h 30px!, gapleft 1%");

        RowPanel row;
        for (Deck d : decks0) {
            if (d.getName() == null) { continue; }

            row = new RowPanel(d);
            row.add(new DeleteButton(row), "w 10%!, h 20px!, gaptop 5px");
            row.add(new EditButton(row), "w 10%!, h 20px!, gaptop 5px");
            row.add(new GenericLabel(d.getName()), "w 60%!, h 20px!, gaptop 5px");
            row.add(new MainLabel(String.valueOf(d.getMain().countAll())), "w 10%, h 20px!, gaptop 5px");
            row.add(new GenericLabel(String.valueOf(d.getSideboard().countAll())), "w 10%!, h 20px!, gaptop 5px");
            this.add(row, "w 98%!, h 30px!, gapleft 1%");
            tempRows.add(row);
        }

        rows = tempRows.toArray(new RowPanel[0]);
        revalidate();
    }

    /** @return {@link forge.deck.Deck} */
    public Deck getSelectedDeck() {
        Deck selectedDeck = null;
        for (RowPanel r : rows) {
            if (r.isSelected()) {
                selectedDeck = r.getDeck();
            }
        }
        return selectedDeck;
    }

    private class DeleteButton extends JButton {
        public DeleteButton(final RowPanel r0) {
            super();
            setRolloverEnabled(true);
            setPressedIcon(icoDeleteOver);
            setRolloverIcon(icoDeleteOver);
            setIcon(icoDelete);
            setOpaque(false);
            setContentAreaFilled(false);
            setBorder(null);
            setBorderPainted(false);
            setToolTipText("Delete this deck");

            this.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    if (!r0.selected) {
                        r0.setBackground(clrHover);
                        r0.setOpaque(true);
                    }
                }
                @Override
                public void mouseExited(MouseEvent e) {
                    if (!r0.selected) {
                        r0.setBackground(clrDefault);
                        r0.setOpaque(false);
                    }
                }
                @Override
                public void mouseClicked(MouseEvent e) {
                    deleteDeck(r0);
                }
            });
        }
    }

    private class EditButton extends JButton {
        public EditButton(final RowPanel r0) {
            super();
            setRolloverEnabled(true);
            setPressedIcon(icoEditOver);
            setRolloverIcon(icoEditOver);
            setIcon(icoEdit);
            setOpaque(false);
            setContentAreaFilled(false);
            setBorder(null);
            setBorderPainted(false);
            setToolTipText("Edit this deck");

            this.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    if (!r0.selected) {
                        r0.setBackground(clrHover);
                        r0.setOpaque(true);
                    }
                }
                @Override
                public void mouseExited(MouseEvent e) {
                    if (!r0.selected) {
                        r0.setBackground(clrDefault);
                        r0.setOpaque(false);
                    }
                }
                @Override
                public void mouseClicked(MouseEvent e) {
                    editDeck(r0.getDeck());
                }
            });
        }
    }

    // Here only to prevent visual artifact problems from translucent skin colors.
    private class TitlePanel extends JPanel {
        @Override
        public void paintComponent(Graphics g) {
            g.setColor(getBackground());
            g.clearRect(0, 0, getWidth(), getHeight());
            g.fillRect(0, 0, getWidth(), getHeight());
            super.paintComponent(g);
        }
    }

    private class RowPanel extends JPanel {
        private boolean selected = false;
        private Deck deck;

        public RowPanel(Deck d0) {
            super();
            setOpaque(false);
            setBackground(new Color(0, 0, 0, 0));
            setLayout(new MigLayout("insets 0, gap 0"));
            setBorder(new MatteBorder(0, 0, 1, 0, clrBorders));
            deck = d0;

            this.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    if (!selected) {
                        ((RowPanel) e.getSource()).setBackground(clrHover);
                        ((RowPanel) e.getSource()).setOpaque(true);
                    }
                }
                @Override
                public void mouseExited(MouseEvent e) {
                    if (!selected) {
                        ((RowPanel) e.getSource()).setBackground(clrDefault);
                        ((RowPanel) e.getSource()).setOpaque(false);
                    }
                }
                @Override
                public void mousePressed(MouseEvent e) {
                    selectHandler((RowPanel) e.getSource());
                }
            });
        }

        public void setSelected(boolean b0) {
            selected = b0;
            setOpaque(b0);
            setBackground(b0 ? clrActive : clrHover);
        }

        public boolean isSelected() {
            return selected;
        }

        public Deck getDeck() {
            return deck;
        }
    }

    private class MainLabel extends JLabel {
        public MainLabel(String txt0) {
            super(txt0);
            setOpaque(true);
            if (Integer.parseInt(txt0) < 40) {
                setBackground(Color.RED.brighter());
            }
            else {
                setBackground(Color.GREEN);
            }
            setHorizontalAlignment(SwingConstants.CENTER);
            setFont(FSkin.getBoldFont(12));
            setHorizontalAlignment(SwingConstants.CENTER);
        }
    }

    private class GenericLabel extends JLabel {
        public GenericLabel(String txt0) {
            super(txt0);
            setHorizontalAlignment(SwingConstants.CENTER);
            setForeground(FSkin.getColor(FSkin.Colors.CLR_TEXT));
            setFont(FSkin.getBoldFont(12));
        }
    }


    /** @return {@link java.lang.Integer} */
    public int getSelectedIndex() {
        for (int i = 0; i < rows.length; i++) {
            if (rows[i].isSelected()) { return i; }
        }
        return -1;
    }

    /** Selects a row programatically.
     * @param i0 &emsp; int
     * @return boolean Was able to select, or not.
     */
    public boolean setSelectedIndex(int i0) {
        if (i0 >= rows.length) { return false; }
        selectHandler(rows[i0]);
        return true;
    }

    /** 
     * @param d0 &emsp; Deck object to select (if exists in list)
     * @return boolean Found deck, or didn't.
     */
    public boolean setSelectedDeck(Deck d0) {
        for (RowPanel r : rows) {
            if (r.getDeck() == d0) {
                selectHandler(r);
                return true;
            }
        }
        return false;
    }

    /** @param c0 &emsp; {@link forge.Command} command executed on delete. */
    public void setDeleteCommand(Command c0) {
        this.cmdDelete = c0;
    }

    /** @param c0 &emsp; {@link forge.Command} command executed on row select. */
    public void setSelectCommand(Command c0) {
        this.cmdRowSelect = c0;
    }

    /** @param c0 &emsp; {@link forge.Command} command executed on editor exit. */
    public void setExitCommand(Command c0) {
        this.cmdEditorExit = c0;
    }

    private void selectHandler(RowPanel r0) {
        if (previousSelect != null) {
            previousSelect.setSelected(false);
        }
        r0.setSelected(true);
        previousSelect = r0;

        if (cmdRowSelect != null) { cmdRowSelect.execute(); }
    }

    private void editDeck(Deck d0) {
        if (gametype == GameType.Quest) {
            Constant.Runtime.HUMAN_DECK[0] = d0;
            final DeckEditorQuest editor = new DeckEditorQuest(AllZone.getQuestData());
            editor.show(cmdEditorExit);
            editor.setVisible(true);
        }
        else {
            final DeckEditorCommon editor = new DeckEditorCommon(gametype);
            editor.show(cmdEditorExit);
            editor.getCustomMenu().showDeck(d0, gametype);
            editor.setVisible(true);
        }
    }

    private void deleteDeck(RowPanel r0) {
        Deck d0 = r0.getDeck();

        final int n = JOptionPane.showConfirmDialog(null,
                "Are you sure you want to delete \"" + d0.getName()
                + "\" ?", "Delete Deck", JOptionPane.YES_NO_OPTION);

        if (n == JOptionPane.NO_OPTION) {
            return;
        }

        DeckManager deckmanager = AllZone.getDeckManager();

        if (gametype.equals(GameType.Draft)) {
            deckmanager.deleteDraftDeck(d0.getName());

            // Since draft deck files are really directories, must delete all children first.
            File dir = DeckIO.makeFileName(d0.getName(), GameType.Draft);
            String[] children = dir.list();

            for (int i = 0; i < children.length; i++) {
                new File(dir.getAbsolutePath() + File.separator + children[i]).delete();
            }

            dir.delete();
        }
        else if (gametype.equals(GameType.Sealed)) {
            deckmanager.deleteDeck(d0.getName());

            File address1 = DeckIO.makeFileName(d0.getName(), GameType.Sealed);
            File address2 = DeckIO.makeFileName("AI_" + d0.getName(), GameType.Sealed);

            // not working??!!
            address1.delete();
            address2.delete();
        }
        else if (gametype.equals(GameType.Quest)) {
            AllZone.getQuestData().removeDeck(d0.getName());
            AllZone.getQuestData().saveData();
            Singletons.getView().getHomeView().getBtnQuest().grabFocus();
        }
        else {
            deckmanager.deleteDeck(d0.getName());

            File address1 = DeckIO.makeFileName(d0.getName(), GameType.Constructed);
            address1.delete();
        }

        this.remove(r0);
        this.repaint();
        this.revalidate();

        if (cmdDelete != null) { cmdDelete.execute(); }
    }
}
