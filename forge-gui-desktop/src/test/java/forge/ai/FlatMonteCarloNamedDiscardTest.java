package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.game.Game;
import forge.game.ability.ApiType;
import forge.game.card.Card;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/**
 * Cabal Therapy ("AILogic$ NamedDiscard"): when the AI casts it and which card it names. Kept in a class of its own.
 */
public class FlatMonteCarloNamedDiscardTest extends AITest {

    private static void aimDiscardAt(final SpellAbility sa, final Player target) {
        for (SpellAbility a = sa; a != null; a = a.getSubAbility()) {
            if (a.getApi() == ApiType.Discard && a.usesTargeting()) {
                a.resetTargets();
                a.getTargets().add(target);
            }
        }
    }

    private SpellAbility setUp(final Game game, final Player ai) {
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        addCard("Swamp", ai);
        final SpellAbility sa = addCardToZone("Cabal Therapy", ai, ZoneType.Hand).getFirstSpellAbility();
        sa.setActivatingPlayer(ai);
        return sa;
    }

    @Test
    public void bridgeFromBelowInHandIsNamedAgainstItself() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        final SpellAbility sa = setUp(game, ai);
        addCardToZone("Bridge from Below", ai, ZoneType.Hand);
        addCardToZone("Lightning Bolt", ai, ZoneType.Hand);
        aimDiscardAt(sa, ai);
        AssertJUnit.assertEquals("Bridge from Below", NamedDiscard.chooseName(ai, sa, "Card.nonLand"));
        AssertJUnit.assertTrue(NamedDiscard.consider(ai, sa).willingToPlay());
    }

    @Test
    public void aCardWithDiscardMeIsNamedAgainstItself() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        final SpellAbility sa = setUp(game, ai);
        final Card fodder = addCardToZone("Lightning Bolt", ai, ZoneType.Hand);
        fodder.setSVar("DiscardMe", "5"); // no dependence on which scripts carry the SVar
        addCardToZone("Brainstorm", ai, ZoneType.Hand);
        aimDiscardAt(sa, ai);
        AssertJUnit.assertEquals("Lightning Bolt", NamedDiscard.chooseName(ai, sa, "Card.nonLand"));
    }

    @Test
    public void anOrdinaryHandIsNotThrownAwayByItself() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        final SpellAbility sa = setUp(game, ai);
        addCardToZone("Lightning Bolt", ai, ZoneType.Hand);
        addCardToZone("Brainstorm", ai, ZoneType.Hand);
        aimDiscardAt(sa, ai);
        AssertJUnit.assertEquals("", NamedDiscard.chooseName(ai, sa, "Card.nonLand"));
    }

    @Test
    public void aCardSeenRevealedInTheHandIsTheOneNamed() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        final Player opp = game.getPlayers().get(0);
        final SpellAbility sa = setUp(game, ai);
        final Card bolt = addCardToZone("Lightning Bolt", opp, ZoneType.Hand);
        addCardToZone("Lightning Bolt", opp, ZoneType.Hand);
        addCardToZone("Island", opp, ZoneType.Hand);
        for (int i = 0; i < 4; i++) {
            addCardToZone("Counterspell", opp, ZoneType.Library);
        }
        AiCardMemory.rememberCard(ai, bolt, AiCardMemory.MemorySet.REVEALED_CARDS);
        aimDiscardAt(sa, opp);
        AssertJUnit.assertEquals("Lightning Bolt", NamedDiscard.chooseName(ai, sa, "Card.nonLand"));
        AssertJUnit.assertTrue(NamedDiscard.consider(ai, sa).willingToPlay());
    }

    @Test
    public void withNothingSeenTheMostCommonNonlandCardOfTheirDeckIsNamed() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        final Player opp = game.getPlayers().get(0);
        final SpellAbility sa = setUp(game, ai);
        addCardToZone("Brainstorm", opp, ZoneType.Hand);
        addCardToZone("Island", opp, ZoneType.Hand);
        for (int i = 0; i < 4; i++) {
            addCardToZone("Counterspell", opp, ZoneType.Library);
        }
        for (int i = 0; i < 2; i++) {
            addCardToZone("Brainstorm", opp, ZoneType.Library);
        }
        for (int i = 0; i < 6; i++) {
            addCardToZone("Island", opp, ZoneType.Library);
        }
        aimDiscardAt(sa, opp);
        AssertJUnit.assertEquals("Counterspell", NamedDiscard.chooseName(ai, sa, "Card.nonLand"));
    }

    @Test
    public void anEmptyOrSingleCardHandIsNotWorthIt() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        final Player opp = game.getPlayers().get(0);
        final SpellAbility sa = setUp(game, ai);
        AssertJUnit.assertFalse(NamedDiscard.consider(ai, sa).willingToPlay());
        addCardToZone("Brainstorm", opp, ZoneType.Hand);
        AssertJUnit.assertFalse(NamedDiscard.consider(ai, sa).willingToPlay());
        addCardToZone("Lightning Bolt", opp, ZoneType.Hand);
        AssertJUnit.assertTrue(NamedDiscard.consider(ai, sa).willingToPlay());
    }

    @Test
    public void theHeuristicsNowApproveCabalTherapyAgainstAHandOfSeveralCards() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        final Player opp = game.getPlayers().get(0);
        final SpellAbility sa = setUp(game, ai);
        addCardToZone("Brainstorm", opp, ZoneType.Hand);
        addCardToZone("Lightning Bolt", opp, ZoneType.Hand);
        addCardToZone("Island", opp, ZoneType.Hand);
        final AiController ctrl = ((PlayerControllerAi) ai.getController()).getAi();
        AssertJUnit.assertEquals(AiPlayDecision.WillPlay, ctrl.canPlaySa(sa));
    }

    @Test
    public void theNameTheAiChoseIsInTheLogAtTheDefaultVerbosity() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        final SpellAbility sa = setUp(game, ai);
        addCardToZone("Bridge from Below", ai, ZoneType.Hand);
        addCardToZone("Lightning Bolt", ai, ZoneType.Hand);
        aimDiscardAt(sa, ai);
        sa.getApi().getSpellEffect().resolve(sa); // the NameCard part of Cabal Therapy
        boolean logged = false;
        for (final forge.game.GameLogEntry e : game.getGameLog().getLogEntriesForVerbosity(forge.game.GameLogVerbosity.MEDIUM)) {
            logged |= e.message().contains("names Bridge from Below");
        }
        AssertJUnit.assertTrue("the chosen name must be in the Medium log", logged);
    }

    @Test
    public void cabalTherapyInHandDoesNotMakeTheAiAimItAtItself() {
        // flashback makes the Therapy in hand look like a card that wants to be in the graveyard; naming it discards nothing
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        final SpellAbility sa = setUp(game, ai);
        addCardToZone("Lightning Bolt", ai, ZoneType.Hand);
        final forge.ai.simulation.ComboPlan plan = ((PlayerControllerAi) ai.getController()).getAi().deckPlan(ai).combos;
        AssertJUnit.assertFalse(plan.selfDiscardWanted(ai, sa.getHostCard()));
        AssertJUnit.assertNull(plan.selfDiscardName(ai, sa.getHostCard()));
    }

    @Test
    public void aBlindGuessNamesAnImportantNonlandCardOfTheOpponent() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        final Player opp = game.getPlayers().get(0);
        final SpellAbility sa = setUp(game, ai);
        for (int i = 0; i < 6; i++) {
            addCardToZone("Island", opp, ZoneType.Library);
        }
        addCardToZone("Thassa's Oracle", opp, ZoneType.Library);
        addCardToZone("Balustrade Spy", opp, ZoneType.Library);
        addCardToZone("Balustrade Spy", opp, ZoneType.Library);
        addCardToZone("Island", opp, ZoneType.Hand);
        addCardToZone("Island", opp, ZoneType.Hand);
        aimDiscardAt(sa, opp);
        final String name = NamedDiscard.chooseName(ai, sa, "Card.nonLand");
        AssertJUnit.assertTrue("named '" + name + "'", name.equals("Thassa's Oracle") || name.equals("Balustrade Spy"));
    }

    @Test
    public void theGuardRefusesTherapyAimedAtTheAiWithNothingToNameButNotAtTheOpponent() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        final Player opp = game.getPlayers().get(0);
        final SpellAbility sa = setUp(game, ai);
        addCardToZone("Lightning Bolt", ai, ZoneType.Hand);
        final forge.ai.simulation.ComboPlan plan = ((PlayerControllerAi) ai.getController()).getAi().deckPlan(ai).combos;
        aimDiscardAt(sa, ai);
        AssertJUnit.assertEquals("named-discard-self", forge.ai.simulation.ComboGuard.problem(ai, sa, null, plan));
        aimDiscardAt(sa, opp);
        AssertJUnit.assertNull(forge.ai.simulation.ComboGuard.problem(ai, sa, null, plan));
    }
}
