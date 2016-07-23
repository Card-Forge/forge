package forge.game;

import forge.card.mana.ManaAtom;
import forge.game.ability.AbilityUtils;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CardFactoryUtil;
import forge.game.card.CardLists;
import forge.game.card.CardUtil;
import forge.game.card.CardView;
import forge.game.card.IHasCardView;
import forge.game.player.Player;
import forge.game.zone.ZoneType;
import forge.util.Expressions;

import java.util.*;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;

/** 
 * Base class for Triggers,ReplacementEffects and StaticAbilities.
 * 
 */
public abstract class CardTraitBase extends GameObject implements IHasCardView {

    /** The host card. */
    protected Card hostCard;

    /** The map params. */
    protected final Map<String, String> originalMapParams = Maps.newHashMap(),
            mapParams = Maps.newHashMap();

    /** The is intrinsic. */
    protected boolean intrinsic;

    /** The temporary. */
    protected boolean temporary = false;

    /** The suppressed. */
    protected boolean suppressed = false;

    /** The temporarily suppressed. */
    protected boolean temporarilySuppressed = false;

    /** Keys of descriptive (text) parameters. */
    private static final ImmutableList<String> descriptiveKeys = ImmutableList.<String>builder()
            .add("Description", "SpellDescription", "StackDescription", "TriggerDescription").build();
    /** Keys to be followed as SVar names when changing text. */
    private static final ImmutableList<String> mutableKeys = ImmutableList.<String>builder()
            .add("AddAbility").build();

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
    
    /**
     * <p>
     * Getter for the field <code>mapParams</code>.
     * </p>
     *
     * @return a {@link java.util.HashMap} object.
     */
    public final Map<String, String> getOriginalMapParams() {
        return this.originalMapParams;
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
            return c.isValid(valids, srcCard.getController(), srcCard, null);
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

    protected final boolean isNonTempSuppressed() {
        return this.suppressed;
    }

    protected boolean meetsCommonRequirements(Map<String, String> params) {
        final Player hostController = this.getHostCard().getController();
        final Game game = hostController.getGame();
        
        if ("True".equalsIgnoreCase(params.get("Metalcraft")) && !hostController.hasMetalcraft()) return false;
        if ("True".equalsIgnoreCase(params.get("Delirium")) && !hostController.hasDelirium()) return false;
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
            CardCollection list = new CardCollection();
            if (presentPlayer.equals("You") || presentPlayer.equals("Any")) {
                list.addAll(this.getHostCard().getController().getCardsIn(presentZone));
            }
            if (presentPlayer.equals("Opponent") || presentPlayer.equals("Any")) {
                for (final Player p : this.getHostCard().getController().getOpponents()) {
                    list.addAll(p.getCardsIn(presentZone));
                }
            }
    
            list = CardLists.getValidCards(list, sIsPresent.split(","), this.getHostCard().getController(), this.getHostCard(), null);
    
            int right = 1;
            final String rightString = presentCompare.substring(2);
            try {
                right = Integer.parseInt(rightString);
            } catch (final NumberFormatException nfe) {
                right = CardFactoryUtil.xCount(this.getHostCard(), this.getHostCard().getSVar(rightString));
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
            CardCollection list = new CardCollection();
            if (presentPlayer.equals("You") || presentPlayer.equals("Any")) {
                list.addAll(this.getHostCard().getController().getCardsIn(presentZone));
            }
            if (presentPlayer.equals("Opponent") || presentPlayer.equals("Any")) {
                for (final Player p : this.getHostCard().getController().getOpponents()) {
                    list.addAll(p.getCardsIn(presentZone));
                }
            }
    
            list = CardLists.getValidCards(list, sIsPresent.split(","), this.getHostCard().getController(), this.getHostCard(), null);
    
            int right = 1;
            final String rightString = presentCompare.substring(2);
            try {
                right = Integer.parseInt(rightString);
            } catch (final NumberFormatException nfe) {
                right = CardFactoryUtil.xCount(this.getHostCard(), this.getHostCard().getSVar(rightString));
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
            final int operandValue = AbilityUtils.calculateAmount(game.getCardState(this.getHostCard()), svarOperand, this);
            if (!Expressions.compare(playersize, svarOperator, operandValue)) {
                return false;
            }
        }
    
        if (params.containsKey("CheckSVar")) {
            final int sVar = AbilityUtils.calculateAmount(game.getCardState(this.getHostCard()), params.get("CheckSVar"), this);
            String comparator = "GE1";
            if (params.containsKey("SVarCompare")) {
                comparator = params.get("SVarCompare");
            }
            final String svarOperator = comparator.substring(0, 2);
            final String svarOperand = comparator.substring(2);
            final int operandValue = AbilityUtils.calculateAmount(game.getCardState(this.getHostCard()), svarOperand, this);
            if (!Expressions.compare(sVar, svarOperator, operandValue)) {
                return false;
            }
        }
    
        if (params.containsKey("ManaSpent")) {
            byte spent = ManaAtom.fromName(params.get("ManaSpent"));
            if ( 0 == (this.getHostCard().getColorsPaid() & spent)) {
                return false;
            }
        }

        if (params.containsKey("ManaNotSpent")) {
            byte spent = ManaAtom.fromName(params.get("ManaNotSpent"));
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
            List<Card> casted = game.getStack().getSpellsCastLastTurn();
            boolean conditionMet = false;
            for (Player p : game.getPlayers()) {
                conditionMet |= CardLists.filterControlledBy(casted, p).size() > 1;
            }
            if (!conditionMet) {
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

    public void changeText() {
        for (final String key : this.mapParams.keySet()) {
            final String value = this.originalMapParams.get(key), newValue;
            if (descriptiveKeys.contains(key)) {
                // change descriptions differently
                newValue = AbilityUtils.applyDescriptionTextChangeEffects(value, this);
            } else if (mutableKeys.contains(key)) {
                // follow SVar and change it
                final String originalSVarValue = hostCard.getSVar(value);
                hostCard.changeSVar(value, AbilityUtils.applyAbilityTextChangeEffects(originalSVarValue, this));
                newValue = null;
            } else if (this.getHostCard().hasSVar(value)) {
                // don't change literal SVar names!
                newValue = null;
            } else {
                newValue = AbilityUtils.applyAbilityTextChangeEffects(value, this);
            }

            if (newValue != null) {
                this.mapParams.put(key, newValue);
            }
        }
    }

    @Override
    public CardView getCardView() {
        return CardView.get(hostCard);
    }
}
