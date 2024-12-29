package forge.ai;

import forge.ai.simulation.GameStateEvaluator;
import forge.game.spellability.LandAbility;

import java.util.ArrayList;
import java.util.List;

import forge.item.PaperCard;
import forge.model.FModel;
import org.testng.AssertJUnit;
import org.testng.annotations.Ignore;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.card.Card;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

public class SpellAbilityPickerTest extends AITest {
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

        List<SpellAbility> chosenSa = p.getController().chooseSpellAbilityToPlay();
        System.out.println(chosenSa);
        SpellAbility sa = chosenSa.get(0);
        AssertJUnit.assertNotNull(sa);
        AssertJUnit.assertNull(sa.getTargetCard());
        AssertJUnit.assertEquals(opponent, sa.getTargets().getFirstTargetedPlayer());
    }

    @Ignore
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

        List<SpellAbility> chosenSa = p.getController().chooseSpellAbilityToPlay();
        System.out.println(chosenSa);
        System.out.println(gameStateToString(game));
        SpellAbility sa = chosenSa.get(0);
        AssertJUnit.assertNotNull(sa);
        AssertJUnit.assertEquals(bearCard, sa.getTargetCard());
        AssertJUnit.assertNull(sa.getTargets().getFirstTargetedPlayer());
    }

    @Ignore
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

        List<SpellAbility> chosenSa = p.getController().chooseSpellAbilityToPlay();
        System.out.println(chosenSa);
        System.out.println(gameStateToString(game));
        SpellAbility sa = chosenSa.get(0);
        AssertJUnit.assertTrue(sa instanceof LandAbility);
        AssertJUnit.assertEquals(mountain, sa.getHostCard());

        playUntilStackClear(game);
        game.getPhaseHandler().mainLoopStep();
        System.out.println(gameStateToString(game));
        sa = p.getController().chooseSpellAbilityToPlay().get(0);

        AssertJUnit.assertEquals("Shock deals 2 damage to any target.", sa.toString());
        // AssertJUnit.assertTrue(plan.getDecisions().get(1).targets.toString().contains("Runeclaw Bear"));
    }

    @Ignore
    @Test
    public void testPlayingLandAfterSpell() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);

        addCards("Island", 2, p);
        addCards("Forest", 3, p);

        Card tatyova = addCardToZone("Tatyova, Benthic Druid", p, ZoneType.Hand);
        addCardToZone("Forest", p, ZoneType.Hand);
        addCardToZone("Forest", p, ZoneType.Library);

        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, p);
        game.getAction().checkStateEffects(true);

        SpellAbility sa = p.getController().chooseSpellAbilityToPlay().get(0);
        AssertJUnit.assertEquals(tatyova, sa.getHostCard());

        // The plan should involve playing Tatyova first and then playing a land, to benefit from
        // the landfall trigger.
        playUntilStackClear(game);
        sa = p.getController().chooseSpellAbilityToPlay().get(0);
        AssertJUnit.assertEquals("Play land", sa.toString());
//        Plan plan = picker.getPlan();
//        AssertJUnit.assertEquals(2, plan.getDecisions().size());
//        AssertJUnit.assertEquals("Tatyova, Benthic Druid - Creature 3 / 3", plan.getDecisions().get(0).saRef.toString());
//        AssertJUnit.assertEquals("Play land", plan.getDecisions().get(1).saRef.toString());
    }

    @Ignore
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
        SpellAbility sa = p.getController().chooseSpellAbilityToPlay().get(0);
//        SpellAbilityPicker picker = new SpellAbilityPicker(game, p);
//        SpellAbility sa = picker.chooseSpellAbilityToPlay(null);
        AssertJUnit.assertEquals(spell.getSpellAbilities().get(0), sa);
        AssertJUnit.assertEquals("Dromar's Charm -> Target creature gets -2/-2 until end of turn.",
                sa.toString());
    }

    @Ignore
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
        SpellAbility sa = p.getController().chooseSpellAbilityToPlay().get(0);
//        SpellAbilityPicker picker = new SpellAbilityPicker(game, p);
//        SpellAbility sa = picker.chooseSpellAbilityToPlay(null);
        AssertJUnit.assertEquals(spell.getSpellAbilities().get(0), sa);
        AssertJUnit.assertEquals("Dromar's Charm -> You gain 5 life.", sa.toString());
    }

    /*
    @Test
    public void testMultipleModes() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);

        addCards("Mountain", 4, p);
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

        addCards("Mountain", 4, p);
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

        addCards("Mountain", 2, p);
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
    */

    @Ignore
    @Test
    public void targetUtilityLandOverRainbow() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);
        Player opponent = game.getPlayers().get(0);
        opponent.setLife(20, null);

        // start with the opponent having a basic land, a dual, and a rainbow
        addCard("Forest", opponent);
        addCard("Breeding Pool", opponent);
        addCard("Mana Confluence", opponent);
        Card desired = addCard("Mutavault", opponent);
        addCard("Strip Mine", p);

        // It doesn't want to use strip mine in main
        game.getPhaseHandler().devModeSet(PhaseType.COMBAT_DECLARE_BLOCKERS, p);
        game.getAction().checkStateEffects(true);

        // ensure that the land is played
        SpellAbility sa = p.getController().chooseSpellAbilityToPlay().get(0);
        AssertJUnit.assertEquals(desired, sa.getTargetCard());
    }
}
