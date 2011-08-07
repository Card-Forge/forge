package forge.card.abilityFactory;

import java.util.ArrayList;
import java.util.HashMap;

import forge.AllZone;
import forge.AllZoneUtil;
import forge.Card;
import forge.CardList;
import forge.ComputerUtil;
import forge.MyRandom;
import forge.card.cardFactory.CardFactoryUtil;
import forge.card.spellability.Ability_Activated;
import forge.card.spellability.Ability_Sub;
import forge.card.spellability.Cost;
import forge.card.spellability.Spell;
import forge.card.spellability.SpellAbility;
import forge.card.spellability.SpellAbility_StackInstance;
import forge.card.spellability.Target;
import forge.card.spellability.Target_Selection;

//Destination - send countered spell to: (only applies to Spells; ignored for Abilities)
//		-Graveyard (Default)
//		-Exile
//		-TopOfLibrary
//		-Hand
//		-BottomOfLibrary
//		-ShuffleIntoLibrary
//PowerSink - true if the drawback type part of Power Sink should be used
//ExtraActions - this has been removed.  All SubAbilitys should now use the standard SubAbility system

//Examples:
//A:SP$Counter | Cost$ 1 G | TargetType$ Activated | SpellDescription$ Counter target activated ability.
//A:AB$Counter | Cost$ G G | TargetType$ Spell | Destination$ Exile | ValidTgts$ Color.Black | SpellDescription$ xxxxx

public class AbilityFactory_CounterMagic {

	private AbilityFactory af = null;
	private HashMap<String,String> params = null;
	private String destination = null;
	private String unlessCost = null;

	public AbilityFactory_CounterMagic(AbilityFactory newAF) {
		af = newAF;
		params = af.getMapParams();
		
		destination = params.containsKey("Destination") ? params.get("Destination") : "Graveyard";
		
		if(params.containsKey("UnlessCost")) 
			unlessCost = params.get("UnlessCost").trim();

	}

	public SpellAbility getAbilityCounter(final AbilityFactory AF) {
		final SpellAbility abCounter = new Ability_Activated(AF.getHostCard(), AF.getAbCost(), AF.getAbTgt()) {
			private static final long serialVersionUID = -3895990436431818899L;

			@Override
			public String getStackDescription() {
				// when getStackDesc is called, just build exactly what is happening
				return counterStackDescription(af, this);
			}

			@Override
			public boolean canPlayAI() {
				return counterCanPlayAI(af, this);
			}

			@Override
			public void resolve() {
				counterResolve(af, this);
			}

			@Override
			public boolean doTrigger(boolean mandatory) {
				return counterCanPlayAI(af, this);
			}

		};
		return abCounter;
	}

	public SpellAbility getSpellCounter(final AbilityFactory AF) {
		final SpellAbility spCounter = new Spell(AF.getHostCard(), AF.getAbCost(), AF.getAbTgt()) {
			private static final long serialVersionUID = -4272851734871573693L;

			@Override
			public String getStackDescription() {
				return counterStackDescription(af, this);
			}

			@Override
			public boolean canPlayAI() {
				return counterCanPlayAI(af, this);
			}

			@Override
			public void resolve() {
				counterResolve(af, this);
			}

		};
		return spCounter;
	}
	
	// Add Counter Drawback
	public SpellAbility getDrawbackCounter(final AbilityFactory AF) {
		final SpellAbility dbCounter = new Ability_Sub(AF.getHostCard(), AF.getAbTgt()) {
			private static final long serialVersionUID = -4272851734871573693L;

			@Override
			public String getStackDescription() {
				return counterStackDescription(af, this);
			}

			@Override
			public boolean canPlayAI() {
				return counterCanPlayAI(af, this);
			}

			@Override
			public void resolve() {
				counterResolve(af, this);
			}
			
			@Override
			public boolean chkAI_Drawback() {
				return counterDoTriggerAI(af, this, true);
			}

			@Override
			public boolean doTrigger(boolean mandatory) {
				return counterDoTriggerAI(af, this, mandatory);
			}

		};
		return dbCounter;
	}

	private boolean counterCanPlayAI(final AbilityFactory af, final SpellAbility sa){
		boolean toReturn = true;
		Cost abCost = af.getAbCost();
		final Card source = sa.getSourceCard();
		if(AllZone.Stack.size() < 1) {
			return false;
		}
		
		if (abCost != null){
			// AI currently disabled for these costs
			if (abCost.getSacCost() && !abCost.getSacThis()){
				//only sacrifice something that's supposed to be sacrificed 
				String type = abCost.getSacType();
			    CardList typeList = AllZoneUtil.getPlayerCardsInPlay(AllZone.ComputerPlayer);
			    typeList = typeList.getValidCards(type.split(","), source.getController(), source);
			    if(ComputerUtil.getCardPreference(source, "SacCost", typeList) == null)
			    	return false;
			}
			if (abCost.getLifeCost()){
				if (AllZone.ComputerPlayer.getLife() - abCost.getLifeAmount() < 4)
					return false;
			}
		}
		
		Target tgt = sa.getTarget();
		if (tgt != null) {
			
			SpellAbility topSA = AllZone.Stack.peekAbility();
			if (!CardFactoryUtil.isCounterable(topSA.getSourceCard()) || topSA.getActivatingPlayer().isComputer())
				return false;
		
			tgt.resetTargets();
			if (Target_Selection.matchSpellAbility(sa, topSA, tgt))
				tgt.addTarget(topSA);
			else
				return false;
		}
		
		
		if (unlessCost != null){
			// Is this Usable Mana Sources? Or Total Available Mana?
			int usableManaSources = CardFactoryUtil.getUsableManaSources(AllZone.HumanPlayer);
			int toPay = 0;
			boolean setPayX = false;
			if (unlessCost.equals("X") && source.getSVar(unlessCost).equals("Count$xPaid")){
				setPayX = true;
				toPay = ComputerUtil.determineLeftoverMana(sa);
			}
			else
				toPay = AbilityFactory.calculateAmount(source, unlessCost, sa);
			
			if (toPay == 0)
				return false;
			
			if (toPay <= usableManaSources){
				// If this is a reusable Resource, feel free to play it most of the time
				if (!sa.getPayCosts().isReusuableResource() || MyRandom.random.nextFloat() < .4)
					return false;
			}
			
			if (setPayX)
				source.setSVar("PayX", Integer.toString(toPay));
		}
		
		// TODO: Improve AI
		
		// Will return true if this spell can counter (or is Reusable and can force the Human into making decisions) 
		
		// But really it should be more picky about how it counters things

		Ability_Sub subAb = sa.getSubAbility();
		if (subAb != null)
			toReturn &= subAb.chkAI_Drawback();
		
		return toReturn;
	}
	
	public boolean counterDoTriggerAI(AbilityFactory af, SpellAbility sa, boolean mandatory){
		boolean toReturn = true;
		if(AllZone.Stack.size() < 1) {
			return false;
		}

		Target tgt = sa.getTarget();
		if (tgt != null) {
			SpellAbility topSA = AllZone.Stack.peekAbility();
			if (!CardFactoryUtil.isCounterable(topSA.getSourceCard()) || topSA.getActivatingPlayer().isComputer())
				return false;

			tgt.resetTargets();
			if (Target_Selection.matchSpellAbility(sa, topSA, tgt))
				tgt.addTarget(topSA);
			else
				return false;
		
			Card source = sa.getSourceCard();
			if (unlessCost != null){
				// Is this Usable Mana Sources? Or Total Available Mana?
				int usableManaSources = CardFactoryUtil.getUsableManaSources(AllZone.HumanPlayer);
				int toPay = 0;
				boolean setPayX = false;
				if (unlessCost.equals("X") && source.getSVar(unlessCost).equals("Count$xPaid")){
					setPayX = true;
					toPay = ComputerUtil.determineLeftoverMana(sa);
				}
				else
					toPay = AbilityFactory.calculateAmount(source, unlessCost, sa);
				
				if (toPay == 0)
					return false;
				
				if (toPay <= usableManaSources){
					// If this is a reusable Resource, feel free to play it most of the time
					if (!sa.getPayCosts().isReusuableResource() || MyRandom.random.nextFloat() < .4)
						return false;
				}
				
				if (setPayX)
					source.setSVar("PayX", Integer.toString(toPay));
			}
		}
		
		// TODO: Improve AI
		
		// Will return true if this spell can counter (or is Reusable and can force the Human into making decisions) 
		
		// But really it should be more picky about how it counters things

		Ability_Sub subAb = sa.getSubAbility();
		if (subAb != null)
			toReturn &= subAb.chkAI_Drawback();
		
		return toReturn;
	}
	
	private void counterResolve(final AbilityFactory af, final SpellAbility sa) {
		
		// TODO: Before this resolves we should see if any of our targets are still on the stack
		ArrayList<SpellAbility> sas;
		
		Target tgt = af.getAbTgt();
		if (tgt != null)
			sas = tgt.getTargetSAs();
		else
			sas = AbilityFactory.getDefinedSpellAbilities(sa.getSourceCard(), params.get("Defined"), sa);

        if(params.containsKey("ForgetOtherTargets"))
        {
            if(params.get("ForgetOtherTargets").equals("True"))
            {
                af.getHostCard().clearRemembered();
            }
        }

		for(final SpellAbility tgtSA : sas){
			Card tgtSACard = tgtSA.getSourceCard();
			
			if (tgtSA.isSpell() && tgtSACard.keywordsContain("CARDNAME can't be countered."))
				continue;
				
			SpellAbility_StackInstance si = AllZone.Stack.getInstanceFromSpellAbility(tgtSA);
			if (si == null)
				continue;

			removeFromStack(tgtSA,sa, si);
				
			// Destroy Permanent may be able to be turned into a SubAbility
			if(tgtSA.isAbility() && params.containsKey("DestroyPermanent")) {
				AllZone.GameAction.destroy(tgtSACard);
			}

            if(params.containsKey("RememberTargets"))
            {
                if(params.get("RememberTargets").equals("True"))
                {
                    af.getHostCard().addRemembered(tgtSACard);
                }
            }
		}
	}//end counterResolve

	private String counterStackDescription(AbilityFactory af, SpellAbility sa) {

		StringBuilder sb = new StringBuilder();

		if (!(sa instanceof Ability_Sub))
			sb.append(sa.getSourceCard().getName()).append(" - ");
		else
			sb.append(" ");
		
		ArrayList<SpellAbility> sas;
		
		Target tgt = af.getAbTgt();
		if (tgt != null)
			sas = tgt.getTargetSAs();
		else
			sas = AbilityFactory.getDefinedSpellAbilities(sa.getSourceCard(), params.get("Defined"), sa);
		
		sb.append("countering");
		
		boolean isAbility = false;
		for(final SpellAbility tgtSA : sas){
			sb.append(" ");
			sb.append(tgtSA.getSourceCard());
			isAbility = tgtSA.isAbility();
			if(isAbility) sb.append("'s ability");
		}
		
		if(isAbility && params.containsKey("DestroyPermanent")) {
			sb.append(" and destroy it");
		}
		
		sb.append(".");

		Ability_Sub abSub = sa.getSubAbility();
		if (abSub != null){
			sb.append(abSub.getStackDescription());
		}

		return sb.toString();
	}//end counterStackDescription
	
	private void removeFromStack(SpellAbility tgtSA, SpellAbility srcSA, SpellAbility_StackInstance si) {
		AllZone.Stack.remove(si);
		
		if(tgtSA.isAbility())  {
			//For Ability-targeted counterspells - do not move it anywhere, even if Destination$ is specified.
		}
		else if(destination.equals("Graveyard")) {
			AllZone.GameAction.moveToGraveyard(tgtSA.getSourceCard());
		}
		else if(destination.equals("Exile")) {
			AllZone.GameAction.exile(tgtSA.getSourceCard());
		}
		else if(destination.equals("TopOfLibrary")) {
			AllZone.GameAction.moveToLibrary(tgtSA.getSourceCard());
		}
		else if(destination.equals("Hand")) {
			AllZone.GameAction.moveToHand(tgtSA.getSourceCard());
		}
		else if(destination.equals("BottomOfLibrary")) {
			AllZone.GameAction.moveToBottomOfLibrary(tgtSA.getSourceCard());
		}
		else if(destination.equals("ShuffleIntoLibrary")) {
			AllZone.GameAction.moveToBottomOfLibrary(tgtSA.getSourceCard());
			tgtSA.getSourceCard().getController().shuffle();
		}
		else {
			throw new IllegalArgumentException("AbilityFactory_CounterMagic: Invalid Destination argument for card " + srcSA.getSourceCard().getName());
		}
		
		if (!tgtSA.isAbility())
			System.out.println("Send countered spell to " + destination);
	}
	
}//end class AbilityFactory_CounterMagic
