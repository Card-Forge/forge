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
	
	// future expansion of Ability_Cost class: untap, and lifeCost
	// private boolean untapCost = false;	
	
	private boolean lifeCost = false;
	private int lifeAmount = 0;
	
	public boolean hasNoManaCost() { return manaCost.equals("") || manaCost.equals("0"); };
	private String manaCost = "";
	public String getMana() { return manaCost; }
	
	private String name;
	
	public Ability_Cost(String parse, String cardName)
	{
		name = cardName;
        if(parse.contains("Sac-")) {
        	sacCost = true;
        	int sacPos = parse.indexOf("Sac-");
        	sacType = parse.substring(sacPos).replace("Sac-", "").trim();
        	sacThis =  (sacType.equals("CARDNAME"));
        	parse = parse.substring(0,sacPos-1).trim();
        }                

        if(parse.contains("T")) {
            tapCost = true;
            parse = parse.replace("T", "");
            parse = parse.trim();
        }
        manaCost = parse.trim();
        if (manaCost == "")
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
		}
		
		if (lifeCost){
			if (caps)
				cost.append("Pay ");
			else
				cost.append(", Pay");
			cost.append(lifeAmount);
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
