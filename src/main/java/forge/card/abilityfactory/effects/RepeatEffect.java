package forge.card.abilityfactory.effects;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import forge.Card;
import forge.CardLists;
import forge.GameActionUtil;
import forge.Singletons;
import forge.card.abilityfactory.AbilityFactory;
import forge.card.abilityfactory.SpellEffect;
import forge.card.cardfactory.CardFactoryUtil;
import forge.card.spellability.AbilitySub;
import forge.card.spellability.SpellAbility;
import forge.game.zone.ZoneType;
import forge.util.Expressions;

public class RepeatEffect extends SpellEffect {
    
    
    /**
     * <p>
     * repeatStackDescription.
     * </p>
     * 
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @return a {@link java.lang.String} object.
     */
    
    @Override
    protected String getStackDescription(java.util.Map<String,String> params, SpellAbility sa) {
        return "Repeat something. Somebody should really write a better StackDescription!";
    } // end repeatStackDescription()

    /**
     * <p>
     * repeatResolve.
     * </p>
     * 
     * @param AF
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @param SA
     *            a {@link forge.card.spellability.SpellAbility} object.
     */
    
    @Override
    public void resolve(java.util.Map<String,String> params, SpellAbility sa) {
        final AbilityFactory afRepeat = new AbilityFactory();
        Card source = sa.getSourceCard();
    
        // setup subability to repeat
        final SpellAbility repeat = afRepeat.getAbility(sa.getSourceCard().getSVar(params.get("RepeatSubAbility")), source);
        repeat.setActivatingPlayer(sa.getActivatingPlayer());
        ((AbilitySub) repeat).setParent(sa);
    
        Integer maxRepeat = null;
        if (params.containsKey("MaxRepeat")) {
            maxRepeat = AbilityFactory.calculateAmount(sa.getSourceCard(), params.get("MaxRepeat"), sa);
        }
        
        //execute repeat ability at least once
        int count = 0;
        do {
             AbilityFactory.resolve(repeat, false);
             count++;
             if (maxRepeat != null && maxRepeat <= count) {
                 // TODO Replace Infinite Loop Break with a game draw. Here are the scenarios that can cause this:
                 // Helm of Obedience vs Graveyard to Library replacement effect
                 StringBuilder infLoop = new StringBuilder(sa.getSourceCard().toString());
                 infLoop.append(" - To avoid an infinite loop, this repeat has been broken ");
                 infLoop.append(" and the game will now continue in the current state, ending the loop early. ");
                 infLoop.append("Once Draws are available this probably should change to a Draw.");
                 System.out.println(infLoop.toString());
                 break;
             }
       } while (checkRepeatConditions(params, sa));
    
    }
// end class AbilityFactory_Repeat
    
    /**
     * <p>
     * checkRepeatConditions.
     * </p>
     * 
     * @param AF
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @param SA
     *            a {@link forge.card.spellability.SpellAbility} object.
     */
    private boolean checkRepeatConditions(final Map<String, String> params, final SpellAbility sa) {
        //boolean doAgain = false;
    
        if (params.containsKey("RepeatPresent")) {
            final String repeatPresent = params.get("RepeatPresent");
            List<Card> list = new ArrayList<Card>();
    
            String repeatCompare = "GE1";
            if (params.containsKey("RepeatCompare")) {
                repeatCompare = params.get("RepeatCompare");
            }
    
            if (params.containsKey("RepeatDefined")) {
                list.addAll(AbilityFactory.getDefinedCards(sa.getSourceCard(), params.get("RepeatDefined"), sa));
            } else {
                list = Singletons.getModel().getGame().getCardsIn(ZoneType.Battlefield);
            }
    
            list = CardLists.getValidCards(list, repeatPresent.split(","), sa.getActivatingPlayer(), sa.getSourceCard());
    
            int right;
            final String rightString = repeatCompare.substring(2);
            try { // If this is an Integer, just parse it
                right = Integer.parseInt(rightString);
            } catch (final NumberFormatException e) { // Otherwise, grab it from
                                                      // the
                // SVar
                right = CardFactoryUtil.xCount(sa.getSourceCard(), sa.getSourceCard().getSVar(rightString));
            }
    
            final int left = list.size();
    
            if (!Expressions.compare(left, repeatCompare, right)) {
                return false;
            }
        }
    
        if (params.containsKey("RepeatCheckSVar")) {
            String sVarOperator = "GE";
            String sVarOperand = "1";
            if (params.containsKey("RepeatSVarCompare")) {
                sVarOperator = params.get("RepeatSVarCompare").substring(0, 2);
                sVarOperand = params.get("RepeatSVarCompare").substring(2);
            }
            final int svarValue = AbilityFactory.calculateAmount(sa.getSourceCard(), params.get("RepeatCheckSVar"), sa);
            final int operandValue = AbilityFactory.calculateAmount(sa.getSourceCard(), sVarOperand, sa);
    
            if (!Expressions.compare(svarValue, sVarOperator, operandValue)) {
                return false;
            }
        }
    
        if (params.containsKey("RepeatOptional")) {
            if (sa.getActivatingPlayer().isComputer()) {
                //TODO add logic to have computer make better choice (ArsenalNut)
                return false;
            } else {
                final StringBuilder sb = new StringBuilder();
                sb.append("Do you want to repeat this process again?");
                if (!GameActionUtil.showYesNoDialog(sa.getSourceCard(), sb.toString())) {
                    return false;
                }
            }
        }
    
        return true;
    }
}