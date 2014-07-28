package forge.game;

import forge.card.MagicColor;
import forge.game.ability.AbilityUtils;
import forge.game.card.Card;
import forge.game.card.CardFactoryUtil;
import forge.game.card.CardLists;
import forge.game.card.CardUtil;
import forge.game.player.Player;
import forge.game.zone.ZoneType;
import forge.util.Expressions;

import java.util.*;

/** 
 * Base class for Triggers,ReplacementEffects and StaticAbilities.
 * 
 */
public abstract class CardTraitBase extends GameObject {

    /** The host card. */
    protected Card hostCard;

    /** The map params. */
    protected final Map<String, String> mapParams = new HashMap<String, String>();

    /** The is intrinsic. */
    protected boolean intrinsic;

    /** The temporary. */
    protected boolean temporary = false;

    /** The suppressed. */
    protected boolean suppressed = false;

    /** The temporarily suppressed. */
    protected boolean temporarilySuppressed = false;

    /**
     * Sets the temporary.
     *
     * @param temp
     *            the new temporary
     */
    public final void setTemporary(final boolean temp) {
        this.temporary = temp;
    }

    /**
     * Checks if is temporary.
     *
     * @return true, if is temporary
     */
    public final boolean isTemporary() {
        return this.temporary;
    }

    /**
     * <p>
     * Getter for the field <code>mapParams</code>.
     * </p>
     *
     * @return a {@link java.util.HashMap} object.
     */
    public final Map<String, String> getMapParams() {
        return this.mapParams;
    }

    public final void setMapParams(Map<String,String> params) {
        this.mapParams.clear();
        this.mapParams.putAll(params);
    }

    /**
     * Checks if is intrinsic.
     *
     * @return the isIntrinsic
     */
    public boolean isIntrinsic() {
        return this.intrinsic;
    }

    public void setIntrinsic(boolean i) {
        this.intrinsic = i;
    }

    /**
     * <p>
     * Getter for the field <code>hostCard</code>.
     * </p>
     * 
     * @return a {@link forge.game.card.Card} object.
     */
    public Card getHostCard() {
        return this.hostCard;
    }

    /**
     * <p>
     * Setter for the field <code>hostCard</code>.
     * </p>
     *
     * @param c
     *            a {@link forge.game.card.Card} object.
     */
    public void setHostCard(final Card c) {
        this.hostCard = c;
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
     *            a {@link forge.game.card.Card} object.
     * @return a boolean.
     */
    public static boolean matchesValid(final Object o, final String[] valids, final Card srcCard) {
        if (o instanceof GameEntity) {
            final GameEntity c = (GameEntity) o;
            return c.isValid(valids, srcCard.getController(), srcCard);
        }

        return false;
    }

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
        final Player hostController = this.getHostCard().getController();
        final Game game = hostController.getGame();
        
        if ("True".equalsIgnoreCase(params.get("Metalcraft")) && !hostController.hasMetalcraft()) return false;
        if ("True".equalsIgnoreCase(params.get("Threshold")) && !hostController.hasThreshold()) return false;
        if ("True".equalsIgnoreCase(params.get("Hellbent")) && !hostController.hasHellbent()) return false;
        if ("True".equalsIgnoreCase(params.get("Bloodthirst")) && !hostController.hasBloodthirst()) return false;
        if ("True".equalsIgnoreCase(params.get("FatefulHour")) && hostController.getLife() > 5) return false;

        if (params.containsKey("LifeTotal")) {
            final String player = params.get("LifeTotal");
            String lifeCompare = "GE1";
            int life = 1;

            if (player.equals("You")) {
                life = hostController.getLife();
            }
            if (player.equals("OpponentSmallest")) {
            	life = hostController.getOpponentsSmallestLifeTotal();
            }
            if (player.equals("OpponentGreatest")) {
                life = hostController.getOpponentsGreatestLifeTotal();
            }
            if (player.equals("ActivePlayer")) {
                life = game.getPhaseHandler().getPlayerTurn().getLife();
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
                for (Player p : this.getHostCard().getController().getOpponents())
                    list.addAll(p.getCardsIn(presentZone));
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

        if (params.containsKey("CheckDefinedPlayer")) {
            final String sIsPresent = params.get("CheckDefinedPlayer");
            int playersize = AbilityUtils.getDefinedPlayers(game.getCardState(this.getHostCard()), sIsPresent, 
                    this.getHostCard().getFirstSpellAbility()).size();
            String comparator = "GE1";
            if (params.containsKey("DefinedPlayerCompare")) {
                comparator = params.get("DefinedPlayerCompare");
            }
            final String svarOperator = comparator.substring(0, 2);
            final String svarOperand = comparator.substring(2);
            final int operandValue = AbilityUtils.calculateAmount(game.getCardState(this.getHostCard()), svarOperand, null);
            if (!Expressions.compare(playersize, svarOperator, operandValue)) {
                return false;
            }
        }
    
        if (params.containsKey("CheckSVar")) {
            final int sVar = AbilityUtils.calculateAmount(game.getCardState(this.getHostCard()), params.get("CheckSVar"), null);
            String comparator = "GE1";
            if (params.containsKey("SVarCompare")) {
                comparator = params.get("SVarCompare");
            }
            final String svarOperator = comparator.substring(0, 2);
            final String svarOperand = comparator.substring(2);
            final int operandValue = AbilityUtils.calculateAmount(game.getCardState(this.getHostCard()), svarOperand, null);
            if (!Expressions.compare(sVar, svarOperator, operandValue)) {
                return false;
            }
        }
    
        if (params.containsKey("ManaSpent")) {
            byte spent = MagicColor.fromName(params.get("ManaSpent"));
            if ( 0 == (this.getHostCard().getColorsPaid() & spent)) {
                return false;
            }
        }

        if (params.containsKey("ManaNotSpent")) {
            byte spent = MagicColor.fromName(params.get("ManaNotSpent"));
            if ( 0 != (this.getHostCard().getColorsPaid() & spent)) {
                return false;
            }
        }

        if (params.containsKey("WerewolfTransformCondition")) {
            if (!CardUtil.getLastTurnCast("Card", this.getHostCard()).isEmpty()) {
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

        if (params.containsKey("ActivateNoLoyaltyAbilitiesCondition")) {
            final Player active = game.getPhaseHandler().getPlayerTurn();
            if (active.getActivateLoyaltyAbilityThisTurn()) {
                return false;
            }
        }
        return true;
    }
}
