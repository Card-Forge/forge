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
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import javax.swing.JOptionPane;

import forge.Constant.Zone;
import forge.card.cardfactory.CardFactoryUtil;
import forge.card.mana.ManaPool;
import forge.card.spellability.SpellAbility;
import forge.card.staticability.StaticAbility;
import forge.game.GameLossReason;

/**
 * <p>
 * Abstract Player class.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 */
public abstract class Player extends GameEntity {

    /** The poison counters. */
    private int poisonCounters;

    /** The life. */
    private int life;

    /** The assigned damage. */
    private int assignedDamage;

    /** The num power surge lands. */
    private int numPowerSurgeLands;

    /** The alt win. */
    private boolean altWin = false;

    /** The alt win source name. */
    private String altWinSourceName;

    /** The alt lose. */
    // private boolean altLose = false;

    /** The loss state. */
    private GameLossReason lossState = GameLossReason.DidNotLoseYet;

    /** The lose condition spell. */
    private String loseConditionSpell;

    /** The n turns. */
    private int nTurns = 0;

    /** The skip next untap. */
    private boolean skipNextUntap = false;

    /** The prowl. */
    private ArrayList<String> prowl = new ArrayList<String>();

    /** The max lands to play. */
    private int maxLandsToPlay = 1;

    /** The num lands played. */
    private int numLandsPlayed = 0;

    /** The last drawn card. */
    private Card lastDrawnCard;

    /** The num drawn this turn. */
    private int numDrawnThisTurn = 0;

    /** The slowtrip list. */
    private CardList slowtripList = new CardList();

    /** The keywords. */
    private ArrayList<String> keywords = new ArrayList<String>();

    /** The mana pool. */
    private ManaPool manaPool = null;

    /** The must attack entity. */
    private Object mustAttackEntity = null;

    /** The zones. */
    private final Map<Constant.Zone, PlayerZone> zones = new EnumMap<Constant.Zone, PlayerZone>(Constant.Zone.class);

    /** The Constant ALL_ZONES. */
    public static final List<Zone> ALL_ZONES = Collections.unmodifiableList(Arrays.asList(Zone.Battlefield,
            Zone.Library, Zone.Graveyard, Zone.Hand, Zone.Exile, Zone.Command));

    /**
     * <p>
     * Constructor for Player.
     * </p>
     * 
     * @param myName
     *            a {@link java.lang.String} object.
     */
    public Player(final String myName) {
        this(myName, 20, 0);
    }

    /**
     * <p>
     * Constructor for Player.
     * </p>
     * 
     * @param myName
     *            a {@link java.lang.String} object.
     * @param myLife
     *            a int.
     * @param myPoisonCounters
     *            a int.
     */
    public Player(final String myName, final int myLife, final int myPoisonCounters) {
        for (final Zone z : Player.ALL_ZONES) {
            final PlayerZone toPut = z == Zone.Battlefield ? new PlayerZoneComesIntoPlay(z, this)
                    : new DefaultPlayerZone(z, this);
            this.zones.put(z, toPut);
        }

        this.reset();

        this.setName(myName);
        this.life = myLife;
        this.poisonCounters = myPoisonCounters;
    }

    /**
     * <p>
     * reset.
     * </p>
     */
    public final void reset() {
        this.life = 20;
        this.poisonCounters = 0;
        this.assignedDamage = 0;
        this.setPreventNextDamage(0);
        this.lastDrawnCard = null;
        this.numDrawnThisTurn = 0;
        this.slowtripList = new CardList();
        this.nTurns = 0;
        this.altWin = false;
        this.altWinSourceName = null;
        // altLose = false;
        this.lossState = GameLossReason.DidNotLoseYet;
        this.loseConditionSpell = null;
        this.maxLandsToPlay = 1;
        this.numLandsPlayed = 0;
        this.prowl = new ArrayList<String>();

        this.handSizeOperations = new ArrayList<HandSizeOp>();
        this.keywords.clear();
        this.manaPool = new ManaPool(this);

        this.updateObservers();
    }

    /**
     * <p>
     * isHuman.
     * </p>
     * 
     * @return a boolean.
     */
    public abstract boolean isHuman();

    /**
     * <p>
     * isComputer.
     * </p>
     * 
     * @return a boolean.
     */
    public abstract boolean isComputer();

    /**
     * <p>
     * isPlayer.
     * </p>
     * 
     * @param p1
     *            a {@link forge.Player} object.
     * @return a boolean.
     */
    public final boolean isPlayer(final Player p1) {
        return (p1 != null) && p1.getName().equals(this.getName());
    }

    /**
     * <p>
     * getOpponent.
     * </p>
     * 
     * @return a {@link forge.Player} object.
     */
    public abstract Player getOpponent();

    // ////////////////////////
    //
    // methods for manipulating life
    //
    // ////////////////////////

    /**
     * <p>
     * Setter for the field <code>life</code>.
     * </p>
     * 
     * @param newLife
     *            a int.
     * @param source
     *            a {@link forge.Card} object.
     * @return a boolean.
     */
    public final boolean setLife(final int newLife, final Card source) {
        boolean change = false;
        // rule 118.5
        if (this.life > newLife) {
            change = this.loseLife(this.life - newLife, source);
        } else if (newLife > this.life) {
            change = this.gainLife(newLife - this.life, source);
        } else {
            // life == newLife
            change = false;
        }
        this.updateObservers();
        return change;
    }

    /**
     * <p>
     * Getter for the field <code>life</code>.
     * </p>
     * 
     * @return a int.
     */
    public final int getLife() {
        return this.life;
    }

    /**
     * <p>
     * addLife.
     * </p>
     * 
     * @param toAdd
     *            a int.
     */
    private void addLife(final int toAdd) {
        this.life += toAdd;
        this.updateObservers();
    }

    /**
     * <p>
     * gainLife.
     * </p>
     * 
     * @param toGain
     *            a int.
     * @param source
     *            a {@link forge.Card} object.
     * @return a boolean.
     */
    public final boolean gainLife(final int toGain, final Card source) {
        boolean newLifeSet = false;
        if (!this.canGainLife()) {
            return false;
        }
        int lifeGain = toGain;

        if (AllZoneUtil.isCardInPlay("Boon Reflection", this)) {
            final int amount = AllZoneUtil.getCardsIn(Zone.Battlefield, "Boon Reflection").size();
            for (int i = 0; i < amount; i++) {
                lifeGain += lifeGain;
            }
        }

        if (lifeGain > 0) {
            if (AllZoneUtil.isCardInPlay("Lich", this)) {
                // draw cards instead of gain life
                this.drawCards(lifeGain);
                newLifeSet = false;
            } else {
                this.addLife(lifeGain);
                newLifeSet = true;
                this.updateObservers();

                // Run triggers
                final HashMap<String, Object> runParams = new HashMap<String, Object>();
                runParams.put("Player", this);
                runParams.put("LifeAmount", lifeGain);
                AllZone.getTriggerHandler().runTrigger("LifeGained", runParams);
            }
        } else {
            System.out.println("Player - trying to gain negative or 0 life");
        }

        return newLifeSet;
    }

    /**
     * <p>
     * canGainLife.
     * </p>
     * 
     * @return a boolean.
     */
    public final boolean canGainLife() {
        if (AllZoneUtil.isCardInPlay("Sulfuric Vortex") || AllZoneUtil.isCardInPlay("Leyline of Punishment")
                || AllZoneUtil.isCardInPlay("Platinum Emperion", this) || AllZoneUtil.isCardInPlay("Forsaken Wastes")) {
            return false;
        }
        return true;
    }

    /**
     * <p>
     * loseLife.
     * </p>
     * 
     * @param toLose
     *            a int.
     * @param c
     *            a {@link forge.Card} object.
     * @return a boolean.
     */
    public final boolean loseLife(final int toLose, final Card c) {
        boolean newLifeSet = false;
        if (!this.canLoseLife()) {
            return false;
        }
        if (toLose > 0) {
            this.subtractLife(toLose);
            newLifeSet = true;
            this.updateObservers();
        } else if (toLose == 0) {
            // Rule 118.4
            // this is for players being able to pay 0 life
            // nothing to do
        } else {
            System.out.println("Player - trying to lose positive life");
        }

        // Run triggers
        final HashMap<String, Object> runParams = new HashMap<String, Object>();
        runParams.put("Player", this);
        runParams.put("LifeAmount", toLose);
        AllZone.getTriggerHandler().runTrigger("LifeLost", runParams);

        return newLifeSet;
    }

    /**
     * <p>
     * canLoseLife.
     * </p>
     * 
     * @return a boolean.
     */
    public final boolean canLoseLife() {
        if (AllZoneUtil.isCardInPlay("Platinum Emperion", this)) {
            return false;
        }
        return true;
    }

    /**
     * <p>
     * subtractLife.
     * </p>
     * 
     * @param toSub
     *            a int.
     */
    private void subtractLife(final int toSub) {
        this.life -= toSub;
        this.updateObservers();
    }

    /**
     * <p>
     * canPayLife.
     * </p>
     * 
     * @param lifePayment
     *            a int.
     * @return a boolean.
     */
    public final boolean canPayLife(final int lifePayment) {
        if (this.life < lifePayment) {
            return false;
        }
        if ((lifePayment > 0) && AllZoneUtil.isCardInPlay("Platinum Emperion", this)) {
            return false;
        }
        return true;
    }

    /**
     * <p>
     * payLife.
     * </p>
     * 
     * @param lifePayment
     *            a int.
     * @param source
     *            a {@link forge.Card} object.
     * @return a boolean.
     */
    public final boolean payLife(final int lifePayment, final Card source) {
        if (!this.canPayLife(lifePayment)) {
            return false;
        }
        // rule 118.8
        if (this.life >= lifePayment) {
            return this.loseLife(lifePayment, source);
        }

        return false;
    }

    // ////////////////////////
    //
    // methods for handling damage
    //
    // ////////////////////////

    // This function handles damage after replacement and prevention effects are
    // applied
    /**
     * <p>
     * addDamageAfterPrevention.
     * </p>
     * 
     * @param damage
     *            a int.
     * @param source
     *            a {@link forge.Card} object.
     * @param isCombat
     *            a boolean.
     */
    @Override
    public final void addDamageAfterPrevention(final int damage, final Card source, final boolean isCombat) {
        final int damageToDo = damage;

        if (source.hasKeyword("Infect")) {
            this.addPoisonCounters(damageToDo, source);
        } else {
            // Worship does not reduce the damage dealt but changes the effect
            // of the damage
            if (PlayerUtil.worshipFlag(this) && (this.life <= damageToDo)) {
                this.loseLife(Math.min(damageToDo, this.life - 1), source);
            } else {
                // rule 118.2. Damage dealt to a player normally causes that
                // player to lose that much life.
                this.loseLife(damageToDo, source);
            }
        }
        if (damageToDo > 0) {
            this.addAssignedDamage(damageToDo);
            GameActionUtil.executeDamageDealingEffects(source, damageToDo);
            GameActionUtil.executeDamageToPlayerEffects(this, source, damageToDo);

            if (isCombat) {
                final ArrayList<String> types = source.getType();
                for (final String type : types) {
                    source.getController().addProwlType(type);
                }
            }

            // Run triggers
            final HashMap<String, Object> runParams = new HashMap<String, Object>();
            runParams.put("DamageSource", source);
            runParams.put("DamageTarget", this);
            runParams.put("DamageAmount", damageToDo);
            runParams.put("IsCombatDamage", isCombat);
            AllZone.getTriggerHandler().runTrigger("DamageDone", runParams);
        }
    }

    /**
     * <p>
     * predictDamage.
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
    public final int predictDamage(final int damage, final Card source, final boolean isCombat) {

        int restDamage = damage;

        restDamage = this.staticReplaceDamage(restDamage, source, isCombat);
        restDamage = this.staticDamagePrevention(restDamage, source, isCombat);

        return restDamage;
    }

    // This should be also usable by the AI to forecast an effect (so it must
    // not change the game state)
    /**
     * <p>
     * staticDamagePrevention.
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
    public final int staticDamagePrevention(final int damage, final Card source, final boolean isCombat) {

        if (AllZoneUtil.isCardInPlay("Leyline of Punishment")) {
            return damage;
        }

        if (this.hasProtectionFrom(source)) {
            return 0;
        }

        int restDamage = damage;

        if (isCombat) {
            if (source.hasKeyword("Prevent all combat damage that would be dealt to and dealt by CARDNAME.")) {
                return 0;
            }
            if (source.hasKeyword("Prevent all combat damage that would be dealt by CARDNAME.")) {
                return 0;
            }
        }
        if (source.hasKeyword("Prevent all damage that would be dealt to and dealt by CARDNAME.")) {
            return 0;
        }
        if (source.hasKeyword("Prevent all damage that would be dealt by CARDNAME.")) {
            return 0;
        }
        if (AllZoneUtil.isCardInPlay("Purity", this) && !isCombat) {
            return 0;
        }

        // Prevent Damage static abilities
        final CardList allp = AllZoneUtil.getCardsIn(Zone.Battlefield);
        for (final Card ca : allp) {
            final ArrayList<StaticAbility> staticAbilities = ca.getStaticAbilities();
            for (final StaticAbility stAb : staticAbilities) {
                restDamage = stAb.applyAbility("PreventDamage", source, this, restDamage, isCombat);
            }
        }

        // specific cards
        if (AllZoneUtil.isCardInPlay("Spirit of Resistance", this)) {
            if ((AllZoneUtil.getPlayerColorInPlay(this, Constant.Color.BLACK).size() > 0)
                    && (AllZoneUtil.getPlayerColorInPlay(this, Constant.Color.BLUE).size() > 0)
                    && (AllZoneUtil.getPlayerColorInPlay(this, Constant.Color.GREEN).size() > 0)
                    && (AllZoneUtil.getPlayerColorInPlay(this, Constant.Color.RED).size() > 0)
                    && (AllZoneUtil.getPlayerColorInPlay(this, Constant.Color.WHITE).size() > 0)) {
                return 0;
            }
        }
        if (restDamage > 0) {
            return restDamage;
        } else {
            return 0;
        }
    }

    // This should be also usable by the AI to forecast an effect (so it must
    // not change the game state)
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

        if (AllZoneUtil.isCardInPlay("Sulfuric Vapors") && source.isSpell() && source.isRed()) {
            final int amount = AllZoneUtil.getCardsIn(Zone.Battlefield, "Sulfuric Vapors").size();
            for (int i = 0; i < amount; i++) {
                restDamage += 1;
            }
        }

        if (AllZoneUtil.isCardInPlay("Pyromancer's Swath", source.getController())
                && (source.isInstant() || source.isSorcery())) {
            final int amount = source.getController().getCardsIn(Zone.Battlefield, "Pyromancer's Swath").size();
            for (int i = 0; i < amount; i++) {
                restDamage += 2;
            }
        }

        if (AllZoneUtil.isCardInPlay("Furnace of Rath")) {
            final int amount = AllZoneUtil.getCardsIn(Zone.Battlefield, "Furnace of Rath").size();
            for (int i = 0; i < amount; i++) {
                restDamage += restDamage;
            }
        }

        if (AllZoneUtil.isCardInPlay("Gratuitous Violence", source.getController()) && source.isCreature()) {
            final int amount = source.getController().getCardsIn(Zone.Battlefield, "Gratuitous Violence").size();
            for (int i = 0; i < amount; i++) {
                restDamage += restDamage;
            }
        }

        if (AllZoneUtil.isCardInPlay("Fire Servant", source.getController()) && source.isRed()
                && (source.isInstant() || source.isSorcery())) {
            final int amount = source.getController().getCardsIn(Zone.Battlefield, "Fire Servant").size();
            for (int i = 0; i < amount; i++) {
                restDamage *= 2;
            }
        }

        if (AllZoneUtil.isCardInPlay("Benevolent Unicorn") && source.isSpell()) {
            final int amount = AllZoneUtil.getCardsIn(Zone.Battlefield, "Benevolent Unicorn").size();
            for (int i = 0; i < amount; i++) {
                if (restDamage > 0) {
                    restDamage -= 1;
                }
            }
        }

        if (AllZoneUtil.isCardInPlay("Divine Presence") && (restDamage > 3)) {

            restDamage = 3;
        }

        if (AllZoneUtil.isCardInPlay("Forethought Amulet", this) && (source.isInstant() || source.isSorcery())
                && (restDamage > 2)) {

            restDamage = 2;
        }

        return restDamage;
    }

    /**
     * <p>
     * replaceDamage.
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
    public final int replaceDamage(final int damage, final Card source, final boolean isCombat) {

        final int restDamage = this.staticReplaceDamage(damage, source, isCombat);

        if (source.getName().equals("Szadek, Lord of Secrets") && isCombat) {
            source.addCounter(Counters.P1P1, restDamage);
            for (int i = 0; i < restDamage; i++) {
                final CardList lib = this.getCardsIn(Zone.Library);
                if (lib.size() > 0) {
                    AllZone.getGameAction().moveToGraveyard(lib.get(0));
                }
            }
            return 0;
        }

        if (AllZoneUtil.isCardInPlay("Crumbling Sanctuary")) {
            for (int i = 0; i < restDamage; i++) {
                final CardList lib = this.getCardsIn(Zone.Library);
                if (lib.size() > 0) {
                    AllZone.getGameAction().exile(lib.get(0));
                }
            }
            // return so things like Lifelink, etc do not trigger. This is a
            // replacement effect I think.
            return 0;
        }

        return restDamage;
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

        if (AllZoneUtil.isCardInPlay("Leyline of Punishment")) {
            return damage;
        }

        int restDamage = damage;

        // Purity has to stay here because it changes the game state
        if (AllZoneUtil.isCardInPlay("Purity", this) && !isCombat) {
            this.gainLife(restDamage, null);
            return 0;
        }

        restDamage = this.staticDamagePrevention(restDamage, source, isCombat);

        if (restDamage >= this.getPreventNextDamage()) {
            restDamage = restDamage - this.getPreventNextDamage();
            this.setPreventNextDamage(0);
        } else {
            this.setPreventNextDamage(this.getPreventNextDamage() - restDamage);
            restDamage = 0;
        }

        return restDamage;
    }

    /**
     * <p>
     * Setter for the field <code>assignedDamage</code>.
     * </p>
     * 
     * @param n
     *            a int.
     */
    public final void setAssignedDamage(final int n) {
        this.assignedDamage = n;
    }

    /**
     * <p>
     * addAssignedDamage.
     * </p>
     * 
     * @param n
     *            a int.
     */
    public final void addAssignedDamage(final int n) {
        this.assignedDamage += n;
    }

    /**
     * <p>
     * Getter for the field <code>assignedDamage</code>.
     * </p>
     * 
     * @return a int.
     */
    public final int getAssignedDamage() {
        return this.assignedDamage;
    }

    /**
     * <p>
     * addCombatDamage.
     * </p>
     * 
     * @param damage
     *            a int.
     * @param source
     *            a {@link forge.Card} object.
     */
    public final void addCombatDamage(final int damage, final Card source) {

        int damageToDo = damage;

        damageToDo = this.replaceDamage(damageToDo, source, true);
        damageToDo = this.preventDamage(damageToDo, source, true);

        this.addDamageAfterPrevention(damageToDo, source, true); // damage
                                                                 // prevention
        // is already
        // checked

        if (damageToDo > 0) {
            GameActionUtil.executeCombatDamageToPlayerEffects(this, source, damageToDo);
        }
    }

    // ////////////////////////
    //
    // methods for handling Poison counters
    //
    // ////////////////////////

    /**
     * <p>
     * addPoisonCounters.
     * </p>
     * 
     * @param num
     *            a int.
     */
    public final void addPoisonCounters(final int num, final Card source) {
        if (!this.hasKeyword("You can't get poison counters")) {
            this.poisonCounters += num;
            AllZone.getGameLog().add("Poison", this + " receives a poison counter from " + source, 3);
            this.updateObservers();
        }
    }

    /**
     * <p>
     * Setter for the field <code>poisonCounters</code>.
     * </p>
     * 
     * @param num
     *            a int.
     */
    public final void setPoisonCounters(final int num) {
        this.poisonCounters = num;
        this.updateObservers();
    }

    /**
     * <p>
     * Getter for the field <code>poisonCounters</code>.
     * </p>
     * 
     * @return a int.
     */
    public final int getPoisonCounters() {
        return this.poisonCounters;
    }

    /**
     * <p>
     * subtractPoisonCounters.
     * </p>
     * 
     * @param num
     *            a int.
     */
    public final void subtractPoisonCounters(final int num) {
        this.poisonCounters -= num;
        this.updateObservers();
    }

    /**
     * Gets the keywords.
     * 
     * @return the keywords
     */
    public final ArrayList<String> getKeywords() {
        return this.keywords;
    }

    /**
     * Sets the keywords.
     * 
     * @param keywords
     *            the new keywords
     */
    public final void setKeywords(final ArrayList<String> keywords) {
        this.keywords = keywords;
    }

    /**
     * Adds the keyword.
     * 
     * @param keyword
     *            the keyword
     */
    public final void addKeyword(final String keyword) {
        this.keywords.add(keyword);
    }

    /**
     * Removes the keyword.
     * 
     * @param keyword
     *            the keyword
     */
    public final void removeKeyword(final String keyword) {
        this.keywords.remove(keyword);
    }

    /*
     * (non-Javadoc)
     * 
     * @see forge.GameEntity#hasKeyword(java.lang.String)
     */
    /**
     * Checks for keyword.
     * 
     * @param keyword
     *            String
     * @return boolean
     */
    @Override
    public final boolean hasKeyword(final String keyword) {
        return this.keywords.contains(keyword);
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
        if (this.hasKeyword("Shroud") || (!this.isPlayer(sa.getActivatingPlayer()) && this.hasKeyword("Hexproof"))
                || this.hasProtectionFrom(sa.getSourceCard())) {
            return false;
        }

        return true;
    }

    /*
     * (non-Javadoc)
     * 
     * @see forge.GameEntity#hasProtectionFrom(forge.Card)
     */
    @Override
    public boolean hasProtectionFrom(final Card source) {
        if (this.getKeywords() != null) {
            final ArrayList<String> list = this.getKeywords();

            String kw = "";
            for (int i = 0; i < list.size(); i++) {
                kw = list.get(i);

                if (kw.equals("Protection from white") && source.isWhite()) {
                    return true;
                }
                if (kw.equals("Protection from blue") && source.isBlue()) {
                    return true;
                }
                if (kw.equals("Protection from black") && source.isBlack()) {
                    return true;
                }
                if (kw.equals("Protection from red") && source.isRed()) {
                    return true;
                }
                if (kw.equals("Protection from green") && source.isGreen()) {
                    return true;
                }

                if (kw.startsWith("Protection:")) { // uses isValid
                    final String characteristic = kw.split(":")[1];
                    final String[] characteristics = characteristic.split(",");
                    if (source.isValid(characteristics, this, null)) {
                        return true;
                    }
                }

            }
        }
        return false;
    }

    /**
     * <p>
     * canPlaySpells.
     * </p>
     * 
     * @return a boolean.
     */
    public final boolean canCastSpells() {
        return !this.keywords.contains("Can't cast spells");
    }

    /**
     * <p>
     * canPlayAbilities.
     * </p>
     * 
     * @return a boolean.
     */
    public final boolean canActivateAbilities() {
        return !this.keywords.contains("Can't activate abilities");
    }

    // //////////////////////////////
    // /
    // / replaces AllZone.getGameAction().draw* methods
    // /
    // //////////////////////////////

    /**
     * <p>
     * canDraw
     * </p>
     * .
     * 
     * @return true if a player can draw a card, false otherwise
     */
    public final boolean canDraw() {
        return !AllZoneUtil.isCardInPlay("Maralen of the Mornsong");
    }

    /**
     * <p>
     * mayDrawCard.
     * </p>
     * 
     * @return a CardList of cards actually drawn
     */
    public abstract CardList mayDrawCard();

    /**
     * <p>
     * mayDrawCards.
     * </p>
     * 
     * @param numCards
     *            a int.
     * @return a CardList of cards actually drawn
     */
    public abstract CardList mayDrawCards(int numCards);

    /**
     * <p>
     * drawCard.
     * </p>
     * 
     * @return a CardList of cards actually drawn
     */
    public final CardList drawCard() {
        return this.drawCards(1);
    }

    /**
     * <p>
     * drawCards.
     * </p>
     * 
     * @return a CardList of cards actually drawn
     */
    public final CardList drawCards() {
        return this.drawCards(1);
    }

    /**
     * <p>
     * dredge.
     * </p>
     * 
     * @return a boolean.
     */
    public abstract boolean dredge();

    /**
     * <p>
     * drawCards.
     * </p>
     * 
     * @param n
     *            a int.
     * @return a CardList of cards actually drawn
     */
    public final CardList drawCards(final int n) {
        return this.drawCards(n, false);
    }

    /**
     * <p>
     * drawCards.
     * </p>
     * 
     * @param n
     *            a int.
     * @param firstFromDraw
     *            true if this is the card drawn from that player's draw step
     *            each turn
     * @return a CardList of cards actually drawn
     */
    public final CardList drawCards(final int n, final boolean firstFromDraw) {
        final CardList drawn = new CardList();

        if (!this.canDraw()) {
            return drawn;
        }

        for (int i = 0; i < n; i++) {

            // TODO: multiple replacements need to be selected by the controller
            if (this.getDredge().size() != 0) {
                if (this.dredge()) {
                    continue;
                }
            }

            if (!firstFromDraw && AllZoneUtil.isCardInPlay("Chains of Mephistopheles")) {
                if (!this.getZone(Zone.Hand).isEmpty()) {
                    if (this.isHuman()) {
                        this.discardChainsOfMephistopheles();
                    } else { // Computer
                        this.discard(1, null, false);
                        // true causes this code not to be run again
                        drawn.addAll(this.drawCards(1, true));
                    }
                } else {
                    this.mill(1);
                }
            } else {
                drawn.addAll(this.doDraw());
            }
        }
        return drawn;
    }

    /**
     * <p>
     * doDraw.
     * </p>
     * 
     * @return a CardList of cards actually drawn
     */
    private CardList doDraw() {
        final CardList drawn = new CardList();
        final PlayerZone library = this.getZone(Constant.Zone.Library);
        if (library.size() != 0) {
            Card c = library.get(0);
            c = AllZone.getGameAction().moveToHand(c);

            this.setLastDrawnCard(c);
            c.setDrawnThisTurn(true);
            this.numDrawnThisTurn++;
            drawn.add(c);

            // Run triggers
            final HashMap<String, Object> runParams = new HashMap<String, Object>();
            runParams.put("Card", c);
            AllZone.getTriggerHandler().runTrigger("Drawn", runParams);
        }
        // lose:
        else if (!Constant.Runtime.DEV_MODE[0] || AllZone.getDisplay().canLoseByDecking()) {
            // if devMode is off, or canLoseByDecking is Enabled, run Lose
            // Condition
            if (!this.cantLose()) {
                this.loseConditionMet(GameLossReason.Milled, null);
                AllZone.getGameAction().checkStateEffects();
            }
        }
        return drawn;
    }

    /**
     * Returns PlayerZone corresponding to the given zone of game.
     * 
     * @param zone
     *            the zone
     * @return the zone
     */
    public final PlayerZone getZone(final Zone zone) {
        return this.zones.get(zone);
    }

    /**
     * gets a list of all cards in the requested zone. This function makes a
     * CardList from Card[].
     * 
     * @param zone
     *            the zone
     * @return a CardList with all the cards currently in requested zone
     */
    public final CardList getCardsIn(final Constant.Zone zone) {
        final Card[] cards = zone == Zone.Stack ? AllZone.getStackZone().getCards() : this.getZone(zone).getCards();
        return new CardList(cards);
    }

    /**
     * Gets the all cards.
     * 
     * @return the all cards
     */
    public final CardList getAllCards() {
        return this.getCardsIn(Player.ALL_ZONES);
    }

    /**
     * Gets the cards include phasing in.
     * 
     * @param zone
     *            the zone
     * @return the cards include phasing in
     */
    public final CardList getCardsIncludePhasingIn(final Constant.Zone zone) {
        final Card[] cards = zone == Zone.Stack ? AllZone.getStackZone().getCards() : this.getZone(zone)
                .getCards(false);
        return new CardList(cards);
    }

    /**
     * gets a list of first N cards in the requested zone. This function makes a
     * CardList from Card[].
     * 
     * @param zone
     *            the zone
     * @param n
     *            the n
     * @return a CardList with all the cards currently in requested zone
     */
    public final CardList getCardsIn(final Constant.Zone zone, final int n) {
        return new CardList(this.getZone(zone).getCards(n));
    }

    /**
     * gets a list of all cards in a given player's requested zones.
     * 
     * @param zones
     *            the zones
     * @return a CardList with all the cards currently in requested zones
     */
    public final CardList getCardsIn(final List<Constant.Zone> zones) {
        final CardList result = new CardList();
        for (final Constant.Zone z : zones) {
            if (this.getZone(z) != null) {
                result.addAll(this.getZone(z).getCards());
            }
        }
        return result;
    }

    /**
     * gets a list of all cards with requested cardName in a given player's
     * requested zone. This function makes a CardList from Card[].
     * 
     * @param zone
     *            the zone
     * @param cardName
     *            the card name
     * @return a CardList with all the cards currently in that player's library
     */
    public final CardList getCardsIn(final Constant.Zone zone, final String cardName) {
        return this.getCardsIn(zone).getName(cardName);
    }

    /**
     * <p>
     * getDredge.
     * </p>
     * 
     * @return a {@link forge.CardList} object.
     */
    protected final CardList getDredge() {
        final CardList dredge = new CardList();
        final CardList cl = this.getCardsIn(Zone.Graveyard);

        for (final Card c : cl) {
            final ArrayList<String> kw = c.getKeyword();
            for (int i = 0; i < kw.size(); i++) {
                if (kw.get(i).toString().startsWith("Dredge")) {
                    if (this.getCardsIn(Zone.Library).size() >= this.getDredgeNumber(c)) {
                        dredge.add(c);
                    }
                }
            }
        }
        return dredge;
    } // hasDredge()

    /**
     * <p>
     * getDredgeNumber.
     * </p>
     * 
     * @param c
     *            a {@link forge.Card} object.
     * @return a int.
     */
    protected final int getDredgeNumber(final Card c) {
        final ArrayList<String> a = c.getKeyword();
        for (int i = 0; i < a.size(); i++) {
            if (a.get(i).toString().startsWith("Dredge")) {
                final String s = a.get(i).toString();
                return Integer.parseInt("" + s.charAt(s.length() - 1));
            }
        }

        throw new RuntimeException("Input_Draw : getDredgeNumber() card doesn't have dredge - " + c.getName());
    } // getDredgeNumber()

    /**
     * <p>
     * resetNumDrawnThisTurn.
     * </p>
     */
    public final void resetNumDrawnThisTurn() {
        this.numDrawnThisTurn = 0;
    }

    /**
     * <p>
     * Getter for the field <code>numDrawnThisTurn</code>.
     * </p>
     * 
     * @return a int.
     */
    public final int getNumDrawnThisTurn() {
        return this.numDrawnThisTurn;
    }

    // //////////////////////////////
    // /
    // / replaces AllZone.getGameAction().discard* methods
    // /
    // //////////////////////////////

    /**
     * Discard_ chains_of_ mephistopheles.
     */
    protected abstract void discardChainsOfMephistopheles();

    /**
     * <p>
     * discard.
     * </p>
     * 
     * @param num
     *            a int.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @param duringResolution
     *            a boolean.
     * @return a {@link forge.CardList} object.
     */
    public abstract CardList discard(final int num, final SpellAbility sa, boolean duringResolution);

    /**
     * <p>
     * discard.
     * </p>
     * 
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @return a {@link forge.CardList} object.
     */
    public final CardList discard(final SpellAbility sa) {
        return this.discard(1, sa, false);
    }

    /**
     * <p>
     * discard.
     * </p>
     * 
     * @param c
     *            a {@link forge.Card} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @return a {@link forge.CardList} object.
     */
    public final CardList discard(final Card c, final SpellAbility sa) {
        this.doDiscard(c, sa);
        return new CardList(c);
    }

    /**
     * <p>
     * doDiscard.
     * </p>
     * 
     * @param c
     *            a {@link forge.Card} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     */
    protected final void doDiscard(final Card c, final SpellAbility sa) {
        // TODO: This line should be moved inside CostPayment somehow
        if (sa != null) {
            sa.addCostToHashList(c, "Discarded");
        }

        AllZone.getGameAction().discardMadness(c);

        if ((c.hasKeyword("If a spell or ability an opponent controls causes "
                + "you to discard CARDNAME, put it onto the battlefield instead of putting it into your graveyard.") || c
                .hasKeyword("If a spell or ability an opponent controls causes "
                        + "you to discard CARDNAME, put it onto the battlefield with two +1/+1 "
                        + "counters on it instead of putting it into your graveyard."))
                && (null != sa) && !c.getController().equals(sa.getSourceCard().getController())) {
            AllZone.getGameAction().discardPutIntoPlayInstead(c);
        } else {
            AllZone.getGameAction().moveToGraveyard(c);
        }

        // Run triggers
        Card cause = null;
        if (sa != null) {
            cause = sa.getSourceCard();
        }
        final HashMap<String, Object> runParams = new HashMap<String, Object>();
        runParams.put("Player", this);
        runParams.put("Card", c);
        runParams.put("Cause", cause);
        AllZone.getTriggerHandler().runTrigger("Discarded", runParams);

    } // end doDiscard

    /**
     * <p>
     * discardHand.
     * </p>
     * 
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @return the card list
     */
    public final CardList discardHand(final SpellAbility sa) {
        final CardList list = this.getCardsIn(Zone.Hand);
        this.discardRandom(list.size(), sa);
        return list;
    }

    /**
     * <p>
     * discardRandom.
     * </p>
     * 
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @return a CardList of cards discarded
     */
    public final CardList discardRandom(final SpellAbility sa) {
        return this.discardRandom(1, sa);
    }

    /**
     * <p>
     * discardRandom.
     * </p>
     * 
     * @param num
     *            a int.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @return a CardList of cards discarded
     */
    public final CardList discardRandom(final int num, final SpellAbility sa) {
        return this.discardRandom(num, sa, "Card");
    }

    /**
     * <p>
     * discardRandom.
     * </p>
     * 
     * @param num
     *            a int.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @param valid
     *            a valid expression
     * @return a CardList of cards discarded
     */
    public final CardList discardRandom(final int num, final SpellAbility sa, final String valid) {
        final CardList discarded = new CardList();
        for (int i = 0; i < num; i++) {
            CardList list = this.getCardsIn(Zone.Hand);
            list = list.getValidCards(valid, sa.getSourceCard().getController(), sa.getSourceCard());
            if (list.size() != 0) {
                final Card disc = CardUtil.getRandom(list.toArray());
                discarded.add(disc);
                this.doDiscard(disc, sa);
            }
        }
        return discarded;
    }

    /**
     * <p>
     * discardUnless.
     * </p>
     * 
     * @param num
     *            a int.
     * @param uType
     *            a {@link java.lang.String} object.
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     */
    public abstract void discardUnless(int num, String uType, SpellAbility sa);

    /**
     * <p>
     * mill.
     * </p>
     * 
     * @param n
     *            a int.
     * @return the card list
     */
    public final CardList mill(final int n) {
        return this.mill(n, Constant.Zone.Graveyard, false);
    }

    /**
     * <p>
     * mill.
     * </p>
     * 
     * @param n
     *            a int.
     * @param zone
     *            a {@link java.lang.String} object.
     * @param bottom
     *            a boolean.
     * @return the card list
     */
    public final CardList mill(final int n, final Constant.Zone zone, final boolean bottom) {
        final CardList lib = this.getCardsIn(Zone.Library);
        final CardList milled = new CardList();

        final int max = Math.min(n, lib.size());

        final Zone destination = this.getZone(zone).getZoneType();

        for (int i = 0; i < max; i++) {
            if (bottom) {
                milled.add(AllZone.getGameAction().moveTo(destination, lib.get(lib.size() - 1)));
            } else {
                milled.add(AllZone.getGameAction().moveTo(destination, lib.get(i)));
            }
        }

        return milled;
    }

    /**
     * <p>
     * handToLibrary.
     * </p>
     * 
     * @param numToLibrary
     *            a int.
     * @param libPos
     *            a {@link java.lang.String} object.
     */
    public abstract void handToLibrary(final int numToLibrary, String libPos);

    // //////////////////////////////
    /**
     * <p>
     * shuffle.
     * </p>
     */
    public final void shuffle() {
        final PlayerZone library = this.getZone(Constant.Zone.Library);
        final Card[] c = this.getCardsIn(Zone.Library).toArray();

        if (c.length <= 1) {
            return;
        }

        final ArrayList<Object> list = new ArrayList<Object>(Arrays.asList(c));
        // overdone but wanted to make sure it was really random
        final Random random = MyRandom.getRandom();
        Collections.shuffle(list, random);
        Collections.shuffle(list, random);
        Collections.shuffle(list, random);
        Collections.shuffle(list, random);
        Collections.shuffle(list, random);
        Collections.shuffle(list, random);

        Object o;
        for (int i = 0; i < list.size(); i++) {
            o = list.remove(random.nextInt(list.size()));
            list.add(random.nextInt(list.size()), o);
        }

        Collections.shuffle(list, random);
        Collections.shuffle(list, random);
        Collections.shuffle(list, random);
        Collections.shuffle(list, random);
        Collections.shuffle(list, random);
        Collections.shuffle(list, random);

        list.toArray(c);
        library.setCards(c);

        // Run triggers
        final HashMap<String, Object> runParams = new HashMap<String, Object>();
        runParams.put("Player", this);
        AllZone.getTriggerHandler().runTrigger("Shuffled", runParams);

    } // shuffle
      // //////////////////////////////

    // //////////////////////////////
    /**
     * <p>
     * doScry.
     * </p>
     * 
     * @param topN
     *            a {@link forge.CardList} object.
     * @param n
     *            a int.
     */
    protected abstract void doScry(CardList topN, int n);

    /**
     * <p>
     * scry.
     * </p>
     * 
     * @param numScry
     *            a int.
     */
    public final void scry(int numScry) {
        final CardList topN = new CardList();
        final PlayerZone library = this.getZone(Constant.Zone.Library);
        numScry = Math.min(numScry, library.size());
        for (int i = 0; i < numScry; i++) {
            topN.add(library.get(i));
        }
        this.doScry(topN, topN.size());
    }

    // /////////////////////////////

    /**
     * <p>
     * playLand.
     * </p>
     * 
     * @param land
     *            a {@link forge.Card} object.
     */
    public final void playLand(final Card land) {
        if (this.canPlayLand()) {
            AllZone.getGameAction().moveToPlay(land);
            CardFactoryUtil.playLandEffects(land);
            this.numLandsPlayed++;

            // check state effects for static animate (Living Lands, Conversion,
            // etc...)
            AllZone.getGameAction().checkStateEffects();

            //add to log
            AllZone.getGameLog().add("Land", this + " played " + land, 2);

            // Run triggers
            final HashMap<String, Object> runParams = new HashMap<String, Object>();
            runParams.put("Card", land);
            AllZone.getTriggerHandler().runTrigger("LandPlayed", runParams);
        }

        AllZone.getStack().unfreezeStack();
    }

    /**
     * <p>
     * canPlayLand.
     * </p>
     * 
     * @return a boolean.
     */
    public final boolean canPlayLand() {

        // CantBeCast static abilities
        final CardList allp = AllZoneUtil.getCardsIn(Zone.Battlefield);
        for (final Card ca : allp) {
            final ArrayList<StaticAbility> staticAbilities = ca.getStaticAbilities();
            for (final StaticAbility stAb : staticAbilities) {
                if (stAb.applyAbility("CantPlayLand", null, this)) {
                    return false;
                }
            }
        }

        return Phase.canCastSorcery(this)
                && ((this.numLandsPlayed < this.maxLandsToPlay) || (this.getCardsIn(Zone.Battlefield, "Fastbond")
                        .size() > 0));
    }

    /**
     * Gets the mana pool.
     * 
     * @return the mana pool
     */
    public final ManaPool getManaPool() {
        return this.manaPool;
    }

    // /////////////////////////////
    // //
    // // properties about the player and his/her cards/game status
    // //
    // /////////////////////////////
    /**
     * <p>
     * hasPlaneswalker.
     * </p>
     * 
     * @return a boolean.
     */
    public final boolean hasPlaneswalker() {
        return null != this.getPlaneswalker();
    }

    /**
     * <p>
     * getPlaneswalker.
     * </p>
     * 
     * @return a {@link forge.Card} object.
     */
    public final Card getPlaneswalker() {
        final CardList c = this.getCardsIn(Zone.Battlefield).getType("Planeswalker");
        if ((null != c) && (c.size() > 0)) {
            return c.get(0);
        } else {
            return null;
        }
    }

    /**
     * <p>
     * Getter for the field <code>numPowerSurgeLands</code>.
     * </p>
     * 
     * @return a int.
     */
    public final int getNumPowerSurgeLands() {
        return this.numPowerSurgeLands;
    }

    /**
     * <p>
     * Setter for the field <code>numPowerSurgeLands</code>.
     * </p>
     * 
     * @param n
     *            a int.
     * @return a int.
     */
    public final int setNumPowerSurgeLands(final int n) {
        this.numPowerSurgeLands = n;
        return this.numPowerSurgeLands;
    }

    /**
     * <p>
     * Getter for the field <code>lastDrawnCard</code>.
     * </p>
     * 
     * @return a {@link forge.Card} object.
     */
    public final Card getLastDrawnCard() {
        return this.lastDrawnCard;
    }

    /**
     * <p>
     * Setter for the field <code>lastDrawnCard</code>.
     * </p>
     * 
     * @param c
     *            a {@link forge.Card} object.
     * @return a {@link forge.Card} object.
     */
    public final Card setLastDrawnCard(final Card c) {
        this.lastDrawnCard = c;
        return this.lastDrawnCard;
    }

    /**
     * <p>
     * resetLastDrawnCard.
     * </p>
     * 
     * @return a {@link forge.Card} object.
     */
    public final Card resetLastDrawnCard() {
        final Card old = this.lastDrawnCard;
        this.lastDrawnCard = null;
        return old;
    }

    /**
     * <p>
     * skipNextUntap.
     * </p>
     * 
     * @return a boolean.
     */
    public final boolean skipNextUntap() {
        return this.skipNextUntap;
    }

    /**
     * <p>
     * Setter for the field <code>skipNextUntap</code>.
     * </p>
     * 
     * @param b
     *            a boolean.
     */
    public final void setSkipNextUntap(final boolean b) {
        this.skipNextUntap = b;
    }

    /**
     * <p>
     * Getter for the field <code>slowtripList</code>.
     * </p>
     * 
     * @return a {@link forge.CardList} object.
     */
    public final CardList getSlowtripList() {
        return this.slowtripList;
    }

    /**
     * <p>
     * clearSlowtripList.
     * </p>
     */
    public final void clearSlowtripList() {
        this.slowtripList.clear();
    }

    /**
     * <p>
     * addSlowtripList.
     * </p>
     * 
     * @param card
     *            a {@link forge.Card} object.
     */
    public final void addSlowtripList(final Card card) {
        this.slowtripList.add(card);
    }

    /**
     * <p>
     * getTurn.
     * </p>
     * 
     * @return a int.
     */
    public final int getTurn() {
        return this.nTurns;
    }

    /**
     * <p>
     * incrementTurn.
     * </p>
     */
    public final void incrementTurn() {
        this.nTurns++;
    }

    // //////////////////////////////
    /**
     * <p>
     * sacrificePermanent.
     * </p>
     * 
     * @param prompt
     *            a {@link java.lang.String} object.
     * @param choices
     *            a {@link forge.CardList} object.
     */
    public abstract void sacrificePermanent(String prompt, CardList choices);

    /**
     * <p>
     * sacrificeCreature.
     * </p>
     */
    public final void sacrificeCreature() {
        final CardList choices = AllZoneUtil.getCreaturesInPlay(this);
        this.sacrificePermanent("Select a creature to sacrifice.", choices);
    }

    /**
     * <p>
     * sacrificeCreature.
     * </p>
     * 
     * @param choices
     *            a {@link forge.CardList} object.
     */
    public final void sacrificeCreature(final CardList choices) {
        this.sacrificePermanent("Select a creature to sacrifice.", choices);
    }

    // Game win/loss

    /**
     * <p>
     * Getter for the field <code>altWin</code>.
     * </p>
     * 
     * @return a boolean.
     */
    public final boolean getAltWin() {
        return this.altWin;
    }

    /**
     * <p>
     * Getter for the field <code>winCondition</code>.
     * </p>
     * 
     * @return a {@link java.lang.String} object.
     */
    public final String getWinConditionSource() {
        return this.altWinSourceName;
    }

    /**
     * <p>
     * Getter for the field <code>loseCondition</code>.
     * </p>
     * 
     * @return a {@link java.lang.String} object.
     */
    public final GameLossReason getLossState() {
        return this.lossState;
    }

    /**
     * Gets the loss condition source.
     * 
     * @return the loss condition source
     */
    public final String getLossConditionSource() {
        return this.loseConditionSpell;
    }

    /**
     * <p>
     * altWinConditionMet.
     * </p>
     * 
     * @param sourceName
     *            the source name
     */
    public final void altWinBySpellEffect(final String sourceName) {
        if (this.cantWin()) {
            System.out.println("Tried to win, but currently can't.");
            return;
        }
        this.altWin = true;
        this.altWinSourceName = sourceName;
    }

    /**
     * <p>
     * altLoseConditionMet.
     * </p>
     * 
     * @param state
     *            the state
     * @param spellName
     *            the spell name
     * @return a boolean.
     */
    public final boolean loseConditionMet(final GameLossReason state, final String spellName) {
        if (this.cantLose()) {
            System.out.println("Tried to lose, but currently can't.");
            return false;
        }
        this.lossState = state;
        this.loseConditionSpell = spellName;
        return true;
    }

    /**
     * Concede.
     */
    public final void concede() { // No cantLose checks - just lose
        this.lossState = GameLossReason.Conceded;
        this.loseConditionSpell = null;
    }

    /**
     * <p>
     * cantLose.
     * </p>
     * 
     * @return a boolean.
     */
    public final boolean cantLose() {
        if (this.lossState == GameLossReason.Conceded) {
            return false;
        }

        CardList list = this.getCardsIn(Zone.Battlefield);
        list = list.getKeyword("You can't lose the game.");

        if (list.size() > 0) {
            return true;
        }

        CardList oppList = this.getOpponent().getCardsIn(Zone.Battlefield);
        oppList = oppList.getKeyword("Your opponents can't lose the game.");

        return oppList.size() > 0;
    }

    /**
     * <p>
     * cantLoseForZeroOrLessLife.
     * </p>
     * 
     * @return a boolean.
     */
    public final boolean cantLoseForZeroOrLessLife() {
        CardList list = this.getCardsIn(Zone.Battlefield);
        list = list.getKeyword("You don't lose the game for having 0 or less life.");

        return list.size() > 0;
    }

    /**
     * <p>
     * cantWin.
     * </p>
     * 
     * @return a boolean.
     */
    public final boolean cantWin() {
        CardList list = this.getCardsIn(Zone.Battlefield);
        list = list.getKeyword("You can't win the game.");

        if (list.size() > 0) {
            return true;
        }

        CardList oppList = this.getOpponent().getCardsIn(Zone.Battlefield);
        oppList = oppList.getKeyword("Your opponents can't win the game.");

        return oppList.size() > 0;
    }

    /**
     * <p>
     * hasLost.
     * </p>
     * 
     * @return a boolean.
     */
    public final boolean hasLost() {

        if (this.cantLose()) {
            return false;
        }

        if (this.lossState != GameLossReason.DidNotLoseYet) {
            return true;
        }

        if (this.poisonCounters >= 10) {
            this.loseConditionMet(GameLossReason.Poisoned, null);
            return true;
        }

        if (this.cantLoseForZeroOrLessLife()) {
            return false;
        }

        final boolean hasNoLife = this.getLife() <= 0;
        if (hasNoLife) {
            this.loseConditionMet(GameLossReason.LifeReachedZero, null);
            return true;
        }

        return false;
    }

    /**
     * <p>
     * hasWon.
     * </p>
     * 
     * @return a boolean.
     */
    public final boolean hasWon() {
        if (this.cantWin()) {
            return false;
        }

        return this.altWin;
    }

    /**
     * <p>
     * hasMetalcraft.
     * </p>
     * 
     * @return a boolean.
     */
    public final boolean hasMetalcraft() {
        final CardList list = this.getCardsIn(Zone.Battlefield).getType("Artifact");
        return list.size() >= 3;
    }

    /**
     * <p>
     * hasThreshold.
     * </p>
     * 
     * @return a boolean.
     */
    public final boolean hasThreshold() {
        return this.getZone(Zone.Graveyard).getCards().length >= 7;
    }

    /**
     * <p>
     * hasHellbent.
     * </p>
     * 
     * @return a boolean.
     */
    public final boolean hasHellbent() {
        return this.getZone(Zone.Hand).getCards().length == 0;
    }

    /**
     * <p>
     * hasLandfall.
     * </p>
     * 
     * @return a boolean.
     */
    public final boolean hasLandfall() {
        final CardList list = ((DefaultPlayerZone) this.getZone(Zone.Battlefield)).getCardsAddedThisTurn(null).getType(
                "Land");
        return !list.isEmpty();
    }

    /**
     * <p>
     * hasProwl.
     * </p>
     * 
     * @param type
     *            the type
     * @return a boolean.
     */
    public final boolean hasProwl(final String type) {
        return this.prowl.contains(type);
    }

    /**
     * Adds the prowl type.
     * 
     * @param type
     *            the type
     */
    public final void addProwlType(final String type) {
        this.prowl.add(type);
    }

    /**
     * Reset prowl.
     */
    public final void resetProwl() {
        this.prowl = new ArrayList<String>();
    }

    /*
     * (non-Javadoc)
     * 
     * @see forge.GameEntity#isValid(java.lang.String, forge.Player, forge.Card)
     */
    @Override
    public final boolean isValid(final String restriction, final Player sourceController, final Card source) {

        final String[] incR = restriction.split("\\.");

        if (!incR[0].equals("Player") && !(incR[0].equals("Opponent") && !this.equals(sourceController))
                && !(incR[0].equals("You") && this.equals(sourceController))) {
            return false;
        }

        if (incR.length > 1) {
            final String excR = incR[1];
            final String[] exR = excR.split("\\+"); // Exclusive Restrictions
                                                    // are ...
            for (int j = 0; j < exR.length; j++) {
                if (!this.hasProperty(exR[j], sourceController, source)) {
                    return false;
                }
            }
        }

        return true;
    }

    /*
     * (non-Javadoc)
     * 
     * @see forge.GameEntity#hasProperty(java.lang.String, forge.Player,
     * forge.Card)
     */
    @Override
    public final boolean hasProperty(final String property, final Player sourceController, final Card source) {

        if (property.equals("You")) {
            if (!this.equals(sourceController)) {
                return false;
            }
        } else if (property.equals("Opponent")) {
            if (this.equals(sourceController)) {
                return false;
            }
        } else if (property.equals("wasDealtDamageBySourceThisGame")) {
            if (this.isHuman() && !source.getDealtDmgToHumanThisGame()) {
                return false;
            }
            if (this.isComputer() && !source.getDealtDmgToComputerThisGame()) {
                return false;
            }
        } else if (property.equals("IsRemembered")) {
            if (!source.getRemembered().contains(this)) {
                return false;
            }
        }

        return true;
    }

    private ArrayList<HandSizeOp> handSizeOperations;

    /**
     * <p>
     * getMaxHandSize.
     * </p>
     * 
     * @return a int.
     */
    public final int getMaxHandSize() {

        int ret = 7;
        for (int i = 0; i < this.handSizeOperations.size(); i++) {
            if (this.handSizeOperations.get(i).getMode().equals("=")) {
                ret = this.handSizeOperations.get(i).getAmount();
            } else if (this.handSizeOperations.get(i).getMode().equals("+") && (ret >= 0)) {
                ret = ret + this.handSizeOperations.get(i).getAmount();
            } else if (this.handSizeOperations.get(i).getMode().equals("-") && (ret >= 0)) {
                ret = ret - this.handSizeOperations.get(i).getAmount();
                if (ret < 0) {
                    ret = 0;
                }
            }
        }
        return ret;
    }

    /**
     * <p>
     * sortHandSizeOperations.
     * </p>
     */
    public final void sortHandSizeOperations() {
        if (this.handSizeOperations.size() < 2) {
            return;
        }

        int changes = 1;

        while (changes > 0) {
            changes = 0;
            for (int i = 1; i < this.handSizeOperations.size(); i++) {
                if (this.handSizeOperations.get(i).getHsTimeStamp() < this.handSizeOperations.get(i - 1)
                        .getHsTimeStamp()) {
                    final HandSizeOp tmp = this.handSizeOperations.get(i);
                    this.handSizeOperations.set(i, this.handSizeOperations.get(i - 1));
                    this.handSizeOperations.set(i - 1, tmp);
                    changes++;
                }
            }
        }
    }

    /**
     * <p>
     * addHandSizeOperation.
     * </p>
     * 
     * @param theNew
     *            a {@link forge.HandSizeOp} object.
     */
    public final void addHandSizeOperation(final HandSizeOp theNew) {
        this.handSizeOperations.add(theNew);
    }

    /**
     * <p>
     * removeHandSizeOperation.
     * </p>
     * 
     * @param timestamp
     *            a int.
     */
    public final void removeHandSizeOperation(final int timestamp) {
        for (int i = 0; i < this.handSizeOperations.size(); i++) {
            if (this.handSizeOperations.get(i).getHsTimeStamp() == timestamp) {
                this.handSizeOperations.remove(i);
                break;
            }
        }
    }

    /**
     * <p>
     * clearHandSizeOperations.
     * </p>
     */
    public final void clearHandSizeOperations() {
        this.handSizeOperations.clear();
    }

    /** Constant <code>NextHandSizeStamp=0</code>. */
    private static int nextHandSizeStamp = 0;

    /**
     * <p>
     * getHandSizeStamp.
     * </p>
     * 
     * @return a int.
     */
    public static int getHandSizeStamp() {
        return Player.nextHandSizeStamp++;
    }

    /**
     * <p>
     * Getter for the field <code>maxLandsToPlay</code>.
     * </p>
     * 
     * @return a int.
     */
    public final int getMaxLandsToPlay() {
        return this.maxLandsToPlay;
    }

    /**
     * <p>
     * Setter for the field <code>maxLandsToPlay</code>.
     * </p>
     * 
     * @param n
     *            a int.
     */
    public final void setMaxLandsToPlay(final int n) {
        this.maxLandsToPlay = n;
    }

    /**
     * <p>
     * addMaxLandsToPlay.
     * </p>
     * 
     * @param n
     *            a int.
     */
    public final void addMaxLandsToPlay(final int n) {
        this.maxLandsToPlay += n;
    }

    /**
     * <p>
     * Getter for the field <code>numLandsPlayed</code>.
     * </p>
     * 
     * @return a int.
     */
    public final int getNumLandsPlayed() {
        return this.numLandsPlayed;
    }

    /**
     * <p>
     * Setter for the field <code>numLandsPlayed</code>.
     * </p>
     * 
     * @param n
     *            a int.
     */
    public final void setNumLandsPlayed(final int n) {
        this.numLandsPlayed = n;
    }

    // //////////////////////////////
    //
    // Clash
    //
    // ///////////////////////////////

    /**
     * <p>
     * clashWithOpponent.
     * </p>
     * 
     * @param source
     *            a {@link forge.Card} object.
     * @return a boolean.
     */
    public final boolean clashWithOpponent(final Card source) {
        /*
         * Each clashing player reveals the top card of his or her library, then
         * puts that card on the top or bottom. A player wins if his or her card
         * had a higher mana cost.
         * 
         * Clash you win or win you don't. There is no tie.
         */
        final Player player = source.getController();
        final Player opponent = player.getOpponent();
        final Constant.Zone lib = Constant.Zone.Library;

        final PlayerZone pLib = player.getZone(lib);
        final PlayerZone oLib = opponent.getZone(lib);

        final StringBuilder reveal = new StringBuilder();

        Card pCard = null;
        Card oCard = null;

        if (pLib.size() > 0) {
            pCard = pLib.get(0);
        }
        if (oLib.size() > 0) {
            oCard = oLib.get(0);
        }

        if ((pLib.size() == 0) && (oLib.size() == 0)) {
            return false;
        } else if (pLib.size() == 0) {
            opponent.clashMoveToTopOrBottom(oCard);
            return false;
        } else if (oLib.size() == 0) {
            player.clashMoveToTopOrBottom(pCard);
            return true;
        } else {
            final int pCMC = CardUtil.getConvertedManaCost(pCard);
            final int oCMC = CardUtil.getConvertedManaCost(oCard);
            reveal.append(player).append(" reveals: ").append(pCard.getName()).append(".  CMC = ").append(pCMC);
            reveal.append("\r\n");
            reveal.append(opponent).append(" reveals: ").append(oCard.getName()).append(".  CMC = ").append(oCMC);
            reveal.append("\r\n\r\n");
            if (pCMC > oCMC) {
                reveal.append(player).append(" wins clash.");
            } else {
                reveal.append(player).append(" loses clash.");
            }
            JOptionPane.showMessageDialog(null, reveal.toString(), source.getName(), JOptionPane.PLAIN_MESSAGE);
            player.clashMoveToTopOrBottom(pCard);
            opponent.clashMoveToTopOrBottom(oCard);
            // JOptionPane.showMessageDialog(null, reveal.toString(),
            // source.getName(), JOptionPane.PLAIN_MESSAGE);
            return pCMC > oCMC;
        }
    }

    /**
     * <p>
     * clashMoveToTopOrBottom.
     * </p>
     * 
     * @param c
     *            a {@link forge.Card} object.
     */
    protected abstract void clashMoveToTopOrBottom(Card c);

    /**
     * a Player or Planeswalker that this Player must attack if able in an
     * upcoming combat. This is cleared at the end of each combat.
     * 
     * @param o
     *            Player or Planeswalker (Card) to attack
     * 
     * @since 1.1.01
     */
    public final void setMustAttackEntity(final Object o) {
        this.mustAttackEntity = o;
    }

    /**
     * get the Player object or Card (Planeswalker) object that this Player must
     * attack this combat.
     * 
     * @return the Player or Card (Planeswalker)
     * @since 1.1.01
     */
    public final Object getMustAttackEntity() {
        return this.mustAttackEntity;
    }

    // //////////////////////////////
    //
    // generic Object overrides
    //
    // ///////////////////////////////

    /** {@inheritDoc} */
    @Override
    public final boolean equals(final Object o) {
        if (o instanceof Player) {
            final Player p1 = (Player) o;
            return p1.getName().equals(this.getName());
        } else {
            return false;
        }
    }

    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        return (41 * (41 + this.getName().hashCode()));
    }
}
