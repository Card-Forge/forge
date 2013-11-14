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
package forge.card.cost;

import java.util.ArrayList;
import org.apache.commons.lang.StringUtils;

import forge.Card;
import forge.card.ColorSet;
import forge.card.MagicColor;
import forge.card.ability.AbilityUtils;
import forge.card.mana.Mana;
import forge.card.spellability.SpellAbility;
import forge.game.Game;
import forge.game.GameType;
import forge.game.player.Player;

/**
 * The Class CostAddMana.
 */
public class CostAddMana extends CostPart {
    /**
     * CostCostAddMana.
     * @param amount
     * @param playerSelector
     */
    public CostAddMana(final String amount, final String type, final String description) {
        super(amount, type, description);
    }

    /*
     * (non-Javadoc)
     * 
     * @see forge.card.cost.CostPart#toString()
     */
    @Override
    public final String toString() {
        final StringBuilder sb = new StringBuilder();
        final Integer i = this.convertAmount();
        sb.append("Add ").append(convertManaAmountType(i, this.getType()));
        sb.append(" to your mana pool");
        return sb.toString();
    }

    /**
     * convertManaAmountType.
     * @param i
     * @param type
     * @return a String
     */
    private String convertManaAmountType(Integer i, String type) {
        return StringUtils.repeat("{" + type + "}", i);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * forge.card.cost.CostPart#canPay(forge.card.spellability.SpellAbility,
     * forge.Card, forge.Player, forge.card.cost.Cost)
     */
    @Override
    public final boolean canPay(final SpellAbility ability) {
        return true;
    }

    /*
     * (non-Javadoc)
     * 
     * @see forge.card.cost.CostPart#payAI(forge.card.spellability.SpellAbility,
     * forge.Card, forge.card.cost.Cost_Payment)
     */
    @Override
    public final boolean payAI(final PaymentDecision decision, final Player ai, SpellAbility ability, Card source) {
        ColorSet cid = null;
        if (ai.getGame().getType() == GameType.Commander) {
            cid = ai.getCommander().getRules().getColorIdentity();
        }
        ArrayList<Mana> manaProduced = new ArrayList<Mana>();
        final String type = this.getType();
        for (int n = 0; n < decision.c; n++) {
            if (StringUtils.isNumeric(type)) {
                for (int i = Integer.parseInt(type); i > 0; i--) {
                    manaProduced.add(new Mana(MagicColor.COLORLESS, source, null));
                }
            } else {
                byte attemptedMana = MagicColor.fromName(type);
                if (cid != null) {
                    if (!cid.hasAnyColor(attemptedMana)) {
                        attemptedMana = MagicColor.COLORLESS;
                    }
                }
                manaProduced.add(new Mana(attemptedMana, source, null));
            }
        }
        ai.getManaPool().add(manaProduced);
        
        return true;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * forge.card.cost.CostPart#payHuman(forge.card.spellability.SpellAbility,
     * forge.Card, forge.card.cost.Cost_Payment)
     */
    @Override
    public final boolean payHuman(final SpellAbility ability, final Game game) {
        final Player activator = ability.getActivatingPlayer();
        final Card source = ability.getSourceCard();
        Integer c = this.convertAmount();
        if (c == null) {
            c = AbilityUtils.calculateAmount(source, this.getAmount(), ability);
        }
        ColorSet cid = null;
        if (activator.getGame().getType() == GameType.Commander) {
            cid = activator.getCommander().getRules().getColorIdentity();
        }
        ArrayList<Mana> manaProduced = new ArrayList<Mana>();
        final String type = this.getType();
        for (int n = 0; n < c; n++) {
            if (StringUtils.isNumeric(type)) {
                for (int i = Integer.parseInt(type); i > 0; i--) {
                    manaProduced.add(new Mana(MagicColor.COLORLESS, source, null));
                }
            } else {
                byte attemptedMana = MagicColor.fromName(type);
                if (cid != null) {
                    if (!cid.hasAnyColor(attemptedMana)) {
                        attemptedMana = MagicColor.COLORLESS;
                    }
                }
                manaProduced.add(new Mana(attemptedMana, source, null));
            }
        }
        activator.getManaPool().add(manaProduced);
        return true;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * forge.card.cost.CostPart#decideAIPayment(forge.card.spellability.SpellAbility
     * , forge.Card, forge.card.cost.Cost_Payment)
     */
    @Override
    public final PaymentDecision decideAIPayment(final Player ai, final SpellAbility ability, final Card source) {
        Integer c = this.convertAmount();

        if (c == null) {
            c = AbilityUtils.calculateAmount(source, this.getAmount(), ability);
        }

        return new PaymentDecision(c);
    }
}
