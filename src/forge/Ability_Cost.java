package forge;

public class Ability_Cost {
	private boolean isAbility = true;
	
	private boolean sacCost = false;
	public boolean getSacCost() { return sacCost; }
	private String sacType = "";	// <type> or CARDNAME
	public String getSacType() { return sacType; }
	private boolean sacThis = false;
	public boolean getSacThis() { return sacThis; }
	private int sacAmount = 0;
	public int getSacAmount() { return sacAmount; }
    
	private boolean tapCost = false;
	public boolean getTap() { return tapCost; } 
	
	// future expansion of Ability_Cost class: tap untapped type
	private boolean tapXTypeCost = false;
	public boolean getTapXTypeCost() { return tapXTypeCost;}
	private int tapXTypeAmount = 0;
	public int getTapXTypeAmount() { return tapXTypeAmount; }
	private String tapXType = "";
	public String getTapXType() { return tapXType;}
	
	private boolean untapCost = false;
	public boolean getUntap() { return untapCost; } 
	
	private boolean subtractCounterCost = false;
	public boolean getSubCounter() { return subtractCounterCost; }
	private boolean addCounterCost = false;
	public boolean getAddCounter() { return addCounterCost; } 
	
	private int counterAmount = 0;
	public int getCounterNum() { return counterAmount; }
	private Counters counterType;
	public Counters getCounterType() { return counterType; }
	
	private boolean lifeCost = false;
	public boolean getLifeCost() { return lifeCost; }
	private int lifeAmount = 0;
	public int getLifeAmount() { return lifeAmount; }
	
	private boolean discardCost = false;
	public boolean getDiscardCost() { return discardCost; }
	private int discardAmount = 0;
	public int getDiscardAmount() { return discardAmount; }
	private String discardType = "";
	public String getDiscardType() { return discardType; }
	
	public boolean hasNoManaCost() { return manaCost.equals("") || manaCost.equals("0"); };
	private String manaCost = "";
	public String getMana() { return manaCost; }
	public void setMana(String sCost) { manaCost = sCost; }
	
	private String name;
	
	public Ability_Cost(String parse, String cardName, boolean bAbility)
	{
		isAbility = bAbility;
		// when adding new costs for cost string, place them here
		name = cardName;
		
        String tapXStr = "tapXType<";
        if (parse.contains(tapXStr))
        {
        	tapXTypeCost = true;
        	String[] splitStr = abCostParse(parse, tapXStr, 2);
        	parse = abUpdateParse(parse, tapXStr);
        	
        	tapXTypeAmount = Integer.parseInt(splitStr[0]);
        	tapXType = splitStr[1];
        }
		
		String subStr = "SubCounter<";
        if(parse.contains(subStr)) {
        	// SubCounter<NumCounters/CounterType>
        	subtractCounterCost = true;
        	String[] splitStr = abCostParse(parse, subStr, 2);
        	parse = abUpdateParse(parse, subStr);

        	counterAmount = Integer.parseInt(splitStr[0]);
        	counterType = Counters.valueOf(splitStr[1]);
        }       
		
        String lifeStr = "PayLife<";
        if(parse.contains(lifeStr)) {
        	// PayLife<LifeCost>
        	lifeCost = true;
        	String[] splitStr = abCostParse(parse, lifeStr, 1);
        	parse = abUpdateParse(parse, lifeStr);
        	
        	lifeAmount = Integer.parseInt(splitStr[0]);
        }
        
        String discStr = "Discard<";
        if (parse.contains(discStr)){
        	// Discard<NumCards/DiscardType>
        	discardCost = true;
        	String[] splitStr = abCostParse(parse, discStr, 2);
        	parse = abUpdateParse(parse, discStr);
        	
        	discardAmount = Integer.parseInt(splitStr[0]);
        	discardType = splitStr[1];
        }
        
        String sacStr = "Sac<";
        if(parse.contains(sacStr)) {
        	// todo(sol): add Support for sacrificing more than 1 of a type
        	// todo: maybe separate SacThis from SacType? not sure if any card would use both
        	sacCost = true;
        	String[] splitStr = abCostParse(parse, sacStr, 2);
        	parse = abUpdateParse(parse, sacStr);
        	
        	sacAmount = Integer.parseInt(splitStr[0]);
        	sacType = splitStr[1];
        	sacThis = (sacType.equals("CARDNAME"));
        }
        
        if (parse.contains("Untap")){
        	untapCost = true;
            parse = parse.replace("Untap", "").trim();
        }
        
        if(parse.contains("T")) {
            tapCost = true;
            parse = parse.replace("T", "");
            parse = parse.trim();
        }
        
        manaCost = parse.trim();
        if (manaCost.equals(""))
        	manaCost = "0";
	}
	
	String[] abCostParse(String parse, String subkey, int numParse){
    	int startPos = parse.indexOf(subkey);
    	int endPos = parse.indexOf(">", startPos);
    	String str = parse.substring(startPos, endPos);
    	
    	str = str.replace(subkey, "");

		String[] splitStr = str.split("/", numParse);
		return splitStr;
	}
	
	String abUpdateParse(String parse, String subkey){
    	int startPos = parse.indexOf(subkey);
    	int endPos = parse.indexOf(">", startPos);
    	String str = parse.substring(startPos, endPos+1);
    	return parse.replace(str, "").trim();
	}
	
	public void changeCost(SpellAbility sa){
		manaCost = AllZone.GameAction.GetSpellCostChange(sa).toString();
	}
	
	public String toString()
	{
		if (isAbility)
			return abilityToString();
		else
			return spellToString();
	}
	
	// maybe add a conversion method that turns the amounts into words 1=a(n), 2=two etc.
	
	private String spellToString() {
		StringBuilder cost = new StringBuilder("As an additional cost to play ");
		cost.append(name);
		cost.append(", ");
		boolean first = true;

		if (!(manaCost.equals("0") || manaCost.equals(""))){
			// never a normal additional mana cost for spells
			cost.append(manaCost);
		}
		
		if (tapCost || untapCost){	
			// tap cost for spells will not be in this form.
		}
		
		if (subtractCounterCost){
			// subtractCounter for spells will not be in this form

		}
		
		if (lifeCost){
			if (first)
				cost.append("pay ");
			else
				cost.append("and pay ");
			cost.append(lifeAmount);
			cost.append(" Life");

			first = false;
		}
		
		if (discardCost){
			if (first)
				cost.append("discard ");
			else
				cost.append("and discard ");
			if (discardType.equals("Hand")){
				cost.append(" your hand");
			}
			else{
				cost.append(discardAmount);
				int type = discardType.indexOf("/");
				if (type != -1)
					cost.append(discardType.substring(type + 1)).append(" ");
				cost.append(" card");
				if (discardAmount > 1)
					cost.append("s");
				if (discardType.equals("Random"))
					cost.append(" at random");
			}

			first = false;
		}
		
		cost.append(sacString(first));
		
		cost.append(".");
		return cost.toString();
	}

	private String abilityToString() {
		StringBuilder cost = new StringBuilder();
		boolean first = true;
		if (!(manaCost.equals("0") || manaCost.equals(""))){
			cost.append(manaCost);
			first = false;
		}
		
		if (tapCost){
			if (first)
				cost.append("Tap");
			else
				cost.append(", tap");
			first = false;
		}
		
		if (untapCost){
			if (first)
				cost.append("Untap ");
			else
				cost.append(", untap ");
			first = false;
		}
		
		if (tapXTypeCost){
			if (first)
				cost.append("Tap ");
			else
				cost.append(", tap ");
			cost.append(tapXTypeAmount);
			cost.append(" untapped ");
			cost.append(tapXType);	// needs IsValid String converter
			if (tapXTypeAmount > 1)
				cost.append("s");
			first = false;
		}
		
		if (subtractCounterCost){
			if (first)
				cost.append("Remove ");
			else
				cost.append(", remove ");
			if (counterAmount != 1)
				cost.append(counterAmount);
			else
				cost.append("a");
			cost.append(" " + counterType.getName());
			cost.append(" counter");
			if (counterAmount != 1)
				cost.append("s");
			cost.append(" from ");
			cost.append(name);

			first = false;
		}
		
		if (lifeCost){
			if (first)
				cost.append("Pay ");
			else
				cost.append(", Pay ");
			cost.append(lifeAmount);
			cost.append(" Life");

			first = false;
		}
		
		if (discardCost){
			if (first)
				cost.append("Discard ");
			else
				cost.append(", discard ");
			if (discardType.equals("Hand")){
				cost.append(" your hand");
			}
			else{
				cost.append(discardAmount);
				if (!discardType.equals("Any") && !discardType.equals("Random")){
					cost.append(" ").append(discardType);
				}
				cost.append(" card");
				if (discardAmount > 1)
					cost.append("s");
				if (discardType.equals("Random"))
					cost.append(" at random");
			}

			first = false;
		}
		
		cost.append(sacString(first));
		
		cost.append(": ");
		return cost.toString();
	}

	public String sacString(boolean first)
	{
		StringBuilder cost = new StringBuilder();
		if (sacCost){
			if (first){
				if (isAbility)
					cost.append("Sacrifice ");
				else
					cost.append("sacrifice ");
			}
			else{
				if (isAbility)
					cost.append(", sacrifice ");
				else
					cost.append(" and sacrifice ");
			}
			
			if (sacType.equals("CARDNAME"))
				cost.append(name);
			else{
				cost.append(sacAmount).append(" ");
				cost.append(sacType);
				if (sacAmount > 1)
					cost.append("s");
			}
		}
		return cost.toString();
	}
}
