package forge.screens.home.gauntlet;

import java.awt.Color;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.swing.JPanel;
import javax.swing.SwingConstants;

import net.miginfocom.swing.MigLayout;
import forge.UiCommand;
import forge.gauntlet.GauntletData;
import forge.gauntlet.GauntletIO;
import forge.toolbox.FLabel;
import forge.toolbox.FSkin;
import forge.toolbox.FSkin.SkinnedPanel;

/**
 * Creates file list/table for quick deleting, editing, and basic info.
 *
 */
@SuppressWarnings("serial")
public class ContestGauntletLister extends JPanel {
    private RowPanel previousSelect;
    private RowPanel[] rows;
    private UiCommand cmdRowSelect;
    private final Color clrDefault;
    private final FSkin.SkinColor clrHover, clrActive, clrBorders;

    public ContestGauntletLister() {
        super();
        this.clrDefault = new Color(0, 0, 0, 0);
        this.clrHover = FSkin.getColor(FSkin.Colors.CLR_HOVER);
        this.clrActive = FSkin.getColor(FSkin.Colors.CLR_ACTIVE);
        this.clrBorders = FSkin.getColor(FSkin.Colors.CLR_BORDERS);

        this.setOpaque(false);
        this.setLayout(new MigLayout("insets 0, gap 0, wrap"));
    }

    public void setGauntlets(final List<GauntletData> gd0) {
        this.removeAll();
        final List<RowPanel> tempRows = new ArrayList<RowPanel>();
        final List<GauntletData> sorted = new ArrayList<GauntletData>();
        for (final GauntletData gd : gd0) { sorted.add(gd); }
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
                "w 98% - 350px!, h 20px!, gap 20px 0 5px 0");
        rowTitle.add(new FLabel.Builder().text("Last Activity").fontAlign(SwingConstants.CENTER).build(),
                "w 100px!, h 20px!, gap 0 0 5px 0");
        rowTitle.add(new FLabel.Builder().text("Opponents").fontAlign(SwingConstants.CENTER).build(),
                "w 100px!, h 20px!, gap 0 0 5px 0");
        rowTitle.add(new FLabel.Builder().text("Progress").fontAlign(SwingConstants.CENTER).build(),
                "w 100px!, h 20px!, gap 0 0 5px 0");
        this.add(rowTitle, "w 98%!, h 30px!, gapleft 1%");

        RowPanel row;
        String name;
        String progress;
        for (final GauntletData gd : sorted) {
            name = gd.getName();
            name = name.substring(GauntletIO.PREFIX_LOCKED.length());

            progress = String.valueOf(Math.round(
                    ((double) gd.getCompleted() / (double) gd.getDecks().size()) * 100)) + " %";

            if (gd.getUserDeck() == null) {
                progress = "---";
            }

            row = new RowPanel(gd);
            row.setToolTipText(name);

            row.add(new FLabel.Builder().fontAlign(SwingConstants.LEFT).text(name).build(),
                    "w 98% - 350px!, h 20px!, gap 20px 0 5px 0");
            row.add(new FLabel.Builder().text(gd.getTimestamp()).fontAlign(SwingConstants.CENTER).build(),
                    "w 100px!, h 20px!, gap 0 0 5px 0");
            row.add(new FLabel.Builder().text(String.valueOf(gd.getDecks().size()))
                    .fontAlign(SwingConstants.CENTER).build(),
                    "w 100px!, h 20px!, gap 0 0 5px 0");
            row.add(new FLabel.Builder().text(progress)
                    .fontAlign(SwingConstants.CENTER).build(),
                    "w 100px!, h 20px!, gap 0 0 5px 0");
            this.add(row, "w 98%!, h 30px!, gap 1% 0 0 0");
            tempRows.add(row);
        }

        rows = tempRows.toArray(new RowPanel[0]);
        revalidate();
    }

    public GauntletData getSelectedGauntlet() {
        if (previousSelect == null) {
            return null;
        }
        else {
            return previousSelect.getGauntletData();
        }
    }

    private class RowPanel extends SkinnedPanel {
        private boolean selected = false;
        private final GauntletData gauntletData;

        public RowPanel(final GauntletData gd0) {
            super();
            setOpaque(false);
            setBackground(new Color(0, 0, 0, 0));
            setLayout(new MigLayout("insets 0, gap 0"));
            this.setBorder(new FSkin.MatteSkinBorder(0, 0, 1, 0, clrBorders));
            gauntletData = gd0;

            this.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(final MouseEvent e) {
                    if (!selected) {
                        RowPanel.this.setBackground(clrHover);
                        RowPanel.this.setOpaque(true);
                    }
                }
                @Override
                public void mouseExited(final MouseEvent e) {
                    if (!selected) {
                        RowPanel.this.setBackground(clrDefault);
                        RowPanel.this.setOpaque(false);
                    }
                }
                @Override
                public void mousePressed(final MouseEvent e) {
                    selectHandler(RowPanel.this);
                }
            });
        }

        public void setSelected(final boolean b0) {
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
        selectHandler(rows[i0]);
        return true;
    }

    /** @param c0 &emsp; {@link forge.UiCommand} command executed on row select. */
    public void setSelectCommand(final UiCommand c0) {
        this.cmdRowSelect = c0;
    }

    private void selectHandler(final RowPanel r0) {
        if (previousSelect != null) {
            previousSelect.setSelected(false);
        }
        r0.setSelected(true);
        previousSelect = r0;

        if (cmdRowSelect != null) { cmdRowSelect.run(); }
    }
}
