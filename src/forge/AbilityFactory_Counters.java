package forge;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

public class AbilityFactory_Counters {
	// An AbilityFactory subclass for Putting or Removing Counters on Cards.
	
	// *******************************************
	// ********** PutCounters *****************
	// *******************************************
	
	public static SpellAbility createAbilityPutCounters(final AbilityFactory AF){

		final SpellAbility abPutCounter = new Ability_Activated(AF.getHostCard(), AF.getAbCost(), AF.getAbTgt()){
			private static final long serialVersionUID = -1259638699008542484L;
			
			final AbilityFactory af = AF;

			@Override
			public String getStackDescription(){
				return putStackDescription(af, this);
			}
			
			public boolean canPlayAI()
			{
				return putCanPlayAI(af, this);
			}
			
			@Override
			public void resolve() {
				putResolve(af, this);
			}
			
		};
		return abPutCounter;
	}
	
	public static SpellAbility createSpellPutCounters(final AbilityFactory AF){
		final SpellAbility spPutCounter = new Spell(AF.getHostCard(), AF.getAbCost(), AF.getAbTgt()){
			private static final long serialVersionUID = -323471693082498224L;
			
			final AbilityFactory af = AF;
		
			@Override
			public String getStackDescription(){
				return putStackDescription(af, this);
			}
			
			public boolean canPlayAI()
			{
				// if X depends on abCost, the AI needs to choose which card he would sacrifice first
				// then call xCount with that card to properly calculate the amount
				// Or choosing how many to sacrifice 
				return putCanPlayAI(af, this);
			}
			
			@Override
			public void resolve() {
				putResolve(af, this);
			}
			
		};
		return spPutCounter;
	}
	
	public static SpellAbility createDrawbackPutCounters(final AbilityFactory AF){
		final SpellAbility dbPutCounter = new Ability_Sub(AF.getHostCard(), AF.getAbTgt()){
			private static final long serialVersionUID = -323471693082498224L;
			
			final AbilityFactory af = AF;
		
			@Override
			public String getStackDescription(){
				return putStackDescription(af, this);
			}
			
			@Override
			public void resolve() {
				putResolve(af, this);
			}

			@Override
			public boolean chkAI_Drawback() {
				return putPlayDrawbackAI(af, this);
			}
			
		};
		return dbPutCounter;
	}
	
	public static String putStackDescription(AbilityFactory af, SpellAbility sa){
		StringBuilder sb = new StringBuilder();

		if (!(sa instanceof Ability_Sub))
			sb.append(sa.getSourceCard().getName()).append(" - ");
		else
			sb.append(" ");

		Counters cType = Counters.valueOf(af.getMapParams().get("CounterType"));
		String name = af.getHostCard().getName();
		int amount = AbilityFactory.calculateAmount(af.getHostCard(), af.getMapParams().get("CounterNum"), sa);

		sb.append("Put ").append(amount).append(" ").append(cType.getName())
				.append(" counter on");

		if (af.getAbTgt() == null)
			sb.append(" ").append(name);
		else {
			ArrayList<Card> tgts = af.getAbTgt().getTargetCards();
			for (Card c : tgts)
				sb.append(" ").append(c.getName());
		}

		sb.append(".");
		
		Ability_Sub abSub = sa.getSubAbility();
        if (abSub != null){
        	sb.append(abSub.getStackDescription());
        }
		
		return sb.toString();
	}
	
	
	public static boolean putCanPlayAI(final AbilityFactory af, final SpellAbility sa){
		// AI needs to be expanded, since this function can be pretty complex based on what the expected targets could be
		Random r = new Random();
		Ability_Cost abCost = sa.getPayCosts();
		Target abTgt = sa.getTarget();
		final Card source = sa.getSourceCard();
		CardList list;
		Card choice = null;
		String type = af.getMapParams().get("CounterType");
		String amountStr = af.getMapParams().get("CounterNum");
		
		Player player = af.isCurse() ? AllZone.HumanPlayer : AllZone.ComputerPlayer;
		
		list = new CardList(AllZone.getZone(Constant.Zone.Battlefield, player).getCards());
		list = list.filter(new CardListFilter() {
			public boolean addCard(Card c) {
				return CardFactoryUtil.canTarget(source, c);
			}
		});
		
		if (abTgt != null){
			list = list.getValidCards(abTgt.getValidTgts(),source.getController(),source);

			if (list.size() == 0)
				return false;
		}
		
		if (abCost != null){
			// AI currently disabled for these costs
			if (abCost.getSacCost()){ 
				return false;
			}
			if (abCost.getLifeCost())	 return false;
			if (abCost.getDiscardCost()) return false;
			
			if (abCost.getSubCounter()){
				// A card has a 25% chance per counter to be able to pass through here
				// 4+ counters will always pass. 0 counters will never
				int currentNum = source.getCounters(abCost.getCounterType());
				double percent = .25 * (currentNum / abCost.getCounterNum());
				if (percent <= r.nextFloat())
					return false;
			}
		}
		
		if (!ComputerUtil.canPayCost(sa))
			return false;
		
		// TODO handle proper calculation of X values based on Cost
		final int amount = AbilityFactory.calculateAmount(af.getHostCard(), amountStr, sa);
		
		 // prevent run-away activations - first time will always return true
		 boolean chance = r.nextFloat() <= Math.pow(.6667, source.getAbilityUsed());
		 
		 // Targeting
		 if (abTgt != null){
			 abTgt.resetTargets();
			 // target loop
			while(abTgt.getNumTargeted() < abTgt.getMaxTargets(sa.getSourceCard(), sa)){ 
				if (list.size() == 0){
					if (abTgt.getNumTargeted() < abTgt.getMinTargets(sa.getSourceCard(), sa) || abTgt.getNumTargeted() == 0){
						abTgt.resetTargets();
						return false;
					}
					else{
						// todo is this good enough? for up to amounts?
						break;
					}
				}
				
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
				 
				if (choice == null){	// can't find anything left
					if (abTgt.getNumTargeted() < abTgt.getMinTargets(sa.getSourceCard(), sa) || abTgt.getNumTargeted() == 0){
						abTgt.resetTargets();
						return false;
					}
					else{
						// todo is this good enough? for up to amounts?
						break;
					}
				}
				list.remove(choice);
				abTgt.addTarget(choice);
			}
		 }
		 else{
			// Placeholder: No targeting necessary
			 int currCounters = sa.getSourceCard().getCounters(Counters.valueOf(type));
			// each counter on the card is a 10% chance of not activating this ability. 
			 if (r.nextFloat() < .1 * currCounters)	
				 return false;
		 }
		 
		 Ability_Sub subAb = sa.getSubAbility();
		 if (subAb != null)
			 chance &= subAb.chkAI_Drawback();
		 
		 return ((r.nextFloat() < .6667) && chance);
	}
	
	public static boolean putPlayDrawbackAI(final AbilityFactory af, final SpellAbility sa){
		boolean chance = true;
		Target abTgt = sa.getTarget();
		final Card source = sa.getSourceCard();
		CardList list;
		Card choice = null;
		String type = af.getMapParams().get("CounterType");
		String amountStr = af.getMapParams().get("CounterNum");
		final int amount = AbilityFactory.calculateAmount(af.getHostCard(), amountStr, sa);
		
		Player player = af.isCurse() ? AllZone.HumanPlayer : AllZone.ComputerPlayer;
		
		list = new CardList(AllZone.getZone(Constant.Zone.Battlefield, player).getCards());
		list = list.filter(new CardListFilter() {
			public boolean addCard(Card c) {
				return CardFactoryUtil.canTarget(source, c);
			}
		});
		
		if (abTgt != null){
			list = list.getValidCards(abTgt.getValidTgts(),source.getController(),source);

			if (list.size() == 0)
				return false;

			 abTgt.resetTargets();
			 // target loop
			while(abTgt.getNumTargeted() < abTgt.getMaxTargets(sa.getSourceCard(), sa)){ 
				if (list.size() == 0){
					if (abTgt.getNumTargeted() < abTgt.getMinTargets(sa.getSourceCard(), sa) || abTgt.getNumTargeted() == 0){
						abTgt.resetTargets();
						return false;
					}
					else{
						break;
					}
				}
				
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
				 
				if (choice == null){	// can't find anything left
					if (abTgt.getNumTargeted() < abTgt.getMinTargets(sa.getSourceCard(), sa) || abTgt.getNumTargeted() == 0){
						abTgt.resetTargets();
						return false;
					}
					else{
						// todo is this good enough? for up to amounts?
						break;
					}
				}
				list.remove(choice);
				abTgt.addTarget(choice);
			}
		 }
		
		
		 Ability_Sub subAb = sa.getSubAbility();
		 if (subAb != null)
			 chance &= subAb.chkAI_Drawback();
		 
		 return chance;
	}

	public static void putResolve(final AbilityFactory af, final SpellAbility sa){
		HashMap<String,String> params = af.getMapParams();
		String DrawBack = params.get("SubAbility");
		Card card = af.getHostCard();
		String type = params.get("CounterType");
		int counterAmount = AbilityFactory.calculateAmount(af.getHostCard(), params.get("CounterNum"), sa);
		
		ArrayList<Card> tgtCards;

		Target tgt = af.getAbTgt();
		if (tgt != null)
			tgtCards = tgt.getTargetCards();
		else{
			tgtCards = new ArrayList<Card>();
			tgtCards.add(card);
		}
		
		for(Card tgtCard : tgtCards)
			if(AllZone.GameAction.isCardInPlay(tgtCard) && (tgt == null || CardFactoryUtil.canTarget(card, tgtCard)))
				tgtCard.addCounter(Counters.valueOf(type), counterAmount);
		
		if (af.hasSubAbility()){
			Ability_Sub abSub = sa.getSubAbility();
			if (abSub != null){
			   abSub.resolve();
			}
			else
				CardFactoryUtil.doDrawBack(DrawBack, counterAmount, card.getController(), card.getController().getOpponent(), card.getController(), card, null, sa);
		}
	}

	// *******************************************
	// ********** RemoveCounters *****************
	// *******************************************
	
	public static SpellAbility createAbilityRemoveCounters(final AbilityFactory AF){

		final SpellAbility abRemCounter = new Ability_Activated(AF.getHostCard(), AF.getAbCost(), AF.getAbTgt()){
			private static final long serialVersionUID = 8581011868395954121L;
			
			final AbilityFactory af = AF;
		
			@Override
			public String getStackDescription(){
				return removeStackDescription(af, this);
			}
			
			public boolean canPlayAI()
			{
				return removeCanPlayAI(af, this);
			}
			
			@Override
			public void resolve() {
				removeResolve(af, this);
			}
			
		};
		return abRemCounter;
	}
	
	public static SpellAbility createSpellRemoveCounters(final AbilityFactory AF){
		final SpellAbility spRemoveCounter = new Spell(AF.getHostCard(), AF.getAbCost(), AF.getAbTgt()){
			private static final long serialVersionUID = -5065591869141835456L;
			
			final AbilityFactory af = AF;
		
			@Override
			public String getStackDescription(){
				return removeStackDescription(af, this);
			}

			public boolean canPlayAI()
			{
				// if X depends on abCost, the AI needs to choose which card he would sacrifice first
				// then call xCount with that card to properly calculate the amount
				// Or choosing how many to sacrifice 
				return removeCanPlayAI(af, this);
			}
			
			@Override
			public void resolve() {
				removeResolve(af, this);
			}
			
		};
		return spRemoveCounter;
	}
	
	public static SpellAbility createDrawbackRemoveCounters(final AbilityFactory AF){
		final SpellAbility spRemoveCounter = new Ability_Sub(AF.getHostCard(), AF.getAbTgt()){
			private static final long serialVersionUID = -5065591869141835456L;
			
			final AbilityFactory af = AF;
		
			@Override
			public String getStackDescription(){
				return removeStackDescription(af, this);
			}
			
			@Override
			public void resolve() {
				removeResolve(af, this);
			}

			@Override
			public boolean chkAI_Drawback() {
				return removePlayDrawbackAI(af, this);
			}
			
		};
		return spRemoveCounter;
	}
	
	public static String removeStackDescription(AbilityFactory af, SpellAbility sa){
		StringBuilder sb = new StringBuilder();

		if (!(sa instanceof Ability_Sub))
			sb.append(sa.getSourceCard().getName()).append(" - ");
		else
			sb.append(" ");

		Counters cType = Counters.valueOf(af.getMapParams().get("CounterType"));
		String name = af.getHostCard().getName();
		int amount = AbilityFactory.calculateAmount(af.getHostCard(), af.getMapParams().get("CounterNum"), sa);

		sb.append("Remove ").append(amount).append(" ").append(cType.getName())
				.append(" counter on");

		if (af.getAbTgt() == null)
			sb.append(" ").append(name);
		else {
			ArrayList<Card> tgts = af.getAbTgt().getTargetCards();
			for (Card c : tgts)
				sb.append(" ").append(c.getName());
		}

		sb.append(".");
		
		Ability_Sub abSub = sa.getSubAbility();
        if (abSub != null){
        	sb.append(abSub.getStackDescription());
        }
		
		return sb.toString();
	}
	
	public static boolean removeCanPlayAI(final AbilityFactory af, final SpellAbility sa){
		// AI needs to be expanded, since this function can be pretty complex based on what the expected targets could be
		Random r = new Random();
		Ability_Cost abCost = sa.getPayCosts();
		//Target abTgt = sa.getTarget();
		final Card source = sa.getSourceCard();
		//CardList list;
		//Card choice = null;
		
		String type = af.getMapParams().get("CounterType");
		//String amountStr = af.getMapParams().get("CounterNum");
		
		//TODO - currently, not targeted, only for Self
		
		//Player player = af.isCurse() ? AllZone.HumanPlayer : AllZone.ComputerPlayer;
		
		
		if (abCost != null){
			// AI currently disabled for these costs
			if (abCost.getSacCost()){ 
				return false;
			}
			if (abCost.getLifeCost())	 return false;
			if (abCost.getDiscardCost()) return false;
			
			if (abCost.getSubCounter()){
				// A card has a 25% chance per counter to be able to pass through here
				// 4+ counters will always pass. 0 counters will never
				int currentNum = source.getCounters(abCost.getCounterType());
				double percent = .25 * (currentNum / abCost.getCounterNum());
				if (percent <= r.nextFloat())
					return false;
			}
		}
		
		if (!ComputerUtil.canPayCost(sa))
			return false;
		
		// TODO handle proper calculation of X values based on Cost
		//final int amount = calculateAmount(af.getHostCard(), amountStr, sa);
		
		 // prevent run-away activations - first time will always return true
		 boolean chance = r.nextFloat() <= Math.pow(.6667, source.getAbilityUsed());
		 
		 //currently, not targeted

		 // Placeholder: No targeting necessary
		 int currCounters = sa.getSourceCard().getCounters(Counters.valueOf(type));
		 // each counter on the card is a 10% chance of not activating this ability. 
		 if (r.nextFloat() < .1 * currCounters)	
			 return false;
		 
		 Ability_Sub subAb = sa.getSubAbility();
		 if (subAb != null)
		 	chance &= subAb.chkAI_Drawback();

		 return ((r.nextFloat() < .6667) && chance);
	}
	
	public static boolean removePlayDrawbackAI(final AbilityFactory af, final SpellAbility sa){
		// AI needs to be expanded, since this function can be pretty complex based on what the expected targets could be
		//Target abTgt = sa.getTarget();
		//final Card source = sa.getSourceCard();
		//CardList list;
		//Card choice = null;
		
		//String type = af.getMapParams().get("CounterType");
		//String amountStr = af.getMapParams().get("CounterNum");
		
		//TODO - currently, not targeted, only for Self
		
		//Player player = af.isCurse() ? AllZone.HumanPlayer : AllZone.ComputerPlayer;
		
		// TODO handle proper calculation of X values based on Cost
		//final int amount = calculateAmount(af.getHostCard(), amountStr, sa);
		
		 // prevent run-away activations - first time will always return true
		 boolean chance = true;
		 
		 //currently, not targeted
		 
		 Ability_Sub subAb = sa.getSubAbility();
		 if (subAb != null)
		 	chance &= subAb.chkAI_Drawback();

		 return chance;
	}
	
	public static void removeResolve(final AbilityFactory af, final SpellAbility sa){
		HashMap<String,String> params = af.getMapParams();
		String type = params.get("CounterType");
		int counterAmount = AbilityFactory.calculateAmount(af.getHostCard(), params.get("CounterNum"), sa);
		
		String DrawBack = params.get("SubAbility");
		Card card = af.getHostCard();
		
		ArrayList<Card> tgtCards;

		Target tgt = af.getAbTgt();
		if (tgt != null)
			tgtCards = tgt.getTargetCards();
		else{
			tgtCards = new ArrayList<Card>();
			tgtCards.add(card);
		}
		
		for(Card tgtCard : tgtCards)
			if(AllZone.GameAction.isCardInPlay(tgtCard) && (tgt == null || CardFactoryUtil.canTarget(card, tgtCard)))
				tgtCard.subtractCounter(Counters.valueOf(type), counterAmount);
		
		if (af.hasSubAbility()){
			Ability_Sub abSub = sa.getSubAbility();
			if (abSub != null){
			   abSub.resolve();
			}
			else
				CardFactoryUtil.doDrawBack(DrawBack, counterAmount, card.getController(), card.getController().getOpponent(), card.getController(), card, null, sa);
		}
	}
	
	// *******************************************
	// ********** Proliferate ********************
	// *******************************************
	// For Contagion Engine do Proliferate with SubAbility$Proliferate
	
	public static SpellAbility createAbilityProliferate(final AbilityFactory AF) {
		final SpellAbility abProliferate = new Ability_Activated(AF.getHostCard(), AF.getAbCost(), AF.getAbTgt()) {
    		private static final long serialVersionUID = -6617234927365102930L;

			@Override
			public boolean canPlayAI() {
    			return shouldProliferateAI(this);
    		}
			
    		@Override
			public void resolve() {
    			proliferateResolve(AF, this);
    		}
    		
    		@Override
    		public boolean canPlay(){
				// super takes care of AdditionalCosts
				return super.canPlay();	
			}
    		
    		@Override
			public String getStackDescription(){
    			return proliferateStackDescription(this);
			}
    	};
    	
    	return abProliferate;
    }
	
	public static SpellAbility createSpellProliferate(final AbilityFactory AF) {
		final SpellAbility spProliferate = new Spell(AF.getHostCard(), AF.getAbCost(), AF.getAbTgt()) {
			private static final long serialVersionUID = 1265466498444897146L;

			@Override
			public boolean canPlayAI() {
    			return shouldProliferateAI(this);
    		}
			
    		@Override
			public void resolve() {
    			proliferateResolve(AF, this);
    		}
    		
    		@Override
    		public boolean canPlay(){
				// super takes care of AdditionalCosts
				return super.canPlay();	
			}
    		
    		@Override
			public String getStackDescription(){
    			return proliferateStackDescription(this);
			}
    	};
    	
    	return spProliferate;
    }
	
	public static SpellAbility createDrawbackProliferate(final AbilityFactory AF) {
		final SpellAbility dbProliferate = new Ability_Sub(AF.getHostCard(), AF.getAbTgt()) {
			private static final long serialVersionUID = 1265466498444897146L;

			@Override
			public boolean canPlayAI() {
    			return shouldProliferateAI(this);
    		}
			
    		@Override
			public void resolve() {
    			proliferateResolve(AF, this);
    		}
    		
    		@Override
			public String getStackDescription(){
    			return proliferateStackDescription(this);
			}

			@Override
			public boolean chkAI_Drawback() {
				return shouldProliferateAI(this);
			}
    	};
    	
    	return dbProliferate;
    }
	
	private static String proliferateStackDescription(SpellAbility sa){
		StringBuilder sb = new StringBuilder();
		if (!(sa instanceof Ability_Sub))
			sb.append(sa.getSourceCard().getName()).append(" - ");
		else
			sb.append(" ");
		sb.append("Proliferate.");
		sb.append(" (You choose any number of permanents and/or players with ");
		sb.append("counters on them, then give each another counter of a kind already there.)");
		
		Ability_Sub abSub = sa.getSubAbility();
        if (abSub != null){
        	sb.append(abSub.getStackDescription());
        }
		
		return sb.toString();
	}
	
	private static boolean shouldProliferateAI(SpellAbility sa) {
		boolean chance = true;
		Ability_Sub subAb = sa.getSubAbility();
		if (subAb != null)
			chance &= subAb.chkAI_Drawback();
		
		// todo: Make sure Human has poison counters or there are some counters we want to proliferate
		return chance;
	}
	
	private static void proliferateResolve(final AbilityFactory AF, SpellAbility sa) {
		CardList hperms = AllZoneUtil.getPlayerCardsInPlay(AllZone.HumanPlayer);
		hperms = hperms.filter(new CardListFilter() {
			public boolean addCard(Card crd)
			{
				return !crd.getName().equals("Mana Pool") /*&& crd.hasCounters()*/;
			}
		});
		
		CardList cperms = AllZoneUtil.getPlayerCardsInPlay(AllZone.ComputerPlayer);
		cperms = cperms.filter(new CardListFilter() {
			public boolean addCard(Card crd)
			{
				return !crd.getName().equals("Mana Pool") /*&& crd.hasCounters()*/;
			}
		});
		
		if (AF.getHostCard().getController().equals(AllZone.HumanPlayer)) {	
			cperms.addAll(hperms.toArray());
			final CardList unchosen = cperms;
			AllZone.InputControl.setInput(new Input() {
				private static final long serialVersionUID = -1779224307654698954L;

				@Override
				public void showMessage() {
					AllZone.Display.showMessage("Choose permanents and/or players");
					ButtonUtil.enableOnlyOK();
				}

				@Override
				public void selectButtonOK() {
					stop();
				}

				@Override
				public void selectCard(Card card, PlayerZone zone)
				{
					if(!unchosen.contains(card)) return;
					unchosen.remove(card);
					ArrayList<String> choices = new ArrayList<String>();
					for(Counters c_1:Counters.values())
						if(card.getCounters(c_1) != 0) choices.add(c_1.getName());
					if (choices.size() > 0)
						card.addCounter(Counters.getType((choices.size() == 1 ? choices.get(0) : AllZone.Display.getChoice("Select counter type", choices.toArray()).toString())), 1);
				}
				boolean selComputer = false;
				boolean selHuman = false;
				@Override
				public void selectPlayer(Player player) {
					if (player.equals(AllZone.HumanPlayer) && selHuman == false) {
						selHuman = true;
						if (AllZone.HumanPlayer.getPoisonCounters() > 0)
							AllZone.HumanPlayer.addPoisonCounters(1);
					}
					if (player.equals(AllZone.ComputerPlayer) && selComputer == false) {
						selComputer = true;
						if (AllZone.ComputerPlayer.getPoisonCounters() > 0)
							AllZone.ComputerPlayer.addPoisonCounters(1);
					}
				}
			});
		}
		else { //Compy
			cperms = cperms.filter(new CardListFilter() {
				public boolean addCard(Card crd) {
					int pos = 0;
					int neg = 0;
					for(Counters c_1:Counters.values()) {
                        if(crd.getCounters(c_1) != 0) {
                        	if (CardFactoryUtil.isNegativeCounter(c_1))
                        		neg++;
                        	else
                        		pos++;
                        }
					}
					return pos > neg;
				}
			});
			
			hperms = hperms.filter(new CardListFilter() {
				public boolean addCard(Card crd) {
					int pos = 0;
					int neg = 0;
					for(Counters c_1:Counters.values()) {
                        if(crd.getCounters(c_1) != 0) {
                        	if (CardFactoryUtil.isNegativeCounter(c_1))
                        		neg++;
                        	else
                        		pos++;
                        }
					}
					return pos < neg;
				}
			});
			
			StringBuilder sb = new StringBuilder();
			sb.append("<html>Proliferate: <br>Computer selects ");
			if (cperms.size() == 0 && hperms.size() == 0 && AllZone.HumanPlayer.getPoisonCounters() == 0)
				sb.append("<b>nothing</b>.");
			else {
				if (cperms.size()>0) {
					sb.append("<br>From Computer's permanents: <br><b>");
					for (Card c:cperms) {
						sb.append(c);
						sb.append(" ");
					}
					sb.append("</b><br>");
				}
				if (hperms.size()>0) {
					sb.append("<br>From Human's permanents: <br><b>");
					for (Card c:cperms) {
						sb.append(c);
						sb.append(" ");
					}
					sb.append("</b><br>");
				}
				if (AllZone.HumanPlayer.getPoisonCounters() > 0)
					sb.append("<b>Human Player</b>.");
			}//else
			sb.append("</html>");
			
			
			//add a counter for each counter type, if it would benefit the computer
			for (Card c:cperms) {
				for(Counters c_1:Counters.values())
                    if(c.getCounters(c_1) != 0) c.addCounter(c_1, 1);
			}
			
			//add a counter for each counter type, if it would screw over the player
			for (Card c:hperms) {
				for(Counters c_1:Counters.values())
                    if(c.getCounters(c_1) != 0) c.addCounter(c_1, 1);
			}
			
			//give human a poison counter, if he has one
			if (AllZone.HumanPlayer.getPoisonCounters() > 0)
        		AllZone.HumanPlayer.addPoisonCounters(1);
			
		} //comp
		
		if (AF.hasSubAbility()){
			Ability_Sub abSub = sa.getSubAbility();
			if (abSub != null){
			   abSub.resolve();
			}
		}
	}
}
