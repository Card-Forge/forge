package forge.ai.ability;

import org.testng.annotations.Test;

import forge.ai.AITest;
import forge.ai.SpellApiToAi;
import forge.game.Game;
import forge.game.ability.ApiType;
import forge.game.card.Card;
import forge.game.card.CounterEnumType;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;

/**
 * Making everything unblockable is only worth the cost when the swing it enables is lethal.
 * Exercised with Venser's -1, which never fired at all before EffectAi could ask that.
 */
public class VenserAiTest extends AITest {

    @Test
    public void goesUnblockableOnlyForLethal() {
        assertTrue("18 power against 18 life should swing for the win", wouldGoUnblockable(18, null));
        assertFalse("18 power against 19 life is not worth a loyalty point", wouldGoUnblockable(19, null));
        assertFalse("a swing the opponent can Fog away is not lethal",
                wouldGoUnblockable(18, "Kami of False Hope"));
    }

    private boolean wouldGoUnblockable(int oppLife, String oppExtra) {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);

        Card venser = addCard("Venser, the Sojourner", ai);
        venser.setCounters(CounterEnumType.LOYALTY, 5);
        addCards("Colossal Dreadmaw", 3, ai); // 18 power
        addCards("Colossal Dreadmaw", 2, opp); // blockers, which stop mattering
        if (oppExtra != null) {
            addCard(oppExtra, opp);
        }
        opp.setLife(oppLife, null);

        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        game.getAction().checkStateEffects(true);
        for (Card c : ai.getCardsIn(ZoneType.Battlefield)) {
            c.setSickness(false);
        }

        for (SpellAbility sa : venser.getSpellAbilities()) {
            if (sa.getApi() == ApiType.Effect && !sa.hasParam("Ultimate")) {
                sa.setActivatingPlayer(ai);
                return SpellApiToAi.Converter.get(sa).canPlayWithSubs(ai, sa).willingToPlay();
            }
        }
        throw new AssertionError("Venser has no -1 ability");
    }
}
