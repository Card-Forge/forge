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
        blankCard.setOwner(AllZone.HumanPlayer);
        blankCard.setController(AllZone.HumanPlayer);
        
        HumanNullCard.setOwner(AllZone.HumanPlayer);
        HumanNullCard.setController(AllZone.HumanPlayer);
        ComputerNullCard.setOwner(AllZone.ComputerPlayer);
        ComputerNullCard.setController(AllZone.ComputerPlayer);
        
        removedCardList = new HashSet<String>(FileUtil.readFile(ForgeProps.getFile(REMOVED)));
        

        try {
            readCards(file);
            
            // initialize CardList allCards
            Iterator<String> it = map.keySet().iterator();
            Card c;
            while(it.hasNext()) {
                c = getCard(it.next().toString(), AllZone.HumanPlayer);
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
        
        out.setSVars(in.getSVars());
        out.setSets(in.getSets());
        out.setCurSetCode(in.getCurSetCode());
        out.setImageFilename(in.getImageFilename());
        return out;
    	
    }
    
    final public Card copyCardintoNew(Card in) {
        
    	Card out = getCard(in.getName(), in.getOwner());
        PlayerZone Hplay = AllZone.getZone(Constant.Zone.Battlefield, AllZone.HumanPlayer);
        PlayerZone Cplay = AllZone.getZone(Constant.Zone.Battlefield, AllZone.ComputerPlayer);
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
				if(in.getSpellAbility()[i].getTargetPlayer().equals(AllZone.HumanPlayer)
						|| (in.getSpellAbility()[i].getTargetPlayer().equals(AllZone.ComputerPlayer))) 
					sa[i].setTargetPlayer(in.getSpellAbility()[i].getTargetPlayer());
				}
				if(Source.getController().equals(AllZone.HumanPlayer)) AllZone.GameAction.playSpellAbility(sa[i]);
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
    final public Card getCard(String cardName, Player owner) {
        if(removedCardList.contains(cardName) || cardName.equals(blankCard.getName())) return blankCard;
        
        return getCard2(cardName, owner);
    }
    
    final static int hasKeyword(Card c, String k) {
        ArrayList<String> a = c.getKeyword();
        for(int i = 0; i < a.size(); i++)
            if(a.get(i).toString().startsWith(k)) return i;
        
        return -1;
    }
    
    // Sol's Soulshift fix
    final static int hasKeyword(Card c, String k, int startPos) {
        ArrayList<String> a = c.getKeyword();
        for(int i = startPos; i < a.size(); i++)
            if(a.get(i).toString().startsWith(k)) return i;
        
        return -1;
    }
    
    private final int shouldManaAbility(Card c) {
        ArrayList<String> a = c.getIntrinsicKeyword();
        for(int i = 0; i < a.size(); i++)
            if(a.get(i).toString().contains(": add ") || a.get(i).toString().contains(": Add ") ) return i;
        return -1;
    }
    
    
    final private Card getCard2(final String cardName, final Player owner) {
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
        			PlayerZone pzICtrl = AllZone.getZone(Constant.Zone.Battlefield, card.getOwner());
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
        if(hasKeyword(card,"Sunburst") != -1)
        {	
        	Command sunburstCIP = new Command() {
				private static final long serialVersionUID = 1489845860231758299L;
				public void execute() {
					if(card.isType("Creature")) {
						card.addCounter(Counters.P1P1, card.getSunburstValue());
					}						
					else {
						card.addCounter(Counters.CHARGE, card.getSunburstValue());
					}
						
				}
        	};
        	
        	Command sunburstLP = new Command() {
				private static final long serialVersionUID = -7564420917490677427L;
				public void execute() {
        			card.setSunburstValue(0);
        		}
        	};
        		
        	card.addComesIntoPlayCommand(sunburstCIP);
        	card.addLeavesPlayCommand(sunburstLP);
        }
        //converted all to AbilityFactory
        /*
        if(hasKeyword(card,"spCounter") != -1) {
    		ComputerAI_counterSpells2.KeywordedCounterspells.add(card.getName());

    		String keyword = card.getKeyword().get(hasKeyword(card,"spCounter"));
    		card.removeIntrinsicKeyword(keyword);
    		String[] splitkeyword = keyword.split(":");
            	
            	final String targetType = splitkeyword[1];
            	final String targetingRestrictions = splitkeyword[2];
            	final String destination = splitkeyword[3];
            	final String extraActions = splitkeyword[4];
            	
            	final String[] splitTargetingRestrictions = targetingRestrictions.split(" ");
            	final String[] splitExtraActions = extraActions.split(" ");

    		final SpellAbility[] tgt = new SpellAbility[1];
    		final SpellAbility spell = new Spell(card) {

				private static final long serialVersionUID = 229983950093253062L;

				@Override
    			public void resolve() {
    				if(CardFactoryUtil.spCounter_MatchSpellAbility(card,tgt[0],splitTargetingRestrictions,targetType) 
    						&& AllZone.Stack.contains(tgt[0])
    						&& !tgt[0].getSourceCard().keywordsContain("CARDNAME can't be countered."))
    				{
    					SpellAbility sa = tgt[0];
    					AllZone.Stack.remove(tgt[0]);
    					
    					System.out.println("Send countered spell to " + destination);
            			
            				if(destination.equals("None") || targetType.contains("Ability")) //For Ability-targeting counterspells
            				{
            				
            				}
            				else if(destination.equals("Graveyard"))
            				{
            					AllZone.GameAction.moveToGraveyard(sa.getSourceCard());
            				}
            				else if(destination.equals("Exile"))
            				{
            					AllZone.GameAction.exile(sa.getSourceCard());
            				}
            				else if(destination.equals("Topdeck"))
            				{
            					AllZone.GameAction.moveToTopOfLibrary(sa.getSourceCard());
            				}
            				else if(destination.equals("Hand"))
            				{
            					AllZone.GameAction.moveToHand(sa.getSourceCard());
            				}
            				else if(destination.equals("BottomDeck"))
            				{
            					AllZone.GameAction.moveToBottomOfLibrary(sa.getSourceCard());
            				}
            				else if(destination.equals("Shuffle"))
            				{
            					AllZone.GameAction.moveToBottomOfLibrary(sa.getSourceCard());
            					sa.getSourceCard().getController().shuffle();
            				}
            				else
            				{
            					throw new IllegalArgumentException("spCounter: Invalid Destination argument for card " + card.getName());
            				}
            				
            				for(int ea = 0;ea<splitExtraActions.length;ea++)
            				{
            					boolean isOptional = false;

            					if(splitExtraActions[0].equals("None"))
            					{
            						break;
            					}
            					String ActionID = splitExtraActions[ea].substring(0,splitExtraActions[ea].indexOf('('));
            					
            					Player Target = null;
            				
            					String ActionParams = splitExtraActions[ea].substring(splitExtraActions[ea].indexOf('(')+1);
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
            						Target = card.getController().getOpponent();
            					}
            					else if(ActionID.startsWith("CC-"))
            					{
            						ActionID = ActionID.substring(3);
            						Target = sa.getSourceCard().getController();
            					}

            					if(ActionID.startsWith("May-"))
            					{
            						ActionID = ActionID.substring(4);
            						isOptional = true;
            					}
            				
            					if(ActionID.equals("Draw"))
            					{
    							if(isOptional)
    							{
    								if(Target == AllZone.HumanPlayer)
    								{
    									if(AllZone.Display.getChoice("Do you want to draw" + SplitActionParams[0] + "card(s)?","Yes","No").equals("Yes"))
    									{
    			        						Target.drawCards(Integer.parseInt(SplitActionParams[0]));
    									}
    								}
    								else
    								{//AI decision-making, only draws a card if it doesn't risk discarding it.
    									if(AllZone.getZone(Constant.Zone.Hand,AllZone.ComputerPlayer).getCards().length + Integer.parseInt(SplitActionParams[0]) < 6)
    									{
    			        						Target.drawCards(Integer.parseInt(SplitActionParams[0]));
    									}
    								}
    							}
    							else
    							{
    	        						Target.drawCards(Integer.parseInt(SplitActionParams[0]));
    							}

            					}
            					else if(ActionID.equals("Discard"))
            					{
            						if(isOptional)
            						{
            							if(Target == AllZone.HumanPlayer)
            							{
            								if(AllZone.Display.getChoice("Do you want to discard" + SplitActionParams[0] + "card(s)?","Yes","No").equals("Yes"))
            								{
            									Target.discard(Integer.parseInt(SplitActionParams[0]),this, false);
            								}
            							}
            							else
            							{//AI decisionmaking. Should take Madness cards and the like into account in the future.Right now always refuses to discard.
            							}
            						}
            						else
            						{
    	        						Target.discard(Integer.parseInt(SplitActionParams[0]), this, false);
            						}
            					}
            					else if(ActionID.equals("LoseLife"))
            					{
            						if(isOptional)
            						{
            							if(Target == AllZone.HumanPlayer)
            							{
            								if(AllZone.Display.getChoice("Do you want to lose" + SplitActionParams[0] + "life?","Yes","No").equals("Yes"))
            								{
    			        							Target.loseLife(Integer.parseInt(SplitActionParams[0]), card);
            								}
            							}
            							else
            							{//AI decisionmaking. Not sure why one would ever want to agree to this, except for the rare case of Near-Death Experience+Ali Baba.
            							}
            						}
            						else
            						{
    	        						Target.loseLife(Integer.parseInt(SplitActionParams[0]), card);
            						}
            						
            					}
            					else if(ActionID.equals("GainLife"))
            					{
            						if(isOptional)
    								{
    									if(Target == AllZone.HumanPlayer)
    									{
    										if(AllZone.Display.getChoice("Do you want to gain" + SplitActionParams[0] + "life?","Yes","No").equals("Yes"))
    										{
    			        						Target.gainLife(Integer.parseInt(SplitActionParams[0]), card);
    										}
    									}
    									else
    									{//AI decisionmaking. Not sure why one would ever want to decline this, except for the rare case of Near-Death Experience.
    		        						Target.gainLife(Integer.parseInt(SplitActionParams[0]), card);
    									}
    								}
    								else
    								{
    	        						Target.gainLife(Integer.parseInt(SplitActionParams[0]), card);
    								}
            					}
						else if(ActionID.equals("RevealHand"))
						{
    							if(isOptional)
    							{
    								System.out.println(Target);
    								if(Target == AllZone.HumanPlayer)
    								{
    									if(AllZone.Display.getChoice("Do you want to reveal your hand?","Yes","No").equals("Yes"))
    									{//Does nothing now, of course, but sometime in the future the AI may be able to remember cards revealed and prioritize discard spells accordingly.

    									}
    								}
    								else
    								{//AI decisionmaking. Not sure why one would ever want to agree to this

    								}
    							}
    							else
    							{
    								System.out.println(Target);
    	        						if(Target == AllZone.HumanPlayer)
    	        						{//Does nothing now, of course, but sometime in the future the AI may be able to remember cards revealed and prioritize discard spells accordingly.

    	        						}
    	        						else
    	        						{
    	        							CardList list = new CardList(AllZone.getZone(Constant.Zone.Hand,AllZone.ComputerPlayer).getCards());
    	        							AllZone.Display.getChoiceOptional("Revealed cards",list.toArray());
    	        						}
    							}
						}
						else if(ActionID.equals("RearrangeTopOfLibrary")) //A'la Aven Fateshaper
						{
							if(isOptional)
    							{
    								if(Target == AllZone.HumanPlayer)
    								{
    									if(AllZone.Display.getChoice("Do you want to rearrange the top " + SplitActionParams[0] + " cards of your library?","Yes","No").equals("Yes"))
    									{
										AllZoneUtil.rearrangeTopOfLibrary(Target, Integer.parseInt(SplitActionParams[0]), false);
    									}
    								}
    								else
    								{//AI decisionmaking. AI simply can't atm, and wouldn't know how best to do it anyway.

    								}
    							}
    							else
    							{
    	        						if(Target == AllZone.HumanPlayer)
								{
									AllZoneUtil.rearrangeTopOfLibrary(Target, Integer.parseInt(SplitActionParams[0]), false);
								}
								else
								{
									CardList list = new CardList(AllZone.getZone(Constant.Zone.Hand,AllZone.ComputerPlayer).getCards());
									AllZone.Display.getChoiceOptional("Revealed cards",list.toArray());
								}
    							}
						}
            					else
            					{
            						throw new IllegalArgumentException("spCounter: Invalid Extra Action for card " + card.getName());
            					}
            				}
    				}
    			
    			}//resolve()
                    
    			public boolean canPlay()
    			{
    				ArrayList<SpellAbility> choosables = new ArrayList<SpellAbility>();

    				for(int i=0;i<AllZone.Stack.size();i++)
    				{
    					choosables.add(AllZone.Stack.peek(i));
    				}

    				for(int i=0;i<choosables.size();i++)
    				{
    					if(!CardFactoryUtil.spCounter_MatchSpellAbility(card,choosables.get(i),splitTargetingRestrictions,targetType))
    					{
    						choosables.remove(i);
    					}
    				}

    				return choosables.size() > 0 && super.canPlay();
    			}//canPlay()
    			
    			public boolean canPlayAI()
    			{
    				if(AllZone.Stack.size() < 1) {
    					return false;
    				}
    				if(CardFactoryUtil.spCounter_MatchSpellAbility(card, AllZone.Stack.peek(),splitTargetingRestrictions, targetType)) {
    					tgt[0] = AllZone.Stack.peek();
    					return  super.canPlayAI();
    				}
    				return false;
    			}
    		};//SpellAbility
    		
    		Input runtime = new Input() {
                    
				private static final long serialVersionUID = 5360660530175041997L;

				@Override
    			public void showMessage() {
    				ArrayList<SpellAbility> choosables = new ArrayList<SpellAbility>();

    				for(int i=0;i<AllZone.Stack.size();i++)
    				{
    					choosables.add(AllZone.Stack.peek(i));
    				}

    				for(int i=0;i<choosables.size();i++)
    				{
    					if(!CardFactoryUtil.spCounter_MatchSpellAbility(card,choosables.get(i),splitTargetingRestrictions,targetType) || choosables.get(i).getSourceCard().equals(card))
    					{
    						choosables.remove(i);
    					}
    				}
    				HashMap<String,SpellAbility> map = new HashMap<String,SpellAbility>();

    				for(SpellAbility sa : choosables)
    				{
    					map.put(sa.getStackDescription(),sa);
    				}

    				String[] choices = new String[map.keySet().size()];
    				choices = map.keySet().toArray(choices);

    				String madeChoice = AllZone.Display.getChoice("Select target spell.",choices);

    				tgt[0] = map.get(madeChoice);
    				System.out.println(tgt[0]);
                    stopSetNext(new Input_PayManaCost(spell));
    			}//showMessage()
    		};//Input
    		card.clearSpellAbility();
    		card.addSpellAbility(spell);
    		spell.setBeforePayMana(runtime);
    	}//spCounter
    	*/
    	
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
                    CardList CardsinPlay = AllZoneUtil.getTypeInPlay("World");
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
                    	if(getSourceCard().getController().equals(AllZone.ComputerPlayer))
                    		setTargetCard(card);//CardFactoryUtil.getRandomCard(new CardList(AllZone.Computer_Play.getCards()).getType("Land")));
                        Card c = getTargetCard();
                        PlayerZone hand = AllZone.getZone(Constant.Zone.Hand, c.getOwner());
                        
                        if(AllZone.GameAction.isCardInPlay(c)) {
                            AllZone.getZone(c).remove(c);
                            
                            if(!c.isToken()) {
                                Card newCard = AllZone.CardFactory.getCard(c.getName(), c.getOwner());
                                
                                newCard.setCurSetCode(c.getCurSetCode());
                                newCard.setImageFilename(c.getImageFilename());
                                
                                hand.add(newCard);
                            }
                        }
                    }
                };
                Command intoPlay = new Command() {
                    private static final long serialVersionUID = 2045940121508110423L;
                    
                    public void execute() {
                        PlayerZone play = AllZone.getZone(Constant.Zone.Battlefield, card.getController());
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
        
        
        /*
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
                                        AllZone.ComputerPlayer).getCards());
                                
                                if(HandList.size() >= 4) weight[1] = 25;
                                else weight[1] = 75;
                                
                                // compare the highest converted mana cost of cards in hand to the number of lands
                                // if there's spare mana, then regeneration might be viable
                                int hCMC = 0;
                                for(int i = 0; i < HandList.size(); i++)
                                    if(CardUtil.getConvertedManaCost(HandList.getCard(i).getManaCost()) > hCMC) hCMC = CardUtil.getConvertedManaCost(HandList.getCard(
                                            i).getManaCost());
                                
                                CardList LandList = new CardList(AllZone.getZone(Constant.Zone.Play,
                                        AllZone.ComputerPlayer).getCards());
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
        */

        
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
                		l.addAll(AllZone.getZone(Constant.Zone.Battlefield, card.getController()).getCards());
                	
                	if (Scope[0].contains("All")) {
                		l.addAll(AllZone.getZone(Constant.Zone.Battlefield, AllZone.HumanPlayer).getCards());
                		l.addAll(AllZone.getZone(Constant.Zone.Battlefield, AllZone.ComputerPlayer).getCards());
                	}
                	
                	String fc[] = {"Creature"};
                	l = l.getValidCards(fc,card.getController(),card);
                	
                	if (Scope.length > 1)
                	{
                		String v = Scope[1]; 
                		if (v.length() > 0)
                			l = l.getValidCards(v.split(","),card.getController(),card);
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
                					return super.canPlayAI();
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
                		CardFactoryUtil.doDrawBack(DrawBack[0], 0, card.getController(), card.getController().getOpponent(), card.getController(), card, card, this);
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
            	
            	SpellAbility abAllPump = new Ability_Activated(card, abCost, null)
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
            				l.addAll(AllZone.getZone(Constant.Zone.Battlefield, card.getController()).getCards());
            			
            			if (Scope[0].contains("All")) {
            				l.addAll(AllZone.getZone(Constant.Zone.Battlefield, AllZone.HumanPlayer).getCards());
            				l.addAll(AllZone.getZone(Constant.Zone.Battlefield, AllZone.ComputerPlayer).getCards());
            			}
            			
            			String fc[] = {"Creature"};
            			l = l.getValidCards(fc,card.getController(),card);
            			
            			if (Scope.length > 1)
            			{
            				String v = Scope[1]; 
            				if (v.length() > 0)
            					l = l.getValidCards(v.split(","),card.getController(),card);
            			}
            			
            			return l;
            		}

                    @Override
                    public boolean canPlay() {
                    	if (abCost.getTap() && (card.isTapped() || card.isSick()))
                    		return false;
                    	
                        return super.canPlay();
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
            							return super.canPlayAI();
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
            				CardFactoryUtil.doDrawBack(DrawBack[0], 0, card.getController(), card.getController().getOpponent(), card.getController(), card, card, this);
            		} // resolve
            	}; // abAllPump

            	abAllPump.setDescription(abCost.toString() + spDesc[0]);
            	abAllPump.setStackDescription(stDesc[0]);

                card.addSpellAbility(abAllPump);
        	}
        }
        
        /* Cards converted to AB$Pump
        while(hasKeyword(card, "abPump") != -1) {
            int n = hasKeyword(card, "abPump");
            if(n != -1) {
                String parse = card.getKeyword().get(n).toString();
                card.removeIntrinsicKeyword(parse);
                
                String k[] = parse.split(":");
        		String tmp =  k[0].replace("abPump", "");
        		
        		String[] tmpCost = tmp.split(" ", 2);
        		
        		final boolean bPumpEquipped = (tmpCost[0].equals("Equipped"));
        		
        		final Target abTgt;
        		
        		if (tmpCost[0].equals("TgtC"))
        			abTgt = new Target(tmpCost[0]);
        		else
        			abTgt = null;
        		   			
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
                
                if(abTgt != null && abTgt.doesTarget()) 
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
                    		if (count.equals(Counters.P1P1)){	// 10% chance to remove +1/+1 to pump
                    			chance = .1;
                    		}
                    		else if (count.equals(Counters.CHARGE)){ // 50% chance to remove +1/+1 to pump
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
                        
                        if(abTgt == null) {
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
                        return super.canPlay();
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
                        if(abTgt != null && abTgt.doesTarget()) 
                        	creature[0] = getTargetCard();
                        else if (bPumpEquipped)
                        	creature[0] = card.getEquippingCard();
                        else 
                        	creature[0] = card;
                    	
                        if(creature[0] != null && AllZone.GameAction.isCardInPlay(creature[0])
                                && (abTgt == null || CardFactoryUtil.canTarget(card, getTargetCard()))) {

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
                                    card.getController().getOpponent(), null, card,
                                    creature[0], this);
                            
                        }//if (card is in play)
                    }//resolve()
                };//SpellAbility

                ability.setDescription(spDesc[0]);
                ability.setStackDescription(stDesc[0]);

                card.addSpellAbility(ability);
            }
        }//while
        */
        

        /* Converted cards to AF_Untap
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
        */
        /*
        if(hasKeyword(card, "Remove three spore counters") != -1) {
            int n = hasKeyword(card, "Remove three spore counters");
            if(n != -1) {
                String parse = card.getKeyword().get(n).toString();
                card.removeIntrinsicKeyword(parse);
                
                card.addSpellAbility(CardFactoryUtil.ability_Spore_Saproling(card));
            }
        }//Spore Saproling
        */
        
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
                        PlayerZone compHand = AllZone.getZone(Constant.Zone.Hand, AllZone.ComputerPlayer);
                        CardList hand = new CardList(compHand.getCards());
                        
                        if(hand.size() >= 7) // anti-discard-at-EOT
                        	return true;
                        
                        if(AllZone.HumanPlayer.getLife() < (10 + damage)) // if damage from this spell would drop the human to less than 10 life
                        	return true;
                        
                        return false;
                    }
                    
                    Card chooseTgtC() {
                        // Combo alert!!
                        PlayerZone compy = AllZone.getZone(Constant.Zone.Battlefield, AllZone.ComputerPlayer);
                        CardList cPlay = new CardList(compy.getCards());
                        if(cPlay.size() > 0) for(int i = 0; i < cPlay.size(); i++)
                            if(cPlay.get(i).getName().equals("Stuffy Doll")) return cPlay.get(i);
                        
                        PlayerZone human = AllZone.getZone(Constant.Zone.Battlefield, AllZone.HumanPlayer);
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
                                setTargetPlayer(AllZone.HumanPlayer);
                                return super.canPlayAI();
                            }
                            
                            Card c = chooseTgtC();
                            if(c != null) {
                                setTargetCard(c);
                                return super.canPlayAI();
                            }
                        }
                        
                        if(TgtPlayer[0] == true || TgtOpp[0] == true) {
                            setTargetPlayer(AllZone.HumanPlayer);
                            return super.canPlayAI();
                        }
                        
                        if(TgtCreature[0] == true) {
                            Card c = chooseTgtC();
                            if(c != null) {
                                setTargetCard(c);
                                return super.canPlayAI();
                            }
                        }
                        
                        return false;
                    }
                    
                    @Override
                    public void resolve() {
                        damage = getNumDamage();
                        if (usesXCost[0])
                        	damage = getNumXDamage();
                        Player tgtP = null;
                        
                        if(TgtOpp[0] == true) setTargetPlayer(card.getController().getOpponent());
                        
                        if(getTargetCard() != null) {
                            if(AllZone.GameAction.isCardInPlay(getTargetCard())
                                    && CardFactoryUtil.canTarget(card, getTargetCard())) {
                                Card c = getTargetCard();
                                c.addDamage(damage, card);
                                tgtP = c.getController();
                                
                                if(!DrawBack[0].equals("none")) 
                                	CardFactoryUtil.doDrawBack(DrawBack[0], damage,
                                        card.getController(), card.getController().getOpponent(), tgtP,
                                        card, getTargetCard(), this);
                            }
                        } else {
                        	tgtP = getTargetPlayer();
                            tgtP.addDamage(damage, card);
                            
                            if(!DrawBack[0].equals("none")) CardFactoryUtil.doDrawBack(DrawBack[0], damage,
                                    card.getController(), card.getController().getOpponent(), tgtP,
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
                	   	if(!super.canPlayAI()) return false;
                	   
                	   	int ndam = getNumDam();
                	   	
                    	if (DmgPlayer[0] && AllZone.ComputerPlayer.getLife() <= ndam) 
                    		return false;       									// The AI will not kill itself
                    	if (DmgPlayer[0] && AllZone.HumanPlayer.getLife() <= ndam && AllZone.ComputerPlayer.getLife() > ndam) 
                    		return true;											// The AI will kill the human if possible
                    	
                	   	CardList human = new CardList(AllZone.Human_Battlefield.getCards());
                	   	CardList computer = new CardList(AllZone.Computer_Battlefield.getCards());
                    
                    	human = human.getValidCards(Tgts,card.getController(),card);
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
                        if (!DmgPlayer[0] && AllZone.ComputerPlayer.getLife() < 7) humanvalue += CardListUtil.sumAttack(human); 
                        // in Low Life Emergency (and not hurting itself) X = X + total power of human creatures
                        
                    	computer = computer.getValidCards(Tgts,card.getController(),card);
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
                    	all.addAll(AllZone.Human_Battlefield.getCards());
                    	all.addAll(AllZone.Computer_Battlefield.getCards());
                    	all = all.getValidCards(Tgts,card.getController(),card);
                    
                    	for(int i = 0; i < all.size(); i++) {
                        	if(CardFactoryUtil.canDamage(card, all.get(i))) all.get(i).addDamage(ndam, card);
                    	}
                    	if (DmgPlayer[0] == true) {
                    		AllZone.ComputerPlayer.addDamage(ndam, card);
                    		AllZone.HumanPlayer.addDamage(ndam, card);
                    	}
                       // if (!DrawBack[0].equals("none"))
                       //    CardFactoryUtil.doDrawBack(DrawBack[0], ndam, card.getController(), card.getController().getOpponent(), null, card, null);
                    	
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
        
        /* Cards converted to AF_AB$Damage
        while(hasKeyword(card, "abDamage") != -1) {
            int n = hasKeyword(card, "abDamage");
            if(n != -1) {
                String parse = card.getKeyword().get(n).toString();
                card.removeIntrinsicKeyword(parse);
                
        		String k[] = parse.split(":");
        		String tmpCost[] =  k[0].replace("abDamage", "").split(" ", 2);
        		
        		int drawBack = 2;
        		
        		// TODO: These can do for some converting for an improved message box
        		final Target abTgt;
        		if (tmpCost[0].contains("TgtV")){
        			int valid = drawBack;
        			abTgt = new Target("Select a target: " + k[valid], k[valid].split(","));
        			drawBack++;
        		}
        		else
        			abTgt = new Target(tmpCost[0]);
        		
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
                    
                    sb.append(abTgt.getVTSelection());
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
                        PlayerZone compHand = AllZone.getZone(Constant.Zone.Hand, AllZone.ComputerPlayer);
                        CardList hand = new CardList(compHand.getCards());
                        
                        if(hand.size() > 7) // anti-discard-at-EOT
                        	return true;
                        
                        if(AllZone.HumanPlayer.getLife() - damage < 10) // if damage from this spell would drop the human to less than 10 life
                        	return true;
                        
                        return false;
                    }
                    
                    Card chooseTgtC() {
                        // Combo alert!!
                        PlayerZone compy = AllZone.getZone(Constant.Zone.Play, AllZone.ComputerPlayer);
                        CardList cPlay = new CardList(compy.getCards());
                        if(cPlay.size() > 0) for(int i = 0; i < cPlay.size(); i++)
                            if(cPlay.get(i).getName().equals("Stuffy Doll")) return cPlay.get(i);
                        
                        PlayerZone human = AllZone.getZone(Constant.Zone.Play, AllZone.HumanPlayer);
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
                        return super.canPlay();
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
                        
                        if(abTgt.canTgtCreatureAndPlayer()) {
                            if(shouldTgtP()) {
                                setTargetPlayer(AllZone.HumanPlayer);
                                return rr;
                            }
                            
                            Card c = chooseTgtC();
                            if(c != null) {
                                setTargetCard(c);
                                return rr;
                            }
                        }
                        
                        // if(abTgt.canTgtPlayer() || TgtOpp[0] == true) {
                        if(abTgt.canTgtPlayer()) {
                            setTargetPlayer(AllZone.HumanPlayer);
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
                        Player tgtP = null;
                        
                        //if(TgtOpp[0] == true) {
                        //    tgtP = card.getController().getOpponent();
                        //    setTargetPlayer(tgtP);
                        //}
                        Card c = getTargetCard();
                        if(c != null) {
                            if(AllZone.GameAction.isCardInPlay(c) && CardFactoryUtil.canTarget(card, c)) {
                                c.addDamage(damage, card);
                                tgtP = c.getController();
                                
                                if(!DrawBack[0].equals("none")) CardFactoryUtil.doDrawBack(DrawBack[0], damage,
                                        card.getController(), card.getController().getOpponent(),
                                        tgtP, card, c, this);

                            }
                        } else {
                            tgtP = getTargetPlayer();
                            tgtP.addDamage(damage, card);
                            
                            if(!DrawBack[0].equals("none")) CardFactoryUtil.doDrawBack(DrawBack[0], damage,
                                    card.getController(), card.getController().getOpponent(),
                                    tgtP, card, null, this);

                        }
                        
                    }//resolve()
                };//Ability_Activated
                
                abDamage.setDescription(spDesc[0]);
                abDamage.setStackDescription(stDesc[0]);
                
                card.addSpellAbility(abDamage);
            }
        }//abDamageTgt
        */
        
        // Generic destroy target card
        /* Converted cards to AF_SP$Destroy
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
                    tmpList = tmpList.getValidCards(Tgts,card.getController(),card);
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
                    		CardFactoryUtil.doDrawBack(Drawback[0], 0, card.getController(), card.getController().getOpponent(), tgtC.getController(), card, tgtC, this);
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
        */
        
        
        // Generic destroy target ___ activated ability
        /* Converted cards to AF_AB$Destroy
        if (hasKeyword(card, "abDestroyTgtV") != -1)
        {
        	int n = hasKeyword(card, "abDestroyTgtV");
        	
        	String parse = card.getKeyword().get(n).toString();
        	card.removeIntrinsicKeyword(parse);
        	
        	String k[] = parse.split(":");
        	
        	String tmpCost = k[0].substring(13);
        	final Ability_Cost abCost = new Ability_Cost(tmpCost, card.getName(), true);
        	
        	final String Tgts[] = k[1].split(",");
        	
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
        	String Selec = "Select target " + tmpDesc + " to destroy.";
        	
        	final Target tgtDstryTgt = new Target(Selec, Tgts);
        	
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
                    tmpList = tmpList.getValidCards(Tgts,card.getController(),card);
                    tmpList = tmpList.getTargetableCards(card);
                    
                    return tmpList;
                }
                
                @Override
                public boolean canPlay(){
                    return super.canPlay();
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
                    		CardFactoryUtil.doDrawBack(Drawback[0], 0, card.getController(), card.getController().getOpponent(), tgtC.getController(), card, tgtC, this);
                    }
                }

        	}; //AbDstryTgt
        	
        	AbDstryTgt.setDescription(spDesc[0]);
        	
        	card.addSpellAbility(AbDstryTgt);
        	card.setSVar("PlayMain1", "TRUE");
        }// abDestroyTgt
        */

        
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
                	                    
                    CardList hCards = new CardList(AllZone.getZone(Constant.Zone.Battlefield, AllZone.HumanPlayer).getCards());
                    hCards = hCards.getValidCards(Tgts,card.getController(),card);
                    hCards = hCards.getTargetableCards(card);
                    if (hCards.size() > 0)
                    	return super.canPlayAI();
                    
                    CardList cCards = new CardList(AllZone.getZone(Constant.Zone.Battlefield, AllZone.ComputerPlayer).getCards());
                    cCards = cCards.getValidCards(Tgts,card.getController(),card);
                    cCards = cCards.getTargetableCards(card);
                    if (cCards.size() == 0)
                    	return super.canPlayAI();
                    else
                    {
                    	if (r.nextInt(100) > 67)
                    		return super.canPlayAI();
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
                        	CardFactoryUtil.doDrawBack(Drawback[0], 0, card.getController(), card.getController().getOpponent(), c.getController(), card, c, this);
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
                    CardList hCards = new CardList(AllZone.Human_Battlefield.getCards());
                    CardList cCards = new CardList(AllZone.Computer_Battlefield.getCards());
                    
                    hCards = hCards.getValidCards(Tgts,card.getController(),card);
                    hCards = hCards.getTargetableCards(card);
                    cCards = cCards.getValidCards(Tgts,card.getController(),card);
                    cCards = cCards.getTargetableCards(card);
                    
                    if(hCards.size() > 0 || cCards.size() > 0) 
                    {
                        if (card.getController().equals(AllZone.HumanPlayer))
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
        /* Cards converted to AF_DestroyAll
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
                    
                    human = human.getValidCards(Tgts,card.getController(),card);
                    human = human.getNotKeyword("Indestructible");
                    int humanvalue = CardListUtil.sumCMC(human);
                    humanvalue += human.size();
                    humanvalue += CardListUtil.sumAttack(human.getTokens()); 
                    humanvalue += human.getType("Land").size();        // X = total converted mana cost + number of permanents + number of lands + total power of tokens (Human)
                    if (AllZone.ComputerPlayer.getLife() < 7) { humanvalue += CardListUtil.sumAttack(human); } // in Low Life Emergency X = X + total power of human creatures

                    computer = computer.getValidCards(Tgts,card.getController(),card);
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
                    all = all.getValidCards(Tgts,card.getController(),card);
                    
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
	                    afterAll = afterAll.getValidCards(Tgts,card.getController(),card);
	                    
	                    ArrayList<Integer> slD = new ArrayList<Integer>();
	                    for (int i=0; i<afterAll.size(); i++)
	                    	slD.add(afterAll.get(i).getUniqueNumber());
	                    
	                    for (int i=0; i<all.size(); i++)
	                    {
	                    	if (!slD.contains(all.get(i).getUniqueNumber()))
	                    		nDestroyed++;
	                    }
	                    Log.error("nDestroyed: " + nDestroyed);
	                    CardFactoryUtil.doDrawBack(Drawback[0], nDestroyed, card.getController(), card.getController().getOpponent(), null, card, null, this);
                    }
                }// resolve()

            }; //SpDstryAll
            
            spDstryAll.setDescription(card.getSpellText());
            card.setText("");
            
            card.addSpellAbility(spDstryAll);            

        }//spDestroyAll
        */

/*
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
                    			tgtC.getOwner().shuffle();
                    		}
                    		else if(Destination.equals("Exile"))
                    			AllZone.GameAction.exile(tgtC); 
                    		else if(Destination.equals("Hand"))
                    		{
                        		PlayerZone hand = AllZone.getZone(Constant.Zone.Hand, tgtC.getOwner());
                       	 		AllZone.GameAction.moveTo(hand, tgtC);
                    		}
                    	}
                    	
                    	if (!Drawback[0].equals("none"))
                    		CardFactoryUtil.doDrawBack(Drawback[0], 0, card.getController(), card.getController().getOpponent(), tgtC.getController(), card, tgtC, this);
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
*/
        
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
                	                    
                    CardList hCards = new CardList(AllZone.getZone(Constant.Zone.Battlefield, AllZone.HumanPlayer).getCards());
                    hCards = hCards.getValidCards(Tgts,card.getController(),card);
                    hCards = hCards.getTargetableCards(card);
                    if (hCards.size() > 0)
                    	return super.canPlayAI();
                    
                    CardList cCards = new CardList(AllZone.getZone(Constant.Zone.Battlefield, AllZone.ComputerPlayer).getCards());
                    cCards = cCards.getValidCards(Tgts,card.getController(),card);
                    cCards = cCards.getTargetableCards(card);
                    if (cCards.size() == 0)
                    	return super.canPlayAI();
                    else
                    {
                    	if (r.nextInt(100) > 67)
                    		return super.canPlayAI();
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
        						tgtC.getOwner().shuffle();
        					}
        					else if (Destination.equals("Exile"))
        						AllZone.GameAction.exile(tgtC);
        					else if (Destination.equals("Hand"))
        						AllZone.GameAction.moveToHand(tgtC);
        				}
        				
        				if (!Drawback[0].equals("none"))
        					CardFactoryUtil.doDrawBack(Drawback[0], 0, card.getController(), card.getController().getOpponent(), tgtC.getController(), card, tgtC, this);
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
        			CardList hCards = new CardList(AllZone.Human_Battlefield.getCards());
        			CardList cCards = new CardList(AllZone.Computer_Battlefield.getCards());
        			
        			hCards = hCards.getValidCards(Tgts,card.getController(),card);
        			hCards = hCards.getTargetableCards(card);
        			cCards = cCards.getValidCards(Tgts,card.getController(),card);
        			cCards = cCards.getTargetableCards(card);
        			
        			if (hCards.size() > 0 || cCards.size() > 0)
        			{
        				if (card.getController().equals(AllZone.HumanPlayer))
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
                	
                	if(!super.canPlayAI()) return false;
                	
                    CardList human = new CardList(AllZone.Human_Battlefield.getCards());
                    CardList computer = new CardList(AllZone.Computer_Battlefield.getCards());
                    
                    human = human.getValidCards(Tgts,card.getController(),card);
                    int humanvalue = CardListUtil.sumCMC(human);
                    humanvalue += human.getType("Land").size();
                    humanvalue += CardListUtil.sumAttack(human.getTokens());        // X = total converted mana cost + number of lands c (Human)
                    if(!Destination.equals("Hand")) humanvalue += human.size();     // if the Destination is not Hand card advantage counts
                    if(Destination.equals("Hand")) humanvalue += CardListUtil.sumDefense(human.getTokens());  // if the Destination is Hand tokens are more important
                    if (AllZone.ComputerPlayer.getLife() < 7) { humanvalue += CardListUtil.sumAttack(human); } // in Low Life Emergency X = X + total power of human creatures

                    computer = computer.getValidCards(Tgts,card.getController(),card);
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
                    all.addAll(AllZone.Human_Battlefield.getCards());
                    all.addAll(AllZone.Computer_Battlefield.getCards());
                    all = all.getValidCards(Tgts,card.getController(),card);
                    
                    for(int i = 0; i < all.size(); i++) {
                        Card c = all.get(i);
                        if(c.isToken()) AllZone.getZone(c).remove(c);
                        else {  
					if(Destination.equals("TopofLibrary")) AllZone.GameAction.moveToTopOfLibrary(c);
					else if(Destination.equals("BottomofLibrary")) AllZone.GameAction.moveToBottomOfLibrary(c);
					else if(Destination.equals("ShuffleIntoLibrary")) {
							AllZone.GameAction.moveToTopOfLibrary(c);
							c.getOwner().shuffle();
						}
					else if(Destination.equals("Exile")) AllZone.GameAction.exile(c); 
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
        
        
        /*
         *  Generic return target card(s) from graveyard to Hand, Battlefield or Top of Library
         *  spReturnTgt:{Num Cards/Parameters}:{Type}:{To Zone}:{DrawBack}:{Spell Desc}
         *  
         *  X Count/Costs are not yet implemented.
         */
        /*
        if (hasKeyword(card, "spReturnTgt") != -1) {
            int n = hasKeyword(card, "spReturnTgt");
            
            String parse = card.getKeyword().get(n).toString();
            card.removeIntrinsicKeyword(parse);
            String k[] = parse.split(":");
            final boolean returnUpTo[] = {false};
            final boolean anyNumber[] = {false};
            final int numCardsToReturn;
            
            String np[] = k[1].split("/");
            
            if (np[0].equals("AnyNumber")) {
                anyNumber[0] = true;
                numCardsToReturn = 0;
            } else {
                numCardsToReturn = Integer.parseInt(np[0]);
            }
            
            if (np.length > 1) {
                if (np[1].equals("UpTo")) {
                    returnUpTo[0] = true;
                }
            }
            
            //  Artifact, Creature, Enchantment, Land, Permanent, Instant, Sorcery, Card
            //  White, Blue, Black, Red, Green, Colorless, MultiColor
            //  non-Artifact, non-Creature, non-Enchantment, non-Land, non-Permanent,
            //  non-White, non-Blue, non-Black, non-Red, non-Green, non-Colorless, non-MultiColor
            
            String Targets = k[2];
            final String Tgts[] = Targets.split(",");
            
            final String Destination = k[3];
            
            String desc = "";
            final String Drawback[] = {"none"};
            
            if (k.length > 4) {
                
                if (k[4].contains("Drawback$")){
                    String kk[] = k[4].split("\\$");
                    Drawback[0] = kk[1];
                } else {
                    desc = k[4];
                }
            }
            if (k.length > 5) {
                desc = k[5];
            }
            
            final SpellAbility spRtrnTgt = new Spell(card) {
                private static final long serialVersionUID = 7970018872459137897L;
                
                @Override
                public boolean canPlay() {
                    if (returnUpTo[0] || anyNumber[0]) return true;
                    return getGraveyardList().size() >= numCardsToReturn && super.canPlay();
                }
                
                @Override
                public boolean canPlayAI() {
                    if (AllZone.Phase.getTurn() <= 3) return false;
                    
                    CardList results = new CardList();
                    CardList choices = getGraveyardList();
                    
                    // We want cards like Footbottom Feast to return at least two cards
                    if (anyNumber[0] 
                            && choices.size() >= 2) {
                        choices.shuffle();
                        setTargetList(choices);
                        return true;
                    }
                    
                    if (choices.size() > 0) {
                        for (int nctr = 0; nctr < numCardsToReturn; nctr ++) {
                            for (int i = 0; i < Tgts.length; i++) {
                            
                                if (Tgts[i].startsWith("Artifact")) {
                                    if (CardFactoryUtil.AI_getBestArtifact(choices) != null) {
                                        Card c = CardFactoryUtil.AI_getBestArtifact(choices);
                                        results.add(c);
                                        choices.remove(c);
                                    }
                                } else if (Tgts[i].startsWith("Creature")) {
                                    if (CardFactoryUtil.AI_getBestCreature(choices) != null) {
                                        Card c = CardFactoryUtil.AI_getBestCreature(choices);
                                        results.add(c);
                                        choices.remove(c);
                                    }
                                } else if (Tgts[i].startsWith("Enchantment")) {
                                    if (CardFactoryUtil.AI_getBestEnchantment(choices, card, true) != null) {
                                        Card c = CardFactoryUtil.AI_getBestEnchantment(choices, card, true);
                                        results.add(c);
                                        choices.remove(c);
                                    }
                                } else if (Tgts[i].startsWith("Land")) {
                                    if (CardFactoryUtil.AI_getBestLand(choices) != null) {
                                        Card c = CardFactoryUtil.AI_getBestLand(choices);
                                        results.add(c);
                                        choices.remove(c);
                                    }
                                } else if (Tgts[i].startsWith("Permanent")) {
                                    if (CardFactoryUtil.AI_getMostExpensivePermanent(choices, card, true) != null) {
                                        Card c = CardFactoryUtil.AI_getMostExpensivePermanent(choices, card, true);
                                        results.add(c);
                                        choices.remove(c);
                                    }
                                } else if (Tgts[i].startsWith("Instant")) {
                                    if (CardFactoryUtil.AI_getMostExpensivePermanent(choices, card, true) != null) {
                                        // Card c = CardFactoryUtil.AI_getMostExpensivePermanent(choices, card, true);
                                        Card c = CardFactoryUtil.getRandomCard(choices);
                                        results.add(c);
                                        choices.remove(c);
                                    }
                                } else if (Tgts[i].startsWith("Sorcery")) {
                                    if (CardFactoryUtil.AI_getMostExpensivePermanent(choices, card, true) != null) {
                                        // Card c = CardFactoryUtil.AI_getMostExpensivePermanent(choices, card, true);
                                        Card c = CardFactoryUtil.getRandomCard(choices);
                                        results.add(c);
                                        choices.remove(c);
                                    }
                                } else {
                                    if (CardFactoryUtil.AI_getMostExpensivePermanent(choices, card, true) != null) {
                                        // Card c = CardFactoryUtil.AI_getMostExpensivePermanent(choices, card, true);
                                        Card c = CardFactoryUtil.getRandomCard(choices);
                                        results.add(c);
                                        choices.remove(c);
                                    }
                                }
                            }// for i
                        }// for nctr
                    }// if choices
                    
                    if (!anyNumber[0]) {
                        CardList targets = new CardList();
                        
                        if (results.size() >= numCardsToReturn) {
                            results.shuffle();
                            for (int i = 0; i < numCardsToReturn; i++) {
                                targets.add(results.get(i));
                            }
                            
                        } else if (results.size() >= 1  
                                       && returnUpTo[0]) {
                            targets = results;
                        }
                        
                        if (targets.size() > 0) {
                            setTargetList(targets);
                            return true;
                        }
                    }
                    return false;
                }// canPlayAI()
                
                @Override
                public void resolve() {
                    
                    CardList targets = getTargetList();
                    PlayerZone grave = AllZone.getZone(Constant.Zone.Graveyard, card.getController());
                    Player player = card.getController();
                    
                    for (Card c:targets) {
                        
                        if (AllZone.GameAction.isCardInZone(c, grave)) {
                            
                            if (Destination.equals("Hand")) {
                                PlayerZone zone = AllZone.getZone(Constant.Zone.Hand, player);
                                AllZone.GameAction.moveTo(zone, c);
                            }
                            else if (Destination.equals("Battlefield")) {
                                PlayerZone zone = AllZone.getZone(Constant.Zone.Play, player);
                                AllZone.GameAction.moveTo(zone, c);
                            }
                            else if (Destination.equals("TopofLibrary")) {
                                // PlayerZone zone = AllZone.getZone(Constant.Zone.Play, player);
                                AllZone.GameAction.moveToTopOfLibrary(c);
                            }
                        }
                    }// for
                    
                    if (!Drawback[0].equals("none")) {
                        CardFactoryUtil.doDrawBack(Drawback[0], 0, card.getController(), 
                                card.getController().getOpponent(), card.getController(), card, card, this);
                    }
                }// resolve()
                
                CardList getGraveyardList() {
                    
                	Player player = card.getController();
                    PlayerZone grave = AllZone.getZone(Constant.Zone.Graveyard, player);
                    CardList list = new CardList(grave.getCards());
                    list = list.getValidCards(Tgts,card.getController(),card);
                    
                    // AI will not use a Boggart Birth Rite to return a Boggart Birth Rite.
                    // In testing the AI targeted a Sage's Knowledge with a Deja Vu.
                    // Fixed this by having AI pick a random Instant or Sorcery
                    // rather than picking the card with highest casting cost.
                    
                    if (card.getController().equals(AllZone.ComputerPlayer)) {
                        list = list.getNotName(card.getName());
                        if (Destination.equals("Battlefield") 
                                && !AllZone.Phase.getPhase().equals(Constant.Phase.Main1)) {
                            list = list.getNotKeyword("At the beginning of the end step, destroy CARDNAME.");
                            list = list.getNotKeyword("At the beginning of the end step, exile CARDNAME.");
                            list = list.getNotKeyword("At the beginning of the end step, sacrifice CARDNAME.");
                        }
                        
                        // I failed to solve the problem above with this code.
                        //
                        //CardList tmp = list;
                        //for (int i = 0; i < tmp.size(); i++) {
                        //    ArrayList<String> kw = tmp.get(i).getKeyword();
                        //    for (int j = 0; j < kw.size(); j++) {
                        //        if (kw.get(j).toString().startsWith("spReturnTgt")) {
                        //            list.remove(kw.get(j));
                        //        }
                        //    }
                        //}
                    }
                    return list;
                }// getGraveyardList()
            };// spRtrnTgt
            
            spRtrnTgt.setBeforePayMana(CardFactoryUtil.spReturnTgt_input_targetCards_InGraveyard(
                    card, spRtrnTgt, returnUpTo[0], numCardsToReturn, Tgts, anyNumber[0]));
            
            if (desc.length() > 0) {
                spRtrnTgt.setDescription(desc);
            }
            
            card.clearSpellAbility();
            card.addSpellAbility(spRtrnTgt);
            
            if (Destination.equals("Hand")) {
                card.setSVar("PlayMain1", "TRUE");
            }
            
            String bbCost = card.getSVar("Buyback");
            if (!bbCost.equals("")) {
                
               SpellAbility bbRtrnTgt = spRtrnTgt.copy();
               bbRtrnTgt.setManaCost(CardUtil.addManaCosts(card.getManaCost(), bbCost));
               
               StringBuilder sb = new StringBuilder();
               sb.append("Buyback ").append(bbCost).append(" (You may pay an additional ").append(bbCost);
               sb.append(" as you cast this spell. If you do, put this card into your hand as it resolves.)");
               bbRtrnTgt.setDescription(sb.toString());
               
               bbRtrnTgt.setIsBuyBackAbility(true);
               bbRtrnTgt.setBeforePayMana(CardFactoryUtil.spReturnTgt_input_targetCards_InGraveyard(
                       card, bbRtrnTgt, returnUpTo[0], numCardsToReturn, Tgts, anyNumber[0]));
               card.addSpellAbility(bbRtrnTgt);
            }
        }// spReturnTgt
        */
        
        /**
         *  Generic return target card(s) from graveyard to Hand, Battlefield or Top of Library.
         *  This version handles abilities that activate when card enters the battlefield.
         *  spReturnTgt:{Num Cards/Parameters}:{Type}:{To Zone}:{DrawBack}:{Spell Desc}
         *  
         *  Buyback and X Count/Costs are not yet implemented.
         */
        while (hasKeyword(card, "etbReturnTgt") != -1) {
            int n = hasKeyword(card, "etbReturnTgt");
            
            String parse = card.getKeyword().get(n).toString();
            card.removeIntrinsicKeyword(parse);
            String k[] = parse.split(":");
            final boolean returnUpTo[] = {false};
            final boolean mayReturn[] = {false};
            final int numCardsToReturn;
            
            String np[] = k[1].split("/");
            numCardsToReturn = Integer.parseInt(np[0]);
            
            if (np.length > 1) {
                if (np[1].equals("UpTo")) {
                    returnUpTo[0] = true;
                } else if (np[1].equals("MayReturn")) {
                    mayReturn[0] = true;
                }
            }
            
            //  Artifact, Creature, Enchantment, Land, Permanent, Instant, Sorcery, Card
            //  White, Blue, Black, Red, Green, Colorless, MultiColor
            //  non-Artifact, non-Creature, non-Enchantment, non-Land, non-Permanent,
            //  non-White, non-Blue, non-Black, non-Red, non-Green, non-Colorless, non-MultiColor
            
            String Targets = k[2];
            final String Tgts[] = Targets.split(",");
            
            final String Destination = k[3];
            
            String desc = "";
            final String Drawback[] = {"none"};
            
            if (k.length > 4) {
                
                if (k[4].contains("Drawback$")){
                    String kk[] = k[4].split("\\$");
                    Drawback[0] = kk[1];
                } else {
                    desc = k[4];
                }
            }
            if (k.length > 5) {
                desc = k[5];
            }
            
            final SpellAbility etbRtrnTgt = new Ability(card, "0") {

                @Override
                public void resolve() {
                    
                    CardList targets = getTargetList();
                    PlayerZone grave = AllZone.getZone(Constant.Zone.Graveyard, card.getController());
                    Player player = card.getController();
                    
                    for (Card c:targets) {
                        
                        if (AllZone.GameAction.isCardInZone(c, grave)) {
                            
                            if (Destination.equals("Hand")) {
                                PlayerZone zone = AllZone.getZone(Constant.Zone.Hand, player);
                                AllZone.GameAction.moveTo(zone, c);
                            }
                            else if (Destination.equals("Battlefield")) {
                                PlayerZone zone = AllZone.getZone(Constant.Zone.Battlefield, player);
                                AllZone.GameAction.moveTo(zone, c);
                            }
                            else if (Destination.equals("TopofLibrary")) {
                                // PlayerZone zone = AllZone.getZone(Constant.Zone.Play, player);
                                AllZone.GameAction.moveToTopOfLibrary(c);
                            }
                        }
                    }// for
                    
                    if (!Drawback[0].equals("none")) {
                        CardFactoryUtil.doDrawBack(Drawback[0], 0, card.getController(), 
                                card.getController().getOpponent(), card.getController(), card, card, this);
                    }
                }//resolve()
            };// etbRtrnTgt
            
            Command intoPlay = new Command() {
                private static final long serialVersionUID = -8592314045228582326L;

                public void execute() {
                    
                    Player player = card.getController();
                    PlayerZone grave = AllZone.getZone(Constant.Zone.Graveyard, player);
                    CardList results = new CardList();
                    CardList choices = new CardList(grave.getCards());
                    choices = choices.getValidCards(Tgts,card.getController(),card);
                    
                    // AI will not use an Eternal Witness to return an Eternal Witness.
                    
                    if (card.getController().equals(AllZone.ComputerPlayer)) {
                        choices = choices.getNotName(card.getName());
                        if (Destination.equals("Battlefield") 
                                && !AllZone.Phase.getPhase().equals(Constant.Phase.Main1)) {
                            choices = choices.getNotKeyword("At the beginning of the end step, destroy CARDNAME.");
                            choices = choices.getNotKeyword("At the beginning of the end step, exile CARDNAME.");
                            choices = choices.getNotKeyword("At the beginning of the end step, sacrifice CARDNAME.");
                        }
                    }
                    
                    if (choices.isEmpty()) return;
                    
                    if (card.getController().equals(AllZone.HumanPlayer)) {
                        
                        CardList targets = new CardList();
                        
                        for (int i = 0; i < numCardsToReturn; i++) {
                            if (grave.size() > 0) {
                                Object o;
                                if (mayReturn[0] || returnUpTo[0]) {
                                    o = AllZone.Display.getChoiceOptional("Select a card", choices.toArray());
                                } else {
                                    o = AllZone.Display.getChoice("Select a card", choices.toArray());
                                }
                                if (o == null) break;
                                Card c = (Card) o;
                                targets.add(c);
                                choices.remove(c);
                            }
                        }
                        if (targets.size() > 0) {
                            etbRtrnTgt.setTargetList(targets);
                            AllZone.Stack.add(etbRtrnTgt);
                        }
                    }// if HumanPlayer
                    
                    else { // ComputerPlayer
                        
                        for (int nctr = 0; nctr < numCardsToReturn; nctr ++) {
                            for (int i = 0; i < Tgts.length; i++) {
                            
                                if (Tgts[i].startsWith("Artifact")) {
                                    if (CardFactoryUtil.AI_getBestArtifact(choices) != null) {
                                        Card c = CardFactoryUtil.AI_getBestArtifact(choices);
                                        results.add(c);
                                        choices.remove(c);
                                    }
                                } else if (Tgts[i].startsWith("Creature")) {
                                    if (CardFactoryUtil.AI_getBestCreature(choices) != null) {
                                        Card c = CardFactoryUtil.AI_getBestCreature(choices);
                                        results.add(c);
                                        choices.remove(c);
                                    }
                                } else if (Tgts[i].startsWith("Enchantment")) {
                                    if (CardFactoryUtil.AI_getBestEnchantment(choices, card, true) != null) {
                                        Card c = CardFactoryUtil.AI_getBestEnchantment(choices, card, true);
                                        results.add(c);
                                        choices.remove(c);
                                    }
                                } else if (Tgts[i].startsWith("Land")) {
                                    if (CardFactoryUtil.AI_getBestLand(choices) != null) {
                                        Card c = CardFactoryUtil.AI_getBestLand(choices);
                                        results.add(c);
                                        choices.remove(c);
                                    }
                                } else if (Tgts[i].startsWith("Permanent")) {
                                    if (CardFactoryUtil.AI_getMostExpensivePermanent(choices, card, true) != null) {
                                        Card c = CardFactoryUtil.AI_getMostExpensivePermanent(choices, card, true);
                                        results.add(c);
                                        choices.remove(c);
                                    }
                                } else if (Tgts[i].startsWith("Instant")) {
                                    if (CardFactoryUtil.AI_getMostExpensivePermanent(choices, card, true) != null) {
                                        // Card c = CardFactoryUtil.AI_getMostExpensivePermanent(choices, card, true);
                                        Card c = CardFactoryUtil.getRandomCard(choices);
                                        results.add(c);
                                        choices.remove(c);
                                    }
                                } else if (Tgts[i].startsWith("Sorcery")) {
                                    if (CardFactoryUtil.AI_getMostExpensivePermanent(choices, card, true) != null) {
                                        // Card c = CardFactoryUtil.AI_getMostExpensivePermanent(choices, card, true);
                                        Card c = CardFactoryUtil.getRandomCard(choices);
                                        results.add(c);
                                        choices.remove(c);
                                    }
                                } else {
                                    if (CardFactoryUtil.AI_getMostExpensivePermanent(choices, card, true) != null) {
                                        // Card c = CardFactoryUtil.AI_getMostExpensivePermanent(choices, card, true);
                                        Card c = CardFactoryUtil.getRandomCard(choices);
                                        results.add(c);
                                        choices.remove(c);
                                    }
                                }
                            }// for i
                        }// for nctr
                        if (results.size() > 0) {
                            etbRtrnTgt.setTargetList(results);
                            AllZone.Stack.add(etbRtrnTgt);
                        }
                    }// ComputerPlayer
                    
                }// execute()
            };// Command()
            card.addComesIntoPlayCommand(intoPlay);
            
            if (desc.length() > 0) {
                etbRtrnTgt.setDescription(desc);
            }
        }// etbReturnTgt
        
        /* all cards converted to SP$Fetch
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
                    CardList list = AllZoneUtil.getPlayerCardsInLibrary(AllZone.ComputerPlayer);
                    list = list.getValidCards(Tgts);
                	  if (list.size() > 0) return true;
                    return false;
                }
                
               @Override
                public void resolve() {
            	   Player player = card.getController();
                    if(player.isHuman()) humanResolve();
                    else computerResolve();   
                }
                
                public void computerResolve() {
                    CardList list = AllZoneUtil.getPlayerCardsInLibrary(AllZone.ComputerPlayer);
                    list = list.getValidCards(Tgts);
                                        
                    if(list.size() != 0) {
                        //comp will just grab the first one it finds, but tries to avoid another copy of same tutor
                    	if (list.getNotName(card.getName()).size() != 0) 
                    	{
                    		list = list.getNotName(card.getName());
                    	}
                        Card c = list.get(0);
                        card.getController().shuffle();
                        AllZone.Computer_Library.remove(c);
                        if (Destination.equals("Hand")) AllZone.Computer_Hand.add(c);         			//move to hand
                        if (Destination.equals("TopOfLibrary")) AllZone.Computer_Library.add(c, 0); //move to top of library
                        if (Destination.equals("ThirdFromTopOfLibrary")) AllZone.Computer_Library.add(c, 2); //move to third from top of library
                        if (Destination.equals("Battlefield")) AllZone.getZone(Constant.Zone.Play, AllZone.ComputerPlayer).add(c); //move to battlezone

                        if (!Targets.startsWith("Card") && !Destination.equals("Battlefield")) {
                        	CardList l = new CardList();
                        	l.add(c);
                        	AllZone.Display.getChoiceOptional("Computer picked:", l.toArray());
                        }
                    }
                }//computerResolve()
                
                public void humanResolve() {
                    CardList list = AllZoneUtil.getPlayerCardsInLibrary(AllZone.HumanPlayer);
                    list = list.getValidCards(Tgts);
                    
                    if(list.size() != 0) {
                        Object o = AllZone.Display.getChoiceOptional("Select a card", list.toArray());
                        
                        card.getController().shuffle();
                        if(o != null) {
                        	AllZone.Human_Library.remove(o);
                        	if (Destination.equals("Hand")) AllZone.Human_Hand.add((Card) o);         			//move to hand
                            if (Destination.equals("TopOfLibrary")) AllZone.Human_Library.add((Card) o, 0); //move to top of library
                            if (Destination.equals("ThirdFromTopOfLibrary")) AllZone.Human_Library.add((Card) o, 2); //move to third from top of library
                            if (Destination.equals("Battlefield")) AllZone.getZone(Constant.Zone.Play, AllZone.HumanPlayer).add((Card) o); //move to battlezone
                        }
                    }//if
                }//resolve()
            }; // spell ability SpTutorTgt
            
            spTtr.setDescription(card.getSpellText());
            card.setText("");
            card.addSpellAbility(spTtr);
        }//spTutor
		*/
        
        
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
                                card.getController().drawCard();
                            }
                        } else if(card.getName().equals("Caldera Hellion")) {
                            CardList creatures = AllZoneUtil.getCreaturesInPlay();
                            
                            for(int i = 0; i < creatures.size(); i++) {
                                Card crd = creatures.get(i);
                                if(CardFactoryUtil.canDamage(card, crd)) crd.addDamage(3, card);
                            }
                        }
                        
                    }
                    
                    @Override
                    public boolean canPlay() {
                        return AllZone.Phase.getPlayerTurn().equals(card.getController()) && card.isFaceDown()
                                && AllZone.GameAction.isCardInPlay(card);
                    }
                    
                };//devour
                
                Command intoPlay = new Command() {
                    private static final long serialVersionUID = -7530312713496897814L;
                    
                    public void execute() {
                        
                        PlayerZone play = AllZone.getZone(Constant.Zone.Battlefield, card.getController());
                        CardList creats = new CardList(play.getCards());
                        creats = creats.filter(new CardListFilter() {
                            public boolean addCard(Card c) {
                                return c.isCreature() && !c.equals(card);
                            }
                        });
                        
                        //System.out.println("Creats size: " + creats.size());
                        
                        if(card.getController().equals(AllZone.HumanPlayer)) {
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
                                AllZone.Computer_Battlefield.updateObservers();
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
                        Card card2 = this.getTargetCard();
                        card2.addCounter(Counters.P1P1, getSourceCard().getCounters(Counters.P1P1));
                    }//resolve()
                };
                
                card.addDestroyCommand(new Command() {
                    private static final long serialVersionUID = 304026662487997331L;
                    
                    public void execute() {
                        // Target as Modular is Destroyed
                        if(card.getController().equals(AllZone.ComputerPlayer)) {
                            CardList choices = new CardList(AllZone.Computer_Battlefield.getCards()).filter(new CardListFilter() {
                                public boolean addCard(Card c) {
                                    return c.isCreature() && c.isArtifact();
                                }
                            });
                            if(choices.size() != 0){
                            	ability.setTargetCard(CardFactoryUtil.AI_getBestCreature(choices));
                            	
                            	if (ability.getTargetCard() != null){
	                            	ability.setStackDescription("Put " + card.getCounters(Counters.P1P1)
	                                        + " +1/+1 counter/s from " + card + " on " + ability.getTargetCard());
	                            	AllZone.Stack.add(ability);
                            	}
                            }
                        }
                        else{
                        	AllZone.InputControl.setInput(CardFactoryUtil.modularInput(ability, card));
                        }
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
            
            final Ability_Activated ability = new Ability_Activated(card, cost, tgt) {
				private static final long serialVersionUID = -1680422554249396500L;

				@Override
                public boolean canPlay() {
                	return super.canPlay();
                }//canPlay()
                
                @Override
                public boolean canPlayAI() {
                    return getCreature().size() != 0 && ComputerUtil.canPayCost(this)
                            && !CardFactoryUtil.AI_doesCreatureAttack(card)
                    		&& super.canPlayAI();
                }//canPlayAI()
                
                @Override
                public void chooseTargetAI() {
                    Card best = CardFactoryUtil.AI_getBestCreature(getCreature());
                    setTargetCard(best);
                    card.subtractCounter(Counters.P1P1, 1);
                }//chooseTargetAI()
                
                CardList getCreature() {
                    CardList list = new CardList(AllZone.Computer_Battlefield.getCards());
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
            ability.setDescription(cost.toString() + " Put a +1/+1 counter on target creature");
            
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
            sb.append(" counter");
            if(1 != numCounters) sb.append("s"); 
            sb.append(" on it.");
            
            card.setText(sb.toString());

            card.addComesIntoPlayCommand(new Command() {
                private static final long serialVersionUID = -2292898970576123040L;

                public void execute() {
                    card.addCounter(counter, numCounters);
                }
            });//ComesIntoPlayCommand
        } // if etbCounter
        
        /* Cards converted to SP$Pump
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
                                card.getController(), card.getController().getOpponent(),
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

        }// spPumpTgt
        */
        

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
                	 card.getController().addDamage(amountHurt, card);
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
                        card.getController().drawCard();
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
        			
        			if (card.getController().equals(AllZone.HumanPlayer))
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
        			card.getController().shuffle();
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
						return super.canPlayAI();
					}
				}
				
				@Override
            	public void resolve() {
					Player controller = (controllerString.equals("Controller") ? card.getController() : card.getController().getOpponent());
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
        
        //**************************************************
        // AbilityFactory cards
        ArrayList<String> IA = card.getIntrinsicAbilities();
        if (IA.size() > 0)
        {
        	if (card.isInstant() || card.isSorcery())
        		card.clearSpellAbility();
        	
        	for (int i=0; i<IA.size(); i++)
        	{
        		AbilityFactory AF = new AbilityFactory();
        		SpellAbility sa = AF.getAbility(IA.get(i), card);
        		
        		card.addSpellAbility(sa);
        		
        		String bbCost = card.getSVar("Buyback"); 
        		if (!bbCost.equals(""))
        		{
        			SpellAbility bbSA = sa.copy();
        			   String newCost = CardUtil.addManaCosts(card.getManaCost(), bbCost);
        			   if (bbSA.payCosts != null)
        			      bbSA.payCosts = new Ability_Cost(newCost, sa.getSourceCard().getName(), false); // create new abCost
        			   StringBuilder sb = new StringBuilder();
        			   sb.append("Buyback ").append(bbCost).append(" (You may pay an additional ").append(bbCost);
        			   sb.append(" as you cast this spell. If you do, put this card into your hand as it resolves.)");
        			   bbSA.setDescription(sb.toString());
        			   bbSA.setIsBuyBackAbility(true);
                                        
                    card.addSpellAbility(bbSA);
        		}
        	}
        		
        }

        
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
                    CardList human = new CardList(AllZone.Human_Battlefield.getCards());
                    CardList computer = new CardList(AllZone.Computer_Battlefield.getCards());
                    
                    human = human.getType("Creature");
                    computer = computer.getType("Creature");
                    
                    return AllZone.ComputerPlayer.getLife() > 2 && !(human.size() == 0 && 0 < computer.size()) && super.canPlayAI();
                }
                
                @Override
                public void resolve() {
                    //get all creatures
                    CardList list = AllZoneUtil.getCreaturesInPlay();
                    
                    for(int i = 0; i < list.size(); i++) {
                        if(CardFactoryUtil.canDamage(card, list.get(i))) list.get(i).addDamage(1, card);
                    }
                    
                    AllZone.HumanPlayer.addDamage(1, card);
                    AllZone.ComputerPlayer.addDamage(1, card);
                }//resolve()
            };//SpellAbility
            ability.setDescription("R: Pyrohemia deals 1 damage to each creature and each player.");
            
            StringBuilder sb = new StringBuilder();
            sb.append(card).append(" deals 1 damage to each creature and each player.");
            ability.setStackDescription(sb.toString());
            
            card.clearSpellAbility();
            card.addSpellAbility(new Spell_Permanent(card) {
                private static final long serialVersionUID = 5596915641671666843L;
                
                @Override
                public boolean canPlayAI() {
                    //get all creatures
                    CardList list = AllZoneUtil.getCreaturesInPlay();
                    
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
                    CardList human = new CardList(AllZone.Human_Battlefield.getCards());
                    CardList computer = new CardList(AllZone.Computer_Battlefield.getCards());
                    
                    human = human.getType("Creature");
                    computer = computer.getType("Creature");
                    
                    return AllZone.ComputerPlayer.getLife() > 2 && !(human.size() == 0 && 0 < computer.size());
                }
                
                @Override
                public void resolve() {
                    //get all creatures
                    CardList list = new CardList();
                    list.addAll(AllZone.Human_Battlefield.getCards());
                    list.addAll(AllZone.Computer_Battlefield.getCards());
                    list = list.getType("Creature");
                    
                    for(int i = 0; i < list.size(); i++) {
                        if(CardFactoryUtil.canDamage(card, list.get(i))){
                        	HashMap<Card, Integer> m = new HashMap<Card, Integer>();
                        	m.put(card, 1);
                        	list.get(i).addDamage(m);
                        }
                    }
                    
                    AllZone.HumanPlayer.addDamage(1, card);
                    AllZone.ComputerPlayer.addDamage(1, card);
                }//resolve()
            };//SpellAbility
            ability.setDescription("B: Pestilence deals 1 damage to each creature and each player.");
            
            StringBuilder sb = new StringBuilder();
            sb.append(card).append(" deals 1 damage to each creature and each player.");
            ability.setStackDescription(sb.toString());
            
            card.clearSpellAbility();
            card.addSpellAbility(new Spell_Permanent(card) {
                private static final long serialVersionUID = -4163089323122672307L;
                
                @Override
                public boolean canPlayAI() {
                    //get all creatures
                    CardList list = new CardList();
                    list.addAll(AllZone.Human_Battlefield.getCards());
                    list.addAll(AllZone.Computer_Battlefield.getCards());
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
        else if(cardName.equals("Conspiracy") || cardName.equals("Cover of Darkness")
                || cardName.equals("Door of Destinies") || cardName.equals("Engineered Plague")
                || cardName.equals("Shared Triumph") || cardName.equals("Belbe's Portal")
                || cardName.equals("Steely Resolve")) {
            final String[] input = new String[1];
            final Player player = card.getController();
            
            final SpellAbility ability = new Ability_Static(card, "0") {
                @Override
                public void resolve() {
                    if(player.equals(AllZone.HumanPlayer)) {
                        input[0] = JOptionPane.showInputDialog(null, "Which creature type?", "Pick type",
                                JOptionPane.QUESTION_MESSAGE);
                        
                        if(!CardUtil.isACreatureType(input[0])) input[0] = "";
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
                	
                	StringBuilder sb = new StringBuilder();
                	sb.append("When ").append(card.getName()).append(" comes into play, choose a creature type.");
                	ability.setStackDescription(sb.toString());
                    
                    AllZone.Stack.add(ability);
                }
            };
            card.addComesIntoPlayCommand(intoPlay);
            
        }//*************** END ************ END **************************
        

        //*************** START *********** START **************************
        else if(cardName.equals("Sarpadian Empires, Vol. VII")) {
            
            final String[] choices = {"Citizen", "Camarid", "Thrull", "Goblin", "Saproling"};
            
            final Player player = card.getController();
            
            final SpellAbility ability = new Ability(card, "0") {
                @Override
                public void resolve() {
                    String type = "";
                    String imageName = "";
                    String color = "";
                    
                    if(player.equals(AllZone.ComputerPlayer)) {
                        type = "Thrull";
                        imageName = "B 1 1 Thrull";
                        color = "B";
                    } else if(player.equals(AllZone.HumanPlayer)) {
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
                    
                    Ability_Cost a1Cost = new Ability_Cost("3 T", cardName, true);
                    final Ability_Activated a1 = new Ability_Activated(card, a1Cost, null) {
                        
                        private static final long serialVersionUID = -2114111483117171609L;
                        
                        @Override
                        public void resolve() {
                            CardFactoryUtil.makeToken(t, in, card.getController(), col, new String[] {"Creature", t}, 1, 1,
                                    new String[] {""});
                        }
                        
                    };
                    StringBuilder sb = new StringBuilder();
                    sb.append(card.getName()).append(" - ").append(card.getController());
                    sb.append(" puts a 1/1 ").append(t).append(" token into play");
                    a1.setStackDescription(sb.toString());
                    
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
                	int n = card.getController().getLife();
                	if (n > 0)
                		card.addCounter(Counters.CHARGE, n);
                }
            };
            card.addComesIntoPlayCommand(intoPlay);          
        }//*************** END ************ END **************************
        

        //*************** START *********** START **************************
        else if(cardName.equals("Oblivion Ring")) {
                    	
        	final CommandReturn getPerm = new CommandReturn() {
                public Object execute() {
                    //get all creatures
                    CardList tempList = AllZoneUtil.getCardsInPlay();
                    
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
                        AllZone.GameAction.exile(getTargetCard());
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
                    Player s = card.getController();
                    if(perm.size() == 0) return;
                    else if(s.equals(AllZone.HumanPlayer)) AllZone.InputControl.setInput(inputComes);
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
                            tempList.addAll(AllZone.Human_Battlefield.getCards());
                            
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
                            humanPerms.addAll(AllZone.Human_Battlefield.getCards());
                            humanPerms = humanPerms.filter(new CardListFilter() {
                                public boolean addCard(Card c) {
                                    return c.isPermanent() && !c.isLand() && CardFactoryUtil.canTarget(card, c);
                                }
                            });
                            
                            if(humanPerms.size() > 0) target = humanPerms.get(0);
                        }
                        
                        if(target == null) {
                            //must target computer creature
                            CardList computer = new CardList(AllZone.Computer_Battlefield.getCards());
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
                    if(o == null || ((Card) o).isToken() || !AllZone.GameAction.isCardExiled((Card) o)) return;
                    
                    SpellAbility ability = new Ability(card, "0") {
                        @Override
                        public void resolve() {
                            //copy card to reset card attributes like attack and defense
                            Card c = abilityComes.getTargetCard();
                            if(!c.isToken()) {
                                c = AllZone.CardFactory.dynamicCopyCard(c);
                                c.setController(c.getOwner());
                                
                                PlayerZone play = AllZone.getZone(Constant.Zone.Battlefield, c.getOwner());
                                PlayerZone removed = AllZone.getZone(Constant.Zone.Exile, c.getOwner());
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
                    
                    CardList cList = new CardList(AllZone.Human_Battlefield.getCards());
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
                        AllZone.GameAction.exile(getTargetCard());
                        
                        //put permanent into play
                        Card c = getSourceCard();
                        AllZone.getZone(Constant.Zone.Battlefield, c.getController()).add(c);
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

            Command commandLeavesPlay = new Command() {
                private static final long serialVersionUID = -2535098005246027777L;
                
                public void execute() {
                    Object o = enchantment.getTargetCard();
                    if(o == null || ((Card) o).isToken() || !AllZone.GameAction.isCardExiled((Card) o)) return;
                    
                    SpellAbility ability = new Ability(card, "0") {
                        @Override
                        public void resolve() {
                            //copy card to reset card attributes like attack and defense
                            Card c = enchantment.getTargetCard();
                            if(!c.isToken()) {
                                c = AllZone.CardFactory.copyCard(c);
                                c.setController(c.getOwner());
                                
                                PlayerZone play = AllZone.getZone(Constant.Zone.Battlefield, c.getOwner());
                                play.add(c);
                            }
                        }//resolve()
                    };//SpellAbility
                    StringBuilder sb = new StringBuilder();
                    sb.append(card.getName()).append(" - returning creature to play");
                    ability.setStackDescription(sb.toString());
                    
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
                    
                    ((PlayerZone_ComesIntoPlay) AllZone.Human_Battlefield).setTriggers(false);
                    ((PlayerZone_ComesIntoPlay) AllZone.Computer_Battlefield).setTriggers(false);
                    
                    PlayerZone from = AllZone.getZone(c);
                    PlayerZone to = AllZone.getZone(Constant.Zone.Battlefield, card.getController());
                    
                    from.remove(c);
                    to.add(c);
                    
                    ((PlayerZone_ComesIntoPlay) AllZone.Human_Battlefield).setTriggers(true);
                    ((PlayerZone_ComesIntoPlay) AllZone.Computer_Battlefield).setTriggers(true);
                    
                }//resolve()
            };
            
            card.clearSpellAbility();
            spell.setBeforePayMana(CardFactoryUtil.input_targetType(spell, "All"));
            card.addSpellAbility(spell);
            
            card.setSVar("PlayMain1", "TRUE");
        }//*************** END ************ END **************************
        

        //*************** START *********** START **************************
        else if(cardName.equals("That Which Was Taken")) {
        	Ability_Cost abCost = new Ability_Cost("4 T", cardName, true);
        	Target target = new Target("Select target permanent other than "+cardName, new String[] {"Permanent.Other"});
            final Ability_Activated ability = new Ability_Activated(card, abCost, target) {
                private static final long serialVersionUID = -8996435083734446340L;
                
                @Override
                public void resolve() {
                    Card c = getTargetCard();
                    
                    if(AllZone.GameAction.isCardInPlay(c) && CardFactoryUtil.canTarget(card, c))
                    	c.addCounter(Counters.DIVINITY, 1);
                }
                
                @Override
                public boolean canPlayAI() {
                    CardList perms = getPerms();
                    
                    return perms.size()>0;
                }
                
                @Override
                public void chooseTargetAI() {
                    CardList a = getPerms();
                    if (a.size()>0) {
	                    setTargetCard(a.get(0));
                    }
                }
                
                CardList getPerms() {
                    CardList list = AllZoneUtil.getPlayerCardsInPlay(AllZone.ComputerPlayer);
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
            ability.setDescription(abCost+"Put a divinity counter on target permanent other than "+cardName+".");
            card.addSpellAbility(ability);
        }//*************** END ************ END **************************
  
        
        //*************** START *********** START **************************
        else if(cardName.equals("Midsummer Revel")) {   
           
        	final SpellAbility ability = new Ability(card, "G") {

        		@Override
        		public boolean canPlayAI() {
        			if(card.getCounters(Counters.VERSE) > 0) return true;
        			return false;
        		}

        		@Override
        		public void resolve() { 
        				for(int i = 0; i < card.getCounters(Counters.VERSE); i++) {
                            CardFactoryUtil.makeToken("Beast", "G 3 3 Beast", card.getController(), "G", new String[] {
                                    "Creature", "Beast"}, 3, 3, new String[] {""});
        				}
        			AllZone.GameAction.sacrifice(card);
        		}
        	};
            card.addSpellAbility(ability);

            StringBuilder sbStack = new StringBuilder();
            sbStack.append(cardName).append(" puts X 3/3 green Beast creature tokens onto the battlefield");
            ability.setStackDescription(sbStack.toString());
            
            StringBuilder sbDesc = new StringBuilder();
            sbDesc.append("G, Sacrifice Midsummer Revel: Put X 3/3 green Beast creature tokens onto ");
            sbDesc.append("the battlefield, where X is the number of verse counters on Midsummer Revel.");
            ability.setDescription(sbDesc.toString());
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
            
            StringBuilder sb = new StringBuilder();
            sb.append(card.getController());
            sb.append(" exiles target creature card in a graveyard. Puts a 1/1 green Saproling creature token into play.");
            necrogen.setStackDescription(sb.toString());
            
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
					CardList grave = AllZoneUtil.getPlayerGraveyard(AllZone.HumanPlayer);
					CardList aiGrave = AllZoneUtil.getPlayerGraveyard(AllZone.ComputerPlayer);
					return (grave.getType("Creature").size() > 1 || aiGrave.getType("Creature").size() > 1) && super.canPlay();
                }
            };
            Input soilTarget = new Input() {

            	boolean once = false;
				private static final long serialVersionUID = 8243511353958609599L;

				@Override
                public void showMessage() {
					CardList grave = AllZoneUtil.getPlayerGraveyard(AllZone.HumanPlayer);
					CardList aiGrave = AllZoneUtil.getPlayerGraveyard(AllZone.ComputerPlayer);
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
						
						Object o = AllZone.Display.getChoice("Choose first creature to exile", chooseGrave.toArray());
						if (o!=null)
						{
							CardList newGrave;
							Card c = (Card)o;
							if (c.getOwner().equals(AllZone.HumanPlayer)){
								newGrave = AllZoneUtil.getPlayerGraveyard(AllZone.HumanPlayer);
							}
							else {
								newGrave = AllZoneUtil.getPlayerGraveyard(AllZone.ComputerPlayer);
							}
							
							newGrave = newGrave.getType("Creature");
							newGrave.remove(c);
							
							Object o2 = AllZone.Display.getChoice("Choose second creature to exile", newGrave.toArray());
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
            
            nightSoil.setDescription("1, Exile two creature cards from a single graveyard: Put a 1/1 green Saproling creature token onto the battlefield.");
            
            StringBuilder sb = new StringBuilder();
            sb.append(card.getController()).append(" puts a 1/1 green Saproling creature token onto the battlefield.");
            nightSoil.setStackDescription(sb.toString());
            
            nightSoil.setAfterPayMana(soilTarget);
            card.addSpellAbility(nightSoil);
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
            
            ability.setDescription("Pay 1 life: Set aside the top card of your library face down. At the end of your turn, put that card into your hand.");
            
            StringBuilder sb = new StringBuilder();
            sb.append(card.getName());
            sb.append(" - Set aside the top card of your library face down. At the end of your turn, put that card into your hand.");
            ability.setStackDescription(sb.toString());
            
            card.addSpellAbility(ability);
            
            //instead of paying mana, pay life and add to stack
            //Input showMessage() is always the first method called
            Input payLife = new Input() {
                private static final long serialVersionUID = -3846772748411690084L;
                
                @Override
                public void showMessage() {
                	boolean paid = card.getController().payLife(1, card);
                    
                    //this order is very important, do not change
                    stop();
                    if (paid)
                    	AllZone.Stack.add(ability);
                }
            };//Input
            ability.setBeforePayMana(payLife);
            
        }//*************** END ************ END **************************
        

        //*************** START *********** START **************************  
        else if(cardName.equals("Hatching Plans")) {
            
            final Ability ability = new Ability(card, "0") {
                @Override
                public void resolve() {
                    card.getController().drawCards(3);
                }
            };
            
            Command draw3Cards = new Command() {
                private static final long serialVersionUID = -4919203791300685078L;
                
                public void execute() {
                	
                	StringBuilder sb = new StringBuilder();
                	sb.append(card.getName()).append(" - draw three cards.");
                	ability.setStackDescription(sb.toString());
                    
                    AllZone.Stack.add(ability);
                }
            };
            
            card.addDestroyCommand(draw3Cards);
        }//*************** END ************ END **************************

        
        //*************** START *********** START **************************
        else if(cardName.equals("Lifespark Spellbomb")) {
        	Ability_Cost abCost = new Ability_Cost("G Sac<1/CARDNAME>", cardName, true);
        	String[] valid = {"Land"};
        	Target abTgt = new Target("Target a land to animate", valid);
            final Ability_Activated ability = new Ability_Activated(card, abCost, abTgt) {
                private static final long serialVersionUID = -5744842090293912606L;
                
                @Override
                public boolean canPlay() {
                    return AllZone.GameAction.isCardInPlay(card);
                }
                
                @Override
                public boolean canPlayAI() {
                    CardList land = new CardList(AllZone.Computer_Battlefield.getCards());
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
        else if(cardName.equals("Sensei's Divining Top")) {
            //ability2: Draw card, and put divining top on top of library
        	Ability_Cost abCost2 = new Ability_Cost("T", cardName, true);
            final Ability_Activated ability2 = new Ability_Activated(card, abCost2, null) {
                private static final long serialVersionUID = -2523015092351744208L;
                
                @Override
                public void resolve() {
                	Player player = card.getController();
                    
                    player.drawCard();
                    AllZone.GameAction.moveToTopOfLibrary(card); //move divining top to top of library
                    card.untap();
                    
                }
                
                @Override
                public boolean canPlayAI() {
                    return false;
                }
                
                @Override
                public boolean canPlay() {
                    if(AllZone.getZone(card).is(Constant.Zone.Battlefield)) return true;
                    else return false;
                }//canPlay()
            };//SpellAbility ability2
            /*
            ability2.setBeforePayMana(new Input() {
                private static final long serialVersionUID = -4773496833654414458L;
                
                @Override
                public void showMessage() {
                    AllZone.Stack.add(ability2);
                    stop();
                }//showMessage()
            });
            */
            

            //ability (rearrange top 3 cards) :
            Ability_Cost abCost = new Ability_Cost("1", cardName, true);
            final Ability_Activated ability1 = new Ability_Activated(card, abCost, null) {
				private static final long serialVersionUID = -4520707446274449995L;

				@Override
                public void resolve() {
                	Player player = card.getController();
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
                    if(AllZone.getZone(card).is(Constant.Zone.Battlefield)) return true;
                    else return false;
                }//canPlay()
            };//ability1
            

            ability1.setDescription(abCost+"Look at the top three cards of your library, then put them back in any order.");
            ability1.setStackDescription(cardName+" - rearrange top 3 cards");
            card.addSpellAbility(ability1);
            
            ability2.setDescription(abCost2+"Draw a card, then put Sensei's Divining Top on top of its owner's library.");
            ability2.setStackDescription(cardName+" - draw a card, then put back on owner's library");
            card.addSpellAbility(ability2);
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
        }//*************** END ************ END **************************     

        
        //*************** START *********** START **************************
        else if(cardName.equals("Counterbalance")) {
        	Player player = card.getController();
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
                	Player player = card.getController();
                    PlayerZone lib = AllZone.getZone(Constant.Zone.Library, player);
                    
                    if(AllZone.Stack.size() == 0 || lib.size() == 0) return false;
                    
                    //see if spell is on stack and that opponent played it
                    Player opponent = card.getController().getOpponent();
                    SpellAbility sa = AllZone.Stack.peek();
                    
                    if(AllZone.getZone(card).is(Constant.Zone.Battlefield) && sa.isSpell()
                            && opponent.equals(sa.getSourceCard().getController())
                            && CardFactoryUtil.isCounterable(sa.getSourceCard())) return true;
                    else return false;
                }//canPlay()
            };//SpellAbility ability
            
            StringBuilder sb = new StringBuilder();
            sb.append("Counterbalance - ").append(player);
            sb.append(" reveals top card and counters spell if it has the same converted manacost");
            ability.setStackDescription(sb.toString());
            
            card.addSpellAbility(ability);
        }//*************** END ************ END **************************
        
        
        //*************** START *********** START **************************
        else if(cardName.equals("Aluren")) {
            final Ability ability1 = new Ability(card, "0") {
                @Override
                public void resolve() {
                    
                    //String player = card.getController();
                    PlayerZone hand = AllZone.getZone(Constant.Zone.Hand, AllZone.HumanPlayer);
                    PlayerZone play = AllZone.getZone(Constant.Zone.Battlefield, AllZone.HumanPlayer);
                    
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
                    if(AllZone.getZone(card).is(Constant.Zone.Battlefield)) return true;
                    else return false;
                }//canPlay()
            };//SpellAbility ability1
            

            ability1.setDescription("Any player may play creature cards with converted mana cost 3 or less without paying their mana cost any time he or she could play an instant.");
            ability1.setStackDescription("Aluren - Play creature with converted manacost 3 or less for free.");
            ability1.getRestrictions().setAnyPlayer(true);
            card.addSpellAbility(ability1);
        }
        //*************** END ************ END **************************
        
        //*************** START *********** START **************************
        else if(cardName.equals("Volrath's Dungeon")) {
        	
        	Ability_Cost dungeonCost = new Ability_Cost("Discard<1/Any>", cardName, true);
        	Target dungeonTgt = new Target("Volrath's Dungeon - Target player" , "player".split(","));
        	
            final SpellAbility dungeon = new Ability_Activated(card, dungeonCost, dungeonTgt){
				private static final long serialVersionUID = 334033015590321821L;

				@Override
                public void chooseTargetAI() {
                    setTargetPlayer(AllZone.HumanPlayer);
                }
                
                @Override
                public void resolve() {
                	Player target = getTargetPlayer();
                    CardList targetHand = new CardList(AllZone.getZone(Constant.Zone.Hand, target).getCards());

                    if (targetHand.size() == 0) return;
                    
                    if (target == AllZone.HumanPlayer){
    	                Object discard = AllZone.Display.getChoice("Select Card to place on top of library.", targetHand.toArray());
    	                
                        Card card = (Card)discard;
                        AllZone.GameAction.moveToTopOfLibrary(card);
                    }
                    else if (target == AllZone.ComputerPlayer){
                    	AllZone.ComputerPlayer.handToLibrary(1, "Top");
                    }
                }
                
                @Override
                public boolean canPlayAI() {
                	return AllZone.Computer_Hand.size() > 0 && AllZone.Human_Hand.size() > 0 && super.canPlay();
                }

            };//SpellAbility dungeon
           
            
        	Ability_Cost bailCost = new Ability_Cost("PayLife<5>", cardName, true);
            final SpellAbility bail = new Ability_Activated(card, bailCost, null){
				private static final long serialVersionUID = -8990402917139817175L;

				@Override
                public void resolve() {
                	AllZone.GameAction.destroy(card);
                }
      
                @Override
                public boolean canPlay() {
                    return super.canPlay();
                }
                                
                @Override
                public boolean canPlayAI() {
                	return card.getController().equals(AllZone.HumanPlayer) && AllZone.ComputerPlayer.getLife() >= 9 && 
                			super.canPlay() && AllZone.Computer_Hand.size() > 0;
                }

            };//SpellAbility pay bail

            dungeon.setDescription("Discard a card: Target player puts a card from his or her hand on top of his or her library. Activate this ability only any time you could cast a sorcery.");
            dungeon.setStackDescription("CARDNAME - Target player chooses a card in hand and puts on top of library.");
            dungeon.getRestrictions().setSorcerySpeed(true);
            
            bail.getRestrictions().setAnyPlayer(true);
            bail.getRestrictions().setPlayerTurn(true);
            bail.setDescription("Pay 5 Life: Destroy Volrath's Dungeon. Any player may activate this ability but only during his or her turn.");
            bail.setStackDescription("Destroy CARDNAME.");
            
            card.addSpellAbility(dungeon);
            card.addSpellAbility(bail);
        }//*************** END ************ END **************************

        
        //*************** START *********** START **************************
        else if(cardName.equals("Khalni Heart Expedition")) {
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
                			Constant.Zone.Battlefield, true, Constant.Zone.Battlefield, true);
                }
            };
            StringBuilder sbDesc = new StringBuilder();
            sbDesc.append("Remove three quest counters from Khalni Heart Expedition and sacrifice it: search ");
            sbDesc.append("your library for two basic lands and put them onto the battlefield tapped.");
            ability.setDescription(sbDesc.toString());

            StringBuilder sbStack = new StringBuilder();
            sbStack.append(card.getName()).append(" - Search for land.");
            ability.setStackDescription(sbStack.toString());
            
            card.addSpellAbility(ability);
        }//*************** END ************ END **************************


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
                        AllZone.HumanPlayer.discard(c, null);
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
                    if(card.getController().equals(AllZone.HumanPlayer)) {
                        if(AllZone.Human_Hand.getCards().length == 0) AllZone.GameAction.sacrifice(card);
                        else AllZone.InputControl.setInput(discard);
                    } else {
                        CardList list = new CardList(AllZone.Computer_Hand.getCards());
                        list = list.filter(new CardListFilter() {
                            public boolean addCard(Card c) {
                                return (c.isLand());
                            }
                        });
                        AllZone.ComputerPlayer.discard(list.get(0), this);
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
                    CardList compCreats = AllZoneUtil.getCreaturesInPlay(AllZone.ComputerPlayer);
                    CardList humCreats = AllZoneUtil.getCreaturesInPlay(AllZone.HumanPlayer);
                    
                    //only play standstill if comp controls more creatures than human
                    //this needs some additional rules, maybe add all power + toughness and compare
                    if(compCreats.size() > humCreats.size()) return true;
                    else return false;
                }
            });
        }//*************** END ************ END **************************
       

        //*************** START ************ START **************************
        else if(cardName.equals("Ashnod's Transmogrant")) {
        	final String[] Tgts = { "Creature.nonArtifact" };
        	
        	Ability_Cost abCost = new Ability_Cost("T Sac<1/CARDNAME>", cardName, true);
        	Target abTgt = new Target("Target a non-Artifact Creature to Transmogrify", Tgts);

        	final Ability_Activated ability = new Ability_Activated(card, abCost, abTgt){
                private static final long serialVersionUID = -401631574059431293L;
                
                @Override
                public void resolve() {
                    Card crd = getTargetCard();
                    // if it's not a valid target on resolution, spell fizzles
                    if (crd == null || !AllZone.GameAction.isCardInPlay(crd) || !crd.isValidCard(Tgts,card.getController(),card))
                    	return;
                    crd.addCounter(Counters.P1P1, 1);
                    
                    // trick to get Artifact on the card type side of the type line
                    ArrayList<String> types = crd.getType();
                    types.add(0, "Artifact");
                    crd.setType(types);
                }
                
                @Override
                public boolean canPlayAI() {
                    CardList list = new CardList(AllZone.Computer_Battlefield.getCards()).filter(new CardListFilter() {
                        public boolean addCard(Card c) {
                            return !c.isArtifact() && c.isCreature();
                        }
                    });
                    Card crd = CardFactoryUtil.AI_getBestCreature(list);
                    if(crd != null) setTargetCard(crd);
                    return (getTargetCard() != null);
                }
            };
            
            StringBuilder sb = new StringBuilder();
            sb.append(abCost.toString());
            sb.append("Put a +1/+1 counter on target nonartifact creature. That creature becomes an artifact in addition to its other types.");
            ability.setDescription(sb.toString());
            
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
                    return AllZone.Phase.is(Constant.Phase.Main2, AllZone.ComputerPlayer);
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
        	Target target = new Target("Select target player", new String[] {"Player"});
        	Ability_Cost abCost = new Ability_Cost("G G R R B B U U W W T Sac<1/CARDNAME>", cardName, true);
        	Ability_Activated ab1 = new Ability_Activated(card, abCost, target) {
                
                private static final long serialVersionUID = 6665327569823149191L;
                
                @Override
                public void resolve() {
                    getTargetPlayer().altLoseConditionMet("Door to Nothingness");
                }
                
                @Override
                public boolean canPlayAI() {
                    return true;
                }
            };
            ab1.setChooseTargetAI(CardFactoryUtil.AI_targetHuman());
            ab1.setDescription(abCost+"Target player loses the game.");
            card.addSpellAbility(ab1);
        }//*************** END ************ END **************************
        
        
        //*************** START *********** START **************************
        else if(cardName.equals("Kaervek's Spite")) {
            final SpellAbility spell = new Spell(card) {
                private static final long serialVersionUID = -6259614160639535500L;
                
                @Override
                public boolean canPlayAI() {
                    if(AllZone.HumanPlayer.getLife() <= 5) return true;
                    
                    CardList playList = AllZoneUtil.getPlayerCardsInPlay(AllZone.ComputerPlayer);
                    CardList libList = AllZoneUtil.getPlayerCardsInLibrary(AllZone.ComputerPlayer);
                    
                    playList = playList.getName("Academy Rector");
                    libList = libList.getName("Barren Glory");
                    
                    return (AllZone.HumanPlayer.getLife() <= 5) || (playList.size() == 1 && libList.size() >= 1);
                }
                
                @Override
                public void resolve() {
                    CardList play = AllZoneUtil.getPlayerCardsInPlay(card.getController());
                    
                    for(Card c:play) {
                        AllZone.GameAction.sacrifice(c);
                    }
                    card.getController().discardHand(this);
                    
                    getTargetPlayer().loseLife(5, card);
                }
            };
            spell.setChooseTargetAI(CardFactoryUtil.AI_targetHuman());
            card.clearSpellAbility();
            card.addSpellAbility(spell);
            
            spell.setBeforePayMana(CardFactoryUtil.input_targetPlayer(spell));
        }//*************** END ************ END **************************
        
        
        //*************** START *********** START **************************
        else if(cardName.equals("Goblin Charbelcher")) {
        	Ability_Cost abCost = new Ability_Cost("3 T", cardName, true);
            final Ability_Activated ability = new Ability_Activated(card, abCost, new Target("TgtCP")) {
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
                            if (crd.isLand())
                            	damage--;
                            
                            if(crd.getName().equals("Mountain")) 
                            	damage *= 2;
                        }
                    }//while
                    AllZone.Display.getChoiceOptional("Revealed cards:", revealed.toArray());
                    
                    if(getTargetCard() != null) {
                        if(AllZone.GameAction.isCardInPlay(getTargetCard())
                                && CardFactoryUtil.canTarget(card, getTargetCard())) {
                            getTargetCard().addDamage(damage, card);
                        }
                    } else getTargetPlayer().addDamage(damage, card);
                }
            };
            
            StringBuilder sb = new StringBuilder();
            sb.append(abCost);
            sb.append("Reveal cards from the top of your library until you reveal a land card. Goblin Charbelcher deals damage equal ");
            sb.append("to the number of nonland cards revealed this way to target creature or player. If the revealed land card was a Mountain, ");
            sb.append("Goblin Charbelcher deals double that damage instead. Put the revealed cards on the bottom of your library in any order.");
            ability.setDescription(sb.toString());

            ability.setChooseTargetAI(CardFactoryUtil.AI_targetHuman());
            card.addSpellAbility(ability);
        }//*************** END ************ END **************************

        
        //*************** START *********** START **************************
        else if(cardName.equals("Time Vault")) {
        	final Ability_Cost abCost = new Ability_Cost("T", cardName, true);

        	final Ability_Activated ability = new Ability_Activated(card, abCost, null){
                private static final long serialVersionUID = 5784473766585071504L;

                @Override
                public void resolve() {
                    AllZone.Phase.addExtraTurn(card.getController());
                }
            };
            card.addSpellAbility(ability);
            
            StringBuilder sb = new StringBuilder();
            sb.append(card).append(" - take an extra turn after this one.");
            ability.setStackDescription(sb.toString());
            
            ability.setDescription("Tap: Take an extra turn after this one.");
        }//*************** END ************ END **************************
                 
        
        //*************** START *********** START **************************
        else if(cardName.equals("Illusions of Grandeur")) {
            final SpellAbility gainLife = new Ability(card, "0") {
                @Override
                public void resolve() {
                    Card c = card;
                    c.getController().gainLife(20, card);
                }
            };
            
            final SpellAbility loseLife = new Ability(card, "0") {
                @Override
                public void resolve() {
                    Card c = card;
                    c.getController().loseLife(20, card);
                }
            };
            
            Command intoPlay = new Command() {
                private static final long serialVersionUID = 502892931516451254L;
                
                public void execute() {
                	
                	StringBuilder sb = new StringBuilder();
                	sb.append(card.getController()).append(" gains 20 life");
                	gainLife.setStackDescription(sb.toString());
                    
                    AllZone.Stack.add(gainLife);
                }
            };
            
            Command leavesPlay = new Command() {
                private static final long serialVersionUID = 5772999389072757369L;
                
                public void execute() {
                	
                	StringBuilder sb = new StringBuilder();
                	sb.append(card.getController()).append(" loses 20 life");
                	loseLife.setStackDescription(sb.toString());
                    
                    AllZone.Stack.add(loseLife);
                }
            };
            
            card.addComesIntoPlayCommand(intoPlay);
            card.addLeavesPlayCommand(leavesPlay);
            
        }//*************** END ************ END **************************       
  
	  
      //*************** START *********** START **************************
      else if(cardName.equals("Isochron Scepter"))
      {
    	  Ability_Cost abCost = new Ability_Cost("2 T", cardName, true);
    	  final Ability_Activated freeCast = new Ability_Activated(card, abCost, null)
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
				if(getSourceCard().getController().equals(AllZone.ComputerPlayer))
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
          freeCast.setDescription(abCost+"You may copy the exiled card. If you do, you may cast the copy without paying its mana cost");
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
                      AllZone.GameAction.moveTo(AllZone.Human_Exile, c);
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
                  if(card.getController().equals(AllZone.HumanPlayer)) {
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
                      AllZone.GameAction.moveTo(AllZone.Computer_Exile, c);
                      card.attachCard(c);
                  }//else
              }//resolve()
          };//SpellAbility
          Command intoPlay = new Command() {
              private static final long serialVersionUID = 9202753910259054021L;
              
              public void execute() {
            	  
            	  StringBuilder sb = new StringBuilder();
            	  sb.append("Imprint - ").append(card.getController());
            	  sb.append(" may exile an instant card with converted mana cost 2 or less from their hand.");
            	  ability.setStackDescription(sb.toString());
                  
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
      	else if(cardName.equals("Bottle of Suleiman")) {
      		/*
      		 * 1, Sacrifice Bottle of Suleiman: Flip a coin. If you lose the flip,
      		 * Bottle of Suleiman deals 5 damage to you. If you win the flip,
      		 * put a 5/5 colorless Djinn artifact creature token with flying
      		 * onto the battlefield.
      		 */
      		Ability_Cost abCost = new Ability_Cost("1 Sac<1/CARDNAME>", cardName, true);
      		final SpellAbility ability = new Ability_Activated(card, abCost, null) {
      			private static final long serialVersionUID = -5741302550353410000L;

      			@Override
      			public boolean canPlayAI() {
      				if( AllZone.ComputerPlayer.getLife() > 10 ) {
      					return true;
      				}
      				CardList play = new CardList(AllZone.Computer_Battlefield.getCards());
      				play = play.getType("Creature");
      				if( play.size() == 0 ) {
      					return true;
      				}
      				return false;
      			}

      			@Override
      			public void resolve() {
      				if(GameActionUtil.flipACoin(card.getController(), card)) {
      					CardFactoryUtil.makeToken("Djinn", "C 5 5 Djinn", card.getController(), "", new String[] {"Creature", "Artifact", "Djinn"}, 5, 5, new String[] {"Flying"});
      				}
      				else {
      					card.getController().addDamage(5, card);
      				}
      			}
      		};//SpellAbility

      		card.addSpellAbility(ability);
      		ability.setDescription(abCost+"Flip a coin. If you lose the flip, Bottle of Suleiman deals 5 damage to you. If you win the flip, put a 5/5 colorless Djinn artifact creature token with flying onto the battlefield.");
      		ability.setStackDescription("Bottle of Suleiman - flip a coin");
      	}//*************** END ************ END **************************
        
      
        //*************** START *********** START **************************
        else if(cardName.equals("Lodestone Bauble")) {
        	/* 1, Tap, Sacrifice Lodestone Bauble: Put up to four target basic
        	 * land cards from a player's graveyard on top of his or her library
        	 * in any order. That player draws a card at the beginning of the next
        	 * turn's upkeep.
        	 */

        	Ability_Cost abCost = new Ability_Cost("1 T Sac<1/CARDNAME>", cardName, true);
        	Target target = new Target("Select target player", new String[]{"Player"});
        	final Ability_Activated ability = new Ability_Activated(card, abCost, target) {
        		private static final long serialVersionUID = -6711849408085138636L;

        		@Override
        		public boolean canPlayAI() {
        			return getComputerLands().size() >= 4;
        		}

        		@Override
        		public void chooseTargetAI() {
        			setTargetPlayer(AllZone.ComputerPlayer);
        		}//chooseTargetAI()

        		@Override
        		public void resolve() {
        			final int limit = 4;   //at most, this can target 4 cards
        			final Player player = getTargetPlayer();
        			PlayerZone grave = AllZone.getZone(Constant.Zone.Graveyard, player);
        			PlayerZone lib = AllZone.getZone(Constant.Zone.Library, player);

        			CardList lands = new CardList(grave.getCards());
        			lands = lands.filter(AllZoneUtil.basicLands);
        			if(card.getController().equals(AllZone.HumanPlayer)){ 
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
        			
        			/*
        			 * TODO - this draw is at End of Turn.  It should be at the beginning of next
        			 * upkeep when a mechanism is in place
        			 */
        			final Command draw = new Command() {
        				private static final long serialVersionUID = 8293374203043368969L;

        				public void execute() {
        					getTargetPlayer().drawCard();
        				}
        			};
        			AllZone.EndOfTurn.addAt(draw);
        		}

        		private CardList getComputerLands() {
        			CardList list = new CardList(AllZone.Computer_Graveyard.getCards());
        			//probably no need to sort the list...
        			return list.filter(AllZoneUtil.basicLands);
        		}
        	};//ability

        	ability.setDescription(abCost+"Put up to four target basic land cards from a player's graveyard on top of his or her library in any order. That player draws a card at the beginning of the next turn's upkeep.");
        	card.addSpellAbility(ability);
        }//*************** END ************ END **************************
     
        
        //*************** START *********** START **************************
        else if(cardName.equals("Grindstone")) {
        	Target target = new Target("Select target player", new String[] {"Player"});
        	Ability_Cost abCost = new Ability_Cost("3 T", cardName, true);
        	Ability_Activated ab1 = new Ability_Activated(card, abCost, target) {
				private static final long serialVersionUID = -6281219446216L;

				@Override
        		public boolean canPlayAI() {
        			CardList libList = AllZoneUtil.getPlayerCardsInLibrary(AllZone.HumanPlayer);
        			//CardList list = AllZoneUtil.getCardsInPlay("Painter's Servant");
        			return libList.size() > 0;// && list.size() > 0;
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
        	ab1.setDescription(abCost+"Put the top two cards of target player's library into that player's graveyard. If both cards share a color, repeat this process.");
        	card.addSpellAbility(ab1);
        }//*************** END ************ END **************************
        
        
        //*************** START *********** START **************************
        else if(cardName.equals("Keening Stone")) {
        	/*
        	 * 5, Tap: Target player puts the top X cards of his or her
        	 * library into his or her graveyard, where X is the number of
        	 * cards in that player's graveyard.
        	 */
        	Target target = new Target("Select target player", new String[] {"Player"});
        	Ability_Cost abCost = new Ability_Cost("5 T", cardName, true);
        	Ability_Activated ab1 = new Ability_Activated(card, abCost, target) {
				private static final long serialVersionUID = -6282104343089446216L;

				@Override
        		public boolean canPlayAI() {
					Player player = getTargetPlayer();
        			PlayerZone lib = AllZone.getZone(Constant.Zone.Library, player);
        			CardList libList = new CardList(lib.getCards());
        			return libList.size() > 0;
        		}

        		@Override
        		public void resolve() {
        			Player player = getTargetPlayer();
        			player.mill(AllZone.getZone(Constant.Zone.Graveyard, player).size());
        		}
        	};
        	ab1.setChooseTargetAI(CardFactoryUtil.AI_targetHuman());
        	ab1.setDescription(abCost+"Target player puts the top X cards of his or her library into his or her graveyard, where X is the number of cards in that player's graveyard.");
        	card.addSpellAbility(ab1);
        }//*************** END ************ END **************************
        
        
        //*************** START *********** START **************************
        else if(cardName.equals("Glasses of Urza")) {
        	Target target = new Target("Select target player", new String[] {"Player"});
        	Ability_Cost abCost = new Ability_Cost("T", cardName, true);
            final Ability_Activated ability = new Ability_Activated(card, abCost, target) {
                private static final long serialVersionUID = -3857979945891501990L;

                @Override
                public void resolve() {
                    final Player player = getTargetPlayer();
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

            ability.setDescription(abCost+"Look at target player's hand.");
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
                    card.getController().drawCard();
                }
            };
            Command destroy = new Command() {
				private static final long serialVersionUID = 7982507967024313067L;

				public void execute() {
					
					StringBuilder sb = new StringBuilder();
					sb.append(card.getName()).append(" - ").append(card.getController()).append(" draws a card");
					ability.setStackDescription(sb.toString());
                    
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
        	Ability_Cost abCost = new Ability_Cost("T Exile<1/CARDNAME>", cardName, true);
        	final Ability_Activated ability = new Ability_Activated(card, abCost, null) {
				private static final long serialVersionUID = -1299603105585632846L;

				@Override
        		public void resolve() {
        			final Player player = card.getController();
        			CardList grave = AllZoneUtil.getPlayerGraveyard(player);
        			PlayerZone lib = AllZone.getZone(Constant.Zone.Library, player);
        			
        			for(Card c:grave) {
        				lib.add(c);
        			}
        			AllZone.getZone(Constant.Zone.Graveyard, player).reset();
        			player.shuffle();
        		}

        		@Override
        		public boolean canPlayAI() {
        			CardList lib = AllZoneUtil.getPlayerCardsInLibrary(AllZone.ComputerPlayer);
        			return lib.size() < 5;
        		}

        	};//SpellAbility
        	
        	StringBuilder sb = new StringBuilder();
        	sb.append(cardName).append(" - Player shuffles grave into library.");
        	ability.setStackDescription(sb.toString());
        	ability.setDescription(abCost+"Shuffle your graveyard into your library.");
        	card.addSpellAbility(ability);
        }//*************** END ************ END **************************
        
        
        //*************** START *********** START **************************
        else if(cardName.equals("Elixir of Immortality")) {
        	/*
        	 * 2, Tap: You gain 5 life. Shuffle Elixir of Immortality and your graveyard into your library.
        	 */
        	Ability_Cost abCost = new Ability_Cost("2 T", cardName, true);
        	final Ability_Activated ability = new Ability_Activated(card, abCost, null) {
				private static final long serialVersionUID = -1299603105585632846L;

				@Override
        		public void resolve() {
        			final Player player = card.getController();
        			CardList grave = AllZoneUtil.getPlayerGraveyard(player);
        			PlayerZone lib = AllZone.getZone(Constant.Zone.Library, player);
        			AllZone.GameAction.moveToLibrary(card);
        			
        			for(Card c:grave) {
        				lib.add(c);
        			}
        			AllZone.getZone(Constant.Zone.Graveyard, player).reset();
        			player.shuffle();
        			player.gainLife(5, card);
        		}

        		@Override
        		public boolean canPlayAI() {
        			PlayerZone lib = AllZone.getZone(Constant.Zone.Library, AllZone.ComputerPlayer);
        			return lib.size() < 5 || AllZone.ComputerPlayer.getLife() < 3;
        		}

        	};//SpellAbility
        	
        	StringBuilder sb = new StringBuilder();
        	sb.append(cardName).append(" - Player shuffles grave into library.");
        	ability.setStackDescription(sb.toString());
        	ability.setDescription(abCost+"You gain 5 life. Shuffle Elixir of Immortality and your graveyard into your library.");
        	card.addSpellAbility(ability);
        }//*************** END ************ END **************************


        //*************** START *********** START **************************
        else if(cardName.equals("Curse of Wizardry")) {
            final Ability ability = new Ability(card, "0") {
                @Override
                public void resolve() {
                    if(card.getController().equals(AllZone.HumanPlayer)) {
                        
                        String color = "";
                        String[] colors = Constant.Color.Colors;
                        colors[colors.length - 1] = null;
                        
                        Object o = AllZone.Display.getChoice("Choose color", colors);
                        color = (String) o;
                        card.setChosenColor(color);
                    } else {
                        CardList list = AllZoneUtil.getPlayerCardsInLibrary(AllZone.HumanPlayer);
                        list.add(AllZoneUtil.getPlayerHand(AllZone.HumanPlayer));
                        
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
            
            StringBuilder sb = new StringBuilder();
            sb.append("As ").append(cardName).append(" enters the battlefield, choose a color.");
            ability.setStackDescription(sb.toString());
            
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
					
					StringBuilder sb = new StringBuilder();
					sb.append("When ").append(card.getName()).append(" enters the battlefield, tap all Legendary creatures.");
					ability.setStackDescription(sb.toString());
        			
        			AllZone.Stack.add(ability);
        		}
        	};
        	card.addComesIntoPlayCommand(intoPlay);
        }//*************** END ************ END **************************
        
       
        //*************** START *********** START **************************
        else if(cardName.equals("Tormod's Crypt")) {
        	/*
        	 * Tap, Sacrifice Tormod's Crypt: Exile all cards from target player's graveyard.
        	 */
        	Target target = new Target("Select target player", new String[] {"Player"});
        	Ability_Cost abCost = new Ability_Cost("T Sac<1/CARDNAME>", cardName, true);
        	final Ability_Activated ability = new Ability_Activated(card, abCost, target) {

				private static final long serialVersionUID = -8877371657709894494L;

				@Override
        		public void resolve() {
					if (card.getController().equals(AllZone.ComputerPlayer))
						setTargetPlayer(AllZone.HumanPlayer);
					
        			final Player player = getTargetPlayer();
        			CardList grave = AllZoneUtil.getPlayerGraveyard(player);
        			
        			for(Card c:grave) {
        				AllZone.GameAction.exile(c);
        			}
        			AllZone.getZone(Constant.Zone.Graveyard, player).reset();
        			player.shuffle();
        		}

        		@Override
        		public boolean canPlayAI() {
        			PlayerZone grave = AllZone.getZone(Constant.Zone.Library, AllZone.HumanPlayer);
        			return grave.size() < 15;
        		}

        	};//SpellAbility
        	ability.setDescription(abCost+"Exile all cards from target player's graveyard.");
        	card.addSpellAbility(ability);
        }//*************** END ************ END **************************
        
        
        //*************** START *********** START **************************
        else if(cardName.equals("Journey to Nowhere")) {

        	final CommandReturn getPerm = new CommandReturn() {
        		public Object execute() {
        			CardList tempList = AllZoneUtil.getCreaturesInPlay();

        			CardList list = new CardList();

        			for(int i = 0; i < tempList.size(); i++) {
        				if(CardFactoryUtil.canTarget(card, tempList.get(i))) list.add(tempList.get(i));
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
        				AllZone.GameAction.exile(getTargetCard());
        			}//if
        		}//resolve()
        	};//abilityComes

        	final Input inputComes = new Input() {
        		private static final long serialVersionUID = -3613946694360326887L;

        		@Override
        		public void showMessage() {
        			CardList choice = (CardList) getPerm.execute();

        			stopSetNext(CardFactoryUtil.input_targetSpecific(abilityComes, choice,
        					"Select target creature to exile", true, false));
        			ButtonUtil.disableAll();//to disable the Cancel button
        		}//showMessage
        	};//inputComes
        	
        	Command commandComes = new Command() {
        		private static final long serialVersionUID = -6250376920501373535L;

        		public void execute() {
        			CardList perm = (CardList) getPerm.execute();
        			Player s = card.getController();
        			if(perm.size() == 0) return;
        			else if(s.equals(AllZone.HumanPlayer)) AllZone.InputControl.setInput(inputComes);
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
        					tempList.addAll(AllZone.Human_Battlefield.getCards());

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
        					humanPerms.addAll(AllZone.Human_Battlefield.getCards());
        					humanPerms = humanPerms.filter(new CardListFilter() {
        						public boolean addCard(Card c) {
        							return c.isPermanent() && !c.isLand() && CardFactoryUtil.canTarget(card, c);
        						}
        					});

        					if(humanPerms.size() > 0) target = humanPerms.get(0);
        				}//if

        				if(target == null) {
        					//must target computer creature
        					CardList computer = new CardList(AllZone.Computer_Battlefield.getCards());
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
        			if(o == null || ((Card) o).isToken() || !AllZone.GameAction.isCardExiled((Card) o)) return;

        			SpellAbility ability = new Ability(card, "0") {

        				@Override
        				public void resolve() {
        					//copy card to reset card attributes like attack and defense
        					Card c = abilityComes.getTargetCard();
        					if(!c.isToken()) {
        						c = AllZone.CardFactory.dynamicCopyCard(c);
        						c.setController(c.getOwner());

        						PlayerZone play = AllZone.getZone(Constant.Zone.Battlefield, c.getOwner());
        						PlayerZone removed = AllZone.getZone(Constant.Zone.Exile, c.getOwner());
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

        			CardList cList = new CardList(AllZone.Human_Battlefield.getCards());
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
        else if(cardName.equals("Mirror Universe")) {
        	/*
        	 * Tap, Sacrifice Mirror Universe: Exchange life totals with
        	 * target opponent. Activate this ability only during your upkeep.
        	 */
        	Ability_Cost abCost = new Ability_Cost("T Sac<1/CARDNAME>", cardName, true);
            final Ability_Activated ability = new Ability_Activated(card, abCost, null) {
        		private static final long serialVersionUID = -1409850598108909739L;

        		@Override
        		public void resolve() {
        			Player player = card.getController();
        			Player opponent = player.getOpponent();
        			int tmp = player.getLife();
        			player.setLife(opponent.getLife(), card);
        			opponent.setLife(tmp, card);
        		}

        		@Override
        		public boolean canPlay() {
        			return super.canPlay() && AllZone.Phase.getPhase().equals(Constant.Phase.Upkeep)
        				&& AllZone.Phase.getPlayerTurn().equals(card.getController());
        		}

        		@Override
        		public boolean canPlayAI() {
        			if(AllZone.ComputerPlayer.getLife() < 5 && AllZone.HumanPlayer.getLife() > 5) {
        				return true;
        			}
        			else if(AllZone.ComputerPlayer.getLife() == 1) {
        				return true;
        			}
        			else if((AllZone.HumanPlayer.getLife() - AllZone.ComputerPlayer.getLife()) > 10) {
        				return true;
        			}
        			else return false;
        		}
        	};//SpellAbility
        	
        	StringBuilder sb = new StringBuilder();
        	sb.append(cardName).append(" - Exchange life totals with target opponent.");
        	ability.setStackDescription(sb.toString());
        	
        	ability.setDescription(abCost+"Exchange life totals with target opponent. Activate this ability only during your upkeep.");
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
            
            StringBuilder sb = new StringBuilder();
            sb.append(card).append(" becomes a 3/6 Golem creature until End of Combat");
            a1.setStackDescription(sb.toString());
            
            Command paid1 = new Command() {
				private static final long serialVersionUID = 1531378274457977155L;

				public void execute() {
                    AllZone.Stack.add(a1);
                }
            };
            
            a1.setBeforePayMana(new Input_PayManaCost_Ability(a1.getManaCost(), paid1));
        }//*************** END ************ END **************************

        //*************** START *********** START **************************
        else if (cardName.equals("An-Zerrin Ruins")) {
        	
        	final Ability_Static comesIntoPlayAbility = new Ability_Static(card, "0") {
                @Override
                public void resolve() {
                	String chosenType = "";
        			if(card.getController().equals(AllZone.HumanPlayer)) {
        				chosenType = JOptionPane.showInputDialog(null, "Enter a creature type:", card.getName(),
        						JOptionPane.QUESTION_MESSAGE);
        			}
        			else {
        				//not implemented for AI
        			}
        			if (!CardUtil.isACreatureType(chosenType)) chosenType = "";
        			card.setChosenType(chosenType);
                }//resolve()
            }; //comesIntoPlayAbility
            
            Command intoPlay = new Command() {
				private static final long serialVersionUID = 2985015252466920757L;

				public void execute() {
					
					StringBuilder sb = new StringBuilder();
					sb.append(card.getName()).append(" - choose a creature type. Creatures of that type do not untap during their controller's untap step.");
					comesIntoPlayAbility.setStackDescription(sb.toString());
                	
                	AllZone.Stack.add(comesIntoPlayAbility);
                }
            };
            
            card.addComesIntoPlayCommand(intoPlay);
        }//*************** END ************ END **************************
        
        
        //*************** START *********** START **************************
        else if(cardName.equals("Barl's Cage")) {
            final String Tgts[] = {"Creature"};
        	Target target= new Target("Select target creature.", Tgts, "1", "1");

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
                    CardList tappedCreatures = AllZoneUtil.getCreaturesInPlay(AllZone.HumanPlayer);
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
        
        //*************** START ************ START **************************
        else if(cardName.equals("Black Mana Battery") || cardName.equals("Blue Mana Battery")
        		|| cardName.equals("Green Mana Battery") || cardName.equals("Red Mana Battery")
        		|| cardName.equals("White Mana Battery")) {
        	final int[] num = new int[1];
        	String name[] = cardName.split(" ");
        	final String shortString = Input_PayManaCostUtil.getShortColorString(name[0].trim().toLowerCase());
        	StringBuilder desc = new StringBuilder();
        	desc.append("tap, Remove any number of charge counters from ");
        	desc.append(cardName);
        	desc.append(": Add ");
        	desc.append(shortString);
        	desc.append(" to your mana pool, then add an additional ");
        	desc.append(shortString);
        	desc.append(" to your mana pool for each charge counter removed this way.");
            
            final Ability_Mana addMana = new Ability_Mana(card, desc.toString()) {
            	private static final long serialVersionUID = -5356224416791741957L;

				@Override
                public void undo() {
                    card.addCounter(Counters.CHARGE, num[0]);
                    card.untap();
                }
                
				//@Override
                public String mana() {
                	StringBuilder mana = new StringBuilder();
                	mana.append(shortString);
                	for(int i = 0; i < num[0]; i++) {
                		mana.append(" ").append(shortString);
                	}
                    return mana.toString();
                }
                
                @Override
                public void resolve() {
                    card.subtractCounter(Counters.CHARGE, num[0]);
                    card.tap();
                    super.resolve();
                }
            };
            
            Input runtime = new Input() {
				private static final long serialVersionUID = -8808673510875540608L;

				@Override
                public void showMessage() {
					num[0] = card.getCounters(Counters.CHARGE);
                	String[] choices = new String[num[0]+1];
                	for(int j=0;j<=num[0];j++) {
                		choices[j] = ""+j;
                	}
                    String answer = (String)(AllZone.Display.getChoiceOptional(
                            "Charge counters to remove", choices));
                    num[0] = Integer.parseInt(answer);
                    AllZone.Stack.add(addMana);
                    stop();
                }
            };
            
            addMana.setBeforePayMana(runtime);
            card.addSpellAbility(addMana);
        }//*************** END ************ END **************************
        
        //*************** START ************ START **************************
        else if(cardName.equals("Magistrate's Scepter")) {
        	Ability_Cost abCost = new Ability_Cost("T SubCounter<3/CHARGE>", cardName, true);
            final Ability_Activated addTurn = new Ability_Activated(card, abCost, null) {
				private static final long serialVersionUID = -8712180600748576359L;
				
                @Override
                public void resolve() {
                    AllZone.Phase.addExtraTurn(card.getController());
                }
            };
            addTurn.setDescription("tap, Remove three charge counters from Magistrate's Scepter: Take an extra turn after this one.");
            
            StringBuilder sb = new StringBuilder();
            sb.append(cardName).append(" - take an extra turn after this one.");
            addTurn.setStackDescription(sb.toString());
            
            card.addSpellAbility(addTurn);
        }//*************** END ************ END **************************
        
        //*************** START ************ START **************************
        else if(cardName.equals("Standing Stones")) {
        	/*
        	 * 1, Tap, Pay 1 life: Add one mana of any color to your mana pool.
        	 */
        	Ability_Cost abCost = new Ability_Cost("1 T PayLife<1>", cardName, true);
        	Ability_Activated mana = new Ability_Activated(card, abCost, null) {
				private static final long serialVersionUID = -5393697921811242255L;

				@Override
        		public void resolve() {
        			String color = "";

        			Object o = AllZone.Display.getChoice("Choose mana color", Constant.Color.Colors);
        			color = (String) o;

        			if(color.equals("white")) color = "W";
        			else if(color.equals("blue")) color = "U";
        			else if(color.equals("black")) color = "B";
        			else if(color.equals("red")) color = "R";
        			else if(color.equals("green")) color = "G";

        			Card mp = AllZone.ManaPool;
        			mp.addExtrinsicKeyword("ManaPool:" + color);
        		}
        	};
        	
        	StringBuilder sbDesc = new StringBuilder();
        	sbDesc.append(abCost).append("Add one mana of any color to your mana pool.");
        	mana.setDescription(sbDesc.toString());
        	
        	StringBuilder sbStack = new StringBuilder();
        	sbStack.append(cardName).append(" - add one mana of any color to your mana pool.");
        	mana.setStackDescription(sbStack.toString());
        	
        	card.addSpellAbility(mana);
        }//*************** END ************ END **************************
        
        
        //*************** START *********** START **************************
        else if(cardName.equals("Sorcerer's Strongbox")) {
        	/*
        	 * 2, Tap: Flip a coin. If you win the flip, sacrifice Sorcerer's
        	 * Strongbox and draw three cards.
        	 */
        	Ability_Cost abCost = new Ability_Cost("2 T", cardName, true);
        	final SpellAbility ability = new Ability_Activated(card, abCost, null) {
        		private static final long serialVersionUID = 5152381570537520053L;

        		@Override
        		public void resolve() {
        			if( GameActionUtil.flipACoin(card.getController(), card)) {
        				AllZone.GameAction.sacrifice(card);
        				card.getController().drawCards(3);
        			}
        			else {
        				//do nothing
        			}
        		}
        	};//SpellAbility

        	card.addSpellAbility(ability);
        	
        	StringBuilder sbDesc = new StringBuilder();
        	sbDesc.append(abCost).append("Flip a coin. If you win the flip, sacrifice Sorcerer's Strongbox and draw three cards.");
        	ability.setDescription(sbDesc.toString());
        	
        	StringBuilder sbStack = new StringBuilder();
        	sbStack.append(card.getName()).append(" - flip a coin");
        	ability.setStackDescription(sbStack.toString());
        }//*************** END ************ END **************************
        
        
        //*************** START *********** START **************************
        else if (cardName.equals("Wrath of Marit Lage")) {
        	/*
        	 * When Wrath of Marit Lage enters the battlefield, tap all red creatures.
        	 */
        	final Ability comesIntoPlayAbility = new Ability(card, "0") {
                @Override
                public void resolve() {
                	CardList red = AllZoneUtil.getCreaturesInPlay();
                	red = red.filter(AllZoneUtil.red);
                	for(Card c:red) c.tap();
                }//resolve()
            }; //comesIntoPlayAbility
            
            Command intoPlay = new Command() {
				private static final long serialVersionUID = -8002808964908985221L;

				public void execute() {
					
					StringBuilder sb = new StringBuilder();
					sb.append(card.getName()).append(" - tap all red creatures.");
					comesIntoPlayAbility.setStackDescription(sb.toString());
                	
                	AllZone.Stack.add(comesIntoPlayAbility);
                }
            };
            
            card.addComesIntoPlayCommand(intoPlay);
        }//*************** END ************ END **************************
        
        //*************** START *********** START **************************
        else if(cardName.equals("Time Bomb")){
        	/*
        	 * 1, Tap, Sacrifice Time Bomb: Time Bomb deals damage equal
        	 * to the number of time counters on it to each creature and
        	 * each player.
        	 */
        	Ability_Cost abCost = new Ability_Cost("1 T Sac<1/CARDNAME>", cardName, true);
        	final Ability_Activated ability = new Ability_Activated(card, abCost, null) {
        		private static final long serialVersionUID = 7550743617522146304L;
        		
        		@Override
        		public void resolve() {
        			int damage = card.getCounters(Counters.TIME);
        			CardList all = AllZoneUtil.getCreaturesInPlay();

        			for(Card c:all) c.addDamage(damage, card);

        			AllZone.HumanPlayer.addDamage(damage, card);
        			AllZone.ComputerPlayer.addDamage(damage, card);
        		}
        		
        		@Override
        		public boolean canPlayAI() {
        			final int damage = card.getCounters(Counters.TIME);

        			if (AllZone.HumanPlayer.getLife() <= damage) return true;

        			CardListFilter filter = new CardListFilter() {
        				public boolean addCard(Card c) {
        					return c.isCreature() && CardFactoryUtil.canDamage(card, c) && damage >= (c.getNetDefense() + c.getDamage());
        				}
        			};

        			CardList human = AllZoneUtil.getPlayerCardsInPlay(AllZone.HumanPlayer);
        			human = human.filter(filter);

        			CardList comp = AllZoneUtil.getPlayerCardsInPlay(AllZone.ComputerPlayer);
        			comp = comp.filter(filter);

        			return human.size() > (comp.size() + 2) && AllZone.ComputerPlayer.getLife() > damage + 3;
        		}
        	};

        	StringBuilder sbStack = new StringBuilder();
        	sbStack.append(card).append(" - deals X damage to each creature and each player.");
        	ability.setStackDescription(sbStack.toString());
        	
        	ability.setDescription(abCost+cardName+" deals damage equal to the number of time counters on it to each creature and each player.");

        	card.addSpellAbility(ability);
        }//*************** END ************ END **************************
        
        //*************** START *********** START **************************
        else if(cardName.equals("Pithing Needle")) {
        	final CardFactory factory = this;
        	final SpellAbility ability = new Ability_Static(card, "0") {
                @Override
                public void resolve() {
                	 final String[] input = new String[1];
                	 CardList allCards = factory.getAllCards();
                	 input[0] = "";
                    if(card.getController().equals(AllZone.HumanPlayer)) {
                    	while(input[0] == "")
                    	{
                    		input[0] = JOptionPane.showInputDialog(null, "Which source?", "Pick a card",JOptionPane.QUESTION_MESSAGE);
                        
                    		
                        	CardList cards = allCards.filter(new CardListFilter() {
                        		public boolean addCard(Card c) {
                        			//System.out.print("Comparing \"" + c.getName().toLowerCase() + "\" to \"" + input[0] + "\": ");
                        			//System.out.println((c.getName().toLowerCase().equals(input[0].toLowerCase())));
                        			return c.getName().toLowerCase().equals(input[0].toLowerCase());
                        		}
                        	});
                        	
                        	if(cards.size() == 0) {
                        		input[0] = "";
                        	}
                        	else {
                        		input[0] = cards.get(0).getName();
                        	}
                    	}
                        //TODO: some more input validation, case-sensitivity, etc.
                        
                    } else {
                        //AI CODE WILL EVENTUALLY GO HERE!
                    }
                    card.setSVar("PithingTarget", input[0]);
                    card.setChosenType(input[0]);
                }
            };//ability
            ability.setStackDescription("As Pithing Needle enters the battlefield, name a card.");
        	Command intoPlay = new Command() {

				private static final long serialVersionUID = 2266471224097876143L;

				public void execute() {
        			AllZone.Stack.add(ability);
        		}
        	};
        	
        	Command leavesPlay = new Command() {

				private static final long serialVersionUID = 7079781778752377760L;

				public void execute() {
        			card.setSVar("Pithing Target", "");
        		}
        	};
        	
        	card.addComesIntoPlayCommand(intoPlay);
        	card.addLeavesPlayCommand(leavesPlay);
        }//*************** END ************ END **************************
        
        //*************** START *********** START **************************
        else if(cardName.equals("Bazaar of Wonders")) {
        	/*
        	 * When Bazaar of Wonders enters the battlefield, exile all cards from all graveyards.
        	 */
            Command intoPlay = new Command() {
				private static final long serialVersionUID = 9209706681167017765L;

				public void execute() {
                	CardList hGrave = AllZoneUtil.getPlayerGraveyard(AllZone.HumanPlayer);
                	CardList cGrave = AllZoneUtil.getPlayerGraveyard(AllZone.ComputerPlayer);
                	
                	for(Card c:hGrave) AllZone.GameAction.exile(c);
                	for(Card c:cGrave) AllZone.GameAction.exile(c);
                }
            };
            card.addComesIntoPlayCommand(intoPlay);          
        }//*************** END ************ END **************************
        
        //*************** START *********** START **************************
        else if(cardName.equals("Phyrexian Processor")) {
            final SpellAbility ability = new Ability_Static(card, "0") {
                @Override
                public void resolve() {
                	Player player = card.getController();
                	int lifeToPay = 0;
                    if(player.isHuman()) {
                    	int num = card.getController().getLife();
                    	String[] choices = new String[num+1];
                    	for(int j = 0; j <= num; j++) {
                    		choices[j] = ""+j;
                    	}
                        String answer = (String)(AllZone.Display.getChoiceOptional(
                                "Life to pay:", choices));
                        lifeToPay = Integer.parseInt(answer);
                    } else {
                        //not implemented for Compy
                    }
                    
                    if(player.payLife(lifeToPay, card)) card.setXLifePaid(lifeToPay);
                }
            };//ability
            Command intoPlay = new Command() {
                private static final long serialVersionUID = 5634360316643996274L;
                
                public void execute() {
                	
                	StringBuilder sb = new StringBuilder();
                	sb.append("As ").append(card.getName()).append(" enters the battlefield, pay any amount of life.");
                	ability.setStackDescription(sb.toString());
                    
                    AllZone.Stack.add(ability);
                }
            };
            card.addComesIntoPlayCommand(intoPlay);
        }//*************** END ************ END **************************
        
        
        //*************** START *********** START **************************
        else if(cardName.equals("Scroll Rack")) {
        	Ability_Cost abCost = new Ability_Cost("1 T", cardName, true); 
            final Ability_Activated ability = new Ability_Activated(card, abCost, null) {
				private static final long serialVersionUID = -5588587187720068547L;

				@Override
        		public void resolve() {
            		//not implemented for compy
        			if(card.getController().isHuman()) {
        				AllZone.InputControl.setInput(new Input() {
							private static final long serialVersionUID = -2305549394512889450L;
							CardList exiled = new CardList();

        					@Override
        					public void showMessage() {
        						AllZone.Display.showMessage(card.getName()+" - Exile cards from hand.  Currently, "+exiled.size()+" selected.  (Press OK when done.)");
        						ButtonUtil.enableOnlyOK();
        					}

        					@Override
        					public void selectButtonOK() { done(); }

        					@Override
        					public void selectCard(final Card c, PlayerZone zone) {
        						if(zone.is(Constant.Zone.Hand, AllZone.HumanPlayer)
        								&& !exiled.contains(c)) {
        							exiled.add(c);
        							showMessage();
        						}
        					}
        					
        					public void done() {
        						//exile those cards
        						for(Card c:exiled) AllZone.GameAction.exile(c);
        						
        						//Put that many cards from the top of your library into your hand.
        						//Ruling: This is not a draw...
        						PlayerZone lib = AllZone.getZone(Constant.Zone.Library, AllZone.HumanPlayer);
        						int numCards = 0;
        						while(lib.size() > 0 && numCards < exiled.size()) {
        							AllZone.GameAction.moveToHand(lib.get(0));
        							numCards++;
        						}
        						
        						AllZone.Display.showMessage(card.getName()+" - Returning cards to top of library.");
        						
        						//Then look at the exiled cards and put them on top of your library in any order.
        						while(exiled.size() > 0) {
        							Object o = AllZone.Display.getChoice("Put a card on top of your library.", exiled.toArray());
        							Card c1 = (Card)o;
        							AllZone.GameAction.moveToTopOfLibrary(c1);
        							exiled.remove(c1);
        						}
        						
    							stop();
        					}
        				});
        			}
        		}
				
				@Override
				public boolean canPlayAI() {
					return false;
				}
            };//ability
            ability.setDescription(abCost+"Exile any number of cards from your hand face down. Put that many cards from the top of your library into your hand. Then look at the exiled cards and put them on top of your library in any order.");
            ability.setStackDescription(cardName+" - exile any number of cards from your hand.");
            card.addSpellAbility(ability);
        }//*************** END ************ END **************************
        

        return postFactoryKeywords(card);
    }//getCard2
    
    public Card postFactoryKeywords(final Card card){
    	// this function should handle any keywords that need to be added after a spell goes through the factory
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
        
        if (hasKeyword(card, "Flashback") != -1) {
            int n = hasKeyword(card, "Flashback");
            if (n != -1) {
                String parse = card.getKeyword().get(n).toString();
                //card.removeIntrinsicKeyword(parse);
                
                String k[] = parse.split(":");
                
                card.setFlashback(true);
                card.addSpellAbility(CardFactoryUtil.ability_Flashback(card, k[1]));
            }
        }//flashback
        
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
        
        // Sol's Soulshift fix
        int shiftPos = hasKeyword(card, "Soulshift");
        while (shiftPos != -1) {
           int n = shiftPos;
           String parse = card.getKeyword().get(n).toString();
           
           String k[] = parse.split(":");
           final String manacost = k[1];
           
           card.addDestroyCommand(CardFactoryUtil.ability_Soulshift(card, manacost));
           shiftPos = hasKeyword(card, "Soulshift", n+1);
        }//Soulshift
        
        /*
        while(hasKeyword(card, "Soulshift") != -1) {
            int n = hasKeyword(card, "Soulshift");
            if(n != -1) {
                String parse = card.getKeyword().get(n).toString();
                card.removeIntrinsicKeyword(parse);
                
                String k[] = parse.split(":");
                final String manacost = k[1];
                
                card.addDestroyCommand(CardFactoryUtil.ability_Soulshift(card, manacost));
            }
        }//Soulshift
        */
        
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
                 card.setSVar("HSStamp","" + Player.getHandSizeStamp());
                 if(Target.equals("Self") || Target.equals("All")) {
                	 card.getController().addHandSizeOperation(new HandSizeOp(Mode,Amount,Integer.parseInt(card.getSVar("HSStamp"))));
                 }
                 if(Target.equals("Opponent") || Target.equals("All")) {
                     card.getController().getOpponent().addHandSizeOperation(new HandSizeOp(Mode,Amount,Integer.parseInt(card.getSVar("HSStamp"))));
                 }
               }
            };
           
            leavesPlay = new Command() {
               private static final long serialVersionUID = -6843545358873L;
               
               public void execute() {
            	   if(Target.equals("Self") || Target.equals("All")) {
                  	 card.getController().removeHandSizeOperation(Integer.parseInt(card.getSVar("HSStamp")));
                   }
                   if(Target.equals("Opponent") || Target.equals("All")) {
                       card.getController().getOpponent().removeHandSizeOperation(Integer.parseInt(card.getSVar("HSStamp")));
                   }
               }
            };
           
            controllerChanges = new Command() {
               private static final long serialVersionUID = 778987998465463L;
               
               public void execute() {
                  Log.debug("HandSize", "Control changed: " + card.getController());
                  if(card.getController().equals(AllZone.HumanPlayer)) {
                	 AllZone.HumanPlayer.removeHandSizeOperation(Integer.parseInt(card.getSVar("HSStamp")));
                	 AllZone.ComputerPlayer.addHandSizeOperation(new HandSizeOp(Mode,Amount,Integer.parseInt(card.getSVar("HSStamp"))));
                     
                     AllZone.ComputerPlayer.sortHandSizeOperations();
                  }
                  else if(card.getController().equals(AllZone.ComputerPlayer)) {
                	 AllZone.ComputerPlayer.removeHandSizeOperation(Integer.parseInt(card.getSVar("HSStamp")));
                     AllZone.HumanPlayer.addHandSizeOperation(new HandSizeOp(Mode,Amount,Integer.parseInt(card.getSVar("HSStamp"))));
                     
                     AllZone.HumanPlayer.sortHandSizeOperations();
                  }
               }
            };
           
            card.addComesIntoPlayCommand(entersPlay);
            card.addLeavesPlayCommand(leavesPlay);
            card.addChangeControllerCommand(controllerChanges);
         } //HandSize
        
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
        }//Suspend
        
        if (card.getManaCost().contains("X"))
        {
        	SpellAbility sa = card.getSpellAbility()[0];
    		sa.setIsXCost(true);
    		
        	if (card.getManaCost().startsWith("X X"))
        		sa.setXManaCost("2");
        	else if (card.getManaCost().startsWith("X"))
        		sa.setXManaCost("1");
        }//X
    	
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

        if(hasKeyword(card, "Fading") != -1) {
            int n = hasKeyword(card, "Fading");
            if(n != -1) {
                String parse = card.getKeyword().get(n).toString();
                
                String k[] = parse.split(":");
                final int power = Integer.parseInt(k[1]);
                
                card.addComesIntoPlayCommand(CardFactoryUtil.fading(card, power));
            }
        }//Fading    	
    	
        if(hasKeyword(card, "Vanishing") != -1) {
            int n = hasKeyword(card, "Vanishing");
            if(n != -1) {
                String parse = card.getKeyword().get(n).toString();
                
                String k[] = parse.split(":");
                final int power = Integer.parseInt(k[1]);
                
                card.addComesIntoPlayCommand(CardFactoryUtil.vanishing(card, power));
            }
        }//Vanishing
        
        // AltCost
		SpellAbility[] abilities = card.getSpellAbility();
		if (abilities.length > 0){
			String altCost = card.getSVar("AltCost");
			String altCostDescription = "";
			String[] altCosts = altCost.split("\\$");
			
			if (altCosts.length > 1) {
				altCostDescription = altCosts[1];
				altCost = altCosts[0];
			}
			
			SpellAbility sa = abilities[0];
			if (!altCost.equals("") && sa.isSpell())
			{
				SpellAbility altCostSA = sa.copy();
	
				Ability_Cost abCost = new Ability_Cost(altCost, card.getName(), altCostSA.isAbility());
				altCostSA.payCosts = abCost;
				
	            StringBuilder sb = new StringBuilder();
	            
	            if (altCosts.length > 1) {
	            	sb.append(altCostDescription);
	            }
	            else {
	            	sb.append("You may ").append(abCost.toStringAlt());
	            	sb.append(" rather than pay ").append(card.getName()).append("'s mana cost");
	            }
	            
	            altCostSA.setDescription(sb.toString());
	
	            card.addSpellAbility(altCostSA);
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
        c.setBaseLoyalty(sim.getBaseLoyalty());
        c.setBaseAttackString(sim.getBaseAttackString());
        c.setBaseDefenseString(sim.getBaseDefenseString());
        c.setIntrinsicKeyword(sim.getKeyword());
        c.setName(sim.getName());
        c.setImageName(sim.getImageName());
        c.setType(sim.getType());
        c.setText(sim.getSpellText());
        c.setManaCost(sim.getManaCost());
        c.setColor(sim.getColor());
        c.setSVars(sim.getSVars());
        c.setSets(sim.getSets());
        c.setIntrinsicAbilities(sim.getIntrinsicAbilities());
        c.setCurSetCode(sim.getCurSetCode());
        c.setImageFilename(sim.getImageFilename());
        
        return c;
    }// copyStats()
    
    public static void main(String[] args) {
        CardFactory f = new CardFactory(ForgeProps.getFile(CARDSFOLDER));
        Card c = f.getCard("Arc-Slogger", null);
        System.out.println(c.getOwner());
    }
}
