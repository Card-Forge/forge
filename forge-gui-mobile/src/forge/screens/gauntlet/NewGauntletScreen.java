package forge.screens.gauntlet;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import forge.assets.FSkinFont;
import forge.deck.Deck;
import forge.deck.DeckType;
import forge.deck.FDeckChooser;
import forge.game.GameType;
import forge.game.player.RegisteredPlayer;
import forge.gauntlet.GauntletData;
import forge.gauntlet.GauntletIO;
import forge.gauntlet.GauntletUtil;
import forge.model.FModel;
import forge.player.GamePlayerUtil;
import forge.screens.LaunchScreen;
import forge.screens.home.NewGameMenu;
import forge.toolbox.FLabel;
import forge.toolbox.FTextArea;
import forge.toolbox.GuiChoose;
import forge.toolbox.ListChooser;
import forge.util.Callback;
import forge.util.Utils;

public class NewGauntletScreen extends LaunchScreen {
    private static final float PADDING = Utils.scale(10);

    private final FTextArea lblDesc = add(new FTextArea(false,
            "In Gauntlet mode, you select a deck and play against multiple opponents.\n\n" +
            "Configure how many opponents you wish to face and what decks or types of decks they will play.\n\n" +
            "Then, try to beat all AI opponents without losing a match."));

    public NewGauntletScreen() {
        super(null, NewGameMenu.getMenu());

        lblDesc.setFont(FSkinFont.get(12));
        lblDesc.setTextColor(FLabel.INLINE_LABEL_COLOR);
    }

    @Override
    protected void doLayoutAboveBtnStart(float startY, float width, float height) {
        float x = PADDING;
        float y = startY + PADDING;
        float w = width - 2 * PADDING;
        float h = height - y - PADDING;
        lblDesc.setBounds(x, y, w, h);
    }

    @Override
    protected void startMatch() {
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

    private void createQuickGauntlet() {
        GuiChoose.getInteger("How many opponents are you willing to face?", 3, 50, new Callback<Integer>() {
            @Override
            public void run(final Integer numOpponents) {
                if (numOpponents == null) { return; }

                ListChooser<DeckType> chooser = new ListChooser<DeckType>(
                        "Choose allowed deck types for opponents", 0, 11, Arrays.asList(new DeckType[] {
                        DeckType.CUSTOM_DECK,
                        DeckType.PRECONSTRUCTED_DECK,
                        DeckType.QUEST_OPPONENT_DECK,
                        DeckType.COLOR_DECK,
                        DeckType.STANDARD_COLOR_DECK,
                        DeckType.STANDARD_CARDGEN_DECK,
                        DeckType.MODERN_COLOR_DECK,
                        DeckType.MODERN_CARDGEN_DECK,
                        DeckType.LEGACY_CARDGEN_DECK,
                        DeckType.VINTAGE_CARDGEN_DECK,
                        DeckType.THEME_DECK
                }), null, new Callback<List<DeckType>>() {
                    @Override
                    public void run(final List<DeckType> allowedDeckTypes) {
                        if (allowedDeckTypes == null || allowedDeckTypes.isEmpty()) { return; }

                        FDeckChooser.promptForDeck("Select Your Deck", GameType.Gauntlet, false, new Callback<Deck>() {
                            @Override
                            public void run(Deck userDeck) {
                                if (userDeck == null) { return; }

                                GauntletData gauntlet = GauntletUtil.createQuickGauntlet(userDeck, numOpponents, allowedDeckTypes, null);
                                launchGauntlet(gauntlet);
                            }
                        });
                    }
                });
                chooser.show(null, true);
            }
        });
    }

    private void createCustomGauntlet() {
        GuiChoose.getInteger("How many opponents are you willing to face?", 3, 50, new Callback<Integer>() {
            @Override
            public void run(final Integer numOpponents) {
                if (numOpponents == null) { return; }

                GauntletData gauntlet = new GauntletData();
                gauntlet.setDecks(new ArrayList<Deck>());
                promptForAiDeck(gauntlet, numOpponents);
            }
        });
    }

    private void promptForAiDeck(final GauntletData gauntlet, final int numOpponents) {
        final int opponentNum = gauntlet.getDecks().size() + 1;
        FDeckChooser.promptForDeck("Select Deck for Opponent " + opponentNum + " / " + numOpponents, GameType.Gauntlet, true, new Callback<Deck>() {
            @Override
            public void run(Deck aiDeck) {
                if (aiDeck == null) { return; }

                gauntlet.getDecks().add(aiDeck);
                gauntlet.getEventNames().add(aiDeck.getName());

                if (opponentNum < numOpponents) {
                    promptForAiDeck(gauntlet, numOpponents);
                }
                else {
                    //once all ai decks have been selected, prompt for user deck
                    FDeckChooser.promptForDeck("Select Your Deck", GameType.Gauntlet, false, new Callback<Deck>() {
                        @Override
                        public void run(Deck userDeck) {
                            if (userDeck == null) { return; }

                            gauntlet.setUserDeck(userDeck);
                            GauntletUtil.setDefaultGauntletName(gauntlet, GauntletIO.PREFIX_CUSTOM);
                            launchGauntlet(gauntlet);
                        }
                    });
                }
            }
        });
    }

    private void createGauntletContest() {
        final File[] files = GauntletIO.getGauntletFilesLocked();
        final List<GauntletData> contests = new ArrayList<GauntletData>();
        for (final File f : files) {
            GauntletData gd = GauntletIO.loadGauntlet(f);
            if (gd != null) {
                contests.add(gd);
            }
        }

        GuiChoose.oneOrNone("Select Gauntlet Contest", contests, new Callback<GauntletData>() {
            @Override
            public void run(final GauntletData contest) {
                if (contest == null) { return; }

                FDeckChooser.promptForDeck("Select Your Deck", GameType.Gauntlet, false, new Callback<Deck>() {
                    @Override
                    public void run(final Deck userDeck) {
                        if (userDeck == null) { return; }

                        //create copy of contest to use as gauntlet
                        GauntletData gauntlet = new GauntletData();
                        gauntlet.setDecks(new ArrayList<Deck>(contest.getDecks()));
                        gauntlet.setEventNames(new ArrayList<String>(contest.getEventNames()));
                        gauntlet.setUserDeck(userDeck);
                        GauntletUtil.setDefaultGauntletName(gauntlet, contest.getDisplayName() + "_");
                        launchGauntlet(gauntlet);
                    }
                });
            }
        });
    }

    private void launchGauntlet(GauntletData gauntlet) {
        if (gauntlet == null) { return; }
        FModel.setGauntletData(gauntlet);
        gauntlet.reset();

        RegisteredPlayer humanPlayer = new RegisteredPlayer(gauntlet.getUserDeck()).setPlayer(GamePlayerUtil.getGuiPlayer());
        List<RegisteredPlayer> players = new ArrayList<RegisteredPlayer>();
        players.add(humanPlayer);
        players.add(new RegisteredPlayer(gauntlet.getDecks().get(gauntlet.getCompleted())).setPlayer(GamePlayerUtil.createAiPlayer()));
        gauntlet.startRound(players, humanPlayer);
    }
}
