package forge;

public class Ability_Cost {
	private boolean sacCost = false;
	public boolean getSacCost() { return sacCost; }
	private String sacType = "";	// <type> or CARDNAME
	public String getSacType() { return sacType; }
	private boolean sacThis = false;
	public boolean getSacThis() { return sacThis; }
    
	private boolean tapCost = false;
	public boolean getTap() { return tapCost; } 
	
	// future expansion of Ability_Cost class: untap
	// private boolean untapCost = false;	
	
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
	
	public boolean hasNoManaCost() { return manaCost.equals("") || manaCost.equals("0"); };
	private String manaCost = "";
	public String getMana() { return manaCost; }
	
	private String name;
	
	public Ability_Cost(String parse, String cardName)
	{
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
        
        if(parse.contains("Sac-")) {
        	sacCost = true;
        	int sacPos = parse.indexOf("Sac-");
        	sacType = parse.substring(sacPos).replace("Sac-", "").trim();
        	sacThis =  (sacType.equals("CARDNAME"));
        	if (sacPos > 0)
        		parse = parse.substring(0,sacPos-1).trim();
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
		StringBuilder cost = new StringBuilder();
		boolean caps = true;
		if (!(manaCost.equals("0") || manaCost.equals(""))){
			cost.append(manaCost);
			caps = false;
		}
		
		if (tapCost){
			if (caps)
				cost.append("Tap");
			else
				cost.append(", tap");
			caps = false;
		}
		
		if (subtractCounterCost){
			if (caps)
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
			cost.append(" from CARDNAME");

			caps = false;
		}
		
		if (lifeCost){
			if (caps)
				cost.append("Pay ");
			else
				cost.append(", Pay ");
			cost.append(lifeAmount);
			cost.append(" Life");

			caps = false;
		}
		
		cost.append(sacString(caps));
		
		cost.append(": ");
		return cost.toString();
	}
	
	public String sacString(boolean caps)
	{
		StringBuilder cost = new StringBuilder();
		if (sacCost){
			if (caps)
				cost.append("Sacrifice ");
			else
				cost.append(", Sacrifice ");
			
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
