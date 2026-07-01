package forge.ai.ability;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.ai.AITest;
import forge.ai.AiAbilityDecision;
import forge.ai.ComputerUtil;
import forge.ai.SpellAbilityAi;
import forge.ai.SpellApiToAi;
import forge.game.Game;
import forge.game.GameSnapshot;
import forge.game.ability.ApiType;
import forge.game.card.Card;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

public class CounterAiTest extends AITest {
    private static final String GENERIC_COUNTERSPELL = "Counterspell";
    private static final String GENERIC_BOUNCE_SPELL = "Unsummon";
    private static final String GENERIC_TEST_CREATURE = "Timber Wolves";
    private static final String GENERIC_SWEEPER = "Wrath of God";

    private AiAbilityDecision getCounterDecision(Player ai, Card counterCard) {
        SpellAbility counterSa = counterCard.getFirstSpellAbility();
        counterSa.setActivatingPlayer(ai);
        SpellAbilityAi counterAi = SpellApiToAi.Converter.get(ApiType.Counter);
        return counterAi.canPlayWithSubs(ai, counterSa);
    }

    private void runOpponentCounteringAnotherOpponentTest(boolean useSimulation) {
        Game game = initAndCreateThreePlayerGame(useSimulation);
        Player spellCaster = game.getPlayers().get(0);
        Player ai = game.getPlayers().get(1);
        Player opposingCounterPlayer = game.getPlayers().get(2);

        spellCaster.setTeam(0);
        ai.setTeam(1);
        opposingCounterPlayer.setTeam(2);

        addCards("Plains", 4, spellCaster);
        addCards("Island", 2, ai);
        addCards("Island", 2, opposingCounterPlayer);

        Card wrathOfGod = addCardToZone(GENERIC_SWEEPER, spellCaster, ZoneType.Hand);
        Card counterspellFromOpponent = addCardToZone(GENERIC_COUNTERSPELL, opposingCounterPlayer, ZoneType.Hand);
        Card counterspellFromAi = addCardToZone(GENERIC_COUNTERSPELL, ai, ZoneType.Hand);

        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, spellCaster);
        game.getAction().checkStateEffects(true);

        SpellAbility wrathSa = wrathOfGod.getFirstSpellAbility();
        wrathSa.setActivatingPlayer(spellCaster);
        AssertJUnit.assertTrue(ComputerUtil.handlePlayingSpellAbility(spellCaster, wrathSa, null));

        SpellAbility opposingCounterSa = counterspellFromOpponent.getFirstSpellAbility();
        opposingCounterSa.setActivatingPlayer(opposingCounterPlayer);
        opposingCounterSa.getTargets().add(wrathSa);
        AssertJUnit.assertTrue(ComputerUtil.handlePlayingSpellAbility(opposingCounterPlayer, opposingCounterSa, null));

        AiAbilityDecision decision = getCounterDecision(ai, counterspellFromAi);
        AssertJUnit.assertFalse("AI should avoid countering an opponent's counterspell aimed at another opponent.",
                decision.willingToPlay());
    }

    private void runCounterspellCounterWarTest(boolean useSimulation) {
        Game game = initAndCreateThreePlayerGame(useSimulation);
        Player spellCaster = game.getPlayers().get(0);
        Player ai = game.getPlayers().get(1);
        Player counterCaster = game.getPlayers().get(2);

        spellCaster.setTeam(0);
        ai.setTeam(1);
        counterCaster.setTeam(2);

        addCards("Forest", 1, spellCaster);
        addCards("Island", 2, ai);
        addCards("Island", 2, counterCaster);

        Card creatureSpell = addCardToZone(GENERIC_TEST_CREATURE, spellCaster, ZoneType.Hand);
        Card counterspell = addCardToZone(GENERIC_COUNTERSPELL, counterCaster, ZoneType.Hand);
        Card aiCounterspell = addCardToZone(GENERIC_COUNTERSPELL, ai, ZoneType.Hand);

        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, spellCaster);
        game.getAction().checkStateEffects(true);

        SpellAbility creatureSa = creatureSpell.getFirstSpellAbility();
        creatureSa.setActivatingPlayer(spellCaster);
        AssertJUnit.assertTrue(ComputerUtil.handlePlayingSpellAbility(spellCaster, creatureSa, null));

        SpellAbility counterspellSa = counterspell.getFirstSpellAbility();
        counterspellSa.setActivatingPlayer(counterCaster);
        counterspellSa.getTargets().add(creatureSa);
        AssertJUnit.assertTrue(ComputerUtil.handlePlayingSpellAbility(counterCaster, counterspellSa, null));

        AiAbilityDecision decision = getCounterDecision(ai, aiCounterspell);
        AssertJUnit.assertFalse("AI should not use Counterspell to counter another opponent's Counterspell.",
                decision.willingToPlay());
    }

    private void runCounterspellIgnoresBounceAimedAtOtherOpponentTest(boolean useSimulation, boolean commanderTarget) {
        Game game = initAndCreateThreePlayerGame(useSimulation);
        Player bounceCaster = game.getPlayers().get(0);
        Player ai = game.getPlayers().get(1);
        Player victim = game.getPlayers().get(2);

        bounceCaster.setTeam(0);
        ai.setTeam(1);
        victim.setTeam(2);

        addCards("Island", 1, bounceCaster);
        addCards("Island", 2, ai);

        Card victimPermanent = addCard(GENERIC_TEST_CREATURE, victim);
        victimPermanent.setCommander(commanderTarget);
        Card unsummon = addCardToZone(GENERIC_BOUNCE_SPELL, bounceCaster, ZoneType.Hand);
        Card counterspell = addCardToZone(GENERIC_COUNTERSPELL, ai, ZoneType.Hand);

        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, bounceCaster);
        game.getAction().checkStateEffects(true);

        SpellAbility unsummonSa = unsummon.getFirstSpellAbility();
        unsummonSa.setActivatingPlayer(bounceCaster);
        unsummonSa.getTargets().add(victimPermanent);
        AssertJUnit.assertTrue(ComputerUtil.handlePlayingSpellAbility(bounceCaster, unsummonSa, null));

        AiAbilityDecision decision = getCounterDecision(ai, counterspell);
        AssertJUnit.assertFalse("AI should not counter Unsummon when it only targets another opponent's creature.",
                decision.willingToPlay());
    }

    @Test
    public void testCounterspellIgnoresRemovalAimedAtOtherOpponent() {
        Game game = initAndCreateThreePlayerGame(true);
        Player caster = game.getPlayers().get(0);
        Player ai = game.getPlayers().get(1);
        Player victim = game.getPlayers().get(2);

        caster.setTeam(0);
        ai.setTeam(1);
        victim.setTeam(2);

        addCards("Swamp", 2, caster);
        addCards("Island", 2, ai);

        Card victimCreature = addCard(GENERIC_TEST_CREATURE, victim);
        Card doomBlade = addCardToZone("Doom Blade", caster, ZoneType.Hand);
        Card counterspell = addCardToZone(GENERIC_COUNTERSPELL, ai, ZoneType.Hand);

        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, caster);
        game.getAction().checkStateEffects(true);

        SpellAbility removalSa = doomBlade.getFirstSpellAbility();
        removalSa.setActivatingPlayer(caster);
        removalSa.getTargets().add(victimCreature);
        AssertJUnit.assertTrue(ComputerUtil.handlePlayingSpellAbility(caster, removalSa, null));

        AiAbilityDecision decision = getCounterDecision(ai, counterspell);

        AssertJUnit.assertFalse("AI should avoid countering removal that only harms another opponent in multiplayer.",
                decision.willingToPlay());
    }

    @Test
    public void testCounterspellStillCountersBoardWipeThreateningAi() {
        Game game = initAndCreateThreePlayerGame(true);
        Player caster = game.getPlayers().get(0);
        Player ai = game.getPlayers().get(1);
        Player otherOpponent = game.getPlayers().get(2);

        caster.setTeam(0);
        ai.setTeam(1);
        otherOpponent.setTeam(2);

        addCards("Plains", 4, caster);
        addCards("Island", 2, ai);

        addCard(GENERIC_TEST_CREATURE, ai);
        Card wrathOfGod = addCardToZone(GENERIC_SWEEPER, caster, ZoneType.Hand);
        Card counterspell = addCardToZone(GENERIC_COUNTERSPELL, ai, ZoneType.Hand);

        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, caster);
        game.getAction().checkStateEffects(true);

        SpellAbility sweeperSa = wrathOfGod.getFirstSpellAbility();
        sweeperSa.setActivatingPlayer(caster);
        AssertJUnit.assertTrue(ComputerUtil.handlePlayingSpellAbility(caster, sweeperSa, null));

        AiAbilityDecision decision = getCounterDecision(ai, counterspell);

        AssertJUnit.assertTrue("AI should still counter a board wipe that threatens its own permanents.",
                decision.willingToPlay());
    }

    @Test
    public void testCounterspellIgnoresOpponentCounteringAnotherOpponent() {
        runOpponentCounteringAnotherOpponentTest(true);
    }

    @Test
    public void testCounterspellIgnoresOpponentCounteringAnotherOpponentWithoutSimulation() {
        runOpponentCounteringAnotherOpponentTest(false);
    }

    @Test
    public void testCounterspellIgnoresUnsummonOnAnotherOpponentsCreature() {
        runCounterspellIgnoresBounceAimedAtOtherOpponentTest(true, false);
    }

    @Test
    public void testCounterspellIgnoresUnsummonOnAnotherOpponentsCommander() {
        runCounterspellIgnoresBounceAimedAtOtherOpponentTest(true, true);
    }

    @Test
    public void testCounterspellIgnoresCounterspellCounterWarBetweenOpponents() {
        runCounterspellCounterWarTest(true);
    }

    @Test
    public void testCounterspellIgnoresCounterspellCounterWarBetweenOpponentsWithoutSimulation() {
        runCounterspellCounterWarTest(false);
    }

    @Test
    public void testGameSnapshotRestoresCounterspellTargetingSpellOnStack() {
        Game game = initAndCreateThreePlayerGame(true);
        Player spellCaster = game.getPlayers().get(0);
        Player ai = game.getPlayers().get(1);
        Player counterCaster = game.getPlayers().get(2);

        spellCaster.setTeam(0);
        ai.setTeam(1);
        counterCaster.setTeam(2);

        addCards("Forest", 1, spellCaster);
        addCards("Island", 2, counterCaster);

        Card timberWolves = addCardToZone(GENERIC_TEST_CREATURE, spellCaster, ZoneType.Hand);
        Card counterspell = addCardToZone(GENERIC_COUNTERSPELL, counterCaster, ZoneType.Hand);

        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, spellCaster);
        game.getAction().checkStateEffects(true);

        SpellAbility timberWolvesSa = timberWolves.getFirstSpellAbility();
        timberWolvesSa.setActivatingPlayer(spellCaster);
        AssertJUnit.assertTrue(ComputerUtil.handlePlayingSpellAbility(spellCaster, timberWolvesSa, null));

        SpellAbility counterspellSa = counterspell.getFirstSpellAbility();
        counterspellSa.setActivatingPlayer(counterCaster);
        counterspellSa.getTargets().add(timberWolvesSa);
        AssertJUnit.assertTrue(ComputerUtil.handlePlayingSpellAbility(counterCaster, counterspellSa, null));

        Game copiedGame = new GameSnapshot(game).makeCopy();
        SpellAbility copiedTopSa = copiedGame.getStack().peekAbility();

        AssertJUnit.assertEquals(GENERIC_COUNTERSPELL, copiedTopSa.getHostCard().getName());
        AssertJUnit.assertNotNull("Copied Counterspell should still target the spell it was countering.",
                copiedTopSa.getTargets().getFirstTargetedSpell());
        AssertJUnit.assertEquals(GENERIC_TEST_CREATURE,
                copiedTopSa.getTargets().getFirstTargetedSpell().getHostCard().getName());
    }
}
