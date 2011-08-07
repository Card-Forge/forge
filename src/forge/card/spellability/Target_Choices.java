package forge.card.spellability;

import java.util.ArrayList;

import forge.Card;
import forge.Player;

public class Target_Choices {
	private int numTargeted = 0;
	public int getNumTargeted() { return numTargeted; }
	
	// Card or Player are legal targets.
	private ArrayList<Card> targetCards = new ArrayList<Card>();
	private ArrayList<Player> targetPlayers = new ArrayList<Player>();
	private ArrayList<SpellAbility> targetSAs = new ArrayList<SpellAbility>();

	public boolean addTarget(Object o){
		if (o instanceof Player)
			return addTarget((Player)o);
		
		else if (o instanceof Card)
			return addTarget((Card)o);
		
		else if (o instanceof SpellAbility)
			return addTarget((SpellAbility)o);
		
		return false;
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
			if (o instanceof SpellAbility){
				SpellAbility sa = (SpellAbility)o;
				sb.append(sa);
			}
			sb.append(" ");
		}
		
		return sb.toString();
	}
}
