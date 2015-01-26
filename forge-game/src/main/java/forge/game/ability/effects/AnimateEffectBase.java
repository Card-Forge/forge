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
package forge.game.ability.effects;

import forge.card.CardType;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.replacement.ReplacementEffect;
import forge.game.spellability.SpellAbility;
import forge.game.staticability.StaticAbility;
import forge.game.trigger.Trigger;

import java.util.ArrayList;
import java.util.List;

public abstract class AnimateEffectBase extends SpellAbilityEffect {
    void doAnimate(final Card c, final SpellAbility sa, final int power, final int toughness,
            final CardType addType, final CardType removeType, final String colors,
            final List<String> keywords, final List<String> removeKeywords,
            final List<String> hiddenKeywords, final long timestamp) {

        boolean removeSuperTypes = false;
        boolean removeCardTypes = false;
        boolean removeSubTypes = false;
        boolean removeCreatureTypes = false;

        if (sa.hasParam("OverwriteTypes")) {
            removeSuperTypes = true;
            removeCardTypes = true;
            removeSubTypes = true;
            removeCreatureTypes = true;
        }

        if (sa.hasParam("KeepSupertypes")) {
            removeSuperTypes = false;
        }

        if (sa.hasParam("KeepCardTypes")) {
            removeCardTypes = false;
        }

        if (sa.hasParam("RemoveSuperTypes")) {
            removeSuperTypes = true;
        }

        if (sa.hasParam("RemoveCardTypes")) {
            removeCardTypes = true;
        }

        if (sa.hasParam("RemoveSubTypes")) {
            removeSubTypes = true;
        }

        if (sa.hasParam("RemoveCreatureTypes")) {
            removeCreatureTypes = true;
        }

        if ((power != -1) || (toughness != -1)) {
            c.addNewPT(power, toughness, timestamp);
        }

        if (!addType.isEmpty() || !removeType.isEmpty() || removeCreatureTypes) {
            c.addChangedCardTypes(addType, removeType, removeSuperTypes, removeCardTypes, removeSubTypes,
                    removeCreatureTypes, timestamp);
        }

        c.addChangedCardKeywords(keywords, removeKeywords, sa.hasParam("RemoveAllAbilities"), timestamp);

        for (final String k : hiddenKeywords) {
            c.addHiddenExtrinsicKeyword(k);
        }

        c.addColor(colors, !sa.hasParam("OverwriteColors"), timestamp);
    }

    /**
     * <p>
     * doUnanimate.
     * </p>
     * 
     * @param c
     *            a {@link forge.game.card.Card} object.
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
    void doUnanimate(final Card c, SpellAbility sa, final String colorDesc,
            final List<String> hiddenKeywords, final List<SpellAbility> addedAbilities,
            final List<Trigger> addedTriggers, final List<ReplacementEffect> addedReplacements, 
            final boolean givesStAbs, final List<SpellAbility> removedAbilities, final long timestamp) {

        c.removeNewPT(timestamp);

        c.removeChangedCardKeywords(timestamp);

        // remove all static abilities
        if (givesStAbs) {
            c.setStaticAbilities(new ArrayList<StaticAbility>());
        }

        if (sa.hasParam("Types") || sa.hasParam("RemoveTypes")
                || sa.hasParam("RemoveCreatureTypes")) {
            c.removeChangedCardTypes(timestamp);
        }

        c.removeColor(timestamp);

        for (final String k : hiddenKeywords) {
            c.removeHiddenExtrinsicKeyword(k);
        }

        for (final SpellAbility saAdd : addedAbilities) {
            c.removeSpellAbility(saAdd);
        }

        for (final SpellAbility saRem : removedAbilities) {
            c.addSpellAbility(saRem);
        }

        for (final Trigger t : addedTriggers) {
            c.removeTrigger(t);
        }

        for (final ReplacementEffect rep : addedReplacements) {
            c.removeReplacementEffect(rep);
        }

        // any other unanimate cleanup
        if (!c.isCreature()) {
            c.unEquipAllCards();
        }
    }

}
