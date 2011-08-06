package forge;

public class Target {
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
	
	public Target(String parse){
		if (parse.contains("Tgt")){
			// Tgt{C}{P}[/<MinTargets>/<MaxTargets>] min-max is optional 
			String tgtStr = parse.replace("Tgt", "");
			String[] tgtSplit = tgtStr.split("/");
			
			if (tgtSplit[0].contains("C"))	// creature
				tgtCreature = true;
			if (tgtSplit[0].contains("P"))	// player
				tgtPlayer = true;
			//todo add Opponent and other permanent types
			
			if (tgtSplit.length != 3){
				minTargets = 1;
				maxTargets = 1;
			}
			else{
				minTargets = Integer.parseInt(tgtSplit[1]);
				maxTargets = Integer.parseInt(tgtSplit[2]);
			}
		}
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
