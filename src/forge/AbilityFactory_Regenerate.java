package forge;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

public class AbilityFactory_Regenerate {
	
	// Ex: A:SP$Regenerate|Cost$W|Tgt$TgtC|SpellDescription$Regenerate target creature.

	public static SpellAbility getAbility(final AbilityFactory af) {

		final SpellAbility abRegenerate = new Ability_Activated(af.getHostCard(), af.getAbCost(), af.getAbTgt()) {
			private static final long serialVersionUID = -6386981911243700037L;
			
			@Override
			public boolean canPlay(){
				return super.canPlay();
			}

			@Override
			public boolean canPlayAI() {
				return doCanPlayAI(af, this);
			}

			@Override
			public void resolve() {
				doResolve(af, this);
				af.getHostCard().setAbilityUsed(af.getHostCard().getAbilityUsed() + 1);
			}
			
			@Override
			public String getStackDescription(){
				return regenerateStackDescription(af, this);
			}
			
		};//Ability_Activated

		return abRegenerate;
	}

	public static SpellAbility getSpell(final AbilityFactory af){

		final SpellAbility spRegenerate = new Spell(af.getHostCard(), af.getAbCost(), af.getAbTgt()) {
			private static final long serialVersionUID = -3899905398102316582L;
			
			@Override
			public boolean canPlay(){
				return super.canPlay();
			}

			@Override
			public boolean canPlayAI() {
				return doCanPlayAI(af, this);
			}

			@Override
			public void resolve() {
				doResolve(af, this);
			}
			
			@Override
			public String getStackDescription(){
				return regenerateStackDescription(af, this);
			}
			
		}; // Spell

		return spRegenerate;
	}
	
	private static String regenerateStackDescription(AbilityFactory af, SpellAbility sa){
		final HashMap<String,String> params = af.getMapParams();
		StringBuilder sb = new StringBuilder();
		String name = af.getHostCard().getName();

		ArrayList<Card> tgtCards;
		Target tgt = af.getAbTgt();
		if (tgt != null)
			tgtCards = tgt.getTargetCards();
		else
			tgtCards = AbilityFactory.getDefinedCards(sa.getSourceCard(), params.get("Defined"), sa);

		if(tgtCards.size() > 0) {
			if (sa instanceof Ability_Sub)
				sb.append(" ");
			else
				sb.append(name).append(" - ");

			sb.append("Regenerate ");
			for(Card tgtC:tgtCards) {
				if(tgtC.isFaceDown()) sb.append("Morph ");
				else sb.append(tgtC.getName()).append(" ");
			}
		}
		sb.append(".");

		Ability_Sub abSub = sa.getSubAbility();
		if (abSub != null) {
			sb.append(abSub.getStackDescription());
		}

		return sb.toString();
	}

	private static boolean doCanPlayAI(final AbilityFactory af, final SpellAbility sa) {
		final HashMap<String,String> params = af.getMapParams();
		final Card hostCard = af.getHostCard();
		boolean chance = false;
		ArrayList<Card> tgtCards = AbilityFactory.getDefinedCards(sa.getSourceCard(), params.get("Defined"), sa);
		
		// if there is no target and host card isn't in play, don't activate
		if (af.getAbTgt() == null && !AllZone.GameAction.isCardInPlay(hostCard)) 
    		return false;
		
		// temporarily disabled until better AI
		if (af.getAbCost().getSacCost())	 return false;
		if (af.getAbCost().getSubCounter())  return false;
		if (af.getAbCost().getLifeCost())	 return false;

		if (!ComputerUtil.canPayCost(sa))
			return false;

		//TODO perhaps we could see if there is a spell on the stack that opponent
		//controls targeting a regeneratable creature, and return a higher
		//chance of true

		if(!CardFactoryUtil.AI_isMainPhase())	// this will need to be removed soon
			return false;
		
		// TODO: Regenerate can't handle multi-targeting currently, structure is way different than other canPlayAIs
		// so I didn't know where to start. Also, this section is unnecessary for self Regenerate, and doesn't
		// seem to follow the targeting rules otherwise.
		
		CardList play = AllZoneUtil.getCreaturesInPlay(AllZone.ComputerPlayer);
		for(Card card:play) {
			if(!card.canBeShielded() || card.getShield() > 0) return false;
			if(CardFactoryUtil.AI_doesCreatureAttack(card)) {
				//"Fuzzy logic" to determine if using a regenerate ability might be helpful because
				//we can't wait to decide to play this ability during combat, like the human can
				//weight[] is a set of probability percentages to be averaged later
				int weight[] = new int[3];

				// cards with real keywords (flying, trample, etc) are probably more desireable
				if(card.getKeyword().size() > 0) weight[0] = 75;
				else weight[0] = 0;

				// if there are many cards in hand, then maybe it's not such a great idea to waste mana
				CardList HandList = new CardList(AllZone.getZone(Constant.Zone.Hand,
						AllZone.ComputerPlayer).getCards());

				if(HandList.size() >= 4) weight[1] = 25;
				else weight[1] = 75;

				// compare the highest converted mana cost of cards in hand to the number of lands
				// if there's spare mana, then regeneration might be viable
				int hCMC = 0;
				for(int i = 0; i < HandList.size(); i++)
					if(CardUtil.getConvertedManaCost(HandList.getCard(i).getManaCost()) > hCMC) hCMC = CardUtil.getConvertedManaCost(HandList.getCard(
							i).getManaCost());

				CardList LandList = new CardList(AllZone.getZone(Constant.Zone.Battlefield,
						AllZone.ComputerPlayer).getCards());
				LandList = LandList.getType("Land");

				//most regenerate abilities cost 2 or less
				if(hCMC + 2 >= LandList.size()) weight[2] = 50;
				else weight[2] = 0;

				// ultimately, it's random fate that dictates if this was the right play
				int aw = (weight[0] + weight[1] + weight[2]) / 3;
				Random r = new Random();
				if(r.nextInt(100) <= aw) {
					if(af.isTargeted()) 
						af.getAbTgt().addTarget(card);
					chance = true;
					break;
				}
			}
		}
		
		Ability_Sub subAb = sa.getSubAbility();
		if (subAb != null)
			chance &= subAb.chkAI_Drawback();
		
		return chance;
	}

	private static void doResolve(final AbilityFactory af, final SpellAbility sa) {
		Card hostCard = af.getHostCard();
		final HashMap<String,String> params = af.getMapParams();
		
		ArrayList<Card> tgtCards;
		Target tgt = af.getAbTgt();
		if (tgt != null)
			tgtCards = tgt.getTargetCards();
		else
			tgtCards = AbilityFactory.getDefinedCards(hostCard, params.get("Defined"), sa);

		for(final Card tgtC : tgtCards){
			final Command untilEOT = new Command() {
				private static final long serialVersionUID = 1922050611313909200L;

				public void execute() {
					tgtC.setShield(0);
				}
			};
			
			if (AllZone.GameAction.isCardInPlay(tgtC) && (tgt == null || CardFactoryUtil.canTarget(hostCard, tgtC))){
				tgtC.addShield();
				AllZone.EndOfTurn.addUntil(untilEOT);
			}
		}
		
		if (af.hasSubAbility()){
			Ability_Sub abSub = sa.getSubAbility();
			if (abSub != null){
			   abSub.resolve();
			}
			else{
				Card first = tgtCards.get(0);
	        	CardFactoryUtil.doDrawBack(params.get("SubAbility"), 0,
	                hostCard.getController(), hostCard.getController().getOpponent(),
	                first.getController(), hostCard, first, sa);
			}
		}
	}//doResolve
}
