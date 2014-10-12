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
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import forge.GameCommand;
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
import forge.game.card.CardCollectionView;
import forge.game.card.CardPredicates.Presets;
import forge.game.card.CardView;
import forge.game.combat.AttackingBand;
import forge.game.combat.Combat;
import forge.game.cost.Cost;
import forge.game.event.*;
import forge.game.event.GameEventCardAttachment.AttachMethod;
import forge.game.event.GameEventCardDamaged.DamageType;
import forge.game.keyword.KeywordsChange;
import forge.game.player.Player;
import forge.game.replacement.ReplaceMoved;
import forge.game.replacement.ReplacementEffect;
import forge.game.replacement.ReplacementResult;
import forge.game.spellability.*;
import forge.game.staticability.StaticAbility;
import forge.game.trigger.Trigger;
import forge.game.trigger.TriggerType;
import forge.game.trigger.ZCTrigger;
import forge.game.zone.Zone;
import forge.game.zone.ZoneType;
import forge.item.IPaperCard;
import forge.item.PaperCard;
import forge.trackable.TrackableProperty;
import forge.util.CollectionSuppliers;
import forge.util.Expressions;
import forge.util.FCollectionView;
import forge.util.Lang;
import forge.util.TextUtil;
import forge.util.maps.HashMapOfLists;
import forge.util.maps.MapOfLists;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentSkipListMap;
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
public class Card extends GameEntity implements Comparable<Card>, IIdentifiable {
    private static HashMap<Integer, Card> cardCache = new HashMap<Integer, Card>();
    public static Card get(CardView cardView) {
        return cardCache.get(cardView.getId());
    }
    public static CardCollection getList(Iterable<CardView> cardViews) {
        CardCollection list = new CardCollection();
        for (CardView cv : cardViews) {
            list.add(get(cv));
        }
        return list;
    }
    public static void clearCache() {
        cardCache.clear();
    }

    private final IPaperCard paperCard;

    private final Map<CardCharacteristicName, CardCharacteristics> characteristicsMap
        = new EnumMap<CardCharacteristicName, CardCharacteristics>(CardCharacteristicName.class);
    private CardCharacteristicName curCharacteristics = CardCharacteristicName.Original;
    private CardCharacteristicName preTFDCharacteristic = CardCharacteristicName.Original;

    private ZoneType castFrom = null;

    private final CardDamageHistory damageHistory = new CardDamageHistory();
    private Map<CounterType, Integer> counters = new TreeMap<CounterType, Integer>();
    private Map<Card, Map<CounterType, Integer>> countersAddedBy = new TreeMap<Card, Map<CounterType, Integer>>();
    private ArrayList<String> extrinsicKeyword = new ArrayList<String>();
    // Hidden keywords won't be displayed on the card
    private final CopyOnWriteArrayList<String> hiddenExtrinsicKeyword = new CopyOnWriteArrayList<String>();

    // cards attached or otherwise linked to this card
    private CardCollection equippedBy, fortifiedBy, hauntedBy, devouredCards, imprintedCards, encodedCards;
    private CardCollection mustBlockCards, clones, gainControlTargets, chosenCards, blockedThisTurn, blockedByThisTurn;

    // if this card is attached or linked to something, what card is it currently attached to
    private Card equipping, fortifying, cloneOrigin, haunting, effectSource, pairedWith;

    // which auras enchanted this card?
    // if this card is an Aura, what Entity is it enchanting?
    private GameEntity enchanting = null;

    // changes by AF animate and continuous static effects - timestamp is the key of maps
    private Map<Long, CardChangedType> changedCardTypes = new ConcurrentSkipListMap<Long, CardChangedType>();
    private Map<Long, KeywordsChange> changedCardKeywords = new ConcurrentSkipListMap<Long, KeywordsChange>();

    // changes that say "replace each instance of one [color,type] by another - timestamp is the key of maps
    private final CardChangedWords changedTextColors = new CardChangedWords();
    private final CardChangedWords changedTextTypes = new CardChangedWords();
    /** List of the keywords that have been added by text changes. */
    private final List<String> keywordsGrantedByTextChanges = Lists.newArrayList();
    /** Original values of SVars changed by text changes. */
    private Map<String, String> originalSVars = Maps.newHashMap();

    private final Set<Object> rememberedObjects = new LinkedHashSet<Object>();
    private final MapOfLists<GameEntity, Object> rememberMap = new HashMapOfLists<GameEntity, Object>(CollectionSuppliers.<Object>arrayLists());
    private Map<Player, String> flipResult;

    private Map<Card, Integer> receivedDamageFromThisTurn = new TreeMap<Card, Integer>();
    private Map<Card, Integer> dealtDamageToThisTurn = new TreeMap<Card, Integer>();
    private Map<String, Integer> dealtDamageToPlayerThisTurn = new TreeMap<String, Integer>();
    private final Map<Card, Integer> assignedDamageMap = new TreeMap<Card, Integer>();

    private boolean isCommander = false;
    private boolean startsGameInPlay = false;
    private boolean drawnThisTurn = false;
    private boolean becameTargetThisTurn = false;
    private boolean startedTheTurnUntapped = false;
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

    private long bestowTimestamp = -1;
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
    private ArrayList<CardPowerToughness> newPT = new ArrayList<CardPowerToughness>();
    private int baseLoyalty = 0;
    private String baseAttackString = null;
    private String baseDefenseString = null;

    private int damage;

    // regeneration
    private List<CardShields> shields = new ArrayList<CardShields>();
    private int regeneratedThisTurn = 0;

    private int turnInZone;

    private int tempAttackBoost = 0;
    private int tempDefenseBoost = 0;

    private int semiPermanentAttackBoost = 0;
    private int semiPermanentDefenseBoost = 0;

    private int xManaCostPaid = 0;
    private Map<String, Integer> xManaCostPaidByColor;

    private int sunburstValue = 0;
    private byte colorsPaid = 0;

    private Player owner = null;
    private Player controller = null;
    private long controllerTimestamp = 0;
    private TreeMap<Long, Player> tempControllers = new TreeMap<Long, Player>();
    private final Set<Player> mayLookAt = Sets.newHashSet();

    private String originalText = "", text = "";
    private String echoCost = "";
    private Cost miracleCost = null;
    private String chosenType = "";
    private List<String> chosenColors;
    private String namedCard = "";
    private int chosenNumber;
    private Player chosenPlayer;
    private Direction chosenDirection = null;

    private final List<AbilityTriggered> zcTriggers = new ArrayList<AbilityTriggered>();
    private final List<GameCommand> untapCommandList = new ArrayList<GameCommand>();
    private final List<GameCommand> changeControllerCommandList = new ArrayList<GameCommand>();
    private final List<Object[]> staticCommandList = new ArrayList<Object[]>();

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

    /**
     * Instantiates a new card not associated to any paper card.
     * @param id the unique id of the new card.
     */
    public Card(final int id0) {
        this(id0, null);
    }

    /**
     * Instantiates a new card with a given paper card.
     * @param id the unique id of the new card.
     * @param paperCard the {@link IPaperCard} of which the new card is a
     * representation, or {@code null} if this new {@link Card} doesn't represent any paper
     * card.
     * @see IPaperCard
     */
    public Card(final int id0, final IPaperCard paperCard0) {
        super(id0);

        if (id0 >= 0) {
            cardCache.put(id0, this);
        }
        paperCard = paperCard0;
        view = new CardView(id0);
        characteristicsMap.put(CardCharacteristicName.Original, new CardCharacteristics(view.getOriginal()));
        characteristicsMap.put(CardCharacteristicName.FaceDown, CardUtil.getFaceDownCharacteristic(this));
        view.updateChangedColorWords(this);
        view.updateChangedTypes(this);
        view.updateSickness(this);
    }

    public boolean changeToState(final CardCharacteristicName state) {
        CardCharacteristicName cur = curCharacteristics;

        if (!setState(state, true)) {
            return false;
        }

        if ((cur == CardCharacteristicName.Original && state == CardCharacteristicName.Transformed)
                || (cur == CardCharacteristicName.Transformed && state == CardCharacteristicName.Original)) {

            // Clear old dfc trigger from the trigger handler
            getGame().getTriggerHandler().clearInstrinsicActiveTriggers(this);
            getGame().getTriggerHandler().registerActiveTrigger(this, false);
            HashMap<String, Object> runParams = new HashMap<String, Object>();
            runParams.put("Transformer", this);
            getGame().getTriggerHandler().runTrigger(TriggerType.Transformed, runParams, false);
        }

        return true;
    }

    public boolean setState(final CardCharacteristicName state, boolean updateView) {
        if (state == CardCharacteristicName.FaceDown && isDoubleFaced()) {
            return false; // Doublefaced cards can't be turned face-down.
        }

        if (!characteristicsMap.containsKey(state)) {
            System.out.println(getName() + " tried to switch to non-existant state \"" + state + "\"!");
            return false; // Nonexistant state.
        }

        if (state.equals(curCharacteristics)) {
            return false;
        }

        curCharacteristics = state;

        if (updateView) {
            view.updateState(this, true);
    
            Game game = getGame();
            if (game != null) {
                game.fireEvent(new GameEventCardStatsChanged(this)); //ensure stats updated for new characteristics
            }
        }

        return true;
    }

    public Set<CardCharacteristicName> getStates() {
        return characteristicsMap.keySet();
    }

    public CardCharacteristicName getCurState() {
        return curCharacteristics;
    }

    public void switchStates(final CardCharacteristicName from, final CardCharacteristicName to, boolean updateView) {
        final CardCharacteristics tmp = characteristicsMap.get(from);
        characteristicsMap.put(from, characteristicsMap.get(to));
        characteristicsMap.put(to, tmp);
        if (updateView) {
            view.updateState(this, false);
        }
    }

    public void clearStates(final CardCharacteristicName state, boolean updateView) {
        if (characteristicsMap.remove(state) != null && updateView) {
            view.updateState(this, false);
        }
    }

    public void updateStateForView() {
        view.updateState(this, false);
    }

    public void setPreFaceDownCharacteristic(CardCharacteristicName preCharacteristic) {
        preTFDCharacteristic = preCharacteristic;
    }

    public boolean turnFaceDown() {
        if (!isDoubleFaced()) {
            preTFDCharacteristic = curCharacteristics;
            return setState(CardCharacteristicName.FaceDown, true);
        }
        return false;
    }

    public boolean turnFaceUp() {
        if (curCharacteristics == CardCharacteristicName.FaceDown) {
            boolean result = setState(preTFDCharacteristic, true);
            if (result) {
                getGame().getTriggerHandler().registerActiveTrigger(this, false);
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

    public CardCharacteristics getState(final CardCharacteristicName state) {
        return characteristicsMap.get(state);
    }

    public CardCharacteristics getCharacteristics() {
        return characteristicsMap.get(curCharacteristics);
    }

    public final void addAlternateState(final CardCharacteristicName state, final boolean updateView) {
        characteristicsMap.put(state, new CardCharacteristics(view.createAlternateState()));
        if (updateView) {
            view.updateState(this, false);
        }
    }

    public void updateAttackingForView() {
        view.updateAttacking(this);
    }
    public void updateBlockingForView() {
        view.updateBlocking(this);
    }

    @Override
    public final String getName() {
        return getCharacteristics().getName();
    }

    @Override
    public final void setName(final String name0) {
        getCharacteristics().setName(name0);
    }

    public final boolean isInAlternateState() {
        return curCharacteristics != CardCharacteristicName.Original
            && curCharacteristics != CardCharacteristicName.Cloned;
    }

    public final boolean hasAlternateState() {
        return characteristicsMap.keySet().size() > 2;
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

    public boolean isCloned() {
        return characteristicsMap.containsKey(CardCharacteristicName.Cloner);
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
        return CardCollection.hasCard(imprintedCards);
    }
    public final boolean hasImprintedCard(Card c) {
        return CardCollection.hasCard(imprintedCards, c);
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
        return CardCollection.hasCard(encodedCards);
    }
    public final boolean hasEncodedCard(Card c) {
        return CardCollection.hasCard(encodedCards, c);
    }
    public final void addEncodedCard(final Card c) {
        if (encodedCards == null) {
            encodedCards = new CardCollection();
        }
        encodedCards.add(c);
    }
    public final void addEncodedCards(final Iterable<Card> cards) {
        if (encodedCards == null) {
            encodedCards = new CardCollection();
        }
        encodedCards.addAll(cards);
    }
    public final void removeEncodedCard(final Card c) {
        if (encodedCards.remove(c)) {
            if (encodedCards.isEmpty()) {
                encodedCards = null;
            }
        }
    }
    public final void clearEncodedCards() {
        encodedCards = null;
    }

    public final String getFlipResult(final Player flipper) {
        if (flipResult == null) {
            return null;
        }
        return flipResult.get(flipper);
    }
    public final void addFlipResult(final Player flipper, final String result) {
        if (flipResult == null) {
            flipResult = new TreeMap<Player, String>();
        }
        flipResult.put(flipper, result);
    }
    public final void clearFlipResult() {
        flipResult = null;
    }

    public final List<Trigger> getTriggers() {
        return getCharacteristics().getTriggers();
    }
    public final void setTriggers(final List<Trigger> trigs, boolean intrinsicOnly) {
        final List<Trigger> copyList = new CopyOnWriteArrayList<Trigger>();
        for (final Trigger t : trigs) {
            if (!intrinsicOnly || t.isIntrinsic()) {
                copyList.add(t.getCopyForHostCard(this));
            }
        }
        getCharacteristics().setTriggers(copyList);
    }
    public final Trigger addTrigger(final Trigger t) {
        final Trigger newtrig = t.getCopyForHostCard(this);
        getCharacteristics().getTriggers().add(newtrig);
        return newtrig;
    }
    public final void moveTrigger(final Trigger t) {
        t.setHostCard(this);
        if (!getCharacteristics().getTriggers().contains(t)) {
            getCharacteristics().getTriggers().add(t);
        }
    }
    public final void removeTrigger(final Trigger t) {
        getCharacteristics().getTriggers().remove(t);
    }
    public final void removeTrigger(final Trigger t, final CardCharacteristicName state) {
        CardCharacteristics stateCharacteristics = getState(state);
        stateCharacteristics.getTriggers().remove(t);
    }
    public final void clearTriggersNew() {
        getCharacteristics().getTriggers().clear();
    }

    public final Object getTriggeringObject(final String typeIn) {
        Object triggered = null;
        if (!getCharacteristics().getTriggers().isEmpty()) {
            for (final Trigger t : getCharacteristics().getTriggers()) {
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
        colorsPaid = s; // TODO: Append colors instead of replacing
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

    private void addCounter(final CounterType counterType, final int n, final boolean applyMultiplier, final boolean fireEvents) {
        int addAmount = n;
        final HashMap<String, Object> repParams = new HashMap<String, Object>();
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
            final Integer newValue = addAmount + (oldValue == null ? 0 : oldValue.intValue());

            if (newValue != oldValue) {
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
                getGame().fireEvent(new GameEventCardCounters(this, counterType, oldValue == null ? 0 : oldValue.intValue(), newValue));
            }
        }

        // Run triggers
        final Map<String, Object> runParams = new TreeMap<String, Object>();
        runParams.put("Card", this);
        runParams.put("CounterType", counterType);
        for (int i = 0; i < addAmount; i++) {
            getGame().getTriggerHandler().runTrigger(TriggerType.CounterAdded, runParams, false);
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
        final Map<CounterType, Integer> counterMap = new TreeMap<CounterType, Integer>();
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
        int newValue = oldValue == null ? 0 : Math.max(oldValue.intValue() - n, 0);

        final int delta = (oldValue == null ? 0 : oldValue.intValue()) - newValue;
        if (delta == 0) { return; }

        int powerBonusBefore = getPowerBonusFromCounters();
        int toughnessBonusBefore = getToughnessBonusFromCounters();
        int loyaltyBefore = getCurrentLoyalty();

        if (newValue > 0) {
            counters.put(counterName, Integer.valueOf(newValue));
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
        getGame().fireEvent(new GameEventCardCounters(this, counterName, oldValue == null ? 0 : oldValue.intValue(), newValue));

        // Run triggers
        int curCounters = oldValue == null ? 0 : oldValue.intValue();
        for (int i = 0; i < delta && curCounters != 0; i++) {
            final Map<String, Object> runParams = new TreeMap<String, Object>();
            runParams.put("Card", this);
            runParams.put("CounterType", counterName);
            runParams.put("NewCounterAmount", --curCounters);
            getGame().getTriggerHandler().runTrigger(TriggerType.CounterRemoved, runParams, false);
        }
    }

    public final int getCounters(final CounterType counterName) {
        Integer value = counters.get(counterName);
        return value == null ? 0 : value.intValue();
    }

    // get all counters from a card
    public final Map<CounterType, Integer> getCounters() {
        return counters;
    }

    public final boolean hasCounters() {
        return !counters.isEmpty();
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
        return getCharacteristics().getSVar(var);
    }

    public final boolean hasSVar(final String var) {
        return getCharacteristics().hasSVar(var);
    }

    public final void setSVar(final String var, final String str) {
        getCharacteristics().setSVar(var, str);
    }

    public final Map<String, String> getSVars() {
        return getCharacteristics().getSVars();
    }

    public final void setSVars(final Map<String, String> newSVars) {
        getCharacteristics().setSVars(newSVars);
    }

    public final int sumAllCounters() {
        int count = 0;
        for (final Integer value2 : counters.values()) {
            count += value2.intValue();
        }
        return count;
    }

    public final int getTurnInZone() {
        return turnInZone;
    }

    public final void setTurnInZone(final int turn) {
        turnInZone = turn;
    }

    public final void setEchoCost(final String s) {
        echoCost = s;
    }

    public final String getEchoCost() {
        return echoCost;
    }

    public final void setManaCost(final ManaCost s) {
        getCharacteristics().setManaCost(s);
    }

    public final ManaCost getManaCost() {
        return getCharacteristics().getManaCost();
    }

    public final void addColor(String s) {
        if (s.equals("")) {
            s = "0";
        }
        ManaCost mc = new ManaCost(new ManaCostParser(s));
        getCharacteristics().getCardColor().add(new CardColor(mc.getColorProfile()));
    }

    public final long addColor(final String s, final boolean addToColors, final boolean bIncrease) {
        if (bIncrease) {
            CardColor.increaseTimestamp();
        }
        getCharacteristics().getCardColor().add(new CardColor(s, addToColors));
        return CardColor.getTimestamp();
    }

    public final void removeColor(final String s, final Card c, final boolean addTo, final long timestampIn) {
        CardColor removeCol = null;
        for (final CardColor cc : getCharacteristics().getCardColor()) {
            if (cc.equals(s, c, addTo, timestampIn)) {
                removeCol = cc;
            }
        }

        if (removeCol != null) {
            getCharacteristics().getCardColor().remove(removeCol);
        }
    }

    public final void setColor(final Iterable<CardColor> colors) {
        getCharacteristics().setCardColor(colors);
    }

    public final Iterable<CardColor> getColor() {
        return getCharacteristics().getCardColor();
    }

    public final ColorSet determineColor() {
        if (isImmutable()) {
            return ColorSet.getNullColor();
        }

        return getCharacteristics().determineColor();
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
            return new ArrayList<String>();
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
        chosenCards = view.setCards(chosenCards, cards, TrackableProperty.ChosenColors);
    }
    public boolean hasChosenCard() {
        return CardCollection.hasCard(chosenCards);
    }
    public boolean hasChosenCard(Card c) {
        return CardCollection.hasCard(chosenCards, c);
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
        return CardCollection.hasCard(gainControlTargets);
    }
    public final boolean hasGainControlTarget(Card c) {
        return CardCollection.hasCard(gainControlTargets, c);
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
        final ArrayList<String> keyword = getHiddenExtrinsicKeyword();

        sb.append(keywordsToText(keyword));

        return sb.toString();
    }

    // convert a keyword list to the String that should be displayed ingame
    public final String keywordsToText(final ArrayList<String> keywords) {
        final StringBuilder sb = new StringBuilder();
        final StringBuilder sbLong = new StringBuilder();

        // Prepare text changes
        final Map<String, String> changedColorWords = getChangedTextColorWords(),
                changedTypes = getChangedTextTypeWords();
        final Set<Entry<String, String>> textChanges = Sets.union(
                changedColorWords.entrySet(), changedTypes.entrySet());

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
                    if ("1" != numCounters) {
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
                sbLong.append(parts[0] + " " + ManaCostParser.parse(parts[1])).append("\r\n");
            } else if (keyword.startsWith("Devour")) {
                final String[] parts = keyword.split(":");
                final String extra = parts.length > 2 ? parts[2] : "";
                final String devour = "Devour " + parts[1] + extra;
                sbLong.append(devour).append("\r\n");
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
                sbLong.append("As an additional cost to cast " + getName() + ", " + cost1.toSimpleString()
                        + " or pay " + cost2.toSimpleString() + ".\r\n");
            } else if (keyword.startsWith("Kicker")) {
                final Cost cost = new Cost(keyword.substring(7), false);
                sbLong.append("Kicker " + cost.toSimpleString() + "\r\n");
            } else if (keyword.endsWith(".") && !keyword.startsWith("Haunt")) {
                sbLong.append(keyword.toString()).append("\r\n");
            } else if (keyword.contains("At the beginning of your upkeep, ")
                    && keyword.contains(" unless you pay")) {
                sbLong.append(keyword.toString()).append("\r\n");
            } else if (keyword.startsWith("Modular") || keyword.startsWith("Soulshift") || keyword.startsWith("Bloodthirst")
                    || keyword.startsWith("ETBReplacement") || keyword.startsWith("MayEffectFromOpeningHand")) {
                continue;
            } else if (keyword.startsWith("Provoke")) {
                sbLong.append(keyword);
                sbLong.append(" (When this attacks, you may have target creature ");
                sbLong.append("defending player controls untap and block it if able.)");
            } else if (keyword.contains("Haunt")) {
                sb.append("\r\nHaunt (");
                if (isCreature()) {
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
                sb.append("Convoke (Each creature you tap while casting this spell pays for {1} or one mana of that creature's color.)");
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
                sbLong.append(keyword);
                sbLong.append(" (You may pair this creature ");
                sbLong.append("with another unpaired creature when either ");
                sbLong.append("enters the battlefield. They remain paired for ");
                sbLong.append("as long as you control both of them)");
            } else if (keyword.startsWith("Equip") || keyword.startsWith("Fortify") || keyword.startsWith("Outlast")) {
                // keyword parsing takes care of adding a proper description
                continue;
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

    private String getTextForKwCantBeBlockedByAmount(String keyword) {
        String restriction = keyword.split(" ", 2)[1];
        boolean isLT = "LT".equals(restriction.substring(0,2));
        final String byClause = isLT ? "except by " : "by more than ";
        int cnt = Integer.parseInt(restriction.substring(2));
        return byClause + Lang.nounWithNumeral(cnt, isLT ? "or more creature" : "creature");
    }

    private String getTextForKwCantBeBlockedByType(String keyword) {
        boolean negative = true;
        List<String> subs = Lists.newArrayList(TextUtil.split(keyword.split(" ", 2)[1], ','));
        List<List<String>> subsAnd = Lists.newArrayList();
        List<String> orClauses = new ArrayList<String>();
        for (int iOr = 0; iOr < subs.size(); iOr++) {
            String expession = subs.get(iOr);
            List<String> parts = Lists.newArrayList(expession.split("[.+]"));
            for (int p = 0; p < parts.size(); p++) {
                String part = parts.get(p);
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
                boolean useNon = inp.getKey().booleanValue() == allNegative;
                return (useNon ? "*NO* " : "") + inp.getRight();
            }
        };

        for (int iOr = 0; iOr < subsAnd.size(); iOr++) {
            List<String> andOperands = subsAnd.get(iOr);
            List<Pair<Boolean, String>> prependedAdjectives = Lists.newArrayList();
            List<Pair<Boolean, String>> postponedAdjectives = Lists.newArrayList();
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
                } else if (forge.card.CardType.isACreatureType(part)) {
                    creatures = StringUtils.capitalize(Lang.getPlural(part)) + (creatures == null ? "" : " or " + creatures);
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

                    boolean useNon = pre.getKey().booleanValue() == allNegative;
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
                    boolean useNon = pre.getKey().booleanValue() == allNegative;
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
        if (isInstant() || isSorcery()) {
            final StringBuilder sb = abilityTextInstantSorcery();

            if (haunting != null) {
                sb.append("Haunting: ").append(haunting);
                sb.append("\r\n");
            }

            while (sb.toString().endsWith("\r\n")) {
                sb.delete(sb.lastIndexOf("\r\n"), sb.lastIndexOf("\r\n") + 3);
            }

            return sb.toString().replaceAll("CARDNAME", getName());
        }

        final StringBuilder sb = new StringBuilder();
        final ArrayList<String> keyword = getUnhiddenKeyword();

        if (monstrous) {
            sb.append("Monstrous\r\n");
        }
        sb.append(keywordsToText(keyword));

        // Give spellText line breaks for easier reading
        sb.append("\r\n");
        sb.append(text.replaceAll("\\\\r\\\\n", "\r\n"));
        sb.append("\r\n");

        // Triggered abilities
        for (final Trigger trig : getCharacteristics().getTriggers()) {
            if (!trig.isSecondary()) {
                sb.append(trig.toString() + "\r\n");
            }
        }

        // Replacement effects
        for (final ReplacementEffect replacementEffect : getCharacteristics().getReplacementEffects()) {
            if (!replacementEffect.isSecondary()) {
                sb.append(replacementEffect.toString() + "\r\n");
            }
        }

        // static abilities
        for (final StaticAbility stAb : getCharacteristics().getStaticAbilities()) {
            sb.append(stAb.toString() + "\r\n");
        }

        final ArrayList<String> addedManaStrings = new ArrayList<String>();
        boolean primaryCost = true;
        for (final SpellAbility sa : getSpellAbilities()) {
            // only add abilities not Spell portions of cards
            if (sa == null || !isPermanent()) {
                continue;
            }

            if ((sa instanceof SpellPermanent) && primaryCost && !isAura()) {
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

            if ((sa instanceof SpellPermanent) && !isAura()) {
                sb.insert(0, "\r\n");
                sb.insert(0, sAbility);
            } else if (!sAbility.endsWith(getName())) {
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

        return sb.toString().replaceAll("CARDNAME", getName()).trim();
    } // getText()

    private StringBuilder abilityTextInstantSorcery() {
        final String s = getSpellText();
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

        // I think SpellAbilities should be displayed after Keywords
        // Add SpellAbilities
        for (final SpellAbility element : getSpellAbilities()) {
            String elementText = element.toString();

            //Determine if a card has multiple choices, then format it in an easier to read list.
            if (element.getApi() != null && element.getApi().equals(ApiType.Charm)) {

                String chooseText = elementText.split("-")[0].trim();
                String[] splitElemText = elementText.split("-");
                String[] choices = splitElemText.length > 1 ? splitElemText[1].split(";") : null;

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

        }

        // Add Keywords
        final List<String> kw = getKeyword();

        // Triggered abilities
        for (final Trigger trig : getCharacteristics().getTriggers()) {
            if (!trig.isSecondary()) {
                sb.append(trig.toString() + "\r\n");
            }
        }

        // Replacement effects
        for (final ReplacementEffect replacementEffect : getCharacteristics().getReplacementEffects()) {
            sb.append(replacementEffect.toString() + "\r\n");
        }

        // static abilities
        for (final StaticAbility stAb : getCharacteristics().getStaticAbilities()) {
            final String stAbD = stAb.toString();
            if (!stAbD.equals("")) {
                sb.append(stAbD + "\r\n");
            }
        }

        // keyword descriptions
        for (int i = 0; i < kw.size(); i++) {
            final String keyword = kw.get(i);
            if ((keyword.startsWith("Ripple") && !sb.toString().contains("Ripple"))
                    || (keyword.startsWith("Dredge") && !sb.toString().contains("Dredge"))
                    || (keyword.startsWith("CARDNAME is ") && !sb.toString().contains("CARDNAME is "))) {
                sb.append(keyword.replace(":", " ")).append("\r\n");
            }
            else if ((keyword.startsWith("Madness") && !sb.toString().contains("Madness"))
                    || (keyword.startsWith("Recover") && !sb.toString().contains("Recover"))
                    || (keyword.startsWith("Miracle") && !sb.toString().contains("Miracle"))) {
                String[] parts = keyword.split(":");
                sb.append(parts[0] + " " + ManaCostParser.parse(parts[1])).append("\r\n");
            }
            else if (keyword.equals("CARDNAME can't be countered.")
                    || keyword.startsWith("May be played") || keyword.startsWith("Conspire")
                    || keyword.startsWith("Cascade") || keyword.startsWith("Wither")
                    || (keyword.startsWith("Epic") && !sb.toString().contains("Epic"))
                    || (keyword.startsWith("Split second") && !sb.toString().contains("Split second"))
                    || (keyword.startsWith("Multikicker") && !sb.toString().contains("Multikicker"))) {
                sb.append(keyword).append("\r\n");
            }
            else if (keyword.equals("You may cast CARDNAME any time you could cast an instant if you pay 2 more to cast it.")) {
                sb.append(keyword).append("\r\n");
            }
            else if (keyword.startsWith("Flashback")) {
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
                sb.append("Buyback ").append(cost.toSimpleString());
                sb.append(" (You may pay an additional cost as you cast CARDNAME. If you do, put CARDNAME back into your hand as it resolves.)");
                sb.append("\r\n");
            } else if (keyword.startsWith("Entwine")) {
                final Cost cost = new Cost(keyword.substring(8), false);
                sb.append("Entwine ").append(cost.toSimpleString());
                sb.append(" (Choose both if you pay the entwine cost.)");
                sb.append("\r\n");
            } else if (keyword.startsWith("Kicker")) {
                final Cost cost = new Cost(keyword.substring(7), false);
                sb.append("Kicker " + cost.toSimpleString() + "\r\n");
            } else if (keyword.startsWith("AlternateAdditionalCost")) {
                final String costString1 = keyword.split(":")[1];
                final String costString2 = keyword.split(":")[2];
                final Cost cost1 = new Cost(costString1, false);
                final Cost cost2 = new Cost(costString2, false);
                sb.append("As an additional cost to cast " + getName() + ", " + cost1.toSimpleString()
                        + " or pay " + cost2.toSimpleString() + ".\r\n");
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
                if (isCreature()) {
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

    public final List<SpellAbility> getManaAbility() {
        return Collections.unmodifiableList(getCharacteristics().getManaAbility());
    }

    public final boolean canProduceSameManaTypeWith(final Card c) {
        final List<SpellAbility> manaAb = getManaAbility();
        if (manaAb.isEmpty()) {
            return false;
        }
        Set<String> colors = new HashSet<String>();
        for (final SpellAbility ab : c.getManaAbility()) {
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
        for (int i = 0; i < getCharacteristics().getSpellAbility().size(); i++) {
            if (getCharacteristics().getSpellAbility().get(i).isSpell()) {
                getCharacteristics().getSpellAbility().remove(i);
                return;
            }
        }
    }

    public final SpellAbility getFirstSpellAbility() {
        final List<SpellAbility> sas = getCharacteristics().getSpellAbility();
        return sas.isEmpty() ? null : sas.get(0);
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
        for (final SpellAbility sa : getCharacteristics().getSpellAbility()) {
            if (sa instanceof SpellPermanent) {
                return (SpellPermanent) sa;
            }
        }
        return null;
    }

    public final void addSpellAbility(final SpellAbility a) {
        a.setHostCard(this);
        if (a.isManaAbility()) {
            getCharacteristics().getManaAbility().add(a);
        } else {
            getCharacteristics().getSpellAbility().add(a);
        }
    }

    public final void removeSpellAbility(final SpellAbility a) {
        if (a.isManaAbility()) {
            // if (a.isExtrinsic()) //never remove intrinsic mana abilities, is
            // this the way to go??
            getCharacteristics().getManaAbility().remove(a);
        } else {
            getCharacteristics().getSpellAbility().remove(a);
        }
    }

    public final List<SpellAbility> getSpellAbilities() {
        final ArrayList<SpellAbility> res = new ArrayList<SpellAbility>(getManaAbility());
        res.addAll(getCharacteristics().getSpellAbility());
        return res;
    }

    public final List<SpellAbility> getNonManaSpellAbilities() {
        return getCharacteristics().getSpellAbility();
    }

    public final ArrayList<SpellAbility> getAllSpellAbilities() {
        final ArrayList<SpellAbility> res = new ArrayList<SpellAbility>();

        for (final CardCharacteristicName key : characteristicsMap.keySet()) {
            res.addAll(getState(key).getSpellAbility());
            res.addAll(getState(key).getManaAbility());
        }

        return res;
    }

    public final List<SpellAbility> getSpells() {
        final List<SpellAbility> res = new ArrayList<SpellAbility>();
        for (final SpellAbility sa : getCharacteristics().getSpellAbility()) {
            if (!sa.isSpell()) {
                continue;
            }
            res.add(sa);
        }
        return res;
    }

    public final List<SpellAbility> getBasicSpells() {
        final ArrayList<SpellAbility> res = new ArrayList<SpellAbility>();

        for (final SpellAbility sa : getCharacteristics().getSpellAbility()) {
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
        copiedPermanent = c;
    }

    public final boolean isCopiedSpell() {
        return copiedSpell;
    }
    public final void setCopiedSpell(final boolean b) {
        copiedSpell = b;
    }

    public final boolean isFaceDown() {
        return curCharacteristics == CardCharacteristicName.FaceDown;
    }

    public final void setCanCounter(final boolean b) {
        canCounter = b;
    }

    public final boolean getCanCounter() {
        return canCounter;
    }

    public final void addTrigger(final GameCommand c, final ZCTrigger typeIn) {
        zcTriggers.add(new AbilityTriggered(this, c, typeIn));
    }

    public final void removeTrigger(final GameCommand c, final ZCTrigger typeIn) {
        zcTriggers.remove(new AbilityTriggered(this, c, typeIn));
    }

    public final void executeTrigger(final ZCTrigger type) {
        for (final AbilityTriggered t : zcTriggers) {
            if (t.getTrigger().equals(type) && t.isBasic()) {
                t.run();
            }
        }
    }

    public final void clearTriggers() {
        zcTriggers.clear();
    }

    public final void addComesIntoPlayCommand(final GameCommand c) {
        addTrigger(c, ZCTrigger.ENTERFIELD);
    }

    public final void addDestroyCommand(final GameCommand c) {
        addTrigger(c, ZCTrigger.DESTROY);
    }

    public final void addLeavesPlayCommand(final GameCommand c) {
        addTrigger(c, ZCTrigger.LEAVEFIELD);
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

    public final Player getOwner() {
        return owner;
    }
    public final void setOwner(final Player owner0) {
        if (owner == owner0) { return; }
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

    public final void setMayLookAt(final Player player, final boolean mayLookAt0) {
        if (mayLookAt0) {
            mayLookAt.add(player);
        }
        else {
            mayLookAt.remove(player);
        }
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
        return CardCollection.hasCard(equippedBy);
    }
    public final boolean isEquippedBy(Card c) {
        return CardCollection.hasCard(equippedBy, c);
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
        return CardCollection.hasCard(fortifiedBy);
    }
    public final boolean isFortifiedBy(Card c) {
        return CardCollection.hasCard(fortifiedBy, c);
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
            final String parse = getKeyword().get(keywordPosition).toString();
            final String[] k = parse.split(" ", 2);
            final String[] restrictions = k[1].split(",");
            if (c.isValid(restrictions, getController(), this)) {
                getGame().getGameLog().add(GameLogEntryType.STACK_RESOLVE, "Trying to equip " + c.getName() + " but it can't be equipped.");
                return;
            }
        }

        Card oldTarget = null;
        if (isEquipping()) {
            oldTarget = equipping;
            unEquipCard(oldTarget);
        }

        // They use double links... it's doubtful
        setEquipping(c);
        setTimestamp(getGame().getNextTimestamp());
        c.equippedBy = c.view.addCard(c.equippedBy, this, TrackableProperty.EquippedBy);

        // Play the Equip sound
        getGame().fireEvent(new GameEventCardAttachment(this, oldTarget, c, AttachMethod.Equip));

        // run trigger
        final HashMap<String, Object> runParams = new HashMap<String, Object>();
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
        final HashMap<String, Object> runParams = new HashMap<String, Object>();
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
        final Map<String, Object> runParams = new TreeMap<String, Object>();
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
        final HashMap<String, Object> runParams = new HashMap<String, Object>();
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
        getCharacteristics().setType(type0);
    }

    public final void addType(final String type0) {
        getCharacteristics().addType(type0);
    }

    public final CardTypeView getType() {
        if (changedCardTypes.isEmpty()) {
            return getCharacteristics().getType();
        }
        //return view's type since that already has cached changes from changedCardTypes
        return getCharacteristics().getView().getType();
    }

    public Map<Long, CardChangedType> getChangedCardTypes() {
        return changedCardTypes;
    }

    public final void addChangedCardTypes(final CardType addType, final CardType removeType,
            final boolean removeSuperTypes, final boolean removeCardTypes, final boolean removeSubTypes,
            final boolean removeCreatureTypes, final long timestamp) {

        changedCardTypes.put(timestamp, new CardChangedType(addType, removeType, removeSuperTypes, removeCardTypes, removeSubTypes, removeCreatureTypes));
        view.getOriginal().updateType(getCharacteristics());
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
        changedCardTypes.remove(Long.valueOf(timestamp));
        view.getOriginal().updateType(getCharacteristics());
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
        baseLoyalty = n;
    }

    // values that are printed on card
    public final int getBaseAttack() {
        return getCharacteristics().getBaseAttack();
    }

    public final int getBaseDefense() {
        return getCharacteristics().getBaseDefense();
    }

    // values that are printed on card
    public final void setBaseAttack(final int n) {
        getCharacteristics().setBaseAttack(n);
    }

    public final void setBaseDefense(final int n) {
        getCharacteristics().setBaseDefense(n);
    }

    // values that are printed on card
    public final String getBaseAttackString() {
        return (null == baseAttackString) ? "" + getBaseAttack() : baseAttackString;
    }

    public final String getBaseDefenseString() {
        return (null == baseDefenseString) ? "" + getBaseDefense() : baseDefenseString;
    }

    // values that are printed on card
    public final void setBaseAttackString(final String s) {
        baseAttackString = s;
    }

    public final void setBaseDefenseString(final String s) {
        baseDefenseString = s;
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
    private final synchronized Pair<Integer, Integer> getLatestPT() {
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
    }

    public final void removeNewPT(final long timestamp) {
        for (int i = 0; i < newPT.size(); i++) {
            final CardPowerToughness cardPT = newPT.get(i);
            if (cardPT.getTimestamp() == timestamp) {
                newPT.remove(cardPT);
            }
        }
    }

    public final int getCurrentPower() {
        int total = getBaseAttack();
        final int setPower = getSetPower();
        if (setPower != -1) {
            total = setPower;
        }

        return total;
    }

    public final int getUnswitchedPower() {
        int total = getCurrentPower();

        total += getTempAttackBoost() + getSemiPermanentAttackBoost() + getPowerBonusFromCounters();
        return total;
    }

    public final int getPowerBonusFromCounters() {
        int total = 0;

        total += getCounters(CounterType.P1P1) + getCounters(CounterType.P1P2) + getCounters(CounterType.P1P0)
                - getCounters(CounterType.M1M1) + 2 * getCounters(CounterType.P2P2) - 2 * getCounters(CounterType.M2M1)
                - 2 * getCounters(CounterType.M2M2) - getCounters(CounterType.M1M0) + 2 * getCounters(CounterType.P2P0);
        return total;
    }

    public final int getNetAttack() {
        if (getAmountOfKeyword("CARDNAME's power and toughness are switched") % 2 != 0) {
            return getUnswitchedToughness();
        }
        return getUnswitchedPower();
    }

    public final int getCurrentToughness() {
        int total = getBaseDefense();

        final int setToughness = getSetToughness();
        if (setToughness != -1) {
            total = setToughness;
        }

        return total;
    }

    public final int getUnswitchedToughness() {
        int total = getCurrentToughness();

        total += getTempDefenseBoost() + getSemiPermanentDefenseBoost() + getToughnessBonusFromCounters();
        return total;
    }

    public final int getToughnessBonusFromCounters() {
        int total = 0;

        total += getCounters(CounterType.P1P1) + 2 * getCounters(CounterType.P1P2) - getCounters(CounterType.M1M1)
                + getCounters(CounterType.P0P1) - 2 * getCounters(CounterType.M0M2) + 2 * getCounters(CounterType.P2P2)
                - getCounters(CounterType.M0M1) - getCounters(CounterType.M2M1) - 2 * getCounters(CounterType.M2M2)
                + 2 * getCounters(CounterType.P0P2);
        return total;
    }

    public final int getNetDefense() {
        if (getAmountOfKeyword("CARDNAME's power and toughness are switched") % 2 != 0) {
            return getUnswitchedPower();
        }
        return getUnswitchedToughness();
    }

    // How much combat damage does the card deal
    public final int getNetCombatDamage() {
        if (hasKeyword("CARDNAME assigns no combat damage")) {
            return 0;
        }

        if (getGame().getStaticEffects().getGlobalRuleChange(GlobalRuleChange.toughnessAssignsDamage)) {
            return getNetDefense();
        }
        return getNetAttack();
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
    public final int getTempAttackBoost() {
        return tempAttackBoost;
    }

    public final int getTempDefenseBoost() {
        return tempDefenseBoost;
    }

    public final void addTempAttackBoost(final int n) {
        tempAttackBoost += n;
    }

    public final void addTempDefenseBoost(final int n) {
        tempDefenseBoost += n;
    }

    // for cards like Glorious Anthem, etc.
    public final int getSemiPermanentAttackBoost() {
        return semiPermanentAttackBoost;
    }

    public final int getSemiPermanentDefenseBoost() {
        return semiPermanentDefenseBoost;
    }

    public final void addSemiPermanentAttackBoost(final int n) {
        semiPermanentAttackBoost += n;
    }

    public final void addSemiPermanentDefenseBoost(final int n) {
        semiPermanentDefenseBoost += n;
    }

    public final void setSemiPermanentAttackBoost(final int n) {
        semiPermanentAttackBoost = n;
    }

    public final void setSemiPermanentDefenseBoost(final int n) {
        semiPermanentDefenseBoost = n;
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
        final Map<String, Object> runParams = new TreeMap<String, Object>();
        runParams.put("Card", this);
        getGame().getTriggerHandler().runTrigger(TriggerType.Taps, runParams, false);

        setTapped(true);
        getGame().fireEvent(new GameEventCardTapped(this, true));
    }

    public final void untap() {
        if (!tapped) { return; }

        // Run Replacement effects
        final HashMap<String, Object> repRunParams = new HashMap<String, Object>();
        repRunParams.put("Event", "Untap");
        repRunParams.put("Affected", this);

        if (getGame().getReplacementHandler().run(repRunParams) != ReplacementResult.NotReplaced) {
            return;
        }

        // Run triggers
        final Map<String, Object> runParams = new TreeMap<String, Object>();
        runParams.put("Card", this);
        getGame().getTriggerHandler().runTrigger(TriggerType.Untaps, runParams, false);

        for (final GameCommand var : untapCommandList) {
            var.run();
        }
        setTapped(false);
        getGame().fireEvent(new GameEventCardTapped(this, false));
    }

    // keywords are like flying, fear, first strike, etc...
    public final List<String> getKeyword() {
        final ArrayList<String> keywords = getUnhiddenKeyword();
        keywords.addAll(getHiddenExtrinsicKeyword());

        return keywords;
    }

    public final int getKeywordAmount(final String keyword) {
        int res = 0;
        for (final String k : getKeyword()) {
            if (k.equals(keyword)) {
                res++;
            }
        }
        return res;
    }

    public final void addChangedCardKeywords(final List<String> keywords, final List<String> removeKeywords,
            final boolean removeAllKeywords, final long timestamp) {
        keywords.removeAll(getCantHaveOrGainKeyword());
        // if the key already exists - merge entries
        if (changedCardKeywords.containsKey(timestamp)) {
            List<String> kws = keywords;
            List<String> rkws = removeKeywords;
            boolean remAll = removeAllKeywords;
            final KeywordsChange cks = changedCardKeywords.get(timestamp);
            kws.addAll(cks.getKeywords());
            rkws.addAll(cks.getRemoveKeywords());
            remAll |= cks.isRemoveAllKeywords();
            changedCardKeywords.put(timestamp, new KeywordsChange(kws, rkws, remAll));
            return;
        }

        changedCardKeywords.put(timestamp, new KeywordsChange(keywords, removeKeywords, removeAllKeywords));
    }

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

        addChangedCardKeywords(keywordsList, removeKeywordsList, removeAllKeywords, timestamp);
    }

    public final KeywordsChange removeChangedCardKeywords(final long timestamp) {
        return changedCardKeywords.remove(Long.valueOf(timestamp));
    }

    // Hidden keywords will be left out
    public final ArrayList<String> getUnhiddenKeyword() {
        final ArrayList<String> keywords = new ArrayList<String>();
        keywords.addAll(getIntrinsicKeyword());
        keywords.addAll(getExtrinsicKeyword());

        // see if keyword changes are in effect
        for (final KeywordsChange ck : changedCardKeywords.values()) {

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
     * Replace all instances of one color word in this card's text by another.
     * @param originalWord the original color word.
     * @param newWord the new color word.
     * @return the timestamp.
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
                removeChangedCardKeywords(timestamp.longValue()));
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
                removeChangedCardKeywords(timestamp.longValue()));
        updateChangedText();
    }

    private final void updateKeywordsChangedText(final Long timestamp) {
        if (hasSVar("LockInKeywords")) {
            return;
        }

        final List<String> addKeywords = Lists.newArrayList(),
                removeKeywords = Lists.newArrayList(keywordsGrantedByTextChanges);

        for (final String kw : getIntrinsicKeyword()) {
            final String newKw = AbilityUtils.applyKeywordTextChangeEffects(kw, this);
            if (!newKw.equals(kw)) {
                addKeywords.add(newKw);
                removeKeywords.add(kw);
                keywordsGrantedByTextChanges.add(newKw);
            }
        }
        addChangedCardKeywords(addKeywords, removeKeywords, false, timestamp.longValue());
    }

    private final void updateKeywordsOnRemoveChangedText(final KeywordsChange k) {
        if (k != null) {
            keywordsGrantedByTextChanges.removeAll(k.getKeywords());
        }
    }

    /**
     * Update the changed text of the intrinsic spell abilities and keywords.
     */
    private final void updateChangedText() {
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

    public final List<String> getUnparsedAbilities() {
        return getCharacteristics().getUnparsedAbilities();
    }

    public final List<String> getIntrinsicKeyword() {
        // will not create a copy here - due to performance reasons.
        // Most of other code checks for contains, or creates copy by itself
        return getCharacteristics().getIntrinsicKeyword();
    }

    public final void setIntrinsicAbilities(final List<String> a) {
        getCharacteristics().setUnparsedAbilities(new ArrayList<String>(a));
    }

    public final void addIntrinsicKeyword(final String s) {
        if (s.trim().length() != 0) {
            getCharacteristics().getIntrinsicKeyword().add(s);
        }
    }

    public final void addIntrinsicAbility(final String s) {
        if (s.trim().length() != 0) {
            getCharacteristics().getUnparsedAbilities().add(s);
        }
    }

    public final void removeIntrinsicKeyword(final String s) {
        getCharacteristics().getIntrinsicKeyword().remove(s);
    }

    public ArrayList<String> getExtrinsicKeyword() {
        return extrinsicKeyword;
    }
    public final void setExtrinsicKeyword(final ArrayList<String> a) {
        extrinsicKeyword = new ArrayList<String>(a);
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
            extrinsicKeyword.remove(s);
        }
    }

    public void removeAllExtrinsicKeyword(final String s) {
        final ArrayList<String> strings = new ArrayList<String>();
        strings.add(s);
        hiddenExtrinsicKeyword.removeAll(strings);
        extrinsicKeyword.removeAll(strings);
    }

    // Hidden Keywords will be returned without the indicator HIDDEN
    public final ArrayList<String> getHiddenExtrinsicKeyword() {
        while (true) {
            try {
                final ArrayList<String> keywords = new ArrayList<String>();
                for (String keyword : hiddenExtrinsicKeyword) {
                    if (keyword == null) {
                        continue;
                    }
                    if (keyword.startsWith("HIDDEN")) {
                        keyword = keyword.substring(7);
                    }
                    keywords.add(keyword);
                }
                return keywords;
            } catch (IndexOutOfBoundsException ex) {
                // Do nothing and let the while loop retry
            }
        }
    }

    public final void addHiddenExtrinsicKeyword(final String s) {
        hiddenExtrinsicKeyword.add(s);
    }

    public final void removeHiddenExtrinsicKeyword(final String s) {
        hiddenExtrinsicKeyword.remove(s);
    }

    public final List<String> getCantHaveOrGainKeyword() {
        final List<String> cantGain = new ArrayList<String>();
        for (String s : hiddenExtrinsicKeyword) {
            if (s.contains("can't have or gain")) {
                cantGain.add(s.split("can't have or gain ")[1]);
            }
        }
        return cantGain;
    }

    public final void setStaticAbilityStrings(final List<String> a) {
        getCharacteristics().setStaticAbilityStrings(new ArrayList<String>(a));
    }

    public final List<String> getStaticAbilityStrings() {
        return getCharacteristics().getStaticAbilityStrings();
    }
    public final void setStaticAbilities(final ArrayList<StaticAbility> a) {
        getCharacteristics().setStaticAbilities(new ArrayList<StaticAbility>(a));
    }
    public final void addStaticAbilityString(final String s) {
        if (StringUtils.isNotBlank(s)) {
            getCharacteristics().getStaticAbilityStrings().add(s);
        }
    }

    public final ArrayList<StaticAbility> getStaticAbilities() {
        return new ArrayList<StaticAbility>(getCharacteristics().getStaticAbilities());
    }
    public final StaticAbility addStaticAbility(final String s) {
        if (s.trim().length() != 0) {
            final StaticAbility stAb = new StaticAbility(s, this);
            getCharacteristics().getStaticAbilities().add(stAb);
            return stAb;
        }
        return null;
    }

    public final boolean isPermanent() {
        if (isImmutable) {
            return false;
        }
        return getType().isPermanent();
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
        } else if (id > that.id) {
            return +1;
        } else if (id < that.id) {
            return -1;
        } else {
            return 0;
        }
    }

    /** {@inheritDoc} */
    @Override
    public final int hashCode() {
        return id;
    }

    /** {@inheritDoc} */
    @Override
    public final String toString() {
        return getName() + " (" + id + ")";
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

        final Map<String, Object> runParams = new TreeMap<String, Object>();
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
            getGame().getTriggerHandler().suppressMode(TriggerType.ChangesZone);
            getZone().remove(this);
            getGame().getTriggerHandler().clearSuppression(TriggerType.ChangesZone);
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
        for (final SpellAbility a : getCharacteristics().getManaAbility()) {
            if (a.getApi() == ApiType.ManaReflected) {
                return true;
            }
        }
        return false;
    }

    @Override
    public final boolean hasKeyword(final String keyword) {
        String kw = keyword;
        if (kw.startsWith("HIDDEN")) {
            kw = kw.substring(7);
        }
        return getKeyword().contains(kw);
    }

    public final boolean hasStartOfKeyword(final String keyword) {
        final List<String> a = getKeyword();
        for (int i = 0; i < a.size(); i++) {
            if (a.get(i).toString().startsWith(keyword)) {
                return true;
            }
        }
        return false;
    }

    public final boolean hasStartOfUnHiddenKeyword(final String keyword) {
        final List<String> a = getUnhiddenKeyword();
        for (int i = 0; i < a.size(); i++) {
            if (a.get(i).toString().startsWith(keyword)) {
                return true;
            }
        }
        return false;
    }

    public final int getKeywordPosition(final String k) {
        final List<String> a = getKeyword();
        for (int i = 0; i < a.size(); i++) {
            if (a.get(i).toString().startsWith(k)) {
                return i;
            }
        }
        return -1;
    }

    public final boolean hasAnyKeyword(final Iterable<String> keywords) {
        for (final String keyword : keywords) {
            if (hasKeyword(keyword)) {
                return true;
            }
        }

        return false;
    }

    // This counts the number of instances of a keyword a card has
    public final int getAmountOfKeyword(final String k) {
        int count = 0;
        for (String kw : getKeyword()) {
            if (kw.equals(k)) {
                count++;
            }
        }

        return count;
    }

    // This is for keywords with a number like Bushido, Annihilator and Rampage.
    // It returns the total.
    public final int getKeywordMagnitude(final String k) {
        int count = 0;
        for (final String kw : getKeyword()) {
            if (kw.startsWith(k)) {
                final String[] parse = kw.split(" ");
                final String s = parse[1];
                count += Integer.parseInt(s);
            }
        }
        return count;
    }

    // Takes one argument like Permanent.Blue+withFlying
    @Override
    public final boolean isValid(final String restriction, final Player sourceController, final Card source) {
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
            final String[] exR = excR.split("\\+"); // Exclusive Restrictions are ...
            for (int j = 0; j < exR.length; j++) {
                if (!hasProperty(exR[j], sourceController, source)) {
                    return testFailed;
                }
            }
        }
        return !testFailed;
    } 

    // Takes arguments like Blue or withFlying
    @Override
    public boolean hasProperty(final String property, final Player sourceController, final Card source) {
        final Game game = getGame();
        final Combat combat = game.getCombat();
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
            if (!getName().equals(source.getName())) {
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
            int desiredColor = MagicColor.fromName(mustHave ? property : property.substring(3));
            boolean hasColor = CardUtil.getColors(this).hasAnyColor(desiredColor);
            if (mustHave != hasColor)
                return false;

        } else if (property.contains("Colorless")) { // ... Card is colorless
            if (property.startsWith("non") == CardUtil.getColors(this).isColorless()) return false;

        } else if (property.contains("MultiColor")) { // ... Card is multicolored
            if (property.startsWith("non") == CardUtil.getColors(this).isMulticolor()) return false;

        } else if (property.contains("MonoColor")) { // ... Card is monocolored
            if (property.startsWith("non") == CardUtil.getColors(this).isMonoColor()) return false;

        } else if (property.equals("ChosenColor")) {
            if (!source.hasChosenColor() || !CardUtil.getColors(this).hasAnyColor(MagicColor.fromName(source.getChosenColor())))
                return false;

        } else if (property.equals("AllChosenColors")) {
            if (!source.hasChosenColor() || !CardUtil.getColors(this).hasAllColors(ColorSet.fromNames(source.getChosenColors()).getColor()))
                return false;

        } else if (property.equals("AnyChosenColor")) {
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
            if (!getController().equals(sourceController)) {
                return false;
            }
        } else if (property.startsWith("YouDontCtrl")) {
            if (getController().equals(sourceController)) {
                return false;
            }
        } else if (property.startsWith("OppCtrl")) {
            if (!getController().getOpponents().contains(sourceController)) {
                return false;
            }
        } else if (property.startsWith("ChosenCtrl")) {
            if (!getController().equals(source.getChosenPlayer())) {
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
                if (getGame().getCombat().getDefendingPlayerRelatedTo((Card) source.getFirstRemembered()) != getController()) {
                    return false;
                }
            } else {
                if (getGame().getCombat().getDefendingPlayerRelatedTo(source) != getController()) {
                    return false;
                }
            }
        } else if (property.startsWith("DefendingPlayerCtrl")) {
            if (!game.getPhaseHandler().inCombat()) {
                return false;
            }
            if (!getGame().getCombat().isPlayerAttacked(getController())) {
                return false;
            }
        } else if (property.startsWith("EnchantedPlayerCtrl")) {
            final Object o = source.getEnchanting();
            if (o instanceof Player) {
                if (!getController().equals(o)) {
                    return false;
                }
            } else { // source not enchanting a player
                return false;
            }
        } else if (property.startsWith("EnchantedControllerCtrl")) {
            final Object o = source.getEnchanting();
            if (o instanceof Card) {
                if (!getController().equals(((Card) o).getController())) {
                    return false;
                }
            } else { // source not enchanting a card
                return false;
            }
        } else if (property.startsWith("RememberedPlayer")) {
            Player p = property.endsWith("Ctrl") ? getController() : getOwner();
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
                if (newCard.isRemembered(getController())) {
                    return false;
                }
            }

            if (source.isRemembered(getController())) {
                return false;
            }
        } else if (property.equals("TargetedPlayerCtrl")) {
            for (final SpellAbility sa : source.getCharacteristics().getSpellAbility()) {
                final SpellAbility saTargeting = sa.getSATargetingPlayer();
                if (saTargeting != null) {
                    for (final Player p : saTargeting.getTargets().getTargetPlayers()) {
                        if (!getController().equals(p)) {
                            return false;
                        }
                    }
                }
            }
        } else if (property.equals("TargetedControllerCtrl")) {
            for (final SpellAbility sa : source.getCharacteristics().getSpellAbility()) {
                final CardCollectionView cards = AbilityUtils.getDefinedCards(source, "Targeted", sa);
                final List<SpellAbility> sas = AbilityUtils.getDefinedSpellAbilities(source, "Targeted", sa);
                for (final Card c : cards) {
                    final Player p = c.getController();
                    if (!getController().equals(p)) {
                        return false;
                    }
                }
                for (final SpellAbility s : sas) {
                    final Player p = s.getHostCard().getController();
                    if (!getController().equals(p)) {
                        return false;
                    }
                }
            }
        } else if (property.startsWith("ActivePlayerCtrl")) {
            if (!game.getPhaseHandler().isPlayerTurn(getController())) {
                return false;
            }
        } else if (property.startsWith("NonActivePlayerCtrl")) {
            if (game.getPhaseHandler().isPlayerTurn(getController())) {
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
            for (final SpellAbility sa : source.getCharacteristics().getSpellAbility()) {
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
            if (!getOwner().isValid(valid, sourceController, source)) {
                return false;
            }
        } else if (property.startsWith("OwnerDoesntControl")) {
            if (getOwner().equals(getController())) {
                return false;
            }
        } else if (property.startsWith("ControllerControls")) {
            final String type = property.substring(18);
            if (type.startsWith("AtLeastAsMany")) {
                String realType = type.split("AtLeastAsMany")[1];
                CardCollectionView cards = CardLists.getType(getController().getCardsIn(ZoneType.Battlefield), realType);
                CardCollectionView yours = CardLists.getType(sourceController.getCardsIn(ZoneType.Battlefield), realType);
                if (cards.size() < yours.size()) {
                    return false;
                }
            } else {
                final CardCollectionView cards = getController().getCardsIn(ZoneType.Battlefield);
                if (CardLists.getType(cards, type).isEmpty()) {
                    return false;
                }
            }
        } else if (property.startsWith("Other")) {
            if (equals(source)) {
                return false;
            }
        } else if (property.startsWith("Self")) {
            if (!equals(source)) {
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
                if (!source.getCharacteristics().getTriggers().isEmpty()) {
                    for (final Trigger t : source.getCharacteristics().getTriggers()) {
                        final SpellAbility sa = t.getTriggeredSA();
                        final CardCollectionView cards = AbilityUtils.getDefinedCards(source, "Targeted", sa);
                        for (final Card c : cards) {
                            if (equipping != c && !c.equals(enchanting)) {
                                return false;
                            }
                        }
                    }
                } else {
                    for (final SpellAbility sa : source.getCharacteristics().getSpellAbility()) {
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
                if ((enchanting == null || !enchanting.isValid(restriction, sourceController, source))
                        && (equipping == null || !equipping.isValid(restriction, sourceController, source))
                        && (fortifying == null || !fortifying.isValid(restriction, sourceController, source))) {
                    return false;
                }
            }
        } else if (property.equals("NameNotEnchantingEnchantedPlayer")) {
            Player enchantedPlayer = source.getEnchantingPlayer();
            if (enchantedPlayer == null) {
                return false;
            }
            for (Card c : enchantedPlayer.getEnchantedBy(false)) {
                if (getName().equals(c.getName())) {
                    return false;
                }
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
                if (restriction.equals("Imprinted")) {
                    for (final Card card : source.getImprintedCards()) {
                        if (!isEnchantedBy(card) && !equals(card.getEnchanting())) {
                            return false;
                        }
                    }
                } else if (restriction.equals("Targeted")) {
                    for (final SpellAbility sa : source.getCharacteristics().getSpellAbility()) {
                        final SpellAbility saTargeting = sa.getSATargetingCard();
                        if (saTargeting != null) {
                            for (final Card c : saTargeting.getTargets().getTargetCards()) {
                                if (!isEnchantedBy(c) && !equals(c.getEnchanting())) {
                                    return false;
                                }
                            }
                        }
                    }
                } else { // EnchantedBy Aura.Other
                    for (final Card aura : getEnchantedBy(false)){
                        if (aura.isValid(restriction, sourceController, source)) {
                            return true;
                        }
                    }
                    return false;
                }
            }
        } else if (property.startsWith("NotEnchantedBy")) {
            if (property.substring(14).equals("Targeted")) {
                for (final SpellAbility sa : source.getCharacteristics().getSpellAbility()) {
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
                for (final SpellAbility sa : source.getCharacteristics().getSpellAbility()) {
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
                for (final SpellAbility sa : source.getCharacteristics().getSpellAbility()) {
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
            if (!receivedDamageFromThisTurn.containsKey(source)) {
                return false;
            }
        } else if (property.startsWith("Damaged")) {
            if (!dealtDamageToThisTurn.containsKey(source)) {
                return false;
            }
        } else if (property.startsWith("IsTargetingSource")) {
            for (final SpellAbility sa : getCharacteristics().getSpellAbility()) {
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
        } else if (property.startsWith("SharesColorWith")) {
            if (property.equals("SharesColorWith")) {
                if (!sharesColorWith(source)) {
                    return false;
                }
            } else {
                final String restriction = property.split("SharesColorWith ")[1];
                if (restriction.equals("TopCardOfLibrary")) {
                    final CardCollectionView cards = sourceController.getCardsIn(ZoneType.Library);
                    if (cards.isEmpty() || !sharesColorWith(cards.get(0))) {
                        return false;
                    }
                } else if (restriction.equals("Remembered")) {
                    for (final Object obj : source.getRemembered()) {
                        if (!(obj instanceof Card) || !sharesColorWith((Card) obj)) {
                            return false;
                        }
                    }
                } else if (restriction.equals("Imprinted")) {
                    for (final Card card : source.getImprintedCards()) {
                        if (!sharesColorWith(card)) {
                            return false;
                        }
                    }
                } else if (restriction.equals("Equipped")) {
                    if (!source.isEquipment() || !source.isEquipping()
                            || !sharesColorWith(source.getEquipping())) {
                        return false;
                    }
                } else if (restriction.equals("MostProminentColor")) {
                    byte mask = CardFactoryUtil.getMostProminentColors(game.getCardsIn(ZoneType.Battlefield));
                    if (!CardUtil.getColors(this).hasAnyColor(mask))
                        return false;
                } else if (restriction.equals("LastCastThisTurn")) {
                    final CardCollectionView c = game.getStack().getSpellsCastThisTurn();
                    if (c.isEmpty() || !sharesColorWith(c.get(c.size() - 1))) {
                        return false;
                    }
                } else if (restriction.equals("ActivationColor")) {
                    byte manaSpent = source.getColorsPaid();
                    if (!CardUtil.getColors(this).hasAnyColor(manaSpent)) {
                        return false;
                    }
                } else {
                    for (final Card card : sourceController.getCardsIn(ZoneType.Battlefield)) {
                        if (card.isValid(restriction, sourceController, source) && sharesColorWith(card)) {
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
                    if (card.isValid(restriction, sourceController, source) && sharesColorWith(card)) {
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
                if (restriction.equals("TopCardOfLibrary")) {
                    final CardCollectionView cards = sourceController.getCardsIn(ZoneType.Library);
                    if (cards.isEmpty() || !sharesCreatureTypeWith(cards.get(0))) {
                        return false;
                    }
                } else if (restriction.equals("Enchanted")) {
                    for (final SpellAbility sa : source.getCharacteristics().getSpellAbility()) {
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
                } else if (restriction.equals("Equipped")) {
                    if (source.isEquipping() && sharesCreatureTypeWith(source.getEquipping())) {
                        return true;
                    }
                    return false;
                } else if (restriction.equals("Remembered")) {
                    for (final Object rem : source.getRemembered()) {
                        if (rem instanceof Card) {
                            final Card card = (Card) rem;
                            if (sharesCreatureTypeWith(card)) {
                                return true;
                            }
                        }
                    }
                    return false;
                } else if (restriction.equals("AllRemembered")) {
                    for (final Object rem : source.getRemembered()) {
                        if (rem instanceof Card) {
                            final Card card = (Card) rem;
                            if (!sharesCreatureTypeWith(card)) {
                                return false;
                            }
                        }
                    }
                } else {
                    boolean shares = false;
                    for (final Card card : sourceController.getCardsIn(ZoneType.Battlefield)) {
                        if (card.isValid(restriction, sourceController, source) && sharesCreatureTypeWith(card)) {
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
                if (!sharesCardTypeWith(source)) {
                    return false;
                }
            } else {
                final String restriction = property.split("sharesCardTypeWith ")[1];
                if (restriction.equals("Imprinted")) {
                    if (!source.hasImprintedCard() || !sharesCardTypeWith(Iterables.getFirst(source.getImprintedCards(), null))) {
                        return false;
                    }
                } else if (restriction.equals("Remembered")) {
                    for (final Object rem : source.getRemembered()) {
                        if (rem instanceof Card) {
                            final Card card = (Card) rem;
                            if (sharesCardTypeWith(card)) {
                                return true;
                            }
                        }
                    }
                    return false;
                } else if (restriction.equals("EachTopLibrary")) {
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
                if (!getName().equals(source.getName())) {
                    return false;
                }
            } else {
                final String restriction = property.split("sharesNameWith ")[1];
                if (restriction.equals("YourGraveyard")) {
                    for (final Card card : sourceController.getCardsIn(ZoneType.Graveyard)) {
                        if (getName().equals(card.getName())) {
                            return true;
                        }
                    }
                    return false;
                } else if (restriction.equals(ZoneType.Graveyard.toString())) {
                    for (final Card card : game.getCardsIn(ZoneType.Graveyard)) {
                        if (getName().equals(card.getName())) {
                            return true;
                        }
                    }
                    return false;
                } else if (restriction.equals(ZoneType.Battlefield.toString())) {
                    for (final Card card : game.getCardsIn(ZoneType.Battlefield)) {
                        if (getName().equals(card.getName())) {
                            return true;
                        }
                    }
                    return false;
                } else if (restriction.equals("ThisTurnCast")) {
                    for (final Card card : CardUtil.getThisTurnCast("Card", source)) {
                        if (getName().equals(card.getName())) {
                            return true;
                        }
                    }
                    return false;
                } else if (restriction.equals("Remembered")) {
                    for (final Object rem : source.getRemembered()) {
                        if (rem instanceof Card) {
                            final Card card = (Card) rem;
                            if (getName().equals(card.getName())) {
                                return true;
                            }
                        }
                    }
                    return false;
                } else if (restriction.equals("Imprinted")) {
                    for (final Card card : source.getImprintedCards()) {
                        if (getName().equals(card.getName())) {
                            return true;
                        }
                    }
                    return false;
                } else if (restriction.equals("MovedToGrave")) {
                    for (final SpellAbility sa : source.getCharacteristics().getSpellAbility()) {
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
                    final CardCollectionView cards = CardLists.filter(game.getCardsIn(ZoneType.Battlefield),
                            Presets.NON_TOKEN);
                    for (final Card card : cards) {
                        if (getName().equals(card.getName())) {
                            return true;
                        }
                    }
                    return false;
                }
            }
        } else if (property.startsWith("doesNotShareNameWith")) {
            if (property.equals("doesNotShareNameWith")) {
                if (getName().equals(source.getName())) {
                    return false;
                }
            } else {
                final String restriction = property.split("doesNotShareNameWith ")[1];
                if (restriction.equals("Remembered")) {
                    for (final Object rem : source.getRemembered()) {
                        if (rem instanceof Card) {
                            final Card card = (Card) rem;
                            if (getName().equals(card.getName())) {
                                return false;
                            }
                        }
                    }
                }
            }
        } else if (property.startsWith("sharesControllerWith")) {
            if (property.equals("sharesControllerWith")) {
                if (!sharesControllerWith(source)) {
                    return false;
                }
            } else {
                final String restriction = property.split("sharesControllerWith ")[1];
                if (restriction.equals("Remembered")) {
                    for (final Object rem : source.getRemembered()) {
                        if (rem instanceof Card) {
                            final Card card = (Card) rem;
                            if (!sharesControllerWith(card)) {
                                return false;
                            }
                        }
                    }
                } else if (restriction.equals("Imprinted")) {
                    for (final Card card : source.getImprintedCards()) {
                        if (!sharesControllerWith(card)) {
                            return false;
                        }
                    }
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
            final CardCollectionView cards = CardUtil.getThisTurnCast("Card", source);
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
                    if (pl.isValid(res[1], sourceController, source)) {
                        p = pl;
                        break;
                    }
                }
            } else {
                p = sourceController;
            }
            if (p == null || !getController().equals(game.getNextPlayerAfter(p, direction))) {
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
            if (!source.getDamageHistory().getThisTurnCombatDamaged().contains(getController())) {
                return false;
            }
        } else if (property.startsWith("controllerWasDealtDamageByThisTurn")) {
            if (!source.getDamageHistory().getThisTurnDamaged().contains(getController())) {
                return false;
            }
        } else if (property.startsWith("wasDealtDamageThisTurn")) {
            if ((getReceivedDamageFromThisTurn().keySet()).isEmpty()) {
                return false;
            }
        } else if (property.equals("wasDealtDamageByHostThisTurn")) {
            if (!getReceivedDamageFromThisTurn().keySet().contains(source)) {
                return false;
            }
        } else if (property.equals("wasDealtDamageByEquipeeThisTurn")) {
            Card equipee = source.getEquipping();
            if (equipee == null || getReceivedDamageFromThisTurn().keySet().isEmpty()
                    || !getReceivedDamageFromThisTurn().keySet().contains(equipee)) {
                return false;
            }
         } else if (property.equals("wasDealtDamageByEnchantedThisTurn")) {
            Card enchanted = source.getEnchantingCard();
            if (enchanted == null || getReceivedDamageFromThisTurn().keySet().isEmpty()
                    || !getReceivedDamageFromThisTurn().keySet().contains(enchanted)) {
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
            return getDamageHistory().getCreatureAttackedLastTurnOf(getController());
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
            return !getDamageHistory().getCreatureAttackedLastTurnOf(getController());
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
                if (crd.getNetAttack() > getNetAttack()) {
                    return false;
                }
            }
        } else if (property.startsWith("yardGreatestPower")) {
            final CardCollectionView cards = CardLists.filter(sourceController.getCardsIn(ZoneType.Graveyard), Presets.CREATURES);
            for (final Card crd : cards) {
                if (crd.getNetAttack() > getNetAttack()) {
                    return false;
                }
            }
        } else if (property.startsWith("leastPower")) {
            final CardCollectionView cards = CardLists.filter(game.getCardsIn(ZoneType.Battlefield), Presets.CREATURES);
            for (final Card crd : cards) {
                if (crd.getNetAttack() < getNetAttack()) {
                    return false;
                }
            }
        } else if (property.startsWith("leastToughness")) {
            final CardCollectionView cards = CardLists.filter(game.getCardsIn(ZoneType.Battlefield), Presets.CREATURES);
            for (final Card crd : cards) {
                if (crd.getNetDefense() < getNetDefense()) {
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

        } else if (property.startsWith("power") || property.startsWith("toughness")
                || property.startsWith("cmc") || property.startsWith("totalPT")) {
            int x = 0;
            int y = 0;
            int y2 = -1; // alternative value for the second split face of a split card
            String rhs = "";

            if (property.startsWith("power")) {
                rhs = property.substring(7);
                y = getNetAttack();
            } else if (property.startsWith("toughness")) {
                rhs = property.substring(11);
                y = getNetDefense();
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
                y = getNetAttack() + getNetDefense();
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
                if (!Expressions.compare(y, property, x) && !Expressions.compare(y2, property, x)) {
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
             * System.out.println("Parsing completed of: "+Property); for (int i
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

            final int actualnumber = getCounters(CounterType.getType(counterType));

            if (!Expressions.compare(actualnumber, comparator, number)) {
                return false;
            }
        }
        // These predicated refer to ongoing combat. If no combat happens, they'll return false (meaning not attacking/blocking ATM)
        else if (property.startsWith("attacking")) {
            if (null == combat) return false;
            if (property.equals("attacking"))    return combat.isAttacking(this);
            if (property.equals("attackingYou")) return combat.isAttacking(this, sourceController);
        } else if (property.startsWith("notattacking")) {
            return null == combat || !combat.isAttacking(this);
        } else if (property.equals("attackedBySourceThisCombat")) {
            if (null == combat) return false;
            final GameEntity defender = combat.getDefenderByAttacker(source);
            if (defender instanceof Card) {
                if (!equals((Card) defender)) {
                    return false;
                }
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
        } else if (property.startsWith("evoked")) {
            if (!isEvoked()) {
                return false;
            }
        } else if (property.equals("HasDevoured")) {
            if (devouredCards.size() == 0) {
                return false;
            }
        } else if (property.equals("HasNotDevoured")) {
            if (devouredCards.size() != 0) {
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
        } else if (property.startsWith("non")) {
            // ... Other Card types
            if (getType().hasStringType(property.substring(3))) {
                return false;
            }
        } else if (property.equals("CostsPhyrexianMana")) {
            if (!getCharacteristics().getManaCost().hasPhyrexian()) {
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
            if (!((getAbilityText().trim().equals("") || isFaceDown()) && (getUnhiddenKeyword().size() == 0))) {
                return false;
            }
        } else if (property.equals("HasCounters")) {
            if (!hasCounters()) {
                return false;
            }
        } else if (property.startsWith("wasCastFrom")) {
            final String strZone = property.substring(11);
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
            if (!getCurSetCode().equals(setCode)) {
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

    public final boolean sharesColorWith(final Card c1) {
        boolean shares = false;
        shares |= (isBlack() && c1.isBlack());
        shares |= (isBlue() && c1.isBlue());
        shares |= (isGreen() && c1.isGreen());
        shares |= (isRed() && c1.isRed());
        shares |= (isWhite() && c1.isWhite());
        return shares;
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
        if (c1 == null) {
            return false;
        }
        return getController().equals(c1.getController());
    }

    public final boolean hasACreatureType() {
        for (final String type : getType().getSubtypes()) {
            if (forge.card.CardType.isACreatureType(type) ||  type.equals("AllCreatureTypes")) {
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
        receivedDamageFromThisTurn.put(c, damage);
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

    public final Map<Card, Integer> getDealtDamageToThisTurn() {
        return dealtDamageToThisTurn;
    }
    public final void setDealtDamageToThisTurn(final Map<Card, Integer> dealtDamageList) {
        dealtDamageToThisTurn = dealtDamageList;
    }
    public final void addDealtDamageToThisTurn(final Card c, final int damage) {
        dealtDamageToThisTurn.put(c, damage);
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
        dealtDamageToPlayerThisTurn.put(player, damage);
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
        return getNetDefense() - getDamage() - getTotalAssignedDamage();
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
        final CardCollection cards = new CardCollection();

        for (final Entry<Card, Integer> entry : map.entrySet()) {
            final Card source = entry.getKey();
            cards.add(source);
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
                        && !source.isValid(params.get("ValidSource"), ca.getController(), ca)) {
                    continue;
                }
                if (params.containsKey("ValidTarget")
                        && !isValid(params.get("ValidTarget"), ca.getController(), ca)) {
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

        if (hasProtectionFrom(source)) {
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
        for (String kw : getKeyword()) {
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
                String valid = getKeyword().get(getKeywordPosition("PreventAllDamageBy"));
                valid = valid.split(" ", 2)[1];
                if (source.isValid(valid, getController(), this)) {
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
            TreeMap<Card, Map<String, String>> shieldMap = getPreventNextDamageWithEffect();
            CardCollectionView preventionEffectSources = new CardCollection(shieldMap.keySet());
            Card shieldSource = preventionEffectSources.get(0);
            if (preventionEffectSources.size() > 1) {
                Map<String, Card> choiceMap = new TreeMap<String, Card>();
                List<String> choices = new ArrayList<String>();
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
        final int damageToAdd = damageIn;

        if (damageToAdd == 0) {
            return false; // Rule 119.8
        }

        addReceivedDamageFromThisTurn(source, damageToAdd);
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
        if (isPlaneswalker()) {
            subtractCounter(CounterType.LOYALTY, damageToAdd);
            damageType = DamageType.LoyaltyLoss;
        }
        else {
            final Game game = source.getGame();

            boolean wither = (game.getStaticEffects().getGlobalRuleChange(GlobalRuleChange.alwaysWither)
                    || source.hasKeyword("Wither") || source.hasKeyword("Infect"));

            if (isInPlay()) {
                if (wither) {
                    addCounter(CounterType.M1M1, damageToAdd, true);
                    damageType = DamageType.M1M1Counters;
                }
                else {
                    damage += damageToAdd;
                    view.updateDamage(this);
                }
            }

            if (source.hasKeyword("Deathtouch") && isCreature()) {
                game.getAction().destroy(this, null);
                damageType = DamageType.Deathtouch;
            }

            // Play the Damage sound
            game.fireEvent(new GameEventCardDamaged(this, source, damageToAdd, damageType));
        }
        return true;
    }

    public final String getCurSetCode() {
        return getCharacteristics().getCurSetCode();
    }
    public final void setCurSetCode(final String setCode) {
        if (getCurSetCode().equals(setCode)) { return; }
        getCharacteristics().setCurSetCode(setCode);
        view.updateSetCode(this);
    }

    public final CardRarity getRarity() {
        return getCharacteristics().getRarity();
    }
    public final void setRarity(CardRarity r) {
        if (getRarity().equals(r)) { return; }
        getCharacteristics().setRarity(r);
        view.updateRarity(this);
    }

    public final String getMostRecentSet() {
        return StaticData.instance().getCommonCards().getCard(getName()).getEdition();
    }

    public final String getImageKey() {
        return getCardForUi().getCharacteristics().getImageKey();
    }
    public final void setImageKey(final String iFN) {
        getCardForUi().getCharacteristics().setImageKey(iFN);
    }

    public String getImageKey(CardCharacteristicName state) {
        CardCharacteristics c = getCardForUi().characteristicsMap.get(state);
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

    public final void animateBestow() {
        bestowTimestamp = getGame().getNextTimestamp();
        addChangedCardTypes(new CardType(Arrays.asList("Aura")),
                new CardType(Arrays.asList("Creature")), false, false, false, true, bestowTimestamp);
        addChangedCardKeywords(Arrays.asList("Enchant creature"), new ArrayList<String>(), false, bestowTimestamp);
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
     *
     * @param remove if true, a random foil is assigned, otherwise it is removed.
     */
    public final void setRandomFoil() {
        setFoil(CardEdition.getRandomFoil(getCurSetCode()));
    }

    public final void setFoil(final int f) {
        getCharacteristics().setSVar("Foil", Integer.toString(f));
    }

    public final CardCollectionView getHauntedBy() {
        return CardCollection.getView(hauntedBy);
    }
    public final boolean isHaunted() {
        return CardCollection.hasCard(hauntedBy);
    }
    public final boolean isHauntedBy(Card c) {
        return CardCollection.hasCard(hauntedBy, c);
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
        return hasProtectionFrom(source, false);
    }

    public boolean hasProtectionFrom(final Card source, final boolean checkSBA) {
        if (source == null) {
            return false;
        }

        if (isImmutable()) {
            return true;
        }

        final List<String> keywords = getKeyword();
        if (keywords != null) {
            for (int i = 0; i < keywords.size(); i++) {
                final String kw = keywords.get(i);
                if (!kw.startsWith("Protection")) {
                    continue;
                }
                if (kw.equals("Protection from white")) {
                    if (source.isWhite()) {
                        return true;
                    }
                } else if (kw.equals("Protection from blue")) {
                    if (source.isBlue()) {
                        return true;
                    }
                } else if (kw.equals("Protection from black")) {
                    if (source.isBlack()) {
                        return true;
                    }
                } else if (kw.equals("Protection from red")) {
                    if (source.isRed()) {
                        return true;
                    }
                } else if (kw.equals("Protection from green")) {
                    if (source.isGreen()) {
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
                    final String characteristic = kws[1];
                    final String[] characteristics = characteristic.split(",");
                    final String exception = kws.length > 3 ? kws[3] : null; // check "This effect cannot remove sth"
                    if (source.isValid(characteristics, getController(), this)
                      && (!checkSBA || exception == null || !source.isValid(exception, getController(), this))) {
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
        return isInPlay() && (!hasKeyword("Indestructible") || (isCreature() && getNetDefense() <= 0));
    }

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

        if (getKeyword() != null) {
            for (String kw : getKeyword()) {
                if (kw.equals("Shroud")) {
                    return false;
                }

                if (kw.equals("Hexproof")) {
                    if (sa.getActivatingPlayer().getOpponents().contains(getController())) {
                        if (!sa.getActivatingPlayer().hasKeyword("Spells and abilities you control can target hexproof creatures")) {
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
        if (sa.isSpell() && source.hasStartOfKeyword("SpellCantTarget")) {
            final int keywordPosition = source.getKeywordPosition("SpellCantTarget");
            final String parse = source.getKeyword().get(keywordPosition).toString();
            final String[] k = parse.split(":");
            final String[] restrictions = k[1].split(",");
            if (isValid(restrictions, source.getController(), source)) {
                return false;
            }
        }
        return true;
    }

    public final boolean canBeControlledBy(final Player newController) {
        if (hasKeyword("Other players can't gain control of CARDNAME.")
                && !getController().equals(newController)) {
            return false;
        }
        return true;
    }

    public final boolean canBeEnchantedBy(final Card aura) {
        return canBeEnchantedBy(aura, false);
    }

    public final boolean canBeEnchantedBy(final Card aura, final boolean checkSBA) {
        SpellAbility sa = aura.getFirstAttachSpell();
        if (aura.isBestowed()) {
            for (SpellAbility s : aura.getSpellAbilities()) {
                if (s.getApi() == ApiType.Attach && s.hasParam("Bestow")) {
                    sa = s;
                    break;
                }
            }
        }
        TargetRestrictions tgt = null;
        if (sa != null) {
            tgt = sa.getTargetRestrictions();
        }

        if (hasProtectionFrom(aura, checkSBA)
            || (hasKeyword("CARDNAME can't be enchanted in the future.") && !isEnchantedBy(aura))
            || (hasKeyword("CARDNAME can't be enchanted.") && !aura.getName().equals("Anti-Magic Aura")
                    && !(aura.getName().equals("Consecrate Land") && aura.isInZone(ZoneType.Battlefield)))
            || ((tgt != null) && !isValid(tgt.getValidTgts(), aura.getController(), aura))) {
            return false;
        }
        return true;
    }

    public final boolean canBeEquippedBy(final Card equip) {
        if (equip.hasStartOfKeyword("CantEquip")) {
            final int keywordPosition = equip.getKeywordPosition("CantEquip");
            final String parse = equip.getKeyword().get(keywordPosition).toString();
            final String[] k = parse.split(" ", 2);
            final String[] restrictions = k[1].split(",");
            if (isValid(restrictions, equip.getController(), equip)) {
                return false;
            }
        }
        if (hasProtectionFrom(equip)
            || hasKeyword("CARDNAME can't be equipped.")
            || !isValid("Creature", equip.getController(), equip)) {
            return false;
        }
        return true;
    }

    public List<ReplacementEffect> getReplacementEffects() {
        return getCharacteristics().getReplacementEffects();
    }

    public void setReplacementEffects(final List<ReplacementEffect> res) {
        getCharacteristics().getReplacementEffects().clear();
        for (final ReplacementEffect replacementEffect : res) {
            if (replacementEffect.isIntrinsic()) {
                addReplacementEffect(replacementEffect);
            }
        }
    }

    public ReplacementEffect addReplacementEffect(final ReplacementEffect replacementEffect) {
        final ReplacementEffect replacementEffectCopy = replacementEffect.getCopy(); // doubtful - every caller provides a newly parsed instance, why copy?
        replacementEffectCopy.setHostCard(this);
        getCharacteristics().getReplacementEffects().add(replacementEffectCopy);
        return replacementEffectCopy;
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
        resetPreventNextDamage();
        resetPreventNextDamageWithEffect();
        resetReceivedDamageFromThisTurn();
        resetDealtDamageToThisTurn();
        resetDealtDamageToPlayerThisTurn();
        getDamageHistory().newTurn();
        setRegeneratedThisTurn(0);
        setBecameTargetThisTurn(false);
        clearMustBlockCards();
        getDamageHistory().setCreatureAttackedLastTurnOf(turn, getDamageHistory().getCreatureAttackedThisTurn());
        getDamageHistory().setCreatureAttackedThisTurn(false);
        getDamageHistory().setCreatureAttacksThisTurn(0);
        getDamageHistory().setCreatureBlockedThisTurn(false);
        getDamageHistory().setCreatureGotBlockedThisTurn(false);
        clearBlockedByThisTurn();
        clearBlockedThisTurn();
    }

    public boolean hasETBTrigger() {
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

    public boolean canBeShownTo(final Player viewer) {
        if (viewer == null) { return false; }

        Zone zone = getZone();
        if (zone == null) { return true; } //cards outside any zone are visible to all

        final Player controller = getController();
        switch (zone.getZoneType()) {
        case Ante:
        case Command:
        case Exile:
        case Battlefield:
        case Graveyard:
        case Stack:
            //cards in these zones are visible to all
            return true;
        case Hand:
            if (controller.hasKeyword("Play with your hand revealed.")) {
                return true;
            }
            //fall through
        case Sideboard:
            //face-up cards in these zones are hidden to opponents unless they specify otherwise
            if (controller.isOpponentOf(viewer) && !hasKeyword("Your opponent may look at this card.")) {
                break;
            }
            return true;
        case Library:
        case PlanarDeck:
            //cards in these zones are hidden to all unless they specify otherwise
            if (controller == viewer && hasKeyword("You may look at this card.")) {
                return true;
            }
            if (controller.isOpponentOf(viewer) && hasKeyword("Your opponent may look at this card.")) {
                return true;
            }
            break;
        case SchemeDeck:
            // true for now, to actually see the Scheme cards (can't see deck anyway)
            return true;
        }

        // special viewing permissions for viewer
        if (mayLookAt.contains(viewer)) {
            return true;
        }

        //if viewer is controlled by another player, also check if card can be shown to that player
        if (controller.isMindSlaved() && viewer == controller.getMindSlaveMaster()) {
            return canBeShownTo(controller);
        }

        return false;
    }

    public boolean canCardFaceBeShownTo(final Player viewer) {
        if (!isFaceDown()) {
            return true;
        }
        if (viewer.hasKeyword("CanSeeOpponentsFaceDownCards")) {
            return true;
        }

        // special viewing permissions for viewer
        if (mayLookAt.contains(viewer)) {
            return true;
        }

        //if viewer is controlled by another player, also check if face can be shown to that player
        if (viewer.isMindSlaved() && canCardFaceBeShownTo(viewer.getMindSlaveMaster())) {
            return true;
        }
        return !getController().isOpponentOf(viewer) || hasKeyword("Your opponent may look at this card.");
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
        }
        else {
            requestedCMC = getManaCost().getCMC() + xPaid;
        }
        return requestedCMC;
    }

    public final boolean canBeSacrificedBy(final SpellAbility source) {
        if (isImmutable()) {
            System.out.println("Trying to sacrifice immutables: " + this);
            return false;
        }
        if (isPhasedOut()) {
            return false;
        }
        if (source != null && getController().isOpponentOf(source.getActivatingPlayer())
                && getController().hasKeyword("Spells and abilities your opponents control can't cause you to sacrifice permanents.")) {
            return false;
        }
        return true;
    }

    public CardRules getRules() {
        return cardRules;
    }
    public void setRules(CardRules r) {
        cardRules = r;
    }

    public boolean isCommander() {
        return isCommander;
    }

    public void setCommander(boolean b) {
        isCommander = b;
    }

    public void setSplitStateToPlayAbility(SpellAbility sa) {
        if (!isSplitCard()) return; // just in case
        // Split card support
        for (SpellAbility a : getState(CardCharacteristicName.LeftSplit).getSpellAbility()) {
            if (sa == a || sa.getDescription().equals(String.format("%s (without paying its mana cost)", a.getDescription()))) {
                setState(CardCharacteristicName.LeftSplit, true);
                return;
            }
        }
        for (SpellAbility a : getState(CardCharacteristicName.RightSplit).getSpellAbility()) {
            if (sa == a || sa.getDescription().equals(String.format("%s (without paying its mana cost)", a.getDescription()))) {
                setState(CardCharacteristicName.RightSplit, true);
                return;
            }
        }
        if (sa.getHostCard().hasKeyword("Fuse")) // it's ok that such card won't change its side
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
        if (controller != null) {
            return controller.getGame();
        }
        Player owner = getOwner();
        if (owner != null) {
            return owner.getGame();
        }
        return null;
    }

    public List<SpellAbility> getAllPossibleAbilities(Player player, boolean removeUnplayable) {
        // this can only be called by the Human

        final List<SpellAbility> abilities = new ArrayList<SpellAbility>();
        for (SpellAbility sa : getSpellAbilities()) {
            //add alternative costs as additional spell abilities
            abilities.add(sa);
            abilities.addAll(GameActionUtil.getAlternativeCosts(sa));
        }

        for (int i = abilities.size() - 1; i >= 0; i--) {
            SpellAbility sa = abilities.get(i);
            sa.setActivatingPlayer(player);
            if (removeUnplayable && !sa.canPlay()) {
                abilities.remove(i);
            }
            else if (!sa.isPossible()) {
                abilities.remove(i);
            }
        }

        if (isLand() && player.canPlayLand(this)) {
            Ability.PLAY_LAND_SURROGATE.setHostCard(this);
            abilities.add(Ability.PLAY_LAND_SURROGATE);
        }

        return abilities;
    }

    public static Card fromPaperCard(IPaperCard pc, Player owner) {
        return CardFactory.getCard(pc, owner);
    }

    private static final Map<PaperCard, Card> cp2card = new HashMap<PaperCard, Card>();
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
        final String set = getCurSetCode();

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
        return rules != null ? rules.getOracleText() : "";
    }

    @Override
    public CardView getView() {
        return view;
    }
}
