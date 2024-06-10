package forge.gui.card;

import com.google.common.collect.Sets;
import forge.card.CardRarity;
import forge.card.CardStateName;
import forge.card.ColorSet;
import forge.card.MagicColor;
import forge.card.mana.ManaCostShard;
import forge.deck.DeckRecognizer;
import forge.game.GameView;
import forge.game.card.Card;
import forge.game.card.CardView;
import forge.game.card.CardView.CardStateView;
import forge.game.card.CounterType;
import forge.game.player.PlayerView;
import forge.game.zone.ZoneType;
import forge.item.InventoryItemFromSet;
import forge.item.PaperCard;
import forge.item.PreconDeck;
import forge.item.SealedProduct;
import forge.localinstance.properties.ForgeConstants;
import forge.localinstance.properties.ForgePreferences;
import forge.model.FModel;
import forge.util.CardTranslation;
import forge.util.Lang;
import forge.util.Localizer;
import forge.util.TextUtil;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

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

        DetailColors(final int r0, final int g0, final int b0) {
            r = r0;
            g = g0;
            b = b0;
        }
    }

    public static DetailColors getBorderColor(final CardStateView card, final boolean canShow) {
        if (card == null) {
            return getBorderColors(null, false, false, false).iterator().next();
        }
        return getBorderColors(card.getColors(), card.isLand(), canShow, false).iterator().next();
    }
    public static List<DetailColors> getBorderColors(final CardStateView card, final boolean canShow) {
        if (card == null) {
            return getBorderColors(null, false, false, true);
        }
        return getBorderColors(card.getColors(), card.isLand(), canShow, true);
    }
    public static List<DetailColors> getBorderColors(final ColorSet colorSet) {
        return getBorderColors(colorSet, false, true, true);
    }
    private static List<DetailColors> getBorderColors(final ColorSet cardColors, final boolean isLand, final boolean canShow, final boolean supportMultiple) {
        final List<DetailColors> borderColors = new ArrayList<>();

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
            final int colorCount = cardColors.countColors();
            if (colorCount > 3 || (colorCount > 1 && !supportMultiple)) {
                borderColors.add(DetailColors.MULTICOLOR);
            }
            else { //for 3 colors or fewer, return all colors in shard order
                for (ManaCostShard shard : cardColors.getOrderedShards()) {
                    switch (shard.getColorMask()) {
                    case MagicColor.WHITE:
                        borderColors.add(DetailColors.WHITE);
                        break;
                    case MagicColor.BLUE:
                        borderColors.add(DetailColors.BLUE);
                        break;
                    case MagicColor.BLACK:
                        borderColors.add(DetailColors.BLACK);
                        break;
                    case MagicColor.RED:
                        borderColors.add(DetailColors.RED);
                        break;
                    case MagicColor.GREEN:
                        borderColors.add(DetailColors.GREEN);
                        break;
                    }
                }
            }
        }

        if (borderColors.isEmpty()) { // If your card has a violet border, something is wrong
            borderColors.add(DetailColors.UNKNOWN);
        }
        return borderColors;
    }

    public static String getCurrentColors(final CardStateView c) {
        ColorSet curColors = c.getColors();
        String strCurColors = "";

        if (curColors.hasWhite()) { strCurColors += "{W}"; }
        if (curColors.hasBlue())  { strCurColors += "{U}"; }
        if (curColors.hasBlack()) { strCurColors += "{B}"; }
        if (curColors.hasRed())   { strCurColors += "{R}"; }
        if (curColors.hasGreen()) { strCurColors += "{G}"; }

        if (strCurColors.isEmpty()) {
            strCurColors = "{C}";
        }

        return strCurColors;
    }

    public static DetailColors getRarityColor(final CardRarity rarity) {
        switch (rarity) {
            case Uncommon:
                return DetailColors.UNCOMMON;
            case Rare:
                return DetailColors.RARE;
            case MythicRare:
                return DetailColors.MYTHIC;
            case Special: //"Timeshifted" or other Special Rarity Cards
                return DetailColors.SPECIAL;
            default: //case BasicLand: + case Common:
                return DetailColors.COMMON;
        }
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

    public static String formatCardName(final CardView card, final boolean canShow, final boolean forAltState) {
        final String name = forAltState ? card.getAlternateState().getName() : card.getName();
        String translatedname = CardTranslation.getTranslatedName(name);
        return StringUtils.isEmpty(translatedname) || !canShow ? "???" : translatedname.trim();
    }

    public static String formatCardType(final CardStateView card, final boolean canShow) {
        boolean isInPlay = card.getCard() != null && ZoneType.Battlefield.equals(card.getCard().getZone());
        String translatedtype = CardTranslation.getTranslatedType(card.getName(), card.getType().toString());
        return canShow ? translatedtype : (card.getState() == CardStateName.FaceDown && isInPlay ? "Creature" : "");
    }

    public static String formatPrimaryCharacteristic(final CardStateView card, final boolean canShow) {
        if (!canShow && card.getState() != CardStateName.FaceDown) {
            return "";
        }
        final StringBuilder ptText = new StringBuilder();
        boolean vehicle = card.getType().hasSubtype("Vehicle");
        if (vehicle && !card.isCreature()) {
            ptText.append("{");
        }

        if (card.isCreature() || vehicle) {
            ptText.append(card.getPower()).append(" / ").append(card.getToughness());
        }

        if (vehicle && !card.isCreature()) {
            ptText.append("}");
        }

        if (card.isPlaneswalker()) {
            if (ptText.length() > 0) {
                ptText.insert(0, "P/T: ");
                ptText.append(" - ").append("Loy: ");
            } else {
                ptText.append(Localizer.getInstance().getMessage("lblLoyalty")).append(": ");
            }

            ptText.append(card.getLoyalty());
        }

        if (card.isBattle()) {
            ptText.append(Localizer.getInstance().getMessage("lblDefense")).append(": ");
            ptText.append(card.getDefense());
        }

        return ptText.toString();
    }

    public static String formatCardId(final CardStateView card) {
        final String id = card.getDisplayId();
        return id.isEmpty() ? id : "[" + id + "]";
    }

    public static String formatCurrentCardColors(final CardStateView state) {
        final String showCurColorMode = FModel.getPreferences().getPref(ForgePreferences.FPref.UI_DISPLAY_CURRENT_COLORS);
        boolean showCurColors = false;
        String curColors = "";

        // do not show current colors for temp effect cards, emblems and the like
        if (state.getCard().isImmutable()) {
            return "";
        }

        if (!showCurColorMode.equals(ForgeConstants.DISP_CURRENT_COLORS_NEVER)) {
            final CardView card = state.getCard();
            boolean isMulticolor = state.getColors().isMulticolor();
            boolean isChanged = false;
            curColors = getCurrentColors(state);

            // do not waste CPU cycles on this if the mode does not involve checking for changed colors
            if (showCurColorMode.equals(ForgeConstants.DISP_CURRENT_COLORS_CHANGED) || showCurColorMode.equals(ForgeConstants.DISP_CURRENT_COLORS_MULTI_OR_CHANGED)) {
                String origIdent = "";
                PaperCard origPaperCard = null;
                Card origCard = null;
                try {
                    if (!card.getName().isEmpty()) {
                        origPaperCard = FModel.getMagicDb().getCommonCards().getCard(card.getName());
                    } else {
                        // probably a morph or manifest, try to get its identity from the alternate state
                        String altName = card.getAlternateState().getName();
                        if (!altName.isEmpty()) {
                            origPaperCard = FModel.getMagicDb().getCommonCards().getCard(card.getAlternateState().getName());
                        }
                    }
                    if (origPaperCard != null) {
                        origCard = Card.getCardForUi(origPaperCard); // if null, probably a variant card
                    }
                    origIdent = origCard != null ? getCurrentColors(origCard.isFaceDown() ? CardView.get(origCard).getState(false) : CardView.get(origCard).getCurrentState()) : "";
                } catch(Exception ex) {
                    System.err.println("Unexpected behavior: card " + card.getName() + "[" + card.getId() + "] tripped an exception when trying to process current card colors.");
                }
                isChanged = !curColors.equals(origIdent);
            }

            if ((showCurColorMode.equals(ForgeConstants.DISP_CURRENT_COLORS_MULTICOLOR) && isMulticolor) ||
                    (showCurColorMode.equals(ForgeConstants.DISP_CURRENT_COLORS_CHANGED) && isChanged) ||
                    (showCurColorMode.equals(ForgeConstants.DISP_CURRENT_COLORS_MULTI_OR_CHANGED) && (isChanged || isMulticolor)) ||
                    (showCurColorMode.equals(ForgeConstants.DISP_CURRENT_COLORS_ALWAYS))) {
                showCurColors = true;
            }
        }

        return showCurColors ? curColors : "";
    }

    public static String composeCardText(final CardStateView state, final GameView gameView, final boolean canShow) {
        if (!canShow) { return ""; }

        final CardView card = state.getCard();
        final StringBuilder area = new StringBuilder();

        // Token
        if (card.isToken()) {
            area.append("Token");
        } else if (card.isTokenCard()) {
            area.append("Token card");
        } else if (card.isEmblem()) {
            area.append("Emblem");
        } else if (card.isBoon()) {
            area.append("Boon");
        } else if (card.isImmutable()) {
            area.append("Effect");
        }
        // card text
        if (area.length() != 0) {
            area.append("\n");
        }

        boolean needTranslation = true;
        if (card.isToken()) {
            if (card.getCloneOrigin() == null)
                needTranslation = false;
        }
        String text = !card.isSplitCard() ?
            card.getText(state, needTranslation ? CardTranslation.getTranslationTexts(state.getName(), "") : null) :
            card.getText(state, needTranslation ? CardTranslation.getTranslationTexts(card.getLeftSplitState().getName(), card.getRightSplitState().getName()) : null );

        // Bracket P/T for Level up
        if (text.contains("LEVEL")) {
            String regex = "[0-99]+/[0-99]";
            text = text.replaceAll(regex, "[$0]");
        }

        // LEVEL [0-9]+-[0-9]+
        String regex = "LEVEL [0-9]+-[0-9]+ \\[[0-99]+/[0-99]]+ ";
        text = text.replaceAll(regex, "$0\r\n");

        // LEVEL [0-9]+\+
        regex = "LEVEL [0-9]+\\+ \\[[0-99]+/[0-99]]+ ";
        text = text.replaceAll(regex, "$0\r\n");

        // ",,," becomes a line break
        regex = ",,,";
        text = text.replaceAll(regex, "\r\n");

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
        if (changedColorWords != null && changedTypes != null) {
            if (!(changedColorWords.isEmpty() && changedTypes.isEmpty())) {
                if (area.length() != 0) {
                    area.append("\n");
                }
            }

            for (final Entry<String, String> e : Sets.union(changedColorWords.entrySet(), changedTypes.entrySet())) {
                area.append("Text changed: all instances of ");
                if (e.getKey().equals("Any")) {
                    if (changedColorWords.containsKey(e.getValue())) {
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

        final int intensity = card.getIntensity();
        if (intensity > 0) {
            if (area.length() != 0) {
                area.append("\n");
            }
            area.append("Intensity: ").append(intensity);
        }

        // counter text
        if (card.getCounters() != null) {
            for (final Entry<CounterType, Integer> c : card.getCounters().entrySet()) {
                if (c.getValue().intValue() != 0) {
                    if (area.length() != 0) {
                        area.append("\n");
                    }
                    area.append(c.getKey().getName()).append(" counters: ");
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
                area.append("Damage: ").append(damage);
            }
        }
        if (state.isCreature() || state.isPlaneswalker()) {
            final int assigned = card.getAssignedDamage();
            if (assigned > 0) {
                if (area.length() != 0) {
                    area.append("\n");
                }
                area.append("Assigned Damage: ").append(assigned);
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

        // Draft keywords
        if (card.getDraftAction() != null) {
            for(final String draftAction : card.getDraftAction()) {
                if (area.length() != 0) {
                    area.append("\n");
                }
                area.append(TextUtil.fastReplace(draftAction, "CARDNAME", card.getName()));
            }
        }

        // Draft notes
        PlayerView pl = card.getController();
        if (pl != null) {
            Map<String, String> notes = pl.getDraftNotes();
            if (notes != null) {
                String note = notes.get(card.getName());
                if (note != null) {
                    if (area.length() != 0) {
                        area.append("\n");
                    }
                    area.append("Draft Notes: ").append(note);
                }
            }
        }

        // chosen type
        if (!card.getChosenType().isEmpty()) {
            if (area.length() != 0) {
                area.append("\n");
            }
            area.append("(chosen type: ");
            area.append(card.getChosenType());
            if (!card.getChosenType2().isEmpty()) {
                area.append(", ").append(card.getChosenType2());
            }
            area.append(")");
        }

        // noted types
        if (card.getNotedTypes() != null && !card.getNotedTypes().isEmpty()) {
            if (area.length() != 0) {
                area.append("\n");
            }
            area.append("(noted type").append(card.getNotedTypes().size() == 1 ? ": " : "s: ");
            area.append(Lang.joinHomogenous(card.getNotedTypes()));
            area.append(")");
        }

        // chosen color
        if (card.getChosenColors() != null && !card.getChosenColors().isEmpty()) {
            if (area.length() != 0) {
                area.append("\n");
            }
            area.append("(").append(Localizer.getInstance().getMessage("lblChosenColors")).append(" ");
            area.append(Lang.joinHomogenous(card.getChosenColors().stream().map(DeckRecognizer::getLocalisedMagicColorName).collect(Collectors.toList())));
            area.append(")");
        }

        // chosen cards
        if (card.getChosenCards() != null) {
            if (area.length() != 0) {
                area.append("\n");
            }
            area.append("(chosen card").append(card.getChosenCards().size() == 1 ? ": " : "s: ");
            area.append(Lang.joinHomogenous(card.getChosenCards())).append(")");
        }

        // chosen number
        if (!card.getChosenNumber().isEmpty()) {
            if (area.length() != 0) {
                area.append("\n");
            }
            area.append("(chosen number: ").append(card.getChosenNumber()).append(")");
        }

        // stored dice results
        if (card.getStoredRolls() != null) {
            if (area.length() != 0) {
                area.append("\n");
            }
            area.append("(stored dice results: ").append(StringUtils.join(card.getStoredRolls(), ", "));
            area.append(")");
        }

        // chosen player
        if (card.getChosenPlayer() != null) {
            if (area.length() != 0) {
                area.append("\n");
            }
            area.append("(chosen player: ").append(card.getChosenPlayer()).append(")");
        }

        // chosen mode
        if (!card.getChosenMode().isEmpty()) {
            if (area.length() != 0) {
                area.append("\n");
            }
            area.append("(chosen mode: ").append(card.getChosenMode()).append(")");
        }

        // named card
        if (card.getNamedCard() != null && !card.getNamedCard().isEmpty()) {
            if (area.length() != 0) {
                area.append("\n");
            }
            area.append("(named card").append(card.getNamedCard().size() > 1 ? "s" : "").append(": ");
            if (card.isFaceDown() && state.getState() == CardStateName.FaceDown) {
                area.append("Hidden");
            } else {
                area.append(StringUtils.join(card.getNamedCard(), ", "));
            }
            area.append(")");
        }

        // dungeon room
        if (card.getCurrentRoom() != null && !card.getCurrentRoom().isEmpty()) {
            if (area.length() != 0) {
                area.append("\n\n");
            }
            area.append("(In room: ");
            area.append(card.getCurrentRoom()).append(")");
        }

        // class level
        if (card.getId() >= 0 && card.getCurrentState().getType().hasStringType("Class") && card.getZone() == ZoneType.Battlefield) {
            if (area.length() != 0) {
                area.append("\n\n");
            }
            area.append("(Class Level:").append(card.getClassLevel()).append(")");
        }

        //ring level
        if (card.getRingLevel() > 0 && card.getZone() == ZoneType.Command) {
            if (area.length() != 0) {
                area.append("\n\n");
            }
            area.append("(Ring Level:").append(card.getRingLevel()).append(")");
        }

        // sector
        if (card.getSector() != null && !card.getSector().isEmpty()) {
            if (area.length() != 0) {
                area.append("\n");
            }
            area.append("Sector: ").append(card.getSector());
        }

        // a card has something attached to it
        if (card.hasCardAttachments()) {
            if (area.length() != 0) {
                area.append("\n");
            }
            area.append("=Attached: ");
            area.append(StringUtils.join(card.getAttachedCards(), ", "));
            area.append("=");
        }

        // a card is attached to something
        if (card.getAttachedTo() != null) {
            if (area.length() != 0) {
                area.append("\n");
            }
            area.append("*Attached to ").append(card.getAttachedTo()).append("*");
        }
        if (card.getEnchantedPlayer() != null) {
            if (area.length() != 0) {
                area.append("\n");
            }
            area.append("*Enchanting ").append(card.getEnchantedPlayer()).append("*");
        }

        // controlling
        if (card.getGainControlTargets() != null) {
            if (area.length() != 0) {
                area.append("\n");
            }
            area.append("+Controlling: ");
            area.append(StringUtils.join(card.getGainControlTargets(), ", "));
            area.append("+");
        }

        if (card.getProtectingPlayer() != null) {
            if (area.length() != 0) {
                area.append("\n");
            }
            area.append("Protected by: ").append(card.getProtectingPlayer());
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
            area.append(StringUtils.join(card.getImprintedCards(), ", "));
        }

        // CardsExiledBy
        if (card.getExiledCards() != null) {
            if (area.length() != 0) {
                area.append("\n");
            }
            area.append("Exiled: ");
            area.append(StringUtils.join(card.getExiledCards(), ", "));
        }

        // Haunt
        if (card.getHauntedBy() != null) {
            if (area.length() != 0) {
                area.append("\n");
            }
            area.append("Haunted by: ");
            area.append(StringUtils.join(card.getHauntedBy(), ", "));
        }
        if (card.getHaunting() != null) {
            if (area.length() != 0) {
                area.append("\n");
            }
            area.append("Haunting ").append(card.getHaunting());
        }

        // Cipher
        if (card.getEncodedCards() != null) {
            if (area.length() != 0) {
                area.append("\n");
            }
            area.append("Encoded: ").append(card.getEncodedCards());
        }

        if (card.getUntilLeavesBattlefield() != null) {
            if (area.length() != 0) {
                area.append("\n");
            }
            area.append("Exiled until this leaves the battlefield: ").append(card.getUntilLeavesBattlefield());
        }

        // must block
        if (card.getMustBlockCards() != null) {
            if (area.length() != 0) {
                area.append("\n");
            }
            final String mustBlockThese = Lang.joinHomogenous(card.getMustBlockCards());
            area.append("Must block ").append(mustBlockThese);
        }

        // exerted
        if (card.isExertedThisTurn()) {
            if (area.length() != 0) {
                area.append("\n\n");
            }
            area.append("^Exerted^");
        }

        //show current card colors if enabled
        String curCardColors = formatCurrentCardColors(state);
        if (!curCardColors.isEmpty()) {
            if (area.length() != 0) {
                area.append("\n\n");
            }
            area.append("Current Card Colors: ");
            area.append(curCardColors);
        }

        //show current storm count for storm cards
        if (state.hasStorm()) {
            if (gameView != null) {
                if (area.length() != 0) {
                    area.append("\n\n");
                }
                area.append("Current Storm Count: ").append(gameView.getStormCount());
            }
        }

        //show owner if being controlled by a different player
        if (card.getOwner() != card.getController()) {
            if (area.length() != 0) {
                area.append("\n\n");
            }
            area.append("Owner: ").append(card.getOwner().toString());
        }
        return area.toString();
    }
}
