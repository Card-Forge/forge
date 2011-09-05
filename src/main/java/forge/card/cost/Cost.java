package forge.card.cost;

import java.util.ArrayList;
import java.util.regex.Pattern;

import forge.AllZone;
import forge.Card;
import forge.Constant;
import forge.Counters;
import forge.card.mana.ManaCost;
import forge.card.spellability.SpellAbility;

/**
 * <p>Cost class.</p>
 *
 * @author Forge
 * @version $Id$
 */
public class Cost {
    private boolean isAbility = true;
    ArrayList<CostPart> costParts = new ArrayList<CostPart>();
    
    public ArrayList<CostPart> getCostParts(){ return costParts; }
    
    private boolean sacCost = false;

    /**
     * <p>Getter for the field <code>sacCost</code>.</p>
     *
     * @return a boolean.
     */
    public boolean getSacCost() {
        return sacCost;
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

    /**
     * <p>hasNoManaCost.</p>
     *
     * @return a boolean.
     */
    public boolean hasNoManaCost() {
    	for(CostPart part : costParts)
    		if (part instanceof CostMana)
    			return false;
    			
    	return true;
    }

    /**
     * <p>isOnlyManaCost.</p>
     *
     * @return a boolean.
     */
    public boolean isOnlyManaCost() {
        // Only used by Morph and Equip... why do we need this?
    	for(CostPart part : costParts)
    		if (!(part instanceof CostMana))
    			return false;
    			
    	return true;
    }

    /**
     * <p>getTotalMana.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String getTotalMana() {
    	for(CostPart part : costParts)
    		if (part instanceof CostMana)
    			return part.toString();

        return "0";
    }

    private String name;

    // Parsing Strings
    private final static String tapXStr = "tapXType<";
    private final static String subStr = "SubCounter<";
    private final static String addStr = "AddCounter<";
    private final static String lifeStr = "PayLife<";
    private final static String discStr = "Discard<";
    private final static String sacStr = "Sac<";
    private final static String exileStr = "Exile<";
    private final static String exileFromHandStr = "ExileFromHand<";
    private final static String exileFromGraveStr = "ExileFromGrave<";
    private final static String exileFromTopStr = "ExileFromTop<";
    private final static String returnStr = "Return<";
    private final static String revealStr = "Reveal<";
    
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
 
        while (parse.contains(tapXStr)) {
            String[] splitStr = abCostParse(parse, tapXStr, 3);
            parse = abUpdateParse(parse, tapXStr);
            
            String description = splitStr.length > 2 ? splitStr[2] : null;
            costParts.add(new CostTapType(splitStr[0], splitStr[1], description));
        }
        
        while (parse.contains(subStr)) {
            // SubCounter<NumCounters/CounterType>
            String[] splitStr = abCostParse(parse, subStr, 4);
            parse = abUpdateParse(parse, subStr);

            String type = splitStr.length > 2 ? splitStr[2] : "CARDNAME";
            String description = splitStr.length > 3 ? splitStr[3] : null;
            
            costParts.add(new CostRemoveCounter(splitStr[0], Counters.valueOf(splitStr[1]), type, description));
        }

       
        while (parse.contains(addStr)) {
            // AddCounter<NumCounters/CounterType>
            String[] splitStr = abCostParse(parse, addStr, 2);
            parse = abUpdateParse(parse, addStr);

            costParts.add(new CostPutCounter(splitStr[0], Counters.valueOf(splitStr[1])));
        }

        // While no card has "PayLife<2> PayLife<3> there might be a card that Changes Cost by adding a Life Payment
        while (parse.contains(lifeStr)) {
            // PayLife<LifeCost>
            String[] splitStr = abCostParse(parse, lifeStr, 1);
            parse = abUpdateParse(parse, lifeStr);

            costParts.add(new CostPayLife(splitStr[0]));
        }

        
        while (parse.contains(discStr)) {
        	// Discard<NumCards/Type>
            String[] splitStr = abCostParse(parse, discStr, 3);
            parse = abUpdateParse(parse, discStr);

            String description = splitStr.length > 2 ? splitStr[2] : null;
            costParts.add(new CostDiscard(splitStr[0], splitStr[1], description));
        }

        
        while (parse.contains(sacStr)) {
            sacCost = true;
            String[] splitStr = abCostParse(parse, sacStr, 3);
            parse = abUpdateParse(parse, sacStr);

            String description = splitStr.length > 2 ? splitStr[2] : null;
            costParts.add(new CostSacrifice(splitStr[0], splitStr[1], description));
        }

        
        while (parse.contains(exileStr)) {
            String[] splitStr = abCostParse(parse, exileStr, 3);
            parse = abUpdateParse(parse, exileStr);

            String description = splitStr.length > 2 ? splitStr[2] : null;
            costParts.add(new CostExile(splitStr[0], splitStr[1], description, Constant.Zone.Battlefield));
        }

        
        while (parse.contains(exileFromHandStr)) {
            String[] splitStr = abCostParse(parse, exileFromHandStr, 3);
            parse = abUpdateParse(parse, exileFromHandStr);

            String description = splitStr.length > 2 ? splitStr[2] : null;
            costParts.add(new CostExile(splitStr[0], splitStr[1], description, Constant.Zone.Hand));
        }

        
        while (parse.contains(exileFromGraveStr)) {
            String[] splitStr = abCostParse(parse, exileFromGraveStr, 3);
            parse = abUpdateParse(parse, exileFromGraveStr);

            String description = splitStr.length > 2 ? splitStr[2] : null;
            costParts.add(new CostExile(splitStr[0], splitStr[1], description, Constant.Zone.Graveyard));
        }

        
        while (parse.contains(exileFromTopStr)) {
            String[] splitStr = abCostParse(parse, exileFromTopStr, 3);
            parse = abUpdateParse(parse, exileFromTopStr);

            String description = splitStr.length > 2 ? splitStr[2] : null;
            costParts.add(new CostExile(splitStr[0], splitStr[1], description, Constant.Zone.Library));
        }

        
        while (parse.contains(returnStr)) {
            String[] splitStr = abCostParse(parse, returnStr, 3);
            parse = abUpdateParse(parse, returnStr);

            String description = splitStr.length > 2 ? splitStr[2] : null;
            costParts.add(new CostReturn(splitStr[0], splitStr[1], description));
        }
        
        while (parse.contains(revealStr)) {
            String[] splitStr = abCostParse(parse, revealStr, 3);
            parse = abUpdateParse(parse, revealStr);

            String description = splitStr.length > 2 ? splitStr[2] : null;
            costParts.add(new CostReveal(splitStr[0], splitStr[1], description));
        }

        int manaLocation = 0;
        // These won't show up with multiples
        if (parse.contains("Untap")) {
            parse = parse.replace("Untap", "").trim();
            costParts.add(0, new CostUntap());
            manaLocation++;
        }

        if (parse.contains("Q")) {
            parse = parse.replace("Q", "").trim();
            costParts.add(0, new CostUntap());
            manaLocation++;
        }

        if (parse.contains("T")) {
        	tapCost = true;
            parse = parse.replace("T", "").trim();
            costParts.add(0, new CostTap());
            manaLocation++;
        }

        String stripXCost = parse.replaceAll("X", "");

        int amountX = parse.length() - stripXCost.length();

        String mana = stripXCost.trim();
        if (mana.equals(""))
        	mana = "0";
        
        if (amountX > 0 || !mana.equals("0")){
            costParts.add(manaLocation, new CostMana(mana, amountX));
        }
    }

    /**
     * <p>abCostParse.</p>
     *
     * @param parse a {@link java.lang.String} object.
     * @param subkey a {@link java.lang.String} object.
     * @param numParse a int.
     * @return an array of {@link java.lang.String} objects.
     */
    private String[] abCostParse(String parse, String subkey, int numParse) {
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
    private String abUpdateParse(String parse, String subkey) {
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
    	// TODO: Change where ChangeCost happens
    	for(CostPart part : costParts){
    		if (part instanceof CostMana){
    			CostMana costMana = (CostMana)part;
    			
    			String mana = getTotalMana();
    	        if (mana != "0") {    // 11/15/10 use getTotalMana() to account for X reduction
    	            costMana.setAdjustedMana(AllZone.getGameAction().getSpellCostChange(sa, new ManaCost(mana)).toString());
    	        }
    		}
    	}
    }

    /**
     * <p>refundPaidCost.</p>
     *
     * @param source a {@link forge.Card} object.
     */
    public void refundPaidCost(Card source) {
        // prereq: isUndoable is called first
    	for(CostPart part : costParts)
    		part.refund(source);
    }

    /**
     * <p>isUndoable.</p>
     *
     * @return a boolean.
     */
    public boolean isUndoable() {
    	for(CostPart part : costParts)
    		if (!part.isUndoable())
    			return false;
    			
    	return true;
    }

    /**
     * <p>isReusuableResource.</p>
     *
     * @return a boolean.
     */
    public boolean isReusuableResource() {
    	for(CostPart part : costParts)
    		if (!part.isReusable())
    			return false;
    			
    	return isAbility;
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
        boolean first = true;
        
        if (bFlag)
            cost.append("As an additional cost to cast ").append(name).append(", ");
        else{
            // usually no additional mana cost for spells
            // only three Alliances cards have additional mana costs, but they are basically kicker/multikicker
            /*
            if (!getTotalMana().equals("0")) {
                cost.append("pay ").append(getTotalMana());
                first = false;
            }
            */
        }

        for(CostPart part : costParts){
            if (part instanceof CostMana)
                continue;
        	if (!first)
        		cost.append(" and ");
        	cost.append(part.toString());
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
        
        for(CostPart part : costParts){
            boolean append = true;
            if (!first){
                if (part instanceof CostMana){
                    cost.insert(0, ", ").insert(0, part.toString());
                    append = false;
                }
                else{
                    cost.append(", ");
                }
            }
            if (append){
                cost.append(part.toString());
            }
        	first = false;
        }

        if (first)    // No costs, append 0
            cost.append("0");

        cost.append(": ");
        return cost.toString();
    }

    // TODO: If a Cost needs to pay more than 10 of something, fill this array as appropriate
    /** Constant <code>numNames="{zero, a, two, three, four, five, six, "{trunked}</code> */
    private static final String[] numNames = {"zero", "a", "two", "three", "four", "five", "six", "seven", "eight", "nine", "ten"};
    /** Constant <code>vowelPattern</code> */
    private static final Pattern vowelPattern = Pattern.compile("^[aeiou]", Pattern.CASE_INSENSITIVE);


    public static String convertAmountTypeToWords(Integer i, String amount, String type){
    	if (i == null)
    		return convertAmountTypeToWords(amount, type);
    	
    	return convertIntAndTypeToWords(i.intValue(), type);
    }
    
    /**
     * <p>convertIntAndTypeToWords.</p>
     *
     * @param i a int.
     * @param type a {@link java.lang.String} object.
     * @return a {@link java.lang.String} object.
     */
    public static String convertIntAndTypeToWords(int i, String type) {
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
    
    
    public static String convertAmountTypeToWords(String amount, String type) {
        StringBuilder sb = new StringBuilder();

        sb.append(amount);
        sb.append(" ");
        sb.append(type);


        return sb.toString();
    }
}
