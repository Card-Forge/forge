package forge.ai.simulation;

import org.testng.AssertJUnit;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import forge.ai.ability.SacrificeWorth;
import forge.game.ability.AbilityFactory;
import forge.game.card.Card;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/** "You may sacrifice a creature. If you do, ...": the reward is weighed against the permanent that would go. */
public class SacrificeWorthTest extends FlatMcTestBase {
    @BeforeMethod
    public void setUp() {
        setUpGame();
    }

    /** The sacrifice of a creature other than the host, followed by the steps of {@code subs} (SVars DB1, DB2 ...). */
    private SpellAbility maySacrifice(final Card host, final String... subs) {
        final StringBuilder script = new StringBuilder("DB$ Sacrifice | Optional$ True | SacValid$ Creature.Other | Amount$ 1 | RememberSacrificed$ True");
        for (int i = 0; i < subs.length; i++) {
            host.setSVar("DB" + (i + 1), subs[i] + (i + 1 < subs.length ? " | SubAbility$ DB" + (i + 2) : ""));
        }
        if (subs.length > 0) {
            script.append(" | SubAbility$ DB1");
        }
        final SpellAbility sa = AbilityFactory.getAbility(script.toString(), host);
        sa.setActivatingPlayer(ai);
        return sa;
    }

    @Test
    public void nothingToSacrificeIsNotSacrificed() {
        final Card host = put("Crovax the Cursed", ai, ZoneType.Battlefield);
        AssertJUnit.assertFalse(SacrificeWorth.confirm(ai, maySacrifice(host, "DB$ Draw | NumCards$ 3")));
    }

    @Test
    public void aGoodCreatureIsNotSacrificedForOneCard() {
        final Card host = put("Crovax the Cursed", ai, ZoneType.Battlefield);
        put("Serra Angel", ai, ZoneType.Battlefield);
        AssertJUnit.assertFalse(SacrificeWorth.confirm(ai, maySacrifice(host, "DB$ Draw | NumCards$ 1")));
    }

    @Test
    public void aSmallCreatureIsSacrificedForTwoCards() {
        final Card host = put("Crovax the Cursed", ai, ZoneType.Battlefield);
        put("Grizzly Bears", ai, ZoneType.Battlefield);
        AssertJUnit.assertTrue(SacrificeWorth.confirm(ai, maySacrifice(host, "DB$ Draw | NumCards$ 2")));
    }

    @Test
    public void notLosingACounterIsPartOfTheReward() {
        // Crovax: +1 counter if you do, -1 counter if you do not: the counter lost counts for the sacrifice
        final Card host = put("Crovax the Cursed", ai, ZoneType.Battlefield);
        put("Grizzly Bears", ai, ZoneType.Battlefield);
        final String plus = "DB$ PutCounter | CounterType$ P1P1 | CounterNum$ 2 | Defined$ Self";
        final String minus = "DB$ RemoveCounter | CounterType$ P1P1 | CounterNum$ 2 | Defined$ Self | ConditionDefined$ Remembered | ConditionPresent$ Creature | ConditionCompare$ EQ0";
        AssertJUnit.assertTrue("two counters up and two not lost", SacrificeWorth.confirm(ai, maySacrifice(host, plus, minus)));
        AssertJUnit.assertTrue("the reward counts both steps", SacrificeWorth.reward(maySacrifice(host, plus, minus))
                > SacrificeWorth.reward(maySacrifice(host, plus)));
    }

    @Test
    public void aSacrificeThatKeepsTheAiAliveIsTaken() {
        // Pillar Tombs of Aku: sacrifice a creature or lose 5 life
        final Card host = put("Crovax the Cursed", ai, ZoneType.Battlefield);
        put("Serra Angel", ai, ZoneType.Battlefield);
        final String lose = "DB$ LoseLife | Defined$ TriggeredPlayer | LifeAmount$ 5 | ConditionDefined$ Remembered | ConditionPresent$ Card | ConditionCompare$ EQ0";
        ai.setLife(20, null);
        AssertJUnit.assertFalse("losing 5 of 20 life is cheaper than the Angel", SacrificeWorth.confirm(ai, maySacrifice(host, lose)));
        ai.setLife(4, null);
        AssertJUnit.assertTrue("losing 5 of 4 life is not", SacrificeWorth.confirm(ai, maySacrifice(host, lose)));
    }
}
