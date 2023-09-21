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
package forge.game.spellability;

import java.util.*;

import forge.game.cost.CostSacrifice;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

import com.google.common.collect.*;

import forge.GameCommand;
import forge.card.CardStateName;
import forge.card.ColorSet;
import forge.card.MagicColor;
import forge.card.mana.ManaAtom;
import forge.card.mana.ManaCost;
import forge.game.CardTraitBase;
import forge.game.ForgeScript;
import forge.game.Game;
import forge.game.GameActionUtil;
import forge.game.GameEntity;
import forge.game.GameEntityCounterTable;
import forge.game.GameObject;
import forge.game.IHasSVars;
import forge.game.IIdentifiable;
import forge.game.ability.AbilityKey;
import forge.game.ability.AbilityUtils;
import forge.game.ability.ApiType;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CardCollectionView;
import forge.game.card.CardDamageMap;
import forge.game.card.CardFactory;
import forge.game.card.CardPlayOption;
import forge.game.card.CardPredicates;
import forge.game.card.CardZoneTable;
import forge.game.cost.Cost;
import forge.game.cost.CostPart;
import forge.game.cost.CostTap;
import forge.game.event.GameEventCardStatsChanged;
import forge.game.keyword.Keyword;
import forge.game.mana.Mana;
import forge.game.mana.ManaCostBeingPaid;
import forge.game.phase.Untap;
import forge.game.player.Player;
import forge.game.player.PlayerCollection;
import forge.game.replacement.ReplacementEffect;
import forge.game.staticability.StaticAbility;
import forge.game.staticability.StaticAbilityCastWithFlash;
import forge.game.staticability.StaticAbilityMustTarget;
import forge.game.trigger.Trigger;
import forge.game.trigger.TriggerType;
import forge.game.zone.ZoneType;
import forge.util.Aggregates;
import forge.util.CardTranslation;
import forge.util.Lang;
import forge.util.Localizer;
import forge.util.TextUtil;

//only SpellAbility can go on the stack
//override any methods as needed
/**
 * <p>
 * Abstract SpellAbility class.
 * </p>
 *
 * @author Forge
 * @version $Id$
 */
public abstract class SpellAbility extends CardTraitBase implements ISpellAbility, IIdentifiable, Comparable<SpellAbility> {
    private static int maxId = 0;
    private static int nextId() { return ++maxId; }

    public static class EmptySa extends SpellAbility {
        public EmptySa(Card sourceCard) { super(sourceCard, Cost.Zero); setActivatingPlayer(sourceCard.getController());}
        public EmptySa(ApiType api0, Card sourceCard) { super(sourceCard, Cost.Zero); setActivatingPlayer(sourceCard.getController()); api = api0;}
        public EmptySa(Card sourceCard, Player activator) { super(sourceCard, Cost.Zero); setActivatingPlayer(activator);}
        public EmptySa(ApiType api0, Card sourceCard, Player activator) { super(sourceCard, Cost.Zero); setActivatingPlayer(activator); api = api0;}
        @Override public void resolve() {}
        @Override public boolean canPlay() { return false; }
    }

    private int id;

    // choices for constructor isPermanent argument
    private String originalDescription = "", description = "";
    private String originalStackDescription = "", stackDescription = "";
    private ManaCost multiKickerManaCost;
    private Player activatingPlayer;
    private Player targetingPlayer;
    private Card playEffectCard;
    private Pair<Long, Player> controlledByPlayer;
    private ManaCostBeingPaid manaCostBeingPaid;
    private int spentPhyrexian = 0;
    private int paidLifeAmount = 0;

    private SpellAbility grantorOriginal;
    private StaticAbility grantorStatic;

    private CardCollection splicedCards = null;

    private boolean basicSpell = true;
    private Trigger triggerObj;
    private boolean optionalTrigger = false;
    private ReplacementEffect replacementEffect = null;
    private int sourceTrigger = -1;
    private List<Object> triggerRemembered = Lists.newArrayList();

    private AlternativeCost altCost = null;

    private boolean aftermath = false;

    private boolean blessing = false;
    private Integer chapter = null;
    private boolean lastChapter = false;

    /** The pay costs. */
    private Cost payCosts;
    private SpellAbilityRestriction restrictions;
    private SpellAbilityCondition conditions = new SpellAbilityCondition();
    private AbilitySub subAbility;

    private Map<String, SpellAbility> additionalAbilities = Maps.newHashMap();
    private Map<String, List<AbilitySub>> additionalAbilityLists = Maps.newHashMap();

    protected ApiType api = null;

    private List<Mana> payingMana = Lists.newArrayList();
    private List<SpellAbility> paidAbilities = Lists.newArrayList();
    private Integer xManaCostPaid = null;

    private TreeBasedTable<String, Boolean, CardCollection> paidLists = TreeBasedTable.create();

    private EnumMap<AbilityKey, Object> triggeringObjects = AbilityKey.newMap();

    private EnumMap<AbilityKey, Object> replacingObjects = AbilityKey.newMap();

    private final List<String> pipsToReduce = new ArrayList<>();

    private List<AbilitySub> chosenList = null;
    private CardCollection tappedForConvoke = new CardCollection();
    private Card sacrificedAsOffering;
    private Card sacrificedAsEmerge;

    private AbilityManaPart manaPart;

    private boolean undoable;

    private boolean isCopied = false;
    private boolean mayChooseNewTargets = false;

    private EnumSet<OptionalCost> optionalCosts = EnumSet.noneOf(OptionalCost.class);
    private TargetRestrictions targetRestrictions;
    private TargetChoices targetChosen = new TargetChoices();

    private Integer dividedValue = null;

    private SpellAbilityView view;

    private CardPlayOption mayPlay;

    private CardCollection lastStateBattlefield;
    private CardCollection lastStateGraveyard;

    private CardCollection rollbackEffects = new CardCollection();

    private CardDamageMap damageMap;
    private CardDamageMap preventMap;
    private GameEntityCounterTable counterTable;
    private CardZoneTable changeZoneTable;
    private Map<Player, Integer> loseLifeMap;

    public CardCollection getLastStateBattlefield() {
        return lastStateBattlefield;
    }
    public void setLastStateBattlefield(final CardCollectionView lastStateBattlefield) {
        this.lastStateBattlefield = new CardCollection(lastStateBattlefield);
    }

    public CardCollection getLastStateGraveyard() {
        return lastStateGraveyard;
    }
    public void setLastStateGraveyard(final CardCollectionView lastStateGraveyard) {
        this.lastStateGraveyard = new CardCollection(lastStateGraveyard);
    }

    public void clearLastState() {
        lastStateBattlefield = null;
        lastStateGraveyard = null;
    }

    protected SpellAbility(final Card iSourceCard, final Cost toPay) {
        this(iSourceCard, toPay, null);
    }
    protected SpellAbility(final Card iSourceCard, final Cost toPay, SpellAbilityView view0) {
        id = nextId();
        hostCard = iSourceCard;
        payCosts = toPay;
        if (view0 == null) {
            view0 = new SpellAbilityView(this);
        }
        view = view0;
        if (!(this instanceof AbilitySub)) {
            restrictions = new SpellAbilityRestriction();
        }
    }

    @Override
    public final int getId() {
        return id;
    }
    @Override
    public int hashCode() {
        return Objects.hash(SpellAbility.class, getId());
    }
    @Override
    public boolean equals(final Object obj) {
        return obj instanceof SpellAbility && this.id == ((SpellAbility) obj).id;
    }

    @Override
    public void setHostCard(final Card c) {
        if (hostCard == c) { return; }
        super.setHostCard(c);

        if (manaPart != null) {
            manaPart.setSourceCard(c);
        }

        if (subAbility != null) {
            subAbility.setHostCard(c);
        }
        for (SpellAbility sa : additionalAbilities.values()) {
            if (sa.getHostCard() != c) {
                sa.setHostCard(c);
            }
        }
        for (List<AbilitySub> list : additionalAbilityLists.values()) {
            for (AbilitySub sa : list) {
                if (sa.getHostCard() != c) {
                    sa.setHostCard(c);
                }
            }
        }

        view.updateHostCard(this);
        view.updateDescription(this); //description can change if host card does
    }

    public boolean canThisProduce(final String s) {
        AbilityManaPart mp = getManaPart();
        if (mp != null && metConditions() && mp.canProduce(s, this)) {
            return true;
        }
        return false;
    }

    public boolean canProduce(final String s) {
        if (canThisProduce(s)) {
            return true;
        }

        return this.subAbility != null && this.subAbility.canProduce(s);
    }

    public boolean isManaAbilityFor(SpellAbility saPaidFor, byte colorNeeded) {
        // is root ability
        if (this.getParent() == null) {
            if (!canPlay()) {
                return false;
            }
            if (isAbility() && getRestrictions().isInstantSpeed()) {
                return false;
            }
        }

        AbilityManaPart mp = getManaPart();
        if (mp != null && metConditions() && mp.meetsManaRestrictions(saPaidFor) && mp.abilityProducesManaColor(this, colorNeeded) && saPaidFor.allowsPayingWithShard(mp.getSourceCard(), colorNeeded)) {
            return true;
        }
        return this.subAbility != null && this.subAbility.isManaAbilityFor(saPaidFor, colorNeeded);
    }

    public boolean isManaCannotCounter(SpellAbility saPaidFor) {
        AbilityManaPart mp = getManaPart();
        if (mp != null && metConditions() && mp.meetsManaRestrictions(saPaidFor) && mp.cannotCounterPaidWith(saPaidFor)) {
            return true;
        }
        return this.subAbility != null && this.subAbility.isManaCannotCounter(saPaidFor);
    }

    public int amountOfManaGenerated(boolean multiply) {
        int result = 0;
        AbilityManaPart mp = getManaPart();
        if (mp != null && metConditions()) {
            int amount = hasParam("Amount") ? AbilityUtils.calculateAmount(getHostCard(), getParam("Amount"), this) : 1;

            if (!multiply || mp.isAnyMana() || mp.isComboMana() || mp.isSpecialMana()) {
                result += amount;
            } else {
                // For cards that produce like {C}{R} vs cards that produce {R}{R}.
                result += mp.mana(this).split(" ").length * amount;
            }
        }
        return result;
    }

    public int totalAmountOfManaGenerated(SpellAbility saPaidFor, boolean multiply) {
        int result = 0;
        AbilityManaPart mp = getManaPart();
        if (mp != null && metConditions() && mp.meetsManaRestrictions(saPaidFor)) {
            result += amountOfManaGenerated(multiply);
        }
        result += subAbility != null ? subAbility.totalAmountOfManaGenerated(saPaidFor, multiply) : 0;
        return result;
    }

    public void setManaExpressChoice(ColorSet cs) {
        AbilityManaPart mp = getManaPart();
        if (mp != null) {
            mp.setExpressChoice(cs);
        }
        if (subAbility != null) {
            subAbility.setManaExpressChoice(cs);
        }
    }

    public final AbilityManaPart getManaPart() {
        return manaPart;
    }

    public final List<AbilityManaPart> getAllManaParts() {
        AbilityManaPart mp = getManaPart();
        if (mp == null && subAbility == null) {
            return ImmutableList.of();
        }
        List<AbilityManaPart> result = Lists.newArrayList();
        if (mp != null) {
            result.add(mp);
        }
        if (subAbility != null) {
            result.addAll(subAbility.getAllManaParts());
        }
        return result;
    }

    public final boolean isManaAbility() {
        // Check whether spell or ability first
        if (isSpell()) {
            return false;
        }
        // without a target
        if (usesTargeting()) { return false; }
        if (isPwAbility()) {
            return false; //Loyalty ability, not a mana ability.
        }
        // CR 605.1b
        if (isTrigger() && this.getTrigger().getMode() != TriggerType.TapsForMana && this.getTrigger().getMode() != TriggerType.ManaAdded) {
            return false;
        }

        SpellAbility tail = this;
        while (tail != null) {
            if (tail.manaPart != null) {
                return true;
            }
            tail = tail.getSubAbility();
        }
        return false;
    }

    protected final void setManaPart(AbilityManaPart manaPart0) {
        manaPart = manaPart0;
    }

    public boolean allowsPayingWithShard(Card src, byte shard) {
        if (!hasParam("ManaRestriction")) { return true; }
        String res = getParam("ManaRestriction");
        if (res.equals("None")) {
            return false;
        }
        if (res.equals("ChosenColor")) {
            return this.getHostCard().hasChosenColor() && shard == ManaAtom.fromName(this.getHostCard().getChosenColor());
        }
        if (!src.isValid(res, null, null, this)) {
            return false;
        }
        return true;
    }

    // Spell, and Ability, and other Ability objects override this method
    public abstract boolean canPlay();

    public boolean canPlay(boolean checkOptionalCosts) {
        if (canPlay()) {
            return true;
        }
        if (!checkOptionalCosts) {
            return false;
        }
        for (OptionalCostValue val : GameActionUtil.getOptionalCostValues(this)) {
            if (canPlayWithOptionalCost(val)) {
                return true;
            }
        }
        return false;
    }

    public boolean canPlayWithOptionalCost(OptionalCostValue opt) {
        return GameActionUtil.addOptionalCosts(this, Lists.newArrayList(opt)).canPlay();
    }

    public boolean isPossible() {
        return canPlay(); //by default, ability is only possible if it can be played
    }

    public boolean promptIfOnlyPossibleAbility() {
        return false; //by default, don't prompt user if ability is only possible ability
    }

    // all Spell's and Abilities must override this method
    public abstract void resolve();

    public ManaCost getMultiKickerManaCost() {
        return multiKickerManaCost;
    }
    public void setMultiKickerManaCost(final ManaCost cost) {
        multiKickerManaCost = cost;
    }

    public Player getActivatingPlayer() {
        return activatingPlayer;
    }
    public void setActivatingPlayer(final Player player) {
        setActivatingPlayer(player, false);
    }
    public boolean setActivatingPlayer(final Player player, final boolean lki) {
        // trickle down activating player
        boolean updated = false;
        // don't use equals because player might be from simulation
        if (player == null || player != activatingPlayer) {
            activatingPlayer = player;
            updated = true;
        }
        if (subAbility != null) {
            updated |= subAbility.setActivatingPlayer(player, lki);
        }
        for (SpellAbility sa : additionalAbilities.values()) {
            updated |= sa.setActivatingPlayer(player, lki);
        }
        for (List<AbilitySub> list : additionalAbilityLists.values()) {
            for (AbilitySub sa : list) {
                updated |= sa.setActivatingPlayer(player, lki);
            }
        }
        if (!lki && updated) {
            view.updateCanPlay(this, false);
        }
        return updated;
    }

    public Player getTargetingPlayer() {
        return targetingPlayer;
    }
    public void setTargetingPlayer(Player targetingPlayer0) {
        targetingPlayer = targetingPlayer0;
    }

    /**
     * @return returns who controls the controller of this sa when it is resolving (for Word of Command effect). Null means not being controlled by other
     */
    public Pair<Long, Player> getControlledByPlayer() {
        return controlledByPlayer;
    }
    /**
     * @param ts time stamp of the control player effect
     * @param controller the player who will control the controller of this sa
     */
    public void setControlledByPlayer(long ts, Player controller) {
        if (controller != null) {
            controlledByPlayer = Pair.of(ts, controller);
        } else {
            controlledByPlayer = null;
        }
    }

    public ManaCostBeingPaid getManaCostBeingPaid() {
        return manaCostBeingPaid;
    }
    public void setManaCostBeingPaid(ManaCostBeingPaid costBeingPaid) {
        manaCostBeingPaid = costBeingPaid;
    }

    public boolean isSpell() { return false; }
    public boolean isAbility() { return true; }
    public boolean isActivatedAbility() { return false; }

    public boolean isMorphUp() {
        return this.hasParam("MorphUp");
    }

    public boolean isCastFaceDown() {
        return false;
    }

    public boolean isManifestUp() {
        return hasParam("ManifestUp");
    }

    public boolean isCycling() {
        return isAlternativeCost(AlternativeCost.Cycling);
    }

    public boolean isBackup() {
        return this.hasParam("Backup");
    }

    public boolean isBoast() {
        return this.hasParam("Boast");
    }

    public boolean isNinjutsu() {
        return this.hasParam("Ninjutsu");
    }

    public boolean isCumulativeupkeep() {
        return hasParam("CumulativeUpkeep");
    }

    public boolean isEpic() {
        AbilitySub sub = this.getSubAbility();
        while (sub != null && !sub.hasParam("Epic")) {
            sub = sub.getSubAbility();
        }
        return sub != null && sub.hasParam("Epic");
    }

    // If this is not null, then ability was made in a factory
    public ApiType getApi() {
        return api;
    }

    public void setApi(ApiType apiType) {
        api = apiType;
    }

    public SpellAbility findSubAbilityByType(ApiType apiType) {
        SpellAbility sub = this.getSubAbility();
        while (sub != null) {
            if (apiType.equals(sub.getApi())) {
                return sub;
            }
            sub = sub.getSubAbility();
        }
        return null;
    }

    public final boolean isCurse() {
        return hasParam("IsCurse");
    }

    public final boolean isPwAbility() {
        // TODO try to check the Cost itself
        return hasParam("Planeswalker");
    }

    // begin - Input methods

    public Cost getPayCosts() {
        return payCosts;
    }
    public void setPayCosts(final Cost abCost) {
        payCosts = abCost;
    }

    public boolean costHasX() {
        return getPayCosts().hasXInAnyCostPart();
    }

    public boolean costHasManaX() {
        if (getPayCosts().hasNoManaCost()) {
            return false;
        }
        return getPayCosts().getCostMana().getAmountOfX() > 0;
    }

    public SpellAbilityRestriction getRestrictions() {
        return restrictions;
    }
    public void setRestrictions(final SpellAbilityRestriction restrict) {
        restrictions = restrict;
    }

    /**
     * Shortcut to see how many activations there were this turn.
     */
    public int getActivationsThisTurn() {
        return getHostCard().getAbilityActivatedThisTurn(this);
    }
    public int getActivationsThisGame() {
        return getHostCard().getAbilityActivatedThisGame(this);
    }
    public int getResolvedThisTurn() {
        return getHostCard().getAbilityResolvedThisTurn(this);
    }

    public SpellAbilityCondition getConditions() {
        return conditions;
    }
    public final void setConditions(final SpellAbilityCondition condition) {
        conditions = condition;
    }

    public boolean metConditions() {
        return getConditions() != null && getConditions().areMet(this);
    }

    public List<Mana> getPayingMana() {
        return payingMana;
    }
    public void setPayingMana(List<Mana> paying) {
        payingMana = Lists.newArrayList(paying);
    }
    public final void clearManaPaid() {
        payingMana.clear();
    }

    public final int getSpendPhyrexianMana() {
        return this.spentPhyrexian;
    }
    public final void setSpendPhyrexianMana(boolean bool) {
        this.spentPhyrexian = bool ? this.spentPhyrexian + 2 : 0;
    }

    public final int getAmountLifePaid() {
        return this.paidLifeAmount;
    }
    public final void setPaidLife(int value) {
        this.paidLifeAmount = value;
    }

    public final void applyPayingManaEffects() {
        Card host = getHostCard();

        for (Mana mana : getPayingMana()) {
            if (mana.triggersWhenSpent()) {
                mana.getManaAbility().addTriggersWhenSpent(this, host);
            }

            if (mana.addsCounters(this)) {
                mana.getManaAbility().createETBCounters(host, getActivatingPlayer());
            }

            if (mana.addsNoCounterMagic(this)) {
                mana.getManaAbility().addNoCounterEffect(this);
            }

            if (isSpell() && host != null) {
                if (mana.addsKeywords(this) && mana.addsKeywordsType()
                        && this.isValid(mana.getManaAbility().getAddsKeywordsType(),
                        mana.getSourceCard().getController(), mana.getSourceCard(), null)) {
                    final long timestamp = host.getGame().getNextTimestamp();
                    final List<String> kws = Arrays.asList(mana.getAddedKeywords().split(" & "));
                    host.addChangedCardKeywords(kws, null, false, timestamp, 0);
                    if (mana.addsKeywordsUntil()) {
                        final GameCommand untilEOT = new GameCommand() {
                            private static final long serialVersionUID = -8285169579025607693L;

                            @Override
                            public void run() {
                                host.removeChangedCardKeywords(timestamp, 0);
                                host.getGame().fireEvent(new GameEventCardStatsChanged(host));
                            }
                        };
                        String until = mana.getManaAbility().getAddsKeywordsUntil();
                        if ("UntilEOT".equals(until)) {
                            host.getGame().getEndOfTurn().addUntil(untilEOT);
                        }
                    }
                }
            }
        }
    }

    public ColorSet getPayingColors() {
        byte colors = 0;
        for (Mana m : payingMana) {
            colors |= m.getColor();
        }
        return ColorSet.fromMask(colors);
    }

    public List<SpellAbility> getPayingManaAbilities() {
        return paidAbilities;
    }

    // Combined PaidLists
    public TreeBasedTable<String, Boolean, CardCollection> getPaidHash() {
        return paidLists;
    }
    public void setPaidHash(final TreeBasedTable<String, Boolean, CardCollection> hash) {
        paidLists = TreeBasedTable.create(hash);
    }

    // use if it doesn't matter if payment was caused by extrinsic cost modifier
    public Iterable<Card> getPaidList(final String str) {
        return Iterables.concat(paidLists.row(str).values());
    }

    public CardCollection getPaidList(final String str, final boolean intrinsic) {
        return paidLists.get(str, intrinsic);
    }

    public void addCostToHashList(final Card c, final String str, final boolean intrinsic) {
        if (!paidLists.contains(str, intrinsic)) {
            paidLists.put(str, intrinsic, new CardCollection());
        }
        paidLists.get(str, intrinsic).add(c);
    }

    public void resetPaidHash() {
        paidLists.clear();
    }

    public Iterable<OptionalCost> getOptionalCosts() {
        return optionalCosts;
    }
    public final void addOptionalCost(OptionalCost cost) {
        // Optional costs are added to swallow copies of original SAs,
        // Thus, to protect the original's set from changes, we make a copy right here.
        optionalCosts = EnumSet.copyOf(optionalCosts);
        optionalCosts.add(cost);
        if (!cost.getPip().equals("")) {
            pipsToReduce.add(cost.getPip());
        }
    }

    public boolean isBargain() {
        return isOptionalCostPaid(OptionalCost.Bargain);
    }

    public boolean isBuyBackAbility() {
        return isOptionalCostPaid(OptionalCost.Buyback);
    }

    public boolean isKicked() {
        return isOptionalCostPaid(OptionalCost.Kicker1) || isOptionalCostPaid(OptionalCost.Kicker2) ||
            getHostCard().getKickerMagnitude() > 0;
    }

    public boolean isEntwine() {
        return isOptionalCostPaid(OptionalCost.Entwine);
    }

    public boolean isJumpstart() {
        return isOptionalCostPaid(OptionalCost.Jumpstart);
    }

    public boolean isOptionalCostPaid(OptionalCost cost) {
        SpellAbility saRoot = getRootAbility();
        return saRoot.optionalCosts.contains(cost);
    }

    public Map<AbilityKey, Object> getTriggeringObjects() {
        return triggeringObjects;
    }
    public void setTriggeringObjects(final Map<AbilityKey, Object> triggeredObjects) {
        triggeringObjects = AbilityKey.newMap(triggeredObjects);
    }
    public Object getTriggeringObject(final AbilityKey type) {
        return triggeringObjects.get(type);
    }
    public void setTriggeringObject(final AbilityKey type, final Object o) {
        triggeringObjects.put(type, o);
    }

    public void setTriggeringObjectsFrom(final Map<AbilityKey, Object> runParams, final AbilityKey... types) {
        int typesLength = types.length;
        for (int i = 0; i < typesLength; i += 1) {
            AbilityKey type = types[i];
            if (runParams.containsKey(type)) {
                triggeringObjects.put(type, runParams.get(type));
            }
        }
    }

    public boolean hasTriggeringObject(final AbilityKey type) {
        return triggeringObjects.containsKey(type);
    }
    public void resetTriggeringObjects() {
        triggeringObjects = AbilityKey.newMap();
    }

    @Override
    public List<Object> getTriggerRemembered() {
        return triggerRemembered;
    }
    public void setTriggerRemembered(List<Object> list) {
        triggerRemembered = list;
    }
    public void resetTriggerRemembered() {
        triggerRemembered = Lists.newArrayList();
    }

    public Map<AbilityKey, Object> getReplacingObjects() {
        return replacingObjects;
    }
    public Object getReplacingObject(final AbilityKey type) {
        return replacingObjects.get(type);
    }

    public void setReplacingObject(final AbilityKey type, final Object o) {
        replacingObjects.put(type, o);
    }
    public void setReplacingObjectsFrom(final Map<AbilityKey, Object> repParams, final AbilityKey... types) {
        int typesLength = types.length;
        for (int i = 0; i < typesLength; i += 1) {
            AbilityKey type = types[i];
            if (repParams.containsKey(type)) {
                setReplacingObject(type, repParams.get(type));
            }
        }
    }

    public void resetOnceResolved() {
        //resetPaidHash(); // FIXME: if uncommented, breaks Dragon Presence, e.g. Orator of Ojutai + revealing a Dragon from hand.
                           // Is it truly necessary at this point? The paid hash seems to be reset on all SA instance operations.
        // Epic spell keeps original targets
        if (!isEpic()) {
            resetTargets();
        }
        resetTriggeringObjects();
        resetTriggerRemembered();

        if (isActivatedAbility()) {
            setXManaCostPaid(null);
        }
    }

    // key for autoyield - the card description (including number) (if there is a card) plus the effect description
    public String yieldKey() {
        if (getHostCard() != null) {
            return getHostCard().toString() + ": " + toUnsuppressedString();
        } else {
            return toUnsuppressedString();
        }
    }

    public String getStackDescription() {
        String text = getHostCard().getView().getText();
        if (stackDescription.equals(text) && !text.isEmpty()) {
            return getHostCard().getName() + " - " + text;
        }
        return TextUtil.fastReplace(stackDescription, "CARDNAME", getHostCard().getName());
    }
    public void setStackDescription(final String s) {
        originalStackDescription = s;
        stackDescription = originalStackDescription;
        if (StringUtils.isEmpty(description) && StringUtils.isEmpty(hostCard.getView().getText())) {
            setDescription(s);
        }
    }

    public String getOriginalStackDescription() {
        return originalStackDescription;
    }

    // setDescription() includes mana cost and everything like
    // "G, tap: put target creature from your hand onto the battlefield"
    public String getDescription() {
        return description;
    }
    public void setDescription(final String s) {
        originalDescription = TextUtil.fastReplace(s, "VERT", "|");
        description = originalDescription;
    }

    public String getOriginalDescription() {
        return originalDescription;
    }

    public String getCostDescription() {
        if (payCosts == null || (this instanceof AbilitySub)) { // SubAbilities don't have Costs or Cost
            return "";
        } else {
            StringBuilder sb = new StringBuilder();
            // descriptors
            if (hasParam("PrecostDesc")) {
                sb.append(getParam("PrecostDesc")).append(" ");
            }
            if (hasParam("CostDesc")) {
                sb.append(getParam("CostDesc")).append(" ");
            } else {
                if (hasParam("AlternateCost")) {
                    Cost alternateCost = new Cost(getParam("AlternateCost"), payCosts.isAbility());
                    boolean altOnlyMana = alternateCost.isOnlyManaCost();
                    if (payCosts.isOnlyManaCost() && !altOnlyMana) {
                        sb.append("Pay ");
                    }
                    sb.append(payCosts.toString());
                    sb.append(" or ").append(altOnlyMana ? alternateCost.toString() :
                            StringUtils.uncapitalize(alternateCost.toString()));
                    sb.append(isEquip() && !altOnlyMana ? "." : "");
                } else {
                    sb.append(payCosts.toString());
                }

                if (payCosts.isAbility() && !isEquip()) {
                    sb.append(": ");
                }
            }

            return sb.toString();
        }
    }

    public void rebuiltDescription() {
        final StringBuilder sb = new StringBuilder();

        // SubAbilities don't have Costs or Cost descriptors
        sb.append(getCostDescription());

        sb.append(getParam("SpellDescription"));
        setDescription(sb.toString());
    }

    /** {@inheritDoc} */
    @Override
    public final String toString() {
        if (isSuppressed()) {
            return "";
        }
        return toUnsuppressedString();
    }

    public String toUnsuppressedString() {
        final StringBuilder sb = new StringBuilder();
        SpellAbility node = this;

        while (node != null) {
            if (node != this) {
                sb.append(" ");
            }
            String desc = node.getDescription();
            if (node.getHostCard() != null) {
                String currentName;
                // if alternate state is viewed while card uses original
                if (node.isIntrinsic() && node.cardState != null && node.cardState.getCard() == node.getHostCard()) {
                    currentName = node.cardState.getName();
                } else {
                    currentName = node.getHostCard().getName();
                }
                desc = CardTranslation.translateMultipleDescriptionText(desc, currentName);
                desc = TextUtil.fastReplace(desc, "CARDNAME", CardTranslation.getTranslatedName(currentName));
                desc = TextUtil.fastReplace(desc, "NICKNAME", Lang.getInstance().getNickName(CardTranslation.getTranslatedName(currentName)));
                if (node.getOriginalHost() != null) {
                    desc = TextUtil.fastReplace(desc, "ORIGINALHOST", node.getOriginalHost().getName());
                }
                sb.append(desc);
            }
            node = node.getSubAbility();
        }
        return sb.toString();
    }

    public AbilitySub getSubAbility() {
        return subAbility;
    }
    public void setSubAbility(final AbilitySub subAbility0) {
        if (subAbility == subAbility0) { return; }
        subAbility = subAbility0;
        if (subAbility != null) {
            subAbility.setParent(this);
        }
        view.updateDescription(this); //description changes when sub-abilities change
    }

    public Map<String, SpellAbility> getAdditionalAbilities() {
        return additionalAbilities;
    }
    public SpellAbility getAdditionalAbility(final String name) {
        if (hasAdditionalAbility(name)) {
            return additionalAbilities.get(name);
        }
        return null;
    }

    public boolean hasAdditionalAbility(final String name) {
        return additionalAbilities.containsKey(name);
    }

    public void setAdditionalAbility(final String name, final SpellAbility sa) {
        if (sa == null) {
            additionalAbilities.remove(name);
        } else {
            if (sa instanceof AbilitySub) {
                ((AbilitySub)sa).setParent(this);
            }
            additionalAbilities.put(name, sa);
        }
        view.updateDescription(this); //description changes when sub-abilities change
    }

    public Map<String, List<AbilitySub>> getAdditionalAbilityLists() {
        return additionalAbilityLists;
    }
    public List<AbilitySub> getAdditionalAbilityList(final String name) {
        if (additionalAbilityLists.containsKey(name)) {
            return additionalAbilityLists.get(name);
        } else {
            return ImmutableList.of();
        }
    }

    public void setAdditionalAbilityList(final String name, final List<AbilitySub> list) {
        if (list == null || list.isEmpty()) {
            additionalAbilityLists.remove(name);
        } else {
            List<AbilitySub> result = Lists.newArrayList(list);
            for (AbilitySub sa : result) {
                sa.setParent(this);
            }
            additionalAbilityLists.put(name, result);
        }
        view.updateDescription(this);
    }
    public void appendSubAbility(final AbilitySub toAdd) {
        SpellAbility tailend = this;
        while (tailend.getSubAbility() != null) {
            tailend = tailend.getSubAbility();
        }
        tailend.setSubAbility(toAdd);
    }

    public boolean isBasicSpell() {
        return basicSpell && this.altCost == null && getRootAbility().optionalCosts.isEmpty();
    }
    public void setBasicSpell(final boolean basicSpell0) {
        basicSpell = basicSpell0;
    }

    public boolean isFlashBackAbility() {
        return this.isAlternativeCost(AlternativeCost.Flashback);
    }

    public boolean isForetelling() {
        return false;
    }
    public boolean isForetold() {
        return this.isAlternativeCost(AlternativeCost.Foretold);
    }

    /**
     * @return the aftermath
     */
    public boolean isAftermath() {
        return aftermath;
    }

    /**
     * @param aftermath the aftermath to set
     */
    public void setAftermath(boolean aftermath) {
        this.aftermath = aftermath;
    }

    public boolean isOutlast() {
        return isAlternativeCost(AlternativeCost.Outlast);
    }

    public boolean isEquip() {
        return hasParam("Equip");
    }

    public boolean isBlessing() {
        return blessing;
    }
    public void setBlessing(boolean blessing0) {
        blessing = blessing0;
    }

    public boolean isChapter() {
        return chapter != null;
    }

    public Integer getChapter() {
        return chapter;
    }

    public void setChapter(int val) {
        chapter = val;
    }

    public boolean isLastChapter() {
        return lastChapter;
    }
    public boolean setLastChapter(boolean value) {
        return lastChapter = value;
    }

    public CardPlayOption getMayPlayOption() {
        return mayPlay;
    }
    public StaticAbility getMayPlay() {
        return mayPlay != null ? mayPlay.getAbility() : null;
    }
    public void setMayPlay(final CardPlayOption sta) {
        mayPlay = sta;
    }

    public boolean isAdventure() {
        return this.getCardStateName() == CardStateName.Adventure;
    }

    public SpellAbility copy() {
        return copy(hostCard, false);
    }
    public SpellAbility copy(Player activ) {
        return copy(hostCard, activ, false);
    }
    public SpellAbility copy(Card host, final boolean lki) {
        return copy(host, this.getActivatingPlayer(), lki);
    }
    public SpellAbility copy(Card host, Player activ, final boolean lki) {
        SpellAbility clone = null;
        try {
            clone = (SpellAbility) clone();
            clone.id = lki ? id : nextId();
            clone.view = new SpellAbilityView(clone, lki || host.getGame() == null ? null : host.getGame().getTracker());

            // don't use setHostCard to not trigger the not copied parts yet

            copyHelper(clone, host);

            // always set this to false, it is only set in CopyEffect
            clone.mayChooseNewTargets = false;

            clone.triggeringObjects = AbilityKey.newMap(this.triggeringObjects);

            clone.setPayCosts(getPayCosts().copy());
            if (manaPart != null) {
                clone.manaPart = new AbilityManaPart(clone, mapParams);
            }

            // need to copy the damage tables
            if (damageMap != null) {
                clone.damageMap = new CardDamageMap(damageMap);
            }
            if (preventMap != null) {
                clone.preventMap = new CardDamageMap(preventMap);
            }
            if (counterTable != null) {
                clone.counterTable = new GameEntityCounterTable(counterTable);
            }
            if (changeZoneTable != null) {
                clone.changeZoneTable = new CardZoneTable(changeZoneTable);
            }

            clone.payingMana = Lists.newArrayList(payingMana);
            clone.paidAbilities = Lists.newArrayList();
            clone.setPaidHash(getPaidHash());

            // copy last chapter flag for Trigger
            clone.lastChapter = this.lastChapter;

            if (usesTargeting()) {
                // the targets need to be cloned, otherwise they might be cleared
                clone.targetChosen = getTargets().clone();
            }

            // clear maps for copy, the values will be added later
            clone.additionalAbilities = Maps.newHashMap();
            clone.additionalAbilityLists = Maps.newHashMap();
            // run special copy Ability to make a deep copy
            CardFactory.copySpellAbility(this, clone, host, activ, lki);
        } catch (final CloneNotSupportedException e) {
            System.err.println(e);
        }
        return clone;
    }

    public SpellAbility copyWithNoManaCost() {
        return copyWithNoManaCost(getActivatingPlayer());
    }
    public SpellAbility copyWithNoManaCost(Player active) {
        final SpellAbility newSA = copy(active);
        if (newSA == null) {
            return null; // the ability was not copyable, e.g. a Suspend SA may get here
        }
        newSA.setPayCosts(newSA.getPayCosts().copyWithNoMana());
        if (!newSA.hasParam("WithoutManaCost")) {
            newSA.mapParams.put("WithoutManaCost", "True");
        }
        newSA.setDescription(newSA.getDescription() + " (without paying its mana cost)");

        return newSA;
    }

    public SpellAbility copyWithDefinedCost(Cost abCost) {
        final SpellAbility newSA = copy();
        newSA.setPayCosts(abCost);
        return newSA;
    }

    public SpellAbility copyWithDefinedCost(String abCost) {
        return copyWithDefinedCost(new Cost(abCost, isAbility()));
    }

    public SpellAbility copyWithManaCostReplaced(Player active, Cost abCost) {
        final SpellAbility newSA = copy(active);
        if (newSA == null) {
            return null; // the ability was not copyable, e.g. a Suspend SA may get here
        }
        final Cost newCost = newSA.getPayCosts().copyWithNoMana();
        newCost.add(abCost);
        newSA.setPayCosts(newCost);
        return newSA;
    }

    public boolean isTrigger() {
        return getTrigger() != null;
    }

    public Trigger getTrigger() {
        if (getParent() != null) {
            return getParent().getTrigger();
        }
        return triggerObj;
    }

    public void setTrigger(final Trigger t) {
        triggerObj = t;
    }

    public boolean isOptionalTrigger() {
        return optionalTrigger;
    }
    public void setOptionalTrigger(final boolean optrigger) {
        optionalTrigger = optrigger;
    }

    public int getSourceTrigger() {
        return sourceTrigger;
    }
    public void setSourceTrigger(final int id) {
        sourceTrigger = id;
    }

    public boolean isReplacementAbility() {
        return getParent() != null ? getParent().isReplacementAbility() : replacementEffect != null;
    }

    public ReplacementEffect getReplacementEffect() {
        if (getParent() != null) {
            return getParent().getReplacementEffect();
        }
        return replacementEffect;
    }

    public void setReplacementEffect(final ReplacementEffect re) {
        this.replacementEffect = re;
    }

    public boolean isMandatory() {
        return isTrigger() && !isOptionalTrigger();
    }

    public final boolean canTarget(final GameObject entity) {
        if (entity == null) {
            return false;
        }

        final SpellAbility rootAbility = this.getRootAbility();
        // 115.5. A spell or ability on the stack is an illegal target for itself.
        // (This covers the spell case.)
        if (rootAbility.isSpell() && rootAbility.getHostCard() == entity) {
            return false;
        }

        // Restriction related to this ability
        if (usesTargeting()) {
            final TargetRestrictions tr = getTargetRestrictions();
            if (tr.isUniqueTargets() && getUniqueTargets().contains(entity))
                return false;

            // If the cards must have a specific controller
            if (hasParam("TargetsWithDefinedController") && entity instanceof Card) {
                final Card c = (Card) entity;
                List<Player> pl = AbilityUtils.getDefinedPlayers(getHostCard(), getParam("TargetsWithDefinedController"), this);
                if (pl == null || !pl.contains(c.getController()) ) {
                    return false;
                }
            }
            if (hasParam("TargetsWithSharedCardType") && entity instanceof Card) {
                final Card c = (Card) entity;
                CardCollection pl = AbilityUtils.getDefinedCards(getHostCard(), getParam("TargetsWithSharedCardType"), this);
                for (final Card crd : pl) {
                    // one of those types
                    if (hasParam("TargetsWithSharedTypes")) {
                        boolean flag = false;
                        for (final String type : getParam("TargetsWithSharedTypes").split(",")) {
                            if (c.getType().hasStringType(type) && crd.getType().hasStringType(type)) {
                                flag = true;
                                break;
                            }
                        }
                        if (!flag) {
                            return false;
                        }
                    } else {
                        if (!c.sharesCardTypeWith(crd)) {
                            return false;
                        }
                    }
                }
            }

            if (hasParam("TargetsWithControllerProperty") && entity instanceof Card) {
                final String prop = getParam("TargetsWithControllerProperty");
                final Card c = (Card) entity;
                if (prop.equals("cmcLECardsInGraveyard")
                        && c.getCMC() > c.getController().getCardsIn(ZoneType.Graveyard).size()) {
                    return false;
                }
            }
            if (hasParam("TargetsWithRelatedProperty") && entity instanceof Card) {
                final String related = getParam("TargetsWithRelatedProperty");
                final Card c = (Card) entity;
                Card parentTarget = null;
                for (GameObject o : getUniqueTargets()) {
                    if (o instanceof Card) {
                        parentTarget = (Card) o;
                        break;
                    }
                }
                if (parentTarget == null) {
                    return false;
                }
                switch (related) {
                case "LEPower" :
                    if (c.getNetPower() > parentTarget.getNetPower()) {
                        return false;
                    }
                    break;
                case "LECMC" :
                    if (c.getCMC() > parentTarget.getCMC()) {
                        return false;
                    }
                }
            }

            if (hasParam("TargetingPlayerControls") && entity instanceof Card) {
                final Card c = (Card) entity;
                if (!c.getController().equals(getTargetingPlayer())) {
                    return false;
                }
            }

            if (hasParam("MaxTotalTargetCMC") && entity instanceof Card) {
                int soFar = Aggregates.sum(getTargets().getTargetCards(), CardPredicates.Accessors.fnGetCmc);
                // only add if it isn't already targeting
                if (!isTargeting(entity)) {
                    final Card c = (Card) entity;
                    soFar += c.getCMC();
                }

                if (soFar > tr.getMaxTotalCMC(getHostCard(), this)) {
                    return false;
                }
            }

            if (hasParam("MaxTotalTargetPower") && entity instanceof Card) {
                int soFar = Aggregates.sum(getTargets().getTargetCards(), CardPredicates.Accessors.fnGetNetPower);
                // only add if it isn't already targeting
                if (!isTargeting(entity)) {
                    final Card c = (Card) entity;
                    soFar += c.getNetPower();
                }

                if (soFar > tr.getMaxTotalPower(getHostCard(), this)) {
                    return false;
                }
            }

            if (tr.isSameController() && entity instanceof Card) {
                Player newController;
                newController = ((Card) entity).getController();
                for (final Card c : targetChosen.getTargetCards()) {
                    if (entity != c && !c.getController().equals(newController))
                        return false;
                }
            }

            if (tr.isDifferentControllers() && entity instanceof Card) {
                Player newController;
                newController = ((Card) entity).getController();
                for (final Card c : targetChosen.getTargetCards()) {
                    if (entity != c && c.getController().equals(newController))
                        return false;
                }
            }

            if (tr.isWithoutSameCreatureType() && entity instanceof Card) {
                for (final Card c : targetChosen.getTargetCards()) {
                    if (entity != c && c.sharesCreatureTypeWith((Card) entity)) {
                        return false;
                    }
                }
            }

            if (tr.isWithSameCreatureType() && entity instanceof Card) {
                for (final Card c : targetChosen.getTargetCards()) {
                    if (entity != c && !c.sharesCreatureTypeWith((Card) entity)) {
                        return false;
                    }
                }
            }

            if (tr.isWithSameCardType() && entity instanceof Card) {
                for (final Card c : targetChosen.getTargetCards()) {
                    if (entity != c && !c.sharesCardTypeWith((Card) entity)) {
                        return false;
                    }
                }
            }

            if (entity instanceof GameEntity) {
                GameEntity e = (GameEntity)entity;
                if (!e.isValid(tr.getValidTgts(), getActivatingPlayer(), getHostCard(), this)) {
                    return false;
                }
                if (hasParam("TargetType") && !e.isValid(getParam("TargetType").split(","), getActivatingPlayer(), getHostCard(), this)) {
                    return false;
                }
            }

            if (entity instanceof Card) {
                final Card c = (Card) entity;
                if (c.getZone() != null && !tr.getZone().contains(c.getZone().getZoneType())) {
                    return false;
                }
            }
        }

        // Restrictions coming from target
        return entity.canBeTargetedBy(this);
    }

    // is this a wrapping ability (used by trigger abilities)
    public boolean isWrapper() {
        return false;
    }

    public final boolean isBestow() {
        return isAlternativeCost(AlternativeCost.Bestow);
    }

    public final boolean isBlitz() {
        return isAlternativeCost(AlternativeCost.Blitz);
    }

    public final boolean isDash() {
        return isAlternativeCost(AlternativeCost.Dash);
    }

    public final boolean isDisturb() {
        return isAlternativeCost(AlternativeCost.Disturb);
    }

    public final boolean isEscape() {
        return isAlternativeCost(AlternativeCost.Escape);
    }

    public final boolean isEvoke() {
        return isAlternativeCost(AlternativeCost.Evoke);
    }

    public final boolean isMadness() {
        return isAlternativeCost(AlternativeCost.Madness);
    }

    public final boolean isMutate() {
        return isAlternativeCost(AlternativeCost.Mutate);
    }

    public final boolean isProwl() {
        return isAlternativeCost(AlternativeCost.Prowl);
    }

    public final boolean isSurged() {
        return isAlternativeCost(AlternativeCost.Surge);
    }

    public final boolean isSpectacle() {
        return isAlternativeCost(AlternativeCost.Spectacle);
    }

    public List<String> getPipsToReduce() {
        return pipsToReduce;
    }
    public final void clearPipsToReduce() {
        pipsToReduce.clear();
    }

    public CardCollection getTappedForConvoke() {
        return tappedForConvoke;
    }
    public void addTappedForConvoke(final Card c) {
        if (tappedForConvoke == null) {
            tappedForConvoke = new CardCollection();
        }
        tappedForConvoke.add(c);
    }
    public void clearTappedForConvoke() {
        if (tappedForConvoke != null) {
            tappedForConvoke.clear();
        }
    }

    public boolean isEmerge() {
        return isAlternativeCost(AlternativeCost.Emerge);
    }

    public Card getSacrificedAsEmerge() {
        return sacrificedAsEmerge;
    }
    public void setSacrificedAsEmerge(final Card c) {
        sacrificedAsEmerge = c;
    }
    public void resetSacrificedAsEmerge() {
        sacrificedAsEmerge = null;
    }

    public boolean isOffering() {
        return isAlternativeCost(AlternativeCost.Offering);
    }

    public Card getSacrificedAsOffering() { //for Patron offering
        return sacrificedAsOffering;
    }
    public void setSacrificedAsOffering(final Card c) {
        sacrificedAsOffering = c;
    }
    public void resetSacrificedAsOffering() {
        sacrificedAsOffering = null;
    }

    public CardCollection getSplicedCards() {
        return splicedCards;
    }
    public void setSplicedCards(CardCollection splicedCards0) {
        splicedCards = splicedCards0;
    }
    public void addSplicedCards(Card splicedCard) {
        if (splicedCards == null) {
            splicedCards = new CardCollection();
        }
        splicedCards.add(splicedCard);
    }

    public CardCollection knownDetermineDefined(final String defined) {
        final CardCollection ret = new CardCollection();
        final CardCollection list = AbilityUtils.getDefinedCards(getHostCard(), defined, this);
        final Game game = getActivatingPlayer().getGame();

        for (final Card c : list) {
            final Card actualCard = game.getCardState(c);
            ret.add(actualCard);
        }
        return ret;
    }

    public SpellAbility getRootAbility() {
        SpellAbility parent = this;
        while (null != parent.getParent()) {
            parent = parent.getParent();
        }
        return parent;
    }

    public SpellAbility getParent() {
        return null;
    }

    protected IHasSVars getSVarFallback() {
        return ObjectUtils.firstNonNull(this.getParent(), super.getSVarFallback());
    }

    public boolean isUndoable() {
        return undoable && payCosts.isUndoable() && getHostCard().isInPlay();
    }

    public boolean undo() {
        if (isUndoable() && getActivatingPlayer().getManaPool().accountFor(getManaPart())) {
            payCosts.refundPaidCost(hostCard);
        }
        return false;
    }

    public void setUndoable(boolean b) {
        undoable = b;
    }

    public boolean isCopied() {
        return isCopied;
    }
    public void setCopied(boolean isCopied0) {
        isCopied = isCopied0;
        if (this.getSubAbility() != null) {
            this.getSubAbility().setCopied(isCopied0);
        }
    }

    public boolean isMayChooseNewTargets() {
        return mayChooseNewTargets;
    }
    public void setMayChooseNewTargets(boolean value) {
        mayChooseNewTargets = value;
    }

    /**
     * Returns whether variable was present in the announce list.
     */
    public boolean isAnnouncing(String variable) {
        String announce = getParam("Announce");
        if (StringUtils.isBlank(announce)) { return false; }

        String[] announcedOnes = TextUtil.split(announce, ',');
        for (String a : announcedOnes) {
            if (a.trim().equalsIgnoreCase(variable)) {
                return true;
            }
        }
        return false;
    }

    public void addAnnounceVar(String variable) {
        String announce = getParam("Announce");
        if (StringUtils.isBlank(announce)) {
            mapParams.put("Announce", variable);
            originalMapParams.put("Announce", variable);
            return;
        }
        String[] announcedOnes = TextUtil.split(announce, ',');
        for (String a : announcedOnes) {
            if (a.trim().equalsIgnoreCase(variable)) {
                return; //don't add announce variable that already exists
            }
        }
        mapParams.put("Announce", announce + ";" + variable);
        originalMapParams.put("Announce", announce + ";" + variable);
    }

    @Override
    public boolean canBeTargetedBy(SpellAbility sa) {
        return sa.canTargetSpellAbility(this);
    }

    public boolean usesTargeting() {
        return targetRestrictions != null;
    }

    public TargetRestrictions getTargetRestrictions() {
        return targetRestrictions;
    }

    /////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //
    // THE CODE BELOW IS RELATED TO TARGETING. It might be extracted to other class from here
    //
    /////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public void setTargetRestrictions(final TargetRestrictions tgt) {
        targetRestrictions = tgt;
    }

    /**
     * Gets the chosen target.
     *
     * @return the chosenTarget
     */
    public TargetChoices getTargets() {
        return targetChosen;
    }

    public void setTargets(TargetChoices targets) {
        // TODO should copy the target choices?
        targetChosen = targets;
    }

    public void resetTargets() {
        targetChosen = new TargetChoices();
    }

    /**
     * @return a boolean dividedAsYouChoose
     */
    public boolean isDividedAsYouChoose() {
        return hasParam("DividedAsYouChoose");
    }

    public final void addDividedAllocation(final GameObject tgt, final Integer portionAllocated) {
        getTargets().addDividedAllocation(tgt, portionAllocated);
    }
    public Integer getDividedValue(GameObject c) {
        return getTargets().getDividedValue(c);
    }

    public int getTotalDividedValue() {
        return getTargets().getTotalDividedValue();
    }

    public Integer getDividedValue() {
        return this.dividedValue;
    }

    public int getStillToDivide() {
        if (!isDividedAsYouChoose() || dividedValue == null) {
            return 0;
        }
        return dividedValue - getTotalDividedValue();
    }

    /**
     * Reset the first target.
     *
     */
    public void resetFirstTarget(GameObject c, SpellAbility originalSA) {
        SpellAbility sa = this;
        while (sa != null) {
            if (sa.usesTargeting()) {
                sa.resetTargets();
                sa.getTargets().add(c);
                if (!originalSA.getTargets().getDividedValues().isEmpty()) {
                    sa.addDividedAllocation(c, Iterables.getFirst(originalSA.getTargets().getDividedValues(), null));
                }
                break;
            }
            sa = sa.getSubAbility();
        }
    }

    /**
     * returns true if another target can be added
     * @return
     */
    public boolean canAddMoreTarget() {
        if (!this.usesTargeting()) {
            return false;
        }

        return getTargets().size() < getMaxTargets();
    }

    public boolean isZeroTargets() {
        return getMinTargets() == 0 && getTargets().isEmpty();
    }

    public boolean isMinTargetChosen() {
        return getTargetRestrictions().isMinTargetsChosen(hostCard, this);
    }
    public boolean isMaxTargetChosen() {
        return getTargetRestrictions().isMaxTargetsChosen(hostCard, this);
    }

    public int getMinTargets() {
        return getTargetRestrictions().getMinTargets(getHostCard(), this);
    }

    public int getMaxTargets() {
        return getTargetRestrictions().getMaxTargets(getHostCard(), this);
    }

    public boolean isTargetNumberValid() {
        if (!this.usesTargeting()) {
            return getTargets().isEmpty();
        }

        if (!isMinTargetChosen()) {
            return false;
        }

        return getMaxTargets() >= getTargets().size();
    }
    /**
     * <p>
     * getAllTargetChoices.
     * </p>
     *
     * @return a {@link java.util.ArrayList} object.
     * @since 1.0.15
     */
    public final List<TargetChoices> getAllTargetChoices() {
        final List<TargetChoices> res = Lists.newArrayList();
        SpellAbility sa = getRootAbility();

        while (sa != null) {
            if (sa.usesTargeting()) {
                res.add(sa.getTargets());
            }
            sa = sa.getSubAbility();
        }

        return res;
    }

    public Card getTargetCard() {
        return targetChosen.getFirstTargetedCard();
    }

    /**
     * <p>
     * Setter for the field <code>targetCard</code>.
     * </p>
     *
     * @param card
     *            a {@link forge.game.card.Card} object.
     */
    public void setTargetCard(final Card card) {
        if (card == null) {
            System.out.println(getHostCard()
                    + " - SpellAbility.setTargetCard() called with null for target card.");
            return;
        }

        resetTargets();
        targetChosen.add(card);
        setStackDescription(getHostCard().getName() + " - targeting " + card);
    }

    public void setPlayEffectCard(final Card card) {
        playEffectCard = card;
    }
    public Card getPlayEffectCard() {
        return playEffectCard;
    }

    /**
     * <p>
     * findTargetCards.
     * </p>
     *
     * @return a {@link forge.game.spellability.SpellAbility} object.
     */
    public CardCollectionView findTargetedCards() {
        // First search for targeted cards associated with current ability
        if (getTargets().isTargetingAnyCard()) {
            return getTargets().getTargetCards();
        }

        // Next search for source cards of targeted SAs associated with current ability
        if (getTargets().isTargetingAnySpell()) {
            CardCollection res = new CardCollection();
            for (final SpellAbility ability : getTargets().getTargetSpells()) {
                res.add(ability.getHostCard());
            }
            return res;
        }

        // Lastly Search parent SAs that targets a card
        SpellAbility parent = getParentTargetingCard();
        if (null != parent) {
            return parent.findTargetedCards();
        }

        // Lastly Search parent SAs that targets an SA
        parent = getParentTargetingSA();
        if (null != parent) {
            return parent.findTargetedCards();
        }

        return CardCollection.EMPTY;
    }

    public SpellAbility getSATargetingCard() {
        return getTargets().isTargetingAnyCard() ? this : getParentTargetingCard();
    }

    public SpellAbility getParentTargetingCard() {
        SpellAbility parent = getParent();
        while (parent != null) {
            if (parent.getTargets().isTargetingAnyCard()) {
                return parent;
            }
            parent = parent.getParent();
        }
        return null;
    }

    public SpellAbility getSATargetingSA() {
        return getTargets().isTargetingAnySpell() ? this : getParentTargetingSA();
    }

    public SpellAbility getParentTargetingSA() {
        SpellAbility parent = getParent();
        while (parent != null) {
            if (parent.getTargets().isTargetingAnySpell())
                return parent;
            parent = parent.getParent();
        }
        return null;
    }

    public SpellAbility getSATargetingPlayer() {
        return getTargets().isTargetingAnyPlayer() ? this : getParentTargetingPlayer();
    }

    public SpellAbility getParentTargetingPlayer() {
        SpellAbility parent = getParent();
        while (parent != null) {
            if (parent.getTargets().isTargetingAnyPlayer())
                return parent;
            parent = parent.getParent();
        }
        return null;
    }

    public final List<GameObject> getUniqueTargets() {
        final List<GameObject> targets = Lists.newArrayList();
        SpellAbility child = getParent();
        while (child != null) {
            if (child.usesTargeting()) {
                Iterables.addAll(targets, child.getTargets());
            }
            child = child.getParent();
        }
        return targets;
    }

    public boolean canTargetSpellAbility(final SpellAbility topSA) {
        final TargetRestrictions tgt = getTargetRestrictions();

        if (this.equals(topSA)) {
            return false;
        }

        if (hasParam("TargetType") && !topSA.isValid(getParam("TargetType").split(","), getActivatingPlayer(), getHostCard(), this)) {
            return false;
        }
        if (hasParam("TargetsWithControllerProperty")) {
            final String prop = getParam("TargetsWithControllerProperty");
            if (prop.equals("cmcLECardsInGraveyard")
                    && topSA.getHostCard().getCMC() > topSA.getActivatingPlayer().getCardsIn(ZoneType.Graveyard).size()) {
                return false;
            }
        }

        final String splitTargetRestrictions = tgt.getSAValidTargeting();
        if (splitTargetRestrictions != null) {
            boolean result = false;
            SpellAbility subAb = topSA;
            while (subAb != null && !result) {
                if (subAb.usesTargeting()) {
                    TargetChoices matchTgt = subAb.getTargets();
                    if (matchTgt == null) {
                        continue;
                    }
                    for (final GameObject o : matchTgt) {
                        // CR 115.9b need to check current target state but nothing else that could make it illegal
                        if (o.isValid(splitTargetRestrictions.split(","), getActivatingPlayer(), getHostCard(), this)) {
                            result = true;
                            break;
                        }
                    }
                }
                subAb = subAb.getSubAbility();
            }

            if (!result) {
                return false;
            }
        }

        Card host = topSA.getHostCard();
        // if from an effect it's usually Delayed Trigger
        if (host.isImmutable() && !host.isEmblem()) {
            if (host.getEffectSource() != null) {
                host = host.getEffectSource();
            } else {
                // or it could be the monarch, in that case it's only targetable if no source restriction
                return StringUtils.indexOfAny("Card", tgt.getValidTgts()) != -1;
            }
        }

        return host.isValid(tgt.getValidTgts(), getActivatingPlayer(), getHostCard(), this);
    }

    public boolean isTargeting(GameObject o) {
        if (getTargets().contains(o)) {
            return true;
        }
        SpellAbility p = getParent();
        return p != null && p.isTargeting(o);
    }

    public boolean setupNewTargets(Player forceTargetingPlayer) {
        // Skip to paying if parent ability doesn't target and has no subAbilities.
        // (or trigger case where its already targeted)
        SpellAbility currentAbility = this;
        do {
            if (currentAbility.usesTargeting()) {
                TargetChoices oldTargets = currentAbility.getTargets();
                if (forceTargetingPlayer.getController().chooseNewTargetsFor(currentAbility, null, true) == null) {
                    currentAbility.setTargets(oldTargets);
                }
            }
            final AbilitySub subAbility = currentAbility.getSubAbility();
            if (subAbility != null) {
                // This is necessary for "TargetsWithDefinedController$ ParentTarget"
                subAbility.setParent(currentAbility);
            }
            currentAbility = subAbility;
        } while (currentAbility != null);
        return true;
    }

    public boolean setupTargets() {
        // Skip to paying if parent ability doesn't target and has no subAbilities.
        // (or trigger case where its already targeted)
        SpellAbility currentAbility = this;
        final Card source = getHostCard();
        do {
            if (currentAbility.usesTargeting()) {
                currentAbility.clearTargets();
                Player targetingPlayer;
                if (currentAbility.hasParam("TargetingPlayer")) {
                    final PlayerCollection candidates = AbilityUtils.getDefinedPlayers(source, currentAbility.getParam("TargetingPlayer"), currentAbility);
                    // activator chooses targeting player
                    targetingPlayer = getActivatingPlayer().getController().chooseSingleEntityForEffect(
                            candidates, currentAbility, "Choose the targeting player", null);
                } else {
                    targetingPlayer = getActivatingPlayer();
                }
                // don't set targeting player when forceful target,
                // "targeting player controls" should not be reset when the spell is copied
                currentAbility.setTargetingPlayer(targetingPlayer);
                if (!targetingPlayer.getController().chooseTargetsFor(currentAbility)) {
                    return false;
                }
            }
            final AbilitySub subAbility = currentAbility.getSubAbility();
            if (subAbility != null) {
                // This is necessary for "TargetsWithDefinedController$ ParentTarget"
                subAbility.setParent(currentAbility);
            }
            currentAbility = subAbility;
        } while (currentAbility != null);

        // Check if meet MustTarget restriction
        if (!StaticAbilityMustTarget.meetsMustTargetRestriction(this)) {
            String message = Localizer.getInstance().getMessage("lblInvalidTargetSpecification");
            getActivatingPlayer().getController().notifyOfValue(null, null, message);
            return false;
        }

        return true;
    }
    public final void clearTargets() {
        if (usesTargeting()) {
            resetTargets();
            if (isDividedAsYouChoose()) {
                this.dividedValue = AbilityUtils.calculateAmount(getHostCard(), this.getParam("DividedAsYouChoose"), this);
            }
        }
    }

    // Takes one argument like Permanent.Blue+withFlying
    @Override
    public final boolean isValid(final String restriction, final Player sourceController, final Card source, CardTraitBase spellAbility) {
        // Inclusive restrictions are Card types
        final String[] incR = restriction.split("\\.", 2);
        SpellAbility root = getRootAbility();

        boolean testFailed = false;
        if (incR[0].startsWith("!")) {
            testFailed = true; // a bit counterintuitive
            incR[0] = incR[0].substring(1); // consume negation sign
        }

        if (incR[0].equals("Spell")) {
            if (!root.isSpell()) {
                return testFailed;
            }
        }
        else if (incR[0].equals("Ability")) {
            if (!root.isAbility()) {
                return testFailed;
            }
        }
        else if (incR[0].equals("Instant")) {
            if (!root.getCardState().getType().isInstant()) {
                return testFailed;
            }
        }
        else if (incR[0].equals("Sorcery")) {
            if (!root.getCardState().getType().isSorcery()) {
                return testFailed;
            }
        }
        else if (incR[0].equals("Triggered")) {
            if (!root.isTrigger()) {
                return testFailed;
            }
        }
        else if (incR[0].equals("Activated")) {
            if (!root.isActivatedAbility()) {
                return testFailed;
            }
        }
        else if (incR[0].equals("Static")) {
            if (!(root instanceof AbilityStatic)) {
                return testFailed;
            }
        }
        else if (incR[0].contains("LandAbility")) {
            if (!(root instanceof LandAbility)) {
                return testFailed;
            }
        }
        else if (incR[0].equals("SpellAbility")) {
            // Match anything
        }
        else { //not a spell/ability type
            return testFailed;
        }

        if (incR.length > 1) {
            final String excR = incR[1];
            final String[] exR = excR.split("\\+"); // Exclusive Restrictions are ...
            for (int j = 0; j < exR.length; j++) {
                if (!hasProperty(exR[j], sourceController, source, spellAbility)) {
                    return testFailed;
                }
            }
        }
        return !testFailed;
    }

    // Takes arguments like Blue or withFlying
    @Override
    public boolean hasProperty(final String property, final Player sourceController, final Card source, CardTraitBase spellAbility) {
        return ForgeScript.spellAbilityHasProperty(this, property, sourceController, source, spellAbility);
    }

    // Return whether this spell tracks what color mana is spent to cast it for the sake of the effect
    public boolean tracksManaSpent() {
        if (hostCard == null || hostCard.getRules() == null) { return false; }

        if (isSpell() && hostCard.hasConverge()) {
            return true;
        }

        String text = hostCard.getRules().getOracleText();
        if (isSpell() && text.contains("was spent to cast")) {
            return true;
        }
        if (isAbility() && text.contains("mana spent to pay")) {
            return true;
        }
        return false;
    }

    public int getTotalManaSpent() {
        return this.getPayingMana().size();
    }

    public List<AbilitySub> getChosenList() {
        return chosenList;
    }
    public void setChosenList(List<AbilitySub> choices) {
        chosenList = choices;
    }

    @Override
    public void changeText() {
        super.changeText();

        if (targetRestrictions != null) {
            targetRestrictions.applyTargetTextChanges(this);
        }

        getPayCosts().applyTextChangeEffects(this);

        stackDescription = AbilityUtils.applyDescriptionTextChangeEffects(originalStackDescription, this);
        description = AbilityUtils.applyDescriptionTextChangeEffects(originalDescription, this);

        if (subAbility != null) {
            // if the parent of the subability is not this,
            // then there might be a loop
            if (subAbility.getParent() == this) {
                subAbility.changeText();
            }
        }
        for (SpellAbility sa : additionalAbilities.values()) {
            sa.changeText();
        }

        for (List<AbilitySub> list : additionalAbilityLists.values()) {
            for (AbilitySub sa : list) {
                sa.changeText();
            }
        }
    }

    /* (non-Javadoc)
     * @see forge.game.CardTraitBase#changeTextIntrinsic(java.util.Map, java.util.Map)
     */
    @Override
    public void changeTextIntrinsic(Map<String, String> colorMap, Map<String, String> typeMap) {
        super.changeTextIntrinsic(colorMap, typeMap);

        if (subAbility != null) {
            // if the parent of the subability is not this,
            // then there might be a loop
            if (subAbility.getParent() == this) {
                subAbility.changeTextIntrinsic(colorMap, typeMap);
            }
        }
        for (SpellAbility sa : additionalAbilities.values()) {
            sa.changeTextIntrinsic(colorMap, typeMap);
        }

        for (List<AbilitySub> list : additionalAbilityLists.values()) {
            for (AbilitySub sa : list) {
                sa.changeTextIntrinsic(colorMap, typeMap);
            }
        }
    }

    @Override
    public void setIntrinsic(boolean i) {
        super.setIntrinsic(i);
        if (subAbility != null) {
            subAbility.setIntrinsic(i);
        }
        for (SpellAbility sa : additionalAbilities.values()) {
            if (sa.isIntrinsic() != i) {
                sa.setIntrinsic(i);
            }
        }
        for (List<AbilitySub> list : additionalAbilityLists.values()) {
            for (AbilitySub sa : list) {
                if (sa.isIntrinsic() != i) {
                    sa.setIntrinsic(i);
                }
            }
        }
    }

    public SpellAbilityView getView() {
        view.updateHostCard(this);
        view.updateDescription(this);
        view.updateCanPlay(this, true);
        view.updatePromptIfOnlyPossibleAbility(this);
        return view;
    }

    @Override
    public int compareTo(SpellAbility ab) {
        if (this.isManaAbility() && ab.isManaAbility()) {
            return this.calculateScoreForManaAbility() - ab.calculateScoreForManaAbility();
        }
        return 0;
    }

    public int calculateScoreForManaAbility() {
        int score = 0;
        if (manaPart == null) {
            score++; //Assume a mana ability can generate at least 1 mana if the amount of mana can't be determined now.
        } else {
            String mana = manaPart.mana(this);
            if (!mana.equals("Any")) {
                score += mana.length();
                if (!canProduce("C")) {
                    // Producing colorless should produce a slightly lower score
                    score += 1;
                }
            } else {
                score += 7;
            }
        }

        //increase score if any part of ability's cost is not reusable or renewable (such as paying life)
        for (CostPart costPart : payCosts.getCostParts()) {
            if (!costPart.isReusable()) {
                score += 3;
            }
            if (!costPart.isRenewable()) {
                score += 3;
            }
            if (costPart instanceof CostTap && !Untap.canUntap(getHostCard())) {
                score += 10;
            }
            if (costPart instanceof CostSacrifice && !costPart.payCostFromSource()) {
                // Need to sacrifice "something else". Since we lose that something else, add a large sum since the AI
                // isn't trustworthy to pick
                score += 40;
            }
            // Increase score by 1 for each costpart in general
            score++;
        }

        if (!this.isUndoable()) {
            score += 50; //only use non-undoable mana abilities as a last resort
        }
        if (subAbility != null) {
            // If the primary ability has a sub, it's probably "more expensive"
            score += 2;
        }

        return score;
    }

    public CardDamageMap getDamageMap() {
        if (damageMap != null) {
            return damageMap;
        } else if (getParent() != null) {
            return getParent().getDamageMap();
        }
        return null;
    }

    public CardDamageMap getPreventMap() {
        if (preventMap != null) {
            return preventMap;
        } else if (getParent() != null) {
            return getParent().getPreventMap();
        }
        return null;
    }

    public GameEntityCounterTable getCounterTable() {
        if (counterTable != null) {
            return counterTable;
        } else if (getParent() != null) {
            return getParent().getCounterTable();
        }
        return null;
    }

    public CardZoneTable getChangeZoneTable() {
        if (changeZoneTable != null) {
            return changeZoneTable;
        } else if (getParent() != null) {
            return getParent().getChangeZoneTable();
        }
        return null;
    }

    public Map<Player, Integer> getLoseLifeMap() {
        if (loseLifeMap != null) {
            return loseLifeMap;
        } else if (getParent() != null) {
            return getParent().getLoseLifeMap();
        }
        return null;
    }

    public void setDamageMap(final CardDamageMap map) {
        damageMap = map;
    }
    public void setPreventMap(final CardDamageMap map) {
        preventMap = map;
    }
    public void setCounterTable(final GameEntityCounterTable table) {
        counterTable = table;
    }
    public void setChangeZoneTable(final CardZoneTable table) {
        changeZoneTable = table;
    }
    public void setLoseLifeMap(final Map<Player, Integer> map) {
        loseLifeMap = map;
    }

    public SpellAbility getOriginalAbility() {
        return grantorOriginal == null ? null : ObjectUtils.firstNonNull(grantorOriginal.getOriginalAbility(), grantorOriginal);
    }
    public void setOriginalAbility(final SpellAbility sa) {
        grantorOriginal = sa;
    }

    public StaticAbility getGrantorStatic() {
        return grantorStatic;
    }
    public void setGrantorStatic(final StaticAbility st) {
        grantorStatic = st;
    }

    public boolean isAlternativeCost(AlternativeCost ac) {
        if (ac.equals(altCost)) {
            return true;
        }

        SpellAbility parent = getParent();
        if (parent != null) {
            return parent.isAlternativeCost(ac);
        }
        return false;
    }

    public AlternativeCost getAlternativeCost() {
        if (altCost != null) {
            return altCost;
        }

        SpellAbility parent = getParent();
        if (parent != null) {
            return parent.getAlternativeCost();
        }
        return null;
    }

    public void setAlternativeCost(AlternativeCost ac) {
        altCost = ac;
    }

    public Integer getXManaCostPaid() {
        return xManaCostPaid;
    }
    public void setXManaCostPaid(final Integer n) {
        xManaCostPaid = n;
    }

    public String getXColor() {
        if (!hasParam("XColor")) {
            return null;
        }

        StringBuilder sb = new StringBuilder();
        String parts[] = getParam("XColor").split(",");
        for (String col : parts) {
            // color word used
            if (col.length() > 2) {
                col = MagicColor.toShortString(col);
            }
            sb.append(col);
        }
        return sb.toString();
    }

    public boolean canCastTiming(Player activator) {
        return canCastTiming(getHostCard(), activator);
    }
    public boolean canCastTiming(Card host, Player activator) {
        // for companion
        if (this instanceof AbilityStatic && getRestrictions().isSorcerySpeed() && !activator.canCastSorcery()) {
            return false;
        }

        // no spell or no activated ability, no check there
        if (!isSpell() && !isActivatedAbility()) {
            return true;
        }

        if (activator.canCastSorcery() || withFlash(host, activator)) {
            return true;
        }

        // spells per default are sorcerySpeed
        if (isSpell()) {
            return false;
        }

        if (isActivatedAbility()) {
            // Activated Abilities are instant speed per default, except Planeswalker abilities
            return !isPwAbility() && !getRestrictions().isSorcerySpeed();
        }
        return true;
    }

    public boolean withFlash(Card host, Player activator) {
        if (getRestrictions().isInstantSpeed()) {
            return true;
        }
        if (isSpell() && (hasSVar("IsCastFromPlayEffect") || host.isInstant() || host.hasKeyword(Keyword.FLASH))) {
            return true;
        }

        return StaticAbilityCastWithFlash.anyWithFlash(this, host, activator);
    }

    public boolean checkRestrictions(Player activator) {
        return checkRestrictions(getHostCard(), activator);
    }

    public boolean checkRestrictions(Card host, Player activator) {
        return true;
    }

    public void addRollbackEffect(Card eff) {
        rollbackEffects.add(eff);
    }

    public void rollback() {
        for (Card c : rollbackEffects) {
            c.getGame().getAction().ceaseToExist(c, true);
        }
        rollbackEffects.clear();
    }

    public boolean isHidden() {
        boolean hidden = hasParam("Hidden");
        if (!hidden && hasParam("Origin")) {
            hidden = ZoneType.isHidden(getParam("Origin"));
        }
        return hidden;
    }

    public boolean isLegalAfterStack() {
        if (!matchesValidParam("ValidAfterStack", this)) {
            return false;
        }
        return true;
    }
}
