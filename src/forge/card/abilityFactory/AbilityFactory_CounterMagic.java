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
import forge.Constant;
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
import forge.gui.GuiUtils;

//Destination - send countered spell to: (only applies to Spells; ignored for Abilities)
//		-Graveyard (Default)
//		-Exile
//		-TopOfLibrary
//		-Hand
//		-BottomOfLibrary
//		-ShuffleIntoLibrary
//UnlessCost - counter target spell unless it's controller pays this cost
//PowerSink - true if the drawback type part of Power Sink should be used
//ExtraActions - implemented exactly as spCounter used them (can probably be updated to SubAbility/Drawback), then this param is eliminated

//Examples:
//A:SP$Counter | Cost$ 1 G |  TargetType$ Activated | SpellDescription$ Counter target activated ability.
//A:AB$Counter | Cost$ G G | TargetType$ Spell | Destination$ Exile | ValidTgts$ Color.Black | SpellDescription$ xxxxx

public class AbilityFactory_CounterMagic {

	private AbilityFactory af = null;
	private HashMap<String,String> params = null;
	private String destination = null;
	private String[] splitExtraActions;
	private String unlessCost = null;

	public AbilityFactory_CounterMagic(AbilityFactory newAF) {
		af = newAF;
		params = af.getMapParams();
		
		destination = params.containsKey("Destination") ? params.get("Destination") : "Graveyard";
		if(params.containsKey("ExtraActions")) {
			splitExtraActions = params.get("ExtraActions").split(" ");
		}
		else splitExtraActions = new String[] {"None"};
		
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

	private boolean counterCanPlayAI(final AbilityFactory af, final SpellAbility sa){
		boolean toReturn = true;
		if(AllZone.Stack.size() < 1) {
			return false;
		}
		
		SpellAbility topSA = AllZone.Stack.peek();
		if (!CardFactoryUtil.isCounterable(topSA.getSourceCard()))
			return false;
		
		Target tgt = sa.getTarget();
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
		// TODO: Before this resolves we should see if any of our targets are still on the stack
		Card source = sa.getSourceCard();
		Target tgt = sa.getTarget();
		
		ArrayList<SpellAbility> sas = tgt.getTargetSAs();
		
		if (sas == null){
			sas = AbilityFactory.getDefinedSpellAbilities(sa.getSourceCard(), params.get("Defined"), sa);
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
			}
			// Do Extra Actions whether or not the spell was actually countered
			doExtraActions(tgtSA,sa);
		}

		if (af.hasSubAbility()){
			Ability_Sub abSub = sa.getSubAbility();
			if (abSub != null){
				abSub.resolve();
			}
			else{
				//I think UntapUpTo is the main thing holding this back
				String DrawBack = params.get("SubAbility");
				CardFactoryUtil.doDrawBack(DrawBack, 0, source.getController(), source.getController().getOpponent(), source.getController(), source, null, sa);
			}
		}
	}
	
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

	private String counterStackDescription(AbilityFactory af, SpellAbility sa){

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
	
	private void removeFromStack(SpellAbility tgtSA,SpellAbility srcSA)
	{
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
	
	private void doExtraActions(SpellAbility tgtSA,SpellAbility srcSA)
	{
		// TODO: Convert Extra Actions
		for(int ea = 0; ea < splitExtraActions.length; ea++) {
			boolean isOptional = false;

			if(splitExtraActions[0].equals("None")) {
				break;
			}
			String ActionID = splitExtraActions[ea].substring(0,splitExtraActions[ea].indexOf('('));

			Player Target = null;

			String ActionParams = splitExtraActions[ea].substring(splitExtraActions[ea].indexOf('(')+1);
			ActionParams = ActionParams.substring(0,ActionParams.length()-1);

			String[] SplitActionParams = ActionParams.split(",");

			System.out.println("Extra Action: " + ActionID);
			System.out.println("Parameters: " + ActionParams);

			if(ActionID.startsWith("My-")) {
				ActionID = ActionID.substring(3);
				Target = srcSA.getSourceCard().getController();
			}
			else if(ActionID.startsWith("Opp-")) {
				ActionID = ActionID.substring(4);
				Target = srcSA.getSourceCard().getController().getOpponent();
			}
			else if(ActionID.startsWith("CC-")) {
				ActionID = ActionID.substring(3);
				Target = tgtSA.getSourceCard().getController();
			}

			if(ActionID.startsWith("May-")) {
				ActionID = ActionID.substring(4);
				isOptional = true;
			}

			if(ActionID.equals("Draw")) {
				if(isOptional) {
					if(Target == AllZone.HumanPlayer) {
						if(GameActionUtil.showYesNoDialog(srcSA.getSourceCard(), "Do you want to draw " + SplitActionParams[0] + " card(s)?")) {
							Target.drawCards(Integer.parseInt(SplitActionParams[0]));
						}
					}
					else {
						//AI decision-making, only draws a card if it doesn't risk discarding it.
						
						if(AllZoneUtil.getPlayerHand(AllZone.ComputerPlayer).size() + Integer.parseInt(SplitActionParams[0]) < 6) {
							Target.drawCards(Integer.parseInt(SplitActionParams[0]));
						}
					}
				}
				else {
					Target.drawCards(Integer.parseInt(SplitActionParams[0]));
				}

			}
			else if(ActionID.equals("Discard")) {
				if(isOptional) {
					if(Target == AllZone.HumanPlayer) {
						if(GameActionUtil.showYesNoDialog(srcSA.getSourceCard(), "Do you want to discard " + SplitActionParams[0] + " card(s)?")) {
							Target.discard(Integer.parseInt(SplitActionParams[0]), srcSA, true);
						}
					}
					else {
						//AI decisionmaking. Should take Madness cards and the like into account in the future.  Right now always refuses to discard.
					}
				}
				else {
					Target.discard(Integer.parseInt(SplitActionParams[0]), srcSA, true);
				}
			}
			else if(ActionID.equals("LoseLife")) {
				if(isOptional) {
					if(Target == AllZone.HumanPlayer) {
						if(GameActionUtil.showYesNoDialog(srcSA.getSourceCard(), "Do you want to lose " + SplitActionParams[0] + " life?")) {
							Target.loseLife(Integer.parseInt(SplitActionParams[0]), srcSA.getSourceCard());
						}
					}
					else {
						//AI decisionmaking. Not sure why one would ever want to agree to this, except for the rare case of Near-Death Experience+Ali Baba.
					}
				}
				else {
					Target.loseLife(Integer.parseInt(SplitActionParams[0]), srcSA.getSourceCard());
				}

			}
			else if(ActionID.equals("GainLife")) {
				if(isOptional) {
					if(Target == AllZone.HumanPlayer) {
						if(GameActionUtil.showYesNoDialog(srcSA.getSourceCard(), "Do you want to gain" + SplitActionParams[0] + "life?")) {
							Target.gainLife(Integer.parseInt(SplitActionParams[0]), srcSA.getSourceCard());
						}
					}
					else {
						//AI decisionmaking. Not sure why one would ever want to decline this, except for the rare case of Near-Death Experience.
						Target.gainLife(Integer.parseInt(SplitActionParams[0]), srcSA.getSourceCard());
					}
				}
				else {
					Target.gainLife(Integer.parseInt(SplitActionParams[0]), srcSA.getSourceCard());
				}
			}
			else if(ActionID.equals("RevealHand")) {
				if(isOptional) {
					System.out.println(Target);
					if(Target == AllZone.HumanPlayer) {
						if(GameActionUtil.showYesNoDialog(srcSA.getSourceCard(), "Do you want to reveal your hand?")) {
							//Does nothing now, of course, but sometime in the future the AI may be able to remember cards revealed and prioritize discard spells accordingly.
						}
					}
					else {
						//AI decisionmaking. Not sure why one would ever want to agree to this
					}
				}
				else {
					System.out.println(Target);
					if(Target == AllZone.HumanPlayer) {
						//Does nothing now, of course, but sometime in the future the AI may be able to remember cards revealed and prioritize discard spells accordingly.
					}
					else {
						CardList list = AllZoneUtil.getPlayerHand(AllZone.ComputerPlayer);
						GuiUtils.getChoiceOptional("Revealed cards",list.toArray());
					}
				}
			}
			else if(ActionID.equals("RearrangeTopOfLibrary")) {
				if(isOptional) {
					if(Target == AllZone.HumanPlayer) {
						if(GameActionUtil.showYesNoDialog(srcSA.getSourceCard(), "Do you want to rearrange the top " + SplitActionParams[0] + " cards of your library?")) {
							AllZoneUtil.rearrangeTopOfLibrary(srcSA.getSourceCard(), Target, Integer.parseInt(SplitActionParams[0]), false);
						}
					}
					else {
						//AI decisionmaking. AI simply can't atm, and wouldn't know how best to do it anyway.
					}
				}
				else {
					if(Target == AllZone.HumanPlayer) {
						AllZoneUtil.rearrangeTopOfLibrary(srcSA.getSourceCard(), Target, Integer.parseInt(SplitActionParams[0]), false);
					}
					else {
						CardList list = AllZoneUtil.getCardsInZone(Constant.Zone.Hand, AllZone.ComputerPlayer);
						GuiUtils.getChoiceOptional("Revealed cards",list.toArray());
					}
				}
			}
			else {
				throw new IllegalArgumentException("AbilityFactory_CounterMagic: Invalid Extra Action for card " + srcSA.getSourceCard().getName());
			}
		}
	}
}
