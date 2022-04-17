package forge.ai.simulation;

import java.util.List;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.card.Card;
import forge.game.card.CounterEnumType;
import forge.game.combat.Combat;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

public class SpellAbilityPickerSimulationTest extends SimulationTest {
    @Test
	public void testPickingLethalDamage() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);
        p.setTeam(0);
        
        addCard("Mountain", p);
        addCardToZone("Shock", p, ZoneType.Hand);

        Player opponent = game.getPlayers().get(0);
        opponent.setTeam(1);
        
        addCard("Runeclaw Bear", opponent);
        opponent.setLife(2, null);

        game.getPhaseHandler().devModeSet(PhaseType.MAIN2, p);
        game.getAction().checkStateEffects(true);

        SpellAbilityPicker picker = new SpellAbilityPicker(game, p);
        SpellAbility sa = picker.chooseSpellAbilityToPlay(null);
        AssertJUnit.assertNotNull(sa);
        AssertJUnit.assertNull(sa.getTargetCard());
        AssertJUnit.assertEquals(opponent, sa.getTargets().getFirstTargetedPlayer());
    }

    @Test
	public void testPickingKillingCreature() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);

        addCard("Mountain", p);
        addCardToZone("Shock", p, ZoneType.Hand);

        Player opponent = game.getPlayers().get(0);
        Card bearCard = addCard("Runeclaw Bear", opponent);
        opponent.setLife(20, null);

        game.getPhaseHandler().devModeSet(PhaseType.MAIN2, p);
        game.getAction().checkStateEffects(true);

        SpellAbilityPicker picker = new SpellAbilityPicker(game, p);
        SpellAbility sa = picker.chooseSpellAbilityToPlay(null);
        AssertJUnit.assertNotNull(sa);
        AssertJUnit.assertEquals(bearCard, sa.getTargetCard());
        AssertJUnit.assertNull(sa.getTargets().getFirstTargetedPlayer());
    }

    @Test
	public void testSequenceStartingWithPlayingLand() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);

        Card mountain = addCardToZone("Mountain", p, ZoneType.Hand);
        addCardToZone("Shock", p, ZoneType.Hand);

        Player opponent = game.getPlayers().get(0);
        addCard("Runeclaw Bear", opponent);
        opponent.setLife(20, null);

        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, p);
        game.getAction().checkStateEffects(true);

        SpellAbilityPicker picker = new SpellAbilityPicker(game, p);
        SpellAbility sa = picker.chooseSpellAbilityToPlay(null);
        //assertEquals(game.PLAY_LAND_SURROGATE, sa);
        AssertJUnit.assertEquals(mountain, sa.getHostCard());

        Plan plan = picker.getPlan();
        AssertJUnit.assertEquals(2, plan.getDecisions().size());
        AssertJUnit.assertEquals("Play land Mountain", plan.getDecisions().get(0).saRef.toString());
        AssertJUnit.assertEquals("Shock deals 2 damage to any target.", plan.getDecisions().get(1).saRef.toString());
        AssertJUnit.assertTrue(plan.getDecisions().get(1).targets.toString().contains("Runeclaw Bear"));
    }

    @Test
	public void testModeSelection() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);

        addCard("Plains", p);
        addCard("Island", p);
        addCard("Swamp", p);
        Card spell = addCardToZone("Dromar's Charm", p, ZoneType.Hand);

        Player opponent = game.getPlayers().get(0);
        addCard("Runeclaw Bear", opponent);

        game.getPhaseHandler().devModeSet(PhaseType.MAIN2, p);
        game.getAction().checkStateEffects(true);

        // Expected: All creatures get -2/-2 to kill the bear.
        SpellAbilityPicker picker = new SpellAbilityPicker(game, p);
        SpellAbility sa = picker.chooseSpellAbilityToPlay(null);
        AssertJUnit.assertEquals(spell.getSpellAbilities().get(0), sa);
        AssertJUnit.assertEquals("Dromar's Charm -> Target creature gets -2/-2 until end of turn.", picker.getPlan().getDecisions().get(0).modesStr);
    }

    @Test
	public void testModeSelection2() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);

        addCard("Plains", p);
        addCard("Island", p);
        addCard("Swamp", p);
        Card spell = addCardToZone("Dromar's Charm", p, ZoneType.Hand);

        game.getPhaseHandler().devModeSet(PhaseType.MAIN2, p);
        game.getAction().checkStateEffects(true);

        // Expected: Gain 5 life, since other modes aren't helpful.
        SpellAbilityPicker picker = new SpellAbilityPicker(game, p);
        SpellAbility sa = picker.chooseSpellAbilityToPlay(null);
        AssertJUnit.assertEquals(spell.getSpellAbilities().get(0), sa);
        AssertJUnit.assertEquals("Dromar's Charm -> You gain 5 life.", picker.getPlan().getDecisions().get(0).modesStr);
    }

    @Test
	public void testMultipleModes() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);

        addCard("Mountain", p);
        addCard("Mountain", p);
        addCard("Mountain", p);
        addCard("Mountain", p);
        Card spell = addCardToZone("Fiery Confluence", p, ZoneType.Hand);

        Player opponent = game.getPlayers().get(0);
        addCard("Runeclaw Bear", opponent);
        opponent.setLife(20, null);

        game.getPhaseHandler().devModeSet(PhaseType.MAIN2, p);
        game.getAction().checkStateEffects(true);

        // Expected: 2x 1 damage to each creature, 1x 2 damage to each opponent.
        SpellAbilityPicker picker = new SpellAbilityPicker(game, p);
        SpellAbility sa = picker.chooseSpellAbilityToPlay(null);
        AssertJUnit.assertEquals(spell.getSpellAbilities().get(0), sa);

        String dmgCreaturesStr = "Fiery Confluence deals 1 damage to each creature.";
        String dmgOppStr = "Fiery Confluence deals 2 damage to each opponent.";
        String expected = "Fiery Confluence -> " + dmgCreaturesStr + " " + dmgCreaturesStr + " " + dmgOppStr;
        AssertJUnit.assertEquals(expected, picker.getPlan().getDecisions().get(0).modesStr);
    }

    @Test
	public void testMultipleModes2() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);

        addCard("Mountain", p);
        addCard("Mountain", p);
        addCard("Mountain", p);
        addCard("Mountain", p);
        Card spell = addCardToZone("Fiery Confluence", p, ZoneType.Hand);

        Player opponent = game.getPlayers().get(0);
        addCard("Runeclaw Bear", opponent);
        opponent.setLife(6, null);

        game.getPhaseHandler().devModeSet(PhaseType.MAIN2, p);
        game.getAction().checkStateEffects(true);

        // Expected: 3x 2 damage to each opponent.
        SpellAbilityPicker picker = new SpellAbilityPicker(game, p);
        SpellAbility sa = picker.chooseSpellAbilityToPlay(null);
        AssertJUnit.assertEquals(spell.getSpellAbilities().get(0), sa);

        String dmgOppStr = "Fiery Confluence deals 2 damage to each opponent.";
        String expected = "Fiery Confluence -> " + dmgOppStr + " " + dmgOppStr + " " + dmgOppStr;
        AssertJUnit.assertEquals(expected, picker.getPlan().getDecisions().get(0).modesStr);
    }

    @Test
	public void testMultipleTargets() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);

        addCard("Mountain", p);
        addCard("Mountain", p);
        Card spell = addCardToZone("Arc Trail", p, ZoneType.Hand);

        Player opponent = game.getPlayers().get(0);
        Card bear = addCard("Runeclaw Bear", opponent);
        Card men = addCard("Flying Men", opponent);
        opponent.setLife(20, null);

        game.getPhaseHandler().devModeSet(PhaseType.MAIN2, p);
        game.getAction().checkStateEffects(true);

        SpellAbilityPicker picker = new SpellAbilityPicker(game, p);
        SpellAbility sa = picker.chooseSpellAbilityToPlay(null);
        AssertJUnit.assertEquals(spell.getSpellAbilities().get(0), sa);
        AssertJUnit.assertEquals(bear, sa.getTargetCard());
        AssertJUnit.assertEquals("2", sa.getParam("NumDmg"));
        SpellAbility subSa = sa.getSubAbility();
        AssertJUnit.assertEquals(men, subSa.getTargetCard());
        AssertJUnit.assertEquals("1", subSa.getParam("NumDmg"));
    }

    @Test
	public void testLandSearchForCombo() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);

        addCard("Forest", p);
        addCard("Thespian's Stage", p);
        Card darkDepths = addCard("Dark Depths", p);

        Card cropRotation = addCardToZone("Crop Rotation", p, ZoneType.Hand);

        addCardToZone("Forest", p, ZoneType.Library);
        addCardToZone("Urborg, Tomb of Yawgmoth", p, ZoneType.Library);
        addCardToZone("Swamp", p, ZoneType.Library);

        darkDepths.setCounters(CounterEnumType.ICE, 10);

        game.getPhaseHandler().devModeSet(PhaseType.MAIN2, p);
        game.getAction().checkStateEffects(true);

        AssertJUnit.assertEquals(10, darkDepths.getCounters(CounterEnumType.ICE));
        SpellAbilityPicker picker = new SpellAbilityPicker(game, p);
        SpellAbility sa = picker.chooseSpellAbilityToPlay(null);
        AssertJUnit.assertEquals(cropRotation.getSpellAbilities().get(0), sa);
        // Expected: Sac a Forest to get an Urborg.
        List<String> choices = picker.getPlan().getDecisions().get(0).choices;
        AssertJUnit.assertEquals(2, choices.size());
        AssertJUnit.assertEquals("Forest", choices.get(0));
        AssertJUnit.assertEquals("Urborg, Tomb of Yawgmoth", choices.get(1));
        // Next, expected to use Thespian's Stage to copy Dark Depths.
        Plan.Decision d2 = picker.getPlan().getDecisions().get(1);
        String expected = "{2}, {T}: Thespian's Stage becomes a copy of target land, except it has this ability.";
        AssertJUnit.assertEquals(expected, d2.saRef.toString());
        AssertJUnit.assertTrue(d2.targets.toString().contains("Dark Depths"));
    }

    @Test
	public void testPlayRememberedCardsLand() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);

        addCard("Mountain", p);
        addCard("Mountain", p);
        Card abbot = addCardToZone("Abbot of Keral Keep", p, ZoneType.Hand);
        addCardToZone("Lightning Bolt", p, ZoneType.Hand);
        // Note: This assumes the top of library is revealed. If the AI is made
        // smarter to not assume that, then this test can be updated to have
        // something that reveals top of library active - e.g. Lens of Clarity.
        addCardToZone("Mountain", p, ZoneType.Library);

        game.getPhaseHandler().devModeSet(PhaseType.MAIN2, p);
        game.getAction().checkStateEffects(true);

        // Expected plan:
        //  1. Play Abbot.
        //  2. Play land exiled by Abbot.
        //  3. Play Bolt targeting opponent.
        SpellAbilityPicker picker = new SpellAbilityPicker(game, p);
        SpellAbility sa = picker.chooseSpellAbilityToPlay(null);
        AssertJUnit.assertEquals(abbot.getSpellAbilities().get(0), sa);
        Plan plan = picker.getPlan();
        AssertJUnit.assertEquals(3, plan.getDecisions().size());
        AssertJUnit.assertEquals("Play land Mountain", plan.getDecisions().get(1).saRef.toString());
        AssertJUnit.assertEquals("Lightning Bolt deals 3 damage to any target.", plan.getDecisions().get(2).saRef.toString());
    }

    @Test
	public void testPlayRememberedCardsSpell() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);

        addCard("Mountain", p);
        addCard("Mountain", p);
        addCard("Mountain", p);
        Card abbot = addCardToZone("Abbot of Keral Keep", p, ZoneType.Hand);
        // Note: This assumes the top of library is revealed. If the AI is made
        // smarter to not assume that, then this test can be updated to have
        // something that reveals top of library active - e.g. Lens of Clarity.
        addCardToZone("Lightning Bolt", p, ZoneType.Library);

        game.getPhaseHandler().devModeSet(PhaseType.MAIN2, p);
        game.getAction().checkStateEffects(true);

        // Expected plan:
        //  1. Play Abbot.
        //  3. Play Bolt exiled by Abbot.
        SpellAbilityPicker picker = new SpellAbilityPicker(game, p);
        SpellAbility sa = picker.chooseSpellAbilityToPlay(null);
        AssertJUnit.assertEquals(abbot.getSpellAbilities().get(0), sa);
        Plan plan = picker.getPlan();
        AssertJUnit.assertEquals(2, plan.getDecisions().size());
        String saDesc = plan.getDecisions().get(1).saRef.toString();
        AssertJUnit.assertTrue(saDesc, saDesc.startsWith("Lightning Bolt deals 3 damage to any target."));
    }
    
    @Test
	public void testPlayingPumpSpellsAfterBlocks() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);
        Player opponent = game.getPlayers().get(0);
        opponent.setLife(2, null);
        
        Card blocker = addCard("Fugitive Wizard", opponent);
        Card attacker1 = addCard("Dwarven Trader", p);
        attacker1.setSickness(false);
        Card attacker2 = addCard("Dwarven Trader", p);
        attacker2.setSickness(false);
        addCard("Mountain", p);
        addCardToZone("Brute Force", p, ZoneType.Hand);

        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, p);
        game.getAction().checkStateEffects(true);

        SpellAbilityPicker picker = new SpellAbilityPicker(game, p);
        AssertJUnit.assertNull(picker.chooseSpellAbilityToPlay(null));

        game.getPhaseHandler().devAdvanceToPhase(PhaseType.COMBAT_BEGIN);
        game.getAction().checkStateEffects(true);
        AssertJUnit.assertNull(picker.chooseSpellAbilityToPlay(null));

        game.getPhaseHandler().devModeSet(PhaseType.COMBAT_DECLARE_ATTACKERS, p);
        Combat combat = new Combat(p);
        combat.addAttacker(attacker1, opponent);
        combat.addAttacker(attacker2, opponent);
        game.getPhaseHandler().setCombat(combat);
        game.getAction().checkStateEffects(true);        
        AssertJUnit.assertNull(picker.chooseSpellAbilityToPlay(null));

        game.getPhaseHandler().devModeSet(PhaseType.COMBAT_DECLARE_BLOCKERS, p, false);
        game.getAction().checkStateEffects(true);
        combat.addBlocker(attacker1, blocker);
        combat.getBandOfAttacker(attacker1).setBlocked(true);
        combat.getBandOfAttacker(attacker2).setBlocked(false);
        combat.orderBlockersForDamageAssignment();
        combat.orderAttackersForDamageAssignment();
        SpellAbility sa = picker.chooseSpellAbilityToPlay(null);
        AssertJUnit.assertNotNull(sa);
        AssertJUnit.assertEquals("Target creature gets +3/+3 until end of turn.", sa.toString());
        AssertJUnit.assertEquals(attacker2, sa.getTargetCard());
    }
    
    @Test
	public void testPlayingSorceryPumpSpellsBeforeBlocks() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);
        Player opponent = game.getPlayers().get(0);
        opponent.setLife(2, null);

        addCard("Fugitive Wizard", opponent);
        Card attacker1 = addCard("Dwarven Trader", p);
        attacker1.setSickness(false);
        Card attacker2 = addCard("Kird Ape", p);
        attacker2.setSickness(false);
        addCard("Mountain", p);
        Card furor = addCardToZone("Furor of the Bitten", p, ZoneType.Hand);

        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, p);
        game.getAction().checkStateEffects(true);

        SpellAbilityPicker picker = new SpellAbilityPicker(game, p);
        SpellAbility sa = picker.chooseSpellAbilityToPlay(null);
        AssertJUnit.assertNotNull(sa);
        AssertJUnit.assertEquals(furor.getSpellAbilities().get(0), sa);
        AssertJUnit.assertEquals(attacker1, sa.getTargetCard());
    }

    @Test
	public void testPlayingRemovalBeforeBlocks() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);
        Player opponent = game.getPlayers().get(0);
        opponent.setLife(2, null);

        Card blocker = addCard("Fugitive Wizard", opponent);
        Card attacker1 = addCard("Dwarven Trader", p);
        attacker1.setSickness(false);
        addCard("Swamp", p);
        addCard("Swamp", p);
        addCardToZone("Doom Blade", p, ZoneType.Hand);

        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, p);
        game.getAction().checkStateEffects(true);

        SpellAbilityPicker picker = new SpellAbilityPicker(game, p);
        SpellAbility sa = picker.chooseSpellAbilityToPlay(null);
        AssertJUnit.assertNotNull(sa);
        AssertJUnit.assertEquals("Destroy target nonblack creature.", sa.toString());
        AssertJUnit.assertEquals(blocker, sa.getTargetCard());
    }
}
