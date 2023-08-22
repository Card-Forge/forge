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
    public void playTaplandIfNoPlays() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);

        // start with a hand with a basic, a tapland, and a card that can't be cast
        addCard("Forest", p);
        addCardToZone("Forest", p, ZoneType.Hand);
        Card desired = addCardToZone("Simic Guildgate", p, ZoneType.Hand);
        addCardToZone("Centaur Courser", p, ZoneType.Hand);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, p);
        game.getAction().checkStateEffects(true);

        // ensure that the tapland is paid
        SpellAbility sa = p.getController().chooseSpellAbilityToPlay().get(0);
        AssertJUnit.assertEquals(desired, sa.getHostCard());
    }

    @Ignore
    @Test
    public void playBouncelandIfNoPlays() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);

        // start with a hand with a basic, a bounceland, and a card that can't be cast
        addCard("Forest", p);
        addCardToZone("Forest", p, ZoneType.Hand);
        Card desired = addCardToZone("Simic Growth Chamber", p, ZoneType.Hand);
        addCardToZone("Centaur Courser", p, ZoneType.Hand);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, p);
        game.getAction().checkStateEffects(true);

        // ensure that the tapland is played
        SpellAbility sa = p.getController().chooseSpellAbilityToPlay().get(0);
        AssertJUnit.assertEquals(desired, sa.getHostCard());
    }

    @Ignore
    @Test
    public void playTronOverBasic() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);

        // start with a hand with a basic, a Tron land, and a card that can't be cast
        addCard("Urza's Tower", p);
        addCard("Urza's Mine", p);
        addCardToZone("Forest", p, ZoneType.Hand);
        Card desired = addCardToZone("Urza's Power Plant", p, ZoneType.Hand);
        addCardToZone("Opt", p, ZoneType.Hand);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, p);
        game.getAction().checkStateEffects(true);

        // ensure that the tron land is played
        SpellAbility sa = p.getController().chooseSpellAbilityToPlay().get(0);
        AssertJUnit.assertEquals(desired, sa.getHostCard());
    }

    @Test
    public void playManalessLands() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);

        // start with a hand with a land that can't produce mana.
        Card desired = addCardToZone("Maze of Ith", p, ZoneType.Hand);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, p);
        game.getAction().checkStateEffects(true);

        // ensure that the land is played
        SpellAbility sa = p.getController().chooseSpellAbilityToPlay().get(0);
        AssertJUnit.assertEquals(desired, sa.getHostCard());
    }

    @Test
    public void playBasicOverUtility() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);

        // start with a hand with a colorless utility land and a basic
        addCardToZone("Rogue's Passage", p,  ZoneType.Hand);
        Card desired = addCardToZone("Forest", p, ZoneType.Hand);

        // make sure that there is a card in the library with G mana cost
        addCardToZone("Grizzly Bears", p,  ZoneType.Library);

        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, p);
        game.getAction().checkStateEffects(true);

        // ensure that the basic land is played
        SpellAbility sa = p.getController().chooseSpellAbilityToPlay().get(0);
        AssertJUnit.assertEquals(desired, sa.getHostCard());
    }

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

    @Ignore
    @Test
    public void ensureAllLandsArePlayable() {
        initAndCreateGame();

        System.out.println("Adding lands to hand");

        // add every land to the player's hand
        List<Card> funky = new ArrayList<>();
        String previous = "";
        for (PaperCard c : FModel.getMagicDb().getCommonCards().getAllCards()) {
            // Only test one version of a card
            if (c.getName().equals(previous)) {
                continue;
            }
            previous = c.getName();

            // skip nonland cards
            if (!c.getRules().getType().isLand()) {
                continue;
            }

//            System.out.println(c.getName());

            // Skip glacial chasm, it's really weird.
            if (c.getName().equals("Glacial Chasm")) {
                System.out.println("Skipping " + c.getName());
                continue;
            }

            // reset the game
            Game game = resetGame();
            Player p = game.getPlayers().get(1);
            Player opponent = game.getPlayers().get(0);
            opponent.setLife(20, null);

            // add one of each basic to the battlefield so that bouncelands and similar work
            addCard("Plains", p);
            addCard("Island", p);
            addCard("Swamp", p);
            addCard("Mountain", p);
            addCard("Forest", p);
            // Add basics to library to ensure fetches work
            addCardToZone("Plains", p, ZoneType.Library);
            addCardToZone("Island", p, ZoneType.Library);
            addCardToZone("Swamp", p, ZoneType.Library);
            addCardToZone("Mountain", p, ZoneType.Library);
            addCardToZone("Forest", p, ZoneType.Library);

            game.getPhaseHandler().devModeSet(PhaseType.MAIN1, p);
            game.getAction().checkStateEffects(true);

            // Add the target card to the hand and test it
            Card desired = addCardToZone(c.getName(), p, ZoneType.Hand);

            List<SpellAbility> choices = p.getController().chooseSpellAbilityToPlay();
            if (choices == null) {
                funky.add(desired);
                continue;
            }
            SpellAbility sa = choices.get(0);
            if (sa == null || sa.getHostCard() != desired) {
                funky.add(desired);
                continue;
            }
//            AssertJUnit.assertEquals(desired, sa.getTargetCard());
//            GameStateEvaluator.Score s = new GameStateEvaluator().getScoreForGameState(game, p);
//            System.out.println("Starting score: " + s);
//            SpellAbilityPicker picker = new SpellAbilityPicker(game, p);
//            List<SpellAbility> candidateSAs = picker.getCandidateSpellsAndAbilities();
//            for (int i = 0; i < candidateSAs.size(); i++) {
//                SpellAbility sa = candidateSAs.get(i);
//                if (sa.isActivatedAbility()) {
//                    continue;
//                }
//                GameStateEvaluator.Score value = picker.evaluateSa(new SimulationController(s), game.getPhaseHandler().getPhase(), candidateSAs, i);
//                System.out.println("sa: " + sa.getHostCard() + ", value: " + value);
//                if (!(value.value > s.value)) {
//                    funky.add(sa.getHostCard());
//                }
//            }
        }

        // ensure that every land play has a higher evaluation than doing nothing
        System.out.println(funky);
        for (Card c : funky) {
            GameStateEvaluator gse = new GameStateEvaluator();
            Game game = resetGame();
            System.out.println(c.getName() + ": " + gse.evalCard(game, game.getStartingPlayer(), c));
        }
        AssertJUnit.assertEquals(0, funky.size());
    }


    /*
    @Test
    public void testPlayRememberedCardsLand() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);

        addCards("Mountain", 2, p);
        Card abbot = addCardToZone("Abbot of Keral Keep", p, ZoneType.Hand);
        addCardToZone("Lightning Bolt", p, ZoneType.Hand);
        // Note: This assumes the top of library is revealed. If the AI is made
        // smarter to not assume that, then this test can be updated to have
        // something that reveals top of library active - e.g. Lens of Clarity.
        addCardToZone("Mountain", p, ZoneType.Library);

        game.getPhaseHandler().devModeSet(PhaseType.MAIN2, p);
        game.getAction().checkStateEffects(true);

        // Expected plan:
        // 1. Play Abbot.
        // 2. Play land exiled by Abbot.
        // 3. Play Bolt targeting opponent.
        SpellAbilityPicker picker = new SpellAbilityPicker(game, p);
        SpellAbility sa = picker.chooseSpellAbilityToPlay(null);
        AssertJUnit.assertEquals(abbot.getSpellAbilities().get(0), sa);
        Plan plan = picker.getPlan();
        AssertJUnit.assertEquals(3, plan.getDecisions().size());
        AssertJUnit.assertEquals("Mountain (5) -> Play land by Abbot of Keral Keep (3)",
                plan.getDecisions().get(1).saRef.toString(true));
        AssertJUnit.assertEquals("Lightning Bolt deals 3 damage to any target.",
                plan.getDecisions().get(2).saRef.toString());
    }

    @Test
    public void testPlayRememberedCardsSpell() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);

        addCards("Mountain", 3, p);
        Card abbot = addCardToZone("Abbot of Keral Keep", p, ZoneType.Hand);
        // Note: This assumes the top of library is revealed. If the AI is made
        // smarter to not assume that, then this test can be updated to have
        // something that reveals top of library active - e.g. Lens of Clarity.
        addCardToZone("Lightning Bolt", p, ZoneType.Library);

        game.getPhaseHandler().devModeSet(PhaseType.MAIN2, p);
        game.getAction().checkStateEffects(true);

        // Expected plan:
        // 1. Play Abbot.
        // 3. Play Bolt exiled by Abbot.
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
        addCards("Swamp", 2, p);
        addCardToZone("Doom Blade", p, ZoneType.Hand);

        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, p);
        game.getAction().checkStateEffects(true);

        SpellAbilityPicker picker = new SpellAbilityPicker(game, p);
        SpellAbility sa = picker.chooseSpellAbilityToPlay(null);
        AssertJUnit.assertNotNull(sa);
        AssertJUnit.assertEquals("Destroy target nonblack creature.", sa.toString());
        AssertJUnit.assertEquals(blocker, sa.getTargetCard());
    }

    // Run the test 100 times to ensure there's no flakiness.
    @Test(invocationCount = 100)
    public void testChoicesResultingFromRandomEffects() {
        // Sometimes, the effect of a spell can be random, and as a result of that, new choices
        // could be selected during simulation. This test verifies that this doesn't cause problems.
        //
        // Note: The current implementation works around the issue by setting a consistent random
        // seed during choice evaluation, although in the future, it may make sense to handle it
        // some other way.

        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);
        Player opponent = game.getPlayers().get(0);

        addCardToZone("Chaos Warp", p, ZoneType.Hand);
        addCards("Mountain", 3, p);

        addCard("Plains", opponent);
        addCard("Mountain", opponent);
        addCard("Forest", opponent);
        // Use a card that is worthwhile to target even if the shuffle ends up choosing it
        // again. In this case, life loss on ETB and leaving.
        Card expectedTarget = addCard("Raving Oni-Slave", opponent);

        addCardToZone("Chaos Warp", opponent, ZoneType.Library);
        addCardToZone("Island", opponent, ZoneType.Library);
        addCardToZone("Swamp", opponent, ZoneType.Library);
        // The presence of Pilgrim's Eye in the library is important for this test, as this
        // will result in sub-choices (which land to pick) if this card ends up being the top
        // of the library during simulation.
        addCardToZone("Pilgrim's Eye", opponent, ZoneType.Library);

        game.getPhaseHandler().devModeSet(PhaseType.MAIN2, p);
        game.getAction().checkStateEffects(true);

        SpellAbilityPicker picker = new SpellAbilityPicker(game, p);
        SpellAbility sa = picker.chooseSpellAbilityToPlay(null);
        AssertJUnit.assertNotNull(sa);
        AssertJUnit.assertEquals("Chaos Warp", sa.getHostCard().getName());
        AssertJUnit.assertEquals(expectedTarget, sa.getTargetCard());
    }

    @Test
    public void testNoSimulationsWhenNoTargets() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);

        addCards("Forest", 2, p);
        addCardToZone("Counterspell", p, ZoneType.Hand);
        addCardToZone("Unsummon", p, ZoneType.Hand);

        game.getPhaseHandler().devModeSet(PhaseType.MAIN2, p);
        game.getAction().checkStateEffects(true);

        SpellAbilityPicker picker = new SpellAbilityPicker(game, p);
        SpellAbility sa = picker.chooseSpellAbilityToPlay(null);
        AssertJUnit.assertNull(sa);
        AssertJUnit.assertEquals(0, picker.getNumSimulations());
    }

    @Test
    public void testSpellCantTargetSelf() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);
        Player opponent = game.getPlayers().get(0);

        addCardToZone("Unsubstantiate", p, ZoneType.Hand);
        addCard("Forest", p);
        addCard("Island", p);
        Card expectedTarget = addCard("Flying Men", opponent);

        game.getPhaseHandler().devModeSet(PhaseType.MAIN2, p);
        game.getAction().checkStateEffects(true);

        SpellAbilityPicker picker = new SpellAbilityPicker(game, p);
        SpellAbility sa = picker.chooseSpellAbilityToPlay(null);
        AssertJUnit.assertNotNull(sa);
        AssertJUnit.assertEquals(expectedTarget, sa.getTargetCard());
        // Only a single simulation expected (no target self).
        AssertJUnit.assertEquals(1, picker.getNumSimulations());
    }

    @Test
    public void testModalSpellCantTargetSelf() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);
        Player opponent = game.getPlayers().get(0);

        addCardToZone("Decisive Denial", p, ZoneType.Hand);
        addCard("Forest", p);
        addCard("Island", p);
        addCard("Runeclaw Bear", p);
        addCard("Flying Men", opponent);

        game.getPhaseHandler().devModeSet(PhaseType.MAIN2, p);
        game.getAction().checkStateEffects(true);

        SpellAbilityPicker picker = new SpellAbilityPicker(game, p);
        SpellAbility sa = picker.chooseSpellAbilityToPlay(null);
        AssertJUnit.assertNotNull(sa);
        // Expected: Runeclaw Bear fights Flying Men
        // Only a single simulation expected (no target self).
        AssertJUnit.assertEquals(1, picker.getNumSimulations());
    }

    @Test
    public void testModalSpellNoTargetsForModeWithSubAbility() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);

        addCardToZone("Temur Charm", p, ZoneType.Hand);
        addCard("Forest", p);
        addCard("Island", p);
        addCard("Mountain", p);

        game.getPhaseHandler().devModeSet(PhaseType.MAIN2, p);
        game.getAction().checkStateEffects(true);

        SpellAbilityPicker picker = new SpellAbilityPicker(game, p);
         picker.chooseSpellAbilityToPlay(null);
        // Only mode "Creatures with power 3 or less can’t block this turn" should be simulated.
        AssertJUnit.assertEquals(1, picker.getNumSimulations());
    }

    @Test
    public void testModalSpellNoTargetsForAnyModes() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);

        addCardToZone("Drown in the Loch", p, ZoneType.Hand);
        addCard("Swamp", p);
        addCard("Island", p);

        game.getPhaseHandler().devModeSet(PhaseType.MAIN2, p);
        game.getAction().checkStateEffects(true);

        SpellAbilityPicker picker = new SpellAbilityPicker(game, p);
        picker.chooseSpellAbilityToPlay(null);
        // TODO: Ideally, this would be 0 simulations, but we currently only determine there are no
        // valid modes in SpellAbilityChoicesIterator, which runs already when we're simulating.
        // Still, this test case exercises the code path and ensures we don't crash in this case.
        AssertJUnit.assertEquals(1, picker.getNumSimulations());
    }

    @Test
    public void threeDistinctTargetSpell() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);
        Player opponent = game.getPlayers().get(0);

        addCardToZone("Incremental Growth", p, ZoneType.Hand);
        addCards("Forest", 5, p);
        addCard("Forest Bear", p);
        addCard("Flying Men", opponent);
        addCard("Runeclaw Bear", p);
        addCard("Water Elemental", opponent);
        addCard("Grizzly Bears", p);

        game.getPhaseHandler().devModeSet(PhaseType.MAIN2, p);
        game.getAction().checkStateEffects(true);
        SpellAbilityPicker picker = new SpellAbilityPicker(game, p);
        SpellAbility sa = picker.chooseSpellAbilityToPlay(null);
        AssertJUnit.assertNotNull(sa);
        MultiTargetSelector.Targets targets = picker.getPlan().getSelectedDecision().targets;
        AssertJUnit.assertEquals(3, targets.size());
        AssertJUnit.assertTrue(targets.toString().contains("Forest Bear"));
        AssertJUnit.assertTrue(targets.toString().contains("Runeclaw Bear"));
        AssertJUnit.assertTrue(targets.toString().contains("Grizzly Bear"));
        // Expected 5*4*3=60 iterations (5 choices for first target, 4 for next, 3 for last.)
        AssertJUnit.assertEquals(60, picker.getNumSimulations());
    }

    @Test
    public void threeDistinctTargetSpellCantBeCast() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);
        Player opponent = game.getPlayers().get(0);

        addCardToZone("Incremental Growth", p, ZoneType.Hand);
        addCards("Forest", 5, p);
        addCard("Forest Bear", p);
        addCard("Flying Men", opponent);

        game.getPhaseHandler().devModeSet(PhaseType.MAIN2, p);
        game.getAction().checkStateEffects(true);
        SpellAbilityPicker picker = new SpellAbilityPicker(game, p);
        SpellAbility sa = picker.chooseSpellAbilityToPlay(null);
        AssertJUnit.assertNull(sa);
    }

    @Test
    public void correctTargetChoicesWithTwoTargetSpell() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);
        Player opponent = game.getPlayers().get(0);

        addCardToZone("Rites of Reaping", p, ZoneType.Hand);
        addCard("Swamp", p);
        addCards("Forest", 5, p);
        addCard("Flying Men", opponent);
        addCard("Forest Bear", p);
        addCard("Water Elemental", opponent);

        game.getPhaseHandler().devModeSet(PhaseType.MAIN2, p);
        game.getAction().checkStateEffects(true);
        SpellAbilityPicker picker = new SpellAbilityPicker(game, p);
        SpellAbility sa = picker.chooseSpellAbilityToPlay(null);
        AssertJUnit.assertNotNull(sa);
        MultiTargetSelector.Targets targets = picker.getPlan().getSelectedDecision().targets;
        AssertJUnit.assertEquals(2, targets.size());
        AssertJUnit.assertTrue(targets.toString().contains("Forest Bear"));
        AssertJUnit.assertTrue(targets.toString().contains("Flying Men"));
    }
    */
}
