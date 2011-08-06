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
			
			@Override
			public void resolve() {
				Player tgt = getTargetPlayer();
				if(null == tgt) tgt = this.getActivatingPlayer();
				doFetch(af, this, tgt);
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
			
			@Override
			public void resolve() {
				Player tgt = getTargetPlayer();
				if(null == tgt) tgt = this.getActivatingPlayer();
				doFetch(af, this, tgt);
			}		
		};
		return spFetch;
	}
	
	private static void doFetch(AbilityFactory af, final SpellAbility sa, Player player){
		if (player.isComputer()){
			doFetchAI(af, sa, player);
			return;
		}
				
		HashMap<String,String> params = af.getMapParams();
		CardList library = AllZoneUtil.getPlayerCardsInLibrary(player);
		library = filterListByType(library, params, "FetchType");
		String DrawBack = params.get("SubAbility");
        String destination = params.get("Destination");
        Card card = af.getHostCard();
		
        int fetchNum = 1;	// Default to 1
        if (params.containsKey("FetchNum"))
        	fetchNum = Integer.parseInt(params.get("FetchNum"));
        
        int libraryPosition = 0;        // this needs to be zero indexed. Top = 0, Third = 2
        if (params.containsKey("LibraryPosition"))
        	libraryPosition = Integer.parseInt(params.get("LibraryPosition"));
        
        for(int i=0;i<fetchNum;i++){
	        if(library.size() != 0 && destination != null) {
	        	
	            Object o = AllZone.Display.getChoiceOptional("Select a card", library.toArray());
	            
	            if(o != null) {
	            	AllZone.Human_Library.remove(o);
	                player.shuffle();
	            	Card c = (Card) o;
	            	library.remove(c);
	            	if (destination.equals("Hand")) 
	            		AllZone.Human_Hand.add(c);         			//move to hand
	            	else if (destination.equals("Library")) 
	                	AllZone.Human_Library.add(c, libraryPosition); //move to top of library
	            	else if (destination.equals("Battlefield")){
	                	AllZone.getZone(Constant.Zone.Play, player).add(c); //move to battlefield
	                	if (params.containsKey("Tapped"))
	                		c.tap();
	            	}
	            }
	            if (af.hasSubAbility())
	    			CardFactoryUtil.doDrawBack(DrawBack, 0, card.getController(), card.getController().getOpponent(), card.getController(), card, null, sa);
	        }//if
        }
	}
	
	private static void doFetchAI(AbilityFactory af, final SpellAbility sa, Player player){
		HashMap<String,String> params = af.getMapParams();
		Card card = af.getHostCard();
		CardList library = AllZoneUtil.getPlayerCardsInLibrary(player);
		library = filterListByType(library, params, "FetchType");
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
                	AllZone.getZone(Constant.Zone.Play, player).add(c); //move to battlefield
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
	        if (af.hasSubAbility())
    			CardFactoryUtil.doDrawBack(DrawBack, 0, card.getController(), card.getController().getOpponent(), card.getController(), card, null, sa);
        }
	}

	private static boolean fetchCanPlayAI(SpellAbility sa, AbilityFactory af){
		// Fetching should occur fairly often as it helps cast more spells, and have access to more mana
		Ability_Cost abCost = af.getAbCost();
		Card source = af.getHostCard();
		
		if (abCost != null){
			// AI currently disabled for these costs
			if (abCost.getSacCost()){
				// Sac is ok in general, but should add some decision making based off SacType and Fetch Type
			}
			if (abCost.getLifeCost()){
				if (AllZone.ComputerPlayer.getLife() < 5)
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
		if(tgt != null && tgt.canTgtPlayer()) {
			sa.setTargetPlayer(AllZone.ComputerPlayer);
		}

		// todo: add more decision making for Fetching
		// if Type is Land or a Land Type, improve chances for each Landfall card you control
		
		return ((r.nextFloat() < .8) && chance);
	}
	

	private static CardList filterListByType(CardList list, HashMap<String,String> params, String type){
		if (params.containsKey(type))
			list = list.getValidCards(params.get(type).split(","));
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
				return fetchCanPlayAI(this, AF);
			}
			
			public boolean canPlay(){
				// super takes care of AdditionalCosts
				// make sure there's a legal Target
				
				return super.canPlay();	
			}
			
			@Override
			public void resolve() {
				Card retrieval = getTargetCard();
				
				if(null == retrieval){
				// see if the choice is defined, like "Top" or "Random"
					if (AF.getMapParams().containsKey("Defined"))
						retrieval = retrieveDetermineDefined(AF.getMapParams().get("Defined"), getActivatingPlayer());
					else
						retrieval = this.getSourceCard();
				}
				
				if (null != retrieval)
					doRetrieve(af, this, retrieval);
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
			public void resolve() {
				Card retrieval = getTargetCard();
				
				if(null == retrieval){
				// see if the choice is defined, like "Top" or "Random"
					if (AF.getMapParams().containsKey("Defined"))
						retrieval = retrieveDetermineDefined(AF.getMapParams().get("Defined"), getActivatingPlayer());
					else
						retrieval = this.getSourceCard();
				}
				
				if (null != retrieval)
					doRetrieve(af, this, retrieval);
			}		
		};
		if (spRetrieve.getTarget() != null && !AF.getMapParams().containsKey("TgtZone"))
			spRetrieve.getTarget().setZone(Constant.Zone.Graveyard);
		return spRetrieve;
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
		
		if (abCost != null){
			// AI currently disabled for these costs
			if (abCost.getSacCost()){
				// Sac is ok in general, but should add some decision making based off SacType and Retrieve Type
			}
			if (abCost.getLifeCost()){
				if (AllZone.ComputerPlayer.getLife() < 5)
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
			// AI Targeting 
			CardList list = AllZoneUtil.getPlayerGraveyard(AllZone.ComputerPlayer);
			list = list.getValidCards(tgt.getValidTgts(), AllZone.ComputerPlayer);
			
			if (list.size() == 0)
				return false;
			
			list.shuffle();
			sa.setTargetCard(list.get(0));
			// todo: the AI actually needs a "smart" way to choose before we add any Retrieve cards that tgt.
			// reuse some code from spReturn here
		}
		else{
			Card retrieval;
			if (af.getMapParams().containsKey("Defined")){
				retrieval = retrieveDetermineDefined(af.getMapParams().get("Defined"), sa.getActivatingPlayer());
				if (retrieval == null)
					return false;
			}
			else{
				retrieval = sa.getSourceCard();
			}
		}

		// todo: add more decision making for Retrieve
		
		return ((r.nextFloat() < .8) && chance);
	}
	
	private static void doRetrieve(AbilityFactory af, final SpellAbility sa, Card tgt){
		// retrieve currently can't target things due to a lack of an input method
		HashMap<String,String> params = af.getMapParams();
		
		Player player = sa.getActivatingPlayer();
		CardList grave = AllZoneUtil.getPlayerGraveyard(player);
		grave = filterListByType(grave, params, "RetrieveType");
		
		String DrawBack = params.get("SubAbility");
        String destination = params.get("Destination");
        Card card = af.getHostCard();
        
        if(grave.size() == 0 || destination == null) 
        	return;
        	
    	grave.remove(tgt);
    	AllZone.getZone(tgt).remove(tgt);
    	
    	if (destination.equals("Hand")) 
    		AllZone.getZone(Constant.Zone.Hand, player).add(tgt);         			//move to hand
    	else if (destination.equals("Library")){
           int libraryPosition = 0;        // this needs to be zero indexed. Top = 0, Third = 2, -1 = Bottom
           if (params.containsKey("LibraryPosition"))
        	   libraryPosition = Integer.parseInt(params.get("LibraryPosition"));
            
           if (libraryPosition == -1)
        	   libraryPosition = AllZone.Human_Library.size();
    		
           AllZone.getZone(Constant.Zone.Library, player).add(tgt, libraryPosition); //move to library
    	}
    	else if (destination.equals("Battlefield")){
        	AllZone.getZone(Constant.Zone.Play, player).add(tgt); //move to battlefield
        	if (params.containsKey("Tapped"))
        		tgt.tap();
    	}
            	
        if (af.hasSubAbility())
			CardFactoryUtil.doDrawBack(DrawBack, 0, card.getController(), card.getController().getOpponent(), card.getController(), card, null, sa);
    }//if
}
