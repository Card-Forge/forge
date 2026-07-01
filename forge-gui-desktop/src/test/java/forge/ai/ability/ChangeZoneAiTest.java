package forge.ai.ability;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.ai.AITest;
import forge.ai.AiAbilityDecision;
import forge.ai.ComputerUtil;
import forge.ai.SpellAbilityAi;
import forge.ai.SpellApiToAi;
import forge.game.Game;
import forge.game.ability.ApiType;
import forge.game.card.Card;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

public class ChangeZoneAiTest extends AITest {
    private static final String GENERIC_BOUNCE_SPELL = "Unsummon";
    private static final String GENERIC_TEST_CREATURE = "Timber Wolves";
    private static final String THREATENING_CHANGE_ZONE_SPELL = "Commit";

    private AiAbilityDecision getChangeZoneDecision(Player ai, Card changeZoneCard) {
        SpellAbility changeZoneSa = changeZoneCard.getFirstSpellAbility();
        changeZoneSa.setActivatingPlayer(ai);
        SpellAbilityAi changeZoneAi = SpellApiToAi.Converter.get(ApiType.ChangeZone);
        return changeZoneAi.canPlayWithSubs(ai, changeZoneSa);
    }

    private void runUnsummonDoesNotSaveOpponentCreatureFromCommitTest(boolean useSimulation) {
        Game game = initAndCreateThreePlayerGame(useSimulation, 2, "victim", "caster", "ai");
        Player victim = game.getPlayers().get(0);
        Player caster = game.getPlayers().get(1);
        Player ai = game.getPlayers().get(2);

        victim.setTeam(0);
        caster.setTeam(1);
        ai.setTeam(2);

        Card victimCreature = addCard(GENERIC_TEST_CREATURE, victim);
        addCards("Island", 4, caster);
        addCards("Island", 1, ai);

        Card commit = addCardToZone(THREATENING_CHANGE_ZONE_SPELL, caster, ZoneType.Hand);
        Card unsummon = addCardToZone(GENERIC_BOUNCE_SPELL, ai, ZoneType.Hand);

        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, caster);
        game.getAction().checkStateEffects(true);

        SpellAbility commitSa = commit.getFirstSpellAbility();
        commitSa.setActivatingPlayer(caster);
        commitSa.getTargets().add(victimCreature);
        AssertJUnit.assertTrue(ComputerUtil.handlePlayingSpellAbility(caster, commitSa, null));

        AiAbilityDecision decision = getChangeZoneDecision(ai, unsummon);
        AssertJUnit.assertFalse("AI should not bounce an opponent's creature to save it from another opponent's Commit.",
                decision.willingToPlay());
    }

    private void runUnsummonDoesNotRedundantlyBounceCommanderTest(boolean useSimulation) {
        Game game = initAndCreateThreePlayerGame(useSimulation, 2, "victim", "caster", "ai");
        Player victim = game.getPlayers().get(0);
        Player caster = game.getPlayers().get(1);
        Player ai = game.getPlayers().get(2);

        victim.setTeam(0);
        caster.setTeam(1);
        ai.setTeam(2);

        Card victimCommander = addCard(GENERIC_TEST_CREATURE, victim);
        victimCommander.setCommander(true);
        addCards("Island", 1, caster);
        addCards("Island", 1, ai);

        Card firstUnsummon = addCardToZone(GENERIC_BOUNCE_SPELL, caster, ZoneType.Hand);
        Card secondUnsummon = addCardToZone(GENERIC_BOUNCE_SPELL, ai, ZoneType.Hand);

        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, caster);
        game.getAction().checkStateEffects(true);

        SpellAbility firstUnsummonSa = firstUnsummon.getFirstSpellAbility();
        firstUnsummonSa.setActivatingPlayer(caster);
        firstUnsummonSa.getTargets().add(victimCommander);
        AssertJUnit.assertTrue(ComputerUtil.handlePlayingSpellAbility(caster, firstUnsummonSa, null));

        AiAbilityDecision decision = getChangeZoneDecision(ai, secondUnsummon);
        AssertJUnit.assertFalse("AI should not redundantly bounce an opponent's commander already targeted by Unsummon.",
                decision.willingToPlay());
    }

    @Test
    public void testUnsummonDoesNotSaveOpponentCreatureFromCommit() {
        runUnsummonDoesNotSaveOpponentCreatureFromCommitTest(false);
    }

    @Test
    public void testUnsummonDoesNotSaveOpponentCreatureFromCommitWithSimulation() {
        runUnsummonDoesNotSaveOpponentCreatureFromCommitTest(true);
    }

    @Test
    public void testUnsummonStillSavesOwnCreatureFromCommit() {
        Game game = initAndCreateThreePlayerGame();
        Player ai = game.getPlayers().get(0);
        Player caster = game.getPlayers().get(1);
        Player otherOpponent = game.getPlayers().get(2);

        ai.setTeam(0);
        caster.setTeam(1);
        otherOpponent.setTeam(2);

        Card aiCreature = addCard(GENERIC_TEST_CREATURE, ai);
        addCards("Island", 4, caster);
        addCards("Island", 1, ai);
        addCard(GENERIC_TEST_CREATURE, otherOpponent);

        Card commit = addCardToZone(THREATENING_CHANGE_ZONE_SPELL, caster, ZoneType.Hand);
        Card unsummon = addCardToZone(GENERIC_BOUNCE_SPELL, ai, ZoneType.Hand);

        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, caster);
        game.getAction().checkStateEffects(true);

        SpellAbility commitSa = commit.getFirstSpellAbility();
        commitSa.setActivatingPlayer(caster);
        commitSa.getTargets().add(aiCreature);
        AssertJUnit.assertTrue(ComputerUtil.handlePlayingSpellAbility(caster, commitSa, null));

        SpellAbility unsummonSa = unsummon.getFirstSpellAbility();
        unsummonSa.setActivatingPlayer(ai);
        SpellAbilityAi unsummonAi = SpellApiToAi.Converter.get(ApiType.ChangeZone);
        AiAbilityDecision decision = unsummonAi.canPlayWithSubs(ai, unsummonSa);

        AssertJUnit.assertTrue("AI should still bounce its own creature to save it from Commit.",
                decision.willingToPlay());
        AssertJUnit.assertEquals(aiCreature, unsummonSa.getTargetCard());
    }

    @Test
    public void testUnsummonDoesNotRedundantlyBounceOpponentsCommanderAlreadyTargetedByUnsummon() {
        runUnsummonDoesNotRedundantlyBounceCommanderTest(false);
    }

    @Test
    public void testUnsummonDoesNotRedundantlyBounceOpponentsCommanderAlreadyTargetedByUnsummonWithSimulation() {
        runUnsummonDoesNotRedundantlyBounceCommanderTest(true);
    }
}
