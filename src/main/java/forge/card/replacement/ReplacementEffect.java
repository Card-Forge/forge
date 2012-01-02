package forge.card.replacement;

import java.util.HashMap;

import forge.AllZoneUtil;
import forge.Card;
import forge.CardList;
import forge.CardUtil;
import forge.Player;
import forge.Constant.Zone;
import forge.card.abilityfactory.AbilityFactory;
import forge.card.cardfactory.CardFactoryUtil;
import forge.card.spellability.SpellAbility;

/** 
 * TODO: Write javadoc for this type.
 *
 */
public abstract class ReplacementEffect {
    protected boolean hasRun = false;

    /**
     * @return the hasRun
     */
    public final boolean hasRun() {
        return hasRun;
    }

    /**
     * @param hasRun0 the hasRun to set
     */
    public final void setHasRun(boolean hasRun0) {
        this.hasRun = hasRun0;
    }
    
    /** The map params, denoting what to replace. */
    protected HashMap<String, String> mapParams = new HashMap<String, String>();

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
     * @param mapParams0
     *            the mapParams to set
     */
    public final void setMapParams(final HashMap<String, String> mapParams0) {
        this.mapParams = mapParams0;
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
        return this.hostCard;
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
        this.hostCard = c;
    }
    
    public abstract boolean canReplace(final HashMap<String, Object> runParams);
    
    public String toString() {
        if(mapParams.containsKey("Description")) {
            return mapParams.get("Description");
        }
        else {
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
    public final boolean requirementsCheck() {
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
            Zone presentZone = Zone.Battlefield;
            String presentPlayer = "Any";
            if (this.getMapParams().containsKey("PresentCompare")) {
                presentCompare = this.getMapParams().get("PresentCompare");
            }
            if (this.getMapParams().containsKey("PresentZone")) {
                presentZone = Zone.smartValueOf(this.getMapParams().get("PresentZone"));
            }
            if (this.getMapParams().containsKey("PresentPlayer")) {
                presentPlayer = this.getMapParams().get("PresentPlayer");
            }
            CardList list = new CardList();
            if (presentPlayer.equals("You") || presentPlayer.equals("Any")) {
                list.addAll(this.getHostCard().getController().getCardsIn(presentZone));
            }
            if (presentPlayer.equals("Opponent") || presentPlayer.equals("Any")) {
                list.addAll(this.getHostCard().getController().getOpponent().getCardsIn(presentZone));
            }

            list = list.getValidCards(sIsPresent.split(","), this.getHostCard().getController(), this.getHostCard());

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
            Zone presentZone = Zone.Battlefield;
            String presentPlayer = "Any";
            if (this.getMapParams().containsKey("PresentCompare2")) {
                presentCompare = this.getMapParams().get("PresentCompare2");
            }
            if (this.getMapParams().containsKey("PresentZone2")) {
                presentZone = Zone.smartValueOf(this.getMapParams().get("PresentZone2"));
            }
            if (this.getMapParams().containsKey("PresentPlayer2")) {
                presentPlayer = this.getMapParams().get("PresentPlayer2");
            }
            CardList list = new CardList();
            if (presentPlayer.equals("You") || presentPlayer.equals("Any")) {
                list.addAll(this.getHostCard().getController().getCardsIn(presentZone));
            }
            if (presentPlayer.equals("Opponent") || presentPlayer.equals("Any")) {
                list.addAll(this.getHostCard().getController().getOpponent().getCardsIn(presentZone));
            }

            list = list.getValidCards(sIsPresent.split(","), this.getHostCard().getController(), this.getHostCard());

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
            final CardList you = CardUtil.getLastTurnCast("Card.YouCtrl", this.getHostCard());
            final CardList opp = CardUtil.getLastTurnCast("Card.YouDontCtrl", this.getHostCard());
            if (!((you.size() > 1) || (opp.size() > 1))) {
                return false;
            }
        }

        return true;
    }
    
    public final boolean matchesValid(final Object o, final String[] valids, final Card srcCard) {
        if (o instanceof Card) {
            final Card c = (Card) o;
            return c.isValid(valids, srcCard.getController(), srcCard);
        }

        if (o instanceof Player) {
            for (final String v : valids) {
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
    
    public abstract ReplacementEffect getCopy();
    
    public void setReplacingObjects(HashMap<String,Object> runParams, SpellAbility sa) {
        //Should be overriden by replacers that need it.
    }
    
    public ReplacementEffect(final HashMap<String,String> map, final Card host) {
        this.mapParams = map;
        this.hostCard = host;
    }
}
