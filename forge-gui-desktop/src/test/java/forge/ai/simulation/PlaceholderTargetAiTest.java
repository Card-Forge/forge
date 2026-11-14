package forge.ai.simulation;

import org.testng.AssertJUnit;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import forge.ai.AiAbilityDecision;
import forge.ai.PlaceholderTargetAi;
import forge.game.card.Card;
import forge.game.phase.PhaseType;
import forge.game.cost.Cost;
import forge.game.spellability.SpellAbility;
import forge.game.trigger.Trigger;
import forge.game.zone.ZoneType;

/**
 * The "choose target opponent" of Speedbrood Stalker, Descendant of Masumaro, Iroh, Jon Irenicus, Coveted Falcon,
 * Parnesse and Tresserhorn's Lord, and the decisions that follow them: what to give away, and when to pay to keep a card.
 */
public class PlaceholderTargetAiTest extends FlatMcTestBase {

    @BeforeMethod
    public void setUp() {
        setUpGame();
    }

    /** The ability a trigger of the card runs (the first one whose root is an ability of the card). */
    private SpellAbility trigger(final Card host, final int index) {
        final Trigger t = host.getTriggers().get(index);
        final SpellAbility sa = t.ensureAbility();
        sa.setActivatingPlayer(host.getController());
        return sa;
    }

    /** The ability of the trigger that runs the given SVar (a card has more than one trigger). */
    private SpellAbility triggerRunning(final Card host, final String svar) {
        for (final Trigger t : host.getTriggers()) {
            if (svar.equals(t.getMapParams().get("Execute"))) {
                final SpellAbility sa = t.ensureAbility();
                sa.setActivatingPlayer(host.getController());
                return sa;
            }
        }
        throw new AssertionError(host + " has no trigger that runs " + svar);
    }

    @Test
    public void theSevenCardsUseTheTargetOnlyEffectNotAPump() {
        final String[][] cards = { { "Speedbrood Stalker", "TrigTarget" }, { "Descendant of Masumaro", "SelectOpponent" },
                { "Parnesse, the Subtle Brush", "TrigTarget" }, { "Tresserhorn's Lord, Returned", "TrigTarget" },
                { "Iroh, Tea Master", "TrigTarget" }, { "Coveted Falcon", "TrigDonate" } };
        for (final String[] c : cards) {
            final Card card = put(c[0], ai, ZoneType.Battlefield);
            final String script = card.getSVar(c[1]);
            AssertJUnit.assertTrue(c[0] + ": " + script, script.startsWith("DB$ TargetOnly"));
        }
    }

    @Test
    public void speedbroodStalkerTargetsAnOpponent() {
        final Card stalker = put("Speedbrood Stalker", ai, ZoneType.Battlefield);
        put("Grizzly Bears", opp, ZoneType.Battlefield);
        final SpellAbility sa = trigger(stalker, 0);
        final AiAbilityDecision d = PlaceholderTargetAi.decide(ai, sa, true);
        AssertJUnit.assertTrue(d.willingToPlay());
        AssertJUnit.assertTrue(com.google.common.collect.Iterables.contains(sa.getTargets().getTargetPlayers(), opp));
    }

    @Test
    public void parnesseNeverHandsTheOpponentACopy() {
        final Card parnesse = put("Parnesse, the Subtle Brush", ai, ZoneType.Battlefield);
        final SpellAbility sa = triggerRunning(parnesse, "TrigTarget");
        AssertJUnit.assertFalse(PlaceholderTargetAi.decide(ai, sa, false).willingToPlay());
        AssertJUnit.assertTrue(com.google.common.collect.Iterables.isEmpty(sa.getTargets().getTargetPlayers()));
    }

    @Test
    public void jonIrenicusGivesNothingAwayWithoutJunk() {
        final Card jon = put("Jon Irenicus, Shattered One", ai, ZoneType.Battlefield);
        put("Grizzly Bears", ai, ZoneType.Battlefield);
        final SpellAbility sa = trigger(jon, 0);
        AssertJUnit.assertFalse(PlaceholderTargetAi.decide(ai, sa, false).willingToPlay());
        AssertJUnit.assertTrue(PlaceholderTargetAi.junk(ai, sa).isEmpty());
    }

    @Test
    public void jonIrenicusGivesAwayACardMarkedDonateMe() {
        final Card jon = put("Jon Irenicus, Shattered One", ai, ZoneType.Battlefield);
        final Card bears = put("Grizzly Bears", ai, ZoneType.Battlefield);
        bears.setSVar("DonateMe", "1");
        final SpellAbility sa = trigger(jon, 0);
        AssertJUnit.assertTrue(PlaceholderTargetAi.decide(ai, sa, false).willingToPlay());
        final SpellAbility gift = sa.getSubAbility();
        AssertJUnit.assertTrue(PlaceholderTargetAi.donate(ai, gift).willingToPlay());
        AssertJUnit.assertTrue(gift.getTargets().getTargetCards().contains(bears));
    }

    @Test
    public void tresserhornsLordIsPaidForOnlyWithCreaturesToSpare() {
        final Card lord = put("Tresserhorn's Lord, Returned", ai, ZoneType.Battlefield);
        final SpellAbility sac = trigger(lord, 0).getSubAbility();
        final Cost cost = new Cost("Sac<3/Creature> PayLife<3>", false);
        AssertJUnit.assertFalse(PlaceholderTargetAi.worthPaying(ai, sac, cost));
        put("Grizzly Bears", 3, ai, ZoneType.Battlefield);
        AssertJUnit.assertTrue(PlaceholderTargetAi.worthPaying(ai, sac, cost));
    }

    private SpellAbility spellOf(final Card c) {
        final SpellAbility sa = c.getFirstSpellAbility();
        sa.setActivatingPlayer(ai);
        return sa;
    }

    @Test
    public void targetOnlyAiIsNotAPumpAi() {
        AssertJUnit.assertFalse(forge.ai.ability.PumpAi.class.isAssignableFrom(forge.ai.ability.TargetOnlyAi.class));
        AssertJUnit.assertEquals(forge.ai.ability.TargetOnlyAi.class,
                forge.ai.SpellApiToAi.Converter.get(spellOf(put("Bite Down", ai, ZoneType.Hand))).getClass());
    }

    @Test
    public void biteDownChoosesItsOwnCreatureThatKillsTheirs() {
        put("Forest", 2, ai, ZoneType.Battlefield);
        at(PhaseType.MAIN1, ai);
        final Card hill = put("Hill Giant", ai, ZoneType.Battlefield); // 3/3
        put("Grizzly Bears", ai, ZoneType.Battlefield); // 2/2: the weaker biter
        put("Grizzly Bears", opp, ZoneType.Battlefield);
        final SpellAbility bite = spellOf(put("Bite Down", ai, ZoneType.Hand));
        final AiAbilityDecision d = forge.ai.SpellApiToAi.Converter.get(bite).canPlayWithSubs(ai, bite);
        AssertJUnit.assertTrue(String.valueOf(d), d.willingToPlay());
        AssertJUnit.assertSame(hill, bite.getTargetCard());
    }

    @Test
    public void biteDownWithNothingToBiteIsNotCast() {
        put("Forest", 2, ai, ZoneType.Battlefield);
        at(PhaseType.MAIN1, ai);
        put("Hill Giant", ai, ZoneType.Battlefield);
        final SpellAbility bite = spellOf(put("Bite Down", ai, ZoneType.Hand));
        AssertJUnit.assertFalse(forge.ai.SpellApiToAi.Converter.get(bite).canPlayWithSubs(ai, bite).willingToPlay());
    }
}
