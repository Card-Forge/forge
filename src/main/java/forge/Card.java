package forge;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.TreeMap;

import com.esotericsoftware.minlog.Log;

import forge.Constant.Zone;
import forge.card.cardFactory.CardFactoryUtil;
import forge.card.cost.Cost;
import forge.card.mana.ManaCost;
import forge.card.spellability.Ability_Mana;
import forge.card.spellability.Ability_Triggered;
import forge.card.spellability.SpellAbility;
import forge.card.spellability.Spell_Permanent;
import forge.card.staticAbility.StaticAbility;
import forge.card.trigger.Trigger;
import forge.item.CardDb;

/**
 * <p>Card class.</p>
 *
 * Can now be used as keys in Tree data structures.  The comparison is based
 * entirely on getUniqueNumber().
 *
 * @author Forge
 * @version $Id$
 */
public class Card extends GameEntity implements Comparable<Card> {
    private static int nextUniqueNumber = 1;
    private int uniqueNumber = nextUniqueNumber++;

    private long value;

    private Map<String, Object> triggeringObjects = new TreeMap<String, Object>();
    private ArrayList<Trigger> triggers = new ArrayList<Trigger>();
    private ArrayList<String> intrinsicAbility = new ArrayList<String>();
    private ArrayList<String> staticAbilityStrings = new ArrayList<String>();
    private ArrayList<String> intrinsicKeyword = new ArrayList<String>();
    private ArrayList<String> extrinsicKeyword = new ArrayList<String>();
    //Hidden keywords won't be displayed on the card
    private ArrayList<String> hiddenExtrinsicKeyword = new ArrayList<String>();
    private ArrayList<String> prevIntrinsicKeyword = new ArrayList<String>();
    private ArrayList<Card> attachedByMindsDesire = new ArrayList<Card>();
    //which equipment cards are equipping this card?
    private ArrayList<Card> equippedBy = new ArrayList<Card>();
    //equipping size will always be 0 or 1
    //if this card is of the type equipment, what card is it currently equipping?
    private ArrayList<Card> equipping = new ArrayList<Card>();
    //which auras enchanted this card?

    //if this card is an Aura, what Entity is it enchanting?
    private GameEntity enchanting = null;
    private ArrayList<String> type = new ArrayList<String>();
    private ArrayList<String> prevType = new ArrayList<String>();
    private ArrayList<String> choicesMade = new ArrayList<String>();
    private ArrayList<String> targetsForChoices = new ArrayList<String>();
    private ArrayList<SpellAbility> spellAbility = new ArrayList<SpellAbility>();
    private ArrayList<Ability_Mana> manaAbility = new ArrayList<Ability_Mana>();
    private ArrayList<Card_Color> cardColor = new ArrayList<Card_Color>();
    //changes by AF animate and continuous static effects
    private ArrayList<Card_Type> changedCardTypes = new ArrayList<Card_Type>();
    private ArrayList<Card_Keywords> changedCardKeywords = new ArrayList<Card_Keywords>();
    private ArrayList<StaticAbility> staticAbilities = new ArrayList<StaticAbility>();

    private ArrayList<Object> rememberedObjects = new ArrayList<Object>();
    private ArrayList<Card> imprintedCards = new ArrayList<Card>();
    private Card championedCard = null;
    private CardList devouredCards = new CardList();

    private Map<Card, Integer> receivedDamageFromThisTurn = new TreeMap<Card, Integer>();
    private Map<Card, Integer> dealtDamageToThisTurn = new TreeMap<Card, Integer>();
    private Map<Card, Integer> assignedDamageMap = new TreeMap<Card, Integer>();

    private boolean unCastable;
    private boolean drawnThisTurn = false;
    private boolean tapped;
    private boolean sickness = true;                              //summoning sickness
    private boolean token = false;
    private boolean copiedToken = false;
    private boolean copiedSpell = false;
    private boolean spellWithChoices = false;
    private boolean spellCopyingCard = false;
    private boolean creatureAttackedThisTurn = false;
    private boolean creatureAttackedThisCombat = false;
    private boolean creatureBlockedThisCombat = false;
    private boolean creatureGotBlockedThisCombat = false;
    private boolean dealtDmgToHumanThisTurn = false;
    private boolean dealtDmgToComputerThisTurn = false;
    private boolean sirenAttackOrDestroy = false;
    private ArrayList<Card> mustBlockCards = new ArrayList<Card>();

    private boolean canMorph = false;
    private boolean faceDown = false;
    private boolean kicked = false;
    private boolean evoked = false;

    private boolean levelUp = false;
    private boolean bounceAtUntap = false;
    private boolean finishedEnteringBF = false;

    private boolean flashback = false;
    private boolean unearth = false;
    private boolean unearthed;

    private boolean madness = false;
    private boolean suspendCast = false;
    private boolean suspend = false;
    
    private boolean phasedOut = false;
    private boolean directlyPhasedOut = true;

    //for Vanguard / Manapool / Emblems etc.
    private boolean isImmutable = false;

    private long timestamp = -1; // permanents on the battlefield

    private int baseAttack = 0;
    private int baseDefense = 0;
    private ArrayList<CardPowerToughness> newPT = new ArrayList<CardPowerToughness>(); // stack of set power/toughness
    private int baseLoyalty = 0;
    private String baseAttackString = null;
    private String baseDefenseString = null;

    private int damage;

    // regeneration
    private int nShield;

    private int turnInZone;

    private int tempAttackBoost = 0;
    private int tempDefenseBoost = 0;

    private int semiPermanentAttackBoost = 0;
    private int semiPermanentDefenseBoost = 0;

    private int randomPicture = 0;

    private int xManaCostPaid = 0;

    private int xLifePaid = 0;

    private int multiKickerMagnitude = 0;
    private int replicateMagnitude = 0;

    private int sunburstValue = 0;
    private String colorsPaid = "";

    private Player owner = null;
    private ArrayList<Object> controllerObjects = new ArrayList<Object>();
    private String imageName = "";
    //private String rarity = "";
    private String text = "";
    private String manaCost = "";
    private String echoCost = "";
    private String madnessCost = "";
    private String chosenType = "";
    //private String chosenColor = "";
    private ArrayList<String> chosenColor = new ArrayList<String>();
    private String namedCard = "";
    private int chosenNumber;
    private Player chosenPlayer;

    private Card cloneOrigin = null;
    private ArrayList<Card> clones = new ArrayList<Card>();
    private Card currentlyCloningCard = null;
    private Command cloneLeavesPlayCommand = null;
    private ArrayList<Card> gainControlTargets = new ArrayList<Card>();
    private ArrayList<Command> gainControlReleaseCommands = new ArrayList<Command>();

    private ArrayList<Ability_Triggered> zcTriggers = new ArrayList<Ability_Triggered>();
    private ArrayList<Command> turnFaceUpCommandList = new ArrayList<Command>();
    private ArrayList<Command> equipCommandList = new ArrayList<Command>();
    private ArrayList<Command> unEquipCommandList = new ArrayList<Command>();
    private ArrayList<Command> enchantCommandList = new ArrayList<Command>();
    private ArrayList<Command> unEnchantCommandList = new ArrayList<Command>();
    private ArrayList<Command> untapCommandList = new ArrayList<Command>();
    private ArrayList<Command> changeControllerCommandList = new ArrayList<Command>();
    private ArrayList<Command> replaceMoveToGraveyardCommandList = new ArrayList<Command>();
    private ArrayList<Command> cycleCommandList = new ArrayList<Command>();

    private Map<Counters, Integer> counters = new TreeMap<Counters, Integer>();
    private Map<String, String> sVars = new TreeMap<String, String>();
    private static String[] storableSVars = {"ChosenX"};
    
    private ArrayList<Card> hauntedBy = new ArrayList<Card>();
    private Card haunting = null;

    /**
     * 
     * TODO Write javadoc for this method.
     * @return a String array
     */
    public static String[] getStorableSVars() { return storableSVars; }

    //hacky code below, used to limit the number of times an ability
    //can be used per turn like Vampire Bats
    //should be put in SpellAbility, but it is put here for convienance
    //this is make public just to make things easy
    //this code presumes that each card only has one ability that can be
    //used a limited number of times per turn
    //CardFactory.SSP_canPlay(Card) uses these variables

    //Only used with Replicate
    private int abilityUsed;

    /**
     * 
     * TODO Write javadoc for this method.
     */
    public static void resetUniqueNumber() {
        nextUniqueNumber = 1;
    }

    /**
     * 
     * TODO Write javadoc for this method.
     * @param c a Card object
     */
    public final void addDevoured(final Card c) {
        devouredCards.add(c);
    }

    /**
     * 
     * TODO Write javadoc for this method.
     */
    public final void clearDevoured() {
        devouredCards.clear();
    }

    /**
     * 
     * TODO Write javadoc for this method.
     * @return a CardList object
     */
    public final CardList getDevoured() {
        return devouredCards;
    }

    /**
     * <p>addRemembered.</p>
     *
     * @param o a {@link java.lang.Object} object.
     */
    public final void addRemembered(final Object o) {
        rememberedObjects.add(o);
    }

    /**
     * <p>getRemembered.</p>
     *
     * @return a {@link java.util.ArrayList} object.
     */
    public final ArrayList<Object> getRemembered() {
        return rememberedObjects;
    }

    /**
     * <p>clearRemembered.</p>
     */
    public final void clearRemembered() {
        rememberedObjects.clear();
    }

    /**
     * <p>addImprinted.</p>
     *
     * @param c a {@link forge.Card} object.
     */
    public final void addImprinted(final Card c) {
        imprintedCards.add(c);
    }

    /**
     * <p>addImprinted.</p>
     *
     * @param list a {@link java.util.ArrayList} object.
     */
    public final void addImprinted(final ArrayList<Card> list) {
        imprintedCards.addAll(list);
    }

    /**
     * <p>getImprinted.</p>
     *
     * @return a {@link java.util.ArrayList} object.
     */
    public final ArrayList<Card> getImprinted() {
        return imprintedCards;
    }

    /**
     * <p>clearImprinted.</p>
     */
    public final void clearImprinted() {
        imprintedCards.clear();
    }

    /**
     * <p>Setter for the field <code>championedCard</code>.</p>
     *
     * @param c a {@link forge.Card} object.
     * @since 1.0.15
     */
    public final void setChampionedCard(final Card c) {
        championedCard = c;
    }

    /**
     * <p>Getter for the field <code>championedCard</code>.</p>
     *
     * @return a {@link forge.Card} object.
     * @since 1.0.15
     */
    public final Card getChampionedCard() {
        return championedCard;
    }

    /**
     * <p>addTrigger.</p>
     *
     * @param t a {@link forge.card.trigger.Trigger} object.
     * @return a {@link forge.card.trigger.Trigger} object.
     */
    public final Trigger addTrigger(final Trigger t) {
        Trigger newtrig = t.getCopy();
        newtrig.setHostCard(this);
        triggers.add(newtrig);
        return newtrig;
    }
    
    public final void moveTrigger(final Trigger t) {
        t.setHostCard(this);
        if(!triggers.contains(t))
            triggers.add(t);
    }

    /**
     * <p>removeTrigger.</p>
     *
     * @param t a {@link forge.card.trigger.Trigger} object.
     */
    public final void removeTrigger(final Trigger t) {
        triggers.remove(t);
    }

    /**
     * <p>Getter for the field <code>triggers</code>.</p>
     *
     * @return a {@link java.util.ArrayList} object.
     */
    public final ArrayList<Trigger> getTriggers() {
        return triggers;
    }

    /**
     * <p>getNamedTrigger.</p>
     *
     * @param name a {@link java.lang.String} object.
     * @return a {@link forge.card.trigger.Trigger} object.
     */
    public final Trigger getNamedTrigger(final String name) {
        for (Trigger t : triggers) {
            if (t.getName() != null && t.getName().equals(name)) {
                    return t;
            }
        }

        return null;
    }

    /**
     * <p>Setter for the field <code>triggers</code>.</p>
     *
     * @param trigs a {@link java.util.ArrayList} object.
     */
    public final void setTriggers(final ArrayList<Trigger> trigs) {
        for (Trigger t : trigs) {
            Trigger newtrig = t.getCopy();
            newtrig.setHostCard(this);
            triggers.add(newtrig);
        }
    }

    /**
     * <p>clearTriggersNew.</p>
     */
    public final void clearTriggersNew() {
        triggers.clear();
    }

    /**
     * <p>getTriggeringObject.</p>
     *
     * @param typeIn a {@link java.lang.String} object.
     * @return a {@link java.lang.Object} object.
     */
    public final Object getTriggeringObject(final String typeIn) {
        return triggeringObjects.get(typeIn);
    }

    /**
    field <code>abilityUsed</code>.
    *
    * @param i a int.
    */
    public final void setAbilityUsed(final int i) {
        abilityUsed = i;
    }

    /**
     * <p>Getter for the field <code>abilityUsed</code>.</p>
     *
     * @return a int.
     */
    public final int getAbilityUsed() {
        return abilityUsed;
    }

    /**
     * <p>Getter for the field <code>sunburstValue</code>.</p>
     *
     * @return a int.
     */
    public final int getSunburstValue() {
        return sunburstValue;
    }

    /**
     * <p>Setter for the field <code>colorsPaid</code>.</p>
     *
     * @param s a String
     */
    public final void setColorsPaid(final String s) {
        colorsPaid = s;
    }

    /**
     * <p>Getter for the field <code>colorsPaid</code>.</p>
     *
     * @return a String.
     */
    public final String getColorsPaid() {
        return colorsPaid;
    }

    /**
     * <p>Setter for the field <code>sunburstValue</code>.</p>
     *
     * @param valueIn a int.
     */
    public final void setSunburstValue(final int valueIn) {
        sunburstValue = valueIn;
    }

    /**
     * <p>addXManaCostPaid.</p>
     *
     * @param n a int.
     */
    public final void addXManaCostPaid(final int n) {
        xManaCostPaid += n;
    }

    /**
     * <p>Setter for the field <code>xManaCostPaid</code>.</p>
     *
     * @param n a int.
     */
    public final void setXManaCostPaid(final int n) {
        xManaCostPaid = n;
    }

    /**
     * <p>Getter for the field <code>xManaCostPaid</code>.</p>
     *
     * @return a int.
     */
    public final int getXManaCostPaid() {
        return xManaCostPaid;
    }

    /**
     * <p>Setter for the field <code>xLifePaid</code>.</p>
     *
     * @param n a int.
     */
    public final void setXLifePaid(final int n) {
        xLifePaid = n;
    }

    /**
     * <p>Getter for the field <code>xLifePaid</code>.</p>
     *
     * @return a int.
     */
    public final int getXLifePaid() {
        return xLifePaid;
    }

    //used to see if an attacking creature with a triggering attack ability triggered this phase:
    /**
     * <p>Setter for the field <code>creatureAttackedThisCombat</code>.</p>
     *
     * @param b a boolean.
     */
    public final void setCreatureAttackedThisCombat(final boolean b) {
        creatureAttackedThisCombat = b;
        if (true == b) {
            setCreatureAttackedThisTurn(true);
        }
    }

    /**
     * <p>Getter for the field <code>creatureAttackedThisCombat</code>.</p>
     *
     * @return a boolean.
     */
    public final boolean getCreatureAttackedThisCombat() {
        return creatureAttackedThisCombat;
    }

    /**
     * <p>Setter for the field <code>creatureAttackedThisTurn</code>.</p>
     *
     * @param b a boolean.
     */
    public final void setCreatureAttackedThisTurn(final boolean b) {
        creatureAttackedThisTurn = b;
    }

    /**
     * <p>Getter for the field <code>creatureAttackedThisTurn</code>.</p>
     *
     * @return a boolean.
     */
    public final boolean getCreatureAttackedThisTurn() {
        return creatureAttackedThisTurn;
    }

    /**
     * <p>Setter for the field <code>creatureBlockedThisCombat</code>.</p>
     *
     * @param b a boolean.
     */
    public final void setCreatureBlockedThisCombat(final boolean b) {
        creatureBlockedThisCombat = b;
    }

    /**
     * <p>Getter for the field <code>creatureBlockedThisCombat</code>.</p>
     *
     * @return a boolean.
     */
    public final boolean getCreatureBlockedThisCombat() {
        return creatureBlockedThisCombat;
    }

    /**
     * <p>Setter for the field <code>creatureGotBlockedThisCombat</code>.</p>
     *
     * @param b a boolean.
     */
    public final void setCreatureGotBlockedThisCombat(final boolean b) {
        creatureGotBlockedThisCombat = b;
    }

    /**
     * <p>Getter for the field <code>creatureGotBlockedThisCombat</code>.</p>
     *
     * @return a boolean.
     */
    public final boolean getCreatureGotBlockedThisCombat() {
        return creatureGotBlockedThisCombat;
    }

    /**
     * <p>canAnyPlayerActivate.</p>
     *
     * @return a boolean.
     */
    public final boolean canAnyPlayerActivate() {
        for (SpellAbility s : spellAbility) {
            if (s.getRestrictions().getAnyPlayer()) {
                return true;
            }
        }
        return false;
    }

    /**
     * <p>Setter for the field <code>dealtDmgToHumanThisTurn</code>.</p>
     *
     * @param b a boolean.
     */
    public final void setDealtDmgToHumanThisTurn(final boolean b) {
        dealtDmgToHumanThisTurn = b;
    }

    /**
     * <p>Getter for the field <code>dealtDmgToHumanThisTurn</code>.</p>
     *
     * @return a boolean.
     */
    public final boolean getDealtDmgToHumanThisTurn() {
        return dealtDmgToHumanThisTurn;
    }

    /**
     * <p>Setter for the field <code>dealtDmgToComputerThisTurn</code>.</p>
     *
     * @param b a boolean.
     */
    public final void setDealtDmgToComputerThisTurn(final boolean b) {
        dealtDmgToComputerThisTurn = b;
    }

    /**
     * <p>Getter for the field <code>dealtDmgToComputerThisTurn</code>.</p>
     *
     * @return a boolean.
     */
    public final boolean getDealtDmgToComputerThisTurn() {
        return dealtDmgToComputerThisTurn;
    }

    /**
     * <p>Setter for the field <code>sirenAttackOrDestroy</code>.</p>
     *
     * @param b a boolean.
     */
    public final void setSirenAttackOrDestroy(final boolean b) {
        sirenAttackOrDestroy = b;
    }

    /**
     * <p>Getter for the field <code>sirenAttackOrDestroy</code>.</p>
     *
     * @return a boolean.
     */
    public final boolean getSirenAttackOrDestroy() {
        return sirenAttackOrDestroy;
    }
    
    /**
     * a Card that this Card must block if able in an upcoming combat.
     * This is cleared at the end of each turn.
     * 
     * @param o Card to block
     * 
     * @since 1.1.6
     */
    public void addMustBlockCard(Card c) {
        mustBlockCards.add(c);
    }
    
    /**
     * get the Card that this Card must block this combat
     * 
     * @return the Cards to block (if able)
     * 
     * @since 1.1.6
     */
    public ArrayList<Card> getMustBlockCards() {
        return mustBlockCards;
    }
    
    /**
     * clear the list of Cards that this Card must block this combat
     * 
     * @since 1.1.6
     */
    public void clearMustBlockCards() {
        mustBlockCards.clear();
    }

    /**
     * <p>Getter for the field <code>clones</code>.</p>
     *
     * @return a {@link java.util.ArrayList} object.
     */
    public final ArrayList<Card> getClones() {
        return clones;
    }

    /**
     * <p>Setter for the field <code>clones</code>.</p>
     *
     * @param c a {@link java.util.ArrayList} object.
     */
    public final void setClones(final ArrayList<Card> c) {
        clones.clear();
        clones.addAll(c);
    }

    /**
     * <p>addClone.</p>
     *
     * @param c a {@link forge.Card} object.
     */
    public final void addClone(final Card c) {
        clones.add(c);
    }

    /**
     * <p>addClones.</p>
     *
     * @param c a {@link java.util.ArrayList} object.
     */
    public final void addClones(final ArrayList<Card> c) {
        clones.addAll(c);
    }

    /**
     * <p>clearClones.</p>
     */
    public final void clearClones() {
        clones.clear();
    }

    /**
     * <p>Getter for the field <code>cloneOrigin</code>.</p>
     *
     * @return a {@link forge.Card} object.
     */
    public final Card getCloneOrigin() {
        return cloneOrigin;
    }

    /**
     * <p>Setter for the field <code>cloneOrigin</code>.</p>
     *
     * @param name a {@link forge.Card} object.
     */
    public final void setCloneOrigin(final Card name) {
        cloneOrigin = name;
    }

    /**
     * <p>Getter for the field <code>cloneLeavesPlayCommand</code>.</p>
     *
     * @return a {@link forge.Command} object.
     */
    public final Command getCloneLeavesPlayCommand() {
        return cloneLeavesPlayCommand;
    }

    /**
     * <p>Setter for the field <code>cloneLeavesPlayCommand</code>.</p>
     *
     * @param com a {@link forge.Command} object.
     */
    public final void setCloneLeavesPlayCommand(final Command com) {
        cloneLeavesPlayCommand = com;
    }

    /**
     * <p>Getter for the field <code>currentlyCloningCard</code>.</p>
     *
     * @return a {@link forge.Card} object.
     */
    public final Card getCurrentlyCloningCard() {
        return currentlyCloningCard;
    }

    /**
     * <p>Setter for the field <code>currentlyCloningCard</code>.</p>
     *
     * @param c a {@link forge.Card} object.
     */
    public final void setCurrentlyCloningCard(final Card c) {
        currentlyCloningCard = c;
    }

    /**
     * <p>Getter for the field <code>sacrificeAtEOT</code>.</p>
     *
     * @return a boolean.
     */
    public final boolean getSacrificeAtEOT() {
        return hasKeyword("At the beginning of the end step, sacrifice CARDNAME.");
    }

    /**
     * <p>Getter for the field <code>bounceAtUntap</code>.</p>
     *
     * @return a boolean.
     */
    public final boolean getBounceAtUntap() {
        return bounceAtUntap;
    }

    /**
     * <p>Setter for the field <code>bounceAtUntap</code>.</p>
     *
     * @param bounce a boolean.
     */
    public final void setBounceAtUntap(final boolean bounce) {
        this.bounceAtUntap = bounce;
    }

    /**
     * <p>Getter for the field <code>finishedEnteringBF</code>.</p>
     *
     * @return a boolean.
     */
    public final boolean getFinishedEnteringBF() {
        return finishedEnteringBF;
    }

    /**
     * <p>Setter for the field <code>finishedEnteringBF</code>.</p>
     *
     * @param b a boolean.
     */
    public final void setFinishedEnteringBF(final boolean b) {
        this.finishedEnteringBF = b;
    }

    /**
     * <p>hasFirstStrike.</p>
     *
     * @return a boolean.
     */
    public final boolean hasFirstStrike() {
        return hasKeyword("First Strike");
    }

    /**
     * <p>hasDoubleStrike.</p>
     *
     * @return a boolean.
     */
    public final boolean hasDoubleStrike() {
        return hasKeyword("Double Strike");
    }

    /**
     * <p>hasSecondStrike.</p>
     *
     * @return a boolean.
     */
    public final boolean hasSecondStrike() {
        return hasDoubleStrike() || !hasFirstStrike();
    }

    //for costs (like Planeswalker abilities) Doubling Season gets ignored.
    /**
     * <p>addCounterFromNonEffect.</p>
     *
     * @param counterName a {@link forge.Counters} object.
     * @param n a int.
     */
    public final void addCounterFromNonEffect(final Counters counterName, final int n) {
        if (this.hasKeyword("CARDNAME can't have counters placed on it.")) {
            return;
        }
        if (this.hasKeyword("CARDNAME can't have -1/-1 counters placed on it.") && counterName.equals(Counters.M1M1)) {
            return;
        }
        if (counters.containsKey(counterName)) {
            Integer aux = counters.get(counterName) + n;
            counters.put(counterName, aux);
        } else {
            counters.put(counterName, Integer.valueOf(n));
        }

        if (counterName.equals(Counters.P1P1) || counterName.equals(Counters.M1M1)) {
            // +1/+1 counters should erase -1/-1 counters
            int plusOneCounters = 0;
            int minusOneCounters = 0;

            Counters p1Counter = Counters.P1P1;
            Counters m1Counter = Counters.M1M1;
            if (counters.containsKey(p1Counter)) {
                plusOneCounters = counters.get(p1Counter);
            }
            if (counters.containsKey(m1Counter)) {
                minusOneCounters = counters.get(m1Counter);
            }

            if (plusOneCounters == minusOneCounters) {
                counters.remove(m1Counter);
                counters.remove(p1Counter);
            }
            if (plusOneCounters > minusOneCounters) {
                counters.remove(m1Counter);
                counters.put(p1Counter, (Integer) (plusOneCounters - minusOneCounters));
            } else {
                counters.put(m1Counter, (Integer) (minusOneCounters - plusOneCounters));
                counters.remove(p1Counter);
            }
        }

        /////////////////
        //
        // Not sure if we want to fire triggers on addCounterFromNonEffect
        // I don't think so since reverting cost payments uses this.

        /*
        //Run triggers
        HashMap<String,Object> runParams = new HashMap<String,Object>();
        runParams.put("Card", this);
        runParams.put("CounterType", counterName);
        AllZone.getTriggerHandler().runTrigger("CounterAdded", runParams);
        */

        this.updateObservers();
    }

    /**
     * <p>addCounter.</p>
     *
     * @param counterName a {@link forge.Counters} object.
     * @param n a int.
     */
    public final void addCounter(final Counters counterName, final int n) {
        if (this.hasKeyword("CARDNAME can't have counters placed on it.")) {
            return;
        }
        if (this.hasKeyword("CARDNAME can't have -1/-1 counters placed on it.") && counterName.equals(Counters.M1M1)) {
            return;
        }
        int multiplier = AllZoneUtil.getDoublingSeasonMagnitude(this.getController());
        if (counters.containsKey(counterName)) {
            Integer aux = counters.get(counterName) + (multiplier * n);
            counters.put(counterName, aux);
        } else {
            counters.put(counterName, Integer.valueOf(multiplier * n));
        }

        //Run triggers
        Map<String, Object> runParams = new TreeMap<String, Object>();
        runParams.put("Card", this);
        runParams.put("CounterType", counterName);
        for (int i = 0; i < (multiplier * n); i++) {
            AllZone.getTriggerHandler().runTrigger("CounterAdded", runParams);
        }

        if (counterName.equals(Counters.P1P1) || counterName.equals(Counters.M1M1)) {
            // +1/+1 counters should erase -1/-1 counters
            int plusOneCounters = 0;
            int minusOneCounters = 0;

            Counters p1Counter = Counters.P1P1;
            Counters m1Counter = Counters.M1M1;
            if (counters.containsKey(p1Counter)) {
                plusOneCounters = counters.get(p1Counter);
            }
            if (counters.containsKey(m1Counter)) {
                minusOneCounters = counters.get(m1Counter);
            }

            if (plusOneCounters == minusOneCounters) {
                counters.remove(m1Counter);
                counters.remove(p1Counter);
            }
            if (plusOneCounters > minusOneCounters) {
                counters.remove(m1Counter);
                counters.put(p1Counter, (Integer) (plusOneCounters - minusOneCounters));
            } else {
                counters.put(m1Counter, (Integer) (minusOneCounters - plusOneCounters));
                counters.remove(p1Counter);
            }
        }

        AllZone.getGameAction().checkStateEffects();

        this.updateObservers();
    }

    /**
     * <p>subtractCounter.</p>
     *
     * @param counterName a {@link forge.Counters} object.
     * @param n a int.
     */
    public final void subtractCounter(final Counters counterName, final int n) {
        if (counters.containsKey(counterName)) {
            Integer aux = counters.get(counterName) - n;
            if (aux < 0) {
                aux = 0;
            }
            counters.put(counterName, aux);
            if (counterName.equals(Counters.TIME) && aux == 0) {
                boolean hasVanish = CardFactoryUtil.hasKeyword(this, "Vanishing") != -1;

                if (hasVanish && AllZoneUtil.isCardInPlay(this)) {
                    AllZone.getGameAction().sacrifice(this);
                }

                if (hasSuspend() && AllZoneUtil.isCardExiled(this)) {
                    final Card c = this;

                    c.setSuspendCast(true);
                    // set activating player for base spell ability
                    c.getSpellAbility()[0].setActivatingPlayer(c.getOwner());
                    // Any trigger should cause the phase not to skip
                    AllZone.getPhase().setSkipPhase(false);
                    AllZone.getGameAction().playCardNoCost(c);
                }
            }

            AllZone.getGameAction().checkStateEffects();

            this.updateObservers();
        }
    }

    /**
     * <p>Getter for the field <code>counters</code>.</p>
     *
     * @param counterName a {@link forge.Counters} object.
     * @return a int.
     */
    public final int getCounters(final Counters counterName) {
        if (counters.containsKey(counterName)) {
            return counters.get(counterName);
        } else {
            return 0;
        }
    }

    //get all counters from a card
    /**
     * <p>Getter for the field <code>counters</code>.</p>
     *
     * @return a Map object.
     * @since 1.0.15
     */
    public final Map<Counters, Integer> getCounters() {
        return counters;
    }

    /**
     * <p>hasCounters.</p>
     *
     * @return a boolean.
     */
    public final boolean hasCounters() {
        return counters.size() > 0;
    }
    
    public final int getNumberOfCounters() {
        int number = 0;
        for(Integer i : counters.values()) {
            number += i.intValue();
        }
        return number;
    }

    /**
     * <p>setCounter.</p>
     *
     * @param counterName a {@link forge.Counters} object.
     * @param n a int.
     * @param bSetValue a boolean.
     */
    public final void setCounter(final Counters counterName, final int n, final boolean bSetValue) {
        if (this.hasKeyword("CARDNAME can't have counters placed on it.")) {
            return;
        }
        if (this.hasKeyword("CARDNAME can't have -1/-1 counters placed on it.") && counterName.equals(Counters.M1M1)) {
            return;
        }
        // sometimes you just need to set the value without being affected by DoublingSeason
        if (bSetValue) {
            counters.put(counterName, Integer.valueOf(n));
        }
        else {
            int num = getCounters(counterName);
            // if counters on card is less than the setting value, addCounters
            if (num < n) {
                addCounter(counterName, n - num);
            } else {
                subtractCounter(counterName, num - n);
            }
        }
        this.updateObservers();
    }

    //get all counters from a card
    /**
     * <p>Setter for the field <code>counters</code>.</p>
     *
     * @param allCounters a Map object.
     * @since 1.0.15
     */
    public final void setCounters(final Map<Counters, Integer> allCounters) {
        counters = allCounters;
    }

    //get all counters from a card
    /**
     * <p>clearCounters.</p>
     *
     * @since 1.0.15
     */
    public final void clearCounters() {
        counters = new TreeMap<Counters, Integer>();
    }

    /**
     * hasLevelUp() - checks to see if a creature has the "Level up" ability introduced in Rise of the Eldrazi.
     *
     * @return true if this creature can "Level up", false otherwise
     */
    public final boolean hasLevelUp() {
        return levelUp;
    }

    /**
     * <p>Setter for the field <code>levelUp</code>.</p>
     *
     * @param b a boolean.
     */
    public final void setLevelUp(final boolean b) {
        levelUp = b;
    }

    /**
     * <p>getSVar.</p>
     *
     * @param var a {@link java.lang.String} object.
     * @return a {@link java.lang.String} object.
     */
    public final String getSVar(final String var) {
        if (sVars.containsKey(var)) {
            return sVars.get(var);
        } else {
            return "";
        }
    }

    /**
     * <p>setSVar.</p>
     *
     * @param var a {@link java.lang.String} object.
     * @param str a {@link java.lang.String} object.
     */
    public final void setSVar(final String var, final String str) {
        if (sVars.containsKey(var)) {
            sVars.remove(var);
        }

        sVars.put(var, str);
    }

    /**
     * <p>getSVars.</p>
     *
     * @return a Map object.
     */
    public final Map<String, String> getSVars() {
        return sVars;
    }

    /**
     * <p>setSVars.</p>
     *
     * @param newSVars a Map object.
     */
    public final void setSVars(final Map<String, String> newSVars) {
        sVars = newSVars;
    }

    /**
     * <p>sumAllCounters.</p>
     *
     * @return a int.
     */
    public final int sumAllCounters() {
        Object[] values = counters.values().toArray();
        int count = 0;
        int num = 0;
        for (int i = 0; i < values.length; i++) {
            num = (Integer) values[i];
            count += num;
        }
        return count;
    }

    /**
     * <p>getNetPTCounters.</p>
     *
     * @return a int.
     */
    public final int getNetPTCounters() {
        return getCounters(Counters.P1P1) - getCounters(Counters.M1M1);
    }

    /**
     * <p>Getter for the field <code>turnInZone</code>.</p>
     *
     * @return a int.
     */
    public final int getTurnInZone() {
        return turnInZone;
    }

    /**
     * <p>Setter for the field <code>turnInZone</code>.</p>
     *
     * @param turn a int.
     */
    public final void setTurnInZone(final int turn) {
        turnInZone = turn;
    }

    /**
     * <p>Setter for the field <code>echoCost</code>.</p>
     *
     * @param s a {@link java.lang.String} object.
     */
    public final void setEchoCost(final String s) {
        echoCost = s;
    }

    /**
     * <p>Getter for the field <code>echoCost</code>.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    public final String getEchoCost() {
        return echoCost;
    }

    /**
     * <p>Setter for the field <code>manaCost</code>.</p>
     *
     * @param s a {@link java.lang.String} object.
     */
    public final void setManaCost(final String s) {
        manaCost = s;
    }

    /**
     * <p>Getter for the field <code>manaCost</code>.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    public final String getManaCost() {
        return manaCost;
    }

    /**
     * <p>addColor.</p>
     *
     * @param s a {@link java.lang.String} object.
     */
    public final void addColor(String s) {
        if (s.equals("")) {
            s = "0";
        }
        cardColor.add(new Card_Color(new ManaCost(s), this, false, true));
    }

    /**
     * <p>addColor.</p>
     *
     * @param s a {@link java.lang.String} object.
     * @param c a {@link forge.Card} object.
     * @param addToColors a boolean.
     * @param bIncrease a boolean.
     * @return a long.
     */
    public final long addColor(final String s, final Card c, final boolean addToColors, final boolean bIncrease) {
        if (bIncrease) {
            Card_Color.increaseTimestamp();
        }
        cardColor.add(new Card_Color(new ManaCost(s), c, addToColors, false));
        return Card_Color.getTimestamp();
    }

    /**
     * <p>removeColor.</p>
     *
     * @param s a {@link java.lang.String} object.
     * @param c a {@link forge.Card} object.
     * @param addTo a boolean.
     * @param timestampIn a long.
     */
    public final void removeColor(final String s, final Card c, final boolean addTo, final long timestampIn) {
        Card_Color removeCol = null;
        for (Card_Color cc : cardColor) {
            if (cc.equals(s, c, addTo, timestampIn)) {
                removeCol = cc;
            }
        }

        if (removeCol != null) {
            cardColor.remove(removeCol);
        }
    }

    /**
     * <p>determineColor.</p>
     *
     * @return a {@link forge.Card_Color} object.
     */
    public final Card_Color determineColor() {
        if (this.isImmutable()) {
            return new Card_Color(this);
        }
        Card_Color colors = null;
        ArrayList<Card_Color> globalChanges = AllZone.getColorChanger().getColorChanges();
        colors = determineColor(globalChanges);
        colors.fixColorless();
        return colors;
    }

    /**
     * <p>setColor.</p>
     *
     * @param colors a {@link java.util.ArrayList} object.
     */
    public final void setColor(final ArrayList<Card_Color> colors) {
        cardColor = colors;
    }

    /**
     * <p>getColor.</p>
     *
     * @return a {@link java.util.ArrayList} object.
     */
    public final ArrayList<Card_Color> getColor() {
        return cardColor;
    }

    /**
     * 
     * TODO Write javadoc for this method.
     * @param globalChanges an ArrayList<Card_Color>
     * @return a Card_Color
     */
    final Card_Color determineColor(final ArrayList<Card_Color> globalChanges) {
        Card_Color colors = new Card_Color(this);
        int i = cardColor.size() - 1;
        int j = -1;
        if (globalChanges != null) { j = globalChanges.size() - 1; }
        // if both have changes, see which one is most recent
        while (i >= 0 && j >= 0) {
            Card_Color cc = null;
            if (cardColor.get(i).getStamp() > globalChanges.get(j).getStamp()) {
                // Card has a more recent color stamp
                cc = cardColor.get(i);
                i--;
            } else {
                // Global effect has a more recent color stamp
                cc = globalChanges.get(j);
                j--;
            }

            for (String s : cc.toStringArray()) {
                colors.addToCardColor(s);
            }
            if (!cc.getAdditional()) {
                return colors;
            }
        }
        while (i >= 0) {
            Card_Color cc = cardColor.get(i);
            i--;
            for (String s : cc.toStringArray()) {
                colors.addToCardColor(s);
            }
            if (!cc.getAdditional()) {
                return colors;
            }
        }
        while (j >= 0) {
            Card_Color cc = globalChanges.get(j);
            j--;
            for (String s : cc.toStringArray()) {
                colors.addToCardColor(s);
            }
            if (!cc.getAdditional()) {
                return colors;
            }
        }

        return colors;
    }

    /**
     * <p>getCMC.</p>
     *
     * @return a int.
     */
    public final int getCMC() {
        return CardUtil.getConvertedManaCost(manaCost);
    }
    
    /**
     * <p>Getter for the field <code>chosenPlayer</code>.</p>
     *
     * @return a Player
     * @since 1.1.6
     */
    public final Player getChosenPlayer() {
        return chosenPlayer;
    }

    /**
     * <p>Setter for the field <code>chosenNumber</code>.</p>
     *
     * @param s an int
     * @since 1.1.6
     */
    public final void setChosenPlayer(final Player p) {
        chosenPlayer = p;
    }
    
    /**
     * <p>Getter for the field <code>chosenNumber</code>.</p>
     *
     * @return an int
     */
    public final int getChosenNumber() {
        return chosenNumber;
    }

    /**
     * <p>Setter for the field <code>chosenNumber</code>.</p>
     *
     * @param s an int
     */
    public final void setChosenNumber(final int i) {
        chosenNumber = i;
    }

    //used for cards like Belbe's Portal, Conspiracy, Cover of Darkness, etc.
    /**
     * <p>Getter for the field <code>chosenType</code>.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    public final String getChosenType() {
        return chosenType;
    }

    /**
     * <p>Setter for the field <code>chosenType</code>.</p>
     *
     * @param s a {@link java.lang.String} object.
     */
    public final void setChosenType(final String s) {
        chosenType = s;
    }

    /**
     * <p>Getter for the field <code>chosenColor</code>.</p>
     *
     * @return an ArrayList<String> object.
     */
    public final ArrayList<String> getChosenColor() {
        return chosenColor;
    }

    /**
     * <p>Setter for the field <code>chosenColor</code>.</p>
     *
     * @param s an ArrayList<String> object.
     */
    public final void setChosenColor(final ArrayList<String> s) {
        chosenColor = s;
    }

    //used for cards like Meddling Mage...
    /**
     * <p>Getter for the field <code>namedCard</code>.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    public final String getNamedCard() {
        return namedCard;
    }

    /**
     * <p>Setter for the field <code>namedCard</code>.</p>
     *
     * @param s a {@link java.lang.String} object.
     */
    public final void setNamedCard(final String s) {
        namedCard = s;
    }

    /**
     * <p>Setter for the field <code>drawnThisTurn</code>.</p>
     *
     * @param b a boolean.
     */
    public final void setDrawnThisTurn(final boolean b) {
        drawnThisTurn = b;
    }

    /**
     * <p>Getter for the field <code>drawnThisTurn</code>.</p>
     *
     * @return a boolean.
     */
    public final boolean getDrawnThisTurn() {
        return drawnThisTurn;
    }

    /**
     * get a list of Cards this card has gained control of.
     * <p/>
     * used primarily with AbilityFactory_GainControl
     *
     * @return a list of cards this card has gained control of
     */
    public final ArrayList<Card> getGainControlTargets() {
        return gainControlTargets;
    }

    /**
     * add a Card to the list of Cards this card has gained control of.
     * <p/>
     * used primarily with AbilityFactory_GainControl
     *
     * @param c a {@link forge.Card} object.
     */
    public final void addGainControlTarget(Card c) {
        gainControlTargets.add(c);
    }

    /**
     * clear the list of Cards this card has gained control of.
     * <p/>
     * used primarily with AbilityFactory_GainControl
     */
    public final void clearGainControlTargets() {
        gainControlTargets.clear();
    }

    /**
     * get the commands to be executed to lose control of Cards this
     * card has gained control of.
     * <p/>
     * used primarily with AbilityFactory_GainControl (Old Man of the Sea specifically)
     *
     * @return a {@link java.util.ArrayList} object.
     */
    public final ArrayList<Command> getGainControlReleaseCommands() {
        return gainControlReleaseCommands;
    }

    /**
     * set a command to be executed to lose control of Cards this
     * card has gained control of.
     * <p/>
     * used primarily with AbilityFactory_GainControl (Old Man of the Sea specifically)
     *
     * @param c the Command to be executed
     */
    public final void addGainControlReleaseCommand(final Command c) {
        gainControlReleaseCommands.add(c);
    }

    /**
     * <p>clearGainControlReleaseCommands.</p>
     */
    public final void clearGainControlReleaseCommands() {
        gainControlReleaseCommands.clear();
    }

    /**
     * <p>getSpellText.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    public final String getSpellText() {
        return text;
    }

    /**
     * <p>Setter for the field <code>text</code>.</p>
     *
     * @param t a {@link java.lang.String} object.
     */
    public final void setText(final String t) {
        text = t;
    }

    // get the text that should be displayed
    /**
     * <p>Getter for the field <code>text</code>.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String getText() {
        StringBuilder sb = new StringBuilder();
        sb.append(this.getAbilityText());
        String nonAbilityText = getNonAbilityText();
        if (nonAbilityText.length() > 0) {
            sb.append("\r\n \r\nNon ability features: \r\n");
            sb.append(nonAbilityText.replaceAll("CARDNAME", getName()));
        }

        return sb.toString();
    }

    // get the text that does not belong to a cards abilities (and is not really there rules-wise)
    /**
     * <p>getNonAbilityText.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    public final String getNonAbilityText() {
        StringBuilder sb = new StringBuilder();
        ArrayList<String> keyword = getHiddenExtrinsicKeyword();

        sb.append(keywordsToText(keyword));

        return sb.toString();
    }

    // convert a keyword list to the String that should be displayed ingame
    /**
     * <p>keywordsToText.</p>
     *
     * @param keyword a {@link java.util.ArrayList} object.
     * @return a {@link java.lang.String} object.
     */
    public final String keywordsToText(final ArrayList<String> keyword) {
        StringBuilder sb = new StringBuilder();
        StringBuilder sbLong = new StringBuilder();
        StringBuilder sbMana = new StringBuilder();

        for (int i = 0; i < keyword.size(); i++) {
            if (!keyword.get(i).toString().contains("CostChange")
                    &&
                    !keyword.get(i).toString().contains("Permanents don't untap during their controllers' untap steps")
                    &&
                    !keyword.get(i).toString().contains("PreventAllDamageBy")
                    &&
                    !keyword.get(i).toString().contains("CantBlock")
                    &&
                    !keyword.get(i).toString().contains("CantBeBlockedBy"))
            {
                if (keyword.get(i).toString().contains("StaticEffect")) {
                    String[] k = keyword.get(i).split(":");
                    sbLong.append(k[5]).append("\r\n");
                } else if (keyword.get(i).toString().contains("Protection:")) {
                    String[] k = keyword.get(i).split(":");
                    sbLong.append(k[2]).append("\r\n");
                } else if (keyword.get(i).toString().contains("Creatures can't attack unless their controller pays")) {
                    String[] k = keyword.get(i).split(":");
                    if (!k[3].equals("no text")) {
                        sbLong.append(k[3]).append("\r\n");
                    }
                } else if (keyword.get(i).startsWith("Enchant")) {
                    String k = keyword.get(i);
                    k = k.replace("Curse", "");
                    sbLong.append(k).append("\r\n");
                } else if (keyword.get(i).startsWith("Soulshift") || keyword.get(i).startsWith("Cumulative upkeep")
                        || keyword.get(i).startsWith("Echo") || keyword.get(i).startsWith("Fading")
                        || keyword.get(i).startsWith("Ripple") || keyword.get(i).startsWith("Unearth")
                        || keyword.get(i).startsWith("Vanishing") || keyword.get(i).startsWith("Madness")
                        || keyword.get(i).startsWith("Devour")) {
                    String k = keyword.get(i);
                    k = k.replace(":", " ");
                    sbLong.append(k).append("\r\n");
                } else if (keyword.get(i).startsWith("Champion")) {
                    String k = getKeyword().get(i);
                    String[] kk = k.split(":");
                    String types = kk[1];
                    if (kk.length > 2) {
                        types = kk[2];
                    }
                    if (kk[1].equals("Creature")) {
                        kk[1] = kk[1].toLowerCase();
                    }
                    sbLong.append("Champion a");
                    if (kk[1].toLowerCase().startsWith("a")
                            || kk[1].toLowerCase().startsWith("e")
                            || kk[1].toLowerCase().startsWith("i")
                            || kk[1].toLowerCase().startsWith("o")
                            || kk[1].toLowerCase().startsWith("u")) {
                        sbLong.append("n");
                    }
                    sbLong.append(" ").append(types);
                    sbLong.append(" (When this enters the battlefield, sacrifice it unless you exile another ");
                    sbLong.append(types);
                    sbLong.append(" you control. When this leaves the battlefield, that card returns to the battlefield.)\r\n");
                } else if (keyword.get(i).endsWith(".") && !keyword.get(i).startsWith("Haunt")) {
                    sbLong.append(keyword.get(i).toString()).append("\r\n");
                } else if (keyword.get(i).contains("At the beginning of your upkeep, ")
                        && keyword.get(i).contains(" unless you pay"))
                {
                    sbLong.append(keyword.get(i).toString()).append("\r\n");
                } else if (keyword.get(i).toString().contains("tap: add ")) {
                    sbMana.append(keyword.get(i).toString()).append("\r\n");
                } else if (keyword.get(i).contains("Bloodthirst")) {
                    String k = keyword.get(i);
                    String[] kk = k.split(" ");
                    sbLong.append(keyword.get(i)).append(" (If an opponent was dealt damage this turn, this creature enters the battlefield with ");
                    sbLong.append(kk[1]).append(" +1/+1 counter");
                    if (kk[1].equals("X")) {
                        sbLong.append("s on it, where X is the damage dealt to your opponents this turn.)");
                        sbLong.append("\r\n");
                    } else {
                        if (Integer.parseInt(kk[1]) > 1) {
                            sbLong.append("s");
                        }
                        sbLong.append(" on it.)").append("\r\n");
                    }
                } else if (keyword.get(i).startsWith("Modular")) {
                    String numCounters = keyword.get(i).split(" ")[1];
                    sbLong.append(keyword.get(i));
                    sbLong.append(" (This enters the battlefield with ");
                    sbLong.append(numCounters);
                    sbLong.append(" +1/+1 counters on it. When it's put into a graveyard, you may put its +1/+1 counters on target artifact creature.)");
                } else if (keyword.get(i).startsWith("MayEffectFromOpeningHand")) {
                    continue;
                } else if (keyword.get(i).contains("Haunt")) {
                    sb.append("\r\nHaunt (");
                    if(isCreature()) {
                        sb.append("When this creature dies, exile it haunting target creature.");
                    }
                    else
                    {
                        sb.append("When this spell card is put into a graveyard after resolving, exile it haunting target creature.");
                    }
                    sb.append(")");
                    continue;
                } else {
                    if (i != 0 && sb.length() != 0) {
                        sb.append(", ");
                    }
                    sb.append(keyword.get(i).toString());
                }
            }
        }
        if (sb.length() > 0) {
            sb.append("\r\n\r\n");
        }
        if (sbLong.length() > 0) {
            sbLong.append("\r\n");
        }
        sb.append(sbLong);
        sb.append(sbMana);
        return sb.toString();
    }

    //get the text of the abilities of a card
    /**
     * <p>getAbilityText.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String getAbilityText() {
        if (isInstant() || isSorcery()) {
            String s = getSpellText();
            StringBuilder sb = new StringBuilder();

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
            SpellAbility[] sa = getSpellAbility();
            for (int i = 0; i < sa.length; i++) {
                sb.append(sa[i].toString() + "\r\n");
            }

            // Add Keywords
            ArrayList<String> kw = getKeyword();

            // Triggered abilities
            for (Trigger trig : triggers) {
                if (!trig.isSecondary()) {
                    sb.append(trig.toString() + "\r\n");
                }
            }

            // static abilities
            for (StaticAbility stAb : staticAbilities) {
            	String stAbD = stAb.toString();
            	if (!stAbD.equals("")) {
                    sb.append(stAbD + "\r\n");
                }
            }

            // Ripple + Dredge + Madness + CARDNAME is {color} + Recover.
            for (int i = 0; i < kw.size(); i++) {
                if ((kw.get(i).startsWith("Ripple") && !sb.toString().contains("Ripple"))
                        || (kw.get(i).startsWith("Dredge") && !sb.toString().contains("Dredge"))
                        || (kw.get(i).startsWith("Madness") && !sb.toString().contains("Madness"))
                        || (kw.get(i).startsWith("CARDNAME is ") && !sb.toString().contains("CARDNAME is "))
                        || (kw.get(i).startsWith("Recover") && !sb.toString().contains("Recover")))
                {
                    sb.append(kw.get(i).replace(":", " ")).append("\r\n");
                }
            }

            // Changeling + CARDNAME can't be countered. + Cascade + Multikicker
            for (int i = 0; i < kw.size(); i++) {
                if ((kw.get(i).contains("CARDNAME can't be countered.")
                        && !sb.toString().contains("CARDNAME can't be countered."))
                        || (kw.get(i).contains("Cascade") && !sb.toString().contains("Cascade"))
                        || (kw.get(i).contains("Multikicker") && !sb.toString().contains("Multikicker")))
                {
                    sb.append(kw.get(i)).append("\r\n");
                }
            }

            // Storm
            if (hasKeyword("Storm") && !sb.toString().contains("Storm (When you ")) {
                if (sb.toString().endsWith("\r\n\r\n")) {
                    sb.delete(sb.lastIndexOf("\r\n"), sb.lastIndexOf("\r\n") + 3);
                }
                sb.append("Storm (When you cast this spell, copy it for each spell cast before it this turn.");
                if (sb.toString().contains("Target") || sb.toString().contains("target")) {
                    sb.append(" You may choose new targets for the copies.");
                }
                sb.append(")\r\n");
            }

            //Replicate
            for (String keyw : kw) {
                if (keyw.contains("Replicate") && !sb.toString().contains("you paid its replicate cost.")) {
                    if (sb.toString().endsWith("\r\n\r\n")) {
                        sb.delete(sb.lastIndexOf("\r\n"), sb.lastIndexOf("\r\n") + 3);
                    }
                    sb.append(keyw);
                    sb.append(" (When you cast this spell, copy it for each time you paid its replicate cost.");
                    if (sb.toString().contains("Target") || sb.toString().contains("target")) {
                        sb.append(" You may choose new targets for the copies.");
                    }
                    sb.append(")\r\n");
                }
            }
            
            for(String keyw : kw) {
                if(keyw.startsWith("Haunt")) {
                    if (sb.toString().endsWith("\r\n\r\n")) {
                        sb.delete(sb.lastIndexOf("\r\n"), sb.lastIndexOf("\r\n") + 3);
                    }
                    sb.append("Haunt (");
                    if(isCreature()) {
                        sb.append("When this creature dies, exile it haunting target creature.");
                    }
                    else {
                        sb.append("When this spell card is put into a graveyard after resolving, exile it haunting target creature.");
                    }
                    sb.append(")\r\n");
                }
            }
            
            if(haunting != null) {
                sb.append("Haunting: ").append(haunting);
                sb.append("\r\n");
            }
            
            while (sb.toString().endsWith("\r\n")) {
                sb.delete(sb.lastIndexOf("\r\n"), sb.lastIndexOf("\r\n") + 3);
            }            

            return sb.toString().replaceAll("CARDNAME", getName());
        }

        StringBuilder sb = new StringBuilder();
        ArrayList<String> keyword = getUnhiddenKeyword();

        sb.append(keywordsToText(keyword));

        // Give spellText line breaks for easier reading
        sb.append("\r\n");
        sb.append(text.replaceAll("\\\\r\\\\n", "\r\n"));
        sb.append("\r\n");

        /*
         * if(isAura()) {
            // Give spellText line breaks for easier reading
            sb.append(getSpellText().replaceAll("\\\\r\\\\n", "\r\n")).append("\r\n");
        }
        */

        // Triggered abilities
        for (Trigger trig : triggers) {
            if (!trig.isSecondary()) {
                sb.append(trig.toString() + "\r\n");
            }
        }

        // static abilities
        for (StaticAbility stAb : staticAbilities) {
        	sb.append(stAb.toString() + "\r\n");
        }

        ArrayList<String> addedManaStrings = new ArrayList<String>();
        SpellAbility[] abilities = getSpellAbility();
        boolean primaryCost = true;
        for (SpellAbility sa : abilities) {
            // only add abilities not Spell portions of cards
            if (!isPermanent()) {
                continue;
            }

            if (sa instanceof Spell_Permanent && primaryCost && !isAura()) {
                // For Alt costs, make sure to display the cost!
                primaryCost = false;
                continue;
            }

            String sAbility = sa.toString();

            if (sa instanceof Ability_Mana) {
                if (addedManaStrings.contains(sAbility)) {
                    continue;
                }
                addedManaStrings.add(sAbility);
            }

            if (sa instanceof Spell_Permanent && !isAura()) {
                sb.insert(0, "\r\n");
                sb.insert(0, sAbility);
            } else if (!sAbility.endsWith(getName())) {
                sb.append(sAbility);
                sb.append("\r\n");
                // The test above appears to prevent the card name from showing and therefore
                //it no longer needs to be deleted from the stringbuilder
                //if (sb.toString().endsWith("CARDNAME"))
                //    sb.replace(sb.toString().lastIndexOf("CARDNAME"),
                //    sb.toString().lastIndexOf("CARDNAME") + name.length() - 1, "");
            }
        }

        // NOTE:
        if (sb.toString().contains(" (NOTE: ")) {
            sb.insert(sb.indexOf("(NOTE: "), "\r\n");
        }
        if (sb.toString().contains("(NOTE: ") && sb.toString().contains(".) ")) {
            sb.insert(sb.indexOf(".) ") + 3, "\r\n");
        }

        // replace tripple line feeds with double line feeds
        int start;
        String s = "\r\n\r\n\r\n";
        while (sb.toString().contains(s)) {
            start = sb.lastIndexOf(s);
            if (start < 0 || start >= sb.length()) {
                break;
            }
            sb.replace(start, start + 4, "\r\n");
        }

        //Remembered cards
        if (rememberedObjects.size() > 0) {
            sb.append("\r\nRemembered: \r\n");
            for (Object o : rememberedObjects) {
                if (o instanceof Card) {
                    Card c = (Card) o;
                    sb.append(c.getName());
                    sb.append("(");
                    sb.append(c.getUniqueNumber());
                    sb.append(")");
                } else {
                    sb.append(o.toString());
                }
                sb.append("\r\n");
            }
        }
        
        if(hauntedBy.size() != 0) {
            sb.append("Haunted by: ");
            for(Card c : hauntedBy) {
                sb.append(c).append(",");
            }
            sb.deleteCharAt(sb.length()-1);
            sb.append("\r\n");
        }
        
        if(haunting != null) {
            sb.append("Haunting: ").append(haunting);
            sb.append("\r\n");
        }

        /*
        sb.append("\r\nOwner: ").append(owner).append("\r\n");
        sb.append("Controller(s):");
        for(Object o : controllerObjects)
        {
            sb.append(o);
        }
        sb.append("\r\n");
        */
        return sb.toString().replaceAll("CARDNAME", getName()).trim();
    } //getText()

    /**
     * <p>Getter for the field <code>manaAbility</code>.</p>
     *
     * @return a {@link java.util.ArrayList} object.
     */
    public final ArrayList<Ability_Mana> getManaAbility() {
        return new ArrayList<Ability_Mana>(manaAbility);
    }

    // Returns basic mana abilities plus "reflected mana" abilities
    /**
     * <p>getAIPlayableMana.</p>
     *
     * @return a {@link java.util.ArrayList} object.
     */
    public final ArrayList<Ability_Mana> getAIPlayableMana() {
        ArrayList<Ability_Mana> res = new ArrayList<Ability_Mana>();
        for (Ability_Mana am : getManaAbility()) {

            //if a mana ability has a mana cost the AI will miscalculate
            Cost cost = am.getPayCosts();
            if (!cost.hasNoManaCost()) {
                continue;
            }

            if (am.isBasic() && !res.contains(am)) {
                res.add(am);
            } else if (am.isReflectedMana() && !res.contains(am)) {
                res.add(am);
            }
        }

        return res;

    }

    /**
     * <p>getBasicMana.</p>
     *
     * @return a {@link java.util.ArrayList} object.
     */
    public final ArrayList<Ability_Mana> getBasicMana() {
        ArrayList<Ability_Mana> res = new ArrayList<Ability_Mana>();
        for (Ability_Mana am : getManaAbility())
            if (am.isBasic() && !res.contains(am)) {
                res.add(am);
            }
        return res;
    }

    /**
     * <p>clearFirstSpellAbility.</p>
     */
    public final void clearFirstSpell() {
    	for(int i = 0; i < spellAbility.size(); i++) {
    		if (spellAbility.get(i).isSpell()) {
    			spellAbility.remove(i);
    			return;
    		}
    	}
    }

    /**
     * <p>clearAllButFirstSpellAbility.</p>
     */
    public final void clearAllButFirstSpellAbility() {
        if (!spellAbility.isEmpty()) {
            SpellAbility first = spellAbility.get(0);
            spellAbility.clear();
            spellAbility.add(first);
        }
        manaAbility.clear();
    }

    /**
     * <p>getAllButFirstSpellAbility.</p>
     *
     * @return a {@link java.util.ArrayList} object.
     */
    public final ArrayList<SpellAbility> getAllButFirstSpellAbility() {
        ArrayList<SpellAbility> sas = new ArrayList<SpellAbility>();
        sas.addAll(spellAbility);
        if (!sas.isEmpty()) {
            SpellAbility first = spellAbility.get(0);
            sas.remove(first);
        }
        sas.addAll(manaAbility);

        return sas;
    }

    /**
     * <p>clearSpellAbility.</p>
     */
    public final void clearSpellAbility() {
        spellAbility.clear();
        manaAbility.clear();
    }

    /**
     * <p>getSpellPermanent.</p>
     *
     * @return a {@link forge.card.spellability.Spell_Permanent} object.
     */
    public final Spell_Permanent getSpellPermanent() {
        for (SpellAbility sa : spellAbility) {
            if (sa instanceof Spell_Permanent) {
                return (Spell_Permanent) sa;
            }
        }
        return null;
    }

    /**
     * <p>clearSpellKeepManaAbility.</p>
     */
    public final void clearSpellKeepManaAbility() {
        spellAbility.clear();
    }

    /**
     * <p>clearManaAbility.</p>
     */
    public final void clearManaAbility() {
        manaAbility.clear();
    }


    /**
     * <p>addFirstSpellAbility.</p>
     *
     * @param a a {@link forge.card.spellability.SpellAbility} object.
     */
    public final void addFirstSpellAbility(final SpellAbility a) {
        a.setSourceCard(this);
        if (a instanceof Ability_Mana) {
            manaAbility.add(0, (Ability_Mana) a);
        } else {
            spellAbility.add(0, a);
        }
    }

    /**
     * <p>addSpellAbility.</p>
     *
     * @param a a {@link forge.card.spellability.SpellAbility} object.
     */
    public final void addSpellAbility(final SpellAbility a) {
        a.setSourceCard(this);
        if (a instanceof Ability_Mana) {
            manaAbility.add((Ability_Mana) a);
        } else {
            spellAbility.add(a);
        }
    }

    /**
     * <p>removeSpellAbility.</p>
     *
     * @param a a {@link forge.card.spellability.SpellAbility} object.
     */
    public final void removeSpellAbility(final SpellAbility a) {
        if (a instanceof Ability_Mana) {
            //if (a.isExtrinsic()) //never remove intrinsic mana abilities, is this the way to go??
            manaAbility.remove(a);
        }
        else {
            spellAbility.remove(a);
        }
    }


    /**
     * <p>removeAllExtrinsicManaAbilities.</p>
     */
    public final void removeAllExtrinsicManaAbilities() {
        //temp ArrayList, otherwise ConcurrentModificationExceptions occur:
        ArrayList<SpellAbility> saList = new ArrayList<SpellAbility>();

        for (SpellAbility var : manaAbility) {
            if (var.isExtrinsic()) {
                saList.add(var);
            }
        }
        for (SpellAbility sa : saList) {
            removeSpellAbility(sa);
        }
    }

    /**
     * <p>getIntrinsicManaAbilitiesDescriptions.</p>
     *
     * @return a {@link java.util.ArrayList} object.
     */
    public ArrayList<String> getIntrinsicManaAbilitiesDescriptions() {
        ArrayList<String> list = new ArrayList<String>();
        for (SpellAbility var : manaAbility) {
            if (var.isIntrinsic()) list.add(var.toString());
        }
        return list;
    }

    /**
     * <p>Getter for the field <code>spellAbility</code>.</p>
     *
     * @return an array of {@link forge.card.spellability.SpellAbility} objects.
     */
    public SpellAbility[] getSpellAbility() {
        ArrayList<SpellAbility> res = new ArrayList<SpellAbility>(spellAbility);
        res.addAll(getManaAbility());
        SpellAbility[] s = new SpellAbility[res.size()];
        res.toArray(s);
        return s;
    }

    /**
     * <p>getSpellAbilities.</p>
     *
     * @return a {@link java.util.ArrayList} object.
     */
    public ArrayList<SpellAbility> getSpellAbilities() {
        ArrayList<SpellAbility> res = new ArrayList<SpellAbility>(spellAbility);
        res.addAll(getManaAbility());
        return res;
    }

    /**
     * <p>getSpells.</p>
     *
     * @return a {@link java.util.ArrayList} object.
     */
    public ArrayList<SpellAbility> getSpells() {
        ArrayList<SpellAbility> s = new ArrayList<SpellAbility>(spellAbility);
        ArrayList<SpellAbility> res = new ArrayList<SpellAbility>();

        for (SpellAbility sa : s) {
            if (sa.isSpell()) res.add(sa);
        }
        return res;
    }

    /**
     * <p>getBasicSpells.</p>
     *
     * @return a {@link java.util.ArrayList} object.
     */
    public ArrayList<SpellAbility> getBasicSpells() {
        ArrayList<SpellAbility> s = new ArrayList<SpellAbility>(spellAbility);
        ArrayList<SpellAbility> res = new ArrayList<SpellAbility>();

        for (SpellAbility sa : s) {
            if (sa.isSpell() && !sa.isFlashBackAbility() && !sa.isBuyBackAbility()) res.add(sa);
        }
        return res;
    }

    /**
     * <p>getAdditionalCostSpells.</p>
     *
     * @return a {@link java.util.ArrayList} object.
     */
    public ArrayList<SpellAbility> getAdditionalCostSpells() {
        ArrayList<SpellAbility> s = new ArrayList<SpellAbility>(spellAbility);
        ArrayList<SpellAbility> res = new ArrayList<SpellAbility>();

        for (SpellAbility sa : s) {
            if (sa.isSpell() && !sa.getAdditionalManaCost().equals("")) res.add(sa);
        }
        return res;
    }

    //shield = regeneration
    /**
     * <p>setShield.</p>
     *
     * @param n a int.
     */
    public void setShield(int n) {
        nShield = n;
    }

    /**
     * <p>getShield.</p>
     *
     * @return a int.
     */
    public int getShield() {
        return nShield;
    }

    /**
     * <p>addShield.</p>
     */
    public void addShield() {
        nShield++;
    }

    /**
     * <p>subtractShield.</p>
     */
    public void subtractShield() {
        nShield--;
    }

    /**
     * <p>resetShield.</p>
     */
    public void resetShield() {
        nShield = 0;
    }

    /**
     * <p>canBeShielded.</p>
     *
     * @return a boolean.
     */
    public boolean canBeShielded() {
        return !hasKeyword("CARDNAME can't be regenerated.");
    }

    //is this "Card" supposed to be a token?
    /**
     * <p>Setter for the field <code>token</code>.</p>
     *
     * @param b a boolean.
     */
    public void setToken(boolean b) {
        token = b;
    }

    /**
     * <p>isToken.</p>
     *
     * @return a boolean.
     */
    public boolean isToken() {
        return token;
    }

    /**
     * <p>Setter for the field <code>copiedToken</code>.</p>
     *
     * @param b a boolean.
     */
    public void setCopiedToken(boolean b) {
        copiedToken = b;
    }

    /**
     * <p>isCopiedToken.</p>
     *
     * @return a boolean.
     */
    public boolean isCopiedToken() {
        return copiedToken;
    }

    /**
     * <p>Setter for the field <code>copiedSpell</code>.</p>
     *
     * @param b a boolean.
     */
    public void setCopiedSpell(boolean b) {
        copiedSpell = b;
    }

    /**
     * <p>isCopiedSpell.</p>
     *
     * @return a boolean.
     */
    public boolean isCopiedSpell() {
        return copiedSpell;
    }

    /**
     * <p>addSpellChoice.</p>
     *
     * @param string a {@link java.lang.String} object.
     */
    public void addSpellChoice(String string) {
        choicesMade.add(string);
    }

    /**
     * <p>getChoices.</p>
     *
     * @return a {@link java.util.ArrayList} object.
     */
    public ArrayList<String> getChoices() {
        return choicesMade;
    }

    /**
     * <p>getChoice.</p>
     *
     * @param i a int.
     * @return a {@link java.lang.String} object.
     */
    public final String getChoice(final int i) {
        return choicesMade.get(i);
    }

    /**
     * <p>setSpellChoiceTarget.</p>
     *
     * @param string a {@link java.lang.String} object.
     */
    public final void setSpellChoiceTarget(final String string) {
        targetsForChoices.add(string);
    }

    /**
     * <p>getChoiceTargets.</p>
     *
     * @return a {@link java.util.ArrayList} object.
     */
    public final ArrayList<String> getChoiceTargets() {
        return targetsForChoices;
    }

    /**
     * <p>getChoiceTarget.</p>
     *
     * @param i a int.
     * @return a {@link java.lang.String} object.
     */
    public final String getChoiceTarget(final int i) {
        return targetsForChoices.get(i);
    }

    /**
     * <p>setSpellWithChoices.</p>
     *
     * @param b a boolean.
     */
    public final void setSpellWithChoices(final boolean b) {
        spellWithChoices = b;
    }

    /**
     * <p>hasChoices.</p>
     *
     * @return a boolean.
     */
    public final boolean hasChoices() {
        return spellWithChoices;
    }

    /**
     * <p>setCopiesSpells.</p>
     *
     * @param b a boolean.
     */
    public final void setCopiesSpells(final boolean b) {
        spellCopyingCard = b;
    }

    /**
     * <p>copiesSpells.</p>
     *
     * @return a boolean.
     */
    public final boolean copiesSpells() {
        return spellCopyingCard;
    }

    /**
     * <p>setIsFaceDown.</p>
     *
     * @param b a boolean.
     */
    public final void setIsFaceDown(final boolean b) {
        faceDown = b;
    }

    /**
     * <p>isFaceDown.</p>
     *
     * @return a boolean.
     */
    public final boolean isFaceDown() {
        return faceDown;
    }

    /**
     * <p>setCanMorph.</p>
     * @param b a boolean.
     */
    public final void setCanMorph(final boolean b) {
        canMorph = b;
    }

    /**
     * <p>getCanMorph.</p>
     * @return a boolean.
     */
    public final boolean getCanMorph() {
        return canMorph;
    }

    /**
     * <p>addTrigger.</p>
     *
     * @param c a {@link forge.Command} object.
     * @param typeIn a {@link forge.ZCTrigger} object.
     */
    public final void addTrigger(final Command c, final ZCTrigger typeIn) {
        zcTriggers.add(new Ability_Triggered(this, c, typeIn));
    }

    /**
     * <p>removeTrigger.</p>
     *
     * @param c a {@link forge.Command} object.
     * @param typeIn a {@link forge.ZCTrigger} object.
     */
    public final void removeTrigger(final Command c, final ZCTrigger typeIn) {
        zcTriggers.remove(new Ability_Triggered(this, c, typeIn));
    }

    /**
     * <p>executeTrigger.</p>
     *
     * @param type a {@link forge.ZCTrigger} object.
     */
    public final void executeTrigger(final ZCTrigger type) {
        for (Ability_Triggered t : zcTriggers) {
            if (t.trigger.equals(type) && t.isBasic()) {
                t.execute();
            }
        }
    }

    /**
     * <p>clearTriggers.</p>
     */
    public final void clearTriggers() {
        zcTriggers.clear();
    }

    /**
     * <p>addComesIntoPlayCommand.</p>
     *
     * @param c a {@link forge.Command} object.
     */
    public final void addComesIntoPlayCommand(final Command c) {
        addTrigger(c, ZCTrigger.ENTERFIELD);
    }

    /**
     * <p>removeComesIntoPlayCommand.</p>
     *
     * @param c a {@link forge.Command} object.
     */
    public final void removeComesIntoPlayCommand(final Command c) {
        removeTrigger(c, ZCTrigger.ENTERFIELD);
    }

    /**
     * <p>comesIntoPlay.</p>
     */
    public final void comesIntoPlay() {
        executeTrigger(ZCTrigger.ENTERFIELD);
    }

    /**
     * <p>addTurnFaceUpCommand.</p>
     *
     * @param c a {@link forge.Command} object.
     */
    public final void addTurnFaceUpCommand(final Command c) {
        turnFaceUpCommandList.add(c);
    }

    /**
     * <p>removeTurnFaceUpCommand.</p>
     *
     * @param c a {@link forge.Command} object.
     */
    public final void removeTurnFaceUpCommand(final Command c) {
        turnFaceUpCommandList.remove(c);
    }

    /**
     * <p>turnFaceUp.</p>
     */
    public final void turnFaceUp() {
        for (Command var : turnFaceUpCommandList) {
            var.execute();
        }

        //Run triggers
        Map<String, Object> runParams = new TreeMap<String, Object>();
        runParams.put("Card", this);
        AllZone.getTriggerHandler().runTrigger("TurnFaceUp", runParams);
    }

    /**
     * <p>addDestroyCommand.</p>
     *
     * @param c a {@link forge.Command} object.
     */
    public final void addDestroyCommand(final Command c) {
        addTrigger(c, ZCTrigger.DESTROY);
    }

    /**
     * <p>removeDestroyCommand.</p>
     *
     * @param c a {@link forge.Command} object.
     */
    public final void removeDestroyCommand(final Command c) {
        removeTrigger(c, ZCTrigger.DESTROY);
    }

    /**
     * <p>destroy.</p>
     */
    public final void destroy() {
        executeTrigger(ZCTrigger.DESTROY);
    }

    /**
     * <p>addLeavesPlayCommand.</p>
     *
     * @param c a {@link forge.Command} object.
     */
    public final void addLeavesPlayCommand(final Command c) {
        addTrigger(c, ZCTrigger.LEAVEFIELD);
    }

    /**
     * <p>removeLeavesPlayCommand.</p>
     *
     * @param c a {@link forge.Command} object.
     */
    public final void removeLeavesPlayCommand(final Command c) {
        removeTrigger(c, ZCTrigger.LEAVEFIELD);
    }

    /**
     * <p>leavesPlay.</p>
     */
    public final void leavesPlay() {
        executeTrigger(ZCTrigger.LEAVEFIELD);
    }

    /**
     * <p>addEquipCommand.</p>
     *
     * @param c a {@link forge.Command} object.
     */
    public final void addEquipCommand(final Command c) {
        equipCommandList.add(c);
    }

    /**
     * <p>removeEquipCommand.</p>
     *
     * @param c a {@link forge.Command} object.
     */
    public final void removeEquipCommand(final Command c) {
        equipCommandList.remove(c);
    }

    /**
     * <p>equip.</p>
     */
    public final void equip() {
        for (Command var : equipCommandList) {
            var.execute();
        }
    }

    /**
     * <p>addUnEquipCommand.</p>
     *
     * @param c a {@link forge.Command} object.
     */
    public final void addUnEquipCommand(final Command c) {
        unEquipCommandList.add(c);
    }

    /**
     * <p>removeUnEquipCommand.</p>
     *
     * @param c a {@link forge.Command} object.
     */
    public final void removeUnEquipCommand(final Command c) {
        unEquipCommandList.remove(c);
    }

    /**
     * <p>unEquip.</p>
     */
    public final void unEquip() {
        for (Command var : unEquipCommandList) {
            var.execute();
        }
    }

    /**
     * <p>addEnchantCommand.</p>
     *
     * @param c a {@link forge.Command} object.
     */
    public final void addEnchantCommand(final Command c) {
        enchantCommandList.add(c);
    }

    /**
     * <p>clearEnchantCommand.</p>
     */
    public final void clearEnchantCommand() {
        enchantCommandList.clear();
    }

    /**
     * <p>enchant.</p>
     */
    public final void enchant() {
        for (Command var : enchantCommandList) {
            var.execute();
        }
    }

    /**
     * <p>addUnEnchantCommand.</p>
     *
     * @param c a {@link forge.Command} object.
     */
    public final void addUnEnchantCommand(final Command c) {
        unEnchantCommandList.add(c);
    }

    /**
     * <p>clearUnEnchantCommand.</p>
     */
    public final void clearUnEnchantCommand() {
        unEnchantCommandList.clear();
    }

    /**
     * <p>unEnchant.</p>
     */
    public final void unEnchant() {
        for (Command var : unEnchantCommandList)
            var.execute();
    }

    /**
     * <p>addUntapCommand.</p>
     *
     * @param c a {@link forge.Command} object.
     */
    public final void addUntapCommand(Command c) {
        untapCommandList.add(c);
    }

    /**
     * <p>addChangeControllerCommand.</p>
     *
     * @param c a {@link forge.Command} object.
     */
    public final void addChangeControllerCommand(Command c) {
        changeControllerCommandList.add(c);
    }

    /**
     * <p>getReplaceMoveToGraveyard.</p>
     *
     * @return a {@link java.util.ArrayList} object.
     */
    public final ArrayList<Command> getReplaceMoveToGraveyard() {
        return replaceMoveToGraveyardCommandList;
    }

    /**
     * <p>addReplaceMoveToGraveyardCommand.</p>
     *
     * @param c a {@link forge.Command} object.
     */
    public final void addReplaceMoveToGraveyardCommand(Command c) {
        replaceMoveToGraveyardCommandList.add(c);
    }

    /**
     * <p>clearReplaceMoveToGraveyardCommandList.</p>
     */
    public final void clearReplaceMoveToGraveyardCommandList() {
        replaceMoveToGraveyardCommandList.clear();
    }

    /**
     * <p>replaceMoveToGraveyard.</p>
     */
    public final void replaceMoveToGraveyard() {
        for (Command var : replaceMoveToGraveyardCommandList) {
            var.execute();
        }
    }

    /**
     * <p>addCycleCommand.</p>
     *
     * @param c a {@link forge.Command} object.
     */
    public final void addCycleCommand(final Command c) {
        cycleCommandList.add(c);
    }

    /**
     * <p>cycle.</p>
     */
    public final void cycle() {
        for (Command var : cycleCommandList) {
            var.execute();
        }
    }

    /**
     * <p>Setter for the field <code>sickness</code>.</p>
     *
     * @param b a boolean.
     */
    public final void setSickness(final boolean b) {
        sickness = b;
    }

    /**
     * <p>hasSickness.</p>
     *
     * @return a boolean.
     */
    public final boolean hasSickness() { return !hasKeyword("Haste") && sickness; }
    public final boolean isSick() { return !hasKeyword("Haste") && sickness && isCreature(); }
    
    /**
     * <p>Setter for the field <code>imageName</code>.</p>
     *
     * @param s a {@link java.lang.String} object.
     */
    public final void setImageName(final String s) {
        imageName = s;
    }

    /**
     * <p>Getter for the field <code>imageName</code>.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    public final String getImageName() {
        if (!imageName.equals("")) {
            return imageName;
        }
        return getName();
    }

    /**
     * <p>Getter for the field <code>owner</code>.</p>
     *
     * @return a {@link forge.Player} object.
     */
    public final Player getOwner() {
        return owner;
    }

    /**
     * TODO write a javadoc for this method.
     *
     * @return a {@link forge.Player} object.
     */
    public final Player getController() {
        if (controllerObjects.size() == 0) {
            return owner;
        }
        Object topController = controllerObjects.get(controllerObjects.size() - 1);
        if (topController instanceof Player) {
            return (Player) topController;
        }
        else {
            return ((Card) topController).getController();
        }
    }

    /**
     * 
     * TODO Write javadoc for this method.
     * @param controllerObject an Object
     */
    public final void addController(final Object controllerObject) {
        Object prevController = controllerObjects.size() == 0 ? owner
                : controllerObjects.get(controllerObjects.size() - 1);
        if (!controllerObject.equals(prevController)) {
            if (controllerObject instanceof Player) {
                for (int i = 0; i < controllerObjects.size(); i++) {
                    if (controllerObjects.get(i) instanceof Player) {
                        controllerObjects.remove(i);
                    }
                }
            }
            controllerObjects.add(controllerObject);
            if (AllZone.getGameAction() != null && prevController != null) {
                AllZone.getGameAction().controllerChangeZoneCorrection(this);
            }


            if (prevController != null) {
                for (Command c : changeControllerCommandList) {
                    c.execute();
                }
            }

            updateObservers();
        }
    }

    /**
     * 
     * TODO Write javadoc for this method.
     * @param controllerObject a Object
     */
    public final void removeController(final Object controllerObject) {
        Object currentController = getController();
        controllerObjects.remove(controllerObject);

        if (!currentController.equals(getController())) {
            AllZone.getGameAction().controllerChangeZoneCorrection(this);

            for (Command c : changeControllerCommandList) {
                c.execute();
            }

            updateObservers();
        }
    }

    /**
     * 
     * TODO Write javadoc for this method.
     */
    public final void clearControllers() {
        controllerObjects.clear();
    }

    /**
     * 
     * TODO Write javadoc for this method.
     * @return an ArrayList<Object>
     */
    public final ArrayList<Object> getControllerObjects() {
        return controllerObjects;
    }

    /**
     * 
     * TODO Write javadoc for this method.
     * @param in an Object
     */
    public final void setControllerObjects(final ArrayList<Object> in) {
        controllerObjects = in;
    }

    /**
     * <p>Setter for the field <code>owner</code>.</p>
     *
     * @param player a {@link forge.Player} object.
     */
    public final void setOwner(final Player player) {
        owner = player;
        this.updateObservers();
    }

    /**
     * <p>Setter for the field <code>controller</code>.</p>
     *
     * @param player a {@link forge.Player} object.
     */ /*
    public void setController(Player player) {
        boolean sameController = controller == null ? false : controller.isPlayer(player);
        controller = player;
        if (null != controller && !sameController) {
            for (Command var : changeControllerCommandList)
                var.execute();
        }
        this.updateObservers();
    }*/

    /**
     * <p>Getter for the field <code>equippedBy</code>.</p>
     *
     * @return a {@link java.util.ArrayList} object.
     */
    public final ArrayList<Card> getEquippedBy() {
        return equippedBy;
    }

    /**
     * <p>Setter for the field <code>equippedBy</code>.</p>
     *
     * @param list a {@link java.util.ArrayList} object.
     */
    public final void setEquippedBy(final ArrayList<Card> list) {
        equippedBy = list;
    }

    /**
     * <p>Getter for the field <code>equipping</code>.</p>
     *
     * @return a {@link java.util.ArrayList} object.
     */
    public final ArrayList<Card> getEquipping() {
        return equipping;
    }

    /**
     * <p>getEquippingCard.</p>
     *
     * @return a {@link forge.Card} object.
     */
    public final Card getEquippingCard() {
        if (equipping.size() == 0) {
            return null;
        }
        return equipping.get(0);
    }

    /**
     * <p>Setter for the field <code>equipping</code>.</p>
     *
     * @param list a {@link java.util.ArrayList} object.
     */
    public final void setEquipping(final ArrayList<Card> list) {
        equipping = list;
    }

    /**
     * <p>isEquipped.</p>
     *
     * @return a boolean.
     */
    public final boolean isEquipped() {
        return !equippedBy.isEmpty();
    }

    /**
     * <p>isEquipping.</p>
     *
     * @return a boolean.
     */
    public final boolean isEquipping() {
        return equipping.size() != 0;
    }

    /**
     * <p>addEquippedBy.</p>
     *
     * @param c a {@link forge.Card} object.
     */
    public final void addEquippedBy(final Card c) {
        equippedBy.add(c);
        this.updateObservers();
    }

    /**
     * <p>removeEquippedBy.</p>
     *
     * @param c a {@link forge.Card} object.
     */
    public final void removeEquippedBy(final Card c) {
        equippedBy.remove(c);
        this.updateObservers();
    }

    /**
     * <p>addEquipping.</p>
     *
     * @param c a {@link forge.Card} object.
     */
    public final void addEquipping(final Card c) {
        equipping.add(c);
        setTimestamp(AllZone.getNextTimestamp());
        this.updateObservers();
    }

    /**
     * <p>removeEquipping.</p>
     *
     * @param c a {@link forge.Card} object.
     */
    public final void removeEquipping(final Card c) {
        equipping.remove(c);
        this.updateObservers();
    }

    /**
     * <p>equipCard.</p>
     * equipment.equipCard(cardToBeEquipped)
     *
     * @param c a {@link forge.Card} object.
     */
    public void equipCard(Card c) {
        addEquipping(c);
        c.addEquippedBy(this);
        this.equip();
    }

    /**
     * <p>unEquipCard.</p>
     *
     * @param c a {@link forge.Card} object.
     */
    public void unEquipCard(Card c) //equipment.unEquipCard(equippedCard);
    {
        this.unEquip();
        equipping.remove(c);
        c.removeEquippedBy(this);

        //Run triggers
        Map<String, Object> runParams = new TreeMap<String, Object>();
        runParams.put("Equipment", this);
        runParams.put("Card", c);
        AllZone.getTriggerHandler().runTrigger("Unequip", runParams);
    }

    /**
     * <p>unEquipAllCards.</p>
     */
    public final void unEquipAllCards() {
        while (equippedBy.size() > 0) {    // while there exists equipment, unequip the first one
            equippedBy.get(0).unEquipCard(this);
        }
    }

    /**
     * <p>Getter for the field <code>enchanting</code>.</p>
     *
     * @return a {@link forge.enchanting} object.
     */
    public final GameEntity getEnchanting() {
        return enchanting;
    }

    /**
     * <p>getEnchantingCard.</p>
     *
     * @return a {@link forge.Card} object.
     */
    public final Card getEnchantingCard() {
        if (enchanting != null && enchanting instanceof Card){
            return (Card)enchanting;
        }
        return null;
    }
    
    /**
     * <p>getEnchantingPlayer.</p>
     *
     * @return a {@link forge.Player} object.
     */
    public final Player getEnchantingPlayer() {
        if (enchanting != null && enchanting instanceof Player){
            return (Player)enchanting;
        }
        return null;
    }

    /**
     * <p>Setter for the field <code>enchanting</code>.</p>
     *
     * @param list a {@link java.util.ArrayList} object.
     */
    public final void setEnchanting(GameEntity e) {
        enchanting = e;
    }

    /**
     * <p>isEnchanting.</p>
     *
     * @return a boolean.
     */
    public final boolean isEnchanting() {
        return enchanting != null;
    }
    
    /**
     * <p>isEnchanting.</p>
     *
     * @return a boolean.
     */
    public final boolean isEnchantingCard() {
        return getEnchantingCard() != null;
    }
    
    /**
     * <p>isEnchanting.</p>
     *
     * @return a boolean.
     */
    public final boolean isEnchantingPlayer() {
        return getEnchantingPlayer() != null;
    }

    /**
     * checks to see if this card is enchanted by an aura with a given name.
     *
     * @param cardName the name of the aura
     * @return true if this card is enchanted by an aura with the given name, false otherwise
     */
    public final boolean isEnchantedBy(final String cardName) {
        ArrayList<Card> allAuras = this.getEnchantedBy();
        for (Card aura : allAuras) {
            if (aura.getName().equals(cardName)) {
                return true;
            }
        }
        return false;
    }

    /**
     * <p>addEnchanting.</p>
     *
     * @param e a {@link forge.GameEntity} object.
     */
    public final void addEnchanting(final GameEntity e) {
        enchanting = e;
        setTimestamp(AllZone.getNextTimestamp());
        this.updateObservers();
    }

    /**
     * <p>removeEnchanting.</p>
     *
     * @param e a {@link forge.GameEntity} object.
     */
    public final void removeEnchanting(final GameEntity e) {
        if (enchanting.equals(e)){
            enchanting = null;
            this.updateObservers();
        }
    }

    /**
     * <p>enchant</p>
     *
     * @param entity a {@link forge.GameEntity} object.
     */
    public final void enchantEntity(final GameEntity entity) {
        addEnchanting(entity);
        entity.addEnchantedBy(this);
        this.enchant();
    }

    /**
     * <p>unEnchant.</p>
     *
     * @param gameEntity a {@link forge.GameEntity} object.
     */
    public final void unEnchantEntity(final GameEntity gameEntity) {
        if (enchanting != null && enchanting.equals(gameEntity)){
            this.unEnchant();
            enchanting = null;
            gameEntity.removeEnchantedBy(this);
        }
    }

    //array size might equal 0, will NEVER be null
    /**
     * <p>getAttachedCards.</p>
     *
     * @return an array of {@link forge.Card} objects.
     */
    public final Card[] getAttachedCardsByMindsDesire() {
        Card[] c = new Card[attachedByMindsDesire.size()];
        attachedByMindsDesire.toArray(c);
        return c;
    }

    /**
     * <p>hasAttachedCards.</p>
     *
     * @return a boolean.
     */
    public final boolean hasAttachedCardsByMindsDesire() {
        return getAttachedCardsByMindsDesire().length != 0;
    }

    /**
     * <p>attachCard.</p>
     *
     * @param c a {@link forge.Card} object.
     */
    public final void attachCardByMindsDesire(final Card c) {
        attachedByMindsDesire.add(c);
        this.updateObservers();
    }

    /**
     * <p>unattachCard.</p>
     *
     * @param c a {@link forge.Card} object.
     */
    public final void unattachCardByMindDesire(final Card c) {
        attachedByMindsDesire.remove(c);
        this.updateObservers();
    }

    /**
     * <p>Setter for the field <code>type</code>.</p>
     *
     * @param a a {@link java.util.ArrayList} object.
     */
    public final void setType(final ArrayList<String> a) {
        type = new ArrayList<String>(a);
    }

    /**
     * <p>addType.</p>
     *
     * @param a a {@link java.lang.String} object.
     */
    public final void addType(final String a) {
        type.add(a);
    }

    /**
     * <p>removeType.</p>
     *
     * @param a a {@link java.lang.String} object.
     */
    public final void removeType(final String a) {
        type.remove(a);
    }

    /**
     * <p>Getter for the field <code>type</code>.</p>
     *
     * @return a {@link java.util.ArrayList} object.
     */
    public final ArrayList<String> getType() {

    	// see if type changes are in effect
    	if (!changedCardTypes.isEmpty()) {

	    	ArrayList<String> newType = new ArrayList<String>(type);
	    	ArrayList<Card_Type> types = changedCardTypes;
	    	Collections.sort(types);  // sorts types by timeStamp

	    	for (Card_Type ct : types) {
	    		ArrayList<String> removeTypes = new ArrayList<String>();
	    		if (ct.getRemoveType() != null) {
	    		    removeTypes.addAll(ct.getRemoveType());
	    		}
	    		//remove old types
	    		for (int i = 0; i < newType.size(); i++) {
	    			String t = newType.get(i);
	    			if (ct.isRemoveSuperTypes() && CardUtil.isASuperType(t)) {
                        removeTypes.add(t);
                    }
	    			if (ct.isRemoveCardTypes() && CardUtil.isACardType(t)) {
                        removeTypes.add(t);
                    }
	    			if (ct.isRemoveSubTypes() && CardUtil.isASubType(t)) {
                        removeTypes.add(t);
                    }
	    			if (ct.isRemoveCreatureTypes() && (CardUtil.isACreatureType(t)
	    			        || t.equals("AllCreatureTypes")))
	    			{
	    				removeTypes.add(t);
	    			}
	    		}
	    		newType.removeAll(removeTypes);
	    		//add new types
	    		if (ct.getType() != null) {
	    		    newType.addAll(ct.getType());
	    		}

	    	}

	    	return newType;
    	}

    	//nothing changed
        return new ArrayList<String>(type);
    }
    
    public void setChangedCardTypes(ArrayList<Card_Type> types) {
        changedCardTypes = types;
    }
    
    public ArrayList<Card_Type> getChangedCardTypes() {
        return changedCardTypes;
    }
        
    public void addChangedCardTypes(ArrayList<String> types, ArrayList<String> removeTypes, boolean removeSuperTypes, 
            boolean removeCardTypes, boolean removeSubTypes, boolean removeCreatureTypes, long timestamp) {
   
    	changedCardTypes.add(new Card_Type(types, removeTypes, removeSuperTypes, removeCardTypes, removeSubTypes, removeCreatureTypes, 
    	        timestamp));
    }
    
    public void addChangedCardTypes(String[] types, final String[] removeTypes, boolean removeSuperTypes, boolean removeCardTypes, 
    		boolean removeSubTypes, boolean removeCreatureTypes, long timestamp) {
        ArrayList<String> typeList = null;
        ArrayList<String> removeTypeList = null;
        if(types != null) {
            typeList = new ArrayList<String>(Arrays.asList(types));
        }

        if(removeTypes  != null) {
            removeTypeList = new ArrayList<String>(Arrays.asList(removeTypes));
        }
        
    	addChangedCardTypes(typeList, removeTypeList, removeSuperTypes, removeCardTypes, removeSubTypes, removeCreatureTypes, timestamp);
    }
    
    public void removeChangedCardTypes(long timestamp) {
    	for (int i = 0; i < changedCardTypes.size(); i++) {
    		Card_Type cardT = changedCardTypes.get(i);
    		if (cardT.getTimestamp() == timestamp) {
                changedCardTypes.remove(cardT);
            }
    	}
    }

    /**
     * <p>clearAllTypes.</p>
     *
     * @return a {@link java.util.ArrayList} object.
     */
    public final ArrayList<String> clearAllTypes() {
        ArrayList<String> originalTypes = new ArrayList<String>();
        originalTypes.addAll(type);
        type.clear();
        return originalTypes;
    }

    /**
     * <p>Setter for the field <code>prevType</code>.</p>
     *
     * @param a a {@link java.util.ArrayList} object.
     */
    public final void setPrevType(final ArrayList<String> a) {
        prevType = new ArrayList<String>(a);
    }

    /**
     * <p>addPrevType.</p>
     *
     * @param a a {@link java.lang.String} object.
     */
    public final void addPrevType(final String a) {
        prevType.add(a);
    }

    /**
     * <p>removePrevType.</p>
     *
     * @param a a {@link java.lang.String} object.
     */
    public final void removePrevType(final String a) {
        prevType.remove(a);
    }

    /**
     * <p>Getter for the field <code>prevType</code>.</p>
     *
     * @return a {@link java.util.ArrayList} object.
     */
    public final ArrayList<String> getPrevType() {
        return new ArrayList<String>(prevType);
    }

    //values that are printed on card
    /**
     * <p>Getter for the field <code>baseLoyalty</code>.</p>
     *
     * @return a int.
     */
    public final int getBaseLoyalty() {
        return baseLoyalty;
    }

    //values that are printed on card
    /**
     * <p>Setter for the field <code>baseLoyalty</code>.</p>
     *
     * @param n a int.
     */
    public final void setBaseLoyalty(final int n) {
        baseLoyalty = n;
    }

    //values that are printed on card
    /**
     * <p>Getter for the field <code>baseAttack</code>.</p>
     *
     * @return a int.
     */
    public final int getBaseAttack() {
        return baseAttack;
    }

    /**
     * <p>Getter for the field <code>baseDefense</code>.</p>
     *
     * @return a int.
     */
    public final int getBaseDefense() {
        return baseDefense;
    }

    //values that are printed on card
    /**
     * <p>Setter for the field <code>baseAttack</code>.</p>
     *
     * @param n a int.
     */
    public final void setBaseAttack(final int n) {
        baseAttack = n;
    }

    /**
     * <p>Setter for the field <code>baseDefense</code>.</p>
     *
     * @param n a int.
     */
    public final void setBaseDefense(final int n) {
        baseDefense = n;
    }

    //values that are printed on card
    /**
     * <p>Getter for the field <code>baseAttackString</code>.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    public final String getBaseAttackString() {
        return (null == baseAttackString) ? "" + getBaseAttack() : baseAttackString;
    }

    /**
     * <p>Getter for the field <code>baseDefenseString</code>.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    public final String getBaseDefenseString() {
        return (null == baseDefenseString) ? "" + getBaseDefense() : baseDefenseString;
    }

    //values that are printed on card
    /**
     * <p>Setter for the field <code>baseAttackString</code>.</p>
     *
     * @param s a {@link java.lang.String} object.
     */
    public final void setBaseAttackString(final String s) {
        baseAttackString = s;
    }

    /**
     * <p>Setter for the field <code>baseDefenseString</code>.</p>
     *
     * @param s a {@link java.lang.String} object.
     */
    public final void setBaseDefenseString(String s) {
        baseDefenseString = s;
    }
    
    public void setNewPT(final ArrayList<CardPowerToughness> pt) {
        newPT = pt;
    }
    
    public ArrayList<CardPowerToughness> getNewPT() {
        return newPT;
    }
    
    public int getSetPower() {
    	if (newPT.isEmpty())
    		return -1;
    	
    	CardPowerToughness latestPT = getLatestPT();
    	
    	return latestPT.getPower();
    }
    
    public int getSetToughness() {
    	if (newPT.isEmpty())
    		return -1;
    	
    	CardPowerToughness latestPT = getLatestPT();
    	
    	return latestPT.getToughness();
    }
    
    public CardPowerToughness getLatestPT() {
    	CardPowerToughness latestPT = new CardPowerToughness(-1,-1,0);
    	long max = 0;
    	
    	for (CardPowerToughness pt : newPT) {
    		if (pt.getTimestamp() >= max) {
    			max = pt.getTimestamp();
    			latestPT = pt;
    		}
    	}
    	
    	return latestPT;
    }
    
    public void addNewPT(int power, int toughness, long timestamp) {
    	newPT.add(new CardPowerToughness(power, toughness, timestamp));
    }
    
    public void removeNewPT(long timestamp) {
    	for (int i = 0; i < newPT.size(); i++) {
    		CardPowerToughness cardPT = newPT.get(i);
    		if (cardPT.getTimestamp() == timestamp)
    			newPT.remove(cardPT);
    	}
    }
    
    public int getCurrentPower() {
    	int total = getBaseAttack();
        int setPower = getSetPower();
        if(setPower != -1)
        	total = setPower;
        
        return total;
    }

    /**
     * <p>getUnswitchedAttack.</p>
     *
     * @return a int.
     */
    public final int getUnswitchedAttack() {
        int total = getCurrentPower();

        total += getTempAttackBoost() + getSemiPermanentAttackBoost()
                + getCounters(Counters.P1P1) + getCounters(Counters.P1P2)
                + getCounters(Counters.P1P0) - getCounters(Counters.M1M1)
                + (2 * getCounters(Counters.P2P2) - (2 * getCounters(Counters.M2M1))
                - (2 * getCounters(Counters.M2M2)) - getCounters(Counters.M1M0));
        return total;
    }

    /**
     * <p>getNetAttack.</p>
     *
     * @return a int.
     */
    public final int getNetAttack() {
        if (this.getAmountOfKeyword("CARDNAME's power and toughness are switched") % 2 != 0) {
            return getUnswitchedDefense();
        } else {
            return getUnswitchedAttack();
        }
    }

    /**
     * 
     * TODO Write javadoc for this method.
     * @return an int
     */
    public final int getCurrentToughness() {
    	int total = getBaseDefense();

        int setToughness = getSetToughness();
        if (setToughness != -1) {
            total = setToughness;
        }

        return total;
    }

    /**
     * <p>getUnswitchedDefense.</p>
     *
     * @return a int.
     */
    public final int getUnswitchedDefense() {
        int total = getCurrentToughness();

        total += getTempDefenseBoost() + getSemiPermanentDefenseBoost()
                + getCounters(Counters.P1P1) + (2 * getCounters(Counters.P1P2))
                - getCounters(Counters.M1M1) + getCounters(Counters.P0P1)
                - (2 * getCounters(Counters.M0M2))
                + (2 * getCounters(Counters.P2P2)) - getCounters(Counters.M0M1)
                - getCounters(Counters.M2M1) - (2 * getCounters(Counters.M2M2));
        return total;
    }

    /**
     * <p>getNetDefense.</p>
     *
     * @return a int.
     */
    public final int getNetDefense() {
        if (this.getAmountOfKeyword("CARDNAME's power and toughness are switched") % 2 != 0) {
            return getUnswitchedAttack();
        } else {
            return getUnswitchedDefense();
        }
    }

    //How much combat damage does the card deal
    /**
     * <p>getNetCombatDamage.</p>
     *
     * @return a int.
     */
    public final int getNetCombatDamage() {
        if (hasKeyword("CARDNAME assigns no combat damage")) {
            return 0;
        }
        
        if (AllZoneUtil.isCardInPlay("Doran, the Siege Tower")) {
            return getNetDefense();
        }
        return getNetAttack();
    }

    /**
     * <p>Setter for the field <code>randomPicture</code>.</p>
     *
     * @param n a int.
     */
    public final void setRandomPicture(final int n) {
        randomPicture = n;
    }

    /**
     * <p>Getter for the field <code>randomPicture</code>.</p>
     *
     * @return a int.
     */
    public final int getRandomPicture() {
        return randomPicture;
    }

    /**
     * <p>addMultiKickerMagnitude.</p>
     *
     * @param n a int.
     */
    public final void addMultiKickerMagnitude(final int n) {
        multiKickerMagnitude += n;
    }

    /**
     * <p>Setter for the field <code>multiKickerMagnitude</code>.</p>
     *
     * @param n a int.
     */
    public final void setMultiKickerMagnitude(final int n) {
        multiKickerMagnitude = n;
    }

    /**
     * <p>Getter for the field <code>multiKickerMagnitude</code>.</p>
     *
     * @return a int.
     */
    public final int getMultiKickerMagnitude() {
        return multiKickerMagnitude;
    }

    /**
     * <p>addReplicateMagnitude.</p>
     *
     * @param n a int.
     */
    public final void addReplicateMagnitude(final int n) {
        replicateMagnitude += n;
    }

    /**
     * <p>Setter for the field <code>replicateMagnitude</code>.</p>
     *
     * @param n a int.
     */
    public final void setReplicateMagnitude(final int n) {
        replicateMagnitude = n;
    }

    /**
     * <p>Getter for the field <code>replicateMagnitude</code>.</p>
     *
     * @return a int.
     */
    public final int getReplicateMagnitude() {
        return replicateMagnitude;
    }

    //for cards like Giant Growth, etc.
    /**
     * <p>Getter for the field <code>tempAttackBoost</code>.</p>
     *
     * @return a int.
     */
    public final int getTempAttackBoost() {
        return tempAttackBoost;
    }

    /**
     * <p>Getter for the field <code>tempDefenseBoost</code>.</p>
     *
     * @return a int.
     */
    public final int getTempDefenseBoost() {
        return tempDefenseBoost;
    }

    /**
     * <p>addTempAttackBoost.</p>
     *
     * @param n a int.
     */
    public final void addTempAttackBoost(final int n) {
        tempAttackBoost += n;
    }

    /**
     * <p>addTempDefenseBoost.</p>
     *
     * @param n a int.
     */
    public final void addTempDefenseBoost(final int n) {
        tempDefenseBoost += n;
    }

    /**
     * <p>Setter for the field <code>tempAttackBoost</code>.</p>
     *
     * @param n a int.
     */
    public final void setTempAttackBoost(final int n) {
        tempAttackBoost = n;
    }

    /**
     * <p>Setter for the field <code>tempDefenseBoost</code>.</p>
     *
     * @param n a int.
     */
    public final void setTempDefenseBoost(final int n) {
        tempDefenseBoost = n;
    }

    //for cards like Glorious Anthem, etc.
    /**
     * <p>Getter for the field <code>semiPermanentAttackBoost</code>.</p>
     *
     * @return a int.
     */
    public final int getSemiPermanentAttackBoost() {
        return semiPermanentAttackBoost;
    }

    /**
     * <p>Getter for the field <code>semiPermanentDefenseBoost</code>.</p>
     *
     * @return a int.
     */
    public final int getSemiPermanentDefenseBoost() {
        return semiPermanentDefenseBoost;
    }

    /**
     * <p>addSemiPermanentAttackBoost.</p>
     *
     * @param n a int.
     */
    public final void addSemiPermanentAttackBoost(final int n) {
        semiPermanentAttackBoost += n;
    }

    /**
     * <p>addSemiPermanentDefenseBoost.</p>
     *
     * @param n a int.
     */
    public final void addSemiPermanentDefenseBoost(final int n) {
        semiPermanentDefenseBoost += n;
    }

    /**
     * <p>Setter for the field <code>semiPermanentAttackBoost</code>.</p>
     *
     * @param n a int.
     */
    public final void setSemiPermanentAttackBoost(final int n) {
        semiPermanentAttackBoost = n;
    }

    /**
     * <p>Setter for the field <code>semiPermanentDefenseBoost</code>.</p>
     *
     * @param n a int.
     */
    public final void setSemiPermanentDefenseBoost(final int n) {
        semiPermanentDefenseBoost = n;
    }

    /**
     * <p>isUntapped.</p>
     *
     * @return a boolean.
     */
    public final boolean isUntapped() {
        return !tapped;
    }

    /**
     * <p>isTapped.</p>
     *
     * @return a boolean.
     */
    public final boolean isTapped() {
        return tapped;
    }

    /**
     * <p>Setter for the field <code>tapped</code>.</p>
     *
     * @param b a boolean.
     */
    public final void setTapped(final boolean b) {
        tapped = b;
        updateObservers();
    }

    /**
     * <p>tap.</p>
     */
    public final void tap() {
        if (isUntapped()) {
            //Run triggers
            Map<String, Object> runParams = new TreeMap<String, Object>();
            runParams.put("Card", this);
            AllZone.getTriggerHandler().runTrigger("Taps", runParams);
        }
        setTapped(true);
    }

    /**
     * <p>untap.</p>
     */
    public final void untap() {
        if (isTapped()) {
            //Run triggers
            Map<String, Object> runParams = new TreeMap<String, Object>();
            runParams.put("Card", this);
            AllZone.getTriggerHandler().runTrigger("Untaps", runParams);

        }

        for (Command var : untapCommandList) {
            var.execute();
        }

        setTapped(false);
    }

    /**
     * <p>isUnCastable.</p>
     *
     * @return a boolean.
     */
    public final boolean isUnCastable() {
        return unCastable;
    }

    /**
     * <p>Setter for the field <code>unCastable</code>.</p>
     *
     * @param b a boolean.
     */
    public final void setUnCastable(final boolean b) {
        unCastable = b;
        updateObservers();
    }

    //keywords are like flying, fear, first strike, etc...
    /**
     * <p>getKeyword.</p>
     *
     * @return a {@link java.util.ArrayList} object.
     */
    public final ArrayList<String> getKeyword() {
        ArrayList<String> keywords = getUnhiddenKeyword();
        ArrayList<String> a4 = new ArrayList<String>(getHiddenExtrinsicKeyword());
        keywords.addAll(a4);

        return keywords;
    }

    public int getKeywordAmount(final String keyword) {
        int res = 0;
        for (String k : getKeyword()) {
            if (k.equals(keyword)) {
                res++;
            }
        }

        return res;
    }
    
    public void setChangedCardKeywords(ArrayList<Card_Keywords> kw) {
        changedCardKeywords = kw;
    }
    
    public ArrayList<Card_Keywords> getChangedCardKeywords() {
        return changedCardKeywords;
    }
        
    public void addChangedCardKeywords(ArrayList<String> keywords, ArrayList<String> removeKeywords, boolean removeAllKeywords, 
            long timestamp) {
   
        changedCardKeywords.add(new Card_Keywords(keywords, removeKeywords, removeAllKeywords, timestamp));
    }
    
    public void addChangedCardKeywords(String[] keywords, String[] removeKeywords, boolean removeAllKeywords, long timestamp) {
        ArrayList<String> keywordsList = null;
        ArrayList<String> removeKeywordsList = null;
        if(keywords != null) {
            keywordsList = new ArrayList<String>(Arrays.asList(keywords));
        }

        if(removeKeywords  != null) {
            removeKeywordsList = new ArrayList<String>(Arrays.asList(removeKeywords));
        }
        
        addChangedCardKeywords(keywordsList, removeKeywordsList, removeAllKeywords, timestamp);
    }
    
    public void removeChangedCardKeywords(long timestamp) {
        for (int i = 0; i < changedCardKeywords.size(); i++) {
            Card_Keywords cardK = changedCardKeywords.get(i);
            if (cardK.getTimestamp() == timestamp) {
                changedCardKeywords.remove(cardK);
            }
        }
    }

    // Hidden keywords will be left out
    /**
     * <p>getUnhiddenKeyword.</p>
     *
     * @return a {@link java.util.ArrayList} object.
     */
    public final ArrayList<String> getUnhiddenKeyword() {
        ArrayList<String> keywords = new ArrayList<String>(getIntrinsicKeyword());
        ArrayList<String> a2 = new ArrayList<String>(getExtrinsicKeyword());
        keywords.addAll(a2);

        // see if keyword changes are in effect
        if (!changedCardKeywords.isEmpty()) {

            ArrayList<Card_Keywords> newKeywords = changedCardKeywords;
            Collections.sort(newKeywords);  // sorts newKeywords by timeStamp

            for (Card_Keywords ck : newKeywords) {
                
                if(ck.isRemoveAllKeywords()) {
                    keywords.clear();
                } else if (ck.getRemoveKeywords() != null) {
                    keywords.removeAll(ck.getRemoveKeywords());
                }

                if (ck.getKeywords() != null) {
                    keywords.addAll(ck.getKeywords());
                }

            }
        }

        return keywords;
    }

    /**
     * <p>getIntrinsicAbilities.</p>
     *
     * @return a {@link java.util.ArrayList} object.
     */
    public final ArrayList<String> getIntrinsicAbilities() {
        return intrinsicAbility;
    }

    /**
     * <p>Getter for the field <code>intrinsicKeyword</code>.</p>
     *
     * @return a {@link java.util.ArrayList} object.
     */
    public final ArrayList<String> getIntrinsicKeyword() {
        return new ArrayList<String>(intrinsicKeyword);
    }

    /**
     * <p>clearIntrinsicKeyword.</p>
     */
    public final void clearIntrinsicKeyword() {
        intrinsicKeyword.clear();
    }

    /**
     * <p>Setter for the field <code>intrinsicKeyword</code>.</p>
     *
     * @param a a {@link java.util.ArrayList} object.
     */
    public final void setIntrinsicKeyword(final ArrayList<String> a) {
        intrinsicKeyword = new ArrayList<String>(a);
    }

    /**
     * <p>clearAllKeywords.</p>
     */
    public final void clearAllKeywords() {
        intrinsicKeyword.clear();
        extrinsicKeyword.clear();
        hiddenExtrinsicKeyword.clear();        //Hidden keywords won't be displayed on the card
    }

    /**
     * <p>setIntrinsicAbilities.</p>
     *
     * @param a a {@link java.util.ArrayList} object.
     */
    public final void setIntrinsicAbilities(final ArrayList<String> a) {
        intrinsicAbility = new ArrayList<String>(a);
    }

    /**
     * <p>addIntrinsicKeyword.</p>
     *
     * @param s a {@link java.lang.String} object.
     */
    public final void addIntrinsicKeyword(final String s) {
        if (s.trim().length() != 0) {
            intrinsicKeyword.add(s);
        //intrinsicKeyword.add((getName().trim().length()== 0 ? s :s.replaceAll(getName(), "CARDNAME")));
        }
    }

    /**
     * <p>addIntrinsicAbility.</p>
     *
     * @param s a {@link java.lang.String} object.
     */
    public void addIntrinsicAbility(String s) {
        if (s.trim().length() != 0)
            intrinsicAbility.add(s);
    }

    /**
     * <p>addNonStackingIntrinsicKeyword.</p>
     *
     * @param s a {@link java.lang.String} object.
     */
    public final void addNonStackingIntrinsicKeyword(String s) {
        if (!getIntrinsicKeyword().contains(s) && s.trim().length() != 0) {
                intrinsicKeyword.add((getName().trim().length() == 0 ? s : s.replaceAll(getName(), "CARDNAME")));
        }
    }

    /**
     * <p>removeIntrinsicKeyword.</p>
     *
     * @param s a {@link java.lang.String} object.
     */
    public final void removeIntrinsicKeyword(String s) {
        intrinsicKeyword.remove(s);
    }

    /**
     * <p>getIntrinsicKeywordSize.</p>
     *
     * @return a int.
     */
    public final int getIntrinsicKeywordSize() {
        return intrinsicKeyword.size();
    }

    /**
     * <p>Getter for the field <code>extrinsicKeyword</code>.</p>
     *
     * @return a {@link java.util.ArrayList} object.
     */
    public ArrayList<String> getExtrinsicKeyword() {
        return new ArrayList<String>(extrinsicKeyword);
    }

    /**
     * <p>Setter for the field <code>extrinsicKeyword</code>.</p>
     *
     * @param a a {@link java.util.ArrayList} object.
     */
    public void setExtrinsicKeyword(final ArrayList<String> a) {
        extrinsicKeyword = new ArrayList<String>(a);
    }

    /**
     * <p>addExtrinsicKeyword.</p>
     *
     * @param s a {@link java.lang.String} object.
     */
    public void addExtrinsicKeyword(final String s) {
        //if(!hasKeyword(s)){
        if (s.startsWith("HIDDEN")) {
            addHiddenExtrinsicKeyword(s);
        }
        else {
            extrinsicKeyword.add(s);
        //extrinsicKeyword.add((getName().trim().length()==0 ? s :s.replaceAll(getName(), "CARDNAME")));
        //}
        }
    }

    /**
     * <p>addStackingExtrinsicKeyword.</p>
     *
     * @param s a {@link java.lang.String} object.
     */
    public  void addStackingExtrinsicKeyword(final String s) {
        if (s.startsWith("HIDDEN")) {
            addHiddenExtrinsicKeyword(s);
        } else {
            extrinsicKeyword.add(s);
        }
    }

    /**
     * <p>removeExtrinsicKeyword.</p>
     *
     * @param s a {@link java.lang.String} object.
     */
    public void removeExtrinsicKeyword(final String s) {
        if (s.startsWith("HIDDEN")) {
            removeHiddenExtrinsicKeyword(s);
        } else {
            extrinsicKeyword.remove(s);
        }
    }

    /**
     * <p>getExtrinsicKeywordSize.</p>
     *
     * @return a int.
     */
    public int getExtrinsicKeywordSize() {
        return extrinsicKeyword.size();
    }

    /**
     * <p>Getter for the field <code>prevIntrinsicKeyword</code>.</p>
     *
     * @return a {@link java.util.ArrayList} object.
     */
    public ArrayList<String> getPrevIntrinsicKeyword() {
        return new ArrayList<String>(prevIntrinsicKeyword);
    }

    /**
     * <p>Setter for the field <code>prevIntrinsicKeyword</code>.</p>
     *
     * @param a a {@link java.util.ArrayList} object.
     */
    public void setPrevIntrinsicKeyword(ArrayList<String> a) {
        prevIntrinsicKeyword = new ArrayList<String>(a);
        this.updateObservers();
    }

    /**
     * <p>addPrevIntrinsicKeyword.</p>
     *
     * @param s a {@link java.lang.String} object.
     */
    public void addPrevIntrinsicKeyword(String s) {
        prevIntrinsicKeyword.add(s);
    }

    /**
     * <p>removePrevIntrinsicKeyword.</p>
     *
     * @param s a {@link java.lang.String} object.
     */
    public void removePrevIntrinsicKeyword(String s) {
        prevIntrinsicKeyword.remove(s);
        this.updateObservers();
    }

    /**
     * <p>getPrevIntrinsicKeywordSize.</p>
     *
     * @return a int.
     */
    public int getPrevIntrinsicKeywordSize() {
        return prevIntrinsicKeyword.size();
    }

    // Hidden Keywords will be returned without the indicator HIDDEN
    /**
     * <p>getHiddenExtrinsicKeyword.</p>
     *
     * @return a {@link java.util.ArrayList} object.
     */
    public ArrayList<String> getHiddenExtrinsicKeyword() {
        ArrayList<String> keywords = new ArrayList<String>();
        for (int i = 0; i < hiddenExtrinsicKeyword.size(); i++) {
            String keyword = hiddenExtrinsicKeyword.get(i);
            keywords.add(keyword.substring(7));
        }
        return keywords;
    }

    /**
     * <p>addHiddenExtrinsicKeyword.</p>
     *
     * @param s a {@link java.lang.String} object.
     */
    public void addHiddenExtrinsicKeyword(String s) {
        hiddenExtrinsicKeyword.add(s);
    }

    /**
     * <p>removeHiddenExtrinsicKeyword.</p>
     *
     * @param s a {@link java.lang.String} object.
     */
    public void removeHiddenExtrinsicKeyword(String s) {
        hiddenExtrinsicKeyword.remove(s);
        //this.updateObservers();
    }
    
    /**
     * <p>setStaticAbilityStrings.</p>
     *
     * @param a a {@link java.util.ArrayList} object.
     */
    public void setStaticAbilityStrings(ArrayList<String> a) {
    	staticAbilityStrings = new ArrayList<String>(a);
    }
    
    public ArrayList<String> getStaticAbilityStrings() {
    	return staticAbilityStrings;
    }

    /**
     * <p>addStaticAbilityStrings.</p>
     *
     * @param s a {@link java.lang.String} object.
     */
    public void addStaticAbilityString(String s) {
        if (s.trim().length() != 0)
        	staticAbilityStrings.add(s);
    }
    
    public void setStaticAbilities(ArrayList<StaticAbility> a) {
    	staticAbilities = new ArrayList<StaticAbility>(a);
    }
    
    public ArrayList<StaticAbility> getStaticAbilities() {
        return new ArrayList<StaticAbility>(staticAbilities);
    }
    
    public void addStaticAbility(String s) {
    	
        if (s.trim().length() != 0) {
        	StaticAbility stAb = new StaticAbility(s,this);
        	staticAbilities.add(stAb);
        }
    }

    /**
     * <p>isPermanent.</p>
     *
     * @return a boolean.
     */
    public boolean isPermanent() {
        return !(isInstant() || isSorcery() || isImmutable());
    }

    /**
     * <p>isSpell.</p>
     *
     * @return a boolean.
     */
    public boolean isSpell() {
        return (isInstant() || isSorcery() || (isAura() && !AllZoneUtil.getCardsIn(Zone.Battlefield).contains(this)));
    }

    /**
     * <p>isCreature.</p>
     *
     * @return a boolean.
     */
    public boolean isCreature() {
        return typeContains("Creature");
    }

    /**
     * <p>isWall.</p>
     *
     * @return a boolean.
     */
    public boolean isWall() {
        return typeContains("Wall");
    }

    /**
     * <p>isBasicLand.</p>
     *
     * @return a boolean.
     */
    public boolean isBasicLand() {
        return typeContains("Basic");
    }

    /**
     * <p>isLand.</p>
     *
     * @return a boolean.
     */
    public boolean isLand() {
        return typeContains("Land");
    }

    /**
     * <p>isSorcery.</p>
     *
     * @return a boolean.
     */
    public boolean isSorcery() {
        return typeContains("Sorcery");
    }

    /**
     * <p>isInstant.</p>
     *
     * @return a boolean.
     */
    public boolean isInstant() {
        return typeContains("Instant");
    }

    /**
     * <p>isArtifact.</p>
     *
     * @return a boolean.
     */
    public boolean isArtifact() {
        return typeContains("Artifact");
    }

    /**
     * <p>isEquipment.</p>
     *
     * @return a boolean.
     */
    public boolean isEquipment() {
        return typeContains("Equipment");
    }

    /**
     * <p>isPlaneswalker.</p>
     *
     * @return a boolean.
     */
    public boolean isPlaneswalker() {
        return typeContains("Planeswalker");
    }

    /**
     * <p>isEmblem.</p>
     *
     * @return a boolean.
     */
    public boolean isEmblem() {
        return typeContains("Emblem");
    }

    /**
     * <p>isTribal.</p>
     *
     * @return a boolean.
     */
    public boolean isTribal() {
        return typeContains("Tribal");
    }

    /**
     * <p>isSnow.</p>
     *
     * @return a boolean.
     */
    public boolean isSnow() {
        return typeContains("Snow");
    }

    //global and local enchantments
    /**
     * <p>isEnchantment.</p>
     *
     * @return a boolean.
     */
    public boolean isEnchantment() {
        return typeContains("Enchantment");
    }

    /**
     * <p>isAura.</p>
     *
     * @return a boolean.
     */
    public boolean isAura() {
        return typeContains("Aura");
    }

    /**
     * <p>isGlobalEnchantment.</p>
     *
     * @return a boolean.
     */
    public boolean isGlobalEnchantment() {
        return typeContains("Enchantment") && (!isAura());
    }

    private boolean typeContains(String s) {
        Iterator<?> it = this.getType().iterator();
        while (it.hasNext())
            if (it.next().toString().startsWith(s)) return true;

        return false;
    }

    /**
     * <p>Setter for the field <code>uniqueNumber</code>.</p>
     *
     * @param n a int.
     */
    public void setUniqueNumber(int n) {
        uniqueNumber = n;
        this.updateObservers();
    }

    /**
     * <p>Getter for the field <code>uniqueNumber</code>.</p>
     *
     * @return a int.
     */
    public int getUniqueNumber() {
        return uniqueNumber;
    }

    /**
     * <p>Setter for the field <code>value</code>.</p>
     *
     * @param n a long.
     */
    public void setValue(long n) {
        value = n;
    }

    /**
     * <p>Getter for the field <code>value</code>.</p>
     *
     * @return a long.
     */
    public long getValue() {
        return value;
    }

    /** {@inheritDoc} */
    @Override
    public int compareTo(Card that) {
        /*
         * Return a negative integer of this < that,
         * a positive integer if this > that,
         * and zero otherwise.
         */

        if (that == null) {
            /*
             * "Here we can arbitrarily decide that all non-null Cards are
             * `greater than' null Cards. It doesn't really matter what we
             * return in this case, as long as it is consistent. I rather think
             * of null as being lowly."  --Braids
             */
            return +1;
        }
        else if (getUniqueNumber() > that.getUniqueNumber()) {
            return +1;
        }
        else if (getUniqueNumber() < that.getUniqueNumber()) {
            return -1;
        }
        else {
            return 0;
        }
    }

    /** {@inheritDoc} */
    @Override
    public boolean equals(Object o) {
        if (o instanceof Card) {
            Card c = (Card) o;
            int a = getUniqueNumber();
            int b = c.getUniqueNumber();
            return (a == b);
        }
        return false;
    }

    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        return getUniqueNumber();
    }
    
    /** {@inheritDoc} */
    @Override
    public String toString() {
        return this.getName() + " (" + this.getUniqueNumber() + ")";
    }

    /**
     * <p>hasFlashback.</p>
     *
     * @return a boolean.
     */
    public boolean hasFlashback() {
        return flashback;
    }

    /**
     * <p>Setter for the field <code>flashback</code>.</p>
     *
     * @param b a boolean.
     */
    public void setFlashback(boolean b) {
        flashback = b;
    }

    /**
     * <p>hasUnearth.</p>
     *
     * @return a boolean.
     */
    public boolean hasUnearth() {
        return unearth;
    }

    /**
     * <p>Setter for the field <code>unearth</code>.</p>
     *
     * @param b a boolean.
     */
    public void setUnearth(boolean b) {
        unearth = b;
    }

    /**
     * <p>isUnearthed.</p>
     *
     * @return a boolean.
     */
    public boolean isUnearthed() {
        return unearthed;
    }

    /**
     * <p>Setter for the field <code>unearthed</code>.</p>
     *
     * @param b a boolean.
     */
    public void setUnearthed(boolean b) {
        unearthed = b;
    }

    /**
     * <p>hasMadness.</p>
     *
     * @return a boolean.
     */
    public boolean hasMadness() {
        return madness;
    }

    /**
     * <p>Setter for the field <code>madness</code>.</p>
     *
     * @param b a boolean.
     */
    public void setMadness(boolean b) {
        madness = b;
    }

    /**
     * <p>Getter for the field <code>madnessCost</code>.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String getMadnessCost() {
        return madnessCost;
    }

    /**
     * <p>Setter for the field <code>madnessCost</code>.</p>
     *
     * @param cost a {@link java.lang.String} object.
     */
    public void setMadnessCost(String cost) {
        madnessCost = cost;
    }

    /**
     * <p>hasSuspend.</p>
     *
     * @return a boolean.
     */
    public boolean hasSuspend() {
        return suspend;
    }

    /**
     * <p>Setter for the field <code>suspend</code>.</p>
     *
     * @param b a boolean.
     */
    public void setSuspend(boolean b) {
        suspend = b;
    }

    /**
     * <p>wasSuspendCast.</p>
     *
     * @return a boolean.
     */
    public boolean wasSuspendCast() {
        return suspendCast;
    }

    /**
     * <p>Setter for the field <code>suspendCast</code>.</p>
     *
     * @param b a boolean.
     */
    public void setSuspendCast(boolean b) {
        suspendCast = b;
    }

    /**
     * <p>Setter for the field <code>kicked</code>.</p>
     *
     * @param b a boolean.
     */
    public void setKicked(boolean b) {
        kicked = b;
    }

    /**
     * <p>isKicked.</p>
     *
     * @return a boolean.
     */
    public boolean isKicked() {
        return kicked;
    }

    public boolean isPhasedOut() {
        return phasedOut;
    }

    public void setPhasedOut(boolean phasedOut) {
        this.phasedOut = phasedOut;
    }
    
    public void phase(){
        this.phase(true);
    }
    
    public void phase(boolean direct){
        boolean phasingIn = this.isPhasedOut();
        
        if (!this.switchPhaseState()){
            // Switch Phase State returns False if the Permanent can't Phase Out
            return;
        }
        
        if (!phasingIn){
            this.setDirectlyPhasedOut(direct);
        }
        
        for (Card eq : this.getEquippedBy()){
            if (eq.isPhasedOut() == phasingIn){
                eq.phase(false);
            }
        }
        
        for (Card aura : this.getEnchantedBy()){
            if (aura.isPhasedOut() == phasingIn){
                aura.phase(false);
            }
        }
    }
  
    private boolean switchPhaseState(){
        if (!this.phasedOut && this.hasKeyword("CARDNAME can't phase out.")){
            return false;
        }
        
        this.phasedOut = !this.phasedOut;
        if (this.phasedOut && isToken()){
            // 702.23k Phased-out tokens cease to exist as a state-based action. See rule 704.5d. 
            // 702.23d The phasing event doesn't actually cause a permanent to change zones or control, 
            // even though it's treated as though it's not on the battlefield and not under its controller's control while it's phased out. 
            // Zone-change triggers don't trigger when a permanent phases in or out.
            
            // Suppressed Exiling is as close as we can get to "ceasing to exist"
            AllZone.getTriggerHandler().suppressMode("ChangesZone");
            AllZone.getGameAction().exile(this);
            AllZone.getTriggerHandler().clearSuppression("ChangesZone");
        }
        return true;
    }
    
    public boolean isDirectlyPhasedOut(){
        return this.directlyPhasedOut;
    }
    
    public void setDirectlyPhasedOut(boolean direct){
        this.directlyPhasedOut = direct;
    }

    /**
     * <p>isReflectedLand.</p>
     *
     * @return a boolean.
     */
    public boolean isReflectedLand() {
    	for(Ability_Mana am : manaAbility)
    		if (am.isReflectedMana())
    			return true;
    			
    	return false;
    }

    /**
     * <p>hasKeyword.</p>
     *
     * @param keyword a {@link java.lang.String} object.
     * @return a boolean.
     */
    public boolean hasKeyword(String keyword) {
        return getKeyword().contains(keyword);
    }

    /**
     * <p>hasStartOfKeyword.</p>
     *
     * @param keyword a {@link java.lang.String} object.
     * @return a boolean.
     */
    public boolean hasStartOfKeyword(String keyword) {
        ArrayList<String> a = getKeyword();
        for (int i = 0; i < a.size(); i++)
            if (a.get(i).toString().startsWith(keyword)) return true;
        return false;
    }
    
    /**
     * <p>hasStartOfKeyword.</p>
     *
     * @param keyword a {@link java.lang.String} object.
     * @return a boolean.
     */
    public boolean hasStartOfUnHiddenKeyword(String keyword) {
        ArrayList<String> a = this.getUnhiddenKeyword();
        for (int i = 0; i < a.size(); i++)
            if (a.get(i).toString().startsWith(keyword)) return true;
        return false;
    }

    /**
     * <p>getKeywordPosition.</p>
     *
     * @param k a {@link java.lang.String} object.
     * @return a int.
     */
    public int getKeywordPosition(String k) {
        ArrayList<String> a = getKeyword();
        for (int i = 0; i < a.size(); i++)
            if (a.get(i).toString().startsWith(k)) return i;
        return -1;
    }

    /**
     * <p>keywordsContain.</p>
     *
     * @param keyword a {@link java.lang.String} object.
     * @return a boolean.
     */
    public boolean keywordsContain(String keyword) {
        ArrayList<String> a = getKeyword();
        for (int i = 0; i < a.size(); i++)
            if (a.get(i).toString().contains(keyword)) return true;
        return false;
    }


    /**
     * <p>hasAnyKeyword.</p>
     *
     * @param keywords an array of {@link java.lang.String} objects.
     * @return a boolean.
     */
    public boolean hasAnyKeyword(String keywords[]) {
        for (int i = 0; i < keywords.length; i++)
            if (hasKeyword(keywords[i]))
                return true;

        return false;
    }

    /**
     * <p>hasAnyKeyword.</p>
     *
     * @param keywords a {@link java.util.ArrayList} object.
     * @return a boolean.
     */
    public boolean hasAnyKeyword(ArrayList<String> keywords) {
        for (int i = 0; i < keywords.size(); i++)
            if (hasKeyword(keywords.get(i)))
                return true;

        return false;
    }

    //This counts the number of instances of a keyword a card has
    /**
     * <p>getAmountOfKeyword.</p>
     *
     * @param k a {@link java.lang.String} object.
     * @return a int.
     */
    public int getAmountOfKeyword(String k) {
        int count = 0;
        ArrayList<String> keywords = getKeyword();
        for (int j = 0; j < keywords.size(); j++) {
            if (keywords.get(j).equals(k)) count++;
        }

        return count;
    }

    // This is for keywords with a number like Bushido, Annihilator and Rampage. It returns the total.
    /**
     * <p>getKeywordMagnitude.</p>
     *
     * @param k a {@link java.lang.String} object.
     * @return a int.
     */
    public int getKeywordMagnitude(String k) {
        int count = 0;
        ArrayList<String> keywords = getKeyword();
        for (String kw : keywords) {
            if (kw.startsWith(k)) {
                String[] parse = kw.split(" ");
                String s = parse[1];
                count += Integer.parseInt(s);
            }
        }
        return count;
    }

    private String toMixedCase(String s) {
        if (s.equals("")) return s;
        StringBuilder sb = new StringBuilder();
        // to handle hyphenated Types
        String[] types = s.split("-");
        for (int i = 0; i < types.length; i++) {
            if (i != 0)
                sb.append("-");
            sb.append(types[i].substring(0, 1).toUpperCase());
            sb.append(types[i].substring(1).toLowerCase());
        }

        return sb.toString();
    }

    //usable to check for changelings
    /**
     * <p>isType.</p>
     *
     * @param cardType a {@link java.lang.String} object.
     * @return a boolean.
     */
    public boolean isType(String cardType) {
        cardType = toMixedCase(cardType);

        if (typeContains(cardType)
                || ((isCreature() || isTribal())
                && CardUtil.isACreatureType(cardType) && typeContains("AllCreatureTypes"))) return true;
        return false;
    }//isType

    // Takes one argument like Permanent.Blue+withFlying
    /**
     * <p>isValid.</p>
     *
     * @param Restriction a {@link java.lang.String} object.
     * @param sourceController a {@link forge.Player} object.
     * @param source a {@link forge.Card} object.
     * @return a boolean.
     */
    @Override
    public boolean isValid(final String Restriction, final Player sourceController, final Card source) {

        if (getName().equals("Mana Pool") || isImmutable()) return false;

        String incR[] = Restriction.split("\\."); // Inclusive restrictions are Card types

        if (incR[0].equals("Spell") && !isSpell())
            return false;
        if (incR[0].equals("Permanent") && (isInstant() || isSorcery()))
            return false;
        if (!incR[0].equals("card")
                && !incR[0].equals("Card")
                && !incR[0].equals("Spell")
                && !incR[0].equals("Permanent")
                && !(isType(incR[0])))
            return false; //Check for wrong type

        if (incR.length > 1) {
            final String excR = incR[1];
            String exR[] = excR.split("\\+"); // Exclusive Restrictions are ...
            for (int j = 0; j < exR.length; j++)
                if (hasProperty(exR[j], sourceController, source) == false) return false;
        }
        return true;
    }//isValid(String Restriction)

    // Takes arguments like Blue or withFlying
    /**
     * <p>hasProperty.</p>
     *
     * @param Property a {@link java.lang.String} object.
     * @param sourceController a {@link forge.Player} object.
     * @param source a {@link forge.Card} object.
     * @return a boolean.
     */
    @Override
    public boolean hasProperty(String Property, final Player sourceController, final Card source) {
        //by name can also have color names, so needs to happen before colors.
        if (Property.startsWith("named")) {
            if (!getName().equals(Property.substring(5))) return false;
        } else if (Property.startsWith("notnamed")) {
            if (getName().equals(Property.substring(8))) return false;
        } else if (Property.startsWith("sameName")) {
            if (!getName().equals(source.getName())) return false;
        } else if (Property.equals("NamedCard")) {
            if (!getName().equals(source.getNamedCard())) return false;
        }
        // ... Card colors
        else if (Property.contains("White")
                || Property.contains("Blue")
                || Property.contains("Black")
                || Property.contains("Red")
                || Property.contains("Green")
                || Property.contains("Colorless")) {
            if (Property.startsWith("non")) {
                if (CardUtil.getColors(this).contains(Property.substring(3).toLowerCase())) return false;
            } else if (!CardUtil.getColors(this).contains(Property.toLowerCase())) return false;
        } else if (Property.contains("MultiColor")) // ... Card is multicolored
        {
            if (Property.startsWith("non") && (CardUtil.getColors(this).size() > 1)) return false;
            if (!Property.startsWith("non") && (CardUtil.getColors(this).size() <= 1)) return false;
        } else if (Property.contains("MonoColor")) // ... Card is monocolored
        {
            if (Property.startsWith("non") && (CardUtil.getColors(this).size() == 1 && !isColorless())) return false;
            if (!Property.startsWith("non") && (CardUtil.getColors(this).size() > 1 || isColorless())) return false;
        } else if (Property.equals("ChosenColor")) {
            //Should this match All chosen colors, or any?  Default to first chosen for now until it matters.
            if (!CardUtil.getColors(this).contains(source.getChosenColor().get(0))) return false;
        } else if (Property.startsWith("YouCtrl")) {
            if (!getController().isPlayer(sourceController)) return false;
        } else if (Property.startsWith("YouDontCtrl")) {
            if (getController().isPlayer(sourceController)) return false;
        } else if (Property.startsWith("EnchantedPlayerCtrl")) {
            Object o = source.getEnchanting();
            if (o instanceof Player) {
                if (!getController().isPlayer((Player) o)) return false;
            }
            else { // source not enchanting a player
                return false;
            }
        } else if (Property.startsWith("YouOwn")) {
            if (!getOwner().isPlayer(sourceController)) return false;
        } else if (Property.startsWith("YouDontOwn")) {
            if (getOwner().isPlayer(sourceController)) return false;
        } else if (Property.startsWith("ControllerControls")) {
            String type = Property.substring(18);
            CardList list = getController().getCardsIn(Zone.Battlefield);
            if (list.getType(type).isEmpty()) return false;
        } else if (Property.startsWith("Other")) {
            if (this.equals(source)) return false;
        } else if (Property.startsWith("Self")) {
            if (!this.equals(source)) return false;
        } else if (Property.startsWith("AttachedBy")) {
            if (!equippedBy.contains(source) && !enchantedBy.contains(source)) return false;
        } else if (Property.startsWith("Attached")) {
            if (!equipping.contains(source) && !source.equals(enchanting)) return false;
        } else if (Property.startsWith("EnchantedBy")) {
            if (!enchantedBy.contains(source)) return false;
        } else if (Property.startsWith("Enchanted")) {
            if (!source.equals(enchanting)) return false;
        } else if (Property.startsWith("EquippedBy")) {
            if (!equippedBy.contains(source)) return false;
        } else if (Property.startsWith("Equipped")) {
            if (!equipping.contains(source)) return false;
        } else if (Property.startsWith("HauntedBy")) {
            if (!hauntedBy.contains(source)) return false;
        } else if (Property.startsWith("Above")){	// "Are Above" Source
        	CardList list = this.getOwner().getCardsIn(Zone.Graveyard);
        	if (!list.getAbove(source, this))
        		return false;
        }else if (Property.startsWith("DirectlyAbove")){	// "Are Directly Above" Source
        	CardList list = this.getOwner().getCardsIn(Zone.Graveyard);
        	if (!list.getDirectlyAbove(source, this))
        		return false;
        }else if (Property.startsWith("TopGraveyardCreature")){
            CardList list = this.getOwner().getCardsIn(Zone.Graveyard);
            list = list.getType("Creature");
            list.reverse();
            if (list.isEmpty() || !this.equals(list.get(0)))
                return false;
        }else if (Property.startsWith("TopGraveyard")){
            CardList list = this.getOwner().getCardsIn(Zone.Graveyard);
            list.reverse();
            if (list.isEmpty() || !this.equals(list.get(0)))
                return false;
        } else if (Property.startsWith("Cloned")) {
            if (cloneOrigin == null || !cloneOrigin.equals(source)) return false;
        } else if (Property.startsWith("DamagedBy")) {
            if (!receivedDamageFromThisTurn.containsKey(source)) return false;
        } else if (Property.startsWith("Damaged")) {
            if (!dealtDamageToThisTurn.containsKey(source)) return false;
        } else if (Property.startsWith("SharesColorWith")) {
            if (!sharesColorWith(source)) return false;
        } else if (Property.startsWith("withFlashback")) {
            boolean fb = false;
            if (hasStartOfUnHiddenKeyword("Flashback")) {
                fb = true;
            }
            for (SpellAbility sa : this.getSpellAbilities()) {
                if (sa.isFlashBackAbility()) {
                    fb = true;
                }
            }
            if (!fb) {
                return false;
            }
        } else if (Property.startsWith("with")) // ... Card keywords
        {
            if (Property.startsWith("without") && hasStartOfUnHiddenKeyword(Property.substring(7))) return false;
            if (!Property.startsWith("without") && !hasStartOfUnHiddenKeyword(Property.substring(4))) return false;
        } else if (Property.startsWith("tapped")) {
            if (!isTapped()) return false;
        } else if (Property.startsWith("untapped")) {
            if (!isUntapped()) return false;
        } else if (Property.startsWith("faceDown")) {
            if (!isFaceDown()) return false;
        } else if (Property.startsWith("faceUp")) {
            if (isFaceDown()) return false;
        } else if (Property.startsWith("hasLevelUp")) {
            if (!hasLevelUp()) return false;
        } else if (Property.startsWith("enteredBattlefieldThisTurn")) {
            if (!(getTurnInZone() == AllZone.getPhase().getTurn())) return false;
        } else if (Property.startsWith("dealtDamageToYouThisTurn")) {
            if (!(dealtDmgToHumanThisTurn && getController().isPlayer(AllZone.getComputerPlayer()))
                    && !(dealtDmgToComputerThisTurn && getController().isPlayer(AllZone.getHumanPlayer())))
                return false;
        } else if (Property.startsWith("wasDealtDamageThisTurn")) {
            if ((getReceivedDamageFromThisTurn().keySet()).isEmpty()) return false;
        } else if (Property.startsWith("greatestPower")) {
            CardList list = AllZoneUtil.getCreaturesInPlay();
            for (Card crd : list) {
                if (crd.getNetAttack() > getNetAttack()) {
                    return false;
                }
            }
        } else if (Property.startsWith("leastPower")) {
            CardList list = AllZoneUtil.getCreaturesInPlay();
            for (Card crd : list) {
                if (crd.getNetAttack() < getNetAttack()) {
                    return false;
                }
            }
        } else if (Property.startsWith("greatestCMC")) {
            CardList list = AllZoneUtil.getCreaturesInPlay();
            for (Card crd : list) {
                if (crd.getCMC() > getCMC()) {
                    return false;
                }
            }
        } else if (Property.startsWith("enchanted")) {
            if (!isEnchanted()) return false;
        } else if (Property.startsWith("unenchanted")) {
            if (isEnchanted()) return false;
        } else if (Property.startsWith("enchanting")) {
            if (!isEnchanting()) return false;
        } else if (Property.startsWith("equipped")) {
            if (!isEquipped()) return false;
        } else if (Property.startsWith("unequipped")) {
            if (isEquipped()) return false;
        } else if (Property.startsWith("equipping")) {
            if (!isEquipping()) return false;
        } else if (Property.startsWith("token")) {
            if (!isToken()) return false;
        } else if (Property.startsWith("nonToken")) {
            if (isToken()) return false;
        } else if (Property.startsWith("power") ||     // 8/10
                Property.startsWith("toughness") ||
                Property.startsWith("cmc")) {
            int x = 0;
            int y = 0;
            int z = 0;

            if (Property.startsWith("power")) {
                z = 7;
                y = getNetAttack();
            } else if (Property.startsWith("toughness")) {
                z = 11;
                y = getNetDefense();
            } else if (Property.startsWith("cmc")) {
                z = 5;
                y = getCMC();
            }

            if (Property.substring(z).equals("X")) {
                x = CardFactoryUtil.xCount(source, source.getSVar("X"));
            } else if (Property.substring(z).equals("Y")) {
                x = CardFactoryUtil.xCount(source, source.getSVar("Y"));
            } else
                x = Integer.parseInt(Property.substring(z));

            if (!AllZoneUtil.compare(y, Property, x))
                return false;
        }

        // syntax example: countersGE9 P1P1 or countersLT12TIME (greater number than 99 not supported)
        /*
               * slapshot5 - fair warning, you cannot use numbers with 2 digits (greater number than 9 not supported
               * you can use X and the SVar:X:Number$12 to get two digits.  This will need a better fix, and I have the
               * beginnings of a regex below
               */
        else if (Property.startsWith("counters")) {
            /*
                    Pattern p = Pattern.compile("[a-z]*[A-Z][A-Z][X0-9]+.*$");
                    String[] parse = ???
                    System.out.println("Parsing completed of: "+Property);
                    for(int i = 0; i < parse.length; i++) {
                        System.out.println("parse["+i+"]: "+parse[i]);
                    }*/

            // TODO: get a working regex out of this pattern so the amount of digits doesn't matter
            int number = 0;
            String[] splitProperty = Property.split("_");
            String strNum = splitProperty[1].substring(2);
            String comparator = splitProperty[1].substring(0,2);
            String counterType = "";
            try {
                number = Integer.parseInt(strNum);
            }
            catch(NumberFormatException e) {
                number = CardFactoryUtil.xCount(source, source.getSVar(strNum));
            }
            counterType = splitProperty[2];
            
            int actualnumber = getCounters(Counters.getType(counterType));
            
            if (!AllZoneUtil.compare(actualnumber, comparator, number))
                return false;
        } else if (Property.startsWith("attacking")) {
            if (!isAttacking()) return false;
        } else if (Property.startsWith("notattacking")) {
            if (isAttacking()) return false;
        } else if (Property.equals("blocking")) {
            if (!isBlocking()) return false;
        } else if (Property.startsWith("blockingSource")) {
            if (!isBlocking(source)) return false;
        } else if (Property.startsWith("notblocking")) {
            if (isBlocking()) return false;
        } else if (Property.startsWith("blocked")) {
            if (!AllZone.getCombat().isBlocked(this)) return false;
        } else if (Property.startsWith("blockedBySource")) {
            if (!isBlockedBy(source)) return false;
        } else if (Property.startsWith("unblocked")) {
            if (!AllZone.getCombat().isUnblocked(this)) return false;
        } else if (Property.startsWith("kicked")) {
            if (!isKicked()) return false;
        } else if (Property.startsWith("notkicked")) {
            if (isKicked()) return false;
        } else if (Property.startsWith("evoked")) {
            if (!isEvoked()) return false;
        } else if (Property.equals("HasDevoured")) {
            if(devouredCards.size() == 0) return false;
        } else if (Property.equals("HasNotDevoured")) {
            if(devouredCards.size() != 0) return false;
        } else if (Property.startsWith("non")) // ... Other Card types
        {
            if (isType(Property.substring(3))) return false;
        } else if (Property.equals("CostsPhyrexianMana")) {
            if (!manaCost.contains("P")) return false;
        } else if (Property.equals("IsRemembered")) {
            if(!source.getRemembered().contains(this)) return false;
        } else {
            if (Property.equals("ChosenType")) {
                if (!isType(source.getChosenType())) return false;
            } else {
                if (!isType(Property)) return false;
            }
        }
        return true;
    }//hasProperty

    /**
     * <p>setImmutable.</p>
     *
     * @param isImmutable a boolean.
     */
    public void setImmutable(boolean isImmutable) {
        this.isImmutable = isImmutable;
    }

    /**
     * <p>isImmutable.</p>
     *
     * @return a boolean.
     */
    public boolean isImmutable() {
        return isImmutable;
    }

    /*
      * there are easy checkers for Color.  The CardUtil functions should
      * be made part of the Card class, so calling out is not necessary
      */

    /**
     * <p>isColor.</p>
     *
     * @param col a {@link java.lang.String} object.
     * @return a boolean.
     */
    public boolean isColor(String col) {
        return CardUtil.getColors(this).contains(col);
    }

    /**
     * <p>isBlack.</p>
     *
     * @return a boolean.
     */
    public boolean isBlack() {
        return CardUtil.getColors(this).contains(Constant.Color.Black);
    }

    /**
     * <p>isBlue.</p>
     *
     * @return a boolean.
     */
    public boolean isBlue() {
        return CardUtil.getColors(this).contains(Constant.Color.Blue);
    }

    /**
     * <p>isRed.</p>
     *
     * @return a boolean.
     */
    public boolean isRed() {
        return CardUtil.getColors(this).contains(Constant.Color.Red);
    }

    /**
     * <p>isGreen.</p>
     *
     * @return a boolean.
     */
    public boolean isGreen() {
        return CardUtil.getColors(this).contains(Constant.Color.Green);
    }

    /**
     * <p>isWhite.</p>
     *
     * @return a boolean.
     */
    public boolean isWhite() {
        return CardUtil.getColors(this).contains(Constant.Color.White);
    }

    /**
     * <p>isColorless.</p>
     *
     * @return a boolean.
     */
    public boolean isColorless() {
        return CardUtil.getColors(this).contains(Constant.Color.Colorless);
    }

    /**
     * <p>sharesColorWith.</p>
     *
     * @param c1 a {@link forge.Card} object.
     * @return a boolean.
     */
    public boolean sharesColorWith(final Card c1) {
        boolean shares = false;
        shares |= (isBlack() && c1.isBlack());
        shares |= (isBlue() && c1.isBlue());
        shares |= (isGreen() && c1.isGreen());
        shares |= (isRed() && c1.isRed());
        shares |= (isWhite() && c1.isWhite());
        return shares;
    }

    /**
     * <p>isAttacking.</p>
     *
     * @return a boolean.
     */
    public boolean isAttacking() {
        return AllZone.getCombat().isAttacking(this);
    }

    /**
     * <p>isBlocking.</p>
     *
     * @return a boolean.
     */
    public boolean isBlocking() {
        CardList blockers = AllZone.getCombat().getAllBlockers();
        return blockers.contains(this);
    }

    /**
     * <p>isBlocking.</p>
     *
     * @param attacker a {@link forge.Card} object.
     * @return a boolean.
     */
    public boolean isBlocking(Card attacker) {
        return attacker.equals(AllZone.getCombat().getAttackerBlockedBy(this));
    }

    /**
     * <p>isBlockedBy.</p>
     *
     * @param blocker a {@link forge.Card} object.
     * @return a boolean.
     */
    public boolean isBlockedBy(Card blocker) {
        return this.equals(AllZone.getCombat().getAttackerBlockedBy(blocker));
    }

    ///////////////////////////
    //
    // Damage code
    //
    //////////////////////////

    //all damage to cards is now handled in Card.java, no longer AllZone.getGameAction()...
    /**
     * <p>addReceivedDamageFromThisTurn.</p>
     *
     * @param c a {@link forge.Card} object.
     * @param damage a int.
     */
    public void addReceivedDamageFromThisTurn(Card c, int damage) {
        receivedDamageFromThisTurn.put(c, damage);
    }

    /**
     * <p>Setter for the field <code>receivedDamageFromThisTurn</code>.</p>
     *
     * @param receivedDamageList a Map object.
     */
    public void setReceivedDamageFromThisTurn(Map<Card, Integer> receivedDamageList) {
        receivedDamageFromThisTurn = receivedDamageList;
    }

    /**
     * <p>Getter for the field <code>receivedDamageFromThisTurn</code>.</p>
     *
     * @return a Map object.
     */
    public Map<Card, Integer> getReceivedDamageFromThisTurn() {
        return receivedDamageFromThisTurn;
    }

    /**
     * <p>resetReceivedDamageFromThisTurn.</p>
     */
    public void resetReceivedDamageFromThisTurn() {
        receivedDamageFromThisTurn.clear();
    }

    /**
     * <p>addDealtDamageToThisTurn.</p>
     *
     * @param c a {@link forge.Card} object.
     * @param damage a int.
     */
    public void addDealtDamageToThisTurn(Card c, int damage) {
        dealtDamageToThisTurn.put(c, damage);
    }

    /**
     * <p>Setter for the field <code>dealtDamageToThisTurn</code>.</p>
     *
     * @param dealtDamageList a {@link java.util.Map} object.
     */
    public void setDealtDamageToThisTurn(Map<Card, Integer> dealtDamageList) {
        dealtDamageToThisTurn = dealtDamageList;
    }

    /**
     * <p>Getter for the field <code>dealtDamageToThisTurn</code>.</p>
     *
     * @return a {@link java.util.Map} object.
     */
    public Map<Card, Integer> getDealtDamageToThisTurn() {
        return dealtDamageToThisTurn;
    }

    /**
     * <p>resetDealtDamageToThisTurn.</p>
     */
    public void resetDealtDamageToThisTurn() {
        dealtDamageToThisTurn.clear();
    }

    //how much damage is enough to kill the creature (for AI)
    /**
     * <p>getEnoughDamageToKill.</p>
     *
     * @param maxDamage a int.
     * @param source a {@link forge.Card} object.
     * @param isCombat a boolean.
     * @return a int.
     */
    public int getEnoughDamageToKill(int maxDamage, Card source, boolean isCombat) {
        return getEnoughDamageToKill(maxDamage, source, isCombat, false);
    }

    /**
     * <p>getEnoughDamageToKill.</p>
     *
     * @param maxDamage a int.
     * @param source a {@link forge.Card} object.
     * @param isCombat a boolean.
     * @param noPrevention a boolean.
     * @return a int.
     */
    public int getEnoughDamageToKill(int maxDamage, Card source, boolean isCombat, boolean noPrevention) {
        int killDamage = getKillDamage();

        if (hasKeyword("Indestructible") || getShield() > 0) {
            if (!(source.hasKeyword("Wither") || source.hasKeyword("Infect")))
                return maxDamage + 1;
        } else if (source.hasKeyword("Deathtouch")) {
            for (int i = 1; i <= maxDamage; i++) {
                if (noPrevention) {
                    if (staticReplaceDamage(i, source, isCombat) > 0)
                        return i;
                } else if (predictDamage(i, source, isCombat) > 0)
                    return i;
            }
        }

        for (int i = 1; i <= maxDamage; i++) {
            if (noPrevention) {
                if (staticReplaceDamage(i, source, isCombat) >= killDamage)
                    return i;
            } else {
                if (predictDamage(i, source, isCombat) >= killDamage)
                    return i;
            }
        }

        return maxDamage + 1;
    }

    //the amount of damage needed to kill the creature (for AI)
    /**
     * <p>getKillDamage.</p>
     *
     * @return a int.
     */
    public int getKillDamage() {
        int killDamage = getLethalDamage() + getPreventNextDamage();
        if (killDamage > getPreventNextDamage() && hasStartOfKeyword("When CARDNAME is dealt damage, destroy it.")) {
            killDamage = 1 + getPreventNextDamage();
        }

        return killDamage;
    }

    //this is the minimal damage a trampling creature has to assign to a blocker
    /**
     * <p>getLethalDamage.</p>
     *
     * @return a int.
     */
    public int getLethalDamage() {
        int lethalDamage = getNetDefense() - getDamage() - getTotalAssignedDamage();

        return lethalDamage;
    }

    /**
     * <p>Setter for the field <code>damage</code>.</p>
     *
     * @param n a int.
     */
    public void setDamage(int n) {
        //if (this.hasKeyword("Prevent all damage that would be dealt to CARDNAME.")) n = 0;
        damage = n;
    }

    /**
     * <p>Getter for the field <code>damage</code>.</p>
     *
     * @return a int.
     */
    public int getDamage() {
        return damage;
    }

    /**
     * <p>addAssignedDamage.</p>
     *
     * @param damage a int.
     * @param sourceCard a {@link forge.Card} object.
     */
    public void addAssignedDamage(int damage, Card sourceCard) {
        if (damage < 0) {
            damage = 0;
        }

        int assignedDamage = damage;

        Log.debug(this + " - was assigned " + assignedDamage + " damage, by " + sourceCard);
        if (!assignedDamageMap.containsKey(sourceCard)) {
            assignedDamageMap.put(sourceCard, assignedDamage);
        } else {
            assignedDamageMap.put(sourceCard, assignedDamageMap.get(sourceCard) + assignedDamage);
        }

        Log.debug("***");
        /*
        if(sourceCards.size() > 1)
          System.out.println("(MULTIPLE blockers):");
        System.out.println("Assigned " + damage + " damage to " + card);
        for (int i=0;i<sourceCards.size();i++){
          System.out.println(sourceCards.get(i).getName() + " assigned damage to " + card.getName());
        }
        System.out.println("***");
        */
    }

    /**
     * <p>clearAssignedDamage.</p>
     */
    public void clearAssignedDamage() {
        assignedDamageMap.clear();
    }

    /**
     * <p>getTotalAssignedDamage.</p>
     *
     * @return a int.
     */
    public int getTotalAssignedDamage() {
        int total = 0;

        Collection<Integer> c = assignedDamageMap.values();

        Iterator<Integer> itr = c.iterator();
        while (itr.hasNext()) {
            total += itr.next();
        }

        return total;
    }

    /**
     * <p>Getter for the field <code>assignedDamageMap</code>.</p>
     *
     * @return a {@link java.util.Map} object.
     */
    public Map<Card, Integer> getAssignedDamageMap() {
        return assignedDamageMap;
    }

    /**
     * <p>addCombatDamage.</p>
     *
     * @param map a {@link java.util.Map} object.
     */
    public void addCombatDamage(Map<Card, Integer> map) {
        CardList list = new CardList();

        for (Entry<Card, Integer> entry : map.entrySet()) {
            Card source = entry.getKey();
            list.add(source);
            int damageToAdd = entry.getValue();

            damageToAdd = replaceDamage(damageToAdd, source, true);
            damageToAdd = preventDamage(damageToAdd, source, true);

            if (damageToAdd > 0 && isCreature()) {
                    GameActionUtil.executeCombatDamageToCreatureEffects(source, this, damageToAdd);
            }
            map.put(source, damageToAdd);
        }

        if (AllZoneUtil.isCardInPlay(this)) {
            addDamage(map);
        }
    }

    //This function helps the AI calculate the actual amount of damage an effect would deal
    /**
     * <p>predictDamage.</p>
     *
     * @param damage a int.
     * @param possiblePrevention a int.
     * @param source a {@link forge.Card} object.
     * @param isCombat a boolean.
     * @return a int.
     */
    public int predictDamage(final int damage, final int possiblePrevention, final Card source, final boolean isCombat) {

        int restDamage = damage;

        restDamage = staticReplaceDamage(restDamage, source, isCombat);

        restDamage = staticDamagePrevention(restDamage, possiblePrevention, source, isCombat);

        return restDamage;
    }

    //This should be also usable by the AI to forecast an effect (so it must not change the game state)
    /**
     * <p>staticDamagePrevention.</p>
     *
     * @param damage a int.
     * @param possiblePrvenetion a int.
     * @param source a {@link forge.Card} object.
     * @param isCombat a boolean.
     * @return a int.
     */
    public int staticDamagePrevention(final int damage, final int possiblePrvenetion, final Card source, final boolean isCombat) {

        if (AllZoneUtil.isCardInPlay("Leyline of Punishment")) {
            return damage;
        }

        int restDamage = damage - possiblePrvenetion;

        restDamage = staticDamagePrevention(restDamage, source, isCombat);

        return restDamage;
    }

    //This should be also usable by the AI to forecast an effect (so it must not change the game state)
    /**
     * <p>staticDamagePrevention.</p>
     *
     * @param damageIn a int.
     * @param source a {@link forge.Card} object.
     * @param isCombat a boolean.
     * @return a int.
     */
    @Override
    public final int staticDamagePrevention(final int damageIn, final Card source, final boolean isCombat) {

        if (AllZoneUtil.isCardInPlay("Leyline of Punishment")) {
            return damageIn;
        }

        int restDamage = damageIn;

        if (CardFactoryUtil.hasProtectionFrom(source, this)) {
            return 0;
        }

        if (isCombat) {
            if (hasKeyword("Prevent all combat damage that would be dealt to and dealt by CARDNAME.")) return 0;
            if (hasKeyword("Prevent all combat damage that would be dealt to CARDNAME.")) return 0;
            if (source.hasKeyword("Prevent all combat damage that would be dealt to and dealt by CARDNAME.")) return 0;
            if (source.hasKeyword("Prevent all combat damage that would be dealt by CARDNAME.")) return 0;
        }
        if (hasKeyword("Prevent all damage that would be dealt to CARDNAME.")) return 0;
        if (hasKeyword("Prevent all damage that would be dealt to and dealt by CARDNAME.")) return 0;
        if (source.hasKeyword("Prevent all damage that would be dealt to and dealt by CARDNAME.")) return 0;
        if (source.hasKeyword("Prevent all damage that would be dealt by CARDNAME.")) return 0;

        if (hasStartOfKeyword("Absorb")) {
            int absorbed = this.getKeywordMagnitude("Absorb");
            if (restDamage > absorbed) {
                restDamage = restDamage - absorbed;
            } else {
                return 0;
            }
        }

        if (hasStartOfKeyword("PreventAllDamageBy")) {
            String valid = getKeyword().get(getKeywordPosition("PreventAllDamageBy"));
            valid = valid.split(" ", 2)[1];
            if (source.isValid(valid, this.getController(), this)) {
                return 0;
            }
        }
        
        //Prevent Damage static abilities
        CardList allp = AllZoneUtil.getCardsIn(Zone.Battlefield);
        for (Card ca : allp) {
            ArrayList<StaticAbility> staticAbilities = ca.getStaticAbilities();
            for (StaticAbility stAb : staticAbilities) {
                restDamage = stAb.applyAbility("PreventDamage", source, this, restDamage, isCombat);
            }
        }

        // specific Cards
        if (isCreature()) { //and not a planeswalker
            if (getName().equals("Swans of Bryn Argoll")) {
                return 0;
            }

            if ((source.isCreature() && AllZoneUtil.isCardInPlay("Well-Laid Plans") && source.sharesColorWith(this))) {
                return 0;
            }
        } //Creature end

        if (restDamage > 0) {
            return restDamage;
        } else {
            return 0;
        }
    }

    /**
     * <p>preventDamage.</p>
     *
     * @param damage a int.
     * @param source a {@link forge.Card} object.
     * @param isCombat a boolean.
     * @return a int.
     */
    @Override
    public int preventDamage(final int damage, Card source, boolean isCombat) {

        if (AllZoneUtil.isCardInPlay("Leyline of Punishment")) {
            return damage;
        }

        int restDamage = damage;

        if (getName().equals("Swans of Bryn Argoll")) {
            source.getController().drawCards(restDamage);
            return 0;
        }

        restDamage = staticDamagePrevention(restDamage, source, isCombat);

        if (restDamage == 0) {
            return 0;
        }

        if (this.hasKeyword("If damage would be dealt to CARDNAME, prevent that damage. Remove a +1/+1 counter from CARDNAME.")) {
            restDamage = 0;
            this.subtractCounter(Counters.P1P1, 1);
        }

        if (restDamage >= getPreventNextDamage()) {
            restDamage = restDamage - getPreventNextDamage();
            setPreventNextDamage(0);
        } else {
            setPreventNextDamage(getPreventNextDamage() - restDamage);
            restDamage = 0;
        }

        if (getName().equals("Phyrexian Hydra")) {
            addCounter(Counters.M1M1, restDamage);
            return 0;
        }

        return restDamage;
    }

    //This should be also usable by the AI to forecast an effect (so it must not change the game state)
    /**
     * <p>staticReplaceDamage.</p>
     *
     * @param damage a int.
     * @param source a {@link forge.Card} object.
     * @param isCombat a boolean.
     * @return a int.
     */
    @Override
    public final int staticReplaceDamage(final int damage, Card source, boolean isCombat) {

        int restDamage = damage;

        if (AllZoneUtil.isCardInPlay("Sulfuric Vapors") && source.isSpell() && source.isRed()) {
            int amount = AllZoneUtil.getCardsIn(Zone.Battlefield, "Sulfuric Vapors").size();
            for (int i = 0; i < amount; i++) {
                restDamage += 1;
            }
        }

        if (AllZoneUtil.isCardInPlay("Pyromancer's Swath", source.getController()) && (source.isInstant() || source.isSorcery()) 
                && isCreature())
        {
            int amount = source.getController().getCardsIn(Zone.Battlefield, "Pyromancer's Swath").size();
            for (int i = 0; i < amount; i++) {
                restDamage += 2;
            }
        }

        if (AllZoneUtil.isCardInPlay("Furnace of Rath") && isCreature()) {
            int amount = AllZoneUtil.getCardsIn(Zone.Battlefield, "Furnace of Rath").size();
            for (int i = 0; i < amount; i++) {
                restDamage += restDamage;
            }
        }

        if (AllZoneUtil.isCardInPlay("Gratuitous Violence", source.getController()) && source.isCreature() && isCreature()) {
            int amount = source.getController().getCardsIn(Zone.Battlefield, "Gratuitous Violence").size();
            for (int i = 0; i < amount; i++) {
                restDamage += restDamage;
            }
        }

        if (AllZoneUtil.isCardInPlay("Fire Servant", source.getController()) && source.isRed()
                && (source.isInstant() || source.isSorcery()))
        {
            int amount = source.getController().getCardsIn(Zone.Battlefield, "Fire Servant").size();
            for (int i = 0; i < amount; i++) {
                restDamage += restDamage;
            }
        }

        if (AllZoneUtil.isCardInPlay("Benevolent Unicorn") && source.isSpell() && isCreature()) {
            int amount = AllZoneUtil.getCardsIn(Zone.Battlefield, "Benevolent Unicorn").size();
            for (int i = 0; i < amount; i++) {
                if (restDamage > 0) {
                    restDamage -= 1;
                }
            }
        }

        if (AllZoneUtil.isCardInPlay("Lashknife Barrier", getController()) && isCreature()) {
            int amount = getController().getCardsIn(Zone.Battlefield, "Lashknife Barrier").size();
            for (int i = 0; i < amount; i++) {
                if (restDamage > 0) {
                    restDamage -= 1;
                }
            }
        }

        if (AllZoneUtil.isCardInPlay("Divine Presence") && isCreature() && restDamage > 3) {

            restDamage = 3;
        }

        if (getName().equals("Phytohydra")) {
            return 0;
        }

        return restDamage;
    }

    /**
     * <p>replaceDamage.</p>
     *
     * @param damageIn a int.
     * @param source a {@link forge.Card} object.
     * @param isCombat a boolean.
     * @return a int.
     */
    @Override
    public final int replaceDamage(final int damageIn, final Card source, final boolean isCombat) {

        int restDamage = damageIn;
        CardList auras = new CardList(getEnchantedBy().toArray());

        if (getName().equals("Phytohydra")) {
            addCounter(Counters.P1P1, restDamage);
            return 0;
        }

        if (auras.containsName("Treacherous Link")) {
            getController().addDamage(restDamage, source);
            return 0;
        }

        restDamage = staticReplaceDamage(restDamage, source, isCombat);

        if (getName().equals("Lichenthrope")) {
            addCounter(Counters.M1M1, restDamage);
            return 0;
        }

        return restDamage;
    }

    /**
     * <p>addDamage.</p>
     *
     * @param sourcesMap a {@link java.util.Map} object.
     */
    public final void addDamage(final Map<Card, Integer> sourcesMap) {
        for (Entry<Card, Integer> entry : sourcesMap.entrySet()) {
            addDamageAfterPrevention(entry.getValue(), entry.getKey(), true); // damage prevention is already checked!
        }
    }

    /**
     * <p>addDamageAfterPrevention.</p>
     * This function handles damage after replacement and prevention effects are applied.
     *
     * @param damageIn a int.
     * @param source a {@link forge.Card} object.
     * @param isCombat a boolean.
     */
    @Override
    public final void addDamageAfterPrevention(final int damageIn, final Card source, final boolean isCombat) {
        int damageToAdd = damageIn;
        boolean wither = false;

        if (damageToAdd == 0) {
            return;  //Rule 119.8
        }

        System.out.println("Adding " + damageToAdd + " damage to " + getName());
        Log.debug("Adding " + damageToAdd + " damage to " + getName());

        addReceivedDamageFromThisTurn(source, damageToAdd);
        source.addDealtDamageToThisTurn(this, damageToAdd);

        GameActionUtil.executeDamageDealingEffects(source, damageToAdd);

        //Run triggers
        Map<String, Object> runParams = new TreeMap<String, Object>();
        runParams.put("DamageSource", source);
        runParams.put("DamageTarget", this);
        runParams.put("DamageAmount", damageToAdd);
        runParams.put("IsCombatDamage", isCombat);
        AllZone.getTriggerHandler().runTrigger("DamageDone", runParams);

        if (this.isPlaneswalker()) {
            this.subtractCounter(Counters.LOYALTY, damageToAdd);
            return;
        }

        if ((source.hasKeyword("Wither") || source.hasKeyword("Infect"))) {
            wither = true;
        }

        GameActionUtil.executeDamageToCreatureEffects(source, this, damageToAdd);

        if (AllZoneUtil.isCardInPlay(this) && wither) {
            addCounter(Counters.M1M1, damageToAdd);
        }
        if (AllZoneUtil.isCardInPlay(this) && !wither) {
            damage += damageToAdd;
        }

    }

    private ArrayList<SetInfo> Sets = new ArrayList<SetInfo>();
    private String curSetCode = "";

    /**
     * <p>addSet.</p>
     *
     * @param sInfo a {@link forge.SetInfo} object.
     */
    public final void addSet(final SetInfo sInfo) {
        Sets.add(sInfo);
    }

    /**
     * <p>getSets.</p>
     *
     * @return a {@link java.util.ArrayList} object.
     */
    public final ArrayList<SetInfo> getSets() {
        return Sets;
    }

    /**
     * <p>setSets.</p>
     *
     * @param siList a {@link java.util.ArrayList} object.
     */
    public final void setSets(final ArrayList<SetInfo> siList) {
        Sets = siList;
    }

    /**
     * <p>Setter for the field <code>curSetCode</code>.</p>
     *
     * @param setCode a {@link java.lang.String} object.
     */
    public final void setCurSetCode(final String setCode) {
        curSetCode = setCode;
    }

    /**
     * <p>Getter for the field <code>curSetCode</code>.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    public final String getCurSetCode() {
        return curSetCode;
    }

    /**
     * <p>setRandomSetCode.</p>
     */
    public final void setRandomSetCode() {
        if (Sets.size() < 1) {
            return;
        }

        Random r = MyRandom.random;
        SetInfo si = Sets.get(r.nextInt(Sets.size()));

        curSetCode = si.Code;
    }

    /**
     * <p>getSetImageName.</p>
     *
     * @param setCode a {@link java.lang.String} object.
     * @return a {@link java.lang.String} object.
     */
    public final String getSetImageName(final String setCode) {
        return "/" + setCode + "/" + getImageName();
    }

    /**
     * <p>getCurSetImage.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    public final String getCurSetImage() {
        return getSetImageName(curSetCode);
    }

    /**
     * <p>getCurSetRarity.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    public final String getCurSetRarity() {
        for (int i = 0; i < Sets.size(); i++) {
            if (Sets.get(i).Code.equals(curSetCode)) {
                return Sets.get(i).Rarity;
            }
        }

        return "";
    }

    /**
     * <p>getCurSetURL.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    public final String getCurSetURL() {
        for (int i = 0; i < Sets.size(); i++) {
            if (Sets.get(i).Code.equals(curSetCode)) {
                return Sets.get(i).URL;
            }
        }

        return "";
    }

    /**
     * <p>getMostRecentSet.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    public final String getMostRecentSet() {
        return CardDb.instance().getCard(this.getName()).getSet();
    }

    private String ImageFilename = "";

    /**
     * <p>setImageFilename.</p>
     *
     * @param iFN a {@link java.lang.String} object.
     */
    public void setImageFilename(final String iFN) {
        ImageFilename = iFN;
    }

    /**
     * <p>getImageFilename.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    public final String getImageFilename() {
        return ImageFilename;
    }

    /**
     * <p>Setter for the field <code>evoked</code>.</p>
     *
     * @param evokedIn a boolean.
     */
    public final void setEvoked(final boolean evokedIn) {
        evoked = evokedIn;
    }

    /**
     * <p>isEvoked.</p>
     *
     * @return a boolean.
     */
    public final boolean isEvoked() {
        return evoked;
    }

    /**
     * 
     * TODO Write javadoc for this method.
     * @param t a long
     */
    public final void setTimestamp(final long t) {
        timestamp = t;
    }

    /**
     * 
     * TODO Write javadoc for this method.
     * @return a long
     */
    public final long getTimestamp() {
        return timestamp;
    }

    /**
     * 
     * TODO Write javadoc for this method.
     * @return an int
     */
    public final int getFoil() {
        if (sVars.containsKey("Foil")) {
            return Integer.parseInt(sVars.get("Foil"));
        }
        return 0;
    }

    /**
     * 
     * TODO Write javadoc for this method.
     * @param f an int
     */
    public final void setFoil(final int f) {
    	sVars.put("Foil", Integer.toString(f));
    }
    
    public final void addHauntedBy(final Card c) {
        hauntedBy.add(c);
        if(c != null) {
            c.setHaunting(this);
        }
    }
    
    public final ArrayList<Card> getHauntedBy() {
        return hauntedBy;
    }
    
    public final void removeHauntedBy(final Card c) {
        hauntedBy.remove(c);
    }
    
    public final Card getHaunting() {
        return haunting;
    }
    
    public final void setHaunting(final Card c) {
        haunting = c;
    }    
    
    public final int getDamageDoneThisTurn() {
        int sum = 0;
        for(Card c : dealtDamageToThisTurn.keySet()) {
            sum += dealtDamageToThisTurn.get(c);
        }
        
        return sum;
    }

} //end Card class
