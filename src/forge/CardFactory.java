package forge;


import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;

import javax.swing.JOptionPane;

import forge.error.ErrorViewer;
import forge.properties.ForgeProps;
import forge.properties.NewConstants;
import com.esotericsoftware.minlog.Log;

public class CardFactory implements NewConstants {
    // String cardname is the key, Card is the value
    private Map<String, Card> map       = new HashMap<String, Card>();
    
    private CardList          allCards  = new CardList();
    
    private HashSet<String>   removedCardList;
    private Card              blankCard = new Card();                 //new code
    // The Following "Cards" are used by the Whenever Keyword
    public Card               HumanNullCard = new Card();
    public Card               ComputerNullCard = new Card();
                                                                       
    public CardFactory(String filename) {
        this(new File(filename));
    }
    public CardList CopiedList = new CardList();
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
        
        HumanNullCard.setOwner(Constant.Player.Human);
        HumanNullCard.setController(Constant.Player.Human);
        ComputerNullCard.setOwner(Constant.Player.Computer);
        ComputerNullCard.setController(Constant.Player.Computer);
        
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
        
        //ReadCard read = new ReadCard(ForgeProps.getFile(CARDS));
        ReadCard read = new ReadCard(ForgeProps.getFile(CARDSFOLDER));
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
    
    final public Card dynamicCopyCard(Card in)
    {
    	if(in.getType().contains("Creature")) {
            Card card2 = new Card();
            card2 = CardFactory_Creatures.getCard(in, in.getName(), in.getOwner(), this);
            
            return card2;
        } else if(in.getType().contains("Aura")) {
            Card card2 = new Card();
            card2 = CardFactory_Auras.getCard(in, in.getName(), in.getOwner());
            
            return card2;
        } else if(in.getType().contains("Equipment")) {
            Card card2 = new Card();
            card2 = CardFactory_Equipment.getCard(in, in.getName(), in.getOwner());
            
            return card2;
        } else if(in.getType().contains("Planeswalker")) {
            Card card2 = new Card();
            card2 = CardFactory_Planeswalkers.getCard(in, in.getName(), in.getOwner());
            
            return card2;
        } else if(in.getType().contains("Land")) {
            Card card2 = new Card();
            card2 = CardFactory_Lands.getCard(in, in.getName(), in.getOwner());
            
            return card2;
        }
        else
        {
        	Card out = getCard(in.getName(), in.getOwner());
            out.setUniqueNumber(in.getUniqueNumber());
            return out;
        }
    }
    
    final public Card copyCard(Card in) {
        
    	Card out = getCard(in.getName(), in.getOwner());
        out.setUniqueNumber(in.getUniqueNumber());
        return out;
    	
    }
    
    final public Card copyCardintoNew(Card in) {
        
    	Card out = getCard(in.getName(), in.getOwner());
        PlayerZone Hplay = AllZone.getZone(Constant.Zone.Play, Constant.Player.Human);
        PlayerZone Cplay = AllZone.getZone(Constant.Zone.Play, Constant.Player.Computer);
        CardList all = AllZone.CardFactory.getAllCards();
        CardList tokens = new CardList(Hplay.getCards());
        tokens.add(new CardList(Cplay.getCards()));
        tokens = tokens.filter(new CardListFilter() {
        public boolean addCard(Card c) {
                  return c.isToken();
                }
            });
        all.add(tokens);
        all.add(CopiedList);
        int Unumber = 0;
        for(int i = 0; i < all.size(); i++) {
        if(all.get(i).getUniqueNumber() > Unumber) Unumber = all.get(i).getUniqueNumber();	
        }
        out.setUniqueNumber(Unumber + 4); // +4 because +1 didn't work lol.
        out.setCopiedSpell(true);
        CopiedList.add(out);
        return out;
    	
    }
    
    final void copySpellontoStack(Card Source, Card in, boolean CopyDetails) {
		Card c = AllZone.CardFactory.copyCardintoNew(in);
		SpellAbility[] sa = c.getSpellAbility();
		c.setController(Source.getController());
		if(CopyDetails == true) {
		c.addXManaCostPaid(in.getXManaCostPaid());
		c.addMultiKickerMagnitude(in.getMultiKickerMagnitude());
		if(in.isKicked()) c.setKicked(true);
		
		if(c.hasChoices()) {
			for(int i = 0; i < in.getChoices().size(); i++) {
				c.addSpellChoice(in.getChoice(i));
			}
			for(int i = 0; i < in.getChoiceTargets().size(); i++) {
				c.setSpellChoiceTarget(in.getChoiceTarget(i));
			}
		}
		}
		for(int i = 0; i < sa.length; i++) {
			if(in.getAbilityUsed() == i) {
				if(c.isKicked() && !sa[i].isKickerAbility())  {
			} else {
				if(in.getSpellAbility()[i].getTargetCard() != null)
					sa[i].setTargetCard(in.getSpellAbility()[i].getTargetCard());
				if(in.getSpellAbility()[i].getTargetPlayer() != null) {
				if(in.getSpellAbility()[i].getTargetPlayer().equals(Constant.Player.Human)
						|| (in.getSpellAbility()[i].getTargetPlayer().equals(Constant.Player.Computer))) 
					sa[i].setTargetPlayer(in.getSpellAbility()[i].getTargetPlayer());
				}
				if(Source.getController().equals(Constant.Player.Human)) AllZone.GameAction.playSpellAbility(sa[i]);
				else {
					if(sa[i].canPlayAI()) {
						ComputerUtil.playStackFree(sa[i]);
					}
				}
			}
			}	
}   	
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
    
    private final int hasKeyword(Card c, String k) {
        ArrayList<String> a = c.getKeyword();
        for(int i = 0; i < a.size(); i++)
            if(a.get(i).toString().startsWith(k)) return i;
        
        return -1;
    }
    
    private final int shouldManaAbility(Card c) {
        ArrayList<String> a = c.getIntrinsicKeyword();
        for(int i = 0; i < a.size(); i++)
            if(a.get(i).toString().contains(": add ") || a.get(i).toString().contains(": Add ") ) return i;
        return -1;
    }
    
    
    final private Card getCard2(final String cardName, final String owner) {
        //o should be Card object
        Object o = map.get(cardName);
        if(o == null) throw new RuntimeException("CardFactory : getCard() invalid card name - " + cardName);
        
        final Card card = copyStats(o);
        card.setOwner(owner);
        card.setController(owner);
        //may have to change the spell
        //this is so permanents like creatures and artifacts have a "default" spell
        if(!card.isLand()) card.addSpellAbility(new Spell_Permanent(card));
        
        //look for "Comes into play tapped." in the "no text" line
        //of card.txt and add the appropriate code to make that happen
        while(card.getKeyword().contains("Comes into play tapped."))
        {
        	card.removeIntrinsicKeyword("Comes into play tapped.");
        	card.addIntrinsicKeyword("CARDNAME enters the battlefield tapped.");
        }
        if(card.getKeyword().contains("CARDNAME enters the battlefield tapped.")) {
            card.addComesIntoPlayCommand(new Command() {
                private static final long serialVersionUID = 203335252453049234L;
                
                public void execute() {
                	System.out.println("Executing previous keyword");
                    card.tap();
                }
            });
        }//if "Comes into play tapped."
        if(card.getKeyword().contains("CARDNAME enters the battlefield tapped unless you control two or fewer other lands.")) {
        	card.addComesIntoPlayCommand(new Command() {
				private static final long serialVersionUID = 6436821515525468682L;

				public void execute() {
					CardList lands = AllZoneUtil.getPlayerLandsInPlay(card.getController());
					lands.remove(card);
                    if(!(lands.size() <= 2)) {
                    	card.tap();
                    }
                }
            });
        }
        if (hasKeyword(card, "CARDNAME enters the battlefield tapped unless you control a") != -1)
        {
        	int n = hasKeyword(card, "CARDNAME enters the battlefield tapped unless you control a");
        	String parse = card.getKeyword().get(n).toString();
        	
        	String splitString;
        	if (parse.contains(" or a "))
        		splitString = " or a ";
        	else
        		splitString = " or an ";
        	
        	final String types[] = parse.substring(60, parse.length() - 1).split(splitString);
        	
        	card.addComesIntoPlayCommand(new Command()
        	{
        		private static final long serialVersionUID = 403635232455049834L;
        		
        		public void execute()
        		{
        			PlayerZone pzICtrl = AllZone.getZone(Constant.Zone.Play, card.getOwner());
        			CardList clICtrl = new CardList(pzICtrl.getCards());
        			
        			boolean fnd = false;
        			
        			for (int i = 0; i < clICtrl.size(); i++)
        			{
        				Card c = clICtrl.get(i);
        				for (int j = 0; j < types.length; j++)
        					if (c.getType().contains(types[j].trim()))
        						fnd = true;
        			}
        			
        			if (!fnd)
        				card.tap();
        		}
        	});
        }
        if(hasKeyword(card,"spCounter") != -1) {
        	//System.out.println("Processing spCounter for card " + card.getName());
        	ComputerAI_counterSpells2.KeywordedCounterspells.add(card.getName());
        	String keyword = card.getKeyword().get(hasKeyword(card,"spCounter"));
        	if(keyword.contains("X"))
        	{
        		keyword = keyword.replace("X", card.getSVar("X"));
        	}
            card.removeIntrinsicKeyword(keyword);
        	
        	String[] splitkeyword = keyword.split(":");
        	
        	final String TargetType = splitkeyword[1];
        	final String TargetingRestrictions = splitkeyword[2];
        	final String Destination = splitkeyword[3];
        	final String ExtraActions = splitkeyword[4];
        	
        	final String[] SplitTargetingRestrictions = TargetingRestrictions.split(" ");
        	final String[] SplitExtraActions = ExtraActions.split(" ");
        	
        	SpellAbility spCounterAbility = new Spell(card)
        	{
        		private static final long serialVersionUID = 9763720166553L;
        		
        		@Override
        		public boolean canPlayAI()
        		{
        			System.out.println("AI is pondering us...");
        			return canPlay();
        		}
        		
        		@Override
        		public boolean canPlay()
        		{
        			if(AllZone.Stack.size() == 0)
        			{
        				return false;
        			}
        			
        			boolean fullResult = true;
        			SpellAbility sa = AllZone.Stack.peek();
        			Card tgtCard = sa.getSourceCard();
        			
        			if(TargetType.equals("Spell"))
        			{
        				if(sa.isAbility())
        				{
        					System.out.println(card.getName() + " can only counter spells, not abilities.");
        					return false;
        					
        				}
        			}
        			else if(TargetType.equals("Ability"))
        			{
        				if(sa.isSpell())
        				{
        					System.out.println(card.getName() + " can only counter abilities, not spells.");
        					return false;
        				}
        			}
        			else if(TargetType.equals("SpellOrAbility"))
        			{
        				//Do nothing. This block is only for clarity and enforcing parameters.
        			}
        			else
        			{
        				throw new IllegalArgumentException("Invalid target type for card " + card.getName());
        			}
        			
        			for(int i=0;i<SplitTargetingRestrictions.length;i++)
        			{
        				boolean subResult = false;
        				if(TargetingRestrictions.equals("None"))
        				{
        					return true;
        				}
        				
        				String RestrictionID = SplitTargetingRestrictions[i].substring(0,SplitTargetingRestrictions[i].indexOf('('));
        				String Parameters = SplitTargetingRestrictions[i].substring(SplitTargetingRestrictions[i].indexOf('(')+1);
        				Parameters = Parameters.substring(0,Parameters.length()-1);
        				
        				String[] SplitParameters = Parameters.split(",");
        				
        				System.out.println(card.getName() + " currently checking restriction '" + RestrictionID + "'");
        				if(RestrictionID.equals("Color"))
        				{
        					for(int p=0;p<SplitParameters.length;p++)
        					{
        						System.out.println("Parameter: " + SplitParameters[p]);
        						if(SplitParameters[p].startsWith("Non-"))
        						{
        							subResult |= !CardUtil.getColors(tgtCard).contains(SplitParameters[p].substring(4).toLowerCase()); 
        						}
        						else
        						{
        							subResult |= CardUtil.getColors(tgtCard).contains(SplitParameters[p].toLowerCase());
        						}
        					}
        				}
        				else if(RestrictionID.equals("Type"))
        				{
        					for(int p=0;p<SplitParameters.length;p++)
        					{
        						System.out.println("Parameter: " + SplitParameters[p]);
        						if(SplitParameters[p].startsWith("Non-"))
        						{
        							System.out.println(SplitParameters[p].substring(4));
        							subResult |= !tgtCard.getType().contains(SplitParameters[p].substring(4));
        						}
        						else
        						{
        							subResult |= tgtCard.getType().contains(SplitParameters[p]);
        						}
        					}
        				}
        				else if(RestrictionID.equals("CMC"))
        				{
        					String mode = SplitParameters[0];
        					int value = Integer.parseInt(SplitParameters[1]);
        					System.out.println(mode);
        					System.out.println(Integer.toString(value));
        					
        					if(mode.equals("<"))
        					{
        						subResult |= (CardUtil.getConvertedManaCost(tgtCard) < value);
        					}
        					else if(mode.equals(">"))
        					{
        						subResult |= (CardUtil.getConvertedManaCost(tgtCard) > value);
        					}
        					else if(mode.equals("=="))
        					{
        						subResult |= (CardUtil.getConvertedManaCost(tgtCard) == value);
        					}
        					else if(mode.equals("!="))
        					{
        						subResult |= (CardUtil.getConvertedManaCost(tgtCard) != value);
        					}
        					else if(mode.equals("<="))
        					{
        						subResult |= (CardUtil.getConvertedManaCost(tgtCard) <= value);
        					}
        					else if(mode.equals(">="))
        					{
        						subResult |= (CardUtil.getConvertedManaCost(tgtCard) >= value);
        					}        					
        					else
        					{
        						throw new IllegalArgumentException("spCounter: Invalid mode parameter to CMC restriction in card " + card.getName());
        					}
        				}
        				else if(RestrictionID.equals("Targets"))
        				{        		
							if(sa.getTargetCard() == null)
							{
								return false;
							}
        					for(int p=0;p<SplitParameters.length;p++)
        					{
        						System.out.println("Parameter: " + SplitParameters[p]);
        						if(SplitParameters[p].startsWith("My-")) //Targets my <type> permanent
        						{
        							if(sa.getTargetCard().getController() != card.getController())
        							{
        								return false;
        							}
        							if(SplitParameters[p].contains("Non-"))
        							{
        								subResult |= !sa.getTargetCard().getType().contains(SplitParameters[p].substring(7));
        							}
        							else
        							{
        								subResult |= (sa.getTargetCard().getType().contains(SplitParameters[p].substring(3)));
        							}
        						}
        						else if(SplitParameters[p].startsWith("Opp-")) //Targets opponent's <type> permanent
        						{
        							if(sa.getTargetCard().getController() == card.getController())
        							{
        								return false;
        							}
        							
        							if(SplitParameters[p].contains("Non-"))
        							{
        								subResult |= !(sa.getTargetCard().getType().contains(SplitParameters[p].substring(8)));
        							}
        							else
        							{
        								subResult |= (sa.getTargetCard().getType().contains(SplitParameters[p].substring(4)));
        							}
        						}
        						else
        						{
        							if(SplitParameters[p].contains("Non-"))
        							{
        								subResult |= !(sa.getTargetCard().getType().contains(SplitParameters[p].substring(4)));
        							}
        							else
        							{
        								subResult |= (sa.getTargetCard().getType().contains(SplitParameters[p]));
        							}
        						}
        					}
        				}
        				System.out.println("Sub: " + Boolean.toString(subResult));
        				fullResult &= subResult;
        			} //End Targeting parsing
        			System.out.println("Success: " + Boolean.toString(fullResult));
        			return fullResult;
        		}
        		
        		@Override
        		public void resolve()
        		{
        			System.out.println("Resolving " + card.getName());
        			SpellAbility sa = AllZone.Stack.pop();
        			
        			System.out.println("Send countered spell to " + Destination);
        			
        			if(Destination.equals("None") || TargetType.contains("Ability")) //For Ability-targeting counterspells
        			{
        				
        			}
        			else if(Destination.equals("Graveyard"))
        			{
        				AllZone.GameAction.moveToGraveyard(sa.getSourceCard());
        			}
        			else if(Destination.equals("Exile"))
        			{
        				AllZone.GameAction.exile(sa.getSourceCard());
        			}
        			else if(Destination.equals("Topdeck"))
        			{
        				AllZone.GameAction.moveToTopOfLibrary(sa.getSourceCard());
        			}
        			else if(Destination.equals("Hand"))
        			{
        				AllZone.GameAction.moveToHand(sa.getSourceCard());
        			}
        			else if(Destination.equals("BottomDeck"))
        			{
        				AllZone.GameAction.moveToBottomOfLibrary(sa.getSourceCard());
        			}
        			else if(Destination.equals("Shuffle"))
        			{
        				AllZone.GameAction.moveToBottomOfLibrary(sa.getSourceCard());
        				AllZone.GameAction.shuffle(sa.getSourceCard().getController());
        			}
        			else
        			{
        				throw new IllegalArgumentException("spCounter: Invalid Destination argument for card " + card.getName());
        			}
        			
        			for(int ea = 0;ea<SplitExtraActions.length;ea++)
        			{
        				
        				if(ExtraActions.equals("None"))
        				{
        					break;
        				}
        				String ActionID = SplitExtraActions[ea].substring(0,SplitExtraActions[ea].indexOf('('));
        				
        				String Target = "";
        				
        				String ActionParams = SplitExtraActions[ea].substring(SplitExtraActions[ea].indexOf('(')+1);
        				ActionParams = ActionParams.substring(0,ActionParams.length()-1);
        				
        				String[] SplitActionParams = ActionParams.split(",");
        				
        				System.out.println("Extra Action: " + ActionID);
        				System.out.println("Parameters: " + ActionParams);
        				
        				if(ActionID.startsWith("My-"))
        				{
        					ActionID = ActionID.substring(3);
        					Target = card.getController();
        				}
        				else if(ActionID.startsWith("Opp-"))
        				{
        					ActionID = ActionID.substring(4);
        					Target = AllZone.GameAction.getOpponent(card.getController());
        				}
        				else if(ActionID.startsWith("CC-"))
        				{
        					ActionID = ActionID.substring(3);
        					Target = sa.getSourceCard().getController();
        				}
        				
        				if(ActionID.equals("Draw"))
        				{
        					AllZone.GameAction.drawCards(Target, Integer.parseInt(SplitActionParams[0]));
        				}
        				else if(ActionID.equals("Discard"))
        				{
        					AllZone.GameAction.discard(Target, Integer.parseInt(SplitActionParams[0]), this);
        				}
        				else if(ActionID.equals("LoseLife"))
        				{
        					AllZone.GameAction.getPlayerLife(Target).subtractLife(Integer.parseInt(SplitActionParams[0]), card);
        				}
        				else if(ActionID.equals("GainLife"))
        				{
        					AllZone.GameAction.gainLife(Target, Integer.parseInt(SplitActionParams[0]));
        				}
        				else
        				{
        					throw new IllegalArgumentException("spCounter: Invalid Extra Action for card " + card.getName());
        				}
        			}
        		}
        	};
        	
        	card.clearSpellAbility();
        	card.addSpellAbility(spCounterAbility);
        } //spCounter
        
        

        // Support for using string variables to define Count$ for X or Y
        // Or just about any other String that a card object needs at any given time
// TODO: To Be Removed 
        while(hasKeyword(card, "SVar") != -1) {
            int n = hasKeyword(card, "SVar");
            if(n != -1) {
                String parse = card.getKeyword().get(n).toString();
                card.removeIntrinsicKeyword(parse);
                
                String k[] = parse.split(":", 3);
                
                if(k.length > 2) card.setSVar(k[1], k[2]);
            }
        }
        
        
        if (card.isType("World")) // Enforce the "World rule"
        {
        	Command intoPlay = new Command() {
            private static final long serialVersionUID = 6536398032388958127L;
                    
            	public void execute() {
                    CardList CardsinPlay = new CardList();
              		CardsinPlay.addAll(AllZone.Human_Play.getCards());
              		CardsinPlay.addAll(AllZone.Computer_Play.getCards());
              		CardsinPlay = CardsinPlay.getType("World");
              		CardsinPlay = CardsinPlay.filter(new CardListFilter() {
        				public boolean addCard(Card c) {
        					if(!c.equals(card)) return true;
        					return false;
        				}
        	      	});
              		for(int i = 0; i < CardsinPlay.size(); i++)
                        AllZone.GameAction.sacrificeDestroy(CardsinPlay.get(i));	 
                    }//execute()
                };//Command
                card.addComesIntoPlayCommand(intoPlay);
        }
        
        
        if (hasKeyword(card, "When CARDNAME enters the battlefield, return a land you control to its owner's hand.") != -1)
        {
        	int n = hasKeyword(card, "When CARDNAME enters the battlefield, return a land you control to its owner's hand.");
        	if (n!= -1)
        	{
        		final SpellAbility ability = new Ability(card, "0") {
                    @Override
                    public void resolve() {
                    	if(getSourceCard().getController().equals(Constant.Player.Computer))
                    		setTargetCard(card);//CardFactoryUtil.getRandomCard(new CardList(AllZone.Computer_Play.getCards()).getType("Land")));
                        Card c = getTargetCard();
                        PlayerZone hand = AllZone.getZone(Constant.Zone.Hand, c.getOwner());
                        
                        if(AllZone.GameAction.isCardInPlay(c)) {
                            AllZone.getZone(c).remove(c);
                            
                            if(!c.isToken()) {
                                Card newCard = AllZone.CardFactory.getCard(c.getName(), c.getOwner());
                                hand.add(newCard);
                            }
                        }
                    }
                };
                Command intoPlay = new Command() {
                    private static final long serialVersionUID = 2045940121508110423L;
                    
                    public void execute() {
                        PlayerZone play = AllZone.getZone(Constant.Zone.Play, card.getController());
                        CardList choice = new CardList(play.getCards()).getType("Land");
                        if (!choice.isEmpty()) {
                        	AllZone.InputControl.setInput(CardFactoryUtil.input_targetSpecific(ability, choice,
                                "Select a land you control.", false, false));
                        	ButtonUtil.disableAll();
                        }
                    }//execute()
                };//Command
                card.addComesIntoPlayCommand(intoPlay);
        	}
        }
        
        if (hasKeyword(card, "Multikicker") != -1)
        {
        	int n = hasKeyword(card, "Multikicker");
        	if (n!= -1)
        	{
        		String parse = card.getKeyword().get(n).toString();
        		String k[] = parse.split("kicker ");
        	
        		SpellAbility sa = card.getSpellAbility()[0];
        		sa.setIsMultiKicker(true);
        		sa.setMultiKickerManaCost(k[1]);
        	}
        }
        
        
        
        //Creatures with self-regenerate abilities
        //-1 means keyword "RegenerateMe" not found
        while(hasKeyword(card, "RegenerateMe") != -1) {
            int n = hasKeyword(card, "RegenerateMe");
            if(n != -1) {
                String parse = card.getKeyword().get(n).toString();
                card.removeIntrinsicKeyword(parse);
                
                String k[] = parse.split(":");
                final String manacost = k[1];
                
                final Command untilEOT = new Command() {
                    private static final long serialVersionUID = -7619842476705984912L;
                    
                    public void execute() {
                        card.setShield(0);
                        
                    }
                };
                
                final SpellAbility a1 = new Ability(card, manacost) {
                    @Override
                    public boolean canPlayAI() {
                        if(CardFactoryUtil.AI_isMainPhase()) {
                            if(CardFactoryUtil.AI_doesCreatureAttack(card)) {
                                //"Fuzzy logic" to determine if using a regenerate ability might be helpful because
                                //we can't wait to decide to play this ability during combat, like the human can
                                //weight[] is a set of probability percentages to be averaged later
                                int weight[] = new int[3];
                                
                                // cards with real keywords (flying, trample, etc) are probably more desireable
                                if(card.getKeyword().size() > 0) weight[0] = 75;
                                else weight[0] = 0;
                                
                                // if there are many cards in hand, then maybe it's not such a great idea to waste mana
                                CardList HandList = new CardList(AllZone.getZone(Constant.Zone.Hand,
                                        Constant.Player.Computer).getCards());
                                
                                if(HandList.size() >= 4) weight[1] = 25;
                                else weight[1] = 75;
                                
                                // compare the highest converted mana cost of cards in hand to the number of lands
                                // if there's spare mana, then regeneration might be viable
                                int hCMC = 0;
                                for(int i = 0; i < HandList.size(); i++)
                                    if(CardUtil.getConvertedManaCost(HandList.getCard(i).getManaCost()) > hCMC) hCMC = CardUtil.getConvertedManaCost(HandList.getCard(
                                            i).getManaCost());
                                
                                CardList LandList = new CardList(AllZone.getZone(Constant.Zone.Play,
                                        Constant.Player.Computer).getCards());
                                LandList = LandList.getType("Land");
                                
                                //most regenerate abilities cost 2 or less
                                if(hCMC + 2 >= LandList.size()) weight[2] = 50;
                                else weight[2] = 0;
                                
                                // ultimately, it's random fate that dictates if this was the right play
                                int aw = (weight[0] + weight[1] + weight[2]) / 3;
                                Random r = new Random();
                                if(r.nextInt(100) <= aw) return true;
                            }
                        }
                        return false;
                    }
                    
                    @Override
                    public void resolve() {
                        card.addShield();
                        AllZone.EndOfTurn.addUntil(untilEOT);
                    }
                }; //SpellAbility
                
                card.addSpellAbility(a1);
                
                String Desc = "";
                Desc = "Regenerate " + cardName;
                
                a1.setDescription(manacost + ": " + Desc);
                a1.setStackDescription(Desc);
                
                a1.setBeforePayMana(new Input_PayManaCost(a1));
            } //if (should RegenerateMe)
        } //while - card has more RegenerateMe - Jungle Troll has two Regenerate keywords
        
        if (hasKeyword(card, "spDiscard") != -1)
        {
        	int n = hasKeyword(card, "spDiscard");
        	
        	String parse = card.getKeyword().get(n).toString();
        	card.removeIntrinsicKeyword(parse);
        	
        	String k[] = parse.split(":");
        	final boolean Tgt = k[0].contains("Tgt");
        	final boolean Opp = k[0].contains("Opp");
        	
        	final String DiscardMethod = k[1];
        	
        	final int NumCards[] = {-1138};
        	final String NumCardsX[] = {"none"};
        	final String UnlessType[] = {"none"};
        	
        	if (k[2].length() > 1)
        	{
        		String kk[] = k[2].split("/");
        		if (kk[1].startsWith("UnlessDiscardType"))
        		{
        			String jk[] = kk[1].split("\\.");
        			UnlessType[0] = jk[1];
        			NumCards[0] = Integer.parseInt(kk[0]);
        		}
        	}
        	else if (k[2].matches("X"))
        	{
    			String xy = card.getSVar(k[2]);
    			if (xy.startsWith("Count$"))
    			{
    				String kk[] = xy.split("\\$");
    				NumCardsX[0] = kk[1];
    			}
        	}
        	else if (k[2].matches("[0-9]"))
        	{
        		NumCards[0] = Integer.parseInt(k[2]);
        	}
        	
        	
        	final String Drawback[] = {"none"};
        	final String spDesc[] = {"none"};
        	final String stDesc[] = {"none"};
        	
    		if (k[3].contains("Drawback$"))
    		{
    			String kk[] = k[3].split("\\$");
    			Drawback[0] = kk[1];
    			if (k.length > 4) spDesc[0] = k[4];
    			if (k.length > 5) stDesc[0] = k[5];
    		}
    		else
    		{
    			if (k.length > 3) spDesc[0] = k[3];
    			if (k.length > 4) stDesc[0] = k[4];
    		}
    		
        	SpellAbility spDiscard = new Spell(card)
        	{
        		private static final long serialVersionUID = 837472987492L;
        		
                private int getNumCards() {
                    if(NumCards[0] != -1138) return NumCards[0];
                    
                    if(!NumCardsX[0].equals("none")) return CardFactoryUtil.xCount(card, NumCardsX[0]);
                    
                    return 0;
                }
                
                public boolean canPlayAI()
                {
                	int nCards = getNumCards();
                	
                	PlayerZone pzH = AllZone.getZone(Constant.Zone.Hand, Constant.Player.Human);
                	int numHHand = pzH.size();
                	
                	if (numHHand >= nCards)
                	{
                		if (Tgt)
                			setTargetPlayer(Constant.Player.Human);
                		
                		return true;
                	}
                		
                	return false;
                }
                public void resolve()
                {
                	int nCards = getNumCards();
                	String discardingPlayer = "";
                	
                	if (Tgt)
                		discardingPlayer = getTargetPlayer();
                	else if (Opp)
                		discardingPlayer = AllZone.GameAction.getOpponent(card.getController());
                	
                	if (DiscardMethod.equals("OppChoose") || DiscardMethod.equals("TgtChoose"))
                	{
                		//String opp = AllZone.GameAction.getOpponent(card.getController());
                		
                		if (!UnlessType[0].equals("none"))
                			AllZone.GameAction.discardUnless(discardingPlayer, nCards, UnlessType[0], this);
                		else
                			AllZone.GameAction.discard(discardingPlayer, nCards, this);
                	}

                	else if (DiscardMethod.equals("AtRandom"))
                	{
                		AllZone.GameAction.discardRandom(discardingPlayer, nCards, this);
                	}

                	else if (DiscardMethod.equals("Hand"))
                	{
                		AllZone.GameAction.discardHand(discardingPlayer, this);
                	}
                	else if (DiscardMethod.startsWith("RevealYouChoose"))
                	{
                    	PlayerZone pzH = AllZone.getZone(Constant.Zone.Hand, discardingPlayer);
                    	if (pzH.size() != 0)
                    	{
                		    CardList dPHand = new CardList(pzH.getCards());
                		    CardList dPChHand = new CardList(dPHand.toArray());
                		    
                		    if (DiscardMethod.contains("/"))	// Restrict card choices
                		    {
                		    	int dot = DiscardMethod.indexOf("/");
                		    	String dV = DiscardMethod.substring(dot + 1);
                		    	String dValid[] = dV.split(",");
                		    	
                		    	dPChHand = dPHand.getValidCards(dValid);
                		    }
                		    
	                		if (card.getController().equals(Constant.Player.Computer))
	                		{
	                			//AI
	                			for (int i=0; i<nCards; i++)
	                			{
		                			if (dPChHand.size() > 0)
		                			{
		                				CardList dChoices = new CardList();
			                				                			
			                			if (DiscardMethod.contains("Creature") && !DiscardMethod.contains("nonCreature")) {
			                				Card c = CardFactoryUtil.AI_getBestCreature(dPChHand);
			                				if (c!=null)
			                					dChoices.add(CardFactoryUtil.AI_getBestCreature(dPChHand));
			                			}
			                				
			                			
		                				CardListUtil.sortByTextLen(dPChHand);
		                				dChoices.add(dPChHand.get(0));
		                				
		                				CardListUtil.sortCMC(dPChHand);
		                				dChoices.add(dPChHand.get(0));
			                			
			                			Card dC = dChoices.get(CardUtil.getRandomIndex(dChoices));
		                				dPChHand.remove(dC);
		                				
		                				CardList dCs = new CardList();
		                				dCs.add(dC);
		                				AllZone.Display.getChoiceOptional("Computer has chosen", dCs.toArray());
		                				
			                			AllZone.GameAction.discard(dC, this);
		                			}
	                			}
	                		}
	                		else
	                		{
	                			//human
	                			AllZone.Display.getChoiceOptional("Revealed computer hand", dPHand.toArray());
	                			
	                			for (int i=0; i<nCards; i++)
	                			{
	                				if (dPChHand.size() > 0)
	                				{
		                				Card dC = AllZone.Display.getChoice("Choose a card to be discarded", dPChHand.toArray());
		                				
		                				dPChHand.remove(dC);
		                				AllZone.GameAction.discard(dC, this);
	                				}
	                			}
	                		}
                    	}
                	}

                	if (!Drawback[0].equals("none"))
                	{
                		CardFactoryUtil.doDrawBack(Drawback[0], nCards, card.getController(), AllZone.GameAction.getOpponent(card.getController()), discardingPlayer, card, card, this);
                	}
                }
        	};
        	
            if (Tgt)
                spDiscard.setBeforePayMana(CardFactoryUtil.input_targetPlayer(spDiscard));
            
            spDiscard.setDescription(spDesc[0]);
            spDiscard.setStackDescription(stDesc[0]);
            
            card.clearSpellAbility();
            card.addSpellAbility(spDiscard);
            
            String bbCost = card.getSVar("Buyback");
            if (!bbCost.equals(""))
            {
               SpellAbility bbDiscard = spDiscard.copy();
               bbDiscard.setManaCost(CardUtil.addManaCosts(card.getManaCost(), bbCost));
               StringBuilder sb = new StringBuilder();
               sb.append("Buyback ").append(bbCost).append(" (You may pay an additional ").append(bbCost);
               sb.append(" as you cast this spell. If you do, put this card into your hand as it resolves.)");
               bbDiscard.setDescription(sb.toString());
               // bbDiscard.setDescription("Buyback " + bbCost + " (You may pay an additional " + bbCost + " as you cast this spell. If you do, put this card into your hand as it resolves.)");
               bbDiscard.setIsBuyBackAbility(true);
               
               if (Tgt)
                   bbDiscard.setBeforePayMana(CardFactoryUtil.input_targetPlayer(bbDiscard));
               
               card.addSpellAbility(bbDiscard);
            }

        }//spDiscardTgt
        
        if (hasKeyword(card, "spAllPump") != -1)
        {
        	int n = hasKeyword(card, "spAllPump");
        	
        	String parse = card.getKeyword().get(n).toString();
        	card.removeIntrinsicKeyword(parse);
        	
        	String k[] = parse.split(":");
        	
        	final String Scope[] = k[1].split("/");
        	
        	final int NumAttack[] = {-1138};
        	final String AttackX[] = {"none"};
        	final int NumDefense[] = {-1138};
        	final String DefenseX[] = {"none"};
        	final String Keyword[] = {"none"};
        	
        	String ptk[] = k[2].split("/");
        	
        	if (ptk.length == 1)
        		Keyword[0] = ptk[0];
        	
        	if (ptk.length >= 2)
        	{
        		if (ptk[0].matches("[\\+\\-][XY]"))
        		{
        			String xy = card.getSVar(ptk[0].replaceAll("[\\+\\-]", ""));
        			if (xy.startsWith("Count$"))
        			{
        				String kk[] = xy.split("\\$");
        				AttackX[0] = kk[1];
        				
        				if (ptk[0].contains("-"))
        				{
        					if (AttackX[0].contains("/"))
        						AttackX[0] = AttackX[0].replace("/", "/Negative");
        					else
        						AttackX[0] += "/Negative";
        				}
        				
        			}
        		}
        		else if (ptk[0].matches("[\\+\\-][0-9]"))
        			NumAttack[0] = Integer.parseInt(ptk[0].replace("+", ""));
        		
        		if (ptk[1].matches("[\\+\\-][XY]"))
        		{
        			String xy = card.getSVar(ptk[1].replaceAll("[\\+\\-]", ""));
        			if (xy.startsWith("Count$"))
        			{
        				String kk[] = xy.split("\\$");
        				DefenseX[0] = kk[1];
        				
        				if (ptk[1].contains("-"))
        				{
        					if (DefenseX[0].contains("/"))
        						DefenseX[0] = DefenseX[0].replace("/", "/Negative");
        					else
        						DefenseX[0] += "/Negative";
        				}
        				
        			}
        		}
        		else if (ptk[1].matches("[\\+\\-][0-9]"))
        			NumDefense[0] = Integer.parseInt(ptk[1].replace("+", ""));
        	}
        	
        	if (ptk.length == 3)
        		Keyword[0] = ptk[2];
        	
        	final String DrawBack[] = {"none"};
        	final String spDesc[] = {"none"};
        	final String stDesc[] = {"none"};
        	
        	if (k.length > 3)
        	{
        		if (k[3].contains("Drawback$"))
        		{
        			String kk[] = k[3].split("\\$");
        			DrawBack[0] = kk[1];
        			if (k.length > 4) spDesc[0] = k[4];
        			if (k.length > 5) stDesc[0] = k[5];
        		}
        		else
        		{
        			if (k.length > 3) spDesc[0] = k[3];
        			if (k.length > 4) stDesc[0] = k[4];
        		}
        	}
        	
        	SpellAbility spAllPump = new Spell(card)
        	{
        		private static final long serialVersionUID = 837472987492L;
        		
                private int getNumAttack() {
                    if(NumAttack[0] != -1138) return NumAttack[0];
                    
                    if(!AttackX[0].equals("none")) return CardFactoryUtil.xCount(card, AttackX[0]);
                    
                    return 0;
                }
                
                private int getNumDefense() {
                    if(NumDefense[0] != -1138) return NumDefense[0];
                    
                    if(!DefenseX[0].equals("none")) return CardFactoryUtil.xCount(card, DefenseX[0]);
                    
                    return 0;
                }
                
                private int getNumKeyword()
                {
                	if (!Keyword[0].equals("none"))
                		return Keyword[0].split(" & ").length;
                	else return 0;
                }
                
                private CardList getScopeList()
                {
                	CardList l = new CardList();
                	
                	if (Scope[0].contains("YouCtrl"))
                		l.addAll(AllZone.getZone(Constant.Zone.Play, card.getController()).getCards());
                	
                	if (Scope[0].contains("All")) {
                		l.addAll(AllZone.getZone(Constant.Zone.Play, Constant.Player.Human).getCards());
                		l.addAll(AllZone.getZone(Constant.Zone.Play, Constant.Player.Computer).getCards());
                	}
                	
                	String fc[] = {"Creature"};
                	l = l.getValidCards(fc);
                	
                	if (Scope.length > 1)
                	{
                		String v = Scope[1]; 
                		if (v.length() > 0)
                			l = l.getValidCards(v.split(","));
                	}
                	
                	return l;
                }

                public boolean canPlayAI()
                {
                	//Log.debug("spAllPump", "Phase - " + AllZone.Phase.getPhase());
                	String curPhase = AllZone.Phase.getPhase();
                	if (curPhase.equals(Constant.Phase.Main2)) 
                		return false;
                
                	CardList sl = getScopeList();
                	int NumScope = sl.size();
                	
                	int defense = getNumDefense();
                	int attack = getNumAttack();
                	int key = getNumKeyword();
                	int th = (attack + defense + key) / 2; // Benefit Threshold
                	
                	if (NumScope > th) // have enough creatures in play
                	{
                		Combat c = ComputerUtil.getAttackers();
                		if (c.getAttackers().length >= th) // have enough creatures that will attack
                		{
                			int ndead = 0;
                			for (int i=0; i<sl.size(); i++) // check to see if this will kill any creatures
                				if ((sl.get(i).getNetDefense() + defense) < 1)
                					ndead++;
                			if (!(ndead > (sl.size() / 2))) // don't kill more than half of the creatures
                					return true;
                		}
                	}
                	
                	return false;
                }
                
                public void resolve()
                {
                	final int attack = getNumAttack();
                	final int defense = getNumDefense();
                	
                	final CardList sl = getScopeList();
                	
                	Log.debug("spAllPump", "Phase - " + AllZone.Phase.getPhase());
                	
                	final Command untilEOT = new Command()
                	{
                		private static final long serialVersionUID = 92848209484928L;
                		
                		public void execute()
                		{
                			for (int i=0; i<sl.size(); i++)
                			{
                				Card c = sl.get(i);
                				if (AllZone.GameAction.isCardInPlay(c))
                				{
                					c.addTempAttackBoost(-attack);
                					c.addTempDefenseBoost(-defense);
                					
                					if (!Keyword[0].equals("none"))
                					{
                						String kws[] = Keyword[0].split(" & ");
                						for (int j=0; j<kws.length; j++)
                							c.removeExtrinsicKeyword(kws[j]);
                					}
                				}
                			}
                		}
                	}; // untilEOT command
                	
                	for (int i=0; i<sl.size(); i++)
                	{
                		Card c = sl.get(i);
                		
                		if (AllZone.GameAction.isCardInPlay(c))
                		{
                			c.addTempAttackBoost(attack);
                			c.addTempDefenseBoost(defense);
                			
                			if (!Keyword[0].equals("none"))
                			{
                				String kws[] = Keyword[0].split(" & ");
                				for (int j=0; j<kws.length; j++)
                					c.addExtrinsicKeyword(kws[j]);
                			}
                		}
                	}
                	
                	AllZone.EndOfTurn.addUntil(untilEOT);
                	
                	if (!DrawBack[0].equals("none"))
                		CardFactoryUtil.doDrawBack(DrawBack[0], 0, card.getController(), AllZone.GameAction.getOpponent(card.getController()), card.getController(), card, card, this);
                } // resolve
        	}; // spAllPump
        	
        	spAllPump.setDescription(spDesc[0]);
        	spAllPump.setStackDescription(stDesc[0]);
        	
        	card.clearSpellAbility();
        	card.addSpellAbility(spAllPump);
        	
        	card.setSVar("PlayMain1", "TRUE");
        	
            String bbCost = card.getSVar("Buyback");
            if (!bbCost.equals(""))
            {
                SpellAbility bbAllPump = spAllPump.copy();
                bbAllPump.setManaCost(CardUtil.addManaCosts(card.getManaCost(), bbCost));
                
                StringBuilder sb = new StringBuilder();
                sb.append("Buyback ").append(bbCost).append(" (You may pay an additional ").append(bbCost);
                sb.append(" as you cast this spell. If you do, put this card into your hand as it resolves.)");
                bbAllPump.setDescription(sb.toString());
                // bbAllPump.setDescription("Buyback " + bbCost + "(You may pay an additional " + bbCost + " as you cast this spell. If you do, put this card into your hand as it resolves.)");
                bbAllPump.setIsBuyBackAbility(true);
                               
                card.addSpellAbility(bbAllPump);
             }
         }//spAllPump
        
        while (hasKeyword(card, "abAllPump") != -1)
        {
        	int n = hasKeyword(card, "abAllPump");
        	if (n != -1)
        	{
        		String parse = card.getKeyword().get(n).toString();
        		card.removeIntrinsicKeyword(parse);
        		
        		String k[] = parse.split(":");
        		String tmpCost[] =  k[0].replace("abAllPump", "").split(" ", 2);
        		
        		final Target abTgt = new Target(tmpCost[0]);
        		final Ability_Cost abCost = new Ability_Cost(tmpCost[1], card.getName(), true);        		
        		
        		final String Scope[] = k[1].split("/");
        		
            	final int NumAttack[] = {-1138};
            	final String AttackX[] = {"none"};
            	final int NumDefense[] = {-1138};
            	final String DefenseX[] = {"none"};
            	final String Keyword[] = {"none"};
            	
            	String ptk[] = k[2].split("/");
            	
            	if (ptk.length == 1)
            		Keyword[0] = ptk[0];
            	
            	if (ptk.length >= 2)
            	{
            		if (ptk[0].matches("[\\+\\-][XY]"))
            		{
            			String xy = card.getSVar(ptk[0].replaceAll("[\\+\\-]", ""));
            			if (xy.startsWith("Count$"))
            			{
            				String kk[] = xy.split("\\$");
            				AttackX[0] = kk[1];
            				
            				if (ptk[0].contains("-"))
            				{
            					if (AttackX[0].contains("/"))
            						AttackX[0] = AttackX[0].replace("/", "/Negative");
            					else
            						AttackX[0] += "/Negative";
            				}
            				
            			}
            		}
            		else if (ptk[0].matches("[\\+\\-][0-9]"))
            			NumAttack[0] = Integer.parseInt(ptk[0].replace("+", ""));
            		
            		if (ptk[1].matches("[\\+\\-][XY]"))
            		{
            			String xy = card.getSVar(ptk[1].replaceAll("[\\+\\-]", ""));
            			if (xy.startsWith("Count$"))
            			{
            				String kk[] = xy.split("\\$");
            				DefenseX[0] = kk[1];
            				
            				if (ptk[1].contains("-"))
            				{
            					if (DefenseX[0].contains("/"))
            						DefenseX[0] = DefenseX[0].replace("/", "/Negative");
            					else
            						DefenseX[0] += "/Negative";
            				}
            				
            			}
            		}
            		else if (ptk[1].matches("[\\+\\-][0-9]"))
            			NumDefense[0] = Integer.parseInt(ptk[1].replace("+", ""));
            	}
            	
            	if (ptk.length == 3)
            		Keyword[0] = ptk[2];
            	
            	final String DrawBack[] = {"none"};
            	final String spDesc[] = {"none"};
            	final String stDesc[] = {"none"};
            	
            	if (k.length > 3)
            	{
            		if (k[3].contains("Drawback$"))
            		{
            			String kk[] = k[3].split("\\$");
            			DrawBack[0] = kk[1];
            			if (k.length > 4) spDesc[0] = k[4];
            			if (k.length > 5) stDesc[0] = k[5];
            		}
            		else
            		{
            			if (k.length > 3) spDesc[0] = k[3];
            			if (k.length > 4) stDesc[0] = k[4];
            		}
            	}
            	
            	SpellAbility abAllPump = new Ability_Activated(card, abCost, abTgt)
            	{
            		private static final long serialVersionUID = 7783282947592874L;
            		
            		private int getNumAttack() {
            			if(NumAttack[0] != -1138) return NumAttack[0];
            			
            			if(!AttackX[0].equals("none")) return CardFactoryUtil.xCount(card, AttackX[0]);
            			
            			return 0;
            		}
            		
            		private int getNumDefense() {
            			if(NumDefense[0] != -1138) return NumDefense[0];
            			
            			if(!DefenseX[0].equals("none")) return CardFactoryUtil.xCount(card, DefenseX[0]);
            			
            			return 0;
            		}
            		
            		private int getNumKeyword()
            		{
            			if (!Keyword[0].equals("none"))
            				return Keyword[0].split(" & ").length;
            			else return 0;
            		}
            		
            		private CardList getScopeList()
            		{
            			CardList l = new CardList();
            			
            			if (Scope[0].contains("YouCtrl"))
            				l.addAll(AllZone.getZone(Constant.Zone.Play, card.getController()).getCards());
            			
            			if (Scope[0].contains("All")) {
            				l.addAll(AllZone.getZone(Constant.Zone.Play, Constant.Player.Human).getCards());
            				l.addAll(AllZone.getZone(Constant.Zone.Play, Constant.Player.Computer).getCards());
            			}
            			
            			String fc[] = {"Creature"};
            			l = l.getValidCards(fc);
            			
            			if (Scope.length > 1)
            			{
            				String v = Scope[1]; 
            				if (v.length() > 0)
            					l = l.getValidCards(v.split(","));
            			}
            			
            			return l;
            		}

                    @Override
                    public boolean canPlay() {
                    	if (abCost.getTap() && (card.isTapped() || card.isSick()))
                    		return false;
                    	
                        return (CardFactoryUtil.canUseAbility(card) && super.canPlay());
                    }
            		
                    @Override
            		public boolean canPlayAI()
            		{
            			//Log.debug("spAllPump", "Phase - " + AllZone.Phase.getPhase());
            			String curPhase = AllZone.Phase.getPhase();
            			if (curPhase.equals(Constant.Phase.Main2)) 
            				return false;
            				
            			// temporarily disabled until AI is improved
                    	if (abCost.getSacCost()) return false;	
                    	if (abCost.getSubCounter()) return false;
                    	if (abCost.getLifeCost())	 return false;
            			
            			if (abCost.getTap() && (card.isTapped() || card.isSick()))
            				return false;
            			
            				
            			CardList sl = getScopeList();
            			int NumScope = sl.size();
            			
            			int defense = getNumDefense();
            			int attack = getNumAttack();
            			int key = getNumKeyword();
            			int th = (attack + defense + key) / 2; // Benefit Threshold
            			
            			if (NumScope > th) // have enough creatures in play
            			{
            				Combat c = ComputerUtil.getAttackers();
            				if (c.getAttackers().length >= th) // have enough creatures that will attack
            				{
            					int ndead = 0;
            					for (int i=0; i<sl.size(); i++) // check to see if this will kill any creatures
            						if ((sl.get(i).getNetDefense() + defense) < 1)
            							ndead++;
            					if (!(ndead > (sl.size() / 2))) // don't kill more than half of the creatures
            							return true;
            				}
            			}
            			
            			return false;
            		}
            		
                    @Override
            		public void resolve()
            		{
            			final int attack = getNumAttack();
            			final int defense = getNumDefense();
            			
            			final CardList sl = getScopeList();
            			
            			//Log.debug("spAllPump", "Phase - " + AllZone.Phase.getPhase());
            			
            			final Command untilEOT = new Command()
            			{
            				private static final long serialVersionUID = 92848209484928L;
            				
            				public void execute()
            				{
            					for (int i=0; i<sl.size(); i++)
            					{
            						Card c = sl.get(i);
            						if (AllZone.GameAction.isCardInPlay(c))
            						{
            							c.addTempAttackBoost(-attack);
            							c.addTempDefenseBoost(-defense);
            							
            							if (!Keyword[0].equals("none"))
            							{
            								String kws[] = Keyword[0].split(" & ");
            								for (int j=0; j<kws.length; j++)
            									c.removeExtrinsicKeyword(kws[j]);
            							}
            						}
            					}
            				}
            			}; // untilEOT command
            			
            			for (int i=0; i<sl.size(); i++)
            			{
            				Card c = sl.get(i);
            				
            				if (AllZone.GameAction.isCardInPlay(c))
            				{
            					c.addTempAttackBoost(attack);
            					c.addTempDefenseBoost(defense);
            					
            					if (!Keyword[0].equals("none"))
            					{
            						String kws[] = Keyword[0].split(" & ");
            						for (int j=0; j<kws.length; j++)
            							c.addExtrinsicKeyword(kws[j]);
            					}
            				}
            			}
            			
            			AllZone.EndOfTurn.addUntil(untilEOT);
            			
            			if (!DrawBack[0].equals("none"))
            				CardFactoryUtil.doDrawBack(DrawBack[0], 0, card.getController(), AllZone.GameAction.getOpponent(card.getController()), card.getController(), card, card, this);
            		} // resolve
            	}; // abAllPump

            	abAllPump.setDescription(abCost.toString() + spDesc[0]);
            	abAllPump.setStackDescription(stDesc[0]);

                card.addSpellAbility(abAllPump);
        	}
        }
        
        while(hasKeyword(card, "abPump") != -1) {
            int n = hasKeyword(card, "abPump");
            if(n != -1) {
                String parse = card.getKeyword().get(n).toString();
                card.removeIntrinsicKeyword(parse);
                
                String k[] = parse.split(":");
        		String tmp =  k[0].replace("abPump", "");
        		
        		String[] tmpCost = tmp.split(" ", 2);
        		
        		final boolean bPumpEquipped = (tmpCost[0].equals("Equipped"));
        		
        		final Target abTgt = new Target(tmpCost[0]);        			
        		final Ability_Cost abCost = new Ability_Cost(tmpCost[1], card.getName(), true);
                
                final int NumAttack[] = {-1138};
                final String AttackX[] = {"none"};
                final int NumDefense[] = {-1138};
                final String DefenseX[] = {"none"};
                final String Keyword[] = {"none"};
                
                String ptk[] = k[1].split("/");
                
                if(ptk.length == 1) // keyword only
                Keyword[0] = ptk[0];
                
                if(ptk.length >= 2) // power/toughness
                {
                    if(ptk[0].matches("[\\+\\-][XY]")) {
                        String xy = card.getSVar(ptk[0].replaceAll("[\\+\\-]", ""));
                        if(xy.startsWith("Count$")) {
                            String kk[] = xy.split("\\$");
                            AttackX[0] = kk[1];
                            
                            if(ptk[0].contains("-")) // handle "-X" or "-Y"
                            if(AttackX[0].contains("/")) // already contains math element
                            AttackX[0] = AttackX[0].replace("/", "/Negative"); // insert into existing math element
                            else AttackX[0] += "/Negative"; // add math element
                        }
                    } else if(ptk[0].matches("[\\+\\-][0-9]")) 
                    	NumAttack[0] = Integer.parseInt(ptk[0].replace("+", ""));
                    
                    if(ptk[1].matches("[\\+\\-][XY]")) {
                        String xy = card.getSVar(ptk[1].replaceAll("[\\+\\-]", ""));
                        if(xy.startsWith("Count$")) {
                            String kk[] = xy.split("\\$");
                            DefenseX[0] = kk[1];
                            
                            if(ptk[1].contains("-")) //handle "-X" or "-Y"
                            if(DefenseX[0].contains("/")) // already contains math element
                            DefenseX[0] = DefenseX[0].replace("/", "/Negative"); // insert into existing math element
                            else DefenseX[0] += "/Negative"; // add math element
                        }
                    } else if(ptk[1].matches("[\\+\\-][0-9]")) NumDefense[0] = Integer.parseInt(ptk[1].replace(
                            "+", ""));
                }
                
                if(ptk.length == 3) // power/toughness/keyword
                Keyword[0] = ptk[2];
                
                String dK = Keyword[0];
                if (Keyword[0].contains(" & "))
                {
                	int amp = Keyword[0].lastIndexOf("&");
                	StringBuffer sbk = new StringBuffer(Keyword[0]);
                	sbk.replace(amp, amp + 1, "and");
                	dK = sbk.toString();
                	dK = dK.replace(" & ", ", ");
                }
                
                final String DrawBack[] = {"none"};
                final String spDesc[] = {"none"};
                final String stDesc[] = {"none"};
                String d = "none";
                StringBuilder sbD = new StringBuilder();
                
                if(abTgt.doesTarget()) 
                	sbD.append("Target creature");
                else if (bPumpEquipped)
                	sbD.append("Equipped creature");
                else 
                    sbD.append(cardName);
                
                if((AttackX[0].equals("none") && !(NumAttack[0] == -1138))
                        && (DefenseX[0].equals("none") && !(NumDefense[0] == -1138)) && Keyword[0].equals("none")) {
                    // pt boost
                    sbD.append(" gets ");  
                    
                    if(NumAttack[0] > 0 || (NumAttack[0] == 0 && NumDefense[0] > 0)) // +0/+1
                    	sbD.append("+");
                    else if(NumAttack[0] < 0 || (NumAttack[0] == 0 && NumDefense[0] < 0)) // -0/-1
                    	sbD.append("-");
                    
                    sbD.append(Math.abs(NumAttack[0]) + "/");
                    
                    if(NumDefense[0] > 0 || (NumDefense[0] == 0 && NumAttack[0] > 0)) // +1/+0
                    sbD.append("+");
                    else if(NumDefense[0] < 0 || (NumDefense[0] == 0 && NumAttack[0] < 0)) // -1/-0
                    sbD.append("-");
                    
                    sbD.append(Math.abs(NumDefense[0]));
                    sbD.append(" until end of turn.");
                }
                if((AttackX[0].equals("none") && NumAttack[0] == -1138)
                        && (DefenseX[0].equals("none") && NumDefense[0] == -1138) && !Keyword[0].equals("none")) {
                    // k boost
                	sbD.append(" gains ");
                    
                    sbD.append(dK);
                    sbD.append(" until end of turn.");
                }
                if((AttackX[0].equals("none") && !(NumAttack[0] == -1138))
                        && (DefenseX[0].equals("none") && !(NumDefense[0] == -1138)) && !Keyword[0].equals("none")) {
                    // ptk boost
                    sbD.append(" gets ");
                    
                    if(NumAttack[0] > 0 || (NumAttack[0] == 0 && NumDefense[0] > 0)) // +0/+1
                    sbD.append("+");
                    else if(NumAttack[0] < 0 || (NumAttack[0] == 0 && NumDefense[0] < 0)) // -0/-1
                    sbD.append("-");
                    
                    sbD.append(Math.abs(NumAttack[0]) + "/");
                    
                    if(NumDefense[0] > 0 || (NumDefense[0] == 0 && NumAttack[0] > 0)) // +1/+0
                    sbD.append("+");
                    else if(NumDefense[0] < 0 || (NumDefense[0] == 0 && NumAttack[0] < 0)) // -1/-0
                    sbD.append("-");
                    
                    sbD.append(Math.abs(NumDefense[0]));
                    sbD.append(" and gains ");
                    sbD.append(dK);
                    sbD.append(" until end of turn.");
                }
                //if (!sbD.toString().isEmpty())
                if(sbD.toString().trim().length() != 0) d = sbD.toString();
                
                if(k.length > 2) {
                    if(k[2].contains("Drawback$")) {
                        String kk[] = k[2].split("\\$");
                        DrawBack[0] = kk[1];
                        if(k.length > 3) d = k[3];
                    } else if(k.length > 2) d = k[2];
                }
                
                if(!d.equals("none")) {
                    spDesc[0] = abCost.toString() + d;
                    stDesc[0] = d;
                }
                
                // start ability here:
                final SpellAbility ability = new Ability_Activated(card, abCost, abTgt) {
                    private static final long serialVersionUID = -1118592153328758083L;
                    
                    private int               defense;
                    private String            keyword;
                    
                    private int getNumAttack() {
                        if(NumAttack[0] != -1138) return NumAttack[0];
                        
                        if(!AttackX[0].equals("none")) return CardFactoryUtil.xCount(card, AttackX[0]);
                        
                        return 0;
                    }
                    
                    private int getNumDefense() {
                        if(NumDefense[0] != -1138) return NumDefense[0];
                        
                        if(!DefenseX[0].equals("none")) return CardFactoryUtil.xCount(card, DefenseX[0]);
                        
                        return 0;
                    }
                    
                    @Override
                    public boolean canPlayAI() {
                    	// temporarily disabled until AI is improved
                    	if (abCost.getSacCost()) return false;	
                    	if (abCost.getLifeCost())	 return false;
                    	if (abCost.getSubCounter()){
                    		// instead of never removing counters, we will have a random possibility of failure.
                    		// all the other tests still need to pass if a counter will be removed
                    		Counters count = abCost.getCounterType();
                    		double chance = .66;
                    		if (count.equals("P1P1")){	// 10% chance to remove +1/+1 to pump
                    			chance = .1;
                    		}
                    		else if (count.equals("CHARGE")){ // 50% chance to remove +1/+1 to pump
                    			chance = .5;
                    		}
                            Random r = new Random();
                            if(r.nextFloat() > chance)
                            	return false;
                    	}
                    	if (bPumpEquipped && card.getEquippingCard() == null) return false;
                    	
                    	if (!ComputerUtil.canPayCost(this))
                    		return false;
                    	
                        defense = getNumDefense();
                        keyword = Keyword[0];
                        
                        if(AllZone.Phase.getPhase().equals(Constant.Phase.Main2)) return false;
                        
                        if(!abTgt.doesTarget()) {
                        	Card creature;
                            if (bPumpEquipped)
                            	creature = card.getEquippingCard();
                            else 
                            	creature = card;
                            
                            if((creature.getNetDefense() + defense > 0) && (!creature.getKeyword().contains(keyword))) {
                            	if(creature.hasSickness() && keyword.contains("Haste")) 
                            		return true;
                            	else if (creature.hasSickness() ^ keyword.contains("Haste"))
                                    return false;
                            	else {
	                                Random r = new Random();
	                                if(r.nextFloat() <= Math.pow(.6667, card.getAbilityUsed())) 
	                                	return CardFactoryUtil.AI_doesCreatureAttack(creature);
	                            }
                            }
                        }
                        
                        CardList list = getCreatures();
                        if(!list.isEmpty()) {
                            boolean goodt = false;
                            Card t = new Card();
                            while(goodt == false && !list.isEmpty()) // loop until we find a target that is best and won't die when targeted or until no more creatures
                            {
                                t = CardFactoryUtil.AI_getBestCreature(list);
                                if((t.getNetDefense() + defense) > 0) // handle negative defense pumps
                                goodt = true;
                                else list.remove(t);
                            }
                            if(goodt == true) {
                                Random r = new Random();
                                if(r.nextFloat() <= Math.pow(.6667, card.getAbilityUsed())) {
                                    setTargetCard(t);
                                    return true;
                                }
                            }
                        }
                        
                        return false;
                    }
                    
                    @Override
                    public boolean canPlay() {
                    	Cost_Payment pay = new Cost_Payment(abCost, this);
                        return (pay.canPayAdditionalCosts() && CardFactoryUtil.canUseAbility(card) && super.canPlay());
                    }
                    
                    private CardList getCreatures() {
                        CardList list = new CardList(AllZone.Computer_Play.getCards());
                        list = list.filter(new CardListFilter() {
                            public boolean addCard(Card c) {
                                if(c.isCreature()) {
                                    if(c.hasSickness() && keyword.contains("Haste")) // AI_doesCreatureAttack would have prevented the effect from granting haste, because it assumes the creature would already have it
                                    return CardFactoryUtil.canTarget(card, c);
                                    
                                    return (CardFactoryUtil.AI_doesCreatureAttack(c))
                                            && (CardFactoryUtil.canTarget(card, c))
                                            && (!keyword.equals("none") && !c.hasAnyKeyword(keyword.split(" & ")))
                                            && (!(!c.hasSickness()) && keyword.contains("Haste")); // if creature doesn't have sickness, the haste keyword won't help
                                }
                                return false;
                            }
                        });
                        if (abCost.getSacCost() && abCost.getSacThis())	// if sacrifice <this>, don't self-target
                        	list.remove(card);
                        return list;
                    }//getCreatures()
                    
                    @Override
                    public void resolve() {
                        final Card[] creature = new Card[1];
                        if(abTgt.doesTarget()) 
                        	creature[0] = getTargetCard();
                        else if (bPumpEquipped)
                        	creature[0] = card.getEquippingCard();
                        else 
                        	creature[0] = card;
                    	
                        if(creature[0] != null && AllZone.GameAction.isCardInPlay(creature[0])
                                && (!abTgt.doesTarget() || CardFactoryUtil.canTarget(card, getTargetCard()))) {

                            final int a = getNumAttack();
                            final int d = getNumDefense();
                            
                            final Command EOT = new Command() {
                                private static final long serialVersionUID = -8840812331316327448L;
                                
                                public void execute() {
                                    if(AllZone.GameAction.isCardInPlay(creature[0])) {
                                        creature[0].addTempAttackBoost(-1 * a);
                                        creature[0].addTempDefenseBoost(-1 * d);
                                        if(!Keyword[0].equals("none"))
                                        {
                                        	String[] kws = Keyword[0].split(" & ");
                                        	for (int i=0; i<kws.length; i++)
                                        		creature[0].removeExtrinsicKeyword(kws[i]);
                                        }
                                    }
                                    
                                }
                            };
                            
                            creature[0].addTempAttackBoost(a);
                            creature[0].addTempDefenseBoost(d);
                            if(!Keyword[0].equals("none")) 
                            {
                            	String[] kws = Keyword[0].split(" & ");
                            	for (int i=0; i<kws.length; i++)
                            		creature[0].addExtrinsicKeyword(kws[i]);
                            }
                            
                            card.setAbilityUsed(card.getAbilityUsed() + 1);
                            AllZone.EndOfTurn.addUntil(EOT);
                            
                            if(!DrawBack[0].equals("none")) CardFactoryUtil.doDrawBack(DrawBack[0], 0,
                                    card.getController(),
                                    AllZone.GameAction.getOpponent(card.getController()), null, card,
                                    creature[0], this);
                            
                        }//if (card is in play)
                    }//resolve()
                };//SpellAbility

                ability.setDescription(spDesc[0]);
                ability.setStackDescription(stDesc[0]);
                
                if(!abTgt.doesTarget())
                	ability.setTargetCard(card);

                card.addSpellAbility(ability);
            }
        }//while
        

        if(hasKeyword(card, "Untap") != -1) {
            int n = hasKeyword(card, "Untap");
            if(n != -1) {
                String parse = card.getKeyword().get(n).toString();
                card.removeIntrinsicKeyword(parse);
                
                String k[] = parse.split(":");
                final String manacost = k[1];
                
                card.addSpellAbility(CardFactoryUtil.ability_Untap(card, manacost));
            }
        }
        
        if(hasKeyword(card, "Remove three spore counters") != -1) {
            int n = hasKeyword(card, "Remove three spore counters");
            if(n != -1) {
                String parse = card.getKeyword().get(n).toString();
                card.removeIntrinsicKeyword(parse);
                
                card.addSpellAbility(CardFactoryUtil.ability_Spore_Saproling(card));
            }
        }//Spore Saproling
        
        if (hasKeyword(card, "abAddReflectedMana") != -1) {
        	int n = hasKeyword(card,"abAddReflectedMana");
        	
        	String parse = card.getKeyword().get(n).toString();
            card.removeIntrinsicKeyword(parse);
        	String[] k = parse.split(":");
        	
        	// Reflecting Pool, Exotic Orchard, Fellwar Stone
        	card.setReflectedLand(true);
        	//
            final Ability_Mana reflectedManaAbility = CardFactoryUtil.getReflectedManaAbility(card, k[1], k[2]); 

            card.addSpellAbility(reflectedManaAbility);
        } // ReflectingPool
        
        if(hasKeyword(card, "spDamageTgt") != -1) {
            int n = hasKeyword(card, "spDamageTgt");
            if(n != -1) {
                String parse = card.getKeyword().get(n).toString();
                card.removeIntrinsicKeyword(parse);
                
                card.clearSpellAbility();
                
                String k[] = parse.split(":");
                
                final boolean TgtCreature[] = {false};
                final boolean TgtPlayer[] = {false};
                final boolean TgtCP[] = {false};
                final boolean TgtOpp[] = {false};
                final boolean usesXCost[] = {false};
                
                if(k[0].contains("CP")) TgtCP[0] = true;
                else if(k[0].contains("P")) TgtPlayer[0] = true;
                else if(k[0].contains("C")) TgtCreature[0] = true;
                else if(k[0].contains("Opp")) TgtOpp[0] = true;
                
                // how much damage
                final int NumDmg[] = {-1};
                final String NumDmgX[] = {"none"};
                
                if(k[1].matches("X")) {
                    String x = card.getSVar(k[1]);
                    if(x.startsWith("Count$")) {
                        String kk[] = x.split("\\$");
                        NumDmgX[0] = kk[1];
                    }
                    if (x.equals("Count$xPaid"))
                    {
                    	usesXCost[0] = true;
                    }
 
                } else if(k[1].matches("[0-9][0-9]?")) NumDmg[0] = Integer.parseInt(k[1]);
                
                //drawbacks and descriptions
                final String DrawBack[] = {"none"};
                final String spDesc[] = {"none"};
                final String stDesc[] = {"none"};
                if(k.length > 2) {
                    if(k[2].contains("Drawback$")) {
                        String kk[] = k[2].split("\\$");
                        DrawBack[0] = kk[1];
                        if(k.length > 3) spDesc[0] = k[3];
                        if(k.length > 4) stDesc[0] = k[4];
                    } else {
                        if(k.length > 2) spDesc[0] = k[2];
                        if(k.length > 3) stDesc[0] = k[3];
                    }
                }
                
                final SpellAbility DamageTgt = new Spell(card) {
                    private static final long serialVersionUID = 7239608350643325111L;
                    private int               damage;
                    
                    public int getNumDamage() {
                        if(NumDmg[0] != -1) return NumDmg[0];
                        
                        if(!NumDmgX[0].equals("none")) return CardFactoryUtil.xCount(card, NumDmgX[0]);
                        
                        return 0;
                    }
                    
                    public int getNumXDamage()
                    {
                    	return card.getXManaCostPaid();
                    }
                    
                    boolean shouldTgtP() {
                        PlayerZone compHand = AllZone.getZone(Constant.Zone.Hand, Constant.Player.Computer);
                        CardList hand = new CardList(compHand.getCards());
                        
                        if(hand.size() >= 7) // anti-discard-at-EOT
                        	return true;
                        
                        if(AllZone.Human_Life.getLife() < (10 + damage)) // if damage from this spell would drop the human to less than 10 life
                        	return true;
                        
                        return false;
                    }
                    
                    Card chooseTgtC() {
                        // Combo alert!!
                        PlayerZone compy = AllZone.getZone(Constant.Zone.Play, Constant.Player.Computer);
                        CardList cPlay = new CardList(compy.getCards());
                        if(cPlay.size() > 0) for(int i = 0; i < cPlay.size(); i++)
                            if(cPlay.get(i).getName().equals("Stuffy Doll")) return cPlay.get(i);
                        
                        PlayerZone human = AllZone.getZone(Constant.Zone.Play, Constant.Player.Human);
                        CardList hPlay = new CardList(human.getCards());
                        hPlay = hPlay.filter(new CardListFilter() {
                            public boolean addCard(Card c) {
                                // will include creatures already dealt damage
                                return c.isCreature() && ((c.getNetDefense() + c.getDamage()) <= damage)
                                        && CardFactoryUtil.canTarget(card, c);
                            }
                        });
                        
                        if(hPlay.size() > 0) {
                            Card best = hPlay.get(0);
                            
                            if(hPlay.size() > 1) {
                                for(int i = 1; i < hPlay.size(); i++) {
                                    Card b = hPlay.get(i);
                                    // choose best overall creature?
                                    if(b.getSpellAbility().length > best.getSpellAbility().length
                                            || b.getKeyword().size() > best.getKeyword().size()
                                            || b.getNetAttack() > best.getNetAttack()) best = b;
                                }
                            }
                            
                            return best;
                        }
                        
                        return null;
                    }
                    
                    @Override
                    public boolean canPlayAI() {
                        damage = getNumDamage();
                        
                        if (damage == 0)
                        	return false;
                        
                        if(TgtCP[0] == true) {
                            if(shouldTgtP() == true) {
                                setTargetPlayer(Constant.Player.Human);
                                return true;
                            }
                            
                            Card c = chooseTgtC();
                            if(c != null) {
                                setTargetCard(c);
                                return true;
                            }
                        }
                        
                        if(TgtPlayer[0] == true || TgtOpp[0] == true) {
                            setTargetPlayer(Constant.Player.Human);
                            return true;
                        }
                        
                        if(TgtCreature[0] == true) {
                            Card c = chooseTgtC();
                            if(c != null) {
                                setTargetCard(c);
                                return true;
                            }
                        }
                        
                        return false;
                    }
                    
                    @Override
                    public void resolve() {
                        damage = getNumDamage();
                        if (usesXCost[0])
                        	damage = getNumXDamage();
                        String tgtP = "";
                        
                        if(TgtOpp[0] == true) setTargetPlayer(AllZone.GameAction.getOpponent(card.getController()));
                        
                        if(getTargetCard() != null) {
                            if(AllZone.GameAction.isCardInPlay(getTargetCard())
                                    && CardFactoryUtil.canTarget(card, getTargetCard())) {
                                Card c = getTargetCard();
                                //c.addDamage(damage);
                                AllZone.GameAction.addDamage(c, card, damage);
                                tgtP = c.getController();
                                
                                if(!DrawBack[0].equals("none")) 
                                	CardFactoryUtil.doDrawBack(DrawBack[0], damage,
                                        card.getController(), AllZone.GameAction.getOpponent(card.getController()), tgtP,
                                        card, getTargetCard(), this);
                            }
                        } else {
                        	tgtP = getTargetPlayer();
                            AllZone.GameAction.addDamage(tgtP, card, damage);
                            
                            if(!DrawBack[0].equals("none")) CardFactoryUtil.doDrawBack(DrawBack[0], damage,
                                    card.getController(), AllZone.GameAction.getOpponent(card.getController()), tgtP,
                                    card, null, this);
                        }
                        
                        
                    }// resolove
                }; //spellAbility
                
                card.setSVar("PlayMain1", "TRUE");
                
                if(!spDesc[0].equals("none")) DamageTgt.setDescription(spDesc[0]);
                else {
                    String s;
                    s = card.getName() + " deals " + NumDmg[0] + " damage to target";
                    if(TgtCP[0]) s = s + " creature or player.";
                    else if(TgtCreature[0]) s = s + " creature.";
                    else if(TgtPlayer[0]) s = s + " player.";
                    DamageTgt.setDescription(s);
                }
                
                if(!stDesc[0].equals("none")) DamageTgt.setStackDescription(stDesc[0]);
                else DamageTgt.setStackDescription(card.getName() + " - deals " + NumDmg[0] + " damage.");
                
                if(TgtCP[0]) DamageTgt.setBeforePayMana(CardFactoryUtil.input_targetCreaturePlayer(DamageTgt,
                        true, false));
                else if(TgtCreature[0]) DamageTgt.setBeforePayMana(CardFactoryUtil.input_targetCreature(DamageTgt));
                else if(TgtPlayer[0]) DamageTgt.setBeforePayMana(CardFactoryUtil.input_targetPlayer(DamageTgt));
                
                card.addSpellAbility(DamageTgt);
                
                String bbCost = card.getSVar("Buyback");
                if (!bbCost.equals(""))
                {
                   SpellAbility bbDamageTgt = DamageTgt.copy();
                   bbDamageTgt.setManaCost(CardUtil.addManaCosts(card.getManaCost(), bbCost));
                   bbDamageTgt.setDescription("Buyback " + bbCost + "(You may pay an additional " + bbCost + " as you cast this spell. If you do, put this card into your hand as it resolves.)");
                   bbDamageTgt.setIsBuyBackAbility(true);
                   
                   if (TgtCP[0] == true)
                       bbDamageTgt.setBeforePayMana(CardFactoryUtil.input_targetCreaturePlayer(bbDamageTgt, true, false));
                   else if (TgtCreature[0])
                	   bbDamageTgt.setBeforePayMana(CardFactoryUtil.input_targetCreature(bbDamageTgt));
                   else if (TgtPlayer[0])
                	   bbDamageTgt.setBeforePayMana(CardFactoryUtil.input_targetPlayer(bbDamageTgt));
                   
                   card.addSpellAbility(bbDamageTgt);
                }

            }
        }// spDamageTgt

        
        //Keyword for spells, that damage all creatures
        if (hasKeyword(card, "spDamageAll") != -1)
        {
        	int n = hasKeyword(card, "spDamageAll");
        	if (n != -1)
        	{
                String parse = card.getKeyword().get(n).toString();
                card.removeIntrinsicKeyword(parse);
                
                final int NumDam[] = {-1};
                final String NumDamX[] = {"none"};
                final boolean DmgPlayer[] = {false};
                
                String k[] = parse.split(":");
                String Targets = k[1]; // Artifact, Creature, Enchantment, Land, Permanent, White, Blue, Black, Red, Green, Colorless, MultiColor
                // non-Artifact, non-Creature, non-Enchantment, non-Land, non-Permanent,
                //non-White, non-Blue, non-Black, non-Red, non-Green, non-Colorless, non-MultiColor
                if (Targets.startsWith("Player")) {
                	Targets = Targets.replaceFirst("Player,", "");
                	DmgPlayer[0] = true;
                }											// if Players are affected they have to be at the start
                final String Tgts[] = Targets.split(","); 

                
                  if (k[2].matches("X"))
                  {
                     String x = card.getSVar(k[2]);
                     if (x.startsWith("Count$"))
                     {
                        String kk[] = x.split("\\$");
                        NumDamX[0] = kk[1];
                     }
                  }
                  else if (k[2].matches("[0-9][0-9]?"))
                     NumDam[0] = Integer.parseInt(k[2]);
                 
                  // drawbacks and descriptions
                  final String DrawBack[] = {"none"};
                  final String spDesc[] = {"none"};
                  if (k.length > 3)
                  {
                     if (k[3].contains("Drawback$"))
                     {
                    	 String kk[] = k[3].split("\\$");
                    	 DrawBack[0] = kk[1];
                    	 spDesc[0] = k[4];
                     }
                     else
                    	 spDesc[0] = k[3];
                  }
                  else
                	  spDesc[0] = "cardName deals " + NumDam[0] + " damage to each creature and player.";
 

                  final SpellAbility spDmgAll = new Spell(card)
                  {
                  private static final long serialVersionUID = -2598054704232863475L;

                   public int getNumDam()
                   {
                      if (NumDam[0] != -1)
                         return NumDam[0];

                      if (! NumDamX[0].equals("none"))
                      return CardFactoryUtil.xCount(card, NumDamX[0]);
                     
                      return 0;
                   }
                   
                   public boolean canPlayAI()
                   {
                	   	int ndam = getNumDam();
                	   	
                    	if (DmgPlayer[0] && AllZone.Computer_Life.getLife() <= ndam) 
                    		return false;       									// The AI will not kill itself
                    	if (DmgPlayer[0] && AllZone.Human_Life.getLife() <= ndam && AllZone.Computer_Life.getLife() > ndam) 
                    		return true;											// The AI will kill the human if possible
                    	
                	   	CardList human = new CardList(AllZone.Human_Play.getCards());
                	   	CardList computer = new CardList(AllZone.Computer_Play.getCards());
                    
                    	human = human.getValidCards(Tgts);
                        human = human.canBeDamagedBy(card);
                    	human = human.getNotKeyword("Indestructible");
                    	human = human.filter(new CardListFilter() {
                            public boolean addCard(Card c) {
                                return (c.getKillDamage() <= getNumDam());
                            }
                    	}); // leaves all creatures that will be destroyed
                        int humanvalue = CardListUtil.sumCMC(human);
                        humanvalue += human.size();
                        humanvalue += CardListUtil.sumAttack(human.getTokens());
                        // X = total converted mana cost + number of permanents + total power of tokens (Human)
                        if (!DmgPlayer[0] && AllZone.Computer_Life.getLife() < 7) humanvalue += CardListUtil.sumAttack(human); 
                        // in Low Life Emergency (and not hurting itself) X = X + total power of human creatures
                        
                    	computer = computer.getValidCards(Tgts);
                    	computer = computer.canBeDamagedBy(card);
                    	computer = computer.getNotKeyword("Indestructible");
                    	computer = computer.filter(new CardListFilter() {
                            public boolean addCard(Card c) {
                                return (c.getKillDamage() <= getNumDam());
                            }
                    	}); // leaves all creatures that will be destroyed
                        int computervalue = CardListUtil.sumCMC(computer);
                        computervalue += computer.size();
                        computervalue += CardListUtil.sumAttack(computer.getTokens()); 
                        // Y = total converted mana cost + number of permanents + total power of tokens (Computer)

                        // the computer will play the spell if Y < X - 3
                        return  AllZone.Phase.getPhase().equals(Constant.Phase.Main2) && 
                        		(computervalue < humanvalue - 3);
                   	}

                  public void resolve()
                    {
                        int ndam = getNumDam();
                       
                        CardList all = new CardList();
                    	all.addAll(AllZone.Human_Play.getCards());
                    	all.addAll(AllZone.Computer_Play.getCards());
                    	all = all.getValidCards(Tgts);
                    
                    	for(int i = 0; i < all.size(); i++) {
                        	if(CardFactoryUtil.canDamage(card, all.get(i))) all.get(i).addDamage(ndam, card);
                    	}
                    	if (DmgPlayer[0] == true) {
                    		AllZone.GameAction.addDamage(Constant.Player.Computer, card, ndam);
                    		AllZone.GameAction.addDamage(Constant.Player.Human, card, ndam);
                    	}
                       // if (!DrawBack[0].equals("none"))
                       //    CardFactoryUtil.doDrawBack(DrawBack[0], ndam, card.getController(), AllZone.GameAction.getOpponent(card.getController()), null, card, null);
                    	
                     }//resolve()
                  };//SpellAbility spDmgAll
                 
                  spDmgAll.setDescription(spDesc[0]);
                  spDmgAll.setStackDescription(spDesc[0]);
                 
                  card.clearSpellAbility();
                  card.addSpellAbility(spDmgAll);
                  
                  String bbCost = card.getSVar("Buyback");
                  if (!bbCost.equals(""))
                  {
                     SpellAbility bbspDmgAll = spDmgAll.copy();
                     bbspDmgAll.setManaCost(CardUtil.addManaCosts(card.getManaCost(), bbCost));
                     bbspDmgAll.setDescription("Buyback " + bbCost + "(You may pay an additional " + bbCost + " as you cast this spell. If you do, put this card into your hand as it resolves.)");
                     bbspDmgAll.setIsBuyBackAbility(true);
                     
                     card.addSpellAbility(bbspDmgAll);
                  }
        	}
        }//spDamageAll
        
        
        while(hasKeyword(card, "abDamage") != -1) {
            int n = hasKeyword(card, "abDamage");
            if(n != -1) {
                String parse = card.getKeyword().get(n).toString();
                card.removeIntrinsicKeyword(parse);
                
        		String k[] = parse.split(":");
        		String tmpCost[] =  k[0].replace("abDamage", "").split(" ", 2);
        		
        		int drawBack = 2;
        		
        		final Target abTgt = new Target(tmpCost[0]);
        		if (abTgt.canTgtValid()){
        			int valid = drawBack;
					// Looks like VTSelection is used for the Message box, should improve the message
        			abTgt.setVTSelection("Select a target: " + k[valid]);
        			abTgt.setValidTgts(k[valid].split(","));
        			drawBack++;
        		}
        		
        		final Ability_Cost abCost = new Ability_Cost(tmpCost[1], card.getName(), true);    
                
                final int NumDmg[] = {-1};
                final String NumDmgX[] = {"none"};
                
                if(k[1].matches("X")) {
                    String x = card.getSVar(k[1]);
                    if(x.startsWith("Count$")) {
                        String kk[] = x.split("\\$");
                        NumDmgX[0] = kk[1];
                    }
                } 
                else if(k[1].matches("[0-9][0-9]?")) 
                	NumDmg[0] = Integer.parseInt(k[1]);
                
                //drawbacks and descriptions
                final String DrawBack[] = {"none"};
                final String spDesc[] = {"none"};
                final String stDesc[] = {"none"};
                if(k.length > drawBack) {
                    if(k[drawBack].contains("Drawback$")) {
                        String kk[] = k[drawBack].split("\\$");
                        DrawBack[0] = kk[1];
                        if(k.length > drawBack+1) spDesc[0] = k[drawBack+1];
                        if(k.length > drawBack+2) stDesc[0] = k[drawBack+2];
                    } else {
                        if(k.length > drawBack) spDesc[0] = k[drawBack];
                        if(k.length > drawBack+1) stDesc[0] = k[drawBack+1];
                    }
                } else {
                    StringBuilder sb = new StringBuilder();
                    sb.append(card.getName());
                    sb.append(" deals " + NumDmg[0] + " damage to ");
                    
                    sb.append(abTgt.targetString());
                    spDesc[0] = sb.toString();
                    stDesc[0] = card.getName() + " -" + sb.toString();
                }
                
                spDesc[0] = abCost.toString() + spDesc[0];
                           
                // Damage ability starts here
                final SpellAbility abDamage = new Ability_Activated(card, abCost, abTgt) {
                    private static final long serialVersionUID = -7560349014757367722L;
                    
                    private int               damage;
                    
                    public int getNumDamage() {
                        if(NumDmg[0] != -1) return NumDmg[0];
                        
                        if(!NumDmgX[0].equals("none")) return CardFactoryUtil.xCount(card, NumDmgX[0]);
                        
                        return 0;
                    }
                    
                    boolean shouldTgtP() {
                        PlayerZone compHand = AllZone.getZone(Constant.Zone.Hand, Constant.Player.Computer);
                        CardList hand = new CardList(compHand.getCards());
                        
                        if(hand.size() > 7) // anti-discard-at-EOT
                        	return true;
                        
                        if(AllZone.Human_Life.getLife() - damage < 10) // if damage from this spell would drop the human to less than 10 life
                        	return true;
                        
                        return false;
                    }
                    
                    Card chooseTgtC() {
                        // Combo alert!!
                        PlayerZone compy = AllZone.getZone(Constant.Zone.Play, Constant.Player.Computer);
                        CardList cPlay = new CardList(compy.getCards());
                        if(cPlay.size() > 0) for(int i = 0; i < cPlay.size(); i++)
                            if(cPlay.get(i).getName().equals("Stuffy Doll")) return cPlay.get(i);
                        
                        PlayerZone human = AllZone.getZone(Constant.Zone.Play, Constant.Player.Human);
                        CardList hPlay = new CardList(human.getCards());
                        hPlay = hPlay.filter(new CardListFilter() {
                            public boolean addCard(Card c) {
                                // will include creatures already dealt damage
                                return c.isCreature() && ((c.getNetDefense() + c.getDamage()) <= damage)
                                        && CardFactoryUtil.canTarget(card, c);
                            }
                        });
                        
                        if(hPlay.size() > 0) {
                            Card best = hPlay.get(0);
                            
                            if(hPlay.size() > 1) {
                                for(int i = 1; i < hPlay.size(); i++) {
                                    Card b = hPlay.get(i);
                                    // choose best overall creature?
                                    if(b.getSpellAbility().length > best.getSpellAbility().length
                                            || b.getKeyword().size() > best.getKeyword().size()
                                            || b.getNetAttack() > best.getNetAttack()) best = b;
                                }
                            }
                            return best;
                        }
                        return null;
                    }
                    
                    @Override
                    public boolean canPlay(){
                    	Cost_Payment pay = new Cost_Payment(abCost, this);
                        return (pay.canPayAdditionalCosts() && CardFactoryUtil.canUseAbility(card) && super.canPlay());
                    }
                    
                    @Override
                    public boolean canPlayAI() {
                    	// temporarily disabled until better AI
                    	if (abCost.getSacCost())	 return false;
                    	if (abCost.getSubCounter())  return false;
                    	if (abCost.getLifeCost())	 return false;
                    	
                    	if (!ComputerUtil.canPayCost(this))
                    		return false;

                        damage = getNumDamage();
                        
                        Random r = new Random(); // prevent run-away activations 
                        boolean rr = false;
                        if(r.nextFloat() <= Math.pow(.6667, card.getAbilityUsed())) 
                        	rr = true;
                        
                        if(abTgt.canTgtCreaturePlayer()) {
                            if(shouldTgtP()) {
                                setTargetPlayer(Constant.Player.Human);
                                return rr;
                            }
                            
                            Card c = chooseTgtC();
                            if(c != null) {
                                setTargetCard(c);
                                return rr;
                            }
                        }
                        
                        if(abTgt.canTgtPlayer()/* || TgtOpp[0] == true */) {
                            setTargetPlayer(Constant.Player.Human);
                            return rr;
                        }
                        
                        if(abTgt.canTgtCreature()) {
                            Card c = chooseTgtC();
                            if(c != null) {
                                setTargetCard(c);
                                return rr;
                            }
                        }
                        return false;
                    }
                    
                    @Override
                    public void resolve() {
                        int damage = getNumDamage();
                        String tgtP = "";
                        
                        //if(TgtOpp[0] == true) {
                        //    tgtP = AllZone.GameAction.getOpponent(card.getController());
                        //    setTargetPlayer(tgtP);
                        //}
                        Card c = getTargetCard();
                        if(c != null) {
                            if(AllZone.GameAction.isCardInPlay(c) && CardFactoryUtil.canTarget(card, c)) {
                                AllZone.GameAction.addDamage(c, card, damage);
                                tgtP = c.getController();
                                
                                if(!DrawBack[0].equals("none")) CardFactoryUtil.doDrawBack(DrawBack[0], damage,
                                        card.getController(), AllZone.GameAction.getOpponent(card.getController()),
                                        tgtP, card, c, this);

                            }
                        } else {
                            tgtP = getTargetPlayer();
                            AllZone.GameAction.addDamage(tgtP, card, damage);
                            
                            if(!DrawBack[0].equals("none")) CardFactoryUtil.doDrawBack(DrawBack[0], damage,
                                    card.getController(), AllZone.GameAction.getOpponent(card.getController()),
                                    tgtP, card, null, this);

                        }
                        
                    }//resolve()
                };//Ability_Activated
                
                abDamage.setDescription(spDesc[0]);
                abDamage.setStackDescription(stDesc[0]);
                
                card.addSpellAbility(abDamage);
            }
        }//abDamageTgt

        // Generic destroy target card
        if(hasKeyword(card, "spDestroyTgt") != -1) {
            int n = hasKeyword(card, "spDestroyTgt");
            
            String parse = card.getKeyword().get(n).toString();
            card.removeIntrinsicKeyword(parse);
            
            String k[] = parse.split(":");
            String Targets = k[1]; // Artifact, Creature, Enchantment, Land, Permanent, White, Blue, Black, Red, Green, Colorless, MultiColor
            // non-Artifact, non-Creature, non-Enchantment, non-Land, non-Permanent,
            //non-White, non-Blue, non-Black, non-Red, non-Green, non-Colorless, non-MultiColor
            final String Tgts[] = Targets.split(",");
            
            String tmpDesc = card.getText().substring(15);
            int i = tmpDesc.indexOf(".");
            tmpDesc = tmpDesc.substring(0, i);
            final String Selec = "Select target " + tmpDesc + " to destroy.";
            
            final boolean NoRegen[] = {false};
            final String Drawback[] = {"none"};
            
            if (k.length > 2)
            {
            	if (k[2].equals("NoRegen"))
            		NoRegen[0] = true;
            	
            	else if (k[2].startsWith("Drawback$"))
            		Drawback[0] = k[2];
            	            	
            	if (k.length > 3)
            	{
            		if (k[3].startsWith("Drawback$"))
            			Drawback[0] = k[3];
            	}
            	
            	if (!Drawback[0].equals("none"))
            	{
            		String kk[] = Drawback[0].split("\\$");
                    Drawback[0] = kk[1];
            	}
            }
            
            card.clearSpellAbility();
            
            final SpellAbility spDstryTgt = new Spell(card) {
                private static final long serialVersionUID = 142142142142L;
                
                @Override
                public boolean canPlayAI() {
                    CardList results = new CardList();
                    CardList choices = getTargets();
                    
                    choices = choices.filter(new CardListFilter(){
                    	public boolean addCard(Card c)
                    	{
                    		return !c.getKeyword().contains("Indestructible");
                    	}
                    });
                    
                    
                    if(choices.size() > 0) {
                        for(int i = 0; i < Tgts.length; i++) {
                            if(Tgts[i].startsWith("Artifact")) {
                                if(CardFactoryUtil.AI_getBestArtifact(choices) != null) results.add(CardFactoryUtil.AI_getBestArtifact(choices));
                            } else if(Tgts[i].startsWith("Creature")) {
                                if(CardFactoryUtil.AI_getBestCreature(choices) != null) results.add(CardFactoryUtil.AI_getBestCreature(choices));
                            } else if(Tgts[i].startsWith("Enchantment")) {
                                if(CardFactoryUtil.AI_getBestEnchantment(choices, card, true) != null) results.add(CardFactoryUtil.AI_getBestEnchantment(
                                        choices, card, true));
                            } else if(Tgts[i].startsWith("Land")) {
                                if(CardFactoryUtil.AI_getBestLand(choices) != null) results.add(CardFactoryUtil.AI_getBestLand(choices));
                            } else if(Tgts[i].startsWith("Permanent")) {
                                if(CardFactoryUtil.AI_getMostExpensivePermanent(choices, card, true) != null) results.add(CardFactoryUtil.AI_getMostExpensivePermanent(
                                        choices, card, true));
                            }
                        }
                    }
                    
                    if(results.size() > 0) {
                        results.shuffle();
                        setTargetCard(results.get(0));
                        return true;
                    }
                    return false;
                }
                
                CardList getTargets() {
                    CardList tmpList = new CardList();
                    tmpList.addAll(AllZone.Human_Play.getCards());
                    tmpList = tmpList.getValidCards(Tgts);
                    tmpList = tmpList.getTargetableCards(card);
                    
                    return tmpList;
                }
                
                @Override
                public void resolve() 
                {
                	Card tgtC = getTargetCard();
                    if(AllZone.GameAction.isCardInPlay(tgtC) && CardFactoryUtil.canTarget(card, tgtC))
                    {
                    	if(NoRegen[0]) 
                    		AllZone.GameAction.destroyNoRegeneration(tgtC);
                    	else 
                    		AllZone.GameAction.destroy(tgtC);
                    	
                    	if (!Drawback[0].equals("none"))
                    		CardFactoryUtil.doDrawBack(Drawback[0], 0, card.getController(), AllZone.GameAction.getOpponent(card.getController()), tgtC.getController(), card, tgtC, this);
                    }
                }
            }; //SpDstryTgt
            
            Input InGetTarget = CardFactoryUtil.input_targetValid(spDstryTgt, Tgts, Selec);
            
            spDstryTgt.setBeforePayMana(InGetTarget);
            
            spDstryTgt.setDescription(card.getSpellText());
            card.setText("");
            
            card.setSVar("PlayMain1", "TRUE");
            
            card.addSpellAbility(spDstryTgt);
            
            String bbCost = card.getSVar("Buyback");
            if (!bbCost.equals(""))
            {
                SpellAbility bbDstryTgt = spDstryTgt.copy();
                bbDstryTgt.setManaCost(CardUtil.addManaCosts(card.getManaCost(), bbCost));
                
                StringBuilder sb = new StringBuilder();
                sb.append("Buyback ").append(bbCost).append(" (You may pay an additional ").append(bbCost);
                sb.append(" as you cast this spell. If you do, put this card into your hand as it resolves.)");
                bbDstryTgt.setDescription(sb.toString());
                // bbDstryTgt.setDescription("Buyback " + bbCost + "(You may pay an additional " + bbCost + " as you cast this spell. If you do, put this card into your hand as it resolves.)");
                bbDstryTgt.setIsBuyBackAbility(true);
                
                bbDstryTgt.setBeforePayMana(CardFactoryUtil.input_targetValid(bbDstryTgt, Tgts, Selec));
                
                card.addSpellAbility(bbDstryTgt);
             }
         }//spDestroyTgt
        
        // Generic destroy target ___ activated ability
        if (hasKeyword(card, "abDestroyTgtV") != -1)
        {
        	int n = hasKeyword(card, "abDestroyTgtV");
        	
        	String parse = card.getKeyword().get(n).toString();
        	card.removeIntrinsicKeyword(parse);
        	
        	String k[] = parse.split(":");
        	
        	String tmpCost = k[0].substring(13);
        	final Ability_Cost abCost = new Ability_Cost(tmpCost, card.getName(), true);
        	
        	final Target tgtDstryTgt = new Target("TgtV");
        	final String Tgts[] = k[1].split(",");
        	tgtDstryTgt.setValidTgts(Tgts);
        	
        	final boolean NoRegen[] = {false};
        	final String Drawback[] = {"none"};
        	final String spDesc[] = {"none"};
        	
        	if (k[2].equals("NoRegen"))
        	{
        		NoRegen[0] = true;
        		
        		if (k.length > 3)
        		{
        			if (k[3].startsWith("Drawback$"))
        				Drawback[0] = k[3].substring(9);
        			else
        				spDesc[0] = k[3];
        		}
        	}
        	else if (k[2].startsWith("Drawback$"))
        	{
        		Drawback[0] = k[2].substring(9);
        		
        		if (k.length > 3)
        			spDesc[0] = k[3];
        	}
        	else
        		spDesc[0] = k[2];
        	
        	String tmpDesc = spDesc[0].substring(15);
        	int i = tmpDesc.indexOf(".");
        	tmpDesc = tmpDesc.substring(0, i);
        	//final String Selec = "Select target " + tmpDesc + " to destroy.";
        	tgtDstryTgt.setVTSelection("Select target " + tmpDesc + " to destroy.");
        	
        	spDesc[0] = abCost.toString() + spDesc[0];
        	
        	final SpellAbility AbDstryTgt = new Ability_Activated(card, abCost, tgtDstryTgt)
        	{
        		private static final long serialVersionUID = -141142183348756081L;
        		
                @Override
                public boolean canPlayAI() {
                    if (!ComputerUtil.canPayCost(this))
                    	return false;
                	
                    CardList hCards = getTargets();
                    
                    hCards = hCards.filter(new CardListFilter(){
                    	public boolean addCard(Card c)
                    	{
                    		return !c.getKeyword().contains("Indestructible");
                    	}
                    });
                    
                    Random r = new Random();
                    boolean rr = false;
                    if (r.nextFloat() <= Math.pow(.6667, card.getAbilityUsed()))
                    	rr = true;
                    
                    if(hCards.size() > 0) {
                    	Card c = null;
                    	CardList dChoices = new CardList();
                    	
                        for(int i = 0; i < Tgts.length; i++) {
                        	if (Tgts[i].startsWith("Creature"))
                        	{
                        		c = CardFactoryUtil.AI_getBestCreature(hCards);
                        		if (c != null)
                        			dChoices.add(c);
                        	}
                        	
                        	CardListUtil.sortByTextLen(hCards);
                        	dChoices.add(hCards.get(0));
                        	
                        	CardListUtil.sortCMC(hCards);
                        	dChoices.add(hCards.get(0));
                        }
                        
                        c = dChoices.get(CardUtil.getRandomIndex(dChoices));
                        setTargetCard(c);
                        
                        return rr;
                    }
                    
                    return false;
                }
                
                CardList getTargets() {
                    CardList tmpList = new CardList();
                    tmpList.addAll(AllZone.Human_Play.getCards());
                    tmpList = tmpList.getValidCards(Tgts);
                    tmpList = tmpList.getTargetableCards(card);
                    
                    return tmpList;
                }
                
                @Override
                public boolean canPlay(){
                	Cost_Payment pay = new Cost_Payment(abCost, this);
                    return (pay.canPayAdditionalCosts() && CardFactoryUtil.canUseAbility(card) && super.canPlay());
                }

                
                @Override
                public void resolve() 
                {
                	Card tgtC = getTargetCard();
                    if(AllZone.GameAction.isCardInPlay(tgtC) && CardFactoryUtil.canTarget(card, tgtC))
                    {
                    	if(NoRegen[0]) 
                    		AllZone.GameAction.destroyNoRegeneration(tgtC);
                    	else 
                    		AllZone.GameAction.destroy(tgtC);
                    	
                    	if (!Drawback[0].equals("none"))
                    		CardFactoryUtil.doDrawBack(Drawback[0], 0, card.getController(), AllZone.GameAction.getOpponent(card.getController()), tgtC.getController(), card, tgtC, this);
                    }
                }

        	}; //AbDstryTgt
        	
        	AbDstryTgt.setDescription(spDesc[0]);
        	
        	card.addSpellAbility(AbDstryTgt);
        	card.setSVar("PlayMain1", "TRUE");
        }

        
        // Generic enters the battlefield destroy target
        if (hasKeyword(card, "etbDestroyTgt") != -1)
        {
        	int n = hasKeyword(card, "etbDestroyTgt");
        	
        	String parse = card.getKeyword().get(n).toString();
        	card.removeIntrinsicKeyword(parse);
        	
        	String k[] = parse.split(":");
        	
        	final boolean May[] = {false};
        	if (k[0].contains("May"))
        		May[0] = true;
        	
        	String Targets = k[1];
        	final String Tgts[] = Targets.split(",");
        	
            String tmpDesc = card.getSpellText();
            int i = tmpDesc.indexOf("destroy target ");
            tmpDesc = tmpDesc.substring(i + 15);
            i = tmpDesc.indexOf(".");
            tmpDesc = tmpDesc.substring(0, i);
            final String Selec = "Select target " + tmpDesc + " to destroy.";
        	
        	final boolean NoRegen[] = {false};
        	final String Drawback[] = {"none"};
        	if (k.length > 2)
        	{
        		if (k[2].equals("NoRegen"))
				{
					NoRegen[0] = true;
					if (k.length > 3)
						Drawback[0] = k[3];
				}
        		else
        			Drawback[0] = k[2];
        	}
        	
        	if (!Drawback[0].equals("none"))
        	{
        		String kk[] = Drawback[0].split("\\$");
                Drawback[0] = kk[1];
        	}
        	
        	final boolean Evoke[] = {false};
        	if (card.getSVar("Evoke").length() > 0)
        		Evoke[0] = true;
        	
        	card.setSVar("PlayMain1", "TRUE");
        	card.clearSpellAbility();
        	
        	// over-rides the default Spell_Permanent 
        	// enables the AI to play the card when appropriate 
        	SpellAbility spETBDestroyTgt = new Spell_Permanent(card)
        	{
                private static final long serialVersionUID = -1148528222969323318L;
                
                @Override
                public boolean canPlayAI() 
                {
                    Random r = new Random();
                	                    
                    CardList hCards = new CardList(AllZone.getZone(Constant.Zone.Play, Constant.Player.Human).getCards());
                    hCards = hCards.getValidCards(Tgts);
                    hCards = hCards.getTargetableCards(card);
                    if (hCards.size() > 0)
                    	return true;
                    
                    CardList cCards = new CardList(AllZone.getZone(Constant.Zone.Play, Constant.Player.Computer).getCards());
                    cCards = cCards.getValidCards(Tgts);
                    cCards = cCards.getTargetableCards(card);
                    if (cCards.size() == 0)
                    	return true;
                    else
                    {
                    	if (r.nextInt(100) > 67)
                    		return true;
                    }
                    
                	return false;
                }
        	};
        	card.addSpellAbility(spETBDestroyTgt);
        	
            // performs the destruction
        	final SpellAbility saDestroyTgt = new Ability(card, "0") 
            {
                @Override
                public void resolve() 
                {
                    Card c = getTargetCard();
                    if (c == null)
                    	return;
                    
                    if(AllZone.GameAction.isCardInPlay(c) && CardFactoryUtil.canTarget(card, c)) 
                    {
                        if(c.isToken()) 
                        	AllZone.getZone(c).remove(c);
                        else
                        	AllZone.GameAction.destroy(c);
                        
                        if (!Drawback[0].equals("none"))
                        	CardFactoryUtil.doDrawBack(Drawback[0], 0, card.getController(), AllZone.GameAction.getOpponent(card.getController()), c.getController(), card, c, this);
                    } 
                }
            };
            saDestroyTgt.setStackDescription(card.getName() + " - destroy target " + Selec + ".");
            
            // when the card enters the battlefield, enable the human to target 
            // or the AI decides on a target
            Command etbDestroyTgt = new Command()
            {
                private static final long serialVersionUID = 9072052875006010497L;
                
                public void execute() 
                {
                    CardList hCards = new CardList(AllZone.Human_Play.getCards());
                    CardList cCards = new CardList(AllZone.Computer_Play.getCards());
                    
                    hCards = hCards.getValidCards(Tgts);
                    hCards = hCards.getTargetableCards(card);
                    cCards = cCards.getValidCards(Tgts);
                    cCards = cCards.getTargetableCards(card);
                    
                    if(hCards.size() > 0 || cCards.size() > 0) 
                    {
                        if (card.getController().equals(Constant.Player.Human))
                        {
                            Input inDT = CardFactoryUtil.input_targetValid(saDestroyTgt, Tgts, Selec);

                            AllZone.InputControl.setInput(inDT);
                            
                            if (May[0] == true)
                            	ButtonUtil.enableOnlyCancel();
                            else
                            	ButtonUtil.disableAll();
                        } 
                        else
                        {
                        	Card c = new Card();
                        	CardList dChoices = new CardList();
                        	if(hCards.size() > 0) 
                            {
                                for (int i=0; i<Tgts.length; i++)
                                {
                                	if (Tgts[i].startsWith("Creature"))
                                	{
                                		c = CardFactoryUtil.AI_getBestCreature(hCards);
                                		if (c != null)
                                			dChoices.add(c);
                                	}
                                	
                                	CardListUtil.sortByTextLen(hCards);
                                	dChoices.add(hCards.get(0));
                                	
                                	CardListUtil.sortCMC(hCards);
                                	dChoices.add(hCards.get(0));
                                }
                                
                                c = dChoices.get(CardUtil.getRandomIndex(dChoices));
                                saDestroyTgt.setTargetCard(c);
                                AllZone.Stack.add(saDestroyTgt);
                            } 
                        	else if (cCards.size() > 0 && May[0] == false)
                        	{
                    			for (int i=0; i<Tgts.length; i++)
                    			{
                    				if (Tgts[i].startsWith("Creature"))
                    				{
                    					c = CardFactoryUtil.AI_getWorstCreature(cCards);
                    					if (c != null)
                    						dChoices.add(c);
                    				}
                    				
                    				CardListUtil.sortByTextLen(cCards);
                    				dChoices.add(cCards.get(cCards.size() - 1));
                    				
                    				CardListUtil.sortCMC(cCards);
                    				dChoices.add(cCards.get(cCards.size() - 1));
                    			}
                    			
                    			c = dChoices.get(CardUtil.getRandomIndex(dChoices));
                                saDestroyTgt.setTargetCard(c);
                                AllZone.Stack.add(saDestroyTgt);
                        	}
                        }
                    }
                }
            };
            card.addComesIntoPlayCommand(etbDestroyTgt);
            
            // handle SVar:Evoke:{cost}
            if (Evoke[0] == true)
            {
            	String EvCost = card.getSVar("Evoke");
            	
            	SpellAbility evDestroyTgt = new Spell_Evoke(card, EvCost)
            	{
                    private static final long serialVersionUID = 5261598836419831953L;
                    
                    @Override
                    public boolean canPlayAI() {
                        return false;
                    }            		
            	};
            	card.addSpellAbility(evDestroyTgt);
            }
        } // etbDestoryTgt
        

        // Generic destroy all card
        if(hasKeyword(card, "spDestroyAll") != -1) {
            int n = hasKeyword(card, "spDestroyAll");
            
            String parse = card.getKeyword().get(n).toString();
            card.removeIntrinsicKeyword(parse);
            
            String k[] = parse.split(":");
            String Targets = k[1]; // Artifact, Creature, Enchantment, Land, Permanent, White, Blue, Black, Red, Green, Colorless, MultiColor
            // non-Artifact, non-Creature, non-Enchantment, non-Land, non-Permanent,
            //non-White, non-Blue, non-Black, non-Red, non-Green, non-Colorless, non-MultiColor
            final String Tgts[] = Targets.split(",");
            
            final boolean NoRegen[] = {false};
            final String Drawback[] = {"none"};
            
            if (k.length > 2)
            {
            	if (k[2].equals("NoRegen"))
            		NoRegen[0] = true;
            	
            	else if (k[2].startsWith("Drawback$"))
            		Drawback[0] = k[2];
            	            	
            	if (k.length > 3)
            	{
            		if (k[3].startsWith("Drawback$"))
            			Drawback[0] = k[3];
            	}
            	
            	if (!Drawback[0].equals("none"))
            	{
            		String kk[] = Drawback[0].split("\\$");
                    Drawback[0] = kk[1];
            	}
            }
            
            card.clearSpellAbility();
            
            final SpellAbility spDstryAll = new Spell(card) {
                private static final long serialVersionUID = 132554543614L;
                
                @Override
                public boolean canPlayAI() {
                    CardList human = new CardList(AllZone.Human_Play.getCards());
                    CardList computer = new CardList(AllZone.Computer_Play.getCards());
                    
                    human = human.getValidCards(Tgts);
                    human = human.getNotKeyword("Indestructible");
                    int humanvalue = CardListUtil.sumCMC(human);
                    humanvalue += human.size();
                    humanvalue += CardListUtil.sumAttack(human.getTokens()); 
                    humanvalue += human.getType("Land").size();        // X = total converted mana cost + number of permanents + number of lands + total power of tokens (Human)
                    if (AllZone.Computer_Life.getLife() < 7) { humanvalue += CardListUtil.sumAttack(human); } // in Low Life Emergency X = X + total power of human creatures

                    computer = computer.getValidCards(Tgts);
                    computer = computer.getNotKeyword("Indestructible");
                    int computervalue = CardListUtil.sumCMC(computer);
                    computervalue += computer.size();
                    computervalue += CardListUtil.sumAttack(computer.getTokens()); 
                    computervalue += computer.getType("Land").size();  // Y = total converted mana cost + number of permanents + number of lands + total power of tokens (Computer)
                    
                    // the computer will play the spell if Y < X - 3
                    return  AllZone.Phase.getPhase().equals(Constant.Phase.Main2) && 
                    		(computervalue < humanvalue - 3);
                }

                @Override
                public void resolve() {
                    CardList all = new CardList();
                    all.addAll(AllZone.Human_Play.getCards());
                    all.addAll(AllZone.Computer_Play.getCards());
                    all = all.getValidCards(Tgts);
                    
                    CardListUtil.sortByIndestructible(all);
                    CardListUtil.sortByDestroyEffect(all);
                    
                    for(int i = 0; i < all.size(); i++) {
                        Card c = all.get(i);
                        if(NoRegen[0])
                        	AllZone.GameAction.destroyNoRegeneration(c);
                        else
                        	AllZone.GameAction.destroy(c);
                        
                    }
                    
                    if (!Drawback[0].equals("none"))
                    {
                    	// drawbacks for DestroyAll spells usually involve the
                    	// number of permanents that were actually destroyed
                    	int nDestroyed = 0;
	                    CardList afterAll = new CardList();
	                    afterAll.addAll(AllZone.Human_Play.getCards());
	                    afterAll.addAll(AllZone.Computer_Play.getCards());
	                    afterAll = afterAll.getValidCards(Tgts);
	                    
	                    ArrayList<Integer> slD = new ArrayList<Integer>();
	                    for (int i=0; i<afterAll.size(); i++)
	                    	slD.add(afterAll.get(i).getUniqueNumber());
	                    
	                    for (int i=0; i<all.size(); i++)
	                    {
	                    	if (!slD.contains(all.get(i).getUniqueNumber()))
	                    		nDestroyed++;
	                    }
	                    Log.error("nDestroyed: " + nDestroyed);
	                    CardFactoryUtil.doDrawBack(Drawback[0], nDestroyed, card.getController(), AllZone.GameAction.getOpponent(card.getController()), null, card, null, this);
                    }
                }// resolve()

            }; //SpDstryAll
            
            spDstryAll.setDescription(card.getSpellText());
            card.setText("");
            
            card.addSpellAbility(spDstryAll);            

        }//spDestroyAll


        // Generic bounce target card
        if(hasKeyword(card, "spBounceTgt") != -1) {
            int n = hasKeyword(card, "spBounceTgt");
            
            String parse = card.getKeyword().get(n).toString();
            card.removeIntrinsicKeyword(parse);
            
            String k[] = parse.split(":");
            String Targets = k[1]; // Artifact, Creature, Enchantment, Land, Permanent, White, Blue, Black, Red, Green, Colorless, MultiColor
            // non-Artifact, non-Creature, non-Enchantment, non-Land, non-Permanent,
            //non-White, non-Blue, non-Black, non-Red, non-Green, non-Colorless, non-MultiColor
            final String Tgts[] = Targets.split(",");
                        
            final String Destination = k[2];
            
            final String Drawback[] = {"none"};
            if (k.length > 3)
            {
            	if (k[3].contains("Drawback$")){
                    String kk[] = k[3].split("\\$");
                    Drawback[0] = kk[1];
            	}
            }
            
            final String Selec[] = {"Select a target "};
            String tgtType = "";
            if (Destination.equals("Hand"))
            {
                tgtType = card.getSpellText().substring("Return target ".length());
                int i = tgtType.indexOf(" to its owner's hand.");
                tgtType = tgtType.substring(0, i);
                Selec[0] += tgtType + " to return.";
            }
            else if (Destination.equals("Exile"))
            {
            	tgtType = card.getSpellText().substring("Exile target ".length());
            	int i = tgtType.indexOf(".");
            	tgtType = tgtType.substring(0, i);
            	Selec[0] += tgtType + " to exile.";
            }
            else if (Destination.equals("TopofLibrary"))
            {
            	tgtType = card.getSpellText().substring("Put target ".length());
            	int i = tgtType.indexOf(" on top of its owner's library.");
            	tgtType = tgtType.substring(0, i);
            	Selec[0] += tgtType + " to put on top of the library.";
            }
            else if (Destination.equals("BottomofLibrary"))
            {
            	tgtType = card.getSpellText().substring("Put target ".length());
            	int i = tgtType.indexOf(" on the bottom of its owner's library.");
            	tgtType = tgtType.substring(0, i);
            	Selec[0] += tgtType + " to put on the bottom of the library.";
            }
            else
            {
            	Selec[0] = card.getSpellText();
            }
            
            card.clearSpellAbility();
            
            final SpellAbility spBnceTgt = new Spell(card) { 
                private static final long serialVersionUID = 152897134770L;
                
                @Override
                public boolean canPlayAI() {
                	if (AllZone.Phase.getTurn() <= 3) return false;
                	
                    CardList results = new CardList();
                    CardList choices = getTargets();                    
                    
                    if(choices.size() > 0) {
                        for(int i = 0; i < Tgts.length; i++) {
                            if(Tgts[i].startsWith("Artifact")) {
                                if(CardFactoryUtil.AI_getBestArtifact(choices) != null) results.add(CardFactoryUtil.AI_getBestArtifact(choices));
                            } else if(Tgts[i].startsWith("Creature")) {
                                if(CardFactoryUtil.AI_getBestCreature(choices) != null) results.add(CardFactoryUtil.AI_getBestCreature(choices));
                            } else if(Tgts[i].startsWith("Enchantment")) {
                                if(CardFactoryUtil.AI_getBestEnchantment(choices, card, true) != null) results.add(CardFactoryUtil.AI_getBestEnchantment(
                                        choices, card, true));
                            } else if(Tgts[i].startsWith("Land")) {
                                if(CardFactoryUtil.AI_getBestLand(choices) != null) results.add(CardFactoryUtil.AI_getBestLand(choices));
                            } else if(Tgts[i].startsWith("Permanent")) {
                                if(CardFactoryUtil.AI_getMostExpensivePermanent(choices, card, true) != null) results.add(CardFactoryUtil.AI_getMostExpensivePermanent(
                                        choices, card, true));
                            }
                        }
                    }

                    if(results.size() > 0) {
                        results.shuffle();
                        setTargetCard(results.get(0));
                        return true;
                    }
                    return false;
                }
                
                CardList getTargets() {
                    CardList tmpList = new CardList();
                    tmpList.addAll(AllZone.Human_Play.getCards());
                    tmpList = tmpList.getValidCards(Tgts);
                    tmpList = tmpList.getTargetableCards(card);
                    
                    return tmpList;
                }
                
               @Override
                public void resolve()
                {
                   Card tgtC = getTargetCard();
                   
            	   if(AllZone.GameAction.isCardInPlay(tgtC)
                            && CardFactoryUtil.canTarget(card, tgtC)) 
                    {
                    	if(getTargetCard().isToken())
                    		AllZone.getZone(tgtC).remove(tgtC);
                    	else 
                    	{  
                    		if(Destination.equals("TopofLibrary"))
                    			AllZone.GameAction.moveToTopOfLibrary(tgtC);
                    		else if(Destination.equals("BottomofLibrary")) 
                    			AllZone.GameAction.moveToBottomOfLibrary(tgtC);
                    		else if(Destination.equals("ShuffleIntoLibrary"))
                    		{
                    			AllZone.GameAction.moveToTopOfLibrary(tgtC);
                    			AllZone.GameAction.shuffle(tgtC.getOwner());
                    		}
                    		else if(Destination.equals("Exile"))
                    			AllZone.GameAction.removeFromGame(tgtC); 
                    		else if(Destination.equals("Hand"))
                    		{
                        		PlayerZone hand = AllZone.getZone(Constant.Zone.Hand, tgtC.getOwner());
                       	 		AllZone.GameAction.moveTo(hand, tgtC);
                    		}
                    	}
                    	
                    	if (!Drawback[0].equals("none"))
                    		CardFactoryUtil.doDrawBack(Drawback[0], 0, card.getController(), AllZone.GameAction.getOpponent(card.getController()), tgtC.getController(), card, tgtC, this);
                    }
                }
            }; //SpBnceTgt

            Input InGetTarget = CardFactoryUtil.input_targetValid(spBnceTgt, Tgts, Selec[0]);
            
            card.setSVar("PlayMain1", "TRUE");
            
            spBnceTgt.setBeforePayMana(InGetTarget);
            spBnceTgt.setDescription(card.getSpellText());
            card.setText("");
            card.addSpellAbility(spBnceTgt);
            
            String bbCost = card.getSVar("Buyback");
            if (!bbCost.equals(""))
            {
               SpellAbility bbBnceTgt = spBnceTgt.copy();
               bbBnceTgt.setManaCost(CardUtil.addManaCosts(card.getManaCost(), bbCost));
               
               StringBuilder sb = new StringBuilder();
               sb.append("Buyback ").append(bbCost).append(" (You may pay an additional ").append(bbCost);
               sb.append(" as you cast this spell. If you do, put this card into your hand as it resolves.)");
               bbBnceTgt.setDescription(sb.toString());
               // bbBnceTgt.setDescription("Buyback " + bbCost + "(You may pay an additional " + bbCost + " as you cast this spell. If you do, put this card into your hand as it resolves.)");
               bbBnceTgt.setIsBuyBackAbility(true);
               
               bbBnceTgt.setBeforePayMana(CardFactoryUtil.input_targetValid(bbBnceTgt, Tgts, Selec[0]));
               
               card.addSpellAbility(bbBnceTgt);
            }
        }//spBounceTgt
        
        // Generic bounce when enters the battlefield
        if (hasKeyword(card, "etbBounceTgt") != -1)
        {
        	int n = hasKeyword(card, "etbBounceTgt");
        	
        	String parse = card.getKeyword().get(n).toString();
        	card.removeIntrinsicKeyword(parse);
        	
        	String k[] = parse.split(":");
        	
        	final boolean May[] = {false};
        	if (k[0].contains("May"))
        		May[0] = true;
        	
        	final String Tgts[] = k[1].split(",");
        	
        	final String Destination = k[2];
        	
        	final String Selec[] = {"Select a target "};
        	String tgtType = card.getSpellText();
        	int i = 0;
        	if (Destination.equals("Hand"))
        	{
        		i = tgtType.indexOf("return target ");
        		tgtType = tgtType.substring(i + "return target ".length());
        		i = tgtType.indexOf(" to its owner's hand.");
        		tgtType = tgtType.substring(0, i);
        		Selec[0] += tgtType + " to return.";
        	}
        	else if (Destination.equals("Exile"))
        	{
        		i = tgtType.indexOf("exile target ");
        		tgtType = tgtType.substring(i + "exile target ".length());
        		i = tgtType.indexOf(".");
        		tgtType = tgtType.substring(0, i);
        		Selec[0] += tgtType + " to exile.";
        	}
        	else if (Destination.equals("TopofLibrary"))
        	{
        		i = tgtType.indexOf("put target ".length());
        		tgtType = tgtType.substring(i + "put target ".length());
        		i = tgtType.indexOf(" on top of its owner's library.");
        		tgtType = tgtType.substring(0, i);
        		Selec[0] += tgtType + " to put on top of the library.";
        	}
        	else
        	{
        		Selec[0] = card.getSpellText();
        	}
        	
        	final String Drawback[] = {"none"};
        	if (k.length > 3)
        	{
        		if (k[3].startsWith("Drawback"))
        			Drawback[0] = k[3].substring("Drawback$".length());
        	}
        	
        	final boolean Evoke[] = {false};
        	if (card.getSVar("Evoke").length() > 0)
        		Evoke[0] = true;
        	
        	card.setSVar("PlayMain1", "TRUE");
        	card.clearSpellAbility();
        	
        	// over-rides the default Spell_Permanent
        	// enables the AI to play the card when appropriate
        	SpellAbility spETBBounceTgt = new Spell_Permanent(card)
        	{
                private static final long serialVersionUID = -1548526222769333358L;
                
                @Override
                public boolean canPlayAI() 
                {
                    Random r = new Random();
                	                    
                    CardList hCards = new CardList(AllZone.getZone(Constant.Zone.Play, Constant.Player.Human).getCards());
                    hCards = hCards.getValidCards(Tgts);
                    hCards = hCards.getTargetableCards(card);
                    if (hCards.size() > 0)
                    	return true;
                    
                    CardList cCards = new CardList(AllZone.getZone(Constant.Zone.Play, Constant.Player.Computer).getCards());
                    cCards = cCards.getValidCards(Tgts);
                    cCards = cCards.getTargetableCards(card);
                    if (cCards.size() == 0)
                    	return true;
                    else
                    {
                    	if (r.nextInt(100) > 67)
                    		return true;
                    }
                    
                	return false;
                }
        	};
        	card.addSpellAbility(spETBBounceTgt);
        	
        	// performs the bounce
        	final SpellAbility saBounceTgt = new Ability(card, "0")
        	{
        		@Override
        		public void resolve()
        		{
        			Card tgtC = getTargetCard();
        			if (tgtC == null)
        				return;
        			
        			if (AllZone.GameAction.isCardInPlay(tgtC) && CardFactoryUtil.canTarget(card, tgtC))
        			{
        				if (tgtC.isToken())
        					AllZone.getZone(tgtC).remove(tgtC);
        				else
        				{
        					if (Destination.equals("TopofLibrary"))
        						AllZone.GameAction.moveToTopOfLibrary(tgtC);
        					else if (Destination.equals("ShuffleIntoLibrary"))
        					{
        						AllZone.GameAction.moveToTopOfLibrary(tgtC);
        						AllZone.GameAction.shuffle(tgtC.getOwner());
        					}
        					else if (Destination.equals("Exile"))
        						AllZone.GameAction.removeFromGame(tgtC);
        					else if (Destination.equals("Hand"))
        						AllZone.GameAction.moveToHand(tgtC);
        				}
        				
        				if (!Drawback[0].equals("none"))
        					CardFactoryUtil.doDrawBack(Drawback[0], 0, card.getController(), AllZone.GameAction.getOpponent(card.getController()), tgtC.getController(), card, tgtC, this);
        			}
        		}
        	}; //saBounceTgt
        	saBounceTgt.setStackDescription(card.getName() + " - bounce target.");
        	
        	// when the card enters the battlefield, enable the human to target
        	// or the AI decides on a target
        	Command etbBounceTgt = new Command()
        	{
        		private static final long serialVersionUID = 829702746298938287L;
        		
        		public void execute()
        		{
        			CardList hCards = new CardList(AllZone.Human_Play.getCards());
        			CardList cCards = new CardList(AllZone.Computer_Play.getCards());
        			
        			hCards = hCards.getValidCards(Tgts);
        			hCards = hCards.getTargetableCards(card);
        			cCards = cCards.getValidCards(Tgts);
        			cCards = cCards.getTargetableCards(card);
        			
        			if (hCards.size() > 0 || cCards.size() > 0)
        			{
        				if (card.getController().equals(Constant.Player.Human))
        				{
        					Input inDT = CardFactoryUtil.input_targetValid(saBounceTgt, Tgts, Selec[0]);
        					
        					AllZone.InputControl.setInput(inDT);
        					
        					if (May[0] == true)
        						ButtonUtil.enableOnlyCancel();
        					else
        						ButtonUtil.disableAll();
        				}
        				else
        				{
        					Card c = new Card();
        					CardList dChoices = new CardList();
        					if (hCards.size() > 0)
        					{
        						for (int i=0; i<Tgts.length; i++)
        						{
        							if (Tgts[i].startsWith("Creature"))
        							{
        								c = CardFactoryUtil.AI_getBestCreature(hCards);
        								if (c != null)
        									dChoices.add(c);
        							}
        							
        							CardListUtil.sortByTextLen(hCards);
        							dChoices.add(hCards.get(0));
        							
        							CardListUtil.sortCMC(hCards);
        							dChoices.add(hCards.get(0));
        						}
        						
        						c = dChoices.get(CardUtil.getRandomIndex(dChoices));
        						saBounceTgt.setTargetCard(c);
        						AllZone.Stack.add(saBounceTgt);
        					}
        					else if (cCards.size() > 0 && May[0] == false)
        					{
        						for (int i=0; i<Tgts.length; i++)
        						{
        							if (Tgts[i].startsWith("Creature"))
        							{
        								c = CardFactoryUtil.AI_getWorstCreature(cCards);
        								if (c != null)
        									dChoices.add(c);
        							}
        							
        							CardListUtil.sortByTextLen(cCards);
        							dChoices.add(cCards.get(cCards.size() - 1));
        							
        							CardListUtil.sortCMC(cCards);
        							dChoices.add(cCards.get(cCards.size() - 1));
        						}
        						
        						c = dChoices.get(CardUtil.getRandomIndex(dChoices));
        						saBounceTgt.setTargetCard(c);
        						AllZone.Stack.add(saBounceTgt);
        					}
        				}
        			}
        		}
        	}; // etbBounceTgt
        	card.addComesIntoPlayCommand(etbBounceTgt);
        	
        	// handle SVar:Evoke:{cost}
        	if (Evoke[0] == true)
        	{
        		String EvCost = card.getSVar("Evoke");
        		
        		SpellAbility evBounceTgt = new Spell_Evoke(card, EvCost)
        		{
        			private static final long serialVersionUID = 865327909209183746L;
        			
        			@Override
        			public boolean canPlayAI() {
        				return false;
        			}
        		};
        		card.addSpellAbility(evBounceTgt);
        	}
        } // etbBounceTgt

        // Generic bounce all card
        if(hasKeyword(card, "spBounceAll") != -1) {
            int n = hasKeyword(card, "spBounceAll");
            
            String parse = card.getKeyword().get(n).toString();
            card.removeIntrinsicKeyword(parse);
            
            String k[] = parse.split(":");
            String Targets = k[1]; // Artifact, Creature, Enchantment, Land, Permanent, White, Blue, Black, Red, Green, Colorless, MultiColor
            // non-Artifact, non-Creature, non-Enchantment, non-Land, non-Permanent,
            //non-White, non-Blue, non-Black, non-Red, non-Green, non-Colorless, non-MultiColor
            final String Tgts[] = Targets.split(",");
            
            final String Destination = k[2];
            
            card.clearSpellAbility();
            
            final SpellAbility spBnceAll = new Spell(card) {
                private static final long serialVersionUID = 897326872601L;
                
                @Override
                public boolean canPlayAI() {
                    CardList human = new CardList(AllZone.Human_Play.getCards());
                    CardList computer = new CardList(AllZone.Computer_Play.getCards());
                    
                    human = human.getValidCards(Tgts);
                    int humanvalue = CardListUtil.sumCMC(human);
                    humanvalue += human.getType("Land").size();
                    humanvalue += CardListUtil.sumAttack(human.getTokens());        // X = total converted mana cost + number of lands c (Human)
                    if(!Destination.equals("Hand")) humanvalue += human.size();     // if the Destination is not Hand card advantage counts
                    if(Destination.equals("Hand")) humanvalue += CardListUtil.sumDefense(human.getTokens());  // if the Destination is Hand tokens are more important
                    if (AllZone.Computer_Life.getLife() < 7) { humanvalue += CardListUtil.sumAttack(human); } // in Low Life Emergency X = X + total power of human creatures

                    computer = computer.getValidCards(Tgts);
                    int computervalue = CardListUtil.sumCMC(computer);
                    computervalue += computer.getType("Land").size();
                    computervalue += CardListUtil.sumAttack(computer.getTokens());    // Y = total converted mana cost + number of lands + total power of tokens (Computer)
                    if(!Destination.equals("Hand")) computervalue += computer.size(); // if the Destination is not Hand card advantage counts
                    if(Destination.equals("Hand")) computervalue += CardListUtil.sumDefense(computer.getTokens()); // if the Destination is Hand tokens are more important
                    
                    // the computer will play the spell if Y < X - 2
                    return  AllZone.Phase.getPhase().equals(Constant.Phase.Main2) && 
                    		(computervalue < humanvalue - 2);
                }

                @Override
                public void resolve() {
                    CardList all = new CardList();
                    all.addAll(AllZone.Human_Play.getCards());
                    all.addAll(AllZone.Computer_Play.getCards());
                    all = all.getValidCards(Tgts);
                    
                    for(int i = 0; i < all.size(); i++) {
                        Card c = all.get(i);
                        if(c.isToken()) AllZone.getZone(c).remove(c);
                        else {  
					if(Destination.equals("TopofLibrary")) AllZone.GameAction.moveToTopOfLibrary(c);
					else if(Destination.equals("BottomofLibrary")) AllZone.GameAction.moveToBottomOfLibrary(c);
					else if(Destination.equals("ShuffleIntoLibrary")) {
							AllZone.GameAction.moveToTopOfLibrary(c);
							AllZone.GameAction.shuffle(c.getOwner());
						}
					else if(Destination.equals("Exile")) AllZone.GameAction.removeFromGame(c); 
					else if(Destination.equals("Hand")) {
                            			PlayerZone hand = AllZone.getZone(Constant.Zone.Hand, c.getOwner());
                           	 		AllZone.GameAction.moveTo(hand, c);
						}
				}
                    }
                }// resolve()

            }; //SpBnceAll
            
            spBnceAll.setDescription(card.getSpellText());
            card.setText("");
            
            card.addSpellAbility(spBnceAll);            

        }//spBounceAll

        // Generic Tutor cards
        if(hasKeyword(card, "spTutor") != -1) {
            int n = hasKeyword(card, "spTutor");
            
            String parse = card.getKeyword().get(n).toString();
            card.removeIntrinsicKeyword(parse);
            
            String k[] = parse.split(":");
            final String Targets = k[1]; // Artifact, Creature, Enchantment, Land, Permanent, White, Blue, Black, Red, Green, Colorless, MultiColor
            // non-Artifact, non-Creature, non-Enchantment, non-Land, non-Permanent,
            //non-White, non-Blue, non-Black, non-Red, non-Green, non-Colorless, non-MultiColor
            final String Tgts[] = Targets.split(",");
                        
            final String Destination = k[2];
            card.clearSpellAbility();
            
            final SpellAbility spTtr = new Spell(card) {     
                private static final long serialVersionUID = 209109273165L;
                
                @Override
                public boolean canPlayAI() {
                    CardList list = AllZoneUtil.getPlayerCardsInLibrary(Constant.Player.Computer);
                    list = list.getValidCards(Tgts);
                	  if (list.size() > 0) return true;
                    return false;
                }
                
               @Override
                public void resolve() {
                    String player = card.getController();
                    if(player.equals(Constant.Player.Human)) humanResolve();
                    else computerResolve();   
                }
                
                public void computerResolve() {
                    CardList list = AllZoneUtil.getPlayerCardsInLibrary(Constant.Player.Computer);
                    list = list.getValidCards(Tgts);
                                        
                    if(list.size() != 0) {
                        //comp will just grab the first one it finds, but tries to avoid another copy of same tutor
                    	if (list.getNotName(card.getName()).size() != 0) 
                    	{
                    		list = list.getNotName(card.getName());
                    	}
                        Card c = list.get(0);
                        AllZone.GameAction.shuffle(card.getController());
                        AllZone.Computer_Library.remove(c);
                        if (Destination.equals("Hand")) AllZone.Computer_Hand.add(c);         			//move to hand
                        if (Destination.equals("TopOfLibrary")) AllZone.Computer_Library.add(c, 0); //move to top of library
                        if (Destination.equals("ThirdFromTopOfLibrary")) AllZone.Computer_Library.add(c, 2); //move to third from top of library
                        
                        if (!Targets.startsWith("Card")) {
                        	CardList l = new CardList();
                        	l.add(c);
                        	AllZone.Display.getChoiceOptional("Computer picked:", l.toArray());
                        }
                    }
                }//computerResolve()
                
                public void humanResolve() {
                    CardList list = AllZoneUtil.getPlayerCardsInLibrary(Constant.Player.Human);
                    list = list.getValidCards(Tgts);
                    
                    if(list.size() != 0) {
                        Object o = AllZone.Display.getChoiceOptional("Select a card", list.toArray());
                        
                        AllZone.GameAction.shuffle(card.getController());
                        if(o != null) {
                        	AllZone.Human_Library.remove(o);
                        	if (Destination.equals("Hand")) AllZone.Human_Hand.add((Card) o);         			//move to hand
                            if (Destination.equals("TopOfLibrary")) AllZone.Human_Library.add((Card) o, 0); //move to top of library
                            if (Destination.equals("ThirdFromTopOfLibrary")) AllZone.Human_Library.add((Card) o, 2); //move to third from top of library
                        }
                    }//if
                }//resolve()
            }; // spell ability SpTutorTgt
            
            spTtr.setDescription(card.getSpellText());
            card.setText("");
            card.addSpellAbility(spTtr);
        }//spTutor

        while(hasKeyword(card, "abDrawCards") != -1) {
            int n = hasKeyword(card, "abDrawCards");
            String parse = card.getKeyword().get(n).toString();
            card.removeIntrinsicKeyword(parse);
            
            String k[] = parse.split(":");
    		String tmp = k[0].replace("abDrawCards", "");
    		
    		String[] tmpCost = tmp.split(" ", 2);
            
    		final Target abTgt;
    		if (tmpCost[0].equals(""))
    			abTgt = null;
    		else{
    			abTgt = new Target(tmpCost[0]);
    			abTgt.setValidTgts("player".split(","));
    			abTgt.setVTSelection("Target a player to draw cards");
    		}
    		
       		final Ability_Cost abCost = new Ability_Cost(tmpCost[1], card.getName(), true);
            
            final int NumCards[] = {-1};
            final String NumCardsX[] = {"none"};
            
            if(k[1].matches("X")) {
                String x = card.getSVar(k[1]);
                if(x.startsWith("Count$")) {
                    String kk[] = x.split("\\$");
                    NumCardsX[0] = kk[1];
                }
            } else if(k[1].matches("[0-9][0-9]?")) NumCards[0] = Integer.parseInt(k[1]);
            
            // drawbacks and descriptions
            final String DrawBack[] = {"none"};
            final String spDesc[] = {"none"};
            final String stDesc[] = {"none"};
            if(k.length > 2) {
                if(k[2].contains("Drawback$")) {
                    String kk[] = k[2].split("\\$");
                    DrawBack[0] = kk[1];
                    if(k.length > 3) spDesc[0] = k[3];
                    if(k.length > 4) stDesc[0] = k[4];
                } else {
                    if(k.length > 2) spDesc[0] = k[2];
                    if(k.length > 3) stDesc[0] = k[3];
                }
            }

            spDesc[0] = abCost.toString() + spDesc[0];
            
            final Ability_Activated abDraw = new Ability_Activated(card, abCost, abTgt) {
                private static final long serialVersionUID = -206739246009089196L;
                
                private int               ncards;
                
                public int getNumCards() {
                    if(NumCards[0] != -1) return NumCards[0];
                    
                    if(!NumCardsX[0].equals("none")) return CardFactoryUtil.xCount(card, NumCardsX[0]);
                    
                    return 0;
                }
                
                @Override
                public boolean canPlayAI() {
                    ncards = getNumCards();
                    int handSize = AllZone.getZone(Constant.Zone.Hand, Constant.Player.Computer).size();
                    int hl = AllZone.getZone(Constant.Zone.Library, Constant.Player.Human).size();
                    int cl = AllZone.getZone(Constant.Zone.Library, Constant.Player.Computer).size();
                	
                	if (abCost.getSacCost() && handSize > 2) 	return false;	
                	if (abCost.getSubCounter() && handSize > 3) return false;
                	if (abCost.getLifeCost() && handSize > 2)	return false;
                	
                	if (!ComputerUtil.canPayCost(this))			return false;
                    
                    Random r = new Random();
                    
                    // prevent run-away activations - first time will always return true
                    boolean rr = false;
                    if(r.nextFloat() <= Math.pow(.6667, card.getAbilityUsed())) rr = true;
                    

                    if(((hl - ncards) < 2) && abTgt != null) // attempt to deck the human
                    {
                        setTargetPlayer(Constant.Player.Human);
                        return true && rr;
                    }
                    
                    if(((handSize + ncards) <= 7) && !((cl - ncards) < 1) && (r.nextInt(10) > 4)) {
                        setTargetPlayer(Constant.Player.Computer);
                        return true && rr;
                    }
                    
                    return false;
                }
                
                @Override
				public boolean canPlay(){
                	Cost_Payment pay = new Cost_Payment(abCost, this);
                    return (pay.canPayAdditionalCosts() && CardFactoryUtil.canUseAbility(card) && super.canPlay());
				}
                
                @Override
                public void resolve() {
                    ncards = getNumCards();
                    
                    String TgtPlayer = (abTgt == null) ? card.getController() : getTargetPlayer();
                    
                    for(int i = 0; i < ncards; i++)
                        AllZone.GameAction.drawCard(TgtPlayer);
                    
                    if(!DrawBack[0].equals("none")) CardFactoryUtil.doDrawBack(DrawBack[0], ncards,
                            card.getController(), AllZone.GameAction.getOpponent(card.getController()),
                            TgtPlayer, card, null, this);
                }
            };
            
            abDraw.setDescription(spDesc[0]);
            abDraw.setStackDescription(stDesc[0]);
            
            if (abTgt != null)
            	abDraw.setTarget(abTgt);
            abDraw.setPayCosts(abCost);
            
            card.addSpellAbility(abDraw);
        }
        
        if(hasKeyword(card, "spDrawCards") != -1) {
            int n = hasKeyword(card, "spDrawCards");
            String parse = card.getKeyword().get(n).toString();
            card.removeIntrinsicKeyword(parse);
            
            String k[] = parse.split(":");
            
            final boolean Tgt[] = {false};
            Tgt[0] = k[0].contains("Tgt");
            
            final int NumCards[] = {-1};
            final String NumCardsX[] = {"none"};
            
            if(k[1].matches("X")) {
                String x = card.getSVar(k[1]);
                if(x.startsWith("Count$")) {
                    String kk[] = x.split("\\$");
                    NumCardsX[0] = kk[1];
                }
            } else if(k[1].matches("[0-9][0-9]?")) NumCards[0] = Integer.parseInt(k[1]);
            
            // drawbacks and descriptions
            final String DrawBack[] = {"none"};
            final String spDesc[] = {"none"};
            final String stDesc[] = {"none"};
            if(k.length > 2) {
                if(k[2].contains("Drawback$")) {
                    String kk[] = k[2].split("\\$");
                    DrawBack[0] = kk[1];
                    if(k.length > 3) spDesc[0] = k[3];
                    if(k.length > 4) stDesc[0] = k[4];
                } else {
                    if(k.length > 2) spDesc[0] = k[2];
                    if(k.length > 3) stDesc[0] = k[3];
                }
            }
            
            final SpellAbility spDraw = new Spell(card) {
                private static final long serialVersionUID = -7049779241008089696L;
                
                private int               ncards;
                
                public int getNumCards() {
                    if(NumCards[0] != -1) return NumCards[0];
                    
                    if(!NumCardsX[0].equals("none")) return CardFactoryUtil.xCount(card, NumCardsX[0]);
                    
                    return 0;
                }
                
                @Override
                public boolean canPlayAI() {
                    ncards = getNumCards();
                    int h = AllZone.getZone(Constant.Zone.Hand, Constant.Player.Computer).size();
                    int hl = AllZone.getZone(Constant.Zone.Library, Constant.Player.Human).size();
                    int cl = AllZone.getZone(Constant.Zone.Library, Constant.Player.Computer).size();
                    Random r = new Random();
                    
                    if(((hl - ncards) < 2) && Tgt[0]) // attempt to deck the human if possible
                    {
                        setTargetPlayer(Constant.Player.Human);
                        return true;
                    }
                    
                    if(((h + ncards) <= 7) && !((cl - ncards) < 1) && (r.nextInt(10) > 4)) {
                        setTargetPlayer(Constant.Player.Computer);
                        return true;
                    }
                    
                    return false;
                }
                
                @Override
                public void resolve() {
                    ncards = getNumCards();
                    
                    String TgtPlayer = card.getController();
                    if(Tgt[0]) TgtPlayer = getTargetPlayer();
                    
                    for(int i = 0; i < ncards; i++)
                        AllZone.GameAction.drawCard(TgtPlayer);
                    
                    if(!DrawBack[0].equals("none")) CardFactoryUtil.doDrawBack(DrawBack[0], ncards,
                            card.getController(), AllZone.GameAction.getOpponent(card.getController()), TgtPlayer,
                            card, null, this);
                }
            };
            
            if(Tgt[0]) spDraw.setBeforePayMana(CardFactoryUtil.input_targetPlayer(spDraw));
            
            if(!spDesc[0].equals("none")) spDraw.setDescription(spDesc[0]);
            else spDraw.setDescription("Draw " + NumCards[0] + " cards.");
            
            if(!stDesc[0].equals("none")) spDraw.setStackDescription(stDesc[0]);
            else spDraw.setStackDescription("You draw " + NumCards[0] + " cards.");
            
            card.clearSpellAbility();
            card.addSpellAbility(spDraw);
            
            String bbCost = card.getSVar("Buyback");
            if (!bbCost.equals(""))
            {
               SpellAbility bbDraw = spDraw.copy();
               bbDraw.setManaCost(CardUtil.addManaCosts(card.getManaCost(), bbCost));
               bbDraw.setDescription("Buyback " + bbCost + "(You may pay an additional " + bbCost + " as you cast this spell. If you do, put this card into your hand as it resolves.)");
               bbDraw.setIsBuyBackAbility(true);
               
               if (Tgt[0] == true)
                   bbDraw.setBeforePayMana(CardFactoryUtil.input_targetPlayer(bbDraw));
               
               card.addSpellAbility(bbDraw);
            }

        }//spDrawCards
        
        if (hasKeyword(card, "spLoseLife") != -1)
        {
           int n = hasKeyword(card, "spLoseLife");
           if (n != -1)
           {
              String parse = card.getKeyword().get(n).toString();
              card.removeIntrinsicKeyword(parse);
              
              String k[] = parse.split(":");
              
                final boolean Tgt[] = {false};
                Tgt[0] = k[0].contains("Tgt");

              final int NumLife[] = {-1};
              final String NumLifeX[] = {"none"};
              
                if (k[1].matches("X"))
                {
                   String x = card.getSVar(k[1]);
                   if (x.startsWith("Count$"))
                   {
                      String kk[] = x.split("\\$");
                      NumLifeX[0] = kk[1];
                   }
                }
                else if (k[1].matches("[0-9][0-9]?"))
                   NumLife[0] = Integer.parseInt(k[1]);
               
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
                else
                {
                   if (Tgt[0] == true)
                   {
                      spDesc[0] = "Target player loses " + NumLife[0] + " life.";
                      stDesc[0] =  cardName + " - target player loses life";
                   }
                   else
                   {
                      spDesc[0] = "You lose " + NumLife[0] + " life.";
                      stDesc[0] = cardName + " - you lose life";
                   }
                }
               
               
                final SpellAbility spLoseLife = new Spell(card)
                {
                private static final long serialVersionUID = -8361697584661592092L;

                 public int getNumLife()
                 {
                    if (NumLife[0] != -1)
                       return NumLife[0];

                    if (! NumLifeX[0].equals("none"))
                       return CardFactoryUtil.xCount(card, NumLifeX[0]);
                   
                    return 0;
                 }
                 
                 public boolean canPlayAI()
                 {
                    if (Tgt[0] == true)
                    {
                       setTargetPlayer(Constant.Player.Human);
                       return true;
                    }
                    else   // pretty much just for Stronghold Discipline...
                    {      // assumes there's a good Drawback$ that makes losing life worth it
                       int nlife = getNumLife();
                       if ((AllZone.Computer_Life.getLife() - nlife) > 10)
                          return true;
                       else
                          return false;
                    }
                 }

                public void resolve()
                   {
                      int nlife = getNumLife();
                      String TgtPlayer;

                      if (Tgt[0] == true)
                         TgtPlayer = getTargetPlayer();
                      else
                         TgtPlayer = card.getController();
                     
                      AllZone.GameAction.getPlayerLife(TgtPlayer).subtractLife(nlife,card);
                     
                      if (!DrawBack[0].equals("none"))
                         CardFactoryUtil.doDrawBack(DrawBack[0], nlife, card.getController(), AllZone.GameAction.getOpponent(card.getController()), TgtPlayer, card, null, this);
                   }//resolve()
                };//SpellAbility
               
                if (Tgt[0] == true)
                   spLoseLife.setBeforePayMana(CardFactoryUtil.input_targetPlayer(spLoseLife));
               
                spLoseLife.setDescription(spDesc[0]);
                spLoseLife.setStackDescription(stDesc[0]);
               
                card.clearSpellAbility();
                card.addSpellAbility(spLoseLife);
                
                String bbCost = card.getSVar("Buyback");
                if (!bbCost.equals(""))
                {
                   SpellAbility bbLoseLife = spLoseLife.copy();
                   bbLoseLife.setManaCost(CardUtil.addManaCosts(card.getManaCost(), bbCost));
                   
                   StringBuilder sb = new StringBuilder();
                   sb.append("Buyback ").append(bbCost).append(" (You may pay an additional ").append(bbCost);
                   sb.append(" as you cast this spell. If you do, put this card into your hand as it resolves.)");
                   bbLoseLife.setDescription(sb.toString());
                   // bbLoseLife.setDescription("Buyback " + bbCost + "(You may pay an additional " + bbCost + " as you cast this spell. If you do, put this card into your hand as it resolves.)");
                   bbLoseLife.setIsBuyBackAbility(true);
                   
                   if (Tgt[0] == true)
                       bbLoseLife.setBeforePayMana(CardFactoryUtil.input_targetPlayer(bbLoseLife));
                   
                   card.addSpellAbility(bbLoseLife);
                }
           }
        }//spLoseLife

        if (hasKeyword(card, "abLoseLife") != -1)
        {
        	// abLoseLife{Tgt} <abCost>:<lifeLoss>:{Drawback$<Drawback>}:<SADesc>:<StackDesc>
        	int n = hasKeyword(card, "abLoseLife");
        	if (n != -1)
        	{
        		String parse = card.getKeyword().get(n).toString();
        		card.removeIntrinsicKeyword(parse);
        		
        		String k[] = parse.split(":");
        		String tmp = k[0].replace("abLoseLife", "");
        		
        		String[] tmpCost = tmp.split(" ", 2);
        		
        		final Target abTgt;
        		if (tmpCost[0].equals(""))
        			abTgt = null;
        		else{
        			abTgt = new Target(tmpCost[0]+"V");
        			abTgt.setValidTgts("player".split(","));
        			abTgt.setVTSelection("Target a player to lose life");
        		}
        		
        		final Ability_Cost abCost = new Ability_Cost(tmpCost[1], card.getName(), true);
        		
        		final int NumLife[] = {-1};
        		final String NumLifeX[] = {"none"};
        		
           		int lifePos = 1;
        		int drawbackPos = 2;
        		
        		if (k[lifePos].matches("X"))
        		{
        			String x = card.getSVar(k[1]);
        			if (x.startsWith("Count$"))
        			{
        				String kk[] = x.split("\\$");
        				NumLifeX[0] = kk[1];
        			}
        		}
        		else if (k[lifePos].matches("[0-9][0-9]?"))
        			NumLife[0] = Integer.parseInt(k[1]);
        		
        		// drawbacks and descriptions
                final String DrawBack[] = {"none"};
                final String spDesc[] = {"none"};
                final String stDesc[] = {"none"};
                if (k.length > drawbackPos)
                {
                   if (k[drawbackPos].contains("Drawback$"))
                   {
                      String kk[] = k[drawbackPos].split("\\$");
                      DrawBack[0] = kk[1];
                      if (k.length > drawbackPos+1)
                         spDesc[0] = k[drawbackPos+1];
                      if (k.length > drawbackPos+2)
                         stDesc[0] = k[drawbackPos+2];
                   }
                   else
                   {
                      if (k.length > drawbackPos)
                         spDesc[0] = k[drawbackPos];
                      if (k.length > drawbackPos+1)
                         stDesc[0] = k[drawbackPos+1];
                   }
                }
                else
                {
                   if (abTgt != null)
                   {
                      spDesc[0] = "Target player loses " + NumLife[0] + " life.";
                      stDesc[0] =  cardName + " - target player loses life";
                   }
                   else
                   {
                      spDesc[0] = "You lose " + NumLife[0] + " life.";
                      stDesc[0] = cardName + " - you lose life";
                   }
                }

                final Ability_Activated abLoseLife = new Ability_Activated(card, abCost, abTgt)
                {
	                private static final long serialVersionUID = -936369754466156082L;
	
	                 public int getNumLife()
	                 {
	                    if (NumLife[0] != -1)
	                       return NumLife[0];
	
	                    if (! NumLifeX[0].equals("none"))
	                       return CardFactoryUtil.xCount(card, NumLifeX[0]);
	                   
	                    return 0;
	                 }
	                 
	                 public boolean canPlayAI()
	                 {
	                	 int nlife = getNumLife();
	                	 int life = AllZone.GameAction.getPlayerLife(Constant.Player.Human).getLife();
	                	 if (abCost.getSacCost() && life > nlife + 4) return false;	
	                	 if (abCost.getSubCounter() && life > nlife + 6) return false;
	                	 if (abCost.getLifeCost() && life >= AllZone.GameAction.getPlayerLife(Constant.Player.Computer).getLife())	 
	                		 return false;
	                	 
	                	 Random r = new Random();
	                	 boolean rr = false; // prevent run-away activations - first time will always return true
	                	 if (r.nextFloat() <= Math.pow(.6667, card.getAbilityUsed()))
	                		 rr = true;
	                	 
	                    if (abTgt != null)
	                    {
	                       setTargetPlayer(Constant.Player.Human);
	                       return true && rr;
	                    }
	                    else   
	                    {      // assumes there's a good Drawback$ that makes losing life worth it
	                       if ((AllZone.Computer_Life.getLife() - nlife) >= 10)
	                          return true && rr;
	                       else
	                          return false;
	                    }
	                 }
	
	                public void resolve()
	                {
	                      int nlife = getNumLife();
	                      String TgtPlayer;
	
	                      if (abTgt != null)
	                         TgtPlayer = getTargetPlayer();
	                      else
	                         TgtPlayer = card.getController();
	                     
	                      AllZone.GameAction.getPlayerLife(TgtPlayer).subtractLife(nlife,card);
	                     
	                      if (!DrawBack[0].equals("none"))
	                         CardFactoryUtil.doDrawBack(DrawBack[0], nlife, card.getController(), AllZone.GameAction.getOpponent(card.getController()), TgtPlayer, card, null, this);
	                 }//resolve()
                };//SpellAbility
               
                if (abTgt != null)
                	abLoseLife.setTarget(abTgt);
               
                abLoseLife.setPayCosts(abCost);
                abLoseLife.setDescription(abCost.toString() + spDesc[0]);
                abLoseLife.setStackDescription(stDesc[0]);
               
                card.addSpellAbility(abLoseLife);
        	}
        }// abLoseLife
        
        if (hasKeyword(card, "spGainLife") != -1)
        {
        	int n = hasKeyword(card, "spGainLife");
        	if (n != -1)
        	{
                String parse = card.getKeyword().get(n).toString();
                card.removeIntrinsicKeyword(parse);
                
                String k[] = parse.split(":");
                
                  final boolean Tgt[] = {false};
                  Tgt[0] = k[0].contains("Tgt");

                final int NumLife[] = {-1};
                final String NumLifeX[] = {"none"};
                
                  if (k[1].matches("X"))
                  {
                     String x = card.getSVar(k[1]);
                     if (x.startsWith("Count$"))
                     {
                        String kk[] = x.split("\\$");
                        NumLifeX[0] = kk[1];
                     }
                  }
                  else if (k[1].matches("[0-9][0-9]?"))
                     NumLife[0] = Integer.parseInt(k[1]);
                 
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
                  else
                  {
                     if (Tgt[0] == true)
                     {
                        spDesc[0] = "Target player gains " + NumLife[0] + " life.";
                        stDesc[0] =  cardName + " - target player gains " + NumLife[0] + " life";
                     }
                     else
                     {
                        spDesc[0] = "You gain " + NumLife[0] + " life.";
                        stDesc[0] = cardName + " - you gain " + NumLife[0] + " life";
                     }
                  }

                  final SpellAbility spGainLife = new Spell(card)
                  {
                  private static final long serialVersionUID = -8361697584661592092L;

                   public int getNumLife()
                   {
                      if (NumLife[0] != -1)
                         return NumLife[0];

                      if (! NumLifeX[0].equals("none"))
                         return CardFactoryUtil.xCount(card, NumLifeX[0]);
                     
                      return 0;
                   }
                   
                   public boolean canPlayAI()
                   {
                      if (Tgt[0] == true)
                         setTargetPlayer(Constant.Player.Computer);
                      
                      if (AllZone.Computer_Life.getLife() < 10)
                    	  return true;
                      else
                      {
                    	  Random r = new Random();
                    	  return (r.nextFloat() < .6667);
                      }
                   }

                  public void resolve()
                     {
                        int nlife = getNumLife();
                        String TgtPlayer;

                        if (Tgt[0] == true)
                           TgtPlayer = getTargetPlayer();
                        else
                           TgtPlayer = card.getController();
                       
                        AllZone.GameAction.gainLife(TgtPlayer, nlife);
                       
                        if (!DrawBack[0].equals("none"))
                           CardFactoryUtil.doDrawBack(DrawBack[0], nlife, card.getController(), AllZone.GameAction.getOpponent(card.getController()), TgtPlayer, card, null, this);
                     }//resolve()
                  };//SpellAbility
                 
                  if (Tgt[0] == true)
                     spGainLife.setBeforePayMana(CardFactoryUtil.input_targetPlayer(spGainLife));
                 
                  spGainLife.setDescription(spDesc[0]);
                  spGainLife.setStackDescription(stDesc[0]);
                 
                  card.clearSpellAbility();
                  card.addSpellAbility(spGainLife);
                  
                  String bbCost = card.getSVar("Buyback");
                  if (!bbCost.equals(""))
                  {
                     SpellAbility bbGainLife = spGainLife.copy();
                     bbGainLife.setManaCost(CardUtil.addManaCosts(card.getManaCost(), bbCost));
                     bbGainLife.setDescription("Buyback " + bbCost + "(You may pay an additional " + bbCost + " as you cast this spell. If you do, put this card into your hand as it resolves.)");
                     bbGainLife.setIsBuyBackAbility(true);
                     
                     if (Tgt[0] == true)
                         bbGainLife.setBeforePayMana(CardFactoryUtil.input_targetPlayer(bbGainLife));
                     
                     card.addSpellAbility(bbGainLife);
                  }
        	}
        }//spGainLife

        if (hasKeyword(card, "abGainLife") != -1)
        {
        	int n = hasKeyword(card, "abGainLife");
        	if (n != -1)
        	{
        		String parse = card.getKeyword().get(n).toString();
        		card.removeIntrinsicKeyword(parse);
        		
        		String k[] = parse.split(":");
        		String tmp = k[0].replace("abGainLife", "");
        		
        		String[] tmpCost = tmp.split(" ", 2);
        		
        		int inc = 0;
        		
        		final Target abTgt;
        		if (tmpCost[0].equals(""))
        			abTgt = null;
        		else{
        			abTgt = new Target(tmpCost[0]);
        			abTgt.setValidTgts(k[1].split(","));
        			abTgt.setVTSelection("Target a player to gain life");
        			
        			inc++;
        		}
        		
        		final Ability_Cost abCost = new Ability_Cost(tmpCost[1], card.getName(), true);
        		
        		final int NumLife[] = {-1};
        		final String NumLifeX[] = {"none"};
        		
        		int lifePos = 1 + inc;
        		int drawbackPos = 2 + inc;
        		
        		if (k[lifePos].matches("X"))
        		{
        			String x = card.getSVar(k[lifePos]);
        			if (x.startsWith("Count$"))
        			{
        				String kk[] = x.split("\\$");
        				NumLifeX[0] = kk[1];
        			}
        		}
        		else if (k[lifePos].matches("[0-9][0-9]?"))
        			NumLife[0] = Integer.parseInt(k[lifePos]);
        		
        		// drawbacks and descriptions
                final String DrawBack[] = {"none"};
                final String spDesc[] = {"none"};
                final String stDesc[] = {"none"};
                if (k.length > drawbackPos)
                {
                   if (k[drawbackPos].contains("Drawback$"))
                   {
                      String kk[] = k[drawbackPos].split("\\$");
                      DrawBack[0] = kk[1];
                      if (k.length > drawbackPos+1)
                         spDesc[0] = k[drawbackPos+1];
                      if (k.length > drawbackPos+2)
                         stDesc[0] = k[drawbackPos+2];
                   }
                   else
                   {
                      if (k.length > drawbackPos)
                         spDesc[0] = k[drawbackPos];
                      if (k.length > drawbackPos+1)
                         stDesc[0] = k[drawbackPos+1];
                   }
                }
                else
                {
                   if (abTgt != null)
                   {
                      spDesc[0] = "Target player gains " + NumLife[0] + " life.";
                      stDesc[0] =  cardName + " - target player gains life";
                   }
                   else
                   {
                      spDesc[0] = "You gain " + NumLife[0] + " life.";
                      stDesc[0] = cardName + " - you gain life";
                   }
                }
                final Ability_Activated abGainLife = new Ability_Activated(card, abCost, abTgt)
                {
					private static final long serialVersionUID = -936369754466156082L;
					
					public int getNumLife()
					{
					    if (NumLife[0] != -1)
					       return NumLife[0];
					
					    if (! NumLifeX[0].equals("none"))
					       return CardFactoryUtil.xCount(card, NumLifeX[0]);
					   
					    return 0;
					}
					
					public boolean canPlay(){
                    	Cost_Payment pay = new Cost_Payment(abCost, this);
                        return (pay.canPayAdditionalCosts() && CardFactoryUtil.canUseAbility(card) && super.canPlay());
					}
					 
					public boolean canPlayAI()
					{
						int life = AllZone.GameAction.getPlayerLife(Constant.Player.Computer).getLife();
                    	if (abCost.getSacCost() && life > 5) return false;	
                    	if (abCost.getSubCounter() && life > 5) return false;
                    	if (abCost.getLifeCost() && life > 5)	 return false;
                    	
                    	if (!ComputerUtil.canPayCost(this))
                    		return false;
						
						 Random r = new Random();
						 boolean rr = false; // prevent run-away activations - first time will always return true
					    	 if (r.nextFloat() <= Math.pow(.6667, card.getAbilityUsed()))
					    		 rr = true;
					    	 
					         if (abTgt != null)
					        	 setTargetPlayer(Constant.Player.Computer);
					         
					         if (AllZone.Computer_Life.getLife() < 10)
					        	 return true && rr;
					         else
					        	 return ((r.nextFloat() < .6667) && rr);
					}
					
					public void resolve()
					{
						 int nlife = getNumLife();
						 String TgtPlayer = (abTgt != null) ? getTargetPlayer() : getActivatingPlayer();
	
						 AllZone.GameAction.gainLife(TgtPlayer, nlife);
						 if (!DrawBack[0].equals("none"))
							 CardFactoryUtil.doDrawBack(DrawBack[0], nlife, card.getController(), AllZone.GameAction.getOpponent(card.getController()), TgtPlayer, card, null, this);
					}//resolve()
                };//SpellAbility
               
                abGainLife.setDescription(abCost.toString() + spDesc[0]);
                abGainLife.setStackDescription(stDesc[0]);
                
                if (abTgt != null)
                	abGainLife.setTarget(abTgt);

                abGainLife.setPayCosts(abCost);
                
                card.addSpellAbility(abGainLife);
        	}
        }// abGainLife
        
        
        if(hasKeyword(card, "SearchRebel") != -1) {
            int n = hasKeyword(card, "SearchRebel");
            if(n != -1) {
                String parse = card.getKeyword().get(n).toString();
                card.removeIntrinsicKeyword(parse);
                
                String k[] = parse.split(":");
                final String manacost = k[1];
                
                card.addSpellAbility(CardFactoryUtil.ability_Rebel_Search(card, manacost));
            }
        }//Rebel search
        
        if(hasKeyword(card, "SearchMerc") != -1) {
            int n = hasKeyword(card, "SearchMerc");
            if(n != -1) {
                String parse = card.getKeyword().get(n).toString();
                card.removeIntrinsicKeyword(parse);
                
                String k[] = parse.split(":");
                final String manacost = k[1];
                
                card.addSpellAbility(CardFactoryUtil.ability_Merc_Search(card, manacost));
            }
        }//Merc search
        
        if(hasKeyword(card, "Morph") != -1) {
            int n = hasKeyword(card, "Morph");
            if(n != -1) {
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
        
        if(hasKeyword(card, "Flashback") != -1) {
            int n = hasKeyword(card, "Flashback");
            if(n != -1) {
                String parse = card.getKeyword().get(n).toString();
                //card.removeIntrinsicKeyword(parse);
                
                String k[] = parse.split(":");
                
                final String manacost = k[1];
                
                card.addSpellAbility(CardFactoryUtil.ability_Flashback(card, manacost, "0"));
            }
        }//flashback
        
        if(hasKeyword(card, "Unearth") != -1) {
            int n = hasKeyword(card, "Unearth");
            if(n != -1) {
                String parse = card.getKeyword().get(n).toString();
                //card.removeIntrinsicKeyword(parse);
                
                String k[] = parse.split(":");
                
                final String manacost = k[1];
                
                card.addSpellAbility(CardFactoryUtil.ability_Unearth(card, manacost));
                card.setUnearth(true);
            }
        }//unearth
        
        if(hasKeyword(card, "Madness") != -1) {
            int n = hasKeyword(card, "Madness");
            if(n != -1) {
                String parse = card.getKeyword().get(n).toString();
                //card.removeIntrinsicKeyword(parse);
                
                String k[] = parse.split(":");

                card.setMadness(true);
                card.setMadnessCost(k[1]);
            }
        }//madness
        
        if(hasKeyword(card, "Suspend") != -1) {
        	// Suspend:<TimeCounters>:<Cost>
            int n = hasKeyword(card, "Suspend");
            if(n != -1) {
                String parse = card.getKeyword().get(n).toString();
                card.removeIntrinsicKeyword(parse);
                card.setSuspend(true);
                String k[] = parse.split(":");

                final int timeCounters = Integer.parseInt(k[1]);
                final String cost = k[2];
                card.addSpellAbility(CardFactoryUtil.ability_suspend(card, cost, timeCounters));
            }
        }//madness

        if(hasKeyword(card, "Devour") != -1) {
            int n = hasKeyword(card, "Devour");
            if(n != -1) {
                
                String parse = card.getKeyword().get(n).toString();
                card.removeIntrinsicKeyword(parse);
                
                String k[] = parse.split(":");
                final String magnitude = k[1];
                

                final int multiplier = Integer.parseInt(magnitude);
                //final String player = card.getController();
                final int[] numCreatures = new int[1];
                

                final SpellAbility devour = new Spell(card) {
                    private static final long serialVersionUID = 4888189840817163900L;
                    
                    @Override
                    public void resolve() {
                        int totalCounters = numCreatures[0] * multiplier;
                        card.addCounter(Counters.P1P1, totalCounters);
                        
                        if(card.getName().equals("Skullmulcher")) {
                            for(int i = 0; i < numCreatures[0]; i++) {
                                AllZone.GameAction.drawCard(card.getController());
                            }
                        } else if(card.getName().equals("Caldera Hellion")) {
                            PlayerZone hPlay = AllZone.getZone(Constant.Zone.Play, Constant.Player.Human);
                            PlayerZone cPlay = AllZone.getZone(Constant.Zone.Play, Constant.Player.Computer);
                            CardList creatures = new CardList();
                            
                            creatures.addAll(hPlay.getCards());
                            creatures.addAll(cPlay.getCards());
                            creatures = creatures.getType("Creature");
                            
                            for(int i = 0; i < creatures.size(); i++) {
                                Card crd = creatures.get(i);
                                if(CardFactoryUtil.canDamage(card, crd)) crd.addDamage(3, card);
                            }
                        }
                        
                    }
                    
                    @Override
                    public boolean canPlay() {
                        return AllZone.Phase.getActivePlayer().equals(card.getController()) && card.isFaceDown()
                                && !AllZone.Phase.getPhase().equals("End of Turn")
                                && AllZone.GameAction.isCardInPlay(card);
                    }
                    
                };//devour
                
                Command intoPlay = new Command() {
                    private static final long serialVersionUID = -7530312713496897814L;
                    
                    public void execute() {
                        
                        PlayerZone play = AllZone.getZone(Constant.Zone.Play, card.getController());
                        CardList creats = new CardList(play.getCards());
                        creats = creats.filter(new CardListFilter() {
                            public boolean addCard(Card c) {
                                return c.isCreature() && !c.equals(card);
                            }
                        });
                        
                        //System.out.println("Creats size: " + creats.size());
                        
                        if(card.getController().equals(Constant.Player.Human)) {
                            if (creats.size() > 0)
                            {
	                        	List<Card> selection = AllZone.Display.getChoicesOptional("Select creatures to sacrifice", creats.toArray());
	                            
	                            numCreatures[0] = selection.size();
	                            for(int m = 0; m < selection.size(); m++) {
	                                AllZone.GameAction.sacrifice(selection.get(m));
	                            }
                            }
                            
                        }//human
                        else {
                            int count = 0;
                            for(int i = 0; i < creats.size(); i++) {
                                Card c = creats.get(i);
                                if(c.getNetAttack() <= 1 && c.getNetDefense() <= 2) {
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
                
                devour.setStackDescription(card.getName() + " - gets " + magnitude
                        + " +1/+1 counter(s) per devoured creature.");
                devour.setDescription("Devour " + magnitude);
                card.addSpellAbility(devour);
                card.addComesIntoPlayCommand(intoPlay);
                
                //card.addSpellAbility(CardFactoryUtil.ability_Devour(card, magnitude));
            }
        }//Devour
        
        while(hasKeyword(card, "Modular") != -1) {
            int n = hasKeyword(card, "Modular");
            if(n != -1) {
                String parse = card.getKeyword().get(n).toString();
                card.removeIntrinsicKeyword(parse);
                
                final int m = Integer.parseInt(parse.substring(8));
                String t = card.getSpellText();
                if(!t.equals("")) t += "\r\n";
                card.setText(t
                        + parse
                        + " (This enters the battlefield with "
                        + m
                        + " +1/+1 counters on it. When it's put into a graveyard, you may put its +1/+1 counters on target artifact creature.)");//Erm help? Isn't there a normal way to do this?...
                card.addComesIntoPlayCommand(new Command() {
                    private static final long serialVersionUID = 339412525059881775L;
                    
                    public void execute() {
                        card.addCounter(Counters.P1P1, m);
                    }
                });
                final SpellAbility ability = new Ability(card, "0") {
                    @Override
                    public void resolve() {
                        if(card.getController().equals(Constant.Player.Computer)) {
                            CardList choices = new CardList(AllZone.Computer_Play.getCards()).filter(new CardListFilter() {
                                public boolean addCard(Card c) {
                                    return c.isCreature() && c.isArtifact();
                                }
                            });
                            if(choices.size() != 0) CardFactoryUtil.AI_getBestCreature(choices).addCounter(
                                    Counters.P1P1, getSourceCard().getCounters(Counters.P1P1));
                        } else {
                            final SpellAbility ability = this;
                            AllZone.InputControl.setInput(new Input() {
                                
                                private static final long serialVersionUID = 2322926875771867901L;
                                
                                @Override
                                public void showMessage() {
                                    AllZone.Display.showMessage("Select target artifact creature");
                                    ButtonUtil.enableOnlyCancel();
                                }
                                
                                @Override
                                public void selectButtonCancel() {
                                    stop();
                                }
                                
                                @Override
                                public void selectCard(Card card2, PlayerZone zone) {
                                    if(card2.isCreature() && card2.isArtifact() && zone.is(Constant.Zone.Play)
                                            && CardFactoryUtil.canTarget(ability, card)) {
                                        card2.addCounter(Counters.P1P1, ability.getSourceCard().getCounters(
                                                Counters.P1P1));//combining input and resolve is skirting rules and hacky at best, but non-stackability of destroyCommand Inputs turns into a major problem when the keyword is mainly used during the simultaneous destruction of combat.
                                        stop();
                                    }
                                }
                            });
                        }//else
                    }//resolve()
                };
                
                card.addDestroyCommand(new Command() {
                    private static final long serialVersionUID = 304026662487997331L;
                    
                    public void execute() {
                        ability.setStackDescription("Put " + card.getCounters(Counters.P1P1)
                                + " +1/+1 counter/s from " + card + " on target artifact creature.");
                        AllZone.Stack.push(ability);
                    }
                });
                
            }
            
        }//while shouldModular
        

        int spike = hasKeyword(card, "Spike");
        if(spike != -1) {
            String parse = card.getKeyword().get(spike).toString();
            card.removeIntrinsicKeyword(parse);
            
            final Ability_Cost cost = new Ability_Cost("2 SubCounter<1/P1P1>", card.getName(), true);
            final Target tgt = new Target("TgtC");
            
            final int m = Integer.parseInt(parse.substring(6));
            String t = card.getSpellText();
            if(!t.equals("")) t += "\r\n";
            card.setText(t
                    + parse
                    + " (This enters the battlefield with "
                    + m
                    + " +1/+1 counters on it.)");
            
            card.addComesIntoPlayCommand(new Command() {
                private static final long serialVersionUID = -2292898970576123040L;

                public void execute() {
                    card.addCounter(Counters.P1P1, m);
                }
            });//ComesIntoPlayCommand
            
            final SpellAbility ability = new Ability(card, cost.getMana()) {
                
                @Override
                public boolean canPlay() {
                	Cost_Payment pay = new Cost_Payment(cost, this);
                	return (pay.canPayAdditionalCosts() && CardFactoryUtil.canUseAbility(card) && super.canPlay());
                }//canPlay()
                
                @Override
                public boolean canPlayAI() {
                    return getCreature().size() != 0 && ComputerUtil.canPayCost(this)
                            && !CardFactoryUtil.AI_doesCreatureAttack(card);
                }//canPlayAI()
                
                @Override
                public void chooseTargetAI() {
                    Card best = CardFactoryUtil.AI_getBestCreature(getCreature());
                    setTargetCard(best);
                    card.subtractCounter(Counters.P1P1, 1);
                }//chooseTargetAI()
                
                CardList getCreature() {
                    CardList list = new CardList(AllZone.Computer_Play.getCards());
                    list = list.filter(new CardListFilter() {
                        public boolean addCard(Card c) {
                            return c.isCreature() 
                                    && CardFactoryUtil.canTarget(card, c) 
                                    && c.getNetAttack() > card.getNetAttack()
                                    && c != card;
                        }
                    });
                    return list;
                }//getCreature()
                
                @Override
                public void resolve() {
                    if (AllZone.GameAction.isCardInPlay(getTargetCard())
                            && CardFactoryUtil.canTarget(card, getTargetCard())) {
                        getTargetCard().addCounter(Counters.P1P1, 1);
                    }
                }//resolve()
            };//SpellAbility
            
            StringBuffer sb = new StringBuffer();
            sb.append("Put a +1/+1 counter from ").append(card.getName()).append(" on target creature.");
            
            ability.setStackDescription(sb.toString());
            ability.setDescription("2, Remove a +1/+1 counter: Put a +1/+1 counter on target creature");
            ability.setPayCosts(cost);
            ability.setTarget(tgt);
            
            card.addSpellAbility(ability);
        } // if Spike
        
        int etbCounter = hasKeyword(card, "etbCounter");	// etbCounter:CounterType:CounterAmount
        // enters the battlefield with CounterAmount of CounterType
        if(etbCounter != -1) {
            String parse = card.getKeyword().get(etbCounter).toString();
            card.removeIntrinsicKeyword(parse);
            
            String p[] = parse.split(":");
            final Counters counter = Counters.valueOf(p[1]);
            final int numCounters = Integer.parseInt(p[2]);
              
            StringBuilder sb = new StringBuilder(card.getSpellText());
            if (sb.length() != 0)
            	sb.append("\n");
            
            sb.append(card.getName());
            sb.append(" enters the battlefield with ");
            sb.append(numCounters);
            sb.append(" ");
            sb.append(counter.getName());
            sb.append(" counters on it.");
            
            card.setText(sb.toString());

            card.addComesIntoPlayCommand(new Command() {
                private static final long serialVersionUID = -2292898970576123040L;

                public void execute() {
                    card.addCounter(counter, numCounters);
                }
            });//ComesIntoPlayCommand
        } // if etbCounter
        
        
        // Generic target creature pump
        if(hasKeyword(card, "spPumpTgt") != -1) {
            int n = hasKeyword(card, "spPumpTgt");
            
            String parse = card.getKeyword().get(n).toString();
            card.removeIntrinsicKeyword(parse);
            
            String k[] = parse.split(":");
            
            final int NumAttack[] = {-1138};
            final String AttackX[] = {"none"};
            final int NumDefense[] = {-1138};
            final String DefenseX[] = {"none"};
            final String Keyword[] = {"none"};
            final boolean curse[] = {false};
                
            curse[0] = k[0].contains("Curse");
            
            String ptk[] = k[1].split("/");
            
            if(ptk.length == 1) Keyword[0] = ptk[0];
            
            if(ptk.length >= 2) {
                if(ptk[0].matches("[\\+\\-][XY]")) {
                    String xy = card.getSVar(ptk[0].replaceAll("[\\+\\-]", ""));
                    if(xy.startsWith("Count$")) {
                        String kk[] = xy.split("\\$");
                        AttackX[0] = kk[1];
                        
                        if(ptk[0].contains("-")) if(AttackX[0].contains("/")) AttackX[0] = AttackX[0].replace("/",
                                "/Negative");
                        else AttackX[0] += "/Negative";
                    }
                } else if(ptk[0].matches("[\\+\\-][0-9]")) NumAttack[0] = Integer.parseInt(ptk[0].replace("+", ""));
                
                if(ptk[1].matches("[\\+\\-][XY]")) {
                    String xy = card.getSVar(ptk[1].replaceAll("[\\+\\-]", ""));
                    if(xy.startsWith("Count$")) {
                        String kk[] = xy.split("\\$");
                        DefenseX[0] = kk[1];
                        
                        if(ptk[1].contains("-")) if(DefenseX[0].contains("/")) DefenseX[0] = DefenseX[0].replace(
                                "/", "/Negative");
                        else DefenseX[0] += "/Negative";
                    }
                } else if(ptk[1].matches("[\\+\\-][0-9]")) NumDefense[0] = Integer.parseInt(ptk[1].replace("+", ""));
            }
            
            if(ptk.length == 3) Keyword[0] = ptk[2];
                        
            String dK = Keyword[0];
            if (Keyword[0].contains(" & "))////////////////
            {
            	int amp = Keyword[0].lastIndexOf("&");
            	StringBuffer sbk = new StringBuffer(Keyword[0]);
            	sbk.replace(amp, amp + 1, "and");
            	dK = sbk.toString();
            	dK = dK.replace(" & ", ", ");
            }
            
            final String DrawBack[] = {"none"};
            final String spDesc[] = {"none"};
            final String stDesc[] = {"none"};
            //String d = new String("none");
            StringBuilder sb = new StringBuilder();
            
            if((AttackX[0].equals("none") && !(NumAttack[0] == -1138))
                    && (DefenseX[0].equals("none") && !(NumDefense[0] == -1138)) && Keyword[0].equals("none")) {
                // pt boost
                sb.append("Target creature gets ");
                
                if(NumAttack[0] > 0 || (NumAttack[0] == 0 && NumDefense[0] > 0)) // +0/+1
                sb.append("+");
                else if(NumAttack[0] < 0 || (NumAttack[0] == 0 && NumDefense[0] < 0)) // -0/-1
                sb.append("-");
                
                sb.append(Math.abs(NumAttack[0]) + "/");
                
                if(NumDefense[0] > 0 || (NumDefense[0] == 0 && NumAttack[0] > 0)) // +1/+0
                sb.append("+");
                else if(NumDefense[0] < 0 || (NumDefense[0] == 0 && NumAttack[0] < 0)) // -1/-0
                sb.append("-");
                
                sb.append(Math.abs(NumDefense[0]) + " until end of turn.");
            }
            if((AttackX[0].equals("none") && NumAttack[0] == -1138)
                    && (DefenseX[0].equals("none") && NumDefense[0] == -1138) && !Keyword[0].equals("none")) {
                // k boost
                sb.append("Target creature gains " + dK + " until end of turn.");
            }
            if((AttackX[0].equals("none") && !(NumAttack[0] == -1138))
                    && (DefenseX[0].equals("none") && !(NumDefense[0] == -1138)) && !Keyword[0].equals("none")) {
                // ptk boost
                sb.append("Target creature gets ");
                
                if(NumAttack[0] > 0 || (NumAttack[0] == 0 && NumDefense[0] > 0)) // +0/+1
                sb.append("+");
                else if(NumAttack[0] < 0 || (NumAttack[0] == 0 && NumDefense[0] < 0)) // -0/-1
                sb.append("-");
                
                sb.append(Math.abs(NumAttack[0]) + "/");
                
                if(NumDefense[0] > 0 || (NumDefense[0] == 0 && NumAttack[0] > 0)) // +1/+0
                sb.append("+");
                else if(NumDefense[0] < 0 || (NumDefense[0] == 0 && NumAttack[0] < 0)) // -1/-0
                sb.append("-");
                
                sb.append(Math.abs(NumDefense[0]));
                
                sb.append(" and gains " + dK + " until end of turn.");
            }
            
            if(k.length > 2) {
                if(k[2].contains("Drawback$")) {
                    String kk[] = k[2].split("\\$");
                    DrawBack[0] = kk[1];
                    if(k.length > 3) spDesc[0] = k[3];
                    if(k.length > 4) stDesc[0] = k[4];
                } else {
                    if(k.length > 2) spDesc[0] = k[2];
                    if(k.length > 3) stDesc[0] = k[3];
                }
            } else if(!sb.toString().equals("none")) {
                spDesc[0] = sb.toString();
                stDesc[0] = sb.toString();
            }
            

            SpellAbility spPump = new Spell(card) {
                private static final long serialVersionUID = 42244224L;
                
                private int getNumAttack() {
                    if(NumAttack[0] != -1138) return NumAttack[0];
                    
                    if(!AttackX[0].equals("none")) return CardFactoryUtil.xCount(card, AttackX[0]);
                    
                    return 0;
                }
                
                private int getNumDefense() {
                    if(NumDefense[0] != -1138) return NumDefense[0];
                    
                    if(!DefenseX[0].equals("none")) return CardFactoryUtil.xCount(card, DefenseX[0]);
                    
                    return 0;
                }
                
                @Override
                public boolean canPlayAI() {
                    int defense = getNumDefense();
                    
                    String curPhase = AllZone.Phase.getPhase();
                    if(curPhase.equals(Constant.Phase.Main2) && !(curse[0] && NumDefense[0] < 0))
                    	return false;
                    
            		boolean goodt = false;
            		Card t = new Card();
            		
                    if (curse[0]) {  // Curse means spells with negative effect
                    	CardList list = new CardList(AllZone.Human_Play.getCards());
                        list = list.filter(new CardListFilter() {
                            public boolean addCard(Card c) { 
                                    	return CardFactoryUtil.canTarget(card, c) && c.isCreature(); 
                                }
                        });        
                    	if (NumDefense[0] < 0 && !list.isEmpty()) { // with spells that give -X/-X, compi will try to destroy a creature
                    		list = list.filter(new CardListFilter() {
                                public boolean addCard(Card c) {
                                	if (c.getNetDefense() <= -NumDefense[0] ) return true; // can kill indestructible creatures
                                    return (c.getKillDamage() <= -NumDefense[0] && !c.hasKeyword("Indestructible"));
                                }
                        	}); // leaves all creatures that will be destroyed
                    	} // -X/-X end
                    	if (!list.isEmpty()) {
                    		t = CardFactoryUtil.AI_getBestCreature(list);
                    		goodt = true;
                    	}
                    }
                    else { // no Curse means spell with positive effect
                    	CardList list = getCreatures();
                    	if(!list.isEmpty()) {
                    		while(goodt == false && !list.isEmpty()) {
                    			t = CardFactoryUtil.AI_getBestCreature(list);
                    			if((t.getNetDefense() + defense) > 0) goodt = true;
                    			else list.remove(t);
                    		}
                        }
                    }
                    if(goodt == true) {
                            setTargetCard(t);
                            return true;  
                    }
                    
                    return false;
                }
                
                CardList getCreatures() {
                    CardList list = new CardList(AllZone.Computer_Play.getCards());
                    list = list.filter(new CardListFilter() {
                        public boolean addCard(Card c) {
                            if(c.isCreature()) {
                                if(c.hasSickness() && Keyword[0].contains("Haste")) 
                                	return CardFactoryUtil.canTarget(card, c);
                                
                                return (CardFactoryUtil.AI_doesCreatureAttack(c))
                                        && (CardFactoryUtil.canTarget(card, c))
                                        && (!Keyword[0].equals("none") && !c.hasAnyKeyword(Keyword[0].split(" & ")))
                                        && (!(!c.hasSickness()) && Keyword[0].contains("Haste"));
                                
                            }
                            return false;
                        }
                    });
                    return list;
                }
                
                @Override
                public void resolve() {
                    if(AllZone.GameAction.isCardInPlay(getTargetCard())
                            && CardFactoryUtil.canTarget(card, getTargetCard())) {
                        final Card[] creature = new Card[1];
                        creature[0] = getTargetCard();
                        
                        final int a = getNumAttack();
                        final int d = getNumDefense();
                        
                        final Command untilEOT = new Command() {
                            private static final long serialVersionUID = -42244224L;
                            
                            public void execute() {
                                if(AllZone.GameAction.isCardInPlay(creature[0])) {
                                    creature[0].addTempAttackBoost(-1 * a);
                                    creature[0].addTempDefenseBoost(-1 * d);
                                    
                                    if(!Keyword[0].equals("none"))
                                    {
                                    	String[] kws = Keyword[0].split(" & ");
                                    	for (int i=0; i<kws.length; i++)
                                    		creature[0].removeExtrinsicKeyword(kws[i]);	
                                    }
                                    	
                                }
                            }
                        };
                        
                        creature[0].addTempAttackBoost(a);
                        creature[0].addTempDefenseBoost(d);
                        if(!Keyword[0].equals("none"))
                        {
                        	String[] kws = Keyword[0].split(" & ");
                        	for (int i=0; i<kws.length; i++)
                        		creature[0].addExtrinsicKeyword(kws[i]);
                        }
                        
                        AllZone.EndOfTurn.addUntil(untilEOT);
                        
                        if(!DrawBack[0].equals("none")) CardFactoryUtil.doDrawBack(DrawBack[0], 0,
                                card.getController(), AllZone.GameAction.getOpponent(card.getController()),
                                creature[0].getController(), card, creature[0], this);
                    }
                }//resolve
            };//SpellAbility
            
            spPump.setBeforePayMana(CardFactoryUtil.input_targetCreature(spPump));
            spPump.setDescription(spDesc[0]);
            spPump.setStackDescription(stDesc[0]);
            
            card.setSVar("PlayMain1", "TRUE");
            
            card.clearSpellAbility();
            card.addSpellAbility(spPump);
            
            String bbCost = card.getSVar("Buyback");
            if (!bbCost.equals(""))
            {
               SpellAbility bbPump = spPump.copy();
               bbPump.setManaCost(CardUtil.addManaCosts(card.getManaCost(), bbCost));
               bbPump.setDescription("Buyback " + bbCost + "(You may pay an additional " + bbCost + " as you cast this spell. If you do, put this card into your hand as it resolves.)");
               bbPump.setIsBuyBackAbility(true);
               
               bbPump.setBeforePayMana(CardFactoryUtil.input_targetCreature(bbPump));
               
               card.addSpellAbility(bbPump);
            }

        }
        
        if(hasKeyword(card, "spRaiseDead") != -1) {
            int n = hasKeyword(card, "spRaiseDead");
            if(n != -1) {
                String parse = card.getKeyword().get(n).toString();
                card.removeIntrinsicKeyword(parse);
                String k[] = parse.split(":"); // charm descriptions will appear at k[2] and k[3]
                final String kk[] = k[1].split("/"); // numCreatures = kk[0], other fields = kk[1] through kk[2]
                int numFieldsKK = kk.length;
                final int numCreatures = Integer.parseInt(kk[0]);
                boolean quantifier = false;
                String tmpTgt = "Creature";
                
                for(int i = 2; i <= numFieldsKK; i++) {
                    if(kk[(i - 1)].equals("Some")) {
                        quantifier = true;
                    } else // can only be a specific creature type at his time, Goblin for goblin creatures and Tarfire
                    {
                        tmpTgt = kk[i - 1];
                    }
                }
                
                final String targetTypeToReturn = tmpTgt;
                final boolean weReturnUpTo = quantifier;
                final String spDesc[] = {"none"};
                final String stDesc[] = {"none"};
                
                if(k.length > 2) spDesc[0] = k[2];
                if(k.length > 3) stDesc[0] = k[3];
                
                final SpellAbility spell = new Spell(card) {
                    private static final long serialVersionUID = 6938982619919149188L;
                    
                    @Override
                    public boolean canPlayAI() {
                        return getGraveCreatures().size() >= numCreatures;
                    }
                    
                    CardList targets;
                    
                    @Override
                    public void chooseTargetAI() {
                        CardList grave = getGraveCreatures();
                        targets = new CardList();
                        
                        if(targetTypeToReturn.equals("Creature")) {
                            for(int i = 0; i < numCreatures; i++) {
                                Card c = CardFactoryUtil.AI_getBestCreature(grave);
                                targets.add(c);
                                grave.remove(c);
                            }
                        } else // this is for returning Goblins and Tarfire (and Changelings ?)
                        {
                            for(int i = 0; i < numCreatures; i++) {
                                Card c = CardFactoryUtil.getRandomCard(grave); // getRandomCard(grave);
                                targets.add(c);
                                grave.remove(c);
                            }
                        }
                    }
                    
                    @Override
                    public void resolve() {
                        if(card.getController().equals(Constant.Player.Human)) {
                            CardList grave = getGraveCreatures();
                            targets = new CardList();
                            
                            if(weReturnUpTo) // this is for spells which state Return up to X target creature card
                            {
                                for(int i = 0; i < numCreatures; i++) {
                                    Card c = AllZone.Display.getChoiceOptional("Select card", grave.toArray());
                                    targets.add(c);
                                    grave.remove(c);
                                }
                            }

                            else if(grave.size() > numCreatures) // this is for spells which state Return target creature card
                            for(int i = 0; i < numCreatures; i++) {
                                Card c = AllZone.Display.getChoice("Select card", grave.toArray());
                                targets.add(c);
                                grave.remove(c);
                            }
                            else targets = grave;
                        }
                        
                        PlayerZone grave = AllZone.getZone(Constant.Zone.Graveyard, card.getController());
                        PlayerZone hand = AllZone.getZone(Constant.Zone.Hand, card.getController());
                        for(Card c:targets)
                            if(AllZone.GameAction.isCardInZone(c, grave)) AllZone.GameAction.moveTo(hand, c);
                    }//resolve()
                    
                    @Override
                    public boolean canPlay() {
                    	if (weReturnUpTo) return true;
                        return getGraveCreatures().size() >= numCreatures;
                    }
                    
                    CardList getGraveCreatures() {
                        PlayerZone grave = AllZone.getZone(Constant.Zone.Graveyard, card.getController());
                        CardList list = new CardList(grave.getCards());
                        String cardController = card.getController();
                        
                        if(cardController.equals("Human") || (cardController.equals("Computer"))
                                && (targetTypeToReturn.equals("Creature"))) {
                            list = list.getType(targetTypeToReturn);
                        } else // prevent the computer from using a Boggart Birth Rite to return a Boggart Birth Rite
                        {
                            CardList tempList;
                            tempList = list.getType(targetTypeToReturn);
                            list = new CardList();
                            for(int i = 0; i < tempList.size(); i++) {
                                if(!cardName.equals(tempList.get(i).getName())) {
                                    list.add(tempList.get(i));
                                }
                            }
                        }
                        return list;
                    }
                };//SpellAbility
                
                if(spDesc[0].equals("none")) // create the card descriptiopn
                {
                    spDesc[0] = ("Return ");
                    if(weReturnUpTo) {
                        spDesc[0] = (spDesc[0] + "up to ");
                    }
                    if(numCreatures > 1) {
                        spDesc[0] = (spDesc[0] + numCreatures + " ");
                    }
                    spDesc[0] = (spDesc[0] + "target ");
                    if(targetTypeToReturn.equals("Creature")) {
                        spDesc[0] = (spDesc[0] + "creature");
                    } else {
                        spDesc[0] = (spDesc[0] + targetTypeToReturn);
                    }
                    if(numCreatures > 1) {
                        spDesc[0] = (spDesc[0] + "s");
                    }
                    spDesc[0] = (spDesc[0] + " card");
                    if(numCreatures > 1) {
                        spDesc[0] = (spDesc[0] + "s");
                    }
                    spDesc[0] = (spDesc[0] + " from your graveyard to your hand.");
                }
                
                if(stDesc[0].equals("none")) // create the card stack descriptiopn
                {
                    stDesc[0] = (card.getName() + " - returns target card");
                    if(numCreatures > 1) {
                        stDesc[0] = (stDesc[0] + "s");
                    }
                    stDesc[0] = (stDesc[0] + " from " + card.getController() + "'s graveyard to "
                            + card.getController() + "'s hand.");
                }
                
                spell.setDescription(spDesc[0]);
                spell.setStackDescription(stDesc[0]);
                card.clearSpellAbility();
                card.addSpellAbility(spell);
                
                card.setSVar("PlayMain1", "TRUE");
            }
        }// spRaiseDead
        

        while(shouldManaAbility(card) != -1) {
            int n = shouldManaAbility(card);
            if(n != -1) {
                String parse = card.getKeyword().get(n).toString();
                card.removeIntrinsicKeyword(parse);
                final Ability_Mana ability = new Ability_Mana(card, parse) {
                    private static final long serialVersionUID = -113811381138L;
                    
                    @Override
                    public boolean canPlayAI() {
                        return false;
                    }
                };
                //ability.setDescription(parse);
                card.addSpellAbility(ability);
            }
        }
        
        while(hasKeyword(card,"paintap") != -1)
        {
           String toParse = card.getIntrinsicKeyword().get(hasKeyword(card,"paintap"));
           card.removeIntrinsicKeyword(toParse);
           String[] splitkeyword = toParse.split(":");

           final int amountHurt = Integer.parseInt(splitkeyword[1]);
           final String manaGenerated = splitkeyword[2];
           StringBuilder sb = new StringBuilder();
           sb.append("tap: add ").append(manaGenerated).append(" to your mana pool. CARDNAME deals ").append(amountHurt).append(" damage to you.");
           final String abilityDescriptionString = sb.toString();
           
           // final Ability_Mana addMana = new Ability_Mana(card, "tap: add " + manaGenerated + " to your mana pool. CARDNAME deals " + amountHurt + " damage to you.") {
           
           final Ability_Mana addMana = new Ability_Mana(card, abilityDescriptionString) {
                 private static final long serialVersionUID = -259088242789L;
                 
                 @Override
                 public void resolve()
                 {
                    AllZone.GameAction.getPlayerLife(getController()).subtractLife(amountHurt,card);
                    super.resolve();
                 }
                 
                 @Override
                 public String mana() {
                 return manaGenerated;
              }
                                                
            };
            card.addSpellAbility(addMana);
        }//paintap
        
        ////////////////////////////////////////////////////////////////
        
        if (card.getKeyword().contains("When CARDNAME enters the battlefield, draw a card.") || 
        		card.getKeyword().contains("When CARDNAME enters the battlefield, draw two cards.")) {
            
            final SpellAbility ability = new Ability(card, "0") {
                @Override
                public void resolve() {
                	int drawCardsNum = 1;
                	if (card.getKeyword().contains("When CARDNAME enters the battlefield, draw two cards.")) {
                		drawCardsNum = 2;
                	}
                	for (int i = 0; i < drawCardsNum; i++) {
                        AllZone.GameAction.drawCard(card.getController());
                	}//for loop
                }//resolve()
            };//SpellAbility
            Command intoPlay = new Command() {
                
                private static final long serialVersionUID = 1707519783018941582L;
                
                public void execute() {
                	StringBuilder sb = new StringBuilder();
                    sb.append(card.getName());
                    sb.append(" - ");
                    sb.append(card.getController());
                    sb.append(" draws ");
                    if (card.getKeyword().contains("When CARDNAME enters the battlefield, draw a card.")) {
                    	sb.append("a card.");
                    } else sb.append("two cards.");
                	
                    ability.setStackDescription(sb.toString());
                    AllZone.Stack.add(ability);
                }
            };
            card.addComesIntoPlayCommand(intoPlay);
        }//end if
        
        if (card.getKeyword().contains("When CARDNAME enters the battlefield, you may search your library for up to three cards named CARDNAME, reveal them, and put them into your hand. If you do, shuffle your library."))
        {
        	final SpellAbility ability = new Ability(card, "0")
        	{
        		public void resolve()
        		{
        			CardList list = AllZoneUtil.getPlayerCardsInLibrary(card.getController());
        			list = list.getName(cardName);
        			if (list.size() < 1)
        				return;
        			
        			if (card.getController().equals(Constant.Player.Human))
        			{
        				List<Card> selection = AllZone.Display.getChoicesOptional("Select cards to fetch from Library", list.toArray());
        				
                        for(int m = 0; m < selection.size(); m++) {
                        	AllZone.Human_Library.remove(selection.get(m));
        					AllZone.Human_Hand.add(selection.get(m));
                        }
        			}
        			else
        			{
        				for (Card c:list)
        				{
        					AllZone.Computer_Library.remove(c);
        					AllZone.Computer_Hand.add(c);
        				}
        				
        				StringBuilder sb = new StringBuilder();
        				
        				sb.append("Opponent fetches and reveals ");
        				sb.append(list.size());
        				sb.append(" copies of ");
        				sb.append(cardName);
        				sb.append(".");
        				JOptionPane.showMessageDialog(null, sb.toString(), "", JOptionPane.INFORMATION_MESSAGE);
        				
        				
        			}
        			AllZone.GameAction.shuffle(card.getController());
        		}
        		
        	};
        	
        	Command intoPlay = new Command() {
				private static final long serialVersionUID = 8374287903074067063L;

				public void execute() {
                	StringBuilder sb = new StringBuilder();
                    sb.append(card.getName());
                    sb.append(" - ");
                    sb.append(" search your library for up to three cards named ");
                    sb.append(cardName);
                    sb.append(", reveal them, and put them into your hand. If you do, shuffle your library.");
                	
                    ability.setStackDescription(sb.toString());
                    AllZone.Stack.add(ability);
                }
            };
            card.addComesIntoPlayCommand(intoPlay);
        }
        
        if(hasKeyword(card, "spMakeToken") != -1) {
        	int n = hasKeyword(card, "spMakeToken");
        	String parse = card.getKeyword().get(n).toString();
            card.removeIntrinsicKeyword(parse);
            final String[] k = parse.split("<>");
            final String numString = k[1].equals("X") ? card.getSVar("X") : k[1];
            final boolean xString = k[1].equals("X") ? true : false;
            final String name = k[2];
            final String imageName = k[3];
            final String controllerString = k[4];
            final String manaCost = k[5];
            final String[] types = k[6].split(";");
            final int attack = Integer.valueOf(k[7]);
            final int defense = Integer.valueOf(k[8]);
            final String[] keywords = k[9].split(";");
            
            SpellAbility spell = new Spell(card) {
				private static final long serialVersionUID = -5286946184688616830L;
				
				@Override
				public boolean canPlayAI() {
					if(xString && CardFactoryUtil.xCount(card, numString) > 0) {
						return false;
					}
					else{
						return true;
					}
				}
				
				@Override
            	public void resolve() {
					String controller = (controllerString.equals("Controller") ? card.getController() : AllZone.GameAction.getOpponent(card.getController()));
					if(keywords[0].equals("None")) keywords[0] = "";
					
					int num = xString ? CardFactoryUtil.xCount(card, numString) : Integer.valueOf(numString);
		            for(int i = 0; i < num; i ++ ){
                    	CardFactoryUtil.makeToken(name, imageName, controller, manaCost, types, attack, defense, keywords);
                    }
            	}
            };
            card.clearSpellAbility();
            card.addSpellAbility(spell);
        }//end MakeToken
        
        if(hasKeyword(card, "abMakeToken") != -1) {
        	int n = hasKeyword(card, "abMakeToken");
        	
        	String parse = card.getKeyword().get(n).toString();
        	card.removeIntrinsicKeyword(parse);
            
        	final String[] k = parse.split("<>");
            final String numString = k[1].equals("X") ? card.getSVar("X") : k[1];
            final boolean xString = k[1].equals("X") ? true : false;
            final String name = k[2];
            final String imageName = k[3];
            final String controllerString = k[4];
            final String manaCost = k[5];
            final String[] types = k[6].split(";");
            final int attack = Integer.valueOf(k[7]);
            final int defense = Integer.valueOf(k[8]);
            final String[] keywords = k[9].split(";");
            final String abDesc = k[10];
            
            String fullCost = k[0].substring(11);
            final Ability_Cost abCost = new Ability_Cost(fullCost, card.getName(), true);
        	          	
        	final String spDesc[] = {"none"};
        	spDesc[0] = abCost.toString()+ " - " + abDesc;
        	
        	final SpellAbility abMakeToken = new Ability_Activated(card, abCost, null) {
				private static final long serialVersionUID = -1415539558367883075L;
				
				public boolean canPlayAI() {
                    if (!ComputerUtil.canPayCost(this)) return false;
                    if(xString && CardFactoryUtil.xCount(card, numString) == 0) {
						return false;
					}
					else {
						Random r = new Random();
	                    boolean rr = false;
	                    if( r.nextFloat() <= Math.pow(.6667, card.getAbilityUsed()) ) {
	                    	rr = true;
	                    }
	                    return rr;
					}
        		}
				
				@Override
				public boolean canPlay(){
					Cost_Payment pay = new Cost_Payment(abCost, this);
					return !card.isSick() && (pay.canPayAdditionalCosts() && CardFactoryUtil.canUseAbility(card) && super.canPlay());
				}
        		
				@Override
                public void resolve() {
        			String controller = (controllerString.equals("Controller") ? card.getController() : AllZone.GameAction.getOpponent(card.getController()));
					if(keywords[0].equals("None")) keywords[0] = "";
					
					int num = xString ? CardFactoryUtil.xCount(card, numString) : Integer.valueOf(numString);
		            for(int i = 0; i < num; i ++ ){
                    	CardFactoryUtil.makeToken(name, imageName, controller, manaCost, types, attack, defense, keywords);
                    }
                }
        	};//end abMakeToken ability
        	abMakeToken.setDescription(spDesc[0]);
        	abMakeToken.setStackDescription(card+" - "+abDesc);
        	card.addSpellAbility(abMakeToken);
        	card.setSVar("PlayMain1", "TRUE");
        }// end abMakeToken keyword
        
        if(hasKeyword(card, "etbMakeToken") != -1) {
        	int n = hasKeyword(card, "etbMakeToken");
        	String parse = card.getKeyword().get(n).toString();
            card.removeIntrinsicKeyword(parse);
            final String[] k = parse.split("<>");
            final String numString = k[1].equals("X") ? card.getSVar("X") : k[1];
            final boolean xString = k[1].equals("X") ? true : false;
            final String name = k[2];
            final String imageName = k[3];
            final String controllerString = k[4];
            final String manaCost = k[5];
            final String[] types = k[6].split(";");
            final int attack = Integer.valueOf(k[7]);
            final int defense = Integer.valueOf(k[8]);
            final String[] keywords = k[9].split(";");
            final String abDesc = k[10];
            
            final SpellAbility ability = new Ability(card, "0") {
				@Override
				public boolean canPlayAI() {
					if(xString && CardFactoryUtil.xCount(card, numString) > 0) {
						return false;
					}
					else{
						return true;
					}
				}
				
				@Override
            	public void resolve() {
					String controller = (controllerString.equals("Controller") ? card.getController() : AllZone.GameAction.getOpponent(card.getController()));
					if(keywords[0].equals("None")) keywords[0] = "";
					
					int num = xString ? CardFactoryUtil.xCount(card, numString) : Integer.valueOf(numString);
		            for(int i = 0; i < num; i ++ ){
                    	CardFactoryUtil.makeToken(name, imageName, controller, manaCost, types, attack, defense, keywords);
                    }
            	}
            };
            
            final Command cip = new Command() {
				private static final long serialVersionUID = 903342987065874979L;

				public void execute() {
            		ability.setStackDescription(card+" - "+abDesc);
            		AllZone.Stack.add(ability);
            	}
            };
            card.addComesIntoPlayCommand(cip);
        }//end etbMakeToken
        
        // Generic tap target ___ activated ability
        //abTapTgt {Ability_Cost}:{Valid Targets}:{Description}
        if (hasKeyword(card, "abTapTgt") != -1)
        {
        	int n = hasKeyword(card, "abTapTgt");

        	String parse = card.getKeyword().get(n).toString();
        	card.removeIntrinsicKeyword(parse);

        	String k[] = parse.split(":");

        	String tmpCost = k[0].substring(8);
        	final Ability_Cost abCost = new Ability_Cost(tmpCost, card.getName(), true);

        	final Target tapTargets = new Target("TgtV");
        	final String Tgts[] = k[1].split(",");
        	tapTargets.setValidTgts(Tgts);
        	final String abDesc[] = {"none"};
        	abDesc[0] = k[2];

        	String tmpDesc = abDesc[0].substring(11);
        	int i = tmpDesc.indexOf(".");
        	tmpDesc = tmpDesc.substring(0, i);
        	tapTargets.setVTSelection("Select target " + tmpDesc + " to tap.");

        	abDesc[0] = abCost.toString() + abDesc[0];

        	final SpellAbility AbTapTgt = new Ability_Activated(card, abCost, tapTargets) {
        		private static final long serialVersionUID = 2794477584289098775L;

        		@Override
        		public boolean canPlayAI() {
        			//TODO: perhaps this can borrow from the Crowd Favorites AI...
        			if (!ComputerUtil.canPayCost(this))
        				return false;

        			CardList hCards = getTargets();

        			Random r = new Random();
        			boolean rr = false;
        			if (r.nextFloat() <= Math.pow(.6667, card.getAbilityUsed()))
        				rr = true;

        			if(hCards.size() > 0) {
        				Card c = null;
        				CardList dChoices = new CardList();

        				for(int i = 0; i < Tgts.length; i++) {
        					if (Tgts[i].startsWith("Creature")) {
        						c = CardFactoryUtil.AI_getBestCreature(hCards);
        						if (c != null)
        							dChoices.add(c);
        					}

        					CardListUtil.sortByTextLen(hCards);
        					dChoices.add(hCards.get(0));

        					CardListUtil.sortCMC(hCards);
        					dChoices.add(hCards.get(0));
        				}

        				c = dChoices.get(CardUtil.getRandomIndex(dChoices));
        				setTargetCard(c);

        				return rr;
        			}

        			return false;
        		}

        		CardList getTargets() {
        			CardList tmpList = AllZoneUtil.getPlayerCardsInPlay(Constant.Player.Human);
        			tmpList = tmpList.getValidCards(Tgts);
        			tmpList = tmpList.getTargetableCards(card);
        			return tmpList;
        		}

        		@Override
        		public boolean canPlay() {
        			Cost_Payment pay = new Cost_Payment(abCost, this);
        			return (pay.canPayAdditionalCosts() && CardFactoryUtil.canUseAbility(card) && super.canPlay());
        		}

        		@Override
        		public void resolve() {
        			Card tgtC = getTargetCard();
        			if(AllZone.GameAction.isCardInPlay(tgtC)
        					&& CardFactoryUtil.canTarget(card, tgtC)) {
        				tgtC.tap();
        			}
        		}
        	}; //AbTapTgt

        	AbTapTgt.setDescription(abDesc[0]);
        	card.addSpellAbility(AbTapTgt);
        	card.setSVar("PlayMain1", "TRUE");
        }//End abTapTgt
        
        // Generic tap target ___ activated ability
        //abTapAll {Ability_Cost}:{Valid Targets}:{Description}
        if (hasKeyword(card, "abTapAll") != -1)
        {
        	int n = hasKeyword(card, "abTapAll");

        	String parse = card.getKeyword().get(n).toString();
        	card.removeIntrinsicKeyword(parse);

        	String k[] = parse.split(":");

        	String tmpCost = k[0].substring(8);
        	final Ability_Cost abCost = new Ability_Cost(tmpCost, card.getName(), true);

        	//final Target tapTargets = new Target("TgtV");
        	String Targets = k[1];
        	final String Tgts[] = Targets.split(",");
        	//tapTargets.setValidTgts(Tgts);
        	final String abDesc[] = {"none"};
        	final String stackDesc[] = {"none"};
        	stackDesc[0] = k[2];

        	String tmpDesc = stackDesc[0].substring(8);
        	int i = tmpDesc.indexOf(".");
        	tmpDesc = tmpDesc.substring(0, i);
        	//tapTargets.setVTSelection("Select target " + tmpDesc + " to tap.");

        	abDesc[0] = abCost.toString() + stackDesc[0];

        	final SpellAbility AbTapAll = new Ability_Activated(card, abCost, null) {
				private static final long serialVersionUID = 2161279836590135215L;

				@Override
        		public boolean canPlayAI() {
        			if (!ComputerUtil.canPayCost(this))
        				return false;

        			CardList hCards = getTargets();

        			Random r = new Random();
        			boolean rr = false;
        			if (r.nextFloat() <= Math.pow(.6667, card.getAbilityUsed()))
        				rr = true;

        			if(hCards.size() > 0) {
        				CardList human = hCards.filter(new CardListFilter() {
        					public boolean addCard(Card c) {
        						return c.getController().equals(Constant.Player.Human);
        					}
        				});
        				CardList compy = hCards.filter(new CardListFilter() {
        					public boolean addCard(Card c) {
        						return c.getController().equals(Constant.Player.Human);
        					}
        				});
        				if(human.size() > compy.size()) {
        					return rr;
        				}
        			}
        			return false;
        		}

        		private CardList getTargets() {
        			CardList tmpList = AllZoneUtil.getCardsInPlay();
        			tmpList = tmpList.getValidCards(Tgts);
        			tmpList = tmpList.getTargetableCards(card);
        			return tmpList;
        		}

        		@Override
        		public boolean canPlay() {
        			Cost_Payment pay = new Cost_Payment(abCost, this);
        			return (pay.canPayAdditionalCosts() && CardFactoryUtil.canUseAbility(card) && super.canPlay());
        		}

        		@Override
        		public void resolve() {
        			CardList tgts = getTargets();
        			for(Card c:tgts) {
        				if(AllZone.GameAction.isCardInPlay(c)
        						&& CardFactoryUtil.canTarget(card, c)) {
        					c.tap();
        				}
        			}
        		}
        	}; //AbTapTgt

        	AbTapAll.setDescription(abDesc[0]);
        	AbTapAll.setStackDescription(card.getName()+" - "+stackDesc[0]);
        	card.addSpellAbility(AbTapAll);
        	card.setSVar("PlayMain1", "TRUE");
        }//End abTapAll
        
     // Generic untap target ___ activated ability
        //abUntapTgt {Ability_Cost}:{Valid Targets}:{Description}
        if (hasKeyword(card, "abUntapTgt") != -1)
        {
        	int n = hasKeyword(card, "abUntapTgt");

        	String parse = card.getKeyword().get(n).toString();
        	card.removeIntrinsicKeyword(parse);

        	String k[] = parse.split(":");

        	String tmpCost = k[0].substring(10);
        	final Ability_Cost abCost = new Ability_Cost(tmpCost, card.getName(), true);

        	final Target untapTargets = new Target("TgtV");
        	final String Tgts[] = k[1].split(",");
        	untapTargets.setValidTgts(Tgts);
        	final String abDesc[] = {"none"};
        	abDesc[0] = k[2];

        	String tmpDesc = abDesc[0].substring(13);
        	int i = tmpDesc.indexOf(".");
        	tmpDesc = tmpDesc.substring(0, i);
        	untapTargets.setVTSelection("Select target " + tmpDesc + " to untap.");

        	abDesc[0] = abCost.toString() + abDesc[0];

        	final SpellAbility AbUntapTgt = new Ability_Activated(card, abCost, untapTargets) {
				private static final long serialVersionUID = 6286367744794697322L;

				@Override
        		public boolean canPlayAI() {
        			if (!ComputerUtil.canPayCost(this))
        				return false;

        			CardList hCards = getTargets();

        			Random r = new Random();
        			boolean rr = false;
        			if (r.nextFloat() <= Math.pow(.6667, card.getAbilityUsed()))
        				rr = true;

        			if(hCards.size() > 0) {
        				Card c = null;
        				CardList dChoices = new CardList();

        				for(int i = 0; i < Tgts.length; i++) {
        					if (Tgts[i].startsWith("Creature")) {
        						c = CardFactoryUtil.AI_getBestCreature(hCards);
        						if (c != null)
        							dChoices.add(c);
        					}

        					CardListUtil.sortByTextLen(hCards);
        					dChoices.add(hCards.get(0));

        					CardListUtil.sortCMC(hCards);
        					dChoices.add(hCards.get(0));
        				}

        				c = dChoices.get(CardUtil.getRandomIndex(dChoices));
        				setTargetCard(c);

        				return rr;
        			}

        			return false;
        		}

        		CardList getTargets() {
        			CardList tmpList = AllZoneUtil.getPlayerCardsInPlay(Constant.Player.Human);
        			tmpList = tmpList.getValidCards(Tgts);
        			tmpList = tmpList.getTargetableCards(card);
        			return tmpList;
        		}

        		@Override
        		public boolean canPlay() {
        			Cost_Payment pay = new Cost_Payment(abCost, this);
        			return (pay.canPayAdditionalCosts() && CardFactoryUtil.canUseAbility(card) && super.canPlay());
        		}

        		@Override
        		public void resolve() {
        			Card tgtC = getTargetCard();
        			if(AllZone.GameAction.isCardInPlay(tgtC)
        					&& CardFactoryUtil.canTarget(card, tgtC)) {
        				tgtC.untap();
        			}
        		}
        	}; //AbTapTgt

        	AbUntapTgt.setDescription(abDesc[0]);
        	card.addSpellAbility(AbUntapTgt);
        }//End abUntapTgt
        	
        //******************************************************************
        //************** Link to different CardFactories ******************* 
        Card card2 = null;
        if(card.getType().contains("Creature")) {
            card2 = CardFactory_Creatures.getCard(card, cardName, owner, this);
        } else if(card.getType().contains("Aura")) {
            card2 = CardFactory_Auras.getCard(card, cardName, owner);
        } else if(card.getType().contains("Equipment")) {
            card2 = CardFactory_Equipment.getCard(card, cardName, owner);
        } else if(card.getType().contains("Planeswalker")) {
            card2 = CardFactory_Planeswalkers.getCard(card, cardName, owner);
        } else if(card.getType().contains("Land")) {
            card2 = CardFactory_Lands.getCard(card, cardName, owner);
        } else if (card.getType().contains("Instant")) {
        	card2 = CardFactory_Instants.getCard(card, cardName, owner);
        } else if (card.getType().contains("Sorcery")) {
        	card2 = CardFactory_Sorceries.getCard(card, cardName, owner);
        }
        
        if (card2 != null)
        	return postFactoryKeywords(card2);
        
        //*************** START *********** START **************************
        else if(cardName.equals("Pyrohemia")) {
            SpellAbility ability = new Ability(card, "R") {
                @Override
                public boolean canPlayAI() {
                    CardList human = new CardList(AllZone.Human_Play.getCards());
                    CardList computer = new CardList(AllZone.Computer_Play.getCards());
                    
                    human = human.getType("Creature");
                    computer = computer.getType("Creature");
                    
                    return AllZone.Computer_Life.getLife() > 2 && !(human.size() == 0 && 0 < computer.size());
                }
                
                @Override
                public void resolve() {
                    //get all creatures
                    CardList list = new CardList();
                    list.addAll(AllZone.Human_Play.getCards());
                    list.addAll(AllZone.Computer_Play.getCards());
                    list = list.getType("Creature");
                    
                    for(int i = 0; i < list.size(); i++) {
                        if(CardFactoryUtil.canDamage(card, list.get(i))) list.get(i).addDamage(1, card);
                    }
                    
                    AllZone.Human_Life.subtractLife(1,card);
                    AllZone.Computer_Life.subtractLife(1,card);
                }//resolve()
            };//SpellAbility
            ability.setDescription("R: Pyrohemia deals 1 damage to each creature and each player.");
            ability.setStackDescription(card + " deals 1 damage to each creature and each player.");
            
            card.clearSpellAbility();
            card.addSpellAbility(new Spell_Permanent(card) {
                private static final long serialVersionUID = 5596915641671666843L;
                
                @Override
                public boolean canPlayAI() {
                    //get all creatures
                    CardList list = new CardList();
                    list.addAll(AllZone.Human_Play.getCards());
                    list.addAll(AllZone.Computer_Play.getCards());
                    list = list.getType("Creature");
                    
                    return 0 < list.size();
                }
            });
            
            card.addSpellAbility(ability);
            
            card.setSVar("PlayMain1", "TRUE");
        }
        //*************** END ************ END **************************
        

        //*************** START *********** START **************************
        else if(cardName.equals("Pestilence")) {
            SpellAbility ability = new Ability(card, "B") {
                @Override
                public boolean canPlayAI() {
                    CardList human = new CardList(AllZone.Human_Play.getCards());
                    CardList computer = new CardList(AllZone.Computer_Play.getCards());
                    
                    human = human.getType("Creature");
                    computer = computer.getType("Creature");
                    
                    return AllZone.Computer_Life.getLife() > 2 && !(human.size() == 0 && 0 < computer.size());
                }
                
                @Override
                public void resolve() {
                    //get all creatures
                    CardList list = new CardList();
                    list.addAll(AllZone.Human_Play.getCards());
                    list.addAll(AllZone.Computer_Play.getCards());
                    list = list.getType("Creature");
                    
                    for(int i = 0; i < list.size(); i++) {
                        if(CardFactoryUtil.canDamage(card, list.get(i))) list.get(i).addDamage(1, card);
                    }
                    
                    AllZone.Human_Life.subtractLife(1,card);
                    AllZone.Computer_Life.subtractLife(1,card);
                }//resolve()
            };//SpellAbility
            ability.setDescription("B: Pestilence deals 1 damage to each creature and each player.");
            ability.setStackDescription(card + " deals 1 damage to each creature and each player.");
            
            card.clearSpellAbility();
            card.addSpellAbility(new Spell_Permanent(card) {
                private static final long serialVersionUID = -4163089323122672307L;
                
                @Override
                public boolean canPlayAI() {
                    //get all creatures
                    CardList list = new CardList();
                    list.addAll(AllZone.Human_Play.getCards());
                    list.addAll(AllZone.Computer_Play.getCards());
                    list = list.getType("Creature");
                    
                    return 0 < list.size();
                }
            });
            
            card.addSpellAbility(ability);
            
            card.setSVar("PlayMain1", "TRUE");
        }
        //*************** END ************ END **************************
        
        //*************** START *********** START **************************
        else if(cardName.equals("Bridge from Below")) {
            SpellAbility spell = new Spell_Permanent(card) {
                private static final long serialVersionUID = 7254358703158629514L;
                
                @Override
                public boolean canPlayAI() {
                    return false;
                }
            };
            card.clearSpellAbility();
            card.addSpellAbility(spell);
        }
        //*************** END ************ END *************************
        
        //*************** START *********** START **************************
        else if(cardName.equals("Nevinyrral's Disk")) {
            SpellAbility summoningSpell = new Spell_Permanent(card) {
                private static final long serialVersionUID = -8859376851358601934L;
                
                @Override
                public boolean canPlayAI() {
                    boolean nevinyrralInPlay = false;
                    
                    CardList inPlay = new CardList();
                    inPlay.addAll(AllZone.Computer_Play.getCards());
                    for(int i = 0; i < inPlay.size(); ++i) {
                        if(inPlay.getCard(i).getName().equals("Nevinyrral's Disk")) {
                            nevinyrralInPlay = true;
                        }
                    }
                    return !nevinyrralInPlay && (0 < CardFactoryUtil.AI_getHumanCreature(card, false).size());
                }
            };
            card.clearSpellAbility();
            card.addSpellAbility(summoningSpell);
            
            card.addComesIntoPlayCommand(new Command() {
                private static final long serialVersionUID = -2504426622672629123L;
                
                public void execute() {
                    card.tap();
                }
            });
            final SpellAbility ability = new Ability_Tap(card, "1") {
                private static final long serialVersionUID = 4175577092552330100L;
                
                @Override
                public void resolve() {
                    CardList all = new CardList();
                    all.addAll(AllZone.Human_Play.getCards());
                    all.addAll(AllZone.Computer_Play.getCards());
                    all = filter(all);
                    
                    for(int i = 0; i < all.size(); i++)
                        AllZone.GameAction.destroy(all.get(i));
                }
                
                private CardList filter(CardList list) {
                    return list.filter(new CardListFilter() {
                        public boolean addCard(Card c) {
                            return c.isArtifact() || c.isCreature() || c.isEnchantment();
                        }
                    });
                }//filter()
                
                @Override
                public boolean canPlayAI() {
                    CardList human = new CardList(AllZone.Human_Play.getCards());
                    CardList computer = new CardList(AllZone.Computer_Play.getCards());
                    
                    human = human.getType("Creature");
                    computer = computer.getType("Creature");
                    
                    //the computer will at least destroy 2 more human creatures
                    return  AllZone.Phase.getPhase().equals(Constant.Phase.Main2) && 
                    		(computer.size() < human.size() - 1 || AllZone.Computer_Life.getLife() < 7);
                }
            };//SpellAbility
            card.addSpellAbility(ability);
            ability.setDescription("1, tap: Destroy all artifacts, creatures, and enchantments.");
            ability.setStackDescription("Destroy all artifacts, creatures, and enchantments.");
        }//*************** END ************ END **************************
        

        //*************** START *********** START **************************
        else if(cardName.equals("Conspiracy") || cardName.equals("Cover of Darkness")
                || cardName.equals("Door of Destinies") || cardName.equals("Engineered Plague")
                || cardName.equals("Shared Triumph") || cardName.equals("Belbe's Portal")
                || cardName.equals("Steely Resolve")) {
            final String[] input = new String[1];
            final String player = card.getController();
            
            final SpellAbility ability = new Ability(card, "0") {
                @Override
                public void resolve() {
                    if(player.equals(Constant.Player.Human)) {
                        input[0] = JOptionPane.showInputDialog(null, "Which creature type?", "Pick type",
                                JOptionPane.QUESTION_MESSAGE);
                        
                        if(!CardUtil.isCreatureType(input[0])) input[0] = "";
                        //TODO: some more input validation, case-sensitivity, etc.
                        
                        input[0] = input[0].trim(); //this is to prevent "cheating", and selecting multiple creature types,eg "Goblin Soldier"
                    } else {
                        String chosenType = CardFactoryUtil.chooseCreatureTypeAI(card);
                        if(!chosenType.equals("")) input[0] = chosenType;
                        else input[0] = "Sliver"; //what to put here for the AI???
                    }
                    
                    card.setChosenType(input[0]);
                }
            };//ability
            Command intoPlay = new Command() {
                private static final long serialVersionUID = 5634360316643996274L;
                
                public void execute() {
                    ability.setStackDescription("When " + card.getName()
                            + " comes into play, choose a creature type.");
                    AllZone.Stack.add(ability);
                }
            };
            card.addComesIntoPlayCommand(intoPlay);
            
        }//*************** END ************ END **************************
        

        //*************** START *********** START **************************
        else if(cardName.equals("Sarpadian Empires, Vol. VII")) {
            
            final String[] choices = {"Citizen", "Camarid", "Thrull", "Goblin", "Saproling"};
            
            final String player = card.getController();
            
            final SpellAbility ability = new Ability(card, "0") {
                @Override
                public void resolve() {
                    String type = "";
                    String imageName = "";
                    String color = "";
                    
                    if(player.equals(Constant.Player.Computer)) {
                        type = "Thrull";
                        imageName = "B 1 1 Thrull";
                        color = "B";
                    } else if(player.equals(Constant.Player.Human)) {
                        Object q = AllZone.Display.getChoiceOptional("Select type of creature", choices);
                        if(q != null){
	                        if(q.equals("Citizen")) {
	                            type = "Citizen";
	                            imageName = "W 1 1 Citizen";
	                            color = "W";
	                        } else if(q.equals("Camarid")) {
	                            type = "Camarid";
	                            imageName = "U 1 1 Camarid";
	                            color = "U";
	                        } else if(q.equals("Thrull")) {
	                            type = "Thrull";
	                            imageName = "B 1 1 Thrull";
	                            color = "B";
	                        } else if(q.equals("Goblin")) {
	                            type = "Goblin";
	                            imageName = "R 1 1 Goblin";
	                            color = "R";
	                        } else if(q.equals("Saproling")) {
	                            type = "Saproling";
	                            imageName = "G 1 1 Saproling";
	                            color = "G";
	                        }
                        }
                    }
                    card.setChosenType(type);
                    
                    final String t = type;
                    final String in = imageName;
                    final String col = color;
                    //card.setChosenType(input[0]);
                    
                    final Ability_Tap a1 = new Ability_Tap(card, "3") {
                        
                        private static final long serialVersionUID = -2114111483117171609L;
                        
                        @Override
                        public void resolve() {
                            CardFactoryUtil.makeToken(t, in, card, col, new String[] {"Creature", t}, 1, 1,
                                    new String[] {""});
                        }
                        
                    };
//    			a1.setDescription("3, Tap: Put a 1/1 creature token of the chosen color and type onto the battlefield.");
                    a1.setStackDescription(card.getName() + " - " + card.getController() + " puts a 1/1" + t
                            + " token into play");
                    card.addSpellAbility(a1);
                }
            };//ability
            Command intoPlay = new Command() {
                private static final long serialVersionUID = 7202704600935499188L;
                
                public void execute() {
                    ability.setStackDescription("As Sarpadian Empires, Vol. VII enters the battlefield, choose white Citizen, blue Camarid, black Thrull, red Goblin, or green Saproling.");
                    AllZone.Stack.add(ability);
                }
            };
            card.setText("As Sarpadian Empires, Vol. VII enters the battlefield, choose white Citizen, blue Camarid, black Thrull, red Goblin, or green Saproling.\r\n"
                    + "3, Tap: Put a 1/1 creature token of the chosen color and type onto the battlefield.\r\n"
                    + card.getText()); // In the slight chance that there may be a need to add a note to this card.
            card.addComesIntoPlayCommand(intoPlay);
            
        }//*************** END ************ END **************************		
        
        //*************** START *********** START **************************
        else if(cardName.equals("Eternity Vessel")) {
            Command intoPlay = new Command() {
                private static final long serialVersionUID = 7202704600935499188L;
                
                public void execute() {
                	int player = 0;
                	if(card.getController() == "Human") {
                		player = AllZone.Human_Life.getLife();
                	} else {
                		player = AllZone.Computer_Life.getLife();               		
                	}
                	card.addCounter(Counters.CHARGE, player);
                }
            };
            card.addComesIntoPlayCommand(intoPlay);          
        }//*************** END ************ END **************************
        
        /* converted to keyword
        //*************** START *********** START **************************
        else if(cardName.equals("Dragon Roost")) {
            final SpellAbility ability = new Ability(card, "5 R R") {
                @Override
                public void resolve() {
                    CardFactoryUtil.makeToken("Dragon", "R 5 5 Dragon", card, "R", new String[] {
                            "Creature", "Dragon"}, 5, 5, new String[] {"Flying"});
                }//resolve()
            };
            ability.setDescription("5RR: Put a 5/5 red Dragon creature token with flying into play.");
            ability.setStackDescription("Dragon Roost - Put a 5/5 red Dragon creature token with flying into play.");
            card.addSpellAbility(ability);
        }//*************** END ************ END **************************
        */
        
        /* converted to keyword
        //*************** START *********** START **************************
        else if(cardName.equals("The Hive")) {
            final SpellAbility ability = new Ability_Tap(card, "5") {
                private static final long serialVersionUID = -1091111822316858416L;
                
                @Override
                public void resolve() {
                    CardFactoryUtil.makeToken("Wasp", "C 1 1 Wasp", card, "", new String[] {
                            "Artifact", "Creature", "Insect"}, 1, 1, new String[] {"Flying"});
                }//resolve()
            };
            ability.setDescription("5, tap: Put a 1/1 Insect artifact creature token with flying named Wasp into play.");
            ability.setStackDescription("The Hive - Put a 1/1 token with flying into play.");
            card.addSpellAbility(ability);
        }//*************** END ************ END **************************
        */
        /*
        //*************** START *********** START **************************
        else if(cardName.equals("Mobilization")) {
            final SpellAbility ability = new Ability(card, "2 W") {
                @Override
                public void resolve() {
                    CardFactoryUtil.makeToken("Soldier", "W 1 1 Soldier", card, "W", new String[] {
                            "Creature", "Soldier"}, 1, 1, new String[] {""});
                }//resolve()
            };
            ability.setDescription("2W: Put a 1/1 white Soldier creature token into play.");
            ability.setStackDescription("Mobilization - Put a 1/1 Soldier token into play.");
            card.addSpellAbility(ability);
            
        }//*************** END ************ END **************************
        */
        /* converted to keyword
        //*************** START *********** START **************************
        else if(cardName.equals("Centaur Glade")) {
            final SpellAbility ability = new Ability(card, "2 G G") {
                @Override
                public void resolve() {
                    CardFactoryUtil.makeToken("Centaur", "G 3 3 Centaur", card, "G", new String[] {
                            "Creature", "Centaur"}, 3, 3, new String[] {""});
                }//resolve()
            };
            ability.setDescription("2GG: Put a 3/3 green Centaur creature token into play.");
            ability.setStackDescription("Centaur Glade - Put a 3/3 token into play.");
            card.addSpellAbility(ability);
        }//*************** END ************ END **************************
        */

        //*************** START *********** START **************************
        else if(cardName.equals("Oblivion Ring")) {
                    	
        	final CommandReturn getPerm = new CommandReturn() {
                public Object execute() {
                    //get all creatures
                    CardList tempList = new CardList();
                    tempList.addAll(AllZone.Human_Play.getCards());
                    tempList.addAll(AllZone.Computer_Play.getCards());
                    
                    CardList list = new CardList();
                    
                    for(int i = 0; i < tempList.size(); i++) {
                        if(tempList.get(i).isPermanent() && !tempList.get(i).isLand()
                                && CardFactoryUtil.canTarget(card, tempList.get(i))) list.add(tempList.get(i));
                    }
                    
                    //remove "this card"
                    list.remove(card);
                    
                    return list;
                }
            };//CommandReturn
            
            final SpellAbility abilityComes = new Ability(card, "0") {
                @Override
                public void resolve() {
                    if(AllZone.GameAction.isCardInPlay(getTargetCard())
                            && CardFactoryUtil.canTarget(card, getTargetCard())) {
                        AllZone.GameAction.removeFromGame(getTargetCard());
                    }
                }//resolve()
            };
            
            final Input inputComes = new Input() {
                private static final long serialVersionUID = -3613946694360326887L;
                
                @Override
                public void showMessage() {
                    CardList choice = (CardList) getPerm.execute();
                    
                    stopSetNext(CardFactoryUtil.input_targetSpecific(abilityComes, choice,
                            "Select target permanent to remove from the game", true, false));
                    ButtonUtil.disableAll();//to disable the Cancel button
                }
            };
            Command commandComes = new Command() {
                private static final long serialVersionUID = -6250376920501373535L;
                
                public void execute() {
                    CardList perm = (CardList) getPerm.execute();
                    String s = card.getController();
                    if(perm.size() == 0) return;
                    else if(s.equals(Constant.Player.Human)) AllZone.InputControl.setInput(inputComes);
                    else //computer
                    {
                        Card target;
                        
                        //try to target human creature
                        CardList human = CardFactoryUtil.AI_getHumanCreature(card, true);
                        target = CardFactoryUtil.AI_getBestCreature(human);//returns null if list is empty
                        
                        // try to target human permanent
                        if(target == null) {
                            int convertedCost = 0;
                            CardList tempList = new CardList();
                            tempList.addAll(AllZone.Human_Play.getCards());
                            
                            for(int i = 0; i < tempList.size(); i++) {
                                if(tempList.get(i).isPermanent()
                                        && !tempList.get(i).isLand()
                                        && CardFactoryUtil.canTarget(card, tempList.get(i))
                                        && (CardUtil.getConvertedManaCost(tempList.get(i).getManaCost()) > convertedCost)) {
                                    target = tempList.get(i);
                                    convertedCost = CardUtil.getConvertedManaCost(tempList.get(i).getManaCost());
                                }
                            }
                        }
                        
                        //target something cheaper (manacost 0?) instead:
                        if(target == null) {
                            CardList humanPerms = new CardList();
                            humanPerms.addAll(AllZone.Human_Play.getCards());
                            humanPerms = humanPerms.filter(new CardListFilter() {
                                public boolean addCard(Card c) {
                                    return c.isPermanent() && !c.isLand() && CardFactoryUtil.canTarget(card, c);
                                }
                            });
                            
                            if(humanPerms.size() > 0) target = humanPerms.get(0);
                        }
                        
                        if(target == null) {
                            //must target computer creature
                            CardList computer = new CardList(AllZone.Computer_Play.getCards());
                            computer = computer.getType("Creature");
                            computer.remove(card);
                            
                            computer.shuffle();
                            if(computer.size() != 0) target = computer.get(0);
                            else target = card;
                        }
                        abilityComes.setTargetCard(target);
                        AllZone.Stack.add(abilityComes);
                    }//else
                }//execute()
            };//CommandComes
            Command commandLeavesPlay = new Command() {
                private static final long serialVersionUID = 6997038208952910355L;
                
                public void execute() {
                    Object o = abilityComes.getTargetCard();
                    if(o == null || ((Card) o).isToken() || !AllZone.GameAction.isCardRemovedFromGame((Card) o)) return;
                    
                    SpellAbility ability = new Ability(card, "0") {
                        @Override
                        public void resolve() {
                            //copy card to reset card attributes like attack and defense
                            Card c = abilityComes.getTargetCard();
                            if(!c.isToken()) {
                                c = AllZone.CardFactory.dynamicCopyCard(c);
                                c.setController(c.getOwner());
                                
                                PlayerZone play = AllZone.getZone(Constant.Zone.Play, c.getOwner());
                                PlayerZone removed = AllZone.getZone(Constant.Zone.Removed_From_Play, c.getOwner());
                                removed.remove(c);
                                if (c.isTapped())
                                	c.untap();
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
            
            card.setSVar("PlayMain1", "TRUE");
            
            card.clearSpellAbility();
            card.addSpellAbility(new Spell_Permanent(card) {
                private static final long serialVersionUID = -3250095291930182087L;
                
                @Override
                public boolean canPlayAI() {
                    Object o = getPerm.execute();
                    if(o == null) return false;
                    
                    CardList cList = new CardList(AllZone.Human_Play.getCards());
            		cList = cList.filter(new CardListFilter()
            		{
            			public boolean addCard(Card crd)
            			{
            				return CardFactoryUtil.canTarget(card, crd) && crd.isPermanent() && !crd.isLand();
            			}
            		});
                    
                    CardList cl = (CardList) getPerm.execute();
                    return (o != null) && cList.size() > 0 && cl.size() > 0 && AllZone.getZone(getSourceCard()).is(Constant.Zone.Hand);
                }
            });
            
        }//*************** END ************ END **************************
        

        //*************** START *********** START **************************
        else if(cardName.equals("Oubliette")) {
            final SpellAbility enchantment = new Spell(card) {
                private static final long serialVersionUID = -6751177094537759827L;
                
                @Override
                public void resolve() {
                    if(AllZone.GameAction.isCardInPlay(getTargetCard())
                            && CardFactoryUtil.canTarget(card, getTargetCard())) {
                        AllZone.GameAction.removeFromGame(getTargetCard());
                        
                        //put permanent into play
                        Card c = getSourceCard();
                        AllZone.getZone(Constant.Zone.Play, c.getController()).add(c);
                    }
                }//resolve()
                
                @Override
                public boolean canPlayAI() {
                    //try to target human creature
                    CardList human = CardFactoryUtil.AI_getHumanCreature(card, true);
                    Card target = CardFactoryUtil.AI_getBestCreature(human);//returns null if list is empty
                    
                    if(target == null) return false;
                    else {
                        setTargetCard(target);
                        return true;
                    }
                }//canPlayAI()
            };//SpellAbility enchantment
            
            /*

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
            */

            Command commandLeavesPlay = new Command() {
                private static final long serialVersionUID = -2535098005246027777L;
                
                public void execute() {
                    Object o = enchantment.getTargetCard();
                    if(o == null || ((Card) o).isToken() || !AllZone.GameAction.isCardRemovedFromGame((Card) o)) return;
                    
                    SpellAbility ability = new Ability(card, "0") {
                        @Override
                        public void resolve() {
                            //copy card to reset card attributes like attack and defense
                            Card c = enchantment.getTargetCard();
                            if(!c.isToken()) {
                                c = AllZone.CardFactory.copyCard(c);
                                c.setController(c.getOwner());
                                
                                PlayerZone play = AllZone.getZone(Constant.Zone.Play, c.getOwner());
                                play.add(c);
                            }
                        }//resolve()
                    };//SpellAbility
                    ability.setStackDescription(card.getName() + " - returning creature to play");
                    AllZone.Stack.add(ability);
                }//execute()
            };//Command
            card.addLeavesPlayCommand(commandLeavesPlay);
            
            card.clearSpellAbility();
            card.addSpellAbility(enchantment);
            
            card.setSVar("PlayMain1", "TRUE");
            
            enchantment.setBeforePayMana(CardFactoryUtil.input_targetCreature(enchantment));
        }//*************** END ************ END **************************
        

        //*************** START *********** START **************************
        else if(cardName.equals("Test Destroy")) {
            final SpellAbility spell = new Spell(card) {
                private static final long serialVersionUID = 6637283804612570910L;
                
                @Override
                public boolean canPlayAI() {
                    return false;
                }
                
                @Override
                public void resolve() {
                    AllZone.GameAction.destroy(getTargetCard());
                }//resolve()
            };
            
            card.clearSpellAbility();
            spell.setBeforePayMana(CardFactoryUtil.input_targetType(spell, "All"));
            card.addSpellAbility(spell);
        }//*************** END ************ END **************************
        

        //*************** START *********** START **************************
        else if(cardName.equals("Take Possession")) {
            final SpellAbility spell = new Spell(card) {
                private static final long serialVersionUID = -7359291736123492910L;
                
                @Override
                public boolean canPlayAI() {
                    return 0 < CardFactoryUtil.AI_getHumanCreature(card, true).size();
                }
                
                @Override
                public void chooseTargetAI() {
                    Card best = CardFactoryUtil.AI_getBestCreature(CardFactoryUtil.AI_getHumanCreature(card, true));
                    setTargetCard(best);
                }
                
                @Override
                public void resolve() {
                    Card c = getTargetCard();
                    c.setController(card.getController());
                    
                    ((PlayerZone_ComesIntoPlay) AllZone.Human_Play).setTriggers(false);
                    ((PlayerZone_ComesIntoPlay) AllZone.Computer_Play).setTriggers(false);
                    
                    PlayerZone from = AllZone.getZone(c);
                    PlayerZone to = AllZone.getZone(Constant.Zone.Play, card.getController());
                    
                    from.remove(c);
                    to.add(c);
                    
                    ((PlayerZone_ComesIntoPlay) AllZone.Human_Play).setTriggers(true);
                    ((PlayerZone_ComesIntoPlay) AllZone.Computer_Play).setTriggers(true);
                    
                }//resolve()
            };
            
            card.clearSpellAbility();
            spell.setBeforePayMana(CardFactoryUtil.input_targetType(spell, "All"));
            card.addSpellAbility(spell);
            
            card.setSVar("PlayMain1", "TRUE");
        }//*************** END ************ END **************************
        

        //*************** START *********** START **************************
        else if(cardName.equals("Belbe's Portal")) {
            final SpellAbility ability = new Ability_Tap(card, "3") {
                private static final long serialVersionUID = 3790805878629855813L;
                
                @Override
                public boolean canPlayAI() {
                    return getCreature().size() != 0;
                }
                
                @Override
                public void chooseTargetAI() {
                    card.tap();
                    Card target = CardFactoryUtil.AI_getBestCreature(getCreature());
                    setTargetCard(target);
                }
                
                CardList getCreature() {
                    CardList list = new CardList(AllZone.Computer_Hand.getCards());
                    list = list.getType(card.getChosenType());
                    return list;
                }
                
                @Override
                public void resolve() {
                    Card c = getTargetCard();
                    PlayerZone hand = AllZone.getZone(Constant.Zone.Hand, card.getController());
                    PlayerZone play = AllZone.getZone(Constant.Zone.Play, card.getController());
                    
                    if(AllZone.GameAction.isCardInZone(c, hand)) {
                        hand.remove(c);
                        play.add(c);
                    }
                }
            };//SpellAbility
            card.addSpellAbility(ability);
            ability.setDescription("3, tap: Put a creature card of the chosen type from your hand into play.");
            
            final Command paid = new Command() {
                private static final long serialVersionUID = 4258139342966165260L;
                
                public void execute() {
                    AllZone.InputControl.resetInput();
                    AllZone.Stack.add(ability);
                }
            };
            final Command unpaid = new Command() {
                private static final long serialVersionUID = 5792270994683837097L;
                
                public void execute() {
                    card.untap();
                }
            };
            final Input target = new Input() {
                private static final long serialVersionUID = -3180364352114242238L;
                
                @Override
                public void showMessage() {
                    ButtonUtil.enableOnlyCancel();
                    AllZone.Display.showMessage("Select creature from your hand to put into play");
                }
                
                @Override
                public void selectCard(Card c, PlayerZone zone) {
                    if(c.isCreature() && zone.is(Constant.Zone.Hand, Constant.Player.Human)
                            && c.getType().contains(card.getChosenType())) {
                        card.tap();
                        
                        ability.setTargetCard(c);//since setTargetCard() changes stack description
                        ability.setStackDescription("Put into play " + c);
                        
                        AllZone.InputControl.setInput(new Input_PayManaCost_Ability(ability.getManaCost(), paid,
                                unpaid));
                    }
                }
                
                @Override
                public void selectButtonCancel() {
                    card.untap();
                    stop();
                }
            };//Input target
            ability.setBeforePayMana(target);
        }//*************** END ************ END **************************
        


        //*************** START *********** START **************************
        else if(cardName.equals("That Which Was Taken")) {
            final SpellAbility ability = new Ability_Tap(card, "4") {
                private static final long serialVersionUID = -8996435083734446340L;
                
                @Override
                public void resolve() {
                    Card c = getTargetCard();
                    
                    if(AllZone.GameAction.isCardInPlay(c) && CardFactoryUtil.canTarget(card, c)) 
                    	//c.addExtrinsicKeyword("Indestructible");
                    	c.addCounter(Counters.DIVINITY, 1);
                }
                
                @Override
                public boolean canPlayAI() {
                    CardList perms = getPerms();
                    
                    return perms.size()>0;
                }
                
                @Override
                public void chooseTargetAI() {
                    //Card c = CardFactoryUtil.AI_getBestCreature(getCreatures());
                    CardList a = getPerms();
                    if (a.size()>0) {
	                    //CardListUtil.sortAttack(a);
	                    //CardListUtil.sortFlying(a);
	                    setTargetCard(a.get(0));
                    }
                }
                
                CardList getPerms() {
                    CardList list = new CardList();
                    list.addAll(AllZone.Computer_Play.getCards());
                    list = list.filter(new CardListFilter(){
                    	public boolean addCard(Card c)
                    	{
                    		return c.getCounters(Counters.DIVINITY) == 0 && (CardUtil.getConvertedManaCost(c.getManaCost()) > 3 || c.getNetAttack() > 4) &&
                    			   !c.getName().equals("That Which Was Taken");
                    	}
                    });
                    return list;
                }
            };//SpellAbility
            
            Input target = new Input() {
                private static final long serialVersionUID = 137806881250205274L;
                
                @Override
                public void showMessage() {
                    AllZone.Display.showMessage("Select target permanent");
                    ButtonUtil.enableOnlyCancel();
                }
                
                @Override
                public void selectButtonCancel() {
                    stop();
                }
                
                @Override
                public void selectCard(Card c, PlayerZone zone) {
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
        if(cardName.equals("Midsummer Revel")) {   
           
        	final SpellAbility ability = new Ability(card, "G") {

        		@Override
        		public boolean canPlayAI() {
        			if(card.getCounters(Counters.VERSE) > 0) return true;
        			return false;
        		}

        		@Override
        		public void resolve() { 
        				for(int i = 0; i < card.getCounters(Counters.VERSE); i++) {
                            CardFactoryUtil.makeToken("Beast", "G 3 3 Beast", card, "G", new String[] {
                                    "Creature", "Beast"}, 3, 3, new String[] {""});
        				}
        			AllZone.GameAction.sacrifice(card);
        		}
        	};
            card.addSpellAbility(ability);
            ability.setStackDescription(cardName
                    + " puts X 3/3 green Beast creature tokens onto the battlefield");
            ability.setDescription("G, Sacrifice Midsummer Revel: Put X 3/3 green Beast creature tokens onto the battlefield, where X is the number of verse counters on Midsummer Revel.");
        }//*************** END ************ END **************************
             
        
        //*************** START *********** START **************************
        else if(cardName.equals("Necrogenesis")) {
            final SpellAbility necrogen = new Ability(card, "2") {
                private static final long serialVersionUID = 1299216756153970592L;
                
                @Override
                public void resolve() {
                	Card c = getTargetCard();
                	if (AllZone.getZone(c).is(Constant.Zone.Graveyard)){
                		// target is still in the grave, ability resolves
                		AllZone.GameAction.exile(c);
                		CardFactoryUtil.makeTokenSaproling(card.getController());
                	}
                }
                
                @Override
                public boolean canPlayAI(){
                	return false;
                }
                
                @Override
                public boolean canPlay(){
					CardList list = new CardList(AllZone.Human_Graveyard.getCards());
					list.addAll(AllZone.Computer_Graveyard.getCards());
					return list.getType("Creature").size() > 0 && super.canPlay();
                }
            };
            Input necroTarget = new Input() {

            	boolean once = false;
				private static final long serialVersionUID = 8243511353958609599L;

				@Override
                public void showMessage() {
					CardList list = new CardList(AllZone.Human_Graveyard.getCards());
					list.addAll(AllZone.Computer_Graveyard.getCards());
					list = list.getType("Creature");
					if (list.size() == 0 || once) {
						once = false;
						stop();
					}
					else {
						Object o = AllZone.Display.getChoice("Choose card to exile", list.toArray());
						if (o!=null)
						{
							Card c = (Card)o;
							necrogen.setTargetCard(c);
							once = true;
							AllZone.Stack.add(necrogen);
						}
					}
					stop();
                }
            };
            
            necrogen.setDescription("2: Exile target creature card in a graveyard. Put a 1/1 green Saproling creature token into play.");
            necrogen.setStackDescription(card.getController()
                    + " exiles target creature card in a graveyard. Puts a 1/1 green Saproling creature token into play.");
            necrogen.setAfterPayMana(necroTarget);
            card.addSpellAbility(necrogen);
        }//*************** END ************ END **************************
        
        
        //*************** START *********** START **************************
        else if(cardName.equals("Night Soil")) {
            final SpellAbility nightSoil = new Ability(card, "1") {
                @Override
                public void resolve() {
            		CardFactoryUtil.makeTokenSaproling(card.getController());
                }
                
                @Override
                public boolean canPlayAI(){
                	return false;
                }
                
                @Override
                public boolean canPlay(){
					CardList grave = new CardList(AllZone.Human_Graveyard.getCards());
					CardList aiGrave = new CardList(AllZone.Computer_Graveyard.getCards());
					return (grave.getType("Creature").size() > 1 || aiGrave.getType("Creature").size() > 1) && super.canPlay();
                }
            };
            Input soilTarget = new Input() {

            	boolean once = false;
				private static final long serialVersionUID = 8243511353958609599L;

				@Override
                public void showMessage() {
					CardList grave = new CardList(AllZone.Human_Graveyard.getCards());
					CardList aiGrave = new CardList(AllZone.Computer_Graveyard.getCards());
					grave = grave.getType("Creature");
					aiGrave = aiGrave.getType("Creature");
					
					if (once || (grave.size() < 2 && aiGrave.size() < 2)) {
						once = false;
						stop();
					}
					else {
						CardList chooseGrave;
						if (grave.size() < 2)
							chooseGrave = aiGrave;
						else if (aiGrave.size() < 2)
							chooseGrave = grave;
						else{
							chooseGrave = aiGrave;
							chooseGrave.addAll(grave.toArray());
						}
						
						Object o = AllZone.Display.getChoice("Choose first card to exile", chooseGrave.toArray());
						if (o!=null)
						{
							CardList newGrave;
							Card c = (Card)o;
							if (c.getOwner().equals("Human")){
								newGrave = new CardList(AllZone.Human_Graveyard.getCards());
							}
							else {
								newGrave = new CardList(AllZone.Computer_Graveyard.getCards());
							}
							
							newGrave = newGrave.getType("Creature");
							newGrave.remove(c);
							
							Object o2 = AllZone.Display.getChoice("Choose second card to exile", newGrave.toArray());
							if (o2!=null)
							{
								Card c2 = (Card)o2;
								newGrave.remove(c2);
								AllZone.GameAction.exile(c);
								AllZone.GameAction.exile(c2);
								once = true;
								AllZone.Stack.add(nightSoil);
							}
						}
					}
					stop();
                }
            };
            
            nightSoil.setDescription("1, Exile target creature card in a graveyard: Put a 1/1 green Saproling creature token into play.");
            nightSoil.setStackDescription(card.getController()
                    + " put a 1/1 green Saproling creature token into play.");
            nightSoil.setAfterPayMana(soilTarget);
            card.addSpellAbility(nightSoil);
        }//*************** END ************ END **************************
        

        //*************** START *********** START **************************
        else if(cardName.equals("Yawgmoth's Bargain")) {
            final SpellAbility ability = new Ability(card, "0") {
                @Override
                public void resolve() {
                    	AllZone.GameAction.drawCard(card.getController());
                }
                
                @Override
                public boolean canPlayAI() {
                    return false;
                }
            };//SpellAbility
            
            ability.setDescription("Pay 1 life: Draw a card.");
            ability.setStackDescription(card.getName() + " - Pay 1 life: Draw a card.");

            card.addSpellAbility(ability);
            
            //instead of paying mana, pay life and add to stack
            //Input showMessage() is always the first method called
            Input payLife = new Input() {
                
                private static final long serialVersionUID = 8660593629867722192L;
                
                @Override
                public void showMessage() {
                	boolean paid = AllZone.GameAction.payLife(card.getController(), 1, card);
                    
                    //this order is very important, do not change
                    stop();
                    if (paid)
                    	AllZone.Stack.push(ability);
                }
            };//Input
            ability.setBeforePayMana(payLife);
        }//*************** END ************ END **************************
        
        
        //*************** START *********** START **************************
        else if(cardName.equals("Necropotence")) {
            final CardList necroCards = new CardList();
            
            final Command necro = new Command() {
                private static final long serialVersionUID = 4511445425867383336L;
                
                public void execute() {
                    //put cards removed by Necropotence into player's hand
                    if(necroCards.size() > 0) {
                        PlayerZone hand = AllZone.getZone(Constant.Zone.Hand, card.getController());
                        
                        for(int i = 0; i < necroCards.size(); i++) {
                            hand.add(necroCards.get(i));
                        }
                        necroCards.clear();
                    }
                }
            };
            
            final SpellAbility ability = new Ability(card, "0") {
                @Override
                public void resolve() {
                    PlayerZone library = AllZone.getZone(Constant.Zone.Library, card.getController());
                    
                    if(library.size() != 0) {
                        Card c = library.get(0);
                        library.remove(0);
                        necroCards.add(c); //add card to necro so that it goes into hand at end of turn
                        AllZone.EndOfTurn.addAt(necro);
                    }
                }
                
                @Override
                public boolean canPlayAI() {
                    return false;
                }
            };//SpellAbility
            
            ability.setDescription("1 life: Set aside the top card of your library face down. At the end of your turn, put that card into your hand.");
            ability.setStackDescription(card.getName()
                    + " - 1 life: Set aside the top card of your library face down. At the end of your turn, put that card into your hand.");
            
            card.addSpellAbility(ability);
            
            //instead of paying mana, pay life and add to stack
            //Input showMessage() is always the first method called
            Input payLife = new Input() {
                private static final long serialVersionUID = -3846772748411690084L;
                
                @Override
                public void showMessage() {
                	boolean paid = AllZone.GameAction.payLife(card.getController(), 1, card);
                    
                    //this order is very important, do not change
                    stop();
                    if (paid)
                    	AllZone.Stack.push(ability);
                }
            };//Input
            ability.setBeforePayMana(payLife);
            
        }//*************** END ************ END **************************

        
        //*************** START *********** START **************************
        else if(cardName.equals("Hoofprints of the Stag")) {
            Command intoPlay = new Command() {
                private static final long serialVersionUID = -7275454992618058248L;
                public boolean            firstTime        = true;
                
                public void execute() {
                    
                    if(firstTime) {
                        card.setCounter(Counters.HOOFPRINT, 0, false);
                    }
                    firstTime = false;
                }
            };
            
            card.addComesIntoPlayCommand(intoPlay);
            
            final SpellAbility a2 = new Ability(card, "2 W") {
                @Override
                public void resolve() {
                    card.subtractCounter(Counters.HOOFPRINT, 4);
                    CardFactoryUtil.makeToken("Elemental", "W 4 4 Elemental", card, "W", new String[] {
                            "Creature", "Elemental"}, 4, 4, new String[] {"Flying"});
                }
                
                @Override
                public boolean canPlay() {
                    SpellAbility sa;
                    for(int i = 0; i < AllZone.Stack.size(); i++) {
                        sa = AllZone.Stack.peek(i);
                        if(sa.getSourceCard().equals(card)) return false;
                    }
                    
                    return card.getCounters(Counters.HOOFPRINT) >= 4
                            && AllZone.getZone(card).is(Constant.Zone.Play)
                            && AllZone.Phase.getActivePlayer().equals(card.getController())
                            && !AllZone.Phase.getPhase().equals("End of Turn");
                }//canPlay()
                
                @Override
                public boolean canPlayAI() {
                    return true;
                }
            };//spellAbility
            
            a2.setDescription("2 W, Remove four hoofprint counters from Hoofprints of the Stag: Put a 4/4 white Elemental creature token with flying into play. Play this ability only during your turn.");
            a2.setStackDescription(card.getName()
                    + " - put a 4/4 white Elemental creature token with flying into play.");
            
            card.addSpellAbility(a2);
            
        }//*************** END ************ END **************************
        
        /*
        //*************** START *********** START **************************
        else if(cardName.equals("Goblin Trenches")) {
            final String player = card.getController();
            
            final SpellAbility ability = new Ability(card, "2") {
                @Override
                public boolean canPlay() {
                    PlayerZone play = AllZone.getZone(Constant.Zone.Play, player);
                    
                    CardList lands = new CardList();
                    lands.addAll(play.getCards());
                    lands = lands.getType("Land");
                    
                    if(lands.size() >= 1 && AllZone.GameAction.isCardInPlay(card)) return true;
                    else return false;
                }
                
                @Override
                public void chooseTargetAI() {
                    Card c = getTappedLand(); //first, try to get a tapped land to sac
                    if(c != null) {
                        setTargetCard(c);
                        
                    } else {
                        PlayerZone play = AllZone.getZone(Constant.Zone.Play, player);
                        
                        CardList lands = new CardList();
                        lands.addAll(play.getCards());
                        lands = lands.getType("Land");
                        
                        c = lands.get(0);
                        
                        setTargetCard(c);
                        
                    }
                }
                
                public Card getTappedLand() {
                    //target creature that is going to attack
                    PlayerZone play = AllZone.getZone(Constant.Zone.Play, player);
                    
                    CardList lands = new CardList();
                    lands.addAll(play.getCards());
                    lands = lands.getType("Land");
                    
                    for(int i = 0; i < lands.size(); i++) {
                        if(lands.get(i).isTapped()) return lands.get(i);
                    }
                    
                    return null;
                }//getAttacker()
                

                @Override
                public boolean canPlayAI() {
                    String phase = AllZone.Phase.getPhase();
                    return phase.equals(Constant.Phase.Main2);
                }
                
                @Override
                public void resolve() {
                    
                    Card c = getTargetCard();
                    if(AllZone.GameAction.isCardInPlay(c)) {
                        AllZone.GameAction.sacrifice(c);
                        makeToken();
                        makeToken();
                    }
                }//resolve
                
                public void makeToken() {
                    CardFactoryUtil.makeToken("Goblin Soldier", "RW 1 1 Goblin Soldier", card, "RW", new String[] {
                            "Creature", "Goblin", "Soldier"}, 1, 1, new String[] {""});
                }
            };
            
            Input runtime = new Input() {
                private static final long serialVersionUID = -7823269301012427007L;
                
                @Override
                public void showMessage() {
                    PlayerZone play = AllZone.getZone(Constant.Zone.Play, player);
                    
                    CardList lands = new CardList();
                    lands.addAll(play.getCards());
                    lands = lands.getType("Land");
                    
                    stopSetNext(CardFactoryUtil.input_targetSpecific(ability, lands, "Select a land to sacrifice",
                            false, false));
                    
                }//showMessage()
            };//Input
            
            card.addSpellAbility(ability);
            ability.setDescription("2, Sacrifice a land: Put two 1/1 red and white Goblin Soldier creature tokens into play.");
            ability.setStackDescription(card.getName()
                    + " - put two 1/1 red and white Goblin Soldier creature tokens into play.");
            ability.setBeforePayMana(runtime);
            
        }//*************** END ************ END **************************
        */

        //*************** START *********** START **************************  
        else if(cardName.equals("Hatching Plans")) {
            
            final Ability ability = new Ability(card, "0") {
                @Override
                public void resolve() {
                    AllZone.GameAction.drawCards(card.getController(), 3);
                    //AllZone.GameAction.drawCard(card.getController());
                    //AllZone.GameAction.drawCard(card.getController());
                }
            };
            
            Command draw3Cards = new Command() {
                private static final long serialVersionUID = -4919203791300685078L;
                
                public void execute() {
                    ability.setStackDescription(card.getName() + " - draw three cards.");
                    AllZone.Stack.add(ability);
                }
            };
            
            card.addDestroyCommand(draw3Cards);
            
        }//*************** END ************ END **************************  
        

        //*************** START *********** START **************************
        else if(cardName.equals("Dragon Blood")) {
            Ability_Tap ability = new Ability_Tap(card, "3") {
                private static final long serialVersionUID = -8095802059752537764L;
                
                @Override
                public void resolve() {
                    if(getTargetCard() != null && getTargetCard().isCreature()
                            && CardFactoryUtil.canTarget(card, getTargetCard())) getTargetCard().addCounter(
                            Counters.P1P1, 1);
                }
                
                @Override
                public boolean canPlayAI() {
                    CardList list = new CardList(AllZone.Computer_Play.getCards());
                    list = list.getType("Creature");
                    if(list.size() > 0) {
                        setTargetCard(CardFactoryUtil.AI_getBestCreature(list));
                        return (getTargetCard() != null);
                    }
                    return false;
                }
            };
            ability.setBeforePayMana(CardFactoryUtil.input_targetCreature(ability));
            ability.setDescription("3, tap: Put a +1/+1 counter on target creature.");
            StringBuilder sb = new StringBuilder();
            sb.append(card).append(": put a +1/+1 counter on target Creature.");
            ability.setStackDescription(sb.toString());
            card.addSpellAbility(ability);
        }//*************** END ************ END **************************

        
        //*************** START *********** START **************************
        else if(cardName.equals("AEther Vial")) {
            //final int[] converted = null;
            final Ability_Tap ability = new Ability_Tap(card, "0") {
                private static final long serialVersionUID = 1854859213307704018L;
                
                @Override
                public boolean canPlay() {
                    return card.getCounters(Counters.CHARGE) > 0;
                }
                
                @Override
                public void resolve() {
                    String player = card.getController();
                    
                    PlayerZone hand = AllZone.getZone(Constant.Zone.Hand, player);
                    PlayerZone play = AllZone.getZone(Constant.Zone.Play, player);
                    
                    //converted[0] = card.getCounters(Counters.CHARGE);
                    //System.out.println("converted: " + converted[0]);
                    
                    CardList list = new CardList(hand.getCards());
                    list = list.filter(new CardListFilter() {
                        public boolean addCard(Card c) {
                            return CardUtil.getConvertedManaCost(c.getManaCost()) == card.getCounters(Counters.CHARGE)
                                    && c.isCreature();
                        }
                    });
                    

                    if(list.size() > 0) {
                        if(player.equals(Constant.Player.Human)) {
                            Object o = AllZone.Display.getChoiceOptional("Pick creature to put into play",
                                    list.toArray());
                            if(o != null) {
                                Card c = (Card) o;
                                hand.remove(c);
                                play.add(c);
                            }
                        } else {
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
            ability.setStackDescription(card.getName()
                    + " - put creature card with converted mana cost equal to the number of charge counters into play.");
            
            card.addSpellAbility(ability);
        }//*************** END ************ END **************************
        
        
        //*************** START *********** START **************************
        else if(cardName.equals("AEther Spellbomb")) {
        	Ability_Cost abCost = new Ability_Cost("U Sac<1/CARDNAME>", cardName, true);
        	String[] valid = {"Creature"};
        	Target abTgt = new Target("TgtV", "Target a creature to bounce", valid);
            final Ability_Activated ability = new Ability_Activated(card, abCost, abTgt) {
				private static final long serialVersionUID = 1L;

				@Override
                public boolean canPlay() {
                    return AllZone.GameAction.isCardInPlay(card);
                }
                
                @Override
                public boolean canPlayAI() {
                    CardList humanPlay = new CardList(AllZone.Human_Play.getCards());
                    humanPlay = humanPlay.filter(new CardListFilter() {
                        public boolean addCard(Card c) {
                            return c.isCreature() && CardFactoryUtil.canTarget(card, c);
                        }
                    });
                    if(humanPlay.size() > 0) setTargetCard(CardFactoryUtil.AI_getBestCreature(humanPlay));
                    return ((AllZone.Computer_Hand.size() > 2) && (getTargetCard() != null));
                }
                
                @Override
                public void resolve() {
                    final Card[] target = new Card[1];
                    target[0] = getTargetCard();
                    PlayerZone hand = AllZone.getZone(Constant.Zone.Hand, target[0].getOwner());
                    
                    if(AllZone.GameAction.isCardInPlay(target[0]) && CardFactoryUtil.canTarget(card, target[0])) {
                        if(!target[0].isToken()) AllZone.GameAction.moveTo(hand, target[0]);
                        else AllZone.getZone(target[0]).remove(target[0]);
                    }
                }//resolve()
            };//SpellAbility
            ability.setDescription("U, Sacrifice AEther Spellbomb: Return target creature to its owner's hand.");
            card.addSpellAbility(ability);
        }//*************** END ************ END **************************
        
        
        //*************** START *********** START **************************
        else if(cardName.equals("Lifespark Spellbomb")) {
        	Ability_Cost abCost = new Ability_Cost("G Sac<1/CARDNAME>", cardName, true);
        	String[] valid = {"Land"};
        	Target abTgt = new Target("TgtV", "Target a land to animate", valid);
            final Ability_Activated ability = new Ability_Activated(card, abCost, abTgt) {
                private static final long serialVersionUID = -5744842090293912606L;
                
                @Override
                public boolean canPlay() {
                    return AllZone.GameAction.isCardInPlay(card);
                }
                
                @Override
                public boolean canPlayAI() {
                    CardList land = new CardList(AllZone.Computer_Play.getCards());
                    land = land.getType("Land");
                    CardList basic = land.getType("Basic");
                    if(basic.size() < 3) return false;
                    Card[] basic_1 = basic.toArray();
                    for(Card var:basic_1)
                        if(var.isTapped()) basic.remove(var);
                    basic.shuffle();
                    if(basic.size() == 0) return false;
                    if(basic.get(0) != null) {
                        setTargetCard(basic.get(0));
                        return true;
                    }
                    return false;
                }//canPlayAI() 
                
                @Override
                public void resolve() {
                    //in case ability is played twice
                    final int[] oldAttack = new int[1];
                    final int[] oldDefense = new int[1];
                    
                    final Card card[] = new Card[1];
                    card[0] = getTargetCard();
                    
                    oldAttack[0] = card[0].getBaseAttack();
                    oldDefense[0] = card[0].getBaseDefense();
                    
                    card[0].setBaseAttack(3);
                    card[0].setBaseDefense(3);
                    card[0].addType("Creature");
                    
                    //EOT
                    final Command untilEOT = new Command() {
                        private static final long serialVersionUID = 7236360479349324099L;
                        
                        public void execute() {
                            card[0].setBaseAttack(oldAttack[0]);
                            card[0].setBaseDefense(oldDefense[0]);
                            
                            card[0].removeType("Creature");
                            card[0].unEquipAllCards();
                        }
                    };
                    
                    AllZone.EndOfTurn.addUntil(untilEOT);
                }//resolve()
            };//SpellAbility
            card.addSpellAbility(ability);
            ability.setDescription("G, Sacrifice Lifespark Spellbomb: Target land becomes a 3/3 Creature until end of turn. It is still a land.");
            
        }//*************** END ************ END **************************
        
        //*************** START *********** START **************************
        else if(cardName.equals("Necrogen Spellbomb")) {
        	Ability_Cost abCost = new Ability_Cost("B Sac<1/CARDNAME>", cardName, true);
        	String[] valid = {"player"};
        	Target abTgt = new Target("TgtV","Target player discards a card", valid);
            final Ability_Activated ability = new Ability_Activated(card, abCost, abTgt) {
				private static final long serialVersionUID = -5712428914792877529L;

				@Override
                public boolean canPlay() {
                    return AllZone.GameAction.isCardInPlay(card);
                }
                
                @Override
                public boolean canPlayAI() {
                    setTargetPlayer(Constant.Player.Human);
                    return (MyRandom.random.nextBoolean() && AllZone.Human_Hand.size() > 0);
                }
                
                @Override
                public void resolve() {
                    String s = getTargetPlayer();
                    
                    StringBuilder sb = new StringBuilder();
                    sb.append("Necrogen Spellbomb - ").append(s).append(" discards a card");
                    setStackDescription(sb.toString());
                    // setStackDescription("Necrogen Spellbomb - " + s + " discards a card");
                    
                    if(Constant.Player.Computer.equals(getTargetPlayer())) AllZone.GameAction.discardRandom(getTargetPlayer(), this);
                    else AllZone.InputControl.setInput(CardFactoryUtil.input_discard(this));
                }//resolve()
            };//SpellAbility
            ability.setDescription("B, Sacrifice Necrogen Spellbomb: Target player discards a card.");
            card.addSpellAbility(ability);
        } //*************** END ************ END **************************
        
        
        //*************** START *********** START **************************
        else if(cardName.equals("Sensei's Divining Top")) {
            //ability2: Draw card, and put divining top on top of library
            final SpellAbility ability2 = new Ability_Tap(card, "0") {
                private static final long serialVersionUID = -2523015092351744208L;
                
                @Override
                public void resolve() {
                    String player = card.getController();
                    String owner = card.getOwner();
                    
                    PlayerZone play = AllZone.getZone(Constant.Zone.Play, player);
                    PlayerZone lib = AllZone.getZone(Constant.Zone.Library, owner);
                    
                    AllZone.GameAction.drawCard(player);
                    play.remove(card);
                    lib.add(card, 0); //move divining top to top of library
                    card.untap();
                    
                }
                
                @Override
                public boolean canPlayAI() {
                    return false;
                }
                
                @Override
                public boolean canPlay() {
                    if(AllZone.getZone(card).is(Constant.Zone.Play)) return true;
                    else return false;
                }//canPlay()
            };//SpellAbility ability2
            
            ability2.setBeforePayMana(new Input() {
                private static final long serialVersionUID = -4773496833654414458L;
                
                @Override
                public void showMessage() {
                    AllZone.Stack.push(ability2);
                    stop();
                }//showMessage()
            });
            

            //ability (rearrange top 3 cards) :
            final SpellAbility ability1 = new Ability(card, "1") {
                @Override
                public void resolve() {
                    String player = card.getController();
                    PlayerZone lib = AllZone.getZone(Constant.Zone.Library, player);
                    
                    if(lib.size() < 3) return;
                    
                    CardList topThree = new CardList();
                    
                    //show top 3 cards:
                    topThree.add(lib.get(0));
                    topThree.add(lib.get(1));
                    topThree.add(lib.get(2));
                    
                    for(int i = 1; i <= 3; i++) {
                        String Title = "Put on top: ";
                        if(i == 2) Title = "Put second from top: ";
                        if(i == 3) Title = "Put third from top: ";
                        Object o = AllZone.Display.getChoiceOptional(Title, topThree.toArray());
                        if(o == null) break;
                        Card c_1 = (Card) o;
                        topThree.remove(c_1);
                        lib.remove(c_1);
                        lib.add(c_1, i - 1);
                    }
                    
                }
                
                @Override
                public boolean canPlayAI() {
                    return false;
                    
                }
                
                @Override
                public boolean canPlay() {
                    if(AllZone.getZone(card).is(Constant.Zone.Play)) return true;
                    else return false;
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
        else if(cardName.equals("Quicksilver Amulet")) {
            final SpellAbility ability = new Ability_Tap(card, "4") {
                private static final long serialVersionUID = 4414609319033894302L;
                
                @Override
                public boolean canPlayAI() {
                    return (getCreature().size() > 0);
                }
                
                @Override
                public void chooseTargetAI() {
                    card.tap();
                    Card target = CardFactoryUtil.AI_getBestCreature(getCreature());
                    setTargetCard(target);
                }
                
                CardList getCreature() {
                    CardList list = new CardList(AllZone.Computer_Hand.getCards());
                    list = list.getType("Creature");
                    list = list.filter(new CardListFilter() {
                        public boolean addCard(Card c) {
                            return (c.getCMC() > 4);
                        }
                    });
                    return list;
                }
                
                @Override
                public void resolve() {
                    Card c = getTargetCard();
                    PlayerZone hand = AllZone.getZone(Constant.Zone.Hand, card.getController());
                    PlayerZone play = AllZone.getZone(Constant.Zone.Play, card.getController());
                    
                    if(AllZone.GameAction.isCardInZone(c, hand)) {
                        hand.remove(c);
                        play.add(c);
                    }
                }
            };
            
            ability.setBeforePayMana(new Input() {
                private static final long serialVersionUID = -1647181037510967127L;
                
                @Override
                public void showMessage() {
                    String controller = card.getController();
                    CardList creats = new CardList(AllZone.getZone(Constant.Zone.Hand, controller).getCards());
                    creats = creats.filter(new CardListFilter() {
                        public boolean addCard(Card c) {
                            PlayerZone zone = AllZone.getZone(c);
                            return c.isCreature() && zone.is(Constant.Zone.Hand);
                        }
                        
                    });
                    stopSetNext(CardFactoryUtil.input_targetSpecific(ability, creats, "Select a creature", false,
                            false));
                }
            });
            card.addSpellAbility(ability);
        }//*************** END ************ END **************************
        

        //*************** START *********** START **************************
        else if(cardName.equals("Chalice of the Void")) {
            Command intoPlay = new Command() {
                private static final long serialVersionUID = -7679939432259603542L;
                
                public void execute() {
                	int XCounters = card.getXManaCostPaid();
                	card.addCounter(Counters.CHARGE, XCounters);                
                }
            };
            card.addComesIntoPlayCommand(intoPlay);
        }
        //*************** END ************ END **************************     

        
        //*************** START *********** START **************************
        else if(cardName.equals("Counterbalance")) {
            String player = card.getController();
            final SpellAbility ability = new Ability(card, "0") {
                @Override
                public void resolve() {
                    PlayerZone lib = AllZone.getZone(Constant.Zone.Library, card.getController());
                    
                    Card topCard = lib.get(0);
                    
                    SpellAbility sa = AllZone.Stack.peek();
                    

                    int convertedManaTopCard = CardUtil.getConvertedManaCost(topCard.getManaCost());
                    int convertedManaSpell = CardUtil.getConvertedManaCost(sa.getSourceCard().getManaCost());
                    
                    CardList showTop = new CardList();
                    showTop.add(topCard);
                    AllZone.Display.getChoiceOptional("Revealed top card: ", showTop.toArray());
                    
                    if(convertedManaTopCard == convertedManaSpell) {
                        
                        AllZone.Stack.pop();
                        AllZone.GameAction.moveToGraveyard(sa.getSourceCard());
                    }
                }
                
                @Override
                public boolean canPlayAI() {
                    return false;
                    
                }
                
                @Override
                public boolean canPlay() {
                    String player = card.getController();
                    PlayerZone lib = AllZone.getZone(Constant.Zone.Library, player);
                    
                    if(AllZone.Stack.size() == 0 || lib.size() == 0) return false;
                    
                    //see if spell is on stack and that opponent played it
                    String opponent = AllZone.GameAction.getOpponent(card.getController());
                    SpellAbility sa = AllZone.Stack.peek();
                    
                    if(AllZone.getZone(card).is(Constant.Zone.Play) && sa.isSpell()
                            && opponent.equals(sa.getSourceCard().getController())
                            && CardFactoryUtil.isCounterable(sa.getSourceCard())) return true;
                    else return false;
                }//canPlay()
            };//SpellAbility ability
            
            ability.setStackDescription("Counterbalance - " + player
                    + " reveals top card and counters spell if it has the same converted manacost");
            //ability.setBeforePayMana(new Input_PayManaCost(ability));
            card.addSpellAbility(ability);
            
        }
        //*************** END ************ END **************************
        
        
        //*************** START *********** START **************************
        else if(cardName.equals("Aluren")) {
            final Ability ability1 = new Ability(card, "0") {
                @Override
                public void resolve() {
                    
                    //String player = card.getController();
                    PlayerZone hand = AllZone.getZone(Constant.Zone.Hand, Constant.Player.Human);
                    PlayerZone play = AllZone.getZone(Constant.Zone.Play, Constant.Player.Human);
                    
                    if(hand.size() == 0) return;
                    
                    CardList creatures = new CardList();
                    
                    for(int i = 0; i < hand.size(); i++) {
                        if(hand.get(i).getType().contains("Creature")
                                && CardUtil.getConvertedManaCost(hand.get(i).getManaCost()) <= 3) creatures.add(hand.get(i));
                    }
                    
                    if(creatures.size() == 0) return;
                    

                    Object o = AllZone.Display.getChoiceOptional("Select target creature to play",
                            creatures.toArray());
                    if(o != null) {
                        Card c = (Card) o;
                        hand.remove(c);
                        play.add(c);
                        c.setSickness(true);
                    }
                    

                }
                
                @Override
                public boolean canPlayAI() {
                    return false;
                    
                }
                
                @Override
                public boolean canPlay() {
                    if(AllZone.getZone(card).is(Constant.Zone.Play)) return true;
                    else return false;
                }//canPlay()
            };//SpellAbility ability1
            

            ability1.setDescription("Any player may play creature cards with converted mana cost 3 or less without paying their mana cost any time he or she could play an instant.");
            ability1.setStackDescription("Aluren - Play creature with converted manacost 3 or less for free.");
            ability1.setAnyPlayer(true);
            card.addSpellAbility(ability1);
        }
        //*************** END ************ END **************************
        

        //*************** START *********** START **************************
        else if(cardName.equals("Survival of the Fittest")) {
            SpellAbility ability = new Ability(card, "G") {
                @Override
                public void resolve() {
                    String player = card.getController();
                    if(player.equals(Constant.Player.Human)) humanResolve();
                    else computerResolve();
                }//resolve()
                
                public void humanResolve() {
                    CardList handCreatures = new CardList(AllZone.Human_Hand.getCards());
                    handCreatures = handCreatures.getType("Creature");
                    
                    if(handCreatures.size() == 0) return;
                    
                    Object discard = AllZone.Display.getChoiceOptional("Select Creature to discard",
                            handCreatures.toArray());
                    if(discard != null) {
                        
                        CardList creatures = new CardList(AllZone.Human_Library.getCards());
                        creatures = creatures.getType("Creature");
                        
                        if(creatures.size() != 0) {
                            Object check = AllZone.Display.getChoiceOptional("Select Creature",
                                    creatures.toArray());
                            if(check != null) {
                                PlayerZone hand = AllZone.getZone(Constant.Zone.Hand, card.getController());
                                AllZone.GameAction.moveTo(hand, (Card) check);
                            }
                            AllZone.GameAction.shuffle(Constant.Player.Human);
                        }
                        AllZone.GameAction.discard((Card) discard, this);
                    }
                }
                
                public void computerResolve() {
                //TODO
                }
                
                @Override
                public boolean canPlayAI() {
                    return false;
                }
                
            };//SpellAbility
            
            //card.clearSpellAbility();
            ability.setDescription("G: Discard a creature card: Search your library for a creature card, reveal that card, and put it into your hand. Then shuffle your library.");
            ability.setStackDescription("Survival of the Fittest - search for a creature card and put into hand");
            card.addSpellAbility(ability);
        }//*************** END ************ END ************************** 

        
        //*************** START *********** START **************************
        else if(cardName.equals("Volrath's Dungeon")) {
            final SpellAbility dungeon = new Ability(card, "0") {
            	// todo(sol) discard really needs to happen as a cost but in resolution for now :(
            	
                @Override
                public void chooseTargetAI() {
                    setTargetPlayer(Constant.Player.Human);
                }
                
                @Override
                public boolean canPlay() {
                    return Phase.canCastSorcery(Constant.Player.Human) && AllZone.GameAction.isCardInPlay(card) && super.canPlay() &&
                    		AllZone.getZone(Constant.Zone.Hand, Constant.Player.Human).getCards().length > 0;
                }
                
                @Override
                public void resolve() {
                	String player = getActivatingPlayer();
                    String target = getTargetPlayer();
                    CardList playerHand = new CardList(AllZone.getZone(Constant.Zone.Hand, player).getCards());
                    CardList targetHand = new CardList(AllZone.getZone(Constant.Zone.Hand, target).getCards());
                    
                    if(playerHand.size() == 0) return;
                    
                    if (player == Constant.Player.Human){
                    	if (!humanDiscard(playerHand, false))
                    		return;
                    }
                    else if (player == Constant.Player.Computer){
                    	if (!computerDiscard(playerHand, false))
                    		return;
                    }
                    
                    if (targetHand.size() == 0) return;
                    
                    if (target == Constant.Player.Human){
                    	if (!humanDiscard(targetHand, true))
                    		return;
                    }
                    else if (target == Constant.Player.Computer){
                    	if (!computerDiscard(targetHand, true))
                    		return;
                    }
                }
                
                public boolean humanDiscard(CardList hand, boolean toLibrary)
                {
                	String destination = "discard";
                	if (toLibrary)
                		destination = "place on top of library.";
                    Object discard = AllZone.Display.getChoiceOptional("Select Card to " + destination,
                    		hand.toArray());
                    if(discard == null) return false;
                    
                    Card card = (Card)discard;
                    
                    if (toLibrary)
                    	AllZone.GameAction.moveToTopOfLibrary(card);
                    else
                    	AllZone.GameAction.discard(card, this);
                    
                	return true;
                }
                
                public boolean computerDiscard(CardList hand, boolean toLibrary)
                {
                    if (toLibrary)
                    	AllZone.GameAction.AI_handToLibrary("Top");
                    else
                    	AllZone.GameAction.AI_discard(this);
                    
                	return true;
                }
                
                @Override
                public boolean canPlayAI() {
                    return (card.getController().equals(Constant.Player.Computer) && Phase.canCastSorcery(Constant.Player.Computer)
                    		&& AllZone.getZone(Constant.Zone.Hand, Constant.Player.Computer).getCards().length > 0
                    		&& AllZone.getZone(Constant.Zone.Hand, getTargetPlayer()).getCards().length > 0);
                }

            };//SpellAbility dungeon
           
            final SpellAbility bail = new Ability(card, "0") {
            	// Life payment really should happen on activation, maybe can do with a popup?
                @Override
                public void resolve() {
                	String player = getActivatingPlayer();
                	
                	if (AllZone.GameAction.payLife(player, 5, card))
                		AllZone.GameAction.destroy(card);
                }
      
                @Override
                public boolean canPlay() {
                    if(AllZone.Human_Life.getLife() >= 5 && AllZone.GameAction.isPlayerTurn(Constant.Player.Human) && super.canPlay()) 
                    	return true;
                    else return false;
                }
                                
                @Override
                public boolean canPlayAI() {
                	if (card.getController().equals(Constant.Player.Human) && AllZone.Computer_Life.getLife() >= 9 && 
                			AllZone.GameAction.isPlayerTurn(Constant.Player.Computer) && 
                			AllZone.GameAction.isCardInPlay(card)) 
                    	return true;
                    else return false;
                }

            };//SpellAbility pay bail

            dungeon.setBeforePayMana(CardFactoryUtil.input_targetPlayer(dungeon));
            dungeon.setChooseTargetAI(CardFactoryUtil.AI_targetHuman());
            dungeon.setDescription("Discard a card: Target player puts a card from his or her hand on top of his or her library. Activate this ability only any time you could cast a sorcery.");
            dungeon.setStackDescription("CARDNAME - Target player chooses a card in hand and puts on top of library.");
            
            bail.setAnyPlayer(true);
            bail.setDescription("Pay 5 Life: Destroy Volrath's Dungeon. Any player may activate this ability but only during his or her turn.");
            bail.setStackDescription("Destroy CARDNAME.");
            
            card.addSpellAbility(dungeon);
            card.addSpellAbility(bail);
        }//*************** END ************ END **************************


        //*************** START *********** START **************************
        if(cardName.equals("Ior Ruin Expedition")) {
            final SpellAbility ability = new Ability(card, "0") {
                @Override
                public boolean canPlay() {
                    return card.getCounters(Counters.QUEST) >= 3 && AllZone.GameAction.isCardInPlay(card)
                            && !AllZone.Stack.getSourceCards().contains(card);//in play and not already activated(Sac cost problems)
                }
                
                @Override
                public boolean canPlayAI() {
                    return (AllZone.Computer_Hand.size() < 6) && (AllZone.Computer_Library.size() > 0);
                }
                
                @Override
                public void resolve() {
                    AllZone.GameAction.drawCard(card.getController());
                    AllZone.GameAction.drawCard(card.getController());
                    AllZone.GameAction.sacrifice(getSourceCard());
                }
            };
            ability.setDescription("Remove three quest counters from Ior Ruin Expedition and sacrifice it: Draw two cards.");
            ability.setStackDescription(card.getName() + " - Draw two cards.");
            
            card.addSpellAbility(ability);
        }//*************** END ************ END **************************

        
        //*************** START *********** START **************************
        if(cardName.equals("Khalni Heart Expedition")) {
            final SpellAbility ability = new Ability(card, "0") {
                @Override
                public boolean canPlay() {
                    return card.getCounters(Counters.QUEST) >= 3 && AllZone.GameAction.isCardInPlay(card)
                            && !AllZone.Stack.getSourceCards().contains(card);//in play and not already activated(Sac cost problems)
                }
                
                @Override
                public boolean canPlayAI() {
                    return (AllZone.Computer_Library.size() > 0);
                }
                
                @Override
                public void resolve() {
                	// Sacrifice this first, otherwise the land search triggers 
                	// the landfall ability
                    AllZone.GameAction.sacrifice(getSourceCard());

                	// Put two basic lands into play tapped
                	AllZone.GameAction.searchLibraryTwoBasicLand(card.getController(),
                			Constant.Zone.Play, true, Constant.Zone.Play, true);
                }
            };
            ability.setDescription("Remove three quest counters from Khalni Heart Expedition and sacrifice it: search your library for two basic lands and put them onto the battlefield tapped.");
            ability.setStackDescription(card.getName() + " - Search for land.");
            
            card.addSpellAbility(ability);
        }//*************** END ************ END **************************

        /*
        //*************** START *********** START **************************
        else if(cardName.equals("Earthcraft")) {
            final SpellAbility a1 = new Ability(card, "0") {
                private static final long serialVersionUID = 6787319311700905218L;
                
                @Override
                public boolean canPlayAI() {
                    return false;
                }
                
                @Override
                public boolean canPlay() {
                    String controller = card.getController();
                    PlayerZone play = AllZone.getZone(Constant.Zone.Play, controller);
                    
                    CardList creats = new CardList();
                    
                    creats.addAll(play.getCards());
                    creats = creats.getType("Creature");
                    creats = creats.filter(new CardListFilter() {
                        public boolean addCard(Card c) {
                            return c.isUntapped();
                        }
                    });
                    
                    if(creats.size() > 0 && AllZone.GameAction.isCardInPlay(card)) return true;
                    else return false;
                }
                
                @Override
                public void resolve() {
                    
                    if(getTargetCard() == null) return;
                    
                    String player = card.getController();
                    if(player.equals(Constant.Player.Human)) humanResolve();
                    //else
                    //  computerResolve();
                }
                
                public void humanResolve() {
                    String controller = card.getController();
                    PlayerZone play = AllZone.getZone(Constant.Zone.Play, controller);
                    CardList creats = new CardList();
                    
                    creats.addAll(play.getCards());
                    creats = creats.getType("Creature");
                    creats = creats.filter(new CardListFilter() {
                        public boolean addCard(Card c) {
                            return c.isUntapped() && !c.equals(card);
                        }
                    });
                    
                    if(creats.size() == 1) {
                        creats.get(0).tap();
                        if(AllZone.GameAction.isCardInPlay(getTargetCard())
                                && CardFactoryUtil.canTarget(card, getTargetCard())) getTargetCard().untap();
                        
                    } else if(creats.size() > 1) {
                        Object o = AllZone.Display.getChoice("Select creature to tap", creats.toArray());
                        
                        if(o != null) {
                            Card c1 = (Card) o;
                            c1.tap();
                            if(AllZone.GameAction.isCardInPlay(getTargetCard())
                                    && CardFactoryUtil.canTarget(card, getTargetCard())) getTargetCard().untap();
                        }
                    }
                    

                }//humanResolve
                
            };//a1
            
            //card.clearSpellAbility();
            card.addSpellAbility(a1);
            a1.setDescription("Tap an untapped creature you control: untap target basic land.");
            
            Input runtime = new Input() {
                private static final long serialVersionUID = -2287693175821059029L;
                
                @Override
                public void showMessage() {
                    CardList all = new CardList();
                    all.addAll(AllZone.Human_Play.getCards());
                    all.addAll(AllZone.Computer_Play.getCards());
                    all = all.filter(new CardListFilter() {
                        public boolean addCard(Card c) {
                            return (c.isBasicLand()) && CardFactoryUtil.canTarget(card, c) && c.isTapped();
                        }
                    });
                    
                    stopSetNext(CardFactoryUtil.input_targetSpecific(a1, all, "Select target basic land", true,
                            false));
                }
            };
            a1.setBeforePayMana(runtime);
            
            final SpellAbility a2 = new Ability(card,"0")
            {
				public void resolve() {
					CardList list = CardFactoryUtil.getCards("Squirrel Nest",Constant.Player.Human);
					list = list.filter(new CardListFilter()
					{
						public boolean addCard(Card crd)
						{
							CardList l = new CardList(crd.getEnchanting().toArray());
							if (l.size()>0 && l.get(0).isBasicLand() && l.get(0).isUntapped())	
								return true;
							else 
								return false;
						}
					});
					
					if (list.size() == 0)
						return;
					
					CardList l = new CardList(list.get(0).getEnchanting().toArray());
					if (l.size() > 0) {
						Card basicLand = l.get(0);
						
						int max = 10;
						for (int i=0;i<max;i++)
						{
							basicLand.tap();
							CardList tokens = CardFactoryUtil.makeToken("Squirrel", "G 1 1 Squirrel", basicLand, "G", new String[] {
		                            "Creature", "Squirrel"}, 1, 1, new String[] {""});
							if (i < (max-1))
							{
								tokens.get(0).tap();
								basicLand.untap();
							}
							else
							{
								;
							}
						}
					}
				}
				
				public boolean canPlayAI()
				{
					return false;
				}
				
				public boolean canPlay()
				{
					CardList list = CardFactoryUtil.getCards("Squirrel Nest", Constant.Player.Human);
					list = list.filter(new CardListFilter()
					{
						public boolean addCard(Card crd)
						{
							CardList l = new CardList(crd.getEnchanting().toArray());
							if (l.size()>0 && l.get(0).isBasicLand() && l.get(0).isUntapped())	
								return true;
							else 
								return false;
						}
					});
					
					return super.canPlay() && list.size() > 0;
				}
            	
            };
            card.addSpellAbility(a2);
            a2.setDescription("(Shortcut 10x)");
            a2.setStackDescription(card + " - add 10 Squirrel tokens using Squirrel Nest");
            
            final SpellAbility a3 = new Ability(card,"0")
            {
				public void resolve() {
					CardList list = CardFactoryUtil.getCards("Squirrel Nest",Constant.Player.Human);
					list = list.filter(new CardListFilter()
					{
						public boolean addCard(Card crd)
						{
							CardList l = new CardList(crd.getEnchanting().toArray());
							if (l.size()>0 && l.get(0).isBasicLand() && l.get(0).isUntapped())	
								return true;
							else 
								return false;
						}
					});
					
					if (list.size() == 0)
						return;
					
					CardList l = new CardList(list.get(0).getEnchanting().toArray());
					if (l.size() > 0) {
						Card basicLand = l.get(0);
						
						int max = 30;
						for (int i=0;i<max;i++)
						{
							basicLand.tap();
							CardList tokens = CardFactoryUtil.makeToken("Squirrel", "G 1 1 Squirrel", basicLand, "G", new String[] {
		                            "Creature", "Squirrel"}, 1, 1, new String[] {""});
							if (i < (max-1))
							{
								tokens.get(0).tap();
								basicLand.untap();
							}
							else
							{
								;
							}
						}
					}
				}
				
				public boolean canPlayAI()
				{
					return false;
				}
				
				public boolean canPlay()
				{
					CardList list = CardFactoryUtil.getCards("Squirrel Nest", Constant.Player.Human);
					list = list.filter(new CardListFilter()
					{
						public boolean addCard(Card crd)
						{
							CardList l = new CardList(crd.getEnchanting().toArray());
							if (l.size()>0 && l.get(0).isBasicLand() && l.get(0).isUntapped())	
								return true;
							else 
								return false;
						}
					});
					
					return super.canPlay() && list.size() > 0;
				}
            	
            };
            card.addSpellAbility(a3);
            a3.setDescription("(Shortcut 30x)");
            a3.setStackDescription(card + " - add 30 Squirrel tokens using Squirrel Nest");
            
        }//*************** END ************ END **************************
        */

        //*************** START *********** START **************************
        else if(cardName.equals("Mox Diamond")) {
            final Input discard = new Input() {
                private static final long serialVersionUID = -1319202902385425204L;
                
                @Override
                public void showMessage() {
                    AllZone.Display.showMessage("Discard a land card (or select Mox Diamond to sacrifice it)");
                    ButtonUtil.enableOnlyCancel();
                }
                
                @Override
                public void selectCard(Card c, PlayerZone zone) {
                    if(zone.is(Constant.Zone.Hand) && c.isLand()) {
                        AllZone.GameAction.discard(c, null);
                        stop();
                    } else if(c.equals(card)) {
                        AllZone.GameAction.sacrifice(card);
                        stop();
                    }
                }
            };//Input
            
            final SpellAbility ability = new Ability(card, "0") {
                @Override
                public void resolve() {
                    if(card.getController().equals(Constant.Player.Human)) {
                        if(AllZone.Human_Hand.getCards().length == 0) AllZone.GameAction.sacrifice(card);
                        else AllZone.InputControl.setInput(discard);
                    } else {
                        CardList list = new CardList(AllZone.Computer_Hand.getCards());
                        list = list.filter(new CardListFilter() {
                            public boolean addCard(Card c) {
                                return (c.isLand());
                            }
                        });
                        AllZone.GameAction.discard(list.get(0), this);
                    }//else
                }//resolve()
            };//SpellAbility
            Command intoPlay = new Command() {
                private static final long serialVersionUID = -7679939432259603542L;
                
                public void execute() {
                    ability.setStackDescription("If Mox Diamond would come into play, you may discard a land card instead. If you do, put Mox Diamond into play. If you don't, put it into its owner's graveyard.");
                    AllZone.Stack.add(ability);
                }
            };
            SpellAbility spell = new Spell_Permanent(card) {
                private static final long serialVersionUID = -1818766848857998431L;
                
                //could never get the AI to work correctly
                //it always played the same card 2 or 3 times
                @Override
                public boolean canPlayAI() {
                    return false;
                }
                
                @Override
                public boolean canPlay() {
                    PlayerZone hand = AllZone.getZone(Constant.Zone.Hand, card.getController());
                    CardList list = new CardList(hand.getCards());
                    list.remove(card);
                    list = list.filter(new CardListFilter() {
                        public boolean addCard(Card c) {
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
        else if(cardName.equals("Standstill")) {
            card.clearSpellAbility();
            card.addSpellAbility(new Spell_Permanent(card) {
                private static final long serialVersionUID = 6912683989507840172L;
                
                @Override
                public boolean canPlayAI() {
                    PlayerZone compPlay = AllZone.getZone(Constant.Zone.Play, Constant.Player.Computer);
                    PlayerZone humPlay = AllZone.getZone(Constant.Zone.Play, Constant.Player.Human);
                    
                    CardList compCreats = new CardList(compPlay.getCards());
                    compCreats = compCreats.getType("Creature");
                    
                    CardList humCreats = new CardList(humPlay.getCards());
                    humCreats = humCreats.getType("Creature");
                    
                    //only play standstill if comp controls more creatures than human
                    //this needs some additional rules, maybe add all power + toughness and compare
                    if(compCreats.size() > humCreats.size()) return true;
                    else return false;
                }
            });
        }//*************** END ************ END **************************
        

        //*************** START *********** START **************************
        if(cardName.equals("Legacy Weapon")) {
            final Ability ability = new Ability(card, "W U B R G") {
                @Override
                public void resolve() {
                    Card c = getTargetCard();
                    
                    if(AllZone.GameAction.isCardInPlay(c) && CardFactoryUtil.canTarget(card, c)) {
                        AllZone.GameAction.removeFromGame(c);
                    }
                }
                
                @Override
                public void chooseTargetAI() {
                    PlayerZone hplay = AllZone.getZone(Constant.Zone.Play, Constant.Player.Human);
                    CardList human = new CardList(hplay.getCards());
                    human = human.filter(new CardListFilter() {
                        public boolean addCard(Card c) {
                            return CardFactoryUtil.canTarget(card, c);
                        }
                    });
                    
                    if(human.size() != 0) {
                        setTargetCard(CardFactoryUtil.AI_getMostExpensivePermanent(human, card, true));
                    }
                }
                
                @Override
                public boolean canPlayAI() {
                    PlayerZone hplay = AllZone.getZone(Constant.Zone.Play, Constant.Player.Human);
                    CardList human = new CardList(hplay.getCards());
                    human = human.filter(new CardListFilter() {
                        public boolean addCard(Card c) {
                            return CardFactoryUtil.canTarget(card, c);
                        }
                    });
                    return human.size() > 0;
                }
                
            };//ability
            
            Input target = new Input() {
                private static final long serialVersionUID = -7279903055386088569L;
                
                @Override
                public void showMessage() {
                    AllZone.Display.showMessage("Select target permanent for " + ability.getSourceCard());
                    ButtonUtil.enableOnlyCancel();
                }
                
                @Override
                public void selectButtonCancel() {
                    stop();
                }
                
                @Override
                public void selectCard(Card crd, PlayerZone zone) {
                    if(zone.is(Constant.Zone.Play) && CardFactoryUtil.canTarget(card, crd)) {
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
        else if(cardName.equals("Voltaic Key")) {
            final Ability_Tap ability = new Ability_Tap(card, "1") {
                private static final long serialVersionUID = 6097818373831898299L;
                
                @Override
                public void resolve() {
                    Card c = getTargetCard();
                    if(AllZone.GameAction.isCardInPlay(c) && CardFactoryUtil.canTarget(card, c) && c.isTapped()) c.untap();
                }
                
                @Override
                public boolean canPlayAI() {
                    return false;
                }
            };//ability
            
            Input target = new Input() {
                private static final long serialVersionUID = -7279903055386088569L;
                
                @Override
                public void showMessage() {
                    AllZone.Display.showMessage("Select target tapped artifact for " + ability.getSourceCard());
                    ButtonUtil.enableOnlyCancel();
                }
                
                @Override
                public void selectButtonCancel() {
                    stop();
                }
                
                @Override
                public void selectCard(Card crd, PlayerZone zone) {
                    if(zone.is(Constant.Zone.Play) && CardFactoryUtil.canTarget(card, crd) && crd.isArtifact()
                            && crd.isTapped()) {
                        ability.setTargetCard(crd);
                        stopSetNext(new Input_PayManaCost(ability));
                    }
                }
            };//Input
            ability.setDescription("1, tap: Untap target artifact.");
            
            ability.setBeforePayMana(target);
            card.addSpellAbility(ability);
            
        }//*************** END ************ END **************************
        

        //*************** START ************ START **************************
        else if(cardName.equals("Ashnod's Transmogrant")) {
            final Ability_Tap ability = new Ability_Tap(card) {
                private static final long serialVersionUID = -401631574059431293L;
                
                @Override
                public void resolve() {
                    if(card.getController().equals(Constant.Player.Computer)) AllZone.GameAction.sacrifice(card);
                    if(getTargetCard() == null || !getTargetCard().isCreature()) return;
                    Card crd = getTargetCard();
                    crd.addCounter(Counters.P1P1, 1);
                    if(!crd.getType().contains("Artifact")) crd.addType("Artifact");
                }
                
                @Override
                public boolean canPlayAI() {
                    CardList list = new CardList(AllZone.Computer_Play.getCards()).filter(new CardListFilter() {
                        public boolean addCard(Card c) {
                            return !c.isArtifact() && c.isCreature();
                        }
                    });
                    Card crd = CardFactoryUtil.AI_getBestCreature(list);
                    if(crd != null) setTargetCard(crd);
                    return (getTargetCard() != null);
                }
            };
            Input runtime = new Input() {
                private static final long serialVersionUID = 141164423096887945L;
                
                @Override
                public void showMessage() {
                    AllZone.Display.showMessage("Select target creature for " + card);
                    ButtonUtil.enableOnlyCancel();
                }
                
                @Override
                public void selectButtonCancel() {
                    stop();
                }
                
                @Override
                public void selectCard(Card c, PlayerZone zone) {
                    if(!CardFactoryUtil.canTarget(ability, c)) {
                        AllZone.Display.showMessage("Cannot target this card (Shroud? Protection?).");
                    } else if(c.isCreature() && !c.isArtifact() && zone.is(Constant.Zone.Play)) {
                        ability.setTargetCard(c);
                        AllZone.GameAction.sacrifice(card);
                        stopSetNext(new Input_NoCost_TapAbility(ability));
                    }
                }
            };
            ability.setBeforePayMana(runtime);
            ability.setDescription("tap, Sacrifice Ashnod's Transmogrant: put a +1/+1 counter on target nonartifact creature. That creature becomes an artifact in addition to its other types.");
            card.addSpellAbility(ability);
        }//*************** END ************ END **************************
       
        
        //*************** START ************ START **************************
        else if(cardName.equals("Gemstone Array")) {
            final Ability store = new Ability(card, "2") {
                @Override
                public void resolve() {
                    card.addCounter(Counters.CHARGE, 1);
                }
                
                @Override
                public boolean canPlayAI() {
                    return AllZone.Phase.is(Constant.Phase.Main2, Constant.Player.Computer);
                }
            };
            store.setDescription("2: Put a charge counter on Gemstone Array.");
            store.setStackDescription("Put a charge counter on Gemstone Array");
            card.addSpellAbility(store);
            
            final Ability_Mana retrieve = new Ability_Mana(card,
                    "0, Remove a charge counter from Gemstone Array: Add one mana of any color to your mana pool.") {
                private static final long serialVersionUID = -2938965362221626028L;
                
                @Override
                public void undo() {
                    card.addCounter(Counters.CHARGE, 1);
                }
                
                //@Override
                public String mana() {
                    return this.choices_made[0].toString();
                }
                
                @Override
                public boolean canPlay() {
                    if(choices_made[0] == null) choices_made[0] = "1";
                    return super.canPlay() && card.getCounters(Counters.CHARGE) > 0;
                }
                
                @Override
                public void resolve() {
                    card.subtractCounter(Counters.CHARGE, 1);
                    super.resolve();
                }
            };
            retrieve.choices_made = new String[1];
            retrieve.setBeforePayMana(new Input() {
                
                private static final long serialVersionUID = 376497609786542558L;
                
                @Override
                public void showMessage() {
                    retrieve.choices_made[0] = Input_PayManaCostUtil.getShortColorString((String)(AllZone.Display.getChoiceOptional(
                            "Select a Color", Constant.Color.onlyColors)));
                    AllZone.Stack.add(retrieve);
                    stop();
                }
            });
            card.addSpellAbility(retrieve);
        }//*************** END ************ END **************************
        

        //*************** START *********** START **************************
        else if(cardName.equals("Door to Nothingness")) {
            Ability_Tap ab1 = new Ability_Tap(card, "G G R R B B U U W W") {
                
                private static final long serialVersionUID = 6665327569823149191L;
                
                @Override
                public void resolve() {
            		// Win / Lose
            		final String player = getTargetPlayer();
            		PlayerZone playZone = AllZone.getZone(Constant.Zone.Play, player);
            		PlayerZone OpplayZone = AllZone.getZone(Constant.Zone.Play, AllZone.GameAction.getOpponent(player));
            		CardList Platinumlist = new CardList(OpplayZone.getCards());
            		Platinumlist = Platinumlist.getName("Platinum Angel");
            		CardList Abyssallist = new CardList(playZone.getCards());
            		Abyssallist = Abyssallist.getName("Abyssal Persecutor");
            		if(Platinumlist.size() == 0 && Abyssallist.size() == 0) {
                    AllZone.GameAction.getPlayerLife(getTargetPlayer()).setLife(0);
            		
                    if (getTargetPlayer().equals(Constant.Player.Computer)) {
	                    int gameNumber = 0;
	                    if (Constant.Runtime.WinLose.getWin()==1)
	                    	gameNumber = 1;
	                    Constant.Runtime.WinLose.setWinMethod(gameNumber,"Door to Nothingness");
                    }
                }
            }
                
                @Override
                public boolean canPlayAI() {
                    return true;
                }
            };
            ab1.setChooseTargetAI(CardFactoryUtil.AI_targetHuman());
            ab1.setBeforePayMana(CardFactoryUtil.input_targetPlayer(ab1));
            ab1.setDescription("WWUUBBRRGG, tap, sacrifice Door to Nothingness: Target player loses the game.");
            card.addSpellAbility(ab1);
        }//*************** END ************ END **************************
        

        //*************** START *********** START **************************
        else if(cardName.equals("Seal of Cleansing") || cardName.equals("Seal of Primordium")) {
            final Ability ability = new Ability(card, "0") {
                @Override
                public boolean canPlayAI() {
                    return getArtEnchantments().size() != 0;
                }
                
                @Override
                public void chooseTargetAI() {
                    
                    CardList list = getArtEnchantments();
                    if(list.size() > 0) {
                        CardListUtil.sortCMC(list);
                        setTargetCard(list.get(0));
                        AllZone.GameAction.sacrifice(card);
                    }
                }//chooseTargetAI()
                
                CardList getArtEnchantments() {
                    
                    PlayerZone play = AllZone.getZone(Constant.Zone.Play, Constant.Player.Human);
                    CardList list = new CardList(play.getCards());
                    list = list.filter(new CardListFilter() {
                        public boolean addCard(Card c) {
                            return c.isArtifact() || c.isEnchantment();
                        }
                    });
                    return list;
                }//getArtEnchantments()
                
                @Override
                public void resolve() {
                    if(getTargetCard() != null) {
                        if(AllZone.GameAction.isCardInPlay(getTargetCard())
                                && CardFactoryUtil.canTarget(card, getTargetCard())) AllZone.GameAction.destroy(getTargetCard());
                    }
                }//resolve()
            };//SpellAbility
            
            Input runtime = new Input() {
                private static final long serialVersionUID = -1750678113925588670L;
                
                @Override
                public void showMessage() {
                    //card.addSpellAbility(ability);
                    //ability.setDescription("Sacrifice " + cardName + ": destroy target artifact or enchantment.");
                    
                    PlayerZone hplay = AllZone.getZone(Constant.Zone.Play, Constant.Player.Human);
                    PlayerZone cplay = AllZone.getZone(Constant.Zone.Play, Constant.Player.Computer);
                    CardList choices = new CardList();
                    choices.addAll(hplay.getCards());
                    choices.addAll(cplay.getCards());
                    
                    choices = choices.filter(new CardListFilter() {
                        public boolean addCard(Card c) {
                            return c.isEnchantment() || c.isArtifact();
                        }
                    });
                    stopSetNext(CardFactoryUtil.input_targetSpecific(ability, choices,
                            "Destroy target artifact or enchantment", new Command() {
                                
                                private static final long serialVersionUID = -4987328870651000691L;
                                
                                public void execute() {
                                    AllZone.GameAction.sacrifice(card);
                                }
                            }, true, false));
                }
            };
            
            ability.setDescription("Sacrifice " + card.getName() + ": destroy target artifact or enchantment.");
            ability.setBeforePayMana(runtime);
            card.addSpellAbility(ability);
            
        }//*************** END ************ END **************************
        
        
        //*************** START *********** START **************************
        else if(cardName.equals("Seal of Fire") || cardName.equals("Moonglove Extract")) {
            final Ability ability = new Ability(card, "0") {
                @Override
                public boolean canPlayAI() {
                    return getCreature().size() != 0 || AllZone.Human_Life.getLife() < 4;
                }
                
                @Override
                public void chooseTargetAI() {
                    if(AllZone.Human_Life.getLife() < 4) setTargetPlayer(Constant.Player.Human);
                    else {
                        CardList list = getCreature();
                        list.shuffle();
                        setTargetCard(list.get(0));
                    }
                    AllZone.GameAction.sacrifice(card);
                }//chooseTargetAI()
                
                CardList getCreature() {
                    //toughness of 1
                    CardList list = CardFactoryUtil.AI_getHumanCreature(2, card, true);
                    list = list.filter(new CardListFilter() {
                        public boolean addCard(Card c) {
                            //only get 1/1 flyers or 2/1 or bigger creatures
                            return (2 <= c.getNetAttack()) || c.getKeyword().contains("Flying");
                        }
                    });
                    return list;
                }//getCreature()
                
                @Override
                public void resolve() {
                    if(getTargetCard() != null) {
                        if(AllZone.GameAction.isCardInPlay(getTargetCard())
                                && CardFactoryUtil.canTarget(card, getTargetCard())) getTargetCard().addDamage(2,
                                card);
                    } else AllZone.GameAction.getPlayerLife(getTargetPlayer()).subtractLife(2,card);
                }//resolve()
            };//SpellAbility
            
            card.addSpellAbility(ability);
            ability.setDescription("Sacrifice " + cardName + ": "+cardName+" deals 2 damage to target creature or player.");
            ability.setBeforePayMana(CardFactoryUtil.input_targetCreaturePlayer(ability, new Command() {
                private static final long serialVersionUID = 4180346673509230280L;
                
                public void execute() {
                    AllZone.GameAction.sacrifice(card);
                }
            }, true, false));
        }//*************** END ************ END **************************
        
        
        //*************** START *********** START **************************
        else if(cardName.equals("Seal of Removal")) {
            final Ability ability = new Ability(card, "0") {
                @Override
                public boolean canPlayAI() {
                    return getCreature().size() != 0;
                }
                
                @Override
                public void chooseTargetAI() {
                    CardList list = getCreature();
                    list.shuffle();
                    setTargetCard(list.get(0));
                    
                    AllZone.GameAction.sacrifice(card);
                }//chooseTargetAI()
                
                CardList getCreature() {
                    CardList list = new CardList(AllZone.Human_Play.getCards());
                    list = list.filter(new CardListFilter() {
                        public boolean addCard(Card c) {
                            return c.isCreature()
                                    && (c.getNetAttack() >= 3 || c.getKeyword().contains("Flying") || c.isEnchanted())
                                    && CardFactoryUtil.canTarget(card, c);
                        }
                    });
                    return list;
                }//getCreature()
                
                @Override
                public void resolve() {
                    if(getTargetCard() != null) {
                        
                        if(AllZone.GameAction.isCardInPlay(getTargetCard())
                                && CardFactoryUtil.canTarget(card, getTargetCard())) {
                            PlayerZone hand = AllZone.getZone(Constant.Zone.Hand, getTargetCard().getOwner());
                            AllZone.GameAction.moveTo(hand, getTargetCard());
                        }
                    }
                }//resolve()
            };//SpellAbility
            
            card.addSpellAbility(ability);
            ability.setDescription("Sacrifice Seal of Removal: return target creature to its owner's hand.");
            ability.setBeforePayMana(CardFactoryUtil.input_targetCreature(ability, new Command() {
                
                private static final long serialVersionUID = 2565599788533507611L;
                
                public void execute() {
                    AllZone.GameAction.sacrifice(card);
                }
            }));
        }//*************** END ************ END **************************
        
     
        //*************** START *********** START **************************
        else if(cardName.equals("Kaervek's Spite")) {
            final SpellAbility spell = new Spell(card) {
                private static final long serialVersionUID = -6259614160639535500L;
                
                @Override
                public boolean canPlayAI() {
                    if(AllZone.Human_Life.getLife() <= 5) return true;
                    PlayerZone play = AllZone.getZone(Constant.Zone.Play, Constant.Player.Computer);
                    PlayerZone lib = AllZone.getZone(Constant.Zone.Library, Constant.Player.Computer);
                    
                    CardList playList = new CardList(play.getCards());
                    CardList libList = new CardList(lib.getCards());
                    
                    playList = playList.getName("Academy Rector");
                    libList = libList.getName("Barren Glory");
                    
                    return (AllZone.Human_Life.getLife() <= 5) || (playList.size() == 1 && libList.size() >= 1);
                }
                
                @Override
                public void resolve() {
                    PlayerZone play = AllZone.getZone(Constant.Zone.Play, card.getController());
                    PlayerZone hand = AllZone.getZone(Constant.Zone.Play, card.getController());
                    CardList list = new CardList(play.getCards());
                    list = list.filter(new CardListFilter() {
                        public boolean addCard(Card c) {
                            return !c.getName().equals("Mana Pool");
                        }
                    });
                    CardList handList = new CardList(hand.getCards());
                    
                    for(Card c:list) {
                        AllZone.GameAction.sacrifice(c);
                    }
                    AllZone.GameAction.discardRandom(card.getController(), handList.size(), this);
                    
                    PlayerLife life = AllZone.GameAction.getPlayerLife(getTargetPlayer());
                    life.subtractLife(5,card);
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
        else if(cardName.equals("Staff of Domination")) {
            /*
            final Ability_Tap ability2 = new Ability_Tap(card, "2") {
                
                private static final long serialVersionUID = -5513078874305811825L;
                
                @Override
                public boolean canPlayAI() {
                    return AllZone.Phase.getPhase().equals(Constant.Phase.Main2);
                }
                
                @Override
                public void resolve() {
                	AllZone.GameAction.gainLife(card.getController(), 1);
                }
            };//SpellAbility
            
            ability2.setDescription("2, tap: You gain 1 life.");
            // ability2.setStackDescription(cardName + " - You gain 1 life.");
            StringBuilder sb2 = new StringBuilder();
            sb2.append(cardName).append(" - You gain 1 life.");
            ability2.setStackDescription(sb2.toString());
            */
            final SpellAbility ability3 = new Ability_Tap(card, "3") {
                private static final long serialVersionUID = 1125696151526415705L;
                
                @Override
                public boolean canPlayAI() {
                    return getTapped().size() != 0;
                }
                
                @Override
                public void chooseTargetAI() {
                    card.tap();
                    Card target = CardFactoryUtil.AI_getBestCreature(getTapped());
                    setTargetCard(target);
                }
                
                CardList getTapped() {
                    CardList list = new CardList(AllZone.Computer_Play.getCards());
                    list = list.filter(new CardListFilter() {
                        public boolean addCard(Card c) {
                            return c.isCreature() && c.isTapped();
                        }
                    });
                    return list;
                }//getTapped()
                
                @Override
                public void resolve() {
                    if(AllZone.GameAction.isCardInPlay(getTargetCard())
                            && CardFactoryUtil.canTarget(card, getTargetCard())) {
                        Card c = getTargetCard();
                        if(c.isTapped()) c.untap();
                    }
                }//resolve()
            };//SpellAbility
            
            ability3.setDescription("3, tap: Untap target creature.");
            ability3.setBeforePayMana(CardFactoryUtil.input_targetCreature(ability3));
            /*
            final SpellAbility ability4 = new Ability_Tap(card, "4") {
                
                private static final long serialVersionUID = 8102011024731535257L;
                
                @Override
                public boolean canPlayAI() {
                    return false;
                }
                
                @Override
                public void resolve() {
                    if(AllZone.GameAction.isCardInPlay(getTargetCard())
                            && CardFactoryUtil.canTarget(card, getTargetCard())) {
                        Card c = getTargetCard();
                        if(c.isUntapped()) c.tap();
                    }
                }//resolve()
            };//SpellAbility
            
            ability4.setDescription("4, tap: Tap target creature.");
            ability4.setBeforePayMana(CardFactoryUtil.input_targetCreature(ability4));
            */
            /*
            final Ability_Tap ability5 = new Ability_Tap(card, "5") {
                
                private static final long serialVersionUID = -8459438547823091716L;
                
                @Override
                public boolean canPlayAI() {
                    return true;
                }
                
                @Override
                public void resolve() {
                    AllZone.GameAction.drawCard(card.getController());
                }
            };//SpellAbility
            
            ability5.setDescription("5, tap: Draw a card.");
            // ability5.setStackDescription(card.getName() + " - draw a card.");
            StringBuilder sb5 = new StringBuilder();
            sb5.append(card.getName()).append(" - draw a card.");
            ability5.setStackDescription(sb5.toString());
            */
            
            //card.addSpellAbility(ability2);
            card.addSpellAbility(ability3);
            //card.addSpellAbility(ability4);
            //card.addSpellAbility(ability5);         
        }//*************** END ************ END **************************
        
        
        //*************** START *********** START **************************
        if(cardName.equals("Goblin Charbelcher")) {
            final Ability_Tap ability = new Ability_Tap(card, "3") {
                private static final long serialVersionUID = -840041589720758423L;
                
                @Override
                public void resolve() {
                    PlayerZone lib = AllZone.getZone(Constant.Zone.Library, card.getController());
                    CardList topOfLibrary = new CardList(lib.getCards());
                    CardList revealed = new CardList();
                    
                    if(topOfLibrary.size() == 0) return;
                    
                    int damage = 0;
                    int count = 0;
                    Card c = null;
                    Card crd;
                    while(c == null) {
                        revealed.add(topOfLibrary.get(count));
                        crd = topOfLibrary.get(count++);
                        if(crd.isLand() || count == topOfLibrary.size()) {
                            c = crd;
                            damage = count;
                            if(crd.getName().equals("Mountain")) damage = damage * 2;
                        }
                    }//while
                    AllZone.Display.getChoiceOptional("Revealed cards:", revealed.toArray());
                    
                    if(getTargetCard() != null) {
                        if(AllZone.GameAction.isCardInPlay(getTargetCard())
                                && CardFactoryUtil.canTarget(card, getTargetCard())) {
                            getTargetCard().addDamage(damage, card);
                        }
                    } else AllZone.GameAction.getPlayerLife(getTargetPlayer()).subtractLife(damage,card);
                }
            };
            ability.setDescription("3, tap: Reveal cards from the top of your library until you reveal a land card. Goblin Charbelcher deals damage equal to the number of nonland cards revealed this way to target creature or player. If the revealed land card was a Mountain, Goblin Charbelcher deals double that damage instead. Put the revealed cards on the bottom of your library in any order.");
            ability.setChooseTargetAI(CardFactoryUtil.AI_targetHuman());
            ability.setBeforePayMana(CardFactoryUtil.input_targetCreaturePlayer(ability, true, false));
            card.addSpellAbility(ability);
            

        }//*************** END ************ END **************************
        
       
        //*************** START *********** START **************************
        else if(cardName.equals("Thopter Foundry")) {
            final String player = card.getController();
            
            final SpellAbility ability = new Ability(card, "1") {
                @Override
                public void chooseTargetAI() {
                    Card c;
                    PlayerZone play = AllZone.getZone(Constant.Zone.Play, player);
                    
                    CardList meek = new CardList();
                    meek.addAll(play.getCards());
                    meek = meek.getName("Sword of the Meek");
                    
                    if(meek.size() >= 1) c = meek.get(0);
                    else c = getArtifact();
                    if(c != null) setTargetCard(c);
                    
                }
                
                public Card getArtifact() {
                    //target creature that is going to attack
                    PlayerZone play = AllZone.getZone(Constant.Zone.Play, player);
                    
                    CardList arts = new CardList();
                    arts.addAll(play.getCards());
                    arts = arts.filter(new CardListFilter() {
                        public boolean addCard(Card c) {
                            return c.isArtifact()
                                    && !c.isToken()
                                    && (CardUtil.getConvertedManaCost(c.getManaCost()) <= 1 && !c.equals(card) || c.getName().equals(
                                            "Sword of the Meek"));
                        }
                    });
                    
                    if(arts.size() > 0) {
                        arts.shuffle();
                        return arts.get(0);
                    } else return null;
                }
                
                
                @Override
                public boolean canPlayAI() {
                    String phase = AllZone.Phase.getPhase();
                    return phase.equals(Constant.Phase.Main2) && getArtifact() != null;
                }
                
                @Override
                public void resolve() {
                    Card c = getTargetCard();
                    if(AllZone.GameAction.isCardInPlay(c)) {
                        AllZone.GameAction.sacrifice(c);
                        makeToken();
                        AllZone.GameAction.gainLife(card.getController(), 1);
                    }
                }//resolve
                
                public void makeToken() {
                    CardFactoryUtil.makeToken("Thopter", "U 1 1 Thopter", card, "U", new String[] {
                            "Artifact", "Creature", "Thopter"}, 1, 1, new String[] {"Flying"});
                }
            };
            
            Input runtime = new Input() {
                
                private static final long serialVersionUID = 3557158378851031238L;
                
                @Override
                public void showMessage() {
                    PlayerZone play = AllZone.getZone(Constant.Zone.Play, player);
                    
                    CardList arts = new CardList();
                    arts.addAll(play.getCards());
                    arts = arts.filter(new CardListFilter() {
                        public boolean addCard(Card c) {
                            return c.isArtifact() && !c.isToken();
                        }
                    });
                    
                    stopSetNext(CardFactoryUtil.input_targetSpecific(ability, arts,
                            "Select a non-token Artifact to sacrifice", false, false));
                    
                }//showMessage()
            };//Input
            
            card.addSpellAbility(ability);
            ability.setDescription("1, Sacrifice a nontoken artifact: Put a 1/1 blue Thopter artifact creature token with flying onto the battlefield. You gain 1 life.");
            ability.setStackDescription(card.getName()
                    + " - Put a 1/1 blue Thopter artifact creature token with flying onto the battlefield. You gain 1 life.");
            ability.setBeforePayMana(runtime);
            
        }//*************** END ************ END **************************

        
        //*************** START *********** START **************************
        else if(cardName.equals("Time Vault")) {
            final Ability_Tap ability = new Ability_Tap(card) {
                private static final long serialVersionUID = 5784473766585071504L;
                
                @Override
                public void resolve() {
                    //System.out.println("Turn: " + AllZone.Phase.getTurn());
                    AllZone.Phase.addExtraTurn(card.getController());
                }
            };
            card.addSpellAbility(ability);
            ability.setStackDescription(card + " - take an extra turn after this one.");
            ability.setDescription("Tap: Take an extra turn after this one.");
        }//*************** END ************ END **************************


        //*************** START *********** START **************************
        else if(cardName.equals("Millstone") || cardName.equals("Tower of Murmurs")) {
        	final int millCards = cardName.equals("Millstone") ? 2 : 8;
        	final String cost = cardName.equals("Millstone") ? "2" : "8";
        	final String desc = cardName.equals("Millstone") ? 
        			"2, tap: Target player puts the top two cards of his or her library into his or her graveyard." :
        			"8, tap: Target player puts the top eight cards of his or her library into his or her graveyard.";
            
        	Ability_Tap ab1 = new Ability_Tap(card, cost) {
                
                private static final long serialVersionUID = 42470566751344693L;
                
                @Override
                public boolean canPlayAI() {
                    String player = getTargetPlayer();
                    PlayerZone lib = AllZone.getZone(Constant.Zone.Library, player);
                    CardList libList = new CardList(lib.getCards());
                    return libList.size() > 0;
                }
                
                @Override
                public void resolve() {
                    AllZone.GameAction.mill(getTargetPlayer(), millCards);
                }
            };
            ab1.setChooseTargetAI(CardFactoryUtil.AI_targetHuman());
            ab1.setBeforePayMana(CardFactoryUtil.input_targetPlayer(ab1));
            ab1.setDescription(desc);
            card.addSpellAbility(ab1);
        }//*************** END ************ END **************************
        

        //*************** START *********** START **************************
        if(cardName.equals("Serpent Generator")) {
            final SpellAbility ability = new Ability_Tap(card, "4") {
                private static final long serialVersionUID = 8428205362391909464L;
                
                @Override
                public void resolve() {
                    CardFactoryUtil.makeToken(
                            "Snake",
                            "C 1 1 Snake",
                            card,
                            "",
                            new String[] {"Artifact", "Creature", "Snake"},
                            1,
                            1,
                            new String[] {"Whenever this creature deals damage to a player, that player gets a poison counter."});
                }
            };
            ability.setStackDescription("Put a 1/1 colorless Snake artifact creature token onto the battlefield. This creature has \"Whenever this creature deals damage to a player, that player gets a poison counter.\"");
            ability.setDescription("4, Tap: Put a 1/1 colorless Snake artifact creature token onto the battlefield. This creature has \"Whenever this creature deals damage to a player, that player gets a poison counter.\" (A player with ten or more poison counters loses the game.)");
            card.addSpellAbility(ability);
            
        }//*************** END ************ END **************************
                       
        
        //*************** START *********** START **************************
        else if(cardName.equals("Illusions of Grandeur")) {
            final SpellAbility gainLife = new Ability(card, "0") {
                @Override
                public void resolve() {
                    Card c = card;
                    AllZone.GameAction.gainLife(c.getController(), 20);
                }
            };
            
            final SpellAbility loseLife = new Ability(card, "0") {
                @Override
                public void resolve() {
                    Card c = card;
                    PlayerLife life = AllZone.GameAction.getPlayerLife(c.getController());
                    life.subtractLife(20,card);
                }
            };
            
            Command intoPlay = new Command() {
                private static final long serialVersionUID = 502892931516451254L;
                
                public void execute() {
                    gainLife.setStackDescription(card.getController() + " gains 20 life");
                    AllZone.Stack.add(gainLife);
                }
            };
            
            Command leavesPlay = new Command() {
                private static final long serialVersionUID = 5772999389072757369L;
                
                public void execute() {
                    loseLife.setStackDescription(card.getController() + " loses 20 life");
                    AllZone.Stack.add(loseLife);
                }
            };
            
            card.addComesIntoPlayCommand(intoPlay);
            card.addLeavesPlayCommand(leavesPlay);
            
        }//*************** END ************ END **************************       
  
	  
      //*************** START *********** START **************************
      else if(cardName.equals("Isochron Scepter"))
      {
    	  final Ability_Tap freeCast = new Ability_Tap(card, "2")
          {

			private static final long serialVersionUID = 4455819149429678456L;

			@Override
			public void resolve() {
				if(getSourceCard().getAttachedCards().length == 0)
				{
					//AllZone.Display.showMessage("You have not exiled a card.");
					return;
				}
				Card c = copyCard(getSourceCard().getAttachedCards()[0]);
				if(getSourceCard().getController().equals(Constant.Player.Computer))
				{
					for(SpellAbility sa:getSourceCard().getAttachedCards()[0].getSpellAbility())
		            if(sa.canPlayAI())
		            {
		            	ComputerUtil.playStackFree(sa);
		            	return;
		            }
				}
				else AllZone.GameAction.playCardNoCost(c);
			}
			
			public boolean canPlay()
			{
				if (!super.canPlay())
					return false;
				
				if (getSourceCard().getAttachedCards().length > 0)
				{
					Card c = copyCard(getSourceCard().getAttachedCards()[0]);
					if (ComputerAI_counterSpells2.KeywordedCounterspells.contains(c.getName()))
					{
						SpellAbility sa = c.getSpellAbility()[0];
						return sa.canPlay();
					}
					else return true;
				}
				else
					return false;
			}
			
			public boolean canPlayAI()
			{
				if (getSourceCard().getAttachedCards().length == 0)
					return false;
				for(SpellAbility sa:getSourceCard().getAttachedCards()[0].getSpellAbility())
                    if(sa.canPlayAI())
                    	return true;
                return false;
			}
          };
          freeCast.setDescription("2, Tap: You may copy the exiled card. If you do, you may cast the copy without paying its mana cost");
          freeCast.setStackDescription("Copy the exiled card and cast the copy without paying its mana cost.");
          
          final Input exile = new Input() {
              private static final long serialVersionUID = -6392468000100283596L;
              
              @Override
              public void showMessage() {
                  AllZone.Display.showMessage("You may exile an Instant with converted mana cost two or less from your hand");
                  ButtonUtil.enableOnlyCancel();
              }
              
              @Override
              public void selectCard(Card c, PlayerZone zone) {
                  if(zone.is(Constant.Zone.Hand) && c.isInstant() && CardUtil.getConvertedManaCost(c) <= 2)
                  {
                      AllZone.GameAction.moveTo(AllZone.Human_Removed, c);
                      card.attachCard(c);
                      stop();
                  }
              }
              
              @Override
              public void selectButtonCancel() {
                  stop();
              }
          };//Input
          
          final SpellAbility ability = new Ability(card, "0") {
              @Override
              public void resolve() {
                  if(card.getController().equals(Constant.Player.Human)) {
                      if(AllZone.Human_Hand.getCards().length > 0)
                    	  AllZone.InputControl.setInput(exile);
                  } else {
                      CardList list = new CardList(AllZone.Computer_Hand.getCards())
                        .filter(
                    		  new CardListFilter(){
                    			  public boolean addCard(Card c)
                    			  {
                    				  return c.isInstant()
                    				  && CardUtil.getConvertedManaCost(c) <=2 ;
                    			  }
                    		  });
                      CardListUtil.sortCMC(list);
                      list.reverse();
                      Card c = list.get(0);
                      AllZone.GameAction.moveTo(AllZone.Human_Removed, c);
                      card.attachCard(c);
                  }//else
              }//resolve()
          };//SpellAbility
          Command intoPlay = new Command() {
              private static final long serialVersionUID = 9202753910259054021L;
              
              public void execute() {
                  ability.setStackDescription("Imprint - " + card.getController()
                		  + " may exile an instant card with converted mana cost 2 or less from their hand.");
                  AllZone.Stack.add(ability);
              }
          };
          SpellAbility spell = new Spell_Permanent(card) {
              private static final long serialVersionUID = -2940969025405788931L;
              
              //could never get the AI to work correctly
              //it always played the same card 2 or 3 times
              @Override
              public boolean canPlayAI() {
                  for(Card c : AllZone.Computer_Hand.getCards())
                	  if(c.isInstant() && CardUtil.getConvertedManaCost(c) <=2)
                		  return true;
                  return false;
              }
          };
          card.addComesIntoPlayCommand(intoPlay);
          card.clearSpellAbility();
          card.addSpellAbility(spell);
          card.addSpellAbility(freeCast);
      }
      //*************** END ************ END **************************

        
      //*************** START *********** START **************************
      else if(cardName.equals("Helix Pinnacle"))
      {
    	  final Ability ability = new Ability(card, "0")
    	  {
    		  public void resolve()
    		  {
    			  getSourceCard().addCounter(Counters.TOWER, Integer.parseInt(getManaCost()));
    		  }
    		  public boolean canPlayAI()
    		  {
    			  int m = (int)Math.pow(2, CardFactoryUtil.getCards("Doubling Season", Constant.Player.Computer).size());
    			  int n = Math.max(1, Math.min((100-getSourceCard().getCounters(Counters.TOWER))/m,
    					           ComputerUtil.getAvailableMana().size())) ;
    			  setManaCost(n + "");
    			  return !(new CardList(AllZone.Computer_Hand.getCards()).containsName("Doubling Season") && n>=5 )
    			  			&&  m*n >= Math.min(20, 100 - getSourceCard().getCounters(Counters.TOWER));
    			  //Persuming if AI cast the Pinnacle, it has green mana
    		  }
    	  };
    	  ability.setBeforePayMana(new Input()
    	  {
    		private static final long serialVersionUID = 43786418486732L;

			public void showMessage()
    		 {
    			 String s = JOptionPane.showInputDialog("What would you like X to be?");
    	  		 try {
    	  			     Integer.parseInt(s);
    	  				 ability.setManaCost(s);
    	  				 stopSetNext(new Input_PayManaCost(ability));
    	  			 }
    	  			 catch(NumberFormatException e){
    	  				 AllZone.Display.showMessage("\"" + s + "\" is not a number.");
    	  				 showMessage();
    	  			 }
    		 }
    	  });
    	  ability.setDescription("X: Put X tower counters on Helix Pinnacle.");
    	  ability.setStackDescription("Put X counters on Helix Pinnacle");
    	  card.addSpellAbility(ability);
      }
      //*************** END ************ END **************************
        
    /*
        //*****************************START*******************************
        else if(cardName.equals("Icy Manipulator") || cardName.equals("Ring of Gix")) {
           // The Rules state that this can target a tapped card, but it won't do anything 
           
           final Ability_Tap ability = new Ability_Tap(card, "1") {
			private static final long serialVersionUID = 6349074398830621348L;
			public boolean canPlayAI() {
                 return false;
              }
              public void chooseTargetAI() {
                 //setTargetCard(c);
              }//chooseTargetAI()
              public void resolve() {
                 if(AllZone.GameAction.isCardInPlay(getTargetCard())) {
                    getTargetCard().tap();
                 }
              }
           };//SpellAbility
           
           card.addSpellAbility(ability);
           ability.setDescription("1, tap: Tap target artifact, creature or land.");
           ability.setBeforePayMana(CardFactoryUtil.input_targetType(ability, "Artifact;Creature;Land"));
        }//end Icy Manipulator
        //****************END*******END***********************
        */
 
        
        //*****************************START*******************************
        else if(cardName.equals("Jandor's Saddlebags")) {
        	/* Assuing the Rules state that this can target an untapped card,
        	 * but it won't do anything useful
        	 *
        	 * This would bring the ruling in line with Icy Manipulator
        	 * */

        	final Ability_Tap ability = new Ability_Tap(card, "3") {
        		private static final long serialVersionUID = 6349074098650621348L;
        		
        		@Override
        		public boolean canPlayAI() {
        			return false;
        		}
        		
        		@Override
        		public void resolve() {
        			if(AllZone.GameAction.isCardInPlay(getTargetCard())) {
        				getTargetCard().untap();
        			}
        		}
        	};//SpellAbility

        	card.addSpellAbility(ability);
        	ability.setDescription("3, tap: Untap target creature.");
        	ability.setBeforePayMana(CardFactoryUtil.input_targetType(ability, "Creature"));
        }//****************END*******END***********************
        
       
        //*************** START *********** START **************************
        else if(cardName.equals("Bottle of Suleiman")) {
           /*
            * Sacrifice Bottle of Suleiman: Flip a coin. If you lose the flip,
            * Bottle of Suleiman deals 5 damage to you. If you win the flip,
            * put a 5/5 colorless Djinn artifact creature token with flying
            * onto the battlefield.
            */
        	Ability_Cost abCost = new Ability_Cost("1 Sac<1/CARDNAME>", cardName, true);
           final SpellAbility ability = new Ability_Activated(card, abCost, null) {
             private static final long serialVersionUID = -5741302550353410000L;
             
             @Override
             public boolean canPlayAI() {
                 PlayerLife life = AllZone.GameAction.getPlayerLife(Constant.Player.Computer);
                 if( life.getLife() > 10 ) {
                    return true;
                 }
                 CardList play = new CardList(AllZone.Computer_Play.getCards());
                 play = play.getType("Creature");
                 if( play.size() == 0 ) {
                    return true;
                 }
                 return false;
              }
             
             @Override
              public void resolve() {
                 final String player = AllZone.Phase.getActivePlayer();
                 String choice = "";
                 String choices[] = {"heads","tails"};
                 boolean flip = MyRandom.percentTrue(50);
                 if(card.getController().equals(Constant.Player.Human)) {
                    choice = (String) AllZone.Display.getChoice("Choose one", choices);
                 }
                 else {
                    choice = choices[MyRandom.random.nextInt(2)];
                 }

                 if( (flip == true && choice.equals("heads")) ||   (flip == false && choice.equals("tails"))) {
                    JOptionPane.showMessageDialog(null, "Bottle of Suleiman - Win! - "+player+" puts a 5/5 Flying Djinn in play.", "Bottle of Suleiman", JOptionPane.PLAIN_MESSAGE);
                    CardFactoryUtil.makeToken("Djinn", "C 5 5 Djinn", card, "", new String[] {"Creature", "Artifact", "Djinn"}, 5, 5, new String[] {"Flying"});
                 }
                 else{
                    JOptionPane.showMessageDialog(null, "Bottle of Suleiman - Lose - Bottle does 5 damage to "+player+".", "Bottle of Suleiman", JOptionPane.PLAIN_MESSAGE);
                    AllZone.GameAction.addDamage(card.getController(), card, 5);
                 }
              }
           };//SpellAbility

           card.addSpellAbility(ability);
           ability.setDescription("1, Sacrifice Bottle of Suleiman: Flip a coin.  Win: Put 5/5 Djinn in play.  Lose: Does 5 damage to you.");
           ability.setStackDescription("Bottle of Suleiman - flip a coin");
        }//*************** END ************ END **************************
        
      
        //*************** START *********** START **************************
        else if(cardName.equals("Lodestone Bauble")) {
        	/* 1, Tap, Sacrifice Lodestone Bauble: Put up to four target basic
        	 * land cards from a player's graveyard on top of his or her library
        	 * in any order. That player draws a card at the beginning of the next
        	 * turn's upkeep.
        	 */

        	final Ability_Tap ability = new Ability_Tap(card, "1") {
        		private static final long serialVersionUID = -6711849408085138636L;

        		@Override
        		public boolean canPlayAI() {
        			return getComputerLands().size() >= 4;
        		}

        		@Override
        		public void chooseTargetAI() {
        			setTargetPlayer(Constant.Player.Computer);
        		}//chooseTargetAI()

        		@Override
        		public void resolve() {
        			final int limit = 4;   //at most, this can target 4 cards
        			final String player = getTargetPlayer();
        			PlayerZone grave = AllZone.getZone(Constant.Zone.Graveyard, player);
        			PlayerZone lib = AllZone.getZone(Constant.Zone.Library, player);

        			CardList lands = new CardList(grave.getCards());
        			lands = lands.filter(basicLands);
        			//this should probably be card.getController().equals(Constant.Player.Human) instead of player
        			//if(player.equals(Constant.Player.Human)) {
        			if(card.getController().equals(Constant.Player.Human)){ 
        				//now, select up to four lands
        				int end = -1;
        				end = Math.min(lands.size(), limit);
        				//TODO - maybe pop a message box here that no basic lands found (if necessary)
        				for(int i = 1; i <= end; i++) {
        					String Title = "Put on top of library: ";
        					if(i == 2) Title = "Put second from top of library: ";
        					if(i == 3) Title = "Put third from top of library: ";
        					if(i == 4) Title = "Put fourth from top of library: ";
        					Object o = AllZone.Display.getChoiceOptional(Title, lands.toArray());
        					if(o == null) break;
        					Card c_1 = (Card) o;
        					lands.remove(c_1); //remove from the display list
        					grave.remove(c_1); //remove from graveyard
        					lib.add(c_1, i - 1); //add to library
        				}
        			}
        			else { //Computer
        				//based on current AI, computer should always target himself.
        				CardList list = getComputerLands();
        				int max = list.size();
        				if (max > limit) max = limit;

        				for(int i=0;i<max;i++) {
        					grave.remove(list.get(i));
        					lib.add(list.get(i), i);
        				}
        			}
        			//now, sacrifice Lodestone Bauble
        			AllZone.GameAction.sacrifice(card);
        			/*
        			 * TODO - this draw is at End of Turn.  It should be at the beginning of next
        			 * upkeep when a mechanism is in place
        			 */
        			final Command draw = new Command() {
        				private static final long serialVersionUID = 8293374203043368969L;

        				public void execute() {
        					AllZone.GameAction.drawCard(getTargetPlayer());
        				}
        			};
        			AllZone.EndOfTurn.addAt(draw);
        		}

        		private CardList getComputerLands() {
        			CardList list = new CardList(AllZone.Computer_Graveyard.getCards());
        			//probably no need to sort the list...
        			return list.filter(basicLands);
        		}

        		private CardListFilter basicLands = new CardListFilter() {
        			public boolean addCard(Card c) {
        				//the isBasicLand() check here may be sufficient...
        				return c.isLand() && c.isBasicLand();
        			}
        		};

        	};//ability

        	//ability.setStackDescription("Put up to 4 basic lands from target player's graveyeard to the top of their library.  Draw a card at beginning of next upkeep.");
        	card.addSpellAbility(ability);
        	ability.setBeforePayMana(CardFactoryUtil.input_targetPlayer(ability));
        }//*************** END ************ END **************************
     
        
        //*************** START *********** START **************************
        else if(cardName.equals("Grindstone")) {
        	Ability_Tap ab1 = new Ability_Tap(card, "3") {
				private static final long serialVersionUID = -6281219446216L;

				@Override
        		public boolean canPlayAI() {
        			String player = getTargetPlayer();
        			PlayerZone lib = AllZone.getZone(Constant.Zone.Library, player);
        			CardList libList = new CardList(lib.getCards());
        			CardList list = AllZoneUtil.getCardsInPlay("Painter's Servant");
        			return libList.size() > 0 && list.size() > 0;
        		}

        		@Override
        		public void resolve() {
					PlayerZone lib = AllZone.getZone(Constant.Zone.Library, getTargetPlayer());
					PlayerZone grave = AllZone.getZone(Constant.Zone.Graveyard, getTargetPlayer());
					CardList libList = new CardList(lib.getCards());
					int count = 0;
					int broken = 0;
					for(int i = 0; i < libList.size(); i = i + 2) {
						Card c1 = null;
						Card c2 = null;
						if(i < libList.size()) c1 = libList.get(i);
						else broken = 1;
						if(i + 1 < libList.size()) c2 = libList.get(i + 1);
						else broken = 1;
						if(broken == 0) {
							ArrayList<String> C2Color = CardUtil.getColors(c2);
							broken = 1;
							for(int x = 0; x < C2Color.size(); x++) {
							if(CardUtil.getColors(c1).contains(C2Color.get(x)) && C2Color.get(x) != Constant.Color.Colorless)  {
								count = count + 1;
								broken = 0;
							} 				
							}
						}

					}
					count = (count * 2) + 2;
					int max = count;
					if(libList.size() < count) max = libList.size();

					for(int j = 0; j < max; j++) {
						Card c = libList.get(j);
						lib.remove(c);
						grave.add(c);
					}
				}
        	};
        	ab1.setChooseTargetAI(CardFactoryUtil.AI_targetHuman());
        	ab1.setBeforePayMana(CardFactoryUtil.input_targetPlayer(ab1));
        	card.addSpellAbility(ab1);
        }//*************** END ************ END **************************
        
        
        //*************** START *********** START **************************
        else if(cardName.equals("Keening Stone")) {
        	/*
        	 * 5, Tap: Target player puts the top X cards of his or her
        	 * library into his or her graveyard, where X is the number of
        	 * cards in that player's graveyard.
        	 */
        	Ability_Tap ab1 = new Ability_Tap(card, "5") {
				private static final long serialVersionUID = -6282104343089446216L;

				@Override
        		public boolean canPlayAI() {
        			String player = getTargetPlayer();
        			PlayerZone lib = AllZone.getZone(Constant.Zone.Library, player);
        			CardList libList = new CardList(lib.getCards());
        			return libList.size() > 0;
        		}

        		@Override
        		public void resolve() {
        			String player = getTargetPlayer();
        			AllZone.GameAction.mill(player,
        					AllZone.getZone(Constant.Zone.Graveyard, player).size());
        		}
        	};
        	ab1.setChooseTargetAI(CardFactoryUtil.AI_targetHuman());
        	ab1.setBeforePayMana(CardFactoryUtil.input_targetPlayer(ab1));
        	card.addSpellAbility(ab1);
        }//*************** END ************ END **************************
        
      
        //*************** START *********** START **************************
        else if(cardName.equals("Dreamstone Hedron")) {
        	final Ability_Tap ability = new Ability_Tap(card, "3") {
				private static final long serialVersionUID = 4493940591347356773L;

				@Override
        		public boolean canPlayAI() {
					PlayerZone lib = AllZone.getZone(Constant.Zone.Library, Constant.Player.Computer);
					return lib.size() > 0;
        		}

				@Override
				public void resolve() {
					final String player = card.getController();
					AllZone.GameAction.sacrifice(card);
					AllZone.GameAction.drawCards(player, 3);
				}
        	};
        	ability.setDescription("3, tap: Sacrifice Dreamstone Hedron: Draw 3 cards.");
        	ability.setStackDescription(cardName+" - Draw 3 cards.");
        	ability.setBeforePayMana(new Input_PayManaCost(ability));
        	card.addSpellAbility(ability);
        }//*************** END ************ END **************************

        
        //*************** START *********** START **************************
        else if(cardName.equals("Slate of Ancestry")) {
        	/*
        	 * 4, Tap, Discard your hand: Draw a card for each creature you control.
        	 */
            final Ability_Tap ability = new Ability_Tap(card, "4") {
				private static final long serialVersionUID = 5135410670684913401L;

				@Override
                public void resolve() {
                	final String player = card.getController();
                	// Discard hand into graveyard
                    PlayerZone hand = AllZone.getZone(Constant.Zone.Hand, player);
                    Card[] c = hand.getCards();
                    for(int i = 0; i < c.length; i++)
                        AllZone.GameAction.discard(c[i], this);
                    
                    // Draw a card for each creature
                    CardList creatures = AllZoneUtil.getCreaturesInPlay(player);
                    for(int i = 0; i < creatures.size(); i++)
                        AllZone.GameAction.drawCard(player);
                	
                }//resolve()

                @Override
                public boolean canPlayAI() {
                	CardList creatures = AllZoneUtil.getCreaturesInPlay(Constant.Player.Computer);
                    CardList hand = AllZoneUtil.getPlayerHand(Constant.Player.Computer);
                    return creatures.size() > hand.size();
                }
                
            };//Ability_Tap
            
            ability.setDescription("4, tap: Discard your hand: Draw a card for each creature you control.");
            ability.setStackDescription(cardName+" - discard hand and draw 1 card for every creature you control.");
            ability.setBeforePayMana(new Input_PayManaCost(ability));
            card.addSpellAbility(ability);
        }//*************** END ************ END **************************
        
        
      //*************** START *********** START **************************
        else if(cardName.equals("Glasses of Urza")) {
            final Ability_Tap ability = new Ability_Tap(card, "0") {
                private static final long serialVersionUID = -3857979945891501990L;

                @Override
                public void resolve() {
                    final String player = getTargetPlayer();
                    CardList hand = AllZoneUtil.getPlayerHand(player);
                    if (hand.size() > 0) {
                        AllZone.Display.getChoice("Target player's hand", hand.toArray());
                    } else {
                    	StringBuilder sb = new StringBuilder();
                        sb.append(getTargetPlayer()).append("'s hand is empty!");
                        javax.swing.JOptionPane.showMessageDialog(null, sb.toString(), "Target player's hand", JOptionPane.INFORMATION_MESSAGE);
                    }
                }//resolve()

                @Override
                public boolean canPlayAI() {
                    return false;
                }

            };//SpellAbility

            ability.setBeforePayMana(CardFactoryUtil.input_targetPlayer(ability));
            card.addSpellAbility(ability);
        }//*************** END ************ END **************************

        
        //*************** START *********** START **************************
        else if(cardName.equals("Everflowing Chalice")) {
        	final Ability_Mana addMana = new Ability_Mana(card, "tap: add 1 to your mana pool for each charge counter on Everflowing Chalice.") {
 				private static final long serialVersionUID = -2661488839088242789L;

 				@Override
				public String mana() {
						return Integer.toString(card.getCounters(Counters.CHARGE));
                }
				                      		
        	};

        	final Ability addChargeCounters = new Ability(card, "0") {
                @Override
                public void resolve() {
                    card.addCounter(Counters.CHARGE, card.getMultiKickerMagnitude());
                    card.setMultiKickerMagnitude(0);
                }
            };
            StringBuilder sb = new StringBuilder();
            sb.append(cardName);
            sb.append(" enters the battlefield with a charge counter on it for each time it was kicked.");
            addChargeCounters.setStackDescription(sb.toString());
            
            final Command comesIntoPlay = new Command() {
				private static final long serialVersionUID = 4245563898487609274L;

				public void execute() {
                    AllZone.Stack.add(addChargeCounters);
                }
            };
            card.addSpellAbility(addMana);
            card.addComesIntoPlayCommand(comesIntoPlay);
        }//*************** END ************ END **************************
        
        
        //*************** START *********** START **************************
        else if(cardName.equals("Chromatic Star")) {
            final SpellAbility ability = new Ability(card, "0") {
                @Override
                public void resolve() {
                    AllZone.GameAction.drawCard(card.getController());
                }
            };
            Command destroy = new Command() {
				private static final long serialVersionUID = 7982507967024313067L;

				public void execute() {
                    ability.setStackDescription(card.getName() + " - " + card.getController() + " draws a card");
                    AllZone.Stack.add(ability);
                }
            };
            card.addDestroyCommand(destroy);
        }//*************** END ************ END **************************   
   
        
        //*************** START *********** START **************************
        else if(cardName.equals("Feldon's Cane")) {
        	/*
        	 * Tap, Exile Feldon's Cane: Shuffle your graveyard into your library.
        	 */
        	final Ability_Tap ability = new Ability_Tap(card, "0") {
				private static final long serialVersionUID = -1299603105585632846L;

				@Override
        		public void resolve() {
        			final String player = card.getController();
        			CardList grave = AllZoneUtil.getPlayerGraveyard(player);
        			PlayerZone lib = AllZone.getZone(Constant.Zone.Library, player);
        			//exile Feldon's Cane
        			AllZone.GameAction.removeFromGame(card);
        			
        			for(Card c:grave) {
        				lib.add(c);
        			}
        			AllZone.getZone(Constant.Zone.Graveyard, player).reset();
        			AllZone.GameAction.shuffle(player);
        		}

        		@Override
        		public boolean canPlayAI() {
        			PlayerZone lib = AllZone.getZone(Constant.Zone.Library, Constant.Player.Computer);
        			return lib.size() < 5;
        		}

        	};//SpellAbility

        	ability.setStackDescription(cardName+" - Player shuffles grave into library.");
        	card.addSpellAbility(ability);
        }//*************** END ************ END **************************


        //*************** START *********** START **************************
        else if(cardName.equals("Curse of Wizardry")) {
            final Ability ability = new Ability(card, "0") {
                @Override
                public void resolve() {
                    if(card.getController().equals(Constant.Player.Human)) {
                        
                        String color = "";
                        String[] colors = Constant.Color.Colors;
                        colors[colors.length - 1] = null;
                        
                        Object o = AllZone.Display.getChoice("Choose color", colors);
                        color = (String) o;
                        card.setChosenColor(color);
                    } else {
                        CardList list = AllZoneUtil.getPlayerCardsInLibrary(Constant.Player.Human);
                        list.add(AllZoneUtil.getPlayerHand(Constant.Player.Human));
                        
                        if(list.size() > 0) {
                            String color = CardFactoryUtil.getMostProminentColor(list);
                            if(!color.equals("")) card.setChosenColor(color);
                            else card.setChosenColor("black");
                        } else {
                            card.setChosenColor("black");
                        }
                    }
                }
            };
            Command comesIntoPlay = new Command() {
				private static final long serialVersionUID = -6417019967914398902L;

				public void execute() {
                    AllZone.Stack.add(ability);
                }
            };//Command
            ability.setStackDescription("As "+cardName+" enters the battlefield, choose a color.");
            card.addComesIntoPlayCommand(comesIntoPlay);
        }//*************** END ************ END **************************
 
        
        //*************** START *********** START **************************
        else if(cardName.equals("Arena of the Ancients")) {
        	/*
        	 * When Arena of the Ancients enters the battlefield, tap
        	 * all legendary creatures.
        	 */
        	final SpellAbility ability = new Ability(card, "0") {
        		@Override
        		public void resolve() {
        			CardList legends = AllZoneUtil.getTypeInPlay("Legendary");
        			legends = legends.filter(AllZoneUtil.creatures);
        			for(int i = 0; i < legends.size(); i++) {
        				Card c = legends.get(i);
        				if(c.isUntapped()) c.tap();
        			}
        		}
        	};//ability
        	Command intoPlay = new Command() {
				private static final long serialVersionUID = 3564466123797650567L;

				public void execute() {
        			ability.setStackDescription("When " + card.getName()
        					+ " comes into play, tap all Legendary creatures.");
        			AllZone.Stack.add(ability);
        		}
        	};
        	card.addComesIntoPlayCommand(intoPlay);
        }//*************** END ************ END **************************
      
        
        //*************** START *********** START **************************
        else if(cardName.equals("Expedition Map")) {
        	final Ability_Tap ability = new Ability_Tap(card, "2") {

 				private static final long serialVersionUID = -5796728507926918991L;

				@Override
        		public boolean canPlayAI() {
        			return AllZoneUtil.getPlayerTypeInLibrary(Constant.Player.Computer,
        					"Land").size() >= 1;
        		}

        		@Override
        		public void resolve() {
        			AllZone.GameAction.searchLibraryLand("Land", 
        					card.getController(), Constant.Zone.Hand, false);
        			AllZone.GameAction.sacrifice(card);
        		}
        	};//ability

        	ability.setDescription("2, tap, sacrifice Expedition Map: Search your library for a land card, reveal it, and put it into your hand. Then shuffle your library.");
        	ability.setStackDescription("Sacrifice Expedition Map: search your library for a land and put it into your hand.");
        	ability.setManaCost("2");
        	card.addSpellAbility(ability);
        }//*************** END ************ END **************************
        
       
        //*************** START *********** START **************************
        else if(cardName.equals("Tormod's Crypt")) {
        	/*
        	 * Tap, Sacrifice Tormod's Crypt: Exile all cards from target player's graveyard.
        	 */
        	final Ability_Tap ability = new Ability_Tap(card, "0") {

				private static final long serialVersionUID = -8877371657709894494L;

				@Override
        		public void resolve() {
					if (card.getController().equals(Constant.Player.Computer))
						setTargetPlayer(Constant.Player.Human);
					
        			final String player = getTargetPlayer();
        			CardList grave = AllZoneUtil.getPlayerGraveyard(player);
        			//sac tormod's crypt
        			AllZone.GameAction.sacrifice(card);
        			
        			for(Card c:grave) {
        				AllZone.GameAction.removeFromGame(c);
        			}
        			AllZone.getZone(Constant.Zone.Graveyard, player).reset();
        			AllZone.GameAction.shuffle(player);
        		}

        		@Override
        		public boolean canPlayAI() {
        			PlayerZone grave = AllZone.getZone(Constant.Zone.Library, Constant.Player.Human);
        			return grave.size() < 15;
        		}

        	};//SpellAbility
        	ability.setDescription("Tap, Sacrifice Tormod's Crypt: Exile all cards from target player's graveyard.");
        	ability.setBeforePayMana(CardFactoryUtil.input_targetPlayer(ability));
        	card.addSpellAbility(ability);
        }//*************** END ************ END **************************
        
        
        //*************** START *********** START **************************
        else if(cardName.equals("Journey to Nowhere")) {

        	final CommandReturn getPerm = new CommandReturn() {
        		public Object execute() {
        			//get all creatures
        			CardList tempList = new CardList();
        			tempList.addAll(AllZone.Human_Play.getCards());
        			tempList.addAll(AllZone.Computer_Play.getCards());

        			CardList list = new CardList();

        			for(int i = 0; i < tempList.size(); i++) {
        				if(tempList.get(i).isPermanent() && tempList.get(i).isCreature()
        						&& CardFactoryUtil.canTarget(card, tempList.get(i))) list.add(tempList.get(i));
        			}//for

        			//remove "this card"
        			list.remove(card);

        			return list;

        		}//execute
        	};//CommandReturn

        	final SpellAbility abilityComes = new Ability(card, "0") {

        		@Override
        		public void resolve() {
        			if(AllZone.GameAction.isCardInPlay(getTargetCard())
        					&& CardFactoryUtil.canTarget(card, getTargetCard())) {
        				AllZone.GameAction.removeFromGame(getTargetCard());
        			}//if
        		}//resolve()
        	};//abilityComes

        	final Input inputComes = new Input() {
        		private static final long serialVersionUID = -3613946694360326887L;

        		@Override
        		public void showMessage() {
        			CardList choice = (CardList) getPerm.execute();

        			stopSetNext(CardFactoryUtil.input_targetSpecific(abilityComes, choice,
        					"Select target creature to remove from the game", true, false));
        			ButtonUtil.disableAll();//to disable the Cancel button
        		}//showMessage
        	};//inputComes
        	
        	Command commandComes = new Command() {
        		private static final long serialVersionUID = -6250376920501373535L;

        		public void execute() {
        			CardList perm = (CardList) getPerm.execute();
        			String s = card.getController();
        			if(perm.size() == 0) return;
        			else if(s.equals(Constant.Player.Human)) AllZone.InputControl.setInput(inputComes);
        			else //computer
        			{
        				Card target;

        				//try to target human creature
        				CardList human = CardFactoryUtil.AI_getHumanCreature(card, true);
        				target = CardFactoryUtil.AI_getBestCreature(human);//returns null if list is empty

        				// try to target human permanent
        				if(target == null) {
        					int convertedCost = 0;
        					CardList tempList = new CardList();
        					tempList.addAll(AllZone.Human_Play.getCards());

        					for(int i = 0; i < tempList.size(); i++) {
        						if(tempList.get(i).isPermanent()
        								&& !tempList.get(i).isLand()
        								&& CardFactoryUtil.canTarget(card, tempList.get(i))
        								&& (CardUtil.getConvertedManaCost(tempList.get(i).getManaCost()) > convertedCost)) {
        							target = tempList.get(i);
        							convertedCost = CardUtil.getConvertedManaCost(tempList.get(i).getManaCost());
        						}//if
        					}//for
        				}//if

        				//target something cheaper (manacost 0?) instead:
        				if(target == null) {
        					CardList humanPerms = new CardList();
        					humanPerms.addAll(AllZone.Human_Play.getCards());
        					humanPerms = humanPerms.filter(new CardListFilter() {
        						public boolean addCard(Card c) {
        							return c.isPermanent() && !c.isLand() && CardFactoryUtil.canTarget(card, c);
        						}
        					});

        					if(humanPerms.size() > 0) target = humanPerms.get(0);
        				}//if

        				if(target == null) {
        					//must target computer creature
        					CardList computer = new CardList(AllZone.Computer_Play.getCards());
        					computer = computer.getType("Creature");
        					computer.remove(card);

        					computer.shuffle();
        					if(computer.size() != 0) target = computer.get(0);
        					else target = card;
        				}//if

        				abilityComes.setTargetCard(target);
        				AllZone.Stack.add(abilityComes);
        			}//else
        		}//execute()
        	};//CommandComes

        	Command commandLeavesPlay = new Command() {
        		private static final long serialVersionUID = 6997038208952910355L;

        		public void execute() {
        			Object o = abilityComes.getTargetCard();
        			if(o == null || ((Card) o).isToken() || !AllZone.GameAction.isCardRemovedFromGame((Card) o)) return;

        			SpellAbility ability = new Ability(card, "0") {

        				@Override
        				public void resolve() {
        					//copy card to reset card attributes like attack and defense
        					Card c = abilityComes.getTargetCard();
        					if(!c.isToken()) {
        						c = AllZone.CardFactory.dynamicCopyCard(c);
        						c.setController(c.getOwner());

        						PlayerZone play = AllZone.getZone(Constant.Zone.Play, c.getOwner());
        						PlayerZone removed = AllZone.getZone(Constant.Zone.Removed_From_Play, c.getOwner());
        						removed.remove(c);
        						if (c.isTapped()) c.untap();

        						play.add(c);

        					}//if
        				}//resolve()
        			};//SpellAbility
        			ability.setStackDescription("Journey To Nowhere - returning creature to play.");
        			AllZone.Stack.add(ability);
        		}//execute()
        	};//Command

        	card.addComesIntoPlayCommand(commandComes);
        	card.addLeavesPlayCommand(commandLeavesPlay);

        	card.setSVar("PlayMain1", "TRUE");

        	card.clearSpellAbility();
		
        	card.addSpellAbility(new Spell_Permanent(card) {
        		private static final long serialVersionUID = -3250095291930182087L;

        		@Override
        		public boolean canPlayAI() {
        			Object o = getPerm.execute();
        			if(o == null) return false;

        			CardList cList = new CardList(AllZone.Human_Play.getCards());
        			cList = cList.filter(new CardListFilter() {
        				public boolean addCard(Card crd) {
        					return CardFactoryUtil.canTarget(card, crd) && crd.isCreature();
        				}
        			});

        			CardList cl = (CardList) getPerm.execute();
					return (o != null) && cList.size() > 0 && cl.size() > 0 && AllZone.getZone(getSourceCard()).is(Constant.Zone.Hand);
        		}//canPlayAI
        	});//addSpellAbility
        }//*************** END ************ END **************************

        
        //*************** START *********** START **************************
        else if(cardName.equals("Recurring Nightmare")) {
        	/*
        	 * Sacrifice a creature, Return Recurring Nightmare to its owner's
        	 * hand: Return target creature card from your graveyard to the
        	 * battlefield. Activate this ability only any time you could cast
        	 * a sorcery.
        	 */
        	final Ability ability = new Ability(card, "0") {
        		
        		@Override
        		public boolean canPlayAI() {
        			return false;
        		}
        		
        		@Override
        		public boolean canPlay() {
        			return super.canPlay() && Phase.canCastSorcery(card.getController());
        		}

        		@Override
        		public void resolve() {
        			if(AllZone.GameAction.isCardInPlay(getTargetCard())) {
        				//choose a card from graveyard
        				String player = card.getController();
        				CardList grave = AllZoneUtil.getPlayerGraveyard(player);
        				grave = grave.filter(AllZoneUtil.creatures);
        				final String title = "Select creature";
        				Object o = AllZone.Display.getChoiceOptional(title, grave.toArray());
        				if(null != o) {
        					Card toReturn = (Card) o;
        					PlayerZone play = AllZone.getZone(Constant.Zone.Play, player);
        					AllZone.GameAction.moveTo(play, toReturn);
        				}
        				//sacrifice the creature (target card)
        				AllZone.GameAction.sacrifice(getTargetCard());
        				//return Recurring Nightmare to player's hand
        				AllZone.GameAction.moveToHand(card);
        			}
        		}

        	};//Ability

        	Input target = new Input() {
				private static final long serialVersionUID = 8486351837945158454L;
				public void showMessage() {
        			AllZone.Display.showMessage("Select target creature to sacrifice");
        			ButtonUtil.enableOnlyCancel();
        		}
        		public void selectButtonCancel() {stop();}
        		public void selectCard(Card c, PlayerZone zone) {
        			if(c.isCreature() && zone.is(Constant.Zone.Play, Constant.Player.Human)) {
        				ability.setTargetCard(c);
        				AllZone.Stack.add(ability);
        				stop();
        			}
        		}//selectCard()
        	};//Input

        	card.addSpellAbility(ability);
        	ability.setBeforePayMana(target);
        }//*************** END ************ END **************************
        
        
        //*************** START *********** START **************************
        else if(cardName.equals("Mirror Universe")) {
        	/*
        	 * Tap, Sacrifice Mirror Universe: Exchange life totals with
        	 * target opponent. Activate this ability only during your upkeep.
        	 */
        	final Ability_Tap ability = new Ability_Tap(card, "0") {
        		private static final long serialVersionUID = -1409850598108909739L;

        		@Override
        		public void resolve() {
        			String player = card.getController();
        			String opponent = AllZone.GameAction.getOpponent(player);
        			PlayerLife playerLife = AllZone.GameAction.getPlayerLife(player);
        			PlayerLife opponentLife = AllZone.GameAction.getPlayerLife(opponent);
        			int tmp = playerLife.getLife();
        			playerLife.setLife(opponentLife.getLife());
        			opponentLife.setLife(tmp);
        			AllZone.GameAction.sacrifice(card);
        		}

        		@Override
        		public boolean canPlay() {
        			//TODO: This should be limited to Upkeep when we have a phase for that
        			return super.canPlay() && AllZone.Phase.getPhase().equals(Constant.Phase.Main1);
        		}

        		@Override
        		public boolean canPlayAI() {
        			PlayerLife compy = AllZone.GameAction.getPlayerLife(Constant.Player.Computer);
        			PlayerLife human = AllZone.GameAction.getPlayerLife(Constant.Player.Human);
        			if(compy.getLife() < 5 && human.getLife() > 5) {
        				return true;
        			}
        			else if(compy.getLife() == 1) {
        				return true;
        			}
        			else if((human.getLife() - compy.getLife()) > 10) {
        				return true;
        			}
        			else return false;
        		}
        	};//SpellAbility
        	
        	ability.setStackDescription(cardName+" - Exchange life totals with target opponent.");
        	card.addSpellAbility(ability);
        }//*************** END ************ END **************************
        
        
        //*************** START *********** START **************************
        else if(cardName.equals("Jade Statue")) {
        	/*
        	 * 2: Jade Statue becomes a 3/6 Golem artifact creature until
        	 * end of combat. Activate this ability only during combat.
        	 */
        	final long[] timeStamp = new long[1];
            
            final SpellAbility a1 = new Ability(card, "2") {
                @Override
                public boolean canPlayAI() {
                    return false;
                }
                
                @Override
                public boolean canPlay() {
                	return Phase.canPlayDuringCombat();
                }
                
                @Override
                public void resolve() {
                    Card c = card;
                    String[] types = { "Creature", "Golem" };
                    String[] keywords = {  };
                    timeStamp[0] = CardFactoryUtil.activateManland(c, 3, 6, types, keywords, "4");

                    final Command untilEOC = new Command() {
        				private static final long serialVersionUID = -8432597117196682284L;
        				long stamp = timeStamp[0];
        				public void execute() {
                            Card c = card;
                            String[] types = { "Creature", "Golem" };
                            String[] keywords = {  };
                            CardFactoryUtil.revertManland(c, types, keywords, "4", stamp);
                        }
                    };
                    
                    AllZone.EndOfCombat.addUntil(untilEOC);
                }
            };//SpellAbility
            
            //card.clearSpellKeepManaAbility();
            card.addSpellAbility(a1);
            a1.setStackDescription(card + " becomes a 3/6 Golem creature until End of Combat");
            
            Command paid1 = new Command() {
				private static final long serialVersionUID = 1531378274457977155L;

				public void execute() {
                    AllZone.Stack.add(a1);
                }
            };
            
            a1.setBeforePayMana(new Input_PayManaCost_Ability(a1.getManaCost(), paid1));
        }//*************** END ************ END **************************

        
        //*************** START *********** START **************************
        else if(cardName.equals("Jester's Cap")) {
        	/*
        	 * 2, Tap, Sacrifice Jester's Cap: Search target player's library
        	 * for three cards and exile them. Then that player shuffles his
        	 * or her library.
        	 */
        	final Ability_Tap ability = new Ability_Tap(card, "2") {
				private static final long serialVersionUID = 222308932796127795L;

				@Override
        		public boolean canPlayAI() {
        			//TODO
        			return false;
        		}

        		@Override
        		public void resolve() {
        			String target = getTargetPlayer();
        			String player = card.getController();
        			PlayerZone zone = AllZone.getZone(Constant.Zone.Library, target);
        			if(player.equals(Constant.Player.Human)) {
        				AllZoneUtil.exileNCardsFromZone(zone, null, 3, true);
        			}
        			else { //computer
        				
        			}
        			AllZone.GameAction.sacrifice(card);
        		}

        	};//Ability

        	card.addSpellAbility(ability);
        	ability.setBeforePayMana(CardFactoryUtil.input_targetPlayer(ability));
        }//*************** END ************ END **************************
        
        
        //*************** START *********** START **************************
        else if (cardName.equals("An-Zerrin Ruins")) {
        	
        	final Ability_Static comesIntoPlayAbility = new Ability_Static(card, "0") {
                @Override
                public void resolve() {
                	String chosenType = "";
        			if(card.getController().equals(Constant.Player.Human)) {
        				chosenType = JOptionPane.showInputDialog(null, "Enter a creature type:", card.getName(),
        						JOptionPane.QUESTION_MESSAGE);
        			}
        			else {
        				//not implemented for AI
        			}
        			if (!CardUtil.isCreatureType(chosenType)) chosenType = "";
        			card.setChosenType(chosenType);
                }//resolve()
            }; //comesIntoPlayAbility
            
            Command intoPlay = new Command() {
				private static final long serialVersionUID = 2985015252466920757L;

				public void execute() {
                	comesIntoPlayAbility.setStackDescription(card.getName()+" - choose a creature type.  Creatures of that type do not untap during their controller's untap step.");
                	AllZone.Stack.add(comesIntoPlayAbility);
                }
            };
            
            card.addComesIntoPlayCommand(intoPlay);
        }//*************** END ************ END **************************
        
        
        //*************** START *********** START **************************
        else if(cardName.equals("Tumble Magnet")) {
        	/*
        	 * Tumble Magnet enters the battlefield with three charge
        	 * counters on it.
        	 * Tap, Remove a charge counter from Tumble Magnet: Tap target
        	 * artifact or creature.
        	 */
        	final Ability_Tap ability = new Ability_Tap(card, "0") {
				private static final long serialVersionUID = -5513092896385146010L;

				@Override
        		public boolean canPlayAI() {
        			return false;
        		}
				
				@Override
				public boolean canPlay() {
					return card.getCounters(Counters.CHARGE) > 0;
				}

        		@Override
        		public void resolve() {
        			Card target = getTargetCard();
        			//remove charge counter
        			card.subtractCounter(Counters.CHARGE, 1);
        			card.tap();
        			
        			if(CardFactoryUtil.canTarget(card, target) && AllZoneUtil.isCardInPlay(target)) {
        				target.tap();
        			}
        		}

        	};//Ability

        	card.addSpellAbility(ability);
        	ability.setBeforePayMana(CardFactoryUtil.input_targetType(ability, "Creature;Artifact"));
        }//*************** END ************ END **************************
        
        //*************** START *********** START **************************
        else if(cardName.equals("Barl's Cage")) {
            Target target = new Target("TgtV");
            target.setVTSelection("Select target creature.");
            final String Tgts[] = {"Creature"};
            target.setValidTgts(Tgts);

            final Ability_Cost cost = new Ability_Cost("3", card.getName(), true);
        	
            final SpellAbility ability = new Ability_Activated(card, cost, target) {
				private static final long serialVersionUID = 8941566961041310961L;

				@Override
                public boolean canPlayAI() {
                    Card c = getCreature();
                    if(c == null) return false;
                    else {
                        setTargetCard(c);
                        return true;
                    }
                }//canPlayAI()
                
                //may return null
                public Card getCreature() {
                    CardList tappedCreatures = AllZoneUtil.getCreaturesInPlay();
                    tappedCreatures = tappedCreatures.filter(AllZoneUtil.tapped);
                    tappedCreatures = tappedCreatures.filter(AllZoneUtil.getCanTargetFilter(card));
                    if(tappedCreatures.isEmpty()) return null;
                    
                    return CardFactoryUtil.AI_getBestCreature(tappedCreatures);
                }
                
                @Override
                public void resolve() {
                	Card target = getTargetCard();
                    if(AllZone.GameAction.isCardInPlay(target)
                            && CardFactoryUtil.canTarget(card, target)) {
                    	target.addExtrinsicKeyword("This card doesn't untap during your next untap step.");
                    }//is card in play?
                }//resolve()
            };//SpellAbility
            
            card.addSpellAbility(ability); 
        }//*************** END ************ END **************************

        
        // Cards with Cycling abilities
        // -1 means keyword "Cycling" not found
        if(hasKeyword(card, "Cycling") != -1) {
            int n = hasKeyword(card, "Cycling");
            if(n != -1) {
                String parse = card.getKeyword().get(n).toString();
                card.removeIntrinsicKeyword(parse);
                
                String k[] = parse.split(":");
                final String manacost = k[1];
                
                card.addSpellAbility(CardFactoryUtil.ability_cycle(card, manacost));
            }
        }//Cycling

        while(hasKeyword(card, "TypeCycling") != -1) {
            int n = hasKeyword(card, "TypeCycling");
            if(n != -1) {
                String parse = card.getKeyword().get(n).toString();
                card.removeIntrinsicKeyword(parse);
                
                String k[] = parse.split(":");
                final String type = k[1];
                final String manacost = k[2];
                
                card.addSpellAbility(CardFactoryUtil.ability_typecycle(card, manacost, type));
            }
        }//TypeCycling
        
        if(hasKeyword(card, "Transmute") != -1) {
            int n = hasKeyword(card, "Transmute");
            if(n != -1) {
                String parse = card.getKeyword().get(n).toString();
                card.removeIntrinsicKeyword(parse);
                
                String k[] = parse.split(":");
                final String manacost = k[1];
                
                card.addSpellAbility(CardFactoryUtil.ability_transmute(card, manacost));
            }
        }//transmute
        
        while(hasKeyword(card, "Soulshift") != -1) {
            int n = hasKeyword(card, "Soulshift");
            if(n != -1) {
                String parse = card.getKeyword().get(n).toString();
                card.removeIntrinsicKeyword(parse);
                
                String k[] = parse.split(":");
                final String manacost = k[1];
                
                card.addSpellAbility(CardFactoryUtil.soul_desc(card, manacost));
                card.addDestroyCommand(CardFactoryUtil.ability_Soulshift(card, manacost));
            }
        }//Soulshift
        
        if(hasKeyword(card, "Echo") != -1) {
            int n = hasKeyword(card, "Echo");
            if(n != -1) {
                String parse = card.getKeyword().get(n).toString();
                //card.removeIntrinsicKeyword(parse);
                
                String k[] = parse.split(":");
                final String manacost = k[1];
                
                card.setEchoCost(manacost);
                
                final Command intoPlay = new Command() {

					private static final long serialVersionUID = -7913835645603984242L;

					public void execute() {
                        card.addIntrinsicKeyword("(Echo unpaid)");
                    }
                };
                card.addComesIntoPlayCommand(intoPlay);
                
            }
        }//echo
        
        if(hasKeyword(card,"HandSize") != -1) {
            String toParse = card.getKeyword().get(hasKeyword(card,"HandSize"));
            card.removeIntrinsicKeyword(toParse);
           
            String parts[] = toParse.split(" ");
            final String Mode = parts[1];
            final int Amount;
            if(parts[2].equals("INF")) {
               Amount = -1;
            }
            else {
               Amount = Integer.parseInt(parts[2]);
            }
            final String Target = parts[3];
           
            final Command entersPlay,leavesPlay, controllerChanges;
           
            entersPlay = new Command() {
               private static final long serialVersionUID = 98743547743456L;
               
               public void execute() {
                 card.setSVar("HSStamp","" + Input_Cleanup.GetHandSizeStamp());
                  if(card.getController() == Constant.Player.Human) {
                     //System.out.println("Human played me! Mode(" + Mode + ") Amount(" + Amount + ") Target(" + Target + ")" );
                     if(Target.equals("Self")) {
                        Input_Cleanup.addHandSizeOperation(new HandSizeOp(Mode,Amount,Integer.parseInt(card.getSVar("HSStamp"))));
                     }
                     else if(Target.equals("Opponent")) {
                        Computer_Cleanup.addHandSizeOperation(new HandSizeOp(Mode,Amount,Integer.parseInt(card.getSVar("HSStamp"))));
                     }
                     else if(Target.equals("All")) {
                        Computer_Cleanup.addHandSizeOperation(new HandSizeOp(Mode,Amount,Integer.parseInt(card.getSVar("HSStamp"))));
                        Input_Cleanup.addHandSizeOperation(new HandSizeOp(Mode,Amount,Integer.parseInt(card.getSVar("HSStamp"))));
                     }
                  }
                  else
                  {
                     //System.out.println("Compy played me! Mode(" + Mode + ") Amount(" + Amount + ") Target(" + Target + ")" );
                     if(Target.equals("Self")) {
                        Computer_Cleanup.addHandSizeOperation(new HandSizeOp(Mode,Amount,Integer.parseInt(card.getSVar("HSStamp"))));
                     }
                     else if(Target.equals("Opponent")) {
                        Input_Cleanup.addHandSizeOperation(new HandSizeOp(Mode,Amount,Integer.parseInt(card.getSVar("HSStamp"))));
                     }
                     else if(Target.equals("All")) {
                        Computer_Cleanup.addHandSizeOperation(new HandSizeOp(Mode,Amount,Integer.parseInt(card.getSVar("HSStamp"))));
                        Input_Cleanup.addHandSizeOperation(new HandSizeOp(Mode,Amount,Integer.parseInt(card.getSVar("HSStamp"))));
                     }
                  }
               }
            };
           
            leavesPlay = new Command() {
               private static final long serialVersionUID = -6843545358873L;
               
               public void execute() {
                  if(card.getController() == Constant.Player.Human) {
                     if(Target.equals("Self")) {
                        Input_Cleanup.removeHandSizeOperation(Integer.parseInt(card.getSVar("HSStamp")));
                     }
                     else if(Target.equals("Opponent")) {
                        Computer_Cleanup.removeHandSizeOperation(Integer.parseInt(card.getSVar("HSStamp")));
                     }
                     else if(Target.equals("All")) {
                        Computer_Cleanup.removeHandSizeOperation(Integer.parseInt(card.getSVar("HSStamp")));
                        Input_Cleanup.removeHandSizeOperation(Integer.parseInt(card.getSVar("HSStamp")));
                     }
                  }
                  else
                  {
                     if(Target.equals("Self")) {
                        Computer_Cleanup.removeHandSizeOperation(Integer.parseInt(card.getSVar("HSStamp")));
                     }
                     else if(Target.equals("Opponent")) {
                        Input_Cleanup.removeHandSizeOperation(Integer.parseInt(card.getSVar("HSStamp")));
                     }
                     else if(Target.equals("All")) {
                        Computer_Cleanup.removeHandSizeOperation(Integer.parseInt(card.getSVar("HSStamp")));
                        Input_Cleanup.removeHandSizeOperation(Integer.parseInt(card.getSVar("HSStamp")));
                     }
                  }
               }
            };
           
            controllerChanges = new Command() {
               private static final long serialVersionUID = 778987998465463L;
               
               public void execute() {
                  Log.debug("HandSize", "Control changed: " + card.getController());
                  if(card.getController().equals(Constant.Player.Human)) {
                     Input_Cleanup.removeHandSizeOperation(Integer.parseInt(card.getSVar("HSStamp")));
                     Computer_Cleanup.addHandSizeOperation(new HandSizeOp(Mode,Amount,Integer.parseInt(card.getSVar("HSStamp"))));
                     
                     Computer_Cleanup.sortHandSizeOperations();
                  }
                  else if(card.getController().equals(Constant.Player.Computer)) {
                     Computer_Cleanup.removeHandSizeOperation(Integer.parseInt(card.getSVar("HSStamp")));
                     Input_Cleanup.addHandSizeOperation(new HandSizeOp(Mode,Amount,Integer.parseInt(card.getSVar("HSStamp"))));
                     
                     Input_Cleanup.sortHandSizeOperations();
                  }
               }
            };
           
            card.addComesIntoPlayCommand(entersPlay);
            card.addLeavesPlayCommand(leavesPlay);
            card.addChangeControllerCommand(controllerChanges);
         } //HandSize
        
        if (card.getManaCost().contains("X"))
        {
        	SpellAbility sa = card.getSpellAbility()[0];
    		sa.setIsXCost(true);
    		
        	if (card.getManaCost().startsWith("X X"))
        		sa.setXManaCost("2");
        	else if (card.getManaCost().startsWith("X"))
        		sa.setXManaCost("1");
        }//X

        return postFactoryKeywords(card);
    }//getCard2
    
    public Card postFactoryKeywords(Card card){
		card.addColor(card.getManaCost());
    	int cardnameSpot = hasKeyword(card, "CARDNAME is ");
    	if (cardnameSpot != -1){
			String color = "1";
    		while(cardnameSpot != -1){
	            if(cardnameSpot != -1) {
	                String parse = card.getKeyword().get(cardnameSpot).toString();
	                card.removeIntrinsicKeyword(parse);
	                color += " " + Input_PayManaCostUtil.getShortColorString(parse.replace("CARDNAME is ","").replace(".", ""));
	                cardnameSpot = hasKeyword(card, "CARDNAME is ");
	            }
    		}
            card.addColor(color);
    	}

    	// this function should handle any keywords that need to be added after a spell goes through the factory
        if(hasKeyword(card, "Fading") != -1) {
            int n = hasKeyword(card, "Fading");
            if(n != -1) {
                String parse = card.getKeyword().get(n).toString();
                card.removeIntrinsicKeyword(parse);
                
                String k[] = parse.split(":");
                final int power = Integer.parseInt(k[1]);
                

                card.addComesIntoPlayCommand(CardFactoryUtil.fading(card, power));
                card.addSpellAbility(CardFactoryUtil.fading_desc(card, power));
            }
        }//Fading    	
    	
        if(hasKeyword(card, "Vanishing") != -1) {
            int n = hasKeyword(card, "Vanishing");
            if(n != -1) {
                String parse = card.getKeyword().get(n).toString();
                card.removeIntrinsicKeyword(parse);
                
                String k[] = parse.split(":");
                final int power = Integer.parseInt(k[1]);
                

                card.addComesIntoPlayCommand(CardFactoryUtil.vanishing(card, power));
                card.addSpellAbility(CardFactoryUtil.vanish_desc(card, power));
            }
        }//Vanishing
    	
        // Spell has Additional Cost. Reuse Ability_Cost
        if (hasKeyword(card, "ACost") != -1){
        	int n = hasKeyword(card, "ACost");
        	String parse = card.getKeyword().get(n).toString();
        	card.removeIntrinsicKeyword(parse);
        	
        	String k[] = parse.split(":");
        	
        	Ability_Cost cost = new Ability_Cost(k[1], card.getName(), false);
        	
        	StringBuilder sb = new StringBuilder(cost.toString());
        	sb.append("\n");
        	sb.append(card.getText());
        	card.setText(sb.toString());
        	
        	// loop through each of the cards spells and add abCost to it
        	ArrayList<SpellAbility> spells = card.getSpells();
        	for(SpellAbility sa : spells){
        		if (sa instanceof Spell){
        			sa.setPayCosts(cost);
        		}
        	}
        }
        return card;
    }
    
    // copies stats like attack, defense, etc..
    public static Card copyStats(Object o) {
        Card sim = (Card) o;
        Card c = new Card();
        
        c.setBaseAttack(sim.getBaseAttack());
        c.setBaseDefense(sim.getBaseDefense());
        c.setIntrinsicKeyword(sim.getKeyword());
        c.setName(sim.getName());
        c.setImageName(sim.getImageName());
        c.setType(sim.getType());
        c.setText(sim.getSpellText());
        c.setManaCost(sim.getManaCost());
        c.addColor(sim.getManaCost());
        c.setSVars(sim.getSVars());
        
        return c;
    }// copyStats()
    
    public static void main(String[] args) {
        CardFactory f = new CardFactory(ForgeProps.getFile(CARDSFOLDER));
        Card c = f.getCard("Arc-Slogger", "d");
        System.out.println(c.getOwner());
    }
}
