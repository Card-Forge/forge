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
package forge.card.spellability;

import java.util.ArrayList;
import java.util.List;

import forge.Card;
import forge.Singletons;

import forge.CardLists;
import forge.card.cardfactory.CardFactoryUtil;
import forge.card.cost.Cost;
import forge.card.cost.CostPayment;
import forge.card.staticability.StaticAbility;
import forge.error.ErrorViewer;
import forge.game.player.Player;
import forge.game.zone.ZoneType;
import forge.util.Expressions;

/**
 * <p>
 * Abstract Spell class.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 */
public abstract class Spell extends SpellAbility implements java.io.Serializable, Cloneable {

    /** Constant <code>serialVersionUID=-7930920571482203460L</code>. */
    private static final long serialVersionUID = -7930920571482203460L;
    
    private boolean castFaceDown = false;

    /**
     * <p>
     * Constructor for Spell.
     * </p>
     * 
     * @param sourceCard
     *            a {@link forge.Card} object.
     */
    public Spell(final Card sourceCard) {
        super(SpellAbility.getSpell(), sourceCard);

        this.setManaCost(sourceCard.getManaCost());
        this.setStackDescription(sourceCard.getSpellText());
        this.getRestrictions().setZone(ZoneType.Hand);
    }

    /**
     * <p>
     * Constructor for Spell.
     * </p>
     * 
     * @param sourceCard
     *            a {@link forge.Card} object.
     * @param abCost
     *            a {@link forge.card.cost.Cost} object.
     * @param abTgt
     *            a {@link forge.card.spellability.Target} object.
     */
    public Spell(final Card sourceCard, final Cost abCost, final Target abTgt) {
        super(SpellAbility.getSpell(), sourceCard);

        this.setManaCost(sourceCard.getManaCost());

        this.setPayCosts(abCost);
        this.setTarget(abTgt);
        this.setStackDescription(sourceCard.getSpellText());
        this.getRestrictions().setZone(ZoneType.Hand);
    }

    /** {@inheritDoc} */
    @Override
    public boolean canPlay() {
        if (Singletons.getModel().getGame().getStack().isSplitSecondOnStack()) {
            return false;
        }

        final Card card = this.getSourceCard();

        Player activator = this.getActivatingPlayer();
        if (activator == null) {
            activator = this.getSourceCard().getController();
        }

        if (!(card.isInstant() || activator.canCastSorcery() || card.hasKeyword("Flash")
               || this.getRestrictions().isInstantSpeed()
               || activator.hasKeyword("You may cast nonland cards as though they had flash.")
               || card.hasStartOfKeyword("You may cast CARDNAME as though it had flash."))) {
            return false;
        }

        if (!this.getRestrictions().canPlay(card, this)) {
            return false;
        }
        // for uncastables like lotus bloom, check if manaCost is blank
        if (isBasicSpell() && getManaCost().equals("")) {
            return false;
        }

        if (this.getPayCosts() != null) {
            if (!CostPayment.canPayAdditionalCosts(this.getPayCosts(), this)) {
                return false;
            }
        }

        // CantBeCast static abilities
        final List<Card> allp = new ArrayList<Card>(Singletons.getModel().getGame().getCardsIn(ZoneType.Battlefield));
        allp.add(card);
        for (final Card ca : allp) {
            final ArrayList<StaticAbility> staticAbilities = ca.getStaticAbilities();
            for (final StaticAbility stAb : staticAbilities) {
                if (stAb.applyAbility("CantBeCast", card, activator)) {
                    return false;
                }
            }
        }

        return true;
    } // canPlay()

    /** {@inheritDoc} */
    @Override
    public boolean canPlayAI() {
        final Card card = this.getSourceCard();
        if (card.getSVar("NeedsToPlay").length() > 0) {
            final String needsToPlay = card.getSVar("NeedsToPlay");
            List<Card> list = Singletons.getModel().getGame().getCardsIn(ZoneType.Battlefield);

            list = CardLists.getValidCards(list, needsToPlay.split(","), card.getController(), card);
            if (list.isEmpty()) {
                return false;
            }
        }
        if (card.getSVar("NeedsToPlayVar").length() > 0) {
            final String needsToPlay = card.getSVar("NeedsToPlayVar");
            int x = 0;
            int y = 0;
            String sVar = needsToPlay.split(" ")[0];
            String comparator = needsToPlay.split(" ")[1];
            String compareTo = comparator.substring(2);
            try {
                x = Integer.parseInt(sVar);
            } catch (final NumberFormatException e) {
                x = CardFactoryUtil.xCount(card, card.getSVar(sVar));
            }
            try {
                y = Integer.parseInt(compareTo);
            } catch (final NumberFormatException e) {
                y = CardFactoryUtil.xCount(card, card.getSVar(compareTo));
            }
            if (!Expressions.compare(x, comparator, y)) {
                return false;
            }
        }

        return super.canPlayAI();
    }

    /** {@inheritDoc} */
    @Override
    public String getStackDescription() {
        return super.getStackDescription();
    }

    /** {@inheritDoc} */
    @Override
    public final Object clone() {
        try {
            return super.clone();
        } catch (final Exception ex) {
            ErrorViewer.showError(ex);
            throw new RuntimeException("Spell : clone() error, " + ex);
        }
    }

    /**
     * <p>
     * canPlayFromEffectAI.
     * </p>
     *
     * @param mandatory
     *            can the controller chose not to play the spell
     * @param withOutManaCost
     *            is the spell cast without paying mana
     * @return a boolean.
     */
    public boolean canPlayFromEffectAI(boolean mandatory, boolean withOutManaCost) {
        return canPlayAI();
    }

    /**
     * @return the castFaceDown
     */
    public boolean isCastFaceDown() {
        return castFaceDown;
    }

    /**
     * @param faceDown the castFaceDown to set
     */
    public void setCastFaceDown(boolean faceDown) {
        this.castFaceDown = faceDown; 
    }
    
}
