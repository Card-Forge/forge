
package forge;


import java.util.ArrayList;


class CardFactory_Equipment {
    
    public static int shouldEquip(Card c) {
        ArrayList<String> a = c.getKeyword();
        for(int i = 0; i < a.size(); i++)
            if(a.get(i).toString().startsWith("VanillaEquipment")) return i;
        
        return -1;
    }
    
    public static Card getCard(final Card card, String cardName, String owner) {
        
        //*************** START *********** START **************************
        if(cardName.equals("Skullclamp")) {
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
                    return list;
                }//getCreature()
                
            };//equip ability
            

            Command onEquip = new Command() {
                private static final long serialVersionUID = 277714373478367657L;
                
                public void execute() {
                    if(card.isEquipping()) {
                        Card crd = card.getEquipping().get(0);
                        
                        crd.addSemiPermanentAttackBoost(1);
                        crd.addSemiPermanentDefenseBoost(-1);
                    }
                }//execute()
            };//Command
            

            Command onUnEquip = new Command() {
                
                private static final long serialVersionUID = 6496501799243208207L;
                
                public void execute() {
                    if(card.isEquipping()) {
                        Card crd = card.getEquipping().get(0);
                        
                        crd.addSemiPermanentAttackBoost(-1);
                        crd.addSemiPermanentDefenseBoost(1);
                    }
                    
                }//execute()
            };//Command
            

            Input runtime = new Input() {
                private static final long serialVersionUID = -5844375382897176476L;
                
                @Override
                public void showMessage() {
                    //get all creatures you control
                    CardList list = new CardList();
                    list.addAll(AllZone.Human_Play.getCards());
                    list = list.getType("Creature");
                    
                    stopSetNext(CardFactoryUtil.input_targetSpecific(equip, list,
                            "Select target creature to equip", true, false));
                }
            };//Input
            
            equip.setBeforePayMana(runtime);
            
            equip.setDescription("Equip: 1");
            card.addSpellAbility(equip);
            
            card.addEquipCommand(onEquip);
            card.addUnEquipCommand(onUnEquip);
            
        } //*************** END ************ END **************************
    	
        
/*
        //*************** START *********** START **************************
        else if(cardName.equals("Lightning Greaves")) {
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
                                    && CardFactoryUtil.canTarget(card, c) && (!c.getKeyword().contains("Haste"))
                                    && (!c.getKeyword().contains("Defender"));
                        }
                    });
                    // list.remove(card);      // if mana-only cost, allow self-target
                    return list;
                }//getCreature()
                
            };//equip ability
            

            Command onEquip = new Command() {
                private static final long serialVersionUID = 277714373478367657L;
                
                public void execute() {
                    if(card.isEquipping()) {
                        Card crd = card.getEquipping().get(0);
                        crd.addExtrinsicKeyword("Haste");
                        crd.addExtrinsicKeyword("Shroud");
                    }
                }//execute()
            };//Command
            

            Command onUnEquip = new Command() {
                
                private static final long serialVersionUID = -3427116314295067303L;
                
                public void execute() {
                    if(card.isEquipping()) {
                        Card crd = card.getEquipping().get(0);
                        crd.removeExtrinsicKeyword("Haste");
                        crd.removeExtrinsicKeyword("Shroud");
                        
                    }
                    
                }//execute()
            };//Command
            

            Input runtime = new Input() {
                
                private static final long serialVersionUID = 3195056500461797420L;
                
                @Override
                public void showMessage() {
                    //get all creatures you control
                    CardList list = new CardList();
                    list.addAll(AllZone.Human_Play.getCards());
                    list = list.getType("Creature");
                    
                    stopSetNext(CardFactoryUtil.input_targetSpecific(equip, list,
                            "Select target creature to equip", true, false));
                }
            };//Input
            
            equip.setBeforePayMana(runtime);
            
            equip.setDescription("Equip: 0");
            card.addSpellAbility(equip);
            
            card.addEquipCommand(onEquip);
            card.addUnEquipCommand(onUnEquip);
            
        } //*************** END ************ END **************************
*/
        
/*
        //*************** START *********** START **************************
        else if(cardName.equals("Loxodon Warhammer")) {
            final Ability equip = new Ability(card, "3") {
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
                    return list;
                }//getCreature()
                
            };//equip ability
            

            Command onEquip = new Command() {
                
                private static final long serialVersionUID = 8130682765214560887L;
                
                public void execute() {
                    if(card.isEquipping()) {
                        Card crd = card.getEquipping().get(0);
                        crd.addExtrinsicKeyword("Trample");
                        crd.addExtrinsicKeyword("Lifelink");
                        crd.addSemiPermanentAttackBoost(3);
                    }
                }//execute()
            };//Command
            

            Command onUnEquip = new Command() {
                
                private static final long serialVersionUID = 5783423127748320501L;
                
                public void execute() {
                    if(card.isEquipping()) {
                        Card crd = card.getEquipping().get(0);
                        crd.removeExtrinsicKeyword("Trample");
                        crd.removeExtrinsicKeyword("Lifelink");
                        crd.addSemiPermanentAttackBoost(-3);
                        
                    }
                    
                }//execute()
            };//Command
            

            Input runtime = new Input() {
                private static final long serialVersionUID = -6785656229070523470L;
                
                @Override
                public void showMessage() {
                    //get all creatures you control
                    CardList list = new CardList();
                    list.addAll(AllZone.Human_Play.getCards());
                    list = list.getType("Creature");
                    
                    stopSetNext(CardFactoryUtil.input_targetSpecific(equip, list,
                            "Select target creature to equip", true, false));
                }
            };//Input
            
            equip.setBeforePayMana(runtime);
            
            equip.setDescription("Equip: 3");
            card.addSpellAbility(equip);
            
            card.addEquipCommand(onEquip);
            card.addUnEquipCommand(onUnEquip);
            
        } //*************** END ************ END **************************
*/
        
/*
        //*************** START *********** START **************************
        else if(cardName.equals("Behemoth Sledge")) {
            final Ability equip = new Ability(card, "3") {
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
                    return list;
                }//getCreature()
                
            };//equip ability
            

            Command onEquip = new Command() {
                
                private static final long serialVersionUID = 8130682765214560887L;
                
                public void execute() {
                    if(card.isEquipping()) {
                        Card crd = card.getEquipping().get(0);
                        crd.addExtrinsicKeyword("Lifelink");
                        crd.addExtrinsicKeyword("Trample");
                        crd.addSemiPermanentAttackBoost(2);
                        crd.addSemiPermanentDefenseBoost(2);
                    }
                }//execute()
            };//Command
            

            Command onUnEquip = new Command() {
                
                private static final long serialVersionUID = 5783423127748320501L;
                
                public void execute() {
                    if(card.isEquipping()) {
                        Card crd = card.getEquipping().get(0);
                        crd.removeExtrinsicKeyword("Lifelink");
                        crd.removeExtrinsicKeyword("Trample");
                        crd.addSemiPermanentAttackBoost(-2);
                        crd.addSemiPermanentDefenseBoost(-2);
                        
                    }
                    
                }//execute()
            };//Command
            

            Input runtime = new Input() {
                private static final long serialVersionUID = -6785656229070523470L;
                
                @Override
                public void showMessage() {
                    //get all creatures you control
                    CardList list = new CardList();
                    list.addAll(AllZone.Human_Play.getCards());
                    list = list.getType("Creature");
                    
                    stopSetNext(CardFactoryUtil.input_targetSpecific(equip, list,
                            "Select target creature to equip", true, false));
                }
            };//Input
            
            equip.setBeforePayMana(runtime);
            
            equip.setDescription("Equip: 3");
            card.addSpellAbility(equip);
            
            card.addEquipCommand(onEquip);
            card.addUnEquipCommand(onUnEquip);
            
        } //*************** END ************ END **************************
*/
        
/*
        //*************** START *********** START **************************
        else if(cardName.equals("Fireshrieker")) {
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
                                    && (!c.getKeyword().contains("Double Strike"))
                                    && (!c.getKeyword().contains("Defender"));
                        }
                    });
                    return list;
                }//getCreature()
                
            };//equip ability
            

            Command onEquip = new Command() {
                
                private static final long serialVersionUID = 277714373478367657L;
                
                public void execute() {
                    if(card.isEquipping()) {
                        Card crd = card.getEquipping().get(0);
                        crd.addExtrinsicKeyword("Double Strike");
                    }
                }//execute()
            };//Command
            

            Command onUnEquip = new Command() {
                
                private static final long serialVersionUID = -3427116314295067303L;
                
                public void execute() {
                    if(card.isEquipping()) {
                        Card crd = card.getEquipping().get(0);
                        crd.removeExtrinsicKeyword("Double Strike");
                        
                    }
                    
                }//execute()
            };//Command
            

            Input runtime = new Input() {
                private static final long serialVersionUID = 3195056500461797420L;
                
                @Override
                public void showMessage() {
                    //get all creatures you control
                    CardList list = new CardList();
                    list.addAll(AllZone.Human_Play.getCards());
                    list = list.getType("Creature");
                    
                    stopSetNext(CardFactoryUtil.input_targetSpecific(equip, list,
                            "Select target creature to equip", true, false));
                }
            };//Input
            
            equip.setBeforePayMana(runtime);
            
            equip.setDescription("Equip: 2");
            card.addSpellAbility(equip);
            
            card.addEquipCommand(onEquip);
            card.addUnEquipCommand(onUnEquip);
            
        } //*************** END ************ END **************************
*/
        
/*
        //*************** START *********** START **************************
        else if(cardName.equals("Bonesplitter")) {
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
                    return list;
                }//getCreature()
                
            };//equip ability
            

            Command onEquip = new Command() {
                private static final long serialVersionUID = -6930553087037330743L;
                
                public void execute() {
                    if(card.isEquipping()) {
                        Card crd = card.getEquipping().get(0);
                        crd.addSemiPermanentAttackBoost(2);
                    }
                    
                }//execute()
            };//Command
            

            Command onUnEquip = new Command() {
                private static final long serialVersionUID = -3427116314295067303L;
                
                public void execute() {
                    if(card.isEquipping()) {
                        Card crd = card.getEquipping().get(0);
                        crd.addSemiPermanentAttackBoost(-2);
                    }
                    
                }//execute()
            };//Command
            
            Input runtime = new Input() {
                private static final long serialVersionUID = 5184756493874218024L;
                
                @Override
                public void showMessage() {
                    //get all creatures you control
                    CardList list = new CardList();
                    list.addAll(AllZone.Human_Play.getCards());
                    list = list.getType("Creature");
                    
                    stopSetNext(CardFactoryUtil.input_targetSpecific(equip, list,
                            "Select target creature to equip", true, false));
                }
            };//Input
            
            equip.setBeforePayMana(runtime);
            
            equip.setDescription("Equip: 1");
            card.addSpellAbility(equip);
            
            card.addEquipCommand(onEquip);
            card.addUnEquipCommand(onUnEquip);
        } //*************** END ************ END **************************
*/
        
        
        //*************** START *********** START **************************
        else if(cardName.equals("Trailblazer's Boots")) {
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
                    return list;
                }//getCreature()
                
            };//equip ability
            

            Command onEquip = new Command() {
                
                private static final long serialVersionUID = -8783427230086868847L;
                
                public void execute() {
                    if(card.isEquipping()) {
                        Card crd = card.getEquipping().get(0);
                        crd.addExtrinsicKeyword("Nonbasic landwalk");
                    }
                }//execute()
            };//Command
            

            Command onUnEquip = new Command() {
                
                private static final long serialVersionUID = 732383503612045113L;
                
                public void execute() {
                    if(card.isEquipping()) {
                        Card crd = card.getEquipping().get(0);
                        crd.removeExtrinsicKeyword("Nonbasic landwalk");
                    }
                    
                }//execute()
            };//Command
            

            Input runtime = new Input() {
                
                private static final long serialVersionUID = -696882688005519805L;
                
                @Override
                public void showMessage() {
                    //get all creatures you control
                    CardList list = new CardList();
                    list.addAll(AllZone.Human_Play.getCards());
                    list = list.getType("Creature");
                    
                    stopSetNext(CardFactoryUtil.input_targetSpecific(equip, list,
                            "Select target creature to equip", true, false));
                }
            };//Input
            
            equip.setBeforePayMana(runtime);
            
            equip.setDescription("Equip: 2");
            card.addSpellAbility(equip);
            
            card.addEquipCommand(onEquip);
            card.addUnEquipCommand(onUnEquip);
            
        } //*************** END ************ END **************************
        
        
/*
        //*************** START *********** START **************************
        else if(cardName.equals("Blight Sickle")) {
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
                    return list;
                }//getCreature()
                
            };//equip ability
            

            Command onEquip = new Command() {
                
                private static final long serialVersionUID = 8130682765214560887L;
                
                public void execute() {
                    if(card.isEquipping()) {
                        Card crd = card.getEquipping().get(0);
                        crd.addExtrinsicKeyword("Wither");
                        crd.addSemiPermanentAttackBoost(1);
                    }
                }//execute()
            };//Command
            

            Command onUnEquip = new Command() {
                
                private static final long serialVersionUID = 5783423127748320501L;
                
                public void execute() {
                    if(card.isEquipping()) {
                        Card crd = card.getEquipping().get(0);
                        crd.removeExtrinsicKeyword("Wither");
                        crd.addSemiPermanentAttackBoost(-1);
                        
                    }
                    
                }//execute()
            };//Command
            

            Input runtime = new Input() {
                
                private static final long serialVersionUID = -8564484340029497370L;
                
                @Override
                public void showMessage() {
                    //get all creatures you control
                    CardList list = new CardList();
                    list.addAll(AllZone.Human_Play.getCards());
                    list = list.getType("Creature");
                    
                    stopSetNext(CardFactoryUtil.input_targetSpecific(equip, list,
                            "Select target creature to equip", true, false));
                }
            };//Input
            
            equip.setBeforePayMana(runtime);
            
            equip.setDescription("Equip: 2");
            card.addSpellAbility(equip);
            
            card.addEquipCommand(onEquip);
            card.addUnEquipCommand(onUnEquip);
            
        } //*************** END ************ END **************************
*/
        
        
        //*************** START *********** START **************************
        else if(cardName.equals("Spidersilk Net")) {
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
                                    && (!c.getKeyword().contains("Flying") || !c.getKeyword().contains("Reach"));
                        }
                    });
                    // list.remove(card);      // if mana-only cost, allow self-target
                    return list;
                }//getCreature()
                
            };//equip ability
            

            Command onEquip = new Command() {
                
                private static final long serialVersionUID = -5830699867070741036L;
                
                public void execute() {
                    if(card.isEquipping()) {
                        Card crd = card.getEquipping().get(0);
                        crd.addExtrinsicKeyword("Reach");
                        crd.addSemiPermanentDefenseBoost(2);
                    }
                }//execute()
            };//Command
            

            Command onUnEquip = new Command() {
                
                private static final long serialVersionUID = -4098923908462881875L;
                
                public void execute() {
                    if(card.isEquipping()) {
                        Card crd = card.getEquipping().get(0);
                        crd.removeExtrinsicKeyword("Reach");
                        crd.addSemiPermanentDefenseBoost(-2);
                        
                    }
                    
                }//execute()
            };//Command
            

            Input runtime = new Input() {
                
                private static final long serialVersionUID = 5068745895084312024L;
                
                @Override
                public void showMessage() {
                    //get all creatures you control
                    CardList list = new CardList();
                    list.addAll(AllZone.Human_Play.getCards());
                    list = list.getType("Creature");
                    
                    stopSetNext(CardFactoryUtil.input_targetSpecific(equip, list,
                            "Select target creature to equip", true, false));
                }
            };//Input
            
            equip.setBeforePayMana(runtime);
            
            equip.setDescription("Equip: 2");
            card.addSpellAbility(equip);
            
            card.addEquipCommand(onEquip);
            card.addUnEquipCommand(onUnEquip);
            
        } //*************** END ************ END **************************
        

        //*************** START *********** START **************************
        else if(cardName.equals("Whispersilk Cloak")) {
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
                    return list;
                }//getCreature()
                
            };//equip ability
            

            Command onEquip = new Command() {
                
                private static final long serialVersionUID = -1829389094046225543L;
                
                public void execute() {
                    if(card.isEquipping()) {
                        Card crd = card.getEquipping().get(0);
                        crd.addExtrinsicKeyword("Unblockable");
                        crd.addExtrinsicKeyword("Shroud");
                        
                    }
                }//execute()
            };//Command
            

            Command onUnEquip = new Command() {
                
                private static final long serialVersionUID = 110426811459225458L;
                
                public void execute() {
                    if(card.isEquipping()) {
                        Card crd = card.getEquipping().get(0);
                        crd.removeExtrinsicKeyword("Unblockable");
                        crd.removeExtrinsicKeyword("Shroud");
                    }
                    
                }//execute()
            };//Command
            

            Input runtime = new Input() {
                
                private static final long serialVersionUID = 2399248271613089612L;
                
                @Override
                public void showMessage() {
                    //get all creatures you control
                    CardList list = new CardList();
                    list.addAll(AllZone.Human_Play.getCards());
                    list = list.getType("Creature");
                    
                    stopSetNext(CardFactoryUtil.input_targetSpecific(equip, list,
                            "Select target creature to equip", true, false));
                }
            };//Input
            
            equip.setBeforePayMana(runtime);
            
            equip.setDescription("Equip: 2");
            card.addSpellAbility(equip);
            
            card.addEquipCommand(onEquip);
            card.addUnEquipCommand(onUnEquip);
            
        } //*************** END ************ END **************************
        
        //*************** START *********** START **************************
        else if(cardName.equals("Trusty Machete")) {
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
                    return list;
                }//getCreature()
                
            };//equip ability
            

            Command onEquip = new Command() {
                
                private static final long serialVersionUID = -6434466688054628650L;
                
                public void execute() {
                    if(card.isEquipping()) {
                        Card crd = card.getEquipping().get(0);
                        crd.addSemiPermanentAttackBoost(2);
                        crd.addSemiPermanentDefenseBoost(1);
                    }
                }//execute()
            };//Command
            

            Command onUnEquip = new Command() {
                
                private static final long serialVersionUID = -5297369538913528146L;
                
                public void execute() {
                    if(card.isEquipping()) {
                        Card crd = card.getEquipping().get(0);
                        crd.addSemiPermanentAttackBoost(-2);
                        crd.addSemiPermanentDefenseBoost(-1);
                        
                    }
                    
                }//execute()
            };//Command
            

            Input runtime = new Input() {
                private static final long serialVersionUID = -1425693231661483469L;
                
                @Override
                public void showMessage() {
                    //get all creatures you control
                    CardList list = new CardList();
                    list.addAll(AllZone.Human_Play.getCards());
                    list = list.getType("Creature");
                    
                    stopSetNext(CardFactoryUtil.input_targetSpecific(equip, list,
                            "Select target creature to equip", true, false));
                }
            };//Input
            
            equip.setBeforePayMana(runtime);
            
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
            
            equip.setBeforePayMana(CardFactoryUtil.input_targetCreature(equip));
            
            equip.setDescription("Equip: 0");
            card.addSpellAbility(equip);
            
            card.addEquipCommand(onEquip);
            card.addUnEquipCommand(onUnEquip);
        } //*************** END ************ END **************************  
        
        //*************** START *********** START **************************
        else if(cardName.equals("No-Dachi")) {
            final Ability equip = new Ability(card, "3") {
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
                    return list;
                }//getCreature()
                
            };//equip ability
            

            Command onEquip = new Command() {
                
                private static final long serialVersionUID = -3581510347221716639L;
                
                public void execute() {
                    if(card.isEquipping()) {
                        Card crd = card.getEquipping().get(0);
                        crd.addExtrinsicKeyword("First Strike");
                        crd.addSemiPermanentAttackBoost(2);
                    }
                }//execute()
            };//Command
            

            Command onUnEquip = new Command() {
                
                private static final long serialVersionUID = 7782372477768948526L;
                
                public void execute() {
                    if(card.isEquipping()) {
                        Card crd = card.getEquipping().get(0);
                        crd.removeExtrinsicKeyword("First Strike");
                        crd.addSemiPermanentAttackBoost(-2);
                        
                    }
                    
                }//execute()
            };//Command
            

            Input runtime = new Input() {
                private static final long serialVersionUID = 8252169208912917353L;
                
                @Override
                public void showMessage() {
                    //get all creatures you control
                    CardList list = new CardList();
                    list.addAll(AllZone.Human_Play.getCards());
                    list = list.getType("Creature");
                    
                    stopSetNext(CardFactoryUtil.input_targetSpecific(equip, list,
                            "Select target creature to equip", true, false));
                }
            };//Input
            
            equip.setBeforePayMana(runtime);
            
            equip.setDescription("Equip: 3");
            card.addSpellAbility(equip);
            
            card.addEquipCommand(onEquip);
            card.addUnEquipCommand(onUnEquip);
            
        } //*************** END ************ END **************************
        

        //*************** START *********** START **************************
        else if(cardName.equals("Shuko")) {
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
                    return list;
                }//getCreature()
                
            };//equip ability
            

            Command onEquip = new Command() {
                
                private static final long serialVersionUID = -5615942134074972356L;
                
                public void execute() {
                    if(card.isEquipping()) {
                        Card crd = card.getEquipping().get(0);
                        
                        crd.addSemiPermanentAttackBoost(1);
                    }
                }//execute()
            };//Command
            

            Command onUnEquip = new Command() {
                
                private static final long serialVersionUID = 8169940790698709406L;
                
                public void execute() {
                    if(card.isEquipping()) {
                        Card crd = card.getEquipping().get(0);
                        
                        crd.addSemiPermanentAttackBoost(-1);
                        
                    }
                    
                }//execute()
            };//Command
            

            Input runtime = new Input() {
                private static final long serialVersionUID = -5319605106507450668L;
                
                @Override
                public void showMessage() {
                    //get all creatures you control
                    CardList list = new CardList();
                    list.addAll(AllZone.Human_Play.getCards());
                    list = list.getType("Creature");
                    
                    stopSetNext(CardFactoryUtil.input_targetSpecific(equip, list,
                            "Select target creature to equip", true, false));
                }
            };//Input
            
            equip.setBeforePayMana(runtime);
            
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
                    return list;
                }//getCreature()
                
            };//equip ability
            

            Input runtime = new Input() {
                
                private static final long serialVersionUID = 3087795844819115833L;
                
                @Override
                public void showMessage() {
                    //get all creatures you control
                    CardList list = new CardList();
                    list.addAll(AllZone.Human_Play.getCards());
                    list = list.getType("Creature");
                    
                    stopSetNext(CardFactoryUtil.input_targetSpecific(equip, list,
                            "Select target creature to equip", true, false));
                }
            };//Input
            

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
            
            equip.setBeforePayMana(runtime);
            equip.setDescription("Equip: 2");
            card.addSpellAbility(equip);
            card.addSpellAbility(boost);
            card.addSpellAbility(negBoost);
            card.addSpellAbility(gainLife);
            
        } //*************** END ************ END **************************
        

        //*************** START *********** START **************************
        else if(cardName.equals("Sword of the Meek")) {
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
            

            Input runtime = new Input() {
                
                private static final long serialVersionUID = 6238211194632758032L;
                
                @Override
                public void showMessage() {
                    //get all creatures you control
                    CardList list = new CardList();
                    list.addAll(AllZone.Human_Play.getCards());
                    list = list.getType("Creature");
                    
                    stopSetNext(CardFactoryUtil.input_targetSpecific(equip, list,
                            "Select target creature to equip", true, false));
                }
            };//Input
            
            equip.setBeforePayMana(runtime);
            
            equip.setDescription("Equip: 2");
            card.addSpellAbility(equip);
            
            card.addEquipCommand(onEquip);
            card.addUnEquipCommand(onUnEquip);
            
        } //*************** END ************ END **************************
        
        if(shouldEquip(card) != -1) {
            int n = shouldEquip(card);
            if(n != -1) {
                String parse = card.getKeyword().get(n).toString();
                card.removeIntrinsicKeyword(parse);
                
                String k[] = parse.split(":");
                final String P = k[1];
                final String T = k[2];
                final String manacost = k[3];
                final String Ab1 = k[4];
                final String Ab2 = k[5];
                final String Ab3 = k[6]; //quantity of keyword abilities could be modified	          
                final int Power = Integer.parseInt(P);
                final int Tough = Integer.parseInt(T);
                
                card.addSpellAbility(CardFactoryUtil.vanila_equip(card, Power, Tough, Ab1, Ab2, Ab3, manacost));
                card.addEquipCommand(CardFactoryUtil.vanila_onequip(card, Power, Tough, Ab1, Ab2, Ab3, manacost));
                card.addUnEquipCommand(CardFactoryUtil.vanila_unequip(card, Power, Tough, Ab1, Ab2, Ab3, manacost));
                
            }
        }//VanillaEquipment
        
        return card;
    }
}
