package forge;

import java.util.ArrayList;
import java.util.HashMap;

import forge.Constant.Zone;
import forge.card.cardfactory.CardFactoryUtil;
import forge.card.spellability.Ability;
import forge.card.spellability.AbilityMana;
import forge.card.spellability.AbilityStatic;
import forge.card.spellability.SpellAbility;
import forge.gui.GuiUtils;
import forge.gui.input.Input;

//handles "until next upkeep", "until your next upkeep" and "at beginning of upkeep" commands from cards
/**
 * <p>
 * Upkeep class.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 */
public class Upkeep implements java.io.Serializable {
    private static final long serialVersionUID = 6906459482978819354L;

    private final HashMap<Player, CommandList> until = new HashMap<Player, CommandList>();

    /**
     * <p>
     * addUntil.
     * </p>
     * 
     * @param p
     *            a {@link forge.Player} object
     * @param c
     *            a {@link forge.Command} object.
     */
    public final void addUntil(Player p, final Command c) {
        if (null == p) {
            p = AllZone.getPhase().getPlayerTurn();
        }

        if (this.until.containsKey(p)) {
            this.until.get(p).add(c);
        } else {
            this.until.put(p, new CommandList(c));
        }
    }

    /**
     * <p>
     * executeUntil.
     * </p>
     * 
     * @param p
     *            the player the execute until for
     */
    public final void executeUntil(final Player p) {
        if (this.until.containsKey(p)) {
            this.execute(this.until.get(p));
        }
    }

    /**
     * <p>
     * sizeUntil.
     * </p>
     * 
     * @return a int.
     */
    public final int sizeUntil() {
        return this.until.size();
    }

    private void execute(final CommandList c) {
        final int length = c.size();

        for (int i = 0; i < length; i++) {
            c.remove(0).execute();
        }
    }

    /**
     * <p>
     * executeAt.
     * </p>
     */
    public final void executeAt() {
        AllZone.getStack().freezeStack();
        Upkeep.upkeepBraidOfFire();

        Upkeep.upkeepSlowtrips(); // for
        // "Draw a card at the beginning of the next turn's upkeep."
        Upkeep.upkeepUpkeepCost(); // sacrifice unless upkeep cost is paid
        Upkeep.upkeepEcho();

        Upkeep.upkeepTheAbyss();
        Upkeep.upkeepYawgmothDemon();
        Upkeep.upkeepLordOfThePit();
        Upkeep.upkeepDropOfHoney();
        Upkeep.upkeepDemonicHordes();
        Upkeep.upkeepCarnophage();
        Upkeep.upkeepSangrophage();
        Upkeep.upkeepDegaSanctuary();
        Upkeep.upkeepCetaSanctuary();
        Upkeep.upkeepTangleWire();

        Upkeep.upkeepVesuvanDoppelgangerKeyword();

        // Kinship cards
        Upkeep.upkeepInkDissolver();
        Upkeep.upkeepKithkinZephyrnaut();
        Upkeep.upkeepLeafCrownedElder();
        Upkeep.upkeepMudbuttonClanger();
        Upkeep.upkeepNightshadeSchemers();
        Upkeep.upkeepPyroclastConsul();
        Upkeep.upkeepSensationGorger();
        Upkeep.upkeepSqueakingPieGrubfellows();
        Upkeep.upkeepWanderingGraybeard();
        Upkeep.upkeepWaterspoutWeavers();
        Upkeep.upkeepWinnowerPatrol();
        Upkeep.upkeepWolfSkullShaman();

        // upkeep_Dragon_Broodmother(); //put this before bitterblossom and
        // mycoloth, so that they will resolve FIRST

        Upkeep.upkeepKarma();
        Upkeep.upkeepOathOfDruids();
        Upkeep.upkeepOathOfGhouls();
        Upkeep.upkeepSuspend();
        Upkeep.upkeepVanishing();
        Upkeep.upkeepFading();
        Upkeep.upkeepMasticore();
        Upkeep.upkeepEldraziMonument();
        Upkeep.upkeepBlazeCounters();
        // upkeep_Dark_Confidant(); // keep this one semi-last
        Upkeep.upkeepPowerSurge();
        Upkeep.upkeepAlurenAI();
        // experimental, AI abuse aluren

        AllZone.getStack().unfreezeStack();
    }

    // UPKEEP CARDS:

    /**
     * <p>
     * upkeep_Braid_Of_Fire.
     * </p>
     */
    private static void upkeepBraidOfFire() {
        final Player player = AllZone.getPhase().getPlayerTurn();

        final CardList braids = player.getCardsIn(Zone.Battlefield, "Braid of Fire");

        for (int i = 0; i < braids.size(); i++) {
            final Card c = braids.get(i);

            final StringBuilder sb = new StringBuilder();
            sb.append("Cumulative Upkeep for ").append(c).append("\n");
            final Ability upkeepAbility = new Ability(c, "0") {
                @Override
                public void resolve() {
                    c.addCounter(Counters.AGE, 1);
                    final int ageCounters = c.getCounters(Counters.AGE);
                    final AbilityMana abMana = new AbilityMana(c, "0", "R", ageCounters) {
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
    } // upkeep_Braid_of_Fire

    /**
     * <p>
     * upkeep_Echo.
     * </p>
     */
    private static void upkeepEcho() {
        CardList list = AllZone.getPhase().getPlayerTurn().getCardsIn(Zone.Battlefield);
        list = list.filter(new CardListFilter() {
            @Override
            public boolean addCard(final Card c) {
                return c.hasKeyword("(Echo unpaid)");
            }
        });

        for (int i = 0; i < list.size(); i++) {
            final Card c = list.get(i);
            if (c.getIntrinsicKeyword().contains("(Echo unpaid)")) {

                final Command paidCommand = Command.BLANK;

                final Command unpaidCommand = new Command() {
                    private static final long serialVersionUID = -7354791599039157375L;

                    @Override
                    public void execute() {
                        AllZone.getGameAction().sacrifice(c);
                    }
                };

                final Ability aiPaid = Upkeep.upkeepAIPayment(c, c.getEchoCost());

                final StringBuilder sb = new StringBuilder();
                sb.append("Echo for ").append(c).append("\n");

                final Ability sacAbility = new Ability(c, "0") {
                    @Override
                    public void resolve() {
                        if (c.getController().isHuman()) {
                            GameActionUtil.payManaDuringAbilityResolve(sb.toString(), c.getEchoCost(), paidCommand,
                                    unpaidCommand);
                        } else { // computer
                            if (ComputerUtil.canPayCost(aiPaid)) {
                                ComputerUtil.playNoStack(aiPaid);
                            } else {
                                AllZone.getGameAction().sacrifice(c);
                            }
                        }
                    }
                };
                sacAbility.setStackDescription(sb.toString());

                AllZone.getStack().addSimultaneousStackEntry(sacAbility);

                c.removeIntrinsicKeyword("(Echo unpaid)");
            }
        }
    } // echo

    /**
     * <p>
     * upkeep_Slowtrips. Draw a card at the beginning of the next turn's upkeep.
     * </p>
     */
    private static void upkeepSlowtrips() {
        final Player player = AllZone.getPhase().getPlayerTurn();

        CardList list = player.getSlowtripList();

        for (int i = 0; i < list.size(); i++) {
            final Card card = list.get(i);

            // otherwise another slowtrip gets added
            card.removeIntrinsicKeyword("Draw a card at the beginning of the next turn's upkeep.");

            final Ability slowtrip = new Ability(card, "0") {
                @Override
                public void resolve() {
                    player.drawCard();
                }
            };
            slowtrip.setStackDescription(card + " - Draw a card.");

            AllZone.getStack().addSimultaneousStackEntry(slowtrip);

        }
        player.clearSlowtripList();

        // Do the same for the opponent
        final Player opponent = player.getOpponent();

        list = opponent.getSlowtripList();

        for (int i = 0; i < list.size(); i++) {
            final Card card = list.get(i);

            // otherwise another slowtrip gets added
            card.removeIntrinsicKeyword("Draw a card at the beginning of the next turn's upkeep.");

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
     * <p>
     * upkeep_UpkeepCost.
     * </p>
     */
    private static void upkeepUpkeepCost() {
        final CardList list = AllZone.getPhase().getPlayerTurn().getCardsIn(Zone.Battlefield);

        for (int i = 0; i < list.size(); i++) {
            final Card c = list.get(i);
            final Player controller = c.getController();
            final ArrayList<String> a = c.getKeyword();
            for (int j = 0; j < a.size(); j++) {
                final String ability = a.get(j);

                // destroy
                if (ability.startsWith("At the beginning of your upkeep, destroy CARDNAME")) {
                    final String[] k = ability.split(" pay ");
                    final String upkeepCost = k[1].toString();

                    final Command unpaidCommand = new Command() {
                        private static final long serialVersionUID = 8942537892273123542L;

                        @Override
                        public void execute() {
                            if (c.getName().equals("Cosmic Horror")) {
                                controller.addDamage(7, c);
                            }
                            AllZone.getGameAction().destroy(c);
                        }
                    };

                    final Command paidCommand = Command.BLANK;

                    final Ability aiPaid = Upkeep.upkeepAIPayment(c, upkeepCost);

                    final StringBuilder sb = new StringBuilder();
                    sb.append("Upkeep for ").append(c).append("\n");
                    final Ability upkeepAbility = new Ability(c, "0") {
                        @Override
                        public void resolve() {
                            if (controller.isHuman()) {
                                GameActionUtil.payManaDuringAbilityResolve(sb.toString(), upkeepCost, paidCommand,
                                        unpaidCommand);
                            } else { // computer
                                if (ComputerUtil.canPayCost(aiPaid) && !c.hasKeyword("Indestructible")) {
                                    ComputerUtil.playNoStack(aiPaid);
                                } else {
                                    AllZone.getGameAction().destroy(c);
                                }
                            }
                        }
                    };
                    upkeepAbility.setStackDescription(sb.toString());
                    upkeepAbility.setDescription(sb.toString());

                    AllZone.getStack().addSimultaneousStackEntry(upkeepAbility);
                } // destroy

                // sacrifice
                if (ability.startsWith("At the beginning of your upkeep, sacrifice")
                        || ability.startsWith("Cumulative upkeep")) {
                    String cost = "0";
                    final StringBuilder sb = new StringBuilder();

                    if (ability.startsWith("At the beginning of your upkeep, sacrifice")) {
                        final String[] k = ability.split(" pay ");
                        cost = k[1].toString();
                        sb.append("Sacrifice upkeep for ").append(c).append("\n");
                    }

                    if (ability.startsWith("Cumulative upkeep")) {
                        final String[] k = ability.split(":");
                        c.addCounter(Counters.AGE, 1);
                        cost = CardFactoryUtil.multiplyManaCost(k[1], c.getCounters(Counters.AGE));
                        sb.append("Cumulative upkeep for ").append(c).append("\n");
                    }

                    final String upkeepCost = cost;

                    final Command unpaidCommand = new Command() {
                        private static final long serialVersionUID = 5612348769167529102L;

                        @Override
                        public void execute() {
                            AllZone.getGameAction().sacrifice(c);
                        }
                    };

                    final Command paidCommand = Command.BLANK;

                    final Ability aiPaid = Upkeep.upkeepAIPayment(c, upkeepCost);

                    final Ability upkeepAbility = new Ability(c, "0") {
                        @Override
                        public void resolve() {
                            if (controller.isHuman()) {
                                GameActionUtil.payManaDuringAbilityResolve(sb.toString(), upkeepCost, paidCommand,
                                        unpaidCommand);
                            } else { // computer
                                if (ComputerUtil.canPayCost(aiPaid)) {
                                    ComputerUtil.playNoStack(aiPaid);
                                } else {
                                    AllZone.getGameAction().sacrifice(c);
                                }
                            }
                        }
                    };
                    upkeepAbility.setStackDescription(sb.toString());
                    upkeepAbility.setDescription(sb.toString());

                    AllZone.getStack().addSimultaneousStackEntry(upkeepAbility);
                } // sacrifice

                // destroy
                if (ability.startsWith("At the beginning of your upkeep, CARDNAME deals ")) {
                    final String[] k = ability.split("deals ");
                    final String s1 = k[1].substring(0, 2);
                    final int upkeepDamage = Integer.parseInt(s1.trim());
                    final String[] l = k[1].split(" pay ");
                    final String upkeepCost = l[1].toString();

                    final Command unpaidCommand = new Command() {
                        private static final long serialVersionUID = 1238166187561501928L;

                        @Override
                        public void execute() {
                            controller.addDamage(upkeepDamage, c);
                        }
                    };

                    final Command paidCommand = Command.BLANK;

                    final Ability aiPaid = Upkeep.upkeepAIPayment(c, upkeepCost);

                    final StringBuilder sb = new StringBuilder();
                    sb.append("Damage upkeep for ").append(c).append("\n");
                    final Ability upkeepAbility = new Ability(c, "0") {
                        @Override
                        public void resolve() {
                            if (controller.isHuman()) {
                                GameActionUtil.payManaDuringAbilityResolve(sb.toString(), upkeepCost, paidCommand,
                                        unpaidCommand);
                            } else { // computer
                                if (ComputerUtil.canPayCost(aiPaid)
                                        && (controller.predictDamage(upkeepDamage, c, false) > 0)) {
                                    ComputerUtil.playNoStack(aiPaid);
                                } else {
                                    controller.addDamage(upkeepDamage, c);
                                }
                            }
                        }
                    };
                    upkeepAbility.setStackDescription(sb.toString());
                    upkeepAbility.setDescription(sb.toString());

                    AllZone.getStack().addSimultaneousStackEntry(upkeepAbility);
                } // destroy
            }

        } // for
    } // upkeepCost

    /**
     * <p>
     * upkeepAIPayment.
     * </p>
     * 
     * @param c
     *            a {@link forge.Card} object.
     * @param cost
     *            a {@link java.lang.String} object.
     * @param cost
     *            a {@link java.lang.String} object.
     * @return a {@link forge.card.spellability.Ability} object.
     */
    private static Ability upkeepAIPayment(final Card c, final String cost) {
        return new AbilityStatic(c, cost) {
            @Override
            public void resolve() {

            }
        };
    }

    /**
     * <p>
     * upkeep_The_Abyss.
     * </p>
     */
    private static void upkeepTheAbyss() {
        /*
         * At the beginning of each player's upkeep, destroy target nonartifact
         * creature that player controls of his or her choice. It can't be
         * regenerated.
         */
        final Player player = AllZone.getPhase().getPlayerTurn();
        final CardList the = AllZoneUtil.getCardsIn(Zone.Battlefield, "The Abyss");
        final CardList magus = AllZoneUtil.getCardsIn(Zone.Battlefield, "Magus of the Abyss");

        final CardList cards = new CardList();
        cards.addAll(the);
        cards.addAll(magus);

        for (final Card c : cards) {
            final Card abyss = c;
            
            final CardList abyssGetTargets = AllZoneUtil.getCreaturesInPlay(player).filter(CardListFilter.NON_ARTIFACTS);

            final Ability sacrificeCreature = new Ability(abyss, "") {
                @Override
                public void resolve() {
                    final CardList targets = abyssGetTargets.getTargetableCards(this);
                    if (player.isHuman()) {
                        if (targets.size() > 0) {
                            AllZone.getInputControl().setInput(new Input() {
                                private static final long serialVersionUID = 4820011040853968644L;

                                @Override
                                public void showMessage() {
                                    AllZone.getDisplay().showMessage(
                                            abyss.getName() + " - Select one nonartifact creature to destroy");
                                    ButtonUtil.disableAll();
                                }

                                @Override
                                public void selectCard(final Card selected, final PlayerZone zone) {
                                    // probably need to restrict by controller
                                    // also
                                    if (targets.contains(selected)) {
                                        AllZone.getGameAction().destroyNoRegeneration(selected);
                                        this.stop();
                                    }
                                } // selectCard()
                            }); // Input
                        }
                    } else { // computer
                        
                        final CardList indestruct = targets.getKeyword("Indestructible");
                        if (indestruct.size() > 0) {
                            AllZone.getGameAction().destroyNoRegeneration(indestruct.get(0));
                        } else if (targets.size() > 0){
                            final Card target = CardFactoryUtil.getWorstCreatureAI(targets);
                            if (null == target) {
                                // must be nothing valid to destroy
                            } else {
                                AllZone.getGameAction().destroyNoRegeneration(target);
                            }
                        }
                    }
                } // resolve
            }; // sacrificeCreature

            final StringBuilder sb = new StringBuilder();
            sb.append(abyss.getName()).append(" - destroy a nonartifact creature of your choice.");
            sacrificeCreature.setStackDescription(sb.toString());
            AllZone.getStack().addSimultaneousStackEntry(sacrificeCreature);

        } // end for
    } // The Abyss

    /**
     * <p>
     * upkeep_Yawgmoth_Demon.
     * </p>
     */
    private static void upkeepYawgmothDemon() {
        /*
         * At the beginning of your upkeep, you may sacrifice an artifact. If
         * you don't, tap Yawgmoth Demon and it deals 2 damage to you.
         */
        final Player player = AllZone.getPhase().getPlayerTurn();
        final CardList cards = player.getCardsIn(Zone.Battlefield, "Yawgmoth Demon");

        for (int i = 0; i < cards.size(); i++) {
            final Card c = cards.get(i);

            final Ability sacrificeArtifact = new Ability(c, "") {
                @Override
                public void resolve() {
                    final CardList artifacts = player.getCardsIn(Zone.Battlefield).filter(CardListFilter.ARTIFACTS);

                    if (player.isHuman()) {
                        AllZone.getInputControl().setInput(new Input() {
                            private static final long serialVersionUID = -1698502376924356936L;

                            @Override
                            public void showMessage() {
                                AllZone.getDisplay().showMessage(
                                        "Yawgmoth Demon - Select one artifact to sacrifice or be dealt 2 damage");
                                ButtonUtil.enableOnlyCancel();
                            }

                            @Override
                            public void selectButtonCancel() {
                                tapAndDamage(player);
                                this.stop();
                            }

                            @Override
                            public void selectCard(final Card artifact, final PlayerZone zone) {
                                // probably need to restrict by controller also
                                if (artifact.isArtifact() && zone.is(Constant.Zone.Battlefield)
                                        && zone.getPlayer().isHuman()) {
                                    AllZone.getGameAction().sacrifice(artifact);
                                    this.stop();
                                }
                            } // selectCard()
                        }); // Input
                    } else { // computer
                        final Card target = CardFactoryUtil.getCheapestPermanentAI(artifacts, this, false);
                        if (null == target) {
                            this.tapAndDamage(player);
                        } else {
                            AllZone.getGameAction().sacrifice(target);
                        }
                    }
                } // resolve

                private void tapAndDamage(final Player player) {
                    c.tap();
                    player.addDamage(2, c);
                }
            };

            final StringBuilder sb = new StringBuilder();
            sb.append(c.getName()).append(" - sacrifice an artifact or ");
            sb.append(c.getName()).append(" becomes tapped and deals 2 damage to you.");
            sacrificeArtifact.setStackDescription(sb.toString());

            AllZone.getStack().addSimultaneousStackEntry(sacrificeArtifact);

        } // end for
    }

    /**
     * <p>
     * upkeep_Lord_of_the_Pit.
     * </p>
     */
    private static void upkeepLordOfThePit() {
        /*
         * At the beginning of your upkeep, sacrifice a creature other than Lord
         * of the Pit. If you can't, Lord of the Pit deals 7 damage to you.
         */
        final Player player = AllZone.getPhase().getPlayerTurn();
        final CardList lords = player.getCardsIn(Zone.Battlefield, "Lord of the Pit");
        lords.addAll(player.getCardsIn(Zone.Battlefield, "Liege of the Pit"));
        final CardList cards = lords;

        for (int i = 0; i < cards.size(); i++) {
            final Card c = cards.get(i);
            if (c.isFaceDown()) {
                continue;
            }

            final Ability sacrificeCreature = new Ability(c, "") {
                @Override
                public void resolve() {
                    // TODO - this should handle the case where you sacrifice 2
                    // LOTPs to each other
                    final CardList creatures = AllZoneUtil.getCreaturesInPlay(player);
                    creatures.remove(c);
                    if (player.isHuman()) {
                        AllZone.getInputControl().setInput(
                                PlayerUtil.inputSacrificePermanent(creatures, c.getName()
                                        + " - Select a creature to sacrifice."));
                    } else { // computer
                        final Card target = CardFactoryUtil.getWorstCreatureAI(creatures);
                        AllZone.getGameAction().sacrifice(target);
                    }
                } // resolve
            };

            final Ability sevenDamage = new Ability(c, "") {
                @Override
                public void resolve() {
                    player.addDamage(7, c);
                }
            };

            final CardList creatures = AllZoneUtil.getCreaturesInPlay(player);
            creatures.remove(c);
            if (creatures.size() == 0) {
                // there are no creatures to sacrifice, so we must do the 7
                // damage

                final StringBuilder sb = new StringBuilder();
                sb.append(c.getName()).append(" - deals 7 damage to controller");
                sevenDamage.setStackDescription(sb.toString());

                AllZone.getStack().addSimultaneousStackEntry(sevenDamage);

            } else {

                final StringBuilder sb = new StringBuilder();
                sb.append(c.getName()).append(" - sacrifice a creature.");
                sacrificeCreature.setStackDescription(sb.toString());

                AllZone.getStack().addSimultaneousStackEntry(sacrificeCreature);

            }
        } // end for
    } // upkeep_Lord_of_the_Pit()

    /**
     * <p>
     * upkeep_Drop_of_Honey.
     * </p>
     */
    private static void upkeepDropOfHoney() {
        /*
         * At the beginning of your upkeep, destroy the creature with the least
         * power. It can't be regenerated. If two or more creatures are tied for
         * least power, you choose one of them.
         */
        final Player player = AllZone.getPhase().getPlayerTurn();
        final CardList drops = player.getCardsIn(Zone.Battlefield, "Drop of Honey");
        drops.addAll(player.getCardsIn(Zone.Battlefield, "Porphyry Nodes"));
        final CardList cards = drops;

        for (int i = 0; i < cards.size(); i++) {
            final Card c = cards.get(i);

            final Ability ability = new Ability(c, "") {
                @Override
                public void resolve() {
                    final CardList creatures = AllZoneUtil.getCreaturesInPlay();
                    if (creatures.size() > 0) {
                        CardListUtil.sortAttackLowFirst(creatures);
                        final int power = creatures.get(0).getNetAttack();
                        if (player.isHuman()) {
                            AllZone.getInputControl().setInput(
                                    CardFactoryUtil.inputDestroyNoRegeneration(this.getLowestPowerList(creatures),
                                            "Select creature with power: " + power + " to sacrifice."));
                        } else { // computer
                            final Card compyTarget = this.getCompyCardToDestroy(creatures);
                            AllZone.getGameAction().destroyNoRegeneration(compyTarget);
                        }
                    }
                } // resolve

                private CardList getLowestPowerList(final CardList original) {
                    final CardList lowestPower = new CardList();
                    final int power = original.get(0).getNetAttack();
                    int i = 0;
                    while ((i < original.size()) && (original.get(i).getNetAttack() == power)) {
                        lowestPower.add(original.get(i));
                        i++;
                    }
                    return lowestPower;
                }

                private Card getCompyCardToDestroy(final CardList original) {
                    final CardList options = this.getLowestPowerList(original);
                    final CardList humanCreatures = options.filter(new CardListFilter() {
                        @Override
                        public boolean addCard(final Card c) {
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
            }; // Ability

            final StringBuilder sb = new StringBuilder();
            sb.append(c.getName()).append(" - destroy 1 creature with lowest power.");
            ability.setStackDescription(sb.toString());

            AllZone.getStack().addSimultaneousStackEntry(ability);

        } // end for
    } // upkeep_Drop_of_Honey()

    /**
     * <p>
     * upkeep_Demonic_Hordes.
     * </p>
     */
    private static void upkeepDemonicHordes() {

        /*
         * At the beginning of your upkeep, unless you pay BBB, tap Demonic
         * Hordes and sacrifice a land of an opponent's choice.
         */

        final Player player = AllZone.getPhase().getPlayerTurn();
        final CardList cards = player.getCardsIn(Zone.Battlefield, "Demonic Hordes");

        for (int i = 0; i < cards.size(); i++) {

            final Card c = cards.get(i);

            final Ability noPay = new Ability(c, "B B B") {
                @Override
                public void resolve() {
                    final CardList playerLand = AllZoneUtil.getPlayerLandsInPlay(player);

                    c.tap();
                    if (c.getController().isComputer()) {
                        if (playerLand.size() > 0) {
                            AllZone.getInputControl().setInput(
                                    PlayerUtil.inputSacrificePermanent(playerLand, c.getName()
                                            + " - Select a land to sacrifice."));
                        }
                    } else {
                        final Card target = CardFactoryUtil.getBestLandAI(playerLand);

                        AllZone.getGameAction().sacrifice(target);
                    }
                } // end resolve()
            }; // end noPay ability

            if (c.getController().isHuman()) {
                final String question = "Pay Demonic Hordes upkeep cost?";
                if (GameActionUtil.showYesNoDialog(c, question)) {
                    final Ability pay = new Ability(c, "0") {
                        @Override
                        public void resolve() {
                            if (AllZone.getZoneOf(c).is(Constant.Zone.Battlefield)) {
                                final StringBuilder cost = new StringBuilder();
                                cost.append("Pay cost for ").append(c).append("\r\n");
                                GameActionUtil.payManaDuringAbilityResolve(cost.toString(), noPay.getManaCost(),
                                        Command.BLANK, Command.BLANK);
                            }
                        } // end resolve()
                    }; // end pay ability
                    pay.setStackDescription("Demonic Hordes - Upkeep Cost");

                    AllZone.getStack().addSimultaneousStackEntry(pay);

                } // end choice
                else {
                    final StringBuilder sb = new StringBuilder();
                    sb.append(c.getName()).append(" - is tapped and you must sacrifice a land of opponent's choice");
                    noPay.setStackDescription(sb.toString());

                    AllZone.getStack().addSimultaneousStackEntry(noPay);

                }
            } // end human
            else { // computer
                if ((c.getController().isComputer() && (ComputerUtil.canPayCost(noPay)))) {
                    final Ability computerPay = new Ability(c, "0") {
                        @Override
                        public void resolve() {
                            ComputerUtil.payManaCost(noPay);
                        }
                    };
                    computerPay.setStackDescription("Computer pays Demonic Hordes upkeep cost");

                    AllZone.getStack().addSimultaneousStackEntry(computerPay);

                } else {
                    AllZone.getStack().addSimultaneousStackEntry(noPay);

                }
            } // end computer

        } // end for loop

    } // upkeep_Demonic_Hordes

    /**
     * <p>
     * upkeep_AI_Aluren.
     * </p>
     */
    private static void upkeepAlurenAI() {
        final CardList alurens = AllZoneUtil.getCardsIn(Zone.Battlefield, "Aluren");
        if (alurens.size() == 0) {
            return;
        }

        CardList inHand = AllZone.getComputerPlayer().getCardsIn(Zone.Hand);
        inHand = inHand.getType("Creature");
        final CardList playable = new CardList();

        for (final Card c : inHand) {
            if (CardUtil.getConvertedManaCost(c.getManaCost()) <= 3) {
                playable.add(c);
            }
        }

        for (final Card c : playable) {
            AllZone.getGameAction().playSpellAbilityForFree(c.getSpellPermanent());
        }
    }

    // ///////////////////////
    // Start of Kinship cards
    // ///////////////////////

    /**
     * <p>
     * upkeep_Ink_Dissolver.
     * </p>
     */
    private static void upkeepInkDissolver() {
        final Player player = AllZone.getPhase().getPlayerTurn();
        final Player opponent = player.getOpponent();
        final CardList kinship = player.getCardsIn(Zone.Battlefield, "Ink Dissolver");

        final PlayerZone library = player.getZone(Constant.Zone.Library);
        // Players would not choose to trigger Kinship ability if library is
        // empty.
        // Useful for games when the "Milling = Loss Condition" check box is
        // unchecked.

        if ((kinship.size() == 0) || (library.size() <= 0)) {
            return;
        }

        final String[] shareTypes = { "Merfolk", "Wizard" };
        final Card[] prevCardShown = { null };
        final Card[] peek = { null };

        for (final Card k : kinship) {
            final Ability ability = new Ability(k, "0") { // change to triggered
                // abilities when ready
                @Override
                public void resolve() {
                    final PlayerZone library = player.getZone(Constant.Zone.Library);
                    if (library.size() <= 0) {
                        return;
                    }

                    peek[0] = library.get(0);
                    boolean wantToMillOpponent = false;

                    // We assume that both players will want to peek, ask if
                    // they want to reveal.
                    // We do not want to slow down the pace of the game by
                    // asking too many questions.
                    // Dialogs outside of the Ability appear at the previous end
                    // of turn phase !!!

                    if (peek[0].isValid(shareTypes, k.getController(), k)) {
                        if (player.isHuman()) {
                            final StringBuilder question = new StringBuilder();
                            question.append("Your top card is ").append(peek[0].getName());
                            question.append(". Reveal card and opponent puts the top 3 ");
                            question.append("cards of his library into his graveyard?");
                            if (GameActionUtil.showYesNoDialog(k, question.toString())) {
                                wantToMillOpponent = true;
                            }
                        }
                        // player isComputer()
                        else {
                            final String title = "Computer reveals";
                            this.revealTopCard(title);
                            wantToMillOpponent = true;
                        }
                    } else if (player.isHuman()) {
                        final String title = "Your top card is";
                        this.revealTopCard(title);
                    }

                    if (wantToMillOpponent) {
                        opponent.mill(3);
                    }
                } // resolve()

                private void revealTopCard(final String title) {
                    if (peek[0] != prevCardShown[0]) {
                        GuiUtils.getChoice(title, peek[0]);
                        prevCardShown[0] = peek[0];
                    }
                } // revealTopCard()
            }; // ability

            final StringBuilder sb = new StringBuilder();
            sb.append("Ink Dissolver - ").append(player);
            sb.append(" triggers Kinship");
            ability.setStackDescription(sb.toString());

            AllZone.getStack().addSimultaneousStackEntry(ability);

        } // for
    } // upkeep_Ink_Dissolver()

    /**
     * <p>
     * upkeep_Kithkin_Zephyrnaut.
     * </p>
     */
    private static void upkeepKithkinZephyrnaut() {
        final Player player = AllZone.getPhase().getPlayerTurn();
        final CardList kinship = player.getCardsIn(Zone.Battlefield, "Kithkin Zephyrnaut");

        final PlayerZone library = player.getZone(Constant.Zone.Library);
        // Players would not choose to trigger Kinship ability if library is
        // empty.
        // Useful for games when the "Milling = Loss Condition" check box is
        // unchecked.

        if ((kinship.size() == 0) || (library.size() <= 0)) {
            return;
        }

        final String[] shareTypes = { "Kithkin", "Soldier" };
        final Card[] prevCardShown = { null };
        final Card[] peek = { null };

        for (final Card k : kinship) {
            final Ability ability = new Ability(k, "0") { // change to triggered
                // abilities when ready
                @Override
                public void resolve() {
                    final PlayerZone library = player.getZone(Constant.Zone.Library);
                    if (library.size() <= 0) {
                        return;
                    }

                    peek[0] = library.get(0);
                    boolean wantKithkinBuff = false;

                    // We assume that both players will want to peek, ask if
                    // they want to reveal.
                    // We do not want to slow down the pace of the game by
                    // asking too many questions.
                    // Dialogs outside of the Ability appear at the previous end
                    // of turn phase !!!

                    if (peek[0].isValid(shareTypes, k.getController(), k)) {
                        if (player.isHuman()) {
                            final StringBuilder question = new StringBuilder();
                            question.append("Your top card is ").append(peek[0].getName());
                            question.append(". Reveal card, Kithkin Zephyrnaut gets +2/+2 and ");
                            question.append("gains flying and vigilance until end of turn?");
                            if (GameActionUtil.showYesNoDialog(k, question.toString())) {
                                wantKithkinBuff = true;
                            }
                        }
                        // player isComputer()
                        else {
                            final String title = "Computer reveals";
                            this.revealTopCard(title);
                            wantKithkinBuff = true;
                        }
                    } else if (player.isHuman()) {
                        final String title = "Your top card is";
                        this.revealTopCard(title);
                    }

                    if (wantKithkinBuff) {
                        k.addTempAttackBoost(2);
                        k.addTempDefenseBoost(2);
                        k.addExtrinsicKeyword("Flying");
                        k.addExtrinsicKeyword("Vigilance");

                        final Command untilEOT = new Command() {
                            private static final long serialVersionUID = 213717084767008154L;

                            @Override
                            public void execute() {
                                k.addTempAttackBoost(-2);
                                k.addTempDefenseBoost(-2);
                                k.removeExtrinsicKeyword("Flying");
                                k.removeExtrinsicKeyword("Vigilance");
                            }
                        };
                        AllZone.getEndOfTurn().addUntil(untilEOT);
                    }
                } // resolve()

                private void revealTopCard(final String title) {
                    if (peek[0] != prevCardShown[0]) {
                        GuiUtils.getChoice(title, peek[0]);
                        prevCardShown[0] = peek[0];
                    }
                } // revealTopCard()
            }; // ability

            final StringBuilder sb = new StringBuilder();
            sb.append("Kithkin Zephyrnaut - ").append(player);
            sb.append(" triggers Kinship");
            ability.setStackDescription(sb.toString());

            AllZone.getStack().addSimultaneousStackEntry(ability);

        } // for
    } // upkeep_Kithkin_Zephyrnaut()

    /**
     * <p>
     * upkeep_Leaf_Crowned_Elder.
     * </p>
     */
    private static void upkeepLeafCrownedElder() {
        final Player player = AllZone.getPhase().getPlayerTurn();
        final CardList kinship = player.getCardsIn(Zone.Battlefield, "Leaf-Crowned Elder");

        final PlayerZone library = player.getZone(Constant.Zone.Library);
        // Players would not choose to trigger Kinship ability if library is
        // empty.
        // Useful for games when the "Milling = Loss Condition" check box is
        // unchecked.

        if ((kinship.size() == 0) || (library.size() <= 0)) {
            return;
        }

        final String[] shareTypes = { "Treefolk", "Shaman" };
        final Card[] prevCardShown = { null };
        final Card[] peek = { null };

        for (final Card k : kinship) {
            final Ability ability = new Ability(k, "0") { // change to triggered
                // abilities when ready
                @Override
                public void resolve() {
                    final PlayerZone library = player.getZone(Constant.Zone.Library);
                    if (library.size() <= 0) {
                        return;
                    }

                    peek[0] = library.get(0);
                    boolean wantToPlayCard = false;

                    // We assume that both players will want to peek, ask if
                    // they want to reveal.
                    // We do not want to slow down the pace of the game by
                    // asking too many questions.
                    // Dialogs outside of the Ability appear at the previous end
                    // of turn phase !!!

                    if (peek[0].isValid(shareTypes, k.getController(), k)) {
                        if (player.isHuman()) {
                            final StringBuilder question = new StringBuilder();
                            question.append("Your top card is ").append(peek[0].getName());
                            question.append(". Reveal and play this card without paying its mana cost?");
                            if (GameActionUtil.showYesNoDialog(k, question.toString())) {
                                wantToPlayCard = true;
                            }
                        }
                        // player isComputer()
                        else {
                            final String title = "Computer reveals";
                            this.revealTopCard(title);
                            wantToPlayCard = true;
                        }
                    } else if (player.isHuman()) {
                        final String title = "Your top card is";
                        this.revealTopCard(title);
                    }

                    if (wantToPlayCard) {
                        if (player.isHuman()) {
                            final Card c = library.get(0);
                            AllZone.getGameAction().playCardNoCost(c);
                        }
                        // player isComputer()
                        else {
                            final Card c = library.get(0);
                            final ArrayList<SpellAbility> choices = c.getBasicSpells();

                            for (final SpellAbility sa : choices) {
                                if (sa.canPlayAI()) {
                                    ComputerUtil.playStackFree(sa);
                                    break;
                                }
                            }
                        }
                    } // wantToPlayCard
                } // resolve()

                private void revealTopCard(final String title) {
                    if (peek[0] != prevCardShown[0]) {
                        GuiUtils.getChoice(title, peek[0]);
                        prevCardShown[0] = peek[0];
                    }
                } // revealTopCard()
            }; // ability

            final StringBuilder sb = new StringBuilder();
            sb.append("Leaf-Crowned Elder - ").append(player);
            sb.append(" triggers Kinship");
            ability.setStackDescription(sb.toString());

            AllZone.getStack().addSimultaneousStackEntry(ability);

        } // for
    } // upkeep_Leaf_Crowned_Elder()

    /**
     * <p>
     * upkeep_Mudbutton_Clanger.
     * </p>
     */
    private static void upkeepMudbuttonClanger() {
        final Player player = AllZone.getPhase().getPlayerTurn();
        final CardList kinship = player.getCardsIn(Zone.Battlefield, "Mudbutton Clanger");

        final PlayerZone library = player.getZone(Constant.Zone.Library);
        // Players would not choose to trigger Kinship ability if library is
        // empty.
        // Useful for games when the "Milling = Loss Condition" check box is
        // unchecked.

        if ((kinship.size() == 0) || (library.size() <= 0)) {
            return;
        }

        final String[] shareTypes = { "Goblin", "Warrior" };
        final Card[] prevCardShown = { null };
        final Card[] peek = { null };

        for (final Card k : kinship) {
            final Ability ability = new Ability(k, "0") { // change to triggered
                // abilities when ready
                @Override
                public void resolve() {
                    final PlayerZone library = player.getZone(Constant.Zone.Library);
                    if (library.size() <= 0) {
                        return;
                    }

                    peek[0] = library.get(0);
                    boolean wantGoblinBuff = false;

                    // We assume that both players will want to peek, ask if
                    // they want to reveal.
                    // We do not want to slow down the pace of the game by
                    // asking too many questions.
                    // Dialogs outside of the Ability appear at the previous end
                    // of turn phase !!!

                    if (peek[0].isValid(shareTypes, k.getController(), k)) {
                        if (player.isHuman()) {
                            final StringBuilder question = new StringBuilder();
                            question.append("Your top card is ").append(peek[0].getName());
                            question.append(". Reveal card and Mudbutton Clanger gets +1/+1 until end of turn?");
                            if (GameActionUtil.showYesNoDialog(k, question.toString())) {
                                wantGoblinBuff = true;
                            }
                        }
                        // player isComputer()
                        else {
                            final String title = "Computer reveals";
                            this.revealTopCard(title);
                            wantGoblinBuff = true;
                        }
                    } else if (player.isHuman()) {
                        final String title = "Your top card is";
                        this.revealTopCard(title);
                    }

                    if (wantGoblinBuff) {
                        k.addTempAttackBoost(1);
                        k.addTempDefenseBoost(1);

                        final Command untilEOT = new Command() {
                            private static final long serialVersionUID = -103560515951630426L;

                            @Override
                            public void execute() {
                                k.addTempAttackBoost(-1);
                                k.addTempDefenseBoost(-1);
                            }
                        };
                        AllZone.getEndOfTurn().addUntil(untilEOT);
                    }
                } // resolve()

                private void revealTopCard(final String title) {
                    if (peek[0] != prevCardShown[0]) {
                        GuiUtils.getChoice(title, peek[0]);
                        prevCardShown[0] = peek[0];
                    }
                } // revealTopCard()
            }; // ability

            final StringBuilder sb = new StringBuilder();
            sb.append("Mudbutton Clanger - ").append(player);
            sb.append(" triggers Kinship");
            ability.setStackDescription(sb.toString());

            AllZone.getStack().addSimultaneousStackEntry(ability);

        } // for
    } // upkeep_Mudbutton_Clanger()

    /**
     * <p>
     * upkeep_Nightshade_Schemers.
     * </p>
     */
    private static void upkeepNightshadeSchemers() {
        final Player player = AllZone.getPhase().getPlayerTurn();
        final CardList kinship = player.getCardsIn(Zone.Battlefield, "Nightshade Schemers");
        final Player opponent = player.getOpponent();

        final PlayerZone library = player.getZone(Constant.Zone.Library);
        // Players would not choose to trigger Kinship ability if library is
        // empty.
        // Useful for games when the "Milling = Loss Condition" check box is
        // unchecked.

        if ((kinship.size() == 0) || (library.size() <= 0)) {
            return;
        }

        final String[] shareTypes = { "Faerie", "Wizard" };
        final Card[] prevCardShown = { null };
        final Card[] peek = { null };

        for (final Card k : kinship) {
            final Ability ability = new Ability(k, "0") { // change to triggered
                // abilities when ready
                @Override
                public void resolve() {
                    final PlayerZone library = player.getZone(Constant.Zone.Library);
                    if (library.size() <= 0) {
                        return;
                    }

                    peek[0] = library.get(0);
                    boolean wantOpponentLoseLife = false;

                    // We assume that both players will want to peek, ask if
                    // they want to reveal.
                    // We do not want to slow down the pace of the game by
                    // asking too many questions.
                    // Dialogs outside of the Ability appear at the previous end
                    // of turn phase !!!

                    if (peek[0].isValid(shareTypes, k.getController(), k)) {
                        if (player.isHuman()) {
                            final StringBuilder question = new StringBuilder();
                            question.append("Your top card is ").append(peek[0].getName());
                            question.append(". Reveal card and opponent loses 2 life?");
                            if (GameActionUtil.showYesNoDialog(k, question.toString())) {
                                wantOpponentLoseLife = true;
                            }
                        }
                        // player isComputer()
                        else {
                            final String title = "Computer reveals";
                            this.revealTopCard(title);
                            wantOpponentLoseLife = true;
                        }
                    } else if (player.isHuman()) {
                        final String title = "Your top card is";
                        this.revealTopCard(title);
                    }
                    if (wantOpponentLoseLife) {
                        opponent.loseLife(2, k);
                    }
                } // resolve()

                private void revealTopCard(final String title) {
                    if (peek[0] != prevCardShown[0]) {
                        GuiUtils.getChoice(title, peek[0]);
                        prevCardShown[0] = peek[0];
                    }
                } // revealTopCard()
            }; // ability

            final StringBuilder sb = new StringBuilder();
            sb.append("Nightshade Schemers - ").append(player);
            sb.append(" triggers Kinship");
            ability.setStackDescription(sb.toString());

            AllZone.getStack().addSimultaneousStackEntry(ability);

        } // for
    } // upkeep_Nightshade_Schemers()

    /**
     * <p>
     * upkeep_Pyroclast_Consul.
     * </p>
     */
    private static void upkeepPyroclastConsul() {
        final Player player = AllZone.getPhase().getPlayerTurn();
        final CardList kinship = player.getCardsIn(Zone.Battlefield, "Pyroclast Consul");

        final PlayerZone library = player.getZone(Constant.Zone.Library);
        // Players would not choose to trigger Kinship ability if library is
        // empty.
        // Useful for games when the "Milling = Loss Condition" check box is
        // unchecked.

        if ((kinship.size() == 0) || (library.size() <= 0)) {
            return;
        }

        final String[] shareTypes = { "Elemental", "Shaman" };
        final Card[] prevCardShown = { null };
        final Card[] peek = { null };

        for (final Card k : kinship) {
            final Ability ability = new Ability(k, "0") { // change to triggered
                // abilities when ready
                @Override
                public void resolve() {
                    final PlayerZone library = player.getZone(Constant.Zone.Library);
                    if (library.size() <= 0) {
                        return;
                    }

                    peek[0] = library.get(0);
                    boolean wantDamageCreatures = false;
                    final String[] smallCreatures = { "Creature.toughnessLE2" };

                    CardList humanCreatures = AllZoneUtil.getCreaturesInPlay(AllZone.getHumanPlayer());
                    humanCreatures = humanCreatures.getValidCards(smallCreatures, k.getController(), k);
                    humanCreatures = humanCreatures.getNotKeyword("Indestructible");

                    CardList computerCreatures = AllZoneUtil.getCreaturesInPlay(AllZone.getComputerPlayer());
                    computerCreatures = computerCreatures.getValidCards(smallCreatures, k.getController(), k);
                    computerCreatures = computerCreatures.getNotKeyword("Indestructible");

                    // We assume that both players will want to peek, ask if
                    // they want to reveal.
                    // We do not want to slow down the pace of the game by
                    // asking too many questions.
                    // Dialogs outside of the Ability appear at the previous end
                    // of turn phase !!!

                    if (peek[0].isValid(shareTypes, k.getController(), k)) {
                        if (player.isHuman()) {
                            final StringBuilder question = new StringBuilder();
                            question.append("Your top card is ").append(peek[0].getName());
                            question.append(". Reveal card and Pyroclast Consul deals 2 damage to each creature?");
                            if (GameActionUtil.showYesNoDialog(k, question.toString())) {
                                wantDamageCreatures = true;
                            }
                        }
                        // player isComputer()
                        else {
                            if (humanCreatures.size() > computerCreatures.size()) {
                                final String title = "Computer reveals";
                                this.revealTopCard(title);
                                wantDamageCreatures = true;
                            }
                        }
                    } else if (player.isHuman()) {
                        final String title = "Your top card is";
                        this.revealTopCard(title);
                    }

                    if (wantDamageCreatures) {
                        final CardList allCreatures = AllZoneUtil.getCreaturesInPlay();
                        for (final Card crd : allCreatures) {
                            crd.addDamage(2, k);
                        }
                    }
                } // resolve()

                private void revealTopCard(final String title) {
                    if (peek[0] != prevCardShown[0]) {
                        GuiUtils.getChoice(title, peek[0]);
                        prevCardShown[0] = peek[0];
                    }
                } // revealTopCard()
            }; // ability

            final StringBuilder sb = new StringBuilder();
            sb.append("Pyroclast Consul - ").append(player);
            sb.append(" triggers Kinship");
            ability.setStackDescription(sb.toString());

            AllZone.getStack().addSimultaneousStackEntry(ability);

        } // for
    } // upkeep_Pyroclast_Consul()

    /**
     * <p>
     * upkeep_Sensation_Gorger.
     * </p>
     */
    private static void upkeepSensationGorger() {
        final Player player = AllZone.getPhase().getPlayerTurn();
        final CardList kinship = player.getCardsIn(Zone.Battlefield, "Sensation Gorger");
        final Player opponent = player.getOpponent();

        final PlayerZone library = player.getZone(Constant.Zone.Library);
        // Players would not choose to trigger Kinship ability if library is
        // empty.
        // Useful for games when the "Milling = Loss Condition" check box is
        // unchecked.

        if ((kinship.size() == 0) || (library.size() <= 0)) {
            return;
        }

        final String[] shareTypes = { "Goblin", "Shaman" };
        final Card[] prevCardShown = { null };
        final Card[] peek = { null };

        for (final Card k : kinship) {
            final Ability ability = new Ability(k, "0") { // change to triggered
                // abilities when ready
                @Override
                public void resolve() {
                    final PlayerZone library = player.getZone(Constant.Zone.Library);
                    final PlayerZone hand = player.getZone(Constant.Zone.Hand);
                    if (library.size() <= 0) {
                        return;
                    }

                    peek[0] = library.get(0);
                    boolean wantDiscardThenDraw = false;

                    // We assume that both players will want to peek, ask if
                    // they want to reveal.
                    // We do not want to slow down the pace of the game by
                    // asking too many questions.
                    // Dialogs outside of the Ability appear at the previous end
                    // of turn phase !!!

                    if (peek[0].isValid(shareTypes, k.getController(), k)) {
                        if (player.isHuman()) {
                            final StringBuilder question = new StringBuilder();
                            question.append("Your top card is ").append(peek[0].getName());
                            question.append(". Reveal card and have both players discard their hand and draw 4 cards?");
                            if (GameActionUtil.showYesNoDialog(k, question.toString())) {
                                wantDiscardThenDraw = true;
                            }
                        }
                        // player isComputer()
                        else {
                            if ((library.size() > 4) && (hand.size() < 2)) {
                                final String title = "Computer reveals";
                                this.revealTopCard(title);
                                wantDiscardThenDraw = true;
                            }
                        }
                    } else if (player.isHuman()) {
                        final String title = "Your top card is";
                        this.revealTopCard(title);
                    }
                    if (wantDiscardThenDraw) {
                        player.discardHand(this);
                        opponent.discardHand(this);

                        player.drawCards(4);
                        opponent.drawCards(4);
                    }
                } // resolve()

                private void revealTopCard(final String title) {
                    if (peek[0] != prevCardShown[0]) {
                        GuiUtils.getChoice(title, peek[0]);
                        prevCardShown[0] = peek[0];
                    }
                } // revealTopCard()
            }; // ability

            final StringBuilder sb = new StringBuilder();
            sb.append("Sensation Gorger - ").append(player);
            sb.append(" triggers Kinship");
            ability.setStackDescription(sb.toString());

            AllZone.getStack().addSimultaneousStackEntry(ability);

        } // for
    } // upkeep_Sensation_Gorger()

    /**
     * <p>
     * upkeep_Squeaking_Pie_Grubfellows.
     * </p>
     */
    private static void upkeepSqueakingPieGrubfellows() {
        final Player player = AllZone.getPhase().getPlayerTurn();
        final CardList kinship = player.getCardsIn(Zone.Battlefield, "Squeaking Pie Grubfellows");
        final Player opponent = player.getOpponent();

        final PlayerZone library = player.getZone(Constant.Zone.Library);
        // Players would not choose to trigger Kinship ability if library is
        // empty.
        // Useful for games when the "Milling = Loss Condition" check box is
        // unchecked.

        if ((kinship.size() == 0) || (library.size() <= 0)) {
            return;
        }

        final String[] shareTypes = { "Goblin", "Shaman" };
        final Card[] prevCardShown = { null };
        final Card[] peek = { null };

        for (final Card k : kinship) {
            final Ability ability = new Ability(k, "0") { // change to triggered
                // abilities when ready
                @Override
                public void resolve() {
                    final PlayerZone library = player.getZone(Constant.Zone.Library);
                    if (library.size() <= 0) {
                        return;
                    }

                    peek[0] = library.get(0);
                    boolean wantOpponentDiscard = false;

                    // We assume that both players will want to peek, ask if
                    // they want to reveal.
                    // We do not want to slow down the pace of the game by
                    // asking too many questions.
                    // Dialogs outside of the Ability appear at the previous end
                    // of turn phase !!!

                    if (peek[0].isValid(shareTypes, k.getController(), k)) {
                        if (player.isHuman()) {
                            final StringBuilder question = new StringBuilder();
                            question.append("Your top card is ").append(peek[0].getName());
                            question.append(". Reveal card and have opponent discard a card?");
                            if (GameActionUtil.showYesNoDialog(k, question.toString())) {
                                wantOpponentDiscard = true;
                            }
                        }
                        // player isComputer()
                        else {
                            final String title = "Computer reveals";
                            this.revealTopCard(title);
                            wantOpponentDiscard = true;
                        }
                    } else if (player.isHuman()) {
                        final String title = "Your top card is";
                        this.revealTopCard(title);
                    }

                    if (wantOpponentDiscard) {
                        opponent.discard(this);
                    }
                } // resolve()

                private void revealTopCard(final String title) {
                    if (peek[0] != prevCardShown[0]) {
                        GuiUtils.getChoice(title, peek[0]);
                        prevCardShown[0] = peek[0];
                    }
                } // revealTopCard()
            }; // ability

            final StringBuilder sb = new StringBuilder();
            sb.append("Squeaking Pie Grubfellows - ").append(player);
            sb.append(" triggers Kinship");
            ability.setStackDescription(sb.toString());

            AllZone.getStack().addSimultaneousStackEntry(ability);

        } // for
    } // upkeep_Squeaking_Pie_Grubfellows()

    /**
     * <p>
     * upkeep_Wandering_Graybeard.
     * </p>
     */
    private static void upkeepWanderingGraybeard() {
        final Player player = AllZone.getPhase().getPlayerTurn();
        final CardList kinship = player.getCardsIn(Zone.Battlefield, "Wandering Graybeard");

        final PlayerZone library = player.getZone(Constant.Zone.Library);
        // Players would not choose to trigger Kinship ability if library is
        // empty.
        // Useful for games when the "Milling = Loss Condition" check box is
        // unchecked.

        if ((kinship.size() == 0) || (library.size() <= 0)) {
            return;
        }

        final String[] shareTypes = { "Giant", "Wizard" };
        final Card[] prevCardShown = { null };
        final Card[] peek = { null };

        for (final Card k : kinship) {
            final Ability ability = new Ability(k, "0") { // change to triggered
                // abilities when ready
                @Override
                public void resolve() {
                    final PlayerZone library = player.getZone(Constant.Zone.Library);
                    if (library.size() <= 0) {
                        return;
                    }

                    peek[0] = library.get(0);
                    boolean wantGainLife = false;

                    // We assume that both players will want to peek, ask if
                    // they want to reveal.
                    // We do not want to slow down the pace of the game by
                    // asking too many questions.
                    // Dialogs outside of the Ability appear at the previous end
                    // of turn phase !!!

                    if (peek[0].isValid(shareTypes, k.getController(), k)) {
                        if (player.isHuman()) {
                            final StringBuilder question = new StringBuilder();
                            question.append("Your top card is ").append(peek[0].getName());
                            question.append(". Reveal card and gain 4 life?");
                            if (GameActionUtil.showYesNoDialog(k, question.toString())) {
                                wantGainLife = true;
                            }
                        }
                        // player isComputer()
                        else {
                            final String title = "Computer reveals";
                            this.revealTopCard(title);
                            wantGainLife = true;
                        }
                    } else if (player.isHuman()) {
                        final String title = "Your top card is";
                        this.revealTopCard(title);
                    }
                    if (wantGainLife) {
                        player.gainLife(4, k);
                    }
                } // resolve()

                private void revealTopCard(final String title) {
                    if (peek[0] != prevCardShown[0]) {
                        GuiUtils.getChoice(title, peek[0]);
                        prevCardShown[0] = peek[0];
                    }
                } // revealTopCard()
            }; // ability

            final StringBuilder sb = new StringBuilder();
            sb.append("Wandering Graybeard - ").append(player);
            sb.append(" triggers Kinship");
            ability.setStackDescription(sb.toString());

            AllZone.getStack().addSimultaneousStackEntry(ability);

        } // for
    } // upkeep_Wandering_Graybeard()

    /**
     * <p>
     * upkeep_Waterspout_Weavers.
     * </p>
     */
    private static void upkeepWaterspoutWeavers() {
        final Player player = AllZone.getPhase().getPlayerTurn();
        final CardList kinship = player.getCardsIn(Zone.Battlefield, "Waterspout Weavers");

        final PlayerZone library = player.getZone(Constant.Zone.Library);
        // Players would not choose to trigger Kinship ability if library is
        // empty.
        // Useful for games when the "Milling = Loss Condition" check box is
        // unchecked.

        if ((kinship.size() == 0) || (library.size() <= 0)) {
            return;
        }

        final String[] shareTypes = { "Merfolk", "Wizard" };
        final Card[] prevCardShown = { null };
        final Card[] peek = { null };

        for (final Card k : kinship) {
            final Ability ability = new Ability(k, "0") { // change to triggered
                // abilities when ready
                @Override
                public void resolve() {
                    final PlayerZone library = player.getZone(Constant.Zone.Library);
                    if (library.size() <= 0) {
                        return;
                    }

                    peek[0] = library.get(0);
                    boolean wantMerfolkBuff = false;

                    // We assume that both players will want to peek, ask if
                    // they want to reveal.
                    // We do not want to slow down the pace of the game by
                    // asking too many questions.
                    // Dialogs outside of the Ability appear at the previous end
                    // of turn phase !!!

                    if (peek[0].isValid(shareTypes, k.getController(), k)) {
                        if (player.isHuman()) {
                            final StringBuilder question = new StringBuilder();
                            question.append("Your top card is ").append(peek[0].getName());
                            question.append(". Reveal card and each creature you ");
                            question.append("control gains flying until end of turn?");
                            if (GameActionUtil.showYesNoDialog(k, question.toString())) {
                                wantMerfolkBuff = true;
                            }
                        }
                        // player isComputer()
                        else {
                            final String title = "Computer reveals";
                            this.revealTopCard(title);
                            wantMerfolkBuff = true;
                        }
                    } else if (player.isHuman()) {
                        final String title = "Your top card is";
                        this.revealTopCard(title);
                    }

                    if (wantMerfolkBuff) {
                        final CardList creatures = AllZoneUtil.getCreaturesInPlay(player);
                        for (int i = 0; i < creatures.size(); i++) {
                            if (!creatures.get(i).hasKeyword("Flying")) {
                                creatures.get(i).addExtrinsicKeyword("Flying");
                            }
                        }
                        final Command untilEOT = new Command() {
                            private static final long serialVersionUID = -1978446996943583910L;

                            @Override
                            public void execute() {
                                final CardList creatures = AllZoneUtil.getCreaturesInPlay(player);
                                for (int i = 0; i < creatures.size(); i++) {
                                    if (creatures.get(i).hasKeyword("Flying")) {
                                        creatures.get(i).removeExtrinsicKeyword("Flying");
                                    }
                                }
                            }
                        };
                        AllZone.getEndOfTurn().addUntil(untilEOT);
                    }
                } // resolve()

                private void revealTopCard(final String title) {
                    if (peek[0] != prevCardShown[0]) {
                        GuiUtils.getChoice(title, peek[0]);
                        prevCardShown[0] = peek[0];
                    }
                } // revealTopCard()
            }; // ability

            final StringBuilder sb = new StringBuilder();
            sb.append("Waterspout Weavers - ").append(player);
            sb.append(" triggers Kinship");
            ability.setStackDescription(sb.toString());

            AllZone.getStack().addSimultaneousStackEntry(ability);

        } // for
    } // upkeep_Waterspout_Weavers()

    /**
     * <p>
     * upkeep_Winnower_Patrol.
     * </p>
     */
    private static void upkeepWinnowerPatrol() {
        final Player player = AllZone.getPhase().getPlayerTurn();
        final CardList kinship = player.getCardsIn(Zone.Battlefield, "Winnower Patrol");

        final PlayerZone library = player.getZone(Constant.Zone.Library);
        // Players would not choose to trigger Kinship ability if library is
        // empty.
        // Useful for games when the "Milling = Loss Condition" check box is
        // unchecked.

        if ((kinship.size() == 0) || (library.size() <= 0)) {
            return;
        }

        final String[] shareTypes = { "Elf", "Warrior" };
        final Card[] prevCardShown = { null };
        final Card[] peek = { null };

        for (final Card k : kinship) {
            final Ability ability = new Ability(k, "0") { // change to triggered
                // abilities when ready
                @Override
                public void resolve() {
                    final PlayerZone library = player.getZone(Constant.Zone.Library);
                    if (library.size() <= 0) {
                        return;
                    }

                    peek[0] = library.get(0);
                    boolean wantCounter = false;

                    // We assume that both players will want to peek, ask if
                    // they want to reveal.
                    // We do not want to slow down the pace of the game by
                    // asking too many questions.
                    // Dialogs outside of the Ability appear at the previous end
                    // of turn phase !!!

                    if (peek[0].isValid(shareTypes, k.getController(), k)) {
                        if (player.isHuman()) {
                            final StringBuilder question = new StringBuilder();
                            question.append("Your top card is ").append(peek[0].getName());
                            question.append(". Reveal card and put a +1/+1 counter on Winnower Patrol?");
                            if (GameActionUtil.showYesNoDialog(k, question.toString())) {
                                wantCounter = true;
                            }
                        }
                        // player isComputer()
                        else {
                            final String title = "Computer reveals";
                            this.revealTopCard(title);
                            wantCounter = true;
                        }
                    } else if (player.isHuman()) {
                        final String title = "Your top card is";
                        this.revealTopCard(title);
                    }
                    if (wantCounter) {
                        k.addCounter(Counters.P1P1, 1);
                    }
                } // resolve()

                private void revealTopCard(final String title) {
                    if (peek[0] != prevCardShown[0]) {
                        GuiUtils.getChoice(title, peek[0]);
                        prevCardShown[0] = peek[0];
                    }
                } // revealTopCard()
            }; // ability

            final StringBuilder sb = new StringBuilder();
            sb.append("Winnower Patrol - ").append(player);
            sb.append(" triggers Kinship");
            ability.setStackDescription(sb.toString());

            AllZone.getStack().addSimultaneousStackEntry(ability);

        } // for
    } // upkeep_Winnower_Patrol()

    /**
     * <p>
     * upkeep_Wolf_Skull_Shaman.
     * </p>
     */
    private static void upkeepWolfSkullShaman() {
        final Player player = AllZone.getPhase().getPlayerTurn();
        final CardList kinship = player.getCardsIn(Zone.Battlefield, "Wolf-Skull Shaman");

        final PlayerZone library = player.getZone(Constant.Zone.Library);
        // Players would not choose to trigger Kinship ability if library is
        // empty.
        // Useful for games when the "Milling = Loss Condition" check box is
        // unchecked.

        if ((kinship.size() == 0) || (library.size() <= 0)) {
            return;
        }

        final String[] shareTypes = { "Elf", "Shaman" };
        final Card[] prevCardShown = { null };
        final Card[] peek = { null };

        for (final Card k : kinship) {
            final Ability ability = new Ability(k, "0") { // change to triggered
                // abilities when ready
                @Override
                public void resolve() {
                    final PlayerZone library = player.getZone(Constant.Zone.Library);
                    if (library.size() <= 0) {
                        return;
                    }

                    peek[0] = library.get(0);
                    boolean wantToken = false;

                    // We assume that both players will want to peek, ask if
                    // they want to reveal.
                    // We do not want to slow down the pace of the game by
                    // asking too many questions.
                    // Dialogs outside of the Ability appear at the previous end
                    // of turn phase !!!

                    if (peek[0].isValid(shareTypes, k.getController(), k)) {
                        if (player.isHuman()) {
                            final StringBuilder question = new StringBuilder();
                            question.append("Your top card is ").append(peek[0].getName());
                            question.append(". Reveal card and put a 2/2 green "
                                    + "Wolf creature token onto the battlefield?");
                            if (GameActionUtil.showYesNoDialog(k, question.toString())) {
                                wantToken = true;
                            }
                        }
                        // player isComputer()
                        else {
                            final String title = "Computer reveals";
                            this.revealTopCard(title);
                            wantToken = true;
                        }
                    } else if (player.isHuman()) {
                        final String title = "Your top card is";
                        this.revealTopCard(title);
                    }

                    if (wantToken) {
                        CardFactoryUtil.makeToken("Wolf", "G 2 2 Wolf", k.getController(), "G", new String[] {
                                "Creature", "Wolf" }, 2, 2, new String[] { "" });
                    }
                } // resolve()

                private void revealTopCard(final String title) {
                    if (peek[0] != prevCardShown[0]) {
                        GuiUtils.getChoice(title, peek[0]);
                        prevCardShown[0] = peek[0];
                    }
                } // revealTopCard()
            }; // ability

            final StringBuilder sb = new StringBuilder();
            sb.append("Wolf-Skull Shaman - ").append(player);
            sb.append(" triggers Kinship");
            ability.setStackDescription(sb.toString());

            AllZone.getStack().addSimultaneousStackEntry(ability);

        } // for
    } // upkeep_Wolf_Skull_Shaman()

    // /////////////////////
    // End of Kinship cards
    // /////////////////////

    /**
     * <p>
     * upkeep_Dark_Confidant.
     * </p>
     */
    /*
     * private static void upkeep_Dark_Confidant() { final Player player =
     * AllZone.getPhase().getPlayerTurn();
     * 
     * CardList list = player.getCardsIn(Zone.Battlefield); list =
     * list.filter(new CardListFilter() { public boolean addCard(final Card c) {
     * return c.getName().equals("Dark Confidant") ||
     * c.getName().equals("Dark Tutelage"); } });
     * 
     * Ability ability; for (int i = 0; i < list.size(); i++) { final Card fCard
     * = list.get(i); ability = new Ability(fCard, "0") {
     * 
     * @Override public void resolve() { CardList lib =
     * AllZoneUtil.getPlayerCardsInLibrary(player); if (lib.size() > 0) { Card
     * toMove = lib.get(0); AllZone.getGameAction().moveToHand(toMove);
     * player.loseLife(toMove.getCMC(), fCard); } } // resolve() }; // Ability
     * 
     * StringBuilder sb = new StringBuilder();
     * sb.append(fCard).append(" - ").append( "At the beginning of your upkeep,
     * reveal the top card of your library and put that card into your hand. You
     * lose life equal to its converted mana cost." );
     * ability.setStackDescription(sb.toString());
     * 
     * AllZone.getStack().addSimultaneousStackEntry(ability);
     * 
     * } // for } // upkeep_Dark_Confidant()
     */

    /**
     * <p>
     * upkeep_Suspend.
     * </p>
     */
    public static void upkeepSuspend() {
        final Player player = AllZone.getPhase().getPlayerTurn();

        CardList list = player.getCardsIn(Zone.Exile);

        list = list.filter(new CardListFilter() {
            @Override
            public boolean addCard(final Card c) {
                return c.hasSuspend();
            }
        });

        if (list.size() == 0) {
            return;
        }

        for (final Card c : list) {
            final int counters = c.getCounters(Counters.TIME);
            if (counters > 0) {
                c.subtractCounter(Counters.TIME, 1);
            }
        }
    } // suspend

    /**
     * <p>
     * upkeep_Vanishing.
     * </p>
     */
    private static void upkeepVanishing() {

        final Player player = AllZone.getPhase().getPlayerTurn();
        CardList list = player.getCardsIn(Zone.Battlefield);
        list = list.filter(new CardListFilter() {
            @Override
            public boolean addCard(final Card c) {
                return CardFactoryUtil.hasKeyword(c, "Vanishing") != -1;
            }
        });
        if (list.size() > 0) {
            for (int i = 0; i < list.size(); i++) {
                final Card card = list.get(i);
                final Ability ability = new Ability(card, "0") {
                    @Override
                    public void resolve() {
                        card.subtractCounter(Counters.TIME, 1);
                    }
                }; // ability

                final StringBuilder sb = new StringBuilder();
                sb.append(card.getName()).append(" - Vanishing - remove a time counter from it. ");
                sb.append("When the last is removed, sacrifice it.)");
                ability.setStackDescription(sb.toString());

                AllZone.getStack().addSimultaneousStackEntry(ability);

            }
        }
    }

    /**
     * <p>
     * upkeep_Fading.
     * </p>
     */
    private static void upkeepFading() {

        final Player player = AllZone.getPhase().getPlayerTurn();
        CardList list = player.getCardsIn(Zone.Battlefield);
        list = list.filter(new CardListFilter() {
            @Override
            public boolean addCard(final Card c) {
                return CardFactoryUtil.hasKeyword(c, "Fading") != -1;
            }
        });
        if (list.size() > 0) {
            for (int i = 0; i < list.size(); i++) {
                final Card card = list.get(i);
                final Ability ability = new Ability(card, "0") {
                    @Override
                    public void resolve() {
                        final int fadeCounters = card.getCounters(Counters.FADE);
                        if (fadeCounters <= 0) {
                            AllZone.getGameAction().sacrifice(card);
                        } else {
                            card.subtractCounter(Counters.FADE, 1);
                        }
                    }
                }; // ability

                final StringBuilder sb = new StringBuilder();
                sb.append(card.getName()).append(" - Fading - remove a fade counter from it. ");
                sb.append("If you can't, sacrifice it.)");
                ability.setStackDescription(sb.toString());

                AllZone.getStack().addSimultaneousStackEntry(ability);

            }
        }
    }

    /**
     * <p>
     * upkeep_Oath_of_Druids.
     * </p>
     */
    private static void upkeepOathOfDruids() {
        final CardList oathList = AllZoneUtil.getCardsIn(Zone.Battlefield, "Oath of Druids");
        if (oathList.isEmpty()) {
            return;
        }

        final Player player = AllZone.getPhase().getPlayerTurn();

        if (AllZoneUtil.compareTypeAmountInPlay(player, "Creature") < 0) {
            for (int i = 0; i < oathList.size(); i++) {
                final Card oath = oathList.get(i);
                final Ability ability = new Ability(oath, "0") {
                    @Override
                    public void resolve() {
                        final CardList libraryList = player.getCardsIn(Zone.Library);
                        final PlayerZone battlefield = player.getZone(Constant.Zone.Battlefield);
                        boolean oathFlag = true;

                        if (AllZoneUtil.compareTypeAmountInPlay(player, "Creature") < 0) {
                            if (player.isHuman()) {
                                final StringBuilder question = new StringBuilder();
                                question.append("Reveal cards from the top of your library and place ");
                                question.append("the first creature revealed onto the battlefield?");
                                if (!GameActionUtil.showYesNoDialog(oath, question.toString())) {
                                    oathFlag = false;
                                }
                            } else { // if player == Computer
                                final CardList creaturesInLibrary = player.getCardsIn(Zone.Library).getType("Creature");
                                final CardList creaturesInBattlefield = player.getCardsIn(Zone.Battlefield).getType(
                                        "Creature");

                                // if there are at least 3 creatures in library,
                                // or none in play with one in library, oath
                                if ((creaturesInLibrary.size() > 2)
                                        || ((creaturesInBattlefield.size() == 0) && (creaturesInLibrary.size() > 0))) {
                                    oathFlag = true;
                                } else {
                                    oathFlag = false;
                                }
                            }

                            if (oathFlag) {
                                final CardList cardsToReveal = new CardList();
                                final int max = libraryList.size();
                                for (int i = 0; i < max; i++) {
                                    final Card c = libraryList.get(i);
                                    cardsToReveal.add(c);
                                    if (c.isCreature()) {
                                        AllZone.getGameAction().moveTo(battlefield, c);
                                        break;
                                    } else {
                                        AllZone.getGameAction().moveToGraveyard(c);
                                    }
                                } // for loop
                                if (cardsToReveal.size() > 0) {
                                    GuiUtils.getChoice("Revealed cards", cardsToReveal.toArray());
                                }
                            }
                        }
                    }
                }; // Ability

                final StringBuilder sb = new StringBuilder();
                sb.append("At the beginning of each player's upkeep, that player chooses target player ");
                sb.append("who controls more creatures than he or she does and is his or her opponent. The ");
                sb.append("first player may reveal cards from the top of his or her library until he or she ");
                sb.append("reveals a creature card. If he or she does, that player puts that card onto the ");
                sb.append("battlefield and all other cards revealed this way into his or her graveyard.");
                ability.setStackDescription(sb.toString());

                AllZone.getStack().addSimultaneousStackEntry(ability);

            }
        }
    } // upkeep_Oath of Druids()

    /**
     * <p>
     * upkeep_Oath_of_Ghouls.
     * </p>
     */
    private static void upkeepOathOfGhouls() {
        final CardList oathList = AllZoneUtil.getCardsIn(Zone.Battlefield, "Oath of Ghouls");
        if (oathList.isEmpty()) {
            return;
        }

        final Player player = AllZone.getPhase().getPlayerTurn();

        if (AllZoneUtil.compareTypeAmountInGraveyard(player, "Creature") > 0) {
            for (int i = 0; i < oathList.size(); i++) {
                final Ability ability = new Ability(oathList.get(0), "0") {
                    @Override
                    public void resolve() {
                        final CardList graveyardCreatures = player.getCardsIn(Zone.Graveyard).getType("Creature");

                        if (AllZoneUtil.compareTypeAmountInGraveyard(player, "Creature") > 0) {
                            if (player.isHuman()) {
                                final Object o = GuiUtils.getChoiceOptional("Pick a creature to return to hand",
                                        graveyardCreatures.toArray());
                                if (o != null) {
                                    final Card card = (Card) o;

                                    AllZone.getGameAction().moveToHand(card);
                                }
                            } else if (player.isComputer()) {
                                final Card card = graveyardCreatures.get(0);

                                AllZone.getGameAction().moveToHand(card);
                            }
                        }
                    }
                }; // Ability

                final StringBuilder sb = new StringBuilder();
                sb.append("At the beginning of each player's upkeep, Oath of Ghouls returns a creature ");
                sb.append("from their graveyard to owner's hand if they have more than an opponent.");
                ability.setStackDescription(sb.toString());

                AllZone.getStack().addSimultaneousStackEntry(ability);

            }
        }
    } // Oath of Ghouls

    /**
     * <p>
     * upkeep_Karma.
     * </p>
     */
    private static void upkeepKarma() {
        final Player player = AllZone.getPhase().getPlayerTurn();
        final CardList karmas = AllZoneUtil.getCardsIn(Zone.Battlefield, "Karma");
        final CardList swamps = player.getCardsIn(Zone.Battlefield).getType("Swamp");

        // determine how much damage to deal the current player
        final int damage = swamps.size();

        // if there are 1 or more Karmas on the
        // battlefield have each of them deal damage.
        if (0 < karmas.size()) {
            for (final Card karma : karmas) {
                final Card src = karma;
                final Ability ability = new Ability(src, "0") {
                    @Override
                    public void resolve() {
                        if (damage > 0) {
                            player.addDamage(damage, src);
                        }
                    }
                }; // Ability
                if (damage > 0) {

                    final StringBuilder sb = new StringBuilder();
                    sb.append("Karma deals ").append(damage).append(" damage to ").append(player);
                    ability.setStackDescription(sb.toString());

                    AllZone.getStack().addSimultaneousStackEntry(ability);

                }
            }
        } // if
    } // upkeep_Karma()

    /**
     * <p>
     * upkeep_Dega_Sanctuary.
     * </p>
     */
    private static void upkeepDegaSanctuary() {
        final Player player = AllZone.getPhase().getPlayerTurn();

        final CardList list = player.getCardsIn(Zone.Battlefield, "Dega Sanctuary");

        for (final Card sanc : list) {
            final Card source = sanc;
            final Ability ability = new Ability(source, "0") {
                @Override
                public void resolve() {
                    int gain = 0;
                    final CardList play = player.getCardsIn(Zone.Battlefield);
                    final CardList black = play.filter(CardListFilter.BLACK);
                    final CardList red = play.filter(CardListFilter.RED);
                    if ((black.size() > 0) && (red.size() > 0)) {
                        gain = 4;
                    } else if ((black.size() > 0) || (red.size() > 0)) {
                        gain = 2;
                    }
                    player.gainLife(gain, source);
                }
            }; // Ability

            final StringBuilder sb = new StringBuilder();
            sb.append(source.getName()).append(" - ");
            sb.append("if you control a black or red permanent, you gain 2 life. "
                    + "If you control a black permanent and a red permanent, you gain 4 life instead.");
            ability.setStackDescription(sb.toString());

            AllZone.getStack().addSimultaneousStackEntry(ability);

        } // for
    } // upkeep_Dega_Sanctuary()

    /**
     * <p>
     * upkeep_Ceta_Sanctuary.
     * </p>
     */
    private static void upkeepCetaSanctuary() {
        final Player player = AllZone.getPhase().getPlayerTurn();

        final CardList list = player.getCardsIn(Zone.Battlefield, "Ceta Sanctuary");

        for (final Card sanc : list) {
            final Card source = sanc;
            final Ability ability = new Ability(source, "0") {
                @Override
                public void resolve() {
                    int draw = 0;
                    final CardList play = player.getCardsIn(Zone.Battlefield);
                    final CardList green = play.filter(CardListFilter.GREEN);
                    final CardList red = play.filter(CardListFilter.RED);

                    if ((green.size() > 0) && (red.size() > 0)) {
                        draw = 2;
                    } else if ((green.size() > 0) || (red.size() > 0)) {
                        draw = 1;
                    }

                    if (draw > 0) {
                        player.drawCards(draw);
                        player.discard(1, this, true);
                    }
                }
            }; // Ability

            final StringBuilder sb = new StringBuilder();
            sb.append(source).append(" - ");
            sb.append("At the beginning of your upkeep, if you control a red or green permanent, "
                    + "draw a card, then discard a card. If you control a red permanent and a green permanent, "
                    + "instead draw two cards, then discard a card.");
            ability.setStackDescription(sb.toString());

            AllZone.getStack().addSimultaneousStackEntry(ability);

        } // for
    } // upkeep_Ceta_Sanctuary()

    /**
     * <p>
     * upkeep_Power_Surge.
     * </p>
     */
    private static void upkeepPowerSurge() {
        /*
         * At the beginning of each player's upkeep, Power Surge deals X damage
         * to that player, where X is the number of untapped lands he or she
         * controlled at the beginning of this turn.
         */
        final Player player = AllZone.getPhase().getPlayerTurn();
        final CardList list = AllZoneUtil.getCardsIn(Zone.Battlefield, "Power Surge");
        final int damage = player.getNumPowerSurgeLands();

        for (final Card surge : list) {
            final Card source = surge;
            final Ability ability = new Ability(source, "0") {
                @Override
                public void resolve() {
                    player.addDamage(damage, source);
                }
            }; // Ability

            final StringBuilder sb = new StringBuilder();
            sb.append(source).append(" - deals ").append(damage).append(" damage to ").append(player);
            ability.setStackDescription(sb.toString());

            if (damage > 0) {
                AllZone.getStack().addSimultaneousStackEntry(ability);
            }
        } // for
    } // upkeep_Power_Surge()

    /**
     * <p>
     * upkeep_Vesuvan_Doppelganger_Keyword.
     * </p>
     */
    private static void upkeepVesuvanDoppelgangerKeyword() {
        // TODO - what about enchantments? i dont know how great this solution
        // is
        final Player player = AllZone.getPhase().getPlayerTurn();
        final String keyword = "At the beginning of your upkeep, you may have this "
        + "creature become a copy of target creature except it doesn't copy that "
                + "creature's color. If you do, this creature gains this ability.";
        CardList list = player.getCardsIn(Zone.Battlefield);
        list = list.getKeyword(keyword);

        for (final Card c : list) {
            final SpellAbility ability = new Ability(c, "0") {
                @Override
                public void resolve() {
                    final Card[] newTarget = new Card[1];
                    newTarget[0] = null;

                    final Ability switchTargets = new Ability(c, "0") {
                        @Override
                        public void resolve() {
                            if (newTarget[0] != null) {
                                /*
                                 * 1. need to select new card - DONE 1a. need to
                                 * create the newly copied card with pic and
                                 * setinfo 2. need to add the leaves play
                                 * command 3. need to transfer the keyword 4.
                                 * need to update the clone origin of new card
                                 * and old card 5. remove clone leaves play
                                 * commands from old 5a. remove old from play 6.
                                 * add new to play
                                 */

                                final Card newCopy = AllZone.getCardFactory().getCard(newTarget[0].getState("Original").getName(), player);
                                newCopy.setCurSetCode(newTarget[0].getCurSetCode());
                                newCopy.setImageFilename(newTarget[0].getImageFilename());

                                newCopy.setState(newTarget[0].getCurState());
                                
                                CardFactoryUtil.copyCharacteristics(newCopy, c);
                                c.addColor("U");
                                
                                c.addExtrinsicKeyword(keyword);
                            }
                        }
                    };

                    AllZone.getInputControl().setInput(new Input() {
                        private static final long serialVersionUID = 5662272658873063221L;

                        @Override
                        public void showMessage() {
                            AllZone.getDisplay().showMessage(
                                    c.getName() + " - Select new target creature.  (Click Cancel to remain as is.)");
                            ButtonUtil.enableOnlyCancel();
                        }

                        @Override
                        public void selectButtonCancel() {
                            this.stop();
                        }

                        @Override
                        public void selectCard(final Card selectedCard, final PlayerZone z) {
                            if (z.is(Constant.Zone.Battlefield) && selectedCard.isCreature()
                                    && selectedCard.canBeTargetedBy(switchTargets)) {
                                newTarget[0] = selectedCard;
                                final StringBuilder sb = new StringBuilder();
                                sb.append(c.getCloneOrigin()).append(
                                        " - switching to copy " + selectedCard.getName() + ".");
                                switchTargets.setStackDescription(sb.toString());
                                AllZone.getStack().add(switchTargets);
                                this.stop();
                            }
                        }
                    });
                }
            };
            ability.setDescription("At the beginning of your upkeep, you may have this creature become "
            + "a copy of target creature except it doesn't copy that creature's color. If you do, this creature gains this ability.");
            ability.setStackDescription(c.getName() + " - you may have this creature become a copy of target creature.");

            AllZone.getStack().addSimultaneousStackEntry(ability);

        } // foreach(Card)
    } // upkeep_Vesuvan_Doppelganger_Keyword

    /**
     * <p>
     * upkeep_Tangle_Wire.
     * </p>
     */
    private static void upkeepTangleWire() {
        final Player player = AllZone.getPhase().getPlayerTurn();
        final CardList wires = AllZoneUtil.getCardsIn(Zone.Battlefield, "Tangle Wire");

        for (final Card source : wires) {
            final SpellAbility ability = new Ability(source, "0") {
                @Override
                public void resolve() {
                    final int num = source.getCounters(Counters.FADE);
                    final CardList list = player.getCardsIn(Zone.Battlefield).filter(new CardListFilter() {
                        @Override
                        public boolean addCard(final Card c) {
                            return (c.isArtifact() || c.isLand() || c.isCreature()) && c.isUntapped();
                        }
                    });

                    for (int i = 0; i < num; i++) {
                        if (player.isComputer()) {
                            final Card toTap = CardFactoryUtil.getWorstPermanentAI(list, false, false, false, false);
                            if (null != toTap) {
                                toTap.tap();
                                list.remove(toTap);
                            }
                        } else {
                            AllZone.getInputControl().setInput(new Input() {
                                private static final long serialVersionUID = 5313424586016061612L;

                                @Override
                                public void showMessage() {
                                    if (list.size() == 0) {
                                        this.stop();
                                        return;
                                    }
                                    AllZone.getDisplay().showMessage(
                                            source.getName() + " - Select " + num
                                                    + " untapped artifact(s), creature(s), or land(s) you control");
                                    ButtonUtil.disableAll();
                                }

                                @Override
                                public void selectCard(final Card card, final PlayerZone zone) {
                                    if (zone.is(Constant.Zone.Battlefield, AllZone.getHumanPlayer())
                                            && list.contains(card)) {
                                        card.tap();
                                        list.remove(card);
                                        this.stop();
                                    }
                                }
                            });
                        }
                    }
                }
            };
            ability.setStackDescription(source.getName() + " - " + player
                    + " taps X artifacts, creatures or lands he or she controls.");

            AllZone.getStack().addSimultaneousStackEntry(ability);

        } // foreach(wire)
    } // upkeep_Tangle_Wire()

    /**
     * <p>
     * upkeep_Masticore.
     * </p>
     */
    private static void upkeepMasticore() {
        final Player player = AllZone.getPhase().getPlayerTurn();

        final CardList list = player.getCardsIn(Zone.Battlefield, "Masticore");
        list.addAll(player.getCardsIn(Zone.Battlefield, "Molten-Tail Masticore"));
        list.addAll(player.getCardsIn(Zone.Battlefield, "Razormane Masticore"));

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
                public void selectCard(final Card c, final PlayerZone zone) {
                    if (zone.is(Constant.Zone.Hand)) {
                        c.getController().discard(c, null);
                        this.stop();
                    }
                }

                @Override
                public void selectButtonCancel() {
                    AllZone.getGameAction().sacrifice(crd);
                    this.stop();
                }
            }; // Input

            ability = new Ability(crd, "0") {
                @Override
                public void resolve() {
                    if (crd.getController().isHuman()) {
                        if (AllZone.getHumanPlayer().getZone(Zone.Hand).size() == 0) {
                            AllZone.getGameAction().sacrifice(crd);
                        } else {
                            AllZone.getInputControl().setInput(discard);
                        }
                    } else { // comp
                        final CardList list = AllZone.getComputerPlayer().getCardsIn(Zone.Hand);

                        if (list.size() != 0) {
                            list.get(0).getController().discard(list.get(0), this);
                        } else {
                            AllZone.getGameAction().sacrifice(crd);
                        }
                    } // else
                } // resolve()
            }; // Ability

            final StringBuilder sb = new StringBuilder();
            sb.append(crd).append(" - sacrifice ").append(crd).append(" unless you discard a card.");
            ability.setStackDescription(sb.toString());
            ability.setDescription(sb.toString());

            AllZone.getStack().addSimultaneousStackEntry(ability);

        } // for
    } // upkeep_Masticore

    /**
     * <p>
     * upkeep_Eldrazi_Monument.
     * </p>
     */
    private static void upkeepEldraziMonument() {
        final Player player = AllZone.getPhase().getPlayerTurn();

        final CardList list = player.getCardsIn(Zone.Battlefield, "Eldrazi Monument");

        Ability ability;
        for (int i = 0; i < list.size(); i++) {
            final Card card = list.get(i);
            ability = new Ability(list.get(i), "0") {
                @Override
                public void resolve() {
                    final CardList creats = AllZoneUtil.getCreaturesInPlay(player);

                    if (creats.size() < 1) {
                        AllZone.getGameAction().sacrifice(card);
                        return;
                    }

                    if (player.isHuman()) {
                        final Object o = GuiUtils.getChoiceOptional("Select creature to sacrifice", creats.toArray());
                        Card sac = (Card) o;
                        if (sac == null) {
                            creats.shuffle();
                            sac = creats.get(0);
                        }
                        AllZone.getGameAction().sacrifice(sac);
                    } else { // computer
                        CardListUtil.sortAttackLowFirst(creats);
                        AllZone.getGameAction().sacrifice(creats.get(0));
                    }
                }
            }; // ability

            final StringBuilder sb = new StringBuilder();
            sb.append("Eldrazi Monument - ").append(player).append(" sacrifices a creature.");
            ability.setStackDescription(sb.toString());

            AllZone.getStack().addSimultaneousStackEntry(ability);

        }

    } // upkeep_Eldrazi_Monument

    /**
     * <p>
     * upkeep_Blaze_Counters.
     * </p>
     */
    private static void upkeepBlazeCounters() {
        final Player player = AllZone.getPhase().getPlayerTurn();

        CardList blaze = player.getCardsIn(Zone.Battlefield);
        blaze = blaze.filter(new CardListFilter() {
            @Override
            public boolean addCard(final Card c) {
                return c.isLand() && (c.getCounters(Counters.BLAZE) > 0);
            }
        });

        for (int i = 0; i < blaze.size(); i++) {
            final Card source = blaze.get(i);
            final Ability ability = new Ability(blaze.get(i), "0") {
                @Override
                public void resolve() {
                    player.addDamage(1, source);
                }
            }; // ability

            final StringBuilder sb = new StringBuilder();
            sb.append(blaze.get(i)).append(" - has a blaze counter and deals 1 damage to ");
            sb.append(player).append(".");
            ability.setStackDescription(sb.toString());

            AllZone.getStack().addSimultaneousStackEntry(ability);

        }
    }

    /**
     * <p>
     * upkeep_Carnophage.
     * </p>
     */
    private static void upkeepCarnophage() {
        final Player player = AllZone.getPhase().getPlayerTurn();

        final CardList list = player.getCardsIn(Zone.Battlefield, "Carnophage");
        if (player.isHuman()) {
            for (int i = 0; i < list.size(); i++) {
                final Card c = list.get(i);
                final String[] choices = { "Yes", "No" };
                final Object choice = GuiUtils.getChoice("Pay Carnophage's upkeep?", choices);
                if (choice.equals("Yes")) {
                    player.loseLife(1, c);
                } else {
                    c.tap();
                }
            }
        } else if (player.isComputer()) {
            for (int i = 0; i < list.size(); i++) {
                final Card c = list.get(i);
                if (AllZone.getComputerPlayer().getLife() > 1) {
                    player.loseLife(1, c);
                } else {
                    c.tap();
                }
            }
        }
    } // upkeep_Carnophage

    /**
     * <p>
     * upkeep_Sangrophage.
     * </p>
     */
    private static void upkeepSangrophage() {
        final Player player = AllZone.getPhase().getPlayerTurn();

        final CardList list = player.getCardsIn(Zone.Battlefield, "Sangrophage");
        if (player.isHuman()) {
            for (int i = 0; i < list.size(); i++) {
                final Card c = list.get(i);
                final String[] choices = { "Yes", "No" };
                final Object choice = GuiUtils.getChoice("Pay Sangrophage's upkeep?", choices);
                if (choice.equals("Yes")) {
                    player.loseLife(2, c);
                } else {
                    c.tap();
                }
            }
        } else if (player.isComputer()) {
            for (int i = 0; i < list.size(); i++) {
                final Card c = list.get(i);
                if (AllZone.getComputerPlayer().getLife() > 2) {
                    player.loseLife(2, c);
                } else {
                    c.tap();
                }
            }
        }
    } // upkeep_Carnophage

} // end class Upkeep
