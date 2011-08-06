
package forge;


import java.util.ArrayList;


public class PlayerZone_ComesIntoPlay extends DefaultPlayerZone {
    private static final long serialVersionUID = 5750837078903423978L;
    
    private boolean           trigger          = true;
    private boolean           leavesTrigger    = true;
	static boolean SimultaneousEntry = false; // For Cards with Multiple Token Entry. Only Affects Allies at the moment.
	static int SimultaneousEntryCounter = 1; // For Cards with Multiple Token Entry. Only Affects Allies at the moment.
    
    public PlayerZone_ComesIntoPlay(String zone, Player player) {
        super(zone, player);
    }
    
    @Override
    public void add(Object o) {
        if(o == null) throw new RuntimeException("PlayerZone_ComesInto Play : add() object is null");
        
        super.add(o);
        
        Card c = (Card) o;
        //final Player player = c.getController();
        
        if(trigger && ((CardFactoryUtil.oppHasKismet(c.getController()) && (c.isLand() || c.isCreature() || c.isArtifact()))
        		|| (AllZoneUtil.isCardInPlay("Root Maze") && (c.isLand() || c.isArtifact()))
        		|| (AllZoneUtil.isCardInPlay("Orb of Dreams") && c.isPermanent()))) c.tap();
        
        //cannot use addComesIntoPlayCommand - trigger might be set to false;
        // Keep track of max lands can play per turn
        int addMax = 0;
        boolean isHuman = c.getController().equals(AllZone.HumanPlayer);
        boolean adjustLandPlays = false;
        boolean eachPlayer = false;
        
        if(c.getName().equals("Exploration") || c.getName().equals("Oracle of Mul Daya")) {
        	addMax = 1;
        	adjustLandPlays = true;
        } 
        else if(c.getName().equals("Azusa, Lost but Seeking")) {
        	addMax = 2;
        	adjustLandPlays = true;
        }
        else if (c.getName().equals("Storm Cauldron") || c.getName().equals("Rites of Flourishing")){
        	// these two aren't in yet, but will just need the other part of the card to work with more lands
        	adjustLandPlays = true;
        	eachPlayer = true;
        	addMax = 1;
        }
        // 7/13: fastbond code removed, fastbond should be unlimited and will be handled elsewhere.
        
        if (adjustLandPlays){
        	if (eachPlayer){
        		AllZone.GameInfo.addHumanMaxPlayNumberOfLands(addMax);
        		AllZone.GameInfo.addComputerMaxPlayNumberOfLands(addMax);
        	}
        	else if (isHuman)
        		AllZone.GameInfo.addHumanMaxPlayNumberOfLands(addMax);
        	else
        		AllZone.GameInfo.addComputerMaxPlayNumberOfLands(addMax);
        }
        
        if(trigger) {
            c.setSickness(true);// summoning sickness
            c.comesIntoPlay();
            AllZone.GameAction.checkWheneverKeyword(c,"EntersBattleField",null);
            
            PlayerZone play = AllZone.getZone(Constant.Zone.Play, c.getController());
            PlayerZone grave = AllZone.getZone(Constant.Zone.Graveyard, c.getController());
            
            //Amulet of Vigor
            if(c.isTapped()) {
                final Card untapCrd = c;
                Ability ability = new Ability(c, "0") {
                    @Override
                    public void resolve() {
                        untapCrd.untap();
                    }
                };
                StringBuilder sb = new StringBuilder();
                sb.append("Amulet of Vigor - Untap ").append(c);
                ability.setStackDescription(sb.toString());
                
                for(int i = 0; i < AllZoneUtil.getPlayerCardsInPlay(c.getController(), "Amulet of Vigor").size(); i++)
                    AllZone.Stack.add(ability);
            }
            
            if(c.isCreature() && (c.getType().contains("Elf")) || c.getKeyword().contains("Changeling")) {
            	CardList list = AllZoneUtil.getPlayerCardsInPlay(c.getController(), "Elvish Vanguard");
                
            	//not for the Elvish Vanguard coming into play now
            	list.remove(c);
                for(Card var:list) {
                    GameActionUtil.Elvish_Vanguard(var);
                }
            }
            
            if(c.isCreature()) {
            	CardList flashes = AllZoneUtil.getCardsInPlay("AEther Flash");
            	final Card enteringCard = c;
            	for(final Card flash:flashes){
                    	SpellAbility ability = new Ability(flash, "") {
                    		@Override
                    		public void resolve() {
                    			enteringCard.addDamage(2, flash);
                    		}
                    	};
                    	StringBuilder sb = new StringBuilder();
                    	sb.append(flash).append(" - deals 2 damage to ").append(enteringCard.getName());
                    	ability.setStackDescription(sb.toString());
                    	
                    	AllZone.Stack.add(ability);
                    }
            }
            
            if(c.isCreature() && AllZoneUtil.isCardInPlay("Intruder Alarm")) {
            	CardList alarms = AllZoneUtil.getCardsInPlay("Intruder Alarm");
            	for(Card alarm:alarms) {
            		final Card triggerer = alarm;
            		Ability ability = new Ability(triggerer, "0") {
            			@Override
            			public void resolve() {
            				CardList creatures = AllZoneUtil.getCreaturesInPlay();
            				for(Card cr:creatures) cr.untap();
            			}
            		};
            		StringBuilder sb = new StringBuilder();
            		sb.append(triggerer.getName()).append(" - untap all creatures.");
            		ability.setStackDescription(sb.toString());
            		
            		AllZone.Stack.add(ability);
            	}
            }
            
            if(c.isLand()) {
                //System.out.println("A land just came into play: " + c.getName());
                
                CardList list = new CardList(play.getCards());
                CardList graveList = new CardList(grave.getCards());
                
                CardList listValakut = list.filter(new CardListFilter() {
                	public boolean addCard(Card c) {
                		return c.getName().contains("Valakut, the Molten Pinnacle");
                	}
                });
                
                list = list.filter(new CardListFilter() {
                    public boolean addCard(Card c) {
                        return c.getKeyword().contains("Landfall") || 
                        	   c.getKeyword().contains("Landfall - Whenever a land enters the battlefield under your control, CARDNAME gets +2/+2 until end of turn.");
                    }
                });
                
                graveList = graveList.filter(new CardListFilter() {
                    public boolean addCard(Card c) {
                        return c.getName().equals("Bloodghast");
                    }
                });
                
                for(Card crd:graveList) {
                    list.add(crd);
                }
                
                for(int i = 0; i < list.size(); i++) {
                    GameActionUtil.executeLandfallEffects(list.get(i));
                }
                
                // Check for a mountain
                if (!listValakut.isEmpty() && c.getType().contains("Mountain") ) {
                	for (int i = 0; i < listValakut.size(); i++) {
                		boolean b = GameActionUtil.executeValakutEffect(listValakut.get(i),c);
                		if (!b) {
                			// Not enough mountains to activate Valakut -- stop the loop
                			break;
                		}
                	}
                }
                
                CardList ankhs = AllZoneUtil.getCardsInPlay("Ankh of Mishra");
                ankhs.add(AllZoneUtil.getCardsInPlay("Zo-Zu the Punisher"));
                final Card ankhLand = c;
                for(Card ankh:ankhs) {
                	final Card source = ankh;
                	SpellAbility ability = new Ability(source, "") {
                		@Override
                		public void resolve() {
                			ankhLand.getController().addDamage(2, source);
                		}
                	};
                	StringBuilder sb = new StringBuilder();
                	sb.append(source).append(" - deals 2 damage to ").append(ankhLand.getController());
                	ability.setStackDescription(sb.toString());
                	
                	AllZone.Stack.add(ability);
                }
                
                CardList seeds = AllZoneUtil.getCardsInPlay("Seed the Land");
                final Card seedLand = c;
                for(Card seed:seeds) {
                	final Card source = seed;
                	SpellAbility ability = new Ability(source, "") {
                		@Override
                		public void resolve() {
                			CardFactoryUtil.makeToken("Snake", "G 1 1 Snake", seedLand.getController(),
                					"G", new String[] {"Creature", "Snake"}, 1, 1, new String[] {});
                		}
                	};
                	StringBuilder sb = new StringBuilder();
                	sb.append(source).append(" - ").append(seedLand.getController());
                	sb.append(" puts a 1/1 green Snake token in play");
                	ability.setStackDescription(sb.toString());
                	
                	AllZone.Stack.add(ability);
                }
                
                //Tectonic Instability
                CardList tis = AllZoneUtil.getCardsInPlay("Tectonic Instability");
                final Card tisLand = c;
                for(Card ti:tis) {
                	final Card source = ti;
                	SpellAbility ability = new Ability(source, "") {
                		@Override
                		public void resolve() {
                			CardList lands = AllZoneUtil.getPlayerCardsInPlay(tisLand.getController());
                			lands = lands.filter(AllZoneUtil.lands);
                			for(Card land:lands) land.tap();
                		}
                	};
                	StringBuilder sb = new StringBuilder();
                	sb.append(source).append(" - tap all lands ");
                	sb.append(tisLand.getController()).append(" controls.");
                	ability.setStackDescription(sb.toString());
                	
                	AllZone.Stack.add(ability);
                }
                
                CardList les = AllZoneUtil.getPlayerCardsInPlay(c.getOwner().getOpponent(), "Land Equilibrium");
                final Card lesLand = c;
                if(les.size() > 0) {
                	final Card source = les.get(0);
                	SpellAbility ability = new Ability(source, "") {
                		@Override
                		public void resolve() {
                			CardList lands = AllZoneUtil.getPlayerLandsInPlay(lesLand.getOwner());
                			lesLand.getOwner().sacrificePermanent(source.getName()+" - Select a land to sacrifice", lands);
                		}
                	};
                	StringBuilder sb = new StringBuilder();
                	sb.append(source).append(" - ");
                	sb.append(tisLand.getController()).append(" sacrifices a land.");
                	ability.setStackDescription(sb.toString());
                	CardList pLands = AllZoneUtil.getPlayerLandsInPlay(lesLand.getOwner());
                	CardList oLands = AllZoneUtil.getPlayerLandsInPlay(lesLand.getOwner().getOpponent());
                	//(pLands - 1) because this land is in play, and the ability is before it is in play
                	if(oLands.size() <= (pLands.size() - 1)) {
                		AllZone.Stack.add(ability);
                	}
                }
                
            }//isLand()
            
            //hack to make tokens trigger ally effects:
            CardList clist = new CardList(play.getCards());
            clist = clist.filter(new CardListFilter() {
                public boolean addCard(Card c) {
                    return c.getName().equals("Conspiracy") && c.getChosenType().equals("Ally");
                }
            });
            
            String[] allyNames = {
                    "Umara Raptor", "Tuktuk Grunts", "Oran-Rief Survivalist", "Nimana Sell-Sword",
                    "Makindi Shieldmate", "Kazandu Blademaster", "Turntimber Ranger", "Highland Berserker",
                    "Joraga Bard", "Bojuka Brigand", "Graypelt Hunter", "Kazuul Warlord"};
            final ArrayList<String> allyNamesList = new ArrayList<String>();
            
            for(int i = 0; i < allyNames.length; i++) {
                allyNamesList.add(allyNames[i]);
            }
            
            if(SimultaneousEntry == false) { // For Cards with Multiple Token Entry. Only Affects Allies at the moment.
            	for(int i = 0; i < SimultaneousEntryCounter; i++) {
            if(c.getType().contains("Ally") || (c.getKeyword().contains("Changeling") && c.isCreature())
                    || (clist.size() > 0 && (c.getType().contains("Creature") || c.getKeyword().contains(
                            "Changeling"))) || allyNamesList.contains(c.getName())) {
                CardList list = new CardList(play.getCards());
                list = list.filter(new CardListFilter() {
                    public boolean addCard(Card c) {
                        return c.getType().contains("Ally") || c.getKeyword().contains("Changeling")
                                || allyNamesList.contains(c.getName());
                    }
                });
                
                for(Card var:list) {
                    GameActionUtil.executeAllyEffects(var);
                }
            }
            	}
            	SimultaneousEntryCounter = 1;
        } else SimultaneousEntryCounter = SimultaneousEntryCounter + 1;
        }
        
        if(AllZone.StaticEffects.getCardToEffectsList().containsKey(c.getName())) {
            String[] effects = AllZone.StaticEffects.getCardToEffectsList().get(c.getName());
            for(String effect:effects) {
                AllZone.StaticEffects.addStateBasedEffect(effect);
            }
        }
        
        PlayerZone grave = AllZone.getZone(Constant.Zone.Graveyard, c.getController());
        PlayerZone play = AllZone.getZone(Constant.Zone.Play, c.getController());
        CardList meek = new CardList(grave.getCards());
        
        meek = meek.getName("Sword of the Meek");
        
        if(meek.size() > 0 && c.isCreature() && c.getNetAttack() == 1 && c.getNetDefense() == 1) {
            for(int i = 0; i < meek.size(); i++) {
                final Card crd = meek.get(i);
                final Card creat = c;
                final PlayerZone graveZone = grave;
                final PlayerZone playZone = play;
                Ability ability = new Ability(meek.get(i), "0") {
                    @Override
                    public void resolve() {
                        if(crd.getController().equals(AllZone.HumanPlayer)) {
                            String[] choices = {"Yes", "No"};
                            
                            Object q = null;
                            
                            q = AllZone.Display.getChoiceOptional("Attach " + crd + " to " + creat + "?", choices);
                            if(q == null || q.equals("No")) ;
                            else if(AllZone.GameAction.isCardInZone(crd, graveZone)
                                    && AllZone.GameAction.isCardInPlay(creat) && creat.isCreature()
                                    && creat.getNetAttack() == 1 && creat.getNetDefense() == 1) {
                                graveZone.remove(crd);
                                playZone.add(crd);
                                
                                crd.equipCard(creat);
                            }
                            
                        } else //computer
                        {
                            if(AllZone.GameAction.isCardInZone(crd, graveZone)
                                    && AllZone.GameAction.isCardInPlay(creat) && creat.isCreature()
                                    && creat.getNetAttack() == 1 && creat.getNetDefense() == 1) {
                                graveZone.remove(crd);
                                playZone.add(crd);
                                
                                crd.equipCard(creat);
                            }
                        }
                    }
                };
                
                StringBuilder sb = new StringBuilder();
                sb.append("Sword of the Meek - Whenever a 1/1 creature enters the battlefield under your control, you may ");
                sb.append("return Sword of the Meek from your graveyard to the battlefield, then attach it to that creature.");
                ability.setStackDescription(sb.toString());

                AllZone.Stack.add(ability);
            }
        }
        
        /*
        for (String effect : AllZone.StateBasedEffects.getStateBasedMap().keySet() ) {
        	Command com = GameActionUtil.commands.get(effect);
        	com.execute();
        }
        */

        //System.out.println("Size: " + AllZone.StateBasedEffects.getStateBasedMap().size());
    }
    
    @Override
    public void remove(Object o) {
        
        super.remove(o);
        
        Card c = (Card) o;
        
        // Keep track of max lands can play per turn
        int addMax = 0;
        boolean isHuman = c.getController().equals(AllZone.HumanPlayer);
        boolean adjustLandPlays = false;
        boolean eachPlayer = false;
        
        if(c.getName().equals("Exploration") || c.getName().equals("Oracle of Mul Daya")) {
        	addMax = -1;
        	adjustLandPlays = true;
        } else if(c.getName().equals("Azusa, Lost but Seeking")) {
        	addMax = -2;
        	adjustLandPlays = true;
        } 
        else if (c.getName().equals("Storm Cauldron") || c.getName().equals("Rites of Flourishing")){
        	// once their second half of their abilities are programmed these two can be added in
        	adjustLandPlays = true;
        	eachPlayer = true;
        	addMax = -1;
        }
        // 7/12: fastbond code removed, fastbond should be unlimited and will be handled elsewhere.
        
        if (adjustLandPlays){
        	if (eachPlayer){
        		AllZone.GameInfo.addHumanMaxPlayNumberOfLands(addMax);
        		AllZone.GameInfo.addComputerMaxPlayNumberOfLands(addMax);
        	}
        	else if (isHuman)
        		AllZone.GameInfo.addHumanMaxPlayNumberOfLands(addMax);
        	else
        		AllZone.GameInfo.addComputerMaxPlayNumberOfLands(addMax);
        }
        

        if(leavesTrigger) {
        	AllZone.GameAction.checkWheneverKeyword(c,"LeavesBattleField",null);
        	c.leavesPlay();
        }
        
        if(AllZone.StaticEffects.getCardToEffectsList().containsKey(c.getName())) {
            String[] effects = AllZone.StaticEffects.getCardToEffectsList().get(c.getName());
            String tempEffect = "";
            for(String effect:effects) {
                tempEffect = effect;
                AllZone.StaticEffects.removeStateBasedEffect(effect);
                Command comm = GameActionUtil.commands.get(tempEffect); //this is to make sure cards reset correctly
                comm.execute();
            }
            
        }
        for(String effect:AllZone.StaticEffects.getStateBasedMap().keySet()) {
            Command com = GameActionUtil.commands.get(effect);
            com.execute();
        }
        
    }
    
    public void setTrigger(boolean b) {%
        trigger = b;
    }
    
    public void setLeavesTrigger(boolean b) {
        leavesTrigger = b;
    }
    
    public void setTriggers(boolean b) {
        trigger = b;
        leavesTrigger = b;
    }
}
