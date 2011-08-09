package forge.card.spellability;

import forge.AllZone;
import forge.Card;
import forge.Counters;
import forge.card.mana.ManaCost;

import java.util.regex.Pattern;

/**
 * <p>Cost class.</p>
 *
 * @author Forge
 * @version $Id$
 */
public class Cost {
    private boolean isAbility = true;

    private boolean sacCost = false;

    /**
     * <p>Getter for the field <code>sacCost</code>.</p>
     *
     * @return a boolean.
     */
    public boolean getSacCost() {
        return sacCost;
    }

    private String sacType = "";    // <type> or CARDNAME

    /**
     * <p>Getter for the field <code>sacType</code>.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String getSacType() {
        return sacType;
    }

    private boolean sacThis = false;

    /**
     * <p>Getter for the field <code>sacThis</code>.</p>
     *
     * @return a boolean.
     */
    public boolean getSacThis() {
        return sacThis;
    }

    private int sacAmount = 0;

    /**
     * <p>Getter for the field <code>sacAmount</code>.</p>
     *
     * @return a int.
     */
    public int getSacAmount() {
        return sacAmount;
    }

    private boolean sacX = false;

    /**
     * <p>isSacX.</p>
     *
     * @return a boolean.
     */
    public boolean isSacX() {
        return sacX;
    }

    private boolean sacAll = false;

    /**
     * <p>isSacAll.</p>
     *
     * @return a boolean.
     */
    public boolean isSacAll() {
        return sacAll;
    }

    private boolean exileCost = false;

    /**
     * <p>Getter for the field <code>exileCost</code>.</p>
     *
     * @return a boolean.
     */
    public boolean getExileCost() {
        return exileCost;
    }

    private String exileType = "";    // <type> or CARDNAME

    /**
     * <p>Getter for the field <code>exileType</code>.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String getExileType() {
        return exileType;
    }

    private boolean exileThis = false;

    /**
     * <p>Getter for the field <code>exileThis</code>.</p>
     *
     * @return a boolean.
     */
    public boolean getExileThis() {
        return exileThis;
    }

    private int exileAmount = 0;

    /**
     * <p>Getter for the field <code>exileAmount</code>.</p>
     *
     * @return a int.
     */
    public int getExileAmount() {
        return exileAmount;
    }

    private boolean exileFromHandCost = false;

    /**
     * <p>Getter for the field <code>exileFromHandCost</code>.</p>
     *
     * @return a boolean.
     */
    public boolean getExileFromHandCost() {
        return exileFromHandCost;
    }

    private String exileFromHandType = "";    // <type> or CARDNAME

    /**
     * <p>Getter for the field <code>exileFromHandType</code>.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String getExileFromHandType() {
        return exileFromHandType;
    }

    private boolean exileFromHandThis = false;

    /**
     * <p>Getter for the field <code>exileFromHandThis</code>.</p>
     *
     * @return a boolean.
     */
    public boolean getExileFromHandThis() {
        return exileFromHandThis;
    }

    private int exileFromHandAmount = 0;

    /**
     * <p>Getter for the field <code>exileFromHandAmount</code>.</p>
     *
     * @return a int.
     */
    public int getExileFromHandAmount() {
        return exileFromHandAmount;
    }

    private boolean exileFromGraveCost = false;

    /**
     * <p>Getter for the field <code>exileFromGraveCost</code>.</p>
     *
     * @return a boolean.
     */
    public boolean getExileFromGraveCost() {
        return exileFromGraveCost;
    }

    private String exileFromGraveType = "";    // <type> or CARDNAME

    /**
     * <p>Getter for the field <code>exileFromGraveType</code>.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String getExileFromGraveType() {
        return exileFromGraveType;
    }

    private boolean exileFromGraveThis = false;

    /**
     * <p>Getter for the field <code>exileFromGraveThis</code>.</p>
     *
     * @return a boolean.
     */
    public boolean getExileFromGraveThis() {
        return exileFromGraveThis;
    }

    private int exileFromGraveAmount = 0;

    /**
     * <p>Getter for the field <code>exileFromGraveAmount</code>.</p>
     *
     * @return a int.
     */
    public int getExileFromGraveAmount() {
        return exileFromGraveAmount;
    }

    private boolean exileFromTopCost = false;

    /**
     * <p>Getter for the field <code>exileFromTopCost</code>.</p>
     *
     * @return a boolean.
     */
    public boolean getExileFromTopCost() {
        return exileFromTopCost;
    }

    private String exileFromTopType = "";    // <type> or CARDNAME

    /**
     * <p>Getter for the field <code>exileFromTopType</code>.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String getExileFromTopType() {
        return exileFromTopType;
    }

    private boolean exileFromTopThis = false;

    /**
     * <p>Getter for the field <code>exileFromTopThis</code>.</p>
     *
     * @return a boolean.
     */
    public boolean getExileFromTopThis() {
        return exileFromTopThis;
    }

    private int exileFromTopAmount = 0;

    /**
     * <p>Getter for the field <code>exileFromTopAmount</code>.</p>
     *
     * @return a int.
     */
    public int getExileFromTopAmount() {
        return exileFromTopAmount;
    }

    private boolean tapCost = false;

    /**
     * <p>getTap.</p>
     *
     * @return a boolean.
     */
    public boolean getTap() {
        return tapCost;
    }

    // future expansion of Ability_Cost class: tap untapped type
    private boolean tapXTypeCost = false;

    /**
     * <p>Getter for the field <code>tapXTypeCost</code>.</p>
     *
     * @return a boolean.
     */
    public boolean getTapXTypeCost() {
        return tapXTypeCost;
    }

    private int tapXTypeAmount = 0;

    /**
     * <p>Getter for the field <code>tapXTypeAmount</code>.</p>
     *
     * @return a int.
     */
    public int getTapXTypeAmount() {
        return tapXTypeAmount;
    }

    private String tapXType = "";

    /**
     * <p>Getter for the field <code>tapXType</code>.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String getTapXType() {
        return tapXType;
    }

    private boolean untapCost = false;

    /**
     * <p>getUntap.</p>
     *
     * @return a boolean.
     */
    public boolean getUntap() {
        return untapCost;
    }

    private boolean subtractCounterCost = false;

    /**
     * <p>getSubCounter.</p>
     *
     * @return a boolean.
     */
    public boolean getSubCounter() {
        return subtractCounterCost;
    }

    private boolean addCounterCost = false;

    /**
     * <p>getAddCounter.</p>
     *
     * @return a boolean.
     */
    public boolean getAddCounter() {
        return addCounterCost;
    }

    private int counterAmount = 0;

    /**
     * <p>getCounterNum.</p>
     *
     * @return a int.
     */
    public int getCounterNum() {
        return counterAmount;
    }

    private Counters counterType;

    /**
     * <p>Getter for the field <code>counterType</code>.</p>
     *
     * @return a {@link forge.Counters} object.
     */
    public Counters getCounterType() {
        return counterType;
    }

    private boolean lifeCost = false;

    /**
     * <p>Getter for the field <code>lifeCost</code>.</p>
     *
     * @return a boolean.
     */
    public boolean getLifeCost() {
        return lifeCost;
    }

    private int lifeAmount = 0;

    /**
     * <p>Getter for the field <code>lifeAmount</code>.</p>
     *
     * @return a int.
     */
    public int getLifeAmount() {
        return lifeAmount;
    }

    private boolean discardCost = false;

    /**
     * <p>Getter for the field <code>discardCost</code>.</p>
     *
     * @return a boolean.
     */
    public boolean getDiscardCost() {
        return discardCost;
    }

    private int discardAmount = 0;

    /**
     * <p>Getter for the field <code>discardAmount</code>.</p>
     *
     * @return a int.
     */
    public int getDiscardAmount() {
        return discardAmount;
    }

    private String discardType = "";

    /**
     * <p>Getter for the field <code>discardType</code>.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String getDiscardType() {
        return discardType;
    }

    private boolean discardThis = false;

    /**
     * <p>Getter for the field <code>discardThis</code>.</p>
     *
     * @return a boolean.
     */
    public boolean getDiscardThis() {
        return discardThis;
    }

    private boolean returnCost = false;    // Return something to owner's hand

    /**
     * <p>Getter for the field <code>returnCost</code>.</p>
     *
     * @return a boolean.
     */
    public boolean getReturnCost() {
        return returnCost;
    }

    private String returnType = "";    // <type> or CARDNAME

    /**
     * <p>Getter for the field <code>returnType</code>.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String getReturnType() {
        return returnType;
    }

    private boolean returnThis = false;

    /**
     * <p>Getter for the field <code>returnThis</code>.</p>
     *
     * @return a boolean.
     */
    public boolean getReturnThis() {
        return returnThis;
    }

    private int returnAmount = 0;

    /**
     * <p>Getter for the field <code>returnAmount</code>.</p>
     *
     * @return a int.
     */
    public int getReturnAmount() {
        return returnAmount;
    }

    /**
     * <p>hasNoManaCost.</p>
     *
     * @return a boolean.
     */
    public boolean hasNoManaCost() {
        return manaCost.equals("") || manaCost.equals("0");
    }

    private String manaCost = "";

    /**
     * <p>getMana.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String getMana() {
        return manaCost;
    }    // Only used for Human to pay for non-X cost first

    /**
     * <p>setMana.</p>
     *
     * @param sCost a {@link java.lang.String} object.
     */
    public void setMana(String sCost) {
        manaCost = sCost;
    }

    /**
     * <p>hasNoXManaCost.</p>
     *
     * @return a boolean.
     */
    public boolean hasNoXManaCost() {
        return manaXCost == 0;
    }

    private int manaXCost = 0;

    /**
     * <p>getXMana.</p>
     *
     * @return a int.
     */
    public int getXMana() {
        return manaXCost;
    }

    /**
     * <p>setXMana.</p>
     *
     * @param xCost a int.
     */
    public void setXMana(int xCost) {
        manaXCost = xCost;
    }

    /**
     * <p>isOnlyManaCost.</p>
     *
     * @return a boolean.
     */
    public boolean isOnlyManaCost() {
        return !sacCost && !exileCost && !exileFromHandCost && !exileFromGraveCost && !exileFromTopCost && !tapCost &&
                !tapXTypeCost && !untapCost && !subtractCounterCost && !addCounterCost && !lifeCost && !discardCost && !returnCost;
    }

    /**
     * <p>getTotalMana.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String getTotalMana() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < manaXCost; i++)
            sb.append("X ");

        if (!hasNoManaCost())
            sb.append(manaCost);

        if (sb.toString().equals(""))
            return "0";

        return sb.toString().trim();
    }


    private String name;

    /**
     * <p>Constructor for Cost.</p>
     *
     * @param parse a {@link java.lang.String} object.
     * @param cardName a {@link java.lang.String} object.
     * @param bAbility a boolean.
     */
    public Cost(String parse, String cardName, boolean bAbility) {
        isAbility = bAbility;
        // when adding new costs for cost string, place them here
        name = cardName;

        String tapXStr = "tapXType<";
        if (parse.contains(tapXStr)) {
            tapXTypeCost = true;
            String[] splitStr = abCostParse(parse, tapXStr, 2);
            parse = abUpdateParse(parse, tapXStr);

            tapXTypeAmount = Integer.parseInt(splitStr[0]);
            tapXType = splitStr[1];
        }

        String subStr = "SubCounter<";
        if (parse.contains(subStr)) {
            // SubCounter<NumCounters/CounterType>
            subtractCounterCost = true;
            String[] splitStr = abCostParse(parse, subStr, 2);
            parse = abUpdateParse(parse, subStr);

            counterAmount = Integer.parseInt(splitStr[0]);
            counterType = Counters.valueOf(splitStr[1]);
        }

        String addStr = "AddCounter<";
        if (parse.contains(addStr)) {
            // AddCounter<NumCounters/CounterType>
            addCounterCost = true;
            String[] splitStr = abCostParse(parse, addStr, 2);
            parse = abUpdateParse(parse, addStr);

            counterAmount = Integer.parseInt(splitStr[0]);
            counterType = Counters.valueOf(splitStr[1]);
        }

        String lifeStr = "PayLife<";
        if (parse.contains(lifeStr)) {
            // PayLife<LifeCost>
            lifeCost = true;
            String[] splitStr = abCostParse(parse, lifeStr, 1);
            parse = abUpdateParse(parse, lifeStr);

            lifeAmount = Integer.parseInt(splitStr[0]);
        }

        String discStr = "Discard<";
        if (parse.contains(discStr)) {
            // Discard<NumCards/DiscardType>
            discardCost = true;
            String[] splitStr = abCostParse(parse, discStr, 2);
            parse = abUpdateParse(parse, discStr);

            discardAmount = Integer.parseInt(splitStr[0]);
            discardType = splitStr[1];
            discardThis = (discardType.equals("CARDNAME"));
        }

        String sacStr = "Sac<";
        if (parse.contains(sacStr)) {
            // TODO: maybe separate SacThis from SacType? not sure if any card would use both
            sacCost = true;
            String[] splitStr = abCostParse(parse, sacStr, 2);
            parse = abUpdateParse(parse, sacStr);

            if (splitStr[0].equals("X")) sacX = true;
            else if (splitStr[0].equals("All")) sacAll = true;
            else sacAmount = Integer.parseInt(splitStr[0]);
            sacType = splitStr[1];
            sacThis = (sacType.equals("CARDNAME"));
        }

        String exileStr = "Exile<";
        if (parse.contains(exileStr)) {
            exileCost = true;
            String[] splitStr = abCostParse(parse, exileStr, 2);
            parse = abUpdateParse(parse, exileStr);

            exileAmount = Integer.parseInt(splitStr[0]);
            exileType = splitStr[1];
            exileThis = (exileType.equals("CARDNAME"));
        }

        String exileFromHandStr = "ExileFromHand<";
        if (parse.contains(exileFromHandStr)) {
            exileFromHandCost = true;
            String[] splitStr = abCostParse(parse, exileFromHandStr, 2);
            parse = abUpdateParse(parse, exileFromHandStr);

            exileFromHandAmount = Integer.parseInt(splitStr[0]);
            exileFromHandType = splitStr[1];
            exileFromHandThis = (exileFromHandType.equals("CARDNAME"));
        }

        String exileFromGraveStr = "ExileFromGrave<";
        if (parse.contains(exileFromGraveStr)) {
            exileFromGraveCost = true;
            String[] splitStr = abCostParse(parse, exileFromGraveStr, 2);
            parse = abUpdateParse(parse, exileFromGraveStr);

            exileFromGraveAmount = Integer.parseInt(splitStr[0]);
            exileFromGraveType = splitStr[1];
            exileFromGraveThis = (exileFromGraveType.equals("CARDNAME"));
        }

        String exileFromTopStr = "ExileFromTop<";
        if (parse.contains(exileFromTopStr)) {
            exileFromTopCost = true;
            String[] splitStr = abCostParse(parse, exileFromTopStr, 2);
            parse = abUpdateParse(parse, exileFromTopStr);

            exileFromTopAmount = Integer.parseInt(splitStr[0]);
            exileFromTopType = splitStr[1];
            exileFromTopThis = false;
        }

        String returnStr = "Return<";
        if (parse.contains(returnStr)) {
            returnCost = true;
            String[] splitStr = abCostParse(parse, returnStr, 2);
            parse = abUpdateParse(parse, returnStr);

            returnAmount = Integer.parseInt(splitStr[0]);
            returnType = splitStr[1];
            returnThis = (returnType.equals("CARDNAME"));
        }

        if (parse.contains("Untap")) {
            untapCost = true;
            parse = parse.replace("Untap", "").trim();
        }

        if (parse.contains("Q")) {
            untapCost = true;
            parse = parse.replace("Q", "").trim();
        }

        if (parse.contains("T")) {
            tapCost = true;
            parse = parse.replace("T", "");
            parse = parse.trim();
        }

        String stripXCost = parse.replaceAll("X", "");

        manaXCost = parse.length() - stripXCost.length();

        manaCost = stripXCost.trim();
        if (manaCost.equals(""))
            manaCost = "0";
    }

    /**
     * <p>abCostParse.</p>
     *
     * @param parse a {@link java.lang.String} object.
     * @param subkey a {@link java.lang.String} object.
     * @param numParse a int.
     * @return an array of {@link java.lang.String} objects.
     */
    String[] abCostParse(String parse, String subkey, int numParse) {
        int startPos = parse.indexOf(subkey);
        int endPos = parse.indexOf(">", startPos);
        String str = parse.substring(startPos, endPos);

        str = str.replace(subkey, "");

        String[] splitStr = str.split("/", numParse);
        return splitStr;
    }

    /**
     * <p>abUpdateParse.</p>
     *
     * @param parse a {@link java.lang.String} object.
     * @param subkey a {@link java.lang.String} object.
     * @return a {@link java.lang.String} object.
     */
    String abUpdateParse(String parse, String subkey) {
        int startPos = parse.indexOf(subkey);
        int endPos = parse.indexOf(">", startPos);
        String str = parse.substring(startPos, endPos + 1);
        return parse.replace(str, "").trim();
    }

    /**
     * <p>changeCost.</p>
     *
     * @param sa a {@link forge.card.spellability.SpellAbility} object.
     */
    public void changeCost(SpellAbility sa) {
        if (getTotalMana() != "0") {    // 11/15/10 use getTotalMana() to account for X reduction
            String mana = getTotalMana();
            manaCost = AllZone.getGameAction().getSpellCostChange(sa, new ManaCost(mana)).toString();
        }
    }

    /**
     * <p>refundPaidCost.</p>
     *
     * @param source a {@link forge.Card} object.
     */
    public void refundPaidCost(Card source) {
        // prereq: isUndoable is called first
        if (tapCost)
            source.untap();
        else if (untapCost)
            source.tap();

        if (subtractCounterCost)
            source.addCounterFromNonEffect(counterType, counterAmount);
        else if (addCounterCost)
            source.subtractCounter(counterType, counterAmount);

        // refund chained mana abilities?
    }

    /**
     * <p>isUndoable.</p>
     *
     * @return a boolean.
     */
    public boolean isUndoable() {
        return !(sacCost || exileCost || exileFromHandCost || exileFromGraveCost || tapXTypeCost || discardCost ||
                returnCost || lifeCost || exileFromTopCost) && hasNoXManaCost() && hasNoManaCost();
    }


    /**
     * <p>isReusuableResource.</p>
     *
     * @return a boolean.
     */
    public boolean isReusuableResource() {
        return !(sacCost || exileCost || exileFromHandCost || tapXTypeCost || discardCost ||
                returnCost || lifeCost) && isAbility;
        // TODO: add/sub counter? Maybe check if it's we're adding a positive counter, or removing a negative counter
    }

    /**
     * <p>toString.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String toString() {
        if (isAbility)
            return abilityToString();
        else
            return spellToString(true);
    }

    // maybe add a conversion method that turns the amounts into words 1=a(n), 2=two etc.

    /**
     * <p>toStringAlt.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String toStringAlt() {
        return spellToString(false);
    }

    /**
     * <p>spellToString.</p>
     *
     * @param bFlag a boolean.
     * @return a {@link java.lang.String} object.
     */
    private String spellToString(boolean bFlag) {
        StringBuilder cost = new StringBuilder();

        if (bFlag)
            cost.append("As an additional cost to cast ").append(name).append(", ");

        boolean first = true;

        if (!bFlag) {
            // usually no additional mana cost for spells
            // only three Alliances cards have additional mana costs, but they are basically kicker/multikicker
            if (!getTotalMana().equals("0")) {
                cost.append("pay ").append(getTotalMana());
                first = false;
            }
        }

        if (tapCost || untapCost) {
            // tap cost for spells will not be in this form.
        }

        if (subtractCounterCost || addCounterCost) {
            // add counterCost only appears in this form, which is currently on supported:
            // put a -1/-1 counter on a creature you control.

            // subtractCounter for spells will not be in this form

        }

        if (lifeCost) {
            if (first)
                cost.append("pay ");
            else
                cost.append("and pay ");
            cost.append(lifeAmount);
            cost.append(" Life");

            first = false;
        }

        if (discardCost) {
            cost.append(discardString(first));
            first = false;
        }

        if (sacCost) {
            cost.append(sacString(first));
            first = false;
        }

        if (exileCost) {
            cost.append(exileString(first));
            first = false;
        }

        if (exileFromHandCost) {
            cost.append(exileFromHandString(first));
            first = false;
        }

        if (exileFromGraveCost) {
            cost.append(exileFromGraveString(first));
            first = false;
        }

        if (exileFromTopCost) {
            cost.append(exileFromTopString(first));
            first = false;
        }

        if (returnCost) {
            cost.append(returnString(first));
            first = false;
        }

        if (first)
            return "";

        if (bFlag)
            cost.append(".").append("\n");

        return cost.toString();
    }

    /**
     * <p>abilityToString.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    private String abilityToString() {
        StringBuilder cost = new StringBuilder();
        boolean first = true;
        if (manaXCost > 0) {
            for (int i = 0; i < manaXCost; i++) {
                cost.append("X").append(" ");
            }
            first = false;
        }

        if (!(manaCost.equals("0") || manaCost.equals(""))) {
            cost.append(manaCost);
            first = false;
        }

        if (tapCost) {
            if (first)
                cost.append("Tap");
            else
                cost.append(", tap");
            first = false;
        }

        if (untapCost) {
            if (first)
                cost.append("Untap ");
            else
                cost.append(", untap");
            first = false;
        }

        if (tapXTypeCost) {
            if (first)
                cost.append("Tap ");
            else
                cost.append(", tap ");
            cost.append(convertIntAndTypeToWords(tapXTypeAmount, "untapped " + tapXType));
            cost.append(" you control");
//			cost.append(tapXType);	// needs IsValid String converter
            first = false;
        }

        if (subtractCounterCost) {
            if (counterType.getName().equals("Loyalty"))
                cost.append("-").append(counterAmount);
            else {
                if (first)
                    cost.append("Remove ");
                else
                    cost.append(", remove ");

                cost.append(convertIntAndTypeToWords(counterAmount, counterType.getName() + " counter"));

                cost.append(" from ");
                cost.append(name);
            }

            first = false;
        }

        if (addCounterCost) {
            if (counterType.getName().equals("Loyalty"))
                cost.append("+").append(counterAmount);
            else {
                if (first)
                    cost.append("Put ");
                else
                    cost.append(", put ");

                cost.append(convertIntAndTypeToWords(counterAmount, counterType.getName() + " counter"));

                cost.append(" on ");
                cost.append(name);
            }
            first = false;
        }

        if (lifeCost) {
            if (first)
                cost.append("Pay ");
            else
                cost.append(", Pay ");
            cost.append(lifeAmount);
            cost.append(" Life");

            first = false;
        }

        if (discardCost) {
            cost.append(discardString(first));
            first = false;
        }

        if (sacCost) {
            cost.append(sacString(first));
            first = false;
        }

        if (exileCost) {
            cost.append(exileString(first));
            first = false;
        }

        if (exileFromHandCost) {
            cost.append(exileFromHandString(first));
            first = false;
        }

        if (exileFromGraveCost) {
            cost.append(exileFromGraveString(first));
            first = false;
        }

        if (exileFromTopCost) {
            cost.append(exileFromTopString(first));
            first = false;
        }

        if (returnCost) {
            cost.append(returnString(first));
            first = false;
        }

        if (first)    // No costs, append 0
            cost.append("0");

        cost.append(": ");
        return cost.toString();
    }

    /**
     * <p>discardString.</p>
     *
     * @param first a boolean.
     * @return a {@link java.lang.String} object.
     */
    public String discardString(boolean first) {
        StringBuilder cost = new StringBuilder();
        if (first) {
            if (isAbility)
                cost.append("Discard ");
            else
                cost.append("discard ");
        } else {
            if (isAbility)
                cost.append(", discard ");
            else
                cost.append("and discard ");
        }

        if (discardThis) {
            cost.append(name);
        } else if (discardType.equals("Hand")) {
            cost.append("your hand");
        } else if (discardType.equals("LastDrawn")) {
            cost.append("last drawn card");
        } else {
            if (!discardType.equals("Any") && !discardType.equals("Card") && !discardType.equals("Random")) {
                cost.append(convertIntAndTypeToWords(discardAmount, discardType + " card"));
            } else
                cost.append(convertIntAndTypeToWords(discardAmount, "card"));

            if (discardType.equals("Random"))
                cost.append(" at random");
        }
        return cost.toString();
    }

    /**
     * <p>sacString.</p>
     *
     * @param first a boolean.
     * @return a {@link java.lang.String} object.
     */
    public String sacString(boolean first) {
        StringBuilder cost = new StringBuilder();
        if (first) {
            if (isAbility)
                cost.append("Sacrifice ");
            else
                cost.append("sacrifice ");
        } else {
            cost.append(", sacrifice ");
        }

        if (sacType.equals("CARDNAME"))
            cost.append(name);
        else
            cost.append(convertIntAndTypeToWords(sacAmount, sacType));

        return cost.toString();
    }

    /**
     * <p>exileString.</p>
     *
     * @param first a boolean.
     * @return a {@link java.lang.String} object.
     */
    public String exileString(boolean first) {
        StringBuilder cost = new StringBuilder();
        if (first) {
            if (isAbility)
                cost.append("Exile ");
            else
                cost.append("exile ");
        } else {
            cost.append(", exile ");
        }

        if (exileType.equals("CARDNAME"))
            cost.append(name);
        else
            cost.append(convertIntAndTypeToWords(exileAmount, exileType));

        return cost.toString();
    }

    /**
     * <p>exileFromHandString.</p>
     *
     * @param first a boolean.
     * @return a {@link java.lang.String} object.
     */
    public String exileFromHandString(boolean first) {
        StringBuilder cost = new StringBuilder();
        if (first) {
            if (isAbility)
                cost.append("Exile ");
            else
                cost.append("exile ");
        } else {
            cost.append(", exile ");
        }

        if (exileType.equals("CARDNAME"))
            cost.append(name);
        else {
            cost.append(convertIntAndTypeToWords(exileFromHandAmount, exileFromHandType));
            cost.append(" from your hand");
        }
        return cost.toString();
    }

    /**
     * <p>exileFromGraveString.</p>
     *
     * @param first a boolean.
     * @return a {@link java.lang.String} object.
     */
    public String exileFromGraveString(boolean first) {
        StringBuilder cost = new StringBuilder();
        if (first) {
            if (isAbility)
                cost.append("Exile ");
            else
                cost.append("exile ");
        } else {
            cost.append(", exile ");
        }

        if (exileType.equals("CARDNAME"))
            cost.append(name);
        else {
            cost.append(convertIntAndTypeToWords(exileFromGraveAmount, exileFromGraveType));
            cost.append(" from your graveyard");
        }
        return cost.toString();
    }

    /**
     * <p>exileFromTopString.</p>
     *
     * @param first a boolean.
     * @return a {@link java.lang.String} object.
     */
    public String exileFromTopString(boolean first) {
        StringBuilder cost = new StringBuilder();
        if (first) {
            if (isAbility)
                cost.append("Exile ");
            else
                cost.append("exile ");
        } else {
            cost.append(", Exile ");
        }

        if (exileType.equals("CARDNAME"))
            cost.append(name).append(" from the top of you library");
        else {
            cost.append("the top ");
            cost.append(convertIntAndTypeToWords(exileFromTopAmount, exileFromTopType));
            cost.append(" of your library");
        }
        return cost.toString();
    }

    /**
     * <p>returnString.</p>
     *
     * @param first a boolean.
     * @return a {@link java.lang.String} object.
     */
    public String returnString(boolean first) {
        StringBuilder cost = new StringBuilder();
        if (first) {
            if (isAbility)
                cost.append("Return ");
            else
                cost.append("return ");
        } else {
            cost.append(", return ");
        }
        String pronoun = "its";
        if (returnType.equals("CARDNAME"))
            cost.append(name);
        else {
            cost.append(convertIntAndTypeToWords(returnAmount, returnType));

            if (returnAmount > 1) {
                pronoun = "their";
            }
            cost.append(" you control");
        }
        cost.append(" to ").append(pronoun).append(" owner's hand");
        return cost.toString();
    }

    // TODO: If an Ability_Cost needs to pay more than 10 of something, fill this array as appropriate
    /** Constant <code>numNames="{zero, a, two, three, four, five, six, "{trunked}</code> */
    private static final String[] numNames = {"zero", "a", "two", "three", "four", "five", "six", "seven", "eight", "nine", "ten"};
    /** Constant <code>vowelPattern</code> */
    private static final Pattern vowelPattern = Pattern.compile("^[aeiou]", Pattern.CASE_INSENSITIVE);


    /**
     * <p>convertIntAndTypeToWords.</p>
     *
     * @param i a int.
     * @param type a {@link java.lang.String} object.
     * @return a {@link java.lang.String} object.
     */
    private String convertIntAndTypeToWords(int i, String type) {
        StringBuilder sb = new StringBuilder();

        if (i >= numNames.length) {
            sb.append(i);
        } else if (1 == i && vowelPattern.matcher(type).find())
            sb.append("an");
        else
            sb.append(numNames[i]);

        sb.append(" ");
        sb.append(type);
        if (1 != i)
            sb.append("s");

        return sb.toString();
    }
}
