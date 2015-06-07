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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.lang3.StringUtils;

import com.google.common.collect.Iterables;

import forge.card.CardEdition;
import forge.card.CardRarity;
import forge.card.CardType;
import forge.card.CardTypeView;
import forge.card.MagicColor;
import forge.card.mana.ManaCost;
import forge.card.mana.ManaCostParser;
import forge.game.card.CardView.CardStateView;
import forge.game.replacement.ReplacementEffect;
import forge.game.spellability.SpellAbility;
import forge.game.staticability.StaticAbility;
import forge.game.trigger.Trigger;
import forge.util.collect.FCollection;
import forge.util.collect.FCollectionView;

public class CardState {
    private String name = "";
    private CardType type = new CardType();
    private ManaCost manaCost = ManaCost.NO_COST;
    private byte color = MagicColor.COLORLESS;
    private int basePower = 0;
    private int baseToughness = 0;
    private List<String> intrinsicKeywords = new ArrayList<String>();
    private final FCollection<SpellAbility> nonManaAbilities = new FCollection<SpellAbility>();
    private final FCollection<SpellAbility> manaAbilities = new FCollection<SpellAbility>();
    private List<String> unparsedAbilities = new ArrayList<String>();
    private FCollection<Trigger> triggers = new FCollection<Trigger>();
    private FCollection<ReplacementEffect> replacementEffects = new FCollection<ReplacementEffect>();
    private FCollection<StaticAbility> staticAbilities = new FCollection<StaticAbility>();
    private List<String> staticAbilityStrings = new ArrayList<String>();
    private String imageKey = "";
    private Map<String, String> sVars = new TreeMap<String, String>();

    private CardRarity rarity = CardRarity.Unknown;
    private String setCode = CardEdition.UNKNOWN.getCode();

    private final CardStateView view;
    private final Card card;

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

    @Override
    public String toString() {
        return name + " (" + view.getState() + ")";
    }

    public final CardTypeView getType() {
        return type;
    }
    public final void addType(String type0) {
        if (type.add(type0)) {
            view.updateType(this);
        }
    }
    public final void setType(final CardType type0) {
        if (type0.isEmpty() && type.isEmpty()) { return; }
        type.clear();
        type.addAll(type0);
        view.updateType(this);
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
    public final void setColor(final String color) {
        final ManaCostParser parser = new ManaCostParser(color);
        final ManaCost cost = new ManaCost(parser);
        setColor(cost.getColorProfile());
    }
    public final void setColor(final byte color) {
        this.color = color;
        view.updateColors(card);
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

    public final Iterable<String> getIntrinsicKeywords() {
        return intrinsicKeywords;
    }
    public final boolean hasIntrinsicKeyword(String k) {
        return intrinsicKeywords.contains(k);
    }
    public final void setIntrinsicKeywords(final List<String> intrinsicKeyword0) {
        intrinsicKeywords = intrinsicKeyword0;
    }

    public final boolean addIntrinsicKeyword(final String s) {
        return s.trim().length() != 0 && intrinsicKeywords.add(s);
    }
    public final boolean addIntrinsicKeywords(final Iterable<String> keywords) {
        boolean changed = false;
        for (String k : keywords) {
            if (addIntrinsicKeyword(k)) {
                changed = false;
            }
        }
        return changed;
    }

    public final boolean addIntrinsicAbility(final String s) {
        return s.trim().length() != 0 && unparsedAbilities.add(s);
    }

    public final boolean removeIntrinsicKeyword(final String s) {
        return intrinsicKeywords.remove(s);
    }

    public final FCollectionView<SpellAbility> getSpellAbilities() {
        if (manaAbilities.isEmpty()) {
            return nonManaAbilities;
        }
        if (nonManaAbilities.isEmpty()) {
            return manaAbilities;
        }
        FCollection<SpellAbility> newCol = new FCollection<SpellAbility>(manaAbilities);
        newCol.addAll(nonManaAbilities);
        return newCol;
    }
    public final FCollectionView<SpellAbility> getManaAbilities() {
        return manaAbilities;
    }
    public final FCollectionView<SpellAbility> getNonManaAbilities() {
        return nonManaAbilities;
    }
    public final void setNonManaAbilities(SpellAbility sa) {
    	nonManaAbilities.clear();
    	if (sa != null) {
    	    nonManaAbilities.add(sa);
    	}
    }

    public final boolean addSpellAbility(final SpellAbility a) {
        if (a.isManaAbility()) {
            return manaAbilities.add(a);
        }
        return nonManaAbilities.add(a);
    }
    public final boolean removeSpellAbility(final SpellAbility a) {
        if (a.isManaAbility()) {
            // if (!a.isExtrinsic()) { return false; } //never remove intrinsic mana abilities, is this the way to go??
            return manaAbilities.remove(a);
        }
        return nonManaAbilities.remove(a);
    }
    public final boolean addManaAbility(final SpellAbility a) {
        return manaAbilities.add(a);
    }
    public final boolean addManaAbilities(final Iterable<SpellAbility> a) {
        return manaAbilities.addAll(a);
    }
    public final boolean removeManaAbility(final SpellAbility a) {
        return manaAbilities.remove(a);
    }
    public final boolean addNonManaAbility(final SpellAbility a) {
        return nonManaAbilities.add(a);
    }
    public final boolean addNonManaAbilities(final Iterable<SpellAbility> a) {
        return nonManaAbilities.addAll(a);
    }
    public final boolean removeNonManaAbility(final SpellAbility a) {
        return nonManaAbilities.remove(a);
    }

    public final void clearFirstSpell() {
        for (int i = 0; i < nonManaAbilities.size(); i++) {
            if (nonManaAbilities.get(i).isSpell()) {
                nonManaAbilities.remove(i);
                return;
            }
        }
    }

    public final Iterable<String> getUnparsedAbilities() {
        return unparsedAbilities;
    }
    public final String getFirstUnparsedAbility() {
        return Iterables.getFirst(unparsedAbilities, null);
    }
    public final boolean addUnparsedAbility(String a) {
        return unparsedAbilities.add(a);
    }
    public final void setUnparsedAbilities(final List<String> list) {
        unparsedAbilities = list;
    }

    public final FCollectionView<Trigger> getTriggers() {
        return triggers;
    }
    public final void setTriggers(final FCollection<Trigger> triggers0) {
        triggers = triggers0;
    }
    public final boolean addTrigger(final Trigger t) {
        return triggers.add(t);
    }
    public final boolean removeTrigger(final Trigger t) {
        return triggers.remove(t);
    }
    public final void clearTriggers() {
        triggers.clear();
    }

    public final FCollectionView<StaticAbility> getStaticAbilities() {
        return staticAbilities;
    }
    public final boolean addStaticAbility(StaticAbility stab) {
        return staticAbilities.add(stab);
    }
    public final boolean removeStaticAbility(StaticAbility stab) {
        return staticAbilities.remove(stab);
    }
    public final void setStaticAbilities(final Iterable<StaticAbility> staticAbilities0) {
        staticAbilities = new FCollection<StaticAbility>(staticAbilities0);
    }

    public final String getImageKey() {
        return imageKey;
    }
    public final void setImageKey(final String imageFilename0) {
        imageKey = imageFilename0;
        view.updateImageKey(this);
    }

    public final Iterable<String> getStaticAbilityStrings() {
        return staticAbilityStrings;
    }
    public final void setStaticAbilityStrings(final List<String> staticAbilityStrings0) {
        staticAbilityStrings = staticAbilityStrings0;
    }
    public boolean addStaticAbilityString(String s) {
        if (StringUtils.isBlank(s)) { return false; }
        return staticAbilityStrings.add(s);
    }

    public FCollectionView<ReplacementEffect> getReplacementEffects() {
        return replacementEffects;
    }
    public boolean addReplacementEffect(final ReplacementEffect replacementEffect) {
        return replacementEffects.add(replacementEffect);
    }
    public boolean removeReplacementEffect(final ReplacementEffect replacementEffect) {
        return replacementEffects.remove(replacementEffect);
    }
    public void clearReplacementEffects() {
        replacementEffects.clear();
    }

    public final Map<String, String> getSVars() {
        return sVars;
    }
    public final String getSVar(final String var) {
        if (sVars.containsKey(var)) {
            return sVars.get(var);
        } else {
            return "";
        }
    }
    public final boolean hasSVar(final String var) {
        return sVars.containsKey(var);
    }
    public final void setSVar(final String var, final String str) {
        sVars.put(var, str);
        view.updateFoilIndex(this);
    }
    public final void setSVars(final Map<String, String> newSVars) {
        sVars = newSVars;
        view.updateFoilIndex(this);
    }

    public final int getFoil() {
        final String foil = getSVar("Foil");
        if (!foil.isEmpty()) {
            return Integer.parseInt(foil);
        }
        return 0;
    }

    public final void copyFrom(final Card c, final CardState source) {
        // Makes a "deeper" copy of a CardState object
        setName(source.getName());
        setType(source.type);
        setManaCost(source.getManaCost());
        setColor(source.getColor());
        setBasePower(source.getBasePower());
        setBaseToughness(source.getBaseToughness());
        intrinsicKeywords = new ArrayList<String>(source.intrinsicKeywords);
        unparsedAbilities = new ArrayList<String>(source.unparsedAbilities);
        staticAbilityStrings = new ArrayList<String>(source.staticAbilityStrings);
        setImageKey(source.getImageKey());
        setRarity(source.rarity);
        setSetCode(source.setCode);
        setSVars(new TreeMap<String, String>(source.getSVars()));
        replacementEffects = new FCollection<ReplacementEffect>();
        for (ReplacementEffect RE : source.getReplacementEffects()) {
            replacementEffects.add(RE.getCopy());
        }
        view.updateKeywords(c, this);
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
}
