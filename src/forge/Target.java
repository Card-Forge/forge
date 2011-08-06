package forge;

public class Target {
	private boolean tgtPlayer = false;
	public boolean canTgtPlayer() { return tgtPlayer; }
	private boolean tgtCreature = false;
	public boolean canTgtCreature() { return tgtCreature; }
	
	public boolean canTgtCreaturePlayer() { return tgtCreature && tgtPlayer; }
	public boolean doesTarget() { return tgtCreature || tgtPlayer || tgtValid; }
	
	private boolean tgtValid = false;
	private String ValidTgts[];
	private String vtSelection = "";
	public boolean canTgtValid() { return tgtValid; }
	public String[] getValidTgts() { return ValidTgts; }
	public void setValidTgts(String vTgts[]) { ValidTgts = vTgts; }
	public String getVTSelection() { return vtSelection; }
	public void setVTSelection(String vtSelStr) { vtSelection = vtSelStr; }
	
	private String tgtZone = Constant.Zone.Play;
	public void setZone(String tZone) { tgtZone = tZone; }
	public String getZone() { return tgtZone; }
	
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
			if (tgtSplit[0].contains("V")) // valid
				tgtValid = true;
			
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
	
	public Target(String parse, String select, String[] valid){
		if (parse.contains("Tgt")){
			// Tgt{C}{P}{V}[/<MinTargets>/<MaxTargets>] min-max is optional 
			String tgtStr = parse.replace("Tgt", "");
			String[] tgtSplit = tgtStr.split("/");
			
			if (tgtSplit[0].contains("C"))	// creature
				tgtCreature = true;
			if (tgtSplit[0].contains("P"))	// player
				tgtPlayer = true;	
			if (tgtSplit[0].contains("V")) // valid
				tgtValid = true;
			
			if (tgtSplit.length != 3){
				minTargets = 1;
				maxTargets = 1;
			}
			else{
				minTargets = Integer.parseInt(tgtSplit[1]);
				maxTargets = Integer.parseInt(tgtSplit[2]);
			}
			
			if (tgtValid){
				vtSelection = select;
				ValidTgts = valid;
			}
		}
	}
	
	public Target(String parse, int min, int max){
		minTargets = min;
		maxTargets = max;
		
		tgtValid = true;
		ValidTgts = parse.split(",");
	}
	
	public String targetString()
	{
		StringBuilder sb = new StringBuilder("target ");

		if (tgtCreature)
			sb.append("creature");
		if (tgtPlayer){
			if (tgtCreature)
				sb.append(" or ");
			sb.append("player");
		}
		if (tgtValid){
			sb.append(vtSelection);
		}
		sb.append(".");
		
		return sb.toString();
	}
}
