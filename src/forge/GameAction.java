package forge;
import java.util.*;
import javax.swing.*;


public class GameAction
{
  //  private StaticEffects staticEffects = new StaticEffects();

  //returns null if playes does not have a Planeswalker
  public Card getPlaneswalker(String player)
  {
    PlayerZone p = AllZone.getZone(Constant.Zone.Play, player);
    CardList c = new CardList(p.getCards());
    c = c.getType("Planeswalker");

    if(c.isEmpty())
      return null;

    return c.get(0);
  }

  @SuppressWarnings("unused") // getCurrentCard
private Card getCurrentCard(int ID)
  {
    CardList all = new CardList();
    all.addAll(AllZone.Human_Graveyard.getCards());
    all.addAll(AllZone.Human_Hand.getCards());
    all.addAll(AllZone.Human_Library.getCards());
    all.addAll(AllZone.Human_Play.getCards());
    all.addAll(AllZone.Human_Removed.getCards());

    all.addAll(AllZone.Computer_Graveyard.getCards());
    all.addAll(AllZone.Computer_Hand.getCards());
    all.addAll(AllZone.Computer_Library.getCards());
    all.addAll(AllZone.Computer_Play.getCards());
    all.addAll(AllZone.Computer_Removed.getCards());

    for(int i = 0; i < all.size(); i++)
      if(all.get(i).getUniqueNumber() == ID)
        return all.get(i);

    return null;
  }//getCurrentCard()

  public Card moveTo(PlayerZone zone, Card c)
  {
    //c = getCurrentCard(c); - breaks things, seems to not be needed
    //not 100% sure though, this might be broken

    
    //here I did a switcheroo: remove card from zone first, 
	//THEN do copyCard, to ensure leavesPlay() effects will execute.

    PlayerZone p = AllZone.getZone(c);
    //like if a Sorcery was resolved and needs to be put in the graveyard
    //used by Input_Instant
    if(p != null)
      p.remove(c);
    
    //create new Card, which resets stats and anything that might have changed during play
    if(! c.isToken())
        c = AllZone.CardFactory.copyCard(c);

    zone.add(c);
    return c;
  }
  //card can be anywhere like in Hand or in Play
  public void moveToGraveyard(Card c)
  {
    //must put card in OWNER's graveyard not controller's
    PlayerZone grave = AllZone.getZone(Constant.Zone.Graveyard, c.getOwner());
    moveTo(grave, c);
  }

  public void moveToTopOfLibrary(Card c)
  {
	  PlayerZone p = AllZone.getZone(c);
	  PlayerZone library = AllZone.getZone(Constant.Zone.Library, c.getOwner());
	  
	  if(p != null)
	      p.remove(c);
	  if(! c.isToken())
	        c = AllZone.CardFactory.copyCard(c);

	    library.add(c, 0);
  }
  
  public void discardRandom(String player)
  {
    Card[] c = AllZone.getZone(Constant.Zone.Hand, player).getCards();
    if(c.length != 0)
      discard(CardUtil.getRandom(c));
  }

  public void discard(Card c)
  {
	discard_nath(c);
	discard_megrim(c);
    moveToGraveyard(c);
  }
  
  public void discardRandom(String player, int numDiscard)
  {
     for (int i=0; i < numDiscard; i++)
     {
        Card[] c = AllZone.getZone(Constant.Zone.Hand, player).getCards();
        if (c.length != 0)
           discard(CardUtil.getRandom(c));
     }
  }

  public void discard(String player, int numDiscard)
  {
	 if (player.equals(Constant.Player.Human))
		  AllZone.InputControl.setInput(CardFactoryUtil.input_discard(numDiscard));
	 else {
     for (int i=0; i < numDiscard; i++)
           AI_discard();
	 }
  }

  public void discardUnless(String player, int numDiscard, String uType)
  {
     if (player.equals(Constant.Player.Human))
        AllZone.InputControl.setInput(CardFactoryUtil.input_discardNumUnless(numDiscard, uType));
     else
        AI_discardNumUnless(numDiscard, uType);
  }
 
  public void AI_discardNumUnless(int numDiscard, String uType)
  {
     CardList hand = new CardList();
     hand.addAll(AllZone.getZone(Constant.Zone.Hand, Constant.Player.Computer).getCards());
     CardList tHand = hand.getType(uType);
    
     if (tHand.size() > 0)
     {
        CardListUtil.sortCMC(tHand);
        tHand.reverse();
        discard(tHand.get(0));
        return;
     }
     for (int i=0; i < numDiscard; i++)
        AI_discard();
  }
 
  public void AI_discard()
  {
     CardList hand = new CardList();
     hand.addAll(AllZone.getZone(Constant.Zone.Hand, Constant.Player.Computer).getCards());
    
     if (hand.size() > 0)
     {
	     CardList blIP = new CardList();
	     blIP.addAll(AllZone.getZone(Constant.Zone.Play, Constant.Player.Computer).getCards());
	     blIP = blIP.getType("Basic");
	     if (blIP.size() > 5)
	     {
	        CardList blIH = hand.getType("Basic");
	        if (blIH.size() > 0)
	        {
	           discard(blIH.get(CardUtil.getRandomIndex(blIH)));
	           return;
	        }
	       
	        CardListUtil.sortAttackLowFirst(hand);
	        CardListUtil.sortNonFlyingFirst(hand);
	        discard(hand.get(0));
	        return;
	     }
	     else
	     {
	        CardListUtil.sortCMC(hand);
	        discard(hand.get(0));
	        return;
	     }
     }
  }

  public void handToLibrary(String player, int numToLibrary, String libPos)
  {    
     if (player.equals(Constant.Player.Human))
     {
        if (libPos.equals("Top") || libPos.equals("Bottom"))
           libPos = libPos.toLowerCase();
        else
        {
           Object o = new Object();
           String s = "card";
           if (numToLibrary > 1)
              s = "cards";

           o = AllZone.Display.getChoice("Do you want to put the " + s + " on the top or bottom of your library?", new Object[] {"top", "bottom"});
           libPos = o.toString();
        }
        AllZone.InputControl.setInput(CardFactoryUtil.input_putFromHandToLibrary(libPos, numToLibrary));
     }
     else
     {
        for (int i=0; i < numToLibrary; i++)
        {
           if (libPos.equals("Top") || libPos.equals("Bottom"))
              libPos = libPos.toLowerCase();
           else
           {
              Random r = new Random();
              if (r.nextBoolean())
                 libPos = "top";
              else
                 libPos = "bottom";
           }
           AI_handToLibrary(libPos);
        }
     }
  }
 
  public void AI_handToLibrary(String libPos)
  {
     CardList hand = new CardList();
     hand.addAll(AllZone.getZone(Constant.Zone.Hand, Constant.Player.Computer).getCards());
    
     CardList blIP = new CardList();
     blIP.addAll(AllZone.getZone(Constant.Zone.Play, Constant.Player.Computer).getCards());
     blIP = blIP.getType("Basic");
     if (blIP.size() > 5)
     {
        CardList blIH = hand.getType("Basic");
        if (blIH.size() > 0)
        {
           Card card = blIH.get(CardUtil.getRandomIndex(blIH));
           AllZone.Computer_Hand.remove(card);
           if (libPos.equals("top"))
              AllZone.Computer_Library.add(card, 0);
           else
              AllZone.Computer_Library.add(card);
          
           return;
        }
       
        CardListUtil.sortAttackLowFirst(hand);
        CardListUtil.sortNonFlyingFirst(hand);
        discard(hand.get(0));
        return;
     }
     else
     {
        CardListUtil.sortCMC(hand);
        discard(hand.get(0));
        return;
     }    
  }

  
  public void discard_nath(Card discardedCard)
  {
	  final String owner = discardedCard.getOwner();
	  final String opponent = AllZone.GameAction.getOpponent(owner);
	  
	  PlayerZone opponentZone = AllZone.getZone(Constant.Zone.Play,opponent);
	  CardList opponentList = new CardList(opponentZone.getCards());
	  
	  for(int i=0;i < opponentList.size();i++)
	  {
		  Card card = opponentList.get(i);
		  if (card.getName().equals("Nath of the Gilt-Leaf"))
		  {
			  Card c = new Card();

	          c.setOwner(card.getController());
	          c.setController(card.getController());

			  c.setName("Elf Warrior");
	          c.setImageName("G 1 1 Elf Warrior");
	          c.setManaCost("G");
	          c.setToken(true);
	          
	          c.addType("Creature");
	          c.addType("Elf");
	          c.addType("Warrior");
	          c.setBaseAttack(1);
	          c.setBaseDefense(1);
	          
	          opponentZone.add(c);
		  }
	  }
  }
  
  public void discard_megrim(Card c)
  {
    //Whenever an opponent discards a card, Megrim deals 2 damage to that player.
   //when a card is discarded, deal 2 damage to that player for each Megrim in play controlled by the opponent.
   final String owner = c.getOwner();
   final String opponent = AllZone.GameAction.getOpponent(owner);
    PlayerZone opponentZone = AllZone.getZone(Constant.Zone.Play,opponent);
   CardList opponentList = new CardList(opponentZone.getCards());
   
   //remove 2 life for each Megrim the opponent controls.
   for(int i=0;i<opponentList.size();i++){
     Card tmpCard = opponentList.get(i);
     if(tmpCard.getName().equals("Megrim")){
      AllZone.GameAction.getPlayerLife(owner).subtractLife(2);
     }
   }
  }

  public void checkStateEffects()
  {
//    System.out.println("checking !!!");
//    RuntimeException run = new RuntimeException();
//    run.printStackTrace();

    JFrame frame = (JFrame) AllZone.Display;
    if(! frame.isDisplayable())
      return;

    boolean stop = false;

    if(AllZone.Computer_Life.getLife() <= 0)
    {
      Constant.Runtime.WinLose.addWin();
      stop = true;
    }
    if(AllZone.Human_Life.getLife() <= 0)
    {
      Constant.Runtime.WinLose.addLose();
      stop = true;
    }

    if(stop)
    {
      frame.dispose();
      new Gui_WinLose();
      return;
    }

    //card state effects like Glorious Anthem
    for (String effect : AllZone.StateBasedEffects.getStateBasedMap().keySet() ) {
		Command com = GameActionUtil.commands.get(effect);
		com.execute();
	}
    
    GameActionUtil.executeCardStateEffects();

    //System.out.println("checking state effects");
    ArrayList<Card> creature = PlayerZoneUtil.getCardType(AllZone.Computer_Play, "Creature");
    creature.addAll(PlayerZoneUtil.getCardType(AllZone.Human_Play   , "Creature"));

    Card c;
    Iterator<Card> it = creature.iterator();
    while(it.hasNext())
    {
      c = (Card)it.next();
      
      if (c.isEquipped())
      {
    	  for (int i=0;i < c.getEquippedBy().size(); i++)
    	  {
    		  Card equipment = c.getEquippedBy().get(i);
    		  if (!AllZone.GameAction.isCardInPlay(equipment))
    		  {
    			  equipment.unEquipCard(c);    			  
    		  }
    	  }
      }//if isEquipped()
      
      if(c.getNetDefense() <= c.getDamage() && !c.getKeyword().contains("Indestructible")){
        destroy(c);
        AllZone.Combat.removeFromCombat(c); //this is untested with instants and abilities but required for First Strike combat phase
      }
      
      else if (c.getNetDefense() <= 0)
      {
    	  destroy(c);
    	  AllZone.Combat.removeFromCombat(c);
      }
      
    }//while it.hasNext()
    
    
    ArrayList<Card> enchantments  = PlayerZoneUtil.getCardType(AllZone.Computer_Play, "Enchantment");
    enchantments.addAll     (PlayerZoneUtil.getCardType(AllZone.Human_Play   , "Enchantment"));

    Iterator<Card> iterate = enchantments.iterator();
    while(iterate.hasNext())
    {
      c = (Card)iterate.next();

      if (c.isAura())
      {
    	  for (int i=0;i < c.getEnchanting().size(); i++)
    	  {
    		  Card perm = c.getEnchanting().get(i);
    		  if (!AllZone.GameAction.isCardInPlay(perm) || CardFactoryUtil.hasProtectionFrom(c, perm) 
    			  || (c.getKeyword().contains("Enchant creature") && !perm.getType().contains("Creature")) )
    		  {
    			  c.unEnchantCard(perm); 			  
    			  destroy(c);
     		  }
    	  }
      }//if isAura
      
    }//while iterate.hasNext()
    
    
    //Make sure all equipment stops equipping previously equipped creatures that have left play.
    ArrayList<Card> equip = PlayerZoneUtil.getCardType(AllZone.Computer_Play, "Equipment");
    equip.addAll(PlayerZoneUtil.getCardType(AllZone.Human_Play, "Equipment"));
    
    Iterator<Card> iter = equip.iterator();
    while(iter.hasNext())
    {
    	c = (Card)iter.next();
    	if (c.isEquipping())
    	{
    		Card equippedCreature = c.getEquipping().get(0);
    		if (!AllZone.GameAction.isCardInPlay(equippedCreature))
    			c.unEquipCard(equippedCreature);
    	}
    }//while iter.hasNext()
	

    destroyLegendaryCreatures();
    destroyPlaneswalkers();
  }//checkStateEffects()


  private void destroyPlaneswalkers()
  {
    //get all Planeswalkers
    CardList list = new CardList();
    list.addAll(AllZone.Human_Play.getCards());
    list.addAll(AllZone.Computer_Play.getCards());
    list = list.getType("Planeswalker");

    Card c;
    for(int i = 0; i < list.size(); i++)
    {
      c = list.get(i);
      
      if(c.getCounters(Counters.LOYALTY) <= 0)
        AllZone.GameAction.moveToGraveyard(c);
      
      CardList cl = getPlaneswalkerSubtype(list, c);
      
      if (cl.size() > 1)
      {
    	  for (Card crd : cl)
    	  {
    		  AllZone.GameAction.moveToGraveyard(crd);
    	  }
      }
    }
     
  }

  private void destroyLegendaryCreatures()
  {
    ArrayList<Card> a = PlayerZoneUtil.getCardType(AllZone.Human_Play   , "Legendary");
    a.addAll     (PlayerZoneUtil.getCardType(AllZone.Computer_Play, "Legendary"));

    while(! a.isEmpty())
    {
      ArrayList<Card> b = getCardsNamed(a, ((Card)a.get(0)).getName());
      a.remove(0);
      if(1 < b.size())
      {
        for(int i = 0; i < b.size(); i++)
          AllZone.GameAction.destroy((Card)b.get(i));
        
        
      }
    }
  }//destroyLegendaryCreatures()

  //ArrayList search is all Card objects, returns ArrayList of Cards
  public ArrayList<Card> getCardsNamed(ArrayList<Card> search, String name)
  {
    ArrayList<Card> a = new ArrayList<Card>();
    Card c[] = CardUtil.toCard(search);

    for(int i = 0; i < c.length; i++)
    {
      if(c[i].getName().equals(name))
        a.add(c[i]);
    }
    return a;
  }
  
  public CardList getPlaneswalkerSubtype(CardList search, Card planeswalker)
  {
	  final String type = planeswalker.getType().toString();
	  CardList list = search;
	  list = list.filter(new CardListFilter()
	  {
		  public boolean addCard(Card c)
		  {
			  return c.getType().toString().equals(type);
		  }
	  });
	  
	  return list;
  }
  
  
  public void sacrificeCreature(String player, SpellAbility sa)
  {
	  PlayerZone play = AllZone.getZone(Constant.Zone.Play, player);
	  CardList list = new CardList(play.getCards());
	  list = list.getType("Creature");
	  
	  this.sacrificePermanent(player, sa, list);
  }
  
  public void sacrificePermanent(String player, SpellAbility sa, CardList choices)
  {
	  if (choices.size() > 0 ){
		  if (player.equals(Constant.Player.Human))
		  {	  
			  Input in = CardFactoryUtil.input_sacrificePermanent(choices, "Select a creature to sacrifice.");
			  AllZone.InputControl.setInput(in);
		  }
		  else
		  {
			  //Card c = CardFactoryUtil.AI_getCheapestPermanent(choices, sa.getSourceCard(), false);
			  CardListUtil.sortDefense(choices);
			  choices.reverse();
			  CardListUtil.sortAttackLowFirst(choices);
			  Card c = choices.get(0);
			  this.sacrificeDestroy(c);
		  }
	  } 
  }
  
  public void sacrifice(Card c)
  {
    sacrificeDestroy(c);
  }
  public void destroyNoRegeneration(Card c)
  {
    if(! AllZone.GameAction.isCardInPlay(c) || c.getKeyword().contains("Indestructible"))
      return;

    sacrificeDestroy(c);
  }
  private void sacrificeDestroy(Card c)
  {
    if(! isCardInPlay(c))
      return;
    
    boolean persist = false;
    PlayerZone play = AllZone.getZone(c);

    if(c.getOwner().equals(Constant.Player.Human) || c.getOwner().equals(Constant.Player.Computer))
      ;
    else
      throw new RuntimeException("GameAction : destroy() invalid card.getOwner() - " +c +" " +c.getOwner());

    play.remove(c);
    
    
    if (c.getKeyword().contains("Persist") && c.getCounters(Counters.M1M1) == 0)
    	persist = true;
    
    //tokens don't go into the graveyard
    //TODO: must change this if any cards have effects that trigger "when creatures go to the graveyard"
    if(! c.isToken())
      //resets the card, untaps the card, removes anything "extra", resets attack and defense
      moveToGraveyard(c);

    
    c.destroy();
    if (c.isEquipped())
    	skullClamp_destroy(c);
    
    //destroy card effects:
    PlayerZone comp = AllZone.getZone(Constant.Zone.Play, Constant.Player.Computer);
    PlayerZone hum = AllZone.getZone(Constant.Zone.Play, Constant.Player.Human);
    
    CardList list = new CardList();
    list.addAll(comp.getCards());
    list.addAll(hum.getCards());
    list = list.filter(new CardListFilter(){
		public boolean addCard(Card c) {
			ArrayList<String> keywords = c.getKeyword();
			for (String kw : keywords)
			{
				if (kw.startsWith("Whenever ") && kw.contains(" put into a graveyard from the battlefield,"))
					return true;
			}
			return false;
		}
    });
    
    for (int i=0;i<list.size();i++)
    	GameActionUtil.executeDestroyCardEffects(list.get(i), c);	
    
    if (persist){
    	c.setDamage(0);
    	c.untap();
    	PlayerZone ownerPlay = AllZone.getZone(Constant.Zone.Play, c.getOwner());
    	PlayerZone grave = AllZone.getZone(c);
    	
    	if (c.isEquipped())
    		c.unEquipAllCards();
    	
    	grave.remove(c);
    	ownerPlay.add(c);
    	
    	c.setTempAttackBoost(0);
    	c.setTempDefenseBoost(0);
    	c.setExaltedBonus(false);
    	//reset more stuff ?
    	
    	c.addCounter(Counters.M1M1, 1);
    }
    
    //if (c.getName().equals("Rancor") || c.getName().equals("Brilliant Halo") || c.getName().equals("Undying Rage"))
    if (c.getKeyword().contains("When this card is put into a graveyard from the battlefield, return this card to its owner's hand"))
    {
    	PlayerZone hand = AllZone.getZone(Constant.Zone.Hand,c.getOwner());
    	moveTo(hand, c);
    }
    
    else if (c.getName().equals("Nissa's Chosen"))
    {
    	PlayerZone library = AllZone.getZone(Constant.Zone.Library, c.getOwner());   
    	moveTo(library, c);
    }
    
    else if (c.getName().equals("Guan Yu, Sainted Warrior"))
    {
    	PlayerZone library = AllZone.getZone(Constant.Zone.Library, c.getOwner());
    	moveTo(library, c);
    	AllZone.GameAction.shuffle(c.getOwner());
    }	
  }//sacrificeDestroy()
  public void destroy(Card c)
  {
    if(! AllZone.GameAction.isCardInPlay(c) ||
    	(c.getKeyword().contains("Indestructible") && (!c.isCreature() || c.getNetDefense() > 0)) )
      return;

    if(c.getShield() > 0)
    {
      c.subtractShield();
      c.setDamage(0);
      c.tap();
      return;
    }
    //System.out.println("Card " + c.getName() + " is getting sent to GY, and this turn it got damaged by: ");
    for (Card crd : c.getReceivedDamageFromThisTurn().keySet() ) {	
		  if (c.getReceivedDamageFromThisTurn().get(crd) > 0)
		  {
			  //System.out.println(crd.getName() );
			  GameActionUtil.executeVampiricEffects(crd);
		  }
    }
    
    this.sacrificeDestroy(c);
  }
  
  //because originally, MTGForge didn't keep track of cards removed from the game.
  public void removeFromGame(Card c)
  {
	  if (AllZone.GameAction.isCardRemovedFromGame(c) )
		  return;
	  
	  PlayerZone zone = AllZone.getZone(c); //could be hand, grave, play, ...
	  PlayerZone removed = AllZone.getZone(Constant.Zone.Removed_From_Play, c.getOwner());
	  
	  zone.remove(c);
	  if (!c.isToken())
		  removed.add(c);
	  
  }
  
  
  private boolean shouldDraw = true;
  private String lastPlayerToDraw = Constant.Player.Human;
  
  public void drawCard(String player)
  {
    //TODO: show that milled player looses

   boolean isDrawPhase = AllZone.Phase.getPhase().equals(Constant.Phase.Draw);
   if(isDrawPhase){
      String currentPlayer = AllZone.Phase.getActivePlayer();
      if(!currentPlayer.equals(lastPlayerToDraw)){
         shouldDraw = true;
      }
      lastPlayerToDraw = currentPlayer;
      
      if(!shouldDraw){
         return;
      }
      //so they can't draw twice in a row during the draw phase
      shouldDraw = false;
   }
    PlayerZone library = AllZone.getZone(Constant.Zone.Library, player);
    PlayerZone hand = AllZone.getZone(Constant.Zone.Hand, player);

    if(library.size() != 0)
    {
      
      Card c = library.get(0);
      library.remove(0);
      hand.add(c);
      
      GameActionUtil.executeDrawCardTriggeredEffects(player);
    }
  }
  
  //is this card a permanent that is in play?
  public boolean isCardInPlay(Card c)
  {
    return PlayerZoneUtil.isCardInZone(AllZone.Computer_Play, c) ||
        PlayerZoneUtil.isCardInZone(AllZone.Human_Play   , c);
  }
  
  public boolean isCardInGrave(Card c)
  {
	  return PlayerZoneUtil.isCardInZone(AllZone.Computer_Graveyard, c) ||
	  		 PlayerZoneUtil.isCardInZone(AllZone.Human_Graveyard, c);
  }
  
  public boolean isCardRemovedFromGame(Card c)
  {
	  return PlayerZoneUtil.isCardInZone(AllZone.Computer_Removed, c) ||
	  		 PlayerZoneUtil.isCardInZone(AllZone.Human_Removed, c);
  }
  
  public String getOpponent(String p)
  {
    return p.equals(Constant.Player.Human) ? Constant.Player.Computer : Constant.Player.Human;
  }
  //TODO: shuffling seems to change a card's unique number but i'm not 100% sure
  public void shuffle(String player)
  {
    PlayerZone library = AllZone.getZone(Constant.Zone.Library, player);
    Card c[] = library.getCards();

    if(c.length <= 1)
      return;

    ArrayList<Object> list = new ArrayList<Object>(Arrays.asList(c));
      //overdone but wanted to make sure it was really random
      Random random = new Random();
      Collections.shuffle(list, random);
      Collections.shuffle(list, random);
      Collections.shuffle(list, random);
      Collections.shuffle(list, random);
      Collections.shuffle(list, random);
      Collections.shuffle(list, random);

//      random = java.security.SecureRandom.getInstance("SHA1PRNG");

      Object o;
      for(int i = 0; i < list.size(); i++)
      {
        o = list.remove(random.nextInt(list.size()));
        list.add(random.nextInt(list.size()), o);
      }

      Collections.shuffle(list, random);
      Collections.shuffle(list, random);
      Collections.shuffle(list, random);
      Collections.shuffle(list, random);
      Collections.shuffle(list, random);
      Collections.shuffle(list, random);


      list.toArray(c);
      library.setCards(c);
  }//shuffle
  public boolean isCardInZone(Card card, PlayerZone p)
  {
    ArrayList<Card> list = new ArrayList<Card>(Arrays.asList(p.getCards()));
    return list.contains(card);
  }
  public PlayerLife getPlayerLife(String player)
  {
    if(player == null)
      throw new RuntimeException("GameAction : getPlayerLife() player == null");

    if(player.equals(Constant.Player.Human))
      return AllZone.Human_Life;
    else if(player.equals(Constant.Player.Computer))
      return AllZone.Computer_Life;
    else
      throw new RuntimeException("GameAction : getPlayerLife() invalid player string " +player);
  }
  //removes all damage from player's creatures
  public void removeDamage(String player)
  {
    PlayerZone p = AllZone.getZone(Constant.Zone.Play, player);
    ArrayList<Card> a = PlayerZoneUtil.getCardType(p, "Creature");
    Card c[] = CardUtil.toCard(a);
    for(int i = 0; i < c.length; i++)
      c[i].setDamage(0);
  }
  public void newGame(Deck humanDeck, Deck computerDeck)
  {
//    AllZone.Computer = new ComputerAI_Input(new ComputerAI_General());
	System.gc(); //garbage collection... does it make a difference though?
	lastPlayerToDraw = Constant.Player.Human;
		
    AllZone.Computer_Life.setLife(20);
    AllZone.Human_Life.setLife(20);

    AllZone.Human_Life.setAssignedDamage(0);
    AllZone.Computer_Life.setAssignedDamage(0);

    AllZone.Stack.reset();
    AllZone.Phase.reset();
    AllZone.Combat.reset();
    AllZone.Display.showCombat("");

    AllZone.Human_Graveyard.reset();
    AllZone.Human_Hand.reset();
    AllZone.Human_Library.reset();
    AllZone.Human_Play.reset();
    AllZone.Human_Removed.reset();

    AllZone.Computer_Graveyard.reset();
    AllZone.Computer_Hand.reset();
    AllZone.Computer_Library.reset();
    AllZone.Computer_Play.reset();
    AllZone.Computer_Removed.reset();

    AllZone.InputControl.resetInput();
    
    AllZone.StateBasedEffects.reset();

    //clear Image caches, so the problem doesn't get slower and slower
    //cached images are cleared in Gui_WinLose.quitButton_actionPerformed()


    {//re-number cards just so their unique numbers are low, just for user friendliness
      CardFactory c = AllZone.CardFactory;
      Card card;
      int nextUniqueNumber = 1;
      
      Random generator = new Random();
      
      for(int i = 0; i < humanDeck.countMain(); i++)
      {
        card = c.getCard(humanDeck.getMain(i), Constant.Player.Human);
        card.setUniqueNumber(nextUniqueNumber++);
        if (card.isBasicLand())
        {
        	card.setRandomPicture(generator.nextInt(4));
        	//System.out.println("human random number:" + card.getRandomPicture());
        }
        AllZone.Human_Library.add(card);

        //get card picture so that it is in the image cache
//        ImageCache.getImage(card);
      }

      for(int i = 0; i < computerDeck.countMain(); i++)
      {
        card = c.getCard(computerDeck.getMain(i), Constant.Player.Computer);
        card.setUniqueNumber(nextUniqueNumber++);
        if (card.isBasicLand())
        {
        	card.setRandomPicture(generator.nextInt(4));
        	//System.out.println("computer random number:" + card.getRandomPicture());
        }
        AllZone.Computer_Library.add(card);

        //get card picture so that it is in the image cache
//        ImageCache.getImage(card);
      }
    }//end re-numbering

    for(int i = 0; i < 100; i++)
      this.shuffle(Constant.Player.Human);

    //do this instead of shuffling Computer's deck
    boolean smoothLand = Constant.Runtime.smooth[0];

    if(smoothLand)
    {
      Card[] c = smoothComputerManaCurve(AllZone.Computer_Library.getCards());
      AllZone.Computer_Library.setCards(c);
    }
    else
    {
      AllZone.Computer_Library.setCards(AllZone.Computer_Library.getCards());
      this.shuffle(Constant.Player.Computer);
    }

    for(int i = 0; i < 7; i++)
    {
      this.drawCard(Constant.Player.Computer);
      this.drawCard(Constant.Player.Human);
    }
    /*
    Card mp = new Card();
    mp.setName("Mana Pool");
    //mp.addType("Land");
    mp.addIntrinsicKeyword("Indestructible");
    mp.addIntrinsicKeyword("Shroud");
    AllZone.Human_Play.add(mp);
	*/
    
    ManaPool mp = AllZone.ManaPool;
    AllZone.Human_Play.add(mp.smp);
    AllZone.Human_Play.add(mp);

    AllZone.Stack.reset();//this works, it clears the stack of Upkeep effects like Bitterblossom
    AllZone.InputControl.setInput(new Input_Mulligan());
  }//newGame()

  //this is where the computer cheats
  //changes AllZone.Computer_Library
  private Card[] smoothComputerManaCurve(Card[] in)
  {
	CardList library = new CardList(in);
    library.shuffle();

    //remove all land, keep non-basicland in there, shuffled
    CardList land = library.getType("Land");
    for(int i = 0; i < land.size(); i++)
      if (land.get(i).isLand())
    	library.remove(land.get(i));

    //non-basic lands are removed, because the computer doesn't seem to
    //effectively use them very well
    land = threadLand(land);
    
    try
	{
    //mana weave, total of 7 land
    library.add(7, land.get(0));
    library.add(8, land.get(1));
    library.add(9, land.get(2));
    library.add(10, land.get(3));
    library.add(11, land.get(4));

    library.add(13, land.get(5));
    library.add(16, land.get(6));
	}
    catch(IndexOutOfBoundsException e)
	{
		System.err.println("Error: cannot smooth mana curve, not enough land");
		return in;
	}

    //add the rest of land to the end of the deck
    for(int i = 0; i < land.size(); i++)
      if(! library.contains(land.get(i)))
      library.add(land.get(i));


    //check
    for(int i = 0; i < library.size(); i++)
      System.out.println(library.get(i));

    

    
    return library.toArray();
  }//smoothComputerManaCurve()

  @SuppressWarnings("unused") // getComputerLand
private CardList getComputerLand(CardList in)
  {
    CardList land = new CardList();
    land = in.filter(new CardListFilter()
    {
      public boolean addCard(Card c)
      {
        return c.isLand();
      }
    });

    return land;
  }//getComputerLand()

  //non-basic lands are removed, because the computer doesn't seem to
  //effectively used them very well
  public CardList threadLand(CardList in)
  {
    //String[] basicLand = {"Forest", "Swamp", "Mountain", "Island", "Plains"}; //unused
	
    //Thread stuff with as large a spread of colors as possible:
    String[] allLand = {"Bayou", "Volcanic Island", "Savannah", "Badlands", "Tundra",
    					"Taiga", "Underground Sea", "Plateau", "Tropical Island", "Scrubland", 
    					"Overgrown Tomb", "Steam Vents", "Temple Garden", "Blood Crypt", "Hallowed Fountain",
    					"Stomping Ground", "Watery Grave", "Sacred Foundry", "Breeding Pool", "Godless Shrine",
    					"Pendelhaven", "Flagstones of Trokair",
    					"Forest", "Swamp", "Mountain", "Island", "Plains",
    					"Tree of Tales", "Vault of Whispers", "Great Furnace", "Seat of the Synod", "Ancient Den",
    					"Treetop Village", "Ghitu Encampment", "Faerie Conclave", "Forbidding Watchtower",
    					"Savage Lands", "Arcane Sanctum", "Jungle Shrine", "Crumbling Necropolis", "Seaside Citadel",
    					"Elfhame Palace", "Coastal Tower", "Salt Marsh", 
    					"Kher Keep", "Library of Alexandria", "Dryad Arbor" };
    
    			
    
    ArrayList<CardList> land = new ArrayList<CardList>();

    //get different CardList of all Forest, Swamps, etc...
    CardList check;
    for(int i = 0; i < allLand.length; i++)
    {
      check = in.getName(allLand[i]);

      if(! check.isEmpty())
        land.add(check);
    }
/*
    //get non-basic land CardList
    check = in.filter(new CardListFilter()
    {
      public boolean addCard(Card c)
      {
        return c.isLand() && !c.isBasicLand();
      }
    });
    if(! check.isEmpty())
      land.add(check);
*/

    //thread all separate CardList's of land together to get something like
    //Mountain, Plains, Island, Mountain, Plains, Island
    CardList out = new CardList();

    int i = 0;
    while(! land.isEmpty())
    {
      i = (i + 1) % land.size();

      check = (CardList) land.get(i);
      if(check.isEmpty())
      {
    	//System.out.println("removed");
        land.remove(i);
        i--;
        continue;
      }

      out.add(check.get(0));
      check.remove(0);
    }//while

    return out;
  }//threadLand()


  @SuppressWarnings("unused") // getDifferentLand
private int getDifferentLand(CardList list, String land)
  {
    int out = 0;

    return out;
  }


  //decides who goes first when starting another game, used by newGame()
  public void chooseWhoPlaysFirst()
  {
    //lets the user decides who plays first
    boolean humanChoose = false;
    if(Constant.Runtime.WinLose.getWin() == 0 && Constant.Runtime.WinLose.getLose() == 0)
      humanChoose = MyRandom.random.nextBoolean();
    else if(! Constant.Runtime.WinLose.didWinRecently())
      humanChoose = true;

    //does the player go first?
    boolean humanFirst;

    if(humanChoose)
    {
      int n = JOptionPane.showConfirmDialog(null, "Do you want to play first?", "", JOptionPane.YES_NO_OPTION);
      if(n == JOptionPane.YES_OPTION)
        humanFirst =  true;
      else
        humanFirst = false;
    }
    else//computer randomly decides who goes first
      humanFirst = MyRandom.random.nextBoolean();

    String message;
    if(humanFirst)
      message = "You play first";
    else
    {
      message = "Computer plays first";
      AllZone.Phase.setPhase(Constant.Phase.Main1, Constant.Player.Computer);
    }

    //show message if player doesn't get to choose
    if(! humanChoose)
      JOptionPane.showMessageDialog(null, message);
  }//choooseWhoPlaysFirst()

  //if Card had the type "Aura" this method would always return true, since local enchantments are always attached to something
  //if Card is "Equipment", returns true if attached to something
  public boolean isAttachee(Card c)
  {
    CardList list = new CardList(AllZone.Computer_Play.getCards());
    list.addAll                 (AllZone.Human_Play.getCards());

    for(int i = 0; i < list.size(); i++)
    {
      CardList check = new CardList(list.getCard(i).getAttachedCards());
      if(check.contains(c))
        return true;
    }

    return false;
  }//isAttached(Card c)

  public boolean canTarget(String targetPlayer)
  {
    return true;
  }
  public boolean canTarget(Card target, Card source)
  {
    if(target.getKeyword().contains("Cannot be target of spells or abilities"))
      return false;

    return true;
  }
  public void playCard(Card c)
  {
	  
	//TODO: add code for cards like Standstill ??
	
    SpellAbility[] choices = canPlaySpellAbility(c.getSpellAbility());
    SpellAbility sa;
/*
 System.out.println(choices.length);
 for(int i = 0; i < choices.length; i++)
     System.out.println(choices[i]);
*/
    if(choices.length == 0)
      return;
    else if(choices.length == 1)
      sa = choices[0];
    else
      sa = (SpellAbility) AllZone.Display.getChoiceOptional("Choose", choices);

    if(sa == null)
      return;

    playSpellAbility(sa);
  }
  public void playSpellAbility(SpellAbility sa)
  {
	  
	if (sa.getManaCost().equals("0") && sa.getBeforePayMana() == null){
		AllZone.Stack.add(sa);
		if (sa.isTapAbility())
			sa.getSourceCard().tap();
		return;
	}
	  
    if(sa.getBeforePayMana() == null)
      AllZone.InputControl.setInput(new Input_PayManaCost(sa));
    else
      AllZone.InputControl.setInput(sa.getBeforePayMana());
  }
  public SpellAbility[] canPlaySpellAbility(SpellAbility[] sa)
  {
    ArrayList<SpellAbility> list = new ArrayList<SpellAbility>();

    for(int i = 0; i < sa.length; i++)
      if(sa[i].canPlay())
        list.add(sa[i]);

    SpellAbility[] array = new SpellAbility[list.size()];
    list.toArray(array);
    return array;
  }//canPlaySpellAbility()
  
  public void skullClamp_destroy(Card c)
  {
	  CardList equipment = new CardList();
	  equipment.addAll(c.getEquippedBy().toArray());
	  equipment = equipment.getName("Skullclamp");
	  
	  if (equipment.size() == 0)
		  return;
	  
	  final Card crd = c;
	  
	  final Ability draw = new Ability(crd,"0")
	    {
			public void resolve() {
				String s = crd.getOwner();
				//System.out.println("owner of " + crd.getName() + " = " + s);
	    		AllZone.GameAction.drawCard(s);
	    		AllZone.GameAction.drawCard(s);
			}
	  };
	  draw.setStackDescription("Skullclamp - " + c.getController() + " draws 2 cards (" + c.getName() +").");
	  
	  for (int i=0; i<equipment.size(); i++)
	  {
		  	AllZone.Stack.add(draw);	  
	  }

  }
  
  public void setAssignedDamage(Card card, CardList sourceCards, int damage)
  {
	  if (damage < 0)
		  damage = 0;
	  
	  int assignedDamage = damage;
	  if (sourceCards.size() == 1)
	  {
		  Card sourceCard = sourceCards.get(0);
		  card.addReceivedDamageFromThisTurn(sourceCard, damage);
		  if (card.getKeyword().contains("Protection from white") && CardUtil.getColors(sourceCard).contains(Constant.Color.White))
			  assignedDamage = 0;
		  if (card.getKeyword().contains("Protection from blue") && CardUtil.getColors(sourceCard).contains(Constant.Color.Blue))
			  assignedDamage = 0;
		  if (card.getKeyword().contains("Protection from black") && CardUtil.getColors(sourceCard).contains(Constant.Color.Black))
			  assignedDamage = 0;
		  if (card.getKeyword().contains("Protection from red") && CardUtil.getColors(sourceCard).contains(Constant.Color.Red))
			  assignedDamage = 0;
		  if (card.getKeyword().contains("Protection from green") && CardUtil.getColors(sourceCard).contains(Constant.Color.Green))
			  assignedDamage = 0;
		  
		  if (card.getKeyword().contains("Protection from creatures") && sourceCard.isCreature())
			  assignedDamage = 0;
		  if (card.getKeyword().contains("Protection from everything"))
			  assignedDamage = 0;
		  if (card.getKeyword().contains("Protection from artifacts") && sourceCard.isArtifact())
			  assignedDamage = 0;
		  
		  if (card.getKeyword().contains("Protection from Dragons") && sourceCard.getType().contains("Dragon"))
			  assignedDamage = 0;
		  if (card.getKeyword().contains("Protection from Demons") && sourceCard.getType().contains("Demon"))
			  assignedDamage = 0;
		  if (card.getKeyword().contains("Protection from Goblins") && sourceCard.getType().contains("Goblin"))
			  assignedDamage = 0;
		  
		  if (card.getKeyword().contains("Protection from enchantments") && sourceCard.getType().contains("Enchantment"))
			  assignedDamage = 0;
	  }
	  else //got blocked by multiple blockers
	  {
		  for(int i=0;i<sourceCards.size();i++)
		  {
			  Card sourceCard = sourceCards.get(i); //blocker number i
			  card.addReceivedDamageFromThisTurn(sourceCard, sourceCard.getNetAttack());
		  }
	  }
	  card.setAssignedDamage(assignedDamage);
	  
	  System.out.println("***");
	  if(sourceCards.size() > 1)
		  System.out.println("(MULTIPLE blockers):");
	  System.out.println("Assigned " + damage + " damage to " + card);
	  for (int i=0;i<sourceCards.size();i++){
		  System.out.println(sourceCards.get(i).getName() + " assigned damage to " + card.getName());
	  }
	  System.out.println("***");
  }
  
  public void addDamage(Card card, int damage)
  {
	  int damageToAdd = damage;
	  //System.out.println("size of sources: " + card.getReceivedDamageFromThisTurn().size());
	  if (card.getReceivedDamageFromThisTurn().size() >= 1)
	  { 
		  for (Card c : card.getReceivedDamageFromThisTurn().keySet() ) {	
			  if (card.getReceivedDamageFromThisTurn().get(c) > 0)
			  {
				  int power;
				  System.out.println("addDamage: " +card.getName() + " has received damage from " + c.getName() );
				  
				  if (c.getKeyword().contains("Wither"))
				  {
					  power = card.getReceivedDamageFromThisTurn().get(c);
					  damageToAdd = damageToAdd - power;
					  card.addCounter(Counters.M1M1, power);
				  }
				  if (c.getName().equals("Spiritmonger") || c.getName().equals("Mirri the Cursed"))
				  {
					  	final Card thisCard = c;
						Ability ability2 = new Ability(c, "0")
						{
							public void resolve()
							{
								thisCard.addCounter(Counters.P1P1, 1);
							}
						}; // ability2

						ability2.setStackDescription(c.getName() + " - gets a +1/+1 counter");
						AllZone.Stack.add(ability2);
				  }
				  if (c.getKeyword().contains("Deathtouch"))
				  {
					  AllZone.GameAction.destroy(card);
					  AllZone.Combat.removeFromCombat(card);
				  }
			  }
		  }
	  }
	  System.out.println("Adding " + damageToAdd + " damage to " + card.getName());
	  if (isCardInPlay(card))
		  card.addDamage(damageToAdd);
  }
  
  public void addLife(String player, int life)
  {
     // place holder for future life gain modification rules
    
     getPlayerLife(player).addLife(life);
  }
 
  public void subLife(String player, int life)
  {
     // place holder for future life loss modification rules
    
     getPlayerLife(player).subtractLife(life);
  }
 
  public void addDamage(String player, int damage)
  {
     // place holder for future damage modification rules (prevention?)
    
     getPlayerLife(player).subtractLife(damage);
  }


  public static void main(String[] args)
  {
    GameAction gameAction = new GameAction();
    GenerateConstructedDeck gen = new GenerateConstructedDeck();

    for(int i = 0; i < 2000; i++)
    {
      CardList list = gen.generateDeck();

      Card[] card = gameAction.smoothComputerManaCurve(list.toArray());

      CardList check = new CardList();
      for(int a = 0; a < 30; a++)
        check.add(card[a]);

      if(check.getType("Land").size() != 7)
      {
        System.out.println("error - " +check);
        break;
      }
    }//for
  }
}