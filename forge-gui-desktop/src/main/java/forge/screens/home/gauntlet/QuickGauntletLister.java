package forge.screens.home.gauntlet;

import java.awt.Color;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.border.Border;

import net.miginfocom.swing.MigLayout;
import forge.UiCommand;
import forge.assets.FSkinProp;
import forge.gauntlet.GauntletData;
import forge.gauntlet.GauntletIO;
import forge.quest.QuestUtil;
import forge.toolbox.FLabel;
import forge.toolbox.FMouseAdapter;
import forge.toolbox.FOptionPane;
import forge.toolbox.FSkin;
import forge.toolbox.FSkin.SkinIcon;
import forge.toolbox.FSkin.SkinnedButton;
import forge.toolbox.FSkin.SkinnedPanel;

/**
 * Creates file list/table for quick deleting, editing, and basic info.
 */
@SuppressWarnings("serial")
public class QuickGauntletLister extends JPanel {
    private final SkinIcon icoDelete, icoDeleteOver, icoEdit, icoEditOver;
    private RowPanel previousSelect;
    private RowPanel[] rows;
    private UiCommand cmdRowSelect, cmdRowDelete, cmdRowActivate;
    private final Color clrDefault;
    private final FSkin.SkinColor clrHover, clrActive, clrBorders;
    private List<GauntletData> gauntlets;

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
        icoEdit = FSkin.getIcon(FSkinProp.ICO_EDIT);
        icoEditOver = FSkin.getIcon(FSkinProp.ICO_EDIT_OVER);
    }

    public void setGauntlets(final List<GauntletData> gd0) {
        gauntlets = gd0;
        refresh();
    }

    public void refresh() {
        this.removeAll();
        final List<RowPanel> tempRows = new ArrayList<RowPanel>();
        final List<GauntletData> sorted = new ArrayList<GauntletData>();
        for (final GauntletData gd : gauntlets) { sorted.add(gd); }
        Collections.sort(sorted, new Comparator<GauntletData>() {
            @Override
            public int compare(final GauntletData x, final GauntletData y) {
                return x.getName().toLowerCase().compareTo(y.getName().toLowerCase());
            }
        });

        // Title row
        // Note: careful with the widths of the rows here;
        // scroll panes will have difficulty dynamically resizing if 100% width is set.
        final SkinnedPanel rowTitle = new SkinnedPanel();
        rowTitle.setBackground(FSkin.getColor(FSkin.Colors.CLR_ZEBRA));
        rowTitle.setLayout(new MigLayout("insets 0, gap 0"));
        rowTitle.add(new FLabel.Builder().text("Name").fontAlign(SwingConstants.LEFT).build(),
                "w 49% - 185px!, h 20px!, gap 64px 0 5px 0");
        rowTitle.add(new FLabel.Builder().text("Your Deck").fontAlign(SwingConstants.LEFT).build(),
                "w 49% - 185px!, h 20px!, gap 0 0 5px 0");
        rowTitle.add(new FLabel.Builder().text("Last Activity").fontAlign(SwingConstants.LEFT).build(),
                "w 140px!, h 20px!, gap 0 0 5px 0");
        rowTitle.add(new FLabel.Builder().text("Opponents").fontAlign(SwingConstants.RIGHT).build(),
                "w 90px!, h 20px!, gap 0 0 5px 0");
        rowTitle.add(new FLabel.Builder().text("Progress").fontAlign(SwingConstants.RIGHT).build(),
                "w 90px!, h 20px!, gap 0 0 5px 0");
        this.add(rowTitle, "w 98%!, h 30px!, gapleft 1%");

        RowPanel row;
        String name;
        for (final GauntletData gd : sorted) {
            name = gd.getName();

            row = new RowPanel(gd);
            row.setToolTipText(name);

            row.add(new DeleteButton(row), "w 22px!, h 20px!, gap 5px 0 5px 0");
            row.add(new EditButton(row), "w 22px!, h 20px!, gap 5px 0 5px 0");
            row.add(new FLabel.Builder().fontAlign(SwingConstants.LEFT).text(name).build(),
                    "w 49% - 185px!, h 20px!, gap 10px 0 5px 0");
            row.add(new FLabel.Builder().text(gd.getUserDeck() == null ? "(none)" : gd.getUserDeck().getName()).fontAlign(SwingConstants.LEFT).build(),
                    "w 49% - 185px!, h 20px!, gap 0 0 5px 0");
            row.add(new FLabel.Builder().text(gd.getTimestamp()).fontAlign(SwingConstants.LEFT).build(),
                    "w 140px!, h 20px!, gap 0 0 5px 0");
            row.add(new FLabel.Builder().text(String.valueOf(gd.getDecks().size()))
                    .fontAlign(SwingConstants.RIGHT).build(),
                    "w 90px!, h 20px!, gap 0 0 5px 0");
            row.add(new FLabel.Builder().text(String.valueOf(Math.round(
                    ((double) gd.getCompleted() / (double) gd.getDecks().size()) * 100)) + "%")
                    .fontAlign(SwingConstants.RIGHT).build(),
                    "w 90px!, h 20px!, gap 0 0 5px 0");
            this.add(row, "w 98%!, h 30px!, gap 1% 0 0 0");
            tempRows.add(row);
        }

        rows = tempRows.toArray(new RowPanel[0]);
        revalidate();
    }

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
            setToolTipText("Delete this gauntlet");

            this.addMouseListener(new FMouseAdapter() {
                @Override
                public void onMouseEnter(final MouseEvent e) {
                    if (!r0.selected) {
                        r0.setBackground(clrHover);
                        r0.setOpaque(true);
                    }
                }
                @Override
                public void onMouseExit(final MouseEvent e) {
                    if (!r0.selected) {
                        r0.setBackground(clrDefault);
                        r0.setOpaque(false);
                    }
                }
                @Override
                public void onLeftClick(final MouseEvent e) {
                    deleteFile(r0);
                }
            });
        }
    }

    private class EditButton extends SkinnedButton {
        public EditButton(final RowPanel r0) {
            super();
            setRolloverEnabled(true);
            setPressedIcon(icoEditOver);
            setRolloverIcon(icoEditOver);
            setIcon(icoEdit);
            setOpaque(false);
            setContentAreaFilled(false);
            setBorder((Border)null);
            setBorderPainted(false);
            setToolTipText("Rename this gauntlet");

            this.addMouseListener(new FMouseAdapter() {
                @Override
                public void onMouseEnter(final MouseEvent e) {
                    if (!r0.selected) {
                        r0.setBackground(clrHover);
                        r0.setOpaque(true);
                    }
                }
                @Override
                public void onMouseExit(final MouseEvent e) {
                    if (!r0.selected) {
                        r0.setBackground(clrDefault);
                        r0.setOpaque(false);
                    }
                }
                @Override
                public void onLeftClick(final MouseEvent e) {
                    renameGauntlet(r0.getGauntletData());
                }
            });
        }
    }

    private class RowPanel extends SkinnedPanel {
        private boolean selected = false;
        private boolean hovered = false;
        private final GauntletData gauntletData;

        public RowPanel(final GauntletData gd0) {
            super();
            setOpaque(false);
            setBackground(new Color(0, 0, 0, 0));
            setLayout(new MigLayout("insets 0, gap 0"));
            this.setBorder(new FSkin.MatteSkinBorder(0, 0, 1, 0, clrBorders));
            gauntletData = gd0;

            this.addMouseListener(new FMouseAdapter() {
                @Override
                public void onMouseEnter(final MouseEvent e) {
                    RowPanel.this.hovered = true;
                    if (!RowPanel.this.selected) {
                        RowPanel.this.setBackground(clrHover);
                        RowPanel.this.setOpaque(true);
                    }
                }

                @Override
                public void onMouseExit(final MouseEvent e) {
                    RowPanel.this.hovered = false;
                    if (!RowPanel.this.selected) {
                        RowPanel.this.setBackground(clrDefault);
                        RowPanel.this.setOpaque(false);
                    }
                }

                @Override
                public void onLeftMouseDown(final MouseEvent e) {
                    if (e.getClickCount() == 1) {
                        selectHandler(RowPanel.this);
                    }
                    else if (cmdRowActivate != null) {
                        cmdRowActivate.run();
                    }
                }
            });
        }

        public void setSelected(final boolean b0) {
            this.selected = b0;
            this.setOpaque(b0);
            if (b0) { this.setBackground(clrActive); }
            else if (this.hovered) { this.setBackground(clrHover); }
            else { this.setBackground(clrDefault); }
        }

        public boolean isSelected() {
            return selected;
        }

        public GauntletData getGauntletData() {
            return gauntletData;
        }
    }

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
    public boolean setSelectedIndex(final int i0) {
        if (i0 >= rows.length) { return false; }
        selectHandler(rows[Math.max(0, i0)]);
        return true;
    }

    /** @param c0 &emsp; {@link forge.UiCommand} command executed on row select. */
    public void setCmdSelect(final UiCommand c0) {
        this.cmdRowSelect = c0;
    }

    /** @param c0 &emsp; {@link forge.UiCommand} command executed on row delete. */
    public void setCmdDelete(final UiCommand c0) {
        this.cmdRowDelete = c0;
    }

    /** @param c0 &emsp; {@link forge.UiCommand} command executed on row activate. */
    public void setCmdActivate(final UiCommand c0) {
        this.cmdRowActivate = c0;
    }

    private void selectHandler(final RowPanel r0) {
        if (previousSelect != null) {
            previousSelect.setSelected(false);
        }
        r0.setSelected(true);
        previousSelect = r0;

        if (cmdRowSelect != null) { cmdRowSelect.run(); }
    }

    private void renameGauntlet(final GauntletData gauntlet) {
        String gauntletName;
        final String oldGauntletName = gauntlet.getName();
        while (true) {
            gauntletName = FOptionPane.showInputDialog("Rename gauntlet to:", "Gauntlet Rename", null, oldGauntletName);
            if (gauntletName == null) { return; }

            gauntletName = QuestUtil.cleanString(gauntletName);
            if (gauntletName.equals(oldGauntletName)) { return; } //quit if chose same name

            if (gauntletName.isEmpty()) {
                FOptionPane.showMessageDialog("Please specify a gauntlet name.");
                continue;
            }

            boolean exists = false;
            for (final RowPanel r : rows) {
                if (r.getGauntletData().getName().equalsIgnoreCase(gauntletName)) {
                    exists = true;
                    break;
                }
            }
            if (exists) {
                FOptionPane.showMessageDialog("A gauntlet already exists with that name. Please pick another gauntlet name.");
                continue;
            }
            break;
        }

        gauntlet.rename(gauntletName);
        refresh();
    }

    private void deleteFile(final RowPanel r0) {
        final GauntletData gd = r0.getGauntletData();

        if (!FOptionPane.showConfirmDialog(
                "Are you sure you want to delete \"" + gd.getName()
                + "\"?", "Delete Gauntlet")) { return; }

        GauntletIO.getGauntletFile(gd).delete();
        if (cmdRowDelete != null) { cmdRowDelete.run(); }

        this.setSelectedIndex(0);
        this.remove(r0);
        this.repaint();
        this.revalidate();
    }
}
