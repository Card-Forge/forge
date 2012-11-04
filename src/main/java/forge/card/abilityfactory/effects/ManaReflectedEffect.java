package forge.card.abilityfactory.effects;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import forge.Card;
import forge.CardUtil;
import forge.card.abilityfactory.AbilityFactory;
import forge.card.abilityfactory.SpellEffect;
import forge.card.spellability.AbilityManaPart;
import forge.card.spellability.SpellAbility;
import forge.control.input.InputPayManaCostUtil;
import forge.game.player.Player;
import forge.gui.GuiChoose;

public class ManaReflectedEffect extends SpellEffect {
    

    /* (non-Javadoc)
     * @see forge.card.abilityfactory.SpellEffect#resolve(java.util.Map, forge.card.spellability.SpellAbility)
     */
    @Override
    public void resolve(Map<String, String> params, SpellAbility sa) {

        // Spells are not undoable
        AbilityManaPart ma = sa.getManaPart();
        ma.setUndoable(sa.getAbilityFactory().isAbility() && ma.isUndoable());
    
        final List<String> colors = CardUtil.getReflectableManaColors(sa, params, new ArrayList<String>(),
                new ArrayList<Card>());
    
        final List<Player> tgtPlayers = getTargetPlayers(sa, params);
        for (final Player player : tgtPlayers) {
            final String generated = generatedReflectedMana(params, sa, colors, player);
            if (ma.getCanceled()) {
                ma.undo();
                ma.setCanceled(false);
                return;
            }
    
            ma.produceMana(generated, player);
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
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @param colors
     *            a {@link java.util.ArrayList} object.
     * @param player
     *            a {@link forge.game.player.Player} object.
     * @return a {@link java.lang.String} object.
     */
    private static String generatedReflectedMana(final Map<String, String> params, final SpellAbility sa, final List<String> colors, final Player player) {
        // Calculate generated mana here for stack description and resolving
        final int amount = params.containsKey("Amount") ? AbilityFactory.calculateAmount(sa.getSourceCard(), params.get("Amount"), sa) : 1;
    
        String baseMana = "";
    
        if (colors.size() == 0) {
            return "0";
        } else if (colors.size() == 1) {
            baseMana = InputPayManaCostUtil.getShortColorString(colors.get(0));
        } else {
            if (player.isHuman()) {
                final Object o = GuiChoose.oneOrNone("Select Mana to Produce", colors);
                if (o == null) {
                    // User hit cancel
                    sa.getManaPart().setCanceled(true);
                    return "";
                } else {
                    baseMana = InputPayManaCostUtil.getShortColorString((String) o);
                }
            } else {
                // AI doesn't really have anything here yet
                baseMana = InputPayManaCostUtil.getShortColorString(colors.get(0));
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