/*
 * Forge: Play Magic: the Gathering.
 * Copyright (C) 2011  Forge Team
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package forge.card.cardfactory;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.swing.JOptionPane;

import forge.AllZone;
import forge.AllZoneUtil;
import forge.Card;
import forge.CardCharactersticName;
import forge.CardList;
import forge.CardListFilter;
import forge.CardUtil;
import forge.Command;
import forge.Counters;
import forge.GameActionUtil;
import forge.Singletons;
import forge.card.cost.Cost;
import forge.card.spellability.Ability;
import forge.card.spellability.AbilityActivated;
import forge.card.spellability.Spell;
import forge.card.spellability.SpellAbility;
import forge.card.spellability.SpellPermanent;
import forge.card.spellability.Target;
import forge.card.trigger.Trigger;
import forge.card.trigger.TriggerType;
import forge.control.input.Input;
import forge.control.input.InputPayManaCost;
import forge.game.GameLossReason;
import forge.game.player.ComputerUtil;
import forge.game.player.Player;
import forge.game.zone.PlayerZone;
import forge.game.zone.ZoneType;
import forge.gui.GuiUtils;
import forge.properties.ForgeProps;
import forge.properties.NewConstants;
import forge.util.FileUtil;
import forge.view.ButtonUtil;

/**
 * <p>
 * AbstractCardFactory class.
 * </p>
 * 
 * TODO The map field contains Card instances that have not gone through
 * getCard2, and thus lack abilities. However, when a new Card is requested via
 * getCard, it is this map's values that serve as the templates for the values
 * it returns. This class has another field, allCards, which is another copy of
 * the card database. These cards have abilities attached to them, and are owned
 * by the human player by default. <b>It would be better memory-wise if we had
 * only one or the other.</b> We may experiment in the future with using
 * allCard-type values for the map instead of the less complete ones that exist
 * there today.
 * 
 * @author Forge
 * @version $Id$
 */
public abstract class AbstractCardFactory implements CardFactoryInterface {
    /**
     * This maps card name Strings to Card instances. The Card instances have no
     * owner, and lack abilities.
     * 
     * To get a full-fledged card, see allCards field or the iterator method.
     */
    private final Map<String, Card> map = new TreeMap<String, Card>();

    /** This is a special list of cards, with all abilities attached. */
    protected List<Card> allCardsReadOnly;

    private Set<String> removedCardList;
    private final Card blankCard = new Card(); // new code

    private final CardList copiedList = new CardList();

    /**
     * <p>
     * Constructor for CardFactory.
     * </p>
     * 
     * @param file
     *            a {@link java.io.File} object.
     */
    protected AbstractCardFactory(final File file) {
        final SpellAbility spell = new SpellAbility(SpellAbility.getSpell(), this.blankCard) {
            // neither computer nor human play can play this card
            @Override
            public boolean canPlay() {
                return false;
            }

            @Override
            public void resolve() {
            }
        };
        this.blankCard.addSpellAbility(spell);
        spell.setManaCost("1");
        this.blankCard.setName("Removed Card");

        // owner and controller will be wrong sometimes
        // but I don't think it will matter
        // theoretically blankCard will go to the wrong graveyard
        this.blankCard.setOwner(AllZone.getHumanPlayer());

        this.removedCardList = new TreeSet<String>(FileUtil.readFile(ForgeProps.getFile(NewConstants.REMOVED)));

    } // constructor

    /**
     * Getter for allCards.
     * 
     * @return allCards
     */
    protected abstract List<Card> getAllCards();

    /**
     * Getter for map.
     * 
     * @return map
     */
    protected final Map<String, Card> getMap() {
        return this.map;
    }

    /**
     * Iterate over all full-fledged cards in the database; these cards are
     * owned by the human player by default.
     * 
     * @return an Iterator that does NOT support the remove method
     */
    @Override
    public Iterator<Card> iterator() {
        if (allCardsReadOnly == null) {

            allCardsReadOnly = Collections.unmodifiableList(getAllCards());
        }
        return allCardsReadOnly.iterator();
    }

    /**
     * <p>
     * copyCard.
     * </p>
     * 
     * @param in
     *            a {@link forge.Card} object.
     * @return a {@link forge.Card} object.
     */
    @Override
    public final Card copyCard(final Card in) {
        final CardCharactersticName curState = in.getCurState();
        AllZone.getTriggerHandler().suppressMode(TriggerType.Transformed);
        if (in.isInAlternateState()) {
            in.setState(CardCharactersticName.Original);
        }
        final Card out = this.getCard(in.getName(), in.getOwner());
        out.setUniqueNumber(in.getUniqueNumber());
        out.setCurSetCode(in.getCurSetCode());

        CardFactoryUtil.copyCharacteristics(in, out);
        if (in.hasAlternateState()) {
            for (final CardCharactersticName state : in.getStates()) {
                in.setState(state);
                out.setState(state);
                CardFactoryUtil.copyCharacteristics(in, out);
            }
            in.setState(curState);
            out.setState(curState);
        }
        AllZone.getTriggerHandler().clearSuppression(TriggerType.Transformed);

        // I'm not sure if we really should be copying enchant/equip stuff over.
        out.setEquipping(in.getEquipping());
        out.setEquippedBy(in.getEquippedBy());
        out.setEnchantedBy(in.getEnchantedBy());
        out.setEnchanting(in.getEnchanting());
        out.setClones(in.getClones());
        for (final Object o : in.getRemembered()) {
            out.addRemembered(o);
        }
        for (final Card o : in.getImprinted()) {
            out.addImprinted(o);
        }

        return out;

    }

    /**
     * <p>
     * copyCardintoNew.
     * </p>
     * 
     * @param in
     *            a {@link forge.Card} object.
     * @return a {@link forge.Card} object.
     */
    @Override
    public final Card copyCardintoNew(final Card in) {
        final Card out = CardFactoryUtil.copyStats(in);
        out.setOwner(in.getOwner());
        out.setCopiedSpell(true);
        this.copiedList.add(out);
        return out;
    }

    /**
     * <p>
     * copySpellontoStack.
     * </p>
     * 
     * @param source
     *            a {@link forge.Card} object.
     * @param original
     *            a {@link forge.Card} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @param bCopyDetails
     *            a boolean.
     */
    @Override
    public final void copySpellontoStack(final Card source, final Card original, final SpellAbility sa,
            final boolean bCopyDetails) {
        Player controller = sa.getActivatingPlayer();
        if (sa.getPayCosts() == null) {
            this.copySpellontoStack(source, source, bCopyDetails);
            return;
        }
        final Card c = AllZone.getCardFactory().copyCard(original);
        c.addController(controller);
        c.setCopiedSpell(true);

        final SpellAbility copySA = sa.copy();
        if (sa.getTarget() != null) {
            Target target = new Target(sa.getTarget());
            target.setSourceCard(c);
            copySA.setTarget(target);
            /*if (copySA.getAbilityFactory() != null) {
                AbilityFactory af = new AbilityFactory(sa.getAbilityFactory());
                af.setAbTgt(target);
                af.setHostCard(source);
                copySA.setAbilityFactory(af);
            }*/
        }
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

        if (controller.isHuman()) {
            Singletons.getModel().getGameAction().playSpellAbilityForFree(copySA);
        } else if (copySA.canPlayAI()) {
            ComputerUtil.playStackFree(copySA);
        }
    }

    /**
     * <p>
     * copySpellontoStack.
     * </p>
     * 
     * @param source
     *            a {@link forge.Card} object.
     * @param original
     *            a {@link forge.Card} object.
     * @param bCopyDetails
     *            a boolean.
     */
    @Override
    public final void copySpellontoStack(final Card source, final Card original, final boolean bCopyDetails) {
        final SpellAbility[] sas = original.getSpellAbility();
        SpellAbility sa = null;
        for (int i = 0; i < sas.length; i++) {
            if (original.getAbilityUsed() == i) {
                sa = sas[i];
            }
        }

        if (sa == null) {
            final StringBuilder sb = new StringBuilder();
            sb.append("Couldn't find matching SpellAbility to copy Source: ").append(source);
            sb.append(" Spell to Copy: ").append(source);
            System.out.println(sb.toString());
            return;
        }

        if (sa.getPayCosts() != null) {
            this.copySpellontoStack(source, original, sa, bCopyDetails);
            return;
        }

        final Card c = AllZone.getCardFactory().copyCardintoNew(original);

        SpellAbility copySA = null;
        for (final SpellAbility s : c.getSpellAbility()) {
            if (s.equals(sa)) {
                copySA = s;
            }
        }

        if (copySA == null) {
            final StringBuilder sb = new StringBuilder();
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
            if (sa.getTargetPlayer().isHuman() || (sa.getTargetPlayer().isComputer())) {
                copySA.setTargetPlayer(sa.getTargetPlayer());
            }
        }

        if (source.getController().isHuman()) {
            Singletons.getModel().getGameAction().playSpellAbilityForFree(copySA);
        } else if (copySA.canPlayAI()) {
            ComputerUtil.playStackFree(copySA);
        }
    }

    /**
     * <p>
     * getCard.
     * </p>
     * 
     * @param cardName
     *            a {@link java.lang.String} object.
     * @param owner
     *            a {@link forge.game.player.Player} object.
     * @return a {@link forge.Card} instance, owned by owner; or the special
     *         blankCard
     */
    @Override
    public final Card getCard(final String cardName, final Player owner) {
        if (this.removedCardList.contains(cardName) || cardName.equals(this.blankCard.getName())) {
            return this.blankCard;
        }

        //System.out.println(cardName);
        return this.getCard2(cardName, owner);
    }

    protected Card getCard2(final String cardName, final Player owner) {
        // o should be Card object
        final Card o = this.map.get(cardName);
        if (o == null) {
            final StringBuilder sb = new StringBuilder();
            sb.append("CardFactory : getCard() invalid card name - ").append(cardName);
            throw new RuntimeException(sb.toString());
        }

        return getCard2(o, owner);
    }

    public static Card getCard2(final Card o, final Player owner) {
        final Card copy = CardFactoryUtil.copyStats(o);
        copy.setOwner(owner);
        return buildAbilities(copy);
    }

    public static Card buildAbilities(final Card card) {
        final String cardName = card.getName();

        if (!card.isCardColorsOverridden()) {
            card.addColor(card.getManaCost());
        }
        // may have to change the spell

        // this is so permanents like creatures and artifacts have a "default"
        // spell
        if (card.isPermanent() && !card.isLand() && !card.isAura()) {
            card.addSpellAbility(new SpellPermanent(card));
        }

        CardFactoryUtil.parseKeywords(card, cardName);

        for (final CardCharactersticName state : card.getStates()) {
            if (card.isDoubleFaced() && state == CardCharactersticName.FaceDown) {
                continue; // Ignore FaceDown for DFC since they have none.
            }
            card.setState(state);
            CardFactoryUtil.addAbilityFactoryAbilities(card);
            final ArrayList<String> stAbs = card.getStaticAbilityStrings();
            if (stAbs.size() > 0) {
                for (int i = 0; i < stAbs.size(); i++) {
                    card.addStaticAbility(stAbs.get(i));
                }
            }
        }

        card.setState(CardCharactersticName.Original);

        // ******************************************************************
        // ************** Link to different CardFactories *******************
        Card card2 = null;
        if (card.isCreature()) {
            card2 = CardFactoryCreatures.getCard(card, cardName);
        } else if (card.isAura()) {
            card2 = CardFactoryAuras.getCard(card, cardName);
        } else if (card.isEquipment()) {
            card2 = CardFactoryEquipment.getCard(card, cardName);
        } else if (card.isPlaneswalker()) {
            card2 = CardFactoryPlaneswalkers.getCard(card, cardName);
        } else if (card.isLand()) {
            card2 = CardFactoryLands.getCard(card, cardName);
        } else if (card.isInstant()) {
            card2 = CardFactoryInstants.getCard(card, cardName);
        } else if (card.isSorcery()) {
            card2 = CardFactorySorceries.getCard(card, cardName);
        }

        if (card2 != null) {
            return CardFactoryUtil.postFactoryKeywords(card2);
        } else if (cardName.equals("Bridge from Below")) {
            final SpellAbility spell = new SpellPermanent(card) {
                private static final long serialVersionUID = 7254358703158629514L;

                @Override
                public boolean canPlayAI() {
                    return false;
                }
            };

            // Do not remove SpellAbilities created by AbilityFactory or
            // Keywords.
            card.clearFirstSpell();
            card.addSpellAbility(spell);
        }
        // *************** END ************ END *************************

        // *************** START *********** START **************************
        else if (cardName.equals("Sarpadian Empires, Vol. VII")) {

            final String[] choices = { "Citizen", "Camarid", "Thrull", "Goblin", "Saproling" };

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
                        final Object q = GuiUtils.chooseOneOrNone("Select type of creature", choices);
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
                    // card.setChosenType(input[0]);

                    final Cost a1Cost = new Cost(card, "3 T", true);
                    final AbilityActivated a1 = new AbilityActivated(card, a1Cost, null) {

                        private static final long serialVersionUID = -2114111483117171609L;

                        @Override
                        public void resolve() {
                            CardFactoryUtil.makeToken(t, in, card.getController(), col, new String[] { "Creature", t },
                                    1, 1, new String[] { "" });
                        }

                    };
                    final StringBuilder sb = new StringBuilder();
                    sb.append(card.getName()).append(" - ").append(card.getController());
                    sb.append(" puts a 1/1 ").append(t).append(" token onto the battlefield");
                    a1.setStackDescription(sb.toString());

                    card.addSpellAbility(a1);
                }
            }; // ability
            final Command intoPlay = new Command() {
                private static final long serialVersionUID = 7202704600935499188L;

                @Override
                public void execute() {
                    final StringBuilder sb = new StringBuilder();
                    sb.append("As Sarpadian Empires, Vol. VII enters the battlefield, ");
                    sb.append("choose white Citizen, blue Camarid, black Thrull, red Goblin, or green Saproling.");
                    ability.setStackDescription(sb.toString());

                    AllZone.getStack().addSimultaneousStackEntry(ability);

                }
            };
            final StringBuilder sb = new StringBuilder();
            sb.append("As Sarpadian Empires, Vol. VII enters the battlefield, ");
            sb.append("choose white Citizen, blue Camarid, black Thrull, red Goblin, or green Saproling.\r\n");
            sb.append("3, Tap: Put a 1/1 creature token of the chosen color and type onto the battlefield.\r\n");
            sb.append(card.getText()); // In the slight chance that there may be
                                       // a need to add a note to this card.
            card.setText(sb.toString());

            card.addComesIntoPlayCommand(intoPlay);

        } // *************** END ************ END **************************

        // *************** START *********** START **************************
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
                    final CardList grave = AllZone.getHumanPlayer().getCardsIn(ZoneType.Graveyard);
                    final CardList aiGrave = AllZone.getComputerPlayer().getCardsIn(ZoneType.Graveyard);
                    return ((grave.getType("Creature").size() > 1) || (aiGrave.getType("Creature").size() > 1))
                            && super.canPlay();
                }
            };
            final Input soilTarget = new Input() {

                private boolean once = false;
                private static final long serialVersionUID = 8243511353958609599L;

                @Override
                public void showMessage() {
                    CardList grave = AllZone.getHumanPlayer().getCardsIn(ZoneType.Graveyard);
                    CardList aiGrave = AllZone.getComputerPlayer().getCardsIn(ZoneType.Graveyard);
                    grave = grave.getType("Creature");
                    aiGrave = aiGrave.getType("Creature");

                    if (this.once || ((grave.size() < 2) && (aiGrave.size() < 2))) {
                        this.once = false;
                        this.stop();
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

                        final Object o = GuiUtils.chooseOne("Choose first creature to exile", chooseGrave.toArray());
                        if (o != null) {
                            CardList newGrave;
                            final Card c = (Card) o;
                            if (c.getOwner().isHuman()) {
                                newGrave = AllZone.getHumanPlayer().getCardsIn(ZoneType.Graveyard);
                            } else {
                                newGrave = AllZone.getComputerPlayer().getCardsIn(ZoneType.Graveyard);
                            }

                            newGrave = newGrave.getType("Creature");
                            newGrave.remove(c);

                            final Object o2 = GuiUtils.chooseOne("Choose second creature to exile", newGrave.toArray());
                            if (o2 != null) {
                                final Card c2 = (Card) o2;
                                newGrave.remove(c2);
                                Singletons.getModel().getGameAction().exile(c);
                                Singletons.getModel().getGameAction().exile(c2);
                                this.once = true;

                                AllZone.getStack().addSimultaneousStackEntry(nightSoil);

                            }
                        }
                    }
                    this.stop();
                }
            };

            final StringBuilder sbDesc = new StringBuilder();
            sbDesc.append("1, Exile two creature cards from a single graveyard: ");
            sbDesc.append("Put a 1/1 green Saproling creature token onto the battlefield.");
            nightSoil.setDescription(sbDesc.toString());

            final StringBuilder sbStack = new StringBuilder();
            sbStack.append(card.getController());
            sbStack.append(" puts a 1/1 green Saproling creature token onto the battlefield.");
            nightSoil.setStackDescription(sbStack.toString());

            nightSoil.setAfterPayMana(soilTarget);
            card.addSpellAbility(nightSoil);
        } // *************** END ************ END **************************

        // *************** START *********** START **************************
        else if (cardName.equals("Mox Diamond")) {
            final Input discard = new Input() {
                private static final long serialVersionUID = -1319202902385425204L;

                @Override
                public void showMessage() {
                    Singletons.getControl().getControlMatch().showMessage("Discard a land card (or select Mox Diamond to sacrifice it)");
                    ButtonUtil.enableOnlyCancel();
                }

                @Override
                public void selectCard(final Card c, final PlayerZone zone) {
                    if (zone.is(ZoneType.Hand) && c.isLand()) {
                        AllZone.getHumanPlayer().discard(c, null);
                        this.stop();
                    } else if (c.equals(card)) {
                        Singletons.getModel().getGameAction().sacrifice(card);
                        this.stop();
                    }
                }
            }; // Input

            final SpellAbility ability = new Ability(card, "0") {
                @Override
                public void resolve() {
                    if (card.getController().isHuman()) {
                        if (AllZone.getHumanPlayer().getZone(ZoneType.Hand).isEmpty()) {
                            Singletons.getModel().getGameAction().sacrifice(card);
                        } else {
                            AllZone.getInputControl().setInput(discard);
                        }
                    } else {
                        CardList list = AllZone.getComputerPlayer().getCardsIn(ZoneType.Hand);
                        list = list.filter(new CardListFilter() {
                            @Override
                            public boolean addCard(final Card c) {
                                return (c.isLand());
                            }
                        });
                        AllZone.getComputerPlayer().discard(list.get(0), this);
                    } // else
                } // resolve()
            }; // SpellAbility
            final Command intoPlay = new Command() {
                private static final long serialVersionUID = -7679939432259603542L;

                @Override
                public void execute() {
                    final StringBuilder sb = new StringBuilder();
                    sb.append("If Mox Diamond would enter the battlefield, you may ");
                    sb.append("discard a land card instead. If you do, put Mox Diamond onto the battlefield. ");
                    sb.append("If you don't, put it into its owner's graveyard.");
                    ability.setStackDescription(sb.toString());
                    AllZone.getStack().addSimultaneousStackEntry(ability);

                }
            };
            final SpellAbility spell = new SpellPermanent(card) {
                private static final long serialVersionUID = -1818766848857998431L;

                // could never get the AI to work correctly
                // it always played the same card 2 or 3 times
                @Override
                public boolean canPlayAI() {
                    return false;
                }

                @Override
                public boolean canPlay() {
                    CardList list = card.getController().getCardsIn(ZoneType.Hand);
                    list.remove(card);
                    list = list.filter(new CardListFilter() {
                        @Override
                        public boolean addCard(final Card c) {
                            return (c.isLand());
                        }
                    });
                    return (list.size() != 0) && super.canPlay();
                } // canPlay()
            };
            card.addComesIntoPlayCommand(intoPlay);
            card.clearSpellKeepManaAbility();
            card.addSpellAbility(spell);
        } // *************** END ************ END **************************

        // *************** START *********** START **************************
        else if (cardName.equals("Standstill")) {

            // Do not remove SpellAbilities created by AbilityFactory or
            // Keywords.
            card.clearFirstSpell();

            card.addSpellAbility(new SpellPermanent(card) {
                private static final long serialVersionUID = 6912683989507840172L;

                @Override
                public boolean canPlayAI() {
                    final CardList compCreats = AllZoneUtil.getCreaturesInPlay(AllZone.getComputerPlayer());
                    final CardList humCreats = AllZoneUtil.getCreaturesInPlay(AllZone.getHumanPlayer());

                    // only play standstill if comp controls more creatures than
                    // human
                    // this needs some additional rules, maybe add all power +
                    // toughness and compare
                    return (compCreats.size() > humCreats.size());
                }
            });
        } // *************** END ************ END **************************

        // *************** START *********** START **************************
        else if (cardName.equals("Goblin Charbelcher")) {
            final Cost abCost = new Cost(card, "3 T", true);
            final AbilityActivated ability = new AbilityActivated(card, abCost, new Target(card, "TgtCP")) {
                private static final long serialVersionUID = -840041589720758423L;

                @Override
                public void resolve() {
                    final CardList topOfLibrary = card.getController().getCardsIn(ZoneType.Library);
                    final CardList revealed = new CardList();

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
                        if (crd.isLand() || (count == topOfLibrary.size())) {
                            c = crd;
                            damage = count;
                            if (crd.isLand()) {
                                damage--;
                            }

                            if (crd.isType("Mountain")) {
                                damage *= 2;
                            }
                        }
                    } // while
                    GuiUtils.chooseOneOrNone("Revealed cards:", revealed.toArray());
                    for (final Card revealedCard : revealed) {
                        Singletons.getModel().getGameAction().moveToBottomOfLibrary(revealedCard);
                    }

                    if (this.getTargetCard() != null) {
                        if (AllZoneUtil.isCardInPlay(this.getTargetCard())
                                && this.getTargetCard().canBeTargetedBy(this)) {
                            this.getTargetCard().addDamage(damage, card);
                        }
                    } else {
                        this.getTargetPlayer().addDamage(damage, card);
                    }
                }
            };

            final StringBuilder sb = new StringBuilder();
            sb.append(abCost);
            sb.append("Reveal cards from the top of your library until you reveal a land card. ");
            sb.append("Goblin Charbelcher deals damage equal to the number of nonland cards revealed ");
            sb.append("this way to target creature or player. If the revealed land card was a ");
            sb.append("Mountain, Goblin Charbelcher deals double that damage instead. Put the ");
            sb.append("revealed cards on the bottom of your library in any order.");
            ability.setDescription(sb.toString());

            ability.setChooseTargetAI(CardFactoryUtil.targetHumanAI());
            card.addSpellAbility(ability);
        } // *************** END ************ END **************************

        // *************** START *********** START **************************
        else if (cardName.equals("Lodestone Bauble")) {
            /*
             * 1, Tap, Sacrifice Lodestone Bauble: Put up to four target basic
             * land cards from a player's graveyard on top of his or her library
             * in any order. That player draws a card at the beginning of the
             * next turn's upkeep.
             */

            final Cost abCost = new Cost(card, "1 T Sac<1/CARDNAME>", true);
            final Target target = new Target(card, "Select target player", new String[] { "Player" });
            final AbilityActivated ability = new AbilityActivated(card, abCost, target) {
                private static final long serialVersionUID = -6711849408085138636L;

                @Override
                public boolean canPlayAI() {
                    return this.getComputerLands().size() >= 4;
                }

                @Override
                public void chooseTargetAI() {
                    this.setTargetPlayer(AllZone.getComputerPlayer());
                } // chooseTargetAI()

                @Override
                public void resolve() {
                    final int limit = 4; // at most, this can target 4 cards
                    final Player player = this.getTargetPlayer();

                    CardList lands = player.getCardsIn(ZoneType.Graveyard);
                    lands = lands.filter(CardListFilter.BASIC_LANDS);
                    if (card.getController().isHuman()) {
                        // now, select up to four lands
                        int end = -1;
                        end = Math.min(lands.size(), limit);
                        // TODO - maybe pop a message box here that no basic
                        // lands found (if necessary)
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
                            final Object o = GuiUtils.chooseOneOrNone(title, lands.toArray());
                            if (o == null) {
                                break;
                            }
                            final Card c1 = (Card) o;
                            lands.remove(c1); // remove from the display list
                            Singletons.getModel().getGameAction().moveToLibrary(c1, i - 1);
                        }
                    } else { // Computer
                        // based on current AI, computer should always target
                        // himself.
                        final CardList list = this.getComputerLands();
                        int max = list.size();
                        if (max > limit) {
                            max = limit;
                        }

                        for (int i = 0; i < max; i++) {
                            Singletons.getModel().getGameAction().moveToLibrary(list.get(i));
                        }
                    }

                    player.addSlowtripList(card);
                }

                private CardList getComputerLands() {
                    final CardList list = AllZone.getComputerPlayer().getCardsIn(ZoneType.Graveyard);
                    return list.getType("Basic");
                }
            }; // ability

            final StringBuilder sb = new StringBuilder();
            sb.append(abCost);
            sb.append("Put up to four target basic land cards from a player's graveyard on top ");
            sb.append("of his or her library in any order. That player draws a card at the ");
            sb.append("beginning of the next turn's upkeep.");
            ability.setDescription(sb.toString());
            card.addSpellAbility(ability);
        } // *************** END ************ END **************************

        // *************** START *********** START **************************
        else if (cardName.equals("Grindstone")) {
            final Target target = new Target(card, "Select target player", new String[] { "Player" });
            final Cost abCost = new Cost(card, "3 T", true);
            final AbilityActivated ab1 = new AbilityActivated(card, abCost, target) {
                private static final long serialVersionUID = -6281219446216L;

                @Override
                public boolean canPlayAI() {
                    final CardList libList = AllZone.getHumanPlayer().getCardsIn(ZoneType.Library);
                    // CardList list =
                    // AllZoneUtil.getCardsInPlay("Painter's Servant");
                    return libList.size() > 0; // && list.size() > 0;
                }

                @Override
                public void resolve() {
                    final Player target = this.getTargetPlayer();
                    final CardList library = this.getTargetPlayer().getCardsIn(ZoneType.Library);

                    boolean loop = true;
                    final CardList grinding = new CardList();
                    do {
                        grinding.clear();

                        for (int i = 0; i < 2; i++) {
                            // Move current grinding to a different list
                            if (library.size() > 0) {
                                final Card c = library.get(0);
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
            ab1.setChooseTargetAI(CardFactoryUtil.targetHumanAI());
            final StringBuilder sb = new StringBuilder();
            sb.append(abCost);
            sb.append("Put the top two cards of target player's library into that player's graveyard. ");
            sb.append("If both cards share a color, repeat this process.");
            ab1.setDescription(sb.toString());
            card.addSpellAbility(ab1);
        } // *************** END ************ END **************************

        // *************** START *********** START **************************
        else if (cardName.equals("Everflowing Chalice")) {
            final Command comesIntoPlay = new Command() {
                private static final long serialVersionUID = 4245563898487609274L;

                @Override
                public void execute() {
                    card.addCounter(Counters.CHARGE, card.getMultiKickerMagnitude());
                    card.setMultiKickerMagnitude(0);
                }
            };
            card.addComesIntoPlayCommand(comesIntoPlay);
        } // *************** END ************ END **************************

        // *************** START *********** START **************************
        else if (cardName.equals("Phyrexian Processor")) {
            final Command intoPlay = new Command() {
                private static final long serialVersionUID = 5634360316643996274L;

                @Override
                public void execute() {

                    final Player player = card.getController();
                    int lifeToPay = 0;
                    if (player.isHuman()) {
                        final int num = card.getController().getLife();
                        final String[] choices = new String[num + 1];
                        for (int j = 0; j <= num; j++) {
                            choices[j] = "" + j;
                        }
                        final String answer = (GuiUtils.chooseOneOrNone("Life to pay:", choices));
                        lifeToPay = Integer.parseInt(answer);
                    } else {
                        // not implemented for Compy
                    }

                    if (player.payLife(lifeToPay, card)) {
                        card.setXLifePaid(lifeToPay);
                    }

                }
            };
            card.addComesIntoPlayCommand(intoPlay);
        } // *************** END ************ END **************************

        // *************** START *********** START **************************
        else if (cardName.equals("Scroll Rack")) {
            final Cost abCost = new Cost(card, "1 T", true);
            final AbilityActivated ability = new AbilityActivated(card, abCost, null) {
                private static final long serialVersionUID = -5588587187720068547L;

                @Override
                public void resolve() {
                    // not implemented for compy
                    if (card.getController().isHuman()) {
                        AllZone.getInputControl().setInput(new Input() {
                            private static final long serialVersionUID = -2305549394512889450L;
                            private final CardList exiled = new CardList();

                            @Override
                            public void showMessage() {
                                final StringBuilder sb = new StringBuilder();
                                sb.append(card.getName()).append(" - Exile cards from hand.  Currently, ");
                                sb.append(this.exiled.size()).append(" selected.  (Press OK when done.)");
                                Singletons.getControl().getControlMatch().showMessage(sb.toString());
                                ButtonUtil.enableOnlyOK();
                            }

                            @Override
                            public void selectButtonOK() {
                                this.done();
                            }

                            @Override
                            public void selectCard(final Card c, final PlayerZone zone) {
                                if (zone.is(ZoneType.Hand, AllZone.getHumanPlayer()) && !this.exiled.contains(c)) {
                                    this.exiled.add(c);
                                    this.showMessage();
                                }
                            }

                            public void done() {
                                // exile those cards
                                for (final Card c : this.exiled) {
                                    Singletons.getModel().getGameAction().exile(c);
                                }

                                // Put that many cards from the top of your
                                // library into your hand.
                                // Ruling: This is not a draw...
                                final PlayerZone lib = AllZone.getHumanPlayer().getZone(ZoneType.Library);
                                int numCards = 0;
                                while ((lib.size() > 0) && (numCards < this.exiled.size())) {
                                    Singletons.getModel().getGameAction().moveToHand(lib.get(0));
                                    numCards++;
                                }

                                final StringBuilder sb = new StringBuilder();
                                sb.append(card.getName()).append(" - Returning cards to top of library.");
                                Singletons.getControl().getControlMatch().showMessage(sb.toString());

                                // Then look at the exiled cards and put them on
                                // top of your library in any order.
                                while (this.exiled.size() > 0) {
                                    final Object o = GuiUtils.chooseOne("Put a card on top of your library.",
                                            this.exiled.toArray());
                                    final Card c1 = (Card) o;
                                    Singletons.getModel().getGameAction().moveToLibrary(c1);
                                    this.exiled.remove(c1);
                                }

                                this.stop();
                            }
                        });
                    }
                }

                @Override
                public boolean canPlayAI() {
                    return false;
                }
            }; // ability
            final StringBuilder sbDesc = new StringBuilder();
            sbDesc.append(abCost);
            sbDesc.append("Exile any number of cards from your hand face down. Put that many cards ");
            sbDesc.append("from the top of your library into your hand. Then look at the exiled cards ");
            sbDesc.append("and put them on top of your library in any order.");
            ability.setDescription(sbDesc.toString());

            final StringBuilder sbStack = new StringBuilder();
            sbStack.append(cardName).append(" - exile any number of cards from your hand.");
            ability.setStackDescription(sbStack.toString());
            card.addSpellAbility(ability);
        } // *************** END ************ END **************************

        // *************** START *********** START **************************
        else if (cardName.equals("Temporal Aperture")) {
            /*
             * 5, Tap: Shuffle your library, then reveal the top card. Until end
             * of turn, for as long as that card remains on top of your library,
             * play with the top card of your library revealed and you may play
             * that card without paying its mana cost. (If it has X in its mana
             * cost, X is 0.)
             */
            final Card[] topCard = new Card[1];

            final Ability freeCast = new Ability(card, "0") {

                @Override
                public boolean canPlay() {
                    final PlayerZone lib = card.getController().getZone(ZoneType.Library);
                    return super.canPlay() && ((lib.size() > 0) && lib.get(0).equals(topCard[0]));
                }

                @Override
                public void resolve() {
                    final Card freeCard = topCard[0];
                    final Player player = card.getController();
                    if (freeCard != null) {
                        if (freeCard.isLand()) {
                            if (player.canPlayLand()) {
                                player.playLand(freeCard);
                            } else {
                                JOptionPane.showMessageDialog(null, "You can't play any more lands this turn.", "",
                                        JOptionPane.INFORMATION_MESSAGE);
                            }
                        } else {
                            Singletons.getModel().getGameAction().playCardNoCost(freeCard);
                        }
                    } else {
                        JOptionPane.showMessageDialog(null, "Error in " + cardName + ".  freeCard is null", "",
                                JOptionPane.INFORMATION_MESSAGE);
                    }

                }

                @Override
                public boolean canPlayAI() {
                    return false;
                }

            };
            freeCast.setDescription("Play the previously revealed top card of your library for free.");
            final StringBuilder sb = new StringBuilder();
            sb.append(cardName).append(" - play card without paying its mana cost.");
            freeCast.setStackDescription(sb.toString());

            final Cost abCost = new Cost(card, "5 T", true);
            final AbilityActivated ability = new AbilityActivated(card, abCost, null) {
                private static final long serialVersionUID = -7328518969488588777L;

                @Override
                public void resolve() {
                    final PlayerZone lib = card.getController().getZone(ZoneType.Library);
                    if (lib.size() > 0) {

                        // shuffle your library
                        card.getController().shuffle();

                        // reveal the top card
                        topCard[0] = lib.get(0);
                        JOptionPane.showMessageDialog(null, "Revealed card:\n" + topCard[0].getName(), card.getName(),
                                JOptionPane.PLAIN_MESSAGE);

                        card.addSpellAbility(freeCast);
                        card.addExtrinsicKeyword("Play with the top card of your library revealed.");
                        AllZone.getEndOfTurn().addUntil(new Command() {
                            private static final long serialVersionUID = -2860753262177388046L;

                            @Override
                            public void execute() {
                                card.removeSpellAbility(freeCast);
                                card.removeExtrinsicKeyword("Play with the top card of your library revealed.");
                            }
                        });
                    }
                } // resolve

                @Override
                public boolean canPlayAI() {
                    return false;
                }
            };

            final StringBuilder sbStack = new StringBuilder();
            sbStack.append(card).append(" - Shuffle your library, then reveal the top card.");
            ability.setStackDescription(sbStack.toString());

            final StringBuilder sbDesc = new StringBuilder();
            sbDesc.append(abCost).append("Shuffle your library, then reveal the top card. ");
            sbDesc.append("Until end of turn, for as long as that card remains on top of your ");
            sbDesc.append("library, play with the top card of your library revealed ");
            sbDesc.append("and you may play that card without paying its mana cost. ");
            sbDesc.append("(If it has X in its mana cost, X is 0.)");
            ability.setDescription(sbDesc.toString());

            card.addSpellAbility(ability);
        } // *************** END ************ END **************************

        // *************** START *********** START **************************
        else if (cardName.equals("Lich")) {
            final SpellAbility loseAllLife = new Ability(card, "0") {
                @Override
                public void resolve() {
                    final int life = card.getController().getLife();
                    card.getController().loseLife(life, card);
                }
            };

            final Command intoPlay = new Command() {
                private static final long serialVersionUID = 1337794055075168785L;

                @Override
                public void execute() {

                    final StringBuilder sb = new StringBuilder();
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

            final Command toGrave = new Command() {
                private static final long serialVersionUID = 5863295714122376047L;

                @Override
                public void execute() {

                    final StringBuilder sb = new StringBuilder();
                    sb.append(cardName).append(" - ").append(card.getController());
                    sb.append("loses the game.");
                    loseGame.setStackDescription(sb.toString());

                    AllZone.getStack().addSimultaneousStackEntry(loseGame);

                }
            };

            card.addComesIntoPlayCommand(intoPlay);
            card.addDestroyCommand(toGrave);
        } // *************** END ************ END **************************

        // *************** START *********** START **************************
        /*else if (cardName.equals("Triangle of War")) {

            final Target t2 = new Target(card, "Select target creature an opponent controls",
                    "Creature.YouDontCtrl".split(","));
            final AbilitySub sub = new AbilitySub(card, t2) {
                private static final long serialVersionUID = -572849470457911366L;

                @Override
                public boolean chkAIDrawback() {
                    return false;
                }

                @Override
                public void resolve() {
                    final Card myc = this.getParent().getTargetCard();
                    final Card oppc = this.getTargetCard();
                    if (AllZoneUtil.isCardInPlay(myc) && AllZoneUtil.isCardInPlay(oppc)) {
                        if (myc.canBeTargetedBy(this) && oppc.canBeTargetedBy(this)) {
                            final int myPower = myc.getNetAttack();
                            final int oppPower = oppc.getNetAttack();
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

            final Cost abCost = new Cost("2 Sac<1/CARDNAME>", cardName, true);
            final Target t1 = new Target(card, "Select target creature you control", "Creature.YouCtrl".split(","));
            final AbilityActivated ability = new AbilityActivated(card, abCost, t1) {
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
            final StringBuilder sbDesc = new StringBuilder();
            sbDesc.append(abCost);
            sbDesc.append("Choose target creature you control and target creature an opponent controls. ");
            sbDesc.append("Each of those creatures deals damage equal to its power to the other.");
            ability.setDescription(sbDesc.toString());

            final StringBuilder sbStack = new StringBuilder();
            sbStack.append(card).append(" - Each creature deals damage equal to its power to the other.");
            ability.setStackDescription(sbStack.toString());
            card.addSpellAbility(ability);
        }*/ // *************** END ************ END **************************

        // *************** START *********** START **************************
        else if (cardName.equals("Copy Artifact") || cardName.equals("Sculpting Steel")) {
            final Card[] copyTarget = new Card[1];

            final SpellAbility copy = new Spell(card) {
                private static final long serialVersionUID = 4496978456522751302L;

                @Override
                public void resolve() {
                    if (card.getController().isComputer()) {
                        final CardList cards = AllZoneUtil.getCardsIn(ZoneType.Battlefield).getType("Artifact");
                        if (!cards.isEmpty()) {
                            copyTarget[0] = CardFactoryUtil.getBestAI(cards);
                        }
                    }

                    if (copyTarget[0] != null) {
                        Card cloned;

                        AllZone.getTriggerHandler().suppressMode(TriggerType.Transformed);

                        // TODO: transform back and forth
                        cloned = getCard2(copyTarget[0], card.getOwner());
                        // TODO: untransform

                        card.addAlternateState(CardCharactersticName.Cloner);
                        card.switchStates(CardCharactersticName.Original, CardCharactersticName.Cloner);
                        card.setState(CardCharactersticName.Original);

                        if (copyTarget[0].getCurState() == CardCharactersticName.Transformed && copyTarget[0].isDoubleFaced()) {
                            cloned.setState(CardCharactersticName.Transformed);
                        }

                        CardFactoryUtil.copyCharacteristics(cloned, card);
                        this.grantExtras();

                        // If target is a flipped card, also copy the flipped
                        // state.
                        if (copyTarget[0].isFlip()) {
                            cloned.setState(CardCharactersticName.Flipped);
                            cloned.setImageFilename(CardUtil.buildFilename(cloned));
                            card.addAlternateState(CardCharactersticName.Flipped);
                            card.setState(CardCharactersticName.Flipped);
                            CardFactoryUtil.copyCharacteristics(cloned, card);
                            this.grantExtras();

                            card.setFlip(true);

                            card.setState(CardCharactersticName.Original);
                        } else {
                            card.setFlip(false);
                        }

                        AllZone.getTriggerHandler().clearSuppression(TriggerType.Transformed);
                    }

                    Singletons.getModel().getGameAction().moveToPlay(card);
                }

                private void grantExtras() {
                    // Grant stuff from specific cloners
                    if (cardName.equals("Copy Artifact")) {
                        card.addType("Enchantment");
                    }

                }
            }; // SpellAbility

            final Input runtime = new Input() {
                private static final long serialVersionUID = 8117808324791871452L;

                @Override
                public void showMessage() {
                    final StringBuilder sb = new StringBuilder();
                    sb.append(cardName).append(" - Select an artifact on the battlefield");
                    Singletons.getControl().getControlMatch().showMessage(sb.toString());
                    ButtonUtil.enableOnlyCancel();
                }

                @Override
                public void selectButtonCancel() {
                    this.stop();
                }

                @Override
                public void selectCard(final Card c, final PlayerZone z) {
                    if (z.is(ZoneType.Battlefield) && c.isArtifact()) {
                        copyTarget[0] = c;
                        this.stopSetNext(new InputPayManaCost(copy));
                    }
                }
            };
            // Do not remove SpellAbilities created by AbilityFactory or
            // Keywords.
            card.clearFirstSpell();
            card.addSpellAbility(copy);
            final StringBuilder sb = new StringBuilder();
            sb.append(cardName).append(" - enters the battlefield as a copy of selected card.");
            copy.setStackDescription(sb.toString());
            copy.setBeforePayMana(runtime);
        } // *************** END ************ END **************************

        // *************** START *********** START **************************
        else if (cardName.equals("Sylvan Library")) {

            final Ability ability = new Ability(card, "") {
                @Override
                public void resolve() {
                    final Player player = card.getController();
                    if (player.isHuman()) {
                        final String cardQuestion = "Pay 4 life and keep in hand?";
                        player.drawCards(2);
                        for (int i = 0; i < 2; i++) {
                            final String prompt = card + " - Select a card drawn this turn: " + (2 - i) + " of 2";
                            AllZone.getInputControl().setInput(new Input() {
                                private static final long serialVersionUID = -3389565833121544797L;

                                @Override
                                public void showMessage() {
                                    if (AllZone.getHumanPlayer().getZone(ZoneType.Hand).size() == 0) {
                                        this.stop();
                                    }
                                    Singletons.getControl().getControlMatch().showMessage(prompt);
                                    ButtonUtil.disableAll();
                                }

                                @Override
                                public void selectCard(final Card card, final PlayerZone zone) {
                                    if (zone.is(ZoneType.Hand) && card.getDrawnThisTurn()) {
                                        if (player.canPayLife(4) && GameActionUtil.showYesNoDialog(card, cardQuestion)) {
                                            player.payLife(4, card);
                                            // card stays in hand
                                        } else {
                                            Singletons.getModel().getGameAction().moveToLibrary(card);
                                        }
                                        this.stop();
                                    }
                                }
                            }); // end Input
                        }
                    } else {
                        // Computer, but he's too stupid to play this
                    }
                } // resolve
            }; // Ability

            final StringBuilder sb = new StringBuilder();
            sb.append("At the beginning of your draw step, you may draw two additional cards. ");
            sb.append("If you do, choose two cards in your hand drawn this turn. For each of those cards, ");
            sb.append("pay 4 life or put the card on top of your library.");
            ability.setStackDescription(sb.toString());

            final Trigger drawStepTrigger = forge.card.trigger.TriggerHandler.parseTrigger(
                    "Mode$ Phase | Phase$ Draw | ValidPlayer$ You | OptionalDecider$ You | "
                            + "TriggerZones$ Battlefield | Secondary$ True | TriggerDescription$ At the beginning of "
                            + "your draw step, you may draw two additional cards. If you do, choose two "
                            + "cards in your hand drawn this turn. For each of those cards, "
                            + "pay 4 life or put the card on top of your library.", card, true);
            drawStepTrigger.setOverridingAbility(ability);
            card.addTrigger(drawStepTrigger);
        } // *************** END ************ END **************************

        return CardFactoryUtil.postFactoryKeywords(card);
    } // getCard2

} // end class AbstractCardFactory
