package forge.gui.card;

import com.google.common.collect.Multiset;
import forge.card.CardRarity;
import forge.card.CardStateName;
import forge.card.ColorSet;
import forge.card.MagicColor;
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
import java.util.Set;
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
            return getBorderColors(null, false, false, false, false).iterator().next();
        }
        return getBorderColors(card.getColors(), card.isLand(), canShow, false, card.isEnchantment()).iterator().next();
    }
    public static List<DetailColors> getBorderColors(final CardStateView card, final boolean canShow) {
        if (card == null) {
            return getBorderColors(null, false, false, true, false);
        }
        return getBorderColors(card.getColors(), card.isLand(), canShow, true, card.isEnchantment());
    }
    public static List<DetailColors> getBorderColors(final ColorSet colorSet) {
        return getBorderColors(colorSet, false, true, true, false);
    }
    private static List<DetailColors> getBorderColors(final ColorSet cardColors, final boolean isLand, final boolean canShow, final boolean supportMultiple, final boolean isEnchantment) {
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
                for (MagicColor.Color shard : cardColors.getOrderedColors()) {
                    DetailColors colors = getColor(shard);
                    if (colors != null) {
                        borderColors.add(colors);
                    }
                }
            }
        }

        if (borderColors.isEmpty()) { // If your card has a violet border, something is wrong
            borderColors.add(DetailColors.UNKNOWN);
        }
        return borderColors;
    }
    public static DetailColors getColor(MagicColor.Color shard) {
        return switch (shard) {
            case WHITE -> DetailColors.WHITE;
            case BLUE -> DetailColors.BLUE;
            case BLACK -> DetailColors.BLACK;
            case RED -> DetailColors.RED;
            case GREEN -> DetailColors.GREEN;
            default -> null;
        };
    }

    public static String getCurrentColors(final CardStateView c) {
        return c.getColors().getSymbolsString();
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
        return item.getDisplayName();
    }

    public static String formatCardName(final CardView card, final boolean canShow, final boolean forAltState) {
        final String name = forAltState ? card.getAlternateState().getName() : card.getName();
        String translatedname = CardTranslation.getTranslatedName(name);
        return StringUtils.isEmpty(translatedname) || !canShow ? "???" : translatedname.trim();
    }

    public static String formatCardType(final CardStateView card, final boolean canShow) {
        boolean isInPlay = card.getCard() != null && ZoneType.Battlefield.equals(card.getCard().getZone());
        String translatedtype = CardTranslation.getTranslatedType(card);
        return canShow ? translatedtype : (card.getState() == CardStateName.FaceDown && isInPlay ? "Creature" : "");
    }

    public static String formatPrimaryCharacteristic(final CardStateView card, final boolean canShow) {
        if (!canShow && card.getState() != CardStateName.FaceDown) {
            return "";
        }
        final StringBuilder ptText = new StringBuilder();
        if (card.hasPrintedPT() && !card.isCreature()) {
            ptText.append("[");
        }

        if (card.isCreature() || card.hasPrintedPT()) {
            ptText.append(card.getPower()).append(" / ").append(card.getToughness());
        }

        if (card.hasPrintedPT() && !card.isCreature()) {
            ptText.append("]");
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

        if (card.isAttraction()) {
            ptText.append(Localizer.getInstance().getMessage("lblLights")).append(": ");
            ptText.append(formatAttractionLights(card.getAttractionLights()));
        }

        return ptText.toString();
    }

    public static String formatAttractionLights(Set<Integer> lights) {
        return (lights.contains(1) ? "{AL1ON} " : "{AL1OFF} ") +
                (lights.contains(2) ? "{AL2ON} " : "{AL2OFF} ") +
                (lights.contains(3) ? "{AL3ON} " : "{AL3OFF} ") +
                (lights.contains(4) ? "{AL4ON} " : "{AL4OFF} ") +
                (lights.contains(5) ? "{AL5ON} " : "{AL5OFF} ") +
                (lights.contains(6) ? "{AL6ON}" : "{AL6OFF}");
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
                    if (!card.getOracleName().isEmpty()) {
                        origPaperCard = FModel.getMagicDb().getCommonCards().getCard(card.getOracleName());
                    } else {
                        // probably a morph or manifest, try to get its identity from the alternate state
                        String altName = card.getAlternateState().getOracleName();
                        if (!altName.isEmpty()) {
                            origPaperCard = FModel.getMagicDb().getCommonCards().getCard(altName);
                        }
                    }
                    if (origPaperCard != null) {
                        origCard = Card.getCardForUi(origPaperCard); // if null, probably a variant card
                    }
                    origIdent = origCard != null ? getCurrentColors(origCard.isFaceDown() ? CardView.get(origCard).getState(false) : CardView.get(origCard).getCurrentState()) : "";
                } catch(Exception ex) {
                    System.err.println("Unexpected behavior: card " + card.getOracleName() + "[" + card.getId() + "] tripped an exception when trying to process current card colors.");
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
        if (!canShow || state == null) {
            return "";
        }

        final CardView card = state.getCard();
        if (card == null) {
            return "";
        }

        // a local builder: a shared static one is corrupted when two threads (UI, netplay, AI) compose text at once
        final StringBuilder area = new StringBuilder(512);

        // Token / Emblem / Effect Types
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
        area.append("\n");

        boolean needTranslation = true;
        if (card.isToken() && card.getCloneOrigin() == null) {
            needTranslation = false;
        }

        String text = !card.isSplitCard() ?
                card.getText(state, needTranslation ? CardTranslation.getTranslationTexts(state) : null) :
                card.getText(state, needTranslation ? CardTranslation.getTranslationTexts(card.getLeftSplitState(), card.getRightSplitState()) : null);

        if (text != null) {
            // Bracket P/T for Level up
            if (text.contains("LEVEL")) {
                text = text.replaceAll("[0-99]+/[0-99]", "[$0]");
            }
            // LEVEL [0-9]+-[0-9]+
            text = text.replaceAll("LEVEL [0-9]+-[0-9]+ \\[[0-99]+/[0-99]]+ ", "$0\n");
            // LEVEL [0-9]+\+
            text = text.replaceAll("LEVEL [0-9]+\\+ \\[[0-99]+/[0-99]]+ ", "$0\n");
            // ",,," becomes a line break
            text = text.replace(",,,", "\n");
            // displays keywords that have dots in them a little better:
            text = text.replaceAll("\\., ", ".\n");

            area.append(text);
        }

        if (card.isPhasedOut()) {
            area.append("\nPhased Out");
        }

        // Text Changes
        final Map<String, String> changedColorWords = card.getChangedColorWords();
        final Map<String, String> changedTypes = card.getChangedTypes();
        if (changedColorWords != null && changedTypes != null && !(changedColorWords.isEmpty() && changedTypes.isEmpty())) {
            area.append("\n");

            // Refactored to map keys iteratively without implicit Entry collection creations
            if (!changedColorWords.isEmpty()) {
                for (String key: changedColorWords.keySet()) {
                    String value = changedColorWords.get(key);
                    area.append("Text changed: all instances of ");
                    if ("Any".equals(key)) {
                        area.append("color words");
                    } else {
                        area.append(key);
                    }
                    area.append(" are replaced by ").append(value).append(".\n");
                }
            }
            if (!changedTypes.isEmpty()) {
                for (String key: changedTypes.keySet()) {
                    String value = changedTypes.get(key);
                    area.append("Text changed: all instances of ");
                    if ("Any".equals(key)) {
                        if (forge.card.CardType.getBasicTypes().contains(value)) {
                            area.append("basic land types");
                        } else {
                            area.append("creature types");
                        }
                    } else {
                        area.append(key);
                    }
                    area.append(" are replaced by ").append(value).append(".\n");
                }
            }
        }

        final int intensity = card.getIntensity();
        if (intensity > 0) {
            area.append("\nIntensity: ").append(intensity);
        }

        // Counter Text
        final Multiset < CounterType > counters = card.getCounters();
        if (counters != null && !counters.isEmpty()) {
            for (CounterType type: counters.elementSet()) {
                int count = counters.count(type);
                if (count != 0) {
                    area.append("\n").append(type.getName()).append(" counters: ").append(count);
                }
            }
        }

        if (state.isCreature()) {
            final int damage = card.getDamage();
            if (damage > 0) {
                area.append("\nDamage: ").append(damage);
            }
        }
        if (state.isCreature() || state.isPlaneswalker()) {
            final int assigned = card.getAssignedDamage();
            if (assigned > 0) {
                area.append("\nAssigned Damage: ").append(assigned);
            }
        }

        // Regeneration Shields
        final int regenShields = card.getShieldCount();
        if (regenShields > 0) {
            area.append("\nRegeneration Shields: ").append(regenShields);
        }

        // Damage Prevention
        final int preventNextDamage = card.getPreventNextDamage();
        if (preventNextDamage > 0) {
            area.append("\nPrevent the next ").append(preventNextDamage).append(" damage that would be dealt to ")
                    .append(state.getName()).append(" this turn.");
        }

        // Draft Keywords
        if (card.getDraftAction() != null) {
            final List < String > draftActions = card.getDraftAction();
            final int size = draftActions.size();
            for (int i = 0; i < size; i++) {
                area.append("\n").append(TextUtil.fastReplace(draftActions.get(i), "CARDNAME", card.getName()));
            }
        }

        // Draft Notes
        final PlayerView pl = card.getController();
        if (pl != null) {
            final Map<String, String> notes = pl.getDraftNotes();
            if (notes != null) {
                final String note = notes.get(card.getOracleName());
                if (note != null) {
                    area.append("\nDraft Notes: ").append(note);
                }
            }
        }

        // Chosen Type
        if (!card.getChosenType().isEmpty()) {
            area.append("\n(chosen type: ").append(card.getChosenType());
            if (!card.getChosenType2().isEmpty()) {
                area.append(", ").append(card.getChosenType2());
            }
            area.append(")");
        }

        // Noted Types
        if (card.getNotedTypes() != null && !card.getNotedTypes().isEmpty()) {
            area.append("\n(noted type").append(card.getNotedTypes().size() == 1 ? ": " : "s: ")
                    .append(Lang.joinHomogenous(card.getNotedTypes())).append(")");
        }

        // Chosen Spire / Colors
        if (card.getMarkedColors() != null && !card.getMarkedColors().isColorless()) {
            area.append("\n(").append(Localizer.getInstance().getMessage("lblSelected")).append(": ")
                    .append(Lang.joinHomogenous(card.getMarkedColors().stream().map(MagicColor.Color::getTranslatedName).collect(Collectors.toList())))
                    .append(")");
        }

        if (card.getChosenColors() != null && !card.getChosenColors().isEmpty()) {
            area.append("\n(").append(Localizer.getInstance().getMessage("lblChosenColors")).append(" ")
                    .append(Lang.joinHomogenous(ColorSet.fromNames(card.getChosenColors()).stream().map(MagicColor.Color::getTranslatedName).collect(Collectors.toList())))
                    .append(")");
        }

        // Chosen Cards
        if (!card.getChosenCards().isEmpty()) {
            area.append("\n(chosen card").append(card.getChosenCards().size() == 1 ? ": " : "s: ")
                    .append(Lang.joinHomogenous(card.getChosenCards())).append(")");
        }

        // chosen number
        if (!card.getChosenNumber().isEmpty()) {
            area.append("\n(chosen number: ").append(card.getChosenNumber()).append(")");
        }

        // stored dice results
        if (card.getStoredRolls() != null) {
            area.append("\n(stored dice results: ").append(StringUtils.join(card.getStoredRolls(), ", ")).append(")");
        }

        // chosen player
        if (card.getChosenPlayer() != null) {
            area.append("\n(chosen player: ").append(card.getChosenPlayer()).append(")");
        }

        // chosen mode
        if (!card.getChosenMode().isEmpty()) {
            area.append("\n(chosen mode: ").append(card.getChosenMode()).append(")");
        }

        // Named Card
        if (card.getNamedCard() != null && !card.getNamedCard().isEmpty()) {
            area.append("\n(named card").append(card.getNamedCard().size() > 1 ? "s" : "").append(": ");
            if (card.isFaceDown() && state.getState() == CardStateName.FaceDown) {
                area.append("Hidden");
            } else {
                area.append(StringUtils.join(card.getNamedCard(), ", "));
            }
            area.append(")");
        }

        // dungeon room
        if (card.getCurrentRoom() != null && !card.getCurrentRoom().isEmpty()) {
            area.append("\n\n(In room: ").append(card.getCurrentRoom()).append(")");
        }

        // class level
        if (card.getId() >= 0 && card.getCurrentState().getType().hasStringType("Class") && card.getZone() == ZoneType.Battlefield) {
            area.append("\n\n(Class Level: ").append(card.getClassLevel()).append(")");
        }

        //ring level
        if (card.getRingLevel() > 0 && card.getZone() == ZoneType.Command) {
            area.append("\n\n(Ring Level: ").append(card.getRingLevel()).append(")");
        }

        // Text on gameplay trackers (e.g. Speed)
        if (StringUtils.isNotEmpty(card.getOverlayText())) {
            area.append("\n\n(").append(card.getOverlayText()).append(")");
        }

        // sector
        if (card.getSector() != null && !card.getSector().isEmpty()) {
            area.append("\n\nSector: ").append(card.getSector());
        }

        // a card has something attached to it
        if (card.hasCardAttachments()) {
            area.append("\n=Attached: ").append(StringUtils.join(card.getAttachedCards(), ", ")).append("=");
        }

        // a card is attached to something
        if (card.getAttachedTo() != null) {
            area.append("\n*Attached to ").append(card.getAttachedTo()).append("*");
        }
        if (card.getEnchantedPlayer() != null) {
            area.append("\n*Enchanting ").append(card.getEnchantedPlayer()).append("*");
        }

        // controlling
        if (!card.getGainControlTargets().isEmpty()) {
            area.append("\n+Controlling: ").append(StringUtils.join(card.getGainControlTargets(), ", ")).append("+");
        }

        // Sprocket a contraption is on.
        if (card.getSprocket() > 0 && card.getCurrentState().isContraption()) {
            area.append("\nSprocket: ").append(card.getSprocket());
        }

        if (card.getProtectingPlayer() != null) {
            area.append("\nProtected by: ").append(card.getProtectingPlayer());
        }

        // cloned via
        if (card.getCloneOrigin() != null) {
            area.append("\n^Cloned via: ").append(card.getCloneOrigin().getCurrentState().getName()).append("^");
        }

        // Imprint
        if (!card.getImprintedCards().isEmpty()) {
            area.append("\nImprinting: ").append(StringUtils.join(card.getImprintedCards(), ", "));
        }

        // CardsExiledBy
        if (!card.getExiledCards().isEmpty()) {
            area.append("\nExiled: ").append(StringUtils.join(card.getExiledCards(), ", "));
        }

        // TODO don't print both if identical
        if (!card.getUntilLeavesBattlefield().isEmpty()) {
            area.append("\nExiled until this leaves the battlefield: ").append(card.getUntilLeavesBattlefield());
        }

        // Haunt
        if (!card.getHauntedBy().isEmpty()) {
            area.append("\nHaunted by: ").append(StringUtils.join(card.getHauntedBy(), ", "));
        }
        if (card.getHaunting() != null) {
            area.append("\nHaunting ").append(card.getHaunting());
        }

        // Cipher
        if (!card.getEncodedCards().isEmpty()) {
            area.append("\nEncoded: ").append(card.getEncodedCards());
        }

        // must block
        if (!card.getMustBlockCards().isEmpty()) {
            area.append("\nMust block ").append(Lang.joinHomogenous(card.getMustBlockCards()));
        }

        // exerted
        if (card.isExertedThisTurn()) {
            area.append("\n^Exerted^");
        }
        // detained
        if (card.isDetained()) {
            area.append("\n^Detained^");
        }

        //show current card colors if enabled
        final String curCardColors = formatCurrentCardColors(state);
        if (!curCardColors.isEmpty()) {
            area.append("\n\nCurrent Card Colors: ").append(curCardColors);
        }

        //show current storm count for storm cards
        if (state.hasStorm() && gameView != null) {
            area.append("\n\nCurrent Storm Count: ").append(gameView.getStormCount());
        }

        //show owner if being controlled by a different player
        if (card.getOwner() != card.getController()) {
            area.append("\n\nOwner: ").append(card.getOwner().toString());
        }

        if (card.hasAlternateState() && state.getState() == CardStateName.Original) {
            if (card.getAlternateState().getType().hasSubtype("Adventure")) {
                area.append("\n\n").append(Localizer.getInstance().getMessage("lblAdventure")).append(" — ").append(getAlternateStateDesc(card));
            }

            if (card.getAlternateState().getType().hasSubtype("Omen")) {
                area.append("\n\n").append(Localizer.getInstance().getMessage("lblOmen")).append(" — ").append(getAlternateStateDesc(card));
            }

            if (card.getAlternateState().getType().hasSubtype("Vengeance")) {
                area.append("\n\n");
                area.append(Localizer.getInstance().getMessage("lblVengeance") + " — " + getAlternateStateDesc(card));
            }

            if (card.hasPreparedSpell()) {
                area.append("\n\n").append(Localizer.getInstance().getMessage("lblPrepared")).append(" — ").append(getAlternateStateDesc(card));
            }
        }

        return area.toString().trim();
    }

    protected static String getAlternateStateDesc(CardView card) {
        CardStateView state = card.getAlternateState();
        return state.getName() + " "
                + state.getManaCost().toString() + ": "
                + (card.getId() > 0 ? state.getAbilityText() : state.getOracleText());
    }
}
