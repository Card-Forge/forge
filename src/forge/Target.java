package forge;

public class Target {	
	private boolean tgtValid = false;
	private String ValidTgts[];
	private String vtSelection = "";
	
	public boolean doesTarget() { return tgtValid; }
	public String[] getValidTgts() { return ValidTgts; }
	public String getVTSelection() { return vtSelection; }
	
	private int minTargets = 1;
	public int getMinTargets() { return minTargets; }
	private int maxTargets = 1;
	public int getMaxTargets() { return maxTargets; }
	
	private String tgtZone = Constant.Zone.Play;
	public void setZone(String tZone) { tgtZone = tZone; }
	public String getZone() { return tgtZone; }
	
	// add array of targets here?

	private int numTargeted = 0;
	public int getNumTargeted() { return numTargeted; }
	public void incrementTargets() { numTargeted++; }
	public void resetTargets() { numTargeted = 0; }
	
	public Target(String parse){
		this(parse, 1, 1);
	}
	
	public Target(String parse, int min, int max){
		// parse=Tgt{C}{P} - Primarily used for Pump or Damage 
		// C = Creature   P=Player/Planeswalker
		// CP = All three
		
		tgtValid = true;

		if (parse.contains("Tgt")){
			parse = parse.replace("Tgt", "");
		}

		String valid;
		String prompt;
		
		if (parse.equals("CP")){
			valid = "Creature,Planeswalker,Player";
			prompt = "Select target creature, planeswalker, or player";
		}
		else if (parse.equals("C")){
			valid = "Creature";
			prompt = "Select target creature";
		}
		else if (parse.equals("P")){
			valid = "Planeswalker,Player";
			prompt = "Select target planeswalker or player";
		}
		else{
			System.out.println("Bad Parsing in Target(parse, min, max)");
			return;
		}
		
		vtSelection = prompt;
		ValidTgts = valid.split(",");
		
		minTargets = min;
		maxTargets = max;
	}
	
	public Target(String select, String[] valid){		
		this(select, valid, 1, 1);
	}
	
	public Target(String select, String[] valid, int min, int max){
		tgtValid = true;
		vtSelection = select;
		ValidTgts = valid;
		
		minTargets = min;
		maxTargets = max;
	}
	
	// These below functions are quite limited to the damage classes, we should find a way to move them into AF_DealDamage
	public boolean canTgtPlayer() {
		for(String s: ValidTgts){
			if (s.equals("Player"))
				return true;
		}
		return false; 
	}
	
	public boolean canTgtCreature() { 
		for(String s: ValidTgts){
			if (s.contains("Creature") && !s.contains("nonCreature"))
				return true;
		}
		return false; 
	}
	
	public boolean canTgtCreatureAndPlayer() { return canTgtPlayer() && canTgtCreature(); }
}
