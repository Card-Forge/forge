package forge.card.spellability;

import java.util.HashMap;

import forge.AllZone;
import forge.AllZoneUtil;
import forge.CardList;
import forge.Constant.Zone;
import forge.Phase;
import forge.Player;
import forge.card.abilityFactory.AbilityFactory;
import forge.card.cardFactory.CardFactoryUtil;

/**
 * <p>
 * SpellAbility_Condition class.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 * @since 1.0.15
 */
public class SpellAbility_Condition extends SpellAbility_Variables {
    // A class for handling SpellAbility Conditions. These restrictions include:
    // Zone, Phase, OwnTurn, Speed (instant/sorcery), Amount per Turn, Player,
    // Threshold, Metalcraft, LevelRange, etc
    // Each value will have a default, that can be overridden (mostly by
    // AbilityFactory)
    // The CanPlay function will use these values to determine if the current
    // game state is ok with these restrictions

    /**
     * <p>
     * Constructor for SpellAbility_Condition.
     * </p>
     */
    public SpellAbility_Condition() {
    }

    /**
     * <p>
     * setConditions.
     * </p>
     * 
     * @param params
     *            a {@link java.util.HashMap} object.
     */
    public final void setConditions(final HashMap<String, String> params) {
        if (params.containsKey("Condition")) {
            String value = params.get("Condition");
            if (value.equals("Threshold")) {
                setThreshold(true);
            }
            if (value.equals("Metalcraft")) {
                setMetalcraft(true);
            }
            if (value.equals("Hellbent")) {
                setHellbent(true);
            }
        }

        if (params.containsKey("ConditionZone")) {
            setZone(Zone.smartValueOf(params.get("ContitionZone")));
        }

        if (params.containsKey("ConditionSorcerySpeed")) {
            setSorcerySpeed(true);
        }

        if (params.containsKey("ConditionPlayerTurn")) {
            setPlayerTurn(true);
        }

        if (params.containsKey("ConditionOpponentTurn")) {
            setOpponentTurn(true);
        }

        if (params.containsKey("ConditionPhases")) {
            String phases = params.get("ConditionPhases");

            if (phases.contains("->")) {
                // If phases lists a Range, split and Build Activate String
                // Combat_Begin->Combat_End (During Combat)
                // Draw-> (After Upkeep)
                // Upkeep->Combat_Begin (Before Declare Attackers)

                String[] split = phases.split("->", 2);
                phases = AllZone.getPhase().buildActivateString(split[0], split[1]);
            }

            setPhases(phases);
        }

        if (params.containsKey("ConditionAllM12Empires")) {
            setAllM12Empires(true);
        }
        if (params.containsKey("ConditionNotAllM12Empires")) {
            setNotAllM12Empires(true);
        }

        if (params.containsKey("ConditionCardsInHand")) {
            setActivateCardsInHand(Integer.parseInt(params.get("ConditionCardsInHand")));
        }

        // Condition version of IsPresent stuff
        if (params.containsKey("ConditionPresent")) {
            setIsPresent(params.get("ConditionPresent"));
            if (params.containsKey("ConditionCompare")) {
                setPresentCompare(params.get("ConditionCompare"));
            }
        }

        if (params.containsKey("ConditionDefined")) {
            setPresentDefined(params.get("ConditionDefined"));
        }

        if (params.containsKey("ConditionNotPresent")) {
            setIsPresent(params.get("ConditionNotPresent"));
            setPresentCompare("EQ0");
        }

        // basically PresentCompare for life totals:
        if (params.containsKey("ConditionLifeTotal")) {
            setLifeTotal(params.get("ConditionLifeTotal"));
            if (params.containsKey("ConditionLifeAmount")) {
                setLifeAmount(params.get("ConditionLifeAmount"));
            }
        }

        if (params.containsKey("ConditionManaSpent")) {
            setManaSpent(params.get("ConditionManaSpent"));
        }

        if (params.containsKey("ConditionCheckSVar")) {
            setSvarToCheck(params.get("ConditionCheckSVar"));
        }
        if (params.containsKey("ConditionSVarCompare")) {
            setSvarOperator(params.get("ConditionSVarCompare").substring(0, 2));
            setSvarOperand(params.get("ConditionSVarCompare").substring(2));
        }

    } // setConditions

    /**
     * <p>
     * checkConditions.
     * </p>
     * 
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     * @return a boolean.
     */
    public final boolean checkConditions(final SpellAbility sa) {

        Player activator = sa.getActivatingPlayer();
        if (activator == null) {
            activator = sa.getSourceCard().getController();
            System.out.println(sa.getSourceCard().getName()
                    + " Did not have activator set in SpellAbility_Condition.checkConditions()");
        }

        if (isHellbent()) {
            if (!activator.hasHellbent()) {
                return false;
            }
        }
        if (isThreshold()) {
            if (!activator.hasThreshold()) {
                return false;
            }
        }
        if (isMetalcraft()) {
            if (!activator.hasMetalcraft()) {
                return false;
            }
        }

        if (isSorcerySpeed() && !Phase.canCastSorcery(activator)) {
            return false;
        }

        if (isPlayerTurn() && !AllZone.getPhase().isPlayerTurn(activator)) {
            return false;
        }

        if (isOpponentTurn() && AllZone.getPhase().isPlayerTurn(activator)) {
            return false;
        }

        if (getActivationLimit() != -1 && getNumberTurnActivations() >= getActivationLimit()) {
            return false;
        }

        if (getPhases().size() > 0) {
            boolean isPhase = false;
            String currPhase = AllZone.getPhase().getPhase();
            for (String s : getPhases()) {
                if (s.equals(currPhase)) {
                    isPhase = true;
                    break;
                }
            }

            if (!isPhase) {
                return false;
            }
        }

        if (isAllM12Empires()) {
            Player p = sa.getSourceCard().getController();
            boolean has = AllZoneUtil.isCardInPlay("Crown of Empires", p);
            has &= AllZoneUtil.isCardInPlay("Scepter of Empires", p);
            has &= AllZoneUtil.isCardInPlay("Throne of Empires", p);
            if (!has) {
                return false;
            }
        }
        if (isNotAllM12Empires()) {
            Player p = sa.getSourceCard().getController();
            boolean has = AllZoneUtil.isCardInPlay("Crown of Empires", p);
            has &= AllZoneUtil.isCardInPlay("Scepter of Empires", p);
            has &= AllZoneUtil.isCardInPlay("Throne of Empires", p);
            if (has) {
                return false;
            }
        }

        if (getCardsInHand() != -1) {
            // Can handle Library of Alexandria, or Hellbent
            if (activator.getCardsIn(Zone.Hand).size() != getCardsInHand()) {
                return false;
            }
        }

        if (getIsPresent() != null) {
            CardList list = new CardList();
            if (getPresentDefined() != null) {
                list.addAll(AbilityFactory.getDefinedCards(sa.getSourceCard(), getPresentDefined(), sa).toArray());
            } else {
                list = AllZoneUtil.getCardsIn(Zone.Battlefield);
            }

            list = list.getValidCards(getIsPresent().split(","), sa.getActivatingPlayer(), sa.getSourceCard());

            int right;
            String rightString = getPresentCompare().substring(2);
            try { // If this is an Integer, just parse it
                right = Integer.parseInt(rightString);
            } catch (NumberFormatException e) { // Otherwise, grab it from the
                                                // SVar
                right = CardFactoryUtil.xCount(sa.getSourceCard(), sa.getSourceCard().getSVar(rightString));
            }

            int left = list.size();

            if (!AllZoneUtil.compare(left, getPresentCompare(), right)) {
                return false;
            }
        }

        if (getLifeTotal() != null) {
            int life = 1;
            if (getLifeTotal().equals("You")) {
                life = activator.getLife();
            }
            if (getLifeTotal().equals("Opponent")) {
                life = activator.getOpponent().getLife();
            }

            int right = 1;
            String rightString = getLifeAmount().substring(2);
            if (rightString.equals("X")) {
                right = CardFactoryUtil.xCount(sa.getSourceCard(), sa.getSourceCard().getSVar("X"));
            } else {
                right = Integer.parseInt(getLifeAmount().substring(2));
            }

            if (!AllZoneUtil.compare(life, getLifeAmount(), right)) {
                return false;
            }
        }

        if (null != getManaSpent()) {
            if (!sa.getSourceCard().getColorsPaid().contains(getManaSpent())) {
                return false;
            }
        }

        if (getsVarToCheck() != null) {
            int svarValue = AbilityFactory.calculateAmount(sa.getSourceCard(), getsVarToCheck(), sa);
            int operandValue = AbilityFactory.calculateAmount(sa.getSourceCard(), getsVarOperand(), sa);

            if (!AllZoneUtil.compare(svarValue, getsVarOperator(), operandValue)) {
                return false;
            }

        }

        return true;
    }

} // end class SpellAbility_Condition
