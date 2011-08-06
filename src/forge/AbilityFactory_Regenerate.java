package forge;

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
		}; // Spell

		return spRegenerate;
	}

	private static boolean doCanPlayAI(final AbilityFactory af, final SpellAbility sa) {
		// temporarily disabled until better AI
		if (af.getAbCost().getSacCost())	 return false;
		if (af.getAbCost().getSubCounter())  return false;
		if (af.getAbCost().getLifeCost())	 return false;

		if (!ComputerUtil.canPayCost(sa))
			return false;

		//TODO perhaps we could see if there is a spell on the stack that opponent
		//controls targeting a regeneratable creature, and return a higher
		//chance of true

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
						if(af.isTargeted()) sa.setTargetCard(card);
						return true;
					}
				}
			}
		}
		return false;
	}

	private static void doResolve(final AbilityFactory af, final SpellAbility sa) {
		Card hostCard = af.getHostCard();
		
		Card tgtCard = sa.getTargetCard();
		final Card[] c = new Card[1];
        if (af.isTargeted()) {
        	if (!CardFactoryUtil.canTarget(hostCard, tgtCard))
        		return;
        	else
        		c[0] = tgtCard;
        }
        else {
        	c[0] = hostCard;
        }

		final Command untilEOT = new Command() {
			private static final long serialVersionUID = 1922050611313909200L;

			public void execute() {
				c[0].setShield(0);

			}
		};

		if(AllZone.GameAction.isCardInPlay(c[0]) && CardFactoryUtil.canTarget(af.getHostCard(), c[0])) {
			c[0].addShield();
			AllZone.EndOfTurn.addUntil(untilEOT);
		}
	}//doResolve
}
