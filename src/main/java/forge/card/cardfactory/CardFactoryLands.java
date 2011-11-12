package forge.card.cardfactory;

import javax.swing.JOptionPane;

import forge.AllZone;
import forge.AllZoneUtil;
import forge.ButtonUtil;
import forge.Card;
import forge.CardList;
import forge.CardListFilter;
import forge.Command;
import forge.Constant;
import forge.Constant.Zone;
import forge.Counters;
import forge.GameActionUtil;
import forge.MyRandom;
import forge.Player;
import forge.PlayerZone;
import forge.card.cost.Cost;
import forge.card.spellability.Ability;
import forge.card.spellability.AbilityActivated;
import forge.card.spellability.AbilityMana;
import forge.card.spellability.SpellAbility;
import forge.gui.GuiUtils;
import forge.gui.input.Input;

/**
 * <p>
 * CardFactory_Lands class.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 */
class CardFactoryLands {

    /**
     * <p>
     * getCard.
     * </p>
     * 
     * @param card
     *            a {@link forge.Card} object.
     * @param cardName
     *            a {@link java.lang.String} object.
     * @param cf
     *            a {@link forge.card.cardfactory.CardFactoryInterface} object.
     * @return a {@link forge.Card} object.
     */
    public static Card getCard(final Card card, final String cardName, final CardFactoryInterface cf) {

        // *************** START *********** START **************************
        // Ravinca Dual Lands
        if (cardName.equals("Blood Crypt") || cardName.equals("Breeding Pool") || cardName.equals("Godless Shrine")
                || cardName.equals("Hallowed Fountain") || cardName.equals("Overgrown Tomb")
                || cardName.equals("Sacred Foundry") || cardName.equals("Steam Vents")
                || cardName.equals("Stomping Ground") || cardName.equals("Temple Garden")
                || cardName.equals("Watery Grave")) {
            // if this isn't done, computer plays more than 1 copy
            // card.clearSpellAbility();
            card.clearSpellKeepManaAbility();

            card.addComesIntoPlayCommand(new Command() {
                private static final long serialVersionUID = 7352127748114888255L;

                @Override
                public void execute() {
                    if (card.getController().isHuman()) {
                        this.humanExecute();
                    } else {
                        this.computerExecute();
                    }
                }

                public void computerExecute() {
                    boolean pay = false;

                    if (AllZone.getComputerPlayer().getLife() > 9) {
                        pay = MyRandom.getRandom().nextBoolean();
                    }

                    if (pay) {
                        AllZone.getComputerPlayer().loseLife(2, card);
                    } else {
                        this.tapCard();
                    }
                }

                public void humanExecute() {
                    final int life = card.getController().getLife();
                    if (2 < life) {

                        final StringBuilder question = new StringBuilder();
                        question.append("Pay 2 life? If you don't, ").append(card.getName());
                        question.append(" enters the battlefield tapped.");

                        if (GameActionUtil.showYesNoDialog(card, question.toString())) {
                            AllZone.getHumanPlayer().loseLife(2, card);
                        } else {
                            this.tapCard();
                        }

                    } // if
                    else {
                        this.tapCard();
                    }
                } // execute()

                private void tapCard() {
                    // it enters the battlefield this way, and should not fire
                    // triggers
                    card.setTapped(true);
                }
            });
        } // *************** END ************ END **************************

        // *************** START *********** START **************************
        else if (cardName.equals("Dark Depths")) {

            card.addComesIntoPlayCommand(CardFactoryUtil.entersBattleFieldWithCounters(card, Counters.ICE, 10));

            final SpellAbility ability = new Ability(card, "3") {
                @Override
                public boolean canPlay() {
                    for (int i = 0; i < AllZone.getStack().size(); i++) {
                        if (AllZone.getStack().peekInstance(i).getSourceCard().equals(card)) {
                            return false;
                        }
                    }

                    if ((card.getCounters(Counters.ICE) > 0) && AllZoneUtil.isCardInPlay(card) && super.canPlay()) {
                        return true;
                    } else {
                        return false;
                    }
                }

                @Override
                public boolean canPlayAI() {
                    final String phase = AllZone.getPhase().getPhase();
                    return phase.equals(Constant.Phase.MAIN2) && super.canPlayAI();
                }

                @Override
                public void resolve() {
                    card.subtractCounter(Counters.ICE, 1);

                    if (card.getCounters(Counters.ICE) == 0) {
                        CardFactoryUtil.makeToken("Marit Lage", "B 20 20 Marit Lage", card.getController(), "B",
                                new String[] { "Legendary", "Creature", "Avatar" }, 20, 20, new String[] { "Flying",
                                        "Indestructible" });
                        AllZone.getGameAction().sacrifice(card);
                    }
                }
            };
            final SpellAbility sacrifice = new Ability(card, "0") {
                // TODO - this should probably be a state effect
                @Override
                public boolean canPlay() {
                    return (card.getCounters(Counters.ICE) == 0) && AllZoneUtil.isCardInPlay(card) && super.canPlay();
                }

                @Override
                public boolean canPlayAI() {
                    return this.canPlay() && super.canPlayAI();
                }

                @Override
                public void resolve() {
                    if (card.getCounters(Counters.ICE) == 0) {
                        CardFactoryUtil.makeToken("Marit Lage", "B 20 20 Marit Lage", card.getController(), "B",
                                new String[] { "Legendary", "Creature", "Avatar" }, 20, 20, new String[] { "Flying",
                                        "Indestructible" });
                    }
                    AllZone.getGameAction().sacrifice(card);
                }
            };
            ability.setDescription("3: remove an Ice Counter.");
            final StringBuilder sb = new StringBuilder();
            sb.append(card.getName()).append(" - remove an ice counter.");
            ability.setStackDescription(sb.toString());

            card.addSpellAbility(ability);
            final StringBuilder sbSac = new StringBuilder();
            sbSac.append("Sacrifice ").append(card.getName());
            sacrifice.setStackDescription(sbSac.toString());

            card.addSpellAbility(sacrifice);
        } // *************** END ************ END **************************

        // *************** START *********** START **************************
        else if (cardName.equals("Novijen, Heart of Progress")) {
            card.clearSpellKeepManaAbility();

            final CardListFilter targets = new CardListFilter() {

                @Override
                public boolean addCard(final Card c) {
                    return AllZoneUtil.isCardInPlay(c) && c.isCreature()
                            && (c.getTurnInZone() == AllZone.getPhase().getTurn());
                }
            };

            final Cost abCost = new Cost("G U T", cardName, true);
            final AbilityActivated ability = new AbilityActivated(card, abCost, null) {
                private static final long serialVersionUID = 1416258136308898492L;

                private final CardList inPlay = new CardList();

                @Override
                public boolean canPlayAI() {
                    if (!(AllZone.getPhase().getPhase().equals(Constant.Phase.MAIN1) && AllZone.getPhase()
                            .getPlayerTurn().isComputer())) {
                        return false;
                    }
                    this.inPlay.clear();
                    this.inPlay.addAll(AllZone.getComputerPlayer().getCardsIn(Zone.Battlefield));
                    return (this.inPlay.filter(targets).size() > 1) && super.canPlayAI();
                }

                @Override
                public void resolve() {
                    this.inPlay.clear();
                    this.inPlay.addAll(AllZoneUtil.getCardsIn(Zone.Battlefield));
                    for (final Card targ : this.inPlay.filter(targets)) {
                        targ.addCounter(Counters.P1P1, 1);
                    }
                }
            };
            final StringBuilder sbDesc = new StringBuilder();
            sbDesc.append(abCost);
            sbDesc.append("Put a +1/+1 counter on each creature that entered the battlefield this turn.");
            ability.setDescription(sbDesc.toString());

            final StringBuilder sbStack = new StringBuilder();
            sbStack.append(cardName);
            sbStack.append(" - Put a +1/+1 counter on each creature that entered the battlefield this turn.");
            ability.setStackDescription(sbStack.toString());

            card.addSpellAbility(ability);
        }
        // *************** END ************ END **************************

        // *************** START *********** START **************************
        else if (cardName.equals("Lotus Vale")) {
            /*
             * If Lotus Vale would enter the battlefield, sacrifice two untapped
             * lands instead. If you do, put Lotus Vale onto the battlefield. If
             * you don't, put it into its owner's graveyard.
             */
            final Command comesIntoPlay = new Command() {
                private static final long serialVersionUID = -194247993330560188L;

                private final Player player = card.getController();

                @Override
                public void execute() {
                    if (this.player.isHuman()) {
                        final int[] paid = { 0 };

                        final Input target = new Input() {
                            private static final long serialVersionUID = -7835834281866473546L;

                            @Override
                            public void showMessage() {
                                final StringBuilder sb = new StringBuilder();
                                sb.append(cardName).append(" - Select an untapped land to sacrifice");
                                AllZone.getDisplay().showMessage(sb.toString());
                                ButtonUtil.enableOnlyCancel();
                            }

                            @Override
                            public void selectButtonCancel() {
                                AllZone.getGameAction().sacrifice(card);
                                this.stop();
                            }

                            @Override
                            public void selectCard(final Card c, final PlayerZone zone) {
                                if (c.isLand() && zone.is(Constant.Zone.Battlefield) && c.isUntapped()) {
                                    AllZone.getGameAction().sacrifice(c);
                                    if (paid[0] < 1) {
                                        paid[0]++;
                                        final StringBuilder sb = new StringBuilder();
                                        sb.append(cardName).append(" - Select an untapped land to sacrifice");
                                        AllZone.getDisplay().showMessage(sb.toString());
                                    } else {
                                        this.stop();
                                    }
                                }
                            } // selectCard()
                        }; // Input
                        if ((AllZoneUtil.getPlayerLandsInPlay(AllZone.getHumanPlayer()).filter(CardListFilter.UNTAPPED)
                                .size() < 2)) {
                            AllZone.getGameAction().sacrifice(card);
                            return;
                        } else {
                            AllZone.getInputControl().setInput(target);
                        }
                    } else {
                        // compy can't play this card because it has no mana
                        // pool
                    }
                }
            };

            card.addComesIntoPlayCommand(comesIntoPlay);
        } // *************** END ************ END **************************

        // *************** START *********** START **************************
        else if (cardName.equals("Kjeldoran Outpost") || cardName.equals("Balduvian Trading Post")
                || cardName.equals("Heart of Yavimaya") || cardName.equals("Lake of the Dead")
                || cardName.equals("Soldevi Excavations")) {

            final String[] type = new String[1];
            if (cardName.equals("Kjeldoran Outpost")) {
                type[0] = "Plains";
            } else if (cardName.equals("Balduvian Trading Post")) {
                type[0] = "Mountain.untapped";
            } else if (cardName.equals("Heart of Yavimaya")) {
                type[0] = "Forest";
            } else if (cardName.equals("Lake of the Dead")) {
                type[0] = "Swamp";
            } else if (cardName.equals("Soldevi Excavations")) {
                type[0] = "Island.untapped";
            }

            final Command comesIntoPlay = new Command() {
                private static final long serialVersionUID = 6175830918425915833L;
                private final Player player = card.getController();

                @Override
                public void execute() {
                    final CardList land = this.player.getCardsIn(Zone.Battlefield).getValidCards(type[0], this.player,
                            card);

                    if (this.player.isComputer()) {
                        if (land.size() > 0) {
                            CardList tappedLand = new CardList(land.toArray());
                            tappedLand = tappedLand.filter(CardListFilter.TAPPED);
                            // if any are tapped, sacrifice it
                            // else sacrifice random
                            if (tappedLand.size() > 0) {
                                AllZone.getGameAction().sacrifice(tappedLand.get(0));
                            } else {
                                AllZone.getGameAction().sacrifice(land.get(0));
                            }
                        } else {
                            AllZone.getGameAction().sacrifice(card);
                        }
                    } else { // this is the human resolution
                        final Input target = new Input() {
                            private static final long serialVersionUID = 6653677835621129465L;

                            @Override
                            public void showMessage() {
                                AllZone.getDisplay().showMessage(
                                        cardName + " - Select one " + type[0] + " to sacrifice");
                                ButtonUtil.enableOnlyCancel();
                            }

                            @Override
                            public void selectButtonCancel() {
                                AllZone.getGameAction().sacrifice(card);
                                this.stop();
                            }

                            @Override
                            public void selectCard(final Card c, final PlayerZone zone) {
                                if (c.isLand() && zone.is(Zone.Battlefield) && land.contains(c)) {
                                    AllZone.getGameAction().sacrifice(c);
                                    this.stop();
                                }
                            } // selectCard()
                        }; // Input
                        AllZone.getInputControl().setInput(target);
                    }
                }
            };

            card.addComesIntoPlayCommand(comesIntoPlay);
        } // *************** END ************ END **************************

        // *************** START *********** START **************************
        else if (cardName.equals("Sheltered Valley")) {

            final Command comesIntoPlay = new Command() {
                private static final long serialVersionUID = 685604326470832887L;

                @Override
                public void execute() {
                    final Player player = card.getController();
                    final CardList land = player.getCardsIn(Zone.Battlefield, "Sheltered Valley");
                    land.remove(card);

                    if (land.size() > 0) {
                        for (final Card c : land) {
                            AllZone.getGameAction().sacrifice(c);
                        }
                    }
                }
            };

            card.addComesIntoPlayCommand(comesIntoPlay);
        } // *************** END ************ END **************************

        // *************** START *********** START **************************
        else if (cardName.equals("Scorched Ruins")) {
            final Command comesIntoPlay = new Command() {
                private static final long serialVersionUID = 6175830918425915833L;
                private final Player player = card.getController();

                @Override
                public void execute() {
                    CardList plains = AllZoneUtil.getPlayerLandsInPlay(card.getController());
                    plains = plains.filter(CardListFilter.UNTAPPED);

                    if (this.player.isComputer()) {
                        if (plains.size() > 1) {
                            CardList tappedPlains = new CardList(plains.toArray());
                            tappedPlains = tappedPlains.getType("Basic");
                            for (final Card c : tappedPlains) {
                                AllZone.getGameAction().sacrifice(c);
                            }
                            for (int i = 0; i < tappedPlains.size(); i++) {
                                AllZone.getGameAction().sacrifice(plains.get(i));
                            }
                            // if any are tapped, sacrifice it
                            // else sacrifice random
                        } else {
                            AllZone.getGameAction().sacrifice(card);
                        }
                    } else { // this is the human resolution
                        final int[] paid = { 0 };
                        if ((AllZoneUtil.getPlayerLandsInPlay(AllZone.getHumanPlayer()).filter(CardListFilter.UNTAPPED)
                                .size() < 2)) {
                            AllZone.getGameAction().sacrifice(card);
                            return;
                        }
                        final Input target = new Input() {
                            private static final long serialVersionUID = 6653677835621129465L;

                            @Override
                            public void showMessage() {
                                AllZone.getDisplay().showMessage(
                                        "Scorched Ruins - Select an untapped land to sacrifice");
                                ButtonUtil.enableOnlyCancel();
                            }

                            @Override
                            public void selectButtonCancel() {
                                AllZone.getGameAction().sacrifice(card);
                                this.stop();
                            }

                            @Override
                            public void selectCard(final Card c, final PlayerZone zone) {
                                if (c.isLand() && zone.is(Constant.Zone.Battlefield) && c.isUntapped()) {
                                    AllZone.getGameAction().sacrifice(c);
                                    if (paid[0] < 1) {
                                        paid[0]++;
                                        AllZone.getDisplay().showMessage(
                                                "Scorched Ruins - Select an untapped land to sacrifice");
                                    } else {
                                        this.stop();
                                    }
                                }
                            } // selectCard()
                        }; // Input
                        AllZone.getInputControl().setInput(target);
                    }
                }
            };

            card.addComesIntoPlayCommand(comesIntoPlay);
        } // *************** END ************ END **************************

        // *************** START *********** START **************************
        // Lorwyn Dual Lands, and a couple Morningtide...
        else if (cardName.equals("Ancient Amphitheater") || cardName.equals("Auntie's Hovel")
                || cardName.equals("Gilt-Leaf Palace") || cardName.equals("Secluded Glen")
                || cardName.equals("Wanderwine Hub") || cardName.equals("Rustic Clachan")
                || cardName.equals("Murmuring Bosk")) {

            String shortTemp = "";
            if (cardName.equals("Ancient Amphitheater")) {
                shortTemp = "Giant";
            }
            if (cardName.equals("Auntie's Hovel")) {
                shortTemp = "Goblin";
            }
            if (cardName.equals("Gilt-Leaf Palace")) {
                shortTemp = "Elf";
            }
            if (cardName.equals("Secluded Glen")) {
                shortTemp = "Faerie";
            }
            if (cardName.equals("Wanderwine Hub")) {
                shortTemp = "Merfolk";
            }
            if (cardName.equals("Rustic Clachan")) {
                shortTemp = "Kithkin";
            }
            if (cardName.equals("Murmuring Bosk")) {
                shortTemp = "Treefolk";
            }

            final String type = shortTemp;

            card.addComesIntoPlayCommand(new Command() {
                private static final long serialVersionUID = -5646344170306812481L;

                @Override
                public void execute() {
                    if (card.getController().isHuman()) {
                        this.humanExecute();
                    } else {
                        this.computerExecute();
                    }
                }

                public void computerExecute() {
                    CardList hand = AllZone.getComputerPlayer().getCardsIn(Zone.Hand);
                    hand = hand.getType(type);
                    if (hand.size() > 0) {
                        this.revealCard(hand.get(0));
                    } else {
                        card.tap();
                    }
                }

                public void humanExecute() {
                    AllZone.getInputControl().setInput(new Input() {
                        private static final long serialVersionUID = -2774066137824255680L;

                        @Override
                        public void showMessage() {
                            final StringBuilder sb = new StringBuilder();
                            sb.append(card.getName()).append(" - Reveal a card.");
                            AllZone.getDisplay().showMessage(sb.toString());
                            ButtonUtil.enableOnlyCancel();
                        }

                        @Override
                        public void selectCard(final Card c, final PlayerZone zone) {
                            if (zone.is(Constant.Zone.Hand) && c.isType(type)) {
                                final StringBuilder sb = new StringBuilder();
                                sb.append("Revealed card: ").append(c.getName());
                                JOptionPane.showMessageDialog(null, sb.toString(), card.getName(),
                                        JOptionPane.PLAIN_MESSAGE);
                                this.stop();
                            }
                        }

                        @Override
                        public void selectButtonCancel() {
                            card.tap();
                            this.stop();
                        }
                    });
                } // execute()

                private void revealCard(final Card c) {
                    final StringBuilder sb = new StringBuilder();
                    sb.append(c.getController()).append(" reveals ").append(c.getName());
                    JOptionPane.showMessageDialog(null, sb.toString(), card.getName(),
                            JOptionPane.PLAIN_MESSAGE);
                }
            });
        } // *************** END ************ END **************************

        // *************** START ************ START **************************
        else if (cardName.equals("Calciform Pools") || cardName.equals("Dreadship Reef")
                || cardName.equals("Fungal Reaches") || cardName.equals("Molten Slagheap")
                || cardName.equals("Saltcrusted Steppe")) {
            /*
             * tap, Remove X storage counters from Calciform Pools: Add X mana
             * in any combination of W and/or U to your mana pool.
             */
            final int[] num = new int[1];
            final int[] split = new int[1];

            String pTemp = "";
            String sTemp = "";
            if (cardName.equals("Calciform Pools")) {
                pTemp = "W";
                sTemp = "U";
            }
            if (cardName.equals("Dreadship Reef")) {
                pTemp = "U";
                sTemp = "B";
            }
            if (cardName.equals("Fungal Reaches")) {
                pTemp = "R";
                sTemp = "G";
            }
            if (cardName.equals("Molten Slagheap")) {
                pTemp = "B";
                sTemp = "R";
            }
            if (cardName.equals("Saltcrusted Steppe")) {
                pTemp = "G";
                sTemp = "W";
            }

            final String primary = pTemp;
            final String secondary = sTemp;

            final StringBuilder description = new StringBuilder();
            description.append("1, Remove X storage counters from ").append(cardName);
            description.append(": Add X mana in any combination of ").append(primary);
            description.append(" and/or ").append(secondary).append(" to your mana pool.");

            // This dummy AbMana is for Reflecting and for having an abMana
            // produce mana
            final AbilityMana abMana = new AbilityMana(card, "0", primary + " " + secondary) {
                private static final long serialVersionUID = -4506828762302357781L;

                @Override
                public boolean canPlay() {
                    return false;
                }
            };
            abMana.setUndoable(false);

            final Ability addMana = new Ability(card, "1", description.toString()) {
                // @Override
                public String mana() {
                    final StringBuilder mana = new StringBuilder();
                    for (int i = 0; i < split[0]; i++) {
                        mana.append(primary).append(" ");
                    }
                    for (int j = 0; j < (num[0] - split[0]); j++) {
                        mana.append(secondary).append(" ");
                    }
                    return mana.toString().trim();
                }

                @Override
                public boolean canPlayAI() {
                    return false;
                }

                @Override
                public void resolve() {
                    abMana.setUndoable(false);
                    abMana.produceMana(this.mana(), card.getController());
                }
            };

            final Input runtime = new Input() {
                private static final long serialVersionUID = -8808673510875540608L;

                @Override
                public void showMessage() {
                    num[0] = card.getCounters(Counters.STORAGE);
                    final String[] choices = new String[num[0] + 1];
                    for (int j = 0; j <= num[0]; j++) {
                        choices[j] = "" + j;
                    }
                    final String answer = (GuiUtils.getChoiceOptional("Storage counters to remove", choices));
                    if (answer == null) {
                        this.stop();
                        return;
                    }

                    num[0] = Integer.parseInt(answer);

                    final StringBuilder sb = new StringBuilder();
                    sb.append("Number of ").append(primary).append(" to add");
                    final String splitNum = (GuiUtils.getChoiceOptional(sb.toString(), choices));
                    if (splitNum == null) {
                        this.stop();
                        return;
                    }

                    split[0] = Integer.parseInt(splitNum);
                    if ((num[0] > 0) || (split[0] > 0)) {
                        card.subtractCounter(Counters.STORAGE, num[0]);
                        this.stop();
                        AllZone.getStack().add(addMana);
                        return;
                    }
                    this.stop();
                }
            };
            addMana.setDescription(description.toString());
            addMana.setAfterPayMana(runtime);
            card.addSpellAbility(addMana);
            card.addSpellAbility(abMana);
        } // *************** END ************ END **************************

        // *************** START *********** START **************************
        else if (cardName.equals("Crosis's Catacombs") || cardName.equals("Darigaaz's Caldera")
                || cardName.equals("Dromar's Cavern") || cardName.equals("Rith's Grove")
                || cardName.equals("Treva's Ruins")) {
            final Command comesIntoPlay = new Command() {
                private static final long serialVersionUID = 7813334062721799674L;

                @Override
                public void execute() {
                    final Player player = card.getController();
                    CardList land = AllZoneUtil.getPlayerLandsInPlay(player);
                    land = land.getNotType("Lair");

                    if (player.isComputer()) {
                        if (land.size() > 0) {
                            CardList tappedLand = new CardList(land.toArray());
                            tappedLand = tappedLand.filter(CardListFilter.TAPPED);
                            if (tappedLand.size() > 0) {
                                AllZone.getGameAction().moveToHand(CardFactoryUtil.getWorstLand(tappedLand));
                            } else {
                                AllZone.getGameAction().moveToHand(CardFactoryUtil.getWorstLand(land));
                            }
                        } else {
                            AllZone.getGameAction().sacrifice(card);
                        }
                    } else { // this is the human resolution
                        final Input target = new Input() {
                            private static final long serialVersionUID = 7944127258985401036L;

                            @Override
                            public void showMessage() {
                                final StringBuilder sb = new StringBuilder();
                                sb.append(cardName);
                                sb.append(" - Select one non-Lair land to return to your hand");
                                AllZone.getDisplay().showMessage(sb.toString());
                                ButtonUtil.enableOnlyCancel();
                            }

                            @Override
                            public void selectButtonCancel() {
                                AllZone.getGameAction().sacrifice(card);
                                this.stop();
                            }

                            @Override
                            public void selectCard(final Card c, final PlayerZone zone) {
                                if (c.isLand() && zone.is(Constant.Zone.Battlefield, AllZone.getHumanPlayer())
                                        && !c.isType("Lair")) {
                                    AllZone.getGameAction().moveToHand(c);
                                    this.stop();
                                }
                            } // selectCard()
                        }; // Input
                        AllZone.getInputControl().setInput(target);
                    }
                }
            };

            card.addComesIntoPlayCommand(comesIntoPlay);
        } // *************** END ************ END **************************

        // *************** START *********** START **************************
        else if (cardName.equals("Coral Atoll") || cardName.equals("Dormant Volcano") || cardName.equals("Everglades")
                || cardName.equals("Jungle Basin") || cardName.equals("Karoo")) {

            final String[] type = new String[1];
            if (cardName.equals("Coral Atoll")) {
                type[0] = "Island";
            } else if (cardName.equals("Dormant Volcano")) {
                type[0] = "Mountain";
            } else if (cardName.equals("Everglades")) {
                type[0] = "Swamp";
            } else if (cardName.equals("Jungle Basin")) {
                type[0] = "Forest";
            } else if (cardName.equals("Karoo")) {
                type[0] = "Plains";
            }

            final SpellAbility sacOrNo = new Ability(card, "") {
                @Override
                public void resolve() {
                    final Player player = card.getController();
                    final StringBuilder sb = new StringBuilder();
                    sb.append(type[0]).append(".untapped");
                    final CardList land = player.getCardsIn(Zone.Battlefield).getValidCards(sb.toString(),
                            player, card);

                    if (player.isComputer()) {
                        if (land.size() > 0) {
                            final Card c = CardFactoryUtil.getWorstLand(land);
                            AllZone.getGameAction().moveToHand(c);
                        } else {
                            AllZone.getGameAction().sacrifice(card);
                        }
                    } else { // this is the human resolution
                        final Input target = new Input() {
                            private static final long serialVersionUID = -7886610643693087790L;

                            @Override
                            public void showMessage() {
                                final StringBuilder sb = new StringBuilder();
                                sb.append(card).append(" - Select one untapped ");
                                sb.append(type[0]).append(" to return");
                                AllZone.getDisplay().showMessage(sb.toString());
                                ButtonUtil.enableOnlyCancel();
                            }

                            @Override
                            public void selectButtonCancel() {
                                AllZone.getGameAction().sacrifice(card);
                                this.stop();
                            }

                            @Override
                            public void selectCard(final Card c, final PlayerZone zone) {
                                if (zone.is(Constant.Zone.Battlefield) && land.contains(c)) {
                                    AllZone.getGameAction().moveToHand(c);
                                    this.stop();
                                }
                            } // selectCard()
                        }; // Input
                        AllZone.getInputControl().setInput(target);
                    }
                }
            };
            final StringBuilder sb = new StringBuilder();
            sb.append("When CARDNAME enters the battlefield, ");
            sb.append("sacrifice it unless you return an untapped ");
            sb.append(type[0]).append(" you control to its owner's hand.");
            sacOrNo.setStackDescription(sb.toString());

            final Command comesIntoPlay = new Command() {
                private static final long serialVersionUID = -5777499632266148456L;

                @Override
                public void execute() {
                    AllZone.getStack().addSimultaneousStackEntry(sacOrNo);
                }
            };

            card.addComesIntoPlayCommand(comesIntoPlay);
        } // *************** END ************ END **************************

        return card;
    }

} // end class CardFactory_Lands
