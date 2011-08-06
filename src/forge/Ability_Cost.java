package forge;

public class Ability_Cost {
	private boolean isAbility = true;
	
	private boolean sacCost = false;
	public boolean getSacCost() { return sacCost; }
	private String sacType = "";	// <type> or CARDNAME
	public String getSacType() { return sacType; }
	private boolean sacThis = false;
	public boolean getSacThis() { return sacThis; }
    
	private boolean tapCost = false;
	public boolean getTap() { return tapCost; } 
	
	// future expansion of Ability_Cost class: tap untapped type
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
		
        if(parse.contains("SubCounter<")) {
        	// SubCounter<CounterType/NumCounters>
        	subtractCounterCost = true;
        	int counterPos = parse.indexOf("SubCounter<");
        	int endPos = parse.indexOf(">", counterPos);
        	String str = parse.substring(counterPos, endPos+1);
        	parse = parse.replace(str, "").trim();
        	
        	str = str.replace("SubCounter<", "");
        	str = str.replace(">", "");
        	String[] strSplit = str.split("/");
        	// convert strSplit[0] to Counter.something
        	counterType = Counters.valueOf(strSplit[0]);
        	counterAmount = Integer.parseInt(strSplit[1]);
        }       
		
        if(parse.contains("PayLife<")) {
        	// PayLife<LifeCost>
        	lifeCost = true;
        	int lifePos = parse.indexOf("PayLife<");
        	int endPos = parse.indexOf(">", lifePos);
        	String str = parse.substring(lifePos, endPos+1);
        	parse = parse.replace(str, "").trim();
        	
        	str = str.replace("PayLife<", "");
        	str = str.replace(">", "");
        	
        	lifeAmount = Integer.parseInt(str);
        }
        
        if (parse.contains("Discard<")){
        	// Discard<NumCards,DiscardType>
        	discardCost = true;
        	int startPos = parse.indexOf("Discard<");
        	int endPos = parse.indexOf(">", startPos);
        	String str = parse.substring(startPos, endPos+1);
        	parse = parse.replace(str, "").trim();
        	
        	str = str.replace("Discard<", "");
        	str = str.replace(">", "");
        	
        	String[] splitStr = str.split(",");
        	
        	discardAmount = Integer.parseInt(splitStr[0]);
        	discardType = splitStr[1];
        }
        
        if(parse.contains("Sac-")) {
        	// todo(sol): change from Sac- to Sac<X, type>, also use IsValidCard with type
        	sacCost = true;
        	int sacPos = parse.indexOf("Sac-");
        	// sacType needs to be an Array for IsValidCard
        	sacType = parse.substring(sacPos).replace("Sac-", "").trim();
        	sacThis =  (sacType.equals("CARDNAME"));
        	if (sacPos > 0)
        		parse = parse.substring(0,sacPos-1).trim();
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
	
	public String toString()
	{
		if (isAbility)
			return abilityToString();
		else
			return spellToString();
	}
	
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
				cost.append("Untap");
			else
				cost.append(", untap");
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
				String part;
				String firstLetter = sacType.substring(0, 0);
				if (firstLetter.equalsIgnoreCase("a") || firstLetter.equalsIgnoreCase("e") || firstLetter.equalsIgnoreCase("i")
					|| firstLetter.equalsIgnoreCase("o") || firstLetter.equalsIgnoreCase("u"))
					part = "an";
				part = "a";
				cost.append(part + " ");
				cost.append(sacType);
			}
		}
		return cost.toString();
	}
}
