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

import com.google.common.collect.Iterables;

import forge.card.mana.ManaCost;
import forge.game.CardTraitBase;
import forge.game.Game;
import forge.game.GameEntity;
import forge.game.GameObject;
import forge.game.IIdentifiable;
import forge.game.ability.AbilityFactory;
import forge.game.ability.AbilityUtils;
import forge.game.ability.ApiType;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CardCollectionView;
import forge.game.cost.Cost;
import forge.game.cost.CostPart;
import forge.game.cost.CostPartMana;
import forge.game.mana.Mana;
import forge.game.player.Player;
import forge.game.staticability.StaticAbility;
import forge.game.trigger.TriggerType;
import forge.game.trigger.WrappedAbility;
import forge.util.Expressions;
import forge.util.TextUtil;

import org.apache.commons.lang3.StringUtils;

import java.util.*;

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
    private ManaCost multiKickerManaCost = null;
    private Player activatingPlayer = null;
    private Player targetingPlayer = null;

    private boolean basicLandAbility; // granted by basic land type

    private Card grantorCard = null; // card which grants the ability (equipment or owner of static ability that gave this one)

    private CardCollection splicedCards = null;

    private boolean basicSpell = true;
    private boolean trigger = false;
    private boolean optionalTrigger = false;
    private boolean replacementAbility = false;
    private int sourceTrigger = -1;
    private List<Object> triggerRemembered = new ArrayList<Object>();

    private boolean flashBackAbility = false;
    private boolean cycling = false;
    private boolean delve = false;
    private boolean dash = false;
    private boolean offering = false;
    private boolean emerge = false;
    private boolean morphup = false;
    private boolean manifestUp = false;
    private boolean cumulativeupkeep = false;
    private boolean outlast = false;
    private SplitSide splitSide = null;
    enum SplitSide { LEFT, RIGHT };
    private int totalManaSpent = 0;

    /** The pay costs. */
    private Cost payCosts;
    private SpellAbilityRestriction restrictions = new SpellAbilityRestriction();
    private SpellAbilityCondition conditions = new SpellAbilityCondition();
    private AbilitySub subAbility = null;

    protected ApiType api = null;

    private final List<Mana> payingMana = new ArrayList<Mana>();
    private final List<SpellAbility> paidAbilities = new ArrayList<SpellAbility>();

    private HashMap<String, CardCollection> paidLists = new HashMap<String, CardCollection>();

    private HashMap<String, Object> triggeringObjects = new HashMap<String, Object>();

    private HashMap<String, Object> replacingObjects = new HashMap<String, Object>();

    private List<AbilitySub> chosenList = null;
    private CardCollection tappedForConvoke = new CardCollection();
    private Card sacrificedAsOffering = null;
    private Card sacrificedAsEmerge = null;
    private int conspireInstances = 0;

    private HashMap<String, String> sVars = new HashMap<String, String>();

    private AbilityManaPart manaPart = null;

    private boolean undoable;

    private boolean isCopied = false;

    private EnumSet<OptionalCost> optionalCosts = EnumSet.noneOf(OptionalCost.class);
    private TargetRestrictions targetRestrictions = null;
    private TargetChoices targetChosen = new TargetChoices();

    private SpellAbilityView view;

    private StaticAbility mayPlay = null;

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
        if (hostCard != null && hostCard.getGame() != null) {
            hostCard.getGame().addSpellAbility(id, this);
        }
    }

    @Override
    public int getId() {
        return id;
    }
    @Override
    public int hashCode() {
        return getId();
    }
    @Override
    public boolean equals(final Object obj) {
        return obj instanceof SpellAbility && this.id == ((SpellAbility) obj).id;
    };

    @Override
    public void setHostCard(final Card c) {
        if (hostCard == c) { return; }
        super.setHostCard(c);
        view.updateHostCard(this);
        view.updateDescription(this); //description can change if host card does
    }

    public final AbilityManaPart getManaPart() {
        return manaPart;
    }

    public final AbilityManaPart getManaPartRecursive() {
        SpellAbility tail = this;
        while (tail != null) {
            if (tail.manaPart != null) {
                return tail.manaPart;
            }
            tail = tail.getSubAbility();
        }
        return null;
    }

    public final boolean isManaAbility() {
        // Check whether spell or ability first
        if (isSpell()) {
            return false;
        }
        // without a target
        if (usesTargeting()) { return false; }
        if (restrictions != null && restrictions.isPwAbility()) {
            return false; //Loyalty ability, not a mana ability.
        }
        if (isWrapper() && ((WrappedAbility) this).getTrigger().getMode() != TriggerType.TapsForMana) {
            return false;
        }

        return getManaPartRecursive() != null;
    }

    protected final void setManaPart(AbilityManaPart manaPart0) {
        manaPart = manaPart0;
    }

    public final String getSvarWithFallback(final String name) {
        String var = sVars.get(name);
        if (var == null) {
            var = hostCard.getSVar(name);
        }
        return var;
    }

    public final String getSVar(final String name) {
        String var = sVars.get(name);
        if (var == null) {
            var = "";
        }
        return var;
    }

    public final Integer getSVarInt(final String name) {
        String var = sVars.get(name);
        if (var != null) {
            try {
                return Integer.parseInt(var);
            }
            catch (Exception e) {}
        }
        return null;
    }

    public final void setSVar(final String name, final String value) {
        sVars.put(name, value);
    }

    public Set<String> getSVars() {
        return sVars.keySet();
    }

    // Spell, and Ability, and other Ability objects override this method
    public abstract boolean canPlay();

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
        // trickle down activating player
        activatingPlayer = player;
        if (subAbility != null) {
            subAbility.setActivatingPlayer(player);
        }
        view.updateCanPlay(this);
    }

    public Player getTargetingPlayer() {
        return targetingPlayer;
    }
    public void setTargetingPlayer(Player targetingPlayer0) {
        targetingPlayer = targetingPlayer0;
    }

    public boolean isSpell() { return false; }
    public boolean isAbility() { return true; }

    public boolean isMorphUp() {
        return morphup;
    }
    public final void setIsMorphUp(final boolean b) {
        morphup = b;
    }

    public boolean isManifestUp() {
        return manifestUp;
    }
    public final void setIsManifestUp(final boolean b) {
        manifestUp = b;
    }

    public boolean isCycling() {
        return cycling;
    }
    public final void setIsCycling(final boolean b) {
        cycling = b;
    }

    public Card getOriginalHost() {
        return grantorCard;
    }
    public void setOriginalHost(final Card c) {
        grantorCard = c;
    }

    public String getParamOrDefault(String key, String defaultValue) {
        return mapParams.containsKey(key) ? mapParams.get(key) : defaultValue;
    }

    public String getParam(String key) {
        return mapParams.get(key);
    }
    public boolean hasParam(String key) {
        return mapParams.containsKey(key);
    }

    public void copyParamsToMap(Map<String, String> mapParam) {
        mapParam.putAll(mapParams);
    }

    // If this is not null, then ability was made in a factory
    public ApiType getApi() {
        return api;
    }

    public void setApi(ApiType apiType) {
        api = apiType;
    }

    public final boolean isCurse() {
        return hasParam("IsCurse");
    }

    // begin - Input methods

    public Cost getPayCosts() {
        return payCosts;
    }
    public void setPayCosts(final Cost abCost) {
        payCosts = abCost;
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
        return restrictions.getNumberTurnActivations();
    }

    public SpellAbilityCondition getConditions() {
        return conditions;
    }
    public final void setConditions(final SpellAbilityCondition condition) {
        conditions = condition;
    }

    public List<Mana> getPayingMana() {
        return payingMana;
    }
    public final void clearManaPaid() {
        payingMana.clear();
    }

    public List<SpellAbility> getPayingManaAbilities() {
        return paidAbilities;
    }

    // Combined PaidLists
    public HashMap<String, CardCollection> getPaidHash() {
        return paidLists;
    }
    public void setPaidHash(final HashMap<String, CardCollection> hash) {
        paidLists = hash;
    }

    public CardCollection getPaidList(final String str) {
        return paidLists.get(str);
    }
    public void addCostToHashList(final Card c, final String str) {
        if (!paidLists.containsKey(str)) {
            paidLists.put(str, new CardCollection());
        }
        paidLists.get(str).add(c);
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
    }

    public boolean isBuyBackAbility() {
        return isOptionalCostPaid(OptionalCost.Buyback);
    }

    public boolean isKicked() {
        return isOptionalCostPaid(OptionalCost.Kicker1) || isOptionalCostPaid(OptionalCost.Kicker2);
    }

    public boolean isSurged() {
        return isOptionalCostPaid(OptionalCost.Surge);
    }

    public boolean isOptionalCostPaid(OptionalCost cost) {
        SpellAbility saRoot = getRootAbility();
        return saRoot.optionalCosts.contains(cost);
    }

    public HashMap<String, Object> getTriggeringObjects() {
        return triggeringObjects;
    }
    public void setTriggeringObjects(final HashMap<String, Object> triggeredObjects) {
        triggeringObjects = triggeredObjects;
    }
    public Object getTriggeringObject(final String type) {
        return triggeringObjects.get(type);
    }
    public void setTriggeringObject(final String type, final Object o) {
        triggeringObjects.put(type, o);
    }
    public boolean hasTriggeringObject(final String type) {
        return triggeringObjects.containsKey(type);
    }
    public void resetTriggeringObjects() {
        triggeringObjects = new HashMap<String, Object>();
    }

    public List<Object> getTriggerRemembered() {
        return triggerRemembered;
    }
    public void setTriggerRemembered(List<Object> list) {
        triggerRemembered = list;
    }
    public void resetTriggerRemembered() {
        triggerRemembered = new ArrayList<Object>();
    }

    public HashMap<String, Object> getReplacingObjects() {
        return replacingObjects;
    }
    public Object getReplacingObject(final String type) {
        final Object res = replacingObjects.get(type);
        return res;
    }
    public void setReplacingObject(final String type, final Object o) {
        replacingObjects.put(type, o);
    }

    public void resetOnceResolved() {
        resetPaidHash();
        resetTargets();
        resetTriggeringObjects();
        resetTriggerRemembered();

        // Clear SVars
        for (final String store : Card.getStorableSVars()) {
            final String value = hostCard.getSVar(store);
            if (value.length() > 0) {
                hostCard.setSVar(store, "");
            }
        }
    }

    public String getStackDescription() {
        String text = getHostCard().getView().getText();
        if (stackDescription.equals(text)) {
            return getHostCard().getName() + " - " + text;
        }
        return stackDescription.replaceAll("CARDNAME", getHostCard().getName());
    }
    public void setStackDescription(final String s) {
        originalStackDescription = s;
        stackDescription = originalStackDescription;
        if (StringUtils.isEmpty(description) && StringUtils.isEmpty(hostCard.getView().getText())) {
            setDescription(s);
        }
    }

    // setDescription() includes mana cost and everything like
    // "G, tap: put target creature from your hand onto the battlefield"
    public String getDescription() {
        return description;
    }
    public void setDescription(final String s) {
        originalDescription = s;
        description = originalDescription;
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
            if (node.getHostCard() != null) {
                sb.append(node.getDescription().replace("CARDNAME", node.getHostCard().getName()));
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
    public void appendSubAbility(final AbilitySub toAdd) {
        SpellAbility tailend = this;
        while (tailend.getSubAbility() != null) {
            tailend = tailend.getSubAbility();
        }
        tailend.setSubAbility(toAdd);
    }

    public boolean isBasicSpell() {
        return basicSpell && !isFlashBackAbility() && !isBuyBackAbility();
    }
    public void setBasicSpell(final boolean basicSpell0) {
        basicSpell = basicSpell0;
    }

    public void setFlashBackAbility(final boolean flashBackAbility0) {
        flashBackAbility = flashBackAbility0;
    }
    public boolean isFlashBackAbility() {
        return flashBackAbility;
    }

    public boolean isOutlast() {
        return outlast;
    }
    public void setOutlast(boolean outlast0) {
        outlast = outlast0;
    }

    public StaticAbility getMayPlay() {
        return mayPlay;
    }
    public void setMayPlay(final StaticAbility sta) {
        mayPlay = sta;
    }

    public boolean isLeftSplit() {
        return splitSide == SplitSide.LEFT;
    }
    public boolean isRightSplit() {
        return splitSide == SplitSide.RIGHT;
    }
    public void setNoSplit() {
        splitSide = null;
    }
    public void setLeftSplit() {
        splitSide = SplitSide.LEFT;
    }
    public void setRightSplit() {
        splitSide = SplitSide.RIGHT;
    }

    public SpellAbility copy() {
        SpellAbility clone = null;
        try {
            clone = (SpellAbility) clone();
            clone.id = nextId();
            clone.view = new SpellAbilityView(clone);
            if (clone.hostCard != null && clone.hostCard.getGame() != null) {
                clone.hostCard.getGame().addSpellAbility(clone.id, clone);
            }
        } catch (final CloneNotSupportedException e) {
            System.err.println(e);
        }
        return clone;
    }

    public SpellAbility copyWithNoManaCost() {
        final SpellAbility newSA = copy();
        newSA.setPayCosts(newSA.getPayCosts().copyWithNoMana());
        newSA.setDescription(newSA.getDescription() + " (without paying its mana cost)");
        return newSA;
    }

    public SpellAbility copyWithDefinedCost(Cost abCost) {
        final SpellAbility newSA = copy();
        newSA.setPayCosts(abCost);
        return newSA;
    }

    public boolean isTrigger() {
        return trigger;
    }
    public void setTrigger(final boolean trigger0) {
        trigger = trigger0;
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
        return replacementAbility;
    }
    public void setReplacementAbility(boolean replacement) {
        replacementAbility = replacement;
    }

    public boolean isMandatory() {
        return false;
    }

    public final boolean canTarget(final GameObject entity) {
        final TargetRestrictions tr = getTargetRestrictions();

        // Restriction related to this ability
        if (tr != null) {
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
                    if (!c.sharesCardTypeWith(crd)) {
                        return false;
                    }
                }
            }
            if (hasParam("TargetsWithSharedTypes") && entity instanceof Card) {
                final Card c = (Card) entity;
                final SpellAbility parent = getParentTargetingCard();
                final Card parentTargeted = parent != null ? parent.getTargetCard() : null;
                if (parentTargeted == null) {
                    return false;
                }
                boolean flag = false;
                for (final String type : getParam("TargetsWithSharedTypes").split(",")) {
                    if (c.getType().hasStringType(type) && parentTargeted.getType().hasStringType(type)) {
                        flag = true;
                        break;
                    }
                }
                if (!flag) {
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
                        return c.getNetPower() <= parentTarget.getNetPower();
                    case "LECMC" :
                        return c.getCMC() <= parentTarget.getCMC();
                }
            }

            if (hasParam("TargetingPlayerControls") && entity instanceof Card) {
                final Card c = (Card) entity;
                if (!c.getController().equals(targetingPlayer)) {
                    return false;
                }
            }

            if (hasParam("MaxTotalTargetCMC") && entity instanceof Card) {
                final Card c = (Card) entity;
                if (c.getCMC() > tr.getMaxTotalCMC(c, this)) {
                    return false;
                }
            }

            String[] validTgt = tr.getValidTgts();
            if (entity instanceof GameEntity && !((GameEntity) entity).isValid(validTgt, getActivatingPlayer(), getHostCard(), this)) {
               return false;
            }
        }

        // Restrictions coming from target
        return entity.canBeTargetedBy(this);
    }

    // is this a wrapping ability (used by trigger abilities)
    public boolean isWrapper() {
        return false;
    }

    public final boolean isDelve() {
        return delve;
    }
    public final void setDelve(final boolean isDelve0) {
        delve = isDelve0;
    }

    public final boolean isDash() {
        return dash;
    }
    public final void setDash(final boolean isDash) {
        dash = isDash;
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
        return emerge;
    }
    public void setIsEmerge(final boolean bEmerge) {
        emerge = bEmerge;
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
        return offering;
    }
    public void setIsOffering(final boolean bOffering) {
        offering = bOffering;
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
            return;
        }
        String[] announcedOnes = TextUtil.split(announce, ',');
        for (String a : announcedOnes) {
            if (a.trim().equalsIgnoreCase(variable)) {
                return; //don't add announce variable that already exists
            }
        }
        mapParams.put("Announce", announce + ";" + variable);
    }

    public boolean isXCost() {
        CostPartMana cm = payCosts != null ? getPayCosts().getCostMana() : null;
        return cm != null && cm.getAmountOfX() > 0;
    }

    public boolean isBasicLandAbility() {
        return basicLandAbility;
    }

    public void setBasicLandAbility(boolean basicLandAbility0) {
        basicLandAbility = basicLandAbility0;
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
        targetChosen = targets;
    }

    public void resetTargets() {
        targetChosen = new TargetChoices();
    }

    /**
     * Reset the first target.
     *
     */
    public void resetFirstTarget(GameObject c, SpellAbility originalSA) {
        SpellAbility sa = this;
        while (sa != null) {
            if (sa.targetRestrictions != null) {
                sa.targetChosen = new TargetChoices();
                sa.targetChosen.add(c);
                if (!originalSA.targetRestrictions.getDividedMap().isEmpty()) {
                    sa.targetRestrictions.addDividedAllocation(c,
                            Iterables.getFirst(originalSA.targetRestrictions.getDividedMap().values(), null));
                }
                break;
            }
            sa = sa.subAbility;
        }
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
        final List<TargetChoices> res = new ArrayList<TargetChoices>();

        SpellAbility sa = getRootAbility();
        if (sa.getTargetRestrictions() != null) {
            res.add(sa.getTargets());
        }
        while (sa.getSubAbility() != null) {
            sa = sa.getSubAbility();

            if (sa.getTargetRestrictions() != null) {
                res.add(sa.getTargets());
            }
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

    /**
     * <p>
     * findTargetCards.
     * </p>
     *
     * @return a {@link forge.game.spellability.SpellAbility} object.
     */
    public CardCollectionView findTargetedCards() {
        // First search for targeted cards associated with current ability
        if (targetChosen.isTargetingAnyCard()) {
            return targetChosen.getTargetCards();
        }

        // Next search for source cards of targeted SAs associated with current ability
        if (targetChosen.isTargetingAnySpell()) {
            CardCollection res = new CardCollection();
            for (final SpellAbility ability : targetChosen.getTargetSpells()) {
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
        return targetChosen.isTargetingAnyCard() ? this : getParentTargetingCard();
    }

    public SpellAbility getParentTargetingCard() {
        SpellAbility parent = getParent();
        if (parent instanceof WrappedAbility) {
            parent = ((WrappedAbility) parent).getWrappedAbility();
        }
        while (parent != null) {
            if (parent.targetChosen.isTargetingAnyCard()) {
                return parent;
            }
            parent = parent.getParent();
        }
        return null;
    }

    public SpellAbility getSATargetingSA() {
        return targetChosen.isTargetingAnySpell() ? this : getParentTargetingSA();
    }

    public SpellAbility getParentTargetingSA() {
        SpellAbility parent = getParent();
        while (parent != null) {
            if (parent.targetChosen.isTargetingAnySpell())
                return parent;
            parent = parent.getParent();
        }
        return null;
    }

    public SpellAbility getSATargetingPlayer() {
        return targetChosen.isTargetingAnyPlayer() ? this : getParentTargetingPlayer();
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
        final List<GameObject> targets = new ArrayList<GameObject>();
        SpellAbility child = getParent();
        while (child != null) {
            if (child.getTargetRestrictions() != null) {
                Iterables.addAll(targets, child.getTargets().getTargets());
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

        final String splitTargetRestrictions = tgt.getSAValidTargeting();
        if (splitTargetRestrictions != null) {
            // TODO Ensure that spells with subabilities are processed correctly

            TargetChoices matchTgt = topSA.getTargets();

            if (matchTgt == null) {
                return false;
            }

            boolean result = false;

            for (final GameObject o : matchTgt.getTargets()) {
                if (o.isValid(splitTargetRestrictions.split(","), getActivatingPlayer(), getHostCard(), this)) {
                    result = true;
                    break;
                }
            }

            // spells with subabilities
            if (!result) {
                AbilitySub subAb = topSA.getSubAbility();
                while (subAb != null) {
                    if (subAb.getTargetRestrictions() != null) {
                        matchTgt = subAb.getTargets();
                        if (matchTgt == null) {
                            continue;
                        }
                        for (final GameObject o : matchTgt.getTargets()) {
                            if (o.isValid(splitTargetRestrictions.split(","), getActivatingPlayer(), getHostCard(), this)) {
                                result = true;
                                break;
                            }
                        }
                    }
                    subAb = subAb.getSubAbility();
                }
            }

            if (!result) {
                return false;
            }
        }

        if (tgt.isSingleTarget()) {
            Set<GameObject> targets = new HashSet<>();
            for (TargetChoices tc : topSA.getAllTargetChoices()) {
                targets.addAll(tc.getTargets());
                if (targets.size() > 1) {
                    // As soon as we get more than one, bail out
                    return false;
                }
            }
            if (targets.size() != 1) {
                // Make sure that there actually is one target
                return false;
            }
        }

        return topSA.getHostCard().isValid(tgt.getValidTgts(), getActivatingPlayer(), getHostCard(), this);
    }

    // Takes one argument like Permanent.Blue+withFlying
    @Override
    public final boolean isValid(final String restriction, final Player sourceController, final Card source, SpellAbility spellAbility) {
        // Inclusive restrictions are Card types
        final String[] incR = restriction.split("\\.", 2);

        if (incR[0].equals("Spell")) {
            if (!isSpell()) {
                return false;
            }
        }
        else if (incR[0].equals("Triggered")) {
            if (!isTrigger()) {
                return false;
            }
        }
        else if (incR[0].equals("Activated")) {
            if (!(this instanceof AbilityActivated)) {
                return false;
            }
        }
        else { //not a spell/ability type
            return false;
        }

        if (incR.length > 1) {
            final String excR = incR[1];
            final String[] exR = excR.split("\\+"); // Exclusive Restrictions are ...
            for (int j = 0; j < exR.length; j++) {
                if (!hasProperty(exR[j], sourceController, source, spellAbility)) {
                    return false;
                }
            }
        }
        return true;
    }

    // Takes arguments like Blue or withFlying
    @Override
    public boolean hasProperty(final String property, final Player sourceController, final Card source, SpellAbility spellAbility) {
        return true;
    }

    // Methods enabling multiple instances of conspire
    public void addConspireInstance() {
        conspireInstances++;
    }

    public void subtractConspireInstance() {
        conspireInstances--;
    }

    public int getConspireInstances() {
        return conspireInstances;
    } // End of Conspire methods

    public boolean isCumulativeupkeep() {
        return cumulativeupkeep;
    }

    public void setCumulativeupkeep(boolean cumulativeupkeep0) {
        cumulativeupkeep = cumulativeupkeep0;
    }

    // Return whether this spell tracks what color mana is spent to cast it for the sake of the effect
    public boolean tracksManaSpent() {
        if (hostCard == null || hostCard.getRules() == null) { return false; }

        if (hostCard.hasKeyword("Sunburst")) {
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

    public void checkActivationResloveSubs() {
        if (hasParam("ActivationNumberSacrifice")) {
            String comp = getParam("ActivationNumberSacrifice");
            int right = Integer.parseInt(comp.substring(2));
            int activationNum =  getRestrictions().getNumberTurnActivations();
            if (Expressions.compare(activationNum, comp, right)) {
                SpellAbility deltrig = AbilityFactory.getAbility(hostCard.getSVar(getParam("ActivationResolveSub")), hostCard);
                deltrig.setActivatingPlayer(activatingPlayer);
                AbilityUtils.resolve(deltrig);
            }
        }
    }

    public void setTotalManaSpent(int totManaSpent) {
        totalManaSpent = totManaSpent;
    }

    public int getTotalManaSpent() {
        return totalManaSpent;
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

        if (getPayCosts() != null) {
            getPayCosts().applyTextChangeEffects(this);
        }

        stackDescription = AbilityUtils.applyDescriptionTextChangeEffects(originalStackDescription, this);
        description = AbilityUtils.applyDescriptionTextChangeEffects(originalDescription, this);

        if (subAbility != null) {
            subAbility.changeText();
        }
    }

    @Override
    public void setIntrinsic(boolean i) {
        super.setIntrinsic(i);
        if (subAbility != null) {
            subAbility.setIntrinsic(i);
        }
    }

    public SpellAbilityView getView() {
        view.updateHostCard(this);
        view.updateDescription(this);
        view.updateCanPlay(this);
        view.updatePromptIfOnlyPossibleAbility(this);
        return view;
    }

    @Override
    public int compareTo(SpellAbility ab) {
        if (this.isManaAbility() && ab.isManaAbility()){
            return this.calculateScoreForManaAbility() - ab.calculateScoreForManaAbility();
        }
        return 0;
    }

    public int calculateScoreForManaAbility() {
        int score = 0;
        if (manaPart == null) {
            score++; //Assume a mana ability can generate at least 1 mana if the amount of mana can't be determined now.
        }
        else {
            String mana = manaPart.mana();
            if (!mana.equals("Any")) {
                score += mana.length();
                if (!manaPart.canProduce("C")) {
                    // Producing colorless should produce a slightly lower score
                    score += 1;
                }
            }
            else {
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
}