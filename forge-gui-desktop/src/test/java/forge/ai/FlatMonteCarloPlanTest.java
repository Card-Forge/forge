package forge.ai;

import java.util.List;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.ai.simulation.DeckPlan;
import forge.ai.simulation.FlatMonteCarloPicker;
import forge.game.Game;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/** The deck plan (read once from the whole deck) and the opponent-turn lookahead. */
public class FlatMonteCarloPlanTest extends AITest {
    private static final int LONG_TIMEOUT_MS = 240_000;

    private Game game() {
        final Game game = initAndCreateGame();
        game.AI_FLAT_MONTE_CARLO = true;
        game.AI_TIMEOUT = 10;
        game.getPlayers().get(1).setTeam(0);
        game.getPlayers().get(0).setTeam(1);
        return game;
    }

    private void deck(final Player p, final int copies, final String... spells) {
        for (final String s : spells) {
            for (int i = 0; i < copies; i++) {
                addCardToZone(s, p, ZoneType.Library);
            }
        }
        for (int i = 0; i < 20; i++) {
            addCardToZone("Mountain", p, ZoneType.Library);
        }
    }

    @Test
    public void burnDeckIsRecognised() {
        final Game game = game();
        final Player ai = game.getPlayers().get(1);
        deck(ai, 4, "Lightning Bolt", "Chain Lightning", "Shock");
        deck(ai, 1, "Runeclaw Bear", "Hill Giant");
        AssertJUnit.assertEquals(DeckPlan.Archetype.BURN, DeckPlan.forSetting("auto", ai).archetype);
    }

    @Test
    public void counterspellDeckIsControl() {
        final Game game = game();
        final Player ai = game.getPlayers().get(1);
        deck(ai, 4, "Counterspell", "Mana Leak", "Essence Scatter");
        deck(ai, 1, "Serra Angel", "Divination");
        AssertJUnit.assertEquals(DeckPlan.Archetype.CONTROL, DeckPlan.forSetting("auto", ai).archetype);
    }

    @Test
    public void cheapCreatureDeckIsAggro() {
        final Game game = game();
        final Player ai = game.getPlayers().get(1);
        deck(ai, 4, "Runeclaw Bear", "Goblin Piker", "Elite Vanguard", "Savannah Lions");
        deck(ai, 1, "Shock");
        AssertJUnit.assertEquals(DeckPlan.Archetype.AGGRO, DeckPlan.forSetting("auto", ai).archetype);
    }

    @Test
    public void aTinyOrMixedDeckIsMidrange() {
        final Game game = game();
        final Player ai = game.getPlayers().get(1);
        deck(ai, 1, "Hill Giant", "Divination");
        AssertJUnit.assertEquals(DeckPlan.Archetype.MIDRANGE, DeckPlan.forSetting("auto", ai).archetype);
    }

    @Test
    public void theSettingCanForceOrSwitchOffThePlan() {
        final Game game = game();
        final Player ai = game.getPlayers().get(1);
        deck(ai, 4, "Lightning Bolt", "Shock");
        AssertJUnit.assertSame(DeckPlan.NONE, DeckPlan.forSetting("off", ai));
        AssertJUnit.assertSame(DeckPlan.NONE, DeckPlan.forSetting(null, ai));
        AssertJUnit.assertEquals(DeckPlan.Archetype.CONTROL, DeckPlan.forSetting("Control", ai).archetype);
    }

    @Test
    public void samePlanEveryTime() {
        final Game game = game();
        final Player ai = game.getPlayers().get(1);
        deck(ai, 4, "Runeclaw Bear", "Goblin Piker", "Shock");
        final DeckPlan.Archetype first = DeckPlan.forSetting("auto", ai).archetype;
        for (int i = 0; i < 5; i++) {
            AssertJUnit.assertEquals(first, DeckPlan.forSetting("auto", ai).archetype);
        }
    }

    /** The AI has 5 life; the opponent's two 3/3s are 6 damage next turn. A blocker is the only way to stay alive. */
    private String cracksBack(final int lookaheadPercent) {
        final Game game = game();
        final Player ai = game.getPlayers().get(1);
        final Player opp = game.getPlayers().get(0);
        ai.setLife(5, null);
        for (int i = 0; i < 2; i++) {
            addCard("Mountain", ai).setSickness(false);
            addCard("Island", ai).setSickness(false);
        }
        addCard("Hill Giant", opp).setSickness(false);
        addCard("Hill Giant", opp).setSickness(false);
        final forge.game.card.Card giant = addCardToZone("Hill Giant", ai, ZoneType.Hand);
        final forge.game.card.Card draw = addCardToZone("Divination", ai, ZoneType.Hand);
        for (int i = 0; i < 10; i++) {
            addCardToZone("Mountain", ai, ZoneType.Library);
            addCardToZone("Plains", opp, ZoneType.Library);
        }
        game.getPhaseHandler().devModeSet(PhaseType.MAIN2, ai);
        game.getAction().checkStateEffects(true);
        final AiController brain = ((PlayerControllerAi) ai.getController()).getAi();
        final FlatMonteCarloPicker.Config cfg = FlatMonteCarloPicker.Config.fromProfile(brain, ai);
        cfg.opponentLookaheadPercent = lookaheadPercent;
        cfg.budgetNanos = java.util.concurrent.TimeUnit.SECONDS.toNanos(8); // the test is about the choice, not about speed
        // the heuristics' first choice is the card draw; whether the blocker beats it is for the rollouts to say
        final SpellAbility drawSa = draw.getFirstSpellAbility();
        final SpellAbility giantSa = giant.getFirstSpellAbility();
        drawSa.setActivatingPlayer(ai);
        giantSa.setActivatingPlayer(ai);
        final List<SpellAbility> approved = java.util.Arrays.asList(drawSa, giantSa);
        final FlatMonteCarloPicker.Result r = FlatMonteCarloPicker.choose(ai, approved, cfg, null);
        if (System.getProperty("flatmc.debug") != null) {
            System.out.println("   lookahead " + lookaheadPercent + "% -> " + r);
        }
        return r.hold || r.choice == null ? null : r.choice.getHostCard().getName();
    }

    @Test(timeOut = LONG_TIMEOUT_MS)
    public void lookaheadStillPicksTheBlockerWhenTheCrackBackIsLethal() {
        AssertJUnit.assertEquals("without a blocker two Hill Giants kill the AI next turn", "Hill Giant", cracksBack(60));
    }
}
