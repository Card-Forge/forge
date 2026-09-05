package forge.game.ability.effects;

import org.testng.annotations.Test;

import forge.ai.AITest;
import forge.game.Game;
import forge.game.GameObject;
import forge.game.ability.AbilityUtils;
import forge.game.card.Card;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

import static junit.framework.Assert.assertEquals;

/**
 * Opposition Agent applies only while an opponent searches <i>their own</i> library, so searching
 * someone else's must not hand what is found to the Agent's controller.
 */
public class SearchOwnLibraryTest extends AITest {

    private Game game;
    private Player me; // controls the Agent in every case below
    private Player opp;

    @Test
    public void agentAppliesOnlyWhenAnOpponentSearchesTheirOwnLibrary() {
        // "search target opponent's library" retargets player, leaving me the one searching
        Card ring = newGame("Sol Ring", "Mountain");
        addCards("Island", 6, me);
        cast("Acquire", me, opp);
        assertEquals("my own Acquire must still fetch for me", ZoneType.Battlefield, zoneOf(ring));

        // "search its controller's library" names the victim, but I am still the one looking.
        // Sowing Salt exiles either way, so the tell is whether the Agent claimed what was found.
        newGame("Bojuka Bog", "Mountain");
        addCards("Mountain", 6, me);
        cast("Sowing Salt", me, addCard("Bojuka Bog", opp));
        assertEquals("Sowing Salt must not feed my own Agent", 0, cardsTheAgentTook());

        // "target player searches their library": I cast it, but the Chooser makes them the
        // searcher, so it is still their own search and the Agent takes what they find
        Card forest = newGame("Forest", "Sol Ring");
        addCards("Forest", 6, me);
        cast("Fertilid's Favor", me, opp);
        assertEquals("a search I handed to an opponent is still their own", ZoneType.Exile,
                zoneOf(forest));

        // "any number of target players may each search their library": the target searches, so
        // the fetcher has to be named, not inferred from the caster
        Card basic = newGame("Forest", "Sol Ring");
        cast("Turtle Tracks", me, opp);
        assertEquals("a search the target makes is their own", ZoneType.Exile, zoneOf(basic));

        // and the case the card is for: an opponent tutoring out of their own library
        Card prize = newGame("Sol Ring", "Mountain");
        addCards("Swamp", 6, opp);
        cast("Diabolic Tutor", opp, null);
        assertEquals("their own tutor is still exiled by the Agent", ZoneType.Exile, zoneOf(prize));
    }

    /** Fresh game where I control the Agent and opp's library holds the prize plus filler. */
    private Card newGame(String prize, String filler) {
        game = initAndCreateGame();
        me = game.getPlayers().get(0);
        opp = game.getPlayers().get(1);
        addCard("Opposition Agent", me);
        Card c = addCardToZone(prize, opp, ZoneType.Library);
        for (int i = 0; i < 4; i++) {
            addCardToZone(filler, opp, ZoneType.Library);
        }
        return c;
    }

    /** Casts and resolves {@code name} from {@code p}'s hand, at {@code target} if there is one. */
    private void cast(String name, Player p, GameObject target) {
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, p);
        game.getAction().checkStateEffects(true);
        SpellAbility sa = addCardToZone(name, p, ZoneType.Hand).getFirstSpellAbility();
        sa.setActivatingPlayer(p);
        sa.resetTargets();
        if (target != null) {
            sa.getTargets().add(target);
        }
        AbilityUtils.resolve(sa);
        game.getAction().checkStateEffects(true);
    }

    private ZoneType zoneOf(Card c) {
        return game.getCardState(c, c).getZone().getZoneType();
    }

    /** How many exiled cards the Agent claimed - its replacement is what grants me permission. */
    private int cardsTheAgentTook() {
        int taken = 0;
        for (Card c : opp.getCardsIn(ZoneType.Exile)) {
            if (!c.mayPlay(me).isEmpty()) {
                taken++;
            }
        }
        return taken;
    }
}
