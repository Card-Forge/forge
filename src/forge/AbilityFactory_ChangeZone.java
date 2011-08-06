package forge;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

public class AbilityFactory_ChangeZone {
	
	// Change Zone is going to work much differently than other AFs. 
	// *NOTE* Please do not use this as a base for copying and creating your own AF
	

	public static SpellAbility createAbilityChangeZone(final AbilityFactory AF){
		final SpellAbility abChangeZone = new Ability_Activated(AF.getHostCard(), AF.getAbCost(), AF.getAbTgt()){
			private static final long serialVersionUID = 3728332812890211671L;

			public boolean canPlayAI(){
				return changeZoneCanPlayAI(AF, this);
			}

			@Override
			public void resolve() {
				changeZoneResolve(AF, this);
			}
			
			@Override
			public String getStackDescription(){
				return changeZoneDescription(AF, this);
			}
		
		};
		setMiscellaneous(AF, abChangeZone);
		return abChangeZone;
	}
	
	public static SpellAbility createSpellChangeZone(final AbilityFactory AF){
		final SpellAbility spChangeZone = new Spell(AF.getHostCard(), AF.getAbCost(), AF.getAbTgt()) {
			private static final long serialVersionUID = 3270484211099902059L;

			public boolean canPlayAI(){
				return changeZoneCanPlayAI(AF, this);
			}
			
			@Override
			public void resolve() {
				changeZoneResolve(AF, this);
			}
			
			@Override
			public String getStackDescription(){
				return changeZoneDescription(AF, this);
			}
		};
		setMiscellaneous(AF, spChangeZone);
		return spChangeZone;
	}
	
	public static SpellAbility createDrawbackChangeZone(final AbilityFactory AF){
		final SpellAbility dbChangeZone = new Ability_Sub(AF.getHostCard(), AF.getAbTgt()) {
			private static final long serialVersionUID = 3270484211099902059L;

			@Override
			public void resolve() {
				changeZoneResolve(AF, this);
			}

			@Override
			public boolean chkAI_Drawback() {
				return changeZonePlayDrawbackAI(AF, this);
			}
			
			@Override
			public String getStackDescription(){
				return changeZoneDescription(AF, this);
			}
		};
		setMiscellaneous(AF, dbChangeZone);
		return dbChangeZone;
	}
	
	private static void setMiscellaneous(AbilityFactory af, SpellAbility sa){
		// todo: if moving to or from Battlefield allow use in Main1?
		HashMap<String,String> params = af.getMapParams();
		String origin = params.get("Origin");
		
		Target tgt  = af.getAbTgt();
		
		// Don't set the zone if it targets a player
		if (tgt != null && !tgt.canTgtPlayer())
			af.getAbTgt().setZone(origin);
		
		if (!(sa instanceof Ability_Sub))
			if (origin.equals("Battlefield") || params.get("Destination").equals("Battlefield"))
				af.getHostCard().setSVar("PlayMain1", "TRUE");
	}
	
	private static boolean changeZoneCanPlayAI(AbilityFactory af, SpellAbility sa){
		HashMap<String,String> params = af.getMapParams();
		String origin = params.get("Origin");

		if (origin.equals("Library") || origin.equals("Hand") || origin.equals("Sideboard")){
			return changeHiddenOriginCanPlayAI(af, sa);
		}
		else if (origin.equals("Graveyard") || origin.equals("Exile") || origin.equals("Battlefield")){
			return changeKnownOriginCanPlayAI(af, sa);
		}
		return false;
	}
	
	private static boolean changeZonePlayDrawbackAI(AbilityFactory af, SpellAbility sa){
		HashMap<String,String> params = af.getMapParams();
		String origin = params.get("Origin");

		if (origin.equals("Library") || origin.equals("Hand") || origin.equals("Sideboard")){
			return changeHiddenOriginPlayDrawbackAI(af, sa);
		}
		else if (origin.equals("Graveyard") || origin.equals("Exile") || origin.equals("Battlefield")){
			return changeKnownOriginPlayDrawbackAI(af, sa);
		}
		return false;
	}
	
	private static String changeZoneDescription(AbilityFactory af, SpellAbility sa){
		HashMap<String,String> params = af.getMapParams();
		String origin = params.get("Origin");
		if (origin.equals("Library") || origin.equals("Hand") || origin.equals("Sideboard")){
			return changeHiddenOriginStackDescription(af, sa);
		}
		else if (origin.equals("Graveyard") || origin.equals("Exile") || origin.equals("Battlefield")){
			return changeKnownOriginStackDescription(af, sa);
		}

		return "";
	}
	
	private static void changeZoneResolve(AbilityFactory af, SpellAbility sa){
		HashMap<String,String> params = af.getMapParams();
		String origin = params.get("Origin");
		if (origin.equals("Library") || origin.equals("Hand") || origin.equals("Sideboard")){
			changeHiddenOriginResolve(af, sa);
		}
		else if (origin.equals("Graveyard") || origin.equals("Exile") || origin.equals("Battlefield")){
			changeKnownOriginResolve(af, sa);
		}
	}

	// *************************************************************************************
	// ************ Hidden Origin (Library/Hand/Sideboard) *********************************
	// ******* Hidden origin cards are chosen on the resolution of the spell ***************
	// *************************************************************************************
	
	private static boolean changeHiddenOriginCanPlayAI(AbilityFactory af, SpellAbility sa){
		// Fetching should occur fairly often as it helps cast more spells, and have access to more mana
		Ability_Cost abCost = af.getAbCost();
		Card source = af.getHostCard();
		HashMap<String,String> params = af.getMapParams();
        //String destination = params.get("Destination");
        String origin = params.get("Origin");
		
		if (abCost != null){
			// AI currently disabled for these costs
			if (abCost.getSacCost()){
				// Sac is ok in general, but should add some decision making based off what we Sacrifice and what we might get
			}
			if (abCost.getLifeCost()){
				if (AllZone.ComputerPlayer.getLife() - abCost.getLifeAmount() < 4)
					return false;
			}
			if (abCost.getDiscardCost()) 	return false;
			
			if (abCost.getSubCounter()) 	return true; // only card that uses it is Fertilid
			
		}
		
		if (!ComputerUtil.canPayCost(sa))
			return false;
		
		Random r = new Random();
		// prevent run-away activations - first time will always return true
		boolean chance = r.nextFloat() <= Math.pow(.6667, source.getAbilityUsed());
		
		ArrayList<Player> pDefined;
		Target tgt = af.getAbTgt();
		if(tgt != null && tgt.canTgtPlayer()) {
			if (af.isCurse())
				tgt.addTarget(AllZone.HumanPlayer);
			else
				tgt.addTarget(AllZone.ComputerPlayer);
			pDefined = tgt.getTargetPlayers();
		}
		else{
			pDefined = AbilityFactory.getDefinedPlayers(sa.getSourceCard(),  params.get("Defined"), sa);
		}

		for(Player p : pDefined){
			if (origin.equals("Hand")){
				CardList hand = AllZoneUtil.getPlayerHand(p);
				if (hand.size() == 0)
					return false;

				if (p.isComputer()){
					if (params.containsKey("ChangeType")){
						hand = filterListByType(hand, params, "ChangeType", sa);
						if (hand.size() == 0)
							return false;
					}
				}
				// TODO: add some more improvements based on Destination and Type
			}
			else if (origin.equals("Library")){
				CardList library = AllZoneUtil.getPlayerCardsInLibrary(p);
				if (library.size() == 0)
					return false;
				
				// TODO: add some more improvements based on Destination and Type
			}
			else if (origin.equals("Sideboard")){
				// todo: once sideboard is added
				// canPlayAI for Wishes will go here
			}
		}
		
		Ability_Sub subAb = sa.getSubAbility();
		if (subAb != null)
			chance &= subAb.chkAI_Drawback();
		
		return ((r.nextFloat() < .8) && chance);
	}
	
	private static boolean changeHiddenOriginPlayDrawbackAI(AbilityFactory af, SpellAbility sa){
		// if putting cards from hand to library and parent is drawing cards
		// make sure this will actually do something:
		
		
		return true;
	}
	
	private static String changeHiddenOriginStackDescription(AbilityFactory af, SpellAbility sa){
		// TODO: build Stack Description will need expansion as more cards are added
		HashMap<String,String> params = af.getMapParams();

		 StringBuilder sb = new StringBuilder();
		 Card host = af.getHostCard();
		 
		 if (!(sa instanceof Ability_Sub))
			 sb.append(host.getName()).append(" -");
		 
		 sb.append(" ");
		 
		 String origin = params.get("Origin");
		 String destination = params.get("Destination");
		 
		 String type = params.containsKey("ChangeType") ? params.get("ChangeType") : "";
		 
		 if (origin.equals("Library")){
			 sb.append("Search your library for ").append(params.get("ChangeNum")).append(" ").append(type).append(" and ");
			 
			 sb.append("put that card ");
			 
			 if (destination.equals("Battlefield")){
				 sb.append("onto the battlefield");
				 if (params.containsKey("Tapped"))
					 sb.append(" tapped");
				 
				 
				 sb.append(".");
				 
			 }
			 if (destination.equals("Hand"))
				 sb.append("into your hand.");
			 if (destination.equals("Graveyard"))
				 sb.append("into your graveyard.");
			 
			 sb.append("Then shuffle your library.");
		 }
		 else if (origin.equals("Hand")){
			 
			 
			 sb.append("Put ").append(params.get("ChangeNum")).append(" ").append(type).append(" card(s) from your hand ");
			 
			 if (destination.equals("Battlefield"))
				 sb.append("onto the battlefield.");
			 if (destination.equals("Library")){
				 int libraryPos = params.containsKey("LibraryPosition") ? Integer.parseInt(params.get("LibraryPosition")) : 0;
				 
				 if (libraryPos == 0)
					 sb.append("on top");
				 if (libraryPos == -1)
					 sb.append("on bottom");
				 
				 sb.append(" of your library.");
			 }
		 }
		 
		 Ability_Sub abSub = sa.getSubAbility();
		 if (abSub != null) {
		 	sb.append(abSub.getStackDescription());
		 }
		 
		 return sb.toString();
	}
	
	private static void changeHiddenOriginResolve(AbilityFactory af, SpellAbility sa){
		HashMap<String,String> params = af.getMapParams();

		ArrayList<Player> fetchers = AbilityFactory.getDefinedPlayers(sa.getSourceCard(), params.get("Defined"), sa);

		for(Player player : fetchers){
			if (player.isComputer()){
				changeHiddenOriginResolveAI(af, sa);
			}
			else{
				changeHiddenOriginResolveHuman(af, sa);
			}
		}
	}
	
	private static void changeHiddenOriginResolveHuman(AbilityFactory af, SpellAbility sa){
		HashMap<String,String> params = af.getMapParams();
        Card card = af.getHostCard();
		Target tgt = af.getAbTgt();
		Player player;
		if (tgt != null){
			player = tgt.getTargetPlayers().get(0);
			if (!player.canTarget(sa.getSourceCard()))
				return;
		}
		else{
			player = sa.getActivatingPlayer();
		}

		String origin = params.get("Origin");
        String destination = params.get("Destination");
		
		CardList fetchList = AllZoneUtil.getCardsInZone(origin, player);
        if (destination.equals("Library"))
        	AllZone.Display.getChoice(af.getHostCard().getName() + " - Looking at " + origin, fetchList.toArray());
		
		fetchList = filterListByType(fetchList, params, "ChangeType", sa);

        PlayerZone origZone = AllZone.getZone(origin, player);
        PlayerZone destZone = AllZone.getZone(destination, player);
        
        int changeNum = params.containsKey("ChangeNum") ? Integer.parseInt(params.get("ChangeNum")) : 1;

        for(int i=0; i < changeNum; i++){
	        if(fetchList.size() == 0 || destination == null) 
	        	break;
	        	
            Object o = AllZone.Display.getChoiceOptional("Select a card", fetchList.toArray());
            
            if(o != null) {
            	origZone.remove(o);
            	Card c = (Card) o;
            	fetchList.remove(c);

                if (destination.equals("Library")){
                	// this needs to be zero indexed. Top = 0, Third = 2
                	int libraryPos = params.containsKey("LibraryPosition") ? Integer.parseInt(params.get("LibraryPosition")) : 0;
                	destZone.add(c, libraryPos);
                }
                else{
                	destZone.add(c);
                	if (destination.equals("Battlefield") && params.containsKey("Tapped"))
	                	c.tap();
                }
            }
        }
        player.shuffle();
	            
		String DrawBack = params.get("SubAbility");
        
        if (af.hasSubAbility()){
        	Ability_Sub abSub = sa.getSubAbility();
        	if (abSub != null){
        	   abSub.resolve();
        	}
        	else
        		CardFactoryUtil.doDrawBack(DrawBack, 0, card.getController(), card.getController().getOpponent(), card.getController(), card, null, sa);
        }
	}
	
	private static void changeHiddenOriginResolveAI(AbilityFactory af, SpellAbility sa){
		HashMap<String,String> params = af.getMapParams();
		Target tgt = af.getAbTgt();
		Card card = af.getHostCard();
		
		Player player;
		if (tgt != null){
			player = tgt.getTargetPlayers().get(0);
			if (!player.canTarget(sa.getSourceCard()))
				return;
		}
		else{
			player = sa.getActivatingPlayer();
		}
		
		String origin = params.get("Origin");
		
		CardList fetchList = AllZoneUtil.getCardsInZone(origin, player);
		fetchList = filterListByType(fetchList, params, "ChangeType", sa);
		
        String destination = params.get("Destination");
		
        PlayerZone origZone = AllZone.getZone(origin, player);
        PlayerZone destZone = AllZone.getZone(destination, player);

        String type = params.get("ChangeType");
		
        CardList fetched = new CardList();
        
        int changeNum = params.containsKey("ChangeNum") ? Integer.parseInt(params.get("ChangeNum")) : 1;

        for(int i=0;i<changeNum;i++){
	        if(fetchList.size() == 0 || destination == null)
	        	break;
	        
        	// Improve the AI for fetching. 
        	Card c;
        	if (type.contains("Basic"))
        		c = basicManaFixing(fetchList);
        	else if (areAllBasics(type))	// if Searching for only basics, 
        		c = basicManaFixing(fetchList, type);
        	else if (fetchList.getNotType("Creature").size() == 0)
        		c = CardFactoryUtil.AI_getBestCreature(fetchList); 	//if only creatures take the best
        	else if (destination.equals("Battlefield"))
        		c = CardFactoryUtil.AI_getMostExpensivePermanent(fetchList, af.getHostCard(), false);
        	else if (destination.equals("Exile")){
        		// Exiling your own stuff, if Exiling opponents stuff choose best
        		if (destZone.getPlayer().isHuman())
        			c = CardFactoryUtil.AI_getMostExpensivePermanent(fetchList, af.getHostCard(), false);
        		else
        			c = CardFactoryUtil.AI_getCheapestPermanent(fetchList, af.getHostCard(), false);
        	}
        	else{
        		fetchList.shuffle();
        		c = fetchList.get(0);
        	}

        	fetched.add(c);
        	fetchList.remove(c);
        }
        
        if (origin.equals("Library"))
        	player.shuffle();
        
        for(Card c : fetched){
        	origZone.remove(c);
        	if (destination.equals("Library")){
            	int libraryPos = params.containsKey("LibraryPosition") ? Integer.parseInt(params.get("LibraryPosition")) : 0;
        		destZone.add(c, libraryPos);
        	}
        	else
        		destZone.add(c);
        	if (destination.equals("Battlefield") && params.containsKey("Tapped"))
        		c.tap();
        }
        
        if (!destination.equals("Battlefield") && !type.equals("Card")) 
        	AllZone.Display.getChoice(af.getHostCard().getName() + " - Computer picked:", fetched.toArray());
        
		String DrawBack = params.get("SubAbility");
        
        if (af.hasSubAbility()){
        	Ability_Sub abSub = sa.getSubAbility();
        	if (abSub != null){
        	   abSub.resolve();
        	}
        	else
        		CardFactoryUtil.doDrawBack(DrawBack, 0, card.getController(), card.getController().getOpponent(), card.getController(), card, null, sa);
        }
	}
	
	// *********** Utility functions for Hidden ********************
	private static CardList filterListByType(CardList list, HashMap<String,String> params, String type, SpellAbility sa){		
		if (params.containsKey(type))
			list = list.getValidCards(params.get(type).split(","), sa.getActivatingPlayer(), sa.getSourceCard());
		return list;
	}
	
	private static Card basicManaFixing(CardList list){	// Search for a Basic Land
		return basicManaFixing(list, "Plains, Island, Swamp, Mountain, Forest");
	}
	
	private static Card basicManaFixing(CardList list, String type){	// type = basic land types
        CardList combined = AllZoneUtil.getPlayerCardsInPlay(AllZone.ComputerPlayer);
        combined.add(AllZoneUtil.getPlayerHand(AllZone.ComputerPlayer));
        
        String names[] = type.split(",");
        ArrayList<String> basics = new ArrayList<String>();

        // what types can I go get?
        for(int i = 0; i < names.length; i++){
        	if (list.getType(names[i]).size() != 0)
        		basics.add(names[i]);
        }
        
        // Which basic land is least available from hand and play, that I still have in my deck
        int minSize = Integer.MAX_VALUE;
        String minType = null;
        
        for(int i = 0; i < basics.size(); i++){
        	String b = basics.get(i);
        	int num = combined.getType(names[i]).size();
        	if (num < minSize){
        		minType = b;
        		minSize = num;
        	}
        }
        
        if (minType != null)
        	list = list.getType(minType);
        
        return list.get(0);
	}
	
	private static boolean areAllBasics(String types){
		String[] split = types.split(",");
        String names[] = {"Plains", "Island", "Swamp", "Mountain", "Forest"};
        boolean[] bBasic = new boolean[split.length];
        
        for(String s : names){
        	for(int i = 0; i < split.length; i++)
        		bBasic[i] |= s.equals(split[i]);        		
        }
		
    	for(int i = 0; i < split.length; i++)
    		if (!bBasic[i])
    			return false;

        return true;
	}
	
	
	// *************************************************************************************
	// **************** Known Origin (Battlefield/Graveyard/Exile) *************************
	// ******* Known origin cards are chosen during casting of the spell (target) **********
	// *************************************************************************************
	
	private static boolean changeKnownOriginCanPlayAI(AbilityFactory af, SpellAbility sa){
		// Retrieve either this card, or target Cards in Graveyard
		Ability_Cost abCost = af.getAbCost();
		final Card source = af.getHostCard();
		HashMap<String,String> params = af.getMapParams();

		String origin = params.get("Origin");
        String destination = params.get("Destination");	
        
		float pct = origin.equals("Battlefield") ? .8f : .667f;
		
		Random r = new Random();
		
		if (abCost != null){
			// AI currently disabled for these costs
			if (abCost.getSacCost()){
				// Sac is ok in general, but should add some decision making based off SacType and Retrieve Type
			}
			if (abCost.getLifeCost()){
				if (AllZone.ComputerPlayer.getLife() - abCost.getLifeAmount() < 4)
					return false;
			}
			if (abCost.getDiscardCost()) 	return false;
			
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
		
		// prevent run-away activations - first time will always return true
		boolean chance = r.nextFloat() <= Math.pow(.6667, sa.getRestrictions().getNumberTurnActivations());
		
		Target tgt = af.getAbTgt();
		if(tgt != null) {
			tgt.resetTargets();

			CardList list = AllZoneUtil.getCardsInZone(origin);
			list = list.getValidCards(tgt.getValidTgts(), AllZone.ComputerPlayer, source);
			 
			if (list.size() == 0)
				return false;
			
			// Narrow down the list:
			if (origin.equals("Battlefield")){
				// filter out untargetables
				list = list.filter(new CardListFilter() {
					public boolean addCard(Card c) {
						return CardFactoryUtil.canTarget(source, c);
					}
				});
				
				// if Destination is hand, either bounce opponents dangerous stuff or save my about to die stuff
				
				// if Destination is exile, filter out my cards
			}
			else if (origin.equals("Graveyard")){
				// Retrieve from Graveyard to:
				
			}
			
			if (destination.equals("Exile"))
				list = list.getController(AllZone.HumanPlayer);

			if (list.size() == 0)
				return false;
			
			 // target loop
			while(tgt.getNumTargeted() < tgt.getMaxTargets(sa.getSourceCard(), sa)){ 
				// AI Targeting 
				Card choice = null;

				if (list.getNotType("Creature").size() == 0 && destination.equals("Battlefield"))
	        		choice = CardFactoryUtil.AI_getBestCreature(list); //if only creatures take the best
	        	else if (destination.equals("Battlefield"))
	        		choice = CardFactoryUtil.AI_getMostExpensivePermanent(list, af.getHostCard(), false);
	        	else{
					// todo: AI needs more improvement to it's retrieval (reuse some code from spReturn here)
	        		list.shuffle();
	        		choice = list.get(0);
	        	}
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
				
				list.remove(choice);
				tgt.addTarget(choice);
			}
		}
		else{
			// non-targeted retrieval
			Card retrieval = null;
			if (af.getMapParams().containsKey("Defined")){
				// add hooks into AF_Defined function
				retrieval = knownDetermineDefined(sa, params.get("Defined"), origin);
			}
			
			if (retrieval == null)
				return false;
			
			if (retrieval == source){
				if (origin.equals("Graveyard")){
					// return this card from graveyard: cards like Hammer of Bogardan
					// in general this is cool, but we should add some type of restrictions
					
				}
				else if (origin.equals("Battlefield")){
					// return this card from battlefield: cards like Blinking Spirit
					// in general this should only be used to protect from Imminent Harm (dying or losing control of)
					return false;
				}
			}
		}

		Ability_Sub subAb = sa.getSubAbility();
		if (subAb != null)
			chance &= subAb.chkAI_Drawback();
		
		return ((r.nextFloat() < pct) && chance);
	}
	
	private static boolean changeKnownOriginPlayDrawbackAI(AbilityFactory af, SpellAbility sa){
		
		return true;
	}
	
	
	private static String changeKnownOriginStackDescription(AbilityFactory af, SpellAbility sa){
		HashMap<String,String> params = af.getMapParams();

		 StringBuilder sb = new StringBuilder();
		 Card host = af.getHostCard();
		 
		 sb.append(host.getName()).append(" - ");
		 
		String destination = params.get("Destination");
		String origin = params.get("Origin");
		 
		 StringBuilder sbTargets = new StringBuilder();

		 ArrayList<Card> tgts;
		 if (af.getAbTgt() != null)
			 tgts = af.getAbTgt().getTargetCards();
		 else{
			 // otherwise add self to list and go from there
			 tgts = new ArrayList<Card>();
			 tgts.add(knownDetermineDefined(sa, params.get("Defined"), origin));
		 }
		 
		 for(Card c : tgts)
			 sbTargets.append(" ").append(c.getName());
		 
		 String targetname = sbTargets.toString();
		 
		 String pronoun = tgts.size() > 1 ? " their " : " its ";

		 String fromGraveyard = " from the graveyard";

		 if (destination.equals("Battlefield")){
			 sb.append("Put").append(targetname);
			 if (origin.equals("Graveyard"))
				 sb.append(fromGraveyard);
			 
			 sb.append(" onto the battlefield");
        	if (params.containsKey("Tapped"))
        		sb.append(" tapped");
        	if (params.containsKey("GainControl"))
        		sb.append(" under your control");
        	sb.append(".");
		 }
		 
		 if(destination.equals("Hand")){
			 sb.append("Return").append(targetname);
			 if (origin.equals("Graveyard"))
				 sb.append(fromGraveyard);
			 sb.append(" to").append(pronoun).append("owners hand.");
		 }
		 
	     if (destination.equals("Library")){
	    	 if (params.containsKey("Shuffle")){	// for things like Gaea's Blessing
	    		 sb.append("Shuffle").append(targetname);

	    		 sb.append(" into").append(pronoun).append("owner's library.");
	    	 }
	    	 else{
			    sb.append("Put").append(targetname);
				 if (origin.equals("Graveyard"))
					 sb.append(fromGraveyard);
			    
	 		 	// this needs to be zero indexed. Top = 0, Third = 2, -1 = Bottom
	 		 	int libraryPosition = params.containsKey("LibraryPosition") ? Integer.parseInt(params.get("LibraryPosition")) : 0;        

			    if (libraryPosition == -1)
			    	sb.append(" on the bottom of").append(pronoun).append("owner's library.");
			    else if (libraryPosition == 0)
			    	sb.append(" on top of").append(pronoun).append("owner's library.");
			    else
			    	sb.append(libraryPosition-1).append(" from the top of").append(pronoun).append("owner's library.");
	    	 }
	     }
		 
		 if(destination.equals("Exile")){
			 sb.append("Exile ").append(targetname);
			 if (origin.equals("Graveyard"))
				 sb.append(fromGraveyard);
		 }
		 
		 if(destination.equals("Graveyard")){
			 sb.append("Put").append(targetname);
			 sb.append(" from ").append(origin);
			 sb.append(" into").append(pronoun).append("owner's graveyard.");
		 }
		 
		 Ability_Sub abSub = sa.getSubAbility();
		 if (abSub != null) {
		 	sb.append(abSub.getStackDescription());
		 }
		 
		 return sb.toString();
	}
	
	private static void changeKnownOriginResolve(AbilityFactory af, SpellAbility sa){
		ArrayList<Card> tgtCards;
		HashMap<String,String> params = af.getMapParams();
		Target tgt = af.getAbTgt();
		Player player = sa.getActivatingPlayer();
		
		String destination = params.get("Destination");
		String origin = params.get("Origin");
		
		if (tgt != null)
			tgtCards = tgt.getTargetCards();
		else{
			tgtCards = new ArrayList<Card>();
			tgtCards.add(knownDetermineDefined(sa, params.get("Defined"), origin));
		}

		Card targetCard = tgtCards.get(0);
		
		for(Card tgtC : tgtCards){
			PlayerZone originZone = AllZone.getZone(tgtC);
			if (!originZone.is(origin))
				continue;
	        
	        if (tgt != null && origin.equals("Battlefield")){
	        	// check targeting
	        	if (!CardFactoryUtil.canTarget(sa.getSourceCard(), tgtC))
	        		continue;
	        }
	         
	        originZone.remove(tgtC);
	    	
	    	Player pl = player;
	    	if (!destination.equals("Battlefield"))
	    		pl = tgtC.getOwner();
	    	
	    	if (destination.equals("Library")){
	    		// library position is zero indexed
	    		int libraryPosition = params.containsKey("LibraryPosition") ? Integer.parseInt(params.get("LibraryPosition")) : 0;        
 
	           PlayerZone library = AllZone.getZone(Constant.Zone.Library, pl);
	           
	           if (libraryPosition == -1)
	        	   libraryPosition = library.size();
	           
	           library.add(tgtC, libraryPosition); //move to library
	           
	           if (params.containsKey("Shuffle"))	// for things like Gaea's Blessing
	        	   player.shuffle();
	    	}
	    	else{
		    	if (destination.equals("Battlefield")){
		        	if (params.containsKey("Tapped"))
		        		tgtC.tap();
		        	if (params.containsKey("GainControl"))
		        		tgtC.setController(sa.getActivatingPlayer());
		    	}

		    	AllZone.GameAction.moveTo(AllZone.getZone(destination, pl), tgtC);
	    	}
		}

		
		if (af.hasSubAbility()){
			Ability_Sub abSub = sa.getSubAbility();
			if (abSub != null){
			   abSub.resolve();
			}
			else{
				String DrawBack = af.getMapParams().get("SubAbility");
				Card card = sa.getSourceCard();
				CardFactoryUtil.doDrawBack(DrawBack, 0, card.getController(), card.getController().getOpponent(), card.getController(), card, targetCard, sa);
			}
		}
	}
	
	// **************************** Known Utility **************************************
	private static Card knownDetermineDefined(SpellAbility sa, String defined, String origin){
		// todo: this function should return a ArrayList<Card> and then be handled by the callees
		CardList grave = AllZoneUtil.getCardsInZone(origin, sa.getActivatingPlayer());
		
		if (defined != null && defined.equals("Top")){
			// the "top" of the graveyard, is the last to be added to the graveyard list?
			if (grave.size() == 0)
				return null;
			return grave.get(grave.size()-1);
		}
		
		return AbilityFactory.getDefinedCards(sa.getSourceCard(), defined, sa).get(0);
	}

}
