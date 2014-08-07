package forge.screens.home.gauntlet;

import forge.UiCommand;
import forge.assets.FSkinProp;
import forge.gauntlet.GauntletData;
import forge.gauntlet.GauntletIO;
import forge.toolbox.FLabel;
import forge.toolbox.FOptionPane;
import forge.toolbox.FSkin;
import forge.toolbox.FSkin.SkinIcon;
import forge.toolbox.FSkin.SkinnedButton;
import forge.toolbox.FSkin.SkinnedPanel;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import javax.swing.border.Border;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/** 
 * Creates file list/table for quick deleting, editing, and basic info.
 *
 */
@SuppressWarnings("serial")
public class QuickGauntletLister extends JPanel {
    private SkinIcon icoDelete, icoDeleteOver;
    private RowPanel previousSelect;
    private RowPanel[] rows;
    private UiCommand cmdRowSelect, cmdRowDelete;
    private final Color clrDefault;
    private final FSkin.SkinColor clrHover, clrActive, clrBorders;

    /** */
    public QuickGauntletLister() {
        super();

        this.clrDefault = new Color(0, 0, 0, 0);
        this.clrHover = FSkin.getColor(FSkin.Colors.CLR_HOVER);
        this.clrActive = FSkin.getColor(FSkin.Colors.CLR_ACTIVE);
        this.clrBorders = FSkin.getColor(FSkin.Colors.CLR_BORDERS);

        this.setOpaque(false);
        this.setLayout(new MigLayout("insets 0, gap 0, wrap"));

        icoDelete = FSkin.getIcon(FSkinProp.ICO_DELETE);
        icoDeleteOver = FSkin.getIcon(FSkinProp.ICO_DELETE_OVER);
    }

    /** @param gd0 &emsp; {@link forge.gauntlet.GauntletData}[] */
    public void setGauntlets(List<GauntletData> gd0) {
        this.removeAll();
        List<RowPanel> tempRows = new ArrayList<RowPanel>();
        List<GauntletData> sorted = new ArrayList<GauntletData>();
        for (GauntletData gd : gd0) { sorted.add(gd); }
        Collections.sort(sorted, new Comparator<GauntletData>() {
            @Override
            public int compare(final GauntletData x, final GauntletData y) {
                return x.getName().compareTo(y.getName());
            }
        });

        // Title row
        // Note: careful with the widths of the rows here;
        // scroll panes will have difficulty dynamically resizing if 100% width is set.
        final SkinnedPanel rowTitle = new SkinnedPanel();
        rowTitle.setBackground(FSkin.getColor(FSkin.Colors.CLR_ZEBRA));
        rowTitle.setLayout(new MigLayout("insets 0, gap 0"));
        rowTitle.add(new FLabel.Builder().build(),
                "w 30px!, h 20px!, gap 1% 0 5px 0");
        rowTitle.add(new FLabel.Builder().text("Name").fontAlign(SwingConstants.LEFT).build(),
                "w 49% - 175px!, h 20px!, gap 20px 0 5px 0");
        rowTitle.add(new FLabel.Builder().text("Your Deck").fontAlign(SwingConstants.LEFT).build(),
                "w 49% - 175px!, h 20px!, gap 0 0 5px 0");
        rowTitle.add(new FLabel.Builder().text("Last Activity").fontAlign(SwingConstants.CENTER).build(),
                "w 100px!, h 20px!, gap 0 0 5px 0");
        rowTitle.add(new FLabel.Builder().text("Opponents").fontAlign(SwingConstants.CENTER).build(),
                "w 100px!, h 20px!, gap 0 0 5px 0");
        rowTitle.add(new FLabel.Builder().text("Progress").fontAlign(SwingConstants.CENTER).build(),
                "w 100px!, h 20px!, gap 0 0 5px 0");
        this.add(rowTitle, "w 98%!, h 30px!, gapleft 1%");

        RowPanel row;
        String name;
        for (GauntletData gd : sorted) {
            name = gd.getName();

            row = new RowPanel(gd);
            row.setToolTipText(name);

            row.add(new DeleteButton(row),
                    "w 30px!, h 20px!, gap 1% 0 5px 0");
            row.add(new FLabel.Builder().fontAlign(SwingConstants.LEFT).text(name).build(),
                    "w 49% - 175px!, h 20px!, gap 20px 0 5px 0");
            row.add(new FLabel.Builder().text(gd.getUserDeck() == null ? "(none)" : gd.getUserDeck().getName()).fontAlign(SwingConstants.LEFT).build(),
                    "w 49% - 175px!, h 20px!, gap 0 0 5px 0");
            row.add(new FLabel.Builder().text(gd.getTimestamp()).fontAlign(SwingConstants.CENTER).build(),
                    "w 100px!, h 20px!, gap 0 0 5px 0");
            row.add(new FLabel.Builder().text(String.valueOf(gd.getDecks().size()))
                    .fontAlign(SwingConstants.CENTER).build(),
                    "w 100px!, h 20px!, gap 0 0 5px 0");
            row.add(new FLabel.Builder().text(String.valueOf(Math.round(
                    ((double) gd.getCompleted() / (double) gd.getDecks().size()) * 100)) + " %")
                    .fontAlign(SwingConstants.CENTER).build(),
                    "w 100px!, h 20px!, gap 0 0 5px 0");
            this.add(row, "w 98%!, h 30px!, gap 1% 0 0 0");
            tempRows.add(row);
        }

        rows = tempRows.toArray(new RowPanel[0]);
        revalidate();
    }

    /** @return {@link forge.deck.Deck} */
    public File getSelectedGauntletFile() {
        if (previousSelect == null) {
            return null;
        }
        else {
            return GauntletIO.getGauntletFile(previousSelect.getGauntletData());
        }
    }

    private class DeleteButton extends SkinnedButton {
        public DeleteButton(final RowPanel r0) {
            super();
            setRolloverEnabled(true);
            setPressedIcon(icoDeleteOver);
            setRolloverIcon(icoDeleteOver);
            setIcon(icoDelete);
            setOpaque(false);
            setContentAreaFilled(false);
            setBorder((Border)null);
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
                    deleteFile(r0);
                }
            });
        }
    }

    private class RowPanel extends SkinnedPanel {
        private boolean selected = false;
        private GauntletData gauntletData;

        public RowPanel(GauntletData gd0) {
            super();
            setOpaque(false);
            setBackground(new Color(0, 0, 0, 0));
            setLayout(new MigLayout("insets 0, gap 0"));
            this.setBorder(new FSkin.MatteSkinBorder(0, 0, 1, 0, clrBorders));
            gauntletData = gd0;

            this.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    if (!selected) {
                        RowPanel.this.setBackground(clrHover);
                        RowPanel.this.setOpaque(true);
                    }
                }
                @Override
                public void mouseExited(MouseEvent e) {
                    if (!selected) {
                        RowPanel.this.setBackground(clrDefault);
                        RowPanel.this.setOpaque(false);
                    }
                }
                @Override
                public void mousePressed(MouseEvent e) {
                    selectHandler(RowPanel.this);
                }
            });
        }

        public void setSelected(boolean b0) {
            selected = b0;
            setOpaque(b0);
            this.setBackground(b0 ? clrActive : clrHover);
        }

        public boolean isSelected() {
            return selected;
        }

        public GauntletData getGauntletData() {
            return gauntletData;
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
     * @return boolean success
     */
    public boolean setSelectedIndex(int i0) {
        if (i0 >= rows.length) { return false; }
        selectHandler(rows[Math.max(0, i0)]);
        return true;
    }

    /**
     * @param qd0 &emsp; Gauntlet data object to select (if exists in list)
     * @return boolean success
     */
    public boolean setSelectedGauntletData(GauntletData qd0) {
        /*for (RowPanel r : rows) {
            if (r.getQuestData().getName().equals(qd0.getName())) {
                selectHandler(r);
                return true;
            }
        }*/
        return false;
    }

    /** @param c0 &emsp; {@link forge.UiCommand} command executed on row select. */
    public void setCmdSelect(UiCommand c0) {
        this.cmdRowSelect = c0;
    }

    /** @param c0 &emsp; {@link forge.UiCommand} command executed on row delete. */
    public void setCmdDelete(UiCommand c0) {
        this.cmdRowDelete = c0;
    }

    private void selectHandler(RowPanel r0) {
        if (previousSelect != null) {
            previousSelect.setSelected(false);
        }
        r0.setSelected(true);
        previousSelect = r0;

        if (cmdRowSelect != null) { cmdRowSelect.run(); }
    }

    private void deleteFile(RowPanel r0) {
        final GauntletData gd = r0.getGauntletData();

        if (!FOptionPane.showConfirmDialog(
                "Are you sure you want to delete \"" + gd.getName()
                + "\" ?", "Delete Gauntlet")) { return; }

        GauntletIO.getGauntletFile(gd).delete();
        if (cmdRowDelete != null) { cmdRowDelete.run(); }

        this.setSelectedIndex(0);
        this.remove(r0);
        this.repaint();
        this.revalidate();
    }
}
