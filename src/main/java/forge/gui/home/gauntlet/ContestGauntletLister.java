package forge.gui.home.gauntlet;



import java.awt.Color;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.border.MatteBorder;

import net.miginfocom.swing.MigLayout;
import forge.Command;
import forge.gauntlet.GauntletData;
import forge.gauntlet.GauntletIO;
import forge.gui.toolbox.FLabel;
import forge.gui.toolbox.FSkin;

/** 
 * Creates file list/table for quick deleting, editing, and basic info.
 *
 */
@SuppressWarnings("serial")
public class ContestGauntletLister extends JPanel {
    private RowPanel previousSelect;
    private RowPanel[] rows;
    private Command cmdRowSelect;
    private final Color clrDefault, clrHover, clrActive, clrBorders;

    /** */
    public ContestGauntletLister() {
        super();
        this.clrDefault = new Color(0, 0, 0, 0);
        this.clrHover = FSkin.getColor(FSkin.Colors.CLR_HOVER);
        this.clrActive = FSkin.getColor(FSkin.Colors.CLR_ACTIVE);
        this.clrBorders = FSkin.getColor(FSkin.Colors.CLR_BORDERS);

        this.setOpaque(false);
        this.setLayout(new MigLayout("insets 0, gap 0, wrap"));
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
        final JPanel rowTitle = new JPanel();
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
        for (GauntletData gd : sorted) {
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

    /** @return {@link forge.deck.Deck} */
    public GauntletData getSelectedGauntlet() {
        if (previousSelect == null) {
            return null;
        }
        else {
            return previousSelect.getGauntletData();
        }
    }

    private class RowPanel extends JPanel {
        private boolean selected = false;
        private GauntletData gauntletData;

        public RowPanel(GauntletData gd0) {
            super();
            setOpaque(false);
            setBackground(new Color(0, 0, 0, 0));
            setLayout(new MigLayout("insets 0, gap 0"));
            setBorder(new MatteBorder(0, 0, 1, 0, clrBorders));
            gauntletData = gd0;

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
        selectHandler(rows[i0]);
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

    /** @param c0 &emsp; {@link forge.Command} command executed on row select. */
    public void setSelectCommand(Command c0) {
        this.cmdRowSelect = c0;
    }

    private void selectHandler(RowPanel r0) {
        if (previousSelect != null) {
            previousSelect.setSelected(false);
        }
        r0.setSelected(true);
        previousSelect = r0;

        if (cmdRowSelect != null) { cmdRowSelect.execute(); }
    }
}
