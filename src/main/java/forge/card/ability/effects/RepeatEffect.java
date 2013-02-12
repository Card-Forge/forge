package forge.card.ability.effects;

import java.util.ArrayList;
import java.util.List;
import forge.Card;
import forge.CardLists;
import forge.Singletons;
import forge.card.ability.AbilityFactory;
import forge.card.ability.AbilityUtils;
import forge.card.ability.SpellEffect;
import forge.card.cardfactory.CardFactoryUtil;
import forge.card.spellability.AbilitySub;
import forge.card.spellability.SpellAbility;
import forge.game.zone.ZoneType;
import forge.gui.GuiDialog;
import forge.util.Expressions;

public class RepeatEffect extends SpellEffect {

    @Override
    protected String getStackDescription(SpellAbility sa) {
        return "Repeat something. Somebody should really write a better StackDescription!";
    } // end repeatStackDescription()

    @Override
    public void resolve(SpellAbility sa) {
        final AbilityFactory afRepeat = new AbilityFactory();
        Card source = sa.getSourceCard();

        // setup subability to repeat
        final SpellAbility repeat = afRepeat.getAbility(sa.getSourceCard().getSVar(sa.getParam("RepeatSubAbility")), source);
        repeat.setActivatingPlayer(sa.getActivatingPlayer());
        ((AbilitySub) repeat).setParent(sa);

        Integer maxRepeat = null;
        if (sa.hasParam("MaxRepeat")) {
            maxRepeat = AbilityUtils.calculateAmount(sa.getSourceCard(), sa.getParam("MaxRepeat"), sa);
        }

        //execute repeat ability at least once
        int count = 0;
        do {
             AbilityUtils.resolve(repeat, false);
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
       } while (checkRepeatConditions(sa));

    }
// end class AbilityFactory_Repeat

    /**
     * <p>
     * checkRepeatConditions.
     * </p>
     * 
     * @param AF
     *            a {@link forge.card.ability.AbilityFactory} object.
     * @param SA
     *            a {@link forge.card.spellability.SpellAbility} object.
     */
    private boolean checkRepeatConditions(final SpellAbility sa) {
        //boolean doAgain = false;

        if (sa.hasParam("RepeatPresent")) {
            final String repeatPresent = sa.getParam("RepeatPresent");
            List<Card> list = new ArrayList<Card>();

            String repeatCompare = "GE1";
            if (sa.hasParam("RepeatCompare")) {
                repeatCompare = sa.getParam("RepeatCompare");
            }

            if (sa.hasParam("RepeatDefined")) {
                list.addAll(AbilityUtils.getDefinedCards(sa.getSourceCard(), sa.getParam("RepeatDefined"), sa));
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

        if (sa.hasParam("RepeatCheckSVar")) {
            String sVarOperator = "GE";
            String sVarOperand = "1";
            if (sa.hasParam("RepeatSVarCompare")) {
                sVarOperator = sa.getParam("RepeatSVarCompare").substring(0, 2);
                sVarOperand = sa.getParam("RepeatSVarCompare").substring(2);
            }
            final int svarValue = AbilityUtils.calculateAmount(sa.getSourceCard(), sa.getParam("RepeatCheckSVar"), sa);
            final int operandValue = AbilityUtils.calculateAmount(sa.getSourceCard(), sVarOperand, sa);

            if (!Expressions.compare(svarValue, sVarOperator, operandValue)) {
                return false;
            }
        }

        if (sa.hasParam("RepeatOptional")) {
            if (sa.getActivatingPlayer().isComputer()) {
                //TODO add logic to have computer make better choice (ArsenalNut)
                return false;
            } else {
                final StringBuilder sb = new StringBuilder();
                sb.append("Do you want to repeat this process again?");
                if (!GuiDialog.confirm(sa.getSourceCard(), sb.toString())) {
                    return false;
                }
            }
        }

        return true;
    }
}
