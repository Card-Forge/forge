package forge.card.abilityFactory;

import java.util.ArrayList;
import java.util.HashMap;

import forge.AllZone;
import forge.ComputerUtil;
import forge.Player;
import forge.card.spellability.Ability_Activated;
import forge.card.spellability.Ability_Sub;
import forge.card.spellability.Spell;
import forge.card.spellability.SpellAbility;
import forge.card.spellability.Target;

public class AbilityFactory_Turns {
	
	// *************************************************************************
	// ************************* ADD TURN **************************************
	// *************************************************************************
	
	public static SpellAbility createAbilityAddTurn(final AbilityFactory af) {

		final SpellAbility abAddTurn = new Ability_Activated(af.getHostCard(), af.getAbCost(), af.getAbTgt()) {
			private static final long serialVersionUID = -3526200766738015688L;
		
			@Override
			public String getStackDescription() {
				return addTurnStackDescription(af, this);
			}

			@Override
			public boolean canPlayAI() {
				return addTurnCanPlayAI(af, this);
			}
			
			@Override
			public void resolve() {
				addTurnResolve(af, this);
			}

			@Override
			public boolean doTrigger(boolean mandatory) {
				return addTurnTriggerAI(af, this, mandatory);
			}
			
		};
		return abAddTurn;
	}
	
	public static SpellAbility createSpellAddTurn(final AbilityFactory af) {
		final SpellAbility spAddTurn = new Spell(af.getHostCard(), af.getAbCost(), af.getAbTgt()) {
			private static final long serialVersionUID = -3921131887560356006L;
		
			@Override
			public String getStackDescription(){
				return addTurnStackDescription(af, this);
			}
			
			@Override
			public boolean canPlayAI() {
				return addTurnCanPlayAI(af, this);
			}
			
			@Override
			public void resolve() {
				addTurnResolve(af, this);
			}
			
		};
		return spAddTurn;
	}
	
	public static SpellAbility createDrawbackAddTurn(final AbilityFactory af) {
		final SpellAbility dbAddTurn = new Ability_Sub(af.getHostCard(), af.getAbTgt()) {
			private static final long serialVersionUID = -562517287448810951L;
		
			@Override
			public String getStackDescription() {
				return addTurnStackDescription(af, this);
			}
			
			@Override
			public void resolve() {
				addTurnResolve(af, this);
			}

			@Override
			public boolean chkAI_Drawback() {
				return true;
			}

			@Override
			public boolean doTrigger(boolean mandatory) {
				return addTurnTriggerAI(af, this, mandatory);
			}
			
		};
		return dbAddTurn;
	}

	private static String addTurnStackDescription(AbilityFactory af, SpellAbility sa) {
		StringBuilder sb = new StringBuilder();
		int numTurns = AbilityFactory.calculateAmount(af.getHostCard(), af.getMapParams().get("NumTurns"), sa);

		if (!(sa instanceof Ability_Sub))
			sb.append(sa.getSourceCard().getName()).append(" - ");
		else
			sb.append(" ");

		ArrayList<Player> tgtPlayers;

		Target tgt = af.getAbTgt();
		if(tgt != null)
			tgtPlayers = tgt.getTargetPlayers();
		else
			tgtPlayers = AbilityFactory.getDefinedPlayers(sa.getSourceCard(), af.getMapParams().get("Defined"), sa);
		
		for(Player player : tgtPlayers)
			sb.append(player).append(" ");
		
		sb.append("takes ");
		if(numTurns > 1) {
			sb.append(numTurns);
		}
		else {
			sb.append("an");
		}
		sb.append(" extra turn");
		if(numTurns > 1) sb.append("s");
		sb.append(" after this one.");

		Ability_Sub abSub = sa.getSubAbility();
		if(abSub != null) {
			sb.append(abSub.getStackDescription());
		}

		return sb.toString();
	}
	
	private static boolean addTurnCanPlayAI(final AbilityFactory af, final SpellAbility sa) {
		return addTurnTriggerAI(af, sa, false);
	}
	
	private static boolean addTurnTriggerAI(final AbilityFactory af, final SpellAbility sa, boolean mandatory) {
		if (!ComputerUtil.canPayCost(sa))
			return false;

		Target tgt = sa.getTarget();

		if(sa.getTarget() != null){
			tgt.resetTargets();
			sa.getTarget().addTarget(AllZone.ComputerPlayer);
		}
		else{
			ArrayList<Player> tgtPlayers = AbilityFactory.getDefinedPlayers(sa.getSourceCard(), af.getMapParams().get("Defined"), sa);
			for(Player p : tgtPlayers)
				if(p.isHuman() && !mandatory)
					return false;
			// not sure if the AI should be playing with cards that give the Human more turns.
		}
		return true;
	}
	
	private static void addTurnResolve(final AbilityFactory af, final SpellAbility sa){
		HashMap<String,String> params = af.getMapParams();
		int numTurns = AbilityFactory.calculateAmount(af.getHostCard(), params.get("NumTurns"), sa);
		
		ArrayList<Player> tgtPlayers;

		Target tgt = af.getAbTgt();
		if (tgt != null)
			tgtPlayers = tgt.getTargetPlayers();
		else
			tgtPlayers = AbilityFactory.getDefinedPlayers(sa.getSourceCard(), af.getMapParams().get("Defined"), sa);
		
		for(Player p : tgtPlayers) {
			if (tgt == null || p.canTarget(af.getHostCard())) {
				for(int i = 0; i < numTurns; i++) {
					AllZone.Phase.addExtraTurn(p);
				}
			}
		}
		
		if(af.hasSubAbility()) {
			Ability_Sub abSub = sa.getSubAbility();
			if(abSub != null) {
	     	   abSub.resolve();
	        }
		}
	}
	
}//end class AbilityFactory_Turns
