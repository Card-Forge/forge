package forge.screens.match;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.tuple.Pair;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;

import forge.FThreads;
import forge.Forge;
import forge.game.Game;
import forge.game.Match;
import forge.game.card.Card;
import forge.game.combat.Combat;
import forge.game.phase.PhaseType;
import forge.game.player.LobbyPlayer;
import forge.game.player.Player;
import forge.game.zone.Zone;
import forge.game.zone.ZoneType;
import forge.model.FModel;
import forge.screens.match.events.IUiEventVisitor;
import forge.screens.match.events.UiEvent;
import forge.screens.match.events.UiEventAttackerDeclared;
import forge.screens.match.events.UiEventBlockerAssigned;
import forge.screens.match.input.InputQueue;
import forge.screens.match.views.VPhaseIndicator.PhaseLabel;
import forge.screens.match.views.VPlayerPanel;
import forge.utils.ForgePreferences.FPref;

public class FControl {
    private FControl() { } //don't allow creating instance

    private static Game game;
    private static MatchScreen view;
    private static InputQueue inputQueue;
    private static List<Player> sortedPlayers;
    private static final EventBus uiEvents;
    private static MatchUiEventVisitor visitor = new MatchUiEventVisitor();

    static {
        uiEvents = new EventBus("ui events");
        //uiEvents.register(Singletons.getControl().getSoundSystem());
        uiEvents.register(visitor);
    }

    public static void startGame(final Match match, final MatchScreen view0) {
        game = match.createGame();
        view = view0;

        /*if (game.getRules().getGameType() == GameType.Quest) {
            QuestController qc = Singletons.getModel().getQuest();
            // Reset new list when the Match round starts, not when each game starts
            if (game.getMatch().getPlayedGames().isEmpty()) {
                qc.getCards().resetNewList();
            }
            game.subscribeToEvents(qc); // this one listens to player's mulligans ATM
        }*/

        inputQueue = new InputQueue();

        //game.subscribeToEvents(Singletons.getControl().getSoundSystem());

        LobbyPlayer humanLobbyPlayer = game.getRegisteredPlayers().get(0).getLobbyPlayer(); //FServer.instance.getLobby().getGuiPlayer();
        // The UI controls should use these game data as models
        initMatch(game.getRegisteredPlayers(), humanLobbyPlayer);

        // It's important to run match in a different thread to allow GUI inputs to be invoked from inside game. 
        // Game is set on pause while gui player takes decisions
        /*game.getAction().invoke(new Runnable() {
            @Override
            public void run() {
                match.startGame(game);
            }
        });*/
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

    public static boolean stopAtPhase(final Player turn, final PhaseType phase) {
        PhaseLabel label = getPlayerPanel(turn).getPhaseIndicator().getLabel(phase);
        return label == null || label.getStopAtPhase();
    }

    public static void setCard(final Card c) {
        FThreads.assertExecutedByEdt(true);
        setCard(c, false);
    }

    public static void setCard(final Card c, final boolean showFlipped) {
        //TODO
    }

    private static int getPlayerIndex(Player player) {
        return sortedPlayers.indexOf(player);
    }

    public static void endCurrentGame() {
        if (game == null) { return; }

        Forge.back();
        game = null;
    }

    public static void initMatch(final List<Player> players, LobbyPlayer localPlayer) {
        // TODO fix for use with multiplayer

        final String[] indices = FModel.getPreferences().getPref(FPref.UI_AVATARS).split(",");

        // Instantiate all required field slots (user at 0)
        sortedPlayers = shiftPlayersPlaceLocalFirst(players, localPlayer);

        /*final List<VField> fields = new ArrayList<VField>();
        final List<VCommand> commands = new ArrayList<VCommand>();

        int i = 0;
        for (Player p : sortedPlayers) {
            // A field must be initialized after it's instantiated, to update player info.
            // No player, no init.
            VField f = new VField(EDocID.Fields[i], p, localPlayer);
            VCommand c = new VCommand(EDocID.Commands[i], p);
            fields.add(f);
            commands.add(c);

            //setAvatar(f, new ImageIcon(FSkin.getAvatars().get()));
            setAvatar(f, getPlayerAvatar(p, Integer.parseInt(indices[i > 2 ? 1 : 0])));
            f.getLayoutControl().initialize();
            c.getLayoutControl().initialize();
            i++;
        }

        // Replace old instances
        view.setCommandViews(commands);
        view.setFieldViews(fields);

        VPlayers.SINGLETON_INSTANCE.init(players);*/

        initHandViews(localPlayer);
    }

    public static void initHandViews(LobbyPlayer localPlayer) {
        /*final List<VHand> hands = new ArrayList<VHand>();

        int i = 0;
        for (Player p : sortedPlayers) {
            if (p.getLobbyPlayer() == localPlayer) {
                VHand newHand = new VHand(EDocID.Hands[i], p);
                newHand.getLayoutControl().initialize();
                hands.add(newHand);
            }
            i++;
        }

        if (hands.isEmpty()) { // add empty hand for matches without human
            VHand newHand = new VHand(EDocID.Hands[0], null);
            newHand.getLayoutControl().initialize();
            hands.add(newHand);
        }
        view.setHandViews(hands);*/
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
        for (final VPlayerPanel panel : view.getPlayerPanels()) {
            panel.getPhaseIndicator().resetPhaseButtons();
        }
    }

    public static void showMessage(final String s0) {
        view.getPrompt().setMessage(s0);
    }

    public static VPlayerPanel getPlayerPanel(Player p) {
        int idx = getPlayerIndex(p);
        return idx < 0 ? null : view.getPlayerPanels().get(idx);
    }

    public static boolean mayShowCard(Card c) {
        return true;// game == null || !gameHasHumanPlayer || c.canBeShownTo(getCurrentPlayer());
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
    } // showBlockers()

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
        //System.out.println("updateZones " + zonesToUpdate);
        /*for (Pair<Player, ZoneType> kv : zonesToUpdate) {
            Player owner = kv.getKey();
            ZoneType zt = kv.getValue();

            if (zt == ZoneType.Command)
                getCommandFor(owner).getTabletop().setupPlayZone();
            else if (zt == ZoneType.Hand) {
                VHand vHand = getHandFor(owner);
                if (null != vHand)
                    vHand.getLayoutControl().updateHand();
                getFieldViewFor(owner).getDetailsPanel().updateZones();
            }
            else if (zt == ZoneType.Battlefield) {
                getFieldViewFor(owner).getTabletop().setupPlayZone();
            } else if (zt == ZoneType.Ante) {
                CAntes.SINGLETON_INSTANCE.update();
            }
            else {
                getFieldViewFor(owner).getDetailsPanel().updateZones();
            }
        }*/
    }

    // Player's mana pool changes
    public static void updateManaPool(List<Player> manaPoolUpdate) {
        /*for (Player p : manaPoolUpdate) {
            getFieldViewFor(p).getDetailsPanel().updateManaPool();
        }*/
    }

    // Player's lives and poison counters
    public static void updateLives(List<Player> livesUpdate) {
        /*for (Player p : livesUpdate) {
            getFieldViewFor(p).updateDetails();
        }*/
    }

    public static void updateCards(Set<Card> cardsToUpdate) {
        for (Card c : cardsToUpdate) {
            updateSingleCard(c);
        }
    }

    public static void updateSingleCard(Card c) {
        Zone zone = c.getZone();
        if (zone != null && zone.getZoneType() == ZoneType.Battlefield) {
            /*PlayArea pa = getFieldViewFor(zone.getPlayer()).getTabletop();
            pa.updateSingleCard(c);*/
        }
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
