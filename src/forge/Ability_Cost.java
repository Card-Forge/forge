package forge;

public class Ability_Cost {
	private boolean tgtPlayer = false;
	public boolean canTgtPlayer() { return tgtPlayer; }
	private boolean tgtCreature = false;
	public boolean canTgtCreature() { return tgtCreature; }
	
	public boolean canTgtCreaturePlayer() { return tgtCreature && tgtPlayer; }
	public boolean doesTarget() { return tgtCreature || tgtPlayer; }
	
	private int minTargets = 0;
	public int getMinTargets() { return minTargets; }
	private int maxTargets = 0;
	public int getMaxTargets() { return maxTargets; }
	// add array of targets here?
	
	private int numTargeted = 0;
	public int getNumTargeted() { return numTargeted; }
	public void incrementTargets() { numTargeted++; }
	public void resetTargets() { numTargeted = 0; }
	
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
		// when adding new costs for cost string, place them here
		name = cardName;
		
		if (parse.contains("Tgt")){
			// Tgt{C}{P}{/<MinTargets>/<MaxTargets>} 
			int tgtPos = parse.indexOf("Tgt");
			int endTgt = parse.indexOf(" ");
			//System.out.println(cardName);
			String tgtStr = parse.substring(tgtPos, endTgt);
			parse = parse.substring(endTgt+1);
			tgtStr = tgtStr.replace("Tgt", "");
			String[] tgtSplit = tgtStr.split("/");
			
			if (tgtSplit[0].contains("C"))
				tgtCreature = true;
			if (tgtSplit[0].contains("P"))
				tgtPlayer = true;
			//todo(sol) add Opp
			
			if (tgtSplit.length != 3){
				minTargets = 1;
				maxTargets = 1;
			}
			else{
				minTargets = Integer.parseInt(tgtSplit[1]);
				maxTargets = Integer.parseInt(tgtSplit[2]);
			}
		}
		
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
	
	public String targetString()
	{
		String tgt = "";
		if (tgtCreature)
			tgt += "creature";
		if (tgtPlayer && !tgt.equals(""))
			tgt += " or ";
		if (tgtPlayer)
			tgt += "player";
		
		tgt += ".";
		
		return "target " + tgt;
	}
}
