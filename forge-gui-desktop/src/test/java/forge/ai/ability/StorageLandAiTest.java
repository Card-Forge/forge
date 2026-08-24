package forge.ai.ability;

import forge.ai.AITest;
import forge.ai.PlayerControllerAi;
import forge.card.mana.ManaAtom;
import forge.game.Game;
import forge.game.card.Card;
import forge.game.card.CounterType;
import forge.game.cost.CostRemoveCounter;
import forge.game.mana.Mana;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.player.PlayerController.BinaryChoiceType;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;
import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import java.util.List;

public class StorageLandAiTest extends AITest {
    private static final CounterType STORAGE = CounterType.getType("STORAGE");
    private static final CounterType CHARGE = CounterType.getType("CHARGE");

    @Test
    public void fallenEmpiresStorageLandsAreAvailableToAiDecks() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        final String[] storageLands = {
                "Bottomless Vault", "Dwarven Hold", "Hollow Trees", "Icatian Store", "Sand Silos"
        };
        final String[] producedColors = {"B", "R", "G", "W", "U"};

        for (int i = 0; i < storageLands.length; i++) {
            final String cardName = storageLands[i];
            final Card land = addCard(cardName, ai);
            final SpellAbility manaAbility = getManaBatteryAbility(land);

            AssertJUnit.assertFalse(cardName, land.getRules().getAiHints().getRemAIDecks());
            AssertJUnit.assertEquals(cardName, "ManaRitualBattery", land.getSVar("AIUntapPreference"));
            AssertJUnit.assertEquals(cardName, "Count$xPaid", land.getSVar("X"));
            AssertJUnit.assertTrue(cardName,
                    land.hasKeyword("You may choose not to untap CARDNAME during your untap step."));
            AssertJUnit.assertEquals(cardName, "ManaRitualBattery", manaAbility.getParam("AILogic"));
            AssertJUnit.assertEquals(cardName, producedColors[i], manaAbility.getParam("Produced"));
            AssertJUnit.assertEquals(cardName, "X", manaAbility.getParam("Amount"));
            AssertJUnit.assertEquals(cardName, "True", manaAbility.getParam("AINoRecursiveCheck"));
            AssertJUnit.assertTrue(cardName, manaAbility.getPayCosts().hasTapCost());
            AssertJUnit.assertTrue(cardName,
                    manaAbility.getPayCosts().hasSpecificCostType(CostRemoveCounter.class));
        }
    }

    @Test
    public void storageLandStaysTappedAndAccumulatesWithoutAUsefulSpell() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        final Card silos = addStorageLand("Sand Silos", 4, ai);

        executeUntapStep(game, ai);
        AssertJUnit.assertTrue(silos.isTapped());
        AssertJUnit.assertEquals(4, silos.getCounters(STORAGE));

        game.getPhaseHandler().devAdvanceToPhase(PhaseType.UPKEEP);
        playUntilStackClear(game);
        AssertJUnit.assertEquals(5, silos.getCounters(STORAGE));
    }

    @Test
    public void storageLandUntapsForAnAffordableSameColorSpell() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        final Card silos = addStorageLand("Sand Silos", 4, ai);
        addCardToZone("Phantom Monster", ai, ZoneType.Hand);
        game.getPhaseHandler().devModeSet(PhaseType.UNTAP, ai);

        final SpellAbility manaAbility = silos.getManaAbilities().getFirst();
        manaAbility.setXManaCostPaid(7);
        AssertJUnit.assertTrue(chooseToUntap(ai, manaAbility));
        AssertJUnit.assertEquals(Integer.valueOf(7), manaAbility.getXManaCostPaid());
    }

    @Test
    public void aiUsesStoredManaToCastPayoffAndSpendsOnlyNeededCounters() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        final Card silos = addStorageLand("Sand Silos", 5, ai);
        final Card firstIsland = addTappedCard("Island", ai);
        final Card secondIsland = addTappedCard("Island", ai);
        addCardToZone("Air Elemental", ai, ZoneType.Hand);

        executeUntapStep(game, ai);
        AssertJUnit.assertFalse(silos.isTapped());
        AssertJUnit.assertFalse(firstIsland.isTapped());
        AssertJUnit.assertFalse(secondIsland.isTapped());

        game.getPhaseHandler().devModeSet(PhaseType.MAIN2, ai);
        final PlayerControllerAi controller = (PlayerControllerAi) ai.getController();
        final List<SpellAbility> manaChoices = controller.chooseSpellAbilityToPlay();
        AssertJUnit.assertNotNull(manaChoices);
        AssertJUnit.assertEquals("Sand Silos", manaChoices.get(0).getHostCard().getName());
        AssertJUnit.assertEquals(Integer.valueOf(3), manaChoices.get(0).getXManaCostPaid());

        controller.playChosenSpellAbility(manaChoices.get(0));
        AssertJUnit.assertTrue(silos.isTapped());
        AssertJUnit.assertEquals(2, silos.getCounters(STORAGE));
        AssertJUnit.assertEquals(3, ai.getManaPool().totalMana());

        final List<SpellAbility> spellChoices = controller.chooseSpellAbilityToPlay();
        AssertJUnit.assertNotNull(spellChoices);
        AssertJUnit.assertEquals("Air Elemental", spellChoices.get(0).getHostCard().getName());

        controller.playChosenSpellAbility(spellChoices.get(0));
        AssertJUnit.assertTrue(firstIsland.isTapped());
        AssertJUnit.assertTrue(secondIsland.isTapped());
        AssertJUnit.assertEquals(0, ai.getManaPool().totalMana());
        game.getStack().resolveStack();
        AssertJUnit.assertTrue(ai.isCardInPlay("Air Elemental"));
    }

    @Test
    public void storageLandStaysTappedWhenOtherSourcesAlreadyPayForTheSpell() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        final Card silos = addStorageLand("Sand Silos", 5, ai);
        for (int i = 0; i < 4; i++) {
            addTappedCard("Island", ai);
        }
        addCardToZone("Phantom Monster", ai, ZoneType.Hand);
        game.getPhaseHandler().devModeSet(PhaseType.UNTAP, ai);

        AssertJUnit.assertFalse(chooseToUntap(ai, silos.getManaAbilities().getFirst()));
    }

    @Test
    public void storageLandStaysTappedForAnOffColorSpell() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        final Card vault = addStorageLand("Bottomless Vault", 4, ai);
        addCardToZone("Phantom Monster", ai, ZoneType.Hand);
        game.getPhaseHandler().devModeSet(PhaseType.UNTAP, ai);

        AssertJUnit.assertFalse(chooseToUntap(ai, vault.getManaAbilities().getFirst()));
    }

    @Test
    public void storageLandStaysTappedWhenItCannotSupplyEnoughColoredMana() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        final Card silos = addStorageLand("Sand Silos", 1, ai);
        addCard("Mountain", ai);
        addCard("Mountain", ai);
        addCardToZone("Phantom Warrior", ai, ZoneType.Hand);
        game.getPhaseHandler().devModeSet(PhaseType.UNTAP, ai);

        AssertJUnit.assertFalse(chooseToUntap(ai, silos.getManaAbilities().getFirst()));
    }

    @Test
    public void storageLandStaysTappedForAnAlternativeCostTheAiWillNotUse() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        final Card silos = addStorageLand("Sand Silos", 3, ai);
        addCardToZone("Mulldrifter", ai, ZoneType.Hand);
        game.getPhaseHandler().devModeSet(PhaseType.UNTAP, ai);

        AssertJUnit.assertFalse(chooseToUntap(ai, silos.getManaAbilities().getFirst()));
    }

    @Test
    public void storageLandDoesNotUntapForAnInstantUntilRitualLogicSupportsInstantTiming() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        final Card silos = addStorageLand("Sand Silos", 2, ai);
        addCardToZone("Counterspell", ai, ZoneType.Hand);
        game.getPhaseHandler().devModeSet(PhaseType.UNTAP, ai);

        AssertJUnit.assertFalse(chooseToUntap(ai, silos.getManaAbilities().getFirst()));
    }

    @Test
    public void atLeastOneSufficientStorageLandUntapsDuringTheRealUntapStep() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        final Card firstSilos = addStorageLand("Sand Silos", 4, ai);
        final Card secondSilos = addStorageLand("Sand Silos", 4, ai);
        addCardToZone("Phantom Monster", ai, ZoneType.Hand);

        executeUntapStep(game, ai);

        AssertJUnit.assertFalse(firstSilos.isTapped() && secondSilos.isTapped());
    }

    @Test
    public void storageLandStaysTappedForAnUnusableSpell() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        final Card store = addStorageLand("Icatian Store", 1, ai);
        addCard("Isamaru, Hound of Konda", ai);
        addCardToZone("Isamaru, Hound of Konda", ai, ZoneType.Hand);
        game.getPhaseHandler().devModeSet(PhaseType.UNTAP, ai);

        AssertJUnit.assertFalse(chooseToUntap(ai, store.getManaAbilities().getFirst()));
    }

    @Test
    public void modernStorageLandStillContributesItsNormalManaAbility() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        final Card silos = addStorageLand("Sand Silos", 3, ai);
        final Card network = addCard("Mage-Ring Network", ai);
        network.setTapped(true);
        addCardToZone("Phantom Monster", ai, ZoneType.Hand);
        game.getPhaseHandler().devModeSet(PhaseType.UNTAP, ai);

        AssertJUnit.assertTrue(chooseToUntap(ai, silos.getManaAbilities().getFirst()));
    }

    @Test
    public void existingManaBatteryStillCountsItsBaseMana() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        final Card battery = addCard("White Mana Battery", ai);
        battery.setCounters(CHARGE, 3);
        addCard("Plains", ai);
        addCardToZone("Serra Angel", ai, ZoneType.Hand);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN2, ai);

        final SpellAbility manaAbility = getManaBatteryAbility(battery);
        manaAbility.setActivatingPlayer(ai);
        AssertJUnit.assertTrue(ManaAi.doManaRitualLogic(ai, manaAbility, false));
        AssertJUnit.assertEquals(Integer.valueOf(3), manaAbility.getXManaCostPaid());
    }

    @Test
    public void existingModernStorageLandDoesNotCountItselfTwice() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        final Card network = addCard("Mage-Ring Network", ai);
        network.setCounters(STORAGE, 3);
        addCard("Island", ai);
        addCardToZone("Juggernaut", ai, ZoneType.Hand);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN2, ai);

        final SpellAbility manaAbility = getManaBatteryAbility(network);
        manaAbility.setActivatingPlayer(ai);
        AssertJUnit.assertTrue(ManaAi.doManaRitualLogic(ai, manaAbility, false));
        AssertJUnit.assertEquals(Integer.valueOf(3), manaAbility.getXManaCostPaid());
    }

    @Test
    public void floatingManaReducesTheCountersSpent() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        final Card silos = addStorageLand("Sand Silos", 5, ai);
        silos.setTapped(false);
        addCardToZone("Phantom Monster", ai, ZoneType.Hand);
        final Card manaSource = createCard("Island", ai);
        ai.getManaPool().addMana(new Mana((byte) ManaAtom.BLUE, manaSource, null, ai));
        ai.getManaPool().addMana(new Mana((byte) ManaAtom.BLUE, manaSource, null, ai));
        game.getPhaseHandler().devModeSet(PhaseType.MAIN2, ai);

        final List<SpellAbility> choices = ((PlayerControllerAi) ai.getController())
                .chooseSpellAbilityToPlay();
        AssertJUnit.assertNotNull(choices);
        AssertJUnit.assertEquals("Sand Silos", choices.get(0).getHostCard().getName());
        AssertJUnit.assertEquals(Integer.valueOf(2), choices.get(0).getXManaCostPaid());
    }

    @Test
    public void storageLandSpendsForThePayoffTheControllerPrefers() {
        final Game game = initAndCreateGame();
        final Player ai = game.getPlayers().get(1);
        final Card store = addStorageLand("Icatian Store", 5, ai);
        addCardToZone("Ajani's Welcome", ai, ZoneType.Hand);
        addCardToZone("Serra Angel", ai, ZoneType.Hand);

        executeUntapStep(game, ai);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN2, ai);

        final PlayerControllerAi controller = (PlayerControllerAi) ai.getController();
        final List<SpellAbility> manaChoices = controller.chooseSpellAbilityToPlay();
        AssertJUnit.assertNotNull(manaChoices);
        AssertJUnit.assertEquals("Icatian Store", manaChoices.get(0).getHostCard().getName());
        AssertJUnit.assertEquals(Integer.valueOf(1), manaChoices.get(0).getXManaCostPaid());

        controller.playChosenSpellAbility(manaChoices.get(0));
        AssertJUnit.assertEquals(4, store.getCounters(STORAGE));

        final List<SpellAbility> spellChoices = controller.chooseSpellAbilityToPlay();
        AssertJUnit.assertNotNull(spellChoices);
        AssertJUnit.assertEquals("Ajani's Welcome", spellChoices.get(0).getHostCard().getName());
    }

    private Card addStorageLand(String cardName, int counters, Player ai) {
        final Card land = addCard(cardName, ai);
        land.setTapped(true);
        land.setCounters(STORAGE, counters);
        return land;
    }

    private Card addTappedCard(String cardName, Player ai) {
        final Card card = addCard(cardName, ai);
        card.setTapped(true);
        return card;
    }

    private static boolean chooseToUntap(Player ai, SpellAbility manaAbility) {
        final SpellAbility untapChoice = new SpellAbility.EmptySa(manaAbility.getHostCard(), ai);
        return ((PlayerControllerAi) ai.getController()).chooseBinary(untapChoice, "",
                BinaryChoiceType.UntapOrLeaveTapped, true);
    }

    private static void executeUntapStep(Game game, Player ai) {
        game.getPhaseHandler().devModeSet(PhaseType.UNTAP, ai);
        game.getUntap().executeUntil(ai);
        game.getUntap().executeAt();
    }

    private static SpellAbility getManaBatteryAbility(Card card) {
        return card.getManaAbilities().stream()
                .filter(sa -> sa.getParamOrDefault("AILogic", "").startsWith("ManaRitualBattery"))
                .findFirst()
                .orElseThrow();
    }
}
