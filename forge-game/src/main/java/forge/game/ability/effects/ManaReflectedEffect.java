package forge.game.ability.effects;

import java.util.Collection;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import forge.card.ColorSet;
import forge.card.MagicColor;
import forge.game.ability.AbilityUtils;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.CardUtil;
import forge.game.player.Player;
import forge.game.spellability.AbilityManaPart;
import forge.game.spellability.SpellAbility;
import forge.util.Localizer;

public class ManaReflectedEffect extends SpellAbilityEffect {

    @Override
    public void buildSpellAbility(SpellAbility sa) {
        sa.setManaPart(new AbilityManaPart(sa, sa.getMapParams()));
        if (sa.getParent() == null) {
            sa.setUndoable(true); // will try at least
        }
    }

    /* (non-Javadoc)
     * @see forge.card.abilityfactory.SpellEffect#resolve(java.util.Map, forge.card.spellability.SpellAbility)
     */
    @Override
    public void resolve(SpellAbility sa) {
        final Collection<String> colors = CardUtil.getReflectableManaColors(sa);
        final AbilityManaPart ma = sa.getManaPart();

        // Spells are not undoable
        sa.setUndoable(sa.isAbility() && sa.isUndoable() && sa.getSubAbility() == null);

        final StringBuilder producedMana = new StringBuilder();
        for (final Player player : getTargetPlayers(sa)) {
            final String generated = generatedReflectedMana(sa, colors, player);
            producedMana.append(ma.produceMana(generated, player, sa));
        }

        ma.tapsForMana(sa.getRootAbility(), producedMana.toString());
    }

    // *************** Utility Functions **********************

    /**
     * <p>
     * generatedReflectedMana.
     * </p>
     *
     * @param sa
     *            a {@link forge.game.spellability.SpellAbility} object.
     * @param colors
     *            a {@link java.util.ArrayList} object.
     * @param player
     *            a {@link forge.game.player.Player} object.
     * @return a {@link java.lang.String} object.
     */
    private static String generatedReflectedMana(final SpellAbility sa, final Collection<String> colors, final Player player) {
        // Calculate generated mana here for stack description and resolving
        final int amount = sa.hasParam("Amount") ? AbilityUtils.calculateAmount(sa.getHostCard(), sa.getParam("Amount"), sa) : 1;
        final StringBuilder sb = new StringBuilder();

        if (sa.getManaPart().isComboMana()) {
            Map<Byte, Integer> choices = player.getController().specifyManaCombo(sa, ColorSet.fromNames(colors), amount, false);
            for (Map.Entry<Byte, Integer> e : choices.entrySet()) {
                Byte chosenColor = e.getKey();
                String choice = MagicColor.toShortString(chosenColor);
                Integer count = e.getValue();
                while (count > 0) {
                    if (sb.length() > 0) {
                        sb.append(" ");
                    }
                    sb.append(choice);
                    --count;
                }
            }
            return sb.toString();
        }

        String baseMana;

        // TODO: This effect explicitly obeys express color choice as set by auto payment and AI routines in order
        // to avoid misplays and auto mana payment selection errors. Perhaps a better solution is possible?
        String expressChoiceColors = sa.getManaPart().getExpressChoice();
        ColorSet colorMenu = null;
        byte mask = 0;
        // loop through colors to make menu
        for (int nChar = 0; nChar < expressChoiceColors.length(); nChar++) {
            mask |= MagicColor.fromName(expressChoiceColors.charAt(nChar));
        }

        if (mask == 0 && !expressChoiceColors.isEmpty() && colors.contains("colorless")) {
            baseMana = player.getController().chooseColorAllowColorless(Localizer.getInstance().getMessage("lblSelectManaProduce"), sa.getHostCard(), ColorSet.fromMask(mask)).getShortName();
        } else {
            // Nothing set previously so ask player if needed
            if (mask == 0) {
                if (colors.isEmpty()) {
                    return "0";
                } else if (colors.size() == 1) {
                    baseMana = MagicColor.toShortString(colors.iterator().next());
                } else {
                    if (colors.contains("colorless")) {
                        baseMana = player.getController().chooseColorAllowColorless(Localizer.getInstance().getMessage("lblSelectManaProduce"), sa.getHostCard(), ColorSet.fromNames(colors)).getShortName();
                    } else {
                        baseMana = player.getController().chooseColor(Localizer.getInstance().getMessage("lblSelectManaProduce"), sa, ColorSet.fromNames(colors)).getShortName();
                    }
                }
            } else {
                colorMenu = ColorSet.fromMask(mask);
                MagicColor.Color color = sa.getActivatingPlayer().getController().chooseColor(Localizer.getInstance().getMessage("lblSelectManaProduce"), sa, colorMenu);
                if (color == null) {
                    System.err.println("Unexpected behavior in ManaReflectedEffect: " + sa.getActivatingPlayer() + " - color mana choice is empty for " + sa.getHostCard().getName());
                }
                baseMana = color.getShortName();
            }
        }

        if (amount == 0) {
            sb.append("0");
        } else {
            if (StringUtils.isNumeric(baseMana)) {
                // if baseMana is an integer(colorless), just multiply amount
                // and baseMana
                final int base = Integer.parseInt(baseMana);
                sb.append(base * amount);
            } else {
                for (int i = 0; i < amount; i++) {
                    if (i != 0) {
                        sb.append(" ");
                    }
                    sb.append(baseMana);
                }
            }
        }
        return sb.toString();
    }
}
