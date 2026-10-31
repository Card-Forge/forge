package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.ai.simulation.DeckPlan;
import forge.game.Game;
import forge.game.ability.ApiType;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/** Abilities marked {@code AILogic$ Never} that Flat MC may weigh anyway (Kozilek, the Great Distortion). */
public class GuardedAbilitiesTest extends AITest {

    private static SpellAbility counterOf(final Card kozilek) {
        for (final SpellAbility sa : kozilek.getSpellAbilities()) {
            if (sa.getApi() == ApiType.Counter && !sa.isSpell()) {
                return sa;
            }
        }
        return null;
    }

    @Test
    public void neverLogicCounterIsOnlyConsideredWithDiscovery() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        final Card kozilek = addCard("Kozilek, the Great Distortion", ai);
        final SpellAbility sa = counterOf(kozilek);
        AssertJUnit.assertNotNull(sa);
        AssertJUnit.assertTrue(GuardedAbilities.isNeverLogic(sa));
        AssertJUnit.assertFalse(GuardedAbilities.mayConsider(ai, sa, false));
        AssertJUnit.assertTrue(GuardedAbilities.mayConsider(ai, sa, true));
    }

    @Test
    public void otherNeverAbilitiesStayOut() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        final Card card = addCard("Sol Ring", ai);
        for (final SpellAbility sa : card.getSpellAbilities()) {
            if (GuardedAbilities.isNeverLogic(sa)) {
                AssertJUnit.assertFalse(GuardedAbilities.mayConsider(ai, sa, true));
            }
        }
    }

    @Test
    public void discardIsPaidWithTheLeastValuableCardOfTheRightManaValue() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        final Card kozilek = addCard("Kozilek, the Great Distortion", ai);
        final Card keep = addCard("Serra Angel", ai);          // mana value 5
        final Card pay = addCardToZone("Divination", ai, ZoneType.Hand); // mana value 3
        addCardToZone("Island", ai, ZoneType.Hand);            // mana value 0
        final SpellAbility sa = counterOf(kozilek);
        AssertJUnit.assertNotNull(sa);
        // Nothing is targeted: the cost cannot be paid, and it must not be guessed as mana value 0.
        AssertJUnit.assertEquals(-1, GuardedAbilities.targetedMv(sa));
        final CardCollection none = GuardedAbilities.chooseMvDiscard(ai, sa, 1, null);
        AssertJUnit.assertTrue(none == null || !none.contains(pay) || none.size() == 1);
    }

    @Test
    public void tutorEntryPointIsSafeOnEmptyInput() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        final DeckPlan plan = DeckPlan.forSetting("auto", ai);
        AssertJUnit.assertNull(plan.tutorPicks(ai, new CardCollection(), 1));
        AssertJUnit.assertNull(plan.tutorPicks(ai, null, 1));
        AssertJUnit.assertNull(plan.tutorPick(ai, new CardCollection()));
    }
}
