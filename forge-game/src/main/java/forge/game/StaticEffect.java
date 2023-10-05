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
package forge.game;

import java.util.List;
import java.util.Map;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CardCollectionView;
import forge.game.player.Player;
import forge.game.staticability.StaticAbility;

/**
 * <p>
 * StaticEffect class.
 * </p>
 *
 * @author Forge
 * @version $Id$
 */
public class StaticEffect {

    private final Card source;
    private StaticAbility ability;
    private CardCollectionView affectedCards = new CardCollection();
    private List<Player> affectedPlayers = Lists.newArrayList();
    private long timestamp = -1;

    private Map<String, String> mapParams = Maps.newTreeMap();

    StaticEffect(final Card source) {
        this.source = source;
    }

    StaticEffect(final StaticAbility ability) {
    	this(ability.getHostCard());
        this.ability = ability;
    }

    private StaticEffect makeMappedCopy(GameObjectMap map) {
        StaticEffect copy = new StaticEffect(map.map(this.source));
        copy.ability = this.ability;
        copy.affectedCards = map.mapCollection(this.affectedCards);
        copy.affectedPlayers  = map.mapList(this.affectedPlayers);
        copy.timestamp = this.timestamp;
        copy.mapParams = this.mapParams;
        return copy;
    }

    /**
     * setTimestamp TODO Write javadoc for this method.
     *
     * @param t
     *            a long
     */
    public final void setTimestamp(final long t) {
        this.timestamp = t;
    }

    /**
     * getTimestamp. TODO Write javadoc for this method.
     *
     * @return a long
     */
    public final long getTimestamp() {
        return this.timestamp;
    }

    /**
     * <p>
     * Getter for the field <code>source</code>.
     * </p>
     *
     * @return a {@link forge.game.card.Card} object.
     */
    public final Card getSource() {
        return this.source;
    }

    /**
     * <p>
     * Getter for the field <code>affectedCards</code>.
     * </p>
     *
     * @return a {@link forge.CardList} object.
     */
    public final CardCollectionView getAffectedCards() {
        return affectedCards;
    }

    /**
     * <p>
     * Setter for the field <code>affectedCards</code>.
     * </p>
     *
     * @param list
     *            a {@link forge.CardList} object.
     */
    public final void setAffectedCards(final CardCollectionView list) {
        affectedCards = list;
    }

    /**
     * Gets the affected players.
     *
     * @return the affected players
     */
    public final List<Player> getAffectedPlayers() {
        return this.affectedPlayers;
    }

    /**
     * Sets the affected players.
     *
     * @param list
     *            the new affected players
     */
    public final void setAffectedPlayers(final List<Player> list) {
        this.affectedPlayers = list;
    }

    /**
     * setParams. TODO Write javadoc for this method.
     *
     * @param params
     *            a HashMap
     */
    public final void setParams(final Map<String, String> params) {
        this.mapParams = params;
    }

    /**
     * Gets the params.
     *
     * @return the params
     */
    public final Map<String, String> getParams() {
        return this.mapParams;
    }

    public boolean hasParam(final String key) {
        return this.mapParams.containsKey(key);
    }

    public String getParam(final String key) {
        return this.mapParams.get(key);
    }

    /**
     * Undo everything that was changed by this effect.
     *
     * @return a {@link CardCollectionView} of all affected cards.
     */
    final CardCollectionView remove() {
        final CardCollectionView affectedCards = getAffectedCards();
        final List<Player> affectedPlayers = getAffectedPlayers();

        boolean removeMayPlay = false;

        if (hasParam("MayPlay")) {
            removeMayPlay = true;
        }

        if (hasParam("IgnoreEffectCost")) {
            getSource().removeChangedCardTraits(getTimestamp(), ability.getId());
        }

        // modify players
        for (final Player p : affectedPlayers) {
            p.setUnlimitedHandSize(false);
            p.setMaxHandSize(p.getStartingHandSize());
            p.removeChangedKeywords(getTimestamp(), ability.getId());

            p.removeMaxLandPlays(getTimestamp());
            p.removeMaxLandPlaysInfinite(getTimestamp());

            p.removeControlledWhileSearching(getTimestamp());
            p.removeControlVote(getTimestamp());
            p.removeAdditionalVote(getTimestamp());
            p.removeAdditionalOptionalVote(getTimestamp());
        }

        // modify the affected card
        for (final Card affectedCard : affectedCards) {
            // Gain control
            if (hasParam("GainControl")) {
                affectedCard.removeTempController(getTimestamp());
            }

            // Revert changed color words
            affectedCard.removeChangedTextColorWord(getTimestamp(), ability.getId());

            // remove set P/T
            if (hasParam("SetPower") || hasParam("SetToughness")) {
                affectedCard.removeNewPT(getTimestamp(), ability.getId());
            }

            // remove P/T bonus
            affectedCard.removePTBoost(getTimestamp(), ability.getId());

            // remove keywords
            // (Although nothing uses it at this time)
            if (hasParam("AddKeyword") || hasParam("RemoveKeyword") || hasParam("RemoveLandTypes")
                    || hasParam("ShareRememberedKeywords") || hasParam("RemoveAllAbilities")) {
                affectedCard.removeChangedCardKeywords(getTimestamp(), ability.getId(), false);
            }

            if (hasParam("CantHaveKeyword")) {
                affectedCard.removeCantHaveKeyword(getTimestamp());
            }

            if (hasParam("AddHiddenKeyword")) {
                affectedCard.removeHiddenExtrinsicKeywords(timestamp, ability.getId());
            }

            // remove abilities
            if (hasParam("AddAbility") || hasParam("GainsAbilitiesOf") || hasParam("GainsAbilitiesOfDefined")
                    || hasParam("AddTrigger") || hasParam("AddStaticAbility") || hasParam("AddReplacementEffects")
                    || hasParam("RemoveAllAbilities") || hasParam("RemoveLandTypes")) {
                affectedCard.removeChangedCardTraits(getTimestamp(), ability.getId());
            }

            // remove Types
            if (hasParam("AddType") || hasParam("AddAllCreatureTypes") || hasParam("RemoveType") || hasParam("RemoveLandTypes")) {
                // the view is updated in GameAction#checkStaticAbilities to avoid flickering
                affectedCard.removeChangedCardTypes(getTimestamp(), ability.getId(), false);
            }

            // remove colors
            if (hasParam("AddColor") || hasParam("SetColor")) {
                affectedCard.removeColor(getTimestamp(), ability.getId());
            }

            // remove changed name
            if (hasParam("SetName") || hasParam("AddNames")) {
                affectedCard.removeChangedName(timestamp, ability.getId());
            }

            // remove may look at
            if (hasParam("MayLookAt")) {
                affectedCard.removeMayLookAt(getTimestamp());
            }
            if (removeMayPlay) {
                affectedCard.removeMayPlay(ability);
            }

            if (hasParam("GainTextOf")) {
                affectedCard.removeChangedName(getTimestamp(), ability.getId());
                affectedCard.removeChangedManaCost(getTimestamp(), ability.getId());
                affectedCard.removeColor(getTimestamp(), ability.getId());
                affectedCard.removeChangedCardTypes(getTimestamp(), ability.getId());
                affectedCard.removeChangedCardTraits(getTimestamp(), ability.getId());
                affectedCard.removeChangedCardKeywords(getTimestamp(), ability.getId());
                affectedCard.removeNewPT(getTimestamp(), ability.getId());

                affectedCard.updateChangedText();
            }

            if (hasParam("Goad")) {
                affectedCard.removeGoad(getTimestamp());
            }

            if (hasParam("CanBlockAny")) {
                affectedCard.removeCanBlockAny(getTimestamp());
            }
            if (hasParam("CanBlockAmount")) {
                affectedCard.removeCanBlockAdditional(getTimestamp());
            }

            affectedCard.removeChangedSVars(getTimestamp(), ability.getId());
        }
        return affectedCards;
    }

    public void removeMapped(GameObjectMap map) {
        makeMappedCopy(map).remove();
    }

}
