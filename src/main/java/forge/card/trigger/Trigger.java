package forge.card.trigger;

import forge.Card;
import forge.AllZone;
import forge.AllZoneUtil;
import forge.CardList;
import forge.Player;
import forge.card.abilityFactory.AbilityFactory;
import forge.card.cardFactory.CardFactoryUtil;
import forge.card.spellability.SpellAbility;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * <p>Abstract Trigger class.</p>
 *
 * @author Forge
 * @version $Id$
 */
public abstract class Trigger {

    /** Constant <code>nextID=0</code> */
    private static int nextID = 0;

    /**
     * <p>resetIDs.</p>
     */
    public static void resetIDs() {
        nextID = 50000;
    }

    protected int ID = nextID++;

    protected String name;

    /**
     * <p>Getter for the field <code>name</code>.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String getName() {
        return name;
    }

    /**
     * <p>Setter for the field <code>name</code>.</p>
     *
     * @param n a {@link java.lang.String} object.
     */
    public void setName(String n) {
        name = n;
    }

    /**
     * <p>setID.</p>
     *
     * @param id a int.
     */
    public void setID(int id) {
        ID = id;
    }

    protected HashMap<String, String> mapParams = new HashMap<String, String>();

    /**
     * <p>Getter for the field <code>mapParams</code>.</p>
     *
     * @return a {@link java.util.HashMap} object.
     */
    public HashMap<String, String> getMapParams() {
        return mapParams;
    }

    protected Map<String, Object> runParams;

    /**
     * <p>Setter for the field <code>runParams</code>.</p>
     *
     * @param runParams2 a {@link java.util.Map} object.
     */
    public void setRunParams(Map<String, Object> runParams2) {
        runParams = runParams2;
    }

    /**
     * <p>Getter for the field <code>runParams</code>.</p>
     *
     * @return a {@link java.util.Map} object.
     */
    public Map<String, Object> getRunParams() {
        return runParams;
    }

    protected SpellAbility overridingAbility = null;

    /**
     * <p>Getter for the field <code>overridingAbility</code>.</p>
     *
     * @return a {@link forge.card.spellability.SpellAbility} object.
     */
    public SpellAbility getOverridingAbility() {
        return overridingAbility;
    }

    /**
     * <p>Setter for the field <code>overridingAbility</code>.</p>
     *
     * @param sa a {@link forge.card.spellability.SpellAbility} object.
     */
    public void setOverridingAbility(SpellAbility sa) {
        overridingAbility = sa;
    }

    private HashMap<String, Object> storedTriggeredObjects = null;

    /**
     * <p>Setter for the field <code>storedTriggeredObjects</code>.</p>
     *
     * @param storedTriggeredObjects a {@link java.util.HashMap} object.
     * @since 1.0.15
     */
    public void setStoredTriggeredObjects(HashMap<String, Object> storedTriggeredObjects) {
        this.storedTriggeredObjects = storedTriggeredObjects;
    }

    /**
     * <p>Getter for the field <code>storedTriggeredObjects</code>.</p>
     *
     * @return a {@link java.util.HashMap} object.
     * @since 1.0.15
     */
    public HashMap<String, Object> getStoredTriggeredObjects() {
        return storedTriggeredObjects;
    }

    protected Card hostCard;

    /**
     * <p>Getter for the field <code>hostCard</code>.</p>
     *
     * @return a {@link forge.Card} object.
     */
    public Card getHostCard() {
        return hostCard;
    }

    /**
     * <p>Setter for the field <code>hostCard</code>.</p>
     *
     * @param c a {@link forge.Card} object.
     */
    public void setHostCard(Card c) {
        hostCard = c;
    }

    protected boolean isIntrinsic;

    public boolean getIsIntrinsic()
    {
        return isIntrinsic;
    }

    public void setIsIntrinsic(boolean b)
    {
        isIntrinsic = b;
    }

    /**
     * <p>Constructor for Trigger.</p>
     *
     * @param n a {@link java.lang.String} object.
     * @param params a {@link java.util.HashMap} object.
     * @param host a {@link forge.Card} object.
     */
    public Trigger(String n, HashMap<String, String> params, Card host, boolean intrinsic) {
        name = n;
        mapParams = new HashMap<String, String>();
        for (Map.Entry<String, String> entry : params.entrySet()) {
            mapParams.put(entry.getKey(), entry.getValue());
        }
        hostCard = host;

        isIntrinsic = intrinsic;
    }

    /**
     * <p>Constructor for Trigger.</p>
     *
     * @param params a {@link java.util.HashMap} object.
     * @param host a {@link forge.Card} object.
     */
    public Trigger(HashMap<String, String> params, Card host, boolean intrinsic) {
        mapParams = new HashMap<String, String>();
        for (Map.Entry<String, String> entry : params.entrySet()) {
            mapParams.put(entry.getKey(), entry.getValue());
        }
        hostCard = host;

        isIntrinsic = intrinsic;
    }

    /**
     * <p>toString.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String toString() {
        if (mapParams.containsKey("TriggerDescription") && !isSuppressed()) {
            return mapParams.get("TriggerDescription").replace("CARDNAME", hostCard.getName());
        } else return "";
    }

    /**
     * <p>zonesCheck.</p>
     *
     * @return a boolean.
     */
    public boolean zonesCheck() {
        if (mapParams.containsKey("TriggerZones")) {
            ArrayList<String> triggerZones = new ArrayList<String>();
            for (String s : mapParams.get("TriggerZones").split(",")) {
                triggerZones.add(s);
            }
            if (AllZone.getZone(hostCard) == null) {
                return false;
            }
            if (!triggerZones.contains(AllZone.getZone(hostCard).getZoneName())) {
                return false;
            }
        }

        return true;
    }

    /**
     * <p>phasesCheck.</p>
     *
     * @return a boolean.
     */
    public boolean phasesCheck() {
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

        if (mapParams.containsKey("PlayerTurn"))
            if (!AllZone.getPhase().isPlayerTurn(hostCard.getController()))
                return false;

        if (mapParams.containsKey("OpponentTurn"))
            if (AllZone.getPhase().isPlayerTurn(hostCard.getController()))
                return false;

        return true;
    }

    /**
     * <p>requirementsCheck.</p>
     *
     * @return a boolean.
     */
    public boolean requirementsCheck() {
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
            } else if (mapParams.get("PlayersPoisoned").equals("Opponent") && hostCard.getController().getOpponent().getPoisonCounters() == 0) {
                return false;
            } else if (mapParams.get("PlayersPoisoned").equals("Each") && !(hostCard.getController().getPoisonCounters() != 0 && hostCard.getController().getPoisonCounters() != 0)) {
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
            if (rightString.equals("X")) {
                right = CardFactoryUtil.xCount(hostCard, hostCard.getSVar("X"));
            } else {
                right = Integer.parseInt(lifeCompare.substring(2));
            }

            if (!AllZoneUtil.compare(life, lifeCompare, right)) {
                return false;
            }

        }

        if (mapParams.containsKey("IsPresent")) {
            String sIsPresent = mapParams.get("IsPresent");
            String presentCompare = "GE1";
            String presentZone = "Battlefield";
            String presentPlayer = "Any";
            if (mapParams.containsKey("PresentCompare")) {
                presentCompare = mapParams.get("PresentCompare");
            }
            if (mapParams.containsKey("PresentZone")) {
                presentZone = mapParams.get("PresentZone");
            }
            if (mapParams.containsKey("PresentPlayer")) {
                presentPlayer = mapParams.get("PresentPlayer");
            }
            CardList list = new CardList();
            if (presentPlayer.equals("You") || presentPlayer.equals("Any")) {
                list.addAll(AllZoneUtil.getCardsInZone(presentZone, hostCard.getController()));
            }
            if (presentPlayer.equals("Opponent") || presentPlayer.equals("Any")) {
                list.addAll(AllZoneUtil.getCardsInZone(presentZone, hostCard.getController().getOpponent()));
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
            String presentZone = "Battlefield";
            String presentPlayer = "Any";
            if (mapParams.containsKey("PresentCompare2")) {
                presentCompare = mapParams.get("PresentCompare2");
            }
            if (mapParams.containsKey("PresentZone2")) {
                presentZone = mapParams.get("PresentZone2");
            }
            if (mapParams.containsKey("PresentPlayer2")) {
                presentPlayer = mapParams.get("PresentPlayer2");
            }
            CardList list = new CardList();
            if (presentPlayer.equals("You") || presentPlayer.equals("Any")) {
                list.addAll(AllZoneUtil.getCardsInZone(presentZone, hostCard.getController()));
            }
            if (presentPlayer.equals("Opponent") || presentPlayer.equals("Any")) {
                list.addAll(AllZoneUtil.getCardsInZone(presentZone, hostCard.getController().getOpponent()));
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
            int sVar = AbilityFactory.calculateAmount(AllZoneUtil.getCardState(hostCard), mapParams.get("CheckSVar"), null);
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


        return true;
    }


    /**
     * <p>matchesValid.</p>
     *
     * @param o a {@link java.lang.Object} object.
     * @param valids an array of {@link java.lang.String} objects.
     * @param srcCard a {@link forge.Card} object.
     * @return a boolean.
     */
    public boolean matchesValid(Object o, String[] valids, Card srcCard) {
        if (o instanceof Card) {
            Card c = (Card) o;
            return c.isValidCard(valids, srcCard.getController(), srcCard);
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
            }
        }

        return false;
    }

    /**
     * <p>isSecondary.</p>
     *
     * @return a boolean.
     */
    public boolean isSecondary() {
        if (mapParams.containsKey("Secondary")) {
            if (mapParams.get("Secondary").equals("True"))
                return true;
        }
        return false;
    }

    /** {@inheritDoc} */
    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Trigger))
            return false;

        return this.ID == ((Trigger) o).ID;
    }

    /**
     * <p>performTest.</p>
     *
     * @param runParams2 a {@link java.util.HashMap} object.
     * @return a boolean.
     */
    public abstract boolean performTest(java.util.Map<String, Object> runParams2);

    /**
     * <p>getCopy.</p>
     *
     * @return a {@link forge.card.trigger.Trigger} object.
     */
    public abstract Trigger getCopy();

    /**
     * <p>setTriggeringObjects.</p>
     *
     * @param sa a {@link forge.card.spellability.SpellAbility} object.
     */
    public abstract void setTriggeringObjects(SpellAbility sa);
    
    protected boolean temporary = false;
    
    public void setTemporary(boolean temp) {
    	temporary = temp;
    }
    
    public boolean isTemporary() {
    	return temporary;
    }
    
    protected boolean suppressed = false;
    protected boolean temporarilySuppressed = false;
    
    /*public void setSuppressed(boolean supp) {
        suppressed = supp;
    }*/
    
    public void setTemporarilySuppressed(boolean supp) {
        temporarilySuppressed = supp;
    }
    
    public boolean isSuppressed() {
        return (suppressed || temporarilySuppressed);
    }
}
