package forge.ai.simulation;

import java.util.List;

import org.testng.AssertJUnit;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import forge.ai.PlayerControllerAi;
import forge.ai.ability.MayDecisions;
import forge.game.ability.AbilityFactory;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.player.PlayerActionConfirmMode;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/** The "you may ..." answers of MayDecisions: what the AI says to optional effects that used to get a plain yes. */
public class MayDecisionsTest extends FlatMcTestBase {
    @BeforeMethod
    public void setUp() {
        setUpGame();
    }

    private SpellAbility script(final Card host, final String script) {
        final SpellAbility sa = AbilityFactory.getAbility(script, host);
        sa.setActivatingPlayer(ai);
        return sa;
    }

    private boolean may(final Card host, final String script, final PlayerActionConfirmMode mode) {
        return ((PlayerControllerAi) ai.getController()).getAi().confirmAction(script(host, script), mode, "", null);
    }

    @Test
    public void newTargetsAreForWhatAnOpponentCastNotWhatTheAiCast() {
        final Card bolt = put("Lightning Bolt", ai, ZoneType.Hand);
        final SpellAbility mine = bolt.getFirstSpellAbility();
        mine.setActivatingPlayer(ai);
        final Card theirBolt = put("Lightning Bolt", opp, ZoneType.Hand);
        final SpellAbility theirs = theirBolt.getFirstSpellAbility();
        theirs.setActivatingPlayer(opp);
        AssertJUnit.assertFalse(MayDecisions.redirects(ai, List.of(mine)));
        AssertJUnit.assertTrue(MayDecisions.redirects(ai, List.of(theirs)));
    }

    @Test
    public void aGoodCounterIsNotRemovedForNothing() {
        final Card host = put("Grizzly Bears", ai, ZoneType.Battlefield);
        AssertJUnit.assertFalse(may(host, "DB$ RemoveCounter | Defined$ Self | CounterType$ P1P1 | CounterNum$ 1 | Optional$ True", null));
        AssertJUnit.assertTrue(may(host, "DB$ RemoveCounter | Defined$ Self | CounterType$ M1M1 | CounterNum$ 1 | Optional$ True", null));
    }

    @Test
    public void controlIsNotGivenToAnOpponent() {
        final Card host = put("Grizzly Bears", ai, ZoneType.Battlefield);
        AssertJUnit.assertFalse(may(host, "DB$ GainControl | Defined$ Self | NewController$ Opponent | Optional$ True", null));
        AssertJUnit.assertTrue(may(host, "DB$ GainControl | Defined$ Self | NewController$ You | Optional$ True", null));
    }

    @Test
    public void aHandOfDeadCardsIsDiscardedForANewOneAndAGoodHandIsKept() {
        final Card host = put("Grizzly Bears", ai, ZoneType.Battlefield);
        host.setSVar("DBDraw", "DB$ Draw | NumCards$ 7");
        final String wheel = "DB$ Discard | Mode$ Hand | Defined$ Player | Optional$ True | SubAbility$ DBDraw";
        for (final Card c : new java.util.ArrayList<>(ai.getCardsIn(ZoneType.Hand))) {
            ai.getZone(ZoneType.Hand).remove(c);
        }
        AssertJUnit.assertTrue("an empty hand loses nothing", may(host, wheel, PlayerActionConfirmMode.Random));
        put("Serra Angel", 4, ai, ZoneType.Hand); // nothing it can cast soon
        AssertJUnit.assertTrue(may(host, wheel, PlayerActionConfirmMode.Random));
        for (final Card c : new java.util.ArrayList<>(ai.getCardsIn(ZoneType.Hand))) {
            ai.getZone(ZoneType.Hand).remove(c);
        }
        put("Plains", 4, ai, ZoneType.Battlefield);
        put("Grizzly Bears", 5, ai, ZoneType.Hand);
        AssertJUnit.assertFalse("five cards it can cast against a fresh seven", may(host, wheel, PlayerActionConfirmMode.Random));
    }

    @Test
    public void anyNumberOfCreaturesIsOnlyAsManyAsWorthIt() {
        final Card host = put("Crovax the Cursed", ai, ZoneType.Battlefield);
        final Card a1 = put("Serra Angel", ai, ZoneType.Battlefield);
        final Card a2 = put("Serra Angel", ai, ZoneType.Battlefield);
        host.setSVar("DB1", "DB$ Draw | NumCards$ 1");
        final SpellAbility sa = AbilityFactory.getAbility("DB$ Sacrifice | Optional$ True | SacValid$ Creature.Other | Defined$ You | Amount$ 5 | SubAbility$ DB1", host);
        sa.setActivatingPlayer(ai);
        final CardCollection both = new CardCollection();
        both.add(a1);
        both.add(a2);
        AssertJUnit.assertTrue("an Angel is worth more than a card", MayDecisions.limitSacrifices(ai, sa, both).isEmpty());
        host.setSVar("DB1", "DB$ Draw | NumCards$ 4");
        final SpellAbility rich = AbilityFactory.getAbility("DB$ Sacrifice | Optional$ True | SacValid$ Creature.Other | Defined$ You | Amount$ 5 | SubAbility$ DB1", host);
        rich.setActivatingPlayer(ai);
        AssertJUnit.assertEquals(2, MayDecisions.limitSacrifices(ai, rich, both).size());
    }
}
