package forge;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
//import java.util.HashMap;
//import java.util.Map;


public class ComputerUtil
{

  //if return true, go to next phase
  static public boolean playCards()
  {
    return playCards(getSpellAbility());
  }

  //if return true, go to next phase
  static public boolean playCards(SpellAbility[] all)
  {
    //not sure "playing biggest spell" matters?
	    sortSpellAbilityByCost(all);
	//    MyRandom.shuffle(all);
	
	    for(int i = 0; i < all.length; i++)
	    {
	    	all[i].setActivatingPlayer(AllZone.ComputerPlayer);
	    	if(canPayCost(all[i]) && all[i].canPlay() && all[i].canPlayAI())
	    	{
		    	AllZone.Stack.freezeStack();
	    		if(all[i].isSpell() && AllZone.GameAction.isCardInZone(all[i].getSourceCard(),AllZone.Computer_Hand))
		        	AllZone.Computer_Hand.remove(all[i].getSourceCard());
		
		        Ability_Cost cost = all[i].getPayCosts();
		        Target tgt = all[i].getTarget();
		        
		        if (cost == null){
			        if(all[i] instanceof Ability_Tap)
			        	all[i].getSourceCard().tap();
			
			        payManaCost(all[i]);
			        all[i].chooseTargetAI();
			        all[i].getBeforePayManaAI().execute();
			        AllZone.Stack.addAndUnfreeze(all[i]);
		        }
		        else{
		        	if (tgt != null && tgt.doesTarget())
		        		all[i].chooseTargetAI();
		        	
		        	Cost_Payment pay = new Cost_Payment(cost, all[i]);
		        	pay.payComputerCosts();
		        }
		
		        return false;
	    	}
	    }//while
	    return true;
  }//playCards()
  
  //this is used for AI's counterspells
  final static public void playStack(SpellAbility sa)
  {
	  if (canPayCost(sa))
	  {
		  if (AllZone.GameAction.isCardInZone(sa.getSourceCard(),AllZone.Computer_Hand))
	    		AllZone.Computer_Hand.remove(sa.getSourceCard());
	  
		  sa.setActivatingPlayer(AllZone.ComputerPlayer);
	  
		  if (sa.getSourceCard().getKeyword().contains("Draw a card."))
			  sa.getSourceCard().getController().drawCard();
		  if (sa.getSourceCard().getKeyword().contains("Draw a card at the beginning of the next turn's upkeep."))
			  sa.getSourceCard().getController().addSlowtripList(sa.getSourceCard());
		  payManaCost(sa);
		  
		  AllZone.Stack.add(sa);
	  }
  }
  
  final static public void playStackFree(SpellAbility sa)
  {
	  sa.setActivatingPlayer(AllZone.ComputerPlayer);
	  
	  if (AllZone.GameAction.isCardInZone(sa.getSourceCard(),AllZone.Computer_Hand))
		  AllZone.Computer_Hand.remove(sa.getSourceCard());
	  
	  
	  if (sa.getSourceCard().getKeyword().contains("Draw a card."))
		  sa.getSourceCard().getController().drawCard();
	  if (sa.getSourceCard().getKeyword().contains("Draw a card at the beginning of the next turn's upkeep."))
		  sa.getSourceCard().getController().addSlowtripList(sa.getSourceCard());
		  
	  AllZone.Stack.add(sa);
  }
  
  final static public void playNoStack(SpellAbility sa)
  {
    if(canPayCost(sa))
    {
      if(sa.isSpell())
      {
    	if (AllZone.GameAction.isCardInZone(sa.getSourceCard(),AllZone.Computer_Hand))
    		AllZone.Computer_Hand.remove(sa.getSourceCard());
        //probably doesn't really matter anyways
        //sa.getSourceCard().comesIntoPlay(); - messes things up, maybe for the future fix this
      }

      sa.setActivatingPlayer(AllZone.ComputerPlayer);
      
      if(sa instanceof Ability_Tap)
        sa.getSourceCard().tap();
      
      payManaCost(sa);
      // todo(sol): if sa has targets, if all of them are invalid, counter the spell
      sa.resolve();

      if (sa.getSourceCard().getKeyword().contains("Draw a card."))
    	  sa.getSourceCard().getController().drawCard();
	  if (sa.getSourceCard().getKeyword().contains("Draw a card at the beginning of the next turn's upkeep."))
		  sa.getSourceCard().getController().addSlowtripList(sa.getSourceCard());

      for (int i=0; i<sa.getSourceCard().getKeyword().size(); i++)
      {
      	String k = sa.getSourceCard().getKeyword().get(i);
      	if (k.startsWith("Scry"))
      	{
      		String kk[] = k.split(" ");
      		sa.getSourceCard().getController().scry(Integer.parseInt(kk[1]));
      	}
      }

      //destroys creatures if they have lethal damage, etc..
      AllZone.GameAction.checkStateEffects();
    }
  }//play()

  //gets Spells of cards in hand and Abilities of cards in play
  //checks to see
  //1. if canPlay() returns true, 2. can pay for mana
  static public SpellAbility[] getSpellAbility()
  {
    CardList all = new CardList();
    all.addAll(AllZone.Computer_Battlefield.getCards());
    all.addAll(AllZone.Computer_Hand.getCards());
    all.addAll(CardFactoryUtil.getGraveyardActivationCards(AllZone.ComputerPlayer).toArray());
    
    CardList humanPlayable = new CardList();
    humanPlayable.addAll(AllZone.Human_Battlefield.getCards());
    humanPlayable = humanPlayable.filter(new CardListFilter()
    {
      public boolean addCard(Card c)
      {
        return (c.canAnyPlayerActivate());
      }
    });
    
    all.addAll(humanPlayable.toArray());
    
    all = all.filter(new CardListFilter()
    {
      public boolean addCard(Card c)
      {
        if(c.isBasicLand())
          return false;

        return true;
      }
    });
    

    ArrayList<SpellAbility> spellAbility = new ArrayList<SpellAbility>();
    for(int outer = 0; outer < all.size(); outer++)
    {
      SpellAbility[] sa = all.get(outer).getSpellAbility();
      for(int i = 0; i < sa.length; i++)
        if(sa[i].canPlayAI() && canPayCost(sa[i]) /*&& sa[i].canPlay()*/)
          spellAbility.add(sa[i]);//this seems like it needs to be copied, not sure though
    }

    SpellAbility[] sa = new SpellAbility[spellAbility.size()];
    spellAbility.toArray(sa);
    return sa;
  }
  static public boolean canPlay(SpellAbility sa)
  {
    return sa.canPlayAI() && canPayCost(sa);
  }
  static public boolean canPayCost(SpellAbility sa)
  {
    CardList land = getAvailableMana();
   
    if(sa.getSourceCard().isLand() /*&& sa.isTapAbility()*/)
    {
       land.remove(sa.getSourceCard());
    }
 // Beached - Delete old
    String mana = sa.getPayCosts() != null ? sa.getPayCosts().getTotalMana() : sa.getManaCost();
    ManaCost cost = new ManaCost(mana);
    
    cost = AllZone.GameAction.getSpellCostChange(sa, cost);
    if(cost.isPaid())
        return canPayAdditionalCosts(sa);
 // Beached - Delete old
    ArrayList<String> colors;

    for(int i = 0; i < land.size(); i++)
    {
      colors = getColors(land.get(i));
      int once = 0;
     
      for(int j =0; j < colors.size(); j++)
      {
         if(cost.isNeeded(colors.get(j)) && once == 0)
         {
          //System.out.println(j + " color:" +colors.get(j));
           cost.payMana(colors.get(j));
           //System.out.println("thinking, I just subtracted " + colors.get(j) + ", cost is now: " + cost.toString());
           once++;
         }

         if(cost.isPaid()) {
            //System.out.println("Cost is paid.");
            return canPayAdditionalCosts(sa);
         }
      }
    }
    return false;
  }//canPayCost()
  
  static public boolean canPayAdditionalCosts(SpellAbility sa)
  {
	  	// Add additional cost checks here before attempting to activate abilities
		Ability_Cost cost = sa.getPayCosts();
		if (cost == null)
			return true;
	  	Card card = sa.getSourceCard();

    	if (cost.getTap() && (card.isTapped() || card.isSick()))
    		return false;
    	
    	if (cost.getUntap() && (card.isUntapped() || card.isSick()))
    		return false;
    	
		if (cost.getTapXTypeCost())
		{
			PlayerZone play = AllZone.getZone(Constant.Zone.Battlefield, AllZone.ComputerPlayer);
			CardList typeList = new CardList(play.getCards());
			typeList = typeList.getValidCards(cost.getTapXType().split(","),sa.getActivatingPlayer() ,sa.getSourceCard());
			
			if (cost.getTap())
				typeList.remove(sa.getSourceCard());
			typeList = typeList.filter( new CardListFilter() {
				public boolean addCard(Card c) {
					return c.isUntapped();
				}
			});
			
			if (cost.getTapXTypeAmount() > typeList.size())
				return false;
		}
    	
		if (cost.getSubCounter()){
			Counters c = cost.getCounterType();
			if (card.getCounters(c) - cost.getCounterNum() < 0 || !AllZone.GameAction.isCardInPlay(card)){
				return false;
			}
		}
		
		if (cost.getAddCounter()){
			// this should always be true
		}
		
		if (cost.getLifeCost()){
			if (AllZone.ComputerPlayer.getLife() <= cost.getLifeAmount())
				return false;
		}
	  
		if (cost.getDiscardCost()){
    		PlayerZone zone = AllZone.getZone(Constant.Zone.Hand, card.getController());
    		CardList handList = new CardList(zone.getCards());
    		String discType = cost.getDiscardType();
    		int discAmount = cost.getDiscardAmount();
    		
    		if (cost.getDiscardThis()){
    			if (!AllZone.getZone(card).getZoneName().equals(Constant.Zone.Hand))
    				return false;
    		}
    		else if( discType.equals("LastDrawn")) {
    			//compy can't yet use this effectively
    			return false;
    		}
    		else if (discType.equals("Hand")){
    			// this will always work
    		}
    		else{
    			if (!discType.equals("Any") && !discType.equals("Random")){
    				String validType[] = discType.split(",");
    				handList = handList.getValidCards(validType,sa.getActivatingPlayer() ,sa.getSourceCard());
    			}
	    		if (discAmount > handList.size()){
	    			// not enough cards in hand to pay
	    			return false;
	    		}
    		}
		}
		
		if (cost.getSacCost()){
			  // if there's a sacrifice in the cost, just because we can Pay it doesn't mean we want to. 
			if (!cost.getSacThis()){
			    PlayerZone play = AllZone.getZone(Constant.Zone.Battlefield, AllZone.ComputerPlayer);
			    CardList typeList = new CardList(play.getCards());
			    typeList = typeList.getValidCards(cost.getSacType().split(","),sa.getActivatingPlayer() ,sa.getSourceCard());
			    Card target = sa.getTargetCard();
				if (target != null && target.getController().equals(AllZone.ComputerPlayer)) // don't sacrifice the card we're pumping
					  typeList.remove(target);
				
				if (cost.getSacAmount() > typeList.size())
					return false;
			}
			else if (cost.getSacThis() && !AllZone.GameAction.isCardInPlay(card))
				return false;
		}
		
		if (cost.getExileCost()){
			  // if there's an exile in the cost, just because we can Pay it doesn't mean we want to. 
			if (!cost.getExileThis()){
			    PlayerZone play = AllZone.getZone(Constant.Zone.Battlefield, AllZone.ComputerPlayer);
			    CardList typeList = new CardList(play.getCards());
			    typeList = typeList.getValidCards(cost.getExileType().split(","),sa.getActivatingPlayer() ,sa.getSourceCard());
			    Card target = sa.getTargetCard();
				if (target != null && target.getController().equals(AllZone.ComputerPlayer)) // don't exile the card we're pumping
					  typeList.remove(target);
				
				if (cost.getExileAmount() > typeList.size())
					return false;
			}
			else if (cost.getExileThis() && !AllZone.GameAction.isCardInPlay(card))
				return false;
		}
		
		if (cost.getReturnCost()){
			  // if there's a return in the cost, just because we can Pay it doesn't mean we want to. 
			if (!cost.getReturnThis()){
			    PlayerZone play = AllZone.getZone(Constant.Zone.Battlefield, AllZone.ComputerPlayer);
			    CardList typeList = new CardList(play.getCards());
			    typeList = typeList.getValidCards(cost.getReturnType().split(","),sa.getActivatingPlayer() ,sa.getSourceCard());
			    Card target = sa.getTargetCard();
				if (target != null && target.getController().equals(AllZone.ComputerPlayer)) // don't bounce the card we're pumping
					  typeList.remove(target);
				
				if (cost.getReturnAmount() > typeList.size())
					return false;
			}
			else if (!AllZone.GameAction.isCardInPlay(card))
				return false;
		}
		
		return true;
  }
  
  static public boolean canPayCost(String cost)
  {
    if(cost.equals(("0")))
       return true;

    CardList land = getAvailableMana();
    
    ManaCost manacost = new ManaCost(cost);
    ArrayList<String> colors;

    for(int i = 0; i < land.size(); i++)
    {
      colors = getColors(land.get(i));
      int once = 0;
      
      for(int j =0; j < colors.size(); j++)
      {
	      if(manacost.isNeeded(colors.get(j)) && once == 0)
	      { 
	        manacost.payMana(colors.get(j));
	        once++;
	      }

	      if(manacost.isPaid()) {
	    	  return true;
	      }
      }
    }
    return false;
  }//canPayCost()


  static public void payManaCost(SpellAbility sa)
  {
    CardList land = getAvailableMana();
   
    //this is to prevent errors for land cards that have abilities that cost mana.
    if(sa.getSourceCard().isLand() /*&& sa.isTapAbility()*/)
    {
       land.remove(sa.getSourceCard());
    }
    
    String mana = sa.getPayCosts() != null ? sa.getPayCosts().getTotalMana() : sa.getManaCost();
    
    ManaCost cost = AllZone.GameAction.getSpellCostChange(sa, new ManaCost(mana));
    // Beached - Delete old
    if(cost.isPaid())
        return;
 // Beached - Delete old
    ArrayList<String> colors;

    for(int i = 0; i < land.size(); i++)
    {
    	final Card sourceLand = land.get(i);
       colors = getColors(land.get(i));
      for(int j = 0; j <colors.size();j++)
      {
         if(cost.isNeeded(colors.get(j)) && sourceLand.isUntapped())
         {
            sourceLand.tap();
            cost.payMana(colors.get(j));
            
            if (sourceLand.getName().equals("Undiscovered Paradise")) {
            	sourceLand.setBounceAtUntap(true);
            }
            
            if (sourceLand.getName().equals("Forbidden Orchard")) {
            	AllZone.Stack.add(CardFactoryUtil.getForbiddenOrchardAbility(sourceLand, AllZone.HumanPlayer));
            }
            
            //Manabarbs code
            if(sourceLand.isLand()) { //&& this.isTapAbility()) {
            	CardList barbs = AllZoneUtil.getCardsInPlay("Manabarbs");
            	for(Card barb:barbs) {
            		final Card manabarb = barb;
            		SpellAbility ability = new Ability(manabarb, "") {
            			@Override
            			public void resolve() {
            				sourceLand.getController().addDamage(1, manabarb);
            			}
            		};
            		
            		StringBuilder sb = new StringBuilder();
            		sb.append(manabarb.getName()).append(" - deal 1 damage to ").append(sourceLand.getController());
            		ability.setStackDescription(sb.toString());
            		
            		AllZone.Stack.add(ability);
            	}
            }
            
            if(sourceLand.getName().equals("Rainbow Vale")) {
            	sourceLand.addExtrinsicKeyword("An opponent gains control of CARDNAME at the beginning of the next end step.");
            }
            
            //System.out.println("just subtracted " + colors.get(j) + ", cost is now: " + cost.toString());

         }
         if(cost.isPaid())
            break;
      }
     
    }
    if(! cost.isPaid())
      throw new RuntimeException("ComputerUtil : payManaCost() cost was not paid for " + sa.getSourceCard().getName());
  }//payManaCost()
  
 
  public static ArrayList<String> getColors(Card land)
  {
		ArrayList<String> colors = new ArrayList<String>();
	  	if (land.isReflectedLand()){
	  		// Reflected lands (Exotic Orchard and Reflecting Pool) have one
	  		// mana ability, and it has a method called 'getPossibleColors"
	  		ArrayList<Ability_Mana> amList = land.getManaAbility();
	  		colors = ((Ability_Reflected_Mana)amList.get(0)).getPossibleColors();
	  	} else {  		 
	  		if (land.getKeyword().contains("tap: add B"))
	  			colors.add(Constant.Color.Black);
	  		if (land.getKeyword().contains("tap: add W"))
	  			colors.add(Constant.Color.White);
	  		if (land.getKeyword().contains("tap: add G"))
	  			colors.add(Constant.Color.Green);
	  		if (land.getKeyword().contains("tap: add R"))
	  			colors.add(Constant.Color.Red);
	  		if (land.getKeyword().contains("tap: add U"))
	  			colors.add(Constant.Color.Blue);
	  		if (land.getKeyword().contains("tap: add 1"))
	  			colors.add(Constant.Color.Colorless);
	  	} 	
	return colors;		
	  
  }

  static public CardList getAvailableMana()
  {
    CardList list = new CardList(AllZone.Computer_Battlefield.getCards());
    CardList mana = list.filter(new CardListFilter()
    {
      public boolean addCard(Card c)
      {
        //if(c.isCreature() && c.hasSickness())
        //  return false;

        for (Ability_Mana am : c.getAIPlayableMana())
        	if (am.canPlay()) return true;
                
        return false;
      }
    });//CardListFilter
    
    CardList sortedMana = new CardList();
    
    for (int i=0; i<mana.size();i++)
    {
    	Card card = mana.get(i);
    	if (card.isBasicLand()){
    		sortedMana.add(card);
    		mana.remove(card);
    	}
    }
    for (int j=0; j<mana.size();j++)
    {
    	sortedMana.add(mana.get(j));
    }
    
    return sortedMana;
    
  }//getAvailableMana()

  //plays a land if one is available
  static public void chooseLandsToPlay()
  {
		ArrayList<Card> landList = PlayerZoneUtil.getCardType(AllZone.Computer_Hand, "Land");
		
		if (AllZoneUtil.getPlayerCardsInPlay(AllZone.ComputerPlayer, "Crucible of Worlds").size() > 0)
		{
			CardList lands = AllZoneUtil.getPlayerTypeInGraveyard(AllZone.ComputerPlayer, "Land");
			for (Card crd : lands)
				landList.add(crd);
		}
		
		while(!landList.isEmpty() && (AllZone.GameInfo.computerNumberLandPlaysLeft() > 0 ||
		    	AllZoneUtil.getPlayerCardsInPlay(AllZone.ComputerPlayer, "Fastbond").size() > 0)){
			// play as many lands as you can
		    int ix = 0;
		    while (landList.get(ix).isReflectedLand() && (ix+1 < landList.size())) {
		    	// Skip through reflected lands. Choose last if they are all reflected.
		    	ix++;
		    }

	    	Card land = landList.get(ix);
		    landList.remove(ix);
		    playLand(land, AllZone.getZone(land));
		    
		    AllZone.GameAction.checkStateEffects();
		}
  }
  
  static public void playLand(Card land, PlayerZone zone)
  {
	    zone.remove(land);
	    AllZone.Computer_Battlefield.add(land);
	    CardFactoryUtil.playLandEffects(land);
	    AllZone.GameInfo.incrementComputerPlayedLands();
  }
  
  static public void playEOT(){
	  // TODO: Called from End of Turn of Player. 
	  // Play any abilities of a renewable resource (tapping, mana)
	  boolean bPass = true;
	  
	  if (bPass)
		  passPriority();
  }
  
  static public void playBeginHumanCombat(){
	  // TODO: Called from Begin Combat of Player. 
	  // should tap creatures we don't want to attack, or other sneaky things like that
	  boolean bPass = true;
	  
	  if (bPass)
		  passPriority();
  }
  
  static public void playBeginAICombat(){
	  // TODO: Called from Begin Combat of Computer. 
	  // should tap creatures we don't want to attack, or other sneaky things like that
	  boolean bPass = true;
	  
	  if (bPass)
		  passPriority();
  }
  
  static public void playDeclareAttacks(){
	  // TODO: Called from Declare Attackers (Abilities)
	  boolean bPass = true;
	  
	  if (bPass)
		  passPriority();
  }
  
  static public void playDeclareBlockers(){
	  // TODO: Called from Declare Blockers (Abilities)
	  boolean bPass = true;
	  
	  if (bPass)
		  passPriority();
  }
  
  static public void playRespondToStack(){
	  // TODO: Called from Declare Blockers (Abilities)
	  boolean bPass = true;
	  
	  if (bPass)
		  passPriority();
  }
  
  static public void passPriority(){
	  // if it's the computers turn and the player should get priority. 
	  if (AllZone.Phase.getPlayerTurn().isComputer()){
		  // Let the human player have access to pass the turn
	  }
  }
  
  static public Card getCardPreference(Card activate, String pref, CardList typeList){
	     String[] prefValid = activate.getSVar("AIPreference").split("\\$");
	     if (prefValid[0].equals(pref)){
	        CardList prefList = typeList.getValidCards(prefValid[1].split(","),activate.getController() ,activate);
	        if (prefList.size() != 0){
	           prefList.shuffle();
	           return prefList.get(0);
	        }
	     }
	     if (pref.contains("SacCost")) { // search for permanents with SacMe
	    	 for(int ip = 0; ip < 9; ip++) {    // priority 0 is the lowest, priority 5 the highest 
	    		 final int priority = 9-ip;
	    		 CardList SacMeList = typeList.filter(new CardListFilter() {
	    			 public boolean addCard(Card c) {
	    				 return (!c.getSVar("SacMe").equals("") && Integer.parseInt(c.getSVar("SacMe")) == priority);
	    			 }
	    		 });
	    		 if (SacMeList.size() != 0){
	    			 SacMeList.shuffle();
	    			 return SacMeList.get(0);
	    		 }
	    	 }
	     }
	     return null;
	  }
  
  static public Card chooseSacrificeType(String type, Card activate, Card target){
      PlayerZone play = AllZone.getZone(Constant.Zone.Battlefield, AllZone.ComputerPlayer);
      CardList typeList = new CardList(play.getCards());
      typeList = typeList.getValidCards(type.split(","),activate.getController() ,activate);
	  if (target != null && target.getController().equals(AllZone.ComputerPlayer) && typeList.contains(target)) // don't sacrifice the card we're pumping
		  typeList.remove(target);
	  
	  if (typeList.size() == 0)
		  return null;
	  
	  Card prefCard = getCardPreference(activate, "SacCost", typeList);
	  if (prefCard != null)
		  return prefCard;
	  
      CardListUtil.sortAttackLowFirst(typeList);
	  return typeList.get(0);
  }
  
  static public Card chooseExileType(String type, Card activate, Card target){
	  //logic is the same as sacrifice...
      return chooseSacrificeType(type, activate, target);
  }
  
  static public Card chooseTapType(String type, Card activate, boolean tap, int index){
	  PlayerZone play = AllZone.getZone(Constant.Zone.Battlefield, AllZone.ComputerPlayer);
      CardList typeList = new CardList(play.getCards());
      typeList = typeList.getValidCards(type.split(","),activate.getController() ,activate);
	  
      //is this needed?
      typeList = typeList.filter(new CardListFilter()
	  {
		 public boolean addCard(Card c)
		 {
			 return c.isUntapped();
		 }
	  });
      
      if (tap)
    	  typeList.remove(activate);
    	  
	  if (typeList.size() == 0 || index >= typeList.size())
		  return null;
	  
      CardListUtil.sortAttackLowFirst(typeList);
	  return typeList.get(index);
  }
  
  static public Card chooseReturnType(String type, Card activate, Card target){
      PlayerZone play = AllZone.getZone(Constant.Zone.Battlefield, AllZone.ComputerPlayer);
      CardList typeList = new CardList(play.getCards());
      typeList = typeList.getValidCards(type.split(","),activate.getController() ,activate);
	  if (target != null && target.getController().equals(AllZone.ComputerPlayer) && typeList.contains(target)) // don't bounce the card we're pumping
		  typeList.remove(target);
	  
	  if (typeList.size() == 0)
		  return null;
	  
      CardListUtil.sortAttackLowFirst(typeList);
	  return typeList.get(0);
  }

  static public CardList getPossibleAttackers()
  {
	  CardList list = new CardList(AllZone.Computer_Battlefield.getCards());
	  list = list.filter(new CardListFilter()
	  {
		public boolean addCard(Card c) {
			return c.isCreature() && CombatUtil.canAttack(c);
		}
	  });
	  return list;
  }
  static public Combat getAttackers()
  {
    ComputerUtil_Attack2 att = new ComputerUtil_Attack2(
        AllZone.Computer_Battlefield.getCards(),
        AllZone.Human_Battlefield.getCards()   ,  AllZone.HumanPlayer.getLife());

    return att.getAttackers();
  }
  static public Combat getBlockers()
  {
    CardList blockers = new CardList(AllZone.Computer_Battlefield.getCards());

    return ComputerUtil_Block2.getBlockers(AllZone.Combat, blockers);
  }
  
  static void sortSpellAbilityByCost(SpellAbility sa[])
  {
	  //sort from highest cost to lowest
	  //we want the highest costs first
	  Comparator<SpellAbility> c = new Comparator<SpellAbility>()
	  {
		  public int compare(SpellAbility a, SpellAbility b)
		  {
			  int a1 = CardUtil.getConvertedManaCost(a);
			  int b1 = CardUtil.getConvertedManaCost(b);

			  //puts creatures in front of spells
			  if(a.getSourceCard().isCreature())
				  a1 += 1;

			  if(b.getSourceCard().isCreature())
				  b1 += 1;


			  return b1 - a1;
		  }
	  };//Comparator
	  Arrays.sort(sa, c);
  }//sortSpellAbilityByCost()
  
	static void sacrificePermanents(int amount, CardList list) {
		// used in Annihilator and AF_Sacrifice
		int max = list.size();
		if (max > amount)
			max = amount;

		CardListUtil.sortCMC(list);
		list.reverse();

		for (int i = 0; i < max; i++) {
			// todo: use getWorstPermanent() would be wayyyy better

			Card c;
			if (list.getNotType("Creature").size() == 0) {
				c = CardFactoryUtil.AI_getWorstCreature(list);
			} else if (list.getNotType("Land").size() == 0) {
				c = CardFactoryUtil.getWorstLand(AllZone.ComputerPlayer);
			} else {
				c = list.get(0);
			}
			
			ArrayList<Card> auras = c.getEnchantedBy();

			if (auras.size() > 0){
				// todo: choose "worst" controlled enchanting Aura
				for(int j = 0; j < auras.size(); j++){
					Card aura = auras.get(j);
					if (aura.getController().isPlayer(c.getController()) && list.contains(aura)){
						c = aura;
						break;
					}
				}
			}
			
			list.remove(c);
			AllZone.GameAction.sacrifice(c);
		}
	}
}