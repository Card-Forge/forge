package forge.card;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.google.common.collect.Sets;

import forge.GuiBase;
import forge.game.Game;
import forge.game.GameEntity;
import forge.game.card.Card;
import forge.game.card.CardUtil;
import forge.game.card.CounterType;
import forge.game.player.Player;
import forge.game.zone.ZoneType;
import forge.item.InventoryItemFromSet;
import forge.item.PreconDeck;
import forge.item.SealedProduct;
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

    public static DetailColors getBorderColor(final Card card, boolean canShow) {
        return getBorderColors(card.determineColor(), card.isLand(), canShow, false).get(0);
    }
    public static DetailColors getBorderColor(final ColorSet cardColors, final boolean isLand, boolean canShow) {
        return getBorderColors(cardColors, isLand, canShow, false).get(0);
    }
    public static List<DetailColors> getBorderColors(final Card card, boolean canShow, boolean supportMultiple) {
        return getBorderColors(card.determineColor(), card.isLand(), canShow, supportMultiple);
    }
    public static List<DetailColors> getBorderColors(final ColorSet cardColors, final boolean isLand, boolean canShow, boolean supportMultiple) {
        List<DetailColors> borderColors = new ArrayList<DetailColors>();

        if (!canShow) {
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

    public static String formatCardType(final Card card) {
        final ArrayList<String> list = card.getType();
        final StringBuilder sb = new StringBuilder();
    
        final ArrayList<String> superTypes = new ArrayList<String>();
        final ArrayList<String> cardTypes = new ArrayList<String>();
        final ArrayList<String> subTypes = new ArrayList<String>();
        final boolean allCreatureTypes = list.contains("AllCreatureTypes");
    
        for (final String t : list) {
            if (allCreatureTypes && t.equals("AllCreatureTypes")) {
                continue;
            }
            if (CardType.isASuperType(t) && !superTypes.contains(t)) {
                superTypes.add(t);
            }
            if (CardType.isACardType(t) && !cardTypes.contains(t)) {
                cardTypes.add(t);
            }
            if (CardType.isASubType(t) && !subTypes.contains(t) && (!allCreatureTypes || !CardType.isACreatureType(t))) {
                subTypes.add(t);
            }
        }
    
        for (final String type : superTypes) {
            sb.append(type).append(" ");
        }
        for (final String type : cardTypes) {
            sb.append(type).append(" ");
        }
        if (!subTypes.isEmpty() || allCreatureTypes) {
            sb.append("- ");
        }
        if (allCreatureTypes) {
            sb.append("All creature types ");
        }
        for (final String type : subTypes) {
            sb.append(type).append(" ");
        }
    
        return sb.toString();
    }

    public static String formatPowerToughness(final Card card) {
        StringBuilder ptText = new StringBuilder();
        if (card.isCreature()) {
            ptText.append(card.getNetAttack()).append(" / ").append(card.getNetDefense());
        }

        if (card.isPlaneswalker()) {
            if (ptText.length() > 0) {
                ptText.insert(0, "P/T: ");
                ptText.append(" - ").append("Loy: ");
            }
            else {
                ptText.append("Loyalty: ");
            }

            ptText.append(card.getCurrentLoyalty());
        }
        return ptText.toString();
    }
    
    public static String formatCardId(final Card card) {
        return card.getUniqueNumber() > 0 ? "[" + card.getUniqueNumber() + "]" : "";
    }

    public static String composeCardText(final Card card, final boolean canShow) {
        final StringBuilder area = new StringBuilder();

        // Token
        if (card.isToken()) {
            area.append("Token");
        }

        if (canShow) {
            // card text
            if (area.length() != 0) {
                area.append("\n");
            }
            String text = card.getText();
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
        }

        if (card.isPhasedOut()) {
            if (area.length() != 0) {
                area.append("\n");
            }
            area.append("Phased Out");
        }

        // text changes
        final Map<String, String> changedColorWords = card.getChangedTextColorWords(),
                changedTypes = card.getChangedTextTypeWords();
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
        final CounterType[] counters = CounterType.values();
        for (final CounterType counter : counters) {
            if (card.getCounters(counter) != 0) {
                if (area.length() != 0) {
                    area.append("\n");
                }
                area.append(counter.getName() + " counters: ");
                area.append(card.getCounters(counter));
            }
        }

        if (card.isCreature()) {
            int damage = card.getDamage();
            if (damage > 0) {
                if (area.length() != 0) {
                    area.append("\n");
                }
                area.append("Damage: " + damage);
            }
            int assigned = card.getTotalAssignedDamage();
            if (assigned > 0) {
                if (area.length() != 0) {
                    area.append("\n");
                }
                area.append("Assigned Damage: " + assigned);
            }
        }
        if (card.isPlaneswalker()) {
            int assigned = card.getTotalAssignedDamage();
            if (assigned > 0) {
                if (area.length() != 0) {
                    area.append("\n");
                }
                area.append("Assigned Damage: " + assigned);
            }
        }

        // Regeneration Shields
        final int regenShields = card.getShield().size();
        if (regenShields > 0) {
            if (area.length() != 0) {
                area.append("\n");
            }
            area.append("Regeneration Shield(s): ").append(regenShields);
        }

        // Damage Prevention
        final int preventNextDamage = card.getPreventNextDamageTotalShields();
        if (preventNextDamage > 0) {
            area.append("\n");
            area.append("Prevent the next ").append(preventNextDamage).append(" damage that would be dealt to ");
            area.append(card.getName()).append(" this turn.");
        }

        // top revealed
        if ((card.hasKeyword("Play with the top card of your library revealed.") || card
                .hasKeyword("Players play with the top card of their libraries revealed."))
                && card.getController() != null
                && (card.isInZone(ZoneType.Battlefield) || (card.isInZone(ZoneType.Command) && !card.isCommander()))
                && !card.getController().getZone(ZoneType.Library).isEmpty()) {
            area.append("\r\nTop card of your library: ");
            area.append(card.getController().getCardsIn(ZoneType.Library, 1));
            if (card.hasKeyword("Players play with the top card of their libraries revealed.")) {
                for (final Player p : card.getController().getAllOtherPlayers()) {
                    if (p.getZone(ZoneType.Library).isEmpty()) {
                        area.append(p.getName());
                        area.append("'s library is empty.");
                    } else {
                        area.append("\r\nTop card of ");
                        area.append(p.getName());
                        area.append("'s library: ");
                        area.append(p.getCardsIn(ZoneType.Library, 1));
                    }
                }
            }
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
        if (!card.getChosenColor().isEmpty()) {
            if (area.length() != 0) {
                area.append("\n");
            }
            area.append("(chosen colors: ");
            area.append(card.getChosenColor());
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
        if (!card.getEquipping().isEmpty()) {
            if (area.length() != 0) {
                area.append("\n");
            }
            area.append("=Equipping ");
            area.append(card.getEquipping().get(0));
            area.append("=");
        }

        // equipped by
        if (!card.getEquippedBy().isEmpty()) {
            if (area.length() != 0) {
                area.append("\n");
            }
            area.append("=Equipped by ");
            for (final Iterator<Card> it = card.getEquippedBy().iterator(); it.hasNext();) {
                area.append(it.next());
                if (it.hasNext()) {
                    area.append(", ");
                }
            }
            area.append("=");
        }

        // enchanting
        final GameEntity entity = card.getEnchanting();
        if (entity != null) {
            if (area.length() != 0) {
                area.append("\n");
            }
            area.append("*Enchanting ");

            if (entity instanceof Card) {
                final Card c = (Card) entity;
                if (!GuiBase.getInterface().mayShowCard(c)) {
                    area.append("Morph (");
                    area.append(card.getUniqueNumber());
                    area.append(")");
                } else {
                    area.append(entity);
                }
            } else {
                area.append(entity);
            }
            area.append("*");
        }

        // enchanted by
        if (!card.getEnchantedBy().isEmpty()) {
            if (area.length() != 0) {
                area.append("\n");
            }
            area.append("*Enchanted by ");
            for (final Iterator<Card> it = card.getEnchantedBy().iterator(); it.hasNext();) {
                area.append(it.next());
                if (it.hasNext()) {
                    area.append(", ");
                }
            }
            area.append("*");
        }

        // controlling
        if (card.getGainControlTargets().size() > 0) {
            if (area.length() != 0) {
                area.append("\n");
            }
            area.append("+Controlling: ");
            for (final Iterator<Card> it = card.getGainControlTargets().iterator(); it.hasNext();) {
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
            area.append(card.getCloneOrigin().getName());
            area.append("^");
        }

        // Imprint
        if (!card.getImprinted().isEmpty()) {
            if (area.length() != 0) {
                area.append("\n");
            }
            area.append("Imprinting: ");
            for (final Iterator<Card> it = card.getImprinted().iterator(); it.hasNext();) {
                area.append(it.next());
                if (it.hasNext()) {
                    area.append(", ");
                }
            }
        }

        // Haunt
        if (!card.getHauntedBy().isEmpty()) {
            if (area.length() != 0) {
                area.append("\n");
            }
            area.append("Haunted by: ");
            for (final Iterator<Card> it = card.getHauntedBy().iterator(); it.hasNext();) {
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
            String mustBlockThese = Lang.joinHomogenous(card.getMustBlockCards());
            area.append("Must block " + mustBlockThese);
        }

        //show current storm count for storm cards
        if (card.getKeyword().contains("Storm")) {
            Game game = GuiBase.getInterface().getGame();
            if (game != null) {
                if (area.length() != 0) {
                    area.append("\n\n");
                }
                area.append("Current Storm Count: " + game.getStack().getCardsCastThisTurn().size());
            }
        }
        return area.toString();
    }

    public static boolean isCardFlippable(Card card) {
        return card.isDoubleFaced() || card.isFlipCard() || card.isFaceDown();
    }

    /**
     * Card characteristic state machine.
     * <p>
     * Given a card and a state in terms of {@code CardCharacteristicName} this
     * will determine whether there is a valid alternate {@code CardCharacteristicName}
     * state for that card.
     * 
     * @param card the {@code Card}
     * @param currentState not necessarily {@code card.getCurState()}
     * @return the alternate {@code CardCharacteristicName} state or default if not applicable
     */
    public static CardCharacteristicName getAlternateState(final Card card, CardCharacteristicName currentState) {
        // Default. Most cards will only ever have an "Original" state represented by a single image.
        CardCharacteristicName alternateState = CardCharacteristicName.Original;

        if (card.isDoubleFaced()) {
            if (currentState == CardCharacteristicName.Original) {
                alternateState = CardCharacteristicName.Transformed;
            }
        }
        else if (card.isFlipCard()) {
            if (currentState == CardCharacteristicName.Original) {
                alternateState = CardCharacteristicName.Flipped;
            }
        }
        else if (card.isFaceDown()) {
            if (currentState == CardCharacteristicName.Original) {
                alternateState = CardCharacteristicName.FaceDown;
            }
            else if (GuiBase.getInterface().mayShowCard(card)) {
                alternateState = CardCharacteristicName.Original;
            }
            else {
                alternateState = currentState;
            }
        }

        return alternateState;
    }
}
