package forge.card.staticability;

import java.util.HashMap;
import java.util.Map;

import forge.AllZone;
import forge.AllZoneUtil;
import forge.Card;
import forge.Constant.Zone;
import forge.GameEntity;
import forge.Player;
import forge.card.abilityfactory.AbilityFactory;
import forge.card.spellability.SpellAbility;

/**
 * The Class StaticAbility.
 */
public class StaticAbility {

    private Card hostCard = null;

    private HashMap<String, String> mapParams = new HashMap<String, String>();

    /** The temporarily suppressed. */
    private boolean temporarilySuppressed = false;

    /** The suppressed. */
    private boolean suppressed = false;

    /**
     * <p>
     * getHostCard.
     * </p>
     * 
     * @return a {@link forge.Card} object.
     */
    public final Card getHostCard() {
        return this.hostCard;
    }

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
        final HashMap<String, String> mapParameters = new HashMap<String, String>();

        if (!(abString.length() > 0)) {
            throw new RuntimeException("StaticEffectFactory : getAbility -- abString too short in "
                    + hostCard.getName() + ": [" + abString + "]");
        }

        final String[] a = abString.split("\\|");

        for (int aCnt = 0; aCnt < a.length; aCnt++) {
            a[aCnt] = a[aCnt].trim();
        }

        if (!(a.length > 0)) {
            throw new RuntimeException("StaticEffectFactory : getAbility -- a[] too short in " + hostCard.getName());
        }

        for (final String element : a) {
            final String[] aa = element.split("\\$");

            for (int aaCnt = 0; aaCnt < aa.length; aaCnt++) {
                aa[aaCnt] = aa[aaCnt].trim();
            }

            if (aa.length != 2) {
                final StringBuilder sb = new StringBuilder();
                sb.append("StaticEffectFactory Parsing Error: Split length of ");
                sb.append(element).append(" in ").append(hostCard.getName()).append(" is not 2.");
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

        if (!this.mapParams.get("Mode").equals("Continuous")) {
            return 0;
        }

        if (this.mapParams.containsKey("AddType") || this.mapParams.containsKey("RemoveType")
                || this.mapParams.containsKey("RemoveCardType") || this.mapParams.containsKey("RemoveSubType")
                || this.mapParams.containsKey("RemoveSuperType")) {
            return 4;
        }

        if (this.mapParams.containsKey("AddColor") || this.mapParams.containsKey("RemoveColor")
                || this.mapParams.containsKey("SetColor")) {
            return 5;
        }

        if (this.mapParams.containsKey("RemoveAllAbilities")) {
            return 6; // Layer 6
        }

        if (this.mapParams.containsKey("AddKeyword") || this.mapParams.containsKey("AddAbility")
                || this.mapParams.containsKey("AddTrigger") || this.mapParams.containsKey("RemoveTriggers")
                || this.mapParams.containsKey("RemoveKeyword")) {
            return 7; // Layer 6 (dependent)
        }

        if (this.mapParams.containsKey("CharacteristicDefining")) {
            return 8; // Layer 7a
        }

        if (this.mapParams.containsKey("AddPower") || this.mapParams.containsKey("AddToughness")
                || this.mapParams.containsKey("SetPower") || this.mapParams.containsKey("SetToughness")) {
            return 9; // This is the collection of 7b and 7c
        }

        if (this.mapParams.containsKey("AddHiddenKeyword")) {
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
    @Override
    public final String toString() {
        if (this.mapParams.containsKey("Description") && !this.isSuppressed()) {
            return this.mapParams.get("Description").replace("CARDNAME", this.hostCard.getName());
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
        this.mapParams = this.getMapParams(params, host);
        this.hostCard = host;
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
        this.mapParams = new HashMap<String, String>();
        for (final Map.Entry<String, String> entry : params.entrySet()) {
            this.mapParams.put(entry.getKey(), entry.getValue());
        }
        this.hostCard = host;
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
        if (!this.mapParams.get("Mode").equals(mode)) {
            return;
        }

        if (this.isSuppressed() || !this.checkConditions()) {
            return;
        }

        if (mode.equals("Continuous")) {
            StaticAbilityContinuous.applyContinuousAbility(this);
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
    public final int applyAbility(final String mode, final Card source, final GameEntity target, final int in,
            final boolean b) {

        // don't apply the ability if it hasn't got the right mode
        if (!this.mapParams.get("Mode").equals(mode)) {
            return in;
        }

        if (this.isSuppressed() || !this.checkConditions()) {
            return in;
        }

        if (mode.equals("PreventDamage")) {
            return StaticAbilityPreventDamage.applyPreventDamageAbility(this, source, target, in, b);
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
        if (!this.mapParams.get("Mode").equals(mode)) {
            return false;
        }

        if (this.isSuppressed() || !this.checkConditions()) {
            return false;
        }

        if (mode.equals("CantBeCast")) {
            return StaticAbilityCantBeCast.applyCantBeCastAbility(this, card, activator);
        }

        return false;
    }
    
    /**
     * Apply ability.
     * 
     * @param mode
     *            the mode
     * @param card
     *            the card
     * @param activator
     *            the activator
     * @param sa
     *            the ability
     * @return true, if successful
     */
    public final boolean applyAbility(final String mode, final Card card, final Player activator, SpellAbility sa) {

        // don't apply the ability if it hasn't got the right mode
        if (!this.mapParams.get("Mode").equals(mode)) {
            return false;
        }

        if (this.isSuppressed() || !this.checkConditions()) {
            return false;
        }

        if (mode.equals("CantBeActivated")) {
            return StaticAbilityCantBeCast.applyCantBeActivatedAbility(this, card, activator, sa);
        }

        return false;
    }

    /**
     * Check conditions.
     * 
     * @return true, if successful
     */
    public final boolean checkConditions() {
        final Player controller = this.hostCard.getController();

        Zone effectZone = Zone.Battlefield; // default

        if (this.mapParams.containsKey("EffectZone")) {
            effectZone = Zone.smartValueOf(this.mapParams.get("EffectZone"));
        }

        if ((effectZone != null) && (!this.hostCard.isInZone(effectZone) || this.hostCard.isPhasedOut())) {
            return false;
        }

        if (this.mapParams.containsKey("Threshold") && !controller.hasThreshold()) {
            return false;
        }

        if (this.mapParams.containsKey("Hellbent") && !controller.hasHellbent()) {
            return false;
        }

        if (this.mapParams.containsKey("Metalcraft") && !controller.hasMetalcraft()) {
            return false;
        }

        if (this.mapParams.containsKey("PlayerTurn") && !AllZone.getPhase().isPlayerTurn(controller)) {
            return false;
        }

        if (this.mapParams.containsKey("OpponentTurn") && !AllZone.getPhase().isPlayerTurn(controller.getOpponent())) {
            return false;
        }

        if (this.mapParams.containsKey("TopCardOfLibraryIs")) {
            final Card topCard = controller.getCardsIn(Zone.Library).get(0);
            if (!topCard.isValid(this.mapParams.get("TopCardOfLibraryIs").split(","), controller, this.hostCard)) {
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

        if (this.mapParams.containsKey("CheckSVar")) {
            final int sVar = AbilityFactory.calculateAmount(this.hostCard, this.mapParams.get("CheckSVar"), null);
            String comparator = "GE1";
            if (this.mapParams.containsKey("SVarCompare")) {
                comparator = this.mapParams.get("SVarCompare");
            }
            final String svarOperator = comparator.substring(0, 2);
            final String svarOperand = comparator.substring(2);
            final int operandValue = AbilityFactory.calculateAmount(this.hostCard, svarOperand, null);
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
        this.temporarilySuppressed = supp;
    }

    /**
     * Checks if is suppressed.
     * 
     * @return true, if is suppressed
     */
    public final boolean isSuppressed() {
        return (this.suppressed || this.temporarilySuppressed);
    }

} // end class StaticEffectFactory
