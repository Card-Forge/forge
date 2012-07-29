package forge.gui.home.quest;

import java.awt.Color;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.border.MatteBorder;

import net.miginfocom.swing.MigLayout;
import forge.Command;
import forge.gui.GuiUtils;
import forge.gui.toolbox.FLabel;
import forge.gui.toolbox.FSkin;
import forge.properties.ForgeProps;
import forge.properties.NewConstants;
import forge.quest.data.QuestData;

/** 
 * Creates file list/table for quick deleting, editing, and basic info.
 *
 */
@SuppressWarnings("serial")
public class QuestFileLister extends JPanel {
    private ImageIcon icoDelete, icoDeleteOver, icoEdit, icoEditOver;
    private RowPanel previousSelect;
    private RowPanel[] rows;
    private Command cmdRowSelect, cmdRowDelete, cmdRowEdit;
    private final Color clrDefault, clrHover, clrActive, clrBorders;

    /** */
    public QuestFileLister() {
        this(true, true);
    }

    /**
     * Creates deck list for selected decks for quick deleting, editing, and basic info.
     * Set "selectable" and "editable" to show those buttons, or not.
     * 
     * @param deletable {@link java.lang.Boolean}
     * @param editable {@link java.lang.Boolean}
     */
    public QuestFileLister(final boolean deletable, final boolean editable) {
        super();

        this.clrDefault = new Color(0, 0, 0, 0);
        this.clrHover = FSkin.getColor(FSkin.Colors.CLR_HOVER);
        this.clrActive = FSkin.getColor(FSkin.Colors.CLR_ACTIVE);
        this.clrBorders = FSkin.getColor(FSkin.Colors.CLR_BORDERS);

        this.setOpaque(false);
        this.setLayout(new MigLayout("insets 0, gap 0, wrap"));

        icoDelete = FSkin.getIcon(FSkin.InterfaceIcons.ICO_DELETE);
        icoDeleteOver = FSkin.getIcon(FSkin.InterfaceIcons.ICO_DELETE_OVER);
        icoEdit = FSkin.getIcon(FSkin.InterfaceIcons.ICO_EDIT);
        icoEditOver = FSkin.getIcon(FSkin.InterfaceIcons.ICO_EDIT_OVER);
    }

    /** @param qd0 &emsp; {@link forge.quest.data.QuestData}[] */
    public void setQuests(List<QuestData> qd0) {
        this.removeAll();
        List<RowPanel> tempRows = new ArrayList<RowPanel>();
        List<QuestData> sorted = new ArrayList<QuestData>();
        for (QuestData qd : qd0) { sorted.add(qd); }
        Collections.sort(sorted, new Comparator<QuestData>() {
            @Override
            public int compare(final QuestData x, final QuestData y) {
                return x.getName().compareTo(y.getName());
            }
        });

        // Title row
        // Note: careful with the widths of the rows here;
        // scroll panes will have difficulty dynamically resizing if 100% width is set.
        final JPanel rowTitle = new JPanel();
        rowTitle.setBackground(FSkin.getColor(FSkin.Colors.CLR_ZEBRA));
        rowTitle.setLayout(new MigLayout("insets 0, gap 0"));
        rowTitle.add(new FLabel.Builder().text("Delete").fontAlign(SwingConstants.CENTER).build(), "w 15%!, h 20px!, gap 0 0 5px 0");
        rowTitle.add(new FLabel.Builder().text("Rename").fontAlign(SwingConstants.CENTER).build(), "w 15%!, h 20px!, gap 0 0 5px 0");
        rowTitle.add(new FLabel.Builder().text("Name").fontAlign(SwingConstants.CENTER).build(), "w 40%!, h 20px!, gap 0 0 5px 0");
        rowTitle.add(new FLabel.Builder().text("Mode").fontAlign(SwingConstants.CENTER).build(), "w 15%!, h 20px!, gap 0 0 5px 0");
        rowTitle.add(new FLabel.Builder().text("Record").fontAlign(SwingConstants.CENTER).build(), "w 15%!, h 20px!, gap 0 0 5px 0");
        this.add(rowTitle, "w 98%!, h 30px!, gapleft 1%");

        RowPanel row;
        String mode;
        for (QuestData qd : sorted) {
            mode = qd.getMode().toString();
            row = new RowPanel(qd);
            row.add(new DeleteButton(row), "w 15%!, h 20px!, gap 0 0 5px 0");
            row.add(new EditButton(row), "w 15%!, h 20px!, gaptop 5px");
            row.add(new FLabel.Builder().text(qd.getName()).build(), "w 40%!, h 20px!, gap 0 0 5px 0");
            row.add(new FLabel.Builder().text(mode).fontAlign(SwingConstants.CENTER).build(), "w 15%!, h 20px!, gap 0 0 5px 0");
            row.add(new FLabel.Builder().text(qd.getAchievements().getWin() + "/" + qd.getAchievements().getLost())
                    .fontAlign(SwingConstants.CENTER).build(), "w 15%!, h 20px!, gap 0 0 5px 0");
            this.add(row, "w 98%!, h 30px!, gap 1% 0 0 0");
            tempRows.add(row);
        }

        rows = tempRows.toArray(new RowPanel[0]);
        revalidate();
    }

    /** @return {@link forge.deck.Deck} */
    public QuestData getSelectedQuest() {
        return previousSelect.getQuestData();
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
                    deleteFile(r0);
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
                    editFileName(r0.getQuestData().getName());
                }
            });
        }
    }

    private class RowPanel extends JPanel {
        private boolean selected = false;
        private QuestData questData;

        public RowPanel(QuestData qd0) {
            super();
            setOpaque(false);
            setBackground(new Color(0, 0, 0, 0));
            setLayout(new MigLayout("insets 0, gap 0"));
            setBorder(new MatteBorder(0, 0, 1, 0, clrBorders));
            questData = qd0;

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

        public QuestData getQuestData() {
            return questData;
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
     * @param qd0 &emsp; Quest data object to select (if exists in list)
     * @return boolean success
     */
    public boolean setSelectedQuestData(QuestData qd0) {
        for (RowPanel r : rows) {
            if (r.getQuestData().getName().equals(qd0.getName())) {
                selectHandler(r);
                return true;
            }
        }
        return false;
    }

    /** @param c0 &emsp; {@link forge.Command} command executed on row select. */
    public void setSelectCommand(Command c0) {
        this.cmdRowSelect = c0;
    }

    /** @param c0 &emsp; {@link forge.Command} command executed on row edit. */
    public void setEditCommand(Command c0) {
        this.cmdRowEdit = c0;
    }

    /** @param c0 &emsp; {@link forge.Command} command executed on delete. */
    public void setDeleteCommand(Command c0) {
        this.cmdRowDelete = c0;
    }

    private void selectHandler(RowPanel r0) {
        if (previousSelect != null) {
            previousSelect.setSelected(false);
        }
        r0.setSelected(true);
        previousSelect = r0;

        if (cmdRowSelect != null) { cmdRowSelect.execute(); }
    }

    private void editFileName(String s0) {
        final Object o = JOptionPane.showInputDialog(null,
                "Rename Quest to:", "Quest Rename", JOptionPane.OK_CANCEL_OPTION);

        if (o == null) { return; }

        final String questName = GuiUtils.cleanString(o.toString());

        boolean exists = false;

        for (RowPanel r : rows) {
            if (r.getQuestData().getName().equalsIgnoreCase(questName)) {
                exists = true;
                break;
            }
        }

        if (exists || questName.equals("")) {
            JOptionPane.showMessageDialog(null, "Please pick another quest name, a quest already has that name.");
            return;
        }
        else {
            File newpath = new File(ForgeProps.getFile(NewConstants.Quest.DATA_DIR) + File.separator + questName + ".dat");
            File oldpath = new File(ForgeProps.getFile(NewConstants.Quest.DATA_DIR) + File.separator + s0 + ".dat");

            oldpath.renameTo(newpath);
        }

        if (cmdRowEdit != null) { cmdRowEdit.execute(); }
    }

    private void deleteFile(RowPanel r0) {
        final QuestData qd = r0.getQuestData();

        final int n = JOptionPane.showConfirmDialog(null,
                "Are you sure you want to delete \"" + qd.getName()
                + "\" ?", "Delete Deck", JOptionPane.YES_NO_OPTION);

        if (n == JOptionPane.NO_OPTION) {
            return;
        }

        new File(ForgeProps.getFile(NewConstants.Quest.DATA_DIR) + File.separator + r0.getQuestData().getName() + ".dat").delete();

        if (cmdRowDelete != null) { cmdRowDelete.execute(); }

        this.remove(r0);
        this.repaint();
        this.revalidate();
    }
}
