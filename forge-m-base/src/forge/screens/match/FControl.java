package forge.screens.match;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.tuple.Pair;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;

import forge.FThreads;
import forge.Forge;
import forge.game.Game;
import forge.game.GameEntity;
import forge.game.GameRules;
import forge.game.GameType;
import forge.game.Match;
import forge.game.card.Card;
import forge.game.card.CardLists;
import forge.game.card.CardPredicates.Presets;
import forge.game.combat.Combat;
import forge.game.combat.CombatUtil;
import forge.game.phase.PhaseHandler;
import forge.game.phase.PhaseType;
import forge.game.player.LobbyPlayer;
import forge.game.player.Player;
import forge.game.player.RegisteredPlayer;
import forge.game.zone.Zone;
import forge.game.zone.ZoneType;
import forge.model.FModel;
import forge.net.FServer;
import forge.screens.match.events.IUiEventVisitor;
import forge.screens.match.events.UiEvent;
import forge.screens.match.events.UiEventAttackerDeclared;
import forge.screens.match.events.UiEventBlockerAssigned;
import forge.screens.match.input.InputProxy;
import forge.screens.match.input.InputQueue;
import forge.screens.match.views.VAssignDamage;
import forge.screens.match.views.VPhaseIndicator.PhaseLabel;
import forge.screens.match.views.VPlayerPanel;
import forge.toolbox.FCardPanel;
import forge.toolbox.FOptionPane;
import forge.utils.ForgePreferences.FPref;

public class FControl {
    private FControl() { } //don't allow creating instance

    private static Game game;
    private static MatchScreen view;
    private static InputQueue inputQueue;
    private static InputProxy inputProxy;
    private static List<Player> sortedPlayers;
    private static final EventBus uiEvents;
    private static boolean gameHasHumanPlayer;
    private static boolean devMode;
    private static final MatchUiEventVisitor visitor = new MatchUiEventVisitor();
    private static final FControlGameEventHandler fcVisitor = new FControlGameEventHandler();
    private static final FControlGamePlayback playbackControl = new FControlGamePlayback();

    static {
        uiEvents = new EventBus("ui events");
        //uiEvents.register(Singletons.getControl().getSoundSystem());
        uiEvents.register(visitor);
    }

    public static void startMatch(GameType gameType, List<RegisteredPlayer> players) {
        startMatch(gameType, null, players);
    }
    public static void startMatch(GameType gameType, List<GameType> appliedVariants, List<RegisteredPlayer> players) {
        boolean useRandomFoil = FModel.getPreferences().getPrefBoolean(FPref.UI_RANDOM_FOIL);
        for (RegisteredPlayer rp : players) {
            rp.setRandomFoil(useRandomFoil);
        }

        GameRules rules = new GameRules(gameType);
        if (appliedVariants != null && !appliedVariants.isEmpty()) {
            rules.setAppliedVariants(appliedVariants);
        }
        rules.setPlayForAnte(FModel.getPreferences().getPrefBoolean(FPref.UI_ANTE));
        rules.setManaBurn(FModel.getPreferences().getPrefBoolean(FPref.UI_MANABURN));
        rules.canCloneUseTargetsImage = FModel.getPreferences().getPrefBoolean(FPref.UI_CLONE_MODE_SOURCE);

        startGame(new Match(rules, players));
    }

    public static void startGame(final Match match) {
        game = match.createGame();

        /*if (game.getRules().getGameType() == GameType.Quest) {
            QuestController qc = Singletons.getModel().getQuest();
            // Reset new list when the Match round starts, not when each game starts
            if (game.getMatch().getPlayedGames().isEmpty()) {
                qc.getCards().resetNewList();
            }
            game.subscribeToEvents(qc); // this one listens to player's mulligans ATM
        }*/

        inputQueue = new InputQueue();
        inputProxy = new InputProxy();

        //game.subscribeToEvents(Singletons.getControl().getSoundSystem());

        LobbyPlayer humanLobbyPlayer = game.getRegisteredPlayers().get(0).getLobbyPlayer(); //FServer.instance.getLobby().getGuiPlayer();
        // The UI controls should use these game data as models
        initMatch(game.getRegisteredPlayers(), humanLobbyPlayer);

        FModel.getPreferences().actuateMatchPreferences();
        inputProxy.setGame(game);

        // Listen to DuelOutcome event to show ViewWinLose
        game.subscribeToEvents(fcVisitor);

        // Add playback controls to match if needed
        gameHasHumanPlayer = false;
        for (Player p :  game.getPlayers()) {
            if (p.getController().getLobbyPlayer() == FServer.getLobby().getGuiPlayer()) {
                gameHasHumanPlayer = true;
            }
        }
        if (!gameHasHumanPlayer) {
            game.subscribeToEvents(playbackControl);
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

    public static MatchScreen getView() {
        return view;
    }

    public static InputQueue getInputQueue() {
        return inputQueue;
    }

    public static InputProxy getInputProxy() {
        return inputProxy;
    }

    public static boolean stopAtPhase(final Player turn, final PhaseType phase) {
        PhaseLabel label = getPlayerPanel(turn).getPhaseIndicator().getLabel(phase);
        return label == null || label.getStopAtPhase();
    }

    public static void endCurrentTurn() {
        Player p = getCurrentPlayer();

        if (p != null) {
            p.getController().autoPassUntil(PhaseType.CLEANUP);
            if (!inputProxy.passPriority()) {
                p.getController().autoPassCancel();
            }
        }
    }

    public static void setCard(final Card c) {
        FThreads.assertExecutedByEdt(true);
        setCard(c, false);
    }

    public static void setCard(final Card c, final boolean showFlipped) {
        //TODO
    }

    public static void initMatch(final List<Player> players, LobbyPlayer localPlayer) {
        final String[] indices = FModel.getPreferences().getPref(FPref.UI_AVATARS).split(",");

        // Instantiate all required field slots (user at 0)
        sortedPlayers = shiftPlayersPlaceLocalFirst(players, localPlayer);

        List<VPlayerPanel> playerPanels = new ArrayList<VPlayerPanel>();

        int i = 0;
        int avatarIndex = 0;
        for (Player p : sortedPlayers) {
            if (i < indices.length) {
                avatarIndex = Integer.parseInt(indices[i]);
                i++;
            }
            p.getLobbyPlayer().setAvatarIndex(avatarIndex);
            playerPanels.add(new VPlayerPanel(p));
        }

        view = new MatchScreen(playerPanels) {
            @Override
            public void onActivate() {
                devMode = FModel.getPreferences().getPrefBoolean(FPref.DEV_MODE_ENABLED); //cache devMode for performance when match screen opened
                super.onActivate();
            }
        };
    }

    private static List<Player> shiftPlayersPlaceLocalFirst(final List<Player> players, LobbyPlayer localPlayer) {
        // get an arranged list so that the first local player is at index 0
        List<Player> sortedPlayers = new ArrayList<Player>(players);
        int ixFirstHuman = -1;
        for (int i = 0; i < players.size(); i++) {
            if (sortedPlayers.get(i).getLobbyPlayer() == localPlayer) {
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
        if (view.getCardZoom().isVisible() &&
                view.getCardZoom().getPrompt().getMessage().equals(view.getPrompt().getMessage())) {
            //update zoom view's prompt message if it's shared with main view's prompt's message
            view.getCardZoom().getPrompt().setMessage(s0);
        }
        view.getPrompt().setMessage(s0);
    }

    public static VPlayerPanel getPlayerPanel(Player p) {
        return view.getPlayerPanels().get(p);
    }

    public static void highlightCard(final Card c) {
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

    public static Iterable<Player> getSortedPlayers() {
        return sortedPlayers;
    }

    public static Player getCurrentPlayer() {
        // try current priority
        Player currentPriority = game.getPhaseHandler().getPriorityPlayer();
        if (null != currentPriority && currentPriority.getLobbyPlayer() == FServer.getLobby().getGuiPlayer()) {
            return currentPriority;
        }

        // otherwise find just any player, belonging to this lobbyplayer
        for (Player p : game.getPlayers()) {
            if (p.getLobbyPlayer() == FServer.getLobby().getGuiPlayer()) {
                return p;
            }
        }

        return null;
    }

    public static boolean mayShowCard(Card c) {
        return game == null || !gameHasHumanPlayer || devMode || c.canBeShownTo(getCurrentPlayer());
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

    public static void showCombat(Combat combat) {
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

    @SuppressWarnings("unchecked")
    public static Map<Card, Integer> getDamageToAssign(final Card attacker, final List<Card> blockers, final int damage, final GameEntity defender, final boolean overrideOrder) {
        if (damage <= 0) {
            return new HashMap<Card, Integer>();
        }

        // If the first blocker can absorb all of the damage, don't show the Assign Damage Frame
        Card firstBlocker = blockers.get(0);
        if (!overrideOrder && !attacker.hasKeyword("Deathtouch") && firstBlocker.getLethalDamage() >= damage) {
            Map<Card, Integer> res = new HashMap<Card, Integer>();
            res.put(firstBlocker, damage);
            return res;
        }

        final Object[] result = { null }; // how else can I extract a value from EDT thread?
        FThreads.invokeInEdtAndWait(new Runnable() {
            @Override
            public void run() {
                VAssignDamage v = new VAssignDamage(attacker, blockers, damage, defender, overrideOrder);
                result[0] = v.getDamageMap();
            }});
        return (Map<Card, Integer>)result[0];
    }

    private static Set<Player> highlightedPlayers = new HashSet<Player>();
    public static void setHighlighted(Player ge, boolean b) {
        if (b) highlightedPlayers.add(ge);
        else highlightedPlayers.remove(ge);
    }

    public static boolean isHighlighted(Player player) {
        return highlightedPlayers.contains(player);
    }

    private static Set<Card> highlightedCards = new HashSet<Card>();
    // used to highlight cards in UI
    public static void setUsedToPay(Card card, boolean value) {
        FThreads.assertExecutedByEdt(true);

        boolean hasChanged = value ? highlightedCards.add(card) : highlightedCards.remove(card);
        if (hasChanged) { // since we are in UI thread, may redraw the card right now
            updateSingleCard(card);
        }
    }

    public static boolean isUsedToPay(Card card) {
        return highlightedCards.contains(card);
    }

    public static void updateZones(List<Pair<Player, ZoneType>> zonesToUpdate) {
        for (Pair<Player, ZoneType> kv : zonesToUpdate) {
            Player owner = kv.getKey();
            ZoneType zt = kv.getValue();
            getPlayerPanel(owner).updateZone(zt);
        }
    }

    // Player's mana pool changes
    public static void updateManaPool(List<Player> manaPoolUpdate) {
        for (Player p : manaPoolUpdate) {
            getPlayerPanel(p).updateManaPool();
        }
    }

    // Player's lives and poison counters
    public static void updateLives(List<Player> livesUpdate) {
        for (Player p : livesUpdate) {
            getPlayerPanel(p).updateLife();
        }
    }

    public static void updateCards(Set<Card> cardsToUpdate) {
        for (Card c : cardsToUpdate) {
            updateSingleCard(c);
        }
    }

    public static void updateSingleCard(Card c) {
        Zone zone = c.getZone();
        if (zone != null && zone.getZoneType() == ZoneType.Battlefield) {
            getPlayerPanel(zone.getPlayer()).getField().updateCard(c);
        }
    }
    public static void undoLastAction() {
        Game game = getGame();
        Player player = game.getPhaseHandler().getPriorityPlayer();
        if (player != null && player.getLobbyPlayer() == FServer.getLobby().getGuiPlayer()) {
            game.stack.undo();
        }
    }

    /** Concede game, bring up WinLose UI. */
    public static void concede() {
        String userPrompt =
                "This will end the current game and you will not be able to resume.\n\n" +
                        "Concede anyway?";
        if (FOptionPane.showConfirmDialog(userPrompt, "Concede Game?", "Concede", "Cancel", false)) {
            stopGame();
        }
    }

    public static void stopGame() {
        List<Player> pp = new ArrayList<Player>();
        for (Player p : game.getPlayers()) {
            if (p.getOriginalLobbyPlayer() == FServer.getLobby().getGuiPlayer()) {
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
        boolean humanHasPriority = priorityPlayer == null || priorityPlayer.getLobbyPlayer() == FServer.getLobby().getGuiPlayer();

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
}
