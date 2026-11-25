package forge.ai.ability;

import forge.ai.AiCostDecision;
import forge.ai.AiAbilityDecision;
import forge.ai.AiPlayDecision;
import forge.ai.ComputerUtil;
import forge.ai.ComputerUtilCard;
import forge.ai.LobbyPlayerAi;
import forge.ai.SpellAbilityAi;
import forge.ai.SpellApiToAi;
import forge.ai.simulation.SimulationTest;
import forge.game.Game;
import forge.game.ability.ApiType;
import forge.game.card.Card;
import forge.game.card.CounterEnumType;
import forge.game.combat.Combat;
import forge.game.cost.CostSacrifice;
import forge.game.cost.PaymentDecision;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;
import org.testng.annotations.Test;
import org.testng.annotations.DataProvider;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;

public class CountersPutAiTest extends SimulationTest {

    @Test
    public void testPowerUp_savesAttackerFromLethalCombat_aiActivates() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opponent = game.getPlayers().get(0);

        // Brave Brawler: 2/1, Power-Up {4}{W}: put two +1/+1 counters on this creature (becomes 4/3)
        Card brawler = addCard("Brave Brawler", ai);
        addCards("Plains", 5, ai);

        // Giant Spider: 2/4 - unpumped Brawler dies without trading; pumped Brawler (4/3) survives and kills it
        Card spider = addCard("Giant Spider", opponent);

        game.getPhaseHandler().devModeSet(PhaseType.COMBAT_DECLARE_BLOCKERS, ai);
        game.getAction().checkStateEffects(true);

        Combat combat = new Combat(ai);
        combat.addAttacker(brawler, opponent);
        combat.addBlocker(brawler, spider);
        game.getPhaseHandler().setCombat(combat);

        SpellAbility powerUpSA = findSAWithPrefix(brawler, "Power-Up");
        assertNotNull(powerUpSA);
        powerUpSA.setActivatingPlayer(ai);

        SpellAbilityAi aiLogic = SpellApiToAi.Converter.get(ApiType.PutCounter);
        AiPlayDecision decision = aiLogic.canPlayWithSubs(ai, powerUpSA).decision();
        assertEquals("AI should activate Power-Up to save its attacker and win combat",
                AiPlayDecision.WillPlay, decision);
    }

    private SpellAbility counterAbility(final Card source, final Player ai) {
        for (final SpellAbility ability : source.getSpellAbilities()) {
            if (ability.isActivatedAbility() && ability.getApi() == ApiType.PutCounter) {
                ability.setActivatingPlayer(ai);
                return ability;
            }
        }
        throw new AssertionError("missing counter ability on " + source);
    }

    @DataProvider
    public Object[][] noncreatureSacrificeCosts() {
        return new Object[][] {
            {"Flamewar, Brash Veteran", false},
            {"Dreadmalkin", true}
        };
    }

    @Test(dataProvider = "noncreatureSacrificeCosts")
    public void counterOutletsCanPayWithSuitableNoncreatures(final String name, final boolean planeswalker) {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        final Player opponent = game.getPlayers().get(0);
        ((LobbyPlayerAi) ai.getLobbyPlayer()).setAiProfile("Default");
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        final Card outlet = addCard(name, ai);
        final Card valuable = addCard(planeswalker ? "Liliana Vess" : "Crucible of Worlds", ai);
        if (planeswalker) {
            valuable.setCounters(CounterEnumType.LOYALTY, 5);
        }
        addCards("Swamp", 3, ai);
        final SpellAbility growth = counterAbility(outlet, ai);
        final SpellAbilityAi logic = SpellApiToAi.Converter.get(ApiType.PutCounter);
        assertFalse("a healthy valuable permanent is retained", logic.canPlayWithSubs(ai, growth).willingToPlay());

        final Card fodder;
        if (planeswalker) {
            fodder = valuable;
            final Card removal = addCardToZone("Hero's Downfall", opponent, ZoneType.Hand);
            final SpellAbility destroy = removal.getFirstSpellAbility();
            destroy.setActivatingPlayer(opponent);
            destroy.getTargets().add(fodder);
            game.getStack().addAndUnfreeze(destroy);
        } else {
            fodder = addToken("c_a_treasure_sac", ai);
        }

        final AiAbilityDecision decision = logic.canPlayWithSubs(ai, growth);
        assertTrue("another creature is not required to use a legal noncreature payment: " + decision.decision(),
                decision.willingToPlay());
        assertTrue("the actual cost is paid and the ability resolves", ComputerUtil.playNoStack(ai, growth, game, false));
        assertFalse(ai.getCardsIn(ZoneType.Battlefield).contains(fodder));
        assertTrue(ai.getCardsIn(ZoneType.Battlefield).contains(outlet));
        assertEquals(planeswalker ? 2 : 1, outlet.getCounters(CounterEnumType.P1P1));
        if (!planeswalker) {
            assertTrue("the valuable artifact is not used instead of Treasure", ai.getCardsIn(ZoneType.Battlefield).contains(valuable));
        }
    }

    @Test
    public void routineAristocratGrowthIsPrecombatAndOncePerTurn() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        ((LobbyPlayerAi) ai.getLobbyPlayer()).setAiProfile("Default");
        game.getPhaseHandler().devModeSet(PhaseType.MAIN2, ai);
        final Card feeder = addCard("Carrion Feeder", ai);
        addTokens("w_0_1_goat", 2, ai);
        final SpellAbility growth = counterAbility(feeder, ai);
        final SpellAbilityAi logic = SpellApiToAi.Converter.get(ApiType.PutCounter);

        assertFalse("routine growth waits for a precombat opportunity",
                logic.canPlayWithSubs(ai, growth).willingToPlay());
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, ai);
        assertTrue("expendable fodder allows one precombat activation",
                logic.canPlayWithSubs(ai, growth).willingToPlay());
        feeder.addAbilityActivated(growth);
        assertFalse("routine growth stops after the first activation",
                logic.canPlayWithSubs(ai, growth).willingToPlay());
    }

    @Test
    public void blockedAristocratRequiresEnoughFodderAndStopsWhenSafe() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        final Player opponent = game.getPlayers().get(0);
        ((LobbyPlayerAi) ai.getLobbyPlayer()).setAiProfile("Default");
        game.getPhaseHandler().devModeSet(PhaseType.COMBAT_DECLARE_BLOCKERS, ai);
        final Card feeder = addCard("Carrion Feeder", ai);
        final Card blocker = addCard("Hill Giant", opponent);
        addToken("w_0_1_goat", ai);
        final SpellAbility growth = counterAbility(feeder, ai);
        final SpellAbilityAi logic = SpellApiToAi.Converter.get(ApiType.PutCounter);
        final Combat combat = new Combat(ai);
        combat.addAttacker(feeder, opponent);
        combat.addBlocker(feeder, blocker);
        game.getPhaseHandler().setCombat(combat);

        assertFalse("one counter cannot save the attacker or kill the blocker",
                logic.canPlayWithSubs(ai, growth).willingToPlay());
        addTokens("w_0_1_goat", 2, ai);
        assertTrue("three sacrifices can make the attacker survive and kill",
                logic.canPlayWithSubs(ai, growth).willingToPlay());
        feeder.setCounters(CounterEnumType.P1P1, 3);
        assertFalse("no more sacrifices are needed once the combat goal is met",
                logic.canPlayWithSubs(ai, growth).willingToPlay());
    }

    @Test
    public void threatenedFodderIsChosenOnTheOpponentsTurn() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        final Player opponent = game.getPlayers().get(0);
        ((LobbyPlayerAi) ai.getLobbyPlayer()).setAiProfile("Default");
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, opponent);
        final Card outlet = addCard("Carrion Feeder", ai);
        final SpellAbility growth = counterAbility(outlet, ai);
        final Card victim = addCard("Hill Giant", ai);
        final Card otherFodder = addToken("w_0_1_goat", ai);
        final Card swords = addCardToZone("Swords to Plowshares", opponent, ZoneType.Hand);
        final SpellAbility exile = swords.getFirstSpellAbility();
        exile.setActivatingPlayer(opponent);
        exile.getTargets().add(victim);
        game.getStack().addAndUnfreeze(exile);

        assertTrue("a response to removal is not routine growth",
                SpellApiToAi.Converter.get(ApiType.PutCounter).canPlayWithSubs(ai, growth).willingToPlay());
        final CostSacrifice cost = (CostSacrifice) growth.getPayCosts().getCostParts().get(0);
        final PaymentDecision payment = new AiCostDecision(ai, growth, false).visit(cost);
        assertNotNull(payment);
        assertEquals(1, payment.cards.size());
        assertTrue("the doomed creature is sacrificed before preferred safe fodder", payment.cards.contains(victim));
        assertFalse("the outlet must survive", payment.cards.contains(outlet));
        assertFalse("safe fodder remains available", payment.cards.contains(otherFodder));
        assertTrue(ComputerUtil.playNoStack(ai, growth, game, false));
        assertTrue(ai.getCardsIn(ZoneType.Graveyard).contains(victim));
        assertTrue(ai.getCreaturesInPlay().contains(otherFodder));
        assertEquals(1, outlet.getCounters(CounterEnumType.P1P1));
    }

    @DataProvider
    public Object[][] defensiveAristocrats() {
        return new Object[][] {
            {"Unburied Earthcarver", "Grizzly Bears", false},
            {"Falkenrath Aristocrat", "Air Elemental", false},
            {"Falkenrath Aristocrat", "Air Elemental", true}
        };
    }

    @Test(dataProvider = "defensiveAristocrats")
    public void combatAristocratsPayForProtectionAndStop(final String name, final String enemyName,
            final boolean attacking) {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        final Player opponent = game.getPlayers().get(0);
        ((LobbyPlayerAi) ai.getLobbyPlayer()).setAiProfile("Default");
        final Card outlet = addCard(name, ai);
        final Card enemy = addCard(enemyName, opponent);
        final Card valuableHuman = addCard("Eager Cadet", ai);
        valuableHuman.setCounters(CounterEnumType.P1P1, 8);
        addToken("w_0_1_goat", ai);
        addToken("w_0_1_goat", ai);
        addCards("Swamp", 4, ai);
        final SpellAbility ability = outlet.getSpellAbilities().stream()
                .filter(sa -> sa.isActivatedAbility() && sa.hasParam("AILogic")).findFirst().get();
        ability.setActivatingPlayer(ai);
        final Player active = attacking ? ai : opponent;
        game.getPhaseHandler().devModeSet(PhaseType.COMBAT_DECLARE_BLOCKERS, active);
        final Combat combat = new Combat(active);
        combat.addAttacker(attacking ? outlet : enemy, attacking ? opponent : ai);
        combat.addBlocker(attacking ? outlet : enemy, attacking ? enemy : outlet);
        game.getPhaseHandler().setCombat(combat);
        outlet.addAbilityActivated(ability);

        final SpellAbilityAi logic = SpellApiToAi.Converter.get(ability);
        assertTrue("combat protection remains available after a previous activation",
                logic.canPlayWithSubs(ai, ability).willingToPlay());
        assertTrue("the actual ability can be paid and resolved", ComputerUtil.playNoStack(ai, ability, game, false));
        assertEquals("exactly one non-Human token paid for protection", 3, ai.getCreaturesInPlay().size());
        assertTrue("a Human preference must not sacrifice a valuable creature", ai.getCreaturesInPlay().contains(valuableHuman));
        if (ability.getApi() == ApiType.Pump) {
            assertTrue(outlet.hasKeyword(forge.game.keyword.Keyword.INDESTRUCTIBLE));
            assertEquals("a Goat does not grant the Human counter bonus", 0, outlet.getCounters(CounterEnumType.P1P1));
        } else {
            assertEquals(1, outlet.getCounters(CounterEnumType.P1P1));
        }
        assertFalse("stop after the combat purpose is achieved", logic.canPlayWithSubs(ai, ability).willingToPlay());
    }

    @DataProvider
    public Object[][] relativeAristocratValues() {
        return new Object[][] {
            {"Fallen Angel", "Hill Giant", 0, true},
            {"Bloodthrone Vampire", "Eager Cadet", -1, false}
        };
    }

    @Test(dataProvider = "relativeAristocratValues")
    public void ordinaryAristocratsCompareFodderWithTheOutlet(final String outletName, final String fodderName,
            final int powerModifier, final boolean expected) {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        final Player opponent = game.getPlayers().get(0);
        ((LobbyPlayerAi) ai.getLobbyPlayer()).setAiProfile("Default");
        final Card outlet = addCard(outletName, ai);
        outlet.addPTBoost(powerModifier, 0, game.getNextTimestamp(), 0);
        final Card fodder = addCard(fodderName, ai);
        final int fodderValue = ComputerUtilCard.evaluateCreature(fodder);
        assertEquals(expected, fodderValue < ComputerUtilCard.evaluateCreature(outlet));
        assertEquals("the fixed threshold gives the opposite result", expected, fodderValue > 135);
        final SpellAbility ability = outlet.getSpellAbilities().stream()
                .filter(sa -> "Aristocrat".equals(sa.getParam("AILogic"))).findFirst().get();
        ability.setActivatingPlayer(ai);
        game.getPhaseHandler().devModeSet(PhaseType.COMBAT_DECLARE_BLOCKERS, opponent);
        final Combat combat = new Combat(opponent);
        final Card attacker = addCard("Hill Giant", opponent);
        combat.addAttacker(attacker, ai);
        combat.addBlocker(attacker, outlet);
        game.getPhaseHandler().setCombat(combat);
        assertEquals(expected, SpellApiToAi.Converter.get(ability).canPlayWithSubs(ai, ability).willingToPlay());
    }
}
