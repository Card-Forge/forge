package forge.api;

import com.google.gson.JsonObject;
import com.google.common.eventbus.Subscribe;
import forge.StaticData;
import forge.ai.LobbyPlayerAi;
import forge.card.MagicColor;
import forge.deck.Deck;
import forge.game.*;
import forge.game.event.GameEventTurnPhase;
import forge.game.card.CardView;
import forge.game.player.*;
import forge.game.spellability.SpellAbilityView;
import forge.game.zone.ZoneType;
import forge.gamemodes.match.input.*;
import forge.gui.control.PlaybackSpeed;
import forge.gui.interfaces.IGuiGame;
import forge.item.PaperCard;
import forge.player.LobbyPlayerHuman;
import forge.player.PlayerControllerHuman;
import forge.player.PlayerZoneUpdates;
import forge.util.FSerializableFunction;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.*;

/** One local human with one or more AI opponents. Mutable engine objects never leave this adapter. */
public final class MatchSession {
    private static final forge.util.ITriggerEvent CARD_CLICK = new forge.util.ITriggerEvent() {
        public int getButton() { return 1; }
        public int getX() { return 0; }
        public int getY() { return 0; }
    };
    private final String id = UUID.randomUUID().toString();
    private final Game game;
    private final String format;
    private final PlayerControllerHuman human;
    private final PlayerView viewer;
    private final MatchActivity activity;
    private final IGuiGame gui;
    private final Object gate = new Object();
    private final Set<CardView> selectable = new HashSet<>();
    private final Set<CardView> actionable = new HashSet<>();
    private final Set<GameEntityView> highlighted = new HashSet<>();
    private final List<String> notices = new ArrayList<>();
    private volatile Map<String, Object> latest;
    private volatile Pending pending;
    private volatile boolean closed;
    private volatile String error;
    private long revision;
    private long activityRevision;
    private String message = "Preparing the match…", ok = "Continue", cancel = "Cancel";
    private boolean okEnabled, cancelEnabled;
    private Input displayedInput;

    private static final class Pending {
        final String id = UUID.randomUUID().toString();
        final String kind;
        final Input input;
        final Map<String, CardView> cards = new LinkedHashMap<>();
        final CompletableFuture<JsonObject> response = new CompletableFuture<>();
        Map<String, Object> prompt;
        int size, min, max;
        int amount;
        boolean atLeastOne, numeric, maySkip;
        List<Integer> limits = List.of();
        java.util.function.Consumer<List<Integer>> validateOrder;
        int defenderIndex = -1;
        List<Integer> lethal = List.of();
        Pending(String kind, Input input) { this.kind = kind; this.input = input; }
    }

    public MatchSession(Deck deck, String format, List<String> opponents, Path resources, Path profile) {
        validateOpponents(format, opponents);
        HeadlessPlatform.initialize(resources, profile);
        this.format = format;
        boolean commander = format.equals("Commander");
        var rules = new GameRules(GameType.Constructed);
        if (commander) rules.addAppliedVariant(GameType.Commander);
        rules.setGamesPerMatch(1);
        rules.setWarnAboutAICards(false); // Opponent lists are supplied by the host, not chosen by the player.
        var humanPlayer = (commander ? RegisteredPlayer.forCommander(new Deck(deck)) : new RegisteredPlayer(new Deck(deck))).setPlayer(new LobbyPlayerHuman("You"));
        var seats = new ArrayList<RegisteredPlayer>();
        seats.add(humanPlayer);
        for (String opponent : opponents) {
            var preset = commander && opponent.startsWith("preset:") ? DeckPresets.find(opponent.substring(7)) : null;
            String name = preset != null ? preset.name() : opponent.equals("red") ? "Cinder" : "Verdant";
            var ai = new LobbyPlayerAi(name + " · AI" + (opponents.size() > 1 ? " " + seats.size() : ""), Set.of());
            ai.setAiProfile("Default");
            var aiDeck = preset != null ? DeckPresets.create(preset.id()) : commander ? CommanderOpponents.create(opponent) : opponentDeck(opponent);
            seats.add((commander ? RegisteredPlayer.forCommander(aiDeck) : new RegisteredPlayer(aiDeck)).setPlayer(ai));
        }
        var match = new Match(rules, seats, "Mana Table");
        game = match.createGame();
        human = (PlayerControllerHuman) game.getPlayers().get(0).getController();
        viewer = human.getPlayer().getView();
        activity = new MatchActivity(viewer);
        game.subscribeToEvents(activity);
        game.subscribeToEvents(this);
        gui = (IGuiGame) Proxy.newProxyInstance(IGuiGame.class.getClassLoader(), new Class<?>[]{IGuiGame.class}, (proxy, method, args) -> {
            if (method.isDefault()) return InvocationHandler.invokeDefault(proxy, method, args);
            return invokeGui(method.getName(), args == null ? new Object[0] : args);
        });
        human.setGui(gui);
        for (var player : game.getPlayers()) player.updateOpponentsForView();
        latest = Map.of("id", id, "revision", 0L, "status", "starting", "message", message, "format", format);
        HeadlessPlatform.activate(this);
        game.getAction().invoke(() -> {
            try {
                match.startGame(game);
                synchronized (gate) { pending = null; publish(null); }
            } catch (Throwable failure) { if (!closed) fail(failure); }
            finally { game.unsubscribeFromEvents(activity); game.unsubscribeFromEvents(this); }
        });
    }

    IGuiGame gui() { return gui; }
    public Map<String, Object> state() {
        synchronized (gate) {
            // During AI work, publish only event-time copies. Never read a live board from IPC.
            if ("resolving".equals(latest.get("status"))) {
                var frame = activity.frame();
                if (frame.revision() != activityRevision) {
                    activityRevision = frame.revision();
                    var next = new LinkedHashMap<>(latest);
                    next.put("revision", ++revision);
                    next.put("turn", frame.turn()); next.put("phase", frame.phase()); next.put("phaseKey", frame.phaseKey());
                    next.put("activePlayerId", frame.activePlayerId()); next.put("activity", frame.entries());
                    latest = Collections.unmodifiableMap(next);
                }
            }
            return latest;
        }
    }
    public boolean finished() { return closed || error != null || game.isGameOver(); }

    static void validateOpponents(String format, List<String> opponents) {
        int maximum = format.equals("Commander") ? 5 : 1;
        if (opponents.isEmpty() || opponents.size() > maximum) throw new IllegalArgumentException(
                format.equals("Commander") ? "Choose 1–5 AI opponents for a 2–6 player Commander table" : "Constructed supports one AI opponent");
        var allowed = opponents(format).stream().map(opponent -> opponent.get("id")).toList();
        for (String opponent : opponents) if (!allowed.contains(opponent)) throw new IllegalArgumentException("Unknown opponent: " + opponent);
    }

    @Subscribe
    public void onPhase(GameEventTurnPhase event) {
        // A local table ends when its only human is eliminated. Do not leave an
        // invisible AI-only game running after the player starts another table.
        if (viewer.getHasLost() && !game.isGameOver()) {
            game.setGameOver(GameEndReason.AllHumansLost);
            human.getInputQueue().onGameOver(true);
        }
    }

    public static List<Map<String, String>> opponents(String format) {
        if (format.equals("Commander")) {
            var opponents = new ArrayList<Map<String, String>>();
            for (var preset : DeckPresets.list()) opponents.add(Map.of("id", "preset:" + preset.id(), "name", preset.name(),
                    "description", preset.commanders().get(0).name() + " · " + preset.theme() + " · Commander · 100 cards"));
            opponents.add(Map.of("id", "green", "name", "Goreclaw · Verdant", "description", "Goreclaw, Terror of Qal Sisma · Green ramp and big creatures · Commander · 100 cards"));
            opponents.add(Map.of("id", "red", "name", "Torbran · Cinder", "description", "Torbran, Thane of Red Fell · Red creatures and damage · Commander · 100 cards"));
            return List.copyOf(opponents);
        }
        return List.of(Map.of("id", "green", "name", "Verdant", "description", "Green creatures, mana ramp, and combat tricks · Constructed · 60 cards"),
                Map.of("id", "red", "name", "Cinder", "description", "Red creatures and direct damage · Constructed · 60 cards"));
    }

    private static Deck opponentDeck(String key) {
        String[] lines = switch (key) {
            case "green" -> new String[]{"24 Forest", "4 Llanowar Elves", "4 Elvish Mystic", "4 Grizzly Bears", "4 Elvish Visionary", "4 Centaur Courser", "4 Giant Growth", "4 Colossal Dreadmaw", "4 Rumbling Baloth", "4 Sentinel Spider"};
            case "red" -> new String[]{"24 Mountain", "4 Monastery Swiftspear", "4 Ghitu Lavarunner", "4 Shock", "4 Lightning Bolt", "4 Lightning Strike", "4 Goblin Arsonist", "4 Borderland Marauder", "4 Viashino Pyromancer", "4 Chandra's Pyrohelix"};
            default -> throw new IllegalArgumentException("Unknown opponent");
        };
        Deck deck = new Deck(key);
        for (String line : lines) {
            int split = line.indexOf(' ');
            PaperCard card = StaticData.instance().getCommonCards().getCard(line.substring(split + 1));
            if (card == null) throw new IllegalStateException("Missing opponent card: " + line);
            deck.getMain().add(card, Integer.parseInt(line.substring(0, split)));
        }
        return deck;
    }

    /** Called only after an input-display runnable completes, while the game awaits that input. */
    void publishInput() {
        synchronized (gate) {
            if (closed || error != null || pending != null && !pending.kind.equals("input")) return;
            Input current = human.getInputQueue().getInput();
            if (current == null || current != displayedInput || current != human.getInputProxy().getInput() || game.isGameOver()) return;
            Pending next = new Pending("input", current);
            // Cleanup's discard input (among others) is an anonymous subclass.
            // Preserve its actual input kind instead of publishing an empty name.
            Class<?> inputClass = current.getClass();
            while (inputClass.getSimpleName().isEmpty()) inputClass = inputClass.getSuperclass();
            var playerChoices = current instanceof InputSelectEntitiesFromList<?> selection
                    ? selection.getValidChoices().stream().filter(Player.class::isInstance).map(entity -> ((Player) entity).getId()).toList() : List.of();
            next.prompt = map("id", next.id, "kind", "input", "inputType", inputClass.getSimpleName(),
                    "message", message, "ok", ok, "cancel", cancel, "okEnabled", okEnabled, "cancelEnabled", cancelEnabled,
                    "canAttackAll", current instanceof InputAttack, "playerChoices", playerChoices);
            pending = next;
            publish(next);
        }
    }

    public Map<String, Object> action(JsonObject request) {
        synchronized (gate) {
            requireSession(request);
            Pending next = pending;
            if (next == null || !next.id.equals(string(request, "promptId"))) throw new IllegalArgumentException("That choice has changed. Use the current prompt.");
            if (!next.kind.equals("input")) {
                validateDialog(next, request);
                pending = null;
                markBusy();
                next.response.complete(request.deepCopy());
                return latest;
            }
            String action = string(request, "action");
            CardView card = null;
            PlayerView target = null;
            switch (action) {
                case "ok" -> { if (!okEnabled) throw new IllegalArgumentException("Continue is not available"); }
                case "cancel" -> { if (!cancelEnabled) throw new IllegalArgumentException("Cancel is not available"); }
                case "attackAll" -> { if (!(next.input instanceof InputAttack)) throw new IllegalArgumentException("Not declaring attackers"); }
                case "card" -> { card = next.cards.get(string(request, "key")); if (card == null) throw new IllegalArgumentException("Card is not visible in this prompt"); }
                case "player" -> {
                    int playerId = exactInt(string(request, "playerId"));
                    target = game.getView().getPlayers().stream().filter(p -> p.getId() == playerId).findFirst().orElseThrow();
                }
                default -> throw new IllegalArgumentException("Unknown match action");
            }
            CardView chosenCard = card;
            PlayerView chosenPlayer = target;
            pending = null;
            markBusy();
            HeadlessPlatform.later(() -> {
                if (closed || human.getInputQueue().getInput() != next.input) return;
                switch (action) {
                    case "ok" -> human.selectButtonOk();
                    case "cancel" -> human.selectButtonCancel();
                    case "attackAll" -> human.alphaStrike();
                    case "card" -> human.selectCard(chosenCard, null, CARD_CLICK);
                    case "player" -> human.selectPlayer(chosenPlayer, null);
                }
            });
            return latest;
        }
    }

    public Map<String, Object> concede(JsonObject request) {
        synchronized (gate) {
            requireSession(request);
            if (game.isGameOver()) return latest;
            closed = true;
            Pending old = pending;
            pending = null;
            if (old != null) old.response.completeExceptionally(new CancellationException("Match conceded"));
            human.concede();
            if (!game.isGameOver()) game.setGameOver(GameEndReason.AllHumansLost);
            human.getInputQueue().onGameOver(true);
            publish(null);
            return latest;
        }
    }

    private void requireSession(JsonObject request) {
        if (!id.equals(string(request, "sessionId"))) throw new IllegalArgumentException("This match is no longer active");
        if (error != null) throw new IllegalStateException(error);
    }

    private void markBusy() {
        var next = new LinkedHashMap<>(latest);
        next.put("revision", ++revision); next.put("prompt", null); next.put("status", "resolving");
        latest = Collections.unmodifiableMap(next);
    }

    private void publish(Pending prompt) {
        var view = game.getView();
        var players = new ArrayList<Object>();
        var allPlayers = game.getRegisteredPlayers().stream().map(Player::getView).toList();
        for (PlayerView player : allPlayers) {
            var zones = new ArrayList<Object>();
            for (ZoneType zone : List.of(ZoneType.Battlefield, ZoneType.Hand, ZoneType.Library, ZoneType.Graveyard, ZoneType.Exile, ZoneType.Command)) {
                var visible = new ArrayList<Object>();
                var cards = player.getCards(zone);
                if (cards != null) for (CardView card : cards) if (card.canBeShownTo(viewer)) visible.add(cardState(card, prompt));
                zones.add(map("name", zone.name(), "count", player.getZoneSize(zone), "cards", visible));
            }
            var mana = new LinkedHashMap<String, Integer>();
            byte[] colors = {MagicColor.WHITE, MagicColor.BLUE, MagicColor.BLACK, MagicColor.RED, MagicColor.GREEN, MagicColor.COLORLESS};
            String[] labels = {"W", "U", "B", "R", "G", "C"};
            for (int i = 0; i < colors.length; i++) mana.put(labels[i], player.getMana(colors[i]));
            var commanderDamage = new ArrayList<Object>();
            if (format.equals("Commander")) for (PlayerView owner : allPlayers) {
                if (owner.equals(player) || owner.getCommanders() == null) continue;
                for (CardView commander : owner.getCommanders()) commanderDamage.add(map("name", commander.isFaceDown() ? "Face-down commander" : commander.getCurrentState().getName(),
                        "ownerId", owner.getId(), "owner", owner.getName(), "damage", player.getCommanderDamage(commander)));
            }
            players.add(map("id", player.getId(), "name", player.equals(viewer) ? "You" : player.getName(), "human", player.equals(viewer),
                    "seat", allPlayers.indexOf(player) + 1, "eliminated", player.getHasLost(),
                    "life", player.getLife(), "priority", player.getHasPriority(), "mana", mana, "zones", zones, "commanderDamage", commanderDamage));
        }
        var stack = new ArrayList<Object>();
        for (var item : view.getStack()) {
            CardView source = item.getSourceCard();
            boolean visible = source != null && source.canBeShownTo(viewer) && !source.isFaceDown();
            stack.add(map("name", visible ? source.getCurrentState().getName() : "Face-down spell", "text", visible ? item.getText() : "",
                    "controller", item.getActivatingPlayer() == null ? "" : item.getActivatingPlayer().getName()));
        }
        String result = null;
        if (game.isGameOver() && game.getOutcome() != null) {
            var outcome = game.getOutcome();
            result = viewer.getHasLost() ? "Defeat" : outcome.isDraw() ? "Draw" : outcome.getWinningLobbyPlayer() == human.getLobbyPlayer() ? "Victory" : "Defeat";
        }
        var activityFrame = activity.frame();
        activityRevision = activityFrame.revision();
        latest = Collections.unmodifiableMap(map("id", id, "revision", ++revision, "boardRevision", revision, "format", format, "status", error != null ? "error" : game.isGameOver() ? "finished" : "playing",
                "error", error, "playerCount", allPlayers.size(), "viewerId", viewer.getId(), "turn", view.getTurn(), "phase", view.getPhase() == null ? "Pregame" : view.getPhase().nameForUi,
                "phaseKey", view.getPhase() == null ? "PREGAME" : view.getPhase().name(),
                "activePlayerId", view.getPlayerTurn() == null ? null : view.getPlayerTurn().getId(), "players", players,
                "stack", stack, "prompt", prompt == null ? null : prompt.prompt, "result", result, "notices", List.copyOf(notices), "activity", activityFrame.entries()));
    }

    private Map<String, Object> cardState(CardView card, Pending prompt) {
        boolean hidden = card.isFaceDown();
        var face = card.getCurrentState();
        // Never let an old card handle identify a different card in a later prompt.
        String key = prompt == null ? "" : prompt.id + ":c" + prompt.cards.size();
        if (prompt != null) prompt.cards.put(key, card);
        var counters = new LinkedHashMap<String, Integer>();
        if (card.getCounters() != null) for (var entry : card.getCounters().entrySet()) counters.put(entry.getElement().getName(), entry.getCount());
        var combat = game.getView().getCombat();
        var defender = combat == null ? null : combat.getDefender(card);
        return map("key", key, "visualId", activity.visualId(card), "name", hidden ? "Face-down card" : face.getName(), "type", hidden ? "" : face.getType().toString(),
                "manaCost", hidden ? "" : face.getManaCost().toString(), "power", hidden ? null : face.getPower(), "toughness", hidden ? null : face.getToughness(),
                "text", hidden ? "" : card.getText(), "tapped", card.isTapped(), "sick", card.isSick(), "damage", card.getDamage(),
                "attacking", card.isAttacking(), "blocking", card.isBlocking(), "counters", counters,
                "defenderId", defender instanceof PlayerView ? defender.getId() : null,
                "defender", defender == null ? null : defender instanceof CardView target && (!target.canBeShownTo(viewer) || target.isFaceDown()) ? "Face-down permanent" : defender.getName(),
                "selectable", selectable.contains(card) || actionable.contains(card)
                        || prompt != null && prompt.input instanceof InputLondonMulligan && card.getController().equals(viewer) && card.getZone() == ZoneType.Hand,
                "highlighted", highlighted.contains(card), "faceDown", hidden);
    }

    void fail(Throwable failure) { failure.printStackTrace(System.err); fail(failure.getMessage() == null ? failure.getClass().getSimpleName() : failure.getMessage()); }
    void fail(String failure) {
        synchronized (gate) {
            if (closed) return;
            error = failure;
            closed = true;
            if (pending != null) pending.response.completeExceptionally(new IllegalStateException(failure));
            pending = null;
            var next = new LinkedHashMap<>(latest);
            next.put("revision", ++revision); next.put("status", "error"); next.put("error", failure); next.put("prompt", null);
            latest = Collections.unmodifiableMap(next);
            human.concede();
            if (!game.isGameOver()) game.setGameOver(GameEndReason.AllHumansLost);
            human.getInputQueue().onGameOver(true);
        }
    }

    @SuppressWarnings("unchecked")
    private Object invokeGui(String name, Object[] a) {
        return switch (name) {
            case "getGameView" -> game.getView();
            case "isLibgdxPort", "isNetGame", "isGamePaused", "isUiSetToSkipPhase" -> false;
            case "getGameSpeed" -> PlaybackSpeed.NORMAL;
            case "getDayTime" -> null;
            case "isSelecting" -> !selectable.isEmpty();
            case "showPromptMessage" -> { message = String.valueOf(a[1]); displayedInput = human.getInputProxy().getInput(); yield null; }
            case "updateButtons" -> { ok = (String)a[1]; cancel = (String)a[2]; okEnabled = (boolean)a[3]; cancelEnabled = (boolean)a[4]; yield null; }
            case "setSelectables" -> { selectable.clear(); ((Iterable<CardView>)a[0]).forEach(selectable::add); yield null; }
            case "clearSelectables" -> { selectable.clear(); yield null; }
            case "setWeaklySelectable" -> { actionable.clear(); ((Iterable<CardView>)a[0]).forEach(actionable::add); yield null; }
            case "clearWeaklySelectable" -> { actionable.clear(); yield null; }
            case "setHighlighted" -> { for (var value : (Iterable<GameEntityView>)a[0]) { if ((boolean)a[1]) highlighted.add(value); else highlighted.remove(value); } yield null; }
            case "openZones" -> new PlayerZoneUpdates();
            case "tempShowZones" -> a[1];
            case "getAbilityToPlay" -> {
                // Engine-driven choices (including triggers) have no pointer event and
                // are not activated abilities. Filtering them by canPlay silently drops triggers.
                var abilities = a[2] == null ? (List<SpellAbilityView>)a[1]
                        : ((List<SpellAbilityView>)a[1]).stream().filter(SpellAbilityView::canPlay).toList();
                yield abilities.size() == 1 && (a[2] == null || !abilities.get(0).promptIfOnlyPossibleAbility()) ? abilities.get(0)
                        : a[2] == null ? first(choose("Choose an ability of " + label(a[0]), 0, 1, abilities, false, null))
                        : chooseAbility((CardView)a[0], abilities);
            }
            case "one" -> first(choose((String)a[0], 1, 1, (List<?>)a[1], false, (FSerializableFunction<Object, String>)a[2]));
            case "oneOrNone" -> first(choose((String)a[0], 0, 1, (List<?>)a[1], false, null));
            case "getChoices" -> choose((String)a[0], (int)a[1], (int)a[2], (List<?>)a[3], false, (FSerializableFunction<Object, String>)a[5]);
            case "many" -> choose(a[0] + " · " + a[1], (int)a[2], (int)a[3], combine(a[4], a[5]), false, null);
            case "order" -> {
                List<?> choices = combine(a[4], a[5]);
                yield new IGuiGame.OrderResult<>(choose(a[0] + " · " + a[1], choices.size() - (int)a[3], choices.size() - (int)a[2], choices, true, null), false);
            }
            case "insertInList" -> { var choices = new ArrayList<Object>((List<?>)a[2]); choices.add(a[1]); yield choose((String)a[0], choices.size(), choices.size(), choices, true, null); }
            case "showConfirmDialog" -> first(choose(a[1] + "\n" + a[0], 1, 1, List.of(a[2], a[3]), false, null)).equals(a[2]);
            case "confirm" -> first(choose((String)a[1], 1, 1, (List<?>)a[3], false, null)).equals(((List<?>)a[3]).get(0));
            case "showOptionDialog" -> { var options = (List<?>)a[3]; yield options.indexOf(first(choose(a[1] + "\n" + a[0], 1, 1, options, false, null))); }
            case "getInteger" -> number((String)a[0], (int)a[1], (int)a[2]);
            case "showInputDialog" -> inputText(a);
            case "chooseSingleEntityForEffect" -> first(chooseForEffect((String)a[0], (boolean)a[3] ? 0 : 1, 1, (List<?>)a[1], (DelayedReveal)a[2]));
            case "chooseEntitiesForEffect" -> chooseForEffect((String)a[0], (int)a[2], (int)a[3], (List<?>)a[1], (DelayedReveal)a[4]);
            case "reveal" -> { reveal((String)a[0], (List<?>)a[1]); yield null; }
            case "message", "showErrorDialog" -> { synchronized (gate) { notices.add(String.valueOf(a[0])); while (notices.size() > 12) notices.remove(0); } yield null; }
            case "assignCombatDamage" -> combatDamage(a);
            case "assignGenericAmount" -> genericAmount(a);
            case "manipulateCardList" -> reorderCards(a);
            case "sideboard" -> null;
            case "setGameView", "setOriginalGameController", "setGameController", "setSpectator", "setCurrentPlayer", "openView",
                 "showCombat", "alertUser", "flashIncorrectAction", "updatePhase", "updateTurn", "updatePlayerControl", "enableOverlay", "disableOverlay",
                 "showManaPool", "hideManaPool", "updateStack", "notifyStackAddition", "notifyStackRemoval", "handleLandPlayed", "handleGameEvent",
                 "hideZones", "updateZones", "updateSingleCard", "updateCards", "updateRevealedCards", "refreshCardDetails", "refreshField",
                 "updateManaPool", "updateLives", "updateShards", "updateDependencies", "setPanelSelection", "setCard", "setPlayerAvatar",
                 "restoreOldZones", "awaitNextInput", "cancelAwaitNextInput", "showWaitingTimer", "updateAutoPassPrompt", "applyYieldUpdate",
                 "setGamePause", "setGameSpeed", "updateDayTime", "afterGameEnd", "finishGame" -> null;
            case "toString" -> "Mana Table match";
            default -> throw new UnsupportedOperationException("Match dialog is not implemented: " + name);
        };
    }

    Object platformDialog(String name, Object[] a) {
        if (name.equals("getChoices")) return choose((String)a[0], (int)a[1], (int)a[2], new ArrayList<>((Collection<?>)a[3]), false, (FSerializableFunction<Object, String>)a[5]);
        if (name.equals("chooseCard")) return first(choose(a[0] + "\n" + a[1], 1, 1, (List<?>)a[2], false, null));
        if (name.equals("order")) {
            var choices = combine(a[4], a[5]);
            return choose(a[0] + "\n" + a[1], choices.size() - (int)a[3], choices.size() - (int)a[2], choices, true, null);
        }
        return invokeGui(name, a);
    }

    private void revealDelayed(DelayedReveal reveal) { if (reveal != null) reveal(reveal.getMessagePrefix(), reveal.getCards()); }
    private void reveal(String title, List<?> choices) {
        if (!libraryCards(choices)) { dialog("reveal", title, choices, 0, 0, false, null); return; }
        var next = choicePrompt("reveal", title, List.of(), 0, 0, false, null);
        libraryPrompt(next, List.of(), choices, ((CardView)choices.get(0)).getController(), false);
        await(next);
    }

    private static boolean libraryCards(List<?> choices) {
        return !choices.isEmpty() && choices.stream().allMatch(item -> item instanceof CardView card && card.getZone() == ZoneType.Library)
                && choices.stream().map(item -> ((CardView)item).getController()).distinct().count() == 1;
    }

    private List<?> chooseForEffect(String title, int min, int max, List<?> choices, DelayedReveal reveal) {
        boolean library = reveal == null ? libraryCards(choices) : reveal.getZone().equals(Set.of(ZoneType.Library)) && choices.stream().allMatch(CardView.class::isInstance);
        if (!library) { revealDelayed(reveal); return choose(title, min, max, choices, false, null); }
        if (choices.isEmpty()) return List.of();
        var next = choicePrompt("choice", title, choices, Math.max(0, min), Math.min(choices.size(), max < 0 ? choices.size() : max), false, null);
        libraryPrompt(next, choices, reveal == null ? choices : reveal.getCards(),
                reveal == null ? ((CardView)choices.get(0)).getController() : reveal.getOwner(), true);
        var selected = new ArrayList<Object>();
        for (var index : await(next).getAsJsonArray("choices")) selected.add(choices.get(index.getAsInt()));
        return selected;
    }

    private void libraryPrompt(Pending next, List<?> choices, List<?> revealed, PlayerView owner, boolean choosing) {
        String library = owner.equals(viewer) ? "your library" : owner.getName() + "’s library";
        next.prompt.put("context", "librarySearch");
        next.prompt.put("title", (choosing ? "Search " : "Look at ") + library);
        var permitted = new LinkedHashSet<Object>(revealed);
        permitted.addAll(choices);
        var cards = new ArrayList<Map<String, Object>>();
        for (Object item : permitted) {
            if (!(item instanceof CardView card)) continue;
            var details = choiceCard(card, viewer);
            int index = choices.indexOf(card);
            cards.add(map("index", index < 0 ? null : index, "label", details.get("name"), "card", details));
        }
        // A search grants access to these cards, not a catalog query or a new
        // permission to inspect another hidden zone. Do not expose deck order.
        cards.sort(Comparator.comparing(card -> String.valueOf(card.get("label")), String.CASE_INSENSITIVE_ORDER));
        next.prompt.put("libraryCards", cards);
    }

    static Map<String, Object> choiceCard(CardView card, PlayerView viewer) {
        boolean hidden = card.isFaceDown() || !card.canBeShownTo(viewer);
        var face = card.getCurrentState();
        return map("name", hidden ? "Face-down or hidden card" : face.getName(), "faceDown", hidden,
                "type", hidden ? "" : face.getType().toString(), "manaCost", hidden ? "" : face.getManaCost().toString(),
                "text", hidden ? "" : card.getText(), "power", hidden ? null : face.getPower(), "toughness", hidden ? null : face.getToughness());
    }
    private static Object first(List<?> choices) { return choices.isEmpty() ? null : choices.get(0); }
    private static List<?> combine(Object source, Object destination) {
        var combined = new ArrayList<Object>((Collection<?>)source);
        if (destination != null) combined.addAll((Collection<?>)destination);
        return combined;
    }

    private SpellAbilityView chooseAbility(CardView host, List<SpellAbilityView> abilities) {
        if (abilities.isEmpty()) return null;
        boolean visible = host != null && host.canBeShownTo(viewer) && !host.isFaceDown();
        String name = visible ? host.getCurrentState().getName() : "this card";
        var next = choicePrompt("choice", "Choose how to play " + name + ". Mana payment and any targets come next.",
                abilities, 0, 1, false, ability -> visible ? String.valueOf(ability) : "Card ability");
        next.prompt.put("context", "playAbility");
        next.prompt.put("title", "Play " + name);
        var items = new ArrayList<Object>();
        for (int i = 0; i < abilities.size(); i++) {
            var ability = abilities.get(i);
            String description = visible ? ability.getDescription() : "Card ability";
            String title = description;
            String detail = "";
            if (visible && ability.isSpell() && description.startsWith("Bestow ")) {
                title = "Bestow — cast as an Aura";
                detail = description;
            } else if (visible && ability.isSpell() && description.startsWith(name + " - Creature")) {
                title = "Cast as a creature";
                detail = host.getCurrentState().getManaCost() + " · " + description.substring(name.length() + 3);
            }
            items.add(map("index", i, "label", title, "detail", detail));
        }
        next.prompt.put("choices", items);
        var chosen = await(next).getAsJsonArray("choices");
        return chosen.isEmpty() ? null : abilities.get(chosen.get(0).getAsInt());
    }

    private List<?> choose(String title, int min, int max, List<?> choices, boolean ordered, FSerializableFunction<Object, String> display) {
        if (choices.isEmpty()) return List.of();
        var answer = dialog("choice", title, choices, Math.max(0, min), Math.min(choices.size(), max < 0 ? choices.size() : max), ordered, display);
        var selected = new ArrayList<Object>();
        for (var index : answer.getAsJsonArray("choices")) selected.add(choices.get(index.getAsInt()));
        return selected;
    }

    private JsonObject dialog(String kind, String title, List<?> choices, int min, int max, boolean ordered, FSerializableFunction<Object, String> display) {
        return await(choicePrompt(kind, title, choices, min, max, ordered, display));
    }

    private Pending choicePrompt(String kind, String title, List<?> choices, int min, int max, boolean ordered, FSerializableFunction<Object, String> display) {
        Pending next = new Pending(kind, null);
        next.size = choices.size(); next.min = min; next.max = max;
        var items = new ArrayList<Object>();
        for (int i = 0; i < choices.size(); i++) {
            Object choice = choices.get(i);
            String label = display == null ? label(choice) : display.apply(choice);
            items.add(map("index", i, "label", label));
        }
        next.prompt = map("id", next.id, "kind", kind, "message", title, "choices", items, "min", min, "max", max, "ordered", ordered);
        return next;
    }

    private List<?> reorderCards(Object[] args) {
        var cards = new ArrayList<CardView>();
        ((Iterable<CardView>)args[1]).forEach(cards::add);
        var movable = new HashSet<CardView>();
        ((Iterable<CardView>)args[2]).forEach(movable::add);
        if (cards.isEmpty()) return cards;
        var next = choicePrompt("choice", (String)args[0], cards, cards.size(), cards.size(), true, null);
        next.validateOrder = order -> {
            var fixed = cards.stream().filter(card -> !movable.contains(card)).toList();
            var arranged = order.stream().map(cards::get).toList();
            if (!arranged.stream().filter(card -> !movable.contains(card)).toList().equals(fixed)) throw new IllegalArgumentException("Keep the hidden or fixed cards in their original order");
            if ((boolean)args[5]) return;
            int firstFixed = fixed.isEmpty() ? cards.size() : arranged.indexOf(fixed.get(0));
            int lastFixed = fixed.isEmpty() ? -1 : arranged.indexOf(fixed.get(fixed.size() - 1));
            for (CardView card : movable) {
                int index = arranged.indexOf(card);
                if (index < 0 || (boolean)args[3] && index < firstFixed || (boolean)args[4] && index > lastFixed) continue;
                long before = cards.subList(0, cards.indexOf(card)).stream().filter(fixed::contains).count();
                long after = arranged.subList(0, index).stream().filter(fixed::contains).count();
                if (before != after) throw new IllegalArgumentException("Move selected cards only to the allowed top or bottom of the library");
            }
        };
        var result = new ArrayList<CardView>();
        for (var index : await(next).getAsJsonArray("choices")) result.add(cards.get(index.getAsInt()));
        return result;
    }

    private String label(Object item) {
        if (item instanceof CardView card) return card.canBeShownTo(viewer) && !card.isFaceDown() ? card.getCurrentState().getName() : "Face-down or hidden card";
        if (item instanceof PlayerView player) return player.equals(viewer) ? "You" : player.getName();
        if (item instanceof PaperCard card) return card.getName();
        return String.valueOf(item);
    }

    private JsonObject await(Pending next) {
        synchronized (gate) {
            if (closed) throw new CancellationException("Match closed");
            pending = next;
            publish(next);
        }
        try { return next.response.get(); }
        catch (InterruptedException e) { Thread.currentThread().interrupt(); throw new CancellationException("Match interrupted"); }
        catch (ExecutionException e) { throw new CancellationException(e.getCause().getMessage()); }
    }

    private int number(String title, int min, int max) {
        Pending next = new Pending("number", null);
        next.min = min; next.max = max;
        next.prompt = map("id", next.id, "kind", "number", "message", title, "min", min, "max", max);
        return await(next).get("value").getAsInt();
    }

    private String inputText(Object[] args) {
        if (args[4] != null) return String.valueOf(first(choose(args[1] + "\n" + args[0], 1, 1, (List<?>)args[4], false, null)));
        Pending next = new Pending("text", null);
        next.numeric = (boolean)args[5];
        next.prompt = map("id", next.id, "kind", "text", "message", args[0], "initial", args[3], "numeric", args[5]);
        return await(next).get("value").getAsString();
    }

    private Map<CardView, Integer> combatDamage(Object[] a) {
        var blockers = (List<CardView>)a[1];
        var choices = new ArrayList<Object>(blockers);
        var attacker = ((CardView)a[0]).getCurrentState();
        boolean canHitDefender = a[3] != null && (attacker.hasTrample() || attacker.hasDivideDamage() && (boolean)a[4]);
        if (canHitDefender) choices.add(a[3]);
        Pending next = allocation("Assign " + a[2] + " combat damage from " + label(a[0]), choices, (int)a[2], false);
        next.defenderIndex = !canHitDefender || attacker.hasDivideDamage() && (boolean)a[4] ? -1 : choices.size() - 1;
        boolean deathtouch = attacker.hasDeathtouch();
        next.lethal = blockers.stream().map(card -> Math.max(0, deathtouch ? Math.min(1, card.getLethalDamage()) : card.getLethalDamage())).toList();
        next.maySkip = (boolean)a[5];
        next.prompt.put("maySkip", next.maySkip);
        var response = await(next);
        if (string(response, "action").equals("skip")) return null;
        var amounts = response.getAsJsonArray("values");
        var result = new LinkedHashMap<CardView, Integer>();
        for (int i = 0; i < amounts.size(); i++) if (amounts.get(i).getAsInt() > 0) result.put(i < blockers.size() ? blockers.get(i) : null, amounts.get(i).getAsInt());
        return result;
    }

    private Map<Object, Integer> genericAmount(Object[] a) {
        var choices = new ArrayList<>(((Map<?, ?>)a[1]).keySet());
        Pending next = allocation("Assign " + a[2] + " " + a[4], choices, (int)a[2], (boolean)a[3]);
        next.limits = choices.stream().map(choice -> (Integer)((Map<?, ?>)a[1]).get(choice)).toList();
        next.prompt.put("limits", next.limits);
        var amounts = await(next).getAsJsonArray("values");
        var result = new LinkedHashMap<Object, Integer>();
        for (int i = 0; i < amounts.size(); i++) result.put(choices.get(i), amounts.get(i).getAsInt());
        return result;
    }

    private Pending allocation(String title, List<?> choices, int amount, boolean atLeastOne) {
        Pending next = new Pending("allocate", null);
        next.size = choices.size(); next.amount = amount; next.atLeastOne = atLeastOne;
        var items = new ArrayList<Object>();
        for (int i = 0; i < choices.size(); i++) items.add(map("index", i, "label", label(choices.get(i))));
        next.prompt = map("id", next.id, "kind", "allocate", "message", title, "choices", items, "amount", amount, "atLeastOne", atLeastOne);
        return next;
    }

    private static void validateDialog(Pending next, JsonObject request) {
        switch (next.kind) {
            case "choice" -> {
                var choices = request.getAsJsonArray("choices");
                if (choices == null || choices.size() < next.min || choices.size() > next.max) throw new IllegalArgumentException("Choose the required number of items");
                Set<Integer> selected = new LinkedHashSet<>();
                for (var item : choices) {
                    int index = exactInt(item.getAsString());
                    if (index < 0 || index >= next.size || !selected.add(index)) throw new IllegalArgumentException("Invalid or duplicate choice");
                }
                if (next.validateOrder != null) next.validateOrder.accept(new ArrayList<>(selected));
            }
            case "number" -> { int value = exactInt(string(request, "value")); if (value < next.min || value > next.max) throw new IllegalArgumentException("Number is out of range"); }
            case "text" -> {
                String value = string(request, "value");
                if (value.length() > 500) throw new IllegalArgumentException("Input is too long");
                if (next.numeric) exactInt(value);
            }
            case "allocate" -> {
                if (next.maySkip && string(request, "action").equals("skip")) return;
                var values = request.getAsJsonArray("values");
                if (values == null || values.size() != next.size) throw new IllegalArgumentException("Assign every amount");
                long total = 0;
                for (var value : values) { int amount = exactInt(value.getAsString()); if (amount < (next.atLeastOne ? 1 : 0) || amount > next.amount) throw new IllegalArgumentException("Invalid amount"); total += amount; }
                if (total != next.amount) throw new IllegalArgumentException("The assigned amounts must total " + next.amount);
                for (int i = 0; i < next.limits.size(); i++) if (values.get(i).getAsInt() > next.limits.get(i)) throw new IllegalArgumentException("An amount exceeds its target's limit");
                if (next.defenderIndex >= 0 && values.get(next.defenderIndex).getAsInt() > 0) {
                    for (int i = 0; i < next.lethal.size(); i++) if (values.get(i).getAsInt() < next.lethal.get(i)) throw new IllegalArgumentException("Assign lethal damage to blockers before the defender");
                }
            }
            case "reveal" -> { }
            default -> throw new IllegalArgumentException("Unknown prompt");
        }
    }

    private static int exactInt(String value) { if (!value.matches("-?\\d{1,10}")) throw new IllegalArgumentException("Expected an integer"); return Integer.parseInt(value); }
    private static String string(JsonObject json, String key) { return json.has(key) ? json.get(key).getAsString() : ""; }
    private static LinkedHashMap<String, Object> map(Object... fields) {
        var result = new LinkedHashMap<String, Object>();
        for (int i = 0; i < fields.length; i += 2) result.put((String)fields[i], fields[i + 1]);
        return result;
    }
}
