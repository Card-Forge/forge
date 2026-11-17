package forge.gamesimulationtests.priority;

import static org.testng.Assert.assertEquals;

import org.testng.annotations.Test;

import forge.game.card.Card;
import forge.game.spellability.SpellAbility;

/**
 * CR 117.3c: a player who activates an ability while they have priority receives priority afterward, including a mana
 * ability (CR 605.3a). A spell another player casts while that mana ability resolves does not take priority from them.
 */
public class ManaAbilityCastPriorityTest extends PriorityTestBase {

    /**
     * a taps Grove of the Burnwillows for red: each opponent gains 1 life, which b's Lich turns into a draw and Archmage
     * Ascension into a search, during which b casts Panglacial Wurm. a, who activated the mana ability, keeps priority.
     */
    @Test(timeOut = 60000)
    public void castDuringOpponentsManaAbility() {
        setUpGame();
        final Card grove = addCard("Grove of the Burnwillows", a);
        wurmEngine(b);
        SpellAbility red = null;
        for (final SpellAbility sa : grove.getManaAbilities()) {
            if (sa.getSubAbility() != null) {
                red = sa; // "Add {R} or {G}. Each opponent gains 1 life."
            }
        }
        ctrl(a).actions.add(red);

        runUntil(() -> log.stream().anyMatch(p -> WURM.equals(p.stackTop())), "priority with the Wurm on the stack");
        final Priority after = log.stream().filter(p -> WURM.equals(p.stackTop())).findFirst().get();
        assertEquals(after.player(), a, "a activated the mana ability and keeps priority, not the Wurm's caster\n" + log);
    }
}
