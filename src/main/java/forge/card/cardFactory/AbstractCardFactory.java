package forge.card.cardFactory;


import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import javax.swing.JOptionPane;

import forge.AllZone;
import forge.AllZoneUtil;
import forge.ButtonUtil;
import forge.Card;
import forge.CardList;
import forge.CardListFilter;
import forge.CardUtil;
import forge.Command;
import forge.ComputerUtil;
import forge.Constant;
import forge.Counters;
import forge.FileUtil;
import forge.Player;
import forge.PlayerZone;
import forge.card.abilityFactory.AbilityFactory;
import forge.card.cost.Cost;
import forge.card.spellability.*;
import forge.card.trigger.Trigger;
import forge.game.GameLossReason;
import forge.gui.GuiUtils;
import forge.gui.input.Input;
import forge.gui.input.Input_PayManaCost;
import forge.properties.ForgeProps;
import forge.properties.NewConstants;

import com.google.code.jyield.Generator;
import com.google.code.jyield.YieldUtils;

import net.slightlymagic.braids.util.ImmutableIterableFrom;
import net.slightlymagic.braids.util.generator.GeneratorFunctions;
import net.slightlymagic.braids.util.lambda.Lambda1;

import java.io.File;


/**
 * <p>AbstractCardFactory class.</p>
 *
 * TODO The map field contains Card instances that have not gone through
 * getCard2, and thus lack abilities.  However, when a new
 * Card is requested via getCard, it is this map's values that serve as
 * the templates for the values it returns.  This class has another field,
 * allCards, which is another copy of the card database.  These cards have
 * abilities attached to them, and are owned by the human player by
 * default.  <b>It would be better memory-wise if we had only one or the
 * other.</b>  We may experiment in the future with using allCard-type
 * values for the map instead of the less complete ones that exist there
 * today.
 *
 * @author Forge
 * @version $Id$
 */
public abstract class AbstractCardFactory implements NewConstants, CardFactoryInterface {
    /**
     * This maps card name Strings to Card instances. The Card instances have
     * no owner, and lack abilities.
     *
     * To get a full-fledged card, see allCards field or the iterator method.
     */
    private Map<String, Card> map = new TreeMap<String, Card>();

    /** This is a special list of cards, with all abilities attached. */
    private CardList allCards = new CardList();

    private Set<String> removedCardList;
    private Card blankCard = new Card();                 //new code

    private CardList copiedList = new CardList();

    /**
     * <p>Constructor for CardFactory.</p>
     *
     * @param file a {@link java.io.File} object.
     */
    protected AbstractCardFactory(final File file) {
        SpellAbility spell = new SpellAbility(SpellAbility.Spell, blankCard) {
            //neither computer nor human play can play this card
            @Override
            public boolean canPlay() {
                return false;
            }

            @Override
            public void resolve() {
            }
        };
        blankCard.addSpellAbility(spell);
        spell.setManaCost("1");
        blankCard.setName("Removed Card");

        //owner and controller will be wrong sometimes
        //but I don't think it will matter
        //theoretically blankCard will go to the wrong graveyard
        blankCard.setOwner(AllZone.getHumanPlayer());

        removedCardList = new TreeSet<String>(FileUtil.readFile(ForgeProps.getFile(REMOVED)));

    } // constructor


    /**
     * Getter for allCards.
     *
     * @return allCards
     */
    protected final CardList getAllCards() {
        return allCards;
    } // getAllCards()


    /**
     * Getter for map.
     *
     * @return map
     */
    protected final Map<String, Card> getMap() {
        return map;
    }


    /**
     * Iterate over all full-fledged cards in the database; these cards are
     * owned by the human player by default.
     *
     * @return an Iterator that does NOT support the remove method
     */
    @Override
    public Iterator<Card> iterator() {
        return new ImmutableIterableFrom<Card>(allCards);
    }


    /**
     * Typical size method.
     *
     * @return an estimate of the number of items encountered by this object's
     * iterator
     *
     * @see #iterator
     */
    @Override
    public final int size() {
        return allCards.size();
    }


    /**
     * <p>copyCard.</p>
     *
     * @param in a {@link forge.Card} object.
     * @return a {@link forge.Card} object.
     */
    @Override
    public final Card copyCard(final Card in) {

        Card out = getCard(in.getName(), in.getOwner());
        out.setUniqueNumber(in.getUniqueNumber());

        out.setSVars(in.getSVars());
        out.setSets(in.getSets());
        out.setCurSetCode(in.getCurSetCode());
        out.setImageFilename(in.getImageFilename());
        // I'm not sure if we really should be copying enchant/equip stuff over.
        out.setEquipping(in.getEquipping());
        out.setEquippedBy(in.getEquippedBy());
        out.setEnchantedBy(in.getEnchantedBy());
        out.setEnchanting(in.getEnchanting());
        out.setClones(in.getClones());
        out.setCounters(in.getCounters());
        
        out.clearTriggersNew();
        for(Trigger trigger : in.getTriggers()) {
            out.moveTrigger(trigger);
        }
        return out;

    }

    /**
     * <p>copyCardintoNew.</p>
     *
     * @param in a {@link forge.Card} object.
     * @return a {@link forge.Card} object.
     */
    @Override
    public final Card copyCardintoNew(final Card in) {

        Card out = CardFactoryUtil.copyStats(in);
        out.setOwner(in.getOwner());
        CardList all = new CardList(getAllCards());
        CardList tokens = AllZoneUtil.getCardsInPlay();
        tokens = tokens.filter(AllZoneUtil.token);
        all.addAll(tokens);
        out.setCopiedSpell(true);
        for (Trigger t : out.getTriggers()) {
            AllZone.getTriggerHandler().registerTrigger(t);
        }
        copiedList.add(out);
        return out;

    }

    /**
     * <p>copySpellontoStack.</p>
     *
     * @param source a {@link forge.Card} object.
     * @param original a {@link forge.Card} object.
     * @param sa a {@link forge.card.spellability.SpellAbility} object.
     * @param bCopyDetails a boolean.
     */
    @Override
    public final void copySpellontoStack(final Card source, final Card original, final SpellAbility sa,
            final boolean bCopyDetails)
    {
        if (sa.getPayCosts() == null) {
            copySpellontoStack(source, source, bCopyDetails);
            return;
        }
        Card c = AllZone.getCardFactory().copyCard(original);
        c.addController(source.getController());
        c.setCopiedSpell(true);

        SpellAbility copySA = sa.copy();
        copySA.setSourceCard(c);

        if (bCopyDetails) {
            c.addXManaCostPaid(original.getXManaCostPaid());
            c.addMultiKickerMagnitude(original.getMultiKickerMagnitude());
            if (original.isKicked()) {
                c.setKicked(true);
            }
            c.addReplicateMagnitude(original.getReplicateMagnitude());
            if (sa.isReplicate()) {
                copySA.setIsReplicate(true);
            }
        }

        if (source.getController().isHuman()) {
            AllZone.getGameAction().playSpellAbilityForFree(copySA);
        } else if (copySA.canPlayAI()) {
            ComputerUtil.playStackFree(copySA);
        }
    }


    /**
     * <p>copySpellontoStack.</p>
     *
     * @param source a {@link forge.Card} object.
     * @param original a {@link forge.Card} object.
     * @param bCopyDetails a boolean.
     */
    @Override
    public final void copySpellontoStack(final Card source, final Card original, final boolean bCopyDetails) {
        SpellAbility[] sas = original.getSpellAbility();
        SpellAbility sa = null;
        for (int i = 0; i < sas.length; i++) {
            if (original.getAbilityUsed() == i) {
                sa = sas[i];
            }
        }

        if (sa == null) {
            StringBuilder sb = new StringBuilder();
            sb.append("Couldn't find matching SpellAbility to copy Source: ").append(source);
            sb.append(" Spell to Copy: ").append(source);
            System.out.println(sb.toString());
            return;
        }

        if (sa.getPayCosts() != null) {
            copySpellontoStack(source, original, sa, bCopyDetails);
            return;
        }

        Card c = AllZone.getCardFactory().copyCardintoNew(original);

        SpellAbility copySA = null;
        for (SpellAbility s : c.getSpellAbility()) {
            if (s.equals(sa)) {
                copySA = s;
            }
        }

        if (copySA == null) {
            StringBuilder sb = new StringBuilder();
            sb.append("Couldn't find matching SpellAbility to copy Source: ").append(source);
            sb.append(" Spell to Copy: ").append(source);
            System.out.println(sb.toString());
            return;
        }

        c.addController(source.getController());
        if (bCopyDetails) {
            c.addXManaCostPaid(original.getXManaCostPaid());
            c.addMultiKickerMagnitude(original.getMultiKickerMagnitude());
            if (original.isKicked()) {
                c.setKicked(true);
            }
            c.addReplicateMagnitude(original.getReplicateMagnitude());
            if (sa.isReplicate()) {
                copySA.setIsReplicate(true);
            }

            // I have no idea what get choice does?
            if (c.hasChoices()) {
                for (int i = 0; i < original.getChoices().size(); i++) {
                    c.addSpellChoice(original.getChoice(i));
                }
                for (int i = 0; i < original.getChoiceTargets().size(); i++) {
                    c.setSpellChoiceTarget(original.getChoiceTarget(i));
                }
            }
        }

        if (sa.getTargetCard() != null) {
            copySA.setTargetCard(sa.getTargetCard());
        }

        if (sa.getTargetPlayer() != null) {
            if (sa.getTargetPlayer().isHuman()
                    || (sa.getTargetPlayer().isComputer()))
            {
                copySA.setTargetPlayer(sa.getTargetPlayer());
            }
        }

        if (source.getController().isHuman()) {
            AllZone.getGameAction().playSpellAbilityForFree(copySA);
        } else if (copySA.canPlayAI()) {
            ComputerUtil.playStackFree(copySA);
        }
    }


    /**
     * <p>getCard.</p>
     *
     * @param cardName a {@link java.lang.String} object.
     *
     * @param owner a {@link forge.Player} object.
     *
     * @return a {@link forge.Card} instance, owned by owner; or the special
     * blankCard
     *
     * @throws RuntimeException if cardName isn't in the Card map
     */
    @Override
    public final Card getCard(final String cardName, final Player owner) {
        if (removedCardList.contains(cardName) || cardName.equals(blankCard.getName())) {
            return blankCard;
        }

        return getCard2(cardName, owner);
    }

    /**
     * Fetch a random combination of cards without any duplicates.
     *
     * This algorithm is reasonably fast if numCards is small. If it is larger
     * than, say, size()/10, it starts to get noticeably slow.
     *
     * @param numCards
     *            the number of cards to return
     *
     * @return a list of fleshed-out card instances
     *
     * @throws IllegalArgumentException if numCards >= size()/4
     */
    @Override
    public final CardList getRandomCombinationWithoutRepetition(final int numCards) {
        Set<Integer> intSelections = new TreeSet<Integer>();

        if (numCards >= size()) {
            throw new IllegalArgumentException("numCards (" + numCards
                    + ") is larger than the size of the card database.");
        }
        else if (numCards >= size() / 4) {
            throw new IllegalArgumentException("numCards (" + numCards
                    + ") is too large for this algorithm; it will take too long to complete.");
        }

        while (intSelections.size() < numCards) {
            intSelections.add((int) (Math.random() * size()));
        }


        CardList result = new CardList(numCards);
        for (Integer index : intSelections) {
            result.add(allCards.get(index));
        }

        return result;
    }

    /**
     * <p>getCard2.</p>
     *
     * @param cardName a {@link java.lang.String} object.
     * @param owner a {@link forge.Player} object.
     * @return a {@link forge.Card} object.
     * @throws RuntimeException if cardName isn't in the Card map
     */
    protected Card getCard2(final String cardName, final Player owner) {
        //o should be Card object
        Object o = map.get(cardName);
        if (o == null) {
            throw new RuntimeException("CardFactory : getCard() invalid card name - " + cardName);
        }

        final Card card = CardFactoryUtil.copyStats(o);
        card.setOwner(owner);
        card.addColor(card.getManaCost());
        //may have to change the spell

        //this is so permanents like creatures and artifacts have a "default" spell
        if (card.isPermanent() && !card.isLand() && !card.isAura()) {
            card.addSpellAbility(new Spell_Permanent(card));
        }

        CardFactoryUtil.parseKeywords(card, cardName);

        //**************************************************
        // AbilityFactory cards
        ArrayList<String> ia = card.getIntrinsicAbilities();
        if (ia.size() > 0) {
            for (int i = 0; i < ia.size(); i++) {
                AbilityFactory af = new AbilityFactory();
                SpellAbility sa = af.getAbility(ia.get(i), card);

                card.addSpellAbility(sa);

                String bbCost = card.getSVar("Buyback");
                if (!bbCost.equals("")) {
                    SpellAbility bbSA = sa.copy();
                    String newCost = CardUtil.addManaCosts(card.getManaCost(), bbCost);
                    if (bbSA.getPayCosts() != null) {
                        bbSA.setPayCosts(new Cost(newCost, sa.getSourceCard().getName(), false)); // create new abCost
                    }
                    StringBuilder sb = new StringBuilder();
                    sb.append("Buyback ").append(bbCost).append(" (You may pay an additional ").append(bbCost);
                    sb.append(" as you cast this spell. If you do, put this card into your hand as it resolves.)");
                    bbSA.setDescription(sb.toString());
                    bbSA.setIsBuyBackAbility(true);

                    card.addSpellAbility(bbSA);
                }
            }

        }

        //register static abilities
        ArrayList<String> stAbs = card.getStaticAbilityStrings();
        if (stAbs.size() > 0) {
            for (int i = 0; i < stAbs.size(); i++) {
                card.addStaticAbility(stAbs.get(i));
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

        if (card2 != null) {
            return CardFactoryUtil.postFactoryKeywords(card2);
        } else if (cardName.equals("Bridge from Below")) {
            SpellAbility spell = new Spell_Permanent(card) {
                private static final long serialVersionUID = 7254358703158629514L;

                @Override
                public boolean canPlayAI() {
                    return false;
                }
            };

            // Do not remove SpellAbilities created by AbilityFactory or Keywords.
            card.clearFirstSpell();
            card.addSpellAbility(spell);
        }
        //*************** END ************ END *************************


        //*************** START *********** START **************************
        else if (cardName.equals("Sarpadian Empires, Vol. VII")) {

            final String[] choices = {"Citizen", "Camarid", "Thrull", "Goblin", "Saproling"};

            final Player player = card.getController();

            final SpellAbility ability = new Ability(card, "0") {
                @Override
                public void resolve() {
                    String type = "";
                    String imageName = "";
                    String color = "";

                    if (player.isComputer()) {
                        type = "Thrull";
                        imageName = "B 1 1 Thrull";
                        color = "B";
                    } else if (player.isHuman()) {
                        Object q = GuiUtils.getChoiceOptional("Select type of creature", choices);
                        if (q != null) {
                            if (q.equals("Citizen")) {
                                type = "Citizen";
                                imageName = "W 1 1 Citizen";
                                color = "W";
                            } else if (q.equals("Camarid")) {
                                type = "Camarid";
                                imageName = "U 1 1 Camarid";
                                color = "U";
                            } else if (q.equals("Thrull")) {
                                type = "Thrull";
                                imageName = "B 1 1 Thrull";
                                color = "B";
                            } else if (q.equals("Goblin")) {
                                type = "Goblin";
                                imageName = "R 1 1 Goblin";
                                color = "R";
                            } else if (q.equals("Saproling")) {
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
                            CardFactoryUtil.makeToken(t, in, card.getController(), col, new String[]{"Creature", t}, 1,
                                    1, new String[]{""});
                        }

                    };
                    StringBuilder sb = new StringBuilder();
                    sb.append(card.getName()).append(" - ").append(card.getController());
                    sb.append(" puts a 1/1 ").append(t).append(" token onto the battlefield");
                    a1.setStackDescription(sb.toString());

                    card.addSpellAbility(a1);
                }
            }; //ability
            Command intoPlay = new Command() {
                private static final long serialVersionUID = 7202704600935499188L;

                public void execute() {
                    ability.setStackDescription("As Sarpadian Empires, Vol. VII enters the battlefield, choose white Citizen, blue Camarid, black Thrull, red Goblin, or green Saproling.");

                    AllZone.getStack().addSimultaneousStackEntry(ability);

                }
            };
            card.setText("As Sarpadian Empires, Vol. VII enters the battlefield, "
                    + "choose white Citizen, blue Camarid, black Thrull, red Goblin, or green Saproling.\r\n"
                    + "3, Tap: Put a 1/1 creature token of the chosen color and type onto the battlefield.\r\n"
                    + card.getText()); // In the slight chance that there may be a need to add a note to this card.
            card.addComesIntoPlayCommand(intoPlay);

        } //*************** END ************ END **************************


        //*************** START *********** START **************************
        else if (cardName.equals("Night Soil")) {
            final SpellAbility nightSoil = new Ability(card, "1") {
                @Override
                public void resolve() {
                    CardFactoryUtil.makeTokenSaproling(card.getController());
                }

                @Override
                public boolean canPlayAI() {
                    return false;
                }

                @Override
                public boolean canPlay() {
                    CardList grave = AllZoneUtil.getPlayerGraveyard(AllZone.getHumanPlayer());
                    CardList aiGrave = AllZoneUtil.getPlayerGraveyard(AllZone.getComputerPlayer());
                    return (grave.getType("Creature").size() > 1 || aiGrave.getType("Creature").size() > 1)
                            && super.canPlay();
                }
            };
            Input soilTarget = new Input() {

                boolean once = false;
                private static final long serialVersionUID = 8243511353958609599L;

                @Override
                public void showMessage() {
                    CardList grave = AllZoneUtil.getPlayerGraveyard(AllZone.getHumanPlayer());
                    CardList aiGrave = AllZoneUtil.getPlayerGraveyard(AllZone.getComputerPlayer());
                    grave = grave.getType("Creature");
                    aiGrave = aiGrave.getType("Creature");

                    if (once || (grave.size() < 2 && aiGrave.size() < 2)) {
                        once = false;
                        stop();
                    } else {
                        CardList chooseGrave;
                        if (grave.size() < 2) {
                            chooseGrave = aiGrave;
                        } else if (aiGrave.size() < 2) {
                            chooseGrave = grave;
                        } else {
                            chooseGrave = aiGrave;
                            chooseGrave.addAll(grave);
                        }

                        Object o = GuiUtils.getChoice("Choose first creature to exile", chooseGrave.toArray());
                        if (o != null) {
                            CardList newGrave;
                            Card c = (Card) o;
                            if (c.getOwner().isHuman()) {
                                newGrave = AllZoneUtil.getPlayerGraveyard(AllZone.getHumanPlayer());
                            } else {
                                newGrave = AllZoneUtil.getPlayerGraveyard(AllZone.getComputerPlayer());
                            }

                            newGrave = newGrave.getType("Creature");
                            newGrave.remove(c);

                            Object o2 = GuiUtils.getChoice("Choose second creature to exile", newGrave.toArray());
                            if (o2 != null) {
                                Card c2 = (Card) o2;
                                newGrave.remove(c2);
                                AllZone.getGameAction().exile(c);
                                AllZone.getGameAction().exile(c2);
                                once = true;

                                AllZone.getStack().addSimultaneousStackEntry(nightSoil);

                            }
                        }
                    }
                    stop();
                }
            };

            nightSoil.setDescription("1, Exile two creature cards from a single graveyard: "
                    + "Put a 1/1 green Saproling creature token onto the battlefield.");

            StringBuilder sb = new StringBuilder();
            sb.append(card.getController()).append(" puts a 1/1 green Saproling creature token onto the battlefield.");
            nightSoil.setStackDescription(sb.toString());

            nightSoil.setAfterPayMana(soilTarget);
            card.addSpellAbility(nightSoil);
        } //*************** END ************ END **************************


        //*************** START *********** START **************************
        else if (cardName.equals("Necropotence")) {
            final CardList necroCards = new CardList();

            final Command necro = new Command() {
                private static final long serialVersionUID = 4511445425867383336L;

                public void execute() {
                    //put cards removed by Necropotence into the player's hand
                    if (necroCards.size() > 0) {
                        PlayerZone hand = AllZone.getZone(Constant.Zone.Hand, card.getController());

                        for (int i = 0; i < necroCards.size(); i++) {
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

                    if (library.size() != 0) {
                        Card c = library.get(0);
                        library.remove(c);

                        // TODO Necro really exiles face down, but for now we'll just do it this way
                        // c.setIsFaceDown(true);
                        // AllZone.getGameAction().exile(c);
                        necroCards.add(c); //add card to necro so that it goes into hand at end of turn
                        AllZone.getEndOfTurn().addAt(necro);
                    }
                }

                @Override
                public boolean canPlayAI() {
                    return false;
                }
            }; //SpellAbility

            ability.setDescription("Pay 1 life: Exile the top card of your library face down. " +
            		"Put that card into your hand at the beginning of your next end step.");

            StringBuilder sb = new StringBuilder();
            sb.append(card.getName());
            sb.append(" - Exile the top card of your library face down. " +
            		"Put that card into your hand at the beginning of your next end step.");
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
                    if (paid) {
                        AllZone.getStack().add(ability);
                    }
                }
            }; //Input
            ability.setBeforePayMana(payLife);

        } //*************** END ************ END **************************


        //*************** START *********** START **************************
        else if (cardName.equals("Aluren")) {
            final Ability ability1 = new Ability(card, "0") {
                @Override
                public void resolve() {
                    PlayerZone hand = AllZone.getZone(Constant.Zone.Hand, AllZone.getHumanPlayer());

                    if (hand.size() == 0) {
                        return;
                    }

                    CardList creatures = new CardList();

                    for (int i = 0; i < hand.size(); i++) {
                        if (hand.get(i).isCreature()
                                && CardUtil.getConvertedManaCost(hand.get(i).getManaCost()) <= 3)
                        {
                            creatures.add(hand.get(i));
                        }
                    }

                    if (creatures.size() == 0) {
                        return;
                    }


                    Object o = GuiUtils.getChoiceOptional("Select target creature to play",
                            creatures.toArray());
                    if (o != null) {
                        Card c = (Card) o;
                        AllZone.getStack().add(c.getSpellPermanent());
                    }


                }

                @Override
                public boolean canPlayAI() {
                    return false;

                }

                @Override
                public boolean canPlay() {
                    return (AllZone.getZone(card).is(Constant.Zone.Battlefield));
                } //canPlay()
            }; //SpellAbility ability1


            ability1.setDescription("Any player may play creature cards with converted mana cost 3 or less without "
                    + "paying their mana cost any time he or she could play an instant.");
            ability1.setStackDescription("Aluren - Play creature with converted manacost 3 or less for free.");
            ability1.getRestrictions().setAnyPlayer(true);
            card.addSpellAbility(ability1);
        } //*************** END ************ END **************************


        //*************** START *********** START **************************
        else if (cardName.equals("Volrath's Dungeon")) {

            Cost dungeonCost = new Cost("Discard<1/Card>", cardName, true);
            Target dungeonTgt = new Target(card, "Volrath's Dungeon - Target player", "player".split(","));

            final SpellAbility dungeon = new Ability_Activated(card, dungeonCost, dungeonTgt) {
                private static final long serialVersionUID = 334033015590321821L;

                @Override
                public void chooseTargetAI() {
                    setTargetPlayer(AllZone.getHumanPlayer());
                }

                @Override
                public void resolve() {
                    Player target = getTargetPlayer();
                    CardList targetHand = AllZoneUtil.getPlayerHand(target);

                    if (targetHand.size() == 0) {
                        return;
                    }

                    if (target.isHuman()) {
                        Object discard = GuiUtils.getChoice("Select Card to place on top of library.",
                                targetHand.toArray());

                        Card card = (Card) discard;
                        AllZone.getGameAction().moveToLibrary(card);
                    } else if (target.isComputer()) {
                        AllZone.getComputerPlayer().handToLibrary(1, "Top");
                    }
                }

                @Override
                public boolean canPlayAI() {
                    return AllZone.getComputerHand().size() > 0 && AllZone.getHumanHand().size() > 0 && super.canPlay();
                }

            }; //SpellAbility dungeon


            Cost bailCost = new Cost("PayLife<5>", cardName, true);
            final SpellAbility bail = new Ability_Activated(card, bailCost, null) {
                private static final long serialVersionUID = -8990402917139817175L;

                @Override
                public void resolve() {
                    AllZone.getGameAction().destroy(card);
                }

                @Override
                public boolean canPlay() {
                    return super.canPlay();
                }

                @Override
                public boolean canPlayAI() {
                    return card.getController().isHuman() && AllZone.getComputerPlayer().getLife() >= 9
                            && super.canPlay() && AllZone.getComputerHand().size() > 0;
                }

            }; //SpellAbility pay bail

            dungeon.setDescription("Discard a card: "
                    + "Target player puts a card from his or her hand on top of his or her library. "
                    + "Activate this ability only any time you could cast a sorcery.");
            dungeon.setStackDescription("CARDNAME - Target player chooses a card in hand and puts on top of library.");
            dungeon.getRestrictions().setSorcerySpeed(true);

            bail.getRestrictions().setAnyPlayer(true);
            bail.getRestrictions().setPlayerTurn(true);
            bail.setDescription("Pay 5 Life: Destroy Volrath's Dungeon. "
                    + "Any player may activate this ability but only during his or her turn.");
            bail.setStackDescription("Destroy CARDNAME.");

            card.addSpellAbility(dungeon);
            card.addSpellAbility(bail);
        } //*************** END ************ END **************************


        //*************** START *********** START **************************
        else if (cardName.equals("Mox Diamond")) {
            final Input discard = new Input() {
                private static final long serialVersionUID = -1319202902385425204L;

                @Override
                public void showMessage() {
                    AllZone.getDisplay().showMessage("Discard a land card (or select Mox Diamond to sacrifice it)");
                    ButtonUtil.enableOnlyCancel();
                }

                @Override
                public void selectCard(final Card c, final PlayerZone zone) {
                    if (zone.is(Constant.Zone.Hand) && c.isLand()) {
                        AllZone.getHumanPlayer().discard(c, null);
                        stop();
                    } else if (c.equals(card)) {
                        AllZone.getGameAction().sacrifice(card);
                        stop();
                    }
                }
            }; //Input

            final SpellAbility ability = new Ability(card, "0") {
                @Override
                public void resolve() {
                    if (card.getController().isHuman()) {
                        if (AllZone.getHumanHand().size() == 0) {
                            AllZone.getGameAction().sacrifice(card);
                        } else {
                            AllZone.getInputControl().setInput(discard);
                        }
                    } else {
                        CardList list = AllZoneUtil.getPlayerHand(AllZone.getComputerPlayer());
                        list = list.filter(new CardListFilter() {
                            public boolean addCard(final Card c) {
                                return (c.isLand());
                            }
                        });
                        AllZone.getComputerPlayer().discard(list.get(0), this);
                    } //else
                } //resolve()
            }; //SpellAbility
            Command intoPlay = new Command() {
                private static final long serialVersionUID = -7679939432259603542L;

                public void execute() {
                    ability.setStackDescription("If Mox Diamond would enter the battlefield, you may discard a land card instead. If you do, put Mox Diamond onto the battlefield. If you don't, put it into its owner's graveyard.");
                    AllZone.getStack().addSimultaneousStackEntry(ability);

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
                        public boolean addCard(final Card c) {
                            return (c.isLand());
                        }
                    });
                    return list.size() != 0 && super.canPlay();
                } //canPlay()
            };
            card.addComesIntoPlayCommand(intoPlay);
            card.clearSpellKeepManaAbility();
            card.addSpellAbility(spell);
        } //*************** END ************ END **************************


        //*************** START *********** START **************************
        else if (cardName.equals("Standstill")) {

            // Do not remove SpellAbilities created by AbilityFactory or Keywords.
            card.clearFirstSpell();

            card.addSpellAbility(new Spell_Permanent(card) {
                private static final long serialVersionUID = 6912683989507840172L;

                @Override
                public boolean canPlayAI() {
                    CardList compCreats = AllZoneUtil.getCreaturesInPlay(AllZone.getComputerPlayer());
                    CardList humCreats = AllZoneUtil.getCreaturesInPlay(AllZone.getHumanPlayer());

                    //only play standstill if comp controls more creatures than human
                    //this needs some additional rules, maybe add all power + toughness and compare
                    return (compCreats.size() > humCreats.size());
                }
            });
        } //*************** END ************ END **************************


        //*************** START *********** START **************************
        else if (cardName.equals("Goblin Charbelcher")) {
            Cost abCost = new Cost("3 T", cardName, true);
            final Ability_Activated ability = new Ability_Activated(card, abCost, new Target(card, "TgtCP")) {
                private static final long serialVersionUID = -840041589720758423L;

                @Override
                public void resolve() {
                    CardList topOfLibrary = AllZoneUtil.getPlayerCardsInLibrary(card.getController());
                    CardList revealed = new CardList();

                    if (topOfLibrary.size() == 0) {
                        return;
                    }

                    int damage = 0;
                    int count = 0;
                    Card c = null;
                    Card crd;
                    while (c == null) {
                        revealed.add(topOfLibrary.get(count));
                        crd = topOfLibrary.get(count++);
                        if (crd.isLand() || count == topOfLibrary.size()) {
                            c = crd;
                            damage = count;
                            if (crd.isLand()) {
                                damage--;
                            }

                            if (crd.getName().equals("Mountain")) {
                                damage *= 2;
                            }
                        }
                    } //while
                    GuiUtils.getChoiceOptional("Revealed cards:", revealed.toArray());

                    if (getTargetCard() != null) {
                        if (AllZoneUtil.isCardInPlay(getTargetCard())
                                && CardFactoryUtil.canTarget(card, getTargetCard()))
                        {
                            getTargetCard().addDamage(damage, card);
                        }
                    } else {
                        getTargetPlayer().addDamage(damage, card);
                    }
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
        } //*************** END ************ END **************************

        //*************** START *********** START **************************
        else if (cardName.equals("Lodestone Bauble")) {
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
                    setTargetPlayer(AllZone.getComputerPlayer());
                } //chooseTargetAI()

                @Override
                public void resolve() {
                    final int limit = 4;   //at most, this can target 4 cards
                    final Player player = getTargetPlayer();

                    CardList lands = AllZoneUtil.getPlayerGraveyard(player);
                    lands = lands.filter(AllZoneUtil.basicLands);
                    if (card.getController().isHuman()) {
                        //now, select up to four lands
                        int end = -1;
                        end = Math.min(lands.size(), limit);
                        //TODO - maybe pop a message box here that no basic lands found (if necessary)
                        for (int i = 1; i <= end; i++) {
                            String title = "Put on top of library: ";
                            if (i == 2) {
                                title = "Put second from top of library: ";
                            }
                            if (i == 3) {
                                title = "Put third from top of library: ";
                            }
                            if (i == 4) {
                                title = "Put fourth from top of library: ";
                            }
                            Object o = GuiUtils.getChoiceOptional(title, lands.toArray());
                            if (o == null) {
                                break;
                            }
                            Card c1 = (Card) o;
                            lands.remove(c1); //remove from the display list
                            AllZone.getGameAction().moveToLibrary(c1, i - 1);
                        }
                    } else { //Computer
                        //based on current AI, computer should always target himself.
                        CardList list = getComputerLands();
                        int max = list.size();
                        if (max > limit) {
                            max = limit;
                        }

                        for (int i = 0; i < max; i++) {
                            AllZone.getGameAction().moveToLibrary(list.get(i));
                        }
                    }

                    player.addSlowtripList(card);
                }

                private CardList getComputerLands() {
                    CardList list = AllZoneUtil.getPlayerGraveyard(AllZone.getComputerPlayer());
                    return list.getType("Basic");
                }
            }; //ability

            ability.setDescription(abCost + "Put up to four target basic land cards from a player's graveyard on top of his or her library in any order. That player draws a card at the beginning of the next turn's upkeep.");
            card.addSpellAbility(ability);
        } //*************** END ************ END **************************


        //*************** START *********** START **************************
        else if (cardName.equals("Grindstone")) {
            Target target = new Target(card, "Select target player", new String[]{"Player"});
            Cost abCost = new Cost("3 T", cardName, true);
            Ability_Activated ab1 = new Ability_Activated(card, abCost, target) {
                private static final long serialVersionUID = -6281219446216L;

                @Override
                public boolean canPlayAI() {
                    CardList libList = AllZoneUtil.getPlayerCardsInLibrary(AllZone.getHumanPlayer());
                    //CardList list = AllZoneUtil.getCardsInPlay("Painter's Servant");
                    return libList.size() > 0; // && list.size() > 0;
                }

                @Override
                public void resolve() {
                    Player target = getTargetPlayer();
                    CardList library = AllZoneUtil.getPlayerCardsInLibrary(getTargetPlayer());

                    boolean loop = true;
                    CardList grinding = new CardList();
                    do {
                        grinding.clear();

                        for (int i = 0; i < 2; i++) {
                            // Move current grinding to a different list
                            if (library.size() > 0) {
                                Card c = library.get(0);
                                grinding.add(c);
                                library.remove(c);
                            } else {
                                loop = false;
                                break;
                            }
                        }

                        // if current grinding dont share a color, stop grinding
                        if (loop) {
                            loop = grinding.get(0).sharesColorWith(grinding.get(1));
                        }
                        target.mill(grinding.size());
                    } while (loop);
                }
            };
            ab1.setChooseTargetAI(CardFactoryUtil.AI_targetHuman());
            ab1.setDescription(abCost + "Put the top two cards of target player's library into that player's graveyard. If both cards share a color, repeat this process.");
            card.addSpellAbility(ab1);
        } //*************** END ************ END **************************


        //*************** START *********** START **************************
        else if (cardName.equals("Everflowing Chalice")) {
            final Command comesIntoPlay = new Command() {
                private static final long serialVersionUID = 4245563898487609274L;

                public void execute() {
                    card.addCounter(Counters.CHARGE, card.getMultiKickerMagnitude());
                    card.setMultiKickerMagnitude(0);
                }
            };
            card.addComesIntoPlayCommand(comesIntoPlay);
        } //*************** END ************ END **************************


        //*************** START *********** START **************************
        else if (cardName.equals("Curse of Wizardry")) {
            final Ability ability = new Ability(card, "0") {
                @Override
                public void resolve() {
                    if (card.getController().isHuman()) {

                        String color = "";
                        String[] colors = Constant.Color.Colors;
                        colors[colors.length - 1] = null;

                        Object o = GuiUtils.getChoice("Choose color", colors);
                        color = (String) o;
                        card.setChosenColor(color);
                    } else {
                        CardList list = AllZoneUtil.getPlayerCardsInLibrary(AllZone.getHumanPlayer());
                        list.addAll(AllZoneUtil.getPlayerHand(AllZone.getHumanPlayer()));

                        if (list.size() > 0) {
                            String color = CardFactoryUtil.getMostProminentColor(list);
                            if (!color.equals("")) {
                                card.setChosenColor(color);
                            } else {
                                card.setChosenColor("black");
                            }
                        } else {
                            card.setChosenColor("black");
                        }
                    }
                }
            };
            Command comesIntoPlay = new Command() {
                private static final long serialVersionUID = -6417019967914398902L;

                public void execute() {
                    AllZone.getStack().addSimultaneousStackEntry(ability);

                }
            }; //Command

            StringBuilder sb = new StringBuilder();
            sb.append("As ").append(cardName).append(" enters the battlefield, choose a color.");
            ability.setStackDescription(sb.toString());

            card.addComesIntoPlayCommand(comesIntoPlay);
        } //*************** END ************ END **************************


        //*************** START *********** START **************************
        else if (cardName.equals("Mirror Universe")) {
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
                    return super.canPlay() && AllZone.getPhase().getPhase().equals(Constant.Phase.Upkeep)
                            && AllZone.getPhase().getPlayerTurn().equals(card.getController());
                }

                @Override
                public boolean canPlayAI() {
                    if (AllZone.getComputerPlayer().getLife() < 5 && AllZone.getHumanPlayer().getLife() > 5) {
                        return true;
                    } else if (AllZone.getComputerPlayer().getLife() == 1) {
                        return true;
                    } else {
                        return ((AllZone.getHumanPlayer().getLife() - AllZone.getComputerPlayer().getLife()) > 10);
                    }
                }
            }; //SpellAbility

            StringBuilder sb = new StringBuilder();
            sb.append(cardName).append(" - Exchange life totals with target opponent.");
            ability.setStackDescription(sb.toString());

            ability.setDescription(abCost + "Exchange life totals with target opponent. Activate this ability only during your upkeep.");
            card.addSpellAbility(ability);
        } //*************** END ************ END **************************


        //*************** START *********** START **************************
        else if (cardName.equals("Barl's Cage")) {
            final String[] tgts = {"Creature"};
            Target target = new Target(card, "Select target creature.", tgts, "1", "1");

            final Cost cost = new Cost("3", card.getName(), true);

            final SpellAbility ability = new Ability_Activated(card, cost, target) {
                private static final long serialVersionUID = 8941566961041310961L;

                @Override
                public boolean canPlayAI() {
                    Card c = getCreature();
                    if (c == null) {
                        return false;
                    } else {
                        setTargetCard(c);
                        return true;
                    }
                } //canPlayAI()

                //may return null
                public Card getCreature() {
                    CardList tappedCreatures = AllZoneUtil.getCreaturesInPlay(AllZone.getHumanPlayer());
                    tappedCreatures = tappedCreatures.filter(AllZoneUtil.tapped);
                    tappedCreatures = tappedCreatures.filter(AllZoneUtil.getCanTargetFilter(card));
                    if (tappedCreatures.isEmpty()) {
                        return null;
                    }

                    return CardFactoryUtil.AI_getBestCreature(tappedCreatures);
                }

                @Override
                public void resolve() {
                    Card target = getTargetCard();
                    if (AllZoneUtil.isCardInPlay(target)
                            && CardFactoryUtil.canTarget(card, target))
                    {
                        target.addExtrinsicKeyword("This card doesn't untap during your next untap step.");
                    } //is card in play?
                } //resolve()
            }; //SpellAbility

            card.addSpellAbility(ability);
        } //*************** END ************ END **************************


        //*************** START *********** START **************************
        else if (cardName.equals("Pithing Needle")) {
            final CardFactoryInterface factory = this;
            final SpellAbility ability = new Ability_Static(card, "0") {
                @Override
                public void resolve() {
                    final String[] input = new String[1];
                    Generator<Card> allCardsGen = YieldUtils.toGenerator(factory);
                    input[0] = "";
                    if (card.getController().isHuman()) {
                        while (input[0] == "") {
                            input[0] = JOptionPane.showInputDialog(null, "Which source?", "Pick a card", JOptionPane.QUESTION_MESSAGE);

                            final String input0 = input[0];

                            Lambda1<Boolean, Card> predicate = new Lambda1<Boolean, Card>() {
                                public Boolean apply(final Card c) {
                                    return c.getName().toLowerCase().equals(input0.toLowerCase());
                                    //System.out.print("Comparing \"" + c.getName().toLowerCase() + "\" to \"" + input[0] + "\": ");
                                    //System.out.println((c.getName().toLowerCase().equals(input[0].toLowerCase())));
                                }
                            };

                            Generator<Card> cardGen = GeneratorFunctions.filterGenerator(predicate, allCardsGen);
                            CardList cards = new CardList(cardGen);

                            if (cards.size() == 0) {
                                input[0] = "";
                            } else {
                                input[0] = cards.get(0).getName();
                            }
                        }
                        //TODO some more input validation, case-sensitivity, etc.

                    } else {
                        //AI CODE WILL EVENTUALLY GO HERE!
                    }
                    card.setSVar("PithingTarget", input[0]);
                    card.setChosenType(input[0]);
                }
            }; //ability
            ability.setStackDescription("As Pithing Needle enters the battlefield, name a card.");
            Command intoPlay = new Command() {

                private static final long serialVersionUID = 2266471224097876143L;

                public void execute() {
                    AllZone.getStack().addSimultaneousStackEntry(ability);

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
        } //*************** END ************ END **************************


        //*************** START *********** START **************************
        else if (cardName.equals("Bazaar of Wonders")) {
            /*
             * When Bazaar of Wonders enters the battlefield, exile all cards from all graveyards.
             */
            Command intoPlay = new Command() {
                private static final long serialVersionUID = 9209706681167017765L;

                public void execute() {
                    CardList hGrave = AllZoneUtil.getPlayerGraveyard(AllZone.getHumanPlayer());
                    CardList cGrave = AllZoneUtil.getPlayerGraveyard(AllZone.getComputerPlayer());

                    for (Card c : hGrave) {
                        AllZone.getGameAction().exile(c);
                    }
                    for (Card c : cGrave) {
                        AllZone.getGameAction().exile(c);
                    }
                }
            };
                                    card.addComesIntoPlayCommand(intoPlay);
        } //*************** END ************ END **************************


        //*************** START *********** START **************************
        else if (cardName.equals("Phyrexian Processor")) {
            Command intoPlay = new Command() {
                private static final long serialVersionUID = 5634360316643996274L;

                public void execute() {

                    Player player = card.getController();
                    int lifeToPay = 0;
                    if (player.isHuman()) {
                        int num = card.getController().getLife();
                        String[] choices = new String[num + 1];
                        for (int j = 0; j <= num; j++) {
                            choices[j] = "" + j;
                        }
                        String answer = (String) (GuiUtils.getChoiceOptional(
                                "Life to pay:", choices));
                        lifeToPay = Integer.parseInt(answer);
                    } else {
                        //not implemented for Compy
                    }

                    if (player.payLife(lifeToPay, card)) {
                        card.setXLifePaid(lifeToPay);
                    }

                }
            };
            card.addComesIntoPlayCommand(intoPlay);
        } //*************** END ************ END **************************


        //*************** START *********** START **************************
        else if (cardName.equals("Scroll Rack")) {
            Cost abCost = new Cost("1 T", cardName, true);
            final Ability_Activated ability = new Ability_Activated(card, abCost, null) {
                private static final long serialVersionUID = -5588587187720068547L;

                @Override
                public void resolve() {
                    //not implemented for compy
                    if (card.getController().isHuman()) {
                        AllZone.getInputControl().setInput(new Input() {
                            private static final long serialVersionUID = -2305549394512889450L;
                            CardList exiled = new CardList();

                            @Override
                            public void showMessage() {
                                AllZone.getDisplay().showMessage(card.getName() + " - Exile cards from hand.  Currently, " + exiled.size() + " selected.  (Press OK when done.)");
                                ButtonUtil.enableOnlyOK();
                            }

                            @Override
                            public void selectButtonOK() {
                                done();
                            }

                            @Override
                            public void selectCard(final Card c, final PlayerZone zone) {
                                if (zone.is(Constant.Zone.Hand, AllZone.getHumanPlayer())
                                        && !exiled.contains(c))
                                {
                                    exiled.add(c);
                                    showMessage();
                                }
                            }

                            public void done() {
                                //exile those cards
                                for (Card c : exiled) {
                                    AllZone.getGameAction().exile(c);
                                }

                                        //Put that many cards from the top of your library into your hand.
                                        //Ruling: This is not a draw...
                                        PlayerZone lib = AllZone.getZone(Constant.Zone.Library, AllZone.getHumanPlayer());
                                        int numCards = 0;
                                        while (lib.size() > 0 && numCards < exiled.size()) {
                                            AllZone.getGameAction().moveToHand(lib.get(0));
                                            numCards++;
                                        }

                                        AllZone.getDisplay().showMessage(card.getName() + " - Returning cards to top of library.");

                                        //Then look at the exiled cards and put them on top of your library in any order.
                                        while (exiled.size() > 0) {
                                            Object o = GuiUtils.getChoice("Put a card on top of your library.", exiled.toArray());
                                            Card c1 = (Card) o;
                                            AllZone.getGameAction().moveToLibrary(c1);
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
            }; //ability
            ability.setDescription(abCost + "Exile any number of cards from your hand face down. Put that many cards from the top of your library into your hand. Then look at the exiled cards and put them on top of your library in any order.");
            ability.setStackDescription(cardName + " - exile any number of cards from your hand.");
            card.addSpellAbility(ability);
        } //*************** END ************ END **************************


        //*************** START *********** START **************************
        else if (cardName.equals("Cursed Scroll")) {
            /*
             * 3, Tap: Name a card.  Reveal a card at random from your hand.  If it's the named card,
             * Cursed Scroll deals 2 damage to target creature or player.
             */
            Cost abCost = new Cost("3 T", cardName, true);
            final Ability_Activated ability = new Ability_Activated(card, abCost, new Target(card, "TgtCP")) {
                private static final long serialVersionUID = 7550743617522146304L;

                @Override
                public void resolve() {
                    Player player = card.getController();
                    String input = "";
                    if (player.isHuman()) {
                        input = JOptionPane.showInputDialog(null, "Name a card.", card.getName(), JOptionPane.QUESTION_MESSAGE);
                    } else {
                        CardList hand = AllZoneUtil.getPlayerHand(AllZone.getComputerPlayer());
                        if (!hand.isEmpty()) {
                            hand.shuffle();
                            input = hand.get(0).getName();
                        }
                    }

                    //reveal a card at random, and damage if it matches
                    CardList hand = AllZoneUtil.getPlayerHand(card.getController());
                    if (!hand.isEmpty()) {
                        hand.shuffle();
                        Card c = hand.get(0);
                        JOptionPane.showMessageDialog(null, "Revealed card:\n" + c.getName(), card.getName(), JOptionPane.PLAIN_MESSAGE);
                        if (input.equals(c.getName())) {
                            if (null != getTargetCard()) {
                                getTargetCard().addDamage(2, card);
                            } else if (null != getTargetPlayer()) {
                                getTargetPlayer().addDamage(2, card);
                            }
                        }
                    } else {
                        JOptionPane.showMessageDialog(null, "No cards to reveal.  Damage fail.", card.getName(), JOptionPane.PLAIN_MESSAGE);
                    }
                }

                @Override
                public boolean canPlayAI() {
                    return !AllZoneUtil.getPlayerHand(AllZone.getComputerPlayer()).isEmpty();
                }
            };

            StringBuilder sbStack = new StringBuilder();
            sbStack.append(card).append(" - Name a card.");
            ability.setStackDescription(sbStack.toString());

            ability.setDescription(abCost + "Name a card.  Reveal a card at random from your hand.  If it's the named card, CARDNAME deals 2 damage to target creature or player.");

            ability.setChooseTargetAI(CardFactoryUtil.AI_targetHuman());
            card.addSpellAbility(ability);
        } //*************** END ************ END **************************


        //*************** START *********** START **************************
        else if (cardName.equals("Temporal Aperture")) {
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
                    if (freeCard != null) {
                        if (freeCard.isLand()) {
                            if (player.canPlayLand()) {
                                player.playLand(freeCard);
                            } else {
                                JOptionPane.showMessageDialog(null, "You can't play any more lands this turn.", "", JOptionPane.INFORMATION_MESSAGE);
                            }
                        } else {
                            AllZone.getGameAction().playCardNoCost(freeCard);
                        }
                    } else {
                        JOptionPane.showMessageDialog(null, "Error in " + cardName + ".  freeCard is null", "", JOptionPane.INFORMATION_MESSAGE);
                    }

                }

                @Override
                public boolean canPlayAI() {
                    return false;
                }

            };
            freeCast.setDescription("Play the previously revealed top card of your library for free.");
            freeCast.setStackDescription(cardName + " - play card without paying its mana cost.");

            Cost abCost = new Cost("5 T", cardName, true);
            final Ability_Activated ability = new Ability_Activated(card, abCost, null) {
                private static final long serialVersionUID = -7328518969488588777L;

                @Override
                public void resolve() {
                    PlayerZone lib = AllZone.getZone(Constant.Zone.Library, card.getController());
                    if (lib.size() > 0) {

                        //shuffle your library
                        card.getController().shuffle();

                        //reveal the top card
                        topCard[0] = lib.get(0);
                        JOptionPane.showMessageDialog(null, "Revealed card:\n" + topCard[0].getName(), card.getName(), JOptionPane.PLAIN_MESSAGE);

                        card.addSpellAbility(freeCast);
                        card.addExtrinsicKeyword("Play with the top card of your library revealed.");
                        AllZone.getEndOfTurn().addUntil(new Command() {
                            private static final long serialVersionUID = -2860753262177388046L;

                            public void execute() {
                                card.removeSpellAbility(freeCast);
                                card.removeExtrinsicKeyword("Play with the top card of your library revealed.");
                            }
                        });
                    }
                } //resolve

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
            ability.setDescription(abCost + sbDesc.toString());

            card.addSpellAbility(ability);
        } //*************** END ************ END **************************


        //*************** START *********** START **************************
        else if (cardName.equals("Lich")) {
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

                    AllZone.getStack().addSimultaneousStackEntry(loseAllLife);

                }
            };

            final SpellAbility loseGame = new Ability(card, "0") {
                @Override
                public void resolve() {
                    card.getController().loseConditionMet(GameLossReason.SpellEffect, card.getName());
                }
            };

            Command toGrave = new Command() {
                private static final long serialVersionUID = 5863295714122376047L;

                public void execute() {

                    StringBuilder sb = new StringBuilder();
                    sb.append(cardName).append(" - ").append(card.getController());
                    sb.append("loses the game.");
                    loseGame.setStackDescription(sb.toString());

                    AllZone.getStack().addSimultaneousStackEntry(loseGame);

                }
            };

            card.addComesIntoPlayCommand(intoPlay);
            card.addDestroyCommand(toGrave);
        } //*************** END ************ END **************************


        //*************** START *********** START **************************
        else if (cardName.equals("Triangle of War")) {

            Target t2 = new Target(card, "Select target creature an opponent controls",
                    "Creature.YouDontCtrl".split(","));
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
                    if (AllZoneUtil.isCardInPlay(myc) && AllZoneUtil.isCardInPlay(oppc)) {
                        if (CardFactoryUtil.canTarget(card, myc) && CardFactoryUtil.canTarget(card, oppc)) {
                            int myPower = myc.getNetAttack();
                            int oppPower = oppc.getNetAttack();
                            myc.addDamage(oppPower, oppc);
                            oppc.addDamage(myPower, myc);
                        }
                    }
                }

                @Override
                public boolean doTrigger(final boolean b) {
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
            ability.setDescription(abCost + "Choose target creature you control and target creature an opponent controls. Each of those creatures deals damage equal to its power to the other.");
            ability.setStackDescription(card + " - Each creature deals damage equal to its power to the other.");
            card.addSpellAbility(ability);
        } //*************** END ************ END **************************


        //*************** START *********** START **************************
        else if (cardName.equals("Copy Artifact") || cardName.equals("Sculpting Steel")) {
            final CardFactoryInterface cfact = this;
            final Card[] copyTarget = new Card[1];
            final Card[] cloned = new Card[1];

            final Command leaves = new Command() {
                private static final long serialVersionUID = 6212378498863558380L;

                public void execute() {
                    //Slight hack if the cloner copies a card with triggers
                    AllZone.getTriggerHandler().removeAllFromCard(cloned[0]);

                    Card orig = cfact.getCard(card.getName(), card.getController());
                    PlayerZone dest = AllZone.getZone(card.getCurrentlyCloningCard());
                    AllZone.getGameAction().moveTo(dest, orig);
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
                        if (!arts.isEmpty()) {
                            copyTarget[0] = CardFactoryUtil.AI_getBestArtifact(arts);
                        }
                    }

                    if (copyTarget[0] != null) {
                        cloned[0] = CardFactoryUtil.copyStats(copyTarget[0]);
                        cloned[0].setOwner(card.getController());
                        if (cardName.equals("Copy Artifact")) {
                            cloned[0].addType("Enchantment");
                        }
                        cloned[0].setCloneOrigin(card);
                        cloned[0].addLeavesPlayCommand(leaves);
                        cloned[0].setCloneLeavesPlayCommand(leaves);
                        cloned[0].setCurSetCode(copyTarget[0].getCurSetCode());
                        cloned[0].setImageFilename(copyTarget[0].getImageFilename());

                        for (SpellAbility sa : copyTarget[0].getSpellAbilities()) {
                            cloned[0].addSpellAbility(sa);
                        }

                        //Slight hack in case the cloner copies a card with triggers
                        for (Trigger t : cloned[0].getTriggers()) {
                            AllZone.getTriggerHandler().registerTrigger(t);
                        }

                        AllZone.getGameAction().moveToPlay(cloned[0]);
                        card.setCurrentlyCloningCard(cloned[0]);
                    }
                }
            }; //SpellAbility

            Input runtime = new Input() {
                private static final long serialVersionUID = 8117808324791871452L;

                @Override
                public void showMessage() {
                    AllZone.getDisplay().showMessage(cardName + " - Select an artifact on the battlefield");
                    ButtonUtil.enableOnlyCancel();
                }

                @Override
                public void selectButtonCancel() {
                    stop();
                }

                @Override
                public void selectCard(final Card c, final PlayerZone z) {
                    if (z.is(Constant.Zone.Battlefield) && c.isArtifact()) {
                        copyTarget[0] = c;
                        stopSetNext(new Input_PayManaCost(copy));
                    }
                }
            };
            // Do not remove SpellAbilities created by AbilityFactory or Keywords.
            card.clearFirstSpell();
            card.addSpellAbility(copy);
            copy.setStackDescription(cardName + " - enters the battlefield as a copy of selected card.");
            copy.setBeforePayMana(runtime);
        } //*************** END ************ END **************************


        return CardFactoryUtil.postFactoryKeywords(card);
    } //getCard2

} //end class AbstractCardFactory
