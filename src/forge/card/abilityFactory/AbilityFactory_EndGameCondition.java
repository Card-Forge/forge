package forge.card.abilityFactory;

import java.util.ArrayList;
import java.util.HashMap;


import forge.AllZone;
import forge.Card;
import forge.ComputerUtil;
import forge.Player;
import forge.card.spellability.Ability_Activated;
import forge.card.spellability.Ability_Sub;
import forge.card.spellability.Spell;
import forge.card.spellability.SpellAbility;
import forge.card.spellability.Target;

public class AbilityFactory_EndGameCondition {
	// ***********************************************************************************************
	// ***************************************** Wins Game *******************************************
	// ***********************************************************************************************
	public static SpellAbility createAbilityWinsGame(final AbilityFactory AF){

		final SpellAbility abWinsGame = new Ability_Activated(AF.getHostCard(), AF.getAbCost(), AF.getAbTgt()){
			private static final long serialVersionUID = 8869422603616247307L;
			
			final AbilityFactory af = AF;
		
			@Override
			public String getStackDescription(){
				// when getStackDesc is called, just build exactly what is happening
				return winsGameStackDescription(af, this);
			}

			public boolean canPlayAI()
			{
				return winsGameCanPlayAI(af, this);
			}
			
			@Override
			public void resolve() {
				winsGameResolve(af, this);
			}

			@Override
			public boolean doTrigger(boolean mandatory) {
				return winsGameDoTriggerAI(af, this, mandatory);
			}
			
		};
		return abWinsGame;
	}
	
	public static SpellAbility createSpellWinsGame(final AbilityFactory AF){
		final SpellAbility spWinsGame = new Spell(AF.getHostCard(), AF.getAbCost(), AF.getAbTgt()){
			private static final long serialVersionUID = 6631124959690157874L;
			
			final AbilityFactory af = AF;
		
			@Override
			public String getStackDescription(){
			// when getStackDesc is called, just build exactly what is happening
				return winsGameStackDescription(af, this);
			}
			
			public boolean canPlayAI()
			{
				// if X depends on abCost, the AI needs to choose which card he would sacrifice first
				// then call xCount with that card to properly calculate the amount
				// Or choosing how many to sacrifice 
				return winsGameCanPlayAI(af, this);
			}
			
			@Override
			public void resolve() {
				winsGameResolve(af, this);
			}
			
		};
		return spWinsGame;
	}
	
	public static SpellAbility createDrawbackWinsGame(final AbilityFactory AF){
		final SpellAbility dbWinsGame = new Ability_Sub(AF.getHostCard(), AF.getAbTgt()){
			private static final long serialVersionUID = 6631124959690157874L;
			
			final AbilityFactory af = AF;
		
			@Override
			public String getStackDescription(){
			// when getStackDesc is called, just build exactly what is happening
				return winsGameStackDescription(af, this);
			}
			
			public boolean canPlayAI()
			{
				// if X depends on abCost, the AI needs to choose which card he would sacrifice first
				// then call xCount with that card to properly calculate the amount
				// Or choosing how many to sacrifice 
				return winsGameCanPlayAI(af, this);
			}
			
			@Override
			public void resolve() {
				winsGameResolve(af, this);
			}

			@Override
			public boolean chkAI_Drawback() {
				return true;
			}

			@Override
			public boolean doTrigger(boolean mandatory) {
				return winsGameDoTriggerAI(af, this, mandatory);
			}
			
		};
		return dbWinsGame;
	}

	public static String winsGameStackDescription(AbilityFactory af, SpellAbility sa){
		StringBuilder sb = new StringBuilder();

		if (!(sa instanceof Ability_Sub))
			sb.append(sa.getSourceCard().getName()).append(" - ");
		else
			sb.append(" ");

		// Let the spell description also be the stack description
		sb.append(sa.getDescription());

		Ability_Sub abSub = sa.getSubAbility();
		if (abSub != null) {
			sb.append(abSub.getStackDescription());
		}

		return sb.toString();
	}
	
	public static boolean winsGameCanPlayAI(final AbilityFactory af, final SpellAbility sa){
		if (AllZone.ComputerPlayer.cantWin())
			return false;

		// TODO: Check conditions are met on card (e.g. Coalition Victory)
		
		// TODO: Consider likelihood of SA getting countered
		
		// In general, don't return true. 
		// But this card wins the game, I can make an exception for that
		return true;
	}
	
	public static boolean winsGameDoTriggerAI(AbilityFactory af, SpellAbility sa, boolean mandatory){
		if (!ComputerUtil.canPayCost(sa) && !mandatory)	// If there is a cost payment it's usually not mandatory
			return false;

		// WinGame abilities usually don't have subAbilities but for consistency...
		Ability_Sub abSub = sa.getSubAbility();
		if (abSub != null) {
			return abSub.doTrigger(mandatory);
		}

		return true;
	}
	
	public static void winsGameResolve(final AbilityFactory af, final SpellAbility sa){
		HashMap<String,String> params = af.getMapParams();
		if (!AbilityFactory.checkConditional(params, sa)){
			AbilityFactory.resolveSubAbility(sa);
			return;
		}		
		
		Card card = af.getHostCard();

		ArrayList<Player> players = AbilityFactory.getDefinedPlayers(card, af.getMapParams().get("Defined"), sa);
		
		for(Player p : players)
			p.altWinConditionMet(card.getName());
		
		if (af.hasSubAbility()){
			Ability_Sub abSub = sa.getSubAbility();
			if (abSub != null){
	     	   abSub.resolve();
	        }
		}
	}
	
	// ***********************************************************************************************
	// **************************************** Loses Game *******************************************
	// ***********************************************************************************************
	
	public static SpellAbility createAbilityLosesGame(final AbilityFactory AF){

		final SpellAbility abLosesGame = new Ability_Activated(AF.getHostCard(), AF.getAbCost(), AF.getAbTgt()){
			private static final long serialVersionUID = 8869422603616247307L;
			
			final AbilityFactory af = AF;
		
			@Override
			public String getStackDescription(){
				// when getStackDesc is called, just build exactly what is happening
				return losesGameStackDescription(af, this);
			}

			public boolean canPlayAI()
			{
				return losesGameCanPlayAI(af, this);
			}
			
			@Override
			public void resolve() {
				losesGameResolve(af, this);
			}

			@Override
			public boolean doTrigger(boolean mandatory) {
				return losesGameDoTriggerAI(af, this, mandatory);
			}
			
		};
		return abLosesGame;
	}
	
	public static SpellAbility createSpellLosesGame(final AbilityFactory AF){
		final SpellAbility spLosesGame = new Spell(AF.getHostCard(), AF.getAbCost(), AF.getAbTgt()){
			private static final long serialVersionUID = 6631124959690157874L;
			
			final AbilityFactory af = AF;
		
			@Override
			public String getStackDescription(){
			// when getStackDesc is called, just build exactly what is happening
				return losesGameStackDescription(af, this);
			}
			
			public boolean canPlayAI()
			{
				// if X depends on abCost, the AI needs to choose which card he would sacrifice first
				// then call xCount with that card to properly calculate the amount
				// Or choosing how many to sacrifice 
				return losesGameCanPlayAI(af, this);
			}
			
			@Override
			public void resolve() {
				losesGameResolve(af, this);
			}
			
		};
		return spLosesGame;
	}
	
	public static SpellAbility createDrawbackLosesGame(final AbilityFactory AF){
		final SpellAbility dbLosesGame = new Ability_Sub(AF.getHostCard(), AF.getAbTgt()){
			private static final long serialVersionUID = 6631124959690157874L;
			
			final AbilityFactory af = AF;
		
			@Override
			public String getStackDescription(){
			// when getStackDesc is called, just build exactly what is happening
				return losesGameStackDescription(af, this);
			}
			
			public boolean canPlayAI()
			{
				// if X depends on abCost, the AI needs to choose which card he would sacrifice first
				// then call xCount with that card to properly calculate the amount
				// Or choosing how many to sacrifice 
				return losesGameCanPlayAI(af, this);
			}
			
			@Override
			public void resolve() {
				losesGameResolve(af, this);
			}

			@Override
			public boolean chkAI_Drawback() {
				return true;
			}

			@Override
			public boolean doTrigger(boolean mandatory) {
				return losesGameDoTriggerAI(af, this, mandatory);
			}
			
		};
		return dbLosesGame;
	}

	public static String losesGameStackDescription(AbilityFactory af, SpellAbility sa){
		StringBuilder sb = new StringBuilder();
		Card source = sa.getSourceCard();

		if (!(sa instanceof Ability_Sub))
			sb.append(source.getName()).append(" - ");
		else
			sb.append(" ");

		Target tgt = sa.getTarget();
		ArrayList<Player> players = null;
		if (sa.getTarget() != null) {
			players = tgt.getTargetPlayers();
		}
		else{
			players = AbilityFactory.getDefinedPlayers(source, af.getMapParams().get("Defined"), sa);
		}
		
		for(Player p : players){
			sb.append(p.getName()).append(" ");
		}
		
		sb.append("loses the game.");

		Ability_Sub abSub = sa.getSubAbility();
		if (abSub != null) {
			sb.append(abSub.getStackDescription());
		}

		return sb.toString();
	}
	
	public static boolean losesGameCanPlayAI(final AbilityFactory af, final SpellAbility sa){
		if (AllZone.HumanPlayer.cantLose())
			return false;
		
		// Only one SA Lose the Game card right now, which is Door to Nothingness
		
		 Target tgt = sa.getTarget();
		 if (tgt != null){
			 tgt.resetTargets();
			 tgt.addTarget(AllZone.HumanPlayer);
		 }
		
		// In general, don't return true. 
		// But this card wins the game, I can make an exception for that
		return true;
	}
	
	public static boolean losesGameDoTriggerAI(AbilityFactory af, SpellAbility sa, boolean mandatory){
		if (!ComputerUtil.canPayCost(sa) && !mandatory)	// If there is a cost payment it's usually not mandatory
			return false;

		// Phage the Untouchable 
		// (Final Fortune would need to attach it's delayed trigger to a specific turn, which can't be done yet)
		
		if (!mandatory && AllZone.HumanPlayer.cantLose())
			return false;
		
		Target tgt = sa.getTarget();
		if (tgt != null) {
			tgt.resetTargets();
			tgt.addTarget(AllZone.HumanPlayer);
		}
		
		// WinGame abilities usually don't have subAbilities but for consistency...
		Ability_Sub abSub = sa.getSubAbility();
		if (abSub != null) {
			return abSub.doTrigger(mandatory);
		}

		return true;
	}
	
	public static void losesGameResolve(final AbilityFactory af, final SpellAbility sa){
		HashMap<String,String> params = af.getMapParams();
		if (!AbilityFactory.checkConditional(params, sa)){
			AbilityFactory.resolveSubAbility(sa);
			return;
		}
		
		Card card = af.getHostCard();

		Target tgt = sa.getTarget();
		ArrayList<Player> players = null;
		if (sa.getTarget() != null) {
			players = tgt.getTargetPlayers();
		}
		else{
			players = AbilityFactory.getDefinedPlayers(card, af.getMapParams().get("Defined"), sa);
		}
		
		for(Player p : players)
			p.altLoseConditionMet(card.getName());
		
		if (af.hasSubAbility()){
			Ability_Sub abSub = sa.getSubAbility();
			if (abSub != null){
	     	   abSub.resolve();
	        }
		}
	}
	
}
