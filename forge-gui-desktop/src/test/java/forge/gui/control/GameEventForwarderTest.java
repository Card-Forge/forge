package forge.gui.control;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import com.google.common.eventbus.Subscribe;

import forge.ai.AITest;
import forge.ai.ComputerUtil;
import forge.game.Game;
import forge.game.card.Card;
import forge.game.event.GameEvent;
import forge.game.event.GameEventAttackersDeclared;
import forge.game.event.GameEventBlockersDeclared;
import forge.game.event.GameEventCardChangeZone;
import forge.game.event.GameEventCardDamaged;
import forge.game.event.GameEventLandPlayed;
import forge.game.event.GameEventPlayerDamaged;
import forge.game.event.GameEventSpellAbilityCast;
import forge.game.event.GameEventSpellResolved;
import forge.game.event.GameEventTurnBegan;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;
import forge.gui.interfaces.IGuiGame;

/** What a batch sent to a remote client holds, cut by the real forwarder from a real game. */
public class GameEventForwarderTest extends AITest {
    /** A batch's events, and where every card was when it was cut, which is the state sent with it. */
    private record Batch(List<GameEvent> events, Map<Integer, ZoneType> zones) {
        boolean has(final Class<? extends GameEvent> kind) {
            return events.stream().anyMatch(kind::isInstance);
        }

        boolean onBattlefield(final Card card) {
            return zones.get(card.getId()) == ZoneType.Battlefield;
        }

        boolean moves(final int card) {
            return events.stream().anyMatch(e -> e instanceof GameEventCardChangeZone move && move.card().getId() == card);
        }
    }

    /** The forwarder on a game, the batches it has cut, and every event the game fired beside it. */
    private static final class Sent {
        final List<Batch> batches = new ArrayList<>();
        final List<GameEvent> fired = new ArrayList<>();
        final Map<Integer, ZoneType> zonesAtStart;
        final GameEventForwarder forwarder;

        @SuppressWarnings("unchecked")
        Sent(final Game game) {
            zonesAtStart = zonesOf(game);
            final IGuiGame gui = (IGuiGame) Proxy.newProxyInstance(IGuiGame.class.getClassLoader(), new Class<?>[] { IGuiGame.class }, (proxy, method, args) -> {
                if ("handleGameEvents".equals(method.getName())) {
                    batches.add(new Batch(List.copyOf((List<GameEvent>) args[0]), zonesOf(game)));
                }
                return null;
            });
            forwarder = new GameEventForwarder(gui);
            game.subscribeToEvents(forwarder);
            game.subscribeToEvents(this);
        }

        @Subscribe
        public void fired(final GameEvent event) {
            fired.add(event);
        }

        int indexOf(final Class<? extends GameEvent> kind) {
            for (int i = 0; i < batches.size(); i++) {
                if (batches.get(i).has(kind)) {
                    return i;
                }
            }
            return -1;
        }
    }

    private static Map<Integer, ZoneType> zonesOf(final Game game) {
        final Map<Integer, ZoneType> zones = new HashMap<>();
        for (final Card card : game.getCardsInGame()) {
            if (card.getZone() != null) {
                zones.put(card.getId(), card.getZone().getZoneType());
            }
        }
        return zones;
    }

    // Fails if a spell is cast and resolves in one batch, so a client never sees it on the stack
    @Test
    public void aSpellIsSentBeforeItResolves() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        addCards("Forest", 2, ai);
        addCardToZone("Grizzly Bears", ai, ZoneType.Hand);
        moveToMain2(game, ai);
        final Sent sent = new Sent(game);

        playUntilNextTurn(game);

        final int cast = sent.indexOf(GameEventSpellAbilityCast.class);
        final int resolved = sent.indexOf(GameEventSpellResolved.class);
        AssertJUnit.assertTrue("the spell was cast and resolved", cast >= 0 && resolved >= 0);
        AssertJUnit.assertTrue("the cast is in an earlier batch than the resolution", cast < resolved);
    }

    // Fails if a land waits for the next thing its player does, so others see it late
    @Test
    public void aLandIsSentBeforeItsPlayersNextAction() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        addCard("Forest", ai);
        addCardToZone("Forest", ai, ZoneType.Hand);
        addCardToZone("Grizzly Bears", ai, ZoneType.Hand);
        moveToMain2(game, ai);
        final Sent sent = new Sent(game);

        playUntilNextTurn(game);

        final int land = sent.indexOf(GameEventLandPlayed.class);
        final int cast = sent.indexOf(GameEventSpellAbilityCast.class);
        AssertJUnit.assertTrue("the land was played and the spell cast in the same phase", land >= 0 && cast >= 0);
        AssertJUnit.assertTrue("the land is in an earlier batch than the cast", land < cast);
    }

    // Fails if a death is sent apart from the damage that caused it, or if a card leaves the battlefield in a batch that does not say so
    @Test
    public void aCreatureLeavesWithTheDamageThatKilledIt() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        final Player opponent = game.getPlayers().get(0);
        addCards("Mountain", 2, ai);
        final Card bears = addCard("Grizzly Bears", opponent);
        final Card sweep = addCardToZone("Pyroclasm", ai, ZoneType.Hand);
        game.getAction().checkStateEffects(true);
        final Sent sent = new Sent(game);

        final SpellAbility cast = sweep.getSpellAbilities().get(0);
        cast.setActivatingPlayer(ai);
        AssertJUnit.assertTrue(ComputerUtil.playStack(cast, ai, game));
        playUntilStackClear(game);
        game.getPhaseHandler().mainLoopStep();

        final int damaged = sent.indexOf(GameEventCardDamaged.class);
        AssertJUnit.assertTrue("the creature was damaged", damaged >= 0);
        final Batch batch = sent.batches.get(damaged);
        AssertJUnit.assertTrue("its move to the graveyard is in the batch with the damage", batch.moves(bears.getId()));
        AssertJUnit.assertFalse("it is off the battlefield in the state sent with that batch", batch.onBattlefield(bears));
        for (int i = 0; i < damaged; i++) {
            AssertJUnit.assertTrue("it is still on the battlefield in every earlier batch", sent.batches.get(i).onBattlefield(bears));
        }
    }

    // Fails if an attack and its damage arrive together, so a client draws the attack already dealt
    @Test
    public void anAttackIsSentBeforeItsDamage() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        addCard("Hill Giant", ai).setSickness(false);
        game.getAction().checkStateEffects(true);
        final Sent sent = new Sent(game);

        playUntilNextTurn(game);

        final int attack = sent.indexOf(GameEventAttackersDeclared.class);
        final int damage = sent.indexOf(GameEventPlayerDamaged.class);
        AssertJUnit.assertTrue("the creature attacked and dealt damage", attack >= 0 && damage >= 0);
        AssertJUnit.assertTrue("the attack is in an earlier batch than its damage", attack < damage);
    }

    // Fails if a resolution arrives with the turn after it, so a client shows the new turn with the spell still on the stack
    @Test
    public void aResolutionIsSentBeforeTheNextTurn() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        addCards("Forest", 2, ai);
        addCardToZone("Grizzly Bears", ai, ZoneType.Hand);
        moveToMain2(game, ai);
        final Sent sent = new Sent(game);

        playUntilNextTurn(game);
        // The new turn is sent once somebody receives priority in it, which its untap step gives nobody
        for (int i = 0; i < 10 && sent.indexOf(GameEventTurnBegan.class) < 0; i++) {
            game.getPhaseHandler().mainLoopStep();
        }

        final int resolved = sent.indexOf(GameEventSpellResolved.class);
        final int turn = sent.indexOf(GameEventTurnBegan.class);
        AssertJUnit.assertTrue("the spell resolved and the turn changed", resolved >= 0 && turn >= 0);
        AssertJUnit.assertTrue("the resolution is in an earlier batch than the new turn", resolved < turn);
    }

    // Fails if a discard to hand size arrives with the turn after it, so a client shows the card leaving under the new turn
    @Test
    public void aDiscardToHandSizeIsSentBeforeTheNextTurn() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        // More than a hand holds, and nothing here can be cast, so the turn ends with a discard
        final List<Card> hand = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            hand.add(addCardToZone("Craw Wurm", ai, ZoneType.Hand));
        }
        moveToMain2(game, ai);
        final Sent sent = new Sent(game);
        playUntilNextTurn(game);
        // The new turn is sent once somebody receives priority in it, which its untap step gives nobody
        for (int i = 0; i < 10 && sent.indexOf(GameEventTurnBegan.class) < 0; i++) {
            game.getPhaseHandler().mainLoopStep();
        }
        int discard = -1;
        for (int i = 0; i < sent.batches.size() && discard < 0; i++) {
            final Batch batch = sent.batches.get(i);
            if (hand.stream().anyMatch(card -> batch.moves(card.getId()))) {
                discard = i;
            }
        }
        final int turn = sent.indexOf(GameEventTurnBegan.class);
        AssertJUnit.assertTrue("a card was discarded and the turn changed", discard >= 0 && turn >= 0);
        AssertJUnit.assertTrue("the discard is in an earlier batch than the new turn", discard < turn);
    }

    /** Two turns in which lands are played, spells cast, tokens made, cards drawn and a hand discarded down to size. */
    private Sent twoBusyTurns() {
        final Game game = initAndCreateGame();
        for (final Player player : game.getPlayers()) {
            addCards("Mountain", 3, player);
            addCardToZone("Mountain", player, ZoneType.Hand);
            addCardToZone("Dragon Fodder", player, ZoneType.Hand);
            addCardToZone("Raging Goblin", player, ZoneType.Hand);
            // More than a hand holds, and nothing here can be cast, so the turn ends with a discard
            for (int i = 0; i < 8; i++) {
                addCardToZone("Craw Wurm", player, ZoneType.Hand);
            }
            fillLibrary(player, 10);
        }
        game.getAction().checkStateEffects(true);
        final Sent sent = new Sent(game);
        playUntilNextTurn(game);
        playUntilNextTurn(game);
        sent.forwarder.flush();
        return sent;
    }

    // Fails if an event is dropped, sent twice or sent out of order, so a sound, a log line or an animation is lost or misplaced
    @Test
    public void everyEventIsSentOnceAndInOrder() {
        final Sent sent = twoBusyTurns();

        final List<GameEvent> received = new ArrayList<>();
        sent.batches.forEach(batch -> received.addAll(batch.events()));
        AssertJUnit.assertTrue("the turns were busy", sent.fired.size() > 40);
        AssertJUnit.assertEquals("as many events were sent as were fired", sent.fired.size(), received.size());
        for (int i = 0; i < received.size(); i++) {
            AssertJUnit.assertSame("event " + i + " is the one fired in that place", sent.fired.get(i), received.get(i));
        }
    }

    // Fails if a card is in a new zone in a batch that holds no move for it, so a client has nothing to show it move by
    @Test
    public void aCardChangesZoneOnlyInABatchThatSaysSo() {
        final Sent sent = twoBusyTurns();

        final Set<ZoneType> arrivedIn = new HashSet<>();
        Map<Integer, ZoneType> before = sent.zonesAtStart;
        for (int i = 0; i < sent.batches.size(); i++) {
            final Batch batch = sent.batches.get(i);
            final Set<Integer> cards = new HashSet<>(before.keySet());
            cards.addAll(batch.zones().keySet());
            for (final Integer card : cards) {
                if (before.get(card) != batch.zones().get(card)) {
                    arrivedIn.add(batch.zones().get(card));
                    AssertJUnit.assertTrue("card " + card + " went from " + before.get(card) + " to " + batch.zones().get(card) + " in batch " + i + " with its move",
                            batch.moves(card));
                }
            }
            before = batch.zones();
        }
        AssertJUnit.assertTrue("cards were played, cast, drawn and discarded: " + arrivedIn,
                arrivedIn.containsAll(Set.of(ZoneType.Battlefield, ZoneType.Stack, ZoneType.Hand, ZoneType.Graveyard)));
    }

    // Fails if two things resolve in one batch, or one resolves in the batch it went on the stack in, so a client skips a step of the stack
    @Test
    public void theStackMovesOneStepAtATime() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        addCards("Forest", 2, ai);
        // Its trigger goes on the stack as it enters, which is a second thing to see resolve
        addCardToZone("Elvish Visionary", ai, ZoneType.Hand);
        fillLibrary(ai, 5);
        moveToMain2(game, ai);
        final Sent sent = new Sent(game);

        playUntilNextTurn(game);

        int casts = 0, resolutions = 0;
        for (final Batch batch : sent.batches) {
            boolean cast = false;
            int resolved = 0;
            for (final GameEvent event : batch.events()) {
                if (event instanceof GameEventSpellAbilityCast) {
                    cast = true;
                    casts++;
                } else if (event instanceof GameEventSpellResolved) {
                    AssertJUnit.assertFalse("nothing resolves after something went on the stack in the same batch", cast);
                    resolved++;
                    resolutions++;
                }
            }
            AssertJUnit.assertTrue("at most one thing resolves in a batch", resolved <= 1);
        }
        AssertJUnit.assertEquals("the spell and its trigger both went on the stack", 2, casts);
        AssertJUnit.assertEquals("and both resolved", 2, resolutions);
    }

    // Fails if blocks arrive with the damage that follows them, or a blocker's death arrives apart from its damage
    @Test
    public void combatWithABlockArrivesInSteps() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        final Player opponent = game.getPlayers().get(0);
        addCard("Hill Giant", ai).setSickness(false);
        final Card blocker = addCard("Elvish Mystic", opponent);
        // An attack that would kill is one the computer makes, and a chump block is how it survives one
        opponent.setLife(2, null);
        game.getAction().checkStateEffects(true);
        final Sent sent = new Sent(game);

        playUntilNextTurn(game);

        final int blocks = sent.indexOf(GameEventBlockersDeclared.class);
        final int damage = sent.indexOf(GameEventCardDamaged.class);
        AssertJUnit.assertTrue("the creature was blocked and dealt damage", blocks >= 0 && damage >= 0);
        AssertJUnit.assertTrue("the blocks are in an earlier batch than the damage", blocks < damage);
        AssertJUnit.assertTrue("the blocker is still on the battlefield when its block is sent", sent.batches.get(blocks).onBattlefield(blocker));
        AssertJUnit.assertTrue("its death is in the batch with its damage", sent.batches.get(damage).moves(blocker.getId()));
        AssertJUnit.assertFalse("and it is off the battlefield in that batch's state", sent.batches.get(damage).onBattlefield(blocker));
    }

    // Fails if a turn's draw is sent before the turn itself, so a client shows the card drawn in the turn before
    @Test
    public void aTurnsDrawIsNotSentBeforeTheTurn() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        for (final Player player : game.getPlayers()) {
            fillLibrary(player, 5);
        }
        moveToMain2(game, ai);
        final Sent sent = new Sent(game);

        playUntilNextTurn(game);
        for (int i = 0; i < 10 && draw(sent) < 0; i++) {
            game.getPhaseHandler().mainLoopStep();
        }

        final int turn = sent.indexOf(GameEventTurnBegan.class);
        final int draw = draw(sent);
        AssertJUnit.assertTrue("the turn changed and its player drew", turn >= 0 && draw >= 0);
        AssertJUnit.assertTrue("the draw is not in an earlier batch than the turn", turn <= draw);
    }

    private static int draw(final Sent sent) {
        for (int i = 0; i < sent.batches.size(); i++) {
            if (sent.batches.get(i).events().stream().anyMatch(e -> e instanceof GameEventCardChangeZone move
                    && move.from() != null && move.from().zoneType() == ZoneType.Library && move.to() != null && move.to().zoneType() == ZoneType.Hand)) {
                return i;
            }
        }
        return -1;
    }
}
