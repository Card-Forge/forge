package forge.ai;

import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import forge.ai.simulation.XChoice;
import forge.game.Game;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/**
 * The X of a spell or ability in the rollouts (XChoice), for eleven cards with X in their mana cost or ability cost. Each
 * test says what X is right and why; a failure names the card and the X it got. The rule to adjust is in XChoice.
 */
public class FlatMonteCarloXChoiceTest extends AITest {
    private Game game;
    private Player opp;
    private Player ai;

    private void setUp() {
        game = initAndCreateGame();
        opp = game.getPlayers().get(0);
        ai = game.getPlayers().get(1);
        opp.setLife(20, null);
        ai.setLife(20, null);
    }

    private SpellAbility spell(final String name) {
        final Card c = addCardToZone(name, ai, ZoneType.Hand);
        final SpellAbility sa = c.getFirstSpellAbility();
        sa.setActivatingPlayer(ai);
        return sa;
    }

    private void check(final String card, final int expected, final int actual, final String why) {
        AssertJUnit.assertEquals(card + ": " + why + " (got X=" + actual + ")", expected, actual);
    }

    // ---- Chalice of the Void: the X that the AI's own spells do not have

    @Test
    public void chaliceAvoidsTheCostOfItsOwnHand() {
        setUp();
        for (int i = 0; i < 3; i++) {
            addCardToZone("Runeclaw Bear", ai, ZoneType.Hand); // mana value 2
        }
        final SpellAbility chalice = spell("Chalice of the Void");
        check("Chalice of the Void", 1, XChoice.adjust(ai, chalice, 5), "a hand of two drops: 1 hits none of them, 5 hits nothing but wastes the mana");
        check("Chalice of the Void", 0, XChoice.adjust(ai, chalice, 0), "no mana for more");
    }

    @Test
    public void chaliceTakesTwoWhenTheHandIsFullOfOneDrops() {
        setUp();
        for (int i = 0; i < 6; i++) {
            addCardToZone("Gitaxian Probe", ai, ZoneType.Hand); // mana value 1
            addCardToZone("Lightning Bolt", ai, ZoneType.Hand); // mana value 1
        }
        final SpellAbility chalice = spell("Chalice of the Void");
        final int x = XChoice.adjust(ai, chalice, 4);
        AssertJUnit.assertTrue("Chalice of the Void: not 1, the hand and library are full of one drops (got X=" + x + ")", x != 1);
        AssertJUnit.assertTrue("Chalice of the Void: never above 2 (got X=" + x + ")", x <= 2);
    }

    // ---- X damage at one target: lethal and no more

    @Test
    public void fireballStopsAtLethal() {
        setUp();
        opp.setLife(7, null);
        check("Fireball", 7, XChoice.adjust(ai, spell("Fireball"), 12), "7 life: 7 damage kills, the rest of the mana is kept");
        check("Fireball", 4, XChoice.adjust(ai, spell("Fireball"), 4), "never more than can be paid");
    }

    @Test
    public void fireballCountsTheCreaturesItMayHaveToKill() {
        setUp();
        opp.setLife(5, null);
        addCard("Runeclaw Bear", opp); // toughness 2: the damage may be divided between the player and the creature
        check("Fireball", 7, XChoice.adjust(ai, spell("Fireball"), 12), "life 5 and a 2 toughness creature: 7 clears both");
    }

    @Test
    public void banefireAndBlazeStopAtLethal() {
        setUp();
        opp.setLife(6, null);
        check("Banefire", 6, XChoice.adjust(ai, spell("Banefire"), 10), "6 life");
        check("Blaze", 6, XChoice.adjust(ai, spell("Blaze"), 10), "6 life");
        opp.setLife(30, null);
        check("Banefire", 10, XChoice.adjust(ai, spell("Banefire"), 10), "30 life: all the mana, 5 or more also stops the counterspell");
    }

    // ---- X damage to every player: not at the price of the AI's own life

    @Test
    public void earthquakeAndHurricaneNeverKillTheAi() {
        setUp();
        ai.setLife(4, null);
        opp.setLife(20, null);
        check("Earthquake", 3, XChoice.adjust(ai, spell("Earthquake"), 9), "4 life: 3 is the most it can survive");
        check("Hurricane", 3, XChoice.adjust(ai, spell("Hurricane"), 9), "4 life: 3 is the most it can survive");
    }

    @Test
    public void earthquakeIsNotPaidBeyondTheOpponentsLife() {
        setUp();
        ai.setLife(20, null);
        opp.setLife(5, null);
        check("Earthquake", 5, XChoice.adjust(ai, spell("Earthquake"), 9), "the opponent dies at 5");
    }

    @Test
    public void cryptRatsAbilityNeverKillsTheAi() {
        setUp();
        ai.setLife(3, null);
        opp.setLife(20, null);
        final Card rats = addCard("Crypt Rats", ai);
        SpellAbility ability = null;
        for (final SpellAbility sa : rats.getSpellAbilities()) {
            if (sa.isAbility()) {
                ability = sa;
            }
        }
        AssertJUnit.assertNotNull(ability);
        ability.setActivatingPlayer(ai);
        check("Crypt Rats (ability)", 2, XChoice.adjust(ai, ability, 8), "3 life: 2 is the most it can survive");
    }

    // ---- discard X: no more than the hand

    @Test
    public void mindTwistTakesTheHandAndNoMore() {
        setUp();
        final int already = opp.getCardsIn(ZoneType.Hand).size();
        addCardToZone("Runeclaw Bear", opp, ZoneType.Hand);
        addCardToZone("Runeclaw Bear", opp, ZoneType.Hand);
        check("Mind Twist", already + 2, XChoice.adjust(ai, spell("Mind Twist"), already + 9), "a hand of that many cards");
        check("Mind Twist", 1, XChoice.adjust(ai, spell("Mind Twist"), 1), "never more than can be paid");
    }

    // ---- the cards that want every counter: the most mana

    @Test
    public void walkingBallistaHangarbackAndKrasisKeepTheMostMana() {
        setUp();
        check("Walking Ballista", 4, XChoice.adjust(ai, spell("Walking Ballista"), 4), "every counter is a point of damage");
        check("Hangarback Walker", 3, XChoice.adjust(ai, spell("Hangarback Walker"), 3), "every counter is a thopter");
        check("Hydroid Krasis", 5, XChoice.adjust(ai, spell("Hydroid Krasis"), 5), "every counter is size and cards");
    }
}
