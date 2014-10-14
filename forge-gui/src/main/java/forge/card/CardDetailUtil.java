package forge.card;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import org.apache.commons.lang3.StringUtils;

import com.google.common.collect.Sets;

import forge.game.GameView;
import forge.game.card.CardUtil;
import forge.game.card.CardView;
import forge.game.card.CardView.CardStateView;
import forge.game.card.CounterType;
import forge.item.InventoryItemFromSet;
import forge.item.PreconDeck;
import forge.item.SealedProduct;
import forge.match.MatchUtil;
import forge.util.Lang;

public class CardDetailUtil {
    private CardDetailUtil() {
    }

    public enum DetailColors {
        WHITE(254, 253, 244),
        BLUE(90, 146, 202),
        BLACK(32, 34, 31),
        RED(253, 66, 40),
        GREEN(22, 115, 69),
        MULTICOLOR(248, 219, 85),
        COLORLESS(160, 166, 164),
        LAND(190, 153, 112),
        FACE_DOWN(83, 61, 40),
        COMMON(10, 7, 10),
        UNCOMMON(160, 172, 174),
        RARE(193, 170, 100),
        MYTHIC(171, 54, 39),
        SPECIAL(141, 114, 147),
        UNKNOWN(200, 0, 230);

        public final int r, g, b;

        private DetailColors(int r0, int g0, int b0) {
            r = r0;
            g = g0;
            b = b0;
        }
    }

    public static DetailColors getBorderColor(final CardStateView card) {
        if (card == null) {
            return getBorderColors(null, false, false, false).iterator().next();
        }
        return getBorderColors(card.getColors(), card.isLand(), MatchUtil.canCardBeShown(card.getCard()), false).iterator().next();
    }
    public static DetailColors getBorderColor(final ColorSet cardColors, final boolean isLand, boolean canShow) {
        return getBorderColors(cardColors, isLand, canShow, false).get(0);
    }
    public static List<DetailColors> getBorderColors(final CardStateView card) {
        if (card == null) {
            return getBorderColors(null, false, false, true);
        }
        return getBorderColors(card.getColors(), card.isLand(), MatchUtil.canCardBeShown(card.getCard()), true);
    }
    private static List<DetailColors> getBorderColors(final ColorSet cardColors, final boolean isLand, boolean canShow, boolean supportMultiple) {
        List<DetailColors> borderColors = new ArrayList<DetailColors>();

        if (cardColors == null || !canShow) {
            borderColors.add(DetailColors.FACE_DOWN);
        }
        else if (cardColors.isColorless()) {
            if (isLand) { //use different color for lands vs. other colorless cards
                borderColors.add(DetailColors.LAND);
            }
            else {
                borderColors.add(DetailColors.COLORLESS);
            }
        }
        else {
            int colorCount = cardColors.countColors();
            if (colorCount > 2 || (colorCount > 1 && !supportMultiple)) {
                borderColors.add(DetailColors.MULTICOLOR);
            }
            else if (cardColors.hasWhite()) {
                if (colorCount == 1) {
                    borderColors.add(DetailColors.WHITE);
                }
                else if (cardColors.hasBlue()) {
                    borderColors.add(DetailColors.WHITE);
                    borderColors.add(DetailColors.BLUE);
                }
                else if (cardColors.hasBlack()) {
                    borderColors.add(DetailColors.WHITE);
                    borderColors.add(DetailColors.BLACK);
                }
                else if (cardColors.hasRed()) {
                    borderColors.add(DetailColors.RED);
                    borderColors.add(DetailColors.WHITE);
                }
                else if (cardColors.hasGreen()) {
                    borderColors.add(DetailColors.GREEN);
                    borderColors.add(DetailColors.WHITE);
                }
            }
            else if (cardColors.hasBlue()) {
                if (colorCount == 1) {
                    borderColors.add(DetailColors.BLUE);
                }
                else if (cardColors.hasBlack()) {
                    borderColors.add(DetailColors.BLUE);
                    borderColors.add(DetailColors.BLACK);
                }
                else if (cardColors.hasRed()) {
                    borderColors.add(DetailColors.BLUE);
                    borderColors.add(DetailColors.RED);
                }
                else if (cardColors.hasGreen()) {
                    borderColors.add(DetailColors.GREEN);
                    borderColors.add(DetailColors.BLUE);
                }
            }
            else if (cardColors.hasBlack()) {
                if (colorCount == 1) {
                    borderColors.add(DetailColors.BLACK);
                }
                else if (cardColors.hasRed()) {
                    borderColors.add(DetailColors.BLACK);
                    borderColors.add(DetailColors.RED);
                }
                else if (cardColors.hasGreen()) {
                    borderColors.add(DetailColors.BLACK);
                    borderColors.add(DetailColors.GREEN);
                }
            }
            else if (cardColors.hasRed()) { //if we got this far, must be mono-red or red-green
                borderColors.add(DetailColors.RED);
                if (cardColors.hasGreen()) {
                    borderColors.add(DetailColors.GREEN);
                }
            }
            else if (cardColors.hasGreen()) { //if we got this far, must be mono-green
                borderColors.add(DetailColors.GREEN);
            }
        }

        if (borderColors.isEmpty()) { // If your card has a violet border, something is wrong
            borderColors.add(DetailColors.UNKNOWN);
        }
        return borderColors;
    }

    public static String getItemDescription(final InventoryItemFromSet item) {
        if (item instanceof SealedProduct) {
            return ((SealedProduct)item).getDescription();
        }
        if (item instanceof PreconDeck) {
            return ((PreconDeck) item).getDescription();
        }
        return item.getName(); 
    }

    public static String formatCardName(final CardStateView card) {
        final String name = card.getName();
        return StringUtils.isEmpty(name) ? "???" : name.trim();
    }

    public static String formatCardType(final CardStateView card) {
        return card.getType().toString();
    }

    public static String formatPowerToughness(final CardStateView card) {
        StringBuilder ptText = new StringBuilder();
        if (card.isCreature()) {
            ptText.append(card.getPower()).append(" / ").append(card.getToughness());
        }

        if (card.isPlaneswalker()) {
            if (ptText.length() > 0) {
                ptText.insert(0, "P/T: ");
                ptText.append(" - ").append("Loy: ");
            }
            else {
                ptText.append("Loyalty: ");
            }

            ptText.append(card.getLoyalty());
        }
        return ptText.toString();
    }

    public static String formatCardId(final CardStateView card) {
        final int id = card.getCard().getId();
        return id > 0 ? "[" + id + "]" : "";
    }

    public static String composeCardText(final CardStateView state) {
        final CardView card = state.getCard();
        final StringBuilder area = new StringBuilder();

        // Token
        if (card.isToken()) {
            area.append("Token");
        }

        // card text
        if (area.length() != 0) {
            area.append("\n");
        }
        String text = card.getText(state);
        // LEVEL [0-9]+-[0-9]+
        // LEVEL [0-9]+\+

        String regex = "LEVEL [0-9]+-[0-9]+ ";
        text = text.replaceAll(regex, "$0\r\n");

        regex = "LEVEL [0-9]+\\+ ";
        text = text.replaceAll(regex, "\r\n$0\r\n");

        // displays keywords that have dots in them a little better:
        regex = "\\., ";
        text = text.replaceAll(regex, ".\r\n");

        area.append(text);

        if (card.isPhasedOut()) {
            if (area.length() != 0) {
                area.append("\n");
            }
            area.append("Phased Out");
        }

        // text changes
        final Map<String, String> changedColorWords = card.getChangedColorWords();
        final Map<String, String> changedTypes = card.getChangedTypes();
        if (!(changedColorWords.isEmpty() && changedTypes.isEmpty())) {
            if (area.length() != 0) {
                area.append("\n");
            }
        }
        for (final Entry<String, String> e : Sets.union(changedColorWords.entrySet(), changedTypes.entrySet())) {
            // ignore lower case and plural form keys, to avoid duplicity
            if (Character.isUpperCase(e.getKey().charAt(0)) && 
                    !CardUtil.singularTypes.containsKey(e.getKey())) {
                area.append("Text changed: all instances of ");
                if (e.getKey().equals("Any")) {
                    if (changedColorWords.containsKey(e.getKey())) {
                        area.append("color words");
                    } else if (forge.card.CardType.getBasicTypes().contains(e.getValue())) {
                        area.append("basic land types");
                    } else {
                        area.append("creature types");
                    }
                } else {
                    area.append(e.getKey());
                }
                area.append(" are replaced by ");
                area.append(e.getValue());
                area.append(".\n");
            }
        }

        // counter text
        if (card.getCounters() != null) {
            for (final Entry<CounterType, Integer> c : card.getCounters().entrySet()) {
                if (c.getValue().intValue() != 0) {
                    if (area.length() != 0) {
                        area.append("\n");
                    }
                    area.append(c.getKey().getName() + " counters: ");
                    area.append(c.getValue());
                }
            }
        }

        if (state.isCreature()) {
            final int damage = card.getDamage();
            if (damage > 0) {
                if (area.length() != 0) {
                    area.append("\n");
                }
                area.append("Damage: " + damage);
            }
        }
        if (state.isCreature() || state.isPlaneswalker()) {
            final int assigned = card.getAssignedDamage();
            if (assigned > 0) {
                if (area.length() != 0) {
                    area.append("\n");
                }
                area.append("Assigned Damage: " + assigned);
            }
        }

        // Regeneration Shields
        final int regenShields = card.getShieldCount();
        if (regenShields > 0) {
            if (area.length() != 0) {
                area.append("\n");
            }
            area.append("Regeneration Shields: ").append(regenShields);
        }

        // Damage Prevention
        final int preventNextDamage = card.getPreventNextDamage();
        if (preventNextDamage > 0) {
            area.append("\n");
            area.append("Prevent the next ").append(preventNextDamage).append(" damage that would be dealt to ");
            area.append(state.getName()).append(" this turn.");
        }

        // chosen type
        if (!card.getChosenType().equals("")) {
            if (area.length() != 0) {
                area.append("\n");
            }
            area.append("(chosen type: ");
            area.append(card.getChosenType());
            area.append(")");
        }

        // chosen color
        if (card.getChosenColors() != null) {
            if (area.length() != 0) {
                area.append("\n");
            }
            area.append("(chosen colors: ");
            area.append(Lang.joinHomogenous(card.getChosenColors()));
            area.append(")");
        }

        // chosen player
        if (card.getChosenPlayer() != null) {
            if (area.length() != 0) {
                area.append("\n");
            }
            area.append("(chosen player: " + card.getChosenPlayer() + ")");
        }

        // named card
        if (!card.getNamedCard().equals("")) {
            if (area.length() != 0) {
                area.append("\n");
            }
            area.append("(named card: ");
            area.append(card.getNamedCard());
            area.append(")");
        }

        // equipping
        if (card.getEquipping() != null) {
            if (area.length() != 0) {
                area.append("\n");
            }
            area.append("=Equipping ");
            area.append(card.getEquipping());
            area.append("=");
        }

        // equipped by
        if (card.isEquipped()) {
            if (area.length() != 0) {
                area.append("\n");
            }
            area.append("=Equipped by ");
            for (final Iterator<CardView> it = card.getEquippedBy().iterator(); it.hasNext();) {
                area.append(it.next());
                if (it.hasNext()) {
                    area.append(", ");
                }
            }
            area.append("=");
        }

        // enchanting
        if (card.getEnchantingCard() != null) {
            if (area.length() != 0) {
                area.append("\n");
            }
            area.append("*Enchanting ").append(card.getEnchantingCard()).append("*");
        }
        if (card.getEnchantingPlayer() != null) {
            if (area.length() != 0) {
                area.append("\n");
            }
            area.append("*Enchanting ").append(card.getEnchantingPlayer()).append("*");
        }

        // enchanted by
        if (card.isEnchanted()) {
            if (area.length() != 0) {
                area.append("\n");
            }
            area.append("*Enchanted by ");
            for (final Iterator<CardView> it = card.getEnchantedBy().iterator(); it.hasNext();) {
                area.append(it.next());
                if (it.hasNext()) {
                    area.append(", ");
                }
            }
            area.append("*");
        }

        // controlling
        if (card.getGainControlTargets() != null) {
            if (area.length() != 0) {
                area.append("\n");
            }
            area.append("+Controlling: ");
            for (final Iterator<CardView> it = card.getGainControlTargets().iterator(); it.hasNext();) {
                area.append(it.next());
                if (it.hasNext()) {
                    area.append(", ");
                }
            }
            area.append("+");
        }

        // cloned via
        if (card.getCloneOrigin() != null) {
            if (area.length() != 0) {
                area.append("\n");
            }
            area.append("^Cloned via: ");
            area.append(card.getCloneOrigin().getCurrentState().getName());
            area.append("^");
        }

        // Imprint
        if (card.getImprintedCards() != null) {
            if (area.length() != 0) {
                area.append("\n");
            }
            area.append("Imprinting: ");
            for (final Iterator<CardView> it = card.getImprintedCards().iterator(); it.hasNext();) {
                area.append(it.next());
                if (it.hasNext()) {
                    area.append(", ");
                }
            }
        }

        // Haunt
        if (card.getHauntedBy() != null) {
            if (area.length() != 0) {
                area.append("\n");
            }
            area.append("Haunted by: ");
            for (final Iterator<CardView> it = card.getHauntedBy().iterator(); it.hasNext();) {
                area.append(it.next());
                if (it.hasNext()) {
                    area.append(", ");
                }
            }
        }
        if (card.getHaunting() != null) {
            if (area.length() != 0) {
                area.append("\n");
            }
            area.append("Haunting " + card.getHaunting());
        }

        // must block
        if (card.getMustBlockCards() != null) {
            if (area.length() != 0) {
                area.append("\n");
            }
            final String mustBlockThese = Lang.joinHomogenous(card.getMustBlockCards());
            area.append("Must block " + mustBlockThese);
        }

        //show current storm count for storm cards
        if (state.hasStorm()) {
            GameView gameView = MatchUtil.getGameView();
            if (gameView != null) {
                if (area.length() != 0) {
                    area.append("\n\n");
                }
                area.append("Current Storm Count: " + gameView.getStormCount());
            }
        }
        return area.toString();
    }
}
