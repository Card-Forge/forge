package forge.card.cardFactory;


import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.swing.JOptionPane;

import com.esotericsoftware.minlog.Log;

import forge.AllZone;
import forge.AllZoneUtil;
import forge.ButtonUtil;
import forge.Card;
import forge.CardList;
import forge.CardListFilter;
import forge.CardListUtil;
import forge.CardUtil;
import forge.Command;
import forge.ComputerUtil;
import forge.Constant;
import forge.Counters;
import forge.FileUtil;
import forge.GameActionUtil;
import forge.HandSizeOp;
import forge.Player;
import forge.PlayerZone;
import forge.ReadCard;

import forge.card.abilityFactory.AbilityFactory;

import forge.card.mana.ManaCost;

import forge.card.spellability.Ability;
import forge.card.spellability.Ability_Activated;
import forge.card.spellability.Ability_Mana;
import forge.card.spellability.Ability_Static;
import forge.card.spellability.Ability_Sub;
import forge.card.spellability.Cost;
import forge.card.spellability.Spell;
import forge.card.spellability.SpellAbility;
import forge.card.spellability.Spell_Permanent;
import forge.card.spellability.Target;

import forge.card.trigger.Trigger;

import forge.error.ErrorViewer;

import forge.gui.GuiUtils;
import forge.gui.input.Input;
import forge.gui.input.Input_PayManaCostUtil;
import forge.gui.input.Input_PayManaCost;

import forge.properties.ForgeProps;
import forge.properties.NewConstants;

public class CardFactory implements NewConstants {
    // String cardname is the key, Card is the value
    private Map<String, Card> map       = new HashMap<String, Card>();
    
    private CardList          allCards  = new CardList();
    
    private HashSet<String>   removedCardList;
    private Card              blankCard = new Card();                 //new code
                                                                       
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

    public CardList getCards() {
        return allCards;
    }// getAllCards()

    
    private void readCards(File file) {
        map.clear();
        
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
    
    //TODO - this can probably be deleted.  I don't think it's used.
    final public Card dynamicCopyCard(Card in)
    {
    	if (in.isCreature()) {
            Card card2 = new Card();
            card2 = CardFactory_Creatures.getCard(in, in.getName(), in.getOwner(), this);
            
            return card2;
        } else if (in.isAura()) {
            Card card2 = new Card();
            card2 = CardFactory_Auras.getCard(in, in.getName(), in.getOwner());
            
            return card2;
        } else if (in.isEquipment()) {
            Card card2 = new Card();
            card2 = CardFactory_Equipment.getCard(in, in.getName(), in.getOwner());
            
            return card2;
        } else if (in.isPlaneswalker()) {
            Card card2 = new Card();
            card2 = CardFactory_Planeswalkers.getCard(in, in.getName(), in.getOwner());
            
            return card2;
        } else if (in.isLand()) {
            Card card2 = new Card();
            card2 = CardFactory_Lands.getCard(in, in.getName(), in.getOwner(), this);
            
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
        out.setAllTriggeringObjects(in.getAllTriggeringObjects());
        out.setEquipping(in.getEquipping());
        out.setEquippedBy(in.getEquippedBy());
        out.setEnchantedBy(in.getEnchantedBy());
        out.setEnchanting(in.getEnchanting());
        out.setClones(in.getClones());
        out.setCounters(in.getCounters());
        return out;
    	
    }
    
    final public Card copyCardintoNew(Card in) {
        
    	Card out = copyStats(in);
    	out.setOwner(in.getOwner());
        CardList all = AllZone.CardFactory.getAllCards();
        CardList tokens = AllZoneUtil.getCardsInPlay();
        tokens = tokens.filter(AllZoneUtil.token);
        all.addAll(tokens);
        all.addAll(CopiedList);
        int Unumber = 0;
        for(int i = 0; i < all.size(); i++) {
        if(all.get(i).getUniqueNumber() > Unumber) Unumber = all.get(i).getUniqueNumber();	
        }
        out.setUniqueNumber(Unumber + 4); // +4 because +1 didn't work lol.
        out.setCopiedSpell(true);
        for(Trigger t : out.getTriggers())
        {
            AllZone.TriggerHandler.registerTrigger(t);
        }
        CopiedList.add(out);
        return out;
    	
    }
    
    public final void copySpellontoStack(Card source, Card original, SpellAbility sa, boolean bCopyDetails) {
    	if (sa.getPayCosts() == null){
    		copySpellontoStack(source, source, bCopyDetails);
    		return;
    	}
    	Card c = AllZone.CardFactory.copyCard(original);
    	c.setController(source.getController());
    	c.setCopiedSpell(true);
    	
    	SpellAbility[] sas = c.getSpellAbility();
    	SpellAbility copySA = null;
    	for(int i = 0; i < sas.length; i++) {
    		if(original.getAbilityUsed() == i) {
    			copySA = sas[i];
    		}
    	}

    	if (copySA == null){
    		StringBuilder sb = new StringBuilder();
    		sb.append("Couldn't find matching SpellAbility to copy Source: ").append(source);
    		sb.append(" Spell to Copy: ").append(source);
    		System.out.println(sb.toString());
    		return;
    	}
    	
    	if (bCopyDetails){
    		c.addXManaCostPaid(original.getXManaCostPaid());
    		c.addMultiKickerMagnitude(original.getMultiKickerMagnitude());
    		if(original.isKicked()) c.setKicked(true);
    		c.addReplicateMagnitude(original.getReplicateMagnitude());
    		if(sa.isReplicate()) copySA.setIsReplicate(true);
    	}
    	
    	if(source.getController().isHuman())
    		AllZone.GameAction.playSpellAbilityForFree(copySA);
    	
    	else if(copySA.canPlayAI())
    		ComputerUtil.playStackFree(copySA);
    }
    
    
    public final void copySpellontoStack(Card source, Card original, boolean bCopyDetails) {
    	SpellAbility[] sas = original.getSpellAbility();
    	SpellAbility sa = null;
    	for(int i = 0; i < sas.length; i++) {
    		if(original.getAbilityUsed() == i) {
    			sa = sas[i];
    		}
    	}
    	
    	if (sa == null){
    		StringBuilder sb = new StringBuilder();
    		sb.append("Couldn't find matching SpellAbility to copy Source: ").append(source);
    		sb.append(" Spell to Copy: ").append(source);
    		System.out.println(sb.toString());
    		return;
    	}
    	
    	if (sa.getPayCosts() != null){
    		copySpellontoStack(source, original, sa, bCopyDetails);
    		return;
    	}
    	
    	Card c = AllZone.CardFactory.copyCardintoNew(original);

    	SpellAbility copySA = null;
    	for(SpellAbility s : c.getSpellAbility()){
    		if (s.equals(sa))
    			copySA = s;
    	}
    	
    	if (copySA == null){
    		StringBuilder sb = new StringBuilder();
    		sb.append("Couldn't find matching SpellAbility to copy Source: ").append(source);
    		sb.append(" Spell to Copy: ").append(source);
    		System.out.println(sb.toString());
    		return;
    	}

    	c.setController(source.getController());
    	if(bCopyDetails) {
    		c.addXManaCostPaid(original.getXManaCostPaid());
    		c.addMultiKickerMagnitude(original.getMultiKickerMagnitude());
    		if(original.isKicked()) c.setKicked(true);
    		c.addReplicateMagnitude(original.getReplicateMagnitude());
    		if(sa.isReplicate()) copySA.setIsReplicate(true);

    		// I have no idea what get choice does?
    		if(c.hasChoices()) {
    			for(int i = 0; i < original.getChoices().size(); i++) {
    				c.addSpellChoice(original.getChoice(i));
    			}
    			for(int i = 0; i < original.getChoiceTargets().size(); i++) {
    				c.setSpellChoiceTarget(original.getChoiceTarget(i));
    			}
    		}
    	}

    	if(sa.getTargetCard() != null)
    		copySA.setTargetCard(sa.getTargetCard());
    	
    	if(sa.getTargetPlayer() != null) {
    		if(sa.getTargetPlayer().isHuman()
    				|| (sa.getTargetPlayer().isComputer())) 
    			copySA.setTargetPlayer(sa.getTargetPlayer());
    	}
    	
    	if(source.getController().isHuman())
    		AllZone.GameAction.playSpellAbilityForFree(copySA);
    	
    	else if(copySA.canPlayAI())
    		ComputerUtil.playStackFree(copySA);
    }

    //!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
    //this is the new getCard() method, you have to remove the old getCard()
    final public Card getCard(String cardName, Player owner) {
        if(removedCardList.contains(cardName) || cardName.equals(blankCard.getName())) return blankCard;
        
        return getCard2(cardName, owner);
    }
    
    public final static int hasKeyword(Card c, String k) {
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
        
        if (card.hasKeyword("CARDNAME enters the battlefield tapped.")) {
            card.addComesIntoPlayCommand(new Command() {
                private static final long serialVersionUID = 203335252453049234L;
                
                public void execute() {
                	//System.out.println("Executing previous keyword");
                    card.tap();
                }
            });
        }//if "Comes into play tapped."
        if (card.hasKeyword("CARDNAME enters the battlefield tapped unless you control two or fewer other lands.")) {
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
        			CardList clICtrl = AllZoneUtil.getPlayerCardsInPlay(card.getOwner());
        			
        			boolean fnd = false;
        			
        			for (int i = 0; i < clICtrl.size(); i++)
        			{
        				Card c = clICtrl.get(i);
        				for (int j = 0; j < types.length; j++)
        					if (c.isType(types[j].trim()))
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
					if (card.isCreature()) {
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
              		CardsinPlay.remove(card);
              		for(int i = 0; i < CardsinPlay.size(); i++)
                        AllZone.GameAction.sacrificeDestroy(CardsinPlay.get(i));	 
                    }//execute()
                };//Command
                card.addComesIntoPlayCommand(intoPlay);
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
        
        if(hasKeyword(card, "Morph") != -1) {
            int n = hasKeyword(card, "Morph");
            if(n != -1) {
                card.setPrevIntrinsicKeyword(card.getIntrinsicKeyword());
                card.setPrevType(card.getType());
                
                String parse = card.getKeyword().get(n).toString();
                card.removeIntrinsicKeyword(parse);
                
                String k[] = parse.split(":");
                final Cost cost = new Cost(k[1], cardName, true);
                
                int attack = card.getBaseAttack();
                int defense = card.getBaseDefense();
                
                String orgManaCost = card.getManaCost();
                
                card.addSpellAbility(CardFactoryUtil.ability_Morph_Up(card, cost, orgManaCost, attack, defense));
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
                
                /*
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
                        }
                        
                    }
                    
                    @Override
                    public boolean canPlay() {
                        return AllZone.Phase.getPlayerTurn().equals(card.getController()) && card.isFaceDown()
                                && AllZoneUtil.isCardInPlay(card);
                    }
                    
                };//devour*/
                
                Command intoPlay = new Command() {
                    private static final long serialVersionUID = -7530312713496897814L;
                    
                    public void execute() {
                        CardList creats = AllZoneUtil.getCreaturesInPlay(card.getController());
                        creats.remove(card);                        
                        //System.out.println("Creats size: " + creats.size());
                        
                        if(card.getController().isHuman()) {
                            if (creats.size() > 0)
                            {
	                        	List<Card> selection = GuiUtils.getChoicesOptional("Select creatures to sacrifice", creats.toArray());
	                            
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
                                if(c.getNetAttack() <= 1 && c.getNetAttack()+c.getNetDefense() <= 3) {
                                    AllZone.GameAction.sacrifice(c);
                                    count++;
                                }
                                //is this needed?
                                AllZone.Computer_Battlefield.updateObservers();
                            }
                            numCreatures[0] = count;
                        }
                        int totalCounters = numCreatures[0] * multiplier;

                        card.addCounter(Counters.P1P1, totalCounters);

                        if(card.getName().equals("Skullmulcher")) {
                            for(int i = 0; i < numCreatures[0]; i++) {
                                card.getController().drawCard();
                            }
                        }

                    }
                };
                /*
                devour.setStackDescription(card.getName() + " - gets " + magnitude
                        + " +1/+1 counter(s) per devoured creature.");
                devour.setDescription("Devour " + magnitude);
                card.addSpellAbility(devour);*/
                card.addComesIntoPlayCommand(intoPlay);
            }
        }//Devour
        
        if(hasKeyword(card, "Modular") != -1) {
            int n = hasKeyword(card, "Modular");
            if(n != -1) {
                String parse = card.getKeyword().get(n).toString();

                final int m = Integer.parseInt(parse.substring(8));

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
                        if(card.getController().isComputer()) {
                        	CardList choices = AllZoneUtil.getPlayerCardsInPlay(AllZone.ComputerPlayer);
                        	choices = choices.filter(new CardListFilter() {
                                public boolean addCard(Card c) {
                                    return c.isCreature() && c.isArtifact();
                                }
                            });
                            if(choices.size() != 0){
                            	ability.setTargetCard(CardFactoryUtil.AI_getBestCreature(choices));
                            	
                            	if (ability.getTargetCard() != null){
	                            	ability.setStackDescription("Put " + card.getCounters(Counters.P1P1)
	                                        + " +1/+1 counter/s from " + card + " on " + ability.getTargetCard());
	                            	AllZone.Stack.addSimultaneousStackEntry(ability);

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

        int etbCounter = hasKeyword(card, "etbCounter");	// etbCounter:CounterType:CounterAmount:Condition:Description
        // enters the battlefield with CounterAmount of CounterType
        if(etbCounter != -1) {
            String parse = card.getKeyword().get(etbCounter).toString();
            card.removeIntrinsicKeyword(parse);
            
            String p[] = parse.split(":");
            final Counters counter = Counters.valueOf(p[1]);
            final String numCounters = p[2];
            final String condition = p.length > 3 ? p[3] : "";
              
            StringBuilder sb = new StringBuilder(card.getSpellText());
            if (sb.length() != 0)
            	sb.append("\n");
            if(p.length > 4)
            	sb.append(p[4]);
            else {
	            sb.append(card.getName());
	            sb.append(" enters the battlefield with ");
	            sb.append(numCounters);
	            sb.append(" ");
	            sb.append(counter.getName());
	            sb.append(" counter");
	            if("1" != numCounters) sb.append("s"); 
	            sb.append(" on it.");
            }
            
            card.setText(sb.toString());

            card.addComesIntoPlayCommand(new Command() {
                private static final long serialVersionUID = -2292898970576123040L;

                public void execute() {
                	if(GameActionUtil.specialConditionsMet(card, condition)) {
	                	int toAdd = -1;
	                	if(numCounters.equals("X")) { 
	                		toAdd = CardFactoryUtil.xCount(card, card.getSVar("X"));
	                	}
	                	else {
	                		toAdd = Integer.parseInt(numCounters);
	                	}
	
	                    card.addCounter(counter, toAdd);
                	}

                }
            });//ComesIntoPlayCommand
        } // if etbCounter
        
        int bloodthirst = hasKeyword(card, "Bloodthirst");
        if(bloodthirst != -1) {
        	final int count = Integer.parseInt(card.getKeyword().get(bloodthirst).split(" ")[1]);
        	
        	card.addComesIntoPlayCommand(new Command() {
				private static final long serialVersionUID = -1849308549161972508L;

				public void execute() {
					if(card.getController().getOpponent().getAssignedDamage() > 0)
                    {

						card.addCounter(Counters.P1P1, count);

                    }
				}
        		
        	});
        }//bloodthirst
        
        
        //**************************************************
        // AbilityFactory cards
        ArrayList<String> IA = card.getIntrinsicAbilities();
        if (IA.size() > 0)
        {
        	if (card.isInstant() || card.isSorcery())
        		
        		// Do not remove SpellAbilities created by AbilityFactory or Keywords.
        		card.clearFirstSpellAbility();
        	
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
        			   if (bbSA.getPayCosts() != null)
        			      bbSA.setPayCosts(new Cost(newCost, sa.getSourceCard().getName(), false)); // create new abCost
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
        if (card.isCreature()) {
            card2 = CardFactory_Creatures.getCard(card, cardName, owner, this);
        } else if (card.isAura()) {
            card2 = CardFactory_Auras.getCard(card, cardName, owner);
        } else if (card.isEquipment()) {
            card2 = CardFactory_Equipment.getCard(card, cardName, owner);
        } else if (card.isPlaneswalker()) {
            card2 = CardFactory_Planeswalkers.getCard(card, cardName, owner);
        } else if (card.isLand()) {
            card2 = CardFactory_Lands.getCard(card, cardName, owner, this);
        } else if (card.isInstant()) {
        	card2 = CardFactory_Instants.getCard(card, cardName, owner);
        } else if (card.isSorcery()) {
        	card2 = CardFactory_Sorceries.getCard(card, cardName, owner);
        }
        
        if (card2 != null)
        	return postFactoryKeywords(card2);
        
        //*************** START *********** START **************************
        else if(cardName.equals("Pestilence") || cardName.equals("Pyrohemia")) {
            SpellAbility spell = new Spell_Permanent(card) {
                private static final long serialVersionUID = 7254358703158629514L;
                
                @Override
                public boolean canPlayAI() {
                	CardList creatures = AllZoneUtil.getCreaturesInPlay();
                	return creatures.size() > 0;
                }
            };
            card.clearFirstSpellAbility();
            card.addFirstSpellAbility(spell);
        }
        //*************** END ************ END *************************
                
        
        //*************** START *********** START **************************
        else if(cardName.equals("Bridge from Below")) {
            SpellAbility spell = new Spell_Permanent(card) {
                private static final long serialVersionUID = 7254358703158629514L;
                
                @Override
                public boolean canPlayAI() {
                    return false;
                }
            };
            
            // Do not remove SpellAbilities created by AbilityFactory or Keywords.
            card.clearFirstSpellAbility();
            card.addSpellAbility(spell);
        }
        //*************** END ************ END *************************
        

        //*************** START *********** START **************************
        else if(cardName.equals("Conspiracy")           || cardName.equals("Cover of Darkness")
                || cardName.equals("Door of Destinies") || cardName.equals("Engineered Plague")
                || cardName.equals("Shared Triumph")    || cardName.equals("Belbe's Portal")
                || cardName.equals("Steely Resolve")	|| cardName.equals("Xenograft")) {
            final String[] input = new String[1];
            final Player player = card.getController();
            
            final SpellAbility ability = new Ability_Static(card, "0") {
                @Override
                public void resolve() {
                    if(player.isHuman()) {
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
                	sb.append("As ").append(card.getName()).append(" enters the battlefield, choose a creature type.");
                	ability.setStackDescription(sb.toString());
                    
                    AllZone.Stack.addSimultaneousStackEntry(ability);

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
                    
                    if(player.isComputer()) {
                        type = "Thrull";
                        imageName = "B 1 1 Thrull";
                        color = "B";
                    } else if(player.isHuman()) {
                        Object q = GuiUtils.getChoiceOptional("Select type of creature", choices);
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
                    
                    Cost a1Cost = new Cost("3 T", cardName, true);
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
                    sb.append(" puts a 1/1 ").append(t).append(" token onto the battlefield");
                    a1.setStackDescription(sb.toString());
                    
                    card.addSpellAbility(a1);
                }
            };//ability
            Command intoPlay = new Command() {
                private static final long serialVersionUID = 7202704600935499188L;
                
                public void execute() {
                    ability.setStackDescription("As Sarpadian Empires, Vol. VII enters the battlefield, choose white Citizen, blue Camarid, black Thrull, red Goblin, or green Saproling.");

                    AllZone.Stack.addSimultaneousStackEntry(ability);

                }
            };
            card.setText("As Sarpadian Empires, Vol. VII enters the battlefield, choose white Citizen, blue Camarid, black Thrull, red Goblin, or green Saproling.\r\n"
                    + "3, Tap: Put a 1/1 creature token of the chosen color and type onto the battlefield.\r\n"
                    + card.getText()); // In the slight chance that there may be a need to add a note to this card.
            card.addComesIntoPlayCommand(intoPlay);
            
        }//*************** END ************ END **************************
        

        //*************** START *********** START **************************
        else if(cardName.equals("That Which Was Taken")) {
        	Cost abCost = new Cost("4 T", cardName, true);
        	Target target = new Target(card,"Select target permanent other than "+cardName, new String[] {"Permanent.Other"});
            final Ability_Activated ability = new Ability_Activated(card, abCost, target) {
                private static final long serialVersionUID = -8996435083734446340L;
                
                @Override
                public void resolve() {
                    Card c = getTargetCard();
                    
                    if(AllZoneUtil.isCardInPlay(c) && CardFactoryUtil.canTarget(card, c))
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
					CardList list = AllZoneUtil.getPlayerGraveyard(AllZone.HumanPlayer);
					list.addAll(AllZoneUtil.getPlayerGraveyard(AllZone.ComputerPlayer));
					return list.getType("Creature").size() > 0 && super.canPlay();
                }
            };
            Input necroTarget = new Input() {

            	boolean once = false;
				private static final long serialVersionUID = 8243511353958609599L;

				@Override
                public void showMessage() {
					CardList list = AllZoneUtil.getPlayerGraveyard(AllZone.HumanPlayer);
					list.addAll(AllZoneUtil.getPlayerGraveyard(AllZone.ComputerPlayer));
					list = list.getType("Creature");
					if (list.size() == 0 || once) {
						once = false;
						stop();
					}
					else {
						Object o = GuiUtils.getChoice("Choose card to exile", list.toArray());
						if (o!=null)
						{
							Card c = (Card)o;
							necrogen.setTargetCard(c);
							once = true;

                            AllZone.Stack.addSimultaneousStackEntry(necrogen);

						}
					}
					stop();
                }
            };
            
            necrogen.setDescription("2: Exile target creature card in a graveyard. Put a 1/1 green Saproling creature token onto the battlefield.");
            
            StringBuilder sb = new StringBuilder();
            sb.append(card.getController());
            sb.append(" exiles target creature card in a graveyard. Puts a 1/1 green Saproling creature token onto the battlefield.");
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
							chooseGrave.addAll(grave);
						}
						
						Object o = GuiUtils.getChoice("Choose first creature to exile", chooseGrave.toArray());
						if (o!=null)
						{
							CardList newGrave;
							Card c = (Card)o;
							if (c.getOwner().isHuman()){
								newGrave = AllZoneUtil.getPlayerGraveyard(AllZone.HumanPlayer);
							}
							else {
								newGrave = AllZoneUtil.getPlayerGraveyard(AllZone.ComputerPlayer);
							}
							
							newGrave = newGrave.getType("Creature");
							newGrave.remove(c);
							
							Object o2 = GuiUtils.getChoice("Choose second creature to exile", newGrave.toArray());
							if (o2!=null)
							{
								Card c2 = (Card)o2;
								newGrave.remove(c2);
								AllZone.GameAction.exile(c);
								AllZone.GameAction.exile(c2);
								once = true;

                                AllZone.Stack.addSimultaneousStackEntry(nightSoil);

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
                    //put cards removed by Necropotence into the player's hand
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
                    	library.remove(c);
                    	
                    	// TODO: Necro really exiles face down, but for now we'll just do it this way
                    	// c.setIsFaceDown(true);
                    	// AllZone.GameAction.exile(c);
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
        else if (cardName.equals("Aluren")) {
            final Ability ability1 = new Ability(card, "0") {
                @Override
                public void resolve() {
                    PlayerZone hand = AllZone.getZone(Constant.Zone.Hand, AllZone.HumanPlayer);
                    
                    if (hand.size() == 0) return;
                    
                    CardList creatures = new CardList();
                    
                    for (int i = 0; i < hand.size(); i++) {
                        if (hand.get(i).isCreature()
                                && CardUtil.getConvertedManaCost(hand.get(i).getManaCost()) <= 3) creatures.add(hand.get(i));
                    }
                    
                    if (creatures.size() == 0) return;
                    

                    Object o = GuiUtils.getChoiceOptional("Select target creature to play",
                            creatures.toArray());
                    if(o != null) {
                        Card c = (Card) o;
                        AllZone.Stack.add(c.getSpellPermanent());
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
        	
        	Cost dungeonCost = new Cost("Discard<1/Card>", cardName, true);
        	Target dungeonTgt = new Target(card,"Volrath's Dungeon - Target player" , "player".split(","));
        	
            final SpellAbility dungeon = new Ability_Activated(card, dungeonCost, dungeonTgt){
				private static final long serialVersionUID = 334033015590321821L;

				@Override
                public void chooseTargetAI() {
                    setTargetPlayer(AllZone.HumanPlayer);
                }
                
                @Override
                public void resolve() {
                	Player target = getTargetPlayer();
                    CardList targetHand = AllZoneUtil.getPlayerHand(target);

                    if (targetHand.size() == 0) return;
                    
                    if (target.isHuman()){
    	                Object discard = GuiUtils.getChoice("Select Card to place on top of library.", targetHand.toArray());
    	                
                        Card card = (Card)discard;
                        AllZone.GameAction.moveToLibrary(card);
                    }
                    else if (target.isComputer()){
                    	AllZone.ComputerPlayer.handToLibrary(1, "Top");
                    }
                }
                
                @Override
                public boolean canPlayAI() {
                	return AllZone.Computer_Hand.size() > 0 && AllZone.Human_Hand.size() > 0 && super.canPlay();
                }

            };//SpellAbility dungeon
           
            
        	Cost bailCost = new Cost("PayLife<5>", cardName, true);
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
                	return card.getController().isHuman() && AllZone.ComputerPlayer.getLife() >= 9 && 
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
                    if(card.getController().isHuman()) {
                        if(AllZone.Human_Hand.size() == 0) AllZone.GameAction.sacrifice(card);
                        else AllZone.InputControl.setInput(discard);
                    } else {
                        CardList list = AllZoneUtil.getPlayerHand(AllZone.ComputerPlayer);
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
                    ability.setStackDescription("If Mox Diamond would enter the battlefield, you may discard a land card instead. If you do, put Mox Diamond onto the battlefield. If you don't, put it into its owner's graveyard.");
                    AllZone.Stack.addSimultaneousStackEntry(ability);

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
                    CardList list = AllZoneUtil.getPlayerHand(card.getController());
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
        	
        	// Do not remove SpellAbilities created by AbilityFactory or Keywords.
            card.clearFirstSpellAbility();
            
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

 
        //*************** START *********** START **************************
        else if(cardName.equals("Goblin Charbelcher")) {
        	Cost abCost = new Cost("3 T", cardName, true);
            final Ability_Activated ability = new Ability_Activated(card, abCost, new Target(card,"TgtCP")) {
                private static final long serialVersionUID = -840041589720758423L;
                
                @Override
                public void resolve() {
                    CardList topOfLibrary = AllZoneUtil.getPlayerCardsInLibrary(card.getController());
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
                    GuiUtils.getChoiceOptional("Revealed cards:", revealed.toArray());
                    
                    if(getTargetCard() != null) {
                        if(AllZoneUtil.isCardInPlay(getTargetCard())
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
        else if(cardName.equals("Isochron Scepter"))
        {
        	Cost abCost = new Cost("2 T", cardName, true);
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
        			if(getSourceCard().getController().isComputer())
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
        				// Isochron Scepter might be broken?
        				Card c = copyCard(getSourceCard().getAttachedCards()[0]);
        				SpellAbility sa = c.getSpellAbility()[0];
    					return sa.canPlay();
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
        			if(card.getController().isHuman()) {
        				if(AllZone.Human_Hand.size() > 0)
        					AllZone.InputControl.setInput(exile);
        			} else {
        				CardList list = AllZoneUtil.getPlayerHand(AllZone.ComputerPlayer);
        				list = list.filter( new CardListFilter(){
        					public boolean addCard(Card c)
        					{
        						return c.isInstant() && CardUtil.getConvertedManaCost(c) <=2 ;
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

                    AllZone.Stack.addSimultaneousStackEntry(ability);

        		}
        	};
        	
        	SpellAbility spell = new Spell_Permanent(card) {
        		private static final long serialVersionUID = -2940969025405788931L;
              
        		//could never get the AI to work correctly
        		//it always played the same card 2 or 3 times
        		@Override
        		public boolean canPlayAI() {
        			for(Card c : AllZoneUtil.getPlayerHand(AllZone.ComputerPlayer))
        				if(c.isInstant() && CardUtil.getConvertedManaCost(c) <=2)
        					return true;
        			return false;
        		}
        	};
          
        	card.addComesIntoPlayCommand(intoPlay);
          
        	// Do not remove SpellAbilities created by AbilityFactory or Keywords.
        	card.clearFirstSpellAbility();
        	card.addSpellAbility(spell);
        	card.addSpellAbility(freeCast);
        }//*************** END ************ END **************************
        
      
        //*************** START *********** START **************************
        else if(cardName.equals("Lodestone Bauble")) {
        	/* 1, Tap, Sacrifice Lodestone Bauble: Put up to four target basic
        	 * land cards from a player's graveyard on top of his or her library
        	 * in any order. That player draws a card at the beginning of the next
        	 * turn's upkeep.
        	 */

        	Cost abCost = new Cost("1 T Sac<1/CARDNAME>", cardName, true);
        	Target target = new Target(card, "Select target player", new String[]{"Player"});
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

        			CardList lands = AllZoneUtil.getPlayerGraveyard(player);
        			lands = lands.filter(AllZoneUtil.basicLands);
        			if(card.getController().isHuman()){ 
        				//now, select up to four lands
        				int end = -1;
        				end = Math.min(lands.size(), limit);
        				//TODO - maybe pop a message box here that no basic lands found (if necessary)
        				for(int i = 1; i <= end; i++) {
        					String Title = "Put on top of library: ";
        					if(i == 2) Title = "Put second from top of library: ";
        					if(i == 3) Title = "Put third from top of library: ";
        					if(i == 4) Title = "Put fourth from top of library: ";
        					Object o = GuiUtils.getChoiceOptional(Title, lands.toArray());
        					if(o == null) break;
        					Card c_1 = (Card) o;
        					lands.remove(c_1); //remove from the display list
        					AllZone.GameAction.moveToLibrary(c_1, i-1);
        				}
        			}
        			else { //Computer
        				//based on current AI, computer should always target himself.
        				CardList list = getComputerLands();
        				int max = list.size();
        				if (max > limit) max = limit;

        				for(int i=0;i<max;i++)
        					AllZone.GameAction.moveToLibrary(list.get(i));
        			}
   
        			player.addSlowtripList(card);
        		}

        		private CardList getComputerLands() {
        			CardList list = AllZoneUtil.getPlayerGraveyard(AllZone.ComputerPlayer);
        			return list.getType("Basic");
        		}
        	};//ability

        	ability.setDescription(abCost+"Put up to four target basic land cards from a player's graveyard on top of his or her library in any order. That player draws a card at the beginning of the next turn's upkeep.");
        	card.addSpellAbility(ability);
        }//*************** END ************ END **************************
     
        
        //*************** START *********** START **************************
        else if(cardName.equals("Grindstone")) {
        	Target target = new Target(card,"Select target player", new String[] {"Player"});
        	Cost abCost = new Cost("3 T", cardName, true);
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
        			Player target = getTargetPlayer();
        			CardList library = AllZoneUtil.getPlayerCardsInLibrary(getTargetPlayer());

        			boolean loop = true;
        			CardList grinding = new CardList();
        			do{
        				grinding.clear();
        				
        				for(int i = 0; i < 2; i++){
        					// Move current grinding to a different list
        					if (library.size() > 0){
        						Card c = library.get(0);
        						grinding.add(c);
        						library.remove(c);
        					}
        					else{
        						loop = false;
        						break;
        					}
        				}
        				
        				// if current grinding dont share a color, stop grinding
        				if (loop){
        					loop = grinding.get(0).sharesColorWith(grinding.get(1));
        				}
        				target.mill(grinding.size());
        			}while(loop);
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
        	Target target = new Target(card,"Select target player", new String[] {"Player"});
        	Cost abCost = new Cost("5 T", cardName, true);
        	Ability_Activated ab1 = new Ability_Activated(card, abCost, target) {
				private static final long serialVersionUID = -6282104343089446216L;

				@Override
        		public boolean canPlayAI() {
					Player player = AllZone.HumanPlayer;
					if (!player.canTarget(card))
						return false;
					
					setTargetPlayer(AllZone.HumanPlayer);
        			CardList libList = AllZoneUtil.getPlayerCardsInLibrary(player);
        			return libList.size() > 0;
        		}

        		@Override
        		public void resolve() {
        			Player player = getTargetPlayer();
        			player.mill(AllZone.getZone(Constant.Zone.Graveyard, player).size());
        		}
        	};

        	ab1.setDescription(abCost+"Target player puts the top X cards of his or her library into his or her graveyard, where X is the number of cards in that player's graveyard.");
        	card.addSpellAbility(ab1);
        }//*************** END ************ END **************************       
        

        //*************** START *********** START **************************
        else if(cardName.equals("Everflowing Chalice")) {
            final Command comesIntoPlay = new Command() {
				private static final long serialVersionUID = 4245563898487609274L;

				public void execute() {
                    card.addCounter(Counters.CHARGE, card.getMultiKickerMagnitude());
                    card.setMultiKickerMagnitude(0);
                }
            };
            card.addComesIntoPlayCommand(comesIntoPlay);
        }//*************** END ************ END **************************
        

        //*************** START *********** START **************************
        else if(cardName.equals("Curse of Wizardry")) {
            final Ability ability = new Ability(card, "0") {
                @Override
                public void resolve() {
                    if(card.getController().isHuman()) {
                        
                        String color = "";
                        String[] colors = Constant.Color.Colors;
                        colors[colors.length - 1] = null;
                        
                        Object o = GuiUtils.getChoice("Choose color", colors);
                        color = (String) o;
                        card.setChosenColor(color);
                    } else {
                        CardList list = AllZoneUtil.getPlayerCardsInLibrary(AllZone.HumanPlayer);
                        list.addAll(AllZoneUtil.getPlayerHand(AllZone.HumanPlayer));
                        
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
                    AllZone.Stack.addSimultaneousStackEntry(ability);

                }
            };//Command
            
            StringBuilder sb = new StringBuilder();
            sb.append("As ").append(cardName).append(" enters the battlefield, choose a color.");
            ability.setStackDescription(sb.toString());
            
            card.addComesIntoPlayCommand(comesIntoPlay);
        }//*************** END ************ END **************************

        
        //*************** START *********** START **************************
        else if(cardName.equals("Mirror Universe")) {
        	/*
        	 * Tap, Sacrifice Mirror Universe: Exchange life totals with
        	 * target opponent. Activate this ability only during your upkeep.
        	 */
        	Cost abCost = new Cost("T Sac<1/CARDNAME>", cardName, true);
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
        else if (cardName.equals("An-Zerrin Ruins")) {
        	
        	final Ability_Static comesIntoPlayAbility = new Ability_Static(card, "0") {
                @Override
                public void resolve() {
                	String chosenType = "";
        			if(card.getController().isHuman()) {
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

                    AllZone.Stack.addSimultaneousStackEntry(comesIntoPlayAbility);

                }
            };
            
            card.addComesIntoPlayCommand(intoPlay);
        }//*************** END ************ END **************************
        
        
        //*************** START *********** START **************************
        else if(cardName.equals("Barl's Cage")) {
            final String Tgts[] = {"Creature"};
        	Target target= new Target(card,"Select target creature.", Tgts, "1", "1");

            final Cost cost = new Cost("3", card.getName(), true);
        	
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
                    if(AllZoneUtil.isCardInPlay(target)
                            && CardFactoryUtil.canTarget(card, target)) {
                    	target.addExtrinsicKeyword("This card doesn't untap during your next untap step.");
                    }//is card in play?
                }//resolve()
            };//SpellAbility
            
            card.addSpellAbility(ability); 
        }//*************** END ************ END **************************
        
        
        //*************** START ************ START **************************
        else if(cardName.equals("Black Mana Battery")    || cardName.equals("Blue Mana Battery")
        		|| cardName.equals("Green Mana Battery") || cardName.equals("Red Mana Battery")
        		|| cardName.equals("White Mana Battery")) {
        	
        	final int[] num = new int[1];
        	String name[] = cardName.split(" ");
        	final String shortString = Input_PayManaCostUtil.getShortColorString(name[0].trim().toLowerCase());
        	StringBuilder desc = new StringBuilder();
        	desc.append("tap, Remove any number of charge counters from ").append(cardName);
        	desc.append(": Add ").append(shortString).append(" to your mana pool, then add an additional ");
        	desc.append(shortString).append(" to your mana pool for each charge counter removed this way.");
            
        	final Ability_Mana abMana = new Ability_Mana(card, "0", shortString){
				private static final long serialVersionUID = -4506828762302357781L;
        		
                @Override
                public boolean canPlay(){
                	return false;
                }
        	};
        	abMana.setUndoable(false);
        	
            final Ability addMana = new Ability(card, "0", desc.toString()) {
            	private static final long serialVersionUID = -5356224416791741957L;

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
                	abMana.produceMana(mana(), card.getController());
                }
                
                @Override
                public boolean canPlayAI(){
                	return false;
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
                    String answer = (String)(GuiUtils.getChoiceOptional(
                            "Charge counters to remove", choices));
                    if(null != answer && !answer.equals("")) {
                    	num[0] = Integer.parseInt(answer);
                        card.tap();
                        card.subtractCounter(Counters.CHARGE, num[0]);
                        stop();
                        AllZone.Stack.add(addMana);
                        return;
                    }
                    stop();
                }
            };
            
            addMana.setDescription(desc.toString());
            addMana.setBeforePayMana(runtime);
            card.addSpellAbility(addMana);
        }
        //*************** END ************ END **************************        
        
        
        //*************** START *********** START **************************
        else if(cardName.equals("Time Bomb")){
        	/*
        	 * 1, Tap, Sacrifice Time Bomb: Time Bomb deals damage equal
        	 * to the number of time counters on it to each creature and
        	 * each player.
        	 */
        	Cost abCost = new Cost("1 T Sac<1/CARDNAME>", cardName, true);
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
        					return c.isCreature() && damage >= (c.getNetDefense() + c.getDamage());
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
                    if(card.getController().isHuman()) {
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
        			AllZone.Stack.addSimultaneousStackEntry(ability);

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
                        String answer = (String)(GuiUtils.getChoiceOptional(
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

                    AllZone.Stack.addSimultaneousStackEntry(ability);

                }
            };
            card.addComesIntoPlayCommand(intoPlay);
        }//*************** END ************ END **************************
        
        
        //*************** START *********** START **************************
        else if(cardName.equals("Scroll Rack")) {
        	Cost abCost = new Cost("1 T", cardName, true); 
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
        							Object o = GuiUtils.getChoice("Put a card on top of your library.", exiled.toArray());
        							Card c1 = (Card)o;
        							AllZone.GameAction.moveToLibrary(c1);
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
        
        
        //*************** START *********** START **************************
        else if(cardName.equals("Cursed Scroll")){
        	/*
        	 * 3, Tap: Name a card.  Reveal a card at random from your hand.  If it's the named card,
        	 * Cursed Scroll deals 2 damage to target creature or player.
        	 */
        	Cost abCost = new Cost("3 T", cardName, true);
        	final Ability_Activated ability = new Ability_Activated(card, abCost, new Target(card,"TgtCP")) {
        		private static final long serialVersionUID = 7550743617522146304L;
        		
        		@Override
        		public void resolve() {
        			Player player = card.getController();
        			String input = "";
        			if(player.isHuman()) {
                        input = JOptionPane.showInputDialog(null, "Name a card.", card.getName(), JOptionPane.QUESTION_MESSAGE);
                    } else {
                    	CardList hand = AllZoneUtil.getPlayerHand(AllZone.ComputerPlayer);
                    	if(!hand.isEmpty()) {
                    		hand.shuffle();
                    		input = hand.get(0).getName();
                    	}
                    }
        			
        			//reveal a card at random, and damage if it matches
        			CardList hand = AllZoneUtil.getPlayerHand(card.getController());
        			if(!hand.isEmpty()) {
        				hand.shuffle();
        				Card c = hand.get(0);
        				JOptionPane.showMessageDialog(null, "Revealed card:\n"+c.getName(), card.getName(), JOptionPane.PLAIN_MESSAGE);
        				if(input.equals(c.getName())) {
        					if(null != getTargetCard()) {
        						getTargetCard().addDamage(2, card);
        					}
        					else if(null != getTargetPlayer()) {
        						getTargetPlayer().addDamage(2, card);
        					}
        				}
        			}
        			else {
        				JOptionPane.showMessageDialog(null, "No cards to reveal.  Damage fail.", card.getName(), JOptionPane.PLAIN_MESSAGE);
        			}
        		}
        		
        		@Override
        		public boolean canPlayAI() {
        			return !AllZoneUtil.getPlayerHand(AllZone.ComputerPlayer).isEmpty();
        		}
        	};

        	StringBuilder sbStack = new StringBuilder();
        	sbStack.append(card).append(" - Name a card.");
        	ability.setStackDescription(sbStack.toString());
        	
        	ability.setDescription(abCost+"Name a card.  Reveal a card at random from your hand.  If it's the named card, CARDNAME deals 2 damage to target creature or player.");

        	ability.setChooseTargetAI(CardFactoryUtil.AI_targetHuman());
        	card.addSpellAbility(ability);
        }//*************** END ************ END **************************
        
        
        //*************** START *********** START **************************
        else if(cardName.equals("Temporal Aperture")){
        	/*
        	 * 5, Tap: Shuffle your library, then reveal the top card. Until end
        	 * of turn, for as long as that card remains on top of your library,
        	 * play with the top card of your library revealed and you may play that
        	 * card without paying its mana cost. (If it has X in its mana cost, X is 0.)
        	 */
        	final Card[] topCard = new Card[1];
        	
        	final Ability freeCast = new Ability(card, "0") {
        		
        		@Override
        		public boolean canPlay() {
        			PlayerZone lib = AllZone.getZone(Constant.Zone.Library, card.getController());
        			return super.canPlay() && (lib.size() > 0 && lib.get(0).equals(topCard[0]));
        		}

        		@Override
        		public void resolve() {
        			Card freeCard = topCard[0];
        			Player player = card.getController();
        				if(freeCard != null) {
        					if(freeCard.isLand()) {
        						if(player.canPlayLand()) {
        							player.playLand(freeCard);
        						}
        						else {
        							JOptionPane.showMessageDialog(null, "You can't play any more lands this turn.", "", JOptionPane.INFORMATION_MESSAGE);
        						}
        					}
        					else{
        						AllZone.GameAction.playCardNoCost(freeCard);
        					}
        				}
        				else JOptionPane.showMessageDialog(null, "Error in "+cardName+".  freeCard is null", "", JOptionPane.INFORMATION_MESSAGE);
        			
        		}
        		
        		@Override
        		public boolean canPlayAI() {
        			return false;
        		}

        	};
        	freeCast.setDescription("Play the previously revealed top card of your library for free.");
        	freeCast.setStackDescription(cardName+" - play card without paying its mana cost.");

        	Cost abCost = new Cost("5 T", cardName, true);
        	final Ability_Activated ability = new Ability_Activated(card, abCost, null) {
				private static final long serialVersionUID = -7328518969488588777L;

				@Override
        		public void resolve() {
        			PlayerZone lib = AllZone.getZone(Constant.Zone.Library, card.getController());
        			if(lib.size() > 0) {

        				//shuffle your library
        				card.getController().shuffle();

        				//reveal the top card
        				topCard[0] = lib.get(0);
        				JOptionPane.showMessageDialog(null, "Revealed card:\n"+topCard[0].getName(), card.getName(), JOptionPane.PLAIN_MESSAGE);
        			
        				card.addSpellAbility(freeCast);
        				card.addExtrinsicKeyword("Play with the top card of your library revealed.");
                        AllZone.EndOfTurn.addUntil(new Command() {
							private static final long serialVersionUID = -2860753262177388046L;

							public void execute() {
                                card.removeSpellAbility(freeCast);
                                card.removeExtrinsicKeyword("Play with the top card of your library revealed.");
                            }
                        });
        			}
        		}//resolve
        		
        		@Override
        		public boolean canPlayAI() {
        			return false;
        		}
        	};

        	StringBuilder sbStack = new StringBuilder();
        	sbStack.append(card).append(" - Shuffle your library, then reveal the top card.");
        	ability.setStackDescription(sbStack.toString());
        	
        	StringBuilder sbDesc = new StringBuilder();
        	sbDesc.append("Shuffle your library, then reveal the top card. ");
        	sbDesc.append("Until end of turn, for as long as that card remains on top of your library, play with the top card of your library revealed ");
        	sbDesc.append("and you may play that card without paying its mana cost. ");
        	sbDesc.append("(If it has X in its mana cost, X is 0.)");
        	ability.setDescription(abCost+sbDesc.toString());

        	card.addSpellAbility(ability);
        }//*************** END ************ END **************************
        
      
        //*************** START *********** START **************************
        else if(cardName.equals("Day of the Dragons")) {
        	final CardList exiled = new CardList();
            final SpellAbility exileAll = new Ability(card, "0") {
                @Override
                public void resolve() {
                    CardList myCreatures = AllZoneUtil.getCreaturesInPlay(card.getController());
                    for(Card c:myCreatures) {
                    	exiled.add(c);
                    	AllZone.GameAction.exile(c);
                    }
                    for(int i = 0; i < exiled.size(); i++) {
                    	CardFactoryUtil.makeToken("Dragon", "R 5 5 Dragon", card.getController(),
                    			"R", new String[] {"Creature", "Dragon"}, 5, 5, 
                    			new String[] {"Flying"});
                    }
                }
            };
            
            Command intoPlay = new Command() {
				private static final long serialVersionUID = 7181675096954076868L;

				public void execute() {
                	
                	StringBuilder sb = new StringBuilder();
                	sb.append(cardName).append(" - ");
                	sb.append("exile all creatures you control. ");
                	sb.append("Then put that many 5/5 red Dragon creature tokens with flying onto the battlefield.");
                	exileAll.setStackDescription(sb.toString());

                    AllZone.Stack.addSimultaneousStackEntry(exileAll);

                }
            };
            
            final SpellAbility returnAll = new Ability(card, "0") {
                @Override
                public void resolve() {
                    CardList dragons = AllZoneUtil.getPlayerTypeInPlay(card.getController(), "Dragon");
                    for(Card c:dragons) {
                    	AllZone.GameAction.sacrifice(c);
                    }
                    for(Card c:exiled) {
                    	AllZone.GameAction.moveToPlay(c);
                    }
                    exiled.clear();
                }
            };
            
            Command leavesPlay = new Command() {
				private static final long serialVersionUID = -5553218901524553718L;

				public void execute() {
                	
                	StringBuilder sb = new StringBuilder();
                	sb.append(cardName).append(" - ");
                	sb.append("sacrifice all Dragons you control. ");
                	sb.append("Then return the exiled cards to the battlefield under your control.");
                	returnAll.setStackDescription(sb.toString());

                    AllZone.Stack.addSimultaneousStackEntry(returnAll);

                }
            };
            
            card.addComesIntoPlayCommand(intoPlay);
            card.addLeavesPlayCommand(leavesPlay);
        }//*************** END ************ END **************************
        
      
        //*************** START *********** START **************************
        else if(cardName.equals("Lich")) {
            final SpellAbility loseAllLife = new Ability(card, "0") {
                @Override
                public void resolve() {
                    int life = card.getController().getLife();
                    card.getController().loseLife(life, card);
                }
            };
            
            Command intoPlay = new Command() {
				private static final long serialVersionUID = 1337794055075168785L;

				public void execute() {
                	
                	StringBuilder sb = new StringBuilder();
                	sb.append(cardName).append(" - ").append(card.getController());
                	sb.append(" loses life equal to his or her life total.");
                	loseAllLife.setStackDescription(sb.toString());

                    AllZone.Stack.addSimultaneousStackEntry(loseAllLife);

                }
            };
            
            final SpellAbility loseGame = new Ability(card, "0") {
                @Override
                public void resolve() {
                	card.getController().altLoseConditionMet(card.getName());
                }
            };
            
            Command toGrave = new Command() {
				private static final long serialVersionUID = 5863295714122376047L;

				public void execute() {
                	
                	StringBuilder sb = new StringBuilder();
                	sb.append(cardName).append(" - ").append(card.getController());
                	sb.append("loses the game.");
                	loseGame.setStackDescription(sb.toString());

                    AllZone.Stack.addSimultaneousStackEntry(loseGame);

                }
            };
            
            card.addComesIntoPlayCommand(intoPlay);
            card.addDestroyCommand(toGrave);
        }//*************** END ************ END **************************
        
      
        //*************** START *********** START **************************
        else if(cardName.equals("Triangle of War")) {
        	
        	Target t2 = new Target(card, "Select target creature an opponent controls", "Creature.YouDontCtrl".split(","));
            final Ability_Sub sub = new Ability_Sub(card, t2) {
				private static final long serialVersionUID = -572849470457911366L;

				@Override
            	public boolean chkAI_Drawback() {
            		return false;
            	}
				
				@Override
				public void resolve() {
					Card myc = this.getParent().getTargetCard();
					Card oppc = getTargetCard();
					if(AllZoneUtil.isCardInPlay(myc) && AllZoneUtil.isCardInPlay(oppc)) {
						if(CardFactoryUtil.canTarget(card, myc) && CardFactoryUtil.canTarget(card, oppc)) {
							int myPower = myc.getNetAttack();
							int oppPower = oppc.getNetAttack();
							myc.addDamage(oppPower, oppc);
							oppc.addDamage(myPower, myc);
						}
					}
				}
            	
            	@Override
            	public boolean doTrigger(boolean b) {
            		return false;
            	}
            };
        	
        	Cost abCost = new Cost("2 Sac<1/CARDNAME>", cardName, true);
        	Target t1 = new Target(card, "Select target creature you control", "Creature.YouCtrl".split(","));
        	final Ability_Activated ability = new Ability_Activated(card, abCost, t1) {
				private static final long serialVersionUID = 2312243293988795896L;

				@Override
                public boolean canPlayAI() {
                    return false;
                }
                
                @Override
                public void resolve() {
                	sub.resolve();
                }
            };
            ability.setSubAbility(sub);
            ability.setDescription(abCost+"Choose target creature you control and target creature an opponent controls. Each of those creatures deals damage equal to its power to the other.");
            ability.setStackDescription(card+" - Each creature deals damage equal to its power to the other.");
            card.addSpellAbility(ability);
        }//*************** END ************ END **************************
        
        
        //*************** START *********** START **************************
        else if(cardName.equals("Copy Artifact") || cardName.equals("Sculpting Steel")) {
        	final CardFactory cfact = this;
        	final Card[] copyTarget = new Card[1];
        	final Card[] cloned = new Card[1];
        	
        	final Command leaves = new Command() {
				private static final long serialVersionUID = 6212378498863558380L;

				public void execute() {
                    //Slight hack if the cloner copies a card with triggers
                    AllZone.TriggerHandler.removeAllFromCard(cloned[0]);
                    
                    Card orig = cfact.getCard(card.getName(), card.getController());
                    PlayerZone dest = AllZone.getZone(card.getCurrentlyCloningCard());
                    AllZone.GameAction.moveTo(dest, orig);
                    dest.remove(card.getCurrentlyCloningCard());

                }
            };
        	
        	final SpellAbility copy = new Spell(card) {
				private static final long serialVersionUID = 4236580139968159802L;

				@Override
        		public boolean canPlayAI() {
        			CardList arts = AllZoneUtil.getTypeInPlay("Artifact");
					return !arts.isEmpty();
        		}

				@Override
                public void resolve() {
					if (card.getController().isComputer()) {
						CardList arts = AllZoneUtil.getTypeInPlay("Artifact");
						if(!arts.isEmpty()) {
							copyTarget[0] = CardFactoryUtil.AI_getBestArtifact(arts);
						}
					}
					
					if (copyTarget[0] != null) {
						cloned[0] = CardFactory.copyStats(copyTarget[0]);
						cloned[0].setOwner(card.getController());
						cloned[0].setController(card.getController());
						if(cardName.equals("Copy Artifact")) cloned[0].addType("Enchantment");
						cloned[0].setCloneOrigin(card);
						cloned[0].addLeavesPlayCommand(leaves);
						cloned[0].setCloneLeavesPlayCommand(leaves);
						cloned[0].setCurSetCode(copyTarget[0].getCurSetCode());
						cloned[0].setImageFilename(copyTarget[0].getImageFilename());
						
						for(SpellAbility sa : copyTarget[0].getSpellAbilities()) {
							cloned[0].addSpellAbility(sa);
						}
						
						//Slight hack in case the cloner copies a card with triggers
						for(Trigger t : cloned[0].getTriggers())
						{
							AllZone.TriggerHandler.registerTrigger(t);
						}
						
						AllZone.GameAction.moveToPlay(cloned[0]);
						card.setCurrentlyCloningCard(cloned[0]);
					}
                }
            };//SpellAbility
            
            Input runtime = new Input() {
				private static final long serialVersionUID = 8117808324791871452L;

				@Override
            	public void showMessage() {
            		AllZone.Display.showMessage(cardName+" - Select an artifact on the battlefield");
            		ButtonUtil.enableOnlyCancel();
            	}
				
				@Override
				public void selectButtonCancel() { stop(); }
            	
            	@Override
            	public void selectCard(Card c, PlayerZone z) {
            		if( z.is(Constant.Zone.Battlefield) && c.isArtifact()) {
            			copyTarget[0] = c;
            			stopSetNext(new Input_PayManaCost(copy));
            		}
            	}
            };
            // Do not remove SpellAbilities created by AbilityFactory or Keywords.
            card.clearFirstSpellAbility();
            card.addSpellAbility(copy);
            copy.setStackDescription(cardName+" - enters the battlefield as a copy of selected card.");
            copy.setBeforePayMana(runtime);
        }//*************** END ************ END **************************
        
        

        return postFactoryKeywords(card);
    }//getCard2
    
    public Card postFactoryKeywords(final Card card){
    	// this function should handle any keywords that need to be added after a spell goes through the factory
        // Cards with Cycling abilities
        // -1 means keyword "Cycling" not found
    	
    	// TODO: certain cards have two different kicker types, kicker will need to be written differently to handle this
    	// TODO: kicker costs can only be mana right now i think?
    	// TODO: this kicker only works for pemanents. maybe we can create an optional cost class for buyback, kicker, that type of thing
    	int kicker = hasKeyword(card, "Kicker");
    	if (kicker != -1){
            final SpellAbility kickedSpell = new Spell(card) {
                private static final long serialVersionUID = -1598664196463358630L;
                
                @Override
                public void resolve() {
                    card.setKicked(true);
                    AllZone.GameAction.moveToPlay(card);
                }
            };
            String parse = card.getKeyword().get(kicker).toString();
            card.removeIntrinsicKeyword(parse);
            
            String k[] = parse.split(":");
            final String kickerCost = k[1];
            
            ManaCost mc = new ManaCost(card.getManaCost());
            mc.combineManaCost(kickerCost);

            kickedSpell.setKickerAbility(true);
            kickedSpell.setManaCost(mc.toString());
            kickedSpell.setAdditionalManaCost(kickerCost);
            
            StringBuilder desc = new StringBuilder();
            desc.append("Kicker ").append(kickerCost).append(" (You may pay an additional ");
            desc.append(kickerCost).append(" as you cast this spell.)");
            
            kickedSpell.setDescription(desc.toString());
            
            StringBuilder sb = new StringBuilder();
            sb.append(card.getName()).append(" (Kicked)");
            kickedSpell.setStackDescription(sb.toString());
            
            card.addSpellAbility(kickedSpell);
    	}
    	
    	if (hasKeyword(card, "Replicate") != -1) {
        	int n = hasKeyword(card, "Replicate");
        	if (n!= -1) {
        		String parse = card.getKeyword().get(n).toString();
        		String k[] = parse.split("cate ");
        	
        		SpellAbility sa = card.getSpellAbility()[0];
        		sa.setIsReplicate(true);
        		sa.setReplicateManaCost(k[1]);
        	}
        }
    	
    	int evokeKeyword = hasKeyword(card, "Evoke");
    	if (evokeKeyword != -1){
            final SpellAbility evokedSpell = new Spell(card) {
                private static final long serialVersionUID = -1598664196463358630L;
                
                @Override
                public void resolve() {
                	card.setEvoked(true);
                    AllZone.GameAction.moveToPlay(card);
                }
                
				@Override
        		public boolean canPlayAI() {
					if (!Spell_Permanent.checkETBEffects(card, this, null))
			        	return false;
					return super.canPlayAI();
        		}
            };
            String parse = card.getKeyword().get(evokeKeyword).toString();
            card.removeIntrinsicKeyword(parse);
            
            String k[] = parse.split(":");
            final String evokedCost = k[1];

            evokedSpell.setManaCost(evokedCost);
            
            StringBuilder desc = new StringBuilder();
            desc.append("Evoke ").append(evokedCost);
            desc.append(" (You may cast this spell for its evoke cost. If you do, when it enters the battlefield, sacrifice it.)");
            
            evokedSpell.setDescription(desc.toString());
            
            StringBuilder sb = new StringBuilder();
            sb.append(card.getName()).append(" (Evoked)");
            evokedSpell.setStackDescription(sb.toString());
            
            card.addSpellAbility(evokedSpell);
    	}
    	
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
                  if(card.getController().isHuman()) {
                	 AllZone.HumanPlayer.removeHandSizeOperation(Integer.parseInt(card.getSVar("HSStamp")));
                	 AllZone.ComputerPlayer.addHandSizeOperation(new HandSizeOp(Mode,Amount,Integer.parseInt(card.getSVar("HSStamp"))));
                     
                     AllZone.ComputerPlayer.sortHandSizeOperations();
                  }
                  else if(card.getController().isComputer()) {
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
	
				Cost abCost = new Cost(altCost, card.getName(), altCostSA.isAbility());
				altCostSA.setPayCosts(abCost);
				
	            StringBuilder sb = new StringBuilder();
	            
	            if (altCosts.length > 1) {
	            	sb.append(altCostDescription);
	            }
	            else {
	            	sb.append("You may ").append(abCost.toStringAlt());
	            	sb.append(" rather than pay ").append(card.getName()).append("'s mana cost.");
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
        c.setTriggers(sim.getTriggers());
        
        return c;
    }// copyStats()
    
    public static void main(String[] args) {
        CardFactory f = new CardFactory(ForgeProps.getFile(CARDSFOLDER));
        Card c = f.getCard("Arc-Slogger", null);
        System.out.println(c.getOwner());
    }
}
