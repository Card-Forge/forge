package forge.game.combat;

import java.util.List;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.ai.simulation.SimulationTest;
import forge.game.Game;
import forge.game.GameEntity;
import forge.game.ability.AbilityUtils;
import forge.game.card.Card;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.staticability.StaticAbilityMustAttack;
import forge.game.zone.ZoneType;

public class CombatExplainerTest extends SimulationTest {

    private Game game;
    private Player attacking;
    private Player defending;

    private void setUp() {
        game = initAndCreateGame();
        attacking = game.getPlayers().get(1);
        defending = game.getPlayers().get(0);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, attacking);
    }

    private Card addCreature(String name, Player p) {
        Card c = addCard(name, p);
        c.setSickness(false);
        return c;
    }

    private Combat startCombat() {
        game.getAction().checkStateEffects(true);
        Combat combat = new Combat(attacking);
        game.getPhaseHandler().setCombat(combat);
        return combat;
    }

    private static void assertContains(String text, String... expected) {
        AssertJUnit.assertNotNull(text);
        for (String e : expected) {
            AssertJUnit.assertTrue("Expected '" + e + "' in: " + text, text.contains(e));
        }
    }

    @Test
    public void testPacifismPreventsAttack() {
        setUp();
        Card bear = addCreature("Grizzly Bears", attacking);
        Card pacifism = addCard("Pacifism", defending);
        pacifism.attachToEntity(bear, null);
        game.getAction().checkStateEffects(true);

        AssertJUnit.assertFalse(CombatUtil.canAttack(bear, defending));
        assertContains(CombatExplainer.whyCantAttack(bear, defending), "Grizzly Bears", "Pacifism", "can't attack or block");
    }

    @Test
    public void testSummoningSickness() {
        setUp();
        Card bear = addCard("Grizzly Bears", attacking);
        bear.setSickness(true);
        game.getAction().checkStateEffects(true);

        AssertJUnit.assertFalse(CombatUtil.canAttack(bear, defending));
        assertContains(CombatExplainer.whyCantAttack(bear, defending), "Grizzly Bears", "summoning sickness");
    }

    @Test
    public void testCanAttackHasNoReason() {
        setUp();
        Card bear = addCreature("Grizzly Bears", attacking);
        Combat combat = startCombat();

        AssertJUnit.assertNull(CombatExplainer.whyCantAttack(bear, defending));
        combat.addAttacker(bear, defending);
        AssertJUnit.assertTrue(CombatUtil.validateAttackers(combat));
        AssertJUnit.assertNull(CombatUtil.explainInvalidAttack(combat));
    }

    @Test
    public void testMaxAttackers() {
        setUp();
        addCreature("Silent Arbiter", attacking);
        Card bear1 = addCreature("Grizzly Bears", attacking);
        Card bear2 = addCreature("Grizzly Bears", attacking);
        Combat combat = startCombat();
        combat.addAttacker(bear1, defending);
        combat.addAttacker(bear2, defending);

        AssertJUnit.assertFalse(CombatUtil.validateAttackers(combat));
        List<String> reasons = CombatExplainer.explainInvalidAttack(combat);
        assertContains(reasons.get(0), "1", "Silent Arbiter", "2");
    }

    @Test
    public void testMustAttackRequirement() {
        setUp();
        Card juggernaut = addCreature("Juggernaut", attacking);
        Card bear = addCreature("Grizzly Bears", attacking);
        Combat combat = startCombat();
        combat.addAttacker(bear, defending);

        AssertJUnit.assertFalse(CombatUtil.validateAttackers(combat));
        List<String> reasons = CombatExplainer.explainInvalidAttack(combat);
        AssertJUnit.assertEquals(reasons.toString(), 1, reasons.size());
        assertContains(reasons.get(0), juggernaut.toString(), "attacks each combat if able");
        // the closest legal attack suggested on demand adds the Juggernaut
        assertContains(CombatExplainer.suggestLegalAttacks(combat),
                bear + " attacking " + defending + ", " + juggernaut + " attacking " + defending);
    }

    @Test
    public void testGoadedCreatureMustAttack() {
        setUp();
        Card bear = addCreature("Grizzly Bears", attacking);
        bear.addGoad(game.getNextTimestamp(), defending);
        Combat combat = startCombat();

        AssertJUnit.assertFalse(CombatUtil.validateAttackers(combat));
        assertContains(CombatUtil.explainInvalidAttack(combat), bear.toString(), "goaded");
    }

    @Test
    public void testCantAttackAlone() {
        setUp();
        Card mogg = addCreature("Mogg Flunkies", attacking);
        Card bear = addCreature("Grizzly Bears", attacking);
        Combat combat = startCombat();
        combat.addAttacker(mogg, defending);

        AssertJUnit.assertFalse(CombatUtil.validateAttackers(combat));
        assertContains(CombatUtil.explainInvalidAttack(combat), mogg.toString(), "can't attack or block alone.");
        // both not attacking and attacking together with another creature are suggested
        assertContains(CombatExplainer.suggestLegalAttacks(combat), "Not attacking at all",
                mogg + " attacking " + defending + ", " + bear + " attacking " + defending);
    }

    // Furygale Flocking: each pair of tokens has to attack a specific opponent
    @Test
    public void testMustAttackSpecificPlayer() {
        game = initAndCreateThreePlayerGame();
        attacking = game.getPlayers().get(1);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, attacking);
        SpellAbility sa = addCardToZone("Furygale Flocking", attacking, ZoneType.Hand).getFirstSpellAbility();
        sa.setActivatingPlayer(attacking);
        AbilityUtils.resolve(sa);
        Combat combat = startCombat();

        List<Card> tokens = attacking.getCreaturesInPlay();
        AssertJUnit.assertEquals(4, tokens.size());
        Card token = tokens.get(0);
        GameEntity designated = StaticAbilityMustAttack.entitiesMustAttack(token).get(0);
        Card otherToken = null;
        for (Card t : tokens) {
            if (!StaticAbilityMustAttack.entitiesMustAttack(t).contains(designated)) {
                otherToken = t;
                break;
            }
        }
        AssertJUnit.assertNotNull(otherToken);

        // every token attacks the same opponent, which is the wrong one for half of them
        for (Card t : tokens) {
            combat.addAttacker(t, designated);
        }
        AssertJUnit.assertFalse(CombatUtil.validateAttackers(combat));
        String explanation = CombatUtil.explainInvalidAttack(combat);
        GameEntity otherDesignated = StaticAbilityMustAttack.entitiesMustAttack(otherToken).get(0);
        assertContains(explanation, otherToken + " must attack " + otherDesignated + " if able", "Furygale Flocking");
        AssertJUnit.assertFalse(explanation, explanation.contains(token + " must attack"));
        assertContains(CombatExplainer.suggestLegalAttacks(combat), otherToken + " attacking " + otherDesignated);
    }

    @Test
    public void testFlyingPreventsBlock() {
        setUp();
        Card angel = addCreature("Serra Angel", attacking);
        Card bear = addCreature("Grizzly Bears", defending);
        Combat combat = startCombat();
        combat.addAttacker(angel, defending);

        AssertJUnit.assertFalse(CombatUtil.canBlock(angel, bear, combat));
        assertContains(CombatExplainer.whyCantBlock(angel, bear, combat), "Serra Angel", "Grizzly Bears", "Flying");
    }

    @Test
    public void testCanBlockHasNoReason() {
        setUp();
        Card attacker = addCreature("Grizzly Bears", attacking);
        Card blocker = addCreature("Grizzly Bears", defending);
        Combat combat = startCombat();
        combat.addAttacker(attacker, defending);

        AssertJUnit.assertNull(CombatExplainer.whyCantBlock(attacker, blocker, combat));
    }

    @Test
    public void testTappedBlocker() {
        setUp();
        Card attacker = addCreature("Grizzly Bears", attacking);
        Card blocker = addCreature("Grizzly Bears", defending);
        blocker.setTapped(true);
        Combat combat = startCombat();
        combat.addAttacker(attacker, defending);

        assertContains(CombatExplainer.whyCantBlock(attacker, blocker, combat), "tapped");
    }

    @Test
    public void testCantBlockAlone() {
        setUp();
        Card attacker = addCreature("Grizzly Bears", attacking);
        Card mogg = addCreature("Mogg Flunkies", defending);
        addCreature("Grizzly Bears", defending);
        Combat combat = startCombat();
        combat.addAttacker(attacker, defending);
        combat.addBlocker(attacker, mogg);

        assertContains(CombatUtil.validateBlocks(combat, defending), mogg.toString(), "can't attack or block alone.");
    }

    @Test
    public void testLureSkippedBlock() {
        setUp();
        Card attacker = addCreature("Grizzly Bears", attacking);
        Card lure = addCard("Lure", attacking);
        lure.attachToEntity(attacker, null);
        Card blocker = addCreature("Grizzly Bears", defending);
        Combat combat = startCombat();
        combat.addAttacker(attacker, defending);

        assertContains(CombatUtil.validateBlocks(combat, defending), blocker.toString(), attacker.toString(),
                "Lure", "All creatures able to block enchanted creature do so.");
    }

    @Test
    public void testLureBlockingOtherAttacker() {
        setUp();
        Card lured = addCreature("Grizzly Bears", attacking);
        Card lure = addCard("Lure", attacking);
        lure.attachToEntity(lured, null);
        Card other = addCreature("Grizzly Bears", attacking);
        Card blocker = addCreature("Grizzly Bears", defending);
        Combat combat = startCombat();
        combat.addAttacker(lured, defending);
        combat.addAttacker(other, defending);

        // clicking the other attacker's blocker is rejected with the lure as reason
        AssertJUnit.assertFalse(CombatUtil.canBlock(other, blocker, combat));
        assertContains(CombatExplainer.whyCantBlock(other, blocker, combat), lured.toString(), "Lure");

        combat.addBlocker(other, blocker);
        assertContains(CombatUtil.validateBlocks(combat, defending), "is blocking " + other, lured.toString(), "Lure");
    }

    @Test
    public void testBlocksEachCombatIfAble() {
        setUp();
        Card attacker = addCreature("Grizzly Bears", attacking);
        Card golem = addCreature("Iron Golem", defending);
        Combat combat = startCombat();
        combat.addAttacker(attacker, defending);

        assertContains(CombatUtil.validateBlocks(combat, defending), golem.toString(), attacker.toString(), "each combat");
    }

    @Test
    public void testMenaceSingleBlocker() {
        setUp();
        Card brute = addCreature("Boggart Brute", attacking);
        Card blocker = addCreature("Grizzly Bears", defending);
        Combat combat = startCombat();
        combat.addAttacker(brute, defending);
        combat.addBlocker(brute, blocker);

        assertContains(CombatUtil.validateBlocks(combat, defending), brute.toString(), "at least 2", "Menace");
    }

    // Marble Priest: only specific creatures (Walls) are required to block it
    @Test
    public void testMustBeBlockedBySpecificCreatures() {
        setUp();
        Card priest = addCreature("Marble Priest", attacking);
        Card other = addCreature("Grizzly Bears", attacking);
        Card wall = addCreature("Wall of Wood", defending);
        Card bear = addCreature("Grizzly Bears", defending);
        Combat combat = startCombat();
        combat.addAttacker(priest, defending);
        combat.addAttacker(other, defending);

        assertContains(CombatUtil.validateBlocks(combat, defending), wall + " must block " + priest,
                "All Walls able to block Marble Priest do so.");
        // creatures that aren't Walls are free to block something else
        AssertJUnit.assertNull(CombatExplainer.whyCantBlock(other, bear, combat));

        combat.addBlocker(other, wall);
        assertContains(CombatUtil.validateBlocks(combat, defending), wall + " is blocking " + other, "must block " + priest);
    }

    // "target creature blocks target creature this turn if able" (e.g. Hunt Down)
    @Test
    public void testMustBlockSpecificAttacker() {
        setUp();
        Card required = addCreature("Grizzly Bears", attacking);
        Card other = addCreature("Runeclaw Bear", attacking);
        Card blocker = addCreature("Centaur Courser", defending);
        blocker.addMustBlockCard(game.getNextTimestamp(), required);
        Combat combat = startCombat();
        combat.addAttacker(required, defending);
        combat.addAttacker(other, defending);

        assertContains(CombatUtil.validateBlocks(combat, defending), blocker + " must block " + required + " if able");
        assertContains(CombatExplainer.whyCantBlock(other, blocker, combat), blocker + " must block " + required);

        combat.addBlocker(other, blocker);
        assertContains(CombatUtil.validateBlocks(combat, defending), blocker + " is blocking " + other, "must block " + required);
    }

    // every creature with a problem is reported, up to three of them
    @Test
    public void testSeveralBlockersReported() {
        setUp();
        Card piper = addCreature("Talruum Piper", attacking);
        List<Card> thopters = List.of(addCreature("Ornithopter", defending), addCreature("Ornithopter", defending),
                addCreature("Ornithopter", defending), addCreature("Ornithopter", defending));
        Combat combat = startCombat();
        combat.addAttacker(piper, defending);

        String[] errors = CombatUtil.validateBlocks(combat, defending).split("\n");
        AssertJUnit.assertEquals(3, errors.length);
        for (int i = 0; i < errors.length; i++) {
            assertContains(errors[i], thopters.get(i) + " must block " + piper);
        }

        // once enough of them block, only the remaining ones are reported
        combat.addBlocker(piper, thopters.get(0));
        combat.addBlocker(piper, thopters.get(1));
        combat.addBlocker(piper, thopters.get(2));
        assertContains(CombatUtil.validateBlocks(combat, defending), thopters.get(3) + " must block " + piper);
        combat.addBlocker(piper, thopters.get(3));
        AssertJUnit.assertNull(CombatUtil.validateBlocks(combat, defending));
    }
}
