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
import com.google.common.collect.Maps;
import forge.card.*;
import forge.card.mana.ManaCost;
import forge.game.CardTraitBase;
import forge.game.ForgeScript;
import forge.game.GameObject;
import forge.game.IHasSVars;
import forge.game.ability.AbilityFactory;
import forge.game.ability.ApiType;
import forge.game.card.CardView.CardStateView;
import forge.game.keyword.Keyword;
import forge.game.keyword.KeywordCollection;
import forge.game.keyword.KeywordInterface;
import forge.game.player.Player;
import forge.game.replacement.ReplacementEffect;
import forge.game.spellability.LandAbility;
import forge.game.spellability.SpellAbility;
import forge.game.spellability.SpellAbilityPredicates;
import forge.game.spellability.SpellPermanent;
import forge.game.staticability.StaticAbility;
import forge.game.trigger.Trigger;
import forge.util.ITranslatable;
import forge.util.IterableUtil;
import forge.util.collect.FCollection;
import forge.util.collect.FCollectionView;
import io.sentry.Breadcrumb;
import io.sentry.Sentry;
import org.apache.commons.lang3.StringUtils;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class CardState extends GameObject implements IHasSVars, ITranslatable {
    private String name = "";
    private CardType type = new CardType(false);
    private ManaCost manaCost = ManaCost.NO_COST;
    private byte color = MagicColor.COLORLESS;
    private String oracleText = "";
    private String functionalVariantName = null;
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
        return getType().getTypeWithChanges(card.getChangedCardTypes());
    }

    public final CardTypeView getType() {
        return type;
    }
    public final void addType(String type0) {
        if (type.add(type0)) {
            view.updateType(this);
        }
    }
    public final void addType(Iterable<String> type0) {
        if (type.addAll(type0)) {
            view.updateType(this);
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
        view.updateType(this);
    }

    public final void removeType(final CardType.Supertype st) {
        if (type.remove(st)) {
            view.updateType(this);
        }
    }

    public final void removeCardTypes(boolean sanisfy) {
        type.removeCardTypes();
        if (sanisfy) {
            type.sanisfySubtypes();
        }
    }

    public final void setCreatureTypes(Collection<String> ctypes) {
        if (type.setCreatureTypes(ctypes)) {
            view.updateType(this);
        }
    }

    public final ManaCost getManaCost() {
        return manaCost;
    }
    public final void setManaCost(final ManaCost manaCost0) {
        manaCost = manaCost0;
        view.updateManaCost(this);
    }

    public final byte getColor() {
        return color;
    }
    public final void addColor(final byte color) {
        this.color |= color;
        view.updateColors(card);
    }
    public final void setColor(final byte color) {
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
    public final void setIntrinsicKeywords(final Iterable<KeywordInterface> intrinsicKeyword0, final boolean lki) {
        intrinsicKeywords.clear();
        for (KeywordInterface k : intrinsicKeyword0) {
            intrinsicKeywords.insert(k.copy(card, lki));
        }
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
        updateSpellAbilities(newCol, null);
        newCol.addAll(abilities);
        card.updateSpellAbilities(newCol, this, null);
        return newCol;
    }
    public final FCollectionView<SpellAbility> getManaAbilities() {
        FCollection<SpellAbility> newCol = new FCollection<>();
        updateSpellAbilities(newCol, true);
        // stream().toList() causes crash on Android, use Collectors.toList()
        newCol.addAll(abilities.stream().filter(SpellAbility::isManaAbility).collect(Collectors.toList()));
        card.updateSpellAbilities(newCol, this, true);
        return newCol;
    }
    public final FCollectionView<SpellAbility> getNonManaAbilities() {
        FCollection<SpellAbility> newCol = new FCollection<>();
        updateSpellAbilities(newCol, false);
        // stream().toList() causes crash on Android, use Collectors.toList()
        newCol.addAll(abilities.stream().filter(Predicate.not(SpellAbility::isManaAbility)).collect(Collectors.toList()));
        card.updateSpellAbilities(newCol, this, false);
        return newCol;
    }

    protected final void updateSpellAbilities(FCollection<SpellAbility> newCol, Boolean mana) {
        // add Split to Original
        if (getStateName().equals(CardStateName.Original)) {
            if (getCard().hasState(CardStateName.LeftSplit)) {
                CardState leftState = getCard().getState(CardStateName.LeftSplit);
                Collection<SpellAbility> leftAbilities = leftState.abilities;
                if (null != mana) {
                    leftAbilities = leftAbilities.stream()
                            .filter(mana ? SpellAbility::isManaAbility : Predicate.not(SpellAbility::isManaAbility))
                            // stream().toList() causes crash on Android, use Collectors.toList()
                            .collect(Collectors.toList());
                }
                newCol.addAll(leftAbilities);
                leftState.updateSpellAbilities(newCol, mana);
            }
            if (getCard().hasState(CardStateName.RightSplit)) {
                CardState rightState = getCard().getState(CardStateName.RightSplit);
                Collection<SpellAbility> rightAbilities = rightState.abilities;
                if (null != mana) {
                    rightAbilities = rightAbilities.stream()
                            .filter(mana ? SpellAbility::isManaAbility : Predicate.not(SpellAbility::isManaAbility))
                            // stream().toList() causes crash on Android, use Collectors.toList()
                            .collect(Collectors.toList());
                }
                newCol.addAll(rightAbilities);
                rightState.updateSpellAbilities(newCol, mana);
            }
        }

        if (null != mana && true == mana) {
            return;
        }

        // SpellPermanent only for Original State
        switch(getStateName()) {
        case Original:
        case LeftSplit:
        case RightSplit:
        case Modal:
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

    public final Iterable<SpellAbility> getIntrinsicSpellAbilities() {
        return IterableUtil.filter(getSpellAbilities(), SpellAbilityPredicates.isIntrinsic());
    }

    public final SpellAbility getFirstAbility() {
        return Iterables.getFirst(getIntrinsicSpellAbilities(), null);
    }
    public final SpellAbility getFirstSpellAbility() {
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
                String o = ki.getOriginal();
                String m[] = o.split(":");
                if (m.length > 2) {
                    desc = m[2];
                } else {
                    desc = m[1];
                    if (CardType.isACardType(desc) || "Permanent".equals(desc) || "Player".equals(desc) || "Opponent".equals(desc)) {
                        desc = desc.toLowerCase();
                    }
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
            String st = "SP$ Attach | ValidTgts$ Card.CanBeEnchantedBy,Player.CanBeEnchantedBy | TgtZone$ Battlefield,Graveyard | TgtPrompt$ Select target " + desc + extra;
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
            // TODO This is currently breaking for Battle/Defense
            // Going to script the cards to work but ideally it would happen here
            if (defenseRep == null) {
                defenseRep = CardFactoryUtil.makeEtbCounter("etbCounter:DEFENSE:" + this.baseDefense, this, true);
            }
            result.add(defenseRep);

            // TODO add Siege "Choose a player to protect it"
        }
        if (type.hasSubtype("Saga") && !hasKeyword(Keyword.READ_AHEAD)) {
            if (sagaRep == null) {
                sagaRep = CardFactoryUtil.makeEtbCounter("etbCounter:LORE:1", this, true);
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

        card.updateReplacementEffects(result, this);
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
        setSVars(source.getSVars());

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
                triggers.add(tr.copy(card, lki));
            }
        }

        replacementEffects.clear();
        for (ReplacementEffect re : source.replacementEffects) {
            if (re.isIntrinsic()) {
                replacementEffects.add(re.copy(card, lki));
            }
        }

        staticAbilities.clear();
        for (StaticAbility sa : source.staticAbilities) {
            if (sa.isIntrinsic()) {
                staticAbilities.add(sa.copy(card, lki));
            }
        }
        if (lki) {
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
        CardState result = new CardState(host, name);
        result.copyFrom(this, lki);
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

    @Override
    public String getTranslationKey() {
        if(StringUtils.isNotEmpty(functionalVariantName))
            return name + " $" + functionalVariantName;
        return name;
    }

    @Override
    public String getUntranslatedType() {
        return getType().toString();
    }

    @Override
    public String getUntranslatedOracle() {
        return getOracleText();
    }
}
