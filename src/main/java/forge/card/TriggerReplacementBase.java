package forge.card;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;

import forge.Card;
import forge.CardLists;
import forge.CardUtil;
import forge.GameEntity;
import forge.Singletons;
import forge.card.ability.AbilityUtils;
import forge.card.cardfactory.CardFactoryUtil;
import forge.card.spellability.SpellAbility;
import forge.game.player.Player;
import forge.game.zone.Zone;
import forge.game.zone.ZoneType;
import forge.util.Expressions;

/** 
 * Base class for Triggers and ReplacementEffects.
 * Provides the matchesValid function to both classes.
 * 
 */
public abstract class TriggerReplacementBase {

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

        if (overridingAbility != null) {
            CardFactoryUtil.correctAbilityChainSourceCard(overridingAbility, c);
        }
    }

    protected EnumSet<ZoneType> validHostZones;

    public void setActiveZone(EnumSet<ZoneType> zones) {
        validHostZones = zones;
    }

    /**
     * <p>
     * zonesCheck.
     * </p>
     * 
     * @return a boolean.
     */
    public final boolean zonesCheck(Zone hostCardZone) {
        return !this.hostCard.isPhasedOut()
                && (validHostZones == null || validHostZones.isEmpty()
                || (hostCardZone != null && validHostZones.contains(hostCardZone.getZoneType()))
              );
    }

    /** The overriding ability. */
    private SpellAbility overridingAbility = null;

    /**
     * Gets the overriding ability.
     * 
     * @return the overridingAbility
     */
    public SpellAbility getOverridingAbility() {
        return this.overridingAbility;
    }

    /**
     * Sets the overriding ability.
     * 
     * @param overridingAbility0
     *            the overridingAbility to set
     */
    public void setOverridingAbility(final SpellAbility overridingAbility0) {
        this.overridingAbility = overridingAbility0;
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
    public static boolean matchesValid(final Object o, final String[] valids, final Card srcCard) {
        if (o instanceof GameEntity) {
            final GameEntity c = (GameEntity) o;
            return c.isValid(valids, srcCard.getController(), srcCard);
        }

        return false;
    }

    /** The suppressed. */
    private boolean suppressed = false;

    /** The temporarily suppressed. */
    private boolean temporarilySuppressed = false;

    /**
     * Sets the suppressed.
     * 
     * @param supp
     *            the new suppressed
     */
    public final void setSuppressed(final boolean supp) {
        this.suppressed = supp;
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

    protected boolean meetsCommonRequirements(Map<String, String> params) {
        
        Player hostController = this.getHostCard().getController();
        
        if ("True".equalsIgnoreCase(params.get("Metalcraft")) && !hostController.hasMetalcraft()) return false;
        if ("True".equalsIgnoreCase(params.get("Threshold")) && !hostController.hasThreshold()) return false;
        if ("True".equalsIgnoreCase(params.get("Hellbent")) && !hostController.hasHellbent()) return false;
        if ("True".equalsIgnoreCase(params.get("Bloodthirst")) && !hostController.hasBloodthirst()) return false;
        if ("True".equalsIgnoreCase(params.get("FatefulHour")) && hostController.getLife() > 5) return false;

        if ("You".equalsIgnoreCase(params.get("PlayersPoisoned")) && hostController.getPoisonCounters() == 0) return false;
        if ("Opponent".equalsIgnoreCase(params.get("PlayersPoisoned")) && hostController.getOpponent().getPoisonCounters() == 0) return false;
        if ("Each".equalsIgnoreCase(params.get("PlayersPoisoned"))) {
            for( Player p : Singletons.getModel().getGame().getPlayers())
                if( p.getPoisonCounters() == 0 ) 
                    return false;
        }

        if (params.containsKey("LifeTotal")) {
            final String player = params.get("LifeTotal");
            String lifeCompare = "GE1";
            int life = 1;

            if (player.equals("You")) {
                life = hostController.getLife();
            }
            if (player.equals("Opponent")) {
                life = hostController.getOpponent().getLife();
            }

            if (params.containsKey("LifeAmount")) {
                lifeCompare = params.get("LifeAmount");
            }

            int right = 1;
            final String rightString = lifeCompare.substring(2);
            try {
                right = Integer.parseInt(rightString);
            } catch (final NumberFormatException nfe) {
                right = CardFactoryUtil.xCount(this.getHostCard(), this.getHostCard().getSVar(rightString));
            }

            if (!Expressions.compare(life, lifeCompare, right)) {
                return false;
            }
        }


        if (params.containsKey("IsPresent")) {
            final String sIsPresent = params.get("IsPresent");
            String presentCompare = "GE1";
            ZoneType presentZone = ZoneType.Battlefield;
            String presentPlayer = "Any";
            if (params.containsKey("PresentCompare")) {
                presentCompare = params.get("PresentCompare");
            }
            if (params.containsKey("PresentZone")) {
                presentZone = ZoneType.smartValueOf(params.get("PresentZone"));
            }
            if (params.containsKey("PresentPlayer")) {
                presentPlayer = params.get("PresentPlayer");
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
    
            if (!Expressions.compare(left, presentCompare, right)) {
                return false;
            }
    
        }
    
        if (params.containsKey("IsPresent2")) {
            final String sIsPresent = params.get("IsPresent2");
            String presentCompare = "GE1";
            ZoneType presentZone = ZoneType.Battlefield;
            String presentPlayer = "Any";
            if (params.containsKey("PresentCompare2")) {
                presentCompare = params.get("PresentCompare2");
            }
            if (params.containsKey("PresentZone2")) {
                presentZone = ZoneType.smartValueOf(params.get("PresentZone2"));
            }
            if (params.containsKey("PresentPlayer2")) {
                presentPlayer = params.get("PresentPlayer2");
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
    
            if (!Expressions.compare(left, presentCompare, right)) {
                return false;
            }
    
        }
    
        if (params.containsKey("CheckSVar")) {
            final int sVar = AbilityUtils.calculateAmount(Singletons.getModel().getGame().getCardState(this.getHostCard()), params.get("CheckSVar"), null);
            String comparator = "GE1";
            if (params.containsKey("SVarCompare")) {
                comparator = params.get("SVarCompare");
            }
            final String svarOperator = comparator.substring(0, 2);
            final String svarOperand = comparator.substring(2);
            final int operandValue = AbilityUtils.calculateAmount(Singletons.getModel().getGame().getCardState(this.getHostCard()),
                    svarOperand, null);
            if (!Expressions.compare(sVar, svarOperator, operandValue)) {
                return false;
            }
        }
    
        if (params.containsKey("ManaSpent")) {
            if (!this.getHostCard().getColorsPaid().contains(params.get("ManaSpent"))) {
                return false;
            }
        }
    
        if (params.containsKey("ManaNotSpent")) {
            if (this.getHostCard().getColorsPaid().contains(params.get("ManaNotSpent"))) {
                return false;
            }
        }
    
        if (params.containsKey("WerewolfTransformCondition")) {
            if (CardUtil.getLastTurnCast("Card", this.getHostCard()).size() > 0) {
                return false;
            }
        }
    
        if (params.containsKey("WerewolfUntransformCondition")) {
            final List<Card> you = CardUtil.getLastTurnCast("Card.YouCtrl", this.getHostCard());
            final List<Card> opp = CardUtil.getLastTurnCast("Card.YouDontCtrl", this.getHostCard());
            if (!((you.size() > 1) || (opp.size() > 1))) {
                return false;
            }
        }
        
        return true;
    }
}
