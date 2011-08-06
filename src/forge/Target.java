package forge;

import java.util.ArrayList;

public class Target {	
	private boolean tgtValid = false;
	private String ValidTgts[];
	private String vtSelection = "";
	
	public boolean doesTarget() { return tgtValid; }
	public String[] getValidTgts() { return ValidTgts; }
	public String getVTSelection() { return vtSelection; }
	
	private String minTargets;
	private String maxTargets;
	public int getMinTargets(Card c, SpellAbility sa)  	{ return AbilityFactory.calculateAmount(c, minTargets, sa); } 
	public int getMaxTargets(Card c, SpellAbility sa)  	{ return AbilityFactory.calculateAmount(c, maxTargets, sa); } 
	
	public boolean isMaxTargetsChosen(Card c, SpellAbility sa) 	{ return getMaxTargets(c, sa) == numTargeted; }
	public boolean isMinTargetsChosen(Card c, SpellAbility sa) 	{ return getMinTargets(c, sa) <= numTargeted; }
	
	private String tgtZone = Constant.Zone.Play;
	public void setZone(String tZone) { tgtZone = tZone; }
	public String getZone() { return tgtZone; }
	
	// Card or Player are legal targets.
	private ArrayList<Card> targetCards = new ArrayList<Card>();
	private ArrayList<Player> targetPlayers = new ArrayList<Player>();
	
	public void addTarget(Object o){
		if (o instanceof Player){
			Player p = (Player)o;
			if (!targetPlayers.contains(p))
				targetPlayers.add(p);
		}
		if (o instanceof Card){
			Card c = (Card)o;
			if (!targetCards.contains(c))
				targetCards.add(c);
		}
	}
	
	public boolean addTarget(Card c){
		if (!targetCards.contains(c)){
			targetCards.add(c);
			numTargeted++;
			return true;
		}
		return false;
	}

	public boolean addTarget(Player p){
		if (!targetPlayers.contains(p)){
			targetPlayers.add(p);
			numTargeted++;
			return true;
		}
		return false;
	}

	public ArrayList<Card> getTargetCards(){
		return targetCards;
	}
	
	public ArrayList<Player> getTargetPlayers(){
		return targetPlayers;
	}
	
	public ArrayList<Object> getTargets(){
		ArrayList<Object> tgts = new ArrayList<Object>();
		tgts.addAll(targetPlayers);
		tgts.addAll(targetCards);

		return tgts;
	}
	
	private int numTargeted = 0;
	public int getNumTargeted() { return numTargeted; }
	
	public void resetTargets() { 
		numTargeted = 0; 
		targetCards.clear();
		targetPlayers.clear();
	}
	
	public Target(String parse){
		this(parse, "1", "1");
	}
	
	public Target(String parse, String min, String max){
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
			valid = "Creature,Planeswalker.YouDontCtrl,Player";
			prompt = "Select target creature, planeswalker, or player";
		}
		else if (parse.equals("C")){
			valid = "Creature";
			prompt = "Select target creature";
		}
		else if (parse.equals("P")){
			valid = "Planeswalker.YouDontCtrl,Player";
			prompt = "Select target planeswalker or player";
		}
		else{
			System.out.println("Bad Parsing in Target(parse, min, max): "+parse);
			return;
		}
		
		vtSelection = prompt;
		ValidTgts = valid.split(",");
		
		minTargets = min;
		maxTargets = max;
	}
	
	public Target(String select, String[] valid){		
		this(select, valid, "1", "1");
	}
	
	public Target(String select, String[] valid, String min, String max){
		tgtValid = true;
		vtSelection = select;
		ValidTgts = valid;
		
		minTargets = min;
		maxTargets = max;
	}
	
	public String getTargetedString(){
		ArrayList<Object> tgts = getTargets();
		StringBuilder sb = new StringBuilder("");
		for(Object o : tgts){
			if (o instanceof Player){
				Player p = (Player)o;
				sb.append(p.getName());
			}
			if (o instanceof Card){
				Card c = (Card)o;
				sb.append(c);
			}
			sb.append(" ");
		}
		
		return sb.toString();
	}
	
	
	// These below functions are quite limited to the damage classes, we should find a way to move them into AF_DealDamage
	public boolean canTgtPlayer() {
		for(String s: ValidTgts){
			if (s.equals("Player") || s.equals("Opponent"))
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
