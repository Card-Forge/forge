package forge.game;

import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;

import forge.card.CardStateName;
import forge.card.MagicColor;
import forge.card.mana.ManaAtom;
import forge.game.ability.AbilityUtils;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CardLists;
import forge.game.card.CardPredicates;
import forge.game.card.CardState;
import forge.game.card.CardView;
import forge.game.card.IHasCardView;
import forge.game.keyword.KeywordInterface;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.trigger.Trigger;
import forge.game.zone.ZoneType;
import forge.util.Expressions;

/**
 * Base class for Triggers,ReplacementEffects and StaticAbilities.
 *
 */
public abstract class CardTraitBase extends GameObject implements IHasCardView, IHasSVars {

    /** The host card. */
    protected Card hostCard;
    protected CardState cardState = null;
    protected KeywordInterface keyword = null;

    /** The map params. */
    protected Map<String, String> originalMapParams = Maps.newHashMap(),
            mapParams = Maps.newHashMap();

    /** The is intrinsic. */
    protected boolean intrinsic;

    /** The suppressed. */
    protected boolean suppressed = false;

    protected Map<String, String> sVars = Maps.newTreeMap();

    protected Map<String, String> intrinsicChangedTextColors = Maps.newHashMap();
    protected Map<String, String> intrinsicChangedTextTypes = Maps.newHashMap();
    protected Map<String, String> changedTextColors = Maps.newHashMap();
    protected Map<String, String> changedTextTypes = Maps.newHashMap();

    /** Keys of descriptive (text) parameters. */
    private static final ImmutableList<String> descriptiveKeys = ImmutableList.<String>builder()
            .add("Description", "SpellDescription", "StackDescription", "TriggerDescription").build();

    /**
     * Keys that should not changed
     */
    private static final ImmutableList<String> noChangeKeys = ImmutableList.<String>builder()
            .add("TokenScript", "LegacyImage", "TokenImage", "NewName", "ChooseFromList")
            .add("AddAbility").build();

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
    public String putParam(String key, String value) {
        return mapParams.put(key, value);
    }
    public void removeParam(String key) {
        mapParams.remove(key);
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

    public KeywordInterface getKeyword() {
        return this.keyword;
    }
    public void setKeyword(final KeywordInterface kw) {
        this.keyword = kw;
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

    public final boolean isClassAbility() {
        return hasParam("ClassLevel");
    }
    public final boolean isClassLevelNAbility(int level) {
        String classLevel = getParamOrDefault("ClassLevel", "0");
        if (!StringUtils.isNumeric(classLevel)) {
            classLevel = classLevel.substring(2);
        }
        return level == Integer.parseInt(classLevel);
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
    public boolean matchesValid(final Object o, final String[] valids, final Card srcCard) {
        if (srcCard == null) {
            return false;
        }

        Player controller = srcCard.getController();
        if (this instanceof Trigger) {
            // check for delayed trigger
            if (((Trigger) this).getSpawningAbility() != null) {
                controller = ((Trigger) this).getSpawningAbility().getActivatingPlayer();
            }
        }
        return matchesValid(o, valids, srcCard, controller);
    }

    public boolean matchesValid(final Object o, final String[] valids, final Card srcCard, final Player srcPlayer) {
        if (o instanceof GameObject) {
            final GameObject c = (GameObject) o;
            return c.isValid(valids, srcPlayer, srcCard, this);
        } else if (o instanceof Iterable<?>) {
            for (Object o2 : (Iterable<?>)o) {
                if (matchesValid(o2, valids, srcCard, srcPlayer)) {
                    return true;
                }
            }
        } else if (o instanceof String) {
            if (ArrayUtils.contains(valids, o)) {
                return true;
            }
        } else if (o instanceof PlanarDice) {
            for (String s : valids) {
                PlanarDice valid = PlanarDice.smartValueOf(s);
                if (((PlanarDice) o).name().equals(valid.name())) {
                    return true;
                }
            }
        }

        return false;
    }

    public boolean matchesValid(final Object o, final String[] valids) {
        return matchesValid(o, valids, getHostCard());
    }

    public boolean matchesValidParam(String param, final Object o, final Card srcCard) {
        if (hasParam(param) && !matchesValid(o, getParam(param).split(","), srcCard)) {
            return false;
        }
        return true;
    }

    public boolean matchesValidParam(String param, final Object o) {
        return matchesValidParam(param, o, getHostCard());
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
        Player hostController = this.getHostCard().getController();
        final Game game = hostController.getGame();

        // intervening if check, make sure to use right controller
        if (!game.getStack().isEmpty() && game.getStack().isResolving(getHostCard())) {
            SpellAbility sa = game.getStack().peek().getSpellAbility();
            if (sa.isTrigger()) {
                hostController = sa.getActivatingPlayer();
            }
        }

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
            else if ("None".equalsIgnoreCase(params.get("Revolt"))) {
                boolean none = true;
                for (Player p : game.getRegisteredPlayers()) {
                    if (p.hasRevolt()) {
                        none = false;
                        break;
                    }
                }
                if (!none) {
                    return false;
                }
            }
        }
        if (params.containsKey("Desert")) {
            if ("True".equalsIgnoreCase(params.get("Desert")) != hostController.hasDesert()) return false;
        }
        if (params.containsKey("Blessing")) {
            if ("True".equalsIgnoreCase(params.get("Blessing")) != hostController.hasBlessing()) return false;
        }

        if (params.containsKey("DayTime")) {
            if ("Day".equalsIgnoreCase(params.get("DayTime"))) {
                if (!game.isDay()) {
                    return false;
                }
            } else if ("Night".equalsIgnoreCase(params.get("DayTime"))) {
                if (!game.isNight()) {
                    return false;
                }
            } else if ("Neither".equalsIgnoreCase(params.get("DayTime"))) {
                if (!game.isNeitherDayNorNight()) {
                    return false;
                }
            }
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
            final String lifeCompare = getParamOrDefault("LifeAmount", "GE1");
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

            final String rightString = lifeCompare.substring(2);
            int right = AbilityUtils.calculateAmount(getHostCard(), rightString, this);

            if (!Expressions.compare(life, lifeCompare, right)) {
                return false;
            }
        }

        if (params.containsKey("IsPresent")) {
            final String sIsPresent = params.get("IsPresent");
            final String presentCompare = getParamOrDefault("PresentCompare", "GE1");
            final String presentPlayer = getParamOrDefault("PresentPlayer", "Any");
            ZoneType presentZone = ZoneType.Battlefield;
            if (params.containsKey("PresentZone")) {
                presentZone = ZoneType.smartValueOf(params.get("PresentZone"));
            }
            CardCollection list = new CardCollection();
            if (presentPlayer.equals("You") || presentPlayer.equals("Any")) {
                list.addAll(hostController.getCardsIn(presentZone));
            }
            if (presentPlayer.equals("Opponent") || presentPlayer.equals("Any")) {
                list.addAll(hostController.getOpponents().getCardsIn(presentZone));
            }
            if (presentPlayer.equals("Any")) {
                list.addAll(hostController.getAllies().getCardsIn(presentZone));
            }
            list = CardLists.getValidCards(list, sIsPresent, hostController, this.getHostCard(), this);

            final String rightString = presentCompare.substring(2);
            int right = AbilityUtils.calculateAmount(getHostCard(), rightString, this);
            final int left = list.size();

            if (!Expressions.compare(left, presentCompare, right)) {
                return false;
            }
        }

        if (params.containsKey("IsPresent2")) {
            final String sIsPresent = params.get("IsPresent2");
            final String presentCompare = getParamOrDefault("PresentCompare2", "GE1");
            final String presentPlayer = getParamOrDefault("PresentPlayer2", "Any");
            ZoneType presentZone = ZoneType.Battlefield;
            if (params.containsKey("PresentZone2")) {
                presentZone = ZoneType.smartValueOf(params.get("PresentZone2"));
            }
            CardCollection list = new CardCollection();
            if (presentPlayer.equals("You") || presentPlayer.equals("Any")) {
                list.addAll(hostController.getCardsIn(presentZone));
            }
            if (presentPlayer.equals("Opponent") || presentPlayer.equals("Any")) {
                list.addAll(hostController.getOpponents().getCardsIn(presentZone));
            }

            list = CardLists.getValidCards(list, sIsPresent, hostController, this.getHostCard(), this);

            final String rightString = presentCompare.substring(2);
            int right = AbilityUtils.calculateAmount(getHostCard(), rightString, this);
            final int left = list.size();

            if (!Expressions.compare(left, presentCompare, right)) {
                return false;
            }
        }

        if (params.containsKey("CheckDefinedPlayer")) {
            final String sIsPresent = params.get("CheckDefinedPlayer");
            int playersize = AbilityUtils.getDefinedPlayers(getHostCard(), sIsPresent, this).size();
            final String comparator = getParamOrDefault("DefinedPlayerCompare", "GE1");
            final String svarOperator = comparator.substring(0, 2);
            final String svarOperand = comparator.substring(2);
            final int operandValue = AbilityUtils.calculateAmount(getHostCard(), svarOperand, this);
            if (!Expressions.compare(playersize, svarOperator, operandValue)) {
                return false;
            }
        }

        if (params.containsKey("CheckSVar")) {
            // TODO this provides only the card controller instead of the stack one
            final int sVar = AbilityUtils.calculateAmount(getHostCard(), params.get("CheckSVar"), this);
            final String comparator = getParamOrDefault("SVarCompare", "GE1");
            final String svarOperator = comparator.substring(0, 2);
            final String svarOperand = comparator.substring(2);
            final int operandValue = AbilityUtils.calculateAmount(getHostCard(), svarOperand, this);
            if (!Expressions.compare(sVar, svarOperator, operandValue)) {
                return false;
            }
            if (hasParam("CheckSecondSVar")) {
                final int sVar2 = AbilityUtils.calculateAmount(this.hostCard, getParam("CheckSecondSVar"), this);
                final String comparator2 = getParamOrDefault("SecondSVarCompare", "GE1");
                final String svarOperator2 = comparator2.substring(0, 2);
                final String svarOperand2 = comparator2.substring(2);
                final int operandValue2 = AbilityUtils.calculateAmount(getHostCard(), svarOperand2, this);
                if (!Expressions.compare(sVar2, svarOperator2, operandValue2)) {
                    return false;
                }
            }
        }

        if (params.containsKey("ManaSpent")) {
            SpellAbility castSA = getHostCard().getCastSA();
            if (castSA == null) {
                return false;
            }
            if (!castSA.getPayingColors().hasAllColors(ManaAtom.fromName(params.get("ManaSpent")))) {
                return false;
            }
        }

        if (params.containsKey("ManaNotSpent")) {
            SpellAbility castSA = getHostCard().getCastSA();
            if (castSA != null && castSA.getPayingColors().hasAllColors(ManaAtom.fromName(params.get("ManaNotSpent")))) {
                return false;
            }
        }

        if (params.containsKey("WerewolfTransformCondition")) {
            if (!game.getStack().getSpellsCastLastTurn().isEmpty()) {
                return false;
            }
        }

        if (params.containsKey("WerewolfUntransformCondition")) {
            List<Card> casted = game.getStack().getSpellsCastLastTurn();
            boolean conditionMet = false;
            for (Player p : game.getPlayers()) {
                if (CardLists.count(casted, CardPredicates.isController(p)) > 1) {
                    conditionMet = true;
                    break;
                }
            }
            if (!conditionMet) {
                return false;
            }
        }

        if (params.containsKey("ActivateNoLoyaltyAbilitiesCondition")) {
            final Player active = game.getPhaseHandler().getPlayerTurn();
            return !active.getActivateLoyaltyAbilityThisTurn();
        }

        if (params.containsKey("ClassLevel")) {
            final int level = getHostCard().getClassLevel();
            final int levelMin = Integer.parseInt(params.get("ClassLevel"));
            if (level < levelMin) {
                return false;
            }
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

    protected IHasSVars getSVarFallback() {
        if (getCardState() != null)
            return getCardState();
        return getHostCard();
    }

    @Override
    public String getSVar(final String name) {
        if (sVars.containsKey(name)) {
            return sVars.get(name);
        }
        return getSVarFallback().getSVar(name);
    }

    @Override
    public boolean hasSVar(final String name) {
        return sVars.containsKey(name) || getSVarFallback().hasSVar(name);
    }

    public Integer getSVarInt(final String name) {
        String var = this.getSVar(name);
        if (var != null) {
            try {
                return Integer.parseInt(var);
            }
            catch (Exception e) {}
        }
        return null;
    }

    @Override
    public final void setSVar(final String name, final String value) {
        sVars.put(name, value);
    }

    @Override
    public Map<String, String> getSVars() {
        Map<String, String> res = Maps.newHashMap(getSVarFallback().getSVars());
        res.putAll(sVars);
        return res;
    }

    @Override
    public Map<String, String> getDirectSVars() {
        return sVars;
    }

    @Override
    public void setSVars(Map<String, String> newSVars) {
        sVars = Maps.newTreeMap();
        sVars.putAll(newSVars);
    }

    @Override
    public void removeSVar(String var) {
        sVars.remove(var);
    }

    public CardState getCardState() {
        return cardState;
    }
    public void setCardState(CardState state) {
        this.cardState = state;
    }
    public CardStateName getCardStateName() {
        if (this.getCardState() == null) {
            return null;
        }
        return getCardState().getView().getState();
    }

    public Card getOriginalHost() {
        if (getCardState() != null)
            return getCardState().getCard();
        return null;
    }

    public boolean isCopiedTrait() {
        if (this.getCardState() == null) {
            return false;
        }
        return !getHostCard().equals(getCardState().getCard());
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
            } else if (this.getHostCard().hasSVar(value)) {
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
        copy.setSVars(sVars);
        copy.setCardState(cardState);
        // dont use setHostCard to not trigger the not copied parts yet
        copy.hostCard = host;
        copy.keyword = this.keyword;
    }

    abstract public List<Object> getTriggerRemembered();

}
