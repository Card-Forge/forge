package forge.card.spellability;

import java.util.ArrayList;

import forge.AllZone;
import forge.Card;
import forge.Constant;
import forge.Player;
import forge.card.abilityFactory.AbilityFactory;
import forge.card.cardFactory.CardFactoryUtil;

public class Target {
	private boolean bMandatory = false;
	private Card srcCard;
	
	public boolean getMandatory() { return bMandatory; }
	public void setMandatory(boolean m)	{ bMandatory = m; }
	
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
	
	private String tgtZone = Constant.Zone.Battlefield;
	public void setZone(String tZone) { tgtZone = tZone; }
	public String getZone() { return tgtZone; }
	
	// Used for Counters. Currently, Spell,Activated,Triggered can be Comma-separated
	private String targetSpellAbilityType = null;
	public void setTargetSpellAbilityType(String tgtSAType) { targetSpellAbilityType = tgtSAType; }
	public String getTargetSpellAbilityType() { return targetSpellAbilityType; }
	
	// Used for Counters. The target SA of this SA must be targeting a Valid X
	private String saValidTargeting = null;
	public void setSAValidTargeting(String saValidTgting) { saValidTargeting = saValidTgting; }
	public String getSAValidTargeting() { return saValidTargeting; }
	
	// Card or Player are legal targets.
	private ArrayList<Card> targetCards = new ArrayList<Card>();
	private ArrayList<Player> targetPlayers = new ArrayList<Player>();
	private ArrayList<SpellAbility> targetSAs = new ArrayList<SpellAbility>();
	
	public void addTarget(Object o){
		if (o instanceof Player)
			addTarget((Player)o);
		
		else if (o instanceof Card)
			addTarget((Card)o);
		
		else if (o instanceof SpellAbility)
			addTarget((SpellAbility)o);
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
	
	public boolean addTarget(SpellAbility sa){
		if (!targetSAs.contains(sa)){
			targetSAs.add(sa);
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
	
	public ArrayList<SpellAbility> getTargetSAs(){
		return targetSAs;
	}
	
	public ArrayList<Object> getTargets(){
		ArrayList<Object> tgts = new ArrayList<Object>();
		tgts.addAll(targetPlayers);
		tgts.addAll(targetCards);
		tgts.addAll(targetSAs);

		return tgts;
	}
	
	private int numTargeted = 0;
	public int getNumTargeted() { return numTargeted; }
	
	public void resetTargets() { 
		numTargeted = 0; 
		targetCards.clear();
		targetPlayers.clear();
		targetSAs.clear();
	}
	
	public Target(Card src,String parse){
		this(src,parse, "1", "1");
	}
	
	public Target(Card src,String parse, String min, String max){
		// parse=Tgt{C}{P} - Primarily used for Pump or Damage 
		// C = Creature   P=Player/Planeswalker
		// CP = All three
		
		tgtValid = true;
		srcCard = src;

		if (parse.contains("Tgt")){
			parse = parse.replace("Tgt", "");
		}

		String valid;
		String prompt;
		StringBuilder sb = new StringBuilder();
		
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
		
		if(src != null) sb.append(src + " - ");
		sb.append(prompt);
		vtSelection = sb.toString();
		ValidTgts = valid.split(",");
		
		minTargets = min;
		maxTargets = max;
	}
	
	public Target(Card src, String select, String[] valid){		
		this(src, select, valid, "1", "1");
	}
	
	public Target(Card src, String select, String[] valid, String min, String max){
		srcCard = src;
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
	
		
	public boolean canOnlyTgtOpponent() {
		boolean player = false;
		boolean opponent = false;
		for(String s: ValidTgts){
			if (s.equals("Opponent"))
				opponent = true;
			else if (s.equals("Player"))
				player = true;
		}
		return opponent && !player; 
	}
	
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
	
	public boolean hasCandidates()
	{
		if(canTgtPlayer())
		{
			return true;
		}
		
		for(Card c : AllZone.getZone(tgtZone,AllZone.HumanPlayer).getCards())
		{
			if(c.isValidCard(ValidTgts, srcCard.getController(), srcCard) && CardFactoryUtil.canTarget(srcCard, c))
			{
				return true;
			}
		}
		
		for(Card c : AllZone.getZone(tgtZone,AllZone.ComputerPlayer).getCards())
		{
			if(c.isValidCard(ValidTgts, srcCard.getController(), srcCard) && CardFactoryUtil.canTarget(srcCard, c))
			{
				return true;
			}
		}
		
		return false;
	}
}
