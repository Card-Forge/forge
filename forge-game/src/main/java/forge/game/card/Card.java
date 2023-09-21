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
package forge.game.card;

import com.esotericsoftware.minlog.Log;
import com.google.common.base.Predicates;
import com.google.common.collect.*;

import forge.GameCommand;
import forge.StaticData;
import forge.card.*;
import forge.card.CardDb.CardArtPreference;
import forge.card.mana.ManaCost;
import forge.card.mana.ManaCostParser;
import forge.game.CardTraitBase;
import forge.game.Direction;
import forge.game.EvenOdd;
import forge.game.Game;
import forge.game.GameActionUtil;
import forge.game.GameEntity;
import forge.game.GameEntityCounterTable;
import forge.game.GameStage;
import forge.game.IHasSVars;
import forge.game.ability.AbilityFactory;
import forge.game.ability.AbilityKey;
import forge.game.ability.AbilityUtils;
import forge.game.ability.ApiType;
import forge.game.combat.Combat;
import forge.game.combat.CombatLki;
import forge.game.cost.Cost;
import forge.game.event.*;
import forge.game.event.GameEventCardDamaged.DamageType;
import forge.game.keyword.*;
import forge.game.player.Player;
import forge.game.player.PlayerCollection;
import forge.game.replacement.ReplaceMoved;
import forge.game.replacement.ReplacementEffect;
import forge.game.replacement.ReplacementHandler;
import forge.game.replacement.ReplacementResult;
import forge.game.replacement.ReplacementType;
import forge.game.spellability.*;
import forge.game.staticability.*;
import forge.game.trigger.Trigger;
import forge.game.trigger.TriggerHandler;
import forge.game.trigger.TriggerType;
import forge.game.zone.Zone;
import forge.game.zone.ZoneType;
import forge.item.IPaperCard;
import forge.item.PaperCard;
import forge.trackable.TrackableProperty;
import forge.trackable.Tracker;
import forge.util.*;
import forge.util.collect.FCollection;
import forge.util.collect.FCollectionView;
import io.sentry.Breadcrumb;
import io.sentry.Sentry;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;

import java.util.*;
import java.util.Map.Entry;

/**
 * <p>
 * Card class.
 * </p>
 *
 * Can now be used as keys in Tree data structures. The comparison is based
 * entirely on id.
 *
 * @author Forge
 * @version $Id$
 */
public class Card extends GameEntity implements Comparable<Card>, IHasSVars {
    private final Game game;
    private final IPaperCard paperCard;

    private final Map<CardStateName, CardState> states = Maps.newEnumMap(CardStateName.class);
    private CardState currentState;
    private CardStateName currentStateName = CardStateName.Original;

    private Zone castFrom;
    private SpellAbility castSA;

    private CardDamageHistory damageHistory = new CardDamageHistory();
    // Hidden keywords won't be displayed on the card
    // x=timestamp y=StaticAbility id
    private final Table<Long, Long, List<String>> hiddenExtrinsicKeywords = TreeBasedTable.create();

    // cards attached or otherwise linked to this card
    private CardCollection hauntedBy, devouredCards, exploitedCards, delvedCards, convokedCards, imprintedCards,
            exiledCards, encodedCards;
    private CardCollection gainControlTargets, chosenCards;
    private CardCollection mergedCards;
    private Map<Long, CardCollection> mustBlockCards = Maps.newHashMap();
    private List<Card> blockedThisTurn = Lists.newArrayList();
    private List<Card> blockedByThisTurn = Lists.newArrayList();

    private CardCollection untilLeavesBattlefield = new CardCollection();

    // if this card is attached or linked to something, what card is it currently attached to
    private Card encoding, cloneOrigin, haunting, effectSource, pairedWith, meldedWith;
    private Card mergedTo;

    private SpellAbility effectSourceAbility;
    private SpellAbility tokenSpawningAbility;

    private GameEntity entityAttachedTo;

    private final Map<StaticAbility, CardPlayOption> mayPlay = Maps.newHashMap();

    // changes by AF animate and continuous static effects

    protected CardChangedType changedTypeByText; // Layer 3 by Text Change
    // x=timestamp y=StaticAbility id
    private final Table<Long, Long, CardChangedType> changedCardTypesByText = TreeBasedTable.create(); // Layer 3
    private final Table<Long, Long, CardChangedType> changedCardTypesCharacterDefining = TreeBasedTable.create(); // Layer 4 CDA
    private final Table<Long, Long, CardChangedType> changedCardTypes = TreeBasedTable.create(); // Layer 4

    private final Table<Long, Long, CardChangedName> changedCardNames = TreeBasedTable.create(); // Layer 3
    private final Table<Long, Long, KeywordsChange> changedCardKeywordsByText = TreeBasedTable.create(); // Layer 3 by Text Change
    protected KeywordsChange changedCardKeywordsByWord = new KeywordsChange(ImmutableList.<KeywordInterface>of(), ImmutableList.<KeywordInterface>of(), false); // Layer 3 by Word Change
    private final Table<Long, Long, KeywordsChange> changedCardKeywords = TreeBasedTable.create(); // Layer 6

    // stores the keywords created by static abilities
    private final Map<Triple<String, Long, Long>, KeywordInterface> storedKeywords = Maps.newHashMap();

    // x=timestamp y=StaticAbility id
    private final Table<Long, Long, CardTraitChanges> changedCardTraitsByText = TreeBasedTable.create(); // Layer 3 by Text Change
    private final Table<Long, Long, CardTraitChanges> changedCardTraits = TreeBasedTable.create(); // Layer 6

    // stores the card traits created by static abilities
    private final Table<StaticAbility, String, SpellAbility> storedSpellAbilility = TreeBasedTable.create();
    private final Table<StaticAbility, String, Trigger> storedTrigger = TreeBasedTable.create();
    private final Table<StaticAbility, String, ReplacementEffect> storedReplacementEffect = TreeBasedTable.create();
    private final Table<StaticAbility, String, StaticAbility> storedStaticAbility = TreeBasedTable.create();

    private final Table<StaticAbility, SpellAbility, SpellAbility> storedSpellAbililityByText = HashBasedTable.create();
    private final Table<StaticAbility, String, SpellAbility> storedSpellAbililityGainedByText = TreeBasedTable.create();
    private final Table<StaticAbility, Trigger, Trigger> storedTriggerByText = HashBasedTable.create();
    private final Table<StaticAbility, ReplacementEffect, ReplacementEffect> storedReplacementEffectByText = HashBasedTable.create();
    private final Table<StaticAbility, StaticAbility, StaticAbility> storedStaticAbilityByText = HashBasedTable.create();

    private final Map<Triple<String, Long, Long>, KeywordInterface> storedKeywordByText = Maps.newHashMap();

    // x=timestamp y=StaticAbility id
    private final Table<Long, Long, CardColor> changedCardColorsByText = TreeBasedTable.create(); // Layer 3 by Text Change
    private final Table<Long, Long, CardColor> changedCardColorsCharacterDefining = TreeBasedTable.create(); // Layer 5 CDA
    private final Table<Long, Long, CardColor> changedCardColors = TreeBasedTable.create(); // Layer 5

    protected final Table<Long, Long, ManaCost> changedCardManaCost = TreeBasedTable.create(); // Layer 3

    private final NavigableMap<Long, CardCloneStates> clonedStates = Maps.newTreeMap(); // Layer 1

    private final Table<Long, Long, Map<String, String>> changedSVars = TreeBasedTable.create();

    private final Map<Long, PlayerCollection> mayLook = Maps.newHashMap();
    private final PlayerCollection mayLookFaceDownExile = new PlayerCollection();
    private final PlayerCollection mayLookTemp = new PlayerCollection();

    // don't use Enum Set Values or it causes a slow down
    private final Multimap<Long, Keyword> cantHaveKeywords = MultimapBuilder.hashKeys().hashSetValues().build();

    private final Map<CounterType, Long> counterTypeTimestamps = Maps.newHashMap();

    private final Map<Long, Integer> canBlockAdditional = Maps.newTreeMap();
    private final Set<Long> canBlockAny = Sets.newHashSet();

    // changes that say "replace each instance of one [color,type] by another - timestamp is the key of maps
    private final CardChangedWords changedTextColors = new CardChangedWords();
    private final CardChangedWords changedTextTypes = new CardChangedWords();

    private final Set<Object> rememberedObjects = Sets.newLinkedHashSet();
    private Map<Player, String> flipResult;

    private final Map<Card, Integer> assignedDamageMap = Maps.newTreeMap();

    private boolean isCommander = false;
    private boolean canMoveToCommandZone = false;

    private boolean startsGameInPlay = false;
    private boolean drawnThisTurn = false;
    private boolean foughtThisTurn = false;
    private boolean becameTargetThisTurn = false;
    private boolean startedTheTurnUntapped = false;
    private boolean cameUnderControlSinceLastUpkeep = true; // for Echo
    private boolean tapped = false;
    private boolean sickness = true; // summoning sickness
    private boolean token = false;
    private boolean tokenCard = false;
    private Card copiedPermanent;
    private boolean copiedSpell = false;

    private boolean unearthed;
    private boolean ringbearer;
    private boolean monstrous;

    private boolean renowned;

    private boolean manifested;

    private boolean foretold;
    private boolean foretoldThisTurn;
    private boolean foretoldCostByEffect;

    private boolean specialized;

    private int timesCrewedThisTurn = 0;

    private int classLevel = 1;
    private long bestowTimestamp = -1;
    private long transformedTimestamp = 0;
    private long mutatedTimestamp = -1;
    private long prototypeTimestamp = -1;
    private int timesMutated = 0;
    private boolean tributed = false;
    private boolean discarded = false;

    private boolean flipped = false;
    private boolean facedown = false;
    // set for transform and meld, needed for clone effects
    private boolean backside = false;

    private Player phasedOut;
    private boolean directlyPhasedOut = true;
    private boolean wontPhaseInNormal = false;

    private boolean usedToPayCost = false;

    // for Vanguard / Manapool / Emblems etc.
    private boolean isImmutable = false;
    private boolean isEmblem = false;
    private boolean isBoon = false;

    private int exertThisTurn = 0;
    private PlayerCollection exertedByPlayer = new PlayerCollection();

    private long timestamp = -1; // permanents on the battlefield

    // stack of set power/toughness
    // x=timestamp y=StaticAbility id
    private Table<Long, Long, Pair<Integer,Integer>> newPTText = TreeBasedTable.create(); // Text Change Layer 3
    private Table<Long, Long, Pair<Integer,Integer>> newPTCharacterDefining = TreeBasedTable.create(); // Layer 7a
    private Table<Long, Long, Pair<Integer,Integer>> newPT = TreeBasedTable.create(); // Layer 7b
    private Table<Long, Long, Pair<Integer,Integer>> boostPT = TreeBasedTable.create(); // Layer 7c

    private String oracleText = "";

    private Map<Integer, Integer> damage = Maps.newHashMap();
    private boolean hasBeenDealtDeathtouchDamage;
    private boolean hasBeenDealtExcessDamageThisTurn;

    // regeneration
    private int shieldCount = 0;
    private int regeneratedThisTurn;

    private int turnInZone;
    // the player that under which control it enters
    private Player turnInController;

    private Map<String, Integer> xManaCostPaidByColor;

    private Player owner;
    private Player controller;
    private long controllerTimestamp;
    private NavigableMap<Long, Player> tempControllers = Maps.newTreeMap();

    private String originalText = "", text = "";
    private String chosenType = "";
    private String chosenType2 = "";
    private List<String> notedTypes = new ArrayList<>();
    private List<String> chosenColors;
    private String chosenName = "";
    private String chosenName2 = "";
    private Integer chosenNumber;
    private Player chosenPlayer;
    private Player protectingPlayer;
    private EvenOdd chosenEvenOdd = null;
    private Direction chosenDirection = null;
    private String chosenMode = "";
    private String currentRoom = null;
    private String sector = null;
    private String chosenSector = null;

    // points to the host that exiled this card, usually the one that has this object it its exiledCards field
    // however it could also be a different card which isn't an error but means the exiling SA was gained
    private Card exiledWith;
    private Player exiledBy;

    private Map<Long, Player> goad = Maps.newTreeMap();

    private final List<GameCommand> leavePlayCommandList = Lists.newArrayList();
    private final List<GameCommand> untapCommandList = Lists.newArrayList();
    private final List<GameCommand> changeControllerCommandList = Lists.newArrayList();
    private final List<GameCommand> unattachCommandList = Lists.newArrayList();
    private final List<GameCommand> faceupCommandList = Lists.newArrayList();
    private final List<GameCommand> facedownCommandList = Lists.newArrayList();
    private final List<Object[]> staticCommandList = Lists.newArrayList();

    // Zone-changing spells should store card's zone here
    private Zone currentZone;

    // LKI copies of cards are allowed to store the LKI about the zone the card was known to be in last.
    // For all cards except LKI copies this should always be null.
    private Zone savedLastKnownZone;
    // LKI copies of cards store CMC separately to avoid shenanigans with the game state visualization
    // breaking when the LKI object is changed to a different card state.
    private int lkiCMC = -1;

    private CardRules cardRules;
    private final CardView view;

    private Table<Player, CounterType, Integer> etbCounters = HashBasedTable.create();

    private SpellAbility[] basicLandAbilities = new SpellAbility[MagicColor.WUBRG.length];

    private int planeswalkerAbilityActivated;
    private boolean planeswalkerActivationLimitUsed;

    private final ActivationTable numberTurnActivations = new ActivationTable();
    private final ActivationTable numberGameActivations = new ActivationTable();
    private final ActivationTable numberAbilityResolved = new ActivationTable();

    private final Map<SpellAbility, List<String>> chosenModesTurn = Maps.newHashMap();
    private final Map<SpellAbility, List<String>> chosenModesGame = Maps.newHashMap();
    private final Map<SpellAbility, List<String>> chosenModesYourCombat = Maps.newHashMap();
    private final Map<SpellAbility, List<String>> chosenModesYourLastCombat = Maps.newHashMap();

    private final Table<SpellAbility, StaticAbility, List<String>> chosenModesTurnStatic = HashBasedTable.create();
    private final Table<SpellAbility, StaticAbility, List<String>> chosenModesGameStatic = HashBasedTable.create();
    private final Table<SpellAbility, StaticAbility, List<String>> chosenModesYourCombatStatic = HashBasedTable.create();
    private final Table<SpellAbility, StaticAbility, List<String>> chosenModesYourLastCombatStatic = HashBasedTable.create();

    private CombatLki combatLKI;

    private ReplacementEffect shieldCounterReplaceDamage = null;
    private ReplacementEffect shieldCounterReplaceDestroy = null;
    private ReplacementEffect stunCounterReplaceUntap = null;

    // Enumeration for CMC request types
    public enum SplitCMCMode {
        CurrentSideCMC,
        LeftSplitCMC,
        RightSplitCMC
    }

    /**
     * Instantiates a new card not associated to any paper card.
     * @param id0 the unique id of the new card.
     */
    public Card(final int id0, final Game game0) {
        this(id0, null, game0);
    }

    /**
     * Instantiates a new card with a given paper card.
     * @param id0 the unique id of the new card.
     * @param paperCard0 the {@link IPaperCard} of which the new card is a
     * representation, or {@code null} if this new {@link Card} doesn't represent any paper
     * card.
     * @see IPaperCard
     */
    public Card(final int id0, final IPaperCard paperCard0, final Game game0) {
        this(id0, paperCard0, game0, game0 == null ? null : game0.getTracker());
    }
    public Card(final int id0, final IPaperCard paperCard0, final Game game0, final Tracker tracker0) {
        super(id0);

        game = game0;
        paperCard = paperCard0;
        view = new CardView(id0, tracker0);
        currentState = new CardState(view.getCurrentState(), this);
        states.put(CardStateName.Original, currentState);
        view.updateChangedColorWords(this);
        view.updateChangedTypes(this);
        view.updateSickness(this);
        view.updateClassLevel(this);
    }

    public boolean changeToState(final CardStateName state) {
        if (hasState(state)) {
            return setState(state, true);
        }
        return false;
    }

    public long getPrototypeTimestamp() { return prototypeTimestamp; }

    public long getTransformedTimestamp() { return transformedTimestamp; }
    public void incrementTransformedTimestamp() { this.transformedTimestamp++; }
    public void undoIncrementTransformedTimestamp() { this.transformedTimestamp--; }

    public CardState getCurrentState() {
        return currentState;
    }

    public CardStateName getAlternateStateName() {
        if (hasAlternateState()) {
            if (isSplitCard()) {
                return currentStateName == CardStateName.RightSplit ? CardStateName.LeftSplit : CardStateName.RightSplit;
            } else if (getRules() != null) {
                CardStateName changedState = getRules().getSplitType().getChangedStateName();
                if (currentStateName != changedState) {
                    return changedState;
                }
            }
            return CardStateName.Original;
        }
        else if (isFaceDown()) {
            return CardStateName.Original;
        }
        return null;
    }

    public CardState getAlternateState() {
        if (hasAlternateState() || isFaceDown()) {
            return states.get(getAlternateStateName());
        }
        return null;
    }

    public CardState getState(final CardStateName state) {
        if (state == CardStateName.FaceDown) {
            return getFaceDownState();
        }
        CardCloneStates clStates = getLastClonedState();
        if (clStates == null) {
            return getOriginalState(state);
        }
        return clStates.get(state);
    }

    public boolean hasState(final CardStateName state) {
        if (state == CardStateName.FaceDown) {
            return true;
        }
        CardCloneStates clStates = getLastClonedState();
        if (clStates == null) {
            return states.containsKey(state);
        }
        return clStates.containsKey(state);
    }

    public CardState getOriginalState(final CardStateName state) {
        if (state == CardStateName.FaceDown) {
            return getFaceDownState();
        }
        return states.get(state);
    }

    public CardState getFaceDownState() {
        if (!states.containsKey(CardStateName.FaceDown)) {
            states.put(CardStateName.FaceDown, CardUtil.getFaceDownCharacteristic(this));
        }
        return states.get(CardStateName.FaceDown);
    }

    public void setOriginalStateAsFaceDown() {
        // For Ertai's Meddling a morph spell
        currentState = CardUtil.getFaceDownCharacteristic(this, CardStateName.Original);
        states.put(CardStateName.Original, currentState);
    }

    public boolean setState(final CardStateName state, boolean updateView) {
        return setState(state, updateView, false);
    }
    public boolean setState(final CardStateName state, boolean updateView, boolean forceUpdate) {
        boolean rollback = state == CardStateName.Original
                && (currentStateName == CardStateName.Flipped || currentStateName == CardStateName.Transformed);
        boolean transform = state == CardStateName.Flipped || state == CardStateName.Transformed || state == CardStateName.Meld;
        boolean needsTransformAnimation = transform || rollback;
        // faceDown has higher priority over clone states
        // while text change states doesn't apply while the card is faceDown
        if (state != CardStateName.FaceDown) {
            CardCloneStates cloneStates = getLastClonedState();
            if (cloneStates != null) {
                if (!cloneStates.containsKey(state)) {
                    throw new RuntimeException(getName() + " tried to switch to non-existant cloned state \"" + state + "\"!");
                    //return false; // Nonexistant state.
                }
            } else {
                if (!states.containsKey(state)) {
                    System.out.println(getName() + " tried to switch to non-existant state \"" + state + "\"!");
                    return false; // Nonexistant state.
                }
            }
        }

        if (state.equals(currentStateName) && !forceUpdate) {
            return false;
        }

        // Cleared tests, about to change states
        if (currentStateName.equals(CardStateName.FaceDown) && state.equals(CardStateName.Original)) {
            this.setManifested(false);
        }

        currentStateName = state;
        currentState = getState(state);

        if (updateView) {
            view.updateState(this);
            view.updateNeedsTransformAnimation(needsTransformAnimation);

            if (game != null) {
                // update Type, color and keywords again if they have changed
                if (!changedCardTypes.isEmpty()) {
                    updateTypesForView();
                }
                updateColorForView();

                if (!changedCardKeywords.isEmpty()) {
                    updateKeywords();
                }

                if (state == CardStateName.FaceDown) {
                    view.updateHiddenId(game.nextHiddenCardId());
                }
                game.fireEvent(new GameEventCardStatsChanged(this)); //ensure stats updated for new characteristics
            }
        }
        return true;
    }

    public Set<CardStateName> getStates() {
        return states.keySet();
    }

    public CardStateName getCurrentStateName() {
        return currentStateName;
    }

    // use by CopyPermanent
    public void setStates(Map<CardStateName, CardState> map) {
        states.clear();
        states.putAll(map);
    }

    public final void addAlternateState(final CardStateName state, final boolean updateView) {
        states.put(state, new CardState(this, state));
        if (updateView) {
            view.updateState(this);
        }
    }

    public void clearStates(final CardStateName state, boolean updateView) {
        if (states.remove(state) == null) {
            return;
        }
        if (state == currentStateName) {
            currentStateName = CardStateName.Original;
        }
        if (updateView) {
            view.updateState(this);
        }
    }

    public void updateStateForView() {
        view.updateState(this);
    }

    // The following methods are used to selectively update certain view components (text,
    // P/T, card types) in order to avoid card flickering due to aggressive full update
    public void updateAbilityTextForView() {
        updateKeywords(); // does call update Ability text
        //view.getCurrentState().updateAbilityText(this, getCurrentState());
    }

    public final void updatePowerToughnessForView() {
        view.updateCounters(this);
    }

    public final void updateTypesForView() {
        currentState.getView().updateType(currentState);
    }

    public boolean changeCardState(final String mode, final String customState, final SpellAbility cause) {
        if (isPhasedOut()) {
            return false;
        }
        if (mode == null)
            return changeToState(CardStateName.smartValueOf(customState));

        // flip and face-down don't overlap. That is there is no chance to turn face down a flipped permanent
        // and then any effect have it turn upface again and demand its former flip state to be restored
        // Proof: Morph cards never have ability that makes them flip, Ixidron does not suppose cards to be turned face up again,
        // Illusionary Mask affects cards in hand.
        if (mode.equals("Transform") && (isTransformable() || hasMergedCard())) {
            if (!canTransform(cause)) {
                return false;
            }

            // Need to remove mutated states, otherwise the changeToState() will fail
            if (hasMergedCard()) {
                removeMutatedStates();
            }
            CardCollectionView cards = hasMergedCard() ? getMergedCards() : new CardCollection(this);
            boolean retResult = false;
            for (final Card c : cards) {
                if (!c.isTransformable()) {
                    continue;
                }
                c.backside = !c.backside;

                boolean result = c.changeToState(c.backside ? CardStateName.Transformed : CardStateName.Original);
                retResult = retResult || result;
            }
            if (hasMergedCard()) {
                rebuildMutatedStates(cause);
            }

            // run valid Replacement here
            getGame().getReplacementHandler().run(ReplacementType.Transform, AbilityKey.mapFromAffected(this));

            // do the Transform trigger here, it can also happen if the resulting state doesn't change
            // Clear old dfc trigger from the trigger handler
            getGame().getTriggerHandler().clearActiveTriggers(this, null);
            getGame().getTriggerHandler().registerActiveTrigger(this, false);

            if (cause == null || !cause.hasParam("ETB")) {
                final Map<AbilityKey, Object> runParams = AbilityKey.mapFromCard(this);
                getGame().getTriggerHandler().runTrigger(TriggerType.Transformed, runParams, false);
            }
            incrementTransformedTimestamp();

            return retResult;
        } else if (mode.equals("Flip")) {
            // 709.4. Flipping a permanent is a one-way process.
            if (isFlipped()) {
                return false;
            }

            boolean retResult = false;
            if (isFlipCard() || hasMergedCard()) {
                // Need to remove mutated states, otherwise the changeToState() will fail
                if (hasMergedCard()) {
                    removeMutatedStates();
                }
                CardCollectionView cards = hasMergedCard() ? getMergedCards() : new CardCollection(this);
                for (final Card c : cards) {
                    c.flipped = true;
                    // a facedown card does flip but the state doesn't change
                    if (c.facedown) continue;

                    boolean result = c.changeToState(CardStateName.Flipped);
                    retResult = retResult || result;
                }
                if (hasMergedCard()) {
                    rebuildMutatedStates(cause);
                    game.getTriggerHandler().clearActiveTriggers(this, null);
                    game.getTriggerHandler().registerActiveTrigger(this, false);
                }
            } else {
                // a card without flip state can still flip rules vise
                retResult = true;
                this.flipped = true;
            }
            return retResult;
        } else if (mode.equals("TurnFace")) {
            CardStateName oldState = getCurrentStateName();
            if (oldState == CardStateName.Original || oldState == CardStateName.Flipped) {
                return turnFaceDown();
            } else if (isFaceDown()) {
                return turnFaceUp(cause);
            }
        } else if (mode.equals("Meld") && isMeldable()) {
            return changeToState(CardStateName.Meld);
        } else if (mode.equals("Specialize") && canSpecialize()) {
            if (customState.equalsIgnoreCase("white")) {
                return changeToState(CardStateName.SpecializeW);
            } else if (customState.equalsIgnoreCase("blue")) {
                return changeToState(CardStateName.SpecializeU);
            } else if (customState.equalsIgnoreCase("black")) {
                return changeToState(CardStateName.SpecializeB);
            } else if (customState.equalsIgnoreCase("red")) {
                return changeToState(CardStateName.SpecializeR);
            } else if (customState.equalsIgnoreCase("green")) {
                return changeToState(CardStateName.SpecializeG);
            }
        } else if (mode.equals("Unspecialize") && isSpecialized()) {
            return changeToState(CardStateName.Original);
        }
        return false;
    }

    public Card manifest(Player p, SpellAbility sa, Map<AbilityKey, Object> params) {
        // Turn Face Down (even if it's DFC).
        // Sometimes cards are manifested while already being face down
        if (!turnFaceDown(true) && !isFaceDown()) {
            return null;
        }

        // Just in case you aren't the controller, now you are!
        setController(p, game.getNextTimestamp());

        // Mark this card as "manifested"
        setManifested(true);

        // Move to p's battlefield
        Card c = game.getAction().moveToPlay(this, p, sa, params);
        if (c.isInPlay()) {
            c.setManifested(true);
            c.turnFaceDown(true);
            c.updateStateForView();
        }

        return c;
    }

    public boolean turnFaceDown() {
        return turnFaceDown(false);
    }

    public boolean turnFaceDown(boolean override) {
        CardCollectionView cards = hasMergedCard() ? getMergedCards() : new CardCollection(this);
        boolean retResult = false;
        for (final Card c : cards) {
            if (override || !c.isDoubleFaced()) {
                c.facedown = true;
                if (c.setState(CardStateName.FaceDown, true)) {
                    c.runFacedownCommands();
                    retResult = true;
                }
            }
        }
        if (retResult && hasMergedCard()) {
            removeMutatedStates();
            rebuildMutatedStates(null);
            game.getTriggerHandler().clearActiveTriggers(this, null);
            game.getTriggerHandler().registerActiveTrigger(this, false);
        }
        return retResult;
    }

    public boolean turnFaceDownNoUpdate() {
        facedown = true;
        return setState(CardStateName.FaceDown, false);
    }

    public boolean turnFaceUp(SpellAbility cause) {
        return turnFaceUp(false, true, cause);
    }

    public boolean turnFaceUp(boolean manifestPaid, boolean runTriggers, SpellAbility cause) {
        if (isFaceDown()) {
            if (manifestPaid && isManifested() && !getRules().getType().isCreature()) {
                // If we've manifested a non-creature and we're demanifesting disallow it

                // Unless this creature also has a Morph ability
                return false;
            }

            CardCollectionView cards = hasMergedCard() ? getMergedCards() : new CardCollection(this);
            boolean retResult = false;
            for (final Card c : cards) {
                boolean result;
                if (c.isFlipped() && c.isFlipCard()) {
                    result = c.setState(CardStateName.Flipped, true);
                } else {
                    result = c.setState(CardStateName.Original, true);
                }

                c.facedown = false;
                c.updateStateForView(); //fixes cards with backside viewable
                // need to run faceup commands, currently
                // it does cleanup the modified facedown state
                if (result) {
                    c.runFaceupCommands();
                }
                retResult = retResult || result;
            }
            if (retResult && hasMergedCard()) {
                removeMutatedStates();
                rebuildMutatedStates(cause);
                game.getTriggerHandler().clearActiveTriggers(this, null);
                game.getTriggerHandler().registerActiveTrigger(this, false);
            }
            if (retResult && runTriggers) {
                // Run replacement effects
                getGame().getReplacementHandler().run(ReplacementType.TurnFaceUp, AbilityKey.mapFromAffected(this));

                // Run triggers
                final Map<AbilityKey, Object> runParams = AbilityKey.mapFromCard(this);
                runParams.put(AbilityKey.Cause, cause);

                getGame().getTriggerHandler().registerActiveTrigger(this, false);
                getGame().getTriggerHandler().runTrigger(TriggerType.TurnFaceUp, runParams, false);
            }
            return retResult;
        }
        return false;
    }

    public boolean canTransform(SpellAbility cause) {
        if (isFaceDown()) {
            return false;
        }
        Card transformCard = this;
        if (hasMergedCard()) {
            boolean hasTransformCard = false;
            for (final Card c : getMergedCards()) {
                if (c.isTransformable()) {
                    hasTransformCard = true;
                    transformCard = c;
                    break;
                }
            }
            if (!hasTransformCard) {
                return false;
            }
        } else if (!isTransformable()) {
            return false;
        }

        // below only when in play
        if (!isInPlay()) {
            return true;
        }

        CardStateName destState = transformCard.backside ? CardStateName.Original : CardStateName.Transformed;

        // use Original State for the transform check
        if (!transformCard.getOriginalState(destState).getType().isPermanent()) {
            return false;
        }

        return !StaticAbilityCantTransform.cantTransform(this, cause);
    }

    public int getHiddenId() {
        return view.getHiddenId();
    }

    public void updateAttackingForView() {
        view.updateAttacking(this);
        getGame().updateCombatForView();
    }
    public void updateBlockingForView() {
        view.updateBlocking(this);
        getGame().updateCombatForView(); //ensure blocking arrow shown/hidden as needed
    }

    @Override
    public final String getName() {
        return getName(currentState, false);
    }

    public final String getName(boolean alt) {
        return getName(currentState, alt);
    }
    public final String getName(CardStateName stateName) {
        return getName(getState(stateName), false);
    }
    public final String getName(CardState state, boolean alt) {
        String name = state.getName();
        for (CardChangedName change : this.changedCardNames.values()) {
            if (change.isOverwrite()) {
                name = change.getNewName();
            }
        }
        return alt ? StaticData.instance().getCommonCards().getName(name, true) :  name;
    }

    public final boolean hasNonLegendaryCreatureNames() {
        boolean result = false;
        for (CardChangedName change : this.changedCardNames.values()) {
            if (change.isOverwrite()) {
                result = false;
            } else if (change.isAddNonLegendaryCreatureNames()) {
                result = true;
            }
        }
        return result;
    }

    @Override
    public final void setName(final String name0) {
        currentState.setName(name0);
    }

    public void addChangedName(final String name0, boolean addNonLegendaryCreatureNames, long timestamp, long staticId) {
        changedCardNames.put(timestamp, staticId, new CardChangedName(name0, addNonLegendaryCreatureNames));
        updateNameforView();
    }

    public void removeChangedName(long timestamp, long staticId) {
        if (changedCardNames.remove(timestamp, staticId) != null) {
            updateNameforView();
        }
    }

    public boolean clearChangedName() {
        boolean changed = !changedCardNames.isEmpty();
        changedCardNames.clear();
        return changed;
    }

    public void updateNameforView() {
        currentState.getView().updateName(currentState);
    }

    public Table<Long, Long, CardChangedName> getChangedCardNames() {
        return changedCardNames;
    }

    public void setChangedCardNames(Table<Long, Long, CardChangedName> changedCardNames) {
        this.changedCardNames.clear();
        this.changedCardNames.putAll(changedCardNames);
    }

    public final boolean isInAlternateState() {
        return currentStateName != CardStateName.Original;
    }

    public final boolean hasAlternateState() {
        // Note: Since FaceDown state is created lazily (whereas previously
        // it was always created), adjust threshold based on its existence.
        int threshold = states.containsKey(CardStateName.FaceDown) ? 2 : 1;

        int numStates = states.keySet().size();

        return numStates > threshold;
    }

    public final boolean isTransformable() {
        return getRules() != null && getRules().getSplitType() == CardSplitType.Transform;
    }

    public final boolean isMeldable() {
        return getRules() != null && getRules().getSplitType() == CardSplitType.Meld;
    }

    public final boolean isModal() {
        return getRules() != null && getRules().getSplitType() == CardSplitType.Modal;
    }

    public final boolean isDoubleFaced() {
        return isTransformable() || isMeldable() || isModal();
    }

    public final boolean isFlipCard() {
        return hasState(CardStateName.Flipped);
    }

    public final boolean isSplitCard() {
        return getRules() != null && getRules().getSplitType() == CardSplitType.Split;
    }

    public final boolean isAdventureCard() {
        return hasState(CardStateName.Adventure);
    }

    public final boolean isBackSide() {
        return backside;
    }
    public final void setBackSide(boolean value) {
        backside = value;
    }

    public boolean isCloned() {
        return !clonedStates.isEmpty() && clonedStates.lastEntry().getKey() != mutatedTimestamp
                && clonedStates.lastEntry().getKey() != prototypeTimestamp;
    }

    public final CardCollectionView getDevouredCards() {
        return CardCollection.getView(devouredCards);
    }
    public final void addDevoured(final Card c) {
        if (devouredCards == null) {
            devouredCards = new CardCollection();
        }
        devouredCards.add(c);
    }
    public final void clearDevoured() {
        devouredCards = null;
    }

    public final CardCollectionView getExploited() {
        return CardCollection.getView(exploitedCards);
    }
    public final void addExploited(final Card c) {
        if (exploitedCards == null) {
            exploitedCards = new CardCollection();
        }
        exploitedCards.add(c);
    }
    public final void clearExploited() {
        exploitedCards = null;
    }

    public final CardCollectionView getDelved() {
        return CardCollection.getView(delvedCards);
    }
    public final void addDelved(final Card c) {
        if (delvedCards == null) {
            delvedCards = new CardCollection();
        }
        delvedCards.add(c);
    }
    public final void clearDelved() {
        delvedCards = null;
    }

    public final CardCollectionView getConvoked() {
        return CardCollection.getView(convokedCards);
    }
    public final void addConvoked(final Card c) {
        if (convokedCards == null) {
            convokedCards = new CardCollection();
        }
        convokedCards.add(c);
    }
    public final void clearConvoked() {
        convokedCards = null;
    }

    public final Iterable<Object> getRemembered() {
        return rememberedObjects;
    }
    public final boolean hasRemembered() {
        return !rememberedObjects.isEmpty();
    }
    public final int getRememberedCount() {
        return rememberedObjects.size();
    }
    public final Object getFirstRemembered() {
        return Iterables.getFirst(rememberedObjects, null);
    }
    public final <T> boolean isRemembered(T o) {
        return rememberedObjects.contains(o);
    }
    public final <T> void addRemembered(final T o) {
        if (rememberedObjects.add(o)) {
            view.updateRemembered(this);
        }
    }
    public final <T> void addRemembered(final Iterable<T> objects) {
        boolean changed = false;
        for (T o : objects) {
            if (rememberedObjects.add(o)) {
                changed = true;
            }
        }
        if (changed) {
            view.updateRemembered(this);
        }
    }
    public final <T> void removeRemembered(final T o) {
        if (rememberedObjects.remove(o)) {
            view.updateRemembered(this);
        }
    }

    public final <T> void removeRemembered(final Iterable<T> list) {
        boolean changed = false;
        for (T o : list) {
            if (rememberedObjects.remove(o)) {
                changed = true;
            }
        }
        if (changed) {
            view.updateRemembered(this);
        }
    }
    public final void clearRemembered() {
        if (rememberedObjects.isEmpty()) { return; }
        rememberedObjects.clear();
        view.updateRemembered(this);
    }
    public final void updateRemembered() {
        view.updateRemembered(this);
    }

    public final CardCollectionView getImprintedCards() {
        return CardCollection.getView(imprintedCards);
    }
    public final boolean hasImprintedCard() {
        return FCollection.hasElements(imprintedCards);
    }
    public final boolean hasImprintedCard(Card c) {
        return FCollection.hasElement(imprintedCards, c);
    }
    public final void addImprintedCard(final Card c) {
        imprintedCards = view.addCard(imprintedCards, c, TrackableProperty.ImprintedCards);
    }
    public final void addImprintedCards(final Iterable<Card> cards) {
        imprintedCards = view.addCards(imprintedCards, cards, TrackableProperty.ImprintedCards);
    }
    public final void removeImprintedCard(final Card c) {
        imprintedCards = view.removeCard(imprintedCards, c, TrackableProperty.ImprintedCards);
    }
    public final void removeImprintedCards(final Iterable<Card> cards) {
        imprintedCards = view.removeCards(imprintedCards, cards, TrackableProperty.ImprintedCards);
    }
    public final void clearImprintedCards() {
        imprintedCards = view.clearCards(imprintedCards, TrackableProperty.ImprintedCards);
    }

    public final CardCollectionView getExiledCards() {
        return CardCollection.getView(exiledCards);
    }
    public final boolean hasExiledCard() {
        return FCollection.hasElements(exiledCards);
    }
    public final boolean hasExiledCard(Card c) {
        return FCollection.hasElement(exiledCards, c);
    }
    public final void addExiledCard(final Card c) {
        exiledCards = view.addCard(exiledCards, c, TrackableProperty.ExiledCards);
    }
    public final void addExiledCards(final Iterable<Card> cards) {
        exiledCards = view.addCards(exiledCards, cards, TrackableProperty.ExiledCards);
    }
    public final void removeExiledCard(final Card c) {
        exiledCards = view.removeCard(exiledCards, c, TrackableProperty.ExiledCards);
    }
    public final void removeExiledCards(final Iterable<Card> cards) {
        exiledCards = view.removeCards(exiledCards, cards, TrackableProperty.ExiledCards);
    }
    public final void clearExiledCards() {
        exiledCards = view.clearCards(exiledCards, TrackableProperty.ExiledCards);
    }

    public final CardCollectionView getEncodedCards() {
        return CardCollection.getView(encodedCards);
    }
    public final boolean hasEncodedCard() {
        return FCollection.hasElements(encodedCards);
    }
    public final boolean hasEncodedCard(Card c) {
        return FCollection.hasElement(encodedCards, c);
    }
    public final void addEncodedCard(final Card c) {
        encodedCards = view.addCard(encodedCards, c, TrackableProperty.EncodedCards);
    }
    public final void addEncodedCards(final Iterable<Card> cards) {
        encodedCards = view.addCards(encodedCards, cards, TrackableProperty.EncodedCards);
    }
    public final void removeEncodedCard(final Card c) {
        encodedCards = view.removeCard(encodedCards, c, TrackableProperty.EncodedCards);
    }
    public final void clearEncodedCards() {
        encodedCards = view.clearCards(encodedCards, TrackableProperty.EncodedCards);
    }

    public final Card getEncodingCard() {
        return encoding;
    }
    public final void setEncodingCard(final Card e) {
        encoding = e;
    }

    public final CardCollectionView getMergedCards() {
        return CardCollection.getView(mergedCards);
    }
    public final Card getTopMergedCard() {
        return mergedCards.get(0);
    }
    public final boolean hasMergedCard() {
        return FCollection.hasElements(mergedCards);
    }
    public final void addMergedCard(final Card c) {
        if (mergedCards == null) {
            mergedCards = new CardCollection();
        }
        mergedCards.add(c);
    }
    public final void addMergedCardToTop(final Card c) {
        mergedCards.add(0, c);
    }
    public final void removeMergedCard(final Card c) {
        mergedCards.remove(c);
    }
    public final void clearMergedCards() {
        mergedCards.clear();
    }

    public final Card getMergedToCard() {
        return mergedTo;
    }
    public final void setMergedToCard(final Card c) {
        mergedTo = c;
    }
    public final boolean isMerged() {
        return getMergedToCard() != null;
    }

    public final boolean isMutated() {
        return mutatedTimestamp != -1;
    }
    public final long getMutatedTimestamp() {
        return mutatedTimestamp;
    }
    public final void setMutatedTimestamp(final long t) {
        mutatedTimestamp = t;
    }

    public final int getTimesMutated() {
        return timesMutated;
    }
    public final void setTimesMutated(final int t) {
        timesMutated = t;
    }

    public final void removeMutatedStates() {
        if (isMutated()) {
            removeCloneState(getMutatedTimestamp());
        }
    }
    public final void rebuildMutatedStates(final CardTraitBase sa) {
        if (!isFaceDown()) {
            final CardCloneStates mutatedStates = CardFactory.getMutatedCloneStates(this, sa);
            addCloneState(mutatedStates, getMutatedTimestamp());
        }
    }

    public final void moveMergedToSubgame(SpellAbility cause) {
        if (hasMergedCard()) {
            Zone zone = getZone();
            int pos = -1;
            for (int i = 0; i < zone.size(); i++) {
                if (zone.get(i) == this) {
                    pos = i;
                    break;
                }
            }
            Card newTop = null;
            for (final Card c : mergedCards) {
                if (c != this) {
                    newTop = c;
                }
            }

            if (newTop != null) {
                removeMutatedStates();
                newTop.mergedCards = mergedCards;
                newTop.mergedTo = null;
                mergedCards = null;
                mergedTo = newTop;

                newTop.mutatedTimestamp = mutatedTimestamp;
                newTop.timesMutated = timesMutated;
                mutatedTimestamp = -1;
                timesMutated = 0;

                zone.remove(this);
                newTop.getZone().add(this);
                setZone(newTop.getZone());

                newTop.getZone().remove(newTop);
                zone.add(newTop, pos);
                newTop.setZone(zone);
            }
        }

        Card topCard = getMergedToCard();
        if (topCard != null) {
            setMergedToCard(null);
            topCard.removeMergedCard(this);
            topCard.removeMutatedStates();
            topCard.rebuildMutatedStates(cause);
        }
    }

    public final String getFlipResult(final Player flipper) {
        if (flipResult == null) {
            return null;
        }
        return flipResult.get(flipper);
    }
    public final void addFlipResult(final Player flipper, final String result) {
        if (flipResult == null) {
            flipResult = Maps.newTreeMap();
        }
        flipResult.put(flipper, result);
    }
    public final void clearFlipResult() {
        flipResult = null;
    }

    public final FCollectionView<Trigger> getTriggers() {
        return currentState.getTriggers();
    }
    public final Trigger addTrigger(final Trigger t) {
        currentState.addTrigger(t);
        return t;
    }

    public final boolean hasTrigger(final Trigger t) {
       return currentState.hasTrigger(t);
    }
    public final boolean hasTrigger(final int id) {
        return currentState.hasTrigger(id);
    }

    public void updateTriggers(List<Trigger> list, CardState state) {
        for (final CardTraitChanges ck : getChangedCardTraitsList(state)) {
            if (ck.isRemoveAll()) {
                list.clear();
            }
            list.addAll(ck.getTriggers());
        }

        // Keywords are already sorted by Layer
        for (KeywordInterface kw : getUnhiddenKeywords(state)) {
            list.addAll(kw.getTriggers());
        }
    }

    public final int getXManaCostPaid() {
        if (getCastSA() != null) {
            Integer paid = getCastSA().getXManaCostPaid();
            return paid == null ? 0 : paid;
        }
        return 0;
    }

    public final Map<String, Integer> getXManaCostPaidByColor() {
        return xManaCostPaidByColor;
    }
    public final void setXManaCostPaidByColor(final Map<String, Integer> xByColor) {
        xManaCostPaidByColor = xByColor;
    }

    public final int getXManaCostPaidCount(final String colors) {
        int count = 0;
        if (xManaCostPaidByColor != null) {
            for (Entry<String, Integer> m : xManaCostPaidByColor.entrySet()) {
                if (colors.contains(m.getKey())) {
                    count += m.getValue();
                }
            }
        }
        return count;
    }

    public List<Card> getBlockedThisTurn() {
        return blockedThisTurn;
    }
    public void addBlockedThisTurn(Card attacker) {
        blockedThisTurn.add(attacker);
    }
    public void clearBlockedThisTurn() {
        blockedThisTurn.clear();
    }

    public List<Card> getBlockedByThisTurn() {
        return blockedByThisTurn;
    }
    public void addBlockedByThisTurn(Card blocker) {
        blockedByThisTurn.add(blocker);
    }
    public void clearBlockedByThisTurn() {
        blockedByThisTurn.clear();
    }

    //MustBlockCards are cards that this Card must block if able in an upcoming combat.
    //This is cleared at the end of each turn.
    public final CardCollectionView getMustBlockCards() {
        return CardCollection.getView(Iterables.concat(mustBlockCards.values()));
    }
    public final void addMustBlockCard(long ts, final Card c) {
        mustBlockCards.put(ts, new CardCollection(c));
        view.updateMustBlockCards(this);
    }
    public final void addMustBlockCards(long ts, final Iterable<Card> attackersToBlock) {
        mustBlockCards.put(ts, new CardCollection(attackersToBlock));
        view.updateMustBlockCards(this);
    }
    public final void removeMustBlockCards(long ts) {
        mustBlockCards.remove(ts);
        view.updateMustBlockCards(this);
    }
    public final void clearMustBlockCards() {
        mustBlockCards.clear();
        view.updateMustBlockCards(this);
    }

    public final Card getCloneOrigin() {
        return cloneOrigin;
    }
    public final void setCloneOrigin(final Card cloneOrigin0) {
        cloneOrigin = view.setCard(cloneOrigin, cloneOrigin0, TrackableProperty.CloneOrigin);
    }

    public final boolean hasFirstStrike() {
        return hasKeyword(Keyword.FIRST_STRIKE);
    }

    public final boolean hasDoubleStrike() {
        return hasKeyword(Keyword.DOUBLE_STRIKE);
    }

    public final boolean hasDoubleTeam() {
        return hasKeyword(Keyword.DOUBLE_TEAM);
    }

    public final boolean hasSecondStrike() {
        return hasDoubleStrike() || !hasFirstStrike();
    }

    public final boolean hasConverge() {
        return "Count$Converge".equals(getSVar("X")) || "Count$Converge".equals(getSVar("Y")) ||
            hasKeyword(Keyword.SUNBURST) || hasKeyword("Modular:Sunburst");
    }

    @Override
    public final boolean canReceiveCounters(final CounterType type) {
        if (isPhasedOut()) {
            return false;
        }
        if (StaticAbilityCantPutCounter.anyCantPutCounter(this, type)) {
            return false;
        }
        return true;
    }

    @Override
    public void addCounterInternal(final CounterType counterType, final int n, final Player source, final boolean fireEvents, GameEntityCounterTable table, Map<AbilityKey, Object> params) {
        int addAmount = n;
        // Rules say it is only a SBA, but is it checked there too?
        if (counterType.is(CounterEnumType.DREAM) && hasKeyword("CARDNAME can't have more than seven dream counters on it.")) {
            addAmount = Math.min(addAmount, 7 - getCounters(CounterEnumType.DREAM));
        }
        if (addAmount <= 0 || !canReceiveCounters(counterType)) {
            // As per rule 107.1b
            return;
        }

        final int oldValue = getCounters(counterType);
        final int newValue = addAmount + oldValue;
        if (fireEvents) {
            getGame().updateLastStateForCard(this);

            // Not sure why firing events wraps EVERYTHING ins

            final int powerBonusBefore = getPowerBonusFromCounters();
            final int toughnessBonusBefore = getToughnessBonusFromCounters();
            final int loyaltyBefore = getCurrentLoyalty();

            int addedThisTurn = getGame().getCounterAddedThisTurn(counterType, this);
            setCounters(counterType, newValue);
            getGame().addCounterAddedThisTurn(source, counterType, this, addAmount);
            view.updateCounters(this);

            //fire card stats changed event if p/t bonuses or loyalty changed from added counters
            if (powerBonusBefore != getPowerBonusFromCounters() || toughnessBonusBefore != getToughnessBonusFromCounters() || loyaltyBefore != getCurrentLoyalty()) {
                getGame().fireEvent(new GameEventCardStatsChanged(this));
            }

            // play the Add Counter sound
            getGame().fireEvent(new GameEventCardCounters(this, counterType, oldValue, newValue));

            // Run triggers
            final Map<AbilityKey, Object> runParams = AbilityKey.mapFromCard(this);
            runParams.put(AbilityKey.Source, source);
            runParams.put(AbilityKey.CounterType, counterType);
            if (params != null) {
                runParams.putAll(params);
            }
            for (int i = 0; i < addAmount; i++) {
                runParams.put(AbilityKey.CounterAmount, oldValue + i + 1);
                getGame().getTriggerHandler().runTrigger(
                        TriggerType.CounterAdded, AbilityKey.newMap(runParams), false);
            }
            if (addAmount > 0) {
                runParams.put(AbilityKey.CounterAmount, addAmount);
                runParams.put(AbilityKey.FirstTime, addedThisTurn == 0);
                getGame().getTriggerHandler().runTrigger(
                        TriggerType.CounterAddedOnce, AbilityKey.newMap(runParams), false);
            }
        } else {
            setCounters(counterType, newValue);

            getGame().addCounterAddedThisTurn(source, counterType, this, addAmount);
            view.updateCounters(this);
        }
        if (newValue <= 0) {
            removeCounterTimestamp(counterType);
        } else if (addCounterTimestamp(counterType)) {
            updateAbilityTextForView();
        }
        if (table != null) {
            table.put(source, this, counterType, addAmount);
        }
    }

    public boolean addCounterTimestamp(CounterType counterType) {
        return addCounterTimestamp(counterType, true);
    }
    public boolean addCounterTimestamp(CounterType counterType, boolean updateView) {
        if (counterType.is(CounterEnumType.MANABOND)) {
            removeCounterTimestamp(counterType);

            long timestamp = game.getNextTimestamp();
            counterTypeTimestamps.put(counterType, timestamp);
            // becomes land in instead of other card types
            addChangedCardTypes(new CardType(ImmutableList.of("Land"), false), null, false,
                    EnumSet.of(RemoveType.CardTypes, RemoveType.SubTypes),
                    timestamp, 0, updateView, false);

            String abStr = "AB$ ManaReflected | Cost$ T | Valid$ Defined.Self | ColorOrType$ Color | ReflectProperty$ Is | SpellDescription$ Add one mana of any of this card's colors.";

            SpellAbility sa = AbilityFactory.getAbility(abStr, this);
            sa.setIntrinsic(false);

            addChangedCardTraits(ImmutableList.of(sa), null, null, null, null, true, false, timestamp, 0);
            return true;
        }
        if (!counterType.isKeywordCounter()) {
            return false;
        }
        removeCounterTimestamp(counterType);

        long timestamp = game.getNextTimestamp();
        counterTypeTimestamps.put(counterType, timestamp);
        addChangedCardKeywords(ImmutableList.of(counterType.toString()), null, false, timestamp, 0, updateView);
        return true;
    }

    public boolean removeCounterTimestamp(CounterType counterType) {
        return removeCounterTimestamp(counterType, true);
    }
    public boolean removeCounterTimestamp(CounterType counterType, boolean updateView) {
        Long old = counterTypeTimestamps.remove(counterType);
        if (old != null) {
            removeChangedCardTypes(old, 0, updateView);
            removeChangedCardTraits(old, 0);
            removeChangedCardKeywords(old, 0, updateView);
        }
        return old != null;
    }

    @Override
    public final void subtractCounter(final CounterType counterName, final int n) {
        subtractCounter(counterName, n, false);
    }

    public final void subtractCounter(final CounterType counterName, final int n, final boolean isDamage) {
        int oldValue = getCounters(counterName);
        int newValue = Math.max(oldValue - n, 0);

        final Map<AbilityKey, Object> repParams = AbilityKey.mapFromAffected(this);
        repParams.put(AbilityKey.CounterType, counterName);
        repParams.put(AbilityKey.Result, newValue);
        repParams.put(AbilityKey.IsDamage, isDamage);
        switch (getGame().getReplacementHandler().run(ReplacementType.RemoveCounter, repParams)) {
            case NotReplaced:
                break;
            case Updated:
                int result = (int) repParams.get(AbilityKey.Result);
                newValue = result;
                if (newValue <= 0) {
                    newValue = 0;
                }
                break;
            default:
                break;
        }

        final int delta = oldValue - newValue;
        if (delta == 0) { return; }

        int powerBonusBefore = getPowerBonusFromCounters();
        int toughnessBonusBefore = getToughnessBonusFromCounters();
        int loyaltyBefore = getCurrentLoyalty();

        setCounters(counterName, newValue);
        view.updateCounters(this);

        if (newValue <= 0) {
            if (removeCounterTimestamp(counterName)) {
                updateAbilityTextForView();
            }
        }

        //fire card stats changed event if p/t bonuses or loyalty changed from subtracted counters
        if (powerBonusBefore != getPowerBonusFromCounters() || toughnessBonusBefore != getToughnessBonusFromCounters() || loyaltyBefore != getCurrentLoyalty()) {
            getGame().fireEvent(new GameEventCardStatsChanged(this));
        }

        // Play the Subtract Counter sound
        getGame().fireEvent(new GameEventCardCounters(this, counterName, oldValue, newValue));

        getGame().addCounterRemovedThisTurn(counterName, this, delta);

        // Run triggers
        int curCounters = oldValue;
        final Map<AbilityKey, Object> runParams = AbilityKey.mapFromCard(this);
        runParams.put(AbilityKey.CounterType, counterName);
        for (int i = 0; i < delta && curCounters != 0; i++) {
            runParams.put(AbilityKey.NewCounterAmount, --curCounters);
            getGame().getTriggerHandler().runTrigger(TriggerType.CounterRemoved, AbilityKey.newMap(runParams), false);
        }
        runParams.put(AbilityKey.CounterAmount, delta);
        runParams.put(AbilityKey.NewCounterAmount, newValue);
        getGame().getTriggerHandler().runTrigger(TriggerType.CounterRemovedOnce, runParams, false);
    }

    @Override
    public final void setCounters(final Map<CounterType, Integer> allCounters) {
        boolean changed = false;
        for (CounterType ct : counters.keySet()) {
            if (removeCounterTimestamp(ct, false)) {
                changed = true;
            }
        }
        counters = allCounters;
        view.updateCounters(this);

        for (CounterType ct : counters.keySet()) {
            if (addCounterTimestamp(ct, false)) {
                changed = true;
            }
        }
        if (changed) {
            updateKeywords();
            updateAbilityTextForView();
        }
    }

    @Override
    public final void clearCounters() {
        if (counters.isEmpty()) { return; }
        counters.clear();
        view.updateCounters(this);

        boolean changed = false;
        for (CounterType ct : Lists.newArrayList(counterTypeTimestamps.keySet())) {
            if (removeCounterTimestamp(ct, false)) {
                changed = true;
            }
        }
        if (changed) {
            updateKeywords();
            updateAbilityTextForView();
        }
    }

    public final int sumAllCounters() {
        int count = 0;
        for (final Integer value2 : counters.values()) {
            count += value2;
        }
        return count;
    }

    public final String getSVar(final String var) {
        for (Map<String, String> map : changedSVars.values()) {
            if (map.containsKey(var)) {
                return map.get(var);
            }
        }
        return currentState.getSVar(var);
    }

    public final boolean hasSVar(final String var) {
        for (Map<String, String> map : changedSVars.values()) {
            if (map.containsKey(var)) {
                return true;
            }
        }
        return currentState.hasSVar(var);
    }

    public final void setSVar(final String var, final String str) {
        currentState.setSVar(var, str);
    }

    public final void copyChangedSVarsFrom(Card other) {
        changedSVars.clear();
        changedSVars.putAll(other.changedSVars);
    }

    @Override
    public final Map<String, String> getSVars() {
        return currentState.getSVars();
    }

    @Override
    public Map<String, String> getDirectSVars() {
        return ImmutableMap.of();
    }

    public final void setSVars(final Map<String, String> newSVars) {
        currentState.setSVars(newSVars);
    }

    public final void removeSVar(final String var) {
        currentState.removeSVar(var);
    }

    public final void addChangedSVars(Map<String, String> map, long timestamp, long staticId) {
        this.changedSVars.put(timestamp, staticId, map);
    }
    public final void removeChangedSVars(long timestamp, long staticId) {
        this.changedSVars.remove(timestamp, staticId);
    }

    public final int getTurnInZone() {
        return turnInZone;
    }
    public final void setTurnInZone(final int turn) {
        turnInZone = turn;
    }

    public final Player getTurnInController() {
        return turnInController;
    }
    public final void setTurnInController(final Player p) {
        turnInController = p;
    }

    public final void setManaCost(final ManaCost s) {
        currentState.setManaCost(s);
    }
    public final ManaCost getOriginalManaCost() {
        return currentState.getManaCost();
    }

    public final ManaCost getManaCost() {
        ManaCost result = getOriginalManaCost();
        for (ManaCost mc : changedCardManaCost.values()) {
            result = mc;
        }
        return result;
    }

    public void addChangedManaCost(ManaCost cost, long timestamp, long staticId) {
        changedCardManaCost.put(timestamp, staticId, cost);
    }
    public boolean removeChangedManaCost(long timestamp, long staticId) {
        return changedCardManaCost.remove(timestamp, staticId) != null;
    }

    public final boolean hasChosenPlayer() {
        return chosenPlayer != null;
    }
    public final Player getChosenPlayer() {
        return chosenPlayer;
    }
    public final void setChosenPlayer(final Player p) {
        if (chosenPlayer == p) { return; }
        chosenPlayer = p;
        view.updateChosenPlayer(this);
    }

    public final Player getProtectingPlayer() {
        return protectingPlayer;
    }
    public final void setProtectingPlayer(final Player p) {
        if (protectingPlayer == p) { return; }
        protectingPlayer = p;
        view.updateProtectingPlayer(this);
    }

    public final void setSecretChosenPlayer(final Player p) {
        chosenPlayer = p;
    }
    public final void revealChosenPlayer() {
        view.updateChosenPlayer(this);
    }

    public final boolean hasChosenNumber() {
        return chosenNumber != null;
    }
    public final Integer getChosenNumber() {
        return chosenNumber;
    }
    public final void setChosenNumber(final int i) {
        chosenNumber = i;
        view.updateChosenNumber(this);
    }

    public final Card getExiledWith() {
        return exiledWith;
    }
    public final void setExiledWith(final Card e) {
        exiledWith = view.setCard(exiledWith, e, TrackableProperty.ExiledWith);
    }

    public final void cleanupExiledWith() {
        if (exiledWith == null) {
            return;
        }

        // TODO this may not find the right card in case the ability was granted
        exiledWith.removeExiledCard(this);
        exiledWith.removeUntilLeavesBattlefield(this);

        exiledWith = null;
        exiledBy = null;
    }

    public final Player getExiledBy() { return exiledBy; }
    public final void setExiledBy(final Player ep) {
        exiledBy = ep;
    }

    public final String getChosenType() {
        return chosenType;
    }
    public final void setChosenType(final String s) {
        chosenType = s;
        view.updateChosenType(this);
    }
    public final boolean hasChosenType() {
        return chosenType != null && !chosenType.isEmpty();
    }

    // used by card Illusionary Terrain
    public final String getChosenType2() {
        return chosenType2;
    }
    public final void setChosenType2(final String s) {
        chosenType2 = s;
        view.updateChosenType2(this);
    }
    public final boolean hasChosenType2() {
        return chosenType2 != null && !chosenType2.isEmpty();
    }

    public final boolean hasAnyNotedType() {
        return notedTypes != null && !notedTypes.isEmpty();
    }

    public final void addNotedType(final String type) {
        notedTypes.add(type);
        view.updateNotedTypes(this);
    }

    public final Iterable<String> getNotedTypes() {
        if (notedTypes == null) {
            return Lists.newArrayList();
        }
        return notedTypes;
    }

    public final int getNumNotedTypes() {
        if (notedTypes == null) {
            return 0;
        }
        return notedTypes.size();
    }

    public final String getChosenColor() {
        if (hasChosenColor()) {
            return chosenColors.get(0);
        }
        return "";
    }
    public final Iterable<String> getChosenColors() {
        if (chosenColors == null) {
            return Lists.newArrayList();
        }
        return chosenColors;
    }
    public final void setChosenColors(final List<String> s) {
        chosenColors = s;
        view.updateChosenColors(this);
    }
    public boolean hasChosenColor() {
        return chosenColors != null && !chosenColors.isEmpty();
    }
    public boolean hasChosenColor(String s) {
        return chosenColors != null && chosenColors.contains(s);
    }

    public final Card getChosenCard() {
        return getChosenCards().getFirst();
    }
    public final CardCollectionView getChosenCards() {
        return CardCollection.getView(chosenCards);
    }
    public final void setChosenCards(final Iterable<Card> cards) {
        chosenCards = view.setCards(chosenCards, cards, TrackableProperty.ChosenCards);
    }
    public boolean hasChosenCard() {
        return FCollection.hasElements(chosenCards);
    }
    public boolean hasChosenCard(Card c) {
        return FCollection.hasElement(chosenCards, c);
    }

    public Direction getChosenDirection() {
        return chosenDirection;
    }
    public void setChosenDirection(Direction chosenDirection0) {
        if (chosenDirection == chosenDirection0) { return; }
        chosenDirection = chosenDirection0;
        view.updateChosenDirection(this);
    }

    public String getChosenMode() {
        return chosenMode;
    }
    public void setChosenMode(String mode) {
        chosenMode = mode;
        view.updateChosenMode(this);
    }

    public String getCurrentRoom() {
        return currentRoom;
    }
    public void setCurrentRoom(String room) {
        currentRoom = room;
        view.updateCurrentRoom(this);
        view.getCurrentState().updateAbilityText(this, getCurrentState());
    }
    public boolean isInLastRoom() {
        for (final Trigger t : getTriggers()) {
            SpellAbility sa = t.getOverridingAbility();
            if (sa.getParam("RoomName").equals(currentRoom) && !sa.hasParam("NextRoom")) {
                return true;
            }
        }
        return false;
    }

    public String getSector() {
        return sector;
    }
    public void assignSector(String s) {
        sector = s;
        view.updateSector(this);
    }
    public boolean hasSector() {
        return sector != null;
    }
    public String getChosenSector() {
        return chosenSector;
    }
    public final void setChosenSector(final String s) {
        chosenSector = s;
    }

    // used for cards like Meddling Mage...
    public final String getNamedCard() {
        return getChosenName();
    }
    public final void setNamedCard(final String s) {
        setChosenName(s);
    }
    public final String getNamedCard2() { return getChosenName2(); }
    public final void setNamedCard2(final String s) {
        setChosenName2(s);
    }

    public boolean hasChosenName() {
        return chosenName != null;
    }
    public boolean hasChosenName2() { return chosenName2 != null; }

    public String getChosenName() {
        return chosenName;
    }
    public final void setChosenName(final String s) {
        chosenName = s;
        view.updateNamedCard(this);
    }
    public String getChosenName2() {
        return chosenName2;
    }
    public final void setChosenName2(final String s) {
        chosenName2 = s;
        view.updateNamedCard2(this);
    }

    public boolean hasChosenEvenOdd() {
        return chosenEvenOdd != null;
    }

    public EvenOdd getChosenEvenOdd() {
        return chosenEvenOdd;
    }
    public void setChosenEvenOdd(EvenOdd chosenEvenOdd0) {
        if (chosenEvenOdd == chosenEvenOdd0) { return; }
        chosenEvenOdd = chosenEvenOdd0;
        view.updateChosenEvenOdd(this);
    }

    public final boolean getDrawnThisTurn() {
        return drawnThisTurn;
    }
    public final void setDrawnThisTurn(final boolean b) {
        drawnThisTurn = b;
    }

    public final boolean getFoughtThisTurn() {
        return foughtThisTurn;
    }
    public final void setFoughtThisTurn(final boolean b) {
        foughtThisTurn = b;
    }

    public final CardCollectionView getGainControlTargets() { //used primarily with AbilityFactory_GainControl
        return CardCollection.getView(gainControlTargets);
    }
    public final void addGainControlTarget(final Card c) {
        gainControlTargets = view.addCard(gainControlTargets, c, TrackableProperty.GainControlTargets);
    }
    public final void removeGainControlTargets(final Card c) {
        gainControlTargets = view.removeCard(gainControlTargets, c, TrackableProperty.GainControlTargets);
    }
    public final boolean hasGainControlTarget() {
        return FCollection.hasElements(gainControlTargets);
    }
    public final boolean hasGainControlTarget(Card c) {
        return FCollection.hasElement(gainControlTargets, c);
    }

    public final String getSpellText() {
        return text;
    }

    public final void setText(final String t) {
        originalText = t;
        text = originalText;
    }

    // get the text that does not belong to a cards abilities (and is not really
    // there rules-wise)
    public final String getNonAbilityText() {
        final StringBuilder sb = new StringBuilder();
        final StringBuilder sbLong = new StringBuilder();

        for (String keyword : getHiddenExtrinsicKeywords()) {
            if (keyword.startsWith("CantBeCounteredBy")) {
                final String[] p = keyword.split(":");
                sbLong.append(p[2]).append("\r\n");
            } else {
                sbLong.append(keyword).append("\r\n");
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
        return CardTranslation.translateMultipleDescriptionText(sb.toString(), getName());
    }

    // convert a keyword list to the String that should be displayed in game
    public final String keywordsToText(final Collection<KeywordInterface> keywords) {
        final StringBuilder sb = new StringBuilder();
        final StringBuilder sbLong = new StringBuilder();

        List<String> printedKW = new ArrayList<>();

        int i = 0;
        for (KeywordInterface inst : keywords) {
            String keyword = inst.getOriginal();
            try {
                if (keyword.startsWith("CantBeCounteredBy")) {
                    final String[] p = keyword.split(":");
                    sbLong.append(p[2]).append("\r\n");
                } else if (keyword.startsWith("etbCounter")) {
                    final String[] p = keyword.split(":");
                    final StringBuilder s = new StringBuilder();
                    if (p.length > 4) {
                        if (!"no desc".equals(p[4])) {
                            s.append(p[4]);
                        }
                    } else {
                        s.append(getName()).append(" enters the battlefield with ");
                        s.append(Lang.nounWithNumeralExceptOne(p[2],
                                CounterType.getType(p[1]).getName().toLowerCase() + " counter"));
                        s.append(" on it.");
                    }
                    sbLong.append(s).append("\r\n");
                } else if (keyword.startsWith("Protection:") || keyword.startsWith("DeckLimit")) {
                    final String[] k = keyword.split(":");
                    sbLong.append(k[2]).append("\r\n");
                } else if (keyword.startsWith("Creatures can't attack unless their controller pays")) {
                    final String[] k = keyword.split(":");
                    if (!k[3].equals("no text")) {
                        sbLong.append(k[3]).append("\r\n");
                    }
                } else if (keyword.startsWith("Enchant")) {
                    String k = keyword;
                    k = TextUtil.fastReplace(k, "Curse", "");
                    sbLong.append(k).append("\r\n");
                } else if (keyword.startsWith("Ripple")) {
                    sbLong.append(TextUtil.fastReplace(keyword, ":", " ")).append("\r\n");
                } else if (keyword.startsWith("Morph") || keyword.startsWith("Megamorph")
                        || keyword.startsWith("Escape") || keyword.startsWith("Foretell:")
                        || keyword.startsWith("Madness:")|| keyword.startsWith("Recover")
                        || keyword.startsWith("Reconfigure") || keyword.startsWith("Squad")
                        || keyword.startsWith("Miracle") || keyword.startsWith("More Than Meets the Eye")
                        || keyword.startsWith("Level up")) {
                    String[] k = keyword.split(":");
                    sbLong.append(k[0]);
                    if (k.length > 1) {
                        final Cost mCost = new Cost(k[1], true);
                        if (mCost.isOnlyManaCost()) {
                            sbLong.append(" ");
                        } else {
                            sbLong.append("—");
                        }
                        sbLong.append(mCost.toString());
                        if (!mCost.isOnlyManaCost()) {
                            sbLong.append(".");
                        }
                        sbLong.append(" (").append(inst.getReminderText()).append(")");
                        sbLong.append("\r\n");
                    }
                } else if (keyword.startsWith("Madness")) {
                    // If no colon exists in Madness keyword, it must have been granted and assumed the cost from host
                    sbLong.append("Madness ").append(this.getManaCost()).append(" (").append(inst.getReminderText());
                    sbLong.append(")").append("\r\n");
                } else if (keyword.startsWith("Emerge") || keyword.startsWith("Reflect")) {
                    final String[] k = keyword.split(":");
                    sbLong.append(k[0]).append(" ").append(ManaCostParser.parse(k[1]));
                    sbLong.append(" (").append(inst.getReminderText()).append(")");
                    sbLong.append("\r\n");
                } else if (keyword.startsWith("Echo")) {
                    sbLong.append("Echo ");
                    final String[] upkeepCostParams = keyword.split(":");
                    sbLong.append(upkeepCostParams.length > 2 ? "— " + upkeepCostParams[2] : ManaCostParser.parse(upkeepCostParams[1]));
                    sbLong.append(" (At the beginning of your upkeep, if CARDNAME came under your control since the beginning of your last upkeep, sacrifice it unless you pay its echo cost.)");
                    sbLong.append("\r\n");
                } else if (keyword.startsWith("Cumulative upkeep")) {
                    sbLong.append("Cumulative upkeep ");
                    final String[] upkeepCostParams = keyword.split(":");
                    sbLong.append(upkeepCostParams.length > 2 ? "— " + upkeepCostParams[2] : ManaCostParser.parse(upkeepCostParams[1]));
                    sbLong.append("\r\n");
                } else if (keyword.startsWith("Alternative Cost")) {
                    sbLong.append("Has alternative cost.");
                } else if (keyword.startsWith("AlternateAdditionalCost")) {
                    final String[] costs = keyword.split(":", 2)[1].split(":");
                    sbLong.append("As an additional cost to cast this spell, ");
                    for (int n = 0; n < costs.length; n++) {
                        final Cost cost = new Cost(costs[n], false);
                        if (cost.isOnlyManaCost()) {
                            sbLong.append(" pay ");
                        }
                        sbLong.append(StringUtils.uncapitalize(cost.toSimpleString()));
                        sbLong.append(n + 1 == costs.length ? ".\r\n\r\n" : n + 2 == costs.length && costs.length > 2
                                ? ", or " : n + 2 == costs.length ? " or " : ", ");
                    }
                } else if (keyword.startsWith("Multikicker")) {
                    if (!keyword.endsWith("Generic")) {
                        final String[] n = keyword.split(":");
                        final Cost cost = new Cost(n[1], false);
                        sbLong.append("Multikicker ").append(cost.toSimpleString());
                        sbLong.append(" (").append(inst.getReminderText()).append(")").append("\r\n");
                    }
                } else if (keyword.startsWith("Kicker")) {
                    sbLong.append(kickerDesc(keyword, inst.getReminderText())).append("\r\n");
                } else if (keyword.startsWith("Trample:")) {
                    sbLong.append("Trample over planeswalkers").append(" (").append(inst.getReminderText()).append(")").append("\r\n");
                } else if (keyword.startsWith("Hexproof:")) {
                    final String[] k = keyword.split(":");
                    if(!k[2].equals("Secondary")) {
                        sbLong.append("Hexproof from ");
                        if (k[2].equals("chosen")) {
                            k[2] = k[1].substring(5).toLowerCase();
                        }
                        sbLong.append(k[2]);
                        if (!k[2].contains(" and ")) { // skip reminder text for more complicated Hexproofs
                            sbLong.append(" (").append(inst.getReminderText().replace("chosen", k[2]));
                            sbLong.append(")");
                        }
                        sbLong.append("\r\n");
                    }
                } else if (inst.getKeyword().equals(Keyword.COMPANION)) {
                    sbLong.append("Companion — ");
                    sbLong.append(((Companion)inst).getDescription());
                } else if (keyword.startsWith("MayFlash")) {
                    // Pseudo keywords, only print Reminder
                    sbLong.append(inst.getReminderText()).append("\r\n");
                } else if (keyword.contains("At the beginning of your upkeep, ")
                        && keyword.contains(" unless you pay")) {
                    sbLong.append(keyword).append("\r\n");
                } else if (keyword.startsWith("Strive") || keyword.startsWith("Escalate")
                        || keyword.startsWith("ETBReplacement")
                        || keyword.startsWith("Affinity")
                        || keyword.startsWith("UpkeepCost")) {
                } else if (keyword.equals("Provoke") || keyword.equals("Ingest") || keyword.equals("Unleash")
                        || keyword.equals("Soulbond") || keyword.equals("Partner") || keyword.equals("Retrace")
                        || keyword.equals("Living Weapon") || keyword.equals("Myriad") || keyword.equals("Exploit")
                        || keyword.equals("Changeling") || keyword.equals("Delve") || keyword.equals("Decayed")
                        || keyword.equals("Split second") || keyword.equals("Sunburst")
                        || keyword.equals("Double team") || keyword.equals("Living metal")
                        || keyword.equals("Suspend") // for the ones without amount
                        || keyword.equals("Foretell") // for the ones without cost
                        || keyword.equals("Ascend") || keyword.equals("Totem armor")
                        || keyword.equals("Battle cry") || keyword.equals("Devoid") || keyword.equals("Riot")
                        || keyword.equals("Daybound") || keyword.equals("Nightbound")
                        || keyword.equals("Friends forever") || keyword.equals("Choose a Background")
                        || keyword.equals("Space sculptor")) {
                    sbLong.append(keyword).append(" (").append(inst.getReminderText()).append(")");
                } else if (keyword.startsWith("Partner:")) {
                    final String[] k = keyword.split(":");
                    sbLong.append("Partner with ").append(k[1]).append(" (").append(inst.getReminderText()).append(")");
                } else if (keyword.equals("Compleated")) {
                    sbLong.append(keyword).append(" (");
                    final ManaCost mc = this.getManaCost();
                    if (mc != ManaCost.NO_COST && mc.hasPhyrexian()) {
                        String pip = mc.getFirstPhyrexianPip();
                        String[] parts = pip.substring(1, pip.length() - 1).split("/");
                        final StringBuilder rem = new StringBuilder();
                        rem.append(pip).append(" can be paid with {").append(parts[1]).append("}");
                        if (parts.length > 2) {
                            rem.append(", {").append(parts[2]).append("},");
                        }
                        rem.append(" or 2 life. ");
                        if (mc.getPhyrexianCount() > 1) {
                            rem.append("For each ").append(pip).append(" paid with life,");
                        } else {
                            rem.append("If life was paid,");
                        }
                        rem.append(" this planeswalker enters with two fewer loyalty counters.");
                        sbLong.append(rem.toString());
                    }
                    sbLong.append(")");
                } else if (keyword.startsWith("Devour ")) {
                    final String[] k = keyword.split(":");
                    final String[] s = (k[0]).split(" ");
                    final String t = s[1];
                    sbLong.append(k[0]).append(" ").append(k[1]).append(" (As this enters the battlefield, you may ");
                    sbLong.append("sacrifice any number of ").append(t).append("s. This creature enters the ");
                    sbLong.append("battlefield with that many +1/+1 counters on it.)");
                } else if (keyword.startsWith("Prototype")) {
                    final String[] k = keyword.split(":");
                    final Cost cost = new Cost(k[1], false);
                    sbLong.append(k[0]).append(" ").append(cost.toSimpleString()).append(" ").append("[").append(k[2]);
                    sbLong.append("/").append(k[3]).append("] ").append("(").append(inst.getReminderText()).append(")");
                } else if (keyword.startsWith("Modular") || keyword.startsWith("Bloodthirst") || keyword.startsWith("Dredge")
                        || keyword.startsWith("Fabricate") || keyword.startsWith("Soulshift") || keyword.startsWith("Bushido")
                        || keyword.startsWith("Crew") || keyword.startsWith("Tribute") || keyword.startsWith("Absorb")
                        || keyword.startsWith("Graft") || keyword.startsWith("Fading") || keyword.startsWith("Vanishing:")
                        || keyword.startsWith("Afterlife") || keyword.startsWith("Hideaway") || keyword.startsWith("Toxic")
                        || keyword.startsWith("Afflict") || keyword.startsWith ("Poisonous") || keyword.startsWith("Rampage")
                        || keyword.startsWith("Renown") || keyword.startsWith("Annihilator") || keyword.startsWith("Devour")) {
                    final String[] k = keyword.split(":");
                    sbLong.append(k[0]).append(" ").append(k[1]).append(" (").append(inst.getReminderText()).append(")");
                } else if (keyword.startsWith("Casualty")) {
                    final String[] k = keyword.split(":");
                    sbLong.append("Casualty ").append(k[1]);
                    if (k.length >= 4) {
                        sbLong.append(". ").append(k[3]);
                    }
                    sbLong.append(" (").append(inst.getReminderText()).append(")");
                } else if (keyword.startsWith("Starting intensity")) {
                    sbLong.append(TextUtil.fastReplace(keyword, ":", " "));
                } else if (keyword.contains("Haunt")) {
                    sb.append("\r\nHaunt (");
                    if (isCreature()) {
                        sb.append("When this creature dies, exile it haunting target creature.");
                    } else {
                        sb.append("When this spell card is put into a graveyard after resolving, ");
                        sb.append("exile it haunting target creature.");
                    }
                    sb.append(")");
                } else if (keyword.equals("Convoke") || keyword.equals("Dethrone")|| keyword.equals("Fear")
                         || keyword.equals("Melee") || keyword.equals("Improvise")|| keyword.equals("Shroud")
                         || keyword.equals("Banding") || keyword.equals("Intimidate")|| keyword.equals("Evolve")
                         || keyword.equals("Exalted") || keyword.equals("Extort")|| keyword.equals("Flanking")
                         || keyword.equals("Horsemanship") || keyword.equals("Infect")|| keyword.equals("Persist")
                         || keyword.equals("Phasing") || keyword.equals("Shadow")|| keyword.equals("Skulk")
                         || keyword.equals("Undying") || keyword.equals("Wither")
                         || keyword.equals("Bargain")
                         || keyword.equals("Mentor") || keyword.equals("Training")) {
                    if (sb.length() != 0) {
                        sb.append("\r\n");
                    }
                    sb.append(keyword);
                    if (!printedKW.contains(keyword)) {
                        sb.append(" (").append(inst.getReminderText()).append(")");
                        printedKW.add(keyword);
                    }
                } else if (keyword.equals("Cascade")) { // this could become a list for easy keywords that stack
                    if (printedKW.contains(keyword)) {
                        continue;
                    }
                    if (sb.length() != 0) {
                        sb.append("\r\n");
                    }

                    StringBuilder descStr = new StringBuilder(keyword);
                    int times = 0;
                    for (KeywordInterface keyw : keywords) {
                        String kw = keyw.getOriginal();
                        if (kw.equals(keyword)) {
                            descStr.append(times == 0 ? "" : ", " + StringUtils.uncapitalize(keyword));
                            times++;
                        }
                    }
                    sb.append(descStr).append(" ").append(" (").append(inst.getReminderText()).append(")");
                    printedKW.add(keyword);
                } else if (keyword.startsWith("Ward")) {
                    final String[] k = keyword.split(":");
                    final Cost cost = new Cost(k[1], false);
                    final boolean onlyMana = cost.isOnlyManaCost();
                    final boolean complex = k[1].contains("X") || k[1].contains("Sac<");
                    final String extra = k.length > 2 ? ", " + k[2] + "." : "";

                    sbLong.append(k[0]).append(onlyMana ? " " : "—").append(cost.toSimpleString());
                    sbLong.append(onlyMana? "" : ".").append(extra);
                    sbLong.append(!complex ? " (" + (inst.getReminderText()) + ")" : "");
                    sbLong.append("\r\n");
                } else if (keyword.endsWith(" offering")) {
                    String offeringType = keyword.split(" ")[0];
                    if (sb.length() != 0) {
                        sb.append("\r\n");
                    }
                    sbLong.append(keyword);
                    sbLong.append(" (").append(Keyword.getInstance("Offering:" + offeringType).getReminderText()).append(")");
                } else if (keyword.startsWith("Equip") || keyword.startsWith("Fortify") || keyword.startsWith("Outlast")
                        || keyword.startsWith("Unearth") || keyword.startsWith("Scavenge")
                        || keyword.startsWith("Spectacle") || keyword.startsWith("Evoke")
                        || keyword.startsWith("Bestow") || keyword.startsWith("Surge")
                        || keyword.startsWith("Transmute") || keyword.startsWith("Suspend")
                        || keyword.startsWith("Dash") || keyword.startsWith("Disturb")
                        || keyword.equals("Undaunted") || keyword.startsWith("Monstrosity")
                        || keyword.startsWith("Embalm") || keyword.equals("Prowess")
                        || keyword.startsWith("Eternalize") || keyword.startsWith("Reinforce")
                        || keyword.startsWith("Champion") || keyword.startsWith("Prowl") || keyword.startsWith("Adapt")
                        || keyword.startsWith("Amplify") || keyword.startsWith("Ninjutsu") || keyword.startsWith("Saga")
                        || keyword.startsWith("Transfigure") || keyword.startsWith("Aura swap")
                        || keyword.startsWith("Cycling") || keyword.startsWith("TypeCycling")
                        || keyword.startsWith("Encore") || keyword.startsWith("Mutate") || keyword.startsWith("Dungeon")
                        || keyword.startsWith("Class") || keyword.startsWith("Blitz")
                        || keyword.startsWith("Specialize") || keyword.equals("Ravenous")
                        || keyword.equals("For Mirrodin")) {
                    // keyword parsing takes care of adding a proper description
                } else if (keyword.startsWith("Read ahead")) {
                    sb.append(Localizer.getInstance().getMessage("lblReadAhead")).append(" (").append(Localizer.getInstance().getMessage("lblReadAheadDesc"));
                    sb.append(" ").append(Localizer.getInstance().getMessage("lblSagaFooter")).append(" ").append(TextUtil.toRoman(getFinalChapterNr())).append(".");
                    sb.append(")").append("\r\n\r\n");
                } else if (keyword.startsWith("Backup")) {
                    if (printedKW.contains("Backup")) {
                        continue;
                    }
                    boolean plural = false;

                    StringBuilder descStr = new StringBuilder("Backup ");
                    int times = 0;
                    for (KeywordInterface keyw : keywords) {
                        String kw = keyw.getOriginal();
                        if (kw.startsWith("Backup")) {
                            final String[] k = keyword.split(":");
                            String magnitude = k[1];
                            if (times == 0 && k[2].endsWith("s")) {
                                plural = true;
                            }
                            descStr.append(times == 0 ? magnitude : ", backup " + magnitude);
                            times++;
                        }
                    }
                    sb.append(descStr).append(" ").append(" (");
                    String remStr = inst.getReminderText();
                    if (plural) {
                        remStr = remStr.replace("ability", "abilities");
                    }
                    sb.append(remStr).append(times > 1 ? " Each backup ability triggers separately." : "").append(")");
                    printedKW.add("Backup");
                } else if (keyword.startsWith("MayEffectFromOpening")) {
                    final String[] k = keyword.split(":");
                    // need to get SpellDescription from Svar
                    String desc = AbilityFactory.getMapParams(getSVar(k[1])).get("SpellDescription");
                    sbLong.append(desc);
                } else if (keyword.endsWith(".") && !keyword.startsWith("Haunt")) {
                    sbLong.append(keyword).append("\r\n");
                } else {
                    if (keyword.contains("Strike")) {
                        keyword = keyword.replace("Strike", "strike");
                    }
                    sb.append(i !=0 && sb.length() !=0 ? ", " : "");
                    sb.append(i > 0 && sb.length() !=0 ? StringUtils.uncapitalize(keyword) : keyword);
                }
                if (sbLong.length() > 0) {
                    sbLong.append("\r\n");
                }

                if (keyword.equals("Flash") || keyword.startsWith("Backup")) {
                    sb.append("\r\n\r\n");
                    i = 0;
                } else {
                    i++;
                }
            } catch (Exception e) {
                String msg = "Card:keywordToText: crash in Keyword parsing";

                Breadcrumb bread = new Breadcrumb(msg);
                bread.setData("Card", this.getName());
                bread.setData("Keyword", keyword);
                Sentry.addBreadcrumb(bread, this);

                throw new RuntimeException("Error in Card " + this.getName() + " with Keyword " + keyword, e);
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
        return CardTranslation.translateMultipleDescriptionText(sb.toString(), getName());
    }

    private String kickerDesc(String keyword, String remText) {
        final StringBuilder sbx = new StringBuilder();
        final String[] n = keyword.split(":");
        final Cost cost = new Cost(n[1], false);
        final String costStr = cost.toSimpleString();
        final boolean manaOnly = cost.isOnlyManaCost();
        if (!keyword.endsWith("Generic")) {
            sbx.append("Kicker").append(manaOnly ? " " + costStr : "—" + costStr + ".");
            if (Lists.newArrayList(n).size() > 2) {
                sbx.append(" and/or ");
                final Cost cost2 = new Cost(n[2], false);
                sbx.append(cost2.toSimpleString());
            }
            if (!manaOnly) {
                if (cost.hasNoManaCost()) {
                    remText = remText.replaceFirst(" pay an additional", "");
                    remText = remText.replace(remText.charAt(8), Character.toLowerCase(remText.charAt(8)));
                } else {
                    remText = remText.replaceFirst(" an additional", "");
                    char c = remText.charAt(remText.indexOf(",") + 2);
                    remText = remText.replace(c, Character.toLowerCase(c));
                    remText = remText.replaceFirst(", ", " and ");
                }
                remText = remText.replaceFirst("as", "in addition to any other costs as");
                if (remText.contains(" tap ")) {
                    if (remText.contains("tap a")) {
                        String noun = remText.substring(remText.indexOf("untapped") + 9, remText.indexOf(" in "));
                        remText = remText.replace(remText.substring(12, remText.indexOf(" in ")),
                                Lang.nounWithNumeralExceptOne(1, noun) + " ");
                    } else {
                        remText = remText.replaceFirst(" untapped ", "");
                    }
                }
            }
            sbx.append(" (").append(remText).append(")\r\n");
        } else {
            sbx.append("As an additional cost to cast this spell, you may ");
            String costS = StringUtils.uncapitalize(costStr);
            sbx.append(cost.hasManaCost() ? "pay " + costS : costS);
            sbx.append(".").append("\r\n");
        }
        return sbx.toString();
    }

    // get the text of the abilities of a card
    public String getAbilityText() {
        return getAbilityText(currentState);
    }
    public String getAbilityText(final CardState state) {
        final String linebreak = "\r\n\r\n";
        boolean useGrayTag = true;
        if (getGame() != null) {
            useGrayTag = game.getRules().useGrayText();
        }
        final String grayTag = useGrayTag ? "<span style=\"color:gray;\">" : "";
        final String endTag = useGrayTag ? "</span>" : "";
        final CardTypeView type = state.getType();

        final StringBuilder sb = new StringBuilder();
        if (!mayPlay.isEmpty()) {
            Set<String> players = new HashSet<>();
            for (CardPlayOption o : mayPlay.values()) {
                if (getController() == o.getPlayer())
                    players.add(o.getPlayer().getName());
                else if (o.grantsZonePermissions())
                    players.add(o.getPlayer().getName());
            }
            if (!players.isEmpty()) {
                sb.append("May be played by: ");
                sb.append(Lang.joinHomogenous(players));
                sb.append("\r\n");
            }
        }

        if (type.isInstant() || type.isSorcery()) {
            sb.append(abilityTextInstantSorcery(state));

            if (haunting != null) {
                sb.append("Haunting: ").append(haunting);
                sb.append("\r\n");
            }

            String result = sb.toString();
            while (result.endsWith("\r\n")) {
                result = result.substring(0, result.length() - 2);
            }
            return TextUtil.fastReplace(result, "CARDNAME", CardTranslation.getTranslatedName(state.getName()));
        }

        if (type.hasSubtype("Class")) {
            sb.append("(Gain the next level as a sorcery to add its ability.)").append(linebreak);
        }

        // Check if the saga card does not have the keyword Read ahead
        if (type.hasSubtype("Saga") && !this.hasStartOfKeyword("Read ahead")) {
            sb.append("(").append(Localizer.getInstance().getMessage("lblSagaHeader"));
            if (!state.getCard().isTransformable()) {
                sb.append(" ").append(Localizer.getInstance().getMessage("lblSagaFooter")).append(" ").append(TextUtil.toRoman(getFinalChapterNr())).append(".");
            }
            sb.append(")").append(linebreak);
        }

        // add As an additional cost to Permanent spells
        SpellAbility first = state.getFirstAbility();
        if (first != null && type.isPermanent()) {
            if (first.isSpell()) {
                Cost cost = first.getPayCosts();
                if (cost != null && !cost.isOnlyManaCost()) {
                    String additionalDesc = "";
                    if (first.hasParam("AdditionalDesc")) {
                        additionalDesc = first.getParam("AdditionalDesc");
                    }
                    sb.append(cost.toString().replace("\n", "")).append(" ").append(additionalDesc);
                    sb.append(linebreak);
                }
            }
        }

        if (monstrous) {
            sb.append("Monstrous\r\n");
        }
        if (renowned) {
            sb.append("Renowned\r\n");
        }
        if (manifested) {
            sb.append("Manifested\r\n");
        }
        String keywordText = keywordsToText(getUnhiddenKeywords(state));
        sb.append(keywordText).append(keywordText.length() > 0 ? linebreak : "");

        // Process replacement effects first so that "enters the battlefield tapped"
        // and "as ~ enters the battlefield, choose...", etc can be printed
        // here. The rest will be printed later.
        StringBuilder replacementEffects = new StringBuilder();
        for (final ReplacementEffect replacementEffect : state.getReplacementEffects()) {
            if (!replacementEffect.isSecondary() && !replacementEffect.isClassAbility()) {
                String text = replacementEffect.getDescription();
                // Get original description since text might be translated
                if (replacementEffect.hasParam("Description") &&
                        replacementEffect.getParam("Description").contains("enters the battlefield")) {
                    sb.append(text).append(linebreak);
                } else {
                    replacementEffects.append(text).append(linebreak);
                }
            }
        }

        if (this.getRules() != null && state.getView().getState().equals(CardStateName.Original)) {
            // try to look which what card this card can be meld to
            // only show this info if this card does not has the meld Effect itself

            boolean hasMeldEffect = hasSVar("Meld")
                    || Iterables.any(state.getNonManaAbilities(), SpellAbilityPredicates.isApi(ApiType.Meld));
            String meld = this.getRules().getMeldWith();
            if (meld != "" && !hasMeldEffect) {
                sb.append("\r\n");
                sb.append("(Melds with ").append(meld).append(".)");
                sb.append("\r\n");
            }
        }

        // Give spellText line breaks for easier reading
        sb.append(text.replaceAll("\\\\r\\\\n", "\r\n"));
        sb.append(linebreak);

        // Triggered abilities
        for (final Trigger trig : state.getTriggers()) {
            if (!trig.isSecondary() && !trig.isClassAbility()) {
                boolean disabled;
                // Disable text of other rooms
                if (type.isDungeon()) {
                    disabled = !trig.getOverridingAbility().getParam("RoomName").equals(getCurrentRoom());
                } else {
                    disabled = getGame() != null && !trig.requirementsCheck(getGame());
                }
                String trigStr = trig.replaceAbilityText(trig.toString(), state);
                if (disabled) sb.append(grayTag);
                sb.append(trigStr.replaceAll("\\\\r\\\\n", "\r\n"));
                if (disabled) sb.append(endTag);
                sb.append(linebreak);
            }
        }

        // Replacement effects
        sb.append(replacementEffects);

        // static abilities
        for (final StaticAbility stAb : state.getStaticAbilities()) {
            if (!stAb.isSecondary() && !stAb.isClassAbility()) {
                final String stAbD = stAb.toString();
                if (!stAbD.equals("")) {
                    boolean disabled = getGame() != null && getController() != null && game.getAge() != GameStage.Play && !stAb.checkConditions();
                    if (disabled) sb.append(grayTag);
                    sb.append(stAbD);
                    if (disabled) sb.append(endTag);
                    sb.append(linebreak);
                }
            }
        }

        final List<String> addedManaStrings = Lists.newArrayList();

        for (final SpellAbility sa : state.getSpellAbilities()) {
            // This code block is not shared by instants or sorceries. We don't need to check for permanence.
            if (sa == null || sa.isSecondary() || sa.isClassAbility()) {
                continue;
            }

            // skip Basic Spells
            if (sa.isSpell() && sa.isBasicSpell()) {
                continue;
            }

            // should not print Spelldescription for Morph
            if (sa.isCastFaceDown()) {
                continue;
            }

            String sAbility = formatSpellAbility(sa);

            // add Adventure to AbilityText
            if (sa.isAdventure() && state.getStateName().equals(CardStateName.Original)) {
                CardState advState = getState(CardStateName.Adventure);
                StringBuilder sbSA = new StringBuilder();
                sbSA.append(Localizer.getInstance().getMessage("lblAdventure"));
                sbSA.append(" — ").append(CardTranslation.getTranslatedName(advState.getName()));
                sbSA.append(" ").append(sa.getPayCosts().toSimpleString());
                sbSA.append(": ");
                sbSA.append(sAbility);
                sAbility = sbSA.toString();
            }

            if (sa.getManaPart() != null) {
                if (addedManaStrings.contains(sAbility)) {
                    continue;
                }
                addedManaStrings.add(sAbility);
            }

            if (!sAbility.endsWith(state.getName() + "\r\n")) {
                sb.append(sAbility);
                sb.append("\r\n");
            }
        }

        // CantBlockBy static abilities
        if (game != null && isCreature() && isInPlay()) {
            for (final Card ca : game.getCardsIn(ZoneType.STATIC_ABILITIES_SOURCE_ZONES)) {
                if (equals(ca)) {
                    continue;
                }
                for (final StaticAbility stAb : ca.getStaticAbilities()) {
                    if (!stAb.checkConditions()) {
                        continue;
                    }

                    boolean found = false;
                    if (stAb.checkMode("CantBlockBy")) {
                        if (!stAb.hasParam("ValidAttacker") || (stAb.hasParam("ValidBlocker") && stAb.getParam("ValidBlocker").equals("Creature.Self"))) {
                            continue;
                        }
                        if (stAb.matchesValidParam("ValidAttacker", this)) {
                            found = true;
                        }
                    } else if (stAb.checkMode(StaticAbilityCantAttackBlock.MinMaxBlockerMode)) {
                        if (stAb.matchesValidParam("ValidCard", this)) {
                            found = true;
                        }
                    }

                    if (found) {
                        final Card host = stAb.getHostCard();

                        String currentName = host.getName();
                        String desc = TextUtil.fastReplace(stAb.toString(), "CARDNAME", currentName);
                        desc = TextUtil.fastReplace(desc, "NICKNAME", Lang.getInstance().getNickName(currentName));
                        if (host.getEffectSource() != null) {
                            desc = TextUtil.fastReplace(desc, "EFFECTSOURCE", host.getEffectSource().getName());
                        }
                        sb.append(desc);
                        sb.append(linebreak);
                    }
                }
            }
        }

        // Class Abilities
        if (isClassCard()) {
            sb.append(linebreak);
            // Currently the maximum levels of all Class cards are all 3
            for (int level = 1; level <= 3; ++level) {
                boolean disabled = level > getClassLevel() && isInPlay();
                // Class second part is a static ability that grants the other abilities
                for (final StaticAbility st : state.getStaticAbilities()) {
                    if (st.isClassLevelNAbility(level) && !st.isSecondary()) {
                        if (disabled) sb.append(grayTag);
                        sb.append(st.toString());
                        if (disabled) sb.append(endTag);
                        sb.append(linebreak);
                    }
                }
                // Currently all activated abilities on Class cards are level up abilities
                for (final SpellAbility sa : state.getSpellAbilities()) {
                    if (sa.isClassLevelNAbility(level) && !sa.isSecondary()) {
                        sb.append(sa.toString()).append(linebreak);
                    }
                }
            }
        }

        // NOTE:
        if (sb.toString().contains(" (NOTE: ")) {
            sb.insert(sb.indexOf("(NOTE: "), "\r\n");
        }
        if (sb.toString().contains("(NOTE: ") && sb.toString().contains(".) ")) {
            sb.insert(sb.indexOf(".) ") + 3, "\r\n");
        }

        if (isGoaded()) {
            sb.append("is goaded by: ").append(Lang.joinHomogenous(getGoaded()));
            sb.append("\r\n");
        }

        // replace triple line feeds with double line feeds
        final String s = "\r\n\r\n\r\n";
        int start = sb.lastIndexOf(s);
        while (start != -1) {
            sb.replace(start, start + 4, "\r\n");
            start = sb.lastIndexOf(s);
        }

        String desc = TextUtil.fastReplace(sb.toString(), "CARDNAME", CardTranslation.getTranslatedName(state.getName()));
        if (getEffectSource() != null) {
            desc = TextUtil.fastReplace(desc, "EFFECTSOURCE", getEffectSource().getName());
        }

        return desc.trim();
    }

    private StringBuilder abilityTextInstantSorcery(CardState state) {
        final StringBuilder sb = new StringBuilder();

        // Give spellText line breaks for easier reading
        String spellText = text.replaceAll("\\\\r\\\\n", "\r\n");
        sb.append(spellText);

        // NOTE:
        if (spellText.contains(" (NOTE: ")) {
            sb.insert(sb.indexOf("(NOTE: "), "\r\n");
        }
        if (spellText.contains("(NOTE: ") && spellText.endsWith(".)") && !spellText.endsWith("\r\n")) {
            sb.append("\r\n");
        }

        final StringBuilder sbSpell = new StringBuilder();

        // I think SpellAbilities should be displayed after Keywords
        // Add SpellAbilities
        for (final SpellAbility element : state.getSpellAbilities()) {
            if (!element.isSecondary()) {
                element.usesTargeting();
                sbSpell.append(formatSpellAbility(element));
            }
        }
        final String strSpell = sbSpell.toString();

        final StringBuilder sbBefore = new StringBuilder();
        final StringBuilder sbAfter = new StringBuilder();

        for (final KeywordInterface inst : getKeywords(state)) {
            final String keyword = inst.getOriginal();

            try {
                if (keyword.equals("Ascend")  || keyword.equals("Changeling")
                        || keyword.equals("Aftermath") || keyword.equals("Wither")
                        || keyword.equals("Convoke") || keyword.equals("Delve")
                        || keyword.equals("Improvise") || keyword.equals("Retrace")
                        || keyword.equals("Undaunted") || keyword.equals("Cascade")
                        || keyword.equals("Devoid") ||  keyword.equals("Lifelink")
                        || keyword.equals("Bargain")
                        || keyword.equals("Split second")) {
                    sbBefore.append(keyword).append(" (").append(inst.getReminderText()).append(")");
                    sbBefore.append("\r\n\r\n");
                } else if (keyword.equals("Conspire") || keyword.equals("Epic")
                        || keyword.equals("Suspend") || keyword.equals("Jump-start")
                        || keyword.equals("Fuse")) {
                    sbAfter.append(keyword).append(" (").append(inst.getReminderText()).append(")");
                    sbAfter.append("\r\n");
                } else if (keyword.startsWith("Casualty")) {
                    final String[] k = keyword.split(":");
                    sbBefore.append("Casualty ").append(k[1]);
                    if (k.length >= 4) {
                        sbBefore.append(". ").append(k[3]);
                    }
                    sbBefore.append(" (").append(inst.getReminderText()).append(")").append("\r\n\r\n");
                } else if (keyword.startsWith("Dredge") || keyword.startsWith("Ripple")) {
                    sbAfter.append(TextUtil.fastReplace(keyword, ":", " "));
                    sbAfter.append(" (").append(inst.getReminderText()).append(")").append("\r\n");
                } else if (keyword.startsWith("Starting intensity")) {
                    sbAfter.append(TextUtil.fastReplace(keyword, ":", " ")).append("\r\n");
                } else if (keyword.startsWith("Escalate") || keyword.startsWith("Buyback")
                        || keyword.startsWith("Prowl")) {
                    final String[] k = keyword.split(":");
                    final String manacost = k[1];
                    final Cost cost = new Cost(manacost, false);

                    StringBuilder sbCost = new StringBuilder(k[0]);
                    if (!cost.isOnlyManaCost()) {
                        sbCost.append("—");
                    } else {
                        sbCost.append(" ");
                    }
                    sbCost.append(cost.toSimpleString());
                    sbBefore.append(sbCost).append(" (").append(inst.getReminderText()).append(")");
                    sbBefore.append("\r\n");
                } else if (keyword.startsWith("Multikicker")) {
                    if (!keyword.endsWith("Generic")) {
                        final String[] n = keyword.split(":");
                        final Cost cost = new Cost(n[1], false);
                        sbBefore.append("Multikicker ").append(cost.toSimpleString()).append(" (").append(inst.getReminderText()).append(")").append("\r\n");
                    }
                } else if (keyword.startsWith("Kicker")) {
                    sbBefore.append(kickerDesc(keyword, inst.getReminderText())).append("\r\n");
                } else if (keyword.startsWith("AlternateAdditionalCost")) {
                    final String[] costs = keyword.split(":", 2)[1].split(":");
                    sbBefore.append("As an additional cost to cast this spell, ");
                    for (int n = 0; n < costs.length; n++) {
                        final Cost cost = new Cost(costs[n], false);
                        if (cost.isOnlyManaCost()) {
                            sbBefore.append(" pay ");
                        }
                        sbBefore.append(StringUtils.uncapitalize(cost.toSimpleString()));
                        sbBefore.append(n + 1 == costs.length ? ".\r\n\r\n" : n + 2 == costs.length && costs.length > 2
                                ? ", or " : n + 2 == costs.length ? " or " : ", ");
                    }
                } else if (keyword.startsWith("MayFlash")) {
                    // Pseudo keywords, only print Reminder
                    sbBefore.append(inst.getReminderText());
                    sbBefore.append("\r\n");
                } else if (keyword.startsWith("Entwine") || keyword.startsWith("Madness")
                        || keyword.startsWith("Miracle") || keyword.startsWith("Recover")
                        || keyword.startsWith("Escape") || keyword.startsWith("Foretell:")
                        || keyword.startsWith("Disturb")) {
                    final String[] k = keyword.split(":");
                    final Cost cost = new Cost(k[1], false);

                    StringBuilder sbCost = new StringBuilder(k[0]);
                    if (!cost.isOnlyManaCost()) {
                        sbCost.append("—");
                    } else {
                        sbCost.append(" ");
                    }
                    sbCost.append(cost.toSimpleString());
                    sbAfter.append(sbCost).append(" (").append(inst.getReminderText()).append(")");
                    sbAfter.append("\r\n");
                } else if (keyword.equals("CARDNAME can't be countered.") || keyword.equals("This spell can't be " +
                        "countered.") || keyword.equals("Remove CARDNAME from your deck before playing if you're not " +
                        "playing for ante.")) {
                    sbBefore.append(keyword);
                    sbBefore.append("\r\n");
                } else if (keyword.startsWith("Haunt")) {
                    sbAfter.append("Haunt (");
                    sbAfter.append("When this spell card is put into a graveyard after resolving, ");
                    sbAfter.append("exile it haunting target creature.");
                    sbAfter.append(")");
                    sbAfter.append("\r\n");
                } else if (keyword.startsWith("Splice")) {
                    final String[] n = keyword.split(":");
                    final Cost cost = new Cost(n[2], false);

                    String desc;

                    if (n.length > 3) {
                        desc = n[3];
                    } else {
                        String[] k = n[1].split(",");
                        for (int i = 0; i < k.length; i++) {
                            if (CardType.isACardType(k[i])) {
                                k[i] = k[i].toLowerCase();
                            }
                        }
                        desc = StringUtils.join(k, " or ");
                    }

                    sbAfter.append("Splice onto ").append(desc).append(" ").append(cost.toSimpleString());
                    sbAfter.append(" (").append(inst.getReminderText()).append(")").append("\r\n");
                } else if (keyword.equals("Storm")) {
                    sbAfter.append("Storm (");

                    sbAfter.append("When you cast this spell, copy it for each spell cast before it this turn.");

                    if (strSpell.contains("Target") || strSpell.contains("target")) {
                        sbAfter.append(" You may choose new targets for the copies.");
                    }

                    sbAfter.append(")");
                    sbAfter.append("\r\n");
                } else if (keyword.startsWith("Replicate")) {
                    final String[] n = keyword.split(":");
                    final Cost cost = new Cost(n[1], false);
                    sbBefore.append("Replicate ").append(cost.toSimpleString());
                    sbBefore.append(" (When you cast this spell, copy it for each time you paid its replicate cost.");
                    if (strSpell.contains("Target") || strSpell.contains("target")) {
                        sbBefore.append(" You may choose new targets for the copies.");
                    }
                    sbBefore.append(")\r\n");
                } else if (keyword.startsWith("DeckLimit")) {
                    final String[] k = keyword.split(":");
                    sbBefore.append(k[2]).append("\r\n");
                }
            } catch (Exception e) {
                String msg = "Card:abilityTextInstantSorcery: crash in Keyword parsing";

                Breadcrumb bread = new Breadcrumb(msg);
                bread.setData("Card", this.getName());
                bread.setData("Keyword", keyword);
                Sentry.addBreadcrumb(bread, this);

                throw new RuntimeException("Error in Card " + this.getName() + " with Keyword " + keyword, e);
            }
        }

        sb.append(CardTranslation.translateMultipleDescriptionText(sbBefore.toString(), state.getName()));

        // add Spells there to main StringBuilder
        sb.append(strSpell);

        // Triggered abilities
        for (final Trigger trig : state.getTriggers()) {
            if (!trig.isSecondary()) {
                sb.append(trig.replaceAbilityText(trig.toString(), state)).append("\r\n");
            }
        }

        // Replacement effects
        for (final ReplacementEffect replacementEffect : state.getReplacementEffects()) {
            if (!replacementEffect.isSecondary()) {
                sb.append(replacementEffect.getDescription()).append("\r\n");
            }
        }

        // static abilities
        for (final StaticAbility stAb : state.getStaticAbilities()) {
            if (!stAb.isSecondary()) {
                final String stAbD = stAb.toString();
                if (!stAbD.equals("")) {
                    sb.append(stAbD).append("\r\n");
                }
            }
        }

        sb.append(CardTranslation.translateMultipleDescriptionText(sbAfter.toString(), state.getName()));
        return sb;
    }

    private String formatSpellAbility(final SpellAbility sa) {
        final StringBuilder sb = new StringBuilder();
        sb.append(sa.toString()).append("\r\n\r\n");
        return sb.toString();
    }

    public final boolean canProduceColorMana(final Set<String> colors) {
        for (final SpellAbility mana : getManaAbilities()) {
            for (String s : colors) {
                if (mana.getApi() == ApiType.ManaReflected) {
                    if (CardUtil.getReflectableManaColors(mana).contains(s)) {
                        return true;
                    }
                } else {
                    if (mana.canProduce(MagicColor.toShortString(s))) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public final boolean canProduceSameManaTypeWith(final Card c) {
        if (getManaAbilities().isEmpty()) {
            return false;
        }
        Set<String> colors = new HashSet<>();
        for (final SpellAbility ab : c.getManaAbilities()) {
            if (ab.getApi() == ApiType.ManaReflected) {
                colors.addAll(CardUtil.getReflectableManaColors(ab));
            } else {
                colors = CardUtil.canProduce(6, ab, colors);
            }
        }
        return canProduceColorMana(colors);
    }

    public final void clearFirstSpell() {
        currentState.clearFirstSpell();
    }

    public final SpellAbility getFirstSpellAbility() {
        return Iterables.getFirst(currentState.getNonManaAbilities(), null);
    }

    /**
     * @return the first {@link SpellAbility} marked as a Spell with API type
     * {@link ApiType#Attach} in this {@link Card}, or {@code null} if no such
     * object exists.
     * @see SpellAbility#isSpell()
     */
    public final SpellAbility getFirstAttachSpell() {
        for (final SpellAbility sa : getSpells()) {
            if (sa.getApi() == ApiType.Attach && !sa.isSuppressed()) {
                return sa;
            }
        }
        return null;
    }

    public final SpellPermanent getSpellPermanent() {
        for (final SpellAbility sa : currentState.getNonManaAbilities()) {
            if (sa instanceof SpellPermanent) {
                return (SpellPermanent) sa;
            }
        }
        return null;
    }

    public final void addSpellAbility(final SpellAbility a) {
        addSpellAbility(a, true);
    }
    public final void addSpellAbility(final SpellAbility a, final boolean updateView) {
        a.setHostCard(this);
        if (currentState.addSpellAbility(a) && updateView) {
            currentState.getView().updateAbilityText(this, currentState);
        }
    }

    @Deprecated
    public final void removeSpellAbility(final SpellAbility a) {
        removeSpellAbility(a, true);
    }

    @Deprecated
    public final void removeSpellAbility(final SpellAbility a, final boolean updateView) {
        if (currentState.removeSpellAbility(a) && updateView) {
            currentState.getView().updateAbilityText(this, currentState);
        }
    }

    public final FCollectionView<SpellAbility> getSpellAbilities() {
        return currentState.getSpellAbilities();
    }
    public final FCollectionView<SpellAbility> getManaAbilities() {
        return currentState.getManaAbilities();
    }
    public final FCollectionView<SpellAbility> getNonManaAbilities() {
        return currentState.getNonManaAbilities();
    }

    public final boolean hasSpellAbility(final SpellAbility sa) {
        return currentState.hasSpellAbility(sa);
    }

    public final boolean hasSpellAbility(final int id) {
        return currentState.hasSpellAbility(id);
    }

    public boolean hasRemoveIntrinsic() {
        for (final CardChangedType ct : getChangedCardTypes()) {
            if (ct.isRemoveLandTypes()) {
                return true;
            }
        }
        return false;
    }

    public boolean hasNoAbilities() {
        if (!getUnhiddenKeywords().isEmpty()) {
            return false;
        }
        if (!getStaticAbilities().isEmpty()) {
            return false;
        }
        if (!getReplacementEffects().isEmpty()) {
            return false;
        }
        if (!getTriggers().isEmpty()) {
            return false;
        }
        for (SpellAbility sa : getSpellAbilities()) {
            if (!(sa instanceof SpellPermanent && sa.isBasicSpell()) && !sa.isMorphUp()) {
                return false;
            }
        }
        return true;
    }

    public void updateSpellAbilities(List<SpellAbility> list, CardState state, Boolean mana) {
        for (final CardTraitChanges ck : getChangedCardTraitsList(state)) {
            if (ck.isRemoveNonMana()) {
                // List only has nonMana
                if (null == mana) {
                    List<SpellAbility> toRemove = Lists.newArrayList(
                            Iterables.filter(list, Predicates.not(SpellAbilityPredicates.isManaAbility())));
                    list.removeAll(toRemove);
                } else if (false == mana) {
                    list.clear();
                }
            } else if (ck.isRemoveAll()) {
                list.clear();
            }
            list.removeAll(ck.getRemovedAbilities());
            for (SpellAbility sa : ck.getAbilities()) {
                if (mana == null || mana == sa.isManaAbility()) {
                    list.add(sa);
                }
            }
        }

        // add Facedown abilities from Original state but only if this state is face down
        // need CardStateView#getState or might crash in StackOverflow
        if (isInPlay()) {
            if ((null == mana || false == mana) && isFaceDown() && state.getView().getState() == CardStateName.FaceDown) {
                for (SpellAbility sa : getState(CardStateName.Original).getNonManaAbilities()) {
                    if (sa.isManifestUp() || sa.isMorphUp()) {
                        list.add(sa);
                    }
                }
            }
        } else {
            // Adenture may only be cast not from Battlefield
            if (isAdventureCard() && state.getView().getState() == CardStateName.Original) {
                for (SpellAbility sa : getState(CardStateName.Adventure).getSpellAbilities()) {
                    if (mana == null || mana == sa.isManaAbility()) {
                        list.add(sa);
                    }
                }
            }
        }

        // keywords should already been cleanup by layers
        for (KeywordInterface kw : getUnhiddenKeywords(state)) {
            for (SpellAbility sa : kw.getAbilities()) {
                if (mana == null || mana == sa.isManaAbility()) {
                    list.add(sa);
                }
            }
        }
    }

    private void updateBasicLandAbilities(List<SpellAbility> list, CardState state) {
        final CardTypeView type = state.getTypeWithChanges();

        if (!type.isLand()) {
            // no land, do nothing there
            return;
        }

        for (int i = 0; i < MagicColor.WUBRG.length; i++ ) {
            byte c = MagicColor.WUBRG[i];
            if (type.hasSubtype(MagicColor.Constant.BASIC_LANDS.get(i))) {
                SpellAbility sa = basicLandAbilities[i];

                // no Ability for this type yet, make a new one
                if (sa == null) {
                    sa = CardFactory.buildBasicLandAbility(state, c);
                    basicLandAbilities[i] = sa;
                }

                list.add(sa);
            }
        }
    }

    public final FCollectionView<SpellAbility> getAllSpellAbilities() {
        final FCollection<SpellAbility> res = new FCollection<>();
        for (final CardStateName key : states.keySet()) {
            res.addAll(getState(key).getNonManaAbilities());
            res.addAll(getState(key).getManaAbilities());
        }
        return res;
    }

    public final FCollectionView<SpellAbility> getSpells() {
        final FCollection<SpellAbility> res = new FCollection<>();
        for (final SpellAbility sa : currentState.getNonManaAbilities()) {
            if (sa.isSpell()) {
                res.add(sa);
            }
        }
        return res;
    }

    public final FCollectionView<SpellAbility> getBasicSpells() {
        return getBasicSpells(currentState);
    }
    public final FCollectionView<SpellAbility> getBasicSpells(CardState state) {
        final FCollection<SpellAbility> res = new FCollection<>();
        for (final SpellAbility sa : state.getNonManaAbilities()) {
            if (sa.isSpell() && sa.isBasicSpell()) {
                res.add(sa);
            }
        }
        return res;
    }

    // shield = regeneration
    public final int getShieldCount() {
        return shieldCount;
    }

    public final void incShieldCount() {
        shieldCount++;
        view.updateShieldCount(this);
    }

    public final void decShieldCount() {
        shieldCount--;
        view.updateShieldCount(this);
    }

    public final void resetShieldCount() {
        shieldCount = 0;
        view.updateShieldCount(this);
    }

    public final void addRegeneratedThisTurn() {
        regeneratedThisTurn++;
    }

    public final int getRegeneratedThisTurn() {
        return regeneratedThisTurn;
    }
    public final void setRegeneratedThisTurn(final int n) {
        regeneratedThisTurn = n;
    }

    public final boolean canBeShielded() {
        return !StaticAbilityCantRegenerate.cantRegenerate(this);
    }

    // is this "Card" supposed to be a token?
    public final boolean isToken() {
        if (isInPlay() && hasMergedCard()) {
            return getTopMergedCard().token;
        }
        return token;
    }
    public final boolean isRealToken() {
        return token;
    }
    public final void setToken(boolean token0) {
        if (token == token0) { return; }
        token = token0;
        view.updateToken(this);
    }
    public final void updateTokenView() {
        view.updateToken(this);
    }

    public final boolean isTokenCard() {
        if (isInPlay() && hasMergedCard()) {
            return getTopMergedCard().tokenCard;
        }
        return tokenCard;
    }
    public final void setTokenCard(boolean tokenC) {
        if (tokenCard == tokenC) { return; }
        tokenCard = tokenC;
        view.updateTokenCard(this);
    }

    public void updateWasDestroyed(boolean value) {
        view.updateWasDestroyed(value);
    }

    public final Card getCopiedPermanent() {
        return copiedPermanent;
    }
    public final void setCopiedPermanent(final Card c) {
        if (copiedPermanent == c) { return; }
        copiedPermanent = c;
        currentState.getView().updateOracleText(this);
    }

    public final boolean isCopiedSpell() {
        return copiedSpell;
    }
    public final void setCopiedSpell(final boolean b) {
        copiedSpell = b;
    }

    public final boolean isFaceDown() {
        if (hasMergedCard()) {
            return getTopMergedCard().facedown;
        }
        return facedown;
    }

    public final boolean isRealFaceDown() {
        return facedown;
    }

    public final void setFaceDown(boolean value) {
        facedown = value;
    }

    public final boolean isTransformed() {
        return getTransformedTimestamp() != 0;
    }

    public final boolean isFlipped() {
        return flipped;
    }
    public final void setFlipped(boolean value) {
        flipped = value;
    }

    public final void addLeavesPlayCommand(final GameCommand c) {
        leavePlayCommandList.add(c);
    }
    public final void addUntapCommand(final GameCommand c) {
        untapCommandList.add(c);
    }
    public final void addUnattachCommand(final GameCommand c) {
        unattachCommandList.add(c);
    }
    public final void addFaceupCommand(final GameCommand c) {
        faceupCommandList.add(c);
    }
    public final void addFacedownCommand(final GameCommand c) {
        facedownCommandList.add(c);
    }
    public final void addChangeControllerCommand(final GameCommand c) {
        changeControllerCommandList.add(c);
    }

    public final void runLeavesPlayCommands() {
        for (final GameCommand c : leavePlayCommandList) {
            c.run();
        }
        leavePlayCommandList.clear();
    }
    public final void runUntapCommands() {
        for (final GameCommand c : untapCommandList) {
            c.run();
        }
        untapCommandList.clear();
    }
    public final void runUnattachCommands() {
        for (final GameCommand c : unattachCommandList) {
            c.run();
        }
        unattachCommandList.clear();
    }
    public final void runFaceupCommands() {
        for (final GameCommand c : faceupCommandList) {
            c.run();
        }
        faceupCommandList.clear();
    }
    public final void runFacedownCommands() {
        for (final GameCommand c : facedownCommandList) {
            c.run();
        }
        facedownCommandList.clear();
    }
    public final void runChangeControllerCommands() {
        for (final GameCommand c : changeControllerCommandList) {
            c.run();
        }
        changeControllerCommandList.clear();
    }

    public final void setSickness(boolean sickness0) {
        if (sickness == sickness0) { return; }
        sickness = sickness0;
        view.updateSickness(this);
    }

    public final boolean hasSickness() {
        return sickness && !hasKeyword(Keyword.HASTE);
    }

    public final boolean isSick() {
        return hasSickness() && isCreature();
    }

    public final boolean isFirstTurnControlled() {
        return sickness;
    }

    public boolean hasBecomeTargetThisTurn() {
        return becameTargetThisTurn;
    }
    public void setBecameTargetThisTurn(boolean becameTargetThisTurn0) {
        becameTargetThisTurn = becameTargetThisTurn0;
    }

    public boolean hasStartedTheTurnUntapped() {
        return startedTheTurnUntapped;
    }
    public void setStartedTheTurnUntapped(boolean untapped) {
        startedTheTurnUntapped = untapped;
    }

    public boolean cameUnderControlSinceLastUpkeep() {
        return cameUnderControlSinceLastUpkeep;
    }
    public void setCameUnderControlSinceLastUpkeep(boolean underControlSinceLastUpkeep) {
        this.cameUnderControlSinceLastUpkeep = underControlSinceLastUpkeep;
    }

    public final Player getOwner() {
        return owner;
    }
    public final void setOwner(final Player owner0) {
        if (owner == owner0) { return; }
        if (owner != null && owner.getGame() != this.getGame()) {
            // Sanity check.
            throw new RuntimeException();
        }
        owner = owner0;
        view.updateOwner(this);
        view.updateController(this);
    }

    public final Player getController() {
        Entry<Long, Player> lastEntry = tempControllers.lastEntry();
        if (lastEntry != null) {
            final long lastTimestamp = lastEntry.getKey();
            if (lastTimestamp > controllerTimestamp) {
                return lastEntry.getValue();
            }
        }
        if (controller != null) {
            return controller;
        }
        return owner;
    }

    public final void setController(final Player player, final long tstamp) {
        tempControllers.clear();
        controller = player;
        controllerTimestamp = tstamp;
        view.updateController(this);
    }

    public final void addTempController(final Player player, final long tstamp) {
        tempControllers.put(tstamp, player);
        view.updateController(this);
    }

    public final void removeTempController(final long tstamp) {
        if (tempControllers.remove(tstamp) != null) {
            view.updateController(this);
        }
    }

    public final void removeTempController(final Player player) {
        boolean changed = false;
        // Remove each key that yields this player
        while (tempControllers.values().remove(player)) {
            changed = true;
        }
        if (changed) {
            view.updateController(this);
        }
    }

    public final void clearTempControllers() {
        if (tempControllers.isEmpty()) { return; }
        tempControllers.clear();
        view.updateController(this);
    }

    public final void clearControllers() {
        if (tempControllers.isEmpty() && controller == null) { return; }
        tempControllers.clear();
        controller = null;
        view.updateController(this);
    }

    public boolean mayPlayerLook(final Player player) {
        return view.mayPlayerLook(player.getView());
    }

    public final void addMayLookFaceDownExile(final Player p) {
        mayLookFaceDownExile.add(p);
        updateMayLook();
    }

    public final void addMayLookAt(final long timestamp, final Iterable<Player> list) {
        PlayerCollection plist = new PlayerCollection(list);
        mayLook.put(timestamp, plist);
        if (isFaceDown() && isInZone(ZoneType.Exile)) {
            mayLookFaceDownExile.addAll(plist);
        }
        updateMayLook();
    }

    public final void removeMayLookAt(final long timestamp) {
        if (mayLook.remove(timestamp) != null) {
            updateMayLook();
        }
    }

    public final void addMayLookTemp(final Player player) {
        if (mayLookTemp.add(player)) {
            if (isFaceDown() && isInZone(ZoneType.Exile)) {
                mayLookFaceDownExile.add(player);
            }
            updateMayLook();
        }
    }

    public final void removeMayLookTemp(final Player player) {
        if (mayLookTemp.remove(player)) {
            updateMayLook();
        }
    }

    public final void updateMayLook() {
        PlayerCollection result = new PlayerCollection();
        for (PlayerCollection v : mayLook.values()) {
            result.addAll(v);
        }
        result.addAll(mayLookFaceDownExile);
        result.addAll(mayLookTemp);
        getView().setPlayerMayLook(result);
    }

    public final void updateMayPlay() {
        PlayerCollection result = new PlayerCollection();
        for (CardPlayOption o : mayPlay.values()) {
            if (o.grantsZonePermissions())
                result.add(o.getPlayer());
        }
        getView().setMayPlayPlayers(result);
    }

    public final CardPlayOption mayPlay(final StaticAbility sta) {
        if (sta == null) {
            return null;
        }
        return mayPlay.get(sta);
    }

    public final List<CardPlayOption> mayPlay(final Player player) {
        List<CardPlayOption> result = Lists.newArrayList();
        for (CardPlayOption o : mayPlay.values()) {
            if (o.getPlayer().equals(player)) {
                result.add(o);
            }
        }
        return result;
    }
    public final void setMayPlay(final Player player, final boolean withoutManaCost, final Cost altManaCost, final boolean altIsAdditional, final boolean withFlash, final boolean grantZonePermissions, final StaticAbility sta) {
        this.mayPlay.put(sta, new CardPlayOption(player, sta, withoutManaCost, altManaCost, altIsAdditional, withFlash, grantZonePermissions));
        this.updateMayPlay();
    }
    public final void removeMayPlay(final StaticAbility sta) {
        this.mayPlay.remove(sta);
        this.updateMayPlay();
    }

    public void resetMayPlayTurn() {
        for (StaticAbility sta : getStaticAbilities()) {
            sta.resetMayPlayTurn();
        }
    }

    public final CardCollectionView getEquippedBy() {
        return CardLists.filter(getAttachedCards(), CardPredicates.Presets.EQUIPMENT);
    }

    public final boolean isEquipped() {
        return Iterables.any(getAttachedCards(), CardPredicates.Presets.EQUIPMENT);
    }
    public final boolean isEquippedBy(Card c) {
        return this.hasCardAttachment(c);
    }
    public final boolean isEquippedBy(final String cardName) {
        return this.hasCardAttachment(cardName);
    }

    public final CardCollectionView getFortifiedBy() {
        return CardLists.filter(getAttachedCards(), CardPredicates.Presets.FORTIFICATION);
    }

    public final boolean isFortified() {
        return Iterables.any(getAttachedCards(), CardPredicates.Presets.FORTIFICATION);
    }
    public final boolean isFortifiedBy(Card c) {
        // 301.5e + 301.6
        return hasCardAttachment(c);
    }

    public final boolean isFortifying() {
        return this.isAttachedToEntity();
    }

    public final Card getEquipping() {
        return this.getAttachedTo();
    }
    public final boolean isEquipping() {
        return this.isAttachedToEntity();
    }

    public final GameEntity getEntityAttachedTo() {
        return entityAttachedTo;
    }
    public final void setEntityAttachedTo(final GameEntity e) {
        if (entityAttachedTo == e) { return; }
        entityAttachedTo = e;
        view.updateAttachedTo(this);
    }
    public final void removeAttachedTo(final GameEntity e) {
        if (entityAttachedTo == e) {
            setEntityAttachedTo(null);
        }
    }
    public final boolean isAttachedToEntity() {
        return entityAttachedTo != null;
    }
    public final boolean isAttachedToEntity(final GameEntity e) {
        return (entityAttachedTo == e);
    }

    public final Card getAttachedTo() {
        if (entityAttachedTo instanceof Card) {
            return (Card) entityAttachedTo;
        }
        return null;
    }

    public final Card getEnchantingCard() {
        return getAttachedTo();
    }
    public final Player getPlayerAttachedTo() {
        if (entityAttachedTo instanceof Player) {
            return (Player) entityAttachedTo;
        }
        return null;
    }
    public final boolean isEnchanting() {
        return isAttachedToEntity();
    }
    public final boolean isEnchantingCard() {
        return getEnchantingCard() != null;
    }

    public final void attachToEntity(final GameEntity entity, SpellAbility sa) {
        attachToEntity(entity, sa, false);
    }
    public final void attachToEntity(final GameEntity entity, SpellAbility sa, boolean overwrite) {
        if (!overwrite && !entity.canBeAttached(this, sa)) {
            return;
        }

        GameEntity oldTarget = null;
        if (isAttachedToEntity()) {
            oldTarget = getEntityAttachedTo();
            // If attempting to reattach to the same object, don't do anything.
            if (oldTarget.equals(entity)) {
                return;
            }
            unattachFromEntity(oldTarget);
        }

        // They use double links... it's doubtful
        setEntityAttachedTo(entity);
        setTimestamp(getGame().getNextTimestamp());
        entity.addAttachedCard(this);

        // Play the Equip sound
        getGame().fireEvent(new GameEventCardAttachment(this, oldTarget, entity));

        // Run replacement effects
        final Map<AbilityKey, Object> repParams = AbilityKey.mapFromAffected(this);
        repParams.put(AbilityKey.AttachTarget, entity);
        getGame().getReplacementHandler().run(ReplacementType.Attached, repParams);

        // run trigger
        final Map<AbilityKey, Object> runParams = AbilityKey.newMap();
        runParams.put(AbilityKey.AttachSource, this);
        runParams.put(AbilityKey.AttachTarget, entity);
        getController().getGame().getTriggerHandler().runTrigger(TriggerType.Attached, runParams, false);

        if (hasKeyword(Keyword.RECONFIGURE)) {
            // need extra time stamp so it doesn't collide with existing ones
            long ts = getGame().getNextTimestamp();
            // 702.151b Attaching an Equipment with reconfigure to another creature causes the Equipment to stop being a creature until it becomes unattached from that creature.
            // it is not a Static Ability
            addChangedCardTypes(null, CardType.parse("Creature", true), false, EnumSet.noneOf(RemoveType.class), ts, 0, true, false);

            GameCommand unattach = new GameCommand() {
                private static final long serialVersionUID = 1L;

                @Override
                public void run() {
                    removeChangedCardTypes(ts, 0);
                }
            };
            addUnattachCommand(unattach);
        }
    }

    public final void unattachFromEntity(final GameEntity entity) {
        if (entityAttachedTo == null || !entityAttachedTo.equals(entity)) {
            return;
        }

        if (isPhasedOut()) {
            return;
        }

        setEntityAttachedTo(null);
        entity.removeAttachedCard(this);

        // Handle Bestowed Aura part
        unanimateBestow();

        getGame().fireEvent(new GameEventCardAttachment(this, entity, null));

        // Run triggers
        final Map<AbilityKey, Object> runParams = AbilityKey.newMap();
        runParams.put(AbilityKey.Attach, this);
        runParams.put(AbilityKey.Object, entity);
        getGame().getTriggerHandler().runTrigger(TriggerType.Unattach, runParams, false);
        runUnattachCommands();
    }

    public final boolean isModified() {
        if (!isCreature()) {
            return false;
        }
        if (this.isEquipped() || this.hasCounters()) {
            return true;
        }
        return Iterables.any(this.getEnchantedBy(), CardPredicates.isController(this.getController()));
    }

    public final void setType(final CardType type0) {
        currentState.setType(type0);
    }

    public final void addType(final String type0) {
        currentState.addType(type0);
    }
    public final void addType(final Iterable<String> type0) {
        currentState.addType(type0);
    }

    public final void removeType(final CardType.Supertype st) {
        currentState.removeType(st);
    }

    public final void setCreatureTypes(Collection<String> ctypes) {
        currentState.setCreatureTypes(ctypes);
    }

    public final CardTypeView getType() {
        return getType(currentState);
    }
    public final CardTypeView getType(CardState state) {
        final Iterable<CardChangedType> changedCardTypes = getChangedCardTypes();
        if (Iterables.isEmpty(changedCardTypes)) {
            return state.getType();
        }
        // CR 506.4 attacked planeswalkers leave combat
        boolean checkCombat = state.getType().isPlaneswalker() && game.getCombat() != null && !game.getCombat().getAttackersOf(this).isEmpty();
        CardTypeView types = state.getType().getTypeWithChanges(changedCardTypes);
        if (checkCombat && !types.isPlaneswalker()) {
            game.getCombat().removeFromCombat(this);
        }
        return types;
    }

    public final CardTypeView getOriginalType() {
        return getOriginalType(currentState);
    }
    public final CardTypeView getOriginalType(CardState state) {
        return state.getType();
    }

    // TODO add changed type by card text
    public Iterable<CardChangedType> getChangedCardTypes() {
        // If there are no changed types, just return an empty immutable list, which actually
        // produces a surprisingly large speedup by avoid lots of temp objects and making iteration
        // over the result much faster. (This function gets called a lot!)
        if (changedCardTypesByText.isEmpty() && changedTypeByText == null && changedCardTypesCharacterDefining.isEmpty() && changedCardTypes.isEmpty()) {
            return ImmutableList.of();
        }
        Iterable<CardChangedType> byText = changedTypeByText == null ? ImmutableList.of() : ImmutableList.of(this.changedTypeByText);
        return Iterables.unmodifiableIterable(Iterables.concat(
                changedCardTypesByText.values(), // Layer 3
                byText, // Layer 3 by Word Changes,
                changedCardTypesCharacterDefining.values(), // Layer 4
                changedCardTypes.values() // Layer 6
                ));
    }

    public Table<Long, Long, CardChangedType> getChangedCardTypesTable() {
        return Tables.unmodifiableTable(changedCardTypes);
    }
    public Table<Long, Long, CardChangedType> getChangedCardTypesCharacterDefiningTable() {
        return Tables.unmodifiableTable(changedCardTypesCharacterDefining);
    }

    public boolean clearChangedCardTypes() {
        boolean changed = false;

        if (changedTypeByText != null)
            changed = true;
        changedTypeByText = null;

        if (!changedCardTypesByText.isEmpty())
            changed = true;
        changedCardTypesByText.clear();

        if (!changedCardTypesCharacterDefining.isEmpty())
            changed = true;
        changedCardTypesCharacterDefining.clear();

        if (!changedCardTypes.isEmpty())
            changed = true;
        changedCardTypes.clear();

        return changed;
    }

    public boolean clearChangedCardColors() {
        boolean changed = false;

        if (!changedCardColorsByText.isEmpty())
            changed = true;
        changedCardColorsByText.clear();

        if (!changedCardTypesCharacterDefining.isEmpty())
            changed = true;
        changedCardTypesCharacterDefining.clear();

        if (!changedCardColors.isEmpty())
            changed = true;
        changedCardColors.clear();

        return changed;
    }

    public Table<Long, Long, KeywordsChange> getChangedCardKeywordsByText() {
        return changedCardKeywordsByText;
    }

    public Iterable<KeywordsChange> getChangedCardKeywordsList() {
        return Iterables.concat(
            changedCardKeywordsByText.values(), // Layer 3
            ImmutableList.of(new KeywordsChange(ImmutableList.<KeywordInterface>of(), ImmutableList.<KeywordInterface>of(), this.hasRemoveIntrinsic())), // Layer 4
            changedCardKeywords.values() // Layer 6
        );
    }

    public Table<Long, Long, KeywordsChange> getChangedCardKeywords() {
        return changedCardKeywords;
    }

    public Table<Long, Long, CardColor> getChangedCardColorsTable() {
        return changedCardColors;
    }
    public Table<Long, Long, CardColor> getChangedCardColorsCharacterDefiningTable() {
        return changedCardColorsCharacterDefining;
    }
    public Iterable<CardColor> getChangedCardColors() {
        return Iterables.concat(changedCardColorsByText.values(), changedCardColorsCharacterDefining.values(), changedCardColors.values());
    }

    public final void addChangedCardTypesByText(final CardType addType, final long timestamp, final long staticId) {
        addChangedCardTypesByText(addType, timestamp, staticId, true);
    }

    public final void addChangedCardTypesByText(final CardType addType, final long timestamp, final long staticId, final boolean updateView) {
        changedCardTypesByText.put(timestamp, staticId, new CardChangedType(addType, null, false,
                EnumSet.of(RemoveType.SuperTypes,
                        RemoveType.CardTypes,
                        RemoveType.SubTypes)));

        // setting card type via text, does overwrite any other word change effects?
        this.changedTextColors.addEmpty(timestamp, staticId);
        this.changedTextTypes.addEmpty(timestamp, staticId);

        if (updateView) {
            updateTypesForView();
        }
    }

    public final void addChangedCardTypes(final CardType addType, final CardType removeType, final boolean addAllCreatureTypes,
            final Set<RemoveType> remove,
            final long timestamp, final long staticId, final boolean updateView, final boolean cda) {
        (cda ? changedCardTypesCharacterDefining : changedCardTypes).put(timestamp, staticId, new CardChangedType(
                addType, removeType, addAllCreatureTypes, remove));
        if (updateView) {
            updateTypesForView();
        }
    }

    public final void addChangedCardTypes(final Iterable<String> types, final Iterable<String> removeTypes, final boolean addAllCreatureTypes,
            final Set<RemoveType> remove,
            final long timestamp, final long staticId, final boolean updateView, final boolean cda) {
        CardType addType = null;
        CardType removeType = null;
        if (types != null) {
            addType = new CardType(types, true);
        }

        if (removeTypes != null) {
            removeType = new CardType(removeTypes, true);
        }

        addChangedCardTypes(addType, removeType, addAllCreatureTypes, remove, timestamp, staticId, updateView, cda);
    }

    public final void removeChangedCardTypes(final long timestamp, final long staticId) {
        removeChangedCardTypes(timestamp, staticId, true);
    }
    public final void removeChangedCardTypes(final long timestamp, final long staticId, final boolean updateView) {
        boolean removed = false;
        removed |= changedCardTypes.remove(timestamp, staticId) != null;
        removed |= changedCardTypesCharacterDefining.remove(timestamp, staticId) != null;
        if (removed && updateView) {
            updateTypesForView();
        }
    }

    public void addColorByText(final ColorSet color, final long timestamp, final long staticId) {
        changedCardColorsByText.put(timestamp, staticId, new CardColor(color, false));
        updateColorForView();
    }

    public final void addColor(final ColorSet color, final boolean addToColors, final long timestamp, final long staticId, final boolean cda) {
        (cda ? changedCardColorsCharacterDefining : changedCardColors).put(timestamp, staticId, new CardColor(color, addToColors));
        updateColorForView();
    }

    public final void removeColor(final long timestampIn, final long staticId) {
        boolean removed = false;
        removed |= changedCardColorsByText.remove(timestampIn, staticId) != null;
        removed |= changedCardColors.remove(timestampIn, staticId) != null;
        removed |= changedCardColorsCharacterDefining.remove(timestampIn, staticId) != null;

        if (removed) {
            updateColorForView();
        }
    }

    public final void updateColorForView() {
        currentState.getView().updateColors(this);
        currentState.getView().updateHasChangeColors(!Iterables.isEmpty(getChangedCardColors()));
    }

    public final void setColor(final String... color) {
        setColor(ColorSet.fromNames(color).getColor());
    }
    public final void setColor(final byte color) {
        currentState.setColor(color);
    }

    public final ColorSet getColor() {
        return getColor(currentState);
    }
    public final ColorSet getColor(CardState state) {
        byte colors = state.getColor();
        for (final CardColor cc : getChangedCardColors()) {
            if (cc.isAdditional()) {
                colors |= cc.getColorMask();
            } else {
                colors = cc.getColorMask();
            }
        }
        return ColorSet.fromMask(colors);
    }

    public final int getCurrentLoyalty() {
        return getCounters(CounterEnumType.LOYALTY);
    }

    public final void setBaseLoyalty(final int n) {
        currentState.setBaseLoyalty(Integer.toString(n));
    }

    public final int getCurrentDefense() {
        return getCounters(CounterEnumType.DEFENSE);
    }
    public final void setBaseDefense(final int n) {
        currentState.setBaseDefense(Integer.toString(n));
    }
    public final int getBasePower() {
        return currentState.getBasePower();
    }
    public final int getBaseToughness() {
        return currentState.getBaseToughness();
    }

    public final void setBasePower(final int n) {
        currentState.setBasePower(n);
    }
    public final void setBaseToughness(final int n) {
        currentState.setBaseToughness(n);
    }

    // values that are printed on card
    public final String getBasePowerString() {
        return (null == currentState.getBasePowerString()) ? String.valueOf(getBasePower()) : currentState.getBasePowerString();
    }
    public final String getBaseToughnessString() {
        return (null == currentState.getBaseToughnessString()) ? String.valueOf(getBaseToughness()) : currentState.getBaseToughnessString();
    }

    // values that are printed on card
    public final void setBasePowerString(final String s) {
        currentState.setBasePowerString(s);
    }
    public final void setBaseToughnessString(final String s) {
        currentState.setBaseToughnessString(s);
    }

    public final void addCloneState(CardCloneStates states, final long timestamp) {
        clonedStates.put(timestamp, states);
        updateCloneState(true);
    }

    public final boolean removeCloneState(final long timestamp) {
        if (clonedStates.remove(timestamp) != null) {
            updateCloneState(true);
            return true;
        }
        return false;
    }

    public final boolean removeCloneState(final CardTraitBase ctb) {
        boolean changed = false;
        List<Long> toRemove = Lists.newArrayList();
        for (final Entry<Long, CardCloneStates> e : clonedStates.entrySet()) {
            if (ctb.equals(e.getValue().getSource())) {
                toRemove.add(e.getKey());
                changed = true;
            }
        }
        for (final Long l : toRemove) {
            clonedStates.remove(l);
        }
        if (changed) {
            updateCloneState(true);
        }

        return changed;
    }

    public final Card getCloner() {
        CardCloneStates clStates = getLastClonedState();
        if (!isCloned() || clStates == null) {
            return null;
        }
        return clStates.getHost();
    }

    public final boolean removeCloneStates() {
        if (clonedStates.isEmpty()) {
            return false;
        }
        clonedStates.clear();
        updateCloneState(false);
        return true;
    }

    public final Map<Long, CardCloneStates> getCloneStates() {
        return clonedStates;
    }

    public final void setCloneStates(Map<Long, CardCloneStates> val) {
        clonedStates.clear();
        clonedStates.putAll(val);
        updateCloneState(true);
    }

    private final void updateCloneState(final boolean updateView) {
        if (isFaceDown()) {
            setState(CardStateName.FaceDown, updateView, true);
        } else {
            setState(getFaceupCardStateName(), updateView, true);
        }
        updateChangedText();
    }

    public final CardStateName getFaceupCardStateName() {
        if (isFlipped() && hasState(CardStateName.Flipped)) {
            return CardStateName.Flipped;
        } else if (isSpecialized()) {
            return getCurrentStateName();
        } else if (backside && isDoubleFaced()) {
            CardStateName stateName = getRules().getSplitType().getChangedStateName();
            if (hasState(stateName)) {
                return stateName;
            }
        }
        return CardStateName.Original;
    }

    private final CardCloneStates getLastClonedState() {
        if (clonedStates.isEmpty()) {
            return null;
        }
        return clonedStates.lastEntry().getValue();
    }

    public final Table<Long, Long, Pair<Integer, Integer>> getSetPTTable() {
        return newPT;
    }

    public final void setPTTable(Table<Long, Long, Pair<Integer, Integer>> table) {
        newPT.clear();
        newPT.putAll(table);
    }

    public final Table<Long, Long, Pair<Integer, Integer>> getSetPTCharacterDefiningTable() {
        return newPTCharacterDefining;
    }

    public final void setPTCharacterDefiningTable(Table<Long, Long, Pair<Integer, Integer>> table) {
        newPTCharacterDefining.clear();
        newPTCharacterDefining.putAll(table);
    }

    public final void addNewPTByText(final Integer power, final Integer toughness, final long timestamp, final long staticId) {
        newPTText.put(timestamp, staticId, Pair.of(power, toughness));
        updatePTforView();
    }

    public final void addNewPT(final Integer power, final Integer toughness, final long timestamp, final long staticId) {
        addNewPT(power, toughness, timestamp, staticId, false);
    }
    public final void addNewPT(final Integer power, final Integer toughness, final long timestamp, final long staticId, final boolean cda) {
        (cda ? newPTCharacterDefining : newPT).put(timestamp, staticId, Pair.of(power, toughness));
        updatePTforView();
    }

    public final void removeNewPT(final long timestamp, final long staticId) {
        boolean removed = false;

        removed |= newPTText.remove(timestamp, staticId) != null;
        removed |= newPT.remove(timestamp, staticId) != null;
        removed |= newPTCharacterDefining.remove(timestamp, staticId) != null;

        if (removed) {
            updatePTforView();
        }
    }

    public void updatePTforView() {
        getView().updateLethalDamage(this);
        currentState.getView().updatePower(this);
        currentState.getView().updateToughness(this);
    }

    public Iterable<Pair<Integer, Integer>> getPTIterable() {
        return Iterables.concat(this.newPTText.values(), this.newPTCharacterDefining.values(), this.newPT.values());
    }

    public final boolean clearNewPT() {
        boolean changed = false;
        if (!newPTText.isEmpty()) {
            changed = true;
            newPTText.clear();
        }
        if (!newPTCharacterDefining.isEmpty()) {
            changed = true;
            newPTCharacterDefining.clear();
        }
        if (!newPT.isEmpty()) {
            changed = true;
            newPT.clear();
        }
        return changed;
    }

    public final int getCurrentPower() {
        int total = getBasePower();
        for (Pair<Integer, Integer> p : getPTIterable()) {
            if (p.getLeft() != null) {
                total = p.getLeft();
            }
        }
        return total;
    }

    public final StatBreakdown getUnswitchedPowerBreakdown() {
        // 208.3 A noncreature permanent has no power or toughness
        if (isInPlay() && !isCreature()) {
            return new StatBreakdown();
        }
        return new StatBreakdown(getCurrentPower(), getTempPowerBoost(), getPowerBonusFromCounters());
    }
    public final int getUnswitchedPower() {
        return getUnswitchedPowerBreakdown().getTotal();
    }

    public final int getPowerBonusFromCounters() {
        return getCounters(CounterEnumType.P1P1) + getCounters(CounterEnumType.P1P2) + getCounters(CounterEnumType.P1P0)
                - getCounters(CounterEnumType.M1M1) + 2 * getCounters(CounterEnumType.P2P2) - 2 * getCounters(CounterEnumType.M2M1)
                - 2 * getCounters(CounterEnumType.M2M2) - getCounters(CounterEnumType.M1M0) + 2 * getCounters(CounterEnumType.P2P0);
    }

    public final StatBreakdown getNetPowerBreakdown() {
        if (getAmountOfKeyword("CARDNAME's power and toughness are switched") % 2 != 0) {
            return getUnswitchedToughnessBreakdown();
        }
        return getUnswitchedPowerBreakdown();
    }
    public final int getNetPower() {
        if (getAmountOfKeyword("CARDNAME's power and toughness are switched") % 2 != 0) {
            return getUnswitchedToughness();
        }
        return getUnswitchedPower();
    }

    public final int getCurrentToughness() {
        int total = getBaseToughness();
        for (Pair<Integer, Integer> p : getPTIterable()) {
            if (p.getRight() != null) {
                total = p.getRight();
            }
        }
        return total;
    }

    public static class StatBreakdown {
        public final int currentValue;
        public final int tempBoost;
        public final int bonusFromCounters;
        public StatBreakdown() {
            this.currentValue = 0;
            this.tempBoost = 0;
            this.bonusFromCounters = 0;
        }
        public StatBreakdown(int currentValue, int tempBoost, int bonusFromCounters) {
            this.currentValue = currentValue;
            this.tempBoost = tempBoost;
            this.bonusFromCounters = bonusFromCounters;
        }
        public int getTotal() {
            return currentValue + tempBoost + bonusFromCounters;
        }
        @Override
        public String toString() {
            return TextUtil.concatWithSpace("c:"+ currentValue,"tb:"+ tempBoost,"bfc:"+ bonusFromCounters);
        }
    }

    public final StatBreakdown getUnswitchedToughnessBreakdown() {
        // 208.3 A noncreature permanent has no power or toughness
        if (isInPlay() && !isCreature()) {
            return new StatBreakdown();
        }
        return new StatBreakdown(getCurrentToughness(), getTempToughnessBoost(), getToughnessBonusFromCounters());
    }
    public final int getUnswitchedToughness() {
        return getUnswitchedToughnessBreakdown().getTotal();
    }

    public final int getToughnessBonusFromCounters() {
        return getCounters(CounterEnumType.P1P1) + 2 * getCounters(CounterEnumType.P1P2) - getCounters(CounterEnumType.M1M1)
                + getCounters(CounterEnumType.P0P1) - 2 * getCounters(CounterEnumType.M0M2) + 2 * getCounters(CounterEnumType.P2P2)
                - getCounters(CounterEnumType.M0M1) - getCounters(CounterEnumType.M2M1) - 2 * getCounters(CounterEnumType.M2M2)
                + 2 * getCounters(CounterEnumType.P0P2);
    }

    public final StatBreakdown getNetToughnessBreakdown() {
        if (getAmountOfKeyword("CARDNAME's power and toughness are switched") % 2 != 0) {
            return getUnswitchedPowerBreakdown();
        }
        return getUnswitchedToughnessBreakdown();
    }
    public final int getNetToughness() {
        return getNetToughnessBreakdown().getTotal();
    }

    public final boolean toughnessAssignsDamage() {
        return StaticAbilityCombatDamageToughness.combatDamageToughness(this);
    }

    // How much combat damage does the card deal
    public final StatBreakdown getNetCombatDamageBreakdown() {
        if (hasKeyword("CARDNAME assigns no combat damage")) {
            return new StatBreakdown();
        }

        if (toughnessAssignsDamage()) {
            return getNetToughnessBreakdown();
        }
        return getNetPowerBreakdown();
    }
    public final int getNetCombatDamage() {
        return getNetCombatDamageBreakdown().getTotal();
    }

    private int intensity = 0;
    public final void addIntensity(final int n) {
        intensity = intensity + n;
        view.updateIntensity(this);
    }
    public final int getIntensity(boolean total) {
        if (total && hasKeyword(Keyword.STARTING_INTENSITY)) {
            return getKeywordMagnitude(Keyword.STARTING_INTENSITY) + intensity;
        } else {
            return intensity;
        }
    }
    public final void setIntensity(final int n) { intensity = n; }
    public final boolean hasIntensity() {
        return intensity > 0;
    }

    private int multiKickerMagnitude = 0;
    public final void addMultiKickerMagnitude(final int n) { multiKickerMagnitude += n; }
    public final void setKickerMagnitude(final int n) { multiKickerMagnitude = n; }
    public final int getKickerMagnitude() {
        if (multiKickerMagnitude > 0) {
            return multiKickerMagnitude;
        }
        boolean hasK1 = isOptionalCostPaid(OptionalCost.Kicker1);
        return hasK1 == isOptionalCostPaid(OptionalCost.Kicker2) ? (hasK1 ? 2 : 0) : 1;
    }

    private int pseudoKickerMagnitude = 0;
    public final void addPseudoMultiKickerMagnitude(final int n) { pseudoKickerMagnitude += n; }
    public final void setPseudoMultiKickerMagnitude(final int n) { pseudoKickerMagnitude = n; }
    public final int getPseudoKickerMagnitude() { return pseudoKickerMagnitude; }

    // for cards like Giant Growth, etc.
    public final int getTempPowerBoost() {
        int result = 0;
        for (Pair<Integer, Integer> pair : boostPT.values()) {
            if (pair.getLeft() != null) {
                result += pair.getLeft();
            }
        }
        return result;
    }

    public final int getTempToughnessBoost() {
        int result = 0;
        for (Pair<Integer, Integer> pair : boostPT.values()) {
            if (pair.getRight() != null) {
                result += pair.getRight();
            }
        }
        return result;
    }

    public void addPTBoost(final Integer power, final Integer toughness, final long timestamp, final long staticId) {
        boostPT.put(timestamp, staticId, Pair.of(power, toughness));
    }

    public void removePTBoost(final long timestamp, final long staticId) {
        boostPT.remove(timestamp, staticId);
    }

    public Table<Long, Long, Pair<Integer, Integer>> getPTBoostTable() {
        return ImmutableTable.copyOf(boostPT);
    }

    public void setPTBoost(Table<Long, Long, Pair<Integer, Integer>> table) {
        this.boostPT.clear();
        boostPT.putAll(table);
    }

    public final boolean isUntapped() {
        return !tapped;
    }

    public final boolean isTapped() {
        return tapped;
    }
    public final void setTapped(boolean tapped0) {
        if (tapped == tapped0) { return; }
        tapped = tapped0;
        view.updateTapped(this);
    }

    public final void tap(boolean tapAnimation, SpellAbility cause, Player tapper) {
        tap(false, tapAnimation, cause, tapper);
    }
    public final void tap(boolean attacker, boolean tapAnimation, SpellAbility cause, Player tapper) {
        if (tapped) { return; }

        // Run replacement effects
        getGame().getReplacementHandler().run(ReplacementType.Tap, AbilityKey.mapFromAffected(this));

        // Run triggers
        final Map<AbilityKey, Object> runParams = AbilityKey.mapFromCard(this);
        runParams.put(AbilityKey.Attacker, attacker);
        runParams.put(AbilityKey.Cause, cause);
        runParams.put(AbilityKey.Player, tapper);
        getGame().getTriggerHandler().runTrigger(TriggerType.Taps, runParams, false);

        setTapped(true);
        view.updateNeedsTapAnimation(tapAnimation);
        getGame().fireEvent(new GameEventCardTapped(this, true));
    }

    public final void untap(boolean untapAnimation) {
        if (!tapped) { return; }

        // Run Replacement effects
        if (getGame().getReplacementHandler().run(ReplacementType.Untap, AbilityKey.mapFromAffected(this)) != ReplacementResult.NotReplaced) {
            return;
        }

        // Run triggers
        getGame().getTriggerHandler().runTrigger(TriggerType.Untaps, AbilityKey.mapFromCard(this), false);

        runUntapCommands();
        setTapped(false);
        view.updateNeedsUntapAnimation(untapAnimation);
        getGame().fireEvent(new GameEventCardTapped(this, false));
    }

    public final Table<Long, Long, CardTraitChanges> getChangedCardTraitsByText() {
        return changedCardTraitsByText;
    }
    public final void setChangedCardTraitsByText(Table<Long, Long, CardTraitChanges> changes) {
        changedCardTraitsByText.clear();
        for (Table.Cell<Long, Long, CardTraitChanges> e : changes.cellSet()) {
            changedCardTraitsByText.put(e.getRowKey(), e.getColumnKey(), e.getValue().copy(this, true));
        }
    }
    public final void addChangedCardTraitsByText(Collection<SpellAbility> spells,
            Collection<Trigger> trigger, Collection<ReplacementEffect> replacements, Collection<StaticAbility> statics, long timestamp, long staticId) {
        changedCardTraitsByText.put(timestamp, staticId, new CardTraitChanges(
            spells, null, trigger, replacements, statics, true, false
        ));
        // update view
        updateAbilityTextForView();
    }

    public final SpellAbility getSpellAbilityForStaticAbility(final String str, final StaticAbility stAb) {
        SpellAbility result = storedSpellAbilility.get(stAb, str);
        if (result == null) {
            result = AbilityFactory.getAbility(str, this, stAb);
            result.setIntrinsic(false);
            result.setGrantorStatic(stAb);
            storedSpellAbilility.put(stAb, str, result);
        }
        return result;
    }

    public final Trigger getTriggerForStaticAbility(final String str, final StaticAbility stAb) {
        Trigger result = storedTrigger.get(stAb, str);
        if (result == null) {
            result = TriggerHandler.parseTrigger(str, this, false, stAb);
            storedTrigger.put(stAb, str, result);
        }
        return result;
    }

    public void setStoredReplacements(Table<StaticAbility, String, ReplacementEffect> table) {
        storedReplacementEffect.clear();
        for (Table.Cell<StaticAbility, String, ReplacementEffect> c : table.cellSet()) {
            storedReplacementEffect.put(c.getRowKey(), c.getColumnKey(), c.getValue().copy(this, true));
        }
    }

    public final Table<StaticAbility, String, ReplacementEffect> getStoredReplacements() {
        return storedReplacementEffect;
    }

    public final ReplacementEffect getReplacementEffectForStaticAbility(final String str, final StaticAbility stAb) {
        ReplacementEffect result = storedReplacementEffect.get(stAb, str);
        if (result == null) {
            result = ReplacementHandler.parseReplacement(str, this, false, stAb);
            storedReplacementEffect.put(stAb, str, result);
        }
        return result;
    }

    public final StaticAbility getStaticAbilityForStaticAbility(final String str, final StaticAbility stAb) {
        StaticAbility result = storedStaticAbility.get(stAb, str);
        if (result == null) {
            result = StaticAbility.create(str, this, stAb.getCardState(), false);
            storedStaticAbility.put(stAb, str, result);
        }
        return result;
    }

    public final SpellAbility getSpellAbilityForStaticAbilityByText(final SpellAbility sa, final StaticAbility stAb) {
        SpellAbility result = storedSpellAbililityByText.get(stAb, sa);
        if (result == null) {
            result = sa.copy(this, false);
            result.setOriginalAbility(sa); // need to be set to get the Once Per turn Clause correct
            result.setGrantorStatic(stAb);
            result.setIntrinsic(true); // needs to be changed by CardTextChanges
            storedSpellAbililityByText.put(stAb, sa, result);
        }
        return result;
    }

    public final SpellAbility getSpellAbilityForStaticAbilityGainedByText(final String str, final StaticAbility stAb) {
        SpellAbility result = storedSpellAbililityGainedByText.get(stAb, str);
        if (result == null) {
            result = AbilityFactory.getAbility(str, this, stAb);
            result.setIntrinsic(true); // needs to be affected by Text
            result.setGrantorStatic(stAb);
            storedSpellAbililityGainedByText.put(stAb, str, result);
        }
        return result;
    }

    public final Trigger getTriggerForStaticAbilityByText(final Trigger tr, final StaticAbility stAb) {
        Trigger result = storedTriggerByText.get(stAb, tr);
        if (result == null) {
            result = tr.copy(this, false);
            result.setIntrinsic(true); // needs to be changed by CardTextChanges
            storedTriggerByText.put(stAb, tr, result);
        }
        return result;
    }

    public final ReplacementEffect getReplacementEffectForStaticAbilityByText(final ReplacementEffect re, final StaticAbility stAb) {
        ReplacementEffect result = storedReplacementEffectByText.get(stAb, re);
        if (result == null) {
            result = re.copy(this, false);
            result.setIntrinsic(true); // needs to be changed by CardTextChanges
            storedReplacementEffectByText.put(stAb, re, result);
        }
        return result;
    }

    public final StaticAbility getStaticAbilityForStaticAbilityByText(final StaticAbility st, final StaticAbility stAb) {
        StaticAbility result = storedStaticAbilityByText.get(stAb, st);
        if (result == null) {
            result = st.copy(this, false);
            result.setIntrinsic(true); // needs to be changed by CardTextChanges
            storedStaticAbilityByText.put(stAb, st, result);
        }
        return result;
    }

    public final KeywordInterface getKeywordForStaticAbilityByText(final KeywordInterface ki, final StaticAbility stAb, long idx) {
        Triple<String, Long, Long> triple = Triple.of(ki.getOriginal(), (long)stAb.getId(), idx);
        KeywordInterface result = storedKeywordByText.get(triple);
        if (result == null) {
            result = ki.copy(this, false);
            result.setStaticId(stAb.getId());
            result.setIdx(idx);
            result.setIntrinsic(true);
            storedKeywordByText.put(triple, result);
        }
        return result;
    }

    public final void addChangedCardTraits(Collection<SpellAbility> spells, Collection<SpellAbility> removedAbilities,
            Collection<Trigger> trigger, Collection<ReplacementEffect> replacements, Collection<StaticAbility> statics,
            boolean removeAll, boolean removeNonMana, long timestamp, long staticId) {
        changedCardTraits.put(timestamp, staticId, new CardTraitChanges(
            spells, removedAbilities, trigger, replacements, statics, removeAll, removeNonMana
        ));
        // update view
        updateAbilityTextForView();
    }

    public final boolean removeChangedCardTraits(long timestamp, long staticId) {
        boolean changed = false;
        changed |= changedCardTraitsByText.remove(timestamp, staticId) != null;
        changed |= changedCardTraits.remove(timestamp, staticId) != null;
        return changed;
    }

    public Iterable<CardTraitChanges> getChangedCardTraitsList(CardState state) {
        List<SpellAbility> landManaAbilities = Lists.newArrayList();
        this.updateBasicLandAbilities(landManaAbilities, state);

        return Iterables.concat(
            changedCardTraitsByText.values(), // Layer 3
            ImmutableList.of(new CardTraitChanges(landManaAbilities, null, null, null, null, hasRemoveIntrinsic(), false)), // Layer 4
            changedCardTraits.values() // Layer 6
        );
    }

    public final Table<Long, Long, CardTraitChanges> getChangedCardTraits() {
        return changedCardTraits;
    }

    public final void setChangedCardTraits(Table<Long, Long, CardTraitChanges> changes) {
        changedCardTraits.clear();
        for (Table.Cell<Long, Long, CardTraitChanges> e : changes.cellSet()) {
            changedCardTraits.put(e.getRowKey(), e.getColumnKey(), e.getValue().copy(this, true));
        }
    }

    public boolean clearChangedCardTraits() {
        boolean changed = false;
        if (changedCardTraitsByText.isEmpty()) {
            changed = true;
        }
        changedCardTraitsByText.clear();
        if (changedCardTraits.isEmpty()) {
            changed = true;
        }
        changedCardTraits.clear();
        return changed;
    }

    // keywords are like flying, fear, first strike, etc...
    public final List<KeywordInterface> getKeywords() {
        return getKeywords(currentState);
    }
    public final List<KeywordInterface> getKeywords(CardState state) {
        ListKeywordVisitor visitor = new ListKeywordVisitor();
        visitKeywords(state, visitor);
        return visitor.getKeywords();
    }
    // Allows traversing the card's keywords without needing to concat a bunch
    // of lists. Optimizes common operations such as hasKeyword().
    public final void visitKeywords(CardState state, Visitor<KeywordInterface> visitor) {
        visitUnhiddenKeywords(state, visitor);
    }

    @Override
    public final boolean hasKeyword(Keyword keyword) {
        return hasKeyword(keyword, currentState);
    }
    public final boolean hasKeyword(Keyword key, CardState state) {
        return state.hasKeyword(key);
    }

    @Override
    public final boolean hasKeyword(String keyword) {
        return hasKeyword(keyword, currentState);
    }
    public final boolean hasKeyword(String keyword, CardState state) {
        if (keyword.startsWith("HIDDEN")) {
            keyword = keyword.substring(7);
        }

        // shortcut for hidden keywords
        for (List<String> kw : this.hiddenExtrinsicKeywords.values()) {
            if (kw.contains(keyword)) {
                return true;
            }
        }

        HasKeywordVisitor visitor = new HasKeywordVisitor(keyword, false);
        visitKeywords(state, visitor);
        return visitor.getResult();
    }

    public final void updateKeywords() {
        getCurrentState().getView().updateKeywords(this, getCurrentState());
        getView().updateLethalDamage(this);
    }

    public final void addChangedCardKeywords(final List<String> keywords, final List<String> removeKeywords,
            final boolean removeAllKeywords, final long timestamp, final long staticId) {
        addChangedCardKeywords(keywords, removeKeywords, removeAllKeywords, timestamp, staticId, true);
    }
    public final void addChangedCardKeywords(final List<String> keywords, final List<String> removeKeywords,
            final boolean removeAllKeywords, final long timestamp, final long staticId, final boolean updateView) {
        List<KeywordInterface> kws = Lists.newArrayList();
        if (keywords != null) {
            long idx = 1;
            for (String kw : keywords) {
                kws.add(getKeywordForStaticAbility(kw, staticId, idx));
                idx++;
            }
        }

        final KeywordsChange newCks = new KeywordsChange(kws, removeKeywords, removeAllKeywords);
        changedCardKeywords.put(timestamp, staticId, newCks);

        if (updateView) {
            updateKeywords();
            if (isToken())
                game.fireEvent(new GameEventTokenStateUpdate(this));
        }
    }

    public final KeywordInterface getKeywordForStaticAbility(String kw, final long staticId, final long idx) {
        KeywordInterface result;
        Triple<String, Long, Long> triple = Triple.of(kw, staticId, idx);
        if (staticId < 1 || !storedKeywords.containsKey(triple)) {
            result = Keyword.getInstance(kw);
            result.setStaticId(staticId);
            result.setIdx(idx);
            result.createTraits(this, false);
            if (staticId > 0) {
                storedKeywords.put(triple, result);
            }
        } else {
            result = storedKeywords.get(triple);
        }
        return result;
    }

    public final void addKeywordForStaticAbility(KeywordInterface kw) {
        if (kw.getStaticId() > 0) {
            storedKeywords.put(Triple.of(kw.getOriginal(), kw.getStaticId(), kw.getIdx()), kw);
        }
    }

    public Map<Triple<String, Long, Long>, KeywordInterface> getStoredKeywords() {
        return storedKeywords;
    }

    public void setStoredKeywords(Map<Triple<String, Long, Long>, KeywordInterface> map, boolean lki) {
        storedKeywords.clear();
        for (Map.Entry<Triple<String, Long, Long>, KeywordInterface> e : map.entrySet()) {
            storedKeywords.put(e.getKey(), getCopyForStoredKeyword(e, lki));
        }
    }

    private final KeywordInterface getCopyForStoredKeyword(Map.Entry<Triple<String, Long, Long>, KeywordInterface> e, boolean lki) {
        // for performance check if we already copied this
        if (lki) {
            for (KeywordsChange kc : changedCardKeywords.column(e.getKey().getMiddle()).values()) {
                // same static id
                for (KeywordInterface kw : kc.getKeywords()) {
                    if (kw.getOriginal().equals(e.getValue().getOriginal())) {
                        // same kw
                        return kw;
                    }
                }
            }
        }

        return e.getValue().copy(this, lki);
    }

    public final void addChangedCardKeywordsByText(final List<KeywordInterface> keywords, final long timestamp, final long staticId, final boolean updateView) {
        // keywords should already created for Card, so no addKeywordsToCard
        // this one is done for Volrath's Shapeshifter which replaces all the card text
        changedCardKeywordsByText.put(timestamp, staticId, new KeywordsChange(keywords, ImmutableList.<KeywordInterface>of(), true));

        if (updateView) {
            updateKeywords();
        }
    }

    public final void addChangedCardKeywordsInternal(
        final List<KeywordInterface> keywords, final List<KeywordInterface> removeKeywords,
        final boolean removeAllKeywords,
        final long timestamp, final long staticId, final boolean updateView) {
        final KeywordsChange newCks = new KeywordsChange(keywords, removeKeywords, removeAllKeywords);
        changedCardKeywords.put(timestamp, staticId, newCks);

        if (updateView) {
            updateKeywords();
        }
    }

    public final boolean removeChangedCardKeywords(final long timestamp, final long staticId) {
        return removeChangedCardKeywords(timestamp, staticId, true);
    }
    public final boolean removeChangedCardKeywords(final long timestamp, final long staticId, final boolean updateView) {
        boolean changed = false;
        changed |= changedCardKeywords.remove(timestamp, staticId) != null;
        changed |= changedCardKeywordsByText.remove(timestamp, staticId) != null;
        if (updateView) {
            updateKeywords();
            if (isToken())
                game.fireEvent(new GameEventTokenStateUpdate(this));
        }
        return changed;
    }

    public boolean clearChangedCardKeywords() {
        return clearChangedCardKeywords(false);
    }
    public final boolean clearChangedCardKeywords(final boolean updateView) {
        boolean changed = false;
        if (!changedCardKeywordsByText.isEmpty()) {
            changed = true;
        }
        changedCardKeywordsByText.clear();
        if (!changedCardKeywords.isEmpty()) {
            changed = true;
        }
        changedCardKeywords.clear();
        if (changed && updateView) {
            updateKeywords();
        }
        return changed;
    }

    public boolean clearStaticChangedCardKeywords(final boolean updateView) {
        // remove all keywords which are done by static ability, where the staticId isn't 0 (these are currently pump or animate effects)
        boolean changed = changedCardKeywords.columnKeySet().retainAll(ImmutableList.of((long)0));
        if (changed && updateView) {
            updateKeywords();
        }
        return changed;
    }

    // Hidden keywords will be left out
    public final Collection<KeywordInterface> getUnhiddenKeywords() {
        return getUnhiddenKeywords(currentState);
    }
    public final Collection<KeywordInterface> getUnhiddenKeywords(CardState state) {
        return state.getCachedKeywords();
    }

    public final void updateKeywordsCache(final CardState state) {
        KeywordCollection keywords = new KeywordCollection();

        // Layer 1
        keywords.insertAll(state.getIntrinsicKeywords());

        keywords.applyChanges(getChangedCardKeywordsList());

        // remove Can't have keywords
        for (Keyword k : getCantHaveKeyword()) {
            keywords.removeAll(k);
        }

        state.setCachedKeywords(keywords);
    }
    private void visitUnhiddenKeywords(CardState state, Visitor<KeywordInterface> visitor) {
        for (KeywordInterface kw : getUnhiddenKeywords(state)) {
            if (!visitor.visit(kw)) {
                return;
            }
        }
    }

    /**
     * Replace all instances of one color word in this card's text by another.
     * @param originalWord the original color word.
     * @param newWord the new color word.
     * @throws RuntimeException if either of the strings is not a valid Magic
     *  color.
     */
    public final void addChangedTextColorWord(final String originalWord, final String newWord, final Long timestamp, final long staticId) {
        if (MagicColor.fromName(newWord) == 0) {
            throw new RuntimeException("Not a color: " + newWord);
        }
        changedTextColors.add(timestamp, staticId, StringUtils.capitalize(originalWord), StringUtils.capitalize(newWord));

        updateChangedText();
    }

    public final void removeChangedTextColorWord(final Long timestamp, final long staticId) {
        if (changedTextColors.remove(timestamp, staticId)) {
            updateChangedText();
        }
    }

    /**
     * Replace all instances of one type in this card's text by another.
     * @param originalWord the original type word.
     * @param newWord the new type word.
     */
    public final void addChangedTextTypeWord(final String originalWord, final String newWord, final Long timestamp, final long staticId) {
        changedTextTypes.add(timestamp, staticId, originalWord, newWord);
        updateChangedText();
    }

    public final void removeChangedTextTypeWord(final Long timestamp, final long staticId) {
        if (changedTextTypes.remove(timestamp, staticId)) {
            updateChangedText();
        }
    }

    /**
     * Update the changed text of the intrinsic spell abilities and keywords.
     */
    public void updateChangedText() {
        // update type
        List<String> toAdd = Lists.newArrayList();
        List<String> toRemove = Lists.newArrayList();

        // before change by text word change, apply the other card type change by text for Volrath's Shapeshifter
        CardTypeView changedByText = getOriginalType().getTypeWithChanges(this.changedCardTypesByText.values());

        for (Map.Entry<String, String> e : this.changedTextTypes.entrySet()) {
            if (changedByText.hasStringType(e.getKey())) {
                toRemove.add(e.getKey());
                toAdd.add(e.getValue());
            }
        }

        this.changedTypeByText = new CardChangedType(new CardType(toAdd, true), new CardType(toRemove, true), false, EnumSet.noneOf(RemoveType.class));

        currentState.updateChangedText();

        // update changed text in the layer, for Volrath's Shapeshifter
        for (CardTraitChanges change : this.changedCardTraitsByText.values()) {
            change.changeText();
        }

        // need to get keywords before text change
        KeywordCollection beforeKeywords = new KeywordCollection();
        beforeKeywords.insertAll(currentState.getIntrinsicKeywords());
        beforeKeywords.applyChanges(this.changedCardKeywordsByText.values());

        final List<KeywordInterface> addKeywords = Lists.newArrayList();
        final List<KeywordInterface> removeKeywords = Lists.newArrayList();
        // Text Change for intrinsic keywords
        for (KeywordInterface kw : beforeKeywords) {
            String oldtxt = kw.getOriginal();
            final String newtxt = AbilityUtils.applyKeywordTextChangeEffects(oldtxt, this);
            if (!newtxt.equals(oldtxt)) {
                KeywordInterface newKw = Keyword.getInstance(newtxt);
                addKeywords.add(newKw);
                removeKeywords.add(kw);
            }
        }

        changedCardKeywordsByWord = new KeywordsChange(addKeywords, removeKeywords, false);

        text = AbilityUtils.applyDescriptionTextChangeEffects(originalText, this);

        currentState.getView().updateAbilityText(this, currentState);
        view.updateNonAbilityText(this);
    }

    public final ImmutableMap<String, String> getChangedTextColorWords() {
        return ImmutableMap.copyOf(changedTextColors);
    }

    public final ImmutableMap<String, String> getChangedTextTypeWords() {
        return ImmutableMap.copyOf(changedTextTypes);
    }

    /**
     * Copy the color and type text changes from another {@link Card} to this
     * one. The original changes of this Card are removed.
     */
    public final void copyChangedTextFrom(final Card other) {
        changedTextColors.copyFrom(other.changedTextColors);
        changedTextTypes.copyFrom(other.changedTextTypes);
    }

    public final KeywordInterface addIntrinsicKeyword(final String s) {
        KeywordInterface inst = currentState.addIntrinsicKeyword(s, true);
        if (inst != null) {
            updateKeywords();
        }
        return inst;
    }

    public final void addIntrinsicKeywords(final Iterable<String> s) {
        addIntrinsicKeywords(s, true);
    }
    public final void addIntrinsicKeywords(final Iterable<String> s, boolean initTraits) {
        if (currentState.addIntrinsicKeywords(s, initTraits)) {
            updateKeywords();
        }
    }

    public final void removeIntrinsicKeyword(final String s) {
        if (currentState.removeIntrinsicKeyword(s)) {
            updateKeywords();
        }
    }

    public final void removeIntrinsicKeyword(final KeywordInterface s) {
        if (currentState.removeIntrinsicKeyword(s)) {
            updateKeywords();
        }
    }

    // Hidden Keywords will be returned without the indicator HIDDEN
    public final Iterable<String> getHiddenExtrinsicKeywords() {
        return Iterables.concat(this.hiddenExtrinsicKeywords.values());
    }
    public final Table<Long, Long, List<String>> getHiddenExtrinsicKeywordsTable() {
        return hiddenExtrinsicKeywords;
    }

    public final void addHiddenExtrinsicKeywords(long timestamp, long staticId, Iterable<String> keywords) {
        // TODO if some keywords aren't removed anymore, then no need for extra Array List
        hiddenExtrinsicKeywords.put(timestamp, staticId, Lists.newArrayList(keywords));

        view.updateNonAbilityText(this);
        updateKeywords();
    }

    public final void removeHiddenExtrinsicKeywords(long timestamp, long staticId) {
        if (hiddenExtrinsicKeywords.remove(timestamp, staticId) != null) {
            view.updateNonAbilityText(this);
            updateKeywords();
        }
    }

    public final void removeHiddenExtrinsicKeyword(String s) {
        boolean updated = false;
        for (List<String> list : hiddenExtrinsicKeywords.values()) {
            if (list.remove(s)) {
                updated = true;
            }
        }
        if (updated) {
            view.updateNonAbilityText(this);
            updateKeywords();
        }
    }

    public void addCantHaveKeyword(Keyword keyword, Long timestamp) {
        cantHaveKeywords.put(timestamp, keyword);
        getView().updateCantHaveKeyword(this);
    }

    public void addCantHaveKeyword(Long timestamp, Iterable<Keyword> keywords) {
        cantHaveKeywords.putAll(timestamp, keywords);
        getView().updateCantHaveKeyword(this);
    }

    public boolean removeCantHaveKeyword(Long timestamp) {
        return removeCantHaveKeyword(timestamp, true);
    }
    public boolean removeCantHaveKeyword(Long timestamp, boolean updateView) {
        boolean change = !cantHaveKeywords.removeAll(timestamp).isEmpty();
        if (change && updateView) {
            getView().updateCantHaveKeyword(this);
            updateKeywords();
            if (isToken())
                game.fireEvent(new GameEventTokenStateUpdate(this));
        }
        return change;
    }

    public Collection<Keyword> getCantHaveKeyword() {
        return cantHaveKeywords.values();
    }

    public final void setStaticAbilities(final List<StaticAbility> a) {
        currentState.setStaticAbilities(a);
    }

    public final FCollectionView<StaticAbility> getStaticAbilities() {
        return currentState.getStaticAbilities();
    }
    public final StaticAbility addStaticAbility(final String s) {
        if (!s.trim().isEmpty()) {
            final StaticAbility stAb = StaticAbility.create(s, this, currentState, true);
            currentState.addStaticAbility(stAb);
            return stAb;
        }
        return null;
    }
    public final StaticAbility addStaticAbility(final StaticAbility stAb) {
        currentState.addStaticAbility(stAb);
        return stAb;
    }

    @Deprecated
    public final void removeStaticAbility(StaticAbility stAb) {
        currentState.removeStaticAbility(stAb);
    }

    public void updateStaticAbilities(List<StaticAbility> list, CardState state) {
        for (final CardTraitChanges ck : getChangedCardTraitsList(state)) {
            if (ck.isRemoveAll()) {
                list.clear();
            }
            list.addAll(ck.getStaticAbilities());
        }

        // keywords are already sorted by Layer
        for (KeywordInterface kw : getUnhiddenKeywords(state)) {
            list.addAll(kw.getStaticAbilities());
        }
    }

    public final boolean isPermanent() {
        return !isImmutable() && (isInPlay() || getType().isPermanent());
    }

    public final boolean isSpell() {
        return isInstant() || isSorcery() || (isAura() && !isInZone((ZoneType.Battlefield)));
    }

    public final boolean isLand()       { return getType().isLand(); }
    public final boolean isBasicLand()  { return getType().isBasicLand(); }
    public final boolean isSnow()       { return getType().isSnow(); }

    public final boolean isTribal()     { return getType().isTribal(); }
    public final boolean isSorcery()    { return getType().isSorcery(); }
    public final boolean isInstant()    { return getType().isInstant(); }

    public final boolean isCreature()   { return getType().isCreature(); }
    public final boolean isArtifact()   { return getType().isArtifact(); }
    public final boolean isPlaneswalker()   { return getType().isPlaneswalker(); }
    public final boolean isBattle()      { return getType().isBattle(); }
    public final boolean isEnchantment()    { return getType().isEnchantment(); }

    public final boolean isEquipment()  { return getType().hasSubtype("Equipment"); }
    public final boolean isFortification()  { return getType().hasSubtype("Fortification"); }
    public final boolean isCurse()          { return getType().hasSubtype("Curse"); }
    public final boolean isAura()           { return getType().hasSubtype("Aura"); }
    public final boolean isShrine()           { return getType().hasSubtype("Shrine"); }

    public final boolean isAttachment() { return isAura() || isEquipment() || isFortification(); }
    public final boolean isHistoric()   { return getType().isLegendary() || isArtifact() || getType().hasSubtype("Saga"); }

    public final boolean isScheme()     { return getType().isScheme(); }
    public final boolean isPhenomenon() { return getType().isPhenomenon(); }
    public final boolean isPlane()      { return getType().isPlane(); }

    /** {@inheritDoc} */
    @Override
    public final int compareTo(final Card that) {
        if (that == null) {
            /*
             * "Here we can arbitrarily decide that all non-null Cards are
             * `greater than' null Cards. It doesn't really matter what we
             * return in this case, as long as it is consistent. I rather think
             * of null as being lowly." --Braids
             */
            return 1;
        }
        return Integer.compare(id, that.id);
    }

    /** {@inheritDoc} */
    @Override
    public final String toString() {
        if (getView() == null) {
            return getPaperCard().getName();
        }
        return getView().toString();
    }

    public final boolean isUnearthed() {
        return unearthed;
    }
    public final void setUnearthed(final boolean b) {
        unearthed = b;
    }

    public final boolean hasSuspend() {
        return hasKeyword(Keyword.SUSPEND) && getLastKnownZone().is(ZoneType.Exile)
                && getCounters(CounterEnumType.TIME) >= 1;
    }

    public final boolean isPhasedOut() {
        return phasedOut != null;
    }
    public final boolean isPhasedOut(Player turn) {
        return turn.equals(phasedOut);
    }
    public final Player getPhasedOut() {
        return phasedOut;
    }
    public final void setPhasedOut(final Player phasedOut0) {
        if (phasedOut == phasedOut0) { return; }
        phasedOut = phasedOut0;
        view.updatePhasedOut(this);
    }

    public final void phase(final boolean fromUntapStep) {
        phase(fromUntapStep, true);
    }
    public final void phase(final boolean fromUntapStep, final boolean direct) {
        final boolean phasingIn = isPhasedOut();

        if (!switchPhaseState(fromUntapStep)) {
            // Switch Phase State bails early if the Permanent can't Phase Out
            return;
        }

        if (!phasingIn) {
            setDirectlyPhasedOut(direct);
        }

        // CR 702.26g
        if (!getAllAttachedCards().isEmpty()) {
            for (final Card eq : getAllAttachedCards()) {
                if (eq.isPhasedOut() == phasingIn) {
                    eq.phase(fromUntapStep, false);
                }
            }
        }

        // update the game entity it was attached to
        GameEntity ge = this.getEntityAttachedTo();
        if (ge != null) {
            ge.updateAttachedCards();
        }

        getGame().fireEvent(new GameEventCardPhased(this, isPhasedOut()));
    }

    private boolean switchPhaseState(final boolean fromUntapStep) {
        if (isPhasedOut() && StaticAbilityCantPhaseIn.cantPhaseIn(this)) {
            return false;
        }

        if (!isPhasedOut() && StaticAbilityCantPhaseOut.cantPhaseOut(this)) {
            return false;
        }

        if (isPhasedOut() && fromUntapStep && wontPhaseInNormal) {
            return false;
        }

        final Map<AbilityKey, Object> runParams = AbilityKey.mapFromCard(this);

        if (!isPhasedOut()) {
            // If this is currently PhasedIn, it's about to phase out.
            // Run trigger before it does because triggers don't work with phased out objects
            getGame().getTriggerHandler().runTrigger(TriggerType.PhaseOut, runParams, true);
            // when it doesn't exist the game will no longer see it as tapped
            runUntapCommands();
            // TODO CR 702.26f need to run LeavesPlay + changeController commands but only when worded "for as long as"

            // these links also break
            clearEncodedCards();
            if (isPaired()) {
                getPairedWith().setPairedWith(null);
                setPairedWith(null);
            }
        }

        setPhasedOut(isPhasedOut() ? null : getController());
        final Combat combat = getGame().getCombat();
        if (combat != null && isPhasedOut()) {
            combat.saveLKI(this);
            combat.removeFromCombat(this);
        }

        if (!isPhasedOut()) {
            // CR 702.26g phases in unattached if that object is still in the same zone or that player is still in the game
            if (isAttachedToEntity()) {
                final GameEntity ge = getEntityAttachedTo();
                boolean unattach = false;
                if (ge instanceof Player) {
                    unattach = !((Player) ge).isInGame();
                } else {
                    unattach = !((Card) ge).isInPlay();
                }
                if (unattach) {
                    unattachFromEntity(ge);
                }
            }

            // Just phased in, time to run the phased in trigger
            getGame().getTriggerHandler().registerActiveTrigger(this, false);
            getGame().getTriggerHandler().runTrigger(TriggerType.PhaseIn, runParams, false);
        }

        game.updateLastStateForCard(this);

        return true;
    }

    public final boolean isDirectlyPhasedOut() {
        return directlyPhasedOut;
    }
    public final void setDirectlyPhasedOut(final boolean direct) {
        directlyPhasedOut = direct;
    }

    public final boolean isWontPhaseInNormal() {
        return wontPhaseInNormal;
    }
    public final void setWontPhaseInNormal(final boolean phaseFlag) {
        wontPhaseInNormal = phaseFlag;
    }

    public final boolean isReflectedLand() {
        for (final SpellAbility a : currentState.getManaAbilities()) {
            if (a.getApi() == ApiType.ManaReflected) {
                return true;
            }
        }
        return false;
    }

    public final boolean hasStartOfKeyword(final String keyword) {
        return hasStartOfKeyword(keyword, currentState);
    }
    public final boolean hasStartOfKeyword(String keyword, CardState state) {
        for (String s : this.getHiddenExtrinsicKeywords()) {
            if (s.startsWith(keyword)) {
                return true;
            }
        }

        HasKeywordVisitor visitor = new HasKeywordVisitor(keyword, true);
        visitKeywords(state, visitor);
        return visitor.getResult();
    }

    public final boolean hasStartOfUnHiddenKeyword(String keyword) {
        return hasStartOfUnHiddenKeyword(keyword, currentState);
    }
    public final boolean hasStartOfUnHiddenKeyword(String keyword, CardState state) {
        HasKeywordVisitor visitor = new HasKeywordVisitor(keyword, true);
        visitUnhiddenKeywords(state, visitor);
        return visitor.getResult();
    }

    public final boolean hasAnyKeyword(final Iterable<String> keywords) {
        return hasAnyKeyword(keywords, currentState);
    }
    public final boolean hasAnyKeyword(final Iterable<String> keywords, CardState state) {
        for (final String keyword : keywords) {
            if (hasKeyword(keyword, state)) {
                return true;
            }
        }
        return false;
    }

    // This counts the number of instances of a keyword a card has
    public final int getAmountOfKeyword(final String k) {
        return getAmountOfKeyword(k, currentState);
    }
    public final int getAmountOfKeyword(final String k, CardState state) {
        int count = Iterables.frequency(this.getHiddenExtrinsicKeywords(), k);
        CountKeywordVisitor visitor = new CountKeywordVisitor(k);
        visitKeywords(state, visitor);
        return count + visitor.getCount();
    }

    public final int getAmountOfKeyword(final Keyword k) {
        return getAmountOfKeyword(k, currentState);
    }
    public final int getAmountOfKeyword(final Keyword k, CardState state) {
        return getKeywords(k, state).size();
    }

    public final Collection<KeywordInterface> getKeywords(final Keyword k) {
        return getKeywords(k, currentState);
    }
    public final Collection<KeywordInterface> getKeywords(final Keyword k, CardState state) {
        return state.getCachedKeyword(k);
    }

    // This is for keywords with a number like Bushido, Annihilator and Rampage.
    // It returns the total.
    public final int getKeywordMagnitude(final Keyword k) {
        return getKeywordMagnitude(k, currentState);
    }

    /**
     * use it only for real keywords and not with hidden ones
     *
     * @return Int
     */
    public final int getKeywordMagnitude(final Keyword k, CardState state) {
        int count = 0;
        for (final KeywordInterface inst : getKeywords(k, state)) {
            String kw = inst.getOriginal();
            // this can't be used yet for everything because of X values in Bushido X
            // KeywordInterface#getAmount
            // KeywordCollection#getAmount

            final String[] parse = kw.contains(":") ? kw.split(":") : kw.split(" ");
            final String s = parse[1];
            if (StringUtils.isNumeric(s)) {
               count += Integer.parseInt(s);
            } else {
                String svar = StringUtils.join(parse);
                if (state.hasSVar(svar)) {
                    count += AbilityUtils.calculateAmount(this, state.getSVar(svar), null);
                }
            }
        }
        return count;
    }

    // Takes one argument like Permanent.Blue+withFlying
    @Override
    public final boolean isValid(final String restriction, final Player sourceController, final Card source, CardTraitBase spellAbility) {
        // Inclusive restrictions are Card types
        final String[] incR = restriction.split("\\.", 2);

        boolean testFailed = false;
        if (incR[0].startsWith("!")) {
            testFailed = true; // a bit counter logical))
            incR[0] = incR[0].substring(1); // consume negation sign
        }

        if (incR[0].equals("Spell")) {
            if (!isSpell()) {
                return testFailed;
            }
        } else if (incR[0].equals("Permanent")) {
            if (!isPermanent()) {
                return testFailed;
            }
        } else if (incR[0].equals("Effect")) {
            if (!isImmutable()) {
                return testFailed;
            }
        } else if (incR[0].equals("Emblem")) {
            if (!isEmblem()) {
                return testFailed;
            }
        } else if (incR[0].equals("card") || incR[0].equals("Card")) {
            if (isImmutable()) {
                return testFailed;
            }
        } else if (incR[0].equals("Any")) {
            if (!(isCreature() || isPlaneswalker() || isBattle())) {
                return false;
            }
            //todo further check for Effect API and other replacement Effect
            /*if (spellAbility == null)
                return false;
            ApiType apiType = ((SpellAbility) spellAbility).getApi();
            if (!(ApiType.DealDamage.equals(apiType) || ApiType.PreventDamage.equals(apiType)))
                return false;*/
        } else if (!getType().hasStringType(incR[0])) {
            return testFailed; // Check for wrong type
        }

        if (incR.length > 1) {
            final String excR = incR[1];
            final String[] exRs = excR.split("\\+"); // Exclusive Restrictions are ...
            for (String exR : exRs) {
                if (!hasProperty(exR, sourceController, source, spellAbility)) {
                    return testFailed;
                }
            }
        }
        return !testFailed;
    }

    // Takes arguments like Blue or withFlying
    @Override
    public boolean hasProperty(final String property, final Player sourceController, final Card source, CardTraitBase spellAbility) {
        if (property.startsWith("!")) {
            return !CardProperty.cardHasProperty(this, property.substring(1), sourceController, source, spellAbility);
        }
        return CardProperty.cardHasProperty(this, property, sourceController, source, spellAbility);
    }

    public final boolean isImmutable() {
        return isImmutable;
    }
    public final void setImmutable(final boolean isImmutable0) {
        isImmutable = isImmutable0;
        view.updateImmutable(this);
    }

    public final boolean isEmblem() {
        return isEmblem;
    }
    public final void setEmblem(final boolean isEmblem0) {
        isEmblem = isEmblem0;
        view.updateEmblem(this);
    }

    public final boolean isBoon() {
        return isBoon;
    }
    public final void setBoon(final boolean isBoon0) {
        isBoon = isBoon0;
        view.updateBoon(this);
    }

    /*
     * there are easy checkers for Color. The CardUtil functions should be made
     * part of the Card class, so calling out is not necessary
     */
    public final boolean isOfColor(final String col) { return getColor().hasAnyColor(MagicColor.fromName(col)); }
    public final boolean isBlack() { return getColor().hasBlack(); }
    public final boolean isBlue() { return getColor().hasBlue(); }
    public final boolean isRed() { return getColor().hasRed(); }
    public final boolean isGreen() { return getColor().hasGreen(); }
    public final boolean isWhite() { return getColor().hasWhite(); }
    public final boolean isColorless() { return getColor().isColorless(); }
    public final boolean associatedWithColor(final String col) {
        final Set<String> color = new HashSet<>();
        if (col != null) {
            color.add(col);
        }
        return isOfColor(col) || canProduceColorMana(color);
    }

    public final boolean sharesNameWith(final Card c1) {
        // in a corner case where c1 is null, there is no name to share with.
        if (c1 == null) {
            return false;
        }

        // Special Logic for SpyKit
        if (c1.hasNonLegendaryCreatureNames()) {
            if (hasNonLegendaryCreatureNames()) {
                // with both does have this, then they share any name
                return true;
            } else if (getName().isEmpty()) {
                // if this does not have a name, then there is no name to share
                return false;
            } else if (StaticData.instance().getCommonCards().isNonLegendaryCreatureName(getName())) {
                // check if this card has a name from a face
                // in general token creatures does not have this
                return true;
            }
        }
        return sharesNameWith(c1.getName(true));
    }

    public final boolean sharesNameWith(final String name) {
        // the name is null or empty
        if (name == null || name.isEmpty()) {
            return false;
        }

        boolean shares = getName(true).equals(name);

        // Split cards has extra logic to check if it does share a name with
        if (isSplitCard()) {
            shares |= name.equals(getState(CardStateName.LeftSplit).getName());
            shares |= name.equals(getState(CardStateName.RightSplit).getName());
        }

        if (!shares && hasNonLegendaryCreatureNames()) {
            // check if the name is from a face
            // in general token creatures does not have this
            return StaticData.instance().getCommonCards().isNonLegendaryCreatureName(name);
        }
        return shares;
    }

    public final boolean sharesColorWith(final Card c1) {
        if (isColorless() || c1.isColorless()) {
            return false;
        }
        boolean shares;
        shares = (isBlack() && c1.isBlack());
        shares |= (isBlue() && c1.isBlue());
        shares |= (isGreen() && c1.isGreen());
        shares |= (isRed() && c1.isRed());
        shares |= (isWhite() && c1.isWhite());
        return shares;
    }

    public final boolean sharesCMCWith(final int n) {
        //need to get GameState for Discarded Cards
        final Card host = game.getCardState(this);

        //do not check for SplitCard anymore
        return host.getCMC() == n;
    }

    public final boolean sharesCMCWith(final Card c1) {
        //need to get GameState for Discarded Cards
        final Card host = game.getCardState(this);
        final Card other = game.getCardState(c1);

        //do not check for SplitCard anymore
        return host.getCMC() == other.getCMC();
    }

    public final boolean sharesCreatureTypeWith(final Card c1) {
        if (c1 == null) {
            return false;
        }
        return getType().sharesCreaturetypeWith(c1.getType());
    }

    public final boolean sharesLandTypeWith(final Card c1) {
        if (c1 == null) {
            return false;
        }
        return getType().sharesLandTypeWith(c1.getType());
    }

    public final boolean sharesPermanentTypeWith(final Card c1) {
        if (c1 == null) {
            return false;
        }
        return getType().sharesPermanentTypeWith(c1.getType());
    }

    public final boolean sharesCardTypeWith(final Card c1) {
        if (c1 == null) {
            return false;
        }
        return getType().sharesCardTypeWith(c1.getType());
    }

    public final boolean sharesControllerWith(final Card c1) {
        return c1 != null && getController().equals(c1.getController());
    }

    public final boolean hasABasicLandType() {
        return getType().hasABasicLandType();
    }

    public final boolean isUsedToPay() {
        return usedToPayCost;
    }
    public final void setUsedToPay(final boolean b) {
        usedToPayCost = b;
    }

    public CardDamageHistory getDamageHistory() {
        return damageHistory;
    }
    public void setDamageHistory(CardDamageHistory history) {
        damageHistory = history;
    }

    public final boolean hasDealtDamageToOpponentThisTurn() {
        return getDamageHistory().getDamageDoneThisTurn(null, true, null, "Player.Opponent", this, getController(), null) > 0;
    }

    /**
     * Gets the total damage done by card this turn (after prevention and redirects).
     *
     * @return the damage done by the card this turn
     */
    public final int getTotalDamageDoneBy() {
        return getDamageHistory().getDamageDoneThisTurn(null, false, null, null, this, getController(), null);
    }

    // this is the amount of damage a creature needs to receive before it dies
    public final int getLethal() {
        if (hasKeyword("Lethal damage dealt to CARDNAME is determined by its power rather than its toughness.")) {
            return getNetPower();
        }
        return getNetToughness();
    }

    // this is the minimal damage a trampling creature has to assign to a blocker
    public final int getLethalDamage() {
        // CR 702.2c
        for (Card c : getAssignedDamageMap().keySet()) {
            if (c.hasKeyword(Keyword.DEATHTOUCH)) {
                return 0;
            }
        }
        return getLethal() - getDamage() - getTotalAssignedDamage();
    }

    public final int getExcessDamageValue(boolean withDeathtouch) {
        ArrayList<Integer> excessCharacteristics = new ArrayList<>();

        // CR 120.10
        if (this.isCreature()) {
            int lethal = this.getLethalDamage();
            if (withDeathtouch && lethal > 0) {
                excessCharacteristics.add(1);
            } else {
                excessCharacteristics.add(Math.max(0, lethal));
            }
        }
        if (this.isPlaneswalker()) {
            excessCharacteristics.add(this.getCurrentLoyalty());
        }
        if (this.isBattle()) {
            excessCharacteristics.add(this.getCurrentDefense());
        }

        if (excessCharacteristics.isEmpty()) {
            return 0;
        }

        return Collections.min(excessCharacteristics);
    }

    public final int getDamage() {
        int sum = 0;
        for (int i : damage.values()) {
            sum += i;
        }
        return sum;
    }
    public final void setDamage(int damage0) {
        if (getDamage() == damage0) { return; }
        damage.clear();
        if (damage0 != 0) {
            damage.put(0, damage0);
        }
        view.updateDamage(this);
        getGame().fireEvent(new GameEventCardStatsChanged(this));
    }

    public int getMaxDamageFromSource() {
        return damage.isEmpty() ? 0 : Collections.max(damage.values());
    }

    public final boolean hasBeenDealtDeathtouchDamage() {
        return hasBeenDealtDeathtouchDamage;
    }
    public final void setHasBeenDealtDeathtouchDamage(final boolean hasBeenDealtDeatchtouchDamage) {
        this.hasBeenDealtDeathtouchDamage = hasBeenDealtDeatchtouchDamage;
    }

    public final boolean hasBeenDealtExcessDamageThisTurn() {
        return hasBeenDealtExcessDamageThisTurn;
    }
    public final void setHasBeenDealtExcessDamageThisTurn(final boolean bool) {
        this.hasBeenDealtExcessDamageThisTurn = bool;
    }

    public final Map<Card, Integer> getAssignedDamageMap() {
        return assignedDamageMap;
    }

    public final void addAssignedDamage(int assignedDamage0, final Card sourceCard) {
        // 510.1a Creatures that would assign 0 or less damage don’t assign combat damage at all.
        if (assignedDamage0 <= 0) {
            return;
        }
        Log.debug(this + " - was assigned " + assignedDamage0 + " damage, by " + sourceCard);
        if (!assignedDamageMap.containsKey(sourceCard)) {
            assignedDamageMap.put(sourceCard, assignedDamage0);
        } else {
            assignedDamageMap.put(sourceCard, assignedDamageMap.get(sourceCard) + assignedDamage0);
        }
        view.updateAssignedDamage(this);
    }
    public final void clearAssignedDamage() {
        if (assignedDamageMap.isEmpty()) { return; }
        assignedDamageMap.clear();
        view.updateAssignedDamage(this);
    }

    public final int getTotalAssignedDamage() {
        int total = 0;
        for (Integer assignedDamage : assignedDamageMap.values()) {
            total += assignedDamage;
        }
        return total;
    }

    public final boolean canDamagePrevented(final boolean isCombat) {
        return !StaticAbilityCantPreventDamage.cantPreventDamage(this, isCombat);
    }

    // This is used by the AI to forecast an effect (so it must not change the game state)
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
                        && isCreature()) {
                    restDamage += 2;
                }
            } else if (c.getName().equals("Furnace of Rath")) {
                if (isCreature()) {
                    restDamage *= 2;
                }
            } else if (c.getName().equals("Dictate of the Twin Gods")) {
                restDamage += restDamage;
            } else if (c.getName().equals("Gratuitous Violence")) {
                if (c.getController().equals(source.getController()) && source.isCreature() && isCreature()) {
                    restDamage *= 2;
                }
            } else if (c.getName().equals("Fire Servant")) {
                if (c.getController().equals(source.getController()) && source.isRed()
                        && (source.isInstant() || source.isSorcery())) {
                    restDamage *= 2;
                }
            } else if (c.getName().equals("Gisela, Blade of Goldnight")) {
                if (!c.getController().equals(getController())) {
                    restDamage *= 2;
                }
            } else if (c.getName().equals("Inquisitor's Flail")) {
                if (isCombat && c.getEquipping() != null
                        && (c.getEquipping().equals(this) || c.getEquipping().equals(source))) {
                    restDamage *= 2;
                }
            } else if (c.getName().equals("Ghosts of the Innocent")) {
                if (isCreature()) {
                    restDamage = restDamage / 2;
                }
            } else if (c.getName().equals("Benevolent Unicorn")) {
                if (source.isSpell() && isCreature()) {
                   restDamage -= 1;
                }
            } else if (c.getName().equals("Divine Presence")) {
                if (restDamage > 3 && isCreature()) {
                    restDamage = 3;
                }
            } else if (c.getName().equals("Lashknife Barrier")) {
                if (c.getController().equals(getController()) && isCreature()) {
                    restDamage -= 1;
                }
            }
        }

        // TODO: improve such that this can be predicted from the replacement effect itself
        // (+ move this function out into ComputerUtilCombat?)
        for (Card c : getGame().getCardsIn(ZoneType.Command)) {
            if (c.getName().equals("Insult Effect")) {
                if (c.getController().equals(source.getController())) {
                    restDamage *= 2;
                }
            } else if (c.getName().equals("Mishra")) {
                if (c.isCreature() && c.getController().equals(source.getController())) {
                    restDamage *= 2;
                }
            }
        }

        if (getName().equals("Phytohydra")) {
            return 0;
        }
        return restDamage;
    }

    /**
     * This function handles damage after replacement and prevention effects are applied.
     */
    @Override
    public final int addDamageAfterPrevention(final int damageIn, final Card source, final SpellAbility cause, final boolean isCombat, GameEntityCounterTable counterTable) {
        if (damageIn <= 0) {
            return 0; // 120.8
        }

        // 120.1a Damage can’t be dealt to an object that’s neither a creature nor a planeswalker nor a battle.
        if (!isPlaneswalker() && !isCreature() && !isBattle()) {
            return 0;
        }

        if (!isInPlay()) { // if target is not in play it can't receive any damage
            return 0;
        }

        // Run replacement effects
        getGame().getReplacementHandler().run(ReplacementType.DealtDamage, AbilityKey.mapFromAffected(this));

        // Run triggers
        Map<AbilityKey, Object> runParams = AbilityKey.newMap();
        runParams.put(AbilityKey.DamageSource, source);
        runParams.put(AbilityKey.DamageTarget, this);
        runParams.put(AbilityKey.Cause, cause);
        runParams.put(AbilityKey.DamageAmount, damageIn);
        runParams.put(AbilityKey.IsCombatDamage, isCombat);
        if (!isCombat) {
            runParams.put(AbilityKey.SpellAbilityStackInstance, game.stack.peek());
        }
        // Defending player at the time the damage was dealt
        runParams.put(AbilityKey.DefendingPlayer, game.getCombat() != null ? game.getCombat().getDefendingPlayerRelatedTo(source) : null);
        getGame().getTriggerHandler().runTrigger(TriggerType.DamageDone, runParams, true);

        DamageType damageType = DamageType.Normal;
        if (isPlaneswalker()) { // 120.3c
            subtractCounter(CounterType.get(CounterEnumType.LOYALTY), damageIn, true);
        }
        if (isBattle()) {
            subtractCounter(CounterType.get(CounterEnumType.DEFENSE), damageIn, true);
        }
        if (isCreature()) {
            if (source.isWitherDamage()) { // 120.3d
                addCounter(CounterEnumType.M1M1, damageIn, source.getController(), counterTable);
                damageType = DamageType.M1M1Counters;
            }
            else { // 120.3e
                int old = damage.getOrDefault(Objects.hash(source.getId(), source.getTimestamp()), 0);
                damage.put(Objects.hash(source.getId(), source.getTimestamp()), old + damageIn);
                view.updateDamage(this);
            }

            if (source.hasKeyword(Keyword.DEATHTOUCH)) {
                setHasBeenDealtDeathtouchDamage(true);
                damageType = DamageType.Deathtouch;
            }

            // 704.8: if it looks like the creature might die from SBA make sure the LKI is refreshed
            if (hasBeenDealtDeathtouchDamage() || (getDamage() > 0 && getLethal() <= getDamage())) {
                game.updateLastStateForCard(this);
            }

            // Play the Damage sound
            game.fireEvent(new GameEventCardDamaged(this, source, damageIn, damageType));
        }

        return damageIn;
    }

    public final String getSetCode() {
        return currentState.getSetCode();
    }
    public final void setSetCode(final String setCode) {
        currentState.setSetCode(setCode);
    }

    public final CardRarity getRarity() {
        return currentState.getRarity();
    }
    public final void setRarity(CardRarity r) {
        currentState.setRarity(r);
    }

    public final String getMostRecentSet() {
        return StaticData.instance().getCommonCards().getCard(getPaperCard().getName()).getEdition();
    }

    public final String getImageKey() {
        return getCardForUi().currentState.getImageKey();
    }
    public final void setImageKey(final String iFN) {
        getCardForUi().currentState.setImageKey(iFN);
    }
    public final void setImageKey(final IPaperCard ipc, final CardStateName stateName) {
        if (ipc == null)
            return;
        switch (stateName) {
            case SpecializeB:
                setImageKey(ipc.getCardBSpecImageKey());
                break;
            case SpecializeR:
                setImageKey(ipc.getCardRSpecImageKey());
                break;
            case SpecializeG:
                setImageKey(ipc.getCardGSpecImageKey());
                break;
            case SpecializeU:
                setImageKey(ipc.getCardUSpecImageKey());
                break;
            case SpecializeW:
                setImageKey(ipc.getCardWSpecImageKey());
                break;
            default:
                break;
        }
    }

    public String getImageKey(CardStateName state) {
        CardState c = getCardForUi().states.get(state);
        return (c != null ? c.getImageKey() : "");
    }

    public final boolean isTributed() { return tributed; }
    public final void setTributed(final boolean b) {
        tributed = b;
    }

    public final SpellAbility getTokenSpawningAbility() {
        return tokenSpawningAbility;
    }
    public void setTokenSpawningAbility(SpellAbility sa) {
        tokenSpawningAbility = sa;
    }

    public final boolean isEmbalmed() {
        SpellAbility sa = getTokenSpawningAbility();
        return sa != null && sa.hasParam("Embalm");
    }

    public final boolean isEternalized() {
        SpellAbility sa = getTokenSpawningAbility();
        return sa != null && sa.hasParam("Eternalize");
    }

    public final int getExertedThisTurn() {
        return exertThisTurn;
    }

    public void exert() {
        exert(getController());
    }
    public void exert(Player p) {
        exertedByPlayer.add(p);
        exertThisTurn++;
        view.updateExertedThisTurn(this, true);
        final Map<AbilityKey, Object> runParams = AbilityKey.mapFromCard(this);
        runParams.put(AbilityKey.Player, p);
        game.getTriggerHandler().runTrigger(TriggerType.Exerted, runParams, false);
    }

    public boolean isExertedBy(final Player player) {
        return exertedByPlayer.contains(player);
    }

    public void removeExertedBy(final Player player) {
        exertedByPlayer.remove(player);
        view.updateExertedThisTurn(this, getExertedThisTurn() > 0);
    }

    protected void resetExertedThisTurn() {
        exertThisTurn = 0;
        view.updateExertedThisTurn(this, false);
    }

    public boolean isMadness() {
        if (this.getCastSA() == null) {
            return false;
        }
        return getCastSA().isMadness();
    }

    public boolean wasDiscarded() { return discarded; }
    public void setDiscarded(boolean state) { discarded = state; }

    public final boolean isRingBearer() {
        return ringbearer;
    }
    public final void setRingBearer(final boolean ringbearer0) {
        ringbearer = ringbearer0;
        view.updateRingBearer(this);
    }
    public final void clearRingBearer() {
        setRingBearer(false);
    }
    public final boolean isMonstrous() {
        return monstrous;
    }
    public final void setMonstrous(final boolean monstrous0) {
        monstrous = monstrous0;
    }

    public final boolean isRenowned() {
        return renowned;
    }
    public final void setRenowned(final boolean renowned0) {
        renowned = renowned0;
    }

    public final boolean isManifested() {
        return manifested;
    }
    public final void setManifested(final boolean manifested) {
        this.manifested = manifested;
    }

    public final boolean isForetold() {
        // in exile and foretold
        if (this.isInZone(ZoneType.Exile)) {
            return this.foretold;
        }
        // cast as foretold, currently only spells
        if (this.getCastSA() != null) {
            return this.getCastSA().isForetold();
        }
        return false;
    }
    public final void setForetold(final boolean foretold) {
        this.foretold = foretold;
    }

    public boolean isForetoldCostByEffect() {
        return foretoldCostByEffect;
    }
    public void setForetoldCostByEffect(final boolean val) {
        this.foretoldCostByEffect = val;
    }

    public boolean isForetoldThisTurn() {
        return foretoldThisTurn;
    }
    public final void setForetoldThisTurn(final boolean foretoldThisTurn) {
        this.foretoldThisTurn = foretoldThisTurn;
    }
    public void resetForetoldThisTurn() {
        foretoldThisTurn = false;
    }

    public boolean isSpecialized() {
        return specialized;
    }
    public final void setSpecialized(final boolean bool) {
        specialized = bool;
        setImageKey(getPaperCard(), getCurrentStateName());
    }
    public final boolean canSpecialize() {
        return getRules() != null && getRules().getSplitType() == CardSplitType.Specialize;
    }

    public int getTimesCrewedThisTurn() {
        return timesCrewedThisTurn;
    }
    public final void setTimesCrewedThisTurn(final int t) {
        this.timesCrewedThisTurn = t;
    }
    public void resetTimesCrewedThisTurn() {
        timesCrewedThisTurn = 0;
    }

    public void becomesCrewed(SpellAbility sa) {
        timesCrewedThisTurn++;
        Map<AbilityKey, Object> runParams = AbilityKey.newMap();
        runParams.put(AbilityKey.Vehicle, this);
        runParams.put(AbilityKey.Crew, sa.getPaidList("TappedCards", true));
        game.getTriggerHandler().runTrigger(TriggerType.BecomesCrewed, runParams, false);
    }

    public final int getClassLevel() {
        return classLevel;
    }
    public void setClassLevel(int level) {
        classLevel = level;
        view.updateClassLevel(this);
        view.getCurrentState().updateAbilityText(this, getCurrentState());
    }
    public boolean isClassCard() {
        return getType().hasStringType("Class");
    }

    public final void animateBestow() {
        animateBestow(true);
    }
    public final void animateBestow(final boolean updateView) {
        if (isBestowed()) {
            return;
        }

        bestowTimestamp = getGame().getNextTimestamp();
        addChangedCardTypes(new CardType(Collections.singletonList("Aura"), true),
                new CardType(Collections.singletonList("Creature"), true),
                false, EnumSet.of(RemoveType.EnchantmentTypes), bestowTimestamp, 0, updateView, false);
        addChangedCardKeywords(Collections.singletonList("Enchant creature"), Lists.newArrayList(),
                false, bestowTimestamp, 0, updateView);
    }

    public final void unanimateBestow() {
        unanimateBestow(true);
    }
    public final void unanimateBestow(final boolean updateView) {
        if (!isBestowed()) {
            return;
        }

        removeChangedCardKeywords(bestowTimestamp, 0, updateView);
        removeChangedCardTypes(bestowTimestamp, 0, updateView);
        bestowTimestamp = -1;
    }

    public final boolean isBestowed() {
        return bestowTimestamp != -1;
    }

    public final long getBestowTimestamp() {
        return bestowTimestamp;
    }
    public final void setBestowTimestamp(final long t) {
        bestowTimestamp = t;
    }

    public final long getTimestamp() {
        return timestamp;
    }
    public final void setTimestamp(final long t) {
        timestamp = t;
    }
    public boolean equalsWithTimestamp(Card c) {
        return equals(c) && c.getTimestamp() == timestamp;
    }

    /**
     * Assign a random foil finish depending on the card edition.
     */
    public final void setRandomFoil() {
        setFoil(CardEdition.getRandomFoil(getSetCode()));
    }
    public final void setFoil(final int f) {
        currentState.setSVar("Foil", Integer.toString(f));
    }

    public final CardCollectionView getHauntedBy() {
        return CardCollection.getView(hauntedBy);
    }
    public final boolean isHaunted() {
        return FCollection.hasElements(hauntedBy);
    }
    public final boolean isHauntedBy(Card c) {
        return FCollection.hasElement(hauntedBy, c);
    }
    public final void addHauntedBy(Card c, final boolean update) {
        hauntedBy = view.addCard(hauntedBy, c, TrackableProperty.HauntedBy);
        if (c != null && update) {
            c.setHaunting(this);
        }
    }
    public final void addHauntedBy(Card c) {
        addHauntedBy(c, true);
    }
    public final void removeHauntedBy(Card c) {
        hauntedBy = view.removeCard(hauntedBy, c, TrackableProperty.HauntedBy);
    }

    public final Card getHaunting() {
        return haunting;
    }
    public final void setHaunting(final Card c) {
        haunting = view.setCard(haunting, c, TrackableProperty.Haunting);
    }

    public final Card getPairedWith() {
        return pairedWith;
    }
    public final void setPairedWith(final Card c) {
        pairedWith = view.setCard(pairedWith, c, TrackableProperty.PairedWith);
    }
    public final boolean isPaired() {
        return pairedWith != null;
    }

    public Card getMeldedWith() { return meldedWith; }
    public void setMeldedWith(Card meldedWith) { this.meldedWith = meldedWith; }

    public String getProtectionKey() {
        String protectKey = "";
        boolean pR = false; boolean pG = false; boolean pB = false; boolean pU = false; boolean pW = false;
        for (final KeywordInterface inst : getKeywords(Keyword.PROTECTION)) {
            String kw = inst.getOriginal();
            if (kw.contains("Protection from red")) {
                if (!pR) {
                    pR = true;
                    protectKey += "R";
                }
            } else if (kw.contains("Protection from green")) {
                if (!pG) {
                    pG = true;
                    protectKey += "G";
                }
            } else if (kw.contains("Protection from black")) {
                if (!pB) {
                    pB = true;
                    protectKey += "B";
                }
            } else if (kw.contains("Protection from blue")) {
                if (!pU) {
                    pU = true;
                    protectKey += "U";
                }
            } else if (kw.contains("Protection from white")) {
                if (!pW) {
                    pW = true;
                    protectKey += "W";
                }
            } else if (kw.equals("Protection from monocolored")) {
                protectKey += "monocolored:";
            } else if (kw.equals("Protection from multicolored")) {
                protectKey += "multicolored:";
            } else if (kw.equals("Protection from all colors")) {
                protectKey += "allcolors:";
            } else if (kw.equals("Protection from colorless")) {
                protectKey += "colorless:";
            } else if (kw.equals("Protection from creatures")) {
                protectKey += "creatures:";
            } else if (kw.equals("Protection from artifacts")) {
                protectKey += "artifacts:";
            } else if (kw.equals("Protection from enchantments")) {
                protectKey += "enchantments:";
            } else if (kw.equals("Protection from everything")) {
                protectKey += "everything:";
            } else if (kw.equals("Protection from colored spells")) {
                protectKey += "coloredspells:";
            } else {
                protectKey += "generic";
            }
        }
        return protectKey;
    }
    public String getHexproofKey() {
        String hexproofKey = "";
        boolean hR = false; boolean hG = false; boolean hB = false; boolean hU = false; boolean hW = false;
        for (final KeywordInterface inst : getKeywords(Keyword.HEXPROOF)) {
            String kw = inst.getOriginal();
            if (kw.equals("Hexproof")) {
                hexproofKey += "generic:";
            }
            if (kw.startsWith("Hexproof:")) {
                String[] k = kw.split(":");
                if (k[2].toString().equals("red")) {
                    if (!hR) {
                        hR = true;
                        hexproofKey += "R:";
                    }
                } else if (k[2].toString().equals("green")) {
                    if (!hG) {
                        hG = true;
                        hexproofKey += "G:";
                    }
                } else if (k[2].toString().equals("black")) {
                    if (!hB) {
                        hB = true;
                        hexproofKey += "B:";
                    }
                } else if (k[2].toString().equals("blue")) {
                    if (!hU) {
                        hU = true;
                        hexproofKey += "U:";
                    }
                } else if (k[2].toString().equals("white")) {
                    if (!hW) {
                        hW = true;
                        hexproofKey += "W:";
                    }
                } else if (k[2].toString().equals("monocolored")) {
                    hexproofKey += "monocolored:";
                }
            }
        }
        return hexproofKey;
    }
    public String getKeywordKey() {
        List<String> ability = new ArrayList<>();
        for (final KeywordInterface inst : getKeywords()) {
            ability.add(inst.getOriginal());
        }
        Collections.sort(ability);
        return StringUtils.join(ability.toArray(), ","); //fix nosuchmethod on some android devices...
    }

    public Zone getZone() {
        return currentZone;
    }
    public void setZone(Zone zone) {
        if (currentZone == zone) { return; }
        currentZone = zone;
        view.updateZone(this);
    }

    public boolean isInZone(final ZoneType zone) {
        Zone z = this.getLastKnownZone();
        return z != null && z.is(zone);
    }

    public final boolean canBeDestroyed() {
        return isInPlay() && !isPhasedOut() && (!hasKeyword(Keyword.INDESTRUCTIBLE) || (isCreature() && getNetToughness() <= 0));
    }

    @Override
    public final boolean canBeTargetedBy(final SpellAbility sa) {
        if (!getOwner().isInGame()) {
            return false;
        }

        if (sa == null) {
            return true;
        }

        // CantTarget static abilities
        if (StaticAbilityCantTarget.cantTarget(this, sa)) {
            return false;
        }

        // keywords don't work outside battlefield
        if (!isInPlay()) {
            return true;
        }

        if (isPhasedOut()) {
            return false;
        }

        return true;
    }

    public final boolean canBeControlledBy(final Player newController) {
        return newController.isInGame() && !(hasKeyword("Other players can't gain control of CARDNAME.") && !getController().equals(newController));
    }

    @Override
    protected final boolean canBeEnchantedBy(final Card aura) {
        SpellAbility sa = aura.getFirstAttachSpell();
        TargetRestrictions tgt = null;
        if (sa != null) {
            tgt = sa.getTargetRestrictions();
        }

        if (tgt != null) {
            boolean zoneValid = false;
            // check the zone types
            for (final ZoneType zt : tgt.getZone()) {
                if (isInZone(zt)) {
                    zoneValid = true;
                    break;
                }
            }
            if (!zoneValid) {
                return false;
            }

            // check valid
            return isValid(tgt.getValidTgts(), aura.getController(), aura, sa);
        }

        return false;
    }

    @Override
    protected final boolean canBeEquippedBy(final Card equip, SpellAbility sa) {
        if (!isInPlay()) {
            return false;
        }
        if (sa != null && sa.isEquip()) {
            return isValid(sa.getTargetRestrictions().getValidTgts(), sa.getActivatingPlayer(), equip, sa);
        }
        return isCreature();
    }

    @Override
    protected boolean canBeFortifiedBy(final Card fort) {
        return isLand() && isInPlay() && !fort.isLand();
    }

    /* (non-Javadoc)
     * @see forge.game.GameEntity#canBeAttached(forge.game.card.Card, boolean)
     */
    @Override
    public boolean canBeAttached(Card attach, SpellAbility sa, boolean checkSBA) {
        // phase check there
        if (isPhasedOut() && !attach.isPhasedOut()) {
            return false;
        }

        return super.canBeAttached(attach, sa, checkSBA);
    }

    public FCollectionView<ReplacementEffect> getReplacementEffects() {
        return currentState.getReplacementEffects();
    }

    public ReplacementEffect addReplacementEffect(final ReplacementEffect replacementEffect) {
        currentState.addReplacementEffect(replacementEffect);
        return replacementEffect;
    }

    @Deprecated
    public void removeReplacementEffect(ReplacementEffect replacementEffect) {
        currentState.removeReplacementEffect(replacementEffect);
    }

    public void updateReplacementEffects(List<ReplacementEffect> list, CardState state) {
        for (final CardTraitChanges ck : getChangedCardTraitsList(state)) {
            if (ck.isRemoveAll()) {
                list.clear();
            }
            list.addAll(ck.getReplacements());
        }

        // Keywords are already sorted by Layer
        for (KeywordInterface kw : getUnhiddenKeywords(state)) {
            list.addAll(kw.getReplacements());
        }

        // Shield Counter aren't affected by Changed Card Traits
        if (getCounters(CounterEnumType.SHIELD) > 0) {
            String sa = "DB$ RemoveCounter | Defined$ Self | CounterType$ Shield | CounterNum$ 1";
            if (shieldCounterReplaceDamage == null) {
                String reStr = "Event$ DamageDone | ActiveZones$ Battlefield | ValidTarget$ Card.Self | PreventionEffect$ True | AlwaysReplace$ True | Secondary$ True "
            + "| Description$ If damage would be dealt to this permanent, prevent that damage and remove a shield counter from it.";
                shieldCounterReplaceDamage = ReplacementHandler.parseReplacement(reStr, this, false, null);
                shieldCounterReplaceDamage.setOverridingAbility(AbilityFactory.getAbility(sa, this));
            }
            if (shieldCounterReplaceDestroy == null) {
                String reStr = "Event$ Destroy | ActiveZones$ Battlefield | ValidCard$ Card.Self | ValidCause$ SpellAbility | Secondary$ True "
            + "| Description$ If this permanent would be destroyed as the result of an effect, instead remove a shield counter from it.";
                shieldCounterReplaceDestroy = ReplacementHandler.parseReplacement(reStr, this, false, null);
                shieldCounterReplaceDestroy.setOverridingAbility(AbilityFactory.getAbility(sa, this));
            }

            list.add(shieldCounterReplaceDamage);
            list.add(shieldCounterReplaceDestroy);
        }
        if (getCounters(CounterEnumType.STUN) > 0) {
            String sa = "DB$ RemoveCounter | Defined$ Self | CounterType$ Stun | CounterNum$ 1";
            if (stunCounterReplaceUntap == null) {
                String reStr = "Event$ Untap | ActiveZones$ Battlefield | ValidCard$ Card.Self  | Secondary$ True "
            + "| Description$ If this permanent would become untapped, instead remove a stun counter from it.";

                stunCounterReplaceUntap = ReplacementHandler.parseReplacement(reStr, this, false, null);
                stunCounterReplaceUntap.setOverridingAbility(AbilityFactory.getAbility(sa, this));
            }
            list.add(stunCounterReplaceUntap);
        }
    }

    public boolean hasReplacementEffect(final ReplacementEffect re) {
        return currentState.hasReplacementEffect(re);
    }
    public boolean hasReplacementEffect(final int id) {
        return currentState.hasReplacementEffect(id);
    }

    public ReplacementEffect getReplacementEffect(final int id) {
        return currentState.getReplacementEffect(id);
    }

    /**
     * Returns what zone this card was cast from (from what zone it was moved to the stack).
     */
    public Zone getCastFrom() {
        return castFrom;
    }
    public void setCastFrom(final Zone castFrom0) {
        castFrom = castFrom0;
    }
    public boolean wasCast() {
        if (hasMergedCard()) {
            boolean wasCast = false;
            for (Card c : getMergedCards()) {
                if (null != c.getCastFrom()) {
                    wasCast = true;
                    break;
                }
            }
            return wasCast;
        }
        return getCastFrom() != null;
    }

    public SpellAbility getCastSA() {
        return castSA;
    }
    public void setCastSA(SpellAbility castSA) {
        this.castSA = castSA;
    }

    public Card getEffectSource() {
        if (effectSourceAbility != null) {
            return effectSourceAbility.getHostCard();
        }
        return effectSource;
    }

    public SpellAbility getEffectSourceAbility() {
        return effectSourceAbility;
    }

    public void setEffectSource(Card src) {
        effectSource = src;
    }

    public void setEffectSource(SpellAbility sa) {
        effectSourceAbility = sa;
    }

    public boolean isStartsGameInPlay() {
        return startsGameInPlay;
    }
    public void setStartsGameInPlay(boolean startsGameInPlay0) {
        startsGameInPlay = startsGameInPlay0;
    }

    public boolean isInPlay() {
        return isInZone(ZoneType.Battlefield);
    }

    public void onEndOfCombat(final Player active) {
        if (this.getController().equals(active)) {
            chosenModesYourLastCombat.clear();
            chosenModesYourLastCombatStatic.clear();
            chosenModesYourLastCombat.putAll(chosenModesYourCombat);
            chosenModesYourLastCombatStatic.putAll(chosenModesYourCombatStatic);
            chosenModesYourCombat.clear();
            chosenModesYourCombatStatic.clear();
            updateAbilityTextForView();
        }
    }

    public void onCleanupPhase(final Player turn) {
        if (!this.hasKeyword("Damage isn't removed from CARDNAME during cleanup steps.")) {
            setDamage(0);
        }
        setHasBeenDealtDeathtouchDamage(false);
        setHasBeenDealtExcessDamageThisTurn(false);
        setRegeneratedThisTurn(0);
        resetShieldCount();
        setBecameTargetThisTurn(false);
        setFoughtThisTurn(false);
        clearMustBlockCards();
        getDamageHistory().setCreatureAttackedLastTurnOf(turn, getDamageHistory().getCreatureAttacksThisTurn() > 0);
        getDamageHistory().newTurn();
        damageReceivedThisTurn.clear();
        clearBlockedByThisTurn();
        clearBlockedThisTurn();
        resetMayPlayTurn();
        resetExertedThisTurn();
        resetTimesCrewedThisTurn();
        resetChosenModeTurn();
        resetAbilityResolvedThisTurn();
    }

    public boolean hasETBTrigger(final boolean drawbackOnly) {
        for (final Trigger tr : getTriggers()) {
            if (tr.getMode() != TriggerType.ChangesZone) {
                continue;
            }

            if (!ZoneType.Battlefield.toString().equals(tr.getParam("Destination"))) {
                continue;
            }

            if (tr.hasParam("ValidCard") && !tr.getParam("ValidCard").contains("Self")) {
                continue;
            }
            if (drawbackOnly) {
                SpellAbility sa = tr.ensureAbility();
                if (sa == null || sa.isActivatedAbility()) {
                    continue;
                }
            }
            return true;
        }
        return false;
    }

    public boolean hasETBReplacement() {
        for (final ReplacementEffect re : getReplacementEffects()) {
            final Map<String, String> params = re.getMapParams();
            if (!(re instanceof ReplaceMoved)) {
                continue;
            }

            if (!ZoneType.Battlefield.toString().equals(params.get("Destination"))) {
                continue;
            }

            if (params.containsKey("ValidCard") && !params.get("ValidCard").contains("Self")) {
                continue;
            }
            return true;
        }
        return false;
    }

    public int getCMC() {
        return getCMC(SplitCMCMode.CurrentSideCMC);
    }
    public int getCMC(SplitCMCMode mode) {
        if (isToken() && getCopiedPermanent() == null) {
            return 0;
        }
        if (lkiCMC >= 0) {
            return lkiCMC; // a workaround used by getLKICopy
        }

        int xPaid = 0;

        // If a card is on the stack, count the xManaCost in with it's CMC
        if (isInZone(ZoneType.Stack) && getManaCost() != null) {
            xPaid = getXManaCostPaid() * getManaCost().countX();
        }

        int requestedCMC = 0;

        if (isSplitCard()) {
            switch(mode) {
                case CurrentSideCMC:
                    requestedCMC = getManaCost().getCMC() + xPaid;
                    break;
                case LeftSplitCMC:
                    requestedCMC = getState(CardStateName.LeftSplit).getManaCost().getCMC() + xPaid;
                    break;
                case RightSplitCMC:
                    requestedCMC = getState(CardStateName.RightSplit).getManaCost().getCMC() + xPaid;
                    break;
                default:
                    System.out.println(TextUtil.concatWithSpace("Illegal Split Card CMC mode", mode.toString(),"passed to getCMC!"));
                    break;
            }
        } else if (currentStateName == CardStateName.Transformed) {
            // Except in the cases were we clone the back-side of a DFC.
            requestedCMC = getState(CardStateName.Original).getManaCost().getCMC();
        } else if (currentStateName == CardStateName.Meld) {
            // Melded creatures have a combined CMC of each of their parts
            requestedCMC = getState(CardStateName.Original).getManaCost().getCMC() + this.getMeldedWith().getManaCost().getCMC();
        } else {
            requestedCMC = getManaCost().getCMC() + xPaid;
        }
        return requestedCMC;
    }

    public final void setLKICMC(final int cmc) {
        this.lkiCMC = cmc;
    }

    public final boolean isLKI() {
        return this.lkiCMC >= 0;
    }

    public final boolean canBeSacrificedBy(final SpellAbility source, final boolean effect) {
        if (isImmutable()) {
            System.out.println("Trying to sacrifice immutables: " + this);
            return false;
        }

        if (!isInPlay() || isPhasedOut()) {
            return false;
        }

        final Card gameCard = game.getCardState(this, null);
        // gameCard is LKI in that case, the card is not in game anymore
        // or the timestamp did change
        // this should check Self too
        if (gameCard == null || !this.equalsWithTimestamp(gameCard)) {
            return false;
        }

        return !StaticAbilityCantSacrifice.cantSacrifice(this, source, effect);
    }

    public CardRules getRules() {
        return cardRules;
    }
    public void setRules(CardRules r) {
        cardRules = r;
        currentState.getView().updateRulesText(r, getType());
        currentState.getView().updateOracleText(this);
    }

    public boolean isCommander() {
        if (this.getMeldedWith() != null && this.getMeldedWith().isCommander())
            return true;
        if (isInPlay() && hasMergedCard()) {
            for (final Card c : getMergedCards())
                if (c.isCommander) return true;
        }
        return isCommander;
    }
    public boolean isRealCommander() {
        return isCommander;
    }
    public void setCommander(boolean b) {
        if (isCommander == b) { return; }
        isCommander = b;
        view.updateCommander(this);
    }
    public void updateCommanderView() {
        view.updateCommander(this);
    }
    public Card getRealCommander() {
        if (isCommander)
            return this;
        if (this.getMeldedWith() != null && this.getMeldedWith().isCommander())
            return this.getMeldedWith();
        if (isInPlay() && hasMergedCard()) {
            for (final Card c : getMergedCards())
                if (c.isCommander) return c;
        }
        return null;
    }

    public boolean canMoveToCommandZone() {
        return canMoveToCommandZone;
    }
    public void setMoveToCommandZone(boolean b) {
        canMoveToCommandZone = b;
    }

    public void setSplitStateToPlayAbility(final SpellAbility sa) {
        if (isInPlay()) {
            return;
        }
        if (sa.isBestow()) {
            animateBestow();
        }
        if (sa.isDisturb() || sa.hasParam("CastTransformed")) {
            incrementTransformedTimestamp();
        }
        if (sa.hasParam("Prototype")) {
            Long next = game.getNextTimestamp();
            addCloneState(CardFactory.getCloneStates(this, this, sa), next);
            prototypeTimestamp = next;
        }
        CardStateName stateName = sa.getCardStateName();
        if (stateName != null && hasState(stateName) && this.getCurrentStateName() != stateName) {
            setState(stateName, true);
            // need to set backSide value according to the SplitType
            if (isDoubleFaced()) {
                setBackSide(getRules().getSplitType().getChangedStateName().equals(stateName));
            }
        }

        if (sa.isCastFaceDown()) {
            turnFaceDown(true);
        }
    }

    public boolean isOptionalCostPaid(OptionalCost cost) { return getCastSA() == null ? false : getCastSA().isOptionalCostPaid(cost); }

    @Override
    public Game getGame() {
        return game;
    }

    public List<SpellAbility> getAllPossibleAbilities(final Player player, final boolean removeUnplayable) {
        CardState oState = getState(CardStateName.Original);
        // this can only be called by the Human
        final List<SpellAbility> abilities = Lists.newArrayList();
        for (SpellAbility sa : getSpellAbilities()) {
            //adventure spell check
            if (isAdventureCard() && sa.isAdventure()) {
                if (getExiledWith() != null && CardStateName.Adventure.equals(getExiledWith().getCurrentStateName()))
                    continue; // skip since it's already on adventure
            }
            //add alternative costs as additional spell abilities
            abilities.add(sa);
            abilities.addAll(GameActionUtil.getAlternativeCosts(sa, player));
        }

        if (isFaceDown() && isInZone(ZoneType.Exile)) {
            for (final SpellAbility sa : oState.getSpellAbilities()) {
                abilities.addAll(GameActionUtil.getAlternativeCosts(sa, player));
            }
        }
        // Add Modal Spells
        if (isModal() && hasState(CardStateName.Modal)) {
            for (SpellAbility sa : getState(CardStateName.Modal).getSpellAbilities()) {
                //add alternative costs as additional spell abilities
                // only add Spells there
                if (sa.isSpell()) {
                    abilities.add(sa);
                    abilities.addAll(GameActionUtil.getAlternativeCosts(sa, player));
                }
            }
        }

        if (isInPlay() && isFaceDown() && isManifested()) {
            ManaCost cost = oState.getManaCost();
            if (oState.getType().isCreature()) {
                abilities.add(CardFactoryUtil.abilityManifestFaceUp(this, cost));
            }
        }

        final Collection<SpellAbility> toRemove = Lists.newArrayListWithCapacity(abilities.size());
        for (final SpellAbility sa : abilities) {
            Player oldController = sa.getActivatingPlayer();
            sa.setActivatingPlayer(player);
            // fix things like retrace
            // check only if SA can't be cast normally
            if (sa.canPlay(true)) {
                continue;
            }
            if ((removeUnplayable && !sa.canPlay()) || !sa.isPossible()) {
                if (oldController != null) {
                    // in case the ability is on the stack this should not change
                    sa.setActivatingPlayer(oldController);
                }
                toRemove.add(sa);
            }
        }
        abilities.removeAll(toRemove);

        // Land Abilities below, move them to CardFactory after MayPlayRefactor
        if (getLastKnownZone().is(ZoneType.Battlefield)) {
            return abilities;
        }
        if (getState(CardStateName.Original).getType().isLand()) {
            LandAbility la = new LandAbility(this, player, null);
            la.setCardState(oState);
            if (la.canPlay()) {
                abilities.add(la);
            }

            Card source = this;
            boolean lkicheck = false;

            // if Card is Facedown, need to check if MayPlay still applies
            if (isFaceDown()) {
                lkicheck = true;
                source = CardUtil.getLKICopy(source);
                source.forceTurnFaceUp();
            }

            if (lkicheck) {
                // double freeze tracker, so it doesn't update view
                game.getTracker().freeze();
                CardCollection preList = new CardCollection(source);
                game.getAction().checkStaticAbilities(false, Sets.newHashSet(source), preList);
            }

            // extra for MayPlay
            for (CardPlayOption o : source.mayPlay(player)) {
                la = new LandAbility(this, player, o);
                la.setCardState(oState);
                if (la.canPlay()) {
                    abilities.add(la);
                }
            }

            // reset static abilities
            if (lkicheck) {
                game.getAction().checkStaticAbilities(false);
                // clear delayed changes, this check should not have updated the view
                game.getTracker().clearDelayed();
                // need to unfreeze tracker
                game.getTracker().unfreeze();
            }
        }

        if (isModal() && hasState(CardStateName.Modal)) {
            CardState modal = getState(CardStateName.Modal);
            if (modal.getType().isLand()) {
                LandAbility la = new LandAbility(this, player, null);
                la.setCardState(modal);

                Card source = CardUtil.getLKICopy(this);
                boolean lkicheck = true;

                // if Card is Facedown, need to check if MayPlay still applies
                if (isFaceDown()) {
                    source.forceTurnFaceUp();
                }

                // the modal state is not copied with lki, need to copy it extra
                if (!source.hasState(CardStateName.Modal)) {
                    source.addAlternateState(CardStateName.Modal, false);
                    source.getState(CardStateName.Modal).copyFrom(this.getState(CardStateName.Modal), true);
                }

                source.setSplitStateToPlayAbility(la);

                if (la.canPlay(source)) {
                    abilities.add(la);
                }

                if (lkicheck) {
                    // double freeze tracker, so it doesn't update view
                    game.getTracker().freeze();
                    CardCollection preList = new CardCollection(source);
                    game.getAction().checkStaticAbilities(false, Sets.newHashSet(source), preList);
                }

                // extra for MayPlay
                for (CardPlayOption o : source.mayPlay(player)) {
                    la = new LandAbility(this, player, o);
                    la.setCardState(modal);
                    if (la.canPlay(source)) {
                        abilities.add(la);
                    }
                }

                // reset static abilities
                if (lkicheck) {
                    game.getAction().checkStaticAbilities(false);
                    // clear delayed changes, this check should not have updated the view
                    game.getTracker().clearDelayed();
                    // need to unfreeze tracker
                    game.getTracker().unfreeze();
                }
            }
        }

        return abilities;
    }

    public static Card fromPaperCard(IPaperCard pc, Player owner) {
        return CardFactory.getCard(pc, owner, owner == null ? null : owner.getGame());
    }

    private static final Map<PaperCard, Card> cp2card = Maps.newHashMap();
    public static Card getCardForUi(IPaperCard pc) {
        if (pc instanceof PaperCard) {
            Card res = cp2card.get(pc);
            if (res == null) {
                res = fromPaperCard(pc, null);
                cp2card.put((PaperCard) pc, res);
            }
            return res;
        }
        return fromPaperCard(pc, null);
    }

    //safe way to get card for ui if card may be null
    public static Card getCardForUi(Card c) {
        if (c == null) { return null; }
        return c.getCardForUi();
    }

    //allow special cards to override this function to return another card for the sake of UI logic
    public Card getCardForUi() {
        return this;
    }

    public IPaperCard getPaperCard() {
        IPaperCard cp = paperCard;
        if (cp != null) {
            return cp;
        }

        final String name = getName();
        final String set = getSetCode();

        if (StringUtils.isNotBlank(set)) {
            cp = StaticData.instance().getVariantCards().getCard(name, set);
            if (cp != null) {
                return cp;
            }
            cp = StaticData.instance().getCommonCards().getCard(name, set);
            if (cp != null) {
                return cp;
            }
        }
        //no specific set for variant
        cp = StaticData.instance().getVariantCards().getCard(name);
        if (cp != null) {
            return cp;
        }
        //try to get from user preference if available
        CardDb.CardArtPreference cardArtPreference = StaticData.instance().getCardArtPreference();
        if (cardArtPreference == null) //fallback
            cardArtPreference = CardArtPreference.ORIGINAL_ART_CORE_EXPANSIONS_REPRINT_ONLY;
        cp = StaticData.instance().getCommonCards().getCardFromEditions(name, cardArtPreference);
        if (cp != null) {
            return cp;
        }
        //lastoption
        return StaticData.instance().getCommonCards().getCard(name);
    }

    /**
     * Update Card instance for the given PaperCard if any
     */
    public static void updateCard(PaperCard pc) {
        Card res = cp2card.get(pc);
        if (res != null) {
            cp2card.put(pc, fromPaperCard(pc, null));
        }
    }

    public List<Object[]> getStaticCommandList() {
        return staticCommandList;
    }

    public void addStaticCommandList(Object[] objects) {
        staticCommandList.add(objects);
    }

    public String getOracleText() {
        CardRules rules = cardRules;
        if (copiedPermanent != null) { //return oracle text of copied permanent if applicable
            rules = copiedPermanent.getRules();
        }
        return rules != null ? rules.getOracleText() : oracleText;
    }
    public void setOracleText(final String oracleText0) {
        oracleText = oracleText0;
        currentState.getView().updateOracleText(this);
    }

    @Override
    public CardView getView() {
        return view;
    }

    // Counts number of instances of a given keyword.
    private static final class CountKeywordVisitor extends Visitor<KeywordInterface> {
        private String keyword;
        private int count;

        private CountKeywordVisitor(String keyword) {
            this.keyword = keyword;
            this.count = 0;
        }

        @Override
        public boolean visit(KeywordInterface inst) {
            final String kw = inst.getOriginal();
            if (kw.equals(keyword)) {
                count++;
            }
            return true;
        }

        public int getCount() {
            return count;
        }
    }

    private static final class HasKeywordVisitor extends Visitor<KeywordInterface> {
        private String keyword;
        private final MutableBoolean result = new MutableBoolean(false);

        private boolean startOf;
        private HasKeywordVisitor(String keyword, boolean startOf) {
            this.keyword = keyword;
            this.startOf = startOf;
        }

        @Override
        public boolean visit(KeywordInterface inst) {
            final String kw = inst.getOriginal();
            if ((startOf && kw.startsWith(keyword)) || kw.equals(keyword)) {
                result.setTrue();
            }
            return result.isFalse();
        }
        public boolean getResult() {
            return result.isTrue();
        }
    }

    // Collects all the keywords into a list.
    private static final class ListKeywordVisitor extends Visitor<KeywordInterface> {
        private List<KeywordInterface> keywords = Lists.newArrayList();

        @Override
        public boolean visit(KeywordInterface kw) {
            keywords.add(kw);
            return true;
        }

        public List<KeywordInterface> getKeywords() {
            return keywords;
        }
    }

    public void setChangedCardTypes(Table<Long, Long, CardChangedType> changedCardTypes) {
        this.changedCardTypes.clear();
        this.changedCardTypes.putAll(changedCardTypes);
    }
    public void setChangedCardTypesCharacterDefining(Table<Long, Long, CardChangedType> changedCardTypes) {
        this.changedCardTypesCharacterDefining.clear();
        this.changedCardTypesCharacterDefining.putAll(changedCardTypes);
    }

    public void setChangedCardKeywords(Table<Long, Long, KeywordsChange> changedCardKeywords) {
        this.changedCardKeywords.clear();
        for (Table.Cell<Long, Long, KeywordsChange> entry : changedCardKeywords.cellSet()) {
            this.changedCardKeywords.put(entry.getRowKey(), entry.getColumnKey(), entry.getValue().copy(this, true));
        }
    }

    public void setChangedCardColors(Table<Long, Long, CardColor> changedCardColors) {
        this.changedCardColors.clear();
        this.changedCardColors.putAll(changedCardColors);
    }
    public void setChangedCardColorsCharacterDefining(Table<Long, Long, CardColor> changedCardColors) {
        this.changedCardColorsCharacterDefining.clear();
        this.changedCardColorsCharacterDefining.putAll(changedCardColors);
    }

    public void cleanupCopiedChangesFrom(Card c) {
        for (StaticAbility stAb : c.getStaticAbilities()) {
            this.removeChangedCardTypes(c.getTimestamp(), stAb.getId(), false);
            this.removeColor(c.getTimestamp(), stAb.getId());
            this.removeChangedCardKeywords(c.getTimestamp(), stAb.getId(), false);
            this.removeChangedCardTraits(c.getTimestamp(), stAb.getId());
        }
    }

    public void forceTurnFaceUp() {
        getGame().getTriggerHandler().suppressMode(TriggerType.TurnFaceUp);
        turnFaceUp(false, false, null);
        getGame().getTriggerHandler().clearSuppression(TriggerType.TurnFaceUp);
    }

    public final void addGoad(Long timestamp, final Player p) {
        goad.put(timestamp, p);
        updateAbilityTextForView();
    }

    public final void removeGoad(Long timestamp) {
        if (goad.remove(timestamp) != null) {
            updateAbilityTextForView();
        }
    }

    public final boolean isGoaded() {
        return !goad.isEmpty();
    }

    public final boolean isGoadedBy(final Player p) {
        return goad.containsValue(p);
    }

    public final PlayerCollection getGoaded() {
        return new PlayerCollection(goad.values()); // 701.38d
    }

    public final Map<Long, Player> getGoadMap() {
        return goad;
    }

    /**
     * Returns the last known zone information for the card. If the card is a LKI copy of another card,
     * then it stores the relevant information in savedLastKnownZone, which is returned. If the card is
     * not a LKI copy (e.g. an ordinary card in the game), it does not have this information and then
     * the last known zone is assumed to be the current zone the card is currently in.
     * @return last known zone of the card (either LKI, if present, or the current zone).
     */
    public final Zone getLastKnownZone() {
        return this.savedLastKnownZone != null ? this.savedLastKnownZone : getZone();
    }

    /**
     * Sets the last known zone information for the card. Should only be used by LKI copies of cards
     * obtained via CardUtil::getLKICopy. Otherwise should be null, which means that current zone the
     * card is in is the last known zone.
     * @param zone last known zone information for the card.
     */
    public final void setLastKnownZone(Zone zone) {
        this.savedLastKnownZone = zone;
    }

    /**
     * ETBCounters are only used between replacementEffects
     * and when the Card really enters the Battlefield with the counters
     * @return map of counters
     */
    public final void addEtbCounter(CounterType type, Integer val, final Player source) {
        int old = etbCounters.contains(source, type) ? etbCounters.get(source, type) : 0;
        etbCounters.put(source, type, old + val);
    }

    public final void clearEtbCounters() {
        etbCounters.clear();
    }

    public final Table<Player, CounterType, Integer> getEtbCounters() {
        return etbCounters;
    }

    public final void putEtbCounters(GameEntityCounterTable table) {
        for (Table.Cell<Player, CounterType, Integer> e : etbCounters.cellSet()) {
            CounterType ct = e.getColumnKey();
            if (this.isLKI()) {
                if (canReceiveCounters(ct)) {
                    setCounters(ct, getCounters(ct) + e.getValue());
                }
            } else {
                addCounter(ct, e.getValue(), e.getRowKey(), table);
            }
        }
    }

    public final int getFinalChapterNr() {
        int n = 0;
        for (final Trigger t : getTriggers()) {
            SpellAbility sa = t.getOverridingAbility();
            if (sa != null && sa.isChapter()) {
                n = Math.max(n, sa.getChapter());
            }
        }
        return n;
    }

    public boolean canBeDiscardedBy(SpellAbility sa, final boolean effect) {
        if (!isInZone(ZoneType.Hand)) {
            return false;
        }

        return getOwner().canDiscardBy(sa, effect);
    }

    public boolean activatedThisTurn() {
        return !numberTurnActivations.isEmpty();
    }

    public void addAbilityActivated(SpellAbility ability) {
        numberTurnActivations.add(ability);
        numberGameActivations.add(ability);

        if (ability.isPwAbility()) {
            addPlaneswalkerAbilityActivated();
        }
    }

    public int getAbilityActivatedThisTurn(SpellAbility ability) {
        return numberTurnActivations.get(ability);
    }

    public int getAbilityActivatedThisGame(SpellAbility ability) {
        return numberGameActivations.get(ability);
    }

    public void addAbilityResolved(SpellAbility ability) {
        numberAbilityResolved.add(ability);
    }
    public int getAbilityResolvedThisTurn(SpellAbility ability) {
        return numberAbilityResolved.get(ability);
    }

    public void resetAbilityResolvedThisTurn() {
        numberAbilityResolved.clear();
    }

    public List<String> getChosenModes(SpellAbility ability, String type) {
        SpellAbility original = null;
        SpellAbility root = ability.getRootAbility();

        // because trigger spell abilities are copied, try to get original one
        if (root.isTrigger()) {
            original = root.getTrigger().getOverridingAbility();
        } else {
            original = ability.getOriginalAbility();
            if (original == null) {
                original = ability;
            }
        }

        if (type.equals("ThisTurn")) {
            if (ability.getGrantorStatic() != null) {
                return chosenModesTurnStatic.get(original, ability.getGrantorStatic());
            }
            return chosenModesTurn.get(original);
        } else if (type.equals("ThisGame")) {
            if (ability.getGrantorStatic() != null) {
                return chosenModesGameStatic.get(original, ability.getGrantorStatic());
            }
            return chosenModesGame.get(original);
        } else if (type.equals("YourLastCombat")) {
            if (ability.getGrantorStatic() != null) {
                return chosenModesYourLastCombatStatic.get(original, ability.getGrantorStatic());
            }
            return chosenModesYourLastCombat.get(original);
        }
        return null;
    }

    public void addChosenModes(SpellAbility ability, String mode, boolean yourCombat) {
        SpellAbility original = null;
        SpellAbility root = ability.getRootAbility();

        // because trigger spell abilities are copied, try to get original one
        if (root.isTrigger()) {
            original = root.getTrigger().getOverridingAbility();
        } else {
            original = ability.getOriginalAbility();
            if (original == null) {
                original = ability;
            }
        }

        if (ability.getGrantorStatic() != null) {
            List<String> result = chosenModesTurnStatic.get(original, ability.getGrantorStatic());
            if (result == null) {
                result = Lists.newArrayList();
                chosenModesTurnStatic.put(original, ability.getGrantorStatic(), result);
            }
            result.add(mode);
            result = chosenModesGameStatic.get(original, ability.getGrantorStatic());
            if (result == null) {
                result = Lists.newArrayList();
                chosenModesGameStatic.put(original, ability.getGrantorStatic(), result);
            }
            result.add(mode);
            if (yourCombat) {
                result = chosenModesYourCombatStatic.get(original, ability.getGrantorStatic());
                if (result == null) {
                    result = Lists.newArrayList();
                    chosenModesYourCombatStatic.put(original, ability.getGrantorStatic(), result);
                }
            }
        } else {
            List<String> result = chosenModesTurn.get(original);
            if (result == null) {
                result = Lists.newArrayList();
                chosenModesTurn.put(original, result);
            }
            result.add(mode);

            result = chosenModesGame.get(original);
            if (result == null) {
                result = Lists.newArrayList();
                chosenModesGame.put(original, result);
            }
            result.add(mode);

            if (yourCombat) {
                result = chosenModesYourCombat.get(original);
                if (result == null) {
                    result = Lists.newArrayList();
                    chosenModesYourCombat.put(original, result);
                }
                result.add(mode);
            }
        }
    }

    public void resetChosenModeTurn() {
        boolean updateView = !chosenModesTurn.isEmpty() || !chosenModesTurnStatic.isEmpty();
        chosenModesTurn.clear();
        chosenModesTurnStatic.clear();
        if (updateView) {
            updateAbilityTextForView();
        }
    }

    public int getPlaneswalkerAbilityActivated() {
        return planeswalkerAbilityActivated;
    }

    public void addPlaneswalkerAbilityActivated() {
        // track if increased limit was used for activation because if there are also additional ones they can count on top
        if (++planeswalkerAbilityActivated == 2 && StaticAbilityNumLoyaltyAct.limitIncrease(this)) {
            planeswalkerActivationLimitUsed = true;
        }
    }

    public boolean planeswalkerActivationLimitUsed() {
        return planeswalkerActivationLimitUsed;
    }

    public void resetActivationsPerTurn() {
        planeswalkerAbilityActivated = 0;
        planeswalkerActivationLimitUsed = false;
        numberTurnActivations.clear();
    }

    public void addCanBlockAdditional(int n, long timestamp) {
        if (n <= 0) {
            return;
        }
        canBlockAdditional.put(timestamp, n);
        getView().updateBlockAdditional(this);
    }

    public boolean removeCanBlockAdditional(long timestamp) {
        boolean result = canBlockAdditional.remove(timestamp) != null;
        if (result) {
            getView().updateBlockAdditional(this);
        }
        return result;
    }

    public int canBlockAdditional() {
        int result = 0;
        for (Integer v : canBlockAdditional.values()) {
            result += v;
        }
        return result;
    }

    public void addCanBlockAny(long timestamp) {
        canBlockAny.add(timestamp);
        getView().updateBlockAdditional(this);
    }

    public boolean removeCanBlockAny(long timestamp) {
        boolean result = canBlockAny.remove(timestamp);
        if (result) {
            getView().updateBlockAdditional(this);
        }
        return result;
    }

    public boolean canBlockAny() {
        return !canBlockAny.isEmpty();
    }

    public boolean removeChangedState() {
        boolean updateState = false;
        updateState |= removeCloneStates();

        updateState |= clearChangedCardTypes();
        updateState |= clearChangedCardKeywords();
        updateState |= clearChangedCardColors();
        updateState |= clearChangedCardTraits();

        updateState |= clearNewPT();
        updateState |= clearChangedName();

        clearEtbCounters();

        return updateState;
    }

    public CardEdition.BorderColor borderColor() {
        CardEdition edition = StaticData.instance().getEditions().get(getSetCode());
        if (edition == null || isBasicLand()) {
            return CardEdition.BorderColor.BLACK;
        }
        return edition.getBorderColor();
    }

    public final CardCollectionView getUntilLeavesBattlefield() {
        return CardCollection.getView(untilLeavesBattlefield);
    }

    public final void addUntilLeavesBattlefield(final Card c) {
        untilLeavesBattlefield = view.addCard(untilLeavesBattlefield, c, TrackableProperty.UntilLeavesBattlefield);
    }
    public final void addUntilLeavesBattlefield(final Iterable<Card> cards) {
        untilLeavesBattlefield = view.addCards(untilLeavesBattlefield, cards, TrackableProperty.UntilLeavesBattlefield);
    }
    public final void removeUntilLeavesBattlefield(final Card c) {
        untilLeavesBattlefield = view.removeCard(untilLeavesBattlefield, c, TrackableProperty.UntilLeavesBattlefield);
    }
    public final void removeUntilLeavesBattlefield(final Iterable<Card> cards) {
        untilLeavesBattlefield = view.removeCards(untilLeavesBattlefield, cards, TrackableProperty.UntilLeavesBattlefield);
    }
    public final void clearUntilLeavesBattlefield() {
        untilLeavesBattlefield = view.clearCards(untilLeavesBattlefield, TrackableProperty.UntilLeavesBattlefield);
    }

    public CombatLki getCombatLKI() {
        return combatLKI;
    }
    public void setCombatLKI(CombatLki combatLKI) {
        this.combatLKI = combatLKI;
    }

    public boolean isAttacking() {
        if (getCombatLKI() != null) {
            return getCombatLKI().isAttacker;
        }
        return getGame().getCombat().isAttacking(this);
    }

    public boolean ignoreLegendRule() {
        // not legendary
        if (!getType().isLegendary()) {
            return true;
        }
        // empty name and no "has non legendary creature names"
        if (this.getName().isEmpty() && !hasNonLegendaryCreatureNames()) {
            return true;
        }
        return StaticAbilityIgnoreLegendRule.ignoreLegendRule(this);
    }

    public boolean attackVigilance() {
        return StaticAbilityAttackVigilance.attackVigilance(this);
    }

    public boolean isAbilitySick() {
        if (!isSick()) {
            return false;
        }
        return !StaticAbilityActivateAbilityAsIfHaste.canActivate(this);
    }

    public boolean isWitherDamage() {
        if (this.hasKeyword(Keyword.WITHER) || this.hasKeyword(Keyword.INFECT)) {
            return true;
        }
        return StaticAbilityWitherDamage.isWitherDamage(this);
    }
}
