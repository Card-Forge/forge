package forge.card.abilityFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

import forge.AllZone;
import forge.AllZoneUtil;
import forge.Card;
import forge.CardList;
import forge.ComputerUtil;
import forge.Constant;
import forge.GameActionUtil;
import forge.MyRandom;
import forge.Player;
import forge.PlayerZone;
import forge.card.cardFactory.CardFactoryUtil;
import forge.card.spellability.Ability_Activated;
import forge.card.spellability.Ability_Sub;
import forge.card.spellability.Cost;
import forge.card.spellability.Spell;
import forge.card.spellability.SpellAbility;
import forge.card.spellability.Target;
import forge.gui.GuiUtils;

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

			@Override
			public boolean doTrigger(boolean mandatory) {
				return changeZoneTriggerAI(AF, this, mandatory);
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

			@Override
			public boolean doTrigger(boolean mandatory) {
				return changeZoneTriggerAI(AF, this, mandatory);
			}
		};
		setMiscellaneous(AF, dbChangeZone);
		return dbChangeZone;
	}
	
	public static boolean isHidden(String origin, boolean hiddenOverride){
		return (hiddenOverride || origin.equals("Library") || origin.equals("Hand") || origin.equals("Sideboard"));
	}
	
	public static boolean isKnown(String origin){
		return (origin.equals("Graveyard") || origin.equals("Exile") || origin.equals("Battlefield") || origin.equals("Stack"));
	}
	
	private static void setMiscellaneous(AbilityFactory af, SpellAbility sa){
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

		if (isHidden(origin, params.containsKey("Hidden")))
			return changeHiddenOriginCanPlayAI(af, sa);
		
		else if (isKnown(origin))
			return changeKnownOriginCanPlayAI(af, sa);
		
		return false;
	}
	
	private static boolean changeZonePlayDrawbackAI(AbilityFactory af, SpellAbility sa){
		HashMap<String,String> params = af.getMapParams();
		String origin = params.get("Origin");

		if (isHidden(origin, params.containsKey("Hidden")))
			return changeHiddenOriginPlayDrawbackAI(af, sa);
		
		else if (isKnown(origin))
			return changeKnownOriginPlayDrawbackAI(af, sa);
		
		return false;
	}
	
	private static boolean changeZoneTriggerAI(AbilityFactory af, SpellAbility sa, boolean mandatory){
		HashMap<String,String> params = af.getMapParams();
		String origin = params.get("Origin");
		
		if (isHidden(origin, params.containsKey("Hidden")))
			return changeHiddenTriggerAI(af, sa, mandatory);
		
		else if (isKnown(origin))
			return changeKnownOriginTriggerAI(af, sa, mandatory);
		
		return false;
	}
	
	private static String changeZoneDescription(AbilityFactory af, SpellAbility sa){
		HashMap<String,String> params = af.getMapParams();
		String origin = params.get("Origin");
		
		if (isHidden(origin, params.containsKey("Hidden")))
			return changeHiddenOriginStackDescription(af, sa);
		
		else if (isKnown(origin))
			return changeKnownOriginStackDescription(af, sa);
		
		return "";
	}
	
	private static void changeZoneResolve(AbilityFactory af, SpellAbility sa){
		HashMap<String,String> params = af.getMapParams();
		String origin = params.get("Origin");
		
		if (isHidden(origin, params.containsKey("Hidden")) && !params.containsKey("Ninjutsu"))
			changeHiddenOriginResolve(af, sa);
		
		else if (isKnown(origin) || params.containsKey("Ninjutsu"))
			changeKnownOriginResolve(af, sa);
	}

	// *************************************************************************************
	// ************ Hidden Origin (Library/Hand/Sideboard/Non-targetd other) ***************
	// ******* Hidden origin cards are chosen on the resolution of the spell ***************
	// ******* It is possible for these to have Destination of Battlefield *****************
	// ****** Example: Cavern Harpy where you don't choose the card until resolution *******
	// *************************************************************************************
	
	private static boolean changeHiddenOriginCanPlayAI(AbilityFactory af, SpellAbility sa){
		// Fetching should occur fairly often as it helps cast more spells, and have access to more mana
		Cost abCost = af.getAbCost();
		Card source = af.getHostCard();
		HashMap<String,String> params = af.getMapParams();
        String origin = params.get("Origin");
		//String destination = params.get("Destination");
		
		if (abCost != null){
			// AI currently disabled for these costs
			if (abCost.getSacCost() && !abCost.getSacThis()){
				//only sacrifice something that's supposed to be sacrificed 
				String type = abCost.getSacType();
			    CardList typeList = AllZoneUtil.getPlayerCardsInPlay(AllZone.ComputerPlayer);
			    typeList = typeList.getValidCards(type.split(","), source.getController(), source);
			    if(ComputerUtil.getCardPreference(source, "SacCost", typeList) == null)
			    	return false;
			}
			if (abCost.getLifeCost()){
				if (AllZone.ComputerPlayer.getLife() - abCost.getLifeAmount() < 4)
					return false;
			}
			if (abCost.getDiscardCost()) 	return false;
			
			if (abCost.getSubCounter()) 	; // SubCounter is fine
			
		}
		
		if (!ComputerUtil.canPayCost(sa))
			return false;
		
		Random r = MyRandom.random;
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
			pDefined = AbilityFactory.getDefinedPlayers(sa.getSourceCard(), params.get("Defined"), sa);
		}
		
		String type = params.get("ChangeType");
		if (type != null){
			if (type.contains("X") && source.getSVar("X").equals("Count$xPaid")){
				// Set PayX here to maximum value.
				int xPay = ComputerUtil.determineLeftoverMana(sa);
				source.setSVar("PayX", Integer.toString(xPay));
			}
		}
		
		for(Player p : pDefined){
			CardList list = AllZoneUtil.getCardsInZone(origin, p);

			if (type != null && p.isComputer()){
				// AI only "knows" about his information
				list = filterListByType(list, params, sa);
			}
			
			if (list.isEmpty())
				return false;
		}

		chance &= (r.nextFloat() < .8);
		
		Ability_Sub subAb = sa.getSubAbility();
		if (subAb != null)
			chance &= subAb.chkAI_Drawback();
		
		return chance;
	}
	
	private static boolean changeHiddenOriginPlayDrawbackAI(AbilityFactory af, SpellAbility sa){
		// if putting cards from hand to library and parent is drawing cards
		// make sure this will actually do something:
		Target tgt = af.getAbTgt();
		if(tgt != null && tgt.canTgtPlayer()) {
			if (af.isCurse())
				tgt.addTarget(AllZone.HumanPlayer);
			else
				tgt.addTarget(AllZone.ComputerPlayer);
		}
		
		return true;
	}
	
	private static boolean changeHiddenTriggerAI(AbilityFactory af, SpellAbility sa, boolean mandatory){
		// Fetching should occur fairly often as it helps cast more spells, and have access to more mana
		if (!ComputerUtil.canPayCost(sa))
			return false;
		
		Card source = sa.getSourceCard();
		
		HashMap<String,String> params = af.getMapParams();
        //String destination = params.get("Destination");
        String origin = params.get("Origin");
		
		// this works for hidden because the mana is paid first. 
		String type = params.get("ChangeType");
		if (type != null && type.contains("X") && source.getSVar("X").equals("Count$xPaid")){
			// Set PayX here to maximum value.
			int xPay = ComputerUtil.determineLeftoverMana(sa);
			source.setSVar("PayX", Integer.toString(xPay));
		}
        
		ArrayList<Player> pDefined;
		Target tgt = af.getAbTgt();
		if(tgt != null && tgt.canTgtPlayer()) {
			if (af.isCurse()){
				if (AllZone.HumanPlayer.canTarget(source))
					tgt.addTarget(AllZone.HumanPlayer);
				else if (mandatory && AllZone.ComputerPlayer.canTarget(source))
					tgt.addTarget(AllZone.ComputerPlayer);
			}
			else{
				if (AllZone.ComputerPlayer.canTarget(source))
					tgt.addTarget(AllZone.ComputerPlayer);
				else if (mandatory && AllZone.HumanPlayer.canTarget(source))
					tgt.addTarget(AllZone.HumanPlayer);
			}

			pDefined = tgt.getTargetPlayers();
			
			if (pDefined.isEmpty())
				return false;
			
			if (mandatory){
				return pDefined.size() > 0;
			}
		}
		else{
			if (mandatory)
				return true;
			pDefined = AbilityFactory.getDefinedPlayers(sa.getSourceCard(),  params.get("Defined"), sa);
		}
		
		for(Player p : pDefined){
			CardList list = AllZoneUtil.getCardsInZone(origin, p);

			if (p.isComputer())	// Computer should "know" his deck
				list = filterListByType(list, params, sa);
			
			if (list.isEmpty())
				return false;
		}
		
		Ability_Sub subAb = sa.getSubAbility();
		if (subAb != null)
			return subAb.doTrigger(mandatory);
		
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
		
		if (params.containsKey("StackDescription"))
			sb.append(params.get("StackDescription"));

		else{
			String origin = params.get("Origin");
			String destination = params.get("Destination");
	
			String type = params.containsKey("ChangeType") ? params.get("ChangeType") : "Card";
			int num = params.containsKey("ChangeNum") ? AbilityFactory.calculateAmount(host, params.get("ChangeNum"), sa) : 1;
	
			if (origin.equals("Library")){
				sb.append("Search your library for ").append(num).append(" ").append(type).append(" and ");
	
				if (params.get("ChangeNum").equals("1"))
					sb.append("put that card ");
				else
					sb.append("put those cards ");
	
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
				sb.append("Put ").append(num).append(" ").append(type).append(" card(s) from your hand ");
	
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
			else if (origin.equals("Battlefield")){
				// TODO: Expand on this Description as more cards use it
				// for the non-targeted SAs when you choose what is returned on resolution
				sb.append("Return ").append(num).append(" ").append(type).append(" card(s) ");
				sb.append(" to your ").append(destination);
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
		Player chooser = null;
		if (params.containsKey("Chooser")) {
			String choose = params.get("Chooser"); 
			if (choose.equals("Targeted") && af.getAbTgt().getTargetPlayers() != null)
				chooser = af.getAbTgt().getTargetPlayers().get(0);
			else 
				chooser = AbilityFactory.getDefinedPlayers(sa.getSourceCard(), choose, sa).get(0);
		}

		for(Player player : fetchers){
			Player decider = chooser;
			if (decider == null) 
				decider = player;
			if (decider.isComputer()){
				changeHiddenOriginResolveAI(af, sa, player);
			}
			else{
				changeHiddenOriginResolveHuman(af, sa, player);
			}
		}
	}
	
	private static void changeHiddenOriginResolveHuman(AbilityFactory af, SpellAbility sa, Player player){
		HashMap<String,String> params = af.getMapParams();
        Card card = af.getHostCard();
		Target tgt = af.getAbTgt();
		if (tgt != null){
			ArrayList<Player> players = tgt.getTargetPlayers();
			player = players.get(0);
			if (players.contains(player) && !player.canTarget(sa.getSourceCard()))
				return;
		}

		String origin = params.get("Origin");
        String destination = params.get("Destination");
        // this needs to be zero indexed. Top = 0, Third = 2
        int libraryPos = params.containsKey("LibraryPosition") ? Integer.parseInt(params.get("LibraryPosition")) : 0;
		
        if (params.containsKey("OriginChoice")){
        	// Currently only used for Mishra, but may be used by other things
        	// Improve how this message reacts for other cards
        	String alt = params.get("OriginAlternative");
        	CardList altFetchList = AllZoneUtil.getCardsInZone(alt, player);
        	altFetchList = filterListByType(altFetchList, params, sa);
        	
        	StringBuilder sb = new StringBuilder();
        	sb.append(params.get("AlternativeMessage")).append(" ");
        	sb.append(altFetchList.size()).append(" cards match your searching type in Alternate Zones.");
        	
        	if (!GameActionUtil.showYesNoDialog(card, sb.toString()))
        		origin = alt;
        }
        
        if (params.containsKey("DestinationAlternative")){
        	
        	StringBuilder sb = new StringBuilder();
        	sb.append(params.get("AlternativeDestinationMessage"));
        	
        	if (!GameActionUtil.showYesNoDialog(card, sb.toString())) {
        		destination = params.get("DestinationAlternative");
        		libraryPos = params.containsKey("LibraryPositionAlternative") ? Integer.parseInt(params.get("LibraryPositionAlternative")) : 0;
        	}
        }
        

		CardList fetchList = AllZoneUtil.getCardsInZone(origin, player);
        if (origin.contains("Library"))	// Look at whole library before moving onto choosing a card{
        	GuiUtils.getChoiceOptional(af.getHostCard().getName() + " - Looking at Library", AllZoneUtil.getCardsInZone("Library", player).toArray());
        
        if (origin.contains("Hand") && player.isComputer())	// Look at opponents hand before moving onto choosing a card
        	GuiUtils.getChoiceOptional(af.getHostCard().getName() + " - Looking at Opponent's Hand", AllZoneUtil.getCardsInZone("Hand", player).toArray());
		
		fetchList = filterListByType(fetchList, params, sa);

        PlayerZone destZone = AllZone.getZone(destination, player);
        
        int changeNum = params.containsKey("ChangeNum") ? AbilityFactory.calculateAmount(card, params.get("ChangeNum"), sa) : 1;

        String remember = params.get("RememberChanged");
        
        for (int i=0; i < changeNum; i++) {
            if (fetchList.size() == 0 || destination == null) 
                break;
                
            Object o;
            if (params.containsKey("Mandatory"))
                o = GuiUtils.getChoice("Select a card", fetchList.toArray());
            else
            	o = GuiUtils.getChoiceOptional("Select a card", fetchList.toArray());
            
            if (o != null) {
                Card c = (Card) o;
                fetchList.remove(c);
                if (remember != null)
                	card.addRemembered(c);

                if (destination.equals("Library")) {
                    // do not shuffle the library once we have placed a fetched card on top.
                    if (origin.contains("Library") && i < 1) {
                        player.shuffle();
                    }
                    AllZone.GameAction.moveToLibrary(c, libraryPos);
                }
                else if (destination.equals("Battlefield")){
		        		if (params.containsKey("Tapped"))
		        			c.tap();
		        		if (params.containsKey("GainControl"))
		        			c.setController(sa.getActivatingPlayer());
		        	
		        		AllZone.GameAction.moveTo(AllZone.getZone(destination, c.getController()),c);
		    		}
		    	else
                	AllZone.GameAction.moveTo(destZone, c);
                
                //for imprinted since this doesn't use Target
                if(params.containsKey("Imprint")) card.addImprinted(c);
            }
            else{
            	StringBuilder sb = new StringBuilder();
            	int num = Math.min(fetchList.size(), changeNum - i);
            	sb.append("Cancel Search? Up to ").append(num).append(" more cards can change zones.");
            	
            	if (i+1 == changeNum || GameActionUtil.showYesNoDialog(card, sb.toString()))
            		break;
            }
        }
        
        if ((origin.contains("Library") && !destination.equals("Library")) || params.containsKey("Shuffle"))
            player.shuffle();
	}
	
	private static void changeHiddenOriginResolveAI(AbilityFactory af, SpellAbility sa, Player player){
		HashMap<String,String> params = af.getMapParams();
		Target tgt = af.getAbTgt();
		Card card = af.getHostCard();
		
		if (tgt != null){
			if(!tgt.getTargetPlayers().isEmpty()) {
				player = tgt.getTargetPlayers().get(0);
				if (!player.canTarget(sa.getSourceCard()))
					return;
			}
		}
		
		String origin = params.get("Origin");
		
		CardList fetchList = AllZoneUtil.getCardsInZone(origin, player);
		fetchList = filterListByType(fetchList, params, sa);
		
        String destination = params.get("Destination");
		
        PlayerZone destZone = AllZone.getZone(destination, player);

        String type = params.get("ChangeType");
        if (type == null)
        	type = "Card";
		
        CardList fetched = new CardList();
        
        int changeNum = params.containsKey("ChangeNum") ? AbilityFactory.calculateAmount(card, params.get("ChangeNum"), sa) : 1;

        String remember = params.get("RememberChanged");
        
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
        	else if ("Battlefield".equals(destination) || "Graveyard".equals(destination))
        		c = CardFactoryUtil.AI_getMostExpensivePermanent(fetchList, af.getHostCard(), false);
        	else if ("Exile".equals(destination)){
        		// Exiling your own stuff, if Exiling opponents stuff choose best
        		if (destZone.getPlayer().isHuman())
        			c = CardFactoryUtil.AI_getMostExpensivePermanent(fetchList, af.getHostCard(), false);
        		else
        			c = CardFactoryUtil.AI_getCheapestPermanent(fetchList, af.getHostCard(), false);
        	}
        	else{
        		//Don't fetch another tutor with the same name
            	if (origin.contains("Library") && !fetchList.getNotName(card.getName()).isEmpty())
            		fetchList = fetchList.getNotName(card.getName());
            	
        		fetchList.shuffle();
        		c = fetchList.get(0);
        	}

            if (remember != null)
            	card.addRemembered(c);
        	fetched.add(c);
        	fetchList.remove(c);
        }
        
        if (origin.contains("Library"))
        	player.shuffle();
        
        for(Card c : fetched){
        	if ("Library".equals(destination)){
        		int libraryPos = params.containsKey("LibraryPosition") ? Integer.parseInt(params.get("LibraryPosition")) : 0;
        		AllZone.GameAction.moveToLibrary(c, libraryPos);
        	}
        	else if ("Battlefield".equals(destination)){
        		if (params.containsKey("Tapped"))
        			c.tap();
        		if (params.containsKey("GainControl"))
        			c.setController(sa.getActivatingPlayer());

        		AllZone.GameAction.moveTo(AllZone.getZone(destination, c.getController()),c);
        	}
        	else
        		AllZone.GameAction.moveTo(destZone, c);
        	
        	//for imprinted since this doesn't use Target
            if(params.containsKey("Imprint")) card.addImprinted(c);
        }
        
        if (!"Battlefield".equals(destination) && !"Card".equals(type)){
        	String picked = af.getHostCard().getName() + " - Computer picked:";
        	if (fetched.size() > 0)
        		GuiUtils.getChoice(picked, fetched.toArray());
        	else
        		GuiUtils.getChoice(picked, new String[]{ "<Nothing>" } );
        }
	}
	
	// *********** Utility functions for Hidden ********************
	private static CardList filterListByType(CardList list, HashMap<String,String> params, SpellAbility sa){
		String type = params.get("ChangeType");
		if (type == null)
			return list;
		
		// Filter List Can send a different Source card in for things like Mishra and Lobotomy
		
		Card source = sa.getSourceCard();
		if (type.contains("Triggered")){
			Object o = sa.getTriggeringObject("Card");
			
			// I won't the card attached to the Triggering object
			if (!(o instanceof Card))
				return new CardList();
			
			source = (Card)(o);
			type = type.replace("Triggered", "Card");
		}
		else if (type.contains("Remembered")){
			boolean hasRememberedCard = false;
			for(Object o : source.getRemembered()){
				if (o instanceof Card){
					hasRememberedCard = true;
					source = (Card)o;
					type = type.replace("Remembered", "Card");
					break;
				}
			}

			if (!hasRememberedCard)
				return new CardList();
		}
		
		return list.getValidCards(type.split(","), sa.getActivatingPlayer(), source);
	}
	
	private static Card basicManaFixing(CardList list){	// Search for a Basic Land
		return basicManaFixing(list, "Plains, Island, Swamp, Mountain, Forest");
	}
	
	private static Card basicManaFixing(CardList list, String type){	// type = basic land types
        CardList combined = AllZoneUtil.getPlayerCardsInPlay(AllZone.ComputerPlayer);
        combined.addAll(AllZoneUtil.getPlayerHand(AllZone.ComputerPlayer));
        
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
		Cost abCost = af.getAbCost();
		final Card source = af.getHostCard();
		HashMap<String,String> params = af.getMapParams();

		String origin = params.get("Origin");
        
		float pct = origin.equals("Battlefield") ? .8f : .667f;
		
		Random r = MyRandom.random;
		
		if (abCost != null){
			// AI currently disabled for these costs
			if (abCost.getSacCost() && !abCost.getSacThis()){
				//only sacrifice something that's supposed to be sacrificed 
				String type = abCost.getSacType();
			    CardList typeList = AllZoneUtil.getPlayerCardsInPlay(AllZone.ComputerPlayer);
			    typeList = typeList.getValidCards(type.split(","), source.getController(), source);
			    if(ComputerUtil.getCardPreference(source, "SacCost", typeList) == null)
			    	return false;
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
			if (!changeKnownPreferredTarget(af, sa, false))
				return false;
		}
		else{
			// non-targeted retrieval
			CardList retrieval = null;
			if (af.getMapParams().containsKey("Defined")){
				// add hooks into AF_Defined function
				retrieval = knownDetermineDefined(sa, params.get("Defined"), origin);
			}
			
			if (retrieval == null)
				return false;
			
			if (retrieval.get(0) == source){
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
		if (sa.getTarget() == null)
			return true;
		
		return changeKnownPreferredTarget(af, sa, false);
	}
	
	private static boolean changeKnownPreferredTarget(AbilityFactory af, SpellAbility sa, boolean mandatory){
		HashMap<String,String> params = af.getMapParams();
		Card source = sa.getSourceCard();
		String origin = params.get("Origin");
		String destination = params.get("Destination");
		Target tgt = af.getAbTgt();
		
		if (tgt != null)
			tgt.resetTargets();

		CardList list = AllZoneUtil.getCardsInZone(origin);
		list = list.getValidCards(tgt.getValidTgts(), AllZone.ComputerPlayer, source);
		
		if (list.size() < tgt.getMinTargets(sa.getSourceCard(), sa))
			return false;
		
		// Narrow down the list:
		if (origin.equals("Battlefield")){
			// filter out untargetables
			list = list.getTargetableCards(source);
			
			// if Destination is hand, either bounce opponents dangerous stuff or save my about to die stuff
			
			// if Destination is exile, filter out my cards
		}
		else if (origin.equals("Graveyard")){
			// Retrieve from Graveyard to:
			
		}
		
		// for now only bounce opponents stuff, but consider my stuff that might die
		if (destination.equals("Exile") || origin.equals("Battlefield"))
			list = list.getController(AllZone.HumanPlayer);

		if (list.isEmpty())
			return false;
		
		if (!mandatory && list.size() < tgt.getMinTargets(sa.getSourceCard(), sa))
			return false;
		
		 // target loop
		while(tgt.getNumTargeted() < tgt.getMaxTargets(sa.getSourceCard(), sa)){ 
			// AI Targeting 
			Card choice = null;

			if (!list.isEmpty()){
				Card mostExpensive = CardFactoryUtil.AI_getMostExpensivePermanent(list, af.getHostCard(), false);
				if (destination.equals("Battlefield") || origin.equals("Battlefield")){
					if (mostExpensive.isCreature()){
						//if a creature is most expensive take the best one
		        		if (destination.equals("Exile"))	// If Exiling things, don't give bonus to Tokens
		        			choice = CardFactoryUtil.AI_getBestCreature(list);
		        		else
		        			choice = CardFactoryUtil.AI_getBestCreatureToBounce(list); 
					}
					else
						choice = mostExpensive;
				}
	        	else{
					// TODO: AI needs more improvement to it's retrieval (reuse some code from spReturn here)
	        		list.shuffle();
	        		choice = list.get(0);
	        	}
			}
			if (choice == null){	// can't find anything left
				if (tgt.getNumTargeted() == 0 || tgt.getNumTargeted() < tgt.getMinTargets(sa.getSourceCard(), sa)){
					if (!mandatory)
						tgt.resetTargets();
					return false;
				}
				else{
					// TODO is this good enough? for up to amounts?
					break;
				}
			}
			
			list.remove(choice);
			tgt.addTarget(choice);
		}
		
		return true;
	}
	
	private static boolean changeKnownUnpreferredTarget(AbilityFactory af, SpellAbility sa, boolean mandatory){
		if (!mandatory)
			return false;
		
		HashMap<String,String> params = af.getMapParams();
		Card source = sa.getSourceCard();
		String origin = params.get("Origin");
		String destination = params.get("Destination");
		Target tgt = af.getAbTgt();

		CardList list = AllZoneUtil.getCardsInZone(origin);
		list = list.getValidCards(tgt.getValidTgts(), AllZone.ComputerPlayer, source);
		
		
		// Narrow down the list:
		if (origin.equals("Battlefield")){
			// filter out untargetables
			list = list.getTargetableCards(source);
			
			// if Destination is hand, either bounce opponents dangerous stuff or save my about to die stuff
			
			// if Destination is exile, filter out my cards
		}
		else if (origin.equals("Graveyard")){
			// Retrieve from Graveyard to:
			
		}
		
		for(Card c : tgt.getTargetCards())
			list.remove(c);

		if (list.isEmpty())
			return false;
		
		 // target loop
		while(tgt.getNumTargeted() < tgt.getMinTargets(sa.getSourceCard(), sa)){ 
			// AI Targeting 
			Card choice = null;

			if (!list.isEmpty()){
				if (CardFactoryUtil.AI_getMostExpensivePermanent(list, af.getHostCard(), false).isCreature() 
						&& (destination.equals("Battlefield") || origin.equals("Battlefield")))
	        		choice = CardFactoryUtil.AI_getBestCreatureToBounce(list); //if a creature is most expensive take the best
	        	else if (destination.equals("Battlefield") || origin.equals("Battlefield"))
	        		choice = CardFactoryUtil.AI_getMostExpensivePermanent(list, af.getHostCard(), false);
	        	else{
					// TODO: AI needs more improvement to it's retrieval (reuse some code from spReturn here)
	        		list.shuffle();
	        		choice = list.get(0);
	        	}
			}
			if (choice == null){	// can't find anything left
				if (tgt.getNumTargeted() == 0 || tgt.getNumTargeted() < tgt.getMinTargets(sa.getSourceCard(), sa)){
					tgt.resetTargets();
					return false;
				}
				else{
					// TODO is this good enough? for up to amounts?
					break;
				}
			}
			
			list.remove(choice);
			tgt.addTarget(choice);
		}
		
		return true;
	}
	
	private static boolean changeKnownOriginTriggerAI(AbilityFactory af, SpellAbility sa, boolean mandatory){
		if (!ComputerUtil.canPayCost(sa))
			return false;
		
		if (sa.getTarget() == null)	// Just in case of Defined cases
			; // do nothing
		else if (changeKnownPreferredTarget(af, sa, mandatory)){
			; // do nothing
		}
		else if (!changeKnownUnpreferredTarget(af, sa, mandatory))
			return false;

		Ability_Sub subAb = sa.getSubAbility();
		if (subAb != null)
			return subAb.doTrigger(mandatory);
		
		return true;
	}
	
	
	private static String changeKnownOriginStackDescription(AbilityFactory af, SpellAbility sa){
		HashMap<String,String> params = af.getMapParams();

		 StringBuilder sb = new StringBuilder();
		 Card host = af.getHostCard();
		 
		 if (!(sa instanceof Ability_Sub))
			 sb.append(host.getName()).append(" -");
		 
		 sb.append(" ");
		 
		String destination = params.get("Destination");
		String origin = params.get("Origin");
		 
		 StringBuilder sbTargets = new StringBuilder();

		 ArrayList<Card> tgts;
		 if (af.getAbTgt() != null)
			 tgts = af.getAbTgt().getTargetCards();
		 else{
			 // otherwise add self to list and go from there
			 tgts = new ArrayList<Card>();
			 for(Card c : knownDetermineDefined(sa, params.get("Defined"), origin))
			 {
				 tgts.add(c);
			 }
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
			    	sb.append(" ").append(libraryPosition+1).append(" from the top of").append(pronoun).append("owner's library.");
	    	 }
	     }
		 
		 if(destination.equals("Exile")){
			 sb.append("Exile").append(targetname);
			 if (origin.equals("Graveyard"))
				 sb.append(fromGraveyard);
			 sb.append(".");
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
			for(Card c : knownDetermineDefined(sa, params.get("Defined"), origin))
			{
				tgtCards.add(c);
			}
		}

		if(tgtCards.size() != 0) 
		{
			for(Card tgtC : tgtCards){
				PlayerZone originZone = AllZone.getZone(tgtC);
				// if Target isn't in the expected Zone, continue
				if (!originZone.is(origin))
					continue;
		        
		        if (tgt != null && origin.equals("Battlefield")){
		        	// check targeting
		        	if (!CardFactoryUtil.canTarget(sa.getSourceCard(), tgtC))
		        		continue;
		        }

		    	Player pl = player;
		    	if (!destination.equals("Battlefield"))
	    			pl = tgtC.getOwner();
	    	
	    		if (destination.equals("Library")){
	    			// library position is zero indexed
	    			int libraryPosition = params.containsKey("LibraryPosition") ? Integer.parseInt(params.get("LibraryPosition")) : 0;        

	    			AllZone.GameAction.moveToLibrary(tgtC, libraryPosition);

	    			if (params.containsKey("Shuffle"))	// for things like Gaea's Blessing
	    				tgtC.getOwner().shuffle();
	    		}
	    		else{
		    		if (destination.equals("Battlefield")){
		        		if (params.containsKey("Tapped") || params.containsKey("Ninjutsu"))
		        			tgtC.tap();
		        		if (params.containsKey("GainControl"))
		        			tgtC.setController(sa.getActivatingPlayer());
		        	
		        		AllZone.GameAction.moveTo(AllZone.getZone(destination, tgtC.getController()),tgtC);
		        		
		        		if(params.containsKey("Ninjutsu") || params.containsKey("Attacking")) {
		        			AllZone.Combat.addAttacker(tgtC);
		        			AllZone.Combat.addUnblockedAttacker(tgtC);
		        		}
		    		}
		    		else
		    		{
		    			AllZone.GameAction.moveTo(AllZone.getZone(destination, pl), tgtC);
		    		}
	    		}
			}
		}
	}
	
	// **************************** Known Utility **************************************
	private static CardList knownDetermineDefined(SpellAbility sa, String defined, String origin){
		// TODO: this function should return a ArrayList<Card> and then be handled by the callees
		CardList grave = AllZoneUtil.getCardsInZone(origin, sa.getActivatingPlayer());
		CardList ret = new CardList();
		
		if (defined != null && defined.equals("Top")){
			// the "top" of the graveyard, is the last to be added to the graveyard list?
			if (grave.size() == 0)
				return null;
			ret.add(grave.get(grave.size()-1));
			
			return ret;
		}
		
		ret.addAll(AbilityFactory.getDefinedCards(sa.getSourceCard(), defined, sa).toArray());
		return ret;
	}

	// *************************************************************************************
	// ************************** ChangeZoneAll ********************************************
	// ************ All is non-targeted and should occur similarly to Hidden ***************
	// ******* Instead of choosing X of type on resolution, all on type go *****************
	// *************************************************************************************
	public static SpellAbility createAbilityChangeZoneAll(final AbilityFactory AF){
		final SpellAbility abChangeZone = new Ability_Activated(AF.getHostCard(), AF.getAbCost(), AF.getAbTgt()){
			private static final long serialVersionUID = 3728332812890211671L;

			public boolean canPlayAI(){
				return changeZoneAllCanPlayAI(AF, this);
			}

			@Override
			public void resolve() {
				changeZoneAllResolve(AF, this);
			}
			
			@Override
			public String getStackDescription(){
				return changeZoneAllDescription(AF, this);
			}

			@Override
			public boolean doTrigger(boolean mandatory) {
				return changeZoneAllCanPlayAI(AF, this);
			}
		
		};
		setMiscellaneous(AF, abChangeZone);
		return abChangeZone;
	}
	
	public static SpellAbility createSpellChangeZoneAll(final AbilityFactory AF){
		final SpellAbility spChangeZone = new Spell(AF.getHostCard(), AF.getAbCost(), AF.getAbTgt()) {
			private static final long serialVersionUID = 3270484211099902059L;

			public boolean canPlayAI(){
				return changeZoneAllCanPlayAI(AF, this);
			}
			
			@Override
			public void resolve() {
				changeZoneAllResolve(AF, this);
			}
			
			@Override
			public String getStackDescription(){
				return changeZoneAllDescription(AF, this);
			}
		};
		setMiscellaneous(AF, spChangeZone);
		return spChangeZone;
	}
	
	public static SpellAbility createDrawbackChangeZoneAll(final AbilityFactory AF){
		final SpellAbility dbChangeZone = new Ability_Sub(AF.getHostCard(), AF.getAbTgt()) {
			private static final long serialVersionUID = 3270484211099902059L;

			@Override
			public void resolve() {
				changeZoneAllResolve(AF, this);
			}

			@Override
			public boolean chkAI_Drawback() {
				return changeZoneAllPlayDrawbackAI(AF, this);
			}
			
			@Override
			public String getStackDescription(){
				return changeZoneAllDescription(AF, this);
			}

			@Override
			public boolean doTrigger(boolean mandatory) {
				return changeZoneAllCanPlayAI(AF, this);
			}
		};
		setMiscellaneous(AF, dbChangeZone);
		return dbChangeZone;
	}
	

	private static boolean changeZoneAllCanPlayAI(AbilityFactory af, SpellAbility sa){
		// Change Zone All, can be any type moving from one zone to another
		Cost abCost = af.getAbCost();
		Card source = af.getHostCard();
		HashMap<String,String> params = af.getMapParams();
        String destination = params.get("Destination");
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
			
			if (abCost.getSubCounter())
				;	// subcounter is fine
			
		}
		
		if (!ComputerUtil.canPayCost(sa))
			return false;
		
		Random r = MyRandom.random;
		// prevent run-away activations - first time will always return true
		boolean chance = r.nextFloat() <= Math.pow(.6667, source.getAbilityUsed());

		// TODO: targeting with ChangeZoneAll
		// really two types of targeting. 
		// Target Player has all their types change zones
		// or target permanent and do something relative to that permanent
		// ex. "Return all Auras attached to target"
		// ex. "Return all blocking/blocked by target creature" 
		
		CardList humanType = AllZoneUtil.getCardsInZone(origin, AllZone.HumanPlayer);
		humanType = filterListByType(humanType, params, sa);
		CardList computerType = AllZoneUtil.getCardsInZone(origin, AllZone.ComputerPlayer);
		computerType = filterListByType(computerType, params, sa);

		// TODO: improve restrictions on when the AI would want to use this
		// spBounceAll has some AI we can compare to. 
		if (origin.equals("Hand")){

		}
		else if (origin.equals("Library")){

		}
		else if (origin.equals("Battlefield")){
			// this statement is assuming the AI is trying to use this spell offensively
			// if the AI is using it defensively, then something else needs to occur
			// if only creatures are affected evaluate both lists and pass only if human creatures are more valuable
			 if (humanType.getNotType("Creature").size() == 0 && computerType.getNotType("Creature").size() == 0) {
				 if(CardFactoryUtil.evaluateCreatureList(computerType) + 200 >= CardFactoryUtil.evaluateCreatureList(humanType))
					 return false;
			 } // otherwise evaluate both lists by CMC and pass only if human permanents are more valuable
			 else if(CardFactoryUtil.evaluatePermanentList(computerType) + 3 >= CardFactoryUtil.evaluatePermanentList(humanType))
				 return false;
			
			// Don't cast during main1?
			if (AllZone.Phase.is(Constant.Phase.Main1, AllZone.ComputerPlayer))
				return false;
		}
		else if (origin.equals("Graveyard")){
			Target tgt = af.getAbTgt();
			if (tgt != null){
				if(AllZoneUtil.getPlayerGraveyard(AllZone.HumanPlayer).isEmpty())
					return false;
				tgt.resetTargets();
				tgt.addTarget(AllZone.HumanPlayer);
			}
		}
		else if (origin.equals("Exile")){

		}
		else if (origin.equals("Stack")){
			// time stop can do something like this:
			// Origin$ Stack | Destination$ Exile | SubAbility$ DBSkip
			// DBSKipToPhase | DB$SkipToPhase | Phase$ Cleanup
			// otherwise, this situation doesn't exist
			return false;
		}
		
		else if (origin.equals("Sideboard")){
			// This situation doesn't exist
			return false;
		}
		
		if (destination.equals(Constant.Zone.Battlefield)){
			if (params.get("GainControl") != null){
				// Check if the cards are valuable enough
				if (humanType.getNotType("Creature").size() == 0 && computerType.getNotType("Creature").size() == 0) {
					 if(CardFactoryUtil.evaluateCreatureList(computerType) + CardFactoryUtil.evaluateCreatureList(humanType) < 400)
						 return false;
				 } // otherwise evaluate both lists by CMC and pass only if human permanents are less valuable
				 else if(CardFactoryUtil.evaluatePermanentList(computerType) + CardFactoryUtil.evaluatePermanentList(humanType) < 6)
					return false;
			}
			else{
				// don't activate if human gets more back than AI does
				if (humanType.getNotType("Creature").size() == 0 && computerType.getNotType("Creature").size() == 0) {
					 if(CardFactoryUtil.evaluateCreatureList(computerType) <= CardFactoryUtil.evaluateCreatureList(humanType) + 100)
						 return false;
				 } // otherwise evaluate both lists by CMC and pass only if human permanents are less valuable
				 else if(CardFactoryUtil.evaluatePermanentList(computerType) <= CardFactoryUtil.evaluatePermanentList(humanType) + 2)
					return false;
			}
		}
		
		Ability_Sub subAb = sa.getSubAbility();
		if (subAb != null)
			chance &= subAb.chkAI_Drawback();
		
		return ((r.nextFloat() < .8) && chance);
	}
	
	private static boolean changeZoneAllPlayDrawbackAI(AbilityFactory af, SpellAbility sa){
		// if putting cards from hand to library and parent is drawing cards
		// make sure this will actually do something:
		
		
		return true;
	}
	
	private static String changeZoneAllDescription(AbilityFactory af, SpellAbility sa){
		// TODO: build Stack Description will need expansion as more cards are added
		 StringBuilder sb = new StringBuilder();
		 Card host = af.getHostCard();
		 
		 if (!(sa instanceof Ability_Sub))
			 sb.append(host.getName()).append(" -");
		 
		 sb.append(" ");
		 
		 String[] desc = sa.getDescription().split(":");
		 
		 if (desc.length > 1)
			 sb.append(desc[1]);
		 else
			 sb.append(desc[0]);
		 
		 Ability_Sub abSub = sa.getSubAbility();
		 if (abSub != null) {
		 	sb.append(abSub.getStackDescription());
		 }
		 
		 return sb.toString();
	}
	
	private static void changeZoneAllResolve(AbilityFactory af, SpellAbility sa){
		HashMap<String,String> params = af.getMapParams();
        String destination = params.get("Destination");
        String origin = params.get("Origin");
		
		CardList cards = null;
		
		ArrayList<Player> tgtPlayers = null;

		Target tgt = af.getAbTgt();
		if (tgt != null)
			tgtPlayers = tgt.getTargetPlayers();
		else if (params.containsKey("Defined"))		// Make sure Defined exists to use it
			tgtPlayers = AbilityFactory.getDefinedPlayers(sa.getSourceCard(), params.get("Defined"), sa);
		
		if (tgtPlayers == null || tgtPlayers.isEmpty())
			cards = AllZoneUtil.getCardsInZone(origin);
		else
			cards = AllZoneUtil.getCardsInZone(origin,tgtPlayers.get(0));			
		
		cards = filterListByType(cards, params, sa);
		
		if (params.containsKey("ForgetOtherRemembered"))
			sa.getSourceCard().clearRemembered();
		
		String remember = params.get("RememberChanged");
		
		// I don't know if library position is necessary. It's here if it is, just in case
		int libraryPos = params.containsKey("LibraryPosition") ? Integer.parseInt(params.get("LibraryPosition")) : 0;
		for(Card c : cards){
			if (destination.equals("Battlefield") && params.containsKey("Tapped"))
                c.tap();
			if (params.containsKey("GainControl")){
				c.setController(sa.getActivatingPlayer());
				AllZone.GameAction.moveToPlay(c, sa.getActivatingPlayer());
			}
			else
				AllZone.GameAction.moveTo(destination, c, libraryPos);
			
			if (remember != null)
				sa.getSourceCard().addRemembered(c);
		}
		
		// if Shuffle parameter exists, and any amount of cards were owned by that player, then shuffle that library
		if (params.containsKey("Shuffle")){
			if (cards.getOwner(AllZone.HumanPlayer).size() > 0)
				AllZone.HumanPlayer.shuffle();
			if (cards.getOwner(AllZone.ComputerPlayer).size() > 0)
				AllZone.ComputerPlayer.shuffle();
		}
	}
	

}
