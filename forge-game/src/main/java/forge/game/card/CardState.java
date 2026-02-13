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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import forge.card.*;
import forge.card.mana.ManaCost;
import forge.card.mana.ManaCostParser;
import forge.game.CardTraitBase;
import forge.game.ForgeScript;
import forge.game.GameObject;
import forge.game.IHasSVars;
import forge.game.ability.AbilityFactory;
import forge.game.ability.ApiType;
import forge.game.card.CardView.CardStateView;
import forge.game.keyword.IKeywordsChange;
import forge.game.keyword.Keyword;
import forge.game.keyword.KeywordCollection;
import forge.game.keyword.KeywordInterface;
import forge.game.keyword.KeywordWithType;
import forge.game.player.Player;
import forge.game.replacement.ReplacementEffect;
import forge.game.spellability.LandAbility;
import forge.game.spellability.SpellAbility;
import forge.game.spellability.SpellPermanent;
import forge.game.staticability.StaticAbility;
import forge.game.staticability.StaticAbilityMode;
import forge.game.trigger.Trigger;
import forge.util.CardTranslation;
import forge.util.ITranslatable;
import forge.util.IterableUtil;
import forge.util.collect.FCollection;
import forge.util.collect.FCollectionView;
import io.sentry.Breadcrumb;
import io.sentry.Sentry;

import org.apache.commons.lang3.StringUtils;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;

public class CardState implements GameObject, IHasSVars, ITranslatable {
    private String name = "";
    private CardType type = new CardType(false);
    private CardTypeView changedType = null;
    private ManaCost manaCost = ManaCost.NO_COST;
    // Track mana cost after adjustments from perpetual cost-changing effects for display
    private ManaCost perpetualAdjustedManaCost = null;
    private ColorSet color = ColorSet.C;
    private String oracleText = "";
    private String functionalVariantName = null;
    private String flavorName = null;
    private int basePower = 0;
    private int baseToughness = 0;
    private String basePowerString = null;
    private String baseToughnessString = null;
    private String baseLoyalty = "";
    private String baseDefense = "";
    private KeywordCollection intrinsicKeywords = new KeywordCollection();
    private Set<Integer> attractionLights = null;

    private final FCollection<SpellAbility> abilities = new FCollection<>();
    private FCollection<Trigger> triggers = new FCollection<>();
    private FCollection<ReplacementEffect> replacementEffects = new FCollection<>();
    private FCollection<StaticAbility> staticAbilities = new FCollection<>();
    private String imageKey = "";
    private Map<String, String> sVars = Maps.newTreeMap();
    private Map<String, SpellAbility> abilityForTrigger = Maps.newHashMap();

    private KeywordCollection cachedKeywords = new KeywordCollection();

    private CardRarity rarity = CardRarity.Unknown;
    private String setCode = CardEdition.UNKNOWN_CODE;

    private final CardStateView view;
    private final Card card;

    private SpellAbility landAbility;
    private SpellAbility auraAbility;
    private SpellAbility permanentAbility;

    private ReplacementEffect loyaltyRep;
    private ReplacementEffect defenseRep;
    private ReplacementEffect sagaRep;
    private ReplacementEffect adventureRep;
    private ReplacementEffect omenRep;

    private SpellAbility manifestUp;
    private SpellAbility cloakUp;

    private LandTraitChanges landTraitChanges = new LandTraitChanges(this);

    public CardState(Card card, CardStateName name) {
        this(card.getView().createAlternateState(name), card);
    }

    public CardState(CardStateView view0, Card card0) {
        view = view0;
        card = card0;
        view.updateRarity(this);
        view.updateSetCode(this);
    }

    public CardStateView getView() {
        return view;
    }

    public Card getCard() {
        return card;
    }

    public final String getName() {
        return name;
    }
    public final void setName(final String name0) {
        name = name0;
        view.updateName(this);
    }

    public CardStateName getStateName() {
        return this.getView().getState();
    }

    @Override
    public String toString() {
        return name + " (" + view.getState() + ")";
    }

    public CardTypeView getTypeWithChanges() {
        return Objects.requireNonNullElse(this.changedType, getType());
    }

    public void updateTypes() {
        this.changedType = getType().getTypeWithChanges(card.getChangedCardTypes());
    }
    public void updateTypesForView() {
        view.updateType(this);
    }

    public final CardTypeView getType() {
        return type;
    }
    public final void addType(String type0) {
        if (type.add(type0)) {
            updateTypes();
            updateTypesForView();
        }
    }
    public final void addType(Iterable<String> type0) {
        if (type.addAll(type0)) {
            updateTypes();
            updateTypesForView();
        }
    }
    public final void setType(final CardType type0) {
        if (type0 == type) {
            // Logic below would incorrectly clear the type if it's the same object.
            return;
        }
        if (type0.isEmpty() && type.isEmpty()) { return; }
        type.clear();
        type.addAll(type0);
        updateTypes();
        updateTypesForView();
    }

    public final void removeType(final CardType.Supertype st) {
        if (type.remove(st)) {
            updateTypes();
            updateTypesForView();
        }
    }

    public final void removeCardTypes(boolean sanisfy) {
        type.removeCardTypes();
        if (sanisfy) {
            type.sanisfySubtypes();
        }

        updateTypes();
        updateTypesForView();
    }

    public final void setCreatureTypes(Collection<String> ctypes) {
        if (type.setCreatureTypes(ctypes)) {
            updateTypes();
            updateTypesForView();
        }
    }

    public final ManaCost getManaCost() {
        return manaCost;
    }
    public final void setManaCost(final ManaCost manaCost0) {
        manaCost = manaCost0;
        view.updateManaCost(this);
    }

    /**
     * Calculate and save the value of the mana cost adjusted by any perpetual raise/lower cost
     * effects for display.
     */
    public void calculatePerpetualAdjustedManaCost() {
        final List<StaticAbility> raiseAbilities = Lists.newArrayList();
        final List<StaticAbility> reduceAbilities = Lists.newArrayList();
        // Separate abilities to apply them in proper order
        if (getCard() == null || getCard().getGame() == null) {
            return;
        }
        ManaCost manaCost = getManaCost();
        // I don't know why refetching the card data like in this next line is necessary but in some cases (e.g. when
        // the cost reduction wasn't added when the card was in the current zone) the static abilities are missing.
        for (final StaticAbility stAb : getCard().getGame().getCardState(getCard()).getStaticAbilities()) {
            // Only collect perpetual cost changes to this card (not cost changes that this card applies to other cards)
            if ("Card.Self".equals(stAb.getParam("ValidCard"))) {
                if (stAb.checkMode(StaticAbilityMode.ReduceCost)) {
                    reduceAbilities.add(stAb);
                } else if (stAb.checkMode(StaticAbilityMode.RaiseCost)) {
                    raiseAbilities.add(stAb);
                }
            }
        }
        int totalGenericCostAdjustment = 0;
        for (final StaticAbility stAb : raiseAbilities) {
            try {
                int amount = Integer.parseInt(stAb.getParamOrDefault("Amount", "1"));
                totalGenericCostAdjustment = totalGenericCostAdjustment + amount;
            } catch (NumberFormatException e) {
                // We only care about adjustments with a specific numeric value.
                // Some cost adjustment abilities put non-numeric values here, such as affinity
            }
        }
        for (final StaticAbility stAb : reduceAbilities) {
            try {
                int amount = Integer.parseInt(stAb.getParamOrDefault("Amount", "1"));
                totalGenericCostAdjustment = totalGenericCostAdjustment - amount;
            } catch (NumberFormatException e) {
                // We only care about adjustments with a specific numeric value.
                // Some cost adjustment abilities put non-numeric values here, such as affinity
            }
        }
        if (totalGenericCostAdjustment != 0) {
            // This doesn't work on hybrid costs such as "Advice from the Fae"
            int genericCost = manaCost.getGenericCost();
            int genericCostAdjustment;
            int remainingGenericCostAdjustment;
            // If the total amount reduced is more than the generic mana cost, keep track of the
            // extra in case it could be applied to an X cost.
            if (genericCost + totalGenericCostAdjustment < 0) {
                genericCostAdjustment = -genericCost;
                remainingGenericCostAdjustment = totalGenericCostAdjustment + genericCost;
            } else {
                genericCostAdjustment = totalGenericCostAdjustment;
                remainingGenericCostAdjustment = 0;
            }
            // If there is a cost adjustment to apply, update the mana cost to reflect that
            String manaCostString = manaCost.getShortString();
            boolean costChange = false;
            if (genericCostAdjustment != 0) { // Replace the original generic mana cost with the adjusted value
                manaCostString = manaCostString.replace("" + genericCost, "" + (genericCost + genericCostAdjustment));
                costChange = true;
            }
            if (remainingGenericCostAdjustment != 0 && manaCostString.contains("X")) {
                // If there is extra cost reduction beyond the numeric generic mana cost, save it to apply to X
                // Store extra cost adjustment as a negative generic value for display
                manaCostString = manaCostString + " " + remainingGenericCostAdjustment;
                costChange = true;
            }
            if (costChange) {
                manaCost = new ManaCost(new ManaCostParser(manaCostString));
            }
        }
        setPerpetualAdjustedManaCost(manaCost);
        view.updateManaCost(this);
    }

    public ManaCost getPerpetualAdjustedManaCost() {
        return perpetualAdjustedManaCost == null ? getManaCost() : perpetualAdjustedManaCost;
    }

    private void setPerpetualAdjustedManaCost(ManaCost manaCost) {
        perpetualAdjustedManaCost = manaCost;
    }

    public final ColorSet getColor() {
        return color;
    }
    public final void addColor(final ColorSet color) {
        this.color = ColorSet.combine(this.color, color);
        view.updateColors(card);
    }
    public final void setColor(final ColorSet color) {
        this.color = color;
        view.updateColors(card);
    }

    public String getOracleText() {
        return oracleText;
    }
    public void setOracleText(final String oracleText) {
        this.oracleText = oracleText;
        view.setOracleText(oracleText);
    }

    public String getFunctionalVariantName() {
        return functionalVariantName;
    }
    public void setFunctionalVariantName(String functionalVariantName) {
        if(functionalVariantName != null && functionalVariantName.isEmpty())
            functionalVariantName = null;
        this.functionalVariantName = functionalVariantName;
        view.setFunctionalVariantName(functionalVariantName);
    }

    public String getFlavorName() {
        return flavorName;
    }

    public void setFlavorName(String flavorName) {
        this.flavorName = flavorName;
        view.updateName(this);
    }

    public final int getBasePower() {
        return basePower;
    }
    public final void setBasePower(final int basePower0) {
        if (basePower == basePower0) { return; }
        basePower = basePower0;
        view.updatePower(this);
    }

    public final int getBaseToughness() {
        return baseToughness;
    }
    public final void setBaseToughness(final int baseToughness0) {
        if (baseToughness == baseToughness0) { return; }
        baseToughness = baseToughness0;
        view.updateToughness(this);
    }

    // values that are printed on card
    public final String getBasePowerString() {
        return basePowerString;
    }
    public final String getBaseToughnessString() {
        return baseToughnessString;
    }

    // values that are printed on card
    public final void setBasePowerString(final String s) {
        basePowerString = s;
    }
    public final void setBaseToughnessString(final String s) {
        baseToughnessString = s;
    }

    public String getBaseLoyalty() {
        return baseLoyalty;
    }
    public final void setBaseLoyalty(final String string) {
        baseLoyalty = string;
        view.updateLoyalty(this);
    }

    public String getBaseDefense() { return baseDefense; }
    public final void setBaseDefense(final String string) {
        baseDefense = string;
        view.updateDefense(this);
    }

    public Set<Integer> getAttractionLights() {
        return this.attractionLights;
    }
    public final void setAttractionLights(Set<Integer> attractionLights) {
        this.attractionLights = attractionLights;
        view.updateAttractionLights(this);
    }

    public final Collection<KeywordInterface> getCachedKeywords() {
        return cachedKeywords.getValues();
    }

    public final Collection<KeywordInterface> getCachedKeyword(final Keyword keyword) {
        return cachedKeywords.getValues(keyword);
    }

    public final void setCachedKeywords(final KeywordCollection col) {
        cachedKeywords = col;
    }

    public final boolean hasKeyword(Keyword key) {
        return cachedKeywords.contains(key);
    }

    public final Collection<KeywordInterface> getIntrinsicKeywords() {
        return intrinsicKeywords.getValues();
    }
    public final boolean hasIntrinsicKeyword(String k) {
        return intrinsicKeywords.contains(k);
    }
    public final boolean hasIntrinsicKeyword(Keyword k) {
        return intrinsicKeywords.contains(k);
    }
    public final void setIntrinsicKeywords(final Iterable<KeywordInterface> intrinsicKeyword0, final boolean lki) {
        intrinsicKeywords.clear();
        for (KeywordInterface k : intrinsicKeyword0) {
            intrinsicKeywords.insert(k.copy(card, lki));
        }
        updateKeywordsCache();
    }

    public final void updateKeywordsCache() {
        card.updateKeywordsCache(this);
    }

    public final KeywordInterface addIntrinsicKeyword(final String s, boolean initTraits) {
        if (s.trim().length() == 0) {
            return null;
        }
        KeywordInterface inst = null;
        try {
            inst = intrinsicKeywords.add(s);
        } catch (Exception e) {
            String msg = "CardState:addIntrinsicKeyword: failed to parse Keyword";

            Breadcrumb bread = new Breadcrumb(msg);
            bread.setData("Card", card.getName());
            bread.setData("Keyword", s);
            Sentry.addBreadcrumb(bread);

            //rethrow
            throw new RuntimeException("Error in Keyword " + s + " for card " + card.getName(), e);
        }
        if (inst != null && initTraits) {
            inst.createTraits(card, true);
        }
        return inst;
    }
    public final boolean addIntrinsicKeywords(final Iterable<String> keywords) {
        return addIntrinsicKeywords(keywords, true);
    }
    public final boolean addIntrinsicKeywords(final Iterable<String> keywords, boolean initTraits) {
        boolean changed = false;
        for (String k : keywords) {
            if (addIntrinsicKeyword(k, initTraits) != null) {
                changed = true;
            }
        }
        return changed;
    }

    public void addIntrinsicKeywords(Collection<KeywordInterface> intrinsicKeywords2) {
        for (KeywordInterface inst : intrinsicKeywords2) {
            intrinsicKeywords.insert(inst);
        }
    }

    public final boolean removeIntrinsicKeyword(final String s) {
        return intrinsicKeywords.remove(s);
    }
    public final boolean removeIntrinsicKeyword(final KeywordInterface s) {
        return intrinsicKeywords.remove(s);
    }
    public final boolean removeIntrinsicKeyword(final Keyword k) {
        return intrinsicKeywords.removeAll(k);
    }

    public final FCollectionView<SpellAbility> getSpellAbilities() {
        FCollection<SpellAbility> newCol = new FCollection<>();
        updateSpellAbilities(newCol);
        newCol.addAll(abilities);
        card.updateSpellAbilities(newCol, this);
        return newCol;
    }
    public final FCollectionView<SpellAbility> getManaAbilities() {
        FCollection<SpellAbility> newCol = new FCollection<>();
        updateSpellAbilities(newCol);
        newCol.addAll(abilities);
        card.updateSpellAbilities(newCol, this);
        newCol.removeIf(Predicate.not(SpellAbility::isManaAbility));
        return newCol;
    }
    public final FCollectionView<SpellAbility> getNonManaAbilities() {
        FCollection<SpellAbility> newCol = new FCollection<>();
        updateSpellAbilities(newCol);
        newCol.addAll(abilities);
        card.updateSpellAbilities(newCol, this);
        newCol.removeIf(SpellAbility::isManaAbility);
        return newCol;
    }

    protected final void updateSpellAbilities(FCollection<SpellAbility> newCol) {
        // add Split to Original
        if (getStateName().equals(CardStateName.Original)) {
            if (getCard().hasState(CardStateName.LeftSplit)) {
                CardState leftState = getCard().getState(CardStateName.LeftSplit);
                newCol.addAll(leftState.abilities);
                leftState.updateSpellAbilities(newCol);
            }
            if (getCard().hasState(CardStateName.RightSplit)) {
                CardState rightState = getCard().getState(CardStateName.RightSplit);
                newCol.addAll(rightState.abilities);
                rightState.updateSpellAbilities(newCol);
            }
        }

        // SpellPermanent only for Original State
        switch(getStateName()) {
        case Backside:
            if (!getCard().isModal()) {
                return;
            }
            break;
        case Original:
        case LeftSplit:
        case RightSplit:
        case SpecializeB:
        case SpecializeG:
        case SpecializeR:
        case SpecializeU:
        case SpecializeW:
            break;
        default:
            return;
        }
        // if card has left or right split, disable intrinsic Spell for original
        if (getStateName().equals(CardStateName.Original) && (getCard().hasState(CardStateName.LeftSplit) || getCard().hasState(CardStateName.RightSplit))) {
            return;
        }

        CardTypeView type = getTypeWithChanges();
        if (type.isLand()) {
            if (landAbility == null) {
                landAbility = new LandAbility(card, this);
            }
            newCol.add(landAbility);
        } else if (type.isAura()) {
            newCol.add(getAuraSpell());
        } else if (type.isPermanent()) {
            if (abilities.anyMatch(s -> (
                    s.isBasicSpell() && s.getSubAbility() == null && (ApiType.PermanentCreature.equals(s.getApi()) || ApiType.PermanentNoncreature.equals(s.getApi())))
                )) {
                return;
            }

            if (permanentAbility == null) {
                permanentAbility = new SpellPermanent(card, this);
            }
            newCol.add(permanentAbility);
        }
    }

    public LandTraitChanges getLandTraitChanges() { return this.landTraitChanges; }

    record LandTraitChanges(CardState state, Map<MagicColor.Color, SpellAbility> map) implements ICardTraitChanges, IKeywordsChange
    {
        LandTraitChanges(CardState state) {
            this(state, Maps.newEnumMap(MagicColor.Color.class));
        }

        public List<SpellAbility> applySpellAbility(List<SpellAbility> list) {
            if (state.getCard().hasRemoveIntrinsic()) {
                list.clear();
            }
            CardTypeView type = state.getTypeWithChanges();
            if (!type.isLand()) {
                return list;
            }
            for (MagicColor.Color c : MagicColor.Color.values()) {
               if (c.getBasicLandType() == null) {
                   continue;
               }
               if (type.hasSubtype(c.getBasicLandType())) {
                   list.add(map.computeIfAbsent(c, a -> {
                       String abString  = "AB$ Mana | Cost$ T | Produced$ " + a.getShortName() +
                               " | Secondary$ True | SpellDescription$ Add " + a.getSymbol() + ".";
                       SpellAbility sa = AbilityFactory.getAbility(abString, state);
                       sa.setIntrinsic(true); // always intrinsic
                       return sa;
                   }));
               }
            }
            return list;
        }
        public List<Trigger> applyTrigger(List<Trigger> list) {
            if (state.getCard().hasRemoveIntrinsic()) {
                list.clear();
            }
            return list;
        }
        public List<ReplacementEffect> applyReplacementEffect(List<ReplacementEffect> list) {
            if (state.getCard().hasRemoveIntrinsic()) {
                list.clear();
            }
            return list;
        }
        public List<StaticAbility> applyStaticAbility(List<StaticAbility> list) {
            if (state.getCard().hasRemoveIntrinsic()) {
                list.clear();
            }
            return list;
        }
        public void applyKeywords(KeywordCollection list) {
            if (state.getCard().hasRemoveIntrinsic()) {
                list.clear();
            }
        }
        public LandTraitChanges copy(Card host, boolean lki) { return this; }
    }

    public final Iterable<SpellAbility> getIntrinsicSpellAbilities() {
        return IterableUtil.filter(getSpellAbilities(), CardTraitBase::isIntrinsic);
    }

    public final SpellAbility getFirstAbility() {
        return Iterables.getFirst(getIntrinsicSpellAbilities(), null);
    }
    public final SpellAbility getFirstSpellAbility() {
        if (this.card.getCastSA() != null) {
            return this.card.getCastSA();
        }
        return Iterables.getFirst(getNonManaAbilities(), null);
    }

    public final SpellAbility getFirstSpellAbilityWithFallback() {
        SpellAbility sa = getFirstSpellAbility();
        CardTypeView type = getTypeWithChanges();
        if (sa != null || type.isLand()) {
            return sa;
        }
        // this happens if it's transformed backside (e.g. Disturbed)
        if (type.isAura()) {
            return getAuraSpell();
        } else {
            if (permanentAbility == null) {
                permanentAbility = new SpellPermanent(card, this);
            }
            return permanentAbility;
        }
    }

    public final SpellAbility getAuraSpell() {
        CardTypeView type = getTypeWithChanges();
        if (!type.isAura()) {
            return null;
        }
        if (auraAbility == null) {
            String desc = "";
            String extra = "";
            for (KeywordInterface ki : this.getCachedKeyword(Keyword.ENCHANT)) {
                if (ki instanceof KeywordWithType kwt) {
                    desc = kwt.getTypeDescription();
                }
                break;
            }
            if (hasSVar("AttachAITgts")) {
                extra += " | AITgts$ " + getSVar("AttachAITgts");
            }
            if (hasSVar("AttachAILogic")) {
                extra += " | AILogic$ " + getSVar("AttachAILogic");
            }
            if (hasSVar("AttachAIValid")) { // TODO combine with AttachAITgts
                extra += " | AIValid$ " + getSVar("AttachAIValid");
            }
            String st = "SP$ Attach | ValidTgts$ Card.CanBeEnchantedBy,Player.CanBeEnchantedBy | TgtZone$ Battlefield,Graveyard | ValidTgtsDesc$ " + desc + extra;
            auraAbility = AbilityFactory.getAbility(st, this);
            auraAbility.setIntrinsic(true);
        }
        return this.auraAbility;
    }

    public final boolean hasSpellAbility(final SpellAbility sa) {
        return getSpellAbilities().contains(sa);
    }
    public final boolean hasSpellAbility(final int id) {
        for (SpellAbility sa : getSpellAbilities()) {
            if (id == sa.getId()) {
                return true;
            }
        }
        return false;
    }

    public final boolean addSpellAbility(final SpellAbility a) {
        return abilities.add(a);
    }

    public final FCollectionView<Trigger> getTriggers() {
        FCollection<Trigger> result = new FCollection<>(triggers);
        if (getStateName().equals(CardStateName.Original)) {
            if (getCard().hasState(CardStateName.LeftSplit))
                result.addAll(getCard().getState(CardStateName.LeftSplit).triggers);
            if (getCard().hasState(CardStateName.RightSplit))
                result.addAll(getCard().getState(CardStateName.RightSplit).triggers);
        }
        card.updateTriggers(result, this);
        return result;
    }

    public final boolean hasTrigger(final Trigger t) {
        return getTriggers().contains(t);
    }

    public final boolean hasTrigger(final int id) {
        for (final Trigger t : getTriggers()) {
            if (id == t.getId()) {
                return true;
            }
        }
        return false;
    }

    public final boolean addTrigger(final Trigger t) {
        return triggers.add(t);
    }

    public final FCollectionView<StaticAbility> getStaticAbilities() {
        FCollection<StaticAbility> result = new FCollection<>(staticAbilities);
        if (getStateName().equals(CardStateName.Original)) {
            if (getCard().hasState(CardStateName.LeftSplit))
                result.addAll(getCard().getState(CardStateName.LeftSplit).staticAbilities);
            if (getCard().hasState(CardStateName.RightSplit))
                result.addAll(getCard().getState(CardStateName.RightSplit).staticAbilities);
        }
        card.updateStaticAbilities(result, this);
        return result;
    }
    public final boolean addStaticAbility(StaticAbility stab) {
        return staticAbilities.add(stab);
    }
    public final boolean removeStaticAbility(StaticAbility stab) {
        return staticAbilities.remove(stab);
    }

    public FCollectionView<ReplacementEffect> getReplacementEffects() {
        FCollection<ReplacementEffect> result = new FCollection<>(replacementEffects);
        // add Split to Original
        if (getStateName().equals(CardStateName.Original)) {
            if (getCard().hasState(CardStateName.LeftSplit))
                result.addAll(getCard().getState(CardStateName.LeftSplit).replacementEffects);
            if (getCard().hasState(CardStateName.RightSplit))
                result.addAll(getCard().getState(CardStateName.RightSplit).replacementEffects);
        }
        CardTypeView type = getTypeWithChanges();
        if (type.isPlaneswalker()) {
            if (loyaltyRep == null) {
                loyaltyRep = CardFactoryUtil.makeEtbCounter("etbCounter:LOYALTY:" + this.baseLoyalty, this, true);
            }
            result.add(loyaltyRep);
        }
        if (type.isBattle()) {
            if (defenseRep == null) {
                defenseRep = CardFactoryUtil.makeEtbCounter("etbCounter:DEFENSE:" + this.baseDefense, this, true);
            }
            result.add(defenseRep);
        }

        card.updateReplacementEffects(result, this);

        // below are global rules
        if (type.hasSubtype("Saga") && !hasKeyword(Keyword.READ_AHEAD)) {
            if (sagaRep == null) {
                sagaRep = CardFactoryUtil.makeEtbCounter("etbCounter:LORE:1", this, false);
            }
            result.add(sagaRep);
        }
        if (type.hasSubtype("Adventure")) {
            if (this.adventureRep == null) {
                adventureRep = CardFactoryUtil.setupAdventureAbility(this);
            }
            result.add(adventureRep);
        }
        if (type.hasSubtype("Omen")) {
            if (this.omenRep == null) {
                omenRep = CardFactoryUtil.setupOmenAbility(this);
            }
            result.add(omenRep);
        }

        return result;
    }
    public boolean addReplacementEffect(final ReplacementEffect replacementEffect) {
        return replacementEffects.add(replacementEffect);
    }

    public final boolean hasReplacementEffect(final ReplacementEffect re) {
        return getReplacementEffects().contains(re);
    }
    public final boolean hasReplacementEffect(final int id) {
        return getReplacementEffect(id) != null;
    }

    public final ReplacementEffect getReplacementEffect(final int id) {
        for (final ReplacementEffect r : getReplacementEffects()) {
            if (id == r.getId()) {
                return r;
            }
        }
        return null;
    }

    @Override
    public final Map<String, String> getSVars() {
        return sVars;
    }

    @Override
    public final String getSVar(final String var) {
        if (sVars.containsKey(var)) {
            return sVars.get(var);
        }
        return "";
    }

    @Override
    public final boolean hasSVar(final String var) {
        if (var == null) {
            return false;
        }
        return sVars.containsKey(var);
    }

    @Override
    public final void setSVar(final String var, final String str) {
        sVars.put(var, str);
        view.updateFoilIndex(card.getState(CardStateName.Original));
    }

    @Override
    public final void setSVars(final Map<String, String> newSVars) {
        sVars = Maps.newTreeMap();
        sVars.putAll(newSVars);
        view.updateFoilIndex(card.getState(CardStateName.Original));
    }

    @Override
    public final void removeSVar(final String var) {
        sVars.remove(var);
    }

    public final int getFoil() {
        final String foil = getSVar("Foil");
        if (!foil.isEmpty()) {
            return Integer.parseInt(foil);
        }
        return 0;
    }

    public final void copyFrom(final CardState source, final boolean lki) {
        copyFrom(source, lki, null);
    }
    public final void copyFrom(final CardState source, final boolean lki, final CardTraitBase ctb) {
        // Makes a "deeper" copy of a CardState object
        setName(source.getName());
        setType(source.type);
        setManaCost(source.getManaCost());
        setColor(source.getColor());
        setOracleText(source.getOracleText());
        setFunctionalVariantName(source.getFunctionalVariantName());
        setBasePower(source.getBasePower());
        setBaseToughness(source.getBaseToughness());
        setBaseLoyalty(source.getBaseLoyalty());
        setBaseDefense(source.getBaseDefense());
        setAttractionLights(source.getAttractionLights());
        setFlavorName(source.getFlavorName());
        setSVars(source.getSVars());

        abilityForTrigger.clear();
        for (Map.Entry<String, SpellAbility> e : source.abilityForTrigger.entrySet()) {
            abilityForTrigger.put(e.getKey(), e.getValue().copy(card, lki));
        }

        abilities.clear();
        for (SpellAbility sa : source.abilities) {
            if (sa.isIntrinsic()) {
                abilities.add(sa.copy(card, lki));
            }
        }

        setIntrinsicKeywords(source.intrinsicKeywords.getValues(), lki);
        setImageKey(source.getImageKey());
        setRarity(source.rarity);
        setSetCode(source.setCode);

        Trigger dontCopyTr = null;
        if (ctb != null && ctb.hasParam("DoesntHaveThisAbility")) {
            SpellAbility root = ((SpellAbility) ctb).getRootAbility();
            if (root.isTrigger()) {
                dontCopyTr = root.getTrigger();
            }
        }

        triggers.clear();
        for (Trigger tr : source.triggers) {
            if (tr.equals(dontCopyTr)) {
                continue;
            }
            if (tr.isIntrinsic()) {
                triggers.add(tr.copy(card, lki, false, tr.hasParam("Execute") ? abilityForTrigger.get(tr.getParam("Execute")) : null));
            }
        }
        ReplacementEffect runRE = null;
        if (ctb instanceof SpellAbility sp && sp.isReplacementAbility()
            && source.getCard().equals(ctb.getHostCard())) {
            runRE = sp.getReplacementEffect();
        }

        replacementEffects.clear();
        for (ReplacementEffect re : source.replacementEffects) {
            if (re.isIntrinsic()) {
                ReplacementEffect reCopy = re.copy(card, lki);
                if (re.equals(runRE) && runRE.hasRun()) {
                    // CR 208.2b prevent loop from card copying itself
                    reCopy.setHasRun(true);
                }
                replacementEffects.add(reCopy);
            }
        }

        staticAbilities.clear();
        for (StaticAbility sa : source.staticAbilities) {
            if (sa.isIntrinsic()) {
                staticAbilities.add(sa.copy(card, lki));
            }
        }
        if (lki) {
            this.changedType = source.changedType;
            if (source.landAbility != null) {
                landAbility = source.landAbility.copy(card, true);
            }
            if (source.auraAbility != null) {
                auraAbility = source.auraAbility.copy(card, true);
            }
            if (source.permanentAbility != null) {
                permanentAbility = source.permanentAbility.copy(card, true);
            }
            if (source.loyaltyRep != null) {
                loyaltyRep = source.loyaltyRep.copy(card, true);
            }
            if (source.defenseRep != null) {
                defenseRep = source.defenseRep.copy(card, true);
            }
            if (source.sagaRep != null) {
                sagaRep = source.sagaRep.copy(card, true);
            }
            if (source.adventureRep != null) {
                adventureRep = source.adventureRep.copy(card, true);
            }
            if (source.omenRep != null) {
                omenRep = source.omenRep.copy(card, true);
            }
        }
    }

    public final void addAbilitiesFrom(final CardState source, final boolean lki) {
        for (SpellAbility sa : source.abilities) {
            if (sa.isIntrinsic() && sa.getApi() != ApiType.PermanentCreature && sa.getApi() != ApiType.PermanentNoncreature) {
                abilities.add(sa.copy(card, lki));
            }
        }

        for (KeywordInterface k : source.intrinsicKeywords) {
            intrinsicKeywords.insert(k.copy(card, lki));
        }

        for (Trigger tr : source.triggers) {
            if (tr.isIntrinsic()) {
                triggers.add(tr.copy(card, lki));
            }
        }

        for (ReplacementEffect re : source.replacementEffects) {
            if (re.isIntrinsic()) {
                replacementEffects.add(re.copy(card, lki));
            }
        }

        for (StaticAbility sa : source.staticAbilities) {
            if (sa.isIntrinsic()) {
                staticAbilities.add(sa.copy(card, lki));
            }
        }
    }

    public CardState copy(final Card host, CardStateName name, final boolean lki) {
        return copy(host, name, lki, null);
    }
    public CardState copy(final Card host, final CardTraitBase ctb) {
        return copy(host, this.getStateName(), false, ctb);
    }
    public CardState copy(final Card host, CardStateName name, final CardTraitBase ctb) {
        return copy(host, name, false, ctb);
    }
    public CardState copy(final Card host, CardStateName name, final boolean lki, final CardTraitBase ctb) {
        CardState result = new CardState(host, name);
        result.copyFrom(this, lki, ctb);
        return result;
    }

    public CardRarity getRarity() {
        return rarity;
    }
    public void setRarity(CardRarity rarity0) {
        rarity = rarity0;
        view.updateRarity(this);
    }

    public String getSetCode() {
        return setCode;
    }
    public void setSetCode(String setCode0) {
        setCode = setCode0;
        view.updateSetCode(this);
    }

    public final String getImageKey() {
        return imageKey;
    }
    public final void setImageKey(final String imageFilename0) {
        imageKey = imageFilename0;
        view.updateImageKey(this);
    }

    /* (non-Javadoc)
     * @see forge.game.GameObject#hasProperty(java.lang.String, forge.game.player.Player, forge.game.card.Card, forge.game.spellability.SpellAbility)
     */
    @Override
    public boolean hasProperty(String property, Player sourceController, Card source, CardTraitBase spellAbility) {
        return ForgeScript.cardStateHasProperty(this, property, sourceController, source, spellAbility);
    }

    public ImmutableList<CardTraitBase> getTraits() {
        return ImmutableList.<CardTraitBase>builder()
                .addAll(abilities)
                .addAll(triggers)
                .addAll(replacementEffects)
                .addAll(staticAbilities)
                .build();
    }

    public void resetOriginalHost(Card oldHost) {
        for (final CardTraitBase ctb : getTraits()) {
            if (ctb.isIntrinsic() && ctb.getOriginalHost() != null && ctb.getOriginalHost().equals(oldHost)) {
                // only update traits with undesired host or SVar lookup would fail
                ctb.setCardState(this);
            }
        }
    }

    public void updateChangedText() {
        for (final CardTraitBase ctb : getTraits()) {
            if (ctb.isIntrinsic()) {
                ctb.changeText();
            }
        }
    }

    public void changeTextIntrinsic(Map<String,String> colorMap, Map<String,String> typeMap) {
        for (final CardTraitBase ctb : getTraits()) {
            if (ctb.isIntrinsic()) {
                ctb.changeTextIntrinsic(colorMap, typeMap);
            }
        }
    }

    public final boolean hasChapter() {
        return getTriggers().anyMatch(Trigger::isChapter);
    }

    public final int getFinalChapterNr() {
        int n = 0;
        for (final Trigger t : getTriggers()) {
            if (t.isChapter()) {
                n = Math.max(n, t.getChapter());
            }
        }
        return n;
    }

    public SpellAbility getManifestUp() {
        if (this.manifestUp == null) {
            manifestUp = CardFactoryUtil.abilityTurnFaceUp(this, "ManifestUp", "Unmanifest");
        }
        return manifestUp;
    }
    public SpellAbility getCloakUp() {
        if (this.cloakUp == null) {
            cloakUp = CardFactoryUtil.abilityTurnFaceUp(this, "CloakUp", "Uncloak");
        }
        return cloakUp;
    }

    public SpellAbility getAbilityForTrigger(String svar) {
        return abilityForTrigger.computeIfAbsent(svar, s -> AbilityFactory.getAbility(getCard(), s, this));
    }

    @Override
    public String getTranslationKey() {
        String displayName = flavorName == null ? name : flavorName;
        if(StringUtils.isNotEmpty(functionalVariantName))
            return displayName + " $" + functionalVariantName;
        return displayName;
    }

    @Override
    public String getUntranslatedType() {
        return getType().toString();
    }

    @Override
    public String getTranslatedName() {
        return CardTranslation.getTranslatedName(this);
    }
}
