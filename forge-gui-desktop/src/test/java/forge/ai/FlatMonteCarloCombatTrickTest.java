package forge.ai;

import java.util.List;

import org.testng.AssertJUnit;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import forge.ai.ability.ProtectAi;
import forge.game.Game;
import forge.game.ability.AbilityUtils;
import forge.game.ability.effects.ProtectEffect;
import forge.game.ability.ApiType;
import forge.game.card.Card;
import forge.game.combat.Combat;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/**
 * Combat tricks on ordinary creatures: does the AI play the right trick at the right moment, and keep it when it
 * would be wasted? Each scenario builds one exact combat (who attacks, who blocks, what the AI holds), gives the AI
 * priority and looks at the play it picks, once with the plain heuristics and once with Flat Monte Carlo.
 *
 * <p>Reading a failure: the message names the scenario and the mode. A "should cast" failure is a trick the AI keeps
 * in hand when it wins or saves the fight (a real misplay to fix). A "should keep" failure is a trick spent on a fight
 * that did not need it, or at the wrong moment.
 *
 * <p>Scenarios: pump to save a blocked attacker; pump for lethal on an unblocked attacker; protection against the
 * blocker; regeneration; pump on a blocker when the AI defends; and the restraint cases (a won fight, no mana, no
 * combat yet, the opponent's creature).
 */
public class FlatMonteCarloCombatTrickTest extends AITest {
    private static final int LONG_TIMEOUT_MS = 240_000;

    private Game game;
    private Player ai;
    private Player opp;

    @DataProvider(name = "modes")
    public Object[][] modes() {
        return new Object[][] { { false }, { true } };
    }

    private void setUp(final boolean flatMc) {
        game = initAndCreateGame();
        game.AI_FLAT_MONTE_CARLO = flatMc;
        game.AI_FLAT_MONTE_CARLO_MAX = flatMc; // a generous budget: the tests are about the choice, not about speed
        game.AI_TIMEOUT = 20;
        ai = game.getPlayers().get(1);
        opp = game.getPlayers().get(0);
        ai.setTeam(0);
        opp.setTeam(1);
        for (int i = 0; i < 10; i++) {
            addCardToZone("Mountain", ai, ZoneType.Library);
            addCardToZone("Plains", opp, ZoneType.Library);
        }
    }

    private Card ready(final String name, final Player owner) {
        final Card c = addCard(name, owner);
        c.setSickness(false);
        return c;
    }

    /** The AI's turn, blockers declared: {@code blockers} (may be empty) block {@code attacker}. */
    private Combat aiAttack(final Card attacker, final Card... blockers) {
        game.getPhaseHandler().devModeSet(PhaseType.COMBAT_DECLARE_BLOCKERS, ai, false);
        final Combat combat = new Combat(ai);
        combat.addAttacker(attacker, opp);
        for (final Card blocker : blockers) {
            combat.addBlocker(attacker, blocker);
        }
        // addBlocker only records the blocker: the band is marked blocked (or not) by the declare blockers step, and a band
        // never marked counts as unblocked in combat damage, in the AI checks and in every rollout copy of the game
        combat.setBlocked(attacker, blockers.length > 0);
        game.getPhaseHandler().setCombat(combat);
        game.getAction().checkStateEffects(true);
        return combat;
    }

    /** The opponent's turn, blockers declared: the AI's {@code blocker} blocks the opponent's {@code attacker}. */
    private Combat oppAttack(final Card attacker, final Card blocker) {
        game.getPhaseHandler().devModeSet(PhaseType.COMBAT_DECLARE_BLOCKERS, opp, false);
        final Combat combat = new Combat(opp);
        combat.addAttacker(attacker, ai);
        combat.addBlocker(attacker, blocker);
        combat.setBlocked(attacker, true); // see aiAttack
        game.getPhaseHandler().setCombat(combat);
        game.getAction().checkStateEffects(true);
        return combat;
    }

    private SpellAbility nextPlay() {
        final AiController brain = ((PlayerControllerAi) ai.getController()).getAi();
        final List<SpellAbility> plays = brain.chooseSpellAbilityToPlay();
        return plays == null || plays.isEmpty() ? null : plays.get(0);
    }

    /**
     * Diagnostic: in the BLOCKED rollouts of the log the attacker's damage reaches the player (opponent at 18 when
     * holding, 15 with +3/+3, 19 against Drudge Skeletons), as if the blockers had been lost. Does a copy of the game
     * keep the blocks?
     */
    @Test(timeOut = LONG_TIMEOUT_MS)
    public void gameCopyKeepsTheBlocks() {
        setUp(true);
        ready("Plains", ai);
        final Card attacker = ready("Grizzly Bears", ai);
        final Combat combat = aiAttack(attacker, ready("Hill Giant", opp));
        AssertJUnit.assertTrue("setup: the attacker is blocked", combat.isBlocked(attacker));
        final forge.ai.simulation.GameCopier copier = new forge.ai.simulation.GameCopier(game);
        final Game copy = copier.makeCopy();
        final Card copiedAttacker = copier.find(attacker);
        final Combat copied = copy.getCombat();
        AssertJUnit.assertNotNull("the copy has no combat at all", copied);
        AssertJUnit.assertTrue("the copy lost the attacker", copied.isAttacking(copiedAttacker));
        AssertJUnit.assertTrue("the copy lost the block, blockers of the Bears: " + copied.getBlockers(copiedAttacker),
                copied.isBlocked(copiedAttacker));
    }

    private static String describe(final SpellAbility sa) {
        return sa == null ? "pass" : sa.getHostCard().getName() + " -> " + sa.getTargetCard();
    }

    private void assertCasts(final String scenario, final boolean flatMc, final String card, final Card target) {
        final SpellAbility play = nextPlay();
        final String where = scenario + " [" + (flatMc ? "FlatMC" : "heuristics") + "]: should cast " + card + " on "
                + target + " but the AI picked: " + describe(play);
        AssertJUnit.assertNotNull(where, play);
        AssertJUnit.assertEquals(where, card, play.getHostCard().getName());
        AssertJUnit.assertEquals(where, target, play.getTargetCard());
    }

    private void assertKeeps(final String scenario, final boolean flatMc, final String card) {
        final SpellAbility play = nextPlay();
        AssertJUnit.assertTrue(scenario + " [" + (flatMc ? "FlatMC" : "heuristics") + "]: should keep " + card
                + " but the AI picked: " + describe(play), play == null || !card.equals(play.getHostCard().getName()));
    }

    // ------------------------------------------------------------------------------------------------ pump

    /** 2/2 blocked by a 2/2: +3/+3 turns a trade into a win with the attacker alive. */
    @Test(dataProvider = "modes", timeOut = LONG_TIMEOUT_MS)
    public void pumpSavesAndWinsABlockedAttacker(final boolean flatMc) {
        setUp(flatMc);
        ready("Forest", ai);
        addCardToZone("Giant Growth", ai, ZoneType.Hand);
        final Card attacker = ready("Grizzly Bears", ai);
        aiAttack(attacker, ready("Grizzly Bears", opp));
        assertCasts("blocked attacker, even trade", flatMc, "Giant Growth", attacker);
    }

    /** 2/2 blocked by a 3/3: +3/+3 kills the blocker and the attacker lives (5/5). */
    @Test(dataProvider = "modes", timeOut = LONG_TIMEOUT_MS)
    public void pumpBeatsABiggerBlocker(final boolean flatMc) {
        setUp(flatMc);
        ready("Forest", ai);
        addCardToZone("Giant Growth", ai, ZoneType.Hand);
        final Card attacker = ready("Grizzly Bears", ai);
        aiAttack(attacker, ready("Hill Giant", opp));
        assertCasts("blocked by a bigger creature", flatMc, "Giant Growth", attacker);
    }

    /** Unblocked 2/2 and the opponent at 5: +3/+3 is exactly lethal. */
    @Test(dataProvider = "modes", timeOut = LONG_TIMEOUT_MS)
    public void pumpAnUnblockedAttackerForLethal(final boolean flatMc) {
        setUp(flatMc);
        opp.setLife(5, null);
        ready("Forest", ai);
        addCardToZone("Giant Growth", ai, ZoneType.Hand);
        final Card attacker = ready("Grizzly Bears", ai);
        aiAttack(attacker);
        assertCasts("unblocked attacker, pump is lethal", flatMc, "Giant Growth", attacker);
    }

    /** The AI's own 2/2 blocks the opponent's 2/2: the pump wins the fight for the blocker. */
    @Test(dataProvider = "modes", timeOut = LONG_TIMEOUT_MS)
    public void pumpTheBlockerWhenDefending(final boolean flatMc) {
        setUp(flatMc);
        ready("Forest", ai);
        addCardToZone("Giant Growth", ai, ZoneType.Hand);
        final Card blocker = ready("Grizzly Bears", ai);
        oppAttack(ready("Grizzly Bears", opp), blocker);
        assertCasts("own blocker, even trade", flatMc, "Giant Growth", blocker);
    }

    // ------------------------------------------------------------------------------------------------ protection

    /** 2/2 blocked by a red 3/3: protection from red saves the attacker (the Giant's damage is prevented). */
    @Test(dataProvider = "modes", timeOut = LONG_TIMEOUT_MS)
    public void protectionSavesABlockedAttacker(final boolean flatMc) {
        setUp(flatMc);
        ready("Plains", ai);
        addCardToZone("Gods Willing", ai, ZoneType.Hand);
        final Card attacker = ready("Grizzly Bears", ai);
        aiAttack(attacker, ready("Hill Giant", opp));
        assertCasts("blocked by a bigger creature, protection in hand", flatMc, "Gods Willing", attacker);
    }

    /** The bait payoff: an animated Mutavault blocked by a 2/2 is saved and wins with Giant Growth. */
    @Test(dataProvider = "modes", timeOut = LONG_TIMEOUT_MS)
    public void pumpAnAnimatedManlandThatIsBlocked(final boolean flatMc) {
        if (!flatMc) {
            throw new org.testng.SkipException("PumpAi never targets an animated land: not part of the FlatMC fixes, FlatMC is asserted");
        }
        setUp(flatMc);
        ready("Forest", ai);
        addCardToZone("Giant Growth", ai, ZoneType.Hand);
        final Card vault = ready("Mutavault", ai);
        for (final SpellAbility sa : vault.getSpellAbilities()) {
            if (sa.isAbility() && sa.getApi() == ApiType.Animate) {
                sa.setActivatingPlayer(ai);
                AbilityUtils.resolve(sa);
            }
        }
        game.getAction().checkStateEffects(true);
        AssertJUnit.assertTrue("setup: Mutavault is a creature", vault.isCreature());
        aiAttack(vault, ready("Grizzly Bears", opp));
        assertCasts("animated manland blocked by a 2/2", flatMc, "Giant Growth", vault);
    }

    /**
     * Diagnostic for {@link #protectionSavesABlockedAttacker}: the same combat, one decision stage at a time, so that a
     * failure names the stage that says no (the colour list, the colour match, the "would be destroyed" check, the
     * creature list, or the final decision of the heuristics).
     */
    @Test(timeOut = LONG_TIMEOUT_MS)
    public void protectionDecisionStages() {
        setUp(false);
        ready("Plains", ai);
        final Card gods = addCardToZone("Gods Willing", ai, ZoneType.Hand);
        final Card attacker = ready("Grizzly Bears", ai);
        final Card giant = ready("Hill Giant", opp);
        final Combat combat = aiAttack(attacker, giant);
        final SpellAbility sa = gods.getSpellAbilities().get(0);
        sa.setActivatingPlayer(ai);
        final StringBuilder failed = new StringBuilder();
        if (!ProtectEffect.getProtectionList(sa).contains("red")) {
            failed.append("\nstage 1, colour list: ").append(ProtectEffect.getProtectionList(sa));
        }
        final String chosen = ProtectAi.toProtectFrom(giant, sa);
        if (!"red".equals(chosen)) {
            failed.append("\nstage 2, colour for the blocker: ").append(chosen);
        }
        if (!ComputerUtilCombat.attackerWouldBeDestroyed(ai, attacker, combat)) {
            failed.append("\nstage 3, the attacker would not be destroyed");
        }
        if (!ProtectAi.getProtectCreatures(ai, sa).contains(attacker)) {
            failed.append("\nstage 4, creatures worth protecting: ").append(ProtectAi.getProtectCreatures(ai, sa))
                    .append(" (targetable: ").append(attacker.canBeTargetedBy(sa))
                    .append(", declare blockers step: ").append(game.getPhaseHandler().is(PhaseType.COMBAT_DECLARE_BLOCKERS))
                    .append(", game combat is the test combat: ").append(game.getCombat() == combat)
                    .append(", attacking: ").append(combat.isAttacking(attacker))
                    .append(", blocked: ").append(combat.isBlocked(attacker)).append(")");
        }
        final AiAbilityDecision decision = SpellApiToAi.Converter.get(sa).canPlayWithSubs(ai, sa);
        if (!decision.willingToPlay()) {
            failed.append("\nstage 5, heuristics' decision: ").append(decision);
        }
        // stage 6: what the spell does when it resolves on the attacker (the colour the AI picks while resolving)
        try {
            sa.resetTargets();
            sa.getTargets().add(attacker);
            AbilityUtils.resolve(sa);
            final String gained = String.valueOf(attacker.getKeywords()).toLowerCase(java.util.Locale.ROOT);
            if (!gained.contains("protection") || !gained.contains("red")) {
                failed.append("\nstage 6, no protection from red after resolving, keywords: ").append(gained);
            }
        } catch (final RuntimeException e) {
            failed.append("\nstage 6, resolving threw ").append(e);
        }
        AssertJUnit.assertTrue("protection decision failed at:" + failed, failed.length() == 0);
    }

    // ------------------------------------------------------------------------------------------------ regeneration

    /** A blocked 1/1 with {B}: Regenerate against a 3/3: regenerate keeps the Skeleton. */
    @Test(dataProvider = "modes", timeOut = LONG_TIMEOUT_MS)
    public void regenerateABlockedCreature(final boolean flatMc) {
        setUp(flatMc);
        ready("Swamp", ai);
        final Card skeleton = ready("Drudge Skeletons", ai);
        aiAttack(skeleton, ready("Hill Giant", opp));
        final SpellAbility play = nextPlay();
        final String where = "blocked Drudge Skeletons [" + (flatMc ? "FlatMC" : "heuristics")
                + "]: should regenerate but the AI picked: " + describe(play);
        AssertJUnit.assertNotNull(where, play);
        AssertJUnit.assertEquals(where, skeleton, play.getHostCard());
        AssertJUnit.assertEquals(where, ApiType.Regenerate, play.getApi());
    }

    // ------------------------------------------------------------------------------------------------ restraint

    /** 3/3 blocked by a 2/1: the fight is won without help, the trick is worth more later. */
    @Test(dataProvider = "modes", timeOut = LONG_TIMEOUT_MS)
    public void keepThePumpWhenTheFightIsAlreadyWon(final boolean flatMc) {
        setUp(flatMc);
        ready("Forest", ai);
        addCardToZone("Giant Growth", ai, ZoneType.Hand);
        aiAttack(ready("Hill Giant", ai), ready("Savannah Lions", opp));
        assertKeeps("attacker already wins the fight", flatMc, "Giant Growth");
    }

    /** No untapped land: nothing can be cast. */
    @Test(dataProvider = "modes", timeOut = LONG_TIMEOUT_MS)
    public void noManaNoTrick(final boolean flatMc) {
        setUp(flatMc);
        ready("Forest", ai).setTapped(true);
        addCardToZone("Giant Growth", ai, ZoneType.Hand);
        final Card attacker = ready("Grizzly Bears", ai);
        aiAttack(attacker, ready("Grizzly Bears", opp));
        assertKeeps("no mana", flatMc, "Giant Growth");
    }

    /** First main phase, no combat: the pump is held for combat (the opponent would see it before blocking). */
    @Test(dataProvider = "modes", timeOut = LONG_TIMEOUT_MS)
    public void pumpIsHeldInTheMainPhase(final boolean flatMc) {
        setUp(flatMc);
        ready("Forest", ai);
        addCardToZone("Giant Growth", ai, ZoneType.Hand);
        ready("Grizzly Bears", ai);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        game.getAction().checkStateEffects(true);
        assertKeeps("main phase, no combat", flatMc, "Giant Growth");
    }

    /** The opponent's 2/2 is blocked by the AI's 3/3 (a won fight): the AI does not pump the opponent's creature. */
    @Test(dataProvider = "modes", timeOut = LONG_TIMEOUT_MS)
    public void neverPumpTheOpponentsCreature(final boolean flatMc) {
        setUp(flatMc);
        ready("Forest", ai);
        addCardToZone("Giant Growth", ai, ZoneType.Hand);
        final Card theirs = ready("Grizzly Bears", opp);
        oppAttack(theirs, ready("Hill Giant", ai));
        final SpellAbility play = nextPlay();
        AssertJUnit.assertTrue("[" + (flatMc ? "FlatMC" : "heuristics") + "] must not target the opponent's creature: "
                + describe(play), play == null || !theirs.equals(play.getTargetCard()));
    }
}
