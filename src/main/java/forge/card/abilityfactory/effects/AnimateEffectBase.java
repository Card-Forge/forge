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
package forge.card.abilityfactory.effects;

import java.util.ArrayList;
import java.util.Map;


import forge.Card;

import forge.card.abilityfactory.SpellEffect;
import forge.card.spellability.SpellAbility;
import forge.card.staticability.StaticAbility;
import forge.card.trigger.Trigger;

public abstract class AnimateEffectBase extends SpellEffect {

    /**
     * <p>
     * doAnimate.
     * </p>
     * 
     * @param c
     *            a {@link forge.Card} object.
     * @param af
     *            a {@link forge.card.abilityfactory.AbilityFactory} object.
     * @param power
     *            a int.
     * @param toughness
     *            a int.
     * @param types
     *            a {@link java.util.ArrayList} object.
     * @param colors
     *            a {@link java.lang.String} object.
     * @param keywords
     *            a {@link java.util.ArrayList} object.
     * @return a long.
     */
    long doAnimate(final Card c, final Map<String, String> params, final int power, final int toughness,
            final ArrayList<String> types, final ArrayList<String> removeTypes, final String colors,
            final ArrayList<String> keywords, final ArrayList<String> removeKeywords,
            final ArrayList<String> hiddenKeywords, final long timestamp) {

        boolean removeSuperTypes = false;
        boolean removeCardTypes = false;
        boolean removeSubTypes = false;
        boolean removeCreatureTypes = false;

        if (params.containsKey("OverwriteTypes")) {
            removeSuperTypes = true;
            removeCardTypes = true;
            removeSubTypes = true;
            removeCreatureTypes = true;
        }

        if (params.containsKey("KeepSupertypes")) {
            removeSuperTypes = false;
        }

        if (params.containsKey("KeepCardTypes")) {
            removeCardTypes = false;
        }

        if (params.containsKey("RemoveSuperTypes")) {
            removeSuperTypes = true;
        }

        if (params.containsKey("RemoveCardTypes")) {
            removeCardTypes = true;
        }

        if (params.containsKey("RemoveSubTypes")) {
            removeSubTypes = true;
        }

        if (params.containsKey("RemoveCreatureTypes")) {
            removeCreatureTypes = true;
        }

        if ((power != -1) || (toughness != -1)) {
            c.addNewPT(power, toughness, timestamp);
        }

        if (!types.isEmpty() || !removeTypes.isEmpty() || removeCreatureTypes) {
            c.addChangedCardTypes(types, removeTypes, removeSuperTypes, removeCardTypes, removeSubTypes,
                    removeCreatureTypes, timestamp);
        }

        c.addChangedCardKeywords(keywords, removeKeywords, params.containsKey("RemoveAllAbilities"), timestamp);

        for (final String k : hiddenKeywords) {
            c.addExtrinsicKeyword(k);
        }

        final long colorTimestamp = c.addColor(colors, c, !params.containsKey("OverwriteColors"), true);
        return colorTimestamp;
    }

    /**
     * <p>
     * doUnanimate.
     * </p>
     * 
     * @param c
     *            a {@link forge.Card} object.
     * @param originalPower
     *            a int.
     * @param originalToughness
     *            a int.
     * @param originalTypes
     *            a {@link java.util.ArrayList} object.
     * @param colorDesc
     *            a {@link java.lang.String} object.
     * @param originalKeywords
     *            a {@link java.util.ArrayList} object.
     * @param addedAbilities
     *            a {@link java.util.ArrayList} object.
     * @param addedTriggers
     *            a {@link java.util.ArrayList} object.
     * @param timestamp
     *            a long.
     */
    void doUnanimate(final Card c, final Map<String, String> params, final String colorDesc,
            final ArrayList<String> addedKeywords, final ArrayList<SpellAbility> addedAbilities,
            final ArrayList<Trigger> addedTriggers, final long colorTimestamp, final boolean givesStAbs,
            final ArrayList<SpellAbility> removedAbilities, final long timestamp) {

        c.removeNewPT(timestamp);

        c.removeChangedCardKeywords(timestamp);

        // remove all static abilities
        if (givesStAbs) {
            c.setStaticAbilities(new ArrayList<StaticAbility>());
        }

        if (params.containsKey("Types") || params.containsKey("RemoveTypes")
                || params.containsKey("RemoveCreatureTypes")) {
            c.removeChangedCardTypes(timestamp);
        }

        c.removeColor(colorDesc, c, !params.containsKey("OverwriteColors"), colorTimestamp);

        for (final String k : addedKeywords) {
            c.removeExtrinsicKeyword(k);
        }

        for (final SpellAbility sa : addedAbilities) {
            c.removeSpellAbility(sa);
        }

        for (final SpellAbility sa : removedAbilities) {
            c.addSpellAbility(sa);
        }

        for (final Trigger t : addedTriggers) {
            c.removeTrigger(t);
        }

        // any other unanimate cleanup
        if (!c.isCreature()) {
            c.unEquipAllCards();
        }
    }

}
