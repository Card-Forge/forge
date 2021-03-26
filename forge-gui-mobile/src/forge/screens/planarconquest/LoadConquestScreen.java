package forge.screens.planarconquest;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.Align;

import forge.Forge;
import forge.Graphics;
import forge.assets.FSkinColor;
import forge.assets.FSkinColor.Colors;
import forge.assets.FSkinFont;
import forge.assets.FSkinImage;
import forge.gamemodes.planarconquest.ConquestController;
import forge.gamemodes.planarconquest.ConquestData;
import forge.gamemodes.planarconquest.ConquestPreferences;
import forge.gamemodes.planarconquest.ConquestPreferences.CQPref;
import forge.gamemodes.quest.QuestUtil;
import forge.gui.FThreads;
import forge.gui.util.SOptionPane;
import forge.localinstance.properties.ForgeConstants;
import forge.model.FModel;
import forge.screens.LaunchScreen;
import forge.screens.home.LoadGameMenu;
import forge.screens.home.NewGameMenu.NewGameScreen;
import forge.screens.planarconquest.ConquestMenu.LaunchReason;
import forge.screens.settings.SettingsScreen;
import forge.toolbox.FButton;
import forge.toolbox.FEvent;
import forge.toolbox.FEvent.FEventHandler;
import forge.toolbox.FList;
import forge.toolbox.FTextArea;
import forge.util.FileUtil;
import forge.util.Localizer;
import forge.util.ThreadUtil;
import forge.util.Utils;

public class LoadConquestScreen extends LaunchScreen {
    private static final float ITEM_HEIGHT = Utils.AVG_FINGER_HEIGHT;
    private static final float PADDING = Utils.AVG_FINGER_HEIGHT * 0.1f;
    private static final FSkinColor OLD_CONQUESTS_BACK_COLOR = FSkinColor.get(Colors.CLR_INACTIVE).getContrastColor(20);
    private static final FSkinColor SEL_COLOR = FSkinColor.get(Colors.CLR_ACTIVE);

    private final FTextArea lblOldConquests = add(new FTextArea(false, Localizer.getInstance().getMessage("lblLoadingExistingConquests")));
    private final ConquestFileLister lstConquests = add(new ConquestFileLister());
    private final FButton btnNewConquest = add(new FButton(Localizer.getInstance().getMessage("lblNewQuest")));
    private final FButton btnRenameConquest = add(new FButton(Localizer.getInstance().getMessage("lblRename")));
    private final FButton btnDeleteConquest = add(new FButton(Localizer.getInstance().getMessage("lblDelete")));

    public LoadConquestScreen() {
        super(null, LoadGameMenu.getMenu());

        lblOldConquests.setFont(FSkinFont.get(12));
        lblOldConquests.setAlignment(Align.center);

        btnNewConquest.setFont(FSkinFont.get(16));
        btnNewConquest.setCommand(new FEventHandler() {
            @Override
            public void handleEvent(FEvent e) {
                NewGameScreen.PlanarConquest.open();
            }
        });
        btnRenameConquest.setFont(btnNewConquest.getFont());
        btnRenameConquest.setCommand(new FEventHandler() {
            @Override
            public void handleEvent(FEvent e) {
                renameConquest(lstConquests.getSelectedConquest());
            }
        });
        btnDeleteConquest.setFont(btnNewConquest.getFont());
        btnDeleteConquest.setCommand(new FEventHandler() {
            @Override
            public void handleEvent(FEvent e) {
                deleteConquest(lstConquests.getSelectedConquest());
            }
        });
    }

    @Override
    public void onActivate() {
        lblOldConquests.setText(Localizer.getInstance().getMessage("lblLoadingExistingConquests"));
        lstConquests.clear();
        updateEnabledButtons();
        revalidate();

        FThreads.invokeInBackgroundThread(new Runnable() {
            @Override
            public void run() {
                final File dirConquests = new File(ForgeConstants.CONQUEST_SAVE_DIR);
                final ConquestController qc = FModel.getConquest();

                Map<String, ConquestData> arrConquests = new HashMap<>();
                if (dirConquests.listFiles() != null) {
                    for (File f : dirConquests.listFiles()) {
                        if (f.isDirectory()) {
                            ConquestData data = new ConquestData(f);
                            arrConquests.put(data.getName(), data);
                        }
                    }
                }

                // Populate list with available conquest data.
                lstConquests.setConquests(new ArrayList<>(arrConquests.values()));

                // If there are quests available, force select.
                if (arrConquests.size() > 0) {
                    final String questname = FModel.getConquestPreferences().getPref(CQPref.CURRENT_CONQUEST);

                    // Attempt to select previous conquest.
                    ConquestData currentConquest = arrConquests.get(questname);
                    if (currentConquest != null) {
                        lstConquests.setSelectedConquest(currentConquest);
                    }
                    else {
                        lstConquests.setSelectedIndex(0);
                    }

                    // Drop into AllZone.
                    qc.setModel(lstConquests.getSelectedConquest());
                }
                else {
                    qc.setModel(null);
                }
                Gdx.app.postRunnable(new Runnable() {
                    @Override
                    public void run() {
                        lblOldConquests.setText(Localizer.getInstance().getMessage("lblOldConquestDataPutIntoDirAndRestartForge", ForgeConstants.CONQUEST_SAVE_DIR));
                        updateEnabledButtons();
                        revalidate();
                        lstConquests.scrollIntoView(lstConquests.selectedIndex);
                    }
                });
            }
        });
    }

    private void updateEnabledButtons() {
        boolean enabled = lstConquests.getSelectedConquest() != null;
        btnStart.setEnabled(enabled);
        btnRenameConquest.setEnabled(enabled);
        btnDeleteConquest.setEnabled(enabled);
    }

    @Override
    protected void drawBackground(Graphics g) {
        super.drawBackground(g);
        float y = Forge.isLandscapeMode() ? 0 : getHeader().getBottom();
        g.fillRect(OLD_CONQUESTS_BACK_COLOR, 0, y, lstConquests.getWidth(), lstConquests.getTop() - y);
    }

    @Override
    protected void drawOverlay(Graphics g) {
        float y = lstConquests.getTop();
        g.drawLine(1, FList.LINE_COLOR, 0, y, lstConquests.getWidth(), y); //draw top border for list
    }

    @Override
    protected void doLayoutAboveBtnStart(float startY, float width, float height) {
        float buttonWidth = (width - 2 * PADDING) / 3;
        float buttonHeight = btnNewConquest.getAutoSizeBounds().height * 1.2f;

        float y = startY + 2 * PADDING;
        lblOldConquests.setBounds(0, y, width, lblOldConquests.getPreferredHeight(width));
        y += lblOldConquests.getHeight() + PADDING;
        lstConquests.setBounds(0, y, width, height - y - buttonHeight - PADDING);
        y += lstConquests.getHeight() + PADDING;

        float x = 0;
        btnNewConquest.setBounds(x, y, buttonWidth, buttonHeight);
        x += buttonWidth + PADDING;
        btnRenameConquest.setBounds(x, y, buttonWidth, buttonHeight);
        x += buttonWidth + PADDING;
        btnDeleteConquest.setBounds(x, y, buttonWidth, buttonHeight);
    }

    private void changeConquest() {
        ConquestData conquest = lstConquests.getSelectedConquest();
        if (conquest == null) { return; }

        FModel.getConquestPreferences().setPref(CQPref.CURRENT_CONQUEST, conquest.getName());
        FModel.getConquestPreferences().save();

        FModel.getConquest().setModel(conquest);
        ConquestMenu.launchPlanarConquest(LaunchReason.LoadConquest);
    }

    private void renameConquest(final ConquestData conquest) {
        if (conquest == null) { return; }

        ThreadUtil.invokeInGameThread(new Runnable() {
            @Override
            public void run() {
                String questName;
                String oldConquestName = conquest.getName();
                while (true) {
                    questName = SOptionPane.showInputDialog(Localizer.getInstance().getMessage("lblEnterNewConquestName"), Localizer.getInstance().getMessage("lblRenameConquest"), null, oldConquestName);
                    if (questName == null) { return; }

                    questName = QuestUtil.cleanString(questName);
                    if (questName.equals(oldConquestName)) { return; } //quit if chose same name

                    if (questName.isEmpty()) {
                        SOptionPane.showMessageDialog(Localizer.getInstance().getMessage("lblPleaseSpecifyConquestName"));
                        continue;
                    }

                    boolean exists = false;
                    for (ConquestData questData : lstConquests) {
                        if (questData.getName().equalsIgnoreCase(questName)) {
                            exists = true;
                            break;
                        }
                    }
                    if (exists) {
                        SOptionPane.showMessageDialog(Localizer.getInstance().getMessage("lblConquestNameExistsPleasePickAnotherName"));
                        continue;
                    }
                    break;
                }

                //ensure prefs updated if current conquest is renamed
                ConquestPreferences prefs = FModel.getConquestPreferences();
                if (conquest.getName().equals(prefs.getPref(CQPref.CURRENT_CONQUEST))) {
                    prefs.setPref(CQPref.CURRENT_CONQUEST, questName);
                    prefs.save();
                }

                conquest.rename(questName);
            }
        });
    }

    private void deleteConquest(final ConquestData conquest) {
        if (conquest == null) { return; }

        ThreadUtil.invokeInGameThread(new Runnable() {
            @Override
            public void run() {
                if (!SOptionPane.showConfirmDialog(
                        Localizer.getInstance().getMessage("lblAreYouSuerDeleteConquest", conquest.getName()),
                        Localizer.getInstance().getMessage("lblDeleteConquest"), Localizer.getInstance().getMessage("lblDelete"), Localizer.getInstance().getMessage("lblCancel"))) {
                    return;
                }

                FThreads.invokeInEdtLater(new Runnable() {
                    @Override
                    public void run() {
                        FileUtil.deleteDirectory(conquest.getDirectory());

                        lstConquests.removeConquest(conquest);
                        updateEnabledButtons();
                    }
                });
            }
        });
    }

    @Override
    protected void startMatch() {
        changeConquest();
    }

    private class ConquestFileLister extends FList<ConquestData> {
        private int selectedIndex = 0;
        
        private ConquestFileLister() {
            setListItemRenderer(new ListItemRenderer<ConquestData>() {
                @Override
                public boolean tap(Integer index, ConquestData value, float x, float y, int count) {
                    if (count == 2) {
                        changeConquest();
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
                public void drawValue(Graphics g, Integer index, ConquestData value, FSkinFont font, FSkinColor foreColor, FSkinColor backColor, boolean pressed, float x, float y, float w, float h) {
                    float offset = SettingsScreen.getInsets(w) - FList.PADDING; //increase padding for settings items
                    x += offset;
                    y += offset;
                    w -= 2 * offset;
                    h -= 2 * offset;

                    float totalHeight = h;
                    String name = value.getName();
                    h = font.getMultiLineBounds(name).height + SettingsScreen.SETTING_PADDING;

                    String progress = value.getProgress();
                    float winRatioWidth = font.getBounds(progress).width + SettingsScreen.SETTING_PADDING;

                    g.drawText(name, font, foreColor, x, y, w - winRatioWidth, h, false, Align.left, false);
                    g.drawText(progress, font, foreColor, x, y, w, h, false, Align.right, false);

                    h += SettingsScreen.SETTING_PADDING;
                    y += h;
                    h = totalHeight - h + SettingsScreen.getInsets(w);
                    float iconSize = h + Utils.scale(2);
                    float iconOffset = SettingsScreen.SETTING_PADDING - Utils.scale(2);

                    String cards = String.valueOf(value.getUnlockedCardCount());
                    String shards = String.valueOf(value.getAEtherShards());
                    font = FSkinFont.get(12);
                    float cardsWidth = font.getBounds(cards).width + iconSize + SettingsScreen.SETTING_PADDING;
                    float shardsWidth = font.getBounds(shards).width + iconSize + SettingsScreen.SETTING_PADDING;
                    g.drawText(value.getPlaneswalker().getName() + " - " + value.getCurrentPlane().getName().replace("_", " "), font, SettingsScreen.DESC_COLOR, x, y, w - shardsWidth - cardsWidth, h, false, Align.left, false);
                    g.drawImage(FSkinImage.SPELLBOOK, x + w - shardsWidth - cardsWidth + iconOffset, y - SettingsScreen.SETTING_PADDING, iconSize, iconSize);
                    g.drawText(cards, font, SettingsScreen.DESC_COLOR, x + w - shardsWidth - cardsWidth + iconSize + SettingsScreen.SETTING_PADDING, y, w, h, false, Align.left, false);
                    g.drawImage(FSkinImage.AETHER_SHARD, x + w - shardsWidth + iconOffset, y - SettingsScreen.SETTING_PADDING, iconSize, iconSize);
                    g.drawText(shards, font, SettingsScreen.DESC_COLOR, x + w - shardsWidth + iconSize + SettingsScreen.SETTING_PADDING, y, w, h, false, Align.left, false);
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

        public void setConquests(List<ConquestData> qd0) {
            List<ConquestData> sorted = new ArrayList<>();
            sorted.addAll(qd0);
            Collections.sort(sorted, new Comparator<ConquestData>() {
                @Override
                public int compare(final ConquestData x, final ConquestData y) {
                    return x.getName().toLowerCase().compareTo(y.getName().toLowerCase());
                }
            });
            setListData(sorted);
        }

        public void removeConquest(ConquestData qd) {
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

        public ConquestData getSelectedConquest() {
            if (selectedIndex == -1) { return null; }
            return getItemAt(selectedIndex);
        }

        public boolean setSelectedConquest(ConquestData qd0) {
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
