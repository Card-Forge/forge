
package forge;

import java.util.ArrayList;
import java.util.HashMap;

import forge.card.cardFactory.CardFactoryUtil;
import forge.card.spellability.Ability;
import forge.card.spellability.Ability_Mana;
import forge.card.spellability.Ability_Static;
import forge.card.spellability.SpellAbility;
import forge.gui.GuiUtils;
import forge.gui.input.Input;

//handles "until next upkeep", "until your next upkeep" and "at beginning of upkeep" commands from cards
/**
 * <p>Upkeep class.</p>
 *
 * @author Forge
 * @version $Id$
 */
public class Upkeep implements java.io.Serializable {
	private static final long serialVersionUID = 6906459482978819354L;
	
	private HashMap<Player,CommandList> until = new HashMap<Player,CommandList>();

    /**
     * <p>addUntil.</p>
     *
     * @param p a {@link forge.Player} object
     * @param c a {@link forge.Command} object.
     */
    public void addUntil(Player p, Command c) {
    	if(null == p) p = AllZone.getPhase().getPlayerTurn();
    	
    	if(until.containsKey(p)) until.get(p).add(c);
    	else until.put(p, new CommandList(c));
    }

    /**
     * <p>executeUntil.</p>
     */
    public void executeUntil(Player p) {
    	if(until.containsKey(p)) execute(until.get(p));
    }

    /**
     * <p>sizeUntil.</p>
     *
     * @return a int.
     */
    public int sizeUntil() {
        return until.size();
    }
    
    
    private void execute(CommandList c) {
        int length = c.size();

        for (int i = 0; i < length; i++)
            c.remove(0).execute();
    }
    
    /**
     * <p>executeAt.</p>
     */
    public void executeAt() {
    	AllZone.getStack().freezeStack();
        upkeep_Braid_Of_Fire();

        upkeep_Slowtrips();  // for "Draw a card at the beginning of the next turn's upkeep."
        upkeep_UpkeepCost(); //sacrifice unless upkeep cost is paid
        upkeep_Echo();

        upkeep_The_Abyss();
        upkeep_Mana_Vortex();
        upkeep_Yawgmoth_Demon();
        upkeep_Lord_of_the_Pit();
        upkeep_Drop_of_Honey();
        upkeep_Demonic_Hordes();
        upkeep_Carnophage();
        upkeep_Sangrophage();
        upkeep_Dega_Sanctuary();
        upkeep_Ceta_Sanctuary();
        upkeep_Tangle_Wire();
        upkeep_Dance_of_the_Dead();
        


        upkeep_Shapeshifter();
        upkeep_Vesuvan_Doppelganger_Keyword();

        //Kinship cards
        upkeep_Ink_Dissolver();
        upkeep_Kithkin_Zephyrnaut();
        upkeep_Leaf_Crowned_Elder();
        upkeep_Mudbutton_Clanger();
        upkeep_Nightshade_Schemers();
        upkeep_Pyroclast_Consul();
        upkeep_Sensation_Gorger();
        upkeep_Squeaking_Pie_Grubfellows();
        upkeep_Wandering_Graybeard();
        upkeep_Waterspout_Weavers();
        upkeep_Winnower_Patrol();
        upkeep_Wolf_Skull_Shaman();

        
        
        upkeep_Sleeper_Agent();
        
        
       // upkeep_Dragon_Broodmother(); //put this before bitterblossom and mycoloth, so that they will resolve FIRST

        //Win / Lose
        // Checks for can't win or can't lose happen in Player.altWinConditionMet()
        
        upkeep_Mortal_Combat();
        upkeep_Near_Death_Experience();
        upkeep_Test_of_Endurance();
        upkeep_Helix_Pinnacle();
        upkeep_Barren_Glory();
        upkeep_Felidar_Sovereign();

        upkeep_Karma();
        upkeep_Oath_of_Druids();
        upkeep_Oath_of_Ghouls();
        upkeep_Suspend();
        upkeep_Vanishing();
        upkeep_Fading();
        upkeep_Masticore();
        upkeep_Eldrazi_Monument();
        upkeep_Blaze_Counters();
        upkeep_Dark_Confidant(); // keep this one semi-last
        upkeep_Power_Surge();
        upkeep_AI_Aluren();
        // experimental, AI abuse aluren

        AllZone.getStack().unfreezeStack();
    }
    
  //UPKEEP CARDS:

    /**
     * <p>upkeep_Braid_Of_Fire.</p>
     */
    private static void upkeep_Braid_Of_Fire() {
        final Player player = AllZone.getPhase().getPlayerTurn();

        CardList braids = AllZoneUtil.getPlayerCardsInPlay(player, "Braid of Fire");

        for (int i = 0; i < braids.size(); i++) {
            final Card c = braids.get(i);

            final StringBuilder sb = new StringBuilder();
            sb.append("Cumulative Upkeep for ").append(c).append("\n");
            final Ability upkeepAbility = new Ability(c, "0") {
                @Override
                public void resolve() {
                    c.addCounter(Counters.AGE, 1);
                    int ageCounters = c.getCounters(Counters.AGE);
                    Ability_Mana abMana = new Ability_Mana(c, "0", "R", ageCounters) {
                        private static final long serialVersionUID = -2182129023960978132L;
                    };
                    if (player.isComputer()) {
                        abMana.produceMana();
                    } else if (GameActionUtil.showYesNoDialog(c, sb.toString())) {
                        abMana.produceMana();
                    } else {
                        AllZone.getGameAction().sacrifice(c);
                    }

                }
            };
            upkeepAbility.setStackDescription(sb.toString());

            AllZone.getStack().addSimultaneousStackEntry(upkeepAbility);

        }
    } //upkeep_Braid_of_Fire

    /**
     * <p>upkeep_Echo.</p>
     */
    private static void upkeep_Echo() {
        CardList list = AllZoneUtil.getPlayerCardsInPlay(AllZone.getPhase().getPlayerTurn());
        list = list.filter(new CardListFilter() {
            public boolean addCard(Card c) {
                return c.hasKeyword("(Echo unpaid)");
            }
        });

        for (int i = 0; i < list.size(); i++) {
            final Card c = list.get(i);
            if (c.getIntrinsicKeyword().contains("(Echo unpaid)")) {

                final Command paidCommand = Command.Blank;

                final Command unpaidCommand = new Command() {
                    private static final long serialVersionUID = -7354791599039157375L;

                    public void execute() {
                        AllZone.getGameAction().sacrifice(c);
                    }
                };

                final Ability aiPaid = upkeepAIPayment(c, c.getEchoCost());

                final StringBuilder sb = new StringBuilder();
                sb.append("Echo for ").append(c).append("\n");

                final Ability sacAbility = new Ability(c, "0") {
                    @Override
                    public void resolve() {
                        if (c.getController().isHuman()) {
                        	GameActionUtil.payManaDuringAbilityResolve(sb.toString(), c.getEchoCost(), paidCommand, unpaidCommand);
                        } else //computer
                        {
                            if (ComputerUtil.canPayCost(aiPaid))
                                ComputerUtil.playNoStack(aiPaid);
                            else
                                AllZone.getGameAction().sacrifice(c);
                        }
                    }
                };
                sacAbility.setStackDescription(sb.toString());

                AllZone.getStack().addSimultaneousStackEntry(sacAbility);


                c.removeIntrinsicKeyword("(Echo unpaid)");
            }
        }
    }//echo

    /**
     * <p>upkeep_Slowtrips.</p>
     */
    private static void upkeep_Slowtrips() {  // Draw a card at the beginning of the next turn's upkeep.
        final Player player = AllZone.getPhase().getPlayerTurn();

        CardList list = player.getSlowtripList();

        for (int i = 0; i < list.size(); i++) {
            Card card = list.get(i);
            card.removeIntrinsicKeyword("Draw a card at the beginning of the next turn's upkeep."); //otherwise another slowtrip gets added

            final Ability slowtrip = new Ability(card, "0") {
                @Override
                public void resolve() {
                    player.drawCard();
                }
            };
            slowtrip.setStackDescription(card.getName() + " - Draw a card");

            AllZone.getStack().addSimultaneousStackEntry(slowtrip);


        }
        player.clearSlowtripList();

        //Do the same for the opponent
        final Player opponent = player.getOpponent();

        list = opponent.getSlowtripList();

        for (int i = 0; i < list.size(); i++) {
            Card card = list.get(i);
            card.removeIntrinsicKeyword("Draw a card at the beginning of the next turn's upkeep."); //otherwise another slowtrip gets added

            final Ability slowtrip = new Ability(card, "0") {
                @Override
                public void resolve() {
                    opponent.drawCard();
                }
            };
            slowtrip.setStackDescription(card.getName() + " - Draw a card");

            AllZone.getStack().addSimultaneousStackEntry(slowtrip);

        }
        opponent.clearSlowtripList();
    }

    /**
     * <p>upkeep_UpkeepCost.</p>
     */
    private static void upkeep_UpkeepCost() {
        CardList list = AllZoneUtil.getPlayerCardsInPlay(AllZone.getPhase().getPlayerTurn());

        for (int i = 0; i < list.size(); i++) {
            final Card c = list.get(i);
            final Player controller = c.getController();
            ArrayList<String> a = c.getKeyword();
            for (int j = 0; j < a.size(); j++) {
                String ability = a.get(j);

                //destroy
                if (ability.startsWith("At the beginning of your upkeep, destroy CARDNAME")) {
                    String k[] = ability.split(" pay ");
                    final String upkeepCost = k[1].toString();


                    final Command unpaidCommand = new Command() {
                        private static final long serialVersionUID = 8942537892273123542L;

                        public void execute() {
                            if (c.getName().equals("Cosmic Horror")) {
                            	controller.addDamage(7, c);
                            }
                            AllZone.getGameAction().destroy(c);
                        }
                    };

                    final Command paidCommand = Command.Blank;

                    final Ability aiPaid = upkeepAIPayment(c, upkeepCost);

                    final StringBuilder sb = new StringBuilder();
                    sb.append("Upkeep for ").append(c).append("\n");
                    final Ability upkeepAbility = new Ability(c, "0") {
                        @Override
                        public void resolve() {
                            if (controller.isHuman()) {
                            	GameActionUtil.payManaDuringAbilityResolve(sb.toString(), upkeepCost, paidCommand, unpaidCommand);
                            } else //computer
                            {
                                if (ComputerUtil.canPayCost(aiPaid)
                                        && !c.hasKeyword("Indestructible"))
                                    ComputerUtil.playNoStack(aiPaid);
                                else
                                    AllZone.getGameAction().destroy(c);
                            }
                        }
                    };
                    upkeepAbility.setStackDescription(sb.toString());
                    upkeepAbility.setDescription(sb.toString());

                    AllZone.getStack().addSimultaneousStackEntry(upkeepAbility);
                }//destroy
                
                //sacrifice
                if (ability.startsWith("At the beginning of your upkeep, sacrifice") || ability.startsWith("Cumulative upkeep")) {
                	String cost = "0";
                    final StringBuilder sb = new StringBuilder();
                	
                	if (ability.startsWith("At the beginning of your upkeep, sacrifice")) {
	                    String k[] = ability.split(" pay ");
	                    cost = k[1].toString();
	                    sb.append("Sacrifice upkeep for ").append(c).append("\n");
                	}
                	
                	if (ability.startsWith("Cumulative upkeep")) {
	                    String k[] = ability.split(":");
	                    c.addCounter(Counters.AGE, 1);
	                    cost = CardFactoryUtil.multiplyManaCost(k[1], c.getCounters(Counters.AGE));
	                    sb.append("Cumulative upkeep for ").append(c).append("\n");
                	}
                	
                	final String upkeepCost = cost;

                    final Command unpaidCommand = new Command() {
                        private static final long serialVersionUID = 5612348769167529102L;

                        public void execute() {
                            AllZone.getGameAction().sacrifice(c);
                        }
                    };

                    final Command paidCommand = Command.Blank;

                    final Ability aiPaid = upkeepAIPayment(c, upkeepCost);

                    final Ability upkeepAbility = new Ability(c, "0") {
                        @Override
                        public void resolve() {
                            if (controller.isHuman()) {
                            	GameActionUtil.payManaDuringAbilityResolve(sb.toString(), upkeepCost, paidCommand, unpaidCommand);
                            } else //computer
                            {
                                if (ComputerUtil.canPayCost(aiPaid))
                                    ComputerUtil.playNoStack(aiPaid);
                                else AllZone.getGameAction().sacrifice(c);
                            }
                        }
                    };
                    upkeepAbility.setStackDescription(sb.toString());
                    upkeepAbility.setDescription(sb.toString());

                    AllZone.getStack().addSimultaneousStackEntry(upkeepAbility);
                }//sacrifice
                
                //destroy
                if (ability.startsWith("At the beginning of your upkeep, CARDNAME deals ")) {
                    String k[] = ability.split("deals ");
                    String s1 = k[1].substring(0, 2);
                    final int upkeepDamage =  Integer.parseInt(s1.trim());
                    String l[] = k[1].split(" pay ");
                    final String upkeepCost = l[1].toString();

                    final Command unpaidCommand = new Command() {
                        private static final long serialVersionUID = 1238166187561501928L;

                        public void execute() {
                        	controller.addDamage(upkeepDamage, c);
                        }
                    };

                    final Command paidCommand = Command.Blank;

                    final Ability aiPaid = upkeepAIPayment(c, upkeepCost);

                    final StringBuilder sb = new StringBuilder();
                    sb.append("Damage upkeep for ").append(c).append("\n");
                    final Ability upkeepAbility = new Ability(c, "0") {
                        @Override
                        public void resolve() {
                            if (controller.isHuman()) {
                                GameActionUtil.payManaDuringAbilityResolve(sb.toString(), upkeepCost, paidCommand, unpaidCommand);
                            } else //computer
                            {
                                if (ComputerUtil.canPayCost(aiPaid)
                                        && controller.predictDamage(upkeepDamage, c, false) > 0)
                                    ComputerUtil.playNoStack(aiPaid);
                                else
                                	controller.addDamage(upkeepDamage, c);
                            }
                        }
                    };
                    upkeepAbility.setStackDescription(sb.toString());
                    upkeepAbility.setDescription(sb.toString());

                    AllZone.getStack().addSimultaneousStackEntry(upkeepAbility);
                }//destroy
            }

        }//for
    }//upkeepCost

    /**
     * <p>upkeepAIPayment.</p>
     *
     * @param c a {@link forge.Card} object.
     * @param cost a {@link java.lang.String} object.
     * @param cost a {@link java.lang.String} object.
     * @return a {@link forge.card.spellability.Ability} object.
     */
    private static Ability upkeepAIPayment(Card c, String cost) {
        return new Ability_Static(c, cost) {
            @Override
            public void resolve() {

            }
        };
    }

    /**
     * <p>upkeep_The_Abyss.</p>
     */
    private static void upkeep_The_Abyss() {
        /*
		 * At the beginning of each player's upkeep, destroy target
		 * nonartifact creature that player controls of his or her
		 * choice. It can't be regenerated.
		 */
        final Player player = AllZone.getPhase().getPlayerTurn();
        final CardList the = AllZoneUtil.getCardsInPlay("The Abyss");
        final CardList magus = AllZoneUtil.getCardsInPlay("Magus of the Abyss");

        CardList cards = new CardList();
        cards.addAll(the);
        cards.addAll(magus);

        for (Card c : cards) {
            final Card abyss = c;

            final Ability sacrificeCreature = new Ability(abyss, "") {
                @Override
                public void resolve() {
                    if (player.isHuman()) {
                        if (abyss_getTargets(player, abyss).size() > 0) {
                            AllZone.getInputControl().setInput(new Input() {
                                private static final long serialVersionUID = 4820011040853968644L;

                                public void showMessage() {
                                    AllZone.getDisplay().showMessage(abyss.getName() + " - Select one nonartifact creature to destroy");
                                    ButtonUtil.disableAll();
                                }

                                public void selectCard(Card selected, PlayerZone zone) {
                                    //probably need to restrict by controller also
                                    if (selected.isCreature() && !selected.isArtifact() && CardFactoryUtil.canTarget(abyss, selected)
                                            && zone.is(Constant.Zone.Battlefield) && zone.getPlayer().isHuman()) {
                                        AllZone.getGameAction().destroyNoRegeneration(selected);
                                        stop();
                                    }
                                }//selectCard()
                            });//Input
                        }
                    } else { //computer
                        CardList targets = abyss_getTargets(player, abyss);
                        CardList indestruct = targets.getKeyword("Indestructible");
                        if (indestruct.size() > 0) {
                            AllZone.getGameAction().destroyNoRegeneration(indestruct.get(0));
                        } else {
                            Card target = CardFactoryUtil.AI_getWorstCreature(targets);
                            if (null == target) {
                                //must be nothing valid to destroy
                            } else AllZone.getGameAction().destroyNoRegeneration(target);
                        }
                    }
                }//resolve
            };//sacrificeCreature

            StringBuilder sb = new StringBuilder();
            sb.append(abyss.getName()).append(" - destroy a nonartifact creature of your choice.");
            sacrificeCreature.setStackDescription(sb.toString());

            if (abyss_getTargets(player, abyss).size() > 0)
                AllZone.getStack().addSimultaneousStackEntry(sacrificeCreature);

        }//end for
    }//The Abyss

    /**
     * <p>abyss_getTargets.</p>
     *
     * @param player a {@link forge.Player} object.
     * @param card a {@link forge.Card} object.
     * @return a {@link forge.CardList} object.
     */
    private static CardList abyss_getTargets(final Player player, Card card) {
        CardList creats = AllZoneUtil.getCreaturesInPlay(player);
        creats = creats.filter(AllZoneUtil.nonartifacts);
        creats = creats.getTargetableCards(card);
        return creats;
    }

    /**
     * <p>upkeep_Mana_Vortex.</p>
     */
    private static void upkeep_Mana_Vortex() {
        /*
		 * At the beginning of each player's upkeep, that player
		 * sacrifices a land.
		 */
        final Player player = AllZone.getPhase().getPlayerTurn();
        final CardList vortices = AllZoneUtil.getCardsInPlay("Mana Vortex");

        for (Card c : vortices) {
            final Card vortex = c;

            final Ability sacrificeLand = new Ability(vortex, "") {
                @Override
                public void resolve() {
                    CardList choices = AllZoneUtil.getPlayerLandsInPlay(player);
                    player.sacrificePermanent(vortex.getName() + " - select a land to sacrifice.", choices);

                    //if no lands in play, sacrifice all "Mana Vortex"s
                    if (AllZoneUtil.getLandsInPlay().size() == 0) {
                        for (Card d : vortices) {
                            AllZone.getGameAction().sacrifice(d);
                        }
                        return;
                    }
                }//resolve
            };//sacrificeCreature

            StringBuilder sb = new StringBuilder();
            sb.append(vortex.getName()).append(" - " + player + " sacrifices a land.");
            sacrificeLand.setStackDescription(sb.toString());

            AllZone.getStack().addSimultaneousStackEntry(sacrificeLand);

        }//end for
    }//Mana_Vortex


    /**
     * <p>upkeep_Yawgmoth_Demon.</p>
     */
    private static void upkeep_Yawgmoth_Demon() {
        /*
		 * At the beginning of your upkeep, you may sacrifice an artifact. If
		 * you don't, tap Yawgmoth Demon and it deals 2 damage to you.
		 */
        final Player player = AllZone.getPhase().getPlayerTurn();
        final CardList cards = AllZoneUtil.getPlayerCardsInPlay(player, "Yawgmoth Demon");

        for (int i = 0; i < cards.size(); i++) {
            final Card c = cards.get(i);

            final Ability sacrificeArtifact = new Ability(c, "") {
                @Override
                public void resolve() {
                    CardList artifacts = AllZoneUtil.getPlayerCardsInPlay(player);
                    artifacts = artifacts.filter(AllZoneUtil.artifacts);

                    if (player.isHuman()) {
                        AllZone.getInputControl().setInput(new Input() {
                            private static final long serialVersionUID = -1698502376924356936L;

                            public void showMessage() {
                                AllZone.getDisplay().showMessage("Yawgmoth Demon - Select one artifact to sacrifice or be dealt 2 damage");
                                ButtonUtil.enableOnlyCancel();
                            }

                            public void selectButtonCancel() {
                                tapAndDamage(player);
                                stop();
                            }

                            public void selectCard(Card artifact, PlayerZone zone) {
                                //probably need to restrict by controller also
                                if (artifact.isArtifact() && zone.is(Constant.Zone.Battlefield)
                                        && zone.getPlayer().isHuman()) {
                                    AllZone.getGameAction().sacrifice(artifact);
                                    stop();
                                }
                            }//selectCard()
                        });//Input
                    } else { //computer
                        Card target = CardFactoryUtil.AI_getCheapestPermanent(artifacts, c, false);
                        if (null == target) {
                            tapAndDamage(player);
                        } else AllZone.getGameAction().sacrifice(target);
                    }
                }//resolve

                private void tapAndDamage(Player player) {
                    c.tap();
                    player.addDamage(2, c);
                }
            };

            StringBuilder sb = new StringBuilder();
            sb.append(c.getName()).append(" - sacrifice an artifact or ");
            sb.append(c.getName()).append(" becomes tapped and deals 2 damage to you.");
            sacrificeArtifact.setStackDescription(sb.toString());

            AllZone.getStack().addSimultaneousStackEntry(sacrificeArtifact);

        }//end for
    }

    /**
     * <p>upkeep_Lord_of_the_Pit.</p>
     */
    private static void upkeep_Lord_of_the_Pit() {
        /*
		 * At the beginning of your upkeep, sacrifice a creature other than
		 * Lord of the Pit. If you can't, Lord of the Pit deals 7 damage to you.
		 */
        final Player player = AllZone.getPhase().getPlayerTurn();
        CardList lords = AllZoneUtil.getPlayerCardsInPlay(player, "Lord of the Pit");
        lords.addAll(AllZoneUtil.getPlayerCardsInPlay(player, "Liege of the Pit"));
        final CardList cards = lords;

        for (int i = 0; i < cards.size(); i++) {
            final Card c = cards.get(i);
            if (c.isFaceDown()) continue;

            final Ability sacrificeCreature = new Ability(c, "") {
                @Override
                public void resolve() {
                    //TODO: this should handle the case where you sacrifice 2 LOTPs to each other
                    CardList creatures = AllZoneUtil.getCreaturesInPlay(player);
                    creatures.remove(c);
                    if (player.isHuman()) {
                        AllZone.getInputControl().setInput(PlayerUtil.input_sacrificePermanent(creatures, c.getName() + " - Select a creature to sacrifice."));
                    } else { //computer
                        Card target = CardFactoryUtil.AI_getWorstCreature(creatures);
                        AllZone.getGameAction().sacrifice(target);
                    }
                }//resolve
            };

            final Ability sevenDamage = new Ability(c, "") {
                @Override
                public void resolve() {
                    player.addDamage(7, c);
                }
            };

            CardList creatures = AllZoneUtil.getCreaturesInPlay(player);
            creatures.remove(c);
            if (creatures.size() == 0) {
                //there are no creatures to sacrifice, so we must do the 7 damage

                StringBuilder sb = new StringBuilder();
                sb.append(c.getName()).append(" - deals 7 damage to controller");
                sevenDamage.setStackDescription(sb.toString());

                AllZone.getStack().addSimultaneousStackEntry(sevenDamage);

            } else {

                StringBuilder sb = new StringBuilder();
                sb.append(c.getName()).append(" - sacrifice a creature.");
                sacrificeCreature.setStackDescription(sb.toString());

                AllZone.getStack().addSimultaneousStackEntry(sacrificeCreature);

            }
        }//end for
    }// upkeep_Lord_of_the_Pit()

    /**
     * <p>upkeep_Drop_of_Honey.</p>
     */
    private static void upkeep_Drop_of_Honey() {
        /*
		 * At the beginning of your upkeep, destroy the creature with the
		 * least power. It can't be regenerated. If two or more creatures
		 * are tied for least power, you choose one of them.
		 */
        final Player player = AllZone.getPhase().getPlayerTurn();
        CardList drops = AllZoneUtil.getPlayerCardsInPlay(player, "Drop of Honey");
        drops.addAll(AllZoneUtil.getPlayerCardsInPlay(player, "Porphyry Nodes"));
        final CardList cards = drops;

        for (int i = 0; i < cards.size(); i++) {
            final Card c = cards.get(i);

            final Ability ability = new Ability(c, "") {
                @Override
                public void resolve() {
                    CardList creatures = AllZoneUtil.getCreaturesInPlay();
                    if (creatures.size() > 0) {
                        CardListUtil.sortAttackLowFirst(creatures);
                        int power = creatures.get(0).getNetAttack();
                        if (player.isHuman()) {
                            AllZone.getInputControl().setInput(CardFactoryUtil.input_destroyNoRegeneration(getLowestPowerList(creatures), "Select creature with power: " + power + " to sacrifice."));
                        } else { //computer
                            Card compyTarget = getCompyCardToDestroy(creatures);
                            AllZone.getGameAction().destroyNoRegeneration(compyTarget);
                        }
                    }
                }//resolve

                private CardList getLowestPowerList(CardList original) {
                    CardList lowestPower = new CardList();
                    int power = original.get(0).getNetAttack();
                    int i = 0;
                    while (i < original.size() && original.get(i).getNetAttack() == power) {
                        lowestPower.add(original.get(i));
                        i++;
                    }
                    return lowestPower;
                }

                private Card getCompyCardToDestroy(CardList original) {
                    CardList options = getLowestPowerList(original);
                    CardList humanCreatures = options.filter(new CardListFilter() {
                        public boolean addCard(Card c) {
                            return c.getController().isHuman();
                        }
                    });
                    if (humanCreatures.isEmpty()) {
                        options.shuffle();
                        return options.get(0);
                    } else {
                        humanCreatures.shuffle();
                        return humanCreatures.get(0);
                    }
                }
            };// Ability

            StringBuilder sb = new StringBuilder();
            sb.append(c.getName()).append(" - destroy 1 creature with lowest power.");
            ability.setStackDescription(sb.toString());

            AllZone.getStack().addSimultaneousStackEntry(ability);

        }//end for
    }// upkeep_Drop_of_Honey()

    /**
     * <p>upkeep_Demonic_Hordes.</p>
     */
    private static void upkeep_Demonic_Hordes() {

        /*
		 * At the beginning of your upkeep, unless you pay BBB, 
		 * tap Demonic Hordes and sacrifice a land of an opponent's choice.
		 */

        final Player player = AllZone.getPhase().getPlayerTurn();
        final CardList cards = AllZoneUtil.getPlayerCardsInPlay(player, "Demonic Hordes");

        for (int i = 0; i < cards.size(); i++) {

            final Card c = cards.get(i);

            final Ability noPay = new Ability(c, "B B B") {
                private static final long serialVersionUID = 4820011390853920644L;

                @Override
                public void resolve() {
                    CardList playerLand = AllZoneUtil.getPlayerLandsInPlay(player);

                    c.tap();
                    if (c.getController().isComputer()) {
                        if (playerLand.size() > 0)
                            AllZone.getInputControl().setInput(PlayerUtil.input_sacrificePermanent(playerLand, c.getName() + " - Select a land to sacrifice."));
                    } else {
                        Card target = CardFactoryUtil.AI_getBestLand(playerLand);

                        AllZone.getGameAction().sacrifice(target);
                    }
                } //end resolve()
            }; //end noPay ability

            if (c.getController().isHuman()) {
                String question = "Pay Demonic Hordes upkeep cost?";
                if (GameActionUtil.showYesNoDialog(c, question)) {
                    final Ability pay = new Ability(c, "0") {
                        private static final long serialVersionUID = 4820011440853920644L;

                        public void resolve() {
                            if (AllZone.getZone(c).is(Constant.Zone.Battlefield)) {
                                StringBuilder cost = new StringBuilder();
                                cost.append("Pay cost for ").append(c).append("\r\n");
                                GameActionUtil.payManaDuringAbilityResolve(cost.toString(), noPay.getManaCost(), Command.Blank, Command.Blank);
                            }
                        } //end resolve()
                    }; //end pay ability
                    pay.setStackDescription("Demonic Hordes - Upkeep Cost");

                    AllZone.getStack().addSimultaneousStackEntry(pay);

                } //end choice
                else {
                    StringBuilder sb = new StringBuilder();
                    sb.append(c.getName()).append(" - is tapped and you must sacrifice a land of opponent's choice");
                    noPay.setStackDescription(sb.toString());

                    AllZone.getStack().addSimultaneousStackEntry(noPay);

                }
            } //end human
            else { //computer
                if ((c.getController().isComputer() && (ComputerUtil.canPayCost(noPay)))) {
                    final Ability computerPay = new Ability(c, "0") {
                        private static final long serialVersionUID = 4820011440852868644L;

                        public void resolve() {
                            ComputerUtil.payManaCost(noPay);
                        }
                    };
                    computerPay.setStackDescription("Computer pays Demonic Hordes upkeep cost");

                    AllZone.getStack().addSimultaneousStackEntry(computerPay);

                } else {
                    AllZone.getStack().addSimultaneousStackEntry(noPay);

                }
            } //end computer

        } //end for loop

    } //upkeep_Demonic_Hordes
    
    /**
     * <p>upkeep_AI_Aluren.</p>
     */
    private static void upkeep_AI_Aluren() {
        CardList alurens = AllZoneUtil.getCardsInPlay("Aluren");
        if (alurens.size() == 0)
            return;

        CardList inHand = AllZoneUtil.getPlayerHand(AllZone.getComputerPlayer());
        inHand = inHand.getType("Creature");
        CardList playable = new CardList();

        for (Card c : inHand)
            if (CardUtil.getConvertedManaCost(c.getManaCost()) <= 3)
                playable.add(c);

        for (Card c : playable)
            AllZone.getGameAction().playSpellAbilityForFree(c.getSpellPermanent());
    }


    /**
     * <p>upkeep_Dance_of_the_Dead.</p>
     */
    private static void upkeep_Dance_of_the_Dead() {
        final Player player = AllZone.getPhase().getPlayerTurn();

        CardList dances = AllZoneUtil.getPlayerCardsInPlay(player, "Dance of the Dead");
        for (Card dance : dances) {
            final Card source = dance;
            final ArrayList<Card> list = source.getEnchanting();
            final Card creature = list.get(0);
            if (creature.isTapped()) {
                Ability vaultChoice = new Ability(source, "0") {

                    @Override
                    public void resolve() {
                        if (GameActionUtil.showYesNoDialog(source, "Untap " + creature.getName() + "?")) {
                            //prompt for pay mana cost, then untap
                            final SpellAbility untap = new Ability(source, "1 B") {
                                @Override
                                public void resolve() {
                                    creature.untap();
                                }
                            };//Ability

                            StringBuilder sb = new StringBuilder();
                            sb.append("Untap ").append(creature);
                            untap.setStackDescription(sb.toString());

                            AllZone.getGameAction().playSpellAbility(untap);
                        }
                    }
                };
                vaultChoice.setStackDescription(source.getName() + " - Untap creature during Upkeep?");

                AllZone.getStack().addSimultaneousStackEntry(vaultChoice);

            }
        }
    }

    
    /////////////////////////
    // Start of Kinship cards
    /////////////////////////


    /**
     * <p>upkeep_Ink_Dissolver.</p>
     */
    private static void upkeep_Ink_Dissolver() {
        final Player player = AllZone.getPhase().getPlayerTurn();
        final Player opponent = player.getOpponent();
        CardList kinship = AllZoneUtil.getPlayerCardsInPlay(player, "Ink Dissolver");

        PlayerZone library = AllZone.getZone(Constant.Zone.Library, player);
        // Players would not choose to trigger Kinship ability if library is empty.
        // Useful for games when the "Milling = Loss Condition" check box is unchecked.

        if (kinship.size() == 0 || library.size() <= 0)
            return;

        final String[] shareTypes = {"Merfolk", "Wizard"};
        final Card[] prevCardShown = {null};
        final Card peek[] = {null};

        for (final Card k : kinship) {
            Ability ability = new Ability(k, "0") {    // change to triggered abilities when ready
                @Override
                public void resolve() {
                    PlayerZone library = AllZone.getZone(Constant.Zone.Library, player);
                    if (library.size() <= 0)
                        return;

                    peek[0] = library.get(0);
                    boolean wantToMillOpponent = false;

                    // We assume that both players will want to peek, ask if they want to reveal.
                    // We do not want to slow down the pace of the game by asking too many questions.
                    // Dialogs outside of the Ability appear at the previous end of turn phase !!!

                    if (peek[0].isValidCard(shareTypes, k.getController(), k)) {
                        if (player.isHuman()) {
                            StringBuilder question = new StringBuilder();
                            question.append("Your top card is ").append(peek[0].getName());
                            question.append(". Reveal card and opponent puts the top 3 ");
                            question.append("cards of his library into his graveyard?");
                            if (GameActionUtil.showYesNoDialog(k, question.toString())) {
                                wantToMillOpponent = true;
                            }
                        }
                        // player isComputer()
                        else {
                            String title = "Computer reveals";
                            revealTopCard(title);
                            wantToMillOpponent = true;
                        }
                    } else if (player.isHuman()) {
                        String title = "Your top card is";
                        revealTopCard(title);
                    }

                    if (wantToMillOpponent)
                        opponent.mill(3);
                }// resolve()

                private void revealTopCard(String title) {
                    if (peek[0] != prevCardShown[0]) {
                        GuiUtils.getChoice(title, peek[0]);
                        prevCardShown[0] = peek[0];
                    }
                }// revealTopCard()
            };// ability

            StringBuilder sb = new StringBuilder();
            sb.append("Ink Dissolver - ").append(player);
            sb.append(" triggers Kinship");
            ability.setStackDescription(sb.toString());

            AllZone.getStack().addSimultaneousStackEntry(ability);

        }// for
    }// upkeep_Ink_Dissolver()


    /**
     * <p>upkeep_Kithkin_Zephyrnaut.</p>
     */
    private static void upkeep_Kithkin_Zephyrnaut() {
        final Player player = AllZone.getPhase().getPlayerTurn();
        CardList kinship = AllZoneUtil.getPlayerCardsInPlay(player, "Kithkin Zephyrnaut");

        PlayerZone library = AllZone.getZone(Constant.Zone.Library, player);
        // Players would not choose to trigger Kinship ability if library is empty.
        // Useful for games when the "Milling = Loss Condition" check box is unchecked.

        if (kinship.size() == 0 || library.size() <= 0)
            return;

        final String[] shareTypes = {"Kithkin", "Soldier"};
        final Card[] prevCardShown = {null};
        final Card peek[] = {null};

        for (final Card k : kinship) {
            Ability ability = new Ability(k, "0") {    // change to triggered abilities when ready
                @Override
                public void resolve() {
                    PlayerZone library = AllZone.getZone(Constant.Zone.Library, player);
                    if (library.size() <= 0)
                        return;

                    peek[0] = library.get(0);
                    boolean wantKithkinBuff = false;

                    // We assume that both players will want to peek, ask if they want to reveal.
                    // We do not want to slow down the pace of the game by asking too many questions.
                    // Dialogs outside of the Ability appear at the previous end of turn phase !!!

                    if (peek[0].isValidCard(shareTypes, k.getController(), k)) {
                        if (player.isHuman()) {
                            StringBuilder question = new StringBuilder();
                            question.append("Your top card is ").append(peek[0].getName());
                            question.append(". Reveal card, Kithkin Zephyrnaut gets +2/+2 and ");
                            question.append("gains flying and vigilance until end of turn?");
                            if (GameActionUtil.showYesNoDialog(k, question.toString())) {
                                wantKithkinBuff = true;
                            }
                        }
                        // player isComputer()
                        else {
                            String title = "Computer reveals";
                            revealTopCard(title);
                            wantKithkinBuff = true;
                        }
                    } else if (player.isHuman()) {
                        String title = "Your top card is";
                        revealTopCard(title);
                    }

                    if (wantKithkinBuff) {
                        k.addTempAttackBoost(2);
                        k.addTempDefenseBoost(2);
                        k.addExtrinsicKeyword("Flying");
                        k.addExtrinsicKeyword("Vigilance");

                        final Command untilEOT = new Command() {
                            private static final long serialVersionUID = 213717084767008154L;

                            public void execute() {
                                k.addTempAttackBoost(-2);
                                k.addTempDefenseBoost(-2);
                                k.removeExtrinsicKeyword("Flying");
                                k.removeExtrinsicKeyword("Vigilance");
                            }
                        };
                        AllZone.getEndOfTurn().addUntil(untilEOT);
                    }
                }// resolve()

                private void revealTopCard(String title) {
                    if (peek[0] != prevCardShown[0]) {
                        GuiUtils.getChoice(title, peek[0]);
                        prevCardShown[0] = peek[0];
                    }
                }// revealTopCard()
            };// ability

            StringBuilder sb = new StringBuilder();
            sb.append("Kithkin Zephyrnaut - ").append(player);
            sb.append(" triggers Kinship");
            ability.setStackDescription(sb.toString());

            AllZone.getStack().addSimultaneousStackEntry(ability);

        }// for
    }// upkeep_Kithkin_Zephyrnaut()


    /**
     * <p>upkeep_Leaf_Crowned_Elder.</p>
     */
    private static void upkeep_Leaf_Crowned_Elder() {
        final Player player = AllZone.getPhase().getPlayerTurn();
        CardList kinship = AllZoneUtil.getPlayerCardsInPlay(player, "Leaf-Crowned Elder");

        PlayerZone library = AllZone.getZone(Constant.Zone.Library, player);
        // Players would not choose to trigger Kinship ability if library is empty.
        // Useful for games when the "Milling = Loss Condition" check box is unchecked.

        if (kinship.size() == 0 || library.size() <= 0)
            return;

        final String[] shareTypes = {"Treefolk", "Shaman"};
        final Card[] prevCardShown = {null};
        final Card peek[] = {null};

        for (final Card k : kinship) {
            Ability ability = new Ability(k, "0") {    // change to triggered abilities when ready
                @Override
                public void resolve() {
                    PlayerZone library = AllZone.getZone(Constant.Zone.Library, player);
                    if (library.size() <= 0)
                        return;

                    peek[0] = library.get(0);
                    boolean wantToPlayCard = false;

                    // We assume that both players will want to peek, ask if they want to reveal.
                    // We do not want to slow down the pace of the game by asking too many questions.
                    // Dialogs outside of the Ability appear at the previous end of turn phase !!!

                    if (peek[0].isValidCard(shareTypes, k.getController(), k)) {
                        if (player.isHuman()) {
                            StringBuilder question = new StringBuilder();
                            question.append("Your top card is ").append(peek[0].getName());
                            question.append(". Reveal and play this card without paying its mana cost?");
                            if (GameActionUtil.showYesNoDialog(k, question.toString())) {
                                wantToPlayCard = true;
                            }
                        }
                        // player isComputer()
                        else {
                            String title = "Computer reveals";
                            revealTopCard(title);
                            wantToPlayCard = true;
                        }
                    } else if (player.isHuman()) {
                        String title = "Your top card is";
                        revealTopCard(title);
                    }

                    if (wantToPlayCard) {
                        if (player.isHuman()) {
                            Card c = library.get(0);
                            AllZone.getGameAction().playCardNoCost(c);
                        }
                        // player isComputer()
                        else {
                            Card c = library.get(0);
                            ArrayList<SpellAbility> choices = c.getBasicSpells();

                            for (SpellAbility sa : choices) {
                                if (sa.canPlayAI()) {
                                    ComputerUtil.playStackFree(sa);
                                    break;
                                }
                            }
                        }
                    }// wantToPlayCard
                }// resolve()

                private void revealTopCard(String title) {
                    if (peek[0] != prevCardShown[0]) {
                        GuiUtils.getChoice(title, peek[0]);
                        prevCardShown[0] = peek[0];
                    }
                }// revealTopCard()
            };// ability

            StringBuilder sb = new StringBuilder();
            sb.append("Leaf-Crowned Elder - ").append(player);
            sb.append(" triggers Kinship");
            ability.setStackDescription(sb.toString());

            AllZone.getStack().addSimultaneousStackEntry(ability);

        }// for
    }// upkeep_Leaf_Crowned_Elder()


    /**
     * <p>upkeep_Mudbutton_Clanger.</p>
     */
    private static void upkeep_Mudbutton_Clanger() {
        final Player player = AllZone.getPhase().getPlayerTurn();
        CardList kinship = AllZoneUtil.getPlayerCardsInPlay(player, "Mudbutton Clanger");

        PlayerZone library = AllZone.getZone(Constant.Zone.Library, player);
        // Players would not choose to trigger Kinship ability if library is empty.
        // Useful for games when the "Milling = Loss Condition" check box is unchecked.

        if (kinship.size() == 0 || library.size() <= 0)
            return;

        final String[] shareTypes = {"Goblin", "Warrior"};
        final Card[] prevCardShown = {null};
        final Card peek[] = {null};

        for (final Card k : kinship) {
            Ability ability = new Ability(k, "0") {    // change to triggered abilities when ready
                @Override
                public void resolve() {
                    PlayerZone library = AllZone.getZone(Constant.Zone.Library, player);
                    if (library.size() <= 0)
                        return;

                    peek[0] = library.get(0);
                    boolean wantGoblinBuff = false;

                    // We assume that both players will want to peek, ask if they want to reveal.
                    // We do not want to slow down the pace of the game by asking too many questions.
                    // Dialogs outside of the Ability appear at the previous end of turn phase !!!

                    if (peek[0].isValidCard(shareTypes, k.getController(), k)) {
                        if (player.isHuman()) {
                            StringBuilder question = new StringBuilder();
                            question.append("Your top card is ").append(peek[0].getName());
                            question.append(". Reveal card and Mudbutton Clanger gets +1/+1 until end of turn?");
                            if (GameActionUtil.showYesNoDialog(k, question.toString())) {
                                wantGoblinBuff = true;
                            }
                        }
                        // player isComputer()
                        else {
                            String title = "Computer reveals";
                            revealTopCard(title);
                            wantGoblinBuff = true;
                        }
                    } else if (player.isHuman()) {
                        String title = "Your top card is";
                        revealTopCard(title);
                    }

                    if (wantGoblinBuff) {
                        k.addTempAttackBoost(1);
                        k.addTempDefenseBoost(1);

                        final Command untilEOT = new Command() {
                            private static final long serialVersionUID = -103560515951630426L;

                            public void execute() {
                                k.addTempAttackBoost(-1);
                                k.addTempDefenseBoost(-1);
                            }
                        };
                        AllZone.getEndOfTurn().addUntil(untilEOT);
                    }
                }// resolve()

                private void revealTopCard(String title) {
                    if (peek[0] != prevCardShown[0]) {
                        GuiUtils.getChoice(title, peek[0]);
                        prevCardShown[0] = peek[0];
                    }
                }// revealTopCard()
            };// ability

            StringBuilder sb = new StringBuilder();
            sb.append("Mudbutton Clanger - ").append(player);
            sb.append(" triggers Kinship");
            ability.setStackDescription(sb.toString());

            AllZone.getStack().addSimultaneousStackEntry(ability);

        }// for
    }// upkeep_Mudbutton_Clanger()


    /**
     * <p>upkeep_Nightshade_Schemers.</p>
     */
    private static void upkeep_Nightshade_Schemers() {
        final Player player = AllZone.getPhase().getPlayerTurn();
        CardList kinship = AllZoneUtil.getPlayerCardsInPlay(player, "Nightshade Schemers");
        final Player opponent = player.getOpponent();

        PlayerZone library = AllZone.getZone(Constant.Zone.Library, player);
        // Players would not choose to trigger Kinship ability if library is empty.
        // Useful for games when the "Milling = Loss Condition" check box is unchecked.

        if (kinship.size() == 0 || library.size() <= 0)
            return;

        final String[] shareTypes = {"Faerie", "Wizard"};
        final Card[] prevCardShown = {null};
        final Card peek[] = {null};

        for (final Card k : kinship) {
            Ability ability = new Ability(k, "0") {    // change to triggered abilities when ready
                @Override
                public void resolve() {
                    PlayerZone library = AllZone.getZone(Constant.Zone.Library, player);
                    if (library.size() <= 0)
                        return;

                    peek[0] = library.get(0);
                    boolean wantOpponentLoseLife = false;

                    // We assume that both players will want to peek, ask if they want to reveal.
                    // We do not want to slow down the pace of the game by asking too many questions.
                    // Dialogs outside of the Ability appear at the previous end of turn phase !!!

                    if (peek[0].isValidCard(shareTypes, k.getController(), k)) {
                        if (player.isHuman()) {
                            StringBuilder question = new StringBuilder();
                            question.append("Your top card is ").append(peek[0].getName());
                            question.append(". Reveal card and opponent loses 2 life?");
                            if (GameActionUtil.showYesNoDialog(k, question.toString())) {
                                wantOpponentLoseLife = true;
                            }
                        }
                        // player isComputer()
                        else {
                            String title = "Computer reveals";
                            revealTopCard(title);
                            wantOpponentLoseLife = true;
                        }
                    } else if (player.isHuman()) {
                        String title = "Your top card is";
                        revealTopCard(title);
                    }
                    if (wantOpponentLoseLife)
                        opponent.loseLife(2, k);
                }// resolve()

                private void revealTopCard(String title) {
                    if (peek[0] != prevCardShown[0]) {
                        GuiUtils.getChoice(title, peek[0]);
                        prevCardShown[0] = peek[0];
                    }
                }// revealTopCard()
            };// ability

            StringBuilder sb = new StringBuilder();
            sb.append("Nightshade Schemers - ").append(player);
            sb.append(" triggers Kinship");
            ability.setStackDescription(sb.toString());

            AllZone.getStack().addSimultaneousStackEntry(ability);

        }// for
    }// upkeep_Nightshade_Schemers()


    /**
     * <p>upkeep_Pyroclast_Consul.</p>
     */
    private static void upkeep_Pyroclast_Consul() {
        final Player player = AllZone.getPhase().getPlayerTurn();
        CardList kinship = AllZoneUtil.getPlayerCardsInPlay(player, "Pyroclast Consul");

        PlayerZone library = AllZone.getZone(Constant.Zone.Library, player);
        // Players would not choose to trigger Kinship ability if library is empty.
        // Useful for games when the "Milling = Loss Condition" check box is unchecked.

        if (kinship.size() == 0 || library.size() <= 0)
            return;

        final String[] shareTypes = {"Elemental", "Shaman"};
        final Card[] prevCardShown = {null};
        final Card peek[] = {null};

        for (final Card k : kinship) {
            Ability ability = new Ability(k, "0") {    // change to triggered abilities when ready
                @Override
                public void resolve() {
                    PlayerZone library = AllZone.getZone(Constant.Zone.Library, player);
                    if (library.size() <= 0)
                        return;

                    peek[0] = library.get(0);
                    boolean wantDamageCreatures = false;
                    String[] smallCreatures = {"Creature.toughnessLE2"};

                    CardList humanCreatures = AllZoneUtil.getCreaturesInPlay(AllZone.getHumanPlayer());
                    humanCreatures = humanCreatures.getValidCards(smallCreatures, k.getController(), k);
                    humanCreatures = humanCreatures.getNotKeyword("Indestructible");

                    CardList computerCreatures = AllZoneUtil.getCreaturesInPlay(AllZone.getComputerPlayer());
                    computerCreatures = computerCreatures.getValidCards(smallCreatures, k.getController(), k);
                    computerCreatures = computerCreatures.getNotKeyword("Indestructible");

                    // We assume that both players will want to peek, ask if they want to reveal.
                    // We do not want to slow down the pace of the game by asking too many questions.
                    // Dialogs outside of the Ability appear at the previous end of turn phase !!!

                    if (peek[0].isValidCard(shareTypes, k.getController(), k)) {
                        if (player.isHuman()) {
                            StringBuilder question = new StringBuilder();
                            question.append("Your top card is ").append(peek[0].getName());
                            question.append(". Reveal card and Pyroclast Consul deals 2 damage to each creature?");
                            if (GameActionUtil.showYesNoDialog(k, question.toString())) {
                                wantDamageCreatures = true;
                            }
                        }
                        // player isComputer()
                        else {
                            if (humanCreatures.size() > computerCreatures.size()) {
                                String title = "Computer reveals";
                                revealTopCard(title);
                                wantDamageCreatures = true;
                            }
                        }
                    } else if (player.isHuman()) {
                        String title = "Your top card is";
                        revealTopCard(title);
                    }

                    if (wantDamageCreatures) {
                        CardList allCreatures = AllZoneUtil.getCreaturesInPlay();
                        for (final Card crd : allCreatures) {
                            crd.addDamage(2, k);
                        }
                    }
                }// resolve()

                private void revealTopCard(String title) {
                    if (peek[0] != prevCardShown[0]) {
                        GuiUtils.getChoice(title, peek[0]);
                        prevCardShown[0] = peek[0];
                    }
                }// revealTopCard()
            };// ability

            StringBuilder sb = new StringBuilder();
            sb.append("Pyroclast Consul - ").append(player);
            sb.append(" triggers Kinship");
            ability.setStackDescription(sb.toString());

            AllZone.getStack().addSimultaneousStackEntry(ability);

        }// for
    }// upkeep_Pyroclast_Consul()


    /**
     * <p>upkeep_Sensation_Gorger.</p>
     */
    private static void upkeep_Sensation_Gorger() {
        final Player player = AllZone.getPhase().getPlayerTurn();
        CardList kinship = AllZoneUtil.getPlayerCardsInPlay(player, "Sensation Gorger");
        final Player opponent = player.getOpponent();

        PlayerZone library = AllZone.getZone(Constant.Zone.Library, player);
        // Players would not choose to trigger Kinship ability if library is empty.
        // Useful for games when the "Milling = Loss Condition" check box is unchecked.

        if (kinship.size() == 0 || library.size() <= 0)
            return;

        final String[] shareTypes = {"Goblin", "Shaman"};
        final Card[] prevCardShown = {null};
        final Card peek[] = {null};

        for (final Card k : kinship) {
            Ability ability = new Ability(k, "0") {    // change to triggered abilities when ready
                @Override
                public void resolve() {
                    PlayerZone library = AllZone.getZone(Constant.Zone.Library, player);
                    PlayerZone hand = AllZone.getZone(Constant.Zone.Hand, player);
                    if (library.size() <= 0)
                        return;

                    peek[0] = library.get(0);
                    boolean wantDiscardThenDraw = false;

                    // We assume that both players will want to peek, ask if they want to reveal.
                    // We do not want to slow down the pace of the game by asking too many questions.
                    // Dialogs outside of the Ability appear at the previous end of turn phase !!!

                    if (peek[0].isValidCard(shareTypes, k.getController(), k)) {
                        if (player.isHuman()) {
                            StringBuilder question = new StringBuilder();
                            question.append("Your top card is ").append(peek[0].getName());
                            question.append(". Reveal card and have both players discard their hand and draw 4 cards?");
                            if (GameActionUtil.showYesNoDialog(k, question.toString())) {
                                wantDiscardThenDraw = true;
                            }
                        }
                        // player isComputer()
                        else {
                            if (library.size() > 4 && hand.size() < 2) {
                                String title = "Computer reveals";
                                revealTopCard(title);
                                wantDiscardThenDraw = true;
                            }
                        }
                    } else if (player.isHuman()) {
                        String title = "Your top card is";
                        revealTopCard(title);
                    }
                    if (wantDiscardThenDraw) {
                        player.discardHand(this);
                        opponent.discardHand(this);

                        player.drawCards(4);
                        opponent.drawCards(4);
                    }
                }// resolve()

                private void revealTopCard(String title) {
                    if (peek[0] != prevCardShown[0]) {
                        GuiUtils.getChoice(title, peek[0]);
                        prevCardShown[0] = peek[0];
                    }
                }// revealTopCard()
            };// ability

            StringBuilder sb = new StringBuilder();
            sb.append("Sensation Gorger - ").append(player);
            sb.append(" triggers Kinship");
            ability.setStackDescription(sb.toString());

            AllZone.getStack().addSimultaneousStackEntry(ability);

        }// for
    }// upkeep_Sensation_Gorger()


    /**
     * <p>upkeep_Squeaking_Pie_Grubfellows.</p>
     */
    private static void upkeep_Squeaking_Pie_Grubfellows() {
        final Player player = AllZone.getPhase().getPlayerTurn();
        CardList kinship = AllZoneUtil.getPlayerCardsInPlay(player, "Squeaking Pie Grubfellows");
        final Player opponent = player.getOpponent();

        PlayerZone library = AllZone.getZone(Constant.Zone.Library, player);
        // Players would not choose to trigger Kinship ability if library is empty.
        // Useful for games when the "Milling = Loss Condition" check box is unchecked.

        if (kinship.size() == 0 || library.size() <= 0)
            return;

        final String[] shareTypes = {"Goblin", "Shaman"};
        final Card[] prevCardShown = {null};
        final Card peek[] = {null};

        for (final Card k : kinship) {
            Ability ability = new Ability(k, "0") {    // change to triggered abilities when ready
                @Override
                public void resolve() {
                    PlayerZone library = AllZone.getZone(Constant.Zone.Library, player);
                    if (library.size() <= 0)
                        return;

                    peek[0] = library.get(0);
                    boolean wantOpponentDiscard = false;

                    // We assume that both players will want to peek, ask if they want to reveal.
                    // We do not want to slow down the pace of the game by asking too many questions.
                    // Dialogs outside of the Ability appear at the previous end of turn phase !!!

                    if (peek[0].isValidCard(shareTypes, k.getController(), k)) {
                        if (player.isHuman()) {
                            StringBuilder question = new StringBuilder();
                            question.append("Your top card is ").append(peek[0].getName());
                            question.append(". Reveal card and have opponent discard a card?");
                            if (GameActionUtil.showYesNoDialog(k, question.toString())) {
                                wantOpponentDiscard = true;
                            }
                        }
                        // player isComputer()
                        else {
                            String title = "Computer reveals";
                            revealTopCard(title);
                            wantOpponentDiscard = true;
                        }
                    } else if (player.isHuman()) {
                        String title = "Your top card is";
                        revealTopCard(title);
                    }

                    if (wantOpponentDiscard) {
                        opponent.discard(this);
                    }
                }// resolve()

                private void revealTopCard(String title) {
                    if (peek[0] != prevCardShown[0]) {
                        GuiUtils.getChoice(title, peek[0]);
                        prevCardShown[0] = peek[0];
                    }
                }// revealTopCard()
            };// ability

            StringBuilder sb = new StringBuilder();
            sb.append("Squeaking Pie Grubfellows - ").append(player);
            sb.append(" triggers Kinship");
            ability.setStackDescription(sb.toString());

            AllZone.getStack().addSimultaneousStackEntry(ability);

        }// for
    }// upkeep_Squeaking_Pie_Grubfellows()


    /**
     * <p>upkeep_Wandering_Graybeard.</p>
     */
    private static void upkeep_Wandering_Graybeard() {
        final Player player = AllZone.getPhase().getPlayerTurn();
        CardList kinship = AllZoneUtil.getPlayerCardsInPlay(player, "Wandering Graybeard");

        PlayerZone library = AllZone.getZone(Constant.Zone.Library, player);
        // Players would not choose to trigger Kinship ability if library is empty.
        // Useful for games when the "Milling = Loss Condition" check box is unchecked.

        if (kinship.size() == 0 || library.size() <= 0)
            return;

        final String[] shareTypes = {"Giant", "Wizard"};
        final Card[] prevCardShown = {null};
        final Card peek[] = {null};

        for (final Card k : kinship) {
            Ability ability = new Ability(k, "0") {    // change to triggered abilities when ready
                @Override
                public void resolve() {
                    PlayerZone library = AllZone.getZone(Constant.Zone.Library, player);
                    if (library.size() <= 0)
                        return;

                    peek[0] = library.get(0);
                    boolean wantGainLife = false;

                    // We assume that both players will want to peek, ask if they want to reveal.
                    // We do not want to slow down the pace of the game by asking too many questions.
                    // Dialogs outside of the Ability appear at the previous end of turn phase !!!

                    if (peek[0].isValidCard(shareTypes, k.getController(), k)) {
                        if (player.isHuman()) {
                            StringBuilder question = new StringBuilder();
                            question.append("Your top card is ").append(peek[0].getName());
                            question.append(". Reveal card and gain 4 life?");
                            if (GameActionUtil.showYesNoDialog(k, question.toString())) {
                                wantGainLife = true;
                            }
                        }
                        // player isComputer()
                        else {
                            String title = "Computer reveals";
                            revealTopCard(title);
                            wantGainLife = true;
                        }
                    } else if (player.isHuman()) {
                        String title = "Your top card is";
                        revealTopCard(title);
                    }
                    if (wantGainLife)
                        player.gainLife(4, k);
                }// resolve()

                private void revealTopCard(String title) {
                    if (peek[0] != prevCardShown[0]) {
                        GuiUtils.getChoice(title, peek[0]);
                        prevCardShown[0] = peek[0];
                    }
                }// revealTopCard()
            };// ability

            StringBuilder sb = new StringBuilder();
            sb.append("Wandering Graybeard - ").append(player);
            sb.append(" triggers Kinship");
            ability.setStackDescription(sb.toString());

            AllZone.getStack().addSimultaneousStackEntry(ability);

        }// for
    }// upkeep_Wandering_Graybeard()


    /**
     * <p>upkeep_Waterspout_Weavers.</p>
     */
    private static void upkeep_Waterspout_Weavers() {
        final Player player = AllZone.getPhase().getPlayerTurn();
        CardList kinship = AllZoneUtil.getPlayerCardsInPlay(player, "Waterspout Weavers");

        PlayerZone library = AllZone.getZone(Constant.Zone.Library, player);
        // Players would not choose to trigger Kinship ability if library is empty.
        // Useful for games when the "Milling = Loss Condition" check box is unchecked.

        if (kinship.size() == 0 || library.size() <= 0)
            return;

        final String[] shareTypes = {"Merfolk", "Wizard"};
        final Card[] prevCardShown = {null};
        final Card peek[] = {null};

        for (final Card k : kinship) {
            Ability ability = new Ability(k, "0") {    // change to triggered abilities when ready
                @Override
                public void resolve() {
                    PlayerZone library = AllZone.getZone(Constant.Zone.Library, player);
                    if (library.size() <= 0)
                        return;

                    peek[0] = library.get(0);
                    boolean wantMerfolkBuff = false;

                    // We assume that both players will want to peek, ask if they want to reveal.
                    // We do not want to slow down the pace of the game by asking too many questions.
                    // Dialogs outside of the Ability appear at the previous end of turn phase !!!

                    if (peek[0].isValidCard(shareTypes, k.getController(), k)) {
                        if (player.isHuman()) {
                            StringBuilder question = new StringBuilder();
                            question.append("Your top card is ").append(peek[0].getName());
                            question.append(". Reveal card and each creature you ");
                            question.append("control gains flying until end of turn?");
                            if (GameActionUtil.showYesNoDialog(k, question.toString())) {
                                wantMerfolkBuff = true;
                            }
                        }
                        // player isComputer()
                        else {
                            String title = "Computer reveals";
                            revealTopCard(title);
                            wantMerfolkBuff = true;
                        }
                    } else if (player.isHuman()) {
                        String title = "Your top card is";
                        revealTopCard(title);
                    }

                    if (wantMerfolkBuff) {
                        CardList creatures = AllZoneUtil.getCreaturesInPlay(player);
                        for (int i = 0; i < creatures.size(); i++) {
                            if (!creatures.get(i).hasKeyword("Flying")) {
                                creatures.get(i).addExtrinsicKeyword("Flying");
                            }
                        }
                        final Command untilEOT = new Command() {
                            private static final long serialVersionUID = -1978446996943583910L;

                            public void execute() {
                                CardList creatures = AllZoneUtil.getCreaturesInPlay(player);
                                for (int i = 0; i < creatures.size(); i++) {
                                    if (creatures.get(i).hasKeyword("Flying")) {
                                        creatures.get(i).removeExtrinsicKeyword("Flying");
                                    }
                                }
                            }
                        };
                        AllZone.getEndOfTurn().addUntil(untilEOT);
                    }
                }// resolve()

                private void revealTopCard(String title) {
                    if (peek[0] != prevCardShown[0]) {
                        GuiUtils.getChoice(title, peek[0]);
                        prevCardShown[0] = peek[0];
                    }
                }// revealTopCard()
            };// ability

            StringBuilder sb = new StringBuilder();
            sb.append("Waterspout Weavers - ").append(player);
            sb.append(" triggers Kinship");
            ability.setStackDescription(sb.toString());

            AllZone.getStack().addSimultaneousStackEntry(ability);

        }// for
    }// upkeep_Waterspout_Weavers()


    /**
     * <p>upkeep_Winnower_Patrol.</p>
     */
    private static void upkeep_Winnower_Patrol() {
        final Player player = AllZone.getPhase().getPlayerTurn();
        CardList kinship = AllZoneUtil.getPlayerCardsInPlay(player, "Winnower Patrol");

        PlayerZone library = AllZone.getZone(Constant.Zone.Library, player);
        // Players would not choose to trigger Kinship ability if library is empty.
        // Useful for games when the "Milling = Loss Condition" check box is unchecked.

        if (kinship.size() == 0 || library.size() <= 0)
            return;

        final String[] shareTypes = {"Elf", "Warrior"};
        final Card[] prevCardShown = {null};
        final Card peek[] = {null};

        for (final Card k : kinship) {
            Ability ability = new Ability(k, "0") {    // change to triggered abilities when ready
                @Override
                public void resolve() {
                    PlayerZone library = AllZone.getZone(Constant.Zone.Library, player);
                    if (library.size() <= 0)
                        return;

                    peek[0] = library.get(0);
                    boolean wantCounter = false;

                    // We assume that both players will want to peek, ask if they want to reveal.
                    // We do not want to slow down the pace of the game by asking too many questions.
                    // Dialogs outside of the Ability appear at the previous end of turn phase !!!

                    if (peek[0].isValidCard(shareTypes, k.getController(), k)) {
                        if (player.isHuman()) {
                            StringBuilder question = new StringBuilder();
                            question.append("Your top card is ").append(peek[0].getName());
                            question.append(". Reveal card and put a +1/+1 counter on Winnower Patrol?");
                            if (GameActionUtil.showYesNoDialog(k, question.toString())) {
                                wantCounter = true;
                            }
                        }
                        // player isComputer()
                        else {
                            String title = "Computer reveals";
                            revealTopCard(title);
                            wantCounter = true;
                        }
                    } else if (player.isHuman()) {
                        String title = "Your top card is";
                        revealTopCard(title);
                    }
                    if (wantCounter)
                        k.addCounter(Counters.P1P1, 1);
                }// resolve()

                private void revealTopCard(String title) {
                    if (peek[0] != prevCardShown[0]) {
                        GuiUtils.getChoice(title, peek[0]);
                        prevCardShown[0] = peek[0];
                    }
                }// revealTopCard()
            };// ability

            StringBuilder sb = new StringBuilder();
            sb.append("Winnower Patrol - ").append(player);
            sb.append(" triggers Kinship");
            ability.setStackDescription(sb.toString());

            AllZone.getStack().addSimultaneousStackEntry(ability);

        }// for
    }// upkeep_Winnower_Patrol()


    /**
     * <p>upkeep_Wolf_Skull_Shaman.</p>
     */
    private static void upkeep_Wolf_Skull_Shaman() {
        final Player player = AllZone.getPhase().getPlayerTurn();
        CardList kinship = AllZoneUtil.getPlayerCardsInPlay(player, "Wolf-Skull Shaman");

        PlayerZone library = AllZone.getZone(Constant.Zone.Library, player);
        // Players would not choose to trigger Kinship ability if library is empty.
        // Useful for games when the "Milling = Loss Condition" check box is unchecked.

        if (kinship.size() == 0 || library.size() <= 0)
            return;

        final String[] shareTypes = {"Elf", "Shaman"};
        final Card[] prevCardShown = {null};
        final Card peek[] = {null};

        for (final Card k : kinship) {
            Ability ability = new Ability(k, "0") {    // change to triggered abilities when ready
                @Override
                public void resolve() {
                    PlayerZone library = AllZone.getZone(Constant.Zone.Library, player);
                    if (library.size() <= 0)
                        return;

                    peek[0] = library.get(0);
                    boolean wantToken = false;

                    // We assume that both players will want to peek, ask if they want to reveal.
                    // We do not want to slow down the pace of the game by asking too many questions.
                    // Dialogs outside of the Ability appear at the previous end of turn phase !!!

                    if (peek[0].isValidCard(shareTypes, k.getController(), k)) {
                        if (player.isHuman()) {
                            StringBuilder question = new StringBuilder();
                            question.append("Your top card is ").append(peek[0].getName());
                            question.append(". Reveal card and put a 2/2 green Wolf creature token onto the battlefield?");
                            if (GameActionUtil.showYesNoDialog(k, question.toString())) {
                                wantToken = true;
                            }
                        }
                        // player isComputer()
                        else {
                            String title = "Computer reveals";
                            revealTopCard(title);
                            wantToken = true;
                        }
                    } else if (player.isHuman()) {
                        String title = "Your top card is";
                        revealTopCard(title);
                    }

                    if (wantToken)
                        CardFactoryUtil.makeToken("Wolf", "G 2 2 Wolf", k.getController(), "G",
                                new String[]{"Creature", "Wolf"}, 2, 2, new String[]{""});
                }// resolve()

                private void revealTopCard(String title) {
                    if (peek[0] != prevCardShown[0]) {
                        GuiUtils.getChoice(title, peek[0]);
                        prevCardShown[0] = peek[0];
                    }
                }// revealTopCard()
            };// ability

            StringBuilder sb = new StringBuilder();
            sb.append("Wolf-Skull Shaman - ").append(player);
            sb.append(" triggers Kinship");
            ability.setStackDescription(sb.toString());

            AllZone.getStack().addSimultaneousStackEntry(ability);

        }// for
    }// upkeep_Wolf_Skull_Shaman()


    ///////////////////////
    // End of Kinship cards
    ///////////////////////


    /**
     * <p>upkeep_Dark_Confidant.</p>
     */
    private static void upkeep_Dark_Confidant() {
        final Player player = AllZone.getPhase().getPlayerTurn();

        CardList list = AllZoneUtil.getPlayerCardsInPlay(player);
        list = list.filter(new CardListFilter() {
            public boolean addCard(Card c) {
                return c.getName().equals("Dark Confidant") || c.getName().equals("Dark Tutelage");
            }
        });

        Ability ability;
        for (int i = 0; i < list.size(); i++) {
            final Card F_card = list.get(i);
            ability = new Ability(F_card, "0") {
                @Override
                public void resolve() {
                    CardList lib = AllZoneUtil.getPlayerCardsInLibrary(player);
                    if (lib.size() > 0) {
                        Card toMove = lib.get(0);
                        AllZone.getGameAction().moveToHand(toMove);
                        player.loseLife(toMove.getCMC(), F_card);
                    }
                }// resolve()
            };// Ability

            StringBuilder sb = new StringBuilder();
            sb.append(F_card).append(" - ").append("At the beginning of your upkeep, reveal the top card of your library and put that card into your hand. You lose life equal to its converted mana cost.");
            ability.setStackDescription(sb.toString());

            AllZone.getStack().addSimultaneousStackEntry(ability);

        }// for
    }// upkeep_Dark_Confidant()

    /**
     * <p>upkeep_Suspend.</p>
     */
    public static void upkeep_Suspend() {
        Player player = AllZone.getPhase().getPlayerTurn();

        CardList list = AllZoneUtil.getPlayerCardsInExile(player);

        list = list.filter(new CardListFilter() {
            public boolean addCard(Card c) {
                return c.hasSuspend();
            }
        });

        if (list.size() == 0) return;

        for (final Card c : list) {
            int counters = c.getCounters(Counters.TIME);
            if (counters > 0) c.subtractCounter(Counters.TIME, 1);
        }
    }//suspend	

    /**
     * <p>upkeep_Vanishing.</p>
     */
    private static void upkeep_Vanishing() {

        final Player player = AllZone.getPhase().getPlayerTurn();
        CardList list = AllZoneUtil.getPlayerCardsInPlay(player);
        list = list.filter(new CardListFilter() {
            public boolean addCard(Card c) {
                return CardFactoryUtil.hasKeyword(c, "Vanishing") != -1;
            }
        });
        if (list.size() > 0) {
            for (int i = 0; i < list.size(); i++) {
                final Card card = list.get(i);
                Ability ability = new Ability(card, "0") {
                    @Override
                    public void resolve() {
                        card.subtractCounter(Counters.TIME, 1);
                    }
                }; // ability

                StringBuilder sb = new StringBuilder();
                sb.append(card.getName()).append(" - Vanishing - remove a time counter from it. ");
                sb.append("When the last is removed, sacrifice it.)");
                ability.setStackDescription(sb.toString());

                AllZone.getStack().addSimultaneousStackEntry(ability);

            }
        }
    }

    /**
     * <p>upkeep_Fading.</p>
     */
    private static void upkeep_Fading() {

        final Player player = AllZone.getPhase().getPlayerTurn();
        CardList list = AllZoneUtil.getPlayerCardsInPlay(player);
        list = list.filter(new CardListFilter() {
            public boolean addCard(Card c) {
                return CardFactoryUtil.hasKeyword(c, "Fading") != -1;
            }
        });
        if (list.size() > 0) {
            for (int i = 0; i < list.size(); i++) {
                final Card card = list.get(i);
                Ability ability = new Ability(card, "0") {
                    @Override
                    public void resolve() {
                        int fadeCounters = card.getCounters(Counters.FADE);
                        if (fadeCounters <= 0)
                            AllZone.getGameAction().sacrifice(card);
                        else
                            card.subtractCounter(Counters.FADE, 1);
                    }
                }; // ability

                StringBuilder sb = new StringBuilder();
                sb.append(card.getName()).append(" - Fading - remove a fade counter from it. ");
                sb.append("If you can't, sacrifice it.)");
                ability.setStackDescription(sb.toString());

                AllZone.getStack().addSimultaneousStackEntry(ability);

            }
        }
    }

    /**
     * <p>upkeep_Oath_of_Druids.</p>
     */
    private static void upkeep_Oath_of_Druids() {
        CardList oathList = AllZoneUtil.getCardsInPlay("Oath of Druids");
        if (oathList.isEmpty())
            return;

        final Player player = AllZone.getPhase().getPlayerTurn();

        if (AllZoneUtil.compareTypeAmountInPlay(player, "Creature") < 0) {
            for (int i = 0; i < oathList.size(); i++) {
                final Card oath = oathList.get(i);
                Ability ability = new Ability(oath, "0") {
                    @Override
                    public void resolve() {
                        CardList libraryList = AllZoneUtil.getPlayerCardsInLibrary(player);
                        PlayerZone battlefield = AllZone.getZone(Constant.Zone.Battlefield, player);
                        boolean oathFlag = true;

                        if (AllZoneUtil.compareTypeAmountInPlay(player, "Creature") < 0) {
                            if (player.isHuman()) {
                                StringBuilder question = new StringBuilder();
                                question.append("Reveal cards from the top of your library and place ");
                                question.append("the first creature revealed onto the battlefield?");
                                if (!GameActionUtil.showYesNoDialog(oath, question.toString())) {
                                    oathFlag = false;
                                }
                            } else {    // if player == Computer
                                CardList creaturesInLibrary = AllZoneUtil.getPlayerTypeInLibrary(player, "Creature");
                                CardList creaturesInBattlefield = AllZoneUtil.getPlayerTypeInPlay(player, "Creature");

                                // if there are at least 3 creatures in library, or none in play with one in library, oath
                                if (creaturesInLibrary.size() > 2
                                        || (creaturesInBattlefield.size() == 0 && creaturesInLibrary.size() > 0))
                                    oathFlag = true;
                                else
                                    oathFlag = false;
                            }

                            if (oathFlag) {
                                CardList cardsToReveal = new CardList();
                                int max = libraryList.size();
                                for (int i = 0; i < max; i++) {
                                    Card c = libraryList.get(i);
                                    cardsToReveal.add(c);
                                    if (c.isCreature()) {
                                        AllZone.getGameAction().moveTo(battlefield, c);
                                        break;
                                    } else {
                                        AllZone.getGameAction().moveToGraveyard(c);
                                    }
                                }// for loop
                                if (cardsToReveal.size() > 0)
                                    GuiUtils.getChoice("Revealed cards", cardsToReveal.toArray());
                            }
                        }
                    }
                };// Ability

                StringBuilder sb = new StringBuilder();
                sb.append("At the beginning of each player's upkeep, that player chooses target player ");
                sb.append("who controls more creatures than he or she does and is his or her opponent. The ");
                sb.append("first player may reveal cards from the top of his or her library until he or she ");
                sb.append("reveals a creature card. If he or she does, that player puts that card onto the ");
                sb.append("battlefield and all other cards revealed this way into his or her graveyard.");
                ability.setStackDescription(sb.toString());

                AllZone.getStack().addSimultaneousStackEntry(ability);

            }
        }
    }// upkeep_Oath of Druids()

    /**
     * <p>upkeep_Oath_of_Ghouls.</p>
     */
    private static void upkeep_Oath_of_Ghouls() {
        CardList oathList = AllZoneUtil.getCardsInPlay("Oath of Ghouls");
        if (oathList.isEmpty())
            return;

        final Player player = AllZone.getPhase().getPlayerTurn();

        if (AllZoneUtil.compareTypeAmountInGraveyard(player, "Creature") > 0) {
            for (int i = 0; i < oathList.size(); i++) {
                Ability ability = new Ability(oathList.get(0), "0") {
                    @Override
                    public void resolve() {
                        CardList graveyardCreatures = AllZoneUtil.getPlayerTypeInGraveyard(player, "Creature");

                        if (AllZoneUtil.compareTypeAmountInGraveyard(player, "Creature") > 0) {
                            if (player.isHuman()) {
                                Object o = GuiUtils.getChoiceOptional("Pick a creature to return to hand",
                                        graveyardCreatures.toArray());
                                if (o != null) {
                                    Card card = (Card) o;

                                    AllZone.getGameAction().moveToHand(card);
                                }
                            } else if (player.isComputer()) {
                                Card card = graveyardCreatures.get(0);

                                AllZone.getGameAction().moveToHand(card);
                            }
                        }
                    }
                };// Ability

                StringBuilder sb = new StringBuilder();
                sb.append("At the beginning of each player's upkeep, Oath of Ghouls returns a creature ");
                sb.append("from their graveyard to owner's hand if they have more than an opponent.");
                ability.setStackDescription(sb.toString());

                AllZone.getStack().addSimultaneousStackEntry(ability);

            }
        }
    }//Oath of Ghouls


    /**
     * <p>upkeep_Karma.</p>
     */
    private static void upkeep_Karma() {
        final Player player = AllZone.getPhase().getPlayerTurn();
        CardList karmas = AllZoneUtil.getCardsInPlay("Karma");
        CardList swamps = AllZoneUtil.getPlayerTypeInPlay(player, "Swamp");

        // determine how much damage to deal the current player
        final int damage = swamps.size();

        // if there are 1 or more Karmas on the  
        // battlefield have each of them deal damage.
        if (0 < karmas.size()) {
            for (Card karma : karmas) {
                final Card src = karma;
                Ability ability = new Ability(src, "0") {
                    @Override
                    public void resolve() {
                        if (damage > 0) {
                            player.addDamage(damage, src);
                        }
                    }
                };// Ability
                if (damage > 0) {

                    StringBuilder sb = new StringBuilder();
                    sb.append("Karma deals ").append(damage).append(" damage to ").append(player);
                    ability.setStackDescription(sb.toString());

                    AllZone.getStack().addSimultaneousStackEntry(ability);

                }
            }
        }// if
    }// upkeep_Karma()


    /**
     * <p>upkeep_Dega_Sanctuary.</p>
     */
    private static void upkeep_Dega_Sanctuary() {
        final Player player = AllZone.getPhase().getPlayerTurn();

        CardList list = AllZoneUtil.getPlayerCardsInPlay(player, "Dega Sanctuary");

        for (Card sanc : list) {
            final Card source = sanc;
            final Ability ability = new Ability(source, "0") {
                public void resolve() {
                    int gain = 0;
                    CardList play = AllZoneUtil.getPlayerCardsInPlay(player);
                    CardList black = play.filter(AllZoneUtil.black);
                    CardList red = play.filter(AllZoneUtil.red);
                    if (black.size() > 0 && red.size() > 0) gain = 4;
                    else if (black.size() > 0 || red.size() > 0) gain = 2;
                    player.gainLife(gain, source);
                }
            };//Ability

            StringBuilder sb = new StringBuilder();
            sb.append(source.getName()).append(" - ");
            sb.append("if you control a black or red permanent, you gain 2 life. If you control a black permanent and a red permanent, you gain 4 life instead.");
            ability.setStackDescription(sb.toString());

            AllZone.getStack().addSimultaneousStackEntry(ability);

        }//for
    }//upkeep_Dega_Sanctuary()

    /**
     * <p>upkeep_Ceta_Sanctuary.</p>
     */
    private static void upkeep_Ceta_Sanctuary() {
        final Player player = AllZone.getPhase().getPlayerTurn();

        CardList list = AllZoneUtil.getPlayerCardsInPlay(player, "Ceta Sanctuary");

        for (Card sanc : list) {
            final Card source = sanc;
            final Ability ability = new Ability(source, "0") {
                public void resolve() {
                    int draw = 0;
                    CardList play = AllZoneUtil.getPlayerCardsInPlay(player);
                    CardList green = play.filter(AllZoneUtil.green);
                    CardList red = play.filter(AllZoneUtil.red);

                    if (green.size() > 0 && red.size() > 0) draw = 2;
                    else if (green.size() > 0 || red.size() > 0) draw = 1;

                    if (draw > 0) {
                        player.drawCards(draw);
                        player.discard(1, this, true);
                    }
                }
            };//Ability

            StringBuilder sb = new StringBuilder();
            sb.append(source).append(" - ");
            sb.append("At the beginning of your upkeep, if you control a red or green permanent, draw a card, then discard a card. If you control a red permanent and a green permanent, instead draw two cards, then discard a card.");
            ability.setStackDescription(sb.toString());

            AllZone.getStack().addSimultaneousStackEntry(ability);

        }//for
    }//upkeep_Ceta_Sanctuary()

    /**
     * <p>upkeep_Power_Surge.</p>
     */
    private static void upkeep_Power_Surge() {
        /*
		 * At the beginning of each player's upkeep, Power Surge deals X
		 * damage to that player, where X is the number of untapped
		 * lands he or she controlled at the beginning of this turn.
		 */
        final Player player = AllZone.getPhase().getPlayerTurn();
        CardList list = AllZoneUtil.getCardsInPlay("Power Surge");
        final int damage = player.getNumPowerSurgeLands();

        for (Card surge : list) {
            final Card source = surge;
            Ability ability = new Ability(source, "0") {
                @Override
                public void resolve() {
                    player.addDamage(damage, source);
                }
            };// Ability

            StringBuilder sb = new StringBuilder();
            sb.append(source).append(" - deals ").append(damage).append(" damage to ").append(player);
            ability.setStackDescription(sb.toString());

            if (damage > 0) {
                AllZone.getStack().addSimultaneousStackEntry(ability);

            }
        }// for
    }// upkeep_Power_Surge()

    /**
     * <p>upkeep_Felidar_Sovereign.</p>
     */
    private static void upkeep_Felidar_Sovereign() {
        final Player player = AllZone.getPhase().getPlayerTurn();

        CardList list = AllZoneUtil.getPlayerCardsInPlay(player, "Felidar Sovereign");

        if (0 < list.size() && player.getLife() >= 40) {
            final Card source = list.get(0);
            Ability ability = new Ability(source, "0") {
                @Override
                public void resolve() {
                    if (player.getLife() >= 40)
                        player.altWinBySpellEffect(source.getName());
                }
            };// Ability

            StringBuilder sb = new StringBuilder();
            sb.append("Felidar Sovereign - ").append(player).append(" wins the game");
            ability.setStackDescription(sb.toString());

            AllZone.getStack().addSimultaneousStackEntry(ability);

        }// if
    }// upkeep_Felidar_Sovereign

    
    /**
     * <p>upkeep_Mortal_Combat.</p>
     */
    private static void upkeep_Mortal_Combat() {
        final Player player = AllZone.getPhase().getPlayerTurn();

        CardList list = AllZoneUtil.getPlayerCardsInPlay(player, "Mortal Combat");
        CardList grave = AllZoneUtil.getPlayerGraveyard(player);
        grave = grave.filter(AllZoneUtil.creatures);

        if (0 < list.size() && 20 <= grave.size()) {
            final Card source = list.get(0);
            Ability ability = new Ability(source, "0") {
                @Override
                public void resolve() {
                    CardList grave = AllZoneUtil.getPlayerGraveyard(player);
                    grave = grave.filter(AllZoneUtil.creatures);
                    if (grave.size() >= 20)
                        player.altWinBySpellEffect(source.getName());
                }
            };// Ability

            StringBuilder sb = new StringBuilder();
            sb.append("Mortal Combat - ").append(player).append(" wins the game");
            ability.setStackDescription(sb.toString());

            AllZone.getStack().addSimultaneousStackEntry(ability);

        }// if
    }// upkeep_Mortal Combat

    /**
     * <p>upkeep_Helix_Pinnacle.</p>
     */
    private static void upkeep_Helix_Pinnacle() {
        final Player player = AllZone.getPhase().getPlayerTurn();

        CardList list = AllZoneUtil.getPlayerCardsInPlay(player, "Helix Pinnacle");

        for (final Card c : list) {
            if (c.getCounters(Counters.TOWER) < 100) continue;

            Ability ability = new Ability(c, "0") {
                @Override
                public void resolve() {
                    if (c.getCounters(Counters.TOWER) >= 100)
                        player.altWinBySpellEffect(c.getName());
                }
            };// Ability

            StringBuilder sb = new StringBuilder();
            sb.append("Helix Pinnacle - ").append(player).append(" wins the game");
            ability.setStackDescription(sb.toString());

            AllZone.getStack().addSimultaneousStackEntry(ability);

        }// if
    }// upkeep_Helix_Pinnacle

    /**
     * <p>upkeep_Near_Death_Experience.</p>
     */
    private static void upkeep_Near_Death_Experience() {
        /*
		 * At the beginning of your upkeep, if you have exactly 1 life, you win the game.
		 */
        final Player player = AllZone.getPhase().getPlayerTurn();

        CardList list = AllZoneUtil.getPlayerCardsInPlay(player, "Near-Death Experience");

        if (0 < list.size() && player.getLife() == 1) {
            final Card source = list.get(0);
            Ability ability = new Ability(source, "0") {
                @Override
                public void resolve() {
                    if (player.getLife() == 1)
                        player.altWinBySpellEffect(source.getName());
                }
            };// Ability

            StringBuilder sb = new StringBuilder();
            sb.append("Near-Death Experience - ").append(player).append(" wins the game");
            ability.setStackDescription(sb.toString());

            AllZone.getStack().addSimultaneousStackEntry(ability);

        }// if
    }// upkeep_Near_Death_Experience

    /**
     * <p>upkeep_Test_of_Endurance.</p>
     */
    private static void upkeep_Test_of_Endurance() {
        /*
		 * At the beginning of your upkeep, if you have 50 or more life, you win the game.
		 */
        final Player player = AllZone.getPhase().getPlayerTurn();

        CardList list = AllZoneUtil.getPlayerCardsInPlay(player, "Test of Endurance");

        if (0 < list.size() && player.getLife() >= 50) {
            final Card source = list.get(0);
            Ability ability = new Ability(source, "0") {
                @Override
                public void resolve() {
                    if (player.getLife() >= 50)
                        player.altWinBySpellEffect(source.getName());
                }
            };// Ability

            StringBuilder sb = new StringBuilder();
            sb.append(list.get(0)).append(" - ").append(player).append(" wins the game");
            ability.setStackDescription(sb.toString());

            AllZone.getStack().addSimultaneousStackEntry(ability);

        }// if
    }// upkeep_Test_of_Endurance


    /**
     * <p>upkeep_Barren_Glory.</p>
     */
    private static void upkeep_Barren_Glory() {
        final Player player = AllZone.getPhase().getPlayerTurn();
        PlayerZone handZone = AllZone.getZone(Constant.Zone.Hand, player);

        CardList list = AllZoneUtil.getPlayerCardsInPlay(player);
        CardList playList = AllZoneUtil.getPlayerCardsInPlay(player);
        playList = playList.filter(new CardListFilter() {
            public boolean addCard(Card c) {
                return !c.getName().equals("Mana Pool");
            }
        });

        list = list.getName("Barren Glory");

        if (playList.size() == 1 && list.size() == 1 && handZone.size() == 0) {
            final Card source = list.get(0);
            Ability ability = new Ability(source, "0") {
                @Override
                public void resolve() {
                    CardList handList = AllZoneUtil.getCardsInZone(Constant.Zone.Hand, player);
                    CardList playList = AllZoneUtil.getCardsInZone(Constant.Zone.Battlefield, player);
                    playList = playList.getValidCards("Permanents".split(","), source.getController(), source);
                    playList.remove(source);

                    if (playList.size() == 0 && handList.size() == 0)
                        player.altWinBySpellEffect(source.getName());
                }
            };// Ability

            StringBuilder sb = new StringBuilder();
            sb.append("Barren Glory - ").append(player).append(" wins the game");
            ability.setStackDescription(sb.toString());

            AllZone.getStack().addSimultaneousStackEntry(ability);

        }// if
    }// upkeep_Barren_Glory

    /**
     * <p>upkeep_Sleeper_Agent.</p>
     */
    private static void upkeep_Sleeper_Agent() {
        final Player player = AllZone.getPhase().getPlayerTurn();

        CardList list = AllZoneUtil.getPlayerCardsInPlay(player, "Sleeper Agent");

        Ability ability;
        for (int i = 0; i < list.size(); i++) {
            final Card F_card = list.get(i);
            ability = new Ability(list.get(i), "0") {
                @Override
                public void resolve() {
                    player.addDamage(2, F_card);
                }
            };

            ability.setStackDescription("Sleeper Agent deals 2 damage to its controller.");

            AllZone.getStack().addSimultaneousStackEntry(ability);

        }
    }//upkeep_Sleeper_Agent

    /**
     * <p>upkeep_Shapeshifter.</p>
     */
    private static void upkeep_Shapeshifter() {
        final Player player = AllZone.getPhase().getPlayerTurn();
        CardList list = AllZoneUtil.getPlayerCardsInPlay(player, "Shapeshifter");
        list = list.filter(AllZoneUtil.nonToken);

        for (final Card c : list) {
            SpellAbility ability = new Ability(c, "0") {
                @Override
                public void resolve() {
                    int num = 0;
                    if (player.isHuman()) {
                        String[] choices = new String[7];
                        for (int j = 0; j < 7; j++) {
                            choices[j] = "" + j;
                        }
                        String answer = (String) (GuiUtils.getChoiceOptional(c.getName() + " - Choose a number", choices));
                        num = Integer.parseInt(answer);
                    } else {
                        num = 3;
                    }
                    c.setBaseAttack(num);
                    c.setBaseDefense(7 - num);
                }
            };
            ability.setStackDescription(c.getName() + " - choose a new number");

            AllZone.getStack().addSimultaneousStackEntry(ability);

        }//foreach(Card)
    }//upkeep_Shapeshifter

    /**
     * <p>upkeep_Vesuvan_Doppelganger_Keyword.</p>
     */
    private static void upkeep_Vesuvan_Doppelganger_Keyword() {
        // TODO: what about enchantments? i dont know how great this solution is
        final Player player = AllZone.getPhase().getPlayerTurn();
        final String keyword = "At the beginning of your upkeep, you may have this creature become a copy of target creature except it doesn't copy that creature's color. If you do, this creature gains this ability.";
        CardList list = AllZoneUtil.getPlayerCardsInPlay(player);
        list = list.filter(AllZoneUtil.getKeywordFilter(keyword));

        for (final Card c : list) {
            final SpellAbility ability = new Ability(c, "0") {
                @Override
                public void resolve() {
                    final Card[] newTarget = new Card[1];
                    newTarget[0] = null;

                    final Ability switchTargets = new Ability(c, "0") {
                        public void resolve() {
                            if (newTarget[0] != null) {
                                /*
								 * 1. need to select new card - DONE
								 * 1a. need to create the newly copied card with pic and setinfo
								 * 2. need to add the leaves play command
								 * 3. need to transfer the keyword
								 * 4. need to update the clone origin of new card and old card
								 * 5. remove clone leaves play commands from old
								 * 5a. remove old from play
								 * 6. add new to play
								 */

                                Card newCopy = AllZone.getCardFactory().getCard(newTarget[0].getName(), player);
                                newCopy.setCurSetCode(newTarget[0].getCurSetCode());
                                newCopy.setImageFilename(newTarget[0].getImageFilename());

                                //need to add the leaves play command (2)
                                newCopy.addLeavesPlayCommand(c.getCloneLeavesPlayCommand());
                                c.removeTrigger(c.getCloneLeavesPlayCommand(), ZCTrigger.LEAVEFIELD);
                                newCopy.setCloneLeavesPlayCommand(c.getCloneLeavesPlayCommand());

                                newCopy.addExtrinsicKeyword(keyword);
                                newCopy.addColor("U", newCopy, false, true);
                                newCopy.setCloneOrigin(c.getCloneOrigin());
                                newCopy.getCloneOrigin().setCurrentlyCloningCard(newCopy);
                                c.setCloneOrigin(null);

                                //5
                                PlayerZone play = AllZone.getZone(c);
                                play.remove(c);

                                play.add(newCopy);
                            }
                        }
                    };

                    AllZone.getInputControl().setInput(new Input() {
                        private static final long serialVersionUID = 5662272658873063221L;

                        @Override
                        public void showMessage() {
                            AllZone.getDisplay().showMessage(c.getName() + " - Select new target creature.  (Click Cancel to remain as is.)");
                            ButtonUtil.enableOnlyCancel();
                        }

                        @Override
                        public void selectButtonCancel() {
                            stop();
                        }

                        @Override
                        public void selectCard(Card selectedCard, PlayerZone z) {
                            if (z.is(Constant.Zone.Battlefield) && selectedCard.isCreature()
                                    && CardFactoryUtil.canTarget(c, selectedCard)) {
                                newTarget[0] = selectedCard;
                                StringBuilder sb = new StringBuilder();
                                sb.append(c.getCloneOrigin()).append(" - switching to copy " + selectedCard.getName() + ".");
                                switchTargets.setStackDescription(sb.toString());
                                AllZone.getStack().add(switchTargets);
                                stop();
                            }
                        }
                    });
                }
            };
            ability.setDescription("At the beginning of your upkeep, you may have this creature become a copy of target creature except it doesn't copy that creature's color. If you do, this creature gains this ability.");
            ability.setStackDescription(c.getName() + " - you may have this creature become a copy of target creature.");

            AllZone.getStack().addSimultaneousStackEntry(ability);

        }//foreach(Card)
    }//upkeep_Vesuvan_Doppelganger_Keyword

    /**
     * <p>upkeep_Tangle_Wire.</p>
     */
    private static void upkeep_Tangle_Wire() {
        final Player player = AllZone.getPhase().getPlayerTurn();
        CardList wires = AllZoneUtil.getCardsInPlay("Tangle Wire");

        for (final Card source : wires) {
            SpellAbility ability = new Ability(source, "0") {
                @Override
                public void resolve() {
                    final int num = source.getCounters(Counters.FADE);
                    final CardList list = AllZoneUtil.getPlayerCardsInPlay(player).filter(new CardListFilter() {
                        public boolean addCard(Card c) {
                            return (c.isArtifact() || c.isLand() || c.isCreature()) && c.isUntapped();
                        }
                    });

                    for (int i = 0; i < num; i++) {
                        if (player.isComputer()) {
                            Card toTap = CardFactoryUtil.AI_getWorstPermanent(list, false, false, false, false);
                            if (null != toTap) {
                                toTap.tap();
                                list.remove(toTap);
                            }
                        } else {
                            AllZone.getInputControl().setInput(new Input() {
                                private static final long serialVersionUID = 5313424586016061612L;

                                public void showMessage() {
                                    if (list.size() == 0) {
                                        stop();
                                        return;
                                    }
                                    AllZone.getDisplay().showMessage(source.getName() + " - Select " + num + " untapped artifact(s), creature(s), or land(s) you control");
                                    ButtonUtil.disableAll();
                                }

                                public void selectCard(Card card, PlayerZone zone) {
                                    if (zone.is(Constant.Zone.Battlefield, AllZone.getHumanPlayer()) && list.contains(card)) {
                                        card.tap();
                                        list.remove(card);
                                        stop();
                                    }
                                }
                            });
                        }
                    }
                }
            };
            ability.setStackDescription(source.getName() + " - " + player + " taps X artifacts, creatures or lands he or she controls.");

            AllZone.getStack().addSimultaneousStackEntry(ability);

        }//foreach(wire)
    }//upkeep_Tangle_Wire()

    /**
     * <p>upkeep_Masticore.</p>
     */
    private static void upkeep_Masticore() {
        final Player player = AllZone.getPhase().getPlayerTurn();

        CardList list = AllZoneUtil.getPlayerCardsInPlay(player, "Masticore");
        list.addAll(AllZoneUtil.getPlayerCardsInPlay(player, "Molten-Tail Masticore"));
        list.addAll(AllZoneUtil.getPlayerCardsInPlay(player, "Razormane Masticore"));

        Ability ability;
        for (int i = 0; i < list.size(); i++) {
            final Card crd = list.get(i);

            final Input discard = new Input() {
                private static final long serialVersionUID = 2252076866782738069L;

                @Override
                public void showMessage() {
                    AllZone.getDisplay().showMessage(crd + " - Discard a card from your hand");
                    ButtonUtil.enableOnlyCancel();
                }

                @Override
                public void selectCard(Card c, PlayerZone zone) {
                    if (zone.is(Constant.Zone.Hand)) {
                        c.getController().discard(c, null);
                        stop();
                    }
                }

                @Override
                public void selectButtonCancel() {
                    AllZone.getGameAction().sacrifice(crd);
                    stop();
                }
            };//Input

            ability = new Ability(crd, "0") {
                @Override
                public void resolve() {
                    if (crd.getController().isHuman()) {
                        if (AllZone.getHumanHand().size() == 0) AllZone.getGameAction().sacrifice(crd);
                        else AllZone.getInputControl().setInput(discard);
                    } else //comp
                    {
                        CardList list = AllZoneUtil.getPlayerHand(AllZone.getComputerPlayer());

                        if (list.size() != 0) list.get(0).getController().discard(list.get(0), this);
                        else AllZone.getGameAction().sacrifice(crd);
                    }//else
                }//resolve()
            };//Ability

            StringBuilder sb = new StringBuilder();
            sb.append(crd).append(" - sacrifice ").append(crd).append(" unless you discard a card.");
            ability.setStackDescription(sb.toString());
            ability.setDescription(sb.toString());

            AllZone.getStack().addSimultaneousStackEntry(ability);

        }// for
    }//upkeep_Masticore


    /**
     * <p>upkeep_Eldrazi_Monument.</p>
     */
    private static void upkeep_Eldrazi_Monument() {
        final Player player = AllZone.getPhase().getPlayerTurn();

        CardList list = AllZoneUtil.getPlayerCardsInPlay(player, "Eldrazi Monument");

        Ability ability;
        for (int i = 0; i < list.size(); i++) {
            final Card card = list.get(i);
            ability = new Ability(list.get(i), "0") {
                @Override
                public void resolve() {
                    CardList creats = AllZoneUtil.getCreaturesInPlay(player);

                    if (creats.size() < 1) {
                        AllZone.getGameAction().sacrifice(card);
                        return;
                    }

                    if (player.isHuman()) {
                        Object o = GuiUtils.getChoiceOptional("Select creature to sacrifice",
                                creats.toArray());
                        Card sac = (Card) o;
                        if (sac == null) {
                            creats.shuffle();
                            sac = creats.get(0);
                        }
                        AllZone.getGameAction().sacrifice(sac);
                    } else//computer
                    {
                        CardListUtil.sortAttackLowFirst(creats);
                        AllZone.getGameAction().sacrifice(creats.get(0));
                    }
                }
            };// ability

            StringBuilder sb = new StringBuilder();
            sb.append("Eldrazi Monument - ").append(player).append(" sacrifices a creature.");
            ability.setStackDescription(sb.toString());

            AllZone.getStack().addSimultaneousStackEntry(ability);

        }

    }//upkeep_Eldrazi_Monument

    /**
     * <p>upkeep_Blaze_Counters.</p>
     */
    private static void upkeep_Blaze_Counters() {
        final Player player = AllZone.getPhase().getPlayerTurn();

        CardList blaze = AllZoneUtil.getPlayerCardsInPlay(player);
        blaze = blaze.filter(new CardListFilter() {
            public boolean addCard(Card c) {
                return c.isLand() && c.getCounters(Counters.BLAZE) > 0;
            }
        });

        for (int i = 0; i < blaze.size(); i++) {
            final Card Source = blaze.get(i);
            Ability ability = new Ability(blaze.get(i), "0") {
                @Override
                public void resolve() {
                    player.addDamage(1, Source);
                }
            };// ability

            StringBuilder sb = new StringBuilder();
            sb.append(blaze.get(i)).append(" - has a blaze counter and deals 1 damage to ");
            sb.append(player).append(".");
            ability.setStackDescription(sb.toString());

            AllZone.getStack().addSimultaneousStackEntry(ability);

        }
    }
    
    /**
     * <p>upkeep_Carnophage.</p>
     */
    private static void upkeep_Carnophage() {
        final Player player = AllZone.getPhase().getPlayerTurn();

        CardList list = AllZoneUtil.getPlayerCardsInPlay(player, "Carnophage");
        if (player.isHuman()) {
            for (int i = 0; i < list.size(); i++) {
                Card c = list.get(i);
                String[] choices = {"Yes", "No"};
                Object choice = GuiUtils.getChoice("Pay Carnophage's upkeep?", choices);
                if (choice.equals("Yes")) player.loseLife(1, c);
                else c.tap();
            }
        } else if (player.isComputer()) for (int i = 0; i < list.size(); i++) {
            Card c = list.get(i);
            if (AllZone.getComputerPlayer().getLife() > 1) player.loseLife(1, c);
            else c.tap();
        }
    }// upkeep_Carnophage

    /**
     * <p>upkeep_Sangrophage.</p>
     */
    private static void upkeep_Sangrophage() {
        final Player player = AllZone.getPhase().getPlayerTurn();

        CardList list = AllZoneUtil.getPlayerCardsInPlay(player, "Sangrophage");
        if (player.isHuman()) {
            for (int i = 0; i < list.size(); i++) {
                Card c = list.get(i);
                String[] choices = {"Yes", "No"};
                Object choice = GuiUtils.getChoice("Pay Sangrophage's upkeep?", choices);
                if (choice.equals("Yes")) player.loseLife(2, c);
                else c.tap();
            }
        } else if (player.isComputer()) for (int i = 0; i < list.size(); i++) {
            Card c = list.get(i);
            if (AllZone.getComputerPlayer().getLife() > 2) player.loseLife(2, c);
            else c.tap();
        }
    }// upkeep_Carnophage    
}//end class Upkeep
