package forge.game.spellability;

import org.testng.annotations.Test;

import forge.ai.AITest;
import forge.game.Game;
import forge.game.card.Card;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.replacement.ReplacementEffect;
import forge.game.zone.ZoneType;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;

/**
 * Beam me up casts from the graveyard, but only if a creature you control can be returned - and
 * Marooned is the card that says a permanent can't be the one returned.
 */
public class BeamMeUpTest extends AITest {

    private SpellAbility beamSA(Card c, Player p) {
        for (SpellAbility sa : c.getAllPossibleAbilities(p, true)) {
            if (sa.isBeamMeUp()) {
                return sa;
            }
        }
        return null;
    }

    private boolean canBeamUp(Card c, Player p) {
        return beamSA(c, p) != null;
    }

    private Card setup(Game game, Player p) {
        addCards("Island", 3, p);
        Card open = addCardToZone("Open Communications", p, ZoneType.Graveyard);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, p);
        return open;
    }

    @Test
    public void beamsUpFromTheGraveyardForItsOwnCost() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);
        Card open = setup(game, p);
        addCard("Grizzly Bears", p);
        game.getAction().checkStateEffects(true);

        SpellAbility beam = beamSA(open, p);
        assertNotNull("a creature is available to return, so the graveyard cast is offered", beam);
        assertEquals("cast for {2}{U}, not the card's own {U}", 3,
                beam.getPayCosts().getTotalMana().getCMC());

        // "then exile this spell" rides on the same replacement Flashback uses, so check it is
        // there; firing it needs a real cast, which this harness cannot drive from the graveyard
        boolean exileRep = false;
        for (ReplacementEffect re : open.getReplacementEffects()) {
            if (re.toString().contains("Beam me up")) {
                exileRep = true;
            }
        }
        assertTrue("the keyword should add the exile replacement", exileRep);
    }

    /** The return is an additional cost, not an option, so an empty board stops the cast. */
    @Test
    public void doesNotBeamUpWithNoCreature() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);
        Card open = setup(game, p);
        game.getAction().checkStateEffects(true);

        assertFalse("nothing to return, so there is nothing to pay the cost with",
                canBeamUp(open, p));
    }

    @Test
    public void aMaroonedCreatureCannotBeBeamedUp() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);
        Card open = setup(game, p);
        Card bears = addCard("Grizzly Bears", p);
        Card marooned = addCard("Marooned", p);
        marooned.attachToEntity(bears, null);
        game.getAction().checkStateEffects(true);

        assertTrue("the aura should be attached", bears.isEnchantedBy(marooned));
        assertFalse("the only creature can't be beamed up, so the cost can't be paid",
                canBeamUp(open, p));
    }

    /** Marooned only stops the creature it is on, not every creature. */
    @Test
    public void anotherCreatureStillPaysTheCost() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);
        Card open = setup(game, p);
        Card bears = addCard("Grizzly Bears", p);
        addCard("Runeclaw Bear", p);
        Card marooned = addCard("Marooned", p);
        marooned.attachToEntity(bears, null);
        game.getAction().checkStateEffects(true);

        assertTrue("the unenchanted creature can still be returned", canBeamUp(open, p));
    }
}
