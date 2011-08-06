package forge;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

public class AbilityFactory_Fetch {
	// An AbilityFactory subclass for Fetching Cards from Places
	
	//Destination$Battlefield|Valid$Basic|FetchNum$2|UpTo$True|[Tgt$TgtP]
	// Fetch from the library
	public static SpellAbility createAbilityFetch(final AbilityFactory AF){
		final SpellAbility abFetch = new Ability_Activated(AF.getHostCard(), AF.getAbCost(), AF.getAbTgt()){
			private static final long serialVersionUID = 3728332812890211671L;

			AbilityFactory af = AF;
			
			public boolean canPlayAI(){
				return fetchCanPlayAI(this, AF);
			}
			
			public boolean canPlay(){
				// super takes care of AdditionalCosts
				return super.canPlay();	
			}
			
			public String getStackDescription(){
				return fetchStackDescription(af, this);
			}
			
			@Override
			public void resolve() {
				doFetch(af, this);
			}
		
		};
		return abFetch;
	}
	
	public static SpellAbility createSpellFetch(final AbilityFactory AF){
		final SpellAbility spFetch = new Spell(AF.getHostCard(), AF.getAbCost(), AF.getAbTgt()) {
			private static final long serialVersionUID = 3270484211099902059L;

			AbilityFactory af = AF;
			
			public boolean canPlayAI(){
				return fetchCanPlayAI(this, AF);
			}
			
			public boolean canPlay(){
				// super takes care of AdditionalCosts
				return super.canPlay();	
			}
			
			public String getStackDescription(){
				return fetchStackDescription(af, this);
			}
			
			@Override
			public void resolve() {
				doFetch(af, this);
			}		
		};
		return spFetch;
	}
	
	private static String fetchStackDescription(AbilityFactory af, final SpellAbility sa){
		StringBuilder sb = new StringBuilder();
		
		sb.append(af.getHostCard()).append(" - ");
		sb.append(sa.toString());
		
		return sb.toString();
	}
	
	private static void doFetch(AbilityFactory af, final SpellAbility sa){
		// todo handle targeting changes
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
		
		if (player.isComputer()){
			doFetchAI(af, sa);
			return;
		}
				
		HashMap<String,String> params = af.getMapParams();
		CardList library = AllZoneUtil.getPlayerCardsInLibrary(player);
		
		
		library = filterListByType(library, params, "FetchType", sa);
		String DrawBack = params.get("SubAbility");
        String destination = params.get("Destination");
        Card card = af.getHostCard();
		
        int fetchNum = 1;	// Default to 1
        if (params.containsKey("FetchNum"))
        	fetchNum = AbilityFactory.calculateAmount(card, params.get("FetchNum"), sa);
        
        int libraryPosition = 0;        // this needs to be zero indexed. Top = 0, Third = 2
        if (params.containsKey("LibraryPosition"))
        	libraryPosition = Integer.parseInt(params.get("LibraryPosition"));
        
        for(int i=0;i<fetchNum;i++){
	        if(library.size() != 0 && destination != null) {
	        	
	            Object o = AllZone.Display.getChoiceOptional("Select a card", library.toArray());
	            
	            if (o == null)	// Player didn't want anything? Bail from loop
	            	break;
	            
            	AllZone.Human_Library.remove(o);
            	Card c = (Card) o;
            	library.remove(c);
                player.shuffle();
            	if (destination.equals("Hand")) 
            		AllZone.Human_Hand.add(c);         			//move to hand
            	else if (destination.equals("Library")) 
                	AllZone.Human_Library.add(c, libraryPosition); //move to top of library
            	else if (destination.equals("Battlefield")){
                	AllZone.getZone(Constant.Zone.Battlefield, player).add(c); //move to battlefield
                	if (params.containsKey("Tapped"))
                		c.tap();
            	}
            	else if(destination.equals("Graveyard")) {
            		AllZone.getZone(Constant.Zone.Graveyard, player).add(c);
            	}
            	else if (destination.equals("Exile"))	// Jester's Cap
            		AllZone.getZone(Constant.Zone.Exile, player).add(c);

	        }//if
        }
        
        if (af.hasSubAbility()){
        	Ability_Sub abSub = sa.getSubAbility();
        	if (abSub != null){
        	   abSub.resolve();
        	}
        	else
        		CardFactoryUtil.doDrawBack(DrawBack, 0, card.getController(), card.getController().getOpponent(), card.getController(), card, null, sa);
        }
	}
	
	private static void doFetchAI(AbilityFactory af, final SpellAbility sa){
		Player player = AllZone.ComputerPlayer;
		
		HashMap<String,String> params = af.getMapParams();
		Card card = af.getHostCard();
		CardList library = AllZoneUtil.getPlayerCardsInLibrary(player);
		library = filterListByType(library, params, "FetchType", sa);
		String DrawBack = params.get("SubAbility");
        String destination = params.get("Destination");
        String type = params.get("FetchType");
		
        int fetchNum = 1;	// Default to 1
        if (params.containsKey("FetchNum"))
        	fetchNum = Integer.parseInt(params.get("FetchNum"));
        
        int libraryPosition = 0;        // this needs to be zero indexed. Top = 0, Third = 2
        if (params.containsKey("LibraryPosition"))
        	libraryPosition = Integer.parseInt(params.get("LibraryPosition"));
		
        for(int i=0;i<fetchNum;i++){
	        if(library.size() != 0 && destination != null) {
	        	// Improve the AI for fetching. 
	        	Card c;
	        	if (type.contains("Basic"))
	        		c = fetchBasicManaFixing(library);
	        	else if (areAllBasics(type))	// if Searching for only basics, 
	        		c = fetchBasicManaFixing(library, type);
	        	else if (library.getNotType("Creature").size() == 0 && destination.equals("Battlefield"))
	        		c = CardFactoryUtil.AI_getBestCreature(library); //if only creatures take the best
	        	else if (destination.equals("Battlefield"))
	        		c = CardFactoryUtil.AI_getMostExpensivePermanent(library, af.getHostCard(), false);
	        	else if (destination.equals("Exile")){
	        		// Exiling your own stuff, if Exiling opponents stuff choose best
	        		c = CardFactoryUtil.AI_getCheapestPermanent(library, af.getHostCard(), false);
	        	}
	        	else
	        		c = library.get(0);

            	AllZone.Computer_Library.remove(c);
                player.shuffle();
            	library.remove(c);
            	if (destination.equals("Hand")) {
                    	CardList l = new CardList();
                    	l.add(c);
                    	if (!type.equals("Card"))
                    	AllZone.Display.getChoiceOptional(af.getHostCard().getName() + " - Computer picked:", l.toArray());
            		AllZone.Computer_Hand.add(c);
            	}//move to hand
            	else if (destination.equals("Battlefield")) {
                	AllZone.getZone(Constant.Zone.Battlefield, player).add(c); //move to battlefield
                	if (params.containsKey("Tapped"))
                		c.tap();
            	}
            	else if (destination.equals("Library")) {
                	CardList l = new CardList();
                	l.add(c);
                	if (!type.equals("Card"))
                	AllZone.Display.getChoiceOptional(af.getHostCard().getName() + " - Computer picked:", l.toArray());
                	AllZone.Computer_Library.add(c, libraryPosition);
            	}//move to top of library
	        }//if
	        
	        if (af.hasSubAbility()){
	        	Ability_Sub abSub = sa.getSubAbility();
	        	if (abSub != null){
	        	   abSub.resolve();
	        	}
	        	else
	        		CardFactoryUtil.doDrawBack(DrawBack, 0, card.getController(), card.getController().getOpponent(), card.getController(), card, null, sa);
	        }
        }
	}

	private static boolean fetchCanPlayAI(SpellAbility sa, AbilityFactory af){
		// Fetching should occur fairly often as it helps cast more spells, and have access to more mana
		Ability_Cost abCost = af.getAbCost();
		Card source = af.getHostCard();
		//HashMap<String,String> params = af.getMapParams();
        //String destination = params.get("Destination");
		
		if (abCost != null){
			// AI currently disabled for these costs
			if (abCost.getSacCost()){
				// Sac is ok in general, but should add some decision making based off SacType and Fetch Type
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
		
		Target tgt = af.getAbTgt();
		if(tgt != null && tgt.canTgtPlayer()) {
			tgt.addTarget(AllZone.ComputerPlayer);
		}

		// todo: add more decision making for Fetching
		// if Type is Land or a Land Type, improve chances for each Landfall card you control
		
		Ability_Sub subAb = sa.getSubAbility();
		if (subAb != null)
			chance &= subAb.chkAI_Drawback();
		
		return ((r.nextFloat() < .8) && chance);
	}
	

	private static CardList filterListByType(CardList list, HashMap<String,String> params, String type, SpellAbility sa){		
		if (params.containsKey(type))
			list = list.getValidCards(params.get(type).split(","), sa.getActivatingPlayer(), sa.getSourceCard());
		return list;
	}
	
	private static Card fetchBasicManaFixing(CardList list){	// Search for a Basic Land
        CardList combined = AllZoneUtil.getPlayerCardsInPlay(AllZone.ComputerPlayer);
        combined.add(AllZoneUtil.getPlayerHand(AllZone.ComputerPlayer));
        
        String names[] = {"Plains", "Island", "Swamp", "Mountain", "Forest"};
        ArrayList<String> basics = new ArrayList<String>();

        // what types are available
        for(int i = 0; i < 5; i++){
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
	
	private static Card fetchBasicManaFixing(CardList list, String type){	// type = basic land types
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
	
	// ********** Retrieve card from Graveyard ***********
	// Self Retrieval
	// A:AB$Retrieve|Cost$2 R R R|Destination$Hand|ActivatingZone$Graveyard
	// Targeted Retrieval
	// A:SP$Retrieve|Cost$B|Destination$Hand|TgtPrompt$Choose target creature card in your graveyard|ValidTgts$Creature|SpellDescription$<Desc>
	// Currently limited to returning itself or targeting one Valid
	// Defined Retrieval
	// A:AB$Retrieve|Cost$2|Destination$Library|LibraryPosition$-1|Defined$Top
	
	public static SpellAbility createAbilityRetrieve(final AbilityFactory AF){
		final SpellAbility abRetrieve = new Ability_Activated(AF.getHostCard(), AF.getAbCost(), AF.getAbTgt()){
			private static final long serialVersionUID = 3728332812890211671L;

			AbilityFactory af = AF;
			
			public boolean canPlayAI(){
				return retrieveCanPlayAI(this, AF);
			}
			
			public boolean canPlay(){
				// super takes care of AdditionalCosts
				// make sure there's a legal Target
				
				return super.canPlay();	
			}
			
			@Override
			public String getStackDescription(){
			// when getStackDesc is called, just build exactly what is happening
				return retrieveStackDescription(af, this);
			}
			
			@Override
			public void resolve() {
				doRetrieve(af, this);
			}
		
		};
		if (abRetrieve.getTarget() != null && !AF.getMapParams().containsKey("TgtZone"))
			abRetrieve.getTarget().setZone(Constant.Zone.Graveyard);
		return abRetrieve;
	}
	
	public static SpellAbility createSpellRetrieve(final AbilityFactory AF){
		final SpellAbility spRetrieve = new Spell(AF.getHostCard(), AF.getAbCost(), AF.getAbTgt()) {
			private static final long serialVersionUID = 3270484211099902059L;

			AbilityFactory af = AF;
			
			public boolean canPlayAI(){
				return retrieveCanPlayAI(this, AF);
			}
			
			public boolean canPlay(){
				// super takes care of AdditionalCosts
				return super.canPlay();	
			}
			
			@Override
			public String getStackDescription(){
			// when getStackDesc is called, just build exactly what is happening
				return retrieveStackDescription(af, this);
			}
			
			@Override
			public void resolve() {
				doRetrieve(af, this);
			}		
		};
		if (spRetrieve.getTarget() != null && !AF.getMapParams().containsKey("TgtZone"))
			spRetrieve.getTarget().setZone(Constant.Zone.Graveyard);
		return spRetrieve;
	}
	
	public static String retrieveStackDescription(AbilityFactory af, SpellAbility sa){
		HashMap<String,String> params = af.getMapParams();
		String destination = params.get("Destination");
		 StringBuilder sb = new StringBuilder();
		 Card host = af.getHostCard();
		 
		 sb.append(host.getName()).append(" - ");
		 
		 if (params.containsKey("Defined"))
			 return sb.toString() + params.get("SpellDescription");
		 
		 StringBuilder sbTargets = new StringBuilder();

		 ArrayList<Card> tgts;
		 if (af.getAbTgt() != null)
			 tgts = af.getAbTgt().getTargetCards();
		 else{
			 // otherwise add self to list and go from there
			 tgts = new ArrayList<Card>();
			 tgts.add(af.getHostCard());
		 }
		 
		 for(Card c : tgts)
			 sbTargets.append(" ").append(c.getName());
		 
		 String targetname = sbTargets.toString();
		 
		 String pronoun = tgts.size() > 1 ? " their " : " its ";
		 
		 if (destination.equals("Battlefield")){
			 sb.append("Put").append(targetname).append(" onto the battlefield");
        	if (params.containsKey("Tapped"))
        		sb.append(" tapped");
        	if (params.containsKey("GainControl"))
        		sb.append(" under your control");
        	sb.append(".");
		 }
		 
		 if(destination.equals("Hand"))
			 sb.append("Return").append(targetname).append(" to").append(pronoun).append("owners hand.");
		 
	     if (destination.equals("Library")){
	    	 if (params.containsKey("Shuffle"))	// for things like Gaea's Blessing
	    		 sb.append("Shuffle").append(targetname).append(" into").append(pronoun).append("owner's library.");
	    	 else{
	 		 	int libraryPosition = 0;        // this needs to be zero indexed. Top = 0, Third = 2, -1 = Bottom
			    if (params.containsKey("LibraryPosition"))
			    	libraryPosition = Integer.parseInt(params.get("LibraryPosition"));
			    
			    if (libraryPosition == -1)
			    	sb.append("Put").append(targetname).append(" on the bottom of").append(pronoun).append("owner's library.");
			    else if (libraryPosition == 0)
			    	sb.append("Put").append(targetname).append(" on top of").append(pronoun).append("owner's library.");
			    else
			    	sb.append("Put").append(targetname).append("").append(libraryPosition-1).append(" from the top of").append(pronoun).append("owner's library.");
	    	 }
	     }
		 
		 if(destination.equals("Exile"))
			 sb.append("Exile").append(targetname);
		 
		 Ability_Sub abSub = sa.getSubAbility();
		 if (abSub != null) {
		 	sb.append(abSub.getStackDescription());
		 }
		 
		 return sb.toString();
	}
	
	private static Card retrieveDetermineDefined(String defined, Player player){
		CardList grave = AllZoneUtil.getCardsInZone(Constant.Zone.Graveyard, player);
		
		if (defined.equals("Top")){
			// i think the "top" of the graveyard, is the last to be added to the graveyard list?
			if (grave.size() == 0)
				return null;
			return grave.get(grave.size()-1);
		}
			
		return null;
	}
	
	private static boolean retrieveCanPlayAI(SpellAbility sa, AbilityFactory af){
		// Retrieve either this card, or target Cards in Graveyard
		Ability_Cost abCost = af.getAbCost();
		Card source = af.getHostCard();
		HashMap<String,String> params = af.getMapParams();
        String destination = params.get("Destination");
		
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
			
			if (abCost.getSubCounter()) 	return false;
			
		}
		
		if (!ComputerUtil.canPayCost(sa))
			return false;
		
		Random r = new Random();
		// prevent run-away activations - first time will always return true
		boolean chance = r.nextFloat() <= Math.pow(.6667, source.getAbilityUsed());
		
		Target tgt = af.getAbTgt();
		if(tgt != null) {
			tgt.resetTargets();
			 // target loop
			
			CardList list = AllZoneUtil.getPlayerGraveyard(AllZone.ComputerPlayer);
			
			 //if possible take best card from all graveyards
			if(destination.equals("Battlefield") && params.containsKey("GainControl")) 
				list.add(AllZoneUtil.getPlayerGraveyard(AllZone.HumanPlayer));
			
			//If the destination is Exile consider only the human graveyard
			if(destination.equals("Exile")) list = AllZoneUtil.getPlayerGraveyard(AllZone.HumanPlayer);
			
			list = list.getValidCards(tgt.getValidTgts(), AllZone.ComputerPlayer, af.getHostCard());
			 
			if (list.size() == 0)
				return false;
			
			
			while(tgt.getNumTargeted() < tgt.getMaxTargets(sa.getSourceCard(), sa)){ 
				// AI Targeting 
				Card choice;
				
				if (list.size() == 0){
					if (tgt.getNumTargeted() < tgt.getMinTargets(sa.getSourceCard(), sa) || tgt.getNumTargeted() == 0){
						tgt.resetTargets();
						return false;
					}
					else{
						// todo is this good enough? for up to amounts?
						break;
					}
				}

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
			Card retrieval;
			if (af.getMapParams().containsKey("Defined")){
				retrieval = retrieveDetermineDefined(af.getMapParams().get("Defined"), sa.getActivatingPlayer());
				if (retrieval == null)
					return false;
			}
		}

		// todo: add more decision making for Retrieve
		Ability_Sub subAb = sa.getSubAbility();
		if (subAb != null)
			chance &= subAb.chkAI_Drawback();
		
		return ((r.nextFloat() < .8) && chance);
	}
	
	private static void doRetrieve(AbilityFactory af, final SpellAbility sa){
		ArrayList<Card> tgtCards;
		Target tgt = af.getAbTgt();
		Player player = sa.getActivatingPlayer();
		
		if (tgt != null)
			tgtCards = tgt.getTargetCards();
		else{
			tgtCards = new ArrayList<Card>();
			Card retrieval;
			if (af.getMapParams().containsKey("Defined"))
				retrieval = retrieveDetermineDefined(af.getMapParams().get("Defined"), player);
			else
				retrieval = sa.getSourceCard();
			tgtCards.add(retrieval);
		}

		for(Card tgtC : tgtCards){
			HashMap<String,String> params = af.getMapParams();
			
			CardList grave = AllZoneUtil.getPlayerGraveyard(player);
			grave = filterListByType(grave, params, "RetrieveType", sa);
			
	        String destination = params.get("Destination");
	        
	        if(grave.size() == 0 || destination == null) 
	        	return;
	        
	        // targeting check would go here, but shroud and protection doesn't matter in graveyards
	        
	    	grave.remove(tgtC);
	    	AllZone.getZone(tgtC).remove(tgtC);
	    	
	    	if (destination.equals("Hand")) 
	    		AllZone.getZone(Constant.Zone.Hand, player).add(tgtC);         			//move to hand
	    	else if (destination.equals("Library")){
	           int libraryPosition = 0;        // this needs to be zero indexed. Top = 0, Third = 2, -1 = Bottom
	           if (params.containsKey("LibraryPosition"))
	        	   libraryPosition = Integer.parseInt(params.get("LibraryPosition"));
	            
	           if (libraryPosition == -1)
	        	   libraryPosition = AllZone.Human_Library.size();
	    		
	           AllZone.getZone(Constant.Zone.Library, player).add(tgtC, libraryPosition); //move to library
	           
	           if (params.containsKey("Shuffle"))	// for things like Gaea's Blessing
	        	   player.shuffle();
	    	}
	    	else if (destination.equals("Battlefield")){
	        	AllZone.getZone(Constant.Zone.Battlefield, player).add(tgtC); //move to battlefield
	        	if (params.containsKey("Tapped"))
	        		tgtC.tap();
	        	if (params.containsKey("GainControl"))
	        		tgtC.setController(sa.getActivatingPlayer());
	    	}
	    	else if (destination.equals("Exile")){
	    		AllZone.getZone(Constant.Zone.Exile, tgtC.getOwner()).add(tgtC);
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
				CardFactoryUtil.doDrawBack(DrawBack, 0, card.getController(), card.getController().getOpponent(), card.getController(), card, null, sa);
			}
		}
    }//if
}
