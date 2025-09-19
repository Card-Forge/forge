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
import com.google.common.collect.*;
import forge.GameCommand;
import forge.ImageKeys;
import forge.StaticData;
import forge.card.*;
import forge.card.CardDb.CardArtPreference;
import forge.card.CardType.Supertype;
import forge.card.mana.ManaCost;
import forge.card.mana.ManaCostParser;
import forge.game.*;
import forge.game.ability.AbilityFactory;
import forge.game.ability.AbilityKey;
import forge.game.ability.AbilityUtils;
import forge.game.ability.ApiType;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.perpetual.PerpetualInterface;
import forge.game.combat.Combat;
import forge.game.combat.CombatLki;
import forge.game.cost.Cost;
import forge.game.event.*;
import forge.game.event.GameEventCardDamaged.DamageType;
import forge.game.keyword.*;
import forge.game.mana.ManaCostBeingPaid;
import forge.game.player.Player;
import forge.game.player.PlayerCollection;
import forge.game.replacement.*;
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

import static java.lang.Math.max;

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
public class Card extends GameEntity implements Comparable<Card>, IHasSVars, ITranslatable {
    private Game game;
    private final IPaperCard paperCard;

    private final Map<CardStateName, CardState> states = Maps.newEnumMap(CardStateName.class);
    private CardState currentState;
    private CardStateName currentStateName = CardStateName.Original;
    private GamePieceType gamePieceType = GamePieceType.CARD;

    private Zone castFrom;
    private SpellAbility castSA;

    // Hidden keywords won't be displayed on the card
    // x=timestamp y=StaticAbility id
    private final Table<Long, Long, List<String>> hiddenExtrinsicKeywords = TreeBasedTable.create();

    // cards attached or otherwise linked to this card
    private CardCollection hauntedBy, devouredCards, exploitedCards, delvedCards, imprintedCards,
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
    private final Table<StaticAbility, String, SpellAbility> storedSpellAbility = TreeBasedTable.create();
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

    private Map<StaticAbility, CardPlayOption> mayPlay = Maps.newHashMap();

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
    private final List<String> draftActions = Lists.newArrayList();
    private Map<Player, String> flipResult;
    private List<Integer> storedRolls;

    private boolean isCommander = false;
    private boolean canMoveToCommandZone = false;

    private boolean startsGameInPlay = false;
    private boolean drawnThisTurn = false;
    private boolean foughtThisTurn = false;
    private boolean enlistedThisCombat = false;
    private boolean startedTheTurnUntapped = false;
    private boolean cameUnderControlSinceLastUpkeep = true; // for Echo
    private boolean tapped = false;
    private boolean sickness = true; // summoning sickness
    private boolean collectible = false;
    private boolean tokenCard = false;
    private Card copiedPermanent;

    private boolean unearthed;
    private boolean ringbearer;
    private boolean monstrous;
    private boolean harnessed;
    private boolean renowned;
    private boolean solved;
    private boolean tributed;
    private Card suspectedEffect = null;

    private SpellAbility manifestedSA;
    private SpellAbility cloakedSA;

    private boolean foretold;
    private boolean foretoldCostByEffect;

    private boolean plotted;

    private Set<CardStateName> unlockedRooms = EnumSet.noneOf(CardStateName.class);
    private Map<CardStateName, SpellAbility> unlockAbilities = Maps.newEnumMap(CardStateName.class);

    private boolean specialized;

    private int timesCrewedThisTurn = 0;
    private CardCollection crewedByThisTurn;

    private boolean saddled = false;
    private int timesSaddledThisTurn = 0;
    private CardCollection saddledByThisTurn;

    private boolean visitedThisTurn = false;

    private int classLevel = 1;

    private boolean discarded, surveilled, milled;

    private boolean flipped = false;
    private boolean facedown = false;
    private boolean turnedFaceUpThisTurn = false;
    // set for transform and meld, needed for clone effects
    private boolean backside = false;

    private Player phasedOut;
    private boolean directlyPhasedOut = true;
    private boolean wontPhaseInNormal = false;

    private boolean usedToPayCost = false;

    private boolean isEmblem = false;
    private boolean isBoon = false;

    private int exertThisTurn = 0;
    private PlayerCollection exertedByPlayer = new PlayerCollection();

    private PlayerCollection targetedFromThisTurn = new PlayerCollection();

    private long worldTimestamp = -1;
    private long bestowTimestamp = -1;
    private long transformedTimestamp = 0;
    private long prototypeTimestamp = -1;
    private long mutatedTimestamp = -1;
    private int timesMutated = 0;

    private long gameTimestamp = -1; // permanents on the battlefield
    private long layerTimestamp = -1; // order for Static Abilities

    // stack of set power/toughness
    // x=timestamp y=StaticAbility id
    private Table<Long, Long, Pair<Integer,Integer>> newPTText = TreeBasedTable.create(); // Text Change Layer 3
    private Table<Long, Long, Pair<Integer,Integer>> newPTCharacterDefining = TreeBasedTable.create(); // Layer 7a
    private Table<Long, Long, Pair<Integer,Integer>> newPT = TreeBasedTable.create(); // Layer 7b
    private Table<Long, Long, Pair<Integer,Integer>> boostPT = TreeBasedTable.create(); // Layer 7c

    private CardDamageHistory damageHistory = new CardDamageHistory();
    private final Map<Card, Integer> assignedDamageMap = Maps.newTreeMap();
    private Map<Integer, Integer> damage = Maps.newHashMap();
    private boolean hasBeenDealtDeathtouchDamage;
    private boolean hasBeenDealtExcessDamageThisTurn;
    private int excessDamageThisTurnAmount = 0;

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
    private ColorSet markedColor;
    private List<String> chosenName = new ArrayList<>();
    private Integer chosenNumber;
    private Player chosenPlayer;
    private Player promisedGift;
    private Player protectingPlayer;
    private EvenOdd chosenEvenOdd = null;
    private Direction chosenDirection = null;
    private String chosenMode = "";
    private String currentRoom = null;
    private String sector = null;
    private String chosenSector = null;
    private int sprocket = 0;
    private Map<Player, CardCollection> chosenMap = Maps.newHashMap();

    // points to the host that exiled this card, usually the one that has this object it its exiledCards field
    // however it could also be a different card which isn't an error but means the exiling SA was gained
    private Card exiledWith;
    private Player exiledBy;
    private SpellAbility exiledSA;

    private Map<Long, Player> goad = Maps.newTreeMap();

    private List<GameCommand> leavePlayCommandList = Lists.newArrayList();
    private final List<GameCommand> untapCommandList = Lists.newArrayList();
    private final List<GameCommand> changeControllerCommandList = Lists.newArrayList();
    private final List<GameCommand> unattachCommandList = Lists.newArrayList();
    private final List<GameCommand> faceupCommandList = Lists.newArrayList();
    private final List<GameCommand> facedownCommandList = Lists.newArrayList();
    private final List<GameCommand> phaseOutCommandList = Lists.newArrayList();
    private final List<Object[]> staticCommandList = Lists.newArrayList();

    // Zone-changing spells should store card's zone here
    private Zone currentZone;

    // LKI copies of cards are allowed to store the LKI about the zone the card was known to be in last.
    // For all cards except LKI copies this should always be null.
    private Zone savedLastKnownZone;
    // LKI copies of cards store CMC separately to avoid shenanigans with the game state visualization
    // breaking when the LKI object is changed to a different card state.
    private int lkiCMC = -1;

    private CombatLki combatLKI;

    private CardRules cardRules;
    protected boolean renderForUi = true;
    private final CardView view;

    private String overlayText = null;

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

    private ReplacementEffect shieldCounterReplaceDamage = null;
    private ReplacementEffect shieldCounterReplaceDestroy = null;
    private ReplacementEffect stunCounterReplaceUntap = null;
    private ReplacementEffect finalityCounterReplaceDying = null;

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
        view.updateDraftAction(this);
        if (paperCard != null) {
            setMarkedColors(paperCard.getMarkedColors());
            setPaperFoil(paperCard.isFoil());
        }
    }

    public int getHiddenId() {
        return view.getHiddenId();
    }

    public long getPrototypeTimestamp() { return prototypeTimestamp; }

    public long getTransformedTimestamp() { return transformedTimestamp; }
    public void incrementTransformedTimestamp() { this.transformedTimestamp++; }
    public void undoIncrementTransformedTimestamp() { this.transformedTimestamp--; }

    // The following methods are used to selectively update certain view components (text,
    // P/T, card types) in order to avoid card flickering due to aggressive full update
    public void updateAbilityTextForView() {
        view.getCurrentState().updateAbilityText(this, getCurrentState());
    }

    public void updateManaCostForView() {
        currentState.getView().updateManaCost(this);
    }

    public void updatePTforView() {
        getView().updateLethalDamage(this);
        currentState.getView().updatePower(this);
        currentState.getView().updateToughness(this);
    }

    public final void updateTypesForView() {
        currentState.getView().updateType(currentState);
    }

    public final void updateColorForView() {
        currentState.getView().updateColors(this);
        currentState.getView().updateHasChangeColors(!Iterables.isEmpty(getChangedCardColors()));
    }

    public void updateAttackingForView() {
        view.updateAttacking(this);
        getGame().updateCombatForView();
    }
    public void updateBlockingForView() {
        view.updateBlocking(this);
        //ensure blocking arrow shown/hidden as needed
        getGame().updateCombatForView();
    }

    public void updateStateForView() {
        view.updateState(this);
    }

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
        if (state == CardStateName.EmptyRoom) {
            return getEmptyRoomState();
        }
        CardCloneStates clStates = getLastClonedState();
        if (clStates == null) {
            return getOriginalState(state);
        }
        return clStates.get(state);
    }

    public boolean hasState(final CardStateName state) {
        if (state == CardStateName.FaceDown || state == CardStateName.EmptyRoom) {
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
        if (state == CardStateName.EmptyRoom) {
            return getEmptyRoomState();
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

    public boolean changeToState(final CardStateName state) {
        if (hasState(state)) {
            return setState(state, true);
        }
        return false;
    }

    public boolean setState(final CardStateName state, boolean updateView) {
        return setState(state, updateView, false);
    }
    public boolean setState(final CardStateName state, boolean updateView, boolean forceUpdate) {
        boolean rollback = state == CardStateName.Original
                && (currentStateName == CardStateName.Flipped || currentStateName == CardStateName.Backside);
        boolean transform = state == CardStateName.Flipped || state == CardStateName.Backside || state == CardStateName.Meld;
        boolean needsTransformAnimation = transform || rollback;
        // faceDown has higher priority over clone states
        // while text change states doesn't apply while the card is faceDown
        if (state != CardStateName.FaceDown && state != CardStateName.EmptyRoom) {
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
            this.setManifested(null);
            this.setCloaked(null);
        }

        currentStateName = state;
        currentState = getState(state);

        if (updateView) {
            updateStateForView();
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
            updateStateForView();
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
            updateStateForView();
        }
    }

    public boolean changeCardState(final String mode, final String customState, final SpellAbility cause) {
        if (isPhasedOut()) {
            return false;
        }
        if (mode == null) {
            return changeToState(CardStateName.smartValueOf(customState));
        }

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
            long ts = game.getNextTimestamp();
            CardCollectionView cards = hasMergedCard() ? getMergedCards() : new CardCollection(this);
            boolean retResult = false;
            for (final Card c : cards) {
                if (!c.isTransformable()) {
                    continue;
                }
                c.backside = !c.backside;
                // 613.7g A transforming double-faced permanent receives a new timestamp each time it transforms.
                c.setLayerTimestamp(ts);
                boolean result = c.changeToState(c.backside ? CardStateName.Backside : CardStateName.Original);
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
                    // flipping doesn't cause it to have a new layer timestamp
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
        } else if (mode.equals("TurnFaceUp")) {
            if (isFaceDown()) {
                return turnFaceUp(cause);
            }
        } else if (mode.equals("TurnFaceDown")) {
            CardStateName oldState = getCurrentStateName();
            if (oldState == CardStateName.Original || oldState == CardStateName.Flipped
                    || oldState == CardStateName.LeftSplit || oldState == CardStateName.RightSplit || oldState == CardStateName.EmptyRoom) {
                return turnFaceDown();
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
        setManifested(sa);

        // Move to p's battlefield
        Card c = game.getAction().moveToPlay(this, p, sa, params);
        if (c.isInPlay()) {
            c.setManifested(sa);
            c.turnFaceDown(true);
            c.updateStateForView();
        }

        return c;
    }

    public Card cloak(Player p, SpellAbility sa, Map<AbilityKey, Object> params) {
        // Turn Face Down (even if it's DFC).
        // Sometimes cards are manifested while already being face down
        if (!turnFaceDown(true) && !isFaceDown()) {
            return null;
        }

        // Just in case you aren't the controller, now you are!
        setController(p, game.getNextTimestamp());

        // Mark this card as "cloaked"
        setCloaked(sa);
        // give it Ward:2
        getFaceDownState().addIntrinsicKeyword("Ward:2", true);

        // Move to p's battlefield
        Card c = game.getAction().moveToPlay(this, p, sa, params);
        if (c.isInPlay()) {
            c.setCloaked(sa);
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
        long ts = game.getNextTimestamp();
        for (final Card c : cards) {
            if (override || !c.isDoubleFaced()) {
                c.facedown = true;
                // 613.7f A permanent receives a new timestamp each time it turns face up or face down.
                c.setLayerTimestamp(ts);
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

    public boolean canBeTurnedFaceUp() {
        Map<AbilityKey, Object> repParams = AbilityKey.mapFromAffected(this);
        return !getGame().getReplacementHandler().cantHappenCheck(ReplacementType.TurnFaceUp, repParams);
    }

    public void forceTurnFaceUp() {
        turnFaceUp(false, null);
    }

    public boolean turnFaceUp(SpellAbility cause) {
        return turnFaceUp(true, cause);
    }
    public boolean turnFaceUp(boolean runTriggers, SpellAbility cause) {
        if (!isFaceDown() || !canBeTurnedFaceUp()) {
            return false;
        }

        CardCollectionView cards = hasMergedCard() ? getMergedCards() : new CardCollection(this);
        boolean retResult = false;
        long ts = game.getNextTimestamp();
        for (final Card c : cards) {
            boolean result;
            if (c.isFlipped() && c.isFlipCard()) {
                result = c.setState(CardStateName.Flipped, true);
            } else {
                result = c.setState(CardStateName.Original, true);
            }

            c.facedown = false;
            // 613.7f A permanent receives a new timestamp each time it turns face up or face down.
            c.setLayerTimestamp(ts);
            c.turnedFaceUpThisTurn = true;
            if (c.isInPlay()) {
                c.updateRooms();
            }
            c.updateStateForView(); //fixes cards with backside viewable
            // need to run faceup commands, currently
            // it does cleanup the modified facedown state
            if (result) {
                c.runFaceupCommands();
            }
            retResult = retResult || result;
        }

        if (!retResult) return false;

        final TriggerHandler triggerHandler = game.getTriggerHandler();
        if (hasMergedCard()) {
            removeMutatedStates();
            rebuildMutatedStates(cause);
            triggerHandler.clearActiveTriggers(this, null);
            triggerHandler.registerActiveTrigger(this, false);
        }
        if (runTriggers) {
            Map<AbilityKey, Object> repParams = AbilityKey.mapFromAffected(this);
            game.getReplacementHandler().run(ReplacementType.TurnFaceUp, repParams);

            final Map<AbilityKey, Object> runParams = AbilityKey.mapFromCard(this);
            runParams.put(AbilityKey.Cause, cause);

            triggerHandler.registerActiveTrigger(this, false);
            triggerHandler.runTrigger(TriggerType.TurnFaceUp, runParams, false);
        }

        return true;
    }

    public boolean wasTurnedFaceUpThisTurn() {
        return turnedFaceUpThisTurn;
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

        CardStateName destState = transformCard.backside ? CardStateName.Original : CardStateName.Backside;

        // use Original State for the transform check
        if (!transformCard.getOriginalState(destState).getType().isPermanent()) {
            return false;
        }

        return !StaticAbilityCantTransform.cantTransform(this, cause);
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

    public final boolean hasNameOverwrite() {
        return changedCardNames.values().stream().anyMatch(CardChangedName::isOverwrite);
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

    public void setGamePieceType(GamePieceType gamePieceType) {
        this.gamePieceType = gamePieceType;
        this.view.updateGamePieceType(this);
        this.view.updateToken(this);
    }

    public GamePieceType getGamePieceType() {
        return gamePieceType;
    }

    // is this "Card" supposed to be a token?
    public final boolean isToken() {
        if (isInPlay() && hasMergedCard()) {
            return getTopMergedCard().gamePieceType == GamePieceType.TOKEN;
        }
        return gamePieceType == GamePieceType.TOKEN;
    }
    public final boolean isRealToken() {
        return gamePieceType == GamePieceType.TOKEN;
    }

    public final boolean isCopiedSpell() {
        return this.gamePieceType == GamePieceType.COPIED_SPELL;
    }

    public final boolean isImmutable() {
        return this.gamePieceType == GamePieceType.EFFECT;
    }

    public final boolean isInAlternateState() {
        return currentStateName != CardStateName.Original;
    }

    public final boolean hasAlternateState() {
        // Note: Since FaceDown state is created lazily (whereas previously
        // it was always created), adjust threshold based on its existence.
        int threshold = states.containsKey(CardStateName.FaceDown) ? 2 : 1;

        int numStates = states.size();

        return numStates > threshold;
    }

    public final boolean isTransformable() {
        return getRules() != null && getRules().isTransformable();
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
        // Normal Split Cards, these need to return true before Split States are added
        if (getRules() != null && getRules().getSplitType() == CardSplitType.Split) {
            return true;
        }
        // in case or clones or copies
        return hasState(CardStateName.LeftSplit);
    }

    public final boolean isAdventureCard() {
        if (!hasState(CardStateName.Secondary))
            return false;
        return getState(CardStateName.Secondary).getType().hasSubtype("Adventure");
    }

    public final boolean isOnAdventure() {
        if (!isAdventureCard())
            return false;
        if (!equals(getExiledWith()))
            return false;
        if (!CardStateName.Secondary.equals(getExiledWith().getCurrentStateName()))
            return false;
        if (!getExiledWith().getType().hasSubtype("Adventure")) {
            return false;
        }
        return true;
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

    public final CardCollectionView getDevouredCards() {
        return CardCollection.getView(devouredCards);
    }
    public final void addDevoured(final Card c) {
        if (devouredCards == null) {
            devouredCards = new CardCollection();
        }
        devouredCards.add(c);
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
        if (getCastSA() == null) {
            return CardCollection.EMPTY;
        }
        return getCastSA().getTappedForConvoke();
    }

    public final CardCollectionView getEmerged() {
        if (getCastSA() == null) {
            return CardCollection.EMPTY;
        }
        return new CardCollection(getCastSA().getSacrificedAsEmerge());
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

    public final void addToChosenMap(final Player p, final CardCollection chosen) {
        chosenMap.put(p, chosen);
    }
    public final Map<Player, CardCollection> getChosenMap() {
        return chosenMap;
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
    public final void setMergedCards(Iterable<Card> mc) {
        mergedCards = new CardCollection(mc);
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

    /**
     * Gives a collection of all cards that are melded, merged, or are otherwise representing
     * a single permanent alongside this one.
     * @param includeSelf Whether this card is included in the resulting CardCollection.
     */
    public final CardCollection getAllComponentCards(boolean includeSelf) {
        CardCollection out = new CardCollection();
        if(includeSelf)
            out.add(this);
        if(this.getMeldedWith() != null)
            out.add(this.getMeldedWith());
        if(mergedTo != null) //Should be safe to recurse here so long as mergedTo remains a one-way relationship.
            out.addAll(mergedTo.getAllComponentCards(true));
        if(this.hasMergedCard())
            out.addAll(mergedCards);
        if(!includeSelf) //mergedCards includes self.
            out.remove(this);
        return out;
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

    public final void retainPaidList(final SpellAbility cause, final String list) {
        for (Card craft : cause.getPaidList(list)) {
            if (!craft.equals(this) && !craft.isToken()) {
                addExiledCard(craft);
                craft.setExiledWith(this);
                craft.setExiledBy(cause.getActivatingPlayer());
            }
        }
    }

    public final List<Integer> getStoredRolls() {
        return storedRolls;
    }
    public final List<String> getStoredRollsForView() {
        List<String> forView = new ArrayList<>();
        for (Integer i : storedRolls) {
            forView.add(String.valueOf(i));
        }
        return forView;
    }
    public final void addStoredRolls(final List<Integer> results) {
        if (storedRolls == null) {
            storedRolls = Lists.newArrayList();
        }
        storedRolls.addAll(results);
        storedRolls.sort(null);
        view.updateStoredRolls(this);
    }
    public final void replaceStoredRoll(final Map<Integer, Integer> replaceMap) {
        for (Integer oldValue : replaceMap.keySet()) {
            storedRolls.remove(oldValue);
            storedRolls.add(replaceMap.get(oldValue));
        }
        storedRolls.sort(null);
        view.updateStoredRolls(this);
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
    public final boolean hasSecondStrike() {
        return hasDoubleStrike() || !hasFirstStrike();
    }

    public final boolean hasSuspend() {
        return hasKeyword(Keyword.SUSPEND) && getLastKnownZone().is(ZoneType.Exile)
                && getCounters(CounterEnumType.TIME) >= 1;
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
    public final boolean canRemoveCounters(final CounterType type) {
        if (isPhasedOut()) {
            return false;
        }
        final Map<AbilityKey, Object> repParams = AbilityKey.mapFromAffected(this);
        repParams.put(AbilityKey.CounterType, type);
        repParams.put(AbilityKey.Result, 0);
        repParams.put(AbilityKey.IsDamage, false);
        if (game.getReplacementHandler().cantHappenCheck(ReplacementType.RemoveCounter, repParams)) {
            return false;
        }
        return true;
    }

    @Override
    public Integer getCounterMax(final CounterType counterType) {
        if (counterType.is(CounterEnumType.DREAM)) {
            return StaticAbilityMaxCounter.maxCounter(this, counterType);
        }
        return null;
    }

    @Override
    public void addCounterInternal(final CounterType counterType, final int n, final Player source, final boolean fireEvents, GameEntityCounterTable table, Map<AbilityKey, Object> params) {
        int addAmount = n;

        if (addAmount <= 0 || !canReceiveCounters(counterType)) {
            // CR 107.1b
            return;
        }
        final int oldValue = getCounters(counterType);

        Integer max = getCounterMax(counterType);
        if (max != null) {
            addAmount = Math.min(addAmount, max - oldValue);
            if (addAmount <= 0) {
                return;
            }
        }

        final int newValue = addAmount + oldValue;
        if (fireEvents) {
            getGame().updateLastStateForCard(this);

            final SpellAbility cause = (SpellAbility) params.get(AbilityKey.Cause);

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
                if (cause != null) {
                    // 702.100b A creature “evolves” when one or more +1/+1 counters are put on it as a result of its evolve ability resolving.
                    if (cause.isKeyword(Keyword.EVOLVE) && counterType.is(CounterEnumType.P1P1)) {
                        getGame().getTriggerHandler().runTrigger(TriggerType.Evolved, AbilityKey.mapFromCard(this), false);
                    }

                    // 702.149c Some creatures with training have abilities that trigger when they train.
                    // “When this creature trains” means “When a resolving training ability puts a +1/+1 counter on this creature.”
                    if (cause.isKeyword(Keyword.TRAINING) && counterType.is(CounterEnumType.P1P1)) {
                        getGame().getTriggerHandler().runTrigger(TriggerType.Trains, AbilityKey.mapFromCard(this), false);
                    }
                }
            }
        } else {
            setCounters(counterType, newValue);

            getGame().addCounterAddedThisTurn(source, counterType, this, addAmount);
            view.updateCounters(this);
        }
        if (newValue <= 0) {
            removeCounterTimestamp(counterType);
        } else if (addCounterTimestamp(counterType, false)) {
            updateKeywords();
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

        int num = 1;
        if (!Keyword.smartValueOf(counterType.toString().split(":")[0]).isMultipleRedundant()) {
            num = getCounters(counterType);
        }
        addChangedCardKeywords(Collections.nCopies(num, counterType.toString()), null, false, timestamp, null, updateView);
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
    public final int subtractCounter(final CounterType counterName, final int n, final Player remover) {
        return subtractCounter(counterName, n, remover, false);
    }

    public final int subtractCounter(final CounterType counterName, final int n, final Player remover, final boolean isDamage) {
        int oldValue = getCounters(counterName);
        int newValue = max(oldValue - n, 0);

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
            case Replaced:
                return 0;
            default:
                break;
        }

        final int delta = oldValue - newValue;
        if (delta == 0) { return 0; }

        int powerBonusBefore = getPowerBonusFromCounters();
        int toughnessBonusBefore = getToughnessBonusFromCounters();
        int loyaltyBefore = getCurrentLoyalty();

        setCounters(counterName, newValue);
        view.updateCounters(this);

        if (newValue <= 0) {
            if (removeCounterTimestamp(counterName, false)) {
                updateKeywords();
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
        runParams.put(AbilityKey.Player, remover);
        for (int i = 0; i < delta && curCounters != 0; i++) {
            runParams.put(AbilityKey.NewCounterAmount, --curCounters);
            getGame().getTriggerHandler().runTrigger(TriggerType.CounterRemoved, AbilityKey.newMap(runParams), false);
        }
        runParams.put(AbilityKey.CounterAmount, delta);
        runParams.put(AbilityKey.NewCounterAmount, newValue);
        getGame().getTriggerHandler().runTrigger(TriggerType.CounterRemovedOnce, runParams, false);

        return delta;
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
        }
    }

    public final int sumAllCounters() {
        int count = 0;
        for (final Integer value2 : counters.values()) {
            count += value2;
        }
        return count;
    }

    public final void putEtbCounters(Map<Optional<Player>, Map<CounterType, Integer>> etbCounters) {
        if (etbCounters == null) {
            return;
        }
        // used for LKI
        for (Map<CounterType, Integer> m : etbCounters.values()) {
            for (Map.Entry<CounterType, Integer> e : m.entrySet()) {
            CounterType ct = e.getKey();
                if (canReceiveCounters(ct)) {
                    setCounters(ct, getCounters(ct) + e.getValue());
                }
            }
        }
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

    public final boolean enteredThisTurn() {
        return getTurnInZone() == game.getPhaseHandler().getTurn();
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

    public ManaCost getChangedManaCost(long timestamp, long staticId) {
        return changedCardManaCost.get(timestamp, staticId);
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

    public final void setSecretChosenPlayer(final Player p) {
        chosenPlayer = p;
    }
    public final void revealChosenPlayer() {
        view.updateChosenPlayer(this);
    }

    public final boolean hasPromisedGift() {
        return promisedGift != null;
    }
    public final Player getPromisedGift() {
        return promisedGift;
    }
    public final void setPromisedGift(final Player p) {
        if (promisedGift == p) { return; }
        promisedGift = p;
        view.updatePromisedGift(this);
    }

    public final Player getProtectingPlayer() {
        return protectingPlayer;
    }
    public final void setProtectingPlayer(final Player p) {
        if (protectingPlayer == p) { return; }
        protectingPlayer = p;
        view.updateProtectingPlayer(this);
    }

    public final boolean hasChosenNumber() {
        return chosenNumber != null;
    }
    public final Integer getChosenNumber() {
        return chosenNumber;
    }

    public final void setChosenNumber(final int i) { setChosenNumber(i, false); }
    public final void setChosenNumber(final int i, final boolean secret) {
        chosenNumber = i;
        if (!secret) view.updateChosenNumber(this);
    }
    public final void clearChosenNumber() {
        chosenNumber = null;
        view.clearChosenNumber();
    }

    public final Card getExiledWith() {
        return exiledWith;
    }
    public final void setExiledWith(final Card e) {
        exiledWith = view.setCard(exiledWith, e, TrackableProperty.ExiledWith);
    }

    public final void cleanupExiledWith() {
        if (exiledWith == null || exiledWith.isLKI()) {
            return;
        }

        // TODO this may not find the right card in case the ability was granted
        exiledWith.removeExiledCard(this);
        exiledWith.removeUntilLeavesBattlefield(this);

        exiledWith = null;
        exiledBy = null;
        exiledSA = null;
    }

    public final Player getExiledBy() { return exiledBy; }
    public final void setExiledBy(final Player ep) {
        exiledBy = ep;
    }

    public final SpellAbility getExiledSA() { return exiledSA;}
    public final void setExiledSA(final SpellAbility sa) {
        exiledSA = sa;
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

    public final void setSecretChosenType(final String s) {
        chosenType = s;
    }
    public final void revealChosenType() {
        view.updateChosenType(this);
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

    public final boolean hasPaperFoil() {
        return view.hasPaperFoil();
    }
    public final void setPaperFoil(final boolean v) {
        view.updatePaperFoil(v);
    }

    public final ColorSet getMarkedColors() {
        if (markedColor == null) {
            return ColorSet.NO_COLORS;
        }
        return markedColor;
    }
    public final void setMarkedColors(final ColorSet s) {
        markedColor = s;
        view.updateMarkedColors(this);
    }
    public boolean hasMarkedColor() {
        return markedColor != null && !markedColor.isColorless();
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
        updateAbilityTextForView();
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

    public int getSprocket() {
        return this.sprocket;
    }
    public void setSprocket(int sprocket) {
        int oldSprocket = this.sprocket;
        this.sprocket = sprocket;
        view.updateSprocket(this);
        game.fireEvent(new GameEventSprocketUpdate(this, oldSprocket, sprocket));
    }
    public void handleChangedControllerSprocketReset() {
        //See GameAction.stateBasedAction_Contraption.
        //This could be rolled into a bigger handleChangedController method, but I don't know what else would go in it.
        if(this.sprocket != 0)
            this.setSprocket(-1);
    }

    // used for cards like Meddling Mage...
    public final String getNamedCard() {
        return hasNamedCard() ? Iterables.getLast(chosenName) : "";
    }
    public final List<String> getNamedCards() {
        return chosenName;
    }
    public final void setNamedCards(final List<String> s) {
        chosenName = s;
        view.updateNamedCard(this);
    }

    public final void addNamedCard(final String s) {
        chosenName.add(s);
        view.updateNamedCard(this);
    }

    public boolean hasNamedCard() {
        return !chosenName.isEmpty();
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
            sbLong.append(keyword).append("\r\n");
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
        return CardTranslation.translateMultipleDescriptionText(sb.toString(), this);
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
                if (keyword.startsWith("etbCounter")) {
                    final String[] p = keyword.split(":");
                    final StringBuilder s = new StringBuilder();
                    if (p.length > 4) {
                        if (!"no desc".equals(p[4])) {
                            s.append(p[4]);
                        }
                    } else {
                        s.append(getName()).append(" enters with ");
                        s.append(Lang.nounWithNumeralExceptOne(p[2],
                                CounterType.getType(p[1]).getName().toLowerCase() + " counter"));
                        s.append(" on it.");
                    }
                    sbLong.append(s).append("\r\n");
                } else if (keyword.startsWith("DeckLimit")) {
                    final String[] k = keyword.split(":");
                    sbLong.append(k[2]).append("\r\n");
                } else if (keyword.startsWith("Enchant")) {
                    String m[] = keyword.split(":");
                    String desc;
                    if (m.length > 2) {
                        desc = m[2];
                    } else {
                        desc = m[1];
                        if (CardType.isACardType(desc) || "Permanent".equals(desc) || "Player".equals(desc) || "Opponent".equals(desc)) {
                            desc = desc.toLowerCase();
                        }
                    }
                    sbLong.append("Enchant ").append(desc).append("\r\n");
                } else if (keyword.startsWith("Morph") || keyword.startsWith("Megamorph")
                        || keyword.startsWith("Disguise") || keyword.startsWith("Reflect")
                        || keyword.startsWith("Escape") || keyword.startsWith("Foretell:")
                        || keyword.startsWith("Madness:")|| keyword.startsWith("Recover")
                        || keyword.startsWith("Reconfigure") || keyword.startsWith("Squad")
                        || keyword.startsWith("Miracle") || keyword.startsWith("More Than Meets the Eye")
                        || keyword.startsWith("Level up") || keyword.startsWith("Plot")
                        || keyword.startsWith("Offspring") || keyword.startsWith("Mayhem")) {
                    String[] k = keyword.split(":");
                    sbLong.append(k[0]);
                    if (k.length > 1) {
                        final Cost mCost;
                        if ("ManaCost".equals(k[1])) {
                            ManaCost cost;
                            if (keyword.startsWith("Miracle") && k.length > 2) {
                                // TODO better handle 2 hybrid, these should not be reduced?
                                ManaCostBeingPaid mcbp = new ManaCostBeingPaid(getManaCost());
                                mcbp.decreaseGenericMana(Integer.valueOf(k[2]));
                                cost = mcbp.toManaCost();
                            } else {
                                cost = getManaCost();
                            }
                            mCost = new Cost(cost, true);
                        } else {
                            mCost = new Cost(k[1], true);
                        }
                        if (mCost.isOnlyManaCost()) {
                            sbLong.append(" ");
                        } else {
                            sbLong.append("—");
                        }
                        if (keyword.startsWith("Reconfigure") && k.length > 2) {
                            final String[] altCost = new Cost(k[2], true).toString().split(" ");
                            sbLong.append("—").append(altCost[0]).append(" ").append(mCost.toString()).append(" or ").append(altCost[1]);
                        } else {
                            sbLong.append(mCost.toString());
                            if (!mCost.isOnlyManaCost()) {
                                sbLong.append(".");
                            }
                            if (k.length > 3) {
                                sbLong.append(". ").append(k[3]);
                            }
                        }
                        sbLong.append(" (").append(inst.getReminderText()).append(")");
                        sbLong.append("\r\n");
                    } else if (keyword.equals("Mayhem")) {
                        sbLong.append(" (").append(inst.getReminderText()).append(")");
                        sbLong.append("\r\n");
                    }
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
                    final String[] n = keyword.split(":");
                    final Cost cost = new Cost(n[1], false);
                    sbLong.append("Multikicker ").append(cost.toSimpleString());
                    sbLong.append(" (").append(inst.getReminderText()).append(")").append("\r\n");
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
                        // skip reminder text for more complicated Hexproofs
                        if (!k[2].contains(" and ") && !k[2].contains("each")) {
                            sbLong.append(" (").append(inst.getReminderText().replace("chosen", k[2]));
                            sbLong.append(")");
                        }
                        sbLong.append("\r\n");
                    }
                } else if (keyword.startsWith("Protection:")) {
                    final String[] k = keyword.split(":");
                    sbLong.append("Protection from ");
                    if (k.length > 2) {
                        sbLong.append(k[2]);
                    } else {
                        if (MagicColor.Constant.ONLY_COLORS.contains(k[1])) {
                            // lower-case color
                            sbLong.append(k[1]);
                        } else {
                            // plural card types
                            sbLong.append(CardType.getPluralType(k[1]));
                        }
                    }
                    sbLong.append("\r\n");
                } else if (keyword.startsWith("Emerge")) {
                    final String[] k = keyword.split(":");
                    sbLong.append(k[0]);
                    if (k.length > 2) {
                        sbLong.append(" from ").append(k[2].toLowerCase());
                    }
                    sbLong.append(" ").append(ManaCostParser.parse(k[1]));
                    sbLong.append(" (").append(inst.getReminderText()).append(")");
                    sbLong.append("\r\n");

                } else if (inst.getKeyword().equals(Keyword.COMPANION)) {
                    sbLong.append("Companion — ");
                    sbLong.append(((Companion)inst).getDescription());
                } else if (keyword.startsWith("MayFlash")) {
                    // Pseudo keywords, only print Reminder
                    sbLong.append(inst.getReminderText()).append("\r\n");
                } else if (keyword.startsWith("Strive") || keyword.startsWith("Escalate")
                        || keyword.startsWith("ETBReplacement")
                        || keyword.startsWith("Affinity")) {
                } else if (keyword.equals("Provoke") || keyword.equals("Ingest") || keyword.equals("Unleash")
                        || keyword.equals("Soulbond") || keyword.equals("Partner") || keyword.equals("Retrace")
                        || keyword.equals("Living Weapon") || keyword.equals("Myriad") || keyword.equals("Exploit")
                        || keyword.equals("Changeling") || keyword.equals("Delve") || keyword.equals("Decayed")
                        || keyword.equals("Split second") || keyword.equals("Sunburst")
                        || keyword.equals("Double team") || keyword.equals("Living metal")
                        || keyword.equals("Suspend") // for the ones without amount
                        || keyword.equals("Foretell") // for the ones without cost
                        || keyword.equals("Ascend") || keyword.equals("Umbra armor")
                        || keyword.equals("Battle cry") || keyword.equals("Devoid") || keyword.equals("Riot")
                        || keyword.equals("Daybound") || keyword.equals("Nightbound")
                        || keyword.equals("Friends forever") || keyword.equals("Choose a Background")
                        || keyword.equals("Space sculptor") || keyword.equals("Doctor's companion")
                        || keyword.equals("Start your engines")) {
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
                        rem.append(pip).append(" can be paid with {").append(parts[0]).append("}");
                        if (parts.length > 2) {
                            rem.append(", {").append(parts[1]).append("},");
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
                    sbLong.append(k[0]).append(" ").append(k[1]).append(" (As this enters, you may ");
                    sbLong.append("sacrifice any number of ").append(t).append("s. This creature enters ");
                    sbLong.append("with that many +1/+1 counters on it.)");
                } else if (keyword.startsWith("Prototype")) {
                    final String[] k = keyword.split(":");
                    final Cost cost = new Cost(k[1], false);
                    sbLong.append(k[0]).append(" ").append(cost.toSimpleString()).append(" ").append("[").append(k[2]);
                    sbLong.append("/").append(k[3]).append("] ").append("(").append(inst.getReminderText()).append(")");
                } else if (keyword.startsWith("Modular") || keyword.startsWith("Bloodthirst") || keyword.startsWith("Dredge")
                        || keyword.startsWith("Fabricate") || keyword.startsWith("Soulshift") || keyword.startsWith("Bushido")
                        || keyword.startsWith("Saddle") || keyword.startsWith("Tribute") || keyword.startsWith("Absorb")
                        || keyword.startsWith("Graft") || keyword.startsWith("Fading") || keyword.startsWith("Vanishing:")
                        || keyword.startsWith("Afterlife") || keyword.startsWith("Hideaway") || keyword.startsWith("Toxic")
                        || keyword.startsWith("Afflict") || keyword.startsWith ("Poisonous") || keyword.startsWith("Rampage")
                        || keyword.startsWith("Renown") || keyword.startsWith("Annihilator") || keyword.startsWith("Ripple")) {
                    final String[] k = keyword.split(":");
                    sbLong.append(k[0]).append(" ").append(k[1]).append(" (").append(inst.getReminderText()).append(")");
                } else if (keyword.startsWith("Crew")) {
                    final String[] k = keyword.split(":");
                    sbLong.append("Crew ").append(k[1]);
                    if (k.length > 2) {
                        if (k[2].contains("ActivationLimit$ 1")) {
                            sbLong.append(". Activate only once each turn.");
                        }
                    }
                    sbLong.append(" (").append(inst.getReminderText()).append(")");
                } else if (keyword.startsWith("Casualty")) {
                    final String[] k = keyword.split(":");
                    sbLong.append("Casualty ").append(k[1]);
                    if (k.length >= 4) {
                        sbLong.append(". ").append(k[3]);
                    }
                    sbLong.append(" (").append(inst.getReminderText()).append(")");
                } else if (keyword.equals("Gift")) {
                    sbLong.append(keyword);
                    Trigger trig = inst.getTriggers().stream().findFirst().orElse(null);
                    if (trig != null && trig.getCardState().getFirstSpellAbility().hasAdditionalAbility("GiftAbility")) {
                        sbLong.append(" ").append(trig.getCardState().getFirstSpellAbility().getAdditionalAbility("GiftAbility").getParam("GiftDescription"));
                    }
                    sbLong.append("\r\n");
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
                } else if (keyword.startsWith("Bands with other")) {
                    final String[] k = keyword.split(":");
                    String desc = k.length > 2 ? k[2] : CardType.getPluralType(k[1]);
                    sbLong.append(k[0]).append(" ").append(desc).append(" (").append(inst.getReminderText()).append(")");
                } else if (keyword.equals("Convoke") || keyword.equals("Dethrone") || keyword.equals("Fear")
                         || keyword.equals("Melee") || keyword.equals("Improvise") || keyword.equals("Shroud")
                         || keyword.equals("Banding") || keyword.equals("Intimidate") || keyword.equals("Evolve")
                         || keyword.equals("Exalted") || keyword.equals("Extort") || keyword.equals("Flanking")
                         || keyword.equals("Horsemanship") || keyword.equals("Infect") || keyword.equals("Persist")
                         || keyword.equals("Phasing") || keyword.equals("Shadow") || keyword.equals("Skulk")
                         || keyword.equals("Undying") || keyword.equals("Wither") || keyword.equals("Bargain")
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
                    final boolean complex = k[1].contains("X") || (k[1].contains (" ") && k[1].contains("<"));
                    final String extra = k.length > 2 ? ", " + k[2] + "." : "";

                    sbLong.append(k[0]).append(onlyMana ? " " : "—").append(cost.toSimpleString());
                    sbLong.append(onlyMana? "" : ".").append(extra);
                    sbLong.append(!complex ? " (" + (inst.getReminderText()) + ")" : "");
                    sbLong.append("\r\n");
                } else if (keyword.startsWith("Offering")) {
                    String type = keyword.split(":")[1];
                    if (sb.length() != 0) {
                        sb.append("\r\n");
                    }
                    sbLong.append(type).append(" offering");
                    sbLong.append(" (").append(inst.getReminderText()).append(")");
                } else if (keyword.startsWith("Equip") || keyword.startsWith("Fortify") || keyword.startsWith("Outlast")
                        || keyword.startsWith("Unearth") || keyword.startsWith("Scavenge")
                        || keyword.startsWith("Spectacle") || keyword.startsWith("Evoke")
                        || keyword.startsWith("Bestow") || keyword.startsWith("Surge")
                        || keyword.startsWith("Transmute") || keyword.startsWith("Suspend")
                        || keyword.startsWith("Dash") || keyword.startsWith("Disturb")
                        || keyword.equals("Undaunted") || keyword.startsWith("Monstrosity") || keyword.startsWith("Impending")
                        || keyword.startsWith("Embalm") || keyword.equals("Prowess")
                        || keyword.startsWith("Eternalize") || keyword.startsWith("Reinforce")
                        || keyword.startsWith("Champion") || keyword.startsWith("Freerunning") || keyword.startsWith("Prowl") || keyword.startsWith("Adapt")
                        || keyword.startsWith("Amplify") || keyword.startsWith("Ninjutsu") || keyword.startsWith("Chapter")
                        || keyword.startsWith("Transfigure") || keyword.startsWith("Aura swap")
                        || keyword.startsWith("Cycling") || keyword.startsWith("TypeCycling")
                        || keyword.startsWith("Encore") || keyword.startsWith("Mutate") || keyword.startsWith("Dungeon")
                        || keyword.startsWith("Class") || keyword.startsWith("Blitz") || keyword.startsWith("Web-slinging")
                        || keyword.startsWith("Specialize") || keyword.equals("Ravenous") || keyword.startsWith("Firebending")
                        || keyword.equals("For Mirrodin") || keyword.equals("Job select") || keyword.startsWith("Craft")
                        || keyword.startsWith("Landwalk") || keyword.startsWith("Visit") || keyword.startsWith("Mobilize")
                        || keyword.startsWith("Station") || keyword.startsWith("Warp") || keyword.startsWith("Devour")) {
                    // keyword parsing takes care of adding a proper description
                } else if (keyword.equals("Read ahead")) {
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
                Sentry.addBreadcrumb(bread);

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
        return CardTranslation.translateMultipleDescriptionText(sb.toString(), this);
    }

    private String kickerDesc(String keyword, String remText) {
        final StringBuilder sbx = new StringBuilder();
        final String[] n = keyword.split(":");
        final Cost cost = new Cost(n[1], false);
        final String costStr = cost.toSimpleString();
        final boolean manaOnly = cost.isOnlyManaCost();
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

        if (plotted) sb.append("Plotted\r\n");

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

        if (state.getStateName().equals(CardStateName.Backside) && state.getCard().isTransformable() &&
                state.getView().getOracleText().startsWith("(Transforms")) {
            sb.append("(").append(Localizer.getInstance().getMessage("lblTransformsFrom",
                    CardTranslation.getTranslatedName(state.getCard().getState(CardStateName.Original).getName())));
            sb.append(")").append(linebreak);
        }

        // Check if the saga card does not have the keyword Read ahead
        if (type.hasSubtype("Saga") && !state.hasKeyword(Keyword.READ_AHEAD)) {
            sb.append("(").append(Localizer.getInstance().getMessage("lblSagaHeader"));
            if (!state.getCard().isTransformable()) {
                sb.append(" ").append(Localizer.getInstance().getMessage("lblSagaFooter")).append(" ").append(TextUtil.toRoman(state.getFinalChapterNr())).append(".");
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
        if (harnessed) {
            sb.append("Harnessed\r\n");
        }
        if (renowned) {
            sb.append("Renowned\r\n");
        }
        if (solved) {
            sb.append("Solved\r\n");
        }
        if (saddled) {
            sb.append("Saddled\r\n");
        }
        if (isSuspected()) {
            sb.append("Suspected\r\n");
        }
        if (isManifested()) {
            sb.append("Manifested\r\n");
        }
        if (isCloaked()) {
            sb.append("Cloaked\r\n");
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
                        replacementEffect.getParam("Description").contains("enters")) {
                    sb.append(text).append(linebreak);
                } else {
                    replacementEffects.append(text).append(linebreak);
                }
            }
        }

        if (this.getRules() != null && state.getStateName().equals(CardStateName.Original)) {
            // try to look which what card this card can be meld to
            // only show this info if this card does not has the meld Effect itself

            boolean hasMeldEffect = hasSVar("Meld")
                    || state.getNonManaAbilities().anyMatch(SpellAbilityPredicates.isApi(ApiType.Meld));
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
                if (!stAbD.isEmpty()) {
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

            // should not print Spelldescription for Morph
            if (sa.isCastFaceDown()) {
                continue;
            }

            String sAbility = formatSpellAbility(sa);

            // add Adventure to AbilityText
            if (sa.isAdventure() && state.getStateName().equals(CardStateName.Original)) {
                CardState advState = getState(CardStateName.Secondary);
                StringBuilder sbSA = new StringBuilder();
                sbSA.append(Localizer.getInstance().getMessage("lblAdventure"));
                sbSA.append(" — ").append(CardTranslation.getTranslatedName(advState.getName()));
                sbSA.append(" ").append(sa.getPayCosts().toSimpleString());
                sbSA.append(": ");
                sbSA.append(sAbility);
                sAbility = sbSA.toString();
            } else if (sa.isOmen() && state.getStateName().equals(CardStateName.Original)) {
                CardState advState = getState(CardStateName.Secondary);
                StringBuilder sbSA = new StringBuilder();
                sbSA.append(Localizer.getInstance().getMessage("lblOmen"));
                sbSA.append(" — ").append(CardTranslation.getTranslatedName(advState.getName()));
                sbSA.append(" ").append(sa.getPayCosts().toSimpleString());
                sbSA.append(": ");
                sbSA.append(sAbility);
                sAbility = sbSA.toString();
            } else if (sa.isSpell() && sa.isBasicSpell()) {
                continue;
            } else if (sa.hasParam("DescriptionFromChosenName") && !getNamedCard().isEmpty()) {
                String name = getNamedCard();
                ICardFace namedFace = StaticData.instance().getCommonCards().getFaceByName(name);
                StringBuilder sbSA = new StringBuilder(sAbility);
                sbSA.append(linebreak);
                sbSA.append(Localizer.getInstance().getMessage("lblSpell"));
                sbSA.append(" — ");
                if(!namedFace.getManaCost().isNoCost()) {
                    sbSA.append(namedFace.getManaCost().getSimpleString()).append(": ");
                }
                sbSA.append(namedFace.getName()).append("\r\n");
                sbSA.append(namedFace.getType()).append("\r\n");
                sbSA.append(namedFace.getOracleText().replaceAll("\\\\n", "\r\n"));
                sbSA.append(linebreak);
                sAbility = sbSA.toString();
            }

            if (sa.getManaPart() != null) {
                if (addedManaStrings.contains(sAbility)) {
                    continue;
                }
                addedManaStrings.add(sAbility);
            }

            boolean alwaysShow = false;
            if (!sa.isIntrinsic()) alwaysShow = true; // allows added abilities to show on face-down stuff (e.g. Morph,Cloaked)
            if (!sAbility.endsWith(state.getName() + "\r\n") || alwaysShow) {
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
                    if (stAb.checkMode(StaticAbilityMode.CantBlockBy)) {
                        if (!stAb.hasParam("ValidAttacker") || (stAb.hasParam("ValidBlocker") && stAb.getParam("ValidBlocker").equals("Creature.Self"))) {
                            continue;
                        }
                        if (stAb.matchesValidParam("ValidAttacker", this)) {
                            found = true;
                        }
                    } else if (stAb.checkMode(StaticAbilityMode.MinMaxBlocker)) {
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

        // Ensure no more escaped linebreak are present
        desc = desc.replace("\\r", "\r")
            .replace("\\n", "\n");

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
                        || keyword.equals("Bargain") || keyword.equals("Spree")
                        || keyword.equals("Tiered") || keyword.equals("Split second")) {
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
                        || keyword.startsWith("Freerunning") || keyword.startsWith("Prowl")) {
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
                    final String[] n = keyword.split(":");
                    final Cost cost = new Cost(n[1], false);
                    sbBefore.append("Multikicker ").append(cost.toSimpleString()).append(" (").append(inst.getReminderText()).append(")").append("\r\n");
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
                        || keyword.startsWith("Disturb") || keyword.startsWith("Overload")
                        || keyword.startsWith("Plot") || keyword.startsWith("Mayhem")) {
                    final String[] k = keyword.split(":");
                    final Cost mCost;
                    if (k.length < 2 || "ManaCost".equals(k[1])) {
                        mCost = new Cost(getManaCost(), false);
                    } else {
                        mCost = new Cost(k[1], false);
                    }

                    StringBuilder sbCost = new StringBuilder(k[0]);
                    if (!mCost.isOnlyManaCost()) {
                        sbCost.append("—");
                    } else {
                        sbCost.append(" ");
                    }
                    sbCost.append(mCost.toSimpleString());
                    sbAfter.append(sbCost).append(" (").append(inst.getReminderText()).append(")");
                    sbAfter.append("\r\n");
                } else if (keyword.equals("Gift")) {
                    sbBefore.append(keyword);
                    if (state.getFirstAbility().hasAdditionalAbility("GiftAbility")) {
                        sbBefore.append(" ").append(state.getFirstAbility().getAdditionalAbility("GiftAbility").getParam("GiftDescription"));
                    }
                    sbBefore.append("\r\n");
                } else if (keyword.equals("Remove CARDNAME from your deck before playing if you're not " +
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
                } else if (keyword.equals("Assist")) {
                    sbBefore.append(keyword).append(" (").
                    append(String.format(inst.getReminderText(), "{" + getManaCost().getGenericCost() + "}"))
                    .append(")");
                    sbBefore.append("\r\n\r\n");
                } else if (keyword.startsWith("DeckLimit")) {
                    final String[] k = keyword.split(":");
                    sbBefore.append(k[2]).append("\r\n");
                }
            } catch (Exception e) {
                String msg = "Card:abilityTextInstantSorcery: crash in Keyword parsing";

                Breadcrumb bread = new Breadcrumb(msg);
                bread.setData("Card", this.getName());
                bread.setData("Keyword", keyword);
                Sentry.addBreadcrumb(bread);

                throw new RuntimeException("Error in Card " + this.getName() + " with Keyword " + keyword, e);
            }
        }

        sb.append(CardTranslation.translateMultipleDescriptionText(sbBefore.toString(), state));

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
                if (!stAbD.isEmpty()) {
                    sb.append(stAbD).append("\r\n");
                }
            }
        }

        sb.append(CardTranslation.translateMultipleDescriptionText(sbAfter.toString(), state));
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

    public final int getMaxManaProduced() {
        int max_produced = 0;
        for (SpellAbility m: getManaAbilities()) {
            m.setActivatingPlayer(getController());
            int mana_cost = m.getPayCosts().getTotalMana().getCMC();
            max_produced = max(max_produced, m.amountOfManaGenerated(true) - mana_cost);
        }
        return max_produced;
    }

    public final SpellAbility getFirstSpellAbility() {
        return Iterables.getFirst(currentState.getNonManaAbilities(), null);
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
        if (!getReplacementEffects().isEmpty()
                && (getReplacementEffects().size() > 1 || !isSaga() || hasKeyword(Keyword.READ_AHEAD))) {
            return false;
        }
        if (!getTriggers().isEmpty()) {
            return false;
        }
        for (SpellAbility sa : getSpellAbilities()) {
            // morph up and disguise up are not part of the card
            if (sa.isMorphUp() || sa.isDisguiseUp()) {
                continue;
            }
            // while Adventure and Omen are part of Secondary
            if ((sa.isAdventure() || sa.isOmen()) && !getCurrentStateName().equals(sa.getCardState())) {
                continue;
            }
            if (!(sa instanceof SpellPermanent && sa.isBasicSpell())) {
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
                    list.removeIf(SpellAbilityPredicates.isManaAbility().negate());
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
            if ((null == mana || false == mana) && isFaceDown() && state.getStateName() == CardStateName.FaceDown) {
                for (SpellAbility sa : getState(CardStateName.Original).getNonManaAbilities()) {
                    if (sa.isTurnFaceUp()) {
                        list.add(sa);
                    }
                }
            }
        } else if (hasState(CardStateName.Secondary) && state.getStateName() == CardStateName.Original) {
            // Adventure and Omen may only be cast not from Battlefield
            for (SpellAbility sa : getState(CardStateName.Secondary).getSpellAbilities()) {
                if (mana == null || mana == sa.isManaAbility()) {
                    list.add(sa);
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

    public final void setCollectible(boolean collectible) {
        this.collectible = collectible;
    }
    /**
     * Indicates whether this is a card in a player's collection that can be lost to an ante,
     * given away, ripped up, or is subject to any other effects beyond the scope of the match.
     * <p>
     * For cards that began in a player's deck, this is usually true. For tokens, cards that
     * are conjured, and game pieces that do not require ownership of a matching card (e.g.
     * sticker sheets and dungeons), this is usually false.
     * @return true if this card is part of its owner's collection.
     */
    public final boolean isCollectible() {
        return this.collectible;
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
        if(c != null) {
            currentState.setOracleText(c.getOracleText());
            currentState.setFunctionalVariantName(c.getCurrentState().getFunctionalVariantName());
        }
        //Could fetch the card rules oracle text in an "else" clause here,
        //but CardRules isn't aware of the card's state. May be better to
        //just stash the original oracle text if this comes up.
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
    public final void addPhaseOutCommand(final GameCommand c) {
        phaseOutCommandList.add(c);
    }
    public final void addLeavesPlayCommand(final GameCommand c) {
        leavePlayCommandList.add(c);
    }
 
    public void addStaticCommandList(Object[] objects) {
        staticCommandList.add(objects);
    }
    public List<Object[]> getStaticCommandList() {
        return staticCommandList;
    }

    public final List<GameCommand> getLeavesPlayCommands() {
        return leavePlayCommandList;
    }
    public final void setLeavesPlayCommands(List<GameCommand> list) {
        leavePlayCommandList = list;
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
    public final void runPhaseOutCommands() {
        for (final GameCommand c : phaseOutCommandList) {
            c.run();
        }
        phaseOutCommandList.clear();
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
        return !targetedFromThisTurn.isEmpty();
    }
    public void addTargetFromThisTurn(Player p) {
        targetedFromThisTurn.add(p);
    }
    public boolean isValiant(Player p) {
        return getController().equals(p) && !targetedFromThisTurn.contains(p);
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
    public final void setMayPlay(final Player player, final boolean withoutManaCost, final Cost altManaCost, final boolean withFlash, final boolean grantZonePermissions, final StaticAbility sta) {
        this.mayPlay.put(sta, new CardPlayOption(player, sta, withoutManaCost, altManaCost, withFlash, grantZonePermissions));
        this.updateMayPlay();
    }
    public final void removeMayPlay(final StaticAbility sta) {
        this.mayPlay.remove(sta);
        this.updateMayPlay();
    }
    public final Map<StaticAbility, CardPlayOption> getMayPlay() {
        return Maps.newHashMap(mayPlay);
    }
    public final Map<StaticAbility, CardPlayOption> setMayPlay(Map<StaticAbility, CardPlayOption> mp) {
        return mayPlay = mp;
    }

    public void resetMayPlayTurn() {
        for (StaticAbility sta : getStaticAbilities()) {
            sta.resetMayPlayTurn();
        }
    }

    public final CardCollectionView getEquippedBy() {
        return CardLists.filter(getAttachedCards(), CardPredicates.EQUIPMENT);
    }

    public final boolean isEquipped() {
        return getAttachedCards().anyMatch(CardPredicates.EQUIPMENT);
    }
    public final boolean isEquippedBy(Card c) {
        return this.hasCardAttachment(c);
    }
    public final boolean isEquippedBy(final String cardName) {
        return this.hasCardAttachment(cardName);
    }

    public final CardCollectionView getFortifiedBy() {
        return CardLists.filter(getAttachedCards(), CardPredicates.FORTIFICATION);
    }

    public final boolean isFortified() {
        return getAttachedCards().anyMatch(CardPredicates.FORTIFICATION);
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
        // 613.7e An Aura, Equipment, or Fortification receives a new timestamp each time it becomes attached to an object or player.
        setLayerTimestamp(getGame().getNextTimestamp());
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
            Card eff = SpellAbilityEffect.createEffect(sa, sa.getActivatingPlayer(), "Reconfigure Effect", getImageKey());
            eff.setRenderForUI(false);
            eff.addRemembered(this);

            String s = "Mode$ Continuous | AffectedDefined$ RememberedCard | EffectZone$ Command | RemoveType$ Creature";
            eff.addStaticAbility(s);

            GameCommand until = SpellAbilityEffect.exileEffectCommand(game, eff);
            addLeavesPlayCommand(until);
            addUnattachCommand(until);
            game.getAction().moveToCommand(eff, sa);
        }
    }

    public final void unattachFromEntity(final GameEntity entity) {
        unattachFromEntity(entity, entity);
    }
    public final void unattachFromEntity(final GameEntity entity, GameEntity old) {
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

        final Map<AbilityKey, Object> runParams = AbilityKey.newMap();
        runParams.put(AbilityKey.Attach, this);
        runParams.put(AbilityKey.Object, old);
        getGame().getTriggerHandler().runTrigger(TriggerType.Unattach, runParams, false);

        runUnattachCommands();
    }

    public final boolean isModified() {
        if (this.isEquipped() || this.hasCounters()) {
            return true;
        }
        return this.getEnchantedBy().anyMatch(CardPredicates.isController(this.getController()));
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
        return currentState.getTypeWithChanges();
    }

    public final CardTypeView getOriginalType() {
        return getOriginalType(currentState);
    }
    public final CardTypeView getOriginalType(CardState state) {
        return state.getType();
    }

    public Iterable<CardChangedType> getChangedCardTypes() {
        // If there are no changed types, just return an empty immutable list, which actually
        // produces a surprisingly large speedup by avoid lots of temp objects and making iteration
        // over the result much faster. (This function gets called a lot!)
        if (changedCardTypesByText.isEmpty() && changedTypeByText == null && changedCardTypesCharacterDefining.isEmpty() && changedCardTypes.isEmpty()) {
            return ImmutableList.of();
        }
        Iterable<CardChangedType> byText = changedTypeByText == null ? ImmutableList.of() : ImmutableList.of(this.changedTypeByText);
        return ImmutableList.copyOf(Iterables.concat(
                changedCardTypesByText.values(), // Layer 3
                byText, // Layer 3 by Word Changes,
                changedCardTypesCharacterDefining.values(), // Layer 4
                changedCardTypes.values() // Layer 6
            ));
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
            ImmutableList.of(changedCardKeywordsByWord), // Layer 3
            ImmutableList.of(new KeywordsChange(ImmutableList.<KeywordInterface>of(), ImmutableList.<KeywordInterface>of(), this.hasRemoveIntrinsic())), // Layer 4
            changedCardKeywords.values() // Layer 6
        );
    }

    public Table<Long, Long, KeywordsChange> getChangedCardKeywords() {
        return changedCardKeywords;
    }

    public void setChangedCardKeywords(Table<Long, Long, KeywordsChange> changedCardKeywords) {
        this.changedCardKeywords.clear();
        for (Table.Cell<Long, Long, KeywordsChange> entry : changedCardKeywords.cellSet()) {
            this.changedCardKeywords.put(entry.getRowKey(), entry.getColumnKey(), entry.getValue().copy(this, true));
        }
    }

    public final void addChangedCardTypesByText(final CardType addType, final long timestamp, final long staticId) {
        addChangedCardTypesByText(addType, timestamp, staticId, true);
    }
    public final void addChangedCardTypesByText(final CardType addType, final long timestamp, final long staticId, final boolean updateView) {
        changedCardTypesByText.put(timestamp, staticId, new CardChangedType(addType, null, false,
                EnumSet.of(RemoveType.SuperTypes, RemoveType.CardTypes, RemoveType.SubTypes)));

        // setting card type via text, does overwrite any other word change effects?
        this.changedTextColors.addEmpty(timestamp, staticId);
        this.changedTextTypes.addEmpty(timestamp, staticId);

        this.updateChangedText();

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

    public Iterable<CardColor> getChangedCardColors() {
        return Iterables.concat(changedCardColorsByText.values(), changedCardColorsCharacterDefining.values(), changedCardColors.values());
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

    public final void setColor(final String... color) {
        setColor(ColorSet.fromNames(color));
    }
    public final void setColor(final ColorSet color) {
        currentState.setColor(color);
    }

    public final ColorSet getColor() {
        return getColor(currentState);
    }
    public final ColorSet getColor(CardState state) {
        byte colors = state.getColor().getColor();
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

    public final Set<Integer> getAttractionLights() {
        return currentState.getAttractionLights();
    }
    public final void setAttractionLights(Set<Integer> attractionLights) {
        currentState.setAttractionLights(attractionLights);
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
        updateWorldTimestamp(timestamp);
    }

    public final boolean removeCloneState(final long timestamp) {
        if (clonedStates.remove(timestamp) != null) {
            updateCloneState(true);
            updateWorldTimestamp(timestamp);
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

    private void updateCloneState(final boolean updateView) {
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

    private CardCloneStates getLastClonedState() {
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
        addNewPT(power, toughness, timestamp, staticId, false, true);
    }
    public final void addNewPT(final Integer power, final Integer toughness, final long timestamp, final long staticId, final boolean cda, final boolean updateView) {
        (cda ? newPTCharacterDefining : newPT).put(timestamp, staticId, Pair.of(power, toughness));
        if (updateView) {
            updatePTforView();
        }
    }

    public final void removeNewPT(final long timestamp, final long staticId) {
        removeNewPT(timestamp, staticId, true);
    }
    public final void removeNewPT(final long timestamp, final long staticId, final boolean updateView) {
        boolean removed = false;

        removed |= newPTText.remove(timestamp, staticId) != null;
        removed |= newPT.remove(timestamp, staticId) != null;
        removed |= newPTCharacterDefining.remove(timestamp, staticId) != null;

        if (removed && updateView) {
            updatePTforView();
        }
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

    public final boolean assignNoCombatDamage() {
        return StaticAbilityAssignNoCombatDamage.assignNoCombatDamage(this);
    }

    // How much combat damage does the card deal
    public final int getNetCombatDamage() {
        return assignNoCombatDamage() ? 0 : (toughnessAssignsDamage() ? getNetToughnessBreakdown() : getNetPowerBreakdown()).getTotal();
    }

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

    public List<String> getDraftActions() {
        return draftActions;
    }

    public void addDraftAction(String s) {
        draftActions.add(s);
    }
 
    private int intensity = 0;
    public final void addIntensity(final int n) {
        intensity += n;
        view.updateIntensity(this);
    }
    public final int getIntensity(boolean total) {
        if (total && hasKeyword(Keyword.STARTING_INTENSITY)) {
            return getKeywordMagnitude(Keyword.STARTING_INTENSITY) + intensity;
        }
        return intensity;
    }
    public final void setIntensity(final int n) { intensity = n; }
    public final boolean hasIntensity() {
        return intensity > 0;
    }

    private List<PerpetualInterface> perpetual = new ArrayList<>();
    public final boolean hasPerpetual() {
        return !perpetual.isEmpty();
    }
    public final List<PerpetualInterface> getPerpetual() {
        return perpetual;
    }

    public final void addPerpetual(PerpetualInterface p) {
        perpetual.add(p);
    }

    public final void removePerpetual(final long timestamp) {
        PerpetualInterface toRemove = null;
        for (PerpetualInterface p : perpetual) {
            if (p.getTimestamp() == (timestamp)) {
                toRemove = p;
                break;
            }
        }
        perpetual.remove(toRemove);
    }

    public final void setPerpetual(final Card oldCard) {
        perpetual = oldCard.getPerpetual();
        for (PerpetualInterface p : perpetual) {
            p.applyEffect(this);
        }
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

    public final boolean canTap() {
        return canTap(false);
    }
    public final boolean canTap(boolean attacker) {
        if (tapped) { return false; }

        // Check replacement effects
        Map<AbilityKey, Object> repParams = AbilityKey.mapFromAffected(this);
        repParams.put(AbilityKey.IsCombat, attacker); // right name for parameter?
        return !getGame().getReplacementHandler().cantHappenCheck(ReplacementType.Tap, repParams);
    }

    public final boolean tap(boolean tapAnimation, SpellAbility cause, Player tapper) {
        return tap(false, tapAnimation, cause, tapper);
    }
    public final boolean tap(boolean attacker, boolean tapAnimation, SpellAbility cause, Player tapper) {
        if (tapped) { return false; }

        // Run replacement effects
        Map<AbilityKey, Object> repParams = AbilityKey.mapFromAffected(this);
        repParams.put(AbilityKey.IsCombat, attacker); // right name for parameter?

        switch (getGame().getReplacementHandler().run(ReplacementType.Tap, repParams)) {
        case NotReplaced:
            break;
        case Updated:
            break;
        default:
            return false;
        }

        final Map<AbilityKey, Object> runParams = AbilityKey.mapFromCard(this);
        runParams.put(AbilityKey.Attacker, attacker);
        runParams.put(AbilityKey.Cause, cause);
        runParams.put(AbilityKey.Player, tapper);
        getGame().getTriggerHandler().runTrigger(TriggerType.Taps, runParams, false);

        setTapped(true);
        view.updateNeedsTapAnimation(tapAnimation);
        getGame().fireEvent(new GameEventCardTapped(this, true));
        return true;
    }

    public final boolean canUntap(Player phase, boolean predict) {
        if (!predict && !tapped) { return false; }
        if (phase != null && isExertedBy(phase)) {
            return false;
        }
        if (phase != null && hasKeyword("This card doesn't untap during your next untap step.")) {
            return false;
        }
        Map<AbilityKey, Object> runParams = AbilityKey.mapFromAffected(this);
        runParams.put(AbilityKey.Player, phase);
        return !getGame().getReplacementHandler().cantHappenCheck(ReplacementType.Untap, runParams);
    }

    public final boolean untap() {
        return untap(null);
    }
    public final boolean untap(Player phase) {
        if (!tapped) { return false; }
        if (phase != null && isExertedBy(phase)) {
            return false;
        }

        Map<AbilityKey, Object> runParams = AbilityKey.mapFromAffected(this);
        runParams.put(AbilityKey.Player, phase);
        if (getGame().getReplacementHandler().run(ReplacementType.Untap, runParams) != ReplacementResult.NotReplaced) {
            return false;
        }

        getGame().getTriggerHandler().runTrigger(TriggerType.Untaps, AbilityKey.mapFromCard(this), false);

        runUntapCommands();
        setTapped(false);
        view.updateNeedsUntapAnimation(true);
        getGame().fireEvent(new GameEventCardTapped(this, false));
        return true;
    }

    public final SpellAbility getSpellAbilityForStaticAbility(final String str, final StaticAbility stAb) {
        SpellAbility result = storedSpellAbility.get(stAb, str);
        if (!canUseCachedTrait(result, stAb)) {
            result = AbilityFactory.getAbility(str, this, stAb);
            // apply text changes from the statics host
            result.changeTextIntrinsic(stAb.getChangedTextColors(), stAb.getChangedTextTypes());
            result.setIntrinsic(false);
            result.setGrantorStatic(stAb);
            storedSpellAbility.put(stAb, str, result);
        }
        return result;
    }

    public final Trigger getTriggerForStaticAbility(final String str, final StaticAbility stAb) {
        Trigger result = storedTrigger.get(stAb, str);
        if (!canUseCachedTrait(result, stAb)) {
            result = TriggerHandler.parseTrigger(str, this, false, stAb);
            // apply text changes from the statics host
            result.changeTextIntrinsic(stAb.getChangedTextColors(), stAb.getChangedTextTypes());
            storedTrigger.put(stAb, str, result);
        }
        return result;
    }

    public final Trigger addTriggerForStaticAbility(final Trigger trig, final StaticAbility stAb) {
        String str = trig.toString() + trig.getId();
        Trigger result = storedTrigger.get(stAb, str);
        if (result == null) {
            result = trig.copy(this, false);
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
        if (!canUseCachedTrait(result, stAb)) {
            result = ReplacementHandler.parseReplacement(str, this, false, stAb);
            // apply text changes from the statics host
            result.changeTextIntrinsic(stAb.getChangedTextColors(), stAb.getChangedTextTypes());
            storedReplacementEffect.put(stAb, str, result);
        }
        return result;
    }

    public final StaticAbility getStaticAbilityForStaticAbility(final String str, final StaticAbility stAb) {
        StaticAbility result = storedStaticAbility.get(stAb, str);
        if (!canUseCachedTrait(result, stAb)) {
            result = StaticAbility.create(str, this, stAb.getCardState(), false);
            // apply text changes from the statics host
            result.changeTextIntrinsic(stAb.getChangedTextColors(), stAb.getChangedTextTypes());
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
            result.setStatic(stAb);
            result.setIdx(idx);
            result.setIntrinsic(true);
            storedKeywordByText.put(triple, result);
        }
        return result;
    }

    private boolean canUseCachedTrait(CardTraitBase cached, CardTraitBase stAb) {
        if (cached == null) {
            return false;
        }
        return cached.getChangedTextColors().equals(stAb.getChangedTextColors()) && cached.getChangedTextTypes().equals(stAb.getChangedTextTypes());
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
        updateAbilityTextForView();
    }

    public final CardTraitChanges addChangedCardTraits(Collection<SpellAbility> spells, Collection<SpellAbility> removedAbilities,
            Collection<Trigger> trigger, Collection<ReplacementEffect> replacements, Collection<StaticAbility> statics,
            boolean removeAll, boolean removeNonMana, long timestamp, long staticId) {
        return addChangedCardTraits(spells, removedAbilities, trigger, replacements, statics, removeAll, removeNonMana, timestamp, staticId, true);
    }
    public final CardTraitChanges addChangedCardTraits(Collection<SpellAbility> spells, Collection<SpellAbility> removedAbilities,
            Collection<Trigger> trigger, Collection<ReplacementEffect> replacements, Collection<StaticAbility> statics,
            boolean removeAll, boolean removeNonMana, long timestamp, long staticId, boolean updateView) {
        CardTraitChanges result = new CardTraitChanges(
            spells, removedAbilities, trigger, replacements, statics, removeAll, removeNonMana
        );
        changedCardTraits.put(timestamp, staticId, result);
        if (updateView) {
            updateAbilityTextForView();
        }
        return result;
    }

    public final void addChangedCardTraits(CardTraitChanges ctc, long timestamp, long staticId) {
        changedCardTraits.put(timestamp, staticId, ctc);
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
        if (!changedCardTraitsByText.isEmpty()) {
            changed = true;
        }
        changedCardTraitsByText.clear();
        if (!changedCardTraits.isEmpty()) {
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
        // for Zilortha
        getView().updateLethalDamage(this);
    }

    public final void addChangedCardKeywords(final List<String> keywords, final List<String> removeKeywords,
            final boolean removeAllKeywords, final long timestamp, final StaticAbility st) {
        addChangedCardKeywords(keywords, removeKeywords, removeAllKeywords, timestamp, st, true);
    }
    public final void addChangedCardKeywords(final List<String> keywords, final List<String> removeKeywords,
            final boolean removeAllKeywords, final long timestamp, final StaticAbility st, final boolean updateView) {
        List<KeywordInterface> kws = Lists.newArrayList();
        if (keywords != null) {
            long idx = 1;
            for (String kw : keywords) {
                // CR 113.11
                boolean canHave = true;
                for (Keyword cantKW : getCantHaveKeyword()) {
                    if (kw.startsWith(cantKW.toString())) {
                        canHave = false;
                        break;
                    }
                }
                if (canHave) {
                    kws.add(getKeywordForStaticAbility(kw, st, idx));
                }
                idx++;
            }
        }

        final KeywordsChange newCks = new KeywordsChange(kws, removeKeywords, removeAllKeywords);
        changedCardKeywords.put(timestamp, st == null ? 0l : st.getId(), newCks);

        if (updateView) {
            updateKeywords();
        }
    }

    public final KeywordInterface getKeywordForStaticAbility(String kw, final StaticAbility st, final long idx) {
        KeywordInterface result;
        long staticId = st == null ? 0 : st.getId();
        Triple<String, Long, Long> triple = Triple.of(kw, staticId, idx);
        if (staticId < 1 || !storedKeywords.containsKey(triple)) {
            result = Keyword.getInstance(kw);
            result.setStatic(st);
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
        if (kw.getStatic() != null) {
            storedKeywords.put(Triple.of(kw.getOriginal(), (long)kw.getStatic().getId(), kw.getIdx()), kw);
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

    private KeywordInterface getCopyForStoredKeyword(Map.Entry<Triple<String, Long, Long>, KeywordInterface> e, boolean lki) {
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

    public void setChangedCardKeywordsByText(Table<Long, Long, KeywordsChange> changedCardKeywords) {
        this.changedCardKeywordsByText.clear();
        for (Table.Cell<Long, Long, KeywordsChange> entry : changedCardKeywords.cellSet()) {
            this.changedCardKeywordsByText.put(entry.getRowKey(), entry.getColumnKey(), entry.getValue().copy(this, true));
        }
    }

    public final void addChangedCardKeywordsInternal(
        final Collection<KeywordInterface> keywords, final Collection<KeywordInterface> removeKeywords,
        final boolean removeAllKeywords,
        final long timestamp, final StaticAbility st, final boolean updateView) {
        final KeywordsChange newCks = new KeywordsChange(keywords, removeKeywords, removeAllKeywords);
        long staticId = st == null ? 0 : st.getId();
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
        if (state.getStateName().equals(CardStateName.Original)) {
            if (hasState(CardStateName.LeftSplit)) {
                keywords.insertAll(getState(CardStateName.LeftSplit).getIntrinsicKeywords());
            }
            if (hasState(CardStateName.RightSplit)) {
                keywords.insertAll(getState(CardStateName.RightSplit).getIntrinsicKeywords());
            }
        }

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
            if (parse.length < 2) {
                count++;
                continue;
            }
            final String s = parse[1];
            if (StringUtils.isNumeric(s)) {
               count += Integer.parseInt(s);
            } else {
                // TODO make keywordinterface inherit from CardTrait somehow, or invent new interface
                if (inst.hasSVar(s)) {
                    count += AbilityUtils.calculateAmount(this, inst.getSVar(s), null);
                } else {
                    String svar = StringUtils.join(parse);
                    if (state.hasSVar(svar)) {
                        count += AbilityUtils.calculateAmount(this, state.getSVar(svar), null);
                    }
                }
            }
        }
        return count;
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
        }
        return change;
    }

    public Collection<Keyword> getCantHaveKeyword() {
        return cantHaveKeywords.values();
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
                newKw.createTraits(this, true);
                addKeywords.add(newKw);
                removeKeywords.add(kw);
            } else if (oldtxt.startsWith("Class")) {
                for (StaticAbility trait : kw.getStaticAbilities()) {
                    trait.changeText();
                }
            } else if (oldtxt.startsWith("Chapter")) {
                for (Trigger trait : kw.getTriggers()) {
                    trait.changeText();
                }
            }
        }

        changedCardKeywordsByWord = new KeywordsChange(addKeywords, removeKeywords, false);

        text = AbilityUtils.applyDescriptionTextChangeEffects(originalText, this);

        getView().updateChangedColorWords(this);
        getView().updateChangedTypes(this);
        updateManaCostForView();

        updateAbilityTextForView();
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

    public final boolean isPermanent() {
        return !isImmutable() && (isInPlay() || getType().isPermanent());
    }

    public final boolean isSpell() {
        return isInstant() || isSorcery() || (isAura() && !isInZone(ZoneType.Battlefield));
    }

    public final boolean hasPlayableLandFace() { return isLand() || (isModal() && getState(CardStateName.Backside).getType().isLand()); }

    public final boolean isLand()       { return getType().isLand(); }
    public final boolean isBasicLand()  { return getType().isBasicLand(); }
    public final boolean isSnow()       { return getType().isSnow(); }

    public final boolean isKindred()     { return getType().isKindred(); }
    public final boolean isSorcery()    { return getType().isSorcery(); }
    public final boolean isInstant()    { return getType().isInstant(); }
    public final boolean isInstantOrSorcery() {return getType().isInstant() || getType().isSorcery();}

    public final boolean isCreature()   { return getType().isCreature(); }
    public final boolean isArtifact()   { return getType().isArtifact(); }
    public final boolean isPlaneswalker()   { return getType().isPlaneswalker(); }
    public final boolean isBattle()      { return getType().isBattle(); }
    public final boolean isEnchantment()    { return getType().isEnchantment(); }

    public final boolean isEquipment()  { return getType().isEquipment(); }
    public final boolean isFortification()  { return getType().isFortification(); }
    public final boolean isAttraction()     { return getType().isAttraction(); }
    public final boolean isContraption()    { return getType().isContraption(); }
    public final boolean isCurse()          { return getType().hasSubtype("Curse"); }
    public final boolean isAura()           { return getType().isAura(); }
    public final boolean isShrine()           { return getType().hasSubtype("Shrine"); }
    public final boolean isSaga()           { return getType().isSaga(); }

    public final boolean isAttachment() { return getType().isAttachment(); }
    public final boolean isHistoric()   { return getType().isHistoric(); }

    public final boolean isScheme()     { return getType().isScheme(); }
    public final boolean isPhenomenon() { return getType().isPhenomenon(); }
    public final boolean isPlane()      { return getType().isPlane(); }

    public final boolean isOutlaw()     { return getType().isOutlaw(); }

    public final boolean isRoom()       { return getType().hasSubtype("Room"); }

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
                if (!eq.isPhasedOut() && StaticAbilityCantPhase.cantPhaseOut(eq)) {
                    continue;
                }
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
        if (isPhasedOut() && fromUntapStep && wontPhaseInNormal) {
            return false;
        }

        final Map<AbilityKey, Object> runParams = AbilityKey.mapFromCard(this);

        if (!isPhasedOut()) {
            // If this is currently PhasedIn, it's about to phase out.
            // Run trigger before it does because triggers don't work with phased out objects
            getGame().getTriggerHandler().runTrigger(TriggerType.PhaseOut, runParams, true);
            // CR 702.26f
            runPhaseOutCommands();

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
            getGame().getTriggerHandler().runTrigger(TriggerType.PhaseIn, runParams, true);
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
        } else if (incR[0].equals("Boon")) {
            if (!isBoon()) {
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

    public final boolean hasNoName() {
        return !hasNonLegendaryCreatureNames() && getName().isEmpty();
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
        if (!shares && !hasNameOverwrite()) {
            if (isInPlay()) {
               // split cards in play are only rooms
               for (String door : getUnlockedRoomNames()) {
                   shares |= name.equals(door);
               }
            } else { // not on the battlefield
                if (hasState(CardStateName.LeftSplit)) {
                    shares |= name.equals(getState(CardStateName.LeftSplit).getName());
                }
                if (hasState(CardStateName.RightSplit)) {
                    shares |= name.equals(getState(CardStateName.RightSplit).getName());
                }
            }
            // TODO does it need extra check for stack?
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

    public final boolean sharesAllCardTypesWith(final Card c1) {
        if (c1 == null) {
            return false;
        }
        return getType().sharesAllCardTypesWith(c1.getType());
    }

    public final boolean sharesControllerWith(final Card c1) {
        return c1 != null && getController().equals(c1.getController());
    }

    public final boolean hasABasicLandType() {
        return getType().hasABasicLandType();
    }
    public final boolean hasANonBasicLandType() {
        return getType().hasANonBasicLandType();
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
                excessCharacteristics.add(max(0, lethal));
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
    public final void logExcessDamage(final int n) {
        excessDamageThisTurnAmount += n;
    }
    public final int getExcessDamageThisTurn() {
        return excessDamageThisTurnAmount;
    }
    public final void setExcessDamageReceivedThisTurn(final int n) {
        excessDamageThisTurnAmount = n;
    }
    private void resetExcessDamage() {
        hasBeenDealtExcessDamageThisTurn = false;
        excessDamageThisTurnAmount = 0;
    }

    public final Map<Card, Integer> getAssignedDamageMap() {
        return assignedDamageMap;
    }

    public final void addAssignedDamage(int assignedDamage0, final Card sourceCard) {
        // 510.1a Creatures that would assign 0 or less damage don't assign combat damage at all.
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

        // 120.1a Damage can't be dealt to an object that’s neither a creature nor a planeswalker nor a battle.
        if (!isPlaneswalker() && !isCreature() && !isBattle()) {
            return 0;
        }

        if (!isInPlay()) { // if target is not in play it can't receive any damage
            return 0;
        }

        getGame().getReplacementHandler().run(ReplacementType.DealtDamage, AbilityKey.mapFromAffected(this));

        Map<AbilityKey, Object> runParams = AbilityKey.newMap();
        runParams.put(AbilityKey.DamageSource, source);
        runParams.put(AbilityKey.DamageTarget, this);
        runParams.put(AbilityKey.Cause, cause);
        runParams.put(AbilityKey.DamageAmount, damageIn);
        runParams.put(AbilityKey.IsCombatDamage, isCombat);
        // Defending player at the time the damage was dealt
        runParams.put(AbilityKey.DefendingPlayer, game.getCombat() != null ? game.getCombat().getDefendingPlayerRelatedTo(source) : null);
        getGame().getTriggerHandler().runTrigger(TriggerType.DamageDone, runParams, true);

        DamageType damageType = DamageType.Normal;
        if (isPlaneswalker()) { // 120.3c
            subtractCounter(CounterEnumType.LOYALTY, damageIn, null, true);
        }
        if (isBattle()) {
            subtractCounter(CounterEnumType.DEFENSE, damageIn, null, true);
        }
        if (isCreature()) {
            if (source.isWitherDamage()) { // 120.3d
                addCounter(CounterEnumType.M1M1, damageIn, source.getController(), counterTable);
                damageType = DamageType.M1M1Counters;
            }
            else { // 120.3e
                int old = damage.getOrDefault(Objects.hash(source.getId(), source.getGameTimestamp()), 0);
                damage.put(Objects.hash(source.getId(), source.getGameTimestamp()), old + damageIn);
                view.updateDamage(this);
            }

            if (source.hasKeyword(Keyword.DEATHTOUCH)) {
                setHasBeenDealtDeathtouchDamage(true);
                damageType = DamageType.Deathtouch;
            }

            // Play the Damage sound
            game.fireEvent(new GameEventCardDamaged(this, source, damageIn, damageType));
        }

        return damageIn;
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

    public CardEdition.BorderColor borderColor() {
        CardEdition edition = StaticData.instance().getEditions().get(getSetCode());
        if (edition == null || isBasicLand()) {
            return CardEdition.BorderColor.BLACK;
        }
        return edition.getBorderColor();
    }

    public final String getMostRecentSet() {
        return StaticData.instance().getCommonCards().getCard(getPaperCard().getName()).getEdition();
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

    public final String getImageKey() {
        if (!getRenderForUI()) {
            return "";
        }
        Card uiCard = getCardForUi();
        if(uiCard == null)
            return "";
        return uiCard.currentState.getImageKey();
    }
    public final void setImageKey(final String iFN) {
        if (!getRenderForUI()) {
            return;
        }
        Card uiCard = getCardForUi();
        if(uiCard != null)
            uiCard.currentState.setImageKey(iFN);
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
        if (!getRenderForUI()) {
            return "";
        }
        Card uiCard = getCardForUi();
        if(uiCard == null)
            return "";
        CardState c = uiCard.states.get(state);
        return (c != null ? c.getImageKey() : "");
    }

    public final String getFacedownImageKey() {
        if (isInZone(ZoneType.Exile)) {
            if (isForetold()) {
                return StaticData.instance().getOtherImageKey(ImageKeys.FORETELL_IMAGE, null);
            }
            return ImageKeys.getTokenKey(ImageKeys.HIDDEN_CARD);
        }

        if (isManifested()) {
            String set = getManifestedSA().getCardState().getSetCode();
            return StaticData.instance().getOtherImageKey(ImageKeys.MANIFEST_IMAGE, set);
        }
        if (isCloaked()) {
            String set = getCloakedSA().getCardState().getSetCode();
            return StaticData.instance().getOtherImageKey(ImageKeys.CLOAKED_IMAGE, set);
        }
        if (getCastSA() != null) {
            String set = getCastSA().getCardState().getSetCode();
            if (getCastSA().isKeyword(Keyword.DISGUISE)) {
                return StaticData.instance().getOtherImageKey(ImageKeys.CLOAKED_IMAGE, set);
            } else if (getCastSA().isKeyword(Keyword.MORPH) || getCastSA().isKeyword(Keyword.MEGAMORPH)) {
                return StaticData.instance().getOtherImageKey(ImageKeys.MORPH_IMAGE, set);
            }
        }
        // TODO add face-down SA to key

        return ImageKeys.getTokenKey(ImageKeys.HIDDEN_CARD);
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
        return sa != null && sa.isEmbalm();
    }

    public final boolean isEternalized() {
        SpellAbility sa = getTokenSpawningAbility();
        return sa != null && sa.isEternalize();
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
        // removeExertedBy is called on Untap phase, where it can't be exerted yet
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

    public final boolean getEnlistedThisCombat()  {
        return enlistedThisCombat;
    }
    public final void setEnlistedThisCombat(final boolean b) {
        enlistedThisCombat = b;
    }

    public boolean wasDiscarded() { return discarded; }
    public void setDiscarded(boolean state) { discarded = state; }
    public boolean wasSurveilled() {
        return this.surveilled;
    }
    public void setSurveilled(boolean value) {
        this.surveilled = value;
    }
    public boolean wasMilled() {
        return milled;
    }
    public void setMilled(boolean value) {
        milled = value;
    }

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

    public final boolean isHarnessed() {
        return harnessed;
    }
    public final boolean setHarnessed(final boolean harnessed0) {
        harnessed = harnessed0;
        return true;
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

    public final boolean isSolved() {
        return solved;
    }
    public final boolean setSolved(final boolean solved) {
        this.solved = solved;
        return true;
    }

    public final int getTimesSaddledThisTurn() {
        return timesSaddledThisTurn;
    }
    public final CardCollection getSaddledByThisTurn() {
        return saddledByThisTurn;
    }
    public final void addSaddledByThisTurn(final CardCollection saddlers) {
        if (saddledByThisTurn != null) saddledByThisTurn.addAll(saddlers);
        else saddledByThisTurn = saddlers;
    }
    public final void setSaddledByThisTurn(final CardCollection saddlers) {
        saddledByThisTurn = saddlers;
    }
    public void resetSaddled() {
        final boolean changed = isSaddled();
        setSaddled(false);
        if (saddledByThisTurn != null) saddledByThisTurn = null;
        timesSaddledThisTurn = 0;
        if (changed) updateAbilityTextForView();
    }
    public final boolean isSaddled() {
        return saddled;
    }
    public final boolean setSaddled(final boolean saddled) {
        this.saddled = saddled;
        if (saddled) timesSaddledThisTurn++;
        return true;
    }

    public Card getSuspectedEffect() {
        return this.suspectedEffect;
    }
    public void setSuspectedEffect(Card effect) {
        this.suspectedEffect = effect;
    }

    public final boolean isSuspected() {
        return suspectedEffect != null;
    }

    public final boolean setSuspected(final boolean suspected) {
        if (suspected && StaticAbilityCantBeSuspected.cantBeSuspected(this)) {
            return false;
        }
        if (suspected) {
            if (isSuspected()) {
                // 701.58d A suspected permanent can’t become suspected again.
                return true;
            }

            suspectedEffect = SpellAbilityEffect.createEffect(null, this, this.getController(), "Suspected Effect", getImageKey(), getGame().getNextTimestamp());
            suspectedEffect.setRenderForUI(false);
            suspectedEffect.addRemembered(this);

            String s = "Mode$ Continuous | AffectedDefined$ RememberedCard | EffectZone$ Command | AddKeyword$ Menace | AddStaticAbility$ SuspectedCantBlockBy";
            StaticAbility suspectedStatic = suspectedEffect.addStaticAbility(s);
            String effect = "Mode$ CantBlock | ValidCard$ Creature.Self | Description$ CARDNAME can't block.";
            suspectedStatic.setSVar("SuspectedCantBlockBy", effect);

            GameCommand until = SpellAbilityEffect.exileEffectCommand(getGame(), suspectedEffect);
            addLeavesPlayCommand(until);
            getGame().getAction().moveToCommand(suspectedEffect, null);
        } else {
            if (isSuspected()) {
                getGame().getAction().exileEffect(suspectedEffect);
                suspectedEffect = null;
            }
        }
        return true;
    }

    public final boolean isManifested() {
        return manifestedSA != null;
    }
    public final SpellAbility getManifestedSA() {
        return manifestedSA;
    }
    public final void setManifested(final SpellAbility sa) {
        this.manifestedSA = sa;
    }

    public final boolean isCloaked() {
        return cloakedSA != null;
    }
    public final SpellAbility getCloakedSA() {
        return cloakedSA;
    }
    public final void setCloaked(final SpellAbility sa) {
        this.cloakedSA = sa;
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

    public final boolean isPlotted() {
        return this.plotted;
    }
    public final boolean setPlotted(final boolean plotted) {
        this.plotted = plotted;
        if (plotted == true && !isLKI()) {
            final Map<AbilityKey, Object> runParams = AbilityKey.mapFromCard(this);
            game.getTriggerHandler().runTrigger(TriggerType.BecomesPlotted, runParams, false);
        }
        return true;
    }

    public boolean isForetoldCostByEffect() {
        return foretoldCostByEffect;
    }
    public void setForetoldCostByEffect(final boolean val) {
        this.foretoldCostByEffect = val;
    }

    public boolean isWarped() {
        if (!isInZone(ZoneType.Exile)) {
            return false;
        }
        if (exiledSA == null) {
            return false;
        }
        return exiledSA.isKeyword(Keyword.WARP);
    }

    public boolean isWebSlinged() {
        return getCastSA() != null & getCastSA().isAlternativeCost(AlternativeCost.WebSlinging);
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

    public boolean canCrew() {
        return canTap() && !StaticAbilityCantCrew.cantCrew(this);
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
        CardCollection crew = sa.getPaidList("Tapped", true);
        addCrewedByThisTurn(crew);
        Map<AbilityKey, Object> runParams = AbilityKey.mapFromCard(this);
        runParams.put(AbilityKey.Crew, crew);
        game.getTriggerHandler().runTrigger(TriggerType.BecomesCrewed, runParams, false);
    }
    public void resetCrewed() {
        resetTimesCrewedThisTurn();
        if (crewedByThisTurn != null) crewedByThisTurn = null;
    }

    public final void addCrewedByThisTurn(final CardCollection crew) {
        if (crewedByThisTurn != null) crewedByThisTurn.addAll(crew);
        else crewedByThisTurn = crew;
    }
    public final CardCollectionView getCrewedByThisTurn() {
        if (crewedByThisTurn == null) {
            return CardCollection.EMPTY;
        }
        return crewedByThisTurn;
    }
    public final void setCrewedByThisTurn(final CardCollectionView crew) {
        crewedByThisTurn = new CardCollection(crew);
    }

    public final void visitAttraction(Player visitor) {
        this.visitedThisTurn = true;

        final Map<AbilityKey, Object> runParams = AbilityKey.mapFromCard(this);
        runParams.put(AbilityKey.Player, visitor);
        game.getTriggerHandler().runTrigger(TriggerType.VisitAttraction, runParams, false);
    }
    public final boolean wasVisitedThisTurn() {
        return this.visitedThisTurn;
    }

    public final int getClassLevel() {
        return classLevel;
    }
    public void setClassLevel(int level) {
        classLevel = level;
        view.updateClassLevel(this);
        updateAbilityTextForView();
    }
    public boolean isClassCard() {
        return getType().hasStringType("Class");
    }

    /**
     * Displays a string as an overlay on top of this card (similar to the way counter text is shown).
     */
    public void setOverlayText(String overlayText) {
        this.overlayText = overlayText;
        view.updateMarkerText(this);
    }
    public String getOverlayText() {
        return this.overlayText;
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
        addChangedCardKeywords(Collections.singletonList("Enchant:Creature"), Lists.newArrayList(),
                false, bestowTimestamp, null, updateView);
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

    public final long getGameTimestamp() {
        return gameTimestamp;
    }
    public final void setGameTimestamp(final long t) {
        gameTimestamp = t;
        // 613.7d An object receives a timestamp at the time it enters a zone.
        layerTimestamp = t;
    }

    public final long getLayerTimestamp() {
        return layerTimestamp;
    }
    public final void setLayerTimestamp(final long t) {
        layerTimestamp = t;
    }

    public boolean equalsWithGameTimestamp(Card c) {
        return equals(c) && c.getGameTimestamp() == gameTimestamp;
    }

    public long getWorldTimestamp() {
        return worldTimestamp;
    }
    public void updateWorldTimestamp(long ts) {
        if (!getType().hasSupertype(Supertype.World)) {
            worldTimestamp = -1;
        } else if (worldTimestamp == -1) {
            worldTimestamp = ts;
        }
    }

    public String getProtectionKey() {
        String protectKey = "";
        boolean pR = false; boolean pG = false; boolean pB = false; boolean pU = false; boolean pW = false;
        for (final KeywordInterface inst : getKeywords(Keyword.PROTECTION)) {
            String kw = inst.getOriginal();
            if (kw.equals("Protection from red") || kw.contains(":red")) {
                if (!pR) {
                    pR = true;
                    protectKey += "R";
                }
            } else if (kw.equals("Protection from green") || kw.contains(":green")) {
                if (!pG) {
                    pG = true;
                    protectKey += "G";
                }
            } else if (kw.equals("Protection from black") || kw.contains(":black")) {
                if (!pB) {
                    pB = true;
                    protectKey += "B";
                }
            } else if (kw.equals("Protection from blue") || kw.contains(":blue")) {
                if (!pU) {
                    pU = true;
                    protectKey += "U";
                }
            } else if (kw.equals("Protection from white") || kw.contains(":white")) {
                if (!pW) {
                    pW = true;
                    protectKey += "W";
                }
            } else if (kw.contains("each color")) {
                protectKey += "allcolors:";
            } else if (kw.equals("Protection from everything")) {
                protectKey += "everything:";
            } else if (kw.contains("colored spells")) {
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

    public boolean isInZones(final List<ZoneType> zones) {
        Zone z = this.getLastKnownZone();
        return z != null && zones.contains(z.getZoneType());
    }

    public boolean canBeDiscardedBy(SpellAbility sa, final boolean effect) {
        if (!isInZone(ZoneType.Hand)) {
            return false;
        }

        return getOwner().canDiscardBy(sa, effect);
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

        if (isPhasedOut()) {
            return false;
        }

        if (StaticAbilityCantTarget.cantTarget(this, sa) != null) {
            return false;
        }

        return true;
    }

    public final boolean canBeControlledBy(final Player newController) {
        return newController.isInGame() && !(hasKeyword("Other players can't gain control of CARDNAME.") && !getController().equals(newController));
    }

    @Override
    protected final boolean canBeEnchantedBy(final Card aura) {
        if (!aura.hasKeyword(Keyword.ENCHANT)) {
            return false;
        }
        for (KeywordInterface ki : aura.getKeywords(Keyword.ENCHANT)) {
            String k = ki.getOriginal();
            String m[] = k.split(":");
            String v = m[1];
            if (!isValid(v.split(","), aura.getController(), aura, null)) {
                return false;
            }
            if (!v.contains("inZone") && !isInPlay()) {
                return false;
            }
        }
        return true;
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

    public final boolean canBeSacrificedBy(final SpellAbility source, final boolean effect) {
        if (isImmutable()) {
            System.out.println("Trying to sacrifice immutables: " + this);
            return false;
        }

        if (!isInPlay() || isPhasedOut()) {
            return false;
        }

        // can't sacrifice it for mana ability if it is already marked as sacrifice
        if (source != null && source.isManaAbility() && isUsedToPay()) {
            return false;
        }

        final Card gameCard = game.getCardState(this, null);
        // gameCard is LKI in that case, the card is not in game anymore
        // or the timestamp did change
        // this should check Self too
        if (gameCard == null || !this.equalsWithGameTimestamp(gameCard)) {
            return false;
        }

        return !StaticAbilityCantSacrifice.cantSacrifice(this, source, effect);
    }

    public final boolean canExiledBy(final SpellAbility source, final boolean effect) {
        return !StaticAbilityCantExile.cantExile(this, source, effect);
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

    public FCollectionView<ReplacementEffect> getReplacementEffects() {
        return currentState.getReplacementEffects();
    }

    public ReplacementEffect addReplacementEffect(final ReplacementEffect replacementEffect) {
        currentState.addReplacementEffect(replacementEffect);
        return replacementEffect;
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
                String reStr = "Event$ Destroy | ActiveZones$ Battlefield | ValidCard$ Card.Self | ValidCause$ SpellAbility | Secondary$ True | ShieldCounter$ True "
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
                String reStr = "Event$ Untap | ActiveZones$ Battlefield | ValidCard$ Card.Self | Secondary$ True "
            + "| Description$ If this permanent would become untapped, instead remove a stun counter from it.";

                stunCounterReplaceUntap = ReplacementHandler.parseReplacement(reStr, this, false, null);
                stunCounterReplaceUntap.setOverridingAbility(AbilityFactory.getAbility(sa, this));
            }
            list.add(stunCounterReplaceUntap);
        }
        if (getCounters(CounterEnumType.FINALITY) > 0) {
            if (finalityCounterReplaceDying == null) {
                String reStr = "Event$ Moved | ActiveZones$ Battlefield | Origin$ Battlefield | Destination$ Graveyard | ValidCard$ Card.Self | Secondary$ True "
            + " | Description$ If CARDNAME would die, exile it instead.";
                String sa = "DB$ ChangeZone | Origin$ Battlefield | Destination$ Exile | Defined$ ReplacedCard";

                finalityCounterReplaceDying = ReplacementHandler.parseReplacement(reStr, this, false, null);
                finalityCounterReplaceDying.setOverridingAbility(AbilityFactory.getAbility(sa, this));
            }
            list.add(finalityCounterReplaceDying);
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
        setEnlistedThisCombat(false);
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
        resetExcessDamage();
        setRegeneratedThisTurn(0);
        resetShieldCount();
        targetedFromThisTurn.clear();
        setFoughtThisTurn(false);
        turnedFaceUpThisTurn = false;
        clearMustBlockCards();
        getDamageHistory().setCreatureAttackedLastTurnOf(turn, getDamageHistory().getCreatureAttacksThisTurn() > 0);
        getDamageHistory().newTurn();
        damageReceivedThisTurn.clear();
        clearBlockedByThisTurn();
        clearBlockedThisTurn();
        resetMayPlayTurn();
        resetExertedThisTurn();
        resetCrewed();
        resetSaddled();
        visitedThisTurn = false;
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
        } else if (currentStateName == CardStateName.Backside && !isModal()) {
            // Except in the cases were we clone the back-side of a DFC.
            if (getCopiedPermanent() != null) {
                return 0;
            }
            requestedCMC = getState(CardStateName.Original).getManaCost().getCMC();
        } else if (currentStateName == CardStateName.Meld) {
            // to follow the rules (but we shouldn't get here while cloned)
            if (getCopiedPermanent() != null) {
                return 0;
            }
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

    public CardRules getRules() {
        return cardRules;
    }
    public void setRules(CardRules r) {
        cardRules = r;
        currentState.getView().updateRulesText(r, getType());
    }

    @Override
    public Game getGame() {
        return game;
    }

    public void dangerouslySetGame(Game newGame) {
        game = newGame;
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
        if (sa.hasParam("Prototype") && prototypeTimestamp == -1) {
            long next = game.getNextTimestamp();
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
            CardFactoryUtil.setFaceDownState(this, sa);
        }
    }

    public boolean isOptionalCostPaid(OptionalCost cost) { return getCastSA() == null ? false : getCastSA().isOptionalCostPaid(cost); }

    public final int getKickerMagnitude() {
        if (this.getCastSA() != null && getCastSA().hasOptionalKeywordAmount(Keyword.MULTIKICKER)) {
            return getCastSA().getOptionalKeywordAmount(Keyword.MULTIKICKER);
        }
        boolean hasK1 = isOptionalCostPaid(OptionalCost.Kicker1);
        return hasK1 == isOptionalCostPaid(OptionalCost.Kicker2) ? (hasK1 ? 2 : 0) : 1;
    }

    public List<SpellAbility> getAllPossibleAbilities(final Player player, final boolean removeUnplayable) {
        CardState oState = getState(CardStateName.Original);
        final List<SpellAbility> abilities = Lists.newArrayList();
        for (SpellAbility sa : getSpellAbilities()) {
            if (sa.isAdventure() && isOnAdventure()) {
                continue; // skip since it's already on adventure
            }
            //add alternative costs as additional spell abilities
            abilities.add(sa);
            abilities.addAll(GameActionUtil.getAlternativeCosts(sa, player, false));
        }

        if (isFaceDown() && isInZone(ZoneType.Exile)) {
            for (final SpellAbility sa : oState.getSpellAbilities()) {
                abilities.addAll(GameActionUtil.getAlternativeCosts(sa, player, false));
            }
        }
        if (isFaceDown() && isInZone(ZoneType.Command)) {
            for (KeywordInterface k : oState.getCachedKeyword(Keyword.HIDDEN_AGENDA)) {
                abilities.addAll(k.getAbilities());
            }
            for (KeywordInterface k : oState.getCachedKeyword(Keyword.DOUBLE_AGENDA)) {
                abilities.addAll(k.getAbilities());
            }
        }
        // Add Modal Spells
        if (isModal() && hasState(CardStateName.Backside)) {
            for (SpellAbility sa : getState(CardStateName.Backside).getSpellAbilities()) {
                //add alternative costs as additional spell abilities
                // only add Spells there
                if (sa.isSpell() || sa.isLandAbility()) {
                    abilities.add(sa);
                    abilities.addAll(GameActionUtil.getAlternativeCosts(sa, player, false));
                }
            }
        }

        if (isInPlay() && !isPhasedOut() && player.canCastSorcery()) {
            if (getCurrentStateName() == CardStateName.RightSplit || getCurrentStateName() == CardStateName.EmptyRoom) {
                abilities.add(getUnlockAbility(CardStateName.LeftSplit));
            }
            if (getCurrentStateName() == CardStateName.LeftSplit || getCurrentStateName() == CardStateName.EmptyRoom) {
                abilities.add(getUnlockAbility(CardStateName.RightSplit));
            }
        }

        if (isInPlay() && isFaceDown() && oState.getType().isCreature() && oState.getManaCost() != null && !oState.getManaCost().isNoCost())
        {
            if (isManifested()) {
                abilities.add(oState.getManifestUp());
            }
            if (isCloaked()) {
                abilities.add(oState.getCloakUp());
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

    public boolean getRenderForUI() {
        return this.renderForUi;
    }
    public void setRenderForUI(boolean value) {
        renderForUi = value;
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

    public String getOracleText() {
        return currentState.getOracleText();
    }
    public void setOracleText(final String oracleText) {
        currentState.setOracleText(oracleText);
    }

    @Override
    public String getTranslationKey() {
        return currentState.getTranslationKey();
    }
    @Override
    public String getUntranslatedName() {
        return this.getName();
    }
    @Override
    public String getUntranslatedType() {
        return currentState.getUntranslatedType();
    }
    @Override
    public String getUntranslatedOracle() {
        return currentState.getUntranslatedOracle();
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

    public void cleanupCopiedChangesFrom(Card c) {
        for (StaticAbility stAb : c.getStaticAbilities()) {
            this.removeChangedCardTypes(c.getLayerTimestamp(), stAb.getId(), false);
            this.removeColor(c.getLayerTimestamp(), stAb.getId());
            this.removeChangedCardKeywords(c.getLayerTimestamp(), stAb.getId(), false);
            this.removeChangedCardTraits(c.getLayerTimestamp(), stAb.getId());
        }
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

    public final void unGoad() {
        goad = Maps.newTreeMap();
        updateAbilityTextForView();
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

    public final boolean hasChapter() {
        return getCurrentState().hasChapter();
    }

    public final int getFinalChapterNr() {
        return getCurrentState().getFinalChapterNr();
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

    public ActivationTable getAbilityActivatedThisTurn() {
        return numberTurnActivations;
    }
    public ActivationTable getAbilityActivatedThisGame() {
        return numberGameActivations;
    }
    public ActivationTable getAbilityResolvedThisTurn() {
        return numberAbilityResolved;
    }

    public int getAbilityActivatedThisTurn(SpellAbility ability) {
        return numberTurnActivations.get(ability);
    }
    public int getAbilityActivatedThisGame(SpellAbility ability) {
        return numberGameActivations.get(ability);
    }
    public int getAbilityResolvedThisTurn(SpellAbility ability) {
        return numberAbilityResolved.get(ability);
    }

    public void addAbilityResolved(SpellAbility ability) {
        numberAbilityResolved.add(ability);
    }
    public List<Player> getAbilityResolvedThisTurnActivators(SpellAbility ability) {
        return numberAbilityResolved.getActivators(ability);
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
            List<String> result = chosenModesTurn.computeIfAbsent(original, k -> Lists.newArrayList());
            result.add(mode);

            result = chosenModesGame.computeIfAbsent(original, k -> Lists.newArrayList());
            result.add(mode);

            if (yourCombat) {
                result = chosenModesYourCombat.computeIfAbsent(original, k -> Lists.newArrayList());
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

        return updateState;
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
        return StaticAbilityCantAttackBlock.attackVigilance(this);
    }

    public boolean isAbilitySick() {
        if (!isSick()) {
            return false;
        }
        return !StaticAbilityActivateAbilityAsIfHaste.canActivate(this);
    }

    public boolean isWitherDamage() {
        if (hasKeyword(Keyword.WITHER) || hasKeyword(Keyword.INFECT)) {
            return true;
        }
        return StaticAbilityWitherDamage.isWitherDamage(this);
    }

    public boolean isInfectDamage(Player target) {
        return hasKeyword(Keyword.INFECT) || StaticAbilityInfectDamage.isInfectDamage(target);
    }

    public Set<CardStateName> getUnlockedRooms() {
        return this.unlockedRooms;
    }
    public void setUnlockedRooms(Set<CardStateName> set) {
        this.unlockedRooms = set;
    }

    public List<String> getUnlockedRoomNames() {
        List<String> result = Lists.newArrayList();
        for (CardStateName stateName : unlockedRooms) {
            if (this.hasState(stateName)) {
                result.add(this.getState(stateName).getName());
            }
        }
        return result;
    }

    public Set<CardStateName> getLockedRooms() {
        Set<CardStateName> result = Sets.newHashSet(CardStateName.LeftSplit, CardStateName.RightSplit);
        result.removeAll(this.unlockedRooms);
        return result;
    }

    public List<String> getLockedRoomNames() {
        List<String> result = Lists.newArrayList();
        for (CardStateName stateName : getLockedRooms()) {
            if (this.hasState(stateName)) {
                result.add(this.getState(stateName).getName());
            }
        }
        return result;
    }

    public boolean unlockRoom(Player p, CardStateName stateName) {
        if (unlockedRooms.contains(stateName) || (stateName != CardStateName.LeftSplit && stateName != CardStateName.RightSplit)) {
            return false;
        }
        unlockedRooms.add(stateName);

        updateRooms();

        getGame().fireEvent(new GameEventDoorChanged(p, this, stateName, true));

        Map<AbilityKey, Object> unlockParams =  AbilityKey.mapFromPlayer(p);
        unlockParams.put(AbilityKey.Card, this);
        unlockParams.put(AbilityKey.CardState, getState(stateName));
        getGame().getTriggerHandler().runTrigger(TriggerType.UnlockDoor, unlockParams, true);

        // fully unlock
        if (unlockedRooms.size() > 1) {
            Map<AbilityKey, Object> fullyUnlockParams = AbilityKey.mapFromPlayer(p);
            fullyUnlockParams.put(AbilityKey.Card, this);

            getGame().getTriggerHandler().runTrigger(TriggerType.FullyUnlock, fullyUnlockParams, true);
        }

        return true;
    }

    public boolean lockRoom(Player p, CardStateName stateName) {
        if (!unlockedRooms.contains(stateName) || (stateName != CardStateName.LeftSplit && stateName != CardStateName.RightSplit)) {
            return false;
        }
        unlockedRooms.remove(stateName);

        updateRooms();

        getGame().fireEvent(new GameEventDoorChanged(p, this, stateName, false));

        return true;
    }

    public void updateRooms() {
        if (!isRoom()) {
            return;
        }
        if (isFaceDown()) {
            return;
        }
        if (unlockedRooms.isEmpty()) {
            this.setState(CardStateName.EmptyRoom, true);
        } else if (unlockedRooms.size() > 1) {
            this.setState(CardStateName.Original, true);
        } else { // we already know the set is only one
            for (CardStateName name : unlockedRooms) {
                this.setState(name, true);
            }
        }
        // update trigger after state change
        getGame().getTriggerHandler().clearActiveTriggers(this, null);
        getGame().getTriggerHandler().registerActiveTrigger(this, false);
    }

    public CardState getEmptyRoomState() {
        if (!states.containsKey(CardStateName.EmptyRoom)) {
            states.put(CardStateName.EmptyRoom, CardUtil.getEmptyRoomCharacteristic(this));
        }
        return states.get(CardStateName.EmptyRoom);
    }

    public SpellAbility getUnlockAbility(CardStateName state) {
        if (!unlockAbilities.containsKey(state)) {
            unlockAbilities.put(state, CardFactoryUtil.abilityUnlockRoom(getState(state)));
        }
        return unlockAbilities.get(state);
    }

    public void copyFrom(Card in) {
        // clean is not needed?
        this.changedCardColors.putAll(in.changedCardColors);
        this.changedCardColorsCharacterDefining.putAll(in.changedCardColorsCharacterDefining);

        setChangedCardKeywords(in.getChangedCardKeywords());

        this.changedCardTypes.putAll(in.changedCardTypes);
        this.changedCardTypesCharacterDefining.putAll(in.changedCardTypesCharacterDefining);

        this.changedCardNames.putAll(in.changedCardNames);
        setChangedCardTraits(in.getChangedCardTraits());

        setChangedCardTraitsByText(in.getChangedCardTraitsByText());
        setChangedCardKeywordsByText(in.getChangedCardKeywordsByText());
    }
}
