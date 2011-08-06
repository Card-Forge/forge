
package forge;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.StringTokenizer;
import com.esotericsoftware.minlog.Log;


class CardFactory_Auras {
	
    public static int shouldCycle(Card c) {
        ArrayList<String> a = c.getKeyword();
        for(int i = 0; i < a.size(); i++)
            if(a.get(i).toString().startsWith("Cycling")) return i;
        
        return -1;
    }
    
    public static int shouldEnchant(Card c) {
        ArrayList<String> a = c.getKeyword();
        for (int i = 0; i < a.size(); i++)
            if (a.get(i).toString().startsWith("enPump")) return i;
        
        return -1;
    }
    
    public static int shouldControl(Card c) {
        ArrayList<String> a = c.getKeyword();
        for (int i = 0; i < a.size(); i++)
            if (a.get(i).toString().startsWith("enControlCreature")) return i;
        
        return -1;
    }
    
    public static Card getCard(final Card card, String cardName, String owner) {
    	
    	Command standardUnenchant = new Command() {
			private static final long serialVersionUID = 3938247133551483568L;

			public void execute() {
                if(card.isEnchanting()) {
                    Card crd = card.getEnchanting().get(0);
                    card.unEnchantCard(crd);
                }
            }
        };
    	
    	
        // *****************************************************************
        // Enchant Lands:              *************************************
    	// Enchant land you control:   *************************************
        // *****************************************************************
        
        //*************** START *********** START **************************
        if(cardName.equals("Caribou Range")) {
            
            final SpellAbility spell = new Spell(card) {
                
                private static final long serialVersionUID = 5394181222737344498L;
                
                @Override
                /*
                 * The computer will now place this aura on unenchanted lands, but
                 * it will tap an enchanted land for mana to produce the token.
                 */
                
                public boolean canPlayAI() {

                    CardList list = new CardList(AllZone.Computer_Play.getCards());
                    list = list.filter(new CardListFilter() {
                        public boolean addCard(Card c) {
                            return c.isLand() && !c.isEnchanted() && CardFactoryUtil.canTarget(card, c);
                        }
                    });

                    if (list.isEmpty()) return false;
                    else {
                        list.shuffle();
                        setTargetCard(list.get(0));
                        return true;
                    }
                }//canPlayAI()
/*
                public boolean canPlayAI() {
                    CardList list = new CardList(AllZone.Computer_Play.getCards());
                    list = list.getType("Land");
                    
                    if(list.isEmpty()) return false;
                    
                    setTargetCard(list.get(0));
                    return true;
                }//canPlayAI()
*/
                @Override
                public void resolve() {
                    PlayerZone play = AllZone.getZone(Constant.Zone.Play, card.getController());
                    play.add(card);
                    
                    Card c = getTargetCard();
                    
                    if(AllZone.GameAction.isCardInPlay(c) && CardFactoryUtil.canTarget(card, c)) card.enchantCard(c);
                    
                }//resolve()
            };//SpellAbility
            spell.setType("Extrinsic");
            card.clearSpellAbility();
            card.addSpellAbility(spell);
            
            final SpellAbility produceCaribou = new Ability_Tap(spell.getTargetCard(), "W W") {
                
                private static final long serialVersionUID = 1358032097310954750L;
                
                @Override
                public void resolve() {
                    //makeToken();
                	CardFactoryUtil.makeToken("Caribou", "W 0 1 Caribou", spell.getTargetCard(), "W", new String[] {
                        "Creature", "Caribou"}, 0, 1, new String[] {""});
                }
                /*
                void makeToken() {
                    Card c = new Card();
                    Card crd = spell.getTargetCard();
                    
                    c.setName("Caribou");
                    c.setImageName("W 0 1 Caribou");
                    
                    c.setOwner(crd.getController());
                    c.setController(crd.getController());
                    
                    c.setManaCost("W");
                    c.setToken(true);
                    
                    c.addType("Creature");
                    c.addType("Caribou");
                    c.setBaseAttack(0);
                    c.setBaseDefense(1);
                    
                    PlayerZone play = AllZone.getZone(Constant.Zone.Play, card.getController());
                    play.add(c);
                }//makeToken()
                */
            };//SpellAbility
            
            produceCaribou.setType("Extrinsic"); // Required for Spreading Seas
            produceCaribou.setDescription("W W, Tap: Put a 0/1 white Caribou creature token onto the battlefield.");
            produceCaribou.setStackDescription("Put a 0/1 white Caribou creature token onto the battlefield.");
            
            Command onEnchant = new Command() {
                
                private static final long serialVersionUID = -4394447413814077965L;
                
                public void execute() {
                    if(card.isEnchanting()) {
                        Card crd = card.getEnchanting().get(0);
                        //crd.clearSpellAbility();
                        crd.addSpellAbility(produceCaribou);
                        
                    }
                }//execute()
            };//Command
            

            Command onUnEnchant = new Command() {
                
                private static final long serialVersionUID = -1492886212745680573L;
                
                public void execute() {
                    if(card.isEnchanting()) {
                        
                        Card crd = card.getEnchanting().get(0);
                        crd.removeSpellAbility(produceCaribou);
                    }
                }//execute()
            };//Command
            
            Command onLeavesPlay = new Command() {
                
                private static final long serialVersionUID = 8067313131085909766L;
                
                public void execute() {
                    if(card.isEnchanting()) {
                        Card crd = card.getEnchanting().get(0);
                        card.unEnchantCard(crd);
                    }
                }
            };
            
            final SpellAbility a2 = new Ability(card, "0") {
                @Override
                public void chooseTargetAI() {
                    PlayerZone play = AllZone.getZone(Constant.Zone.Play, Constant.Player.Computer);
                    CardList saps = new CardList(play.getCards());
                    saps = saps.filter(new CardListFilter() {
                        
                        public boolean addCard(Card c) {
                            if((c.getType().contains("Caribou") || c.getKeyword().contains("Changeling"))
                                    && AllZone.GameAction.isCardInPlay(c) && c.isToken()) return true;
                            return false;
                        }
                        
                    });
                    
                    if(saps.size() != 0) setTargetCard(saps.getCard(0));
                }
                
                @Override
                public void resolve() {
                    //get all saprolings:
                    Card c = getTargetCard();
                    if(c == null) return;
                    
                    if(!AllZone.GameAction.isCardInPlay(c)) return;
                    
                    if(AllZone.GameAction.isCardInPlay(c)) {
                        //AllZone.getZone(c).remove(c);
                        AllZone.GameAction.sacrifice(c);
                        AllZone.GameAction.gainLife(c.getController(), 1);
                    }
                }//resolve
                
                @Override
                public boolean canPlayAI() {
                    PlayerZone play = AllZone.getZone(Constant.Zone.Play, Constant.Player.Computer);
                    CardList cars = new CardList(play.getCards());
                    cars = cars.filter(new CardListFilter() {
                        
                        public boolean addCard(Card c) {
                            if(c.getType().contains("Caribou") || c.getKeyword().contains("Changeling")
                                    && AllZone.GameAction.isCardInPlay(c)) return true;
                            return false;
                        }
                        
                    });
                    if(AllZone.Computer_Life.getLife() < 4 && cars.size() > 0) return true;
                    else return false;
                }
            };//SpellAbility
            
            Input runtime = new Input() {
                
                private static final long serialVersionUID = 1408675899387720506L;
                
                @Override
                public void showMessage() {
                    CardList cars = new CardList(
                            AllZone.getZone(Constant.Zone.Play, card.getController()).getCards());
                    cars = cars.getType("Caribou");
                    
                    stopSetNext(CardFactoryUtil.input_targetSpecific(a2, cars, "Select a Caribou to sacrifice.",
                            false, false));
                }
            };
            
            card.addSpellAbility(a2);
            a2.setDescription("Sacrifice a Caribou: You gain 1 life.");
            a2.setStackDescription(card.getController() + " gains 1 life.");
            a2.setBeforePayMana(runtime);
            

            card.addEnchantCommand(onEnchant);
            card.addUnEnchantCommand(onUnEnchant);
            card.addLeavesPlayCommand(onLeavesPlay);
            
            Input runtime2 = new Input() {
                
                private static final long serialVersionUID = -6674543815905055287L;
                
                @Override
                public void showMessage() {
                    PlayerZone hum = AllZone.getZone(Constant.Zone.Play, Constant.Player.Human);
                    CardList land = new CardList();
                    land.addAll(hum.getCards());
                    land = land.filter(new CardListFilter() {
                        public boolean addCard(Card c) {
                            return c.isLand();
                        }
                    });
                    
                    stopSetNext(CardFactoryUtil.input_targetSpecific(spell, land,
                            "Select target land you control", true, false));
                }
            };
            spell.setBeforePayMana(runtime2);
        }//*************** END ************ END **************************  

        //*************** START *********** START **************************
        else if(cardName.equals("Leafdrake Roost")) {
            
            final SpellAbility spell = new Spell(card) {
                
                private static final long serialVersionUID = -4695012002471107694L;
                
                @Override
                /*
                 * The computer will now place this aura on unenchanted lands, but
                 * it will tap an enchanted land for mana to produce the token.
                 */
                
                public boolean canPlayAI() {

                    CardList list = new CardList(AllZone.Computer_Play.getCards());
                    list = list.filter(new CardListFilter() {
                        public boolean addCard(Card c) {
                            return c.isLand() && !c.isEnchanted() && CardFactoryUtil.canTarget(card, c);
                        }
                    });

                    if (list.isEmpty()) return false;
                    else {
                        list.shuffle();
                        setTargetCard(list.get(0));
                        return true;
                    }
                }//canPlayAI()
/*
                public boolean canPlayAI() {
                    CardList list = new CardList(AllZone.Computer_Play.getCards());
                    list = list.getType("Land");
                    
                    if(list.isEmpty()) return false;
                    
                    setTargetCard(list.get(0));
                    return true;
                }//canPlayAI()
*/
                @Override
                public void resolve() {
                    PlayerZone play = AllZone.getZone(Constant.Zone.Play, card.getController());
                    play.add(card);
                    
                    Card c = getTargetCard();
                    
                    if(AllZone.GameAction.isCardInPlay(c) && CardFactoryUtil.canTarget(card, c)) card.enchantCard(c);
                    
                }//resolve()
            };//SpellAbility
            spell.setType("Extrinsic");
            card.clearSpellAbility();
            card.addSpellAbility(spell);
            
            final SpellAbility produceDrakes = new Ability_Tap(spell.getTargetCard(), "G U") {
                
                private static final long serialVersionUID = -3849765771560556442L;
                
                @Override
                public void resolve() {
                	CardFactoryUtil.makeToken("Drake", "GU 2 2 Drake", spell.getTargetCard(), "G U", new String[] {
                        "Creature", "Drake"}, 2, 2, new String[] {"Flying"});
                	//makeToken();
                }
            };//SpellAbility
            
            produceDrakes.setType("Extrinsic"); // Required for Spreading Seas
            produceDrakes.setDescription("G U, Tap: Put a 2/2 green and blue Drake creature token with flying onto the battlefield.");
            produceDrakes.setStackDescription("Put a 2/2 green and blue Drake creature token with flying onto the battlefield.");
            
            Command onEnchant = new Command() {
                
                private static final long serialVersionUID = -5501311059855861341L;
                
                public void execute() {
                    if(card.isEnchanting()) {
                        Card crd = card.getEnchanting().get(0);
                        //crd.clearSpellAbility();
                        crd.addSpellAbility(produceDrakes);
                        
                    }
                }//execute()
            };//Command
            

            Command onUnEnchant = new Command() {
                
                private static final long serialVersionUID = 3589766088284055294L;
                
                public void execute() {
                    if(card.isEnchanting()) {
                        
                        Card crd = card.getEnchanting().get(0);
                        crd.removeSpellAbility(produceDrakes);
                        
                    }
                    
                }//execute()
            };//Command
            
            Command onLeavesPlay = new Command() {
                
                private static final long serialVersionUID = -3747590484749776557L;
                
                public void execute() {
                    if(card.isEnchanting()) {
                        Card crd = card.getEnchanting().get(0);
                        card.unEnchantCard(crd);
                    }
                }
            };
            
            card.addEnchantCommand(onEnchant);
            card.addUnEnchantCommand(onUnEnchant);
            card.addLeavesPlayCommand(onLeavesPlay);
            
            Input runtime = new Input() {
                
                private static final long serialVersionUID = 967525396666242309L;
                
                @Override
                public void showMessage() {
                    PlayerZone comp = AllZone.getZone(Constant.Zone.Play, Constant.Player.Computer);
                    PlayerZone hum = AllZone.getZone(Constant.Zone.Play, Constant.Player.Human);
                    CardList land = new CardList();
                    land.addAll(comp.getCards());
                    land.addAll(hum.getCards());
                    land = land.filter(new CardListFilter() {
                        public boolean addCard(Card c) {
                            return c.isLand();
                        }
                    });
                    
                    stopSetNext(CardFactoryUtil.input_targetSpecific(spell, land, "Select target land", true,
                            false));
                }
            };
            spell.setBeforePayMana(runtime);
        }//*************** END ************ END **************************

        //*************** START *********** START **************************
        else if(cardName.equals("Squirrel Nest")) {
            
            final SpellAbility spell = new Spell(card) {
                
                private static final long serialVersionUID = 6115713202262504968L;
                
                @Override
                public boolean canPlayAI() {
                	
                	CardList list = new CardList(AllZone.Computer_Play.getCards());
                	list = list.filter(new CardListFilter() {
                	    public boolean addCard(Card c) {
                	        return c.isLand() && !c.isEnchanted() && CardFactoryUtil.canTarget(card, c);
                	    }
                	});
                    
                    if (list.isEmpty()) return false;
                    else {
                    	list.shuffle();
                        setTargetCard(list.get(0));
                        return true;
                    }
                }//canPlayAI()
/*
                public boolean canPlayAI() {
                    CardList list = new CardList(AllZone.Computer_Play.getCards());
                    list = list.getType("Land");
                    
                    if(list.isEmpty()) return false;
                    
                    setTargetCard(list.get(0));
                    return true;
                }//canPlayAI()
*/
                
                @Override
                public void resolve() {
                    PlayerZone play = AllZone.getZone(Constant.Zone.Play, card.getController());
                    play.add(card);
                    
                    Card c = getTargetCard();
                    
                    if(AllZone.GameAction.isCardInPlay(c) && CardFactoryUtil.canTarget(card, c)) card.enchantCard(c);
                    
                }//resolve()
            };//SpellAbility
            spell.setType("Extrinsic");
            
            card.clearSpellAbility();
            card.addSpellAbility(spell);
            
            final SpellAbility produceSquirrels = new Ability_Tap(spell.getTargetCard()) {
                private static final long serialVersionUID = -4800170026789001271L;
                
                @Override
                public void resolve() {
                    //makeToken();
                	CardFactoryUtil.makeToken("Squirrel", "G 1 1 Squirrel", spell.getTargetCard(), "G", new String[] {
                            "Creature", "Squirrel"}, 1, 1, new String[] {""});
                }
            
            };//SpellAbility
            
            produceSquirrels.setType("Extrinsic"); // Required for Spreading Seas
            produceSquirrels.setDescription("Tap: Put a 1/1 green Squirrel creature token into play.");
            produceSquirrels.setStackDescription("Put a 1/1 green Squirrel creature token into play.");
            
            Command onEnchant = new Command() {
                

                private static final long serialVersionUID = 3528675502863241126L;
                
                public void execute() {
                    if(card.isEnchanting()) {
                        Card crd = card.getEnchanting().get(0);
                        //crd.clearSpellAbility();
                        crd.addSpellAbility(produceSquirrels);
                        
                    }
                }//execute()
            };//Command
            

            Command onUnEnchant = new Command() {
                private static final long serialVersionUID = -2021446345291180334L;
                
                public void execute() {
                    if(card.isEnchanting()) {
                        
                        Card crd = card.getEnchanting().get(0);
                        crd.removeSpellAbility(produceSquirrels);
                        
                    }
                    
                }//execute()
            };//Command
            
            Command onLeavesPlay = new Command() {
                
                private static final long serialVersionUID = -4543302260602460839L;
                
                public void execute() {
                    if(card.isEnchanting()) {
                        Card crd = card.getEnchanting().get(0);
                        card.unEnchantCard(crd);
                    }
                }
            };
            
            card.addEnchantCommand(onEnchant);
            card.addUnEnchantCommand(onUnEnchant);
            card.addLeavesPlayCommand(onLeavesPlay);
            
            Input runtime = new Input() {
                
                private static final long serialVersionUID = 967525396666242309L;
                
                @Override
                public void showMessage() {
                    PlayerZone comp = AllZone.getZone(Constant.Zone.Play, Constant.Player.Computer);
                    PlayerZone hum = AllZone.getZone(Constant.Zone.Play, Constant.Player.Human);
                    CardList land = new CardList();
                    land.addAll(comp.getCards());
                    land.addAll(hum.getCards());
                    land = land.filter(new CardListFilter() {
                        public boolean addCard(Card c) {
                            return c.isLand();
                        }
                    });
                    
                    stopSetNext(CardFactoryUtil.input_targetSpecific(spell, land, "Select target land", true,
                            false));
                }
            };
            spell.setBeforePayMana(runtime);
        }//*************** END ************ END **************************		

        //*************** START *********** START **************************
        else if(cardName.equals("Convincing Mirage") || cardName.equals("Phantasmal Terrain")
        		|| cardName.equals("Spreading Seas") || cardName.equals("Evil Presence")
        		|| cardName.equals("Lingering Mirage") || cardName.equals("Sea's Claim")
        		|| cardName.equals("Tainted Well")) {
            
        	final String[] NewType = new String[1];
            final SpellAbility spell = new Spell(card) {
                
                private static final long serialVersionUID = 53941812202244498L;
                
                @Override
                public boolean canPlayAI() {
                    CardList list = new CardList(AllZone.Human_Play.getCards());
                    list = list.getType("Land");
                    list = list.filter(new CardListFilter() {
                    	public boolean addCard(Card c) {
                    		return c.getType().contains("Land");
                    	}
                    });
                    if(list.isEmpty()) return false;
                    
                    setTargetCard(list.get(0));
                    return true;
                }//canPlayAI()
                
                @Override
                public void resolve() {
                	if(card.getName().equals("Spreading Seas")
                			|| card.getName().equals("Lingering Mirage")
                			|| card.getName().equals("Sea's Claim"))
                	{
                		NewType[0] = "Island";
                	}
                	else if(card.getName().equals("Evil Presence") 
                			||card.getName().equals("Tainted Well")) {
                		NewType[0] = "Swamp";
                	}
                	else
                	{
                		String[] LandTypes = new String[] { "Plains","Island","Swamp","Mountain","Forest"};
                		if(card.getController().equals(Constant.Player.Computer))
                		{
                			HashMap<String,Integer> humanLandCount = new HashMap<String,Integer>();
                			CardList humanlands = new CardList(AllZone.Human_Play.getCards());
                			humanlands = humanlands.getType("Land");
                			humanlands = humanlands.filter(new CardListFilter() {
                            	public boolean addCard(Card c) {
                            		return c.getType().contains("Land");
                            	}
                            });
                			
                			for(int i=0;i<LandTypes.length;i++)
                			{
                				humanLandCount.put(LandTypes[i],0);
                			}
                			
                			for(Card c:humanlands)
                			{
                				for(String singleType:c.getType())
                				{
                					if(CardUtil.isBasicLandType(singleType))
                					{
                						humanLandCount.put(singleType, humanLandCount.get(singleType)+1);
                					}
                				}
                			}
                			
                			int minAt = 0;
                			int minVal = Integer.MAX_VALUE;
                			for(int i=0;i<LandTypes.length;i++)
                			{
                				if(getTargetCard().getType().contains(LandTypes[i])) continue;
                				
                				if(humanLandCount.get(LandTypes[i]) < minVal)
                				{
                					minVal = humanLandCount.get(LandTypes[i]);
                					minAt = i;
                				}
                			}
                			
                			NewType[0] = LandTypes[minAt];
                		}
                		else
                		{
                			NewType[0] = AllZone.Display.getChoice("Select land type.", "Plains","Island","Swamp","Mountain","Forest");
                		}
                	}
                	PlayerZone play = AllZone.getZone(Constant.Zone.Play, card.getController());
                    play.add(card);
                    
                    Card c = getTargetCard();
                    
                    if(AllZone.GameAction.isCardInPlay(c) && CardFactoryUtil.canTarget(card, c)) card.enchantCard(c);                    
                    
                }//resolve()
            };//SpellAbility
            card.clearSpellAbility();
            card.addSpellAbility(spell);
            
            Command onEnchant = new Command() {
                

                private static final long serialVersionUID = 3528675112863241126L;
                
                public void execute() {
                    if(card.isEnchanting()) {
                    	Card crd = card.getEnchanting().get(0);
                        ArrayList<Card> Seas = crd.getEnchantedBy();
                        int Count = 0;
                        for(int i = 0; i < Seas.size(); i++) {
                        	if(Seas.get(i).getName().equals(card.getName()))
                        	{
                        		Count = Count + 1;
                        	}
                        }
                        if(Count == 1) {                      
                        	crd.removeType("Swamp");
                        	crd.removeType("Forest");
                        	crd.removeType("Island");
                        	crd.removeType("Plains");
                        	crd.removeType("Mountain");
                        
                        	crd.addType(NewType[0]);
                        	SpellAbility[] Abilities = crd.getSpellAbility();
                        	for(int i = 0; i < Abilities.length; i++) {
                        		if(Abilities[i].isIntrinsic()) {
                        			card.addSpellAbility(Abilities[i]);
                        			crd.removeSpellAbility(Abilities[i]);
                        		}
                        	}
                        	String ManaCol = "";
                        	if(NewType[0].equals("Plains"))
                        	{
                        		ManaCol = "W";
                        	}
                        	else if(NewType[0].equals("Island"))
                        	{
                        		ManaCol = "U";
                        	}
                        	else if(NewType[0].equals("Swamp"))
                        	{
                        		ManaCol = "B";
                        	}
                        	else if(NewType[0].equals("Mountain"))
                        	{
                        		ManaCol = "R";
                        	}
                        	else if(NewType[0].equals("Forest"))
                        	{
                        		ManaCol = "G";
                        	}
                        	crd.addSpellAbility(new Ability_Mana(card, "tap: add " + ManaCol) {
                    			private static final long serialVersionUID = 787111012484588884L;
                    		});
                        } else {
                        	Card Other_Seas = null;
                        	for(int i = 0; i < Seas.size(); i++) {
                        		if(Seas.get(i) != card) Other_Seas = Seas.get(i);
                        	}
                        	SpellAbility[] Abilities = Other_Seas.getSpellAbility();
                        	for(int i = 0; i < Abilities.length; i++) {
                        			card.addSpellAbility(Abilities[i]);
                        	}	
                        }
                    }
                }//execute()
            };//Command
            

            Command onUnEnchant = new Command() {
                private static final long serialVersionUID = -202144631191180334L;
                
                public void execute() {
                    if(card.isEnchanting()) {                       
                        Card crd = card.getEnchanting().get(0);
                        ArrayList<Card> Seas = crd.getEnchantedBy();
                        int Count = 0;
                        for(int i = 0; i < Seas.size(); i++) {
                        if(Seas.get(i).getName().equals(card.getName())) Count = Count + 1;	
                        }
                        if(Count == 1) {
                            crd.removeType(NewType[0]);
                            crd.removeType("Land");
                            crd.removeType("Basic");
                            crd.removeType("Snow");
                            crd.removeType("Legendary");
                            SpellAbility[] Card_Abilities = crd.getSpellAbility();
                            for(int i = 0; i < Card_Abilities.length; i++) {
                            	if(Card_Abilities[i].isIntrinsic()) crd.removeSpellAbility(Card_Abilities[i]);	
                            	}
                        	Card c = AllZone.CardFactory.copyCard(crd);                       	
                        	ArrayList<String> Types = c.getType();
                        	SpellAbility[] Abilities = card.getSpellAbility();
                        	for(int i = 0; i < Types.size(); i++) {
                        	crd.addType(Types.get(i));	
                        	}
                        	for(int i = 0; i < Abilities.length; i++) {
                            	crd.addSpellAbility(Abilities[i]);	
                            	}
                        	}   
                    }
                    
                }//execute()
            };//Command


            
            Command onLeavesPlay = new Command() {
                
                private static final long serialVersionUID = -45433022112460839L;
                
                public void execute() {
                    if(card.isEnchanting()) {
                        Card crd = card.getEnchanting().get(0);
                        card.unEnchantCard(crd);
                    }
                }
            };
            
            card.addEnchantCommand(onEnchant);
            card.addUnEnchantCommand(onUnEnchant);
            card.addLeavesPlayCommand(onLeavesPlay);
            
            Input runtime = new Input() {
                
                private static final long serialVersionUID = -62372711146079880L;
                
                @Override
                public void showMessage() {
                    PlayerZone comp = AllZone.getZone(Constant.Zone.Play, Constant.Player.Computer);
                    PlayerZone hum = AllZone.getZone(Constant.Zone.Play, Constant.Player.Human);
                    CardList land = new CardList();
                    land.addAll(comp.getCards());
                    land.addAll(hum.getCards());
                    land = land.filter(new CardListFilter() {
                        public boolean addCard(Card c) {
                            return c.isLand();
                        }
                    });
                    
                    stopSetNext(CardFactoryUtil.input_targetSpecific(spell, land, "Select target land", true,
                            false));
                }
            };
            spell.setBeforePayMana(runtime);
        }//*************** END ************ END **************************
        
        //*************** START *********** START **************************
        else if(cardName.equals("Cursed Land")) {
            
            final SpellAbility spell = new Spell(card) {
                
                private static final long serialVersionUID = 5394181222737344498L;
                
                @Override
                public boolean canPlayAI() {
                    CardList list = new CardList(AllZone.Human_Play.getCards());
                    list = list.getType("Land");
                    
                    if(list.isEmpty()) return false;
                    
                    setTargetCard(list.get(0));
                    return true;
                }//canPlayAI()
                
                @Override
                public void resolve() {
                    PlayerZone play = AllZone.getZone(Constant.Zone.Play, card.getController());
                    play.add(card);
                    
                    Card c = getTargetCard();
                    
                    if(AllZone.GameAction.isCardInPlay(c) && CardFactoryUtil.canTarget(card, c)) card.enchantCard(c);
                    
                }//resolve()
            };//SpellAbility
            card.clearSpellAbility();
            card.addSpellAbility(spell);
            
            Command onLeavesPlay = new Command() {
                
                private static final long serialVersionUID = 1395122135234314967L;
                
                public void execute() {
                    if(card.isEnchanting()) {
                        Card crd = card.getEnchanting().get(0);
                        card.unEnchantCard(crd);
                    }
                }
            };
            card.addLeavesPlayCommand(onLeavesPlay);
            
            Input runtime = new Input() {
                
                private static final long serialVersionUID = -6237279587146079880L;
                
                @Override
                public void showMessage() {
                    PlayerZone comp = AllZone.getZone(Constant.Zone.Play, Constant.Player.Computer);
                    PlayerZone hum = AllZone.getZone(Constant.Zone.Play, Constant.Player.Human);
                    CardList land = new CardList();
                    land.addAll(comp.getCards());
                    land.addAll(hum.getCards());
                    land = land.filter(new CardListFilter() {
                        public boolean addCard(Card c) {
                            return c.isLand();
                        }
                    });
                    
                    stopSetNext(CardFactoryUtil.input_targetSpecific(spell, land, "Select target land", true,
                            false));
                }
            };
            spell.setBeforePayMana(runtime);
        }//*************** END ************ END **************************
        

        
        // *****************************************************************
        // Enchant artifacts:   ********************************************
        // *****************************************************************

        //*************** START *********** START **************************
        else if (cardName.equals("Animate Artifact")) {
            
            final SpellAbility spell = new Spell(card) {
				private static final long serialVersionUID = 28936527178122685L;

				@Override
                public boolean canPlayAI() {
                	/*
                	 *  AI targets computer artifact but not artifact equipment
                	 */
                	CardList list = new CardList(AllZone.Computer_Play.getCards());
                	list = list.filter(new CardListFilter() {
                		public boolean addCard(Card c) {
                			return !c.isCreature() && 
                				    CardFactoryUtil.canTarget(card, c) && 
                				    c.isArtifact() && 
                				   !c.getType().contains("Equipment");
                		}
                	});
                    
                    if (list.isEmpty()) {
                    	return false;
                    } else {
                    	Card crd = CardFactoryUtil.AI_getBestArtifact(list);
                    	if (CardUtil.getConvertedManaCost(crd) >= 2) setTargetCard(crd);
                    	else return false;
                    }
                    return true;
                }//canPlayAI()
                
                @Override
                public void resolve() {
                    PlayerZone play = AllZone.getZone(Constant.Zone.Play, card.getController());
                    play.add(card);
                    
                    Card c = getTargetCard();
                    
                    if (AllZone.GameAction.isCardInPlay(c) && CardFactoryUtil.canTarget(card, c)) card.enchantCard(c);
                    
                }//resolve()
            };//SpellAbility
            card.clearSpellAbility();
            card.addSpellAbility(spell);
            
            Command onEnchant = new Command() {
				private static final long serialVersionUID = -4748506461176516841L;

				public void execute() {
                    if (card.isEnchanting()) {
                        Card crd = card.getEnchanting().get(0);
                        if (!crd.getType().contains("Creature")) {
                            crd.addType("Creature");
                            crd.setBaseAttack(CardUtil.getConvertedManaCost(crd));
                            crd.setBaseDefense(CardUtil.getConvertedManaCost(crd));
                        }
                    }
                }//execute()
            };//Command
            
            Command onUnEnchant = new Command() {
				private static final long serialVersionUID = 7475405057975133320L;

				public void execute() {
                    if(card.isEnchanting()) {
                        Card crd = card.getEnchanting().get(0);
                        crd.removeType("Creature");
                        crd.setBaseAttack(0);
                        crd.setBaseDefense(0);
                    }
                }//execute()
            };//Command
            
            Command onLeavesPlay = new Command() {
				private static final long serialVersionUID = -7820794265954245241L;

				public void execute() {
                    if(card.isEnchanting()) {
                        Card crd = card.getEnchanting().get(0);
                        card.unEnchantCard(crd);
                    }
                }
            };
            
            Input runtime = new Input() {
				private static final long serialVersionUID = -7462101446917907106L;

				// Can't allow an artifact equipment that is equipping a creature to become animated
				@Override
                public void showMessage() {
                    PlayerZone comp = AllZone.getZone(Constant.Zone.Play, Constant.Player.Computer);
                    PlayerZone hum = AllZone.getZone(Constant.Zone.Play, Constant.Player.Human);
                    CardList artifacts = new CardList();
                    artifacts.addAll(comp.getCards());
                    artifacts.addAll(hum.getCards());
                    artifacts = artifacts.filter(new CardListFilter() {
                        public boolean addCard(Card c) {
                            return !c.isCreature() && 
                            	   CardFactoryUtil.canTarget(card, c) && 
                            	   !c.isEquipping();
                        }
                    });
                    
                    stopSetNext(CardFactoryUtil.input_targetSpecific(spell, artifacts, "Select target artifact", true, false));
                }
            };
            card.addEnchantCommand(onEnchant);
            card.addUnEnchantCommand(onUnEnchant);
            card.addLeavesPlayCommand(onLeavesPlay);
            
            spell.setBeforePayMana(runtime);
        }//*************** END ************ END **************************
        

        
        // *****************************************************************
        // Enchant creatures:   ********************************************
        // *****************************************************************
        
        //*************** START *********** START **************************
        else if(cardName.equals("Earthbind")) {
            final SpellAbility spell = new Spell(card) {
                
                private static final long serialVersionUID = 142389375702113977L;
                
                @Override
                public boolean canPlayAI() {
                    CardList list = new CardList(AllZone.Human_Play.getCards());
                    list = list.getType("Creature").getKeyword("Flying");
                    if(list.isEmpty()) return false;
                    
                    CardListFilter f = new CardListFilter() {
                        public boolean addCard(Card c) {
                            return c.getNetDefense() - c.getDamage() <= 2;
                        }
                    };
                    if(!list.filter(f).isEmpty()) list = list.filter(f);
                    CardListUtil.sortAttack(list);
                    
                    for(int i = 0; i < list.size(); i++) {
                        if(CardFactoryUtil.canTarget(card, list.get(i))) {
                            setTargetCard(list.get(i));
                            return true;
                        }
                    }
                    return false;
                }//canPlayAI()
                
                @Override
                public void resolve() {
                    PlayerZone play = AllZone.getZone(Constant.Zone.Play, card.getController());
                    play.add(card);
                    
                    Card c = getTargetCard();
                    
                    if(AllZone.GameAction.isCardInPlay(c) && CardFactoryUtil.canTarget(card, c)) {
                        card.enchantCard(c);
                        Log.debug("Enchanted: " + getTargetCard());
                    }
                }//resolve()
            };//SpellAbility
            card.clearSpellAbility();
            card.addSpellAbility(spell);
            
            final boolean[] badTarget = {true};
            Command onEnchant = new Command() {
                
                private static final long serialVersionUID = -5302506578307993978L;
                
                public void execute() {
                    if(card.isEnchanting()) {
                        Card crd = card.getEnchanting().get(0);
                        if(crd.getKeyword().contains("Flying")) {
                            badTarget[0] = false;
                            AllZone.GameAction.addDamage(crd, card, 2);
                            crd.removeIntrinsicKeyword("Flying");
                            crd.removeExtrinsicKeyword("Flying");
                        } else badTarget[0] = true;
                    }
                }//execute()
            };//Command
            

            Command onUnEnchant = new Command() {
                
                private static final long serialVersionUID = -6908757692588823391L;
                
                public void execute() {
                    if(card.isEnchanting() && !badTarget[0]) {
                        Card crd = card.getEnchanting().get(0);
                        crd.addIntrinsicKeyword("Flying");
                    }
                    
                }//execute()
            };//Command
            
            Command onLeavesPlay = new Command() {
                
                private static final long serialVersionUID = -7833240882415702940L;
                
                public void execute() {
                    if(card.isEnchanting()) {
                        Card crd = card.getEnchanting().get(0);
                        card.unEnchantCard(crd);
                    }
                }
            };
            
            card.addEnchantCommand(onEnchant);
            card.addUnEnchantCommand(onUnEnchant);
            card.addLeavesPlayCommand(onLeavesPlay);
            
            spell.setBeforePayMana(CardFactoryUtil.input_targetCreature(spell));
        }//*************** END ************ END **************************
    	
        //*************** START *********** START **************************
        else if(cardName.equals("Pillory of the Sleepless")) {
            final SpellAbility spell = new Spell(card) {
                
                private static final long serialVersionUID = 4504925036782582195L;
                
                @Override
                public boolean canPlayAI() {
                    CardList list = new CardList(AllZone.Human_Play.getCards());
                    list = list.getType("Creature");
                    
                    if(list.isEmpty()) return false;
                    
                    //else
                    CardListUtil.sortAttack(list);
                    CardListUtil.sortFlying(list);
                    
                    for(int i = 0; i < list.size(); i++) {
                        if(CardFactoryUtil.canTarget(card, list.get(i))
                                && !list.get(i).getKeyword().contains("Defender")) {
                            setTargetCard(list.get(i));
                            return true;
                        }
                    }
                    return false;
                }//canPlayAI()
                
                @Override
                public void resolve() {
                    PlayerZone play = AllZone.getZone(Constant.Zone.Play, card.getController());
                    play.add(card);
                    
                    Card c = getTargetCard();
                    
                    if(AllZone.GameAction.isCardInPlay(c) && CardFactoryUtil.canTarget(card, c)) {
                        card.enchantCard(c);
                        //System.out.println("Enchanted: " +getTargetCard());
                    }
                }//resolve()
            };//SpellAbility
            card.clearSpellAbility();
            card.addSpellAbility(spell);
            
            Command onEnchant = new Command() {
                
                private static final long serialVersionUID = -6104532173397759007L;
                
                public void execute() {
                    if(card.isEnchanting()) {
                        Card crd = card.getEnchanting().get(0);
                        crd.addExtrinsicKeyword("CARDNAME can't attack or block.");
                    }
                }//execute()
            };//Command
            

            Command onUnEnchant = new Command() {
                
                private static final long serialVersionUID = -2563098134722661731L;
                
                public void execute() {
                    if(card.isEnchanting()) {
                        Card crd = card.getEnchanting().get(0);
                        crd.removeExtrinsicKeyword("CARDNAME can't attack or block.");
                    }
                    
                }//execute()
            };//Command
            
            Command onLeavesPlay = new Command() {
                
                private static final long serialVersionUID = -1621250313053538491L;
                
                public void execute() {
                    if(card.isEnchanting()) {
                        Card crd = card.getEnchanting().get(0);
                        card.unEnchantCard(crd);
                    }
                }
            };
            
            card.addEnchantCommand(onEnchant);
            card.addUnEnchantCommand(onUnEnchant);
            card.addLeavesPlayCommand(onLeavesPlay);
            
            spell.setBeforePayMana(CardFactoryUtil.input_targetCreature(spell));
        }//*************** END ************ END **************************
        
        //*************** START *********** START **************************
        else if(cardName.equals("Guilty Conscience")) {
            final SpellAbility spell = new Spell(card) {
                
                private static final long serialVersionUID = 1169151960692309514L;
                
                @Override
                public boolean canPlayAI() {
                    
                    CardList stuffy = new CardList(AllZone.Computer_Play.getCards());
                    stuffy = stuffy.getName("Stuffy Doll");
                    
                    if(stuffy.size() > 0) {
                        setTargetCard(stuffy.get(0));
                        return true;
                    } else {
                        CardList list = new CardList(AllZone.Human_Play.getCards());
                        list = list.getType("Creature");
                        
                        if(list.isEmpty()) return false;
                        
                        //else
                        CardListUtil.sortAttack(list);
                        CardListUtil.sortFlying(list);
                        
                        for(int i = 0; i < list.size(); i++) {
                            if(CardFactoryUtil.canTarget(card, list.get(i))
                                    && (list.get(i).getNetAttack() >= list.get(i).getNetDefense())
                                    && list.get(i).getNetAttack() >= 3) {
                                setTargetCard(list.get(i));
                                return true;
                            }
                        }
                    }
                    return false;
                    
                }//canPlayAI()
                
                @Override
                public void resolve() {
                    PlayerZone play = AllZone.getZone(Constant.Zone.Play, card.getController());
                    play.add(card);
                    
                    Card c = getTargetCard();
                    
                    if(AllZone.GameAction.isCardInPlay(c) && CardFactoryUtil.canTarget(card, c)) card.enchantCard(c);
                    
                }//resolve()
            };//SpellAbility
            card.clearSpellAbility();
            card.addSpellAbility(spell);
            

            spell.setBeforePayMana(CardFactoryUtil.input_targetCreature(spell));
        }//*************** END ************ END **************************
                
        //******************************************************************
        // This card can't be converted to keyword, problem with CARDNME   *
        //*************** START *********** START **************************
        else if(cardName.equals("Vigilance")) {
            final SpellAbility spell = new Spell(card) {
                
                private static final long serialVersionUID = 3659751920022901998L;
                
                @Override
                public boolean canPlayAI() {
                    CardList list = new CardList(AllZone.Computer_Play.getCards());
                    list = list.getType("Creature");
                    
                    if (list.isEmpty()) return false;
                    
                    //else (is there a Rabid Wombat or a Uril, the Miststalker to target?)
                    
                    CardList auraMagnetList = new CardList(AllZone.Computer_Play.getCards());
                    auraMagnetList = auraMagnetList.getEnchantMagnets();
                    
                    if (! auraMagnetList.isEmpty()) {    // AI has a special target creature(s) to enchant
                        auraMagnetList.shuffle();
                        for (int i = 0; i < auraMagnetList.size(); i++) {
                            if (CardFactoryUtil.canTarget(card, auraMagnetList.get(i))) {
                                setTargetCard(auraMagnetList.get(i));    // Target only Rabid Wombat or Uril, the Miststalker
                    	        return true;
                    	    }
                    	}
                    }
                    //else target another creature
                    
                    CardListUtil.sortAttack(list);
                    CardListUtil.sortFlying(list);
                    
                    for (int i = 0; i < list.size(); i++) {
                        if (CardFactoryUtil.canTarget(card, list.get(i))
                                && !list.get(i).getKeyword().contains("Vigilance")) {
                            setTargetCard(list.get(i));
                            return true;
                        }
                    }
                  
                    return false;
                }//canPlayAI()
                
                @Override
                public void resolve() {
                    PlayerZone play = AllZone.getZone(Constant.Zone.Play, card.getController());
                    play.add(card);
                    
                    Card c = getTargetCard();
                    
                    if(AllZone.GameAction.isCardInPlay(c) && CardFactoryUtil.canTarget(card, c)) {
                        card.enchantCard(c);
                        Log.debug("Enchanted: " + getTargetCard());
                    }
                }//resolve()
            };//SpellAbility
            card.clearSpellAbility();
            card.addSpellAbility(spell);
            
            Command onEnchant = new Command() {
                
                private static final long serialVersionUID = -2060758415927004190L;
                
                public void execute() {
                    if(card.isEnchanting()) {
                        Card crd = card.getEnchanting().get(0);
                        crd.addExtrinsicKeyword("Vigilance");
                    }
                }//execute()
            };//Command
            

            Command onUnEnchant = new Command() {
                
                private static final long serialVersionUID = -5220074511756932255L;
                
                public void execute() {
                    if(card.isEnchanting()) {
                        Card crd = card.getEnchanting().get(0);
                        
                        crd.removeExtrinsicKeyword("Vigilance");
                    }
                    
                }//execute()
            };//Command
            
            Command onLeavesPlay = new Command() {
                
                private static final long serialVersionUID = -549155960320946886L;
                
                public void execute() {
                    if(card.isEnchanting()) {
                        Card crd = card.getEnchanting().get(0);
                        card.unEnchantCard(crd);
                    }
                }
            };
            
            card.addEnchantCommand(onEnchant);
            card.addUnEnchantCommand(onUnEnchant);
            card.addLeavesPlayCommand(onLeavesPlay);
            
            spell.setBeforePayMana(CardFactoryUtil.input_targetCreature(spell));
        }//*************** END ************ END **************************
        
        //************************************************************************
        // This card can't be converted to keyword, problem with keyword parse   *
        //*************** START *********** START ********************************
        else if(cardName.equals("Seeker")) {
            final SpellAbility spell = new Spell(card) {

				private static final long serialVersionUID = 5952584725129324530L;

				@Override
                public boolean canPlayAI() {
                    CardList list = new CardList(AllZone.Computer_Play.getCards());
                    list = list.getType("Creature");
                    
                    if (list.isEmpty()) return false;
                    
                    //else (is there a Rabid Wombat or a Uril, the Miststalker to target?)
                    
                    CardList auraMagnetList = new CardList(AllZone.Computer_Play.getCards());
                    auraMagnetList = auraMagnetList.getEnchantMagnets();
                    
                    if (! auraMagnetList.isEmpty()) {    // AI has a special target creature(s) to enchant
                        auraMagnetList.shuffle();
                        for (int i = 0; i < auraMagnetList.size(); i++) {
                            if (CardFactoryUtil.canTarget(card, auraMagnetList.get(i))) {
                                setTargetCard(auraMagnetList.get(i));    // Target only Rabid Wombat or Uril, the Miststalker
                    	        return true;
                    	    }
                    	}
                    }
                    //else target another creature
                    
                    CardListUtil.sortAttack(list);
                    CardListUtil.sortFlying(list);
                    
                    for (int i = 0; i < list.size(); i++) {
                        if (CardFactoryUtil.canTarget(card, list.get(i))
                                && !list.get(i).getKeyword().contains("CARDNAME can't be blocked except by artifact creatures and/or white creatures.")) {
                            setTargetCard(list.get(i));
                            return true;
                        }
                    }
                  
                    return false;
                }//canPlayAI()
                
                @Override
                public void resolve() {
                    PlayerZone play = AllZone.getZone(Constant.Zone.Play, card.getController());
                    play.add(card);
                    
                    Card c = getTargetCard();
                    
                    if(AllZone.GameAction.isCardInPlay(c) && CardFactoryUtil.canTarget(card, c)) {
                        card.enchantCard(c);
                        Log.debug("Enchanted: " + getTargetCard());
                    }
                }//resolve()
            };//SpellAbility
            card.clearSpellAbility();
            card.addSpellAbility(spell);
            
            Command onEnchant = new Command() {

				private static final long serialVersionUID = 2007362030422979630L;

				public void execute() {
                    if(card.isEnchanting()) {
                        Card crd = card.getEnchanting().get(0);
                        crd.addExtrinsicKeyword("CARDNAME can't be blocked except by artifact creatures and/or white creatures.");
                    }
                }//execute()
            };//Command
            

            Command onUnEnchant = new Command() {

				private static final long serialVersionUID = -8020540432500093584L;

				public void execute() {
                    if(card.isEnchanting()) {
                        Card crd = card.getEnchanting().get(0);
                        
                        crd.removeExtrinsicKeyword("CARDNAME can't be blocked except by artifact creatures and/or white creatures.");
                    }
                    
                }//execute()
            };//Command
            
            Command onLeavesPlay = new Command() {

				private static final long serialVersionUID = 3359456668229802294L;

				public void execute() {
                    if(card.isEnchanting()) {
                        Card crd = card.getEnchanting().get(0);
                        card.unEnchantCard(crd);
                    }
                }
            };
            
            card.addEnchantCommand(onEnchant);
            card.addUnEnchantCommand(onUnEnchant);
            card.addLeavesPlayCommand(onLeavesPlay);
            
            spell.setBeforePayMana(CardFactoryUtil.input_targetCreature(spell));
        }//*************** END ************ END **************************
        
        //******************************************************************
        // This card can't be converted to keyword, problem with Lifelink  *
        //*************** START *********** START **************************
        else if(cardName.equals("Lifelink")) {
            final SpellAbility spell = new Spell(card) {
				private static final long serialVersionUID = 8493277543267009695L;

				@Override
                public boolean canPlayAI() {
                    CardList list = new CardList(AllZone.Computer_Play.getCards());
                    list = list.getType("Creature");
                    
                    if (list.isEmpty()) return false;
                    
                    //else (is there a Rabid Wombat or a Uril, the Miststalker to target?)
                    
                    CardList auraMagnetList = new CardList(AllZone.Computer_Play.getCards());
                    auraMagnetList = auraMagnetList.getEnchantMagnets();
                    
                    if (! auraMagnetList.isEmpty()) {    // AI has a special target creature(s) to enchant
                        auraMagnetList.shuffle();
                        for (int i = 0; i < auraMagnetList.size(); i++) {
                            if (CardFactoryUtil.canTarget(card, auraMagnetList.get(i))) {
                                setTargetCard(auraMagnetList.get(i));    // Target only Rabid Wombat or Uril, the Miststalker
                    	        return true;
                    	    }
                    	}
                    }
                    
                    /*
                     *  else target another creature
                     *  Do not enchant card with Defender or Lifelink or enchant card already enchanted
                     */
                    CardListUtil.sortAttack(list);
                    CardListUtil.sortFlying(list);
                    
                    for (int i = 0; i < list.size(); i++) {
                        if (CardFactoryUtil.canTarget(card, list.get(i))
                                && !list.get(i).getKeyword().contains("Lifelink")  && 
                                !list.get(i).getKeyword().contains("Defender") && !list.get(i).isEnchanted()) {
                            setTargetCard(list.get(i));
                            return true;
                        }
                    }
                  
                    return false;
                }//canPlayAI()
                
                @Override
                public void resolve() {
                    PlayerZone play = AllZone.getZone(Constant.Zone.Play, card.getController());
                    play.add(card);
                    
                    Card c = getTargetCard();
                    
                    if(AllZone.GameAction.isCardInPlay(c) && CardFactoryUtil.canTarget(card, c)) {
                        card.enchantCard(c);
                        Log.debug("Lifelink", "Enchanted: " + getTargetCard());
                    }
                }//resolve()
            };//SpellAbility
            card.clearSpellAbility();
            card.addSpellAbility(spell);
            
            Command onEnchant = new Command() {
                
				private static final long serialVersionUID = -9156474672737153867L;

				public void execute() {
                    if(card.isEnchanting()) {
                        Card crd = card.getEnchanting().get(0);
                        crd.addExtrinsicKeyword("Lifelink");
                    }
                }//execute()
            };//Command
            

            Command onUnEnchant = new Command() {
                
				private static final long serialVersionUID = 3855541943505550043L;

				public void execute() {
                    if(card.isEnchanting()) {
                        Card crd = card.getEnchanting().get(0);
                        
                        crd.removeExtrinsicKeyword("Lifelink");
                    }
                    
                }//execute()
            };//Command
            
            Command onLeavesPlay = new Command() {
                
				private static final long serialVersionUID = 927099787099002012L;
                
                public void execute() {
                    if(card.isEnchanting()) {
                        Card crd = card.getEnchanting().get(0);
                        card.unEnchantCard(crd);
                    }
                }//execute()
            };//Command
            
            card.addEnchantCommand(onEnchant);
            card.addUnEnchantCommand(onUnEnchant);
            card.addLeavesPlayCommand(onLeavesPlay);
            
            spell.setBeforePayMana(CardFactoryUtil.input_targetCreature(spell));
        }//*************** END ************ END **************************
        
        //*************** START *********** START **************************
        else if(cardName.equals("Animate Dead")) {
        	final Card[] targetC = new Card[1];
        	// need to override what happens when this is cast.
        	final Spell_Permanent animate = new Spell_Permanent(card) {
				private static final long serialVersionUID = 7126615291288065344L;

				public CardList getCreturesInGrave(){
					// This includes creatures Animate Dead can't enchant once in play.
					// The human may try to Animate them, the AI will not.
	       			CardList cList = AllZoneUtil.getCardsInGraveyard();
	       			cList = cList.getType("Creature");
					return cList;
				}
				
                public boolean canPlay() {
                    return super.canPlay() && getCreturesInGrave().size() != 0;
                }
				
				@Override
        		public boolean canPlayAI() {
					CardList cList = getCreturesInGrave();
					// AI will only target something that will stick in play.
        			cList = cList.filter(new CardListFilter() {
        				public boolean addCard(Card crd) {
        					return CardFactoryUtil.canTarget(card, crd);
        				}
        			});
        			if (cList.size() == 0)
        				return false;
        			
        			Card c = CardFactoryUtil.AI_getBestCreature(cList);
        			
        			targetC[0] = c;
        			boolean playable = 2 < c.getNetAttack() && 2 < c.getNetDefense();
        			return playable;
        		}//canPlayAI
				
                @Override
                public void chooseTargetAI() {
                	setTargetCard(targetC[0]);
                }
        	};//addSpellAbility
        	
            Input target = new Input() {
                private static final long serialVersionUID = 9027742835781889044L;
                
                @Override
                public void showMessage() {
                    Object check = AllZone.Display.getChoiceOptional("Select creature", getCreatures());
                    if(check != null) {
                    	animate.setTargetCard((Card) check);
                    	targetC[0] = animate.getTargetCard();
                        stopSetNext(new Input_PayManaCost(animate));
                    } else stop();
                }//showMessage()
                
                public Card[] getCreatures() {
	       			CardList cList = AllZoneUtil.getCardsInGraveyard();
        			cList = cList.getType("Creature");
                    return cList.toArray();
                }
            };//Input
            
        	final Ability attach = new Ability(card, "0") {
				private static final long serialVersionUID = 222308932796127795L;

        		@Override
        		public void resolve() {
                    PlayerZone play = AllZone.getZone(Constant.Zone.Play, card.getController());
         
                    // Animate Dead got destroyed before its ability resolved
                    if (!AllZone.GameAction.isCardInZone(card, play))	
                    	return;
                   
                    Card c = targetC[0];
                    PlayerZone grave = AllZone.getZone(c);

                    if(!AllZone.GameAction.isCardInZone(c, grave)){
                    	// Animated Creature got removed before ability resolve
                    	AllZone.GameAction.sacrifice(card);
                    	return;
                    }
                    
                    // Bring creature into play under your control (should trigger etb Abilities)
                    c.setController(card.getController());
                    grave.remove(c);
                    play.add(c);
                    
                    if(!CardFactoryUtil.canTarget(card, c)) {
                    	// Animated a creature with protection or something similar
                    	AllZone.GameAction.sacrifice(card);
                    	return;
                    }
                    
                    // Everything worked out perfectly.
                    card.enchantCard(c);
                    c.addSemiPermanentAttackBoost(-1);
        		}
        	};//Ability

        	final Command attachCmd = new Command() {
				private static final long serialVersionUID = 3595188622377350327L;

				public void execute() {
					AllZone.Stack.add(attach);
				}
			};
        	
        	final Ability detach = new Ability(card, "0") {

        		@Override
        		public void resolve() {
                    Card c = targetC[0];
                    
                    PlayerZone play = AllZone.getZone(Constant.Zone.Play, card.getController());
                    
                    if(AllZone.GameAction.isCardInZone(c, play)) {
                        c.addSemiPermanentAttackBoost(+1);
                        AllZone.GameAction.sacrifice(c);
                    }
        		}
        	};//Detach

        	final Command detachCmd = new Command() {
				private static final long serialVersionUID = 2425333033834543422L;

				public void execute() {
                    Card c = targetC[0];
                    
                    PlayerZone play = AllZone.getZone(Constant.Zone.Play, card.getController());
                    
                    if(AllZone.GameAction.isCardInZone(c, play))
                    	AllZone.Stack.add(detach);
				}
			};
			
        	card.clearSpellAbility();	// clear out base abilities since we're overriding
			
            animate.setBeforePayMana(target);
            card.addSpellAbility(animate);
			
			attach.setStackDescription("Attaching Animate Dead to animated Creature.");
        	card.addComesIntoPlayCommand(attachCmd);
        	detach.setStackDescription("Animate Dead left play. Sacrificing Animated Creature if still around.");
        	card.addLeavesPlayCommand(detachCmd);
        	card.addUnEnchantCommand(detachCmd);
        }//*************** END ************ END **************************
        
        
        //**************************************************************
        // This card can't be converted to keyword, problem with Fear  *
        //*************** START *********** START **********************
        else if (cardName.equals("Fear")) {
            final SpellAbility spell = new Spell(card) {
				private static final long serialVersionUID = -6430665444443363057L;

				@Override
                public boolean canPlayAI() {
                    CardList list = new CardList(AllZone.Computer_Play.getCards());
                    list = list.getType("Creature");
                    
                    if (list.isEmpty()) return false;
                    
                    //else (is there a Rabid Wombat or a Uril, the Miststalker to target?)
                    
                    CardList auraMagnetList = new CardList(AllZone.Computer_Play.getCards());
                    auraMagnetList = auraMagnetList.getEnchantMagnets();
                    
                    if (! auraMagnetList.isEmpty()) {    // AI has a special target creature(s) to enchant
                        auraMagnetList.shuffle();
                        for (int i = 0; i < auraMagnetList.size(); i++) {
                            if (CardFactoryUtil.canTarget(card, auraMagnetList.get(i))) {
                                setTargetCard(auraMagnetList.get(i));    // Target only Rabid Wombat or Uril, the Miststalker
                    	        return true;
                    	    }
                    	}
                    }
                    
                    /*
                     *  else target another creature
                     *  Do not enchant card with Defender or Fear or enchant card already enchanted
                     */
                    CardListUtil.sortAttack(list);
                    CardListUtil.sortFlying(list);
                    
                    for (int i = 0; i < list.size(); i++) {
                        if (CardFactoryUtil.canTarget(card, list.get(i))  && 
                            !list.get(i).getKeyword().contains("Fear")  && 
                            !list.get(i).getKeyword().contains("Defender") && 
                            !list.get(i).isEnchanted()) {
                            	setTargetCard(list.get(i));
                            	return true;
                        }
                    }
                  
                    return false;
                }//canPlayAI()
                
                @Override
                public void resolve() {
                    PlayerZone play = AllZone.getZone(Constant.Zone.Play, card.getController());
                    play.add(card);
                    
                    Card c = getTargetCard();
                    
                    if (AllZone.GameAction.isCardInPlay(c) && 
                    	CardFactoryUtil.canTarget(card, c)) {
                        	card.enchantCard(c);
                        	Log.debug("Fear", "Enchanted: " + getTargetCard());
                    }
                }//resolve()
            };//SpellAbility
            card.clearSpellAbility();
            card.addSpellAbility(spell);
            
            Command onEnchant = new Command() {
                
				private static final long serialVersionUID = 2754287307356877714L;

				public void execute() {
                    if (card.isEnchanting()) {
                        Card crd = card.getEnchanting().get(0);
                        if (!crd.getIntrinsicKeyword().contains("Fear")) {
                        	crd.addExtrinsicKeyword("Fear");
                        }
                    }
                }//execute()
            };//Command
            

            Command onUnEnchant = new Command() {
                
				private static final long serialVersionUID = 2007362030422979630L;

				public void execute() {
                    if(card.isEnchanting()) {
                        Card crd = card.getEnchanting().get(0);
                        
                        crd.removeExtrinsicKeyword("Fear");
                    }
                    
                }//execute()
            };//Command
            
            Command onLeavesPlay = new Command() {
                
				private static final long serialVersionUID = -8020540432500093584L;

				public void execute() {
                    if (card.isEnchanting()) {
                        Card crd = card.getEnchanting().get(0);
                        card.unEnchantCard(crd);
                    }
                }//execute()
            };//Command
            
            card.addEnchantCommand(onEnchant);
            card.addUnEnchantCommand(onUnEnchant);
            card.addLeavesPlayCommand(onLeavesPlay);
            
            spell.setBeforePayMana(CardFactoryUtil.input_targetCreature(spell));
        }//*************** END ************ END **************************
        
        //*************** START *********** START **************************
        else if (cardName.equals("Entangling Vines") || cardName.equals("Glimmerdust Nap") || 
        		 cardName.equals("Melancholy") || cardName.equals("Mystic Restraints") || 
       		     cardName.equals("Roots") || cardName.equals("Thirst")) {
            
            final SpellAbility spell = new Spell(card) {
				private static final long serialVersionUID = 843412563175285562L;
				
				// for flash, which is not working through the keyword for some reason
				// if not flash then limit to main 1 and 2 on controller's turn and card in hand
				@Override
	            public boolean canPlay() {
	                return (card.getKeyword().contains("Flash") && (AllZone.GameAction.isCardInZone(card, AllZone.Human_Hand) || 
	                        AllZone.GameAction.isCardInZone(card, AllZone.Computer_Hand))
	                            || 
	                       (! card.getKeyword().contains("Flash") && (card.getController().equals(AllZone.Phase.getActivePlayer()) &&
	                       (AllZone.GameAction.isCardInZone(card, AllZone.Human_Hand) || AllZone.GameAction.isCardInZone(card, AllZone.Computer_Hand)) && 
	                       (AllZone.Phase.getPhase().equals(Constant.Phase.Main1) || AllZone.Phase.getPhase().equals(Constant.Phase.Main2)))));
	            }

                @Override
                public boolean canPlayAI() {
                	
                	CardList list = new CardList(AllZone.Human_Play.getCards());    // Target human creature
                	list = list.filter(new CardListFilter() {
                		public boolean addCard(Card c) {
                			return c.isCreature() && CardFactoryUtil.canTarget(card, c) && 
                			      !c.getKeyword().contains("CARDNAME doesn't untap during your untap step.");
                		}
                	});
                	
                	if (card.getKeyword().contains("Enchant tapped creature")) {
                		list = list.filter(new CardListFilter() {
                    		public boolean addCard(Card c) {
                    			return c.isTapped();
                    		}
                    	});
                	}
                	
                	if (card.getKeyword().contains("Enchant creature without flying")) {
                		list = list.filter(new CardListFilter() {
                    		public boolean addCard(Card c) {
                    			return ! c.getKeyword().contains("Flying");
                    		}
                    	});
                	}
                    
                    if (list.isEmpty()) {
                    	return false;
                    } else {
                    	CardListUtil.sortAttack(list);
                    	if (! card.getKeyword().contains("Enchant creature without flying")) {
                    		CardListUtil.sortFlying(list);
                    	}
                        setTargetCard(list.get(0));
                    }
                    return true;
                }//canPlayAI()
                
                @Override
                public void resolve() {
                    PlayerZone play = AllZone.getZone(Constant.Zone.Play, card.getController());
                    play.add(card);
                    
                    Card c = getTargetCard();
                    
                    if (AllZone.GameAction.isCardInPlay(c) && CardFactoryUtil.canTarget(card, c)) {
                    	if (card.getKeyword().contains("When CARDNAME enters the battlefield, tap enchanted creature.")) {
                    		c.tap();
                    	}
                    	card.enchantCard(c);
                    }
                }//resolve()
            };//SpellAbility
            card.clearSpellAbility();
            card.addSpellAbility(spell);
            
            Command onEnchant = new Command() {
				private static final long serialVersionUID = -8694692627290877222L;

				public void execute() {
                    if (card.isEnchanting()) {
                        Card crd = card.getEnchanting().get(0);
                        if (! crd.getKeyword().contains("CARDNAME doesn't untap during your untap step."))
                        	crd.addExtrinsicKeyword("CARDNAME doesn't untap during your untap step.");
                    }
                }//execute()
            };//Command
            
            Command onUnEnchant = new Command() {
				private static final long serialVersionUID = -8271629765371049921L;

				public void execute() {
                    if(card.isEnchanting()) {
                        Card crd = card.getEnchanting().get(0);
                        crd.removeExtrinsicKeyword("CARDNAME doesn't untap during your untap step.");
                    }
                    
                }//execute()
            };//Command
            
            Command onLeavesPlay = new Command() {
				private static final long serialVersionUID = -8694692627290877222L;

				public void execute() {
                    if(card.isEnchanting()) {
                        Card crd = card.getEnchanting().get(0);
                        card.unEnchantCard(crd);
                    }
                }
            };
            
            Input runtime = new Input() {
				private static final long serialVersionUID = 5974269912215230241L;

				@Override
                public void showMessage() {
                    PlayerZone comp = AllZone.getZone(Constant.Zone.Play, Constant.Player.Computer);
                    PlayerZone hum = AllZone.getZone(Constant.Zone.Play, Constant.Player.Human);
                    CardList creatures = new CardList();
                    creatures.addAll(comp.getCards());
                    creatures.addAll(hum.getCards());
                    creatures = creatures.filter(new CardListFilter() {
                        public boolean addCard(Card c) {
                            return c.isCreature() && CardFactoryUtil.canTarget(card, c);
                        }
                    });
                    
                    String instruction = "Select target creature";
                    
                    if (card.getKeyword().contains("Enchant tapped creature")) {
                    	instruction = "Select target tapped creature";
                        creatures = creatures.filter(new CardListFilter() {
                            public boolean addCard(Card c) {
                                return c.isTapped();
                            }
                        });
                    }
                    
                    if (card.getKeyword().contains("Enchant creature without flying")) {
                    	instruction = "Select target creature without flying";
                        creatures = creatures.filter(new CardListFilter() {
                            public boolean addCard(Card c) {
                                return ! c.getKeyword().contains("Flying");
                            }
                        });
                    }
                    
                    stopSetNext(CardFactoryUtil.input_targetSpecific(spell, creatures, instruction, true, false));
                }
            };
            card.addEnchantCommand(onEnchant);
            card.addUnEnchantCommand(onUnEnchant);
            card.addLeavesPlayCommand(onLeavesPlay);
            
            spell.setBeforePayMana(runtime);
        }//*************** END ************ END **************************
        
        
        ///////////////////////////////////////////////////////////////////
        ////
        //// CAUTION: Keep this last in the if else if block for cardnames
        ////
        ///////////////////////////////////////////////////////////////////
        
        ////////////////////DRF test generic aura
        //*************** START *********** START **************************
        else if(isAuraType(card, "Land") || isAuraType(card, "Creature") ||
        		isAuraType(card, "Artifact") || isAuraType(card, "Enchantment")) {
        	
        	final String type = getAuraType(card);
        	final boolean curse = isCurseAura(card);
        	if("" == type) {
        		Log.error("Problem in generic Aura code - type is null");
        	}
        	final SpellAbility spell = new Spell(card) {
				private static final long serialVersionUID = 4191777361540717307L;

				@Override
        		public boolean canPlayAI() {
					String player;
					if(curse) {
						player = Constant.Player.Human;
					}
					else {
						player = Constant.Player.Computer;
					}
        			CardList list = AllZoneUtil.getPlayerTypeInPlay(player, type);

        			if(list.isEmpty()) return false;
        			
        			//TODO - maybe do something intelligent here if it's not a curse, like
        			//checking the aura magnet list
        			setTargetCard(list.get(0));
        			return true;
        		}//canPlayAI()

        		@Override
        		public void resolve() {
        			PlayerZone play = AllZone.getZone(Constant.Zone.Play, card.getController());
                    play.add(card);
                    
                    Card c = getTargetCard();
                    if(AllZone.GameAction.isCardInPlay(c) && CardFactoryUtil.canTarget(card, c)) card.enchantCard(c);                  
        		}//resolve()
        	};//SpellAbility
        	card.clearSpellAbility();
        	card.addSpellAbility(spell);
        	card.addLeavesPlayCommand(standardUnenchant);

        	Input runtime = new Input() {
				private static final long serialVersionUID = -7100800261954421849L;

				@Override
        		public void showMessage() {
        			CardList land = AllZoneUtil.getTypeInPlay(type);
        			stopSetNext(CardFactoryUtil.input_targetSpecific(spell, land,
        					"Select target "+type.toLowerCase(), true, false));
        		}
        	};
        	spell.setBeforePayMana(runtime);
        }//*************** END ************ END **************************
        
        ///////////////////////////////////////////////////////////////////
        ////
        //// CAUTION: Keep the above code block last in the if else if block
        ////
        ///////////////////////////////////////////////////////////////////
        ////////////////////DRF test generic aura
        

        
        /*
         *   This section is for cards which add a P/T boost
         *   and/or keywords to the enchanted creature
         */
        if (shouldEnchant(card) != -1) {
            int n = shouldEnchant(card);
            if (n != -1) {
                String parse = card.getKeyword().get(n).toString();
                card.removeIntrinsicKeyword(parse);
                
                String k[] = parse.split(":");
                String keywordsUnsplit = "";
                String extrinsicKeywords[] = {"none"};    // for enchantments with no keywords to add
                
                final String spDesc[] = {"none"};
                final String stDesc[] = {"none"};
                StringBuilder sbD = new StringBuilder();
                StringBuilder sbSD = new StringBuilder();
                final boolean curse[] = {false};
                
                curse[0] = k[0].contains("Curse");

                int Power = 0;
                int Tough = 0;
                
                sbD.append("Enchanted creature ");
                
                String ptk[] = k[1].split("/");
                
                if (ptk.length == 1)     // keywords in first cell
                {
                    keywordsUnsplit = ptk[0];
                }
                
                else    // parse the power/toughness boosts in first two cells
                {
                    sbD.append("gets ");
                    sbD.append(ptk[0].trim());
                    sbD.append("/");
                    sbD.append(ptk[1].trim());
                    
                    for (int i = 0; i < 2; i ++)
                    {
                        if (ptk[i].matches("[\\+\\-][0-9]+")) ptk[i] =ptk[i].replace("+", "");
                    }
                    Power = Integer.parseInt(ptk[0].trim());
                    Tough = Integer.parseInt(ptk[1].trim());
                    
                    if (ptk.length > 2)     // keywords in third cell
                    {
                        keywordsUnsplit = ptk[2];
                        sbD.append(" and ");
                    }
                }
                
                if (keywordsUnsplit.length() > 0)    // then there is at least one extrinsic keyword to assign
                {
                    sbD.append("has ");
                    
                    String tempKwds[] = keywordsUnsplit.split("&");
                    extrinsicKeywords = new String[tempKwds.length];
                    
                    for (int i = 0; i < tempKwds.length; i ++)
                    {
                        extrinsicKeywords[i] = tempKwds[i].trim();
                        
                        sbD.append(extrinsicKeywords[i].toLowerCase());
                        if (i < tempKwds.length - 2)    {    sbD.append(", ");    }
                        if (i == tempKwds.length - 2)    {    sbD.append(" and ");    }
                    }
                }
                sbD.append(".");
                spDesc[0] = sbD.toString();
                
                sbSD.append(cardName);
                sbSD.append(" - ");
                sbSD.append("enchants target creature.");
                stDesc[0] = sbSD.toString();
                
                if (k.length > 2)    {    spDesc[0] = k[2].trim();    }    // Use the spell and stack descriptions included
                if (k.length > 3)    {    stDesc[0] = k[3].trim();    }    // with the keyword if they are present.
                
                card.clearSpellAbility();
                
                if (! curse[0]) {
                    card.addSpellAbility(CardFactoryUtil.enPump_Enchant(card, Power, Tough, extrinsicKeywords, spDesc, stDesc));
                }
                else {
                    card.addSpellAbility(CardFactoryUtil.enPumpCurse_Enchant(card, Power, Tough, extrinsicKeywords, spDesc, stDesc));
                }
                card.addEnchantCommand(CardFactoryUtil.enPump_onEnchant(card, Power, Tough, extrinsicKeywords, spDesc, stDesc));
                card.addUnEnchantCommand(CardFactoryUtil.enPump_unEnchant(card, Power, Tough, extrinsicKeywords, spDesc, stDesc));
                card.addLeavesPlayCommand(CardFactoryUtil.enPump_LeavesPlay(card, Power, Tough, extrinsicKeywords, spDesc, stDesc));
                
                card.setSVar("PlayMain1", "TRUE");                
            }
        }// enPump[Curse]
        
        /*
         *  For Control Magic type of auras
         */
        if (shouldControl(card) != -1) {
            int n = shouldControl(card);
            if (n != -1) {
                String parse = card.getKeyword().get(n).toString();
                card.removeIntrinsicKeyword(parse);
                String k[] = parse.split(":");
                
                String option = "none";
                boolean optionUnTapLands = false;
                final boolean optionCmcTwoOrLess[] = {false};
                final boolean optionRedOrGreen[] = {false};
                
                /*
                 *  Check to see if these are options and/or descriptions
                 *  Descriptions can be added later if needed
                 */
                if (k.length > 1) {
                	if (k[1].startsWith("Option=")) {
                		option = k[1].substring(7);
                	}
                }
                
                /*
                 *  This is for the Treachery aura
                 *  no need to parse the number of lands at this time
                 */
                if (option.contains("untap up to 5 lands") || 
                		card.getKeyword().contains("When CARDNAME enters the battlefield, untap up to five lands.")) {
                	optionUnTapLands = true;
                }
                
                /* 
                 *  This is for the Threads of Disloyalty aura
                 *  no need to parse the CMC number at this time
                 */
                if (option.contains("CMC 2 or less") || 
                		card.getKeyword().contains("Enchant creature with converted mana cost 2 or less")) {
                	optionCmcTwoOrLess[0] = true;
                }
                
                /*
                 *  This is for the Mind Harness aura
                 *  no need to parse the colors at this time
                 */
                if (option.contains("red or green") || 
                		card.getKeyword().contains("Enchant red or green creature")) {
                	optionRedOrGreen[0] = true;
                }
                
                /*
                 *  I borrowed the code from Peregrine Drake
                 */
                final Input untap = new Input() {
					private static final long serialVersionUID = -2208979487849617156L;
					int                       stop             = 5;
                    int                       count            = 0;
                    
                    @Override
                    public void showMessage() {
                        AllZone.Display.showMessage("Select a land to untap");
                        ButtonUtil.enableOnlyCancel();
                    }
                    
                    @Override
                    public void selectButtonCancel() {
                        stop();
                    }
                    
                    @Override
                    public void selectCard(Card card, PlayerZone zone) {
                        if(card.isLand() && zone.is(Constant.Zone.Play)) {
                            card.untap();
                            count++;
                            if(count == stop) stop();
                        }
                    }//selectCard()
                };// Input untap
                
                final SpellAbility untapAbility = new Ability(card, "0") {
                    @Override
                    public void resolve() {
                        if(card.getController().equals(Constant.Player.Human)) AllZone.InputControl.setInput(untap);
                        else {
                            CardList list = new CardList(AllZone.Computer_Play.getCards());
                            list = list.filter(new CardListFilter() {
                                public boolean addCard(Card c) {
                                    return c.isLand() && c.isTapped();
                                }
                            });
                            for(int i = 0; i < 5 && i < list.size(); i++)
                                list.get(i).untap();
                        }//else
                    }//resolve()
                };// untapAbility
                
                Command intoPlay = new Command() {
					private static final long serialVersionUID = -3310362768233358111L;

					public void execute() {
                    	untapAbility.setStackDescription(card.getController() + " untaps up to 5 lands.");
                        AllZone.Stack.add(untapAbility);
                    }
                };
                if (optionUnTapLands) {
                	card.addComesIntoPlayCommand(intoPlay);
                }
                
                /*
                 *  I borrowed this code from Control Magic
                 */
                final SpellAbility spell = new Spell(card) {
					private static final long serialVersionUID = 5211276723523636356L;

					@Override
                    public boolean canPlayAI() {
                        CardList tgts = CardFactoryUtil.AI_getHumanCreature(card, true);
                        CardListUtil.sortAttack(tgts);
                        CardListUtil.sortFlying(tgts);
                        
                        /*
                         *  This is a new addition and is used
                         *  by the Threads of Disloyalty aura
                         */
                        if (optionCmcTwoOrLess[0]) {
                        	tgts = tgts.filter(new CardListFilter() {
                        		public boolean addCard(Card c) {
                        			return CardUtil.getConvertedManaCost(c.getManaCost()) <= 2;
                        		}
                        	});
                        	if (tgts.isEmpty()) return false;
                        	else {
                        		CardListUtil.sortAttack(tgts);
                                CardListUtil.sortFlying(tgts);
                        		setTargetCard(tgts.get(0));
                        		return true;
                        	}
                        }
                        
                        /*
                         *  This is a new addition and is
                         *  used by the Mind Harness aura
                         */
                        if (optionRedOrGreen[0]) {
                        	tgts = tgts.filter(new CardListFilter() {
                        		public boolean addCard(Card c) {
                        			return c.isGreen() || c.isRed();
                        		}
                        	});
                        }
                        
                        if (tgts.isEmpty()) return false;
                                                
                        if (2 <= tgts.get(0).getNetAttack() && tgts.get(0).getKeyword().contains("Flying")) {
                            setTargetCard(tgts.get(0));
                            return true;
                        }
                        
                        CardListUtil.sortAttack(tgts);
                        if (4 <= tgts.get(0).getNetAttack()) {
                            setTargetCard(tgts.get(0));
                            return true;
                        }
                        
                        /*
                         *  This is new and we may want to add more tests
                         *  Do we want the AI to hold these auras when
                         *  losing game and at a creature disadvatange
                         */
                        if (3 <= tgts.get(0).getNetAttack() && AllZone.Human_Life.getLife() > AllZone.Computer_Life.getLife()) {
                            setTargetCard(tgts.get(0));
                            return true;
                        }
                        
                        return false;
                    }//canPlayAI()
                    
                    @Override
                    public void resolve() {
                        PlayerZone play = AllZone.getZone(Constant.Zone.Play, card.getController());
                        play.add(card);
                        
                        Card c = getTargetCard();
                        
                        if (AllZone.GameAction.isCardInPlay(c) && CardFactoryUtil.canTarget(card, c)) card.enchantCard(c);
                        
                    }//resolve()
                };//SpellAbility
                card.clearSpellAbility();
                card.addSpellAbility(spell);
                
                Command onEnchant = new Command() {
					private static final long serialVersionUID = -6323085271405286813L;

					public void execute() {
                        if(card.isEnchanting()) {
                            Card crd = card.getEnchanting().get(0);
                            //set summoning sickness
                            if(crd.getKeyword().contains("Haste")) {
                                crd.setSickness(false);
                            } else {
                                crd.setSickness(true);
                            }
                            
                            ((PlayerZone_ComesIntoPlay) AllZone.Human_Play).setTriggers(false);
                            ((PlayerZone_ComesIntoPlay) AllZone.Computer_Play).setTriggers(false);
                            
                            PlayerZone from = AllZone.getZone(crd);
                            from.remove(crd);
                            
                            crd.setController(card.getController());
                            
                            PlayerZone to = AllZone.getZone(Constant.Zone.Play, card.getController());
                            to.add(crd);
                            
                            ((PlayerZone_ComesIntoPlay) AllZone.Human_Play).setTriggers(true);
                            ((PlayerZone_ComesIntoPlay) AllZone.Computer_Play).setTriggers(true);
                        }
                    }//execute()
                };//Command
                
                Command onUnEnchant = new Command() {
					private static final long serialVersionUID = -3086710987052359078L;

					public void execute() {
                        if(card.isEnchanting()) {
                            Card crd = card.getEnchanting().get(0);
                            if(AllZone.GameAction.isCardInPlay(crd)) {
                                if(crd.getKeyword().contains("Haste")) {
                                    crd.setSickness(false);
                                } else {
                                    crd.setSickness(true);
                                }
                                
                                ((PlayerZone_ComesIntoPlay) AllZone.Human_Play).setTriggers(false);
                                ((PlayerZone_ComesIntoPlay) AllZone.Computer_Play).setTriggers(false);
                                
                                PlayerZone from = AllZone.getZone(crd);
                                from.remove(crd);
                                
                                AllZone.Combat.removeFromCombat(crd);
                                
                                String opp = AllZone.GameAction.getOpponent(crd.getController());
                                crd.setController(opp);
                                
                                PlayerZone to = AllZone.getZone(Constant.Zone.Play, opp);
                                to.add(crd);
                                
                                ((PlayerZone_ComesIntoPlay) AllZone.Human_Play).setTriggers(true);
                                ((PlayerZone_ComesIntoPlay) AllZone.Computer_Play).setTriggers(true);
                            }
                        }
                        
                    }//execute()
                };//Command
                
                Command onLeavesPlay = new Command() {
					private static final long serialVersionUID = 7464815269438815827L;

					public void execute() {
                        if(card.isEnchanting()) {
                            Card crd = card.getEnchanting().get(0);
                            card.unEnchantCard(crd);
                        }
                    }
                };//Command
                
                /*
                 *  We now need an improved input method so we can filter out the inappropriate
                 *  choices for the auras Threads of Disloyalty and Mind Harness
                 * 
                 *  NOTE: can we target a creature in our zone ?
                 */
                Input runtime = new Input() {
                    private static final long serialVersionUID = -7462101446917907106L;

                    @Override
                    public void showMessage() {
                        PlayerZone comp = AllZone.getZone(Constant.Zone.Play, Constant.Player.Computer);
                        PlayerZone hum = AllZone.getZone(Constant.Zone.Play, Constant.Player.Human);
                        CardList creatures = new CardList();
                        creatures.addAll(comp.getCards());
                        creatures.addAll(hum.getCards());
                        creatures = creatures.filter(new CardListFilter() {
                            public boolean addCard(Card c) {
                                return c.isCreature() && CardFactoryUtil.canTarget(card, c) && 
                                ((!optionCmcTwoOrLess[0]) || (optionCmcTwoOrLess[0] && CardUtil.getConvertedManaCost(c.getManaCost()) <= 2)) && 
                                ((!optionRedOrGreen[0]) || (optionRedOrGreen[0] && 
                                	c.isGreen() || c.isRed()));
                            }
                        });
                        
                        stopSetNext(CardFactoryUtil.input_targetSpecific(spell, creatures, "Select target creature", true, false));
                    }
                };
                
                card.setSVar("PlayMain1", "TRUE");
                
                card.addEnchantCommand(onEnchant);
                card.addUnEnchantCommand(onUnEnchant);
                card.addLeavesPlayCommand(onLeavesPlay);
                
                spell.setBeforePayMana(runtime);
            }// SpellAbility spell
        }// enControlCreature
        
        // Cards with Cycling abilities
        // -1 means keyword "Cycling" not found
        if(shouldCycle(card) != -1) {
            int n = shouldCycle(card);
            if(n != -1) {
                String parse = card.getKeyword().get(n).toString();
                card.removeIntrinsicKeyword(parse);
                
                String k[] = parse.split(":");
                final String manacost = k[1];
                
                card.addSpellAbility(CardFactoryUtil.ability_cycle(card, manacost));
            }
        }//Cycling
        
        return card;
    }
    
    //checks if an aura is a given type based on: Enchant <type> in cards.txt
    private static boolean isAuraType(final Card aura, final String type) {
    	ArrayList<String> keywords = aura.getKeyword();
    	for(String keyword:keywords) {
    		if(keyword.startsWith("Enchant "+type)) {
    			return true;
    		}
    	}
    	return false;
    }
    
    //gets the type of aura based on Enchant <type> in cards.txt
    private static String getAuraType(final Card aura) {
    	ArrayList<String> keywords = aura.getKeyword();
    	for(String keyword:keywords) {
    		if(keyword.startsWith("Enchant ")) {
    			StringTokenizer st = new StringTokenizer(keyword);
    			st.nextToken(); //this should be "Enchant"
    			return st.nextToken();  //should be "land", "artifact", etc
    		}
    	}
    	return "";
    }
    
    //checks if an aura is a curse based on Enchant <type> [Curse] in cards.txt
    //Curse just means computer will target human's stuff with this
    private static boolean isCurseAura(final Card aura) {
    	ArrayList<String> keywords = aura.getKeyword();
    	for(String keyword:keywords) {
    		if(keyword.startsWith("Enchant ")) {
    			if(keyword.endsWith("Curse")) return true;
    		}
    	}
    	return false;
    }
    
}
