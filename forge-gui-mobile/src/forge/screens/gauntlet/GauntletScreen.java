package forge.screens.gauntlet;

import java.io.File;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import com.badlogic.gdx.graphics.g2d.BitmapFont.HAlignment;

import forge.FThreads;
import forge.Graphics;
import forge.GuiBase;
import forge.assets.FSkinColor;
import forge.assets.FSkinColor.Colors;
import forge.assets.FSkinFont;
import forge.card.CardRenderer;
import forge.deck.Deck;
import forge.deck.DeckType;
import forge.deck.FDeckChooser;
import forge.game.GameType;
import forge.game.player.RegisteredPlayer;
import forge.gauntlet.GauntletData;
import forge.gauntlet.GauntletIO;
import forge.gauntlet.GauntletUtil;
import forge.model.FModel;
import forge.quest.QuestUtil;
import forge.screens.LaunchScreen;
import forge.screens.settings.SettingsScreen;
import forge.toolbox.FButton;
import forge.toolbox.FEvent;
import forge.toolbox.FList;
import forge.toolbox.FOptionPane;
import forge.toolbox.GuiChoose;
import forge.toolbox.ListChooser;
import forge.toolbox.FEvent.FEventHandler;
import forge.util.Callback;
import forge.util.ThreadUtil;
import forge.util.Utils;
import forge.util.gui.SOptionPane;

public class GauntletScreen extends LaunchScreen {
    private static final float PADDING = Utils.AVG_FINGER_HEIGHT * 0.1f;
    private static final FSkinColor SEL_COLOR = FSkinColor.get(Colors.CLR_ACTIVE);

    private final GauntletFileLister lstGauntlets = add(new GauntletFileLister());
    private final FButton btnNewGauntlet = add(new FButton("New"));
    private final FButton btnRenameGauntlet = add(new FButton("Rename"));
    private final FButton btnDeleteGauntlet = add(new FButton("Delete"));

    public GauntletScreen() {
        super("Gauntlets");

        btnNewGauntlet.setFont(FSkinFont.get(16));
        btnNewGauntlet.setCommand(new FEventHandler() {
            @Override
            public void handleEvent(FEvent e) {
                GuiChoose.oneOrNone("Select a Gauntlet Type", new String[] {
                        "Quick Gauntlet",
                        "Custom Gauntlet",
                        "Gauntlet Contest",
                }, new Callback<String>() {
                    @Override
                    public void run(String result) {
                        if (result == null) { return; }

                        switch (result) {
                        case "Quick Gauntlet":
                            createQuickGauntlet();
                            break;
                        case "Custom Gauntlet":
                            createCustomGauntlet();
                            break;
                        default:
                            createGauntletContest();
                            break;
                        }
                    }
                });
            }
        });
        btnRenameGauntlet.setFont(btnNewGauntlet.getFont());
        btnRenameGauntlet.setCommand(new FEventHandler() {
            @Override
            public void handleEvent(FEvent e) {
                renameGauntlet(lstGauntlets.getSelectedGauntlet());
            }
        });
        btnDeleteGauntlet.setFont(btnNewGauntlet.getFont());
        btnDeleteGauntlet.setCommand(new FEventHandler() {
            @Override
            public void handleEvent(FEvent e) {
                deleteGauntlet(lstGauntlets.getSelectedGauntlet());
            }
        });

        final File[] files = GauntletIO.getGauntletFilesUnlocked();
        final List<GauntletData> data = new ArrayList<GauntletData>();

        for (final File f : files) {
            data.add(GauntletIO.loadGauntlet(f));
        }

        lstGauntlets.setGauntlets(data);

        if (lstGauntlets.isEmpty()) {
            btnRenameGauntlet.setEnabled(false);
            btnDeleteGauntlet.setEnabled(false);
            btnStart.setEnabled(false);
        }
        else {
            lstGauntlets.setSelectedIndex(0);
        }
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

    private void createQuickGauntlet() {
        GuiChoose.getInteger("How many opponents are you willing to face?", 5, 50, new Callback<Integer>() {
            @Override
            public void run(final Integer numOpponents) {
                if (numOpponents == null) { return; }

                ListChooser<DeckType> chooser = new ListChooser<DeckType>(
                        "Choose allowed deck types", 0, 5, Arrays.asList(new DeckType[] {
                        DeckType.CUSTOM_DECK,
                        DeckType.PRECONSTRUCTED_DECK,
                        DeckType.QUEST_OPPONENT_DECK,
                        DeckType.COLOR_DECK,
                        DeckType.THEME_DECK
                }), null, new Callback<List<DeckType>>() {
                    @Override
                    public void run(final List<DeckType> allowedDeckTypes) {
                        if (allowedDeckTypes == null || allowedDeckTypes.isEmpty()) { return; }

                        FDeckChooser.promptForDeck("Select Deck for Gauntlet", GameType.Gauntlet, false, new Callback<Deck>() {
                            @Override
                            public void run(Deck userDeck) {
                                if (userDeck == null) { return; }

                                lstGauntlets.addGauntlet(GauntletUtil.createQuickGauntlet(
                                        userDeck, numOpponents, allowedDeckTypes));
                            }
                        });
                    }
                });
                chooser.show(null, true);
            }
        });
    }

    private void createCustomGauntlet() {
        
    }

    private void createGauntletContest() {
        
    }

    @Override
    protected void startMatch() {
        final GauntletData gauntlet = lstGauntlets.getSelectedGauntlet();
        if (gauntlet == null) {
            FOptionPane.showMessageDialog("You must create and select a gauntlet.");
            return;
        }
        FModel.setGauntletData(gauntlet);
        Deck userDeck = gauntlet.getUserDeck();
        if (userDeck == null) {
            //give user a chance to select a deck if none saved with gauntlet
            FDeckChooser.promptForDeck("Select Deck for Gauntlet", GameType.Gauntlet, false, new Callback<Deck>() {
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
        super.startMatch();
    }

    @Override
    protected boolean buildLaunchParams(LaunchParams launchParams) {
        final GauntletData gauntlet = FModel.getGauntletData();
        launchParams.gameType = GameType.Gauntlet;
        launchParams.players.add(new RegisteredPlayer(gauntlet.getUserDeck()).setPlayer(GuiBase.getInterface().getGuiPlayer()));
        launchParams.players.add(new RegisteredPlayer(gauntlet.getDecks().get(gauntlet.getCompleted())).setPlayer(GuiBase.getInterface().createAiPlayer()));
        return true;
    }

    private void renameGauntlet(final GauntletData gauntlet) {
        if (gauntlet == null) { return; }

        ThreadUtil.invokeInGameThread(new Runnable() {
            @Override
            public void run() {
                String gauntletName;
                String oldGauntletName = gauntlet.getName();
                while (true) {
                    gauntletName = SOptionPane.showInputDialog("Enter new name for gauntlet:", "Rename Gauntlet", null, oldGauntletName);
                    if (gauntletName == null) { return; }

                    gauntletName = QuestUtil.cleanString(gauntletName);
                    if (gauntletName.equals(oldGauntletName)) { return; } //quit if chose same name

                    if (gauntletName.isEmpty()) {
                        SOptionPane.showMessageDialog("Please specify a gauntlet name.");
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
                        SOptionPane.showMessageDialog("A gauntlet already exists with that name. Please pick another gauntlet name.");
                        continue;
                    }
                    break;
                }
                final String newGauntletName = gauntletName;
                FThreads.invokeInEdtLater(new Runnable() {
                    @Override
                    public void run() {
                        gauntlet.rename(newGauntletName);
                        lstGauntlets.refresh();
                        lstGauntlets.setSelectedGauntlet(gauntlet);
                    }
                });
            }
        });
    }

    private void deleteGauntlet(final GauntletData gauntlet) {
        if (gauntlet == null) { return; }

        ThreadUtil.invokeInGameThread(new Runnable() {
            @Override
            public void run() {
                if (!SOptionPane.showConfirmDialog(
                        "Are you sure you want to delete '" + gauntlet.getName() + "'?",
                        "Delete Gauntlet", "Delete", "Cancel")) {
                    return;
                }

                GauntletIO.getGauntletFile(gauntlet).delete();

                lstGauntlets.removeItem(gauntlet);
            }
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
                    return CardRenderer.getCardListItemHeight(false);
                }

                @Override
                public void drawValue(Graphics g, Integer index, GauntletData value, FSkinFont font, FSkinColor foreColor, FSkinColor backColor, boolean pressed, float x, float y, float w, float h) {
                    float offset = w * SettingsScreen.INSETS_FACTOR - FList.PADDING; //increase padding for settings items
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

                    g.drawText(name, font, foreColor, x, y, w - progressWidth, h, false, HAlignment.LEFT, false);
                    g.drawText(progress, font, foreColor, x, y, w, h, false, HAlignment.RIGHT, false);

                    h += SettingsScreen.SETTING_PADDING;
                    y += h;
                    h = totalHeight - h + w * SettingsScreen.INSETS_FACTOR;

                    String timestamp = value.getTimestamp();
                    font = FSkinFont.get(12);
                    float timestampWidth = font.getBounds(timestamp).width + SettingsScreen.SETTING_PADDING;
                    g.drawText(value.getUserDeck() == null ? "(none)" : value.getUserDeck().getName(), font, SettingsScreen.DESC_COLOR, x, y, w - timestampWidth, h, false, HAlignment.LEFT, false);
                    g.drawText(timestamp, font, SettingsScreen.DESC_COLOR, x + w - timestampWidth + SettingsScreen.SETTING_PADDING, y, w, h, false, HAlignment.LEFT, false);
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
        }

        public void addGauntlet(GauntletData gauntlet) {
            if (gauntlets == null) { return; }
            gauntlets.add(gauntlet);
            refresh();
            setSelectedGauntlet(gauntlet);
        }

        public void refresh() {
            List<GauntletData> sorted = new ArrayList<GauntletData>();
            for (GauntletData gauntlet : gauntlets) {
                sorted.add(gauntlet);
            }
            Collections.sort(sorted, new Comparator<GauntletData>() {
                @Override
                public int compare(final GauntletData x, final GauntletData y) {
                    return x.getName().toLowerCase().compareTo(y.getName().toLowerCase());
                }
            });
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
