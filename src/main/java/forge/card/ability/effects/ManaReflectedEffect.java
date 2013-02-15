package forge.card.ability.effects;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import forge.Card;
import forge.CardUtil;
import forge.card.MagicColor;
import forge.card.ability.AbilityUtils;
import forge.card.ability.SpellEffect;
import forge.card.spellability.AbilityManaPart;
import forge.card.spellability.SpellAbility;
import forge.game.player.Player;
import forge.gui.GuiChoose;

public class ManaReflectedEffect extends SpellEffect {

    /* (non-Javadoc)
     * @see forge.card.abilityfactory.SpellEffect#resolve(java.util.Map, forge.card.spellability.SpellAbility)
     */
    @Override
    public void resolve(SpellAbility sa) {

        // Spells are not undoable
        AbilityManaPart ma = sa.getManaPart();
        sa.setUndoable(sa.isAbility() && sa.isUndoable());

        final Collection<String> colors = CardUtil.getReflectableManaColors(sa, sa, new HashSet<String>(), new ArrayList<Card>());

        final List<Player> tgtPlayers = getTargetPlayers(sa);
        for (final Player player : tgtPlayers) {
            final String generated = generatedReflectedMana(sa, colors, player);
            if (ma.getCanceled()) {
                sa.undo();
                ma.setCanceled(false);
                return;
            }

            ma.produceMana(generated, player, sa);
        }

        resolveDrawback(sa);
    }


    // *************** Utility Functions **********************

    /**
     * <p>
     * generatedReflectedMana.
     * </p>
     * 
     * @param sa
     *            a {@link forge.card.spellability.AbilityMana} object.
     * @param af
     *            a {@link forge.card.ability.AbilityFactory} object.
     * @param colors
     *            a {@link java.util.ArrayList} object.
     * @param player
     *            a {@link forge.game.player.Player} object.
     * @return a {@link java.lang.String} object.
     */
    private static String generatedReflectedMana(final SpellAbility sa, final Collection<String> colors, final Player player) {
        // Calculate generated mana here for stack description and resolving
        final int amount = sa.hasParam("Amount") ? AbilityUtils.calculateAmount(sa.getSourceCard(), sa.getParam("Amount"), sa) : 1;

        String baseMana = "";

        if (colors.size() == 0) {
            return "0";
        } else if (colors.size() == 1) {
            baseMana = MagicColor.toShortString(colors.iterator().next());
        } else {
            if (player.isHuman()) {
                final Object o = GuiChoose.oneOrNone("Select Mana to Produce", colors);
                if (o == null) {
                    // User hit cancel
                    sa.getManaPart().setCanceled(true);
                    return "";
                } else {
                    baseMana = MagicColor.toShortString((String) o);
                }
            } else {
                // AI doesn't really have anything here yet
                baseMana = MagicColor.toShortString(colors.iterator().next());
            }
        }

        final StringBuilder sb = new StringBuilder();
        if (amount == 0) {
            sb.append("0");
        } else {
            try {
                // if baseMana is an integer(colorless), just multiply amount
                // and baseMana
                final int base = Integer.parseInt(baseMana);
                sb.append(base * amount);
            } catch (final NumberFormatException e) {
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
