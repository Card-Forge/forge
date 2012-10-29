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
package forge.game.player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import javax.swing.JOptionPane;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import forge.Card;

import forge.CardLists;
import forge.CardPredicates;
import forge.CardPredicates.Presets;
import forge.CardUtil;
import forge.Constant;
import forge.Constant.Preferences;
import forge.Counters;
import forge.GameActionUtil;
import forge.GameEntity;
import forge.Singletons;
import forge.card.cardfactory.CardFactoryUtil;
import forge.card.mana.ManaPool;
import forge.card.replacement.ReplacementResult;
import forge.card.spellability.SpellAbility;
import forge.card.staticability.StaticAbility;
import forge.card.trigger.TriggerType;
import forge.game.GameLossReason;
import forge.game.GameState;
import forge.game.phase.PhaseHandler;
import forge.game.zone.PlayerZone;
import forge.game.zone.PlayerZoneBattlefield;
import forge.game.zone.ZoneType;
import forge.gui.GuiChoose;
import forge.properties.ForgePreferences.FPref;
import forge.util.MyRandom;

/**
 * <p>
 * Abstract Player class.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 */
public abstract class Player extends GameEntity implements Comparable<Player> {
    /** The poison counters. */
    private int poisonCounters = 0;

    /** The life. */
    private int life = 20;

    /** The life this player started the game with. */
    private int startingLife = 20;

    /** The assigned damage. */
    private final Map<Card, Integer> assignedDamage = new HashMap<Card, Integer>();

    /** The life lost this turn. */
    private int lifeLostThisTurn = 0;

    /** The num power surge lands. */
    private int numPowerSurgeLands;

    /** The prowl. */
    private ArrayList<String> prowl = new ArrayList<String>();

    /** The max lands to play. */
    private int maxLandsToPlay = 1;

    /** The num lands played. */
    private int numLandsPlayed = 0;

    /** The max hand size. */
    private int maxHandSize = 7;

    /** The last drawn card. */
    private Card lastDrawnCard = null;

    /** The num drawn this turn. */
    private int numDrawnThisTurn = 0;

    /** The slowtrip list. */
    private List<Card> slowtripList = new ArrayList<Card>();

    /** The keywords. */
    private ArrayList<String> keywords = new ArrayList<String>();

    /** The mana pool. */
    private ManaPool manaPool = new ManaPool(this);

    /** The must attack entity. */
    private GameEntity mustAttackEntity = null;

    /** The attackedWithCreatureThisTurn. */
    private boolean attackedWithCreatureThisTurn = false;

    /** The playerAttackCountThisTurn. */
    private int attackersDeclaredThisTurn = 0;

    /** The zones. */
    private final Map<ZoneType, PlayerZone> zones = new EnumMap<ZoneType, PlayerZone>(ZoneType.class);

    private PlayerStatistics stats = new PlayerStatistics();

    /** The Constant ALL_ZONES. */
    public static final List<ZoneType> ALL_ZONES = Collections.unmodifiableList(Arrays.asList(ZoneType.Battlefield,
            ZoneType.Library, ZoneType.Graveyard, ZoneType.Hand, ZoneType.Exile, ZoneType.Command, ZoneType.Ante));

    
    private final PlayerController controller;
    
    protected final LobbyPlayer lobbyPlayer;
    protected final GameState game; 
    
    public final PlayerOutcome getOutcome() {
        return stats.getOutcome();
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
    public Player(LobbyPlayer lobbyPlayer0, GameState game0) {
        lobbyPlayer = lobbyPlayer0;
        game = game0;
        for (final ZoneType z : Player.ALL_ZONES) {
            final PlayerZone toPut = z == ZoneType.Battlefield 
                    ? new PlayerZoneBattlefield(z, this)
                    : new PlayerZone(z, this);
            this.zones.put(z, toPut);
        }
        this.setName(lobbyPlayer.getName());
        controller = new PlayerController(this);
    }

    public final PlayerStatistics getStats() {
        return stats;
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
    //@Deprecated
    public abstract boolean isComputer();
    public abstract PlayerType getType();

    /**
     * <p>
     * getOpponent. Used by current-generation AI. 
     * </p>
     * 
     * @return a {@link forge.game.player.Player} object.
     */
    public final Player getOpponent() {
        Player otherType = null;
        Player justAnyone = null;
        for (Player p : game.getPlayers()) {
            if ( p == this ) continue;
            justAnyone = p;
            if( otherType == null && p.getType() != this.getType() ) otherType = p;
        }
        return otherType != null ? otherType : justAnyone; 
    }
    
    /**
     * 
     * returns all opponents
     * Should keep player relations somewhere in the match structure 
     * @return
     */
    public final List<Player> getOpponents() {
        List<Player> result = new ArrayList<Player>();
        for (Player p : game.getPlayers()) {
            if (p == this || p.getType() == this.getType())
                continue;
            result.add(p);
        }
        return result;
    }
    
    /**
     * returns allied players
     * Should keep player relations somewhere in the match structure
     * @return
     */
    public final List<Player> getAllies() {
        List<Player> result = new ArrayList<Player>();
        for (Player p : game.getPlayers()) {
            if (p == this || p.getType() != this.getType())
                continue;
            result.add(p);
        }
        return result;
    }
    
    

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
            change = (this.loseLife(this.life - newLife, source) > 0);
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
     * Sets the starting life for a game. Should only be called from
     * newGame()'s.
     * 
     * @param startLife
     *            a int.
     */
    public final void setStartingLife(final int startLife) {
        this.startingLife = startLife;
        this.life = startLife;
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
     * Getter for the field <code>startingLife</code>.
     * </p>
     * 
     * @return a int.
     */
    public final int getStartingLife() {
        return this.startingLife;
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

        // Run any applicable replacement effects.
        final HashMap<String, Object> repParams = new HashMap<String, Object>();
        repParams.put("Event", "GainLife");
        repParams.put("Affected", this);
        repParams.put("LifeGained", toGain);
        if (game.getReplacementHandler().run(repParams) != ReplacementResult.NotReplaced) {
            return false;
        }

        boolean newLifeSet = false;
        if (!this.canGainLife()) {
            return false;
        }
        final int lifeGain = toGain;

        if (lifeGain > 0) {
            this.addLife(lifeGain);
            newLifeSet = true;
            this.updateObservers();

            // Run triggers
            final HashMap<String, Object> runParams = new HashMap<String, Object>();
            runParams.put("Player", this);
            runParams.put("LifeAmount", lifeGain);
            game.getTriggerHandler().runTrigger(TriggerType.LifeGained, runParams);
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
        if (this.hasKeyword("You can't gain life.") || this.hasKeyword("Your life total can't change.")) {
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
     * @return an int.
     */
    public final int loseLife(final int toLose, final Card c) {
        int lifeLost = 0;
        if (!this.canLoseLife()) {
            return 0;
        }
        if (toLose > 0) {
            this.subtractLife(toLose);
            lifeLost = toLose;
            this.updateObservers();
        } else if (toLose == 0) {
            // Rule 118.4
            // this is for players being able to pay 0 life
            // nothing to do
        } else {
            System.out.println("Player - trying to lose negative life");
            return 0;
        }

        this.lifeLostThisTurn += toLose;

        // Run triggers
        final HashMap<String, Object> runParams = new HashMap<String, Object>();
        runParams.put("Player", this);
        runParams.put("LifeAmount", toLose);
        game.getTriggerHandler().runTrigger(TriggerType.LifeLost, runParams);

        return lifeLost;
    }

    /**
     * <p>
     * canLoseLife.
     * </p>
     * 
     * @return a boolean.
     */
    public final boolean canLoseLife() {
        if (this.hasKeyword("Your life total can't change.")) {
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
        if ((lifePayment > 0) && this.hasKeyword("Your life total can't change.")) {
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
            return (this.loseLife(lifePayment, source) > 0);
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
     * @return whether or not damage was dealt
     */
    @Override
    public final boolean addDamageAfterPrevention(final int damage, final Card source, final boolean isCombat) {
        final int damageToDo = damage;

        if (damageToDo == 0) {
            return false;
        }

        source.addDealtDamageToPlayerThisTurn(this.getName(), damageToDo);

        boolean infect = source.hasKeyword("Infect")
                || this.hasKeyword("All damage is dealt to you as though its source had infect.");

        if (infect) {
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

        this.addAssignedDamage(damageToDo, source);
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
        game.getTriggerHandler().runTrigger(TriggerType.DamageDone, runParams);

        return true;
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

        if (game.isCardInPlay("Leyline of Punishment")) {
            return damage;
        }

        if (isCombat && game.getPhaseHandler().isPreventCombatDamageThisTurn()) {
            return 0;
        }

        if (this.hasProtectionFrom(source)) {
            return 0;
        }

        int restDamage = damage;

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

        // Prevent Damage static abilities
        for (final Card ca : game.getCardsIn(ZoneType.Battlefield)) {
            final ArrayList<StaticAbility> staticAbilities = ca.getStaticAbilities();
            for (final StaticAbility stAb : staticAbilities) {
                restDamage = stAb.applyAbility("PreventDamage", source, this, restDamage, isCombat);
            }
        }

        // specific cards
        if (this.isCardInPlay("Spirit of Resistance")) {
            if ((this.getColoredCardsInPlay(Constant.Color.BLACK).size() > 0)
                    && (this.getColoredCardsInPlay(Constant.Color.BLUE).size() > 0)
                    && (this.getColoredCardsInPlay(Constant.Color.GREEN).size() > 0)
                    && (this.getColoredCardsInPlay(Constant.Color.RED).size() > 0)
                    && (this.getColoredCardsInPlay(Constant.Color.WHITE).size() > 0)) {
                return 0;
            }
        }
        if (restDamage > 0) {
            return restDamage;
        } else {
            return 0;
        }
    }

    // This is usable by the AI to forecast an effect (so it must
    // not change the game state)
    // 2012/01/02: No longer used in calculating the finalized damage, but
    // retained for damageprediction. -Hellfish
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

        for (Card c : game.getCardsIn(ZoneType.Battlefield)) {
            if (c.getName().equals("Sulfuric Vapors")) {
                if (source.isSpell() && source.isRed()) {
                    restDamage += 1;
                }
            } else if (c.getName().equals("Pyromancer's Swath")) {
                if (c.getController().equals(source.getController()) && (source.isInstant() || source.isSorcery())) {
                    restDamage += 2;
                }
            } else if (c.getName().equals("Furnace of Rath")) {
                restDamage += restDamage;
            } else if (c.getName().equals("Gratuitous Violence")) {
                if (c.getController().equals(source.getController()) && source.isCreature()) {
                    restDamage += restDamage;
                }
            } else if (c.getName().equals("Fire Servant")) {
                if (c.getController().equals(source.getController()) && source.isRed()
                        && (source.isInstant() || source.isSorcery())) {
                    restDamage *= 2;
                }
            } else if (c.getName().equals("Curse of Bloodletting")) {
                if (c.getEnchanting().equals(this)) {
                    restDamage *= 2;
                }
            } else if (c.getName().equals("Gisela, Blade of Goldnight")) {
                if (!c.getController().equals(this)) {
                    restDamage *= 2;
                }
            } else if (c.getName().equals("Inquisitor's Flail")) {
                if (isCombat && c.getEquippingCard() != null && c.getEquippingCard().equals(source)) {
                    restDamage *= 2;
                }
            } else if (c.getName().equals("Ghosts of the Innocent")) {
                restDamage = restDamage / 2;
            } else if (c.getName().equals("Benevolent Unicorn")) {
                if (source.isSpell()) {
                   restDamage -= 1;
                }
            } else if (c.getName().equals("Divine Presence")) {
                if (restDamage > 3) {
                    restDamage = 3;
                }
            } else if (c.getName().equals("Forethought Amulet")) {
                if (c.getController().equals(this) && (source.isInstant() || source.isSorcery())
                        && restDamage > 2) {
                    restDamage = 2;
                }
            } else if (c.getName().equals("Elderscale Wurm")) {
                if (c.getController().equals(this) && this.getLife() - restDamage < 7) {
                    restDamage = this.getLife() - 7;
                    if (restDamage < 0) {
                        restDamage = 0;
                    }
                }
            }
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

        // Replacement effects
        final HashMap<String, Object> repParams = new HashMap<String, Object>();
        repParams.put("Event", "DamageDone");
        repParams.put("Affected", this);
        repParams.put("DamageSource", source);
        repParams.put("DamageAmount", damage);
        repParams.put("IsCombat", isCombat);

        if (game.getReplacementHandler().run(repParams) != ReplacementResult.NotReplaced) {
            return 0;
        }

        if (game.isCardInPlay("Crumbling Sanctuary")) {
            for (int i = 0; i < damage; i++) {
                final List<Card> lib = this.getCardsIn(ZoneType.Library);
                if (lib.size() > 0) {
                    game.getAction().exile(lib.get(0));
                }
            }
            // return so things like Lifelink, etc do not trigger. This is a
            // replacement effect I think.
            return 0;
        }

        return damage;
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

        if (game.isCardInPlay("Leyline of Punishment")
                || source.hasKeyword("Damage that would be dealt by CARDNAME can't be prevented.")) {
            return damage;
        }

        int restDamage = damage;

        final HashMap<String, Object> repParams = new HashMap<String, Object>();
        repParams.put("Event", "DamageDone");
        repParams.put("Affected", this);
        repParams.put("DamageSource", source);
        repParams.put("DamageAmount", damage);
        repParams.put("IsCombat", isCombat);
        repParams.put("Prevention", true);

        if (game.getReplacementHandler().run(repParams) != ReplacementResult.NotReplaced) {
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
     */
    public final void clearAssignedDamage() {
        this.assignedDamage.clear();
    }

    /**
     * <p>
     * addAssignedDamage.
     * </p>
     * 
     * @param n
     *            a int.
     * @param source
     *            a {@link forge.Card} object.
     */
    public final void addAssignedDamage(final int n, final Card source) {
        this.assignedDamage.put(source, n);
    }

    /**
     * <p>
     * Getter for the field <code>assignedDamage</code>.
     * </p>
     * 
     * @return a int.
     */
    public final int getAssignedDamage() {
        int num = 0;
        for (final Integer value : this.assignedDamage.values()) {
            num += value;
        }
        return num;
    }

    /**
     * <p>
     * Getter for the field <code>assignedDamage</code>.
     * </p>
     * 
     * @param type
     *            a string.
     * 
     * @return a int.
     */
    public final int getAssignedDamage(final String type) {
        final Map<Card, Integer> valueMap = new HashMap<Card, Integer>();
        for (final Card c : this.assignedDamage.keySet()) {
            if (c.isType(type)) {
                valueMap.put(c, this.assignedDamage.get(c));
            }
        }
        int num = 0;
        for (final Integer value : valueMap.values()) {
            num += value;
        }
        return num;
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
     * @param source
     *            the source
     */
    public final void addPoisonCounters(final int num, final Card source) {
        if (!this.hasKeyword("You can't get poison counters")) {
            this.poisonCounters += num;
            game.getGameLog().add("Poison", this + " receives a poison counter from " + source, 3);
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
        if (this.hasKeyword("Shroud") || (!this.equals(sa.getActivatingPlayer()) && this.hasKeyword("Hexproof"))
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
    // / replaces Singletons.getModel().getGameAction().draw* methods
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
        if (this.hasKeyword("You can't draw cards.")) {
            return false;
        }
        return true;
    }

    /**
     * <p>
     * drawCard.
     * </p>
     * 
     * @return a List<Card> of cards actually drawn
     */
    public final List<Card> drawCard() {
        return this.drawCards(1);
    }

    /**
     * <p>
     * drawCards.
     * </p>
     * 
     * @return a List<Card> of cards actually drawn
     */
    public final List<Card> drawCards() {
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
     * @return a List<Card> of cards actually drawn
     */
    public final List<Card> drawCards(final int n) {
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
     * @return a List<Card> of cards actually drawn
     */
    public final List<Card> drawCards(final int n, final boolean firstFromDraw) {
        final List<Card> drawn = new ArrayList<Card>();

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

            if (!firstFromDraw && game.isCardInPlay("Chains of Mephistopheles")) {
                if (!this.getZone(ZoneType.Hand).isEmpty()) {
                    if (this.isHuman()) {
                        this.discardChainsOfMephistopheles();
                    } else { // Computer
                        this.discard(1, null);
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
     * 
     * TODO Write javadoc for this method.
     * 
     * @param player
     *            a Player object
     * @param playerRating
     *            a GamePlayerRating object
     * @return an int
     */
    public  int doMulligan() {
        final List<Card> hand = new ArrayList<Card>(getCardsIn(ZoneType.Hand));
        for (final Card c : hand) {
            game.getAction().moveToLibrary(c);
        }
        shuffle();
        final int newHand = hand.size() - 1;
        for (int i = 0; i < newHand; i++) {
            drawCard();
        }
        game.getGameLog().add("Mulligan", this + " has mulliganed down to " + newHand + " cards.", 0);
        stats.notifyHasMulliganed();
        stats.notifyOpeningHandSize(newHand);
        return newHand;
    }    

    /**
     * <p>
     * doDraw.
     * </p>
     * 
     * @return a List<Card> of cards actually drawn
     */
    private List<Card> doDraw() {
        final List<Card> drawn = new ArrayList<Card>();
        final PlayerZone library = this.getZone(ZoneType.Library);

        // Replacement effects
        final HashMap<String, Object> repRunParams = new HashMap<String, Object>();
        repRunParams.put("Event", "Draw");
        repRunParams.put("Affected", this);

        if (game.getReplacementHandler().run(repRunParams) != ReplacementResult.NotReplaced) {
            return drawn;
        }

        if (library.size() != 0) {

            Card c = library.get(0);
            c = game.getAction().moveToHand(c);
            drawn.add(c);

            if ((this.numDrawnThisTurn == 0) && this.isComputer()) {
                boolean reveal = false;
                final List<Card> cards = this.getCardsIn(ZoneType.Battlefield);
                for (final Card card : cards) {
                    if (card.hasKeyword("Reveal the first card you draw each turn")) {
                        reveal = true;
                        break;
                    }
                }
                if (reveal) {
                    GuiChoose.one("Revealing the first card drawn", drawn);
                }
            }

            this.setLastDrawnCard(c);
            c.setDrawnThisTurn(true);
            this.numDrawnThisTurn++;

            // Miracle draws
            if (this.numDrawnThisTurn == 1) {
                game.getAction().drawMiracle(c);
            }

            // Run triggers
            final HashMap<String, Object> runParams = new HashMap<String, Object>();
            runParams.put("Card", c);
            runParams.put("Number", this.numDrawnThisTurn);
            game.getTriggerHandler().runTrigger(TriggerType.Drawn, runParams);
        }
        // lose:
        else if (!Preferences.DEV_MODE || Singletons.getModel().getPreferences().getPrefBoolean(FPref.DEV_MILLING_LOSS)) {
            // if devMode is off, or canLoseByDecking is Enabled, run Lose condition
            if (!this.cantLose()) {
                this.loseConditionMet(GameLossReason.Milled, null);
                game.getAction().checkStateEffects();
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
    public final PlayerZone getZone(final ZoneType zone) {
        return this.zones.get(zone);
    }
    
    public final List<Card> getCardsIn(final ZoneType zoneType) {
        return getCardsIn(zoneType, true);
    }

    /**
     * gets a list of all cards in the requested zone. This function makes a
     * List<Card> from Card[].
     * 
     * @param zone
     *            the zone
     * @return a List<Card> with all the cards currently in requested zone
     */
    public final List<Card> getCardsIn(final ZoneType zoneType, boolean includePhasedOut) {
        List<Card> result;
        if (zoneType == ZoneType.Stack) {
            result = new ArrayList<Card>();
            for (Card c : game.getStackZone().getCards()) {
                if (c.getOwner().equals(this)) {
                    result.add(c);
                }
            }
        } else {
            PlayerZone zone = this.getZone(zoneType);
            result = zone == null ? null : zone.getCards(includePhasedOut);
        }
        return result;
    }

    /**
     * Gets the cards include phasing in.
     * 
     * @param zone
     *            the zone
     * @return the cards include phasing in
     */
    public final List<Card> getCardsIncludePhasingIn(final ZoneType zone) {
        return this.getCardsIn(zone, false);
    }

    /**
     * gets a list of first N cards in the requested zone. This function makes a
     * List<Card> from Card[].
     * 
     * @param zone
     *            the zone
     * @param n
     *            the n
     * @return a List<Card> with all the cards currently in requested zone
     */
    public final List<Card> getCardsIn(final ZoneType zone, final int n) {
        return Lists.newArrayList(Iterables.limit(this.getCardsIn(zone), n));
    }

    /**
     * gets a list of all cards in a given player's requested zones.
     * 
     * @param zones
     *            the zones
     * @return a List<Card> with all the cards currently in requested zones
     */
    public final List<Card> getCardsIn(final Iterable<ZoneType> zones) {
        final List<Card> result = new ArrayList<Card>();
        for (final ZoneType z : zones) {
            result.addAll(getCardsIn(z));
        }
        return result;
    }
    public final List<Card> getCardsIn(final ZoneType[] zones) {
        final List<Card> result = new ArrayList<Card>();
        for (final ZoneType z : zones) {
            result.addAll(getCardsIn(z));
        }
        return result;
    }
    
    /**
     * gets a list of all cards with requested cardName in a given player's
     * requested zone. This function makes a List<Card> from Card[].
     * 
     * @param zone
     *            the zone
     * @param cardName
     *            the card name
     * @return a List<Card> with all the cards currently in that player's library
     */
    public final List<Card> getCardsIn(final ZoneType zone, final String cardName) {
        return CardLists.filter(this.getCardsIn(zone), CardPredicates.nameEquals(cardName));
    }

    /**
     * Gets the all cards.
     * 
     * @return the all cards
     */
    public final List<Card> getAllCards() {
        List<Card> allExcStack = this.getCardsIn(Player.ALL_ZONES);
        allExcStack.addAll(getCardsIn(ZoneType.Stack));
        return allExcStack;
    }

    /**
     * <p>
     * getDredge.
     * </p>
     * 
     * @return a {@link forge.CardList} object.
     */
    protected final List<Card> getDredge() {
        final List<Card> dredge = new ArrayList<Card>();
        int cntLibrary = this.getCardsIn(ZoneType.Library).size();
        for (final Card c : this.getCardsIn(ZoneType.Graveyard)) {
            int nDr = getDredgeNumber(c);
            if ( nDr > 0 && cntLibrary >= nDr) {
                dredge.add(c);
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
        for (String s : c.getKeyword()) {
            if (s.startsWith("Dredge")) {
                return Integer.parseInt("" + s.charAt(s.length() - 1));
            }
        }
        return 0;
//        throw new RuntimeException("Input_Draw : getDredgeNumber() card doesn't have dredge - " + c.getName());
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
    // / replaces Singletons.getModel().getGameAction().discard* methods
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
    public abstract void discard(final int num, final SpellAbility sa);

    /**
     * <p>
     * discard.
     * </p>
     * 
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @return a {@link forge.CardList} object.
     */
    public final void discard(final SpellAbility sa) {
        this.discard(1, sa);
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
    public final List<Card> discard(final Card c, final SpellAbility sa) {
        this.doDiscard(c, sa);
        return CardLists.createCardList(c);
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
        /*if (sa != null) {
            sa.addCostToHashList(c, "Discarded");
        }*/

        game.getAction().discardMadness(c);

        if ((c.hasKeyword("If a spell or ability an opponent controls causes "
                + "you to discard CARDNAME, put it onto the battlefield instead of putting it into your graveyard.") || c
                .hasKeyword("If a spell or ability an opponent controls causes "
                        + "you to discard CARDNAME, put it onto the battlefield with two +1/+1 "
                        + "counters on it instead of putting it into your graveyard."))
                && (null != sa) && !c.getController().equals(sa.getSourceCard().getController())) {
            game.getAction().discardPutIntoPlayInstead(c);
        } else {
            game.getAction().moveToGraveyard(c);
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
        game.getTriggerHandler().runTrigger(TriggerType.Discarded, runParams);

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
    public final List<Card> discardHand(final SpellAbility sa) {
        final List<Card> list = new ArrayList<Card>(this.getCardsIn(ZoneType.Hand));
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
     * @return a List<Card> of cards discarded
     */
    public final List<Card> discardRandom(final SpellAbility sa) {
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
     * @return a List<Card> of cards discarded
     */
    public final List<Card> discardRandom(final int num, final SpellAbility sa) {
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
     * @return a List<Card> of cards discarded
     */
    public final List<Card> discardRandom(final int num, final SpellAbility sa, final String valid) {
        final List<Card> discarded = new ArrayList<Card>();
        for (int i = 0; i < num; i++) {
            final List<Card> list = 
                    CardLists.getValidCards(this.getCardsIn(ZoneType.Hand), valid, sa.getSourceCard().getController(), sa.getSourceCard());
            if (list.size() != 0) {
                final Card disc = CardUtil.getRandom(list);
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
    public final List<Card> mill(final int n) {
        return this.mill(n, ZoneType.Graveyard, false);
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
    public final List<Card> mill(final int n, final ZoneType zone, final boolean bottom) {
        final List<Card> lib = this.getCardsIn(ZoneType.Library);
        final List<Card> milled = new ArrayList<Card>();

        final int max = Math.min(n, lib.size());

        final ZoneType destination = this.getZone(zone).getZoneType();

        for (int i = 0; i < max; i++) {
            if (bottom) {
                milled.add(game.getAction().moveTo(destination, lib.get(lib.size() - 1)));
            } else {
                milled.add(game.getAction().moveTo(destination, lib.get(i)));
            }
        }

        return milled;
    }

    // //////////////////////////////
    /**
     * <p>
     * shuffle.
     * </p>
     */
    public final void shuffle() {
        final PlayerZone library = this.getZone(ZoneType.Library);
        final List<Card> list = new ArrayList<Card>(this.getCardsIn(ZoneType.Library));

        if (list.size() <= 1) {
            return;
        }

        // overdone but wanted to make sure it was really random
        final Random random = MyRandom.getRandom();
        Collections.shuffle(list, random);
        Collections.shuffle(list, random);
        Collections.shuffle(list, random);
        Collections.shuffle(list, random);
        Collections.shuffle(list, random);
        Collections.shuffle(list, random);

        Card o;
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

        library.setCards(list);

        // Run triggers
        final HashMap<String, Object> runParams = new HashMap<String, Object>();
        runParams.put("Player", this);
        game.getTriggerHandler().runTrigger(TriggerType.Shuffled, runParams);

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
    protected abstract void doScry(List<Card> topN, int n);

    /**
     * <p>
     * scry.
     * </p>
     * 
     * @param numScry
     *            a int.
     */
    public final void scry(int numScry) {
        final List<Card> topN = new ArrayList<Card>();
        final PlayerZone library = this.getZone(ZoneType.Library);
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
            land.addController(this);
            game.getAction().moveTo(this.getZone(ZoneType.Battlefield), land);
            CardFactoryUtil.playLandEffects(land);
            this.numLandsPlayed++;

            // check state effects for static animate (Living Lands, Conversion,
            // etc...)
            game.getAction().checkStateEffects();

            // add to log
            game.getGameLog().add("Land", this + " played " + land, 2);

            // Run triggers
            final HashMap<String, Object> runParams = new HashMap<String, Object>();
            runParams.put("Card", land);
            game.getTriggerHandler().runTrigger(TriggerType.LandPlayed, runParams);
        }

        game.getStack().unfreezeStack();
    }

    /**
     * <p>
     * canPlayLand.
     * </p>
     * 
     * @return a boolean.
     */
    public final boolean canPlayLand() {
        if (Singletons.getModel().getPreferences().getPrefBoolean(FPref.DEV_UNLIMITED_LAND)
                && this.isHuman()
                && Preferences.DEV_MODE) {
            return this.canCastSorcery();
        }

        // CantBeCast static abilities
        for (final Card ca : game.getCardsIn(ZoneType.Battlefield)) {
            final ArrayList<StaticAbility> staticAbilities = ca.getStaticAbilities();
            for (final StaticAbility stAb : staticAbilities) {
                if (stAb.applyAbility("CantPlayLand", null, this)) {
                    return false;
                }
            }
        }

        return this.canCastSorcery()
                && ((this.numLandsPlayed < this.maxLandsToPlay) || (this.getCardsIn(ZoneType.Battlefield, "Fastbond")
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
        final List<Card> c = CardLists.filter(this.getCardsIn(ZoneType.Battlefield), CardPredicates.Presets.PLANEWALKERS);
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
     * Getter for the field <code>slowtripList</code>.
     * </p>
     * 
     * @return a {@link forge.CardList} object.
     */
    public final List<Card> getSlowtripList() {
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
        return this.stats.getTurnsPlayed();
    }

    /**
     * <p>
     * incrementTurn.
     * </p>
     */
    public final void incrementTurn() {
        this.stats.nextTurn();
    }

    /**
     * <p>
     * getAttackedWithCreatureThisTurn.
     * </p>
     * 
     * @return a boolean.
     */
    public final boolean getAttackedWithCreatureThisTurn() {
        return this.attackedWithCreatureThisTurn;
    }

    /**
     * <p>
     * Setter for the field <code>attackedWithCreatureThisTurn</code>.
     * </p>
     * 
     * @param b
     *            a boolean.
     */
    public final void setAttackedWithCreatureThisTurn(final boolean b) {
        this.attackedWithCreatureThisTurn = b;
    }

    /**
     * <p>
     * Gets the number of attackers declared by Player this turn.
     * </p>
     *
     * @return a boolean.
     */
    public final int getAttackersDeclaredThisTurn() {
        return this.attackersDeclaredThisTurn;
    }

    /**
     * <p>
     * Increase number of attackers declared by Player this turn.
     * </p>
     *
     */
    public final void incrementAttackersDeclaredThisTurn() {
        this.attackersDeclaredThisTurn++;
    }

    /**
     * <p>
     * Resets number of attackers declared by Player this turn.
     * </p>
     *
     */
    public final void resetAttackersDeclaredThisTurn() {
        this.attackersDeclaredThisTurn = 0;
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
    public abstract void sacrificePermanent(String prompt, List<Card> choices);

    // Game win/loss

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
        this.setOutcome(PlayerOutcome.altWin(sourceName));

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
        if ( state != GameLossReason.OpponentWon ) {
            if (this.cantLose()) {
                System.out.println("Tried to lose, but currently can't.");
                return false;
            }
    
            // Replacement effects
            final HashMap<String, Object> runParams = new HashMap<String, Object>();
            runParams.put("Affected", this);
            runParams.put("Event", "GameLoss");
    
            if (game.getReplacementHandler().run(runParams) != ReplacementResult.NotReplaced) {
                return false;
            }
        }

        setOutcome(PlayerOutcome.loss(state, spellName));
        return true;
    }

    /**
     * Concede.
     */
    public final void concede() { // No cantLose checks - just lose
        setOutcome(PlayerOutcome.concede());
    }

    /**
     * <p>
     * cantLose.
     * </p>
     * 
     * @return a boolean.
     */
    public final boolean cantLose() {
        if (this.getOutcome() != null && this.getOutcome().lossState == GameLossReason.Conceded) {
            return false;
        }

        return (this.hasKeyword("You can't lose the game.") || this.getOpponent().hasKeyword("You can't win the game."));
    }

    /**
     * <p>
     * cantLoseForZeroOrLessLife.
     * </p>
     * 
     * @return a boolean.
     */
    public final boolean cantLoseForZeroOrLessLife() {
        return (this.hasKeyword("You don't lose the game for having 0 or less life."));
    }

    /**
     * <p>
     * cantWin.
     * </p>
     * 
     * @return a boolean.
     */
    public final boolean cantWin() {
        boolean isAnyOppLoseProof = false;
        for( Player p : game.getPlayers() ) {
            if ( p == this || p.getOutcome() != null ) continue; // except self and already dead
            isAnyOppLoseProof |= p.hasKeyword("You can't lose the game.");
        }
        return this.hasKeyword("You can't win the game.") || isAnyOppLoseProof;
    }

    /**
     * <p>
     * hasLost.
     * </p>
     * 
     * @return a boolean.
     */
    public final boolean checkLoseCondition() {
        
        if ( this.getOutcome() != null )
            return this.getOutcome().lossState != null;

        if (this.poisonCounters >= 10) {
            return this.loseConditionMet(GameLossReason.Poisoned, null);
        }

        final boolean hasNoLife = this.getLife() <= 0;
        if (hasNoLife && !this.cantLoseForZeroOrLessLife()) {
            return this.loseConditionMet(GameLossReason.LifeReachedZero, null);
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
        // in multiplayer game one player's win is replaced by all other's lose (rule 103.4h)
        // so if someone cannot lose, the game appears to continue

        return this.getOutcome() != null && this.getOutcome().lossState == null;
    }

    /**
     * <p>
     * hasMetalcraft.
     * </p>
     * 
     * @return a boolean.
     */
    public final boolean hasMetalcraft() {
        final List<Card> list = CardLists.filter(this.getCardsIn(ZoneType.Battlefield), CardPredicates.Presets.ARTIFACTS);
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
        return this.getZone(ZoneType.Graveyard).size() >= 7;
    }

    /**
     * <p>
     * hasHellbent.
     * </p>
     * 
     * @return a boolean.
     */
    public final boolean hasHellbent() {
        return this.getZone(ZoneType.Hand).isEmpty();
    }

    /**
     * <p>
     * hasLandfall.
     * </p>
     * 
     * @return a boolean.
     */
    public final boolean hasLandfall() {
        final List<Card> list = this.getZone(ZoneType.Battlefield).getCardsAddedThisTurn(null);
        return Iterables.any(list, CardPredicates.Presets.LANDS);
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
        if (this.prowl.contains("AllCreatureTypes")) {
            return true;
        }
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

        if (incR[0].equals("Opponent")) {
            if (this.equals(sourceController)) {
                return false;
            }
        } else if (incR[0].equals("You")) {
            if (!this.equals(sourceController)) {
                return false;
            }
        } else if (incR[0].equals("EnchantedController")) {
            final GameEntity enchanted = source.getEnchanting();
            if ((enchanted == null) || !(enchanted instanceof Card)) {
                return false;
            }
            final Card enchantedCard = (Card) enchanted;
            if (!this.equals(enchantedCard.getController())) {
                return false;
            }
        } else {
            if (!incR[0].equals("Player")) {
                return false;
            }
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
            if (!source.getDamageHistory().getThisGameDamaged().contains(this)) {
                return false;
            }
        } else if (property.equals("wasDealtDamageBySourceThisTurn")) {
            if (!source.getDamageHistory().getThisTurnDamaged().contains(this)) {
                return false;
            }
        } else if (property.startsWith("wasDealtDamageThisTurn")) {
            if (this.assignedDamage.isEmpty()) {
                return false;
            }
        } else if (property.startsWith("LostLifeThisTurn")) {
            if (this.lifeLostThisTurn <= 0) {
                return false;
            }
        } else if (property.equals("IsRemembered")) {
            if (!source.getRemembered().contains(this)) {
                return false;
            }
        } else if (property.startsWith("EnchantedBy")) {
            if (!this.getEnchantedBy().contains(source)) {
                return false;
            }
        } else if (property.startsWith("Chosen")) {
            if (source.getChosenPlayer() == null || !source.getChosenPlayer().equals(this)) {
                return false;
            }
        }

        return true;
    }

    /**
     * <p>
     * getMaxHandSize.
     * </p>
     * 
     * @return a int.
     */
    public final int getMaxHandSize() {
        return maxHandSize;
    }

    /**
     * <p>
     * setMaxHandSize.
     * </p>
     * 
     * @param size
     *            a int.
     */
    public final void setMaxHandSize(int size) {
        maxHandSize = size;
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

    /**
     * <p>
     * Getter for the field <code>lifeLostThisTurn</code>.
     * </p>
     * 
     * @return a int.
     */
    public final int getLifeLostThisTurn() {
        return this.lifeLostThisTurn;
    }

    /**
     * <p>
     * Setter for the field <code>lifeLostThisTurn</code>.
     * </p>
     * 
     * @param n
     *            a int.
     */
    public final void setLifeLostThisTurn(final int n) {
        this.lifeLostThisTurn = n;
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
        final ZoneType lib = ZoneType.Library;

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
    public final void setMustAttackEntity(final GameEntity o) {
        this.mustAttackEntity = o;
    }

    /**
     * get the Player object or Card (Planeswalker) object that this Player must
     * attack this combat.
     * 
     * @return the Player or Card (Planeswalker)
     * @since 1.1.01
     */
    public final GameEntity getMustAttackEntity() {
        return this.mustAttackEntity;
    }

    /**
     * Update label observers.
     */
    public final void updateLabelObservers() {
        this.getZone(ZoneType.Hand).updateObservers();
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

    @Override
    public int compareTo(Player o) {
        if (o == null) {
            return +1;
        }
        int subtractedHash = o.hashCode() - this.hashCode();
        if (subtractedHash == 0) {
            return 0;
        }
        
        return Math.abs(subtractedHash)/subtractedHash;
    }
    
    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        return (41 * (41 + this.getName().hashCode()));
    }

    public static class Predicates { 

        public static final Predicate<Player> NOT_LOST = new Predicate<Player>() {
            @Override
            public boolean apply(Player p) {
                return p.getOutcome() == null || p.getOutcome().hasWon();
            }
        };

        public static Predicate<Player> isType(final PlayerType type) {
            return new Predicate<Player>() {
                @Override
                public boolean apply(Player input){
                    return input.getType() == type;
                }
            };
        }
    }
    
    public static class Accessors {
        public static Function<Player, LobbyPlayer> FN_GET_LOBBY_PLAYER = new Function<Player, LobbyPlayer>(){
            @Override
            public LobbyPlayer apply(Player input) {
                return input.getLobbyPlayer();
            }
        };
        
        public static Function<Player, Integer> FN_GET_LIFE = new Function<Player, Integer>(){
            @Override
            public Integer apply(Player input) {
                return input.getLife();
            }
        };

        public static Function<Player, PlayerType> FN_GET_TYPE = new Function<Player, PlayerType>(){
            @Override
            public PlayerType apply(Player input) {
                return input.getType();
            }
        };

        public static Function<Player, String> FN_GET_NAME = new Function<Player, String>(){
            @Override
            public String apply(Player input) {
                return input.getName();
            }
        };
        
        public static Function<Player, Integer> countCardsInZone(final ZoneType zone){
            return new Function<Player, Integer>(){
                @Override
                public Integer apply(Player input) {
                    return input.getZone(zone).size();
                }
            };
        }
    }

    /**
     * TODO: Write javadoc for this method.
     * @return
     */
    public LobbyPlayer getLobbyPlayer() {
        return lobbyPlayer;
    }

    private void setOutcome(PlayerOutcome outcome) {
        stats.setOutcome(outcome); 
    }

    /**
     * TODO: Write javadoc for this method.
     */
    public void onGameOver() {
        if ( null == stats.getOutcome() ) // not lost?  
            setOutcome(PlayerOutcome.win()); // then won!
    }

    /**
     * use to get a list of creatures in play for a given player.
     * 
     * @param player
     *            the player to get creatures for
     * @return a List<Card> containing all creatures a given player has in play
     */
    public List<Card> getCreaturesInPlay() {
        return CardLists.filter(getCardsIn(ZoneType.Battlefield), Presets.CREATURES);
    }

    /**
     * use to get a list of all lands a given player has on the battlefield.
     * 
     * @param player
     *            the player whose lands we want to get
     * @return a List<Card> containing all lands the given player has in play
     */
    public List<Card> getLandsInPlay() {
        return CardLists.filter(getCardsIn(ZoneType.Battlefield), Presets.LANDS);
    }
    public boolean isCardInPlay(final String cardName) {
        return Iterables.any(getZone(ZoneType.Battlefield), CardPredicates.nameEquals(cardName));
    }

    
    public List<Card> getColoredCardsInPlay(final String color) {
        return CardLists.filter(getCardsIn(ZoneType.Battlefield), new Predicate<Card>() {
            @Override
            public boolean apply(final Card c) {
                return CardUtil.getColors(c).contains(color);
            }
        });
    }

    public int getCounterDoublersMagnitude(final Counters type) {
        int counterDoublers = getCardsIn(ZoneType.Battlefield, "Doubling Season").size();
        if(type == Counters.P1P1) {
            counterDoublers += getCardsIn(ZoneType.Battlefield, "Corpsejack Menace").size();
        }
        return (int) Math.pow(2, counterDoublers); // pow(a,0) = 1; pow(a,1) = a
                                                   // ... no worries about size
                                                   // = 0
    }
    
    public int getTokenDoublersMagnitude() {
        final int tokenDoublers = getCardsIn(ZoneType.Battlefield, "Parallel Lives").size()
                + getCardsIn(ZoneType.Battlefield, "Doubling Season").size();
        return (int) Math.pow(2, tokenDoublers); // pow(a,0) = 1; pow(a,1) = a
                                                 // ... no worries about size =
                                                 // 0
    }

    public void onCleanupPhase() {
        for (Card c : getCardsIn(ZoneType.Hand))
            c.setDrawnThisTurn(false);
        
        resetPreventNextDamage();
        resetNumDrawnThisTurn();
        setAttackedWithCreatureThisTurn(false);
        setNumLandsPlayed(0);
        clearAssignedDamage();
        resetAttackersDeclaredThisTurn();
    }

    public boolean canCastSorcery() {
        PhaseHandler now = game.getPhaseHandler();
        return now.isPlayerTurn(this) && now.getPhase().isMain() && game.getStack().size() == 0;
    }
    
    
    /**
     * <p>
     * couldCastSorcery.
     * for conditions the stack must only have the sa being checked
     * </p>
     * 
     * @param player
     *            a {@link forge.game.player.Player} object.
     * @param sa
     *            a {@link forge.game.player.SpellAbility} object.
     * @return a boolean .
     */
    public boolean couldCastSorcery(final SpellAbility sa) {

        final Card source = sa.getRootSpellAbility().getSourceCard();
        boolean onlyThis = true;

        for (final Card card : game.getCardsIn(ZoneType.Stack)) {
            if (card != source) {
                onlyThis = false;
                //System.out.println("StackCard: " + card + " vs SourceCard: " + source);
            }
        }
        
        PhaseHandler now = game.getPhaseHandler();
        //System.out.println("now.isPlayerTurn(player) - " + now.isPlayerTurn(player));
        //System.out.println("now.getPhase().isMain() - " + now.getPhase().isMain());
        //System.out.println("onlyThis - " + onlyThis);
        return onlyThis && now.isPlayerTurn(this) && now.getPhase().isMain();
    }

    /**
     * TODO: Write javadoc for this method.
     * @return
     */
    public PlayerController getController() {
        return controller;
    }

    /**
     * <p>
     * skipCombat.
     * </p>
     * 
     * @return a boolean.
     */
    public boolean isSkippingCombat() {
    
        if (hasKeyword("Skip your next combat phase.")) {
            return true;
        }
        if (hasKeyword("Skip your combat phase.")) {
            return true;
        }
        if (hasKeyword("Skip all combat phases of your next turn.")) {
            removeKeyword("Skip all combat phases of your next turn.");
            addKeyword("Skip all combat phases of this turn.");
            return true;
        }
        if (hasKeyword("Skip all combat phases of this turn.")) {
            return true;
        }
    
        return false;
    }

    /**
     * <p>
     * skipTurnTimeVault.
     * </p>
     * 
     * @return a {@link forge.game.player.Player} object.
     */
    public boolean skipTurnTimeVault() {
        // time vault:
        List<Card> vaults = getCardsIn(ZoneType.Battlefield, "Time Vault");
        vaults = CardLists.filter(vaults, Presets.TAPPED);
    
        if (vaults.size() > 0) {
            final Card crd = vaults.get(0);
    
            if (isHuman()) {
                if (GameActionUtil.showYesNoDialog(crd, "Untap " + crd + "?")) {
                    crd.untap();
                    return true;
                }
            } else {
                // TODO Should AI skip his turn for time vault?
            }
        }
        return false;
    }
    
}
