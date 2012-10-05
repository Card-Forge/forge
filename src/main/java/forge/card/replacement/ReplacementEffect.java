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
package forge.card.replacement;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import forge.AllZoneUtil;
import forge.Card;

import forge.CardLists;
import forge.CardUtil;
import forge.card.TriggerReplacementBase;
import forge.card.abilityfactory.AbilityFactory;
import forge.card.cardfactory.CardFactoryUtil;
import forge.card.spellability.SpellAbility;
import forge.game.zone.ZoneType;

/**
 * TODO: Write javadoc for this type.
 * 
 */
public abstract class ReplacementEffect extends TriggerReplacementBase {

    private ReplacementLayer layer = ReplacementLayer.None;
    
    /** The has run. */
    private boolean hasRun = false;

    /**
     * Checks for run.
     * 
     * @return the hasRun
     */
    public final boolean hasRun() {
        return this.hasRun;
    }

    /**
     * Checks if is secondary.
     *
     * @return true, if is secondary
     */
    public final boolean isSecondary() {
        return this.mapParams.containsKey("Secondary");
    }

    /**
     * Ai should run.
     *
     * @param sa the sa
     * @return true, if successful
     */
    public final boolean aiShouldRun(final SpellAbility sa) {
        if (this.mapParams.containsKey("AICheckSVar")) {
            System.out.println("aiShouldRun?" + sa);
            final String svarToCheck = this.mapParams.get("AICheckSVar");
            String comparator = "GE";
            int compareTo = 1;

            if (this.mapParams.containsKey("AISVarCompare")) {
                final String fullCmp = this.mapParams.get("AISVarCompare");
                comparator = fullCmp.substring(0, 2);
                final String strCmpTo = fullCmp.substring(2);
                try {
                    compareTo = Integer.parseInt(strCmpTo);
                } catch (final Exception ignored) {
                    if (sa == null) {
                        compareTo = CardFactoryUtil.xCount(this.hostCard, this.hostCard.getSVar(strCmpTo));
                    } else {
                        compareTo = AbilityFactory.calculateAmount(this.hostCard, this.hostCard.getSVar(strCmpTo), sa);
                    }
                }
            }

            int left = 0;

            if (sa == null) {
                left = CardFactoryUtil.xCount(this.hostCard, this.hostCard.getSVar(svarToCheck));
            } else {
                left = AbilityFactory.calculateAmount(this.hostCard, svarToCheck, sa);
            }
            System.out.println("aiShouldRun?" + left + comparator + compareTo);
            if (AllZoneUtil.compare(left, comparator, compareTo)) {
                return true;
            }
        } else if (sa != null && sa.doTrigger(false)){
            return true;
        }

        return false;
    }

    /**
     * Sets the checks for run.
     * 
     * @param hasRun
     *            the hasRun to set
     */
    public final void setHasRun(final boolean hasRun) {
        this.hasRun = hasRun;
    }

    /** The map params, denoting what to replace. */
    private HashMap<String, String> mapParams = new HashMap<String, String>();

    /**
     * <p>
     * Getter for the field <code>mapParams</code>.
     * </p>
     * 
     * @return a {@link java.util.HashMap} object.
     */
    public final HashMap<String, String> getMapParams() {
        return this.mapParams;
    }

    /**
     * Sets the map params.
     * 
     * @param mapParams
     *            the mapParams to set
     */
    public final void setMapParams(final HashMap<String, String> mapParams) {
        this.mapParams = mapParams;
    }

    /**
     * Can replace.
     * 
     * @param runParams
     *            the run params
     * @return true, if successful
     */
    public abstract boolean canReplace(final HashMap<String, Object> runParams);

    /**
     * To string.
     *
     * @return a String
     */
    @Override
    public String toString() {
        if (this.getMapParams().containsKey("Description") && !this.isSuppressed()) {
            return this.getMapParams().get("Description");
        } else {
            return "";
        }
    }

    /**
     * <p>
     * requirementsCheck.
     * </p>
     * 
     * @return a boolean.
     */
    public boolean requirementsCheck() {

        if (this.isSuppressed()) {
            return false; // Effect removed by effect
        }

        if (this.getMapParams().containsKey("Metalcraft")) {
            if (this.getMapParams().get("Metalcraft").equals("True")
                    && !this.getHostCard().getController().hasMetalcraft()) {
                return false;
            }
        }

        if (this.getMapParams().containsKey("Threshold")) {
            if (this.getMapParams().get("Threshold").equals("True")
                    && !this.getHostCard().getController().hasThreshold()) {
                return false;
            }
        }

        if (this.getMapParams().containsKey("Hellbent")) {
            if (this.getMapParams().get("Hellbent").equals("True") && !this.getHostCard().getController().hasHellbent()) {
                return false;
            }
        }

        if (this.getMapParams().containsKey("PlayersPoisoned")) {
            if (this.getMapParams().get("PlayersPoisoned").equals("You")
                    && (this.getHostCard().getController().getPoisonCounters() == 0)) {
                return false;
            } else if (this.getMapParams().get("PlayersPoisoned").equals("Opponent")
                    && (this.getHostCard().getController().getOpponent().getPoisonCounters() == 0)) {
                return false;
            } else if (this.getMapParams().get("PlayersPoisoned").equals("Each")
                    && !((this.getHostCard().getController().getPoisonCounters() != 0) && (this.getHostCard()
                            .getController().getPoisonCounters() != 0))) {
                return false;
            }
        }

        if (this.getMapParams().containsKey("LifeTotal")) {
            final String player = this.getMapParams().get("LifeTotal");
            String lifeCompare = "GE1";
            int life = 1;

            if (player.equals("You")) {
                life = this.getHostCard().getController().getLife();
            }
            if (player.equals("Opponent")) {
                life = this.getHostCard().getController().getOpponent().getLife();
            }

            if (this.getMapParams().containsKey("LifeAmount")) {
                lifeCompare = this.getMapParams().get("LifeAmount");
            }

            int right = 1;
            final String rightString = lifeCompare.substring(2);
            try {
                right = Integer.parseInt(rightString);
            } catch (final NumberFormatException nfe) {
                right = CardFactoryUtil.xCount(this.getHostCard(), this.getHostCard().getSVar(rightString));
            }

            if (!AllZoneUtil.compare(life, lifeCompare, right)) {
                return false;
            }

        }

        if (this.getMapParams().containsKey("IsPresent")) {
            final String sIsPresent = this.getMapParams().get("IsPresent");
            String presentCompare = "GE1";
            ZoneType presentZone = ZoneType.Battlefield;
            String presentPlayer = "Any";
            if (this.getMapParams().containsKey("PresentCompare")) {
                presentCompare = this.getMapParams().get("PresentCompare");
            }
            if (this.getMapParams().containsKey("PresentZone")) {
                presentZone = ZoneType.smartValueOf(this.getMapParams().get("PresentZone"));
            }
            if (this.getMapParams().containsKey("PresentPlayer")) {
                presentPlayer = this.getMapParams().get("PresentPlayer");
            }
            List<Card> list = new ArrayList<Card>();
            if (presentPlayer.equals("You") || presentPlayer.equals("Any")) {
                list.addAll(this.getHostCard().getController().getCardsIn(presentZone));
            }
            if (presentPlayer.equals("Opponent") || presentPlayer.equals("Any")) {
                list.addAll(this.getHostCard().getController().getOpponent().getCardsIn(presentZone));
            }

            list = CardLists.getValidCards(list, sIsPresent.split(","), this.getHostCard().getController(), this.getHostCard());

            int right = 1;
            final String rightString = presentCompare.substring(2);
            if (rightString.equals("X")) {
                right = CardFactoryUtil.xCount(this.getHostCard(), this.getHostCard().getSVar("X"));
            } else {
                right = Integer.parseInt(presentCompare.substring(2));
            }
            final int left = list.size();

            if (!AllZoneUtil.compare(left, presentCompare, right)) {
                return false;
            }

        }

        if (this.getMapParams().containsKey("IsPresent2")) {
            final String sIsPresent = this.getMapParams().get("IsPresent2");
            String presentCompare = "GE1";
            ZoneType presentZone = ZoneType.Battlefield;
            String presentPlayer = "Any";
            if (this.getMapParams().containsKey("PresentCompare2")) {
                presentCompare = this.getMapParams().get("PresentCompare2");
            }
            if (this.getMapParams().containsKey("PresentZone2")) {
                presentZone = ZoneType.smartValueOf(this.getMapParams().get("PresentZone2"));
            }
            if (this.getMapParams().containsKey("PresentPlayer2")) {
                presentPlayer = this.getMapParams().get("PresentPlayer2");
            }
            List<Card> list = new ArrayList<Card>();
            if (presentPlayer.equals("You") || presentPlayer.equals("Any")) {
                list.addAll(this.getHostCard().getController().getCardsIn(presentZone));
            }
            if (presentPlayer.equals("Opponent") || presentPlayer.equals("Any")) {
                list.addAll(this.getHostCard().getController().getOpponent().getCardsIn(presentZone));
            }

            list = CardLists.getValidCards(list, sIsPresent.split(","), this.getHostCard().getController(), this.getHostCard());

            int right = 1;
            final String rightString = presentCompare.substring(2);
            if (rightString.equals("X")) {
                right = CardFactoryUtil.xCount(this.getHostCard(), this.getHostCard().getSVar("X"));
            } else {
                right = Integer.parseInt(presentCompare.substring(2));
            }
            final int left = list.size();

            if (!AllZoneUtil.compare(left, presentCompare, right)) {
                return false;
            }

        }

        if (this.getMapParams().containsKey("CheckSVar")) {
            final int sVar = AbilityFactory.calculateAmount(AllZoneUtil.getCardState(this.getHostCard()), this
                    .getMapParams().get("CheckSVar"), null);
            String comparator = "GE1";
            if (this.getMapParams().containsKey("SVarCompare")) {
                comparator = this.getMapParams().get("SVarCompare");
            }
            final String svarOperator = comparator.substring(0, 2);
            final String svarOperand = comparator.substring(2);
            final int operandValue = AbilityFactory.calculateAmount(AllZoneUtil.getCardState(this.getHostCard()),
                    svarOperand, null);
            if (!AllZoneUtil.compare(sVar, svarOperator, operandValue)) {
                return false;
            }
        }

        if (this.getMapParams().containsKey("ManaSpent")) {
            if (!this.getHostCard().getColorsPaid().contains(this.getMapParams().get("ManaSpent"))) {
                return false;
            }
        }

        if (this.getMapParams().containsKey("ManaNotSpent")) {
            if (this.getHostCard().getColorsPaid().contains(this.getMapParams().get("ManaNotSpent"))) {
                return false;
            }
        }

        if (this.getMapParams().containsKey("WerewolfTransformCondition")) {
            if (CardUtil.getLastTurnCast("Card", this.getHostCard()).size() > 0) {
                return false;
            }
        }

        if (this.getMapParams().containsKey("WerewolfUntransformCondition")) {
            final List<Card> you = CardUtil.getLastTurnCast("Card.YouCtrl", this.getHostCard());
            final List<Card> opp = CardUtil.getLastTurnCast("Card.YouDontCtrl", this.getHostCard());
            if (!((you.size() > 1) || (opp.size() > 1))) {
                return false;
            }
        }

        return true;
    }

    /**
     * Gets the copy.
     * 
     * @return the copy
     */
    public abstract ReplacementEffect getCopy();

    /**
     * Sets the replacing objects.
     * 
     * @param runParams
     *            the run params
     * @param spellAbility
     *            the SpellAbility
     */
    public void setReplacingObjects(final HashMap<String, Object> runParams, final SpellAbility spellAbility) {
        // Should be overridden by replacers that need it.
    }

    /**
     * Instantiates a new replacement effect.
     * 
     * @param map
     *            the map
     * @param host
     *            the host
     */
    public ReplacementEffect(final HashMap<String, String> map, final Card host) {
        this.setMapParams(map);
        this.setHostCard(host);
    }

    /**
     * @return the layer
     */
    public ReplacementLayer getLayer() {
        return layer;
    }

    /**
     * @param layer0 the layer to set
     */
    public void setLayer(ReplacementLayer layer0) {
        this.layer = layer0;
    }
}
