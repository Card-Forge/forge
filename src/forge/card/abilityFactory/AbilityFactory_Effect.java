package forge.card.abilityFactory;

import java.util.HashMap;
import java.util.Random;

import forge.AllZone;
import forge.Card;
import forge.Command;
import forge.ComputerUtil;
import forge.MyRandom;
import forge.Player;
import forge.card.spellability.Ability_Activated;
import forge.card.spellability.Ability_Sub;
import forge.card.spellability.Spell;
import forge.card.spellability.SpellAbility;
import forge.card.spellability.Target;
import forge.card.trigger.Trigger;
import forge.card.trigger.TriggerHandler;

public class AbilityFactory_Effect {
	public static SpellAbility createAbilityEffect(final AbilityFactory AF){

		final SpellAbility abEffect = new Ability_Activated(AF.getHostCard(), AF.getAbCost(), AF.getAbTgt()){
			private static final long serialVersionUID = 8869422603616247307L;
			
			final AbilityFactory af = AF;
		
			@Override
			public String getStackDescription(){
				// when getStackDesc is called, just build exactly what is happening
				return effectStackDescription(af, this);
			}

			public boolean canPlayAI(){
				return effectCanPlayAI(af, this);
			}
			
			@Override
			public void resolve() {
				effectResolve(af, this);
			}

			@Override
			public boolean doTrigger(boolean mandatory) {
				return effectDoTriggerAI(af, this, mandatory);
			}
			
		};
		return abEffect;
	}
	
	public static SpellAbility createSpellEffect(final AbilityFactory AF){
		final SpellAbility spEffect = new Spell(AF.getHostCard(), AF.getAbCost(), AF.getAbTgt()){
			private static final long serialVersionUID = 6631124959690157874L;
			
			final AbilityFactory af = AF;
		
			@Override
			public String getStackDescription(){
			// when getStackDesc is called, just build exactly what is happening
				return effectStackDescription(af, this);
			}
			
			public boolean canPlayAI(){
				return effectCanPlayAI(af, this);
			}
			
			@Override
			public void resolve() {
				effectResolve(af, this);
			}
			
		};
		return spEffect;
	}
	
	public static SpellAbility createDrawbackEffect(final AbilityFactory AF){
		final SpellAbility dbEffect = new Ability_Sub(AF.getHostCard(), AF.getAbTgt()){
			private static final long serialVersionUID = 6631124959690157874L;
			
			final AbilityFactory af = AF;
		
			@Override
			public String getStackDescription(){
			// when getStackDesc is called, just build exactly what is happening
				return effectStackDescription(af, this);
			}
			
			public boolean canPlayAI(){
				return effectCanPlayAI(af, this);
			}
			
			@Override
			public void resolve() {
				effectResolve(af, this);
			}

			@Override
			public boolean chkAI_Drawback() {
				return true;
			}

			@Override
			public boolean doTrigger(boolean mandatory) {
				return effectDoTriggerAI(af, this, mandatory);
			}
			
		};
		return dbEffect;
	}

	public static String effectStackDescription(AbilityFactory af, SpellAbility sa){
		StringBuilder sb = new StringBuilder();

		if (sa instanceof Ability_Sub)
			sb.append(" ");
		else
			sb.append(sa.getSourceCard().getName()).append(" - ");
		
		sb.append(sa.getDescription());

		Ability_Sub abSub = sa.getSubAbility();
		if (abSub != null) {
			sb.append(abSub.getStackDescription());
		}

		return sb.toString();
	}
	
	public static boolean effectCanPlayAI(final AbilityFactory af, final SpellAbility sa){
		Random r = MyRandom.random;
		
		Target tgt = sa.getTarget();
		if (tgt != null){
			tgt.resetTargets();
			if (tgt.canOnlyTgtOpponent())
				tgt.addTarget(AllZone.HumanPlayer);
			else
				tgt.addTarget(AllZone.ComputerPlayer);		
		}

		return ((r.nextFloat() < .6667));
	}
	
	public static boolean effectDoTriggerAI(AbilityFactory af, SpellAbility sa, boolean mandatory){
		if (!ComputerUtil.canPayCost(sa) && !mandatory)	// If there is a cost payment it's usually not mandatory
			return false;

		// TODO: Add targeting effects

		// check SubAbilities DoTrigger?
		Ability_Sub abSub = sa.getSubAbility();
		if (abSub != null) {
			return abSub.doTrigger(mandatory);
		}

		return true;
	}
	
	public static void effectResolve(final AbilityFactory af, final SpellAbility sa){
		HashMap<String,String> params = af.getMapParams();
		Card card = af.getHostCard();
		
		String[] effectAbilities = null;
		String[] effectTriggers = null;
		String[] effectSVars = null;
		String[] effectKeywords = null;
		
		if(params.containsKey("Abilities"))
			effectAbilities = params.get("Abilities").split(",");

		if(params.containsKey("Triggers"))
			effectTriggers = params.get("Triggers").split(",");
		
		if(params.containsKey("SVars"))
			effectSVars = params.get("SVars").split(",");
		
		if(params.containsKey("Keywords"))
			effectKeywords = params.get("Keywords").split(",");
		
		//Effect eff = new Effect();
		String name = params.get("Name");
		if (name == null)
			name = sa.getSourceCard().getName() + "'s Effect";
		
		Player controller = sa.getActivatingPlayer();
		Card eff = new Card();
		eff.setName(name);
		eff.addType("Effect");	// Or Emblem
		eff.setToken(true);	// Set token to true, so when leaving play it gets nuked
		eff.setController(controller);
		eff.setOwner(controller);
		eff.setImageName(card.getImageName());
		eff.setColor(card.getColor());
		
		// Effects should be Orange or something probably
		
		final Card e = eff;
		
		// Abilities, triggers and SVars work the same as they do for Token
		//Grant abilities
		if(effectAbilities != null){
			for(String s : effectAbilities){
				AbilityFactory abFactory = new AbilityFactory();
				String actualAbility = af.getHostCard().getSVar(s);

				SpellAbility grantedAbility = abFactory.getAbility(actualAbility, eff);
				eff.addSpellAbility(grantedAbility);
			}
		}
		
		//Grant triggers
		if(effectTriggers != null){
			for(String s : effectTriggers){
				String actualTrigger = af.getHostCard().getSVar(s);
				
				//Needs to do some voodoo when the effect disappears to remove the triggers at the same time.
				Command LPCommand = new Command() {

					private static final long serialVersionUID = -9007707442828928732L;

					public void execute() {
						AllZone.TriggerHandler.removeAllFromCard(e);
					}
					
				};
				eff.addLeavesPlayCommand(LPCommand);
				Trigger parsedTrigger = TriggerHandler.parseTrigger(actualTrigger, eff);
				eff.addTrigger(parsedTrigger);
				AllZone.TriggerHandler.registerTrigger(parsedTrigger);
			}
		}
		
		//Grant SVars
		if(effectSVars != null){
			for(String s : effectSVars){
				String actualSVar = af.getHostCard().getSVar(s);
				eff.setSVar(s, actualSVar);
			}
		}
		
		//Grant Keywords
		if(effectKeywords != null){
			for(String s : effectKeywords){
				String actualKeyword = af.getHostCard().getSVar(s);
				eff.addIntrinsicKeyword(actualKeyword);
			}
		}
		
		// Duration
		String duration = params.get("Duration");
		if (duration == null || !duration.equals("Permanent")){
			final Command endEffect = new Command() {
				private static final long serialVersionUID = -5861759814760561373L;
	
				public void execute() {
					AllZone.GameAction.exile(e);
				}
			};
			
			if (duration == null || duration.equals("EndOfTurn"))
				AllZone.EndOfTurn.addUntil(endEffect);
		}
		
		// TODO: Add targeting to the effect so it knows who it's dealing with
		
		AllZone.GameAction.moveToPlay(eff);
	}
}
