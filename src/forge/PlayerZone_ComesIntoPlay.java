
package forge;

import forge.card.cardFactory.CardFactoryUtil;
import forge.card.spellability.Ability;
import forge.card.spellability.SpellAbility;

public class PlayerZone_ComesIntoPlay extends DefaultPlayerZone {
    private static final long serialVersionUID = 5750837078903423978L;
    
    private boolean           trigger          = true;
    private boolean           leavesTrigger    = true;
	private static boolean SimultaneousEntry = false; // For Cards with Multiple Token Entry. Only Affects Allies at the moment.
	static int SimultaneousEntryCounter = 1; // For Cards with Multiple Token Entry. Only Affects Allies at the moment.
    
    public PlayerZone_ComesIntoPlay(String zone, Player player) {
        super(zone, player);
    }
    
    @Override
    public void add(Object o) {
        if(o == null) throw new RuntimeException("PlayerZone_ComesInto Play : add() object is null");
        
        super.add(o);
        
        final Card c = (Card) o;
        final Player player = c.getController();
        
        if(trigger && ((CardFactoryUtil.oppHasKismet(c.getController()) && (c.isLand() || c.isCreature() || c.isArtifact()))
        		|| (AllZoneUtil.isCardInPlay("Urabrask the Hidden",c.getController().getOpponent()) && c.isCreature())
        		|| (AllZoneUtil.isCardInPlay("Root Maze") && (c.isLand() || c.isArtifact()))
        		|| (AllZoneUtil.isCardInPlay("Orb of Dreams") && c.isPermanent()))) c.tap();
        
        //cannot use addComesIntoPlayCommand - trigger might be set to false;
        // Keep track of max lands can play per turn
        int addMax = 0;

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
        		AllZone.HumanPlayer.addMaxLandsToPlay(addMax);
        		AllZone.ComputerPlayer.addMaxLandsToPlay(addMax);
        	}
        	else
        		c.getController().addMaxLandsToPlay(addMax);
        }
        
        if(trigger) {
            c.setSickness(true);// summoning sickness
            c.comesIntoPlay();
            AllZone.GameAction.checkWheneverKeyword(c,"EntersBattleField",null);
            
            PlayerZone play = AllZone.getZone(Constant.Zone.Battlefield, c.getController());

            /*
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
            }*/
            
            if(c.isLand()) {
                //System.out.println("A land was just put onto the battlefield: " + c.getName());
                
                CardList list = new CardList(play.getCards());
                
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

                    AllZone.Stack.addSimultaneousStackEntry(ability);

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
                		AllZone.Stack.addSimultaneousStackEntry(ability);

                	}
                }
                
            }//isLand()
        }
        
        if(AllZone.StaticEffects.getCardToEffectsList().containsKey(c.getName())) {
            String[] effects = AllZone.StaticEffects.getCardToEffectsList().get(c.getName());
            for(String effect:effects) {
                AllZone.StaticEffects.addStateBasedEffect(effect);
            }
        }
        
        PlayerZone grave = AllZone.getZone(Constant.Zone.Graveyard, c.getController());
        CardList meek = new CardList(grave.getCards());
        
        meek = meek.getName("Sword of the Meek");
        
        if(meek.size() > 0 && c.isCreature() && c.getNetAttack() == 1 && c.getNetDefense() == 1) {
            for(int i = 0; i < meek.size(); i++) {
                final Card crd = meek.get(i);

                Ability ability = new Ability(meek.get(i), "0") {
                    @Override
                    public void resolve() {
                        if(crd.getController().isHuman()) {
                            if(GameActionUtil.showYesNoDialog(crd, "Attach " + crd + " to " + c + "?")) {
                            	if(AllZoneUtil.isCardInPlayerGraveyard(player, crd)
                                        && AllZoneUtil.isCardInPlay(c) && c.isCreature()
                                        && c.getNetAttack() == 1 && c.getNetDefense() == 1) {
                                    AllZone.GameAction.moveToPlay(crd);
                                    
                                    crd.equipCard(c);
                                }
                            }
                            
                        } else //computer
                        {
                            if(AllZoneUtil.isCardInPlayerGraveyard(player, crd)
                                    && AllZoneUtil.isCardInPlay(c) && c.isCreature()
                                    && c.getNetAttack() == 1 && c.getNetDefense() == 1) {
                            	AllZone.GameAction.moveToPlay(crd);
                                
                                crd.equipCard(c);
                            }
                        }
                    }
                };
                
                StringBuilder sb = new StringBuilder();
                sb.append("Sword of the Meek - Whenever a 1/1 creature enters the battlefield under your control, you may ");
                sb.append("return Sword of the Meek from your graveyard to the battlefield, then attach it to that creature.");
                ability.setStackDescription(sb.toString());

                AllZone.Stack.addSimultaneousStackEntry(ability);

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
        		AllZone.HumanPlayer.addMaxLandsToPlay(addMax);
        		AllZone.ComputerPlayer.addMaxLandsToPlay(addMax);
        	}
        	else
        		c.getController().addMaxLandsToPlay(addMax);
        }
        

        if(leavesTrigger) {
			// Commented out because whenever keyword checks the state, which should NOT happen right now
        	// And nothing uses this whenever keyword anymore
			//AllZone.GameAction.checkWheneverKeyword(c,"LeavesBattleField",null);
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
    
    public void setTrigger(boolean b) {
        trigger = b;
    }
    
    public void setLeavesTrigger(boolean b) {
        leavesTrigger = b;
    }
    
    public void setTriggers(boolean b) {
        trigger = b;
        leavesTrigger = b;
    }

	public static void setSimultaneousEntry(boolean simultaneousEntry) {
		SimultaneousEntry = simultaneousEntry;
	}

	public static boolean isSimultaneousEntry() {
		return SimultaneousEntry;
	}
}
