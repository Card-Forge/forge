package forge.screens.gauntlet;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import forge.Forge;
import forge.deck.Deck;
import forge.deck.DeckType;
import forge.deck.FDeckChooser;
import forge.game.GameType;
import forge.gauntlet.GauntletData;
import forge.gauntlet.GauntletIO;
import forge.gauntlet.GauntletUtil;
import forge.model.FModel;
import forge.screens.LaunchScreen;
import forge.screens.home.NewGameMenu;
import forge.screens.home.LoadGameMenu.LoadGameScreen;
import forge.toolbox.GuiChoose;
import forge.toolbox.ListChooser;
import forge.util.Callback;

public class NewGauntletScreen extends LaunchScreen {
    public NewGauntletScreen() {
        super(null, NewGameMenu.getMenu());

    }

    @Override
    protected void doLayoutAboveBtnStart(float startY, float width, float height) {
        
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
                Forge.back(); //prevent going back to this screen from Load Gauntlet screen
                LoadGameScreen.Gauntlet.open();
            }
        });
    }

    @Override
    protected boolean buildLaunchParams(LaunchParams launchParams) {
        return false; //this override isn't needed
    }

    private void createQuickGauntlet() {
        GuiChoose.getInteger("How many opponents are you willing to face?", 3, 50, new Callback<Integer>() {
            @Override
            public void run(final Integer numOpponents) {
                if (numOpponents == null) { return; }

                ListChooser<DeckType> chooser = new ListChooser<DeckType>(
                        "Choose allowed deck types for opponents", 0, 5, Arrays.asList(new DeckType[] {
                        DeckType.CUSTOM_DECK,
                        DeckType.PRECONSTRUCTED_DECK,
                        DeckType.QUEST_OPPONENT_DECK,
                        DeckType.COLOR_DECK,
                        DeckType.THEME_DECK
                }), null, new Callback<List<DeckType>>() {
                    @Override
                    public void run(final List<DeckType> allowedDeckTypes) {
                        if (allowedDeckTypes == null || allowedDeckTypes.isEmpty()) { return; }

                        FDeckChooser.promptForDeck("Select Your Deck", GameType.Gauntlet, false, new Callback<Deck>() {
                            @Override
                            public void run(Deck userDeck) {
                                if (userDeck == null) { return; }

                                GauntletUtil.createQuickGauntlet(userDeck, numOpponents, allowedDeckTypes);
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
                            FModel.setGauntletData(gauntlet);
                            gauntlet.reset();
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
                        FModel.setGauntletData(gauntlet);
                        gauntlet.reset();
                    }
                });
            }
        });
    }
}
