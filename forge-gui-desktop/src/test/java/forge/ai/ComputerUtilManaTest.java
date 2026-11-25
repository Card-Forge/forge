package forge.ai;

import com.google.common.collect.Lists;
import forge.ai.simulation.SimulationTest;
import forge.card.mana.ManaCost;
import forge.card.mana.ManaCostParser;
import forge.game.Game;
import forge.game.ability.AbilityFactory;
import forge.game.ability.ApiType;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CounterEnumType;
import forge.game.mana.ManaCostBeingPaid;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;
import forge.gamesimulationtests.util.PlayerControllerForTests;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;

/**
 * Whether {@link ComputerUtilMana} can pay a cost with a source, and how much mana the source makes.
 * {@link ComputerUtilManaOrderTest} covers which source pays when several could.
 * A test payment counts a mana ability that sacrifices another permanent once for each permanent it can sacrifice.
 * A source that removes X counters to make mana removes only as many as the other sources leave unpaid.
 */
public class ComputerUtilManaTest extends SimulationTest {

    /** A player with these permanents and this many 2/2 tokens, in their own main phase. */
    private Player sacrificeBoard(final int tokens, final String... permanents) {
        Game game = initAndCreateGame();
        final Player p = game.getPlayers().get(1);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN2, p);
        for (final String name : permanents) {
            addCard(name, p);
        }
        addTokens("b_2_2_zombie", tokens, p);
        return p;
    }

    private boolean canPay(final Player p, final String spell) {
        final SpellAbility sa = addCardToZone(spell, p, ZoneType.Hand).getFirstSpellAbility();
        sa.setActivatingPlayer(p);
        return ComputerUtilMana.canPayManaCost(sa, p, 0, false);
    }

    private void payForReal(final Player p, final String manaCost) {
        final SpellAbility sa = addCardToZone("Grizzly Bears", p, ZoneType.Hand).getFirstSpellAbility();
        sa.setActivatingPlayer(p);
        final ManaCostBeingPaid cost = new ManaCostBeingPaid(new ManaCost(new ManaCostParser(manaCost)));
        assertTrue(ComputerUtilMana.payManaCost(cost, sa, p, false) && cost.isPaid());
    }

    @Test
    public void sacrificeSourceIsUsedOncePerFodder() {
        // Phyrexian Altar with three tokens pays for a three-drop on its own, but not for a four-drop
        final Player p = sacrificeBoard(3, "Phyrexian Altar");
        assertTrue(canPay(p, "Centaur Courser"), "{2}{G} with three tokens to sacrifice");
        assertFalse(canPay(p, "Hill Giant"), "{3}{R} needs a fourth token");
    }

    @Test
    public void sacrificeSourceNeedsFodderForEachUse() {
        assertFalse(canPay(sacrificeBoard(2, "Phyrexian Altar"), "Centaur Courser"), "{2}{G} needs a third token");
    }

    @Test
    public void sacrificeSourceMakingTwoManaIsUsedOncePerFodder() {
        // Ashnod's Altar makes {C}{C} per token
        final Player p = sacrificeBoard(2, "Ashnod's Altar");
        assertTrue(canPay(p, "Juggernaut"), "{4} with two tokens to sacrifice");
        assertFalse(canPay(p, "Gilded Lotus"), "{5} needs a third token");
    }

    @Test
    public void selfSacrificeSourceIsNotAlsoFodder() {
        // Blood Pet makes mana by sacrificing itself, so it can't also be sacrificed to Phyrexian Altar
        final Player p = sacrificeBoard(2, "Phyrexian Altar", "Blood Pet");
        assertTrue(canPay(p, "Centaur Courser"), "{2}{G} from two tokens and Blood Pet");
        assertFalse(canPay(p, "Hill Giant"), "{3}{R} would count Blood Pet twice");
    }

    private CardCollection sourcesToPay(final Player p, final String cost) {
        // applies the static abilities that grant the mana abilities
        p.getGame().getAction().checkStateEffects(true);
        final SpellAbility sa = addCardToZone("Grizzly Bears", p, ZoneType.Hand).getFirstSpellAbility();
        sa.setActivatingPlayer(p);
        return ComputerUtilMana.getManaSourcesToPayCost(new ManaCostBeingPaid(new ManaCost(new ManaCostParser(cost))), sa, p, false);
    }

    @Test
    public void grantedTwoManaAbilityOnLandPaysBothShards() {
        final Player p = sacrificeBoard(0, "Mountain", "Mountain");
        final Card forest = addCard("Forest", p);
        addCard("Gift of Paradise", p).attachToEntity(forest, null);
        assertEquals(sourcesToPay(p, "1 G"), new CardCollection(forest));
    }

    @Test
    public void grantedTwoManaAbilityOnTreasurePaysBothShards() {
        final Player p = sacrificeBoard(0, "Goldspan Dragon");
        final Card treasure = addToken("c_a_treasure_sac", p);
        assertEquals(sourcesToPay(p, "1 R"), new CardCollection(treasure));
    }

    @Test
    public void landIsNotSacrificedForMoreManaThanNeeded() {
        // Crystal Vein's sacrifice ability makes {C}{C}, but {1} only needs the tap ability
        final Player p = sacrificeBoard(0, "Crystal Vein");
        final SpellAbility sa = addCardToZone("Grizzly Bears", p, ZoneType.Hand).getFirstSpellAbility();
        sa.setActivatingPlayer(p);
        assertTrue(ComputerUtilMana.payManaCost(new ManaCostBeingPaid(new ManaCost(new ManaCostParser("1"))), sa, p, false));
        assertEquals(p.getCardsIn(ZoneType.Battlefield).size(), 1, "Crystal Vein should still be on the battlefield");
    }

    @Test
    public void humanScannedByAiCountsSacrificeMana() {
        final Player p = sacrificeBoard(2, "Phyrexian Altar", "Blood Pet");
        p.dangerouslySetController(new PlayerControllerForTests(p.getGame(), p, p.getLobbyPlayer()));
        assertFalse(canPay(p, "Hill Giant"), "without an AI controller the Altar is used once");

        // the available actions scan runs a human's payment under a temporary AI controller
        p.runWithController(() -> {
            assertFalse(p.isAI());
            assertTrue(canPay(p, "Centaur Courser"), "{2}{G} from two tokens and Blood Pet");
            assertFalse(canPay(p, "Hill Giant"), "{3}{R} would count Blood Pet twice");
        }, new PlayerControllerAi(p.getGame(), p, p.getLobbyPlayer()));
    }

    @Test
    public void humanScannedByAiCountsLotusForACheapSpell() {
        final Player p = sacrificeBoard(0, "Lotus Bloom");
        assertFalse(canPay(p, "Savannah Lions"), "the AI keeps a Lotus for a spell of mana value 4 or more");

        p.dangerouslySetController(new PlayerControllerForTests(p.getGame(), p, p.getLobbyPlayer()));
        p.runWithController(() -> assertTrue(canPay(p, "Savannah Lions")),
                new PlayerControllerAi(p.getGame(), p, p.getLobbyPlayer()));
    }

    @Test
    public void humanScannedByAiPaysChannelLifeAtLowLife() {
        final Player p = sacrificeBoard(0);
        p.setLife(5, null);
        final Card channelEffect = addCardToZone("Channel", p, ZoneType.Command);
        channelEffect.addSpellAbility(AbilityFactory.getAbility(channelEffect, "ABMana"));
        p.dangerouslySetController(new PlayerControllerForTests(p.getGame(), p, p.getLobbyPlayer()));
        p.runWithController(() -> assertTrue(canPay(p, "Sol Ring")),
                new PlayerControllerAi(p.getGame(), p, p.getLobbyPlayer()));
    }

    @Test
    public void humanScannedByAiCountsATreasureWhoseSacrificeIsPunished() {
        final Player p = sacrificeBoard(0);
        addToken("c_a_treasure_sac", p);
        addCard("Bloodroot Apothecary", p.getGame().getPlayers().get(0));
        assertFalse(canPay(p, "Savannah Lions"), "the AI won't take two poison counters for a Treasure");

        p.dangerouslySetController(new PlayerControllerForTests(p.getGame(), p, p.getLobbyPlayer()));
        p.runWithController(() -> assertTrue(canPay(p, "Savannah Lions")),
                new PlayerControllerAi(p.getGame(), p, p.getLobbyPlayer()));
    }

    @Test
    public void batteryRemovesOnlyTheCountersItNeeds() {
        final Player p = initAndCreateGame().getPlayers().get(1);
        final Card battery = addCard("Blue Mana Battery", p);
        battery.setCounters(CounterEnumType.CHARGE, 5);

        payForReal(p, "2 U U");
        assertEquals(battery.getCounters(CounterEnumType.CHARGE), 2, "its first {U} needs no counter");
    }

    @Test
    public void batteryKeepsItsCountersWhenLandsCanPay() {
        final Player p = initAndCreateGame().getPlayers().get(1);
        addCards("Swamp", 3, p);
        final Card battery = addCard("Blue Mana Battery", p);
        battery.setCounters(CounterEnumType.CHARGE, 5);

        payForReal(p, "3 U");
        assertEquals(battery.getCounters(CounterEnumType.CHARGE), 5);
    }

    @Test
    public void batteryAddsCountersForWhatTheLandsCannotPay() {
        final Player p = initAndCreateGame().getPlayers().get(1);
        addCard("Swamp", p);
        final Card battery = addCard("Blue Mana Battery", p);
        battery.setCounters(CounterEnumType.CHARGE, 5);

        payForReal(p, "3 U");
        assertEquals(battery.getCounters(CounterEnumType.CHARGE), 3);
        assertTrue(AiCardMemory.getMemorySet(p, AiCardMemory.MemorySetMana.UNPAID_COSTS).isEmpty(),
                "trials with too few counters are not unpaid costs");
    }

    @Test
    public void testPaymentCountsBatteryCounters() {
        final Player p = initAndCreateGame().getPlayers().get(1);
        addCard("Island", p);
        addCard("Blue Mana Battery", p).setCounters(CounterEnumType.CHARGE, 3);
        assertTrue(canPay(p, "Air Elemental"), "{3}{U}{U}: the Island and the Battery's free {U} pay two, its counters the rest");
    }

    @Test
    public void twoCounterSourcesRemoveOnlyWhatTheCostNeeds() {
        final Player p = initAndCreateGame().getPlayers().get(1);
        addCards("Wastes", 2, p);
        final Card battery = addCard("Black Mana Battery", p);
        battery.setCounters(CounterEnumType.CHARGE, 5);
        final Card silos = addCard("Sand Silos", p);
        silos.setCounters(CounterEnumType.STORAGE, 5);

        payForReal(p, "2 U U B");
        assertEquals(battery.getCounters(CounterEnumType.CHARGE), 5, "Silos makes {U}{U}, the Battery its free {B}, the Wastes {2}");
        assertEquals(silos.getCounters(CounterEnumType.STORAGE), 3);
    }

    @Test
    public void signetIsPaidByACounterSourceThatAlsoPaysAColor() {
        final Player p = initAndCreateGame().getPlayers().get(1);
        addCard("Plains", p);
        addCard("Azorius Signet", p);
        final Card battery = addCard("Blue Mana Battery", p);
        battery.setCounters(CounterEnumType.CHARGE, 3);

        payForReal(p, "W W U U");
        assertEquals(battery.getCounters(CounterEnumType.CHARGE), 2);
    }

    @Test
    public void haruspexDoesNotRemoveTheCountersKeepingItAlive() {
        final Player p = sacrificeBoard(0, "Forest");
        final Card haruspex = addCard("Haruspex", p); // 2/2
        haruspex.setSickness(false);
        haruspex.setCounters(CounterEnumType.P1P1, 2);
        haruspex.setDamage(3);
        assertFalse(canPay(p, "Grizzly Bears"));
    }

    @Test
    public void haruspexWithUndyingMayRemoveItsLastCounters() {
        final Player p = sacrificeBoard(0, "Forest");
        final Card haruspex = addCard("Haruspex", p); // 2/2
        haruspex.setSickness(false);
        haruspex.setCounters(CounterEnumType.P1P1, 2);
        haruspex.setDamage(3);
        haruspex.addChangedCardKeywords(Lists.newArrayList("Undying"), null, false, p.getGame().getNextTimestamp(), null);
        assertTrue(canPay(p, "Grizzly Bears"));
    }

    @Test
    public void batteryPayingASignetsCostAddsNoCounterForItsOwnMana() {
        final Player p = initAndCreateGame().getPlayers().get(1);
        final Card battery = addCard("Blue Mana Battery", p);
        battery.setCounters(CounterEnumType.CHARGE, 3);
        final SpellAbility signet = addCard("Azorius Signet", p).getManaAbilities().get(0);
        signet.setActivatingPlayer(p);

        // a mana ability's own cost is paid without caps on X
        final ManaCostBeingPaid cost = new ManaCostBeingPaid(new ManaCost(new ManaCostParser("1")));
        assertTrue(ComputerUtilMana.payManaCost(cost, signet, p, false) && cost.isPaid());
        assertEquals(battery.getCounters(CounterEnumType.CHARGE), 3);
    }

    @Test
    public void testPaymentWithAStorageLandLeavesNoXBehind() {
        final Player p = initAndCreateGame().getPlayers().get(1);
        final Card vault = addCard("Bottomless Vault", p);
        vault.setCounters(CounterEnumType.STORAGE, 4);

        assertTrue(canPay(p, "Gravedigger")); // {3}{B}
        assertNull(vault.getManaAbilities().get(0).getXManaCostPaid());
    }

    @Test
    public void snowStorageLandStillMakesManaForASnowShard() {
        final Player p = initAndCreateGame().getPlayers().get(1);
        final Card vault = addCard("Bottomless Vault", p);
        vault.addType("Snow");
        vault.setCounters(CounterEnumType.STORAGE, 3);

        payForReal(p, "S");
        assertEquals(vault.getCounters(CounterEnumType.STORAGE), 2);
    }

    @Test
    public void manaEstimateCountsRemovableCounters() {
        final Player p = initAndCreateGame().getPlayers().get(1);
        addCard("Plains", p);
        addCard("Blue Mana Battery", p).setCounters(CounterEnumType.CHARGE, 3);
        addCard("Bottomless Vault", p).setCounters(CounterEnumType.STORAGE, 2);

        // Plains 1, Battery {U} plus one per counter, Vault one per counter
        assertEquals(ComputerUtilMana.getAvailableManaEstimate(p), 1 + 4 + 2);
    }

    @Test
    public void nykthosChoosesTheColorThePaymentNeeds() {
        final Player p = initAndCreateGame().getPlayers().get(1);
        addCards("Island", 2, p);
        addCard("Nykthos, Shrine to Nyx", p);
        addCard("Leatherback Baloth", p);
        // blue is the most prominent color, but green has as much devotion
        addCards("Merfolk of the Pearl Trident", 3, p);

        payForReal(p, "G G G");
    }

    @Test
    public void failedTestPaymentClearsTheColorPickedForNykthos() {
        final Player p = initAndCreateGame().getPlayers().get(1);
        addCard("Plains", p);
        addCard("Azorius Signet", p);
        final Card nykthos = addCard("Nykthos, Shrine to Nyx", p);
        addCard("White Knight", p);
        addCards("Merfolk of the Pearl Trident", 3, p);

        final SpellAbility sa = addCardToZone("Grizzly Bears", p, ZoneType.Hand).getFirstSpellAbility();
        sa.setActivatingPlayer(p);
        assertNull(ComputerUtilMana.getManaSourcesToPayCost(new ManaCostBeingPaid(new ManaCost(new ManaCostParser("W W W W"))), sa, p, false));

        final SpellAbility devotion = nykthos.getManaAbilities().stream().filter(ma -> ma.getApi() == ApiType.ChooseColor).findFirst().orElseThrow();
        assertEquals(devotion.getSubAbility().getManaPart().getExpressChoice(), "", "a color left behind would steer Nykthos's next activation");
    }

    @Test
    public void testPaymentCountsDevotionMana() {
        final Player p = initAndCreateGame().getPlayers().get(1);
        addCard("Nyx Lotus", p);
        addCard("Leatherback Baloth", p);
        assertTrue(canPay(p, "Leatherback Baloth"), "{G}{G}{G}: Nyx Lotus adds one {G} for each of the Baloth's");
    }

    @Test
    public void chosenPlayerManaGoesToTheAi() {
        final Player p = initAndCreateGame().getPlayers().get(1);
        addCard("Victory Chimes", p);
        payForReal(p, "1");
    }

    @Test
    public void chosenPlayerChoosesTheColorThePaymentNeeds() {
        final Player p = initAndCreateGame().getPlayers().get(1);
        addCard("Spectral Searchlight", p);
        payForReal(p, "U");
    }

    @Test
    public void chosenPlayerManaPaysWithASacrificeCost() {
        final Game game = initAndCreateGame();
        final Player p = game.getPlayers().get(1);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN2, p);
        addCard("Valleymaker", p).setSickness(false);
        addCard("Forest", p);
        payForReal(p, "G G G");
    }

    @Test
    public void colorChoosingFilterFixesColor() {
        final Player p = initAndCreateGame().getPlayers().get(1);
        addCard("Forest", p);
        addCard("Foraging Wickermaw", p);
        payForReal(p, "U");
    }

    /**
     * The estimate and the payment have to agree. Lion's Eye Diamond is left out on purpose: the payer never takes it for
     * a spell (it is an instant-speed ability that discards the hand, and the loops crack it by themselves), so the
     * estimate says no and the payment says no.
     */
    private void estimateAndPaymentAgree(final Player p, final String spell) {
        final SpellAbility sa = addCardToZone(spell, p, ZoneType.Hand).getFirstSpellAbility();
        sa.setActivatingPlayer(p);
        assertTrue(ComputerUtilMana.canPayManaCost(sa, p, 0, false), "the estimate for " + spell);
        assertTrue(ComputerUtilMana.payManaCost(sa.getPayCosts(), p, sa, false), "the payment of " + spell);
    }

    @Test
    public void lotusPetalPaysForBrainstorm() {
        final Player p = sacrificeBoard(0, "Lotus Petal");
        estimateAndPaymentAgree(p, "Brainstorm");
    }

    @Test
    public void lionsEyeDiamondIsNotAnAutomaticSourceForASpell() {
        final Player p = sacrificeBoard(0, "Lion's Eye Diamond");
        final SpellAbility sa = addCardToZone("Burning Wish", p, ZoneType.Hand).getFirstSpellAbility();
        sa.setActivatingPlayer(p);
        assertFalse(ComputerUtilMana.canPayManaCost(sa, p, 0, false), "the estimate must not count the Diamond");
        assertFalse(ComputerUtilMana.payManaCost(sa.getPayCosts(), p, sa, false), "and the payer must not take it");
    }

    @Test
    public void phyrexianLifePaymentRespectsKarnsSylex() {
        final Player p = sacrificeBoard(0, "Karn's Sylex");
        p.setLife(14, null);
        final SpellAbility sa = addCardToZone("Phyrexian Metamorph", p, ZoneType.Hand).getFirstSpellAbility();
        assertFalse(ComputerUtilMana.canPayLifeForManaShard(p, sa, 2));
    }

    @Test
    public void phyrexianLifePaymentHonorsNeverPolicy() {
        final Player p = sacrificeBoard(0);
        final SpellAbility sa = addCardToZone("Mental Misstep", p, ZoneType.Hand).getFirstSpellAbility();
        assertFalse(ComputerUtilMana.canPayLifeForManaShard(p, sa, 2));
    }

    @Test
    public void phyrexianLifePaymentHonorsOnFatalDamagePolicy() {
        final Player p = sacrificeBoard(0);
        final SpellAbility sa = addCardToZone("Gut Shot", p, ZoneType.Hand).getFirstSpellAbility();
        assertFalse(ComputerUtilMana.canPayLifeForManaShard(p, sa, 2));
        p.getGame().getPlayers().get(0).setLife(1, null);
        assertTrue(ComputerUtilMana.canPayLifeForManaShard(p, sa, 2));
    }

    /** A player in their own main phase. */
    private Player auraBoard() {
        final Game game = initAndCreateGame();
        final Player p = game.getPlayers().get(1);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, p);
        return p;
    }

    private Card enchant(final Player p, final String aura, final Card land) {
        final Card c = addCard(aura, p);
        c.attachToEntity(land, null);
        p.getGame().getAction().checkStateEffects(true);
        return c;
    }

    @Test
    public void auraChosenColorPaysAShard() {
        final Player p = auraBoard();
        final Card forest = addCard("Forest", p);
        enchant(p, "Utopia Sprawl", forest).setChosenColors(Lists.newArrayList("blue"));
        assertTrue(canPay(p, "Coral Merfolk"));
        payForReal(p, "1 U");
        assertTrue(forest.isTapped());
    }

    @Test
    public void auraAnyColorIgnoresMostCommonColorInHand() {
        final Player p = auraBoard();
        enchant(p, "Fertile Ground", addCard("Forest", p));
        addCard("Forest", p);
        for (int i = 0; i < 3; i++) {
            addCardToZone("Coral Merfolk", p, ZoneType.Hand);
        }
        assertTrue(canPay(p, "Goblin Piker"));
        payForReal(p, "1 R");
    }

    @Test
    public void auraAnyColorMakesThePlannedColor() {
        final Player p = auraBoard();
        enchant(p, "Fertile Ground", addCard("Forest", p));
        addCard("Plains", p);
        assertTrue(canPay(p, "Azorius First-Wing"));
        payForReal(p, "W U");
    }

    @Test
    public void auraColorsKeepThePlainsForWhite() {
        final Player p = auraBoard();
        enchant(p, "Utopia Sprawl", addCard("Forest", p)).setChosenColors(Lists.newArrayList("blue"));
        enchant(p, "Fertile Ground", addCard("Forest", p));
        addCard("Plains", p);
        // both Forests are needed for blue, so the Plains has to pay {W}
        payForReal(p, "W U U");
    }

    @Test
    public void auraAnyColorPaysTheShardItsLandLeaves() {
        final Player p = auraBoard();
        enchant(p, "Fertile Ground", addCard("Forest", p));
        addCard("Island", p);
        // the Forest pays {G}, so the extra mana has to be blue
        assertTrue(canPay(p, "Voidslime"));
        payForReal(p, "G U U");
    }
}
