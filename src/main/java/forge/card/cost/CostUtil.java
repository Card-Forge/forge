package forge.card.cost;

import java.util.Random;

import forge.AllZone;
import forge.Card;
import forge.CardList;
import forge.ComputerUtil;
import forge.Constant.Zone;
import forge.Counters;
import forge.card.abilityFactory.AbilityFactory;
import forge.card.spellability.SpellAbility;
import forge.gui.GuiUtils;
import forge.gui.input.Input;

/**
 * The Class CostUtil.
 */
public class CostUtil {
    private static Random r = new Random();
    private static double P1P1Percent = .25;
    private static double OtherPercent = .9;

    /**
     * Check sacrifice cost.
     * 
     * @param cost
     *            the cost
     * @param source
     *            the source
     * @return true, if successful
     */
    public static boolean checkSacrificeCost(final Cost cost, final Card source) {
        for (CostPart part : cost.getCostParts()) {
            if (part instanceof CostSacrifice) {
                CostSacrifice sac = (CostSacrifice) part;

                String type = sac.getType();

                if (type.equals("CARDNAME")) {
                    continue;
                }

                CardList typeList = AllZone.getComputerPlayer().getCardsIn(Zone.Battlefield);
                typeList = typeList.getValidCards(type.split(","), source.getController(), source);
                if (ComputerUtil.getCardPreference(source, "SacCost", typeList) == null) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Check creature sacrifice cost.
     * 
     * @param cost
     *            the cost
     * @param source
     *            the source
     * @return true, if successful
     */
    public static boolean checkCreatureSacrificeCost(final Cost cost, final Card source) {
        for (CostPart part : cost.getCostParts()) {
            if (part instanceof CostSacrifice) {
                CostSacrifice sac = (CostSacrifice) part;
                if (sac.getThis() && source.isCreature()) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Check life cost.
     * 
     * @param cost
     *            the cost
     * @param source
     *            the source
     * @param remainingLife
     *            the remaining life
     * @return true, if successful
     */
    public static boolean checkLifeCost(final Cost cost, final Card source, final int remainingLife) {
        for (CostPart part : cost.getCostParts()) {
            if (part instanceof CostPayLife) {
                CostPayLife payLife = (CostPayLife) part;
                if (AllZone.getComputerPlayer().getLife() - payLife.convertAmount() < remainingLife) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Check discard cost.
     * 
     * @param cost
     *            the cost
     * @param source
     *            the source
     * @return true, if successful
     */
    public static boolean checkDiscardCost(final Cost cost, final Card source) {
        for (CostPart part : cost.getCostParts()) {
            if (part instanceof CostDiscard) {
                CostDiscard disc = (CostDiscard) part;

                String type = disc.getType();
                CardList typeList = AllZone.getComputerPlayer().getCardsIn(Zone.Hand);
                typeList = typeList.getValidCards(type.split(","), source.getController(), source);
                if (ComputerUtil.getCardPreference(source, "DiscardCost", typeList) == null) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Check remove counter cost.
     * 
     * @param cost
     *            the cost
     * @param source
     *            the source
     * @return true, if successful
     */
    public static boolean checkRemoveCounterCost(final Cost cost, final Card source) {
        for (CostPart part : cost.getCostParts()) {
            if (part instanceof CostRemoveCounter) {
                CostRemoveCounter remCounter = (CostRemoveCounter) part;

                // A card has a 25% chance per counter to be able to pass
                // through here
                // 4+ counters will always pass. 0 counters will never
                Counters type = remCounter.getCounter();
                double percent = type.name().equals("P1P1") ? P1P1Percent : OtherPercent;
                int currentNum = source.getCounters(type);

                double chance = percent * (currentNum / part.convertAmount());
                if (chance <= r.nextFloat()) {
                    return false;
                }
            }
        }

        return true;
    }

    /**
     * Check add m1 m1 counter cost.
     * 
     * @param cost
     *            the cost
     * @param source
     *            the source
     * @return true, if successful
     */
    public static boolean checkAddM1M1CounterCost(final Cost cost, final Card source) {
        for (CostPart part : cost.getCostParts()) {
            if (part instanceof CostPutCounter) {
                CostPutCounter addCounter = (CostPutCounter) part;
                Counters type = addCounter.getCounter();

                if (type.equals(Counters.M1M1)) {
                    return false;
                }
            }
        }

        return true;
    }

    /**
     * Checks for discard hand cost.
     * 
     * @param cost
     *            the cost
     * @return true, if successful
     */
    public static boolean hasDiscardHandCost(final Cost cost) {
        for (CostPart part : cost.getCostParts()) {
            if (part instanceof CostDiscard) {
                CostDiscard disc = (CostDiscard) part;
                if (disc.getType().equals("Hand")) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Determine amount.
     * 
     * @param part
     *            the part
     * @param source
     *            the source
     * @param ability
     *            the ability
     * @param maxChoice
     *            the max choice
     * @return the integer
     */
    public static Integer determineAmount(final CostPart part, final Card source, final SpellAbility ability, final int maxChoice) {
        String amount = part.getAmount();
        Integer c = part.convertAmount();
        if (c == null) {
            String sVar = source.getSVar(amount);
            // Generalize this
            if (sVar.equals("XChoice")) {
                c = CostUtil.chooseXValue(source, maxChoice);
            } else {
                c = AbilityFactory.calculateAmount(source, amount, ability);
            }
        }
        return c;
    }

    /**
     * Choose x value.
     * 
     * @param card
     *            the card
     * @param maxValue
     *            the max value
     * @return the int
     */
    public static int chooseXValue(final Card card, final int maxValue) {
        String chosen = card.getSVar("ChosenX");
        if (chosen.length() > 0) {
            return AbilityFactory.calculateAmount(card, "ChosenX", null);
        }

        Integer[] choiceArray = new Integer[maxValue + 1];
        for (int i = 0; i < choiceArray.length; i++) {
            choiceArray[i] = i;
        }
        Object o = GuiUtils.getChoice(card.toString() + " - Choose a Value for X", choiceArray);
        int chosenX = (Integer) o;
        card.setSVar("ChosenX", "Number$" + Integer.toString(chosenX));

        return chosenX;
    }

    /**
     * <p>
     * setInput.
     * </p>
     * 
     * @param in
     *            a {@link forge.gui.input.Input} object.
     */
    public static void setInput(final Input in) {
        // Just a shortcut..
        AllZone.getInputControl().setInput(in, true);
    }
}
