package forge.util;

import forge.game.card.CardView;
import forge.localinstance.properties.ForgePreferences;
import forge.model.FModel;

public class CardRendererUtils {
    public static boolean canShowAlternate(final CardView card, final String reference) {
        if (card == null)
            return false;
        if (card.isFaceDown())
            return false;
        boolean showAlt = false;
        if (card.hasAlternateState()) {
            if (card.isDoubleFacedCard())
                showAlt = reference.contains(card.getAlternateState().getName()) || card.getAlternateState().getAbilityText().contains(reference);
            else if (card.hasSecondaryState())
                showAlt = reference.equals(card.getAlternateState().getAbilityText());
            else if (card.isSplitCard()) {
                //special case if aftermath cards can be cast from graveyard like yawgmoths will, you will have choices
                if (card.getAlternateState().hasAftermath())
                    showAlt = card.getAlternateState().getOracleText().contains(reference.trim());
                else if (card.isRoom()) // special case for room cards
                    showAlt = card.getAlternateState().getOracleName().equalsIgnoreCase(reference);
                else
                    showAlt = reference.contains(card.getAlternateState().getAbilityText());
            }
        }
        return showAlt;
    }
    public static boolean hasAftermath(final CardView card) {
        if (card.hasAlternateState()) {
            if (card.getId() != -1)
                return card.getAlternateState().hasAftermath();
            else
                return card.getAlternateState().getOracleText().contains("Aftermath");
        }
        return false;
    }

    public static boolean isPreferenceEnabled(final ForgePreferences.FPref preferenceName) {
        return FModel.getPreferences().getPrefBoolean(preferenceName);
    }

    public static boolean isShowingOverlays(final CardView card) {
        return isPreferenceEnabled(ForgePreferences.FPref.UI_SHOW_CARD_OVERLAYS) && card != null;
    }

    public static boolean showCardNameOverlay(final CardView card) {
        return isShowingOverlays(card) && isPreferenceEnabled(ForgePreferences.FPref.UI_OVERLAY_CARD_NAME);
    }

    public static boolean showCardPowerOverlay(final CardView card) {
        return isShowingOverlays(card) && isPreferenceEnabled(ForgePreferences.FPref.UI_OVERLAY_CARD_POWER);
    }

    public static boolean showCardManaCostOverlay(final CardView card) {
        return isShowingOverlays(card) &&
                isPreferenceEnabled(ForgePreferences.FPref.UI_OVERLAY_CARD_MANA_COST);
    }

    public static boolean showCardPerpetualManaCostOverlay() {
        return isPreferenceEnabled(ForgePreferences.FPref.UI_OVERLAY_CARD_PERPETUAL_MANA_COST);
    }

    public static boolean showAbilityIcons(final CardView card) {
        return isShowingOverlays(card) && isPreferenceEnabled(ForgePreferences.FPref.UI_OVERLAY_ABILITY_ICONS);
    }

    public static boolean showCardIdOverlay(final CardView card) {
        return card.getId() > 0 && isShowingOverlays(card) && isPreferenceEnabled(ForgePreferences.FPref.UI_OVERLAY_CARD_ID);
    }

    public static boolean drawGray(final CardView card) {
        if (card == null)
            return false;
        return card.wasDestroyed() || card.isPhasedOut();
    }
    public static int getFoilIndex(final CardView card) {
        if (card == null)
            return 0;
        if (!isPreferenceEnabled(ForgePreferences.FPref.UI_OVERLAY_FOIL_EFFECT))
            return 0;
        if (!card.hasPaperFoil())
            return 0;
        return card.getCurrentState().getFoilIndex();
    }
    public static boolean drawCracks(final CardView card, final boolean isMagnify) {
        if (card == null)
            return false;
        if (isMagnify)
            return false;
        return card.getDamage() > 0;
    }

}
