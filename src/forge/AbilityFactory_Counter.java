package forge;

import java.util.ArrayList;
import java.util.HashMap;

//Type - Spell or Ability or SpellOrAbility
//CounterValid - a "valid" expression for types to counter
//Destination - send countered spell to:
//		-Graveyard
//		-Exile
//		-TopDeck
//		-Hand
//		-BottomDeck
//		-Shuffle
//ExtraActions - implemented exactly as spCounter used them (can probably be updated to SubAbility/Drawback), then this param is eliminated

//Examples:
//A:SP$Counter|Cost$1 G|Type$Ability|SpelDescription$Counter target activated ability.
//A:AB$Counter|Cost$G G|Type$Spell|Destination$Graveyard|CounterValid$Color(Black)|SpellDescription$Counter target black spell.

public class AbilityFactory_Counter {

	private AbilityFactory af = null;
	private HashMap<String,String> params = null;
	private String targetType = null;
	private String destination = null;
	private String[] splitTargetingRestrictions = null;
	private String[] splitExtraActions;

	private final SpellAbility[] tgt = new SpellAbility[1];

	public AbilityFactory_Counter(AbilityFactory newAF) {
		af = newAF;
		params = af.getMapParams();
		if (params.containsKey("Type")) {
			targetType = params.get("Type");
		}
		destination = params.containsKey("Destination") ? params.get("Destination") : "Graveyard";
		if(params.containsKey("CounterValid")) {
			splitTargetingRestrictions = params.get("CounterValid").split(",");
		}
		else splitTargetingRestrictions = new String[] {"Card"};
		if(params.containsKey("ExtraActions")) {
			splitExtraActions = params.get("ExtraActions").split(" ");
		}
		else splitExtraActions = new String[] {"None"};

		tgt[0] = null;
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
			public boolean canPlay() {
				// super takes care of AdditionalCosts
				//important to keep super.canPlay() first due to targeting hack in counterCanPlay
				return super.canPlay() && counterCanPlay(af, this);	
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
			public boolean canPlay() {
				// super takes care of AdditionalCosts
				//important to keep super.canPlay() first due to targeting hack in counterCanPlay
				return super.canPlay() && counterCanPlay(af, this);	
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

	private void counterResolve(final AbilityFactory af, final SpellAbility sa) {
		Card source = sa.getSourceCard();
		//copied from spCounter
		if(matchSpellAbility(sa.getSourceCard(), tgt[0], splitTargetingRestrictions, targetType) 
				&& AllZone.Stack.contains(tgt[0])
				&& !tgt[0].getSourceCard().keywordsContain("CARDNAME can't be countered."))
		{
			SpellAbility tgtSA = tgt[0];
			AllZone.Stack.remove(tgt[0]);

			System.out.println("Send countered spell to " + destination);

			if(destination.equals("None") || targetType.contains("Ability"))  {
				//For Ability-targeting counterspells
			}
			else if(destination.equals("Graveyard")) {
				AllZone.GameAction.moveToGraveyard(tgtSA.getSourceCard());
			}
			else if(destination.equals("Exile")) {
				AllZone.GameAction.exile(tgtSA.getSourceCard());
			}
			else if(destination.equals("Topdeck")) {
				AllZone.GameAction.moveToTopOfLibrary(tgtSA.getSourceCard());
			}
			else if(destination.equals("Hand")) {
				AllZone.GameAction.moveToHand(tgtSA.getSourceCard());
			}
			else if(destination.equals("BottomDeck")) {
				AllZone.GameAction.moveToBottomOfLibrary(tgtSA.getSourceCard());
			}
			else if(destination.equals("Shuffle")) {
				AllZone.GameAction.moveToBottomOfLibrary(tgtSA.getSourceCard());
				tgtSA.getSourceCard().getController().shuffle();
			}
			else {
				throw new IllegalArgumentException("spCounter: Invalid Destination argument for card " + source.getName());
			}

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
					Target = source.getController();
				}
				else if(ActionID.startsWith("Opp-")) {
					ActionID = ActionID.substring(4);
					Target = source.getController().getOpponent();
				}
				else if(ActionID.startsWith("CC-")) {
					ActionID = ActionID.substring(3);
					Target = sa.getSourceCard().getController();
				}

				if(ActionID.startsWith("May-")) {
					ActionID = ActionID.substring(4);
					isOptional = true;
				}

				if(ActionID.equals("Draw")) {
					if(isOptional) {
						if(Target == AllZone.HumanPlayer) {
							if(AllZone.Display.getChoice("Do you want to draw" + SplitActionParams[0] + "card(s)?","Yes","No").equals("Yes")) {
								Target.drawCards(Integer.parseInt(SplitActionParams[0]));
							}
						}
						else {
							//AI decision-making, only draws a card if it doesn't risk discarding it.
							if(AllZone.getZone(Constant.Zone.Hand,AllZone.ComputerPlayer).getCards().length + Integer.parseInt(SplitActionParams[0]) < 6) {
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
							if(AllZone.Display.getChoice("Do you want to discard" + SplitActionParams[0] + "card(s)?","Yes","No").equals("Yes")) {
								Target.discard(Integer.parseInt(SplitActionParams[0]), sa);
							}
						}
						else {
							//AI decisionmaking. Should take Madness cards and the like into account in the future.Right now always refuses to discard.
						}
					}
					else {
						Target.discard(Integer.parseInt(SplitActionParams[0]), sa);
					}
				}
				else if(ActionID.equals("LoseLife")) {
					if(isOptional) {
						if(Target == AllZone.HumanPlayer) {
							if(AllZone.Display.getChoice("Do you want to lose" + SplitActionParams[0] + "life?","Yes","No").equals("Yes")) {
								Target.loseLife(Integer.parseInt(SplitActionParams[0]), source);
							}
						}
						else {
							//AI decisionmaking. Not sure why one would ever want to agree to this, except for the rare case of Near-Death Experience+Ali Baba.
						}
					}
					else {
						Target.loseLife(Integer.parseInt(SplitActionParams[0]), source);
					}

				}
				else if(ActionID.equals("GainLife")) {
					if(isOptional) {
						if(Target == AllZone.HumanPlayer) {
							if(AllZone.Display.getChoice("Do you want to gain" + SplitActionParams[0] + "life?","Yes","No").equals("Yes")) {
								Target.gainLife(Integer.parseInt(SplitActionParams[0]), source);
							}
						}
						else {
							//AI decisionmaking. Not sure why one would ever want to decline this, except for the rare case of Near-Death Experience.
							Target.gainLife(Integer.parseInt(SplitActionParams[0]), source);
						}
					}
					else {
						Target.gainLife(Integer.parseInt(SplitActionParams[0]), source);
					}
				}
				else if(ActionID.equals("RevealHand")) {
					if(isOptional) {
						System.out.println(Target);
						if(Target == AllZone.HumanPlayer) {
							if(AllZone.Display.getChoice("Do you want to reveal your hand?","Yes","No").equals("Yes")) {
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
							CardList list = new CardList(AllZone.getZone(Constant.Zone.Hand,AllZone.ComputerPlayer).getCards());
							AllZone.Display.getChoiceOptional("Revealed cards",list.toArray());
						}
					}
				}
				else if(ActionID.equals("RearrangeTopOfLibrary")) {
					if(isOptional) {
						if(Target == AllZone.HumanPlayer) {
							if(AllZone.Display.getChoice("Do you want to rearrange the top " + SplitActionParams[0] + " cards of your library?","Yes","No").equals("Yes")) {
								AllZoneUtil.rearrangeTopOfLibrary(Target, Integer.parseInt(SplitActionParams[0]), false);
							}
						}
						else {
							//AI decisionmaking. AI simply can't atm, and wouldn't know how best to do it anyway.
						}
					}
					else {
						if(Target == AllZone.HumanPlayer) {
							AllZoneUtil.rearrangeTopOfLibrary(Target, Integer.parseInt(SplitActionParams[0]), false);
						}
						else {
							CardList list = new CardList(AllZone.getZone(Constant.Zone.Hand,AllZone.ComputerPlayer).getCards());
							AllZone.Display.getChoiceOptional("Revealed cards",list.toArray());
						}
					}
				}
				else {
					throw new IllegalArgumentException("spCounter: Invalid Extra Action for card " + sa.getSourceCard().getName());
				}
			}
		}

		//end copied from spCounter
		//}

		if (af.hasSubAbility()){
			Ability_Sub abSub = sa.getSubAbility();
			if (abSub != null){
				if (abSub.getParent() == null)
					abSub.setParent(sa);
				abSub.resolve();
			}
			else{
				String DrawBack = params.get("SubAbility");
				if (af.hasSubAbility())
					CardFactoryUtil.doDrawBack(DrawBack, 0, source.getController(), source.getController().getOpponent(), source.getController(), source, null, sa);
			}
		}

		//reset tgts
		tgt[0] = null;
	}

	private String counterStackDescription(AbilityFactory af, SpellAbility sa){

		StringBuilder sb = new StringBuilder();

		if (!(sa instanceof Ability_Sub))
			sb.append(sa.getSourceCard().getName()).append(" - ");
		else
			sb.append(" ");

		sb.append("countering ");
		sb.append(tgt[0].getSourceCard().getName());
		if(tgt[0].isAbility()) sb.append("'s ability.");
		else sb.append(".");

		Ability_Sub abSub = sa.getSubAbility();
		if (abSub != null){
			abSub.setParent(sa);
			sb.append(abSub.getStackDescription());
		}

		return sb.toString();
	}//end counterStackDescription

	private boolean counterCanPlay(final AbilityFactory af, final SpellAbility sa) {
		ArrayList<SpellAbility> choosables = new ArrayList<SpellAbility>();

		for(int i = 0; i < AllZone.Stack.size(); i++) {
			choosables.add(AllZone.Stack.peek(i));
		}

		for(int i = 0; i < choosables.size(); i++) {
			if(!matchSpellAbility(sa.getSourceCard(), choosables.get(i), 
					splitTargetingRestrictions, targetType)) {
				choosables.remove(i);
			}
		}

		if(tgt[0] == null && choosables.size() > 0 ) AllZone.InputControl.setInput(getInput(sa));

		return choosables.size() > 0;
	}

	private boolean counterCanPlayAI(final AbilityFactory af, final SpellAbility sa){
		boolean toReturn = false;
		if(AllZone.Stack.size() < 1) {
			return false;
		}
		if(matchSpellAbility(sa.getSourceCard(), AllZone.Stack.peek(), splitTargetingRestrictions, targetType)) {
			tgt[0] = AllZone.Stack.peek();
			toReturn = true;
		}

		Ability_Sub subAb = sa.getSubAbility();
		if (subAb != null)
			toReturn &= subAb.chkAI_Drawback();
		return toReturn;
	}

	private static boolean matchSpellAbility(Card srcCard, SpellAbility sa, String[] splitRestrictions, String targetType) {
		if(targetType.equals("Spell")) {
			if(sa.isAbility()) {
				System.out.println(srcCard.getName() + " can only counter spells, not abilities.");
				return false;
			}
		}
		else if(targetType.equals("Ability")) {
			if(sa.isSpell()) {
				System.out.println(srcCard.getName() + " can only counter abilities, not spells.");
				return false;
			}
		}
		else if(targetType.equals("SpellOrAbility")) {
			//Do nothing. This block is only for clarity and enforcing parameters.
		}
		else {
			throw new IllegalArgumentException("Invalid target type for card " + srcCard.getName());
		}
		
		return sa.getSourceCard().isValidCard(splitRestrictions, srcCard.getController(), srcCard);
	}//matchSpellAbility

	private Input getInput(final SpellAbility sa) {
		Input runtime = new Input() {

			private static final long serialVersionUID = 5360660530175041997L;

			@Override
			public void showMessage() {
				ArrayList<SpellAbility> choosables = new ArrayList<SpellAbility>();

				for(int i = 0; i < AllZone.Stack.size(); i++) {
					choosables.add(AllZone.Stack.peek(i));
				}

				for(int i = 0; i < choosables.size(); i++) {
					if(!matchSpellAbility(sa.getSourceCard(), choosables.get(i), splitTargetingRestrictions, targetType) || choosables.get(i).getSourceCard().equals(sa.getSourceCard())) {
						choosables.remove(i);
					}
				}
				HashMap<String,SpellAbility> map = new HashMap<String,SpellAbility>();

				for(SpellAbility sa : choosables) {
					map.put(sa.getStackDescription(),sa);
				}

				String[] choices = new String[map.keySet().size()];
				choices = map.keySet().toArray(choices);

				String madeChoice = AllZone.Display.getChoice("Select target spell.",choices);

				tgt[0] = map.get(madeChoice);
				System.out.println(tgt[0]);
				stop();
			}//showMessage()
		};//Input
		return runtime;
	}

}
