package forge;

import java.util.HashMap;
import java.util.Random;

public class AbilityFactory_Counters {
	// An AbilityFactory subclass for Putting or Removing Counters on Cards.
	
	public static SpellAbility createAbilityPutCounters(final AbilityFactory AF){

		final SpellAbility abPutCounter = new Ability_Activated(AF.getHostCard(), AF.getAbCost(), AF.getAbTgt()){
			private static final long serialVersionUID = -1259638699008542484L;
			
			final AbilityFactory af = AF;
			final HashMap<String,String> params = af.getMapParams();
			final int amount = calculateAmount(af.getHostCard(), params.get("CounterNum"));
			final String type = params.get("CounterType");
		
			@Override
			public String getStackDescription(){
			// when getStackDesc is called, just build exactly what is happening
				 Counters cType = Counters.valueOf(type);
				 StringBuilder sb = new StringBuilder();
				 String name = af.getHostCard().getName();
				 sb.append(name).append(" - Put ").append(amount).append(" ").append(cType.getName()).append(" counter on ");
				 Card tgt = getTargetCard();
				 if (tgt != null)
					 sb.append(tgt.getName());
				 else
					 sb.append(name);
				 return sb.toString();
			}
			
			public boolean canPlay(){
				// super takes care of AdditionalCosts
				return (CardFactoryUtil.canUseAbility(af.getHostCard()) && super.canPlay());	
			}
			
			public boolean canPlayAI()
			{
				return putCanPlayAI(af, this, amount, type);
			}
			
			@Override
			public void resolve() {
				putResolve(af, this, amount, type);
			}
			
		};
		return abPutCounter;
	}
	
	public static int calculateAmount(Card card, String counterNum){
		int amount;
		if (counterNum.matches("X"))
		{
			String calcX = card.getSVar(counterNum);
			if (calcX.startsWith("Count$"))
			{
				String kk[] = calcX.split("\\$");
				amount = kk[1].equals("none") ? 0 : CardFactoryUtil.xCount(card, kk[1]);
			}
			else
				amount = 0;
		}
		else
			amount = Integer.parseInt(counterNum);
		return amount;
	}
	
	public static boolean putCanPlayAI(final AbilityFactory af, final SpellAbility sa, final int amount, final String type){
		// AI needs to be expanded, since this function can be pretty complex based on what the expected targets could be
		Random r = new Random();
		Ability_Cost abCost = sa.getPayCosts();
		Target abTgt = sa.getTarget();
		final Card source = sa.getSourceCard();
		CardList list;
		Card choice = null;
		
		String player = af.isCurse() ? Constant.Player.Human : Constant.Player.Computer;
		
		list = new CardList(AllZone.getZone(Constant.Zone.Play, player).getCards());
		list = list.filter(new CardListFilter() {
			public boolean addCard(Card c) {
				return AllZone.GameAction.canTarget(c, source);
			}
		});
		
		if (abTgt != null){
			if (abTgt.canTgtCreature()){
				list = list.getType("creature");
			}
			else{
				list = list.getValidCards(abTgt.getValidTgts());
			}
			if (list.size() == 0)
				return false;
		}
		
		if (abCost != null){
			// AI currently disabled for these costs
			if (abCost.getSacCost()) return false;	
			if (abCost.getLifeCost())	 return false;
			if (abCost.getDiscardCost()) return false;
			
			if (abCost.getSubCounter()){
				// A card has a 25% chance per counter to be able to pass through here
				// 8+ counters will always pass. 0 counters will never
				int currentNum = source.getCounters(abCost.getCounterType());
				double percent = .25 * (currentNum / abCost.getCounterNum());
				if (percent <= r.nextFloat())
					return false;
			}
		}
		
		if (!ComputerUtil.canPayCost(sa))
			return false;
		
		 // prevent run-away activations - first time will always return true
		 boolean chance = r.nextFloat() <= Math.pow(.6667, source.getAbilityUsed());
		 
		 // Targeting
		 if (abTgt != null){
			 if (af.isCurse()){
				 if (type.equals("M1M1")){
					 // try to kill the best killable creature, or reduce the best one 
					 CardList killable = list.filter(new CardListFilter() {
						public boolean addCard(Card c) {
							return c.getNetDefense() <= amount;
						}
					 });
					 if (killable.size() > 0)
						 choice = CardFactoryUtil.AI_getBestCreature(killable);
					 else
						 choice = CardFactoryUtil.AI_getBestCreature(list);
				 }
				 else{
					 // improve random choice here
					 list.shuffle();
					 choice = list.get(0);
				 }
			 }
			 else{
				 if (type.equals("P1P1")){
					 choice = CardFactoryUtil.AI_getBestCreature(list);
				 }
				 else{
					 // The AI really should put counters on cards that can use it. 
					 // Charge counters on things with Charge abilities, etc. Expand these above
					 list.shuffle();
					 choice = list.get(0);
				 } 
			 }
			 if (choice == null)
				 return false;
			 sa.setTargetCard(choice);
		 }
		 else{
			// Placeholder: No targeting necessary
			 int currCounters = sa.getSourceCard().getCounters(Counters.valueOf(type));
			// each counter on the card is a 10% chance of not activating this ability. 
			 if (r.nextFloat() < .1 * currCounters)	
				 return false;
		 }
		 
		 return ((r.nextFloat() < .6667) && chance);
	}
	
	public static void putResolve(final AbilityFactory af, final SpellAbility sa, int counterAmount, final String type){
		HashMap<String,String> params = af.getMapParams();
		String DrawBack = params.get("SubAbility");
		Card card = af.getHostCard();
		
		Card tgtCard = (sa.getTarget() == null) ? card : sa.getTargetCard();
		tgtCard.addCounter(Counters.valueOf(type), counterAmount);
		
		if (af.hasSubAbility())
			 CardFactoryUtil.doDrawBack(DrawBack, counterAmount, card.getController(), AllZone.GameAction.getOpponent(card.getController()), card.getController(), card, null, sa);

	}
}
