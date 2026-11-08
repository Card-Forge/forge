package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.ai.simulation.FlatMonteCarloPicker;
import forge.game.Game;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/**
 * Activated abilities of permanents that are not Planeswalkers, for discovery: firebreathing, a pinger, an equip. They are
 * offered to the rollouts when they are not idle and the mana they cost is spare; they never take the mana a card in hand
 * needs this turn.
 */
public class FlatMonteCarloActivatedAbilityTest extends AITest {
    private Game game;
    private Player ai;

    private void setUp() {
        game = initAndCreateGame();
        game.AI_FLAT_MONTE_CARLO = true;
        game.AI_TIMEOUT = 10;
        ai = game.getPlayers().get(1);
    }

    private SpellAbility activated(final Card c) {
        for (final SpellAbility sa : c.getSpellAbilities()) {
            if (sa.isActivatedAbility() && !sa.isManaAbility()) {
                sa.setActivatingPlayer(ai);
                return sa;
            }
        }
        throw new IllegalStateException("no activated ability on " + c);
    }

    private SpellAbility mine(final String name) {
        return activated(addCardToZone(name, ai, ZoneType.Battlefield));
    }

    @Test
    public void firebreathingIsNotOfferedInTheMainPhase() {
        setUp();
        addCards("Mountain", 4, ai);
        final SpellAbility pump = mine("Shivan Dragon");
        AssertJUnit.assertFalse("the opponent would see it before he blocks", FlatMonteCarloPicker.isDiscoverable(pump));
        AssertJUnit.assertTrue("it is a trick for after the blocks", FlatMonteCarloPicker.isCombatTrick(pump));
    }

    @Test
    public void anEquipWaitsForACreatureInHand() {
        setUp();
        addCards("Plains", 2, ai);
        final SpellAbility equip = mine("Short Sword");
        addCardToZone("Grizzly Bears", ai, ZoneType.Battlefield);
        addCardToZone("Grizzly Bears", ai, ZoneType.Hand); // costs the two
        AssertJUnit.assertFalse("the mana is for the second Bears", FlatMonteCarloPicker.isDiscoverable(equip));
    }

    @Test
    public void anEquipIsDiscoveredWhenTheCreatureStillFits() {
        setUp();
        addCards("Plains", 3, ai);
        final SpellAbility equip = mine("Short Sword");
        addCardToZone("Grizzly Bears", ai, ZoneType.Battlefield);
        addCardToZone("Grizzly Bears", ai, ZoneType.Hand);
        AssertJUnit.assertTrue("three lands: the Bears and the equip", FlatMonteCarloPicker.isDiscoverable(equip));
    }

    @Test
    public void aFreePingerIsDiscovered() {
        setUp();
        AssertJUnit.assertTrue("{T}: 1 damage to any target", FlatMonteCarloPicker.isDiscoverable(mine("Prodigal Sorcerer")));
    }

    @Test
    public void anEquipIsDiscoveredAndAimedAtTheAisCreature() {
        setUp();
        addCards("Plains", 2, ai);
        final SpellAbility equip = mine("Short Sword");
        final Card bears = addCardToZone("Grizzly Bears", ai, ZoneType.Battlefield);
        addCardToZone("Hill Giant", game.getPlayers().get(0), ZoneType.Battlefield);
        AssertJUnit.assertTrue(FlatMonteCarloPicker.isDiscoverable(equip));
        AssertJUnit.assertEquals(bears, FlatMonteCarloPicker.loyaltyTarget(ai, equip));
    }

    @Test
    public void anEquipWithNoCreatureIsNotDiscovered() {
        setUp();
        addCards("Plains", 2, ai);
        AssertJUnit.assertFalse("nothing to equip", FlatMonteCarloPicker.isDiscoverable(mine("Short Sword")));
    }

    @Test
    public void anAbilityThatCannotBePaidForIsNotDiscovered() {
        setUp();
        AssertJUnit.assertFalse("no land for the {R}", FlatMonteCarloPicker.isDiscoverable(mine("Shivan Dragon")));
    }

    // ------------------------------------------------------------------------------------------ after the blocks

    @Test
    public void tricksAreTheKindsOfPlayAfterTheBlocks() {
        setUp();
        addCards("Mountain", 4, ai);
        addCardToZone("Giant Growth", ai, ZoneType.Hand);
        addCardToZone("Lightning Bolt", ai, ZoneType.Hand);
        addCardToZone("Grizzly Bears", ai, ZoneType.Hand);
        for (final Card c : ai.getCardsIn(ZoneType.Hand)) {
            final SpellAbility sa = c.getFirstSpellAbility();
            sa.setActivatingPlayer(ai);
            final boolean expected = !c.getName().equals("Grizzly Bears");
            AssertJUnit.assertEquals(c.getName(), expected, FlatMonteCarloPicker.isCombatTrick(sa));
        }
    }

    @Test
    public void theTrickIsForTheBlockedAttacker() {
        setUp();
        final Player opp = game.getPlayers().get(0);
        final Card blocked = addCardToZone("Grizzly Bears", ai, ZoneType.Battlefield);
        final Card free = addCardToZone("Vampire Nighthawk", ai, ZoneType.Battlefield);
        addCardToZone("Serra Angel", ai, ZoneType.Battlefield); // at home
        final Card giant = addCardToZone("Hill Giant", opp, ZoneType.Battlefield);
        final forge.game.combat.Combat combat = new forge.game.combat.Combat(ai);
        combat.addAttacker(blocked, opp);
        combat.addAttacker(free, opp);
        combat.addBlocker(blocked, giant);
        game.getPhaseHandler().setCombat(combat);
        final Card growth = addCardToZone("Giant Growth", ai, ZoneType.Hand);
        final SpellAbility trick = growth.getFirstSpellAbility();
        trick.setActivatingPlayer(ai);
        AssertJUnit.assertEquals("the one the Giant blocks, not the free one or the one at home", blocked,
                FlatMonteCarloPicker.combatTarget(ai, trick));
    }

    @Test
    public void withNoAttackerThereIsNoTarget() {
        setUp();
        addCardToZone("Grizzly Bears", ai, ZoneType.Battlefield);
        game.getPhaseHandler().setCombat(new forge.game.combat.Combat(ai));
        final SpellAbility trick = addCardToZone("Giant Growth", ai, ZoneType.Hand).getFirstSpellAbility();
        trick.setActivatingPlayer(ai);
        AssertJUnit.assertNull(FlatMonteCarloPicker.combatTarget(ai, trick));
    }

    // ------------------------------------------------------------------------------------------ other zones

    private SpellAbility from(final String name, final ZoneType zone) {
        return activated(addCardToZone(name, ai, zone));
    }

    @Test
    public void unearthIsDiscoveredWithSpareMana() {
        setUp();
        addCards("Swamp", 2, ai);
        AssertJUnit.assertTrue(FlatMonteCarloPicker.isDiscoverable(from("Dregscape Zombie", ZoneType.Graveyard)));
    }

    @Test
    public void unearthWaitsForACreatureInHand() {
        setUp();
        addCards("Swamp", 3, ai);
        final SpellAbility unearth = from("Dregscape Zombie", ZoneType.Graveyard);
        addCardToZone("Vampire Nighthawk", ai, ZoneType.Hand); // the three
        AssertJUnit.assertFalse("the mana is for the Nighthawk", FlatMonteCarloPicker.isDiscoverable(unearth));
    }

    @Test
    public void unearthIsDiscoveredWhenTheCreatureStillFits() {
        setUp();
        addCards("Swamp", 4, ai);
        final SpellAbility unearth = from("Dregscape Zombie", ZoneType.Graveyard);
        addCardToZone("Vampire Nighthawk", ai, ZoneType.Hand);
        AssertJUnit.assertTrue(FlatMonteCarloPicker.isDiscoverable(unearth));
    }

    @Test
    public void embalmIsDiscovered() {
        setUp();
        addCards("Plains", 1, ai);
        AssertJUnit.assertTrue(FlatMonteCarloPicker.isDiscoverable(from("Sacred Cat", ZoneType.Graveyard)));
    }

    @Test
    public void eternalizeNeedsItsFiveMana() {
        setUp();
        addCards("Plains", 3, ai);
        final SpellAbility eternalize = from("Adorned Pouncer", ZoneType.Graveyard);
        AssertJUnit.assertFalse("three lands for a {3}{W}{W}", FlatMonteCarloPicker.isDiscoverable(eternalize));
        addCards("Plains", 2, ai);
        AssertJUnit.assertTrue("five lands", FlatMonteCarloPicker.isDiscoverable(eternalize));
    }

    @Test
    public void scavengeIsDiscoveredAndAimedAtTheAisCreature() {
        setUp();
        final SpellAbility scavenge = from("Slitherhead", ZoneType.Graveyard);
        final Card bears = addCardToZone("Grizzly Bears", ai, ZoneType.Battlefield);
        addCardToZone("Hill Giant", game.getPlayers().get(0), ZoneType.Battlefield);
        AssertJUnit.assertTrue(FlatMonteCarloPicker.isDiscoverable(scavenge));
        AssertJUnit.assertEquals(bears, FlatMonteCarloPicker.loyaltyTarget(ai, scavenge));
    }

    @Test
    public void ninjutsuIsATrickForAfterTheBlocks() {
        setUp();
        addCards("Island", 2, ai);
        final SpellAbility ninjutsu = from("Ninja of the Deep Hours", ZoneType.Hand);
        AssertJUnit.assertTrue(ninjutsu.isNinjutsu());
        AssertJUnit.assertTrue(FlatMonteCarloPicker.isCombatTrick(ninjutsu));
        AssertJUnit.assertFalse("not in the main phase", FlatMonteCarloPicker.isDiscoverable(ninjutsu));
    }

    @Test
    public void bloodrushIsATrickForAfterTheBlocks() {
        setUp();
        addCards("Mountain", 1, ai);
        addCards("Forest", 1, ai);
        final SpellAbility bloodrush = from("Ghor-Clan Rampager", ZoneType.Hand);
        AssertJUnit.assertTrue(FlatMonteCarloPicker.isCombatTrick(bloodrush));
        AssertJUnit.assertFalse("not in the main phase", FlatMonteCarloPicker.isDiscoverable(bloodrush));
    }

    // ------------------------------------------------------------------------------------------ the rest of the kinds

    @Test
    public void aTokenMakerIsDiscoveredWithSpareMana() {
        setUp();
        addCards("Forest", 2, ai);
        AssertJUnit.assertTrue("{1}{G}: a 1/1", FlatMonteCarloPicker.isDiscoverable(mine("Ant Queen")));
    }

    @Test
    public void aTokenMakerWaitsForACreatureInHand() {
        setUp();
        addCards("Forest", 2, ai);
        final SpellAbility token = mine("Ant Queen");
        addCardToZone("Grizzly Bears", ai, ZoneType.Hand);
        AssertJUnit.assertFalse("the two mana are for the Bears", FlatMonteCarloPicker.isDiscoverable(token));
    }

    @Test
    public void aLifeDrainIsDiscoveredWithSpareMana() {
        setUp();
        addCards("Swamp", 2, ai);
        AssertJUnit.assertTrue("{1}{B},{T}: target player loses 1", FlatMonteCarloPicker.isDiscoverable(mine("Acolyte of Xathrid")));
    }

    @Test
    public void aLifeDrainIsAimedAtTheOpponent() {
        setUp();
        addCards("Swamp", 2, ai);
        final SpellAbility drain = mine("Acolyte of Xathrid");
        AssertJUnit.assertEquals(game.getPlayers().get(0), FlatMonteCarloPicker.loyaltyTarget(ai, drain));
    }

    @Test
    public void aDiscardAbilityIsAimedAtTheOpponent() {
        setUp();
        addCards("Plains", 3, ai);
        final SpellAbility discard = mine("Disrupting Scepter");
        AssertJUnit.assertTrue(FlatMonteCarloPicker.isDiscoverable(discard));
        AssertJUnit.assertEquals(game.getPlayers().get(0), FlatMonteCarloPicker.loyaltyTarget(ai, discard));
    }

    @Test
    public void aChannelAbilityIsDiscoveredAsRemoval() {
        setUp();
        addCards("Forest", 2, ai);
        final SpellAbility channel = from("Boseiju, Who Endures", ZoneType.Hand);
        AssertJUnit.assertTrue("channel: destroy an artifact, enchantment or nonbasic land", FlatMonteCarloPicker.isDiscoverable(channel));
    }
}
