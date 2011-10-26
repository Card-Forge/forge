package forge.card.staticAbility;

import java.util.HashMap;
import java.util.Map;

import forge.AllZone;
import forge.AllZoneUtil;
import forge.Card;
import forge.Constant.Zone;
import forge.GameEntity;
import forge.Player;
import forge.card.abilityFactory.AbilityFactory;

/**
 * The Class StaticAbility.
 */
public class StaticAbility {

    private Card hostCard = null;

    private HashMap<String, String> mapParams = new HashMap<String, String>();

    /** The temporarily suppressed. */
    protected boolean temporarilySuppressed = false;

    /** The suppressed. */
    protected boolean suppressed = false;

    /**
     * <p>
     * getHostCard.
     * </p>
     * 
     * @return a {@link forge.Card} object.
     */
    public final Card getHostCard() {
        return hostCard;
    }

    /**
     * <p>
     * Getter for the field <code>mapParams</code>.
     * </p>
     * 
     * @return a {@link java.util.HashMap} object.
     */
    public final HashMap<String, String> getMapParams() {
        return mapParams;
    }

    // *******************************************************

    /**
     * <p>
     * Getter for the field <code>mapParams</code>.
     * </p>
     * 
     * @param abString
     *            a {@link java.lang.String} object.
     * @param hostCard
     *            a {@link forge.Card} object.
     * @return a {@link java.util.HashMap} object.
     */
    public final HashMap<String, String> getMapParams(final String abString, final Card hostCard) {
        HashMap<String, String> mapParameters = new HashMap<String, String>();

        if (!(abString.length() > 0)) {
            throw new RuntimeException("StaticEffectFactory : getAbility -- abString too short in "
                    + hostCard.getName() + ": [" + abString + "]");
        }

        String[] a = abString.split("\\|");

        for (int aCnt = 0; aCnt < a.length; aCnt++) {
            a[aCnt] = a[aCnt].trim();
        }

        if (!(a.length > 0)) {
            throw new RuntimeException("StaticEffectFactory : getAbility -- a[] too short in " + hostCard.getName());
        }

        for (int i = 0; i < a.length; i++) {
            String[] aa = a[i].split("\\$");

            for (int aaCnt = 0; aaCnt < aa.length; aaCnt++) {
                aa[aaCnt] = aa[aaCnt].trim();
            }

            if (aa.length != 2) {
                StringBuilder sb = new StringBuilder();
                sb.append("StaticEffectFactory Parsing Error: Split length of ");
                sb.append(a[i]).append(" in ").append(hostCard.getName()).append(" is not 2.");
                throw new RuntimeException(sb.toString());
            }

            mapParameters.put(aa[0], aa[1]);
        }

        return mapParameters;
    }

    // In which layer should the ability be applied (for continuous effects
    // only)
    /**
     * Gets the layer.
     * 
     * @return the layer
     */
    public final int getLayer() {

        if (!mapParams.get("Mode").equals("Continuous")) {
            return 0;
        }

        if (mapParams.containsKey("AddType") || mapParams.containsKey("RemoveType")
                || mapParams.containsKey("RemoveCardType") || mapParams.containsKey("RemoveSubType")
                || mapParams.containsKey("RemoveSuperType")) {
            return 4;
        }

        if (mapParams.containsKey("AddColor") || mapParams.containsKey("RemoveColor")
                || mapParams.containsKey("SetColor")) {
            return 5;
        }

        if (mapParams.containsKey("RemoveAllAbilities"))
         {
            return 6; // Layer 6
        }

        if (mapParams.containsKey("AddKeyword") || mapParams.containsKey("AddAbility")
                || mapParams.containsKey("AddTrigger") || mapParams.containsKey("RemoveTriggers")
                || mapParams.containsKey("RemoveKeyword"))
         {
            return 7; // Layer 6 (dependent)
        }

        if (mapParams.containsKey("CharacteristicDefining"))
         {
            return 8; // Layer 7a
        }

        if (mapParams.containsKey("AddPower") || mapParams.containsKey("AddToughness")
                || mapParams.containsKey("SetPower") || mapParams.containsKey("SetToughness"))
         {
            return 9; // This is the collection of 7b and 7c
        }

        if (mapParams.containsKey("AddHiddenKeyword"))
         {
            return 10; // rules change
        }

        // Layer 1, 2 & 3 are not supported

        return 0;
    }

    /**
     * <p>
     * toString.
     * </p>
     * 
     * @return a {@link java.lang.String} object.
     */
    public final String toString() {
        if (mapParams.containsKey("Description") && !isSuppressed()) {
            return mapParams.get("Description").replace("CARDNAME", hostCard.getName());
        } else {
            return "";
        }
    }

    // main constructor
    /**
     * Instantiates a new static ability.
     * 
     * @param params
     *            the params
     * @param host
     *            the host
     */
    public StaticAbility(final String params, final Card host) {
        mapParams = getMapParams(params, host);
        hostCard = host;
    }

    /**
     * Instantiates a new static ability.
     * 
     * @param params
     *            the params
     * @param host
     *            the host
     */
    public StaticAbility(final HashMap<String, String> params, final Card host) {
        mapParams = new HashMap<String, String>();
        for (Map.Entry<String, String> entry : params.entrySet()) {
            mapParams.put(entry.getKey(), entry.getValue());
        }
        hostCard = host;
    }

    // apply the ability if it has the right mode
    /**
     * Apply ability.
     * 
     * @param mode
     *            the mode
     */
    public final void applyAbility(final String mode) {

        // don't apply the ability if it hasn't got the right mode
        if (!mapParams.get("Mode").equals(mode)) {
            return;
        }

        if (isSuppressed() || !checkConditions()) {
            return;
        }

        if (mode.equals("Continuous")) {
            StaticAbility_Continuous.applyContinuousAbility(this);
        }
    }

    // apply the ability if it has the right mode
    /**
     * Apply ability.
     * 
     * @param mode
     *            the mode
     * @param source
     *            the source
     * @param target
     *            the target
     * @param in
     *            the in
     * @param b
     *            the b
     * @return the int
     */
    public final int applyAbility(final String mode, final Card source,
            final GameEntity target, final int in, final boolean b) {

        // don't apply the ability if it hasn't got the right mode
        if (!mapParams.get("Mode").equals(mode)) {
            return in;
        }

        if (isSuppressed() || !checkConditions()) {
            return in;
        }

        if (mode.equals("PreventDamage")) {
            return StaticAbility_PreventDamage.applyPreventDamageAbility(this, source, target, in, b);
        }

        return in;
    }

    // apply the ability if it has the right mode
    /**
     * Apply ability.
     * 
     * @param mode
     *            the mode
     * @param card
     *            the card
     * @param activator
     *            the activator
     * @return true, if successful
     */
    public final boolean applyAbility(final String mode, final Card card, final Player activator) {

        // don't apply the ability if it hasn't got the right mode
        if (!mapParams.get("Mode").equals(mode)) {
            return false;
        }

        if (isSuppressed() || !checkConditions()) {
            return false;
        }

        if (mode.equals("CantBeCast")) {
            return StaticAbility_CantBeCast.applyCantBeCastAbility(this, card, activator);
        }

        if (mode.equals("CantBeActivated")) {
            return StaticAbility_CantBeCast.applyCantBeActivatedAbility(this, card, activator);
        }

        return false;
    }

    /**
     * Check conditions.
     * 
     * @return true, if successful
     */
    public final boolean checkConditions() {
        Player controller = hostCard.getController();

        Zone effectZone = Zone.Battlefield; // default

        if (mapParams.containsKey("EffectZone")) {
            effectZone = Zone.smartValueOf(mapParams.get("EffectZone"));
        }

        if (effectZone != null
                && (!AllZone.getZoneOf(hostCard).getZoneType().equals(effectZone) || hostCard.isPhasedOut())) {
            return false;
        }

        if (mapParams.containsKey("Threshold") && !controller.hasThreshold()) {
            return false;
        }

        if (mapParams.containsKey("Hellbent") && !controller.hasHellbent()) {
            return false;
        }

        if (mapParams.containsKey("Metalcraft") && !controller.hasMetalcraft()) {
            return false;
        }

        if (mapParams.containsKey("PlayerTurn") && !AllZone.getPhase().isPlayerTurn(controller)) {
            return false;
        }

        if (mapParams.containsKey("OpponentTurn") && !AllZone.getPhase().isPlayerTurn(controller.getOpponent())) {
            return false;
        }

        if (mapParams.containsKey("TopCardOfLibraryIs")) {
            Card topCard = controller.getCardsIn(Zone.Library).get(0);
            if (!topCard.isValid(mapParams.get("TopCardOfLibraryIs").split(","), controller, hostCard)) {
                return false;
            }
        }

        /*
         * if(mapParams.containsKey("isPresent")) { String isPresent =
         * mapParams.get("isPresent"); CardList list =
         * AllZoneUtil.getCardsInPlay();
         * 
         * list = list.getValidCards(isPresent.split(","), controller,
         * hostCard);
         * 
         * }
         */

        if (mapParams.containsKey("CheckSVar")) {
            int sVar = AbilityFactory.calculateAmount(hostCard, mapParams.get("CheckSVar"), null);
            String comparator = "GE1";
            if (mapParams.containsKey("SVarCompare")) {
                comparator = mapParams.get("SVarCompare");
            }
            String svarOperator = comparator.substring(0, 2);
            String svarOperand = comparator.substring(2);
            int operandValue = AbilityFactory.calculateAmount(hostCard, svarOperand, null);
            if (!AllZoneUtil.compare(sVar, svarOperator, operandValue)) {
                return false;
            }
        }

        return true;
    }

    /**
     * Sets the temporarily suppressed.
     * 
     * @param supp
     *            the new temporarily suppressed
     */
    public final void setTemporarilySuppressed(final boolean supp) {
        temporarilySuppressed = supp;
    }

    /**
     * Checks if is suppressed.
     * 
     * @return true, if is suppressed
     */
    public final boolean isSuppressed() {
        return (suppressed || temporarilySuppressed);
    }

} // end class StaticEffectFactory
