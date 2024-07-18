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
package forge.game.cost;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import forge.game.ability.AbilityKey;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CardCollectionView;
import forge.game.card.CardLists;
import forge.game.card.CardPredicates;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.trigger.TriggerType;
import forge.game.zone.ZoneType;
import forge.util.TextUtil;

/**
 * The Class CostDiscard.
 */
public class CostDiscard extends CostPartWithList {
    // Discard<Num/Type{/TypeDescription}>

    // Inputs

    protected List<Card> discardedBefore;

    private static final long serialVersionUID = 1L;

    /**
     * Instantiates a new cost discard.
     *
     * @param amount
     *            the amount
     * @param type
     *            the type
     * @param description
     *            the description
     */
    public CostDiscard(final String amount, final String type, final String description) {
        super(amount, type, description);
    }

    public int paymentOrder() { return 10; }

    @Override
    public Integer getMaxAmountX(SpellAbility ability, Player payer, final boolean effect) {
        final Card source = ability.getHostCard();
        String type = this.getType();
        CardCollectionView handList = payer.canDiscardBy(ability, effect) ? payer.getCardsIn(ZoneType.Hand) : CardCollection.EMPTY;

        if (!type.equals("Random")) {
            handList = CardLists.getValidCards(handList, type.split(";"), payer, source, ability);
        }
        return handList.size();
    }

    /*
     * (non-Javadoc)
     *
     * @see forge.card.cost.CostPart#toString()
     */
    @Override
    public final String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("Discard ");

        final Integer i = this.convertAmount();

        if (this.payCostFromSource()) {
            sb.append(this.getType());
        }
        else if (this.getType().equals("Hand")) {
            sb.append("your hand");
        }
        else if (this.getType().equals("LastDrawn")) {
            sb.append("the last card you drew this turn");
        }
        else if (this.getType().equals("DifferentNames")) {
            sb.append(Cost.convertAmountTypeToWords(i, this.getAmount(), "Card")).append(" with different names");
        }
        else {
            final StringBuilder desc = new StringBuilder();

            if (this.getType().equals("Card") || this.getType().equals("Random")) {
                desc.append("card");
            }
            else {
                desc.append(this.getDescriptiveType());
                desc.append(" card");
            }

            sb.append(Cost.convertAmountTypeToWords(i, this.getAmount(), desc.toString()));

            if (this.getType().equals("Random")) {
                sb.append(" at random");
            }
        }
        return sb.toString();
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * forge.card.cost.CostPart#canPay(forge.card.spellability.SpellAbility,
     * forge.Card, forge.Player, forge.card.cost.Cost)
     */
    @Override
    public final boolean canPay(final SpellAbility ability, final Player payer, final boolean effect) {
        final Card source = ability.getHostCard();

        CardCollectionView handList = payer.canDiscardBy(ability, effect) ? payer.getCardsIn(ZoneType.Hand) : CardCollection.EMPTY;
        String type = this.getType();
        final int amount = getAbilityAmount(ability);

        if (this.payCostFromSource()) {
            return source.canBeDiscardedBy(ability, effect);
        }
        else if (type.equals("Hand")) {
            // trying to discard an empty hand always work even with Tamiyo
            if (payer.getZone(ZoneType.Hand).isEmpty()) {
                return true;
            }
            return payer.canDiscardBy(ability, effect);
            // this will always work
        }
        else if (type.equals("LastDrawn")) {
            final Card c = payer.getLastDrawnCard();
            return handList.contains(c);
        }
        else if (type.equals("DifferentNames")) {
            Set<String> cardNames = Sets.newHashSet();
            for (Card c : handList) {
                if (!c.hasNoName()) {
                    cardNames.add(c.getName());
                }
            }
            return cardNames.size() >= amount;
        }
        else {
            boolean sameName = false;
            if (type.contains("+WithSameName")) {
                sameName = true;
                type = TextUtil.fastReplace(type, "+WithSameName", "");
            }
            if (type.contains("ChosenColor") && !source.hasChosenColor()) {
                //color hasn't been chosen yet, so skip getValidCards
            } else if (!type.equals("Random") && !type.contains("X")) {
                // Knollspine Invocation fails to activate without the above conditional
                handList = CardLists.getValidCards(handList, type.split(";"), payer, source, ability);
            }
            if (sameName) {
                for (Card c : handList) {
                    if (CardLists.count(handList, CardPredicates.nameEquals(c.getName())) > 1) {
                        return true;
                    }
                }
                return false;
            }
            int adjustment = 0;
            if (source.isInZone(ZoneType.Hand) && payer.equals(source.getOwner())) {
                // If this card is in my hand, I can't use it to pay for it's own cost
                if (handList.contains(source)) {
                    adjustment = 1;
                }
            }

            if (amount > handList.size() - adjustment) {
                // not enough cards in hand to pay
                return false;
            }
        }
        return true;
    }

    /* (non-Javadoc)
     * @see forge.card.cost.CostPartWithList#executePayment(forge.card.spellability.SpellAbility, forge.Card)
     */
    @Override
    protected Card doPayment(Player payer, SpellAbility ability, Card targetCard, final boolean effect) {
        final Map<AbilityKey, Object> runParams = AbilityKey.newMap();
        AbilityKey.addCardZoneTableParams(runParams, table);

        if (ability.isCycling() && targetCard.equals(ability.getHostCard())) {
            // discard itself for cycling cost
            runParams.put(AbilityKey.Cycling, true);
        }
        // if this is caused by 118.12 it's also an effect
        SpellAbility cause = targetCard.getGame().getStack().isResolving(ability.getHostCard()) ? ability : null;
        return payer.discard(targetCard, cause, effect, runParams);
    }

    /* (non-Javadoc)
     * @see forge.card.cost.CostPartWithList#getHashForList()
     */
    @Override
    public String getHashForLKIList() {
        return "Discarded";
    }
    public String getHashForCardList() {
    	return "DiscardedCards";
    }

    @Override
    public <T> T accept(ICostVisitor<T> visitor) {
        return visitor.visit(this);
    }

    protected void handleBeforePayment(Player ai, SpellAbility ability, CardCollectionView targetCards) {
        discardedBefore = Lists.newArrayList(ai.getDiscardedThisTurn());
    }

    @Override
    protected void handleChangeZoneTrigger(Player payer, SpellAbility ability, CardCollectionView targetCards) {
        super.handleChangeZoneTrigger(payer, ability, targetCards);

        if (!cardList.isEmpty()) {
            final Map<AbilityKey, Object> runParams = AbilityKey.mapFromPlayer(payer);
            runParams.put(AbilityKey.Cards, new CardCollection(cardList));
            runParams.put(AbilityKey.Cause, ability);
            runParams.put(AbilityKey.DiscardedBefore, discardedBefore);
            payer.getGame().getTriggerHandler().runTrigger(TriggerType.DiscardedAll, runParams, false);
        }
    }
}
