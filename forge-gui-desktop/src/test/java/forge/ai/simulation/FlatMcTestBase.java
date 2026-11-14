package forge.ai.simulation;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import org.testng.SkipException;

import forge.ai.AiController;
import forge.ai.PlayerControllerAi;
import forge.game.Game;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.cost.CostDiscard;
import forge.game.phase.PhaseHandler;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/**
 * Shared set-up for the tests of PR #12508 (FlatMC Enhancements).
 */
public abstract class FlatMcTestBase extends SimulationTest {
    protected Game game;
    /** The player with a PlayerControllerAi: the one under test. */
    protected Player ai;
    protected Player opp;

    protected void setUpGame() {
        game = initAndCreateGame();
        final Player a = game.getPlayers().get(1);
        final Player b = game.getPlayers().get(0);
        if (a.getController() instanceof PlayerControllerAi) { ai = a; opp = b; } else { ai = b; opp = a; }
        brain().setUseSimulation(null); // SimulationTest enables the full simulation; a Flat MC player runs without it
        setTurn(5);
    }

    // --------------------------------------------------------------------------------------------- cards

    protected Card put(final String name, final Player p, final ZoneType zone) {
        return addCardToZone(name, p, zone);
    }

    protected List<Card> put(final String name, final int count, final Player p, final ZoneType zone) {
        final List<Card> out = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            out.add(addCardToZone(name, p, zone));
        }
        return out;
    }

    protected static CardCollection collection(final Card... cards) {
        final CardCollection cc = new CardCollection();
        for (final Card c : cards) {
            cc.add(c);
        }
        return cc;
    }

    // --------------------------------------------------------------------------------------------- phases

    /** Puts the game into this phase of the turn of {@code turnOf}. */
    protected void at(final PhaseType phase, final Player turnOf) {
        game.getPhaseHandler().devModeSet(phase, turnOf);
        // devModeSet does not create the Combat that a real game has from COMBAT_BEGIN to COMBAT_END
        if (!phase.isBefore(PhaseType.COMBAT_BEGIN) && !phase.isAfter(PhaseType.COMBAT_END)) {
            try {
                final Field f = PhaseHandler.class.getDeclaredField("combat");
                f.setAccessible(true);
                if (f.get(game.getPhaseHandler()) == null) {
                    f.set(game.getPhaseHandler(), new forge.game.combat.Combat(turnOf));
                }
            } catch (final ReflectiveOperationException e) {
                throw new SkipException("cannot create a combat in this tree: " + e);
            }
        }
    }

    /** The turn counter is private in PhaseHandler; the circuit breaker works on it, so tests need to move it. */
    protected void setTurn(final int turn) {
        try {
            final Field f = PhaseHandler.class.getDeclaredField("turn");
            f.setAccessible(true);
            f.setInt(game.getPhaseHandler(), turn);
        } catch (final ReflectiveOperationException e) {
            throw new SkipException("cannot move the turn counter in this tree: " + e);
        }
    }

    // --------------------------------------------------------------------------------------------- the AI

    protected AiController brain() {
        return ((PlayerControllerAi) ai.getController()).getAi();
    }

    /** Whether this is an activated ability whose cost discards a card (Wild Mongrel, Aquamoeba, Psychatog). */
    protected static boolean isDiscardAbility(final SpellAbility sa) {
        return sa != null && sa.isActivatedAbility() && sa.getPayCosts() != null
                && sa.getPayCosts().hasSpecificCostType(CostDiscard.class);
    }

    /** The "Discard a card: ..." ability of a card, with the activating player set. */
    protected SpellAbility discardAbilityOf(final Card c) {
        for (final SpellAbility sa : c.getSpellAbilities()) {
            if (isDiscardAbility(sa)) {
                sa.setActivatingPlayer(c.getController());
                return sa;
            }
        }
        throw new AssertionError(c + " has no discard ability");
    }

    // --------------------------------------------------------------------------------------------- decks

    /**
     * A library that makes the deck scan report the MADNESS combo (3+ madness cards, 3+ outlets, 12+ spells).
     * Add it BEFORE anything asks the AI for its deck plan: the plan is read once and then fixed for the game.
     */
    protected void addMadnessDeck() {
        put("Basking Rootwalla", 2, ai, ZoneType.Library);
        put("Arrogant Wurm", 1, ai, ZoneType.Library);
        put("Wild Mongrel", 2, ai, ZoneType.Library);
        put("Aquamoeba", 1, ai, ZoneType.Library);
        put("Grizzly Bears", 8, ai, ZoneType.Library);
    }

    /** A library that makes the deck scan report the ATOG combo (3+ atogs, 10+ instants/sorceries or fuel). */
    protected void addAtogDeck() {
        put("Psychatog", 2, ai, ZoneType.Library);
        put("Wild Mongrel", 1, ai, ZoneType.Library);
        put("Giant Growth", 10, ai, ZoneType.Library);
    }

    /** A deck without any combo: Grizzly Bears and Giant Growth. */
    protected void addPlainDeck() {
        put("Grizzly Bears", 8, ai, ZoneType.Library);
        put("Giant Growth", 8, ai, ZoneType.Library);
    }
}