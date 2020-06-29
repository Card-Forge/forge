package forge.game;

import forge.card.MagicColor;
import forge.card.mana.ManaAtom;
import forge.game.ability.AbilityUtils;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CardLists;
import forge.game.card.CardUtil;
import forge.game.card.CardView;
import forge.game.card.IHasCardView;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;
import forge.util.Expressions;

import java.util.*;

import org.apache.commons.lang3.StringUtils;

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
    protected Map<String, String> originalMapParams = Maps.newHashMap(),
            mapParams = Maps.newHashMap();

    /** The is intrinsic. */
    protected boolean intrinsic;

    /** The suppressed. */
    protected boolean suppressed = false;

    protected Map<String, String> sVars = Maps.newHashMap();

    protected Map<String, String> intrinsicChangedTextColors = Maps.newHashMap();
    protected Map<String, String> intrinsicChangedTextTypes = Maps.newHashMap();
    protected Map<String, String> changedTextColors = Maps.newHashMap();
    protected Map<String, String> changedTextTypes = Maps.newHashMap();

    /** Keys of descriptive (text) parameters. */
    private static final ImmutableList<String> descriptiveKeys = ImmutableList.<String>builder()
            .add("Description", "SpellDescription", "StackDescription", "TriggerDescription").build();
    /** Keys to be followed as SVar names when changing text. */
    private static final ImmutableList<String> mutableKeys = ImmutableList.<String>builder()
            .add("AddAbility").build();

    /**
     * Keys that should not changed
     */
    private static final ImmutableList<String> noChangeKeys = ImmutableList.<String>builder()
            .add("TokenScript", "LegacyImage", "TokenImage", "NewName").build();

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

    public String getParamOrDefault(String key, String defaultValue) {
        String param = mapParams.get(key);
        return param != null ? param : defaultValue;
    }

    public String getParam(String key) {
        return mapParams.get(key);
    }

    public boolean hasParam(String key) {
        return mapParams.containsKey(key);
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
     * isSecondary.
     * </p>
     * 
     * @return a boolean.
     */
    public final boolean isSecondary() {
        return getParamOrDefault("Secondary", "False").equals("True");
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
        if (o instanceof GameObject) {
            final GameObject c = (GameObject) o;
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
     * Checks if is suppressed.
     * 
     * @return true, if is suppressed
     */
    public final boolean isSuppressed() {
        return this.suppressed;
    }

    protected boolean meetsCommonRequirements(Map<String, String> params) {
        final Player hostController = this.getHostCard().getController();
        final Game game = hostController.getGame();
        
        if (params.containsKey("Metalcraft")) {
            if ("True".equalsIgnoreCase(params.get("Metalcraft")) != hostController.hasMetalcraft()) return false;
        }
        if (params.containsKey("Delirium")) {
            if ("True".equalsIgnoreCase(params.get("Delirium")) != hostController.hasDelirium()) return false;
        }
        if (params.containsKey("Threshold")) {
            if ("True".equalsIgnoreCase(params.get("Threshold")) != hostController.hasThreshold()) return false;
        }
        if (params.containsKey("Hellbent")) {
            if ("True".equalsIgnoreCase(params.get("Hellbent")) != hostController.hasHellbent()) return false;
        }
        if (params.containsKey("Bloodthirst")) {
            if ("True".equalsIgnoreCase(params.get("Bloodthirst")) != hostController.hasBloodthirst()) return false;
        }
        if (params.containsKey("FatefulHour")) {
            if ("True".equalsIgnoreCase(params.get("FatefulHour")) != (hostController.getLife() > 5)) return false;
        }
        if (params.containsKey("Revolt")) {
            if ("True".equalsIgnoreCase(params.get("Revolt")) != hostController.hasRevolt()) return false;
        }
        if (params.containsKey("Desert")) {
            if ("True".equalsIgnoreCase(params.get("Desert")) != hostController.hasDesert()) return false;
        }
        if (params.containsKey("Blessing")) {
            if ("True".equalsIgnoreCase(params.get("Blessing")) != hostController.hasBlessing()) return false;
        }
        
        if (params.containsKey("Adamant")) {
            if (hostCard.getCastSA() == null) {
                return false;
            }
            final String payingMana = StringUtils.join(hostCard.getCastSA().getPayingMana());
            final String color = params.get("Adamant");
            if ("Any".equals(color)) {
                boolean bFlag = false;
                for (byte c : MagicColor.WUBRG) {
                    if (StringUtils.countMatches(payingMana, MagicColor.toShortString(c)) >= 3) {
                        bFlag = true;
                        break;
                    }
                }
                if (!bFlag) {
                    return false;
                }
            } else if (StringUtils.countMatches(payingMana, MagicColor.toShortString(color)) < 3) {
                return false;
            }
        }

        if (params.containsKey("Presence")) {
            if (hostCard.getCastFrom() == null || hostCard.getCastSA() == null)
                return false;

            final String type = params.get("Presence");

            int revealed = AbilityUtils.calculateAmount(hostCard, "Revealed$Valid " + type, hostCard.getCastSA());
            int ctrl = AbilityUtils.calculateAmount(hostCard, "Count$LastStateBattlefield " + type + ".YouCtrl", hostCard.getCastSA());

            if (revealed + ctrl == 0) {
                return false;
            }
        }

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

            final String rightString = lifeCompare.substring(2);
            int right = AbilityUtils.calculateAmount(getHostCard(), rightString, this);

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
            if (presentPlayer.equals("Any")) {
                for (final Player p : this.getHostCard().getController().getAllies()) {
                    list.addAll(p.getCardsIn(presentZone));
                }
            }
            list = CardLists.getValidCards(list, sIsPresent.split(","), this.getHostCard().getController(), this.getHostCard(), null);
    

            final String rightString = presentCompare.substring(2);
            int right = AbilityUtils.calculateAmount(getHostCard(), rightString, this);
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
    
            final String rightString = presentCompare.substring(2);
            int right = AbilityUtils.calculateAmount(getHostCard(), rightString, this);
            final int left = list.size();
    
            if (!Expressions.compare(left, presentCompare, right)) {
                return false;
            }
        }

        if (params.containsKey("CheckDefinedPlayer")) {
            SpellAbility mockAbility = this.getHostCard().getFirstSpellAbility();
            mockAbility.setActivatingPlayer(hostController);
            final String sIsPresent = params.get("CheckDefinedPlayer");
            int playersize = AbilityUtils.getDefinedPlayers(game.getCardState(this.getHostCard()), sIsPresent,
                    mockAbility).size();
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
            return !active.getActivateLoyaltyAbilityThisTurn();
        }
        return true;
    }

    public void changeText() {
        // copy changed text words into card trait there
        this.changedTextColors = getHostCard().getChangedTextColorWords();
        this.changedTextTypes = getHostCard().getChangedTextTypeWords();

        for (final String key : this.mapParams.keySet()) {
            final String value = this.originalMapParams.get(key), newValue;
            if (noChangeKeys.contains(key)) {
                continue;
            } else if (descriptiveKeys.contains(key)) {
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

    public String getSvarWithFallback(final String name) {
        String var = sVars.get(name);
        if (var == null) {
            var = hostCard.getSVar(name);
        }
        return var;
    }

    public String getSVar(final String name) {
        String var = sVars.get(name);
        if (var == null) {
            var = "";
        }
        return var;
    }

    public boolean hasSVar(final String name) {
        return sVars.containsKey(name);
    }

    public Integer getSVarInt(final String name) {
        String var = sVars.get(name);
        if (var != null) {
            try {
                return Integer.parseInt(var);
            }
            catch (Exception e) {}
        }
        return null;
    }

    public final void setSVar(final String name, final String value) {
        sVars.put(name, value);
    }

    public Set<String> getSVars() {
        return sVars.keySet();
    }

    public Map<String, String> getChangedTextColors() {
        return _combineChangedMap(intrinsicChangedTextColors, changedTextColors);
    }
    public Map<String, String> getChangedTextTypes() {
        return _combineChangedMap(intrinsicChangedTextTypes, changedTextTypes);
    }

    private Map<String, String> _combineChangedMap(Map<String, String> input, Map<String, String> output) {
        // no need to do something, just return hash
        if (input.isEmpty()) {
            return output;
        }
        if (output.isEmpty()) {
            return input;
        }
        // magic combine them
        Map<String, String> result = Maps.newHashMap(output);
        for (Map.Entry<String, String> e : input.entrySet()) {
            String value = e.getValue();
            result.put(e.getKey(), output.containsKey(value) ? output.get(value) : value);
        }
        return result;
    }

    public void changeTextIntrinsic(Map<String,String> colorMap, Map<String,String> typeMap) {
        intrinsicChangedTextColors = colorMap;
        intrinsicChangedTextTypes = typeMap;
        for (final String key : this.mapParams.keySet()) {
            final String value = this.originalMapParams.get(key), newValue;
            if (noChangeKeys.contains(key)) {
                continue;
            } else if (descriptiveKeys.contains(key)) {
                // change descriptions differently
                newValue = AbilityUtils.applyTextChangeEffects(value, true, colorMap, typeMap);
            }else if (this.getHostCard().hasSVar(value)) {
                // don't change literal SVar names!
                continue;
            } else {
                newValue = AbilityUtils.applyTextChangeEffects(value, false, colorMap, typeMap);
            }

            if (newValue != null) {
                this.mapParams.put(key, newValue);
            }
        }
        // this does overwrite the original MapParams
        this.originalMapParams = Maps.newHashMap(this.mapParams);
    }

    protected void copyHelper(CardTraitBase copy, Card host) {
        copy.originalMapParams = Maps.newHashMap(originalMapParams);
        copy.mapParams = Maps.newHashMap(originalMapParams);
        copy.sVars = Maps.newHashMap(sVars);
        // dont use setHostCard to not trigger the not copied parts yet
        copy.hostCard = host;
    }
}
