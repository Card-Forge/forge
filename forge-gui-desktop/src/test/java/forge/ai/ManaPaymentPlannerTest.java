package forge.ai;

import java.util.ArrayList;

import forge.ai.simulation.SimulationTest;
import forge.card.mana.ManaCost;
import forge.card.mana.ManaCostParser;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CounterEnumType;
import forge.game.mana.Mana;
import forge.game.mana.ManaCostBeingPaid;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.AbilityManaPart;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;
import org.testng.AssertJUnit;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;

public class ManaPaymentPlannerTest extends SimulationTest {
    @AfterMethod
    public void resetPlannerState() {
        ManaPaymentPlanner.maxStates = 10000;
    }

    @Test
    public void weatherseedTreatyUsesFilterLand() {
        Player player = createTestPlayer();
        Card floodedGrove = addCard("Flooded Grove", player);
        addCard("Glacial Fortress", player);
        addCard("Plains", player);
        SpellAbility treaty = prepareSpell("The Weatherseed Treaty", player);

        AssertJUnit.assertTrue(pay(player, treaty));
        AssertJUnit.assertTrue(floodedGrove.isTapped());
    }

    @Test
    public void hiddenGrottosDoNotPayActivationCostsForFree() {
        Player player = createTestPlayer();
        addCards("Swamp", 4, player);
        addCards("Hidden Grotto", 3, player);
        SpellAbility vigor = prepareSpell("Vigor", player);

        AssertJUnit.assertFalse(canPay(player, vigor));
    }

    @Test
    public void signetsChainOnlyWhenNeeded() {
        assertSignetsUsed("Boros Guildmage", 1);
        assertSignetsUsed("Skyknight Legionnaire", 2);
    }

    @Test
    public void lotusPetalCanActivateSignetAndPayOuterGeneric() {
        Player player = createTestPlayer();
        Card petal = addCard("Lotus Petal", player);
        Card signet = addCard("Selesnya Signet", player);
        SpellAbility aspirant = prepareSpell("Luminarch Aspirant", player);

        AssertJUnit.assertTrue(pay(player, aspirant));
        AssertJUnit.assertTrue(signet.isTapped());
        AssertJUnit.assertEquals(ZoneType.Graveyard, petal.getZone().getZoneType());
    }

    @Test
    public void filterActivationDoesNotStrandRemainingGenericMana() {
        Player player = createTestPlayer();
        addCard("Plains", player);
        addCard("Lotus Petal", player);
        addCard("Study Hall", player);
        SpellAbility calix = prepareSpell("Calix, Guided by Fate", player);

        AssertJUnit.assertTrue(pay(player, calix));
    }

    @Test
    public void yunaPaysWithFilterLandAndTreasures() {
        Player player = createTestPlayer();
        addUntappedCard("Glacial Fortress", player);
        addUntappedCard("Seachrome Coast", player);
        addUntappedCard("Flooded Grove", player);
        addTokens("c_a_treasure_sac", 2, player);
        SpellAbility yuna = addSpellToHand("Yuna, Grand Summoner", player);
        addCardToZone("The Weatherseed Treaty", player, ZoneType.Hand);
        addCardToZone("Boros Guildmage", player, ZoneType.Hand);
        addCardToZone("Cromat", player, ZoneType.Hand);
        prepare(player);

        AssertJUnit.assertTrue(pay(player, yuna));
    }

    @Test
    public void nykthosPaysFromDynamicDevotion() {
        Player player = createTestPlayer();
        addCards("Swamp", 3, player);
        addCards("Nightmare Shepherd", 2, player);
        Card nykthos = addUntappedCard("Nykthos, Shrine to Nyx", player);
        SpellAbility merchant = prepareSpell("Gray Merchant of Asphodel", player);

        AssertJUnit.assertTrue(pay(player, merchant));
        AssertJUnit.assertTrue(nykthos.isTapped());
    }

    @Test
    public void evendoIsUsedOnlyWhenNeeded() {
        assertEvendoUsed("Bear Cub", 3, false);
        assertEvendoUsed("Craw Wurm", 6, true);
    }

    @Test
    public void restrictedManaHonorsThePaidSpell() {
        Player player = createTestPlayer();
        addCard("Forest", player);
        addCard("Wastes", player);
        addCard("Herd Heirloom", player);
        SpellAbility bear = addSpellToHand("Bear Cub", player);
        SpellAbility treaty = addSpellToHand("The Weatherseed Treaty", player);

        prepare(player);

        AssertJUnit.assertTrue(canPay(player, bear));
        AssertJUnit.assertFalse(canPay(player, treaty));
    }

    @Test
    public void equivalentCostedSourcesDoNotExhaustSearch() {
        Player player = createTestPlayer();
        addCards("Wastes", 6, player);
        addCards("Boros Signet", 6, player);
        SpellAbility dragon = prepareSpell("Shivan Dragon", player);

        ManaPaymentPlanner.Plan plan = ManaPaymentPlanner.findPlan(cost(dragon, player), dragon, player, true);
        AssertJUnit.assertNotNull(plan);
        AssertJUnit.assertFalse(plan.isExhausted());
    }

    @Test
    public void manaSourcePredictionIncludesActivationSources() {
        Player player = createTestPlayer();
        addCard("Plains", player);
        addCard("Boros Signet", player);
        SpellAbility guildmage = prepareSpell("Boros Guildmage", player);

        CardCollection sources = manaSources(player, guildmage);
        AssertJUnit.assertNotNull(sources);
        AssertJUnit.assertTrue(sources.anyMatch(card -> "Plains".equals(card.getName())));
        AssertJUnit.assertTrue(sources.anyMatch(card -> "Boros Signet".equals(card.getName())));
    }

    @Test
    public void multipleTapsForManaTriggersAreCombined() {
        Player player = createTestPlayer();
        Card forest = addCard("Forest", player);
        Card first = addCard("Market Festival", player);
        Card second = addCard("Market Festival", player);
        first.attachToEntity(forest, null);
        second.attachToEntity(forest, null);
        player.getGame().getTriggerHandler().registerActiveTrigger(first, false);
        player.getGame().getTriggerHandler().registerActiveTrigger(second, false);
        SpellAbility charm = prepareSpell("Temur Charm", player);

        AssertJUnit.assertTrue(pay(player, customCost("W U B R G"), charm));
    }

    @Test
    public void differentColorComboManaHonorsItsRestriction() {
        Player player = createTestPlayer();
        addUntappedCard("Firemind Vessel", player);
        SpellAbility electromancer = addSpellToHand("Goblin Electromancer", player);
        SpellAbility counterspell = addSpellToHand("Counterspell", player);
        prepare(player);

        AssertJUnit.assertTrue(canPay(player, customCost("U R"), electromancer));
        AssertJUnit.assertFalse(canPay(player, customCost("U U"), counterspell));
    }

    @Test
    public void unchosenManaProducesNoColor() {
        Player player = createTestPlayer();
        addUntappedCard("Coldsteel Heart", player);
        addCard("Boros Signet", player);
        SpellAbility bolt = prepareSpell("Lightning Bolt", player);

        AssertJUnit.assertFalse(canPay(player, bolt));
    }

    @Test
    public void planningRestoresManaChoices() {
        Player player = createTestPlayer();
        Card treasure = addTokens("c_a_treasure_sac", 1, player).get(0);
        AbilityManaPart manaPart = ComputerUtilMana.getManaPartAbility(
                treasure.getManaAbilities().get(0)).getManaPart();
        String originalChoice = manaPart.getExpressChoice();
        SpellAbility bolt = prepareSpell("Lightning Bolt", player);

        AssertJUnit.assertNotNull(ManaPaymentPlanner.findPlan(cost(bolt, player), bolt, player, true));
        AssertJUnit.assertEquals(originalChoice, manaPart.getExpressChoice());
    }

    @Test
    public void replayMismatchRollsBackResolvedManaAbilities() {
        Player player = createTestPlayer();
        Card heart = addUntappedCard("Coldsteel Heart", player);
        heart.setChosenColors(java.util.Collections.singletonList("Red"));
        SpellAbility bolt = prepareSpell("Lightning Bolt", player);

        ManaCostBeingPaid manaCost = cost(bolt, player);
        ManaPaymentPlanner.Plan plan = ManaPaymentPlanner.findPlan(manaCost, bolt, player, true);
        AssertJUnit.assertNotNull(plan);
        heart.setChosenColors(null);

        AssertJUnit.assertFalse(plan.pay(manaCost, bolt, player, false, new ArrayList<Mana>()));
        AssertJUnit.assertFalse(heart.isTapped());
        AssertJUnit.assertEquals(0, player.getManaPool().totalMana());
    }

    @Test
    public void exhaustedPlannerFallsBackForCastablePayment() {
        Player player = createTestPlayer();
        addCard("Mountain", player);
        addCard("Boros Signet", player);
        SpellAbility guildmage = prepareSpell("Boros Guildmage", player);
        ManaPaymentPlanner.maxStates = 0;
        AssertJUnit.assertTrue(canPay(player, customCost("R W"), guildmage));
    }

    @Test
    public void plannerDistinguishesSnowMana() {
        Player player = createTestPlayer();
        addCard("Snow-Covered Forest", player);
        addCards("Forest", 2, player);
        addCard("Boros Signet", player);
        SpellAbility charm = prepareSpell("Temur Charm", player);

        AssertJUnit.assertTrue(canPay(player, customCost("G S R"), charm));
    }

    @Test
    public void disposableManaIsNotRoutedThroughAFilter() {
        Player player = createTestPlayer();
        Card studyHall = addCard("Study Hall", player);
        addCard("Lotus Petal", player);
        SpellAbility lacerator = prepareSpell("Vampire Lacerator", player);

        AssertJUnit.assertTrue(pay(player, lacerator));
        AssertJUnit.assertFalse(studyHall.isTapped());
    }

    private void assertSignetsUsed(final String cardName, final int expected) {
        Player player = createTestPlayer();
        addCard("Plains", player);
        addCards("Boros Signet", 2, player);
        SpellAbility spell = prepareSpell(cardName, player);

        AssertJUnit.assertTrue(pay(player, spell));
        AssertJUnit.assertEquals(expected, player.getCardsIn(ZoneType.Battlefield).stream()
                .filter(card -> "Boros Signet".equals(card.getName()) && card.isTapped()).count());
    }

    private void assertEvendoUsed(final String cardName, final int creatures, final boolean expected) {
        Player player = createTestPlayer();
        addCard("Forest", player);
        addCard("Plains", player);
        Card evendo = addUntappedCard("Evendo, Waking Haven", player);
        evendo.setCounters(CounterEnumType.CHARGE, 12);
        addCards("Squire", creatures, player);
        SpellAbility spell = prepareSpell(cardName, player);

        AssertJUnit.assertTrue(pay(player, spell));
        AssertJUnit.assertEquals(expected, evendo.isTapped());
    }

    private Player createTestPlayer() {
        return initAndCreateGame().getPlayers().get(1);
    }

    private SpellAbility prepareSpell(final String cardName, final Player player) {
        SpellAbility spell = addSpellToHand(cardName, player);
        prepare(player);
        return spell;
    }

    private static void prepare(final Player player) {
        player.getGame().getPhaseHandler().devModeSet(PhaseType.MAIN1, player);
        player.getGame().getAction().checkStateEffects(true);
    }

    private static ManaCostBeingPaid customCost(final String value) {
        return new ManaCostBeingPaid(new ManaCost(new ManaCostParser(value)));
    }

    private static ManaCostBeingPaid cost(final SpellAbility spell, final Player player) {
        return ComputerUtilMana.calculateManaCost(
                spell.getPayCosts(), spell, player, false, 0, spell.isTrigger());
    }

    private static boolean canPay(final Player player, final SpellAbility spell) {
        return canPay(player, cost(spell, player), spell);
    }

    private static boolean canPay(final Player player, final ManaCostBeingPaid cost,
            final SpellAbility spell) {
        boolean[] result = new boolean[1];
        player.runWithController(() -> result[0] = ComputerUtilMana.canPayManaCost(cost, spell, player, false),
                new PlayerControllerAi(player.getGame(), player, player.getOriginalLobbyPlayer()));
        return result[0];
    }

    private static boolean pay(final Player player, final SpellAbility spell) {
        return pay(player, cost(spell, player), spell);
    }

    private static boolean pay(final Player player, final ManaCostBeingPaid cost,
            final SpellAbility spell) {
        boolean[] result = new boolean[1];
        player.runWithController(() -> result[0] = ComputerUtilMana.payManaCost(cost, spell, player, false),
                new PlayerControllerAi(player.getGame(), player, player.getOriginalLobbyPlayer()));
        return result[0];
    }

    private static CardCollection manaSources(final Player player, final SpellAbility spell) {
        CardCollection[] result = new CardCollection[1];
        player.runWithController(() -> result[0] = ComputerUtilMana.getManaSourcesToPayCost(
                        cost(spell, player), spell, player, false),
                new PlayerControllerAi(player.getGame(), player, player.getOriginalLobbyPlayer()));
        return result[0];
    }

    private SpellAbility addSpellToHand(final String cardName, final Player player) {
        SpellAbility spell = addCardToZone(cardName, player, ZoneType.Hand).getFirstSpellAbility();
        spell.setActivatingPlayer(player);
        return spell;
    }

    private Card addUntappedCard(final String cardName, final Player player) {
        Card card = addCard(cardName, player);
        card.setTapped(false);
        return card;
    }
}
