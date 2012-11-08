package forge.card.abilityfactory.effects;

import java.util.List;
import com.google.common.collect.Iterables;

import forge.Card;
import forge.CardPredicates;
import forge.Constant;
import forge.Counters;
import forge.Singletons;
import forge.card.abilityfactory.AbilityFactory;
import forge.card.abilityfactory.SpellEffect;
import forge.card.cardfactory.CardFactoryUtil;
import forge.card.spellability.AbilityManaPart;
import forge.card.spellability.SpellAbility;
import forge.card.spellability.Target;
import forge.control.input.InputPayManaCostUtil;
import forge.game.player.Player;
import forge.game.zone.ZoneType;
import forge.gui.GuiChoose;

public class ManaEffect extends SpellEffect {
    
    /**
     * <p>
     * hasUrzaLands.
     * </p>
     * 
     * @param p
     *            a {@link forge.game.player.Player} object.
     * @return a boolean.
     */
    private static boolean hasUrzaLands(final Player p) {
        final List<Card> landsControlled = p.getCardsIn(ZoneType.Battlefield);
        return Iterables.any(landsControlled, CardPredicates.nameEquals("Urza's Mine")) &&  
                Iterables.any(landsControlled, CardPredicates.nameEquals("Urza's Tower")) && 
                Iterables.any(landsControlled, CardPredicates.nameEquals("Urza's Power Plant"));
    }



    @Override
    public void resolve(SpellAbility sa) {

        final Card card = sa.getSourceCard();

        AbilityManaPart abMana = sa.getManaPart();
        if (!AbilityFactory.checkConditional(sa)) {
            resolveDrawback(sa);
            return;
        }
        
        // Spells are not undoable
        sa.setUndoable(sa.isAbility() && sa.isUndoable());
    
    
        final List<Player> tgtPlayers = getTargetPlayers(sa);
        final Target tgt = sa.getTarget();
    
        if (abMana.isComboMana()) {
            for (Player p : tgtPlayers) {
                int amount = sa.hasParam("Amount") ? AbilityFactory.calculateAmount(card, sa.getParam("Amount"), sa) : 1;
                if (tgt == null || p.canBeTargetedBy(sa)) {
                    Player activator = sa.getActivatingPlayer(); 
                    // AI color choice is set in ComputerUtils so only human players need to make a choice
                    if (activator.isHuman()) {
                        //String colorsNeeded = abMana.getExpressChoice();
                        String[] colorsProduced = abMana.getComboColors().split(" ");
                        final StringBuilder choiceString = new StringBuilder();
                        String[] colorMenu = null;
                        if (!abMana.isAnyMana()) {
                            colorMenu = new String[colorsProduced.length];
                            //loop through colors to make menu
                            for (int nColor = 0; nColor < colorsProduced.length; nColor++) {
                                colorMenu[nColor] = InputPayManaCostUtil.getLongColorString(colorsProduced[nColor]);
                            }
                        }
                        else {
                            colorMenu = Constant.Color.ONLY_COLORS;
                        }
                        for (int nMana = 1; nMana <= amount; nMana++) {
                            String choice = "";
                                Object o = GuiChoose.one("Select Mana to Produce", colorMenu);
                                if (o == null) {
                                    final StringBuilder sb = new StringBuilder();
                                    sb.append("AbilityFactoryMana::manaResolve() - Human color mana choice is empty for ");
                                    sb.append(sa.getSourceCard().getName());
                                    throw new RuntimeException(sb.toString());
                                } else {
                                    choice = InputPayManaCostUtil.getShortColorString((String) o);
                                    if (nMana != 1) {
                                        choiceString.append(" ");
                                    }
                                    choiceString.append(choice);
                                }
                        }
                        abMana.setExpressChoice(choiceString.toString());
                    }
                    else {
                        // TODO: Add some logic for AI choice (ArsenalNut 2012/09/16)
                        if (sa.hasParam("AILogic")) {
                            final String logic = sa.getParam("AILogic");
                            String chosen = Constant.Color.BLACK;
                            if (logic.equals("MostProminentInComputerHand")) {
                                chosen = CardFactoryUtil.getMostProminentColor(activator.getCardsIn(
                                        ZoneType.Hand));
                            }
                            if (chosen.equals("")) {
                                chosen = Constant.Color.BLACK;
                            }
                            GuiChoose.one("Computer picked: ", new String[]{chosen});
                            abMana.setExpressChoice(InputPayManaCostUtil.getShortColorString(chosen));
                        }
                        if (abMana.getExpressChoice().isEmpty()) {
                            final StringBuilder sb = new StringBuilder();
                            sb.append("AbilityFactoryMana::manaResolve() - combo mana color choice is empty for ");
                            sb.append(sa.getSourceCard().getName());
                            throw new RuntimeException(sb.toString());
                        }
                    }
                }
            }
        }
        else if (abMana.isAnyMana()) {
            for (Player p : tgtPlayers) {
                if (tgt == null || p.canBeTargetedBy(sa)) {
                    Player act = sa.getActivatingPlayer(); 
                    // AI color choice is set in ComputerUtils so only human players need to make a choice
                    if (act.isHuman()) {
                        String colorsNeeded = abMana.getExpressChoice();
                        String choice = "";
                        if (colorsNeeded.length() == 1) {
                            choice = colorsNeeded;
                        }
                        else {
                            String[] colorMenu = null;
                            if (colorsNeeded.length() > 1 && colorsNeeded.length() < 5) {
                                colorMenu = new String[colorsNeeded.length()];
                                //loop through colors to make menu
                                for (int nChar = 0; nChar < colorsNeeded.length(); nChar++) {
                                    colorMenu[nChar] = InputPayManaCostUtil.getLongColorString(colorsNeeded.substring(nChar, nChar + 1));
                                }
                            }
                            else {
                                colorMenu = Constant.Color.ONLY_COLORS;
                            }
                            String s = GuiChoose.one("Select Mana to Produce", colorMenu);
                            if (s == null) {
                                final StringBuilder sb = new StringBuilder();
                                sb.append("AbilityFactoryMana::manaResolve() - Human color mana choice is empty for ");
                                sb.append(sa.getSourceCard().getName());
                                throw new RuntimeException(sb.toString());
                            } else {
                                choice = InputPayManaCostUtil.getShortColorString(s);
                            }
                        }
                        abMana.setExpressChoice(choice);
                    }
                    else {
                        if (sa.hasParam("AILogic")) {
                            final String logic = sa.getParam("AILogic");
                            String chosen = Constant.Color.BLACK;
                            if (logic.equals("MostProminentInComputerHand")) {
                                chosen = CardFactoryUtil.getMostProminentColor(act.getCardsIn(ZoneType.Hand));
                            }
                            GuiChoose.one("Computer picked: ", new String[]{chosen});
                            abMana.setExpressChoice(InputPayManaCostUtil.getShortColorString(chosen));
                        }
                        if (abMana.getExpressChoice().isEmpty()) {
                            final StringBuilder sb = new StringBuilder();
                            sb.append("AbilityFactoryMana::manaResolve() - any color mana choice is empty for ");
                            sb.append(sa.getSourceCard().getName());
                            throw new RuntimeException(sb.toString());
                        }
                    }
                }
            }
        }
    
        for (final Player player : tgtPlayers) {
            abMana.produceMana(generatedMana(sa), player, sa);
        }
    
        // Only clear express choice after mana has been produced
        abMana.clearExpressChoice();
    
        // convert these to SubAbilities when appropriate
        if (sa.hasParam("Stuck")) {
            sa.setUndoable(false);
            card.addExtrinsicKeyword("This card doesn't untap during your next untap step.");
        }
    
        final String deplete = sa.getParam("Deplete");
        if (deplete != null) {
            final int num = card.getCounters(Counters.getType(deplete));
            if (num == 0) {
                sa.setUndoable(false);
                Singletons.getModel().getGame().getAction().sacrifice(card, null);
            }
        }
    
        resolveDrawback(sa);
    }



    /**
     * <p>
     * generatedMana.
     * </p>
     * 
     * @param abMana
     *            a {@link forge.card.spellability.AbilityMana} object.
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @return a {@link java.lang.String} object.
     */
    private String generatedMana(final SpellAbility sa) {
        // Calculate generated mana here for stack description and resolving

        int amount = sa.hasParam("Amount") ? AbilityFactory.calculateAmount(sa.getSourceCard(), sa.getParam("Amount"), sa) : 1;
    
        AbilityManaPart abMana = sa.getManaPart();
        String baseMana;
        if (abMana.isComboMana()) {
            baseMana = abMana.getExpressChoice();
            if (baseMana.isEmpty()) {
                baseMana = abMana.getOrigProduced();
            }
        }
        else if (abMana.isAnyMana()) {
            baseMana = abMana.getExpressChoice();
            if (baseMana.isEmpty()) {
                baseMana = "Any";
            }
        }
        else {
            baseMana = abMana.mana();
        }
    
        if (sa.hasParam("Bonus")) {
            // For mana abilities that get a bonus
            // Bonus currently MULTIPLIES the base amount. Base Amounts should
            // ALWAYS be Base
            int bonus = 0;
            if (sa.getParam("Bonus").equals("UrzaLands")) {
                if (hasUrzaLands(sa.getActivatingPlayer())) {
                    bonus = Integer.parseInt(sa.getParam("BonusProduced"));
                }
            }
    
            amount += bonus;
        }
    
        try {
            if ((sa.getParam("Amount") != null) && (amount != Integer.parseInt(sa.getParam("Amount")))) {
                sa.setUndoable(false);
            }
        } catch (final NumberFormatException n) {
            sa.setUndoable(false);
        }
    
        final StringBuilder sb = new StringBuilder();
        if (amount == 0) {
            sb.append("0");
        }
        else if (abMana.isComboMana()) {
            // amount is already taken care of in resolve method for combination mana, just append baseMana
            sb.append(baseMana);
        }
        else {
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


    /**
     * <p>
     * manaStackDescription.
     * </p>
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @param abMana
     *            a {@link forge.card.spellability.AbilityMana} object.
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * 
     * @return a {@link java.lang.String} object.
     */
    
    @Override
    protected String getStackDescription(SpellAbility sa) {
        final StringBuilder sb = new StringBuilder();
        sb.append("Add ").append(generatedMana(sa)).append(" to your mana pool.");
        return sb.toString();
    }
}