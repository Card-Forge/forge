package forge;

import java.util.Random;

public class AbilityFactory_Regenerate {

	public static SpellAbility getAbility(final AbilityFactory AF) {

		final SpellAbility abRegenerate = new Ability_Activated(AF.getHostCard(), AF.getAbCost(), AF.getAbTgt()) {
			private static final long serialVersionUID = -6386981911243700037L;
			AbilityFactory af = AF;
			
			@Override
			public boolean canPlay(){
				return (Cost_Payment.canPayAdditionalCosts(AF.getAbCost(), this) 
						&& CardFactoryUtil.canUseAbility(AF.getHostCard()) 
						&& super.canPlay());
			}

			@Override
			public boolean canPlayAI() {
				return doCanPlayAI(af, this);
			}

			@Override
			public void resolve() {
				doResolve(af, this);
				AF.getHostCard().setAbilityUsed(AF.getHostCard().getAbilityUsed() + 1);
			}
		};//Ability_Activated

		return abRegenerate;
	}

	public static SpellAbility getSpell(final AbilityFactory AF){

		final SpellAbility spRegenerate = new Spell(AF.getHostCard()) {
			private static final long serialVersionUID = -3899905398102316582L;
			AbilityFactory af = AF;

			@Override
			public boolean canPlay(){
				return (Cost_Payment.canPayAdditionalCosts(AF.getAbCost(), this) 
						&& super.canPlay());
			}

			@Override
			public boolean canPlayAI() {
				return doCanPlayAI(af, this);
			}

			@Override
			public void resolve() {
				doResolve(af, this);
			}
		}; // Spell

		return spRegenerate;
	}

	private static boolean doCanPlayAI(final AbilityFactory AF, final SpellAbility saMe) {
		// temporarily disabled until better AI
		if (AF.getAbCost().getSacCost())	 return false;
		if (AF.getAbCost().getSubCounter())  return false;
		if (AF.getAbCost().getLifeCost())	 return false;

		if (!ComputerUtil.canPayCost(saMe))
			return false;

		//TODO: need to special case RegenerateMe keyword

		if(CardFactoryUtil.AI_isMainPhase()) {
			CardList play = AllZoneUtil.getCreaturesInPlay(AllZone.ComputerPlayer);
			for(Card card:play) {
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

					CardList LandList = new CardList(AllZone.getZone(Constant.Zone.Play,
							AllZone.ComputerPlayer).getCards());
					LandList = LandList.getType("Land");

					//most regenerate abilities cost 2 or less
					if(hCMC + 2 >= LandList.size()) weight[2] = 50;
					else weight[2] = 0;

					// ultimately, it's random fate that dictates if this was the right play
					int aw = (weight[0] + weight[1] + weight[2]) / 3;
					Random r = new Random();
					if(r.nextInt(100) <= aw) {
						saMe.setTargetCard(card);
						return true;
					}
				}
			}
		}
		return false;
	}

	private static void doResolve(final AbilityFactory AF, final SpellAbility saMe) {
		final Card c = saMe.getTargetCard();

		final Command untilEOT = new Command() {
			private static final long serialVersionUID = 1922050611313909200L;

			public void execute() {
				c.setShield(0);

			}
		};

		if(AllZone.GameAction.isCardInPlay(c) && CardFactoryUtil.canTarget(AF.getHostCard(), c)) {
			c.addShield();
			AllZone.EndOfTurn.addUntil(untilEOT);
		}
	}//doResolve
}
