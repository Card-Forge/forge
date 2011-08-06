package forge;

import java.util.HashMap;

public class AbilityFactory_Combat {
	public static SpellAbility createAbilityFog(final AbilityFactory AF){
		final SpellAbility abFog = new Ability_Activated(AF.getHostCard(), AF.getAbCost(), AF.getAbTgt()){
			private static final long serialVersionUID = -1933592438783630254L;
			
			final AbilityFactory af = AF;
			
			public boolean canPlay(){
				// super takes care of AdditionalCosts
				return super.canPlay();	
			}
			
			public boolean canPlayAI()
			{
				return putCanPlayAI(af);
			}
			
			@Override
			public void resolve() {
				putResolve(af, this);
			}
			
		};
		return abFog;
	}
	
	public static SpellAbility createSpellFog(final AbilityFactory AF){
		final SpellAbility spFog = new Spell(AF.getHostCard(), AF.getAbCost(), AF.getAbTgt()){
			private static final long serialVersionUID = -5141246507533353605L;
			
			final AbilityFactory af = AF;
			
			public boolean canPlay(){
				// super takes care of AdditionalCosts
				return super.canPlay();	
			}
			
			public boolean canPlayAI()
			{
				return putCanPlayAI(af);
			}
			
			@Override
			public void resolve() {
				putResolve(af, this);
			}
			
		};
		return spFog;
	}
	
	public static boolean putCanPlayAI(final AbilityFactory af){
		// AI cannot use this properly until he can use SAs during Humans turn
		return false;
	}
	
	public static void putResolve(final AbilityFactory af, final SpellAbility sa){
		HashMap<String,String> params = af.getMapParams();
		Card card = sa.getSourceCard();
		String DrawBack = params.get("SubAbility");
		
		// Expand Fog keyword here depending on what we need out of it.
		AllZone.GameInfo.setPreventCombatDamageThisTurn(true);
		
		if (af.hasSubAbility())
			 CardFactoryUtil.doDrawBack(DrawBack, 0, card.getController(), card.getController().getOpponent(), card.getController(), card, null, sa);

	}
}
