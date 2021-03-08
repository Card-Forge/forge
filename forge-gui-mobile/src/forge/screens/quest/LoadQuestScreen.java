package forge.screens.quest;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.Align;
import forge.FThreads;
import forge.Forge;
import forge.Graphics;
import forge.assets.FSkinColor;
import forge.assets.FSkinColor.Colors;
import forge.localinstance.properties.ForgeConstants;
import forge.assets.FSkinFont;
import forge.assets.FSkinImage;
import forge.model.FModel;
import forge.quest.QuestController;
import forge.quest.QuestUtil;
import forge.quest.data.DeckConstructionRules;
import forge.quest.data.QuestData;
import forge.quest.data.QuestPreferences.QPref;
import forge.quest.io.QuestDataIO;
import forge.screens.LaunchScreen;
import forge.screens.home.LoadGameMenu;
import forge.screens.home.NewGameMenu.NewGameScreen;
import forge.screens.quest.QuestMenu.LaunchReason;
import forge.screens.settings.SettingsScreen;
import forge.toolbox.FButton;
import forge.toolbox.FEvent;
import forge.toolbox.FList;
import forge.toolbox.FTextArea;
import forge.toolbox.FEvent.FEventHandler;
import forge.util.ThreadUtil;
import forge.util.Utils;
import forge.util.gui.SOptionPane;
import forge.util.Localizer;

public class LoadQuestScreen extends LaunchScreen {
    private static final float ITEM_HEIGHT = Utils.AVG_FINGER_HEIGHT;
    private static final float PADDING = Utils.AVG_FINGER_HEIGHT * 0.1f;
    private static final FSkinColor OLD_QUESTS_BACK_COLOR = FSkinColor.get(Colors.CLR_INACTIVE).getContrastColor(20);
    private static final FSkinColor SEL_COLOR = FSkinColor.get(Colors.CLR_ACTIVE);

    private final Localizer localizer = Localizer.getInstance();
    private final FTextArea lblOldQuests = add(new FTextArea(false, Localizer.getInstance().getMessage("lblLoadingExistingQuests")));
    private final QuestFileLister lstQuests = add(new QuestFileLister());
    private final FButton btnNewQuest = add(new FButton(Localizer.getInstance().getMessage("lblNewQuest")));
    private final FButton btnRenameQuest = add(new FButton(Localizer.getInstance().getMessage("lblRename")));
    private final FButton btnDeleteQuest = add(new FButton(Localizer.getInstance().getMessage("lblDelete")));

    public LoadQuestScreen() {
        super(null, LoadGameMenu.getMenu());

        lblOldQuests.setFont(FSkinFont.get(12));
        lblOldQuests.setAlignment(Align.center);

        btnNewQuest.setFont(FSkinFont.get(16));
        btnNewQuest.setCommand(new FEventHandler() {
            @Override
            public void handleEvent(FEvent e) {
                NewGameScreen.QuestMode.open();
            }
        });
        btnRenameQuest.setFont(btnNewQuest.getFont());
        btnRenameQuest.setCommand(new FEventHandler() {
            @Override
            public void handleEvent(FEvent e) {
                renameQuest(lstQuests.getSelectedQuest());
            }
        });
        btnDeleteQuest.setFont(btnNewQuest.getFont());
        btnDeleteQuest.setCommand(new FEventHandler() {
            @Override
            public void handleEvent(FEvent e) {
                deleteQuest(lstQuests.getSelectedQuest());
            }
        });
    }

    @Override
    public void onActivate() {
        lblOldQuests.setText(localizer.getMessage("lblLoadingExistingQuests"));
        lstQuests.clear();
        updateEnabledButtons();
        revalidate();

        FThreads.invokeInBackgroundThread(new Runnable() {
            @Override
            public void run() {
                final File dirQuests = new File(ForgeConstants.QUEST_SAVE_DIR);
                final QuestController qc = FModel.getQuest();

                // Iterate over files and load quest data for each.
                FilenameFilter takeDatFiles = new FilenameFilter() {
                    @Override
                    public boolean accept(final File dir, final String name) {
                        return name.endsWith(".dat");
                    }
                };
                File[] arrFiles = dirQuests.listFiles(takeDatFiles);
                Map<String, QuestData> arrQuests = new HashMap<>();
                for (File f : arrFiles) {
                    try {
                        arrQuests.put(f.getName(), QuestDataIO.loadData(f));
                    } catch (IOException e) {
                        System.err.println(String.format("Failed to load quest '%s'", f.getName()));
                        // Failed to load last quest, don't continue with quest loading stuff
                        return;
                    }
                }

                // Populate list with available quest data.
                lstQuests.setQuests(new ArrayList<>(arrQuests.values()));

                // If there are quests available, force select.
                if (arrQuests.size() > 0) {
                    final String questname = FModel.getQuestPreferences().getPref(QPref.CURRENT_QUEST);

                    // Attempt to select previous quest.
                    if (arrQuests.get(questname) != null) {
                        lstQuests.setSelectedQuest(arrQuests.get(questname));
                    }
                    else {
                        lstQuests.setSelectedIndex(0);
                    }

                    // Drop into AllZone.
                    qc.load(lstQuests.getSelectedQuest());
                }
                else {
                    qc.load(null);
                }
                Gdx.app.postRunnable(new Runnable() {
                    @Override
                    public void run() {
                        final String str= ForgeConstants.QUEST_SAVE_DIR.replace('\\', '/');
                        lblOldQuests.setText(localizer.getMessage("lblOldQuestData").replace("%s",str));
                        updateEnabledButtons();
                        revalidate();
                        lstQuests.scrollIntoView(lstQuests.selectedIndex);
                    }
                });
            }
        });
    }

    private void updateEnabledButtons() {
        boolean enabled = lstQuests.getSelectedQuest() != null;
        btnStart.setEnabled(enabled);
        btnRenameQuest.setEnabled(enabled);
        btnDeleteQuest.setEnabled(enabled);
    }

    @Override
    protected void drawBackground(Graphics g) {
        super.drawBackground(g);
        float y = Forge.isLandscapeMode() ? 0 : getHeader().getBottom();
        g.fillRect(OLD_QUESTS_BACK_COLOR, 0, y, lstQuests.getWidth(), lstQuests.getTop() - y);
    }

    @Override
    protected void drawOverlay(Graphics g) {
        float y = lstQuests.getTop();
        g.drawLine(1, FList.LINE_COLOR, 0, y, lstQuests.getWidth(), y); //draw top border for list
    }

    @Override
    protected void doLayoutAboveBtnStart(float startY, float width, float height) {
        float buttonWidth = (width - 2 * PADDING) / 3;
        float buttonHeight = btnNewQuest.getAutoSizeBounds().height * 1.2f;

        float y = startY + 2 * PADDING;
        lblOldQuests.setBounds(0, y, width, lblOldQuests.getPreferredHeight(width));
        y += lblOldQuests.getHeight() + PADDING;
        lstQuests.setBounds(0, y, width, height - y - buttonHeight - PADDING);
        y += lstQuests.getHeight() + PADDING;

        float x = 0;
        btnNewQuest.setBounds(x, y, buttonWidth, buttonHeight);
        x += buttonWidth + PADDING;
        btnRenameQuest.setBounds(x, y, buttonWidth, buttonHeight);
        x += buttonWidth + PADDING;
        btnDeleteQuest.setBounds(x, y, buttonWidth, buttonHeight);
    }

    /** Changes between quest data files. */
    private void changeQuest() {
        QuestData quest = lstQuests.getSelectedQuest();
        if (quest == null) { return; }

        FModel.getQuestPreferences().setPref(QPref.CURRENT_QUEST, quest.getName() + ".dat");
        FModel.getQuestPreferences().save();
        QuestMenu.launchQuestMode(LaunchReason.LoadQuest,quest.deckConstructionRules == DeckConstructionRules.Commander);
    }

    private void renameQuest(final QuestData quest) {
        if (quest == null) { return; }

        ThreadUtil.invokeInGameThread(new Runnable() {
            @Override
            public void run() {
                String questName;
                String oldQuestName = quest.getName();
                while (true) {
                    questName = SOptionPane.showInputDialog(localizer.getMessage("lblEnterNewQuestName"), localizer.getMessage("lblRenameQuest"), null, oldQuestName);
                    if (questName == null) { return; }

                    questName = QuestUtil.cleanString(questName);
                    if (questName.equals(oldQuestName)) { return; } //quit if chose same name

                    if (questName.isEmpty()) {
                        SOptionPane.showMessageDialog(localizer.getMessage("lblQuestNameEmpty"));
                        continue;
                    }

                    boolean exists = false;
                    for (QuestData questData : lstQuests) {
                        if (questData.getName().equalsIgnoreCase(questName)) {
                            exists = true;
                            break;
                        }
                    }
                    if (exists) {
                        SOptionPane.showMessageDialog(localizer.getMessage("lblQuestExists"));
                        continue;
                    }
                    break;
                }

                quest.rename(questName);
            }
        });
    }

    private void deleteQuest(final QuestData quest) {
        if (quest == null) { return; }

        ThreadUtil.invokeInGameThread(new Runnable() {
            @Override
            public void run() {
                if (!SOptionPane.showConfirmDialog(
                        localizer.getMessage("lblConfirmDelete") + " '" + quest.getName() + "'?",
                        localizer.getMessage("lblDeleteQuest"), localizer.getMessage("lblDelete"), localizer.getMessage("lblCancel"))) {
                    return;
                }

                FThreads.invokeInEdtLater(new Runnable() {
                    @Override
                    public void run() {
                        new File(ForgeConstants.QUEST_SAVE_DIR, quest.getName() + ".dat").delete();
                        new File(ForgeConstants.QUEST_SAVE_DIR, quest.getName() + ".dat.bak").delete();
        
                        lstQuests.removeQuest(quest);
                        updateEnabledButtons();
                    }
                });
            }
        });
    }

    @Override
    protected void startMatch() {
        changeQuest();
    }

    private class QuestFileLister extends FList<QuestData> {
        private int selectedIndex = 0;

        private QuestFileLister() {
            setListItemRenderer(new ListItemRenderer<QuestData>() {
                @Override
                public boolean tap(Integer index, QuestData value, float x, float y, int count) {
                    if (count == 2) {
                        changeQuest();
                    }
                    else {
                        selectedIndex = index;
                    }
                    return true;
                }

                @Override
                public float getItemHeight() {
                    return ITEM_HEIGHT;
                }

                @Override
                public void drawValue(Graphics g, Integer index, QuestData value, FSkinFont font, FSkinColor foreColor, FSkinColor backColor, boolean pressed, float x, float y, float w, float h) {
                    float offset = SettingsScreen.getInsets(w) - FList.PADDING; //increase padding for settings items
                    x += offset;
                    y += offset;
                    w -= 2 * offset;
                    h -= 2 * offset;

                    float totalHeight = h;
                    String name = value.getName() + " (" + value.getMode().toString() + ")";
                    h = font.getMultiLineBounds(name).height + SettingsScreen.SETTING_PADDING;

                    String winRatio = value.getAchievements().getWin() + "W / " + value.getAchievements().getLost() + "L";
                    float winRatioWidth = font.getBounds(winRatio).width + SettingsScreen.SETTING_PADDING;

                    g.drawText(name, font, foreColor, x, y, w - winRatioWidth, h, false, Align.left, false);
                    g.drawText(winRatio, font, foreColor, x, y, w, h, false, Align.right, false);

                    h += SettingsScreen.SETTING_PADDING;
                    y += h;
                    h = totalHeight - h + SettingsScreen.getInsets(w);
                    float iconSize = h + Utils.scale(1);
                    float iconOffset = SettingsScreen.SETTING_PADDING - Utils.scale(2);

                    String cards = String.valueOf(value.getAssets().getCardPool().countAll());
                    String credits = String.valueOf(value.getAssets().getCredits());
                    font = FSkinFont.get(12);
                    float cardsWidth = font.getBounds(cards).width + iconSize + SettingsScreen.SETTING_PADDING;
                    float creditsWidth = font.getBounds(credits).width + iconSize + SettingsScreen.SETTING_PADDING;
                    g.drawText(FModel.getQuest().getRank(value.getAchievements().getLevel()), font, SettingsScreen.DESC_COLOR, x, y, w - creditsWidth - cardsWidth, h, false, Align.left, false);
                    g.drawImage(FSkinImage.HAND, x + w - creditsWidth - cardsWidth + iconOffset, y - SettingsScreen.SETTING_PADDING, iconSize, iconSize);
                    g.drawText(cards, font, SettingsScreen.DESC_COLOR, x + w - creditsWidth - cardsWidth + iconSize + SettingsScreen.SETTING_PADDING, y, w, h, false, Align.left, false);
                    g.drawImage(FSkinImage.QUEST_COINSTACK, x + w - creditsWidth + iconOffset, y - SettingsScreen.SETTING_PADDING, iconSize, iconSize);
                    g.drawText(credits, font, SettingsScreen.DESC_COLOR, x + w - creditsWidth + iconSize + SettingsScreen.SETTING_PADDING, y, w, h, false, Align.left, false);
                }
            });
        }

        @Override
        protected FSkinColor getItemFillColor(int index) {
            if (index == selectedIndex) {
                return SEL_COLOR;
            }
            return null;
        }

        public void setQuests(List<QuestData> qd0) {
            List<QuestData> sorted = new ArrayList<>();
            sorted.addAll(qd0);
            Collections.sort(sorted, new Comparator<QuestData>() {
                @Override
                public int compare(final QuestData x, final QuestData y) {
                    return x.getName().toLowerCase().compareTo(y.getName().toLowerCase());
                }
            });
            setListData(sorted);
        }

        public void removeQuest(QuestData qd) {
            removeItem(qd);
            if (selectedIndex == getCount()) {
                selectedIndex--;
            }
            revalidate();
        }

        public boolean setSelectedIndex(int i0) {
            if (i0 >= getCount()) { return false; }
            selectedIndex = i0;
            return true;
        }

        public QuestData getSelectedQuest() {
            if (selectedIndex == -1) { return null; }
            return getItemAt(selectedIndex);
        }

        public boolean setSelectedQuest(QuestData qd0) {
            for (int i = 0; i < getCount(); i++) {
                if (getItemAt(i) == qd0) {
                    selectedIndex = i;
                    return true;
                }
            }
            return false;
        }
    }
}
