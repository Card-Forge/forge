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
import forge.game.keyword.Keyword;
import forge.game.spellability.SpellAbility;
import java.util.List;

public abstract class AnimateEffectBase extends SpellAbilityEffect {
    public static void doAnimate(final Card c, final SpellAbility sa, final Integer power, final Integer toughness,
            final CardType addType, final CardType removeType, final String colors,
            final List<String> keywords, final List<String> removeKeywords,
            final List<String> hiddenKeywords, final long timestamp) {

        boolean removeSuperTypes = false;
        boolean removeCardTypes = false;
        boolean removeSubTypes = false;
        boolean removeLandTypes = false;
        boolean removeCreatureTypes = false;
        boolean removeArtifactTypes = false;
        boolean removeEnchantmentTypes = false;

        if (sa.hasParam("OverwriteTypes")) {
            removeSuperTypes = true;
            removeCardTypes = true;
            removeSubTypes = true;
            removeLandTypes = true;
            removeCreatureTypes = true;
            removeArtifactTypes = true;
            removeEnchantmentTypes = true;
        }

        if (sa.hasParam("KeepSupertypes")) {
            removeSuperTypes = false;
        }

        if (sa.hasParam("KeepCardTypes")) {
            removeCardTypes = false;
        }

        if (sa.hasParam("KeepSubtypes")) {
            removeSubTypes = false;
            removeLandTypes = false;
            removeCreatureTypes = false;
            removeArtifactTypes = false;
            removeEnchantmentTypes = false;
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

        if (sa.hasParam("RemoveLandTypes")) {
            removeLandTypes = true;
        }
        if (sa.hasParam("RemoveCreatureTypes")) {
            removeCreatureTypes = true;
        }
        if (sa.hasParam("RemoveArtifactTypes")) {
            removeArtifactTypes = true;
        }
        if (sa.hasParam("RemoveEnchantmentTypes")) {
            removeEnchantmentTypes = true;
        }

        if ((power != null) || (toughness != null)) {
            c.addNewPT(power, toughness, timestamp);
        }

        if (!addType.isEmpty() || !removeType.isEmpty() || removeCreatureTypes) {
            c.addChangedCardTypes(addType, removeType, removeSuperTypes, removeCardTypes, removeSubTypes,
                    removeLandTypes, removeCreatureTypes, removeArtifactTypes, removeEnchantmentTypes, timestamp);
        }

        c.addChangedCardKeywords(keywords, removeKeywords,
                sa.hasParam("RemoveAllAbilities"), sa.hasParam("RemoveIntrinsicAbilities"), timestamp);

        if (sa.hasParam("CantHaveKeyword")) {
            c.addCantHaveKeyword(timestamp, Keyword.setValueOf(sa.getParam("CantHaveKeyword")));
        }

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
     *            a {@link java.util.ArrayList} object.
     * @param colorDesc
     *            a {@link java.lang.String} object.
     * @param addedAbilities
     *            a {@link java.util.ArrayList} object.
     * @param addedTriggers
     *            a {@link java.util.ArrayList} object.
     * @param timestamp
     *            a long.
     */
    static void doUnanimate(final Card c, SpellAbility sa,
            final List<String> hiddenKeywords, final long timestamp) {

        c.removeNewPT(timestamp);

        c.removeChangedCardKeywords(timestamp);

        c.removeChangedCardTypes(timestamp);
        c.removeColor(timestamp);

        c.removeChangedCardTraits(timestamp);

        c.removeCantHaveKeyword(timestamp);

        for (final String k : hiddenKeywords) {
            c.removeHiddenExtrinsicKeyword(k);
        }

        // any other unanimate cleanup
        if (!c.isCreature()) {
            c.unEquipAllCards();
        }
    }

}
