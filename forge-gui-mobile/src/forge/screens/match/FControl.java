package forge.screens.match;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import org.apache.commons.lang3.tuple.Pair;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;

import forge.Forge;
import forge.Graphics;
import forge.GuiBase;
import forge.LobbyPlayer;
import forge.ai.AiProfileUtil;
import forge.ai.LobbyPlayerAi;
import forge.assets.FImage;
import forge.assets.FSkin;
import forge.assets.FTextureRegionImage;
import forge.card.CardCharacteristicName;
import forge.control.FControlGameEventHandler;
import forge.control.FControlGamePlayback;
import forge.events.IUiEventVisitor;
import forge.events.UiEvent;
import forge.events.UiEventAttackerDeclared;
import forge.events.UiEventBlockerAssigned;
import forge.game.Game;
import forge.game.GameRules;
import forge.game.GameType;
import forge.game.Match;
import forge.game.card.Card;
import forge.game.card.CardLists;
import forge.game.card.CounterType;
import forge.game.card.CardPredicates.Presets;
import forge.game.combat.Combat;
import forge.game.combat.CombatUtil;
import forge.game.phase.PhaseHandler;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.player.RegisteredPlayer;
import forge.game.trigger.TriggerType;
import forge.game.zone.ZoneType;
import forge.match.input.InputPlaybackControl;
import forge.match.input.InputQueue;
import forge.model.FModel;
import forge.player.LobbyPlayerHuman;
import forge.player.PlayerControllerHuman;
import forge.properties.ForgePreferences;
import forge.properties.ForgePreferences.FPref;
import forge.quest.QuestController;
import forge.screens.match.views.VAssignDamage;
import forge.screens.match.views.VPrompt;
import forge.screens.match.views.VCardDisplayArea.CardAreaPanel;
import forge.screens.match.views.VPhaseIndicator;
import forge.screens.match.views.VPhaseIndicator.PhaseLabel;
import forge.screens.match.views.VPlayerPanel;
import forge.sound.MusicPlaylist;
import forge.toolbox.FCardPanel;
import forge.toolbox.FDisplayObject;
import forge.toolbox.FOptionPane;
import forge.util.Callback;
import forge.util.GuiDisplayUtil;
import forge.util.MyRandom;
import forge.util.NameGenerator;
import forge.util.WaitCallback;
import forge.view.CardView;
import forge.view.CardView.CardStateView;
import forge.view.CombatView;
import forge.view.GameEntityView;
import forge.view.IGameView;
import forge.view.PlayerView;

public class FControl {
    private FControl() { } //don't allow creating instance

    private static Game game;
    private static IGameView gameView;
    private static MatchScreen view;
    private static InputQueue inputQueue;
    private static final EventBus uiEvents;
    private static boolean gameHasHumanPlayer;
    private static final MatchUiEventVisitor visitor = new MatchUiEventVisitor();
    private static FControlGameEventHandler fcVisitor;
    private static FControlGamePlayback playbackControl;
    private static final Map<LobbyPlayer, FImage> avatarImages = new HashMap<LobbyPlayer, FImage>();

    static {
        uiEvents = new EventBus("ui events");
        uiEvents.register(Forge.getSoundSystem());
        uiEvents.register(visitor);
    }

    public static void startMatch(GameType gameType, List<RegisteredPlayer> players) {
        startMatch(gameType, null, players);
    }
    public static void startMatch(GameType gameType, Set<GameType> appliedVariants, List<RegisteredPlayer> players) {
        boolean useRandomFoil = FModel.getPreferences().getPrefBoolean(FPref.UI_RANDOM_FOIL);
        for (RegisteredPlayer rp : players) {
            rp.setRandomFoil(useRandomFoil);
        }

        GameRules rules = new GameRules(gameType);
        if (appliedVariants != null && !appliedVariants.isEmpty()) {
            rules.setAppliedVariants(appliedVariants);
        }
        rules.setPlayForAnte(FModel.getPreferences().getPrefBoolean(FPref.UI_ANTE));
        rules.setMatchAnteRarity(FModel.getPreferences().getPrefBoolean(FPref.UI_ANTE_MATCH_RARITY));
        rules.setManaBurn(FModel.getPreferences().getPrefBoolean(FPref.UI_MANABURN));
        rules.canCloneUseTargetsImage = FModel.getPreferences().getPrefBoolean(FPref.UI_CLONE_MODE_SOURCE);

        startGame(new Match(rules, players));
    }

    public static void startGameInSameMatch() {
        endCurrentGame();
        startGame(game.getMatch());
    }

    public static void startGameInNewMatch() {
        endCurrentGame();
        final Match match = game.getMatch();
        match.clearGamesPlayed();
        FControl.endCurrentGame();
        startGame(match);
    }

    public static void startGame(final Match match) {
        cardDetailsCache.clear(); //ensure details cache cleared before starting a new game
        CardAreaPanel.resetForNewGame(); //ensure card panels reset between games

        Forge.getSoundSystem().setBackgroundMusic(MusicPlaylist.MATCH);

        game = match.createGame();

        if (game.getRules().getGameType() == GameType.Quest) {
            QuestController qc = FModel.getQuest();
            // Reset new list when the Match round starts, not when each game starts
            if (game.getMatch().getPlayedGames().isEmpty()) {
                qc.getCards().resetNewList();
            }
            game.subscribeToEvents(qc); // this one listens to player's mulligans ATM
        }

        inputQueue = new InputQueue(game);

        game.subscribeToEvents(Forge.getSoundSystem());

        Player humanLobbyPlayer = game.getRegisteredPlayers().get(0);
        // The UI controls should use these game data as models
        initMatch(game.getRegisteredPlayers(), humanLobbyPlayer);

        actuateMatchPreferences();

        // Listen to DuelOutcome event to show ViewWinLose
        game.subscribeToEvents(fcVisitor);

        // Add playback controls to match if needed
        gameHasHumanPlayer = false;
        for (Player p :  game.getPlayers()) {
            if (p.getController().getLobbyPlayer() == getGuiPlayer()) {
                gameHasHumanPlayer = true;
            }
        }
        if (!gameHasHumanPlayer) {
            game.subscribeToEvents(playbackControl);

            //add special object that pauses game if screen touched
            view.add(new FDisplayObject() {
                @Override
                public void draw(Graphics g) {
                    //don't draw anything
                }

                @Override
                public void buildTouchListeners(float screenX, float screenY, ArrayList<FDisplayObject> listeners) {
                    if (screenY < view.getHeight() - VPrompt.HEIGHT) {
                        pause();
                    }
                }
            });
        }

        Forge.openScreen(view);

        // It's important to run match in a different thread to allow GUI inputs to be invoked from inside game. 
        // Game is set on pause while gui player takes decisions
        game.getAction().invoke(new Runnable() {
            @Override
            public void run() {
                match.startGame(game);
            }
        });
    }

    public static Game getGame() {
        return game;
    }

    public static IGameView getGameView() {
        return gameView;
    }

    public static MatchScreen getView() {
        return view;
    }

    public static InputQueue getInputQueue() {
        return inputQueue;
    }

    public static boolean stopAtPhase(final PlayerView turn, final PhaseType phase) {
        PhaseLabel label = getPlayerPanel(turn).getPhaseIndicator().getLabel(phase);
        return label == null || label.getStopAtPhase();
    }

    public static void endCurrentTurn() {
        gameView.passPriorityUntilEndOfTurn();
    }

    public static void initMatch(final List<Player> players, Player localPlayer) {
        final String[] indices = FModel.getPreferences().getPref(FPref.UI_AVATARS).split(",");

        // Instantiate all required field slots (user at 0)
        final List<Player> sortedPlayers = shiftPlayersPlaceLocalFirst(players, localPlayer);

        List<VPlayerPanel> playerPanels = new ArrayList<VPlayerPanel>();

        int i = 0;
        int avatarIndex = 0;
        for (Player p : sortedPlayers) {
            if (i < indices.length) {
                avatarIndex = Integer.parseInt(indices[i]);
                i++;
            }
            p.getLobbyPlayer().setAvatarIndex(avatarIndex);
        }

        final PlayerControllerHuman humanController = (PlayerControllerHuman) localPlayer.getController();
        gameView = (IGameView) humanController;
        fcVisitor = new FControlGameEventHandler(humanController);
        playbackControl = new FControlGamePlayback(humanController);

        for (Player p : sortedPlayers) {
            playerPanels.add(new VPlayerPanel(humanController.getPlayerView(p)));
        }
        view = new MatchScreen(gameView, humanController.getPlayerView(localPlayer), playerPanels);
    }

    private static List<Player> shiftPlayersPlaceLocalFirst(final List<Player> players, Player localPlayer) {
        // get an arranged list so that the first local player is at index 0
        List<Player> sortedPlayers = new ArrayList<Player>(players);
        int ixFirstHuman = -1;
        for (int i = 0; i < players.size(); i++) {
            if (sortedPlayers.get(i) == localPlayer) {
                ixFirstHuman = i;
                break;
            }
        }
        if (ixFirstHuman > 0) {
            sortedPlayers.add(0, sortedPlayers.remove(ixFirstHuman));
        }
        return sortedPlayers;
    }

    public static void resetAllPhaseButtons() {
        for (final VPlayerPanel panel : view.getPlayerPanels().values()) {
            panel.getPhaseIndicator().resetPhaseButtons();
        }
    }

    public static void showMessage(final String s0) {
        view.getPrompt().setMessage(s0);
    }

    public static VPlayerPanel getPlayerPanel(final PlayerView playerView) {
        return view.getPlayerPanels().get(playerView);
    }

    public static void highlightCard(final CardView c) {
        for (VPlayerPanel playerPanel : FControl.getView().getPlayerPanels().values()) {
            for (FCardPanel p : playerPanel.getField().getCardPanels()) {
                if (p.getCard().equals(c)) {
                    p.setHighlighted(true);
                    return;
                }
            }
        }
    }

    public static void clearCardHighlights() {
        for (VPlayerPanel playerPanel : FControl.getView().getPlayerPanels().values()) {
            for (FCardPanel p : playerPanel.getField().getCardPanels()) {
                p.setHighlighted(false);
            }
        }
    }

    public static Player getCurrentPlayer() {
        // try current priority
        Player currentPriority = game.getPhaseHandler().getPriorityPlayer();
        if (null != currentPriority && currentPriority.getLobbyPlayer() == getGuiPlayer()) {
            return currentPriority;
        }

        // otherwise find just any player, belonging to this lobbyplayer
        for (Player p : game.getPlayers()) {
            if (p.getLobbyPlayer() == getGuiPlayer()) {
                return p;
            }
        }

        return null;
    }

    public static void alphaStrike() {
        final PhaseHandler ph = game.getPhaseHandler();

        final Player p = getCurrentPlayer();
        final Game game = p.getGame();
        Combat combat = game.getCombat();
        if (combat == null) { return; }

        if (ph.is(PhaseType.COMBAT_DECLARE_ATTACKERS, p)) {
            List<Player> defenders = p.getOpponents();

            for (Card c : CardLists.filter(p.getCardsIn(ZoneType.Battlefield), Presets.CREATURES)) {
                if (combat.isAttacking(c)) {
                    continue;
                }

                for (Player defender : defenders) {
                    if (CombatUtil.canAttack(c, defender, combat)) {
                        combat.addAttacker(c, defender);
                        break;
                    }
                }
            }
        }
    }

    public static void showCombat(CombatView combat) {
        /*if (combat != null && combat.getAttackers().size() > 0 && combat.getAttackingPlayer().getGame().getStack().isEmpty()) {
            if (selectedDocBeforeCombat == null) {
                IVDoc<? extends ICDoc> combatDoc = EDocID.REPORT_COMBAT.getDoc();
                if (combatDoc.getParentCell() != null) {
                    selectedDocBeforeCombat = combatDoc.getParentCell().getSelected();
                    if (selectedDocBeforeCombat != combatDoc) {
                        SDisplayUtil.showTab(combatDoc);
                    }
                    else {
                        selectedDocBeforeCombat = null; //don't need to cache combat doc this way
                    }
                }
            }
        }
        else if (selectedDocBeforeCombat != null) { //re-select doc that was selected before once combat finished
            SDisplayUtil.showTab(selectedDocBeforeCombat);
            selectedDocBeforeCombat = null;
        }
        CCombat.SINGLETON_INSTANCE.setModel(combat);
        CCombat.SINGLETON_INSTANCE.update();*/
    }

    public static Map<CardView, Integer> getDamageToAssign(final CardView attacker, final List<CardView> blockers, final int damage, final GameEntityView defender, final boolean overrideOrder) {
        if (damage <= 0) {
            return new HashMap<CardView, Integer>();
        }

        // If the first blocker can absorb all of the damage, don't show the Assign Damage dialog
        CardView firstBlocker = blockers.get(0);
        if (!overrideOrder && !attacker.getOriginal().hasDeathtouch() && firstBlocker.getLethalDamage() >= damage) {
            Map<CardView, Integer> res = new HashMap<CardView, Integer>();
            res.put(firstBlocker, damage);
            return res;
        }

        return new WaitCallback<Map<CardView, Integer>>() {
            @Override
            public void run() {
                VAssignDamage v = new VAssignDamage(attacker, blockers, damage, defender, overrideOrder, this);
                v.show();
            }
        }.invokeAndWait();
    }

    private static Set<PlayerView> highlightedPlayers = new HashSet<PlayerView>();
    public static void setHighlighted(PlayerView ge, boolean b) {
        if (b) highlightedPlayers.add(ge);
        else highlightedPlayers.remove(ge);
    }

    public static boolean isHighlighted(Player player) {
        return highlightedPlayers.contains(player);
    }

    private static Set<CardView> highlightedCards = new HashSet<CardView>();
    // used to highlight cards in UI
    public static void setUsedToPay(CardView card, boolean value) {
        boolean hasChanged = value ? highlightedCards.add(card) : highlightedCards.remove(card);
        if (hasChanged) { // since we are in UI thread, may redraw the card right now
            updateSingleCard(card);
        }
    }

    public static boolean isUsedToPay(CardView card) {
        return highlightedCards.contains(card);
    }

    public static void updateZones(List<Pair<PlayerView, ZoneType>> zonesToUpdate) {
        for (Pair<PlayerView, ZoneType> kv : zonesToUpdate) {
            PlayerView owner = kv.getKey();
            ZoneType zt = kv.getValue();
            getPlayerPanel(owner).updateZone(zt);
        }
    }

    // Player's mana pool changes
    public static void updateManaPool(List<PlayerView> manaPoolUpdate) {
        for (PlayerView p : manaPoolUpdate) {
            getPlayerPanel(p).updateManaPool();
        }
    }

    // Player's lives and poison counters
    public static void updateLives(List<PlayerView> livesUpdate) {
        for (PlayerView p : livesUpdate) {
            getPlayerPanel(p).updateLife();
        }
    }

    public static void updateCards(Set<CardView> cardsToUpdate) {
        for (CardView c : cardsToUpdate) {
            updateSingleCard(c);
        }
    }

    private static final Map<Integer, CardStateView> cardDetailsCache = new HashMap<Integer, CardStateView>();

    public static CardStateView getCardDetails(CardView card) {
        final CardStateView details = cardDetailsCache.get(card.getId());
        if (details == null) {
            cardDetailsCache.put(card.getId(), card.getOriginal());
            return card.getOriginal();
        }
        return details;
    }

    public static void refreshCardDetails(Iterable<CardView> cards) {
        Set<PlayerView> playersNeedingFieldUpdate = null;

        for (final CardView c : cards) {
            //for each card in play, if it changed from creature to non-creature or vice versa,
            //or if it changed from land to non-land or vice-versa,
            //ensure field containing that card is updated to reflect that change
            final CardStateView state = c.getOriginal();
            if (c.getZone() == ZoneType.Battlefield) {
                CardStateView oldDetails = cardDetailsCache.get(c);
                if (oldDetails == null || state.isCreature() != oldDetails.isCreature() || state.isLand() != oldDetails.isLand()) {
                    if (playersNeedingFieldUpdate == null) {
                        playersNeedingFieldUpdate = new HashSet<PlayerView>();
                    }
                    playersNeedingFieldUpdate.add(c.getController());
                }
            }
            cardDetailsCache.put(c.getId(), c.getOriginal());
        }

        if (playersNeedingFieldUpdate != null) { //update field for any players necessary
            for (PlayerView p : playersNeedingFieldUpdate) {
                getPlayerPanel(p).getField().update();
            }
        }
    }

    public static void updateSingleCard(final CardView card) {
        final ZoneType zone = card.getZone();
        if (zone != null && zone == ZoneType.Battlefield) {
            getPlayerPanel(card.getController()).getField().updateCard(card);
        }
    }

    /** Concede game, bring up WinLose UI. */
    public static void concede() {
        String userPrompt =
                "This will end the current game and you will not be able to resume.\n\n" +
                        "Concede anyway?";
        FOptionPane.showConfirmDialog(userPrompt, "Concede Game?", "Concede", "Cancel", false, new Callback<Boolean>() {
            @Override
            public void run(Boolean result) {
                if (result) {
                    stopGame();
                }
            }
        });
    }

    public static void stopGame() {
        List<Player> pp = new ArrayList<Player>();
        for (Player p : game.getPlayers()) {
            if (p.getOriginalLobbyPlayer() == getGuiPlayer()) {
                pp.add(p);
            }
        }
        boolean hasHuman = !pp.isEmpty();

        if (pp.isEmpty()) {
            pp.addAll(game.getPlayers()); // no human? then all players surrender!
        }

        for (Player p: pp) {
            p.concede();
        }

        Player priorityPlayer = game.getPhaseHandler().getPriorityPlayer();
        boolean humanHasPriority = priorityPlayer == null || priorityPlayer.getLobbyPlayer() == getGuiPlayer();

        if (hasHuman && humanHasPriority) {
            game.getAction().checkGameOverCondition();
        }
        else {
            game.isGameOver(); // this is synchronized method - it's used to make Game-0 thread see changes made here
            inputQueue.onGameOver(false); //release any waiting input, effectively passing priority
        }

        playbackControl.onGameStopRequested();
    }

    public static void endCurrentGame() {
        if (game == null) { return; }

        Forge.back();
        game = null;
        cardDetailsCache.clear(); //ensure card details cache cleared ending game
    }

    public static void pause() {
        Forge.getSoundSystem().pause();
        //pause playback if needed
        if (inputQueue != null && inputQueue.getInput() instanceof InputPlaybackControl) {
            ((InputPlaybackControl)inputQueue.getInput()).pause();
        }
    }

    public static void resume() {
        Forge.getSoundSystem().resume();
    }

    private final static boolean LOG_UIEVENTS = false;

    // UI-related events should arrive here
    public static void fireEvent(UiEvent uiEvent) {
        if (LOG_UIEVENTS) {
            //System.out.println("UI: " + uiEvent.toString()  + " \t\t " + FThreads.debugGetStackTraceItem(4, true));
        }
        uiEvents.post(uiEvent);
    }

    private static class MatchUiEventVisitor implements IUiEventVisitor<Void> {
        @Override
        public Void visit(UiEventBlockerAssigned event) {
            updateSingleCard(event.blocker);
            return null;
        }

        @Override
        public Void visit(UiEventAttackerDeclared event) {
            updateSingleCard(event.attacker);
            return null;
        }

        @Subscribe
        public void receiveEvent(UiEvent evt) {
            evt.visit(this);
        }
    }

    public static void setupGameState(String filename) {
        int humanLife = -1;
        int aiLife = -1;

        final Map<ZoneType, String> humanCardTexts = new EnumMap<ZoneType, String>(ZoneType.class);
        final Map<ZoneType, String> aiCardTexts = new EnumMap<ZoneType, String>(ZoneType.class);

        String tChangePlayer = "NONE";
        String tChangePhase = "NONE";

        try {
            final FileInputStream fstream = new FileInputStream(filename);
            final DataInputStream in = new DataInputStream(fstream);
            final BufferedReader br = new BufferedReader(new InputStreamReader(in));

            String temp = "";

            while ((temp = br.readLine()) != null) {
                final String[] tempData = temp.split("=");
                if (tempData.length < 2 || temp.charAt(0) == '#') {
                    continue;
                }

                final String categoryName = tempData[0].toLowerCase();
                final String categoryValue = tempData[1];

                switch (categoryName) {
                case "humanlife":
                    humanLife = Integer.parseInt(categoryValue);
                    break;
                case "ailife":
                    aiLife = Integer.parseInt(categoryValue);
                    break;
                case "activeplayer":
                    tChangePlayer = categoryValue.trim().toLowerCase();
                    break;
                case "activephase":
                    tChangePhase = categoryValue;
                    break;
                case "humancardsinplay":
                    humanCardTexts.put(ZoneType.Battlefield, categoryValue);
                    break;
                case "aicardsinplay":
                    aiCardTexts.put(ZoneType.Battlefield, categoryValue);
                    break;
                case "humancardsinhand":
                    humanCardTexts.put(ZoneType.Hand, categoryValue);
                    break;
                case "aicardsinhand":
                    aiCardTexts.put(ZoneType.Hand, categoryValue);
                    break;
                case "humancardsingraveyard":
                    humanCardTexts.put(ZoneType.Graveyard, categoryValue);
                    break;
                case "aicardsingraveyard":
                    aiCardTexts.put(ZoneType.Graveyard, categoryValue);
                    break;
                case "humancardsinlibrary":
                    humanCardTexts.put(ZoneType.Library, categoryValue);
                    break;
                case "aicardsinlibrary":
                    aiCardTexts.put(ZoneType.Library, categoryValue);
                    break;
                case "humancardsinexile":
                    humanCardTexts.put(ZoneType.Exile, categoryValue);
                    break;
                case "aicardsinexile":
                    aiCardTexts.put(ZoneType.Exile, categoryValue);
                    break;
                }
            }

            in.close();
        }
        catch (final FileNotFoundException fnfe) {
            FOptionPane.showErrorDialog("File not found: " + filename);
        }
        catch (final Exception e) {
            FOptionPane.showErrorDialog("Error loading battle setup file!");
            return;
        }

        setupGameState(humanLife, aiLife, humanCardTexts, aiCardTexts, tChangePlayer, tChangePhase);
    }

    public static void setupGameState(final int humanLife, final int aiLife, final Map<ZoneType, String> humanCardTexts,
            final Map<ZoneType, String> aiCardTexts, final String tChangePlayer, final String tChangePhase) {

        game.getAction().invoke(new Runnable() {
            @Override
            public void run() {
                final Player human = game.getPlayers().get(0);
                final Player ai = game.getPlayers().get(1);

                Player newPlayerTurn = tChangePlayer.equals("human") ? newPlayerTurn = human : tChangePlayer.equals("ai") ? newPlayerTurn = ai : null;
                PhaseType newPhase = tChangePhase.trim().equalsIgnoreCase("none") ? null : PhaseType.smartValueOf(tChangePhase);

                game.getPhaseHandler().devModeSet(newPhase, newPlayerTurn);

                game.getTriggerHandler().suppressMode(TriggerType.ChangesZone);

                setupPlayerState(humanLife, humanCardTexts, human);
                setupPlayerState(aiLife, aiCardTexts, ai);

                game.getTriggerHandler().clearSuppression(TriggerType.ChangesZone);

                game.getAction().checkStaticAbilities();
            }
        });
    }

    private static void setupPlayerState(int life, Map<ZoneType, String> cardTexts, final Player p) {
        Map<ZoneType, List<Card>> humanCards = new EnumMap<ZoneType, List<Card>>(ZoneType.class);
        for (Entry<ZoneType, String> kv : cardTexts.entrySet()) {
            humanCards.put(kv.getKey(), processCardsForZone(kv.getValue().split(";"), p));
        }

        if (life > 0) {
            p.setLife(life, null);
        }

        for (Entry<ZoneType, List<Card>> kv : humanCards.entrySet()) {
            if (kv.getKey() == ZoneType.Battlefield) {
                for (final Card c : kv.getValue()) {
                    p.getZone(ZoneType.Hand).add(c);
                    p.getGame().getAction().moveToPlay(c);
                    c.setSickness(false);
                }
            }
            else {
                p.getZone(kv.getKey()).setCards(kv.getValue());
            }
        }
    }

    private static List<Card> processCardsForZone(final String[] data, final Player player) {
        final List<Card> cl = new ArrayList<Card>();
        for (final String element : data) {
            final String[] cardinfo = element.trim().split("\\|");

            final Card c = Card.fromPaperCard(FModel.getMagicDb().getCommonCards().getCard(cardinfo[0]), player);

            boolean hasSetCurSet = false;
            for (final String info : cardinfo) {
                if (info.startsWith("Set:")) {
                    c.setCurSetCode(info.substring(info.indexOf(':') + 1));
                    hasSetCurSet = true;
                }
                else if (info.equalsIgnoreCase("Tapped:True")) {
                    c.tap();
                }
                else if (info.startsWith("Counters:")) {
                    final String[] counterStrings = info.substring(info.indexOf(':') + 1).split(",");
                    for (final String counter : counterStrings) {
                        c.addCounter(CounterType.valueOf(counter), 1, true);
                    }
                }
                else if (info.equalsIgnoreCase("SummonSick:True")) {
                    c.setSickness(true);
                }
                else if (info.equalsIgnoreCase("FaceDown:True")) {
                    c.setState(CardCharacteristicName.FaceDown);
                }
            }

            if (!hasSetCurSet) {
                c.setCurSetCode(c.getMostRecentSet());
            }

            cl.add(c);
        }
        return cl;
    }

    public static void writeMatchPreferences() {
        ForgePreferences prefs = FModel.getPreferences();

        VPhaseIndicator fvAi = FControl.getView().getTopPlayerPanel().getPhaseIndicator();
        prefs.setPref(FPref.PHASE_AI_UPKEEP, String.valueOf(fvAi.getLabel(PhaseType.UPKEEP).getStopAtPhase()));
        prefs.setPref(FPref.PHASE_AI_DRAW, String.valueOf(fvAi.getLabel(PhaseType.DRAW).getStopAtPhase()));
        prefs.setPref(FPref.PHASE_AI_MAIN1, String.valueOf(fvAi.getLabel(PhaseType.MAIN1).getStopAtPhase()));
        prefs.setPref(FPref.PHASE_AI_BEGINCOMBAT, String.valueOf(fvAi.getLabel(PhaseType.COMBAT_BEGIN).getStopAtPhase()));
        prefs.setPref(FPref.PHASE_AI_DECLAREATTACKERS, String.valueOf(fvAi.getLabel(PhaseType.COMBAT_DECLARE_ATTACKERS).getStopAtPhase()));
        prefs.setPref(FPref.PHASE_AI_DECLAREBLOCKERS, String.valueOf(fvAi.getLabel(PhaseType.COMBAT_DECLARE_BLOCKERS).getStopAtPhase()));
        prefs.setPref(FPref.PHASE_AI_FIRSTSTRIKE, String.valueOf(fvAi.getLabel(PhaseType.COMBAT_FIRST_STRIKE_DAMAGE).getStopAtPhase()));
        prefs.setPref(FPref.PHASE_AI_COMBATDAMAGE, String.valueOf(fvAi.getLabel(PhaseType.COMBAT_DAMAGE).getStopAtPhase()));
        prefs.setPref(FPref.PHASE_AI_ENDCOMBAT, String.valueOf(fvAi.getLabel(PhaseType.COMBAT_END).getStopAtPhase()));
        prefs.setPref(FPref.PHASE_AI_MAIN2, String.valueOf(fvAi.getLabel(PhaseType.MAIN2).getStopAtPhase()));
        prefs.setPref(FPref.PHASE_AI_EOT, String.valueOf(fvAi.getLabel(PhaseType.END_OF_TURN).getStopAtPhase()));
        prefs.setPref(FPref.PHASE_AI_CLEANUP, String.valueOf(fvAi.getLabel(PhaseType.CLEANUP).getStopAtPhase()));

        VPhaseIndicator fvHuman = FControl.getView().getBottomPlayerPanel().getPhaseIndicator();
        prefs.setPref(FPref.PHASE_HUMAN_UPKEEP, String.valueOf(fvHuman.getLabel(PhaseType.UPKEEP).getStopAtPhase()));
        prefs.setPref(FPref.PHASE_HUMAN_DRAW, String.valueOf(fvHuman.getLabel(PhaseType.DRAW).getStopAtPhase()));
        prefs.setPref(FPref.PHASE_HUMAN_MAIN1, String.valueOf(fvHuman.getLabel(PhaseType.MAIN1).getStopAtPhase()));
        prefs.setPref(FPref.PHASE_HUMAN_BEGINCOMBAT, String.valueOf(fvHuman.getLabel(PhaseType.COMBAT_BEGIN).getStopAtPhase()));
        prefs.setPref(FPref.PHASE_HUMAN_DECLAREATTACKERS, String.valueOf(fvHuman.getLabel(PhaseType.COMBAT_DECLARE_ATTACKERS).getStopAtPhase()));
        prefs.setPref(FPref.PHASE_HUMAN_DECLAREBLOCKERS, String.valueOf(fvHuman.getLabel(PhaseType.COMBAT_DECLARE_BLOCKERS).getStopAtPhase()));
        prefs.setPref(FPref.PHASE_HUMAN_FIRSTSTRIKE, String.valueOf(fvHuman.getLabel(PhaseType.COMBAT_FIRST_STRIKE_DAMAGE).getStopAtPhase()));
        prefs.setPref(FPref.PHASE_HUMAN_COMBATDAMAGE, String.valueOf(fvHuman.getLabel(PhaseType.COMBAT_DAMAGE).getStopAtPhase()));
        prefs.setPref(FPref.PHASE_HUMAN_ENDCOMBAT, String.valueOf(fvHuman.getLabel(PhaseType.COMBAT_END).getStopAtPhase()));
        prefs.setPref(FPref.PHASE_HUMAN_MAIN2, String.valueOf(fvHuman.getLabel(PhaseType.MAIN2).getStopAtPhase()));
        prefs.setPref(FPref.PHASE_HUMAN_EOT, fvHuman.getLabel(PhaseType.END_OF_TURN).getStopAtPhase());
        prefs.setPref(FPref.PHASE_HUMAN_CLEANUP, fvHuman.getLabel(PhaseType.CLEANUP).getStopAtPhase());

        prefs.save();
    }

    private static void actuateMatchPreferences() {
        ForgePreferences prefs = FModel.getPreferences();

        VPhaseIndicator fvAi = FControl.getView().getTopPlayerPanel().getPhaseIndicator();
        fvAi.getLabel(PhaseType.UPKEEP).setStopAtPhase(prefs.getPrefBoolean(FPref.PHASE_AI_UPKEEP));
        fvAi.getLabel(PhaseType.DRAW).setStopAtPhase(prefs.getPrefBoolean(FPref.PHASE_AI_DRAW));
        fvAi.getLabel(PhaseType.MAIN1).setStopAtPhase(prefs.getPrefBoolean(FPref.PHASE_AI_MAIN1));
        fvAi.getLabel(PhaseType.COMBAT_BEGIN).setStopAtPhase(prefs.getPrefBoolean(FPref.PHASE_AI_BEGINCOMBAT));
        fvAi.getLabel(PhaseType.COMBAT_DECLARE_ATTACKERS).setStopAtPhase(prefs.getPrefBoolean(FPref.PHASE_AI_DECLAREATTACKERS));
        fvAi.getLabel(PhaseType.COMBAT_DECLARE_BLOCKERS).setStopAtPhase(prefs.getPrefBoolean(FPref.PHASE_AI_DECLAREBLOCKERS));
        fvAi.getLabel(PhaseType.COMBAT_FIRST_STRIKE_DAMAGE).setStopAtPhase(prefs.getPrefBoolean(FPref.PHASE_AI_FIRSTSTRIKE));
        fvAi.getLabel(PhaseType.COMBAT_DAMAGE).setStopAtPhase(prefs.getPrefBoolean(FPref.PHASE_AI_COMBATDAMAGE));
        fvAi.getLabel(PhaseType.COMBAT_END).setStopAtPhase(prefs.getPrefBoolean(FPref.PHASE_AI_ENDCOMBAT));
        fvAi.getLabel(PhaseType.MAIN2).setStopAtPhase(prefs.getPrefBoolean(FPref.PHASE_AI_MAIN2));
        fvAi.getLabel(PhaseType.END_OF_TURN).setStopAtPhase(prefs.getPrefBoolean(FPref.PHASE_AI_EOT));
        fvAi.getLabel(PhaseType.CLEANUP).setStopAtPhase(prefs.getPrefBoolean(FPref.PHASE_AI_CLEANUP));

        VPhaseIndicator fvHuman = FControl.getView().getBottomPlayerPanel().getPhaseIndicator();
        fvHuman.getLabel(PhaseType.UPKEEP).setStopAtPhase(prefs.getPrefBoolean(FPref.PHASE_HUMAN_UPKEEP));
        fvHuman.getLabel(PhaseType.DRAW).setStopAtPhase(prefs.getPrefBoolean(FPref.PHASE_HUMAN_DRAW));
        fvHuman.getLabel(PhaseType.MAIN1).setStopAtPhase(prefs.getPrefBoolean(FPref.PHASE_HUMAN_MAIN1));
        fvHuman.getLabel(PhaseType.COMBAT_BEGIN).setStopAtPhase(prefs.getPrefBoolean(FPref.PHASE_HUMAN_BEGINCOMBAT));
        fvHuman.getLabel(PhaseType.COMBAT_DECLARE_ATTACKERS).setStopAtPhase(prefs.getPrefBoolean(FPref.PHASE_HUMAN_DECLAREATTACKERS));
        fvHuman.getLabel(PhaseType.COMBAT_DECLARE_BLOCKERS).setStopAtPhase(prefs.getPrefBoolean(FPref.PHASE_HUMAN_DECLAREBLOCKERS));
        fvHuman.getLabel(PhaseType.COMBAT_FIRST_STRIKE_DAMAGE).setStopAtPhase(prefs.getPrefBoolean(FPref.PHASE_HUMAN_FIRSTSTRIKE));
        fvHuman.getLabel(PhaseType.COMBAT_DAMAGE).setStopAtPhase(prefs.getPrefBoolean(FPref.PHASE_HUMAN_COMBATDAMAGE));
        fvHuman.getLabel(PhaseType.COMBAT_END).setStopAtPhase(prefs.getPrefBoolean(FPref.PHASE_HUMAN_ENDCOMBAT));
        fvHuman.getLabel(PhaseType.MAIN2).setStopAtPhase(prefs.getPrefBoolean(FPref.PHASE_HUMAN_MAIN2));
        fvHuman.getLabel(PhaseType.END_OF_TURN).setStopAtPhase(prefs.getPrefBoolean(FPref.PHASE_HUMAN_EOT));
        fvHuman.getLabel(PhaseType.CLEANUP).setStopAtPhase(prefs.getPrefBoolean(FPref.PHASE_HUMAN_CLEANUP));
    }
    
    /** Returns a random name from the supplied list. */
    public static String getRandomName() {
        String playerName = GuiDisplayUtil.getPlayerName();
        String aiName = NameGenerator.getRandomName("Any", "Generic", playerName);
        return aiName;
    }
    
    public final static LobbyPlayer getAiPlayer() { return getAiPlayer(getRandomName()); }
    public final static LobbyPlayer getAiPlayer(String name) {
        int avatarCount = GuiBase.getInterface().getAvatarCount();
        return getAiPlayer(name, avatarCount == 0 ? 0 : MyRandom.getRandom().nextInt(avatarCount));
    }
    public final static LobbyPlayer getAiPlayer(String name, int avatarIndex) {
        LobbyPlayerAi player = new LobbyPlayerAi(name);

        // TODO: implement specific AI profiles for quest mode.
        String lastProfileChosen = FModel.getPreferences().getPref(FPref.UI_CURRENT_AI_PROFILE);
        player.setRotateProfileEachGame(lastProfileChosen.equals(AiProfileUtil.AI_PROFILE_RANDOM_DUEL));
        if(lastProfileChosen.equals(AiProfileUtil.AI_PROFILE_RANDOM_MATCH)) {
            lastProfileChosen = AiProfileUtil.getRandomProfile();
            System.out.println(String.format("AI profile %s was chosen for the lobby player %s.", lastProfileChosen, player.getName()));
        }
        player.setAiProfile(lastProfileChosen);
        player.setAvatarIndex(avatarIndex);
        return player;
    }

    private final static LobbyPlayer guiPlayer = new LobbyPlayerHuman("Human", GuiBase.getInterface());
    public final static LobbyPlayer getGuiPlayer() {
        return guiPlayer;
    }

    public static FImage getPlayerAvatar(final PlayerView p) {
        LobbyPlayer lp = p.getLobbyPlayer();
        FImage avatar = avatarImages.get(lp);
        if (avatar == null) {
            avatar = new FTextureRegionImage(FSkin.getAvatars().get(lp.getAvatarIndex()));
        }
        return avatar;
    }

    public static void setPlayerAvatar(final LobbyPlayer lp, final FImage avatarImage) {
        avatarImages.put(lp, avatarImage);
    }
}
