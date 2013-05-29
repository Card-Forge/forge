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

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import forge.Card;
import forge.CardLists;
import forge.CardPredicates;
import forge.CardPredicates.Presets;
import forge.Constant.Preferences;
import forge.CounterType;
import forge.FThreads;
import forge.GameEntity;
import forge.GameEventType;
import forge.Singletons;
import forge.card.MagicColor;
import forge.card.ability.AbilityFactory;
import forge.card.ability.AbilityUtils;
import forge.card.cardfactory.CardFactoryUtil;
import forge.card.cost.Cost;
import forge.card.mana.ManaCost;
import forge.card.mana.ManaPool;
import forge.card.replacement.ReplacementResult;
import forge.card.spellability.Ability;
import forge.card.spellability.SpellAbility;
import forge.card.spellability.Target;
import forge.card.staticability.StaticAbility;
import forge.card.trigger.TriggerType;
import forge.game.GameActionUtil;
import forge.game.GameState;
import forge.game.GlobalRuleChange;
import forge.game.event.GameEventCardDiscarded;
import forge.game.event.GameEventDrawCard;
import forge.game.event.GameEventLandPlayed;
import forge.game.event.GameEventLifeLoss;
import forge.game.event.GameEventMulligan;
import forge.game.event.GameEventPlayerControl;
import forge.game.event.GameEventPoisonCounter;
import forge.game.event.GameEventShuffle;
import forge.game.phase.PhaseHandler;
import forge.game.phase.PhaseType;
import forge.game.zone.PlayerZone;
import forge.game.zone.PlayerZoneBattlefield;
import forge.game.zone.ZoneType;
import forge.gui.GuiChoose;
import forge.gui.GuiDialog;
import forge.properties.ForgePreferences.FPref;
import forge.util.Lang;
import forge.util.MyRandom;

/**
 * <p>
 * Abstract Player class.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 */
public class Player extends GameEntity implements Comparable<Player> {
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

    /** The life Gained this turn. */
    private int lifeGainedThisTurn = 0;

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

    /** Starting hand size. */
    private int startingHandSize = 7;

    /** The unlimited hand size. */
    private boolean unlimitedHandSize = false;

    /** The last drawn card. */
    private Card lastDrawnCard = null;

    /** The num drawn this turn. */
    private int numDrawnThisTurn = 0;
    private int numDrawnThisDrawStep = 0;
    
    /** The num discarded this turn. */
    private int numDiscardedThisTurn = 0;
    
    /** A list of tokens not in play, but on their way.
     * This list is kept in order to not break ETB-replacement
     * on tokens. */
    private List<Card> inboundTokens = new ArrayList<Card>();

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

    private List<Card> currentPlanes = new ArrayList<Card>();

    private PlayerStatistics stats = new PlayerStatistics();
    protected PlayerController controller;
    protected PlayerController controllerCreator = null;

    private int teamNumber = -1;
    
    private Card activeScheme = null;

    /** The Constant ALL_ZONES. */
    public static final List<ZoneType> ALL_ZONES = Collections.unmodifiableList(Arrays.asList(ZoneType.Battlefield,
            ZoneType.Library, ZoneType.Graveyard, ZoneType.Hand, ZoneType.Exile, ZoneType.Command, ZoneType.Ante,
            ZoneType.Sideboard, ZoneType.PlanarDeck,ZoneType.SchemeDeck));

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
    public Player(String name, GameState game0) {
        game = game0;
        for (final ZoneType z : Player.ALL_ZONES) {
            final PlayerZone toPut = z == ZoneType.Battlefield
                    ? new PlayerZoneBattlefield(z, this)
                    : new PlayerZone(z, this);
            this.zones.put(z, toPut);
        }
        this.setName(chooseName(name));
    }

    private String chooseName(String originalName) {
        String nameCandidate = originalName;
        for( int i = 2; i <= 8; i++) { // several tries, not matter how many
            boolean haveDuplicates = false;
            for( Player p : game.getPlayers()) {
                if( p.getName().equals(nameCandidate)) {
                    haveDuplicates = true;
                    break;
                }
            }
            if(!haveDuplicates)
                return nameCandidate;
            nameCandidate = Lang.getOrdinal(i) + " " + originalName;
        }
        return nameCandidate;
    }

    @Override
    public GameState getGame() { // I'll probably regret about this  
        return game;
    }
    
    public final PlayerStatistics getStats() {
        return stats;
    }
    
    public final void setTeam(int iTeam) {
        teamNumber = iTeam;
    }
    public final int getTeam() {
        return teamNumber;
    }

    @Deprecated
    public boolean isHuman() { return getLobbyPlayer().getType() == PlayerType.HUMAN; }
    @Deprecated
    public boolean isComputer() { return getLobbyPlayer().getType() == PlayerType.COMPUTER; }

    public boolean isArchenemy() {

        //Only the archenemy has schemes.
        return getZone(ZoneType.SchemeDeck).size() > 0;
    }

    public void setSchemeInMotion() {
        for (final Player p : game.getPlayers()) {
            if (p.hasKeyword("Schemes can't be set in motion this turn.")) {
                return;
            }
        }

        // Replacement effects
        final HashMap<String, Object> repRunParams = new HashMap<String, Object>();
        repRunParams.put("Event", "SetInMotion");
        repRunParams.put("Affected", this);

        if (game.getReplacementHandler().run(repRunParams) != ReplacementResult.NotReplaced) {
            return;
        }

        game.getTriggerHandler().suppressMode(TriggerType.ChangesZone);
        activeScheme = getZone(ZoneType.SchemeDeck).get(0);
        // gameAction moveTo ? 
        getZone(ZoneType.SchemeDeck).remove(activeScheme);
        this.getZone(ZoneType.Command).add(activeScheme);
        game.getTriggerHandler().clearSuppression(TriggerType.ChangesZone);

        // Run triggers
        final HashMap<String, Object> runParams = new HashMap<String, Object>();
        runParams.put("Scheme", activeScheme);
        game.getTriggerHandler().runTrigger(TriggerType.SetInMotion, runParams, false);
    }

    /**
     * <p>
     * getOpponent. Used by current-generation AI.
     * </p>
     * 
     * @return a {@link forge.game.player.Player} object.
     */
    public final Player getOpponent() {
        for (Player p : game.getPlayers()) {
            if (p.isOpponentOf(this))
                return p;
        }
        throw new IllegalStateException("No opponents left ingame for " + this);
    }

    /**
     * 
     * returns all opponents.
     * Should keep player relations somewhere in the match structure
     * @return
     */
    public final List<Player> getOpponents() {
        List<Player> result = new ArrayList<Player>();
        for (Player p : game.getPlayers()) {
            if (p.isOpponentOf(this))
                result.add(p);
        }
        return result;
    }

    /**
     * returns allied players.
     * Should keep player relations somewhere in the match structure
     * @return
     */
    public final List<Player> getAllies() {
        List<Player> result = new ArrayList<Player>();
        for (Player p : game.getPlayers()) {
            if (!p.isOpponentOf(this))
                result.add(p);
        }
        return result;
    }

    /**
     * returns all other players.
     * Should keep player relations somewhere in the match structure
     * @return
     */
    public final List<Player> getAllOtherPlayers() {
        List<Player> result = new ArrayList<Player>(game.getPlayers());
        result.remove(this);
        return result;
    }

    /**
     * returns the weakest opponent (based on life totals).
     * Should keep player relations somewhere in the match structure
     * @return
     */
    public final Player getWeakestOpponent() {
        List<Player> opponnets = this.getOpponents();
        Player weakest = opponnets.get(0);
        for (int i = 1; i < opponnets.size(); i++) {
            if (weakest.getLife() > opponnets.get(i).getLife()) {
                weakest = opponnets.get(i);
            }
        }
        return weakest;
    }

    public boolean isOpponentOf(Player other) {
        return other != this && ( other.teamNumber < 0 || other.teamNumber != this.teamNumber );
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
            change = (this.loseLife(this.life - newLife) > 0);
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
        repParams.put("Source", source);
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
            this.lifeGainedThisTurn += lifeGain;

            // Run triggers
            final HashMap<String, Object> runParams = new HashMap<String, Object>();
            runParams.put("Player", this);
            runParams.put("LifeAmount", lifeGain);
            game.getTriggerHandler().runTrigger(TriggerType.LifeGained, runParams, false);
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
     * @return an int.
     */
    public final int loseLife(final int toLose) {
        int lifeLost = 0;
        if (!this.canLoseLife()) {
            return 0;
        }
        if (toLose > 0) {
            this.subtractLife(toLose);
            lifeLost = toLose;
            game.getEvents().post(new GameEventLifeLoss());
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
        game.getTriggerHandler().runTrigger(TriggerType.LifeLost, runParams, false);

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
            return (this.loseLife(lifePayment) > 0);
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

        if (damageToDo <= 0) {
            return false;
        }
        String additionalLog = "";
        source.addDealtDamageToPlayerThisTurn(this.getName(), damageToDo);

        boolean infect = source.hasKeyword("Infect")
                || this.hasKeyword("All damage is dealt to you as though its source had infect.");

        if (infect) {
            this.addPoisonCounters(damageToDo, source);
            additionalLog = "(as Poison Counters)";
        } else {
            // Worship does not reduce the damage dealt but changes the effect
            // of the damage
            if (this.hasKeyword("Damage that would reduce your life total to less than 1 reduces it to 1 instead.")
                    && this.life <= damageToDo) {
                this.loseLife(Math.min(damageToDo, this.life - 1));
                additionalLog = "(would reduce life total to less than 1, reduced to 1 instead.)";
            } else {
                // rule 118.2. Damage dealt to a player normally causes that
                // player to lose that much life.
                this.loseLife(damageToDo);
            }
        }

        this.assignedDamage.put(source, damageToDo);
        if (source.hasKeyword("Lifelink")) {
            source.getController().gainLife(damageToDo, source);
        }
        source.getDamageHistory().registerDamage(this);
        this.getGame().getEvents().post(new GameEventLifeLoss());

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
        game.getTriggerHandler().runTrigger(TriggerType.DamageDone, runParams, false);

        game.getGameLog().add(GameEventType.DAMAGE, String.format("Dealing %d damage to %s. %s", damageToDo, this.getName(), additionalLog));

        return true;
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
    public final int staticDamagePrevention(final int damage, final Card source, final boolean isCombat, final boolean isTest) {

        if (game.getStaticEffects().getGlobalRuleChange(GlobalRuleChange.noPrevention)) {
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
        for (final Card ca : game.getCardsIn(ZoneType.listValueOf("Battlefield,Command"))) {
            final ArrayList<StaticAbility> staticAbilities = ca.getStaticAbilities();
            for (final StaticAbility stAb : staticAbilities) {
                restDamage = stAb.applyAbility("PreventDamage", source, this, restDamage, isCombat, isTest);
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

        if (this.hasKeyword("Damage that would reduce your life total to less than 1 reduces it to 1 instead.")) {
            restDamage = Math.min(restDamage, this.life - 1);
        }

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

        if (game.getStaticEffects().getGlobalRuleChange(GlobalRuleChange.noPrevention)
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

        restDamage = this.staticDamagePrevention(restDamage, source, isCombat, false);

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
    
    public final Iterable<Card> getAssignedDamageSources() { 
        return assignedDamage.keySet();
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
    public final boolean addCombatDamage(final int damage, final Card source) {

        int damageToDo = damage;

        damageToDo = this.replaceDamage(damageToDo, source, true);
        damageToDo = this.preventDamage(damageToDo, source, true);

        this.addDamageAfterPrevention(damageToDo, source, true); // damage
                                                                 // prevention
        // is already
        // checked

        if (damageToDo > 0) {
            GameActionUtil.executeCombatDamageToPlayerEffects(this, source, damageToDo);
            return true;
        }
        return false;
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

            game.getEvents().post(new GameEventPoisonCounter(this, source, num));
            game.getGameLog().add(GameEventType.DAMAGE_POISON, this + " receives a poison counter from " + source);

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

    
    public boolean canMulligan() { 
        return !getZone(ZoneType.Hand).isEmpty();
    }

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
        final List<Card> drawn = new ArrayList<Card>();
    
        if (!this.canDraw()) {
            return drawn;
        }
    
        for (int i = 0; i < n; i++) {
    
            // TODO: multiple replacements need to be selected by the controller
            List<Card> dredgers = this.getDredge();
            if (!dredgers.isEmpty()) {
                Card toDredge = getController().chooseCardToDredge(dredgers);
                int dredgeNumber = toDredge == null ? Integer.MAX_VALUE : getDredgeNumber(toDredge);
                if ( dredgeNumber <= getZone(ZoneType.Library).size()) {
                    game.getAction().moveToHand(toDredge);

                    for (int iD = 0; iD < dredgeNumber; iD++) {
                        final Card c2 = getZone(ZoneType.Library).get(0);
                        game.getAction().moveToGraveyard(c2);
                    }
                    continue;
                }
            }
    
            drawn.addAll(this.doDraw());
        }
    
        // Play the Draw sound
        game.getEvents().post(new GameEventDrawCard());
    
        return drawn;
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

        // ======== Chains of Mephistopheles hardcode. ========= 
        // This card requires player to either discard a card, and then he may proceed drawing, or mill 1 - then no draw will happen
        // It's oracle-worded as a replacement effect ("If a player would draw a card ... discards a card instead") (rule 419.1a)
        // Yet, gatherer's rulings read: The effect is cumulative. If there are two of these on the battlefield, each of them will modify each draw
        // That means player isn't supposed to choose one replacement effect out of two (generated by Chains Of Mephistopheles), but both happen.
        // So it's not a common replacement effect and has to handled by special code.

        // This is why the code is placed after any other replacement effects could affect the draw event.  
        List<Card> chainsList = null;
        for(Card c : game.getCardsIn(ZoneType.Battlefield)) {
            if ( c.getName().equals("Chains of Mephistopheles") ) {
                if ( null == chainsList )
                    chainsList = new ArrayList<Card>();
                chainsList.add(c);
            }
        }
        if (chainsList != null && (numDrawnThisDrawStep > 0 || !game.getPhaseHandler().is(PhaseType.DRAW))) {
            for(Card c : chainsList) {
                // I have to target this player - don't know how to do it.
                Target target = new Target(c, null, "");
                target.addTarget(this);

                if (getCardsIn(ZoneType.Hand).isEmpty()) {
                    SpellAbility saMill = AbilityFactory.getAbility(c.getSVar("MillOne"), c);
                    saMill.setActivatingPlayer(c.getController());
                    saMill.setTarget(target);
                    AbilityUtils.resolve(saMill, false);
                    
                    return drawn; // Draw is cancelled
                } else { 
                    SpellAbility saDiscard = AbilityFactory.getAbility(c.getSVar("DiscardOne"), c);
                    saDiscard.setActivatingPlayer(c.getController());
                    saDiscard.setTarget(target);
                    AbilityUtils.resolve(saDiscard, false);
                }
            }
        }
        // End of = Chains of Mephistopheles hardcode. ========= 

        if (!library.isEmpty()) {

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
            if ( game.getPhaseHandler().is(PhaseType.DRAW))
                this.numDrawnThisDrawStep++;

            // Miracle draws
            if (this.numDrawnThisTurn == 1 && game.getPhaseHandler().getTurn() != 1) {
                drawMiracle(c);
            }

            // Run triggers
            final HashMap<String, Object> runParams = new HashMap<String, Object>();
            runParams.put("Card", c);
            runParams.put("Number", this.numDrawnThisTurn);
            runParams.put("Player", this);
            game.getTriggerHandler().runTrigger(TriggerType.Drawn, runParams, false);
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
        allExcStack.addAll(inboundTokens);
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
            if (nDr > 0 && cntLibrary >= nDr) {
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
        this.numDrawnThisDrawStep = 0;
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

    /**
     * <p>
     * Getter for the field <code>numDrawnThisTurnDrawStep</code>.
     * </p>
     * 
     * @return a int.
     */
    public final int numDrawnThisDrawStep() {
        return this.numDrawnThisDrawStep;
    }

    // //////////////////////////////
    // /
    // / replaces Singletons.getModel().getGameAction().discard* methods
    // /
    // //////////////////////////////

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
    public final boolean discard(final Card c, final SpellAbility sa) {
        FThreads.assertExecutedByEdt(false);
        // TODO: This line should be moved inside CostPayment somehow
        /*if (sa != null) {
            sa.addCostToHashList(c, "Discarded");
        }*/
        final Card source = sa != null ? sa.getSourceCard() : null;

        // Replacement effects
        final HashMap<String, Object> repRunParams = new HashMap<String, Object>();
        repRunParams.put("Event", "Discard");
        repRunParams.put("Card", c);
        repRunParams.put("Source", source);
        repRunParams.put("Affected", this);

        if (game.getReplacementHandler().run(repRunParams) != ReplacementResult.NotReplaced) {
            return false;
        }

        game.getAction().discardMadness(c, this);

        boolean discardToTopOfLibrary = null != sa && sa.hasParam("DiscardToTopOfLibrary");

        if (discardToTopOfLibrary) {
            game.getAction().moveToLibrary(c, 0);
            // Play the Discard sound
            game.getEvents().post(new GameEventCardDiscarded());
            this.numDiscardedThisTurn++;
        } else {
            game.getAction().moveToGraveyard(c);

            // Play the Discard sound
            game.getEvents().post(new GameEventCardDiscarded());
            this.numDiscardedThisTurn++;
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
        game.getTriggerHandler().runTrigger(TriggerType.Discarded, runParams, false);
        return true;

    } // end doDiscard

    /**
     * <p>
     * Getter for the field <code>numDiscardedThisTurn</code>.
     * </p>
     * 
     * @return a int.
     */
    public final int getNumDiscardedThisTurn() {
        return this.numDiscardedThisTurn;
    }
    
    /**
     * <p>
     * Getter for the field <code>numDiscardedThisTurn</code>.
     * </p>
     * 
     * @return a int.
     */
    public final void resetNumDiscardedThisTurn() {
        this.numDiscardedThisTurn = 0;
    }

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
        final List<Card> lib = new ArrayList<Card>(this.getCardsIn(ZoneType.Library));
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
        final List<Card> list = Lists.newArrayList(this.getCardsIn(ZoneType.Library));

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

        int s = list.size();
        for (int i = 0; i < s; i++) {
            list.add(random.nextInt(s - 1), list.remove(random.nextInt(s)));
        }

        Collections.shuffle(list, random);
        Collections.shuffle(list, random);
        Collections.shuffle(list, random);
        Collections.shuffle(list, random);
        Collections.shuffle(list, random);
        Collections.shuffle(list, random);

        this.getZone(ZoneType.Library).setCards(list);

        // Run triggers
        final HashMap<String, Object> runParams = new HashMap<String, Object>();
        runParams.put("Player", this);
        game.getTriggerHandler().runTrigger(TriggerType.Shuffled, runParams, false);

        // Play the shuffle sound
        game.getEvents().post(new GameEventShuffle());
    } // shuffle
      // //////////////////////////////

    // //////////////////////////////


    // /////////////////////////////

    /**
     * <p>
     * playLand.
     * </p>
     * 
     * @param land
     *            a {@link forge.Card} object.
     */
    public final boolean playLand(final Card land, final boolean ignoreTiming) {
        FThreads.assertExecutedByEdt(false);
        if (this.canPlayLand(land, ignoreTiming)) {
            land.setController(this, 0);
            game.getAction().moveTo(this.getZone(ZoneType.Battlefield), land);
            CardFactoryUtil.playLandEffects(land);
            this.numLandsPlayed++;

            // check state effects for static animate (Living Lands, Conversion,
            // etc...)
            game.getAction().checkStateEffects();

            // add to log
            game.getGameLog().add(GameEventType.LAND, this + " played " + land);

            // play a sound
            game.getEvents().post(new GameEventLandPlayed(this, land));

            // Run triggers
            final HashMap<String, Object> runParams = new HashMap<String, Object>();
            runParams.put("Card", land);
            game.getTriggerHandler().runTrigger(TriggerType.LandPlayed, runParams, false);
            game.getStack().unfreezeStack();
            return true;
        }

        game.getStack().unfreezeStack();
        return false;
    }

    /**
     * <p>
     * canPlayLand.
     * </p>
     * 
     * @return a boolean.
     */
    public final boolean canPlayLand(final Card land) {
        return canPlayLand(land, false);
    }
    /**
     * <p>
     * canPlayLand.
     * </p>
     * 
     * @return a boolean.
     */
    public final boolean canPlayLand(Card land, final boolean ignoreTiming) {

        if (!ignoreTiming && !this.canCastSorcery()) {
            return false;
        }

        // CantBeCast static abilities
        for (final Card ca : game.getCardsIn(ZoneType.listValueOf("Battlefield,Command"))) {
            final ArrayList<StaticAbility> staticAbilities = ca.getStaticAbilities();
            for (final StaticAbility stAb : staticAbilities) {
                if (stAb.applyAbility("CantPlayLand", land, this)) {
                    return false;
                }
            }
        }
        
        if( land != null && land.getOwner() != this )
            return false;

        // Dev Mode
        if (this.getLobbyPlayer().getType() == PlayerType.HUMAN && Preferences.DEV_MODE &&
            Singletons.getModel().getPreferences().getPrefBoolean(FPref.DEV_UNLIMITED_LAND)) {
            return true;
        }

        // check for adjusted max lands play per turn
        int adjMax = 0;
        for (String keyword : this.getKeywords()) {
            if (keyword.startsWith("AdjustLandPlays")) {
                final String[] k = keyword.split(":");
                adjMax += Integer.valueOf(k[1]);
            }
        }
        final int adjCheck = this.maxLandsToPlay + adjMax;
        // System.out.println("Max lands for player " + this.getName() + ": " + adjCheck);

        return this.numLandsPlayed < adjCheck || this.isCardInPlay("Fastbond") || this.isCardInCommand("Naya");
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
        if (state != GameLossReason.OpponentWon) {
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
        FThreads.assertExecutedByEdt(false);
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
        for (Player p : game.getPlayers()) {
            if (p == this || p.getOutcome() != null) {

                continue; // except self and already dead
            }
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

        if (this.getOutcome() != null) {
            return this.getOutcome().lossState != null;
        }

        if (this.poisonCounters >= 10) {
            return this.loseConditionMet(GameLossReason.Poisoned, null);
        }

        final boolean hasNoLife = this.getLife() <= 0;
        if (hasNoLife && !this.cantLoseForZeroOrLessLife()) {
            return this.loseConditionMet(GameLossReason.LifeReachedZero, null);
        }

        return false;
    }

    public final boolean hasLost() { 
        return this.getOutcome() != null && this.getOutcome().lossState != null;
    }

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
     * hasBloodthirst.
     * </p>
     * 
     * @return a boolean.
     */
    public final boolean hasBloodthirst() {
        for (Player opp : this.getOpponents()) {
            if (opp.getAssignedDamage() > 0) {
                return true;
            }
        }
        return false;
    }

    /**
     * <p>
     * getBloodthirstAmount.
     * </p>
     * 
     * @return a int.
     */
    public final int getBloodthirstAmount() {
        int blood = 0;
        for (Player opp : this.getOpponents()) {
            blood += opp.getAssignedDamage();
        }
        return blood;
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
            if (this.equals(sourceController) || !this.isOpponentOf(sourceController)) {
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
            if (this.equals(sourceController) || !this.isOpponentOf(sourceController)) {
                return false;
            }
        } else if (property.equals("Other")) {
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
        } else if (property.equals("attackedBySourceThisCombat")) {
            if (!this.equals(game.getCombat().getDefenderPlayerByAttacker(source))) {
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
        } else if (property.startsWith("DeclaredAttackerThisTurn")) {
            if (this.attackersDeclaredThisTurn <= 0) {
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
        } else if (property.startsWith("EnchantedBy")) {
            if (!this.getEnchantedBy().contains(source)) {
                return false;
            }
        } else if (property.startsWith("Chosen")) {
            if (source.getChosenPlayer() == null || !source.getChosenPlayer().equals(this)) {
                return false;
            }
        } else if (property.startsWith("withMore")) {
            final String cardType = property.split("sThan")[0].substring(8);
            final List<Card> oppList = CardLists.filter(this.getCardsIn(ZoneType.Battlefield), CardPredicates.isType(cardType));
            final List<Card> yourList = CardLists.filter(sourceController.getCardsIn(ZoneType.Battlefield), CardPredicates.isType(cardType));
            if (oppList.size() <= yourList.size()) {
                return false;
            }
        } else if (property.startsWith("withMost")) {
            if (property.substring(8).equals("Life")) {
                int highestLife = -50; // Negative base just in case a few Lich's are running around
                Player healthiest = null;
                for (final Player p : game.getPlayers()) {
                    if (p.getLife() > highestLife) {
                        highestLife = p.getLife();
                        healthiest = p;
                    }
                }
                if (!this.equals(healthiest)) {
                    return false;
                }
            }
            else if (property.substring(8).equals("CardsInHand")) {
                int largestHand = 0;
                Player withLargestHand = null;
                for (final Player p : game.getPlayers()) {
                    if (p.getCardsIn(ZoneType.Hand).size() > largestHand) {
                        largestHand = p.getCardsIn(ZoneType.Hand).size();
                        withLargestHand = p;
                    }
                }
                if (!this.equals(withLargestHand)) {
                    return false;
                }
            }
            else if (property.substring(8).startsWith("Type")) {
                String type = property.split("Type")[1];
                boolean checkOnly = false;
                if (type.endsWith("Only")) {
                    checkOnly = true;
                    type = type.replace("Only", "");
                }
                int typeNum = 0;
                List<Player> controlmost = new ArrayList<Player>();
                for (final Player p : game.getPlayers()) {
                    final int num = CardLists.getType(p.getCardsIn(ZoneType.Battlefield), type).size();
                    if (num > typeNum) {
                        typeNum = num;
                        controlmost.clear();
                    }
                    if (num == typeNum) {
                        controlmost.add(p);
                    }
                }
                if (checkOnly && controlmost.size() != 1) {
                    return false;
                }
                if (!controlmost.contains(this)) {
                    return false;
                }
            }
        } else if (property.startsWith("withLowest")) {
            if (property.substring(10).equals("Life")) {
                int lowestLife = 1000;
                List<Player> lowestlifep = new ArrayList<Player>();
                for (final Player p : game.getPlayers()) {
                    if (p.getLife() == lowestLife) {
                        lowestlifep.add(p);
                    } else if (p.getLife() < lowestLife) {
                        lowestLife = p.getLife();
                        lowestlifep.clear();
                        lowestlifep.add(p);
                    }
                }
                if (!lowestlifep.contains(this)) {
                    return false;
                }
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
     * @return the unlimitedHandSize
     */
    public boolean isUnlimitedHandSize() {
        return unlimitedHandSize;
    }

    /**
     * @param unlimitedHandSize0 the unlimitedHandSize to set
     */
    public void setUnlimitedHandSize(boolean unlimited) {
        this.unlimitedHandSize = unlimited;
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
     * Getter for the field <code>lifeGainedThisTurn</code>.
     * </p>
     * 
     * @return a int.
     */
    public final int getLifeGainedThisTurn() {
        return this.lifeGainedThisTurn;
    }

    /**
     * <p>
     * Setter for the field <code>lifeGainedThisTurn</code>.
     * </p>
     * 
     * @param n
     *            a int.
     */
    public final void setLifeGainedThisTurn(final int n) {
        this.lifeGainedThisTurn = n;
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

        return Math.abs(subtractedHash) / subtractedHash;
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
    }

    public static class Accessors {
        public static Function<Player, Integer> FN_GET_LIFE = new Function<Player, Integer>() {
            @Override
            public Integer apply(Player input) {
                return input.getLife();
            }
        };

        public static Function<Player, String> FN_GET_NAME = new Function<Player, String>() {
            @Override
            public String apply(Player input) {
                return input.getName();
            }
        };

        public static Function<Player, Integer> countCardsInZone(final ZoneType zone) {
            return new Function<Player, Integer>() {
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
    public final LobbyPlayer getLobbyPlayer() {
        return getController().getLobbyPlayer();
    }
    
    public final boolean isMindSlaved() {
        return controller.getLobbyPlayer() != controllerCreator.getLobbyPlayer();
    }
    
    public final void releaseControl() {
        controller = controllerCreator;
        game.getEvents().post(new GameEventPlayerControl(this, getLobbyPlayer(), null));
    }

    public final void obeyNewMaster(PlayerController pc) {
        LobbyPlayer oldController = getLobbyPlayer();
        controller = pc;
        game.getEvents().post(new GameEventPlayerControl(this, oldController, pc.getLobbyPlayer()));
    }


    private void setOutcome(PlayerOutcome outcome) {
        stats.setOutcome(outcome);
    }

    /**
     * TODO: Write javadoc for this method.
     */
    public void onGameOver() {
        if (null == stats.getOutcome()) {

            setOutcome(PlayerOutcome.win()); // then won!
        }
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
        return getZone(ZoneType.Battlefield).contains(CardPredicates.nameEquals(cardName));
    }

    public boolean isCardInCommand(final String cardName) {
        return getZone(ZoneType.Command).contains(CardPredicates.nameEquals(cardName));
    }

    public List<Card> getColoredCardsInPlay(final String color) {
        return CardLists.filter(getCardsIn(ZoneType.Battlefield), CardPredicates.isColor(MagicColor.fromName(color)));
    }

    public int getCounterDoublersMagnitude(final CounterType type) {
        int counterDoublers = getCardsIn(ZoneType.Battlefield, "Doubling Season").size()
                + CardLists.filter(getGame().getCardsIn(ZoneType.Command), 
                        CardPredicates.nameEquals("Selesnya Loft Gardens")).size();
        if (type == CounterType.P1P1) {
            counterDoublers += getCardsIn(ZoneType.Battlefield, "Corpsejack Menace").size();
        }
        return 1 << counterDoublers;
    }

    public int getTokenDoublersMagnitude() {
        final int tokenDoublers = getCardsIn(ZoneType.Battlefield, "Parallel Lives").size()
                + getCardsIn(ZoneType.Battlefield, "Doubling Season").size()
                + CardLists.filter(getGame().getCardsIn(ZoneType.Command), 
                        CardPredicates.nameEquals("Selesnya Loft Gardens")).size();;
        return 1 << tokenDoublers; // pow(a,0) = 1; pow(a,1) = a
    }

    public void onCleanupPhase() {
        for (Card c : getCardsIn(ZoneType.Hand)) {
            c.setDrawnThisTurn(false);
        }
        resetPreventNextDamage();
        resetNumDrawnThisTurn();
        resetNumDiscardedThisTurn();
        setAttackedWithCreatureThisTurn(false);
        setNumLandsPlayed(0);
        clearAssignedDamage();
        resetAttackersDeclaredThisTurn();
    }

    public boolean canCastSorcery() {
        PhaseHandler now = game.getPhaseHandler();
        return now.isPlayerTurn(this) && now.getPhase().isMain() && game.getStack().isEmpty();
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

        final Card source = sa.getRootAbility().getSourceCard();
        boolean onlyThis = true;

        for (final Card card : game.getCardsIn(ZoneType.Stack)) {
            if (!card.equals(source)) {
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
    public final PlayerController getController() {
        return controller;
    }
    public final void setFirstController(PlayerController ctrlr) {
        if( null != controllerCreator ) throw new IllegalStateException("Controller creator already assigned");
        controllerCreator = ctrlr; 
        controller = ctrlr;
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
                if (GuiDialog.confirm(crd, "Untap " + crd + "?")) {
                    crd.untap();
                    return true;
                }
            } else {
                // TODO Should AI skip his turn for time vault?
            }
        }
        return false;
    }

    public int getStartingHandSize() {

        return this.startingHandSize;
    }

    public void setStartingHandSize(int shs) {

        this.startingHandSize = shs;
    }

    /**
     * 
     * Takes the top plane of the planar deck and put it face up in the command zone.
     * Then runs triggers. 
     */
    public void planeswalk()
    {
        planeswalkTo(Arrays.asList(getZone(ZoneType.PlanarDeck).get(0)));
    }
    
    /**
     * Puts the planes in the argument and puts them face up in the command zone.
     * Then runs triggers.
     * 
     * @param destinations The planes to planeswalk to.
     */
    public void planeswalkTo(final List<Card> destinations)
    {
        System.out.println(this.getName() + ": planeswalk to " + destinations.toString());
        currentPlanes.addAll(destinations);

        for(Card c : currentPlanes) {
            getZone(ZoneType.PlanarDeck).remove(c);
            getZone(ZoneType.Command).add(c);
        }
        
        //DBG
        //System.out.println("CurrentPlanes: " + currentPlanes);
        //System.out.println("ActivePlanes: " + game.getActivePlanes());
        //System.out.println("CommandPlanes: " + getZone(ZoneType.Command).getCards());
        
        game.setActivePlanes(currentPlanes);
        //Run PlaneswalkedTo triggers here.
        HashMap<String,Object> runParams = new HashMap<String,Object>();
        runParams.put("Card", currentPlanes);
        game.getTriggerHandler().runTrigger(TriggerType.PlaneswalkedTo, runParams,false);
    }
    
    /**
     * 
     * Puts my currently active planes, if any, at the bottom of my planar deck.
     */
    public void leaveCurrentPlane()
    {
        if(!currentPlanes.isEmpty())
        {
          //Run PlaneswalkedFrom triggers here.
            HashMap<String,Object> runParams = new HashMap<String,Object>();
            runParams.put("Card", currentPlanes);
            game.getTriggerHandler().runTrigger(TriggerType.PlaneswalkedFrom, runParams,false);
            
            for(Card c : currentPlanes) {
                game.getZoneOf(c).remove(c);
                c.clearControllers();
                getZone(ZoneType.PlanarDeck).add(c);
            }
            currentPlanes.clear();
        }

        //DBG
        //System.out.println("CurrentPlanes: " + currentPlanes);
        //System.out.println("ActivePlanes: " + game.getActivePlanes());
        //System.out.println("CommandPlanes: " + getZone(ZoneType.Command).getCards());
    }
    
    /**
     * 
     * Sets up the first plane of a round.
     */
    public void initPlane()
    {
        Card firstPlane = null;
        while(true)
        {
            firstPlane = getZone(ZoneType.PlanarDeck).get(0);
            getZone(ZoneType.PlanarDeck).remove(firstPlane);
            if(firstPlane.getType().contains("Phenomenon"))
            {
                getZone(ZoneType.PlanarDeck).add(firstPlane);
            }
            else
            {
                currentPlanes.add(firstPlane);
                getZone(ZoneType.Command).add(firstPlane);
                break;
            }
        }
        
        game.setActivePlanes(currentPlanes);
    }

    /**
     * <p>
     * resetAttackedThisCombat.
     * </p>
     * 
     * @param player
     *            a {@link forge.game.player.Player} object.
     */
    public final void resetAttackedThisCombat() {
        // resets the status of attacked/blocked this phase
        List<Card> list = CardLists.filter(getCardsIn(ZoneType.Battlefield), Presets.CREATURES);
    
        for (int i = 0; i < list.size(); i++) {
            final Card c = list.get(i);
            if (c.getDamageHistory().getCreatureAttackedThisCombat()) {
                c.getDamageHistory().setCreatureAttackedThisCombat(false);
            }
            if (c.getDamageHistory().getCreatureBlockedThisCombat()) {
                c.getDamageHistory().setCreatureBlockedThisCombat(false);
            }
    
            if (c.getDamageHistory().getCreatureGotBlockedThisCombat()) {
                c.getDamageHistory().setCreatureGotBlockedThisCombat(false);
            }
        }
    }

    /**
     * <p>
     * drawMiracle.
     * </p>
     * 
     * @param card
     *            a {@link forge.Card} object.
     */
    public final void drawMiracle(final Card card) {
        // Whenever a card with miracle is the first card drawn in a turn,
        // you may cast it for it's miracle cost
        if (card.getMiracleCost() == null) {
            return;
        }

        final SpellAbility playForMiracleCost = card.getFirstSpellAbility().copy();
        playForMiracleCost.setPayCosts(new Cost(card.getMiracleCost(), false));
        playForMiracleCost.setStackDescription(card.getName() + " - Cast via Miracle");

        // TODO Convert this to a Trigger
        final Ability miracleTrigger = new MiracleTrigger(card, ManaCost.ZERO, playForMiracleCost);
        miracleTrigger.setStackDescription(card.getName() + " - Miracle.");
        miracleTrigger.setActivatingPlayer(card.getOwner());
        miracleTrigger.setTrigger(true);
    
        game.getStack().add(miracleTrigger);
    }

    public boolean isSkippingDraw() {
    
        if (hasKeyword("Skip your next draw step.")) {
            removeKeyword("Skip your next draw step.");
            return true;
        }
    
        if (hasKeyword("Skip your draw step.")) {
            return true;
        }
    
        return false;
    }
    
    public void addInboundToken(Card c)
    {
        inboundTokens.add(c);
    }
    
    public void removeInboundToken(Card c)
    {
        inboundTokens.remove(c);
    }

    /** 
     * TODO: Write javadoc for this type.
     *
     */
    private final class MiracleTrigger extends Ability {
        private final SpellAbility miracle;
    
        /**
         * TODO: Write javadoc for Constructor.
         * @param sourceCard
         * @param manaCost
         * @param miracle
         */
        private MiracleTrigger(Card sourceCard, ManaCost manaCost, SpellAbility miracle) {
            super(sourceCard, manaCost);
            this.miracle = miracle;
        }
    
        @Override
        public void resolve() {
            miracle.setActivatingPlayer(getSourceCard().getOwner());
            // pay miracle cost here.
            getSourceCard().getOwner().getController().playMiracle(miracle, getSourceCard());
        }
    }

    // These are used by UI to mark the player currently being attacked or targeted
    // Very bad practice! Someone blame on me.
    private boolean highlited = false; 
    public final void setHighlited(boolean value) { highlited = value; }
    public final boolean isHighlited() { return highlited; }

    /**
     * TODO: Write javadoc for this method.
     */
    public void onMulliganned() {
        game.getEvents().post(new GameEventMulligan(this)); // quest listener may interfere here
        final int newHand = getCardsIn(ZoneType.Hand).size();
        game.getGameLog().add(GameEventType.MULLIGAN, this + " has mulliganed down to " + newHand + " cards.");
        stats.notifyHasMulliganed();
        stats.notifyOpeningHandSize(newHand);
    }

}
