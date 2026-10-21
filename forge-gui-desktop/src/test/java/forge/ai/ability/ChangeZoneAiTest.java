package forge.ai.ability;

import forge.ai.AITest;
import forge.ai.AiPlayDecision;
import forge.ai.ComputerUtil;
import forge.ai.PlayerControllerAi;
import forge.ai.SpellApiToAi;
import forge.game.Game;
import forge.game.ability.AbilityFactory;
import forge.game.ability.ApiType;
import forge.game.card.Card;
import forge.game.cost.Cost;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.AbilitySub;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;
import org.testng.AssertJUnit;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public class ChangeZoneAiTest extends AITest {

    @DataProvider(name = "etbSearches")
    public Object[][] etbSearches() {
        return new Object[][] {
                {"Oreskos Explorer", null},
                {"Oreskos Explorer", "Plains"},
                {"Imperial Recruiter", null},
                {"Imperial Recruiter", "Grizzly Bears"}
        };
    }

    @Test(dataProvider = "etbSearches")
    public void testCreatureSearchPayoffIsNotRequiredToCast(final String name, final String find) {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        addCards("Mountain", 3, ai);
        addCard("Plains", ai);
        addCards("Forest", 5, game.getPlayers().get(0));
        // A nonempty library with no matching cards exercises the search filter, not just an empty zone.
        for (int i = 0; i < 5; i++) {
            addCardToZone("Island", ai, ZoneType.Library);
        }
        if (find != null) {
            addCardToZone(find, ai, ZoneType.Library);
        }
        Card creature = addCardToZone(name, ai, ZoneType.Hand);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN2, ai);
        game.getAction().checkStateEffects(true);
        game.getTriggerHandler().resetActiveTriggers();
        game.copyLastState();

        var controller = ((PlayerControllerAi) ai.getController()).getAi();
        SpellAbility search = creature.getTriggers().get(0).ensureAbility().copy(ai);
        search.setTrigger(creature.getTriggers().get(0));
        AssertJUnit.assertEquals("Trigger assessment still distinguishes a search with a payoff",
                find != null, controller.doTrigger(search, false));
        var choices = controller.chooseSpellAbilityToPlay();
        AssertJUnit.assertNotNull("A harmless search with nothing to find must not veto the creature", choices);
        AssertJUnit.assertTrue(choices.stream().anyMatch(sa -> sa.getHostCard() == creature));

        SpellAbility spell = creature.getFirstSpellAbility();
        spell.setActivatingPlayer(ai);
        AssertJUnit.assertTrue(ComputerUtil.playStack(spell, ai, game));
        game.getStack().resolveStack();
        game.getTriggerHandler().runWaitingTriggers();
        game.getStack().addAllTriggeredAbilitiesToStack();
        for (int i = 0; i < 8 && !game.getStack().isEmpty(); i++) {
            game.getStack().resolveStack();
        }
        AssertJUnit.assertTrue("The creature and its search should finish resolving", game.getStack().isEmpty());
        AssertJUnit.assertEquals(1, countCardsWithName(game, name));
        AssertJUnit.assertEquals(find == null ? 0 : 1, ai.getCardsIn(ZoneType.Hand).size());
        if (find != null) {
            AssertJUnit.assertEquals(find, ai.getCardsIn(ZoneType.Hand).get(0).getName());
        }
    }

    @Test
    public void testEmptySearchDoesNotHideCostsOrFollowUps() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        addCardToZone("Island", ai, ZoneType.Library);
        Card creature = addCardToZone("Imperial Recruiter", ai, ZoneType.Hand);
        SpellAbility spell = creature.getFirstSpellAbility();
        spell.setActivatingPlayer(ai);
        SpellAbility search = creature.getTriggers().get(0).ensureAbility();
        var controller = ((PlayerControllerAi) ai.getController()).getAi();

        search.setPayCosts(new Cost("PayLife<19>", true));
        AssertJUnit.assertFalse("An empty search must not excuse a trigger's cost",
                controller.checkETBEffects(creature, spell, null));

        search.setPayCosts(Cost.Zero);
        search.setSubAbility((AbilitySub) AbilityFactory.getAbility(
                "DB$ LoseLife | Defined$ You | LifeAmount$ 20", creature));
        AssertJUnit.assertFalse("An empty search must not hide a harmful follow-up",
                controller.checkETBEffects(creature, spell, null));
    }

    @Test
    public void testStandaloneTutorStillNeedsSomethingToFind() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        addCard("Forest", ai);
        fillLibrary(ai, 5);
        Card tutor = addCardToZone("Lay of the Land", ai, ZoneType.Hand);
        SpellAbility spell = tutor.getFirstSpellAbility();
        spell.setActivatingPlayer(ai);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN2, ai);
        game.getAction().checkStateEffects(true);

        AssertJUnit.assertFalse("Casting a tutor without a payoff still wastes the spell and mana",
                SpellApiToAi.Converter.get(spell).canPlayWithSubs(ai, spell).willingToPlay());
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
