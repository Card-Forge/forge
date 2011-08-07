package forge.card.cardFactory;

import forge.*;
import forge.card.spellability.*;
import forge.gui.GuiUtils;
import forge.gui.input.Input;

import javax.swing.*;

/**
 * <p>CardFactory_Lands class.</p>
 *
 * @author Forge
 * @version $Id: $
 */
class CardFactory_Lands {

    /**
     * <p>getCard.</p>
     *
     * @param card a {@link forge.Card} object.
     * @param cardName a {@link java.lang.String} object.
     * @param owner a {@link forge.Player} object.
     * @param cf a {@link forge.card.cardFactory.CardFactoryInterface} object.
     * @return a {@link forge.Card} object.
     */
    public static Card getCard(final Card card, final String cardName, Player owner, CardFactoryInterface cf) {


        //*************** START *********** START **************************
        //Ravinca Dual Lands
        if (cardName.equals("Blood Crypt") || cardName.equals("Breeding Pool")
                || cardName.equals("Godless Shrine") || cardName.equals("Hallowed Fountain")
                || cardName.equals("Overgrown Tomb") || cardName.equals("Sacred Foundry")
                || cardName.equals("Steam Vents") || cardName.equals("Stomping Ground")
                || cardName.equals("Temple Garden") || cardName.equals("Watery Grave")) {
            //if this isn't done, computer plays more than 1 copy
            //card.clearSpellAbility();
            card.clearSpellKeepManaAbility();

            card.addComesIntoPlayCommand(new Command() {
                private static final long serialVersionUID = 7352127748114888255L;

                public void execute() {
                    if (card.getController().isHuman()) {
                        humanExecute();
                    }
                    else {
                        computerExecute();
                    }
                }

                public void computerExecute() {
                    boolean pay = false;

                    if (AllZone.getComputerPlayer().getLife() > 9) {
                        pay = MyRandom.random.nextBoolean();
                    }

                    if (pay) {
                        AllZone.getComputerPlayer().loseLife(2, card);
                    }
                    else {
                        card.tap();
                    }
                }

                public void humanExecute() {
                    int life = card.getController().getLife();
                    if (2 < life) {

                        StringBuilder question = new StringBuilder();
                        question.append("Pay 2 life? If you don't, ").append(card.getName());
                        question.append(" enters the battlefield tapped.");

                        if (GameActionUtil.showYesNoDialog(card, question.toString())) {
                            AllZone.getHumanPlayer().loseLife(2, card);
                        } else {
                            tapCard();
                        }

                    }//if
                    else {
                        tapCard();
                    }
                }//execute()

                private void tapCard() {
                    card.tap();
                }
            });
        }//*************** END ************ END **************************


        /*
        //*************** START *********** START **************************
        else if (cardName.equals("Sejiri Steppe")) {
            final HashMap<Card, String[]> creatureMap = new HashMap<Card, String[]>();
            final SpellAbility[] a = new SpellAbility[1];
            final Command eot1 = new Command() {
                private static final long serialVersionUID = 5106629534549783845L;

                public void execute() {
                    Card c = a[0].getTargetCard();
                    if (AllZoneUtil.isCardInPlay(c)) {
                        String[] colors = creatureMap.get(c);
                        for (String col : colors) {
                            c.removeExtrinsicKeyword("Protection from " + col);
                        }
                    }
                }

                ;
            };

            a[0] = new Ability(card, "0") {
                @Override
                public void resolve() {
                    String Color = "";

                    if (card.getController().isHuman()) {
                        if (AllZoneUtil.isCardInPlay(getTargetCard()) && CardFactoryUtil.canTarget(card, getTargetCard())) {
                            Object o = GuiUtils.getChoice("Choose mana color", Constant.Color.onlyColors);
                            Color = (String) o;
                        }

                    } else {
                        CardList creature = AllZoneUtil.getCreaturesInPlay(AllZone.getComputerPlayer());
                        creature = creature.filter(new CardListFilter() {
                            public boolean addCard(Card c) {
                                return CardFactoryUtil.canTarget(a[0], c) && !c.hasKeyword("Defender");
                            }
                        });
                        Card biggest = null;
                        if (creature.size() > 0) {
                            biggest = creature.get(0);

                            for (int i = 0; i < creature.size(); i++) {
                                if (biggest.getNetAttack() < creature.get(i).getNetAttack()) biggest = creature.get(i);
                            }
                            setTargetCard(biggest);

                        }
                        CardList creature2 = AllZoneUtil.getCreaturesInPlay(AllZone.getHumanPlayer());
                        creature2 = creature2.filter(new CardListFilter() {
                            public boolean addCard(Card c) {
                                return (!c.isTapped() && !CardUtil.getColors(c).contains(Constant.Color.Colorless));
                            }
                        });
                        Card biggest2 = null;
                        if (creature2.size() > 0) {
                            biggest2 = creature2.get(0);
                            for (int i = 0; i < creature2.size(); i++) {
                                if (biggest2.getNetAttack() < creature2.get(i).getNetAttack())
                                    biggest2 = creature2.get(i);
                            }
                            if (biggest2 != null) {
                                if (biggest2.isGreen()) Color = "green";
                                if (biggest2.isBlue()) Color = "blue";
                                if (biggest2.isWhite()) Color = "white";
                                if (biggest2.isRed()) Color = "red";
                                if (biggest2.isBlack()) Color = "black";
                            } else {
                                Color = "black";
                            }

                        } else {
                            Color = "black";
                        }
                    }
                    Card Target = getTargetCard();
                    if (Color != "" && Target != null) Target.addExtrinsicKeyword("Protection from " + Color);
                    ;
                    if (creatureMap.containsKey(Target)) {
                        int size = creatureMap.get(Target).length;
                        String[] newString = new String[size + 1];

                        for (int i = 0; i < size; i++) {
                            newString[i] = creatureMap.get(Target)[i];
                        }
                        newString[size] = Color;
                        creatureMap.put(Target, newString);
                    } else creatureMap.put(Target, new String[]{Color});
                    AllZone.getEndOfTurn().addUntil(eot1);
                }
            };

            Command intoPlay = new Command() {
                private static final long serialVersionUID = 5055232386220487221L;

                public void execute() {
                    CardList creats = AllZoneUtil.getCreaturesInPlay(card.getController());
                    StringBuilder sb = new StringBuilder();
                    sb.append(card.getName()).append(" - target creature you control gains protection from the color of your choice until end of turn");
                    a[0].setStackDescription(sb.toString());
                    if (card.getController().isHuman()) {
                        AllZone.getInputControl().setInput(CardFactoryUtil.input_targetSpecific(a[0], creats, "Select target creature you control", false, false));
                    } else {
                        AllZone.getStack().addSimultaneousStackEntry(a[0]);

                    }
                }
            };
            card.addComesIntoPlayCommand(intoPlay);
        }//*************** END ************ END **************************
        */


        //*************** START *********** START **************************
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

                    if (card.getCounters(Counters.ICE) > 0 
                            && AllZoneUtil.isCardInPlay(card) 
                            && super.canPlay()) {
                        return true;
                    }
                    else {
                        return false;
                    }
                }

                @Override
                public boolean canPlayAI() {
                    String phase = AllZone.getPhase().getPhase();
                    return phase.equals(Constant.Phase.Main2) && super.canPlayAI();
                }

                @Override
                public void resolve() {
                    card.subtractCounter(Counters.ICE, 1);

                    if (card.getCounters(Counters.ICE) == 0) {
                        CardFactoryUtil.makeToken("Marit Lage",
                                "B 20 20 Marit Lage", card.getController(), "B", new String[]{"Legendary", "Creature", "Avatar"}, 20,
                                20, new String[]{"Flying", "Indestructible"});
                        AllZone.getGameAction().sacrifice(card);
                    }
                }
            };
            final SpellAbility sacrifice = new Ability(card, "0") {
                //TODO - this should probably be a state effect
                @Override
                public boolean canPlay() {
                    return card.getCounters(Counters.ICE) == 0 && AllZoneUtil.isCardInPlay(card) && super.canPlay();
                }

                @Override
                public boolean canPlayAI() {
                    return canPlay() && super.canPlayAI();
                }

                @Override
                public void resolve() {
                    if (card.getCounters(Counters.ICE) == 0) {
                        CardFactoryUtil.makeToken("Marit Lage",
                                "B 20 20 Marit Lage", card.getController(), "B", new String[]{"Legendary", "Creature", "Avatar"}, 20,
                                20, new String[]{"Flying", "Indestructible"});
                    }
                    AllZone.getGameAction().sacrifice(card);
                }
            };
            //ability.setDescription("Dark Depths enters the battlefield with ten ice counters on it.\r\n\r\n3: Remove an ice counter from Dark Depths.\r\n\r\nWhen Dark Depths has no ice counters on it, sacrifice it. If you do, put an indestructible legendary 20/20 black Avatar creature token with flying named Marit Lage onto the battlefield.");
            ability.setDescription("3: remove an Ice Counter.");
            StringBuilder sb = new StringBuilder();
            sb.append(card.getName()).append(" - remove an ice counter.");
            ability.setStackDescription(sb.toString());

            card.addSpellAbility(ability);
            sacrifice.setStackDescription("Sacrifice " + card.getName());
            card.addSpellAbility(sacrifice);
        }//*************** END ************ END **************************


        //*************** START *********** START **************************
        else if (cardName.equals("Novijen, Heart of Progress")) {
            card.clearSpellKeepManaAbility();

            final CardListFilter targets = new CardListFilter() {

                public boolean addCard(Card c) {
                    return AllZoneUtil.isCardInPlay(c) && c.isCreature()
                            && c.getTurnInZone() == AllZone.getPhase().getTurn();
                }
            };

            Cost abCost = new Cost("G U T", cardName, true);
            Ability_Activated ability = new Ability_Activated(card, abCost, null) {
                private static final long serialVersionUID = 1416258136308898492L;

                CardList inPlay = new CardList();

                @Override
                public boolean canPlayAI() {
                    if (!(AllZone.getPhase().getPhase().equals(Constant.Phase.Main1) 
                            && AllZone.getPhase().getPlayerTurn().isComputer())) {
                        return false;
                    }
                    inPlay.clear();
                    inPlay.addAll(AllZoneUtil.getPlayerCardsInPlay(AllZone.getComputerPlayer()));
                    return (inPlay.filter(targets).size() > 1) && super.canPlayAI();
                }

                @Override
                public void resolve() {
                    inPlay.clear();
                    inPlay.addAll(AllZoneUtil.getCardsInPlay());
                    for (Card targ : inPlay.filter(targets)) {
                        targ.addCounter(Counters.P1P1, 1);
                    }
                }
            };
            ability.setDescription(abCost + "Put a +1/+1 counter on each creature that entered the battlefield this turn.");
            ability.setStackDescription(cardName + " - Put a +1/+1 counter on each creature that entered the battlefield this turn.");
            card.addSpellAbility(ability);
        }
        //*************** END ************ END **************************


        //*************** START *********** START **************************
        else if (cardName.equals("Lotus Vale")) {
            /*
                * If Lotus Vale would enter the battlefield, sacrifice two untapped
                * lands instead. If you do, put Lotus Vale onto the battlefield.
                * If you don't, put it into its owner's graveyard.
                */
            final Command comesIntoPlay = new Command() {
                private static final long serialVersionUID = -194247993330560188L;

                final Player player = card.getController();

                public void execute() {
                    if (player.isHuman()) {
                        final int[] paid = {0};

                        Input target = new Input() {
                            private static final long serialVersionUID = -7835834281866473546L;

                            public void showMessage() {
                                AllZone.getDisplay().showMessage(cardName + " - Select an untapped land to sacrifice");
                                ButtonUtil.enableOnlyCancel();
                            }

                            public void selectButtonCancel() {
                                AllZone.getGameAction().sacrifice(card);
                                stop();
                            }

                            public void selectCard(Card c, PlayerZone zone) {
                                if (c.isLand() && zone.is(Constant.Zone.Battlefield) && c.isUntapped()) {
                                    AllZone.getGameAction().sacrifice(c);
                                    if (paid[0] < 1) {
                                        paid[0]++;
                                        AllZone.getDisplay().showMessage(cardName + " - Select an untapped land to sacrifice");
                                    } else {
                                        stop();
                                    }
                                }
                            }//selectCard()
                        };//Input
                        if ((AllZoneUtil.getPlayerLandsInPlay(AllZone.getHumanPlayer()).filter(AllZoneUtil.untapped).size() < 2)) {
                            AllZone.getGameAction().sacrifice(card);
                            return;
                        } else {
                            AllZone.getInputControl().setInput(target);
                        }
                    } else {
                        //compy can't play this card because it has no mana pool
                    }
                }
            };

            card.addComesIntoPlayCommand(comesIntoPlay);
        }//*************** END ************ END **************************


        //*************** START *********** START **************************
        else if (cardName.equals("Kjeldoran Outpost") || cardName.equals("Balduvian Trading Post")
                || cardName.equals("Heart of Yavimaya") || cardName.equals("Lake of the Dead") 
                || cardName.equals("Soldevi Excavations")) {

            final String[] type = new String[1];
            if (cardName.equals("Kjeldoran Outpost")) {
                type[0] = "Plains";
            }
            else if (cardName.equals("Balduvian Trading Post")) {
                type[0] = "Mountain.untapped";
            }
            else if (cardName.equals("Heart of Yavimaya")) {
                type[0] = "Forest";
            }
            else if (cardName.equals("Lake of the Dead")) {
                type[0] = "Swamp";
            }
            else if (cardName.equals("Soldevi Excavations")) {
                type[0] = "Island.untapped";
            }

            final Command comesIntoPlay = new Command() {
                private static final long serialVersionUID = 6175830918425915833L;
                final Player player = card.getController();

                public void execute() {
                    final CardList land = AllZoneUtil.getPlayerCardsInPlay(player).getValidCards(type[0], player, card);

                    if (player.isComputer()) {
                        if (land.size() > 0) {
                            CardList tappedLand = new CardList(land.toArray());
                            tappedLand = tappedLand.filter(AllZoneUtil.tapped);
                            //if any are tapped, sacrifice it
                            //else sacrifice random
                            if (tappedLand.size() > 0) {
                                AllZone.getGameAction().sacrifice(tappedLand.get(0));
                            } else {
                                AllZone.getGameAction().sacrifice(land.get(0));
                            }
                        } else {
                            AllZone.getGameAction().sacrifice(card);
                        }
                    } else { //this is the human resolution
                        Input target = new Input() {
                            private static final long serialVersionUID = 6653677835621129465L;

                            public void showMessage() {
                                AllZone.getDisplay().showMessage(cardName + " - Select one " + type[0] + " to sacrifice");
                                ButtonUtil.enableOnlyCancel();
                            }

                            public void selectButtonCancel() {
                                AllZone.getGameAction().sacrifice(card);
                                stop();
                            }

                            public void selectCard(Card c, PlayerZone zone) {
                                if (c.isLand() && zone.is(Constant.Zone.Battlefield) && land.contains(c)) {
                                    AllZone.getGameAction().sacrifice(c);
                                    stop();
                                }
                            }//selectCard()
                        };//Input
                        AllZone.getInputControl().setInput(target);
                    }
                }
            };

            card.addComesIntoPlayCommand(comesIntoPlay);
        }//*************** END ************ END **************************


        //*************** START *********** START **************************
        else if (cardName.equals("Sheltered Valley")) {

            final Command comesIntoPlay = new Command() {
                private static final long serialVersionUID = 685604326470832887L;

                public void execute() {
                    final Player player = card.getController();
                    CardList land = AllZoneUtil.getPlayerCardsInPlay(player, "Sheltered Valley");
                    land.remove(card);

                    if (land.size() > 0) {
                        for (Card c : land) {
                            AllZone.getGameAction().sacrifice(c);
                        }
                    }
                }
            };

            card.addComesIntoPlayCommand(comesIntoPlay);
        }//*************** END ************ END **************************


        //*************** START *********** START **************************
        else if (cardName.equals("Scorched Ruins")) {
            final Command comesIntoPlay = new Command() {
                private static final long serialVersionUID = 6175830918425915833L;
                final Player player = card.getController();

                public void execute() {
                    CardList plains = AllZoneUtil.getPlayerLandsInPlay(card.getController());
                    plains = plains.filter(AllZoneUtil.untapped);

                    if (player.isComputer()) {
                        if (plains.size() > 1) {
                            CardList tappedPlains = new CardList(plains.toArray());
                            tappedPlains = tappedPlains.getType("Basic");
                            for (Card c : tappedPlains) {
                                AllZone.getGameAction().sacrifice(c);
                            }
                            for (int i = 0; i < tappedPlains.size(); i++) {
                                AllZone.getGameAction().sacrifice(plains.get(i));
                            }
                            //if any are tapped, sacrifice it
                            //else sacrifice random
                        } else {
                            AllZone.getGameAction().sacrifice(card);
                        }
                    } else { //this is the human resolution
                        final int[] paid = {0};
                        if ((AllZoneUtil.getPlayerLandsInPlay(AllZone.getHumanPlayer()).filter(AllZoneUtil.untapped).size() < 2)) {
                            AllZone.getGameAction().sacrifice(card);
                            return;
                        }
                        Input target = new Input() {
                            private static final long serialVersionUID = 6653677835621129465L;

                            public void showMessage() {
                                AllZone.getDisplay().showMessage("Scorched Ruins - Select an untapped land to sacrifice");
                                ButtonUtil.enableOnlyCancel();
                            }

                            public void selectButtonCancel() {
                                AllZone.getGameAction().sacrifice(card);
                                stop();
                            }

                            public void selectCard(Card c, PlayerZone zone) {
                                if (c.isLand() && zone.is(Constant.Zone.Battlefield) && c.isUntapped()) {
                                    AllZone.getGameAction().sacrifice(c);
                                    if (paid[0] < 1) {
                                        paid[0]++;
                                        AllZone.getDisplay().showMessage("Scorched Ruins - Select an untapped land to sacrifice");
                                    } else {
                                        stop();
                                    }
                                }
                            }//selectCard()
                        };//Input
                        AllZone.getInputControl().setInput(target);
                    }
                }
            };

            card.addComesIntoPlayCommand(comesIntoPlay);
        }//*************** END ************ END **************************


        //*************** START ************ START **************************
        else if (cardName.equals("Bottomless Vault") || cardName.equals("Dwarven Hold")
                || cardName.equals("Hollow Trees") || cardName.equals("Icatian Store")
                || cardName.equals("Sand Silos")) {
            final int[] num = new int[1];
            String shortTemp = "";
            if (cardName.equals("Bottomless Vault")) {
                shortTemp = "B";
            }
            if (cardName.equals("Dwarven Hold")) {
                shortTemp = "R";
            }
            if (cardName.equals("Hollow Trees")) {
                shortTemp = "G";
            }
            if (cardName.equals("Icatian Store")) {
                shortTemp = "W";
            }
            if (cardName.equals("Sand Silos")) {
                shortTemp = "U";
            }

            final String shortString = shortTemp;
            StringBuilder desc = new StringBuilder();
            desc.append("tap, Remove any number of storage counters from ").append(cardName);
            desc.append(": Add ").append(shortString);
            desc.append(" to your mana pool for each charge counter removed this way.");

            final Ability_Mana abMana = new Ability_Mana(card, "0", shortString) {
                private static final long serialVersionUID = -4506828762302357781L;

                @Override
                public boolean canPlay() {
                    return false;
                }
            };
            abMana.setUndoable(false);

            final Ability addMana = new Ability(card, "0", desc.toString()) {
                private static final long serialVersionUID = -7805885635696245285L;

                //@Override
                public String mana() {
                    StringBuilder mana = new StringBuilder();
                    if (num[0] == 0) {
                        mana.append("0");
                    }
                    else {
                        for (int i = 0; i < num[0]; i++) {
                            mana.append(shortString).append(" ");
                        }
                    }
                    return mana.toString().trim();
                }

                @Override
                public boolean canPlayAI() {
                    return false;
                }

                @Override
                public void resolve() {
                    abMana.produceMana(mana(), card.getController());
                }
            };

            Input runtime = new Input() {
                private static final long serialVersionUID = -4990369861806627183L;

                @Override
                public void showMessage() {
                    num[0] = card.getCounters(Counters.STORAGE);
                    String[] choices = new String[num[0] + 1];
                    for (int j = 0; j <= num[0]; j++) {
                        choices[j] = "" + j;
                    }
                    String answer = (String) (GuiUtils.getChoiceOptional("Storage counters to remove", choices));
                    if (null != answer && !answer.equals("")) {
                        num[0] = Integer.parseInt(answer);
                        card.tap();
                        card.subtractCounter(Counters.STORAGE, num[0]);
                        stop();
                        AllZone.getStack().add(addMana);
                        return;
                    }
                    stop();
                }
            };

            addMana.setDescription(desc.toString());
            addMana.setBeforePayMana(runtime);
            card.addSpellAbility(addMana);
            card.addSpellAbility(abMana);
        }//*************** END ************ END **************************


        //*************** START *********** START **************************
        //Lorwyn Dual Lands, and a couple Morningtide...
        else if (cardName.equals("Ancient Amphitheater") || cardName.equals("Auntie's Hovel")
                || cardName.equals("Gilt-Leaf Palace") || cardName.equals("Secluded Glen")
                || cardName.equals("Wanderwine Hub")
                || cardName.equals("Rustic Clachan") || cardName.equals("Murmuring Bosk")) {

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

                public void execute() {
                    if (card.getController().isHuman()) {
                        humanExecute();
                    }
                    else {
                        computerExecute();
                    }
                }

                public void computerExecute() {
                    CardList hand = AllZoneUtil.getPlayerHand(AllZone.getComputerPlayer());
                    hand = hand.filter(AllZoneUtil.getTypeFilter(type));
                    if (hand.size() > 0) {
                        revealCard(hand.get(0));
                    }
                    else {
                        card.tap();
                    }
                }

                public void humanExecute() {
                    AllZone.getInputControl().setInput(new Input() {
                        private static final long serialVersionUID = -2774066137824255680L;

                        @Override
                        public void showMessage() {
                            AllZone.getDisplay().showMessage(card.getName() + " - Reveal a card.");
                            ButtonUtil.enableOnlyCancel();
                        }

                        @Override
                        public void selectCard(Card c, PlayerZone zone) {
                            if (zone.is(Constant.Zone.Hand) && c.isType(type)) {
                                JOptionPane.showMessageDialog(null, "Revealed card: " + c.getName(), card.getName(), JOptionPane.PLAIN_MESSAGE);
                                stop();
                            }
                        }

                        @Override
                        public void selectButtonCancel() {
                            card.tap();
                            stop();
                        }
                    });
                }//execute()

                private void revealCard(Card c) {
                    JOptionPane.showMessageDialog(null, c.getController() + " reveals " + c.getName(), card.getName(), JOptionPane.PLAIN_MESSAGE);
                }
            });
        }//*************** END ************ END **************************


        //*************** START ************ START **************************
        else if (cardName.equals("Calciform Pools") || cardName.equals("Dreadship Reef") 
                || cardName.equals("Fungal Reaches") || cardName.equals("Molten Slagheap") 
                || cardName.equals("Saltcrusted Steppe")) {
                /*
                * tap, Remove X storage counters from Calciform Pools: Add X mana in any combination of W and/or U to your mana pool.
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

            StringBuilder description = new StringBuilder();
            description.append("1, Remove X storage counters from ").append(cardName);
            description.append(": Add X mana in any combination of ").append(primary);
            description.append(" and/or ").append(secondary).append(" to your mana pool.");

            // This dummy AbMana is for Reflecting and for having an abMana produce mana
            final Ability_Mana abMana = new Ability_Mana(card, "0", primary + " " + secondary) {
                private static final long serialVersionUID = -4506828762302357781L;

                @Override
                public boolean canPlay() {
                    return false;
                }
            };
            abMana.setUndoable(false);

            final Ability addMana = new Ability(card, "1", description.toString()) {
                private static final long serialVersionUID = 7177960799748450242L;

                //@Override
                public String mana() {
                    StringBuilder mana = new StringBuilder();
                    for (int i = 0; i < split[0]; i++) {
                        mana.append(primary).append(" ");
                    }
                    for (int j = 0; j < num[0] - split[0]; j++) {
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
                    abMana.produceMana(mana(), card.getController());
                }
            };

            Input runtime = new Input() {
                private static final long serialVersionUID = -8808673510875540608L;

                @Override
                public void showMessage() {
                    num[0] = card.getCounters(Counters.STORAGE);
                    String[] choices = new String[num[0] + 1];
                    for (int j = 0; j <= num[0]; j++) {
                        choices[j] = "" + j;
                    }
                    String answer = (String) (GuiUtils.getChoiceOptional(
                            "Storage counters to remove", choices));
                    if (answer == null) {
                        stop();
                        return;
                    }

                    num[0] = Integer.parseInt(answer);

                    String splitNum = (String) (GuiUtils.getChoiceOptional(
                            "Number of " + primary + " to add", choices));
                    if (splitNum == null) {
                        stop();
                        return;
                    }

                    split[0] = Integer.parseInt(splitNum);
                    if (num[0] > 0 || split[0] > 0) {
                        card.subtractCounter(Counters.STORAGE, num[0]);
                        stop();
                        AllZone.getStack().add(addMana);
                        return;
                    }
                    stop();
                }
            };
            addMana.setDescription(description.toString());
            addMana.setAfterPayMana(runtime);
            card.addSpellAbility(addMana);
            card.addSpellAbility(abMana);
        }//*************** END ************ END **************************


        //*************** START *********** START **************************
        else if (cardName.equals("Crosis's Catacombs") || cardName.equals("Darigaaz's Caldera") 
                || cardName.equals("Dromar's Cavern") || cardName.equals("Rith's Grove") 
                || cardName.equals("Treva's Ruins")) {
            final Command comesIntoPlay = new Command() {
                private static final long serialVersionUID = 7813334062721799674L;

                public void execute() {
                    final Player player = card.getController();
                    CardList land = AllZoneUtil.getPlayerLandsInPlay(player);
                    land = land.getNotType("Lair");

                    if (player.isComputer()) {
                        if (land.size() > 0) {
                            CardList tappedLand = new CardList(land.toArray());
                            tappedLand = tappedLand.filter(AllZoneUtil.tapped);
                            if (tappedLand.size() > 0) {
                                AllZone.getGameAction().moveToHand(CardFactoryUtil.getWorstLand(tappedLand));
                            } else {
                                AllZone.getGameAction().moveToHand(CardFactoryUtil.getWorstLand(land));
                            }
                        } else {
                            AllZone.getGameAction().sacrifice(card);
                        }
                    } else { //this is the human resolution
                        Input target = new Input() {
                            private static final long serialVersionUID = 7944127258985401036L;

                            public void showMessage() {
                                AllZone.getDisplay().showMessage(cardName + " - Select one non-Lair land to return to your hand");
                                ButtonUtil.enableOnlyCancel();
                            }

                            public void selectButtonCancel() {
                                AllZone.getGameAction().sacrifice(card);
                                stop();
                            }

                            public void selectCard(Card c, PlayerZone zone) {
                                if (c.isLand()
                                        && zone.is(Constant.Zone.Battlefield, AllZone.getHumanPlayer())
                                        && !c.isType("Lair")) {
                                    AllZone.getGameAction().moveToHand(c);
                                    stop();
                                }
                            }//selectCard()
                        };//Input
                        AllZone.getInputControl().setInput(target);
                    }
                }
            };

            card.addComesIntoPlayCommand(comesIntoPlay);
        }//*************** END ************ END **************************


        //*************** START *********** START **************************
        else if (cardName.equals("Coral Atoll") || cardName.equals("Dormant Volcano")
                || cardName.equals("Everglades") || cardName.equals("Jungle Basin")
                || cardName.equals("Karoo")) {

            final String[] type = new String[1];
            if (cardName.equals("Coral Atoll")) {
                type[0] = "Island";
            }
            else if (cardName.equals("Dormant Volcano")) {
                type[0] = "Mountain";
            }
            else if (cardName.equals("Everglades")) {
                type[0] = "Swamp";
            }
            else if (cardName.equals("Jungle Basin")) {
                type[0] = "Forest";
            }
            else if (cardName.equals("Karoo")) {
                type[0] = "Plains";
            }

            final SpellAbility sacOrNo = new Ability(card, "") {
                @Override
                public void resolve() {
                    final Player player = card.getController();
                    final CardList land = AllZoneUtil.getPlayerCardsInPlay(player).getValidCards(type[0] + ".untapped", player, card);

                    if (player.isComputer()) {
                        if (land.size() > 0) {
                            Card c = CardFactoryUtil.getWorstLand(land);
                            AllZone.getGameAction().moveToHand(c);
                        } else {
                            AllZone.getGameAction().sacrifice(card);
                        }
                    } else { //this is the human resolution
                        Input target = new Input() {
                            private static final long serialVersionUID = -7886610643693087790L;

                            public void showMessage() {
                                AllZone.getDisplay().showMessage(card + " - Select one untapped " + type[0] + " to return");
                                ButtonUtil.enableOnlyCancel();
                            }

                            public void selectButtonCancel() {
                                AllZone.getGameAction().sacrifice(card);
                                stop();
                            }

                            public void selectCard(Card c, PlayerZone zone) {
                                if (zone.is(Constant.Zone.Battlefield) && land.contains(c)) {
                                    AllZone.getGameAction().moveToHand(c);
                                    stop();
                                }
                            }//selectCard()
                        };//Input
                        AllZone.getInputControl().setInput(target);
                    }
                }
            };
            sacOrNo.setStackDescription("When CARDNAME enters the battlefield, sacrifice it unless you return an untapped " + type[0] + " you control to its owner's hand.");

            final Command comesIntoPlay = new Command() {
                private static final long serialVersionUID = -5777499632266148456L;

                public void execute() {
                    AllZone.getStack().addSimultaneousStackEntry(sacOrNo);
                }
            };

            card.addComesIntoPlayCommand(comesIntoPlay);
        }//*************** END ************ END **************************

        return card;
    }

}//end class CardFactory_Lands
