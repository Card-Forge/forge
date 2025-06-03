package forge.screens.gauntlet;

import java.io.File;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import com.badlogic.gdx.utils.Align;

import forge.Forge;
import forge.Graphics;
import forge.assets.FSkinColor;
import forge.assets.FSkinColor.Colors;
import forge.assets.FSkinFont;
import forge.deck.Deck;
import forge.deck.FDeckChooser;
import forge.game.GameType;
import forge.game.player.RegisteredPlayer;
import forge.gamemodes.gauntlet.GauntletData;
import forge.gamemodes.gauntlet.GauntletIO;
import forge.gamemodes.quest.QuestUtil;
import forge.gui.FThreads;
import forge.gui.util.SOptionPane;
import forge.model.FModel;
import forge.player.GamePlayerUtil;
import forge.screens.LaunchScreen;
import forge.screens.LoadingOverlay;
import forge.screens.home.LoadGameMenu;
import forge.screens.home.NewGameMenu.NewGameScreen;
import forge.screens.settings.SettingsScreen;
import forge.toolbox.FButton;
import forge.toolbox.FList;
import forge.toolbox.FOptionPane;
import forge.util.Callback;
import forge.util.ThreadUtil;
import forge.util.Utils;

public class LoadGauntletScreen extends LaunchScreen {
    private static final float ITEM_HEIGHT = Utils.AVG_FINGER_HEIGHT;
    private static final float PADDING = Utils.AVG_FINGER_HEIGHT * 0.1f;
    private static final FSkinColor SEL_COLOR = FSkinColor.get(Colors.CLR_ACTIVE);

    private final GauntletFileLister lstGauntlets = add(new GauntletFileLister());
    private final FButton btnNewGauntlet = add(new FButton(Forge.getLocalizer().getMessage("lblNewQuest")));
    private final FButton btnRenameGauntlet = add(new FButton(Forge.getLocalizer().getMessage("lblRename")));
    private final FButton btnDeleteGauntlet = add(new FButton(Forge.getLocalizer().getMessage("lblDelete")));

    public LoadGauntletScreen() {
        super(null, LoadGameMenu.getMenu());

        btnNewGauntlet.setFont(FSkinFont.get(16));
        btnNewGauntlet.setCommand(event -> NewGameScreen.Gauntlet.open());
        btnRenameGauntlet.setFont(btnNewGauntlet.getFont());
        btnRenameGauntlet.setCommand(event -> renameGauntlet(lstGauntlets.getSelectedGauntlet()));
        btnDeleteGauntlet.setFont(btnNewGauntlet.getFont());
        btnDeleteGauntlet.setCommand(event -> deleteGauntlet(lstGauntlets.getSelectedGauntlet()));
    }

    public void onActivate() {
        final File[] files = GauntletIO.getGauntletFilesUnlocked(null);
        final List<GauntletData> data = new ArrayList<>();

        for (final File f : files) {
            GauntletData gd = GauntletIO.loadGauntlet(f);
            if (gd != null) {
                data.add(gd);
            }
        }

        lstGauntlets.setGauntlets(data);
    }

    private void updateButtons() {
        boolean enabled = !lstGauntlets.isEmpty();
        btnRenameGauntlet.setEnabled(enabled);
        btnDeleteGauntlet.setEnabled(enabled);
        btnStart.setEnabled(enabled);
    }

    @Override
    protected void doLayoutAboveBtnStart(float startY, float width, float height) {
        float buttonWidth = (width - 2 * PADDING) / 3;
        float buttonHeight = btnNewGauntlet.getAutoSizeBounds().height * 1.2f;

        float y = startY;
        lstGauntlets.setBounds(0, y, width, height - y - buttonHeight - PADDING);
        y += lstGauntlets.getHeight() + PADDING;

        float x = 0;
        btnNewGauntlet.setBounds(x, y, buttonWidth, buttonHeight);
        x += buttonWidth + PADDING;
        btnRenameGauntlet.setBounds(x, y, buttonWidth, buttonHeight);
        x += buttonWidth + PADDING;
        btnDeleteGauntlet.setBounds(x, y, buttonWidth, buttonHeight);
    }

    @Override
    protected void startMatch() {
        final GauntletData gauntlet = lstGauntlets.getSelectedGauntlet();
        if (gauntlet == null) {
            FOptionPane.showMessageDialog(Forge.getLocalizer().getMessage("lblYouMustCreateAndSelectGauntlet"));
            return;
        }
        FModel.setGauntletData(gauntlet);
        Deck userDeck = gauntlet.getUserDeck();
        if (userDeck == null) {
            //give user a chance to select a deck if none saved with gauntlet
            FDeckChooser.promptForDeck(Forge.getLocalizer().getMessage("lblSelectGauntletDeck"), gauntlet.isCommanderGauntlet()
                    ? GameType.CommanderGauntlet : GameType.Gauntlet, false, new Callback<Deck>() {
                @Override
                public void run(Deck result) {
                    if (result != null) {
                        gauntlet.setUserDeck(result);
                        GauntletIO.saveGauntlet(gauntlet);
                    }
                }
            });
            return;
        }

        LoadingOverlay.show(Forge.getLocalizer().getMessage("lblLoadingNewGame"), true, () -> {
            final GauntletData gauntlet1 = FModel.getGauntletData();
            List<RegisteredPlayer> players = new ArrayList<>();
            RegisteredPlayer humanPlayer = gauntlet1.isCommanderGauntlet()
                    ? RegisteredPlayer.forCommander(gauntlet1.getUserDeck()).setPlayer(GamePlayerUtil.getGuiPlayer())
                    : new RegisteredPlayer(gauntlet1.getUserDeck()).setPlayer(GamePlayerUtil.getGuiPlayer());
            players.add(humanPlayer);
            players.add(gauntlet1.isCommanderGauntlet()
                    ? RegisteredPlayer.forCommander(gauntlet1.getDecks().get(gauntlet1.getCompleted())).setPlayer(GamePlayerUtil.createAiPlayer())
                    : new RegisteredPlayer(gauntlet1.getDecks().get(gauntlet1.getCompleted())).setPlayer(GamePlayerUtil.createAiPlayer()));
            gauntlet1.startRound(players, humanPlayer);
        });
    }

    private void renameGauntlet(final GauntletData gauntlet) {
        if (gauntlet == null) { return; }

        ThreadUtil.invokeInGameThread(() -> {
            String gauntletName;
            String oldGauntletName = gauntlet.getName();
            while (true) {
                gauntletName = SOptionPane.showInputDialog(Forge.getLocalizer().getMessage("lblEnterNewGauntletGameName"), Forge.getLocalizer().getMessage("lblRenameGauntlet"), null, oldGauntletName);
                if (gauntletName == null) { return; }

                gauntletName = QuestUtil.cleanString(gauntletName);
                if (gauntletName.equals(oldGauntletName)) { return; } //quit if chose same name

                if (gauntletName.isEmpty()) {
                    SOptionPane.showMessageDialog(Forge.getLocalizer().getMessage("lblPleaseSpecifyGauntletName"));
                    continue;
                }

                boolean exists = false;
                for (GauntletData gauntletData : lstGauntlets) {
                    if (gauntletData.getName().equalsIgnoreCase(gauntletName)) {
                        exists = true;
                        break;
                    }
                }
                if (exists) {
                    SOptionPane.showMessageDialog(Forge.getLocalizer().getMessage("lblGauntletNameExistsPleasePickAnotherName"));
                    continue;
                }
                break;
            }
            final String newGauntletName = gauntletName;
            FThreads.invokeInEdtLater(() -> {
                gauntlet.rename(newGauntletName);
                lstGauntlets.refresh();
                lstGauntlets.setSelectedGauntlet(gauntlet);
            });
        });
    }

    private void deleteGauntlet(final GauntletData gauntlet) {
        if (gauntlet == null) { return; }

        ThreadUtil.invokeInGameThread(() -> {
            if (!SOptionPane.showConfirmDialog(
                    Forge.getLocalizer().getMessage("lblAreYouSuerDeleteGauntlet", gauntlet.getName()),
                    Forge.getLocalizer().getMessage("lblDeleteGauntlet"), Forge.getLocalizer().getMessage("lblDelete"), Forge.getLocalizer().getMessage("lblCancel"))) {
                return;
            }

            GauntletIO.getGauntletFile(gauntlet).delete();

            lstGauntlets.removeGauntlet(gauntlet);
        });
    }

    private class GauntletFileLister extends FList<GauntletData> {
        private int selectedIndex = 0;
        private List<GauntletData> gauntlets;

        private GauntletFileLister() {
            setListItemRenderer(new ListItemRenderer<GauntletData>() {
                @Override
                public boolean tap(Integer index, GauntletData value, float x, float y, int count) {
                    if (count == 2) {
                        startMatch();
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
                public void drawValue(Graphics g, Integer index, GauntletData value, FSkinFont font, FSkinColor foreColor, FSkinColor backColor, boolean pressed, float x, float y, float w, float h) {
                    float offset = SettingsScreen.getInsets(w) - FList.PADDING; //increase padding for settings items
                    x += offset;
                    y += offset;
                    w -= 2 * offset;
                    h -= 2 * offset;

                    float totalHeight = h;
                    String name = value.getName();
                    h = font.getMultiLineBounds(name).height + SettingsScreen.SETTING_PADDING;

                    int completed = value.getCompleted();
                    int opponents = value.getDecks().size();
                    NumberFormat percent = NumberFormat.getPercentInstance();
                    String progress = completed + " / " + opponents + " (" + percent.format((double)completed / (double)opponents) + ")";
                    float progressWidth = font.getBounds(progress).width + SettingsScreen.SETTING_PADDING;

                    g.drawText(name, font, foreColor, x, y, w - progressWidth, h, false, Align.left, false);
                    g.drawText(progress, font, foreColor, x, y, w, h, false, Align.right, false);

                    h += SettingsScreen.SETTING_PADDING;
                    y += h;
                    h = totalHeight - h + SettingsScreen.getInsets(w);

                    String timestamp = value.getTimestamp();
                    font = FSkinFont.get(12);
                    float timestampWidth = font.getBounds(timestamp).width + SettingsScreen.SETTING_PADDING;
                    g.drawText(value.getUserDeck() == null ? "(none)" : value.getUserDeck().getName(), font, SettingsScreen.DESC_COLOR, x, y, w - timestampWidth, h, false, Align.left, false);
                    g.drawText(timestamp, font, SettingsScreen.DESC_COLOR, x + w - timestampWidth + SettingsScreen.SETTING_PADDING, y, w, h, false, Align.left, false);
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

        public void setGauntlets(List<GauntletData> gauntlets0) {
            gauntlets = gauntlets0;
            refresh();
            setSelectedIndex(0);
            updateButtons();
        }

        public void removeGauntlet(GauntletData gauntlet) {
            if (gauntlets == null) { return; }
            removeItem(gauntlet);
            gauntlets.remove(gauntlet);
            if (selectedIndex == gauntlets.size()) {
                selectedIndex--;
            }
            revalidate();
            updateButtons();
        }

        public void refresh() {
            List<GauntletData> sorted = new ArrayList<>(gauntlets);
            sorted.sort(Comparator.comparing(x -> x.getName().toLowerCase()));
            setListData(sorted);
        }

        public boolean setSelectedIndex(int i0) {
            if (i0 >= getCount()) { return false; }
            selectedIndex = i0;
            scrollIntoView(i0);
            return true;
        }

        public GauntletData getSelectedGauntlet() {
            if (selectedIndex == -1) { return null; }
            return getItemAt(selectedIndex);
        }

        public boolean setSelectedGauntlet(GauntletData gauntlet) {
            for (int i = 0; i < getCount(); i++) {
                if (getItemAt(i) == gauntlet) {
                    selectedIndex = i;
                    scrollIntoView(i);
                    return true;
                }
            }
            return false;
        }
    }
}
