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
package forge;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.apache.commons.lang3.StringUtils;

import com.esotericsoftware.minlog.Log;
import com.google.common.collect.ImmutableList;

import forge.CardPredicates.Presets;
import forge.card.CardCharacteristics;
import forge.card.CardRarity;
import forge.card.CardRules;
import forge.card.ColorSet;
import forge.card.MagicColor;
import forge.card.ability.AbilityUtils;
import forge.card.ability.ApiType;
import forge.card.cardfactory.CardFactoryUtil;
import forge.card.cost.Cost;
import forge.card.mana.ManaCost;
import forge.card.replacement.ReplaceMoved;
import forge.card.replacement.ReplacementEffect;
import forge.card.replacement.ReplacementResult;
import forge.card.spellability.Ability;
import forge.card.spellability.AbilityTriggered;
import forge.card.spellability.OptionalCost;
import forge.card.spellability.SpellAbility;
import forge.card.spellability.SpellPermanent;
import forge.card.spellability.Target;
import forge.card.staticability.StaticAbility;
import forge.card.trigger.Trigger;
import forge.card.trigger.TriggerType;
import forge.card.trigger.ZCTrigger;
import forge.game.Game;
import forge.game.GlobalRuleChange;
import forge.game.event.GameEventCardDamaged;
import forge.game.event.GameEventCardDamaged.DamageType;
import forge.game.event.GameEventCardEquipped;
import forge.game.event.GameEventCounterAdded;
import forge.game.event.GameEventCounterRemoved;
import forge.game.event.GameEventCardTapped;
import forge.game.phase.Combat;
import forge.game.player.Player;
import forge.game.zone.ZoneType;
import forge.item.CardDb;
import forge.util.Expressions;

/**
 * <p>
 * Card class.
 * </p>
 * 
 * Can now be used as keys in Tree data structures. The comparison is based
 * entirely on getUniqueNumber().
 * 
 * @author Forge
 * @version $Id$
 */
public class Card extends GameEntity implements Comparable<Card> {
    private static int nextUniqueNumber = 1;
    private int uniqueNumber;

    private final Map<CardCharacteristicName, CardCharacteristics> characteristicsMap
        = new EnumMap<CardCharacteristicName, CardCharacteristics>(CardCharacteristicName.class);
    private CardCharacteristicName curCharacteristics = CardCharacteristicName.Original;
    private CardCharacteristicName preTFDCharacteristic = CardCharacteristicName.Original;

    private ZoneType castFrom = null;

    private final CardDamageHistory damageHistory = new CardDamageHistory();
    private Map<CounterType, Integer> counters = new TreeMap<CounterType, Integer>();
    private Map<Card, Map<CounterType, Integer>> countersAddedBy = new TreeMap<Card, Map<CounterType, Integer>>();
    private final Map<String, Object> triggeringObjects = new TreeMap<String, Object>();
    private ArrayList<String> extrinsicKeyword = new ArrayList<String>();
    // Hidden keywords won't be displayed on the card
    private final ArrayList<String> hiddenExtrinsicKeyword = new ArrayList<String>();

    // which equipment cards are equipping this card?
    private ArrayList<Card> equippedBy = new ArrayList<Card>();
    // equipping size will always be 0 or 1
    // if this card is of the type equipment, what card is it currently equipping?
    private ArrayList<Card> equipping = new ArrayList<Card>();
    // which auras enchanted this card?

    // if this card is an Aura, what Entity is it enchanting?
    private GameEntity enchanting = null;

    // changes by AF animate and continuous static effects - timestamp is the key of maps
    private Map<Long, CardType> changedCardTypes = new ConcurrentSkipListMap<Long, CardType>();
    private Map<Long, CardKeywords> changedCardKeywords = new ConcurrentSkipListMap<Long, CardKeywords>();

    private final ArrayList<Object> rememberedObjects = new ArrayList<Object>();
    private final ArrayList<Card> imprintedCards = new ArrayList<Card>();
    private final ArrayList<Card> encodedCards = new ArrayList<Card>();
    private final List<Card> devouredCards = new ArrayList<Card>();
    private Map<Player, String> flipResult = new TreeMap<Player, String>();

    private Map<Card, Integer> receivedDamageFromThisTurn = new TreeMap<Card, Integer>();
    private Map<Card, Integer> dealtDamageToThisTurn = new TreeMap<Card, Integer>();
    private Map<String, Integer> dealtDamageToPlayerThisTurn = new TreeMap<String, Integer>();
    private final Map<Card, Integer> assignedDamageMap = new TreeMap<Card, Integer>();
    private List<Card> blockedThisTurn = null;
    private List<Card> blockedByThisTurn = null;

    private boolean isCommander = false;
    private boolean startsGameInPlay = false;
    private boolean drawnThisTurn = false;
    private boolean tapped = false;
    private boolean sickness = true; // summoning sickness
    private boolean token = false;
    private boolean copiedToken = false;
    private boolean copiedSpell = false;

    private ArrayList<Card> mustBlockCards = null;

    private boolean canCounter = true;
    private boolean evoked = false;

    private boolean levelUp = false;

    private boolean unearthed;

    private boolean suspendCast = false;
    private boolean suspend = false;

    private boolean phasedOut = false;
    private boolean directlyPhasedOut = true;

    private boolean usedToPayCost = false;

    // for Vanguard / Manapool / Emblems etc.
    private boolean isImmutable = false;

    private long timestamp = -1; // permanents on the battlefield

    // stack of set power/toughness
    private ArrayList<CardPowerToughness> newPT = new ArrayList<CardPowerToughness>();
    private int baseLoyalty = 0;
    private String baseAttackString = null;
    private String baseDefenseString = null;

    private int damage;

    // regeneration
    private int nShield;
    private int regeneratedThisTurn = 0;

    private int turnInZone;

    private int tempAttackBoost = 0;
    private int tempDefenseBoost = 0;

    private int semiPermanentAttackBoost = 0;
    private int semiPermanentDefenseBoost = 0;

    private int xManaCostPaid = 0;

    private int replicateMagnitude = 0;

    private int sunburstValue = 0;
    private byte colorsPaid = 0;

    private Player owner = null;
    private Player controller = null;
    private long controllerTimestamp = 0;
    private TreeMap<Long, Player> tempControllers = new TreeMap<Long, Player>();

    private String text = "";
    private String echoCost = "";
    private String madnessCost = null;
    private String miracleCost = null;
    private String chosenType = "";
    private List<String> chosenColor = new ArrayList<String>();
    private String namedCard = "";
    private int chosenNumber;
    private Player chosenPlayer;
    private List<Card> chosenCard = new ArrayList<Card>();

    private Card cloneOrigin = null;
    private final List<Card> clones = new ArrayList<Card>();
    private final List<Card> gainControlTargets = new ArrayList<Card>();
    private final List<Command> gainControlReleaseCommands = new ArrayList<Command>();

    private final List<AbilityTriggered> zcTriggers = new ArrayList<AbilityTriggered>();
    private final List<Command> untapCommandList = new ArrayList<Command>();
    private final List<Command> changeControllerCommandList = new ArrayList<Command>();

    private final static ImmutableList<String> storableSVars = ImmutableList.of("ChosenX" );

    private final List<Card> hauntedBy = new ArrayList<Card>();
    private Card haunting = null;
    private Card effectSource = null;

    // Soulbond pairing card
    private Card pairedWith = null;
    
    // Enumeration for CMC request types
    public enum SplitCMCMode {
        CurrentSideCMC,
        CombinedCMC,
        LeftSplitCMC,
        RightSplitCMC
    }

    /**
     * Instantiates a new card.
     */
    public Card() {
        refreshUniqueNumber();
        this.characteristicsMap.put(CardCharacteristicName.Original, new CardCharacteristics());
        this.characteristicsMap.put(CardCharacteristicName.FaceDown, CardUtil.getFaceDownCharacteristic());
    }

    public void refreshUniqueNumber() {
        this.setUniqueNumber(Card.nextUniqueNumber++);
    }

    /**
     * Sets the state.
     * 
     * @param state
     *            the state
     * @return true, if successful
     */
    public boolean changeToState(final CardCharacteristicName state) {

        CardCharacteristicName cur = this.curCharacteristics;

        if (!setState(state)) {
            return false;
        }

        if ((cur == CardCharacteristicName.Original && state == CardCharacteristicName.Transformed)
                || (cur == CardCharacteristicName.Transformed && state == CardCharacteristicName.Original)) {
            HashMap<String, Object> runParams = new HashMap<String, Object>();
            runParams.put("Transformer", this);
            getGame().getTriggerHandler().runTrigger(TriggerType.Transformed, runParams, false);
        }

        return true;
    }

    /**
     * Sets the state.
     * 
     * @param state
     *            the state
     * @return true, if successful
     */
    public boolean setState(final CardCharacteristicName state) {
        if (state == CardCharacteristicName.FaceDown && this.isDoubleFaced()) {
            return false; // Doublefaced cards can't be turned face-down.
        }

        if (!this.characteristicsMap.containsKey(state)) {
            System.out.println(this.getName() + " tried to switch to non-existant state \"" + state + "\"!");
            return false; // Nonexistant state.
        }

        if (state.equals(this.curCharacteristics)) {
            return false;
        }

        this.curCharacteristics = state;

        return true;
    }

    /**
     * Gets the states.
     * 
     * @return the states
     */
    public Set<CardCharacteristicName> getStates() {
        return this.characteristicsMap.keySet();
    }

    /**
     * Gets the cur state.
     * 
     * @return the cur state
     */
    public CardCharacteristicName getCurState() {
        return this.curCharacteristics;
    }

    /**
     * Switch states.
     * 
     * @param from
     *            the from
     * @param to
     *            the to
     */
    public void switchStates(final CardCharacteristicName from, final CardCharacteristicName to) {
        final CardCharacteristics tmp = this.characteristicsMap.get(from);
        this.characteristicsMap.put(from, this.characteristicsMap.get(to));
        this.characteristicsMap.put(to, tmp);
    }

    /**
     * Clear states.
     * 
     * @param state
     *            the state
     */
    public void clearStates(final CardCharacteristicName state) {
        this.characteristicsMap.remove(state);
    }

    public void setPreFaceDownCharacteristic(CardCharacteristicName preCharacteristic) {
        this.preTFDCharacteristic = preCharacteristic;
    }

    /**
     * Turn face down.
     * 
     * @return true, if successful
     */
    public boolean turnFaceDown() {
        if (!this.isDoubleFaced()) {
            this.preTFDCharacteristic = this.curCharacteristics;
            return this.setState(CardCharacteristicName.FaceDown);
        }

        return false;
    }

    /**
     * Turn face up.
     * 
     * @return true, if successful
     */
    public boolean turnFaceUp() {
        if (this.curCharacteristics == CardCharacteristicName.FaceDown) {
            boolean result = this.setState(this.preTFDCharacteristic);
            if (result) {
                // Run replacement effects
                HashMap<String, Object> repParams = new HashMap<String, Object>();
                repParams.put("Event", "TurnFaceUp");
                repParams.put("Affected", this);
                getGame().getReplacementHandler().run(repParams);

                // Run triggers
                final Map<String, Object> runParams = new TreeMap<String, Object>();
                runParams.put("Card", this);
                getGame().getTriggerHandler().runTrigger(TriggerType.TurnFaceUp, runParams, false);
            }
            return result;
        }

        return false;
    }

    /**
     * Gets the state.
     * 
     * @param state
     *            the state
     * @return the state
     */
    public CardCharacteristics getState(final CardCharacteristicName state) {
        return this.characteristicsMap.get(state);
    }

    /**
     * Gets the characteristics.
     * 
     * @return the characteristics
     */
    public CardCharacteristics getCharacteristics() {
        return this.characteristicsMap.get(this.curCharacteristics);
    }

    /**
     * addAlternateState.
     * 
     * @param state
     *            the state
     */
    public final void addAlternateState(final CardCharacteristicName state) {
        this.characteristicsMap.put(state, new CardCharacteristics());
    }

    /*
     * (non-Javadoc)
     * 
     * @see forge.GameEntity#getName()
     */
    @Override
    public final String getName() {
        return this.getCharacteristics().getName();
    }

    /*
     * (non-Javadoc)
     * 
     * @see forge.GameEntity#setName(java.lang.String)
     */
    @Override
    public final void setName(final String name0) {
        this.getCharacteristics().setName(name0);
    }

    /**
     * 
     * isInAlternateState.
     * 
     * @return boolean
     */
    public final boolean isInAlternateState() {
        return this.curCharacteristics != CardCharacteristicName.Original
            && this.curCharacteristics != CardCharacteristicName.Cloned;
    }

    /**
     * 
     * hasAlternateState.
     * 
     * @return boolean
     */
    public final boolean hasAlternateState() {
        return this.characteristicsMap.keySet().size() > 2;
    }

    public final boolean isDoubleFaced() {
        return characteristicsMap.containsKey(CardCharacteristicName.Transformed);
    }

    public final boolean isFlipCard() {
        return characteristicsMap.containsKey(CardCharacteristicName.Flipped);
    }
    
    public final boolean isSplitCard() { 
        return characteristicsMap.containsKey(CardCharacteristicName.LeftSplit);
    }

    /**
     * Checks if is cloned.
     * 
     * @return true, if is cloned
     */
    public boolean isCloned() {
        return characteristicsMap.containsKey(CardCharacteristicName.Cloner);
    }

    /**
     * 
     * TODO Write javadoc for this method.
     * 
     * @return a String array
     */
    public static List<String> getStorableSVars() {
        return Card.storableSVars;
    }

    // hacky code below, used to limit the number of times an ability
    // can be used per turn like Vampire Bats
    // should be put in SpellAbility, but it is put here for convenience
    // this is make public just to make things easy
    // this code presumes that each card only has one ability that can be
    // used a limited number of times per turn
    // CardFactory.SSP_canPlay(Card) uses these variables

    /**
     * 
     * Resets the unique number for this Card to 1.
     */
    public static void resetUniqueNumber() {
        Card.nextUniqueNumber = 1;
    }

    /**
     * 
     * TODO Write javadoc for this method.
     * 
     * @param c
     *            a Card object
     */
    public final void addDevoured(final Card c) {
        this.devouredCards.add(c);
    }

    /**
     * 
     * TODO Write javadoc for this method.
     */
    public final void clearDevoured() {
        this.devouredCards.clear();
    }

    /**
     * 
     * TODO Write javadoc for this method.
     * 
     * @return a List<Card> object
     */
    public final List<Card> getDevoured() {
        return this.devouredCards;
    }

    /**
     * <p>
     * addRemembered.
     * </p>
     * 
     * @param o
     *            a {@link java.lang.Object} object.
     */
    public final void addRemembered(final Object o) {
        this.rememberedObjects.add(o);
    }

    /**
     * <p>
     * removeRemembered.
     * </p>
     * 
     * @param o
     *            a {@link java.lang.Object} object.
     */
    public final void removeRemembered(final Object o) {
        this.rememberedObjects.remove(o);
    }

    /**
     * <p>
     * getRemembered.
     * </p>
     * 
     * @return a {@link java.util.ArrayList} object.
     */
    public final ArrayList<Object> getRemembered() {
        return this.rememberedObjects;
    }

    /**
     * <p>
     * clearRemembered.
     * </p>
     */
    public final void clearRemembered() {
        this.rememberedObjects.clear();
    }

    /**
     * <p>
     * addImprinted.
     * </p>
     * 
     * @param c
     *            a {@link forge.Card} object.
     */
    public final void addImprinted(final Card c) {
        this.imprintedCards.add(c);
    }

    /**
     * <p>
     * addImprinted.
     * </p>
     * 
     * @param list
     *            a {@link java.util.ArrayList} object.
     */
    public final void addImprinted(final ArrayList<Card> list) {
        this.imprintedCards.addAll(list);
    }

    /**
     * TODO: Write javadoc for this method.
     */
    public final void removeImprinted(final Object o) {
        this.imprintedCards.remove(o);
    }

    /**
     * <p>
     * getImprinted.
     * </p>
     * 
     * @return a {@link java.util.ArrayList} object.
     */
    public final ArrayList<Card> getImprinted() {
        return this.imprintedCards;
    }

    /**
     * <p>
     * clearImprinted.
     * </p>
     */
    public final void clearImprinted() {
        this.imprintedCards.clear();
    }

    /**
     * <p>
     * addEncoded.
     * </p>
     * 
     * @param c
     *            a {@link forge.Card} object.
     */
    public final void addEncoded(final Card c) {
        this.encodedCards.add(c);
    }

    /**
     * <p>
     * addEncoded.
     * </p>
     * 
     * @param list
     *            a {@link java.util.ArrayList} object.
     */
    public final void addEncoded(final ArrayList<Card> list) {
        this.encodedCards.addAll(list);
    }

    /**
     * TODO: Write javadoc for this method.
     */
    public final void removeEncoded(final Object o) {
        this.encodedCards.remove(o);
    }

    /**
     * <p>
     * getEncoded.
     * </p>
     * 
     * @return a {@link java.util.ArrayList} object.
     */
    public final ArrayList<Card> getEncoded() {
        return this.encodedCards;
    }

    /**
     * <p>
     * clearEncoded.
     * </p>
     */
    public final void clearEncoded() {
        this.encodedCards.clear();
    }

    /**
     * <p>
     * addFlipResult.
     * </p>
     * 
     * @param flipper  The Player who flipped the coin.
     * @param result   The result of the coin flip as a String.
     */
    public final void addFlipResult(final Player flipper, final String result) {
        this.flipResult.put(flipper, result);
    }

    /**
     * <p>
     * getFlipResult.
     * </p>
     * 
     * @param flipper  The Player who flipped the coin.
     * @return a String result - Heads or Tails.
     */
    public final String getFlipResult(final Player flipper) {
        return this.flipResult.get(flipper);
    }

    /**
     * <p>
     * clearFlipResult.
     * </p>
     */
    public final void clearFlipResult() {
        this.flipResult.clear();
    }

    /**
     * <p>
     * addTrigger.
     * </p>
     * 
     * @param t
     *            a {@link forge.card.trigger.Trigger} object.
     * @return a {@link forge.card.trigger.Trigger} object.
     */
    public final Trigger addTrigger(final Trigger t) {
        final Trigger newtrig = t.getCopyForHostCard(this);
        this.getCharacteristics().getTriggers().add(newtrig);
        return newtrig;
    }

    /**
     * 
     * moveTrigger.
     * 
     * @param t
     *            a Trigger
     */
    public final void moveTrigger(final Trigger t) {
        t.setHostCard(this);
        if (!this.getCharacteristics().getTriggers().contains(t)) {
            this.getCharacteristics().getTriggers().add(t);
        }
    }

    /**
     * <p>
     * removeTrigger.
     * </p>
     * 
     * @param t
     *            a {@link forge.card.trigger.Trigger} object.
     */
    public final void removeTrigger(final Trigger t) {
        this.getCharacteristics().getTriggers().remove(t);
    }

    /**
     * <p>
     * removeTrigger.
     * </p>
     * 
     * @param t
     *            a {@link forge.card.trigger.Trigger} object.
     *
     * @param state
     *            a {@link forge.CardCharacteristicName} object.
     */
    public final void removeTrigger(final Trigger t, final CardCharacteristicName state) {
        CardCharacteristics stateCharacteristics = this.getState(state);
        stateCharacteristics.getTriggers().remove(t);
    }

    /**
     * <p>
     * Getter for the field <code>triggers</code>.
     * </p>
     * 
     * @return a {@link java.util.ArrayList} object.
     */
    public final List<Trigger> getTriggers() {
        return this.getCharacteristics().getTriggers();
    }

    /**
     * <p>
     * Setter for the field <code>triggers</code>.
     * </p>
     * 
     * @param trigs
     *            a {@link java.util.ArrayList} object.
     */
    public final void setTriggers(final List<Trigger> trigs, boolean intrinsicOnly) {
        final List<Trigger> copyList = new CopyOnWriteArrayList<Trigger>();
        for (final Trigger t : trigs) {
            if (!intrinsicOnly || t.isIntrinsic()) {
                copyList.add(t.getCopyForHostCard(this));
            }
        }

        this.getCharacteristics().setTriggers(copyList);
    }

    /**
     * <p>
     * clearTriggersNew.
     * </p>
     */
    public final void clearTriggersNew() {
        this.getCharacteristics().getTriggers().clear();
    }

    /**
     * <p>
     * getTriggeringObject.
     * </p>
     * 
     * @param typeIn
     *            a {@link java.lang.String} object.
     * @return a {@link java.lang.Object} object.
     */
    public final Object getTriggeringObject(final String typeIn) {
        return this.triggeringObjects.get(typeIn);
    }

    /**
     * <p>
     * Getter for the field <code>sunburstValue</code>.
     * </p>
     * 
     * @return a int.
     */
    public final int getSunburstValue() {
        return this.sunburstValue;
    }

    // TODO: Append colors instead of replacing
    public final void setColorsPaid(final byte s) {
        this.colorsPaid = s;
    }

    /**
     * <p>
     * Getter for the field <code>colorsPaid</code>.
     * </p>
     * 
     * @return a String.
     */
    public final byte getColorsPaid() {
        return this.colorsPaid;
    }

    /**
     * <p>
     * Setter for the field <code>sunburstValue</code>.
     * </p>
     * 
     * @param valueIn
     *            a int.
     */
    public final void setSunburstValue(final int valueIn) {
        this.sunburstValue = valueIn;
    }

    /**
     * <p>
     * addXManaCostPaid.
     * </p>
     * 
     * @param n
     *            a int.
     */
    public final void addXManaCostPaid(final int n) {
        this.xManaCostPaid += n;
    }

    /**
     * <p>
     * Setter or the field <code>xManaCostPaid</code>.
     * </p>
     * 
     * @param n
     *            a int.
     */
    public final void setXManaCostPaid(final int n) {
        this.xManaCostPaid = n;
    }

    /**
     * <p>
     * Getter for the field <code>xManaCostPaid</code>.
     * </p>
     * 
     * @return a int.
     */
    public final int getXManaCostPaid() {
        return this.xManaCostPaid;
    }

    /**
     * @return the blockedThisTurn
     */
    public List<Card> getBlockedThisTurn() {
        return blockedThisTurn;
    }

    /**
     * @param attacker the blockedThisTurn to set
     */
    public void addBlockedThisTurn(Card attacker) {
        if (this.blockedThisTurn == null) {
            this.blockedThisTurn = new ArrayList<Card>();
        }
        this.blockedThisTurn.add(attacker);
    }

    /**
     * <p>
     * clearBlockedThisTurn.
     * </p>
     */
    public void clearBlockedThisTurn() {
        this.blockedThisTurn = null;
    }

    /**
     * @return the blockedByThisTurn
     */
    public List<Card> getBlockedByThisTurn() {
        return blockedByThisTurn;
    }

    /**
     * @param blocker the blockedByThisTurn to set
     */
    public void addBlockedByThisTurn(Card blocker) {
        if (this.blockedByThisTurn == null) {
            this.blockedByThisTurn = new ArrayList<Card>();
        }
        this.blockedByThisTurn.add(blocker);
    }

    /**
     * <p>
     * clearBlockedByThisTurn.
     * </p>
     */
    public void clearBlockedByThisTurn() {
        this.blockedByThisTurn = null;
    }

    /**
     * <p>
     * canAnyPlayerActivate.
     * </p>
     * 
     * @return a boolean.
     */
    public final boolean canAnyPlayerActivate() {
        for (final SpellAbility s : this.getCharacteristics().getSpellAbility()) {
            if (s.getRestrictions().isAnyPlayer()) {
                return true;
            }
        }
        return false;
    }

    /**
     * a Card that this Card must block if able in an upcoming combat. This is
     * cleared at the end of each turn.
     * 
     * @param c
     *            Card to block
     * 
     * @since 1.1.6
     */
    public final void addMustBlockCard(final Card c) {
        if (mustBlockCards == null) {
            mustBlockCards = new ArrayList<Card>();
        }
        this.mustBlockCards.add(c);
    }

    /**
     * get the Card that this Card must block this combat.
     * 
     * @return the Cards to block (if able)
     * 
     * @since 1.1.6
     */
    public final ArrayList<Card> getMustBlockCards() {
        return this.mustBlockCards;
    }

    /**
     * clear the list of Cards that this Card must block this combat.
     * 
     * @since 1.1.6
     */
    public final void clearMustBlockCards() {
        this.mustBlockCards = null;
    }

    /**
     * <p>
     * Getter for the field <code>clones</code>.
     * </p>
     * 
     * @return a {@link java.util.ArrayList} object.
     */
    public final List<Card> getClones() {
        return this.clones;
    }

    /**
     * <p>
     * Setter for the field <code>clones</code>.
     * </p>
     * 
     * @param c
     *            a {@link java.util.ArrayList} object.
     */
    public final void setClones(final Collection<Card> c) {
        this.clones.clear();
        this.clones.addAll(c);
    }

    /**
     * <p>
     * addClone.
     * </p>
     * 
     * @param c
     *            a {@link forge.Card} object.
     */
    public final void addClone(final Card c) {
        this.clones.add(c);
    }

    /**
     * <p>
     * clearClones.
     * </p>
     */
    public final void clearClones() {
        this.clones.clear();
    }

    /**
     * <p>
     * Getter for the field <code>cloneOrigin</code>.
     * </p>
     * 
     * @return a {@link forge.Card} object.
     */
    public final Card getCloneOrigin() {
        return this.cloneOrigin;
    }

    /**
     * <p>
     * Setter for the field <code>cloneOrigin</code>.
     * </p>
     * 
     * @param name
     *            a {@link forge.Card} object.
     */
    public final void setCloneOrigin(final Card name) {
        this.cloneOrigin = name;
    }

    /**
     * <p>
     * Getter for the field <code>sacrificeAtEOT</code>.
     * </p>
     * 
     * @return a boolean.
     */
    public final boolean getSacrificeAtEOT() {
        return this.hasKeyword("At the beginning of the end step, sacrifice CARDNAME.");
    }

    /**
     * <p>
     * hasFirstStrike.
     * </p>
     * 
     * @return a boolean.
     */
    public final boolean hasFirstStrike() {
        return this.hasKeyword("First Strike");
    }

    /**
     * <p>
     * hasDoubleStrike.
     * </p>
     * 
     * @return a boolean.
     */
    public final boolean hasDoubleStrike() {
        return this.hasKeyword("Double Strike");
    }

    /**
     * <p>
     * hasSecondStrike.
     * </p>
     * 
     * @return a boolean.
     */
    public final boolean hasSecondStrike() {
        return this.hasDoubleStrike() || !this.hasFirstStrike();
    }

    /**
     * Can have counters placed on it.
     * 
     * @param type
     *            the counter name
     * @return true, if successful
     */
    public final boolean canReceiveCounters(final CounterType type) {
        if (this.hasKeyword("CARDNAME can't have counters placed on it.")) {
            return false;
        }
        if (this.isCreature() && type == CounterType.M1M1) {
            for (final Card c : this.getController().getCreaturesInPlay()) { // look for Melira, Sylvok Outcast
                if (c.hasKeyword("Creatures you control can't have -1/-1 counters placed on them.")) {
                    return false;
                }
            }
        }
        return true;
    }

    public final int getTotalCountersToAdd(final CounterType counterType, final int baseAmount, final boolean applyMultiplier) {
        if (this.canReceiveCounters(counterType)) {
            final int multiplier = applyMultiplier ? this.getController().getCounterDoublersMagnitude(counterType) : 1;
            return multiplier * baseAmount;
        }
        return 0;
    }

    public final void addCounter(final CounterType counterType, final int n, final boolean applyMultiplier) {
        final int addAmount = getTotalCountersToAdd(counterType, n, applyMultiplier);
        if ( addAmount == 0 )
            return;

        Integer oldValue = this.counters.get(counterType);
        int newValue = addAmount + (oldValue == null ? 0 : oldValue.intValue());
        this.counters.put(counterType, Integer.valueOf(newValue));

        // Run triggers
        final Map<String, Object> runParams = new TreeMap<String, Object>();
        runParams.put("Card", this);
        runParams.put("CounterType", counterType);
        for (int i = 0; i < addAmount; i++) {
            getGame().getTriggerHandler().runTrigger(TriggerType.CounterAdded, runParams, false);
        }

        // play the Add Counter sound
        getGame().fireEvent(new GameEventCounterAdded(addAmount));

        this.updateObservers();
    }

    /**
     * <p>
     * addCountersAddedBy.
     * </p>
     * @param source    the card adding the counters to this card
     * @param counterType    the counter type added
     * @param counterAmount    the amount of counters added
     */
    public final void addCountersAddedBy(final Card source, final CounterType counterType, final int counterAmount) {
        final Map<CounterType, Integer> counterMap = new TreeMap<CounterType, Integer>();
        counterMap.put(counterType, counterAmount);
        this.countersAddedBy.put(source, counterMap);
    }

    /**
     * <p>
     * getCountersAddedBy.
     * </p>
     * @param source    the card the counters were added by
     * @param counterType    the counter type added
     * @return the amount of counters added.
     */
    public final int getCountersAddedBy(final Card source, final CounterType counterType) {
        int counterAmount = 0;
        if (this.countersAddedBy.containsKey(source)) {
            final Map<CounterType, Integer> counterMap = this.countersAddedBy.get(source);
            counterAmount = counterMap.containsKey(counterType) ? counterMap.get(counterType) : 0;
            this.countersAddedBy.remove(source);
        }
        return counterAmount;
    }

    /**
     * <p>
     * subtractCounter.
     * </p>
     * 
     * @param counterName
     *            a {@link forge.CounterType} object.
     * @param n
     *            a int.
     */
    public final void subtractCounter(final CounterType counterName, final int n) {
        Integer oldValue = this.counters.get(counterName);
        int newValue = oldValue == null ? 0 : Math.max(oldValue.intValue() - n, 0);

        final int delta = (oldValue == null ? 0 : oldValue.intValue()) - newValue;
        if (delta == 0) {
            return;
        }

        if (newValue > 0) {
            this.counters.put(counterName, Integer.valueOf(newValue));
        } else {
            this.counters.remove(counterName);
        }

        // Run triggers
        final Map<String, Object> runParams = new TreeMap<String, Object>();
        runParams.put("Card", this);
        runParams.put("CounterType", counterName);
        runParams.put("NewCounterAmount", newValue > 0 ? newValue : 0);
        for (int i = 0; i < delta; i++) {
            getGame().getTriggerHandler().runTrigger(TriggerType.CounterRemoved, runParams, false);
        }

        if (counterName.equals(CounterType.TIME) && (newValue == 0)) {
            final boolean hasVanish = CardFactoryUtil.hasKeyword(this, "Vanishing") != -1;

            if (hasVanish && this.isInPlay()) {
                getGame().getAction().sacrifice(this, null);
            }

        }

        // Play the Subtract Counter sound
        getGame().fireEvent(new GameEventCounterRemoved(delta));

        this.updateObservers();
    }

    /**
     * <p>
     * Getter for the field <code>counters</code>.
     * </p>
     * 
     * @param counterName
     *            a {@link forge.CounterType} object.
     * @return a int.
     */
    public final int getCounters(final CounterType counterName) {
        Integer value = this.counters.get(counterName);
        return value == null ? 0 : value.intValue();
    }

    // get all counters from a card
    /**
     * <p>
     * Getter for the field <code>counters</code>.
     * </p>
     * 
     * @return a Map object.
     * @since 1.0.15
     */
    public final Map<CounterType, Integer> getCounters() {
        return this.counters;
    }

    /**
     * <p>
     * hasCounters.
     * </p>
     * 
     * @return a boolean.
     */
    public final boolean hasCounters() {
        return !this.counters.isEmpty();
    }



    // get all counters from a card
    /**
     * <p>
     * Setter for the field <code>counters</code>.
     * </p>
     * 
     * @param allCounters
     *            a Map object.
     * @since 1.0.15
     */
    public final void setCounters(final Map<CounterType, Integer> allCounters) {
        this.counters = allCounters;
    }

    // get all counters from a card
    /**
     * <p>
     * clearCounters.
     * </p>
     * 
     * @since 1.0.15
     */
    public final void clearCounters() {
        this.counters.clear();
    }

    /**
     * hasLevelUp() - checks to see if a creature has the "Level up" ability
     * introduced in Rise of the Eldrazi.
     * 
     * @return true if this creature can "Level up", false otherwise
     */
    public final boolean hasLevelUp() {
        return this.levelUp;
    }

    /**
     * <p>
     * Setter for the field <code>levelUp</code>.
     * </p>
     * 
     * @param b
     *            a boolean.
     */
    public final void setLevelUp(final boolean b) {
        this.levelUp = b;
    }

    /**
     * <p>
     * getSVar.
     * </p>
     * 
     * @param var
     *            a {@link java.lang.String} object.
     * @return a {@link java.lang.String} object.
     */
    public final String getSVar(final String var) {
        return this.getCharacteristics().getSVar(var);
    }

    /**
     * <p>
     * hasSVar.
     * </p>
     * 
     * @param var
     *            a {@link java.lang.String} object.
     */
    public final boolean hasSVar(final String var) {
        return this.getCharacteristics().hasSVar(var);
    }

    /**
     * <p>
     * setSVar.
     * </p>
     * 
     * @param var
     *            a {@link java.lang.String} object.
     * @param str
     *            a {@link java.lang.String} object.
     */
    public final void setSVar(final String var, final String str) {
        this.getCharacteristics().setSVar(var, str);
    }

    /**
     * <p>
     * getSVars.
     * </p>
     * 
     * @return a Map object.
     */
    public final Map<String, String> getSVars() {
        return this.getCharacteristics().getSVars();
    }

    /**
     * <p>
     * setSVars.
     * </p>
     * 
     * @param newSVars
     *            a Map object.
     */
    public final void setSVars(final Map<String, String> newSVars) {
        this.getCharacteristics().setSVars(newSVars);
    }

    /**
     * <p>
     * sumAllCounters.
     * </p>
     * 
     * @return a int.
     */
    public final int sumAllCounters() {
        int count = 0;
        for (final Integer value2 : this.counters.values()) {
            count += value2.intValue();
        }
        return count;
    }


    /**
     * <p>
     * Getter for the field <code>turnInZone</code>.
     * </p>
     * 
     * @return a int.
     */
    public final int getTurnInZone() {
        return this.turnInZone;
    }

    /**
     * <p>
     * Setter for the field <code>turnInZone</code>.
     * </p>
     * 
     * @param turn
     *            a int.
     */
    public final void setTurnInZone(final int turn) {
        this.turnInZone = turn;
    }

    /**
     * <p>
     * Setter for the field <code>echoCost</code>.
     * </p>
     * 
     * @param s
     *            a {@link java.lang.String} object.
     */
    public final void setEchoCost(final String s) {
        this.echoCost = s;
    }

    /**
     * <p>
     * Getter for the field <code>echoCost</code>.
     * </p>
     * 
     * @return a {@link java.lang.String} object.
     */
    public final String getEchoCost() {
        return this.echoCost;
    }

    /**
     * <p>
     * Setter for the field <code>manaCost</code>.
     * </p>
     * 
     * @param s
     *            a {@link java.lang.String} object.
     */
    public final void setManaCost(final ManaCost s) {
        this.getCharacteristics().setManaCost(s);
    }

    /**
     * <p>
     * Getter for the field <code>manaCost</code>.
     * </p>
     * 
     * @return a {@link java.lang.String} object.
     */
    public final ManaCost getManaCost() {
        return this.getCharacteristics().getManaCost();
    }
    

    /**
     * <p>
     * addColor.
     * </p>
     * 
     * @param s
     *            a {@link java.lang.String} object.
     */
    public final void addColor(String s) {
        if (s.equals("")) {
            s = "0";
        }
        this.getCharacteristics().getCardColor().add(new CardColor(s, false, true));
    }

    /**
     * <p>
     * addColor.
     * </p>
     * 
     * @param s
     *            a {@link java.lang.String} object.
     * @param c
     *            a {@link forge.Card} object.
     * @param addToColors
     *            a boolean.
     * @param bIncrease
     *            a boolean.
     * @return a long.
     */
    public final long addColor(final String s, final boolean addToColors, final boolean bIncrease) {
        if (bIncrease) {
            CardColor.increaseTimestamp();
        }
        this.getCharacteristics().getCardColor().add(new CardColor(s, addToColors, false));
        return CardColor.getTimestamp();
    }

    /**
     * <p>
     * removeColor.
     * </p>
     * 
     * @param s
     *            a {@link java.lang.String} object.
     * @param c
     *            a {@link forge.Card} object.
     * @param addTo
     *            a boolean.
     * @param timestampIn
     *            a long.
     */
    public final void removeColor(final String s, final Card c, final boolean addTo, final long timestampIn) {
        CardColor removeCol = null;
        for (final CardColor cc : this.getCharacteristics().getCardColor()) {
            if (cc.equals(s, c, addTo, timestampIn)) {
                removeCol = cc;
            }
        }

        if (removeCol != null) {
            this.getCharacteristics().getCardColor().remove(removeCol);
        }
    }

    /**
     * <p>
     * determineColor.
     * </p>
     * 
     * @return a {@link forge.CardColor} object.
     */
    public final ColorSet determineColor() {
        if (this.isImmutable()) {
            return ColorSet.getNullColor();
        }

        final List<CardColor> globalChanges = getOwner() == null ? new ArrayList<CardColor>() : getOwner().getGame().getColorChanger().getColorChanges();
        return this.determineColor(globalChanges);
    }

    /**
     * <p>
     * setColor.
     * </p>
     * 
     * @param colors
     *            a {@link java.util.ArrayList} object.
     */
    public final void setColor(final Iterable<CardColor> colors) {
        this.getCharacteristics().setCardColor(colors);
    }

    /**
     * <p>
     * getColor.
     * </p>
     * 
     * @return a {@link java.util.ArrayList} object.
     */
    public final List<CardColor> getColor() {
        return this.getCharacteristics().getCardColor();
    }

    /**
     * 
     * TODO Write javadoc for this method.
     * 
     * @param globalChanges
     *            an ArrayList<CardColor>
     * @return a CardColor
     */
    final ColorSet determineColor(final List<CardColor> globalChanges) {
        byte colors = 0;
        int i = this.getCharacteristics().getCardColor().size() - 1;
        int j = -1;
        if (globalChanges != null) {
            j = globalChanges.size() - 1;
        }
        // if both have changes, see which one is most recent
        while ((i >= 0) && (j >= 0)) {
            CardColor cc = null;
            if (this.getCharacteristics().getCardColor().get(i).getStamp() > globalChanges.get(j).getStamp()) {
                // Card has a more recent color stamp
                cc = this.getCharacteristics().getCardColor().get(i);
                i--;
            } else {
                // Global effect has a more recent color stamp
                cc = globalChanges.get(j);
                j--;
            }

            colors |= cc.getColorMask();
            if (!cc.isAdditional()) {
                return ColorSet.fromMask(colors);
            }
        }
        while (i >= 0) {
            final CardColor cc = this.getCharacteristics().getCardColor().get(i);
            i--;
            colors |= cc.getColorMask();
            if (!cc.isAdditional()) {
                return ColorSet.fromMask(colors);
            }
        }
        while (j >= 0) {
            final CardColor cc = globalChanges.get(j);
            j--;
            colors |= cc.getColorMask();
            if (!cc.isAdditional()) {
                return ColorSet.fromMask(colors);
            }
        }

        return ColorSet.fromMask(colors);
    }

    /**
     * <p>
     * Getter for the field <code>chosenPlayer</code>.
     * </p>
     * 
     * @return a Player
     * @since 1.1.6
     */
    public final Player getChosenPlayer() {
        return this.chosenPlayer;
    }

    /**
     * <p>
     * Setter for the field <code>chosenNumber</code>.
     * </p>
     * 
     * @param p
     *            an int
     * @since 1.1.6
     */
    public final void setChosenPlayer(final Player p) {
        this.chosenPlayer = p;
    }

    /**
     * <p>
     * Getter for the field <code>chosenNumber</code>.
     * </p>
     * 
     * @return an int
     */
    public final int getChosenNumber() {
        return this.chosenNumber;
    }

    /**
     * <p>
     * Setter for the field <code>chosenNumber</code>.
     * </p>
     * 
     * @param i
     *            an int
     */
    public final void setChosenNumber(final int i) {
        this.chosenNumber = i;
    }

    // used for cards like Belbe's Portal, Conspiracy, Cover of Darkness, etc.
    /**
     * <p>
     * Getter for the field <code>chosenType</code>.
     * </p>
     * 
     * @return a {@link java.lang.String} object.
     */
    public final String getChosenType() {
        return this.chosenType;
    }

    /**
     * <p>
     * Setter for the field <code>chosenType</code>.
     * </p>
     * 
     * @param s
     *            a {@link java.lang.String} object.
     */
    public final void setChosenType(final String s) {
        this.chosenType = s;
    }

    /**
     * <p>
     * Getter for the field <code>chosenColor</code>.
     * </p>
     * 
     * @return an ArrayList<String> object.
     */
    public final List<String> getChosenColor() {
        return this.chosenColor;
    }

    /**
     * <p>
     * Setter for the field <code>chosenColor</code>.
     * </p>
     * 
     * @param s
     *            an ArrayList<String> object.
     */
    public final void setChosenColor(final List<String> s) {
        this.chosenColor = s;
    }

    /**
     * <p>
     * Getter for the field <code>chosenCard</code>.
     * </p>
     * 
     * @return an ArrayList<Card> object.
     */
    public final List<Card> getChosenCard() {
        return this.chosenCard;
    }

    /**
     * <p>
     * Setter for the field <code>chosenCard</code>.
     * </p>
     * 
     * @param c
     *            an ArrayList<String> object.
     */
    public final void setChosenCard(final List<Card> c) {
        this.chosenCard = c;
    }

    // used for cards like Meddling Mage...
    /**
     * <p>
     * Getter for the field <code>namedCard</code>.
     * </p>
     * 
     * @return a {@link java.lang.String} object.
     */
    public final String getNamedCard() {
        return this.namedCard;
    }

    /**
     * <p>
     * Setter for the field <code>namedCard</code>.
     * </p>
     * 
     * @param s
     *            a {@link java.lang.String} object.
     */
    public final void setNamedCard(final String s) {
        this.namedCard = s;
    }

    /**
     * <p>
     * Setter for the field <code>drawnThisTurn</code>.
     * </p>
     * 
     * @param b
     *            a boolean.
     */
    public final void setDrawnThisTurn(final boolean b) {
        this.drawnThisTurn = b;
    }

    /**
     * <p>
     * Getter for the field <code>drawnThisTurn</code>.
     * </p>
     * 
     * @return a boolean.
     */
    public final boolean getDrawnThisTurn() {
        return this.drawnThisTurn;
    }

    /**
     * get a list of Cards this card has gained control of.
     * <p/>
     * used primarily with AbilityFactory_GainControl
     * 
     * @return a list of cards this card has gained control of
     */
    public final List<Card> getGainControlTargets() {
        return this.gainControlTargets;
    }

    /**
     * add a Card to the list of Cards this card has gained control of.
     * <p/>
     * used primarily with AbilityFactory_GainControl
     * 
     * @param c
     *            a {@link forge.Card} object.
     */
    public final void addGainControlTarget(final Card c) {
        this.gainControlTargets.add(c);
    }

    /**
     * clear the list of Cards this card has gained control of.
     * <p/>
     * used primarily with AbilityFactory_GainControl
     */
    public final void clearGainControlTargets() {
        this.gainControlTargets.clear();
    }

    /**
     * get the commands to be executed to lose control of Cards this card has
     * gained control of.
     * <p/>
     * used primarily with AbilityFactory_GainControl (Old Man of the Sea
     * specifically)
     * 
     * @return a {@link java.util.ArrayList} object.
     */
    public final List<Command> getGainControlReleaseCommands() {
        return this.gainControlReleaseCommands;
    }

    /**
     * set a command to be executed to lose control of Cards this card has
     * gained control of.
     * <p/>
     * used primarily with AbilityFactory_GainControl (Old Man of the Sea
     * specifically)
     * 
     * @param c
     *            the Command to be executed
     */
    public final void addGainControlReleaseCommand(final Command c) {
        this.gainControlReleaseCommands.add(c);
    }

    /**
     * <p>
     * clearGainControlReleaseCommands.
     * </p>
     */
    public final void clearGainControlReleaseCommands() {
        this.gainControlReleaseCommands.clear();
    }

    /**
     * <p>
     * getSpellText.
     * </p>
     * 
     * @return a {@link java.lang.String} object.
     */
    public final String getSpellText() {
        return this.text;
    }

    /**
     * <p>
     * Setter for the field <code>text</code>.
     * </p>
     * 
     * @param t
     *            a {@link java.lang.String} object.
     */
    public final void setText(final String t) {
        this.text = t;
    }

    // get the text that should be displayed
    /**
     * <p>
     * Getter for the field <code>text</code>.
     * </p>
     * 
     * @return a {@link java.lang.String} object.
     */
    public String getText() {
        final StringBuilder sb = new StringBuilder();

        // Vanguard Modifiers
        if (this.isType("Vanguard")) {
            sb.append("Hand Modifier: ").append(getRules().getHand());
            sb.append("\r\nLife Modifier: ").append(getRules().getLife());
            sb.append("\r\n\r\n");
        }
        sb.append(this.getAbilityText());

        String nonAbilityText = this.getNonAbilityText();
        if (this.getAmountOfKeyword("CARDNAME can block an additional creature.") > 1) {
            final StringBuilder ab = new StringBuilder();
            ab.append("CARDNAME can block an additional ");
            ab.append(this.getAmountOfKeyword("CARDNAME can block an additional creature."));
            ab.append(" creatures.");
            nonAbilityText = nonAbilityText.replaceFirst("CARDNAME can block an additional creature.", ab.toString());
            nonAbilityText = nonAbilityText.replaceAll("CARDNAME can block an additional creature.", "");
            nonAbilityText = nonAbilityText.replaceAll("\r\n\r\n\r\n", "");
        }
        if (nonAbilityText.length() > 0) {
            sb.append("\r\n \r\nNon ability features: \r\n");
            sb.append(nonAbilityText.replaceAll("CARDNAME", this.getName()));
        }

        // Remembered cards
        if (this.rememberedObjects.size() > 0) {
            sb.append("\r\nRemembered: \r\n");
            for (final Object o : this.rememberedObjects) {
                if (o instanceof Card) {
                    final Card c = (Card) o;
                    if (c.isFaceDown()) {
                        sb.append("Face Down ");
                    } else {
                        sb.append(c.getName());
                    }
                    sb.append("(");
                    sb.append(c.getUniqueNumber());
                    sb.append(")");
                } else if (o != null) {
                    sb.append(o.toString());
                }
                sb.append("\r\n");
            }
        }

        if (this.chosenPlayer != null) {
            sb.append("\r\n[Chosen player: ");
            sb.append(this.getChosenPlayer());
            sb.append("]\r\n");
        }

        if (this.hauntedBy.size() != 0) {
            sb.append("Haunted by: ");
            for (final Card c : this.hauntedBy) {
                sb.append(c).append(",");
            }
            sb.deleteCharAt(sb.length() - 1);
            sb.append("\r\n");
        }

        if (this.haunting != null) {
            sb.append("Haunting: ").append(this.haunting);
            sb.append("\r\n");
        }

        if (this.pairedWith != null) {
            sb.append("\r\n \r\nPaired With: ").append(this.pairedWith);
            sb.append("\r\n");
        }

        if (this.characteristicsMap.get(CardCharacteristicName.Cloner) != null) {
            sb.append("\r\nCloned by: ").append(this.characteristicsMap.get(CardCharacteristicName.Cloner).getName()).append(" (")
                    .append(this.getUniqueNumber()).append(")");
        }

        return sb.toString();
    }

    // get the text that does not belong to a cards abilities (and is not really
    // there rules-wise)
    /**
     * <p>
     * getNonAbilityText.
     * </p>
     * 
     * @return a {@link java.lang.String} object.
     */
    public final String getNonAbilityText() {
        final StringBuilder sb = new StringBuilder();
        final ArrayList<String> keyword = this.getHiddenExtrinsicKeyword();

        sb.append(this.keywordsToText(keyword));

        return sb.toString();
    }

    // convert a keyword list to the String that should be displayed ingame
    /**
     * <p>
     * keywordsToText.
     * </p>
     * 
     * @param keywords
     *            a {@link java.util.ArrayList} object.
     * @return a {@link java.lang.String} object.
     */
    public final String keywordsToText(final ArrayList<String> keywords) {
        final StringBuilder sb = new StringBuilder();
        final StringBuilder sbLong = new StringBuilder();
        final StringBuilder sbMana = new StringBuilder();

        for (int i = 0; i < keywords.size(); i++) {
            String keyword = keywords.get(i).toString();
            if (keyword.startsWith("Permanents don't untap during their controllers' untap steps")
                    || keyword.startsWith("PreventAllDamageBy")
                    || keyword.startsWith("CantBlock")
                    || keyword.startsWith("CantBeBlockedBy")
                    || keyword.startsWith("CantEquip")) {
                continue;
            }
            if (keyword.startsWith("CostChange")) {
                final String[] k = keyword.split(":");
                if (k.length > 8) {
                    sbLong.append(k[8]).append("\r\n");
                }
            /*} else if (keyword.startsWith("AdjustLandPlays")) {
                final String[] k = keyword.split(":");
                if (k.length > 3) {
                    sbLong.append(k[3]).append("\r\n");
                }*/
            } else if (keyword.startsWith("etbCounter")) {
                final String[] p = keyword.split(":");
                final StringBuilder s = new StringBuilder();
                if (p.length > 4) {
                    s.append(p[4]);
                } else {
                    final CounterType counter = CounterType.valueOf(p[1]);
                    final String numCounters = p[2];
                    s.append(this.getName());
                    s.append(" enters the battlefield with ");
                    s.append(numCounters);
                    s.append(" ");
                    s.append(counter.getName());
                    s.append(" counter");
                    if ("1" != numCounters) {
                        s.append("s");
                    }
                    s.append(" on it.");
                }
                sbLong.append(s).append("\r\n");
            } else if (keyword.startsWith("Protection:")) {
                final String[] k = keywords.get(i).split(":");
                sbLong.append(k[2]).append("\r\n");
            } else if (keyword.startsWith("Creatures can't attack unless their controller pays")) {
                final String[] k = keywords.get(i).split(":");
                if (!k[3].equals("no text")) {
                    sbLong.append(k[3]).append("\r\n");
                }
            } else if (keyword.startsWith("Enchant")) {
                String k = keywords.get(i);
                k = k.replace("Curse", "");
                sbLong.append(k).append("\r\n");
            } else if (keyword.startsWith("Fading")
                    || keyword.startsWith("Ripple") || keywords.get(i).startsWith("Unearth")
                    || keyword.startsWith("Vanishing") || keywords.get(i).startsWith("Madness")) {
                String k = keywords.get(i);
                k = k.replace(":", " ");
                sbLong.append(k).append("\r\n");
            } else if (keyword.startsWith("Devour")) {
                final String[] parts = keyword.split(":");
                final String extra = parts.length > 2 ? parts[2] : "";
                final String devour = "Devour " + parts[1] + extra;
                sbLong.append(devour).append("\r\n");
            } else if (keyword.startsWith("Morph")) {
                sbLong.append("Morph");
                if (keyword.contains(":")) {
                    final Cost mCost = new Cost(keywords.get(i).substring(6), true);
                    if (!mCost.isOnlyManaCost()) {
                        sbLong.append(" -");
                    }
                    sbLong.append(" ").append(mCost.toString()).delete(sbLong.length() - 2, sbLong.length());
                    if (!mCost.isOnlyManaCost()) {
                        sbLong.append(".");
                    }
                    sbLong.append("\r\n");
                }
            } else if (keyword.startsWith("Echo")) {
                sbLong.append("Echo ");
                final String[] upkeepCostParams = keywords.get(i).split(":");
                final String cost = upkeepCostParams[1];
                final String costDesc = upkeepCostParams.length > 2 ? "- " + upkeepCostParams[2] : cost;
                sbLong.append(costDesc);
                sbLong.append("\r\n");
            } else if (keyword.startsWith("Cumulative upkeep")) {
                sbLong.append("Cumulative upkeep ");
                final String[] upkeepCostParams = keywords.get(i).split(":");
                final String cost = upkeepCostParams[1];
                final String costDesc = upkeepCostParams.length > 2 ? "- " + upkeepCostParams[2] : cost;
                sbLong.append(costDesc);
                sbLong.append("\r\n");
            } else if (keyword.startsWith("Amplify")) {
                sbLong.append("Amplify ");
                final String[] ampParams = keywords.get(i).split(":");
                final String magnitude = ampParams[1];
                sbLong.append(magnitude);
                sbLong.append("(As this creature enters the battlefield, put a +1/+1 counter on it for each ");
                sbLong.append(ampParams[2].replace(",", " and/or ")).append(" card you reveal in your hand.)");
                sbLong.append("\r\n");
            }  else if (keyword.startsWith("Alternative Cost")) {
                sbLong.append("Has alternative cost.");
            } else if (keyword.startsWith("AlternateAdditionalCost")) {
                final String costString1 = keyword.split(":")[1];
                final String costString2 = keyword.split(":")[2];
                final Cost cost1 = new Cost(costString1, false);
                final Cost cost2 = new Cost(costString2, false);
                sbLong.append("As an additional cost to cast " + this.getName() + ", " + cost1.toSimpleString()
                        + " or pay " + cost2.toSimpleString() + ".\r\n");
            } else if (keyword.startsWith("Kicker")) {
                final Cost cost = new Cost(keywords.get(i).substring(7), false);
                sbLong.append("Kicker " + cost.toSimpleString() + "\r\n");
            } else if (keyword.endsWith(".") && !keywords.get(i).startsWith("Haunt")) {
                sbLong.append(keywords.get(i).toString()).append("\r\n");
            } else if (keyword.contains("At the beginning of your upkeep, ")
                    && keyword.contains(" unless you pay")) {
                sbLong.append(keywords.get(i).toString()).append("\r\n");
            } else if (keyword.toString().contains("tap: add ")) {
                sbMana.append(keywords.get(i).toString()).append("\r\n");
            } else if (keyword.startsWith("Modular") || keyword.startsWith("Soulshift") || keyword.startsWith("Bloodthirst")
                    || keyword.startsWith("ETBReplacement") || keyword.startsWith("MayEffectFromOpeningHand")) {
                continue;
            } else if (keyword.startsWith("Provoke")) {
                sbLong.append(keywords.get(i));
                sbLong.append(" (When this attacks, you may have target creature ");
                sbLong.append("defending player controls untap and block it if able.)");
            } else if (keyword.contains("Haunt")) {
                sb.append("\r\nHaunt (");
                if (this.isCreature()) {
                    sb.append("When this creature dies, exile it haunting target creature.");
                } else {
                    sb.append("When this spell card is put into a graveyard after resolving, ");
                    sb.append("exile it haunting target creature.");
                }
                sb.append(")");
                continue;
            } else if (keyword.equals("Convoke")) {
                if (sb.length() != 0) {
                    sb.append("\r\n");
                }
                sb.append("Convoke (Each creature you tap while casting this spell reduces its cost by 1 or by one mana of that creature's color.)");
            } else if (keyword.endsWith(" offering")) {
                String offeringType = keyword.split(" ")[0];
                if (sb.length() != 0) {
                    sb.append("\r\n");
                }
                sbLong.append(keywords.get(i));
                sbLong.append(" (You may cast this card any time you could cast an instant by sacrificing a ");
                sbLong.append(offeringType);
                sbLong.append("and paying the difference in mana costs between this and the sacrificed ");
                sbLong.append(offeringType);
                sbLong.append(". Mana cost includes color.)");
            } else if (keyword.startsWith("Soulbond")) {
                sbLong.append(keywords.get(i));
                sbLong.append(" (You may pair this creature ");
                sbLong.append("with another unpaired creature when either ");
                sbLong.append("enters the battlefield. They remain paired for ");
                sbLong.append("as long as you control both of them)");
            } else if (keyword.startsWith("Equip")) {
                // keyword parsing takes care of adding a proper description
                continue;
            } else {
                if ((i != 0) && (sb.length() != 0)) {
                    sb.append(", ");
                }
                sb.append(keyword);
            }
        }
        if (sb.length() > 0) {
            sb.append("\r\n");
            if (sbLong.length() > 0) {
                sb.append("\r\n");
            }
        }
        if (sbLong.length() > 0) {
            sbLong.append("\r\n");
        }
        sb.append(sbLong);
        sb.append(sbMana);

        return sb.toString();
    }

    // get the text of the abilities of a card
    /**
     * <p>
     * getAbilityText.
     * </p>
     * 
     * @return a {@link java.lang.String} object.
     */
    public String getAbilityText() {
        if (this.isInstant() || this.isSorcery()) {
            final StringBuilder sb = this.abilityTextInstantSorcery();

            if (this.haunting != null) {
                sb.append("Haunting: ").append(this.haunting);
                sb.append("\r\n");
            }

            while (sb.toString().endsWith("\r\n")) {
                sb.delete(sb.lastIndexOf("\r\n"), sb.lastIndexOf("\r\n") + 3);
            }

            return sb.toString().replaceAll("CARDNAME", this.getName());
        }

        final StringBuilder sb = new StringBuilder();
        final ArrayList<String> keyword = this.getUnhiddenKeyword();

        sb.append(this.keywordsToText(keyword));

        // Give spellText line breaks for easier reading
        sb.append("\r\n");
        sb.append(this.text.replaceAll("\\\\r\\\\n", "\r\n"));
        sb.append("\r\n");

        // Triggered abilities
        for (final Trigger trig : this.getCharacteristics().getTriggers()) {
            if (!trig.isSecondary()) {
                sb.append(trig.toString() + "\r\n");
            }
        }

        // Replacement effects
        for (final ReplacementEffect replacementEffect : this.getCharacteristics().getReplacementEffects()) {
            if (!replacementEffect.isSecondary()) {
                sb.append(replacementEffect.toString() + "\r\n");
            }
        }

        // static abilities
        for (final StaticAbility stAb : this.getCharacteristics().getStaticAbilities()) {
            sb.append(stAb.toString() + "\r\n");
        }

        final ArrayList<String> addedManaStrings = new ArrayList<String>();
        boolean primaryCost = true;
        for (final SpellAbility sa : this.getSpellAbilities()) {
            // only add abilities not Spell portions of cards
            if (!this.isPermanent()) {
                continue;
            }

            if ((sa instanceof SpellPermanent) && primaryCost && !this.isAura()) {
                // For Alt costs, make sure to display the cost!
                primaryCost = false;
                continue;
            }

            final String sAbility = sa.toString();

            if (sa.getManaPart() != null) {
                if (addedManaStrings.contains(sAbility)) {
                    continue;
                }
                addedManaStrings.add(sAbility);
            }

            if ((sa instanceof SpellPermanent) && !this.isAura()) {
                sb.insert(0, "\r\n");
                sb.insert(0, sAbility);
            } else if (!sAbility.endsWith(this.getName())) {
                sb.append(sAbility);
                sb.append("\r\n");
            }
        }

        // NOTE:
        if (sb.toString().contains(" (NOTE: ")) {
            sb.insert(sb.indexOf("(NOTE: "), "\r\n");
        }
        if (sb.toString().contains("(NOTE: ") && sb.toString().contains(".) ")) {
            sb.insert(sb.indexOf(".) ") + 3, "\r\n");
        }

        // replace triple line feeds with double line feeds
        int start;
        final String s = "\r\n\r\n\r\n";
        while (sb.toString().contains(s)) {
            start = sb.lastIndexOf(s);
            if ((start < 0) || (start >= sb.length())) {
                break;
            }
            sb.replace(start, start + 4, "\r\n");
        }

        return sb.toString().replaceAll("CARDNAME", this.getName()).trim();
    } // getText()

    /**
     * TODO: Write javadoc for this method.
     * 
     * @return
     */
    private StringBuilder abilityTextInstantSorcery() {
        final String s = this.getSpellText();
        final StringBuilder sb = new StringBuilder();

        // Give spellText line breaks for easier reading
        sb.append(s.replaceAll("\\\\r\\\\n", "\r\n"));

        // NOTE:
        if (sb.toString().contains(" (NOTE: ")) {
            sb.insert(sb.indexOf("(NOTE: "), "\r\n");
        }
        if (sb.toString().contains("(NOTE: ") && sb.toString().endsWith(".)") && !sb.toString().endsWith("\r\n")) {
            sb.append("\r\n");
        }

        // Add SpellAbilities
        for (final SpellAbility element : this.getSpellAbilities()) {
            sb.append(element.toString() + "\r\n");
        }

        // Add Keywords
        final List<String> kw = this.getKeyword();

        // Triggered abilities
        for (final Trigger trig : this.getCharacteristics().getTriggers()) {
            if (!trig.isSecondary()) {
                sb.append(trig.toString() + "\r\n");
            }
        }

        // Replacement effects
        for (final ReplacementEffect replacementEffect : this.getCharacteristics().getReplacementEffects()) {
            sb.append(replacementEffect.toString() + "\r\n");
        }

        // static abilities
        for (final StaticAbility stAb : this.getCharacteristics().getStaticAbilities()) {
            final String stAbD = stAb.toString();
            if (!stAbD.equals("")) {
                sb.append(stAbD + "\r\n");
            }
        }

        // keyword descriptions
        for (int i = 0; i < kw.size(); i++) {
            final String keyword = kw.get(i);
            if (keyword.startsWith("CostChange")) {
                final String[] k = keyword.split(":");
                if (k.length > 8) {
                    sb.append(k[8]).append("\r\n");
                }
            }
            /*if (keyword.startsWith("AdjustLandPlays")) {
                final String[] k = keyword.split(":");
                if (k.length > 3) {
                    sb.append(k[3]).append("\r\n");
                }
            }*/
            if ((keyword.startsWith("Ripple") && !sb.toString().contains("Ripple"))
                    || (keyword.startsWith("Dredge") && !sb.toString().contains("Dredge"))
                    || (keyword.startsWith("Madness") && !sb.toString().contains("Madness"))
                    || (keyword.startsWith("CARDNAME is ") && !sb.toString().contains("CARDNAME is "))
                    || (keyword.startsWith("Recover") && !sb.toString().contains("Recover"))
                    || (keyword.startsWith("Miracle") && !sb.toString().contains("Miracle"))) {
                sb.append(keyword.replace(":", " ")).append("\r\n");
            }
            if (keyword.equals("CARDNAME can't be countered.")
                    || keyword.startsWith("May be played")
                    || keyword.startsWith("Cascade") || keyword.startsWith("Wither")
                    || (keyword.startsWith("Epic") && !sb.toString().contains("Epic"))
                    || (keyword.startsWith("Conspire") && !sb.toString().contains("Conspire"))
                    || (keyword.startsWith("Split second") && !sb.toString().contains("Split second"))
                    || (keyword.startsWith("Multikicker") && !sb.toString().contains("Multikicker"))) {
                sb.append(kw.get(i)).append("\r\n");
            }
            if (keyword.startsWith("Flashback")) {
                sb.append("Flashback");
                if (keyword.contains(" ")) {
                    final Cost fbCost = new Cost(keyword.substring(10), true);
                    if (!fbCost.isOnlyManaCost()) {
                        sb.append(" -");
                    }
                    sb.append(" " + fbCost.toString()).delete(sb.length() - 2, sb.length());
                    if (!fbCost.isOnlyManaCost()) {
                        sb.append(".");
                    }
                }
                sb.append("\r\n");
            } else if (keyword.startsWith("Splice")) {
                final Cost cost = new Cost(keyword.substring(19), false);
                sb.append("Splice onto Arcane " + cost.toSimpleString() + "\r\n");
            } else if (keyword.startsWith("Buyback")) {
                final Cost cost = new Cost(keyword.substring(8), false);
                sb.append("Buyback " + cost.toSimpleString() + "\r\n");
            } else if (keyword.startsWith("Kicker")) {
                final Cost cost = new Cost(keyword.substring(7), false);
                sb.append("Kicker " + cost.toSimpleString() + "\r\n");
            } else if (keyword.startsWith("AlternateAdditionalCost")) {
                final String costString1 = keyword.split(":")[1];
                final String costString2 = keyword.split(":")[2];
                final Cost cost1 = new Cost(costString1, false);
                final Cost cost2 = new Cost(costString2, false);
                sb.append("As an additional cost to cast " + this.getName() + ", " + cost1.toSimpleString()
                        + " or pay " + cost2.toSimpleString() + ".\r\n");
            } else if (keyword.startsWith("Storm")) {
                if (sb.toString().contains("Target") || sb.toString().contains("target")) {
                    sb.insert(
                            sb.indexOf("Storm (When you cast this spell, copy it for each spell cast before it this turn.") + 81,
                            " You may choose new targets for the copies.");
                }
            } else if (keyword.contains("Replicate") && !sb.toString().contains("you paid its replicate cost.")) {
                if (sb.toString().endsWith("\r\n\r\n")) {
                    sb.delete(sb.lastIndexOf("\r\n"), sb.lastIndexOf("\r\n") + 3);
                }
                sb.append(keyword);
                sb.append(" (When you cast this spell, copy it for each time you paid its replicate cost.");
                if (sb.toString().contains("Target") || sb.toString().contains("target")) {
                    sb.append(" You may choose new targets for the copies.");
                }
                sb.append(")\r\n");
            } else if (keyword.startsWith("Haunt")) {
                if (sb.toString().endsWith("\r\n\r\n")) {
                    sb.delete(sb.lastIndexOf("\r\n"), sb.lastIndexOf("\r\n") + 3);
                }
                sb.append("Haunt (");
                if (this.isCreature()) {
                    sb.append("When this creature dies, exile it haunting target creature.");
                } else {
                    sb.append("When this spell card is put into a graveyard after resolving, ");
                    sb.append("exile it haunting target creature.");
                }
                sb.append(")\r\n");
            } else if (keyword.equals("Convoke")) {
                if (sb.toString().endsWith("\r\n\r\n")) {
                    sb.delete(sb.lastIndexOf("\r\n"), sb.lastIndexOf("\r\n") + 3);
                }
                sb.append("Convoke (Each creature you tap while casting this spell reduces its cost by 1 or by one mana of that creature's color.)\r\n");
            } else if (keyword.endsWith(" offering")) {
                if (sb.toString().endsWith("\r\n\r\n")) {
                    sb.delete(sb.lastIndexOf("\r\n"), sb.lastIndexOf("\r\n") + 3);
                }
                String offeringType = keyword.split(" ")[0];
                sb.append(keyword);
                sb.append(" (You may cast this card any time you could cast an instant by sacrificing a ");
                sb.append(offeringType);
                sb.append("and paying the difference in mana costs between this and the sacrificed ");
                sb.append(offeringType);
                sb.append(". Mana cost includes color.)");
            } else if (keyword.equals("Remove CARDNAME from your deck before playing if you're not playing for ante.")) {
                if (sb.toString().endsWith("\r\n\r\n")) {
                    sb.delete(sb.lastIndexOf("\r\n"), sb.lastIndexOf("\r\n") + 3);
                }
                sb.append("Remove CARDNAME from your deck before playing if you're not playing for ante.\r\n");
            } else if (keyword.equals("Rebound")) {
                sb.append(keyword)
                        .append(" (If you cast this spell from your hand, exile it as it resolves. At the beginning of your next upkeep, you may cast this card from exile without paying its mana cost.)\r\n");
            }
        }
        return sb;
    }

    /**
     * <p>
     * Getter for the field <code>manaAbility</code>.
     * </p>
     * 
     * @return a {@link java.util.ArrayList} object.
     */
    public final List<SpellAbility> getManaAbility() {
        return Collections.unmodifiableList(this.getCharacteristics().getManaAbility());
    }


    /**
     * <p>
     * clearFirstSpellAbility.
     * </p>
     */
    public final void clearFirstSpell() {
        for (int i = 0; i < this.getCharacteristics().getSpellAbility().size(); i++) {
            if (this.getCharacteristics().getSpellAbility().get(i).isSpell()) {
                this.getCharacteristics().getSpellAbility().remove(i);
                return;
            }
        }
    }

    /**
     * <p>
     * getFirstSpellAbility.
     * </p>
     * 
     * @return a SpellAbility object.
     */
    public final SpellAbility getFirstSpellAbility() {
        final List<SpellAbility> sas = this.getCharacteristics().getSpellAbility();
        return sas.isEmpty() ? null : sas.get(0);
    }


    /**
     * <p>
     * getSpellPermanent.
     * </p>
     * 
     * @return a {@link forge.card.spellability.SpellPermanent} object.
     */
    public final SpellPermanent getSpellPermanent() {
        for (final SpellAbility sa : this.getCharacteristics().getSpellAbility()) {
            if (sa instanceof SpellPermanent) {
                return (SpellPermanent) sa;
            }
        }
        return null;
    }

    /**
     * <p>
     * addSpellAbility.
     * </p>
     * 
     * @param a
     *            a {@link forge.card.spellability.SpellAbility} object.
     */
    public final void addSpellAbility(final SpellAbility a) {

        a.setSourceCard(this);
        if (a.isManaAbility()) {
            this.getCharacteristics().getManaAbility().add(a);
        } else {
            this.getCharacteristics().getSpellAbility().add(a);
        }
    }


    /**
     * <p>
     * removeSpellAbility.
     * </p>
     * 
     * @param a
     *            a {@link forge.card.spellability.SpellAbility} object.
     */
    public final void removeSpellAbility(final SpellAbility a) {
        if (a.isManaAbility()) {
            // if (a.isExtrinsic()) //never remove intrinsic mana abilities, is
            // this the way to go??
            this.getCharacteristics().getManaAbility().remove(a);
        } else {
            this.getCharacteristics().getSpellAbility().remove(a);
        }
    }

    /**
     * <p>
     * getSpellAbilities.
     * </p>
     * 
     * @return a {@link java.util.ArrayList} object.
     */
    public final ArrayList<SpellAbility> getSpellAbilities() {
        final ArrayList<SpellAbility> res = new ArrayList<SpellAbility>(this.getCharacteristics().getSpellAbility());
        res.addAll(this.getManaAbility());
        return res;
    }

    /**
     * <p>
     * getNonManaSpellAbilities.
     * </p>
     * 
     * @return a {@link java.util.ArrayList} object.
     */
    public final List<SpellAbility> getNonManaSpellAbilities() {
        return this.getCharacteristics().getSpellAbility();
    }

    /**
     * 
     * getAllSpellAbilities.
     * 
     * @return ArrayList<SpellAbility>
     */
    public final ArrayList<SpellAbility> getAllSpellAbilities() {
        final ArrayList<SpellAbility> res = new ArrayList<SpellAbility>();

        for (final CardCharacteristicName key : this.characteristicsMap.keySet()) {
            res.addAll(this.getState(key).getSpellAbility());
            res.addAll(this.getState(key).getManaAbility());
        }

        return res;
    }

    /**
     * <p>
     * getSpells.
     * </p>
     * 
     * @return a {@link java.util.ArrayList} object.
     */
    public final List<SpellAbility> getSpells() {
        final List<SpellAbility> res = new ArrayList<SpellAbility>();
        for (final SpellAbility sa : this.getCharacteristics().getSpellAbility()) {
            if (!sa.isSpell()) {
                continue;
            }
            res.add(sa);
        }
        return res;
    }

    /**
     * <p>
     * getBasicSpells.
     * </p>
     * 
     * @return a {@link java.util.ArrayList} object.
     */
    public final List<SpellAbility> getBasicSpells() {
        final ArrayList<SpellAbility> res = new ArrayList<SpellAbility>();

        for (final SpellAbility sa : this.getCharacteristics().getSpellAbility()) {
            if (sa.isSpell() && sa.isBasicSpell()) {
                res.add(sa);
            }
        }
        return res;
    }

    // shield = regeneration
    /**
     * <p>
     * getShield.
     * </p>
     * 
     * @return a int.
     */
    public final int getShield() {
        return this.nShield;
    }

    /**
     * <p>
     * addShield.
     * </p>
     */
    public final void addShield() {
        this.nShield++;
    }

    /**
     * <p>
     * subtractShield.
     * </p>
     */
    public final void subtractShield() {
        this.nShield--;
    }

    /**
     * Adds the regenerated this turn.
     */
    public final void addRegeneratedThisTurn() {
        this.regeneratedThisTurn += 1;
    }

    /**
     * Gets the regenerated this turn.
     * 
     * @return the regenerated this turn
     */
    public final int getRegeneratedThisTurn() {
        return this.regeneratedThisTurn;
    }

    /**
     * Sets the regenerated this turn.
     * 
     * @param n
     *            the new regenerated this turn
     */
    public final void setRegeneratedThisTurn(final int n) {
        this.regeneratedThisTurn = n;
    }

    /**
     * <p>
     * resetShield.
     * </p>
     */
    public final void resetShield() {
        this.nShield = 0;
    }

    /**
     * <p>
     * canBeShielded.
     * </p>
     * 
     * @return a boolean.
     */
    public final boolean canBeShielded() {
        return !this.hasKeyword("CARDNAME can't be regenerated.");
    }

    // is this "Card" supposed to be a token?
    /**
     * <p>
     * Setter for the field <code>token</code>.
     * </p>
     * 
     * @param b
     *            a boolean.
     */
    public final void setToken(final boolean b) {
        this.token = b;
    }

    /**
     * <p>
     * isToken.
     * </p>
     * 
     * @return a boolean.
     */
    public final boolean isToken() {
        return this.token;
    }

    /**
     * <p>
     * Setter for the field <code>copiedToken</code>.
     * </p>
     * 
     * @param b
     *            a boolean.
     */
    public final void setCopiedToken(final boolean b) {
        this.copiedToken = b;
    }

    /**
     * <p>
     * isCopiedToken.
     * </p>
     * 
     * @return a boolean.
     */
    public final boolean isCopiedToken() {
        return this.copiedToken;
    }

    /**
     * <p>
     * Setter for the field <code>copiedSpell</code>.
     * </p>
     * 
     * @param b
     *            a boolean.
     */
    public final void setCopiedSpell(final boolean b) {
        this.copiedSpell = b;
    }

    /**
     * <p>
     * isCopiedSpell.
     * </p>
     * 
     * @return a boolean.
     */
    public final boolean isCopiedSpell() {
        return this.copiedSpell;
    }

    /**
     * <p>
     * isFaceDown.
     * </p>
     * 
     * @return a boolean.
     */
    public final boolean isFaceDown() {
        return this.curCharacteristics == CardCharacteristicName.FaceDown;
    }

    /**
     * <p>
     * setCanCounter.
     * </p>
     * 
     * @param b
     *            a boolean.
     */
    public final void setCanCounter(final boolean b) {
        this.canCounter = b;
    }

    /**
     * <p>
     * getCanCounter.
     * </p>
     * 
     * @return a boolean.
     */
    public final boolean getCanCounter() {
        return this.canCounter;
    }


    /**
     * <p>
     * addTrigger.
     * </p>
     * 
     * @param c
     *            a {@link forge.Command} object.
     * @param typeIn
     *            a {@link forge.card.trigger.ZCTrigger} object.
     */
    public final void addTrigger(final Command c, final ZCTrigger typeIn) {
        this.zcTriggers.add(new AbilityTriggered(this, c, typeIn));
    }

    /**
     * <p>
     * removeTrigger.
     * </p>
     * 
     * @param c
     *            a {@link forge.Command} object.
     * @param typeIn
     *            a {@link forge.card.trigger.ZCTrigger} object.
     */
    public final void removeTrigger(final Command c, final ZCTrigger typeIn) {
        this.zcTriggers.remove(new AbilityTriggered(this, c, typeIn));
    }

    /**
     * <p>
     * executeTrigger.
     * </p>
     * 
     * @param type
     *            a {@link forge.card.trigger.ZCTrigger} object.
     */
    public final void executeTrigger(final ZCTrigger type) {
        for (final AbilityTriggered t : this.zcTriggers) {
            if (t.getTrigger().equals(type) && t.isBasic()) {
                t.run();
            }
        }
    }

    /**
     * <p>
     * clearTriggers.
     * </p>
     */
    public final void clearTriggers() {
        this.zcTriggers.clear();
    }

    /**
     * <p>
     * addComesIntoPlayCommand.
     * </p>
     * 
     * @param c
     *            a {@link forge.Command} object.
     */
    public final void addComesIntoPlayCommand(final Command c) {
        this.addTrigger(c, ZCTrigger.ENTERFIELD);
    }



    /**
     * <p>
     * addDestroyCommand.
     * </p>
     * 
     * @param c
     *            a {@link forge.Command} object.
     */
    public final void addDestroyCommand(final Command c) {
        this.addTrigger(c, ZCTrigger.DESTROY);
    }


    /**
     * <p>
     * addLeavesPlayCommand.
     * </p>
     * 
     * @param c
     *            a {@link forge.Command} object.
     */
    public final void addLeavesPlayCommand(final Command c) {
        this.addTrigger(c, ZCTrigger.LEAVEFIELD);
    }

    /**
     * <p>
     * addUntapCommand.
     * </p>
     * 
     * @param c
     *            a {@link forge.Command} object.
     */
    public final void addUntapCommand(final Command c) {
        this.untapCommandList.add(c);
    }

    /**
     * <p>
     * addChangeControllerCommand.
     * </p>
     * 
     * @param c
     *            a {@link forge.Command} object.
     */
    public final void addChangeControllerCommand(final Command c) {
        this.changeControllerCommandList.add(c);
    }
    
    
    public final void runChangeControllerCommands() {
        for (final Command c : this.changeControllerCommandList) {
            c.run();
        }
    }

    /**
     * <p>
     * Setter for the field <code>sickness</code>.
     * </p>
     * 
     * @param b
     *            a boolean.
     */
    public final void setSickness(final boolean b) {
        this.sickness = b;
    }

    /** @return boolean */
    public final boolean isFirstTurnControlled() {
        return this.sickness;
    }

    /**
     * <p>
     * hasSickness.
     * </p>
     * 
     * @return a boolean.
     */
    public final boolean hasSickness() {
        return this.sickness && !this.hasKeyword("Haste");
    }

    /**
     * 
     * isSick.
     * 
     * @return boolean
     */
    public final boolean isSick() {
        return this.sickness && this.isCreature() && !this.hasKeyword("Haste");
    }

    /**
     * <p>
     * Getter for the field <code>owner</code>.
     * </p>
     * 
     * @return a {@link forge.game.player.Player} object.
     */
    public final Player getOwner() {
        return this.owner;
    }

    /**
     * Get the controller for this card.
     * 
     * @return a {@link forge.game.player.Player} object.
     */
    public final Player getController() {
        
        if (!this.tempControllers.isEmpty()) {
            final long lastTimestamp = this.tempControllers.lastKey();
            if (lastTimestamp > this.controllerTimestamp) {
                return this.tempControllers.lastEntry().getValue();
            }
        }
        if (this.controller != null) {
            return this.controller;
        }
        return this.owner;
    }

    public final void addTempController(final Player player, final long tstamp) {
        this.tempControllers.put(tstamp, player);
    }

    public final void removeTempController(final long tstamp) {
        this.tempControllers.remove(tstamp);
    }

    public final void clearTempControllers() {
        this.tempControllers.clear();
        
    }

    public final void clearControllers() {
        clearTempControllers();
        this.controller = null;
    }

    public final void setController(final Player player, final long tstamp) {
        clearTempControllers();
        this.controller = player;
        this.controllerTimestamp = tstamp;
    }

    /**
     * <p>
     * Setter for the field <code>owner</code>.
     * </p>
     * 
     * @param player
     *            a {@link forge.game.player.Player} object.
     */
    public final void setOwner(final Player player) {
        this.owner = player;
    }

    /**
     * <p>
     * Getter for the field <code>equippedBy</code>.
     * </p>
     * 
     * @return a {@link java.util.ArrayList} object.
     */
    public final ArrayList<Card> getEquippedBy() {
        return this.equippedBy;
    }

    /**
     * <p>
     * Setter for the field <code>equippedBy</code>.
     * </p>
     * 
     * @param list
     *            a {@link java.util.ArrayList} object.
     */
    public final void setEquippedBy(final ArrayList<Card> list) {
        this.equippedBy = list;
    }

    /**
     * <p>
     * Getter for the field <code>equipping</code>.
     * </p>
     * 
     * @return a {@link java.util.ArrayList} object.
     */
    public final ArrayList<Card> getEquipping() {
        return this.equipping;
    }

    /**
     * <p>
     * getEquippingCard.
     * </p>
     * 
     * @return a {@link forge.Card} object.
     */
    public final Card getEquippingCard() {
        if (this.equipping.isEmpty()) {
            return null;
        }
        return this.equipping.get(0);
    }

    /**
     * <p>
     * Setter for the field <code>equipping</code>.
     * </p>
     * 
     * @param list
     *            a {@link java.util.ArrayList} object.
     */
    public final void setEquipping(final ArrayList<Card> list) {
        this.equipping = list;
    }

    /**
     * <p>
     * isEquipped.
     * </p>
     * 
     * @return a boolean.
     */
    public final boolean isEquipped() {
        return !this.equippedBy.isEmpty();
    }

    /**
     * <p>
     * isEquipping.
     * </p>
     * 
     * @return a boolean.
     */
    public final boolean isEquipping() {
        return this.equipping.size() != 0;
    }

    /**
     * <p>
     * addEquippedBy.
     * </p>
     * 
     * @param c
     *            a {@link forge.Card} object.
     */
    public final void addEquippedBy(final Card c) {
        this.equippedBy.add(c);
        this.updateObservers();
    }

    /**
     * <p>
     * removeEquippedBy.
     * </p>
     * 
     * @param c
     *            a {@link forge.Card} object.
     */
    public final void removeEquippedBy(final Card c) {
        this.equippedBy.remove(c);
        this.updateObservers();
    }

    /**
     * <p>
     * addEquipping.
     * </p>
     * 
     * @param c
     *            a {@link forge.Card} object.
     */
    public final void addEquipping(final Card c) {
        this.equipping.add(c);
        this.setTimestamp(getGame().getNextTimestamp());
        this.updateObservers();
    }

    /**
     * <p>
     * removeEquipping.
     * </p>
     * 
     * @param c
     *            a {@link forge.Card} object.
     */
    public final void removeEquipping(final Card c) {
        this.equipping.remove(c);
        this.updateObservers();
    }

    /**
     * <p>
     * equipCard.
     * </p>
     * equipment.equipCard(cardToBeEquipped)
     * 
     * @param c
     *            a {@link forge.Card} object.
     */
    public final void equipCard(final Card c) {
        if (c.hasKeyword("CARDNAME can't be equipped.")) {
            getGame().getGameLog().add(GameLogEntryType.STACK_RESOLVE, "Trying to equip " + c.getName() + " but it can't be equipped.");
            return;
        }
        if (this.hasStartOfKeyword("CantEquip")) {
            final int keywordPosition = this.getKeywordPosition("CantEquip");
            final String parse = this.getKeyword().get(keywordPosition).toString();
            final String[] k = parse.split(" ", 2);
            final String[] restrictions = k[1].split(",");
            if (c.isValid(restrictions, this.getController(), this)) {
                getGame().getGameLog().add(GameLogEntryType.STACK_RESOLVE, "Trying to equip " + c.getName() + " but it can't be equipped.");
                return;
            }
        }
        if (this.isEquipping()) {
            this.unEquipCard(this.getEquipping().get(0));
        }
        this.addEquipping(c);
        c.addEquippedBy(this);

        // Play the Equip sound
        getGame().fireEvent(new GameEventCardEquipped());
        // run trigger
        final HashMap<String, Object> runParams = new HashMap<String, Object>();
        runParams.put("AttachSource", this);
        runParams.put("AttachTarget", c);
        this.getController().getGame().getTriggerHandler().runTrigger(TriggerType.Attached, runParams, false);
    }

    /**
     * <p>
     * unEquipCard.
     * </p>
     * 
     * @param c
     *            a {@link forge.Card} object.
     */
    public final void unEquipCard(final Card c) // equipment.unEquipCard(equippedCard);
    {
        this.equipping.remove(c);
        c.removeEquippedBy(this);

        // Run triggers
        final Map<String, Object> runParams = new TreeMap<String, Object>();
        runParams.put("Equipment", this);
        runParams.put("Card", c);
        getGame().getTriggerHandler().runTrigger(TriggerType.Unequip, runParams, false);
    }

    /**
     * <p>
     * unEquipAllCards.
     * </p>
     */
    public final void unEquipAllCards() {
        // while there exists equipment, unequip the first one
        while (this.equippedBy.size() > 0) {
            this.equippedBy.get(0).unEquipCard(this);
        }
    }

    /**
     * <p>
     * Getter for the field <code>enchanting</code>.
     * </p>
     * 
     * @return a {@link forge.GameEntity} object.
     */
    public final GameEntity getEnchanting() {
        return this.enchanting;
    }

    /**
     * <p>
     * getEnchantingCard.
     * </p>
     * 
     * @return a {@link forge.Card} object.
     */
    public final Card getEnchantingCard() {
        if ((this.enchanting != null) && (this.enchanting instanceof Card)) {
            return (Card) this.enchanting;
        }
        return null;
    }

    /**
     * <p>
     * getEnchantingPlayer.
     * </p>
     * 
     * @return a {@link forge.game.player.Player} object.
     */
    public final Player getEnchantingPlayer() {
        if ((this.enchanting != null) && (this.enchanting instanceof Player)) {
            return (Player) this.enchanting;
        }
        return null;
    }

    /**
     * <p>
     * Setter for the field <code>enchanting</code>.
     * </p>
     * 
     * @param e
     *            a GameEntity object.
     */
    public final void setEnchanting(final GameEntity e) {
        this.enchanting = e;
    }

    /**
     * <p>
     * isEnchanting.
     * </p>
     * 
     * @return a boolean.
     */
    public final boolean isEnchanting() {
        return this.enchanting != null;
    }

    /**
     * <p>
     * isEnchanting.
     * </p>
     * 
     * @return a boolean.
     */
    public final boolean isEnchantingCard() {
        return this.getEnchantingCard() != null;
    }

    /**
     * <p>
     * isEnchanting.
     * </p>
     * 
     * @return a boolean.
     */
    public final boolean isEnchantingPlayer() {
        return this.getEnchantingPlayer() != null;
    }

    /**
     * checks to see if this card is enchanted by an aura with a given name.
     * 
     * @param cardName
     *            the name of the aura
     * @return true if this card is enchanted by an aura with the given name,
     *         false otherwise
     */
    public final boolean isEnchantedBy(final String cardName) {
        final ArrayList<Card> allAuras = this.getEnchantedBy();
        for (final Card aura : allAuras) {
            if (aura.getName().equals(cardName)) {
                return true;
            }
        }
        return false;
    }

    /**
     * <p>
     * addEnchanting.
     * </p>
     * 
     * @param e
     *            a {@link forge.GameEntity} object.
     */
    public final void addEnchanting(final GameEntity e) {
        this.enchanting = e;
        this.setTimestamp(getGame().getNextTimestamp());
        this.updateObservers();
    }

    /**
     * <p>
     * removeEnchanting.
     * </p>
     * 
     * @param e
     *            a {@link forge.GameEntity} object.
     */
    public final void removeEnchanting(final GameEntity e) {
        if (this.enchanting.equals(e)) {
            this.enchanting = null;
            this.updateObservers();
        }
    }

    /**
     * <p>
     * enchant.
     * </p>
     * 
     * @param entity
     *            a {@link forge.GameEntity} object.
     */
    public final void enchantEntity(final GameEntity entity) {
        if (entity.hasKeyword("CARDNAME can't be enchanted.")) {
            getGame().getGameLog().add(GameLogEntryType.STACK_RESOLVE, "Trying to enchant " + entity.getName()
            + " but it can't be enchanted.");
            return;
        }
        this.addEnchanting(entity);
        entity.addEnchantedBy(this);
        // run trigger
        final HashMap<String, Object> runParams = new HashMap<String, Object>();
        runParams.put("AttachSource", this);
        runParams.put("AttachTarget", entity);
        this.getController().getGame().getTriggerHandler().runTrigger(TriggerType.Attached, runParams, false);
    }

    /**
     * <p>
     * unEnchant.
     * </p>
     * 
     * @param gameEntity
     *            a {@link forge.GameEntity} object.
     */
    public final void unEnchantEntity(final GameEntity gameEntity) {
        if ((this.enchanting != null) && this.enchanting.equals(gameEntity)) {
            this.enchanting = null;
            gameEntity.removeEnchantedBy(this);
        }
    }
    /**
     * <p>
     * Setter for the field <code>type</code>.
     * </p>
     * 
     * @param a
     *            a {@link java.util.ArrayList} object.
     */
    public final void setType(final List<String> a) {
        this.getCharacteristics().setType(new ArrayList<String>(a));
    }

    /**
     * <p>
     * addType.
     * </p>
     * 
     * @param a
     *            a {@link java.lang.String} object.
     */
    public final void addType(final String a) {
        this.getCharacteristics().getType().add(a);
    }


    /**
     * <p>
     * Getter for the field <code>type</code>.
     * </p>
     * 
     * @return a {@link java.util.ArrayList} object.
     */
    public final ArrayList<String> getType() {

        // see if type changes are in effect
        final ArrayList<String> newType = new ArrayList<String>(this.getCharacteristics().getType());
        for (final CardType ct : this.changedCardTypes.values()) {
            final ArrayList<String> removeTypes = new ArrayList<String>();
            if (ct.getRemoveType() != null) {
                removeTypes.addAll(ct.getRemoveType());
            }
            // remove old types
            for (int i = 0; i < newType.size(); i++) {
                final String t = newType.get(i);
                if (ct.isRemoveSuperTypes() && forge.card.CardType.isASuperType(t)) {
                    removeTypes.add(t);
                }
                if (ct.isRemoveCardTypes() && forge.card.CardType.isACardType(t)) {
                    removeTypes.add(t);
                }
                if (ct.isRemoveSubTypes() && forge.card.CardType.isASubType(t)) {
                    removeTypes.add(t);
                }
                if (ct.isRemoveCreatureTypes() && (forge.card.CardType.isACreatureType(t) || t.equals("AllCreatureTypes"))) {
                    removeTypes.add(t);
                }
            }
            newType.removeAll(removeTypes);
            // add new types
            if (ct.getType() != null) {
                newType.addAll(ct.getType());
            }

        }

        return newType;

    }

    /**
     * 
     * TODO Write javadoc for this method.
     * 
     * @param types
     *            ArrayList<String>
     * @param removeTypes
     *            ArrayList<String>
     * @param removeSuperTypes
     *            boolean
     * @param removeCardTypes
     *            boolean
     * @param removeSubTypes
     *            boolean
     * @param removeCreatureTypes
     *            boolean
     * @param timestamp
     *            long
     */
    public final void addChangedCardTypes(final ArrayList<String> types, final ArrayList<String> removeTypes,
            final boolean removeSuperTypes, final boolean removeCardTypes, final boolean removeSubTypes,
            final boolean removeCreatureTypes, final long timestamp) {

        this.changedCardTypes.put(timestamp, new CardType(types, removeTypes, removeSuperTypes, removeCardTypes, removeSubTypes, removeCreatureTypes));
    }

    /**
     * 
     * TODO Write javadoc for this method.
     * 
     * @param types
     *            String[]
     * @param removeTypes
     *            String[]
     * @param removeSuperTypes
     *            boolean
     * @param removeCardTypes
     *            boolean
     * @param removeSubTypes
     *            boolean
     * @param removeCreatureTypes
     *            boolean
     * @param timestamp
     *            long
     */
    public final void addChangedCardTypes(final String[] types, final String[] removeTypes,
            final boolean removeSuperTypes, final boolean removeCardTypes, final boolean removeSubTypes,
            final boolean removeCreatureTypes, final long timestamp) {
        ArrayList<String> typeList = null;
        ArrayList<String> removeTypeList = null;
        if (types != null) {
            typeList = new ArrayList<String>(Arrays.asList(types));
        }

        if (removeTypes != null) {
            removeTypeList = new ArrayList<String>(Arrays.asList(removeTypes));
        }

        this.addChangedCardTypes(typeList, removeTypeList, removeSuperTypes, removeCardTypes, removeSubTypes,
                removeCreatureTypes, timestamp);
    }

    /**
     * 
     * TODO Write javadoc for this method.
     * 
     * @param timestamp
     *            long
     */
    public final void removeChangedCardTypes(final long timestamp) {
        this.changedCardTypes.remove(Long.valueOf(timestamp));
    }

    // values that are printed on card
    /**
     * <p>
     * Getter for the field <code>baseLoyalty</code>.
     * </p>
     * 
     * @return a int.
     */
    public final int getBaseLoyalty() {
        return this.baseLoyalty;
    }

    // values that are printed on card
    /**
     * <p>
     * Setter for the field <code>baseLoyalty</code>.
     * </p>
     * 
     * @param n
     *            a int.
     */
    public final void setBaseLoyalty(final int n) {
        this.baseLoyalty = n;
    }

    // values that are printed on card
    /**
     * <p>
     * Getter for the field <code>baseAttack</code>.
     * </p>
     * 
     * @return a int.
     */
    public final int getBaseAttack() {
        return this.getCharacteristics().getBaseAttack();
    }

    /**
     * <p>
     * Getter for the field <code>baseDefense</code>.
     * </p>
     * 
     * @return a int.
     */
    public final int getBaseDefense() {
        return this.getCharacteristics().getBaseDefense();
    }

    // values that are printed on card
    /**
     * <p>
     * Setter for the field <code>baseAttack</code>.
     * </p>
     * 
     * @param n
     *            a int.
     */
    public final void setBaseAttack(final int n) {
        this.getCharacteristics().setBaseAttack(n);
    }

    /**
     * <p>
     * Setter for the field <code>baseDefense</code>.
     * </p>
     * 
     * @param n
     *            a int.
     */
    public final void setBaseDefense(final int n) {
        this.getCharacteristics().setBaseDefense(n);
    }

    // values that are printed on card
    /**
     * <p>
     * Getter for the field <code>baseAttackString</code>.
     * </p>
     * 
     * @return a {@link java.lang.String} object.
     */
    public final String getBaseAttackString() {
        return (null == this.baseAttackString) ? "" + this.getBaseAttack() : this.baseAttackString;
    }

    /**
     * <p>
     * Getter for the field <code>baseDefenseString</code>.
     * </p>
     * 
     * @return a {@link java.lang.String} object.
     */
    public final String getBaseDefenseString() {
        return (null == this.baseDefenseString) ? "" + this.getBaseDefense() : this.baseDefenseString;
    }

    // values that are printed on card
    /**
     * <p>
     * Setter for the field <code>baseAttackString</code>.
     * </p>
     * 
     * @param s
     *            a {@link java.lang.String} object.
     */
    public final void setBaseAttackString(final String s) {
        this.baseAttackString = s;
    }

    /**
     * <p>
     * Setter for the field <code>baseDefenseString</code>.
     * </p>
     * 
     * @param s
     *            a {@link java.lang.String} object.
     */
    public final void setBaseDefenseString(final String s) {
        this.baseDefenseString = s;
    }

    /**
     * 
     * TODO Write javadoc for this method.
     * 
     * @return int
     */
    public final int getSetPower() {
        if (this.newPT.isEmpty()) {
            return -1;
        }

        final CardPowerToughness latestPT = this.getLatestPT();

        return latestPT.getPower();
    }

    /**
     * 
     * TODO Write javadoc for this method.
     * 
     * @return int
     */
    public final int getSetToughness() {
        if (this.newPT.isEmpty()) {
            return -1;
        }

        final CardPowerToughness latestPT = this.getLatestPT();

        return latestPT.getToughness();
    }

    /**
     * 
     * TODO Write javadoc for this method.
     * 
     * @return CardPowerToughness
     */
    public final CardPowerToughness getLatestPT() {
        CardPowerToughness latestPT = new CardPowerToughness(-1, -1, -2);
        long max = -2;

        for (final CardPowerToughness pt : this.newPT) {
            if (pt.getTimestamp() >= max) {
                max = pt.getTimestamp();
                latestPT = pt;
            }
        }

        return latestPT;
    }

    /**
     * 
     * TODO Write javadoc for this method.
     * 
     * @param power
     *            int
     * @param toughness
     *            int
     * @param timestamp
     *            int
     */
    public final void addNewPT(final int power, final int toughness, final long timestamp) {
        this.newPT.add(new CardPowerToughness(power, toughness, timestamp));
    }

    /**
     * 
     * TODO Write javadoc for this method.
     * 
     * @param timestamp
     *            long
     */
    public final void removeNewPT(final long timestamp) {
        for (int i = 0; i < this.newPT.size(); i++) {
            final CardPowerToughness cardPT = this.newPT.get(i);
            if (cardPT.getTimestamp() == timestamp) {
                this.newPT.remove(cardPT);
            }
        }
    }

    /**
     * 
     * TODO Write javadoc for this method.
     * 
     * @return int
     */
    public final int getCurrentPower() {
        int total = this.getBaseAttack();
        final int setPower = this.getSetPower();
        if (setPower != -1) {
            total = setPower;
        }

        return total;
    }

    /**
     * <p>
     * getUnswitchedAttack.
     * </p>
     * 
     * @return a int.
     */
    public final int getUnswitchedPower() {
        int total = this.getCurrentPower();

        total += this.getTempAttackBoost() + this.getSemiPermanentAttackBoost() + getPowerBonusFromCounters();
        return total;
    }
    
    public final int getPowerBonusFromCounters() {
        int total = 0;

        total += this.getCounters(CounterType.P1P1) + this.getCounters(CounterType.P1P2) + this.getCounters(CounterType.P1P0) 
                - this.getCounters(CounterType.M1M1) + 2 * this.getCounters(CounterType.P2P2) - 2 * this.getCounters(CounterType.M2M1)
                - 2 * this.getCounters(CounterType.M2M2) - this.getCounters(CounterType.M1M0) + 2 * this.getCounters(CounterType.P2P0);
        return total;
    }

    /**
     * <p>
     * getNetAttack.
     * </p>
     * 
     * @return a int.
     */
    public final int getNetAttack() {
        if (this.getAmountOfKeyword("CARDNAME's power and toughness are switched") % 2 != 0) {
            return this.getUnswitchedToughness();
        }
        return this.getUnswitchedPower();
    }

    /**
     * 
     * TODO Write javadoc for this method.
     * 
     * @return an int
     */
    public final int getCurrentToughness() {
        int total = this.getBaseDefense();

        final int setToughness = this.getSetToughness();
        if (setToughness != -1) {
            total = setToughness;
        }

        return total;
    }

    /**
     * <p>
     * getUnswitchedDefense.
     * </p>
     * 
     * @return a int.
     */
    public final int getUnswitchedToughness() {
        int total = this.getCurrentToughness();

        total += this.getTempDefenseBoost() + this.getSemiPermanentDefenseBoost() + getToughnessBonusFromCounters();
        return total;
    }
    
    public final int getToughnessBonusFromCounters() {
        int total = 0;

        total += this.getCounters(CounterType.P1P1) + 2 * this.getCounters(CounterType.P1P2) - this.getCounters(CounterType.M1M1)
                + this.getCounters(CounterType.P0P1) - 2 * this.getCounters(CounterType.M0M2) + 2 * this.getCounters(CounterType.P2P2)
                - this.getCounters(CounterType.M0M1) - this.getCounters(CounterType.M2M1) - 2 * this.getCounters(CounterType.M2M2)
                + 2 * this.getCounters(CounterType.P0P2);
        return total;
    }

    /**
     * <p>
     * getNetDefense.
     * </p>
     * 
     * @return a int.
     */
    public final int getNetDefense() {
        if (this.getAmountOfKeyword("CARDNAME's power and toughness are switched") % 2 != 0) {
            return this.getUnswitchedPower();
        }
        return this.getUnswitchedToughness();
    }

    // How much combat damage does the card deal
    /**
     * <p>
     * getNetCombatDamage.
     * </p>
     * 
     * @return a int.
     */
    public final int getNetCombatDamage() {
        if (this.hasKeyword("CARDNAME assigns no combat damage")) {
            return 0;
        }

        if (getGame().getStaticEffects().getGlobalRuleChange(GlobalRuleChange.toughnessAssignsDamage)) {
            return this.getNetDefense();
        }
        return this.getNetAttack();
    }

    private int multiKickerMagnitude = 0;
    public final void addMultiKickerMagnitude(final int n) { this.multiKickerMagnitude += n; }
    public final void setKickerMagnitude(final int n) { this.multiKickerMagnitude = n; }
    public final int getKickerMagnitude() { 
        if ( this.multiKickerMagnitude > 0 )
            return multiKickerMagnitude;
        boolean hasK1 = costsPaid.contains(OptionalCost.Kicker1);
        return hasK1 == costsPaid.contains(OptionalCost.Kicker2) ? (hasK1 ? 2 : 0) : 1; 
    }

    /**
     * <p>
     * addReplicateMagnitude.
     * </p>
     * 
     * @param n
     *            a int.
     */
    public final void addReplicateMagnitude(final int n) {
        this.replicateMagnitude += n;
    }

    /**
     * <p>
     * Setter for the field <code>replicateMagnitude</code>.
     * </p>
     * 
     * @param n
     *            a int.
     */
    public final void resetReplicateMagnitude() {
        this.replicateMagnitude = 0;
    }

    /**
     * <p>
     * Getter for the field <code>replicateMagnitude</code>.
     * </p>
     * 
     * @return a int.
     */
    public final int getReplicateMagnitude() {
        return this.replicateMagnitude;
    }

    // for cards like Giant Growth, etc.
    /**
     * <p>
     * Getter for the field <code>tempAttackBoost</code>.
     * </p>
     * 
     * @return a int.
     */
    public final int getTempAttackBoost() {
        return this.tempAttackBoost;
    }

    /**
     * <p>
     * Getter for the field <code>tempDefenseBoost</code>.
     * </p>
     * 
     * @return a int.
     */
    public final int getTempDefenseBoost() {
        return this.tempDefenseBoost;
    }

    /**
     * <p>
     * addTempAttackBoost.
     * </p>
     * 
     * @param n
     *            a int.
     */
    public final void addTempAttackBoost(final int n) {
        this.tempAttackBoost += n;
    }

    /**
     * <p>
     * addTempDefenseBoost.
     * </p>
     * 
     * @param n
     *            a int.
     */
    public final void addTempDefenseBoost(final int n) {
        this.tempDefenseBoost += n;
    }

    // for cards like Glorious Anthem, etc.
    /**
     * <p>
     * Getter for the field <code>semiPermanentAttackBoost</code>.
     * </p>
     * 
     * @return a int.
     */
    public final int getSemiPermanentAttackBoost() {
        return this.semiPermanentAttackBoost;
    }

    /**
     * <p>
     * Getter for the field <code>semiPermanentDefenseBoost</code>.
     * </p>
     * 
     * @return a int.
     */
    public final int getSemiPermanentDefenseBoost() {
        return this.semiPermanentDefenseBoost;
    }

    /**
     * <p>
     * addSemiPermanentAttackBoost.
     * </p>
     * 
     * @param n
     *            a int.
     */
    public final void addSemiPermanentAttackBoost(final int n) {
        this.semiPermanentAttackBoost += n;
    }

    /**
     * <p>
     * addSemiPermanentDefenseBoost.
     * </p>
     * 
     * @param n
     *            a int.
     */
    public final void addSemiPermanentDefenseBoost(final int n) {
        this.semiPermanentDefenseBoost += n;
    }

    /**
     * <p>
     * Setter for the field <code>semiPermanentAttackBoost</code>.
     * </p>
     * 
     * @param n
     *            a int.
     */
    public final void setSemiPermanentAttackBoost(final int n) {
        this.semiPermanentAttackBoost = n;
    }

    /**
     * <p>
     * Setter for the field <code>semiPermanentDefenseBoost</code>.
     * </p>
     * 
     * @param n
     *            a int.
     */
    public final void setSemiPermanentDefenseBoost(final int n) {
        this.semiPermanentDefenseBoost = n;
    }

    /**
     * <p>
     * isUntapped.
     * </p>
     * 
     * @return a boolean.
     */
    public final boolean isUntapped() {
        return !this.tapped;
    }

    /**
     * <p>
     * isTapped.
     * </p>
     * 
     * @return a boolean.
     */
    public final boolean isTapped() {
        return this.tapped;
    }

    /**
     * <p>
     * Setter for the field <code>tapped</code>.
     * </p>
     * 
     * @param b
     *            a boolean.
     */
    public final void setTapped(final boolean b) {
        this.tapped = b;
        this.updateObservers();
    }

    /**
     * <p>
     * tap.
     * </p>
     */
    public final void tap() {
        if (this.isUntapped()) {
            // Run triggers
            final Map<String, Object> runParams = new TreeMap<String, Object>();
            runParams.put("Card", this);
            getGame().getTriggerHandler().runTrigger(TriggerType.Taps, runParams, false);
        }
        this.setTapped(true);

        // Play the Tap sound
        getGame().fireEvent(new GameEventCardTapped(this, true));
    }

    /**
     * <p>
     * untap.
     * </p>
     */
    public final void untap() {
        if (this.isTapped()) {
            // Run triggers
            final Map<String, Object> runParams = new TreeMap<String, Object>();
            runParams.put("Card", this);
            getGame().getTriggerHandler().runTrigger(TriggerType.Untaps, runParams, false);

            // Play the Untap sound
            getGame().fireEvent(new GameEventCardTapped(this, false));
        }

        for (final Command var : this.untapCommandList) {
            var.run();
        }

        this.setTapped(false);
    }

    // keywords are like flying, fear, first strike, etc...
    /**
     * <p>
     * getKeyword.
     * </p>
     * 
     * @return a {@link java.util.ArrayList} object.
     */
    public final List<String> getKeyword() {
        final ArrayList<String> keywords = this.getUnhiddenKeyword();
        keywords.addAll(this.getHiddenExtrinsicKeyword());

        return keywords;
    }

    /**
     * Gets the keyword amount.
     * 
     * @param keyword
     *            the keyword
     * @return the keyword amount
     */
    public final int getKeywordAmount(final String keyword) {
        int res = 0;
        for (final String k : this.getKeyword()) {
            if (k.equals(keyword)) {
                res++;
            }
        }

        return res;
    }


    /**
     * Adds the changed card keywords.
     * 
     * @param keywords
     *            the keywords
     * @param removeKeywords
     *            the remove keywords
     * @param removeAllKeywords
     *            the remove all keywords
     * @param timestamp
     *            the timestamp
     */
    public final void addChangedCardKeywords(final List<String> keywords, final List<String> removeKeywords,
            final boolean removeAllKeywords, final long timestamp) {
        
        // if the key already exists - merge entries
        if (changedCardKeywords.containsKey(timestamp)) {
            List<String> kws = keywords;
            List<String> rkws = removeKeywords;
            boolean remAll = removeAllKeywords;
            CardKeywords cks = changedCardKeywords.get(timestamp);
            kws.addAll(cks.getKeywords());
            rkws.addAll(cks.getRemoveKeywords());
            remAll |= cks.isRemoveAllKeywords();
            this.changedCardKeywords.put(timestamp, new CardKeywords(kws, rkws, remAll));
            return;
        }

        this.changedCardKeywords.put(timestamp, new CardKeywords(keywords, removeKeywords, removeAllKeywords));
    }

    /**
     * Adds the changed card keywords.
     * 
     * @param keywords
     *            the keywords
     * @param removeKeywords
     *            the remove keywords
     * @param removeAllKeywords
     *            the remove all keywords
     * @param timestamp
     *            the timestamp
     */
    public final void addChangedCardKeywords(final String[] keywords, final String[] removeKeywords,
            final boolean removeAllKeywords, final long timestamp) {
        ArrayList<String> keywordsList = new ArrayList<String>();
        ArrayList<String> removeKeywordsList = new ArrayList<String>();
        if (keywords != null) {
            keywordsList = new ArrayList<String>(Arrays.asList(keywords));
        }

        if (removeKeywords != null) {
            removeKeywordsList = new ArrayList<String>(Arrays.asList(removeKeywords));
        }

        this.addChangedCardKeywords(keywordsList, removeKeywordsList, removeAllKeywords, timestamp);
    }

    /**
     * Removes the changed card keywords.
     * 
     * @param timestamp
     *            the timestamp
     */
    public final void removeChangedCardKeywords(final long timestamp) {
        changedCardKeywords.remove(Long.valueOf(timestamp));
    }

    // Hidden keywords will be left out
    /**
     * <p>
     * getUnhiddenKeyword.
     * </p>
     * 
     * @return a {@link java.util.ArrayList} object.
     */
    public final ArrayList<String> getUnhiddenKeyword() {
        final ArrayList<String> keywords = new ArrayList<String>();
        keywords.addAll(this.getIntrinsicKeyword());
        keywords.addAll(this.getExtrinsicKeyword());

        // see if keyword changes are in effect
        for (final CardKeywords ck : this.changedCardKeywords.values()) {

            if (ck.isRemoveAllKeywords()) {
                keywords.clear();
            } else if (ck.getRemoveKeywords() != null) {
                keywords.removeAll(ck.getRemoveKeywords());
            }

            if (ck.getKeywords() != null) {
                keywords.addAll(ck.getKeywords());
            }
        }

        return keywords;
    }

    /**
     * <p>
     * getIntrinsicAbilities.
     * </p>
     * 
     * @return a {@link java.util.ArrayList} object.
     */
    public final List<String> getUnparsedAbilities() {
        return this.getCharacteristics().getUnparsedAbilities();
    }

    /**
     * <p>
     * Getter for the field <code>intrinsicKeyword</code>.
     * </p>
     * 
     * @return a {@link java.util.ArrayList} object.
     */
    public final List<String> getIntrinsicKeyword() {
        // will not create a copy here - due to performance reasons.
        // Most of other code checks for contains, or creates copy by itself
        return this.getCharacteristics().getIntrinsicKeyword();
    }

    /**
     * <p>
     * Setter for the field <code>intrinsicKeyword</code>.
     * </p>
     * 
     * @param a
     *            a {@link java.util.ArrayList} object.
     */
    public final void setIntrinsicKeyword(final List<String> a) {
        this.getCharacteristics().setIntrinsicKeyword(new ArrayList<String>(a));
    }


    /**
     * <p>
     * setIntrinsicAbilities.
     * </p>
     * 
     * @param a
     *            a {@link java.util.ArrayList} object.
     */
    public final void setIntrinsicAbilities(final List<String> a) {
        this.getCharacteristics().setUnparsedAbilities(new ArrayList<String>(a));
    }

    /**
     * <p>
     * addIntrinsicKeyword.
     * </p>
     * 
     * @param s
     *            a {@link java.lang.String} object.
     */
    public final void addIntrinsicKeyword(final String s) {
        if (s.trim().length() != 0) {
            this.getCharacteristics().getIntrinsicKeyword().add(s);
        }
    }

    /**
     * <p>
     * addIntrinsicAbility.
     * </p>
     * 
     * @param s
     *            a {@link java.lang.String} object.
     */
    public final void addIntrinsicAbility(final String s) {
        if (s.trim().length() != 0) {
            this.getCharacteristics().getUnparsedAbilities().add(s);
        }
    }

    /**
     * <p>
     * removeIntrinsicKeyword.
     * </p>
     * 
     * @param s
     *            a {@link java.lang.String} object.
     */
    public final void removeIntrinsicKeyword(final String s) {
        this.getCharacteristics().getIntrinsicKeyword().remove(s);
    }


    /**
     * <p>
     * Getter for the field <code>extrinsicKeyword</code>.
     * </p>
     * 
     * @return a {@link java.util.ArrayList} object.
     */
    public ArrayList<String> getExtrinsicKeyword() {
        return this.extrinsicKeyword;
    }

    /**
     * <p>
     * Setter for the field <code>extrinsicKeyword</code>.
     * </p>
     * 
     * @param a
     *            a {@link java.util.ArrayList} object.
     */
    public final void setExtrinsicKeyword(final ArrayList<String> a) {
        this.extrinsicKeyword = new ArrayList<String>(a);
    }

    /**
     * <p>
     * addExtrinsicKeyword.
     * </p>
     * 
     * @param s
     *            a {@link java.lang.String} object.
     */
    public void addExtrinsicKeyword(final String s) {
        if (s.startsWith("HIDDEN")) {
            this.addHiddenExtrinsicKeyword(s);
        } else {
            this.extrinsicKeyword.add(s);
        }
    }

    /**
     * <p>
     * removeExtrinsicKeyword.
     * </p>
     * 
     * @param s
     *            a {@link java.lang.String} object.
     */
    public void removeExtrinsicKeyword(final String s) {
        if (s.startsWith("HIDDEN")) {
            this.removeHiddenExtrinsicKeyword(s);
        } else {
            this.extrinsicKeyword.remove(s);
        }
    }

    /**
     * Removes the all extrinsic keyword.
     * 
     * @param s
     *            the s
     */
    public void removeAllExtrinsicKeyword(final String s) {
        final ArrayList<String> strings = new ArrayList<String>();
        strings.add(s);
        this.hiddenExtrinsicKeyword.removeAll(strings);
        this.extrinsicKeyword.removeAll(strings);
    }


    // Hidden Keywords will be returned without the indicator HIDDEN
    /**
     * <p>
     * getHiddenExtrinsicKeyword.
     * </p>
     * 
     * @return a {@link java.util.ArrayList} object.
     */
    public final ArrayList<String> getHiddenExtrinsicKeyword() {
        final ArrayList<String> keywords = new ArrayList<String>();
        for (int i = 0; i < this.hiddenExtrinsicKeyword.size(); i++) {
            String keyword = this.hiddenExtrinsicKeyword.get(i);
            if (keyword.startsWith("HIDDEN")) {
                keyword = keyword.substring(7);
            }
            keywords.add(keyword);
        }
        return keywords;
    }

    /**
     * <p>
     * addHiddenExtrinsicKeyword.
     * </p>
     * 
     * @param s
     *            a {@link java.lang.String} object.
     */
    public final void addHiddenExtrinsicKeyword(final String s) {
        this.hiddenExtrinsicKeyword.add(s);
    }

    /**
     * <p>
     * removeHiddenExtrinsicKeyword.
     * </p>
     * 
     * @param s
     *            a {@link java.lang.String} object.
     */
    public final void removeHiddenExtrinsicKeyword(final String s) {
        this.hiddenExtrinsicKeyword.remove(s);
    }

    /**
     * <p>
     * setStaticAbilityStrings.
     * </p>
     * 
     * @param a
     *            a {@link java.util.ArrayList} object.
     */
    public final void setStaticAbilityStrings(final List<String> a) {
        this.getCharacteristics().setStaticAbilityStrings(new ArrayList<String>(a));
    }

    /**
     * Gets the static ability strings.
     * 
     * @return the static ability strings
     */
    public final List<String> getStaticAbilityStrings() {
        return this.getCharacteristics().getStaticAbilityStrings();
    }

    /**
     * <p>
     * addStaticAbilityStrings.
     * </p>
     * 
     * @param s
     *            a {@link java.lang.String} object.
     */
    public final void addStaticAbilityString(final String s) {
        if (StringUtils.isNotBlank(s)) {
            this.getCharacteristics().getStaticAbilityStrings().add(s);
        }
    }

    /**
     * Sets the static abilities.
     * 
     * @param a
     *            the new static abilities
     */
    public final void setStaticAbilities(final ArrayList<StaticAbility> a) {
        this.getCharacteristics().setStaticAbilities(new ArrayList<StaticAbility>(a));
    }

    /**
     * Gets the static abilities.
     * 
     * @return the static abilities
     */
    public final ArrayList<StaticAbility> getStaticAbilities() {
        return new ArrayList<StaticAbility>(this.getCharacteristics().getStaticAbilities());
    }

    /**
     * Adds the static ability.
     * 
     * @param s
     *            the s
     */
    public final void addStaticAbility(final String s) {

        if (s.trim().length() != 0) {
            final StaticAbility stAb = new StaticAbility(s, this);
            this.getCharacteristics().getStaticAbilities().add(stAb);
        }
    }


    public final boolean isPermanent() {
        return !(this.isInstant() || this.isSorcery() || this.isImmutable());
    }

    public final boolean isSpell() {
        return (this.isInstant() || this.isSorcery() || (this.isAura() && !this.isInZone((ZoneType.Battlefield))));
    }

    public final boolean isEmblem()     { return this.typeContains("Emblem"); }

    public final boolean isLand()       { return this.typeContains("Land"); }
    public final boolean isBasicLand()  { return this.typeContains("Basic"); }
    public final boolean isSnow()       { return this.typeContains("Snow"); }

    public final boolean isTribal()     { return this.typeContains("Tribal"); }
    public final boolean isSorcery()    { return this.typeContains("Sorcery"); }
    public final boolean isInstant()    { return this.typeContains("Instant"); }

    public final boolean isCreature()   { return this.typeContains("Creature"); }
    public final boolean isArtifact()   { return this.typeContains("Artifact"); }
    public final boolean isEquipment()  { return this.typeContains("Equipment"); }
    public final boolean isScheme()     { return this.typeContains("Scheme"); }
    

    public final boolean isPlaneswalker()   { return this.typeContains("Planeswalker"); }

    public final boolean isEnchantment()    { return this.typeContains("Enchantment"); }
    public final boolean isAura()           { return this.typeContains("Aura"); }



    private boolean typeContains(final String s) {
        final Iterator<String> it = this.getType().iterator();
        while (it.hasNext()) {
            if (it.next().startsWith(s)) {
                return true;
            }
        }

        return false;
    }

    /**
     * <p>
     * Setter for the field <code>uniqueNumber</code>.
     * </p>
     * 
     * @param n
     *            a int.
     */
    public final void setUniqueNumber(final int n) {
        //System.out.println("Card _ " + n);
        this.uniqueNumber = n;
        //this.updateObservers();
    }

    /**
     * <p>
     * Getter for the field <code>uniqueNumber</code>.
     * </p>
     * 
     * @return a int.
     */
    public final int getUniqueNumber() {
        return this.uniqueNumber;
    }

    /** {@inheritDoc} */
    @Override
    public final int compareTo(final Card that) {
        /*
         * Return a negative integer of this < that, a positive integer if this
         * > that, and zero otherwise.
         */

        if (that == null) {
            /*
             * "Here we can arbitrarily decide that all non-null Cards are
             * `greater than' null Cards. It doesn't really matter what we
             * return in this case, as long as it is consistent. I rather think
             * of null as being lowly." --Braids
             */
            return +1;
        } else if (this.getUniqueNumber() > that.getUniqueNumber()) {
            return +1;
        } else if (this.getUniqueNumber() < that.getUniqueNumber()) {
            return -1;
        } else {
            return 0;
        }
    }

    /** {@inheritDoc} */
    @Override
    public final boolean equals(final Object o) {
        if (o instanceof Card) {
            final Card c = (Card) o;
            final int a = this.getUniqueNumber();
            final int b = c.getUniqueNumber();
            return (a == b);
        }
        return false;
    }

    /** {@inheritDoc} */
    @Override
    public final int hashCode() {
        return this.getUniqueNumber();
    }

    /** {@inheritDoc} */
    @Override
    public final String toString() {
        return this.getName() + " (" + this.getUniqueNumber() + ")";
    }

    /**
     * <p>
     * isUnearthed.
     * </p>
     * 
     * @return a boolean.
     */
    public final boolean isUnearthed() {
        return this.unearthed;
    }

    /**
     * <p>
     * Setter for the field <code>unearthed</code>.
     * </p>
     * 
     * @param b
     *            a boolean.
     */
    public final void setUnearthed(final boolean b) {
        this.unearthed = b;
    }

    /**
     * <p>
     * Getter for the field <code>madnessCost</code>.
     * </p>
     * 
     * @return a {@link java.lang.String} object.
     */
    public final String getMadnessCost() {
        return this.madnessCost;
    }

    /**
     * <p>
     * Setter for the field <code>madnessCost</code>.
     * </p>
     * 
     * @param cost
     *            a {@link java.lang.String} object.
     */
    public final void setMadnessCost(final String cost) {
        this.madnessCost = cost;
    }

    /**
     * <p>
     * Getter for the field <code>miracleCost</code>.
     * </p>
     * 
     * @return a {@link java.lang.String} object.
     */
    public final String getMiracleCost() {
        return this.miracleCost;
    }

    /**
     * <p>
     * Setter for the field <code>miracleCost</code>.
     * </p>
     * 
     * @param cost
     *            a {@link java.lang.String} object.
     */
    public final void setMiracleCost(final String cost) {
        this.miracleCost = cost;
    }

    /**
     * <p>
     * hasSuspend.
     * </p>
     * 
     * @return a boolean.
     */
    public final boolean hasSuspend() {
        return this.suspend;
    }

    /**
     * <p>
     * Setter for the field <code>suspend</code>.
     * </p>
     * 
     * @param b
     *            a boolean.
     */
    public final void setSuspend(final boolean b) {
        this.suspend = b;
    }

    /**
     * <p>
     * wasSuspendCast.
     * </p>
     * 
     * @return a boolean.
     */
    public final boolean wasSuspendCast() {
        return this.suspendCast;
    }

    /**
     * <p>
     * Setter for the field <code>suspendCast</code>.
     * </p>
     * 
     * @param b
     *            a boolean.
     */
    public final void setSuspendCast(final boolean b) {
        this.suspendCast = b;
    }

    /**
     * Checks if is phased out.
     * 
     * @return true, if is phased out
     */
    public final boolean isPhasedOut() {
        return this.phasedOut;
    }

    /**
     * Sets the phased out.
     * 
     * @param phasedOut
     *            the new phased out
     */
    public final void setPhasedOut(final boolean phasedOut) {
        this.phasedOut = phasedOut;
    }

    /**
     * Phase.
     */
    public final void phase() {
        this.phase(true);
    }

    /**
     * Phase.
     * 
     * @param direct
     *            the direct
     */
    public final void phase(final boolean direct) {
        final boolean phasingIn = this.isPhasedOut();

        if (!this.switchPhaseState()) {
            // Switch Phase State returns False if the Permanent can't Phase Out
            return;
        }

        if (!phasingIn) {
            this.setDirectlyPhasedOut(direct);
        }

        for (final Card eq : this.getEquippedBy()) {
            if (eq.isPhasedOut() == phasingIn) {
                eq.phase(false);
            }
        }

        for (final Card aura : this.getEnchantedBy()) {
            if (aura.isPhasedOut() == phasingIn) {
                aura.phase(false);
            }
        }
    }

    private boolean switchPhaseState() {
        if (!this.phasedOut && this.hasKeyword("CARDNAME can't phase out.")) {
            return false;
        }

        this.phasedOut = !this.phasedOut;
        if (this.phasedOut && this.isToken()) {
            // 702.23k Phased-out tokens cease to exist as a state-based action.
            // See rule 704.5d.
            // 702.23d The phasing event doesn't actually cause a permanent to
            // change zones or control,
            // even though it's treated as though it's not on the battlefield
            // and not under its controller's control while it's phased out.
            // Zone-change triggers don't trigger when a permanent phases in or
            // out.

            // Suppressed Exiling is as close as we can get to
            // "ceasing to exist"
            getGame().getTriggerHandler().suppressMode(TriggerType.ChangesZone);
            getGame().getAction().exile(this);
            getGame().getTriggerHandler().clearSuppression(TriggerType.ChangesZone);
        }
        return true;
    }

    /**
     * Checks if is directly phased out.
     * 
     * @return true, if is directly phased out
     */
    public final boolean isDirectlyPhasedOut() {
        return this.directlyPhasedOut;
    }

    /**
     * Sets the directly phased out.
     * 
     * @param direct
     *            the new directly phased out
     */
    public final void setDirectlyPhasedOut(final boolean direct) {
        this.directlyPhasedOut = direct;
    }

    /**
     * <p>
     * isReflectedLand.
     * </p>
     * 
     * @return a boolean.
     */
    public final boolean isReflectedLand() {
        for (final SpellAbility a : this.getCharacteristics().getManaAbility()) {
            if (a.getApi() == ApiType.ManaReflected) {
                return true;
            }
        }

        return false;
    }

    /**
     * <p>
     * hasKeyword.
     * </p>
     * 
     * @param keyword
     *            a {@link java.lang.String} object.
     * @return a boolean.
     */
    @Override
    public final boolean hasKeyword(final String keyword) {
        String kw = keyword;
        if (kw.startsWith("HIDDEN")) {
            kw = kw.substring(7);
        }
        return this.getKeyword().contains(kw);
    }

    /**
     * <p>
     * hasStartOfKeyword.
     * </p>
     * 
     * @param keyword
     *            a {@link java.lang.String} object.
     * @return a boolean.
     */
    public final boolean hasStartOfKeyword(final String keyword) {
        final List<String> a = this.getKeyword();
        for (int i = 0; i < a.size(); i++) {
            if (a.get(i).toString().startsWith(keyword)) {
                return true;
            }
        }
        return false;
    }

    /**
     * <p>
     * hasStartOfKeyword.
     * </p>
     * 
     * @param keyword
     *            a {@link java.lang.String} object.
     * @return a boolean.
     */
    public final boolean hasStartOfUnHiddenKeyword(final String keyword) {
        final ArrayList<String> a = this.getUnhiddenKeyword();
        for (int i = 0; i < a.size(); i++) {
            if (a.get(i).toString().startsWith(keyword)) {
                return true;
            }
        }
        return false;
    }

    /**
     * <p>
     * getKeywordPosition.
     * </p>
     * 
     * @param k
     *            a {@link java.lang.String} object.
     * @return a int.
     */
    public final int getKeywordPosition(final String k) {
        final List<String> a = this.getKeyword();
        for (int i = 0; i < a.size(); i++) {
            if (a.get(i).toString().startsWith(k)) {
                return i;
            }
        }
        return -1;
    }

    /**
     * <p>
     * hasAnyKeyword.
     * </p>
     * 
     * @param keywords
     *            an array of {@link java.lang.String} objects.
     * @return a boolean.
     */
    public final boolean hasAnyKeyword(final Iterable<String> keywords) {
        for (final String keyword : keywords) {
            if (this.hasKeyword(keyword)) {
                return true;
            }
        }

        return false;
    }

    // This counts the number of instances of a keyword a card has
    /**
     * <p>
     * getAmountOfKeyword.
     * </p>
     * 
     * @param k
     *            a {@link java.lang.String} object.
     * @return a int.
     */
    public final int getAmountOfKeyword(final String k) {
        int count = 0;
        for (String kw : this.getKeyword()) {
            if (kw.equals(k)) {
                count++;
            }
        }

        return count;
    }

    // This is for keywords with a number like Bushido, Annihilator and Rampage.
    // It returns the total.
    /**
     * <p>
     * getKeywordMagnitude.
     * </p>
     * 
     * @param k
     *            a {@link java.lang.String} object.
     * @return a int.
     */
    public final int getKeywordMagnitude(final String k) {
        int count = 0;
        for (final String kw : this.getKeyword()) {
            if (kw.startsWith(k)) {
                final String[] parse = kw.split(" ");
                final String s = parse[1];
                count += Integer.parseInt(s);
            }
        }
        return count;
    }

    private String toMixedCase(final String s) {
        if (s.equals("")) {
            return s;
        }
        final StringBuilder sb = new StringBuilder();
        // to handle hyphenated Types
        final String[] types = s.split("-");
        for (int i = 0; i < types.length; i++) {
            if (i != 0) {
                sb.append("-");
            }
            sb.append(types[i].substring(0, 1).toUpperCase());
            sb.append(types[i].substring(1).toLowerCase());
        }

        return sb.toString();
    }

    // usable to check for changelings
    /**
     * <p>
     * isType.
     * </p>
     * 
     * @param type
     *            a {@link java.lang.String} object.
     * @return a boolean.
     */
    public final boolean isType(String type) {
        if (type == null) {
            return false;
        }

        type = this.toMixedCase(type);

        if (this.typeContains(type)
                || ((this.isCreature() || this.isTribal()) && forge.card.CardType.isACreatureType(type) && this
                        .typeContains("AllCreatureTypes"))) {
            return true;
        }
        return false;
    } // isType

    // Takes one argument like Permanent.Blue+withFlying
    /**
     * <p>
     * isValid.
     * </p>
     * 
     * @param restriction
     *            a {@link java.lang.String} object.
     * @param sourceController
     *            a {@link forge.game.player.Player} object.
     * @param source
     *            a {@link forge.Card} object.
     * @return a boolean.
     */
    @Override
    public final boolean isValid(final String restriction, final Player sourceController, final Card source) {

        if (this.isImmutable()
                && !source.getRemembered().contains(this)) { // special case exclusion
            return false;
        }

        // Inclusive restrictions are Card types
        final String[] incR = restriction.split("\\.", 2);

        boolean testFailed = false;
        if(incR[0].startsWith("!")) {
            testFailed = true; // a bit counter logical ))
            incR[0] = incR[0].substring(1); // consume negation sign
        }

        if (incR[0].equals("Spell") && !this.isSpell()) {
            return testFailed;
        }
        if (incR[0].equals("Permanent") && (this.isInstant() || this.isSorcery())) {
            return testFailed;
        }
        if (!incR[0].equals("card") && !incR[0].equals("Card") && !incR[0].equals("Spell")
                && !incR[0].equals("Permanent") && !(this.isType(incR[0]))) {
            return testFailed; // Check for wrong type
        }

        if (incR.length > 1) {
            final String excR = incR[1];
            final String[] exR = excR.split("\\+"); // Exclusive Restrictions are ...
            for (int j = 0; j < exR.length; j++) {
                if (!this.hasProperty(exR[j], sourceController, source)) {
                    return testFailed;
                }
            }
        }
        return !testFailed;
    } // isValid(String Restriction)

    // Takes arguments like Blue or withFlying
    /**
     * <p>
     * hasProperty.
     * </p>
     * 
     * @param property
     *            a {@link java.lang.String} object.
     * @param sourceController
     *            a {@link forge.game.player.Player} object.
     * @param source
     *            a {@link forge.Card} object.
     * @return a boolean.
     */
    @Override
    public boolean hasProperty(final String property, final Player sourceController, final Card source) {
        // by name can also have color names, so needs to happen before colors.
        if (property.startsWith("named")) {
            if (!this.getName().equals(property.substring(5))) {
                return false;
            }
        } else if (property.startsWith("notnamed")) {
            if (this.getName().equals(property.substring(8))) {
                return false;
            }
        } else if (property.startsWith("sameName")) {
            if (!this.getName().equals(source.getName())) {
                return false;
            }
        } else if (property.equals("NamedCard")) {
            if (!this.getName().equals(source.getNamedCard())) {
                return false;
            }
        } else if (property.equals("ChosenCard")) {
            if (!source.getChosenCard().contains(this)) {
                return false;
            }
        } else if (property.equals("nonChosenCard")) {
            if (source.getChosenCard().contains(this)) {
                return false;
            }
        }
        // ... Card colors
        else if (property.contains("White") || property.contains("Blue") || property.contains("Black")
                || property.contains("Red") || property.contains("Green") ) {
            boolean mustHave = !property.startsWith("non");
            int desiredColor = MagicColor.fromName(mustHave ? property : property.substring(3)); 
            boolean hasColor = CardUtil.getColors(this).hasAnyColor(desiredColor);
            if( mustHave != hasColor )
                return false;

        } else if (property.contains("Colorless")) { // ... Card is colorless
            if( property.startsWith("non") == CardUtil.getColors(this).isColorless() ) return false;

        } else if (property.contains("MultiColor")) { // ... Card is multicolored
            if( property.startsWith("non") == CardUtil.getColors(this).isMulticolor() ) return false;

        } else if (property.contains("MonoColor")) { // ... Card is monocolored
            if( property.startsWith("non") == CardUtil.getColors(this).isMonoColor() ) return false;

        } else if (property.equals("ChosenColor")) {
            if (source.getChosenColor().isEmpty() || !CardUtil.getColors(this).hasAnyColor(MagicColor.fromName(source.getChosenColor().get(0))))
                return false;

        } else if (property.equals("AllChosenColors")) {
            if ( source.getChosenColor().isEmpty() || !CardUtil.getColors(this).hasAllColors(ColorSet.fromNames(source.getChosenColor()).getColor()) )
                return false;

        } else if (property.equals("AnyChosenColor")) {
            if ( source.getChosenColor().isEmpty() || !CardUtil.getColors(this).hasAnyColor(ColorSet.fromNames(source.getChosenColor()).getColor()) )
                return false;

        } else if (property.equals("DoubleFaced")) {
            if (!this.isDoubleFaced()) {
                return false;
            }
        } else if (property.equals("Flip")) {
            if (!this.isFlipCard()) {
                return false;
            }
        } else if (property.startsWith("YouCtrl")) {
            if (!this.getController().equals(sourceController)) {
                return false;
            }
        } else if (property.startsWith("YouDontCtrl")) {
            if (this.getController().equals(sourceController)) {
                return false;
            }
        } else if (property.startsWith("OppCtrl")) {
            if (!this.getController().getOpponents().contains(sourceController)) {
                return false;
            }
        } else if (property.startsWith("ChosenCtrl")) {
            if (!this.getController().equals(source.getChosenPlayer())) {
                return false;
            }
        } else if (property.startsWith("DefenderCtrl")) {
            if (!getGame().getPhaseHandler().inCombat()) {
                return false;
            }
            if (!getGame().getCombat().getDefendingPlayerRelatedTo(source).contains(this.getController())) {
                return false;
            }
        } else if (property.startsWith("EnchantedPlayerCtrl")) {
            final Object o = source.getEnchanting();
            if (o instanceof Player) {
                if (!this.getController().equals(o)) {
                    return false;
                }
            } else { // source not enchanting a player
                return false;
            }
        } else if (property.startsWith("RememberedPlayerCtrl")) {
            if (source.getRemembered().isEmpty()) {
                final Card newCard = getGame().getCardState(source);
                for (final Object o : newCard.getRemembered()) {
                    if (o instanceof Player) {
                        if (!this.getController().equals(o)) {
                            return false;
                          }
                    }
                }
            }

            for (final Object o : source.getRemembered()) {
                if (o instanceof Player) {
                    if (!this.getController().equals(o)) {
                        return false;
                      }
                }
            }
        } else if (property.equals("TargetedPlayerCtrl")) {
            for (final SpellAbility sa : source.getCharacteristics().getSpellAbility()) {
                final SpellAbility saTargeting = sa.getSATargetingPlayer();
                if (saTargeting != null) {
                    for (final Player p : saTargeting.getTarget().getTargetPlayers()) {
                        if (!this.getController().equals(p)) {
                            return false;
                        }
                    }
                }
            }
        } else if (property.equals("TargetedControllerCtrl")) {
            for (final SpellAbility sa : source.getCharacteristics().getSpellAbility()) {
                final List<Card> list = AbilityUtils.getDefinedCards(source, "Targeted", sa);
                final List<SpellAbility> sas = AbilityUtils.getDefinedSpellAbilities(source, "Targeted", sa);
                for (final Card c : list) {
                    final Player p = c.getController();
                    if (!this.getController().equals(p)) {
                        return false;
                    }
                }
                for (final SpellAbility s : sas) {
                    final Player p = s.getSourceCard().getController();
                    if (!this.getController().equals(p)) {
                        return false;
                    }
                }
            }
        } else if (property.startsWith("ActivePlayerCtrl")) {
            if (!getGame().getPhaseHandler().isPlayerTurn(this.getController())) {
                return false;
            }
        } else if (property.startsWith("NonActivePlayerCtrl")) {
            if (getGame().getPhaseHandler().isPlayerTurn(this.getController())) {
                return false;
            }
        } else if (property.startsWith("YouOwn")) {
            if (!this.getOwner().equals(sourceController)) {
                return false;
            }
        } else if (property.startsWith("YouDontOwn")) {
            if (this.getOwner().equals(sourceController)) {
                return false;
            }
        } else if (property.startsWith("OppOwn")) {
            if (!this.getOwner().getOpponents().contains(sourceController)) {
                return false;
            }
        } else if (property.equals("TargetedPlayerOwn")) {
            for (final SpellAbility sa : source.getCharacteristics().getSpellAbility()) {
                final SpellAbility saTargeting = sa.getSATargetingPlayer();
                if (saTargeting != null) {
                    for (final Player p : saTargeting.getTarget().getTargetPlayers()) {
                        if (!this.getOwner().equals(p)) {
                            return false;
                        }
                    }
                }
            }
        } else if (property.startsWith("OwnedBy")) {
            final String valid = property.substring(8);
            if (!this.getOwner().isValid(valid, sourceController, source)) {
                return false;
            }
        } else if (property.startsWith("OwnerDoesntControl")) {
            if (this.getOwner().equals(this.getController())) {
                return false;
            }
        } else if (property.startsWith("ControllerControls")) {
            final String type = property.substring(18);
            final List<Card> list = this.getController().getCardsIn(ZoneType.Battlefield);
            if (CardLists.getType(list, type).isEmpty()) {
                return false;
            }
        } else if (property.startsWith("Other")) {
            if (this.equals(source)) {
                return false;
            }
        } else if (property.startsWith("Self")) {
            if (!this.equals(source)) {
                return false;
            }
        } else if (property.startsWith("AttachedBy")) {
            if (!this.equippedBy.contains(source) && !this.getEnchantedBy().contains(source)) {
                return false;
            }
        } else if (property.equals("Attached")) {
            if (!this.equipping.contains(source) && !source.equals(this.enchanting)) {
                return false;
            }
        } else if (property.startsWith("AttachedTo")) {
            final String restriction = property.split("AttachedTo ")[1];
            if (restriction.equals("Targeted")) {
                if (!source.getCharacteristics().getTriggers().isEmpty()) {
                    for (final Trigger t : source.getCharacteristics().getTriggers()) {
                        final SpellAbility sa = t.getTriggeredSA();
                        final List<Card> list = AbilityUtils.getDefinedCards(source, "Targeted", sa);
                        for (final Card c : list) {
                            if (!this.equipping.contains(c) && !c.equals(this.enchanting)) {
                                return false;
                            }
                        }
                    }
                } else {
                    for (final SpellAbility sa : source.getCharacteristics().getSpellAbility()) {
                        final List<Card> list = AbilityUtils.getDefinedCards(source, "Targeted", sa);
                        for (final Card c : list) {
                            if (!this.equipping.contains(c) && !c.equals(this.enchanting)) {
                                return false;
                            }
                        }
                    }
                }
            } else {
                if (((this.enchanting == null) || !this.enchanting.isValid(restriction, sourceController, source))
                        && (this.equipping.isEmpty() || !this.equipping.get(0).isValid(restriction, sourceController,
                                source))) {
                    return false;
                }
            }
        } else if (property.equals("NameNotEnchantingEnchantedPlayer")) {
            Player enchantedPlayer = source.getEnchantingPlayer();
            if (enchantedPlayer == null) {
                return false;
            }

            List<Card> enchanting = enchantedPlayer.getEnchantedBy();
            for (Card c : enchanting) {
                if (this.getName().equals(c.getName())) {
                    return false;
                }
            }
        } else if (property.equals("NotAttachedTo")) {
            if (this.equipping.contains(source) || source.equals(this.enchanting)) {
                return false;
            }
        } else if (property.startsWith("EnchantedBy")) {
            if (property.equals("EnchantedBy")) {
                if (!this.getEnchantedBy().contains(source) && !this.equals(source.getEnchanting())) {
                    return false;
                }
            } else {
                final String restriction = property.split("EnchantedBy ")[1];
                if (restriction.equals("Imprinted")) {
                    for (final Card card : source.getImprinted()) {
                        if (!this.getEnchantedBy().contains(card) && !this.equals(card.getEnchanting())) {
                            return false;
                        }
                    }
                } else if (restriction.equals("Targeted")) {
                    for (final SpellAbility sa : source.getCharacteristics().getSpellAbility()) {
                        final SpellAbility saTargeting = sa.getSATargetingCard();
                        if (saTargeting != null) {
                            for (final Card c : saTargeting.getTarget().getTargetCards()) {
                                if (!this.getEnchantedBy().contains(c) && !this.equals(c.getEnchanting())) {
                                    return false;
                                }
                            }
                        }
                    }
                }
            }
        } else if (property.startsWith("NotEnchantedBy")) {
            if (property.substring(14).equals("Targeted")) {
                for (final SpellAbility sa : source.getCharacteristics().getSpellAbility()) {
                    final SpellAbility saTargeting = sa.getSATargetingCard();
                    if (saTargeting != null) {
                        for (final Card c : saTargeting.getTarget().getTargetCards()) {
                            if (this.getEnchantedBy().contains(c)) {
                                return false;
                            }
                        }
                    }
                }
            } else {
                if (this.getEnchantedBy().contains(source)) {
                    return false;
                }
            }
        } else if (property.startsWith("Enchanted")) {
            if (!source.equals(this.enchanting)) {
                return false;
            }
        } else if (property.startsWith("CanEnchantSource")) {
            if (!source.canBeEnchantedBy(this)) {
                return false;
            }
        } else if (property.startsWith("CanBeEnchantedBy")) {
            if (property.substring(16).equals("Targeted")) {
                for (final SpellAbility sa : source.getCharacteristics().getSpellAbility()) {
                    final SpellAbility saTargeting = sa.getSATargetingCard();
                    if (saTargeting != null) {
                        for (final Card c : saTargeting.getTarget().getTargetCards()) {
                            if (!this.canBeEnchantedBy(c)) {
                                return false;
                            }
                        }
                    }
                }
            } else if (property.substring(16).equals("AllRemembered")) {
                for (final Object rem : source.getRemembered()) {
                    if (rem instanceof Card) {
                        final Card card = (Card) rem;
                        if (!this.canBeEnchantedBy(card)) {
                            return false;
                        }
                    }
                }
            } else {
                if (!this.canBeEnchantedBy(source)) {
                    return false;
                }
            }
        } else if (property.startsWith("EquippedBy")) {
            if (property.substring(10).equals("Targeted")) {
                for (final SpellAbility sa : source.getCharacteristics().getSpellAbility()) {
                    final SpellAbility saTargeting = sa.getSATargetingCard();
                    if (saTargeting != null) {
                        for (final Card c : saTargeting.getTarget().getTargetCards()) {
                            if (!this.equippedBy.contains(c)) {
                                return false;
                            }
                        }
                    }
                }
            } else if (property.substring(10).equals("Enchanted")) {
                if (source.getEnchantingCard() == null || 
                        !this.equippedBy.contains(source.getEnchantingCard())) {
                    return false;
                }
            } else {
                if (!this.equippedBy.contains(source)) {
                    return false;
                }
            }
        } else if (property.startsWith("CanBeEquippedBy")) {
            if (!this.canBeEquippedBy(source)) {
                return false;
            }
        } else if (property.startsWith("Equipped")) {
            if (!this.equipping.contains(source)) {
                return false;
            }
        } else if (property.startsWith("HauntedBy")) {
            if (!this.hauntedBy.contains(source)) {
                return false;
            }
        } else if (property.contains("Paired")) {
            if (property.contains("With")) { // PairedWith
                if (!this.isPaired() || this.pairedWith != source) {
                    return false;
                }
            } else if (property.startsWith("Not")) {  // NotPaired
                if (this.isPaired()) {
                    return false;
                }
            } else { // Paired
                if (!this.isPaired()) {
                    return false;
                }
            }
        } else if (property.startsWith("Above")) { // "Are Above" Source
            final List<Card> list = this.getOwner().getCardsIn(ZoneType.Graveyard);
            if (list.indexOf(source) >= list.indexOf(this)) {
                return false;
            }
        } else if (property.startsWith("DirectlyAbove")) { // "Are Directly Above" Source
            final List<Card> list = this.getOwner().getCardsIn(ZoneType.Graveyard);
            if (list.indexOf(this) - list.indexOf(source) != 1) {
                return false;
            }
        } else if (property.startsWith("TopGraveyardCreature")) {
            List<Card> list = CardLists.filter(this.getOwner().getCardsIn(ZoneType.Graveyard), CardPredicates.Presets.CREATURES);
            Collections.reverse(list);
            if (list.isEmpty() || !this.equals(list.get(0))) {
                return false;
            }
        } else if (property.startsWith("TopGraveyard")) {
            final List<Card> list = new ArrayList<Card>(this.getOwner().getCardsIn(ZoneType.Graveyard));
            Collections.reverse(list);
            if (property.substring(12).matches("[0-9][0-9]?")) {
                int n = Integer.parseInt(property.substring(12));
                int num = Math.min(n, list.size());
                final List<Card> newlist = new ArrayList<Card>();
                for (int i = 0; i < num; i++) {
                    newlist.add(list.get(i));
                }
                if (list.isEmpty() || !newlist.contains(this)) {
                    return false;
                }
            } else {
                if (list.isEmpty() || !this.equals(list.get(0))) {
                    return false;
                }
            }
        } else if (property.startsWith("BottomGraveyard")) {
            final List<Card> list = this.getOwner().getCardsIn(ZoneType.Graveyard);
            if (list.isEmpty() || !this.equals(list.get(0))) {
                return false;
            }
        } else if (property.startsWith("TopLibrary")) {
            final List<Card> list = this.getOwner().getCardsIn(ZoneType.Library);
            if (list.isEmpty() || !this.equals(list.get(0))) {
                return false;
            }
        } else if (property.startsWith("Cloned")) {
            if ((this.cloneOrigin == null) || !this.cloneOrigin.equals(source)) {
                return false;
            }
        } else if (property.startsWith("DamagedBy")) {
            if (!this.receivedDamageFromThisTurn.containsKey(source)) {
                return false;
            }
        } else if (property.startsWith("Damaged")) {
            if (!this.dealtDamageToThisTurn.containsKey(source)) {
                return false;
            }
        } else if (property.startsWith("IsTargetingSource")) {
            for (final SpellAbility sa : this.getCharacteristics().getSpellAbility()) {
                final SpellAbility saTargeting = sa.getSATargetingCard();
                if (saTargeting != null) {
                    for (final Card c : saTargeting.getTarget().getTargetCards()) {
                        if (c.equals(source)) {
                            return true;
                        }
                    }
                }
            }
            return false;
        } else if (property.startsWith("SharesColorWith")) {
            if (property.equals("SharesColorWith")) {
                if (!this.sharesColorWith(source)) {
                    return false;
                }
            } else {
                final String restriction = property.split("SharesColorWith ")[1];
                if (restriction.equals("TopCardOfLibrary")) {
                    final List<Card> list = sourceController.getCardsIn(ZoneType.Library);
                    if (list.isEmpty() || !this.sharesColorWith(list.get(0))) {
                        return false;
                    }
                } else if (restriction.equals("Remembered")) {
                    for (final Object obj : source.getRemembered()) {
                        if (!(obj instanceof Card) || !this.sharesColorWith((Card) obj)) {
                            return false;
                        }
                    }
                } else if (restriction.equals("Imprinted")) {
                    for (final Card card : source.getImprinted()) {
                        if (!this.sharesColorWith(card)) {
                            return false;
                        }
                    }
                } else if (restriction.equals("Equipped")) {
                    if (!source.isEquipment() || !source.isEquipping()
                            || !this.sharesColorWith(source.getEquippingCard())) {
                        return false;
                    }
                } else if (restriction.equals("MostProminentColor")) {
                    byte mask = CardFactoryUtil.getMostProminentColors(getGame().getCardsIn(ZoneType.Battlefield));
                    if( !CardUtil.getColors(this).hasAnyColor(mask))
                        return false;
                } else if (restriction.equals("LastCastThisTurn")) {
                    final List<Card> c = source.getGame().getStack().getCardsCastThisTurn();
                    if (c.isEmpty() || !this.sharesColorWith(c.get(c.size() - 1))) {
                        return false;
                    }
                } else {
                    for (final Card card : sourceController.getCardsIn(ZoneType.Battlefield)) {
                        if (card.isValid(restriction, sourceController, source) && this.sharesColorWith(card)) {
                            return true;
                        }
                    }
                    return false;
                }
            }
        } else if (property.startsWith("MostProminentColor")) {
            // MostProminentColor <color>
            // e.g. MostProminentColor black
            String[] props = property.split(" ");
            if (props.length == 1) {
                System.out.println("WARNING! Using MostProminentColor property without a color.");
                return false;
            }
            String color = props[1];

            byte mostProm = CardFactoryUtil.getMostProminentColors(getGame().getCardsIn(ZoneType.Battlefield));
            return ColorSet.fromMask(mostProm).hasAnyColor(MagicColor.fromName(color));
        } else if (property.startsWith("notSharesColorWith")) {
            if (property.equals("notSharesColorWith")) {
                if (this.sharesColorWith(source)) {
                    return false;
                }
            } else {
                final String restriction = property.split("notSharesColorWith ")[1];
                for (final Card card : sourceController.getCardsIn(ZoneType.Battlefield)) {
                    if (card.isValid(restriction, sourceController, source) && this.sharesColorWith(card)) {
                        return false;
                    }
                }
            }
        } else if (property.startsWith("sharesCreatureTypeWith")) {
            if (property.equals("sharesCreatureTypeWith")) {
                if (!this.sharesCreatureTypeWith(source)) {
                    return false;
                }
            } else {
                final String restriction = property.split("sharesCreatureTypeWith ")[1];
                if (restriction.equals("TopCardOfLibrary")) {
                    final List<Card> list = sourceController.getCardsIn(ZoneType.Library);
                    if (list.isEmpty() || !this.sharesCreatureTypeWith(list.get(0))) {
                        return false;
                    }
                } if (restriction.equals("Enchanted")) {
                    for (final SpellAbility sa : source.getCharacteristics().getSpellAbility()) {
                        final SpellAbility root = sa.getRootAbility();
                        Card c = source.getEnchantingCard();
                        if ((c == null) && (root != null)
                            && (root.getPaidList("Sacrificed") != null)
                            && !root.getPaidList("Sacrificed").isEmpty()) {
                            c = root.getPaidList("Sacrificed").get(0).getEnchantingCard();
                            if (!this.sharesCreatureTypeWith(c)) {
                                return false;
                            }
                        }
                    }
                } if (restriction.equals("Equipped")) {
                    if (source.isEquipping() && this.sharesCreatureTypeWith(source.getEquippingCard())) {
                        return true;
                    }
                    return false;
                } if (restriction.equals("Remembered")) {
                    for (final Object rem : source.getRemembered()) {
                        if (rem instanceof Card) {
                            final Card card = (Card) rem;
                            if (this.sharesCreatureTypeWith(card)) {
                                return true;
                            }
                        }
                    }
                    return false;
                } if (restriction.equals("AllRemembered")) {
                    for (final Object rem : source.getRemembered()) {
                        if (rem instanceof Card) {
                            final Card card = (Card) rem;
                            if (!this.sharesCreatureTypeWith(card)) {
                                return false;
                            }
                        }
                    }
                } else {
                    boolean shares = false;
                    for (final Card card : sourceController.getCardsIn(ZoneType.Battlefield)) {
                        if (card.isValid(restriction, sourceController, source) && this.sharesCreatureTypeWith(card)) {
                            shares = true;
                        }
                    }
                    if (!shares) {
                        return false;
                    }
                }
            }
        } else if (property.startsWith("sharesCardTypeWith")) {
            if (property.equals("sharesCardTypeWith")) {
                if (!this.sharesCardTypeWith(source)) {
                    return false;
                }
            } else {
                final String restriction = property.split("sharesCardTypeWith ")[1];
                if (restriction.equals("Imprinted")) {
                    if (source.getImprinted().isEmpty() || !this.sharesCardTypeWith(source.getImprinted().get(0))) {
                        return false;
                    }
                } else if (restriction.equals("Remembered")) {
                    for (final Object rem : source.getRemembered()) {
                        if (rem instanceof Card) {
                            final Card card = (Card) rem;
                            if (this.sharesCardTypeWith(card)) {
                                return true;
                            }
                        }
                    }
                    return false;
                } else if (restriction.equals("EachTopLibrary")) {
                    final List<Card> list = new ArrayList<Card>();
                    for (Player p : getGame().getPlayers()) {
                        final Card top = p.getCardsIn(ZoneType.Library).get(0);
                        list.add(top);
                    }
                    for (Card c : list) {
                        if (this.sharesCardTypeWith(c)) {
                            return true;
                        }
                    }
                    return false;
                }
            }
        } else if (property.startsWith("sharesNameWith")) {
            if (property.equals("sharesNameWith")) {
                if (!this.getName().equals(source.getName())) {
                    return false;
                }
            } else {
                final String restriction = property.split("sharesNameWith ")[1];
                if (restriction.equals("YourGraveyard")) {
                    for (final Card card : sourceController.getCardsIn(ZoneType.Graveyard)) {
                        if (this.getName().equals(card.getName())) {
                            return true;
                        }
                    }
                    return false;
                } else if (restriction.equals(ZoneType.Graveyard.toString())) {
                    for (final Card card : getGame().getCardsIn(ZoneType.Graveyard)) {
                        if (this.getName().equals(card.getName())) {
                            return true;
                        }
                    }
                    return false;
                } else if (restriction.equals(ZoneType.Battlefield.toString())) {
                    for (final Card card : getGame().getCardsIn(ZoneType.Battlefield)) {
                        if (this.getName().equals(card.getName())) {
                            return true;
                        }
                    }
                    return false;
                } else if (restriction.equals("ThisTurnCast")) {
                    for (final Card card : CardUtil.getThisTurnCast("Card", source)) {
                        if (this.getName().equals(card.getName())) {
                            return true;
                        }
                    }
                    return false;
                } else if (restriction.equals("Remembered")) {
                    for (final Object rem : source.getRemembered()) {
                        if (rem instanceof Card) {
                            final Card card = (Card) rem;
                            if (this.getName().equals(card.getName())) {
                                return true;
                            }
                        }
                    }
                    return false;
                } else if (restriction.equals("Imprinted")) {
                    for (final Card card : source.getImprinted()) {
                        if (this.getName().equals(card.getName())) {
                            return true;
                        }
                    }
                    return false;
                } else if (restriction.equals("NonToken")) {
                    final List<Card> list = CardLists.filter(getGame().getCardsIn(ZoneType.Battlefield),
                            Presets.NON_TOKEN);
                    for (final Card card : list) {
                        if (this.getName().equals(card.getName())) {
                            return true;
                        }
                    }
                    return false;
                }
            }
        } else if (property.startsWith("doesNotShareNameWith")) {
            if (property.equals("doesNotShareNameWith")) {
                if (this.getName().equals(source.getName())) {
                    return false;
                }
            } else {
                final String restriction = property.split("doesNotShareNameWith ")[1];
                if (restriction.equals("Remembered")) {
                    for (final Object rem : source.getRemembered()) {
                        if (rem instanceof Card) {
                            final Card card = (Card) rem;
                            if (this.getName().equals(card.getName())) {
                                return false;
                            }
                        }
                    }
                }
            }
        } else if (property.startsWith("sharesControllerWith")) {
            if (property.equals("sharesControllerWith")) {
                if (!this.sharesControllerWith(source)) {
                    return false;
                }
            } else {
                final String restriction = property.split("sharesControllerWith ")[1];
                if (restriction.equals("Remembered")) {
                    for (final Object rem : source.getRemembered()) {
                        if (rem instanceof Card) {
                            final Card card = (Card) rem;
                            if (!this.sharesControllerWith(card)) {
                                return false;
                            }
                        }
                    }
                } else if (restriction.equals("Imprinted")) {
                    for (final Card card : source.getImprinted()) {
                        if (!this.sharesControllerWith(card)) {
                            return false;
                        }
                    }
                }
            }
        } else if (property.startsWith("sharesOwnerWith")) {
            if (property.equals("sharesOwnerWith")) {
                if (!this.getOwner().equals(source.getOwner())) {
                    return false;
                }
            } else {
                final String restriction = property.split("sharesOwnerWith ")[1];
                if (restriction.equals("Remembered")) {
                    for (final Object rem : source.getRemembered()) {
                        if (rem instanceof Card) {
                            final Card card = (Card) rem;
                            if (!this.getOwner().equals(card.getOwner())) {
                                return false;
                            }
                        }
                    }
                }
            }
        } else if (property.startsWith("SecondSpellCastThisTurn")) {
            final List<Card> list = CardUtil.getThisTurnCast("Card", source);
            if (list.size() < 2)  {
                return false;
            }
            else if (list.get(1) != this) {
                return false;
            }
        } else if (property.equals("ThisTurnCast")) {
            for (final Card card : CardUtil.getThisTurnCast("Card", source)) {
                if (this.equals(card)) {
                    return true;
                }
            }
            return false;
        } else if (property.startsWith("ThisTurnEntered")) {
            final String restrictions = property.split("ThisTurnEntered_")[1];
            final String[] res = restrictions.split("_");
            final ZoneType destination = ZoneType.smartValueOf(res[0]);
            ZoneType origin = null;
            if (res[1].equals("from")) {
                origin = ZoneType.smartValueOf(res[2]);
            }
            List<Card> list = CardUtil.getThisTurnEntered(destination,
                    origin, "Card", source);
            if (!list.contains(this)) {
                return false;
            }
        } else if (property.startsWith("sharesTypeWith")) {
            if (property.equals("sharesTypeWith")) {
                if (!this.sharesTypeWith(source)) {
                    return false;
                }
            } else {
                final String restriction = property.split("sharesTypeWith ")[1];
                if (restriction.equals("FirstImprinted")) {
                    if (source.getImprinted().isEmpty() || 
                            !this.sharesTypeWith(source.getImprinted().get(0))) {
                        return false;
                    }
                }
            }
        } else if (property.startsWith("withFlashback")) {
            boolean fb = false;
            if (this.hasStartOfUnHiddenKeyword("Flashback")) {
                fb = true;
            }
            for (final SpellAbility sa : this.getSpellAbilities()) {
                if (sa.isFlashBackAbility()) {
                    fb = true;
                }
            }
            if (!fb) {
                return false;
            }
        } else if (property.startsWith("with")) {
            // ... Card keywords
            if (property.startsWith("without") && this.hasStartOfUnHiddenKeyword(property.substring(7))) {
                return false;
            }
            if (!property.startsWith("without") && !this.hasStartOfUnHiddenKeyword(property.substring(4))) {
                return false;
            }
        } else if (property.startsWith("tapped")) {
            if (!this.isTapped()) {
                return false;
            }
        } else if (property.startsWith("untapped")) {
            if (!this.isUntapped()) {
                return false;
            }
        } else if (property.startsWith("faceDown")) {
            if (!this.isFaceDown()) {
                return false;
            }
        } else if (property.startsWith("faceUp")) {
            if (this.isFaceDown()) {
                return false;
            }
        } else if (property.startsWith("hasLevelUp")) {
            if (!this.hasLevelUp()) {
                return false;
            }
        } else if (property.startsWith("DrawnThisTurn")) {
          if (!this.getDrawnThisTurn()) {
              return false;
          }
        } else if (property.startsWith("enteredBattlefieldThisTurn")) {
            if (!(this.getTurnInZone() == getGame().getPhaseHandler().getTurn())) {
                return false;
            }
        } else if (property.startsWith("notEnteredBattlefieldThisTurn")) {
            if (this.getTurnInZone() == getGame().getPhaseHandler().getTurn()) {
                return false;
            }
        } else if (property.startsWith("firstTurnControlled")) {
            if (!this.isFirstTurnControlled()) {
                return false;
            }
        } else if (property.startsWith("notFirstTurnControlled")) {
            if (this.isFirstTurnControlled()) {
                return false;
            }
        } else if (property.startsWith("dealtDamageToYouThisTurn")) {
            if (!this.getDamageHistory().getThisTurnDamaged().contains(sourceController)) {
                return false;
            }
        } else if (property.startsWith("dealtDamageToOppThisTurn")) {
            if (!this.getDamageHistory().getThisTurnDamaged().contains(sourceController.getOpponent())) {
                return false;
            }
        } else if (property.startsWith("controllerWasDealtCombatDamageByThisTurn")) {
            if (!source.getDamageHistory().getThisTurnCombatDamaged().contains(this.getController())) {
                return false;
            }
        } else if (property.startsWith("controllerWasDealtDamageByThisTurn")) {
            if (!source.getDamageHistory().getThisTurnDamaged().contains(this.getController())) {
                return false;
            }
        } else if (property.startsWith("wasDealtDamageThisTurn")) {
            if ((this.getReceivedDamageFromThisTurn().keySet()).isEmpty()) {
                return false;
            }
        } else if (property.equals("wasDealtDamageByHostThisTurn")) {
            if (!this.getReceivedDamageFromThisTurn().keySet().contains(source)) {
                return false;
            }
        } else if (property.equals("wasDealtDamageByEquipeeThisTurn")) {
            Card equipee = source.getEquippingCard();
            if (this.getReceivedDamageFromThisTurn().keySet().isEmpty()
                    || !this.getReceivedDamageFromThisTurn().keySet().contains(equipee)) {
                return false;
            }
         } else if (property.equals("wasDealtDamageByEnchantedThisTurn")) {
            Card enchanted = source.getEnchantingCard();
            if (this.getReceivedDamageFromThisTurn().keySet().isEmpty()
                    || !this.getReceivedDamageFromThisTurn().keySet().contains(enchanted)) {
                return false;
            }
        } else if (property.startsWith("dealtDamageThisTurn")) {
            if (this.getTotalDamageDoneBy() == 0) {
                return false;
            }
        } else if (property.startsWith("attackedThisTurn")) {
            if (!this.getDamageHistory().getCreatureAttackedThisTurn()) {
                return false;
            }
        } else if (property.startsWith("attackedLastTurn")) {
            return this.getDamageHistory().getCreatureAttackedLastTurnOf(this.getController());
        } else if (property.startsWith("blockedThisTurn")) {
            if (!this.getDamageHistory().getCreatureBlockedThisTurn()) {
                return false;
            }
        } else if (property.startsWith("gotBlockedThisTurn")) {
            if (!this.getDamageHistory().getCreatureGotBlockedThisTurn()) {
                return false;
            }
        } else if (property.startsWith("notAttackedThisTurn")) {
            if (this.getDamageHistory().getCreatureAttackedThisTurn()) {
                return false;
            }
        } else if (property.startsWith("notAttackedLastTurn")) {
            return !this.getDamageHistory().getCreatureAttackedLastTurnOf(this.getController());
        } else if (property.startsWith("notBlockedThisTurn")) {
            if (this.getDamageHistory().getCreatureBlockedThisTurn()) {
                return false;
            }
        } else if (property.startsWith("greatestPower")) {
            final List<Card> list = CardLists.filter(getGame().getCardsIn(ZoneType.Battlefield), Presets.CREATURES);
            for (final Card crd : list) {
                if (crd.getNetAttack() > this.getNetAttack()) {
                    return false;
                }
            }
        } else if (property.startsWith("yardGreatestPower")) {
            final List<Card> list = CardLists.filter(sourceController.getCardsIn(ZoneType.Graveyard), Presets.CREATURES);
            for (final Card crd : list) {
                if (crd.getNetAttack() > this.getNetAttack()) {
                    return false;
                }
            }
        } else if (property.startsWith("leastPower")) {
            final List<Card> list = CardLists.filter(getGame().getCardsIn(ZoneType.Battlefield), Presets.CREATURES);
            for (final Card crd : list) {
                if (crd.getNetAttack() < this.getNetAttack()) {
                    return false;
                }
            }
        } else if (property.startsWith("leastToughness")) {
            final List<Card> list = CardLists.filter(getGame().getCardsIn(ZoneType.Battlefield), Presets.CREATURES);
            for (final Card crd : list) {
                if (crd.getNetDefense() < this.getNetDefense()) {
                    return false;
                }
            }
        } else if (property.startsWith("greatestCMC")) {
            final List<Card> list = CardLists.filter(getGame().getCardsIn(ZoneType.Battlefield), Presets.CREATURES);
            for (final Card crd : list) {
                if (crd.isSplitCard()) {
                    if (crd.getCMC(Card.SplitCMCMode.LeftSplitCMC) > this.getCMC() || crd.getCMC(Card.SplitCMCMode.RightSplitCMC) > this.getCMC()) {
                        return false;
                    }
                } else {
                    if (crd.getCMC() > this.getCMC()) {
                        return false;
                    }
                }
            }
        } else if (property.startsWith("greatestRememberedCMC")) {
            List<Card> list = new ArrayList<Card>();
            for (final Object o : source.getRemembered()) {
                if (o instanceof Card) {
                    list.add(getGame().getCardState((Card) o));
                }
            }
            if (!list.contains(this)) {
                return false;
            }
            list = CardLists.getCardsWithHighestCMC(list);
            if (!list.contains(this)) {
                return false;
            }
        } else if (property.startsWith("lowestCMC")) {
            final List<Card> list = getGame().getCardsIn(ZoneType.Battlefield);
            for (final Card crd : list) {
                if (!crd.isLand() && !crd.isImmutable()) {
                    if (crd.isSplitCard()) {
                        if (crd.getCMC(Card.SplitCMCMode.LeftSplitCMC) < this.getCMC() || crd.getCMC(Card.SplitCMCMode.RightSplitCMC) < this.getCMC()) {
                            return false;
                        }
                    } else {
                        if (crd.getCMC() < this.getCMC()) {
                            return false;
                        }
                    }
                }
            }
        } else if (property.startsWith("enchanted")) {
            if (!this.isEnchanted()) {
                return false;
            }
        } else if (property.startsWith("unenchanted")) {
            if (this.isEnchanted()) {
                return false;
            }
        } else if (property.startsWith("enchanting")) {
            if (!this.isEnchanting()) {
                return false;
            }
        } else if (property.startsWith("equipped")) {
            if (!this.isEquipped()) {
                return false;
            }
        } else if (property.startsWith("unequipped")) {
            if (this.isEquipped()) {
                return false;
            }
        } else if (property.startsWith("equipping")) {
            if (!this.isEquipping()) {
                return false;
            }
        } else if (property.startsWith("token")) {
            if (!this.isToken()) {
                return false;
            }
        } else if (property.startsWith("nonToken")) {
            if (this.isToken()) {
                return false;
            }
        } else if (property.startsWith("hasXCost")) {
            SpellAbility sa1 = this.getFirstSpellAbility();
            if( sa1 != null && !sa1.isXCost()) {
                return false;
            }
        } else if (property.startsWith("suspended")) {
            if (!this.hasSuspend() || !getGame().isCardExiled(this)
                    || !(this.getCounters(CounterType.getType("TIME")) >= 1)) {
                return false;
            }

        } else if (property.startsWith("power") || property.startsWith("toughness")
                || property.startsWith("cmc") || property.startsWith("totalPT")) {
            int x = 0;
            int y = 0;
            int y2 = -1; // alternative value for the second split face of a split card
            String rhs = "";

            if (property.startsWith("power")) {
                rhs = property.substring(7);
                y = this.getNetAttack();
            } else if (property.startsWith("toughness")) {
                rhs = property.substring(11);
                y = this.getNetDefense();
            } else if (property.startsWith("cmc")) {
                rhs = property.substring(5);
                if (isSplitCard() && getCurState() == CardCharacteristicName.Original) {
                    y = getState(CardCharacteristicName.LeftSplit).getManaCost().getCMC();
                    y2 = getState(CardCharacteristicName.RightSplit).getManaCost().getCMC();
                } else {
                    y = getCMC();
                }
            } else if (property.startsWith("totalPT")) {
                rhs = property.substring(10);
                y = this.getNetAttack() + this.getNetDefense();
            }
            try {
                x = Integer.parseInt(rhs);
            } catch (final NumberFormatException e) {
                x = CardFactoryUtil.xCount(source, source.getSVar(rhs));
            }

            if (y2 == -1) {
                if (!Expressions.compare(y, property, x)) {
                    return false;
                }
            } else {
                if (!Expressions.compare(y, property, x) || !Expressions.compare(y2, property, x)) {
                    return false;
                }
            }
        }

        // syntax example: countersGE9 P1P1 or countersLT12TIME (greater number
        // than 99 not supported)
        /*
         * slapshot5 - fair warning, you cannot use numbers with 2 digits
         * (greater number than 9 not supported you can use X and the
         * SVar:X:Number$12 to get two digits. This will need a better fix, and
         * I have the beginnings of a regex below
         */
        else if (property.startsWith("counters")) {
            /*
             * Pattern p = Pattern.compile("[a-z]*[A-Z][A-Z][X0-9]+.*$");
             * String[] parse = ???
             * System.out.println("Parsing completed of: "+Property); for(int i
             * = 0; i < parse.length; i++) {
             * System.out.println("parse["+i+"]: "+parse[i]); }
             */

            // TODO get a working regex out of this pattern so the amount of
            // digits doesn't matter
            int number = 0;
            final String[] splitProperty = property.split("_");
            final String strNum = splitProperty[1].substring(2);
            final String comparator = splitProperty[1].substring(0, 2);
            String counterType = "";
            try {
                number = Integer.parseInt(strNum);
            } catch (final NumberFormatException e) {
                number = CardFactoryUtil.xCount(source, source.getSVar(strNum));
            }
            counterType = splitProperty[2];

            final int actualnumber = this.getCounters(CounterType.getType(counterType));

            if (!Expressions.compare(actualnumber, comparator, number)) {
                return false;
            }
        } else if (property.startsWith("attacking")) {
            if (property.equals("attacking")) {
                if (!this.isAttacking()) {
                    return false;
                }
            } else if (property.equals("attackingYou")) {
                if (!this.isAttacking(sourceController)) {
                    return false;
                }
            }
        } else if (property.startsWith("notattacking")) {
            if (this.isAttacking()) {
                return false;
            }
        } else if (property.equals("attackedBySourceThisCombat")) {
            final GameEntity defender = getGame().getCombat().getDefenderByAttacker(source);
            if (defender instanceof Card) {
                if (!this.equals((Card) defender)) {
                    return false;
                }
            }
        } else if (property.equals("blocking")) {
            if (!this.isBlocking()) {
                return false;
            }
        } else if (property.startsWith("blockingSource")) {
            if (!this.isBlocking(source)) {
                return false;
            }
        } else if (property.startsWith("blockingCreatureYouCtrl")) {
            final List<Card> list = CardLists.filter(sourceController.getCardsIn(ZoneType.Battlefield), Presets.CREATURES);
            for (final Card c : list) {
                if (this.isBlocking(c)) {
                    return true;
                }
            return false;
            }
        } else if (property.startsWith("blockingRemembered")) {
            for (final Object o : source.getRemembered()) {
                if (o instanceof Card) {
                    final Card card = (Card) o;
                    if (this.isBlocking(card)) {
                        return true;
                    }
                }
            }
            return false;
        } else if (property.startsWith("isBlockedByRemembered")) {
            for (final Object o : source.getRemembered()) {
                if (o instanceof Card) {
                    final Card crd = (Card) o;
                    if (this.isBlockedBy(crd)) {
                        return true;
                    }
                }
            }
            return false;
        } else if (property.startsWith("blockedRemembered")) {
            Card rememberedcard;
            for (final Object o : source.getRemembered()) {
                if (o instanceof Card) {
                    rememberedcard = (Card) o;
                    if (this.getBlockedThisTurn() == null || !this.getBlockedThisTurn().contains(rememberedcard)) {
                        return false;
                    }
                }
            }
        } else if (property.startsWith("blockedByRemembered")) {
            Card rememberedcard;
            for (final Object o : source.getRemembered()) {
                if (o instanceof Card) {
                    rememberedcard = (Card) o;
                    if (this.getBlockedByThisTurn() == null || !this.getBlockedByThisTurn().contains(rememberedcard)) {
                        return false;
                    }
                }
            }
        } else if (property.startsWith("notblocking")) {
            if (this.isBlocking()) {
                return false;
            }
        } else if (property.equals("blocked")) {
            if (!this.isBlocked()) {
                return false;
            }
        } else if (property.startsWith("blockedBySource")) {
            if (!this.isBlockedBy(source)) {
                return false;
            }
        } else if (property.startsWith("unblocked")) {
            if (!getGame().getCombat().isUnblocked(this)) {
                return false;
            }
        } else if (property.startsWith("kicked")) {
            if (property.equals("kicked")) {
                if (this.getKickerMagnitude() == 0) {
                    return false;
                }
            } else {
                String s = property.split("kicked ")[1];
                if ("1".equals(s) && !this.isOptionalCostPaid(OptionalCost.Kicker1)) return false;
                if ("2".equals(s) && !this.isOptionalCostPaid(OptionalCost.Kicker2)) return false;
            }
        } else if (property.startsWith("notkicked")) {
            if (this.getKickerMagnitude() > 0) {
                return false;
            }
        } else if (property.startsWith("evoked")) {
            if (!this.isEvoked()) {
                return false;
            }
        } else if (property.equals("HasDevoured")) {
            if (this.devouredCards.size() == 0) {
                return false;
            }
        } else if (property.equals("HasNotDevoured")) {
            if (this.devouredCards.size() != 0) {
                return false;
            }
        } else if (property.startsWith("non")) {
            // ... Other Card types
            if (this.isType(property.substring(3))) {
                return false;
            }
        } else if (property.equals("CostsPhyrexianMana")) {
            if (!this.getCharacteristics().getManaCost().hasPhyrexian()) {
                return false;
            }
        } else if (property.equals("IsRemembered")) {
            if (!source.getRemembered().contains(this)) {
                return false;
            }
        } else if (property.equals("IsNotRemembered")) {
            if (source.getRemembered().contains(this)) {
                return false;
            }
        } else if (property.equals("IsImprinted")) {
            if (!source.getImprinted().contains(this)) {
                return false;
            }
        } else if (property.equals("IsNotImprinted")) {
            if (source.getImprinted().contains(this)) {
                return false;
            }
        } else if (property.equals("hasActivatedAbilityWithTapCost")) {
            for (final SpellAbility sa : this.getSpellAbilities()) {
                if (sa.isAbility() && (sa.getPayCosts() != null) && sa.getPayCosts().hasTapCost()) {
                    return true;
                }
            }
            return false;
        } else if (property.equals("hasActivatedAbility")) {
            for (final SpellAbility sa : this.getSpellAbilities()) {
                if (sa.isAbility()) {
                    return true;
                }
            }
            return false;
        } else if (property.equals("hasManaAbility")) {
            for (final SpellAbility sa : this.getSpellAbilities()) {
                if (sa.isManaAbility()) {
                    return true;
                }
            }
            return false;
        } else if (property.equals("hasNonManaActivatedAbility")) {
            for (final SpellAbility sa : this.getSpellAbilities()) {
                if (sa.isAbility() && !sa.isManaAbility()) {
                    return true;
                }
            }
            return false;
        } else if (property.equals("NoAbilities")) {
            if (!((this.getAbilityText().trim().equals("") || this.isFaceDown()) && (this.getUnhiddenKeyword().size() == 0))) {
                return false;
            }
        } else if (property.equals("HasCounters")) {
            if (!this.hasCounters()) {
                return false;
            }
        } else if (property.startsWith("wasCastFrom")) {
            final String strZone = property.substring(11);
            final ZoneType realZone = ZoneType.smartValueOf(strZone);
            if (realZone != this.getCastFrom()) {
                return false;
            }
        } else if (property.startsWith("wasNotCastFrom")) {
            final String strZone = property.substring(14);
            final ZoneType realZone = ZoneType.smartValueOf(strZone);
            if (realZone == this.getCastFrom()) {
                return false;
            }
        } else if (property.startsWith("set")) {
            final String setCode = property.substring(3, 6);
            if (!this.getCurSetCode().equals(setCode)) {
                return false;
            }
        } else if (property.startsWith("inZone")) {
            final String strZone = property.substring(6);
            final ZoneType realZone = ZoneType.smartValueOf(strZone);
            if (!this.isInZone(realZone)) {
                return false;
            }
        } else if (property.equals("ChosenType")) {
            if (!this.isType(source.getChosenType())) {
                return false;
            }
        } else if (property.equals("IsNotChosenType")) {
            if (this.isType(source.getChosenType())) {
                return false;
            }
        } else if (property.equals("IsCommander")) {
            if(!this.isCommander) {
                return false;
            }
        } else {
            if (!this.isType(property)) {
                return false;
            }
        }
        return true;
    } // hasProperty

    /**
     * <p>
     * setImmutable.
     * </p>
     * 
     * @param isImmutable
     *            a boolean.
     */
    public final void setImmutable(final boolean isImmutable) {
        this.isImmutable = isImmutable;
    }

    /**
     * <p>
     * isImmutable.
     * </p>
     * 
     * @return a boolean.
     */
    public final boolean isImmutable() {
        return this.isImmutable;
    }

    /*
     * there are easy checkers for Color. The CardUtil functions should be made
     * part of the Card class, so calling out is not necessary
     */

    public final boolean isOfColor(final String col) { return CardUtil.getColors(this).hasAnyColor(MagicColor.fromName(col)); }
    public final boolean isBlack() { return CardUtil.getColors(this).hasBlack(); }
    public final boolean isBlue() { return CardUtil.getColors(this).hasBlue(); }
    public final boolean isRed() { return CardUtil.getColors(this).hasRed(); }
    public final boolean isGreen() { return CardUtil.getColors(this).hasGreen(); }
    public final boolean isWhite() { return CardUtil.getColors(this).hasWhite(); }
    public final boolean isColorless() { return CardUtil.getColors(this).isColorless(); }

    /**
     * <p>
     * sharesColorWith.
     * </p>
     * 
     * @param c1
     *            a {@link forge.Card} object.
     * @return a boolean.
     */
    public final boolean sharesColorWith(final Card c1) {
        boolean shares = false;
        shares |= (this.isBlack() && c1.isBlack());
        shares |= (this.isBlue() && c1.isBlue());
        shares |= (this.isGreen() && c1.isGreen());
        shares |= (this.isRed() && c1.isRed());
        shares |= (this.isWhite() && c1.isWhite());
        return shares;
    }

    /**
     * <p>
     * sharesCreatureTypeWith.
     * </p>
     * 
     * @param c1
     *            a {@link forge.Card} object.
     * @return a boolean.
     */
    public final boolean sharesCreatureTypeWith(final Card c1) {

        if (c1 == null) {
            return false;
        }

        for (final String type : this.getType()) {
            if (type.equals("AllCreatureTypes") 
                    && (c1.hasACreatureType() || c1.typeContains("AllCreatureTypes"))) {
                return true;
            }
            if (forge.card.CardType.isACreatureType(type) && c1.isType(type)) {
                return true;
            }
        }
        return false;
    }

    /**
     * <p>
     * sharesTypeWith.
     * </p>
     * 
     * @param c1
     *            a {@link forge.Card} object.
     * @return a boolean.
     */
    public final boolean sharesCardTypeWith(final Card c1) {

        for (final String type : this.getType()) {
            if (forge.card.CardType.isACardType(type) && c1.isType(type)) {
                return true;
            }
        }
        return false;
    }

    /**
     * <p>
     * sharesTypeWith.
     * </p>
     * 
     * @param c1
     *            a {@link forge.Card} object.
     * @return a boolean.
     */
    public final boolean sharesTypeWith(final Card c1) {

        for (final String type : this.getType()) {
            if (c1.isType(type)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * <p>
     * sharesControllerWith.
     * </p>
     * 
     * @param c1
     *            a {@link forge.Card} object.
     * @return a boolean.
     */
    public final boolean sharesControllerWith(final Card c1) {

        if (c1 == null) {
            return false;
        }
        
        return this.getController().equals(c1.getController());
    }

    /**
     * <p>
     * hasACreatureType.
     * </p>
     * 
     * @return a boolean.
     */
    public final boolean hasACreatureType() {
        for (final String type : this.getType()) {
            if (forge.card.CardType.isACreatureType(type)) {
                return true;
            }
        }
        return false;
    }

    /**
     * <p>
     * isUsedToPay.
     * </p>
     * 
     * @return a boolean.
     */
    public final boolean isUsedToPay() {
        return this.usedToPayCost;
    }

    /**
     * <p>
     * setUsedToPay.
     * </p>
     * 
     * @param b
     *            a boolean.
     */
    public final void setUsedToPay(final boolean b) {
        this.usedToPayCost = b;
    }

    /**
     * <p>
     * isAttacking.
     * </p>
     * 
     * @return a boolean.
     */
    public final boolean isAttacking() {
        return getGame().getCombat().isAttacking(this);
    }

    /**
     * <p>
     * isAttacking.
     * </p>
     * @param ge    the GameEntity to check
     * @return a boolean.
     */
    public final boolean isAttacking(GameEntity ge) {
        Combat combat = getGame().getCombat();
        if (!combat.isAttacking(this)) {
            return false;
        }
        return combat.getDefenderByAttacker(this).equals(ge);
    }

    /**
     * <p>
     * isBlocking.
     * </p>
     * 
     * @return a boolean.
     */
    public final boolean isBlocking() {
        final List<Card> blockers = getGame().getCombat().getAllBlockers();
        return blockers.contains(this);
    }

    /**
     * <p>
     * isBlocked.
     * </p>
     * 
     * @return a boolean.
     */
    public final boolean isBlocked() {
        return getGame().getCombat().isBlocked(this);
    }

    /**
     * <p>
     * isBlocking.
     * </p>
     * 
     * @param attacker
     *            a {@link forge.Card} object.
     * @return a boolean.
     */
    public final boolean isBlocking(final Card attacker) {
        return getGame().getCombat().getAttackersBlockedBy(this).contains(attacker);
    }

    /**
     * <p>
     * isBlockedBy.
     * </p>
     * 
     * @param blocker
     *            a {@link forge.Card} object.
     * @return a boolean.
     */
    public final boolean isBlockedBy(final Card blocker) {
        return getGame().getCombat().getAttackersBlockedBy(blocker).contains(this);
    }

    // /////////////////////////
    //
    // Damage code
    //
    // ////////////////////////

    /**
     * <p>
     * addReceivedDamageFromThisTurn.
     * </p>
     * 
     * @param c
     *            a {@link forge.Card} object.
     * @param damage
     *            a int.
     */
    public final void addReceivedDamageFromThisTurn(final Card c, final int damage) {
        this.receivedDamageFromThisTurn.put(c, damage);
    }

    /**
     * <p>
     * Setter for the field <code>receivedDamageFromThisTurn</code>.
     * </p>
     * 
     * @param receivedDamageList
     *            a Map object.
     */
    public final void setReceivedDamageFromThisTurn(final Map<Card, Integer> receivedDamageList) {
        this.receivedDamageFromThisTurn = receivedDamageList;
    }

    /**
     * <p>
     * Getter for the field <code>receivedDamageFromThisTurn</code>.
     * </p>
     * 
     * @return a Map object.
     */
    public final Map<Card, Integer> getReceivedDamageFromThisTurn() {
        return this.receivedDamageFromThisTurn;
    }

    /**
     * <p>
     * resetReceivedDamageFromThisTurn.
     * </p>
     */
    public final void resetReceivedDamageFromThisTurn() {
        this.receivedDamageFromThisTurn.clear();
    }

    public final int getTotalDamageRecievedThisTurn() {
        int total = 0;
        for (int damage : this.receivedDamageFromThisTurn.values()) {
            total += damage;
        }
        return total;
    }

    /**
     * <p>
     * addDealtDamageToThisTurn.
     * </p>
     * 
     * @param c
     *            a {@link forge.Card} object.
     * @param damage
     *            a int.
     */
    public final void addDealtDamageToThisTurn(final Card c, final int damage) {
        this.dealtDamageToThisTurn.put(c, damage);
    }

    /**
     * <p>
     * Setter for the field <code>dealtDamageToThisTurn</code>.
     * </p>
     * 
     * @param dealtDamageList
     *            a {@link java.util.Map} object.
     */
    public final void setDealtDamageToThisTurn(final Map<Card, Integer> dealtDamageList) {
        this.dealtDamageToThisTurn = dealtDamageList;
    }

    /**
     * <p>
     * Getter for the field <code>dealtDamageToThisTurn</code>.
     * </p>
     * 
     * @return a {@link java.util.Map} object.
     */
    public final Map<Card, Integer> getDealtDamageToThisTurn() {
        return this.dealtDamageToThisTurn;
    }

    /**
     * <p>
     * resetDealtDamageToThisTurn.
     * </p>
     */
    public final void resetDealtDamageToThisTurn() {
        this.dealtDamageToThisTurn.clear();
    }

    /**
     * <p>
     * addDealtDamageToPlayerThisTurn.
     * </p>
     * 
     * @param player
     *            player as name String.
     * @param damage
     *            a int.
     */
    public final void addDealtDamageToPlayerThisTurn(final String player, final int damage) {
        this.dealtDamageToPlayerThisTurn.put(player, damage);
    }

    /**
     * <p>
     * Setter for the field <code>dealtDamageToPlayerThisTurn</code>.
     * </p>
     * 
     * @param dealtDamageList
     *            a {@link java.util.Map} object.
     */
    public final void setDealtDamageToPlayerThisTurn(final Map<String, Integer> dealtDamageList) {
        this.dealtDamageToPlayerThisTurn = dealtDamageList;
    }

    /**
     * <p>
     * Getter for the field <code>dealtDamageToPlayerThisTurn</code>.
     * </p>
     * 
     * @return a {@link java.util.Map} object.
     */
    public final Map<String, Integer> getDealtDamageToPlayerThisTurn() {
        return this.dealtDamageToPlayerThisTurn;
    }

    /**
     * <p>
     * resetDealtDamageToPlayerThisTurn.
     * </p>
     */
    public final void resetDealtDamageToPlayerThisTurn() {
        this.dealtDamageToPlayerThisTurn.clear();
    }

    // this is the minimal damage a trampling creature has to assign to a
    // blocker
    /**
     * <p>
     * getLethalDamage.
     * </p>
     * 
     * @return a int.
     */
    public final int getLethalDamage() {
        final int lethalDamage = this.getNetDefense() - this.getDamage() - this.getTotalAssignedDamage();

        return lethalDamage;
    }

    /**
     * <p>
     * Setter for the field <code>damage</code>.
     * </p>
     * 
     * @param n
     *            a int.
     */
    public final void setDamage(final int n) {
        this.damage = n;
    }

    /**
     * <p>
     * Getter for the field <code>damage</code>.
     * </p>
     * 
     * @return a int.
     */
    public final int getDamage() {
        return this.damage;
    }

    /**
     * <p>
     * addAssignedDamage.
     * </p>
     * 
     * @param damage
     *            a int.
     * @param sourceCard
     *            a {@link forge.Card} object.
     */
    public final void addAssignedDamage(int damage, final Card sourceCard) {
        if (damage < 0) {
            damage = 0;
        }

        final int assignedDamage = damage;

        Log.debug(this + " - was assigned " + assignedDamage + " damage, by " + sourceCard);
        if (!this.assignedDamageMap.containsKey(sourceCard)) {
            this.assignedDamageMap.put(sourceCard, assignedDamage);
        } else {
            this.assignedDamageMap.put(sourceCard, this.assignedDamageMap.get(sourceCard) + assignedDamage);
        }

        Log.debug("***");
        /*
         * if(sourceCards.size() > 1)
         * System.out.println("(MULTIPLE blockers):");
         * System.out.println("Assigned " + damage + " damage to " + card); for
         * (int i=0;i<sourceCards.size();i++){
         * System.out.println(sourceCards.get(i).getName() +
         * " assigned damage to " + card.getName()); }
         * System.out.println("***");
         */
    }

    /**
     * <p>
     * clearAssignedDamage.
     * </p>
     */
    public final void clearAssignedDamage() {
        this.assignedDamageMap.clear();
    }

    /**
     * <p>
     * getTotalAssignedDamage.
     * </p>
     * 
     * @return a int.
     */
    public final int getTotalAssignedDamage() {
        int total = 0;

        final Collection<Integer> c = this.assignedDamageMap.values();

        final Iterator<Integer> itr = c.iterator();
        while (itr.hasNext()) {
            total += itr.next();
        }

        return total;
    }

    /**
     * <p>
     * Getter for the field <code>assignedDamageMap</code>.
     * </p>
     * 
     * @return a {@link java.util.Map} object.
     */
    public final Map<Card, Integer> getAssignedDamageMap() {
        return this.assignedDamageMap;
    }

    /**
     * <p>
     * addCombatDamage.
     * </p>
     * 
     * @param map
     *            a {@link java.util.Map} object.
     */
    public final void addCombatDamage(final Map<Card, Integer> map) {
        final List<Card> list = new ArrayList<Card>();

        for (final Entry<Card, Integer> entry : map.entrySet()) {
            final Card source = entry.getKey();
            list.add(source);
            int damageToAdd = entry.getValue();

            damageToAdd = this.replaceDamage(damageToAdd, source, true);
            damageToAdd = this.preventDamage(damageToAdd, source, true);

            map.put(source, damageToAdd);
        }

        if (this.isInPlay()) {
            this.addDamage(map);
        }
    }

    // This should be also usable by the AI to forecast an effect (so it must not change the game state)
    /**
     * <p>
     * staticDamagePrevention.
     * </p>
     * 
     * @param damage
     *            a int.
     * @param possiblePrevention
     *            a int.
     * @param source
     *            a {@link forge.Card} object.
     * @param isCombat
     *            a boolean.
     * @return a int.
     */
    public final int staticDamagePrevention(final int damage, final int possiblePrevention, final Card source,
            final boolean isCombat) {

        if (getGame().getStaticEffects().getGlobalRuleChange(GlobalRuleChange.noPrevention)) {
            return damage;
        }

        for (final Card ca : getGame().getCardsIn(ZoneType.Battlefield)) {
            for (final ReplacementEffect re : ca.getReplacementEffects()) {
                Map<String, String> params = re.getMapParams();
                if (!"DamageDone".equals(params.get("Event")) || !params.containsKey("PreventionEffect")) {
                    continue;
                }
                if (params.containsKey("ValidSource")
                        && !source.isValid(params.get("ValidSource"), ca.getController(), ca)) {
                    continue;
                }
                if (params.containsKey("ValidTarget")
                        && !this.isValid(params.get("ValidTarget"), ca.getController(), ca)) {
                    continue;
                }
                if (params.containsKey("IsCombat")) {
                    if (params.get("IsCombat").equals("True")) {
                        if (!isCombat) {
                            continue;
                        }
                    } else {
                        if (isCombat) {
                            continue;
                        }
                    }

                }
                return 0;
            }
        }

        int restDamage = damage - possiblePrevention;

        restDamage = this.staticDamagePrevention(restDamage, source, isCombat, true);

        return restDamage;
    }

    // This should be also usable by the AI to forecast an effect (so it must not change the game state)
    /**
     * <p>
     * staticDamagePrevention.
     * </p>
     * 
     * @param damageIn
     *            a int.
     * @param source
     *            a {@link forge.Card} object.
     * @param isCombat
     *            a boolean.
     * @return a int.
     */
    @Override
    public final int staticDamagePrevention(final int damageIn, final Card source, final boolean isCombat, final boolean isTest) {

        if (getGame().getStaticEffects().getGlobalRuleChange(GlobalRuleChange.noPrevention)) {
            return damageIn;
        }

        if (isCombat && getGame().getPhaseHandler().isPreventCombatDamageThisTurn()) {
            return 0;
        }

        int restDamage = damageIn;

        if (this.hasProtectionFrom(source)) {
            return 0;
        }

        for (String kw : source.getKeyword()) {
            if (isCombat) {
                if (kw.equals("Prevent all combat damage that would be dealt to and dealt by CARDNAME.")) {
                    return 0;
                }
                if (kw.equals("Prevent all combat damage that would be dealt by CARDNAME.")) {
                    return 0;
                }
            }
            if (kw.equals("Prevent all damage that would be dealt to and dealt by CARDNAME.")) {
                return 0;
            }
            if (kw.equals("Prevent all damage that would be dealt by CARDNAME.")) {
                return 0;
            }
        }
        for (String kw : this.getKeyword()) {
            if (isCombat) {
                if (kw.equals("Prevent all combat damage that would be dealt to and dealt by CARDNAME.")) {
                    return 0;
                }
                if (kw.equals("Prevent all combat damage that would be dealt to CARDNAME.")) {
                    return 0;
                }
            }
            if (kw.equals("Prevent all damage that would be dealt to CARDNAME.")) {
                return 0;
            }
            if (kw.equals("Prevent all damage that would be dealt to and dealt by CARDNAME.")) {
                return 0;
            }
            if (kw.startsWith("Absorb")) {
                final int absorbed = this.getKeywordMagnitude("Absorb");
                if (restDamage > absorbed) {
                    restDamage = restDamage - absorbed;
                } else {
                    return 0;
                }
            }
            if (kw.startsWith("PreventAllDamageBy")) {
                String valid = this.getKeyword().get(this.getKeywordPosition("PreventAllDamageBy"));
                valid = valid.split(" ", 2)[1];
                if (source.isValid(valid, this.getController(), this)) {
                    return 0;
                }
            }
        }

        // Prevent Damage static abilities
        for (final Card ca : getGame().getCardsIn(ZoneType.listValueOf("Battlefield,Command"))) {
            final ArrayList<StaticAbility> staticAbilities = ca.getStaticAbilities();
            for (final StaticAbility stAb : staticAbilities) {
                restDamage = stAb.applyAbility("PreventDamage", source, this, restDamage, isCombat, isTest);
            }
        }

        // specific Cards
        if (this.isCreature()) { // and not a planeswalker
            if (this.getName().equals("Swans of Bryn Argoll")) {
                return 0;
            }

            if (source.isCreature() && getGame().isCardInPlay("Well-Laid Plans")
                    && source.sharesColorWith(this)) {
                return 0;
            }
        } // Creature end


        return restDamage > 0 ? restDamage : 0;
    }

    /**
     * <p>
     * preventDamage.
     * </p>
     * 
     * @param damage
     *            a int.
     * @param source
     *            a {@link forge.Card} object.
     * @param isCombat
     *            a boolean.
     * @return a int.
     */
    @Override
    public final int preventDamage(final int damage, final Card source, final boolean isCombat) {

        if (getGame().getStaticEffects().getGlobalRuleChange(GlobalRuleChange.noPrevention)
                || source.hasKeyword("Damage that would be dealt by CARDNAME can't be prevented.")) {
            return damage;
        }

        int restDamage = damage;

        if (this.getName().equals("Swans of Bryn Argoll")) {
            source.getController().drawCards(restDamage);
            return 0;
        }

        final HashMap<String, Object> repParams = new HashMap<String, Object>();
        repParams.put("Event", "DamageDone");
        repParams.put("Affected", this);
        repParams.put("DamageSource", source);
        repParams.put("DamageAmount", damage);
        repParams.put("IsCombat", isCombat);
        repParams.put("Prevention", true);

        if (getGame().getReplacementHandler().run(repParams) != ReplacementResult.NotReplaced) {
            return 0;
        }

        restDamage = this.staticDamagePrevention(restDamage, source, isCombat, false);

        if (restDamage == 0) {
            return 0;
        }

        if (this.hasKeyword("If damage would be dealt to CARDNAME, "
                + "prevent that damage. Remove a +1/+1 counter from CARDNAME.")) {
            restDamage = 0;
            this.subtractCounter(CounterType.P1P1, 1);
        }

        if (restDamage >= this.getPreventNextDamage()) {
            restDamage = restDamage - this.getPreventNextDamage();
            this.setPreventNextDamage(0);
        } else {
            this.setPreventNextDamage(this.getPreventNextDamage() - restDamage);
            restDamage = 0;
        }

        return restDamage;
    }

    // This should be also usable by the AI to forecast an effect (so it must not change the game state)
    /**
     * <p>
     * staticReplaceDamage.
     * </p>
     * 
     * @param damage
     *            a int.
     * @param source
     *            a {@link forge.Card} object.
     * @param isCombat
     *            a boolean.
     * @return a int.
     */
    @Override
    public final int staticReplaceDamage(final int damage, final Card source, final boolean isCombat) {

        int restDamage = damage;
        for (Card c : getGame().getCardsIn(ZoneType.Battlefield)) {
            if (c.getName().equals("Sulfuric Vapors")) {
                if (source.isSpell() && source.isRed()) {
                    restDamage += 1;
                }
            } else if (c.getName().equals("Pyromancer's Swath")) {
                if (c.getController().equals(source.getController()) && (source.isInstant() || source.isSorcery())
                        && this.isCreature()) {
                    restDamage += 2;
                }
            } else if (c.getName().equals("Furnace of Rath")) {
                if (this.isCreature()) {
                    restDamage += restDamage;
                }
            } else if (c.getName().equals("Gratuitous Violence")) {
                if (c.getController().equals(source.getController()) && source.isCreature() && this.isCreature()) {
                    restDamage += restDamage;
                }
            } else if (c.getName().equals("Fire Servant")) {
                if (c.getController().equals(source.getController()) && source.isRed()
                        && (source.isInstant() || source.isSorcery())) {
                    restDamage *= 2;
                }
            } else if (c.getName().equals("Gisela, Blade of Goldnight")) {
                if (!c.getController().equals(this.getController())) {
                    restDamage *= 2;
                }
            } else if (c.getName().equals("Inquisitor's Flail")) {
                if (isCombat && c.getEquippingCard() != null
                        && (c.getEquippingCard().equals(this) || c.getEquippingCard().equals(source))) {
                    restDamage *= 2;
                }
            } else if (c.getName().equals("Ghosts of the Innocent")) {
                if (this.isCreature()) {
                    restDamage = restDamage / 2;
                }
            } else if (c.getName().equals("Benevolent Unicorn")) {
                if (source.isSpell() && this.isCreature()) {
                   restDamage -= 1;
                }
            } else if (c.getName().equals("Divine Presence")) {
                if (restDamage > 3 && this.isCreature()) {
                    restDamage = 3;
                }
            } else if (c.getName().equals("Lashknife Barrier")) {
                if (c.getController().equals(this.getController()) && this.isCreature()) {
                    restDamage -= 1;
                }
            }
        }

        if (this.getName().equals("Phytohydra")) {
            return 0;
        }

        return restDamage;
    }

    /**
     * <p>
     * replaceDamage.
     * </p>
     * 
     * @param damageIn
     *            a int.
     * @param source
     *            a {@link forge.Card} object.
     * @param isCombat
     *            a boolean.
     * @return a int.
     */
    @Override
    public final int replaceDamage(final int damageIn, final Card source, final boolean isCombat) {
        // Replacement effects
        final HashMap<String, Object> repParams = new HashMap<String, Object>();
        repParams.put("Event", "DamageDone");
        repParams.put("Affected", this);
        repParams.put("DamageSource", source);
        repParams.put("DamageAmount", damageIn);
        repParams.put("IsCombat", isCombat);

        if (getGame().getReplacementHandler().run(repParams) != ReplacementResult.NotReplaced) {
            return 0;
        }

        return damageIn;
    }

    /**
     * <p>
     * addDamage.
     * </p>
     * 
     * @param sourcesMap
     *            a {@link java.util.Map} object.
     */
    public final void addDamage(final Map<Card, Integer> sourcesMap) {
        for (final Entry<Card, Integer> entry : sourcesMap.entrySet()) {
            // damage prevention is already checked!
            this.addDamageAfterPrevention(entry.getValue(), entry.getKey(), true);
        }
    }

    /**
     * <p>
     * addDamageAfterPrevention.
     * </p>
     * This function handles damage after replacement and prevention effects are
     * applied.
     * 
     * @param damageIn
     *            a int.
     * @param source
     *            a {@link forge.Card} object.
     * @param isCombat
     *            a boolean.
     * @return whether or not damage as dealt
     */
    @Override
    public final boolean addDamageAfterPrevention(final int damageIn, final Card source, final boolean isCombat) {
        final int damageToAdd = damageIn;

        if (damageToAdd == 0) {
            return false; // Rule 119.8
        }

        this.addReceivedDamageFromThisTurn(source, damageToAdd);
        source.addDealtDamageToThisTurn(this, damageToAdd);

        if (source.hasKeyword("Lifelink")) {
            source.getController().gainLife(damageToAdd, source);
        }

        // Run triggers
        final Map<String, Object> runParams = new TreeMap<String, Object>();
        runParams.put("DamageSource", source);
        runParams.put("DamageTarget", this);
        runParams.put("DamageAmount", damageToAdd);
        runParams.put("IsCombatDamage", isCombat);
        getGame().getTriggerHandler().runTrigger(TriggerType.DamageDone, runParams, false);

        GameEventCardDamaged.DamageType damageType = DamageType.Normal;
        if (this.isPlaneswalker()) {
            this.subtractCounter(CounterType.LOYALTY, damageToAdd);
            damageType = DamageType.LoyaltyLoss;
        } else {

            final Game game = source.getGame();
            
            final String s = this + " - destroy";

            final int amount = this.getAmountOfKeyword("When CARDNAME is dealt damage, destroy it.");
            if(amount > 0) {
                final Ability abDestroy = new Ability(source, ManaCost.ZERO){ 
                    @Override public void resolve() { game.getAction().destroy(Card.this, this); }
                };
                abDestroy.setStackDescription(s + ", it cannot be regenerated.");

                for (int i = 0; i < amount; i++) {
                    game.getStack().addSimultaneousStackEntry(abDestroy);
                }
            }
        
            final int amount2 = this.getAmountOfKeyword("When CARDNAME is dealt damage, destroy it. It can't be regenerated.");
            if( amount2 > 0 ) {
                final Ability abDestoryNoRegen = new Ability(source, ManaCost.ZERO){ 
                    @Override public void resolve() { game.getAction().destroyNoRegeneration(Card.this, this); }
                };
                abDestoryNoRegen.setStackDescription(s);
            
                for (int i = 0; i < amount2; i++) {
                    game.getStack().addSimultaneousStackEntry(abDestoryNoRegen);
                }
            }
        

            
                
            boolean wither = (getGame().getStaticEffects().getGlobalRuleChange(GlobalRuleChange.alwaysWither)
                    || source.hasKeyword("Wither") || source.hasKeyword("Infect"));

            if (this.isInPlay()) {
                if (wither) {
                    this.addCounter(CounterType.M1M1, damageToAdd, true);
                    damageType = DamageType.M1M1Counters;
                } else
                    this.damage += damageToAdd;
            }
            
            if (source.hasKeyword("Deathtouch") && this.isCreature()) {
                getGame().getAction().destroy(this, null);
                damageType = DamageType.Deathtouch;
            } 

            // Play the Damage sound
            game.fireEvent(new GameEventCardDamaged(this, source, damageToAdd, damageType));
        }

        return true;
    }

    /**
     * <p>
     * Setter for the field <code>curSetCode</code>.
     * </p>
     * 
     * @param setCode
     *            a {@link java.lang.String} object.
     */
    public final void setCurSetCode(final String setCode) {
        this.getCharacteristics().setCurSetCode(setCode);
    }

    /**
     * <p>
     * Getter for the field <code>curSetCode</code>.
     * </p>
     * 
     * @return a {@link java.lang.String} object.
     */
    public final String getCurSetCode() {
        return this.getCharacteristics().getCurSetCode();
    }

    /**
     * <p>
     * getCurSetRarity.
     * </p>
     * 
     * @return a {@link java.lang.String} object.
     */
    public final CardRarity getRarity() {
        return this.getCharacteristics().getRarity();
    }

    public final void setRarity(CardRarity r) {
        this.getCharacteristics().setRarity(r);
    }
    
    /**
     * <p>
     * getMostRecentSet.
     * </p>
     * 
     * @return a {@link java.lang.String} object.
     */
    public final String getMostRecentSet() {
        return CardDb.instance().getCard(this.getName()).getEdition();
    }

    public final void setImageKey(final String iFN) {
        this.getCharacteristics().setImageKey(iFN);
    }

    public final String getImageKey() {
        return this.getCharacteristics().getImageKey();
    }

    /**
     * <p>
     * Setter for the field <code>evoked</code>.
     * </p>
     * 
     * @param evokedIn
     *            a boolean.
     */
    public final void setEvoked(final boolean evokedIn) {
        this.evoked = evokedIn;
    }

    /**
     * <p>
     * isEvoked.
     * </p>
     * 
     * @return a boolean.
     */
    public final boolean isEvoked() {
        return this.evoked;
    }

    /**
     * 
     * TODO Write javadoc for this method.
     * 
     * @param t
     *            a long
     */
    public final void setTimestamp(final long t) {
        this.timestamp = t;
    }

    /**
     * 
     * TODO Write javadoc for this method.
     * 
     * @return a long
     */
    public final long getTimestamp() {
        return this.timestamp;
    }

    /**
     * 
     * TODO Write javadoc for this method.
     * 
     * @return an int
     */
    public final int getFoil() {
        final String foil = this.getCharacteristics().getSVar("Foil");
        if (!foil.isEmpty()) {
            return Integer.parseInt(foil);
        }
        return 0;
    }

    /**
     * 
     * TODO Write javadoc for this method.
     * 
     * @param f
     *            an int
     */
    public final void setFoil(final int f) {
        this.getCharacteristics().setSVar("Foil", Integer.toString(f));
    }

    /**
     * Adds the haunted by.
     * 
     * @param c
     *            the c
     */
    public final void addHauntedBy(final Card c) {
        this.hauntedBy.add(c);
        if (c != null) {
            c.setHaunting(this);
        }
    }

    /**
     * Gets the haunted by.
     * 
     * @return the haunted by
     */
    public final List<Card> getHauntedBy() {
        return this.hauntedBy;
    }

    /**
     * Removes the haunted by.
     * 
     * @param c
     *            the c
     */
    public final void removeHauntedBy(final Card c) {
        this.hauntedBy.remove(c);
    }

    /**
     * Gets the pairing.
     * 
     * @return the pairedWith
     */
    public final Card getPairedWith() {
        return this.pairedWith;
    }

    /**
     * Sets the pairing.
     * 
     * @param c
     *            the new pairedWith
     */
    public final void setPairedWith(final Card c) {
        this.pairedWith = c;
    }

    /**
     * <p>
     * isPaired.
     * </p>
     * 
     * @return a boolean.
     */
    public final boolean isPaired() {
        return this.pairedWith != null;
    }

    /**
     * Gets the haunting.
     * 
     * @return the haunting
     */
    public final Card getHaunting() {
        return this.haunting;
    }

    /**
     * Sets the haunting.
     * 
     * @param c
     *            the new haunting
     */
    public final void setHaunting(final Card c) {
        this.haunting = c;
    }

    /**
     * Gets the damage done this turn.
     * 
     * @return the damage done this turn
     */
    public final int getDamageDoneThisTurn() {
        int sum = 0;
        for (final Card c : this.dealtDamageToThisTurn.keySet()) {
            sum += this.dealtDamageToThisTurn.get(c);
        }

        return sum;
    }

    /**
     * Gets the damage done to a player by card this turn.
     * 
     * @param player
     *            the player name
     * @return the damage done to player p this turn
     */
    public final int getDamageDoneToPlayerBy(final String player) {
        int sum = 0;
        for (final String p : this.dealtDamageToPlayerThisTurn.keySet()) {
            if (p.equals(player)) {
                sum += this.dealtDamageToPlayerThisTurn.get(p);
            }
        }

        return sum;
    }

    /**
     * Gets the total damage done by card this turn (after prevention and redirects).
     * 
     * @return the damage done to player p this turn
     */
    public final int getTotalDamageDoneBy() {
        int sum = 0;
        for (final Card c : this.dealtDamageToThisTurn.keySet()) {
            sum += this.dealtDamageToThisTurn.get(c);
        }
        for (final String p : this.dealtDamageToPlayerThisTurn.keySet()) {
            sum += this.dealtDamageToPlayerThisTurn.get(p);
        }

        return sum;
    }


    /*
     * (non-Javadoc)
     * 
     * @see forge.GameEntity#hasProtectionFrom(forge.Card)
     */
    @Override
    public boolean hasProtectionFrom(final Card source) {
        if (source == null) {
            return false;
        }

        if (this.isImmutable()) {
            return true;
        }

        if (this.getKeyword() != null) {
            final List<String> list = this.getKeyword();

            String kw = "";
            for (int i = 0; i < list.size(); i++) {
                kw = list.get(i);
                if (!kw.startsWith("Protection")) {
                    continue;
                }
                if (kw.equals("Protection from white")) {
                    if (source.isWhite() && !source.getName().equals("White Ward") 
                            && !source.getName().contains("Pledge of Loyalty")) {
                        return true;
                    }
                } else if (kw.equals("Protection from blue")) {
                    if (source.isBlue() && !source.getName().equals("Blue Ward") 
                            && !source.getName().contains("Pledge of Loyalty")) {
                        return true;
                    }
                } else if (kw.equals("Protection from black")) {
                    if (source.isBlack() && !source.getName().equals("Black Ward") 
                            && !source.getName().contains("Pledge of Loyalty")) {
                        return true;
                    }
                } else if (kw.equals("Protection from red")) {
                    if (source.isRed() && !source.getName().equals("Red Ward") 
                            && !source.getName().contains("Pledge of Loyalty")) {
                        return true;
                    }
                } else if (kw.equals("Protection from green")) {
                    if (source.isGreen() && !source.getName().equals("Green Ward") 
                            && !source.getName().contains("Pledge of Loyalty")) {
                        return true;
                    }
                } else if (kw.equals("Protection from creatures")) {
                    if (source.isCreature()) {
                        return true;
                    }
                } else if (kw.equals("Protection from artifacts")) {
                    if (source.isArtifact()) {
                        return true;
                    }
                } else if (kw.equals("Protection from enchantments")) {
                    if (source.isEnchantment() && !source.getName().contains("Tattoo Ward")) {
                        return true;
                    }
                } else if (kw.equals("Protection from everything")) {
                    return true;
                }

                if (kw.equals("Protection from colored spells")
                        && (source.isInstant() || source.isSorcery()
                                || (source.isAura() && !source.isInZone(ZoneType.Battlefield)))
                        && !source.isColorless()) {
                    return true;
                }

                if (kw.equals("Protection from Dragons") && source.isType("Dragon")) {
                    return true;
                }
                if (kw.equals("Protection from Demons") && source.isType("Demon")) {
                    return true;
                }
                if (kw.equals("Protection from Goblins") && source.isType("Goblin")) {
                    return true;
                }
                if (kw.equals("Protection from Clerics") && source.isType("Cleric")) {
                    return true;
                }
                if (kw.equals("Protection from Gorgons") && source.isType("Gorgon")) {
                    return true;
                }

                if (kw.startsWith("Protection:")) { // uses isValid
                    final String characteristic = kw.split(":")[1];
                    final String[] characteristics = characteristic.split(",");
                    if (source.isValid(characteristics, this.getController(), this)
                      && !source.getName().contains("Flickering Ward") && !source.getName().contains("Pentarch Ward")
                      && !source.getName().contains("Cho-Manno's Blessing") && !source.getName().contains("Floating Shield")
                      && !source.getName().contains("Ward of Lights")) {
                        return true;
                    }
                }

            }
        }
        return false;
    }

    /**
     * 
     * is In Zone.
     * 
     * @param zone
     *            Constant.Zone
     * @return boolean
     */
    public boolean isInZone(final ZoneType zone) {
        return getGame().isCardInZone(this, zone);
    }

    public final boolean canBeDestroyed() { 
        return isInPlay() && (!hasKeyword("Indestructible") || (isCreature() && getNetDefense() <= 0));
    }

    /**
     * Can target.
     * 
     * @param sa
     *            the sa
     * @return a boolean
     */
    @Override
    public final boolean canBeTargetedBy(final SpellAbility sa) {

        if (sa == null) {
            return true;
        }

        // CantTarget static abilities
        for (final Card ca : getGame().getCardsIn(ZoneType.listValueOf("Battlefield,Command"))) {
            final ArrayList<StaticAbility> staticAbilities = ca.getStaticAbilities();
            for (final StaticAbility stAb : staticAbilities) {
                if (stAb.applyAbility("CantTarget", this, sa)) {
                    return false;
                }
            }
        }

        // keywords don't work outside battlefield
        if (!this.isInZone(ZoneType.Battlefield)) {
            return true;
        }

        if (this.hasProtectionFrom(sa.getSourceCard())) {
            return false;
        }
        
        if (this.isPhasedOut()) {
            return false;
        }

        if (this.getKeyword() != null) {
            final Card source = sa.getSourceCard();

            for (String kw : this.getKeyword()) {
                if (kw.equals("Shroud")) {
                    return false;
                }

                if (kw.equals("Hexproof")) {
                    if (sa.getActivatingPlayer().getOpponents().contains(this.getController())) {
                        if (!sa.getActivatingPlayer().getKeywords().contains("Spells and abilities you control can target hexproof creatures")) {
                            return false;
                        }
                    }
                }

                if (kw.equals("CARDNAME can't be the target of Aura spells.")) {
                    if (source.isAura() && sa.isSpell()) {
                        return false;
                    }
                }

                if (kw.equals("CARDNAME can't be enchanted.")) {
                    if (source.isAura()) {
                        return false;
                    }
                } //Sets source as invalid enchant target for computer player only.

                if (kw.equals("CARDNAME can't be equipped.")) {
                    if (source.isEquipment()) {
                        return false;
                    }
                } //Sets source as invalid equip target for computer player only.

                if (kw.equals("CARDNAME can't be the target of red spells or abilities from red sources.")) {
                    if (source.isRed()) {
                        return false;
                    }
                }

                if (kw.equals("CARDNAME can't be the target of black spells.")) {
                    if (source.isBlack() && sa.isSpell()) {
                        return false;
                    }
                }

                if (kw.equals("CARDNAME can't be the target of blue spells.")) {
                    if (source.isBlue() && sa.isSpell()) {
                        return false;
                    }
                }

                if (kw.equals("CARDNAME can't be the target of spells.")) {
                    if (sa.isSpell()) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    /**
     * canBeEnchantedBy.
     * 
     * @param aura
     *            a Card
     * @return a boolean
     */
    public final boolean canBeEnchantedBy(final Card aura) {
        final SpellAbility sa = aura.getFirstSpellAbility();
        Target tgt = null;
        if (sa != null) {
            tgt = sa.getTarget();
        }

        if (this.hasProtectionFrom(aura)
            || (this.hasKeyword("CARDNAME can't be enchanted.") && !aura.getName().equals("Anti-Magic Aura")
                    && !(aura.getName().equals("Consecrate Land") && aura.isInZone(ZoneType.Battlefield)))
            || ((tgt != null) && !this.isValid(tgt.getValidTgts(), aura.getController(), aura))) {
            return false;
        }
        return true;
    }
    
    /**
     * canBeEquippedBy.
     * 
     * @param equip
     *            a Card
     * @return a boolean
     */
    public final boolean canBeEquippedBy(final Card equip) {
        if (equip.hasStartOfKeyword("CantEquip")) {
            final int keywordPosition = equip.getKeywordPosition("CantEquip");
            final String parse = equip.getKeyword().get(keywordPosition).toString();
            final String[] k = parse.split(" ", 2);
            final String[] restrictions = k[1].split(",");
            if (this.isValid(restrictions, equip.getController(), equip)) {
                return false;
            }
        }
        if (this.hasProtectionFrom(equip)
            || this.hasKeyword("CARDNAME can't be equipped.")
            || !this.isValid("Creature", equip.getController(), equip)) {
            return false;
        }
        return true;
    }

    /**
     * Gets the replacement effects.
     * 
     * @return the replacement effects
     */
    public List<ReplacementEffect> getReplacementEffects() {
        return this.getCharacteristics().getReplacementEffects();
    }

    /**
     * Sets the replacement effects.
     * 
     * @param res
     *            the new replacement effects
     */
    public void setReplacementEffects(final List<ReplacementEffect> res) {
        this.getCharacteristics().getReplacementEffects().clear();
        for (final ReplacementEffect replacementEffect : res) {
            this.addReplacementEffect(replacementEffect);
        }
    }

    /**
     * Adds the replacement effect.
     * 
     * @param replacementEffect
     *            the rE
     */
    public void addReplacementEffect(final ReplacementEffect replacementEffect) {
        final ReplacementEffect replacementEffectCopy = replacementEffect.getCopy(); // doubtful - every caller provides a newly parsed instance, why copy? 
        replacementEffectCopy.setHostCard(this);
        this.getCharacteristics().getReplacementEffects().add(replacementEffectCopy);
    }

    /**
     * Returns what zone this card was cast from (from what zone it was moved to
     * the stack).
     * 
     * @return the castFrom
     */
    public ZoneType getCastFrom() {
        return this.castFrom;
    }

    /**
     * @param castFrom0
     *            the castFrom to set
     */
    public void setCastFrom(final ZoneType castFrom0) {
        this.castFrom = castFrom0;
    }

    /**
     * @return CardDamageHistory
     */
    public CardDamageHistory getDamageHistory() {
        return damageHistory;
    }

    /**
     * @return the effectSource
     */
    public Card getEffectSource() {
        return effectSource;
    }

    /**
     * @param src the effectSource to set
     */
    public void setEffectSource(Card src) {
        this.effectSource = src;
    }

    /**
     * @return the startsGameInPlay
     */
    public boolean isStartsGameInPlay() {
        return startsGameInPlay;
    }

    /**
     * @param startsGameInPlay the startsGameInPlay to set
     */
    public void setStartsGameInPlay(boolean startsGameInPlay) {
        this.startsGameInPlay = startsGameInPlay;
    }

    /** @return boolean */
    public boolean isInPlay() {
        if (getController() == null) {
            return false;
        }
        return getController().getZone(ZoneType.Battlefield).contains(this);
    }

    public void onCleanupPhase(final Player turn) {
        setDamage(0);
        resetPreventNextDamage();
        resetReceivedDamageFromThisTurn();
        resetDealtDamageToThisTurn();
        resetDealtDamageToPlayerThisTurn();
        getDamageHistory().newTurn();
        setRegeneratedThisTurn(0);
        clearMustBlockCards();
        getDamageHistory().setCreatureAttackedLastTurnOf(turn, getDamageHistory().getCreatureAttackedThisTurn());
        getDamageHistory().setCreatureAttackedThisTurn(false);
        getDamageHistory().setCreatureAttacksThisTurn(0);
        getDamageHistory().setCreatureBlockedThisTurn(false);
        getDamageHistory().setCreatureGotBlockedThisTurn(false);
        clearBlockedByThisTurn();
        clearBlockedThisTurn();
    }

    /**
     * <p>
     * hasETBTrigger.
     * </p>
     * 
     * @param card
     *            a {@link forge.Card} object.
     * @return a boolean.
     */
    public boolean hasETBTrigger() {
        for (final Trigger tr : this.getTriggers()) {
            final HashMap<String, String> params = tr.getMapParams();
            if (tr.getMode() != TriggerType.ChangesZone) {
                continue;
            }

            if (!params.get("Destination").equals(ZoneType.Battlefield.toString())) {
                continue;
            }

            if (params.containsKey("ValidCard") && !params.get("ValidCard").contains("Self")) {
                continue;
            }
            return true;
        }
        return false;
    }

    /**
     * <p>
     * hasETBTrigger.
     * </p>
     * 
     * @return a boolean.
     */
    public boolean hasETBReplacement() {
        for (final ReplacementEffect re : getReplacementEffects()) {
            final Map<String, String> params = re.getMapParams();
            if (!(re instanceof ReplaceMoved)) {
                continue;
            }

            if (!params.get("Destination").equals(ZoneType.Battlefield.toString())) {
                continue;
            }

            if (params.containsKey("ValidCard") && !params.get("ValidCard").contains("Self")) {
                continue;
            }
            return true;
        }
        return false;
    }

    public boolean canBeShownTo(final Player viewer) {
        if (!isFaceDown()) {
            return true;
        }
        if (getController() == viewer && isInZone(ZoneType.Battlefield)) {
            return true;
        }
        final Game game = this.getGame();
        if (getController() == viewer && hasKeyword("You may look at this card.")) {
            return true; 
        }
        if (getController().isOpponentOf(viewer) && hasKeyword("Your opponent may look at this card.")) {
            return true; 
        }
        for (Card host : game.getCardsIn(ZoneType.Battlefield)) {
            final ArrayList<StaticAbility> staticAbilities = host.getStaticAbilities();
            for (final StaticAbility stAb : staticAbilities) {
                if (stAb.applyAbility("MayLookAt", this, viewer)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * <p>
     * getConvertedManaCost.
     * </p>
     * 
     * @return a int.
     */
    public int getCMC() {
        return getCMC(SplitCMCMode.CurrentSideCMC);
    }

    public int getCMC(SplitCMCMode mode) {
        if (isToken() && !isCopiedToken()) {
            return 0;
        }

        int xPaid = 0;

        // 2012-07-22 - If a card is on the stack, count the xManaCost in with it's CMC
        if (getGame().getCardsIn(ZoneType.Stack).contains(this) && getManaCost() != null) {
            xPaid = getXManaCostPaid() * getManaCost().countX();
        }
        
        int requestedCMC = 0;

        if (isSplitCard()) {
            switch(mode) {
                case CurrentSideCMC:
                    // TODO: test if this returns combined CMC for the full face (then get rid of CombinedCMC mode?)
                    requestedCMC = getManaCost().getCMC() + xPaid; 
                    break;
                case LeftSplitCMC:
                    requestedCMC = getState(CardCharacteristicName.LeftSplit).getManaCost().getCMC() + xPaid;
                    break;
                case RightSplitCMC:
                    requestedCMC = getState(CardCharacteristicName.RightSplit).getManaCost().getCMC() + xPaid;
                    break;
                case CombinedCMC:
                    requestedCMC += getState(CardCharacteristicName.LeftSplit).getManaCost().getCMC(); 
                    requestedCMC += getState(CardCharacteristicName.RightSplit).getManaCost().getCMC();
                    requestedCMC += xPaid;
                    break;
                default:
                    System.out.println(String.format("Illegal Split Card CMC mode %s passed to getCMC!", mode.toString()));
                    break;
            }
        } else {
            requestedCMC = getManaCost().getCMC() + xPaid;
        }

        return requestedCMC;
    }

    public final boolean canBeSacrificedBy(final SpellAbility source)
    {
        if (isImmutable()) {
            System.out.println("Trying to sacrifice immutables: " + this);
            return false;
        }
        if (source != null && getController().isOpponentOf(source.getActivatingPlayer())
                && getController().hasKeyword("Spells and abilities your opponents control can't cause you to sacrifice permanents.")) {
            return false;
        }
        return true;
    }

    CardRules cardRules;
    public CardRules getRules() {
        return cardRules;
    }
    public void setRules(CardRules r) {
        cardRules = r;
    }

    public boolean isCommander() {
        // TODO - have a field
        return this.isCommander;
    }
    
    public void setCommander(boolean b) {
        this.isCommander = b;
    }
    
    public void setSplitStateToPlayAbility(SpellAbility sa) {
        if( !isSplitCard() ) return; // just in case
        // Split card support
        for (SpellAbility a : getState(CardCharacteristicName.LeftSplit).getSpellAbility()) {
            if (sa == a || sa.getDescription().equals(String.format("%s (without paying its mana cost)", a.getDescription()))) {
                setState(CardCharacteristicName.LeftSplit);
                return;
            }
        }
        for (SpellAbility a : getState(CardCharacteristicName.RightSplit).getSpellAbility()) {
            if (sa == a || sa.getDescription().equals(String.format("%s (without paying its mana cost)", a.getDescription()))) {
                setState(CardCharacteristicName.RightSplit);
                return;
            }
        }
        if ( sa.getSourceCard().hasKeyword("Fuse") ) // it's ok that such card won't change its side
            return;

        throw new RuntimeException("Not found which part to choose for ability " + sa + " from card " + this);
    }

    // Optional costs paid
    private final EnumSet<OptionalCost> costsPaid = EnumSet.noneOf(OptionalCost.class);
    public void clearOptionalCostsPaid() { costsPaid.clear(); }
    public void addOptionalCostPaid(OptionalCost cost) { costsPaid.add(cost); }
    public Iterable<OptionalCost> getOptionalCostsPaid() { return costsPaid; }
    public boolean isOptionalCostPaid(OptionalCost cost) { return costsPaid.contains(cost); }

    /**
     * Fetch GameState for this card from references to players who may own or control this card. 
     */
    @Override
    public Game getGame() {
        
        Player controller = getController();
        if (null != controller)
            return controller.getGame();
        
        Player owner = getOwner();
        if (null != owner)
            return owner.getGame();
        
        throw new IllegalStateException("Card " + toString() + " has no means to determine the game it belongs to!");
    }

} // end Card class
