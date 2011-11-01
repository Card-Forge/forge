package forge.card.trigger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import forge.AllZone;
import forge.AllZoneUtil;
import forge.Card;
import forge.CardList;
import forge.CardUtil;
import forge.Constant.Zone;
import forge.Player;
import forge.PlayerZone;
import forge.card.abilityFactory.AbilityFactory;
import forge.card.cardFactory.CardFactoryUtil;
import forge.card.spellability.SpellAbility;

/**
 * <p>
 * Abstract Trigger class.
 * </p>
 * 
 * @author Forge
 * @version $Id$
 */
public abstract class Trigger {

    /** Constant <code>nextID=0</code>. */
    private static int nextID = 0;

    /**
     * <p>
     * resetIDs.
     * </p>
     */
    public static void resetIDs() {
        nextID = 50000;
    }

    /** The ID. */
    protected int ID = nextID++;

    /** The name. */
    protected String name;

    /**
     * <p>
     * Getter for the field <code>name</code>.
     * </p>
     * 
     * @return a {@link java.lang.String} object.
     */
    public final String getName() {
        return name;
    }

    /**
     * <p>
     * Setter for the field <code>name</code>.
     * </p>
     * 
     * @param n
     *            a {@link java.lang.String} object.
     */
    public final void setName(final String n) {
        name = n;
    }

    /**
     * <p>
     * setID.
     * </p>
     * 
     * @param id
     *            a int.
     */
    public final void setID(final int id) {
        ID = id;
    }

    /** The map params. */
    protected HashMap<String, String> mapParams = new HashMap<String, String>();

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

    /** The run params. */
    protected Map<String, Object> runParams;

    /**
     * <p>
     * Setter for the field <code>runParams</code>.
     * </p>
     * 
     * @param runParams2
     *            a {@link java.util.Map} object.
     */
    public final void setRunParams(final Map<String, Object> runParams2) {
        runParams = runParams2;
    }

    /**
     * <p>
     * Getter for the field <code>runParams</code>.
     * </p>
     * 
     * @return a {@link java.util.Map} object.
     */
    public final Map<String, Object> getRunParams() {
        return runParams;
    }

    /** The overriding ability. */
    protected SpellAbility overridingAbility = null;

    /**
     * <p>
     * Getter for the field <code>overridingAbility</code>.
     * </p>
     * 
     * @return a {@link forge.card.spellability.SpellAbility} object.
     */
    public final SpellAbility getOverridingAbility() {
        return overridingAbility;
    }

    /**
     * <p>
     * Setter for the field <code>overridingAbility</code>.
     * </p>
     * 
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     */
    public final void setOverridingAbility(final SpellAbility sa) {
        overridingAbility = sa;
    }

    private HashMap<String, Object> storedTriggeredObjects = null;

    /**
     * <p>
     * Setter for the field <code>storedTriggeredObjects</code>.
     * </p>
     * 
     * @param storedTriggeredObjects
     *            a {@link java.util.HashMap} object.
     * @since 1.0.15
     */
    public final void setStoredTriggeredObjects(final HashMap<String, Object> storedTriggeredObjects) {
        this.storedTriggeredObjects = storedTriggeredObjects;
    }

    /**
     * <p>
     * Getter for the field <code>storedTriggeredObjects</code>.
     * </p>
     * 
     * @return a {@link java.util.HashMap} object.
     * @since 1.0.15
     */
    public final HashMap<String, Object> getStoredTriggeredObjects() {
        return storedTriggeredObjects;
    }

    /** The host card. */
    protected Card hostCard;

    /**
     * <p>
     * Getter for the field <code>hostCard</code>.
     * </p>
     * 
     * @return a {@link forge.Card} object.
     */
    public final Card getHostCard() {
        return hostCard;
    }

    /**
     * <p>
     * Setter for the field <code>hostCard</code>.
     * </p>
     * 
     * @param c
     *            a {@link forge.Card} object.
     */
    public final void setHostCard(final Card c) {
        hostCard = c;
    }

    /** The is intrinsic. */
    protected boolean isIntrinsic;

    /**
     * Gets the checks if is intrinsic.
     * 
     * @return the checks if is intrinsic
     */
    public final boolean getIsIntrinsic() {
        return isIntrinsic;
    }

    /**
     * Sets the checks if is intrinsic.
     * 
     * @param b
     *            the new checks if is intrinsic
     */
    public final void setIsIntrinsic(final boolean b) {
        isIntrinsic = b;
    }

    /**
     * <p>
     * Constructor for Trigger.
     * </p>
     * 
     * @param n
     *            a {@link java.lang.String} object.
     * @param params
     *            a {@link java.util.HashMap} object.
     * @param host
     *            a {@link forge.Card} object.
     * @param intrinsic
     *            the intrinsic
     */
    public Trigger(final String n, final HashMap<String, String> params, final Card host, final boolean intrinsic) {
        name = n;
        mapParams = new HashMap<String, String>();
        for (Map.Entry<String, String> entry : params.entrySet()) {
            mapParams.put(entry.getKey(), entry.getValue());
        }
        hostCard = host;

        isIntrinsic = intrinsic;
    }

    /**
     * <p>
     * Constructor for Trigger.
     * </p>
     * 
     * @param params
     *            a {@link java.util.HashMap} object.
     * @param host
     *            a {@link forge.Card} object.
     * @param intrinsic
     *            the intrinsic
     */
    public Trigger(final HashMap<String, String> params, final Card host, final boolean intrinsic) {
        mapParams = new HashMap<String, String>();
        for (Map.Entry<String, String> entry : params.entrySet()) {
            mapParams.put(entry.getKey(), entry.getValue());
        }
        hostCard = host;

        isIntrinsic = intrinsic;
    }

    /**
     * <p>
     * toString.
     * </p>
     * 
     * @return a {@link java.lang.String} object.
     */
    public final String toString() {
        if (mapParams.containsKey("TriggerDescription") && !isSuppressed()) {
            return mapParams.get("TriggerDescription").replace("CARDNAME", hostCard.getName());
        } else {
            return "";
        }
    }

    /**
     * <p>
     * zonesCheck.
     * </p>
     * 
     * @return a boolean.
     */
    public final boolean zonesCheck() {
        if (mapParams.containsKey("TriggerZones")) {
            List<Zone> triggerZones = new ArrayList<Zone>();
            PlayerZone zone = AllZone.getZoneOf(hostCard);
            for (String s : mapParams.get("TriggerZones").split(",")) {
                triggerZones.add(Zone.smartValueOf(s));
            }
            if (zone == null) {
                return false;
            }
            if (!triggerZones.contains(zone.getZoneType()) || hostCard.isPhasedOut()) {
                return false;
            }
        }

        return true;
    }

    /**
     * <p>
     * phasesCheck.
     * </p>
     * 
     * @return a boolean.
     */
    public final boolean phasesCheck() {
        if (mapParams.containsKey("TriggerPhases")) {
            String phases = mapParams.get("TriggerPhases");

            if (phases.contains("->")) {
                // If phases lists a Range, split and Build Activate String
                // Combat_Begin->Combat_End (During Combat)
                // Draw-> (After Upkeep)
                // Upkeep->Combat_Begin (Before Declare Attackers)

                String[] split = phases.split("->", 2);
                phases = AllZone.getPhase().buildActivateString(split[0], split[1]);
            }
            ArrayList<String> triggerPhases = new ArrayList<String>();
            for (String s : phases.split(",")) {
                triggerPhases.add(s);
            }
            if (!triggerPhases.contains(AllZone.getPhase().getPhase())) {
                return false;
            }
        }

        if (mapParams.containsKey("PlayerTurn")) {
            if (!AllZone.getPhase().isPlayerTurn(hostCard.getController())) {
                return false;
            }
        }

        if (mapParams.containsKey("OpponentTurn")) {
            if (AllZone.getPhase().isPlayerTurn(hostCard.getController())) {
                return false;
            }
        }

        return true;
    }

    /**
     * <p>
     * requirementsCheck.
     * </p>
     * 
     * @return a boolean.
     */
    public final boolean requirementsCheck() {
        if (mapParams.containsKey("Metalcraft")) {
            if (mapParams.get("Metalcraft").equals("True") && !hostCard.getController().hasMetalcraft()) {
                return false;
            }
        }

        if (mapParams.containsKey("Threshold")) {
            if (mapParams.get("Threshold").equals("True") && !hostCard.getController().hasThreshold()) {
                return false;
            }
        }

        if (mapParams.containsKey("Hellbent")) {
            if (mapParams.get("Hellbent").equals("True") && !hostCard.getController().hasHellbent()) {
                return false;
            }
        }

        if (mapParams.containsKey("PlayersPoisoned")) {
            if (mapParams.get("PlayersPoisoned").equals("You") && hostCard.getController().getPoisonCounters() == 0) {
                return false;
            } else if (mapParams.get("PlayersPoisoned").equals("Opponent")
                    && hostCard.getController().getOpponent().getPoisonCounters() == 0) {
                return false;
            } else if (mapParams.get("PlayersPoisoned").equals("Each")
                    && !(hostCard.getController().getPoisonCounters() != 0 && hostCard.getController()
                            .getPoisonCounters() != 0)) {
                return false;
            }
        }

        if (mapParams.containsKey("LifeTotal")) {
            String player = mapParams.get("LifeTotal");
            String lifeCompare = "GE1";
            int life = 1;

            if (player.equals("You")) {
                life = hostCard.getController().getLife();
            }
            if (player.equals("Opponent")) {
                life = hostCard.getController().getOpponent().getLife();
            }

            if (mapParams.containsKey("LifeAmount")) {
                lifeCompare = mapParams.get("LifeAmount");
            }

            int right = 1;
            String rightString = lifeCompare.substring(2);
            try {
                right = Integer.parseInt(rightString);
            } catch (NumberFormatException nfe) {
                right = CardFactoryUtil.xCount(hostCard, hostCard.getSVar(rightString));
            }

            if (!AllZoneUtil.compare(life, lifeCompare, right)) {
                return false;
            }

        }

        if (mapParams.containsKey("IsPresent")) {
            String sIsPresent = mapParams.get("IsPresent");
            String presentCompare = "GE1";
            Zone presentZone = Zone.Battlefield;
            String presentPlayer = "Any";
            if (mapParams.containsKey("PresentCompare")) {
                presentCompare = mapParams.get("PresentCompare");
            }
            if (mapParams.containsKey("PresentZone")) {
                presentZone = Zone.smartValueOf(mapParams.get("PresentZone"));
            }
            if (mapParams.containsKey("PresentPlayer")) {
                presentPlayer = mapParams.get("PresentPlayer");
            }
            CardList list = new CardList();
            if (presentPlayer.equals("You") || presentPlayer.equals("Any")) {
                list.addAll(hostCard.getController().getCardsIn(presentZone));
            }
            if (presentPlayer.equals("Opponent") || presentPlayer.equals("Any")) {
                list.addAll(hostCard.getController().getOpponent().getCardsIn(presentZone));
            }

            list = list.getValidCards(sIsPresent.split(","), hostCard.getController(), hostCard);

            int right = 1;
            String rightString = presentCompare.substring(2);
            if (rightString.equals("X")) {
                right = CardFactoryUtil.xCount(hostCard, hostCard.getSVar("X"));
            } else {
                right = Integer.parseInt(presentCompare.substring(2));
            }
            int left = list.size();

            if (!AllZoneUtil.compare(left, presentCompare, right)) {
                return false;
            }

        }

        if (mapParams.containsKey("IsPresent2")) {
            String sIsPresent = mapParams.get("IsPresent2");
            String presentCompare = "GE1";
            Zone presentZone = Zone.Battlefield;
            String presentPlayer = "Any";
            if (mapParams.containsKey("PresentCompare2")) {
                presentCompare = mapParams.get("PresentCompare2");
            }
            if (mapParams.containsKey("PresentZone2")) {
                presentZone = Zone.smartValueOf(mapParams.get("PresentZone2"));
            }
            if (mapParams.containsKey("PresentPlayer2")) {
                presentPlayer = mapParams.get("PresentPlayer2");
            }
            CardList list = new CardList();
            if (presentPlayer.equals("You") || presentPlayer.equals("Any")) {
                list.addAll(hostCard.getController().getCardsIn(presentZone));
            }
            if (presentPlayer.equals("Opponent") || presentPlayer.equals("Any")) {
                list.addAll(hostCard.getController().getOpponent().getCardsIn(presentZone));
            }

            list = list.getValidCards(sIsPresent.split(","), hostCard.getController(), hostCard);

            int right = 1;
            String rightString = presentCompare.substring(2);
            if (rightString.equals("X")) {
                right = CardFactoryUtil.xCount(hostCard, hostCard.getSVar("X"));
            } else {
                right = Integer.parseInt(presentCompare.substring(2));
            }
            int left = list.size();

            if (!AllZoneUtil.compare(left, presentCompare, right)) {
                return false;
            }

        }

        if (mapParams.containsKey("CheckSVar")) {
            int sVar = AbilityFactory.calculateAmount(AllZoneUtil.getCardState(hostCard), mapParams.get("CheckSVar"),
                    null);
            String comparator = "GE1";
            if (mapParams.containsKey("SVarCompare")) {
                comparator = mapParams.get("SVarCompare");
            }
            String svarOperator = comparator.substring(0, 2);
            String svarOperand = comparator.substring(2);
            int operandValue = AbilityFactory.calculateAmount(AllZoneUtil.getCardState(hostCard), svarOperand, null);
            if (!AllZoneUtil.compare(sVar, svarOperator, operandValue)) {
                return false;
            }
        }

        if (mapParams.containsKey("ManaSpent")) {
            if (!hostCard.getColorsPaid().contains(mapParams.get("ManaSpent"))) {
                return false;
            }
        }

        if (mapParams.containsKey("ManaNotSpent")) {
            if (hostCard.getColorsPaid().contains(mapParams.get("ManaNotSpent"))) {
                return false;
            }
        }

        if (mapParams.containsKey("WerewolfTransformCondition")) {
            if (CardUtil.getLastTurnCast("Card", hostCard).size() > 0) {
                return false;
            }
        }

        if (mapParams.containsKey("WerewolfUntransformCondition")) {
            CardList you = CardUtil.getLastTurnCast("Card.YouCtrl", hostCard);
            CardList opp = CardUtil.getLastTurnCast("Card.YouDontCtrl", hostCard);
            if (!(you.size() > 1 || opp.size() > 1)) {
                return false;
            }
        }

        return true;
    }

    /**
     * <p>
     * matchesValid.
     * </p>
     * 
     * @param o
     *            a {@link java.lang.Object} object.
     * @param valids
     *            an array of {@link java.lang.String} objects.
     * @param srcCard
     *            a {@link forge.Card} object.
     * @return a boolean.
     */
    public final boolean matchesValid(final Object o, final String[] valids, final Card srcCard) {
        if (o instanceof Card) {
            Card c = (Card) o;
            return c.isValid(valids, srcCard.getController(), srcCard);
        }

        if (o instanceof Player) {
            for (String v : valids) {
                if (v.equalsIgnoreCase("Player") || v.equalsIgnoreCase("Each")) {
                    return true;
                }
                if (v.equalsIgnoreCase("Opponent")) {
                    if (o.equals(srcCard.getController().getOpponent())) {
                        return true;
                    }
                }
                if (v.equalsIgnoreCase("You")) {
                    return o.equals(srcCard.getController());
                }
                if (v.equalsIgnoreCase("EnchantedController")) {
                    return ((Player) o).isPlayer(srcCard.getEnchantingCard().getController());
                }
                if (v.equalsIgnoreCase("EnchantedPlayer")) {
                    return o.equals(srcCard.getEnchanting());
                }
            }
        }

        return false;
    }

    /**
     * <p>
     * isSecondary.
     * </p>
     * 
     * @return a boolean.
     */
    public final boolean isSecondary() {
        if (mapParams.containsKey("Secondary")) {
            if (mapParams.get("Secondary").equals("True")) {
                return true;
            }
        }
        return false;
    }

    /** {@inheritDoc} */
    @Override
    public final boolean equals(final Object o) {
        if (!(o instanceof Trigger)) {
            return false;
        }

        return this.ID == ((Trigger) o).ID;
    }

    /**
     * <p>
     * performTest.
     * </p>
     * 
     * @param runParams2
     *            a {@link java.util.HashMap} object.
     * @return a boolean.
     */
    public abstract boolean performTest(java.util.Map<String, Object> runParams2);

    /**
     * <p>
     * getCopy.
     * </p>
     * 
     * @return a {@link forge.card.trigger.Trigger} object.
     */
    public abstract Trigger getCopy();

    /**
     * <p>
     * setTriggeringObjects.
     * </p>
     * 
     * @param sa
     *            a {@link forge.card.spellability.SpellAbility} object.
     */
    public abstract void setTriggeringObjects(SpellAbility sa);

    /** The temporary. */
    protected boolean temporary = false;

    /**
     * Sets the temporary.
     * 
     * @param temp
     *            the new temporary
     */
    public final void setTemporary(final boolean temp) {
        temporary = temp;
    }

    /**
     * Checks if is temporary.
     * 
     * @return true, if is temporary
     */
    public final boolean isTemporary() {
        return temporary;
    }

    /** The suppressed. */
    protected boolean suppressed = false;

    /** The temporarily suppressed. */
    protected boolean temporarilySuppressed = false;

    /**
     * Sets the suppressed.
     * 
     * @param supp
     *            the new suppressed
     */
    public final void setSuppressed(final boolean supp) {
        suppressed = supp;
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
}
