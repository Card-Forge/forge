package forge.ai;

import org.testng.annotations.Test;

import java.util.List;
import java.util.Set;

import forge.card.ColorSet;
import forge.card.MagicColor;
import forge.game.Game;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.zone.ZoneType;

import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertTrue;

/**
 * {@link ComputerUtilCost#getAvailableManaColors} collected the raw {@code Produced$} script
 * string, and every caller runs that through {@link ColorSet#fromNames}, which keeps only colour
 * names - so a source producing {@code Any} contributed nothing and a board of nothing but City of
 * Brass read as unable to pay for anything coloured.
 */
public class AnyColorManaSourceTest extends AITest {

    @Test
    public void anyColorSourcesOfferEveryColor() {
        assertTrue("an any-colour source offers white", canPayWhiteOff("City of Brass"));
        assertTrue("and so does one that costs life instead", canPayWhiteOff("Mana Confluence"));
    }

    @Test
    public void plainSourcesStillOnlyOfferWhatTheyProduce() {
        assertTrue("a white source offers white", canPayWhiteOff("Plains"));
        assertFalse("a blue source does not", canPayWhiteOff("Island"));
    }

    /**
     * Colorless is carried in the returned set as a name of its own. It cannot survive
     * {@link ColorSet}, where {@code MagicColor.COLORLESS} is the empty mask, so callers that only
     * want colors drop it - but it has to be there for the ones that do not.
     */
    @Test
    public void colorlessIsReportedButIsNotAColor() {
        Set<String> off = availableOff("Wastes");
        assertTrue("a {C} source reports colorless", off.contains(MagicColor.Constant.COLORLESS));
        assertFalse("and offers no color", ColorSet.fromNames(off).getColor() != 0);

        // "any color" is five colors and not {C}, so it must not claim colorless
        Set<String> any = availableOff("City of Brass");
        assertFalse("an any-colour source is not a colorless source",
                any.contains(MagicColor.Constant.COLORLESS));
        assertTrue("but it does offer every color",
                ColorSet.fromNames(any).getColor() == MagicColor.ALL_COLORS);
    }

    private Set<String> availableOff(String landName) {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        for (int i = 0; i < 3; i++) {
            addCard(landName, ai).setSickness(false);
        }
        game.getAction().checkStateEffects(true);
        return ComputerUtilCost.getAvailableManaColors(ai, (List<Card>) null);
    }

    /** Can the AI see enough to pay {@code {W}} for Swords to Plowshares off three of this land? */
    private boolean canPayWhiteOff(String landName) {
        Game game = initAndCreateGame();
        Player ai = game.getPlayers().get(1);
        for (int i = 0; i < 3; i++) {
            addCard(landName, ai).setSickness(false);
        }
        Card swords = addCardToZone("Swords to Plowshares", ai, ZoneType.Hand);
        game.getAction().checkStateEffects(true);

        ColorSet available = ColorSet.fromNames(
                ComputerUtilCost.getAvailableManaColors(ai, (List<Card>) null));
        return swords.getManaCost().canBePaidWithAvailable(available.getColor());
    }
}
