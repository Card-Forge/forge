package forge.game;

import forge.card.ColorSet;
import forge.card.MagicColor;
import forge.game.ability.AbilityUtils;
import forge.game.card.Card;
import forge.game.card.CardState;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.staticability.StaticAbility;
import forge.util.Expressions;

public class ForgeScript {

    public static boolean cardStateHasProperty(CardState cardState, String property, Player sourceController,
            Card source, SpellAbility spellAbility) {

        final boolean isColorlessSource = cardState.getCard().hasKeyword("Colorless Damage Source", cardState);
        final ColorSet colors = cardState.getCard().determineColor(cardState);
        if (property.contains("White") || property.contains("Blue") || property.contains("Black")
                || property.contains("Red") || property.contains("Green")) {
            boolean mustHave = !property.startsWith("non");
            boolean withSource = property.endsWith("Source");
            if (withSource && isColorlessSource) {
                return false;
            }

            final String colorName = property.substring(mustHave ? 0 : 3, property.length() - (withSource ? 6 : 0));

            int desiredColor = MagicColor.fromName(colorName);
            boolean hasColor = colors.hasAnyColor(desiredColor);
            if (mustHave != hasColor)
                return false;

        } else if (property.contains("Colorless")) { // ... Card is colorless
            boolean non = property.startsWith("non");
            boolean withSource = property.endsWith("Source");
            if (non && withSource && isColorlessSource) {
                return false;
            }
            if (non == colors.isColorless()) return false;

        } else if (property.contains("MultiColor")) {
            // ... Card is multicolored
            if (property.endsWith("Source") && isColorlessSource)
                return false;
            if (property.startsWith("non") == colors.isMulticolor())
                return false;

        } else if (property.contains("MonoColor")) { // ... Card is monocolored
            if (property.endsWith("Source") && isColorlessSource)
                return false;
            if (property.startsWith("non") == colors.isMonoColor())
                return false;

        } else if (property.startsWith("ChosenColor")) {
            if (property.endsWith("Source") && isColorlessSource)
                return false;
            if (!source.hasChosenColor() || !colors.hasAnyColor(MagicColor.fromName(source.getChosenColor())))
                return false;

        } else if (property.startsWith("AnyChosenColor")) {
            if (property.endsWith("Source") && isColorlessSource)
                return false;
            if (!source.hasChosenColor()
                    || !colors.hasAnyColor(ColorSet.fromNames(source.getChosenColors()).getColor()))
                return false;

        } else if (property.startsWith("non")) {
            // ... Other Card types
            if (cardState.getTypeWithChanges().hasStringType(property.substring(3))) {
                return false;
            }
        } else if (property.equals("CostsPhyrexianMana")) {
            if (!cardState.getManaCost().hasPhyrexian()) {
                return false;
            }
        } else if (property.startsWith("HasSVar")) {
            final String svar = property.substring(8);
            if (!cardState.hasSVar(svar)) {
                return false;
            }
        } else if (property.equals("ChosenType")) {
            if (!cardState.getTypeWithChanges().hasStringType(source.getChosenType())) {
                return false;
            }
        } else if (property.equals("IsNotChosenType")) {
            if (cardState.getTypeWithChanges().hasStringType(source.getChosenType())) {
                return false;
            }
        } else if (property.startsWith("HasSubtype")) {
            final String subType = property.substring(11);
            if (!cardState.getTypeWithChanges().hasSubtype(subType)) {
                return false;
            }
        } else if (property.startsWith("HasNoSubtype")) {
            final String subType = property.substring(13);
            if (cardState.getTypeWithChanges().hasSubtype(subType)) {
                return false;
            }
        } else if (property.equals("hasActivatedAbilityWithTapCost")) {
            for (final SpellAbility sa : cardState.getSpellAbilities()) {
                if (sa.isAbility() && (sa.getPayCosts() != null) && sa.getPayCosts().hasTapCost()) {
                    return true;
                }
            }
            return false;
        } else if (property.equals("hasActivatedAbility")) {
            for (final SpellAbility sa : cardState.getSpellAbilities()) {
                if (sa.isAbility()) {
                    return true;
                }
            }
            return false;
        } else if (property.equals("hasManaAbility")) {
            for (final SpellAbility sa : cardState.getSpellAbilities()) {
                if (sa.isManaAbility()) {
                    return true;
                }
            }
            return false;
        } else if (property.equals("hasNonManaActivatedAbility")) {
            for (final SpellAbility sa : cardState.getSpellAbilities()) {
                if (sa.isAbility() && !sa.isManaAbility()) {
                    return true;
                }
            }
            return false;
        } else if (property.startsWith("cmc")) {
            int x;
            String rhs = property.substring(5);
            int y = cardState.getManaCost().getCMC();
            try {
                x = Integer.parseInt(rhs);
            } catch (final NumberFormatException e) {
                x = AbilityUtils.calculateAmount(source, rhs, spellAbility);
            }

            if (!Expressions.compare(y, property, x)) {
                return false;
            }
        } else if (!cardState.getTypeWithChanges().hasStringType(property)) {
            return false;
        }
        
        return true;
        

        
    }


    public static boolean spellAbilityHasProperty(SpellAbility sa, String property, Player sourceController,
            Card source, SpellAbility spellAbility) {
        if (property.equals("ManaAbility")) {
            if (!sa.isManaAbility()) {
                return false;
            }
        } else if (property.equals("nonManaAbility")) {
            if (sa.isManaAbility()) {
                return false;
            }
        } else if (property.equals("Buyback")) {
            if (!sa.isBuyBackAbility()) {
                return false;
            }
        } else if (property.equals("Cycling")) {
            if (!sa.isCycling()) {
                return false;
            }
        } else if (property.equals("Dash")) {
            if (!sa.isDash()) {
                return false;
            }
        } else if (property.equals("Flashback")) {
            if (!sa.isFlashBackAbility()) {
                return false;
            }
        } else if (property.equals("Jumpstart")) {
            if (!sa.isJumpstart()) {
                return false;
            }
        } else if (property.equals("Kicked")) {
            if (!sa.isKicked()) {
                return false;
            }
        } else if (property.equals("Loyalty")) {
            if (!sa.isPwAbility()) {
                return false;
            }
        } else if (property.equals("Aftermath")) {
            if (!sa.isAftermath()) {
                return false;
            }
        } else if (property.equals("MorphUp")) {
            if (!sa.isMorphUp()) {
                return false;
            }
        } else if (property.equals("Equip")) {
            if (!sa.hasParam("Equip")) {
                return false;
            }
        } else if (property.equals("MayPlaySource")) {
            StaticAbility m = sa.getMayPlay();
            if (m == null) {
                return false;
            }
            if (!source.equals(m.getHostCard())) {
                return false;
            }
        } else if (property.startsWith("IsTargeting")) {
            String k[] = property.split(" ", 2);
            boolean found = false;
            for (GameObject o : AbilityUtils.getDefinedObjects(source, k[1], spellAbility)) {
                if (sa.isTargeting(o)) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                return false;
            }
        } else if (property.equals("YouCtrl")) {
            return sa.getActivatingPlayer().equals(sourceController);
        } else if (sa.getHostCard() != null) {
            if (!sa.getHostCard().hasProperty(property, sourceController, source, spellAbility)) {
                return false;
            }
        }

        return true;
    }
}
