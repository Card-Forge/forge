package forge.gui.home.quest;

import java.awt.Color;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import net.miginfocom.swing.MigLayout;
import forge.Command;
import forge.gui.toolbox.FLabel;
import forge.gui.toolbox.FMouseAdapter;
import forge.gui.toolbox.FOptionPane;
import forge.gui.toolbox.FSkin;
import forge.properties.NewConstants;
import forge.quest.data.QuestData;

/** 
 * Creates file list/table for quick deleting, editing, and basic info.
 *
 */
@SuppressWarnings("serial")
public class QuestFileLister extends JPanel {
    private FSkin.SkinIcon icoDelete, icoDeleteOver, icoEdit, icoEditOver;
    private RowPanel previousSelect;
    private RowPanel[] rows;
    private Command cmdRowSelect, cmdRowDelete, cmdRowEdit;
    private final Color clrDefault;
    private final FSkin.SkinColor clrHover, clrActive, clrBorders;

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
        FSkin.get(rowTitle).setBackground(FSkin.getColor(FSkin.Colors.CLR_ZEBRA));
        rowTitle.setLayout(new MigLayout("insets 0, gap 0"));
        rowTitle.add(new FLabel.Builder().text("Name").fontAlign(SwingConstants.LEFT).build(), "w 85%-112px!, h 20px!, gaptop 5px, gapleft 48px");
        rowTitle.add(new FLabel.Builder().text("Mode").fontAlign(SwingConstants.LEFT).build(), "w 15%!, h 20px!, gaptop 5px, gapleft 4px");
        rowTitle.add(new FLabel.Builder().text("Record").fontAlign(SwingConstants.LEFT).build(), "w 60px!, h 20px!, gaptop 5px, gapleft 4px");
        this.add(rowTitle, "w 98%!, h 30px!, gapleft 1%");

        RowPanel row;
        String mode;
        for (QuestData qd : sorted) {
            mode = qd.getMode().toString();
            row = new RowPanel(qd);
            row.add(new DeleteButton(row), "w 22px!, h 20px!, gaptop 5px");
            row.add(new EditButton(row), "w 22px!, h 20px!, gaptop 5px");
            row.add(new FLabel.Builder().text(qd.getName()).fontAlign(SwingConstants.LEFT).build(), "w 85%-112px!, h 20px!, gaptop 5px, gapleft 4px");
            row.add(new FLabel.Builder().text(mode).fontAlign(SwingConstants.LEFT).build(), "w 15%!, h 20px!, gaptop 5px, gapleft 4px");
            row.add(new FLabel.Builder().text(qd.getAchievements().getWin() + "/" + qd.getAchievements().getLost())
                    .fontAlign(SwingConstants.LEFT).build(), "w 60px!, h 20px!, gaptop 5px, gapleft 4px");
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
            FSkin.AbstractButtonSkin<DeleteButton> skin = FSkin.get(this);
            setRolloverEnabled(true);
            skin.setPressedIcon(icoDeleteOver);
            skin.setRolloverIcon(icoDeleteOver);
            skin.setIcon(icoDelete);
            setOpaque(false);
            setContentAreaFilled(false);
            setBorder(null);
            setBorderPainted(false);
            setToolTipText("Delete this quest");

            this.addMouseListener(new FMouseAdapter() {
                @Override
                public void onMouseEnter(MouseEvent e) {
                    if (!r0.selected) {
                        FSkin.get(r0).setBackground(clrHover);
                        r0.setOpaque(true);
                    }
                }
                @Override
                public void onMouseExit(MouseEvent e) {
                    if (!r0.selected) {
                        FSkin.get(r0).setBackground(clrDefault);
                        r0.setOpaque(false);
                    }
                }
                @Override
                public void onLeftClick(MouseEvent e) {
                    deleteFile(r0);
                }
            });
        }
    }

    private class EditButton extends JButton {
        public EditButton(final RowPanel r0) {
            super();
            FSkin.AbstractButtonSkin<EditButton> skin = FSkin.get(this);
            setRolloverEnabled(true);
            skin.setPressedIcon(icoEditOver);
            skin.setRolloverIcon(icoEditOver);
            skin.setIcon(icoEdit);
            setOpaque(false);
            setContentAreaFilled(false);
            setBorder(null);
            setBorderPainted(false);
            setToolTipText("Rename this quest");

            this.addMouseListener(new FMouseAdapter() {
                @Override
                public void onMouseEnter(MouseEvent e) {
                    if (!r0.selected) {
                        FSkin.get(r0).setBackground(clrHover);
                        r0.setOpaque(true);
                    }
                }
                @Override
                public void onMouseExit(MouseEvent e) {
                    if (!r0.selected) {
                        FSkin.get(r0).setBackground(clrDefault);
                        r0.setOpaque(false);
                    }
                }
                @Override
                public void onLeftClick(MouseEvent e) {
                    editQuest(r0.getQuestData());
                }
            });
        }
    }

    private class RowPanel extends JPanel {
        private boolean selected = false;
        private boolean hovered = false;
        private QuestData questData;

        public RowPanel(QuestData qd0) {
            super();
            setOpaque(false);
            setBackground(new Color(0, 0, 0, 0));
            setLayout(new MigLayout("insets 0, gap 0"));
            FSkin.get(this).setMatteBorder(0, 0, 1, 0, clrBorders);
            questData = qd0;

            this.addMouseListener(new FMouseAdapter() {
                @Override
                public void onMouseEnter(final MouseEvent e) {
                    RowPanel.this.hovered = true;
                    if (!RowPanel.this.selected) {
                        FSkin.get(((RowPanel) e.getSource())).setBackground(QuestFileLister.this.clrHover);
                        ((RowPanel) e.getSource()).setOpaque(true);
                    }
                }

                @Override
                public void onMouseExit(final MouseEvent e) {
                    RowPanel.this.hovered = false;
                    if (!RowPanel.this.selected) {
                        FSkin.get(((RowPanel) e.getSource())).setBackground(QuestFileLister.this.clrDefault);
                        ((RowPanel) e.getSource()).setOpaque(false);
                    }
                }

                @Override
                public void onLeftMouseDown(final MouseEvent e) {
                    if (e.getClickCount() == 1) {
                        QuestFileLister.this.selectHandler((RowPanel) e.getSource());
                    }
                }
            });
        }

        public void setSelected(final boolean b0) {
            this.selected = b0;
            this.setOpaque(b0);
            if (b0) { FSkin.get(this).setBackground(QuestFileLister.this.clrActive); }
            else if (this.hovered) { FSkin.get(this).setBackground(QuestFileLister.this.clrHover); }
            else { FSkin.get(this).setBackground(QuestFileLister.this.clrDefault); }
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

        if (cmdRowSelect != null) { cmdRowSelect.run(); }
    }

    private void editQuest(QuestData quest) {
        String questName;
        String oldQuestName = quest.getName();
        while (true) {
            questName = FOptionPane.showInputDialog("Rename quest to:", "Quest Rename", null, oldQuestName);
            if (questName == null) { return; }

            questName = SSubmenuQuestUtil.cleanString(questName);
            if (questName.equals(oldQuestName)) { return; } //quit if chose same name

            if (questName.isEmpty()) {
                FOptionPane.showMessageDialog("Please specify a quest name.");
                continue;
            }

            boolean exists = false;
            for (RowPanel r : rows) {
                if (r.getQuestData().getName().equalsIgnoreCase(questName)) {
                    exists = true;
                    break;
                }
            }
            if (exists) {
                FOptionPane.showMessageDialog("A quest already exists with that name. Please pick another quest name.");
                continue;
            }
            break;
        }

        quest.rename(questName);

        if (cmdRowEdit != null) { cmdRowEdit.run(); }
    }

    private void deleteFile(RowPanel r0) {
        final QuestData qd = r0.getQuestData();

        if (!FOptionPane.showConfirmDialog(
                "Are you sure you want to delete '" + qd.getName() + "'?",
                "Delete Quest", "Delete", "Cancel")) {
            return;
        }

        new File(NewConstants.QUEST_SAVE_DIR, r0.getQuestData().getName() + ".dat").delete();

        if (cmdRowDelete != null) { cmdRowDelete.run(); }

        this.remove(r0);
        this.repaint();
        this.revalidate();
    }
}
