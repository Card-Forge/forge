package forge.game.card;

import forge.card.CardTypeView;
import forge.card.ColorSet;
import forge.card.MagicColor;
import forge.game.CardTraitBase;
import forge.game.ability.AbilityUtils;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.staticability.StaticAbilityColorlessDamageSource;
import forge.game.trigger.Trigger;
import forge.util.Expressions;
import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;

public class CardStateProperty {
    CardState cardState;
    Player sourceController;
    Card source;

    public CardStateProperty(CardState state, Player player, Card source) {
        this.cardState = state;
        this.sourceController = player;
        this.source = source;
    }

    public boolean hasProperty(String property, CardTraitBase spellAbility) {
        boolean withSource = property.endsWith("Source");
        final ColorSet colors;
        if (withSource && StaticAbilityColorlessDamageSource.colorlessDamageSource(cardState)) {
            colors = ColorSet.C;
        } else {
            colors = cardState.getCard().getColor(cardState);
        }

        final CardTypeView type = cardState.getTypeWithChanges();
        if (property.contains("White") || property.contains("Blue") || property.contains("Black")
                || property.contains("Red") || property.contains("Green")) {
            boolean mustHave = !property.startsWith("non");
            final String colorName = property.substring(mustHave ? 0 : 3, property.length() - (withSource ? 6 : 0));

            int desiredColor = MagicColor.fromName(colorName);
            boolean hasColor = colors.hasAnyColor(desiredColor);
            return mustHave == hasColor;
        } else if (property.contains("Colorless")) {
            boolean non = property.startsWith("non");
            return non != colors.isColorless();
        } else if (property.startsWith("MultiColor")) {
            // ... Card is multicolored
            return colors.isMulticolor();
        } else if (property.startsWith("EnemyColor")) {
            if (colors.countColors() != 2) {
                return false;
            }
            // i want only enemy colors
            for (final byte pair : Arrays.copyOfRange(MagicColor.COLORPAIR, 5, 10)) {
                if (colors.hasExactlyColor(pair)) {
                    return true;
                }
            }
            return false;
        } else if (property.startsWith("AllColors")) {
            return colors.isAllColors();
        } else if (property.startsWith("MonoColor")) {
            return colors.isMonoColor();
        } else if (property.startsWith("ChosenColor")) {
            return source.hasChosenColor() && colors.hasAnyColor(MagicColor.fromName(source.getChosenColor()));
        } else if (property.startsWith("AnyChosenColor")) {
            return source.hasChosenColor()
                    && colors.hasAnyColor(ColorSet.fromNames(source.getChosenColors()).getColor());
        } else if (property.equals("AssociatedWithChosenColor")) {
            final String color = source.getChosenColor();
            switch (color) {
                case "white":
                    return type.hasSubtype("Plains");
                case "blue":
                    return type.hasSubtype("Island");
                case "black":
                    return type.hasSubtype("Swamp");
                case "red":
                    return type.hasSubtype("Mountain");
                case "green":
                    return type.hasSubtype("Forest");
                default:
                    return false;
            }
        } else if (property.equals("Worthy")) {
            return cardState.isWorthy();
        } else if (property.equals("Outlaw")) {
            return type.isOutlaw();
        } else if (property.equals("Party")) {
            return type.isParty();
        } else if (property.startsWith("non")) {
            // ... Other Card types
            return !type.hasStringType(property.substring(3));
        } else if (property.equals("CostsPhyrexianMana")) {
            return cardState.getManaCost().hasPhyrexian();
        } else if (property.startsWith("HasSVar")) {
            final String svar = property.substring(8);
            return cardState.hasSVar(svar);
        } else if (property.equals("ChosenType")) {
            String chosenType = source.getChosenType();
            if (chosenType.startsWith("Non")) {
                return !type.hasStringType(StringUtils.capitalize(chosenType.substring(3)));
            }
            return type.hasStringType(chosenType);
        } else if (property.equals("IsNotChosenType")) {
            return !type.hasStringType(source.getChosenType());
        } else if (property.equals("ChosenType2")) {
            return type.hasStringType(source.getChosenType2());
        } else if (property.equals("NotedType")) {
            boolean found = false;
            for (String s : source.getNotedTypes()) {
                if (type.hasStringType(s)) {
                    found = true;
                    break;
                }
            }
            return found;
        } else if (property.startsWith("hasAbility")) {
            String valid = property.substring(11);
            for (final SpellAbility sa : cardState.getSpellAbilities()) {
                if (sa.isValid(valid, sourceController, source, spellAbility)) {
                    return true;
                }
            }
            return false;
        } else if (property.equals("hasManaAbility")) {
            if (!cardState.getManaAbilities().isEmpty()) {
                return true;
            }
            for (final Trigger trig : cardState.getTriggers()) {
                SpellAbility sa = trig.getOverridingAbility();
                if (sa != null) {
                    if (!sa.isTrigger()) {
                        sa.setTrigger(trig);
                    }
                    if (sa.isManaAbility()) {
                        return true;
                    }
                }
            }
            return false;
        } else if (property.equals("hasNonManaActivatedAbility")) {
            for (final SpellAbility sa : cardState.getNonManaAbilities()) {
                if (sa.isActivatedAbility()) {
                    return true;
                }
            }
            return false;
        } else if (property.startsWith("cmc")) {
            String rhs = property.substring(5);
            int y = cardState.getManaCost().getCMC();
            int x = AbilityUtils.calculateAmount(source, rhs, spellAbility);

            return Expressions.compare(y, property, x);
        }

        return type.hasStringType(property);
    }
}