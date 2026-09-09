package forge.ai.ability;

import forge.ai.AITest;
import forge.ai.AiAbilityDecision;
import forge.ai.AiPlayDecision;
import forge.ai.ComputerUtil;
import forge.ai.PlayerControllerAi;
import forge.ai.SpellAbilityAi;
import forge.ai.SpellApiToAi;
import forge.game.Game;
import forge.game.ability.ApiType;
import forge.game.card.Card;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;
import org.testng.AssertJUnit;
import org.testng.annotations.Test;

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
        Game game = initAndCreateThreePlayerGame(useSimulation);
        Player victim = game.getPlayers().get(0);
        Player ai = game.getPlayers().get(1);
        Player caster = game.getPlayers().get(2);

        victim.setTeam(0);
        ai.setTeam(1);
        caster.setTeam(2);

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
        Game game = initAndCreateThreePlayerGame(useSimulation);
        Player victim = game.getPlayers().get(0);
        Player ai = game.getPlayers().get(1);
        Player caster = game.getPlayers().get(2);

        victim.setTeam(0);
        ai.setTeam(1);
        caster.setTeam(2);

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
        Player caster = game.getPlayers().get(0);
        Player ai = game.getPlayers().get(1);
        Player otherOpponent = game.getPlayers().get(2);

        caster.setTeam(0);
        ai.setTeam(1);
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

    @Test
    public void testXTargetCountIgnoresTheAisOwnPermanents() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opponent = game.getPlayers().get(0);

        addCards("Island", 12, ai);
        // Distorting Wake targets any nonland permanent, so everything here is a legal target, but
        // the AI only wants the opponent's. Its own used to inflate X past the number it would
        // actually target, leaving it unable to cast the spell at all.
        addCard("Ornithopter", ai);
        addCard("Sol Ring", ai);
        addCard("Runeclaw Bear", ai);

        addCard("Colossal Dreadmaw", opponent);
        addCard("Ancient Brontodon", opponent);

        Card wake = addCardToZone("Distorting Wake", ai, ZoneType.Hand);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN2, ai);
        game.getAction().checkStateEffects(true);

        SpellAbility sa = null;
        for (SpellAbility s : wake.getSpellAbilities()) {
            if (s.getApi() == ApiType.ChangeZone) {
                sa = s;
                break;
            }
        }
        AssertJUnit.assertNotNull("Distorting Wake should have a ChangeZone spell ability", sa);
        sa.setActivatingPlayer(ai);

        AssertJUnit.assertTrue("AI should still cast an X-targeting bounce while it controls permanents",
                SpellApiToAi.Converter.get(sa).canPlayWithSubs(ai, sa).willingToPlay());
        AssertJUnit.assertEquals("X should match the permanents the AI actually wants to target",
                Integer.valueOf(2), sa.getXManaCostPaid());
        for (Card t : sa.getTargets().getTargetCards()) {
            AssertJUnit.assertFalse("AI should never target its own permanent with this",
                    t.getController().equals(ai));
        }
    }

    @Test
    public void testExhumeWithEmptyOpponentGraveyard() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);

        addCards("Swamp", 2, ai);
        addCardToZone("Akroma, Angel of Wrath", ai, ZoneType.Graveyard);
        Card exhume = addCardToZone("Exhume", ai, ZoneType.Hand);
        SpellAbility sa = exhume.getSpellAbilities().get(0);
        sa.setActivatingPlayer(ai);

        AssertJUnit.assertTrue("AI should cast Exhume when only its graveyard has a creature",
                SpellApiToAi.Converter.get(sa).canPlayWithSubs(ai, sa).willingToPlay());
    }

    @Test
    public void testExhumeRequiresAnOwnCreature() {
        for (String ownCard : new String[] {null, "Swamp"}) {
            Game game = initAndCreateGame();
            Player ai = game.getPlayers().get(1);
            Player opponent = game.getPlayers().get(0);

            addCards("Swamp", 2, ai);
            if (ownCard != null) {
                addCardToZone(ownCard, ai, ZoneType.Graveyard);
            }
            addCardToZone("Grizzly Bears", opponent, ZoneType.Graveyard);
            Card exhume = addCardToZone("Exhume", ai, ZoneType.Hand);
            SpellAbility sa = exhume.getSpellAbilities().get(0);
            sa.setActivatingPlayer(ai);

            AssertJUnit.assertFalse("AI should not cast Exhume without an own creature",
                    ((PlayerControllerAi) ai.getController()).getAi().canPlaySa(sa) == AiPlayDecision.WillPlay);
        }
    }

}
