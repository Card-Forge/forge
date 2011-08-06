package forge;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

public class AbilityFactory_PermanentState {
	// Untapping
	public static SpellAbility createAbilityUntap(final AbilityFactory AF){
		final SpellAbility abUntap = new Ability_Activated(AF.getHostCard(), AF.getAbCost(), AF.getAbTgt()){
			private static final long serialVersionUID = 5445572699000471299L;
			
			final AbilityFactory af = AF;
			
			@Override
			public String getStackDescription(){
				return untapStackDescription(af, this);
			}
			
			public boolean canPlayAI()
			{
				return untapCanPlayAI(af,this);
			}
			
			@Override
			public void resolve() {
				untapResolve(af, this);
			}
			
		};
		return abUntap;
	}
	
	public static SpellAbility createSpellUntap(final AbilityFactory AF){
		final SpellAbility spUntap = new Spell(AF.getHostCard(), AF.getAbCost(), AF.getAbTgt()){
			private static final long serialVersionUID = -4990932993654533449L;
			
			final AbilityFactory af = AF;
			
			@Override
			public String getStackDescription(){
				return untapStackDescription(af, this);
			}

			public boolean canPlayAI()
			{
				return untapCanPlayAI(af, this);
			}
			
			@Override
			public void resolve() {
				untapResolve(af, this);
			}
			
		};
		return spUntap;
	}
	
	public static SpellAbility createDrawbackUntap(final AbilityFactory AF){
		final SpellAbility dbUntap = new Ability_Sub(AF.getHostCard(), AF.getAbTgt()){
			private static final long serialVersionUID = -4990932993654533449L;
			
			final AbilityFactory af = AF;
			
			@Override
			public String getStackDescription(){
				return untapStackDescription(af, this);
			}
			
			@Override
			public void resolve() {
				untapResolve(af, this);
			}

			@Override
			public boolean chkAI_Drawback() {
				return untapPlayDrawbackAI(af, this);
			}
			
		};
		return dbUntap;
	}

	public static String untapStackDescription(AbilityFactory af, SpellAbility sa){
		// when getStackDesc is called, just build exactly what is happening
		 StringBuilder sb = new StringBuilder();
		 
		 if (sa instanceof Ability_Sub)
			 sb.append(" ");
		 else
			 sb.append(sa.getSourceCard()).append(" - ");
		 
		 sb.append("Untap ");

		ArrayList<Card> tgtCards;
		Target tgt = af.getAbTgt();
		if (tgt != null)
			tgtCards = tgt.getTargetCards();
		else{
			tgtCards = new ArrayList<Card>();
			tgtCards.add(af.getHostCard());	
		}
		
		for(Card c : tgtCards)
			sb.append(c.getName()).append(" ");

        Ability_Sub subAb = sa.getSubAbility();
        if (subAb != null)
        	sb.append(subAb.getStackDescription());
		
		 return sb.toString();
	}
	
	public static boolean untapCanPlayAI(final AbilityFactory af, SpellAbility sa){
		// AI cannot use this properly until he can use SAs during Humans turn
		if (!ComputerUtil.canPayCost(sa))
			return false;
		
		Target tgt = af.getAbTgt();
		Card source = sa.getSourceCard();
		
		Random r = new Random();
		boolean randomReturn = r.nextFloat() <= Math.pow(.6667, source.getAbilityUsed());
		
		if (tgt == null){
			if (sa.getSourceCard().isUntapped())
				return false;
		}
		else{
			tgt.resetTargets();
			CardList untapList = AllZoneUtil.getPlayerCardsInPlay(AllZone.ComputerPlayer);
			untapList = untapList.filter(AllZoneUtil.tapped);
			untapList = untapList.getValidCards(tgt.getValidTgts(), source.getController(), source);
			// filter out enchantments and planeswalkers, their tapped state doesn't matter.
			String[] tappablePermanents = {"Creature", "Land", "Artifact"}; 
			untapList = untapList.getValidCards(tappablePermanents, source.getController(), source);

			if (untapList.size() == 0)
				return false;
			
			while(tgt.getNumTargeted() < tgt.getMaxTargets(sa.getSourceCard(), sa)){ 
				Card choice = null;
				
				if (untapList.size() == 0){
					if (tgt.getNumTargeted() < tgt.getMinTargets(sa.getSourceCard(), sa) || tgt.getNumTargeted() == 0){
						tgt.resetTargets();
						return false;
					}
					else{
						// todo is this good enough? for up to amounts?
						break;
					}
				}
				
				if (untapList.getNotType("Creature").size() == 0)
	        		choice = CardFactoryUtil.AI_getBestCreature(untapList); //if only creatures take the best
	        	else
	        		choice = CardFactoryUtil.AI_getMostExpensivePermanent(untapList, af.getHostCard(), false);
				
				if (choice == null){	// can't find anything left
					if (tgt.getNumTargeted() < tgt.getMinTargets(sa.getSourceCard(), sa) || tgt.getNumTargeted() == 0){
						tgt.resetTargets();
						return false;
					}
					else{
						// todo is this good enough? for up to amounts?
						break;
					}
				}
				
				untapList.remove(choice);
				tgt.addTarget(choice);
			}
		}
		
        Ability_Sub subAb = sa.getSubAbility();
        if (subAb != null)
        	randomReturn &= subAb.chkAI_Drawback();
		
		return randomReturn;
	}
	
	public static boolean untapPlayDrawbackAI(final AbilityFactory af, SpellAbility sa){
		// AI cannot use this properly until he can use SAs during Humans turn
		Target tgt = af.getAbTgt();
		Card source = sa.getSourceCard();

		boolean randomReturn = true;
		
		if (tgt == null){
			// who cares if its already untapped, it's only a subability?
		}
		else{
			CardList untapList = AllZoneUtil.getPlayerCardsInPlay(AllZone.ComputerPlayer);
			untapList = untapList.filter(AllZoneUtil.tapped);
			untapList = untapList.getValidCards(tgt.getValidTgts(), source.getController(), source);
			// filter out enchantments and planeswalkers, their tapped state doesn't matter.
			String[] tappablePermanents = {"Creature", "Land", "Artifact"}; 
			untapList = untapList.getValidCards(tappablePermanents, source.getController(), source);

			if (untapList.size() == 0)
				return false;
			
			while(tgt.getNumTargeted() < tgt.getMaxTargets(sa.getSourceCard(), sa)){ 
				Card choice = null;
				
				if (untapList.size() == 0){
					if (tgt.getNumTargeted() < tgt.getMinTargets(sa.getSourceCard(), sa) || tgt.getNumTargeted() == 0){
						tgt.resetTargets();
						return false;
					}
					else{
						// todo is this good enough? for up to amounts?
						break;
					}
				}
				
				if (untapList.getNotType("Creature").size() == 0)
	        		choice = CardFactoryUtil.AI_getBestCreature(untapList); //if only creatures take the best
	        	else
	        		choice = CardFactoryUtil.AI_getMostExpensivePermanent(untapList, af.getHostCard(), false);
				
				if (choice == null){	// can't find anything left
					if (tgt.getNumTargeted() < tgt.getMinTargets(sa.getSourceCard(), sa) || tgt.getNumTargeted() == 0){
						tgt.resetTargets();
						return false;
					}
					else{
						// todo is this good enough? for up to amounts?
						break;
					}
				}
				
				untapList.remove(choice);
				tgt.addTarget(choice);
			}
		}
		
        Ability_Sub subAb = sa.getSubAbility();
        if (subAb != null)
        	randomReturn &= subAb.chkAI_Drawback();
		
		return randomReturn;
	}
	
	public static void untapResolve(final AbilityFactory af, final SpellAbility sa){
		HashMap<String,String> params = af.getMapParams();
		Card card = sa.getSourceCard();
		
		ArrayList<Card> tgtCards;
		Target tgt = af.getAbTgt();
		if (tgt != null)
			tgtCards = tgt.getTargetCards();
		else{
			tgtCards = new ArrayList<Card>();
			tgtCards.add(card);	
		}

		for(Card tgtC : tgtCards){
			if (AllZone.GameAction.isCardInPlay(tgtC) && (tgt == null || CardFactoryUtil.canTarget(af.getHostCard(), tgtC)))
				tgtC.untap();
		}

		if (af.hasSubAbility()){
			Ability_Sub abSub = sa.getSubAbility();
			if (abSub != null){
	     	   abSub.resolve();
	        }
	        else{
	        	Card c = tgtCards.get(0);
				String DrawBack = params.get("SubAbility");
				if (af.hasSubAbility())
					 CardFactoryUtil.doDrawBack(DrawBack, 0, card.getController(), card.getController().getOpponent(), card.getController(), card, c, sa);
	        }
		}
	}
	// ****************************************
	// ************** Tapping *****************
	// ****************************************
	
	public static SpellAbility createAbilityTap(final AbilityFactory AF){
		final SpellAbility abTap = new Ability_Activated(AF.getHostCard(), AF.getAbCost(), AF.getAbTgt()){
			private static final long serialVersionUID = 5445572699000471299L;
			
			final AbilityFactory af = AF;
			
			@Override
			public String getStackDescription(){
				return tapStackDescription(af, this);
			}
			
			public boolean canPlayAI()
			{
				return tapCanPlayAI(af,this);
			}
			
			@Override
			public void resolve() {
				tapResolve(af, this);
			}
			
		};
		return abTap;
	}
	
	public static SpellAbility createSpellTap(final AbilityFactory AF){
		final SpellAbility spTap = new Spell(AF.getHostCard(), AF.getAbCost(), AF.getAbTgt()){
			private static final long serialVersionUID = -4990932993654533449L;
			
			final AbilityFactory af = AF;
			
			@Override
			public String getStackDescription(){
				return tapStackDescription(af, this);
			}
			
			public boolean canPlayAI()
			{
				return tapCanPlayAI(af, this);
			}
			
			@Override
			public void resolve() {
				tapResolve(af, this);
			}
			
		};
		return spTap;
	}
	
	public static SpellAbility createDrawbackTap(final AbilityFactory AF){
		final SpellAbility dbTap = new Ability_Sub(AF.getHostCard(), AF.getAbTgt()){
			private static final long serialVersionUID = -4990932993654533449L;
			
			final AbilityFactory af = AF;
			
			@Override
			public String getStackDescription(){
				return tapStackDescription(af, this);
			}
			
			@Override
			public void resolve() {
				tapResolve(af, this);
			}

			@Override
			public boolean chkAI_Drawback() {
				return tapPlayDrawbackAI(af, this);
			}
			
		};
		return dbTap;
	}
	
	public static String tapStackDescription(AbilityFactory af, SpellAbility sa){
		// when getStackDesc is called, just build exactly what is happening
		 StringBuilder sb = new StringBuilder();
		 
		 if (sa instanceof Ability_Sub)
			 sb.append(" ");
		 else
			 sb.append(sa.getSourceCard()).append(" - ");
		 
		 sb.append("Tap ");
		 
		ArrayList<Card> tgtCards;
		Target tgt = af.getAbTgt();
		if (tgt != null)
			tgtCards = tgt.getTargetCards();
		else{
			tgtCards = new ArrayList<Card>();
			tgtCards.add(af.getHostCard());	
		}
		
		for(Card c : tgtCards)
			sb.append(c.getName()).append(" ");

        Ability_Sub subAb = sa.getSubAbility();
        if (subAb != null)
        	sb.append(subAb.getStackDescription());
		
		 return sb.toString();
	}
	
	public static boolean tapCanPlayAI(final AbilityFactory af, SpellAbility sa){
		// AI cannot use this properly until he can use SAs during Humans turn
		if (!ComputerUtil.canPayCost(sa))
			return false;
		
		Target tgt = af.getAbTgt();
		Card source = sa.getSourceCard();
		
		Random r = new Random();
		boolean randomReturn = r.nextFloat() <= Math.pow(.6667, source.getAbilityUsed());
		
		if (tgt == null){
			if (sa.getSourceCard().isTapped())
				return false;
		}
		else{
			tgt.resetTargets();
			CardList tapList = AllZoneUtil.getPlayerCardsInPlay(AllZone.HumanPlayer);
			tapList = tapList.filter(AllZoneUtil.untapped);
			tapList = tapList.getValidCards(tgt.getValidTgts(), source.getController(), source);
			// filter out enchantments and planeswalkers, their tapped state doesn't matter.
			String[] tappablePermanents = {"Creature", "Land", "Artifact"}; 
			tapList = tapList.getValidCards(tappablePermanents, source.getController(), source);

			if (tapList.size() == 0)
				return false;
			
			while(tgt.getNumTargeted() < tgt.getMaxTargets(sa.getSourceCard(), sa)){ 
				Card choice = null;
				
				if (tapList.size() == 0){
					if (tgt.getNumTargeted() < tgt.getMinTargets(sa.getSourceCard(), sa) || tgt.getNumTargeted() == 0){
						tgt.resetTargets();
						return false;
					}
					else{
						// todo is this good enough? for up to amounts?
						break;
					}
				}
				
				if (tapList.getNotType("Creature").size() == 0)
	        		choice = CardFactoryUtil.AI_getBestCreature(tapList); //if only creatures take the best
	        	else
	        		choice = CardFactoryUtil.AI_getMostExpensivePermanent(tapList, af.getHostCard(), false);
				
				if (choice == null){	// can't find anything left
					if (tgt.getNumTargeted() < tgt.getMinTargets(sa.getSourceCard(), sa) || tgt.getNumTargeted() == 0){
						tgt.resetTargets();
						return false;
					}
					else{
						// todo is this good enough? for up to amounts?
						break;
					}
				}
				
				tapList.remove(choice);
				tgt.addTarget(choice);
			}
		}
		
        Ability_Sub subAb = sa.getSubAbility();
        if (subAb != null)
        	randomReturn &= subAb.chkAI_Drawback();
		
		return randomReturn;
	}
	
	public static boolean tapPlayDrawbackAI(final AbilityFactory af, SpellAbility sa){
		// AI cannot use this properly until he can use SAs during Humans turn
		Target tgt = af.getAbTgt();
		Card source = sa.getSourceCard();
		
		boolean randomReturn = true;
		
		if (tgt == null){
			// who cares if its already tapped, it's only a subability?
		}
		else{
			// target section, maybe pull this out?
			CardList tapList = AllZoneUtil.getPlayerCardsInPlay(AllZone.ComputerPlayer);
			tapList = tapList.filter(AllZoneUtil.untapped);
			tapList = tapList.getValidCards(tgt.getValidTgts(), source.getController(), source);
			// filter out enchantments and planeswalkers, their tapped state doesn't matter.
			String[] tappablePermanents = {"Creature", "Land", "Artifact"}; 
			tapList = tapList.getValidCards(tappablePermanents, source.getController(), source);

			if (tapList.size() == 0)
				return false;
			
			while(tgt.getNumTargeted() < tgt.getMaxTargets(sa.getSourceCard(), sa)){ 
				Card choice = null;
				
				if (tapList.size() == 0){
					if (tgt.getNumTargeted() < tgt.getMinTargets(sa.getSourceCard(), sa) || tgt.getNumTargeted() == 0){
						tgt.resetTargets();
						return false;
					}
					else{
						// todo is this good enough? for up to amounts?
						break;
					}
				}
				
				if (tapList.getNotType("Creature").size() == 0)
	        		choice = CardFactoryUtil.AI_getBestCreature(tapList); //if only creatures take the best
	        	else
	        		choice = CardFactoryUtil.AI_getMostExpensivePermanent(tapList, af.getHostCard(), false);
				
				if (choice == null){	// can't find anything left
					if (tgt.getNumTargeted() < tgt.getMinTargets(sa.getSourceCard(), sa) || tgt.getNumTargeted() == 0){
						tgt.resetTargets();
						return false;
					}
					else{
						// todo is this good enough? for up to amounts?
						break;
					}
				}
				
				tapList.remove(choice);
				tgt.addTarget(choice);
			}
		}
		
        Ability_Sub subAb = sa.getSubAbility();
        if (subAb != null)
        	randomReturn &= subAb.chkAI_Drawback();
		
		return randomReturn;
	}
	
	public static void tapResolve(final AbilityFactory af, final SpellAbility sa){
		HashMap<String,String> params = af.getMapParams();
		Card card = sa.getSourceCard();
		
		ArrayList<Card> tgtCards;
		Target tgt = af.getAbTgt();
		if (tgt != null)
			tgtCards = tgt.getTargetCards();
		else{
			tgtCards = new ArrayList<Card>();
			tgtCards.add(card);
		}

		for(Card tgtC : tgtCards){
			if (AllZone.GameAction.isCardInPlay(tgtC) && (tgt == null || CardFactoryUtil.canTarget(af.getHostCard(), tgtC)))
				tgtC.tap();
		}
		
		if (af.hasSubAbility()){
			Ability_Sub abSub = sa.getSubAbility();
			if (abSub != null){
	     	   abSub.resolve();
	        }
	        else{
	    		Card c = tgtCards.get(0);
	    		String DrawBack = params.get("SubAbility");
	    		if (af.hasSubAbility())
	    			 CardFactoryUtil.doDrawBack(DrawBack, 0, card.getController(), card.getController().getOpponent(), card.getController(), card, c, sa);
	        }
		}
	}
	
	public static SpellAbility createAbilityUntapAll(final AbilityFactory AF){
		final SpellAbility abUntap = new Ability_Activated(AF.getHostCard(), AF.getAbCost(), AF.getAbTgt()){
			private static final long serialVersionUID = 8914852730903389831L;
			final AbilityFactory af = AF;

			@Override
			public String getStackDescription(){
				return untapAllStackDescription(af, this);
			}

			public boolean canPlayAI()
			{
				return untapAllCanPlayAI(af, this);
			}

			@Override
			public void resolve() {
				untapAllResolve(af, this);
			}

		};
		return abUntap;
	}

	public static SpellAbility createSpellUntapAll(final AbilityFactory AF){
		final SpellAbility spUntap = new Spell(AF.getHostCard(), AF.getAbCost(), AF.getAbTgt()){
			private static final long serialVersionUID = 5713174052551899363L;
			final AbilityFactory af = AF;

			@Override
			public String getStackDescription() {
				return untapAllStackDescription(af, this);
			}

			public boolean canPlayAI() {
				return untapAllCanPlayAI(af, this);
			}

			@Override
			public void resolve() {
				untapAllResolve(af, this);
			}

		};
		return spUntap;
	}

	private static void untapAllResolve(final AbilityFactory af, final SpellAbility sa) {
		HashMap<String,String> params = af.getMapParams();
		String DrawBack = params.get("SubAbility");
		Card card = sa.getSourceCard();

		String Valid = "";

		if(params.containsKey("ValidCards")) 
			Valid = params.get("ValidCards");

		CardList list = AllZoneUtil.getCardsInPlay();
		list = list.getValidCards(Valid.split(","), card.getController(), card);

		for(int i = 0; i < list.size(); i++) list.get(i).untap();

		if (af.hasSubAbility()) {
			Ability_Sub abSub = sa.getSubAbility();
			if(abSub != null) {
				abSub.resolve();
			}
			else {
				CardFactoryUtil.doDrawBack(DrawBack, 0, card.getController(), card.getController().getOpponent(), card.getController(), card, null, sa);
			}
		}
	}

	private static boolean untapAllCanPlayAI(final AbilityFactory af, final SpellAbility sa) {
		/*
		 * All cards using this currently have SVar:RemAIDeck:True
		 */
		return false;
	}

	private static String untapAllStackDescription(AbilityFactory af, SpellAbility sa){
		HashMap<String,String> params = af.getMapParams();
		// when getStackDesc is called, just build exactly what is happening
		StringBuilder sb = new StringBuilder();

		if (sa instanceof Ability_Sub) {
			sb.append(" ");
			sb.append("Untap all valid cards.");
		}
		else {
			sb.append(sa.getSourceCard()).append(" - ");
			sb.append(params.get("SpellDescription"));
		}

		Ability_Sub subAb = sa.getSubAbility();
		if (subAb != null)
			sb.append(subAb.getStackDescription());

		return sb.toString();
	}
	
	//Phasing? Something else? Who knows!
}
