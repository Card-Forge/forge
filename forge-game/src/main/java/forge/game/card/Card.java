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
import com.google.common.base.Function;
import com.google.common.collect.*;
import forge.GameCommand;
import forge.ImageKeys;
import forge.StaticData;
import forge.card.*;
import forge.card.CardDb.SetPreference;
import forge.card.CardType.CoreType;
import forge.card.mana.ManaCost;
import forge.card.mana.ManaCostParser;
import forge.game.*;
import forge.game.ability.AbilityFactory;
import forge.game.ability.AbilityUtils;
import forge.game.ability.ApiType;
import forge.game.card.CardPredicates.Presets;
import forge.game.combat.AttackingBand;
import forge.game.combat.Combat;
import forge.game.cost.Cost;
import forge.game.event.*;
import forge.game.event.GameEventCardAttachment.AttachMethod;
import forge.game.event.GameEventCardDamaged.DamageType;
import forge.game.keyword.Keyword;
import forge.game.keyword.KeywordsChange;
import forge.game.player.Player;
import forge.game.replacement.ReplaceMoved;
import forge.game.replacement.ReplacementEffect;
import forge.game.replacement.ReplacementResult;
import forge.game.spellability.OptionalCost;
import forge.game.spellability.SpellAbility;
import forge.game.spellability.SpellPermanent;
import forge.game.spellability.TargetRestrictions;
import forge.game.staticability.StaticAbility;
import forge.game.trigger.Trigger;
import forge.game.trigger.TriggerType;
import forge.game.zone.Zone;
import forge.game.zone.ZoneType;
import forge.item.IPaperCard;
import forge.item.PaperCard;
import forge.trackable.TrackableProperty;
import forge.util.*;
import forge.util.collect.FCollection;
import forge.util.collect.FCollectionView;
import forge.util.maps.HashMapOfLists;
import forge.util.maps.MapOfLists;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.apache.commons.lang3.tuple.Pair;

import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.CopyOnWriteArrayList;

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
public class Card extends GameEntity implements Comparable<Card> {
    private final Game game;
    private final IPaperCard paperCard;

    private final Map<CardStateName, CardState> states = new EnumMap<>(CardStateName.class);
    private CardState currentState;
    private CardStateName currentStateName = CardStateName.Original;
    private CardStateName preFaceDownState = CardStateName.Original;

    private ZoneType castFrom = null;

    private final CardDamageHistory damageHistory = new CardDamageHistory();
    private Map<Card, Map<CounterType, Integer>> countersAddedBy = new TreeMap<>();
    private List<String> extrinsicKeyword = new ArrayList<>();
    // Hidden keywords won't be displayed on the card
    private final CopyOnWriteArrayList<String> hiddenExtrinsicKeyword = new CopyOnWriteArrayList<>();

    // cards attached or otherwise linked to this card
    private CardCollection equippedBy, fortifiedBy, hauntedBy, devouredCards, delvedCards, imprintedCards, encodedCards;
    private CardCollection mustBlockCards, clones, gainControlTargets, chosenCards, blockedThisTurn, blockedByThisTurn;

    // if this card is attached or linked to something, what card is it currently attached to
    private Card equipping, fortifying, cloneOrigin, haunting, effectSource, pairedWith;

    // if this card is an Aura, what Entity is it enchanting?
    private GameEntity enchanting = null;
    private GameEntity mustAttackEntity = null;

    private final Map<Player, CardPlayOption> mayPlay = Maps.newTreeMap();

    // changes by AF animate and continuous static effects - timestamp is the key of maps
    private final Map<Long, CardChangedType> changedCardTypes = new TreeMap<>();
    private final Map<Long, KeywordsChange> changedCardKeywords = new TreeMap<>();
    private final SortedMap<Long, CardColor> changedCardColors = new TreeMap<>();

    // changes that say "replace each instance of one [color,type] by another - timestamp is the key of maps
    private final CardChangedWords changedTextColors = new CardChangedWords();
    private final CardChangedWords changedTextTypes = new CardChangedWords();
    /** List of the keywords that have been added by text changes. */
    private final List<String> keywordsGrantedByTextChanges = Lists.newArrayList();
    /** Original values of SVars changed by text changes. */
    private Map<String, String> originalSVars = Maps.newHashMap();

    private final Set<Object> rememberedObjects = new LinkedHashSet<>();
    private final MapOfLists<GameEntity, Object> rememberMap = new HashMapOfLists<>(CollectionSuppliers.arrayLists());
    private Map<Player, String> flipResult;

    private Map<Card, Integer> receivedDamageFromThisTurn = new TreeMap<>();
    private Map<Card, Integer> dealtDamageToThisTurn = new TreeMap<>();
    private Map<String, Integer> dealtDamageToPlayerThisTurn = new TreeMap<>();
    private final Map<Card, Integer> assignedDamageMap = new TreeMap<>();

    private boolean isCommander = false;
    private boolean startsGameInPlay = false;
    private boolean drawnThisTurn = false;
    private boolean becameTargetThisTurn = false;
    private boolean startedTheTurnUntapped = false;
    private boolean cameUnderControlSinceLastUpkeep = true; // for Echo
    private boolean tapped = false;
    private boolean sickness = true; // summoning sickness
    private boolean token = false;
    private Card copiedPermanent = null;
    private boolean copiedSpell = false;

    private boolean canCounter = true;
    private boolean evoked = false;

    private boolean unearthed;

    private boolean monstrous = false;
    private int monstrosityNum = 0;

    private boolean renowned = false;

    private boolean manifested = false;

    private long bestowTimestamp = -1;
    private long transformedTimestamp = 0;
    private boolean suspendCast = false;
    private boolean suspend = false;
    private boolean tributed = false;
    private boolean madness = false;

    private boolean phasedOut = false;
    private boolean directlyPhasedOut = true;

    private boolean usedToPayCost = false;

    // for Vanguard / Manapool / Emblems etc.
    private boolean isImmutable = false;

    private long timestamp = -1; // permanents on the battlefield

    // stack of set power/toughness
    private List<CardPowerToughness> newPT = new ArrayList<>();
    private int baseLoyalty = 0;
    private String basePowerString = null;
    private String baseToughnessString = null;
    private String oracleText = "";

    private int damage;
    private boolean hasBeenDealtDeathtouchDamage = false;

    // regeneration
    private List<CardShields> shields = new ArrayList<>();
    private int regeneratedThisTurn = 0;

    private int turnInZone;

    private int tempPowerBoost = 0;
    private int tempToughnessBoost = 0;

    private int semiPermanentPowerBoost = 0;
    private int semiPermanentToughnessBoost = 0;

    private int xManaCostPaid = 0;
    private Map<String, Integer> xManaCostPaidByColor;

    private int sunburstValue = 0;
    private byte colorsPaid = 0;

    private Player owner = null;
    private Player controller = null;
    private long controllerTimestamp = 0;
    private NavigableMap<Long, Player> tempControllers = new TreeMap<>();

    private String originalText = "", text = "";
    private Cost miracleCost = null;
    private String chosenType = "";
    private List<String> chosenColors;
    private String namedCard = "";
    private int chosenNumber;
    private Player chosenPlayer;
    private Direction chosenDirection = null;

    private Card exiledWith = null;

    private final List<GameCommand> leavePlayCommandList = new ArrayList<>();
    private final List<GameCommand> etbCommandList = new ArrayList<>();
    private final List<GameCommand> untapCommandList = new ArrayList<>();
    private final List<GameCommand> changeControllerCommandList = new ArrayList<>();
    private final List<Object[]> staticCommandList = new ArrayList<>();

    private final static ImmutableList<String> storableSVars = ImmutableList.of("ChosenX");

    // Zone-changing spells should store card's zone here
    private Zone currentZone = null;

    private int countersAdded = 0;

    private CardRules cardRules;
    private final CardView view;

    // Enumeration for CMC request types
    public enum SplitCMCMode {
        CurrentSideCMC,
        CombinedCMC,
        LeftSplitCMC,
        RightSplitCMC
    }

    public static int SPLIT_CMC_ENCODE_MAGIC_NUMBER = 10000000;

    /**
     * Instantiates a new card not associated to any paper card.
     * @param id0 the unique id of the new card.
     */
    public Card(final int id0, final Game game0) {
        this(id0, null, true, game0);
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
        this(id0, paperCard0, true, game0);
    }
    public Card(final int id0, final IPaperCard paperCard0, final boolean allowCache, final Game game0) {
        super(id0);

        game = game0;
        if (id0 >= 0 && allowCache && game != null) {
            game.addCard(id0, this);
        }
        paperCard = paperCard0;
        view = new CardView(id0, game == null ? null : game.getTracker());
        currentState = new CardState(view.getCurrentState(), this);
        states.put(CardStateName.Original, currentState);
        states.put(CardStateName.FaceDown, CardUtil.getFaceDownCharacteristic(this));
        view.updateChangedColorWords(this);
        view.updateChangedTypes(this);
        view.updateSickness(this);
    }

    public boolean changeToState(final CardStateName state) {
        CardStateName cur = currentStateName;

        if (!setState(state, true)) {
            return false;
        }

        if ((cur == CardStateName.Original && state == CardStateName.Transformed)
                || (cur == CardStateName.Transformed && state == CardStateName.Original)) {

            // Clear old dfc trigger from the trigger handler
            getGame().getTriggerHandler().clearInstrinsicActiveTriggers(this, null);
            getGame().getTriggerHandler().registerActiveTrigger(this, false);
            HashMap<String, Object> runParams = new HashMap<>();
            runParams.put("Transformer", this);
            getGame().getTriggerHandler().runTrigger(TriggerType.Transformed, runParams, false);
            this.incrementTransformedTimestamp();
        }

        return true;
    }

    public long getTransformedTimestamp() {  return transformedTimestamp; }
    public void incrementTransformedTimestamp() {  this.transformedTimestamp++;  }

    public CardState getCurrentState() {
        return currentState;
    }
    public CardState getAlternateState() {
        if (hasAlternateState()) {
            if (isSplitCard()) {
                if (currentStateName == CardStateName.RightSplit) {
                    return states.get(CardStateName.LeftSplit);
                }
                else {
                    return states.get(CardStateName.RightSplit);
                }
            }
            else if (isFlipCard() && currentStateName != CardStateName.Flipped) {
                return states.get(CardStateName.Flipped);
            }
            else if (isDoubleFaced() && currentStateName != CardStateName.Transformed) {
                return states.get(CardStateName.Transformed);
            }
            else {
                return states.get(CardStateName.Original);
            }
        }
        else if (isFaceDown()) {
            return states.get(CardStateName.Original);
        }
        return null;
    }
    public CardState getState(final CardStateName state) {
        return states.get(state);
    }
    public boolean setState(final CardStateName state, boolean updateView) {

        if (!states.containsKey(state)) {
            System.out.println(getName() + " tried to switch to non-existant state \"" + state + "\"!");
            return false; // Nonexistant state.
        }

        if (state.equals(currentStateName)) {
            return false;
        }

        // Cleared tests, about to change states
        if (currentStateName.equals(CardStateName.FaceDown) && state.equals(CardStateName.Original)) {
            this.setManifested(false);
        }

        currentStateName = state;
        currentState = states.get(state);

        if (updateView) {
            view.updateState(this);

            final Game game = getGame();
            if (game != null) {
                // update Type, color and keywords again if they have changed
                if (!changedCardTypes.isEmpty()) {
                    currentState.getView().updateType(currentState);
                }
                if (!changedCardColors.isEmpty()) {
                    currentState.getView().updateColors(this);
                }
                if (!changedCardKeywords.isEmpty()) {
                    currentState.getView().updateKeywords(this, currentState);
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

    public void switchStates(final CardStateName from, final CardStateName to, boolean updateView) {
        final CardState tmp = states.get(from);
        states.put(from, states.get(to));
        states.put(to, tmp);
        if (currentStateName == from) {
            setState(to, false);
        }
        if (updateView) {
            view.updateState(this);
        }
    }

    public final void addAlternateState(final CardStateName state, final boolean updateView) {
        states.put(state, new CardState(view.createAlternateState(state), this));
        if (updateView) {
            view.updateState(this);
        }
    }

    public void clearStates(final CardStateName state, boolean updateView) {
        if (states.remove(state) != null && updateView) {
            view.updateState(this);
        }
    }

    public void updateStateForView() {
        view.updateState(this);
    }

    public void setPreFaceDownState(CardStateName preCharacteristic) {
        preFaceDownState = preCharacteristic;
    }

    public boolean changeCardState(final String mode, final String customState) {
        if (mode == null)
            return changeToState(CardStateName.smartValueOf(customState));

        // flip and face-down don't overlap. That is there is no chance to turn face down a flipped permanent
        // and then any effect have it turn upface again and demand its former flip state to be restored
        // Proof: Morph cards never have ability that makes them flip, Ixidron does not suppose cards to be turned face up again,
        // Illusionary Mask affects cards in hand.
        CardStateName oldState = getCurrentStateName();
        if (mode.equals("Transform") && isDoubleFaced()) {
            if (hasKeyword("CARDNAME can't transform")) {
                return false;
            }
            CardStateName destState = oldState == CardStateName.Transformed ? CardStateName.Original : CardStateName.Transformed;

            if (this.isInPlay() && !this.getState(destState).getType().isPermanent()) {
                return false;
            }

            return changeToState(destState);

        } else if (mode.equals("Flip") && isFlipCard()) {
            CardStateName destState = oldState == CardStateName.Flipped ? CardStateName.Original : CardStateName.Flipped;
            return changeToState(destState);
        } else if (mode.equals("TurnFace")) {
            if (oldState == CardStateName.Original) {
                // Reset cloned state if Vesuvan Shapeshifter
                if (isCloned() && getState(CardStateName.Cloner).getName().equals("Vesuvan Shapeshifter")) {
                    switchStates(CardStateName.Cloner, CardStateName.Original, false);
                    setState(CardStateName.Original, false);
                    clearStates(CardStateName.Cloner, false);
                }
                return turnFaceDown();
            } else if (oldState == CardStateName.FaceDown) {
                return turnFaceUp();
            }
        }
        return false;
    }

    public Card manifest(Player p) {
        // Turn Face Down (even if it's DFC).
        CardState originalCard = this.getState(CardStateName.Original);
        ManaCost cost = originalCard.getManaCost();

        boolean isCreature = this.isCreature();

         // Sometimes cards are manifested while already being face down
         if (!turnFaceDown(true) && currentStateName != CardStateName.FaceDown) {
             return null;
         }
        // Move to p's battlefield
        Game game = p.getGame();
		// Just in case you aren't the controller, now you are!
        this.setController(p, game.getNextTimestamp());
        Card c = game.getAction().moveToPlay(this, p);
        c.setPreFaceDownState(CardStateName.Original);
        // Mark this card as "manifested"
        c.setManifested(true);

        // Add manifest demorph static ability for creatures
        if (isCreature && !cost.isNoCost()) {
            c.addSpellAbility(CardFactoryUtil.abilityManifestFaceUp(c, cost));
        }

        return c;
    }

    public boolean turnFaceDown() {
        return turnFaceDown(false);
    }

    public boolean turnFaceDown(boolean override) {
        if (override || !isDoubleFaced()) {
            preFaceDownState = currentStateName;
            return setState(CardStateName.FaceDown, true);
        }
        return false;
    }

	public boolean turnFaceUp() {
		return turnFaceUp(false, true);
	}

    public boolean turnFaceUp(boolean manifestPaid, boolean runTriggers) {
        if (currentStateName == CardStateName.FaceDown) {
            if (manifestPaid && this.isManifested() && !this.getRules().getType().isCreature()) {
                // If we've manifested a non-creature and we're demanifesting disallow it

                // Unless this creature also has a Morph ability

                return false;
            }

            boolean result = setState(preFaceDownState, true);
            if (result && runTriggers) {
                // Run replacement effects
                HashMap<String, Object> repParams = new HashMap<>();
                repParams.put("Event", "TurnFaceUp");
                repParams.put("Affected", this);
                getGame().getReplacementHandler().run(repParams);

                // Run triggers
                getGame().getTriggerHandler().registerActiveTrigger(this, false);
                final Map<String, Object> runParams = new TreeMap<>();
                runParams.put("Card", this);
                getGame().getTriggerHandler().runTrigger(TriggerType.TurnFaceUp, runParams, false);
            }
            return result;
        }
        return false;
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
        return currentState.getName();
    }

    @Override
    public final void setName(final String name0) {
        currentState.setName(name0);
    }

    public final boolean isInAlternateState() {
        return currentStateName != CardStateName.Original
            && currentStateName != CardStateName.Cloned;
    }

    public final boolean hasAlternateState() {
        return states.keySet().size() > 2;
    }

    public final boolean isDoubleFaced() {
        return states.containsKey(CardStateName.Transformed);
    }

    public final boolean isFlipCard() {
        return states.containsKey(CardStateName.Flipped);
    }

    public final boolean isSplitCard() {
        return states.containsKey(CardStateName.LeftSplit);
    }

    public boolean isCloned() {
        return states.containsKey(CardStateName.Cloner);
    }

    public static List<String> getStorableSVars() {
        return Card.storableSVars;
    }

    public final CardCollectionView getDevoured() {
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

    public MapOfLists<GameEntity, Object> getRememberMap() {
        return rememberMap;
    }
    public final void addRememberMap(final GameEntity e, final List<Object> o) {
        rememberMap.addAll(e, o);
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
    public final void clearRemembered() {
        if (rememberedObjects.isEmpty()) { return; }
        rememberedObjects.clear();
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

    public final String getFlipResult(final Player flipper) {
        if (flipResult == null) {
            return null;
        }
        return flipResult.get(flipper);
    }
    public final void addFlipResult(final Player flipper, final String result) {
        if (flipResult == null) {
            flipResult = new TreeMap<>();
        }
        flipResult.put(flipper, result);
    }
    public final void clearFlipResult() {
        flipResult = null;
    }

    public final FCollectionView<Trigger> getTriggers() {
        return currentState.getTriggers();
    }
    public final void setTriggers(final Iterable<Trigger> trigs, boolean intrinsicOnly) {
        final FCollection<Trigger> copyList = new FCollection<>();
        for (final Trigger t : trigs) {
            if (!intrinsicOnly || t.isIntrinsic()) {
                copyList.add(t.getCopyForHostCard(this));
            }
        }
        currentState.setTriggers(copyList);
    }
    public final Trigger addTrigger(final Trigger t) {
        final Trigger newtrig = t.getCopyForHostCard(this);
        currentState.addTrigger(newtrig);
        return newtrig;
    }
    public final void moveTrigger(final Trigger t) {
        t.setHostCard(this);
        currentState.addTrigger(t);
    }
    public final void removeTrigger(final Trigger t) {
        currentState.removeTrigger(t);
    }
    public final void removeTrigger(final Trigger t, final CardStateName state) {
        getState(state).removeTrigger(t);
    }
    public final void clearTriggersNew() {
        currentState.clearTriggers();
    }

    public final Object getTriggeringObject(final String typeIn) {
        Object triggered = null;
        if (!currentState.getTriggers().isEmpty()) {
            for (final Trigger t : currentState.getTriggers()) {
                final SpellAbility sa = t.getTriggeredSA();
                triggered = sa.getTriggeringObject(typeIn);
                if (triggered != null) {
                    break;
                }
            }
        }
        return triggered;
    }

    public final int getSunburstValue() {
        return sunburstValue;
    }
    public final void setSunburstValue(final int valueIn) {
        sunburstValue = valueIn;
    }

    public final byte getColorsPaid() {
        return colorsPaid;
    }
    public final void setColorsPaid(final byte s) {
        colorsPaid |= s;
    }

    public final int getXManaCostPaid() {
        return xManaCostPaid;
    }
    public final void setXManaCostPaid(final int n) {
        xManaCostPaid = n;
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

    public CardCollectionView getBlockedThisTurn() {
        return CardCollection.getView(blockedThisTurn);
    }
    public void addBlockedThisTurn(Card attacker) {
        if (blockedThisTurn == null) {
            blockedThisTurn = new CardCollection();
        }
        blockedThisTurn.add(attacker);
    }
    public void clearBlockedThisTurn() {
        blockedThisTurn = null;
    }

    public CardCollectionView getBlockedByThisTurn() {
        return CardCollection.getView(blockedByThisTurn);
    }
    public void addBlockedByThisTurn(Card blocker) {
        if (blockedByThisTurn == null) {
            blockedByThisTurn = new CardCollection();
        }
        blockedByThisTurn.add(blocker);
    }
    public void clearBlockedByThisTurn() {
        blockedByThisTurn = null;
    }

    //MustBlockCards are cards that this Card must block if able in an upcoming combat.
    //This is cleared at the end of each turn.
    public final CardCollectionView getMustBlockCards() {
        return CardCollection.getView(mustBlockCards);
    }
    public final void addMustBlockCard(final Card c) {
        mustBlockCards = view.addCard(mustBlockCards, c, TrackableProperty.MustBlockCards);
    }
    public final void addMustBlockCards(final Iterable<Card> attackersToBlock) {
        mustBlockCards = view.addCards(mustBlockCards, attackersToBlock, TrackableProperty.MustBlockCards);
    }
    public final void clearMustBlockCards() {
        mustBlockCards = view.clearCards(mustBlockCards, TrackableProperty.MustBlockCards);
    }

    public final void setMustAttackEntity(final GameEntity e) {
        mustAttackEntity = e;
    }
    public final GameEntity getMustAttackEntity() {
        return mustAttackEntity;
    }
    public final void clearMustAttackEntity(final Player playerturn) {
    	if (this.getController().equals(playerturn)) {
    		mustAttackEntity = null;
    	}
    }

    public final CardCollectionView getClones() {
        return CardCollection.getView(clones);
    }
    public final void setClones(final Iterable<Card> clones0) {
        clones = clones0 == null ? null : new CardCollection(clones0);
    }
    public final void addClone(final Card c) {
        if (clones == null) {
            clones = new CardCollection();
        }
        clones.add(c);
    }
    public final void clearClones() {
        clones = null;
    }

    public final Card getCloneOrigin() {
        return cloneOrigin;
    }
    public final void setCloneOrigin(final Card cloneOrigin0) {
        cloneOrigin = view.setCard(cloneOrigin, cloneOrigin0, TrackableProperty.CloneOrigin);
    }

    public final boolean hasFirstStrike() {
        return hasKeyword("First Strike");
    }

    public final boolean hasDoubleStrike() {
        return hasKeyword("Double Strike");
    }

    public final boolean hasSecondStrike() {
        return hasDoubleStrike() || !hasFirstStrike();
    }
    
    public final boolean hasConverge() {
    	return "Count$Converge".equals(getSVar("X")) || "Count$Converge".equals(getSVar("Y")) || hasKeyword("Sunburst");
    }

    public final boolean canReceiveCounters(final CounterType type) {
        if (hasKeyword("CARDNAME can't have counters placed on it.")) {
            return false;
        }
        if (isCreature() && type == CounterType.M1M1) {
            for (final Card c : getController().getCreaturesInPlay()) { // look for Melira, Sylvok Outcast
                if (c.hasKeyword("Creatures you control can't have -1/-1 counters placed on them.")) {
                    return false;
                }
            }
        } else if (type == CounterType.DREAM) {
            if (hasKeyword("CARDNAME can't have more than seven dream counters on it.") && getCounters(CounterType.DREAM) > 6) {
                return false;
            }
        }
        return true;
    }

    public final int getTotalCountersToAdd() {
        return countersAdded;
    }

    public final void setTotalCountersToAdd(int value) {
        countersAdded = value;
    }

    public final void addCounter(final CounterType counterType, final int n, final boolean applyMultiplier) {
        addCounter(counterType, n, applyMultiplier, true);
    }
    public final void addCounterFireNoEvents(final CounterType counterType, final int n, final boolean applyMultiplier) {
        addCounter(counterType, n, applyMultiplier, false);
    }

    protected void addCounter(final CounterType counterType, final int n, final boolean applyMultiplier, final boolean fireEvents) {
        int addAmount = n;
        if(addAmount < 0) {
            addAmount = 0; // As per rule 107.1b
        }
        final HashMap<String, Object> repParams = new HashMap<>();
        repParams.put("Event", "AddCounter");
        repParams.put("Affected", this);
        repParams.put("CounterType", counterType);
        repParams.put("CounterNum", addAmount);
        repParams.put("EffectOnly", applyMultiplier);
        if (getGame().getReplacementHandler().run(repParams) != ReplacementResult.NotReplaced) {
            return;
        }
        if (canReceiveCounters(counterType)) {
            if (counterType == CounterType.DREAM && hasKeyword("CARDNAME can't have more than seven dream counters on it.")) {
                addAmount = Math.min(7 - getCounters(CounterType.DREAM), addAmount);
            }
        }
        else {
            addAmount = 0;
        }

        if (addAmount == 0) {
            return;
        }
        setTotalCountersToAdd(addAmount);

        if (fireEvents) {
            final Integer oldValue = counters.get(counterType);
            final Integer newValue = addAmount + (oldValue == null ? 0 : oldValue);

            if (!newValue.equals(oldValue)) {
                final int powerBonusBefore = getPowerBonusFromCounters();
                final int toughnessBonusBefore = getToughnessBonusFromCounters();
                final int loyaltyBefore = getCurrentLoyalty();

                counters.put(counterType, newValue);
                view.updateCounters(this);

                //fire card stats changed event if p/t bonuses or loyalty changed from added counters
                if (powerBonusBefore != getPowerBonusFromCounters() || toughnessBonusBefore != getToughnessBonusFromCounters() || loyaltyBefore != getCurrentLoyalty()) {
                    getGame().fireEvent(new GameEventCardStatsChanged(this));
                }

                // play the Add Counter sound
                getGame().fireEvent(new GameEventCardCounters(this, counterType, oldValue == null ? 0 : oldValue, newValue));
            }

            // Run triggers
            final Map<String, Object> runParams = new TreeMap<>();
            runParams.put("Card", this);
            runParams.put("CounterType", counterType);
            for (int i = 0; i < addAmount; i++) {
                getGame().getTriggerHandler().runTrigger(TriggerType.CounterAdded, runParams, false);
            }
            if (addAmount > 0) {
                getGame().getTriggerHandler().runTrigger(TriggerType.CounterAddedOnce, runParams, false);
            }
        }
    }

    /**
     * <p>
     * addCountersAddedBy.
     * </p>
     * @param source - the card adding the counters to this card
     * @param counterType - the counter type added
     * @param counterAmount - the amount of counters added
     */
    public final void addCountersAddedBy(final Card source, final CounterType counterType, final int counterAmount) {
        final Map<CounterType, Integer> counterMap = new TreeMap<>();
        counterMap.put(counterType, counterAmount);
        countersAddedBy.put(source, counterMap);
    }

    /**
     * <p>
     * getCountersAddedBy.
     * </p>
     * @param source - the card the counters were added by
     * @param counterType - the counter type added
     * @return the amount of counters added.
     */
    public final int getCountersAddedBy(final Card source, final CounterType counterType) {
        int counterAmount = 0;
        if (countersAddedBy.containsKey(source)) {
            final Map<CounterType, Integer> counterMap = countersAddedBy.get(source);
            counterAmount = counterMap.containsKey(counterType) ? counterMap.get(counterType) : 0;
            countersAddedBy.remove(source);
        }
        return counterAmount;
    }

    public final void subtractCounter(final CounterType counterName, final int n) {
        Integer oldValue = counters.get(counterName);
        int newValue = oldValue == null ? 0 : Math.max(oldValue - n, 0);

        final int delta = (oldValue == null ? 0 : oldValue) - newValue;
        if (delta == 0) { return; }

        int powerBonusBefore = getPowerBonusFromCounters();
        int toughnessBonusBefore = getToughnessBonusFromCounters();
        int loyaltyBefore = getCurrentLoyalty();

        if (newValue > 0) {
            counters.put(counterName, newValue);
        }
        else {
            counters.remove(counterName);
        }
        view.updateCounters(this);

        //fire card stats changed event if p/t bonuses or loyalty changed from subtracted counters
        if (powerBonusBefore != getPowerBonusFromCounters() || toughnessBonusBefore != getToughnessBonusFromCounters() || loyaltyBefore != getCurrentLoyalty()) {
            getGame().fireEvent(new GameEventCardStatsChanged(this));
        }

        // Play the Subtract Counter sound
        getGame().fireEvent(new GameEventCardCounters(this, counterName, oldValue == null ? 0 : oldValue, newValue));

        // Run triggers
        int curCounters = oldValue == null ? 0 : oldValue;
        for (int i = 0; i < delta && curCounters != 0; i++) {
            final Map<String, Object> runParams = new TreeMap<>();
            runParams.put("Card", this);
            runParams.put("CounterType", counterName);
            runParams.put("NewCounterAmount", --curCounters);
            getGame().getTriggerHandler().runTrigger(TriggerType.CounterRemoved, runParams, false);
        }
    }

    public final void setCounters(final Map<CounterType, Integer> allCounters) {
        counters = allCounters;
        view.updateCounters(this);
    }

    public final void clearCounters() {
        if (counters.isEmpty()) { return; }
        counters.clear();
        view.updateCounters(this);
    }

    public final String getSVar(final String var) {
        return currentState.getSVar(var);
    }

    public final boolean hasSVar(final String var) {
        return currentState.hasSVar(var);
    }

    public final void setSVar(final String var, final String str) {
        currentState.setSVar(var, str);
    }

    public final Map<String, String> getSVars() {
        return currentState.getSVars();
    }

    public final void setSVars(final Map<String, String> newSVars) {
        currentState.setSVars(newSVars);
    }

    public final int sumAllCounters() {
        int count = 0;
        for (final Integer value2 : counters.values()) {
            count += value2;
        }
        return count;
    }

    public final int getTurnInZone() {
        return turnInZone;
    }

    public final void setTurnInZone(final int turn) {
        turnInZone = turn;
    }

    public final void setManaCost(final ManaCost s) {
        currentState.setManaCost(s);
    }

    public final ManaCost getManaCost() {
        return currentState.getManaCost();
    }

    public final Player getChosenPlayer() {
        return chosenPlayer;
    }
    public final void setChosenPlayer(final Player p) {
        if (chosenPlayer == p) { return; }
        chosenPlayer = p;
        view.updateChosenPlayer(this);
    }

    public final int getChosenNumber() {
        return chosenNumber;
    }
    public final void setChosenNumber(final int i) {
        chosenNumber = i;
    }

    public final Card getExiledWith() {
        return exiledWith;
    }
    public final void setExiledWith(final Card e) {
        exiledWith = e;
    }

    // used for cards like Belbe's Portal, Conspiracy, Cover of Darkness, etc.
    public final String getChosenType() {
        return chosenType;
    }
    public final void setChosenType(final String s) {
        chosenType = s;
        view.updateChosenType(this);
    }

    public final String getChosenColor() {
        if (hasChosenColor()) {
            return chosenColors.get(0);
        }
        return "";
    }
    public final Iterable<String> getChosenColors() {
        if (chosenColors == null) {
            return new ArrayList<>();
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
    public final void setChosenCards(final CardCollection cards) {
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

    // used for cards like Meddling Mage...
    public final String getNamedCard() {
        return namedCard;
    }
    public final void setNamedCard(final String s) {
        namedCard = s;
        view.updateNamedCard(this);
    }

    public final boolean getDrawnThisTurn() {
        return drawnThisTurn;
    }
    public final void setDrawnThisTurn(final boolean b) {
        drawnThisTurn = b;
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
        return keywordsToText(getHiddenExtrinsicKeywords());
    }

    // convert a keyword list to the String that should be displayed in game
    public final String keywordsToText(final List<String> keywords) {
        final StringBuilder sb = new StringBuilder();
        final StringBuilder sbLong = new StringBuilder();

        // Prepare text changes
        final Set<Entry<String, String>> textChanges = Sets.union(
                changedTextColors.toMap().entrySet(), changedTextTypes.toMap().entrySet());

        for (int i = 0; i < keywords.size(); i++) {
            String keyword = keywords.get(i);
            if (keyword.startsWith("PreventAllDamageBy")
                    || keyword.startsWith("CantEquip")
                    || keyword.startsWith("SpellCantTarget")) {
                continue;
            }
            // format text changes
            if (CardUtil.isKeywordModifiable(keyword)
                    && keywordsGrantedByTextChanges.contains(keyword)) {
                for (final Entry<String, String> e : textChanges) {
                    final String value = e.getValue();
                    if (keyword.contains(value)) {
                        keyword = keyword.replace(value,
                                "<strike>" + e.getKey() + "</strike> " + value);
                        // assume (for now) max one change per keyword
                        break;
                    }
                }
            }
            if (keyword.startsWith("etbCounter")) {
                final String[] p = keyword.split(":");
                final StringBuilder s = new StringBuilder();
                if (p.length > 4) {
                    s.append(p[4]);
                } else {
                    final CounterType counter = CounterType.valueOf(p[1]);
                    final String numCounters = p[2];
                    s.append(getName());
                    s.append(" enters the battlefield with ");
                    s.append(numCounters);
                    s.append(" ");
                    s.append(counter.getName());
                    s.append(" counter");
                    if (!numCounters.equals("1")) {
                        s.append("s");
                    }
                    s.append(" on it.");
                }
                sbLong.append(s).append("\r\n");
            } else if (keyword.startsWith("Protection:")) {
                final String[] k = keyword.split(":");
                sbLong.append(k[2]).append("\r\n");
            } else if (keyword.startsWith("Creatures can't attack unless their controller pays")) {
                final String[] k = keyword.split(":");
                if (!k[3].equals("no text")) {
                    sbLong.append(k[3]).append("\r\n");
                }
            } else if (keyword.startsWith("Enchant")) {
                String k = keyword;
                k = k.replace("Curse", "");
                sbLong.append(k).append("\r\n");
            } else if (keyword.startsWith("Fading") || keyword.startsWith("Ripple") || keyword.startsWith("Vanishing")) {
                sbLong.append(keyword.replace(":", " ")).append("\r\n");
            } else if (keyword.startsWith("Madness")) {
                String[] parts = keyword.split(":");
                // If no colon exists in Madness keyword, it must have been granted and assumed the cost from host
                if (parts.length < 2) {
                    sbLong.append(parts[0]).append(" ").append(this.getManaCost()).append("\r\n");
                } else {
                    sbLong.append(parts[0]).append(" ").append(ManaCostParser.parse(parts[1])).append("\r\n");
                }
            } else if (keyword.startsWith("Morph")) {
                sbLong.append("Morph");
                if (keyword.contains(":")) {
                    final Cost mCost = new Cost(keyword.substring(6), true);
                    if (!mCost.isOnlyManaCost()) {
                        sbLong.append(" -");
                    }
                    sbLong.append(" ").append(mCost.toString()).delete(sbLong.length() - 2, sbLong.length());
                    if (!mCost.isOnlyManaCost()) {
                        sbLong.append(".");
                    }
                    sbLong.append("\r\n");
                }
            } else if (keyword.startsWith("Megamorph")) {
                sbLong.append("Megamorph");
                if (keyword.contains(":")) {
                    final Cost mCost = new Cost(keyword.substring(10), true);
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
                final String[] upkeepCostParams = keyword.split(":");
                sbLong.append(upkeepCostParams.length > 2 ? "- " + upkeepCostParams[2] : ManaCostParser.parse(upkeepCostParams[1]));
                sbLong.append(" (At the beginning of your upkeep, if CARDNAME came under your control since the beginning of your last upkeep, sacrifice it unless you pay its echo cost.)");
                sbLong.append("\r\n");
            } else if (keyword.startsWith("Cumulative upkeep")) {
                sbLong.append("Cumulative upkeep ");
                final String[] upkeepCostParams = keyword.split(":");
                sbLong.append(upkeepCostParams.length > 2 ? "- " + upkeepCostParams[2] : ManaCostParser.parse(upkeepCostParams[1]));
                sbLong.append("\r\n");
            } else if (keyword.startsWith("Amplify")) {
                sbLong.append("Amplify ");
                final String[] ampParams = keyword.split(":");
                final String magnitude = ampParams[1];
                sbLong.append(magnitude);
                sbLong.append(" (As this creature enters the battlefield, put a +1/+1 counter on it for each ");
                sbLong.append(ampParams[2].replace(",", " and/or ")).append(" card you reveal in your hand.)");
                sbLong.append("\r\n");
            }  else if (keyword.startsWith("Alternative Cost")) {
                sbLong.append("Has alternative cost.");
            } else if (keyword.startsWith("AlternateAdditionalCost")) {
                final String costString1 = keyword.split(":")[1];
                final String costString2 = keyword.split(":")[2];
                final Cost cost1 = new Cost(costString1, false);
                final Cost cost2 = new Cost(costString2, false);
                sbLong.append("As an additional cost to cast ")
                        .append(getName()).append(", ")
                        .append(cost1.toSimpleString())
                        .append(" or pay ")
                        .append(cost2.toSimpleString())
                        .append(".\r\n");
            } else if (keyword.startsWith("Kicker")) {
                if (!keyword.endsWith("Generic")) {
                    final Cost cost = new Cost(keyword.substring(7), false);
                    sbLong.append("Kicker ").append(cost.toSimpleString()).append("\r\n");
                }
            } else if (keyword.endsWith(".") && !keyword.startsWith("Haunt")) {
                sbLong.append(keyword).append("\r\n");
            } else if (keyword.contains("At the beginning of your upkeep, ")
                    && keyword.contains(" unless you pay")) {
                sbLong.append(keyword).append("\r\n");
            } else if (keyword.startsWith("Sunburst") && hasStartOfKeyword("Modular")) {
            } else if (keyword.startsWith("Modular") || keyword.startsWith("Soulshift") || keyword.startsWith("Bloodthirst")
                    || keyword.startsWith("ETBReplacement") || keyword.startsWith("MayEffectFromOpeningHand")) {
            } else if (keyword.startsWith("Provoke") || keyword.startsWith("Devour")) {
                sbLong.append(keyword + "(" + Keyword.getInstance(keyword).getReminderText() + ")");
            } else if (keyword.contains("Haunt")) {
                sb.append("\r\nHaunt (");
                if (isCreature()) {
                    sb.append("When this creature dies, exile it haunting target creature.");
                } else {
                    sb.append("When this spell card is put into a graveyard after resolving, ");
                    sb.append("exile it haunting target creature.");
                }
                sb.append(")");
            } else if (keyword.equals("Convoke") || keyword.equals("Menace") || keyword.equals("Dethrone")) {
                sb.append(keyword + " (" + Keyword.getInstance(keyword).getReminderText() + ")");
            } else if (keyword.endsWith(" offering")) {
                String offeringType = keyword.split(" ")[0];
                if (sb.length() != 0) {
                    sb.append("\r\n");
                }
                sbLong.append(keyword);
                sbLong.append(" (You may cast this card any time you could cast an instant by sacrificing a ");
                sbLong.append(offeringType);
                sbLong.append("and paying the difference in mana costs between this and the sacrificed ");
                sbLong.append(offeringType);
                sbLong.append(". Mana cost includes color.)");
            } else if (keyword.startsWith("Soulbond")) {
                sbLong.append(keyword + " (" + Keyword.getInstance(keyword).getReminderText() + ")");
            } else if (keyword.startsWith("Equip") || keyword.startsWith("Fortify") || keyword.startsWith("Outlast")
                    || keyword.startsWith("Unearth") || keyword.startsWith("Scavenge")) {
                // keyword parsing takes care of adding a proper description
            } else if (keyword.startsWith("CantBeBlockedBy")) {
                sbLong.append(getName()).append(" can't be blocked ");
                if (keyword.startsWith("CantBeBlockedByAmount"))
                    sbLong.append(getTextForKwCantBeBlockedByAmount(keyword));
                else
                    sbLong.append(getTextForKwCantBeBlockedByType(keyword));
            } else if (keyword.startsWith("CantBlock")) {
                sbLong.append(getName()).append(" can't block ");
                if (keyword.contains("CardUID")) {
                    sbLong.append("CardID (").append(Integer.valueOf(keyword.split("CantBlockCardUID_")[1])).append(")");
                } else {
                    final String[] k = keyword.split(":");
                    sbLong.append(k.length > 1 ? k[1] + ".\r\n" : "");
                }
            } else if (keyword.equals("Unblockable")) {
                sbLong.append(getName()).append(" can't be blocked.\r\n");
            }
            else {
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
        return sb.toString();
    }

    private static String getTextForKwCantBeBlockedByAmount(final String keyword) {
        final String restriction = keyword.split(" ", 2)[1];
        final boolean isLT = "LT".equals(restriction.substring(0,2));
        final String byClause = isLT ? "except by " : "by more than ";
        final int cnt = Integer.parseInt(restriction.substring(2));
        return byClause + Lang.nounWithNumeral(cnt, isLT ? "or more creature" : "creature");
    }

    private static String getTextForKwCantBeBlockedByType(final String keyword) {
        boolean negative = true;
        final List<String> subs = Lists.newArrayList(TextUtil.split(keyword.split(" ", 2)[1], ','));
        final List<List<String>> subsAnd = Lists.newArrayList();
        final List<String> orClauses = new ArrayList<>();
        for (final String expession : subs) {
            final List<String> parts = Lists.newArrayList(expession.split("[.+]"));
            for (int p = 0; p < parts.size(); p++) {
                final String part = parts.get(p);
                if (part.equalsIgnoreCase("creature")) {
                    parts.remove(p--);
                    continue;
                }
                // based on suppossition that each expression has at least 1 predicate except 'creature'
                negative &= part.contains("non") || part.contains("without");
            }
            subsAnd.add(parts);
        }

        final boolean allNegative = negative;
        final String byClause = allNegative ? "except by " : "by ";

        final Function<Pair<Boolean, String>, String> withToString = new Function<Pair<Boolean, String>, String>() {
            @Override
            public String apply(Pair<Boolean, String> inp) {
                boolean useNon = inp.getKey() == allNegative;
                return (useNon ? "*NO* " : "") + inp.getRight();
            }
        };

        for (final List<String> andOperands : subsAnd) {
            final List<Pair<Boolean, String>> prependedAdjectives = Lists.newArrayList();
            final List<Pair<Boolean, String>> postponedAdjectives = Lists.newArrayList();
            String creatures = null;

            for (String part : andOperands) {
                boolean positive = true;
                if (part.startsWith("non")) {
                    part = part.substring(3);
                    positive = false;
                }
                if (part.startsWith("with")) {
                    positive = !part.startsWith("without");
                    postponedAdjectives.add(Pair.of(positive, part.substring(positive ? 4 : 7)));
                } else if (part.startsWith("powerLEX")) {// Kraken of the Straits
                    postponedAdjectives.add(Pair.of(true, "power less than the number of islands you control"));
                } else if (part.startsWith("power")) {
                    int kwLength = 5;
                    String opName = Expressions.operatorName(part.substring(kwLength, kwLength + 2));
                    String operand = part.substring(kwLength + 2);
                    postponedAdjectives.add(Pair.of(true, "power" + opName + operand));
                } else if (CardType.isACreatureType(part)) {
                    if (creatures != null && CardType.isACreatureType(creatures)) { // e.g. Kor Castigator
                        creatures = StringUtils.capitalize(Lang.getPlural(part)) + creatures;
                    } else {
                        creatures = StringUtils.capitalize(Lang.getPlural(part)) + (creatures == null ? "" : " or " + creatures);
                    }
                } else {
                    prependedAdjectives.add(Pair.of(positive, part.toLowerCase()));
                }
            }

            StringBuilder sbShort = new StringBuilder();
            if (allNegative) {
                boolean isFirst = true;
                for (Pair<Boolean, String> pre : prependedAdjectives) {
                    if (isFirst) isFirst = false;
                    else sbShort.append(" and/or ");

                    boolean useNon = pre.getKey() == allNegative;
                    if (useNon) sbShort.append("non-");
                    sbShort.append(pre.getValue()).append(" ").append(creatures == null ? "creatures" : creatures);
                }
                if (prependedAdjectives.isEmpty())
                    sbShort.append(creatures == null ? "creatures" : creatures);

                if (!postponedAdjectives.isEmpty()) {
                    if (!prependedAdjectives.isEmpty()) {
                        sbShort.append(" and/or creatures");
                    }

                    sbShort.append(" with ");
                    sbShort.append(Lang.joinHomogenous(postponedAdjectives, withToString, allNegative ? "or" : "and"));
                }

            } else {
                for (Pair<Boolean, String> pre : prependedAdjectives) {
                    boolean useNon = pre.getKey() == allNegative;
                    if (useNon) sbShort.append("non-");
                    sbShort.append(pre.getValue()).append(" ");
                }
                sbShort.append(creatures == null ? "creatures" : creatures);

                if (!postponedAdjectives.isEmpty()) {
                    sbShort.append(" with ");
                    sbShort.append(Lang.joinHomogenous(postponedAdjectives, withToString, allNegative ? "or" : "and"));
                }

            }
            orClauses.add(sbShort.toString());
        }
        return byClause + StringUtils.join(orClauses, " or ") + ".";
    }

    // get the text of the abilities of a card
    public String getAbilityText() {
        return getAbilityText(currentState);
    }
    public String getAbilityText(final CardState state) {
        final CardTypeView type = state.getType();

        final StringBuilder sb = new StringBuilder();
        if (!mayPlay.isEmpty()) {
            sb.append("May be played by: ");
            sb.append(Lang.joinHomogenous(mayPlay.entrySet(), new Function<Entry<Player, CardPlayOption>, String>() {
                @Override public String apply(final Entry<Player, CardPlayOption> entry) {
                    return entry.getKey().toString() + entry.getValue().toString();
                }
            }));
            sb.append("\r\n");
        }

        if (type.isInstant() || type.isSorcery()) {
            sb.append(abilityTextInstantSorcery(state));

            if (haunting != null) {
                sb.append("Haunting: ").append(haunting);
                sb.append("\r\n");
            }

            while (sb.toString().endsWith("\r\n")) {
                sb.delete(sb.lastIndexOf("\r\n"), sb.lastIndexOf("\r\n") + 3);
            }

            return sb.toString().replaceAll("CARDNAME", state.getName());
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
        sb.append(keywordsToText(getUnhiddenKeywords(state)));

        // Process replacement effects first so that ETB tabbed can be printed here.
        // The rest will be printed later.
        StringBuilder replacementEffects = new StringBuilder();
        for (final ReplacementEffect replacementEffect : state.getReplacementEffects()) {
            if (!replacementEffect.isSecondary()) {
                String text = replacementEffect.toString();
                if (text.equals("CARDNAME enters the battlefield tapped.")) {
                    sb.append(text).append("\r\n");
                } else {
                    replacementEffects.append(text).append("\r\n");
                }
            }
        }
        
        // Give spellText line breaks for easier reading
        sb.append("\r\n");
        sb.append(text.replaceAll("\\\\r\\\\n", "\r\n"));
        sb.append("\r\n");

        // Triggered abilities
        for (final Trigger trig : state.getTriggers()) {
            if (!trig.isSecondary()) {
                sb.append(trig.toString().replaceAll("\\\\r\\\\n", "\r\n")).append("\r\n");
            }
        }

        // Replacement effects
        sb.append(replacementEffects);

        // static abilities
        for (final StaticAbility stAb : state.getStaticAbilities()) {
            sb.append(stAb.toString()).append("\r\n");
        }

        final List<String> addedManaStrings = new ArrayList<>();
        boolean primaryCost = true;
        boolean isNonAura = !type.hasSubtype("Aura");

        for (final SpellAbility sa : state.getSpellAbilities()) {
            // only add abilities not Spell portions of cards
            if (sa == null || !state.getType().isPermanent()) {
                continue;
            }

            boolean isNonAuraPermanent = (sa instanceof SpellPermanent) && isNonAura;
            if (isNonAuraPermanent && primaryCost) {
                // For Alt costs, make sure to display the cost!
                primaryCost = false;
                continue;
            }

            final String sAbility = formatSpellAbility(sa);

            if (sa.getManaPart() != null) {
                if (addedManaStrings.contains(sAbility)) {
                    continue;
                }
                addedManaStrings.add(sAbility);
            }

            if (isNonAuraPermanent) {
                sb.insert(0, "\r\n");
                sb.insert(0, sAbility);
            }
            else if (!sAbility.endsWith(state.getName() + "\r\n")) {
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

        return sb.toString().replaceAll("CARDNAME", state.getName()).trim();
    }

    private StringBuilder abilityTextInstantSorcery(CardState state) {
        final StringBuilder sb = new StringBuilder();

        // Give spellText line breaks for easier reading
        sb.append(text.replaceAll("\\\\r\\\\n", "\r\n"));

        // NOTE:
        if (sb.toString().contains(" (NOTE: ")) {
            sb.insert(sb.indexOf("(NOTE: "), "\r\n");
        }
        if (sb.toString().contains("(NOTE: ") && sb.toString().endsWith(".)") && !sb.toString().endsWith("\r\n")) {
            sb.append("\r\n");
        }

        // I think SpellAbilities should be displayed after Keywords
        // Add SpellAbilities
        for (final SpellAbility element : state.getSpellAbilities()) {
            sb.append(formatSpellAbility(element));
        }

        // Add Keywords
        final List<String> kw = getKeywords(state);

        // Triggered abilities
        for (final Trigger trig : state.getTriggers()) {
            if (!trig.isSecondary()) {
                sb.append(trig.toString()).append("\r\n");
            }
        }

        // Replacement effects
        for (final ReplacementEffect replacementEffect : state.getReplacementEffects()) {
            sb.append(replacementEffect.toString()).append("\r\n");
        }

        // static abilities
        for (final StaticAbility stAb : state.getStaticAbilities()) {
            final String stAbD = stAb.toString();
            if (!stAbD.equals("")) {
                sb.append(stAbD).append("\r\n");
            }
        }

        // TODO A lot of these keywords should really show up before the SAs
        // keyword descriptions
        for (final String keyword : kw) {
            if ((keyword.startsWith("Ripple") && !sb.toString().contains("Ripple"))
                    || (keyword.startsWith("Dredge") && !sb.toString().contains("Dredge"))
                    || (keyword.startsWith("CARDNAME is ") && !sb.toString().contains("CARDNAME is "))) {
                sb.append(keyword.replace(":", " ")).append("\r\n");
            } else if ((keyword.startsWith("Madness") && !sb.toString().contains("Madness"))
                    || (keyword.startsWith("Recover") && !sb.toString().contains("Recover"))
                    || (keyword.startsWith("Miracle") && !sb.toString().contains("Miracle"))) {
                String[] parts = keyword.split(":");
                sb.append(parts[0]).append(" ").append(ManaCostParser.parse(parts[1])).append("\r\n");
            } else if (keyword.equals("CARDNAME can't be countered.")
                    || keyword.startsWith("May be played") || keyword.startsWith("Conspire")
                    || keyword.startsWith("Cascade") || keyword.startsWith("Wither")
                    || (keyword.startsWith("Epic") && !sb.toString().contains("Epic"))
                    || (keyword.startsWith("Split second") && !sb.toString().contains("Split second"))
                    || (keyword.startsWith("Devoid"))) {
                sb.append(keyword).append("\r\n");
            } else if (keyword.equals("You may cast CARDNAME as though it had flash if you pay 2 more to cast it.")) {
                sb.append(keyword).append("\r\n");
            } else if (keyword.startsWith("Flashback")) {
                sb.append("Flashback");
                if (keyword.contains(" ")) {
                    final Cost fbCost = new Cost(keyword.substring(10), true);
                    if (!fbCost.isOnlyManaCost()) {
                        sb.append(" -");
                    }
                    sb.append(" ").append(fbCost.toString()).delete(sb.length() - 2, sb.length());
                    if (!fbCost.isOnlyManaCost()) {
                        sb.append(".");
                    }
                }
                sb.append("\r\n");
            } else if (keyword.startsWith("Emerge")) {
                final Cost cost = new Cost(keyword.substring(7), false);
                sb.append("Emerge ").append(cost.toSimpleString());
                sb.append(" (You may cast this spell by sacrificing a creature and paying the emerge cost reduced by that creature's converted mana cost.)");
                sb.append("\r\n");
            } else if (keyword.startsWith("Splice")) {
                final Cost cost = new Cost(keyword.substring(19), false);
                sb.append("Splice onto Arcane ").append(cost.toSimpleString()).append("\r\n");
            } else if (keyword.startsWith("Buyback")) {
                final Cost cost = new Cost(keyword.substring(8), false);
                sb.append("Buyback ").append(cost.toSimpleString());
                sb.append(" (You may pay an additional cost as you cast CARDNAME. If you do, put CARDNAME back into your hand as it resolves.)");
                sb.append("\r\n");
            } else if (keyword.startsWith("Entwine")) {
                final Cost cost = new Cost(keyword.substring(8), false);
                sb.append("Entwine ").append(cost.toSimpleString());
                sb.append(" (Choose both if you pay the entwine cost.)");
                sb.append("\r\n");
            } else if (keyword.startsWith("Multikicker")) {
                if (!keyword.endsWith("Generic")) {
                    final Cost cost = new Cost(keyword.substring(7), false);
                    sb.append("Multikicker ").append(cost.toSimpleString()).append("\r\n");
                }
            } else if (keyword.startsWith("Kicker")) {
                if (!keyword.endsWith("Generic")) {
                    final Cost cost = new Cost(keyword.substring(7), false);
                    sb.append("Kicker ").append(cost.toSimpleString()).append("\r\n");
                }
            } else if (keyword.startsWith("AlternateAdditionalCost")) {
                final String costString1 = keyword.split(":")[1];
                final String costString2 = keyword.split(":")[2];
                final Cost cost1 = new Cost(costString1, false);
                final Cost cost2 = new Cost(costString2, false);
                sb.append("As an additional cost to cast ")
                        .append(state.getName()).append(", ")
                        .append(cost1.toSimpleString())
                        .append(" or pay ")
                        .append(cost2.toSimpleString())
                        .append(".\r\n");
            } else if (keyword.startsWith("Storm")) {
                if (sb.toString().contains("Target") || sb.toString().contains("target")) {
                    sb.insert(
                            sb.indexOf("Storm (When you cast this spell, copy it for each spell cast before it this turn.") + 81,
                            " You may choose new targets for the copies.");
                }
            } else if (keyword.startsWith("Replicate") && !sb.toString().contains("you paid its replicate cost.")) {
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
                if (state.getType().isCreature()) {
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
                sb.append("Convoke (Each creature you tap while casting this spell pays for {1} or one mana of that creature's color.)\r\n");
            } else if (keyword.equals("Delve")) {
                if (sb.toString().endsWith("\r\n\r\n")) {
                    sb.delete(sb.lastIndexOf("\r\n"), sb.lastIndexOf("\r\n") + 3);
                }
                sb.append("Delve (Each card you exile from your graveyard while casting this spell pays for {1}.)\r\n");
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

    private String formatSpellAbility(final SpellAbility sa) {
        final StringBuilder sb = new StringBuilder();
        final String elementText = sa.toString();

        //Determine if a card has multiple choices, then format it in an easier to read list.
        if (ApiType.Charm.equals(sa.getApi())) {
            // Only split once! Otherwise some Charm spells looks broken
            final String[] splitElemText = elementText.split("-", 2);
            final String chooseText = splitElemText[0].trim();
            final String[] choices = splitElemText.length > 1 ? splitElemText[1].split(";") : null;

            sb.append(chooseText);

            if (choices != null) {
                sb.append(" \u2014\r\n");
                for (int i = 0; i < choices.length; i++) {
                    String choice = choices[i].trim();

                    if (choice.startsWith("Or ") || choice.startsWith("or ")) {
                        choice = choice.substring(3);
                    }

                    sb.append("\u2022 ").append(Character.toUpperCase(choice.charAt(0)))
                            .append(choice.substring(1));
                    if (i < choices.length - 1) {
                        sb.append(".");
                    }
                    sb.append("\r\n");
                }
            }
        } else {
            sb.append(elementText).append("\r\n");
        }
        return sb.toString();
    }

    public final boolean canProduceSameManaTypeWith(final Card c) {
        final FCollectionView<SpellAbility> manaAb = getManaAbilities();
        if (manaAb.isEmpty()) {
            return false;
        }
        Set<String> colors = new HashSet<>();
        for (final SpellAbility ab : c.getManaAbilities()) {
            if (ab.getApi() == ApiType.ManaReflected) {
                colors.addAll(CardUtil.getReflectableManaColors(ab));
            } else {
                colors = CardUtil.canProduce(6, ab.getManaPart(), colors);
            }
        }

        for (final SpellAbility mana : manaAb) {
            for (String s : colors) {
                if (mana.getApi() == ApiType.ManaReflected) {
                    if (CardUtil.getReflectableManaColors(mana).contains(s)) {
                        return true;
                    }
                } else {
                    if (mana.getManaPart().canProduce(MagicColor.toShortString(s))) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public final void clearFirstSpell() {
        currentState.clearFirstSpell();
    }

    public final SpellAbility getFirstSpellAbility() {
        return currentState.getNonManaAbilities().isEmpty() ? null : currentState.getNonManaAbilities().getFirst();
    }

    /**
     * @return the first {@link SpellAbility} marked as a Spell with API type
     * {@link ApiType#Attach} in this {@link Card}, or {@code null} if no such
     * object exists.
     * @see SpellAbility#isSpell()
     */
    public final SpellAbility getFirstAttachSpell() {
        for (final SpellAbility sa : getSpells()) {
            if (sa.isSpell() && sa.getApi() == ApiType.Attach) {
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
        a.setHostCard(this);
        currentState.addSpellAbility(a);
        currentState.getView().updateAbilityText(this, currentState);
    }

    public final void removeSpellAbility(final SpellAbility a) {
        currentState.removeSpellAbility(a);
        currentState.getView().updateAbilityText(this, currentState);
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
    public final Iterable<String> getUnparsedAbilities() {
        return currentState.getUnparsedAbilities();
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
        final FCollection<SpellAbility> res = new FCollection<>();
        for (final SpellAbility sa : currentState.getNonManaAbilities()) {
            if (sa.isSpell() && sa.isBasicSpell()) {
                res.add(sa);
            }
        }
        return res;
    }

    // shield = regeneration
    public final Iterable<CardShields> getShields() {
        return shields;
    }
    public final int getShieldCount() {
        return shields.size();
    }

    public final void addShield(final CardShields shield) {
        shields.add(shield);
        view.updateShieldCount(this);
    }

    public final void subtractShield(CardShields shield) {
        if (shield != null && shield.hasTrigger()) {
            getGame().getStack().addSimultaneousStackEntry(shield.getTriggerSA());
        }
        if (shields.remove(shield)) {
            view.updateShieldCount(this);
        }
    }

    public final void resetShield() {
        if (shields.isEmpty()) { return; }
        shields.clear();
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
        return !hasKeyword("CARDNAME can't be regenerated.");
    }

    // is this "Card" supposed to be a token?
    public final boolean isToken() {
        return token;
    }
    public final void setToken(boolean token0) {
        if (token == token0) { return; }
        token = token0;
        view.updateToken(this);
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
        return currentStateName == CardStateName.FaceDown;
    }

    public final void setCanCounter(final boolean b) {
        canCounter = b;
    }

    public final boolean getCanCounter() {
        return canCounter;
    }

    public final void addComesIntoPlayCommand(final GameCommand c) {
        etbCommandList.add(c);
    }

    public final void runComesIntoPlayCommands() {
        for (final GameCommand c : etbCommandList) {
            c.run();
        }
    }

    public final void addLeavesPlayCommand(final GameCommand c) {
        leavePlayCommandList.add(c);
    }

    public final void runLeavesPlayCommands() {
        for (final GameCommand c : leavePlayCommandList) {
            c.run();
        }
    }

    public final void addUntapCommand(final GameCommand c) {
        untapCommandList.add(c);
    }

    public final void addChangeControllerCommand(final GameCommand c) {
        changeControllerCommandList.add(c);
    }

    public final void runChangeControllerCommands() {
        for (final GameCommand c : changeControllerCommandList) {
            c.run();
        }
    }

    public final void setSickness(boolean sickness0) {
        if (sickness == sickness0) { return; }
        sickness = sickness0;
        view.updateSickness(this);
    }

    public final boolean isFirstTurnControlled() {
        return sickness;
    }

    public final boolean hasSickness() {
        return sickness && !hasKeyword("Haste");
    }

    public final boolean isSick() {
        return sickness && isCreature() && !hasKeyword("Haste");
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
        // Remove each key that yields this player
        this.tempControllers.values().remove(player);
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

    public final void setMayLookAt(final Player player, final boolean mayLookAt) {
        setMayLookAt(player, mayLookAt, false);
    }
    public final void setMayLookAt(final Player player, final boolean mayLookAt, final boolean temp) {
        view.setPlayerMayLook(player, mayLookAt, temp);
    }

    public final CardPlayOption mayPlay(final Player player) {
        return mayPlay.get(player);
    }
    public final void setMayPlay(final Player player, final boolean withoutManaCost, final boolean ignoreColor) {
        final CardPlayOption option = this.mayPlay.get(player);
        this.mayPlay.put(player, option == null ? new CardPlayOption(withoutManaCost, ignoreColor) : option.add(withoutManaCost, ignoreColor));
    }
    public final void removeMayPlay(final Player player) {
        this.mayPlay.remove(player);
    }

    public final CardCollectionView getEquippedBy(boolean allowModify) {
        return CardCollection.getView(equippedBy, allowModify);
    }
    public final void setEquippedBy(final CardCollection cards) {
        equippedBy = view.setCards(equippedBy, cards, TrackableProperty.EquippedBy);
    }
    public final void setEquippedBy(final Iterable<Card> cards) {
        equippedBy = view.setCards(equippedBy, cards, TrackableProperty.EquippedBy);
    }
    public final boolean isEquipped() {
        return FCollection.hasElements(equippedBy);
    }
    public final boolean isEquippedBy(Card c) {
        return FCollection.hasElement(equippedBy, c);
    }
    public final boolean isEquippedBy(final String cardName) {
        for (final Card card : getEquippedBy(false)) {
            if (card.getName().equals(cardName)) {
                return true;
            }
        }
        return false;
    }

    public final CardCollectionView getFortifiedBy(boolean allowModify) {
        return CardCollection.getView(fortifiedBy, allowModify);
    }
    public final void setFortifiedBy(final CardCollection cards) {
        fortifiedBy = view.setCards(fortifiedBy, cards, TrackableProperty.FortifiedBy);
    }
    public final void setFortifiedBy(final Iterable<Card> cards) {
        fortifiedBy = view.setCards(fortifiedBy, cards, TrackableProperty.FortifiedBy);
    }
    public final boolean isFortified() {
        return FCollection.hasElements(fortifiedBy);
    }
    public final boolean isFortifiedBy(Card c) {
        return FCollection.hasElement(fortifiedBy, c);
    }

    public final Card getEquipping() {
        return equipping;
    }
    public final void setEquipping(final Card card) {
        equipping = view.setCard(equipping, card, TrackableProperty.Equipping);
    }
    public final boolean isEquipping() {
        return equipping != null;
    }

    public final Card getFortifying() {
        return fortifying;
    }
    public final void setFortifying(final Card card) {
        fortifying = view.setCard(fortifying, card, TrackableProperty.Fortifying);
    }
    public final boolean isFortifying() {
        return fortifying != null;
    }

    public final void equipCard(final Card c) {
        if (c.hasKeyword("CARDNAME can't be equipped.")) {
            getGame().getGameLog().add(GameLogEntryType.STACK_RESOLVE, "Trying to equip " + c.getName() + " but it can't be equipped.");
            return;
        }
        if (hasStartOfKeyword("CantEquip")) {
            final int keywordPosition = getKeywordPosition("CantEquip");
            final String parse = getKeywords().get(keywordPosition);
            final String[] k = parse.split(" ", 2);
            final String[] restrictions = k[1].split(",");
            if (c.isValid(restrictions, getController(), this, null)) {
                getGame().getGameLog().add(GameLogEntryType.STACK_RESOLVE, "Trying to equip " + c.getName() + " but it can't be equipped.");
                return;
            }
        }

        Card oldTarget = null;
        if (isEquipping()) {
            oldTarget = equipping;
            if (oldTarget.equals(c)) {
                // If attempting to reattach to the same object, don't do anything.
                return;
            }
            unEquipCard(oldTarget);
        }

        // They use double links... it's doubtful
        setEquipping(c);
        setTimestamp(getGame().getNextTimestamp());
        c.equippedBy = c.view.addCard(c.equippedBy, this, TrackableProperty.EquippedBy);

        // Play the Equip sound
        getGame().fireEvent(new GameEventCardAttachment(this, oldTarget, c, AttachMethod.Equip));

        // run trigger
        final HashMap<String, Object> runParams = new HashMap<>();
        runParams.put("AttachSource", this);
        runParams.put("AttachTarget", c);
        getController().getGame().getTriggerHandler().runTrigger(TriggerType.Attached, runParams, false);
    }

    public final void fortifyCard(final Card c) {
        Card oldTarget = null;
        if (isFortifying()) {
            oldTarget = fortifying;
            unFortifyCard(oldTarget);
        }

        setFortifying(c);
        setTimestamp(getGame().getNextTimestamp());
        c.fortifiedBy = c.view.addCard(c.fortifiedBy, this, TrackableProperty.FortifiedBy);

        // Play the Equip sound
        getGame().fireEvent(new GameEventCardAttachment(this, oldTarget, c, AttachMethod.Fortify));
        // run trigger
        final HashMap<String, Object> runParams = new HashMap<>();
        runParams.put("AttachSource", this);
        runParams.put("AttachTarget", c);
        getController().getGame().getTriggerHandler().runTrigger(TriggerType.Attached, runParams, false);
    }

    public final void unEquipCard(final Card c) { // equipment.unEquipCard(equippedCard);
        if (equipping == c) {
            setEquipping(null);
        }
        c.equippedBy = c.view.removeCard(c.equippedBy, this, TrackableProperty.EquippedBy);

        getGame().fireEvent(new GameEventCardAttachment(this, c, null, AttachMethod.Equip));

        // Run triggers
        final Map<String, Object> runParams = new TreeMap<>();
        runParams.put("Equipment", this);
        runParams.put("Card", c);
        getGame().getTriggerHandler().runTrigger(TriggerType.Unequip, runParams, false);
    }

    public final void unFortifyCard(final Card c) { // fortification.unEquipCard(fortifiedCard);
        if (fortifying == c) {
            setFortifying(null);
        }
        c.fortifiedBy = c.view.removeCard(c.fortifiedBy, this, TrackableProperty.FortifiedBy);

        getGame().fireEvent(new GameEventCardAttachment(this, c, null, AttachMethod.Fortify));
    }

    public final void unEquipAllCards() {
        if (isEquipped()) {
            for (Card c : getEquippedBy(true)) {
                c.unEquipCard(this);
            }
        }
    }

    public final GameEntity getEnchanting() {
        return enchanting;
    }
    public final void setEnchanting(final GameEntity e) {
        if (enchanting == e) { return; }
        enchanting = e;
        view.updateEnchanting(this);
    }
    public final Card getEnchantingCard() {
        if (enchanting instanceof Card) {
            return (Card) enchanting;
        }
        return null;
    }
    public final Player getEnchantingPlayer() {
        if (enchanting instanceof Player) {
            return (Player) enchanting;
        }
        return null;
    }
    public final boolean isEnchanting() {
        return enchanting != null;
    }
    public final boolean isEnchantingCard() {
        return getEnchantingCard() != null;
    }
    public final boolean isEnchantingPlayer() {
        return getEnchantingPlayer() != null;
    }

    public final void removeEnchanting(final GameEntity e) {
        if (enchanting == e) {
            setEnchanting(null);
        }
    }

    public final void enchantEntity(final GameEntity entity) {
        if (entity.hasKeyword("CARDNAME can't be enchanted.")
                || entity.hasKeyword("CARDNAME can't be enchanted in the future.")) {
            getGame().getGameLog().add(GameLogEntryType.STACK_RESOLVE, "Trying to enchant " + entity.getName()
            + " but it can't be enchanted.");
            return;
        }
        setEnchanting(entity);
        setTimestamp(getGame().getNextTimestamp());
        entity.addEnchantedBy(this);

        getGame().fireEvent(new GameEventCardAttachment(this, null, entity, AttachMethod.Enchant));

        // run trigger
        final HashMap<String, Object> runParams = new HashMap<>();
        runParams.put("AttachSource", this);
        runParams.put("AttachTarget", entity);
        getController().getGame().getTriggerHandler().runTrigger(TriggerType.Attached, runParams, false);
    }

    public final void unEnchantEntity(final GameEntity entity) {
        if (enchanting == null || !enchanting.equals(entity)) {
            return;
        }

        setEnchanting(null);
        entity.removeEnchantedBy(this);
        if (isBestowed()) {
            unanimateBestow();
        }
        getGame().fireEvent(new GameEventCardAttachment(this, entity, null, AttachMethod.Enchant));
    }

    public final void setType(final CardType type0) {
        currentState.setType(type0);
    }

    public final void addType(final String type0) {
        currentState.addType(type0);
    }

    public final CardTypeView getType() {
        if (changedCardTypes.isEmpty()) {
            return currentState.getType();
        }
        return currentState.getType().getTypeWithChanges(changedCardTypes);
    }

    public Map<Long, CardChangedType> getChangedCardTypes() {
        return Collections.unmodifiableMap(changedCardTypes);
    }

    public Map<Long, KeywordsChange> getChangedCardKeywords() {
        return changedCardKeywords;
    }

    public Map<Long, CardColor> getChangedCardColors() {
        return changedCardColors;
    }

    public final void addChangedCardTypes(final CardType addType, final CardType removeType,
            final boolean removeSuperTypes, final boolean removeCardTypes, final boolean removeSubTypes,
            final boolean removeCreatureTypes, final long timestamp) {

        changedCardTypes.put(timestamp, new CardChangedType(addType, removeType, removeSuperTypes, removeCardTypes, removeSubTypes, removeCreatureTypes));
        currentState.getView().updateType(currentState);
    }

    public final void addChangedCardTypes(final String[] types, final String[] removeTypes,
            final boolean removeSuperTypes, final boolean removeCardTypes, final boolean removeSubTypes,
            final boolean removeCreatureTypes, final long timestamp) {
        CardType addType = null;
        CardType removeType = null;
        if (types != null) {
            addType = new CardType(Arrays.asList(types));
        }

        if (removeTypes != null) {
            removeType = new CardType(Arrays.asList(removeTypes));
        }

        addChangedCardTypes(addType, removeType, removeSuperTypes, removeCardTypes, removeSubTypes,
                removeCreatureTypes, timestamp);
    }

    public final void removeChangedCardTypes(final long timestamp) {
        changedCardTypes.remove(timestamp);
        currentState.getView().updateType(currentState);
    }

    public final void addColor(final String s, final boolean addToColors, final long timestamp) {
        changedCardColors.put(timestamp, new CardColor(s, addToColors, timestamp));
        currentState.getView().updateColors(this);
    }

    public final void removeColor(final long timestampIn) {
        final CardColor removeCol = changedCardColors.remove(timestampIn);

        if (removeCol != null) {
            currentState.getView().updateColors(this);
        }
    }

    public final void setColor(final String color) {
        currentState.setColor(color);
        currentState.getView().updateColors(this);
    }
    public final void setColor(final byte color) {
        currentState.setColor(color);
        currentState.getView().updateColors(this);
    }

    public final ColorSet determineColor() {
        final Iterable<CardColor> colorList = changedCardColors.values();
        byte colors = currentState.getColor();
        for (final CardColor cc : colorList) {
            if (cc.isAdditional()) {
                colors |= cc.getColorMask();
            } else {
                colors = cc.getColorMask();
            }
        }
        return ColorSet.fromMask(colors);
    }

    // values that are printed on card
    public final int getBaseLoyalty() {
        return baseLoyalty;
    }

    public final int getCurrentLoyalty() {
        int loyalty = getCounters(CounterType.LOYALTY);
        if (loyalty == 0) {
            loyalty = baseLoyalty;
        }
        return loyalty;
    }

    // values that are printed on card
    public final void setBaseLoyalty(final int n) {
        if (baseLoyalty == n) { return; }
        baseLoyalty = n;
        currentState.getView().updateLoyalty(this);
    }

    // values that are printed on card
    public final int getBasePower() {
        return currentState.getBasePower();
    }

    public final int getBaseToughness() {
        return currentState.getBaseToughness();
    }

    // values that are printed on card
    public final void setBasePower(final int n) {
        currentState.setBasePower(n);
    }

    public final void setBaseToughness(final int n) {
        currentState.setBaseToughness(n);
    }

    // values that are printed on card
    public final String getBasePowerString() {
        return (null == basePowerString) ? "" + getBasePower() : basePowerString;
    }

    public final String getBaseToughnessString() {
        return (null == baseToughnessString) ? "" + getBaseToughness() : baseToughnessString;
    }

    // values that are printed on card
    public final void setBasePowerString(final String s) {
        basePowerString = s;
    }

    public final void setBaseToughnessString(final String s) {
        baseToughnessString = s;
    }

    public final int getSetPower() {
        if (newPT.isEmpty()) {
            return -1;
        }
        return getLatestPT().getLeft();
    }

    public final int getSetToughness() {
        if (newPT.isEmpty()) {
            return -1;
        }
        return getLatestPT().getRight();
    }

    /**
     *
     * Get the latest set Power and Toughness of this Card.
     *
     * @return the latest set Power and Toughness of this {@link Card} as the
     * left and right values of a {@link Pair}, respectively. A value of -1
     * means that particular property has not been set.
     */
    private synchronized Pair<Integer, Integer> getLatestPT() {
        // Find latest set power
        long maxPowerTimestamp = -2;
        int latestPower = -1;
        for (final CardPowerToughness pt : newPT) {
            if (pt.getTimestamp() >= maxPowerTimestamp && pt.getPower() != -1) {
                maxPowerTimestamp = pt.getTimestamp();
                latestPower = pt.getPower();
            }
        }

        // Find latest set toughness
        long maxToughnessTimestamp = -2;
        int latestToughness = -1;
        for (final CardPowerToughness pt : newPT) {
            if (pt.getTimestamp() >= maxToughnessTimestamp && pt.getToughness() != -1) {
                maxToughnessTimestamp = pt.getTimestamp();
                latestToughness = pt.getToughness();
            }
        }

        return Pair.of(latestPower, latestToughness);
    }

    public final void addNewPT(final int power, final int toughness, final long timestamp) {
        newPT.add(new CardPowerToughness(power, toughness, timestamp));
        currentState.getView().updatePower(this);
        currentState.getView().updateToughness(this);
    }

    public final void removeNewPT(final long timestamp) {
        for (int i = 0; i < newPT.size(); i++) {
            final CardPowerToughness cardPT = newPT.get(i);
            if (cardPT.getTimestamp() == timestamp) {
                if (newPT.remove(cardPT)) {
                    currentState.getView().updatePower(this);
                    currentState.getView().updateToughness(this);
                }
            }
        }
    }

    public final int getCurrentPower() {
        int total = getBasePower();
        final int setPower = getSetPower();
        if (setPower != -1) {
            total = setPower;
        }
        return total;
    }

    public final StatBreakdown getUnswitchedPowerBreakdown() {
        return new StatBreakdown(getCurrentPower(), getTempPowerBoost(), getSemiPermanentPowerBoost(), getPowerBonusFromCounters());
    }
    public final int getUnswitchedPower() {
        return getUnswitchedPowerBreakdown().getTotal();
    }

    public final int getPowerBonusFromCounters() {
        return getCounters(CounterType.P1P1) + getCounters(CounterType.P1P2) + getCounters(CounterType.P1P0)
                - getCounters(CounterType.M1M1) + 2 * getCounters(CounterType.P2P2) - 2 * getCounters(CounterType.M2M1)
                - 2 * getCounters(CounterType.M2M2) - getCounters(CounterType.M1M0) + 2 * getCounters(CounterType.P2P0);
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
        final int setToughness = getSetToughness();
        if (setToughness != -1) {
            total = setToughness;
        }
        return total;
    }

    public static class StatBreakdown {
        public final int currentValue;
        public final int tempBoost;
        public final int semiPermanentBoost;
        public final int bonusFromCounters;
        public StatBreakdown() {
            this.currentValue = 0;
            this.tempBoost = 0;
            this.semiPermanentBoost = 0;
            this.bonusFromCounters = 0;
        }
        public StatBreakdown(int currentValue, int tempBoost, int semiPermanentBoost, int bonusFromCounters){
            this.currentValue = currentValue;
            this.tempBoost = tempBoost;
            this.semiPermanentBoost = semiPermanentBoost;
            this.bonusFromCounters = bonusFromCounters;
        }
        public int getTotal() {
            return currentValue + tempBoost + semiPermanentBoost + bonusFromCounters;
        }
        @Override
        public String toString() {
            return String.format("c:%d tb:%d spb:%d bfc:%d", currentValue, tempBoost, semiPermanentBoost, bonusFromCounters);
        }
    }

    public final StatBreakdown getUnswitchedToughnessBreakdown() {
        return new StatBreakdown(getCurrentToughness(), getTempToughnessBoost(), getSemiPermanentToughnessBoost(), getToughnessBonusFromCounters());
    }
    public final int getUnswitchedToughness() {
        return getUnswitchedToughnessBreakdown().getTotal();
    }

    public final int getToughnessBonusFromCounters() {
        return getCounters(CounterType.P1P1) + 2 * getCounters(CounterType.P1P2) - getCounters(CounterType.M1M1)
                + getCounters(CounterType.P0P1) - 2 * getCounters(CounterType.M0M2) + 2 * getCounters(CounterType.P2P2)
                - getCounters(CounterType.M0M1) - getCounters(CounterType.M2M1) - 2 * getCounters(CounterType.M2M2)
                + 2 * getCounters(CounterType.P0P2);
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
    	return getGame().getStaticEffects().getGlobalRuleChange(GlobalRuleChange.toughnessAssignsDamage)
        		|| hasKeyword("CARDNAME assigns combat damage equal to its toughness rather than its power");
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

    private int multiKickerMagnitude = 0;
    public final void addMultiKickerMagnitude(final int n) { multiKickerMagnitude += n; }
    public final void setKickerMagnitude(final int n) { multiKickerMagnitude = n; }
    public final int getKickerMagnitude() {
        if (multiKickerMagnitude > 0) {
            return multiKickerMagnitude;
        }
        boolean hasK1 = costsPaid.contains(OptionalCost.Kicker1);
        return hasK1 == costsPaid.contains(OptionalCost.Kicker2) ? (hasK1 ? 2 : 0) : 1;
    }

    private int pseudoKickerMagnitude = 0;
    public final void addPseudoMultiKickerMagnitude(final int n) { pseudoKickerMagnitude += n; }
    public final void setPseudoMultiKickerMagnitude(final int n) { pseudoKickerMagnitude = n; }
    public final int getPseudoKickerMagnitude() { return pseudoKickerMagnitude; }

    // for cards like Giant Growth, etc.
    public final int getTempPowerBoost() {
        return tempPowerBoost;
    }

    public final int getTempToughnessBoost() {
        return tempToughnessBoost;
    }

    public final void addTempPowerBoost(final int n) {
        if (n == 0) { return; }
        tempPowerBoost += n;
        currentState.getView().updatePower(this);
    }

    public final void addTempToughnessBoost(final int n) {
        if (n == 0) { return; }
        tempToughnessBoost += n;
        currentState.getView().updateToughness(this);
    }

    // for cards like Glorious Anthem, etc.
    public final int getSemiPermanentPowerBoost() {
        return semiPermanentPowerBoost;
    }

    public final int getSemiPermanentToughnessBoost() {
        return semiPermanentToughnessBoost;
    }

    public final void addSemiPermanentPowerBoost(final int n) {
        if (n == 0) { return; }
        semiPermanentPowerBoost += n;
        currentState.getView().updatePower(this);
    }

    public final void addSemiPermanentToughnessBoost(final int n) {
        if (n == 0) { return; }
        semiPermanentToughnessBoost += n;
        currentState.getView().updateToughness(this);
    }

    public final void setSemiPermanentPowerBoost(final int n) {
        if (semiPermanentPowerBoost == n) { return; }
        semiPermanentPowerBoost = n;
        currentState.getView().updatePower(this);
    }

    public final void setSemiPermanentToughnessBoost(final int n) {
        if (semiPermanentToughnessBoost == n) { return; }
        semiPermanentToughnessBoost = n;
        currentState.getView().updateToughness(this);
    }

    public final void updatePowerToughnessView() {
        currentState.getView().updatePower(this);
        currentState.getView().updateToughness(this);
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

    public final void tap() {
        if (tapped) { return; }

        // Run triggers
        final Map<String, Object> runParams = new TreeMap<>();
        runParams.put("Card", this);
        getGame().getTriggerHandler().runTrigger(TriggerType.Taps, runParams, false);

        setTapped(true);
        getGame().fireEvent(new GameEventCardTapped(this, true));
    }

    public final void untap() {
        if (!tapped) { return; }

        // Run Replacement effects
        final HashMap<String, Object> repRunParams = new HashMap<>();
        repRunParams.put("Event", "Untap");
        repRunParams.put("Affected", this);

        if (getGame().getReplacementHandler().run(repRunParams) != ReplacementResult.NotReplaced) {
            return;
        }

        // Run triggers
        final Map<String, Object> runParams = new TreeMap<>();
        runParams.put("Card", this);
        getGame().getTriggerHandler().runTrigger(TriggerType.Untaps, runParams, false);

        for (final GameCommand var : untapCommandList) {
            var.run();
        }
        setTapped(false);
        getGame().fireEvent(new GameEventCardTapped(this, false));
    }

    // keywords are like flying, fear, first strike, etc...
    public final List<String> getKeywords() {
        return getKeywords(currentState);
    }
    public final List<String> getKeywords(CardState state) {
        ListKeywordVisitor visitor = new ListKeywordVisitor();
        visitKeywords(state, visitor);
        return visitor.getKeywords();
    }
    // Allows traversing the card's keywords without needing to concat a bunch
    // of lists. Optimizes common operations such as hasKeyword().
    public final void visitKeywords(CardState state, Visitor<String> visitor) {
        visitUnhiddenKeywords(state, visitor);
        visitHiddenExtreinsicKeywords(visitor);
    }

    @Override
    public final boolean hasKeyword(String keyword) {
        return hasKeyword(keyword, currentState);
    }

    public final boolean hasKeyword(String keyword, CardState state) {
        if (keyword.startsWith("HIDDEN")) {
            keyword = keyword.substring(7);
        }

        CountKeywordVisitor visitor = new CountKeywordVisitor(keyword);
        visitKeywords(state, visitor);
        return visitor.getCount() > 0;
    }

    public final void updateKeywords() {
    	currentState.getView().updateKeywords(this, currentState);
    }

    public final void addChangedCardKeywords(final List<String> keywords, final List<String> removeKeywords,
            final boolean removeAllKeywords, final long timestamp) {
        keywords.removeAll(getCantHaveOrGainKeyword());
        // if the key already exists - merge entries
        final KeywordsChange cks = changedCardKeywords.get(timestamp);
        if (cks != null) {
        	cks.removeKeywords(this);
            List<String> kws = new ArrayList<>(keywords);
            List<String> rkws = new ArrayList<>(removeKeywords);
            boolean remAll = removeAllKeywords;
            kws.addAll(cks.getKeywords());
            rkws.addAll(cks.getRemoveKeywords());
            remAll |= cks.isRemoveAllKeywords();
            final KeywordsChange newCks = new KeywordsChange(kws, rkws, remAll);
            newCks.addKeywordsToCard(this);
            changedCardKeywords.put(timestamp, newCks);
        }
        else {
            final KeywordsChange newCks = new KeywordsChange(keywords, removeKeywords, removeAllKeywords);
            newCks.addKeywordsToCard(this);
            changedCardKeywords.put(timestamp, newCks);
        }
        updateKeywords();
    }

    public final void addChangedCardKeywords(final String[] keywords, final String[] removeKeywords,
            final boolean removeAllKeywords, final long timestamp) {
        List<String> keywordsList = new ArrayList<>();
        List<String> removeKeywordsList = new ArrayList<>();
        if (keywords != null) {
            keywordsList = new ArrayList<>(Arrays.asList(keywords));
        }

        if (removeKeywords != null) {
            removeKeywordsList = new ArrayList<>(Arrays.asList(removeKeywords));
        }

        addChangedCardKeywords(keywordsList, removeKeywordsList, removeAllKeywords, timestamp);
    }

    public final KeywordsChange removeChangedCardKeywords(final long timestamp) {
        KeywordsChange change = changedCardKeywords.remove(timestamp);
        if (change != null) {
            change.removeKeywords(this);
            updateKeywords();
        }
        return change;
    }

    // Hidden keywords will be left out
    public final List<String> getUnhiddenKeywords() {
        return getUnhiddenKeywords(currentState);
    }
    public final List<String> getUnhiddenKeywords(CardState state) {
        final List<String> keywords = new ArrayList<>();
        Iterables.addAll(keywords, state.getIntrinsicKeywords());
        keywords.addAll(extrinsicKeyword);

        // see if keyword changes are in effect
        for (final KeywordsChange ck : changedCardKeywords.values()) {
            if (ck.isRemoveAllKeywords()) {
                keywords.clear();
            }
            else if (ck.getRemoveKeywords() != null) {
                keywords.removeAll(ck.getRemoveKeywords());
            }

            if (ck.getKeywords() != null) {
                keywords.addAll(ck.getKeywords());
            }
        }
        return keywords;
    }
    private void visitUnhiddenKeywords(CardState state, Visitor<String> visitor) {
        if (changedCardKeywords.isEmpty()) {
            // Fast path that doesn't involve temp allocations.
            for (String kw : state.getIntrinsicKeywords()) {
                visitor.visit(kw);
            }
            for (String kw : extrinsicKeyword) {
                visitor.visit(kw);
            }
        } else {
            for (String kw : getUnhiddenKeywords()) {
                visitor.visit(kw);
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
    public final void addChangedTextColorWord(final String originalWord, final String newWord, final Long timestamp) {
        if (MagicColor.fromName(newWord) == 0) {
            throw new RuntimeException("Not a color: " + newWord);
        }
        changedTextColors.add(timestamp, StringUtils.capitalize(originalWord), StringUtils.capitalize(newWord));
        updateKeywordsChangedText(timestamp);
        updateChangedText();
    }

    public final void removeChangedTextColorWord(final Long timestamp) {
        changedTextColors.remove(timestamp);
        updateKeywordsOnRemoveChangedText(
                removeChangedCardKeywords(timestamp));
        updateChangedText();
    }

    /**
     * Replace all instances of one type in this card's text by another.
     * @param originalWord the original type word.
     * @param newWord the new type word.
     */
    public final void addChangedTextTypeWord(final String originalWord, final String newWord, final Long timestamp) {
        changedTextTypes.add(timestamp, originalWord, newWord);
        if (getType().hasSubtype(originalWord)) {
            addChangedCardTypes(CardType.parse(newWord), CardType.parse(originalWord), false, false, false, false, timestamp);
        }
        updateKeywordsChangedText(timestamp);
        updateChangedText();
    }

    public final void removeChangedTextTypeWord(final Long timestamp) {
        changedTextTypes.remove(timestamp);
        removeChangedCardTypes(timestamp);
        updateKeywordsOnRemoveChangedText(
                removeChangedCardKeywords(timestamp));
        updateChangedText();
    }

    private void updateKeywordsChangedText(final Long timestamp) {
        if (hasSVar("LockInKeywords")) {
            return;
        }

        final List<String> addKeywords = Lists.newArrayList();
        final List<String> removeKeywords = Lists.newArrayList(keywordsGrantedByTextChanges);

        for (final String kw : currentState.getIntrinsicKeywords()) {
            final String newKw = AbilityUtils.applyKeywordTextChangeEffects(kw, this);
            if (!newKw.equals(kw)) {
                addKeywords.add(newKw);
                removeKeywords.add(kw);
                keywordsGrantedByTextChanges.add(newKw);
            }
        }
        addChangedCardKeywords(addKeywords, removeKeywords, false, timestamp);
    }

    private void updateKeywordsOnRemoveChangedText(final KeywordsChange k) {
        if (k != null) {
            keywordsGrantedByTextChanges.removeAll(k.getKeywords());
        }
    }

    /**
     * Update the changed text of the intrinsic spell abilities and keywords.
     */
    private void updateChangedText() {
        resetChangedSVars();
        final List<CardTraitBase> allAbs = ImmutableList.<CardTraitBase>builder()
            .addAll(getSpellAbilities())
            .addAll(getStaticAbilities())
            .addAll(getReplacementEffects())
            .addAll(getTriggers())
            .build();
        for (final CardTraitBase ctb : allAbs) {
            if (ctb.isIntrinsic()) {
                ctb.changeText();
            }
        }
        text = AbilityUtils.applyDescriptionTextChangeEffects(originalText, this);

        currentState.getView().updateAbilityText(this, currentState);
        view.updateNonAbilityText(this);
    }

    public final ImmutableMap<String, String> getChangedTextColorWords() {
        return ImmutableMap.copyOf(changedTextColors.toMap());
    }

    public final ImmutableMap<String, String> getChangedTextTypeWords() {
        return ImmutableMap.copyOf(changedTextTypes.toMap());
    }

    /**
     * Copy the color and type text changes from another {@link Card} to this
     * one. The original changes of this Card are removed.
     */
    public final void copyChangedTextFrom(final Card other) {
        changedTextColors.copyFrom(other.changedTextColors);
        changedTextTypes.copyFrom(other.changedTextTypes);
    }

    /**
     * Change a SVar due to a text change effect. Change is volatile and will be
     * reverted upon refreshing text changes (unless it is changed again at that
     * time).
     *
     * @param key the SVar name.
     * @param value the new SVar value.
     */
    public final void changeSVar(final String key, final String value) {
        originalSVars.put(key, getSVar(key));
        setSVar(key, value);
    }

    private void resetChangedSVars() {
        for (final Entry<String, String> svar : originalSVars.entrySet()) {
            setSVar(svar.getKey(), svar.getValue());
        }
        originalSVars.clear();
    }

    public final void setIntrinsicAbilities(final List<String> a) {
        currentState.setUnparsedAbilities(new ArrayList<>(a));
    }

    public final void addIntrinsicKeyword(final String s) {
        if (currentState.addIntrinsicKeyword(s)) {
            currentState.getView().updateKeywords(this, currentState);
        }
    }

    public final void addIntrinsicAbility(final String s) {
        currentState.addIntrinsicAbility(s);
    }

    public final void removeIntrinsicKeyword(final String s) {
        if (currentState.removeIntrinsicKeyword(s)) {
            currentState.getView().updateKeywords(this, currentState);
        }
    }

    public List<String> getExtrinsicKeyword() {
        return extrinsicKeyword;
    }
    public final void setExtrinsicKeyword(final List<String> a) {
        extrinsicKeyword = new ArrayList<>(a);
    }

    public void addExtrinsicKeyword(final String s) {
        if (s.startsWith("HIDDEN")) {
            addHiddenExtrinsicKeyword(s);
        }
        else {
            extrinsicKeyword.add(s);
        }
    }

    public void removeExtrinsicKeyword(final String s) {
        if (s.startsWith("HIDDEN")) {
            removeHiddenExtrinsicKeyword(s);
        }
        else {
            if (extrinsicKeyword.remove(s)) {
                currentState.getView().updateKeywords(this, currentState);
            }
        }
    }

    public void removeAllExtrinsicKeyword(final String s) {
        final List<String> strings = new ArrayList<>();
        strings.add(s);
        boolean needKeywordUpdate = false;
        if (extrinsicKeyword.removeAll(strings)) {
            needKeywordUpdate = true;
        }
        strings.add("HIDDEN " + s);
        if (hiddenExtrinsicKeyword.removeAll(strings)) {
            view.updateNonAbilityText(this);
            needKeywordUpdate = true;
        }
        if (needKeywordUpdate) {
            currentState.getView().updateKeywords(this, currentState);
        }
    }

    // Hidden Keywords will be returned without the indicator HIDDEN
    public final List<String> getHiddenExtrinsicKeywords() {
        ListKeywordVisitor visitor = new ListKeywordVisitor();
        visitHiddenExtreinsicKeywords(visitor);
        return visitor.getKeywords();
    }
    private void visitHiddenExtreinsicKeywords(Visitor<String> visitor) {
        for (String keyword : hiddenExtrinsicKeyword) {
            if (keyword == null) {
                continue;
            }
            if (keyword.startsWith("HIDDEN")) {
                keyword = keyword.substring(7);
            }
            visitor.visit(keyword);
        }
    }

    public final void addHiddenExtrinsicKeyword(final String s) {
        if (hiddenExtrinsicKeyword.add(s)) {
            view.updateNonAbilityText(this);
            currentState.getView().updateKeywords(this, currentState);
        }
    }

    public final void removeHiddenExtrinsicKeyword(final String s) {
        if (hiddenExtrinsicKeyword.remove(s)) {
            view.updateNonAbilityText(this);
            currentState.getView().updateKeywords(this, currentState);
        }
    }

    public final List<String> getCantHaveOrGainKeyword() {
        final List<String> cantGain = new ArrayList<>();
        for (String s : hiddenExtrinsicKeyword) {
            if (s.contains("can't have or gain")) {
                cantGain.add(s.split("can't have or gain ")[1]);
            }
        }
        return cantGain;
    }

    public final void setStaticAbilityStrings(final List<String> a) {
        currentState.setStaticAbilityStrings(new ArrayList<>(a));
    }

    public final Iterable<String> getStaticAbilityStrings() {
        return currentState.getStaticAbilityStrings();
    }
    public final void setStaticAbilities(final List<StaticAbility> a) {
        currentState.setStaticAbilities(new ArrayList<>(a));
    }
    public final void addStaticAbilityString(final String s) {
        currentState.addStaticAbilityString(s);
    }

    public final FCollectionView<StaticAbility> getStaticAbilities() {
        return currentState.getStaticAbilities();
    }
    public final StaticAbility addStaticAbility(final String s) {
        if (!s.trim().isEmpty()) {
            final StaticAbility stAb = new StaticAbility(s, this);
            currentState.addStaticAbility(stAb);
            return stAb;
        }
        return null;
    }
    public final void removeStaticAbility(StaticAbility stAb) {
        currentState.removeStaticAbility(stAb);
    }

    public final boolean isPermanent() {
        return !isImmutable && getType().isPermanent();
    }

    public final boolean isSpell() {
        return (isInstant() || isSorcery() || (isAura() && !isInZone((ZoneType.Battlefield))));
    }

    public final boolean isEmblem()     { return getType().isEmblem(); }

    public final boolean isLand()       { return getType().isLand(); }
    public final boolean isBasicLand()  { return getType().isBasicLand(); }
    public final boolean isSnow()       { return getType().isSnow(); }

    public final boolean isTribal()     { return getType().isTribal(); }
    public final boolean isSorcery()    { return getType().isSorcery(); }
    public final boolean isInstant()    { return getType().isInstant(); }

    public final boolean isCreature()   { return getType().isCreature(); }
    public final boolean isArtifact()   { return getType().isArtifact(); }
    public final boolean isEquipment()  { return getType().hasSubtype("Equipment"); }
    public final boolean isFortification()  { return getType().hasSubtype("Fortification"); }
    public final boolean isPlaneswalker()   { return getType().isPlaneswalker(); }
    public final boolean isEnchantment()    { return getType().isEnchantment(); }
    public final boolean isAura()           { return getType().hasSubtype("Aura"); }

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

    public final Cost getMiracleCost() {
        return miracleCost;
    }
    public final void setMiracleCost(final Cost cost) {
        miracleCost = cost;
    }

    public final boolean hasSuspend() {
        return suspend;
    }
    public final void setSuspend(final boolean b) {
        suspend = b;
    }

    public final boolean wasSuspendCast() {
        return suspendCast;
    }
    public final void setSuspendCast(final boolean b) {
        suspendCast = b;
    }

    public final boolean isPhasedOut() {
        return phasedOut;
    }
    public final void setPhasedOut(final boolean phasedOut0) {
        if (phasedOut == phasedOut0) { return; }
        phasedOut = phasedOut0;
        view.updatePhasedOut(this);
    }

    public final void phase() {
        phase(true);
    }
    public final void phase(final boolean direct) {
        final boolean phasingIn = isPhasedOut();

        if (!switchPhaseState()) {
            // Switch Phase State bails early if the Permanent can't Phase Out
            return;
        }

        if (!phasingIn) {
            setDirectlyPhasedOut(direct);
        }

        if (isEquipped()) {
            for (final Card eq : getEquippedBy(false)) {
                if (eq.isPhasedOut() == phasingIn) {
                    eq.phase(false);
                }
            }
        }
        if (isFortified()) {
            for (final Card f : getFortifiedBy(false)) {
                if (f.isPhasedOut() == phasingIn) {
                    f.phase(false);
                }
            }
        }
        if (isEnchanted()) {
            for (final Card aura : getEnchantedBy(false)) {
                if (aura.isPhasedOut() == phasingIn) {
                    aura.phase(false);
                }
            }
        }

        getGame().fireEvent(new GameEventCardPhased(this, isPhasedOut()));
    }

    private boolean switchPhaseState() {
        if (!phasedOut && hasKeyword("CARDNAME can't phase out.")) {
            return false;
        }

        final Map<String, Object> runParams = new TreeMap<>();
        runParams.put("Card", this);

        if (!isPhasedOut()) {
            // If this is currently PhasedIn, it's about to phase out.
            // Run trigger before it does because triggers don't work with phased out objects
            getGame().getTriggerHandler().runTrigger(TriggerType.PhaseOut, runParams, false);
        }

        setPhasedOut(!phasedOut);
        final Combat combat = getGame().getPhaseHandler().getCombat();
        if (combat != null && phasedOut) {
            combat.removeFromCombat(this);
        }
        if (phasedOut && isToken()) {
            // 702.23k Phased-out tokens cease to exist as a state-based action.
            // See rule 704.5d.
            // 702.23d The phasing event doesn't actually cause a permanent to
            // change zones or control,
            // even though it's treated as though it's not on the battlefield
            // and not under its controller's control while it's phased out.
            // Zone-change triggers don't trigger when a permanent phases in or
            // out.

            // Just remove it's zone, so we don't run through the exile stuff
            // This allows auras on phased out tokens to just phase out permanently
            this.ceaseToExist();
        }

        if (!phasedOut) {
            // Just phased in, time to run the phased in trigger
            getGame().getTriggerHandler().registerActiveTrigger(this, false);
            getGame().getTriggerHandler().runTrigger(TriggerType.PhaseIn, runParams, false);
        }

        return true;
    }

    public final boolean isDirectlyPhasedOut() {
        return directlyPhasedOut;
    }
    public final void setDirectlyPhasedOut(final boolean direct) {
        directlyPhasedOut = direct;
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
        CountKeywordVisitor visitor = new CountKeywordVisitor(keyword, true);
        visitKeywords(state, visitor);
        return visitor.getCount() > 0;
    }

    public final boolean hasStartOfUnHiddenKeyword(String keyword) {
        return hasStartOfUnHiddenKeyword(keyword, currentState);
    }
    public final boolean hasStartOfUnHiddenKeyword(String keyword, CardState state) {
        CountKeywordVisitor visitor = new CountKeywordVisitor(keyword, true);
        visitUnhiddenKeywords(state, visitor);
        return visitor.getCount() > 0;
    }

    public final int getKeywordPosition(String k) {
        return getKeywordPosition(k, currentState);
    }
    public final int getKeywordPosition(String k, CardState state) {
        final List<String> a = getKeywords(state);
        for (int i = 0; i < a.size(); i++) {
            if (a.get(i).startsWith(k)) {
                return i;
            }
        }
        return -1;
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
        CountKeywordVisitor visitor = new CountKeywordVisitor(k);
        visitKeywords(state, visitor);
        return visitor.getCount();
    }

    // This is for keywords with a number like Bushido, Annihilator and Rampage.
    // It returns the total.
    public final int getKeywordMagnitude(final String k) {
        return getKeywordMagnitude(k, currentState);
    }
    public final int getKeywordMagnitude(final String k, CardState state) {
        int count = 0;
        for (final String kw : getKeywords(state)) {
            if (kw.startsWith(k)) {
                final String[] parse = kw.split(" ");
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
        }
        return count;
    }

    // Takes one argument like Permanent.Blue+withFlying
    @Override
    public final boolean isValid(final String restriction, final Player sourceController, final Card source, SpellAbility spellAbility) {
        if (isImmutable() && !source.isRemembered(this)) { // special case exclusion
            return false;
        }

        // Inclusive restrictions are Card types
        final String[] incR = restriction.split("\\.", 2);

        boolean testFailed = false;
        if (incR[0].startsWith("!")) {
            testFailed = true; // a bit counter logical))
            incR[0] = incR[0].substring(1); // consume negation sign
        }

        if (incR[0].equals("Spell") && !isSpell()) {
            return testFailed;
        }
        if (incR[0].equals("Permanent") && (isInstant() || isSorcery())) {
            return testFailed;
        }
        if (!incR[0].equals("card") && !incR[0].equals("Card") && !incR[0].equals("Spell")
                && !incR[0].equals("Permanent") && !getType().hasStringType(incR[0])) {
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
    public boolean hasProperty(final String property, final Player sourceController, final Card source, SpellAbility spellAbility) {
        final Game game = getGame();
        final Combat combat = game.getCombat();
        final Card lki = getGame().getChangeZoneLKIInfo(this);
        final Player controller = lki != null ? lki.getController() :  getController();

        // by name can also have color names, so needs to happen before colors.
        if (property.startsWith("named")) {
            if (!getName().equals(property.substring(5))) {
                return false;
            }
        } else if (property.startsWith("notnamed")) {
            if (getName().equals(property.substring(8))) {
                return false;
            }
        } else if (property.startsWith("sameName")) {
            if (getName().equals("") || !sharesNameWith(source)) {
                return false;
            }
        } else if (property.equals("NamedCard")) {
            if (!getName().equals(source.getNamedCard())) {
                return false;
            }
        } else if (property.equals("NamedByRememberedPlayer")) {
            if (!source.hasRemembered()) {
                final Card newCard = game.getCardState(source);
                for (final Object o : newCard.getRemembered()) {
                    if (o instanceof Player) {
                        if (!getName().equals(((Player) o).getNamedCard())) {
                            return false;
                        }
                    }
                }
            }
            for (final Object o : source.getRemembered()) {
                if (o instanceof Player) {
                    if (!getName().equals(((Player) o).getNamedCard())) {
                        return false;
                    }
                }
            }
        } else if (property.equals("Permanent")) {
            if (isInstant() || isSorcery()) {
                return false;
            }
        } else if (property.startsWith("CardUID_")) {// Protection with "doesn't remove effect"
            if (id != Integer.parseInt(property.split("CardUID_")[1])) {
                return false;
            }
        } else if (property.equals("ChosenCard")) {
            if (!source.hasChosenCard(this)) {
                return false;
            }
        } else if (property.equals("nonChosenCard")) {
            if (source.hasChosenCard(this)) {
                return false;
            }
        }
        // ... Card colors
        else if (property.contains("White") || property.contains("Blue") || property.contains("Black")
                || property.contains("Red") || property.contains("Green")) {
            boolean mustHave = !property.startsWith("non");
            boolean withSource = property.endsWith("Source");
            if (withSource && hasKeyword("Colorless Damage Source")) {
                return false;
            }

            final String colorName = property.substring(mustHave ? 0 : 3, property.length() - (withSource ? 6 : 0));

            int desiredColor = MagicColor.fromName(colorName);
            boolean hasColor = CardUtil.getColors(this).hasAnyColor(desiredColor);
            if (mustHave != hasColor)
                return false;

        } else if (property.contains("Colorless")) { // ... Card is colorless
            boolean non = property.startsWith("non");
            boolean withSource = property.endsWith("Source");
            if (non && withSource && hasKeyword("Colorless Damage Source")) {
                return false;
            }
            if (non == CardUtil.getColors(this).isColorless()) return false;

        } else if (property.contains("MultiColor")) { // ... Card is multicolored
            if (property.endsWith("Source") && hasKeyword("Colorless Damage Source")) return false;
            if (property.startsWith("non") == CardUtil.getColors(this).isMulticolor()) return false;

        } else if (property.contains("MonoColor")) { // ... Card is monocolored
            if (property.endsWith("Source") && hasKeyword("Colorless Damage Source")) return false;
            if (property.startsWith("non") == CardUtil.getColors(this).isMonoColor()) return false;

        } else if (property.contains("ChosenColor")) {
            if (property.endsWith("Source") && hasKeyword("Colorless Damage Source")) return false;
            if (!source.hasChosenColor() || !CardUtil.getColors(this).hasAnyColor(MagicColor.fromName(source.getChosenColor())))
                return false;

        } else if (property.contains("AnyChosenColor")) {
            if (property.endsWith("Source") && hasKeyword("Colorless Damage Source")) return false;
            if (!source.hasChosenColor() || !CardUtil.getColors(this).hasAnyColor(ColorSet.fromNames(source.getChosenColors()).getColor()))
                return false;

        } else if (property.equals("DoubleFaced")) {
            if (!isDoubleFaced()) {
                return false;
            }
        } else if (property.equals("Flip")) {
            if (!isFlipCard()) {
                return false;
            }
        } else if (property.startsWith("YouCtrl")) {
            if (!controller.equals(sourceController)) {
                return false;
            }
        } else if (property.startsWith("YouDontCtrl")) {
            if (controller.equals(sourceController)) {
                return false;
            }
        } else if (property.startsWith("OppCtrl")) {
            if (!controller.getOpponents().contains(sourceController)) {
                return false;
            }
        } else if (property.startsWith("ChosenCtrl")) {
            if (!controller.equals(source.getChosenPlayer())) {
                return false;
            }
        } else if (property.startsWith("DefenderCtrl")) {
            if (!game.getPhaseHandler().inCombat()) {
                return false;
            }
            if (property.endsWith("ForRemembered")) {
                if (!source.hasRemembered()) {
                    return false;
                }
                if (getGame().getCombat().getDefendingPlayerRelatedTo((Card) source.getFirstRemembered()) != controller) {
                    return false;
                }
            } else {
                if (getGame().getCombat().getDefendingPlayerRelatedTo(source) != controller) {
                    return false;
                }
            }
        } else if (property.startsWith("DefendingPlayerCtrl")) {
            if (!game.getPhaseHandler().inCombat()) {
                return false;
            }
            if (!getGame().getCombat().isPlayerAttacked(controller)) {
                return false;
            }
        } else if (property.startsWith("EnchantedPlayerCtrl")) {
            final Object o = source.getEnchanting();
            if (o instanceof Player) {
                if (!controller.equals(o)) {
                    return false;
                }
            } else { // source not enchanting a player
                return false;
            }
        } else if (property.startsWith("EnchantedControllerCtrl")) {
            final Object o = source.getEnchanting();
            if (o instanceof Card) {
                if (!controller.equals(((Card) o).getController())) {
                    return false;
                }
            } else { // source not enchanting a card
                return false;
            }
        } else if (property.startsWith("RememberedPlayer")) {
            Player p = property.endsWith("Ctrl") ? controller : getOwner();
            if (!source.hasRemembered()) {
                final Card newCard = game.getCardState(source);
                for (final Object o : newCard.getRemembered()) {
                    if (o instanceof Player) {
                        if (!p.equals(o)) {
                            return false;
                        }
                    }
                }
            }

            for (final Object o : source.getRemembered()) {
                if (o instanceof Player) {
                    if (!p.equals(o)) {
                        return false;
                    }
                }
            }
        } else if (property.startsWith("nonRememberedPlayerCtrl")) {
            if (!source.hasRemembered()) {
                final Card newCard = game.getCardState(source);
                if (newCard.isRemembered(controller)) {
                    return false;
                }
            }

            if (source.isRemembered(controller)) {
                return false;
            }
        } else if (property.equals("TargetedPlayerCtrl")) {
            for (final SpellAbility sa : source.currentState.getNonManaAbilities()) {
                final SpellAbility saTargeting = sa.getSATargetingPlayer();
                if (saTargeting != null) {
                    for (final Player p : saTargeting.getTargets().getTargetPlayers()) {
                        if (!controller.equals(p)) {
                            return false;
                        }
                    }
                }
            }
        } else if (property.equals("TargetedControllerCtrl")) {
            for (final SpellAbility sa : source.currentState.getNonManaAbilities()) {
                final CardCollectionView cards = AbilityUtils.getDefinedCards(source, "Targeted", sa);
                final List<SpellAbility> sas = AbilityUtils.getDefinedSpellAbilities(source, "Targeted", sa);
                for (final Card c : cards) {
                    final Player p = c.getController();
                    if (!controller.equals(p)) {
                        return false;
                    }
                }
                for (final SpellAbility s : sas) {
                    final Player p = s.getHostCard().getController();
                    if (!controller.equals(p)) {
                        return false;
                    }
                }
            }
        } else if (property.startsWith("ActivePlayerCtrl")) {
            if (!game.getPhaseHandler().isPlayerTurn(controller)) {
                return false;
            }
        } else if (property.startsWith("NonActivePlayerCtrl")) {
            if (game.getPhaseHandler().isPlayerTurn(controller)) {
                return false;
            }
        } else if (property.startsWith("YouOwn")) {
            if (!getOwner().equals(sourceController)) {
                return false;
            }
        } else if (property.startsWith("YouDontOwn")) {
            if (getOwner().equals(sourceController)) {
                return false;
            }
        } else if (property.startsWith("OppOwn")) {
            if (!getOwner().getOpponents().contains(sourceController)) {
                return false;
            }
        } else if (property.equals("TargetedPlayerOwn")) {
            for (final SpellAbility sa : source.currentState.getNonManaAbilities()) {
                final SpellAbility saTargeting = sa.getSATargetingPlayer();
                if (saTargeting != null) {
                    for (final Player p : saTargeting.getTargets().getTargetPlayers()) {
                        if (!getOwner().equals(p)) {
                            return false;
                        }
                    }
                }
            }
        } else if (property.startsWith("OwnedBy")) {
            final String valid = property.substring(8);
            if (!getOwner().isValid(valid, sourceController, source, spellAbility)) {
                return false;
            }
        } else if (property.startsWith("ControlledBy")) {
            final String valid = property.substring(13);
            if (!controller.isValid(valid, sourceController, source, spellAbility)) {
                return false;
            }
        } else if (property.startsWith("OwnerDoesntControl")) {
            if (getOwner().equals(controller)) {
                return false;
            }
        } else if (property.startsWith("ControllerControls")) {
            final String type = property.substring(18);
            if (type.startsWith("AtLeastAsMany")) {
                String realType = type.split("AtLeastAsMany")[1];
                CardCollectionView cards = CardLists.getType(controller.getCardsIn(ZoneType.Battlefield), realType);
                CardCollectionView yours = CardLists.getType(sourceController.getCardsIn(ZoneType.Battlefield), realType);
                if (cards.size() < yours.size()) {
                    return false;
                }
            } else {
                final CardCollectionView cards = controller.getCardsIn(ZoneType.Battlefield);
                if (CardLists.getType(cards, type).isEmpty()) {
                    return false;
                }
            }
        } else if (property.startsWith("Other")) {
            if (equals(source)) {
                return false;
            }
        } else if (property.startsWith("StrictlySelf")) {
            if (!equals(source) || getTimestamp() != source.getTimestamp()) {
                return false;
            }
        } else if (property.startsWith("Self")) {
            if (!equals(source)) {
                return false;
            }
        } else if (property.startsWith("ExiledWithSource")) {
            if (getExiledWith() == null) {
                return false;
            }

            Card host = source;
            //Static Abilites doesn't have spellAbility or OriginalHost
            if (spellAbility != null) {
                host = spellAbility.getOriginalHost();
                if (host == null) {
                    host = spellAbility.getHostCard();
                }
            }

            if (!getExiledWith().equals(host)) {
                return false;
            }
        } else if (property.startsWith("AttachedBy")) {
            if (!isEquippedBy(source) && !isEnchantedBy(source) && !isFortifiedBy(source)) {
                return false;
            }
        } else if (property.equals("Attached")) {
            if (equipping != source && !source.equals(enchanting) && fortifying != source) {
                return false;
            }
        } else if (property.startsWith("AttachedTo")) {
            final String restriction = property.split("AttachedTo ")[1];
            if (restriction.equals("Targeted")) {
                if (!source.currentState.getTriggers().isEmpty()) {
                    for (final Trigger t : source.currentState.getTriggers()) {
                        final SpellAbility sa = t.getTriggeredSA();
                        final CardCollectionView cards = AbilityUtils.getDefinedCards(source, "Targeted", sa);
                        for (final Card c : cards) {
                            if (equipping != c && !c.equals(enchanting)) {
                                return false;
                            }
                        }
                    }
                } else {
                    for (final SpellAbility sa : source.currentState.getNonManaAbilities()) {
                        final CardCollectionView cards = AbilityUtils.getDefinedCards(source, "Targeted", sa);
                        for (final Card c : cards) {
                            if (equipping == c || c.equals(enchanting)) { // handle multiple targets
                                return true;
                            }
                        }
                    }
                    return false;
                }
            } else {
                if ((enchanting == null || !enchanting.isValid(restriction, sourceController, source, spellAbility))
                        && (equipping == null || !equipping.isValid(restriction, sourceController, source, spellAbility))
                        && (fortifying == null || !fortifying.isValid(restriction, sourceController, source, spellAbility))) {
                    return false;
                }
            }
        } else if (property.equals("NameNotEnchantingEnchantedPlayer")) {
            Player enchantedPlayer = source.getEnchantingPlayer();
            if (enchantedPlayer == null || enchantedPlayer.isEnchantedBy(getName())) {
                return false;
            }
        } else if (property.equals("NotAttachedTo")) {
            if (equipping == source || source.equals(enchanting) || fortifying == source) {
                return false;
            }
        } else if (property.startsWith("EnchantedBy")) {
            if (property.equals("EnchantedBy")) {
                if (!isEnchantedBy(source) && !equals(source.getEnchanting())) {
                    return false;
                }
            } else {
                final String restriction = property.split("EnchantedBy ")[1];
                switch (restriction) {
                    case "Imprinted":
                        for (final Card card : source.getImprintedCards()) {
                            if (!isEnchantedBy(card) && !equals(card.getEnchanting())) {
                                return false;
                            }
                        }
                        break;
                    case "Targeted":
                        for (final SpellAbility sa : source.currentState.getNonManaAbilities()) {
                            final SpellAbility saTargeting = sa.getSATargetingCard();
                            if (saTargeting != null) {
                                for (final Card c : saTargeting.getTargets().getTargetCards()) {
                                    if (!isEnchantedBy(c) && !equals(c.getEnchanting())) {
                                        return false;
                                    }
                                }
                            }
                        }
                        break;
                    default:  // EnchantedBy Aura.Other
                        for (final Card aura : getEnchantedBy(false)) {
                            if (aura.isValid(restriction, sourceController, source, spellAbility)) {
                                return true;
                            }
                        }
                        return false;
                }
            }
        } else if (property.startsWith("NotEnchantedBy")) {
            if (property.substring(14).equals("Targeted")) {
                for (final SpellAbility sa : source.currentState.getNonManaAbilities()) {
                    final SpellAbility saTargeting = sa.getSATargetingCard();
                    if (saTargeting != null) {
                        for (final Card c : saTargeting.getTargets().getTargetCards()) {
                            if (isEnchantedBy(c)) {
                                return false;
                            }
                        }
                    }
                }
            } else {
                if (isEnchantedBy(source)) {
                    return false;
                }
            }
        } else if (property.startsWith("Enchanted")) {
            if (!source.equals(enchanting)) {
                return false;
            }
        } else if (property.startsWith("CanEnchant")) {
            final String restriction = property.substring(10);
            if (restriction.equals("Remembered")) {
                for (final Object rem : source.getRemembered()) {
                    if (!(rem instanceof Card) || !((Card) rem).canBeEnchantedBy(this))
                        return false;
                }
            } else if (restriction.equals("Source")) {
                if (!source.canBeEnchantedBy(this)) return false;
            }
        } else if (property.startsWith("CanBeEnchantedBy")) {
            if (property.substring(16).equals("Targeted")) {
                for (final SpellAbility sa : source.currentState.getNonManaAbilities()) {
                    final SpellAbility saTargeting = sa.getSATargetingCard();
                    if (saTargeting != null) {
                        for (final Card c : saTargeting.getTargets().getTargetCards()) {
                            if (!canBeEnchantedBy(c)) {
                                return false;
                            }
                        }
                    }
                }
            } else if (property.substring(16).equals("AllRemembered")) {
                for (final Object rem : source.getRemembered()) {
                    if (rem instanceof Card) {
                        final Card card = (Card) rem;
                        if (!canBeEnchantedBy(card)) {
                            return false;
                        }
                    }
                }
            } else {
                if (!canBeEnchantedBy(source)) {
                    return false;
                }
            }
        } else if (property.startsWith("EquippedBy")) {
            if (property.substring(10).equals("Targeted")) {
                for (final SpellAbility sa : source.currentState.getNonManaAbilities()) {
                    final SpellAbility saTargeting = sa.getSATargetingCard();
                    if (saTargeting != null) {
                        for (final Card c : saTargeting.getTargets().getTargetCards()) {
                            if (!isEquippedBy(c)) {
                                return false;
                            }
                        }
                    }
                }
            } else if (property.substring(10).equals("Enchanted")) {
                if (source.getEnchantingCard() == null ||
                        !isEquippedBy(source.getEnchantingCard())) {
                    return false;
                }
            } else {
                if (!isEquippedBy(source)) {
                    return false;
                }
            }
        } else if (property.startsWith("FortifiedBy")) {
            if (!isFortifiedBy(source)) {
                return false;
            }
        } else if (property.startsWith("CanBeEquippedBy")) {
            if (!canBeEquippedBy(source)) {
                return false;
            }
        } else if (property.startsWith("Equipped")) {
            if (equipping != source) {
                return false;
            }
        } else if (property.startsWith("Fortified")) {
            if (fortifying != source) {
                return false;
            }
        } else if (property.startsWith("HauntedBy")) {
            if (!isHauntedBy(source)) {
                return false;
            }
        } else if (property.startsWith("notTributed")) {
            if (tributed) {
                return false;
            }
        } else if (property.startsWith("madness")) {
            if (!madness) {
                return false;
            }
        } else if (property.contains("Paired")) {
            if (property.contains("With")) { // PairedWith
                if (!isPaired() || pairedWith != source) {
                    return false;
                }
            } else if (property.startsWith("Not")) {  // NotPaired
                if (isPaired()) {
                    return false;
                }
            } else { // Paired
                if (!isPaired()) {
                    return false;
                }
            }
        } else if (property.startsWith("Above")) { // "Are Above" Source
            final CardCollectionView cards = getOwner().getCardsIn(ZoneType.Graveyard);
            if (cards.indexOf(source) >= cards.indexOf(this)) {
                return false;
            }
        } else if (property.startsWith("DirectlyAbove")) { // "Are Directly Above" Source
            final CardCollectionView cards = getOwner().getCardsIn(ZoneType.Graveyard);
            if (cards.indexOf(this) - cards.indexOf(source) != 1) {
                return false;
            }
        } else if (property.startsWith("TopGraveyardCreature")) {
            CardCollection cards = CardLists.filter(getOwner().getCardsIn(ZoneType.Graveyard), CardPredicates.Presets.CREATURES);
            Collections.reverse(cards);
            if (cards.isEmpty() || !equals(cards.get(0))) {
                return false;
            }
        } else if (property.startsWith("TopGraveyard")) {
            final CardCollection cards = new CardCollection(getOwner().getCardsIn(ZoneType.Graveyard));
            Collections.reverse(cards);
            if (property.substring(12).matches("[0-9][0-9]?")) {
                int n = Integer.parseInt(property.substring(12));
                int num = Math.min(n, cards.size());
                final CardCollection newlist = new CardCollection();
                for (int i = 0; i < num; i++) {
                    newlist.add(cards.get(i));
                }
                if (cards.isEmpty() || !newlist.contains(this)) {
                    return false;
                }
            } else {
                if (cards.isEmpty() || !equals(cards.get(0))) {
                    return false;
                }
            }
        } else if (property.startsWith("BottomGraveyard")) {
            final CardCollectionView cards = getOwner().getCardsIn(ZoneType.Graveyard);
            if (cards.isEmpty() || !equals(cards.get(0))) {
                return false;
            }
        } else if (property.startsWith("TopLibrary")) {
            final CardCollectionView cards = getOwner().getCardsIn(ZoneType.Library);
            if (cards.isEmpty() || !equals(cards.get(0))) {
                return false;
            }
        } else if (property.startsWith("Cloned")) {
            if ((cloneOrigin == null) || !cloneOrigin.equals(source)) {
                return false;
            }
        } else if (property.startsWith("DamagedBy")) {
            if ((property.endsWith("Source") || property.equals("DamagedBy")) &&
                    !receivedDamageFromThisTurn.containsKey(source)) {
                return false;
            } else if (property.endsWith("Remembered")) {
                boolean matched = false;
                for (final Object obj : source.getRemembered()) {
                    if (!(obj instanceof Card)) {
                        continue;
                    }
                    matched |= receivedDamageFromThisTurn.containsKey(obj);
                }
                if (!matched)
                    return false;
            } else if (property.endsWith("Equipped")) {
                final Card equipee = source.getEquipping();
                if (equipee == null || !receivedDamageFromThisTurn.containsKey(equipee))
                    return false;
            } else if (property.endsWith("Enchanted")) {
                final Card equipee = source.getEnchantingCard();
                if (equipee == null || !receivedDamageFromThisTurn.containsKey(equipee))
                    return false;
            }
        } else if (property.startsWith("Damaged")) {
            if (!dealtDamageToThisTurn.containsKey(source)) {
                return false;
            }
        } else if (property.startsWith("IsTargetingSource")) {
            for (final SpellAbility sa : currentState.getNonManaAbilities()) {
                final SpellAbility saTargeting = sa.getSATargetingCard();
                if (saTargeting != null) {
                    for (final Card c : saTargeting.getTargets().getTargetCards()) {
                        if (c.equals(source)) {
                            return true;
                        }
                    }
                }
            }
            return false;
        } else if (property.startsWith("SharesCMCWith")) {
            if (property.equals("SharesCMCWith")) {
                if (!sharesCMCWith(source)) {
                    return false;
                }
            } else {
                final String restriction = property.split("SharesCMCWith ")[1];
                CardCollection list = AbilityUtils.getDefinedCards(source, restriction, spellAbility);
                return !CardLists.filter(list, CardPredicates.sharesCMCWith(this)).isEmpty();
            }
        } else if (property.startsWith("SharesColorWith")) {
            if (property.equals("SharesColorWith")) {
                if (!sharesColorWith(source)) {
                    return false;
                }
            } else {
                final String restriction = property.split("SharesColorWith ")[1];
                if (restriction.startsWith("Remembered") || restriction.startsWith("Imprinted")) {
                    CardCollection list = AbilityUtils.getDefinedCards(source, restriction, spellAbility);
                    return !CardLists.filter(list, CardPredicates.sharesColorWith(this)).isEmpty();
                }

                switch (restriction) {
                    case "TopCardOfLibrary":
                        final CardCollectionView cards = sourceController.getCardsIn(ZoneType.Library);
                        if (cards.isEmpty() || !sharesColorWith(cards.get(0))) {
                            return false;
                        }
                        break;
                    case "Equipped":
                        if (!source.isEquipment() || !source.isEquipping()
                                || !sharesColorWith(source.getEquipping())) {
                            return false;
                        }
                        break;
                    case "MostProminentColor":
                        byte mask = CardFactoryUtil.getMostProminentColors(game.getCardsIn(ZoneType.Battlefield));
                        if (!CardUtil.getColors(this).hasAnyColor(mask))
                            return false;
                        break;
                    case "LastCastThisTurn":
                        final List<Card> c = game.getStack().getSpellsCastThisTurn();
                        if (c.isEmpty() || !sharesColorWith(c.get(c.size() - 1))) {
                            return false;
                        }
                        break;
                    case "ActivationColor":
                        byte manaSpent = source.getColorsPaid();
                        if (!CardUtil.getColors(this).hasAnyColor(manaSpent)) {
                            return false;
                        }
                        break;
                    default:
                        for (final Card card : sourceController.getCardsIn(ZoneType.Battlefield)) {
                            if (card.isValid(restriction, sourceController, source, spellAbility) && sharesColorWith(card)) {
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

            byte mostProm = CardFactoryUtil.getMostProminentColors(game.getCardsIn(ZoneType.Battlefield));
            return ColorSet.fromMask(mostProm).hasAnyColor(MagicColor.fromName(color));
        } else if (property.startsWith("notSharesColorWith")) {
            if (property.equals("notSharesColorWith")) {
                if (sharesColorWith(source)) {
                    return false;
                }
            } else {
                final String restriction = property.split("notSharesColorWith ")[1];
                for (final Card card : sourceController.getCardsIn(ZoneType.Battlefield)) {
                    if (card.isValid(restriction, sourceController, source, spellAbility) && sharesColorWith(card)) {
                        return false;
                    }
                }
            }
        } else if (property.startsWith("sharesCreatureTypeWith")) {
            if (property.equals("sharesCreatureTypeWith")) {
                if (!sharesCreatureTypeWith(source)) {
                    return false;
                }
            } else {
                final String restriction = property.split("sharesCreatureTypeWith ")[1];
                switch (restriction) {
                    case "TopCardOfLibrary":
                        final CardCollectionView cards = sourceController.getCardsIn(ZoneType.Library);
                        if (cards.isEmpty() || !sharesCreatureTypeWith(cards.get(0))) {
                            return false;
                        }
                        break;
                    case "Enchanted":
                        for (final SpellAbility sa : source.currentState.getNonManaAbilities()) {
                            final SpellAbility root = sa.getRootAbility();
                            Card c = source.getEnchantingCard();
                            if ((c == null) && (root != null)
                                    && (root.getPaidList("Sacrificed") != null)
                                    && !root.getPaidList("Sacrificed").isEmpty()) {
                                c = root.getPaidList("Sacrificed").get(0).getEnchantingCard();
                                if (!sharesCreatureTypeWith(c)) {
                                    return false;
                                }
                            }
                        }
                        break;
                    case "Equipped":
                        if (source.isEquipping() && sharesCreatureTypeWith(source.getEquipping())) {
                            return true;
                        }
                        return false;
                    case "Remembered":
                        for (final Object rem : source.getRemembered()) {
                            if (rem instanceof Card) {
                                final Card card = (Card) rem;
                                if (sharesCreatureTypeWith(card)) {
                                    return true;
                                }
                            }
                        }
                        return false;
                    case "AllRemembered":
                        for (final Object rem : source.getRemembered()) {
                            if (rem instanceof Card) {
                                final Card card = (Card) rem;
                                if (!sharesCreatureTypeWith(card)) {
                                    return false;
                                }
                            }
                        }
                        break;
                    default:
                        boolean shares = false;
                        for (final Card card : sourceController.getCardsIn(ZoneType.Battlefield)) {
                            if (card.isValid(restriction, sourceController, source, spellAbility) && sharesCreatureTypeWith(card)) {
                                shares = true;
                            }
                        }
                        if (!shares) {
                            return false;
                        }
                        break;
                }
            }
        } else if (property.startsWith("sharesCardTypeWith")) {
            if (property.equals("sharesCardTypeWith")) {
                if (!sharesCardTypeWith(source)) {
                    return false;
                }
            } else {
                final String restriction = property.split("sharesCardTypeWith ")[1];
                switch (restriction) {
                    case "Imprinted":
                        if (!source.hasImprintedCard() || !sharesCardTypeWith(Iterables.getFirst(source.getImprintedCards(), null))) {
                            return false;
                        }
                        break;
                    case "Remembered":
                        for (final Object rem : source.getRemembered()) {
                            if (rem instanceof Card) {
                                final Card card = (Card) rem;
                                if (sharesCardTypeWith(card)) {
                                    return true;
                                }
                            }
                        }
                        return false;
                    case "EachTopLibrary":
                        final CardCollection cards = new CardCollection();
                        for (Player p : game.getPlayers()) {
                            final Card top = p.getCardsIn(ZoneType.Library).get(0);
                            cards.add(top);
                        }
                        for (Card c : cards) {
                            if (sharesCardTypeWith(c)) {
                                return true;
                            }
                        }
                        return false;
                }
            }
        } else if (property.equals("sharesPermanentTypeWith")) {
            if (!sharesPermanentTypeWith(source)) {
                return false;
            }
        } else if (property.equals("canProduceSameManaTypeWith")) {
            if (!canProduceSameManaTypeWith(source)) {
                return false;
            }
        } else if (property.startsWith("sharesNameWith")) {
            if (property.equals("sharesNameWith")) {
                if (!sharesNameWith(source)) {
                    return false;
                }
            } else {
                final String restriction = property.split("sharesNameWith ")[1];
                if (restriction.equals("YourGraveyard")) {
                    return !CardLists.filter(sourceController.getCardsIn(ZoneType.Graveyard), CardPredicates.sharesNameWith(this)).isEmpty();
                } else if (restriction.equals(ZoneType.Graveyard.toString())) {
                    return !CardLists.filter(game.getCardsIn(ZoneType.Graveyard), CardPredicates.sharesNameWith(this)).isEmpty();
                } else if (restriction.equals(ZoneType.Battlefield.toString())) {
                    return !CardLists.filter(game.getCardsIn(ZoneType.Battlefield), CardPredicates.sharesNameWith(this)).isEmpty();
                } else if (restriction.equals("ThisTurnCast")) {
                    return !CardLists.filter(CardUtil.getThisTurnCast("Card", source), CardPredicates.sharesNameWith(this)).isEmpty();
                } else if (restriction.startsWith("Remembered") || restriction.startsWith("Imprinted")) {
                    CardCollection list = AbilityUtils.getDefinedCards(source, restriction, spellAbility);
                    return !CardLists.filter(list, CardPredicates.sharesNameWith(this)).isEmpty();
                } else if (restriction.equals("MovedToGrave")) {
                    for (final SpellAbility sa : source.currentState.getNonManaAbilities()) {
                        final SpellAbility root = sa.getRootAbility();
                        if (root != null && (root.getPaidList("MovedToGrave") != null)
                                && !root.getPaidList("MovedToGrave").isEmpty()) {
                            final CardCollectionView cards = root.getPaidList("MovedToGrave");
                            for (final Card card : cards) {
                                String name = card.getName();
                                if (StringUtils.isEmpty(name)) {
                                    name = card.getPaperCard().getName();
                                }
                                if (getName().equals(name)) {
                                    return true;
                                }
                            }
                        }
                    }
                    return false;
                } else if (restriction.equals("NonToken")) {
                    return !CardLists.filter(game.getCardsIn(ZoneType.Battlefield),
                            Presets.NON_TOKEN, CardPredicates.sharesNameWith(this)).isEmpty();
                } else if (restriction.equals("TriggeredCard")) {
                    if (spellAbility == null) {
                        System.out.println("Looking at TriggeredCard but no SA?");
                    } else {
                        Card triggeredCard = ((Card)spellAbility.getTriggeringObject("Card"));
                        if (triggeredCard != null && sharesNameWith(triggeredCard)) {
                            return true;
                        }
                    }
                    return false;
                }
            }
        } else if (property.startsWith("doesNotShareNameWith")) {
            if (property.equals("doesNotShareNameWith")) {
                if (sharesNameWith(source)) {
                    return false;
                }
            } else {
                final String restriction = property.split("doesNotShareNameWith ")[1];
                if (restriction.startsWith("Remembered") || restriction.startsWith("Imprinted")) {
                    CardCollection list = AbilityUtils.getDefinedCards(source, restriction, spellAbility);
                    return CardLists.filter(list, CardPredicates.sharesNameWith(this)).isEmpty();
                }
            }
        } else if (property.startsWith("sharesControllerWith")) {
            if (property.equals("sharesControllerWith")) {
                if (!sharesControllerWith(source)) {
                    return false;
                }
            } else {
                final String restriction = property.split("sharesControllerWith ")[1];
	            if (restriction.startsWith("Remembered") || restriction.startsWith("Imprinted")) {
	                CardCollection list = AbilityUtils.getDefinedCards(source, restriction, spellAbility);
	                return !CardLists.filter(list, CardPredicates.sharesControllerWith(this)).isEmpty();
	            }
            }
        } else if (property.startsWith("sharesOwnerWith")) {
            if (property.equals("sharesOwnerWith")) {
                if (!getOwner().equals(source.getOwner())) {
                    return false;
                }
            } else {
                final String restriction = property.split("sharesOwnerWith ")[1];
                if (restriction.equals("Remembered")) {
                    for (final Object rem : source.getRemembered()) {
                        if (rem instanceof Card) {
                            final Card card = (Card) rem;
                            if (!getOwner().equals(card.getOwner())) {
                                return false;
                            }
                        }
                    }
                }
            }
        } else if (property.startsWith("SecondSpellCastThisTurn")) {
            final List<Card> cards = CardUtil.getThisTurnCast("Card", source);
            if (cards.size() < 2)  {
                return false;
            }
            else if (cards.get(1) != this) {
                return false;
            }
        } else if (property.equals("ThisTurnCast")) {
            for (final Card card : CardUtil.getThisTurnCast("Card", source)) {
                if (equals(card)) {
                    return true;
                }
            }
            return false;
        } else if (property.startsWith("ThisTurnEntered")) {
            final String restrictions = property.split("ThisTurnEntered_")[1];
            final String[] res = restrictions.split("_");
            final ZoneType destination = ZoneType.smartValueOf(res[0]);
            ZoneType origin = null;
            if (res.length > 1 && res[1].equals("from")) {
                origin = ZoneType.smartValueOf(res[2]);
            }
            CardCollectionView cards = CardUtil.getThisTurnEntered(destination,
                    origin, "Card", source);
            if (!cards.contains(this)) {
                return false;
            }
        } else if (property.startsWith("ControlledByPlayerInTheDirection")) {
            final String restrictions = property.split("ControlledByPlayerInTheDirection_")[1];
            final String[] res = restrictions.split("_");
            final Direction direction = Direction.valueOf(res[0]);
            Player p = null;
            if (res.length > 1) {
                for (Player pl : game.getPlayers()) {
                    if (pl.isValid(res[1], sourceController, source, spellAbility)) {
                        p = pl;
                        break;
                    }
                }
            } else {
                p = sourceController;
            }
            if (p == null || !controller.equals(game.getNextPlayerAfter(p, direction))) {
                return false;
            }
        } else if (property.startsWith("sharesTypeWith")) {
            if (property.equals("sharesTypeWith")) {
                if (!sharesTypeWith(source)) {
                    return false;
                }
            } else {
                final String restriction = property.split("sharesTypeWith ")[1];
                final Card checkCard;
                if (restriction.startsWith("Triggered")) {
                    final Object triggeringObject = source.getTriggeringObject(restriction.substring("Triggered".length()));
                    if (!(triggeringObject instanceof Card)) {
                        return false;
                    }
                    checkCard = (Card) triggeringObject;
                } else {
                    return false;
                }

                if (!sharesTypeWith(checkCard)) {
                    return false;
                }
            }
        } else if (property.startsWith("hasKeyword")) {
            // "withFlash" would find Flashback cards, add this to fix Mystical Teachings
            if (!hasKeyword(property.substring(10))) {
                return false;
            }
        } else if (property.startsWith("withFlashback")) {
            boolean fb = false;
            if (hasStartOfUnHiddenKeyword("Flashback")) {
                fb = true;
            }
            for (final SpellAbility sa : getSpellAbilities()) {
                if (sa.isFlashBackAbility()) {
                    fb = true;
                }
            }
            if (!fb) {
                return false;
            }
        } else if (property.startsWith("with")) {
            // ... Card keywords
            if (property.startsWith("without") && hasStartOfUnHiddenKeyword(property.substring(7))) {
                return false;
            }
            if (!property.startsWith("without") && !hasStartOfUnHiddenKeyword(property.substring(4))) {
                return false;
            }
        } else if (property.startsWith("tapped")) {
            if (!isTapped()) {
                return false;
            }
        } else if (property.startsWith("untapped")) {
            if (!isUntapped()) {
                return false;
            }
        } else if (property.startsWith("faceDown")) {
            if (!isFaceDown()) {
                return false;
            }
        } else if (property.startsWith("faceUp")) {
            if (isFaceDown()) {
                return false;
            }
        } else if (property.startsWith("hasLevelUp")) {
            for (final SpellAbility sa : getSpellAbilities()) {
                if (sa.getApi() == ApiType.PutCounter && sa.hasParam("LevelUp")) {
                    return true;
                }
            }
            return false;
        } else if (property.startsWith("DrawnThisTurn")) {
          if (!getDrawnThisTurn()) {
              return false;
          }
        } else if (property.startsWith("enteredBattlefieldThisTurn")) {
            if (!(getTurnInZone() == game.getPhaseHandler().getTurn())) {
                return false;
            }
        } else if (property.startsWith("notEnteredBattlefieldThisTurn")) {
            if (getTurnInZone() == game.getPhaseHandler().getTurn()) {
                return false;
            }
        } else if (property.startsWith("firstTurnControlled")) {
            if (!isFirstTurnControlled()) {
                return false;
            }
        } else if (property.startsWith("notFirstTurnControlled")) {
            if (isFirstTurnControlled()) {
                return false;
            }
        } else if (property.startsWith("startedTheTurnUntapped")) {
            if (!hasStartedTheTurnUntapped()) {
                return false;
            }
        } else if (property.startsWith("cameUnderControlSinceLastUpkeep")) {
            if (!cameUnderControlSinceLastUpkeep()) {
                return false;
            }
        } else if (property.equals("attackedOrBlockedSinceYourLastUpkeep")) {
            if (!getDamageHistory().hasAttackedSinceLastUpkeepOf(sourceController)
                    && !getDamageHistory().hasBlockedSinceLastUpkeepOf(sourceController)) {
                return false;
            }
        } else if (property.equals("blockedOrBeenBlockedSinceYourLastUpkeep")) {
            if (!getDamageHistory().hasBeenBlockedSinceLastUpkeepOf(sourceController)
                    && !getDamageHistory().hasBlockedSinceLastUpkeepOf(sourceController)) {
                return false;
            }
        } else if (property.startsWith("dealtDamageToYouThisTurn")) {
            if (!getDamageHistory().getThisTurnDamaged().contains(sourceController)) {
                return false;
            }
        } else if (property.startsWith("dealtDamageToOppThisTurn")) {
            if (!hasDealtDamageToOpponentThisTurn()) {
                return false;
            }
        } else if (property.startsWith("controllerWasDealtCombatDamageByThisTurn")) {
            if (!source.getDamageHistory().getThisTurnCombatDamaged().contains(controller)) {
                return false;
            }
        } else if (property.startsWith("controllerWasDealtDamageByThisTurn")) {
            if (!source.getDamageHistory().getThisTurnDamaged().contains(controller)) {
                return false;
            }
        } else if (property.startsWith("wasDealtDamageThisTurn")) {
            if ((getReceivedDamageFromThisTurn().keySet()).isEmpty()) {
                return false;
            }
        } else if (property.startsWith("dealtDamageThisTurn")) {
            if (getTotalDamageDoneBy() == 0) {
                return false;
            }
        } else if (property.startsWith("attackedThisTurn")) {
            if (!getDamageHistory().getCreatureAttackedThisTurn()) {
                return false;
            }
        } else if (property.startsWith("attackedLastTurn")) {
            return getDamageHistory().getCreatureAttackedLastTurnOf(controller);
        } else if (property.startsWith("blockedThisTurn")) {
            if (!getDamageHistory().getCreatureBlockedThisTurn()) {
                return false;
            }
        } else if (property.startsWith("gotBlockedThisTurn")) {
            if (!getDamageHistory().getCreatureGotBlockedThisTurn()) {
                return false;
            }
        } else if (property.startsWith("notAttackedThisTurn")) {
            if (getDamageHistory().getCreatureAttackedThisTurn()) {
                return false;
            }
        } else if (property.startsWith("notAttackedLastTurn")) {
            return !getDamageHistory().getCreatureAttackedLastTurnOf(controller);
        } else if (property.startsWith("notBlockedThisTurn")) {
            if (getDamageHistory().getCreatureBlockedThisTurn()) {
                return false;
            }
        } else if (property.startsWith("greatestPower")) {
            CardCollectionView cards = CardLists.filter(game.getCardsIn(ZoneType.Battlefield), Presets.CREATURES);
            if (property.contains("ControlledBy")) {
                FCollectionView<Player> p = AbilityUtils.getDefinedPlayers(source, property.split("ControlledBy")[1], null);
                cards = CardLists.filterControlledBy(cards, p);
                if (!cards.contains(this)) {
                    return false;
                }
            }
            for (final Card crd : cards) {
                if (crd.getNetPower() > getNetPower()) {
                    return false;
                }
            }
        } else if (property.startsWith("yardGreatestPower")) {
            final CardCollectionView cards = CardLists.filter(sourceController.getCardsIn(ZoneType.Graveyard), Presets.CREATURES);
            for (final Card crd : cards) {
                if (crd.getNetPower() > getNetPower()) {
                    return false;
                }
            }
        } else if (property.startsWith("leastPower")) {
            final CardCollectionView cards = CardLists.filter(game.getCardsIn(ZoneType.Battlefield), Presets.CREATURES);
            for (final Card crd : cards) {
                if (crd.getNetPower() < getNetPower()) {
                    return false;
                }
            }
        } else if (property.startsWith("leastToughness")) {
            final CardCollectionView cards = CardLists.filter(game.getCardsIn(ZoneType.Battlefield), Presets.CREATURES);
            for (final Card crd : cards) {
                if (crd.getNetToughness() < getNetToughness()) {
                    return false;
                }
            }
        } else if (property.startsWith("greatestCMC")) {
            CardCollectionView cards = CardLists.filter(game.getCardsIn(ZoneType.Battlefield), Presets.CREATURES);
            if (property.contains("ControlledBy")) {
                FCollectionView<Player> p = AbilityUtils.getDefinedPlayers(source, property.split("ControlledBy")[1], null);
                cards = CardLists.filterControlledBy(cards, p);
                if (!cards.contains(this)) {
                    return false;
                }
            }
            for (final Card crd : cards) {
                if (crd.isSplitCard()) {
                    if (crd.getCMC(Card.SplitCMCMode.LeftSplitCMC) > getCMC() || crd.getCMC(Card.SplitCMCMode.RightSplitCMC) > getCMC()) {
                        return false;
                    }
                } else {
                    if (crd.getCMC() > getCMC()) {
                        return false;
                    }
                }
            }
        } else if (property.startsWith("greatestRememberedCMC")) {
            CardCollection cards = new CardCollection();
            for (final Object o : source.getRemembered()) {
                if (o instanceof Card) {
                    cards.add(game.getCardState((Card) o));
                }
            }
            if (!cards.contains(this)) {
                return false;
            }
            cards = CardLists.getCardsWithHighestCMC(cards);
            if (!cards.contains(this)) {
                return false;
            }
        } else if (property.startsWith("lowestRememberedCMC")) {
            CardCollection cards = new CardCollection();
            for (final Object o : source.getRemembered()) {
                if (o instanceof Card) {
                    cards.add(game.getCardState((Card) o));
                }
            }
            if (!cards.contains(this)) {
                return false;
            }
            cards = CardLists.getCardsWithLowestCMC(cards);
            if (!cards.contains(this)) {
                return false;
            }
        }
        else if (property.startsWith("lowestCMC")) {
            final CardCollectionView cards = game.getCardsIn(ZoneType.Battlefield);
            for (final Card crd : cards) {
                if (!crd.isLand() && !crd.isImmutable()) {
                    if (crd.isSplitCard()) {
                        if (crd.getCMC(Card.SplitCMCMode.LeftSplitCMC) < getCMC() || crd.getCMC(Card.SplitCMCMode.RightSplitCMC) < getCMC()) {
                            return false;
                        }
                    } else {
                        if (crd.getCMC() < getCMC()) {
                            return false;
                        }
                    }
                }
            }
        } else if (property.startsWith("enchanted")) {
            if (!isEnchanted()) {
                return false;
            }
        } else if (property.startsWith("unenchanted")) {
            if (isEnchanted()) {
                return false;
            }
        } else if (property.startsWith("enchanting")) {
            if (!isEnchanting()) {
                return false;
            }
        } else if (property.startsWith("equipped")) {
            if (!isEquipped()) {
                return false;
            }
        } else if (property.startsWith("unequipped")) {
            if (isEquipped()) {
                return false;
            }
        } else if (property.startsWith("equipping")) {
            if (!isEquipping()) {
                return false;
            }
        } else if (property.startsWith("token")) {
            if (!isToken()) {
                return false;
            }
        } else if (property.startsWith("nonToken")) {
            if (isToken()) {
                return false;
            }
        } else if (property.startsWith("hasXCost")) {
            SpellAbility sa1 = getFirstSpellAbility();
            if (sa1 != null && !sa1.isXCost()) {
                return false;
            }
        } else if (property.startsWith("suspended")) {
            if (!hasSuspend() || !game.isCardExiled(this)
                    || !(getCounters(CounterType.getType("TIME")) >= 1)) {
                return false;
            }
        } else if (property.startsWith("delved")) {
            if (!source.getDelved().contains(this)) {
                return false;
            }
        } else if (property.startsWith("unequalPT")) {
            if (getNetPower() == getNetToughness()) {
                return false;
            }
        } else if (property.equals("powerGTtoughness")) {
            if (getNetPower() <= getNetToughness()) {
                return false;
            }
        } else if (property.equals("powerLTtoughness")) {
            if (getNetPower() >= getNetToughness()) {
                return false;
            }
        } else if (property.startsWith("power") || property.startsWith("toughness")
                || property.startsWith("cmc") || property.startsWith("totalPT")) {
            int x;
            int x2 = -1; // used for the special case when counting TopOfLibraryCMC for a split card and then testing against it
            int y = 0;
            int y2 = -1; // alternative value for the second split face of a split card
            String rhs = "";

            if (property.startsWith("power")) {
                rhs = property.substring(7);
                y = getNetPower();
            } else if (property.startsWith("toughness")) {
                rhs = property.substring(11);
                y = getNetToughness();
            } else if (property.startsWith("cmc")) {
                rhs = property.substring(5);
                if (isSplitCard() && getCurrentStateName() == CardStateName.Original) {
                    y = getState(CardStateName.LeftSplit).getManaCost().getCMC();
                    y2 = getState(CardStateName.RightSplit).getManaCost().getCMC();
                } else {
                    y = getCMC();
                }
            } else if (property.startsWith("totalPT")) {
                rhs = property.substring(10);
                y = getNetPower() + getNetToughness();
            }
            try {
                x = Integer.parseInt(rhs);
            } catch (final NumberFormatException e) {
                x = AbilityUtils.calculateAmount(source, source.getSVar(rhs), spellAbility);

                // TODO: find a better solution for handling Count$TopOfLibraryCMC for split cards
                // (currently two CMCs are encoded in one big integer value)
                if (property.startsWith("cmc") && x > SPLIT_CMC_ENCODE_MAGIC_NUMBER) {
                    x2 = Math.round(x / SPLIT_CMC_ENCODE_MAGIC_NUMBER);
                    x -= x2 * SPLIT_CMC_ENCODE_MAGIC_NUMBER;
                }
            }

            if (y2 == -1) {
                if (!Expressions.compare(y, property, x)) {
                    if (x2 == -1 || !Expressions.compare(y, property, x2)) {
                        return false;
                    }
                }
            } else {
                if (!Expressions.compare(y, property, x) && !Expressions.compare(y2, property, x)) {
                    if (x2 == -1 || (!Expressions.compare(y, property, x2) && !Expressions.compare(y2, property, x2))) {
                        return false;
                    }
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
             * System.out.println("Parsing completed of: "+Property); for (int i
             * = 0; i < parse.length; i++) {
             * System.out.println("parse["+i+"]: "+parse[i]); }
             */

            // TODO get a working regex out of this pattern so the amount of
            // digits doesn't matter
            int number;
            final String[] splitProperty = property.split("_");
            final String strNum = splitProperty[1].substring(2);
            final String comparator = splitProperty[1].substring(0, 2);
            String counterType;
            try {
                number = Integer.parseInt(strNum);
            } catch (final NumberFormatException e) {
                number = CardFactoryUtil.xCount(source, source.getSVar(strNum));
            }
            counterType = splitProperty[2];

            final int actualnumber = getCounters(CounterType.getType(counterType));

            if (!Expressions.compare(actualnumber, comparator, number)) {
                return false;
            }
        }
        // These predicated refer to ongoing combat. If no combat happens, they'll return false (meaning not attacking/blocking ATM)
        else if (property.startsWith("attacking")) {
            if (null == combat) return false;
            if (property.equals("attacking"))    return combat.isAttacking(this);
            if (property.equals("attackingLKI")) return combat.isLKIAttacking(this);
            if (property.equals("attackingYou")) return combat.isAttacking(this, sourceController);
            if (property.equals("attackingYouOrYourPW"))  {
                Player defender = combat.getDefenderPlayerByAttacker(this);
                if (!sourceController.equals(defender)) {
                    return false;
                }
            }
        } else if (property.startsWith("notattacking")) {
            return null == combat || !combat.isAttacking(this);
        } else if (property.equals("attackedThisCombat")) {
        	if (null == combat || !this.getDamageHistory().getCreatureAttackedThisCombat()) {
                return false;
        	}
        } else if (property.equals("attackedBySourceThisCombat")) {
            if (null == combat) return false;
            final GameEntity defender = combat.getDefenderByAttacker(source);
            if (defender instanceof Card && !equals(defender)) {
                return false;
            }
        } else if (property.startsWith("blocking")) {
            if (null == combat) return false;
            String what = property.substring("blocking".length());

            if (StringUtils.isEmpty(what)) return combat.isBlocking(this);
            if (what.startsWith("Source")) return combat.isBlocking(this, source) ;
            if (what.startsWith("CreatureYouCtrl")) {
                for (final Card c : CardLists.filter(sourceController.getCardsIn(ZoneType.Battlefield), Presets.CREATURES))
                    if (combat.isBlocking(this, c))
                        return true;
                return false;
            }
            if (what.startsWith("Remembered")) {
                for (final Object o : source.getRemembered()) {
                    if (o instanceof Card && combat.isBlocking(this, (Card) o)) {
                        return true;
                    }
                }
                return false;
            }
        } else if (property.startsWith("sharesBlockingAssignmentWith")) {
            if (null == combat) { return false; }
            if (null == combat.getAttackersBlockedBy(source) || null == combat.getAttackersBlockedBy(this)) { return false; }

            CardCollection sourceBlocking = new CardCollection(combat.getAttackersBlockedBy(source));
            CardCollection thisBlocking = new CardCollection(combat.getAttackersBlockedBy(this));
            if (Collections.disjoint(sourceBlocking, thisBlocking)) {
                return false;
            }
        } else if (property.startsWith("notblocking")) {
            return null == combat || !combat.isBlocking(this);
        }
        // Nex predicates refer to past combat and don't need a reference to actual combat
        else if (property.equals("blocked")) {
            return null != combat && combat.isBlocked(this);
        } else if (property.startsWith("blockedBySource")) {
            return null != combat && combat.isBlocking(source, this);
        } else if (property.startsWith("blockedThisTurn")) {
            return getBlockedThisTurn() != null;
        } else if (property.startsWith("blockedByThisTurn")) {
            return getBlockedByThisTurn() != null;
        } else if (property.startsWith("blockedBySourceThisTurn")) {
            return source.blockedByThisTurn != null && source.blockedByThisTurn.contains(this);
        } else if (property.startsWith("blockedSource")) {
            return null != combat && combat.isBlocking(this, source);
        } else if (property.startsWith("isBlockedByRemembered")) {
            if (null == combat) return false;
            for (final Object o : source.getRemembered()) {
                if (o instanceof Card && combat.isBlocking((Card) o, this)) {
                    return true;
                }
            }
            return false;
        } else if (property.startsWith("blockedRemembered")) {
            Card rememberedcard;
            for (final Object o : source.getRemembered()) {
                if (o instanceof Card) {
                    rememberedcard = (Card) o;
                    if (blockedThisTurn == null || !blockedThisTurn.contains(rememberedcard)) {
                        return false;
                    }
                }
            }
        } else if (property.startsWith("blockedByRemembered")) {
            Card rememberedcard;
            for (final Object o : source.getRemembered()) {
                if (o instanceof Card) {
                    rememberedcard = (Card) o;
                    if (blockedByThisTurn == null || !blockedByThisTurn.contains(rememberedcard)) {
                        return false;
                    }
                }
            }
        } else if (property.startsWith("unblocked")) {
            if (game.getCombat() == null || !game.getCombat().isUnblocked(this)) {
                return false;
            }
        } else if (property.equals("attackersBandedWith")) {
            if (equals(source)) {
                // You don't band with yourself
                return false;
            }
            AttackingBand band = combat == null ? null : combat.getBandOfAttacker(source);
            if (band == null || !band.getAttackers().contains(this)) {
                return false;
            }
        } else if (property.startsWith("kicked")) {
            if (property.equals("kicked")) {
                if (getKickerMagnitude() == 0) {
                    return false;
                }
            } else {
                String s = property.split("kicked ")[1];
                if ("1".equals(s) && !isOptionalCostPaid(OptionalCost.Kicker1)) return false;
                if ("2".equals(s) && !isOptionalCostPaid(OptionalCost.Kicker2)) return false;
            }
        } else if (property.startsWith("notkicked")) {
            if (getKickerMagnitude() > 0) {
                return false;
            }
        } else if (property.startsWith("pseudokicked")) {
            if (property.equals("pseudokicked")) {
                if (!isOptionalCostPaid(OptionalCost.Generic)) return false;
            }
        } else if (property.startsWith("notpseudokicked")) {
            if (property.equals("pseudokicked")) {
                if (isOptionalCostPaid(OptionalCost.Generic)) return false;
            }
        } else if (property.startsWith("surged")) {
            if (!isOptionalCostPaid(OptionalCost.Surge)) {
                return false;
            }
        } else if (property.startsWith("evoked")) {
            if (!isEvoked()) {
                return false;
            }
        } else if (property.equals("HasDevoured")) {
            if (devouredCards == null || devouredCards.isEmpty()) {
                return false;
            }
        } else if (property.equals("HasNotDevoured")) {
            if (devouredCards != null && !devouredCards.isEmpty()) {
                return false;
            }
        } else if (property.equals("IsMonstrous")) {
            if (!isMonstrous()) {
                return false;
            }
        } else if (property.equals("IsNotMonstrous")) {
            if (isMonstrous()) {
                return false;
            }
        } else if (property.equals("IsUnearthed")) {
            if (!isUnearthed()) {
                return false;
            }
        } else if (property.equals("IsRenowned")) {
            if (!isRenowned()) {
                return false;
            }
        } else if (property.equals("IsNotRenowned")) {
            if (isRenowned()) {
                return false;
            }
        } else if (property.startsWith("non")) {
            // ... Other Card types
            if (getType().hasStringType(property.substring(3))) {
                return false;
            }
        } else if (property.equals("CostsPhyrexianMana")) {
            if (!currentState.getManaCost().hasPhyrexian()) {
                return false;
            }
        } else if (property.startsWith("RememberMap")) {
            System.out.println(source.getRememberMap());
            for (SpellAbility sa : source.getSpellAbilities()) {
                if (sa.getActivatingPlayer() == null) continue;
                for (Player p : AbilityUtils.getDefinedPlayers(source, property.split("RememberMap_")[1], sa)) {
                    if (source.getRememberMap().get(p).contains(this)) {
                        return true;
                    }
                }
            }
            return false;
        } else if (property.equals("IsRemembered")) {
            if (!source.isRemembered(this)) {
                return false;
            }
        } else if (property.equals("IsNotRemembered")) {
            if (source.isRemembered(this)) {
                return false;
            }
        } else if (property.equals("IsImprinted")) {
            if (!source.hasImprintedCard(this)) {
                return false;
            }
        } else if (property.equals("IsNotImprinted")) {
            if (source.hasImprintedCard(this)) {
                return false;
            }
        } else if (property.equals("hasActivatedAbilityWithTapCost")) {
            for (final SpellAbility sa : getSpellAbilities()) {
                if (sa.isAbility() && (sa.getPayCosts() != null) && sa.getPayCosts().hasTapCost()) {
                    return true;
                }
            }
            return false;
        } else if (property.equals("hasActivatedAbility")) {
            for (final SpellAbility sa : getSpellAbilities()) {
                if (sa.isAbility()) {
                    return true;
                }
            }
            return false;
        } else if (property.equals("hasManaAbility")) {
            for (final SpellAbility sa : getSpellAbilities()) {
                if (sa.isManaAbility()) {
                    return true;
                }
            }
            return false;
        } else if (property.equals("hasNonManaActivatedAbility")) {
            for (final SpellAbility sa : getSpellAbilities()) {
                if (sa.isAbility() && !sa.isManaAbility()) {
                    return true;
                }
            }
            return false;
        } else if (property.equals("NoAbilities")) {
            if (!((getAbilityText().trim().equals("") || isFaceDown()) && (getUnhiddenKeywords().isEmpty()))) {
                return false;
            }
        } else if (property.equals("HasCounters")) {
            if (!hasCounters()) {
                return false;
            }
        } else if (property.equals("wasCast")) {
            if (null == getCastFrom()) {
                return false;
            }
        } else if (property.equals("wasNotCast")) {
            if (null != getCastFrom()) {
                return false;
            }
        } else if (property.startsWith("wasCastFrom")) {
            // How are we getting in here with a comma?
            final String strZone = property.split(",")[0].substring(11);
            final ZoneType realZone = ZoneType.smartValueOf(strZone);
            if (realZone != getCastFrom()) {
                return false;
            }
        } else if (property.startsWith("wasNotCastFrom")) {
            final String strZone = property.substring(14);
            final ZoneType realZone = ZoneType.smartValueOf(strZone);
            if (realZone == getCastFrom()) {
                return false;
            }
        } else if (property.startsWith("set")) {
            final String setCode = property.substring(3, 6);
            if (!getSetCode().equals(setCode)) {
                return false;
            }
        } else if (property.startsWith("inZone")) {
            final String strZone = property.substring(6);
            final ZoneType realZone = ZoneType.smartValueOf(strZone);
            if (!isInZone(realZone)) {
                return false;
            }
        } else if (property.equals("ChosenType")) {
            if (!getType().hasStringType(source.getChosenType())) {
                return false;
            }
        } else if (property.equals("IsNotChosenType")) {
            if (getType().hasStringType(source.getChosenType())) {
                return false;
            }
        } else if (property.equals("IsCommander")) {
            if (!isCommander) {
                return false;
            }
        } else if (property.startsWith("HasSVar")) {
        	final String svar = property.substring(8);
        	if (!hasSVar(svar)) {
                return false;
            }
        } else if (property.startsWith("HasSubtype")) {
            final String subType = property.substring(11);
            if (!getType().hasSubtype(subType)) {
                return false;
            }
        } else if (property.startsWith("HasNoSubtype")) {
            final String subType = property.substring(13);
            if (getType().hasSubtype(subType)) {
                return false;
            }
        } else {
            if (!getType().hasStringType(property)) {
                return false;
            }
        }
        return true;
    }

    public final boolean isImmutable() {
        return isImmutable;
    }
    public final void setImmutable(final boolean isImmutable0) {
        isImmutable = isImmutable0;
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

    public final boolean sharesNameWith(final Card c1) {
        boolean shares;
        shares = getName().equals(c1.getName());

        if (isSplitCard() && c1.isSplitCard()) {
            shares |= c1.getName().equals(getState(CardStateName.LeftSplit).getName());
            shares |= c1.getName().equals(getState(CardStateName.RightSplit).getName());
        }

        return shares;
    }

    public final boolean sharesColorWith(final Card c1) {
        boolean shares;
        shares = (isBlack() && c1.isBlack());
        shares |= (isBlue() && c1.isBlue());
        shares |= (isGreen() && c1.isGreen());
        shares |= (isRed() && c1.isRed());
        shares |= (isWhite() && c1.isWhite());
        return shares;
    }

    public final boolean sharesCMCWith(final Card c1) {
        int x;
        int x2 = -1;
        int y = 0;
        int y2 = -1;

        //need to get GameState for Discarded Cards
        final Card host = game.getCardState(this);
        final Card other = game.getCardState(c1);

        if (host.isSplitCard() && host.getCurrentStateName() == CardStateName.Original) {
            x = host.getState(CardStateName.LeftSplit).getManaCost().getCMC();
            x2 = host.getState(CardStateName.RightSplit).getManaCost().getCMC();
        } else {
            x = host.getCMC();
        }

        if (other.isSplitCard() && other.getCurrentStateName() == CardStateName.Original) {
            y = other.getState(CardStateName.LeftSplit).getManaCost().getCMC();
            y2 = other.getState(CardStateName.RightSplit).getManaCost().getCMC();

            if (host.isSplitCard() && host.getCurrentStateName() == CardStateName.Original) {
                return x == y || x == y2 || x2 == y || x2 == y2;
            } else {
                return x == y || x == y2;
            }
        } else {
            y = other.getCMC();
            return x == y || x2 == y;
        }
    }

    public final boolean sharesCreatureTypeWith(final Card c1) {
        if (c1 == null) {
            return false;
        }

        for (final String type : getType().getCreatureTypes()) {
            if (type.equals("AllCreatureTypes") && c1.hasACreatureType()) {
                return true;
            }
            if (c1.getType().hasCreatureType(type)) {
                return true;
            }
        }
        return false;
    }

    public final boolean sharesLandTypeWith(final Card c1) {
        if (c1 == null) {
            return false;
        }

        for (final String type : getType().getLandTypes()) {
            if (c1.getType().hasSubtype(type)) {
                return true;
            }
        }
        return false;
    }

    public final boolean sharesPermanentTypeWith(final Card c1) {
        if (c1 == null) {
            return false;
        }

        for (final CoreType type : getType().getCoreTypes()) {
            if (type.isPermanent && c1.getType().hasType(type)) {
                return true;
            }
        }
        return false;
    }

    public final boolean sharesCardTypeWith(final Card c1) {
        for (final CoreType type : getType().getCoreTypes()) {
            if (c1.getType().hasType(type)) {
                return true;
            }
        }
        return false;
    }

    public final boolean sharesTypeWith(final Card c1) {
        for (final String type : getType()) {
            if (c1.getType().hasStringType(type)) {
                return true;
            }
        }
        return false;
    }

    public final boolean sharesControllerWith(final Card c1) {
        return c1 != null && getController().equals(c1.getController());
    }

    public final boolean hasACreatureType() {
        for (final String type : getType().getSubtypes()) {
            if (forge.card.CardType.isACreatureType(type) ||  type.equals("AllCreatureTypes")) {
                return true;
            }
        }
        return false;
    }

    public final boolean hasALandType() {
        for (final String type : getType().getSubtypes()) {
            if (forge.card.CardType.isALandType(type) || forge.card.CardType.isABasicLandType(type)) {
                return true;
            }
        }
        return false;
    }

    public final boolean isUsedToPay() {
        return usedToPayCost;
    }
    public final void setUsedToPay(final boolean b) {
        usedToPayCost = b;
    }

    // /////////////////////////
    //
    // Damage code
    //
    // ////////////////////////

    public final Map<Card, Integer> getReceivedDamageFromThisTurn() {
        return receivedDamageFromThisTurn;
    }
    public final void setReceivedDamageFromThisTurn(final Map<Card, Integer> receivedDamageList) {
        receivedDamageFromThisTurn = receivedDamageList;
    }
    public final void addReceivedDamageFromThisTurn(final Card c, final int damage) {
        int currentDamage = 0;
        if (receivedDamageFromThisTurn.containsKey(c)) {
            currentDamage = receivedDamageFromThisTurn.get(c);
        }
        receivedDamageFromThisTurn.put(c, damage+currentDamage);
    }
    public final void resetReceivedDamageFromThisTurn() {
        receivedDamageFromThisTurn.clear();
    }

    public final int getTotalDamageRecievedThisTurn() {
        int total = 0;
        for (int damage : receivedDamageFromThisTurn.values()) {
            total += damage;
        }
        return total;
    }

    // TODO: Combine getDealtDamageToThisTurn with addDealtDamageToPlayerThisTurn using GameObject, Integer
    public final Map<Card, Integer> getDealtDamageToThisTurn() {
        return dealtDamageToThisTurn;
    }
    public final void setDealtDamageToThisTurn(final Map<Card, Integer> dealtDamageList) {
        dealtDamageToThisTurn = dealtDamageList;
    }
    public final void addDealtDamageToThisTurn(final Card c, final int damage) {
        int currentDamage = 0;
        if (dealtDamageToThisTurn.containsKey(c)) {
            currentDamage = dealtDamageToThisTurn.get(c);
        }
        dealtDamageToThisTurn.put(c, damage+currentDamage);
    }
    public final void resetDealtDamageToThisTurn() {
        dealtDamageToThisTurn.clear();
    }

    public final Map<String, Integer> getDealtDamageToPlayerThisTurn() {
        return dealtDamageToPlayerThisTurn;
    }
    public final void setDealtDamageToPlayerThisTurn(final Map<String, Integer> dealtDamageList) {
        dealtDamageToPlayerThisTurn = dealtDamageList;
    }
    public final void addDealtDamageToPlayerThisTurn(final String player, final int damage) {
        int currentDamage = 0;
        if (dealtDamageToPlayerThisTurn.containsKey(player)) {
            currentDamage = dealtDamageToPlayerThisTurn.get(player);
        }
        dealtDamageToPlayerThisTurn.put(player, damage+currentDamage);
    }
    public final void resetDealtDamageToPlayerThisTurn() {
        dealtDamageToPlayerThisTurn.clear();
    }

    public final boolean hasDealtDamageToOpponentThisTurn() {
        for (final Player p : getDamageHistory().getThisTurnDamaged()) {
            if (getController().isOpponentOf(p)) {
                return true;
            }
        }
        return false;
    }

    // this is the minimal damage a trampling creature has to assign to a blocker
    public final int getLethalDamage() {
        return getNetToughness() - getDamage() - getTotalAssignedDamage();
    }

    public final int getDamage() {
        return damage;
    }
    public final void setDamage(int damage0) {
        if (damage == damage0) { return; }
        damage = damage0;
        view.updateDamage(this);
        getGame().fireEvent(new GameEventCardStatsChanged(this));
    }

    public final boolean hasBeenDealtDeathtouchDamage() {
        return hasBeenDealtDeathtouchDamage;
    }
    public final void setHasBeenDealtDeathtouchDamage(final boolean hasBeenDealtDeatchtouchDamage) {
        this.hasBeenDealtDeathtouchDamage = hasBeenDealtDeatchtouchDamage;
    }

    public final Map<Card, Integer> getAssignedDamageMap() {
        return assignedDamageMap;
    }

    public final void addAssignedDamage(int assignedDamage0, final Card sourceCard) {
        if (assignedDamage0 < 0) {
            assignedDamage0 = 0;
        }
        Log.debug(this + " - was assigned " + assignedDamage0 + " damage, by " + sourceCard);
        if (!assignedDamageMap.containsKey(sourceCard)) {
            assignedDamageMap.put(sourceCard, assignedDamage0);
        }
        else {
            assignedDamageMap.put(sourceCard, assignedDamageMap.get(sourceCard) + assignedDamage0);
        }
        if (assignedDamage0 > 0) {
            view.updateAssignedDamage(this);
        }
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

    public final void addCombatDamage(final Map<Card, Integer> map) {
        for (final Entry<Card, Integer> entry : map.entrySet()) {
            final Card source = entry.getKey();
            int damageToAdd = entry.getValue();

            damageToAdd = replaceDamage(damageToAdd, source, true);
            damageToAdd = preventDamage(damageToAdd, source, true);

            map.put(source, damageToAdd);
        }

        if (isInPlay()) {
            addDamage(map);
        }
    }

    // This is used by the AI to forecast an effect (so it must not change the game state)
    public final int staticDamagePrevention(final int damage, final int possiblePrevention, final Card source, final boolean isCombat) {
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
                        && !source.isValid(params.get("ValidSource"), ca.getController(), ca, null)) {
                    continue;
                }
                if (params.containsKey("ValidTarget")
                        && !isValid(params.get("ValidTarget"), ca.getController(), ca, null)) {
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
        return staticDamagePrevention(damage - possiblePrevention, source, isCombat, true);
    }

    // This should be also usable by the AI to forecast an effect (so it must not change the game state)
    @Override
    public final int staticDamagePrevention(final int damageIn, final Card source, final boolean isCombat, final boolean isTest) {
        if (getGame().getStaticEffects().getGlobalRuleChange(GlobalRuleChange.noPrevention)) {
            return damageIn;
        }

        if (isCombat && getGame().getPhaseHandler().isPreventCombatDamageThisTurn()) {
            return 0;
        }

        int restDamage = damageIn;

        if (hasProtectionFromDamage(source)) {
            return 0;
        }

        for (String kw : source.getKeywords()) {
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
        for (String kw : getKeywords()) {
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
                final int absorbed = getKeywordMagnitude("Absorb");
                if (restDamage > absorbed) {
                    restDamage = restDamage - absorbed;
                } else {
                    return 0;
                }
            }
            if (kw.startsWith("PreventAllDamageBy")) {
                if (source.isValid(kw.split(" ", 2)[1].split(","), getController(), this, null)) {
                    return 0;
                }
            }
        }

        // Prevent Damage static abilities
        for (final Card ca : getGame().getCardsIn(ZoneType.listValueOf("Battlefield,Command"))) {
            for (final StaticAbility stAb : ca.getStaticAbilities()) {
                restDamage = stAb.applyAbility("PreventDamage", source, this, restDamage, isCombat, isTest);
            }
        }
        return restDamage > 0 ? restDamage : 0;
    }

    @Override
    public final int preventDamage(final int damage, final Card source, final boolean isCombat) {
        if (getGame().getStaticEffects().getGlobalRuleChange(GlobalRuleChange.noPrevention)
                || source.hasKeyword("Damage that would be dealt by CARDNAME can't be prevented.")) {
            return damage;
        }

        int restDamage = damage;

        boolean DEBUGShieldsWithEffects = false;
        while (!getPreventNextDamageWithEffect().isEmpty() && restDamage != 0) {
            Map<Card, Map<String, String>> shieldMap = getPreventNextDamageWithEffect();
            CardCollectionView preventionEffectSources = new CardCollection(shieldMap.keySet());
            Card shieldSource = preventionEffectSources.get(0);
            if (preventionEffectSources.size() > 1) {
                Map<String, Card> choiceMap = new TreeMap<>();
                List<String> choices = new ArrayList<>();
                for (final Card key : preventionEffectSources) {
                    String effDesc = shieldMap.get(key).get("EffectString");
                    int descIndex = effDesc.indexOf("SpellDescription");
                    effDesc = effDesc.substring(descIndex + 18);
                    String shieldDescription = key.toString() + " - " + shieldMap.get(key).get("ShieldAmount")
                            + " shields - " + effDesc;
                    choices.add(shieldDescription);
                    choiceMap.put(shieldDescription, key);
                }
                shieldSource = getController().getController().chooseProtectionShield(this, choices, choiceMap);
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
            SpellAbility shieldSA;
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
            CardCollectionView cardsInCommand = null;
            if (apiIsEffect) {
                cardsInCommand = getGame().getCardsIn(ZoneType.Command);
            }

            getController().getController().playSpellAbilityNoStack(shieldSA, true);
            if (apiIsEffect) {
                CardCollection newCardsInCommand = (CardCollection)getGame().getCardsIn(ZoneType.Command);
                newCardsInCommand.removeAll(cardsInCommand);
                if (!newCardsInCommand.isEmpty()) {
                    newCardsInCommand.get(0).setSVar("PreventedDamage", "Number$" + Integer.toString(dmgToBePrevented));
                }
            }
            subtractPreventNextDamageWithEffect(shieldSource, restDamage);
            restDamage = restDamage - dmgToBePrevented;

            if (DEBUGShieldsWithEffects) {
                System.out.println("Remaining shields: "
                    + (shieldMap.containsKey(shieldSource) ? shieldMap.get(shieldSource).get("ShieldAmount") : "all shields used"));
                System.out.println("Remaining damage: " + restDamage);
            }
        }

        final HashMap<String, Object> repParams = new HashMap<>();
        repParams.put("Event", "DamageDone");
        repParams.put("Affected", this);
        repParams.put("DamageSource", source);
        repParams.put("DamageAmount", damage);
        repParams.put("IsCombat", isCombat);
        repParams.put("Prevention", true);

        if (getGame().getReplacementHandler().run(repParams) != ReplacementResult.NotReplaced) {
            return 0;
        }

        restDamage = staticDamagePrevention(restDamage, source, isCombat, false);

        if (restDamage == 0) {
            return 0;
        }

        if (restDamage >= getPreventNextDamage()) {
            restDamage = restDamage - getPreventNextDamage();
            setPreventNextDamage(0);
        }
        else {
            setPreventNextDamage(getPreventNextDamage() - restDamage);
            restDamage = 0;
        }
        return restDamage;
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
                    restDamage += restDamage;
                }
            } else if (c.getName().equals("Dictate of the Twin Gods")) {
                restDamage += restDamage;
            } else if (c.getName().equals("Gratuitous Violence")) {
                if (c.getController().equals(source.getController()) && source.isCreature() && isCreature()) {
                    restDamage += restDamage;
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

        if (getName().equals("Phytohydra")) {
            return 0;
        }
        return restDamage;
    }

    @Override
    public final int replaceDamage(final int damageIn, final Card source, final boolean isCombat) {
        // Replacement effects
        final HashMap<String, Object> repParams = new HashMap<>();
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

    public final void addDamage(final Map<Card, Integer> sourcesMap) {
        for (final Entry<Card, Integer> entry : sourcesMap.entrySet()) {
            // damage prevention is already checked!
            addDamageAfterPrevention(entry.getValue(), entry.getKey(), true);
        }
    }

    /**
     * This function handles damage after replacement and prevention effects are
     * applied.
     */
    @Override
    public final boolean addDamageAfterPrevention(final int damageIn, final Card source, final boolean isCombat) {

        if (damageIn == 0) {
            return false; // Rule 119.8
        }

        addReceivedDamageFromThisTurn(source, damageIn);
        source.addDealtDamageToThisTurn(this, damageIn);
        if (isCombat) {
            game.getCombat().addDealtDamageTo(source, this);
        }

        if (source.hasKeyword("Lifelink")) {
            source.getController().gainLife(damageIn, source);
        }

        // Run triggers
        final Map<String, Object> runParams = new TreeMap<>();
        runParams.put("DamageSource", source);
        runParams.put("DamageTarget", this);
        runParams.put("DamageAmount", damageIn);
        runParams.put("IsCombatDamage", isCombat);
        if (!isCombat) {
            runParams.put("SpellAbilityStackInstance", game.stack.peek());
        }
        // Defending player at the time the damage was dealt
        runParams.put("DefendingPlayer", game.getCombat() != null ? game.getCombat().getDefendingPlayerRelatedTo(source) : null);
        getGame().getTriggerHandler().runTrigger(TriggerType.DamageDone, runParams, false);

        GameEventCardDamaged.DamageType damageType = DamageType.Normal;
        if (isPlaneswalker()) {
            subtractCounter(CounterType.LOYALTY, damageIn);
        }
        else {
            final Game game = source.getGame();

            boolean wither = (game.getStaticEffects().getGlobalRuleChange(GlobalRuleChange.alwaysWither)
                    || source.hasKeyword("Wither") || source.hasKeyword("Infect"));

            if (isInPlay()) {
                if (wither) {
                    addCounter(CounterType.M1M1, damageIn, true);
                    damageType = DamageType.M1M1Counters;
                }
                else {
                    damage += damageIn;
                    view.updateDamage(this);
                }
            }

            if (source.hasKeyword("Deathtouch") && isCreature()) {
                setHasBeenDealtDeathtouchDamage(true);
                damageType = DamageType.Deathtouch;
            }

            // Play the Damage sound
            game.fireEvent(new GameEventCardDamaged(this, source, damageIn, damageType));
        }
        return true;
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

    public String getImageKey(CardStateName state) {
        CardState c = getCardForUi().states.get(state);
        return (c != null ? c.getImageKey() : "");
    }

    public final boolean isEvoked() {
        return evoked;
    }
    public final void setEvoked(final boolean evokedIn) {
        evoked = evokedIn;
    }

    public final void setTributed(final boolean b) {
        tributed = b;
    }

    public boolean isMadness() {
        return madness;
    }
    public void setMadness(boolean madness0) {
        madness = madness0;
    }

    public final boolean isMonstrous() {
        return monstrous;
    }
    public final void setMonstrous(final boolean monstrous0) {
        monstrous = monstrous0;
    }

    public final int getMonstrosityNum() {
        return monstrosityNum;
    }
    public final void setMonstrosityNum(final int num) {
        monstrosityNum = num;
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
        final String image = manifested ? ImageKeys.MANIFEST_IMAGE : ImageKeys.MORPH_IMAGE;
        getState(CardStateName.FaceDown).setImageKey(ImageKeys.getTokenKey(image));
    }

    public final void animateBestow() {
        bestowTimestamp = getGame().getNextTimestamp();
        addChangedCardTypes(new CardType(Collections.singletonList("Aura")),
                new CardType(Collections.singletonList("Creature")), false, false, false, true, bestowTimestamp);
        addChangedCardKeywords(Collections.singletonList("Enchant creature"), new ArrayList<String>(), false, bestowTimestamp);
    }

    public final void unanimateBestow() {
        removeChangedCardKeywords(bestowTimestamp);
        removeChangedCardTypes(bestowTimestamp);
        bestowTimestamp = -1;
    }

    public final boolean isBestowed() {
        return bestowTimestamp != -1;
    }

    public final long getTimestamp() {
        return timestamp;
    }
    public final void setTimestamp(final long t) {
        timestamp = t;
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
    public final void addHauntedBy(Card c) {
        hauntedBy = view.addCard(hauntedBy, c, TrackableProperty.HauntedBy);
        if (c != null) {
            c.setHaunting(this);
        }
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

    public final int getDamageDoneThisTurn() {
        int sum = 0;
        for (final Card c : dealtDamageToThisTurn.keySet()) {
            sum += dealtDamageToThisTurn.get(c);
        }

        return sum;
    }

    public final int getDamageDoneToPlayerBy(final String player) {
        int sum = 0;
        for (final String p : dealtDamageToPlayerThisTurn.keySet()) {
            if (p.equals(player)) {
                sum += dealtDamageToPlayerThisTurn.get(p);
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
        for (final Card c : dealtDamageToThisTurn.keySet()) {
            sum += dealtDamageToThisTurn.get(c);
        }
        for (final String p : dealtDamageToPlayerThisTurn.keySet()) {
            sum += dealtDamageToPlayerThisTurn.get(p);
        }
        return sum;
    }

    @Override
    public boolean hasProtectionFrom(final Card source) {
        return hasProtectionFrom(source, false, false);
    }

    public boolean hasProtectionFromDamage(final Card source) {
        return hasProtectionFrom(source, false, true);
    }

    public boolean hasProtectionFrom(final Card source, final boolean checkSBA) {
        return hasProtectionFrom(source, checkSBA, false);
    }

    public boolean hasProtectionFrom(final Card source, final boolean checkSBA, final boolean damageSource) {
        if (source == null) {
            return false;
        }

        if (isImmutable()) {
            return true;
        }

        final List<String> keywords = getKeywords();
        if (keywords != null) {
            final boolean colorlessDamage = damageSource && source.hasKeyword("Colorless Damage Source");

            for (final String kw : keywords) {
                if (!kw.startsWith("Protection")) {
                    continue;
                }
                if (kw.equals("Protection from white")) {
                    if (source.isWhite() && !colorlessDamage) {
                        return true;
                    }
                } else if (kw.equals("Protection from blue")) {
                    if (source.isBlue() && !colorlessDamage) {
                        return true;
                    }
                } else if (kw.equals("Protection from black")) {
                    if (source.isBlack() && !colorlessDamage) {
                        return true;
                    }
                } else if (kw.equals("Protection from red")) {
                    if (source.isRed() && !colorlessDamage) {
                        return true;
                    }
                } else if (kw.equals("Protection from green")) {
                    if (source.isGreen() && !colorlessDamage) {
                        return true;
                    }
                } else if (kw.equals("Protection from monocolored")) {
                    if (CardUtil.getColors(source).isMonoColor() && !colorlessDamage) {
                        return true;
                    }
                } else if (kw.equals("Protection from multicolored")) {
                    if (CardUtil.getColors(source).isMulticolor() && !colorlessDamage) {
                        return true;
                    }
                } else if (kw.equals("Protection from all colors")) {
                    if (!source.isColorless() && !colorlessDamage) {
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
                    if (source.isEnchantment()) {
                        return true;
                    }
                } else if (kw.equals("Protection from everything")) {
                    return true;
                } else if (kw.startsWith("Protection:")) { // uses isValid; Protection:characteristic:desc:exception
                    final String[] kws = kw.split(":");
                    String characteristic = kws[1];

                    // if colorlessDamage then it does only check damage color..
                    if (colorlessDamage) {
                        if (characteristic.endsWith("White") || characteristic.endsWith("Blue") 
                            || characteristic.endsWith("Black") || characteristic.endsWith("Red") 
                            || characteristic.endsWith("Green") || characteristic.endsWith("Colorless")
                            || characteristic.endsWith("ChosenColor")) {
                            characteristic += "Source"; 
                        }
                    }

                    final String[] characteristics = characteristic.split(",");
                    final String exception = kws.length > 3 ? kws[3] : null; // check "This effect cannot remove sth"
                    if (source.isValid(characteristics, getController(), this, null)
                            && (!checkSBA || exception == null || !source.isValid(exception, getController(), this, null))) {
                        return true;
                    }
                } else if (kw.equals("Protection from colored spells")) {
                    if (source.isSpell() && !source.isColorless()) {
                        return true;
                    }
                } else if (kw.equals("Protection from the chosen player")) {
                    if (source.getController().equals(chosenPlayer)) {
                        return true;
                    }
                } else if (kw.startsWith("Protection from ")) {
                    final String protectType = CardUtil.getSingularType(kw.substring("Protection from ".length()));
                    if (source.getType().hasStringType(protectType)) {
                        return true;
                    }
                }
            }
        }
        return false;
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
        Zone z = getZone();
        return z != null && z.getZoneType() == zone;
    }

    public final boolean canBeDestroyed() {
        return isInPlay() && (!hasKeyword("Indestructible") || (isCreature() && getNetToughness() <= 0));
    }

    public final boolean canBeSacrificed() {
        return isInPlay() && !this.isPhasedOut() && !hasKeyword("CARDNAME can't be sacrificed.");
    }

    @Override
    public final boolean canBeTargetedBy(final SpellAbility sa) {
        if (sa == null) {
            return true;
        }

        // CantTarget static abilities
        for (final Card ca : getGame().getCardsIn(ZoneType.listValueOf("Battlefield,Command"))) {
            final Iterable<StaticAbility> staticAbilities = ca.getStaticAbilities();
            for (final StaticAbility stAb : staticAbilities) {
                if (stAb.applyAbility("CantTarget", this, sa)) {
                    return false;
                }
            }
        }

        // keywords don't work outside battlefield
        if (!isInZone(ZoneType.Battlefield)) {
            return true;
        }

        if (hasProtectionFrom(sa.getHostCard())) {
            return false;
        }

        if (isPhasedOut()) {
            return false;
        }

        final Card source = sa.getHostCard();
        final MutableBoolean result = new MutableBoolean(true);
        visitKeywords(currentState, new Visitor<String>() {
            @Override
            public void visit(String kw) {
                if (result.isFalse()) {
                    return;
                }
                switch (kw) {
                    case "Shroud":
                        StringBuilder sb = new StringBuilder();
                        sb.append("Can target CardUID_").append(String.valueOf(getId()));
                        sb.append(" with spells and abilities as though it didn't have shroud.");
                        if (!sa.getActivatingPlayer().hasKeyword(sb.toString())) {
                            result.setFalse();
                        }
                        break;
                    case "Hexproof":
                        if (sa.getActivatingPlayer().getOpponents().contains(getController())) {
                            if (!sa.getActivatingPlayer().hasKeyword("Spells and abilities you control can target hexproof creatures")) {
                                result.setFalse();
                            }
                        }
                        break;
                    case "CARDNAME can't be the target of Aura spells.":
                        if (source.isAura() && sa.isSpell()) {
                            result.setFalse();
                        }
                        break;
                    case "CARDNAME can't be enchanted.":
                        if (source.isAura()) {
                            result.setFalse();
                        }
                        break;
                    case "CARDNAME can't be equipped.":
                        if (source.isEquipment()) {
                            result.setFalse();
                        }
                        break;
                    case "CARDNAME can't be the target of spells.":
                        if (sa.isSpell()) {
                            result.setFalse();
                        }
                        break;
                }
            }
        });
        if (result.isFalse()) {
            return false;
        }
        if (sa.isSpell() && source.hasStartOfKeyword("SpellCantTarget")) {
            final int keywordPosition = source.getKeywordPosition("SpellCantTarget");
            final String parse = source.getKeywords().get(keywordPosition);
            final String[] k = parse.split(":");
            final String[] restrictions = k[1].split(",");
            if (isValid(restrictions, source.getController(), source, null)) {
                return false;
            }
        }
        return true;
    }

    public final boolean canBeControlledBy(final Player newController) {
        return !(hasKeyword("Other players can't gain control of CARDNAME.") && !getController().equals(newController));
    }

    public final boolean canBeEnchantedBy(final Card aura) {
        return canBeEnchantedBy(aura, false);
    }

    public final boolean canBeEnchantedBy(final Card aura, final boolean checkSBA) {
        SpellAbility sa = aura.getFirstAttachSpell();
        TargetRestrictions tgt = null;
        if (sa != null) {
            tgt = sa.getTargetRestrictions();
        }

        return !(hasProtectionFrom(aura, checkSBA)
                || (hasKeyword("CARDNAME can't be enchanted in the future.") && !isEnchantedBy(aura))
                || (hasKeyword("CARDNAME can't be enchanted.") && !aura.getName().equals("Anti-Magic Aura")
                && !(aura.getName().equals("Consecrate Land") && aura.isInZone(ZoneType.Battlefield)))
                || ((tgt != null) && !isValid(tgt.getValidTgts(), aura.getController(), aura, sa)));
    }

    public final boolean canBeEquippedBy(final Card equip) {
        if (equip.hasStartOfKeyword("CantEquip")) {
            final int keywordPosition = equip.getKeywordPosition("CantEquip");
            final String parse = equip.getKeywords().get(keywordPosition);
            final String[] k = parse.split(" ", 2);
            final String[] restrictions = k[1].split(",");
            if (isValid(restrictions, equip.getController(), equip, null)) {
                return false;
            }
        }
        return !(hasProtectionFrom(equip)
                || hasKeyword("CARDNAME can't be equipped.")
                || !isValid("Creature", equip.getController(), equip, null));
    }

    public FCollectionView<ReplacementEffect> getReplacementEffects() {
        return currentState.getReplacementEffects();
    }

    public void setReplacementEffects(final Iterable<ReplacementEffect> res) {
        currentState.clearReplacementEffects();
        for (final ReplacementEffect replacementEffect : res) {
            if (replacementEffect.isIntrinsic()) {
                addReplacementEffect(replacementEffect);
            }
        }
    }

    public ReplacementEffect addReplacementEffect(final ReplacementEffect replacementEffect) {
        final ReplacementEffect replacementEffectCopy = replacementEffect.getCopy(); // doubtful - every caller provides a newly parsed instance, why copy?
        replacementEffectCopy.setHostCard(this);
        currentState.addReplacementEffect(replacementEffectCopy);
        return replacementEffectCopy;
    }
    public void removeReplacementEffect(ReplacementEffect replacementEffect) {
        currentState.removeReplacementEffect(replacementEffect);
    }

    /**
     * Returns what zone this card was cast from (from what zone it was moved to the stack).
     */
    public ZoneType getCastFrom() {
        return castFrom;
    }
    public void setCastFrom(final ZoneType castFrom0) {
        castFrom = castFrom0;
    }

    public CardDamageHistory getDamageHistory() {
        return damageHistory;
    }

    public Card getEffectSource() {
        return effectSource;
    }
    public void setEffectSource(Card src) {
        effectSource = src;
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

    public void onCleanupPhase(final Player turn) {
        setDamage(0);
        setHasBeenDealtDeathtouchDamage(false);
        resetPreventNextDamage();
        resetPreventNextDamageWithEffect();
        resetReceivedDamageFromThisTurn();
        resetDealtDamageToThisTurn();
        resetDealtDamageToPlayerThisTurn();
        getDamageHistory().newTurn();
        setRegeneratedThisTurn(0);
        setBecameTargetThisTurn(false);
        clearMustAttackEntity(turn);
        clearMustBlockCards();
        getDamageHistory().setCreatureAttackedLastTurnOf(turn, getDamageHistory().getCreatureAttackedThisTurn());
        getDamageHistory().setCreatureAttackedThisTurn(false);
        getDamageHistory().setCreatureAttacksThisTurn(0);
        getDamageHistory().setCreatureBlockedThisTurn(false);
        getDamageHistory().setCreatureGotBlockedThisTurn(false);
        clearBlockedByThisTurn();
        clearBlockedThisTurn();
    }

    public boolean hasETBTrigger(final boolean drawbackOnly) {
        for (final Trigger tr : getTriggers()) {
            final Map<String, String> params = tr.getMapParams();
            if (tr.getMode() != TriggerType.ChangesZone) {
                continue;
            }

            if (!params.get("Destination").equals(ZoneType.Battlefield.toString())) {
                continue;
            }

            if (params.containsKey("ValidCard") && !params.get("ValidCard").contains("Self")) {
                continue;
            }
            if (drawbackOnly && params.containsKey("Execute")){
            	String exec = this.getSVar(params.get("Execute"));
            	if (exec.contains("AB$")) {
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

    public int getCMC() {
        return getCMC(SplitCMCMode.CurrentSideCMC);
    }

    public int getCMC(SplitCMCMode mode) {
        if (isToken() && getCopiedPermanent() == null) {
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
                    requestedCMC = getState(CardStateName.LeftSplit).getManaCost().getCMC() + xPaid;
                    break;
                case RightSplitCMC:
                    requestedCMC = getState(CardStateName.RightSplit).getManaCost().getCMC() + xPaid;
                    break;
                case CombinedCMC:
                    requestedCMC += getState(CardStateName.LeftSplit).getManaCost().getCMC();
                    requestedCMC += getState(CardStateName.RightSplit).getManaCost().getCMC();
                    requestedCMC += xPaid;
                    break;
                default:
                    System.out.println(String.format("Illegal Split Card CMC mode %s passed to getCMC!", mode.toString()));
                    break;
            }
        }
        else {
            if (currentStateName == CardStateName.Transformed) {
                // Except in the cases were we clone the back-side of a DFC.
                requestedCMC = getState(CardStateName.Original).getManaCost().getCMC();
            } else {
                requestedCMC = getManaCost().getCMC() + xPaid;
            }
        }
        return requestedCMC;
    }

    public final boolean canBeSacrificedBy(final SpellAbility source) {
        if (isImmutable()) {
            System.out.println("Trying to sacrifice immutables: " + this);
            return false;
        }
        if (!canBeSacrificed()) {
            return false;
        }
        return !(source != null && getController().isOpponentOf(source.getActivatingPlayer())
                && getController().hasKeyword("Spells and abilities your opponents control can't cause you to sacrifice permanents."));
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
        return isCommander;
    }
    public void setCommander(boolean b) {
        if (isCommander == b) { return; }
        isCommander = b;
        view.updateCommander(this);
    }

    public void setSplitStateToPlayAbility(final SpellAbility sa) {
        if (!isSplitCard()) {
            return; // just in case
        }
        // Split card support
        if (sa.isLeftSplit()) {
            setState(CardStateName.LeftSplit, true);
        } else if (sa.isRightSplit()) {
            setState(CardStateName.RightSplit, true);
        }
    }

    // Optional costs paid
    private final EnumSet<OptionalCost> costsPaid = EnumSet.noneOf(OptionalCost.class);
    public void clearOptionalCostsPaid() { costsPaid.clear(); }
    public void addOptionalCostPaid(OptionalCost cost) { costsPaid.add(cost); }
    public Iterable<OptionalCost> getOptionalCostsPaid() { return costsPaid; }
    public boolean isOptionalCostPaid(OptionalCost cost) { return costsPaid.contains(cost); }

    @Override
    public Game getGame() {
        return game;
    }

    public List<SpellAbility> getAllPossibleAbilities(final Player player, final boolean removeUnplayable) {
        // this can only be called by the Human
        final List<SpellAbility> abilities = Lists.newArrayList();
        for (SpellAbility sa : getSpellAbilities()) {
            //add alternative costs as additional spell abilities
            abilities.add(sa);
            abilities.addAll(GameActionUtil.getAlternativeCosts(sa, player));
        }
        final CardPlayOption playOption = mayPlay(player);
        if (isFaceDown() && isInZone(ZoneType.Exile) && playOption != null) {
            for (final SpellAbility sa : getState(CardStateName.Original).getSpellAbilities()) {
                abilities.addAll(GameActionUtil.getAlternativeCosts(sa, player));
            }
        }

        final Collection<SpellAbility> toRemove = Lists.newArrayListWithCapacity(abilities.size());
        for (final SpellAbility sa : abilities) {
            sa.setActivatingPlayer(player);
            if ((removeUnplayable && !sa.canPlay()) || !sa.isPossible()) {
                toRemove.add(sa);
            }
        }
        for (final SpellAbility sa : toRemove) {
            abilities.remove(sa);
        }

        if (getState(CardStateName.Original).getType().isLand() && player.canPlayLand(this)) {
            game.PLAY_LAND_SURROGATE.setHostCard(this);
            abilities.add(game.PLAY_LAND_SURROGATE);
        }

        return abilities;
    }

    public static Card fromPaperCard(IPaperCard pc, Player owner) {
        return CardFactory.getCard(pc, owner, owner == null ? null : owner.getGame());
    }

    private static final Map<PaperCard, Card> cp2card = new HashMap<>();
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

    public IPaperCard getPaperCard() {
        IPaperCard cp = paperCard;
        if (cp != null) {
            return cp;
        }

        final String name = getName();
        final String set = getSetCode();

        if (StringUtils.isNotBlank(set)) {
            cp = StaticData.instance().getVariantCards().getCard(name, set);
            return cp == null ? StaticData.instance().getCommonCards().getCard(name, set) : cp;
        }
        cp = StaticData.instance().getVariantCards().getCard(name);
        return cp == null ? StaticData.instance().getCommonCards().getCardFromEdition(name, SetPreference.Latest) : cp;
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

    //allow special cards to override this function to return another card for the sake of UI logic
    public Card getCardForUi() {
        return this;
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
    private static final class CountKeywordVisitor extends Visitor<String> {
        private String keyword;
        private int count;
        private boolean startOf;

        private CountKeywordVisitor(String keyword) {
            this.keyword = keyword;
            this.count = 0;
            this.startOf = false;
        }

        private CountKeywordVisitor(String keyword, boolean startOf) {
            this(keyword);
            this.startOf = startOf;
        }

        @Override
        public void visit(String kw) {
            if ((startOf && kw.startsWith(keyword)) || kw.equals(keyword)) {
                count++;
            }
        }

        public int getCount() {
            return count;
        }
    }

    // Collects all the keywords into a list.
    private static final class ListKeywordVisitor extends Visitor<String> {
        private List<String> keywords = new ArrayList<>();

        @Override
        public void visit(String kw) {
            keywords.add(kw);
        }

        public List<String> getKeywords() {
            return keywords;
        }
    }

    public void setChangedCardTypes(Map<Long, CardChangedType> changedCardTypes) {
        this.changedCardTypes.clear();
        for (Entry<Long, CardChangedType> entry : changedCardTypes.entrySet()) {
            this.changedCardTypes.put(entry.getKey(), entry.getValue());
        }
    }

    public void setChangedCardKeywords(Map<Long, KeywordsChange> changedCardKeywords) {
        this.changedCardKeywords.clear();
        for (Entry<Long, KeywordsChange> entry : changedCardKeywords.entrySet()) {
            this.changedCardKeywords.put(entry.getKey(), entry.getValue());
        }
    }

    public void setChangedCardColors(Map<Long, CardColor> changedCardColors) {
        this.changedCardColors.clear();
        for (Entry<Long, CardColor> entry : changedCardColors.entrySet()) {
            this.changedCardColors.put(entry.getKey(), entry.getValue());
        }
    }

    public void ceaseToExist() {
        getGame().getTriggerHandler().suppressMode(TriggerType.ChangesZone);
        getZone().remove(this);
        getGame().getTriggerHandler().clearSuppression(TriggerType.ChangesZone);
    }
}
