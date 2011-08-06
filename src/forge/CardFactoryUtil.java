package forge;
import java.util.*;

public class CardFactoryUtil
{
  private static Random random = new Random();

  public final static String getPumpString(int n)
  {
    if(0 <= n)
      return "+" +n;
    else
      return "" +n;
  }
  
  public static Card AI_getMostExpensivePermanent(CardList list, final Card spell, boolean targeted)
  {
	  CardList all = list;
	  if (targeted)
	  {
		  all = all.filter(new CardListFilter()
		  {
			  public boolean addCard(Card c) {
			  	return CardFactoryUtil.canTarget(spell, c);
			  }
		  });
	  }
	  if (all.size() == 0)
		  return null;
	  
	    //get biggest Permanent
	    Card biggest = null;
	    biggest = all.get(0);

	    for(int i = 0; i < all.size(); i++){
	        if(CardUtil.getConvertedManaCost(biggest.getManaCost()) >= CardUtil.getConvertedManaCost(biggest.getManaCost())){
	          biggest = all.get(i);
	        }
	    }

	    return biggest;
	  
  }
  
  public static Card AI_getCheapestPermanent(CardList list, final Card spell, boolean targeted)
  {
	  CardList all = list;
	  if (targeted) {
		  all = all.filter(new CardListFilter()
		  {
			  public boolean addCard(Card c) {
			  	return CardFactoryUtil.canTarget(spell, c);
			  }
		  });
	  }
	  if (all.size() == 0)
		  return null;
	  
	    //get cheapest card:
	    Card cheapest = null;
	    cheapest = all.get(0);

	    for(int i = 0; i < all.size(); i++){
	        if(CardUtil.getConvertedManaCost(cheapest.getManaCost()) <= CardUtil.getConvertedManaCost(cheapest.getManaCost())){
	          cheapest = all.get(i);
	        }
	    }

	    return cheapest;
	  
  }
  
  public static Card AI_getBestLand(CardList list)
  {
     CardList land = list.getType("Land");
     if (! (land.size() > 0))
        return null;
    
     CardList nbLand = land.filter(new CardListFilter ()   // prefer to target non basic lands
      {
        public boolean addCard(Card c)
           {
              return (!c.getType().contains("Basic"));
           }
      }
     );
    
     if (nbLand.size() > 0)
     {
        //TODO: Rank non basics?
       
        Random r = new Random();
        return nbLand.get(r.nextInt(nbLand.size()));
     }
    
     // if no non-basic lands, target the least represented basic land type
     String names[] = {"Plains", "Island", "Swamp", "Mountain", "Forest"};
     String sminBL = new String();
     int iminBL = 20000; // hopefully no one will ever have more than 20000 lands of one type....
     int n = 0;
     for (int i = 0; i < 5; i++)
     {
        n = land.getType(names[i]).size();
        if (n < iminBL && n > 0)   // if two or more are tied, only the first one checked will be used
        {                     
           iminBL = n;
           sminBL = names[i];
        }
     }
     if (iminBL == 20000)
        return null;   // no basic land was a minimum
    
     CardList BLand = land.getType(sminBL);
     for (int i=0; i<BLand.size(); i++)
        if (!BLand.get(i).isTapped())      // prefer untapped lands
           return BLand.get(i);
    
     Random r = new Random();
     return BLand.get(r.nextInt(BLand.size()));   // random tapped land of least represented type
  }

  
  
//The AI doesn't really pick the best enchantment, just the most expensive.
  public static Card AI_getBestEnchantment(CardList list, final Card spell, boolean targeted)
  {
    CardList all = list;
    all = all.getType("Enchantment");
    all = all.filter(new CardListFilter()
    {

		public boolean addCard(Card c) {
			return CardFactoryUtil.canTarget(spell, c);
		}
    	
    });
    if(all.size() == 0){
       return null;
    }
   
    //get biggest Enchantment
    Card biggest = null;
    biggest = all.get(0);

    for(int i = 0; i < all.size(); i++){
        if(CardUtil.getConvertedManaCost(biggest.getManaCost()) >= CardUtil.getConvertedManaCost(biggest.getManaCost())){
          biggest = all.get(i);
        }
    }

    return biggest;
  }


//The AI doesn't really pick the best artifact, just the most expensive.
  public static Card AI_getBestArtifact(CardList list)
  {
    CardList all = list;
    all = all.getType("Artifact");
    if(all.size() == 0){
        return null;
     }
    
    //get biggest Artifact
    Card biggest = null;
    biggest = all.get(0);

    for(int i = 0; i < all.size(); i++){
        if(CardUtil.getConvertedManaCost(biggest.getManaCost()) >= CardUtil.getConvertedManaCost(biggest.getManaCost())){
          biggest = all.get(i);
        }
    }

    return biggest;
  }
   
   public static CardList AI_getHumanArtifact(final Card spell, boolean targeted)
   {
     CardList artifact = new CardList(AllZone.Human_Play.getCards());
     artifact = artifact.getType("Artifact");
     if(targeted)
     {
    	 artifact = artifact.filter(new CardListFilter()     
	     {
	 		public boolean addCard(Card c) {
	 			return canTarget(spell,c);
	 		}	
	     });
     }
     return artifact;
  }

  public static CardList AI_getHumanEnchantment(final Card spell, boolean targeted)
  {
    CardList enchantment = new CardList(AllZone.Human_Play.getCards());
    enchantment = enchantment.getType("Enchantment");
    if (targeted)
    {
	    enchantment = enchantment.filter(new CardListFilter()
	    {
			public boolean addCard(Card c) {
				return canTarget(spell,c);
			}	
	    });
    }
    return enchantment;
  }


//yes this is more hacky code
  //Object[0] is the cardname
  //Object[1] is the max number of times it can be used per turn
  //Object[1] has to be an Object like Integer and not just an int
  private static Object[][] AbilityLimits =
  {
    {"Azimaet Drake"    , new Integer(1)},
    {"Drake Hatchling"  , new Integer(1)},
    {"Fire Drake"       , new Integer(1)},
    {"Plated Rootwalla" , new Integer(1)},
    {"Rootwalla"        , new Integer(1)},
    {"Spitting Drake"   , new Integer(1)},
    {"Ghor-Clan Bloodscale", new Integer(1)},
    {"Wild Aesthir",  	  new Integer(1)},
    {"Viashino Slaughtermaster", new Integer(1)},
    {"Twinblade Slasher", new Integer(1)},

    {"Phyrexian Battleflies"   , new Integer(2)},
    {"Pit Imp"                 , new Integer(2)},
    {"Roterothopter"           , new Integer(2)},
    {"Vampire Bats"            , new Integer(2)},
    {"Fire-Belly Changeling"   , new Integer(2)},
    {"Azusa, Lost but Seeking" , new Integer(2)}
  };

  public static boolean canUseAbility(Card card)
  {
    int found = -1;

    //try to find card name in AbilityLimits[][]
    for(int i = 0; i < AbilityLimits.length; i++)
      if(AbilityLimits[i][0].equals(card.getName()))
        found = i;

    if(found == -1)
      return true;

    //card was found
    if(card.getAbilityTurnUsed() != AllZone.Phase.getTurn())
    {
      card.setAbilityTurnUsed(AllZone.Phase.getTurn());
      card.setAbilityUsed(0);
    }
    SpellAbility sa;
    //this is a hack, check the stack to see if this card has an ability on the stack
    //if so, we can't use the ability: this is to prevent using a limited ability too many times
    for (int i=0; i<AllZone.Stack.size(); i++)
    {
    	sa = AllZone.Stack.peek(i);
    	if (sa.getSourceCard().equals(card))
    			return false;
    }
    
    Integer check = (Integer) AbilityLimits[found][1];
    return card.getAbilityUsed() < check.intValue();
  }//canUseAbility(Card card)




  public static boolean AI_doesCreatureAttack(Card card)
  {
    Combat combat = ComputerUtil.getAttackers();
    Card[] att = combat.getAttackers();
    for(int i = 0; i < att.length; i++)
      if(att[i].equals(card))
        return true;

    return false;
  }
  public static Card AI_getBestCreature(CardList list, Card c)
  {
	 final Card crd = c;
	 list = list.filter(new CardListFilter()
	 {
		public boolean addCard(Card c) {
			return CardFactoryUtil.canTarget(crd, c);
		} 
	 });
	  
	 return AI_getBestCreature(list);
	  
  }
  
  //returns null if list.size() == 0
  public static Card AI_getBestCreature(CardList list)
  {
    CardList all = list;
    all = all.getType("Creature");

    CardList flying = all.filter(new CardListFilter()
    {
      public boolean addCard(Card c)
      {
        return c.getKeyword().contains("Flying");
      }
    });
    //get biggest flying creature
    Card biggest = null;
    if(flying.size() != 0)
    {
      biggest = flying.get(0);

      for(int i = 0; i < flying.size(); i++)
        if(biggest.getNetAttack() < flying.get(i).getNetAttack())
          biggest = flying.get(i);
    }

    //if flying creature is small, get biggest non-flying creature
    if(all.size() != 0 &&
      (biggest == null || biggest.getNetAttack() < 3))
    {
      biggest = all.get(0);

      for(int i = 0; i < all.size(); i++)
        if(biggest.getNetAttack() < all.get(i).getNetAttack())
          biggest = all.get(i);
    }
    return biggest;
  }
  public static Input input_targetCreaturePlayer(final SpellAbility spell, boolean targeted)
  {
    return input_targetCreaturePlayer(spell, Command.Blank, targeted);
  }
  
  public static Input input_targetCreaturePlayer(final SpellAbility spell, final Command paid, final boolean targeted)
  {
    Input target = new Input()
    {
	  private static final long serialVersionUID = 2781418414287281005L;
	
	  public void showMessage()
      {
        AllZone.Display.showMessage("Select target Creature, Player, or Planeswalker");
        ButtonUtil.enableOnlyCancel();
      }
      public void selectButtonCancel() {stop();}
      public void selectCard(Card card, PlayerZone zone)
      {
        if((card.isCreature() || card.isPlaneswalker() ) && zone.is(Constant.Zone.Play) && (!targeted || canTarget(spell, card)) )
        {
          spell.setTargetCard(card);
          done();
        }
      }//selectCard()
      public void selectPlayer(String player)
      {
        spell.setTargetPlayer(player);
        done();
      }
      void done()
      {
        paid.execute();

        if(spell instanceof Ability_Tap && spell.getManaCost().equals("0"))
           stopSetNext(new Input_NoCost_TapAbility((Ability_Tap)spell));
        else if(spell.getManaCost().equals("0"))
        {
          AllZone.Stack.add(spell);
          stop();
        }
        else
          stopSetNext(new Input_PayManaCost(spell));
      }
    };
    return target;
  }//input_targetCreaturePlayer()
  
  public static Input input_targetNonCreaturePermanent(final SpellAbility spell, final Command paid)
  {
    Input target = new Input()
    {
	  private static final long serialVersionUID = 8796813407167561318L;

	  public void showMessage()
      {
        AllZone.Display.showMessage("Select target noncreature permanent");
        ButtonUtil.enableOnlyCancel();
      }
      public void selectButtonCancel() {stop();}
      public void selectCard(Card card, PlayerZone zone)
      {
        if(!card.isCreature() && zone.is(Constant.Zone.Play))
        {
          spell.setTargetCard(card);
          done();
        }
      }//selectCard()

      void done()
      {
        paid.execute();

        if(spell instanceof Ability_Tap && spell.getManaCost().equals("0"))
           stopSetNext(new Input_NoCost_TapAbility((Ability_Tap)spell));
        else if(spell.getManaCost().equals("0"))
        {
          AllZone.Stack.add(spell);
          stop();
        }
        else
          stopSetNext(new Input_PayManaCost(spell));
      }
    };
    return target;
  }//input_targetNonCreaturePermanent()

  public static Input input_targetPermanent(final SpellAbility spell)
  {
    Input target = new Input()
    {
      private static final long serialVersionUID = -7635051691776562901L;
	  
      public void showMessage()
      {
        AllZone.Display.showMessage("Select target permanent");
        ButtonUtil.enableOnlyCancel();
      }
      public void selectButtonCancel() {stop();}
      public void selectCard(Card card, PlayerZone zone)
      {
        if(card.isPermanent() && zone.is(Constant.Zone.Play))
        {
          spell.setTargetCard(card);
          done();
        }
      }//selectCard()
      void done()
      {

        if(spell instanceof Ability_Tap && spell.getManaCost().equals("0"))
           stopSetNext(new Input_NoCost_TapAbility((Ability_Tap)spell));
        else if(spell.getManaCost().equals("0"))
        {
          AllZone.Stack.add(spell);
          stop();
        }
        else
          stopSetNext(new Input_PayManaCost(spell));
      }
    };
    return target;
  }//input_targetPermanent()


  //CardList choices are the only cards the user can successful select
  //sacrifices one of the CardList choices
  public static Input input_sacrifice(final SpellAbility spell, final CardList choices, final String message)
  {
    Input target = new Input()
    {
	  private static final long serialVersionUID = 2685832214519141903L;
	
	  public void showMessage()
      {
        AllZone.Display.showMessage(message);
        ButtonUtil.enableOnlyCancel();
      }
      public void selectButtonCancel() {stop();}
      public void selectCard(Card card, PlayerZone zone)
      {
        if(choices.contains(card))
        {
          AllZone.getZone(card).remove(card);
          AllZone.GameAction.moveToGraveyard(card);

          if(spell.getManaCost().equals("0"))
          {
            AllZone.Stack.add(spell);
            stop();
          }
          else
            stopSetNext(new Input_PayManaCost(spell));
        }
      }
    };
    return target;
  }//input_sacrifice()
  
  
  public static Input input_sacrificePermanent(final CardList choices, final String message)
  {
    Input target = new Input()
    {
	  private static final long serialVersionUID = 2685832214519141903L;
	
	  public void showMessage()
      {
        AllZone.Display.showMessage(message);
        ButtonUtil.disableAll();
      }
      public void selectCard(Card card, PlayerZone zone)
      {
        if(choices.contains(card))
        {
          AllZone.getZone(card).remove(card);
          AllZone.GameAction.moveToGraveyard(card);
          stop();
        }
      }
    };
    return target;
  }//input_sacrifice()
  
  public static Input input_putFromHandToLibrary(final String TopOrBottom, final int num)
  {
     Input target = new Input()
     { 
      private static final long serialVersionUID = 5178077952030689103L;
      public int n = 0;
      
      public void showMessage()
        {
           AllZone.Display.showMessage("Select a card to put on the " + TopOrBottom + " of your library.");
           ButtonUtil.disableAll();
          
           if (n == num || AllZone.Human_Hand.getCards().length == 0)
              stop();
        }
        public void selectButtonCancel() {stop();}
        public void selectCard(Card card, PlayerZone zone)
        {
           if (zone.is(Constant.Zone.Hand))
           {
              AllZone.Human_Hand.remove(card);
             
              if (TopOrBottom.equals("top"))
                 AllZone.Human_Library.add(card, 0);
              else if (TopOrBottom.equals("bottom"))
                 AllZone.Human_Library.add(card);
             
              n++;
              if (n == num)
                 stop();
             
              showMessage();
           }
        }
     };
     return target;
  }
 
  public static Input input_discardNumUnless(final int nCards, final String uType)
  {
     Input target = new Input()
     {
      private static final long serialVersionUID = 8822292413831640944L;
      
      int n = 0;
        public void showMessage()
        {
           AllZone.Display.showMessage("Select " + (nCards - n) + " cards to discard, unless you discard a " + uType + ".");
           ButtonUtil.disableAll();
          
           if (n == nCards || AllZone.Human_Hand.getCards().length == 0)
              stop();
        }
        public void selectButtonCancel() {stop();}
        public void selectCard(Card card, PlayerZone zone)
        {
           if (zone.is(Constant.Zone.Hand))
           {
              AllZone.GameAction.discard(card);
              n++;
             
              if (card.getType().contains(uType))
                 stop();
             
              showMessage();
           }
        }
     };
    
     return target;
  }//input_discardNumUnless

  
  public static SpellAbility ability_Untap(final Card sourceCard, String cost)
  {
	  final SpellAbility a1 = new Ability(sourceCard, cost)
      {
        public boolean canPlay()
        {
          return sourceCard.isTapped();
        }
        public void resolve()
        {
          sourceCard.untap();
        }
      };//SpellAbility
      //sourceCard.addSpellAbility(a1);
      a1.setDescription(cost + ": Untap " +sourceCard.getName() +".");
      a1.setStackDescription("Untap " + sourceCard.getName());

      a1.setBeforePayMana(new Input_PayManaCost(a1));
      return a1;
  }
  
  public static SpellAbility ability_Flashback(final Card sourceCard, String manaCost, String lifeloss)
  {
	  final int loss = Integer.parseInt(lifeloss);
	  final SpellAbility flashback = new Spell(sourceCard)
		{
		  	
			private static final long serialVersionUID = -4196027546564209412L;
			public void resolve()
			{
				PlayerZone grave = AllZone.getZone(Constant.Zone.Graveyard ,sourceCard.getController());
				PlayerZone removed = AllZone.getZone(Constant.Zone.Removed_From_Play, sourceCard.getController());
				
				SpellAbility[] sa = sourceCard.getSpellAbility();
				
				if (sourceCard.getController().equals(Constant.Player.Human)) {
					//AllZone.GameAction.playSpellAbility(sa[0]);
					AllZone.Stack.add(sa[0]);
				}
				else {
					//ComputerUtil.playNoStack(sa[0]);
					AllZone.Stack.add(sa[0]);
				}
				grave.remove(sourceCard);
				removed.add(sourceCard);
				
				AllZone.GameAction.getPlayerLife(sourceCard.getController()).subtractLife(loss);
				
			}
			
			public boolean canPlayAI()
			{
				PlayerLife compLife = AllZone.GameAction.getPlayerLife("Computer");
	        	int life = compLife.getLife();
	        	
	        	
	        	return (life > (loss+2)); 
			}
			public boolean canPlay()
			{
				PlayerZone grave = AllZone.getZone(Constant.Zone.Graveyard ,sourceCard.getController());
				String phase         = AllZone.Phase.getPhase();
				String activePlayer  = AllZone.Phase.getActivePlayer();
				
				return AllZone.GameAction.isCardInZone(sourceCard, grave) && (sourceCard.isInstant() ||
						(phase.equals(Constant.Phase.Main1) || phase.equals(Constant.Phase.Main2)) &&
					    sourceCard.getController().equals(activePlayer) && AllZone.Stack.size() == 0 );
				
			}
			
		};
		
		String lifecost = "";
		if (loss != 0)
			lifecost = ", pay " + lifeloss + " life";
		
		
		flashback.setManaCost(manaCost);
		flashback.setDescription("Flashback: " + manaCost + lifecost);
		flashback.setStackDescription("Flashback: " + sourceCard.getName());
		
		return flashback;
    
  }//ability_Flashback()

  public static SpellAbility ability_Spore_Saproling(final Card sourceCard)
  {
    final SpellAbility ability = new Ability(sourceCard, "0")
    {
      public boolean canPlay()
      {
    	  SpellAbility sa;
    	  for (int i=0; i<AllZone.Stack.size(); i++)
    	  {
    	       sa = AllZone.Stack.peek(i);
    	       if (sa.getSourceCard().equals(sourceCard))
    	             return false;
    	  }
    	  
    	  if (sourceCard.getCounters(Counters.SPORE) >= 3 && AllZone.GameAction.isCardInPlay(sourceCard))
    		  return true;
    	  else
    		  return false;
      }
      public boolean canPlayAI() {return true;}

      public void resolve()
      {
        sourceCard.subtractCounter(Counters.SPORE, 3);
        
        PlayerZone play = AllZone.getZone(Constant.Zone.Play, sourceCard.getController());

        //make token
        Card c = new Card();

        c.setOwner(sourceCard.getController());
        c.setController(sourceCard.getController());

		c.setName("Saproling");
        c.setImageName("G 1 1 Saproling");
        c.setManaCost("G");
        c.setToken(true);

        c.addType("Creature");
        c.addType("Saproling");
        c.setBaseAttack(1);
        c.setBaseDefense(1);

        play.add(c);
      }
    };
    ability.setDescription("Remove three spore counters from " +sourceCard.getName() +": Put a 1/1 green Saproling creature token into play.");
    ability.setStackDescription(sourceCard.getName() +" - put a 1/1 green Saproling creature token into play.");
    return ability;
  }//ability_Spore_Saproling()
  
  public static SpellAbility ability_Morph_Down(final Card sourceCard)
  {
	  final String player = sourceCard.getController();
	  final SpellAbility morph_down = new Spell(sourceCard)
	  {
		private static final long serialVersionUID = -1438810964807867610L;
		
		public void resolve()
		{
			PlayerZone hand = AllZone.getZone(Constant.Zone.Hand ,player);
			PlayerZone play = AllZone.getZone(Constant.Zone.Play ,player);
			
			//card.setName("Morph");
			sourceCard.setIsFaceDown(true);
			sourceCard.setManaCost("");
			sourceCard.setBaseAttack(2);
			sourceCard.setBaseDefense(2);
			sourceCard.comesIntoPlay();
			sourceCard.setIntrinsicKeyword(new ArrayList<String>()); //remove all keywords
			sourceCard.setType(new ArrayList<String>()); //remove all types
			sourceCard.addType("Creature");
				
			hand.remove(sourceCard);
			play.add(sourceCard);
		}
		public boolean canPlay()
		{
			return AllZone.Phase.getActivePlayer().equals(sourceCard.getController()) && 
			   	   ( AllZone.Phase.getPhase().equals(Constant.Phase.Main1) || AllZone.Phase.getPhase().equals(Constant.Phase.Main2) ) &&
			   	   !AllZone.Phase.getPhase().equals("End of Turn") && !AllZone.GameAction.isCardInPlay(sourceCard);
		}
			
	};
	
	morph_down.setManaCost("3");
	morph_down.setDescription("You may play this face down as a 2/2 creature for 3. Turn it face up any time for its morph cost.");
	morph_down.setStackDescription("Morph - Creature 2/2");
	
	return morph_down; 
  }
  
  public static SpellAbility ability_Spellbomb(final Card sourceCard){
      final SpellAbility ability = new Ability(sourceCard, "1")
      {
        public boolean canPlay()
        {
                return AllZone.GameAction.isCardInPlay(sourceCard)&&!AllZone.Stack.getSourceCards().contains(sourceCard);//in play and not already activated(Sac cost problems)
        }
        public boolean canPlayAI() {return (AllZone.Computer_Hand.size() < 4)&&(AllZone.Computer_Library.size()>0)&&MyRandom.random.nextBoolean();}

        public void resolve()
        {
          AllZone.GameAction.drawCard(sourceCard.getController());
          AllZone.GameAction.sacrifice(getSourceCard());
        }
      };
      ability.setDescription("1, Sacrifice "+sourceCard.getName()+": Draw a card.");
      ability.setStackDescription(sourceCard.getName() +" - Draw a card.");
      return ability;
  }
  
  public static Ability ability_Morph_Up(final Card sourceCard, String cost, String orgManaCost, int a, int d)
  {
	  //final String player = sourceCard.getController();
	  //final String manaCost = cost;
	  final int attack = a;
	  final int defense = d;
	  final String origManaCost = orgManaCost;
	  final Ability morph_up = new Ability(sourceCard, cost)
	  {
		    private static final long serialVersionUID = -7892773658629724785L;
		    
			public void resolve()
			{
				//PlayerZone hand = AllZone.getZone(Constant.Zone.Hand ,player);
				//PlayerZone play = AllZone.getZone(Constant.Zone.Play ,player);
				
				//card.setName("Morph");
				sourceCard.setIsFaceDown(false);
				sourceCard.setManaCost(origManaCost);
				sourceCard.setBaseAttack(attack);
				sourceCard.setBaseDefense(defense);
				sourceCard.setIntrinsicKeyword(sourceCard.getPrevIntrinsicKeyword());
				sourceCard.setType(sourceCard.getPrevType());
				sourceCard.turnFaceUp();
			}
			public boolean canPlay()
			{
				return sourceCard.isFaceDown() && AllZone.GameAction.isCardInPlay(sourceCard);
			}
			
		};//morph_up
		
		morph_up.setManaCost(cost);
		morph_up.setDescription(cost + " - turn this card face up.");
		morph_up.setStackDescription(sourceCard.getName() +" - turn this card face up.");
		
		return morph_up;
	  
  }
/*
  public static SpellAbility spellability_spDamageP(final Card sourceCard, final String dmg)
  {
      final int damage = Integer.parseInt(dmg);
      
      final SpellAbility spDamageP = new Spell(sourceCard)
      {
		private static final long serialVersionUID = -1263171535312610675L;
		
		@SuppressWarnings("unused")  // check
		Card check;
         
         public boolean canPlayAI()
         {
                 return false;
         }
         
         public void chooseTargetAI()
         {
              CardFactoryUtil.AI_targetHuman();
               return;
         }
         
         public void resolve()
         {
                  AllZone.GameAction.getPlayerLife(getTargetPlayer()).subtractLife(damage);
         }
      };
      spDamageP.setDescription(sourceCard.getName() + " deals " + damage + " damage to target player.");
      spDamageP.setStackDescription(sourceCard.getName() +" deals " + damage + " damage.");
      spDamageP.setBeforePayMana(CardFactoryUtil.input_targetPlayer(spDamageP));
      return spDamageP;
  }//Spellability_spDamageP
  
  public static SpellAbility spellability_spDamageCP(final Card sourceCard, final String dmg)
  {
   final int damage = Integer.parseInt(dmg); // converting string dmg -> int

   final SpellAbility DamageCP =  new Spell(sourceCard)
   {
	private static final long serialVersionUID = 7239608350643325111L;
	
	Card check;
      //Shock's code here atm
      public boolean canPlayAI()
      {
          if(AllZone.Human_Life.getLife() <= damage)
            return true;
            
          PlayerZone compHand = AllZone.getZone(Constant.Zone.Hand, Constant.Player.Computer);
          CardList hand = new CardList(compHand.getCards());
               
           if (hand.size() >= 8)
            return true;
         
          check = getFlying();
          return check != null;
      }
          
      public void chooseTargetAI()
      {
          if(AllZone.Human_Life.getLife() <= damage)
          {
            setTargetPlayer(Constant.Player.Human);
            return;
          }
            
          PlayerZone compHand = AllZone.getZone(Constant.Zone.Hand, Constant.Player.Computer);
           CardList hand = new CardList(compHand.getCards());
            
          if(getFlying() == null && hand.size() >= 7 ) //not 8, since it becomes 7 when getting cast
           {
              setTargetPlayer(Constant.Player.Human);
              return;
           }
         
          Card c = getFlying();
          
          if (check == null &&  c != null)
        	  System.out.println("Check equals null");
          else if((c == null) || (! check.equals(c)))
            throw new RuntimeException(sourceCard +" error in chooseTargetAI() - Card c is " +c +",  Card check is " +check);
         
          if (c != null)
        	  setTargetCard(c);
          else
        	  setTargetPlayer(Constant.Player.Human);
      }//chooseTargetAI()
         
         //uses "damage" variable
      Card getFlying()
      {
    	  CardList flying = CardFactoryUtil.AI_getHumanCreature("Flying", sourceCard, true);
          for(int i = 0; i < flying.size(); i++)
        	  if(flying.get(i).getNetDefense() <= damage){
        		  System.out.println("getFlying() returns " + flying.get(i).getName());
        		  return flying.get(i);
        	  }
          
        System.out.println("getFlying() returned null");
        return null;
      }
      public void resolve()
      {
              
          if(getTargetCard() != null)
          {
            if(AllZone.GameAction.isCardInPlay(getTargetCard()) && canTarget(sourceCard, getTargetCard()))
            {
                Card c = getTargetCard();
                //c.addDamage(damage);
                AllZone.GameAction.addDamage(c, damage);
            }
          }
          else
            AllZone.GameAction.getPlayerLife(getTargetPlayer()).subtractLife(damage);
          //resolve()
      }
   }; //spellAbility
   DamageCP.setDescription(sourceCard.getName() + " deals " + damage + " damage to target creature or player.");
   DamageCP.setStackDescription(sourceCard.getName() +" deals " + damage + " damage.");
   DamageCP.setBeforePayMana(CardFactoryUtil.input_targetCreaturePlayer(DamageCP, true));
   return DamageCP;
  }//spellability_DamageCP
 */
  
  public static SpellAbility ability_Merc_Search(final Card sourceCard, String cost) 
  {
	  final int intCost = Integer.parseInt(cost);
	  //final String player = sourceCard.getController();
	  
	  final SpellAbility ability = new Ability_Tap(sourceCard, cost)
	  {
		private static final long serialVersionUID = 4988299801575232348L;

		public boolean canPlay()
	      {
	    	  SpellAbility sa;
	    	  for (int i=0; i<AllZone.Stack.size(); i++)
	    	  {
	    	       sa = AllZone.Stack.peek(i);
	    	       if (sa.getSourceCard().equals(sourceCard))
	    	             return false;
	    	  }
	    	  
	    	  if (AllZone.GameAction.isCardInPlay(sourceCard) && !sourceCard.hasSickness() && !sourceCard.isTapped())
	    		  return true;
	    	  else
	    		  return false;
	      }
	      public boolean canPlayAI() 
	      {
	    	  PlayerZone lib = AllZone.getZone(Constant.Zone.Library, sourceCard.getController());
	    	  CardList mercs = new CardList();
	    	  CardList list = new CardList(lib.getCards());
	    	  list = list.filter(new CardListFilter()
	    	  {
				public boolean addCard(Card c) {
					return (c.getType().contains("Mercenary") || c.getKeyword().contains("Changeling")) && c.isPermanent() ;
				}  
	    	  });
	    	  
	    	  
	    	  if (list.size()==0)
	    		  return false;
	    	  
	    	  for (int i=0;i < list.size(); i++)
	    	  {
	    		  if (CardUtil.getConvertedManaCost(list.get(i).getManaCost()) <= intCost)
	    		  {
	    			  mercs.add(list.get(i));
	    		  } 
	    	  }
	    	  
	    	  if (AllZone.Phase.getPhase().equals(Constant.Phase.Main2) && mercs.size() > 0)
	    		  return true;
	    	  else
	    		  return false;
	      }
	      
	      public void resolve()
	      {
	    	  PlayerZone lib = AllZone.getZone(Constant.Zone.Library, sourceCard.getController());
	    	  PlayerZone play = AllZone.getZone(Constant.Zone.Play, sourceCard.getController());
	    	  
	    	  CardList mercs = new CardList();
	    	  CardList list = new CardList(lib.getCards());
	    	  list = list.getType("Mercenary");
	    	  
	    	  if (list.size()==0)
	    		  return;
	    	  
	    	  for (int i=0;i < list.size(); i++)
	    	  {
	    		  if (CardUtil.getConvertedManaCost(list.get(i).getManaCost()) <= intCost)
	    		  {
	    			  mercs.add(list.get(i));
	    		  } 
	    	  }
	    	  if (mercs.size() == 0)
	    	  	return;
	    	  
	    	  if (sourceCard.getController().equals(Constant.Player.Computer))
	    	  {
	    		  Card merc = AI_getBestCreature(mercs);
	    		  lib.remove(merc);
	    		  play.add(merc);
	    	  }
	    	  else //human
	    	  {
	    		  Object o = AllZone.Display.getChoiceOptional("Select target Mercenary", mercs.toArray());
	    		  if (o!=null){
	    			  Card merc = (Card)o;
	    			  lib.remove(merc);
	    			  play.add(merc);
	    		  }
	    	  }
	    	  AllZone.GameAction.shuffle(sourceCard.getController());
	      }
	  };
	  ability.setDescription(cost+ ", tap: Search your library for a Mercenary permanent card with converted mana cost " +cost +" or less and put it into play. Then shuffle your library.");
	  ability.setStackDescription(sourceCard.getName() +" - search for a Mercenary and put it into play.");
	  return ability;
  }
  
  public static SpellAbility ability_Rebel_Search(final Card sourceCard, String cost) 
  {
	  String costMinusOne = "";
	  int a = Integer.parseInt(cost);
	  a--;
	  costMinusOne = Integer.toString(a);
	  final int converted = a;
	  //final String player = sourceCard.getController();
	  
	  final SpellAbility ability = new Ability_Tap(sourceCard, cost)
	  {
		private static final long serialVersionUID = 7219065355049285681L;

		public boolean canPlay()
	      {
	    	  SpellAbility sa;
	    	  for (int i=0; i<AllZone.Stack.size(); i++)
	    	  {
	    	       sa = AllZone.Stack.peek(i);
	    	       if (sa.getSourceCard().equals(sourceCard))
	    	             return false;
	    	  }
	    	  
	    	  if (AllZone.GameAction.isCardInPlay(sourceCard) && !sourceCard.hasSickness() && !sourceCard.isTapped())
	    		  return true;
	    	  else
	    		  return false;
	      }
	      public boolean canPlayAI() 
	      {
	    	  PlayerZone lib = AllZone.getZone(Constant.Zone.Library, sourceCard.getController());
	    	  
	    	  CardList rebels = new CardList();
	    	  CardList list = new CardList(lib.getCards());
	    	  list = list.filter(new CardListFilter()
	    	  {
				public boolean addCard(Card c) {
					return (c.getType().contains("Rebel") || c.getKeyword().contains("Changeling")) && c.isPermanent() ;
				}  
	    	  });
	    	  
	    	  if (list.size()==0)
	    		  return false;
	    	  
	    	  for (int i=0;i < list.size(); i++)
	    	  {
	    		  if (CardUtil.getConvertedManaCost(list.get(i).getManaCost()) <= converted)
	    		  {
	    			  rebels.add(list.get(i));
	    		  } 
	    	  }
	    	  
	    	  if (AllZone.Phase.getPhase().equals(Constant.Phase.Main2) && rebels.size() > 0)
	    		  return true;
	    	  else
	    		  return false;
	    	  
	      }
	      
	      
	      public void resolve()
	      {
	    	  
	    	  PlayerZone lib = AllZone.getZone(Constant.Zone.Library, sourceCard.getController());
	    	  PlayerZone play = AllZone.getZone(Constant.Zone.Play, sourceCard.getController());
	    	  
	    	  CardList rebels = new CardList();
	    	  CardList list = new CardList(lib.getCards());
	    	  list = list.getType("Rebel");
	    	  
	    	  if (list.size()==0)
	    		  return;
	    	  
	    	  for (int i=0;i < list.size(); i++)
	    	  {
	    		  if (CardUtil.getConvertedManaCost(list.get(i).getManaCost()) <= converted)
	    		  {
	    			  rebels.add(list.get(i));
	    		  } 
	    	  }
	    	  if (rebels.size() == 0)
	    	  	return;
	    	  
	    	  if (sourceCard.getController().equals(Constant.Player.Computer))
	    	  {
	    		  Card rebel = AI_getBestCreature(rebels);
	    		  lib.remove(rebel);
	    		  play.add(rebel);
	    	  }
	    	  else //human
	    	  {
	    		  Object o = AllZone.Display.getChoiceOptional("Select target Rebel", rebels.toArray());
	    		  if (o!=null){
	    			  Card rebel = (Card)o;
	    			  lib.remove(rebel);
	    			  play.add(rebel);
	    			  if (rebel.isAura())
	    			  {
	    				  Object obj = null;
		        		  if (rebel.getKeyword().contains("Enchant creature"))
		        		  {
		        			  PlayerZone oppPlay = AllZone.getZone(Constant.Zone.Play, Constant.Player.Computer);
			        		  CardList creats = new CardList(play.getCards());
			        		  creats.addAll(oppPlay.getCards());
			        		  creats = creats.getType("Creature");
			        		  obj = AllZone.Display.getChoiceOptional("Pick a creature to attach "+rebel.getName() + " to",creats.toArray() );
		        		  }
		        		  if (obj != null)
		        		  {
		        			  Card target = (Card)obj;
		        			  if(AllZone.GameAction.isCardInPlay(target)) {
		        	        	  rebel.enchantCard(target);
		        			  }
		        		  }

	    			  }
	    		  }
	    	  }
	    	  AllZone.GameAction.shuffle(sourceCard.getController());
	      }
	  };
	  ability.setDescription(cost+ ", tap: Search your library for a Rebel permanent card with converted mana cost " +costMinusOne +" or less and put it into play. Then shuffle your library.");
	  ability.setStackDescription(sourceCard.getName() +" - search for a Rebel and put it into play.");
	  return ability;
  }

  public static SpellAbility ability_cycle(final Card sourceCard, final String cycleCost)
  {
    final SpellAbility cycle = new Ability_Hand(sourceCard, cycleCost)
    {
	  private static final long serialVersionUID = -4960704261761785512L;

	  public boolean canPlayAI() {return false;}

      public void resolve()
      {
        AllZone.GameAction.discard(sourceCard);
        AllZone.GameAction.drawCard(sourceCard.getController());
        sourceCard.cycle();
      }
    };
    cycle.setDescription("Cycling "  +cycleCost +" (" +cycleCost +", Discard this card: Draw a card.)");
    cycle.setStackDescription(sourceCard +" Cycling: Draw a card");
    return cycle;
  }//ability_cycle()


  //CardList choices are the only cards the user can successful select
  public static Input input_targetSpecific(final SpellAbility spell, final CardList choices, final String message, final boolean targeted)
  {
    return input_targetSpecific(spell, choices, message, Command.Blank, targeted);
  }

  //CardList choices are the only cards the user can successful select
  public static Input input_targetSpecific(final SpellAbility spell, final CardList choices, final String message, final Command paid, final boolean targeted)
  {
    Input target = new Input()
    {
	  private static final long serialVersionUID = -1779224307654698954L;
	  
	  public void showMessage()
      {
        AllZone.Display.showMessage(message);
        ButtonUtil.enableOnlyCancel();
      }
      public void selectButtonCancel() {stop();}
      public void selectCard(Card card, PlayerZone zone)
      {
    	if (targeted && !canTarget(spell, card))
    	{
    		 AllZone.Display.showMessage("Cannot target this card (Shroud? Protection?).");
    	}
    	else if(choices.contains(card))
        {
          spell.setTargetCard(card);
          if(spell instanceof Ability_Tap && spell.getManaCost().equals("0"))
             stopSetNext(new Input_NoCost_TapAbility((Ability_Tap)spell));
          else if(spell.getManaCost().equals("0"))
          {
            AllZone.Stack.add(spell);
            stop();
          }
          else
            stopSetNext(new Input_PayManaCost(spell));

          paid.execute();
        }
      }//selectCard()
    };
    return target;
  }//input_targetSpecific()
  
  public static Input input_discard(final SpellAbility spell, final int nCards)
  {
	  Input target = new Input()
	  {
		private static final long serialVersionUID = 5101772642421944050L;
		
		int n = 0;
	      public void showMessage()
	      {
	        AllZone.Display.showMessage("Select a card to discard");
	        ButtonUtil.disableAll();
	        
	        if(n == nCards || AllZone.Human_Hand.getCards().length == 0)
	            stop();
	      }
	      public void selectButtonCancel() {stop();}
	      public void selectCard(Card card, PlayerZone zone)
	      {
	    	  if(zone.is(Constant.Zone.Hand))
	          {
	            AllZone.GameAction.discard(card);
	            n++;
	            if(spell.getManaCost().equals("0"))
		          {
		            AllZone.Stack.add(spell);
		            stop();
		          }
		        else
		        	stopSetNext(new Input_PayManaCost(spell));

	            //showMessage();
	          } // if
	        
	          
	        }//selectCard
	      
	    };
	    return target;
	  
  }
  
  public static Input input_discard()
  {
    return input_discard(1);
  }

  public static Input input_discard(final int nCards)
  {
    Input target = new Input()
    {
	  private static final long serialVersionUID = -329993322080934435L;
	
	  int n = 0;

      public void showMessage()
      {
        AllZone.Display.showMessage("Select a card to discard");
        ButtonUtil.disableAll();

        //in case no more cards in hand
        if(n == nCards || AllZone.Human_Hand.getCards().length == 0)
          stop();
      }
      public void selectCard(Card card, PlayerZone zone)
      {
        if(zone.is(Constant.Zone.Hand))
        {
          AllZone.GameAction.discard(card);
          n++;
          showMessage();
        }
      }
    };
    return target;
  }//input_discard()



  //cardType is like "Creature", "Land", "Artifact", "Goblin", "Legendary"
  //cardType can also be "All", which will allow any permanent to be selected
  public static Input input_targetType(final SpellAbility spell, final String cardType)
  {
    Input target = new Input()
    {
	  private static final long serialVersionUID = 4944828318048780429L;
	
	  public void showMessage()
      {
        AllZone.Display.showMessage("Select target " +cardType);

        if(cardType.equals("All"))
          AllZone.Display.showMessage("Select target permanent");

        ButtonUtil.enableOnlyCancel();
      }
      public void selectButtonCancel() {stop();}
      public void selectCard(Card card, PlayerZone zone)
      {
        if(((card.getType().contains(cardType) || card.getKeyword().contains("Changeling")) || cardType.equals("All")) &&
           zone.is(Constant.Zone.Play) && canTarget(spell, card))
        {
          spell.setTargetCard(card);
          stopSetNext(new Input_PayManaCost(spell));
        }
      }
    };
    return target;
  }//input_targetType()


  public static Input input_targetCreature(final SpellAbility spell)
  {
    return input_targetCreature(spell, Command.Blank);
  }

  public static Input input_targetCreature(final SpellAbility spell, final Command paid)
  {
    Input target = new Input()
    {
	  private static final long serialVersionUID = 141164423096887945L;
	  
	  public void showMessage()
      {
        AllZone.Display.showMessage("Select target creature for " +spell.getSourceCard());
        ButtonUtil.enableOnlyCancel();
      }
      public void selectButtonCancel() {stop();}
      public void selectCard(Card card, PlayerZone zone)
      {
    	if (!canTarget(spell, card))
    	{
    		 AllZone.Display.showMessage("Cannot target this card (Shroud? Protection?).");
    	}
    	else if(card.isCreature() && zone.is(Constant.Zone.Play))
        {
          spell.setTargetCard(card);
          done();
        }
      }
      void done()
      {
        if(spell instanceof Ability_Tap && spell.getManaCost().equals("0"))
          stopSetNext(new Input_NoCost_TapAbility((Ability_Tap)spell));
        else if(spell.getManaCost().equals("0"))//for "sacrifice this card" abilities
        {
          AllZone.Stack.add(spell);
          stop();
        }
        else
          stopSetNext(new Input_PayManaCost(spell));

        paid.execute();
      }
    };
    return target;
  }//input_targetCreature()

  public static Input input_targetCreature_NoCost_TapAbility(final Ability_Tap spell)
  {
    Input target = new Input()
    {
	  private static final long serialVersionUID = 6027194502614341779L;
	
	  public void showMessage()
      {
        AllZone.Display.showMessage("Select target creature");
        ButtonUtil.enableOnlyCancel();
      }
      public void selectButtonCancel() {stop();}
      public void selectCard(Card card, PlayerZone zone)
      {
        if(card.isCreature() && zone.is(Constant.Zone.Play) && canTarget(spell, card))
        {
          spell.setTargetCard(card);
          spell.getSourceCard().tap();
          AllZone.Stack.push(spell);
          stop();
        }
      }
    };
    return target;
  }//input_targetCreature()
  
  public static Input input_targetCreature_NoCost_TapAbility_NoTargetSelf(final Ability_Tap spell)
  {
    Input target = new Input()
    {
	  private static final long serialVersionUID = -6310420275914649718L;
	
	  public void showMessage()
      {
        AllZone.Display.showMessage("Select target creature other than "+spell.getSourceCard().getName());
        ButtonUtil.enableOnlyCancel();
      }
      public void selectButtonCancel() {stop();}
      public void selectCard(Card card, PlayerZone zone)
      {
       if(card == spell.getSourceCard()){
          AllZone.Display.showMessage("You must select a target creature other than "+spell.getSourceCard().getName());
       }
       else if(card.isCreature() && zone.is(Constant.Zone.Play) && !card.getKeyword().contains("Shroud"))
        {
          spell.setTargetCard(card);
          spell.getSourceCard().tap();
          AllZone.Stack.push(spell);
          stop();
        }
      }
    };
    return target;
  }//input_targetCreature_NoCost_TapAbility_NoTargetSelf



  public static Input input_targetPlayer(final SpellAbility spell)
  {
    Input target = new Input()
    {
	  private static final long serialVersionUID = 8736682807625129068L;
	
	  public void showMessage()
      {
        AllZone.Display.showMessage("Select target player");
        ButtonUtil.enableOnlyCancel();
      }
      public void selectButtonCancel() {stop();}
      public void selectPlayer(String player)
      {
        spell.setTargetPlayer(player);
        if(spell.getManaCost().equals("0"))
        {
          AllZone.Stack.add(spell);
          stop();
        }
        else
          stopSetNext(new Input_PayManaCost(spell));
      }
    };
    return target;
  }//input_targetPlayer()
  
  public static Input input_targetPlayer(final SpellAbility spell, final Command command)
  {
    Input target = new Input()
    {
	  private static final long serialVersionUID = 8736682807625129068L;
	
	  public void showMessage()
      {
        AllZone.Display.showMessage("Select target player");
        ButtonUtil.enableOnlyCancel();
      }
      public void selectButtonCancel() {stop();}
      public void selectPlayer(String player)
      {
    	command.execute();
    	  
        spell.setTargetPlayer(player);
        if(spell.getManaCost().equals("0"))
        {
          AllZone.Stack.add(spell);
          stop();
        }
        else
          stopSetNext(new Input_PayManaCost(spell));
      }
    };
    return target;
  }//input_targetPlayer()

  public static CardList AI_getHumanCreature(final Card spell, boolean targeted)
  {
    CardList creature = new CardList(AllZone.Human_Play.getCards());
    creature = creature.getType("Creature");
    if (targeted)
    {
    	creature = creature.filter(new CardListFilter(){
			public boolean addCard(Card c) {
				return canTarget(spell, c);
			}
    	});
    }
    return creature;
  }
  public static CardList AI_getHumanCreature(final String keyword, final Card spell, final boolean targeted)
  {
    CardList creature = new CardList(AllZone.Human_Play.getCards());
    creature = creature.filter(new CardListFilter()
    {
      public boolean addCard(Card c)
      {
    	  if(targeted)    	
    		  return c.isCreature() && c.getKeyword().contains(keyword) && canTarget(spell, c);
    	  else
    		  return c.isCreature() && c.getKeyword().contains(keyword); 
      }
    });
    return creature;
  }//AI_getHumanCreature()
  public static CardList AI_getHumanCreature(final int toughness, final Card spell, final boolean targeted)
  {
    CardList creature = new CardList(AllZone.Human_Play.getCards());
    creature = creature.filter(new CardListFilter()
    {
      public boolean addCard(Card c)
      {
    	if(targeted)  
    	  return c.isCreature() && (c.getNetDefense() <= toughness) && canTarget(spell, c);
    	else
    	  return c.isCreature() && (c.getNetDefense() <= toughness);
      }
    });
    return creature;
  }//AI_getHumanCreature()
  
  public static CardList AI_getHumanCreature(final boolean lower, final int manaCost,final Card spell, final boolean targeted)
  {
    CardList creature = new CardList(AllZone.Human_Play.getCards());
    creature = creature.filter(new CardListFilter()
    {
      public boolean addCard(Card c)
      {
    	if(targeted && lower)  
    	  return c.isCreature() && (CardUtil.getConvertedManaCost(c.getManaCost()) <= 3) && canTarget(spell, c);
    	else if (lower)
    	  return c.isCreature() && (CardUtil.getConvertedManaCost(c.getManaCost()) <= 3);
    	
    	else if (targeted && !lower)
    		return c.isCreature() && (CardUtil.getConvertedManaCost(c.getManaCost()) >= 3) && canTarget(spell, c);
    	else //if !targeted && !lower
    		return c.isCreature() && (CardUtil.getConvertedManaCost(c.getManaCost()) >= 3);
      }
    });
    return creature;
  }//AI_getHumanCreature()

  public static CommandArgs AI_targetHumanCreatureOrPlayer()
  {
    return new CommandArgs()
    {
	  private static final long serialVersionUID = 1530080942899792553L;

	  public void execute(Object o)
      {
        SpellAbility sa = (SpellAbility)o;

        CardList creature = new CardList(AllZone.Human_Play.getCards());
        creature = creature.getType("Creature");
        Card c = getRandomCard(creature);

        if((c == null) || random.nextBoolean())
        {
          sa.setTargetPlayer(Constant.Player.Human);
        }
        else
        {
          sa.setTargetCard(c);
        }
      }
    };//CommandArgs
  }//human_creatureOrPlayer()
  public static CommandArgs AI_targetHuman()
  {
    return new CommandArgs()
    {
	  private static final long serialVersionUID = 8406907523134006697L;

	  public void execute(Object o)
      {
        SpellAbility sa = (SpellAbility)o;
        sa.setTargetPlayer(Constant.Player.Human);
      }
    };
  }//targetHuman()

  //is it the computer's main phase before attacking?
  public static boolean AI_isMainPhase()
  {
    return AllZone.Phase.getPhase().equals(Constant.Phase.Main1) &&
           AllZone.Phase.getActivePlayer().equals(Constant.Player.Computer);
  }
  public static CommandArgs AI_targetComputer()
  {
    return new CommandArgs()
    {
	  private static final long serialVersionUID = -445231553588926627L;

	  public void execute(Object o)
      {
        SpellAbility sa = (SpellAbility)o;
        sa.setTargetPlayer(Constant.Player.Computer);
      }
    };
  }//targetComputer()

  //type can also be "All"
  public static CommandArgs AI_targetType(final String type, final PlayerZone zone)
  {
    return new CommandArgs()
    {
      private static final long serialVersionUID = 6475810798098105603L;

	  public void execute(Object o)
      {
        CardList filter = new CardList(zone.getCards());

        if(! type.equals("All"))
          filter = filter.getType(type);

        Card c = getRandomCard(filter);
        if(c != null)
        {
          SpellAbility sa = (SpellAbility)o;
          sa.setTargetCard(c);

          //doesn't work for some reason
//          if(shouldAttack && CombatUtil.canAttack(c))
//            AllZone.Combat.addAttacker(c);
        }
      }//execute()
    };
  }//targetInPlay()
  
  public static int getNumberOfPermanentsByColor(String color)
  {
	CardList cards = new CardList();
	cards.addAll(AllZone.Human_Play.getCards());
	cards.addAll(AllZone.Computer_Play.getCards());
	
	CardList coloredPerms = new CardList();
	
	for (int i=0; i<cards.size(); i++)
	{
		if(CardUtil.getColors(cards.get(i)).contains(color))
			coloredPerms.add(cards.get(i));
	}
	return coloredPerms.size();  
  }
  
  public static boolean multipleControlled(Card c)
  {
	  PlayerZone play = AllZone.getZone(c);
	  CardList list = new CardList(play.getCards());	  
	  list.remove(c);
	  
	 return list.containsName(c.getName());
  }
  
  public static boolean controlsAnotherMulticoloredPermanent(Card c)
  {
	  PlayerZone play = AllZone.getZone(Constant.Zone.Play, c.getController());
	  
	  final Card crd = c;
	  
	  CardList list = new CardList(play.getCards());
	  list = list.filter(new CardListFilter()
	  {

		public boolean addCard(Card c) {
			return !c.equals(crd) && CardUtil.getColors(c).size() >= 2;
		}
	  
	  });
	  
	  return list.size() >= 1;
	  
  }
  
  public static int getNumberOfManaSymbolsControlledByColor(String colorAbb, String player)
  {
	  PlayerZone play = AllZone.getZone(Constant.Zone.Play,player);
	  
	  CardList cards = new CardList();
	  cards.addAll(play.getCards());
	  
	  int count = 0;
	  for(int i=0;i<cards.size();i++)
	  {
		  Card c = cards.get(i);
		  if (!c.isToken())
		  {
			  String manaCost = c.getManaCost();
			  manaCost = manaCost.trim();
			  count += countOccurrences(manaCost, colorAbb);
		  }
	  }
	  return count;
  }
  public static boolean canTarget(SpellAbility ability, Card target)
  {
	  return canTarget(ability.getSourceCard(), target);
  }
  
  public static boolean canTarget(Card spell, Card target)
  {
	  if (target == null)
		  return true;
	  //System.out.println("Target:" + target);
	  if (target.getKeyword() != null)
	  {
		  ArrayList<String> list = target.getKeyword();
		  
		  String kw = "";
		  for (int i=0;i<list.size();i++)
		  {
			  kw = list.get(i);
			  if (kw.equals("Shroud"))
				  return false;
			  
			  if (kw.equals("This card can't be the target of spells or abilities your opponents control."))
			  {
				  if (!spell.getController().equals(target.getController()))
					  return false;
			  }
			  
			  if (kw.equals("This card can't be the target of Aura spells."))
			  {
				  if (spell.isAura())
					  return false;
			  }

			  if (kw.equals("Protection from white") && CardUtil.getColors(spell).contains(Constant.Color.White))
				  return false;
			  if (kw.equals("Protection from blue") && CardUtil.getColors(spell).contains(Constant.Color.Blue))
				  return false;
			  if (kw.equals("Protection from black") && CardUtil.getColors(spell).contains(Constant.Color.Black))
				  return false;
			  if (kw.equals("Protection from red") && CardUtil.getColors(spell).contains(Constant.Color.Red))
				  return false;
			  if (kw.equals("Protection from green") && CardUtil.getColors(spell).contains(Constant.Color.Green))
				  return false;
			  
			  if (kw.equals("Protection from creatures") && spell.isCreature())
				  return false;
			  
			  if (kw.equals("Protection from artifacts") && spell.isArtifact())
				  return false;
			  
			  if (kw.equals("Protection from Dragons") && (spell.getType().contains("Dragon") || spell.getKeyword().contains("Changeling") ))
				  return false;
			  if (kw.equals("Protection from Demons") && (spell.getType().contains("Demon") || spell.getKeyword().contains("Changeling") ))
				  return false;
			  if (kw.equals("Protection from Goblins") && (spell.getType().contains("Goblin") || spell.getKeyword().contains("Changeling") ))
				  return false;
			  
			  if (kw.equals("Protection from enchantments") && spell.getType().contains("Enchantment"))
				  return false;
			  
			  if (kw.equals("Protection from everything"))
				  return false;
		  }
	  }
	  return true;
  }
  //does "target" have protection from "card"?
  public static boolean hasProtectionFrom(Card card, Card target)
  {
	  if (target == null)
		  return false;

	  if (target.getKeyword() != null)
	  {
		  ArrayList<String> list = target.getKeyword();
		  
		  String kw = "";
		  for (int i=0;i<list.size();i++)
		  {
			  kw = list.get(i);

			  
			  if (kw.equals("Protection from white") && CardUtil.getColors(card).contains(Constant.Color.White))
				  return true;
			  if (kw.equals("Protection from blue") && CardUtil.getColors(card).contains(Constant.Color.Blue))
				  return true;
			  if (kw.equals("Protection from black") && CardUtil.getColors(card).contains(Constant.Color.Black))
				  return true;
			  if (kw.equals("Protection from red") && CardUtil.getColors(card).contains(Constant.Color.Red))
				  return true;
			  if (kw.equals("Protection from green") && CardUtil.getColors(card).contains(Constant.Color.Green))
				  return true;
			  
			  if (kw.equals("Protection from creatures") && card.isCreature())
				  return true;
			  
			  if (kw.equals("Protection from artifacts") && card.isArtifact())
				  return true;
			  
			  if (kw.equals("Protection from everything"))
				  return true;
			  
			  if (kw.equals("Protection from Dragons") && (card.getType().contains("Dragon") || card.getKeyword().contains("Changeling") ))
				  return true;
			  if (kw.equals("Protection from Demons") && (card.getType().contains("Demon") || card.getKeyword().contains("Changeling") ))
				  return true;
			  if (kw.equals("Protection from Goblins") && (card.getType().contains("Goblin") || card.getKeyword().contains("Changeling") ))
				  return true;
			  
			  if (kw.equals("Protection from enchantments") && card.getType().contains("Enchantment"))
				  return true;
		  }
	  }
	  return false;
  }
  
  public static boolean canDamage(Card spell, Card receiver)
  {
	  //this is for untargeted damage spells, such as Pestilence, Pyroclasm, Tremor, etc. 
	  //and also combat damage?
	  ArrayList<String> list = receiver.getKeyword();
	  
	  String kw = "";
	  for (int i=0;i<list.size();i++)
	  {
		  kw = list.get(i);
		  
		  if (kw.equals("Protection from white") && CardUtil.getColors(spell).contains(Constant.Color.White))
			  return false;
		  if (kw.equals("Protection from blue") && CardUtil.getColors(spell).contains(Constant.Color.Blue))
			  return false;
		  if (kw.equals("Protection from black") && CardUtil.getColors(spell).contains(Constant.Color.Black))
			  return false;
		  if (kw.equals("Protection from red") && CardUtil.getColors(spell).contains(Constant.Color.Red))
			  return false;
		  if (kw.equals("Protection from green") && CardUtil.getColors(spell).contains(Constant.Color.Green))
			  return false;
		  
		  if (kw.equals("Protection from creatures") && spell.isCreature())
			  return false;
		  
		  if (kw.equals("Protection from artifacts") && spell.isArtifact())
			  return false;
		  
		  if (kw.equals("Protection from Dragons") && (spell.getType().contains("Dragon") || spell.getKeyword().contains("Changeling") ))
			  return false;
		  if (kw.equals("Protection from Demons") && (spell.getType().contains("Demon") || spell.getKeyword().contains("Changeling") ))
			  return false;
		  if (kw.equals("Protection from Goblins") && (spell.getType().contains("Goblin") || spell.getKeyword().contains("Changeling") ))
			  return false;
		  
		  if (kw.equals("Protection from enchantments") && spell.getType().contains("Enchantment"))
			  return false;
		  
		  if (kw.equals("Protection from everything"))
			  return false;
	  }
	  
	  return true;
	  
  }
  
  public static boolean isCounterable(Card c)
  {
	  if (!c.getKeyword().contains("This card can't be countered."))
		  return true;
	  else
		  return false;
  }
  
  //returns the number of enchantments named "e" card c is enchanted by
  public static int hasNumberEnchantments(Card c, String e)
  {
	  if (!c.isEnchanted())
		  return 0;
	  
	  final String enchantmentName = e;
	  CardList list = new CardList(c.getEnchantedBy().toArray());
	  list = list.filter(new CardListFilter()
	  {
		public boolean addCard(Card c) {
			return c.getName().equals(enchantmentName);
		}
		  
	  });
	  
	  return list.size();
	  
  }
  
  //returns the number of equipments named "e" card c is equipped by
  public static int hasNumberEquipments(Card c, String e)
  {
	  if (!c.isEquipped())
		  return 0;
	  
	  final String equipmentName = e;
	  CardList list = new CardList(c.getEquippedBy().toArray());
	  list = list.filter(new CardListFilter()
	  {
		public boolean addCard(Card c) {
			return c.getName().equals(equipmentName);
		}
		  
	  });
	  
	  return list.size();
	  
  }
  /*public static CardList getValuableCreatures() 
  {
	  
  }
  */
  
  
  public static CardList getFlashbackCards(String player)
  {
	  PlayerZone grave = AllZone.getZone(Constant.Zone.Graveyard, player);
	  CardList cl = new CardList(grave.getCards());
      cl = cl.filter(new CardListFilter()
      {
			public boolean addCard(Card c) {
				return c.hasFlashback();
			}	
      });  
      return cl;
  }
  
  public static int countOccurrences(String arg1, String arg2) {
  
            int count = 0;
            int index = 0;
            while ((index = arg1.indexOf(arg2, index)) != -1) {
            	++index;
                ++count;
            }
            return count;
  }
  
  //parser for non-mana X variables
  public static int xCount(Card c, String s)
  {
     int n = 0;
    
      String cardController = c.getController();
      String oppController = AllZone.GameAction.getOpponent(cardController);
     
      PlayerZone myField = AllZone.getZone(Constant.Zone.Play, cardController);
      PlayerZone opField = AllZone.getZone(Constant.Zone.Play, oppController);

      PlayerZone myYard = AllZone.getZone(Constant.Zone.Graveyard, cardController);
      PlayerZone opYard = AllZone.getZone(Constant.Zone.Graveyard, oppController);
     
      PlayerZone myHand = AllZone.getZone(Constant.Zone.Hand, cardController);
      PlayerZone opHand = AllZone.getZone(Constant.Zone.Hand, oppController);
     
      final String [] l;
      l = s.split("/");      // separate the specification from any math
      final String m[] = {"none"};
      if (l.length > 1)
         m[0] = l[1];
      final String [] sq;
      sq = l[0].split("\\.");

      CardList someCards = new CardList();
     
      //Complex counting methods
     
      // Count$Domain
      if (sq[0].contains("Domain"))
      {
         someCards.addAll(myField.getCards());
         String basic[] = {"Forest", "Plains", "Mountain", "Island", "Swamp"};

         for(int i = 0; i < basic.length; i++)
            if (! someCards.getType(basic[i]).isEmpty())
               n++;
           
         return doXMath(n, m);
      }
     
      // Count$YourLifeTotal
      if (sq[0].contains("YourLifeTotal"))
      {
         if (cardController.equals(Constant.Player.Computer))
            return doXMath(AllZone.Computer_Life.getLife(), m);
         else if (cardController.equals(Constant.Player.Human))
            return doXMath(AllZone.Human_Life.getLife(), m);
        
         return 0;
      }
     
      // Count$OppLifeTotal
      if (sq[0].contains("OppLifeTotal"))
      {
         if (oppController.equals(Constant.Player.Computer))
            return doXMath(AllZone.Computer_Life.getLife(), m);
         else if (oppController.equals(Constant.Player.Human))
            return doXMath(AllZone.Human_Life.getLife(), m);
        
         return 0;
      }
     
      // Count$Chroma.<mana letter>
      if (sq[0].contains("Chroma"))
           return doXMath(getNumberOfManaSymbolsControlledByColor(sq[1], cardController), m);

      // Count$Hellbent.<numHB>.<numNotHB>
      if (sq[0].contains("Hellbent"))
         if (myHand.size() <= 1)
            return doXMath(Integer.parseInt(sq[1]), m);   // Hellbent
         else
            return doXMath(Integer.parseInt(sq[2]), m);    // not Hellbent

    
      //Generic Zone-based counting
     // Count$QualityAndZones.Subquality
     
      // build a list of cards in each possible specified zone

      // if a card was ever written to count two different zones,
      // make sure they don't get added twice.
      boolean MF = false, MY = false, MH = false;
      boolean OF = false, OY = false, OH = false;
     
      if (sq[0].contains("YouCtrl"))
         if (MF == false)
         {
            someCards.addAll(myField.getCards());
            MF = true;
         }

      if (sq[0].contains("InYourYard"))
         if (MY == false)
         {
            someCards.addAll(myYard.getCards());
            MY = true;
         }

      if (sq[0].contains("InYourHand"))
         if (MH == false)
         {
            someCards.addAll(myHand.getCards());
            MH = true;
         }

      if (sq[0].contains("OppCtrl"))
         if (OF == false)
         {
            someCards.addAll(opField.getCards());
            OF = true;
         }

      if (sq[0].contains("InOppYard"))
         if (OY == false)
         {
            someCards.addAll(opYard.getCards());
            OY = true;
         }
     
      if (sq[0].contains("InOppHand"))
         if (OH == false)
         {
            someCards.addAll(opHand.getCards());
            OH = true;
         }
     
      if (sq[0].contains("OnBattlefield"))
      {
         if (MF == false)
            someCards.addAll(myField.getCards());
         if (OF == false)
            someCards.addAll(opField.getCards());
      }

      if (sq[0].contains("InAllYards"))
      {
         if (MY == false)
            someCards.addAll(myYard.getCards());
         if (OY = false)
            someCards.addAll(opYard.getCards());
      }

      if (sq[0].contains("InAllHands"))
      {
         if (MH == false)
            someCards.addAll(myHand.getCards());
         if (OH == false)
            someCards.addAll(opHand.getCards());
      }

      // filter lists based on the specified quality
     
      // "Clerics you control" - Count$TypeYouCtrl.Cleric
      if (sq[0].contains("Type"))
      {
         someCards = someCards.filter(new CardListFilter()
         {
            public boolean addCard(Card c)
            {
               if (c.getType().contains(sq[1]) || c.getKeyword().contains("Changeling"))
                  return true;

               return false;
            }
         });
      }

      // "Named <CARDNAME> in all graveyards" - Count$NamedAllYards.<CARDNAME>
     
      if (sq[0].contains("Named"))
      {
         someCards = someCards.filter(new CardListFilter()
         {
            public boolean addCard(Card c)
            {
               if (c.getName().equals(sq[1]))
                  return true;

               return false;
            }
         });
      }

      // Refined qualities

      // "Untapped Lands" - Count$UntappedTypeYouCtrl.Land
      if (sq[0].contains("Untapped"))
      {
         someCards = someCards.filter(new CardListFilter()
         {
            public boolean addCard(Card c){
               return !c.isTapped();}
         });
      }
     
      if (sq[0].contains("Tapped"))
      {
         someCards = someCards.filter(new CardListFilter()
         {
            public boolean addCard(Card c){
               return c.isTapped();}
         });
      }

      // "White Creatures" - Count$WhiteTypeYouCtrl.Creature
      if (sq[0].contains("White"))
      {
         someCards = someCards.filter(new CardListFilter()
         {
            public boolean addCard(Card c){
               return CardUtil.getColor(c) == Constant.Color.White;}
         });
      }

      if (sq[0].contains("Blue"))
      {
         someCards = someCards.filter(new CardListFilter()
         {
            public boolean addCard(Card c){
               return CardUtil.getColor(c) == Constant.Color.Blue;}
         });
      }

      if (sq[0].contains("Black"))
      {
         someCards = someCards.filter(new CardListFilter()
         {
            public boolean addCard(Card c){
               return CardUtil.getColor(c) == Constant.Color.Black;}
         });
      }

      if (sq[0].contains("Red"))
      {
         someCards = someCards.filter(new CardListFilter()
         {
            public boolean addCard(Card c){
               return CardUtil.getColor(c) == Constant.Color.Red;}
         });
      }

      if (sq[0].contains("Green"))
      {
         someCards = someCards.filter(new CardListFilter()
         {
            public boolean addCard(Card c){
               return CardUtil.getColor(c) == Constant.Color.Green;}
         });
      }
     
      if (sq[0].contains("Multicolor"))
         someCards = someCards.filter(new CardListFilter ()
         {
            public boolean addCard(Card c){
               return (CardUtil.getColors(c).size() > 1);
            }
         });
     
      if (sq[0].contains("Monocolor"))
         someCards = someCards.filter(new CardListFilter ()
         {
            public boolean addCard(Card c){
               return (CardUtil.getColors(c).size() == 1);
            }
         });

      n = someCards.size();
     
      return doXMath(n, m);
  }

  private static int doXMath(int num, String[] m)
  {
     if (m[0].equals("none"))
        return num;
    
     String[] s = m[0].split("\\.");
         
     if (s[0].contains("Plus"))
        return num + Integer.parseInt(s[1]);
     else if (s[0].contains("NMinus"))
        return Integer.parseInt(s[1]) - num;
     else if (s[0].contains("Minus"))
        return num - Integer.parseInt(s[1]);
     else if (s[0].contains("Twice"))
        return num * 2;
     else if (s[0].contains("HalfUp"))
        return (int) (Math.ceil(num / 2));
     else if (s[0].contains("HalfDown"))
        return (int) (Math.floor(num / 2));
    
     return num;
  }

 
  public static void doDrawBack(String DB, int nDB, String cardController, String Opp, String TgtP, Card Src, Card TgtC)
  { 
     // Drawbacks may be any simple additional effect a spell or ability may have
     // not just the negative ones
    
     String d[] = DB.split("/");
     int X;
     if (d[1].equals("X"))
        X = nDB;
     else
        X = Integer.parseInt(d[1]);
    
     String dbPlayer = new String();
     if (d[0].contains("You"))
        dbPlayer = cardController;
     else if (d[0].contains("Opp"))
        dbPlayer = Opp;
     else if (d[0].contains("Tgt"))
        dbPlayer = TgtP;
    
     if (d[0].contains("Damage"))
        AllZone.GameAction.addDamage(dbPlayer, X);
    
     if (d[0].contains("GainLife"))
        AllZone.GameAction.addLife(dbPlayer, X);
         
     if (d[0].contains("LoseLife"))
        AllZone.GameAction.subLife(TgtP, X);
         
     if (d[0].contains("Discard"))
     {       
        if (d.length > 2)
        {
           if (d[2].contains("UnlessDiscardType"))
           {
              String dd[] = d[2].split("\\.");
              AllZone.GameAction.discardUnless(dbPlayer, X, dd[1]);
           }
           if (d[2].contains("AtRandom"))
              AllZone.GameAction.discardRandom(dbPlayer, X);
        } else
           AllZone.GameAction.discard(dbPlayer, X);
     }
    
     if (d[0].contains("HandToLibrary"))
        AllZone.GameAction.handToLibrary(dbPlayer, X, d[2]);
    
     if (d[0].contains("Draw"))
        for (int i=0; i < X; i++)
           AllZone.GameAction.drawCard(dbPlayer);
    
     if (d[0].contains("GenToken")) // placeholder for effect
        X = X + 0;
         
     if (d[0].contains("ReturnFromYard")) // placeholder for effect
        X = X + 0;
       
     if (d[0].contains("Sacrifice")) // placeholder for effect
        X = X + 0;
  }
  public static int getNumberOfMostProminentCreatureType(CardList list, String type)
  {
	 list = list.getType(type); 
	 return list.size();
  }
  
  public static String getMostProminentCreatureType(CardList list)
  {
	  
	  Map<String,Integer> map = new HashMap<String,Integer>();
	  String s = "";
	  
	  for (int i=0;i<list.size();i++)
	  {
		  Card c = list.get(i);
		  ArrayList<String> typeList = c.getType();
		  
		  for (String var : typeList)
		  {
			  if (var.equals("Creature") || var.equals("Artifact") || var.equals("Land") || var.equals("Tribal") || var.equals("Enchantment") || 
				  var.equals("Legendary") )
				  ;
			  else if (!map.containsKey(var))
				  map.put(var, 1);
			  else 
			  {
				  map.put(var, map.get(var)+1);
			  }
		  }
	  }//for
	  
	  int max = 0;
	  String maxType = "";
	  
	  for (int i=0;i<map.size();i++)
	  {
		  Iterator<String> iter = map.keySet().iterator();
		  while(iter.hasNext()) {
			  String type = iter.next();
		      System.out.println(type + " - " + map.get(type));
		      
		      if (max < map.get(type))
		      {
		    	  max = map.get(type);
		    	  maxType = type;
		      }
		  }
	  }
	  s = maxType;
	  return s;
  }
  
  
  public static String chooseCreatureTypeAI(Card c)
  {
	  String s = "";
	  //TODO, take into account what human has
	  
	  PlayerZone humanPlayZone = AllZone.getZone(Constant.Zone.Play, Constant.Player.Human); 
	  PlayerZone humanLibZone  = AllZone.getZone(Constant.Zone.Library, Constant.Player.Human);
	  
	  CardList humanPlay = new CardList(humanPlayZone.getCards());
	  CardList humanLib  = new CardList(humanLibZone.getCards());
	  
	  PlayerZone compPlayZone = AllZone.getZone(Constant.Zone.Play, Constant.Player.Computer); 
	  PlayerZone compLibZone  = AllZone.getZone(Constant.Zone.Library, Constant.Player.Computer);
	  PlayerZone compHandZone = AllZone.getZone(Constant.Zone.Hand, Constant.Player.Computer);
	  
	  CardList compPlay = new CardList(compPlayZone.getCards());
	  CardList compLib  = new CardList(compLibZone.getCards());
	  CardList compHand = new CardList(compHandZone.getCards());

	  humanPlay = humanPlay.getType("Creature");
	  humanLib = humanLib.getType("Creature");
	  
	  compPlay = compPlay.getType("Creature");
	  compLib = compLib.getType("Creature");
	  
	  //Buffs
	  if (c.getName().equals("Conspiracy") || c.getName().equals("Cover of Darkness") || c.getName().equals("Belbe's Portal") ||
		  c.getName().equals("Steely Resolve") || c.getName().equals("Shared Triumph"))
	  {	
		  
		  String type = "";
		  int number = 0;
		  if ((c.getName().equals("Shared Triumph") || c.getName().equals("Cover of Darkness") || c.getName().equals("Steely Resolve") )&& compPlay.size() > 7)
		  {
			 type = getMostProminentCreatureType(compPlay);
			 number = getNumberOfMostProminentCreatureType(compPlay, type);
			 
		  }
		  
		  if (number >= 3)
				 s = type;	
		  else {
			  type = getMostProminentCreatureType(compLib);
			  number = getNumberOfMostProminentCreatureType(compLib, type);
			  if (number >= 5)
				  s = type;	 
			  
		  }
		  
		  CardList turnTimber = new CardList();
		  turnTimber.addAll(compPlay.toArray());
		  turnTimber.addAll(compLib.toArray());
		  turnTimber.addAll(compHand.toArray());
		  
		  turnTimber = turnTimber.getName("Turntimber Ranger");
		  
		  if (c.getName().equals("Conspiracy") && turnTimber.size() > 0 )
			  s = "Ally";
		  
	  }
	  //Debuffs
	  else if(c.getName().equals("Engineered Plague"))
	  {
		  String type = "";
		  int number = 0;
		  if (c.getName().equals("Engineered Plague") && humanPlay.size() > 6)
		  {
			 type = getMostProminentCreatureType(humanPlay);
			 number = getNumberOfMostProminentCreatureType(humanPlay, type);
			 if (number >= 3) 
				 s = type;	 
			 else if (humanLib.size()>0)
			 {
				 type = getMostProminentCreatureType(humanLib);
				 number = getNumberOfMostProminentCreatureType(humanLib, type);
				 if (number >=5)
					 s = type;
			 }
		  }
	  }
	  return s;
  }

  public static int getCanPlayNumberOfLands(String player)
  {
	  int count = 1;
	  CardList list = new CardList(AllZone.getZone(Constant.Zone.Play, player).getCards());
	  list = list.filter(new CardListFilter()
	  {
		public boolean addCard(Card c) {
			return c.getName().equals("Exploration") || c.getName().equals("Azusa, Lost but Seeking") || c.getName().equals("Fastbond");
		}  
	  });
	  
	  for (Card var : list)
	  {
		  if (var.getName().equals("Exploration"))
			  count++;
		  else if (var.getName().equals("Azusa, Lost but Seeking"))
			  count = count + 2;
		  else if (var.getName().equals("Fastbond"))
			  count = 100;
	  }
	  
	  return count;
  }
  
  public static CardList getFastbonds(String player)
  {  
	  CardList list = new CardList(AllZone.getZone(Constant.Zone.Play, player).getCards());
	  list = list.getName("Fastbond");
	  return list;
  }
  
  //do card1 and card2 share any colors?
  public static boolean sharesColorWith(Card card1, Card card2)
  {
	  ArrayList<String> card1Colors = CardUtil.getColors(card1);
	  ArrayList<String> card2Colors = CardUtil.getColors(card2);
	  
	  for (String color : card1Colors)
	  {
		  if (card2Colors.contains(color))
			  return true;
	  }
	  
	  return false;
  }
  
  //may return null
  static public Card getRandomCard(CardList list)
  {
    if(list.size() == 0)
      return null;

    int index = random.nextInt(list.size());
    return list.get(index);
  }
  //may return null
  static public Card getRandomCard(PlayerZone zone)
  {
    return getRandomCard(new CardList(zone.getCards()));
  }
  
  public static void main(String[] args)
  {
	 
	 CardList in = AllZone.CardFactory.getAllCards();
	  
     CardList list = new CardList();
     list.addAll(CardListUtil.getColor(in, "black").toArray());
     list = list.getType("Creature");
     
     System.out.println("Most prominent creature type: " + getMostProminentCreatureType(list));
  }
  
}