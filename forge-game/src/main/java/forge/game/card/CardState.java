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

import com.google.common.collect.Lists;

import forge.card.CardEdition;
import forge.card.CardRarity;
import forge.card.CardType;
import forge.card.CardTypeView;
import forge.card.ColorSet;
import forge.card.mana.ManaCost;
import forge.game.card.CardView.CardStateView;
import forge.game.replacement.ReplacementEffect;
import forge.game.spellability.SpellAbility;
import forge.game.staticability.StaticAbility;
import forge.game.trigger.Trigger;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.CopyOnWriteArrayList;


public class CardState {
    private String name = "";
    private CardType type = new CardType();
    private ManaCost manaCost = ManaCost.NO_COST;
    private List<CardColor> cardColor = new ArrayList<CardColor>();
    private String oracleText = "";
    private int baseAttack = 0;
    private int baseDefense = 0;
    private List<String> intrinsicKeywords = new ArrayList<String>();
    private final List<SpellAbility> nonManaAbilities = new ArrayList<SpellAbility>();
    private final List<SpellAbility> manaAbilities = new ArrayList<SpellAbility>();
    private List<String> unparsedAbilities = new ArrayList<String>();
    private List<Trigger> triggers = new CopyOnWriteArrayList<Trigger>();
    private List<ReplacementEffect> replacementEffects = new ArrayList<ReplacementEffect>();
    private List<StaticAbility> staticAbilities = new ArrayList<StaticAbility>();
    private List<String> staticAbilityStrings = new ArrayList<String>();
    private String imageKey = "";
    private Map<String, String> sVars = new TreeMap<String, String>();

    private CardRarity rarity = CardRarity.Unknown;
    private String setCode = CardEdition.UNKNOWN.getCode();

    private final CardStateView view;

    public CardState(CardStateView view0) {
        view = view0;
        view.updateRarity(this);
        view.updateSetCode(this);
    }

    public CardStateView getView() {
        return view;
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

    public final List<CardColor> getCardColor() {
        return cardColor;
    }
    public final void setCardColor(final Iterable<CardColor> cardColor0) {
        cardColor = Lists.newArrayList(cardColor0);
        view.updateColors(this);
    }
    public final void resetCardColor() {
        if (cardColor.isEmpty()) { return; }

        cardColor = Lists.newArrayList(cardColor.subList(0, 1));
        view.updateColors(this);
    }
    public final ColorSet determineColor() {
        final List<CardColor> colorList = getCardColor();
        byte colors = 0;
        for (int i = colorList.size() - 1;i >= 0;i--) {
            final CardColor cc = colorList.get(i);
            colors |= cc.getColorMask();
            if (!cc.isAdditional()) {
                return ColorSet.fromMask(colors);
            }
        }
        return ColorSet.fromMask(colors);
    }

    public String getOracleText() {
        return oracleText;
    }
    public void setOracleText(final String oracleText0) {
        oracleText = oracleText0;
        view.updateOracleText(this);
    }

    public final int getBaseAttack() {
        return baseAttack;
    }
    public final void setBaseAttack(final int baseAttack0) {
        if (baseAttack == baseAttack0) { return; }
        baseAttack = baseAttack0;
        view.updatePower(this);
    }

    public final int getBaseDefense() {
        return baseDefense;
    }
    public final void setBaseDefense(final int baseDefense0) {
        if (baseDefense == baseDefense0) { return; }
        baseDefense = baseDefense0;
        view.updateToughness(this);
    }

    public final List<String> getIntrinsicKeywords() {
        return intrinsicKeywords;
    }
    public final void setIntrinsicKeywords(final ArrayList<String> intrinsicKeyword0) {
        intrinsicKeywords = intrinsicKeyword0;
    }

    /*public final List<SpellAbility> getSpellAbilities() {
        final ArrayList<SpellAbility> res = new ArrayList<SpellAbility>(manaAbilities);
        res.addAll(nonManaAbilities);
        return res;
    }*/
    public final List<SpellAbility> getManaAbilities() {
        return manaAbilities;
    }
    public final List<SpellAbility> getNonManaAbilities() {
        return nonManaAbilities;
    }
    public final void setNonManaAbilities(SpellAbility sa) {
    	nonManaAbilities.clear();
    	if (sa != null) {
    	    nonManaAbilities.add(sa);
    	}
    }

    public final List<String> getUnparsedAbilities() {
        return unparsedAbilities;
    }
    public final void setUnparsedAbilities(final List<String> list) {
        unparsedAbilities = list;
    }

    public final List<Trigger> getTriggers() {
        return triggers;
    }
    public final void setTriggers(final List<Trigger> triggers0) {
        triggers = triggers0;
    }

    public final List<StaticAbility> getStaticAbilities() {
        return staticAbilities;
    }
    public final void setStaticAbilities(final ArrayList<StaticAbility> staticAbilities0) {
        staticAbilities = new ArrayList<StaticAbility>(staticAbilities0);
    }

    public final String getImageKey() {
        return imageKey;
    }
    public final void setImageKey(final String imageFilename0) {
        imageKey = imageFilename0;
        view.updateImageKey(this);
    }

    public final List<String> getStaticAbilityStrings() {
        return staticAbilityStrings;
    }
    public final void setStaticAbilityStrings(final ArrayList<String> staticAbilityStrings0) {
        staticAbilityStrings = staticAbilityStrings0;
    }

    public List<ReplacementEffect> getReplacementEffects() {
        return replacementEffects;
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
        // Makes a "deeper" copy of a CardCharacteristics object

        setName(source.getName());
        setType(source.type);
        setManaCost(source.getManaCost());
        setCardColor(source.getCardColor());
        setBaseAttack(source.getBaseAttack());
        setBaseDefense(source.getBaseDefense());
        intrinsicKeywords = new ArrayList<String>(source.getIntrinsicKeywords());
        unparsedAbilities = new ArrayList<String>(source.getUnparsedAbilities());
        staticAbilityStrings = new ArrayList<String>(source.getStaticAbilityStrings());
        setImageKey(source.getImageKey());
        setRarity(source.rarity);
        setSetCode(source.setCode);
        setSVars(new TreeMap<String, String>(source.getSVars()));
        replacementEffects = new ArrayList<ReplacementEffect>();
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
