package forge.game.card;

import org.testng.annotations.Test;

import forge.ai.AITest;
import forge.game.ability.AbilityFactory;
import forge.game.ability.AbilityUtils;
import forge.game.Game;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertTrue;

/**
 * Assimilate moves a card between zones and changes its types, so the two halves can break
 * independently: the card can arrive under the wrong controller, or arrive correctly and keep
 * the types it should have shed.
 */
public class AssimilateTest extends AITest {

    private static final String QUEEN = "Borg Queen, Perfection Manifest";

    private void enterQueen(Game game, Player ai) {
        // settle the rest of the board first, so anything already out has its triggers registered
        game.getAction().checkStateEffects(true);
        Card queen = addCardToZone(QUEEN, ai, ZoneType.Hand);
        game.getAction().moveTo(ZoneType.Battlefield, queen, null, null);
        game.getAction().checkStateEffects(true);
        playUntilStackClear(game);
    }

    @Test
    public void assimilatedCreatureArrivesAsABorgArtifactUnderOurControl() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);

        addCardToZone("Grizzly Bears", opp, ZoneType.Graveyard);

        enterQueen(game, ai);

        Card bears = findCardWithName(game, "Grizzly Bears");
        assertEquals(ZoneType.Battlefield, bears.getZone().getZoneType());
        assertEquals(ai, bears.getController());
        assertEquals(1, bears.getCounters(CounterEnumType.P1P1));

        assertTrue("is an artifact", bears.isArtifact());
        assertTrue("is a creature", bears.isCreature());
        assertTrue("is a Borg", bears.getType().hasCreatureType("Borg"));
        assertFalse("lost its other creature types", bears.getType().hasCreatureType("Bear"));
    }

    @Test
    public void theQueenPumpsWhatSheAssimilated() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);

        addCardToZone("Grizzly Bears", opp, ZoneType.Graveyard);

        enterQueen(game, ai);

        // 2/2 base, +1/+1 counter, then +2/+0 for being an artifact creature we control
        Card bears = findCardWithName(game, "Grizzly Bears");
        assertEquals(5, bears.getNetPower());
        assertEquals(3, bears.getNetToughness());
    }

    @Test
    public void itIsAlreadyAnArtifactWhenItEnters() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);

        addCard("Reckless Fireweaver", ai);
        addCardToZone("Grizzly Bears", opp, ZoneType.Graveyard);

        enterQueen(game, ai);

        // one for the Queen, who is an artifact herself, and one for what she assimilated
        assertEquals(18, opp.getLife());
    }

    @Test
    public void anEmptyGraveyardIsNotAProblem() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);

        enterQueen(game, ai);

        Card queen = findCardWithName(game, QUEEN);
        assertEquals(ZoneType.Battlefield, queen.getZone().getZoneType());
        assertEquals(3, queen.getNetPower()); // her own static applies to her
        assertEquals(4, queen.getNetToughness());
    }

    @Test
    public void itKeepsCardTypesAndOnlyShedsCreatureTypes() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);

        // an enchantment creature: the oracle sheds creature types, not card types
        addCardToZone("Nyx-Fleece Ram", opp, ZoneType.Graveyard);

        enterQueen(game, ai);

        Card ram = findCardWithName(game, "Nyx-Fleece Ram");
        assertTrue("still an enchantment", ram.isEnchantment());
        assertTrue("now an artifact too", ram.isArtifact());
        assertTrue("is a Borg", ram.getType().hasCreatureType("Borg"));
        assertFalse("lost Sheep", ram.getType().hasCreatureType("Sheep"));
    }

    /**
     * No printed card assimilates a permanent yet, so this drives the API directly. It is the half
     * of the effect that exists on speculation, and it is the half a future card would use.
     */
    @Test
    public void assimilatingAPermanentTakesItWhereItStands() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);

        Card host = addCard(QUEEN, ai);
        Card bears = addCard("Grizzly Bears", opp);
        game.getAction().checkStateEffects(true);

        SpellAbility sa = AbilityFactory.getAbility(
                "DB$ Assimilate | ValidTgts$ Creature.OppCtrl", host);
        sa.setActivatingPlayer(ai);
        sa.getTargets().add(bears);
        AbilityUtils.resolve(sa);
        game.getAction().checkStateEffects(true);

        assertEquals("stays on the battlefield", ZoneType.Battlefield, bears.getZone().getZoneType());
        assertEquals("changes hands", ai, bears.getController());
        assertEquals(1, bears.getCounters(CounterEnumType.P1P1));
        assertTrue("is an artifact", bears.isArtifact());
        assertTrue("is a Borg", bears.getType().hasCreatureType("Borg"));
        assertFalse("lost Bear", bears.getType().hasCreatureType("Bear"));
        // 2/2 base, +1/+1 counter, +2/+0 now that it is our artifact creature
        assertEquals(5, bears.getNetPower());
    }

    @Test
    public void aCreatureOnTheirBattlefieldIsNotReachable() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);

        // the Angel is the better body, so it gets taken if the zone restriction is not honoured
        Card angel = addCard("Serra Angel", opp);
        addCardToZone("Grizzly Bears", opp, ZoneType.Graveyard);

        enterQueen(game, ai);

        assertEquals(1, countCardsWithName(game, "Grizzly Bears", ZoneType.Battlefield));
        assertEquals("the Angel stays theirs", opp, angel.getController());
    }

    @Test
    public void onlyAnOpponentsGraveyardIsAssimilated() {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        Player opp = game.getPlayers().get(0);

        // the better creature is ours, so a target chosen on power alone would take the wrong one
        addCardToZone("Serra Angel", ai, ZoneType.Graveyard);
        addCardToZone("Grizzly Bears", opp, ZoneType.Graveyard);

        enterQueen(game, ai);

        assertEquals(1, countCardsWithName(game, "Grizzly Bears", ZoneType.Battlefield));
        assertEquals(1, countCardsWithName(game, "Serra Angel", ZoneType.Graveyard));
    }
}
