package forge;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;
import java.util.Vector;

import javax.swing.JOptionPane;

import forge.error.ErrorViewer;
import forge.properties.ForgeProps;
import forge.properties.NewConstants;


public class CardFactory implements NewConstants {
    // String cardname is the key, Card is the value
    private Map<String, Card> map       = new HashMap<String, Card>();
    @SuppressWarnings("unused")
    // random
    private Random            random    = new Random();
    private CardList          allCards  = new CardList();
    
    private HashSet<String>   removedCardList;
    private Card              blankCard = new Card();                 //new code
                                                                       
    public CardFactory(String filename) {
        this(new File(filename));
    }
    
    public CardFactory(File file) {
        SpellAbility spell = new SpellAbility(SpellAbility.Spell, blankCard) {
            //neither computer nor human play can play this card
            @Override
            public boolean canPlay() {
                return false;
            }
            
            @Override
            public void resolve() {}
        };
        blankCard.addSpellAbility(spell);
        spell.setManaCost("1");
        blankCard.setName("Removed Card");
        
        //owner and controller will be wrong sometimes
        //but I don't think it will matter
        //theoretically blankCard will go to the wrong graveyard
        blankCard.setOwner(Constant.Player.Human);
        blankCard.setController(Constant.Player.Human);
        
        removedCardList = new HashSet<String>(FileUtil.readFile(ForgeProps.getFile(REMOVED)));
        

        try {
            readCards(file);
            
            // initialize CardList allCards
            Iterator<String> it = map.keySet().iterator();
            Card c;
            while(it.hasNext()) {
                c = getCard(it.next().toString(), Constant.Player.Human);
                allCards.add(c);
                //System.out.println("cardName: " + c.getName());
                
            }
        } catch(Exception ex) {
            ErrorViewer.showError(ex);
        }
    }// constructor
    
    public CardList getAllCards() {
        return new CardList(allCards.toArray());
    }// getAllCards()
    
    private void readCards(File file) {
        map.clear();
        
        ReadCard read = new ReadCard(ForgeProps.getFile(CARDS));
        try {
            read.run();
            // javax.swing.SwingUtilities.invokeAndWait(read);
        } catch(Exception ex) {
            ErrorViewer.showError(ex);
            throw new RuntimeException("CardFactory : readCards() thread error - " + ex.getMessage());
        }
        
        ArrayList<Card> simpleList = read.getCards();
        Card s;
        Iterator<Card> it = simpleList.iterator();
        while(it.hasNext()) {
            s = it.next();
            map.put(s.getName(), s);
            //System.out.println("cardName: " + s.getName());
        }
    }// readCard()
    
    final public Card copyCard(Card in) {
        Card out = getCard(in.getName(), in.getOwner());
        out.setUniqueNumber(in.getUniqueNumber());
        return out;
    }
    
    /*
    final public Card getCard(String cardName, String owner) {
        cardName = AllZone.NameChanger.getOriginalName(cardName);
        return getCard2(cardName, owner);
    }
    */

    //!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
    //this is the new getCard() method, you have to remove the old getCard()
    final public Card getCard(String cardName, String owner) {
        if(removedCardList.contains(cardName) || cardName.equals(blankCard.getName())) return blankCard;
        
        return getCard2(cardName, owner);
    }
    
    private final int hasKeyword(Card c, String k)
    {
    	ArrayList<String> a = c.getKeyword();
    	for (int i = 0; i < a.size(); i++)
    		if (a.get(i).toString().startsWith(k))
    			return i;

    	return -1;
    }
    
    private final int shouldManaAbility(Card c){
        ArrayList<String> a = c.getIntrinsicKeyword();
        for (int i = 0; i < a.size(); i++)
           if (a.get(i).toString().contains(": add "))
              return i;
       
        return -1;
    }
	
	
	final private Card getCard2(final String cardName, final String owner)
  {
    //o should be Card object
    Object o = map.get(cardName);
    if(o == null)
      throw new RuntimeException("CardFactory : getCard() invalid card name - " +cardName);

    final Card card = copyStats(o);
    card.setOwner(owner);
    card.setController(owner);
    //may have to change the spell
    //this is so permanents like creatures and artifacts have a "default" spell
    if(! card.isLand())
      card.addSpellAbility(new Spell_Permanent(card));
    
  //look for "Comes into play tapped." in the "no text" line
    //of card.txt and add the appropriate code to make that happen
    if(card.getKeyword().contains("Comes into play tapped."))
    {
      card.addComesIntoPlayCommand(new Command()
      {
		private static final long serialVersionUID = 203335252453049234L;

		public void execute()
        {
          card.tap();
        }
      });
    }//if "Comes into play tapped."

  //Creatures with simple, self-targeted mana-activated keyword adding abilities
    //-1 means not found
    while(hasKeyword(card, "KPump") != -1)
    {
      int n = hasKeyword(card, "KPump");
      if(n != -1)
      {
        String parse = card.getKeyword().get(n).toString();
        card.removeIntrinsicKeyword(parse);

        String k[] = parse.split(":");

        final String manaCost = k[0].substring(6);
        final String keyword = k[1];

        final Command untilEOT = new Command()
        {
			private static final long serialVersionUID = -2904643243256352517L;

		public void execute()
          {
            if(AllZone.GameAction.isCardInPlay(card))
            {
              card.removeIntrinsicKeyword(keyword);
            }
          }
        };

        SpellAbility ability = new Ability_Activated(card, manaCost)
        {
			private static final long serialVersionUID = -630655617339584299L;
			
			public boolean canPlayAI()
	        {
	             //in case if a lifelink pump, allow multiple pumps
	             if (CardFactoryUtil.AI_doesCreatureAttack(card) &&
	                !card.getKeyword().contains(keyword))
	             {
	                Random r = new Random();
	                  if (r.nextFloat() <= Math.pow(.6667, card.getAbilityUsed()))
	                     return true;
	                  else
	                     return false;
	             }
	             else
	                return false;
	        }
          public boolean canPlay()
          {
        	  if (CardFactoryUtil.canUseAbility(card) && AllZone.GameAction.isCardInPlay(card))
          		return true;
          	else 
          		return false;
          }
          public void resolve()
          {
            if(AllZone.GameAction.isCardInPlay(card))
            {
              card.addIntrinsicKeyword(keyword);
             
              card.setAbilityUsed(card.getAbilityUsed()+1);

              AllZone.EndOfTurn.addUntil(untilEOT);
            }
          }
        };

        String Desc = new String();
        Desc = cardName + " gains " + keyword + " until end of turn.";

        ability.setDescription(manaCost + ": " + Desc);
        ability.setStackDescription(Desc);

        card.addSpellAbility(ability);
      }//if (should pump card)
    }//while - card has more pump keywords - Blistering Dieflyn has two pump keywords


      //Creatures with simple, self-targeted mana-activated power and/or toughness
      //pumping abilities
      //is the card  "self pumper" like Furnace Whelp - this card gets +1/+1 until end of turn?
      //-1 means not found
      while (hasKeyword(card, "PTPump") != -1)
      {
        int n = hasKeyword(card, "PTPump");
        if(n != -1)
        {
          String parse = card.getKeyword().get(n).toString();
          card.removeIntrinsicKeyword(parse);

          final int attack[] = new int[1];
          final int defense[] = new int[1];

          String k[] = parse.split(":");
          String pt[] = k[1].split("/");

          final String manaCost = k[0].substring(7);
          
          
          Integer na = card.getNetAttack();
          Integer nd = card.getNetDefense();

          pt[0].replace("p", na.toString());
          pt[0].replace("t", nd.toString());
         
          pt[1].replace("p", na.toString());
          pt[1].replace("t", nd.toString());
          
          attack[0] = Integer.parseInt(pt[0].replace("+", ""));
          defense[0] = Integer.parseInt(pt[1].replace("+", ""));


          final Command untilEOT = new Command()
          {
			private static final long serialVersionUID = -4518618729662470596L;
            public void execute()
            {
              if(AllZone.GameAction.isCardInPlay(card))
              {
                card.addTempAttackBoost(-attack[0]);
                card.addTempDefenseBoost(-defense[0]);
              }
            }
          };

          SpellAbility ability = new Ability_Activated(card, manaCost)
          {
			private static final long serialVersionUID = -5718931378326228534L;
			
			public boolean canPlayAI()
            {
                if (card.getNetDefense() + defense[0] < 1) // no point if it would kill the creature outright
                    return false;
                return CardFactoryUtil.AI_doesCreatureAttack(card);
            }
            public boolean canPlay()
            {
            	if (CardFactoryUtil.canUseAbility(card) && AllZone.GameAction.isCardInPlay(card) && 
            		!card.isFaceDown())
            		return true;
            	else 
            		return false;
            }
            public void resolve()
            {
              if(AllZone.GameAction.isCardInPlay(card))
              {
                card.addTempAttackBoost(attack[0]);
                card.addTempDefenseBoost(defense[0]);
               
                card.setAbilityUsed(card.getAbilityUsed()+1);

                AllZone.EndOfTurn.addUntil(untilEOT);
              }
            }
          };
          
          String Desc = new String();
          Desc = cardName + " gets ";
          if (attack[0] > 0)
              Desc = Desc + "+" + attack[0];
          else
              Desc = Desc + attack[0];
          Desc = Desc + "/";
          if (defense[0] > 0)
              Desc = Desc + "+" + defense[0];
          else
              Desc = Desc + defense[0];
          Desc = Desc + " until end of turn.";

          ability.setDescription(manaCost + ": " + Desc);
          ability.setStackDescription(Desc);

          card.addSpellAbility(ability);
        }//if (should pump card)
      }//while - card has more pump keywords - Blistering Dieflyn has two pump keywords

      //Creatures with simple, self-targeted mana-activated power and/or toughness
      //pumping plus keyword adding abilities
      //is the card  "self pumper" like Furnance Whelp - this card gets +1/+1 until end of turn?
      //-1 means not found
      while(hasKeyword(card, "PTKPump") != -1)
      {
        int n = hasKeyword(card, "PTKPump");
        if(n != -1)
        {
          String parse = card.getKeyword().get(n).toString();
          card.removeIntrinsicKeyword(parse);
   
          final int attack[] = new int[1];
          final int defense[] = new int[1];

          String k[] = parse.split(":");
          String ptk[] = k[1].split("/");

          final String manaCost = k[0].substring(8);
                   	
          attack[0] = Integer.parseInt(ptk[0].replace("+", ""));
          defense[0] = Integer.parseInt(ptk[1].replace("+", ""));
          final String keyword = ptk[2];
          
  
          
          final Command untilEOT = new Command()
          {
			private static final long serialVersionUID = -3554196069508466753L;

			public void execute()
            {
              if(AllZone.GameAction.isCardInPlay(card))
              {
                card.removeIntrinsicKeyword(keyword);
                card.addTempAttackBoost(-attack[0]);
                card.addTempDefenseBoost(-defense[0]);

              }
            }
          };
   
          SpellAbility ability = new Ability_Activated(card, manaCost)
          {
			private static final long serialVersionUID = 4196412133232530875L;
			
			public boolean canPlayAI()
              {
                  if (card.getNetDefense() + defense[0] < 1) // no point if it would kill the creature outright
                      return false;
                 
                  if (card.getKeyword().contains(keyword))
                     return false;
                 
                  return CardFactoryUtil.AI_doesCreatureAttack(card);
              }
            public boolean canPlay()
            {
            	if (CardFactoryUtil.canUseAbility(card) && AllZone.GameAction.isCardInPlay(card) &&
            		!card.isFaceDown())
            		return true;
            	else 
            		return false;
            }
            public void resolve()
            {
              if(AllZone.GameAction.isCardInPlay(card))
              {
                card.addIntrinsicKeyword(keyword);
                card.addTempAttackBoost(attack[0]);
                card.addTempDefenseBoost(defense[0]);
                
                card.setAbilityUsed(card.getAbilityUsed()+1);

                AllZone.EndOfTurn.addUntil(untilEOT);
              }
            }
          };
          
   
          String Desc = new String();
          Desc = cardName + " gets ";
          if (attack[0] > 0)
              Desc = Desc + "+" + attack[0];
          else
              Desc = Desc + attack[0];
          Desc = Desc + "/";
          if (defense[0] > 0)
              Desc = Desc + "+" + defense[0];
          else
              Desc = Desc + defense[0];
          Desc = Desc + " and gains " + keyword + " until end of turn.";
   
          ability.setDescription(manaCost + ": " + Desc);
          ability.setStackDescription(Desc);
   
          card.addSpellAbility(ability);
        }//if (should pump card)
      }//while - card has more pump keywords - Blistering Dieflyn has two pump keywords
      
    //Creatures with simple, targeted keyword adding abilities
      // costs may include "T" to indicate a tap cost
      //-1 means not found
      while(hasKeyword(card, "TgtKPump") != -1)
      {
        int n = hasKeyword(card, "TgtKPump");
        if(n != -1)
        {
          String parse = card.getKeyword().get(n).toString();
          card.removeIntrinsicKeyword(parse);

          String k[] = parse.split(":");

          String tmpCost = k[0].substring(9);
          final String keyword = k[1];
         
          boolean tapCost = false;
          boolean tapOnlyCost = false;
         
          if (tmpCost.contains("T"))
          {
             tapCost = true;
             tmpCost = tmpCost.replace("T", "");
             tmpCost = tmpCost.trim();
             if (tmpCost.length() == 0)
                tapOnlyCost = true;
          }
         
          final String manaCost = tmpCost;
         
          String tempDesc = new String();
          tempDesc = "Target creature gains " + keyword + " until end of turn.";
          final String Desc = tempDesc;
         
          if (! tapCost)
          {
              final SpellAbility ability = new Ability_Activated(card, manaCost)
              {

            private static final long serialVersionUID = -1118592153328758083L;
            
            public boolean canPlayAI()
                {
                  //if(CardFactoryUtil.AI_doesCreatureAttack(card))
                  //  return false;
              CardList list = getCreature();
              if (list.isEmpty())
                 return false;
              else
              {
                 Random r = new Random();
                 if (r.nextFloat() <= Math.pow(.6667, card.getAbilityUsed()))
                    return true;
                 else
                    return false;
              }
                }
            
            
                public boolean canPlay()
                {
               
                   if (CardFactoryUtil.canUseAbility(card) && AllZone.GameAction.isCardInPlay(card)&&
                      !card.isFaceDown())
                      return true;
                   else
                      return false;
                }
                public void chooseTargetAI()
                {
                  Card target = CardFactoryUtil.AI_getBestCreature(getCreature());
                  setTargetCard(target);
                }
                CardList getCreature()
                {
                  CardList list = new CardList(AllZone.Computer_Play.getCards());
                  list = list.filter(new CardListFilter()
                  {
                    public boolean addCard(Card c)
                    {
                      return c.isCreature() &&
                            (!CardFactoryUtil.AI_doesCreatureAttack(c)) && CardFactoryUtil.canTarget(card, c) &&
                            (! c.getKeyword().contains(keyword)) &&
                            (! c.getKeyword().contains("Defender")) &&
                            (! c.hasSickness() && keyword.equals("Haste"));
                    }
                  });
                  // list.remove(card);      // if mana-only cost, allow self-target
                  return list;
                }//getCreature()
                public void resolve()
                {
                  if(AllZone.GameAction.isCardInPlay(getTargetCard()) && CardFactoryUtil.canTarget(card, getTargetCard()) )
                  {
                    final Card[] creature = new Card[1];
                    final Command EOT = new Command()
                    {
                  private static final long serialVersionUID = -8840812331316327448L;

               public void execute()
                      {
                        if(AllZone.GameAction.isCardInPlay(creature[0]))
                          creature[0].removeExtrinsicKeyword(keyword);
                      }
                    };
                    creature[0] = getTargetCard();
                    creature[0].addExtrinsicKeyword(keyword);
                    card.setAbilityUsed(card.getAbilityUsed()+1);
                    AllZone.EndOfTurn.addUntil(EOT);
                  }//if (card is in play)
                }//resolve()
              };//SpellAbility
             
              ability.setDescription(manaCost + ": " + Desc);
              ability.setStackDescription(Desc);

              ability.setBeforePayMana(CardFactoryUtil.input_targetCreature(ability));
              card.addSpellAbility(ability);
          }
          if (tapOnlyCost)
          {
              final SpellAbility ability = new Ability_Tap(card)
              {
            private static final long serialVersionUID = 5252594757468128739L;
            
            public boolean canPlayAI()
                {
                  if(CardFactoryUtil.AI_doesCreatureAttack(card))
                    return false;
                 
                  return getCreature().size() != 0;
                }
                public boolean canPlay()
                {
                   boolean sick = true;
                   
                   if (!card.hasSickness() || !card.isCreature())
                       sick = false;
                   
                   if (card.isUntapped() && CardFactoryUtil.canUseAbility(card) && AllZone.GameAction.isCardInPlay(card)
                       && !sick && !card.isFaceDown())
                      return true;
                   else
                      return false;
                }
                public void chooseTargetAI()
                {
                   card.tap();
                   Card target = CardFactoryUtil.AI_getBestCreature(getCreature());
                   setTargetCard(target);
                }
                CardList getCreature()
                {
                  CardList list = new CardList(AllZone.Computer_Play.getCards());
                  list = list.filter(new CardListFilter()
                  {
                    public boolean addCard(Card c)
                    {
                      return c.isCreature() &&
                            (!CardFactoryUtil.AI_doesCreatureAttack(c)) &&
                            (! c.getKeyword().contains(keyword)) &&
                            (! c.getKeyword().contains("Defender")) &&
                            (! c.hasSickness() && keyword.equals("Haste"));
                    }
                  });
                  list.remove(card);
                  return list;
                }//getCreature()
                public void resolve()
                {
                  if(AllZone.GameAction.isCardInPlay(getTargetCard()) && CardFactoryUtil.canTarget(card, getTargetCard()))
                  {
                    final Card[] creature = new Card[1];
                    final Command EOT = new Command()
                    {
                  private static final long serialVersionUID = 2134353417588894452L;

               public void execute()
                      {
                        if(AllZone.GameAction.isCardInPlay(creature[0]))
                          creature[0].removeExtrinsicKeyword(keyword);
                      }
                    };
                    creature[0] = getTargetCard();
                    creature[0].addExtrinsicKeyword(keyword);
                    AllZone.EndOfTurn.addUntil(EOT);
                  }//if (card is in play)
                }//resolve()
              };//SpellAbility
             
              ability.setDescription("tap: " + Desc);
              ability.setStackDescription(Desc);

              ability.setBeforePayMana(CardFactoryUtil.input_targetCreature(ability));
              card.addSpellAbility(ability);
          }
          if (! tapOnlyCost && tapCost)
          {
              final SpellAbility ability = new Ability_Tap(card, manaCost)
              {
            private static final long serialVersionUID = 7593387152288440603L;
            
            public boolean canPlayAI()
                {
                  if(CardFactoryUtil.AI_doesCreatureAttack(card))
                    return false;
                 
                  return getCreature().size() != 0;
                }
                public boolean canPlay()
                {
                   boolean sick = true;
                   
                   if (!card.hasSickness() || !card.isCreature())
                      sick = false;
                   
                   if (card.isUntapped() && CardFactoryUtil.canUseAbility(card) && AllZone.GameAction.isCardInPlay(card) &&
                     !sick && !card.isFaceDown())
                      return true;
                   else
                      return false;
                }
                public void chooseTargetAI()
                {
                   card.tap();
                   Card target = CardFactoryUtil.AI_getBestCreature(getCreature());
                   setTargetCard(target);
                }
                CardList getCreature()
                {
                  CardList list = new CardList(AllZone.Computer_Play.getCards());
                  list = list.filter(new CardListFilter()
                  {
                    public boolean addCard(Card c)
                    {
                      return c.isCreature() &&
                            (!CardFactoryUtil.AI_doesCreatureAttack(c)) &&
                            (! c.getKeyword().contains(keyword)) &&
                            (! c.getKeyword().contains("Defender")) &&
                            (! c.hasSickness() && keyword.equals("Haste"));
                    }
                  });
                  list.remove(card);
                  return list;
                }//getCreature()
                public void resolve()
                {
                  if(AllZone.GameAction.isCardInPlay(getTargetCard()) && CardFactoryUtil.canTarget(card ,getTargetCard()))
                  {
                    final Card[] creature = new Card[1];
                    final Command EOT = new Command()
                    {
                  private static final long serialVersionUID = 3532917180149273560L;

               public void execute()
                      {
                        if(AllZone.GameAction.isCardInPlay(creature[0]))
                          creature[0].removeExtrinsicKeyword(keyword);
                      }
                    };
                    creature[0] = getTargetCard();
                    creature[0].addExtrinsicKeyword(keyword);
                    AllZone.EndOfTurn.addUntil(EOT);
                  }//if (card is in play)
                }//resolve()
              };//SpellAbility
             
              ability.setDescription(manaCost + ", tap: " + Desc);
              ability.setStackDescription(Desc);

              ability.setBeforePayMana(CardFactoryUtil.input_targetCreature(ability));
              card.addSpellAbility(ability);
          }
        }
      }//while

    
    //Creatures with self-regenerate abilities
    //-1 means keyword "RegenerateMe" not found
    while (hasKeyword(card, "RegenerateMe") != -1)
    {
      int n = hasKeyword(card, "RegenerateMe");
      if (n != -1)
      {
        String parse = card.getKeyword().get(n).toString();
        card.removeIntrinsicKeyword(parse);

        String k[] = parse.split(":");
        final String manacost = k[1];

        final Command untilEOT = new Command() {
          private static final long serialVersionUID = -7619842476705984912L;

          public void execute()
          {
            card.setShield(0);
            
          }
        };

        final SpellAbility a1 = new Ability(card, manacost) {
          public boolean canPlayAI()
          {
            if (CardFactoryUtil.AI_isMainPhase())
            {
              if (CardFactoryUtil.AI_doesCreatureAttack(card))
              {
                //"Fuzzy logic" to determine if using a regenerate ability might be helpful because
                //we can't wait to decide to play this ability during combat, like the human can
                //weight[] is a set of probability percentages to be averaged later
                int weight[] = new int[3];

                // cards with real keywords (flying, trample, etc) are probably more desireable
                if (card.getKeyword().size() > 0)
                  weight[0] = 75;
                else
                  weight[0] = 0;

                // if there are many cards in hand, then maybe it's not such a great idea to waste mana
                CardList HandList = new CardList(AllZone.getZone(Constant.Zone.Hand, Constant.Player.Computer).getCards());

                if (HandList.size() >= 4)
                  weight[1] = 25;
                else
                  weight[1] = 75;

                // compare the highest converted mana cost of cards in hand to the number of lands
                // if there's spare mana, then regeneration might be viable
                int hCMC = 0;
                for (int i = 0; i < HandList.size(); i++)
                  if (CardUtil.getConvertedManaCost(HandList.getCard(i).getManaCost()) > hCMC)
                    hCMC = CardUtil.getConvertedManaCost(HandList.getCard(i).getManaCost());

                CardList LandList = new CardList(AllZone.getZone(Constant.Zone.Play, Constant.Player.Computer).getCards());
                LandList = LandList.getType("Land");

                //most regenerate abilities cost 2 or less
                if (hCMC + 2 >= LandList.size())
                  weight[2] = 50;
                else
                  weight[2] = 0;

                // ultimately, it's random fate that dictates if this was the right play
                int aw = (weight[0] + weight[1] + weight[2]) / 3;
                Random r = new Random();
                if (r.nextInt(100) <= aw)
                  return true;
              }
            }
            return false;
          }

          public void resolve()
          {
            card.addShield();
            AllZone.EndOfTurn.addUntil(untilEOT);
          }
        }; //SpellAbility

        card.addSpellAbility(a1);

        String Desc = new String();
        Desc = "Regenerate " + cardName;

        a1.setDescription(manacost + ": " + Desc);
        a1.setStackDescription(Desc);

        a1.setBeforePayMana(new Input_PayManaCost(a1));
      } //if (should RegenerateMe)
    } //while - card has more RegenerateMe - Jungle Troll has two Regenerate keywords
    
    while(hasKeyword(card, "abPump") != -1)
    {
      int n = hasKeyword(card, "abPump");
      if(n != -1)
      {
        String parse = card.getKeyword().get(n).toString();
        card.removeIntrinsicKeyword(parse);

        String k[] = parse.split(":");
       
        final boolean Tgt[] = {false};
        Tgt[0] = k[0].contains("Tgt");

        String tmpCost;
        if (Tgt[0])
           tmpCost = k[0].substring(9);
        else
           tmpCost = k[0].substring(6);
       
        boolean tapCost = false;
        boolean tapOnlyCost = false;
       
        if (tmpCost.contains("T"))
        {
           tapCost = true;
           tmpCost = tmpCost.replace("T", "");
           tmpCost = tmpCost.trim();
           if (tmpCost.length() == 0)
              tapOnlyCost = true;
        }
        final String manaCost = tmpCost.trim();
       
        final int NumAttack[] = {-1138};
        final String AttackX[] = {"none"};
        final int NumDefense[] = {-1138};
        final String DefenseX[] = {"none"};
        final String Keyword[] = {"none"};

        String ptk[] = k[1].split("/");            
       
        if (ptk.length == 1)
           Keyword[0] = ptk[0];
       
        if (ptk.length >= 2)
        {
           if (ptk[0].length() <= 3)
              NumAttack[0] = Integer.parseInt(ptk[0].replace("+", ""));
           else
              if (ptk[0].startsWith("Count$"))
              {
                 String kk[] = ptk[0].split("\\$");
                 AttackX[0] = kk[1].replace("\\", "/");
              }
              
           if (ptk[1].length() <= 3)
              NumDefense[0] = Integer.parseInt(ptk[1].replace("+", ""));
           else
              if (ptk[1].startsWith("Count$"))
              {
                 String kk[] = ptk[1].split("\\$");
                 DefenseX[0] = kk[1].replace("\\", "/");
              }
        }
       
        if (ptk.length == 3)
           Keyword[0] = ptk[2];
       
        final String DrawBack[] = {"none"};
        final String spDesc[] = {"none"};
        final String stDesc[] = {"none"};
        String d = new String("none");
       
        if ((AttackX[0].equals("none") && !(NumAttack[0] == -1138)) && (DefenseX[0].equals("none") && !(NumDefense[0] == -1138)) && Keyword[0].equals("none"))
        {
           // pt boost
           if (Tgt[0] == true)
              d = "Target creature gets ";
           else
              d = cardName + " gets ";
          
           if (NumAttack[0] > 0 || (NumAttack[0] == 0 && NumDefense[0] > 0)) // +0/+1
              d = d + "+";
           else if (NumAttack[0] < 0 || (NumAttack[0] == 0 && NumDefense[0] < 0)) // -0/-1
              d = d + "-";
                       
           d = d + Math.abs(NumAttack[0]) + "/";
          
           if (NumDefense[0] > 0 || (NumDefense[0] == 0 && NumAttack[0] > 0)) // +1/+0
              d = d + "+";
           else if (NumDefense[0] < 0 || (NumDefense[0] == 0 && NumAttack[0] < 0)) // -1/-0
              d = d + "-";
          
           d = d + Math.abs(NumDefense[0]) + " until end of turn.";
        }
        if ((AttackX[0].equals("none") && NumAttack[0] == -1138) && (DefenseX[0].equals("none") && NumDefense[0] == -1138) && !Keyword[0].equals("none"))
        {
           // k boost
           if (Tgt[0] == true)
              d = "Target creature gains ";
           else
              d = cardName + " gains ";
          
           d = d + Keyword[0] + " until end of turn.";
        }
        if ((AttackX[0].equals("none") && !(NumAttack[0] == -1138)) && (DefenseX[0].equals("none") && !(NumDefense[0] == -1138)) && !Keyword[0].equals("none"))
        {
           // ptk boost
           if (Tgt[0] == true)
              d = "Target creature gets ";
           else
              d = cardName + " gets ";
          
           if (NumAttack[0] > 0 || (NumAttack[0] == 0 && NumDefense[0] > 0)) // +0/+1
              d = d + "+";
           else if (NumAttack[0] < 0 || (NumAttack[0] == 0 && NumDefense[0] < 0)) // -0/-1
              d = d + "-";
          
           d = d + Math.abs(NumAttack[0]) + "/";
          
           if (NumDefense[0] > 0 || (NumDefense[0] == 0 && NumAttack[0] > 0)) // +1/+0
              d = d + "+";
           else if (NumDefense[0] < 0 || (NumDefense[0] == 0 && NumAttack[0] < 0)) // -1/-0
              d = d + "-";
          
           d = d + Math.abs(NumDefense[0]);
          
           d = d + " and gains " + Keyword[0] + " until end of turn.";
        }
       
        if (k.length > 2)
        {
           if (k[2].contains("Drawback$"))
           {
              String kk[] = k[2].split("\\$");
              DrawBack[0] = kk[1];
              if (k.length > 3)
                 d = k[3];
           }
           else
              if (k.length > 2)
                 d = k[2];
        }
       
        if (!d.equals("none"))
        {
           if (tapOnlyCost == true)
              spDesc[0] = "Tap: " + d;
           else if (tapCost == true)
              spDesc[0] = manaCost + ", tap: " + d;
           else
              spDesc[0] = manaCost + ": " + d;

           stDesc[0] = d;
        }

        if (! tapCost)
        {
           final SpellAbility ability = new Ability_Activated(card, manaCost)
           {
              private static final long serialVersionUID = -1118592153328758083L;
             
              private int defense;
              private String keyword;
             
              private int getNumAttack()
              {
                 if (NumAttack[0] != -1138)
                    return NumAttack[0];
                
                 if (! AttackX[0].equals("none"))
                    return CardFactoryUtil.xCount(card, AttackX[0]);
                
                 return 0;
              }
              private int getNumDefense()
              {
                 if (NumDefense[0] != -1138)
                    return NumDefense[0];
                
                 if (! DefenseX[0].equals("none"))
                    return CardFactoryUtil.xCount(card, DefenseX[0]);
                
                 return 0;
              }
             
              public boolean canPlayAI()
              {
                 defense = getNumDefense();
                 keyword = Keyword[0];
                
                 if (AllZone.Phase.getPhase().equals(Constant.Phase.Main2))
                    return false;
                
                 if (Tgt[0] == false)
                 {
                    setTargetCard(card);

                    if ((card.getNetDefense() + defense > 0) &&
                          (! card.getKeyword().contains(keyword)))
                       if (card.hasSickness() && keyword.equals("Haste"))
                          return true;
                       else if ((card.hasSickness() && (! keyword.equals("Haste"))) ||
                             ((! card.hasSickness()) && keyword.equals("Haste")))
                          return false;
                       else
                       {
                           Random r = new Random();
                           if (r.nextFloat() <= Math.pow(.6667, card.getAbilityUsed()))
                              return CardFactoryUtil.AI_doesCreatureAttack(card);
                       }
                 }

                 CardList list = getCreatures();
                 if (!list.isEmpty())
                 {
                    boolean goodt = false;
                    Card t = new Card();
                    while (goodt == false && !list.isEmpty())      // loop until we find a target that is best and won't die when targeted or until no more creatures
                    {
                       t = CardFactoryUtil.AI_getBestCreature(list);
                       if ((t.getNetDefense() + defense) > 0)   // handle negative defense pumps
                          goodt = true;
                       else
                          list.remove(t);
                    }
                    if (goodt == true)
                    {
                       Random r = new Random();
                       if (r.nextFloat() <= Math.pow(.6667, card.getAbilityUsed()))
                       {
                          setTargetCard(t);
                          return true;
                       }
                    }
                 }
                
                 return false;
              }

              public boolean canPlay()
              {
                 return (CardFactoryUtil.canUseAbility(card)) &&
                 (AllZone.GameAction.isCardInPlay(card)) &&
                 (! card.isFaceDown());
              }

              private CardList getCreatures()
              {
                 CardList list = new CardList(AllZone.Computer_Play.getCards());
                 list = list.filter(new CardListFilter()
                 {
                    public boolean addCard(Card c)
                    {
                       if (c.isCreature())
                       {
                          if (c.hasSickness() && keyword.equals("Haste")) // AI_doesCreatureAttack would have prevented the effect from granting haste, because it assumes the creature would already have it
                             return CardFactoryUtil.canTarget(card, c);

                          return (CardFactoryUtil.AI_doesCreatureAttack(c)) &&
                          (CardFactoryUtil.canTarget(card, c)) &&
                          (!keyword.equals("none") && !c.getKeyword().contains(keyword)) &&
                          (! (! c.hasSickness()) && keyword.equals("Haste")); // if creature doesn't have sickness, the haste keyword won't help
                       }
                       return false;
                    }
                 });
                 // list.remove(card);      // if mana-only cost, allow self-target
                 return list;
              }//getCreatures()
              public void resolve()
              {
                 if(AllZone.GameAction.isCardInPlay(getTargetCard()) && CardFactoryUtil.canTarget(card, getTargetCard()) )
                 {
                    final Card[] creature = new Card[1];
                    if (Tgt[0] == true)
                       creature[0] = getTargetCard();
                    else
                       creature[0] = card;
                   
                     final int a = getNumAttack();
                     final int d = getNumDefense();
                   
                    final Command EOT = new Command()
                    {
                       private static final long serialVersionUID = -8840812331316327448L;

                       public void execute()
                       {
                          if(AllZone.GameAction.isCardInPlay(creature[0]))
                          {
                             creature[0].addTempAttackBoost(-1 * a);
                             creature[0].addTempDefenseBoost(-1 * d);
                             if (! Keyword[0].equals("none"))
                                creature[0].removeExtrinsicKeyword(Keyword[0]);
                          }

                       }
                    };

                    creature[0].addTempAttackBoost(a);
                    creature[0].addTempDefenseBoost(d);
                    if (! Keyword[0].equals("none"))
                       creature[0].addExtrinsicKeyword(Keyword[0]);

                    card.setAbilityUsed(card.getAbilityUsed()+1);
                    AllZone.EndOfTurn.addUntil(EOT);
                   
                       if (! DrawBack[0].equals("none"))
                           CardFactoryUtil.doDrawBack(DrawBack[0], 0, card.getController(), AllZone.GameAction.getOpponent(card.getController()), null, card, creature[0]);

                 }//if (card is in play)
              }//resolve()
           };//SpellAbility

           ability.setDescription(spDesc[0]);
           ability.setStackDescription(stDesc[0]);

           if (Tgt[0] == true)
              ability.setBeforePayMana(CardFactoryUtil.input_targetCreature(ability));
           else
              ability.setTargetCard(card);
           card.addSpellAbility(ability);
        }
        if (tapOnlyCost)
        {
           final SpellAbility ability = new Ability_Tap(card)
           {
              private static final long serialVersionUID = 5252594757468128739L;

              private int defense;
              private String keyword;
             
              private int getNumAttack()
              {
                 if (NumAttack[0] != -1138)
                    return NumAttack[0];
                
                 if (! AttackX[0].equals("none"))
                    return CardFactoryUtil.xCount(card, AttackX[0]);
                
                 return 0;
              }
              private int getNumDefense()
              {
                 if (NumDefense[0] != -1138)
                    return NumDefense[0];
                
                 if (! DefenseX[0].equals("none"))
                    return CardFactoryUtil.xCount(card, DefenseX[0]);
                
                 return 0;
              }               
             
              public boolean canPlayAI()
              {
                 defense = getNumDefense();
                 keyword = Keyword[0];
                
                 if(CardFactoryUtil.AI_doesCreatureAttack(card))
                    return false;

                 if (AllZone.Phase.getPhase().equals(Constant.Phase.Main2))
                    return false;

                 CardList list = getCreatures();
                 if (!list.isEmpty())
                 {
                    boolean goodt = false;
                    Card t = new Card();
                    while (goodt == false && !list.isEmpty())
                    {
                       t = CardFactoryUtil.AI_getBestCreature(list);
                       if ((t.getNetDefense() + defense) > 0)
                          goodt = true;
                       else
                          list.remove(t);
                    }
                    if (goodt == true)
                    {
                       setTargetCard(t);
                       return true;
                    }
                 }

                 return false;
              }
              public boolean canPlay()
              {
                 boolean sick = true;

                 if (!card.hasSickness() || !card.isCreature())
                    sick = false;

                 if (card.isUntapped() && AllZone.GameAction.isCardInPlay(card)
                       && !sick && !card.isFaceDown())
                    return true;
                 else
                    return false;
              }

              CardList getCreatures()
              {
                 CardList list = new CardList(AllZone.Computer_Play.getCards());
                 list = list.filter(new CardListFilter()
                 {
                    public boolean addCard(Card c)
                    {
                       if (c.isCreature())
                       {
                          if (c.hasSickness() && keyword.equals("Haste"))
                             return CardFactoryUtil.canTarget(card, c);
                         
                          return (CardFactoryUtil.AI_doesCreatureAttack(c)) &&
                          (CardFactoryUtil.canTarget(card, c)) &&
                          (!keyword.equals("none") && !c.getKeyword().contains(keyword)) &&
                          (! (! c.hasSickness()) && keyword.equals("Haste"));
                       }
                       return false;
                    }
                 });
                 list.remove(card);
                 return list;
              }//getCreature()
             
              public void resolve()
              {
                 if(AllZone.GameAction.isCardInPlay(getTargetCard()) && CardFactoryUtil.canTarget(card, getTargetCard()))
                 {
                    final Card[] creature = new Card[1];
                    if (Tgt[0] == true)
                       creature[0] = getTargetCard();
                    else
                       creature[0] = card;
                   
                    final int a = getNumAttack();
                    final int d = getNumDefense();
                   
                    final Command EOT = new Command()
                    {
                       private static final long serialVersionUID = 2134353417588894452L;

                       public void execute()
                       {
                          if(AllZone.GameAction.isCardInPlay(creature[0]))
                          {
                             creature[0].addTempAttackBoost(-1 * a);
                             creature[0].addTempDefenseBoost(-1 * d);
                             if (! Keyword[0].equals("none"))
                                creature[0].removeExtrinsicKeyword(Keyword[0]);
                          }
                       }
                    };
                   
                    creature[0].addTempAttackBoost(a);
                    creature[0].addTempDefenseBoost(d);
                    if (! Keyword[0].equals("none"))
                       creature[0].addExtrinsicKeyword(Keyword[0]);
                   
                    AllZone.EndOfTurn.addUntil(EOT);

                    if (! DrawBack[0].equals("none"))
                           CardFactoryUtil.doDrawBack(DrawBack[0], 0, card.getController(), AllZone.GameAction.getOpponent(card.getController()), null, card, creature[0]);
                 }//if (card is in play)
              }//resolve()
           };//SpellAbility

           ability.setDescription(spDesc[0]);
           ability.setStackDescription(stDesc[0]);

           if (Tgt[0] == true)
              ability.setBeforePayMana(CardFactoryUtil.input_targetCreature(ability));
           else
              ability.setTargetCard(card);
           card.addSpellAbility(ability);
        }
        if (! tapOnlyCost && tapCost)
        {
           final SpellAbility ability = new Ability_Tap(card, manaCost)
           {
              private static final long serialVersionUID = 7593387152288440603L;
             
              private int defense;
              private String keyword;
             
              private int getNumAttack()
              {
                 if (NumAttack[0] != -1138)
                    return NumAttack[0];
                
                 if (! AttackX[0].equals("none"))
                    return CardFactoryUtil.xCount(card, AttackX[0]);
                
                 return 0;
              }
              private int getNumDefense()
              {
                 if (NumDefense[0] != -1138)
                    return NumDefense[0];
                
                 if (! DefenseX[0].equals("none"))
                    return CardFactoryUtil.xCount(card, DefenseX[0]);
                
                 return 0;
              }               

              public boolean canPlayAI()
              {
                 defense = getNumDefense();
                 keyword = Keyword[0];
                 if(CardFactoryUtil.AI_doesCreatureAttack(card))
                    return false;
                
                 if (AllZone.Phase.getPhase().equals(Constant.Phase.Main2))
                    return false;

                 CardList list = getCreatures();
                 if (!list.isEmpty())
                 {
                    boolean goodt = false;
                    Card t = new Card();
                    while (goodt == false && !list.isEmpty())
                    {
                       t = CardFactoryUtil.AI_getBestCreature(list);
                       if ((t.getNetDefense() + defense) > 0)
                          goodt = true;
                       else
                          list.remove(t);
                    }
                    if (goodt == true)
                    {
                       setTargetCard(t);
                       return true;
                    }
                 }
                
                 return false;
              }
              public boolean canPlay()
              {
                 boolean sick = true;

                 if (!card.hasSickness() || !card.isCreature())
                    sick = false;

                 if (card.isUntapped() && AllZone.GameAction.isCardInPlay(card) &&
                       !sick && !card.isFaceDown())
                    return true;
                 else
                    return false;
              }
              CardList getCreatures()
              {
                 CardList list = new CardList(AllZone.Computer_Play.getCards());
                 list = list.filter(new CardListFilter()
                 {
                    public boolean addCard(Card c)
                    {
                       if (c.hasSickness() && keyword.equals("Haste"))
                          return CardFactoryUtil.canTarget(card, c);
                      
                       return (CardFactoryUtil.AI_doesCreatureAttack(c)) &&
                       (CardFactoryUtil.canTarget(card, c)) &&
                       (!keyword.equals("none") && !c.getKeyword().contains(keyword)) &&
                       (! (! c.hasSickness()) && keyword.equals("Haste"));
                    }
                 });
                 list.remove(card);
                 return list;
              }//getCreature()
              public void resolve()
              {
                 if(AllZone.GameAction.isCardInPlay(getTargetCard()) && CardFactoryUtil.canTarget(card ,getTargetCard()))
                 {
                    final Card[] creature = new Card[1];
                    if (Tgt[0] == true)
                       creature[0] = getTargetCard();
                    else
                       creature[0] = card;
                   
                    final int a = getNumAttack();
                    final int d = getNumDefense();
                   
                    final Command EOT = new Command()
                    {
                       private static final long serialVersionUID = 3532917180149273560L;

                       public void execute()
                       {
                          if(AllZone.GameAction.isCardInPlay(creature[0]))
                          {
                             creature[0].addTempAttackBoost(-1 * a);
                             creature[0].addTempDefenseBoost(-1 * d);
                             if (! Keyword[0].equals("none"))
                                creature[0].removeExtrinsicKeyword(Keyword[0]);
                          }
                       }
                    };
                   
                    creature[0].addTempAttackBoost(a);
                    creature[0].addTempDefenseBoost(d);
                    if (! Keyword[0].equals("none"))
                       creature[0].addExtrinsicKeyword(Keyword[0]);
                    AllZone.EndOfTurn.addUntil(EOT);

                    if (! DrawBack[0].equals("none"))
                           CardFactoryUtil.doDrawBack(DrawBack[0], 0, card.getController(), AllZone.GameAction.getOpponent(card.getController()), null, card, creature[0]);
                 }//if (card is in play)
              }//resolve()
           };//SpellAbility
           
            ability.setDescription(spDesc[0]);
            ability.setStackDescription(stDesc[0]);

            if (Tgt[0] == true)
               ability.setBeforePayMana(CardFactoryUtil.input_targetCreature(ability));
            else
               ability.setTargetCard(card);
            card.addSpellAbility(ability);
        }
      }
    }//while


    if (hasKeyword(card, "Untap") != -1)
    {
    	int n = hasKeyword(card, "Untap");
    	if (n != -1)
    	{
    		String parse = card.getKeyword().get(n).toString();
            card.removeIntrinsicKeyword(parse);
            
            String k[] = parse.split(":");
            final String manacost = k[1];
            
            card.addSpellAbility(CardFactoryUtil.ability_Untap(card, manacost));            
    	}
    }
    
    if (hasKeyword(card, "Remove three spore counters") != -1)
    {
      int n = hasKeyword(card, "Remove three spore counters");
      if (n != -1)
      {
    	String parse = card.getKeyword().get(n).toString();
        card.removeIntrinsicKeyword(parse);
        
        card.addSpellAbility(CardFactoryUtil.ability_Spore_Saproling(card));
      }
    }//Spore Saproling
    
    if (hasKeyword(card, "spDamageTgt")  != -1)
    {
       int n = hasKeyword(card, "spDamageTgt");
       if (n != -1)
       {
          String parse = card.getKeyword().get(n).toString();
          card.removeIntrinsicKeyword(parse);
                   
          card.clearSpellAbility();
         
          String k[] = parse.split(":");
         
          final boolean TgtCreature[] = {false};
          final boolean TgtPlayer[] = {false};
          final boolean TgtCP[] = {false};
          final boolean TgtOpp[] = {false};
         
          if (k[0].contains("CP"))
             TgtCP[0] = true;
          else if (k[0].contains("P"))
             TgtPlayer[0] = true;
          else if (k[0].contains("C"))
             TgtCreature[0] = true;
          else if (k[0].contains("Opp"))
        	 TgtOpp[0] = true;
         
          // how much damage
        final int NumDmg[] = {-1};
          final String NumDmgX[] = {"none"};

          if (k[1].length() <= 2)      // numeric
             NumDmg[0] = Integer.parseInt(k[1]);
          else                     // result of some sort of function
          {
             if (k[1].startsWith("Count$"))
             {
                String kk[] = k[1].split("\\$");
                NumDmgX[0] = kk[1];
             }
          }
         
          //drawbacks and descriptions
          final String DrawBack[] = {"none"};
          final String spDesc[] = {"none"};
          final String stDesc[] = {"none"};
          if (k.length > 2)
          {
             if (k[2].contains("Drawback$"))
             {
                String kk[] = k[2].split("\\$");
                DrawBack[0] = kk[1];
                if (k.length > 3)
                   spDesc[0] = k[3];
                if (k.length > 4)
                   stDesc[0] = k[4];
             }
             else
             {
                if (k.length > 2)
                   spDesc[0] = k[2];
                if (k.length > 3)
                   stDesc[0] = k[3];
             }
          }
         
          final SpellAbility DamageTgt = new Spell(card)
          {
             private static final long serialVersionUID = 7239608350643325111L;
             private int damage;

             public int getNumDamage()
             {
                if (NumDmg[0] != -1)
                   return NumDmg[0];

                if (! NumDmgX[0].equals("none"))
                   return CardFactoryUtil.xCount(card, NumDmgX[0]);

                return 0;
             }
            
             boolean shouldTgtP()
             {
                PlayerZone compHand = AllZone.getZone(Constant.Zone.Hand, Constant.Player.Computer);
                CardList hand = new CardList(compHand.getCards());

                if (hand.size() >= 7)      // anti-discard-at-EOT
                   return true;
               
                if(AllZone.Human_Life.getLife() < (10 - damage))   // if damage from this spell would drop the human to less than 10 life
                   return true;
               
                return false;
             }
            
             Card chooseTgtC()
             {
                // Combo alert!!
              PlayerZone compy = AllZone.getZone(Constant.Zone.Play, Constant.Player.Computer);
                CardList cPlay = new CardList(compy.getCards());
                if (cPlay.size() > 0)
                   for (int i = 0; i < cPlay.size(); i++)
                      if (cPlay.get(i).getName().equals("Stuffy Doll"))
                         return cPlay.get(i);
               
                PlayerZone human = AllZone.getZone(Constant.Zone.Play, Constant.Player.Human);
              CardList hPlay = new CardList(human.getCards());
              hPlay = hPlay.filter(new CardListFilter()
               {
                  public boolean addCard(Card c)
                  {
                     // will include creatures already dealt damage
                     return c.isCreature() && ((c.getNetDefense() + c.getDamage()) <= damage) && CardFactoryUtil.canTarget(card, c);
                  }
               }
              );
             
                if (hPlay.size() > 0)
                {
                   Card best = hPlay.get(0);
               
                   if (hPlay.size() > 1)
                   {
                      for (int i = 1; i < hPlay.size(); i++)
                      {
                         Card b = hPlay.get(i);
                         // choose best overall creature?
                       if (b.getSpellAbility().length > best.getSpellAbility().length ||
                               b.getKeyword().size() > best.getKeyword().size() ||
                               b.getNetAttack() > best.getNetAttack())
                            best = b;
                      }
                   }
                  
                   return best;
                }
                  
                return null;
             }
            
             public boolean canPlayAI()
             {
                damage = getNumDamage();
               
                if (TgtCP[0] == true)
                {
                   if (shouldTgtP() == true)
                   {
                      setTargetPlayer(Constant.Player.Human);
                      return true;
                   }
                  
                   Card c = chooseTgtC();
                   if (c != null)
                   {
                      setTargetCard(c);
                      return true;
                   }
                }
               
                if (TgtPlayer[0] == true || TgtOpp[0] == true)
                {
                   setTargetPlayer(Constant.Player.Human);
                   return true; 
                }
               
                if (TgtCreature[0] == true)
                {
                   Card c = chooseTgtC();
                   if (c != null)
                   {
                      setTargetCard(c);
                      return true;
                   }
                }
               
                return false;               
             }

             public void resolve()
             {
                damage = getNumDamage();
                String tgtP = new String();
                
                if (TgtOpp[0] == true)
                	setTargetPlayer(AllZone.GameAction.getOpponent(card.getController()));
               
                if(getTargetCard() != null)
                {
                   if(AllZone.GameAction.isCardInPlay(getTargetCard()) && CardFactoryUtil.canTarget(card, getTargetCard()))
                   {
                      Card c = getTargetCard();
                      //c.addDamage(damage);
                      AllZone.GameAction.addDamage(c, card, damage);
                      tgtP = c.getController();
                   }
                }
                else
                {
                   AllZone.GameAction.addDamage(getTargetPlayer(), damage);
                   tgtP = getTargetPlayer();
                }

                if (! DrawBack[0].equals("none"))
                   CardFactoryUtil.doDrawBack(DrawBack[0], damage, card.getController(), AllZone.GameAction.getOpponent(card.getController()), tgtP, card, getTargetCard());
             }// resolove
          }; //spellAbility
          if (! spDesc[0].equals("none"))
              DamageTgt.setDescription(spDesc[0]);
            else
            {
              String s;
              s = card.getName() + " deals " + NumDmg[0] + " damage to target";
              if (TgtCP[0])
                s = s + " creature or player.";
              else if (TgtCreature[0])
                s = s + " creature.";
              else if (TgtPlayer[0])
                s = s + " player.";
              DamageTgt.setDescription(s);
            }

            if (! stDesc[0].equals("none"))
              DamageTgt.setStackDescription(stDesc[0]);
            else
              DamageTgt.setStackDescription(card.getName() + " - deals " + NumDmg[0] + " damage.");

          if (TgtCP[0])
             DamageTgt.setBeforePayMana(CardFactoryUtil.input_targetCreaturePlayer(DamageTgt, true));
          else if (TgtCreature[0])
             DamageTgt.setBeforePayMana(CardFactoryUtil.input_targetCreature(DamageTgt));
          else if (TgtPlayer[0])
             DamageTgt.setBeforePayMana(CardFactoryUtil.input_targetPlayer(DamageTgt));

        card.addSpellAbility(DamageTgt);
       }
    }// spDamageTgt

    
/*
    //Spell damage cards  CP means Computer and Player (like shock, Lightning Bolt)
    if (hasKeyword(card, "spDamageCP")  != -1)
    {
       int n = hasKeyword(card, "spDamageCP");
       if (n != -1)
       {
          String parse = card.getKeyword().get(n).toString();
          //System.out.println("parse: " + parse);
          card.removeIntrinsicKeyword(parse);
                    
          card.clearSpellAbility();
          
          String k[] = parse.split(":");
          final String dmg = k[1];
          
          card.addSpellAbility(CardFactoryUtil.spellability_spDamageCP(card, dmg));
       }
    }
    
    if (hasKeyword(card, "spDamageP") != -1)
    {
       int n = hasKeyword(card, "spDamageP");
       if (n != -1)
       {
          String parse = card.getKeyword().get(n).toString();
          card.removePrevIntrinsicKeyword(parse);
       
          card.clearSpellAbility();
       
          String k[] = parse.split(":");
          final String dmg = k[1];
       
          card.addSpellAbility(CardFactoryUtil.spellability_spDamageP(card, dmg));
       }
    }//SpDamageP
*/    

    if (hasKeyword(card, "abDamageCP") != -1)
    {
       int n = hasKeyword(card, "abDamageCP");
       if (n != -1)
       {
          String parse = card.getKeyword().get(n).toString();
          card.removeIntrinsicKeyword(parse);
          
          String k[] = parse.split(":");

            String tmpCost = k[0].substring(11);
           
            final int dmg[] = new int[1];
            dmg[0] = Integer.parseInt(k[1]);
           
            boolean tapCost = false;
            boolean tapOnlyCost = false;
           
            if (tmpCost.contains("T"))
            {
               tapCost = true;
               tmpCost = tmpCost.replace("T", "");
               tmpCost = tmpCost.trim();
               if (tmpCost.length() == 0)
                  tapOnlyCost = true;
            }
           
            final String manaCost = tmpCost;
           
            String tempDesc = new String();
            tempDesc = cardName + " deals " + dmg[0] + " damage to target creature or player.";
            final String Desc = tempDesc;

            if (! tapCost)
            {
               final SpellAbility ability = new Ability_Activated(card, manaCost)
               {
                 private static final long serialVersionUID = -7560349014757367722L;
                
                 public boolean canPlayAI()
                 {
                    Random r = new Random();
                    if (r.nextFloat() <= Math.pow(.6667, card.getAbilityUsed()))
                       return true;
                    else
                       return false;
                 }
                
                 public void chooseTargetAI()
                 {
                   CardList list = CardFactoryUtil.AI_getHumanCreature(dmg[0], card, true);
                   list.shuffle();
   
                   if(list.isEmpty() || AllZone.Human_Life.getLife() < 5 + dmg[0])
                     setTargetPlayer(Constant.Player.Human);
                   else
                     setTargetCard(list.get(0));
                 }//chooseTargetAI
                
                 public void resolve()
                 {
                   if(getTargetCard() != null)
                   {
                     if(AllZone.GameAction.isCardInPlay(getTargetCard())  && CardFactoryUtil.canTarget(card, getTargetCard()) ) {
                       if (card.getKeyword().contains("Wither"))
                    	   getTargetCard().addCounter(Counters.M1M1, dmg[0]);
                       else
                    	   getTargetCard().addDamage(dmg[0], card);
                       if (card.getKeyword().contains("Lifelink"))
                    	   GameActionUtil.executeLifeLinkEffects(card, dmg[0]);
                       
                       CardList cl = CardFactoryUtil.getAurasEnchanting(card, "Guilty Conscience");
                	   for (Card c : cl)
                	   {
                		   GameActionUtil.executeGuiltyConscienceEffects(card, c, dmg[0]);
                	   }
                     }
                   }
                   else {
                     AllZone.GameAction.getPlayerLife(getTargetPlayer()).subtractLife(dmg[0]);
                     if (card.getKeyword().contains("Lifelink"))
                    	 GameActionUtil.executeLifeLinkEffects(card, dmg[0]);
                     
                     CardList cl = CardFactoryUtil.getAurasEnchanting(card, "Guilty Conscience");
           		     for (Card c : cl)
           		     {
           			   GameActionUtil.executeGuiltyConscienceEffects(card, c, dmg[0]);
           		     }
                   }
                 }//resolve()
               };//Ability_Activated
               
               ability.setDescription(manaCost + ": " + Desc);
               ability.setBeforePayMana(CardFactoryUtil.input_targetCreaturePlayer(ability, true));
               card.addSpellAbility(ability);
            }//!tapCost
           
            if (tapOnlyCost == true)
            {
               final Ability_Tap ability = new Ability_Tap(card)
               {
                 private static final long serialVersionUID = -7560349014757367722L;
                
                 public void chooseTargetAI()
                 {
                   CardList list = CardFactoryUtil.AI_getHumanCreature(1, card, true);
                   list.shuffle();
   
                   if(list.isEmpty() || AllZone.Human_Life.getLife() < 5 + dmg[0])
                     setTargetPlayer(Constant.Player.Human);
                   else
                     setTargetCard(list.get(0));
                 }//chooseTargetAI
                
                 public void resolve()
                 {
                   if(getTargetCard() != null)
                   {
                     if(AllZone.GameAction.isCardInPlay(getTargetCard())  && CardFactoryUtil.canTarget(card, getTargetCard()) )
                     {
                       if (card.getKeyword().contains("Wither"))
                      	 getTargetCard().addCounter(Counters.M1M1, dmg[0]);
                       else
                      	 getTargetCard().addDamage(dmg[0], card);
                       if (card.getKeyword().contains("Lifelink"))
                    	   GameActionUtil.executeLifeLinkEffects(card, dmg[0]); 
                       
                       CardList cl = CardFactoryUtil.getAurasEnchanting(card, "Guilty Conscience");
             		   for (Card c : cl)
             		   {
             			   GameActionUtil.executeGuiltyConscienceEffects(card, c, dmg[0]);
             		   }
                       
                     }
                   }
                   else {
                     AllZone.GameAction.getPlayerLife(getTargetPlayer()).subtractLife(dmg[0]);
                     if (card.getKeyword().contains("Lifelink"))
                    	 GameActionUtil.executeLifeLinkEffects(card, dmg[0]);

                     CardList cl = CardFactoryUtil.getAurasEnchanting(card, "Guilty Conscience");
           		     for (Card c : cl)
           		     { 
           			     GameActionUtil.executeGuiltyConscienceEffects(card, c, dmg[0]);
           		     }
                     
                     
                     card.setDealtDmgToOppThisTurn(true);
                   }
                 }//resolve()
               };//Ability_Tap
           
               ability.setDescription("tap: " + Desc);
               ability.setBeforePayMana(CardFactoryUtil.input_targetCreaturePlayer(ability, true));
               card.addSpellAbility(ability);
          }//tapOnlyCost
         
          if (! tapOnlyCost && tapCost)
          {
             final SpellAbility ability = new Ability_Tap(card, manaCost)
             {
                 private static final long serialVersionUID = -7560349014757367722L;
                
                 public void chooseTargetAI()
                 {
                   CardList list = CardFactoryUtil.AI_getHumanCreature(1, card, true);
                   list.shuffle();
   
                   if(list.isEmpty() || AllZone.Human_Life.getLife() < 5 + dmg[0])
                     setTargetPlayer(Constant.Player.Human);
                   else
                     setTargetCard(list.get(0));
                 }//chooseTargetAI
                
                 public void resolve()
                 {
                   if(getTargetCard() != null)
                   {
                     if(AllZone.GameAction.isCardInPlay(getTargetCard())  && CardFactoryUtil.canTarget(card, getTargetCard()) ) {
                       if (card.getKeyword().contains("Wither"))
                      	 getTargetCard().addCounter(Counters.M1M1, dmg[0]);
                       else
                      	 getTargetCard().addDamage(dmg[0], card);
                       if (card.getKeyword().contains("Lifelink"))
                    	   GameActionUtil.executeLifeLinkEffects(card, dmg[0]);
                       
                       CardList cl = CardFactoryUtil.getAurasEnchanting(card, "Guilty Conscience");
             		   for (Card c : cl)
             		   {
             			   GameActionUtil.executeGuiltyConscienceEffects(card, c, dmg[0]);
             		   }
                   	 }
                   }
                   else {
                     AllZone.GameAction.getPlayerLife(getTargetPlayer()).subtractLife(dmg[0]);
                     if (card.getKeyword().contains("Lifelink"))
                    	 GameActionUtil.executeLifeLinkEffects(card, dmg[0]);
                     

                     CardList cl = CardFactoryUtil.getAurasEnchanting(card, "Guilty Conscience");
           		     for (Card c : cl)
           		     {
           			    GameActionUtil.executeGuiltyConscienceEffects(card, c, dmg[0]);
           		     }
                     
                     card.setDealtDmgToOppThisTurn(true);
                   }
                 }//resolve()
               };//Ability_Tap
           
               ability.setDescription(manaCost + ", tap: " + Desc);
               ability.setBeforePayMana(CardFactoryUtil.input_targetCreaturePlayer(ability, true));
               card.addSpellAbility(ability);               
          }//!tapOnlyCost && tapCost
       }//n       
    }//AbDamageCP
    
    
    while (hasKeyword(card, "abTgtPTPump") != -1)
    {
     int n = hasKeyword(card, "abTgtPTPump");
     if (n != -1)
     {
        String parse = card.getKeyword().get(n).toString();
        card.removeIntrinsicKeyword(parse);
        
        String k[] = parse.split(":");
        String pt[] = k[1].split("/");
        
          final int attack[] = new int[1];
          final int defense[] = new int[1];
                   
          attack[0] = Integer.parseInt(pt[0].replace("+", ""));
          defense[0] = Integer.parseInt(pt[1].replace("+", ""));

        String tmpCost = k[0].substring(11);
        boolean tapCost = false;
        boolean tapOnlyCost = false;
        
        if (tmpCost.contains("T"))
        {
           tapCost = true;
              tmpCost = tmpCost.replace("T", "");
              tmpCost = tmpCost.trim();
              if (tmpCost.length() == 0)
                 tapOnlyCost = true;
           }
         
           final String manaCost = tmpCost;
           //System.out.println("manaCost is " + manaCost + " for " + card.getName());
         
           String tmpDesc = new String();
           tmpDesc = "Target creature gets ";
           if (attack[0] > 0)
               tmpDesc = tmpDesc + "+" + attack[0];
           else
               tmpDesc = tmpDesc + attack[0];
           tmpDesc = tmpDesc + "/";
           if (defense[0] > 0)
               tmpDesc = tmpDesc + "+" + defense[0];
           else
               tmpDesc = tmpDesc + defense[0];
           tmpDesc = tmpDesc + " until end of turn.";
           final String Desc = tmpDesc;
           
           if (!tapCost)
           {
              final SpellAbility ability = new Ability_Activated(card, manaCost)
              {
             private static final long serialVersionUID = -845173064437485113L;
             
             public boolean canPlay()
                 {
                    if (CardFactoryUtil.canUseAbility(card) && AllZone.GameAction.isCardInPlay(card) &&
                          !card.isFaceDown())
                       return true;
                    else
                       return false;
                 }
                 public boolean canPlayAI()
                 {
                    CardList list = getAttackers();
                    if (list.isEmpty())
                       return false;
                    else
                    {
                       if (list.get(0).getNetDefense() + defense[0] < 1)
                          return false;
                          Random r = new Random();
                          if (r.nextFloat() <= Math.pow(.6667, card.getAbilityUsed()))
                          {
                             setTargetCard(list.get(0));
                             return true;
                          }
                          else
                             return false;
                    }
                 }//canPlayAI
                 public CardList getAttackers()
                 {
                    Card[] c = ComputerUtil.getAttackers().getAttackers();
                    CardList list = new CardList(c);
                   
                    return list;
                 }//getAttacker
                 public void resolve()
                 {
                    if (AllZone.GameAction.isCardInPlay(getTargetCard()) && CardFactoryUtil.canTarget(card, getTargetCard()))
                    {
                       final Card[] creature = new Card[1];
                      
                       creature[0] = getTargetCard();
                       creature[0].addTempAttackBoost(attack[0]);
                       creature[0].addTempDefenseBoost(defense[0]);
                      
                       card.setAbilityUsed(card.getAbilityUsed()+1);
                      
                        final Command EOT = new Command()
                        {
                      private static final long serialVersionUID = 122944434978198700L;

                      public void execute()
                           {
                              if (AllZone.GameAction.isCardInPlay(creature[0]))
                              {
                                 creature[0].addTempAttackBoost(-attack[0]);
                                 creature[0].addTempDefenseBoost(-defense[0]);
                              }
                           }
                        };//EOT
                        AllZone.EndOfTurn.addUntil(EOT);
                    }
                 }//resolve
              };//ability
              ability.setDescription(manaCost+": "+Desc);
              ability.setStackDescription(Desc);
              ability.setBeforePayMana(CardFactoryUtil.input_targetCreature(ability));
              card.addSpellAbility(ability);
           }//!tapCost
           
           if (tapOnlyCost)
           {
              final SpellAbility ability = new Ability_Tap(card)
              {
             private static final long serialVersionUID = 6723777240105966031L;
             
             public boolean canPlay()
                 {
                   boolean sick = true;
                   if (!card.hasSickness() || !card.isCreature())
                      sick = false;
                   if (card.isUntapped() && CardFactoryUtil.canUseAbility(card) &&
                         AllZone.GameAction.isCardInPlay(card) && !sick && !card.isFaceDown())
                      return true;
                   else
                      return false;
                 }//canPlay
                 public boolean canPlayAI()
                 {
                    CardList list = getAttackers();
                    if (list.isEmpty())
                       return false;
                    else
                       if (list.get(0).getNetDefense() + defense[0] < 1)
                          return false;
                    if (CardFactoryUtil.AI_doesCreatureAttack(card))
                       return false;
                    setTargetCard(list.get(0));
                    return true;
                 }//canPlayAI
                 public CardList getAttackers()
                 {
                    Card[] c = ComputerUtil.getAttackers().getAttackers();
                    CardList list = new CardList(c);
                    list.remove(card);
                    return list;
                 }
                 public void resolve()
                 {
                    if (AllZone.GameAction.isCardInPlay(getTargetCard()) && CardFactoryUtil.canTarget(card, getTargetCard()))
                    {
                       final Card[] creature = new Card[1];
                      
                       creature[0] = getTargetCard();
                       creature[0].addTempAttackBoost(attack[0]);
                       creature[0].addTempDefenseBoost(defense[0]);
                      
                       final Command EOT = new Command()
                       {
                      private static final long serialVersionUID = -852905560563053752L;

                      public void execute()
                          {
                             if (AllZone.GameAction.isCardInPlay(creature[0]))
                             {
                                creature[0].addTempAttackBoost(-attack[0]);
                                creature[0].addTempDefenseBoost(-defense[0]);
                             }
                          }
                       };//EOT
                       AllZone.EndOfTurn.addUntil(EOT);
                    }
                 }//resolve
              };//ability
              ability.setDescription("tap: "+Desc);
              ability.setStackDescription(Desc);
              ability.setBeforePayMana(CardFactoryUtil.input_targetCreature_NoCost_TapAbility((Ability_Tap)ability));
              card.addSpellAbility(ability);
           }//tapOnlyCost
           
           if (!tapOnlyCost && tapCost)
           {
              final SpellAbility ability = new Ability_Tap(card, manaCost)
              {
             private static final long serialVersionUID = 2749576299299014851L;
             
             public boolean canPlay()
                 {
                    boolean sick = true;
                    if (!card.hasSickness() || !card.isCreature())
                       sick = false;
                    if (card.isUntapped() && CardFactoryUtil.canUseAbility(card) &&
                          AllZone.GameAction.isCardInPlay(card) && !sick && !card.isFaceDown())
                       return true;
                    else
                       return false;
                 }//canPlay
                 public boolean canPlayAI()
                 {
                    CardList list = getAttackers();
                    if (list.isEmpty())
                       return false;
                    else
                       if (list.get(0).getNetDefense() + defense[0] < 1)
                          return false;
                    if (CardFactoryUtil.AI_doesCreatureAttack(card))
                       return false;
                    setTargetCard(list.get(0));
                    return true;
                 }//canPlayAI
                 public CardList getAttackers()
                 {
                    Card[] c = ComputerUtil.getAttackers().getAttackers();
                    CardList list = new CardList(c);
                    list.remove(card);
                    return list;
                 }//getAttackers
                 public void resolve()
                 {
                    if (AllZone.GameAction.isCardInPlay(getTargetCard()) && CardFactoryUtil.canTarget(card, getTargetCard()))
                    {
                       final Card[] creature = new Card[1];
                      
                       creature[0] = getTargetCard();
                       creature[0].addTempAttackBoost(attack[0]);
                       creature[0].addTempDefenseBoost(defense[0]);
                      
                       final Command EOT = new Command()
                       {
                      private static final long serialVersionUID = 8179097336678296338L;

                      public void execute()
                         {
                            if (AllZone.GameAction.isCardInPlay(creature[0]))
                            {
                               creature[0].addTempAttackBoost(-attack[0]);
                               creature[0].addTempDefenseBoost(-defense[0]);
                            }
                         }
                       };//EOT
                       AllZone.EndOfTurn.addUntil(EOT);
                    }   
                 }//resolve
              };//ability
              ability.setDescription(manaCost+ ", tap: "+Desc);
              ability.setStackDescription(Desc);
              ability.setBeforePayMana(CardFactoryUtil.input_targetCreature(ability));
              card.addSpellAbility(ability);
           }//!tapCost
     }
    }//while
    
    // Generic destroy target card
    if (hasKeyword(card, "spDestroyTgt") != -1)
    {
       int n = hasKeyword(card, "spDestroyTgt");
       
       String parse = card.getKeyword().get(n).toString();
        card.removeIntrinsicKeyword(parse);
       
        String k[] = parse.split(":");
        String Targets = k[1];   // Artifact, Creature, Enchantment, Land, Permanent, White, Blue, Black, Red, Green, Colorless, MultiColor
                          // non-Artifact, non-Creature, non-Enchantment, non-Land, non-Permanent,
                          //non-White, non-Blue, non-Black, non-Red, non-Green, non-Colorless, non-MultiColor
        final String Tgts[] = Targets.split(",");
       
        String tmpDesc = card.getText().substring(15);
        int i = tmpDesc.indexOf(".");
        tmpDesc = tmpDesc.substring(0, i);
        final String Selec = new String("Select target " + tmpDesc + " to destroy.");
      
      final boolean NoRegen = (k.length == 3);
      
        card.clearSpellAbility();
       
        final SpellAbility spDstryTgt = new Spell(card)
        {
         private static final long serialVersionUID = 142142142142L;
         
         public boolean canPlayAI()
         {
            CardList results = new CardList();
            CardList choices = getTargets();
            
            if (choices.size() > 0)
            {
               for (int i = 0; i < Tgts.length; i++)
               {
                  if (Tgts[i].equals("Artifact")) {
                	 if (CardFactoryUtil.AI_getBestArtifact(choices) != null)
                		 results.add(CardFactoryUtil.AI_getBestArtifact(choices));
                  }
                  else if (Tgts[i].equals("Creature")) {
                	 if (CardFactoryUtil.AI_getBestCreature(choices) != null)
                     	results.add(CardFactoryUtil.AI_getBestCreature(choices));
                  }
                  else if (Tgts[i].equals("Enchantment")) {
                	 if (CardFactoryUtil.AI_getBestEnchantment(choices, card, true) != null)
                	  	results.add(CardFactoryUtil.AI_getBestEnchantment(choices, card, true));
                  }
                  else if (Tgts[i].equals("Land")) {
                	  if (CardFactoryUtil.AI_getBestLand(choices) != null)
                	  	results.add(CardFactoryUtil.AI_getBestLand(choices));
                  }
                  else if (Tgts[i].equals("Permanent")){
                	 if (CardFactoryUtil.AI_getMostExpensivePermanent(choices, card, true) != null)
                     	results.add(CardFactoryUtil.AI_getMostExpensivePermanent(choices, card, true));
                  }
               }
            }
            
            if (results.size() > 0)
            {
               results.shuffle();
               setTargetCard(results.get(0));
               return true;
            }
            return false;
         }
         CardList getTargets()
         {
            CardList tmpList = new CardList();
            tmpList.addAll(AllZone.Human_Play.getCards());
            tmpList.filter(new CardListFilter()
            {
               public boolean addCard(Card c)
               {
                  return (CardFactoryUtil.canTarget(card, c));
               }
            });
            
            return tmpList.getValidCards(Tgts);
         }
         public void resolve()
           {
              if (AllZone.GameAction.isCardInPlay(getTargetCard()) &&  CardFactoryUtil.canTarget(card, getTargetCard()))
                 if (NoRegen)
                    AllZone.GameAction.destroyNoRegeneration(getTargetCard());
                 else
                    AllZone.GameAction.destroy(getTargetCard());
              
           }
        }; //SpDstryTgt
       
        Input InGetTarget = new Input()
        {
           private static final long serialVersionUID = -142142142142L;
           
           public void showMessage()
           {
              CardList allCards = new CardList();
              allCards.addAll(AllZone.Human_Play.getCards());
              allCards.addAll(AllZone.Computer_Play.getCards());
              allCards.filter(new CardListFilter()
            {
               public boolean addCard(Card c)
               {
                  return (CardFactoryUtil.canTarget(card, c));
               }
            });
              
              CardList choices = allCards.getValidCards(Tgts);
              stopSetNext(CardFactoryUtil.input_targetSpecific(spDstryTgt, choices, Selec, true));
           }
        };//InGetTarget
       
        //card.clearSpellAbility();
        spDstryTgt.setBeforePayMana(InGetTarget);
        spDstryTgt.setDescription(card.getText());
        card.setText("");
        card.addSpellAbility(spDstryTgt);
    }//spDestroyTgt
    
    if (hasKeyword(card, "spDrawCards") != -1)
    {
       int n = hasKeyword(card, "spDrawCards");
       String parse = card.getKeyword().get(n).toString();
        card.removeIntrinsicKeyword(parse);
       
        String k[] = parse.split(":");
       
        final boolean Tgt[] = {false};
        Tgt[0] = k[0].contains("Tgt");
       
        final int NumCards[] = {-1};
        final String NumCardsX[] = {"none"};

        if (k[1].length() <= 2)
           NumCards[0] = Integer.parseInt(k[1]);
        else
        {
           if (k[1].startsWith("Count$"))
           {             
             String kk[] = k[1].split("\\$");
              NumCardsX[0] = kk[1];
           }
        }
       
        // drawbacks and descriptions
        final String DrawBack[] = {"none"};
        final String spDesc[] = {"none"};
        final String stDesc[] = {"none"};
        if (k.length > 2)
        {
           if (k[2].contains("Drawback$"))
           {
              String kk[] = k[2].split("\\$");
              DrawBack[0] = kk[1];
              if (k.length > 3)
                 spDesc[0] = k[3];
              if (k.length > 4)
                 stDesc[0] = k[4];
           }
           else
           {
              if (k.length > 2)
                 spDesc[0] = k[2];
              if (k.length > 3)
                 stDesc[0] = k[3];
           }
        }
       
        final SpellAbility spDraw = new Spell(card)
        {
           private static final long serialVersionUID = -7049779241008089696L;

           private int ncards;
           
           public int getNumCards()
           {
              if (NumCards[0] != -1)
                 return NumCards[0];

              if (! NumCardsX[0].equals("none"))
                 return CardFactoryUtil.xCount(card, NumCardsX[0]);
             
              return 0;
           }

           public boolean canPlayAI()
           {
              ncards = getNumCards();
              int h = AllZone.getZone(Constant.Zone.Hand, Constant.Player.Computer).size();
              int hl = AllZone.getZone(Constant.Zone.Library, Constant.Player.Human).size();
              int cl = AllZone.getZone(Constant.Zone.Library, Constant.Player.Computer).size();
              Random r = new Random();

              if (((hl - ncards) < 2) && Tgt[0])
              {
                 setTargetPlayer(Constant.Player.Computer);
                 return true;
              }
             
              if (((h + ncards) <= 7) && !((cl - ncards) < 1) && (r.nextInt(10) > 4))
              {
                 setTargetPlayer(Constant.Player.Computer);
                 return true;
              }
             
              return false;
           }

           public void resolve()
           {
              ncards = getNumCards();

              String TgtPlayer = card.getController();
              if (Tgt[0])
                 TgtPlayer = getTargetPlayer();

              for (int i=0; i < ncards; i++)
                 AllZone.GameAction.drawCard(TgtPlayer);

              if (! DrawBack[0].equals("none"))
                 CardFactoryUtil.doDrawBack(DrawBack[0], ncards, card.getController(), AllZone.GameAction.getOpponent(card.getController()), TgtPlayer, card, null);
           }
        };

        if (Tgt[0])
           spDraw.setBeforePayMana(CardFactoryUtil.input_targetPlayer(spDraw));

        if (! spDesc[0].equals("none"))
           spDraw.setDescription(spDesc[0]);
        else
           spDraw.setDescription("Draw " + NumCards[0] + " cards.");
       
        if (! stDesc[0].equals("none"))
           spDraw.setStackDescription(stDesc[0]);
        else
           spDraw.setStackDescription("You draw " + NumCards[0] + " cards.");
       
        card.clearSpellAbility();
        card.addSpellAbility(spDraw);
    }//spDrawCards

    //Spell gain life lose life cards (like Soul Feast)
    if (hasKeyword(card, "spLoseLifeGainLife")  != -1)
    {
       int n = hasKeyword(card, "spLoseLifeGainLife");
       if (n != -1)
       {
          String parse = card.getKeyword().get(n).toString();
          card.removeIntrinsicKeyword(parse);
         
          String k[] = parse.split(":");
          final String lfdmg = k[1];
          
          final String spDesc[] = {"none"};
          final String stDesc[] = {"none"};
          
          if (k.length > 2)
        	  spDesc[0] = k[2];
          if (k.length > 3)
        	  stDesc[0] = k[3];
         
          final SpellAbility spell = new Spell(card)
          {
			 private static final long serialVersionUID = -8361697584661592092L;

			 public void resolve()
             {
                final int n = Integer.parseInt(lfdmg);
             
                AllZone.GameAction.getPlayerLife(getTargetPlayer()).subtractLife(n);
                PlayerLife life = AllZone.GameAction.getPlayerLife(card.getController());
                life.addLife(n);
             }//resolve()
          };//SpellAbility

          spell.setChooseTargetAI(CardFactoryUtil.AI_targetHuman());
          spell.setBeforePayMana(CardFactoryUtil.input_targetPlayer(spell));

          if (spDesc[0].equals("none"))		// create the card description
          {
        	  spDesc[0] = ("Target player loses " + lfdmg + " life and you gain " + lfdmg + " life.");
          }
          
          if (stDesc[0].equals("none"))		// create the card stack description
          {
        	  stDesc[0] = (card.getName() + " - target loses life and you gain life.");
          }
          
          spell.setDescription(spDesc[0]);
          spell.setStackDescription(stDesc[0]);
          
          card.clearSpellAbility();
          card.addSpellAbility(spell);
          
          if (cardName.equals("Absorb Vis"))
          {
              card.addSpellAbility(CardFactoryUtil.ability_typecycle(card, "1 B","Basic"));
          }

          return card;
       }
    }// spLoseLifeGainLife
    
    if (hasKeyword(card, "SearchRebel") != -1)
    {
      int n = hasKeyword(card, "SearchRebel");
      if (n != -1)
      {
    	String parse = card.getKeyword().get(n).toString();
        card.removeIntrinsicKeyword(parse);
        
        String k[] = parse.split(":");
        final String manacost = k[1];
        
        card.addSpellAbility(CardFactoryUtil.ability_Rebel_Search(card, manacost));
      }
    }//Rebel search
    
    if (hasKeyword(card, "SearchMerc") != -1)
    {
      int n = hasKeyword(card, "SearchMerc");
      if (n != -1)
      {
    	String parse = card.getKeyword().get(n).toString();
        card.removeIntrinsicKeyword(parse);
        
        String k[] = parse.split(":");
        final String manacost = k[1];
        
        card.addSpellAbility(CardFactoryUtil.ability_Merc_Search(card, manacost));
      }
    }//Merc search
    
    if (hasKeyword(card, "Morph") != -1)
    {
      int n = hasKeyword(card, "Morph");
      if (n != -1)
      {
    	card.setPrevIntrinsicKeyword(card.getIntrinsicKeyword());
    	card.setPrevType(card.getType());
    	
    	String parse = card.getKeyword().get(n).toString();
        card.removeIntrinsicKeyword(parse);
        
        String k[] = parse.split(":");
        final String manacost = k[1];
        
        int attack = card.getBaseAttack();
        int defense = card.getBaseDefense();
        
        String orgManaCost = card.getManaCost();
        
        card.addSpellAbility(CardFactoryUtil.ability_Morph_Up(card, manacost, orgManaCost, attack, defense));
        card.addSpellAbility(CardFactoryUtil.ability_Morph_Down(card));
      }
    }//Morph

    if (hasKeyword(card, "Flashback") != -1)
    {
      int n = hasKeyword(card, "Flashback");
      if (n != -1)
      {
    	String parse = card.getKeyword().get(n).toString();
        //card.removeIntrinsicKeyword(parse);
        
        String k[] = parse.split(":");
              
        final String manacost = k[1];
        
        card.addSpellAbility(CardFactoryUtil.ability_Flashback(card, manacost,"0"));
      }
    }//flashback
    
    

    
    if (hasKeyword(card, "Devour") != -1)
    {
      int n = hasKeyword(card, "Devour");
      if (n != -1)
      {
    	
    	String parse = card.getKeyword().get(n).toString();
        card.removeIntrinsicKeyword(parse);
        
        String k[] = parse.split(":");
        final String magnitude = k[1];
        
        
        final int multiplier = Integer.parseInt(magnitude);
	  	//final String player = card.getController();
	  	final int[] numCreatures = new int[1];
	  	
	  	  
	  	final SpellAbility devour = new Spell(card)
	  	{
			private static final long serialVersionUID = 4888189840817163900L;
			
			public void resolve()
	  		{
	  			int totalCounters = numCreatures[0] * multiplier;
	  			card.addCounter(Counters.P1P1, totalCounters);
	  			
	  			if(card.getName().equals("Skullmulcher"))
	  			{
	  				for(int i=0;i<numCreatures[0];i++)
	  				{
	  					AllZone.GameAction.drawCard(card.getController());
	  				}
	  			}
	  			else if (card.getName().equals("Caldera Hellion"))
		      	{
	      		  PlayerZone hPlay = AllZone.getZone(Constant.Zone.Play, Constant.Player.Human);
	      		  PlayerZone cPlay = AllZone.getZone(Constant.Zone.Play, Constant.Player.Computer);
	      		  CardList creatures = new CardList();
	      		  
	      		  creatures.addAll(hPlay.getCards());
	      		  creatures.addAll(cPlay.getCards());
	      		  creatures = creatures.getType("Creature");
	      		  
	      		  for(int i=0;i<creatures.size();i++)
	      		  {
	      			  Card crd = creatures.get(i);
	      			  if (CardFactoryUtil.canDamage(card, crd))
	      				  crd.addDamage(3, card);
	      		  }
		      	}
	  				
	  		}
	  		public boolean canPlay()
	  		{
	  			return AllZone.Phase.getActivePlayer().equals(card.getController()) && card.isFaceDown() &&
	                  !AllZone.Phase.getPhase().equals("End of Turn") && AllZone.GameAction.isCardInPlay(card);
	  		}
	  			
	  	};//devour
  		
	  	 Command intoPlay = new Command()
	     {
			private static final long serialVersionUID = -7530312713496897814L;

			public void execute()
	        {
	          
	          PlayerZone play = AllZone.getZone(Constant.Zone.Play, card.getController());
	          CardList creatsToSac = new CardList();
	      	  CardList creats = new CardList(play.getCards());
	      	  creats = creats.filter(new CardListFilter()
	          {
	            public boolean addCard(Card c)
	            {
	              return c.isCreature() && !c.equals(card);
	            }
	          });
	      	  
	      	  //System.out.println("Creats size: " + creats.size());
	      	  
	      	  if (card.getController().equals(Constant.Player.Human))
	      	  {
		      	  Object o = null;
		      	  int creatsSize = creats.size();
		      	  
		      	  for(int k=0; k < creatsSize; k++)
		      	  {
		       		   o = AllZone.Display.getChoiceOptional("Select creature to sacrifice", creats.toArray());
		       		   
		       		   if(o==null)
		       			   break;
		       		   
		       		   Card c = (Card)o;
		       		   creatsToSac.add(c);
		       		   creats.remove(c);
		      	  }
		      	  
		      	  numCreatures[0]=creatsToSac.size();
		      	  for(int m=0;m<creatsToSac.size();m++)
		      	  {
		      		  AllZone.GameAction.sacrifice(creatsToSac.get(m));
		      	  }
		      	 
	      	  }//human
	      	  else
	      	  {
	      		  int count = 0;
	      		  for(int i=0;i<creats.size();i++)
	      		  {
	      			  Card c = creats.get(i);
	      			  if (c.getNetAttack() <= 1 && c.getNetDefense() <= 2) {
	      				  AllZone.GameAction.sacrifice(c);
	      				  count++;
	      			  }
	      			  //is this needed?
	      			  AllZone.Computer_Play.updateObservers(); 
	      		  }
	      		  numCreatures[0] = count;
	      	  }
	          AllZone.Stack.add(devour);
	        }
	      };

	  	  devour.setStackDescription(card.getName() +" - gets " + magnitude + " +1/+1 counter(s) per devoured creature.");
	  	  devour.setDescription("Devour " +magnitude);
	  	  card.addSpellAbility(devour);
	  	  card.addComesIntoPlayCommand(intoPlay);
        
        //card.addSpellAbility(CardFactoryUtil.ability_Devour(card, magnitude));
      }
    }//Devour
    
    while (hasKeyword(card, "Modular") != -1)
    {
       int n = hasKeyword(card, "Modular");
       if (n != -1)
       {
          String parse = card.getKeyword().get(n).toString();
            card.removeIntrinsicKeyword(parse);

            final int m = Integer.parseInt(parse.substring(8));
            String t = card.getSpellText();
            if (!t.equals("")) t += "\r\n";
            card.setText(t + parse + " (This enters the battlefield with " + m + " +1/+1 counters on it. When it's put into a graveyard, you may put its +1/+1 counters on target artifact creature.)");//Erm help? Isn't there a normal way to do this?...
            card.addComesIntoPlayCommand(
                  new Command()
                  {
					private static final long serialVersionUID = 339412525059881775L;

					public void execute()
                     {
                        card.addCounter(Counters.P1P1, m);
                     }});
          final SpellAbility ability = new Ability(card, "0")
            {
             public void resolve()
             {
                if (card.getController().equals(Constant.Player.Computer))
                {
                   CardList choices = new CardList(AllZone.Computer_Play.getCards()).filter(
                    new CardListFilter(){public boolean addCard(Card c) {return c.isCreature() && c.isArtifact(); }});
                   if (choices.size() != 0)
                      CardFactoryUtil.AI_getBestCreature(choices).addCounter(Counters.P1P1, getSourceCard().getCounters(Counters.P1P1));
                }
                else 
                {
	                final SpellAbility ability=this;
	                AllZone.InputControl.setInput(
	                   new Input(){
	                    
						private static final long serialVersionUID = 2322926875771867901L;
						public void showMessage()
	                      {
	                         AllZone.Display.showMessage("Select target artifact creature");
	                         ButtonUtil.enableOnlyCancel();
	                      }
	                      public void selectButtonCancel() {stop();}
	                      public void selectCard(Card card2, PlayerZone zone)
	                      {
	                         if(card2.isCreature() && card2.isArtifact() && zone.is(Constant.Zone.Play) && CardFactoryUtil.canTarget(ability, card))
	                         {
	                            card2.addCounter(Counters.P1P1, ability.getSourceCard().getCounters(Counters.P1P1));//combining input and resolve is skirting rules and hacky at best, but non-stackability of destroyCommand Inputs turns into a major problem when the keyword is mainly used during the simultaneous destruction of combat.
	                            stop();
	                         }
	                      }
	                   });
                }//else
             }//resolve()
            };

          card.addDestroyCommand(new Command()
            {
               private static final long serialVersionUID = 304026662487997331L;
               
               public void execute()
               {
                  ability.setStackDescription("Put "+ card.getCounters(Counters.P1P1) +" +1/+1 counter/s from " + card + " on target artifact creature.");
               AllZone.Stack.push(ability);
               }
            });
           
       }
        
    }//while shouldModular
    
    if (hasKeyword(card, "1, Sacrifice " + card.getName() + ": Draw a card.") != -1)
    {
      int n = hasKeyword(card, "1, Sacrifice " + card.getName() + ": Draw a card.");
      if (n != -1)
      {
    	String parse = card.getKeyword().get(n).toString();
        card.removeIntrinsicKeyword(parse);
       
        card.addSpellAbility(CardFactoryUtil.ability_Spellbomb(card));
      }
    }//Spellbomb
    
    // Generic target creature pump
    if (hasKeyword(card, "spPumpTgt") != -1)
    {
       int n = hasKeyword(card, "spPumpTgt");
       
       String parse = card.getKeyword().get(n).toString();
       card.removeIntrinsicKeyword(parse);
       
       String k[] = parse.split(":");
       String ptk[] = k[1].split("/");
       
       final int attack[] = {0};
       final int defense[] = {0};
       final String keyword[] = {"none"};
       
       if (ptk.length == 1)
          keyword[0] = ptk[0];
       
       if (ptk.length >= 2)
       {
          attack[0] = Integer.parseInt(ptk[0].replace("+", ""));
          defense[0] = Integer.parseInt(ptk[1].replace("+", ""));
       }
       
       if (ptk.length == 3)
          keyword[0] = ptk[2];

       SpellAbility spPump = new Spell(card)
       {
          private static final long serialVersionUID = 42244224L;
          
          public boolean canPlayAI()
          {
             Card c = getAttacker();
             if (c != null)
             {
                setTargetCard(c);
                return true;
             }
             else
                return false;
          }
          
          public Card getAttacker()
          {
             Combat c = ComputerUtil.getAttackers();
             
             CardList list = new CardList(c.getAttackers());
             CardListUtil.sortFlying(list);
             
             list = list.filter(new CardListFilter()
             {
                public boolean addCard(Card c)
                {
                        return (CardFactoryUtil.canTarget(card, c)) &&
                        (! c.getKeyword().contains(keyword[0])) &&
                        (! c.hasSickness() && keyword[0].equals("Haste"));
                }
             });
             
             if (list.size() > 0)
                return list.get(0);
             else
                return null;
          }
          
          public void resolve()
          {
             final Card[] target = new Card[1];
             
             final Command untilEOT = new Command()
             {
                private static final long serialVersionUID = -42244224L;
                
                public void execute()
                {
                   if (AllZone.GameAction.isCardInPlay(target[0]))
                   {
                      target[0].addTempAttackBoost(-attack[0]);
                      target[0].addTempDefenseBoost(-defense[0]);
                      
                      if (!keyword[0].equals("none"))
                         target[0].removeExtrinsicKeyword(keyword[0]);
                   }
                }
             };
             
             target[0] = getTargetCard();
             if (AllZone.GameAction.isCardInPlay(target[0]) && CardFactoryUtil.canTarget(card, target[0]))
                {
                   target[0].addTempAttackBoost(attack[0]);
                   target[0].addTempDefenseBoost(defense[0]);
                   if (!keyword[0].equals("none"))
                      target[0].addExtrinsicKeyword(keyword[0]);
                   
                   AllZone.EndOfTurn.addUntil(untilEOT);
                }
          }
       };
       
       spPump.setBeforePayMana(CardFactoryUtil.input_targetCreature(spPump));
       //spPump.setDescription(Desc);
       card.clearSpellAbility();
       card.addSpellAbility(spPump);
    }
    
    if (hasKeyword(card, "spRaiseDead")  != -1)
    {
       int n = hasKeyword(card, "spRaiseDead");
       if (n != -1)
       {
          String parse = card.getKeyword().get(n).toString();
          card.removeIntrinsicKeyword(parse);
          String k[] = parse.split(":");         // charm descriptions will appear at k[2] and k[3]
          final String kk[] = k[1].split("/");   // numCreatures = kk[0], other fields = kk[1] through kk[2]
          int numFieldsKK = kk.length;
          final int numCreatures = Integer.parseInt(kk[0]);
          boolean quantifier = false;
          String tmpTgt = "Creature";
          
          for (int i=2; i<=numFieldsKK; i++)
          {
             if (kk[(i-1)].equals ("Some"))
             {
                quantifier = true;
             }
             else   // can only be a specific creature type at his time, Goblin for goblin creatures and Tarfire
             {
                tmpTgt = kk[i-1];
             }
          }
          
          final String targetTypeToReturn = tmpTgt;
          final boolean weReturnUpTo = quantifier;
          final String spDesc[] = {"none"};
          final String stDesc[] = {"none"};
          
          if (k.length > 2)
             spDesc[0] = k[2];
          if (k.length > 3)
             stDesc[0] = k[3];
          
          final SpellAbility spell = new Spell(card)
          {
             private static final long serialVersionUID = 6938982619919149188L;
             public boolean canPlayAI() {return getGraveCreatures().size() >= numCreatures;}

             CardList targets;
             public void chooseTargetAI()
             {
                CardList grave = getGraveCreatures();
                targets = new CardList();
                
                if (targetTypeToReturn.equals ("Creature"))
                {
                   for (int i=0; i<numCreatures; i++)
                   {
                      Card c = CardFactoryUtil.AI_getBestCreature(grave);
                      targets.add(c);
                      grave.remove(c);
                   }
                }
                else      // this is for returning Goblins and Tarfire (and Changelings ?)
                {
                   for (int i=0; i<numCreatures; i++)
                   {
                      Card c = CardFactoryUtil.getRandomCard(grave);      // getRandomCard(grave);
                      targets.add(c);
                      grave.remove(c);
                   }
                }
             }

             public void resolve()
             {
                if (card.getController().equals(Constant.Player.Human))
                {
                   CardList grave = getGraveCreatures();
                   targets = new CardList();
                   
                   if (weReturnUpTo)      // this is for spells which state Return up to X target creature card
                   {
                      for (int i=0; i<numCreatures ; i++)
                      {
                         Card c = AllZone.Display.getChoiceOptional("Select card", grave.toArray());
                         targets.add(c);
                         grave.remove(c);
                      }
                   }
                   
                   else if (grave.size() > numCreatures)      // this is for spells which state Return target creature card
                      for (int i=0; i<numCreatures ; i++)
                      {
                         Card c = AllZone.Display.getChoice("Select card", grave.toArray());
                         targets.add(c);
                         grave.remove(c);
                      }
                   else targets = grave;
                }

                PlayerZone grave = AllZone.getZone(Constant.Zone.Graveyard, card.getController());
                PlayerZone hand = AllZone.getZone(Constant.Zone.Hand, card.getController());
                for (Card c : targets)
                   if (AllZone.GameAction.isCardInZone(c, grave))
                      AllZone.GameAction.moveTo(hand, c);
             }//resolve()
             
             public boolean canPlay()
             {
                return getGraveCreatures().size() >= numCreatures;
             }
             
             CardList getGraveCreatures()
             {
                PlayerZone grave = AllZone.getZone(Constant.Zone.Graveyard, card.getController());
                CardList list = new CardList(grave.getCards());
                String cardController = card.getController();
                
                if (cardController.equals ("Human") || (cardController.equals ("Computer")) && (targetTypeToReturn.equals ("Creature")))
                {
                   list = list.getType(targetTypeToReturn);
                }
                else   // prevent the computer from using a Boggart Birth Rite to return a Boggart Birth Rite
                {
                   CardList tempList = new CardList(grave.getCards());
                   tempList = list.getType(targetTypeToReturn);
                   list = new CardList();
                   for (int i=0; i<tempList.size(); i++)
                   {
                      if (! cardName.equals (tempList.get(i).getName()))
                      {
                         list.add(tempList.get(i));
                      }
                   }
                }
                return list;
             }
          };//SpellAbility
          
          if (spDesc[0].equals("none"))   // create the card descriptiopn
          {
             spDesc[0] = ("Return ");
             if (weReturnUpTo)
             {   spDesc[0] = (spDesc[0] + "up to ");   }
             if (numCreatures > 1)
             {   spDesc[0] = (spDesc[0] + numCreatures + " ");   }
             spDesc[0] = (spDesc[0] + "target ");
             if (targetTypeToReturn.equals ("Creature"))
             {   spDesc[0] = (spDesc[0] + "creature");   }
             else
             {   spDesc[0] = (spDesc[0] + targetTypeToReturn);   }
             if (numCreatures > 1)
             {   spDesc[0] = (spDesc[0] + "s");   }
             spDesc[0] = (spDesc[0] + " card");
             if (numCreatures > 1)
             {   spDesc[0] = (spDesc[0] + "s");   }
             spDesc[0] = (spDesc[0] + " from your graveyard to your hand.");
          }
          
          if (stDesc[0].equals("none"))   // create the card stack descriptiopn
          {
             stDesc[0] = (card.getName() + " - returns target card");
             if (numCreatures > 1)
             {   stDesc[0] = (stDesc[0] + "s");   }
             stDesc[0] = (stDesc[0] + " from " + card.getController() + "'s graveyard to " + card.getController() + "'s hand.");
          }
          
          spell.setDescription(spDesc[0]);
          spell.setStackDescription(stDesc[0]);
          card.clearSpellAbility();
          card.addSpellAbility(spell);
       }
    }// spRaiseDead

    
    while (shouldManaAbility(card) != -1)
    {
       int n = shouldManaAbility(card);
       if (n != -1)
       {
          String parse = card.getKeyword().get(n).toString();
          card.removeIntrinsicKeyword(parse);
          final Ability_Mana ability = new Ability_Mana(card, parse)
          {
              private static final long serialVersionUID = -113811381138L;
             
              public boolean canPlayAI()
              {
                 return false;
              }
           };
           //ability.setDescription(parse);
           card.addSpellAbility(ability);
       }
    }
    	    
    ////////////////////////////////////////////////////////////////
    
    if(card.getKeyword().contains("When this card comes into play, draw a card."))
    {
      
    	final SpellAbility ability = new Ability(card, "0")
        {
          public void resolve()
          {
            AllZone.GameAction.drawCard(card.getController());
          }//resolve()
        };//SpellAbility
        Command intoPlay = new Command()
        {

  		private static final long serialVersionUID = 1707519783018941582L;

  		public void execute()
          {
            ability.setStackDescription(card.getName() + " - " +card.getController() +" draws a card.");
            AllZone.Stack.add(ability);
          }
        };
        card.addComesIntoPlayCommand(intoPlay);
    }//end if
    
    
  //******************************************************************
  //************** Link to different CardFactories ******************* 
    if (card.getType().contains("Creature"))
    {
    	Card card2 = new Card();
    	card2 = CardFactory_Creatures.getCard(card, cardName, owner, this);
    	
    	return card2;
    }
    else if (card.getType().contains("Aura"))
    {
    	Card card2 = new Card();
    	card2 = CardFactory_Auras.getCard(card, cardName, owner);
    	
    	return card2;
    }
    else if (card.getType().contains("Equipment"))
    {
    	Card card2 = new Card();
    	card2 = CardFactory_Equipment.getCard(card, cardName, owner);
    	
    	return card2;
    }
    else if (card.getType().contains("Planeswalker"))
    {
    	Card card2 = new Card();
    	card2 = CardFactory_Planeswalkers.getCard(card, cardName, owner);
    	
    	return card2;
    }
    else if (card.getType().contains("Land"))
    {
    	Card card2 = new Card();
    	card2 = CardFactory_Lands.getCard(card, cardName, owner);
    	
    	return card2;
    }
    
    

  //*************** START *********** START **************************
    if(     cardName.equals("Burst of Speed") ||
          cardName.equals("Chorus of Woe") ||
            cardName.equals("Dance of Shadows") ||
            cardName.equals("Desperate Charge") ||
            cardName.equals("Glorious Charge") ||
            cardName.equals("Kjeldoran War Cry") ||
            cardName.equals("Magnify") ||
            cardName.equals("Nature's Cloak") ||
            cardName.equals("Nocturnal Raid") ||
            cardName.equals("Overrun") ||
            cardName.equals("Path of Anger's Flame") ||
            cardName.equals("Resuscitate") ||
            cardName.equals("Righteous Charge") ||
            cardName.equals("Scare Tactics") ||
            cardName.equals("Shield Wall") ||
            cardName.equals("Solidarity") ||
            cardName.equals("Steadfastness") ||
            cardName.equals("Tortoise Formation") ||
            cardName.equals("Tromp the Domains") ||
            cardName.equals("Valorous Charge") ||
            cardName.equals("Virtuous Charge") ||
            cardName.equals("Vitalizing Wind") ||
            cardName.equals("Warrior's Charge") ||
            cardName.equals("Warrior's Honor"))
   {
     SpellAbility spell = new Spell(card)
     {
     private static final long serialVersionUID = -5844664906503221006L;

     public boolean canPlayAI()
       {
         return getAttacker() != null;
       }
       public Card getAttacker()
       {
         //target creatures that is going to attack
         Combat c = ComputerUtil.getAttackers();
         Card[] att = c.getAttackers();
         if(cardName.equals("Dance of Shadows") ||
            cardName.equals("Overrun") ||
            cardName.equals("Tromp the Domains"))
         {
             if (att.length > 2)   // Effect best used with a few creatures
                 return att[0];
             else
                 return null;
         }
         if(cardName.equals("Burst of Speed") ||
           cardName.equals("Chorus of Woe") ||
            cardName.equals("Desperate Charge") ||
            cardName.equals("Glorious Charge") ||
            cardName.equals("Kjeldoran War Cry") ||
            cardName.equals("Magnify") ||
            cardName.equals("Nature's Cloak") ||
            cardName.equals("Nocturnal Raid") ||
            cardName.equals("Path of Anger's Flame") ||
            cardName.equals("Resuscitate") ||
            cardName.equals("Righteous Charge") ||
            cardName.equals("Scare Tactics") ||
            cardName.equals("Shield Wall") ||
            cardName.equals("Solidarity") ||
            cardName.equals("Steadfastness") ||
            cardName.equals("Tortoise Formation") ||
            cardName.equals("Valorous Charge") ||
            cardName.equals("Vitalizing Wind") ||
            cardName.equals("Virtuous Charge") ||
            cardName.equals("Warrior's Charge") ||
            cardName.equals("Warrior's Honor"))
         {
             if (att.length > 1)   // Effect best used on at least a couple creatures
                 return att[0];
             else
                 return null;
         }
         return null;
       }//getAttacker()

       int getPowerBoost()
       {
         if(cardName.equals("Tromp the Domains"))
             return getTtDBoost();

         if(cardName.equals("Kjeldoran War Cry"))
             return getKWCBoost();

         if(cardName.equals("Chorus of Woe") ||
            cardName.equals("Dance of Shadows") ||
            cardName.equals("Glorious Charge") ||
            cardName.equals("Magnify") ||
            cardName.equals("Scare Tactics") ||
            cardName.equals("Virtuous Charge") ||
            cardName.equals("Warrior's Charge") ||
            cardName.equals("Warrior's Honor"))
             return 1;

         if(cardName.equals("Desperate Charge") ||
            cardName.equals("Nocturnal Raid") ||
            cardName.equals("Path of Anger's Flame") ||
            cardName.equals("Righteous Charge") ||
            cardName.equals("Valorous Charge"))
             return 2;

         if(cardName.equals("Overrun"))
             return 3;

         if(cardName.equals("Vitalizing Wind"))
             return 7;

         return 0;
       }//getPowerBoost()

       int getToughBoost()
       {
         if(cardName.equals("Tromp the Domains"))
             return getTtDBoost();

         if(cardName.equals("Kjeldoran War Cry"))
             return getKWCBoost();

         if(cardName.equals("Glorious Charge") ||
           cardName.equals("Magnify") ||
            cardName.equals("Virtuous Charge") ||
            cardName.equals("Warrior's Charge") ||
            cardName.equals("Warrior's Honor"))
             return 1;

         if(cardName.equals("Righteous Charge") ||
            cardName.equals("Shield Wall"))
             return 2;

         if(cardName.equals("Overrun") ||
            cardName.equals("Steadfastness"))
             return 3;

         if(cardName.equals("Solidarity"))
             return 5;

         if(cardName.equals("Vitalizing Wind"))
             return 7;

         return 0;
       }//getToughBoost()

       String getKeywordBoost()
       {
          if(cardName.equals("Burst of Speed"))
              return "Haste";
          
           if(cardName.equals("Overrun") ||
              cardName.equals("Tromp the Domains"))
               return "Trample";

           if(cardName.equals("Dance of Shadows"))
               return "Fear";
          
           if(cardName.equals("Nature's Cloak"))
              return "Forestwalk";
          
           if(cardName.equals("Resuscitate"))
              return "RegenerateMe:1";
          
           if(cardName.equals("Tortoise Formation"))
              return "Shroud";
          
           return "None";
       }//getKeywordBoost()

       int getTtDBoost()     // Tromp the Domains - +1/+1 for each basic land you control
       {
           PlayerZone play = AllZone.getZone(Constant.Zone.Play, card.getController());
           CardList land = new CardList(play.getCards());

           String basic[] = {"Forest", "Plains", "Mountain", "Island", "Swamp"};
           int count = 0;

           for (int i = 0; i < basic.length; i++)
           {
               CardList c = land.getType(basic[i]);
               if (!c.isEmpty())
                   count++;
           }
           return count;
       }//getTtDBoost

       int getKWCBoost()     // Kjeldoran War Cry - +X/+X, X = 1 + Num(KWC in All Graveyards)
       {
           PlayerZone hYard = AllZone.getZone(Constant.Zone.Graveyard, Constant.Player.Human);
           PlayerZone cYard = AllZone.getZone(Constant.Zone.Graveyard, Constant.Player.Computer);
           CardList allYards = new CardList();

           allYards.addAll(hYard.getCards());
           allYards.addAll(cYard.getCards());
           allYards = allYards.getName("Kjeldoran War Cry");

           return allYards.size() + 1;
       }//getKWCBoost

       public void resolve()
       {

           final int pboost = getPowerBoost();
           final int tboost = getToughBoost();
           final String kboost = getKeywordBoost();

           CardList list = new CardList();

           if (cardName.equals("Burst of Speed") || // Creatures "you" Control
              cardName.equals("Chorus of Woe") ||
               cardName.equals("Dance of Shadows") ||
               cardName.equals("Desperate Charge") ||
               cardName.equals("Glorious Charge") ||
               cardName.equals("Kjeldoran War Cry") ||
               cardName.equals("Nature's Cloak") ||
               cardName.equals("Overrun") ||
               cardName.equals("Path of Anger's Flame") ||
               cardName.equals("Resuscitate") ||
               cardName.equals("Righteous Charge") ||
               cardName.equals("Scare Tactics") ||
               cardName.equals("Shield Wall") ||
               cardName.equals("Solidarity") ||
               cardName.equals("Steadfastness") ||
               cardName.equals("Tortoise Formation") ||
               cardName.equals("Virtuous Charge") ||
               cardName.equals("Vitalizing Wind") ||
               cardName.equals("Warrior's Charge") ||
               cardName.equals("Warrior's Honor"))
           {
               PlayerZone play = AllZone.getZone(Constant.Zone.Play, card.getController());
               list.addAll(play.getCards());
              
               if(cardName.equals("Nature's Cloak"))
                  list = list.getColor("G");
           }

           if (cardName.equals("Magnify") || // All Creatures in Play
               cardName.equals("Nocturnal Raid") || // All Black Creatures in Play
               cardName.equals("Valorous Charge")) // All White Creatures in Play
           {
               PlayerZone human = AllZone.getZone(Constant.Zone.Play, Constant.Player.Human);
               PlayerZone comp = AllZone.getZone(Constant.Zone.Play, Constant.Player.Computer);

               list.addAll(human.getCards());
               list.addAll(comp.getCards());

               if(cardName.equals("Nocturnal Raid"))
                   list = list.getColor("B");

               if(cardName.equals("Valorous Charge"))
                   list = list.getColor("W");
           }

         for(int i = 0; i < list.size(); i++)
         {
           final Card[] target = new Card[1];
           target[0] = list.get(i);

           final Command untilEOT = new Command()
           {
          private static final long serialVersionUID = 5063161656920609389L;

          public void execute()
             {
               if(AllZone.GameAction.isCardInPlay(target[0]))
               {
                 target[0].addTempAttackBoost(-pboost);
                 target[0].addTempDefenseBoost(-tboost);

                 if(!kboost.equals("None"))
                     target[0].removeExtrinsicKeyword(kboost);
               }
             }
           };//Command

           if(AllZone.GameAction.isCardInPlay(target[0]))
           {
             target[0].addTempAttackBoost(pboost);
             target[0].addTempDefenseBoost(tboost);

             if(!kboost.equals("None"))
                 target[0].addExtrinsicKeyword(kboost);

             AllZone.EndOfTurn.addUntil(untilEOT);
           }//if
         }//for
       }//resolve()
     };
     card.clearSpellAbility();
     card.addSpellAbility(spell);
   }//*************** END ************ END **************************
    

/*
    //contributed code
    //*************** START *********** START **************************
    if(cardName.equals("Dark Banishing"))
    {
      final SpellAbility spell = new Spell(card)
      {
		private static final long serialVersionUID = -5621665629586583879L;
		
		Card check;

        public boolean canPlayAI()
        {
          check = getNonBlackCreature();
          return check != null;
        }

        public void chooseTargetAI()
        {
          Card c = getNonBlackCreature();
          if((c == null) || (! check.equals(c)))
            throw new RuntimeException(card +" error in chooseTargetAI() - Card c is " +c +",  Card check is " +check);

          setTargetCard(c);
        }//chooseTargetAI()

        Card getNonBlackCreature()
        {
          int maxAttack = 0;
          Card bestCard = null;
          CardList nonBlackCards = CardFactoryUtil.AI_getHumanCreature(card, true);

          for(int i = 0; i < nonBlackCards.size(); i++)
            if(!CardUtil.getColors(nonBlackCards.get(i)).contains(Constant.Color.Black))
              if(nonBlackCards.get (i).getNetAttack() > maxAttack)
          {
            maxAttack = nonBlackCards.get(i).getNetAttack();
            bestCard = nonBlackCards.get(i);
          }

          return bestCard;
        }

        public void resolve()
        {
          if(AllZone.GameAction.isCardInPlay(getTargetCard()) && CardFactoryUtil.canTarget(card,getTargetCard()))
          {
            AllZone.GameAction.destroyNoRegeneration(getTargetCard());
          }
        }//resolve()
      };//SpellAbility
      card.clearSpellAbility();
      card.addSpellAbility(spell);

      //target
      Input target = new Input()
      {
		private static final long serialVersionUID = -3155643868640376193L;
		
		public void showMessage()
        {
          AllZone.Display.showMessage("Select target non-black creature for " +spell.getSourceCard());
          ButtonUtil.enableOnlyCancel();
        }
        public void selectButtonCancel() {stop();}
        public void selectCard(Card c, PlayerZone zone)
        {
          if (!CardFactoryUtil.canTarget(card, c))
          {
        	  AllZone.Display.showMessage("Cannot target this card (Shroud? Protection?).");
          }
          else if((!CardUtil.getColors(c).contains(Constant.Color.Black))
             && c.isCreature()
             && zone.is(Constant.Zone.Play))
          {
            spell.setTargetCard(c);
            stopSetNext(new Input_PayManaCost(spell));
          }
        }
      };//SpellAbility - target

      spell.setBeforePayMana(target);
    }//*************** END ************ END **************************
*/
   

    //*************** START *********** START **************************
    else if(cardName.equals("Pyrohemia"))
    {
      SpellAbility ability = new Ability(card, "R")
      {
        public boolean canPlayAI()
        {
          CardList human    = new CardList(AllZone.Human_Play.getCards());
          CardList computer = new CardList(AllZone.Computer_Play.getCards());

          human = human.getType("Creature");
          computer = computer.getType("Creature");

          return AllZone.Computer_Life.getLife() > 2 && !(human.size() == 0 && 0 < computer.size());
        }
        public void resolve()
        {
          //get all creatures
          CardList list = new CardList();
          list.addAll(AllZone.Human_Play.getCards());
          list.addAll(AllZone.Computer_Play.getCards());
          list = list.getType("Creature");

          for(int i = 0; i < list.size(); i++){
        	  if (CardFactoryUtil.canDamage(card, list.get(i)))
        		  list.get(i).addDamage(1, card);
          }
          
          AllZone.Human_Life.subtractLife(1);
          AllZone.Computer_Life.subtractLife(1);
        }//resolve()
      };//SpellAbility
      ability.setDescription("R: Pyrohemia deals 1 damage to each creature and each player.");
      ability.setStackDescription(card +" deals 1 damage to each creature and each player.");

      card.clearSpellAbility();
     card.addSpellAbility(new Spell_Permanent(card)
     {
	   private static final long serialVersionUID = 5596915641671666843L;

	   public boolean canPlayAI()
       {
         //get all creatures
         CardList list = new CardList();
         list.addAll(AllZone.Human_Play.getCards());
         list.addAll(AllZone.Computer_Play.getCards());
         list = list.getType("Creature");

         return 0 < list.size();
       }
      });

      card.addSpellAbility(ability);
    }
    //*************** END ************ END **************************


    //*************** START *********** START **************************
    else if(cardName.equals("Pestilence"))
    {
      SpellAbility ability = new Ability(card, "B")
      {
        public boolean canPlayAI()
        {
          CardList human    = new CardList(AllZone.Human_Play.getCards());
          CardList computer = new CardList(AllZone.Computer_Play.getCards());

          human = human.getType("Creature");
          computer = computer.getType("Creature");

          return AllZone.Computer_Life.getLife() > 2 && !(human.size() == 0 && 0 < computer.size());
        }
        public void resolve()
        {
          //get all creatures
          CardList list = new CardList();
          list.addAll(AllZone.Human_Play.getCards());
          list.addAll(AllZone.Computer_Play.getCards());
          list = list.getType("Creature");

          for(int i = 0; i < list.size(); i++) {
        	  if (CardFactoryUtil.canDamage(card, list.get(i)))
        		  list.get(i).addDamage(1, card);
          }
        	  
          AllZone.Human_Life.subtractLife(1);
          AllZone.Computer_Life.subtractLife(1);
        }//resolve()
      };//SpellAbility
      ability.setDescription("B: Pestilence deals 1 damage to each creature and each player.");
      ability.setStackDescription(card +" deals 1 damage to each creature and each player.");

      card.clearSpellAbility();
      card.addSpellAbility(new Spell_Permanent(card)
      {
		private static final long serialVersionUID = -4163089323122672307L;

		public boolean canPlayAI()
        {
          //get all creatures
          CardList list = new CardList();
          list.addAll(AllZone.Human_Play.getCards());
          list.addAll(AllZone.Computer_Play.getCards());
          list = list.getType("Creature");

          return 0 < list.size();
        }
      });

      card.addSpellAbility(ability);
    }
    //*************** END ************ END **************************

    //*************** START *********** START **************************
    else if(cardName.equals("Bridge from Below"))
    {
      SpellAbility spell = new Spell_Permanent(card)
      {
		private static final long serialVersionUID = 7254358703158629514L;

		public boolean canPlayAI() { return false; }
      };
      card.clearSpellAbility();
      card.addSpellAbility(spell);
	 }
//*************** END ************ END *************************

    //*************** START *********** START **************************
    else if(cardName.equals("Nevinyrral's Disk"))
    {
      SpellAbility summoningSpell = new Spell_Permanent(card)
      {
		private static final long serialVersionUID = -8859376851358601934L;

		public boolean canPlayAI()
        {
          boolean nevinyrralInPlay = false;

          CardList inPlay = new CardList();
          inPlay.addAll(AllZone.Computer_Play.getCards());
          for(int i=0; i<inPlay.size(); ++i)
          {
            if( inPlay.getCard(i).getName().equals("Nevinyrral's Disk"))
            {
              nevinyrralInPlay = true;
            }
          }
          return ! nevinyrralInPlay && (0 < CardFactoryUtil.AI_getHumanCreature(card, false).size());
        }
      };
      card.clearSpellAbility();
      card.addSpellAbility(summoningSpell);

      card.addComesIntoPlayCommand(new Command()
      {
		private static final long serialVersionUID = -2504426622672629123L;

		public void execute()
        {
          card.tap();
        }
      });
      final SpellAbility ability = new Ability_Tap(card, "1")
      {
		private static final long serialVersionUID = 4175577092552330100L;
		
		public void resolve()
        {
          CardList all = new CardList();
          all.addAll(AllZone.Human_Play.getCards());
          all.addAll(AllZone.Computer_Play.getCards());
          all = filter(all);

          for(int i = 0; i < all.size(); i++)
            AllZone.GameAction.destroy(all.get(i));
        }
        private CardList filter(CardList list)
        {
          return list.filter(new CardListFilter()
          {
            public boolean addCard(Card c)
            {
              return c.isArtifact() || c.isCreature() || c.isEnchantment();
            }
          });
        }//filter()
        public boolean canPlayAI()
        {
          CardList human    = new CardList(AllZone.Human_Play.getCards());
          CardList computer = new CardList(AllZone.Computer_Play.getCards());

          human    = human.getType("Creature");
          computer = computer.getType("Creature");

          //the computer will at least destroy 2 more human creatures
          return computer.size() < human.size()-1 || AllZone.Computer_Life.getLife() < 7;
        }
      };//SpellAbility
      card.addSpellAbility(ability);
      ability.setDescription("1, tap: Destroy all artifacts, creatures, and enchantments.");
      ability.setStackDescription("Destroy all artifacts, creatures, and enchantments.");
    }//*************** END ************ END **************************




    //*************** START *********** START **************************
    else if(cardName.equals("Tanglebloom"))
    {
      final SpellAbility a1 = new Ability_Tap(card, "1")
      {
		private static final long serialVersionUID = -6395076857898740906L;
		
		public boolean canPlayAI() {return AllZone.Phase.getPhase().equals(Constant.Phase.Main2);}
        public void resolve()
        {
          AllZone.GameAction.getPlayerLife(card.getController()).addLife(1);
        }
      };//SpellAbility
      card.addSpellAbility(a1);
      a1.setDescription("1, tap: You gain 1 life.");
      a1.setStackDescription("Tanglebloom - " +card.getController() +" gains 1 life.");

      a1.setBeforePayMana(new Input_PayManaCost(a1));
    }//*************** END ************ END **************************
    
  
    
  //*************** START *********** START **************************
    else if(cardName.equals("Conspiracy") || cardName.equals("Cover of Darkness") || cardName.equals("Door of Destinies") ||
       cardName.equals("Engineered Plague") || cardName.equals("Shared Triumph") || cardName.equals("Belbe's Portal") ||
       cardName.equals("Steely Resolve") )
    {
    	final String[] input = new String[1];
    	final String player = card.getController();
    	
    	final SpellAbility ability = new Ability(card, "0")
    	{
    		public void resolve()
    		{
    			if (player.equals(Constant.Player.Human))
    			{
	    			input[0] =  (String)JOptionPane.showInputDialog(null, "Which creature type?", "Pick type", JOptionPane.QUESTION_MESSAGE);
	    			
	    			if (input[0].equals("Legendary") || input[0].equals("Artifact") || input[0].equals("Enchantment") || input[0].equals("Shrine") || input[0].equals("Creature"))
	    				input[0] = "";
	    			//TODO: some more input validation, case-sensitivity, etc.
	    			
	    			input[0] = input[0].trim(); //this is to prevent "cheating", and selecting multiple creature types,eg "Goblin Soldier"
    			}
    			else
    			{
    				String chosenType = CardFactoryUtil.chooseCreatureTypeAI(card);
    				if (!chosenType.equals(""))
    					input[0] = chosenType;
    				else 
    					input[0] = "Sliver"; //what to put here for the AI???
    			}
    			
    			card.setChosenType(input[0]);
    		}
    	};//ability
    	Command intoPlay = new Command()
        {
		  private static final long serialVersionUID = 5634360316643996274L;

		  public void execute()
          {
            ability.setStackDescription("When " + card.getName() + " comes into play, choose a creature type.");
            AllZone.Stack.add(ability);
          }
        };
        card.addComesIntoPlayCommand(intoPlay);
     
    	
    }//*************** END ************ END **************************
    
  //*************** START *********** START **************************
    else if(cardName.equals("Sarpadian Empires, Vol. VII"))
    {
    	
    	final String[] choices = { 
    			 "Citizen", 
    			 "Camarid",
    			 "Thrull",
    			 "Goblin",
    			 "Saproling"
    	 };
    	 
    	final String player = card.getController();
    	
    	final SpellAbility ability = new Ability(card, "0")
    	{
    		public void resolve()
    		{
    			String type = "";
    			String imageName = "";
    			String color = "";
    			
    			if (player.equals(Constant.Player.Computer))
    			{
    				type = "Thrull";
    				imageName = "B 1 1 Thrull";
    				color = "B";
    			}
    			else if (player.equals(Constant.Player.Human))
    			{
    				Object q = AllZone.Display.getChoiceOptional("Select type of creature", choices);
		        	if (q.equals("Citizen"))
		        	{
		        		type = "Citizen";
		        		imageName = "W 1 1 Citizen";
		        		color = "W";
		        	}
		        	else if(q.equals("Camarid"))
		        	{
		        		type = "Camarid";
		        		imageName = "U 1 1 Camarid";
		        		color = "U";
		        	}
		        	else if(q.equals("Thrull"))
		        	{
		        		type = "Thrull";
		        		imageName = "B 1 1 Thrull";
		        		color = "B";
		        	}
		        	else if(q.equals("Goblin"))
		        	{
		        		type = "Goblin";
		        		imageName = "R 1 1 Goblin";
		        		color = "R";
		        	}
		        	else if(q.equals("Saproling"))
		        	{
		        		type = "Saproling";
		        		imageName = "G 1 1 Saproling";
		        		color = "G";
		        	}
    			}
    			card.setChosenType(type);
    			
    			final String t = type;
    			final String in = imageName;
    			final String col = color;
    			//card.setChosenType(input[0]);
    			
    			final Ability_Tap a1 = new Ability_Tap(card, "3")
    			{
    				
					private static final long serialVersionUID = -2114111483117171609L;

					public void resolve() {
						Card c = new Card();

				        c.setOwner(card.getController());
				        c.setController(card.getController());

				        c.setName(t);
				        c.setImageName(in);
				        c.setManaCost(col);
				        c.setToken(true);
				        
				        c.addType("Creature");
				        c.addType(t);
				        
				        c.setBaseAttack(1);
				        c.setBaseDefense(1);

				        PlayerZone play = AllZone.getZone(Constant.Zone.Play, card.getController());
				        play.add(c);
					}
					
    			};
    			a1.setDescription("3, Tap: Put a 1/1 creature token of the chosen color and type onto the battlefield.");
    			a1.setStackDescription(card.getName() + " - " +  card.getController() + " puts a 1/1" + t + " token into play");
    			card.addSpellAbility(a1);
    		}
    	};//ability
    	Command intoPlay = new Command()
        {
			private static final long serialVersionUID = 7202704600935499188L;
	
			public void execute()
	          {
	            ability.setStackDescription("As Sarpadian Empires, Vol. VII enters the battlefield, choose white Citizen, blue Camarid, black Thrull, red Goblin, or green Saproling.");
	            AllZone.Stack.add(ability);
	          }
        };
        card.addComesIntoPlayCommand(intoPlay);
    	
    }//*************** END ************ END **************************
    		

 

    //*************** START *********** START **************************
    else if(cardName.equals("Timetwister"))
    {
      final SpellAbility spell = new Spell(card)
      {
		private static final long serialVersionUID = 505983020365091226L;
		
		public void resolve()
        {
          discardDraw7(Constant.Player.Human);
          discardDraw7(Constant.Player.Computer);
        }//resolve()

        void discardDraw7(String player)
        {
          // Discard hand into graveyard
          PlayerZone hand = AllZone.getZone(Constant.Zone.Hand, player);
          Card[] c = hand.getCards();
          for(int i = 0; i < c.length; i++)
            AllZone.GameAction.discard(c[i]);

          // Move graveyard into library
          PlayerZone grave = AllZone.getZone(Constant.Zone.Graveyard, player);
          PlayerZone library = AllZone.getZone(Constant.Zone.Library, player);
          Card[] g = grave.getCards();
          for (int i = 0; i < g.length; i++)
          {
            grave.remove(g[i]);
            library.add(g[i],0);
          }

          // Shuffle library
          AllZone.GameAction.shuffle(player);

          // Draw seven cards
          for(int i = 0; i < 7; i++)
            AllZone.GameAction.drawCard(player);
          
          if (card.getController().equals(player)) {
        	  library.remove(card);
        	  grave.add(card);
          }
        }

        // Simple, If computer has two or less playable cards remaining in hand play Timetwister
        public boolean canPlayAI()
        {
          Card[] c = removeLand(AllZone.Computer_Hand.getCards());
          return 2 >= c.length;
        }
        Card[] removeLand(Card[] in)
        {
          CardList c = new CardList(in);
          c = c.filter(new CardListFilter()
          {
            public boolean addCard(Card c)
            {
              return !c.isLand();
            }
          });
          return c.toArray();
        }//removeLand()

      };//SpellAbility
      card.clearSpellAbility();
      card.addSpellAbility(spell);
    }//*************** END ************ END **************************


    //*************** START *********** START **************************
    else if(cardName.equals("Dragon Roost"))
    {
      final SpellAbility ability = new Ability(card, "5 R R")
      {
        public void resolve()
        {
          Card c = new Card();
          
          c.setName("Dragon");
          c.setImageName("R 5 5 Dragon");
          c.setOwner(card.getController());
          c.setController(card.getController());

          c.setManaCost("R");
          c.setToken(true);

          c.addType("Creature");
          c.addType("Dragon");
          c.setBaseAttack(5);
          c.setBaseDefense(5);
          c.addIntrinsicKeyword("Flying");

          PlayerZone play = AllZone.getZone(Constant.Zone.Play, card.getController());
          play.add(c);
        }//resolve()
      };
      ability.setDescription("5RR: Put a 5/5 red Dragon creature token with flying into play.");
      ability.setStackDescription("Dragon Roost - Put a 5/5 red Dragon creature token with flying into play.");
      card.addSpellAbility(ability);
    }//*************** END ************ END **************************



    //*************** START *********** START **************************
    else if(cardName.equals("The Hive"))
    {
      final SpellAbility ability = new Ability_Tap(card, "5")
      {
		private static final long serialVersionUID = -1091111822316858416L;

		public void resolve()
        {
          Card c = new Card();
          c.setName("Wasp");
          c.setImageName("C 1 1 Wasp");

          c.setOwner(card.getController());
          c.setController(card.getController());

          c.setManaCost("");
          c.setToken(true);

          c.addType("Artifact");
          c.addType("Creature");
          c.addType("Insect");

          c.setBaseAttack(1);
          c.setBaseDefense(1);
          c.addIntrinsicKeyword("Flying");

          PlayerZone play = AllZone.getZone(Constant.Zone.Play, card.getController());
          play.add(c);
        }//resolve()
      };
      ability.setDescription("5, tap: Put a 1/1 Insect artifact creature token with flying named Wasp into play.");
      ability.setStackDescription("The Hive - Put a 1/1 token with flying into play.");
      card.addSpellAbility(ability);
    }//*************** END ************ END **************************

    //*************** START *********** START **************************
    else if(cardName.equals("Mobilization"))
    {
    	final SpellAbility ability = new Ability(card, "2 W")
        {
          public void resolve()
          {
            Card c = new Card();
            c.setName("Soldier");
            c.setImageName("W 1 1 Soldier");

            c.setOwner(card.getController());
            c.setController(card.getController());

            c.setManaCost("W");
            c.setToken(true);

            c.addType("Creature");
            c.addType("Soldier");

            c.setBaseAttack(1);
            c.setBaseDefense(1);

            PlayerZone play = AllZone.getZone(Constant.Zone.Play, card.getController());
            play.add(c);
          }//resolve()
        };
        ability.setDescription("2W: Put a 1/1 white Soldier creature token into play.");
        ability.setStackDescription("Mobilization - Put a 1/1 Soldier token into play.");
        card.addSpellAbility(ability);
    	
    	
    }//*************** END ************ END **************************
    
  
    
    //*************** START *********** START **************************
    else if(cardName.equals("Centaur Glade"))
    {
      final SpellAbility ability = new Ability(card, "2 G G")
      {
        public void resolve()
        {
          Card c = new Card();
          c.setName("Centaur");
          c.setImageName("G 3 3 Centaur");

          c.setOwner(card.getController());
          c.setController(card.getController());

          c.setManaCost("G");
          c.setToken(true);

          c.addType("Creature");
          c.addType("Centaur");

          c.setBaseAttack(3);
          c.setBaseDefense(3);

          PlayerZone play = AllZone.getZone(Constant.Zone.Play, card.getController());
          play.add(c);
        }//resolve()
      };
      ability.setDescription("2GG: Put a 3/3 green Centaur creature token into play.");
      ability.setStackDescription("Centaur Glade - Put a 3/3 token into play.");
      card.addSpellAbility(ability);
    }//*************** END ************ END **************************



    //*************** START *********** START **************************
    else if(cardName.equals("Pongify"))
    {
      final SpellAbility spell = new Spell(card)
      {
		private static final long serialVersionUID = -7657135492744577568L;
		
		public boolean canPlayAI()
        {
          return (getCreature().size() != 0) && (AllZone.Phase.getTurn() > 4);
        }
        public void chooseTargetAI()
        {
          Card best = CardFactoryUtil.AI_getBestCreature(getCreature());
          setTargetCard(best);
        }
        CardList getCreature()
        {
          CardList list = CardFactoryUtil.AI_getHumanCreature(card, true);
          list = list.filter(new CardListFilter()
          {
            public boolean addCard(Card c)
            {
              return (3 < c.getNetAttack());
            }
          });
          return list;
        }//getCreature()
        public void resolve()
        {
          if(AllZone.GameAction.isCardInPlay(getTargetCard()) && CardFactoryUtil.canTarget(card,getTargetCard()) )
          {
            PlayerZone play = AllZone.getZone(getTargetCard());
            makeToken(play, getTargetCard().getController());

            AllZone.GameAction.destroyNoRegeneration(getTargetCard());
          }
        }//resolve()
        void makeToken(PlayerZone play, String controller)
        {
          Card c = new Card();

          c.setOwner(card.getController());
          c.setController(controller);

          c.setName("Ape");
          c.setImageName("G 3 3 Ape");
          c.setManaCost("G");
          c.setToken(true);

          c.addType("Creature");
          c.addType("Ape");
          c.setBaseAttack(3);
          c.setBaseDefense(3);

          play.add(c);
        }
      };//SpellAbility

      card.clearSpellAbility();
      card.addSpellAbility(spell);

      spell.setBeforePayMana(CardFactoryUtil.input_targetCreature(spell));
    }//*************** END ************ END **************************



    //*************** START *********** START **************************
    else if(cardName.equals("Devour in Shadow"))
    {
      final SpellAbility spell = new Spell(card)
      {
		private static final long serialVersionUID = 215990562522519924L;
		
		public boolean canPlayAI()
        {
          return (getCreature().size() != 0) && (AllZone.Phase.getTurn() > 4);
        }
        public void chooseTargetAI()
        {
          Card best = CardFactoryUtil.AI_getBestCreature(getCreature());
          setTargetCard(best);

          if(AllZone.Computer_Life.getLife() <= best.getNetDefense())
          {
            CardList human = CardFactoryUtil.AI_getHumanCreature(AllZone.Computer_Life.getLife() - 1, card, true);
            CardListUtil.sortAttack(human);

            if(0 < human.size())
              setTargetCard(human.get(0));
          }
        }
        CardList getCreature()
        {
          return CardFactoryUtil.AI_getHumanCreature(card, true);
        }//getCreature()
        public void resolve()
        {
          if (AllZone.GameAction.isCardInPlay(getTargetCard()) && CardFactoryUtil.canTarget(card, getTargetCard()))
          {
	          AllZone.GameAction.destroyNoRegeneration(getTargetCard());
	
	          PlayerLife life = AllZone.GameAction.getPlayerLife(card.getController());
	          life.subtractLife(getTargetCard().getNetDefense());
          }
        }//resolve()
      };

      card.clearSpellAbility();
      spell.setBeforePayMana(CardFactoryUtil.input_targetCreature(spell));
      card.addSpellAbility(spell);
    }//*************** END ************ END **************************
    
    
/*
  //*************** START *********** START **************************
    else if(cardName.equals("Terminate"))
    {
      final SpellAbility spell = new Spell(card)
      {
		private static final long serialVersionUID = 3334967250557638367L;
		
		public boolean canPlayAI()
        {
          return (getCreature().size() != 0) && (AllZone.Phase.getTurn() > 4);
        }
        public void chooseTargetAI()
        {
          Card best = CardFactoryUtil.AI_getBestCreature(getCreature());
          setTargetCard(best);

          CardList human = CardFactoryUtil.AI_getHumanCreature(AllZone.Computer_Life.getLife() - 1, card, true);
          CardListUtil.sortAttack(human);

          if(0 < human.size())
            setTargetCard(human.get(0));
          
        }
        CardList getCreature()
        {
          return CardFactoryUtil.AI_getHumanCreature(card, true);
        }//getCreature()
        public void resolve()
        {
          if (AllZone.GameAction.isCardInPlay(getTargetCard()) && CardFactoryUtil.canTarget(card, getTargetCard()))
        	  AllZone.GameAction.destroyNoRegeneration(getTargetCard());
        }//resolve()
      };

      card.clearSpellAbility();
      spell.setBeforePayMana(CardFactoryUtil.input_targetCreature(spell));
      card.addSpellAbility(spell);
    }//*************** END ************ END **************************
*/
    
/*    
    //*************** START *********** START **************************
    else if(cardName.equals("Kinsbaile Borderguard"))
    {
    	final SpellAbility ability = new Ability(card, "0")
        {
          public void resolve()
          {
             card.addCounter(Counters.P1P1, countKithkin());
             //System.out.println("all counters: " +card.sumAllCounters());
          }//resolve()
          
          public int countKithkin()
          {
        	  PlayerZone play = AllZone.getZone(Constant.Zone.Play, card.getController());
        	  CardList kithkin = new CardList(play.getCards());
        	  kithkin = kithkin.filter(new CardListFilter()        	  
        	  {

				public boolean addCard(Card c)
				{
					return (c.getType().contains("Kithkin") || c.getKeyword().contains("Changeling"))&& !c.equals(card);
				}
        		  
        	  });
        	  return kithkin.size();
        	  
          }
          @SuppressWarnings("unused") // makeToken
		  public void makeToken()
          {
        	  Card c = new Card();

              c.setOwner(card.getController());
              c.setController(card.getController());

              c.setName("Kithkin Soldier");
              c.setImageName("W 1 1 Kithkin Soldier");
              c.setManaCost("W");
              c.setToken(true);

              c.addType("Creature");
              c.addType("Kithkin");
              c.addType("Soldier");

              c.setBaseAttack(1);
              c.setBaseDefense(1);

              PlayerZone play = AllZone.getZone(Constant.Zone.Play, card.getController());
              play.add(c);
          }
        };
        Command intoPlay = new Command()
        {
		  private static final long serialVersionUID = -7067218066522935060L;

		  public void execute()
          {
            ability.setStackDescription("Kinsbaile Borderguard comes into play with a +1/+1 counter on it for each other Kithkin you control.");
            AllZone.Stack.add(ability);
          }
        };
        
        final SpellAbility ability2 = new Ability(card, "0")
        {
          public void resolve()
          {
             for (int i=0;i<card.sumAllCounters();i++)
             {
            	 makeToken();
             }
          }//resolve()
          
          public void makeToken()
          {
        	  Card c = new Card();

              c.setOwner(card.getController());
              c.setController(card.getController());

              c.setName("Kithkin Soldier");
              c.setImageName("W 1 1 Kithkin Soldier");
              c.setManaCost("W");
              c.setToken(true);

              c.addType("Creature");
              c.addType("Kithkin");
              c.addType("Soldier");

              c.setBaseAttack(1);
              c.setBaseDefense(1);

              PlayerZone play = AllZone.getZone(Constant.Zone.Play, card.getController());
              play.add(c);
          }
          
        };
        
        Command destroy = new Command()
        {
		  private static final long serialVersionUID = 304026662487997331L;

		  public void execute()
          {
            ability2.setStackDescription("When Kinsbaile Borderguard is put into a graveyard from play, put a 1/1 white Kithkin Soldier creature token into play for each counter on it.");
            AllZone.Stack.add(ability2);
          }
        };     
        
        card.addComesIntoPlayCommand(intoPlay);
        card.addDestroyCommand(destroy);
    	
    }//*************** END ************ END **************************
*/

    
  //*************** START *********** START **************************
    else if(cardName.equals("Oblivion Ring"))
    {
      final CommandReturn getPerm = new CommandReturn()
      {
        public Object execute()
        {
          //get all creatures
          CardList tempList = new CardList();
          tempList.addAll(AllZone.Human_Play.getCards());
          tempList.addAll(AllZone.Computer_Play.getCards());
          
          CardList list = new CardList();
          
          for(int i=0;i < tempList.size(); i++)
          {
        	  if(tempList.get(i).isPermanent() && !tempList.get(i).isLand() && CardFactoryUtil.canTarget(card, tempList.get(i)) )
        		  list.add(tempList.get(i));
          }

          //remove "this card"
          list.remove(card);

          return list;
        }
      };//CommandReturn

      final SpellAbility abilityComes = new Ability(card, "0")
      {
        public void resolve()
        {
          if(AllZone.GameAction.isCardInPlay(getTargetCard()) && CardFactoryUtil.canTarget(card,getTargetCard()) )
          {
            AllZone.GameAction.removeFromGame(getTargetCard());
          }
        }//resolve()
      };

      final Input inputComes = new Input()
      {
		private static final long serialVersionUID = -3613946694360326887L;

		public void showMessage()
        {
          CardList choice = (CardList)getPerm.execute();

          stopSetNext(CardFactoryUtil.input_targetSpecific(abilityComes, choice, "Select target permanent to remove from the game", true));
          ButtonUtil.disableAll();//to disable the Cancel button
        }
      };
      Command commandComes = new Command()
      {
		private static final long serialVersionUID = -6250376920501373535L;

		public void execute()
        {
          CardList perm = (CardList)getPerm.execute();
          String s = card.getController();
          if(perm.size() == 0)
            return;
          else if(s.equals(Constant.Player.Human))
            AllZone.InputControl.setInput(inputComes);
          else //computer
          {
            Card target;

            //try to target human creature
            CardList human = CardFactoryUtil.AI_getHumanCreature(card, true);
            target = CardFactoryUtil.AI_getBestCreature(human);//returns null if list is empty
            
            // try to target human permanent
            if (target == null)
            {
            	int convertedCost = 0;
            	CardList tempList = new CardList();
                tempList.addAll(AllZone.Human_Play.getCards());
                
                @SuppressWarnings("unused") // list
				CardList list = new CardList();
                for (int i=0;i<tempList.size();i++)
                {
                	if(tempList.get(i).isPermanent() && !tempList.get(i).isLand() && CardFactoryUtil.canTarget(card, tempList.get(i)) &&
                	   (CardUtil.getConvertedManaCost(tempList.get(i).getManaCost()) > convertedCost))
                	{
                		target = tempList.get(i);
                		convertedCost = CardUtil.getConvertedManaCost(tempList.get(i).getManaCost());
                	}
                }
            }
            
            //target something cheaper (manacost 0?) instead:
            if (target == null)
            {
            	CardList humanPerms = new CardList();
            	humanPerms.addAll(AllZone.Human_Play.getCards());
            	humanPerms = humanPerms.filter(new CardListFilter() {
					public boolean addCard(Card c) {
						return c.isPermanent() && !c.isLand() && CardFactoryUtil.canTarget(card, c);
					}
            	});
            	
            	if (humanPerms.size()>0)
            		target = humanPerms.get(0);
            }

            if(target == null)
            {
              //must target computer creature
              CardList computer = new CardList(AllZone.Computer_Play.getCards());
              computer = computer.getType("Creature");
              computer.remove(card);

              computer.shuffle();
              if (computer.size() != 0)
            	  target = computer.get(0);
              else
            	  target = card;
            }
            abilityComes.setTargetCard(target);
            AllZone.Stack.add(abilityComes);
          }//else
        }//execute()
      };//CommandComes
      Command commandLeavesPlay = new Command()
      {
		private static final long serialVersionUID = 6997038208952910355L;

		public void execute()
        {
          Object o = abilityComes.getTargetCard();
          if(o == null || ((Card)o).isToken() || !AllZone.GameAction.isCardRemovedFromGame((Card)o) )
            return;

          SpellAbility ability = new Ability(card, "0")
          {
            public void resolve()
            {
              //copy card to reset card attributes like attack and defense
              Card c = abilityComes.getTargetCard();
              if(! c.isToken())
              {
                c = AllZone.CardFactory.copyCard(c);
                c.setController(c.getOwner());

                PlayerZone play = AllZone.getZone(Constant.Zone.Play, c.getOwner());
                play.add(c);
              }
            }//resolve()
          };//SpellAbility
          ability.setStackDescription("Oblivion Ring - returning permanent to play.");
          AllZone.Stack.add(ability);
        }//execute()
      };//Command

      card.addComesIntoPlayCommand(commandComes);
      card.addLeavesPlayCommand(commandLeavesPlay);
      
      card.clearSpellAbility();
      card.addSpellAbility(new Spell_Permanent(card)
      {
		private static final long serialVersionUID = -3250095291930182087L;

		public boolean canPlayAI()
        {
          Object o = getPerm.execute();
          if (o == null)
        	  return false;
          
          CardList cl = (CardList)getPerm.execute();
          return (o != null) && cl.size() > 0 && AllZone.getZone(getSourceCard()).is(Constant.Zone.Hand);
        }
      });

    }//*************** END ************ END **************************

 

    //*************** START *********** START **************************
    else if(cardName.equals("Oubliette"))
    {
      final SpellAbility enchantment = new Spell(card)
      {
		private static final long serialVersionUID = -6751177094537759827L;

		public void resolve()
    	{
    	  if(AllZone.GameAction.isCardInPlay(getTargetCard()) && CardFactoryUtil.canTarget(card,getTargetCard()))
    	  {
    		AllZone.GameAction.removeFromGame(getTargetCard());

    		//put permanent into play
    		Card c = getSourceCard();
    		AllZone.getZone(Constant.Zone.Play, c.getController()).add(c);
    	  }
    	}//resolve()

    	public boolean canPlayAI()
    	{
    	  //try to target human creature
    	  CardList human = CardFactoryUtil.AI_getHumanCreature(card, true);
    	  Card target = CardFactoryUtil.AI_getBestCreature(human);//returns null if list is empty

    	  if(target == null)
    		return false;
    	  else
    	  {
    		setTargetCard(target);
    		return true;
    	  }
    	}//canPlayAI()
      };//SpellAbility enchantment


      @SuppressWarnings("unused") // target
	final Input target = new Input()
      {
		private static final long serialVersionUID = -251660220889858176L;

		//showMessage() is always the first method called
    	public void showMessage()
    	{
    	  AllZone.Display.showMessage("Select creature to remove from the game (sorry no phasing yet).");
    	  ButtonUtil.enableOnlyCancel();
    	}
    	public void selectButtonCancel() {stop();}

    	public void selectCard(Card c, PlayerZone zone)
    	{
    	  if(!CardFactoryUtil.canTarget(enchantment, c)){
          	  AllZone.Display.showMessage("Cannot target this card (Shroud? Protection?).");
          }	
    	  else if(zone.is(Constant.Zone.Play) && c.isCreature())
    	  {
    		enchantment.setTargetCard(c);

    		stopSetNext(new Input_PayManaCost(enchantment));
    	  }
    	}
      };//Input target

      Command commandLeavesPlay = new Command()
      {
		private static final long serialVersionUID = -2535098005246027777L;

		public void execute()
    	{
    	  Object o = enchantment.getTargetCard();
    	  if(o == null || ((Card)o).isToken() || !AllZone.GameAction.isCardRemovedFromGame((Card)o) )
    		return;

    	  SpellAbility ability = new Ability(card, "0")
    	  {
    		public void resolve()
    		{
    		  //copy card to reset card attributes like attack and defense
    		  Card c = enchantment.getTargetCard();
    		  if(! c.isToken())
    		  {
    			c = AllZone.CardFactory.copyCard(c);
    			c.setController(c.getOwner());

    			PlayerZone play = AllZone.getZone(Constant.Zone.Play, c.getOwner());
    			play.add(c);
    		  }
    		}//resolve()
    	  };//SpellAbility
    	  ability.setStackDescription(card.getName() +" - returning creature to play");
    	  AllZone.Stack.add(ability);
    	}//execute()
      };//Command
      card.addLeavesPlayCommand(commandLeavesPlay);

      card.clearSpellAbility();
      card.addSpellAbility(enchantment);

      enchantment.setBeforePayMana(CardFactoryUtil.input_targetCreature(enchantment));
    }//*************** END ************ END **************************


    //*************** START *********** START **************************
    else if(cardName.equals("Test Destroy"))
    {
      final SpellAbility spell = new Spell(card)
      {
		private static final long serialVersionUID = 6637283804612570910L;
		
		public boolean canPlayAI() {return false;}
		
        public void resolve()
        {
          AllZone.GameAction.destroy(getTargetCard());
        }//resolve()
      };

      card.clearSpellAbility();
      spell.setBeforePayMana(CardFactoryUtil.input_targetType(spell, "All"));
      card.addSpellAbility(spell);
    }//*************** END ************ END **************************


    //*************** START *********** START **************************
    else if(cardName.equals("Take Possession"))
    {
      final SpellAbility spell = new Spell(card)
      {
		private static final long serialVersionUID = -7359291736123492910L;

		public boolean canPlayAI() {return 0 < CardFactoryUtil.AI_getHumanCreature(card, true).size();}

        public void chooseTargetAI()
        {
          Card best = CardFactoryUtil.AI_getBestCreature(CardFactoryUtil.AI_getHumanCreature(card, true));
          setTargetCard(best);
        }

        public void resolve()
        {
          Card c = getTargetCard();
          c.setController(card.getController());

          ((PlayerZone_ComesIntoPlay)AllZone.Human_Play).setTriggers(false);
          ((PlayerZone_ComesIntoPlay)AllZone.Computer_Play).setTriggers(false);

          PlayerZone from = AllZone.getZone(c);
          PlayerZone to = AllZone.getZone(Constant.Zone.Play, card.getController());

          from.remove(c);
          to.add(c);

          ((PlayerZone_ComesIntoPlay)AllZone.Human_Play).setTriggers(true);
          ((PlayerZone_ComesIntoPlay)AllZone.Computer_Play).setTriggers(true);

        }//resolve()
      };

      card.clearSpellAbility();
      spell.setBeforePayMana(CardFactoryUtil.input_targetType(spell, "All"));
      card.addSpellAbility(spell);
    }//*************** END ************ END **************************



    //*************** START *********** START **************************
    else if(cardName.equals("Infest"))
    {
      final SpellAbility spell = new Spell(card)
      {
		private static final long serialVersionUID = -4970294125917784048L;

		public boolean canPlayAI()
        {
          CardList human    = new CardList(AllZone.Human_Play.getCards());
          CardList computer = new CardList(AllZone.Computer_Play.getCards());

          human    = human.getType("Creature");
          computer = computer.getType("Creature");

          human = CardListUtil.filterToughness(human, 2);
          computer = CardListUtil.filterToughness(computer, 2);

          //the computer will at least destroy 2 more human creatures
          return computer.size() < human.size()-1;
        }//canPlayAI()

        public void resolve()
        {
          //get all creatures
          CardList list = new CardList();
          list.addAll(AllZone.Human_Play.getCards());
          list.addAll(AllZone.Computer_Play.getCards());
          list = list.getType("Creature");

          for(int i = 0; i < list.size(); i++)
            list.get(i).addDamage(2, card);
        }//resolve()
      };
      card.clearSpellAbility();
      card.addSpellAbility(spell);
    }//*************** END ************ END **************************


/*
    //*************** START *********** START **************************
    else if(cardName.equals("Angelic Blessing"))
    {
      SpellAbility spell = new Spell(card)
      {
		private static final long serialVersionUID = 6906094867912276636L;
		
		public void resolve()
        {
          final Command eot = new Command()
          {
			private static final long serialVersionUID = 4672991308703961180L;

			public void execute()
            {
              Card c = getTargetCard();
              if(AllZone.GameAction.isCardInPlay(c) )
              {
                c.addTempAttackBoost(-3);
                c.addTempDefenseBoost(-3);
                c.removeExtrinsicKeyword("Flying");
              }
            }//execute()
          };//Command

          Card c = getTargetCard();
          if(AllZone.GameAction.isCardInPlay(c)  && CardFactoryUtil.canTarget(card, c) )
          {
            c.addTempAttackBoost(3);
            c.addTempDefenseBoost(3);
            c.addExtrinsicKeyword("Flying");

            AllZone.EndOfTurn.addUntil(eot);
          }
        }//resolve()
        public boolean canPlayAI()
        {
          Combat combat = ComputerUtil.getAttackers();
          return (0 != combat.getAttackers().length);
        }
        public void chooseTargetAI()
        {
          Combat combat = ComputerUtil.getAttackers();
          Card[] attacker = combat.getAttackers();
          if(attacker.length != 0)
            setTargetCard(attacker[0]);
          else
          {
            CardList list = new CardList(AllZone.Computer_Play.getCards());
            list = list.getType("Creature");
            Card best = CardFactoryUtil.AI_getBestCreature(list);
            setTargetCard(best);
          }
        }//chooseTargetAI()
      };//SpellAbility
      card.clearSpellAbility();
      card.addSpellAbility(spell);

      spell.setBeforePayMana(CardFactoryUtil.input_targetCreature(spell));
    }//*************** END ************ END **************************
*/


    //*************** START *********** START **************************
    else if (cardName.equals("Molten Rain"))
	{
		final SpellAbility spell = new Spell(card)
		{
			private static final long serialVersionUID = 8855786097956610090L;

			public void resolve()
			{
				Card c = getTargetCard();
				if (AllZone.GameAction.isCardInPlay(c) && CardFactoryUtil.canTarget(card, c)) {
					if(! c.getType().contains("Basic"))
			              AllZone.GameAction.getPlayerLife(c.getController()).subtractLife(2);
					AllZone.GameAction.destroy(c);
				}
				
			}// resolve()
	
		};// Spell
		card.clearSpellAbility();
		card.addSpellAbility(spell);
	
		spell.setChooseTargetAI(CardFactoryUtil.AI_targetType("Land", AllZone.Human_Play));
		spell.setBeforePayMana(CardFactoryUtil.input_targetType(spell,"Land"));
	}// *************** END ************ END **************************


    //*************** START *********** START **************************
    else if(cardName.equals("Belbe's Portal"))
    {
      final SpellAbility ability = new Ability_Tap(card, "3")
      {
        private static final long serialVersionUID = 3790805878629855813L;

		public boolean canPlayAI() {return getCreature().size() != 0;}
        public void chooseTargetAI()
        {
          card.tap();
          Card target = CardFactoryUtil.AI_getBestCreature(getCreature());
          setTargetCard(target);
        }
        CardList getCreature()
        {
          CardList list = new CardList(AllZone.Computer_Hand.getCards());
          list = list.getType(card.getChosenType());
          return list;
        }

        public void resolve()
        {
          Card c = getTargetCard();
          PlayerZone hand = AllZone.getZone(Constant.Zone.Hand, card.getController());
          PlayerZone play = AllZone.getZone(Constant.Zone.Play, card.getController());

          if(AllZone.GameAction.isCardInZone(c, hand))
          {
            hand.remove(c);
            play.add(c);
          }
        }
      };//SpellAbility
      card.addSpellAbility(ability);
      ability.setDescription("3, tap: Put a creature card of the chosen type from your hand into play.");

      final Command paid  = new Command()
      {
		private static final long serialVersionUID = 4258139342966165260L;

		public void execute()
        {
          AllZone.InputControl.resetInput();
          AllZone.Stack.add(ability);
        }
      };
      final Command unpaid = new Command()
      {
		private static final long serialVersionUID = 5792270994683837097L;

		public void execute()
        {
            card.untap();
        }
      };
      final Input target = new Input()
      {
		private static final long serialVersionUID = -3180364352114242238L;
		
		public void showMessage()
        {
          ButtonUtil.enableOnlyCancel();
          AllZone.Display.showMessage("Select creature from your hand to put into play");
        }
        public void selectCard(Card c, PlayerZone zone)
        {
          if(c.isCreature() && zone.is(Constant.Zone.Hand, Constant.Player.Human) && c.getType().contains(card.getChosenType()))
          {
            card.tap();

            ability.setTargetCard(c);//since setTargetCard() changes stack description
            ability.setStackDescription("Put into play " +c);

            AllZone.InputControl.setInput(new Input_PayManaCost_Ability(ability.getManaCost(), paid, unpaid));
          }
        }
        public void selectButtonCancel()
        {
          card.untap();
          stop();
        }
      };//Input target
      ability.setBeforePayMana(target);
    }//*************** END ************ END **************************
    
    
  
  //*************** START *********** START **************************
    else if(cardName.equals("Hex"))
    {
      final Card[] target = new Card[6];
      final int[] index = new int[1];

      final SpellAbility spell = new Spell(card)
      {
        private static final long serialVersionUID = -1880229743741157304L;
		public boolean canPlayAI()
        {
          CardList human = CardFactoryUtil.AI_getHumanCreature(card, true);

          CardListUtil.sortAttack(human);
          CardListUtil.sortFlying(human);

          if(6 <= human.size())
          {
            for(int i = 0; i < 6; i++) //should check to make sure none of these creatures have protection or cannot be the target of spells.
              target[i] = human.get(i);
          }

          return 6 <= human.size();
        }
        public void resolve()
        {
          for(int i = 0; i < target.length; i++)
            if(AllZone.GameAction.isCardInPlay(target[i]) && CardFactoryUtil.canTarget(card, target[i]) )
              AllZone.GameAction.destroy(target[i]);
        }//resolve()
      };//SpellAbility


      final Input input = new Input()
      {
        private static final long serialVersionUID = 5792813689927185739L;
		public void showMessage()
        {
          int count = 6 - index[0];
          AllZone.Display.showMessage("Select target " + count +" creatures to destroy");
          ButtonUtil.enableOnlyCancel();
        }
        public void selectButtonCancel() {stop();}
        public void selectCard(Card c, PlayerZone zone)
        {
          for(int i=0;i<index[0];i++){
             if(c.equals(target[i])){
                AllZone.Display.showMessage("You have already selected this target. You must select unique targets for each of the 6 creatures to destroy.");
                return; //cannot target the same creature twice.
             }
          }
         
          if(c.isCreature() && zone.is(Constant.Zone.Play))
          {
            target[index[0]] = c;
            index[0]++;
            showMessage();

            if(index[0] == target.length)
              stopSetNext(new Input_PayManaCost(spell));
          }
        }//selectCard()
      };//Input

      Input runtime = new Input()
      {
        private static final long serialVersionUID = 3522833806455511494L;

		public void showMessage()
        {
          index[0] = 0;
          stopSetNext(input);
        }
      };//Input

      card.clearSpellAbility();
      card.addSpellAbility(spell);
      spell.setBeforePayMana(runtime);
    }//*************** END ************ END **************************
    
 
    //*************** START *********** START **************************
    else if(cardName.equals("Political Trickery"))
    {
      final Card[] target = new Card[2];
      final int[] index = new int[1];

      final SpellAbility spell = new Spell(card)
      {

		private static final long serialVersionUID = -3075569295823682336L;
		public boolean canPlayAI()
        {
          return false;
        }
        public void resolve()
        {
            
            Card crd0 = target[0];
            Card crd1 = target[1];
            
            if (crd0 != null && crd1 != null)
            {
	            ((PlayerZone_ComesIntoPlay)AllZone.Human_Play).setTriggers(false);
	            ((PlayerZone_ComesIntoPlay)AllZone.Computer_Play).setTriggers(false);
	
	            PlayerZone from0 = AllZone.getZone(crd0);
	            from0.remove(crd0);
	            PlayerZone from1 = AllZone.getZone(crd1);
	            from1.remove(crd1);
	           
	            crd0.setController(AllZone.GameAction.getOpponent(card.getController()));
	            crd1.setController(card.getController());
	            
	            PlayerZone to0 = AllZone.getZone(Constant.Zone.Play, AllZone.GameAction.getOpponent(card.getController()));
	            to0.add(crd0);
	            PlayerZone to1 = AllZone.getZone(Constant.Zone.Play, card.getController());
	            to1.add(crd1);
	
	            ((PlayerZone_ComesIntoPlay)AllZone.Human_Play).setTriggers(true);
	            ((PlayerZone_ComesIntoPlay)AllZone.Computer_Play).setTriggers(true);
            }
            
        }//resolve()
      };//SpellAbility


      final Input input = new Input()
      {
		
		private static final long serialVersionUID = -1017253686774265770L;
		public void showMessage()
        {
          if(index[0] == 0)
            AllZone.Display.showMessage("Select target land you control.");
          else
            AllZone.Display.showMessage("Select target land opponent controls.");

          ButtonUtil.enableOnlyCancel();
        }
        public void selectButtonCancel() {stop();}
        public void selectCard(Card c, PlayerZone zone)
        {
          //must target creature you control
          if(index[0] == 0 && !c.getController().equals(card.getController()))
             return;

          //must target creature you don't control
          if(index[0] == 1 && c.getController().equals(card.getController()))
             return;


          if(c.isLand() && zone.is(Constant.Zone.Play) && CardFactoryUtil.canTarget(card, c))
          {
            target[index[0]] = c;
            index[0]++;
            showMessage();

            if(index[0] == target.length)
              stopSetNext(new Input_PayManaCost(spell));
          }
        }//selectCard()
      };//Input

      Input runtime = new Input()
      {

		private static final long serialVersionUID = 4003351872990899418L;

		public void showMessage()
        {
          index[0] = 0;
          stopSetNext(input);
        }
      };//Input

      card.clearSpellAbility();
      card.addSpellAbility(spell);
      spell.setBeforePayMana(runtime);
    }//*************** END ************ END **************************




    //*************** START *********** START **************************
    else if(cardName.equals("Peel from Reality"))
    {
      final Card[] target = new Card[2];
      final int[] index = new int[1];

      final SpellAbility spell = new Spell(card)
      {
        private static final long serialVersionUID = -5781099237509350795L;
		public boolean canPlayAI()
        {
          return false;
        }
        public void resolve()
        {
          //bounce two creatures in target[]
          for(int i = 0; i < target.length; i++)
          {
            Card c = target[i];
            PlayerZone hand = AllZone.getZone(Constant.Zone.Hand, c.getOwner());

            if(AllZone.GameAction.isCardInPlay(c) && CardFactoryUtil.canTarget(card, c) )
              AllZone.GameAction.moveTo(hand, c);
          }
        }//resolve()
      };//SpellAbility


      final Input input = new Input()
      {
		private static final long serialVersionUID = -5897481915350104062L;
		public void showMessage()
        {
          if(index[0] == 0)
            AllZone.Display.showMessage("Select target creature you control to bounce return to their owners' hand");
          else
            AllZone.Display.showMessage("Select target creature you don't control to return to their owners' hand");

          ButtonUtil.enableOnlyCancel();
        }
        public void selectButtonCancel() {stop();}
        public void selectCard(Card c, PlayerZone zone)
        {
          //must target creature you control
          if(index[0] == 0 && !c.getController().equals(card.getController()))
             return;

          //must target creature you don't control
          if(index[0] == 1 && c.getController().equals(card.getController()))
             return;


          if(c.isCreature() && zone.is(Constant.Zone.Play) && CardFactoryUtil.canTarget(card, c))
          {
            target[index[0]] = c;
            index[0]++;
            showMessage();

            if(index[0] == target.length)
              stopSetNext(new Input_PayManaCost(spell));
          }
        }//selectCard()
      };//Input

      Input runtime = new Input()
      {
		private static final long serialVersionUID = 1194864613104644447L;

		public void showMessage()
        {
          index[0] = 0;
          stopSetNext(input);
        }
      };//Input

      card.clearSpellAbility();
      card.addSpellAbility(spell);
      spell.setBeforePayMana(runtime);
    }//*************** END ************ END **************************




    //*************** START *********** START **************************
    else if(cardName.equals("Ashes to Ashes"))
    {
      final Card[] target = new Card[2];
      final int[] index = new int[1];

      final SpellAbility spell = new Spell(card)
      {
        private static final long serialVersionUID = -6509598408022853029L;

		public boolean canPlayAI()
        {
          return 2 <= getNonArtifact().size() &&
                 5 < AllZone.Computer_Life.getLife() ;
        }
        public void chooseTargetAI()
        {
          CardList human = getNonArtifact();
          CardListUtil.sortAttack(human);

          target[0] = human.get(0);
          target[1] = human.get(1);
        }
        CardList getNonArtifact()
        {
          CardList list = CardFactoryUtil.AI_getHumanCreature(card, true);
          list = list.filter(new CardListFilter()
          {
            public boolean addCard(Card c)
            {
              return !c.isArtifact();
            }
          });
          return list;
        }//getNonArtifact()

        public void resolve()
        {
          for(int i = 0; i < target.length; i++)
          {
            Card c = target[i];
            PlayerZone remove = AllZone.getZone(Constant.Zone.Removed_From_Play, c.getOwner());

            AllZone.GameAction.moveTo(remove, c);
          }

          PlayerLife life = AllZone.GameAction.getPlayerLife(card.getController());
          life.subtractLife(5);
        }//resolve()
      };//SpellAbility


      final Input input = new Input()
      {
		private static final long serialVersionUID = -4114782677700487264L;
		public void showMessage()
        {
          if(index[0] == 0)
            AllZone.Display.showMessage("Select 1st target non-artifact creature to remove from the game");
          else
            AllZone.Display.showMessage("Select 2nd target non-artifact creature to remove from the game");

          ButtonUtil.enableOnlyCancel();
        }
        public void selectButtonCancel() {stop();}
        public void selectCard(Card c, PlayerZone zone)
        {
          if(! c.isArtifact() &&
             c.isCreature()   &&
             zone.is(Constant.Zone.Play))
          {
            target[index[0]] = c;
            index[0]++;
            showMessage();

            if(index[0] == target.length)
              stopSetNext(new Input_PayManaCost(spell));
          }
        }//selectCard()
      };//Input

      Input runtime = new Input()
      {
		private static final long serialVersionUID = -3162536306318797516L;

		public void showMessage()
        {
          index[0] = 0;
          stopSetNext(input);
        }
      };//Input

      card.clearSpellAbility();
      card.addSpellAbility(spell);
      spell.setBeforePayMana(runtime);
    }//*************** END ************ END **************************

    
/*
	// *************** START *********** START **************************
	if (cardName.equals("Lay Waste") || cardName.equals("Stone Rain")
			|| cardName.equals("Ice Storm") || cardName.equals("Sinkhole"))
	{
		final SpellAbility spell = new Spell(card)
		{
			private static final long serialVersionUID = -4973311759179228894L;
			public boolean canPlayAI()
			{
				CardList land = new CardList(AllZone.Human_Play.getCards());
				land = land.getType("Basic");
				return land.size() != 0;
			}
				public void chooseTargetAI()
			{
				// target basic land that Human only has 1 or 2 in play
				CardList land = new CardList(AllZone.Human_Play.getCards());
				land = land.getType("Basic");
					Card target = null;
					String[] name =
				{ "Forest", "Swamp", "Plains", "Mountain", "Island" };
				for (int i = 0; i < name.length; i++)
					if (land.getName(name[i]).size() == 1)
					{
						target = land.getName(name[i]).get(0);
						break;
					}
					// see if there are only 2 lands of the same type
				if (target == null)
				{
					for (int i = 0; i < name.length; i++)
						if (land.getName(name[i]).size() == 2)
						{
							target = land.getName(name[i]).get(0);
							break;
						}
				}// if
				if (target == null)
				{
					land.shuffle();
					target = land.get(0);
				}
				setTargetCard(target);
			}// chooseTargetAI()
				public void resolve()
				{
				if (AllZone.GameAction.isCardInPlay(getTargetCard())  && CardFactoryUtil.canTarget(card, getTargetCard()) )
					AllZone.GameAction.destroy(getTargetCard());
			}// resolve()
		};// SpellAbility
		card.clearSpellAbility();
		spell.setBeforePayMana(CardFactoryUtil.input_targetType(spell,
			"Land"));
			card.addSpellAbility(spell);
			if (cardName.equals("Lay Waste"))
		{
			card.addSpellAbility(CardFactoryUtil.ability_cycle(card, "2"));
			spell.setDescription("Destroy target land");
		}
			return card;
	}// *************** END ************ END **************************
*/


    //*************** START *********** START **************************
    else if(cardName.equals("Wings of Velis Vel"))
    {
      final SpellAbility spell = new Spell(card)
      {
        private static final long serialVersionUID = -5744842090293912606L;
		public boolean canPlayAI()
        {
          Combat combat = ComputerUtil.getAttackers();
          @SuppressWarnings("unused") // attack
		  Card[] attack = combat.getAttackers();

          CardList small = new CardList(AllZone.Computer_Play.getCards());
          small = small.getType("Creature");

          //try to make a good attacker
          if(0 < small.size())
          {
            CardListUtil.sortAttackLowFirst(small);
            setTargetCard(small.get(0));

            return true && AllZone.Phase.getPhase().equals(Constant.Phase.Main1);
          }

          return false;
        }//canPlayAI()
        public void resolve()
        {
          //in case ability is played twice
          final int[] oldAttack = new int[1];
          final int[] oldDefense = new int[1];

          final Card card[] = new Card[1];
          card[0] = getTargetCard();

          oldAttack[0]  = card[0].getBaseAttack();
          oldDefense[0] = card[0].getBaseDefense();

          card[0].setBaseAttack(4);
          card[0].setBaseDefense(4);
          card[0].addExtrinsicKeyword("Flying");

          //EOT
          final Command untilEOT = new Command()
          {
            private static final long serialVersionUID = 7236360479349324099L;

			public void execute()
            {
              card[0].setBaseAttack(oldAttack[0]);
              card[0].setBaseDefense(oldDefense[0]);

              card[0].removeExtrinsicKeyword("Flying");
            }
          };

          AllZone.EndOfTurn.addUntil(untilEOT);
        }//resolve()
      };//SpellAbility
      card.clearSpellAbility();
      card.addSpellAbility(spell);

      spell.setBeforePayMana(CardFactoryUtil.input_targetCreature(spell));
    }//*************** END ************ END **************************


    //*************** START *********** START **************************
    else if(cardName.equals("Lignify"))
    {
      final SpellAbility spell = new Spell(card)
      {
        private static final long serialVersionUID = 5323770119451400755L;
		public boolean canPlayAI()
        {
          CardList c = CardFactoryUtil.AI_getHumanCreature(card, true);
          CardListUtil.sortAttack(c);
          CardListUtil.sortFlying(c);

          if(c.isEmpty())
            return false;

          if(2 <= c.get(0).getNetAttack() && c.get(0).getKeyword().contains("Flying"))
          {
            setTargetCard(c.get(0));
            return true;
          }

          CardListUtil.sortAttack(c);
          if(4 <= c.get(0).getNetAttack())
          {
            setTargetCard(c.get(0));
            return true;
          }

          return false;
        }//canPlayAI()
        public void resolve()
        {
          Card c = getTargetCard();
          if(AllZone.GameAction.isCardInPlay(c) && CardFactoryUtil.canTarget(card, c) )
          {
            c.setBaseAttack(0);
            c.setBaseDefense(4);

            c.setType(new ArrayList<String>());
            c.addType("Creature");
            c.addType("Treefolk");

            c.setIntrinsicKeyword(new ArrayList<String>());

            c.clearSpellAbility();
          }
        }//resolve()
      };//SpellAbility
      card.clearSpellAbility();
      card.addSpellAbility(spell);

      spell.setBeforePayMana(CardFactoryUtil.input_targetCreature(spell));
    }//*************** END ************ END **************************
   
     
    
    //*************** START *********** START **************************
    else if(cardName.equals("That Which Was Taken"))
    {
      final SpellAbility ability = new Ability_Tap(card, "4")
      {
		private static final long serialVersionUID = -8996435083734446340L;
		public void resolve()
        {
          Card c = getTargetCard();

          if(AllZone.GameAction.isCardInPlay(c)  && CardFactoryUtil.canTarget(card, c) )
            c.addExtrinsicKeyword("Indestructible");
        }
        public boolean canPlayAI()
        {
          CardList creatures = getCreatures();

          for (int i = 0; i < creatures.size(); i++)
          {
            if (!creatures.get(i).getKeyword().contains("Indestructible"))
            {
            return true;
          }
          }

          return false;
        }
        public void chooseTargetAI()
        {
          //Card c = CardFactoryUtil.AI_getBestCreature(getCreatures());
          CardList a = getCreatures();
          CardListUtil.sortAttack(a);
          CardListUtil.sortFlying(a);

          Card c = null;

          for (int i = 0; i < a.size(); i++)
          {
            if (!a.get(i).getKeyword().contains("Indestructible"))
            {
            c = a.get(i);
            break;
          }
          }

          setTargetCard(c);
        }
        CardList getCreatures()
        {
          CardList list = new CardList();
          list.addAll(AllZone.Computer_Play.getCards());
          return list.getType("Creature");
        }
      };//SpellAbility

      Input target = new Input()
      {
		private static final long serialVersionUID = 137806881250205274L;
		public void showMessage()
        {
          AllZone.Display.showMessage("Select target permanent");
          ButtonUtil.enableOnlyCancel();
        }
        public void selectButtonCancel() {stop();}
        public void selectCard(Card c, PlayerZone zone)
        {
          if(zone.is(Constant.Zone.Play) && c != card)//cannot target self
          {
            ability.setTargetCard(c);
            stopSetNext(new Input_PayManaCost(ability));
          }
        }
      };//Input -- target

      ability.setBeforePayMana(target);
      ability.setDescription("4, tap: Tap a divinity counter on target permanent other than That Which Was Taken.");

      card.addSpellAbility(ability);
    }//*************** END ************ END **************************


  //*************** START *********** START **************************
    else if(cardName.equals("Spectral Procession"))
    {
    	final SpellAbility spell = new Spell(card)
    	{
			private static final long serialVersionUID = 6118842682065463016L;
			public void resolve()
	    	{
	    		makeToken();
	    		makeToken();
	    		makeToken();
	    	}
	    	void makeToken()
	    	{
	    		PlayerZone play = AllZone.getZone(Constant.Zone.Play, card.getController());
	            Card c = new Card();

	            c.setOwner(card.getController());
	            c.setController(card.getController());

	            c.setManaCost("W");
	            c.setToken(true);

	            c.addType("Creature");
	            c.addType("Spirit");
	            c.setBaseAttack(1);
	            c.setBaseDefense(1);
	            c.addIntrinsicKeyword("Flying");

	            c.setName("Spirit");
	            c.setImageName("W 1 1 Spirit");
	            
	            play.add(c);
	    	}
	    };
	    
	    card.clearSpellAbility();
	    card.addSpellAbility(spell);
  	}//*************** END ************ END **************************
    
  
    
  //*************** START *********** START **************************
    else if(cardName.equals("Sacred Nectar"))
    {
      SpellAbility spell = new Spell(card)
      {

		private static final long serialVersionUID = -2280675656970845618L;

		public boolean canPlay()
        {
          setStackDescription(card.getName() +" - " +card.getController() +" gains 4 life.");
          return super.canPlay();
        }

        public void resolve()
        {
          PlayerLife life = AllZone.GameAction.getPlayerLife(card.getController());
          life.addLife(4);
        }
      };
      spell.setDescription("You gain 4 life.");

      card.clearSpellAbility();
      card.addSpellAbility(spell);
     
    }//*************** END ************ END **************************

   
 

    //*************** START *********** START **************************
    else if(cardName.equals("Tremor"))
    {
      SpellAbility spell = new Spell(card)
      {
		private static final long serialVersionUID = 3451888160398198322L;

		public boolean canPlayAI()
		{
			CardList human = new CardList(AllZone.Human_Play.getCards());
			CardList computer = new CardList(AllZone.Computer_Play.getCards());

			human = human.filter(new CardListFilter()
			{

				public boolean addCard(Card c) {
					return !c.getKeyword().contains("Flying") && CardFactoryUtil.canDamage(card, c) && 
						    c.getNetDefense() == 1;
				}
			});
			computer = computer.filter(new CardListFilter()
			{
				public boolean addCard(Card c) {
					return !c.getKeyword().contains("Flying") && CardFactoryUtil.canDamage(card, c) &&
							c.getNetDefense() == 1;
				}
			});

			// the computer will at least destroy 2 more human creatures
			return computer.size() < human.size() - 1;
		}
		public void resolve()
        {
          CardList all = new CardList();
          all.addAll(AllZone.Human_Play.getCards());
          all.addAll(AllZone.Computer_Play.getCards());
          all = all.getType("Creature");

          for(int i = 0; i < all.size(); i++)
            if(! all.get(i).getKeyword().contains("Flying") && CardFactoryUtil.canDamage(card,all.get(i)))
              all.get(i).addDamage(1, card);
        }
      };
      card.clearSpellAbility();
      card.addSpellAbility(spell);
    }//*************** END ************ END **************************



    //*************** START *********** START **************************
    else if(cardName.equals("Reviving Dose"))
    {
      SpellAbility spell = new Spell(card)
      {
		private static final long serialVersionUID = -3239301336328919121L;

		public void resolve()
        {
          PlayerLife life = AllZone.GameAction.getPlayerLife(card.getController());
          life.addLife(3);

          AllZone.GameAction.drawCard(card.getController());
        }
      };
      card.clearSpellAbility();
      card.addSpellAbility(spell);
    }//*************** END ************ END **************************



    //*************** START *********** START **************************
    else if(cardName.equals("Swords to Plowshares"))
    {
      SpellAbility spell = new Spell(card)
      {
		private static final long serialVersionUID = 4752934806606319269L;
		
		public void resolve()
        {
          if(AllZone.GameAction.isCardInPlay(getTargetCard())  && CardFactoryUtil.canTarget(card, getTargetCard()) )
          {
            //add life
            String player = getTargetCard().getController();
            PlayerLife life = AllZone.GameAction.getPlayerLife(player);
            life.addLife(getTargetCard().getNetAttack());

            //remove card from play
            AllZone.GameAction.removeFromGame(getTargetCard());
          }
        }//resolve()
        public boolean canPlayAI()
        {
          CardList creature = new CardList(AllZone.Human_Play.getCards());
          creature = creature.getType("Creature");
          creature = creature.filter(new CardListFilter()
          {
			public boolean addCard(Card c) {
				return CardFactoryUtil.canTarget(card,c);
			}   
          });
          return creature.size() != 0 && (AllZone.Phase.getTurn() > 4);
        }
        public void chooseTargetAI()
        {
          CardList play = new CardList(AllZone.Human_Play.getCards());
          Card target = CardFactoryUtil.AI_getBestCreature(play, card);
          setTargetCard(target);
        }
      };
      spell.setBeforePayMana(CardFactoryUtil.input_targetCreature(spell));

      card.clearSpellAbility();
      card.addSpellAbility(spell);
    }//*************** END ************ END **************************
    
    //*************** START *********** START **************************
    else if(cardName.equals("Unmake"))
    {
      SpellAbility spell = new Spell(card)
      {
		private static final long serialVersionUID = -9194035528349589512L;
		public void resolve()
        {
          if(AllZone.GameAction.isCardInPlay(getTargetCard())  && CardFactoryUtil.canTarget(card, getTargetCard()) )
          {
        	//remove card from play
            AllZone.GameAction.removeFromGame(getTargetCard());
          }
        }//resolve()
        public boolean canPlayAI()
        {
          CardList creature = new CardList(AllZone.Human_Play.getCards());
          creature = creature.getType("Creature");
          creature = creature.filter(new CardListFilter()
          {
			public boolean addCard(Card c) {
				return CardFactoryUtil.canTarget(card,c);
			}   
          });
          return creature.size() != 0 && (AllZone.Phase.getTurn() > 4);
        }
        public void chooseTargetAI()
        {
          CardList play = new CardList(AllZone.Human_Play.getCards());
          Card target = CardFactoryUtil.AI_getBestCreature(play);
          setTargetCard(target);
        }
      };
      spell.setBeforePayMana(CardFactoryUtil.input_targetCreature(spell));

      card.clearSpellAbility();
      card.addSpellAbility(spell);
    }//*************** END ************ END **************************



  //*************** START *********** START **************************
    else if(cardName.equals("Crib Swap"))
    {
      SpellAbility spell = new Spell(card)
      {
		private static final long serialVersionUID = -4567382566960071562L;
		
		public void resolve()
        {
          if(AllZone.GameAction.isCardInPlay(getTargetCard())  && CardFactoryUtil.canTarget(card, getTargetCard()) )
          {
            String player = getTargetCard().getController();
            makeToken(player);

            //remove card from play
            AllZone.GameAction.removeFromGame(getTargetCard());
          }
        }//resolve()
        public boolean canPlayAI()
        {
          CardList creature = new CardList(AllZone.Human_Play.getCards());
          creature = creature.getType("Creature");
          return creature.size() != 0 && (AllZone.Phase.getTurn() > 4);
        }
        public void chooseTargetAI()
        {
          CardList play = new CardList(AllZone.Human_Play.getCards());
          Card target = CardFactoryUtil.AI_getBestCreature(play);
          setTargetCard(target);
        }
        void makeToken(String player)
        {
          Card c = new Card();

          c.setName("Shapeshifter");
          c.setName("C 1 1 Shapeshifter");

          c.setOwner(player);
          c.setController(player);

          c.setManaCost("");
          c.setToken(true);

          c.addType("Creature");
          c.addType("Shapeshifter");
          c.addIntrinsicKeyword("Changeling");
          c.setBaseAttack(1);
          c.setBaseDefense(1);

          PlayerZone play = AllZone.getZone(Constant.Zone.Play, player);
          play.add(c);
        }//makeToken()

      };
      spell.setBeforePayMana(CardFactoryUtil.input_targetCreature(spell));

      card.clearSpellAbility();
      card.addSpellAbility(spell);
    }//*************** END ************ END **************************




  //*************** START *********** START **************************
    else if(cardName.equals("Demonic Tutor") || cardName.equals("Diabolic Tutor") || cardName.equals("Grim Tutor"))
    {
      final SpellAbility spell = new Spell(card)
      {
      private static final long serialVersionUID = 1481169060428051519L;
      
      public void resolve()
        {
          String player = card.getController();
          if(player.equals(Constant.Player.Human))
            humanResolve();
          else
            computerResolve();
        }
        public void humanResolve()
        {
          Object check = AllZone.Display.getChoiceOptional("Select card", AllZone.Human_Library.getCards());
          if(check != null)
          {
            PlayerZone hand = AllZone.getZone(Constant.Zone.Hand, card.getController());
            AllZone.GameAction.moveTo(hand, (Card)check);
          }
          AllZone.GameAction.shuffle(Constant.Player.Human);
         
          //lose 3 life
          if(cardName.equals("Grim Tutor"))
          {    
           String player = Constant.Player.Human;
           PlayerLife life = AllZone.GameAction.getPlayerLife(player);
           life.subtractLife(3);
          }
        }
        public void computerResolve()
        {
          Card[] library = AllZone.Computer_Library.getCards();
          CardList list = new CardList(library);

          //pick best creature
          Card c = CardFactoryUtil.AI_getBestCreature(list);
          if(c == null)
            c = library[0];
          //System.out.println("comptuer picked - " +c);
          AllZone.Computer_Library.remove(c);
          AllZone.Computer_Hand.add(c);
         
          //lose 3 life
          if(cardName.equals("Grim Tutor"))
          {   
           String player = Constant.Player.Computer;
           PlayerLife life = AllZone.GameAction.getPlayerLife(player);
           life.subtractLife(3);
          }
        }
        public boolean canPlay()
        {
          PlayerZone library = AllZone.getZone(Constant.Zone.Library, card.getController());
         
          return library.getCards().length != 0 && AllZone.Phase.getActivePlayer().equals(card.getController())
          && !AllZone.Phase.getPhase().equals("End of Turn") && super.canPlay();
        }
        public boolean canPlayAI()
        {
          CardList creature = new CardList();
          creature.addAll(AllZone.Computer_Library.getCards());
          creature = creature.getType("Creature");
          return creature.size() != 0;
        }
      };//SpellAbility
      card.clearSpellAbility();
      card.addSpellAbility(spell);
    }//*************** END ************ END **************************
    
    
  //*************** START *********** START **************************
    else if(cardName.equals("Entomb"))
    {
      final SpellAbility spell = new Spell(card)
      {
		private static final long serialVersionUID = 4724906962713222211L;
		public void resolve()
        {
          String player = card.getController();
          if(player.equals(Constant.Player.Human))
            humanResolve();
          else
            computerResolve();
        }
        public void humanResolve()
        {
          Object check = AllZone.Display.getChoiceOptional("Select card", AllZone.Human_Library.getCards());
          if(check != null)
          {
            PlayerZone grave = AllZone.getZone(Constant.Zone.Graveyard, card.getController());
            AllZone.GameAction.moveTo(grave, (Card)check);
          }
          AllZone.GameAction.shuffle(Constant.Player.Human);
        }
        public void computerResolve()
        {
          Card[] library = AllZone.Computer_Library.getCards();
          CardList list = new CardList(library);


          //pick best creature
          Card c = CardFactoryUtil.AI_getBestCreature(list);
          if(c == null)
            c = library[0];
          //System.out.println("comptuer picked - " +c);
          AllZone.Computer_Library.remove(c);
          AllZone.Computer_Graveyard.add(c);
        }
        public boolean canPlay()
        {
          PlayerZone library = AllZone.getZone(Constant.Zone.Library, card.getController());
          
          return library.getCards().length != 0;
        }
        public boolean canPlayAI()
        {
          CardList creature = new CardList();
          creature.addAll(AllZone.Computer_Library.getCards());
          creature = creature.getType("Creature");
          return creature.size() != 0;
        }
      };//SpellAbility
      card.clearSpellAbility();
      card.addSpellAbility(spell);
    }//*************** END ************ END **************************



    //*************** START *********** START **************************
    else if(cardName.equals("Do or Die"))
    {
      final SpellAbility spell = new Spell(card)
      {
		private static final long serialVersionUID = 8241241003478388362L;

		public boolean canPlayAI()
        {
          return 4 <= CardFactoryUtil.AI_getHumanCreature(card, true).size() && 4 < AllZone.Phase.getTurn();
        }

        public void resolve()
        {
          PlayerZone play = AllZone.getZone(Constant.Zone.Play, getTargetPlayer());
          CardList list = new CardList(play.getCards());
          list = list.getType("Creature");

          list.shuffle();

          for(int i = 0; i < list.size() / 2; i++)
            AllZone.GameAction.destroyNoRegeneration(list.get(i));
        }
      };//SpellAbility
      spell.setChooseTargetAI(CardFactoryUtil.AI_targetHuman());
      spell.setBeforePayMana(CardFactoryUtil.input_targetPlayer(spell));

      card.clearSpellAbility();
      card.addSpellAbility(spell);
    }//*************** END ************ END **************************




    //*************** START *********** START **************************
    if (cardName.equals("Hymn to Tourach"))
    {
      final SpellAbility spell = new Spell(card)
      {
		private static final long serialVersionUID = 42470566751344693L;

        public boolean canPlayAI()
        {
          return AllZone.Human_Hand.size() > 0;
        }

        public void resolve()
        {
          String opponent = AllZone.GameAction.getOpponent(card.getController());
          AllZone.GameAction.discardRandom(opponent);
          AllZone.GameAction.discardRandom(opponent);
        }
      };//SpellAbility
      card.clearSpellAbility();
      card.addSpellAbility(spell);
    }//*************** END ************ END **************************


    //*************** START *********** START **************************
    else if(cardName.equals("Beacon of Destruction"))
    {
      final SpellAbility spell = new Spell(card)
      {
		private static final long serialVersionUID = 6653675303299939465L;
		
		public void resolve()
        {
          if(getTargetCard() != null)
          {
            if(AllZone.GameAction.isCardInPlay(getTargetCard()) && CardFactoryUtil.canTarget(card, getTargetCard()) )
            {
              getTargetCard().addDamage(5, card);
              done();
            }
            else
              AllZone.GameAction.moveToGraveyard(card);
          }
          else
          {
            AllZone.GameAction.getPlayerLife(getTargetPlayer()).subtractLife(5);
            done();
          }
        }//resolve()
        void done()
        {
          //shuffle card back into the library
          PlayerZone library = AllZone.getZone(Constant.Zone.Library, card.getController());
          library.add(card);
          AllZone.GameAction.shuffle(card.getController());
        }
      };
      spell.setChooseTargetAI(CardFactoryUtil.AI_targetHumanCreatureOrPlayer());

      spell.setBeforePayMana(CardFactoryUtil.input_targetCreaturePlayer(spell, true));
      card.clearSpellAbility();
      card.addSpellAbility(spell);
    }//*************** END ************ END **************************
    
    //*************** START *********** START **************************
    else if(cardName.equals("Capsize"))
    {
      final SpellAbility spell_one = new Spell(card)
      {
		private static final long serialVersionUID = 7688777028599839669L;

		public boolean canPlayAI() {return false;}

        public void resolve()
        {
          final Card[] target = new Card[1];
          target[0] = getTargetCard();
          PlayerZone hand = AllZone.getZone(Constant.Zone.Hand, target[0].getController());
          
          if(AllZone.GameAction.isCardInPlay(target[0])  && CardFactoryUtil.canTarget(card, target[0]) )
          {
        	  if (!target[0].isToken())
        		  AllZone.GameAction.moveTo(hand ,target[0]);
        	  else
        		  AllZone.GameAction.removeFromGame(target[0]);
        	  
          }
        }//resolve()
      };//SpellAbility

      final Card crd = card;
      
      final SpellAbility spell_two = new Spell(card)
      {

		private static final long serialVersionUID = -2399079881132655853L;
		public boolean canPlayAI()
        {
			CardList human = CardFactoryUtil.AI_getHumanCreature(card, true);
	        return 3 < AllZone.Phase.getTurn() && 0 < human.size();
          
        }
        public void chooseTargetAI()
        {
        	CardList human = CardFactoryUtil.AI_getHumanCreature(card, true);
            setTargetCard(CardFactoryUtil.AI_getBestCreature(human));
        }
        

        public void resolve()
        {
          
        	final Card[] target = new Card[1];
            target[0] = getTargetCard();
            PlayerZone hand = AllZone.getZone(Constant.Zone.Hand, target[0].getOwner());
            
            if(AllZone.GameAction.isCardInPlay(target[0]) && CardFactoryUtil.canTarget(card, target[0]) )
            {
              if (!target[0].isToken())
           		  AllZone.GameAction.moveTo(hand ,target[0]);
           	  else
           		  AllZone.GameAction.removeFromGame(target[0]);
            }
            else
            {
          	  crd.clearReplaceMoveToGraveyardCommandList();
            }
        }//resolve()
      };//SpellAbility
      spell_two.setManaCost("4 U U");

      spell_one.setDescription("Return target permanent to its owner's hand.");
      spell_two.setDescription("Buyback 3 - Pay 4 U U, put this card into your hand as it resolves.");
      spell_two.setIsBuyBackAbility(true);

      Input runtime1 = new Input()
	  {
	
    	    private static final long serialVersionUID = 6884105724632382299L;

			public void showMessage()
	        {
			  PlayerZone human = AllZone.getZone(Constant.Zone.Play, Constant.Player.Human);
		      PlayerZone comp = AllZone.getZone(Constant.Zone.Play, Constant.Player.Computer);
		      CardList perms = new CardList();
			  perms.addAll(human.getCards());
			  perms.addAll(comp.getCards());
			  
	          stopSetNext(CardFactoryUtil.input_targetSpecific(spell_one, perms, "Select target permanent.", true));
	        }
	  };  
	  
	  Input runtime2 = new Input()
	  {
		  	private static final long serialVersionUID = 8564956495965504679L;

			public void showMessage()
	        {
			  PlayerZone human = AllZone.getZone(Constant.Zone.Play, Constant.Player.Human);
	          PlayerZone comp = AllZone.getZone(Constant.Zone.Play, Constant.Player.Computer);
			  CardList perms = new CardList();
			  perms.addAll(human.getCards());
			  perms.addAll(comp.getCards());
	          
	          
	          stopSetNext(CardFactoryUtil.input_targetSpecific(spell_two, perms, "Select target permanent.", true));
	        }
	  };  
      
      spell_one.setBeforePayMana(runtime1);
      spell_two.setBeforePayMana(runtime2);

      card.clearSpellAbility();
      card.addSpellAbility(spell_one);
      card.addSpellAbility(spell_two);
      
    }//*************** END ************ END **************************
    
  //*************** START *********** START **************************
    else if(cardName.equals("Whispers of the Muse"))
    {
      final SpellAbility spell_one = new Spell(card)
      {

		private static final long serialVersionUID = 8341386638247535343L;

		public boolean canPlayAI() {return false;}

        public void resolve()
        {
          AllZone.GameAction.drawCard(card.getController());
        }//resolve()
      };//SpellAbility

      final SpellAbility spell_two = new Spell(card)
      {

 		private static final long serialVersionUID = -131686114078716307L;
		public void resolve()
        {            
        	AllZone.GameAction.drawCard(card.getController());
            done();
        }//resolve()
        void done()
        {
          //return card to the hand
          PlayerZone hand = AllZone.getZone(Constant.Zone.Hand, card.getController());
          AllZone.GameAction.moveTo(hand, card);
        }
      };//SpellAbility
      spell_two.setManaCost("5 U");

      spell_one.setDescription("Draw a card.");
      spell_one.setStackDescription(cardName + " - " +card.getController() + " draws a card.");
      spell_two.setDescription("Buyback 5 - Pay 5 U , put this card into your hand as it resolves.");
      spell_two.setStackDescription(cardName + " - (Buyback) " +card.getController() + " draws a card.");
      spell_two.setIsBuyBackAbility(true);

      card.clearSpellAbility();
      card.addSpellAbility(spell_one);
      card.addSpellAbility(spell_two);
      
    }//*************** END ************ END **************************


    //*************** START *********** START **************************
    else if(cardName.equals("Elvish Fury"))
    {
    
      final SpellAbility spell_one = new Spell(card)
      {
		private static final long serialVersionUID = 3356401944678089378L;

		public boolean canPlayAI() {return false;}

        public void resolve()
        {
          final Card[] target = new Card[1];
          final Command untilEOT = new Command()
          {
			private static final long serialVersionUID = 7120352016188545025L;

			public void execute()
            {
              if(AllZone.GameAction.isCardInPlay(target[0]))
              {
                target[0].addTempAttackBoost(-2);
                target[0].addTempDefenseBoost(-2);
              }
            }
          };

          target[0] = getTargetCard();
          if(AllZone.GameAction.isCardInPlay(target[0])  && CardFactoryUtil.canTarget(card, target[0]) )
          {
            target[0].addTempAttackBoost(2);
            target[0].addTempDefenseBoost(2);

            AllZone.EndOfTurn.addUntil(untilEOT);
          }
          else{
        	  
          }
        }//resolve()
      };//SpellAbility

      final Card crd = card;
      
      final SpellAbility spell_two = new Spell(card)
      {
		private static final long serialVersionUID = 3898017438147188882L;
		
		public boolean canPlayAI()
        {
          return getAttacker() != null;
        }
        public void chooseTargetAI()
        {
          setTargetCard(getAttacker());
        }
        public Card getAttacker()
        {
          //target creature that is going to attack
          Combat c = ComputerUtil.getAttackers();

          CardList list = new CardList(c.getAttackers());
          CardListUtil.sortFlying(list);

          Card[] att = list.toArray();
          if(att.length != 0)
            return att[0];
          else
            return null;
        }//getAttacker()


        public void resolve()
        {
          final Card[] target = new Card[1];
          final Command untilEOT = new Command()
          {
			private static final long serialVersionUID = 280295105716586978L;

			public void execute()
            {
              if(AllZone.GameAction.isCardInPlay(target[0]))
              {
                target[0].addTempAttackBoost(-2);
                target[0].addTempDefenseBoost(-2);
              }
            }
          };

          target[0] = getTargetCard();
          if(AllZone.GameAction.isCardInPlay(target[0]) && CardFactoryUtil.canTarget(card, target[0]) )
          {
            target[0].addTempAttackBoost(2);
            target[0].addTempDefenseBoost(2);

            AllZone.EndOfTurn.addUntil(untilEOT);
          }
          else
          {
        	  crd.clearReplaceMoveToGraveyardCommandList();
          }
        }//resolve()
      };//SpellAbility
      spell_two.setManaCost("4 G");

      spell_one.setDescription("Target creature gets +2/+2 until end of turn.");
      spell_two.setDescription("Buyback 4 - Pay 4G, put this card into your hand as it resolves.");

      spell_one.setBeforePayMana(CardFactoryUtil.input_targetCreature(spell_one));
      spell_two.setBeforePayMana(CardFactoryUtil.input_targetCreature(spell_two));
      
      spell_two.setIsBuyBackAbility(true);

      card.clearSpellAbility();
      card.addSpellAbility(spell_one);
      card.addSpellAbility(spell_two);
      
    }//*************** END ************ END **************************
    
    
    
  //*************** START *********** START **************************
    else if(cardName.equals("Lab Rats"))
    {
      final Command makeToken = new Command()
      {
		private static final long serialVersionUID = -6979735250385389139L;

		public void execute()
        {
            Card c = new Card();

            c.setOwner(card.getController());
            c.setController(card.getController());

            c.setName("Rat");
            c.setImageName("B 1 1 Rat");
            c.setManaCost("B");
            c.setToken(true);

            c.addType("Creature");
            c.addType("Rat");
            c.setBaseAttack(1);
            c.setBaseDefense(1);

            PlayerZone play = AllZone.getZone(Constant.Zone.Play, card.getController());
            play.add(c);
        }//execute()
      };//Command

      final SpellAbility spell_one = new Spell(card)
      {
		private static final long serialVersionUID = -8112024383172056976L;

		public boolean canPlayAI() {return false;}

        public void resolve()
        {
          makeToken.execute();
        }//resolve()
      };//SpellAbility

      final SpellAbility spell_two = new Spell(card)
      {
		private static final long serialVersionUID = -7503701530510847636L;

		public void resolve()
        {
          makeToken.execute();

          //return card to the hand
          PlayerZone hand = AllZone.getZone(Constant.Zone.Hand, card.getController());
          AllZone.GameAction.moveTo(hand, card);
        }
        
        public boolean canPlayAI()
        {
           String phase = AllZone.Phase.getPhase();
      	   return phase.equals(Constant.Phase.Main2);
        }
      };//SpellAbility

      spell_one.setManaCost("B");
      spell_two.setManaCost("4 B");

      spell_one.setDescription("Put a 1/1 black Rat token into play.");
      spell_two.setDescription("Buyback 4 - Pay 4B, put this card into your hand as it resolves.");

      spell_one.setStackDescription("Lab Rats - Put a 1/1 black Rat token into play");
      spell_two.setStackDescription("Lab Rats - Buyback, Put a 1/1 black Rat token into play");

      spell_two.setIsBuyBackAbility(true);
      
      card.clearSpellAbility();
      card.addSpellAbility(spell_one);
      card.addSpellAbility(spell_two);
    }//*************** END ************ END **************************

    //*************** START *********** START **************************
    else if(cardName.equals("Sprout Swarm"))
    {
      final Command makeToken = new Command()
      {
		private static final long serialVersionUID = 648546709124047998L;

		public void execute()
        {
            Card c = new Card();

            c.setOwner(card.getController());
            c.setController(card.getController());

            c.setName("Saproling");
            c.setImageName("G 1 1 Saproling");
            c.setManaCost("G");
            c.setToken(true);

            c.addType("Creature");
            c.addType("Saproling");
            c.setBaseAttack(1);
            c.setBaseDefense(1);

            PlayerZone play = AllZone.getZone(Constant.Zone.Play, card.getController());
            play.add(c);
        }//execute()
      };//Command

      final SpellAbility spell_one = new Spell(card)
      {
		private static final long serialVersionUID = -609007714604161377L;

		public boolean canPlayAI() {return false;}

        public void resolve()
        {
          makeToken.execute();
        }//resolve()
      };//SpellAbility

      final SpellAbility spell_two = new Spell(card)
      {
		private static final long serialVersionUID = -1387385820860395676L;

		public void resolve()
        {
          makeToken.execute();

          //return card to the hand
          PlayerZone hand = AllZone.getZone(Constant.Zone.Hand, card.getController());
          AllZone.GameAction.moveTo(hand, card);
        }
      };//SpellAbility

      spell_one.setManaCost("1 G");
      spell_two.setManaCost("4 G");

      spell_one.setDescription("Put a 1/1 green Saproling token into play.");
      spell_two.setDescription("Buyback 3 - Pay 4G, put this card into your hand as it resolves.");

      spell_one.setStackDescription("Sprout Swarm - Put a 1/1 green Saproling token into play");
      spell_two.setStackDescription("Sprout Swarm - Buyback, Put a 1/1 green Saproling token into play");
      
      spell_two.setIsBuyBackAbility(true);

      card.clearSpellAbility();
      card.addSpellAbility(spell_one);
      card.addSpellAbility(spell_two);
    }//*************** END ************ END **************************



    //*************** START *********** START **************************
    else if(cardName.equals("Threaten") || cardName.equals("Act of Treason"))
    {
      final PlayerZone[] orig = new PlayerZone[1];
      final PlayerZone[] temp = new PlayerZone[1];
      final String[] controllerEOT = new String[1];
      final Card[] target          = new Card[1];

      final Command untilEOT = new Command()
      {
		private static final long serialVersionUID = 5310901886760561889L;

		public void execute()
        {
          //if card isn't in play, do nothing
          if(! AllZone.GameAction.isCardInPlay(target[0]))
            return;

          target[0].setController(controllerEOT[0]);

          ((PlayerZone_ComesIntoPlay)AllZone.Human_Play).setTriggers(false);
          ((PlayerZone_ComesIntoPlay)AllZone.Computer_Play).setTriggers(false);

          //moveTo() makes a new card, so you don't have to remove "Haste"
          //AllZone.GameAction.moveTo(playEOT[0], target[0]);
          temp[0].remove(target[0]);
          orig[0].add(target[0]);
          target[0].untap();
          target[0].removeExtrinsicKeyword("Haste");
          
          ((PlayerZone_ComesIntoPlay)AllZone.Human_Play).setTriggers(true);
          ((PlayerZone_ComesIntoPlay)AllZone.Computer_Play).setTriggers(true);
        }//execute()
      };//Command

      final SpellAbility spell = new Spell(card)
      {
		private static final long serialVersionUID = -3447822168516135816L;
		
		public void resolve()
        {
          if(AllZone.GameAction.isCardInPlay(getTargetCard()) && CardFactoryUtil.canTarget(card, getTargetCard()) )
          {
            orig[0]       = AllZone.getZone(getTargetCard());
            controllerEOT[0] = getTargetCard().getController();
            target[0]        = getTargetCard();

            //set the controller
            getTargetCard().setController(card.getController());

            ((PlayerZone_ComesIntoPlay)AllZone.Human_Play).setTriggers(false);
            ((PlayerZone_ComesIntoPlay)AllZone.Computer_Play).setTriggers(false);

            PlayerZone play = AllZone.getZone(Constant.Zone.Play, card.getController());
            play.add(getTargetCard());
            temp[0] = play;
            orig[0].remove(getTargetCard());

            ((PlayerZone_ComesIntoPlay)AllZone.Human_Play).setTriggers(true);
            ((PlayerZone_ComesIntoPlay)AllZone.Computer_Play).setTriggers(true);


            getTargetCard().untap();
            getTargetCard().addExtrinsicKeyword("Haste");

            AllZone.EndOfTurn.addUntil(untilEOT);
          }//is card in play?
        }//resolve()
        public boolean canPlayAI()
        {
          //only use this card if creatures power is greater than 2
          CardList list = new CardList(AllZone.Human_Play.getCards());
          for(int i = 0; i < list.size(); i++)
            if(2 < list.get(i).getNetAttack())
              return true;

          return false;
        }//canPlayAI()
        public void chooseTargetAI()
        {
          CardList list = new CardList(AllZone.Human_Play.getCards());
          setTargetCard(CardFactoryUtil.AI_getBestCreature(list));
        }
      };//SpellAbility
      spell.setBeforePayMana(CardFactoryUtil.input_targetCreature(spell));
      card.clearSpellAbility();
      card.addSpellAbility(spell);
    }//*************** END ************ END **************************




    //*************** START *********** START **************************
    else if(cardName.equals("Beacon of Unrest"))
    {
      final SpellAbility spell = new Spell(card)
      {
		private static final long serialVersionUID = -7614131436905786565L;
		
		public void resolve()
        {
          Card c = getTargetCard();
          PlayerZone grave = AllZone.getZone(c);

          if(AllZone.GameAction.isCardInZone(c, grave) && (c.isArtifact() || c.isCreature()))
          {
            //set the correct controller if needed
            c.setController(card.getController());

            //card changes zones
            AllZone.getZone(c).remove(c);
            PlayerZone play = AllZone.getZone(Constant.Zone.Play, card.getController());
            play.add(c);

            //shuffle card back into the library
            PlayerZone library = AllZone.getZone(Constant.Zone.Library, card.getController());
            library.add(card);
            AllZone.GameAction.shuffle(card.getController());
          }
        }//resolve()
        public boolean canPlay()
        {
          return getCreaturesAndArtifacts().length != 0;
        }

        public Card[] getCreaturesAndArtifacts()
        {
          CardList graveyardCards = new CardList();
          graveyardCards.addAll(AllZone.Human_Graveyard.getCards());
          graveyardCards.addAll(AllZone.Computer_Graveyard.getCards());

          CardList graveyardCreaturesAndArtifacts = graveyardCards.filter(new CardListFilter()
          {
            public boolean addCard(Card c)
            {
              return c.isArtifact() || c.isCreature();
            }
          });

          return graveyardCreaturesAndArtifacts.toArray();
        }
        public void chooseTargetAI()
        {
          Card c[] = getCreaturesAndArtifacts();
          Card biggest = c[0];
          for(int i = 0; i < c.length; i++)
            if(biggest.getNetAttack() < c[i].getNetAttack())
              biggest = c[i];

          setTargetCard(biggest);
        }
      };//SpellAbility
      card.clearSpellAbility();
      card.addSpellAbility(spell);

      Input target = new Input()
      {
		private static final long serialVersionUID = -83460850846474327L;

		public void showMessage()
        {
          Object check = AllZone.Display.getChoiceOptional("Select creature", getCreaturesAndArtifacts());
          if(check != null)
          {
            spell.setTargetCard((Card)check);
            stopSetNext(new Input_PayManaCost(spell));
          }
          else
            stop();
        }//showMessage()

        //duplicated from SpellAbility above ^^^^^^^^
        public Card[] getCreaturesAndArtifacts()
        {
          CardList graveyardCards = new CardList();
          graveyardCards.addAll(AllZone.Human_Graveyard.getCards());
          graveyardCards.addAll(AllZone.Computer_Graveyard.getCards());

          CardList graveyardCreaturesAndArtifacts = graveyardCards.filter(new CardListFilter()
          {
            public boolean addCard(Card c)
            {
              return c.isArtifact() || c.isCreature();
            }
          });

          return graveyardCreaturesAndArtifacts.toArray();
        }
      };//Input
      spell.setBeforePayMana(target);
    }//*************** END ************ END **************************




    //*************** START *********** START **************************
    else if(cardName.equals("Breath of Life") || cardName.equals("Resurrection") || cardName.equals("False Defeat") || cardName.equals("Zombify"))
    {
      final SpellAbility spell = new Spell(card)
      {
		private static final long serialVersionUID = -5799646914112924814L;
		
		public void resolve()
        {
          Card c = getTargetCard();
          PlayerZone grave = AllZone.getZone(c);

          if(AllZone.GameAction.isCardInZone(c, grave))
          {
            PlayerZone play = AllZone.getZone(Constant.Zone.Play, c.getController());
            AllZone.GameAction.moveTo(play, c);
          }
        }//resolve()
        public boolean canPlay()
        {
          return getCreatures().length != 0;
        }
        public Card[] getCreatures()
        {
          CardList creature = new CardList();
          PlayerZone zone = AllZone.getZone(Constant.Zone.Graveyard, card.getController());
          creature.addAll(zone.getCards());
          creature = creature.getType("Creature");
          return creature.toArray();
        }
        public void chooseTargetAI()
        {
          Card c[] = getCreatures();
          Card biggest = c[0];
          for(int i = 0; i < c.length; i++)
            if(biggest.getNetAttack() < c[i].getNetAttack())
              biggest = c[i];

          setTargetCard(biggest);
        }
      };//SpellAbility
      card.clearSpellAbility();
      card.addSpellAbility(spell);

      Input target = new Input()
      {
		private static final long serialVersionUID = -3717723884199321767L;

		public void showMessage()
        {
          Object check = AllZone.Display.getChoiceOptional("Select creature", getCreatures());
          if(check != null)
          {
            spell.setTargetCard((Card)check);
            stopSetNext(new Input_PayManaCost(spell));
          }
          else
            stop();
        }//showMessage()

        public Card[] getCreatures()
        {
          CardList creature = new CardList();
          PlayerZone zone = AllZone.getZone(Constant.Zone.Graveyard, card.getController());
          creature.addAll(zone.getCards());
          creature = creature.getType("Creature");
          return creature.toArray();
        }
      };//Input
      spell.setBeforePayMana(target);
    }//*************** END ************ END **************************
    
  
    //*************** START *********** START **************************
    else if(cardName.equals("Animate Dead"))
    {
      final SpellAbility spell = new Spell(card)
      {
		private static final long serialVersionUID = 182812167945075560L;

		public void resolve()
        {
          Card c = getTargetCard();

          PlayerZone play = AllZone.getZone(Constant.Zone.Play, card.getController());
          PlayerZone grave = AllZone.getZone(c);

          if(AllZone.GameAction.isCardInZone(c, grave))
          {
            c.addSemiPermanentAttackBoost(-1);
            c.setController(card.getController());

            play.add(c);
            grave.remove(c);
          }
        }//resolve()
        public boolean canPlay()
        {
          return getCreatures().length != 0;
        }
        public Card[] getCreatures()
        {
          //get all creatures
          CardList list = new CardList();
          list.addAll(AllZone.Human_Graveyard.getCards());
          list.addAll(AllZone.Computer_Graveyard.getCards());
          list = list.getType("Creature");

          return list.toArray();
        }
        public boolean canPlayAI()
        {
          CardList all = new CardList(getCreatures());
          if(all.isEmpty())
            return false;

          Card c = CardFactoryUtil.AI_getBestCreature(all);

          if(2 < c.getNetAttack() && 2 < c.getNetDefense())
            return true;

          return false;
        }

        public void chooseTargetAI()
        {
          Card c[] = getCreatures();
          Card biggest = c[0];
          for(int i = 0; i < c.length; i++)
            if(biggest.getNetAttack() < c[i].getNetAttack())
              biggest = c[i];

          setTargetCard(biggest);
        }
      };//SpellAbility
      card.clearSpellAbility();
      card.addSpellAbility(spell);

      Input target = new Input()
      {
		private static final long serialVersionUID = 9027742835781889044L;

		public void showMessage()
        {
          Object check = AllZone.Display.getChoiceOptional("Select creature", getCreatures());
          if(check != null)
          {
            spell.setTargetCard((Card)check);
            stopSetNext(new Input_PayManaCost(spell));
          }
          else
            stop();
        }//showMessage()

        public Card[] getCreatures()
        {
          //get all creatures
          CardList list = new CardList();
          list.addAll(AllZone.Human_Graveyard.getCards());
          list.addAll(AllZone.Computer_Graveyard.getCards());
          list = list.getType("Creature");

          return list.toArray();
        }
      };//Input
      spell.setBeforePayMana(target);
    }//*************** END ************ END **************************



    //*************** START *********** START **************************
    else if(cardName.equals("Blinding Light"))
    {
      SpellAbility spell = new Spell(card)
      {
		private static final long serialVersionUID = -631672055247954361L;
		
		public void resolve()
        {
          CardList nonwhite = new CardList();
          nonwhite.addAll(AllZone.Human_Play.getCards());
          nonwhite.addAll(AllZone.Computer_Play.getCards());
          nonwhite = nonwhite.filter(new CardListFilter()
          {
            public boolean addCard(Card c)
            {
              return c.isCreature() && (!CardUtil.getColors(c).contains(Constant.Color.White));
            }
          });
          for(int i = 0; i < nonwhite.size(); i++)
            nonwhite.get(i).tap();
        }//resolve()
        public boolean canPlayAI()
        {
          //the computer seems to play this card at stupid times
          return false;
        }
      };
      card.clearSpellAbility();
      card.addSpellAbility(spell);
    }//*************** END ************ END **************************
    
  //*************** START *********** START **************************
    else if(cardName.equals("Ensnare"))
    {
      SpellAbility spell = new Spell(card)
      {
		private static final long serialVersionUID = -5170378205496330425L;
		public void resolve()
        {
          CardList creats = new CardList();
          creats.addAll(AllZone.Human_Play.getCards());
          creats.addAll(AllZone.Computer_Play.getCards());
          creats = creats.filter(new CardListFilter()
          {
            public boolean addCard(Card c)
            {
              return c.isCreature();
            }
          });
          for(int i = 0; i < creats.size(); i++)
        	  creats.get(i).tap();
        }//resolve()
        public boolean canPlayAI()
        {
          return false;
        }
      };
      spell.setDescription("Tap all creatures.");
      spell.setStackDescription(card.getName() + " - Tap all creatures");
      
      final SpellAbility bounce = new Spell(card)
	  {
		private static final long serialVersionUID = 6331598238749406160L;
		public void resolve()
		{
			CardList creats = new CardList();
	        creats.addAll(AllZone.Human_Play.getCards());
	        creats.addAll(AllZone.Computer_Play.getCards());
	        creats = creats.filter(new CardListFilter()
	        {
	           public boolean addCard(Card c)
	           {
	             return c.isCreature();
	           }
	        });
	        for(int i = 0; i < creats.size(); i++)
	          creats.get(i).tap();
		}
		public boolean canPlay()
		{
			PlayerZone play = AllZone.getZone(Constant.Zone.Play, card.getController());
			CardList list = new CardList(play.getCards());
			list = list.getType("Island");
			return list.size() >= 2;
		}
		public boolean canPlayAI()
		{
			return false;
		}
		
	  };
	  bounce.setDescription("You may return three Islands you control to their owner's hand rather than pay Ensnare's mana cost.");
	  bounce.setStackDescription(card.getName() + " - Tap all creatures.");
	  bounce.setManaCost("0");
	  
	  final Input bounceIslands = new Input()
      {
		private static final long serialVersionUID = -8511915834608321343L;
		int stop = 2;
        int count = 0;

        public void showMessage()
        {
          AllZone.Display.showMessage("Select an Island");
          ButtonUtil.disableAll();
        }
        public void selectButtonCancel() {stop();}
        public void selectCard(Card c, PlayerZone zone)
        {
          if(c.getType().contains("Island") && zone.is(Constant.Zone.Play))
          {
            AllZone.GameAction.moveToHand(c);
        	  
            count++;
            if(count == stop) {
            	AllZone.Stack.add(bounce);
            	stop();
            }
          }
        }//selectCard()
      };
      
      bounce.setBeforePayMana(bounceIslands);
      
      Command bounceIslandsAI = new Command()
      {
		private static final long serialVersionUID = 6399831162328201755L;

		public void execute()
    	  {
    		  PlayerZone play = AllZone.getZone(Constant.Zone.Play, Constant.Player.Computer);
    		  CardList list = new CardList(play.getCards());
    		  list = list.getType("Island");
    		  //TODO: sort by tapped
    		  
    		  for (int i=0;i<2;i++)
    		  {
    			  AllZone.GameAction.moveToHand(list.get(i));
    		  }  
    	  }
      };
	  
      bounce.setBeforePayManaAI(bounceIslandsAI);
      
      card.clearSpellAbility();
      card.addSpellAbility(bounce);
      card.addSpellAbility(spell);
    }//*************** END ************ END **************************



    //*************** START *********** START **************************
    else if(cardName.equals("Raise the Alarm"))
    {
      SpellAbility spell = new Spell(card)
      {
		private static final long serialVersionUID = 3022771853846089829L;

		public void resolve()
        {
          PlayerZone play = AllZone.getZone(Constant.Zone.Play, card.getController());
          for (int i = 0; i < 2; i++)
          {
            Card c = new Card();

            c.setOwner(card.getController());
            c.setController(card.getController());

            c.setName("Soldier");
            c.setImageName("W 1 1 Soldier");
            c.setManaCost("W");
            c.setToken(true);

            c.addType("Creature");
            c.addType("Soldier");
            c.setBaseAttack(1);
            c.setBaseDefense(1);

            play.add(c);
          }//for
        }//resolve()
      };
      card.clearSpellAbility();
      card.addSpellAbility(spell);
    }//*************** END ************ END **************************
    
    //*************** START *********** START **************************
    else if(cardName.equals("Dragon Fodder"))
    {
      SpellAbility spell = new Spell(card)
      {
      private static final long serialVersionUID = -6704097906643840324L;

      public void resolve()
        {
          PlayerZone play = AllZone.getZone(Constant.Zone.Play, card.getController());
          for (int i = 0; i < 2; i++)
          {
            Card c = new Card();

            c.setOwner(card.getController());
            c.setController(card.getController());

            c.setName("Goblin");
            c.setImageName("R 1 1 Goblin");
            c.setManaCost("R");
            c.setToken(true);

            c.addType("Creature");
            c.addType("Goblin");
            c.setBaseAttack(1);
            c.setBaseDefense(1);

            play.add(c);
          }//for
        }//resolve()
      };
      card.clearSpellAbility();
      card.addSpellAbility(spell);
    }//*************** END ************ END **************************




    //*************** START *********** START **************************
    else if(cardName.equals("Empty the Warrens"))
    {
      SpellAbility spell = new Spell(card)
      {
		private static final long serialVersionUID = 1439643889038241969L;

		public void resolve()
        {
          int stormCount = 0;

          //get storm count
          CardList list = new CardList();
          list.addAll(AllZone.Human_Graveyard.getCards());
          list.addAll(AllZone.Computer_Graveyard.getCards());

          list.addAll(AllZone.Human_Play.getCards());
          list.addAll(AllZone.Computer_Play.getCards());

          for(int i = 0; i < list.size(); i++)
            if(list.get(i).getTurnInZone() == AllZone.Phase.getTurn())
              stormCount++;

          PlayerZone play = AllZone.getZone(Constant.Zone.Play, card.getController());
          for (int i = 0; i < 2 * stormCount; i++)
          {
            Card c = new Card();

            c.setOwner(card.getController());
            c.setController(card.getController());

            c.setName("Goblin");
            c.setImageName("R 1 1 Goblin");
            c.setManaCost("R");
            c.setToken(true);

            c.addType("Creature");
            c.addType("Goblin");
            c.setBaseAttack(1);
            c.setBaseDefense(1);

            play.add(c);
          }//for
        }//resolve()
      };
      card.clearSpellAbility();
      card.addSpellAbility(spell);
    }//*************** END ************ END **************************



    //*************** START *********** START **************************
    else if(cardName.equals("Feudkiller's Verdict"))
    {
      SpellAbility spell = new Spell(card)
      {
		private static final long serialVersionUID = -5532477141899236266L;

		public void resolve()
        {
          PlayerLife life = AllZone.GameAction.getPlayerLife(card.getController());
          life.addLife(10);

          String opponent = AllZone.GameAction.getOpponent(card.getController());
          PlayerLife oppLife = AllZone.GameAction.getPlayerLife(opponent);

          if(oppLife.getLife() < life.getLife())
            makeToken();
        }//resolve()

        void makeToken()
        {
          PlayerZone play = AllZone.getZone(Constant.Zone.Play, card.getController());
          Card c = new Card();

          c.setOwner(card.getController());
          c.setController(card.getController());

          c.setName("Giant Warrior");
          c.setImageName("W 5 5 Giant Warrior");
          c.setManaCost("W");
          c.setToken(true);

          c.addType("Creature");
          c.addType("Giant");
          c.addType("Warrior");
          c.setBaseAttack(5);
          c.setBaseDefense(5);

          play.add(c);
        }//makeToken()

      };//SpellAbility
      card.clearSpellAbility();
      card.addSpellAbility(spell);
    }//*************** END ************ END **************************


    //*************** START *********** START **************************
    else if(cardName.equals("Reach of Branches"))
    {
      SpellAbility spell = new Spell(card)
      {
		private static final long serialVersionUID = 2723115210677439611L;

		public void resolve()
        {
          PlayerZone play = AllZone.getZone(Constant.Zone.Play, card.getController());

          //make token
          Card c = new Card();

          c.setOwner(card.getController());
          c.setController(card.getController());

          c.setName("Treefolk Shaman");
          c.setImageName("G 2 5 Treefolk Shaman");
          c.setManaCost("G");
          c.setToken(true);

          c.addType("Creature");
          c.addType("Treefolk");
          c.addType("Shaman");
          c.setBaseAttack(2);
          c.setBaseDefense(5);

          play.add(c);
        }//resolve()
      };
      card.clearSpellAbility();
      card.addSpellAbility(spell);
    }//*************** END ************ END **************************



    //*************** START *********** START **************************
    else if(cardName.equals("Pyroclasm"))
    {
      SpellAbility spell = new Spell(card)
      {
		private static final long serialVersionUID = 8035123529251645470L;
		
		public void resolve()
        {
          CardList all = new CardList();
          all.addAll(AllZone.Human_Play.getCards());
          all.addAll(AllZone.Computer_Play.getCards());
          all = all.getType("Creature");

          for(int i = 0; i < all.size(); i++)
          {
            if (CardFactoryUtil.canDamage(card,all.get(i)))
        	  all.get(i).addDamage(2, card);
          }
        }
        public boolean canPlayAI()
        {
          CardList human    = new CardList(AllZone.Human_Play.getCards());
          CardList computer = new CardList(AllZone.Computer_Play.getCards());

          human    = human.getType("Creature");
          computer = computer.getType("Creature");

          human = CardListUtil.filterToughness(human, 2);
          computer = CardListUtil.filterToughness(computer, 2);

          //the computer will at least destroy 2 more human creatures
          return computer.size() < human.size()-1;
        }//canPlayAI()
      };
      card.clearSpellAbility();
      card.addSpellAbility(spell);
    }//*************** END ************ END **************************
    
  //*************** START *********** START **************************
    else if(cardName.equals("Volcanic Fallout"))
    {
      SpellAbility spell = new Spell(card)
      {
		private static final long serialVersionUID = 8274309635261086286L;
		public void resolve()
        {
          CardList all = new CardList();
          all.addAll(AllZone.Human_Play.getCards());
          all.addAll(AllZone.Computer_Play.getCards());
          all = all.getType("Creature");

          for(int i = 0; i < all.size(); i++)
          {
            if (CardFactoryUtil.canDamage(card,all.get(i)))
        	  all.get(i).addDamage(2, card);
          }
          
          PlayerLife compLife = AllZone.GameAction.getPlayerLife(Constant.Player.Computer);
          compLife.subtractLife(2);
          
          PlayerLife humLife = AllZone.GameAction.getPlayerLife(Constant.Player.Human);
          humLife.subtractLife(2);
          
        }
        public boolean canPlayAI()
        {
          CardList human    = new CardList(AllZone.Human_Play.getCards());
          CardList computer = new CardList(AllZone.Computer_Play.getCards());

          human    = human.getType("Creature");
          computer = computer.getType("Creature");

          human = CardListUtil.filterToughness(human, 2);
          computer = CardListUtil.filterToughness(computer, 2);

          PlayerLife compLife = AllZone.GameAction.getPlayerLife(Constant.Player.Computer);
          
          //the computer will at least destroy 2 more human creatures
          return computer.size() < human.size()-1 && compLife.getLife() > 3;
        }//canPlayAI()
      };
      card.clearSpellAbility();
      card.addSpellAbility(spell);
    }//*************** END ************ END **************************



    //*************** START *********** START **************************
    else if(cardName.equals("Flamebreak"))
    {
      SpellAbility spell = new Spell(card)
      {
		private static final long serialVersionUID = -4224693616606508949L;
		
		public boolean canPlayAI()
        {
          return AllZone.Computer_Life.getLife()>3;
        }
        public void resolve()
        {
          CardList all = new CardList();
          all.addAll(AllZone.Human_Play.getCards());
          all.addAll(AllZone.Computer_Play.getCards());
          all = all.getType("Creature");

          for(int i = 0; i < all.size(); i++)
            if(! all.get(i).getKeyword().contains("Flying"))
            {
            	if (CardFactoryUtil.canDamage(card, all.get(i)))
            	{            		
            		all.get(i).setShield(0);
            		all.get(i).addDamage(3, card);
            	}
            }

          AllZone.Human_Life.subtractLife(3);
          AllZone.Computer_Life.subtractLife(3);
        }
      };
      card.clearSpellAbility();
      card.addSpellAbility(spell);
    }//*************** END ************ END **************************


    //*************** START *********** START **************************
    else if(cardName.equals("Renewed Faith") || cardName.equals("Rejuvenate") || cardName.equals("Dosan's Oldest Chant") || cardName.equals("Nourish"))
    {
      SpellAbility spell = new Spell(card)
      {
		private static final long serialVersionUID = -1133816506198725425L;

		public boolean canPlay()
        {
          setStackDescription(card.getName() +" - " +card.getController() +" gains 6 life.");
          return super.canPlay();
        }

        public void resolve()
        {
          PlayerLife life = AllZone.GameAction.getPlayerLife(card.getController());
          life.addLife(6);
        }
      };
      String desc = "You gain 6 life.";
      
      if (cardName.equals("Renewed Faith"))
    	   desc = desc + "\r\n\r\nWhen you cycle Renewed Faith, you may gain 2 life.\r\n";
      
      spell.setDescription(desc);

      card.clearSpellAbility();
      card.addSpellAbility(spell);
      if (cardName.equals("Renewed Faith")) {
//    	  card.addSpellAbility(CardFactoryUtil.ability_cycle(card, "1 W"));
    	  card.addCycleCommand(new Command(){
			private static final long serialVersionUID = 7699412574052780825L;

			public void execute() 
    		{
				PlayerLife life = AllZone.GameAction.getPlayerLife(card.getController());
				life.addLife(2);
    		}
    	  });
      }
    }//*************** END ************ END **************************



    //*************** START *********** START **************************
    else if(cardName.equals("TestLife"))
    {
      SpellAbility ability1 = new Ability_Activated(card, "1")
      {
		private static final long serialVersionUID = -7597743923692184213L;

		public void resolve()
        {
          PlayerLife life = AllZone.GameAction.getPlayerLife(card.getController());
          life.addLife(1);
        }
      };
      ability1.setStackDescription(card.getController() +" gains 1 life");
      ability1.setDescription("1: gain 1 life");
      card.addSpellAbility(ability1);

      SpellAbility ability2 = new Ability_Activated(card, "1")
      {
		private static final long serialVersionUID = 1423759257249171223L;

		public void resolve()
        {
          PlayerLife life = AllZone.GameAction.getPlayerLife(card.getController());
          life.subtractLife(1);
        }
      };
      ability2.setStackDescription(card.getController() +" looses 1 life");
      ability2.setDescription("1: loose 1 life");
      card.addSpellAbility(ability2);
    }//*************** END ************ END **************************


    //*************** START *********** START **************************
    else if(cardName.equals("Life Burst"))
    {
      final SpellAbility spell = new Spell(card)
      {
		private static final long serialVersionUID = 5653342880372240806L;

		public void resolve()
        {
          CardList count = new CardList();
          count.addAll(AllZone.Human_Graveyard.getCards());
          count.addAll(AllZone.Computer_Graveyard.getCards());
          count = count.filter(new CardListFilter()
          {
            public boolean addCard(Card c)
            {
              return c.getName().equals("Life Burst");
            }
          });

          PlayerLife life = AllZone.GameAction.getPlayerLife(getTargetPlayer());
          life.addLife(4 + (4 * count.size()));
        }
      };
      spell.setChooseTargetAI(CardFactoryUtil.AI_targetComputer());
      card.clearSpellAbility();
      card.addSpellAbility(spell);

      spell.setBeforePayMana(CardFactoryUtil.input_targetPlayer(spell));
    }//*************** END ************ END **************************
    
    //*************** START *********** START **************************
    else if(cardName.equals("Accumulated Knowledge"))
    {
      final SpellAbility spell = new Spell(card)
      {
		private static final long serialVersionUID = -7650377883588723237L;

		public void resolve()
        {
          CardList count = new CardList();
          count.addAll(AllZone.Human_Graveyard.getCards());
          count.addAll(AllZone.Computer_Graveyard.getCards());
          count = count.filter(new CardListFilter()
          {
            public boolean addCard(Card c)
            {
              return c.getName().equals("Accumulated Knowledge");
            }
          });

          for (int i=0;i<=count.size();i++)
          {
        	  AllZone.GameAction.drawCard(card.getController());
          }
        }
      };
      spell.setDescription("Draw a card, then draw cards equal to the number of cards named Accumulated Knowledge in all graveyards.");
      spell.setStackDescription(cardName + " - Draw a card, then draw cards equal to the number of cards named Accumulated Knowledge in all graveyards.");
      card.clearSpellAbility();
      card.addSpellAbility(spell);

    }//*************** END ************ END **************************


    //*************** START *********** START **************************
    else if(cardName.equals("Wit's End"))
    {
      final SpellAbility spell = new Spell(card)
      {
		private static final long serialVersionUID = 3829311830858468029L;

		public void resolve()
        {
          PlayerZone hand = AllZone.getZone(Constant.Zone.Hand, getTargetPlayer());
          Card c[] = hand.getCards();
          for(int i = 0; i < c.length; i++)
            AllZone.GameAction.discard(c[i]);
        }
      };
      spell.setChooseTargetAI(CardFactoryUtil.AI_targetHuman());
      card.clearSpellAbility();
      card.addSpellAbility(spell);

      spell.setBeforePayMana(CardFactoryUtil.input_targetPlayer(spell));
    }//*************** END ************ END **************************


  //*************** START *********** START **************************
    else if(cardName.equals("Cranial Extraction"))
    {
      final SpellAbility spell = new Spell(card)
      {
		private static final long serialVersionUID = 8127696608769903507L;
		
		@SuppressWarnings("unchecked") // Comparator
		public void resolve()
        {
          String opponent = AllZone.GameAction.getOpponent(card.getController());
          Card choice = null;

          //check for no cards in library
          PlayerZone library = AllZone.getZone(Constant.Zone.Library, opponent);

          if(library.size() == 0) //this is not right, but leaving it in here for now.
            return;

          //human chooses
          if(opponent.equals(Constant.Player.Computer))
          {
            CardList all = AllZone.CardFactory.getAllCards();
            all.sort(new Comparator()
            {
              public int compare(Object a1, Object b1)
              {
                Card a = (Card)a1;
                Card b = (Card)b1;

                return a.getName().compareTo(b.getName());
              }
            });
            choice = (Card) AllZone.Display.getChoice("Choose", removeLand(all.toArray()));

            Card[] showLibrary = library.getCards();
            Comparator com = new TableSorter(new CardList(showLibrary), 2, true);
            Arrays.sort(showLibrary, com);

            AllZone.Display.getChoiceOptional("Opponent's Library", showLibrary);
            AllZone.GameAction.shuffle(opponent);
          }//if
          else//computer chooses
          {
            //the computer cheats by choosing a creature in the human players library or hand
            CardList all = new CardList();
            all.addAll(AllZone.Human_Hand.getCards());
            all.addAll(AllZone.Human_Library.getCards());

            CardList four = all.filter(new CardListFilter()
            {
              public boolean addCard(Card c)
              {
                if(c.isLand())
                  return false;

                return 3 < CardUtil.getConvertedManaCost(c.getManaCost());
              }
            });
            if(! four.isEmpty())
              choice = CardUtil.getRandom(four.toArray());
            else
              choice = CardUtil.getRandom(all.toArray());

          }//else
          remove(choice, opponent);
          AllZone.GameAction.shuffle(opponent);
        }//resolve()

        void remove(Card c, String player)
        {
          PlayerZone hand    = AllZone.getZone(Constant.Zone.Hand, player);
          PlayerZone grave   = AllZone.getZone(Constant.Zone.Graveyard, player);
          PlayerZone library = AllZone.getZone(Constant.Zone.Library, player);

          CardList all = new CardList();
          all.addAll(hand.getCards());
          all.addAll(grave.getCards());
          all.addAll(library.getCards());

          for(int i = 0; i < all.size(); i++)
            if(all.get(i).getName().equals(c.getName())){
               if(player.equals(Constant.Player.Human)){
                  AllZone.GameAction.moveTo(AllZone.Human_Removed, all.get(i));
               }
               else{
                  AllZone.GameAction.moveTo(AllZone.Computer_Removed, all.get(i));
               }
            }
        }//remove()

        public boolean canPlayAI()
        {
          Card[] c = removeLand(AllZone.Human_Library.getCards());
          return 0 < c.length;
        }
        Card[] removeLand(Card[] in)
        {
          CardList c = new CardList(in);
          c = c.filter(new CardListFilter()
          {
            public boolean addCard(Card c)
            {
              return !c.isLand();
            }
          });
          return c.toArray();
        }//removeLand()
      };//SpellAbility spell
      card.clearSpellAbility();
      card.addSpellAbility(spell);

      spell.setBeforePayMana(new Input_PayManaCost(spell));
      spell.setStackDescription(card.getName() +" - targeting opponent");
    }//*************** END ************ END **************************




    //*************** START *********** START **************************
    else if(cardName.equals("Coercion"))
    {
      final SpellAbility spell = new Spell(card)
      {
		private static final long serialVersionUID = 7200945225788294439L;

		public void resolve()
        {
          String opponent = AllZone.GameAction.getOpponent(card.getController());
          Card choice = null;

          //check for no cards in hand on resolve
          PlayerZone hand = AllZone.getZone(Constant.Zone.Hand, opponent);
          Card[] handChoices = removeLand(hand.getCards());

          if(handChoices.length == 0)
            return;

          //human chooses
          if(opponent.equals(Constant.Player.Computer))
          {
            choice = (Card) AllZone.Display.getChoice("Choose", handChoices);
          }
          else//computer chooses
          {
            choice = CardUtil.getRandom(handChoices);
          }

          AllZone.GameAction.discard(choice);
        }//resolve()

        public boolean canPlayAI()
        {
          Card[] c = removeLand(AllZone.Human_Hand.getCards());
          return 0 < c.length;
        }

        Card[] removeLand(Card[] in)
        {
          return in;
        }//removeLand()
      };//SpellAbility spell
      card.clearSpellAbility();
      card.addSpellAbility(spell);

      spell.setBeforePayMana(new Input_PayManaCost(spell));
    }//*************** END ************ END **************************
    
    //*************** START *********** START **************************
    else if(cardName.equals("Brainbite"))
    {
      final SpellAbility spell = new Spell(card)
      {
		private static final long serialVersionUID = 6967309558624188256L;

		public void resolve()
        {
          String opponent = AllZone.GameAction.getOpponent(card.getController());
          Card choice = null;

          //check for no cards in hand on resolve
          PlayerZone hand = AllZone.getZone(Constant.Zone.Hand, opponent);
          Card[] handChoices = removeLand(hand.getCards());

          if(handChoices.length == 0){
        	  AllZone.GameAction.drawCard(card.getController());
        	  return;
          }
            

          //human chooses
          if(opponent.equals(Constant.Player.Computer))
          {
            choice = (Card) AllZone.Display.getChoice("Choose", handChoices);
          }
          else//computer chooses
          {
            choice = CardUtil.getRandom(handChoices);
          }

          AllZone.GameAction.discard(choice);
          AllZone.GameAction.drawCard(card.getController());
        }//resolve()

        public boolean canPlayAI()
        {
          Card[] c = removeLand(AllZone.Human_Hand.getCards());
          return 0 < c.length;
        }

        Card[] removeLand(Card[] in)
        {
          return in;
        }//removeLand()
      };//SpellAbility spell
      card.clearSpellAbility();
      card.addSpellAbility(spell);

      spell.setBeforePayMana(new Input_PayManaCost(spell));
    }//*************** END ************ END **************************




    //*************** START *********** START **************************
    else if(cardName.equals("Thoughtseize") || cardName.equals("Distress"))
    {
      final SpellAbility spell = new Spell(card)
      {
		private static final long serialVersionUID = 5338238621454661783L;

		public void resolve()
        {
          if(cardName.equals("Thoughtseize"))
            AllZone.GameAction.getPlayerLife(card.getController()).subtractLife(2);

          Card choice = null;

          //check for no cards in hand on resolve
          PlayerZone hand = AllZone.getZone(Constant.Zone.Hand, getTargetPlayer());
          CardList fullHand = new CardList(hand.getCards());
          Card[] handChoices = removeLand(hand.getCards());

          if (fullHand.size() > 0 && card.getController().equals(Constant.Player.Human))
        	  AllZone.Display.getChoice("Revealing hand", fullHand.toArray());
        	  
          if(handChoices.length == 0)
            return;

          //human chooses
          if(card.getController().equals(Constant.Player.Human))
          {
            choice = (Card) AllZone.Display.getChoice("Choose", handChoices);
          }
          else//computer chooses
          {
            choice = CardUtil.getRandom(handChoices);
          }

          AllZone.GameAction.discard(choice);
        }//resolve()

        public boolean canPlayAI()
        {
          Card[] c = removeLand(AllZone.Human_Hand.getCards());
          return 0 < c.length;
        }

        Card[] removeLand(Card[] in)
        {
          CardList c = new CardList(in);
          c = c.filter(new CardListFilter()
          {
            public boolean addCard(Card c)
            {
              return !c.isLand();
            }
          });
          return c.toArray();
        }//removeLand()
      };//SpellAbility spell
      spell.setChooseTargetAI(CardFactoryUtil.AI_targetHuman());
      card.clearSpellAbility();
      card.addSpellAbility(spell);

      spell.setBeforePayMana(CardFactoryUtil.input_targetPlayer(spell));
    }//*************** END ************ END **************************

    //*************** START *********** START **************************
    else if(cardName.equals("Echoing Decay"))
    {
      final SpellAbility spell = new Spell(card)
      {
		private static final long serialVersionUID = 3154935854257358023L;

		public boolean canPlayAI()
        {
          CardList c = getCreature();
          if(c.isEmpty())
            return false;
          else
          {
            setTargetCard(c.get(0));
            return true;
          }
        }//canPlayAI()
        CardList getCreature()
        {
          CardList out = new CardList();
          CardList list = CardFactoryUtil.AI_getHumanCreature("Flying", card, true);
          list.shuffle();

          for(int i = 0; i < list.size(); i++)
            if((list.get(i).getNetAttack() >= 2) && (list.get(i).getNetDefense() <= 2))
              out.add(list.get(i));

          //in case human player only has a few creatures in play, target anything
          if(out.isEmpty() &&
              0 < CardFactoryUtil.AI_getHumanCreature(2, card, true).size() &&
             3 > CardFactoryUtil.AI_getHumanCreature(card, true).size())
          {
            out.addAll(CardFactoryUtil.AI_getHumanCreature(2, card, true).toArray());
            CardListUtil.sortFlying(out);
          }
          return out;
        }//getCreature()


        public void resolve()
        {
          if(AllZone.GameAction.isCardInPlay(getTargetCard()) && CardFactoryUtil.canTarget(card, getTargetCard()) )
          {
            final Card c = getTargetCard();
            
            c.addTempAttackBoost(-2);
          	c.addTempDefenseBoost(-2);

          	AllZone.EndOfTurn.addUntil(new Command()
          	{
				private static final long serialVersionUID = 1327455269456577020L;

				public void execute()
          		{
          			c.addTempAttackBoost(2);
          			c.addTempDefenseBoost(2);
          		}
          	});

            //get all creatures
            CardList list = new CardList();
            list.addAll(AllZone.Human_Play.getCards());
            list.addAll(AllZone.Computer_Play.getCards());

            list = list.getName(getTargetCard().getName());
            list.remove(getTargetCard());
              
            if (!getTargetCard().isFaceDown())
	            for(int i = 0; i < list.size(); i++)
	            {
	            	final Card crd = list.get(i);
	            	
	            	crd.addTempAttackBoost(-2);
	            	crd.addTempDefenseBoost(-2);
	            	
	            	AllZone.EndOfTurn.addUntil(new Command()
	              	{
						private static final long serialVersionUID = 5151337777143949221L;

						public void execute()
	              		{
	              			crd.addTempAttackBoost(2);
	              			crd.addTempDefenseBoost(2);
	              		}
	              	});
	            	//list.get(i).addDamage(2);
	            }
	              
          }//in play?
        }//resolve()
      };//SpellAbility
      card.clearSpellAbility();
      card.addSpellAbility(spell);

      spell.setBeforePayMana(CardFactoryUtil.input_targetCreature(spell));
    }//*************** END ************ END **************************
    
    //*************** START *********** START **************************
    else if(cardName.equals("Maelstrom Pulse"))
    {
      final SpellAbility spell = new Spell(card)
      {
		private static final long serialVersionUID = -4050843868789582138L;

		public boolean canPlayAI()
        {
          CardList c = getCreature();
          if(c.isEmpty())
            return false;
          else
          {
            setTargetCard(c.get(0));
            return true;
          }
        }//canPlayAI()
        CardList getCreature()
        {
          CardList out = new CardList();
          CardList list = CardFactoryUtil.AI_getHumanCreature("Flying", card, true);
          list.shuffle();

          for(int i = 0; i < list.size(); i++)
            if((list.get(i).getNetAttack() >= 2) && (list.get(i).getNetDefense() <= 2))
              out.add(list.get(i));

          //in case human player only has a few creatures in play, target anything
          if(out.isEmpty() &&
              0 < CardFactoryUtil.AI_getHumanCreature(2, card, true).size() &&
             3 > CardFactoryUtil.AI_getHumanCreature(card, true).size())
          {
            out.addAll(CardFactoryUtil.AI_getHumanCreature(2, card, true).toArray());
            CardListUtil.sortFlying(out);
          }
          return out;
        }//getCreature()


        public void resolve()
        {
          if(AllZone.GameAction.isCardInPlay(getTargetCard()) && CardFactoryUtil.canTarget(card, getTargetCard()) )
          {

        	AllZone.GameAction.destroy(getTargetCard());
        	
            if(! getTargetCard().isToken() && !getTargetCard().isFaceDown())
            {
              //get all creatures
              CardList list = new CardList();
              list.addAll(AllZone.Human_Play.getCards());
              list.addAll(AllZone.Computer_Play.getCards());

              list = list.getName(getTargetCard().getName());
              list.remove(getTargetCard());
              
              if (!getTargetCard().isFaceDown())
	              for(int i = 0; i < list.size(); i++)
	                AllZone.GameAction.destroy(list.get(i));
            }//is token?
          }//in play?
        }//resolve()
      };//SpellAbility
      card.clearSpellAbility();
      card.addSpellAbility(spell);

      Input target = new Input()
      {
		private static final long serialVersionUID = -4947592326270275532L;
		
		public void showMessage()
    	{
    	  AllZone.Display.showMessage("Select target non-land permanent for " +spell.getSourceCard());
    	  ButtonUtil.enableOnlyCancel();
    	}
    	public void selectButtonCancel() {stop();}
    	public void selectCard(Card card, PlayerZone zone)
    	{
    	  if(zone.is(Constant.Zone.Play) && !card.isLand())
    	  {
    		spell.setTargetCard(card);
    		stopSetNext(new Input_PayManaCost(spell));
    	  }
    	}
      };//Input
      
      spell.setBeforePayMana(target);
    }//*************** END ************ END **************************
    

    
    //*************** START *********** START **************************
    else if(cardName.equals("Sunlance"))
    {
      final SpellAbility spell = new Spell(card)
      {
		private static final long serialVersionUID = -8327380630150660908L;
		
		int damage = 3;
        Card check;
        public boolean canPlayAI()
        {
          check = getFlying();
          return check != null;
        }
        public void chooseTargetAI()
        {
          Card c = getFlying();
          if((c == null) || (! check.equals(c)))
            throw new RuntimeException(card +" error in chooseTargetAI() - Card c is " +c +",  Card check is " +check);

          setTargetCard(c);
        }//chooseTargetAI()

        //uses "damage" variable
        Card getFlying()
        {
          CardList flying = CardFactoryUtil.AI_getHumanCreature("Flying", card, true);
          for(int i = 0; i < flying.size(); i++)
            if(flying.get(i).getNetDefense() <= damage &&
               (!CardUtil.getColors(flying.get(i)).contains(Constant.Color.White)))
          {
              return flying.get(i);
          }
          return null;
        }

        public void resolve()
        {
          if(AllZone.GameAction.isCardInPlay(getTargetCard()) && CardFactoryUtil.canTarget(card, getTargetCard()))
          {
            Card c = getTargetCard();
            c.addDamage(damage, card);
          }
        }//resolve()
      };//SpellAbility
      card.clearSpellAbility();
      card.addSpellAbility(spell);

      //target
      Input target = new Input()
      {
		private static final long serialVersionUID = -579427555773493417L;
		
		public void showMessage()
        {
          AllZone.Display.showMessage("Select target non-white creature for " +spell.getSourceCard());
          ButtonUtil.enableOnlyCancel();
        }
        public void selectButtonCancel() {stop();}
        public void selectCard(Card card, PlayerZone zone)
        {
          if((!CardUtil.getColors(card).contains(Constant.Color.White)) &&
             card.isCreature()                                          &&
             zone.is(Constant.Zone.Play))
          {
            spell.setTargetCard(card);
            stopSetNext(new Input_PayManaCost(spell));
          }
        }
      };//SpellAbility - target

      spell.setBeforePayMana(target);
    }//*************** END ************ END **************************


/*
    //*************** START *********** START **************************
    else if(cardName.equals("Saltblast"))
    {
      final SpellAbility spell = new Spell(card)
      {
		private static final long serialVersionUID = -8701870029034823295L;
		
		Card check;
        public boolean canPlayAI()
        {
          check = getFlying();
          return check != null;
        }
        public void chooseTargetAI()
        {
          Card c = getFlying();
          if((c == null) || (! check.equals(c)))
            throw new RuntimeException(card +" error in chooseTargetAI() - Card c is " +c +",  Card check is " +check);

          setTargetCard(c);
        }//chooseTargetAI()

        //uses "damage" variable
        Card getFlying()
        {
          CardList flying = CardFactoryUtil.AI_getHumanCreature("Flying", card, true);
          for(int i = 0; i < flying.size(); i++)
            if(!CardUtil.getColors(flying.get(i)).contains(Constant.Color.White))
          {
              return flying.get(i);
          }
          return null;
        }

        public void resolve()
        {
          if(AllZone.GameAction.isCardInPlay(getTargetCard()) && CardFactoryUtil.canTarget(card, getTargetCard()) )
          {
            Card c = getTargetCard();

            if(AllZone.GameAction.isCardInPlay(c))
              AllZone.GameAction.destroy(c);
          }
        }//resolve()
      };//SpellAbility
      card.clearSpellAbility();
      card.addSpellAbility(spell);

      //target
      Input target = new Input()
      {
		private static final long serialVersionUID = 6040042391157137555L;
		
		public void showMessage()
        {
          AllZone.Display.showMessage("Select target non-white permanent for " +spell.getSourceCard());
          ButtonUtil.enableOnlyCancel();
        }
        public void selectButtonCancel() {stop();}
        public void selectCard(Card card, PlayerZone zone)
        {
          if((!CardUtil.getColors(card).contains(Constant.Color.White)) &&
             zone.is(Constant.Zone.Play))
          {
            spell.setTargetCard(card);
            stopSetNext(new Input_PayManaCost(spell));
          }
        }
      };//SpellAbility - target

      spell.setBeforePayMana(target);
    }//*************** END ************ END **************************
*/


    //*************** START *********** START **************************
    else if(cardName.equals("Firebolt"))
    {
      final SpellAbility spell = new Spell(card)
      {
		private static final long serialVersionUID = -4100322462753117988L;
		
		int damage = 2;
        Card check;
        
        public boolean canPlayAI()
        {
          PlayerZone compHand = AllZone.getZone(Constant.Zone.Hand, Constant.Player.Computer);
          CardList hand = new CardList(compHand.getCards());
          
            
          if(AllZone.Human_Life.getLife() <= damage)
            return AllZone.GameAction.isCardInZone(card, compHand);
               
          if (hand.size() >= 8)
            return true && AllZone.GameAction.isCardInZone(card, compHand);

          check = getFlying();
          return check != null && AllZone.GameAction.isCardInZone(card, compHand);
        }
        public void chooseTargetAI()
        {
          if(AllZone.Human_Life.getLife() <= damage)
          {
            setTargetPlayer(Constant.Player.Human);
            return;
          }

          Card c = getFlying();
          if((c == null) || (! check.equals(c)))
            throw new RuntimeException(card +" error in chooseTargetAI() - Card c is " +c +",  Card check is " +check);

          setTargetCard(c);
        }//chooseTargetAI()

        //uses "damage" variable
        Card getFlying()
        {
          CardList flying = CardFactoryUtil.AI_getHumanCreature("Flying", card, true);
          for(int i = 0; i < flying.size(); i++)
            if(flying.get(i).getNetDefense() <= damage)
              return flying.get(i);

          return null;
        }

        public void resolve()
        {
          if(getTargetCard() != null)
          {
            if(AllZone.GameAction.isCardInPlay(getTargetCard()) && CardFactoryUtil.canTarget(card, getTargetCard()))
            {
              Card c = getTargetCard();
              c.addDamage(damage, card);
            }
          }
          else
            AllZone.GameAction.getPlayerLife(getTargetPlayer()).subtractLife(damage);
        }
      };//SpellAbility
      
      final SpellAbility flashback = new Spell(card)
      {

		private static final long serialVersionUID = -4811352682106571233L;
		int damage = 2;
        Card check;
        
        public boolean canPlay()
        {
        	PlayerZone grave = AllZone.getZone(Constant.Zone.Graveyard ,card.getController());
			String phase         = AllZone.Phase.getPhase();
			String activePlayer  = AllZone.Phase.getActivePlayer();
			
			return AllZone.GameAction.isCardInZone(card, grave) && 
					((phase.equals(Constant.Phase.Main1) || phase.equals(Constant.Phase.Main2)) &&
				    card.getController().equals(activePlayer) && AllZone.Stack.size() == 0 );
        }
        
        public boolean canPlayAI()
        {
          if(AllZone.Human_Life.getLife() <= damage)
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

          Card c = getFlying();
          if((c == null) || (! check.equals(c)))
            throw new RuntimeException(card +" error in chooseTargetAI() - Card c is " +c +",  Card check is " +check);

          setTargetCard(c);
        }//chooseTargetAI()

        //uses "damage" variable
        Card getFlying()
        {
          CardList flying = CardFactoryUtil.AI_getHumanCreature("Flying", card, true);
          for(int i = 0; i < flying.size(); i++)
            if(flying.get(i).getNetDefense() <= damage)
              return flying.get(i);

          return null;
        }

        public void resolve()
        {
          PlayerZone grave = AllZone.getZone(Constant.Zone.Graveyard , card.getController());
		  PlayerZone removed = AllZone.getZone(Constant.Zone.Removed_From_Play, card.getController());
        	
          if(getTargetCard() != null)
          {
            if(AllZone.GameAction.isCardInPlay(getTargetCard()) && CardFactoryUtil.canTarget(card, getTargetCard()))
            {
              Card c = getTargetCard();
              c.addDamage(damage, card);
            }
          }
          else
            AllZone.GameAction.getPlayerLife(getTargetPlayer()).subtractLife(damage);
          
          grave.remove(card);
		  removed.add(card);
          
        }
      };//flashback
      flashback.setManaCost("4 R");
      flashback.setBeforePayMana(CardFactoryUtil.input_targetCreaturePlayer(flashback,true));
      flashback.setDescription("Flashback: 4R");
      
      card.clearSpellAbility();
      card.addSpellAbility(spell);
      card.addSpellAbility(flashback);

      spell.setBeforePayMana(CardFactoryUtil.input_targetCreaturePlayer(spell,true));
      card.setFlashback(true);
    }//*************** END ************ END **************************

    
/*
    //*************** START *********** START **************************
    else if(cardName.equals("Spark Spray"))
    {
      final SpellAbility spell = new Spell(card)
      {
		private static final long serialVersionUID = 6619448976712515187L;

		public void chooseTargetAI()
        {
          setStackDescription("Spark Spray cycling - Computer draws a card");
        }//chooseTargetAI()

        public void resolve()
        {
          if(card.getController().equals(Constant.Player.Computer))
          {
            AllZone.GameAction.drawCard(Constant.Player.Computer);
            return;
          }

          if(getTargetCard() != null)
          {
            if(AllZone.GameAction.isCardInPlay(getTargetCard()) && CardFactoryUtil.canTarget(card, getTargetCard()) )
            {
              Card c = getTargetCard();
              c.addDamage(1, card);
            }
          }
          else
            AllZone.GameAction.getPlayerLife(getTargetPlayer()).subtractLife(1);
        }
      };//SpellAbility
      spell.setDescription("Spark Spray deals 1 damage to target creature or player.");
      card.clearSpellAbility();

      card.addSpellAbility(spell);
      card.addSpellAbility(CardFactoryUtil.ability_cycle(card, "R"));

      spell.setBeforePayMana(CardFactoryUtil.input_targetCreaturePlayer(spell,true));
    }//*************** END ************ END **************************
*/
    

    //*************** START *********** START **************************
    else if(cardName.equals("Erratic Explosion"))
    {
      final SpellAbility spell = new Spell(card)
      {
		private static final long serialVersionUID = -6003403347798646257L;
		
		int damage = 3;
        Card check;
        public boolean canPlayAI()
        {
          if(AllZone.Human_Life.getLife() <= damage)
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

          Card c = getFlying();
          if((c == null) || (! check.equals(c)))
            throw new RuntimeException(card +" error in chooseTargetAI() - Card c is " +c +",  Card check is " +check);

          setTargetCard(c);
        }//chooseTargetAI()

        //uses "damage" variable
        Card getFlying()
        {
          CardList flying = CardFactoryUtil.AI_getHumanCreature("Flying", card, true);
          for(int i = 0; i < flying.size(); i++)
            if(flying.get(i).getNetDefense() <= damage)
              return flying.get(i);

          return null;
        }

        public void resolve()
        {
          int damage = getDamage();

          if(getTargetCard() != null)
          {
            if(AllZone.GameAction.isCardInPlay(getTargetCard()) && CardFactoryUtil.canTarget(card, getTargetCard()))
            {
              javax.swing.JOptionPane.showMessageDialog(null, "Erratic Explosion causes " +damage +" to " +getTargetCard());

              Card c = getTargetCard();
              c.addDamage(damage, card);
            }
          }
          else
          {
            javax.swing.JOptionPane.showMessageDialog(null, "Erratic Explosion causes " +damage +" to " +getTargetPlayer());
            AllZone.GameAction.getPlayerLife(getTargetPlayer()).subtractLife(damage);
          }
        }
        //randomly choose a nonland card
        int getDamage()
        {
          PlayerZone library = AllZone.getZone(Constant.Zone.Library, card.getController());
          CardList notLand = new CardList(library.getCards());
          notLand = notLand.filter(new CardListFilter()
          {
            public boolean addCard(Card c)
            {
              return ! c.isLand();
            }
          });
          notLand.shuffle();

          if(notLand.isEmpty())
            return 0;

          Card card = notLand.get(0);
          return CardUtil.getConvertedManaCost(card.getSpellAbility()[0]);
        }
      };//SpellAbility
      card.clearSpellAbility();

      card.addSpellAbility(spell);

      spell.setBeforePayMana(CardFactoryUtil.input_targetCreaturePlayer(spell, true));
    }//*************** END ************ END **************************

    //*************** START *********** START **************************
    else if(cardName.equals("Hidetsugu's Second Rite"))
    {
      final SpellAbility spell = new Spell(card)
      {
		private static final long serialVersionUID = 176857775451818523L;
		
		public void resolve()
        {
          PlayerLife life = AllZone.GameAction.getPlayerLife(getTargetPlayer());
          if(life.getLife() == 10)
            life.subtractLife(10);
        }
        public boolean canPlay()
        {
          String opponent = AllZone.GameAction.getOpponent(card.getController());
          PlayerLife p = AllZone.GameAction.getPlayerLife(opponent);
          return p.getLife() == 10;
        }
      };
      spell.setChooseTargetAI(CardFactoryUtil.AI_targetHuman());
      card.clearSpellAbility();
      card.addSpellAbility(spell);

      spell.setBeforePayMana(CardFactoryUtil.input_targetPlayer(spell));
    }//*************** END ************ END **************************
    
    //*************** START *********** START **************************
    else if (cardName.equals("Soulscour") )
    {
       SpellAbility spell = new Spell(card)
       {

		private static final long serialVersionUID = -4554873222565897972L;

		public void resolve()
          {
             CardList all = new CardList();
             all.addAll(AllZone.Human_Play.getCards());
             all.addAll(AllZone.Computer_Play.getCards());
             all = all.filter(new CardListFilter()
             {
            	public boolean addCard(Card c)
            	{
            		return c.isPermanent() && !c.isArtifact();
            	}
             });
             CardListUtil.sortByDestroyEffect(all);
             
             for (int i = 0; i < all.size(); i++)
             {
                Card c = all.get(i);
                AllZone.GameAction.destroy(c);
             }
          }// resolve()

          public boolean canPlayAI()
          {
             CardList human = new CardList(AllZone.Human_Play.getCards());
             CardList computer = new CardList(AllZone.Computer_Play.getCards());

             human = human.getType("Creature");
             computer = computer.getType("Creature");

             // the computer will at least destroy 2 more human creatures
             return computer.size() < human.size() - 1 ||
             (AllZone.Computer_Life.getLife() < 7 && !human.isEmpty());
          }
       };// SpellAbility
       card.clearSpellAbility();
       card.addSpellAbility(spell);
    }// *************** END ************ END **************************


    //*************** START *********** START **************************
    else if(cardName.equals("March of Souls"))
    {
      final SpellAbility spell = new Spell(card)
      {
		private static final long serialVersionUID = -1468254925312413359L;
		
		public void resolve()
        {
          change(AllZone.Human_Play, card.getController());
          change(AllZone.Computer_Play, card.getController());
        }
        public void change(PlayerZone play, String owner)
        {
          Card[] c = play.getCards();
          for (int i = 0; i < c.length; i++)
          {
            if(c[i].isCreature())
            {
              AllZone.GameAction.destroyNoRegeneration(c[i]);
              play.add(getCreature(c[i].getController()));
            }
          }
        }//change()
        public Card getCreature(String owner)
        {
          //TODO: owner and controller is NOT the same player sometimes
          //owner is the player who played March of Souls
          //the controller is the player who's creature was destroyed
          Card c = new Card();
          c.setToken(true);
          c.setOwner(owner);
          c.setController(owner);

          c.setName("Spirit");
          c.setImageName("W 1 1 Spirit");
          c.setBaseAttack(1);
          c.setBaseDefense(1);
          c.addIntrinsicKeyword("Flying");

          c.setManaCost("W");
          c.addType("Creature");
          c.addType("Spirit");
          return c;
        }//getCreature()
      };//SpellAbility
      card.clearSpellAbility();
      card.addSpellAbility(spell);
    }//*************** END ************ END **************************


//*************** START *********** START **************************
    else if (cardName.equals("Wrath of God") || cardName.equals("Damnation"))
	{
		SpellAbility spell = new Spell(card)
		{
			private static final long serialVersionUID = -18728406578984546L;

			public void resolve()
			{
				CardList all = new CardList();
				all.addAll(AllZone.Human_Play.getCards());
				all.addAll(AllZone.Computer_Play.getCards());
				all = all.getType("Creature");

				CardListUtil.sortByDestroyEffect(all);
				
				for (int i = 0; i < all.size(); i++)
				{
					Card c = all.get(i);
					System.out.println("WOG: " + c );
					AllZone.GameAction.destroyNoRegeneration(c);
				}
			}// resolve()

			public boolean canPlayAI()
			{
				CardList human = new CardList(AllZone.Human_Play.getCards());
				CardList computer = new CardList(AllZone.Computer_Play.getCards());

				human = human.getType("Creature");
				computer = computer.getType("Creature");

				// the computer will at least destroy 2 more human creatures
				return computer.size() < human.size() - 1 || 
				(AllZone.Computer_Life.getLife() < 7 && !human.isEmpty());
			}
		};// SpellAbility
		card.clearSpellAbility();
		card.addSpellAbility(spell);
	}// *************** END ************ END **************************
	
	//*************** START *********** START **************************
	else if (cardName.equals("Day of Judgment"))
	{
		SpellAbility spell = new Spell(card)
		{
			
			private static final long serialVersionUID = -2916641841124966207L;

			public void resolve()
			{
				CardList all = new CardList();
				all.addAll(AllZone.Human_Play.getCards());
				all.addAll(AllZone.Computer_Play.getCards());

				for (int i = 0; i < all.size(); i++)
				{
					Card c = all.get(i);
					if (c.isCreature())
						AllZone.GameAction.destroy(c);
				}
			}// resolve()

			public boolean canPlayAI()
			{
				CardList human = new CardList(AllZone.Human_Play.getCards());
				CardList computer = new CardList(AllZone.Computer_Play.getCards());

				human = human.getType("Creature");
				computer = computer.getType("Creature");

				// the computer will at least destroy 2 more human creatures
				return computer.size() < human.size() - 1 || 
				(AllZone.Computer_Life.getLife() < 7 && !human.isEmpty());
			}
		};// SpellAbility
		card.clearSpellAbility();
		card.addSpellAbility(spell);
	}// *************** END ************ END **************************
	
	
	
	//*************** START *********** START **************************
	else if (cardName.equals("Planar Cleansing") )
	{
		SpellAbility spell = new Spell(card)
		{

			private static final long serialVersionUID = -4233719265268955876L;

			public void resolve()
			{
				CardList all = new CardList();
				all.addAll(AllZone.Human_Play.getCards());
				all.addAll(AllZone.Computer_Play.getCards());

				for (int i = 0; i < all.size(); i++)
				{
					Card c = all.get(i);
					if (c.isPermanent() && !c.isLand())
						AllZone.GameAction.destroy(c);
				}
			}// resolve()

			public boolean canPlayAI()
			{
				CardList human = new CardList(AllZone.Human_Play.getCards());
				CardList computer = new CardList(AllZone.Computer_Play.getCards());

				human = human.getType("Creature");
				computer = computer.getType("Creature");

				// the computer will at least destroy 2 more human creatures
				return computer.size() < human.size() - 1 || 
				(AllZone.Computer_Life.getLife() < 7 && !human.isEmpty());
			}
		};// SpellAbility
		card.clearSpellAbility();
		card.addSpellAbility(spell);
	}// *************** END ************ END **************************
	
	//*************** START *********** START **************************
	else if (cardName.equals("Tranquility") || cardName.equals("Tempest of Light") || cardName.equals("Cleanfall"))
	{
		SpellAbility spell = new Spell(card)
		{

			private static final long serialVersionUID = 3087098751201624354L;

			public void resolve()
			{

				CardList all = new CardList();
				all.addAll(AllZone.Human_Play.getCards());
				all.addAll(AllZone.Computer_Play.getCards());
				all = all.getType("Enchantment");
				CardListUtil.sortByDestroyEffect(all);
				
				for (int i = 0; i < all.size(); i++)
				{
					Card c = all.get(i);
					AllZone.GameAction.destroy(c);
				}	
				
			}// resolve()

			public boolean canPlayAI()
			{
				CardList human = new CardList(AllZone.Human_Play.getCards());
				CardList computer = new CardList(AllZone.Computer_Play.getCards());

				human = human.getType("Enchantment");
				computer = computer.getType("Enchantment");
				
				
				if (human.size() == 0) 
					return false;
				
				// the computer will at least destroy 2 more human enchantments
				return computer.size() < human.size() - 1 || 
				(AllZone.Computer_Life.getLife() < 7 && !human.isEmpty());
			}
		};// SpellAbility
		card.clearSpellAbility();
		card.addSpellAbility(spell);
	}// *************** END ************ END **************************
	
	//*************** START *********** START **************************
	else if (cardName.equals("Hush"))
	{
		SpellAbility spell = new Spell(card)
		{
			private static final long serialVersionUID = -1572271570905127602L;

			public void resolve()
			{

				CardList all = new CardList();
				all.addAll(AllZone.Human_Play.getCards());
				all.addAll(AllZone.Computer_Play.getCards());
				all = all.getType("Enchantment");
				CardListUtil.sortByDestroyEffect(all);

				for (int i = 0; i < all.size(); i++)
				{
					Card c = all.get(i);
					AllZone.GameAction.destroy(c);
				}

			}// resolve()

			public boolean canPlayAI()
			{
				CardList human = new CardList(AllZone.Human_Play.getCards());
				CardList computer = new CardList(AllZone.Computer_Play.getCards());

				human = human.getType("Enchantment");
				computer = computer.getType("Enchantment");
				
				
				if (human.size() == 0) 
					return false;
				
				// the computer will at least destroy 2 more human enchantments
				return computer.size() < human.size() - 1 || 
				(AllZone.Computer_Life.getLife() < 7 && !human.isEmpty());
			}
		};// SpellAbility

		spell.setDescription("Destroy all enchantments.");
		card.clearSpellAbility();
	    card.addSpellAbility(spell);
	    card.addSpellAbility(CardFactoryUtil.ability_cycle(card, "2"));
	}// *************** END ************ END **************************


    //*************** START *********** START **************************
    else if(cardName.equals("Incendiary Command"))
    {
      //not sure what to call variables, so I just made up something
      final String[] m_player = new String[1];
      final Card[] m_land = new Card[1];

      final ArrayList<String> userChoice = new ArrayList<String>();

      final String[] cardChoice = {
        "Incendiary Command deals 4 damage to target player",
        "Incendiary Command deals 2 damage to each creature",
        "Destroy target nonbasic land",
        "Each player discards all cards in his or her hand, then draws that many cards"
      };

      final SpellAbility spell = new Spell(card)
      {
		private static final long serialVersionUID = 9178547049760990376L;

		public void resolve()
        {
//          System.out.println(userChoice);
//          System.out.println(m_land[0]);
//          System.out.println(m_player[0]);

          //"Incendiary Command deals 4 damage to target player",
          if(userChoice.contains(cardChoice[0]))
            AllZone.GameAction.getPlayerLife(m_player[0]).subtractLife(4);

          //"Incendiary Command deals 2 damage to each creature",
          if(userChoice.contains(cardChoice[1]))
          {
            //get all creatures
            CardList list = new CardList();
            list.addAll(AllZone.Human_Play.getCards());
            list.addAll(AllZone.Computer_Play.getCards());
            list = list.getType("Creature");


            for(int i = 0; i < list.size(); i++) {
              if (CardFactoryUtil.canDamage(card, list.get(i)))
            	list.get(i).addDamage(2, card);
            }
          }
            

          //"Destroy target nonbasic land",
          if(userChoice.contains(cardChoice[2]))
            AllZone.GameAction.destroy(m_land[0]);

          //"Each player discards all cards in his or her hand, then draws that many cards"
          if(userChoice.contains(cardChoice[3]))
          {
            discardDraw(Constant.Player.Computer);
            discardDraw(Constant.Player.Human);
          }
        }//resolve()

        void discardDraw(String player)
        {
          PlayerZone hand = AllZone.getZone(Constant.Zone.Hand, player);
          int n = hand.size();

          //technically should let the user discard one card at a time
          //in case graveyard order matters
          for(int i = 0; i < n; i++)
            AllZone.GameAction.discardRandom(player);

          for(int i = 0; i < n; i++)
            AllZone.GameAction.drawCard(player);
        }
        public boolean canPlayAI()
        {
          return false;
        }
      };//SpellAbility

      final Command setStackDescription = new Command()
	  {		
	  
	  	private static final long serialVersionUID = -4833850318955216009L;

		public void execute()
        {
          ArrayList<String> a = new ArrayList<String>();
          if(userChoice.contains(cardChoice[0]))
            a.add("deals 4 damage to " +m_player[0]);

          if(userChoice.contains(cardChoice[1]))
            a.add("deals 2 damage to each creature");

          if(userChoice.contains(cardChoice[2]))
            a.add("destroy " +m_land[0]);

          if(userChoice.contains(cardChoice[3]))
            a.add("each player discards all cards in his or her hand, then draws that many cards");

          String s = a.get(0) +", " +a.get(1);
          spell.setStackDescription(card.getName() +" - " +s);
        }
      };//Command


      final Input targetLand = new Input()
      {
		private static final long serialVersionUID = 1485276539154359495L;
		
		public void showMessage()
        {
          AllZone.Display.showMessage("Select target nonbasic land");
          ButtonUtil.enableOnlyCancel();
        }
        public void selectButtonCancel() {stop();}
        public void selectCard(Card c, PlayerZone zone)
        {
          if(c.isLand() &&
             zone.is(Constant.Zone.Play) &&
             !c.getType().contains("Basic"))
          {
            m_land[0] = c;
            setStackDescription.execute();

            stopSetNext(new Input_PayManaCost(spell));
          }//if
        }//selectCard()
      };//Input targetLand

      final Input targetPlayer = new Input()
      {
		private static final long serialVersionUID = -2636869617248434242L;
		
		public void showMessage()
        {
          AllZone.Display.showMessage("Select target player");
          ButtonUtil.enableOnlyCancel();
        }
        public void selectButtonCancel() {stop();}
        public void selectPlayer(String player)
        {
          m_player[0] = player;
          setStackDescription.execute();

          //if user needs to target nonbasic land
          if(userChoice.contains(cardChoice[2]))
            stopSetNext(targetLand);
          else
          {
            stopSetNext(new Input_PayManaCost(spell));
          }
        }//selectPlayer()
      };//Input targetPlayer


      Input chooseTwoInput = new Input()
      {
		private static final long serialVersionUID = 5625588008756700226L;

		public void showMessage()
        {
          //reset variables
          m_player[0] = null;
          m_land[0] = null;

          userChoice.clear();

          ArrayList<String> display = new ArrayList<String>();

          //get all
          CardList list = new CardList();
          list.addAll(AllZone.Human_Play.getCards());
          list.addAll(AllZone.Computer_Play.getCards());

          CardList land = list.getType("Land");
          CardList basicLand = list.getType("Basic");

          display.add("Incendiary Command deals 4 damage to target player");
          display.add("Incendiary Command deals 2 damage to each creature");
          if(land.size() != basicLand.size())
            display.add("Destroy target nonbasic land");
          display.add("Each player discards all cards in his or her hand, then draws that many cards");

          ArrayList<String> a = chooseTwo(display);
          //everything stops here if user cancelled
          if(a == null)
          {
            stop();
            return;
          }

          userChoice.addAll(a);

          if(userChoice.contains(cardChoice[0]))
            stopSetNext(targetPlayer);
          else if(userChoice.contains(cardChoice[2]))
            stopSetNext(targetLand);
          else
          {
            setStackDescription.execute();

            stopSetNext(new Input_PayManaCost(spell));
          }
        }//showMessage()

        ArrayList<String> chooseTwo(ArrayList<String> choices)
        {
          ArrayList<String> out = new ArrayList<String>();
          Object o = AllZone.Display.getChoiceOptional("Choose Two", choices.toArray());
          if(o == null)
            return null;

          out.add((String) o);

          choices.remove(out.get(0));
          o = AllZone.Display.getChoiceOptional("Choose Two", choices.toArray());
          if(o == null)
            return null;

          out.add((String) o);

          return out;
        }//chooseTwo()
      };//Input chooseTwoInput

      card.clearSpellAbility();
      card.addSpellAbility(spell);
      spell.setBeforePayMana(chooseTwoInput);
    }//*************** END ************ END **************************

    //*************** START *********** START **************************
    else if(cardName.equals("Boil") || cardName.equals("Boiling Seas"))
    {
      final SpellAbility spell = new Spell(card)
      {
		private static final long serialVersionUID = -5951776248246552958L;
		
		public void resolve()
        {
          CardList all = new CardList();
          all.addAll(AllZone.Human_Play.getCards());
          all.addAll(AllZone.Computer_Play.getCards());

          for(int i = 0; i < all.size(); i++)
          {
            Card c = all.get(i);
            if(c.getType().contains("Island"))
              AllZone.GameAction.destroy(c);
          }
        }//resolve()
        public boolean canPlayAI()
        {
          CardList list = new CardList(AllZone.Human_Play.getCards());
          list = list.getType("Island");

          return 3 < list.size();
        }
      };//SpellAbility
      spell.setStackDescription(card.getName() + " - destroy all Islands.");
      card.clearSpellAbility();
      card.addSpellAbility(spell);
    }//*************** END ************ END **************************


    //*************** START *********** START **************************
    else if(cardName.equals("Plague Wind"))
    {
      final SpellAbility spell = new Spell(card)
      {
		private static final long serialVersionUID = 6008660207658995400L;

		public void resolve()
        {
          String opponent = AllZone.GameAction.getOpponent(card.getController());
          PlayerZone play = AllZone.getZone(Constant.Zone.Play, opponent);

          CardList all = new CardList(play.getCards());
          all = all.getType("Creature");

          for(int i = 0; i < all.size(); i++)
          {
            Card c = all.get(i);
            if(c.isCreature())
              AllZone.GameAction.destroyNoRegeneration(c);
          }
        }//resolve()
      };//SpellAbility
      card.clearSpellAbility();
      card.addSpellAbility(spell);
    }//*************** END ************ END **************************
    
  //*************** START *********** START **************************
    else if(cardName.equals("Overwhelming Forces"))
    {
      final SpellAbility spell = new Spell(card)
      {
		private static final long serialVersionUID = -7165356050118574287L;

		public void resolve()
        {
          String opponent = AllZone.GameAction.getOpponent(card.getController());
          PlayerZone play = AllZone.getZone(Constant.Zone.Play, opponent);

          CardList all = new CardList(play.getCards());
          all = all.getType("Creature");

          for(int i = 0; i < all.size(); i++)
          {
            Card c = all.get(i);
            if(c.isCreature())
              AllZone.GameAction.destroy(c);
              AllZone.GameAction.drawCard(card.getController());
          }
        }//resolve()
      };//SpellAbility
      card.clearSpellAbility();
      card.addSpellAbility(spell);
    }//*************** END ************ END **************************



    //*************** START *********** START **************************
    else if(cardName.equals("Path of Anger's Flame"))
    {
      final SpellAbility spell = new Spell(card)
      {
		private static final long serialVersionUID = -4070937328002003491L;
		
		public boolean canPlayAI()
        {
          return getAttacker() != null;
        }
        public Card getAttacker()
        {
          //target creature that is going to attack
          Combat c = ComputerUtil.getAttackers();
          Card[] att = c.getAttackers();
          if(att.length != 0)
            return att[0];
          else
            return null;
        }//getAttacker()
        public void resolve()
        {
          PlayerZone play = AllZone.getZone(Constant.Zone.Play, card.getController());
          CardList start = new CardList(play.getCards());
          final CardList list = start.getType("Creature");

          for(int i = 0; i < list.size(); i++)
            list.get(i).addTempAttackBoost(2);

          play.updateObservers();

          Command untilEOT = new Command()
          {
			private static final long serialVersionUID = 6078548097470388679L;

			public void execute()
            {
              for(int i = 0; i < list.size(); i++)
                if(AllZone.GameAction.isCardInPlay(list.get(i)))
                  list.get(i).addTempAttackBoost(-2);
            }
          };
          AllZone.EndOfTurn.addUntil(untilEOT);
        }//resolve()
      };//SpellAbility
      card.clearSpellAbility();
      card.addSpellAbility(spell);
    }//*************** END ************ END **************************


    //*************** START *********** START **************************
    else if(cardName.equals("Kjeldoran War Cry"))
    {
      final SpellAbility spell = new Spell(card)
      {
		private static final long serialVersionUID = -7384618531690849205L;
		
		public boolean canPlayAI()
        {
          return getAttacker() != null;
        }
        public Card getAttacker()
        {
          //target creature that is going to attack
          Combat c = ComputerUtil.getAttackers();
          Card[] att = c.getAttackers();
          if(att.length != 0)
            return att[0];
          else
            return null;
        }//getAttacker()
        public void resolve()
        {
          PlayerZone play = AllZone.getZone(Constant.Zone.Play, card.getController());
          CardList start = new CardList(play.getCards());
          final CardList list = start.getType("Creature");

          final int boost = countCards();

          for(int i = 0; i < list.size(); i++)
          {
            list.get(i).addTempAttackBoost(boost);
            list.get(i).addTempDefenseBoost(boost);
          }

          play.updateObservers();

          Command untilEOT = new Command()
          {
			private static final long serialVersionUID = -2803160667440730370L;

			public void execute()
            {
              for(int i = 0; i < list.size(); i++)
                if(AllZone.GameAction.isCardInPlay(list.get(i)))
                {
                  list.get(i).addTempAttackBoost(- boost);
                  list.get(i).addTempDefenseBoost(- boost);
                }
            }
          };
          AllZone.EndOfTurn.addUntil(untilEOT);
        }//resolve()
        int countCards()
        {
          CardList all = new CardList();
          all.addAll(AllZone.Human_Graveyard.getCards());
          all.addAll(AllZone.Computer_Graveyard.getCards());

          all = all.getName("Kjeldoran War Cry");
          return all.size() + 1;
        }
      };//SpellAbility
      card.clearSpellAbility();
      card.addSpellAbility(spell);
    }//*************** END ************ END **************************


    //*************** START *********** START **************************
    else if(cardName.equals("Akroma's Vengeance") ||
       cardName.equals("Devastation") ||
       cardName.equals("Jokulhaups") ||
       cardName.equals("Purify") ||
       cardName.equals("Shatterstorm") ||
       cardName.equals("Obliterate"))
    {
      final SpellAbility spell = new Spell(card)
      {
      private static final long serialVersionUID = -7384618531690849205L;

      public void resolve()
        {
          CardList all = new CardList();
          all.addAll(AllZone.Human_Play.getCards());
          all.addAll(AllZone.Computer_Play.getCards());
          CardListUtil.sortByDestroyEffect(all);

          for(int i = 0; i < all.size(); i++)
          {
            Card c = all.get(i);
           
            if(cardName.equals("Akroma's Vengeance") && (c.isCreature() || c.isArtifact() || c.isEnchantment())) // Destroy Enchantment rather than Land
               AllZone.GameAction.destroy(c); // Can regenerate
            if(cardName.equals("Devastation") && (c.isCreature() || c.isLand()))
               AllZone.GameAction.destroy(c); // Can regenerate
            if((cardName.equals("Jokulhaups") || cardName.equals("Obliterate") ) && (c.isCreature() || c.isArtifact() || c.isLand()))
                AllZone.GameAction.destroyNoRegeneration(c); // CAN'T regenerate
            if(cardName.equals("Purify") && (c.isArtifact() || c.isEnchantment()))
               AllZone.GameAction.destroy(c); // Can regenerate
            if(cardName.equals("Shatterstorm") && (c.isArtifact()))
               AllZone.GameAction.destroyNoRegeneration(c); // CAN'T regenerate
          }
        }//resolve()
      };//SpellAbility
     
      if(cardName.equals("Akroma's Vengeance"))
      {
    	  spell.setStackDescription("Akroma's Vengeance - Destroy all artifacts, creatures, and enchantments."); // add stack description
          spell.setDescription("Destroy all artifacts, creatures, and enchantments."); // add spell detail description
      }
      card.clearSpellAbility();
      card.addSpellAbility(spell);
      if(cardName.equals("Akroma's Vengeance")) //add cycling
         card.addSpellAbility(CardFactoryUtil.ability_cycle(card, "3"));
      if(cardName.equals("Obliterate"))
    	 card.setText("Obliterate can't be countered. \r\n" + card.getText());
    }//*************** END ************ END **************************

    //*************** START *********** START **************************
    else if(cardName.equals("Wheel of Fortune"))
    {
      final SpellAbility spell = new Spell(card)
      {
		private static final long serialVersionUID = -7707418370887790709L;
		
		public void resolve()
        {
          discardDraw7(Constant.Player.Human);
          discardDraw7(Constant.Player.Computer);
        }//resolve()
        void discardDraw7(String player)
        {
          PlayerZone hand = AllZone.getZone(Constant.Zone.Hand, player);
          Card[] c = hand.getCards();
          for(int i = 0; i < c.length; i++)
            AllZone.GameAction.discard(c[i]);

          for(int i = 0; i < 7; i++)
            AllZone.GameAction.drawCard(player);
        }
      };//SpellAbility
      card.clearSpellAbility();
      card.addSpellAbility(spell);
    }//*************** END ************ END **************************


    //*************** START *********** START **************************
    else if(cardName.equals("Armageddon") || cardName.equals("Ravages of War"))
    {
      final SpellAbility spell = new Spell(card)
      {
		private static final long serialVersionUID = 432601263297207029L;
		
		public void resolve()
        {
          CardList all = new CardList();
          all.addAll(AllZone.Human_Play.getCards());
          all.addAll(AllZone.Computer_Play.getCards());

          for(int i = 0; i < all.size(); i++)
          {
            Card c = all.get(i);
            if(c.isLand())
              AllZone.GameAction.destroy(c);
          }
        }//resolve()
        public boolean canPlayAI()
        {
          int human    = countPower(AllZone.Human_Play);
          int computer = countPower(AllZone.Computer_Play);

          return human < computer || MyRandom.percentTrue(10);
        }
        public int countPower(PlayerZone play)
        {
          CardList list = new CardList(play.getCards());
          list = list.getType("Creature");
          int power = 0;
          for(int i = 0; i < list.size(); i++)
            power += list.get(i).getNetAttack();

          return power;
        }
      };//SpellAbility
      card.clearSpellAbility();
      card.addSpellAbility(spell);
    }//*************** END ************ END **************************


    //*************** START *********** START **************************
    else if(cardName.equals("Remove Soul") || cardName.equals("False Summoning") || cardName.equals("Essence Scatter") || cardName.equals("Preemptive Strike"))
    {
      SpellAbility spell = new Spell(card)
      {
		private static final long serialVersionUID = 4685055135070191326L;
		
		public void resolve()
        {
          SpellAbility sa = AllZone.Stack.pop();
          AllZone.GameAction.moveToGraveyard(sa.getSourceCard());
        }
        public boolean canPlay()
        {
          if(AllZone.Stack.size() == 0)
            return false;

          //see if spell is on stack and that opponent played it
          String opponent = AllZone.GameAction.getOpponent(card.getController());
          SpellAbility sa = AllZone.Stack.peek();

          //is spell?, did opponent play it?, is this a creature spell?
          return sa.isSpell() &&
                 opponent.equals(sa.getSourceCard().getController()) &&
                 sa.getSourceCard().getType().contains("Creature");
        }//canPlay()
      };
      card.clearSpellAbility();
      card.addSpellAbility(spell);
    }//*************** END ************ END **************************


    //*************** START *********** START **************************
    else if(cardName.equals("Counterspell") || cardName.equals("Cancel") || cardName.equals("Last Word") || cardName.equals("Traumatic Visions"))
    {
      SpellAbility spell = new Spell(card)
      {
		private static final long serialVersionUID = -2489268054171391552L;
		
		public void resolve()
        {
          SpellAbility sa = AllZone.Stack.pop();
          AllZone.GameAction.moveToGraveyard(sa.getSourceCard());
        }
        public boolean canPlay()
        {
          if(AllZone.Stack.size() == 0)
            return false;

          //see if spell is on stack and that opponent played it
          String opponent = AllZone.GameAction.getOpponent(card.getController());
          SpellAbility sa = AllZone.Stack.peek();

          return sa.isSpell() && opponent.equals(sa.getSourceCard().getController()) 
          		 && CardFactoryUtil.isCounterable(sa.getSourceCard());
        }
      };
      card.clearSpellAbility();
      card.addSpellAbility(spell);
    }//*************** END ************ END **************************



    //*************** START *********** START **************************
    else if(cardName.equals("Remand"))
    {
      SpellAbility spell = new Spell(card)
      {
		private static final long serialVersionUID = 7259402997927108504L;
		
		public void resolve()
        {
          //counter spell, return it to owner's hand
          SpellAbility sa = AllZone.Stack.pop();
          PlayerZone hand = AllZone.getZone(Constant.Zone.Hand, sa.getSourceCard().getOwner());
          AllZone.GameAction.moveTo(hand, sa.getSourceCard());

          //draw card
          AllZone.GameAction.drawCard(card.getController());
        }
        public boolean canPlay()
        {
          if(AllZone.Stack.size() == 0)
            return false;

          //see if spell is on stack and that opponent played it
          String opponent = AllZone.GameAction.getOpponent(card.getController());
          SpellAbility sa = AllZone.Stack.peek();

          return sa.isSpell() && opponent.equals(sa.getSourceCard().getController())
          		 && CardFactoryUtil.isCounterable(sa.getSourceCard());
        }
      };
      card.clearSpellAbility();
      card.addSpellAbility(spell);
    }//*************** END ************ END **************************



    //*************** START *********** START **************************
    else if(cardName.equals("Regress"))
    {
      SpellAbility spell = new Spell(card)
      {
		private static final long serialVersionUID = 4207725827500789300L;

		public void resolve()
        {
          if(AllZone.GameAction.isCardInPlay(getTargetCard()) && CardFactoryUtil.canTarget(card, getTargetCard()) )
          {
            if(getTargetCard().isToken())
              AllZone.getZone(getTargetCard()).remove(getTargetCard());
            else
            {
              PlayerZone hand = AllZone.getZone(Constant.Zone.Hand, getTargetCard().getOwner());
              AllZone.GameAction.moveTo(hand, getTargetCard());
            }
          }
        }//resolve()
		
		public void chooseTargetAI()
		{
			PlayerZone hPlay = AllZone.getZone(Constant.Zone.Play, Constant.Player.Human);
			CardList hum = new CardList(hPlay.getCards());
			
			Card best = CardFactoryUtil.AI_getMostExpensivePermanent(hum, card, true);
	        if (best!=null)
	        	setTargetCard(best);
		}
		public boolean canPlayAI()
		{
			PlayerZone hPlay = AllZone.getZone(Constant.Zone.Play, Constant.Player.Human);
			CardList hum = new CardList(hPlay.getCards());
			
			return hum.size()>0;
		}
      };
      //spell.setChooseTargetAI(CardFactoryUtil.AI_targetType("All", AllZone.Human_Play));

      spell.setBeforePayMana(CardFactoryUtil.input_targetType(spell, "All"));
      card.clearSpellAbility();
      card.addSpellAbility(spell);
    }//*************** END ************ END **************************



    //*************** START *********** START **************************
    else if(cardName.equals("Echoing Truth"))
    {
      final SpellAbility spell = new Spell(card)
      {
		private static final long serialVersionUID = 563933533543239220L;

		public boolean canPlayAI()
        {
          CardList human = CardFactoryUtil.AI_getHumanCreature(card, true);
          return 4 < AllZone.Phase.getTurn() && 0 < human.size();
        }
        public void chooseTargetAI()
        {
          CardList human = CardFactoryUtil.AI_getHumanCreature(card, true);
          setTargetCard(CardFactoryUtil.AI_getBestCreature(human));
        }

        public void resolve()
        {
          //if target card is not in play, just quit
          if(! AllZone.GameAction.isCardInPlay(getTargetCard()) || !CardFactoryUtil.canTarget(card, getTargetCard()) )
            return;

          //get all permanents
          CardList all = new CardList();
          all.addAll(AllZone.Human_Play.getCards());
          all.addAll(AllZone.Computer_Play.getCards());

          CardList sameName = all.getName(getTargetCard().getName());

          if (!getTargetCard().isFaceDown())
          {
	          //bounce all permanents with the same name
	          for(int i = 0; i < sameName.size(); i++)
	          {
	            if(sameName.get(i).isToken())
	              AllZone.GameAction.destroy(sameName.get(i));
	            else
	            {
	              PlayerZone hand = AllZone.getZone(Constant.Zone.Hand, sameName.get(i).getOwner());
	              AllZone.GameAction.moveTo(hand, sameName.get(i));
	            }
	          }//for
          }//if (!isFaceDown())
          else
          {
        	  PlayerZone hand = AllZone.getZone(Constant.Zone.Hand, getTargetCard().getOwner());
        	  AllZone.GameAction.moveTo(hand, getTargetCard());
          }
        }//resolve()
      };//SpellAbility
      Input target = new Input()
      {
		private static final long serialVersionUID = -3978705328511825933L;
		
		public void showMessage()
        {
          AllZone.Display.showMessage("Select target non-land permanent for " +spell.getSourceCard());
          ButtonUtil.enableOnlyCancel();
        }
        public void selectButtonCancel() {stop();}
        public void selectCard(Card card, PlayerZone zone)
        {
          if(! card.isLand() && zone.is(Constant.Zone.Play) && CardFactoryUtil.canTarget(spell, card))
          {
            spell.setTargetCard(card);
            stopSetNext(new Input_PayManaCost(spell));
          }
        }
      };//Input

      spell.setBeforePayMana(target);
      card.clearSpellAbility();
      card.addSpellAbility(spell);
    }//*************** END ************ END **************************


    //*************** START *********** START **************************
    else if(cardName.equals("Repulse"))
    {
      final SpellAbility spell = new Spell(card)
      {
		private static final long serialVersionUID = 7586791617021788730L;

		public boolean canPlayAI()
        {
          CardList human = CardFactoryUtil.AI_getHumanCreature(card, true);
          return 3 < AllZone.Phase.getTurn() && 0 < human.size();
        }
        public void chooseTargetAI()
        {
          CardList human = CardFactoryUtil.AI_getHumanCreature(card, true);
          setTargetCard(CardFactoryUtil.AI_getBestCreature(human));
        }

        public void resolve()
        {
          if(AllZone.GameAction.isCardInPlay(getTargetCard()) && CardFactoryUtil.canTarget(card, getTargetCard()) )
          {
            if(getTargetCard().isToken())
              AllZone.getZone(getTargetCard()).remove(getTargetCard());
            else
            {
              PlayerZone hand = AllZone.getZone(Constant.Zone.Hand, getTargetCard().getOwner());
              @SuppressWarnings("unused") // play
			  PlayerZone play = AllZone.getZone(Constant.Zone.Play, getTargetCard().getOwner());
              AllZone.GameAction.moveTo(hand, getTargetCard());
              
              //play.remove(getTargetCard());
              //hand.add(getTargetCard());
            }
            AllZone.GameAction.drawCard(card.getController());
          }//if
        }//resolve()
      };//SpellAbility
      Input target = new Input()
      {
		private static final long serialVersionUID = -4976281514575975012L;
		
		public void showMessage()
        {
          AllZone.Display.showMessage("Select target creature for " +spell.getSourceCard());
          ButtonUtil.enableOnlyCancel();
        }
        public void selectButtonCancel() {stop();}
        public void selectCard(Card card, PlayerZone zone)
        { 
          if(!CardFactoryUtil.canTarget(spell, card)){
        	  AllZone.Display.showMessage("Cannot target this card (Shroud? Protection?).");
          }
        	
          else if(card.isCreature() && zone.is(Constant.Zone.Play))
          {
            spell.setTargetCard(card);
            stopSetNext(new Input_PayManaCost(spell));
          }
        }
      };//Input

      spell.setBeforePayMana(target);
      card.clearSpellAbility();
      card.addSpellAbility(spell);
    }//*************** END ************ END **************************
    
    
  //*************** START *********** START **************************
    else if(cardName.equals("Unsummon"))
    {
      final SpellAbility spell = new Spell(card)
      {
		private static final long serialVersionUID = -4920446621228732642L;

		public boolean canPlayAI()
        {
          CardList human = CardFactoryUtil.AI_getHumanCreature(card, true);
          return 2 < AllZone.Phase.getTurn() && 0 < human.size();
        }
        public void chooseTargetAI()
        {
          CardList human = CardFactoryUtil.AI_getHumanCreature(card, true);
          setTargetCard(CardFactoryUtil.AI_getBestCreature(human));
        }

        public void resolve()
        {
          if(AllZone.GameAction.isCardInPlay(getTargetCard()) && CardFactoryUtil.canTarget(card, getTargetCard()) )
          {
            if(getTargetCard().isToken())
              AllZone.getZone(getTargetCard()).remove(getTargetCard());
            else
            {
              PlayerZone hand = AllZone.getZone(Constant.Zone.Hand, getTargetCard().getOwner());
              @SuppressWarnings("unused") // play
			  PlayerZone play = AllZone.getZone(Constant.Zone.Play, getTargetCard().getOwner());
              AllZone.GameAction.moveTo(hand, getTargetCard());
              
              //play.remove(getTargetCard());
              //hand.add(getTargetCard());
            }
          }//if
        }//resolve()
      };//SpellAbility
      Input target = new Input()
      {
		private static final long serialVersionUID = -7657949950004365660L;
		public void showMessage()
        {
          AllZone.Display.showMessage("Select target creature for " +spell.getSourceCard());
          ButtonUtil.enableOnlyCancel();
        }
        public void selectButtonCancel() {stop();}
        public void selectCard(Card card, PlayerZone zone)
        { 
          if(!CardFactoryUtil.canTarget(spell, card)){
        	  AllZone.Display.showMessage("Cannot target this card (Shroud? Protection?).");
          }
        	
          else if(card.isCreature() && zone.is(Constant.Zone.Play))
          {
            spell.setTargetCard(card);
            stopSetNext(new Input_PayManaCost(spell));
          }
        }
      };//Input

      spell.setBeforePayMana(target);
      card.clearSpellAbility();
      card.addSpellAbility(spell);
    }//*************** END ************ END **************************


    //*************** START *********** START **************************
    else if(cardName.equals("Impulse"))
    {
      final SpellAbility spell = new Spell(card)
      {
		private static final long serialVersionUID = -6793636573741251978L;

		public boolean canPlayAI() {return false;}

        public void resolve()
        {
          CardList top = new CardList();
          PlayerZone library = AllZone.getZone(Constant.Zone.Library, card.getController());

          Card c;
          int j = 4;
          if (library.size() < 4)
        	  j = library.size();
          for(int i = 0; i < j; i++)
          {
            c = library.get(0);
            library.remove(0);
            top.add(c);
          }

          if (top.size() >= 1)
          {
	          //let user get choice
	          Card chosen = (Card) AllZone.Display.getChoice("Choose a card to put into your hand", top.toArray());
	          top.remove(chosen);
	
	          //put card in hand
	          PlayerZone hand = AllZone.getZone(Constant.Zone.Hand, card.getController());
	          hand.add(chosen);
	
	          //add cards to bottom of library
	          for(int i = 0; i < top.size(); i++)
	            library.add(top.get(i));
          }
        }//resolve()
      };//SpellAbility

      card.clearSpellAbility();
      card.addSpellAbility(spell);
    }//*************** END ************ END **************************


    //*************** START *********** START **************************
    else if(cardName.equals("Bribery"))
    {
      SpellAbility spell = new Spell(card)
      {
        private static final long serialVersionUID = -4267653042039058744L;
		public void resolve()
        {
          String player = card.getController();
          if(player.equals(Constant.Player.Human))
            humanResolve();
          else
            computerResolve();
        }
        public void humanResolve()
        {
          //choose creature from opponents library to put into play
          //shuffle opponent's library
          String opponent    = AllZone.GameAction.getOpponent(card.getController());
          PlayerZone library = AllZone.getZone(Constant.Zone.Library, opponent);
          CardList choices   = new CardList(library.getCards());

          choices = choices.getType("Creature");
          Object o = AllZone.Display.getChoiceOptional("Choose a creature", choices.toArray());
          if(o != null)
            resolve((Card)o);
        }
        public void computerResolve()
        {
          CardList all = new CardList(AllZone.Human_Library.getCards());
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
          if(biggest != null)
            resolve(biggest);
        }//computerResolve()
        public void resolve(Card selectedCard)
        {
          String opponent    = AllZone.GameAction.getOpponent(card.getController());
          PlayerZone library = AllZone.getZone(Constant.Zone.Library, opponent);

          Card c = selectedCard;
          PlayerZone play = AllZone.getZone(Constant.Zone.Play, card.getController());

          //need to set controller before adding it to "play"
          c.setController(card.getController());
          c.setSickness(true);

          library.remove(c);
          play.add(c);


          AllZone.GameAction.shuffle(opponent);
        }//resolve()
      };

      spell.setBeforePayMana(new Input_PayManaCost(spell));
      card.clearSpellAbility();
      card.addSpellAbility(spell);
    }//*************** END ************ END **************************

    //*************** START *********** START **************************
    else if(cardName.equals("Amnesia"))
    {
      SpellAbility spell = new Spell(card)
      {
        private static final long serialVersionUID = -5456164079438881319L;

		public void resolve()
        {
          PlayerZone hand = AllZone.getZone(Constant.Zone.Hand, getTargetPlayer());
          Card[] c = hand.getCards();

          for(int i = 0; i < c.length; i++)
            if(! c[i].isLand())
              AllZone.GameAction.discard(c[i]);
        }
      };
      spell.setChooseTargetAI(CardFactoryUtil.AI_targetHuman());

      spell.setBeforePayMana(CardFactoryUtil.input_targetPlayer(spell));

      card.clearSpellAbility();
      card.addSpellAbility(spell);
    }//*************** END ************ END **************************


    //*************** START *********** START **************************
    else if(cardName.equals("Evacuation"))
    {
      SpellAbility spell = new Spell(card)
      {
 		private static final long serialVersionUID = -6305494177352031326L;

		public void resolve()
        {
          CardList all = new CardList();
          all.addAll(AllZone.Human_Play.getCards());
          all.addAll(AllZone.Computer_Play.getCards());
          all = all.getType("Creature");

          for(int i = 0; i < all.size(); i++)
          {
            //if is token, remove token from play, else return creature to hand
            if(all.get(i).isToken())
              getPlay(all.get(i)).remove(all.get(i));
            else
              AllZone.GameAction.moveTo(getHand(all.get(i)), all.get(i));
          }
        }//resolve()
        PlayerZone getPlay(Card c)
        {
          return AllZone.getZone(Constant.Zone.Play, c.getController());
        }

        PlayerZone getHand(Card c)
        {
          return AllZone.getZone(Constant.Zone.Hand, c.getOwner());
        }
      };
      card.clearSpellAbility();
      card.addSpellAbility(spell);
    }//*************** END ************ END **************************
    
    
/*
    //*************** END ************ END **************************
    else if (cardName.equals("Lockjaw Snapper"))
    {
    	
    	final Ability ability = new Ability(card, "0")
        {
          public void resolve()
          {
        	PlayerZone hPlay = AllZone.getZone(Constant.Zone.Play, Constant.Player.Human);
  			PlayerZone cPlay = AllZone.getZone(Constant.Zone.Play, Constant.Player.Computer);
  			
  			CardList creatures = new CardList();
  			creatures.addAll(hPlay.getCards());
  			creatures.addAll(cPlay.getCards());
  			creatures = creatures.filter(new CardListFilter()
  			{
					public boolean addCard(Card c) {
						return c.getCounters(Counters.M1M1) > 0;
					}
  			});
  			
  			for (int i=0; i<creatures.size();i++)
  			{
  				Card c = creatures.get(i);
  				c.addCounter(Counters.M1M1, 1);
  			}
          }
        };
    	
    	Command destroy = new Command()
    	{
    		private static final long serialVersionUID = 6389028698247230474L;

			public void execute()
    		{
    			ability.setStackDescription(card.getName()+ " - put -1/-1 counter on each creature that has a -1/-1 counter on it.");
                AllZone.Stack.add(ability);
    		}
    	};//command
    	card.addDestroyCommand(destroy);
    }//*************** START *********** START **************************
*/
    
    
    //*************** START *********** START **************************
    else if(cardName.equals("Deep Analysis"))
    {
      SpellAbility spell = new Spell(card)
      {

		private static final long serialVersionUID = 6317660847906461825L;
		public void resolve()
        {
          AllZone.GameAction.drawCard(card.getController());
          AllZone.GameAction.drawCard(card.getController());
        }
        public boolean canPlayAI()
        {
          return AllZone.Computer_Hand.getCards().length <= 6;
        }
      };
      spell.setDescription("Target player draws two cards.");
      spell.setStackDescription(card.getName() + " - " + card.getController() + " draws two cards.");
      card.clearSpellAbility();
      card.addSpellAbility(spell);
      
      card.addSpellAbility(CardFactoryUtil.ability_Flashback(card, "1 U", "3"));
      card.setFlashback(true);
    }//*************** END ************ END **************************



    //*************** START *********** START **************************
    else if(cardName.equals("Allied Strategies"))
    {
      SpellAbility spell = new Spell(card)
      {
        private static final long serialVersionUID = 2730790148899002194L;

		public void resolve()
        {
          int n = countLandTypes();

          for(int i = 0; i < n; i++)
            AllZone.GameAction.drawCard(getTargetPlayer());
        }

        int countLandTypes()
        {
          PlayerZone play = AllZone.getZone(Constant.Zone.Play, getTargetPlayer());
          CardList land = new CardList(play.getCards());

          String basic[] = {"Forest", "Plains", "Mountain", "Island", "Swamp"};
          int count = 0;

          for(int i = 0; i < basic.length; i++)
          {
            CardList c = land.getType(basic[i]);
            if(! c.isEmpty())
              count++;
          }

          return count;
        }//countLandTypes()

        public boolean canPlayAI() {return AllZone.Computer_Hand.getCards().length <= 5;}
      };
      spell.setChooseTargetAI(CardFactoryUtil.AI_targetComputer());

      spell.setBeforePayMana(CardFactoryUtil.input_targetPlayer(spell));
      card.clearSpellAbility();
      card.addSpellAbility(spell);
    }//*************** END ************ END **************************



    //*************** START *********** START **************************
    else if(cardName.equals("Opt"))
    {
      SpellAbility spell = new Spell(card)
      {
        private static final long serialVersionUID = 6002051826637535590L;
		public void resolve()
        {
          String player = card.getController();
          if(player.equals(Constant.Player.Human))
            humanResolve();
          else
            computerResolve();
        }
        public void computerResolve()
        {
          //if top card of library is a land, put it on bottom of library
          if(AllZone.Computer_Library.getCards().length != 0)
          {
            Card top = AllZone.Computer_Library.get(0);
            if(top.isLand())
            {
              AllZone.Computer_Library.remove(top);
              AllZone.Computer_Library.add(top);
            }
          }
          AllZone.GameAction.drawCard(card.getController());
        }//computerResolve()
        public void humanResolve()
        {
          PlayerZone library = AllZone.getZone(Constant.Zone.Library, card.getController());

          //see if any cards are in library
          if(library.getCards().length != 0)
          {
            Card top = library.get(0);

            Object o = top;
            while(o instanceof Card)
              o = AllZone.Display.getChoice("Do you want draw this card?", new Object[] {top, "Yes", "No"});

            if(o.toString().equals("No"))
            {
              library.remove(top);
              library.add(top);
            }
          }//if
          AllZone.GameAction.drawCard(card.getController());
        }//resolve()
      };
      card.clearSpellAbility();
      card.addSpellAbility(spell);
    }//*************** END ************ END **************************



    //*************** START *********** START **************************
    else if(cardName.equals("Needle Storm"))
    {
      SpellAbility spell = new Spell(card)
      {
        private static final long serialVersionUID = 1477280027897731860L;

		public void resolve()
        {
          CardList list = new CardList();
          list.addAll(AllZone.Human_Play.getCards());
          list.addAll(AllZone.Computer_Play.getCards());
          list = list.filter(new CardListFilter()
          {
            public boolean addCard(Card c)
            {
              return c.isCreature() && c.getKeyword().contains("Flying") && CardFactoryUtil.canDamage(card, c);
            }
          });

          for(int i = 0; i < list.size(); i++)
            list.get(i).addDamage(4, card);
        }//resolve()

        public boolean canPlayAI() {return CardFactoryUtil.AI_getHumanCreature("Flying", card, false).size() != 0;}
      };
      card.clearSpellAbility();
      card.addSpellAbility(spell);
    }//*************** END ************ END **************************


    //*************** START *********** START **************************
    else if(cardName.equals("Wandering Stream"))
    {
      SpellAbility spell = new Spell(card)
      {
        private static final long serialVersionUID = 8777120667537357240L;
		public void resolve()
        {
          PlayerLife life = AllZone.GameAction.getPlayerLife(card.getController());
          life.addLife(countLandTypes() * 2);
        }//resolve()
        int countLandTypes()
        {
          PlayerZone play = AllZone.getZone(Constant.Zone.Play, card.getController());
          CardList land = new CardList(play.getCards());

          String basic[] = {"Forest", "Plains", "Mountain", "Island", "Swamp"};
          int count = 0;

          for(int i = 0; i < basic.length; i++)
          {
            CardList c = land.getType(basic[i]);
            if(! c.isEmpty())
              count++;
          }

          return count;
        }//countLandTypes()
      };
      card.clearSpellAbility();
      card.addSpellAbility(spell);
    }//*************** END ************ END **************************


    //*************** START *********** START **************************
    else if(cardName.equals("Worldly Tutor") || cardName.equals("Sylvan Tutor"))
    {
      SpellAbility spell = new Spell(card)
      {
		private static final long serialVersionUID = 6624899562868794463L;
		public boolean canPlayAI()
        {
          return 6 < AllZone.Phase.getTurn();
        }

        public void resolve()
        {
          String player = card.getController();
          if(player.equals(Constant.Player.Human))
            humanResolve();
          else
            computerResolve();
        }
        public void computerResolve()
        {
          CardList creature = new CardList(AllZone.Computer_Library.getCards());
          creature = creature.getType("Creature");
          if(creature.size() != 0)
          {
            Card c = creature.get(0);
            AllZone.GameAction.shuffle(card.getController());

            //move to top of library
            AllZone.Computer_Library.remove(c);
            AllZone.Computer_Library.add(c, 0);
            
            CardList list = new CardList();
            list.add(c);
            AllZone.Display.getChoiceOptional("Computer picked:", list.toArray());
          }
        }//computerResolve()
        public void humanResolve()
        {
          PlayerZone library = AllZone.getZone(Constant.Zone.Library, card.getController());

          CardList list = new CardList(library.getCards());
          list = list.getType("Creature");

          if(list.size() != 0)
          {
            Object o = AllZone.Display.getChoiceOptional("Select a creature", list.toArray());

            AllZone.GameAction.shuffle(card.getController());
            if(o != null)
            {
              //put creature on top of library
              library.remove(o);
              library.add((Card)o, 0);
            }
          }//if
        }//resolve()
      };
      card.clearSpellAbility();
      card.addSpellAbility(spell);
    }//*************** END ************ END **************************
    
  //*************** START *********** START **************************
    else if(cardName.equals("Enlightened Tutor"))
    {
      SpellAbility spell = new Spell(card)
      {
		private static final long serialVersionUID = 2281623056004772379L;
		public boolean canPlayAI()
        {
          return 4 < AllZone.Phase.getTurn();
        }

        public void resolve()
        {
          String player = card.getController();
          if(player.equals(Constant.Player.Human))
            humanResolve();
          else
            computerResolve();
        }
        public void computerResolve()
        {
          CardList list = new CardList(AllZone.Computer_Library.getCards());
          CardList encharts = new CardList();
          
          for (int i=0;i<list.size();i++)
          {
        	  if (list.get(i).getType().contains("Artifact") || list.get(i).getType().contains("Enchantment"))
        		  encharts.add(list.get(i));
          }
          
          if(encharts.size() != 0)
          {
        	  //comp will just grab the first one it finds
            Card c = encharts.get(0);
            AllZone.GameAction.shuffle(card.getController());
            
            
            //move to top of library
            AllZone.Computer_Library.remove(c);
            AllZone.Computer_Library.add(c, 0);
          }
        }//computerResolve()
        public void humanResolve()
        {
          PlayerZone library = AllZone.getZone(Constant.Zone.Library, card.getController());
          
          CardList list = new CardList(library.getCards());
          CardList encharts = new CardList();
          
          for (int i=0;i<list.size();i++)
          {
        	  if (list.get(i).getType().contains("Artifact") || list.get(i).getType().contains("Enchantment"))
        		  encharts.add(list.get(i));
          }


          if(encharts.size() != 0)
          {
            Object o = AllZone.Display.getChoiceOptional("Select an artifact or enchantment", encharts.toArray());

            AllZone.GameAction.shuffle(card.getController());
            if(o != null)
            {
              //put card on top of library
              library.remove(o);
              library.add((Card)o, 0);
            }
          }//if
        }//resolve()
      };
      card.clearSpellAbility();
      card.addSpellAbility(spell);
    }//*************** END ************ END **************************


    //*************** START *********** START **************************
    else if(cardName.equals("Pulse of the Tangle"))
    {
      SpellAbility spell = new Spell(card)
      {
		private static final long serialVersionUID = 523613120207836692L;

		public void resolve()
        {
          PlayerZone play = AllZone.getZone(Constant.Zone.Play, card.getController());
          Card c = new Card();

          c.setOwner(card.getController());
          c.setController(card.getController());

          c.setName("Beast");
          c.setImageName("G 3 3 Beast");
          c.setManaCost("G");
          c.setToken(true);

          c.addType("Creature");
          c.addType("Beast");
          c.setBaseAttack(3);
          c.setBaseDefense(3);

          play.add(c);

          //return card to hand if necessary
          String opponent = AllZone.GameAction.getOpponent(card.getController());
          PlayerZone oppPlay = AllZone.getZone(Constant.Zone.Play, opponent);
          PlayerZone myPlay  = AllZone.getZone(Constant.Zone.Play, card.getController());

          CardList oppList = new CardList(oppPlay.getCards());
          CardList myList = new CardList(myPlay.getCards());

          oppList = oppList.getType("Creature");
          myList  = myList.getType("Creature");

          //if true, return card to hand
          if(myList.size() < oppList.size())
          {
            PlayerZone hand = AllZone.getZone(Constant.Zone.Hand, card.getController());
            hand.add(card);
          }
          else
            AllZone.GameAction.moveToGraveyard(card);
        }//resolve()
      };
      card.clearSpellAbility();
      card.addSpellAbility(spell);
    }//*************** END ************ END **************************
    
    //*************** START *********** START **************************
    else if (cardName.equals("Think Twice"))
    {
    	SpellAbility spell = new Spell(card)
    	{
			private static final long serialVersionUID = 2571730013113893086L;

			public void resolve()
	        {
	  			AllZone.GameAction.drawCard(card.getController());
	        }//resolve()
        };
        card.clearSpellAbility();
        card.setFlashback(true);
        card.addSpellAbility(spell);
    	card.addSpellAbility(CardFactoryUtil.ability_Flashback(card, "2 U", "0"));
    	
  	}//*************** END ************ END **************************
    
    //*************** START *********** START **************************
    else if(cardName.equals("Call of the Herd"))
    {
      SpellAbility spell = new Spell(card)
      {
		private static final long serialVersionUID = 1959302998030377554L;

		public void resolve()
        {
          PlayerZone play = AllZone.getZone(Constant.Zone.Play, card.getController());
          Card c = new Card();

          c.setOwner(card.getController());
          c.setController(card.getController());

          c.setName("Elephant");
          c.setImageName("G 3 3 Elephant");
          c.setManaCost("G");
          c.setToken(true);

          c.addType("Creature");
          c.addType("Elephant");
          c.setBaseAttack(3);
          c.setBaseDefense(3);

          play.add(c);

        }//resolve()
      };
      
      spell.setDescription("Put a 3/3 green Elephant creature token into play.");
      spell.setStackDescription(card.getController() + " puts a 3/3 green Elephant creature token into play.");
      
      card.setFlashback(true);
      card.clearSpellAbility();
      card.addSpellAbility(spell);
      card.addSpellAbility(CardFactoryUtil.ability_Flashback(card, "3 G", "0"));
      
    }//*************** END ************ END **************************
    
  //*************** START *********** START **************************
    else if(cardName.equals("Elephant Ambush"))
    {
      SpellAbility spell = new Spell(card)
      {

		private static final long serialVersionUID = 1808366787563573082L;

		public void resolve()
        {
          PlayerZone play = AllZone.getZone(Constant.Zone.Play, card.getController());
          Card c = new Card();

          c.setOwner(card.getController());
          c.setController(card.getController());

          c.setName("Elephant");
          c.setImageName("G 3 3 Elephant");
          c.setManaCost("G");
          c.setToken(true);

          c.addType("Creature");
          c.addType("Elephant");
          c.setBaseAttack(3);
          c.setBaseDefense(3);

          play.add(c);

        }//resolve()
      };
      
      spell.setDescription("Put a 3/3 green Elephant creature token into play.");
      spell.setStackDescription(card.getController() + " puts a 3/3 green Elephant creature token into play.");
      
      card.setFlashback(true);
      card.clearSpellAbility();
      card.addSpellAbility(spell);
      card.addSpellAbility(CardFactoryUtil.ability_Flashback(card, "6 G G", "0"));
      
    }//*************** END ************ END **************************
    
  //*************** START *********** START **************************
    else if(cardName.equals("Chatter of the Squirrel"))
    {
      SpellAbility spell = new Spell(card)
      {

		private static final long serialVersionUID = 3787460988525779623L;
		public void resolve()
        {	
	      makeToken();
        }
		public void makeToken()
		{
          PlayerZone play = AllZone.getZone(Constant.Zone.Play, card.getController());
          Card c = new Card();

          c.setOwner(card.getController());
          c.setController(card.getController());

          c.setName("Squirrel");
          c.setName("G 1 1 Squirrel");
          c.setManaCost("G");
          c.setToken(true);

          c.addType("Creature");
          c.addType("Squirrel");
          c.setBaseAttack(1);
          c.setBaseDefense(1);

          play.add(c);

        }//resolve()
      };
      
      spell.setDescription("Put a 1/1 green Squirrel creature token into play.");
      spell.setStackDescription(card.getController() + " puts a 1/1 green Squirrel creature token into play.");
      
      card.setFlashback(true);
      card.clearSpellAbility();
      card.addSpellAbility(spell);
      card.addSpellAbility(CardFactoryUtil.ability_Flashback(card, "1 G", "0"));
      
    }//*************** END ************ END **************************

  //*************** START *********** START **************************
    else if(cardName.equals("Acorn Harvest"))
    {
      SpellAbility spell = new Spell(card)
      {
		private static final long serialVersionUID = 4779507778950336252L;
		public void resolve()
        {	
	      makeToken();
	      makeToken();
        }
		public void makeToken()
		{
          PlayerZone play = AllZone.getZone(Constant.Zone.Play, card.getController());
          Card c = new Card();

          c.setOwner(card.getController());
          c.setController(card.getController());

          c.setName("Squirrel");
          c.setImageName("G 1 1 Squirrel");
          c.setManaCost("G");
          c.setToken(true);

          c.addType("Creature");
          c.addType("Squirrel");
          c.setBaseAttack(1);
          c.setBaseDefense(1);

          play.add(c);

        }//resolve()
      };
      
      spell.setDescription("Put two 1/1 green Squirrel creature tokens into play.");
      spell.setStackDescription(card.getController() + " puts two 1/1 green Squirrel creature tokens into play.");
      
      card.setFlashback(true);
      card.clearSpellAbility();
      card.addSpellAbility(spell);
      card.addSpellAbility(CardFactoryUtil.ability_Flashback(card, "1 G", "3"));
      
    }//*************** END ************ END **************************
    
  //*************** START *********** START **************************
    else if(cardName.equals("Beast Attack"))
    {
      SpellAbility spell = new Spell(card)
      {
    	  
		private static final long serialVersionUID = 381753184772980686L;
		public void resolve()
        {	
	      makeToken();
        }
		//for some reason, without this the AI can keep casting Beast Attack over and over.
		public boolean canPlayAI()
		{
			return !AllZone.GameAction.isCardInGrave(card);
		}
		
		public void makeToken()
		{
          PlayerZone play = AllZone.getZone(Constant.Zone.Play, card.getController());
          Card c = new Card();

          c.setOwner(card.getController());
          c.setController(card.getController());

          c.setName("Beast");
          c.setImageName("G 4 4 Beast");
          c.setManaCost("G");
          c.setToken(true);

          c.addType("Creature");
          c.addType("Beast");
          c.setBaseAttack(4);
          c.setBaseDefense(4);

          play.add(c);

        }//resolve()
      };
      
      spell.setDescription("Put a 4/4 green Beast creature token into play.");
      spell.setStackDescription(card.getController() + " put a 4/4 green Beast creature token into play.");
      
      card.setFlashback(true);
      card.clearSpellAbility();
      card.addSpellAbility(spell);
      card.addSpellAbility(CardFactoryUtil.ability_Flashback(card, "2 G G G", "0"));
      
    }//*************** END ************ END **************************
    
    //*************** START *********** START **************************
    else if(cardName.equals("Roar of the Wurm"))
    {
      SpellAbility spell = new Spell(card)
      {

		private static final long serialVersionUID = -7861877439125080643L;
		public void resolve()
        {	
	      makeToken();
        }
		public void makeToken()
		{
          PlayerZone play = AllZone.getZone(Constant.Zone.Play, card.getController());
          Card c = new Card();

          c.setOwner(card.getController());
          c.setController(card.getController());

          c.setName("Wurm");
          c.setImageName("G 6 6 Wurm");
          c.setManaCost("G");
          c.setToken(true);

          c.addType("Creature");
          c.addType("Wurm");
          c.setBaseAttack(6);
          c.setBaseDefense(6);

          play.add(c);

        }//resolve()
      };
      
      spell.setDescription("Put a 6/6 green Wurm creature token into play.");
      spell.setStackDescription(card.getController() + " put a 6/6 green Wurm creature token into play.");
      
      card.setFlashback(true);
      card.clearSpellAbility();
      card.addSpellAbility(spell);
      card.addSpellAbility(CardFactoryUtil.ability_Flashback(card, "3 G", "0"));
      
    }//*************** END ************ END **************************
    
  //*************** START *********** START **************************
    else if(cardName.equals("Crush of Wurms"))
    {
      SpellAbility spell = new Spell(card)
      {

		private static final long serialVersionUID = 3917531146741977318L;
		public void resolve()
        {	
	      makeToken();
	      makeToken();
	      makeToken();
        }
		public void makeToken()
		{
          PlayerZone play = AllZone.getZone(Constant.Zone.Play, card.getController());
          Card c = new Card();

          c.setOwner(card.getController());
          c.setController(card.getController());

          c.setName("Wurm");
          c.setImageName("G 6 6 Wurm");
          c.setManaCost("G");
          c.setToken(true);

          c.addType("Creature");
          c.addType("Wurm");
          c.setBaseAttack(6);
          c.setBaseDefense(6);

          play.add(c);

        }//resolve()
      };
      
      spell.setDescription("Put three 6/6 green Wurm creature tokens into play.");
      spell.setStackDescription(card.getController() + " Put three 6/6 green Wurm creature tokens into play.");
      
      card.setFlashback(true);
      card.clearSpellAbility();
      card.addSpellAbility(spell);
      card.addSpellAbility(CardFactoryUtil.ability_Flashback(card, "9 G G G", "0"));
      
    }//*************** END ************ END **************************
    
  //*************** START *********** START **************************
    else if(cardName.equals("Grizzly Fate"))
    {
      SpellAbility spell = new Spell(card)
      {
		private static final long serialVersionUID = 731860438110589738L;
		public void resolve()
        {	
		  PlayerZone grave = AllZone.getZone(Constant.Zone.Graveyard, card.getController());
		  CardList list = new CardList(grave.getCards());
	      makeToken();
	      makeToken();
	      if (list.size() >= 7)
	      {
	    	  makeToken();
	    	  makeToken();
	      }
        }
		public void makeToken()
		{
          PlayerZone play = AllZone.getZone(Constant.Zone.Play, card.getController());
          Card c = new Card();

          c.setOwner(card.getController());
          c.setController(card.getController());

          c.setName("Bear");
          c.setImageName("G 2 2 Bear");
          c.setManaCost("G");
          c.setToken(true);

          c.addType("Creature");
          c.addType("Bear");
          c.setBaseAttack(2);
          c.setBaseDefense(2);

          play.add(c);

        }//resolve()
      };
      
      spell.setDescription("Put two 2/2 green Bear creature tokens into play. Threshold - Put four 2/2 green Bear creature tokens into play instead if seven or more cards are in your graveyard. ");
      spell.setStackDescription(card.getController() + " Puts 2/2 green Bear tokens into play.");
      
      card.setFlashback(true);
      card.clearSpellAbility();
      card.addSpellAbility(spell);
      card.addSpellAbility(CardFactoryUtil.ability_Flashback(card, "5 G G", "0"));
      
    }//*************** END ************ END **************************

    //*************** START *********** START **************************
    else if(cardName.equals("Sprout"))
    {
      SpellAbility spell = new Spell(card)
      {

		private static final long serialVersionUID = 1299216756153970592L;
		public void resolve()
        {   
         makeToken();
        }
	    public void makeToken()
	    {
	          PlayerZone play = AllZone.getZone(Constant.Zone.Play, card.getController());
	          Card c = new Card();
	
	          c.setOwner(card.getController());
	          c.setController(card.getController());
	
	          c.setName("Saproling");
	          c.setImageName("G 1 1 Saproling");
	          c.setManaCost("G");
	          c.setToken(true);
	
	          c.addType("Creature");
	          c.addType("Saproling");
	          c.setBaseAttack(1);
	          c.setBaseDefense(1);
	
	          play.add(c);
	
	    	}//resolve()
	     };
	     
	     spell.setDescription("Put a 1/1 green Saproling creature token into play.");
	     spell.setStackDescription(card.getController() + " put a 1/1 green Saproling creature token into play.");
	     
	     card.clearSpellAbility();
	     card.addSpellAbility(spell);
    }//*************** END ************ END **************************

    

    //*************** START *********** START **************************
    else if(cardName.equals("Delirium Skeins"))
    {
      SpellAbility spell = new Spell(card)
      {
		private static final long serialVersionUID = 7901561313373975648L;

		public void resolve()
        {
          for(int i = 0; i < 3; i++)
            AllZone.GameAction.discardRandom(Constant.Player.Computer);

          AllZone.InputControl.setInput(CardFactoryUtil.input_discard(3));
        }//resolve()
      };
      card.clearSpellAbility();
      card.addSpellAbility(spell);
    }//*************** END ************ END **************************


    //*************** START *********** START **************************
    else if(cardName.equals("Wrap in Vigor"))
    {
      SpellAbility spell = new Spell(card)
      {
        private static final long serialVersionUID = -4235465815975050436L;

		public boolean canPlayAI()
        {
          return getAttacker() != null;
        }
        public Card getAttacker()
        {
          //target creature that is going to attack
          Combat c = ComputerUtil.getAttackers();
          Card[] att = c.getAttackers();
          if(att.length != 0)
            return att[0];
          else
            return null;
        }//getAttacker()

        public void resolve()
        {
          final Card[] c = AllZone.getZone(Constant.Zone.Play, card.getController()).getCards();

          for(int i = 0; i < c.length; i++)
            if(c[i].isCreature())
              c[i].addShield();

          AllZone.EndOfTurn.addUntil(new Command()
          {
            private static final long serialVersionUID = -3946800525315027053L;

			public void execute()
            {
              for(int i = 0; i < c.length; i++)
                c[i].resetShield();
            }
          });

        }//resolve()
      };//SpellAbility
      card.clearSpellAbility();
      card.addSpellAbility(spell);
    }//*************** END ************ END **************************
    
  //*************** START *********** START **************************
    else if(cardName.equals("Smother"))
    {
      final SpellAbility spell = new Spell(card)
      {
        private static final long serialVersionUID = 6479035316340603704L;

		public boolean canPlayAI()
        {
          CardList c = CardFactoryUtil.AI_getHumanCreature(true, 3, card, true);
          CardListUtil.sortAttack(c);
          CardListUtil.sortFlying(c);

          if(c.isEmpty())
            return false;
          else
          {
            setTargetCard(c.get(0));
            return true;
          }
        }//canPlayAI()

        public void resolve()
        {
          Card c = getTargetCard();
          if(AllZone.GameAction.isCardInPlay(c) && CardUtil.getConvertedManaCost(card.getManaCost()) <= 3 && CardFactoryUtil.canTarget(card, getTargetCard()) )
            AllZone.GameAction.destroyNoRegeneration(c);
        }//resolve()
      };//SpellAbility

      Input target = new Input()
      {
        private static final long serialVersionUID = 1877945605889747187L;
		public void showMessage()
        {
          AllZone.Display.showMessage("Select target creature for " +card.getName() +" - creature must have a converted manacost of 3 or less");
          ButtonUtil.enableOnlyCancel();
        }
        public void selectButtonCancel() {stop();}
        public void selectCard(Card card, PlayerZone zone)
        {
          if(!CardFactoryUtil.canTarget(spell, card)){
          	  AllZone.Display.showMessage("Cannot target this card (Shroud? Protection?).");
          }
          if(card.isCreature() && zone.is(Constant.Zone.Play) && CardUtil.getConvertedManaCost(card.getManaCost()) <= 3)
          {
            spell.setTargetCard(card);
            stopSetNext(new Input_PayManaCost(spell));
          }
        }
      };//Input
      card.clearSpellAbility();
      card.addSpellAbility(spell);
      spell.setBeforePayMana(target);
    }//*************** END ************ END **************************



    //*************** START *********** START **************************
    else if(cardName.equals("Strangling Soot"))
    {
      final SpellAbility spell = new Spell(card)
      {
		private static final long serialVersionUID = -3598479453933951865L;

		public boolean canPlayAI()
        {
          CardList c = CardFactoryUtil.AI_getHumanCreature(3, card, true);
          CardListUtil.sortAttack(c);
          CardListUtil.sortFlying(c);

          if(c.isEmpty())
            return false;
          else
          {
            setTargetCard(c.get(0));
            PlayerZone hand = AllZone.getZone(Constant.Zone.Hand, Constant.Player.Computer);
            return AllZone.GameAction.isCardInZone(card, hand);
          }
        }//canPlayAI()

        public void resolve()
        {
          
          Card c = getTargetCard();
          if(AllZone.GameAction.isCardInPlay(c) && c.getNetDefense() <= 3 && CardFactoryUtil.canTarget(card, getTargetCard()) )
            AllZone.GameAction.destroy(c);
          
        }//resolve()
      };//SpellAbility
      
      final SpellAbility flashback = new Spell(card)
      {

		private static final long serialVersionUID = -4009531242109129036L;

		public boolean canPlay()
    	{
    		PlayerZone grave = AllZone.getZone(Constant.Zone.Graveyard ,card.getController());

			return AllZone.GameAction.isCardInZone(card, grave);
    	}
		public boolean canPlayAI()
        {
          CardList c = CardFactoryUtil.AI_getHumanCreature(3, card, true);
          CardListUtil.sortAttack(c);
          CardListUtil.sortFlying(c);

          if(c.isEmpty())
            return false;
          else
          {
            setTargetCard(c.get(0));
            return true;
          }
        }//canPlayAI()

        public void resolve()
        {
          PlayerZone grave = AllZone.getZone(Constant.Zone.Graveyard , card.getController());
    	  PlayerZone removed = AllZone.getZone(Constant.Zone.Removed_From_Play, card.getController());
    		  
          Card c = getTargetCard();
          if(AllZone.GameAction.isCardInPlay(c) && c.getNetDefense() <= 3 && CardFactoryUtil.canTarget(card, getTargetCard()) )
            AllZone.GameAction.destroy(c);
          
          grave.remove(card);
		  removed.add(card);
        }//resolve()
      };//flashback
      
      Input targetFB = new Input()
      {
		
		private static final long serialVersionUID = -5469698194749752297L;
		public void showMessage()
        {
          AllZone.Display.showMessage("Select target creature for " +card.getName() +" - creature must have a toughness of 3 or less");
          ButtonUtil.enableOnlyCancel();
        }
        public void selectButtonCancel() {stop();}
        public void selectCard(Card card, PlayerZone zone)
        {
          if(!CardFactoryUtil.canTarget(flashback, card)){
          	  AllZone.Display.showMessage("Cannot target this card (Shroud? Protection?).");
          }
          if(card.isCreature() && zone.is(Constant.Zone.Play) && card.getNetDefense() <= 3)
          {
            flashback.setTargetCard(card);
            stopSetNext(new Input_PayManaCost(flashback));
          }
        }
      };//Input
      
      flashback.setManaCost("5 R");
      flashback.setBeforePayMana(targetFB);
      flashback.setDescription("Flashback: 5 R");

      Input target = new Input()
      {
		private static final long serialVersionUID = -198153850086215235L;
		public void showMessage()
        {
          AllZone.Display.showMessage("Select target creature for " +card.getName() +" - creature must have a toughness of 3 or less");
          ButtonUtil.enableOnlyCancel();
        }
        public void selectButtonCancel() {stop();}
        public void selectCard(Card card, PlayerZone zone)
        {
          if(!CardFactoryUtil.canTarget(spell, card)){
          	  AllZone.Display.showMessage("Cannot target this card (Shroud? Protection?).");
          }
          if(card.isCreature() && zone.is(Constant.Zone.Play) && card.getNetDefense() <= 3)
          {
            spell.setTargetCard(card);
            stopSetNext(new Input_PayManaCost(spell));
          }
        }
      };//Input
      card.clearSpellAbility();
      card.addSpellAbility(spell);
      spell.setBeforePayMana(target);
      
      card.addSpellAbility(flashback);
      
      card.setFlashback(true);
    }//*************** END ************ END **************************

    //*************** START *********** START **************************
    else if(cardName.equals("Ichor Slick"))
    {
      final SpellAbility spell = new Spell(card)
      {
		private static final long serialVersionUID = -273970706213674570L;

		public boolean canPlayAI()
        {
          CardList c = CardFactoryUtil.AI_getHumanCreature(3, card, true);
          CardListUtil.sortAttack(c);
          CardListUtil.sortFlying(c);

          if(c.isEmpty())
            return false;
          else
          {
            setTargetCard(c.get(0));
            return true;
          }
        }//canPlayAI()

        public void resolve()
        {
        	final Card[] target = new Card[1];
            final Command untilEOT = new Command()
            {
				private static final long serialVersionUID = -1615047325868708734L;

			public void execute()
              {
                if(AllZone.GameAction.isCardInPlay(target[0]) )
                {
                  target[0].addTempAttackBoost(3);
                  target[0].addTempDefenseBoost(3);
                }
              }
            };

            target[0] = getTargetCard();
            if(AllZone.GameAction.isCardInPlay(target[0]) && CardFactoryUtil.canTarget(card, target[0]))
            {
              target[0].addTempAttackBoost(-3);
              target[0].addTempDefenseBoost(-3);

              AllZone.EndOfTurn.addUntil(untilEOT);
            }
        }//resolve()
      };//SpellAbility

      Input target = new Input()
      {
        private static final long serialVersionUID = -7381927922574152604L;
		public void showMessage()
        {
          AllZone.Display.showMessage("Select target creature for " +card.getName());
          ButtonUtil.enableOnlyCancel();
        }
        public void selectButtonCancel() {stop();}
        public void selectCard(Card card, PlayerZone zone)
        {
          if(!CardFactoryUtil.canTarget(spell, card)){
          	  AllZone.Display.showMessage("Cannot target this card (Shroud? Protection?).");
          }	
          else if(card.isCreature() && zone.is(Constant.Zone.Play))
          {
            spell.setTargetCard(card);
            stopSetNext(new Input_PayManaCost(spell));
          }
        }
      };//Input
      spell.setDescription("Target creature gets -3/-3 until end of turn");

      card.clearSpellAbility();
      card.addSpellAbility(spell);
      card.addSpellAbility(CardFactoryUtil.ability_cycle(card, "2"));

      spell.setBeforePayMana(target);
    }//*************** END ************ END **************************


    //*************** START *********** START **************************
    else if(cardName.equals("Funeral Charm"))
    {
      //discard
      final SpellAbility spell_one = new Spell(card)
      {
        private static final long serialVersionUID = 8273875515630095127L;
		public boolean canPlayAI()
        {
          setTargetPlayer(Constant.Player.Human);
          return MyRandom.random.nextBoolean();
        }
        public void resolve()
        {
          if(Constant.Player.Computer.equals(getTargetPlayer()))
            AllZone.GameAction.discardRandom(getTargetPlayer());
          else
            AllZone.InputControl.setInput(CardFactoryUtil.input_discard());
        }//resolve()
      };//SpellAbility
      spell_one.setDescription("Target player discards a card.");
      spell_one.setBeforePayMana(CardFactoryUtil.input_targetPlayer(spell_one));


      //creature gets +2/-1
      final SpellAbility spell_two = new Spell(card)
      {
        private static final long serialVersionUID = -4554812851052322555L;


		public boolean canPlayAI()
        {
          CardList list = new CardList(ComputerUtil.getAttackers().getAttackers());
          list = list.filter(new CardListFilter()
          {
            public boolean addCard(Card c) {return 1 < c.getNetDefense();}
          });

          list.shuffle();
          if(list.size() > 0)
            setTargetCard(list.get(0));

          return (list.size() > 0) && MyRandom.random.nextBoolean();
        }


        public void resolve()
        {
          final Card c = getTargetCard();

          if(AllZone.GameAction.isCardInPlay(c) && CardFactoryUtil.canTarget(card, c) )
          {
            c.addTempAttackBoost(2);
            c.addTempDefenseBoost(-1);

            Command until = new Command()
            {
				private static final long serialVersionUID = 4674846621452044251L;

			public void execute()
              {
                c.addTempAttackBoost(-2);
                c.addTempDefenseBoost(1);
              }
            };//Command
            AllZone.EndOfTurn.addUntil(until);
          }//if card in play?
        }//resolve()
      };//SpellAbility
      spell_two.setDescription("Target creature gets +2/-1 until end of turn.");
      spell_two.setBeforePayMana(CardFactoryUtil.input_targetCreature(spell_two));

      card.clearSpellAbility();
      card.addSpellAbility(spell_one);
      card.addSpellAbility(spell_two);
    }//*************** END ************ END **************************


    //*************** START *********** START **************************
    else if(cardName.equals("Regrowth"))
    {
      final SpellAbility spell = new Spell(card)
      {
        private static final long serialVersionUID = -1771016287736735113L;
		public void resolve()
        {
          PlayerZone hand      = AllZone.getZone(Constant.Zone.Hand     , card.getController());
          PlayerZone graveyard = AllZone.getZone(Constant.Zone.Graveyard, card.getController());

          if(AllZone.GameAction.isCardInZone(getTargetCard(), graveyard))
          {
            graveyard.remove(getTargetCard());
            hand.add(getTargetCard());
          }
        }//resolve()
        public boolean canPlay()
        {
          PlayerZone graveyard = AllZone.getZone(Constant.Zone.Graveyard, card.getController());
          return graveyard.getCards().length != 0 && super.canPlay();
        }
      };
      Input runtime = new Input()
      {
        private static final long serialVersionUID = 3687454413838053102L;

		public void showMessage()
        {
          PlayerZone graveyard = AllZone.getZone(Constant.Zone.Graveyard, card.getController());
          Object o = AllZone.Display.getChoiceOptional("Select target card", graveyard.getCards());
          if(o == null)
            stop();
          else
          {
            spell.setStackDescription("Return " +o +" to its owner's hand");
            spell.setTargetCard((Card)o);

            stopSetNext(new Input_PayManaCost(spell));
          }
        }//showMessage()
      };
      spell.setChooseTargetAI(CardFactoryUtil.AI_targetType("All", AllZone.Computer_Graveyard));
      spell.setBeforePayMana(runtime);
      card.clearSpellAbility();
      card.addSpellAbility(spell);
    }//*************** END ************ END **************************


    //*************** START *********** START **************************
    else if(cardName.equals("Commune with Nature"))
    {
      SpellAbility spell = new Spell(card)
      {
        private static final long serialVersionUID = -7652317332073733242L;
		public boolean canPlayAI() {return false;}

        public void resolve()
        {
          String player = card.getController();
          if(player.equals(Constant.Player.Human))
            humanResolve();
          else
            computerResolve();
        }
        public void computerResolve()
        {
          //get top 5 cards of library
          CardList top = new CardList();
          int limit = AllZone.Computer_Library.getCards().length;

          for(int i = 0; i < 5 && i < limit; i++)
          {
            top.add(AllZone.Computer_Library.get(0));
            AllZone.Computer_Library.remove(0);
          }

          //put creature card in hand, if there is one
          CardList creature = top.getType("Creature");
          if(creature.size() != 0)
          {
            AllZone.Computer_Hand.add(creature.get(0));
            top.remove(creature.get(0));
          }

          //put cards on bottom of library
          for(int i = 0; i < top.size(); i++)
            AllZone.Computer_Library.add(top.get(i));
        }//computerResolve()
        public void humanResolve()
        {
          PlayerZone library = AllZone.getZone(Constant.Zone.Library, card.getController());
          PlayerZone hand    = AllZone.getZone(Constant.Zone.Hand   , card.getController());

          CardList list = new CardList();
          for(int i = 0; i < 5 && i < library.getCards().length; i++)
            list.add(library.get(i));

          //optional, select a creature
          Object o = AllZone.Display.getChoiceOptional("Select a creature", list.toArray());
          if(o != null && ((Card)o).isCreature())
          {
            AllZone.GameAction.moveTo(hand, (Card)o);
            list.remove((Card)o);
          }

          //put remaining cards on the bottom of the library
          for(int i = 0; i < list.size(); i++)
          {
            library.remove(list.get(i));
            library.add(list.get(i));
          }
        }//resolve()
      };
      card.clearSpellAbility();
      card.addSpellAbility(spell);
    }//*************** END ************ END **************************


    //*************** START *********** START **************************
    else if(cardName.equals("Kodama's Reach"))
    {
      SpellAbility spell = new Spell(card)
      {
		private static final long serialVersionUID = -3361422153566629825L;

		public void resolve()
        {
          String player = card.getController();

          if(player.equals(Constant.Player.Human))
            humanResolve();
          else
            computerResolve();
        }
        public void computerResolve()
        {
          CardList land = new CardList(AllZone.Computer_Library.getCards());
          land = land.getType("Basic");

          //just to make the computer a little less predictable
          land.shuffle();

          //3 branches: 1-no land in deck, 2-one land in deck, 3-two or more land in deck
          if(land.size() != 0)
          {
            //branch 2 - at least 1 land in library
            Card tapped = land.remove(0);
            tapped.tap();

            AllZone.Computer_Play.add(tapped);
            AllZone.Computer_Library.remove(tapped);

            //branch 3
            if(land.size() != 0)
            {
              Card toHand = land.remove(0);
              AllZone.Computer_Hand.add(toHand);
              AllZone.Computer_Library.remove(toHand);
            }
          }
        }//computerResolve()

        public void humanResolve()
        {
          PlayerZone play    = AllZone.getZone(Constant.Zone.Play   , card.getController());
          PlayerZone library = AllZone.getZone(Constant.Zone.Library, card.getController());
          PlayerZone hand    = AllZone.getZone(Constant.Zone.Hand   , card.getController());

          CardList list = new CardList(library.getCards());
          list = list.getType("Basic");

          //3 branches: 1-no land in deck, 2-one land in deck, 3-two or more land in deck

          //branch 1
          if(list.size() == 0)
            return;

          //branch 2
          Object o = AllZone.Display.getChoiceOptional("Put into play tapped", list.toArray());
          if(o != null)
          {
            Card c = (Card)o;
            c.tap();
            list.remove(c);

            library.remove(c);
            play.add(c);

            if(list.size() == 0)
              return;

            o = AllZone.Display.getChoiceOptional("Put into your hand", list.toArray());
            if(o != null)
            {
              //branch 3
              library.remove(o);
              hand.add(o);
            }
            AllZone.GameAction.shuffle(card.getController());
          }//if
        }//resolve()
      };//SpellAbility
      card.clearSpellAbility();
      card.addSpellAbility(spell);
    }//*************** END ************ END **************************
    
   
  //*************** START *********** START **************************
    else if(cardName.equals("Yawgmoth's Bargain")){
        final SpellAbility ability = new Ability(card, "0")
        {
          public void resolve()
          {
            PlayerZone library = AllZone.getZone(Constant.Zone.Library, card.getController());
            PlayerZone hand = AllZone.getZone(Constant.Zone.Hand, card.getController());
            if(library.size() != 0)
            {
              Card c = library.get(0);
              library.remove(0);
              hand.add(c);
            }
          }

          public boolean canPlayAI()
          {
            return false;
          }
        };//SpellAbility

        ability.setDescription("Pay 1 life: Draw a card.");
        ability.setStackDescription(card.getName() +" - Pay 1 life: Draw a card.");

        card.addSpellAbility(ability);

        //instead of paying mana, pay life and add to stack
        //Input showMessage() is always the first method called
        Input payLife = new Input()
        {

			private static final long serialVersionUID = 8660593629867722192L;

			public void showMessage()
			{
				AllZone.GameAction.getPlayerLife(card.getController()).subtractLife(1);

				//this order is very important, do not change
				stop();
				AllZone.Stack.push(ability);
			}
        };//Input
        ability.setBeforePayMana(payLife);
      }//*************** END ************ END **************************
    
  //*************** START *********** START **************************
    else if(cardName.equals("Necropotence"))
    {
      final CardList necroCards = new CardList();

      final Command necro = new Command()
      {
        private static final long serialVersionUID = 4511445425867383336L;

		public void execute()
        {
          if(AllZone.GameAction.isCardInPlay(card))
          {
            //put cards removed by Necropotence into player's hand
            if(necroCards.size() > 0){
              PlayerZone hand = AllZone.getZone(Constant.Zone.Hand, card.getController());

              for(int i = 0;i<necroCards.size();i++){
                hand.add(necroCards.get(i));
              }
              necroCards.clear();
            }
          }
        }
      };

      final SpellAbility ability = new Ability(card, "0")
      {
        public void resolve()
        {
          PlayerZone library = AllZone.getZone(Constant.Zone.Library, card.getController());

          if(library.size() != 0)
          {
            Card c = library.get(0);
            library.remove(0);
            necroCards.add(c); //add card to necro so that it goes into hand at end of turn
            AllZone.EndOfTurn.addAt(necro);
          }
        }

        public boolean canPlayAI()
        {
          return false;
        }
      };//SpellAbility

      ability.setDescription("1 life: Set aside the top card of your library face down. At the end of your turn, put that card into your hand.");
      ability.setStackDescription(card.getName() +" - 1 life: Set aside the top card of your library face down. At the end of your turn, put that card into your hand.");

      card.addSpellAbility(ability);

      //instead of paying mana, pay life and add to stack
      //Input showMessage() is always the first method called
      Input payLife = new Input()
      {
		private static final long serialVersionUID = -3846772748411690084L;

		public void showMessage()
        {
          AllZone.GameAction.getPlayerLife(card.getController()).subtractLife(1);

          //this order is very important, do not change
          stop();
          AllZone.Stack.push(ability);
        }
      };//Input
      ability.setBeforePayMana(payLife);

    }//*************** END ************ END **************************
    
  //*************** START *********** START **************************
    if (cardName.equals("Storm Herd"))
    {
      final SpellAbility spell = new Spell(card)
      {
        private static final long serialVersionUID = 1578037279604088948L;

		public void resolve()
        {
          int life = AllZone.GameAction.getPlayerLife(card.getController()).getLife();

          PlayerZone play = AllZone.getZone(Constant.Zone.Play, card.getController());

          //we need a new Card object for each token
          //can't just add the same Card object over and over again
          for (int i = 0; i < life; i++)
          {
            play.add(getToken());
          }
        }//resolve()

        Card getToken()
        {
          Card c = new Card();
          c.setOwner(card.getController());
          c.setController(card.getController());

          c.setName("Pegasus");
          c.setImageName("W 1 1 Pegasus");
          c.setManaCost("W");
          c.setToken(true);

          c.addType("Creature");
          c.addType("Pegasus");

          c.setBaseAttack(1);
          c.setBaseDefense(1);

          c.addIntrinsicKeyword("Flying");
          return c;
        }//getToken()
      };//SpellAbility
      card.clearSpellAbility();
      card.addSpellAbility(spell);
    }//*************** END ************ END **************************
    
    //*************** START *********** START **************************
    if (cardName.equals("Festival of Trokin"))
    {
      final SpellAbility spell = new Spell(card)
      {
		private static final long serialVersionUID = 1140489859175764227L;

		public boolean canPlay()
        {
          setStackDescription(card.getName() + " - " + card.getController() + " gains " + calculateLife() + " life.");

          return super.canPlay();
        }

        public boolean canPlayAI()
        {
          CardList creatureList = new CardList(AllZone.Computer_Play.getCards());
          creatureList = creatureList.getType("Creature");

          return creatureList.size() > 0;
        }

        int calculateLife()
        {
          PlayerZone zone = AllZone.getZone(Constant.Zone.Play, card.getController());

          CardList creatureList = new CardList(zone.getCards());
          creatureList = creatureList.getType("Creature");

          return 2 * creatureList.size();
        }

        public void resolve()
        {
          AllZone.GameAction.getPlayerLife(card.getController()).addLife(calculateLife());
        }
      };//SpellAbility
      card.clearSpellAbility();
      card.addSpellAbility(spell);
    }//*************** END ************ END **************************
    
 
    
  //*************** START *********** START **************************
    else if(cardName.equals("Absorb"))
    {
      SpellAbility spell = new Spell(card)
      {
        private static final long serialVersionUID = -2007620906017942538L;
		public void resolve()
        {
          SpellAbility sa = AllZone.Stack.pop();
          AllZone.GameAction.moveToGraveyard(sa.getSourceCard());

		  PlayerLife life = AllZone.GameAction.getPlayerLife(card.getController());
          life.addLife(3);
        }
        public boolean canPlay()
        {
          if(AllZone.Stack.size() == 0)
            return false;

          //see if spell is on stack and that opponent played it
          String opponent = AllZone.GameAction.getOpponent(card.getController());
          SpellAbility sa = AllZone.Stack.peek();

          return sa.isSpell() && opponent.equals(sa.getSourceCard().getController())
          		 && CardFactoryUtil.isCounterable(sa.getSourceCard());
        }
      };
      card.clearSpellAbility();
      card.addSpellAbility(spell);
    }//*************** END ************ END **************************

    
	//*************** START *********** START **************************
    else if(cardName.equals("Undermine"))
    {
      SpellAbility spell = new Spell(card)
      {
		private static final long serialVersionUID = -4999966043862729936L;
		public void resolve()
        {
          SpellAbility sa = AllZone.Stack.pop();
          AllZone.GameAction.moveToGraveyard(sa.getSourceCard());

		  String opponent = AllZone.GameAction.getOpponent(card.getController());
          AllZone.GameAction.getPlayerLife(opponent).subtractLife(3);

        }
        public boolean canPlay()
        {
          if(AllZone.Stack.size() == 0)
            return false;

          //see if spell is on stack and that opponent played it
          String opponent = AllZone.GameAction.getOpponent(card.getController());
          SpellAbility sa = AllZone.Stack.peek();

          return sa.isSpell() && opponent.equals(sa.getSourceCard().getController())
          		 && CardFactoryUtil.isCounterable(sa.getSourceCard());
        }
      };
      card.clearSpellAbility();
      card.addSpellAbility(spell);
    }//*************** END ************ END **************************
    
  //*************** START *********** START **************************
    else if(cardName.equals("Punish Ignorance"))
    {
      SpellAbility spell = new Spell(card)
      {
        private static final long serialVersionUID = 6845184687406705133L;
		public void resolve()
        {
          SpellAbility sa = AllZone.Stack.pop();
          AllZone.GameAction.moveToGraveyard(sa.getSourceCard());

		  String opponent = AllZone.GameAction.getOpponent(card.getController());
          AllZone.GameAction.getPlayerLife(opponent).subtractLife(3);
          
          String player = card.getController();
          AllZone.GameAction.getPlayerLife(player).addLife(3);

        }
        public boolean canPlay()
        {
          if(AllZone.Stack.size() == 0)
            return false;

          //see if spell is on stack and that opponent played it
          String opponent = AllZone.GameAction.getOpponent(card.getController());
          SpellAbility sa = AllZone.Stack.peek();

          return sa.isSpell() && opponent.equals(sa.getSourceCard().getController())
          		 && CardFactoryUtil.isCounterable(sa.getSourceCard());
        }
      };
      card.clearSpellAbility();
      card.addSpellAbility(spell);
    }//*************** END ************ END **************************
    
  //*************** START *********** START **************************
    else if(cardName.equals("Exclude"))
    {
      SpellAbility spell = new Spell(card)
      {
		private static final long serialVersionUID = -5615796501064636046L;
		public void resolve()
        {
          SpellAbility sa = AllZone.Stack.pop();
          AllZone.GameAction.moveToGraveyard(sa.getSourceCard());
		  AllZone.GameAction.drawCard(card.getController());
        }
        public boolean canPlay()
        {
          if(AllZone.Stack.size() == 0)
            return false;

          //see if spell is on stack and that opponent played it
          String opponent = AllZone.GameAction.getOpponent(card.getController());
          SpellAbility sa = AllZone.Stack.peek();

          //is spell?, did opponent play it?, is this a creature spell?
          return sa.isSpell() &&
                 opponent.equals(sa.getSourceCard().getController()) &&
                 sa.getSourceCard().getType().contains("Creature")
                 && CardFactoryUtil.isCounterable(sa.getSourceCard());
        }//canPlay()
      };
      card.clearSpellAbility();
      card.addSpellAbility(spell);
    }//*************** END ************ END **************************
    
    //*************** START *********** START **************************
    else if(cardName.equals("Eladamri's Call"))
    {
      final SpellAbility spell = new Spell(card)
      {
		private static final long serialVersionUID = -6495398165357932918L;
		public void resolve()
    	{
    	  String player = card.getController();
    	  if(player.equals(Constant.Player.Human))
    		humanResolve();
    	  else
    		computerResolve();
    	}
    	public void humanResolve()
    	{
    	  CardList creatures = new CardList(AllZone.Human_Library.getCards());
    	  creatures = creatures.getType("Creature");

    	  Object check = AllZone.Display.getChoiceOptional("Select creature", creatures.toArray());
    	  if(check != null)
    	  {
    		PlayerZone hand = AllZone.getZone(Constant.Zone.Hand, card.getController());
    		AllZone.GameAction.moveTo(hand, (Card)check);
    	  }
    	  AllZone.GameAction.shuffle(Constant.Player.Human);
    	}
    	public void computerResolve()
    	{
    	  Card[] library = AllZone.Computer_Library.getCards();
    	  CardList list = new CardList(library);
    	  list = list.getType("Creature");

    	  if (list.size() > 0) 
    	  {
	    	  //pick best creature
	    	  Card c = CardFactoryUtil.AI_getBestCreature(list);
	    	  if(c == null)
	    		c = list.get(0);
	    	  AllZone.Computer_Library.remove(c);
	    	  AllZone.Computer_Hand.add(c);
	    	  CardList cl = new CardList();
	    	  cl.add(c);
	    	  AllZone.Display.getChoiceOptional("Computer picked:", cl.toArray());
    	  }
    	}
    	public boolean canPlay()
    	{
    	  PlayerZone library = AllZone.getZone(Constant.Zone.Library, card.getController());
    	  return library.getCards().length != 0;
    	}
    	public boolean canPlayAI()
    	{
    	  CardList creature = new CardList();
    	  creature.addAll(AllZone.Computer_Library.getCards());
    	  creature = creature.getType("Creature");
    	  return creature.size() != 0;
    	}
      };//SpellAbility
      card.clearSpellAbility();
      card.addSpellAbility(spell);
    }//*************** END ************ END **************************
    
  //*************** START *********** START **************************
    else if(cardName.equals("Dismiss"))
    {
      SpellAbility spell = new Spell(card)
      {
        private static final long serialVersionUID = -7959473218345045760L;
		public void resolve()
        {
          SpellAbility sa = AllZone.Stack.pop();
          AllZone.GameAction.moveToGraveyard(sa.getSourceCard());

          AllZone.GameAction.drawCard(card.getController());

        }
        public boolean canPlay()
        {
          if(AllZone.Stack.size() == 0)
            return false;

          //see if spell is on stack and that opponent played it
          String opponent = AllZone.GameAction.getOpponent(card.getController());
          SpellAbility sa = AllZone.Stack.peek();

          return sa.isSpell() && opponent.equals(sa.getSourceCard().getController()) 
          		 && CardFactoryUtil.isCounterable(sa.getSourceCard());
        }
      };
      card.clearSpellAbility();
      card.addSpellAbility(spell);
    }//*************** END ************ END **************************
    
  //*************** START *********** START **************************
    else if(cardName.equals("Global Ruin"))
    {
     final CardList target = new CardList();
     //need to use arrays so we can declare them final and still set the values in the input and runtime classes. This is a hack.
     final int[] index = new int[1];
     final int[] countBase = new int[1];
     final Vector<String> humanBasic = new Vector<String>();

     final SpellAbility spell = new Spell(card)
     {
	   private static final long serialVersionUID = 5739127258598357186L;
	   
	   public boolean canPlayAI()
       {
    	 return false;
    	 //should check if computer has land in hand, or if computer has more basic land types than human.
       }
       @SuppressWarnings("unchecked") // computerBasic
	   public void resolve()
       {
    	 //add computer's lands to target
    	 @SuppressWarnings("unused") // computerCountBasic
		 int computerCountBase = 0;
    	 @SuppressWarnings("unused") // computerBasic
		 Vector<?> computerBasic = new Vector();

       //figure out which basic land types the computer has
       CardList land = new CardList(AllZone.Computer_Play.getCards());
       String basic[] = {"Forest", "Plains", "Mountain", "Island", "Swamp"};

       for (int i = 0; i < basic.length; i++)
       {
    	 CardList cl = land.getType(basic[i]);
    	 if (!cl.isEmpty())
    	 {
    	   //remove one land of this basic type from this list
    	   //the computer AI should really jump in here and select the land which is the best.
    	   //to determine the best look at which lands have enchantments, which lands are tapped
    	   cl.remove(cl.get(0));
    	   //add the rest of the lands of this basic type to the target list, this is the list which will be sacrificed.
    	   target.addAll(cl.toArray());
    	 }
       }

    	 //when this spell resolves all basic lands which were not selected are sacrificed.
    	 for(int i = 0; i < target.size(); i++)
    	   if(AllZone.GameAction.isCardInPlay(target.get(i)))
    		 AllZone.GameAction.sacrifice(target.get(i));
       }//resolve()
     };//SpellAbility


     final Input input = new Input()
     {
		private static final long serialVersionUID = 1739423591445361917L;
		private int count;
       public void showMessage()
       { //count is the current index we are on.
    	 //countBase[0] is the total number of basic land types the human has
    	 //index[0] is the number to offset the index by
    	 count = countBase[0] - index[0] - 1; //subtract by one since humanBasic is 0 indexed.
    	 if(count<0){
    		//need to reset the variables in case they cancel this spell and it stays in hand.
    		humanBasic.clear();
    		countBase[0] = 0;
    		index[0] = 0;
    		stop();
    	 }
    	 else{
    		AllZone.Display.showMessage("Select target " + humanBasic.get(count) +" land to not sacrifice");
    		ButtonUtil.enableOnlyCancel();
    	 }
       }
       public void selectButtonCancel() {stop();}
       public void selectCard(Card c, PlayerZone zone)
       {
    	 if(c.isLand() && zone.is(Constant.Zone.Play) && c.getController().equals(Constant.Player.Human) && c.getName().equals(humanBasic.get(count)))
    	 {
    	   //get all other basic[count] lands human player controls and add them to target
    	  PlayerZone humanPlay = AllZone.getZone(Constant.Zone.Play, Constant.Player.Human);
    	   CardList land = new CardList(humanPlay.getCards());
    	   CardList cl = land.getType((String)humanBasic.get(count));
    	   cl.remove(c);
    	   target.addAll(cl.toArray());

    	   index[0]++;
    	   showMessage();

    	   if(index[0] >= humanBasic.size())
    		 stopSetNext(new Input_PayManaCost(spell));
    	 }
       }//selectCard()
     };//Input

     Input runtime = new Input()
     {
       private static final long serialVersionUID = -122635387376995855L;

	public void showMessage()
       {
    	 countBase[0] = 0;
    	//figure out which basic land types the human has
    	//put those in an set to use later
    	CardList land = new CardList(AllZone.Human_Play.getCards());
    	String basic[] = {"Forest", "Plains", "Mountain", "Island", "Swamp"};

    	for (int i = 0; i < basic.length; i++)
    	{
    	  CardList c = land.getType(basic[i]);
    	  if (!c.isEmpty())
    	  {
    		humanBasic.add(basic[i]);
    		countBase[0]++;
    	  }
    	}
    	if(countBase[0] == 0){
    	   //human has no basic land, so don't prompt to select one.
    	   stop();
    	}
    	else{
    	   index[0] = 0;
    	   target.clear();
    	   stopSetNext(input);
    	}
       }
     };//Input

     card.clearSpellAbility();
     card.addSpellAbility(spell);
     spell.setBeforePayMana(runtime);
    }//*************** END ************ END **************************
    
  //*************** START *********** START **************************
    else if(cardName.equals("Gerrard's Verdict"))
    {
      SpellAbility spell = new Spell(card)
      {
    	  
    	private static final long serialVersionUID = 4734024742326763385L;
		public boolean canPlayAI() 
    	{
    		PlayerZone humanHand = AllZone.getZone(Constant.Zone.Hand, Constant.Player.Human);
    		if (humanHand.size() >= 2)
    			return true;
    		else
    			return false;
    	}
    	  
    	public void resolve()
        {
          String player = card.getController();
          if(player.equals(Constant.Player.Human))
            humanResolve();
          else
            computerResolve();
        }
        public void humanResolve()
        {
          PlayerZone hand = AllZone.getZone(Constant.Zone.Hand, Constant.Player.Computer);
          CardList list = new CardList(hand.getCards());
          list.shuffle();
          
          if (list.size()== 0)
        	  return;
          
          Card c1 = list.get(0);
          list.remove(c1);
          AllZone.Computer_Graveyard.add(c1);
          AllZone.Computer_Hand.remove(c1);          
          
          if (list.size()== 0)
        	  return;
          
          Card c2 = list.get(0);
          list.remove(c2);
          AllZone.Computer_Graveyard.add(c2);
          AllZone.Computer_Hand.remove(c2);    
          
          if (c1.getType().contains("Land")) {
        	  PlayerLife life = AllZone.GameAction.getPlayerLife(Constant.Player.Human);
        	  life.addLife(3);
          }
          
          if (c2.getType().contains("Land")) {
        	  PlayerLife life = AllZone.GameAction.getPlayerLife(Constant.Player.Human);
        	  life.addLife(3);
          }         
          

        }//resolve()
        public void computerResolve()
        {
        	PlayerZone hand = AllZone.getZone(Constant.Zone.Hand, Constant.Player.Human);
        	PlayerZone grave = AllZone.getZone(Constant.Zone.Graveyard, Constant.Player.Human);
            CardList list = new CardList(hand.getCards());
            
            if (list.size() > 0){
            
	            Object o = AllZone.Display.getChoiceOptional("First card to discard", list.toArray());
	            
	            Card c = (Card)o;
	      		list.remove(c);
	
	      		hand.remove(c);
	      		grave.add(c);
	      		
	      		if(c.getType().contains("Land")) {
	      			PlayerLife life = AllZone.GameAction.getPlayerLife(Constant.Player.Computer);
	      			life.addLife(3);
	      		}
	      		
	      		if (list.size() > 0)
	      		{
		      		Object o2 = AllZone.Display.getChoiceOptional("Second card to discard", list.toArray());
		            
		            Card c2 = (Card)o2;
		      		list.remove(c2);
		
		      		hand.remove(c2);
		      		grave.add(c2);
		      		
		      		if(c2.getType().contains("Land")) {
		      			PlayerLife life = AllZone.GameAction.getPlayerLife(Constant.Player.Computer);
		      			life.addLife(3);
		      		}
	      		}
            }
        }
      };
      card.clearSpellAbility();
      card.addSpellAbility(spell);
    }//*************** END ************ END **************************
    
  //*************** START *********** START **************************
    else if(cardName.equals("Temporal Spring"))
    {
      final SpellAbility spell = new Spell(card)
      {
		private static final long serialVersionUID = 2649912511833536966L;

		public boolean canPlayAI()
        {
          CardList human = CardFactoryUtil.AI_getHumanCreature(card, true);
          return 3 < AllZone.Phase.getTurn() && 0 < human.size();
        }
        public void chooseTargetAI()
        {
          CardList human = CardFactoryUtil.AI_getHumanCreature(card, true);
          setTargetCard(CardFactoryUtil.AI_getBestCreature(human));
        }

        public void resolve()
        {
          if(AllZone.GameAction.isCardInPlay(getTargetCard()) && CardFactoryUtil.canTarget(card, getTargetCard()) )
          {
            if(getTargetCard().isToken())
              AllZone.getZone(getTargetCard()).remove(getTargetCard());
            else
            {
              AllZone.GameAction.moveToTopOfLibrary(getTargetCard());
            }
          }//if
        }//resolve()
      };//SpellAbility
      Input target = new Input()
      { 

		private static final long serialVersionUID = 3852696858086356864L;
		public void showMessage()
        {
          AllZone.Display.showMessage("Select target permanent for " +spell.getSourceCard());
          ButtonUtil.enableOnlyCancel();
        }
        public void selectButtonCancel() {stop();}
        public void selectCard(Card c, PlayerZone zone)
        {
          if(!CardFactoryUtil.canTarget(spell, c)){
          	  AllZone.Display.showMessage("Cannot target this card (Shroud? Protection?).");
          }
          else if(zone.is(Constant.Zone.Play))
          {
            spell.setTargetCard(c);
            stopSetNext(new Input_PayManaCost(spell));
          }
        }
      };//Input

      spell.setBeforePayMana(target);
      card.clearSpellAbility();
      card.addSpellAbility(spell);
    }//*************** END ************ END **************************
    
    
  //*************** START *********** START **************************
    else if(cardName.equals("Boomerang") || cardName.equals("Eye of Nowhere"))
    {
      final SpellAbility spell = new Spell(card)
      {
		private static final long serialVersionUID = 5383879224433456795L;

		public boolean canPlayAI()
        {
          CardList human = CardFactoryUtil.AI_getHumanCreature(card, true);
          return 3 < AllZone.Phase.getTurn() && 0 < human.size();
        }
        public void chooseTargetAI()
        {
          CardList human = CardFactoryUtil.AI_getHumanCreature(card, true);
          setTargetCard(CardFactoryUtil.AI_getBestCreature(human));
        }

        public void resolve()
        {
          if(AllZone.GameAction.isCardInPlay(getTargetCard()) && CardFactoryUtil.canTarget(card, getTargetCard()) )
          {
            if(getTargetCard().isToken())
              AllZone.getZone(getTargetCard()).remove(getTargetCard());
            else
            {
              PlayerZone hand = AllZone.getZone(Constant.Zone.Hand, getTargetCard().getOwner());
              AllZone.GameAction.moveTo(hand, getTargetCard());
            }
            @SuppressWarnings("unused") // targetManaCost
			String targetManaCost = getTargetCard().getManaCost();
            //System.out.println("target card has a converted manacost of: " +CardUtil.getConvertedManaCost(targetManaCost));
          }//if
        }//resolve()
      };//SpellAbility
      Input target = new Input()
      {
        private static final long serialVersionUID = 7717499561403038165L;
		public void showMessage()
        {
          AllZone.Display.showMessage("Select target permanent for " +spell.getSourceCard());
          ButtonUtil.enableOnlyCancel();
        }
        public void selectButtonCancel() {stop();}
        public void selectCard(Card c, PlayerZone zone)
        {
          if(!CardFactoryUtil.canTarget(spell, c)){
          	  AllZone.Display.showMessage("Cannot target this card (Shroud? Protection?).");
          }
          else if(zone.is(Constant.Zone.Play))
          {
            spell.setTargetCard(c);
            stopSetNext(new Input_PayManaCost(spell));
          }
        }
      };//Input

      spell.setBeforePayMana(target);
      card.clearSpellAbility();
      card.addSpellAbility(spell);
    }//*************** END ************ END **************************
    
  //*************** START *********** START **************************
    else if(cardName.equals("Culling Sun"))
    {
     SpellAbility spell = new Spell(card)
    {
    private static final long serialVersionUID = 2169815434022673011L;
	public void resolve()
    {
    CardList all = new CardList();
    all.addAll(AllZone.Human_Play.getCards());
    all.addAll(AllZone.Computer_Play.getCards());

    for(int i = 0; i < all.size(); i++)
    {
      Card c = all.get(i);
      int convertedManaCost = CardUtil.getConvertedManaCost(c.getManaCost());
      if(c.isCreature() && (convertedManaCost <= 3))
        AllZone.GameAction.destroy(c);
    }
    }//resolve()
    public boolean canPlayAI()
    {
    CardList human    = new CardList(AllZone.Human_Play.getCards());
    CardList computer = new CardList(AllZone.Computer_Play.getCards());

    human    = human.getType("Creature");
    computer = computer.getType("Creature");

    //the computer will at least destroy 2 more human creatures
    return computer.size() < human.size()-1  || (AllZone.Computer_Life.getLife() < 7 && !human.isEmpty());
    }
    };//SpellAbility
    card.clearSpellAbility();
    card.addSpellAbility(spell);
    }//*************** END ************ END **************************
    
  //*************** START *********** START **************************
    else if(cardName.equals("Retribution of the Meek"))
    {
     SpellAbility spell = new Spell(card)
    {
    	 private static final long serialVersionUID = 4989080454206680708L;
    	 public void resolve()
	     {
	    	CardList all = new CardList();
		    all.addAll(AllZone.Human_Play.getCards());
		    all.addAll(AllZone.Computer_Play.getCards());
		
		    for(int i = 0; i < all.size(); i++)
			{
			      Card c = all.get(i);
			      int power = c.getNetAttack();
			      if(c.isCreature() && (power >= 4))
			        AllZone.GameAction.destroyNoRegeneration(c);
			}
	    }//resolve()
	    public boolean canPlayAI()
	    {
		    CardList human    = new CardList(AllZone.Human_Play.getCards());
		    CardList computer = new CardList(AllZone.Computer_Play.getCards());
		
		    human    = human.getType("Creature");
		    computer = computer.getType("Creature");
		    
		    human = human.filter(new CardListFilter()
		    {
				public boolean addCard(Card c) {
					return c.getNetAttack() >= 4;
				}	
		    });
		    
		    human = human.filter(new CardListFilter()
		    {
				public boolean addCard(Card c) {
					return c.getNetAttack() >= 4;
				} 	
		    });
		
		    //the computer will at least destroy 2 more human creatures
		    return computer.size() < human.size()-1  || (AllZone.Computer_Life.getLife() < 7 && !human.isEmpty());
		}
	 };//SpellAbility
	 card.clearSpellAbility();
	 card.addSpellAbility(spell);
    }//*************** END ************ END **************************
    
  //*************** START *********** START **************************
    else if(cardName.equals("Mass Calcify"))
    {
     SpellAbility spell = new Spell(card)
    {
    private static final long serialVersionUID = -3985301372801316515L;
	public void resolve()
    {
    CardList all = new CardList();
    all.addAll(AllZone.Human_Play.getCards());
    all.addAll(AllZone.Computer_Play.getCards());

    for(int i = 0; i < all.size(); i++)
    {
      Card c = all.get(i);
      //int convertedManaCost = CardUtil.getConvertedManaCost(c.getManaCost());
      if(c.isCreature() && !CardUtil.getColors(c).contains(Constant.Color.White))
        AllZone.GameAction.destroy(c);
    }
    }//resolve()
    public boolean canPlayAI()
    {
    CardList human    = new CardList(AllZone.Human_Play.getCards());
    CardList computer = new CardList(AllZone.Computer_Play.getCards());

    human    = human.getType("Creature");
    computer = computer.getType("Creature");

    //the computer will at least destroy 2 more human creatures
    return computer.size() < human.size()-1  || (AllZone.Computer_Life.getLife() < 7 && !human.isEmpty());
    }
    };//SpellAbility
    card.clearSpellAbility();
    card.addSpellAbility(spell);
    }//*************** END ************ END **************************
    
  //*************** START *********** START **************************
    else if(cardName.equals("Cleanse"))
    {
     SpellAbility spell = new Spell(card)
    {
		private static final long serialVersionUID = 6329910910925881386L;
		public void resolve()
	    {
	    CardList all = new CardList();
	    all.addAll(AllZone.Human_Play.getCards());
	    all.addAll(AllZone.Computer_Play.getCards());
	
	    for(int i = 0; i < all.size(); i++)
	    {
	      Card c = all.get(i);
	      
	      if(c.isCreature() && CardUtil.getColors(c).contains(Constant.Color.Black))
	        AllZone.GameAction.destroy(c);
	    }
	    }//resolve()
	    public boolean canPlayAI()
	    {
	    CardList hum = new CardList(AllZone.Human_Play.getCards());
	    CardList comp = new CardList(AllZone.Computer_Play.getCards());
	
	    hum = hum.getType("Creature");
	    comp = comp.getType("Creature");
	    
	    CardList human = new CardList();    
	    CardList computer = new CardList();
	    
	    for (int i=0; i<hum.size();i++)
	    {
	    	Card c = hum.getCard(i);
	    	if (CardUtil.getColors(c).contains(Constant.Color.Black)) {
	    		human.add(c);
	    	}
	    }
	    for (int i=0; i<comp.size();i++)
	    {
	    	Card c = comp.getCard(i);
	    	if (CardUtil.getColors(c).contains(Constant.Color.Black)) {
	    		computer.add(c);
	    	}
	    }
	    
	    //the computer will at least destroy 2 more human creatures
	    return computer.size() < human.size()-1;
	    }
    };//SpellAbility
    card.clearSpellAbility();
    card.addSpellAbility(spell);
    }//*************** END ************ END **************************
    
  //*************** START *********** START **************************
    else if(cardName.equals("Nature's Ruin"))
    {
     SpellAbility spell = new Spell(card)
    {
	private static final long serialVersionUID = -2465672405076170648L;
	
	public void resolve()
    {
    CardList all = new CardList();
    all.addAll(AllZone.Human_Play.getCards());
    all.addAll(AllZone.Computer_Play.getCards());

    for(int i = 0; i < all.size(); i++)
    {
      Card c = all.get(i);
      
      if(c.isCreature() && CardUtil.getColors(c).contains(Constant.Color.Green))
        AllZone.GameAction.destroy(c);
    }
    }//resolve()
    public boolean canPlayAI()
    {
    CardList hum = new CardList(AllZone.Human_Play.getCards());
    CardList comp = new CardList(AllZone.Computer_Play.getCards());

    hum = hum.getType("Creature");
    comp = comp.getType("Creature");
    
    CardList human = new CardList();    
    CardList computer = new CardList();
    
    for (int i=0; i<hum.size();i++)
    {
    	Card c = hum.getCard(i);
    	if (CardUtil.getColors(c).contains(Constant.Color.Green)) {
    		human.add(c);
    	}
    }
    for (int i=0; i<comp.size();i++)
    {
    	Card c = comp.getCard(i);
    	if (CardUtil.getColors(c).contains(Constant.Color.Green)) {
    		computer.add(c);
    	}
    }
    
    //the computer will at least destroy 2 more human creatures
    return computer.size() < human.size()-1;
    }
    };//SpellAbility
    card.clearSpellAbility();
    card.addSpellAbility(spell);
    }//*************** END ************ END **************************
    
  //*************** START *********** START **************************
    else if(cardName.equals("Perish"))
    {
     SpellAbility spell = new Spell(card)
    {
	private static final long serialVersionUID = -9022470313385775867L;
	
	public void resolve()
    {
    CardList all = new CardList();
    all.addAll(AllZone.Human_Play.getCards());
    all.addAll(AllZone.Computer_Play.getCards());

    for(int i = 0; i < all.size(); i++)
    {
      Card c = all.get(i);
      
      if(c.isCreature() && CardUtil.getColors(c).contains(Constant.Color.Green))
        AllZone.GameAction.destroyNoRegeneration(c);
    }
    }//resolve()
    public boolean canPlayAI()
    {
    CardList hum = new CardList(AllZone.Human_Play.getCards());
    CardList comp = new CardList(AllZone.Computer_Play.getCards());

    hum = hum.getType("Creature");
    comp = comp.getType("Creature");
    
    CardList human = new CardList();    
    CardList computer = new CardList();
    
    for (int i=0; i<hum.size();i++)
    {
    	Card c = hum.getCard(i);
    	if (CardUtil.getColors(c).contains(Constant.Color.Green)) {
    		human.add(c);
    	}
    }
    for (int i=0; i<comp.size();i++)
    {
    	Card c = comp.getCard(i);
    	if (CardUtil.getColors(c).contains(Constant.Color.Green)) {
    		computer.add(c);
    	}
    }
    
    //the computer will at least destroy 2 more human creatures
    return computer.size() < human.size()-1;
    }
    };//SpellAbility
    card.clearSpellAbility();
    card.addSpellAbility(spell);
    }//*************** END ************ END **************************
    
  //*************** START *********** START **************************
    else if(cardName.equals("Virtue's Ruin"))
    {
     SpellAbility spell = new Spell(card)
    {

		private static final long serialVersionUID = -4805304550512861722L;
		public void resolve()
	    {
	    CardList all = new CardList();
	    all.addAll(AllZone.Human_Play.getCards());
	    all.addAll(AllZone.Computer_Play.getCards());
	
	    for(int i = 0; i < all.size(); i++)
	    {
	      Card c = all.get(i);
	      
	      if(c.isCreature() && CardUtil.getColors(c).contains(Constant.Color.White))
	        AllZone.GameAction.destroy(c);
	    }
	    }//resolve()
	    public boolean canPlayAI()
	    {
	    CardList hum = new CardList(AllZone.Human_Play.getCards());
	    CardList comp = new CardList(AllZone.Computer_Play.getCards());
	
	    hum = hum.getType("Creature");
	    comp = comp.getType("Creature");
	    
	    CardList human = new CardList();    
	    CardList computer = new CardList();
	    
	    for (int i=0; i<hum.size();i++)
	    {
	    	Card c = hum.getCard(i);
	    	if (CardUtil.getColors(c).contains(Constant.Color.White)) {
	    		human.add(c);
	    	}
	    }
	    for (int i=0; i<comp.size();i++)
	    {
	    	Card c = comp.getCard(i);
	    	if (CardUtil.getColors(c).contains(Constant.Color.White)) {
	    		computer.add(c);
	    	}
	    }
	    
	    //the computer will at least destroy 2 more human creatures
	    return computer.size() < human.size()-1;
	    }
    };//SpellAbility
    card.clearSpellAbility();
    card.addSpellAbility(spell);
    }//*************** END ************ END **************************
    
  //*************** START *********** START **************************
    else if(cardName.equals("Overwhelming Intellect"))
    {
      SpellAbility spell = new Spell(card)
      {
		private static final long serialVersionUID = -8825219868732813877L;
		public void resolve()
        {
          SpellAbility sa = AllZone.Stack.pop();
          AllZone.GameAction.moveToGraveyard(sa.getSourceCard());
          
          int convertedManaCost = CardUtil.getConvertedManaCost(sa.getSourceCard().getManaCost());
          for (int i=0;i < convertedManaCost; i++){
        	  AllZone.GameAction.drawCard(card.getController());
          }

        }
        public boolean canPlay()
        {
          if(AllZone.Stack.size() == 0)
            return false;

          //see if spell is on stack and that opponent played it
          String opponent = AllZone.GameAction.getOpponent(card.getController());
          SpellAbility sa = AllZone.Stack.peek();
          return sa.isSpell() &&
          	opponent.equals(sa.getSourceCard().getController()) &&
          	sa.getSourceCard().getType().contains("Creature") && 
          	CardFactoryUtil.isCounterable(sa.getSourceCard());
          
        
        }
      };
      card.clearSpellAbility();
      card.addSpellAbility(spell);
    }//*************** END ************ END **************************
  
 
    //*************** START *********** START **************************
    else if(cardName.equals("Duress"))
    {
      final SpellAbility spell = new Spell(card)
      {
        private static final long serialVersionUID = 2180416205027322268L;

		public void resolve()
        {

          Card choice = null;

          //check for no cards in hand on resolve
          PlayerZone hand = AllZone.getZone(Constant.Zone.Hand, getTargetPlayer());
          CardList cards = new CardList(hand.getCards());
          
          CardList nonCreatureCards = new CardList();
          
          
          for (int i=0;i < cards.size(); i++)
          {
        	  if(!cards.get(i).getType().contains("Creature") && !cards.get(i).getType().contains("Land")) //remove land + creats
        	  {
        		  //System.out.println("Duress: " + cards.get(i).getType());
        		  //cards.remove(i);
        		  nonCreatureCards.add(cards.get(i));
          	  }
        	  
          }
               
          if(cards.size() == 0)
              return;

          //human chooses
          if(card.getController().equals(Constant.Player.Human))
          {
        	AllZone.Display.getChoice("Revealing hand", cards.toArray());
        	if(nonCreatureCards.size() == 0)
                 return;
            choice = (Card) AllZone.Display.getChoice("Choose", nonCreatureCards.toArray());
          }
          else//computer chooses
          {
        	if(nonCreatureCards.size() == 0)
               return;
            choice = CardUtil.getRandom(nonCreatureCards.toArray());
          }

          AllZone.GameAction.discard(choice);
        }//resolve()

        public boolean canPlayAI()
        {
          Card[] c = removeLandAndCreats(AllZone.Human_Hand.getCards());
          return 0 < c.length;
        }

        Card[] removeLandAndCreats(Card[] in)
        {
          CardList c = new CardList(in);
          c = c.filter(new CardListFilter()
          {
            public boolean addCard(Card c)
            {
              //return !c.isLand();
            	if (!c.isLand() && !c.isCreature())
            		return true;
            	else return false;
            }
          });
          return c.toArray();
        }//removeLand()
      };//SpellAbility spell
      spell.setChooseTargetAI(CardFactoryUtil.AI_targetHuman());
      card.clearSpellAbility();
      card.addSpellAbility(spell);

      spell.setBeforePayMana(CardFactoryUtil.input_targetPlayer(spell));
    }//*************** END ************ END **************************
    
    //*************** START *********** START **************************
    else if(cardName.equals("Ostracize"))
    {
      final SpellAbility spell = new Spell(card)
      {
        private static final long serialVersionUID = -8919895406095857866L;

		public void resolve()
        {

          Card choice = null;

          //check for no cards in hand on resolve
          PlayerZone hand = AllZone.getZone(Constant.Zone.Hand, getTargetPlayer());
          CardList cards = new CardList(hand.getCards());
          CardList creatureCards = new CardList();
          
          
          for (int i=0;i < cards.size(); i++)
          {
        	  if(cards.get(i).getType().contains("Creature"))
        	  {
        		  //System.out.println("ostracize: " + cards.get(i).getType());
        		  creatureCards.add(cards.get(i));
        	  }		 
          }
               
          if(cards.size() == 0)
            return;

          //human chooses
          if(card.getController().equals(Constant.Player.Human))
          {
        	AllZone.Display.getChoice("Revealing hand", cards.toArray());
        	if(creatureCards.size() == 0)
                return;
            choice = (Card) AllZone.Display.getChoice("Choose", creatureCards.toArray());
          }
          else//computer chooses
          {
        	if(creatureCards.size() == 0)
               return;
            choice = CardUtil.getRandom(creatureCards.toArray());
          }

          AllZone.GameAction.discard(choice);
        }//resolve()

        public boolean canPlayAI()
        {
          Card[] c = removeNonCreats(AllZone.Human_Hand.getCards());
          return 0 < c.length;
        }

        Card[] removeNonCreats(Card[] in)
        {
          CardList c = new CardList(in);
          c = c.filter(new CardListFilter()
          {
            public boolean addCard(Card c)
            {
              return c.isCreature();
            }
          });
          return c.toArray();
        }//removeLand()
      };//SpellAbility spell
      spell.setChooseTargetAI(CardFactoryUtil.AI_targetHuman());
      card.clearSpellAbility();
      card.addSpellAbility(spell);

      spell.setBeforePayMana(CardFactoryUtil.input_targetPlayer(spell));
    }//*************** END ************ END **************************
  
  //*************** START *********** START **************************
    else if(cardName.equals("Hoofprints of the Stag")) 
    {
    	Command intoPlay = new Command()
        {
    	   private static final long serialVersionUID = -7275454992618058248L;
		public boolean firstTime = true;
           public void execute()
           {

             if(firstTime){
           	  card.setCounter(Counters.HOOFPRINT, 0); 
             }
             firstTime = false;
           }
         };
         
         card.addComesIntoPlayCommand(intoPlay);
         
         final SpellAbility a2 = new Ability(card, "2 W")
 	     {
 	        public void resolve()
 	        {
 	           card.subtractCounter(Counters.HOOFPRINT, 4);

 	           Card c = new Card();

 	           c.setOwner(card.getController());
 	           c.setController(card.getController());

 	           c.setName("Elemental");
 	           c.setImageName("W 4 4 Elemental");
 	           c.setManaCost("W");
 	           c.setToken(true);
 	           
 	           c.addType("Creature");
 	           c.addType("Elemental");
 	           c.addIntrinsicKeyword("Flying");
 	           c.setBaseAttack(4);
 	           c.setBaseDefense(4);
 	           
 	           PlayerZone play = AllZone.getZone(Constant.Zone.Play, card.getController());
 	           play.add(c);
 	        
 	        }
 	        
 	       public boolean canPlay()
 	       {
 	    	  SpellAbility sa;
 	    	  for (int i=0; i<AllZone.Stack.size(); i++)
 	    	  {
 	    	       sa = AllZone.Stack.peek(i);
 	    	       if (sa.getSourceCard().equals(card))
 	    	             return false;
 	    	  }
 	    	  
 	          return card.getCounters(Counters.HOOFPRINT) >= 4 && AllZone.getZone(card).is(Constant.Zone.Play) &&
 	          						  AllZone.Phase.getActivePlayer().equals(card.getController()) &&
 	          						  !AllZone.Phase.getPhase().equals("End of Turn");
 	       }//canPlay()
 	       public boolean canPlayAI()
 	       {
 	    	   return true;
 	       }
 	     };//spellAbility
 	     
 	     a2.setDescription("2 W, Remove four hoofprint counters from Hoofprints of the Stag: Put a 4/4 white Elemental creature token with flying into play. Play this ability only during your turn.");
 	     a2.setStackDescription(card.getName() + " - put a 4/4 white Elemental creature token with flying into play.");
 	     
 	     card.addSpellAbility(a2);
 	     
    }//*************** END ************ END **************************
   
    
  //*************** START *********** START **************************
    else if (cardName.equals("Goblin Trenches"))
    {
    	final String player = card.getController();
    	
    	final SpellAbility ability = new Ability(card, "2")
        {
    		public boolean canPlay()
    		{
    			PlayerZone play = AllZone.getZone(Constant.Zone.Play, player);
    	    	
    	    	CardList lands = new CardList();
    	    	lands.addAll(play.getCards());
    	    	lands = lands.getType("Land");
    	    	
    			if (lands.size() >= 1 && AllZone.GameAction.isCardInPlay(card))
    				return true;
    			else
    				return false;
    		}
    		
    		public void chooseTargetAI()
            {
              Card c = getTappedLand(); //first, try to get a tapped land to sac
              if (c!=null)
              {
            	  setTargetCard(c);
            	  
              }
              else
              {
            	PlayerZone play = AllZone.getZone(Constant.Zone.Play, player);
      	    	
      	    	CardList lands = new CardList();
      	    	lands.addAll(play.getCards());
      	    	lands = lands.getType("Land");
      	    	
      	    	setTargetCard(c);
            	  
              }
            }
            public Card getTappedLand()
            {
            	//target creature that is going to attack
            	PlayerZone play = AllZone.getZone(Constant.Zone.Play, player);
    	    	
    	    	CardList lands = new CardList();
    	    	lands.addAll(play.getCards());
    	    	lands = lands.getType("Land");
    	    	
    	    	for(int i=0;i<lands.size();i++)
    	    	{
    	    		if (lands.get(i).isTapped())
    	    			return lands.get(i);
    	    	}
    	    	
    	    	return null;
            }//getAttacker()
    		
    		
    		public boolean canPlayAI()
    		{
    		   String phase = AllZone.Phase.getPhase();
         	   return phase.equals(Constant.Phase.Main2);
    		}
    		
    		public void resolve()
    		{

    			Card c = getTargetCard();
    			if (AllZone.GameAction.isCardInPlay(c)){
	    			AllZone.GameAction.sacrifice(c);
	    	    	makeToken();
	    	    	makeToken();
    			}
    	    	
    	    	
    		}//resolve
    		
    		public void makeToken()
    		{
    			Card c = new Card();

    			  c.setName("Goblin Soldier");
    	          c.setImageName("RW 1 1 Goblin Soldier");

    	          c.setOwner(card.getController());
    	          c.setController(card.getController());

    	          c.setManaCost("R W");
    	          c.setToken(true);
    	         
    	          c.addType("Creature");
    	          c.addType("Goblin");
    	          c.addType("Soldier");
    	          c.setBaseAttack(1);
    	          c.setBaseDefense(1);

    	          PlayerZone play = AllZone.getZone(Constant.Zone.Play, card.getController());
    	          play.add(c);
    			
    		}
        };
        
        Input runtime = new Input()
        {
		  private static final long serialVersionUID = -7823269301012427007L;

		  public void showMessage()
          {
        	PlayerZone play = AllZone.getZone(Constant.Zone.Play, player);
  	    	
        	CardList lands = new CardList();
  	    	lands.addAll(play.getCards());
  	    	lands = lands.getType("Land");

            stopSetNext(CardFactoryUtil.input_targetSpecific(ability, lands, "Select a land to sacrifice", false));

          }//showMessage()
        };//Input
        
    	card.addSpellAbility(ability);
    	ability.setDescription("2, Sacrifice a land: Put two 1/1 red and white Goblin Soldier creature tokens into play.");
    	ability.setStackDescription(card.getName() + " - put two 1/1 red and white Goblin Soldier creature tokens into play.");
    	ability.setBeforePayMana(runtime);
    
    }//*************** END ************ END **************************
  
    
  //*************** START *********** START **************************  
  else if(cardName.equals("Hatching Plans"))
  {
	  
	  final Ability ability = new Ability(card, "0")
      {
        public void resolve()
        {
        	AllZone.GameAction.drawCard(card.getController());
        	AllZone.GameAction.drawCard(card.getController());
        	AllZone.GameAction.drawCard(card.getController());        	
        }
      };
      
	  Command draw3Cards = new Command()
      {
		private static final long serialVersionUID = -4919203791300685078L;

		public void execute()
         {
        	 ability.setStackDescription(card.getName()+ " - draw three cards.");
             AllZone.Stack.add(ability);
         }
       };
       
       card.addDestroyCommand(draw3Cards);

    
  }//*************** END ************ END **************************  
    
  
  //*************** START *********** START **************************
  else if(cardName.equals("Sleight of Hand"))
  {
	  final SpellAbility spell = new Spell(card)
      {
		private static final long serialVersionUID = 5608200094037045828L;
		public boolean canPlay()
		{
			PlayerZone library = AllZone.getZone(Constant.Zone.Library, card.getController());
			if (library.size() >= 1 && super.canPlay())
				return true;
			else
				return false;
			
		}
        public void resolve()
        {
        	
        	PlayerZone library = AllZone.getZone(Constant.Zone.Library,card.getController());
        	PlayerZone hand = AllZone.getZone(Constant.Zone.Hand, card.getController());
        	CardList lib = new CardList(library.getCards());
        	
        	CardList topTwo = new CardList();
        	
        	if (lib.size() == 1) {
        		AllZone.GameAction.drawCard(card.getController());
        	}
        	else
        	{
        		if (card.getController().equals(Constant.Player.Human))
        		{
	        		topTwo.add(lib.get(0));
	        		topTwo.add(lib.get(1));
	        		
	        		Object o = AllZone.Display.getChoiceOptional("Select card to put in hand: ", topTwo.toArray());
	        		
	        		Card c1 = (Card)o;
	        		topTwo.remove(c1);
	        		library.remove(c1);
	        		hand.add(c1);
	        		
	        		Card c2 = topTwo.get(0);
	        		library.remove(c2);
	        		library.add(c2);
        		}
        		else //computer
        		{
        			Card c1 = lib.get(0);
        			library.remove(c1);
        			lib.remove(c1);
        			hand.add(c1); 
        			
        			Card c2 = lib.get(0);
        			library.remove(c2);
        			lib.remove(c2);
        			library.add(c2); //put on bottom
        			
        		}
        		        		        		
        	}
        	
        }
      };
      
      card.clearSpellAbility();
      card.addSpellAbility(spell);
      
  }//*************** END ************ END **************************
    
    
  //*************** START *********** START **************************
    else if(cardName.equals("Banishing Knack"))
    {
       SpellAbility spell = new Spell(card)
       {
		private static final long serialVersionUID = 6518824567946786581L;
		public boolean canPlayAI(){return false;}
          public void resolve()
          {
             final Card creature = getTargetCard();
             final Ability_Tap tBanish = new Ability_Tap(creature)
             {
				private static final long serialVersionUID = -1008113001678623984L;

				public boolean canPlayAI() {return false;}

                public void resolve()
                {
                   setStackDescription(creature+" - Return"+getTargetCard()+"to its owner's hand");
                   final Card[] target = new Card[1];
                   target[0] = getTargetCard();
                   PlayerZone hand = AllZone.getZone(Constant.Zone.Hand, target[0].getOwner());

                   if(AllZone.GameAction.isCardInPlay(target[0])  && CardFactoryUtil.canTarget(creature, target[0]) )
                   {
                      AllZone.GameAction.moveTo(hand ,target[0]);
                   }
                }//resolve()
             };//tBanish;
             tBanish.setDescription("T: Return target nonland permanent to its owner's hand.");
             creature.addSpellAbility(tBanish);
             CardList all = new CardList();
             all.addAll(AllZone.Human_Play.getCards());
             all.addAll(AllZone.Computer_Play.getCards());
             all = all.filter(new CardListFilter()
             {
                public boolean addCard(Card c) {
                   return (!c.isLand() && CardFactoryUtil.canTarget(creature, c));
                }
             });
             tBanish.setBeforePayMana(CardFactoryUtil.input_targetSpecific(tBanish, all, "Return target nonland permanent to its owner's hand.", true));
             AllZone.EndOfTurn.addUntil(new Command(){
				private static final long serialVersionUID = -7819140065166374666L;

				public void execute(){
                   creature.removeSpellAbility(tBanish);
                }
             });
          }
       };//SpellAbility
       spell.setBeforePayMana(CardFactoryUtil.input_targetCreature(spell));
       card.clearSpellAbility();
       card.addSpellAbility(spell);
       spell.setDescription("Until end of turn, target creature gains \"T: Return target nonland permanent to its owner's hand.\"");
       spell.setStackDescription("Target creature gains \"T: Return target nonland permanent to its owner's hand.\"");
    }//*************** END ************ END **************************
    
  
   
    
    //*************** START *********** START **************************
    else if(cardName.equals("Dragon Blood"))
    {
       Ability_Tap ability = new Ability_Tap(card, "3")
       {
		private static final long serialVersionUID = -8095802059752537764L;
		public void resolve()
          {
             if(getTargetCard() != null && getTargetCard().isCreature() 
            		 && CardFactoryUtil.canTarget(card, getTargetCard()) )
                getTargetCard().addCounter(Counters.P1P1,1);
          }
          public boolean canPlayAI()
          {
             CardList list = new CardList(AllZone.Computer_Play.getCards());
             list = list.getType("Creature");
             if (list.size() > 0) {
	             setTargetCard(CardFactoryUtil.AI_getBestCreature(list));
	             return (getTargetCard() != null);
             }
             return false;
          }
       };
       ability.setBeforePayMana(CardFactoryUtil.input_targetCreature(ability));
       ability.setDescription("3, T: Put a +1/+1 counter on target creature.");
       ability.setStackDescription(card+": put a +1/+1 counter on target Creature.");
       card.addSpellAbility(ability);
    }//*************** END ************ END **************************
    
    //*************** START *********** START **************************
    else if(cardName.equals("Reminisce"))
    {
      final SpellAbility spell = new Spell(card)
      {
      private static final long serialVersionUID = 505983020365091226L;
      
      public void resolve()
        {
          String player = getTargetPlayer();
          // Move graveyard into library
          PlayerZone grave = AllZone.getZone(Constant.Zone.Graveyard, player);
          PlayerZone library = AllZone.getZone(Constant.Zone.Library, player);
          Card[] g = grave.getCards();
          for (int i = 0; i < g.length; i++)
          {
            grave.remove(g[i]);
            library.add(g[i],0);
          }
          // Shuffle library
          AllZone.GameAction.shuffle(player);;
        }

        public boolean canPlayAI()//97% of the time shuffling your grave into your library is a good thing
        {
           setTargetPlayer(Constant.Player.Computer);
           return true;
        }

      };//SpellAbility
      spell.setBeforePayMana(CardFactoryUtil.input_targetPlayer(spell));
      card.clearSpellAbility();
      card.addSpellAbility(spell);
    }//*************** END ************ END **************************
    
   
    
  //*************** START *********** START **************************
    else if(cardName.equals("AEther Vial"))
    {
    	//final int[] converted = null;
    	final Ability_Tap ability = new Ability_Tap(card, "0")
    	{
			private static final long serialVersionUID = 1854859213307704018L;

			public boolean canPlay()
    		{
    			return card.getCounters(Counters.CHARGE) > 0;
    		}
    	
			public void resolve() {
				String player = card.getController();
				
				PlayerZone hand = AllZone.getZone(Constant.Zone.Hand, player);
				PlayerZone play = AllZone.getZone(Constant.Zone.Play, player);
				
				//converted[0] = card.getCounters(Counters.CHARGE);
				//System.out.println("converted: " + converted[0]);
				
				CardList list = new CardList(hand.getCards());
				list = list.filter(new CardListFilter()
				{
					public boolean addCard(Card c) {
						return CardUtil.getConvertedManaCost(c.getManaCost()) == card.getCounters(Counters.CHARGE) && c.isCreature();					
					}
				});
				
				
				if (list.size()>0)
				{
					if (player.equals(Constant.Player.Human))
					{
						Object o = AllZone.Display.getChoiceOptional("Pick creature to put into play", list.toArray());
						if (o!=null)
						{
							Card c = (Card)o;
							hand.remove(c);
							play.add(c);
						}
					}
					else
					{
						Card c = list.get(0);
						if(AllZone.GameAction.isCardInZone(c, hand)) {
							hand.remove(c);
							play.add(c);
						}
					}
				}
			}
    	};
    	
    	ability.setDescription("Tap: You may put a creature card with converted mana cost equal to the number of charge counters on AEther Vial from your hand into play.");
    	ability.setStackDescription(card.getName() + " - put creature card with converted mana cost equal to the number of charge counters into play.");
    	
    	card.addSpellAbility(ability);
    }//*************** END ************ END **************************
    
  //*************** START *********** START **************************
    else if(cardName.equals("AEther Spellbomb"))
    {
       
      final Ability ability = new Ability(card, "U")
      {
       public boolean canPlay()
         {
            return AllZone.GameAction.isCardInPlay(card)&&!AllZone.Stack.getSourceCards().contains(card);
       }
       public boolean canPlayAI()
        {
    	  CardList humanPlay = new CardList(AllZone.Human_Play.getCards()); 
    	  humanPlay = humanPlay.filter(new CardListFilter()
    	  {
			public boolean addCard(Card c) {
				return c.isCreature() && CardFactoryUtil.canTarget(card, c);
			}  
    	  });
    	  if (humanPlay.size() > 0)
    		  setTargetCard(CardFactoryUtil.AI_getBestCreature(humanPlay));
          return ((AllZone.Computer_Hand.size() > 2)&&(getTargetCard() != null)) ;
        }
       public void resolve()
       {
          final Card[] target = new Card[1];
          target[0] = getTargetCard();
          PlayerZone hand = AllZone.getZone(Constant.Zone.Hand, target[0].getOwner());

          if(AllZone.GameAction.isCardInPlay(target[0])  && CardFactoryUtil.canTarget(card, target[0]) )
          {
        	  if (!target[0].isToken())  
        		  AllZone.GameAction.moveTo(hand ,target[0]);
        	  else
        		  AllZone.getZone(target[0]).remove(target[0]);
          }
          AllZone.GameAction.sacrifice(getSourceCard());
       }//resolve()
      };//SpellAbility
      ability.setDescription("U, Sacrifice AEther Spellbomb: Return target creature to its owner's hand.");
      card.addSpellAbility(ability);
      ability.setBeforePayMana(CardFactoryUtil.input_targetCreature(ability));
    }
  //*************** START *********** START **************************
    else if(cardName.equals("Lifespark Spellbomb"))
    {
    final SpellAbility ability = new Ability_Activated(card, "G")
    {
      private static final long serialVersionUID = -5744842090293912606L;
      public boolean canPlay()
       {
          return AllZone.GameAction.isCardInPlay(card)&&!AllZone.Stack.getSourceCards().contains(card);
         }
      public boolean canPlayAI()
      {
       CardList land = new CardList(AllZone.Computer_Play.getCards());
       land = land.getType("Land");
       CardList basic = land.getType("Basic");
       if (basic.size() < 3) return false;
       Card[] basic_1 = basic.toArray();
       for(Card var : basic_1)
          if (var.isTapped()) basic.remove(var);
        basic.shuffle();
        if (basic.size() == 0)
           return false;
        if (basic.get(0) != null) {
           setTargetCard(basic.get(0));
           return true;
        }
        return false;
      }//canPlayAI() 

     public void resolve()
        {
          //in case ability is played twice
          final int[] oldAttack = new int[1];
          final int[] oldDefense = new int[1];

          final Card card[] = new Card[1];
          card[0] = getTargetCard();

          oldAttack[0]  = card[0].getBaseAttack();
          oldDefense[0] = card[0].getBaseDefense();

          card[0].setBaseAttack(3);
          card[0].setBaseDefense(3);
          card[0].addType("Creature");

          //EOT
          final Command untilEOT = new Command()
          {
            private static final long serialVersionUID = 7236360479349324099L;

         public void execute()
            {
              card[0].setBaseAttack(oldAttack[0]);
              card[0].setBaseDefense(oldDefense[0]);

              card[0].removeType("Creature");
            }
          };

          AllZone.EndOfTurn.addUntil(untilEOT);
          AllZone.GameAction.sacrifice(getSourceCard());
        }//resolve()
      };//SpellAbility
      card.addSpellAbility(ability);
      ability.setDescription("G, Sacrifice Lifespark Spellbomb: Target land becomes a 3/3 Creature until end of turn. It is still a land.");
      ability.setBeforePayMana(CardFactoryUtil.input_targetType(ability, "Land"));
      
    }//*************** END ************ END **************************
   
  //*************** START *********** START **************************
    else if(cardName.equals("Pyrite Spellbomb"))
    {

    final SpellAbility ability = new Ability_Activated(card, "R")
    {   
      private static final long serialVersionUID = 1L;

     public boolean canPlay()
      {
          return AllZone.GameAction.isCardInPlay(card)&&!AllZone.Stack.getSourceCards().contains(card);
      }
      public boolean canPlayAI()
      {
         Random r = new Random();
         if (r.nextFloat() <= Math.pow(.6667, card.getAbilityUsed()))
            return true;
         else
            return false;
      }
     
      public void chooseTargetAI()
      {
        CardList list = CardFactoryUtil.AI_getHumanCreature(2, card, true);
        list.shuffle();

        if(list.isEmpty() || AllZone.Human_Life.getLife() < 5 + 2)
          setTargetPlayer(Constant.Player.Human);
        else
          setTargetCard(list.get(0));
      }//chooseTargetAI
      public void resolve()
      {
        if(getTargetCard() != null)
        {
          if(AllZone.GameAction.isCardInPlay(getTargetCard())  && CardFactoryUtil.canTarget(card, getTargetCard()) )
            getTargetCard().addDamage(2, card);
        }
        else
          AllZone.GameAction.getPlayerLife(getTargetPlayer()).subtractLife(2);
        AllZone.GameAction.sacrifice(getSourceCard());
      }//resolve()
     };//Ability_Activated
     
      ability.setBeforePayMana(CardFactoryUtil.input_targetCreaturePlayer(ability, true));
      ability.setDescription("R, Sacrifice Pyrite Spellbomb: Pyrite Spellbomb deals 2 damage to target creature or player.");
      card.addSpellAbility(ability);
    }//*************** END ************ END **************************
    
  //*************** START *********** START **************************
    else if(cardName.equals("Sunbeam Spellbomb"))
    {
      final Ability ability = new Ability(card, "W")
      {
       public boolean canPlay()
         {
            return AllZone.GameAction.isCardInPlay(card)&&!AllZone.Stack.getSourceCards().contains(card);
       }
       public boolean canPlayAI()
        {
          return (AllZone.GameAction.getPlayerLife(Constant.Player.Computer).getLife() < 7);
        }
        public void resolve()
        {
          AllZone.GameAction.getPlayerLife(card.getController()).addLife(5);
          AllZone.GameAction.sacrifice(getSourceCard());
        }//resolve()
      };//SpellAbility
      ability.setStackDescription("You gain 5 life");
      ability.setDescription("W, Sacrifice Sunbeam Spellbomb: You gain 5 life.");
      card.addSpellAbility(ability);
    } //*************** END ************ END **************************
    
    //*************** START *********** START **************************
    else if(cardName.equals("Necrogen Spellbomb"))
    {
      final Ability ability = new Ability(card, "B")
      {
       public boolean canPlay()
         {
            return AllZone.GameAction.isCardInPlay(card)&&!AllZone.Stack.getSourceCards().contains(card);
       }
       public boolean canPlayAI()
        {
          setTargetPlayer(Constant.Player.Human);
          return (MyRandom.random.nextBoolean()&&AllZone.Human_Hand.size()>0);
        }
        public void resolve()
        {
          String s = getTargetPlayer();
          setStackDescription("Necrogen Spellbomb - " +s +" discards a card");
          if(Constant.Player.Computer.equals(getTargetPlayer()))
            AllZone.GameAction.discardRandom(getTargetPlayer());
          else
            AllZone.InputControl.setInput(CardFactoryUtil.input_discard());
          AllZone.GameAction.sacrifice(getSourceCard());
        }//resolve()
      };//SpellAbility
      ability.setDescription("B, Sacrifice Necrogen Spellbomb: Target player discards a card");
      ability.setBeforePayMana(CardFactoryUtil.input_targetPlayer(ability));
      card.addSpellAbility(ability);
    } //*************** END ************ END **************************
    
  //*************** START *********** START **************************
    else if(cardName.equals("Sensei's Divining Top"))
    {
      //ability2: Draw card, and put divining top on top of library
      final SpellAbility ability2 = new Ability_Tap(card, "0")
      {
		private static final long serialVersionUID = -2523015092351744208L;

		public void resolve()
        {
          String player = card.getController();
          String owner = card.getOwner();
          
          PlayerZone play =  AllZone.getZone(Constant.Zone.Play, player);
          PlayerZone lib =  AllZone.getZone(Constant.Zone.Library, owner);
              
          AllZone.GameAction.drawCard(player);
          play.remove(card);
          lib.add(card,0); //move divining top to top of library
          card.untap();
      
        }

        public boolean canPlayAI()
        {
          return false;
        }

        public boolean canPlay()
        {
        	if (AllZone.getZone(card).is(Constant.Zone.Play))
          	  return true;
            else 
          	  return false;
        }//canPlay()
      };//SpellAbility ability2

      ability2.setBeforePayMana(new Input()
      {
		private static final long serialVersionUID = -4773496833654414458L;
		@SuppressWarnings("unused") // check
		int check = -1;
         public void showMessage()
         {
             AllZone.Stack.push(ability2);
             stop();
         }//showMessage()
      });

      

      //ability (rearrange top 3 cards) :
      final SpellAbility ability1 = new Ability(card, "1")
      {
        public void resolve()
        {
        	String player = card.getController();
        	PlayerZone lib =  AllZone.getZone(Constant.Zone.Library, player);
        	
        	if (lib.size() < 3)
        		return;
        	
        	CardList topThree = new CardList();
        	
        	//show top 3 cards:
        	topThree.add(lib.get(0));
        	topThree.add(lib.get(1));
        	topThree.add(lib.get(2));
        	
        	for (int i=1;i<=3;i++){
        		String Title = "Put on top: ";
        		if (i==2)
        			Title = "Put second from top: ";
        		if (i==3)
        			Title = "Put third from top: ";
        		Object o = AllZone.Display.getChoiceOptional(Title, topThree.toArray());
        		if(o == null)
        			break;
        		Card c_1 = (Card)o;
        		topThree.remove(c_1);
        		lib.remove(c_1);
        		lib.add(c_1,i-1);
        	}
        	
        }
        public boolean canPlayAI()
        {
           return false;
         
        }
        public boolean canPlay()
        {
          if (AllZone.getZone(card).is(Constant.Zone.Play))
        	  return true;
          else 
        	  return false;
        }//canPlay()
      };//SpellAbility ability1


      ability1.setDescription("1: Look at the top three cards of your library, then put them back in any order.");
      ability1.setStackDescription("Sensei's Divining Top - rearrange top 3 cards");
      card.addSpellAbility(ability1);
      ability1.setBeforePayMana(new Input_PayManaCost(ability1));

      ability2.setDescription("tap: Draw a card, then put Sensei's Divining Top on top of its owner's library.");
      ability2.setStackDescription("Sensei's Divining Top - draw a card, then put back on owner's library");
      ability2.setBeforePayMana(new Input_NoCost_TapAbility((Ability_Tap) ability2));
      card.addSpellAbility(ability2);

    }
    //*************** END ************ END **************************
    
  
    
    //*************** START *********** START **************************
    else if(cardName.equals("Vampiric Tutor") || cardName.equals("Cruel Tutor") || cardName.equals("Imperial Seal"))
    {
      SpellAbility spell = new Spell(card)
      {
        private static final long serialVersionUID = 8922434714488681861L;
		public boolean canPlayAI()
        {
        	PlayerLife compLife = AllZone.GameAction.getPlayerLife("Computer");
        	int life = compLife.getLife();
        	if (4 < AllZone.Phase.getTurn() && AllZone.Computer_Library.size() > 0 && life >= 4)
        		return true;
        	else 
        		return false;
        }

        public void resolve()
        {
          String player = card.getController();
          if(player.equals(Constant.Player.Human))
            humanResolve();
          else
            computerResolve();
        }
        public void computerResolve()
        {
        	//TODO: somehow select a good non-creature card for AI
          CardList creature = new CardList(AllZone.Computer_Library.getCards());
          creature = creature.getType("Creature");
          if(creature.size() != 0)
          {
        	Card c = CardFactoryUtil.AI_getBestCreature(creature);
 
         	if(c == null)
            {
         		creature.shuffle();
                c = creature.get(0);
            }
            
            AllZone.GameAction.shuffle(card.getController());

            //move to top of library
            AllZone.Computer_Library.remove(c);
            AllZone.Computer_Library.add(c, 0);
            
            //lose 2 life
            String player = Constant.Player.Computer;
            PlayerLife life = AllZone.GameAction.getPlayerLife(player);
            life.subtractLife(2);
          }
        }//computerResolve()
        public void humanResolve()
        {
          PlayerZone library = AllZone.getZone(Constant.Zone.Library, card.getController());

          CardList list = new CardList(library.getCards());

          if(list.size() != 0)
          {
            Object o = AllZone.Display.getChoiceOptional("Select a card", list.toArray());

            AllZone.GameAction.shuffle(card.getController());
            if(o != null)
            {
              //put card on top of library
              library.remove(o);
              library.add((Card)o, 0);
            }
            //lose 2 life
            String player = Constant.Player.Human;
            PlayerLife life = AllZone.GameAction.getPlayerLife(player);
            life.subtractLife(2);
          }//if
         
          
        }//resolve()
      };
      card.clearSpellAbility();
      card.addSpellAbility(spell);
    }//*************** END ************ END **************************
    
 
  //*************** START *********** START **************************
    else if(cardName.equals("Counterbalance"))
    {
      String player = card.getController();
      final SpellAbility ability = new Ability(card, "0")
      {
        public void resolve()
        {
        	PlayerZone lib =  AllZone.getZone(Constant.Zone.Library, card.getController());
        	
        	Card topCard = lib.get(0);     	
        	
        	SpellAbility sa = AllZone.Stack.peek();
        	
        	
            int convertedManaTopCard = CardUtil.getConvertedManaCost(topCard.getManaCost());
            int convertedManaSpell = CardUtil.getConvertedManaCost(sa.getSourceCard().getManaCost());
            
            CardList showTop = new CardList();
            showTop.add(topCard);
            AllZone.Display.getChoiceOptional("Revealed top card: ", showTop.toArray());
            
            if (convertedManaTopCard == convertedManaSpell)
            {
            	
            	AllZone.Stack.pop();
            	AllZone.GameAction.moveToGraveyard(sa.getSourceCard());
            }
        }
        public boolean canPlayAI()
        {
           return false;
         
        }
        public boolean canPlay()
        {
        	String player = card.getController();
        	PlayerZone lib =  AllZone.getZone(Constant.Zone.Library, player);
        	
        	if(AllZone.Stack.size() == 0 || lib.size() == 0)
                return false;

            //see if spell is on stack and that opponent played it
            String opponent = AllZone.GameAction.getOpponent(card.getController());
            SpellAbility sa = AllZone.Stack.peek();
             
            if (AllZone.getZone(card).is(Constant.Zone.Play) && sa.isSpell() && opponent.equals(sa.getSourceCard().getController())
            	&& CardFactoryUtil.isCounterable(sa.getSourceCard()))
              return true;
            else 
              return false;
        }//canPlay()
      };//SpellAbility ability
      
      ability.setStackDescription("Counterbalance - "+player +" reveals top card and counters spell if it has the same converted manacost");
      ability.setBeforePayMana(new Input_PayManaCost(ability));
      card.addSpellAbility(ability);
      

    }
    //*************** END ************ END **************************
    
    //*************** START *********** START **************************
    else if(cardName.equals("Aluren"))
    {
      final Ability ability1 = new Ability(card, "0")
      {
        public void resolve()
        {
        	
        	//String player = card.getController();
        	PlayerZone hand =  AllZone.getZone(Constant.Zone.Hand, Constant.Player.Human);
        	PlayerZone play =  AllZone.getZone(Constant.Zone.Play, Constant.Player.Human);
        	
        	if (hand.size() == 0)
        		return;
        	
        	CardList creatures = new CardList();
        	
        	for (int i=0;i < hand.size(); i++)
        	{
        		if (hand.get(i).getType().contains("Creature") && CardUtil.getConvertedManaCost(hand.get(i).getManaCost()) <= 3)
        			creatures.add(hand.get(i));
        	}
        	
        	if(creatures.size() == 0)
                return;

            
            Object o = AllZone.Display.getChoiceOptional("Select target creature to play", creatures.toArray());
            if(o != null)
            {
            	Card c = (Card)o;
            	hand.remove(c);
            	play.add(c);
            	c.setSickness(true);
            }
           
        	
        	
        }
        public boolean canPlayAI()
        {
           return false;
         
        }
        public boolean canPlay()
        {
          if (AllZone.getZone(card).is(Constant.Zone.Play))
        	  return true;
          else 
        	  return false;
        }//canPlay()
      };//SpellAbility ability1


      ability1.setDescription("Any player may play creature cards with converted mana cost 3 or less without paying their mana cost any time he or she could play an instant.");
      ability1.setStackDescription("Aluren - Play creature with converted manacost 3 or less for free.");
      card.addSpellAbility(ability1);
      ability1.setBeforePayMana(new Input_PayManaCost(ability1));      

    }
    //*************** END ************ END **************************
    
 
  //*************** START *********** START **************************
    else if(cardName.equals("Intuition"))
    {
      final SpellAbility spell = new Spell(card)
      {
		private static final long serialVersionUID = 8282597086298330698L;
		
		public void resolve()
        {
          String player = card.getController();
          if(player.equals(Constant.Player.Human))
            humanResolve();
          else
            computerResolve();
        }
        public void humanResolve()
        {
          CardList libraryList = new CardList(AllZone.Human_Library.getCards());
          PlayerZone library = AllZone.getZone(Constant.Zone.Library, card.getController());
          CardList selectedCards = new CardList();
        	
          Object o = AllZone.Display.getChoiceOptional("Select first card", libraryList.toArray());
          if(o != null)
          {
        	Card c1 = (Card)o;
        	libraryList.remove(c1);
        	selectedCards.add(c1);
          }
          else {
        	  return;
          }
          o = AllZone.Display.getChoiceOptional("Select second card", libraryList.toArray());
          if(o != null)
          {
        	Card c2 = (Card)o;
        	libraryList.remove(c2);
        	selectedCards.add(c2);
          }
          else {
        	  return;
          }
          o = AllZone.Display.getChoiceOptional("Select third card", libraryList.toArray());
          if(o != null)
          {
        	Card c3 = (Card)o;
        	libraryList.remove(c3);
        	selectedCards.add(c3);
          }
          else {
        	  return;
          }
          
          Card choice = selectedCards.get(MyRandom.random.nextInt(2)); //comp randomly selects one of the three cards
                    
          PlayerZone hand = AllZone.getZone(Constant.Zone.Hand, card.getController());
          PlayerZone grave = AllZone.getZone(Constant.Zone.Graveyard, card.getController());
          library.remove(choice);
          hand.add(choice);
          
          selectedCards.remove(choice);
          Card toGrave1 = selectedCards.get(0);
          Card toGrave2 = selectedCards.get(1);
          library.remove(toGrave1);
          library.remove(toGrave2);
          selectedCards.remove(toGrave2);
          selectedCards.remove(toGrave2);
          
          grave.add(toGrave1);
          grave.add(toGrave2);
          
          AllZone.GameAction.shuffle(Constant.Player.Human);
        }
        public void computerResolve()
        {
          Card[] library = AllZone.Computer_Library.getCards();
          CardList list = new CardList(library);
          CardList selectedCards = new CardList();

          //pick best creature
          Card c = CardFactoryUtil.AI_getBestCreature(list);
          if(c == null) {
            c = library[0];
          }
          list.remove(c);
          selectedCards.add(c);
          
          c = CardFactoryUtil.AI_getBestCreature(list);
          if(c == null) {
            c = library[0];
          }
          list.remove(c);
          selectedCards.add(c);
          
          c = CardFactoryUtil.AI_getBestCreature(list);
          if(c == null) {
            c = library[0];
          }
          list.remove(c);
          selectedCards.add(c);
          
          Object o = AllZone.Display.getChoiceOptional("Select card to give to computer", selectedCards.toArray());
          
          Card choice = (Card)o;
          
          selectedCards.remove(choice);
          AllZone.Computer_Library.remove(choice);
          AllZone.Computer_Hand.add(choice);
          
          AllZone.Computer_Library.remove(selectedCards.get(0));
          AllZone.Computer_Library.remove(selectedCards.get(1));   
          
          AllZone.Computer_Graveyard.add(selectedCards.get(0));
          AllZone.Computer_Graveyard.add(selectedCards.get(1));
          
        }
        public boolean canPlay()
        {
          PlayerZone library = AllZone.getZone(Constant.Zone.Library, card.getController());
          return library.getCards().length >= 3;
        }
        public boolean canPlayAI()
        {
          CardList creature = new CardList();
          creature.addAll(AllZone.Computer_Library.getCards());
          creature = creature.getType("Creature");
          return creature.size() != 0;
        }
      };//SpellAbility
      card.clearSpellAbility();
      card.addSpellAbility(spell);
    }//*************** END ************ END **************************
    
   
   
  //*************** START *********** START **************************
    else if(cardName.equals("Invincible Hymn"))
    {	
    	final String player = card.getController();

    	final SpellAbility spell = new Spell(card)
        {
		  private static final long serialVersionUID = -827136493013927725L;
		  
		  public void resolve()
          {
        	  PlayerZone library = AllZone.getZone(Constant.Zone.Library, card.getController());
        	  CardList libCards = new CardList(library.getCards());
          	  int lifeGain = libCards.size();	    	   	
          	  
          	  System.out.println("lifeGain: " + lifeGain);
          	
        	  PlayerLife life = AllZone.GameAction.getPlayerLife(player);
        	  life.setLife(lifeGain);
        	  
        	  System.out.println("life.getLife(): " + life.getLife());
          }
          public boolean canPlayAI()
          {
        	  PlayerZone library = AllZone.getZone(Constant.Zone.Library, card.getController());
        	  CardList libCards = new CardList(library.getCards());
          	  int lifeGain = libCards.size();	    	   	
          	  
        	  PlayerLife compLife = AllZone.GameAction.getPlayerLife(Constant.Player.Computer);
        	  if (lifeGain > compLife.getLife())
        		  return true;
        	  else
        		  return false;
          }
        };//spell
        card.clearSpellAbility();
        card.addSpellAbility(spell);
    }//*************** END ************ END **************************
    
    
  
    //*************** START *********** START **************************
    else if(cardName.equals("Gift of Estates"))
    {
      SpellAbility spell = new Spell(card)
      {
		private static final long serialVersionUID = -4997834790204261916L;

		public boolean canPlay()
        {
          String oppPlayer = AllZone.GameAction.getOpponent(card.getController());

          PlayerZone selfZone = AllZone.getZone(Constant.Zone.Play, card.getController());
          PlayerZone oppZone = AllZone.getZone(Constant.Zone.Play, oppPlayer);

          CardList self = new CardList(selfZone.getCards());
          CardList opp = new CardList(oppZone.getCards());

          self = self.getType("Land");
          opp = opp.getType("Land");

          return (self.size() < opp.size()) && super.canPlay();
        }//canPlay()

        public void resolve()
        {
          PlayerZone library = AllZone.getZone(Constant.Zone.Library, card.getController());
          PlayerZone hand = AllZone.getZone(Constant.Zone.Hand, card.getController());

          CardList plains = new CardList(library.getCards());
          plains = plains.getType("Plains");

          for(int i = 0; i < 3 && i < plains.size(); i++)
            AllZone.GameAction.moveTo(hand, plains.get(i));
        }//resolve()
      };//SpellAbility
      card.clearSpellAbility();
      card.addSpellAbility(spell);
    }//*************** END ************ END **************************




    //*************** START *********** START **************************
    else if(cardName.equals("Tithe"))
    {
      SpellAbility spell = new Spell(card)
      {
		private static final long serialVersionUID = 1504792204526793942L;

		public boolean oppMoreLand()
        {
          String oppPlayer = AllZone.GameAction.getOpponent(card.getController());

          PlayerZone selfZone = AllZone.getZone(Constant.Zone.Play, card.getController());
          PlayerZone oppZone = AllZone.getZone(Constant.Zone.Play, oppPlayer);

          CardList self = new CardList(selfZone.getCards());
          CardList opp = new CardList(oppZone.getCards());

          self = self.getType("Land");
          opp = opp.getType("Land");

          return (self.size() < opp.size()) && super.canPlay();
        }//oppoMoreLand()

        public void resolve()
        {
          PlayerZone library = AllZone.getZone(Constant.Zone.Library, card.getController());
          PlayerZone hand = AllZone.getZone(Constant.Zone.Hand, card.getController());

          CardList plains = new CardList(library.getCards());
          plains = plains.getType("Plains");

          if(0 < plains.size())
            AllZone.GameAction.moveTo(hand, plains.get(0));

          if(oppMoreLand() && 1 < plains.size())
            AllZone.GameAction.moveTo(hand, plains.get(1));

        }//resolve()
      };//SpellAbility
      card.clearSpellAbility();
      card.addSpellAbility(spell);
    }//*************** END ************ END **************************
    
   
  //*************** START *********** START **************************
    else if(cardName.equals("Survival of the Fittest"))
    {
      SpellAbility ability = new Ability(card, "G")
      {
        public void resolve()
        {
        	String player = card.getController();
            if(player.equals(Constant.Player.Human))
              humanResolve();
            else
              computerResolve();
        }//resolve()
        public void humanResolve()
        {
          CardList handCreatures = new CardList(AllZone.Human_Hand.getCards());
          handCreatures = handCreatures.getType("Creature");
          
          if (handCreatures.size() == 0)
        	  return;
          
          Object discard = AllZone.Display.getChoiceOptional("Select Creature to discard", handCreatures.toArray());
          if (discard != null){
          
        	  CardList creatures = new CardList(AllZone.Human_Library.getCards());
        	  creatures = creatures.getType("Creature");
        	  
         	if (creatures.size() != 0) {          
         		Object check = AllZone.Display.getChoiceOptional("Select Creature", creatures.toArray());
         		if(check != null)
         		{
         			PlayerZone hand = AllZone.getZone(Constant.Zone.Hand, card.getController());
         			AllZone.GameAction.moveTo(hand, (Card)check);
         		}
         		AllZone.GameAction.shuffle(Constant.Player.Human);
         	}
          AllZone.GameAction.discard((Card)discard);
          }
        }
        public void computerResolve()
        {
          //TODO
        }
        
        public boolean canPlayAI() { return false; }
        
      };//SpellAbility
      
      //card.clearSpellAbility();
      ability.setDescription("G: Discard a creature card: Search your library for a creature card, reveal that card, and put it into your hand. Then shuffle your library.");
      ability.setStackDescription("Survival of the Fittest - search for a creature card and put into hand");
      card.addSpellAbility(ability);
    }//*************** END ************ END **************************

    
  //*************** START *********** START **************************
    else if(cardName.equals("Gerrard's Command"))
    {
      SpellAbility spell = new Spell(card)
      {
		private static final long serialVersionUID = 3128602006949603902L;
		
		public boolean canPlayAI()
        {
          return getAttacker() != null;
        }
        public void chooseTargetAI()
        {
          setTargetCard(getAttacker());
        }
        public Card getAttacker()
        {
        	//target creature that is going to attack
          Combat c = ComputerUtil.getAttackers();
          Card[] att = c.getAttackers();
          if(att.length != 0)
            return att[0];
          else
            return null;
        }//getAttacker()
        public void resolve()
        {
          final Card[] target = new Card[1];
          final Command untilEOT = new Command()
          {
			private static final long serialVersionUID = -650846106294962607L;

			public void execute()
            {
              if(AllZone.GameAction.isCardInPlay(target[0]))
              {
                target[0].addTempAttackBoost(-3);
                target[0].addTempDefenseBoost(-3);
              }
            }
          };

          target[0] = getTargetCard();
          if(AllZone.GameAction.isCardInPlay(target[0]) && CardFactoryUtil.canTarget(card, target[0]))
          {
            target[0].addTempAttackBoost(3);
            target[0].addTempDefenseBoost(3);
            target[0].untap();
                        
            AllZone.EndOfTurn.addUntil(untilEOT);
          }
        }//resolve()
      };
      spell.setBeforePayMana(CardFactoryUtil.input_targetCreature(spell));
      card.clearSpellAbility();
      card.addSpellAbility(spell);
    }//*************** END ************ END **************************


/*
    //*************** START *********** START **************************
    else if(cardName.equals("Brute Force"))
    {
      SpellAbility spell = new Spell(card)
      {
		private static final long serialVersionUID = 4733538427752827505L;

		public boolean canPlayAI()
        {
          return getAttacker() != null;
        }
        public void chooseTargetAI()
        {
          setTargetCard(getAttacker());
        }
        public Card getAttacker()
        {
        	//target creature that is going to attack
          Combat c = ComputerUtil.getAttackers();
          //Card[] att = c.getAttackers();
          
          CardList list = new CardList();
          list.addAll(c.getAttackers());
          list = list.filter(new CardListFilter()
          {

			public boolean addCard(Card c) {
				return CardFactoryUtil.canTarget(card, c);
			}
        	  
          });
          
          if(list.size() != 0)
            return list.get(0);
          else
            return null;
        }//getAttacker()
        public void resolve()
        {
          final Card[] target = new Card[1];
          final Command untilEOT = new Command()
          {
			private static final long serialVersionUID = 8299648917436556370L;

			public void execute()
            {
              if(AllZone.GameAction.isCardInPlay(target[0]))
              {
                target[0].addTempAttackBoost(-3);
                target[0].addTempDefenseBoost(-3);
              }
            }
          };

          target[0] = getTargetCard();
          if(AllZone.GameAction.isCardInPlay(target[0]) && CardFactoryUtil.canTarget(card, target[0]) )
          {
            target[0].addTempAttackBoost(3);
            target[0].addTempDefenseBoost(3);

            AllZone.EndOfTurn.addUntil(untilEOT);
          }
        }//resolve()
      };
      spell.setBeforePayMana(CardFactoryUtil.input_targetCreature(spell));
      card.clearSpellAbility();
      card.addSpellAbility(spell);
    }//*************** END ************ END **************************
*/


    //*************** START *********** START **************************
    else if(cardName.equals("Nameless Inversion"))
    {
      SpellAbility spell = new Spell(card)
      {
		private static final long serialVersionUID = 5479536291205544905L;

		public boolean canPlayAI()
        {
          CardList list = CardFactoryUtil.AI_getHumanCreature(3, card, true);
          CardListUtil.sortFlying(list);

          for(int i = 0; i < list.size(); i++)
            if(2 <= list.get(i).getNetAttack())
            {
              setTargetCard(list.get(i));
              return true;
            }
          return false;
        }//canPlayAI()

        public void resolve()
        {
          final Card[] target = new Card[1];
          final Command untilEOT = new Command()
          {
			private static final long serialVersionUID = -1954104042512587145L;

			public void execute()
            {
              if(AllZone.GameAction.isCardInPlay(target[0]))
              {
                target[0].addTempAttackBoost(-3);
                target[0].addTempDefenseBoost(3);
              }
            }
          };

          target[0] = getTargetCard();
          if(AllZone.GameAction.isCardInPlay(target[0]) && CardFactoryUtil.canTarget(card, target[0]))
          {
            target[0].addTempAttackBoost(3);
            target[0].addTempDefenseBoost(-3);

            AllZone.EndOfTurn.addUntil(untilEOT);
          }
        }//resolve()
      };
      spell.setBeforePayMana(CardFactoryUtil.input_targetCreature(spell));
      card.clearSpellAbility();
      card.addSpellAbility(spell);
    }//*************** END ************ END **************************


    //*************** START *********** START **************************
    else if(cardName.equals("Tromp the Domains"))
    {
      SpellAbility spell = new Spell(card)
      {
		private static final long serialVersionUID = 1523193367625798058L;
		
		public boolean canPlayAI()
        {
          return getAttacker() != null;
        }
        public Card getAttacker()
        {
          //target creature that is going to attack
          Combat c = ComputerUtil.getAttackers();
          Card[] att = c.getAttackers();
          if(att.length != 0)
            return att[0];
          else
            return null;
        }//getAttacker()

        int countLandTypes()
        {
          PlayerZone play = AllZone.getZone(Constant.Zone.Play, card.getController());
          CardList land = new CardList(play.getCards());

          String basic[] = {"Forest", "Plains", "Mountain", "Island", "Swamp"};
          int count = 0;

          for(int i = 0; i < basic.length; i++)
          {
            CardList c = land.getType(basic[i]);
            if(! c.isEmpty())
              count++;
          }

          return count;
        }//countLandTypes()
        public void resolve()
        {

          final int boost = countLandTypes();
          PlayerZone play = AllZone.getZone(Constant.Zone.Play, card.getController());
          CardList list = new CardList(play.getCards());
          @SuppressWarnings("unused") // c
		  Card c;

          for(int i = 0; i < list.size(); i++)
          {
            final Card[] target = new Card[1];
            target[0] = list.get(i);

            final Command untilEOT = new Command()
            {
			  private static final long serialVersionUID = -4207130279969069542L;

			  public void execute()
              {
                if(AllZone.GameAction.isCardInPlay(target[0]))
                {
                  target[0].addTempAttackBoost(-boost);
                  target[0].addTempDefenseBoost(-boost);

                  target[0].removeExtrinsicKeyword("Trample");
                }
              }
            };//Command

            if(AllZone.GameAction.isCardInPlay(target[0]))
            {
              target[0].addTempAttackBoost(boost);
              target[0].addTempDefenseBoost(boost);

              target[0].addExtrinsicKeyword("Trample");

              AllZone.EndOfTurn.addUntil(untilEOT);
            }//if
          }//for
        }//resolve()
      };
      card.clearSpellAbility();
      card.addSpellAbility(spell);
    }//*************** END ************ END **************************
    
  //*************** START *********** START **************************
    else if(cardName.equals("Titanic Ultimatum"))
    {
      SpellAbility spell = new Spell(card)
      {
		private static final long serialVersionUID = 4920407567000133514L;

		public boolean canPlayAI()
        {
          return getAttacker() != null;
        }
        public Card getAttacker()
        {
          //target creature that is going to attack
          Combat c = ComputerUtil.getAttackers();
          Card[] att = c.getAttackers();
          if(att.length != 0)
            return att[0];
          else
            return null;
        }//getAttacker()

        public void resolve()
        {
          PlayerZone play = AllZone.getZone(Constant.Zone.Play, card.getController());
          CardList list = new CardList(play.getCards());
          @SuppressWarnings("unused") // c
		  Card c;

          for(int i = 0; i < list.size(); i++)
          {
            final Card[] target = new Card[1];
            target[0] = list.get(i);

            final Command untilEOT = new Command()
            {
			  private static final long serialVersionUID = -3807842003906681893L;

			  public void execute()
              {
                if(AllZone.GameAction.isCardInPlay(target[0]))
                {
                  target[0].addTempAttackBoost(-5);
                  target[0].addTempDefenseBoost(-5);

                  target[0].removeExtrinsicKeyword("Trample");
                  target[0].removeExtrinsicKeyword("First Strike");
                  target[0].removeExtrinsicKeyword("Lifelink");
                }
              }
            };//Command

            if(AllZone.GameAction.isCardInPlay(target[0]))
            {
              target[0].addTempAttackBoost(5);
              target[0].addTempDefenseBoost(5);

              target[0].addExtrinsicKeyword("Trample");
              target[0].addExtrinsicKeyword("First Strike");
              target[0].addExtrinsicKeyword("Lifelink");

              AllZone.EndOfTurn.addUntil(untilEOT);
            }//if
          }//for
        }//resolve()
      };
      card.clearSpellAbility();
      card.addSpellAbility(spell);
    }//*************** END ************ END **************************



    //*************** START *********** START **************************
    else if(cardName.equals("Primal Boost"))
    {
      SpellAbility spell = new Spell(card)
      {
		private static final long serialVersionUID = 2449600319884238808L;
		
		public boolean canPlayAI()
        {
          return getAttacker() != null;
        }
        public void chooseTargetAI()
        {
          setTargetCard(getAttacker());
        }
        public Card getAttacker()
        {
          //target creature that is going to attack
          Combat c = ComputerUtil.getAttackers();

          CardList list = new CardList(c.getAttackers());
          CardListUtil.sortFlying(list);

          Card[] att = list.toArray();
          if(att.length != 0)
            return att[0];
          else
            return null;
        }//getAttacker()
        public void resolve()
        {
          final Card[] target = new Card[1];
          final Command untilEOT = new Command()
          {
			private static final long serialVersionUID = 3753684523153747308L;

			public void execute()
            {
              if(AllZone.GameAction.isCardInPlay(target[0]))
              {
                target[0].addTempAttackBoost(-4);
                target[0].addTempDefenseBoost(-4);
              }
            }
          };

          target[0] = getTargetCard();
          if(AllZone.GameAction.isCardInPlay(target[0]) && CardFactoryUtil.canTarget(card, target[0]))
          {
            target[0].addTempAttackBoost(4);
            target[0].addTempDefenseBoost(4);

            AllZone.EndOfTurn.addUntil(untilEOT);
          }
        }//resolve()
      };
      spell.setDescription("\r\nTarget creature gets +4/+4 until end of turn.");
      spell.setBeforePayMana(CardFactoryUtil.input_targetCreature(spell));
      card.clearSpellAbility();
      card.addSpellAbility(spell);
      //card.addSpellAbility(CardFactoryUtil.ability_cycle(card, "2 G"));
    }//*************** END ************ END **************************
    
    
/*
    //*************** START *********** START **************************
    else if(cardName.equals("Wildsize"))
    {
      SpellAbility spell = new Spell(card)
      {
		private static final long serialVersionUID = -4558777579924787035L;

		public boolean canPlayAI()
        {
          return getAttacker() != null;
        }
        public void chooseTargetAI()
        {
          setTargetCard(getAttacker());
        }
        public Card getAttacker()
        {
          //target creature that is going to attack
          Combat c = ComputerUtil.getAttackers();

          CardList list = new CardList(c.getAttackers());
          CardListUtil.sortFlying(list);

          Card[] att = list.toArray();
          if(att.length != 0)
            return att[0];
          else
            return null;
        }//getAttacker()

        public void resolve()
        {
          final Card[] target = new Card[1];
          final Command untilEOT = new Command()
          {
			private static final long serialVersionUID = -8390763209393328399L;

			public void execute()
            {
              if(AllZone.GameAction.isCardInPlay(target[0]))
              {
                target[0].addTempAttackBoost(-2);
                target[0].addTempDefenseBoost(-2);

                target[0].removeExtrinsicKeyword("Trample");
              }
            }
          };

          target[0] = getTargetCard();
          if(AllZone.GameAction.isCardInPlay(target[0]) && CardFactoryUtil.canTarget(card, target[0]))
          {
            target[0].addTempAttackBoost(2);
            target[0].addTempDefenseBoost(2);
            target[0].addExtrinsicKeyword("Trample");

            AllZone.EndOfTurn.addUntil(untilEOT);
            AllZone.GameAction.drawCard(card.getController());
          }
        }//resolve()
      };
      spell.setBeforePayMana(CardFactoryUtil.input_targetCreature(spell));
      card.clearSpellAbility();
      card.addSpellAbility(spell);
    }//*************** END ************ END **************************
*/
    
    
    //*************** START *********** START **************************
    else if(cardName.equals("Feral Lightning"))
    {
      SpellAbility spell = new Spell(card)
      {
		private static final long serialVersionUID = -1841642966580694848L;
		
		public boolean canPlayAI()
        {
          return AllZone.Phase.getPhase().equals(Constant.Phase.Main1);
        }


        public void resolve()
        {
          final Card[] token = new Card[3];
          final Command atEOT = new Command()
          {
			private static final long serialVersionUID = -1928884889370422828L;

			public void execute()
            {
              //destroy tokens at end of turn
              for(int i = 0; i < token.length; i++)
                if(AllZone.GameAction.isCardInPlay(token[i]))
                  AllZone.GameAction.destroy(token[i]);
            }
          };
          AllZone.EndOfTurn.addAt(atEOT);

          for(int i = 0; i < token.length; i++)
            token[i] = makeToken();
        }//resolve()
        Card makeToken()
        {
          Card c = new Card();

          c.setOwner(card.getController());
          c.setController(card.getController());

          c.setName("Elemental");
          c.setImageName("R 3 1 Elemental");
          c.setManaCost("R");
          c.setToken(true);

          c.addType("Creature");
          c.addType("Elemental");
          c.setBaseAttack(3);
          c.setBaseDefense(1);
          c.addIntrinsicKeyword("Haste");
          c.setSacrificeAtEOT(true);

          PlayerZone play = AllZone.getZone(Constant.Zone.Play, card.getController());
          play.add(c);

          return c;
        }//makeToken()
      };//SpellAbility
      card.clearSpellAbility();
      card.addSpellAbility(spell);
    }//*************** END ************ END **************************

    //*************** START *********** START **************************
    else if(cardName.equals("Inspirit"))
    {
      SpellAbility spell = new Spell(card)
      {
		private static final long serialVersionUID = -601100008975177639L;
		
		public boolean canPlayAI()
        {
          return getAttacker() != null;
        }
        public void chooseTargetAI()
        {
          setTargetCard(getAttacker());
        }
        public Card getAttacker()
        {
          //target creature that is going to attack
          Combat c = ComputerUtil.getAttackers();
          Card[] att = c.getAttackers();
          if(att.length != 0)
            return att[0];
          else
            return null;
        }//getAttacker()
        public void resolve()
        {
          final Card[] target = new Card[1];
          final Command untilEOT = new Command()
          {
			private static final long serialVersionUID = -3197321199337917886L;

			public void execute()
            {
              if(AllZone.GameAction.isCardInPlay(target[0]))
              {
                target[0].addTempAttackBoost(-2);
                target[0].addTempDefenseBoost(-4);
              }
            }
          };

          target[0] = getTargetCard();
          if(AllZone.GameAction.isCardInPlay(target[0]) && CardFactoryUtil.canTarget(card, target[0]))
          {
            target[0].addTempAttackBoost(2);
            target[0].addTempDefenseBoost(4);

            target[0].untap();

            AllZone.EndOfTurn.addUntil(untilEOT);
          }
        }//resolve()
      };
      spell.setBeforePayMana(CardFactoryUtil.input_targetCreature(spell));
      card.clearSpellAbility();
      card.addSpellAbility(spell);
    }//*************** END ************ END **************************


/*
    //*************** START *********** START **************************
    else if(cardName.equals("Might of Oaks"))
    {
      SpellAbility spell = new Spell(card)
      {
		private static final long serialVersionUID = -33985340162641452L;
		
		public boolean canPlayAI()
        {
          return getAttacker() != null;
        }
        public void chooseTargetAI()
        {
          setTargetCard(getAttacker());
        }
        public Card getAttacker()
        {
          //target creature that is going to attack
          Combat c = ComputerUtil.getAttackers();
          Card[] att = c.getAttackers();
          if(att.length != 0)
            return att[0];
          else
            return null;
        }//getAttacker()
        public void resolve()
        {
          final Card[] target = new Card[1];
          final Command untilEOT = new Command()
          {
			private static final long serialVersionUID = -5138969487216215323L;

			public void execute()
            {
              if(AllZone.GameAction.isCardInPlay(target[0]))
              {
                target[0].addTempAttackBoost(-7);
                target[0].addTempDefenseBoost(-7);
              }
            }
          };

          target[0] = getTargetCard();
          if(AllZone.GameAction.isCardInPlay(target[0]) && CardFactoryUtil.canTarget(card, getTargetCard()))
          {
            target[0].addTempAttackBoost(7);
            target[0].addTempDefenseBoost(7);

            AllZone.EndOfTurn.addUntil(untilEOT);
          }
        }//resolve()
      };
      spell.setBeforePayMana(CardFactoryUtil.input_targetCreature(spell));
      card.clearSpellAbility();
      card.addSpellAbility(spell);
    }//*************** END ************ END **************************
*/
    
/*
    //*************** START *********** START **************************
    else if(cardName.equals("Guided Strike"))
    {
      final Card[] target = new Card[1];
      final Command untilEOT = new Command()
      {
		private static final long serialVersionUID = 6381010042083054770L;

		public void execute()
        {
          if(AllZone.GameAction.isCardInPlay(target[0]) && CardFactoryUtil.canTarget(card, target[0]))
          {
            target[0].addTempAttackBoost(-1);
            target[0].removeExtrinsicKeyword("First Strike");
          }
        }
      };

      SpellAbility spell = new Spell(card)
      {
		private static final long serialVersionUID = 6373328936243472966L;
		
		public boolean canPlayAI()
        {
          return getAttacker() != null;
        }
        public void chooseTargetAI()
        {
          setTargetCard(getAttacker());
        }
        public Card getAttacker()
        {
          //target creature that is going to attack
          Combat c = ComputerUtil.getAttackers();
          Card[] att = c.getAttackers();
          if(att.length != 0)
            return att[0];
          else
            return null;
        }//getAttacker()
        public void resolve()
        {
          target[0] = getTargetCard();
          if(AllZone.GameAction.isCardInPlay(target[0]))
          {
            target[0].addTempAttackBoost(1);
            target[0].addExtrinsicKeyword("First Strike");

            AllZone.EndOfTurn.addUntil(untilEOT);
            AllZone.GameAction.drawCard(card.getController());
          }
        }//resolve()
      };
      spell.setBeforePayMana(CardFactoryUtil.input_targetCreature(spell));
      card.clearSpellAbility();
      card.addSpellAbility(spell);
    }//*************** END ************ END **************************
*/
    
/*
    //*************** START *********** START **************************
    else if(cardName.equals("Aggressive Urge"))
    {
      final Card[] target = new Card[1];
      final Command untilEOT = new Command()
      {
		private static final long serialVersionUID = 5519847437187468154L;

		public void execute()
        {
          if(AllZone.GameAction.isCardInPlay(target[0]))
          {
            target[0].addTempAttackBoost(-1);
            target[0].addTempDefenseBoost(-1);
          }
        }
      };

      SpellAbility spell = new Spell(card)
      {
		private static final long serialVersionUID = -1043326570050661433L;
		
		public boolean canPlayAI()
        {
          return getAttacker() != null;
        }
        public void chooseTargetAI()
        {
          setTargetCard(getAttacker());
        }
        public Card getAttacker()
        {
          //target creature that is going to attack
          Combat c = ComputerUtil.getAttackers();
          Card[] att = c.getAttackers();
          if(att.length != 0)
            return att[0];
          else
            return null;
        }//getAttacker()
        public void resolve()
        {
          target[0] = getTargetCard();
          if(AllZone.GameAction.isCardInPlay(target[0]) && CardFactoryUtil.canTarget(card, target[0]))
          {
            target[0].addTempAttackBoost(1);
            target[0].addTempDefenseBoost(1);

            AllZone.EndOfTurn.addUntil(untilEOT);
            AllZone.GameAction.drawCard(card.getController());
          }
        }//resolve()
      };
      spell.setBeforePayMana(CardFactoryUtil.input_targetCreature(spell));
      card.clearSpellAbility();
      card.addSpellAbility(spell);
    }//*************** END ************ END **************************
*/
    
/*
    //*************** START *********** START **************************
    else if(cardName.equals("Vindicate"))
    {
      final SpellAbility spell = new Spell(card)
      {
		private static final long serialVersionUID = -2302610807061478203L;

		public boolean canPlayAI()
    	{
    	  CardList human = CardFactoryUtil.AI_getHumanCreature(card, true);
    	  return 4 < AllZone.Phase.getTurn() && 0 < human.size();
    	}
    	public void chooseTargetAI()
    	{
    	  CardList human = CardFactoryUtil.AI_getHumanCreature(card, true);
    	  setTargetCard(CardFactoryUtil.AI_getBestCreature(human));
    	}

    	public void resolve()
    	{
    	  if(AllZone.GameAction.isCardInPlay(getTargetCard())  && CardFactoryUtil.canTarget(card, getTargetCard()))
    	  {
    		if(getTargetCard().isToken())
    		  AllZone.getZone(getTargetCard()).remove(getTargetCard());
    		else
    		{
    		  AllZone.GameAction.destroy(getTargetCard());
    		}
    	  }//if
    	}//resolve()
      };//SpellAbility
      Input target = new Input()
      {
		private static final long serialVersionUID = -7826307781920832218L;
		
		public void showMessage()
    	{
    	  AllZone.Display.showMessage("Select target permanent for " +spell.getSourceCard());
    	  ButtonUtil.enableOnlyCancel();
    	}
    	public void selectButtonCancel() {stop();}
    	public void selectCard(Card card, PlayerZone zone)
    	{
    	  if(zone.is(Constant.Zone.Play))
    	  {
    		spell.setTargetCard(card);
    		stopSetNext(new Input_PayManaCost(spell));
    	  }
    	}
      };//Input

      spell.setBeforePayMana(target);
      card.clearSpellAbility();
      card.addSpellAbility(spell);
    }//*************** END ************ END **************************
*/
    

    //*************** START *********** START **************************
    else if(cardName.equals("Animate Land"))
    {
      final Card[] target = new Card[1];
      final Command untilEOT = new Command()
      {
		private static final long serialVersionUID = -3359299797188942353L;

		public void execute()
        {
          if(AllZone.GameAction.isCardInPlay(target[0]))
          {
            target[0].removeType("Creature");
          }
        }
      };

      SpellAbility spell = new Spell(card)
      {
		private static final long serialVersionUID = 4890851927124377327L;
		
		public void resolve()
        {
          target[0] = getTargetCard();
          if(AllZone.GameAction.isCardInPlay(target[0])  && CardFactoryUtil.canTarget(card, target[0]))
          {
            target[0].addType("Creature");
            target[0].setBaseAttack(3);
            target[0].setBaseDefense(3);

            AllZone.EndOfTurn.addUntil(untilEOT);
          }
        }//resolve()
        public boolean canPlayAI()
        {
          return false;
/* all this doesnt work, computer will not attack with the animated land

//does the computer have any land in play?
CardList land = new CardList(AllZone.Computer_Play.getCards());
land = land.getType("Land");
land = land.filter(new CardListFilter()
{
  public boolean addCard(Card c)
  {
          //checks for summoning sickness, and is not tapped
    return CombatUtil.canAttack(c);
  }
});
return land.size() > 1 && CardFactoryUtil.AI_isMainPhase();
*/
        }
      };//SpellAbility
//      spell.setChooseTargetAI(CardFactoryUtil.AI_targetType("Land", AllZone.Computer_Play));

      spell.setBeforePayMana(CardFactoryUtil.input_targetType(spell, "Land"));
      card.clearSpellAbility();
      card.addSpellAbility(spell);
    }//*************** END ************ END **************************
    
 
    //*************** START *********** START **************************
    if (cardName.equals("Ior Ruin Expedition"))
    {
	    final SpellAbility ability = new Ability(card, "0")
	    {
	      public boolean canPlay()
	      {
	              return card.getCounters(Counters.QUEST) >= 3 && AllZone.GameAction.isCardInPlay(card)&&!AllZone.Stack.getSourceCards().contains(card);//in play and not already activated(Sac cost problems)
	      }
	      public boolean canPlayAI() {return (AllZone.Computer_Hand.size() < 6)&&(AllZone.Computer_Library.size()>0);}
	
	      public void resolve()
	      {
	        AllZone.GameAction.drawCard(card.getController());
	        AllZone.GameAction.drawCard(card.getController());
	        AllZone.GameAction.sacrifice(getSourceCard());
	      }
	    };
	    ability.setDescription("Remove three quest counters from Ior Ruin Expedition and sacrifice it: Draw two cards.");
	    ability.setStackDescription(card.getName() +" - Draw two cards.");
	    
	    card.addSpellAbility(ability);
    }
    
   

    //*************** START *********** START **************************
    else if(cardName.equals("Earthcraft"))
    {
    	final SpellAbility a1 = new Ability(card,"0")
        {
			private static final long serialVersionUID = 6787319311700905218L;
	
			public boolean canPlayAI()
			  {
				  return false;
			  }
			  
	    	  public boolean canPlay()
	    	  {
	    		  String controller = card.getController();
	    		  PlayerZone play = AllZone.getZone(Constant.Zone.Play, controller);
		  
	    		  CardList creats = new CardList();
	    		  
	    		  creats.addAll(play.getCards());
	    		  creats = creats.getType("Creature");
	    		  creats = creats.filter(new CardListFilter()
	    		  {
					public boolean addCard(Card c) {
						return c.isUntapped();
					} 
	    		  });
	    		  
	    		  if (creats.size() > 0 && AllZone.GameAction.isCardInPlay(card))
	    			  return true;
	    		  else
	    			  return false;
	    	  }
	    	  public void resolve()
	    	  {
	
	    		if (getTargetCard()== null)
	    			return;
	    		  
	            String player = card.getController();
	            if(player.equals(Constant.Player.Human))
	              humanResolve();
	            //else
	            //  computerResolve();
	          }
	      	  public void humanResolve()
	          {
	      		  String controller = card.getController();
	      		  PlayerZone play = AllZone.getZone(Constant.Zone.Play, controller);
	      		  CardList creats = new CardList();
	  		  
	      		  creats.addAll(play.getCards());
	      		  creats = creats.getType("Creature");
	      		  creats = creats.filter(new CardListFilter()
	      		  {
					public boolean addCard(Card c) {
						return c.isUntapped() && !c.equals(card);
					}
	      		  });    		  
	      		  
	      		  if (creats.size() == 1) {
	      			  creats.get(0).tap();
	      			  if (AllZone.GameAction.isCardInPlay(getTargetCard()) && CardFactoryUtil.canTarget(card, getTargetCard()))
		      		      getTargetCard().untap();
	      			  
	      		  }
	      		  else if (creats.size() > 1) {
		      		  Object o = AllZone.Display.getChoice("Select creature to tap", creats.toArray());
		      		  
		      		  if (o!= null)
		      		  {
		      			  Card c1 = (Card)o;
		      			  c1.tap();
		      			  if (AllZone.GameAction.isCardInPlay(getTargetCard()) && CardFactoryUtil.canTarget(card, getTargetCard()))
			      		      getTargetCard().untap();
		      		  }
	      		  }
	      		  
	      		  
	
	          }//humanResolve

        };//a1
        
        //card.clearSpellAbility();
        card.addSpellAbility(a1);
        a1.setDescription("Tap an untapped creature you control: untap target basic land.");
        
        Input runtime = new Input()
        {
			private static final long serialVersionUID = -2287693175821059029L;

			public void showMessage()
	          {
	            CardList all = new CardList();
	            all.addAll(AllZone.Human_Play.getCards());
	            all.addAll(AllZone.Computer_Play.getCards());
	            all = all.filter(new CardListFilter()
	            {
		  			public boolean addCard(Card c) {
		  				return (c.isBasicLand()) && CardFactoryUtil.canTarget(card, c) && c.isTapped();
		  			}
	            });
	            
	            stopSetNext(CardFactoryUtil.input_targetSpecific(a1, all, "Select target basic land", true));
	          }
        };
        a1.setBeforePayMana(runtime);
    	
    }//*************** END ************ END **************************
    
    
   
    
  //*************** START *********** START **************************
    else if(cardName.equals("Mox Diamond"))
    {
    	final Input discard = new Input()
    	{
    			private static final long serialVersionUID = -1319202902385425204L;
				public void showMessage()
    			{
				    AllZone.Display.showMessage("Discard a land card (or select Mox Diamond to sacrifice it)");
				    ButtonUtil.enableOnlyCancel();
				}
				public void selectCard(Card c, PlayerZone zone)
				{
				    if(zone.is(Constant.Zone.Hand) && c.isLand())
				    {
				    	AllZone.GameAction.discard(c);
				    	stop();
				    }
				    else if (c.equals(card))
				    {
				    	AllZone.GameAction.sacrifice(card);
				    	stop();
				    }
				}
	    };//Input

	    final SpellAbility ability = new Ability(card, "0")
	    {
	    	public void resolve()
	    	{
	    		if(card.getController().equals(Constant.Player.Human))
	    		{
	    			if(AllZone.Human_Hand.getCards().length == 0)
	    				AllZone.GameAction.sacrifice(card);
	    			else
	    				AllZone.InputControl.setInput(discard);
	    		}
	    		else
	    		{
	    			CardList list = new CardList(AllZone.Computer_Hand.getCards());
	    			list = list.filter(new CardListFilter()
	    			{
	    				public boolean addCard(Card c)
	    				{
	    					return (c.isLand());
	    				}
	    			});
	    			AllZone.GameAction.discard(list.get(0));
	    		}//else
	    	}//resolve()
	    };//SpellAbility
	    Command intoPlay = new Command()
	    {
			private static final long serialVersionUID = -7679939432259603542L;

			public void execute()
	    	{
	    		ability.setStackDescription("If Mox Diamond would come into play, you may discard a land card instead. If you do, put Mox Diamond into play. If you don't, put it into its owner's graveyard.");
	    		AllZone.Stack.add(ability);
	    	}
	    };
	    SpellAbility spell = new Spell_Permanent(card)
	    {
			private static final long serialVersionUID = -1818766848857998431L;

			//could never get the AI to work correctly
		    //it always played the same card 2 or 3 times
	    	public boolean canPlayAI() {return false;}
	
	    	public boolean canPlay()
	    	{
	    		PlayerZone hand = AllZone.getZone(Constant.Zone.Hand, card.getController());
	    		CardList list = new CardList(hand.getCards());
	    		list.remove(card);
	    		list = list.filter(new CardListFilter()
	    		{
	    			public boolean addCard(Card c)
	    			{
	    				return (c.isLand());
	    			}
	    		});
	    		return list.size() != 0 && super.canPlay();
	    	}//canPlay()
	    };
	    card.addComesIntoPlayCommand(intoPlay);
	    card.clearSpellKeepManaAbility();
	    card.addSpellAbility(spell);
    }//*************** END ************ END **************************
    
    //*************** START *********** START **************************
    else if (cardName.equals("Standstill"))
    {
    	card.clearSpellAbility();
	    card.addSpellAbility(new Spell_Permanent(card)
	    {
		  private static final long serialVersionUID = 6912683989507840172L;

		  public boolean canPlayAI()
	      {
	    	  PlayerZone compPlay = AllZone.getZone(Constant.Zone.Play, Constant.Player.Computer);
	    	  PlayerZone humPlay = AllZone.getZone(Constant.Zone.Play, Constant.Player.Human);
	    	  
	    	  CardList compCreats = new CardList(compPlay.getCards());
	    	  compCreats = compCreats.getType("Creature");
	    	  
	    	  CardList humCreats = new CardList(humPlay.getCards());
	    	  humCreats = humCreats.getType("Creature");
	    	  	    	  
	    	  //only play standstill if comp controls more creatures than human
	    	  //this needs some additional rules, maybe add all power + toughness and compare
	    	  if(compCreats.size() > humCreats.size())
	    		return true;
	    	  else 
	    		return false;
	      }
	    });
 	}//*************** END ************ END **************************
    
   
   
    //*************** START *********** START **************************
    if (cardName.equals("Legacy Weapon"))
    {
    	final Ability ability = new Ability(card, "W U B R G")
    	{
    		public void resolve()
    		{
    			Card c = getTargetCard();
    			
    			if (AllZone.GameAction.isCardInPlay(c) && CardFactoryUtil.canTarget(card, c))
    			{
    				AllZone.GameAction.removeFromGame(c);
    			}
    		}
    		
    		public void chooseTargetAI()
    		{
    			PlayerZone hplay = AllZone.getZone(Constant.Zone.Play, Constant.Player.Human);
    			CardList human = new CardList(hplay.getCards());
    			human = human.filter(new CardListFilter()
    			{
    				public boolean addCard(Card c)
    				{
    					return CardFactoryUtil.canTarget(card, c);
    				}
    			});
    	    	
    			if (human.size() != 0)
    			{
    				setTargetCard(CardFactoryUtil.AI_getMostExpensivePermanent(human, card, true));
    			}
    		}
    		
    		public boolean canPlayAI()
    		{
    			PlayerZone hplay = AllZone.getZone(Constant.Zone.Play, Constant.Player.Human);
    			CardList human = new CardList(hplay.getCards());
    			human = human.filter(new CardListFilter()
    			{
    				public boolean addCard(Card c)
    				{
    					return CardFactoryUtil.canTarget(card, c);
    				}
    			});
    			return human.size() > 0; 
    		}
    		
    	};//ability
    	
    	Input target = new Input()
    	{
			private static final long serialVersionUID = -7279903055386088569L;
			public void showMessage()
	      	{
	      	  AllZone.Display.showMessage("Select target permanent for " +ability.getSourceCard());
	      	  ButtonUtil.enableOnlyCancel();
	      	}
	      	public void selectButtonCancel() {stop();}
	      	public void selectCard(Card crd, PlayerZone zone)
	      	{
	      	  if(zone.is(Constant.Zone.Play) && CardFactoryUtil.canTarget(card, crd))
	      	  {
	      		ability.setTargetCard(crd);
	      		stopSetNext(new Input_PayManaCost(ability));
	      	  }
	      	}
        };//Input
        ability.setDescription("W U B R G: Exile target permanent.");
    	
    	ability.setBeforePayMana(target);
    	card.addSpellAbility(ability);
    
  	}//*************** END ************ END **************************
    
  //*************** START *********** START **************************
    else if (cardName.equals("Voltaic Key"))
    {
    	final Ability_Tap ability = new Ability_Tap(card, "1")
    	{
			private static final long serialVersionUID = 6097818373831898299L;
			public void resolve()
    		{
    			Card c = getTargetCard();
    			if (AllZone.GameAction.isCardInPlay(c) && CardFactoryUtil.canTarget(card, c) && c.isTapped())
    				c.untap();
    		}
    		public boolean canPlayAI()
    		{
    			return false;
    		}
    	};//ability
    	
    	Input target = new Input()
    	{
			private static final long serialVersionUID = -7279903055386088569L;
			public void showMessage()
	      	{
	      	  AllZone.Display.showMessage("Select target tapped artifact for " +ability.getSourceCard());
	      	  ButtonUtil.enableOnlyCancel();
	      	}
	      	public void selectButtonCancel() {stop();}
	      	public void selectCard(Card crd, PlayerZone zone)
	      	{
	      	  if(zone.is(Constant.Zone.Play) && CardFactoryUtil.canTarget(card, crd) && crd.isArtifact() && crd.isTapped())
	      	  {
	      		ability.setTargetCard(crd);
	      		stopSetNext(new Input_PayManaCost(ability));
	      	  }
	      	}
        };//Input
    	ability.setDescription("1, tap: Untap target artifact.");
        
    	ability.setBeforePayMana(target);
    	card.addSpellAbility(ability);
    
  	}//*************** END ************ END **************************
    
    //*************** START *********** START **************************
    else if (cardName.equals("Seething Song"))
    {
       final SpellAbility spell = new Spell(card)
       {
          private static final long serialVersionUID = 113811381138L;
          
          public void resolve()
          {
             //CardList list = new CardList(AllZone.getZone(Constant.Zone.Play, Constant.Player.Human).getCards());
             //list = list.getName("Mana Pool");
             Card mp = AllZone.ManaPool;//list.getCard(0);
             mp.addExtrinsicKeyword("ManaPool:R");
             mp.addExtrinsicKeyword("ManaPool:R");
             mp.addExtrinsicKeyword("ManaPool:R");
             mp.addExtrinsicKeyword("ManaPool:R");
             mp.addExtrinsicKeyword("ManaPool:R");
          }
          public boolean canPlayAI()
          {
             return false;
          }
       };
       
        spell.setStackDescription("Adds R R R R R to your mana pool");
       card.clearSpellAbility();
        card.addSpellAbility(spell);

        return card;
    }//*************** END ************ END **************************
    
    //*************** START *********** START **************************
    else if (cardName.equals("Dark Ritual"))
    {
       final SpellAbility spell = new Spell(card)
       {

		private static final long serialVersionUID = -8579887529151755266L;
		public void resolve()
          {
             /*CardList list = new CardList(AllZone.getZone(Constant.Zone.Play, Constant.Player.Human).getCards());
             list = list.getName("Mana Pool");*/
             Card mp = AllZone.ManaPool;//list.getCard(0);
             mp.addExtrinsicKeyword("ManaPool:B");
             mp.addExtrinsicKeyword("ManaPool:B");
             mp.addExtrinsicKeyword("ManaPool:B");
          }
          public boolean canPlayAI()
          {
             return false;
          }
       };
       
       spell.setStackDescription(cardName + " adds B B B to your mana pool");
       card.clearSpellAbility();
       card.addSpellAbility(spell);

       return card;
    }//*************** END ************ END **************************
    
    //*************** START *********** START **************************
    else if (cardName.equals("Black Lotus"))
    {
       final SpellAbility ability = new Ability_Tap(card)
       {
         private static final long serialVersionUID = 8394047173115959008L;

         public boolean canPlayAI(){return false;}
          
          public void resolve()
          {
             if (card.getController().equals(Constant.Player.Human))
             {
                    //CardList list = new CardList(AllZone.getZone(Constant.Zone.Play, Constant.Player.Human).getCards());
                    //list = list.getName("Mana Pool");
                    Card mp = AllZone.ManaPool;//list.getCard(0);

                    String color = new String();
                                        
                    Object o = AllZone.Display.getChoice("Choose mana color",Constant.Color.Colors);
                    color = (String)o;
                    
                    if (color.equals("white"))
                    	color = "W";
                    else if (color.equals("blue"))
                    	color = "U";
                    else if (color.equals("black"))
                    	color = "B";
                    else if (color.equals("red"))
                    	color = "R";
                    else if (color.equals("green"))
                    	color = "G";
                    else
                    	color = "1";
                    		
                    
                    //System.out.println("ManaPool:"+color+":");
                    for (int i = 0; i < 3; i++)
                    	mp.addExtrinsicKeyword("ManaPool:"+ color);
                   
                    AllZone.GameAction.sacrifice(card);
             }
          }
       };
       ability.setDescription("tap, Sacrifice Black Lotus: Add three mana of any one color to your mana pool.");
       ability.setStackDescription("Adds 3 mana of any one color to your mana pool");
       card.addSpellAbility(ability);
    }//*************** END ************ END **************************
    
 
    
    //**************************Equipment*****************************
    
 
    //*************** START ************ START **************************
    else if (cardName.equals("Ashnod's Transmogrant"))
    {
    	final Ability_Tap ability = new Ability_Tap(card)
    	{
			private static final long serialVersionUID = -401631574059431293L;
			public void resolve()
    		{
				if (card.getController().equals(Constant.Player.Computer))
					AllZone.GameAction.sacrifice(card);
    			if(getTargetCard() == null || !getTargetCard().isCreature())
    				return;
    			Card crd = getTargetCard();
    			crd.addCounter(Counters.P1P1, 1);
    			if(!crd.getType().contains("Artifact"))
    				crd.addType("Artifact");
    		}
    		public boolean canPlayAI()
    		{
    			CardList list = new CardList(AllZone.Computer_Play.getCards()).filter(
    					new CardListFilter(){public boolean addCard(Card c){ return !c.isArtifact() && c.isCreature(); }});
    			Card crd = CardFactoryUtil.AI_getBestCreature(list);
    			if (crd != null)
    				setTargetCard(crd);
    			return (getTargetCard() != null);
    		}
    	};
    	Input runtime = new Input()
        {
    		  private static final long serialVersionUID = 141164423096887945L;
    		  
    		  public void showMessage()
    	      {
    	        AllZone.Display.showMessage("Select target creature for " +card);
    	        ButtonUtil.enableOnlyCancel();
    	      }
    	      public void selectButtonCancel() {stop();}
    	      public void selectCard(Card c, PlayerZone zone)
    	      {
    	    	if (!CardFactoryUtil.canTarget(ability, c))
    	    	{
    	    		 AllZone.Display.showMessage("Cannot target this card (Shroud? Protection?).");
    	    	}
    	    	else if(c.isCreature() && !c.isArtifact() && zone.is(Constant.Zone.Play))
    	        {
    	          ability.setTargetCard(c);
    	          AllZone.GameAction.sacrifice(card);
    	          stopSetNext(new Input_NoCost_TapAbility(ability));
    	          }
    	      }
    	    };
    	ability.setBeforePayMana(runtime);
    	ability.setDescription("T, Sacrifice Ashnod's Transmogrant: put a +1/+1 counter on target nonartifact creature. That creature becomes an artifact in addition to its other types.");
    	card.addSpellAbility(ability);
    }//*************** END ************ END **************************
    
    //*************** START ************ START **************************
    else if (cardName.equals("Gemstone Array"))
    {
    	final Ability store = new Ability(card, "2"){
    		public void resolve(){card.addCounter(Counters.CHARGE, 1);}
    		public boolean canPlayAI(){ return AllZone.Phase.is(Constant.Phase.Main2, Constant.Player.Computer);}
    	};
    	store.setDescription("2: Put a charge counter on Gemstone Array.");
    	store.setStackDescription("Put a charge counter on Gemstone Array");
    	card.addSpellAbility(store);
    	
    	final Ability_Mana retrieve = new Ability_Mana(card, "Remove a charge counter from Gemstone Array: Add one mana of any color to your mana pool.")
    	{
			private static final long serialVersionUID = -2938965362221626028L;
			
			@SuppressWarnings("unused")
    		boolean undoable = true;
    		public void undo() {card.addCounter(Counters.CHARGE, 1);}
    		public String Mana() {return this.choices_made[0].toString();}
    		public boolean canPlay()
    		{
    			if(choices_made[0] == null)
    				choices_made[0] = "1";
    			return super.canPlay() && card.getCounters(Counters.CHARGE) > 0;
    		}
    		public void resolve(){card.subtractCounter(Counters.CHARGE, 1); super.resolve();}
    	};
    	retrieve.choices_made = new String[1];
		retrieve.setBeforePayMana(new Input(){

			private static final long serialVersionUID = 376497609786542558L;

			public void showMessage()
    		{
    			retrieve.choices_made[0] = Input_PayManaCostUtil.getColor2((String)AllZone.Display.getChoiceOptional("Select a Color", Constant.Color.onlyColors));
    			AllZone.Stack.add(retrieve);
    			stop();
    		}
    	});
    	card.addSpellAbility(retrieve);
    }//*************** END ************ END **************************
    
    
    
    //*************** START ************ START **************************
    else if (cardName.equals("Goblin Grenade"))
    {
    	final SpellAbility DamageCP =  new Spell(card)
    	{
			private static final long serialVersionUID = -4289150611689144985L;
			Card check;
    	     
    		  public boolean canPlay()
    		  {
    			  CardList gobs = new CardList(AllZone.getZone(Constant.Zone.Play, card.getController()).getCards());
	    	      gobs = gobs.getType("Goblin");
	    	      
	    	      return super.canPlay() && gobs.size()>0;
    		  }
    		  
    	      public boolean canPlayAI()
    	      {
    	          if(AllZone.Human_Life.getLife() <= 5)
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
    	          if(AllZone.Human_Life.getLife() <= 5)
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
    	            throw new RuntimeException(card +" error in chooseTargetAI() - Card c is " +c +",  Card check is " +check);
    	         
    	          if (c != null)
    	        	  setTargetCard(c);
    	          else
    	        	  setTargetPlayer(Constant.Player.Human);
    	      }//chooseTargetAI()
    	         
    	         //uses "damage" variable
    	      Card getFlying()
    	      {
    	    	  CardList flying = CardFactoryUtil.AI_getHumanCreature("Flying", card, true);
    	          for(int i = 0; i < flying.size(); i++)
    	        	  if(flying.get(i).getNetDefense() <= 5){
    	        		  System.out.println("getFlying() returns " + flying.get(i).getName());
    	        		  return flying.get(i);
    	        	  }
    	          
    	        System.out.println("getFlying() returned null");
    	        return null;
    	      }
    	      public void resolve()
    	      {
    	    	  if (card.getController().equals(Constant.Player.Computer))
    	    	  {
    	    		  CardList gobs = new CardList(AllZone.getZone(Constant.Zone.Play, card.getController()).getCards());
    	    	      gobs = gobs.getType("Goblin");
    	    	      
    	    	      if (gobs.size()>0)
    	    	      {
    	    	    	  CardListUtil.sortAttackLowFirst(gobs);
    	    	    	  AllZone.GameAction.sacrifice(gobs.get(0));
    	    	      }
    	    	      //TODO, if AI can't sack, break out of this
    	    	  }
    	              
    	          if(getTargetCard() != null)
    	          {
    	            if(AllZone.GameAction.isCardInPlay(getTargetCard()) && CardFactoryUtil.canTarget(card, getTargetCard()))
    	            {
    	                Card c = getTargetCard();
    	                //c.addDamage(damage);
    	                AllZone.GameAction.addDamage(c, card, 5);
    	            }
    	          }
    	          else
    	            AllZone.GameAction.getPlayerLife(getTargetPlayer()).subtractLife(5);
    	          //resolve()
    	      }
    	   }; //spellAbility
    	   DamageCP.setDescription(card.getName() + " deals 5 damage to target creature or player.");
    	   //DamageCP.setStackDescription(card.getName() +" deals 5 damage.");
    	   
    	   Input target = new Input()
    	   {
			private static final long serialVersionUID = 1843037500197925110L;
			public void showMessage()
    	      {
    	        AllZone.Display.showMessage("Select target Creature, Player, or Planeswalker");
    	        ButtonUtil.enableOnlyCancel();
    	      }
    	      public void selectButtonCancel() {stop();}
    	      public void selectCard(Card crd, PlayerZone zone)
    	      {
    	        if((crd.isCreature() || crd.isPlaneswalker() ) && zone.is(Constant.Zone.Play) && CardFactoryUtil.canTarget(DamageCP, crd) )
    	        {
    	          DamageCP.setTargetCard(crd);
    	          done();
    	        }
    	      }//selectCard()
    	      public void selectPlayer(String player)
    	      {
    	        DamageCP.setTargetPlayer(player);
    	        done();
    	      }
    	      void done()
    	      {
    	          AllZone.Stack.add(DamageCP);
    	          stop();
    	      }
    	   };
    	   
    	   Input targetSac = new Input()
    	    {
    		  
			private static final long serialVersionUID = -6102143961778874295L;
			public void showMessage()
    	      {
    	        AllZone.Display.showMessage("Select a Goblin to sacrifice.");
    	        ButtonUtil.enableOnlyCancel();
    	      }
    	      public void selectButtonCancel() {stop();}
    	      public void selectCard(Card crd, PlayerZone zone)
    	      {
    	    	CardList choices = new CardList(AllZone.getZone(Constant.Zone.Play, card.getController()).getCards());
    	        choices = choices.getType("Goblin");
    	    	
    	    	if(choices.contains(crd))
    	        {
    	    	  AllZone.GameAction.sacrifice(crd);
    	    	  //DamageCP.setTargetCard(crd);
    	          if(DamageCP instanceof Ability_Tap && DamageCP.getManaCost().equals("0"))
    	             stopSetNext(new Input_NoCost_TapAbility((Ability_Tap)DamageCP));
    	          else if(DamageCP.getManaCost().equals("0"))
    	          {
    	            //AllZone.Stack.add(DamageCP);
    	            stop();
    	          }
    	          else
    	            stopSetNext(new Input_PayManaCost(DamageCP));
    	        }
    	      }//selectCard()
    	   };

    	   DamageCP.setBeforePayMana(targetSac);
    	   DamageCP.setAfterPayMana(target);
    	   card.clearSpellAbility();
    	   card.addSpellAbility(DamageCP);
    }//*************** END ************ END **************************
    
    
    //*************** START *********** START **************************
    else if(cardName.equals("Onyx Goblet"))
    {
      final Ability_Tap ability = new Ability_Tap(card)
      {
		private static final long serialVersionUID = -5726693225692494554L;

		public boolean canPlayAI() {return AllZone.Phase.getPhase().equals(Constant.Phase.Main2);}
	
	      public void resolve()
	        {
	          String opponent = AllZone.GameAction.getOpponent(card.getController());
	          AllZone.GameAction.getPlayerLife(opponent).subtractLife(1);
	        }
      };//SpellAbility
      card.addSpellAbility(ability);
      ability.setDescription("tap: Target player loses 1 life.");
      ability.setStackDescription(card.getName() + " - Opponent loses 1 life.");
      ability.setBeforePayMana(new Input_NoCost_TapAbility(ability));
    }//*************** END ************ END **************************
	
	//*************** START *********** START **************************
    else if(cardName.equals("Braidwood Cup"))
    {
      final Ability_Tap ability = new Ability_Tap(card)
      {
		private static final long serialVersionUID = -7784976576326683976L;

		public boolean canPlayAI() {return AllZone.Phase.getPhase().equals(Constant.Phase.Main2);}
	
	      public void resolve()
	      {
	          AllZone.GameAction.getPlayerLife(card.getController()).addLife(1);      
	      }
      };//SpellAbility
      card.addSpellAbility(ability);
      ability.setDescription("tap: You gain 1 life.");
      ability.setStackDescription("Braidwood Cup -"+card.getController() + " gains 1 life.");
      ability.setBeforePayMana(new Input_NoCost_TapAbility(ability));
    }//*************** END ************ END **************************
    
  //*************** START *********** START **************************
    else if(cardName.equals("Scepter of Insight"))
    {
     final SpellAbility ability = new Ability_Tap(card, "3 U")
     {
		private static final long serialVersionUID = -3567474686431369541L;

		public boolean canPlayAI() {return AllZone.Phase.getPhase().equals(Constant.Phase.Main2);}

         public void resolve()
         {
        	 AllZone.GameAction.drawCard(card.getController());
         }
         
      };//SpellAbility
      card.addSpellAbility(ability);
      ability.setDescription("3 U, tap: Draw a card.");
      ability.setStackDescription(card.getName() + " - draw a card.");
    }//*************** END ************ END **************************
    
  //*************** START *********** START **************************
    else if(cardName.equals("Innocent Blood"))
    {
    	final SpellAbility spell = new Spell(card)
    	{
			private static final long serialVersionUID = 3915880400376059369L;

			public void resolve() {
				AllZone.GameAction.sacrificeCreature(Constant.Player.Human, this);
				AllZone.GameAction.sacrificeCreature(Constant.Player.Computer, this);
			}    	
			
			public boolean canPlayAI()
			{
				PlayerZone cPlay = AllZone.getZone(Constant.Zone.Play, Constant.Player.Computer);
				PlayerZone hPlay = AllZone.getZone(Constant.Zone.Play, Constant.Player.Human);
				
				CardList hList = new CardList(hPlay.getCards());
				CardList cList = new CardList(cPlay.getCards());
				CardList smallCreats = cList.filter(new CardListFilter()
				{
					public boolean addCard(Card c) {
						return c.isCreature() && c.getNetAttack() < 2 && c.getNetDefense() < 3;
					}
				});
				
				hList = hList.getType("Creature");
				cList = cList.getType("Creature");
				
				if (hList.size() == 0)
					return false;
				
				return smallCreats.size() > 0;
			}
    	};
    	
    	card.clearSpellAbility();
    	card.addSpellAbility(spell);
    }//*************** END ************ END **************************
    
    //*************** START *********** START **************************
    else if (cardName.equals("Diabolic Edict") || cardName.equals("Chainer's Edict"))
    {
    	final SpellAbility spell = new Spell(card)
    	{
			private static final long serialVersionUID = 8970446094797667088L;

			public void resolve() {
				AllZone.GameAction.sacrificeCreature(getTargetPlayer(), this);
			}   
			
			public boolean canPlayAI()
			{
				PlayerZone hPlay = AllZone.getZone(Constant.Zone.Play, Constant.Player.Human);
				CardList hList = new CardList(hPlay.getCards());
				hList = hList.getType("Creature");
				return hList.size() > 0;
			}
    	};
    	spell.setChooseTargetAI(CardFactoryUtil.AI_targetHuman());
    	spell.setBeforePayMana(CardFactoryUtil.input_targetPlayer(spell));
    
    	card.clearSpellAbility();
    	card.addSpellAbility(spell);
    	
    	if (cardName.equals("Chainer's Edict"))
    	{
    		final SpellAbility flashback = new Spell(card)
    	    {
				private static final long serialVersionUID = -4889392369463499074L;

				public boolean canPlay()
    	        {
    	        	PlayerZone grave = AllZone.getZone(Constant.Zone.Graveyard ,card.getController());
    				String phase         = AllZone.Phase.getPhase();
    				String activePlayer  = AllZone.Phase.getActivePlayer();
    				
    				return AllZone.GameAction.isCardInZone(card, grave) && 
    						((phase.equals(Constant.Phase.Main1) || phase.equals(Constant.Phase.Main2)) &&
    					    card.getController().equals(activePlayer) && AllZone.Stack.size() == 0 );
    	        }

    	        public boolean canPlayAI()
    			{
    				PlayerZone hPlay = AllZone.getZone(Constant.Zone.Play, Constant.Player.Human);
    				CardList hList = new CardList(hPlay.getCards());
    				hList = hList.getType("Creature");
    				return hList.size() > 0;
    			}
    	        
				public void resolve() {
					PlayerZone grave = AllZone.getZone(Constant.Zone.Graveyard , card.getController());
					PlayerZone removed = AllZone.getZone(Constant.Zone.Removed_From_Play, card.getController());
					
					AllZone.GameAction.sacrificeCreature(getTargetPlayer(), this);
					
					grave.remove(card);
					removed.add(card);
				}
    	    };
    	    
    	    flashback.setManaCost("5 B B");
    	    flashback.setBeforePayMana(CardFactoryUtil.input_targetPlayer(flashback));
    	    flashback.setDescription("Flashback: 5 B B");
    	    flashback.setChooseTargetAI(CardFactoryUtil.AI_targetHuman());
    		
    	    card.addSpellAbility(flashback);
    		card.setFlashback(true);
    	}//if Chainer's Edict
    }//*************** END ************ END **************************
    
  //*************** START *********** START **************************
    else if (cardName.equals("Cruel Edict") || cardName.equals("Imperial Edict"))
    {
    	final SpellAbility spell = new Spell(card)
    	{
			private static final long serialVersionUID = 4782606423085170723L;

			public void resolve() {
				AllZone.GameAction.sacrificeCreature(AllZone.GameAction.getOpponent(card.getController()), this);
			}   
			
			public boolean canPlayAI()
			{
				PlayerZone hPlay = AllZone.getZone(Constant.Zone.Play, Constant.Player.Human);
				CardList hList = new CardList(hPlay.getCards());
				hList = hList.getType("Creature");
				return hList.size() > 0;
			}
    	};
    	card.clearSpellAbility();
    	card.addSpellAbility(spell);
    }//*************** END ************ END **************************


	//*************** START *********** START **************************
    else if (cardName.equals("Door to Nothingness"))
    {
    	Ability_Tap ab1 = new Ability_Tap(card, "G G R R B B U U W W")
    	{

			private static final long serialVersionUID = 6665327569823149191L;
			public void resolve() {
				AllZone.GameAction.getPlayerLife(getTargetPlayer()).setLife(0);
			}
			public boolean canPlayAI(){return true;}
    	};
    	ab1.setChooseTargetAI(CardFactoryUtil.AI_targetHuman());
    	ab1.setBeforePayMana(CardFactoryUtil.input_targetPlayer(ab1));
    	ab1.setDescription("WWUUBBRRGG, tap, sacrifice Door to Nothingness: Target player loses the game.");
    	card.addSpellAbility(ab1);
    }//*************** END ************ END **************************
    
    
  //*************** START *********** START **************************
    else if(cardName.equals("Seal of Cleansing") || cardName.equals("Seal of Primordium") )
    {
      final Ability ability = new Ability(card, "0")
      {
        public boolean canPlayAI() {return getArtEnchantments().size() != 0;}

        public void chooseTargetAI()
        {
          
          CardList list = getArtEnchantments();
          if (list.size() > 0)
          {
	          CardListUtil.sortCMC(list);
	          list.reverse();
	          setTargetCard(list.get(0));
	          AllZone.GameAction.sacrifice(card);
          }
        }//chooseTargetAI()
        CardList getArtEnchantments()
        {
       
          PlayerZone play = AllZone.getZone(Constant.Zone.Play, Constant.Player.Human);
          CardList list = new CardList(play.getCards());
          list = list.filter(new CardListFilter()
          {
            public boolean addCard(Card c)
            {
              return c.isArtifact() || c.isEnchantment();
            }
          });
          return list;
        }//getArtEnchantments()

        public void resolve()
        {
          if(getTargetCard() != null)
          {
            if(AllZone.GameAction.isCardInPlay(getTargetCard()) && CardFactoryUtil.canTarget(card, getTargetCard()) )
              AllZone.GameAction.destroy(getTargetCard());
          }
        }//resolve()
      };//SpellAbility
      
      Input runtime = new Input()
      {
		private static final long serialVersionUID = -1750678113925588670L;

		public void showMessage()
        {
			card.addSpellAbility(ability);
		    ability.setDescription("Sacrifice " +cardName +": destroy target artifact or enchantment.");
		      
		    PlayerZone hplay = AllZone.getZone(Constant.Zone.Play, Constant.Player.Human);
		    PlayerZone cplay = AllZone.getZone(Constant.Zone.Play, Constant.Player.Computer);
		    CardList choices = new CardList();
		    choices.addAll(hplay.getCards());
		    choices.addAll(cplay.getCards());
		      
		    choices = choices.filter(new CardListFilter()
		    {
		    	public boolean addCard(Card c) {
					return c.isEnchantment() || c.isArtifact();
		    	}
		    });
	          stopSetNext(CardFactoryUtil.input_targetSpecific(ability, choices, "Destroy target artifact or enchantment", new Command()
	          {
	              
	      		private static final long serialVersionUID = -4987328870651000691L;
	
	      		public void execute()
	              {
	                AllZone.GameAction.sacrifice(card);
	              }
	            }, true));
        }
      };
      
      ability.setDescription("Sacrifice " + card.getName() + ": destroy target artifact or enchantment.");
      ability.setBeforePayMana(runtime);
      card.addSpellAbility(ability);
      
    }//*************** END ************ END **************************
    
    //*************** START *********** START **************************
    else if(cardName.equals("Seal of Fire"))
    {
      final Ability ability = new Ability(card, "0")
      {
        public boolean canPlayAI() {return getCreature().size() != 0 || AllZone.Human_Life.getLife() < 4;}

        public void chooseTargetAI()
        {
          if(AllZone.Human_Life.getLife() < 4)
            setTargetPlayer(Constant.Player.Human);
          else
          {
            CardList list = getCreature();
            list.shuffle();
            setTargetCard(list.get(0));
          }
          AllZone.GameAction.sacrifice(card);
        }//chooseTargetAI()
        CardList getCreature()
        {
          //toughness of 1
          CardList list = CardFactoryUtil.AI_getHumanCreature(2, card, true);
          list = list.filter(new CardListFilter()
          {
            public boolean addCard(Card c)
            {
              //only get 1/1 flyers or 2/1 or bigger creatures
              return (2 <= c.getNetAttack()) || c.getKeyword().contains("Flying");
            }
          });
          return list;
        }//getCreature()

        public void resolve()
        {
          if(getTargetCard() != null)
          {
            if(AllZone.GameAction.isCardInPlay(getTargetCard()) && CardFactoryUtil.canTarget(card, getTargetCard()) )
              getTargetCard().addDamage(2, card);
          }
          else
            AllZone.GameAction.getPlayerLife(getTargetPlayer()).subtractLife(2);
        }//resolve()
      };//SpellAbility

      card.addSpellAbility(ability);
      ability.setDescription("Sacrifice Seal of Fire: Seal of Fire deals 2 damage to target creature or player.");
      ability.setBeforePayMana(CardFactoryUtil.input_targetCreaturePlayer(ability, new Command()
      {
		private static final long serialVersionUID = 4180346673509230280L;

		public void execute()
        {
          AllZone.GameAction.sacrifice(card);
        }
      }, true));
    }//*************** END ************ END **************************
    
  //*************** START *********** START **************************
    else if(cardName.equals("Seal of Removal"))
    {
      final Ability ability = new Ability(card, "0")
      {
        public boolean canPlayAI() {return getCreature().size() != 0;}

        public void chooseTargetAI()
        {
          CardList list = getCreature();
          list.shuffle();
          setTargetCard(list.get(0));

          AllZone.GameAction.sacrifice(card);
        }//chooseTargetAI()
        CardList getCreature()
        {
          CardList list = new CardList(AllZone.Computer_Play.getCards());
          list = list.filter(new CardListFilter()
          {
            public boolean addCard(Card c)
            {
              return c.isCreature() && (c.getNetAttack() >= 3|| c.getKeyword().contains("Flying") ||
            		 c.isEnchanted());
            }
          });
          return list;
        }//getCreature()

        public void resolve()
        {
          if(getTargetCard() != null)
          {
        	  
            if(AllZone.GameAction.isCardInPlay(getTargetCard()) && CardFactoryUtil.canTarget(card, getTargetCard()) )
            {
            	PlayerZone hand = AllZone.getZone(Constant.Zone.Hand, getTargetCard().getOwner());
            	AllZone.GameAction.moveTo(hand, getTargetCard());
            }
          }
        }//resolve()
      };//SpellAbility

      card.addSpellAbility(ability);
      ability.setDescription("Sacrifice Seal of Removal: return target creature to its owner's hand.");
      ability.setBeforePayMana(CardFactoryUtil.input_targetCreature(ability, new Command()
      {
		
		private static final long serialVersionUID = 2565599788533507611L;

		public void execute()
        {
          AllZone.GameAction.sacrifice(card);
        }
      }));
    }//*************** END ************ END **************************
    

    //*************** START *********** START **************************
    else if(cardName.equals("Conqueror's Pledge"))
    {   	
      SpellAbility spell = new Spell(card)
      {
		private static final long serialVersionUID = -2902179434079334177L;

		public void resolve()
        {
          PlayerZone play = AllZone.getZone(Constant.Zone.Play, card.getController());
          for (int i = 0; i < 6; i++)
          {
            Card c = new Card();

            c.setOwner(card.getController());
            c.setController(card.getController());

            c.setName("Kor Soldier");
            c.setImageName("W 1 1 Kor Soldier");
            c.setManaCost("W");
            c.setToken(true);

            c.addType("Creature");
            c.addType("Kor");
            c.addType("Soldier");
            c.setBaseAttack(1);
            c.setBaseDefense(1);

            play.add(c);
          }//for
        }//resolve()
      };
      
      spell.setDescription("Put six 1/1 white Kor Soldier creature tokens onto the battlefield.");
      spell.setStackDescription(card.getName() + " - " + card.getController() + " puts six 1/1 white Kor Soldier creature tokens onto the battlefield.");
      
      SpellAbility kicker = new Spell(card)
      {

		private static final long serialVersionUID = 1376255732058673590L;

		public void resolve()
        {
          PlayerZone play = AllZone.getZone(Constant.Zone.Play, card.getController());
          for (int i = 0; i < 12; i++)
          {
            Card c = new Card();

            c.setOwner(card.getController());
            c.setController(card.getController());

            c.setName("Kor Soldier");
            c.setImageName("W 1 1 Kor Soldier");
            c.setManaCost("W");
            c.setToken(true);

            c.addType("Creature");
            c.addType("Kor");
            c.addType("Soldier");
            c.setBaseAttack(1);
            c.setBaseDefense(1);

            play.add(c);
          }//for
        }//resolve()
      };
      
      kicker.setManaCost("8 W W W");
      kicker.setDescription("Kicker 6: If Conqueror's Pledge was kicked, put twelve of those tokens onto the battlefield instead.");
      kicker.setStackDescription(card.getName() + " - " + card.getController() + " puts twelve 1/1 white Kor Soldier creature tokens onto the battlefield.");
      
      card.clearSpellAbility();
      card.addSpellAbility(spell);
      card.addSpellAbility(kicker);
    }//*************** END ************ END **************************
    
    
    //*************** START *********** START **************************
    else if(cardName.equals("Kaervek's Spite"))
    {
      final SpellAbility spell = new Spell(card)
      {
		private static final long serialVersionUID = -6259614160639535500L;

		public boolean canPlayAI()
		{
			if(AllZone.Human_Life.getLife() <= 5)
				return true;
			PlayerZone play = AllZone.getZone(Constant.Zone.Play, Constant.Player.Computer);
			PlayerZone lib = AllZone.getZone(Constant.Zone.Library, Constant.Player.Computer);
			
			CardList playList = new CardList(play.getCards());
			CardList libList = new CardList(lib.getCards());
			
			playList = playList.getName("Academy Rector");
			libList = libList.getName("Barren Glory");
			
			return (AllZone.Human_Life.getLife() <= 5) || (playList.size() == 1 && libList.size() >= 1);
		}
		
		public void resolve()
        {
			PlayerZone play = AllZone.getZone(Constant.Zone.Play, card.getController());
			PlayerZone hand = AllZone.getZone(Constant.Zone.Play, card.getController());
			CardList list = new CardList(play.getCards());
			list = list.filter(new CardListFilter()
			{
				public boolean addCard(Card c) {
					return !c.getName().equals("Mana Pool");
				}
			});
			CardList handList = new CardList(hand.getCards());
			
			for (Card c : list)
			{
				AllZone.GameAction.sacrifice(c);
			}
			AllZone.GameAction.discardRandom(card.getController(), handList.size());
			
          PlayerLife life = AllZone.GameAction.getPlayerLife(getTargetPlayer());
          life.subtractLife(5);
        }
      };
      spell.setChooseTargetAI(CardFactoryUtil.AI_targetHuman());
      card.clearSpellAbility();
      card.addSpellAbility(spell);
      
      /*
      final Command sac = new Command(){
		private static final long serialVersionUID = 1643946454479782123L;

		public void execute() {
			PlayerZone play = AllZone.getZone(Constant.Zone.Play, card.getController());
			PlayerZone hand = AllZone.getZone(Constant.Zone.Play, card.getController());
			CardList list = new CardList(play.getCards());
			list = list.filter(new CardListFilter()
			{
				public boolean addCard(Card c) {
					return !c.getName().equals("Mana Pool");
				}
			});
			CardList handList = new CardList(hand.getCards());
			
			for (Card c : list)
			{
				AllZone.GameAction.sacrifice(c);
			}
			AllZone.GameAction.discardRandom(card.getController(), handList.size());
		}
    	  
      };
      */

      spell.setBeforePayMana(CardFactoryUtil.input_targetPlayer(spell));
    }//*************** END ************ END **************************
  
	//*************** START *********** START **************************
	else if (cardName.equals("Staff of Domination"))
	{
	
      final Ability_Tap ability2 = new Ability_Tap(card, "2")
      {

		private static final long serialVersionUID = -5513078874305811825L;

		public boolean canPlayAI() {return AllZone.Phase.getPhase().equals(Constant.Phase.Main2);}
	
	      public void resolve()
	      {
	          AllZone.GameAction.getPlayerLife(card.getController()).addLife(1);      
	      }
      };//SpellAbility
      
      ability2.setDescription("2, tap: You gain 1 life");
      ability2.setStackDescription(cardName + " - You gain 1 life.");
      
      final SpellAbility ability3 = new Ability_Tap(card, "3")
      {
		private static final long serialVersionUID = 1125696151526415705L;
		public boolean canPlayAI() {return getTapped().size() != 0;}
        public void chooseTargetAI()
        {
          card.tap();
          Card target = CardFactoryUtil.AI_getBestCreature(getTapped());
          setTargetCard(target);
        }
        CardList getTapped()
        {
          CardList list = new CardList(AllZone.Computer_Play.getCards());
          list = list.filter(new CardListFilter()
          {
            public boolean addCard(Card c)
            {
              return c.isCreature() && c.isTapped();
            }
          });
          return list;
        }//getTapped()
        public void resolve()
        {
          if(AllZone.GameAction.isCardInPlay(getTargetCard())  && CardFactoryUtil.canTarget(card, getTargetCard()) )
          {
            Card c = getTargetCard();
            if(c.isTapped())
              c.untap();
          }
        }//resolve()
      };//SpellAbility
      
      ability3.setDescription("3, tap: Untap target creature.");
      ability3.setBeforePayMana(CardFactoryUtil.input_targetCreature(ability3));
      
      final SpellAbility ability4 = new Ability_Tap(card, "4")
      {

		private static final long serialVersionUID = 8102011024731535257L;

		public boolean canPlayAI() {return false;}
        
        public void resolve()
        {
          if(AllZone.GameAction.isCardInPlay(getTargetCard())  && CardFactoryUtil.canTarget(card, getTargetCard()) )
          {
            Card c = getTargetCard();
            if(c.isUntapped())
              c.tap();
          }
        }//resolve()
      };//SpellAbility
      
      ability4.setDescription("4, tap: Tap target creature.");
      ability4.setBeforePayMana(CardFactoryUtil.input_targetCreature(ability4));
      
      final Ability_Tap ability5 = new Ability_Tap(card, "5")
      {

		private static final long serialVersionUID = -8459438547823091716L;
		public boolean canPlayAI() {return true;}
        public void resolve()
        {
          AllZone.GameAction.drawCard(card.getController());
        } 
      };//SpellAbility
      
      ability5.setDescription("5, tap: Draw a card.");
      ability5.setStackDescription(card.getName() + " - draw a card.");
      
      card.addSpellAbility(ability2);
      card.addSpellAbility(ability3);
      card.addSpellAbility(ability4);
      card.addSpellAbility(ability5);
      
  	}//*************** END ************ END **************************
	
	//*************** START *********** START **************************
	if (cardName.equals("Goblin Charbelcher"))
	{
		final Ability_Tap ability = new Ability_Tap(card, "3")
		{
			private static final long serialVersionUID = -840041589720758423L;

			public void resolve() {
				PlayerZone lib = AllZone.getZone(Constant.Zone.Library, card.getController());
				CardList topOfLibrary = new CardList(lib.getCards());
				CardList revealed = new CardList();
				
				if (topOfLibrary.size() == 0)
					return;
				
				int damage = 0;
				int count = 0;
				Card c = null;
				Card crd;
				while (c == null)
				{
					revealed.add(topOfLibrary.get(count));
					crd = topOfLibrary.get(count++);
					if (crd.isLand() || count == topOfLibrary.size() ) {
						c = crd;
						damage = count;
						if (crd.getName().equals("Mountain"))
							damage = damage * 2;
					}
				}//while
				AllZone.Display.getChoiceOptional("Revealed cards:", revealed.toArray());
				
				if(getTargetCard() != null)
		          {
		            if(AllZone.GameAction.isCardInPlay(getTargetCard()) && CardFactoryUtil.canTarget(card, getTargetCard()))
		            {
		            	getTargetCard().addDamage(damage, card);
		            }
		          }
		          else
		            AllZone.GameAction.getPlayerLife(getTargetPlayer()).subtractLife(damage);
			}
		};
		ability.setDescription("3, tap: Reveal cards from the top of your library until you reveal a land card. Goblin Charbelcher deals damage equal to the number of nonland cards revealed this way to target creature or player. If the revealed land card was a Mountain, Goblin Charbelcher deals double that damage instead. Put the revealed cards on the bottom of your library in any order.");
		ability.setChooseTargetAI(CardFactoryUtil.AI_targetHuman());
		ability.setBeforePayMana(CardFactoryUtil.input_targetCreaturePlayer(ability, true));
		card.addSpellAbility(ability);

		
    }//*************** END ************ END **************************
	
	//*************** START *********** START **************************
	if (cardName.equals("Tinker"))
	{
		final SpellAbility spell = new Spell(card)
		{
			private static final long serialVersionUID = -5878957726445248334L;

			public boolean canPlay() {
				PlayerZone play = AllZone.getZone(Constant.Zone.Play, card.getController());
				CardList list = new CardList(play.getCards());
				list = list.getType("Artifact");
				
				return list.size() > 0;
			}
			
			public boolean canPlayAI()
			{
				PlayerZone play = AllZone.getZone(Constant.Zone.Play, Constant.Player.Computer);
				PlayerZone lib = AllZone.getZone(Constant.Zone.Library, Constant.Player.Computer);
				
				CardList playList = new CardList(play.getCards());
				playList = playList.filter(new CardListFilter()
				{
					public boolean addCard(Card c) {
						return c.isArtifact() && CardUtil.getConvertedManaCost(c.getManaCost()) <= 2;
					}
				});
				
				CardList libList = new CardList(lib.getCards());
				libList = libList.filter(new CardListFilter()
				{
					public boolean addCard(Card c){
						return c.isArtifact() && CardUtil.getConvertedManaCost(c.getManaCost()) > 5;
					}
				});
				
				if (libList.size() > 0 && playList.size() > 0)
				{
					playList.shuffle();
					setTargetCard(playList.get(0));
					return true;
				}
				return false;
				
			}

			public void resolve() {
				Card c = getTargetCard();
				
				PlayerZone play = AllZone.getZone(Constant.Zone.Play, card.getController());
				PlayerZone lib = AllZone.getZone(Constant.Zone.Library, card.getController());
				
				if (AllZone.GameAction.isCardInPlay(c))
				{
				
					AllZone.GameAction.sacrifice(c);
					
					if (card.getController().equals(Constant.Player.Computer)) {
						
						CardList list = new CardList(lib.getCards());
						list = list.filter(new CardListFilter()
						{
							public boolean addCard(Card c)
							{
								return c.isArtifact() && CardUtil.getConvertedManaCost(c.getManaCost()) > 5;
							}
						});
						
						if (list.size() > 0)
						{
							Card crd = CardFactoryUtil.AI_getBestArtifact(list);
							lib.remove(crd);
							play.add(crd);
							AllZone.GameAction.shuffle(Constant.Player.Computer);
						}
					}
					else //human
					{
						CardList list = new CardList(lib.getCards());
						list = list.filter(new CardListFilter()
						{
							public boolean addCard(Card c)
							{
								return c.isArtifact();
							}
						});
						if (list.size() > 0) {
							Object o = AllZone.Display.getChoiceOptional("Select artifact", list.toArray());
				       		 
							if (o != null)
							{
								Card crd = (Card)o;
								lib.remove(crd);
								play.add(crd);
								
							}
							AllZone.GameAction.shuffle(Constant.Player.Human);
						}
					}
				}//if isCardInPlay
			}
		};
		/*
		final Command sac = new Command()
		{
			private static final long serialVersionUID = -8925816099640324876L;

			public void execute() {
				AllZone.GameAction.sacrifice(spell.getTargetCard());
			}
		};
		*/
		
		Input runtime = new Input()
	    {
			private static final long serialVersionUID = -4653972223582155502L;

			public void showMessage()
	        {
	          CardList choice = new CardList();
	          choice.addAll(AllZone.Human_Play.getCards());
	          choice = choice.getType("Artifact");
	          
	          stopSetNext(CardFactoryUtil.input_targetSpecific(spell, choice, "Select artifact to sacrifice.", false));
	        }
	    };
	    spell.setBeforePayMana(runtime);  
		
		
		card.clearSpellAbility();
		card.addSpellAbility(spell);
		
    }//*************** END ************ END **************************
	
	//*************** START *********** START **************************
    else if (cardName.equals("Thopter Foundry"))
    {
    	final String player = card.getController();
    	
    	final SpellAbility ability = new Ability(card, "1")
        {
    		public void chooseTargetAI()
            {
    			Card c;
    			PlayerZone play = AllZone.getZone(Constant.Zone.Play, player);
      	    	
      	    	CardList meek = new CardList();
      	    	meek.addAll(play.getCards());
      	    	meek = meek.getName("Sword of the Meek");
      	    	
      	    	if (meek.size() >= 1)
      	    		c = meek.get(0); 
      	    	else
      	    		c = getArtifact();
      	    	setTargetCard(c);

            }
            public Card getArtifact()
            {
            	//target creature that is going to attack
            	PlayerZone play = AllZone.getZone(Constant.Zone.Play, player);
    	    	
    	    	CardList arts = new CardList();
    	    	arts.addAll(play.getCards());
    	    	arts = arts.filter(new CardListFilter()
    	    	{
    	    		public boolean addCard(Card c)
    	    		{
    	    			return c.isArtifact() && !c.isToken() && (CardUtil.getConvertedManaCost(c.getManaCost()) <= 1 || c.getName().equals("Sword of the Meek"));
    	    		}
    	    	});
    	    	
    	    	if (arts.size() > 0) {
    	    		arts.shuffle();
    	    		return arts.get(0);
    	    	}
    	    	else
    	    		return null;
            }//getAttacker()
    		
    		
    		public boolean canPlayAI()
    		{
    		   String phase = AllZone.Phase.getPhase();
         	   return phase.equals(Constant.Phase.Main2);
    		}
    		
    		public void resolve()
    		{
    			Card c = getTargetCard();
    			if (AllZone.GameAction.isCardInPlay(c)){
	    			AllZone.GameAction.sacrifice(c);
	    	    	makeToken();
	    	    	PlayerLife life = AllZone.GameAction.getPlayerLife(card.getController());
	                life.addLife(1);
    			}
    		}//resolve
    		
    		public void makeToken()
    		{
    			  Card c = new Card();

    			  c.setName("Thopter");
    	          c.setImageName("U 1 1 Thopter");

    	          c.setOwner(card.getController());
    	          c.setController(card.getController());

    	          c.setManaCost("U");
    	          c.setToken(true);
    	         
    	          c.addType("Artifact");
    	          c.addType("Creature");
    	          c.addType("Thopter");
    	          c.setBaseAttack(1);
    	          c.setBaseDefense(1);
    	          c.addIntrinsicKeyword("Flying");

    	          PlayerZone play = AllZone.getZone(Constant.Zone.Play, card.getController());
    	          play.add(c);
    			
    		}
        };
        
        Input runtime = new Input()
        {

		private static final long serialVersionUID = 3557158378851031238L;

		public void showMessage()
          {
        	PlayerZone play = AllZone.getZone(Constant.Zone.Play, player);
  	    	
        	CardList arts = new CardList();
  	    	arts.addAll(play.getCards());
  	    	arts = arts.filter(new CardListFilter()
  	    	{
  	    		public boolean addCard(Card c)
  	    		{
  	    			return c.isArtifact() && !c.isToken();
  	    		}
  	    	});

            stopSetNext(CardFactoryUtil.input_targetSpecific(ability, arts, "Select a non-token Artifact to sacrifice", false));

          }//showMessage()
        };//Input
        
    	card.addSpellAbility(ability);
    	ability.setDescription("1, Sacrifice a nontoken artifact: Put a 1/1 blue Thopter artifact creature token with flying onto the battlefield. You gain 1 life.");
    	ability.setStackDescription(card.getName() + " - Put a 1/1 blue Thopter artifact creature token with flying onto the battlefield. You gain 1 life.");
    	ability.setBeforePayMana(runtime);
    
    }//*************** END ************ END **************************
	
	//*************** START *********** START **************************
    else if(cardName.equals("Spell Snare"))
    {
      SpellAbility spell = new Spell(card)
      {
		private static final long serialVersionUID = -3254886985412814994L;
		public void resolve()
        {
          SpellAbility sa = AllZone.Stack.pop();
          AllZone.GameAction.moveToGraveyard(sa.getSourceCard());
        }
        public boolean canPlay()
        {
          if(AllZone.Stack.size() == 0)
            return false;

          //see if spell is on stack and that opponent played it
          String opponent = AllZone.GameAction.getOpponent(card.getController());
          SpellAbility sa = AllZone.Stack.peek();

          return sa.isSpell() && opponent.equals(sa.getSourceCard().getController()) 
          		 && CardFactoryUtil.isCounterable(sa.getSourceCard()) && CardUtil.getConvertedManaCost(sa.getSourceCard().getManaCost()) == 2;
        }
      };
      card.clearSpellAbility();
      card.addSpellAbility(spell);
    }//*************** END ************ END **************************

	
	//*************** START *********** START **************************
    else if (cardName.equals("Time Walk") || cardName.equals("Temporal Manipulation") || cardName.equals("Capture of Jingzhou"))
	{
    	final SpellAbility spell = new Spell(card)
    	{
			private static final long serialVersionUID = 35300742940184315L;

			public void resolve() {
				System.out.println("Turn: " + AllZone.Phase.getTurn());
				AllZone.Phase.addExtraTurn(card.getController());
			}
    	};
    	card.clearSpellAbility();
    	card.addSpellAbility(spell);
    }//*************** END ************ END **************************
	
	//*************** START *********** START **************************
    else if (cardName.equals("Time Stretch") || cardName.equals("Time Warp"))
	{
    	final SpellAbility spell = new Spell(card)
    	{
			private static final long serialVersionUID = -76579316599195788L;

			public void resolve() {
				AllZone.Phase.addExtraTurn(getTargetPlayer());
				if (cardName.equals("Time Stretch"));
					AllZone.Phase.addExtraTurn(getTargetPlayer());
			}
    	};
    	card.clearSpellAbility();
    	spell.setChooseTargetAI(CardFactoryUtil.AI_targetHuman());
    	spell.setBeforePayMana(CardFactoryUtil.input_targetPlayer(spell));
    	card.addSpellAbility(spell);
    }//*************** END ************ END **************************
	
	//*************** START *********** START **************************
	  if (cardName.equals("Celestial Purge"))
	    {
	    	final Spell spell = new Spell(card)
	    	{
				private static final long serialVersionUID = 2626237206744317044L;

				public void resolve()
	    		{
	    			Card c = getTargetCard();
	    			
	    			if (AllZone.GameAction.isCardInPlay(c) && CardFactoryUtil.canTarget(card, c))
	    			{
	    				AllZone.GameAction.removeFromGame(c);
	    			}
	    		}
	    		
	    		public void chooseTargetAI()
	    		{
	    			PlayerZone hplay = AllZone.getZone(Constant.Zone.Play, Constant.Player.Human);
	    			CardList human = new CardList(hplay.getCards());
	    			human = human.filter(new CardListFilter()
	    			{
	    				public boolean addCard(Card c)
	    				{
	    					return CardFactoryUtil.canTarget(card, c)
	  					      && (CardUtil.getColors(c).contains(Constant.Color.Black)
	  	    				  || CardUtil.getColors(c).contains(Constant.Color.Red));
	    				}
	    			});
	    	    	
	    			if (human.size() != 0)
	    			{
	    				setTargetCard(CardFactoryUtil.AI_getMostExpensivePermanent(human, card, true));
	    			}
	    		}
	    		
	    		public boolean canPlayAI()
	    		{
	    			PlayerZone hplay = AllZone.getZone(Constant.Zone.Play, Constant.Player.Human);
	    			CardList human = new CardList(hplay.getCards());
	    			human = human.filter(new CardListFilter()
	    			{
	    				public boolean addCard(Card c)
	    				{
	    					return CardFactoryUtil.canTarget(card, c)
	    					  && (CardUtil.getColors(c).contains(Constant.Color.Black)
	    				      || CardUtil.getColors(c).contains(Constant.Color.Red));
	    				}
	    			});
	    			return human.size() > 0; 
	    		}
	    		
	    	};//ability
	    	
	    	Input target = new Input()
	    	{
				private static final long serialVersionUID = -7279903055386088569L;
				public void showMessage()
		      	{
		      	  AllZone.Display.showMessage("Select target black or red permanent for " + card);
		      	  ButtonUtil.enableOnlyCancel();
		      	}
		      	public void selectButtonCancel() {stop();}
		      	public void selectCard(Card crd, PlayerZone zone)
		      	{
		      	  if(zone.is(Constant.Zone.Play) && CardFactoryUtil.canTarget(card, crd)
		      		&& (CardUtil.getColors(crd).contains(Constant.Color.Black)
		      		|| CardUtil.getColors(crd).contains(Constant.Color.Red)))
		      	  {
		      		spell.setTargetCard(crd);
		      		stopSetNext(new Input_PayManaCost(spell));
		      	  }
		      	}
	        };//Input
	        spell.setBeforePayMana(target);
	        card.clearSpellAbility();
	    	card.addSpellAbility(spell);    
	  	}//*************** END ************ END **************************
	  
	  
/*
	  	//*************** START *********** START **************************
	    else if(cardName.equals("Riding the Dilu Horse"))
	    {
	      SpellAbility spell = new Spell(card)
	      {
			private static final long serialVersionUID = -620930445462994580L;

			public boolean canPlayAI()
	        {
			  PlayerZone play = AllZone.getZone(Constant.Zone.Play, Constant.Player.Computer);
			  
		      CardList list = new CardList(play.getCards());
		      list = list.filter(new CardListFilter()
		      {
		    	 public boolean addCard(Card c)
		    	 {
		    		 return c.isCreature() && !c.getKeyword().contains("Horsemanship") && !c.getKeyword().contains("Defender");
		    	 }
		      });
		      if (list.size() > 0) {
			      Card c = CardFactoryUtil.AI_getBestCreature(list, card);
			      setTargetCard(c);
		          return true;
		      }
		      return false;
	        }
	        
	        public void resolve()
	        {
	          final Card[] target = new Card[1];
	          

	          target[0] = getTargetCard();
	          if(AllZone.GameAction.isCardInPlay(target[0]) && CardFactoryUtil.canTarget(card, target[0]))
	          {
	            target[0].addTempAttackBoost(2);
	            target[0].addTempDefenseBoost(2);
	            target[0].addExtrinsicKeyword("Horsemanship");

	            //String s = target[0].getText();
	            target[0].setText("(+2/+2 and Horsemanship from " +card+")");
	          }
	        }//resolve()
	      };
	      spell.setBeforePayMana(CardFactoryUtil.input_targetCreature(spell));
	      card.clearSpellAbility();
	      card.addSpellAbility(spell);
	    }//*************** END ************ END **************************
*/
	  
	  
	    //*************** START *********** START **************************
	    else if (cardName.equals("Millstone"))
	    {
	       Ability_Tap ab1 = new Ability_Tap(card, "2")
	       {

	            private static final long serialVersionUID = 42470566751344693L;

	               public boolean canPlayAI()
	               {
	                   String player = getTargetPlayer();
	                   PlayerZone lib = AllZone.getZone(Constant.Zone.Library, player);
	                   CardList libList = new CardList(lib.getCards());
	                   return libList.size() > 0;
	               }

	               public void resolve()
	               {
	                    String player = getTargetPlayer();
	                    
	                    PlayerZone lib = AllZone.getZone(Constant.Zone.Library, player);
	                    PlayerZone grave = AllZone.getZone(Constant.Zone.Graveyard, player);
	                    CardList libList = new CardList(lib.getCards());

	                    int max = 2;
	                    if (libList.size() < 2)
	                       max = libList.size();
	                    
	                    for (int i=0;i<max;i++)
	                    {
	                       Card c = libList.get(i);
	                       lib.remove(c);
	                       grave.add(c);
	                    }
	               }
	       };
	       ab1.setChooseTargetAI(CardFactoryUtil.AI_targetHuman());
	       ab1.setBeforePayMana(CardFactoryUtil.input_targetPlayer(ab1));
	       ab1.setDescription("2, tap: Target player puts the top two cards of his or her library into his or her graveyard.");
	       card.addSpellAbility(ab1);
	    }//*************** END ************ END **************************
	  
	
	  
	  
	//*************** START *********** START **************************
	    else if(cardName.equals("Path to Exile"))
	    {
	      SpellAbility spell = new Spell(card)
	      {
	      private static final long serialVersionUID = 4752934806606319269L;
	      
	      public void resolve()
	        {
	          if(AllZone.GameAction.isCardInPlay(getTargetCard())  && CardFactoryUtil.canTarget(card, getTargetCard()) )
	          {
	            //add life
	            String player = getTargetCard().getController();
	          //  PlayerLife life = AllZone.GameAction.getPlayerLife(player);
	           // life.addLife(getTargetCard().getNetAttack());
	           PlayerZone lib = AllZone.getZone(Constant.Zone.Library,
	                  player);
	            

	            CardList lands = new CardList(lib.getCards());
	            lands = lands.getType("Basic");

	            if (player.equals("Human") && lands.size() > 0)
	            {
	               String[] choices =
	               { "Yes", "No" };
	               Object choice = AllZone.Display.getChoice(
	                     "Search fo Basic Land?", choices);
	               if (choice.equals("Yes"))
	               {
	                  Object o = AllZone.Display
	                        .getChoiceOptional(
	                              "Pick a basic land card to put into play",
	                              lands.toArray());
	                  if (o != null)
	                  {
	                     Card card = (Card) o;
	                     lib.remove(card);
	                     AllZone.Human_Play.add(card);
	                     card.tap();
	                     lands.remove(card);
	                     AllZone.GameAction.shuffle(player);
	                  }
	               }// if choice yes
	            } // player equals human
	         else if (player.equals("Computer") && lands.size() > 0)
	         {
	            Card card = lands.get(0);
	            lib.remove(card);
	            // hand.add(card);
	            AllZone.Computer_Play.add(card);
	            card.tap();
	            lands.remove(card);
	            AllZone.GameAction.shuffle(player);
	         }
	            //remove card from play
	            AllZone.GameAction.removeFromGame(getTargetCard());
	          }
	        }//resolve()
	        public boolean canPlayAI()
	        {
	          CardList creature = new CardList(AllZone.Human_Play.getCards());
	          creature = creature.getType("Creature");
	          creature = creature.filter(new CardListFilter()
	          {
	         public boolean addCard(Card c) {
	            return CardFactoryUtil.canTarget(card,c);
	         }   
	          });
	          return creature.size() != 0 && (AllZone.Phase.getTurn() > 4);
	        }
	        public void chooseTargetAI()
	        {
	          CardList play = new CardList(AllZone.Human_Play.getCards());
	          Card target = CardFactoryUtil.AI_getBestCreature(play, card);
	          setTargetCard(target);
	        }
	      };
	      spell.setBeforePayMana(CardFactoryUtil.input_targetCreature(spell));

	      card.clearSpellAbility();
	      card.addSpellAbility(spell);
	    }//*************** END ************ END **************************
	  
	  
	//*************** START *********** START **************************
	    if (cardName.equals("Glimpse the Unthinkable") || cardName.equals("Tome Scour"))
	    {
	      final SpellAbility spell = new Spell(card)
	      {
	      private static final long serialVersionUID = 42470566751344693L;

	        public boolean canPlayAI()
	        {
	            String player = getTargetPlayer();
	            PlayerZone lib = AllZone.getZone(Constant.Zone.Library, player);
	            CardList libList = new CardList(lib.getCards());
	            return libList.size() > 0;
	        }

	        public void resolve()
	        {
	             String player = getTargetPlayer();
	            
	             PlayerZone lib = AllZone.getZone(Constant.Zone.Library, player);
	             PlayerZone grave = AllZone.getZone(Constant.Zone.Graveyard, player);
	             CardList libList = new CardList(lib.getCards());

	             int max = 0;
	             if (cardName.equals("Glimpse the Unthinkable")) max = 10; else max = 5;
	             if (libList.size() < max)
	                max = libList.size();
	            
	             for (int i=0;i<max;i++)
	             {
	                Card c = libList.get(i);
	                lib.remove(c);
	                grave.add(c);
	             }
	        }
	      };//SpellAbility
	      spell.setBeforePayMana(CardFactoryUtil.input_targetPlayer(spell));
	      card.clearSpellAbility();
	      card.addSpellAbility(spell);
	    }//*************** END ************ END **************************
	  
	  
	//*************** START *********** START **************************
	    if (cardName.equals("Traumatize"))
	    {
	      final SpellAbility spell = new Spell(card)
	      {
	      private static final long serialVersionUID = 42470566751344693L;

	        public boolean canPlayAI()
	        {
	            String player = getTargetPlayer();
	            PlayerZone lib = AllZone.getZone(Constant.Zone.Library, player);
	            CardList libList = new CardList(lib.getCards());
	            return libList.size() > 0;
	        }

	        public void resolve()
	        {
	             String player = getTargetPlayer();
	            
	             PlayerZone lib = AllZone.getZone(Constant.Zone.Library, player);
	             PlayerZone grave = AllZone.getZone(Constant.Zone.Graveyard, player);
	             CardList libList = new CardList(lib.getCards());

	             int max = libList.size()/2 ;
	            
	             for (int i=0;i<max;i++)
	             {
	                Card c = libList.get(i);
	                lib.remove(c);
	                grave.add(c);
	             }
	        }
	      };//SpellAbility
	      spell.setBeforePayMana(CardFactoryUtil.input_targetPlayer(spell));
	      card.clearSpellAbility();
	      card.addSpellAbility(spell);
	    }//*************** END ************ END **************************
	    
	    
	  //*************** START *********** START **************************
	    if (cardName.equals("Mind Funeral"))
	    {
	      final SpellAbility spell = new Spell(card)
	      {
			private static final long serialVersionUID = 42470566751344693L;

	        public boolean canPlayAI()
	        {
	        	 String player = getTargetPlayer();
	        	 PlayerZone lib = AllZone.getZone(Constant.Zone.Library, player);
	        	 CardList libList = new CardList(lib.getCards());
	        	 return libList.size() > 0;
	        }

	        public void resolve()
	        {
	        	  String player = getTargetPlayer();
		          
		          PlayerZone lib = AllZone.getZone(Constant.Zone.Library, player);
		          PlayerZone grave = AllZone.getZone(Constant.Zone.Graveyard, player);
		          CardList libList = new CardList(lib.getCards());

		          int max = libList.size();
		          int prev = 0 ;
		          int count = 0;
		          int total =0;
		          
		          
		          for (int i=0;i<max;i++)
		          {
		        	  Card c = libList.get(i);
		        	  if (c.getType().contains("Land"))
		        	  {
		        		  count= count + 1;
		        		  if (count == 4 && prev==0) total=i;
		        		  if (count == 4) prev=1;
		        		  
		        	  }
		          }
		        		          
		          for (int i=0;i<total+1;i++)
		          {
		        	  Card c = libList.get(i);
		        	  lib.remove(c);
		        	  grave.add(c);
		          }
	        }
	      };//SpellAbility
	      spell.setBeforePayMana(CardFactoryUtil.input_targetPlayer(spell));
	      card.clearSpellAbility();
	      card.addSpellAbility(spell);
	    }//*************** END ************ END **************************
	    
	    
	  //*************** START *********** START **************************
	    if (cardName.equals("Haunting Echoes"))
	    {
	      final SpellAbility spell = new Spell(card)
	      {
	      private static final long serialVersionUID = 42470566751344693L;

	        public boolean canPlayAI()
	        {
	            String player = getTargetPlayer();
	            PlayerZone lib = AllZone.getZone(Constant.Zone.Library, player);
	            CardList libList = new CardList(lib.getCards());
	            return libList.size() > 0;
	        }

	        public void resolve()
	        {
	             String player = getTargetPlayer();
	            
	             PlayerZone lib = AllZone.getZone(Constant.Zone.Library, player);
	             PlayerZone grave = AllZone.getZone(Constant.Zone.Graveyard, player);
	             PlayerZone exiled = AllZone.getZone(Constant.Zone.Removed_From_Play, player);
	             CardList libList = new CardList(lib.getCards());
	             CardList grvList = new CardList(grave.getCards());

	             int max = libList.size();
	             int grv = grvList.size();
	            
	           for (int j=0;j < grv;j++)
	           {
	             Card g = grvList.get(j);       
	             for (int i=0;i<max;i++)
	              {
	                Card c = libList.get(i);
	                if ( c.getName().equals(g.getName()) && ! g.getType().contains("Basic") )    
	                 {   lib.remove(c);
	                        exiled.add(c);
	                 }
	              }
	             if ( ! g.getType().contains("Basic") ) {grave.remove(g);
	                                                    exiled.add(g);}
	           }
	        }
	      };//SpellAbility
	      spell.setBeforePayMana(CardFactoryUtil.input_targetPlayer(spell));
	      card.clearSpellAbility();
	      card.addSpellAbility(spell);
	    }//*************** END ************ END **************************
	    
	    
	  //*************** START *********** START **************************
	    else if(cardName.equals("Lobotomy"))
	    {
	      final SpellAbility spell = new Spell(card)
	      {
	      private static final long serialVersionUID = 5338238621454661783L;

	      public void resolve()
	        {
	          Card choice = null;

	          //check for no cards in hand on resolve
	          String player = getTargetPlayer();
	          PlayerZone hand = AllZone.getZone(Constant.Zone.Hand, player);
	          PlayerZone lib = AllZone.getZone(Constant.Zone.Library, player);
	          PlayerZone grave = AllZone.getZone(Constant.Zone.Graveyard, player);
	          PlayerZone exiled = AllZone.getZone(Constant.Zone.Removed_From_Play, player);
	          CardList libList = new CardList(lib.getCards());
	          CardList grvList = new CardList(grave.getCards());
	          CardList fullHand = new CardList(hand.getCards());
	          Card[] handChoices = removeLand(hand.getCards());
	          if (fullHand.size() > 0 && card.getController().equals(Constant.Player.Human))
	             AllZone.Display.getChoice("Revealing hand", fullHand.toArray());
	          if(card.getController().equals(Constant.Player.Human))
	          {
	            choice = (Card) AllZone.Display.getChoice("Choose", handChoices);
	          }
	          else//computer chooses
	          {
	            choice = CardUtil.getRandom(handChoices);
	          }
	         
	          String chosen = choice.getName();
	         
	          int max = libList.size();
	          for (int i=0;i<max;i++)
	          {
	            Card c = libList.get(i);
	            if ( c.getName().equals(chosen))    
	             {   lib.remove(c);
	                    exiled.add(c);
	             }
	          }
	          int grv = grvList.size();
	          for (int i=0;i<grv;i++)
	          {
	            Card c = grvList.get(i);
	            if ( c.getName().equals(chosen))    
	             {   grave.remove(c);
	                    exiled.add(c);
	             }
	          }
	          int hnd = fullHand.size();
	          for (int i=0;i<hnd;i++)
	          {
	            Card c = fullHand.get(i);
	            if ( c.getName().equals(chosen))    
	             {   hand.remove(c);
	                    exiled.add(c);
	             }
	          }
	         
	        }//resolve()

	        public boolean canPlayAI()
	        {
	          Card[] c = removeLand(AllZone.Human_Hand.getCards());
	          return 0 < c.length;
	        }

	        Card[] removeLand(Card[] in)
	        {
	          CardList c = new CardList(in);
	          c = c.filter(new CardListFilter()
	          {
	            public boolean addCard(Card c)
	            {
	              return !c.getType().contains("Basic");
	            }
	          });
	          return c.toArray();
	        }//removeLand()
	      };//SpellAbility spell
	      spell.setChooseTargetAI(CardFactoryUtil.AI_targetHuman());
	      card.clearSpellAbility();
	      card.addSpellAbility(spell);

	      spell.setBeforePayMana(CardFactoryUtil.input_targetPlayer(spell));
	    }//*************** END ************ END **************************  
	    
	    
	  //*************** START *********** START **************************
	    if (cardName.equals("Identity Crisis"))
	    {
	      final SpellAbility spell = new Spell(card)
	      {
	      private static final long serialVersionUID = 42470566751344693L;

	        public boolean canPlayAI()
	        {
	            String player = getTargetPlayer();
	            PlayerZone lib = AllZone.getZone(Constant.Zone.Library, player);
	            CardList libList = new CardList(lib.getCards());
	            return libList.size() > 0;
	        }

	        public void resolve()
	        {
	             String player = getTargetPlayer();
	             
	             PlayerZone hand = AllZone.getZone(Constant.Zone.Hand, player);
	             PlayerZone grave = AllZone.getZone(Constant.Zone.Graveyard, player);
	             PlayerZone exiled = AllZone.getZone(Constant.Zone.Removed_From_Play, player);
	             CardList handList = new CardList(hand.getCards());
	             CardList graveList = new CardList(grave.getCards());
	             
	             int max = handList.size() ;
	             for (int i=0;i<max;i++)
	             {
	                Card c = handList.get(i);
	                hand.remove(c);
	                exiled.add(c);
	             }
	             int grv = graveList.size() ;
	             for (int i=0;i<grv;i++)
	             {
	                Card c = graveList.get(i);
	                grave.remove(c);
	                exiled.add(c);
	             }
	        }
	      };//SpellAbility
	      spell.setBeforePayMana(CardFactoryUtil.input_targetPlayer(spell));
	      card.clearSpellAbility();
	      card.addSpellAbility(spell);
	    }//*************** END ************ END **************************
	    
	  //*************** START *********** START **************************
	    if (cardName.equals("Flame Rift"))
	    {
	    	final SpellAbility spell = new Spell(card)
	    	{
				private static final long serialVersionUID = -6008296722680155321L;

				public void resolve()
	    		{
	    			AllZone.Human_Life.subtractLife(4);
	    	        AllZone.Computer_Life.subtractLife(4);
	    		}
				
				public boolean canPlayAI()
				{
					return AllZone.Computer_Life.getLife() > 7 && AllZone.Human_Life.getLife() < 7;
				}
	    	};
	    	card.clearSpellAbility();
	    	card.addSpellAbility(spell);
	    }//*************** END ************ END **************************

	    
	    
		  //*************** START *********** START **************************
		    if (cardName.equals("Serpent Generator"))
		    {
		    	final SpellAbility ability = new Ability_Tap(card, "4")
		    	{
					private static final long serialVersionUID = 8428205362391909464L;

					public void resolve()
		    		{
		    			PlayerZone play = AllZone.getZone(card);
		    			Card c = new Card();

		    	        c.setOwner(card.getController());
		    	        c.setController(card.getController());

		    	        c.setName("Snake");
		    	        c.setImageName("C 1 1 Snake");
		    	        c.setManaCost("");
		    	        c.setToken(true);

		    	        c.addType("Artifact");
		    	        c.addType("Creature");
		    	        c.addType("Snake");
		    	        c.setBaseAttack(1);
		    	        c.setBaseDefense(1);
		    	        
		    	        c.addIntrinsicKeyword("Whenever this creature deals damage to a player, that player gets a poison counter.");

		    	        play.add(c);
		    		}
		    	};
		    	ability.setStackDescription("Put a 1/1 colorless Snake artifact creature token onto the battlefield. This creature has \"Whenever this creature deals damage to a player, that player gets a poison counter.\"");
		    	ability.setDescription("4, Tap: Put a 1/1 colorless Snake artifact creature token onto the battlefield. This creature has \"Whenever this creature deals damage to a player, that player gets a poison counter.\" (A player with ten or more poison counters loses the game.)");
		    	card.addSpellAbility(ability);
		    	
		    }//*************** END ************ END **************************
		    
		    
		      //*************** START *********** START **************************
		      else if(cardName.equals("Urza's Blueprints"))
		      {
		       final SpellAbility ability = new Ability_Tap(card, "0")
		       {
				    private static final long serialVersionUID = -1802481790805608497L;

					public boolean canPlayAI() {return AllZone.Phase.getPhase().equals(Constant.Phase.Main1);}
	
			        public void resolve()
			        {
			          	 AllZone.GameAction.drawCard(card.getController());
			        } 
		        };//SpellAbility
		        card.addSpellAbility(ability);
		        ability.setDescription("tap: Draw a card.");
		        ability.setStackDescription(card.getName() + " - draw a card.");
		      }//*************** END ************ END **************************
		    
		      //*************** START *********** START **************************
		      else if(cardName.equals("Illusions of Grandeur"))
		      {
		    	  final SpellAbility gainLife = new Ability(card, "0")
			      {
			        public void resolve()
			        {
			          Card c = card;
			          PlayerLife life = AllZone.GameAction.getPlayerLife(c.getController());
			          life.addLife(20);
			        }
			      };
			      
			      final SpellAbility loseLife = new Ability(card, "0")
			      {
			        public void resolve()
			        {
			          Card c = card;
			          PlayerLife life = AllZone.GameAction.getPlayerLife(c.getController());
			          life.subtractLife(20);
			        }
			      };
			      
			      Command intoPlay = new Command()
			      {
					private static final long serialVersionUID = 502892931516451254L;

					public void execute()
			        {
			          gainLife.setStackDescription(card.getController() +" gains 20 life");
			          AllZone.Stack.add(gainLife);
			        }
			      };
			      
			      Command leavesPlay = new Command()
			      {
					private static final long serialVersionUID = 5772999389072757369L;

					public void execute()
			    	{
			    		  loseLife.setStackDescription(card.getController() +" loses 20 life");
				          AllZone.Stack.add(loseLife);
			    	}
			      };
			      
			      card.addComesIntoPlayCommand(intoPlay);
			      card.addLeavesPlayCommand(leavesPlay);
			      
		      }//*************** END ************ END **************************
		    
		      //*************** START *********** START **************************
		      else if(cardName.equals("Donate"))
		      {
		    	  final SpellAbility spell = new Spell(card)
		    	  {
					private static final long serialVersionUID = 782912579034503349L;

					public void resolve()
		    		{
		    			  Card c = getTargetCard();
		    			  
		    			  if (c!=null && AllZone.GameAction.isCardInPlay(c) && CardFactoryUtil.canTarget(card, c))
		    			  {
		    				  if (!c.isAura()) 
		    				  {
		    					  ((PlayerZone_ComesIntoPlay)AllZone.Human_Play).setTriggers(false);
		    					  ((PlayerZone_ComesIntoPlay)AllZone.Computer_Play).setTriggers(false);
		    					  
		    					  PlayerZone from = AllZone.getZone(c);
		    			          from.remove(c);
		    			           
		    			          c.setController(AllZone.GameAction.getOpponent(card.getController()));
		    			          
		    			          PlayerZone to = AllZone.getZone(Constant.Zone.Play, AllZone.GameAction.getOpponent(card.getController()));
		    			          to.add(c);
		    					  
		    					  ((PlayerZone_ComesIntoPlay)AllZone.Human_Play).setTriggers(true);
		    					  ((PlayerZone_ComesIntoPlay)AllZone.Computer_Play).setTriggers(true);
		    				  }
		    				  else //Aura
		    				  {
		    					  c.setController(AllZone.GameAction.getOpponent(card.getController()));
		    				  }
		    			  }
		    		}
					
					public boolean canPlayAI()
					{
						CardList list = new CardList(AllZone.Computer_Play.getCards());
						list = list.getName("Illusions of Grandeur");
						
						if (list.size() > 0){
							setTargetCard(list.get(0));
							return true;
						}
						return false;
					}
		    	  };
		    	  
		    	  Input runtime = new Input()
		          {
		    		  private static final long serialVersionUID = -7823269301012427007L;

		    		  public void showMessage()
		    		  {
		    			  PlayerZone play = AllZone.getZone(Constant.Zone.Play, Constant.Player.Human);
		    	    	
		    			  CardList perms = new CardList();
		    			  perms.addAll(play.getCards());
		    			  perms = perms.filter(new CardListFilter()
		    			  {
		    				  public boolean addCard(Card c)
		    				  {
		    					  return c.isPermanent() && !c.getName().equals("Mana Pool");
		    				  }
		    			  });

		    			  stopSetNext(CardFactoryUtil.input_targetSpecific(spell, perms, "Select a permanent you control", true));

		    		  }//showMessage()
		          };//Input
		    	  
		    	  spell.setBeforePayMana(runtime);
		    	  
		    	  card.clearSpellAbility();
		    	  card.addSpellAbility(spell);
		      }//*************** END ************ END **************************
		    
		      //*************** START *********** START **************************
		      else if(cardName.equals("Gush"))
		      {
		    	  final SpellAbility spell = new Spell(card)
		    	  {
    				private static final long serialVersionUID = 8881817765689776033L;

					public void resolve()					
		    		{
		    			  AllZone.GameAction.drawCard(card.getController());
		    			  AllZone.GameAction.drawCard(card.getController());
		    		}
		    	  };
		    	  spell.setDescription("Draw two cards.");
		    	  spell.setStackDescription(card.getName() + " - Draw two cards.");
		    	  
		    	  final SpellAbility bounce = new Spell(card)
		    	  {
					private static final long serialVersionUID = 1950742710354343569L;

					public void resolve()
		    		{
		    			  AllZone.GameAction.drawCard(card.getController());
		    			  AllZone.GameAction.drawCard(card.getController());
		    		}
					public boolean canPlay()
					{
						PlayerZone play = AllZone.getZone(Constant.Zone.Play, card.getController());
						CardList list = new CardList(play.getCards());
						list = list.getType("Island");
						return list.size() >= 2;
					}
					
		    	  };
		    	  bounce.setDescription("You may return two Islands you control to their owner's hand rather than pay Gush's mana cost.");
		    	  bounce.setStackDescription(card.getName() + " - Draw two cards.");
		    	  bounce.setManaCost("0");
		    	  
		    	  final Input bounceIslands = new Input()
			      {
					private static final long serialVersionUID = 3124427514142382129L;
					int stop = 2;
			        int count = 0;

			        public void showMessage()
			        {
			          AllZone.Display.showMessage("Select an Island");
			          ButtonUtil.disableAll();
			        }
			        public void selectButtonCancel() {stop();}
			        public void selectCard(Card c, PlayerZone zone)
			        {
			          if(c.getType().contains("Island") && zone.is(Constant.Zone.Play))
			          {
			            AllZone.GameAction.moveToHand(c);
			        	  
			            count++;
			            if(count == stop) {
			            	AllZone.Stack.add(bounce);
			            	stop();
			            }
			          }
			        }//selectCard()
			      };
			      
			      bounce.setBeforePayMana(bounceIslands);
			      
			      Command bounceIslandsAI = new Command()
			      {
					private static final long serialVersionUID = 235908265780575226L;

					public void execute()
			    	  {
			    		  PlayerZone play = AllZone.getZone(Constant.Zone.Play, Constant.Player.Computer);
			    		  CardList list = new CardList(play.getCards());
			    		  list = list.getType("Island");
			    		  //TODO: sort by tapped
			    		  
			    		  for (int i=0;i<2;i++)
			    		  {
			    			  AllZone.GameAction.moveToHand(list.get(i));
			    		  }  
			    	  }
			      };
		    	  
			      bounce.setBeforePayManaAI(bounceIslandsAI);
			      
		    	  card.clearSpellAbility();
		    	  card.addSpellAbility(bounce);
		    	  card.addSpellAbility(spell);
		      }//*************** END ************ END **************************
		    
		    
		  //*************** START *********** START **************************
		      else if(cardName.equals("Thwart"))
		      {
		    	  final SpellAbility spell = new Spell(card)
		    	  {
					private static final long serialVersionUID = 6549506712141125977L;
					public void resolve()
		    	        {
		    	          SpellAbility sa = AllZone.Stack.pop();
		    	          AllZone.GameAction.moveToGraveyard(sa.getSourceCard());
		    	        }
		    	        public boolean canPlay()
		    	        {
		    	          if(AllZone.Stack.size() == 0)
		    	            return false;

		    	          //see if spell is on stack and that opponent played it
		    	          String opponent = AllZone.GameAction.getOpponent(card.getController());
		    	          SpellAbility sa = AllZone.Stack.peek();

		    	          return sa.isSpell() && opponent.equals(sa.getSourceCard().getController()) 
		    	          		 && CardFactoryUtil.isCounterable(sa.getSourceCard());
		    	        }
		    	        public boolean canPlayAI()
						{
							return false;
						}
		    	  };
		    	  spell.setDescription("Counter target spell.");
		    	  spell.setStackDescription(card.getName() + " - Counter target spell.");
		    	  
		    	  final SpellAbility bounce = new Spell(card)
		    	  {
					private static final long serialVersionUID = -8310299673731730438L;

					public void resolve()
		    		{
						 SpellAbility sa = AllZone.Stack.pop();
		    	         AllZone.GameAction.moveToGraveyard(sa.getSourceCard());
		    		}
					public boolean canPlay()
					{
						if(AllZone.Stack.size() == 0)
		    	            return false;

		    	          //see if spell is on stack and that opponent played it
		    	        String opponent = AllZone.GameAction.getOpponent(card.getController());
		    	        SpellAbility sa = AllZone.Stack.peek();

						PlayerZone play = AllZone.getZone(Constant.Zone.Play, card.getController());
						CardList list = new CardList(play.getCards());
						list = list.getType("Island");
						return sa.isSpell() && opponent.equals(sa.getSourceCard().getController()) 
							   && CardFactoryUtil.isCounterable(sa.getSourceCard()) && list.size() >= 3;
					}
					
					public boolean canPlayAI()
					{
						return false;
					}
					
		    	  };
		    	  bounce.setDescription("You may return two Islands you control to their owner's hand rather than pay Thwart's mana cost.");
		    	  bounce.setStackDescription(card.getName() + " - Counter target spell.");
		    	  bounce.setManaCost("0");
		    	  
		    	  final Input bounceIslands = new Input()
			      {
					private static final long serialVersionUID = 3124427514142382129L;
					int stop = 3;
			        int count = 0;

			        public void showMessage()
			        {
			          AllZone.Display.showMessage("Select an Island");
			          ButtonUtil.disableAll();
			        }
			        public void selectButtonCancel() {stop();}
			        public void selectCard(Card c, PlayerZone zone)
			        {
			          if(c.getType().contains("Island") && zone.is(Constant.Zone.Play))
			          {
			            AllZone.GameAction.moveToHand(c);
			        	  
			            count++;
			            if(count == stop) {
			            	AllZone.Stack.add(bounce);
			            	stop();
			            }
			          }
			        }//selectCard()
			      };
			      
			      bounce.setBeforePayMana(bounceIslands);
			      
			      Command bounceIslandsAI = new Command()
			      {
					private static final long serialVersionUID = 8250154784542733353L;

					public void execute()
			    	  {
			    		  PlayerZone play = AllZone.getZone(Constant.Zone.Play, Constant.Player.Computer);
			    		  CardList list = new CardList(play.getCards());
			    		  list = list.getType("Island");
			    		  //TODO: sort by tapped
			    		  
			    		  for (int i=0;i<3;i++)
			    		  {
			    			  AllZone.GameAction.moveToHand(list.get(i));
			    		  }  
			    	  }
			      };
		    	  
			      bounce.setBeforePayManaAI(bounceIslandsAI);
			      
		    	  card.clearSpellAbility();
		    	  card.addSpellAbility(bounce);
		    	  card.addSpellAbility(spell);
		      }//*************** END ************ END **************************
		    
		    
		    //*************** START *********** START **************************
		      else if(cardName.equals("Force of Will"))
		      {
		    	  final SpellAbility spell = new Spell(card)
		    	  {
					private static final long serialVersionUID = 7960371805654673281L;
					public void resolve()
		    	        {
		    	          SpellAbility sa = AllZone.Stack.pop();
		    	          AllZone.GameAction.moveToGraveyard(sa.getSourceCard());
		    	        }
		    	        public boolean canPlay()
		    	        {
		    	          if(AllZone.Stack.size() == 0)
		    	            return false;

		    	          //see if spell is on stack and that opponent played it
		    	          String opponent = AllZone.GameAction.getOpponent(card.getController());
		    	          SpellAbility sa = AllZone.Stack.peek();

		    	          return sa.isSpell() && opponent.equals(sa.getSourceCard().getController()) 
		    	          		 && CardFactoryUtil.isCounterable(sa.getSourceCard());
		    	        }
		    	        public boolean canPlayAI()
						{
							return false;
						}
		    	  };
		    	  spell.setDescription("Counter target spell.");
		    	  spell.setStackDescription(card.getName() + " - Counter target spell.");
		    	  
		    	  final SpellAbility alt = new Spell(card)
		    	  {
					private static final long serialVersionUID = -8643870743780757816L;

					public void resolve()
		    		{
						 SpellAbility sa = AllZone.Stack.pop();
		    	         AllZone.GameAction.moveToGraveyard(sa.getSourceCard());
		    		}
					public boolean canPlay()
					{
						if(AllZone.Stack.size() == 0)
		    	            return false;

		    	          //see if spell is on stack and that opponent played it
		    	        String opponent = AllZone.GameAction.getOpponent(card.getController());
		    	        SpellAbility sa = AllZone.Stack.peek();

						PlayerZone hand = AllZone.getZone(Constant.Zone.Hand, card.getController());
						CardList list = new CardList(hand.getCards());
						list = list.filter(new CardListFilter()
						{
							public boolean addCard(Card c)
							{
								return CardUtil.getColors(c).contains(Constant.Color.Blue) && !c.equals(card);
							}
						});
						return sa.isSpell() && opponent.equals(sa.getSourceCard().getController()) 
							   && CardFactoryUtil.isCounterable(sa.getSourceCard()) && list.size() >= 1;
					}
					
					public boolean canPlayAI()
					{
						return false;
					}
					
		    	  };
		    	  alt.setDescription("You may pay 1 life and exile a blue card from your hand rather than pay Force of Will's mana cost.");
		    	  alt.setStackDescription(card.getName() + " - Counter target spell.");
		    	  alt.setManaCost("0");
		    	  
		    	  final Input exileBlue = new Input()
			      {
					private static final long serialVersionUID = 8692998689009712987L;
					int stop = 1;
			        int count = 0;

			        public void showMessage()
			        {
			          AllZone.Display.showMessage("Select a blue card");
			          ButtonUtil.disableAll();
			        }
			        public void selectButtonCancel() {stop();}
			        public void selectCard(Card c, PlayerZone zone)
			        {
			          if(CardUtil.getColors(c).contains(Constant.Color.Blue) && zone.is(Constant.Zone.Hand) &&
			             !c.equals(card))
			          {
			            AllZone.GameAction.removeFromGame(c);
			            String player = card.getController();
			            AllZone.GameAction.getPlayerLife(player).subtractLife(1);
			        	  
			            count++;
			            if(count == stop) {
			            	AllZone.Stack.add(alt);
			            	stop();
			            }
			          }
			        }//selectCard()
			      };
			      
			      
			      alt.setBeforePayMana(exileBlue);
			      
			      /*
			      Command bounceIslandsAI = new Command()
			      {
					private static final long serialVersionUID = -8745630329512914365L;

					public void execute()
			    	  {
			    		  PlayerZone play = AllZone.getZone(Constant.Zone.Play, Constant.Player.Computer);
			    		  CardList list = new CardList(play.getCards());
			    		  list = list.getType("Island");
			    		  //TODO: sort by tapped
			    		  
			    		  for (int i=0;i<3;i++)
			    		  {
			    			  AllZone.GameAction.moveToHand(list.get(i));
			    		  }  
			    	  }
			      };
		    	  
			      alt.setBeforePayManaAI(bounceIslandsAI);
			      */
			      
		    	  card.clearSpellAbility();
		    	  card.addSpellAbility(alt);
		    	  card.addSpellAbility(spell);
		      }//*************** END ************ END **************************
		    
	        
    // Cards with Cycling abilities
    // -1 means keyword "Cycling" not found
    if (hasKeyword(card, "Cycling") != -1)
    {
      int n = hasKeyword(card, "Cycling");
      if (n != -1)
      {
        String parse = card.getKeyword().get(n).toString();
        card.removeIntrinsicKeyword(parse);

        String k[] = parse.split(":");
        final String manacost = k[1];

        card.addSpellAbility(CardFactoryUtil.ability_cycle(card, manacost));
      }
    }//Cycling
    
    while (hasKeyword(card, "TypeCycling") != -1)
    {
      int n = hasKeyword(card, "TypeCycling");
      if (n != -1)
      {
        String parse = card.getKeyword().get(n).toString();
        card.removeIntrinsicKeyword(parse);

        String k[] = parse.split(":");
        final String type = k[1];
        final String manacost = k[2];

        card.addSpellAbility(CardFactoryUtil.ability_typecycle(card, manacost,type));
      }
    }//TypeCycling

    if (hasKeyword(card, "Transmute") != -1)
    {
      int n = hasKeyword(card, "Transmute");
      if (n != -1)
      {
        String parse = card.getKeyword().get(n).toString();
        card.removeIntrinsicKeyword(parse);

        String k[] = parse.split(":");
        final String manacost = k[1];

        card.addSpellAbility(CardFactoryUtil.ability_transmute(card, manacost));
      }
    }//transmute

    while (hasKeyword(card,"Soulshift") != -1)
    {
      int n = hasKeyword(card,"Soulshift");
      if (n != -1)
      {
        String parse = card.getKeyword().get(n).toString();
        card.removeIntrinsicKeyword(parse);

        String k[] = parse.split(":");
        final String manacost = k[1];
        

        card.addSpellAbility(CardFactoryUtil.soul_desc(card, manacost));
        card.addDestroyCommand(CardFactoryUtil.ability_Soulshift(card, manacost));
      }
    }//Soulshift
    
    if (hasKeyword(card,"Vanishing") != -1)
    {
      int n = hasKeyword(card,"Vanishing");
      if (n != -1)
      {
        String parse = card.getKeyword().get(n).toString();
        card.removeIntrinsicKeyword(parse);

        String k[] = parse.split(":");
        final int power = Integer.parseInt(k[1]);
        

        card.addComesIntoPlayCommand(CardFactoryUtil.vanishing(card, power));
        card.addSpellAbility(CardFactoryUtil.vanish_desc(card, power));
      }
    }//Vanishing

    
    return card;
  }//getCard2
	   
	// copies stats like attack, defense, etc..
	private Card copyStats(Object o) {
		Card sim = (Card) o;
		Card c = new Card();

		c.setBaseAttack(sim.getBaseAttack());
		c.setBaseDefense(sim.getBaseDefense());
		c.setIntrinsicKeyword(sim.getKeyword());
		c.setName(sim.getName());
		c.setType(sim.getType());
		c.setText(sim.getSpellText());
		c.setManaCost(sim.getManaCost());

		return c;
	}// copyStats()

	public static void main(String[] args) {
		CardFactory f = new CardFactory("cards.txt");
		Card c = f.getCard("Arc-Slogger", "d");
		System.out.println(c.getOwner());
	}
}