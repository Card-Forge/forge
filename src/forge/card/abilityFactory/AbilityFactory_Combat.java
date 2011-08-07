package forge.card.abilityFactory;

import java.util.HashMap;

import forge.AllZone;
import forge.CombatUtil;
import forge.ComputerUtil;
import forge.Constant;
import forge.card.spellability.Ability_Activated;
import forge.card.spellability.Ability_Sub;
import forge.card.spellability.Spell;
import forge.card.spellability.SpellAbility;

public class AbilityFactory_Combat {
	//**************************************************************
	// ****************************** FOG **************************
	//**************************************************************
	
	public static SpellAbility createAbilityFog(final AbilityFactory AF){
		final SpellAbility abFog = new Ability_Activated(AF.getHostCard(), AF.getAbCost(), AF.getAbTgt()){
			private static final long serialVersionUID = -1933592438783630254L;
			
			final AbilityFactory af = AF;
			
			@Override
            public String getStackDescription(){
            // when getStackDesc is called, just build exactly what is happening
                return fogStackDescription(af, this);
            }
			
			public boolean canPlayAI()
			{
				return fogCanPlayAI(af, this);
			}
			
			@Override
			public void resolve() {
				fogResolve(af, this);
			}

			@Override
			public boolean doTrigger(boolean mandatory) {
				return fogDoTriggerAI(af, this, mandatory);
			}
			
		};
		return abFog;
	}
	
	public static SpellAbility createSpellFog(final AbilityFactory AF){
		final SpellAbility spFog = new Spell(AF.getHostCard(), AF.getAbCost(), AF.getAbTgt()){
			private static final long serialVersionUID = -5141246507533353605L;
			
			final AbilityFactory af = AF;
			
			@Override
            public String getStackDescription(){
            // when getStackDesc is called, just build exactly what is happening
                return fogStackDescription(af, this);
            }
			
			public boolean canPlayAI()
			{
				return fogCanPlayAI(af, this);
			}
			
			@Override
			public void resolve() {
				fogResolve(af, this);
			}
			
		};
		return spFog;
	}
	
	public static SpellAbility createDrawbackFog(final AbilityFactory AF){
		final SpellAbility dbFog = new Ability_Sub(AF.getHostCard(), AF.getAbTgt()){
			private static final long serialVersionUID = -5141246507533353605L;
			
			final AbilityFactory af = AF;
			
			@Override
			public void resolve() {
				fogResolve(af, this);
			}

			@Override
			public boolean chkAI_Drawback() {
				return fogPlayDrawbackAI(af, this);
			}

			@Override
			public boolean doTrigger(boolean mandatory) {
				return fogDoTriggerAI(af, this, mandatory);
			}
			
		};
		return dbFog;
	}
	
	public static String fogStackDescription(AbilityFactory af, SpellAbility sa) {
        StringBuilder sb = new StringBuilder();
        
        if (!(sa instanceof Ability_Sub))
            sb.append(sa.getSourceCard().getName()).append(" - ");
        else
            sb.append(" ");
        
        sb.append(sa.getSourceCard().getController());
        sb.append(" prevents all combat damage this turn.");
        
        Ability_Sub abSub = sa.getSubAbility();
        if (abSub != null){
            sb.append(abSub.getStackDescription());
        }
        
        return sb.toString();
    }
	
	public static boolean fogCanPlayAI(final AbilityFactory af, SpellAbility sa){
		// AI should only activate this during Human's Declare Blockers phase
		if (AllZone.Phase.isPlayerTurn(sa.getActivatingPlayer())) return false;
		if (!AllZone.Phase.is(Constant.Phase.Combat_Declare_Blockers_InstantAbility)) return false;
		
		// Only cast when Stack is empty, so Human uses spells/abilities first
		if (AllZone.Stack.size() != 0) return false;
		
		// Don't cast it, if the effect is already in place
		if(AllZone.GameInfo.isPreventCombatDamageThisTurn()) return false;
		
		Ability_Sub subAb = sa.getSubAbility();
		if (subAb != null)
			if(!subAb.chkAI_Drawback()) return false;
		
		// Cast it if life is in danger
		return CombatUtil.lifeInDanger(AllZone.Combat);
	}
	
	public static boolean fogPlayDrawbackAI(final AbilityFactory af, SpellAbility sa){
		// AI should only activate this during Human's turn
		boolean chance;
		if (AllZone.Phase.isPlayerTurn(sa.getActivatingPlayer().getOpponent()))
			chance = AllZone.Phase.isBefore(Constant.Phase.Combat_FirstStrikeDamage);
		else 
			chance = AllZone.Phase.isAfter(Constant.Phase.Combat_Damage);
		
		Ability_Sub subAb = sa.getSubAbility();
		if (subAb != null)
			chance &= subAb.chkAI_Drawback();
		
		return chance;
	}
	
	public static boolean fogDoTriggerAI(AbilityFactory af, SpellAbility sa, boolean mandatory){
		if (!ComputerUtil.canPayCost(sa) && !mandatory)	// If there is a cost payment it's usually not mandatory
			return false;

		boolean chance;
		if (AllZone.Phase.isPlayerTurn(sa.getActivatingPlayer().getOpponent()))
			chance = AllZone.Phase.isBefore(Constant.Phase.Combat_FirstStrikeDamage);
		else 
			chance = AllZone.Phase.isAfter(Constant.Phase.Combat_Damage);
		 
		// check SubAbilities DoTrigger?
		Ability_Sub abSub = sa.getSubAbility();
		if (abSub != null) {
			return chance && abSub.doTrigger(mandatory);
		}
		
		return chance;
	}
	
	public static void fogResolve(final AbilityFactory af, final SpellAbility sa){
		
		// Expand Fog keyword here depending on what we need out of it.
		AllZone.GameInfo.setPreventCombatDamageThisTurn(true);
	}
}
