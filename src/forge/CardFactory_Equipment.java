
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

    
    public static Card getCard(final Card card, String cardName, Player owner) {
    	
        //*************** START *********** START **************************
        if (cardName.equals("Sword of the Meek")) {
            final Ability equip = new Ability(card, "2") {
            	
                @Override
                public void resolve() {
                    if (AllZone.GameAction.isCardInPlay(getTargetCard())
                            && CardFactoryUtil.canTarget(card, getTargetCard())) {
                    	
                        if (card.isEquipping()) {
                            Card crd = card.getEquipping().get(0);
                            if (crd.equals(getTargetCard())) return;
                            
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
                    return getCreature().size() != 0 
                    		&& !card.isEquipping();
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
                            return c.isCreature() 
                            		&& CardFactoryUtil.AI_doesCreatureAttack(c)
                                    && CardFactoryUtil.canTarget(card, c)
                                    && (!c.getKeyword().contains("Defender"));
                        }
                    });
                    
                    // Is there at least 1 Loxodon Punisher and/or Goblin Gaveleer to target
                    CardList equipMagnetList = list;
                    equipMagnetList = equipMagnetList.getEquipMagnets();
                    
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
        else if (cardName.equals("Umbral Mantle")) {
            final Ability equip = new Ability(card, "0") {
            	
                @Override
                public void resolve() {
                    if (AllZone.GameAction.isCardInPlay(getTargetCard())
                            && CardFactoryUtil.canTarget(card, getTargetCard())) {
                    	
                        if (card.isEquipping()) {
                            Card crd = card.getEquipping().get(0);
                            if (crd.equals(getTargetCard())) return;
                            
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
        else if (cardName.equals("Umezawa's Jitte")) {
            final Ability equip = new Ability(card, "2") {
            	
                @Override
                public void resolve() {
                    if (AllZone.GameAction.isCardInPlay(getTargetCard())
                            && CardFactoryUtil.canTarget(card, getTargetCard())) {
                    	
                        if (card.isEquipping()) {
                            Card crd = card.getEquipping().get(0);
                            if (crd.equals(getTargetCard())) return;
                            
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
                    return getCreature().size() != 0 
                    		&& !card.isEquipping();
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
                            return c.isCreature() 
                            		&& CardFactoryUtil.AI_doesCreatureAttack(c)
                                    && CardFactoryUtil.canTarget(card, c)
                                    && (!c.getKeyword().contains("Defender"));
                        }
                    });
                    
                    // Is there at least 1 Loxodon Punisher and/or Goblin Gaveleer to target
                    CardList equipMagnetList = list;
                    equipMagnetList = equipMagnetList.getEquipMagnets();
                    
                    if (equipMagnetList.size() != 0) {
                        return equipMagnetList;
                    }
                    
                    return list;
                }//getCreature()
            };//equip ability

            equip.setBeforePayMana(CardFactoryUtil.input_equipCreature(equip));
            equip.setDescription("Equip: 2");
            card.addSpellAbility(equip);
            
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
        			if (AllZone.GameAction.isCardInPlay(getTargetCard())
        					&& CardFactoryUtil.canTarget(card, getTargetCard())) {
        				
        				if (card.isEquipping()) {
        					Card crd = card.getEquipping().get(0);
        					if (crd.equals(getTargetCard())) return;

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
        					&& (AllZone.Phase.getPhase().equals("Main1") || AllZone.Phase.getPhase().equals("Main2"));
        		}

        		//not changed
        		@Override
        		public boolean canPlayAI() {
        			return getCreature().size() != 0 
        					&& !card.isEquipping();
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
        					return c.isCreature() 
        							&& CardFactoryUtil.AI_doesCreatureAttack(c)
        							&& CardFactoryUtil.canTarget(card, c)
        							&& (!c.getKeyword().contains("Defender"));
        				}
        			});
        			
        			// Is there at least 1 Loxodon Punisher and/or Goblin Gaveleer to target
                    CardList equipMagnetList = list;
                    equipMagnetList = equipMagnetList.getEquipMagnets();

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
        else if (cardName.equals("Adventuring Gear")) {
            final Ability equip = new Ability(card, "1") {
            	
                @Override
                public void resolve() {
                    if (AllZone.GameAction.isCardInPlay(getTargetCard())
                            && CardFactoryUtil.canTarget(card, getTargetCard())) {
                    	
                        if (card.isEquipping()) {
                            Card crd = card.getEquipping().get(0);
                            if (crd.equals(getTargetCard())) return;
                            
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
                    return getCreature().size() != 0 
                    		&& !card.isEquipping();
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
                			return c.isCreature() 
                					&& CardFactoryUtil.AI_doesCreatureAttack(c)
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
        
        
        //*************** START *********** START **************************
        else if (cardName.equals("Sword of Fire and Ice")) {
            final Ability equip = new Ability(card, "2") {
            	
                @Override
                public void resolve() {
                    if (AllZone.GameAction.isCardInPlay(getTargetCard())
                            && CardFactoryUtil.canTarget(card, getTargetCard())) {
                    	
                        if (card.isEquipping()) {
                            Card crd = card.getEquipping().get(0);
                            if (crd.equals(getTargetCard())) return;
                            
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
                    return getCreature().size() != 0 
                    		&& !card.isEquipping();
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
                            return c.isCreature() 
                            		&& CardFactoryUtil.AI_doesCreatureAttack(c)
                                    && CardFactoryUtil.canTarget(card, c)
                                    && (!c.getKeyword().contains("Defender"));
                        }
                    });
                    
                    // Is there at least 1 Loxodon Punisher and/or Goblin Gaveleer to target
                    CardList equipMagnetList = list;
                    equipMagnetList = equipMagnetList.getEquipMagnets();
                    
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
                        crd.addSemiPermanentAttackBoost(2);
                        crd.addSemiPermanentDefenseBoost(2);
                        crd.addExtrinsicKeyword("Protection from red");
                        crd.addExtrinsicKeyword("Protection from blue");
                    }
                }//execute()
            };//Command
            

            Command onUnEquip = new Command() {
                
                private static final long serialVersionUID = -754739553859502626L;
                
                public void execute() {
                    if(card.isEquipping()) {
                        Card crd = card.getEquipping().get(0);
                        crd.addSemiPermanentAttackBoost(-2);
                        crd.addSemiPermanentDefenseBoost(-2);
                        crd.removeExtrinsicKeyword("Protection from red");
                        crd.removeExtrinsicKeyword("Protection from blue");
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
        else if (cardName.equals("Sword of Light and Shadow")) {
            final Ability equip = new Ability(card, "2") {
            	
                @Override
                public void resolve() {
                    if (AllZone.GameAction.isCardInPlay(getTargetCard())
                            && CardFactoryUtil.canTarget(card, getTargetCard())) {
                    	
                        if (card.isEquipping()) {
                            Card crd = card.getEquipping().get(0);
                            if (crd.equals(getTargetCard())) return;
                            
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
                    return getCreature().size() != 0 
                    		&& !card.isEquipping();
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
                            return c.isCreature() 
                            		&& CardFactoryUtil.AI_doesCreatureAttack(c)
                                    && CardFactoryUtil.canTarget(card, c)
                                    && (!c.getKeyword().contains("Defender"));
                        }
                    });
                    
                    // Is there at least 1 Loxodon Punisher and/or Goblin Gaveleer to target
                    CardList equipMagnetList = list;
                    equipMagnetList = equipMagnetList.getEquipMagnets();
                    
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
                        crd.addSemiPermanentAttackBoost(2);
                        crd.addSemiPermanentDefenseBoost(2);
                        crd.addExtrinsicKeyword("Protection from white");
                        crd.addExtrinsicKeyword("Protection from black");
                    }
                }//execute()
            };//Command
            

            Command onUnEquip = new Command() {
                
                private static final long serialVersionUID = -754739553859502626L;
                
                public void execute() {
                    if(card.isEquipping()) {
                        Card crd = card.getEquipping().get(0);
                        crd.addSemiPermanentAttackBoost(-2);
                        crd.addSemiPermanentDefenseBoost(-2);
                        crd.removeExtrinsicKeyword("Protection from white");
                        crd.removeExtrinsicKeyword("Protection from black");
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
        else if (cardName.equals("Sword of Body and Mind")) {
            final Ability equip = new Ability(card, "2") {
            	
                @Override
                public void resolve() {
                    if (AllZone.GameAction.isCardInPlay(getTargetCard())
                            && CardFactoryUtil.canTarget(card, getTargetCard())) {
                    	
                        if (card.isEquipping()) {
                            Card crd = card.getEquipping().get(0);
                            if (crd.equals(getTargetCard())) return;
                            
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
                    return getCreature().size() != 0 
                    		&& !card.isEquipping();
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
                            return c.isCreature() 
                            		&& CardFactoryUtil.AI_doesCreatureAttack(c)
                                    && CardFactoryUtil.canTarget(card, c)
                                    && (!c.getKeyword().contains("Defender"));
                        }
                    });
                    
                    // Is there at least 1 Loxodon Punisher and/or Goblin Gaveleer to target
                    CardList equipMagnetList = list;
                    equipMagnetList = equipMagnetList.getEquipMagnets();
                    
                    if (equipMagnetList.size() != 0) {
                        return equipMagnetList;
                    }
                    
                    return list;
                }//getCreature()
            };//equip ability
            
            Command onEquip = new Command() {
                
				private static final long serialVersionUID = 4792252563711300648L;

				public void execute() {
                    if(card.isEquipping()) {
                        Card crd = card.getEquipping().get(0);
                        crd.addSemiPermanentAttackBoost(2);
                        crd.addSemiPermanentDefenseBoost(2);
                        crd.addExtrinsicKeyword("Protection from green");
                        crd.addExtrinsicKeyword("Protection from blue");
                    }
                }//execute()
            };//Command
            

            Command onUnEquip = new Command() {
                
				private static final long serialVersionUID = 6204739827947031589L;

				public void execute() {
                    if(card.isEquipping()) {
                        Card crd = card.getEquipping().get(0);
                        crd.addSemiPermanentAttackBoost(-2);
                        crd.addSemiPermanentDefenseBoost(-2);
                        crd.removeExtrinsicKeyword("Protection from green");
                        crd.removeExtrinsicKeyword("Protection from blue");
                    }
                    
                }//execute()
            };//Command
            
            equip.setBeforePayMana(CardFactoryUtil.input_equipCreature(equip));
            equip.setDescription("Equip: 2");
            card.addSpellAbility(equip);
            
            card.addEquipCommand(onEquip);
            card.addUnEquipCommand(onUnEquip);
            
        } //*************** END ************ END **************************
        
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
