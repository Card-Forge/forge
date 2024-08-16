package forge.screens.home.quest;

import java.awt.Color;
import java.awt.Font;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.border.Border;

import forge.gamemodes.quest.QuestUtil;
import forge.gamemodes.quest.data.QuestData;
import forge.gui.UiCommand;
import forge.localinstance.properties.ForgeConstants;
import forge.localinstance.skin.FSkinProp;
import forge.model.FModel;
import forge.toolbox.FLabel;
import forge.toolbox.FMouseAdapter;
import forge.toolbox.FOptionPane;
import forge.toolbox.FSkin;
import forge.toolbox.FSkin.SkinnedButton;
import forge.toolbox.FSkin.SkinnedPanel;
import forge.util.Localizer;
import net.miginfocom.swing.MigLayout;

/**
 * Creates file list/table for quick deleting, editing, and basic info.
 *
 */
@SuppressWarnings("serial")
public class QuestFileLister extends JPanel {
    private FSkin.SkinIcon icoDelete, icoDeleteOver, icoEdit, icoEditOver;
    private RowPanel previousSelect;
    private RowPanel[] rows;
    private UiCommand cmdRowSelect, cmdRowDelete, cmdRowEdit;
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

        icoDelete = FSkin.getIcon(FSkinProp.ICO_DELETE);
        icoDeleteOver = FSkin.getIcon(FSkinProp.ICO_DELETE_OVER);
        icoEdit = FSkin.getIcon(FSkinProp.ICO_EDIT);
        icoEditOver = FSkin.getIcon(FSkinProp.ICO_EDIT_OVER);
    }

    /** @param qd0 &emsp; {@link forge.gamemodes.quest.data.QuestData}[] */
    public void setQuests(List<QuestData> qd0) {
        this.removeAll();
        List<RowPanel> tempRows = new ArrayList<>();
        List<QuestData> sorted = new ArrayList<>(qd0);
        sorted.sort(Comparator.comparing(x -> x.getName().toLowerCase()));

        // Title row
        // Note: careful with the widths of the rows here;
        // scroll panes will have difficulty dynamically resizing if 100% width is set.
        final SkinnedPanel rowTitle = new SkinnedPanel();
        rowTitle.setBackground(FSkin.getColor(FSkin.Colors.CLR_ZEBRA));
        rowTitle.setLayout(new MigLayout("insets 0, gap 0"));
        rowTitle.add(new FLabel.Builder().text(Localizer.getInstance().getMessage("lblNameAndRank")).fontAlign(SwingConstants.LEFT).build(), "w 60%!, h 20px!, gaptop 5px, gapleft 48px");
        rowTitle.add(new FLabel.Builder().text(Localizer.getInstance().getMessage("lblModeAndDifficulty")).fontAlign(SwingConstants.LEFT).build(), "w 40% - 112px!, h 20px!, gaptop 5px, gapleft 4px");
        rowTitle.add(new FLabel.Builder().text(Localizer.getInstance().getMessage("lblRecordAndAssets")).fontAlign(SwingConstants.LEFT).build(), "w 120px!, h 20px!, gaptop 5px, gapleft 4px");
        this.add(rowTitle, "w 98%!, h 30px!, gapleft 1%");

		Map<Integer, String> difficultyNameMap = new HashMap<>();
		difficultyNameMap.put(0, Localizer.getInstance().getMessage("questDifficultyEasy"));
		difficultyNameMap.put(1, Localizer.getInstance().getMessage("questDifficultyMedium"));
		difficultyNameMap.put(2, Localizer.getInstance().getMessage("questDifficultyHard"));
		difficultyNameMap.put(3, Localizer.getInstance().getMessage("questDifficultyExpert"));

        RowPanel row;
        String mode;

        for (QuestData qd : sorted) {

            mode = qd.getMode().toString();
            row = new RowPanel(qd);

            row.add(new DeleteButton(row), "w 22px!, h 20px!, cell 0 0 1 2");
            row.add(new EditButton(row), "w 22px!, h 20px!, cell 1 0 1 2");

            row.add(new FLabel.Builder().text(qd.getName()).fontAlign(SwingConstants.LEFT)
					.fontStyle(Font.BOLD)
					.build(), "w 60%, h 20px!, shrinkx, gaptop 5px, gapleft 4px, cell 2 0 1 1");
			row.add(new FLabel.Builder().text(FModel.getQuest().getRank(qd.getAchievements().getLevel()))
					.fontAlign(SwingConstants.LEFT)
					.fontSize(12)
					.build(), "w 60%, h 20px!, shrinkx, gapbottom 5px, gapleft 4px, cell 2 1 1 1");


            row.add(new FLabel.Builder().text(mode).fontAlign(SwingConstants.LEFT).build(), "h 20px!, gaptop 5px, gapleft 4px, cell 3 0 1 1");
			row.add(new FLabel.Builder().text(difficultyNameMap.get(qd.getAchievements().getDifficulty()))
					.fontAlign(SwingConstants.LEFT)
					.fontSize(12)
					.build(), "h 20px!, pushx, gapbottom 5px, gapleft 4px, cell 3 1 1 1");

            row.add(new FLabel.Builder().text(Localizer.getInstance().getMessage("lblXWinOfYLost", qd.getAchievements().getWin(), qd.getAchievements().getLost()))
                    .fontAlign(SwingConstants.RIGHT).build(), "h 20px!, gaptop 5px, gapleft 4px, gapright 5px, cell 4 0 1 1, align right");

			FLabel cardsLabel = new FLabel.Builder().text(String.valueOf(qd.getAssets().getCardPool().countAll()))
					.fontAlign(SwingConstants.LEFT)
					.fontSize(12)
					.icon(FSkin.getImage(FSkinProp.IMG_ZONE_HAND))
					.build();

			FLabel goldLabel = new FLabel.Builder().text(String.valueOf(qd.getAssets().getCredits()))
					.fontAlign(SwingConstants.LEFT)
					.fontSize(12)
					.icon(FSkin.getImage(FSkinProp.ICO_QUEST_GOLD))
					.build();

			row.add(cardsLabel, "h 20px!, gapbottom 5px, cell 4 1 1 1");
			row.add(goldLabel, "h 20px!, gapleft 10px, gapright 5px, gapbottom 5px, cell 4 1 1 1");

            this.add(row, "w 98%!, h 50px!, gap 1% 0 0 0");
            tempRows.add(row);

        }

        rows = tempRows.toArray(new RowPanel[0]);
        revalidate();
    }

    /** @return {@link forge.deck.Deck} */
    public QuestData getSelectedQuest() {
        return previousSelect.getQuestData();
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
            setToolTipText(Localizer.getInstance().getMessage("lblDeleteThisQuest"));

            this.addMouseListener(new FMouseAdapter() {
                @Override
                public void onMouseEnter(MouseEvent e) {
                    if (!r0.selected) {
                        r0.setBackground(clrHover);
                        r0.setOpaque(true);
                    }
                }
                @Override
                public void onMouseExit(MouseEvent e) {
                    if (!r0.selected) {
                        r0.setBackground(clrDefault);
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
            setToolTipText(Localizer.getInstance().getMessage("lblRenameThisQuest"));

            this.addMouseListener(new FMouseAdapter() {
                @Override
                public void onMouseEnter(MouseEvent e) {
                    if (!r0.selected) {
                        r0.setBackground(clrHover);
                        r0.setOpaque(true);
                    }
                }
                @Override
                public void onMouseExit(MouseEvent e) {
                    if (!r0.selected) {
                        r0.setBackground(clrDefault);
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

    private class RowPanel extends SkinnedPanel {
        private boolean selected = false;
        private boolean hovered = false;
        private QuestData questData;

        public RowPanel(QuestData qd0) {
            super();
            setOpaque(false);
            setBackground(new Color(0, 0, 0, 0));
            setLayout(new MigLayout("insets 0, gap 0"));
            this.setBorder(new FSkin.MatteSkinBorder(0, 0, 1, 0, clrBorders));
            questData = qd0;

            this.addMouseListener(new FMouseAdapter() {
                @Override
                public void onMouseEnter(final MouseEvent e) {
                    RowPanel.this.hovered = true;
                    if (!RowPanel.this.selected) {
                        RowPanel.this.setBackground(QuestFileLister.this.clrHover);
                        RowPanel.this.setOpaque(true);
                    }
                }

                @Override
                public void onMouseExit(final MouseEvent e) {
                    RowPanel.this.hovered = false;
                    if (!RowPanel.this.selected) {
                        RowPanel.this.setBackground(QuestFileLister.this.clrDefault);
                        RowPanel.this.setOpaque(false);
                    }
                }

                @Override
                public void onLeftMouseDown(final MouseEvent e) {
                    if (e.getClickCount() == 1) {
                        QuestFileLister.this.selectHandler(RowPanel.this);
                    }
                }
            });
        }

        public void setSelected(final boolean b0) {
            this.selected = b0;
            this.setOpaque(b0);
            if (b0) { this.setBackground(QuestFileLister.this.clrActive); }
            else if (this.hovered) { this.setBackground(QuestFileLister.this.clrHover); }
            else { this.setBackground(QuestFileLister.this.clrDefault); }
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

    /** @param c0 &emsp; {@link forge.forge.gui.UiCommand} command executed on row select. */
    public void setSelectCommand(UiCommand c0) {
        this.cmdRowSelect = c0;
    }

    /** @param c0 &emsp; {@link forge.forge.gui.UiCommand} command executed on row edit. */
    public void setEditCommand(UiCommand c0) {
        this.cmdRowEdit = c0;
    }

    /** @param c0 &emsp; {@link forge.forge.gui.UiCommand} command executed on delete. */
    public void setDeleteCommand(UiCommand c0) {
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
            questName = FOptionPane.showInputDialog(Localizer.getInstance().getMessage("lblRenameQuestTo") + ":", Localizer.getInstance().getMessage("lblQuestRename"), null, oldQuestName);
            if (questName == null) { return; }

            questName = QuestUtil.cleanString(questName);
            if (questName.equals(oldQuestName)) { return; } //quit if chose same name

            if (questName.isEmpty()) {
                FOptionPane.showMessageDialog(Localizer.getInstance().getMessage("lblQuestNameEmpty"));
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
                FOptionPane.showMessageDialog(Localizer.getInstance().getMessage("lblQuestExists"));
                continue;
            }
            break;
        }

        quest.rename(questName);

        if (cmdRowEdit != null) { cmdRowEdit.run(); }
    }

    private void deleteFile(RowPanel r0) {
        final QuestData qd = r0.getQuestData();

        if (!FOptionPane.showConfirmDialog(Localizer.getInstance().getMessage("lblConfirmDelete") + " '" + qd.getName() + "'?",
                Localizer.getInstance().getMessage("lblDeleteQuest"), Localizer.getInstance().getMessage("lblDelete"),
                Localizer.getInstance().getMessage("lblCancel"), false)) {
            return;
        }

        new File(ForgeConstants.QUEST_SAVE_DIR, r0.getQuestData().getName() + ".dat").delete();
        new File(ForgeConstants.QUEST_SAVE_DIR, r0.getQuestData().getName() + ".dat.bak").delete();

        if (cmdRowDelete != null) { cmdRowDelete.run(); }

        this.remove(r0);
        this.repaint();
        this.revalidate();
    }
}
