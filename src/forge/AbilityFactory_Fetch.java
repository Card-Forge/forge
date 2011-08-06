package forge;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

public class AbilityFactory_Fetch {
	// An AbilityFactory subclass for Fetching Cards from the Library
	
	//Destination$Battlefield|Valid$Basic|FetchNum$2|UpTo$True
	
	public static SpellAbility createAbilityFetch(final AbilityFactory AF){
		final SpellAbility abFetch = new Ability_Activated(AF.getHostCard(), AF.getAbCost(), AF.getAbTgt()){
			private static final long serialVersionUID = 3728332812890211671L;

			AbilityFactory af = AF;
			
			public boolean canPlayAI(){
				return fetchCanPlayAI(this, AF);
			}
			
			public boolean canPlay(){
				// super takes care of AdditionalCosts
				return (CardFactoryUtil.canUseAbility(af.getHostCard()) && super.canPlay());	
			}
			
			@Override
			public void resolve() {
				doFetch(af, this.getActivatingPlayer());
			}
		
		};
		return abFetch;
	}
	
	public static SpellAbility createSpellFetch(final AbilityFactory AF){
		final SpellAbility spFetch = new Spell(AF.getHostCard()) {
			private static final long serialVersionUID = 3270484211099902059L;

			AbilityFactory af = AF;
			
			public boolean canPlayAI(){
				return fetchCanPlayAI(this, AF);
			}
			
			public boolean canPlay(){
				// super takes care of AdditionalCosts
				return (CardFactoryUtil.canUseAbility(af.getHostCard()) && super.canPlay());	
			}
			
			@Override
			public void resolve() {
				doFetch(af, this.getActivatingPlayer());
			}		
		};
		return spFetch;
	}
	
	private static void doFetch(AbilityFactory af, String player){
		if (player.equals(Constant.Player.Computer)){
			doFetchAI(af, player);
			return;
		}
				
		HashMap<String,String> params = af.getMapParams();
		CardList library = AllZoneUtil.getPlayerCardsInLibrary(player);
		library = filterDeck(library, params);
        String destination = params.get("Destination");
		
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
	            	Card c = (Card) o;
	            	library.remove(c);
	            	if (destination.equals("Hand")) 
	            		AllZone.Human_Hand.add(c);         			//move to hand
	            	else if (destination.equals("Library")) 
	                	AllZone.Human_Library.add(c, libraryPosition); //move to top of library
	            	else if (destination.equals("Battlefield")) 
	                	AllZone.getZone(Constant.Zone.Play, player).add(c); //move to battlefield
	            }
	            else
	            	break;
	        }//if
        }
        AllZone.GameAction.shuffle(player);
	}
	
	private static void doFetchAI(AbilityFactory af, String player){
		HashMap<String,String> params = af.getMapParams();
		CardList library = AllZoneUtil.getPlayerCardsInLibrary(player);
		library = filterDeck(library, params);
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
	        	else
	        		c = library.get(0);

            	AllZone.Human_Library.remove(c);
            	library.remove(c);
            	if (destination.equals("Hand")) 
            		AllZone.Human_Hand.add(c);         			//move to hand
            	else if (destination.equals("Battlefield")) 
                	AllZone.getZone(Constant.Zone.Play, player).add(c); //move to battlefield
            	else if (destination.equals("Library")) 
                	AllZone.Human_Library.add(c, libraryPosition); //move to top of library
	        }//if
        }
        AllZone.GameAction.shuffle(player);
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
			if (abCost.getLifeCost())	 	return false;
			if (abCost.getDiscardCost()) 	return false;
			
			if (abCost.getSubCounter()) 	return false;
			
		}
		
		if (!ComputerUtil.canPayCost(sa))
			return false;
		
		Random r = new Random();
		// prevent run-away activations - first time will always return true
		boolean chance = r.nextFloat() <= Math.pow(.6667, source.getAbilityUsed());
		
		// todo: add more decision making if Fetch is used for things other than Lands. 
		
		return ((r.nextFloat() < .8) && chance);
	}
	

	private static CardList filterDeck(CardList list, HashMap<String,String> params){
		if (params.containsKey("FetchType"))
			list = list.getValidCards(params.get("FetchType").split(","));
		return list;
	}
	
	private static Card fetchBasicManaFixing(CardList list){
        CardList combined = AllZoneUtil.getPlayerCardsInPlay(Constant.Player.Computer);
        combined.add(AllZoneUtil.getPlayerHand(Constant.Player.Computer));
        
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
        	String b = basics.get(0);
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
	
	
}
