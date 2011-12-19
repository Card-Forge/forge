package forge.view.toolbox;

import java.awt.Color;
import java.awt.Font;
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
import forge.deck.Deck;
import forge.deck.DeckManager;
import forge.game.GameType;
import forge.gui.deckeditor.DeckEditorCommon;

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
    private FSkin skin;
    private RowPanel previousSelection = null;
    private RowPanel[] rows;
    private GameType gametype;
    private Command cmdExit;

    /**
     * Creates deck list for selected decks for quick deleting, editing, and basic info.
     * "selectable" and "editable" assumed true.
     *
     * @param gt0 {@link forge.game.GameType}
     * @param cmd0 {@link forge.Command}, when exiting deck editor
     */
    public DeckLister(GameType gt0, Command cmd0) {
        this(gt0, cmd0, true, true);
    }

    /**
     * Creates deck list for selected decks for quick deleting, editing, and basic info.
     * Set "selectable" and "editable" to show those buttons, or not.
     * 
     * @param gt0 {@link forge.game.GameType}
     * @param cmd0 {@link forge.Command}, when exiting deck editor
     * @param deletable {@link java.lang.Boolean}
     * @param editable {@link java.lang.Boolean}
     */
    public DeckLister(GameType gt0, Command cmd0, boolean deletable, boolean editable) {
        super();
        skin = AllZone.getSkin();
        gametype = gt0;
        cmdExit = cmd0;
        this.setOpaque(false);
        this.setLayout(new MigLayout("insets 0, gap 0, wrap"));

        icoDelete = new ImageIcon("res/images/icons/DeleteIcon.png");
        icoDeleteOver = new ImageIcon("res/images/icons/DeleteIconOver.png");
        icoEdit = new ImageIcon("res/images/icons/EditIcon.png");
        icoEditOver = new ImageIcon("res/images/icons/EditIconOver.png");
    }

    /** @param decks0 {@link forge.deck.Deck}[] */
    public void setDecks(Deck[] decks0) {
        this.removeAll();
        List<RowPanel> tempRows = new ArrayList<RowPanel>();

        // Title row
        // Note: careful with the widths of the rows here;
        // scroll panes will have difficulty dynamically resizing if 100% width is set.
        JPanel rowTitle = new JPanel();
        rowTitle.setBackground(skin.getColor("inactive"));
        rowTitle.setLayout(new MigLayout("insets 0, gap 0"));
        rowTitle.add(new TitleLabel("Delete"), "w 10%!, h 20px!, gaptop 5px");
        rowTitle.add(new TitleLabel("Edit"), "w 10%!, h 20px!, gaptop 5px");
        rowTitle.add(new TitleLabel("Deck Name"), "w 60%!, h 20px!, gaptop 5px");
        rowTitle.add(new TitleLabel("Main"), "w 10%!, h 20px!, gaptop 5px");
        rowTitle.add(new TitleLabel("Side"), "w 10%!, h 20px!, gaptop 5px");
        this.add(rowTitle, "w 98%!, h 30px!, gapleft 1%");

        RowPanel row;
        for (Deck d : decks0) {
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
                    if (r0.selected) { return; }
                    r0.setBackground(skin.getColor("hover"));
                    r0.setOpaque(true);
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
                    if (r0.selected) { return; }
                    r0.setBackground(skin.getColor("hover"));
                    r0.setOpaque(true);
                }
                @Override
                public void mouseClicked(MouseEvent e) {
                    editDeck(r0.getDeck());
                }
            });
        }
    }

    private class RowPanel extends JPanel {
        private Color bgDefault = null;
        private boolean selected = false;
        private Deck deck;

        public RowPanel(Deck d0) {
            super();
            setOpaque(false);
            setLayout(new MigLayout("insets 0, gap 0"));
            setBorder(new MatteBorder(0, 0, 1, 0, skin.getColor("borders")));
            deck = d0;

            this.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    if (selected) { return; }
                    setBackground(skin.getColor("hover"));
                    setOpaque(true);
                }
                @Override
                public void mouseExited(MouseEvent e) {
                    if (selected) { return; }
                    setBackground(bgDefault);
                    setOpaque(false);
                }
                @Override
                public void mousePressed(MouseEvent e) {
                    selectHandler((RowPanel) e.getSource());
                }
            });
        }

        public void setSelected(boolean b0) {
            bgDefault = (b0 ? skin.getColor("active") : null);
            selected = b0;
            setBackground(bgDefault);
        }

        public boolean isSelected() {
            return selected;
        }

        public Deck getDeck() {
            return deck;
        }
    }

    private class TitleLabel extends JLabel {
        public TitleLabel(String txt0) {
            super(txt0);
            setForeground(skin.getColor("text"));
            setFont(skin.getFont1().deriveFont(Font.PLAIN, 11));
            setHorizontalAlignment(SwingConstants.CENTER);
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
            setFont(skin.getFont1().deriveFont(Font.BOLD, 12));
            setHorizontalAlignment(SwingConstants.CENTER);
        }
    }

    private class GenericLabel extends JLabel {
        public GenericLabel(String txt0) {
            super(txt0);
            setHorizontalAlignment(SwingConstants.CENTER);
            setForeground(skin.getColor("text"));
            setFont(skin.getFont1().deriveFont(Font.BOLD, 12));
            setHorizontalAlignment(SwingConstants.CENTER);
        }
    }


    /** @return {@link java.lang.Integer} */
    public int getSelectedIndex() {
        for (int i = 0; i < rows.length; i++) {
            if (rows[i].isSelected()) { return i; }
        }
        return -1;
    }

    private void selectHandler(RowPanel r0) {
        if (previousSelection != null) {
            previousSelection.setSelected(false);
        }
        r0.setSelected(true);
        previousSelection = r0;
    }

    private void editDeck(Deck d0) {
        DeckEditorCommon editor = new DeckEditorCommon(gametype);
        editor.show(cmdExit);
        editor.getCustomMenu().showDeck(d0, gametype);
        editor.setVisible(true);
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
            File dir = DeckManager.makeFileName(d0.getName(), GameType.Draft);
            String[] children = dir.list();

            for (int i = 0; i < children.length; i++) {
                new File(dir.getAbsolutePath() + "\\" + children[i]).delete();
            }

            dir.delete();
        }
        else if (gametype.equals(GameType.Sealed)) {
            deckmanager.deleteDeck(d0.getName());

            File address1 = DeckManager.makeFileName(d0.getName(), GameType.Sealed);
            File address2 = DeckManager.makeFileName("AI_" + d0.getName(), GameType.Sealed);

            // not working??!!
            address1.delete();
            address2.delete();
        }
        else {
            deckmanager.deleteDeck(d0.getName());

            File address1 = DeckManager.makeFileName(d0.getName(), GameType.Constructed);
            address1.delete();
        }

        this.remove(r0);
        this.revalidate();
    }
}
