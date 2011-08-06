
package forge;


import java.util.ArrayList;


class CardFactory_Equipment {
    
    public static int shouldEquip(Card c) {
        ArrayList<String> a = c.getKeyword();
        for(int i = 0; i < a.size(); i++)
        	
        	// Keyword renamed to eqPump, was VanillaEquipment
            if(a.get(i).toString().startsWith("eqPump")) return i;
        
        return -1;
    }
    
    public static Card getCard(final Card card, String cardName, String owner) {
    	
        //*************** START *********** START **************************
        if(cardName.equals("Sword of the Meek")) {
            final Ability equip = new Ability(card, "2") {
                @Override
                public void resolve() {
                    if(AllZone.GameAction.isCardInPlay(getTargetCard())
                            && CardFactoryUtil.canTarget(card, getTargetCard())) {
                        if(card.isEquipping()) {
                            Card crd = card.getEquipping().get(0);
                            if(crd.equals(getTargetCard())) return;
                            
                            card.unEquipCard(crd);
                        }
                        card.equipCard(getTargetCard());
                    }
                }
                
                @Override
                public boolean canPlay() {
                    return AllZone.getZone(card).is(Constant.Zone.Play)
                            && AllZone.Phase.getActivePlayer().equals(card.getController())
                            && (AllZone.Phase.getPhase().equals("Main1") || AllZone.Phase.getPhase().equals(
                                    "Main2"));
                }
                
                @Override
                public boolean canPlayAI() {
                    return getCreature().size() != 0 && !card.isEquipping();
                }
                
                
                @Override
                public void chooseTargetAI() {
                    Card target = CardFactoryUtil.AI_getBestCreature(getCreature());
                    setTargetCard(target);
                }
                
                CardList getCreature() {
                    CardList list = new CardList(AllZone.Computer_Play.getCards());
                    list = list.filter(new CardListFilter() {
                        public boolean addCard(Card c) {
                            return c.isCreature() && (!CardFactoryUtil.AI_doesCreatureAttack(c))
                                    && CardFactoryUtil.canTarget(card, c)
                                    && (!c.getKeyword().contains("Defender"));
                        }
                    });
                    // list.remove(card);      // if mana-only cost, allow self-target
                    
                    // is there at least 1 Loxodon Punisher to target
                    
                    CardList equipMagnetList = list.getName("Loxodon Punisher");
                    if (equipMagnetList.size() != 0) {
                        return equipMagnetList;
                    }
                    
                    return list;
                }//getCreature()
                
            };//equip ability
            

            Command onEquip = new Command() {
                private static final long serialVersionUID = -1783065127683640831L;
                
                public void execute() {
                    if(card.isEquipping()) {
                        Card crd = card.getEquipping().get(0);
                        crd.addSemiPermanentAttackBoost(1);
                        crd.addSemiPermanentDefenseBoost(2);
                    }
                }//execute()
            };//Command
            

            Command onUnEquip = new Command() {
                
                private static final long serialVersionUID = -754739553859502626L;
                
                public void execute() {
                    if(card.isEquipping()) {
                        Card crd = card.getEquipping().get(0);
                        crd.addSemiPermanentAttackBoost(-1);
                        crd.addSemiPermanentDefenseBoost(-2);
                        
                    }
                    
                }//execute()
            };//Command
            
            equip.setBeforePayMana(CardFactoryUtil.input_equipCreature(equip));
            
            equip.setDescription("Equip: 2");
            card.addSpellAbility(equip);
            
            card.addEquipCommand(onEquip);
            card.addUnEquipCommand(onUnEquip);
            
        } //*************** END ************ END **************************

        
    	
        //*************** START *********** START **************************
        else if(cardName.equals("Umbral Mantle")) {
            final Ability equip = new Ability(card, "0") {
                @Override
                public void resolve() {
                    if(AllZone.GameAction.isCardInPlay(getTargetCard())
                            && CardFactoryUtil.canTarget(card, getTargetCard())) {
                        if(card.isEquipping()) {
                            Card crd = card.getEquipping().get(0);
                            if(crd.equals(getTargetCard())) return;
                            
                            card.unEquipCard(crd);
                        }
                        
                        card.equipCard(getTargetCard());
                    }
                }
                
                @Override
                public boolean canPlay() {
                    return AllZone.getZone(card).is(Constant.Zone.Play)
                            && AllZone.Phase.getActivePlayer().equals(card.getController())
                            && !AllZone.Phase.getPhase().equals("End of Turn")
                            && !AllZone.Phase.getPhase().equals(
                                    Constant.Phase.Combat_Declare_Blockers_InstantAbility);
                }
                
                
                @Override
                public boolean canPlayAI() {
                    return false;
                }
            };//equip ability
            
            equip.setType("Extrinsic");
            
            final Ability untapboost = new Ability(card, "3") {
            	Command EOT(final Card c){return new Command() {
                    private static final long serialVersionUID = -8840812331316327448L;
                    
                    public void execute() {
                        if(AllZone.GameAction.isCardInPlay(getSourceCard())) {
                            c.addTempAttackBoost(-2);
                            c.addTempDefenseBoost(-2);
                        }
                        
                    }
                };}
                @Override
                public void resolve() {
                    getSourceCard().addTempAttackBoost(2);
                    getSourceCard().addTempDefenseBoost(2);
                    AllZone.EndOfTurn.addUntil(EOT(getSourceCard()));
                }
                
                @Override
                public boolean canPlay() {
                    return (getSourceCard().isTapped() && !getSourceCard().hasSickness() && super.canPlay());
                }
            };//equiped creature's ability
            untapboost.makeUntapAbility();
            Command onEquip = new Command() {
                
                private static final long serialVersionUID = -4784079305541955698L;
                
                public void execute() {
                    if(card.isEquipping()) {
                        Card crd = card.getEquipping().get(0);
                        untapboost.setDescription("3, Untap: " + crd + " gets +2/+2 until end of turn");
                        untapboost.setStackDescription(crd + " - +2/+2 until EOT");
                        
                        crd.addSpellAbility(untapboost);
                    }
                }//execute()
            };//Command
            

            Command onUnEquip = new Command() {
                private static final long serialVersionUID = -3427116314295067303L;
                
                public void execute() {
                    if(card.isEquipping()) {
                        Card crd = card.getEquipping().get(0);
                        crd.removeSpellAbility(untapboost);
                    }
                    
                }//execute()
            };//Command
            
            equip.setBeforePayMana(CardFactoryUtil.input_equipCreature(equip));
            
            equip.setDescription("Equip: 0");
            card.addSpellAbility(equip);
            
            card.addEquipCommand(onEquip);
            card.addUnEquipCommand(onUnEquip);
        } //*************** END ************ END **************************  
        
        
        
        //*************** START *********** START **************************
        else if(cardName.equals("Umezawa's Jitte")) {
            final Ability equip = new Ability(card, "2") {
                @Override
                public void resolve() {
                    if(AllZone.GameAction.isCardInPlay(getTargetCard())
                            && CardFactoryUtil.canTarget(card, getTargetCard())) {
                        if(card.isEquipping()) {
                            Card crd = card.getEquipping().get(0);
                            if(crd.equals(getTargetCard())) return;
                            
                            card.unEquipCard(crd);
                        }
                        card.equipCard(getTargetCard());
                    }
                }
                
                @Override
                public boolean canPlay() {
                    return AllZone.getZone(card).is(Constant.Zone.Play)
                            && AllZone.Phase.getActivePlayer().equals(card.getController())
                            && (AllZone.Phase.getPhase().equals("Main1") || AllZone.Phase.getPhase().equals(
                                    "Main2"));
                }
                
                @Override
                public boolean canPlayAI() {
                    return getCreature().size() != 0 && !card.isEquipping();
                }
                
                @Override
                public void chooseTargetAI() {
                    Card target = CardFactoryUtil.AI_getBestCreature(getCreature());
                    setTargetCard(target);
                }
                
                CardList getCreature() {
                    CardList list = new CardList(AllZone.Computer_Play.getCards());
                    list = list.filter(new CardListFilter() {
                        public boolean addCard(Card c) {
                            return c.isCreature() && (!CardFactoryUtil.AI_doesCreatureAttack(c))
                                    && CardFactoryUtil.canTarget(card, c)
                                    && (!c.getKeyword().contains("Defender"));
                        }
                    });
                    // list.remove(card);      // if mana-only cost, allow self-target
                    
                    // is there at least 1 Loxodon Punisher to target
                    
                    CardList equipMagnetList = list.getName("Loxodon Punisher");
                    if (equipMagnetList.size() != 0) {
                        return equipMagnetList;
                    }
                    
                    return list;
                }//getCreature()
                
            };//equip ability
            
            final Ability gainLife = new Ability(card, "0") {
                @Override
                public void resolve() {
                    card.subtractCounter(Counters.CHARGE, 1);
                    String player = card.getController();
                    PlayerLife life = AllZone.GameAction.getPlayerLife(player);
                    life.addLife(2);
                }
                
                @Override
                public boolean canPlay() {
                    SpellAbility sa;
                    for(int i = 0; i < AllZone.Stack.size(); i++) {
                        sa = AllZone.Stack.peek(i);
                        if(sa.getSourceCard().equals(card)) return false;
                    }
                    
                    return card.getCounters(Counters.CHARGE) > 0;
                }
                
                @Override
                public boolean canPlayAI() {
                    return AllZone.GameAction.getPlayerLife(Constant.Player.Computer).getLife() <= 4;
                }
                
            };
            
            gainLife.setDescription("Remove a charge counter from Umezawa's Jitte: You gain 2 life.");
            gainLife.setStackDescription(cardName + " - You gain 2 life.");
            
            final Ability negBoost = new Ability(card, "0") {
                @Override
                public void resolve() {
                    
                    card.subtractCounter(Counters.CHARGE, 1);
                    final Card[] target = new Card[1];
                    final Command untilEOT = new Command() {
                        private static final long serialVersionUID = -1615047325868708734L;
                        
                        public void execute() {
                            if(AllZone.GameAction.isCardInPlay(target[0])) {
                                target[0].addTempAttackBoost(1);
                                target[0].addTempDefenseBoost(1);
                            }
                        }
                    };
                    
                    target[0] = getTargetCard();
                    if(AllZone.GameAction.isCardInPlay(target[0]) && CardFactoryUtil.canTarget(card, target[0])) {
                        target[0].addTempAttackBoost(-1);
                        target[0].addTempDefenseBoost(-1);
                        
                        AllZone.EndOfTurn.addUntil(untilEOT);
                    }
                }
                
                @Override
                public boolean canPlay() {
                    SpellAbility sa;
                    for(int i = 0; i < AllZone.Stack.size(); i++) {
                        sa = AllZone.Stack.peek(i);
                        if(sa.getSourceCard().equals(card)) return false;
                    }
                    
                    return card.getCounters(Counters.CHARGE) > 0;
                }
                
                @Override
                public boolean canPlayAI() {
                    if(gainLife.canPlayAI()) return false;
                    
                    CardList c = CardFactoryUtil.AI_getHumanCreature(1, card, true);
                    CardListUtil.sortAttack(c);
                    CardListUtil.sortFlying(c);
                    
                    if(c.isEmpty()) return false;
                    else {
                        setTargetCard(c.get(0));
                        return true;
                    }
                }//canPlayAI()
            };
            Input target = new Input() {
                private static final long serialVersionUID = -5404464532726469761L;
                
                @Override
                public void showMessage() {
                    AllZone.Display.showMessage("Select target creature for " + card.getName());
                    ButtonUtil.enableOnlyCancel();
                }
                
                @Override
                public void selectButtonCancel() {
                    stop();
                }
                
                @Override
                public void selectCard(Card card, PlayerZone zone) {
                    if(!CardFactoryUtil.canTarget(negBoost, card)) {
                        AllZone.Display.showMessage("Cannot target this card (Shroud? Protection?).");
                    } else if(card.isCreature() && zone.is(Constant.Zone.Play)) {
                        negBoost.setTargetCard(card);
                        AllZone.Stack.add(negBoost);
                        stop();
                    }
                }
            };//Input
            negBoost.setDescription("Remove a charge counter from Umezawa's Jitte: Target creature gets -1/-1 until end of turn");
            negBoost.setBeforePayMana(target);
            

            final Ability boost = new Ability(card, "0") {
                @Override
                public void resolve() {
                    card.subtractCounter(Counters.CHARGE, 1);
                    final Card[] target = new Card[1];
                    final Command untilEOT = new Command() {
                        private static final long serialVersionUID = 2751279830522020186L;
                        
                        public void execute() {
                            if(AllZone.GameAction.isCardInPlay(target[0])) {
                                target[0].addTempAttackBoost(-2);
                                target[0].addTempDefenseBoost(-2);
                            }
                        }
                    };
                    
                    target[0] = card.getEquipping().get(0);
                    if(AllZone.GameAction.isCardInPlay(target[0])) {
                        target[0].addTempAttackBoost(2);
                        target[0].addTempDefenseBoost(2);
                        
                        AllZone.EndOfTurn.addUntil(untilEOT);
                    }
                }
                
                @Override
                public boolean canPlay() {
                    SpellAbility sa;
                    for(int i = 0; i < AllZone.Stack.size(); i++) {
                        sa = AllZone.Stack.peek(i);
                        if(sa.getSourceCard().equals(card)) return false;
                    }
                    
                    return card.isEquipping() && card.getCounters(Counters.CHARGE) > 0;
                }
                
                @Override
                public boolean canPlayAI() {
                    if(gainLife.canPlayAI() || negBoost.canPlayAI()) return false;
                    
                    if(card.isEquipping()) {
                        Card c = card.getEquipping().get(0);
                        if(CardFactoryUtil.AI_doesCreatureAttack(c)) return true;
                        
                    }
                    return false;
                }
            };
            
            boost.setDescription("Remove a charge counter from Umezawa's Jitte: Equipped creature gets +2/+2 until end of turn.");
            boost.setStackDescription(cardName + " - Equipped creature gets +2/+2 untin end of turn.");
            
            equip.setBeforePayMana(CardFactoryUtil.input_equipCreature(equip));
            equip.setDescription("Equip: 2");
            card.addSpellAbility(equip);
            card.addSpellAbility(boost);
            card.addSpellAbility(negBoost);
            card.addSpellAbility(gainLife);
            
        } //*************** END ************ END **************************
        
        //*************** START *********** START **************************
        if(cardName.equals("Hedron Matrix")) {
        	/*
        	 * Equipped creature gets +X/+X, where X is its converted mana cost.
        	 */
        	final Ability equip = new Ability(card, "4") {

        		//not changed
        		@Override
        		public void resolve() {
        			if(AllZone.GameAction.isCardInPlay(getTargetCard())
        					&& CardFactoryUtil.canTarget(card, getTargetCard())) {
        				if(card.isEquipping()) {
        					Card crd = card.getEquipping().get(0);
        					if(crd.equals(getTargetCard())) return;

        					card.unEquipCard(crd);
        				}
        				card.equipCard(getTargetCard());
        			}
        		}

        		//not changed
        		@Override
        		public boolean canPlay() {
        			return AllZone.getZone(card).is(Constant.Zone.Play)
        			&& AllZone.Phase.getActivePlayer().equals(card.getController())
        			&& (AllZone.Phase.getPhase().equals("Main1") || AllZone.Phase.getPhase().equals(
        			"Main2"));
        		}

        		//not changed
        		@Override
        		public boolean canPlayAI() {
        			return getCreature().size() != 0 && !card.isEquipping();
        		}

        		//not changed
        		@Override
        		public void chooseTargetAI() {
        			Card target = CardFactoryUtil.AI_getBestCreature(getCreature());
        			setTargetCard(target);
        		}

        		//not changed
        		CardList getCreature() {
        			CardList list = new CardList(AllZone.Computer_Play.getCards());
        			list = list.filter(new CardListFilter() {
        				public boolean addCard(Card c) {
        					return c.isCreature() && (!CardFactoryUtil.AI_doesCreatureAttack(c))
        					&& CardFactoryUtil.canTarget(card, c)
        					&& (!c.getKeyword().contains("Defender"));
        				}
        			});

        			// is there at least 1 Loxodon Punisher to target
        			CardList equipMagnetList = list.getName("Loxodon Punisher");
        			if (equipMagnetList.size() != 0) {
        				return equipMagnetList;
        			}

        			return list;
        		}//getCreature()

        	};//equip ability


        	Command onEquip = new Command() {
				private static final long serialVersionUID = -5356474407155702171L;

				public void execute() {
        			if(card.isEquipping()) {
        				Card crd = card.getEquipping().get(0);
        				int pump = CardUtil.getConvertedManaCost(crd.getManaCost());
        				crd.addSemiPermanentAttackBoost(pump);
        				crd.addSemiPermanentDefenseBoost(pump);
        			}
        		}//execute()
        	};//Command


        	Command onUnEquip = new Command() {
				private static final long serialVersionUID = 5196262972986079207L;

				public void execute() {
        			if(card.isEquipping()) {
        				Card crd = card.getEquipping().get(0);
        				int pump = CardUtil.getConvertedManaCost(crd.getManaCost());
        				crd.addSemiPermanentAttackBoost(-pump);
        				crd.addSemiPermanentDefenseBoost(-pump);

        			}

        		}//execute()
        	};//Command

        	equip.setBeforePayMana(CardFactoryUtil.input_equipCreature(equip));

        	equip.setDescription("Equip: 4");
        	card.addSpellAbility(equip);

        	card.addEquipCommand(onEquip);
        	card.addUnEquipCommand(onUnEquip);

        } //*************** END ************ END **************************
        
        //*************** START *********** START **************************
        else if(cardName.equals("Adventuring Gear")) {
            final Ability equip = new Ability(card, "1") {
                @Override
                public void resolve() {
                    if(AllZone.GameAction.isCardInPlay(getTargetCard())
                            && CardFactoryUtil.canTarget(card, getTargetCard())) {
                        if(card.isEquipping()) {
                            Card crd = card.getEquipping().get(0);
                            if(crd.equals(getTargetCard())) return;
                            
                            card.unEquipCard(crd);
                        }
                        card.equipCard(getTargetCard());
                    }
                }
                
                @Override
                public boolean canPlay() {
                    return AllZone.getZone(card).is(Constant.Zone.Play)
                            && AllZone.Phase.getActivePlayer().equals(card.getController())
                            && (AllZone.Phase.getPhase().equals("Main1") || AllZone.Phase.getPhase().equals("Main2"));
                }
                
                @Override
                public boolean canPlayAI() {
                    return getCreature().size() != 0 && !card.isEquipping();
                }
                
                @Override
                public void chooseTargetAI() {
                    Card target = CardFactoryUtil.AI_getBestCreature(getCreature());
                    setTargetCard(target);
                }
                
                CardList getCreature() {    // build list and do some pruning
                	CardList list = new CardList(AllZone.Computer_Play.getCards());
                	list = list.filter(new CardListFilter() {
                		public boolean addCard(Card c) {
                			return c.isCreature() && (!CardFactoryUtil.AI_doesCreatureAttack(c))
                			&& CardFactoryUtil.canTarget(card, c)
                			&& (!c.getKeyword().contains("Defender"));
                		}
                	});
                	return list;
                }//getCreature()
                
            };// equip ability

            
            Command onEquip = new Command() {
				private static final long serialVersionUID = -5278473287541239581L;

				public void execute() {
        			if(card.isEquipping()) {
        				Card crd = card.getEquipping().get(0);
        				crd.addStackingExtrinsicKeyword("Landfall - Whenever a land enters the battlefield under your control, CARDNAME gets +2/+2 until end of turn.");
        			}
        		}//execute()
        	};//Command
            
            Command onUnEquip = new Command() {
				private static final long serialVersionUID = -2979834244752321236L;

				public void execute() {
        			if(card.isEquipping()) {
        				Card crd = card.getEquipping().get(0);
        				crd.removeExtrinsicKeyword("Landfall - Whenever a land enters the battlefield under your control, CARDNAME gets +2/+2 until end of turn.");
        			}

        		}//execute()
        	};//Command
            
            equip.setBeforePayMana(CardFactoryUtil.input_equipCreature(equip));
            equip.setDescription("Equip: 1");
            
            card.addSpellAbility(equip);

            card.addEquipCommand(onEquip);
        	card.addUnEquipCommand(onUnEquip);
        }//*************** END ************ END **************************

        
        if (shouldEquip(card) != -1) {
            int n = shouldEquip(card);
            if (n != -1) {
                String parse = card.getKeyword().get(n).toString();
                card.removeIntrinsicKeyword(parse);
                
                String k[] = parse.split(":");
                String tmpCost;
                tmpCost = k[0].substring(6);
                String keywordsUnsplit = "";
                String extrinsicKeywords[] = {"none"};    // for equips with no keywords to add

                final String manaCost = tmpCost.trim();
                int Power = 0;
                int Tough = 0;
                
                String ptk[] = k[1].split("/");
                
                if (ptk.length == 1)     // keywords in first cell
                {
                	keywordsUnsplit = ptk[0];
                }
                
                else // parse the power/toughness boosts in first two cells
                {
                    for (int i = 0; i < 2; i ++)
                    {
                        if (ptk[i].matches("[\\+\\-][0-9]")) ptk[i] =ptk[i].replace("+", "");
                    }
                    Power = Integer.parseInt(ptk[0].trim());
                    Tough = Integer.parseInt(ptk[1].trim());
                    
                    if (ptk.length > 2)     // keywords in third cell
                        keywordsUnsplit = ptk[2];
                }
                
                if (keywordsUnsplit.length() > 0)    // then there is at least one extrinsic keyword to assign
                {
                    String tempKwds[] = keywordsUnsplit.split("&");
                    extrinsicKeywords = new String[tempKwds.length];
                    
                    for (int i = 0; i < tempKwds.length; i ++)
                    {
                        extrinsicKeywords[i] = tempKwds[i].trim();
                    }
                }

                card.addSpellAbility(CardFactoryUtil.eqPump_Equip(card, Power, Tough, extrinsicKeywords, manaCost));
                card.addEquipCommand(CardFactoryUtil.eqPump_onEquip(card, Power, Tough, extrinsicKeywords, manaCost));
                card.addUnEquipCommand(CardFactoryUtil.eqPump_unEquip(card, Power, Tough, extrinsicKeywords, manaCost));
                
            }
        }// eqPump (was VanillaEquipment)
        
        return card;
    }
}
