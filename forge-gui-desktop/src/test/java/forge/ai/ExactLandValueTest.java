package forge.ai;

import forge.card.MagicColor;
import forge.game.Game;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;
import org.testng.annotations.Test;

import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

/** What one more land of a colour would actually let the AI cast. */
public class ExactLandValueTest extends AITest {

    private SpellAbility spellOf(Player p, String name) {
        for (forge.game.card.Card c : p.getCardsIn(ZoneType.Hand)) {
            if (c.getName().equals(name)) {
                for (SpellAbility sa : c.getAllPossibleAbilities(p, false)) {
                    if (sa.isSpell()) {
                        return sa;
                    }
                }
            }
        }
        throw new IllegalStateException("no spell for " + name);
    }

    /** A second colored pip needs a second source, not just any source of that colour. */
    @Test
    public void doubleShardNeedsTwoSources() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);
        addCards("Forest", 3, p);
        addCards("Swamp", 1, p);
        addCardToZone("Abyssal Harvester", p, ZoneType.Hand); // {1}{B}{B}
        game.getAction().checkStateEffects(true);
        SpellAbility sa = spellOf(p, "Abyssal Harvester");

        assertFalse(ComputerUtilCost.isPayableWith(sa, p, null),
                "one Swamp cannot pay {B}{B}");
        assertTrue(ComputerUtilCost.isPayableWith(sa, p, MagicColor.Color.BLACK),
                "a second black source casts it");
        assertFalse(ComputerUtilCost.isPayableWith(sa, p, MagicColor.Color.GREEN),
                "a fourth Forest still leaves the second black pip unpaid");
    }

    /** Enough mana is not enough on its own: the colour has to line up too. */
    @Test
    public void landMakesItCastableNow() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);
        addCards("Forest", 1, p);
        addCardToZone("Doom Blade", p, ZoneType.Hand); // {1}{B}
        game.getAction().checkStateEffects(true);
        SpellAbility sa = spellOf(p, "Doom Blade");

        assertFalse(ComputerUtilCost.isPayableWith(sa, p, null), "not castable as things stand");
        assertTrue(ComputerUtilCost.isPayableWith(sa, p, MagicColor.Color.BLACK),
                "Forest plus a new Swamp casts it");
        assertFalse(ComputerUtilCost.isPayableWith(sa, p, MagicColor.Color.GREEN),
                "a second Forest cannot pay the black pip");
    }

    /** One land is one mana: a colour fix does not help what is still turns away. */
    @Test
    public void oneLandCannotBridgeABigGap() {
        Game game = initAndCreateGame();
        Player p = game.getPlayers().get(1);
        addCards("Forest", 1, p);
        addCardToZone("Angel of Mercy", p, ZoneType.Hand); // {4}{W}
        game.getAction().checkStateEffects(true);
        SpellAbility sa = spellOf(p, "Angel of Mercy");

        assertFalse(ComputerUtilCost.isPayableWith(sa, p, MagicColor.Color.WHITE),
                "a Plains fixes the colour but two lands is not five mana");
    }
}
