package forge;

import java.util.HashMap;

public class AbilityFactory_Combat {
	//**************************************************************
	// ****************************** FOG **************************
	//**************************************************************
	
	public static SpellAbility createAbilityFog(final AbilityFactory AF){
		final SpellAbility abFog = new Ability_Activated(AF.getHostCard(), AF.getAbCost(), AF.getAbTgt()){
			private static final long serialVersionUID = -1933592438783630254L;
			
			final AbilityFactory af = AF;
			
			public boolean canPlayAI()
			{
				return fogCanPlayAI(af, this);
			}
			
			@Override
			public void resolve() {
				fogResolve(af, this);
			}
			
		};
		return abFog;
	}
	
	public static SpellAbility createSpellFog(final AbilityFactory AF){
		final SpellAbility spFog = new Spell(AF.getHostCard(), AF.getAbCost(), AF.getAbTgt()){
			private static final long serialVersionUID = -5141246507533353605L;
			
			final AbilityFactory af = AF;
			
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
			
		};
		return dbFog;
	}
	
	public static boolean fogCanPlayAI(final AbilityFactory af, SpellAbility sa){
		// AI should only activate this during Human's Declare Blockers phase
		boolean chance = AllZone.Phase.is(Constant.Phase.Combat_Declare_Blockers_InstantAbility, sa.getActivatingPlayer().getOpponent());

		// Only cast when Stack is empty, so Human uses spells/abilities first
		chance &= AllZone.Stack.size() == 0;
		
		// Some additional checks on how much Damage/Poison AI would take, or how many creatures would be lost
		
		Ability_Sub subAb = sa.getSubAbility();
		if (subAb != null)
			chance &= subAb.chkAI_Drawback();
		
		return chance;
	}
	
	public static boolean fogPlayDrawbackAI(final AbilityFactory af, SpellAbility sa){
		// AI should only activate this during Human's turn
		boolean chance = AllZone.Phase.isPlayerTurn(sa.getActivatingPlayer().getOpponent()) || 
			AllZone.Phase.isAfter(Constant.Phase.Combat_Damage);
		
		Ability_Sub subAb = sa.getSubAbility();
		if (subAb != null)
			chance &= subAb.chkAI_Drawback();
		
		return chance;
	}
	
	public static void fogResolve(final AbilityFactory af, final SpellAbility sa){
		HashMap<String,String> params = af.getMapParams();
		Card card = sa.getSourceCard();
		String DrawBack = params.get("SubAbility");
		
		// Expand Fog keyword here depending on what we need out of it.
		AllZone.GameInfo.setPreventCombatDamageThisTurn(true);
		
		if (af.hasSubAbility()){
			Ability_Sub abSub = sa.getSubAbility();
			if (abSub != null){
			   abSub.resolve();
			}
			else
				CardFactoryUtil.doDrawBack(DrawBack, 0, card.getController(), card.getController().getOpponent(), card.getController(), card, null, sa);
		}
	}
}
