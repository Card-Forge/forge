package forge.card.abilityFactory;

import java.util.ArrayList;
import java.util.HashMap;

import forge.AllZone;
import forge.AllZoneUtil;
import forge.Card;
import forge.CardList;
import forge.CardListFilter;
import forge.Command;
import forge.ComputerUtil;
import forge.GameActionUtil;
import forge.MyRandom;
import forge.Player;
import forge.card.cardFactory.CardFactoryUtil;
import forge.card.spellability.Ability;
import forge.card.spellability.Ability_Activated;
import forge.card.spellability.Ability_Sub;
import forge.card.spellability.Spell;
import forge.card.spellability.SpellAbility;
import forge.card.spellability.Target;
import forge.card.spellability.Target_Selection;

//Destination - send countered spell to: (only applies to Spells; ignored for Abilities)
//		-Graveyard (Default)
//		-Exile
//		-TopOfLibrary
//		-Hand
//		-BottomOfLibrary
//		-ShuffleIntoLibrary
//UnlessCost - counter target spell unless it's controller pays this cost
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
		if(AllZone.Stack.size() < 1) {
			return false;
		}
		
		SpellAbility topSA = AllZone.Stack.peek();
		if (!CardFactoryUtil.isCounterable(topSA.getSourceCard()) || topSA.getActivatingPlayer().isComputer())
			return false;
		
		Target tgt = sa.getTarget();
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
		
		SpellAbility topSA = AllZone.Stack.peek();
		if (!CardFactoryUtil.isCounterable(topSA.getSourceCard()) || topSA.getActivatingPlayer().isComputer())
			return false;
		
		Target tgt = sa.getTarget();
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
		
		// TODO: Improve AI
		
		// Will return true if this spell can counter (or is Reusable and can force the Human into making decisions) 
		
		// But really it should be more picky about how it counters things

		Ability_Sub subAb = sa.getSubAbility();
		if (subAb != null)
			toReturn &= subAb.chkAI_Drawback();
		
		return toReturn;
	}
	
	private void counterResolve(final AbilityFactory af, final SpellAbility sa) {
		if (!AbilityFactory.checkConditional(params, sa)){
			AbilityFactory.resolveSubAbility(sa);
			return;
		}	
		
		// TODO: Before this resolves we should see if any of our targets are still on the stack
		Target tgt = sa.getTarget();
		
		ArrayList<SpellAbility> sas = tgt.getTargetSAs();
		
		if (sas == null){
			sas = AbilityFactory.getDefinedSpellAbilities(sa.getSourceCard(), params.get("Defined"), sa);
		}

        if(params.containsKey("ForgetOtherTargets"))
        {
            if(params.get("ForgetOtherTargets").equals("True"))
            {
                af.getHostCard().clearRemembered();
            }
        }

		for(final SpellAbility tgtSA : sas){
			Card tgtSACard = tgtSA.getSourceCard();
			if (AllZone.Stack.contains(tgtSA) && !tgtSACard.keywordsContain("CARDNAME can't be countered.")){

				// TODO: Unless Cost should be generalized for all AFS
				if(unlessCost != null) {					
					String unlessCostFinal = unlessCost;
					if(unlessCost.equals("X"))
						unlessCostFinal = Integer.toString(CardFactoryUtil.xCount(af.getHostCard(), af.getHostCard().getSVar("X")));
					// Above xCount should probably be changed to a AF.calculateAmount
					
					Ability ability = new Ability(af.getHostCard(), unlessCostFinal) {
	                    @Override
	                    public void resolve() {
	                        ;
	                    }
	                };
	                
	                final Command unpaidCommand = new Command() {
	                    private static final long serialVersionUID = 8094833091127334678L;
	                    
	                    public void execute() {
	                    	removeFromStack(tgtSA, sa);
	                    	if(params.containsKey("PowerSink")) doPowerSink(AllZone.HumanPlayer);
	                    }
	                };
	                
	                if(tgtSA.getActivatingPlayer().isHuman()) {
	                	GameActionUtil.payManaDuringAbilityResolve(af.getHostCard() + "\r\n", ability.getManaCost(), 
	                			Command.Blank, unpaidCommand);
	                } else {
	                    if(ComputerUtil.canPayCost(ability)) 
	                    	ComputerUtil.playNoStack(ability);
	                    
	                    else {
	                        removeFromStack(tgtSA,sa);
	                        if(params.containsKey("PowerSink")) doPowerSink(AllZone.ComputerPlayer);
	                    }
	                }
				}
				else
					removeFromStack(tgtSA,sa);
					
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
		}

		AbilityFactory.resolveSubAbility(sa);
	}//end counterResolve
	
	private void doPowerSink(Player p) {
		//get all lands with mana abilities
		CardList lands = AllZoneUtil.getPlayerLandsInPlay(p);
		lands = lands.filter(new CardListFilter() {
			public boolean addCard(Card c) {
				return c.getManaAbility().size() > 0;
			}
		});
		//tap them
		for(Card c:lands) c.tap();
		
		//empty mana pool
		if(p.isHuman()) AllZone.ManaPool.clearPool();
	}

	private String counterStackDescription(AbilityFactory af, SpellAbility sa) {

		StringBuilder sb = new StringBuilder();

		if (!(sa instanceof Ability_Sub))
			sb.append(sa.getSourceCard().getName()).append(" - ");
		else
			sb.append(" ");

		sb.append("countering");
		
		ArrayList<SpellAbility> sas = sa.getTarget().getTargetSAs();
		if (sas == null)
			sas = AbilityFactory.getDefinedSpellAbilities(sa.getSourceCard(), params.get("Defined"), sa);
		
		boolean isAbility = false;
		for(final SpellAbility tgtSA : sas){
			sb.append(" ");
			sb.append(tgtSA.getSourceCard().getName());
			isAbility = tgtSA.isAbility();
			if(isAbility) sb.append("'s ability");
		}
		
		if(isAbility && params.containsKey("DestroyPermanent")) {
			sb.append(" and Destroy it");
		}
		
		sb.append(".");

		Ability_Sub abSub = sa.getSubAbility();
		if (abSub != null){
			sb.append(abSub.getStackDescription());
		}

		return sb.toString();
	}//end counterStackDescription
	
	private void removeFromStack(SpellAbility tgtSA, SpellAbility srcSA) {
		AllZone.Stack.remove(tgtSA);
		
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
