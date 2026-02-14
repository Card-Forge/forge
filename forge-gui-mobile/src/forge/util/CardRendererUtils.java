package forge.util;

import forge.Forge;
import forge.game.card.CardView;
import forge.localinstance.properties.ForgePreferences;
import forge.model.FModel;
import forge.screens.match.MatchController;

public class CardRendererUtils {
    public static boolean needsRotation(final CardView card) {
        return needsRotation(card.isSplitCard() ? ForgePreferences.FPref.UI_ROTATE_SPLIT_CARDS
                : ForgePreferences.FPref.UI_ROTATE_PLANE_OR_PHENOMENON, card, canShowAlternate(card, card.getOracleName()));
    }
    public static boolean needsRotation(final CardView card, final boolean altState) {
        return needsRotation(card.isSplitCard() ? ForgePreferences.FPref.UI_ROTATE_SPLIT_CARDS
                : ForgePreferences.FPref.UI_ROTATE_PLANE_OR_PHENOMENON, card, altState);
    }
    public static boolean needsRotation(final ForgePreferences.FPref fPref, final CardView card, boolean altState) {
        if (isPreferenceEnabled(fPref)) {
            if (Forge.enableUIMask.equals("Art"))
                return false;
            switch (fPref) {
                case UI_ROTATE_SPLIT_CARDS -> {
                    return card.isSplitCard() && MatchController.instance.mayView(card) && !card.isFaceDown();
                }
                case UI_ROTATE_PLANE_OR_PHENOMENON -> {
                    return card.getCurrentState().isPhenomenon() || card.getCurrentState().isPlane()
                            || (card.getCurrentState().isBattle() && !altState)
                            || (card.getAlternateState() != null && card.getAlternateState().isBattle() && altState);
                }
                default -> {
                    return false;
                }
            }
        }
        return false;
    }
    public static boolean canShowAlternate(final CardView card, final String reference) {
        if (card == null)
            return false;
        if (card.isFaceDown())
            return false;
        boolean showAlt = false;
        if (card.hasAlternateState()) {
            if (card.hasBackSide())
                showAlt = reference.contains(card.getBackSideName()) || card.getAlternateState().getAbilityText().contains(reference);
            else if (card.hasSecondaryState())
                showAlt = reference.equals(card.getAlternateState().getAbilityText());
            else if (card.isSplitCard()) {
                //special case if aftermath cards can be cast from graveyard like yawgmoths will, you will have choices
                if (card.getAlternateState().hasAftermath())
                    showAlt = card.getAlternateState().getOracleText().contains(reference.trim());
                else {
                    if (card.isRoom()) // special case for room cards
                        showAlt = card.getAlternateState().getOracleName().equalsIgnoreCase(reference);
                    else
                        showAlt = reference.contains(card.getAlternateState().getAbilityText());
                }
            }
        }
        return showAlt;
    }
    public static boolean hasAftermath(final CardView card) {
        if (card.hasAlternateState())
            return card.getAlternateState().hasAftermath();
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
    public static boolean drawFoil(final CardView card) {
        if (card == null)
            return false;
        if (isPreferenceEnabled(ForgePreferences.FPref.UI_OVERLAY_FOIL_EFFECT))
            return card.hasPaperFoil(); // TODO the Card BG should be the texture instead of the Foil Overlay
        return false;
    }
    public static boolean drawCracks(final CardView card, final boolean isMagnify) {
        if (card == null)
            return false;
        if (isMagnify)
            return false;
        return card.getDamage() > 0;
    }

}
