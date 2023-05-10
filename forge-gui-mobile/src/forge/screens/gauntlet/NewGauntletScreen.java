package forge.screens.gauntlet;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import forge.Forge;
import forge.assets.FSkinFont;
import forge.deck.Deck;
import forge.deck.DeckType;
import forge.deck.FDeckChooser;
import forge.game.GameType;
import forge.game.player.RegisteredPlayer;
import forge.gamemodes.gauntlet.GauntletData;
import forge.gamemodes.gauntlet.GauntletIO;
import forge.gamemodes.gauntlet.GauntletUtil;
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
            Forge.getLocalizer().getMessage("lblGauntletText1") + "\n\n" +
            Forge.getLocalizer().getMessage("lblGauntletText2") + "\n\n" +
            Forge.getLocalizer().getMessage("lblGauntletText3")));

    public NewGauntletScreen() {
        super(null, NewGameMenu.getMenu());

        lblDesc.setFont(FSkinFont.get(12));
        lblDesc.setTextColor(FLabel.getInlineLabelColor());
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
        GuiChoose.oneOrNone(Forge.getLocalizer().getMessage("lblSelectGauntletType"), new String[] {
                Forge.getLocalizer().getMessage("lblQuickGauntlet"),
                Forge.getLocalizer().getMessage("lblCustomGauntlet"),
                Forge.getLocalizer().getMessage("lblGauntletContest"),
                Forge.getLocalizer().getMessage("lblCommanderGauntlet"),
        }, new Callback<String>() {
            @Override
            public void run(String result) {
                if (result == null) { return; }

                if (Forge.getLocalizer().getMessage("lblQuickGauntlet").equals(result)) {
                    createQuickGauntlet();
                } else if(Forge.getLocalizer().getMessage("lblCustomGauntlet").equals(result)) {
                    createCustomGauntlet();
                } else if(Forge.getLocalizer().getMessage("lblCommanderGauntlet").equals(result)) {
                    createCommandGauntlet();
                } else {
                    createGauntletContest();
                }
            }
        });
    }

    private void createCommandGauntlet() {
        GuiChoose.getInteger(Forge.getLocalizer().getMessage("lblHowManyOpponents"), 3, 50, new Callback<Integer>() {
            @Override
            public void run(final Integer numOpponents) {
                if (numOpponents == null) { return; }

                ListChooser<DeckType> chooser = new ListChooser<>(
                        Forge.getLocalizer().getMessage("lblChooseAllowedDeckTypeOpponents"), 0, 11, Arrays.asList(DeckType.COMMANDER_DECK,
                        DeckType.PRECON_COMMANDER_DECK,
                        DeckType.RANDOM_COMMANDER_DECK), null, new Callback<List<DeckType>>() {
                    @Override
                    public void run(final List<DeckType> allowedDeckTypes) {
                        if (allowedDeckTypes == null || allowedDeckTypes.isEmpty()) {
                            return;
                        }

                        FDeckChooser.promptForDeck(Forge.getLocalizer().getMessage("lblSelectYourDeck"), GameType.Commander, false, new Callback<Deck>() {
                            @Override
                            public void run(Deck userDeck) {
                                if (userDeck == null) {
                                    return;
                                }

                                GauntletData gauntlet = GauntletUtil.createCommanderGauntlet(userDeck, numOpponents, allowedDeckTypes, null);
                                launchGauntlet(gauntlet);
                            }
                        });
                    }
                });
                chooser.show(null, false); /*setting selectMax to true will select all available option*/
            }
        });
    }

    private void createQuickGauntlet() {
        GuiChoose.getInteger(Forge.getLocalizer().getMessage("lblHowManyOpponents"), 3, 50, new Callback<Integer>() {
            @Override
            public void run(final Integer numOpponents) {
                if (numOpponents == null) { return; }

                ListChooser<DeckType> chooser = new ListChooser<>(
                        Forge.getLocalizer().getMessage("lblChooseAllowedDeckTypeOpponents"), 0, 11, Arrays.asList(DeckType.CUSTOM_DECK,
                        DeckType.PRECONSTRUCTED_DECK,
                        DeckType.QUEST_OPPONENT_DECK,
                        DeckType.COLOR_DECK,
                        DeckType.STANDARD_COLOR_DECK,
                        DeckType.STANDARD_CARDGEN_DECK,
                        DeckType.MODERN_COLOR_DECK,
                        DeckType.PIONEER_CARDGEN_DECK,
                        DeckType.HISTORIC_CARDGEN_DECK,
                        DeckType.MODERN_CARDGEN_DECK,
                        DeckType.LEGACY_CARDGEN_DECK,
                        DeckType.VINTAGE_CARDGEN_DECK,
                        DeckType.THEME_DECK), null, new Callback<List<DeckType>>() {
                    @Override
                    public void run(final List<DeckType> allowedDeckTypes) {
                        if (allowedDeckTypes == null || allowedDeckTypes.isEmpty()) {
                            return;
                        }

                        FDeckChooser.promptForDeck(Forge.getLocalizer().getMessage("lblSelectYourDeck"), GameType.Gauntlet, false, new Callback<Deck>() {
                            @Override
                            public void run(Deck userDeck) {
                                if (userDeck == null) {
                                    return;
                                }

                                GauntletData gauntlet = GauntletUtil.createQuickGauntlet(userDeck, numOpponents, allowedDeckTypes, null);
                                launchGauntlet(gauntlet);
                            }
                        });
                    }
                });
                chooser.show(null, false); /*setting selectMax to true will select all available option*/
            }
        });
    }

    private void createCustomGauntlet() {
        GuiChoose.getInteger(Forge.getLocalizer().getMessage("lblHowManyOpponents"), 3, 50, new Callback<Integer>() {
            @Override
            public void run(final Integer numOpponents) {
                if (numOpponents == null) { return; }

                GauntletData gauntlet = new GauntletData();
                gauntlet.setDecks(new ArrayList<>());
                promptForAiDeck(gauntlet, numOpponents);
            }
        });
    }

    private void promptForAiDeck(final GauntletData gauntlet, final int numOpponents) {
        final int opponentNum = gauntlet.getDecks().size() + 1;
        FDeckChooser.promptForDeck(Forge.getLocalizer().getMessage("lblSelectDeckForOpponent") + " " + opponentNum + " / " + numOpponents, GameType.Gauntlet, true, new Callback<Deck>() {
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
                    FDeckChooser.promptForDeck(Forge.getLocalizer().getMessage("lblSelectYourDeck"), GameType.Gauntlet, false, new Callback<Deck>() {
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
        final List<GauntletData> contests = new ArrayList<>();
        for (final File f : files) {
            GauntletData gd = GauntletIO.loadGauntlet(f);
            if (gd != null) {
                contests.add(gd);
            }
        }

        GuiChoose.oneOrNone(Forge.getLocalizer().getMessage("lblSelectGauntletContest"), contests, new Callback<GauntletData>() {
            @Override
            public void run(final GauntletData contest) {
                if (contest == null) { return; }

                FDeckChooser.promptForDeck(Forge.getLocalizer().getMessage("lblSelectYourDeck"), GameType.Gauntlet, false, new Callback<Deck>() {
                    @Override
                    public void run(final Deck userDeck) {
                        if (userDeck == null) { return; }

                        //create copy of contest to use as gauntlet
                        GauntletData gauntlet = new GauntletData();
                        gauntlet.setDecks(new ArrayList<>(contest.getDecks()));
                        gauntlet.setEventNames(new ArrayList<>(contest.getEventNames()));
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
        List<RegisteredPlayer> players = new ArrayList<>();
        RegisteredPlayer humanPlayer;
        if (gauntlet.isCommanderGauntlet()) {
            humanPlayer = RegisteredPlayer.forCommander(gauntlet.getUserDeck()).setPlayer(GamePlayerUtil.getGuiPlayer());
            players.add(RegisteredPlayer.forCommander(gauntlet.getDecks().get(gauntlet.getCompleted())).setPlayer(GamePlayerUtil.createAiPlayer()));
        } else {
            humanPlayer = new RegisteredPlayer(gauntlet.getUserDeck()).setPlayer(GamePlayerUtil.getGuiPlayer());
            players.add(new RegisteredPlayer(gauntlet.getDecks().get(gauntlet.getCompleted())).setPlayer(GamePlayerUtil.createAiPlayer()));
        }
        players.add(humanPlayer);
        gauntlet.startRound(players, humanPlayer);
    }
}
