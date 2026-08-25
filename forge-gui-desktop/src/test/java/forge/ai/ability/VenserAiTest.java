package forge.ai.ability;

import org.testng.annotations.Test;

import forge.ai.AITest;
import forge.game.Game;
import forge.game.card.Card;
import forge.game.card.CounterEnumType;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.zone.ZoneType;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;

public class VenserAiTest extends AITest {

    @Test
    public void goesUnblockableForALethalSwing() {
        Game game = boardWithVenser(18, null);
        Player opp = game.getPlayers().get(0);
        Player ai = game.getPlayers().get(1);

        gameLoopUntilNextPhase(game);

        assertEquals(4, loyalty(game));
        assertTrue(unblockableEffectIsUp(game));

        // and the loyalty actually buys the game: the swing goes through unblocked
        playUntilPhase(game, PhaseType.MAIN2);
        assertEquals(0, opp.getLife());
        assertTrue("the opponent is dead", opp.hasLost());
        assertEquals("and nothing of ours traded, because nothing could block",
                3, ourDreadmaws(ai));
    }

    @Test
    public void holdsTheAbilityWhenTheSwingIsShort() {
        Game game = boardWithVenser(19, null);

        gameLoopUntilNextPhase(game);

        assertEquals(5, loyalty(game));
    }

    @Test
    public void holdsTheAbilityAgainstAFog() {
        Game game = boardWithVenser(18, "Kami of False Hope");

        gameLoopUntilNextPhase(game);

        assertEquals(5, loyalty(game));
    }

    /**
     * 18 power against an opponent holding two 6/6 blockers, so the swing is lethal only because
     * nothing can block it - a check that consulted blockers would answer no on this board.
     */
    private Game boardWithVenser(int oppLife, String oppExtra) {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);

        addCard("Venser, the Sojourner", ai).setCounters(CounterEnumType.LOYALTY, 5);
        addCards("Colossal Dreadmaw", 3, ai);
        addCards("Colossal Dreadmaw", 2, opp);
        if (oppExtra != null) {
            addCard(oppExtra, opp);
        }
        opp.setLife(oppLife, null);
        for (Player p : game.getPlayers()) {
            fillLibrary(p, 20);
        }

        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        game.getAction().checkStateEffects(true);
        for (Card c : ai.getCardsIn(ZoneType.Battlefield)) {
            c.setSickness(false);
        }
        return game;
    }

    private int ourDreadmaws(Player ai) {
        int n = 0;
        for (Card c : ai.getCardsIn(ZoneType.Battlefield)) {
            if (c.getName().equals("Colossal Dreadmaw")) {
                n++;
            }
        }
        return n;
    }

    /** 5 to start: 4 means the -1 was activated, 7 the +2, and 5 that Venser was left alone. */
    private int loyalty(Game game) {
        return findCardWithName(game, "Venser, the Sojourner").getCounters(CounterEnumType.LOYALTY);
    }

    private boolean unblockableEffectIsUp(Game game) {
        for (Card c : game.getCardsIn(ZoneType.Command)) {
            if (c.getName().endsWith("'s Effect") && c.getName().startsWith("Venser, the Sojourner")) {
                return true;
            }
        }
        return false;
    }
}
