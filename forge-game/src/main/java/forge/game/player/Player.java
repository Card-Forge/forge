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

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import forge.LobbyPlayer;
import forge.card.MagicColor;
import forge.card.mana.ManaCost;
import forge.game.*;
import forge.game.ability.AbilityFactory;
import forge.game.ability.AbilityUtils;
import forge.game.ability.ApiType;
import forge.game.card.Card;
import forge.game.card.CardFactoryUtil;
import forge.game.card.CardLists;
import forge.game.card.CardPredicates;
import forge.game.card.CardPredicates.Presets;
import forge.game.event.*;
import forge.game.mana.ManaPool;
import forge.game.phase.PhaseHandler;
import forge.game.phase.PhaseType;
import forge.game.replacement.ReplacementEffect;
import forge.game.replacement.ReplacementHandler;
import forge.game.replacement.ReplacementResult;
import forge.game.spellability.Ability;
import forge.game.spellability.SpellAbility;
import forge.game.staticability.StaticAbility;
import forge.game.trigger.Trigger;
import forge.game.trigger.TriggerHandler;
import forge.game.trigger.TriggerType;
import forge.game.zone.PlayerZone;
import forge.game.zone.PlayerZoneBattlefield;
import forge.game.zone.Zone;
import forge.game.zone.ZoneType;
import forge.item.IPaperCard;
import forge.util.Aggregates;
import forge.util.Lang;
import forge.util.MyRandom;

import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentSkipListMap;

/**
 * <p>
 * Abstract Player class.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 */
public class Player extends GameEntity implements Comparable<Player> {
    private final Map<Card,Integer> commanderDamage = new HashMap<Card,Integer>();

    /** The poison counters. */
    private int poisonCounters = 0;

    /** The life. */
    private int life = 20;

    /** The life this player started the game with. */
    private int startingLife = 20;

    /** The assigned damage. */
    private final Map<Card, Integer> assignedDamage = new HashMap<Card, Integer>();

    /** Number of spells cast this turn. */
    private int spellsCastThisTurn = 0;

    /** The life lost this turn. */
    private int lifeLostThisTurn = 0;

    /** The life lost last turn. */
    private int lifeLostLastTurn = 0;

    /** The life Gained this turn. */
    private int lifeGainedThisTurn = 0;

    /** The num power surge lands. */
    private int numPowerSurgeLands;

    /** The number of times this player has searched his library. */
    private int numLibrarySearchedOwn = 0;

    /** The prowl. */
    private ArrayList<String> prowl = new ArrayList<String>();

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

    /** The named card. */
    private String namedCard = "";

    /** The num drawn this turn. */
    private int numDrawnThisTurn = 0;
    private int numDrawnThisDrawStep = 0;

    /** The num discarded this turn. */
    private int numDiscardedThisTurn = 0;
    
    private int numCardsInHandStartedThisTurnWith = 0;

    /** A list of tokens not in play, but on their way.
     * This list is kept in order to not break ETB-replacement
     * on tokens. */
    private List<Card> inboundTokens = new ArrayList<Card>();

    /** The keywords. */
    private Map<Long, KeywordsChange> changedKeywords = new ConcurrentSkipListMap<Long, KeywordsChange>();

    /** The mana pool. */
    private ManaPool manaPool = new ManaPool(this);

    /** The must attack entity. */
    private GameEntity mustAttackEntity = null;

    /** The attackedWithCreatureThisTurn. */
    private boolean attackedWithCreatureThisTurn = false;

    private boolean activateLoyaltyAbilityThisTurn = false;

    /** The playerAttackCountThisTurn. */
    private int attackersDeclaredThisTurn = 0;

    /** The zones. */
    private final Map<ZoneType, PlayerZone> zones = new EnumMap<ZoneType, PlayerZone>(ZoneType.class);

    private List<Card> currentPlanes = new ArrayList<Card>();

    private PlayerStatistics stats = new PlayerStatistics();
    protected PlayerController controller;
    protected PlayerController controllerCreator = null;

    private Player mindSlaveMaster = null;

    private int teamNumber = -1;

    private Card activeScheme = null;

    private Card commander = null;

    /** The Constant ALL_ZONES. */
    public static final List<ZoneType> ALL_ZONES = Collections.unmodifiableList(Arrays.asList(ZoneType.Battlefield,
            ZoneType.Library, ZoneType.Graveyard, ZoneType.Hand, ZoneType.Exile, ZoneType.Command, ZoneType.Ante,
            ZoneType.Sideboard, ZoneType.PlanarDeck,ZoneType.SchemeDeck));

    protected final Game game;

    private boolean triedToDrawFromEmptyLibrary = false;

    private boolean isPlayingExtraTrun = false;

    public  boolean canCheatPlayUnlimitedLands = false;

    private List<Card> lostOwnership = new ArrayList<Card>();
    private List<Card> gainedOwnership = new ArrayList<Card>();

    private int numManaConversion = 0;

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
    public Player(String name, Game game0) {
        game = game0;
        for (final ZoneType z : Player.ALL_ZONES) {
            final PlayerZone toPut = z == ZoneType.Battlefield ? new PlayerZoneBattlefield(z, this) : new PlayerZone(z, this);
            this.zones.put(z, toPut);
        }
        
        this.setName(chooseName(name));
    }

    private String chooseName(String originalName) {
        String nameCandidate = originalName;
        for (int i = 2; i <= 8; i++) { // several tries, not matter how many
            boolean haveDuplicates = false;
            for (Player p : game.getPlayers()) {
                if (p.getName().equals(nameCandidate)) {
                    haveDuplicates = true;
                    break;
                }
            }
            if (!haveDuplicates) {
                return nameCandidate;
            }
            nameCandidate = Lang.getOrdinal(i) + " " + originalName;
        }
        return nameCandidate;
    }

    @Override
    public Game getGame() { // I'll probably regret about this
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
            if (p.isOpponentOf(this)) {
                return p;
            }
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
            if (p.isOpponentOf(this)) {
                result.add(p);
            }
        }
        return result;
    }

    /**
     * Find the smallest life total amongst this player's opponents.
     * 
     * @return the life total of the opponent with the least life.
     */
    public final int getOpponentsSmallestLifeTotal() {
    	return Aggregates.min(this.getOpponents(), Accessors.FN_GET_LIFE_TOTAL);
    }

    /**
     * Find the greatest life total amongst this player's opponents.
     * 
     * @return the life total of the opponent with the most life.
     */
    public final int getOpponentsGreatestLifeTotal() {
    	return Aggregates.max(this.getOpponents(), Accessors.FN_GET_LIFE_TOTAL);
    }
    
    /**
     * Get the total number of poison counters amongst this player's opponents.
     * 
     * @return the total number of poison counters amongst this player's opponents.
     */
    public final int getOpponentsTotalPoisonCounters() {
    	return Aggregates.sum(this.getOpponents(), Accessors.FN_GET_POISON_COUNTERS);
    }

    /**
     * returns allied players.
     * Should keep player relations somewhere in the match structure
     * @return
     */
    public final List<Player> getAllies() {
        List<Player> result = new ArrayList<Player>();
        for (Player p : game.getPlayers()) {
            if (!p.isOpponentOf(this)) {
                result.add(p);
            }
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
        List<Player> opponents = this.getOpponents();
        Player weakest = opponents.get(0);
        for (int i = 1; i < opponents.size(); i++) {
            if (weakest.getLife() > opponents.get(i).getLife()) {
                weakest = opponents.get(i);
            }
        }
        return weakest;
    }

    public boolean isOpponentOf(Player other) {
    	return other != this && other != null && (other.teamNumber < 0 || other.teamNumber != this.teamNumber);
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
     *            a {@link forge.game.card.Card} object.
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
     * gainLife.
     * </p>
     * 
     * @param toGain
     *            a int.
     * @param source
     *            a {@link forge.game.card.Card} object.
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
            int oldLife = life;
            this.life += lifeGain;
            newLifeSet = true;
            this.lifeGainedThisTurn += lifeGain;

            // Run triggers
            final HashMap<String, Object> runParams = new HashMap<String, Object>();
            runParams.put("Player", this);
            runParams.put("LifeAmount", lifeGain);
            game.getTriggerHandler().runTrigger(TriggerType.LifeGained, runParams, false);

            game.fireEvent(new GameEventPlayerLivesChanged(this, oldLife, life));
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
            int oldLife = life;
            this.life -= toLose;
            lifeLost = toLose;
            game.fireEvent(new GameEventPlayerLivesChanged(this, oldLife, life));
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
     *            a {@link forge.game.card.Card} object.
     * @return a boolean.
     */
    public final boolean payLife(final int lifePayment, final Card source) {
        if (!this.canPayLife(lifePayment)) {
            return false;
        }
        
        if (lifePayment <= 0) 
        	return true;
        
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
     *            a {@link forge.game.card.Card} object.
     * @param isCombat
     *            a boolean.
     * @return whether or not damage was dealt
     */
    @Override
    public final boolean addDamageAfterPrevention(final int amount, final Card source, final boolean isCombat) {
        if (amount <= 0) {
            return false;
        }
        //String additionalLog = "";
        source.addDealtDamageToPlayerThisTurn(this.getName(), amount);

        boolean infect = source.hasKeyword("Infect")
                || this.hasKeyword("All damage is dealt to you as though its source had infect.");

        if (infect) {
            this.addPoisonCounters(amount, source);
        } else {
            // Worship does not reduce the damage dealt but changes the effect
            // of the damage
            if (this.hasKeyword("Damage that would reduce your life total to less than 1 reduces it to 1 instead.")
                    && this.life <= amount) {
                this.loseLife(Math.min(amount, this.life - 1));
            } else {
                // rule 118.2. Damage dealt to a player normally causes that
                // player to lose that much life.
                this.loseLife(amount);
            }
        }

        if (source.isCommander() && isCombat) {
            if (!commanderDamage.containsKey(source)) {
                commanderDamage.put(source, amount);
            }
            else {
                commanderDamage.put(source,commanderDamage.get(source) + amount);
            }
        }

        this.assignedDamage.put(source, amount);
        if (source.hasKeyword("Lifelink")) {
            source.getController().gainLife(amount, source);
        }
        source.getDamageHistory().registerDamage(this);

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
        runParams.put("DamageAmount", amount);
        runParams.put("IsCombatDamage", isCombat);
        game.getTriggerHandler().runTrigger(TriggerType.DamageDone, runParams, false);

        game.fireEvent(new GameEventPlayerDamaged(this, source, amount, isCombat, infect));

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
     *            a {@link forge.game.card.Card} object.
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
     *            a {@link forge.game.card.Card} object.
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
            } else if (c.getName().equals("Pyromancer's Gauntlet")) {
                if (c.getController().equals(source.getController()) && source.isRed()
                        && (source.isInstant() || source.isSorcery() || source.isPlaneswalker())) {
                    restDamage += 2;
                }
            } else if (c.getName().equals("Furnace of Rath") || c.getName().equals("Dictate of the Twin Gods")) {
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
     *            a {@link forge.game.card.Card} object.
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
     *            a {@link forge.game.card.Card} object.
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

        boolean DEBUGShieldsWithEffects = false;
        while (!this.getPreventNextDamageWithEffect().isEmpty() && restDamage != 0) {
            TreeMap<Card, Map<String, String>> shieldMap = this.getPreventNextDamageWithEffect();
            List<Card> preventionEffectSources = new ArrayList<Card>(shieldMap.keySet());
            Card shieldSource = preventionEffectSources.get(0);
            if (preventionEffectSources.size() > 1) {
                Map<String, Card> choiceMap = new TreeMap<String, Card>();
                List<String> choices = new ArrayList<String>();
                for (final Card key : preventionEffectSources) {
                    String effDesc = shieldMap.get(key).get("EffectString");
                    int descIndex = effDesc.indexOf("SpellDescription");
                    effDesc = effDesc.substring(descIndex + 18);
                    String shieldDescription = key.toString() + " - " + shieldMap.get(shieldSource).get("ShieldAmount")
                            + " shields - " + effDesc;
                    choices.add(shieldDescription);
                    choiceMap.put(shieldDescription, key);
                }
                shieldSource = this.getController().chooseProtectionShield(this, choices, choiceMap);
            }
            if (DEBUGShieldsWithEffects) {
                System.out.println("Prevention shield source: " + shieldSource);
            }

            int shieldAmount = Integer.valueOf(shieldMap.get(shieldSource).get("ShieldAmount"));
            int dmgToBePrevented = Math.min(restDamage, shieldAmount);
            if (DEBUGShieldsWithEffects) {
                System.out.println("Selected source initial shield amount: " + shieldAmount);
                System.out.println("Incoming damage: " + restDamage);
                System.out.println("Damage to be prevented: " + dmgToBePrevented);
            }

            //Set up ability
            SpellAbility shieldSA = null;
            String effectAbString = shieldMap.get(shieldSource).get("EffectString");
            effectAbString = effectAbString.replace("PreventedDamage", Integer.toString(dmgToBePrevented));
            effectAbString = effectAbString.replace("ShieldEffectTarget", shieldMap.get(shieldSource).get("ShieldEffectTarget"));
            if (DEBUGShieldsWithEffects) {
                System.out.println("Final shield ability string: " + effectAbString);
            }
            shieldSA = AbilityFactory.getAbility(effectAbString, shieldSource);
            if (shieldSA.usesTargeting()) {
                System.err.println(shieldSource + " - Targeting for prevention shield's effect should be done with initial spell");
            }

            boolean apiIsEffect = (shieldSA.getApi() == ApiType.Effect);
            List<Card> cardsInCommand = null;
            if (apiIsEffect) {
                cardsInCommand = this.getGame().getCardsIn(ZoneType.Command);
            }

            this.getController().playSpellAbilityNoStack(shieldSA, true);
            if (apiIsEffect) {
                List<Card> newCardsInCommand = this.getGame().getCardsIn(ZoneType.Command);
                newCardsInCommand.removeAll(cardsInCommand);
                if (!newCardsInCommand.isEmpty()) {
                	newCardsInCommand.get(0).setSVar("PreventedDamage", "Number$" + Integer.toString(dmgToBePrevented));
                }
            }
            this.subtractPreventNextDamageWithEffect(shieldSource, restDamage);
            restDamage = restDamage - dmgToBePrevented;

            if (DEBUGShieldsWithEffects) {
                System.out.println("Remaining shields: "
                        + (shieldMap.containsKey(shieldSource) ? shieldMap.get(shieldSource).get("ShieldAmount") : "all shields used"));
                System.out.println("Remaining damage: " + restDamage);
            }
        }

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
     * Get the total damage assigned to this player's opponents this turn.
     * 
     * @return the total damage assigned to this player's opponents this turn.
     */
    public final int getOpponentsAssignedDamage() {
    	return Aggregates.sum(this.getOpponents(), Accessors.FN_GET_ASSIGNED_DAMAGE);
    }
    
    /**
     * Get the greatest amount of damage assigned to a single opponent this turn.
     * 
     * @return the greatest amount of damage assigned to a single opponent this turn.
     */
    public final int getMaxOpponentAssignedDamage() {
    	return Aggregates.max(this.getOpponents(), Accessors.FN_GET_ASSIGNED_DAMAGE);
    }

    /**
     * <p>
     * addCombatDamage.
     * </p>
     * 
     * @param damage
     *            a int.
     * @param source
     *            a {@link forge.game.card.Card} object.
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
            setPoisonCounters(poisonCounters + num, source);
        }
    }

    /**
     * <p>
     * Setter for the field <code>poisonCounters</code>.
     * </p>
     * 
     * @param num
     *            a int.
     * @param source
     */
    public final void setPoisonCounters(final int num, Card source) {
        int oldPoison = poisonCounters;
        this.poisonCounters = num;
        game.fireEvent(new GameEventPlayerPoisoned(this, source, oldPoison, num));
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

    public final void addChangedKeywords(final String[] addKeywords, final String[] removeKeywords, final Long timestamp) {
        this.addChangedKeywords(ImmutableList.copyOf(addKeywords), ImmutableList.copyOf(removeKeywords), timestamp);
    }

    public final void addChangedKeywords(final List<String> addKeywords, final List<String> removeKeywords, final Long timestamp) {
        // if the key already exists - merge entries
        if (changedKeywords.containsKey(timestamp)) {
            final List<String> kws = addKeywords == null ? Lists.<String>newArrayList() : Lists.newArrayList(addKeywords);
            final List<String> rkws = removeKeywords == null ? Lists.<String>newArrayList() : Lists.newArrayList(removeKeywords);
            final KeywordsChange cks = changedKeywords.get(timestamp);
            kws.addAll(cks.getKeywords());
            rkws.addAll(cks.getRemoveKeywords());
            this.changedKeywords.put(timestamp, new KeywordsChange(kws, rkws, cks.isRemoveAllKeywords()));
            return;
        }

        this.changedKeywords.put(timestamp, new KeywordsChange(addKeywords, removeKeywords, false));
    }

    public final KeywordsChange removeChangedKeywords(final Long timestamp) {
        return changedKeywords.remove(Long.valueOf(timestamp));
    }

    /**
     * Append a keyword change which adds the specified keyword.
     * @param keyword the keyword to add.
     */
    public final void addKeyword(final String keyword) {
        this.addChangedKeywords(ImmutableList.of(keyword), ImmutableList.<String>of(), this.getGame().getNextTimestamp());
    }

    /**
     * Replace all instances of added keywords.
     * @param oldKeyword the keyword to replace.
     * @param newKeyword the keyword with which to replace.
     */
    private final void replaceAllKeywordInstances(final String oldKeyword, final String newKeyword) {
        for (final KeywordsChange ck : this.changedKeywords.values()) {
            if (ck.getKeywords().contains(oldKeyword)) {
                ck.getKeywords().remove(oldKeyword);
                ck.getKeywords().add(newKeyword);
            }
        }
    }

    /**
     * Remove all keyword changes which grant this {@link Player} the specified
     * keyword. 
     * @param keyword the keyword to remove.
     */
    public final void removeKeyword(final String keyword) {
        for (final KeywordsChange ck : this.changedKeywords.values()) {
            if (ck.getKeywords().contains(keyword)) {
                ck.getKeywords().remove(keyword);
            }
        }

        // Remove the empty changes
        for (final Entry<Long, KeywordsChange> ck : ImmutableList.copyOf(this.changedKeywords.entrySet())) {
            if (ck.getValue().isEmpty()) {
                this.changedKeywords.remove(ck.getKey());
            }
        }
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
        return this.getKeywords().contains(keyword);
    }

    /**
     * @return this player's keywords.
     */
    public final List<String> getKeywords() {
        final ArrayList<String> keywords = Lists.newArrayList();

        // see if keyword changes are in effect
        for (final KeywordsChange ck : this.changedKeywords.values()) {
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
     * Can target.
     * 
     * @param sa
     *            the sa
     * @return a boolean
     */
    @Override
    public final boolean canBeTargetedBy(final SpellAbility sa) {
        if (this.hasKeyword("Shroud") || (!this.equals(sa.getActivatingPlayer()) && this.hasKeyword("Hexproof"))
                || this.hasProtectionFrom(sa.getHostCard())) {
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
            final List<String> list = this.getKeywords();

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
        if (this.hasKeyword("You can't draw more than one card each turn.")) {
            return this.numDrawnThisTurn < 1;
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

        for (int i = 0; i < n; i++) {
            if (!this.canDraw()) {
                return drawn;
            }
            drawn.addAll(this.doDraw());
        }
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

        if (!library.isEmpty()) {

            Card c = library.get(0);
            c = game.getAction().moveToHand(c);
            drawn.add(c);

            if (this.numDrawnThisTurn == 0) {
                boolean reveal = false;
                final List<Card> cards = this.getCardsIn(ZoneType.Battlefield);
                for (final Card card : cards) {
                    if (card.hasKeyword("Reveal the first card you draw each turn")
                            || (card.hasKeyword("Reveal the first card you draw on each of your turns") && game.getPhaseHandler().isPlayerTurn(this))) {
                        reveal = true;
                        break;
                    }
                }
                if (reveal) {
                    game.getAction().reveal(drawn, this, true, "Revealing the first card drawn from ");
                }
            }

            this.setLastDrawnCard(c);
            c.setDrawnThisTurn(true);
            this.numDrawnThisTurn++;
            if (game.getPhaseHandler().is(PhaseType.DRAW)) {
                this.numDrawnThisDrawStep++;
            }

            // Miracle draws
            if (this.numDrawnThisTurn == 1
                    && game.getAge() != GameStage.Mulligan) {
                drawMiracle(c);
            }

            // Run triggers
            final HashMap<String, Object> runParams = new HashMap<String, Object>();
            runParams.put("Card", c);
            runParams.put("Number", this.numDrawnThisTurn);
            runParams.put("Player", this);
            game.getTriggerHandler().runTrigger(TriggerType.Drawn, runParams, false);
        }
        else // Lose by milling is always on. Give AI many cards it cannot play if you want it not to undertake actions
            this.triedToDrawFromEmptyLibrary = true;

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
    public final List<Card> getCardsIn(final ZoneType zoneType, boolean filterOutPhasedOut) {
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
            result = zone == null ? null : zone.getCards(filterOutPhasedOut);
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

    public List<Card> getCardsActivableInExternalZones(boolean includeCommandZone) {
        final List<Card> cl = new ArrayList<Card>();

        cl.addAll(this.getZone(ZoneType.Graveyard).getCardsPlayerCanActivate(this));
        cl.addAll(this.getZone(ZoneType.Exile).getCardsPlayerCanActivate(this));
        cl.addAll(this.getZone(ZoneType.Library).getCardsPlayerCanActivate(this));
        if (includeCommandZone) {
            cl.addAll(this.getZone(ZoneType.Command).getCardsPlayerCanActivate(this));
        }

        //External activatables from all opponents
        for (final Player opponent : this.getOpponents()) {
            cl.addAll(opponent.getZone(ZoneType.Exile).getCardsPlayerCanActivate(this));
            cl.addAll(opponent.getZone(ZoneType.Graveyard).getCardsPlayerCanActivate(this));
            cl.addAll(opponent.getZone(ZoneType.Library).getCardsPlayerCanActivate(this));
            if (opponent.hasKeyword("Play with your hand revealed.")) {
                cl.addAll(opponent.getZone(ZoneType.Hand).getCardsPlayerCanActivate(this));
            }
        }
        cl.addAll(this.getGame().getCardsPlayerCanActivateInStack());
        return cl;
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
    }

    /**
     * <p>
     * getDredgeNumber.
     * </p>
     * 
     * @param c
     *            a {@link forge.game.card.Card} object.
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
    }

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
     *            a {@link forge.game.card.Card} object.
     * @param sa
     *            a {@link forge.game.spellability.SpellAbility} object.
     * @return the discarded {@link Card} in its new location.
     */
    public final Card discard(final Card c, final SpellAbility sa) {
        // TODO: This line should be moved inside CostPayment somehow
        /*if (sa != null) {
            sa.addCostToHashList(c, "Discarded");
        }*/
        final Card source = sa != null ? sa.getHostCard() : null;

        // Replacement effects
        final HashMap<String, Object> repRunParams = new HashMap<String, Object>();
        repRunParams.put("Event", "Discard");
        repRunParams.put("Card", c);
        repRunParams.put("Source", source);
        repRunParams.put("Affected", this);

        if (game.getReplacementHandler().run(repRunParams) != ReplacementResult.NotReplaced) {
            return null;
        }

        boolean discardToTopOfLibrary = null != sa && sa.hasParam("DiscardToTopOfLibrary");
        boolean discardMadness = sa != null && sa.hasParam("Madness");

        final Card newCard;
        if (discardToTopOfLibrary) {
            newCard = game.getAction().moveToLibrary(c, 0);
            // Play the Discard sound
        } else if (discardMadness) {
            newCard = game.getAction().exile(c);
        } else {
            newCard = game.getAction().moveToGraveyard(c);
            // Play the Discard sound
        }
        this.numDiscardedThisTurn++;
        // Run triggers
        Card cause = null;
        if (sa != null) {
            cause = sa.getHostCard();
        }
        final HashMap<String, Object> runParams = new HashMap<String, Object>();
        runParams.put("Player", this);
        runParams.put("Card", c);
        runParams.put("Cause", cause);
        runParams.put("IsMadness", (Boolean) discardMadness);
        game.getTriggerHandler().runTrigger(TriggerType.Discarded, runParams, false);

        return newCard;
    }

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
	 * @return the numCardsInHandStartedThisTurnWith
	 */
	public int getNumCardsInHandStartedThisTurnWith() {
		return numCardsInHandStartedThisTurnWith;
	}

	/**
	 * @param numCardsInHandStartedThisTurnWith the numCardsInHandStartedThisTurnWith to set
	 */
	public void setNumCardsInHandStartedThisTurnWith(int num) {
		this.numCardsInHandStartedThisTurnWith = num;
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
    public final void shuffle(final SpellAbility sa) {
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

        this.getZone(ZoneType.Library).setCards(getController().cheatShuffle(list));

        // Run triggers
        final HashMap<String, Object> runParams = new HashMap<String, Object>();
        runParams.put("Player", this);
        runParams.put("Source", sa);
        game.getTriggerHandler().runTrigger(TriggerType.Shuffled, runParams, false);

        // Play the shuffle sound
        game.fireEvent(new GameEventShuffle(this));
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
     *            a {@link forge.game.card.Card} object.
     */
    public final boolean playLand(final Card land, final boolean ignoreZoneAndTiming) {
        // Dakkon Blackblade Avatar will use a similar effect
        if (this.canPlayLand(land, ignoreZoneAndTiming)) {
            land.setController(this, 0);
            game.getAction().moveTo(this.getZone(ZoneType.Battlefield), land);

            // play a sound
            game.fireEvent(new GameEventLandPlayed(this, land));

            // Run triggers
            final HashMap<String, Object> runParams = new HashMap<String, Object>();
            runParams.put("Card", land);
            game.getTriggerHandler().runTrigger(TriggerType.LandPlayed, runParams, false);
            game.getStack().unfreezeStack();
            this.numLandsPlayed++;
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
    public final boolean canPlayLand(Card land, final boolean ignoreZoneAndTiming) {
        if (!ignoreZoneAndTiming && !this.canCastSorcery()) {
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

        if (land != null && !ignoreZoneAndTiming) {
            if (land.getOwner() != this && !land.hasKeyword("May be played by your opponent"))
                return false;
            
            if (land.getOwner() == this && land.hasKeyword("May be played by your opponent") && !land.hasKeyword("May be played"))
                return false;

            final Zone zone = game.getZoneOf(land);
            if (zone != null && (zone.is(ZoneType.Battlefield) || (!zone.is(ZoneType.Hand) && !land.hasStartOfKeyword("May be played")))) {
                return false;
            }
        }

        // **** Check for land play limit per turn ****
        // Dev Mode
        if (this.canCheatPlayUnlimitedLands || this.hasKeyword("You may play any number of additional lands on each of your turns.")) {
            return true;
        }

        // check for adjusted max lands play per turn
        int adjMax = 1;
        for (String keyword : this.getKeywords()) {
            if (keyword.startsWith("AdjustLandPlays")) {
                final String[] k = keyword.split(":");
                adjMax += Integer.valueOf(k[1]);
            }
        }
        if (this.numLandsPlayed < adjMax) {
            return true;
        }

        return false;
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
     * @return a {@link forge.game.card.Card} object.
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
     *            a {@link forge.game.card.Card} object.
     * @return a {@link forge.game.card.Card} object.
     */
    private final Card setLastDrawnCard(final Card c) {
        this.lastDrawnCard = c;
        return this.lastDrawnCard;
    }

    /**
     * <p>
     * Getter for the field <code>namedCard</code>.
     * </p>
     * 
     * @return a String.
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
     *            a {@link forge.game.card.Card} object.
     * @return
     * @return a {@link forge.game.card.Card} object.
     */
    public final void setNamedCard(final String s) {
        this.namedCard = s;
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
     * getActivateLoyaltyAbilityThisTurn.
     * </p>
     * 
     * @return a boolean.
     */
    public final boolean getActivateLoyaltyAbilityThisTurn() {
        return this.activateLoyaltyAbilityThisTurn;
    }

    /**
     * <p>
     * Setter for the field <code>activateLoyaltyAbilityThisTurn</code>.
     * </p>
     * 
     * @param b
     *            a boolean.
     */
    public final void setActivateLoyaltyAbilityThisTurn(final boolean b) {
        this.activateLoyaltyAbilityThisTurn = b;
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

        return (this.hasKeyword("You can't lose the game.") || Iterables.any(this.getOpponents(), Predicates.CANT_WIN));
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
        // Just in case player already lost
        if (this.getOutcome() != null) {
            return this.getOutcome().lossState != null;
        }

        // Rule 704.5a -  If a player has 0 or less life, he or she loses the game.
        final boolean hasNoLife = this.getLife() <= 0;
        if (hasNoLife && !this.cantLoseForZeroOrLessLife()) {
            return this.loseConditionMet(GameLossReason.LifeReachedZero, null);
        }

        // Rule 704.5b - If a player attempted to draw a card from a library with no cards in it
        //               since the last time state-based actions were checked, he or she loses the game.
        if (triedToDrawFromEmptyLibrary) {
            triedToDrawFromEmptyLibrary = false; // one-shot check
            return this.loseConditionMet(GameLossReason.Milled, null);
        }

        // Rule 704.5c - If a player has ten or more poison counters, he or she loses the game.
        if (this.poisonCounters >= 10) {
            return this.loseConditionMet(GameLossReason.Poisoned, null);
        }

        if (game.getRules().hasAppliedVariant(GameType.Commander)) {
            Map<Card,Integer> cmdDmg = getCommanderDamage();
            for (Card c : cmdDmg.keySet()) {
                if (cmdDmg.get(c) >= 21) {
                    return this.loseConditionMet(GameLossReason.CommanderDamage, null);
                }
            }
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

    public final void setLibrarySearched(final int l) {
        this.numLibrarySearchedOwn = l;
    }

    public final int getLibrarySearched() {
        return this.numLibrarySearchedOwn;
    }
    public final void incLibrarySearched() {
        this.numLibrarySearchedOwn++;
    }

    public final void setNumManaConversion(final int l) {
        this.numManaConversion = l;
    }

    public final boolean hasManaConversion() {
        return this.numManaConversion < this.getAmountOfKeyword("You may spend mana as though"
                + " it were mana of any color to cast a spell this turn.");
    }

    public final void incNumManaConversion() {
        this.numManaConversion++;
    }

    public final void decNumManaConversion() {
        this.numManaConversion--;
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
        } else if (property.equals("Allies")) {
            if (this.equals(sourceController) || this.isOpponentOf(sourceController)) {
                return false;
            }
        } else if (property.equals("NonActive")) {
            if (this.equals(game.getPhaseHandler().getPlayerTurn())) {
                return false;
            }
        } else if (property.equals("OpponentToActive")) {
            final Player active = game.getPhaseHandler().getPlayerTurn();
            if (this.equals(active) || !this.isOpponentOf(active)) {
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
            if (game.getCombat() == null || !this.equals(game.getCombat().getDefenderPlayerByAttacker(source))) {
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
        } else if (property.startsWith("NoCardsInHandAtBeginningOfTurn")) {
            if (this.numCardsInHandStartedThisTurnWith > 0) {
                return false;
            }
        } else if (property.startsWith("CardsInHandAtBeginningOfTurn")) {
            if (this.numCardsInHandStartedThisTurnWith <= 0) {
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
        } else if (property.startsWith("LifeEquals_")) {
            int life = AbilityUtils.calculateAmount(source, property.substring(11), null);
            if (this.getLife() != life) {
                return false;
            }
        } else if (property.equals("IsPoisoned")) {
        	if (this.getPoisonCounters() <= 0) {
        		return false;
        	}
        } else if (property.startsWith("withMore")) {
            final String cardType = property.split("sThan")[0].substring(8);
            final Player controller = "Active".equals(property.split("sThan")[1]) ? game.getPhaseHandler().getPlayerTurn() : sourceController;
            final List<Card> oppList = CardLists.filter(this.getCardsIn(ZoneType.Battlefield), CardPredicates.isType(cardType));
            final List<Card> yourList = CardLists.filter(controller.getCardsIn(ZoneType.Battlefield), CardPredicates.isType(cardType));
            if (oppList.size() <= yourList.size()) {
                return false;
            }
        } else if (property.startsWith("withAtLeast")) {
            final String cardType = property.split("More")[1].split("sThan")[0];
            final int amount = Integer.parseInt(property.substring(11, 12));
            final Player controller = "Active".equals(property.split("sThan")[1]) ? game.getPhaseHandler().getPlayerTurn() : sourceController;
            final List<Card> oppList = CardLists.filter(this.getCardsIn(ZoneType.Battlefield), CardPredicates.isType(cardType));
            final List<Card> yourList = CardLists.filter(controller.getCardsIn(ZoneType.Battlefield), CardPredicates.isType(cardType));
            System.out.println(yourList.size());
            if (oppList.size() < yourList.size() + amount) {
                return false;
            }
        } else if (property.startsWith("hasMore")) {
            final Player controller = property.contains("Than") && "Active".equals(property.split("Than")[1]) ? game.getPhaseHandler().getPlayerTurn() : sourceController;
            if (property.substring(7).startsWith("Life") && this.getLife() <= controller.getLife()) {
                return false;
            } else if (property.substring(7).startsWith("CardsInHand")
                    && this.getCardsIn(ZoneType.Hand).size() <= controller.getCardsIn(ZoneType.Hand).size()) {
                return false;
            }
        } else if (property.startsWith("hasFewer")) {
            final Player controller = "Active".equals(property.split("Than")[1]) ? game.getPhaseHandler().getPlayerTurn() : sourceController;
            if (property.substring(8).startsWith("CreaturesInYard")) {
                final List<Card> oppList = CardLists.filter(this.getCardsIn(ZoneType.Graveyard), Presets.CREATURES);
                final List<Card> yourList = CardLists.filter(controller.getCardsIn(ZoneType.Graveyard), Presets.CREATURES);
                if (oppList.size() >= yourList.size()) {
                    return false;
                }
            }
        } else if (property.startsWith("withMost")) {
            if (property.substring(8).equals("Life")) {
                int highestLife = this.getLife(); // Negative base just in case a few Lich's are running around
                for (final Player p : game.getPlayers()) {
                    if (p.getLife() > highestLife) {
                        highestLife = p.getLife();
                    }
                }
                if (this.getLife() != highestLife) {
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
                int lowestLife = this.getLife();
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
     * @return the number of spells cast by this player this turn.
     */
    public final int getSpellsCastThisTurn() {
        return this.spellsCastThisTurn;
    }

    /**
     * Adds 1 to the number of spells cast by this player this turn.
     */
    public final void addSpellCastThisTurn() {
        this.spellsCastThisTurn++;
    }

    /**
     * Resets the number of spells cast by this player this turn to 0.
     */
    public final void resetSpellsCastThisTurn() {
        this.spellsCastThisTurn = 0;
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
     * <p>
     * Getter for the field <code>lifeLostLastTurn</code>.
     * </p>
     * 
     * @return a int.
     */
    public final int getLifeLostLastTurn() {
        return this.lifeLostLastTurn;
    }

    /**
     * <p>
     * Setter for the field <code>lifeLostLastTurn</code>.
     * </p>
     * 
     * @param n
     *            a int.
     */
    public final void setLifeLostLastTurn(final int n) {
        this.lifeLostLastTurn = n;
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
        public static final Predicate<Player> CANT_WIN = new Predicate<Player>() {
        	@Override
        	public boolean apply(final Player p) {
        		return p.hasKeyword("You can't win the game.");
        	}
        };
    }

    public static class Accessors {
        public static Function<Player, String> FN_GET_NAME = new Function<Player, String>() {
            @Override
            public String apply(Player input) {
                return input.getName();
            }
        };
        public static Function<Player, Integer> FN_GET_LIFE_TOTAL = new Function<Player, Integer>() {
        	@Override
        	public Integer apply(Player input) {
        		return input.getLife();
        	}
        };
        public static Function<Player, Integer> FN_GET_POISON_COUNTERS = new Function<Player, Integer>() {
        	@Override
        	public Integer apply(Player input) {
        		return input.getPoisonCounters();
        	}
        };
        public static final Function<Player, Integer> FN_GET_ASSIGNED_DAMAGE = new Function<Player, Integer>() {
        	@Override
        	public Integer apply(Player input) {
        		return input.getAssignedDamage();
        	}
        };
    }

    /**
     * TODO: Write javadoc for this method.
     * @return
     */
    public final LobbyPlayer getLobbyPlayer() {
        return getController().getLobbyPlayer();
    }

    public final LobbyPlayer getOriginalLobbyPlayer() {
        return controllerCreator.getLobbyPlayer();
    }

    public final boolean isMindSlaved() {
        return mindSlaveMaster != null;
    }

    public final Player getMindSlaveMaster() {
        return mindSlaveMaster;
    }

    public final void setMindSlaveMaster(Player mindSlaveMaster0) {
        if (mindSlaveMaster == mindSlaveMaster0) {
            return;
        }
        mindSlaveMaster = mindSlaveMaster0;

        if (mindSlaveMaster != null) {
            LobbyPlayer oldLobbyPlayer = getLobbyPlayer();
            IGameEntitiesFactory master = (IGameEntitiesFactory)mindSlaveMaster.getLobbyPlayer();
            controller = master.createControllerFor(this);
            game.fireEvent(new GameEventPlayerControl(this, oldLobbyPlayer, getLobbyPlayer()));
        }
        else {
            controller = controllerCreator;
            game.fireEvent(new GameEventPlayerControl(this, getLobbyPlayer(), null));
        }
    }

    private void setOutcome(PlayerOutcome outcome) {
        stats.setOutcome(outcome);
    }

    public void onGameOver() {
        if (null == stats.getOutcome()) {
            setOutcome(PlayerOutcome.win());
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

    public int getTokenDoublersMagnitude() {
        int tokenDoublers = 0;
        for (String kw : this.getKeywords()) {
            if (kw.equals("TokenDoubler")) {
                tokenDoublers++;
            }
        }
        return 1 << tokenDoublers; // pow(a,0) = 1; pow(a,1) = a
    }

    public final int getAmountOfKeyword(final String k) {
        int count = 0;
        for (String kw : this.getKeywords()) {
            if (kw.equals(k)) {
                count++;
            }
        }
        return count;
    }

    public void onCleanupPhase() {
        for (Card c : getCardsIn(ZoneType.Hand)) {
            c.setDrawnThisTurn(false);
        }
        resetPreventNextDamage();
        resetPreventNextDamageWithEffect();
        resetNumDrawnThisTurn();
        resetNumDiscardedThisTurn();
        setNumCardsInHandStartedThisTurnWith(this.getCardsIn(ZoneType.Hand).size());
        setAttackedWithCreatureThisTurn(false);
        setActivateLoyaltyAbilityThisTurn(false);
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
        final Card source = sa.getRootAbility().getHostCard();
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

    public final PlayerController getController() {
        return controller;
    }

    public final void setFirstController(PlayerController ctrlr) {
        if (controllerCreator != null) {
            throw new IllegalStateException("Controller creator already assigned");
        }
        controllerCreator = ctrlr;
        controller = ctrlr;
    }

    /**
     * Run a procedure using a different controller
     * @param proc
     * @param tempController
     */
    public void runWithController(Runnable proc, PlayerController tempController) {
        PlayerController oldController = controller;
        controller = tempController;
        try {
            proc.run();
        } finally {
            controller = oldController;
        }
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
            replaceAllKeywordInstances("Skip all combat phases of your next turn.",
                    "Skip all combat phases of this turn.");
            return true;
        }
        if (hasKeyword("Skip all combat phases of this turn.")) {
            return true;
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
    public void planeswalk() {
        planeswalkTo(Arrays.asList(getZone(ZoneType.PlanarDeck).get(0)));
    }

    /**
     * Puts the planes in the argument and puts them face up in the command zone.
     * Then runs triggers.
     * 
     * @param destinations The planes to planeswalk to.
     */
    public void planeswalkTo(final List<Card> destinations) {
        System.out.println(this.getName() + ": planeswalk to " + destinations.toString());
        currentPlanes.addAll(destinations);

        for (Card c : currentPlanes) {
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
    public void leaveCurrentPlane() {
        if (!currentPlanes.isEmpty()) {
            //Run PlaneswalkedFrom triggers here.
            HashMap<String,Object> runParams = new HashMap<String,Object>();
            runParams.put("Card", new ArrayList<Card>(currentPlanes));
            game.getTriggerHandler().runTrigger(TriggerType.PlaneswalkedFrom, runParams,false);

            for (Card c : currentPlanes) {
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
    public void initPlane() {
        Card firstPlane = null;
        while (true) {
            firstPlane = getZone(ZoneType.PlanarDeck).get(0);
            getZone(ZoneType.PlanarDeck).remove(firstPlane);
            if (firstPlane.getType().contains("Phenomenon")) {
                getZone(ZoneType.PlanarDeck).add(firstPlane);
            }
            else {
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
     *            a {@link forge.game.card.Card} object.
     */
    public final void drawMiracle(final Card card) {
        // Whenever a card with miracle is the first card drawn in a turn,
        // you may cast it for it's miracle cost
        if (card.getMiracleCost() == null) {
            return;
        }

        final SpellAbility playForMiracleCost = card.getFirstSpellAbility().copy();
        playForMiracleCost.setPayCosts(card.getMiracleCost());
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

    public void addInboundToken(Card c) {
        inboundTokens.add(c);
    }

    public void removeInboundToken(Card c) {
        inboundTokens.remove(c);
    }

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
            miracle.setActivatingPlayer(getHostCard().getOwner());
            // pay miracle cost here.
            getHostCard().getOwner().getController().playMiracle(miracle, getHostCard());
        }
    }

    public void onMulliganned() {
        game.fireEvent(new GameEventMulligan(this)); // quest listener may interfere here
        final int newHand = getCardsIn(ZoneType.Hand).size();
        stats.notifyHasMulliganed();
        stats.notifyOpeningHandSize(newHand);
    }

    /**
     * @return the commander
     */
    public Card getCommander() {
        return commander;
    }

    /**
     * @param commander0 the commander to set
     */
    public void setCommander(Card commander0) {
        this.commander = commander0;
    }

    /**
     * @return the commanderDamage
     */
    public Map<Card,Integer> getCommanderDamage() {
        return commanderDamage;
    }

    /**
     * @param b isPlayingExtraTurn to set
     */
    public void setExtraTurn(boolean b) {
        this.isPlayingExtraTrun  = b;
    }

    /**
     * @return a boolean
     */
    public boolean isPlayingExtraTurn() {
        return isPlayingExtraTrun;
    }

    public void initVariantsZones(RegisteredPlayer registeredPlayer) {
        PlayerZone bf = getZone(ZoneType.Battlefield);
        Iterable<? extends IPaperCard> cards = registeredPlayer.getCardsOnBattlefield();
        if (cards != null) {
            for (final IPaperCard cp : cards) {
                Card c = Card.fromPaperCard(cp, this);
                bf.add(c);
                c.setSickness(true);
                c.setStartsGameInPlay(true);
            }
        }

        PlayerZone com = getZone(ZoneType.Command);
        // Mainly for avatar, but might find something else here
        for (final IPaperCard cp : registeredPlayer.getCardsInCommand()) {
            com.add(Card.fromPaperCard(cp, this));
        }
    
        // Schemes
        List<Card> sd = new ArrayList<Card>();
        for (IPaperCard cp : registeredPlayer.getSchemes()) {
            sd.add(Card.fromPaperCard(cp, this));
        }
        if (!sd.isEmpty()) {
            for (Card c : sd) {
                getZone(ZoneType.SchemeDeck).add(c);
            }
            getZone(ZoneType.SchemeDeck).shuffle();
        }

        // Planes
        List<Card> l = new ArrayList<Card>();
        for (IPaperCard cp : registeredPlayer.getPlanes()) {
            l.add(Card.fromPaperCard(cp, this));
        }
        if (!l.isEmpty()) {
            for (Card c : l) {
                getZone(ZoneType.PlanarDeck).add(c);
            }
            getZone(ZoneType.PlanarDeck).shuffle();
        }

        // Commander
        if (registeredPlayer.getCommander() != null) {
            Card cmd = Card.fromPaperCard(registeredPlayer.getCommander(), this);
            cmd.setCommander(true);
            com.add(cmd);
            setCommander(cmd);
            
            final Card eff = new Card(getGame().nextCardId());
            eff.setName("Commander effect");
            eff.addType("Effect");
            eff.setToken(true);
            eff.setOwner(this);
            eff.setColor(cmd.getColor());
            eff.setImmutable(true);
    
            eff.setSVar("CommanderMoveReplacement", "DB$ ChangeZone | Origin$ Battlefield,Graveyard,Exile,Library | Destination$ Command | Defined$ ReplacedCard");
            eff.setSVar("DBCommanderIncCast","DB$ StoreSVar | SVar$ CommanderCostRaise | Type$ CountSVar | Expression$ CommanderCostRaise/Plus.2");
            eff.setSVar("CommanderCostRaise","Number$0");
            
            Trigger t = TriggerHandler.parseTrigger("Mode$ SpellCast | Static$ True | ValidCard$ Card.YouOwn+IsCommander+wasCastFromCommand | Execute$ DBCommanderIncCast", eff, true);
            eff.addTrigger(t);
            ReplacementEffect r = ReplacementHandler.parseReplacement("Event$ Moved | Destination$ Graveyard,Exile | ValidCard$ Card.IsCommander+YouOwn | Secondary$ True | Optional$ True | OptionalDecider$ You | ReplaceWith$ CommanderMoveReplacement | Description$ If a commander would be put into its owner's graveyard or exile from anywhere, that player may put it into the command zone instead.", eff, true);
            eff.addReplacementEffect(r);
            eff.addStaticAbility("Mode$ Continuous | EffectZone$ Command | AddKeyword$ May be played | Affected$ Card.YouOwn+IsCommander | AffectedZone$ Command");
            eff.addStaticAbility("Mode$ RaiseCost | EffectZone$ Command | Amount$ CommanderCostRaise | Type$ Spell | ValidCard$ Card.YouOwn+IsCommander+wasCastFromCommand | EffectZone$ All | AffectedZone$ Command,Stack");
            
            getZone(ZoneType.Command).add(eff);
        }

        for (IPaperCard cp : registeredPlayer.getConspiracies()) {
            Card conspire = Card.fromPaperCard(cp, this);
            if (conspire.hasKeyword("Hidden agenda")) {
                if (!CardFactoryUtil.handleHiddenAgenda(this, conspire)) {
                    continue;
                }
            }
            com.add(conspire);
        }
    }

    public void changeOwnership(Card card) {
        // If lost then gained, just clear out of lost.
        // If gained then lost, just clear out of gained.
        Player oldOwner = card.getOwner();

        if (this.equals(oldOwner)) {
            return;
        }
        card.setOwner(this);

        if (this.lostOwnership.contains(card)) {
            this.lostOwnership.remove(card);
        } else {
            this.gainedOwnership.add(card);
        }

        if (oldOwner.gainedOwnership.contains(card)) {
            oldOwner.gainedOwnership.remove(card);
        } else {
            oldOwner.lostOwnership.add(card);
        }
    }

    public List<Card> getLostOwnership() {
        return lostOwnership;
    }

    public List<Card> getGainedOwnership() {
        return gainedOwnership;
    }

}
