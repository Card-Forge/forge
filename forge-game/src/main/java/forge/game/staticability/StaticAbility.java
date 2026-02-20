/*
 * Forge: Play Magic: the Gathering.
 * Copyright (C) 2011  Forge Team
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package forge.game.staticability;

import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import com.google.common.collect.*;

import forge.game.CardTraitBase;
import forge.game.Game;
import forge.game.GameEntity;
import forge.game.GameStage;
import forge.game.IIdentifiable;
import forge.game.ability.AbilityFactory;
import forge.game.ability.AbilityUtils;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CardCollectionView;
import forge.game.card.CardLists;
import forge.game.card.CardState;
import forge.game.cost.Cost;
import forge.game.cost.CostPart;
import forge.game.phase.PhaseHandler;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.Zone;
import forge.game.zone.ZoneType;
import forge.util.*;

/**
 * The Class StaticAbility.
 */
public class StaticAbility extends CardTraitBase implements IIdentifiable, Cloneable, Comparable<StaticAbility> {
    private static int maxId = 0;
    private static int nextId() { return ++maxId; }

    private int id;

    protected EnumSet<ZoneType> validHostZones;
    private Set<StaticAbilityMode> modes;
    private Set<StaticAbilityLayer> layers;
    private CardCollectionView ignoreEffectCards = new CardCollection();
    private final List<Player> ignoreEffectPlayers = Lists.newArrayList();
    private int mayPlayTurn = 0;

    private SpellAbility payingTrigSA;
    private StaticAbilityView view = null;

    @Override
    public final int getId() {
        return id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(StaticAbility.class, getId());
    }

    @Override
    public boolean equals(final Object obj) {
        return obj instanceof StaticAbility && this.id == ((StaticAbility) obj).id;
    }

    public Set<ZoneType> getActiveZone() {
        return validHostZones;
    }
    public void setActiveZone(EnumSet<ZoneType> zones) {
        validHostZones = zones;
    }

    public Set<StaticAbilityMode> getMode() {
        return this.modes;
    }
    public void setMode(Set<StaticAbilityMode> modes) {
        this.modes = modes;
    }

    public SpellAbility getPayingTrigSA() {
        // already cached?
        if (payingTrigSA == null && hasParam("Trigger")) {
            payingTrigSA = AbilityFactory.getAbility(getSVar(getParam("Trigger")), getHostCard());
            payingTrigSA.setIntrinsic(true);
        }
        return payingTrigSA;
    }

    /**
     * <p>
     * Getter for the field <code>mapParams</code>.
     * </p>
     *
     * @param abString
     *            a {@link java.lang.String} object.
     * @param hostCard
     *            a {@link forge.game.card.Card} object.
     * @return a {@link java.util.HashMap} object.
     */
    private static Map<String, String> parseParams(final String abString, final Card hostCard) {
        if (!(abString.length() > 0)) {
            throw new RuntimeException("StaticEffectFactory : getAbility -- abString too short in "
                    + hostCard.getName() + ": [" + abString + "]");
        }

        return FileSection.parseToMap(abString, FileSection.DOLLAR_SIGN_KV_SEPARATOR);
    }

    /**
     * Gets the {@link Set} of {@link StaticAbilityLayer}s in which this
     * {@link StaticAbility} is to be applied.
     *
     * @return the applicable layers.
     */
    private Set<StaticAbilityLayer> generateLayer() {
        if (!checkMode(StaticAbilityMode.Continuous)) {
            return EnumSet.noneOf(StaticAbilityLayer.class);
        }

        final Set<StaticAbilityLayer> layers = EnumSet.noneOf(StaticAbilityLayer.class);
        if (hasParam("GainControl")) {
            layers.add(StaticAbilityLayer.CONTROL);
        }

        if (hasParam("ChangeColorWordsTo") || hasParam("GainTextOf") || hasParam("AddNames") ||
                hasParam("SetName") || hasParam("Incorporate") || hasParam("ManaCost")) {
            layers.add(StaticAbilityLayer.TEXT);
        }

        if (hasParam("AddType") || hasParam("RemoveType")
                || hasParam("AddAllCreatureTypes")
                || hasParam("RemoveCardTypes") || hasParam("RemoveSubTypes")
                || hasParam("RemoveSuperTypes") || hasParam("RemoveLandTypes")
                || hasParam("RemoveCreatureTypes") || hasParam("RemoveArtifactTypes")
                || hasParam("RemoveEnchantmentTypes")) {
            layers.add(StaticAbilityLayer.TYPE);
        }

        if (hasParam("AddColor") || hasParam("RemoveColor") || hasParam("SetColor")) {
            layers.add(StaticAbilityLayer.COLOR);
        }

        if (hasParam("RemoveAllAbilities") || hasParam("RemoveNonManaAbilities") || hasParam("GainsAbilitiesOf")
                || hasParam("GainsAbilitiesOfDefined") || hasParam("GainsTriggerAbsOf")
                || hasParam("AddKeyword") || hasParam("AddAbility")
                || hasParam("AddTrigger") || hasParam("AddReplacementEffect")
                || hasParam("AddStaticAbility") || hasParam("AddSVar")
                || hasParam("CantHaveKeyword") || hasParam("ShareRememberedKeywords")
                || hasParam("RemoveKeyword")) {
            layers.add(StaticAbilityLayer.ABILITIES);
        }

        if (hasParam("SetPower") || hasParam("SetToughness")) {
            layers.add(isCharacteristicDefining() ? StaticAbilityLayer.CHARACTERISTIC :
                StaticAbilityLayer.SETPT);
        }
        if (hasParam("AddPower") || hasParam("AddToughness")) {
            layers.add(StaticAbilityLayer.MODIFYPT);
        }

        if (hasParam("AddHiddenKeyword") || hasParam("MayPlay")
                || hasParam("IgnoreEffectCost") || hasParam("Goad")
                || hasParam("AdjustLandPlays") || hasParam("ControlVote") || hasParam("AdditionalVote") || hasParam("AdditionalOptionalVote")
                || hasParam("DeclaresAttackers") || hasParam("DeclaresBlockers")) {
            layers.add(StaticAbilityLayer.RULES);
        }

        if (layers.isEmpty()) {
            layers.add(StaticAbilityLayer.RULES);
        }

        return layers;
    }

    public boolean isCharacteristicDefining() {
        return hasParam("CharacteristicDefining");
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
        if (hasParam("Description") && !this.isSuppressed()) {
            ITranslatable nameSource = getHostName(this);
            String desc = CardTranslation.translateSingleDescriptionText(getParam("Description"), nameSource);
            String translatedName = nameSource.getTranslatedName();
            desc = TextUtil.fastReplace(desc, "CARDNAME", translatedName);
            desc = TextUtil.fastReplace(desc, "NICKNAME", Lang.getInstance().getNickName(translatedName));

            return desc;
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
    public StaticAbility(final String params, final Card host, CardState state) {
        this(parseParams(params, host), host, state);
    }

    public static StaticAbility create(final String params, final Card host, CardState state, boolean intrinsic) {
        StaticAbility st = new StaticAbility(params, host, state);
        st.setIntrinsic(intrinsic);
        return st;
    }

    /**
     * Instantiates a new static ability.
     *
     * @param params
     *            the params
     * @param host
     *            the host
     */
    private StaticAbility(final Map<String, String> params, final Card host, CardState state) {
        this.id = nextId();
        this.originalMapParams.putAll(params);
        this.mapParams.putAll(params);
        this.hostCard = host;
        this.setCardState(state);
        if (hasParam("EffectZone")) {
            setActiveZone(EnumSet.copyOf(ZoneType.listValueOf(getParam("EffectZone"))));
        }
        if (hasParam("Mode")) {
            setMode(StaticAbilityMode.setValueOf(getParam("Mode")));
        }
        this.layers = this.generateLayer();
    }

    public StaticAbilityView getView() {
        if (view == null)
            view = new StaticAbilityView(this);
        else {
            view.updateHostCard(this);
            view.updateDescription(this);
        }
        return view;
    }

    public final CardCollectionView applyContinuousAbilityBefore(final StaticAbilityLayer layer, final CardCollectionView preList) {
        if (!shouldApplyContinuousAbility(layer, false)) {
            return null;
        }
        return StaticAbilityContinuous.applyContinuousAbility(this, layer, preList);
    }

    public final CardCollectionView applyContinuousAbility(final StaticAbilityLayer layer, final CardCollectionView affected) {
        if (!shouldApplyContinuousAbility(layer, true)) {
            return null;
        }
        return StaticAbilityContinuous.applyContinuousAbility(this, affected, layer);
    }

    /**
     * Check whether a continuous ability should be applied.
     *
     * @param layer
     *            the {@link StaticAbilityLayer} under investigation.
     * @param ignoreTempSuppression
     *            whether to ignore temporary suppression of this ability, to be
     *            used when this ability has already begun applying in another
     *            layer and has since been removed from its host card by another
     *            effect (see rule 613.5).
     * @return {@code true} if and only if this is a continuous ability that
     *         affects the specified layer, it's not suppressed, and its
     *         conditions are fulfilled.
     */
    private boolean shouldApplyContinuousAbility(final StaticAbilityLayer layer, final boolean previousRun) {
        return layers.contains(layer) && checkConditions(StaticAbilityMode.Continuous) && ( previousRun ||
                getHostCard().getStaticAbilities().contains(this) ||
                getHostCard().getHiddenStaticAbilities().contains(this));
    }

    public final Cost getAttackCost(final Card attacker, final GameEntity target, final List<Card> attackersWithOptionalCost) {
        if (!checkMode(StaticAbilityMode.CantAttackUnless) && (!checkMode(StaticAbilityMode.OptionalAttackCost) || !attackersWithOptionalCost.contains(attacker))) {
            return null;
        }
        if (!checkConditions()) {
            return null;
        }
        return StaticAbilityCantAttackBlock.getAttackCost(this, attacker, target);
    }

    public final boolean hasAttackCost(final Card attacker, Class<? extends CostPart> costType) {
        if (!checkConditions(StaticAbilityMode.OptionalAttackCost)) {
            return false;
        }
        return StaticAbilityCantAttackBlock.getAttackCost(this, attacker, null).hasSpecificCostType(costType);
    }

    public final Cost getBlockCost(final Card blocker, final Card attacker) {
        if (!checkConditions(StaticAbilityMode.CantBlockUnless)) {
            return null;
        }
        return StaticAbilityCantAttackBlock.getBlockCost(this, blocker, attacker);
    }

    public final boolean checkMode(StaticAbilityMode mode) {
        return this.modes.contains(mode);
    }

    public final boolean checkConditions(StaticAbilityMode mode) {
        return checkMode(mode) && checkConditions();
    }

    public final boolean zonesCheck() {
        if (isSuppressed()) {
            return false;
        }
        if (getHostCard().isPhasedOut()) {
            return false;
        }
        if (!isCharacteristicDefining()) {
            if (this.validHostZones != null) {
                Zone zone = getHostCard().getGame().getZoneOf(getHostCard());
                if (zone == null || !this.validHostZones.contains(zone.getZoneType())) {
                    return false;
                }
            } else if (!getHostCard().isInPlay()) { // default
                return false;
            }
        }
        return true;
    }

    /**
     * Check conditions.
     *
     * @return true, if the static ability is applicable.
     */
    public final boolean checkConditions() {
        final Player controller = getHostCard().getController();
        final Game game = getHostCard().getGame();
        final PhaseHandler ph = game.getPhaseHandler();

        if (!zonesCheck()) {
            return false;
        }

        String condition = getParam("Condition");
        if (null != condition) {
            if (condition.equals("Threshold") && !controller.hasThreshold()) return false;
            if (condition.equals("Hellbent") && !controller.hasHellbent()) return false;
            if (condition.equals("Metalcraft") && !controller.hasMetalcraft()) return false;
            if (condition.equals("Delirium") && !controller.hasDelirium()) return false;
            if (condition.equals("Ferocious") && !controller.hasFerocious()) return false;
            if (condition.equals("Desert") && !controller.hasDesert()) return false;
            if (condition.equals("Blessing") && !controller.hasBlessing()) return false;
            if (condition.equals("Monarch") & !controller.isMonarch()) return false;
            if (condition.equals("Night") & !game.isNight()) return false;
            if (condition.equals("MaxSpeed") && !controller.maxSpeed()) return false;

            if (condition.equals("PlayerTurn")) {
                if (!ph.isPlayerTurn(controller)) {
                    return false;
                }
            } else if (condition.equals("NotPlayerTurn")) {
                if (ph.isPlayerTurn(controller)) {
                    return false;
                }
            } else if (condition.equals("ExtraTurn")) {
                if (!game.getPhaseHandler().getPlayerTurn().isExtraTurn()) {
                    return false;
                }
            } else if (condition.equals("FatefulHour")) {
                if (controller.getLife() > 5) {
                    return false;
                }
            }
        }

        if (hasParam("Phases")) {
            if (!PhaseType.parseRange(getParam("Phases")).contains(ph.getPhase())) {
                return false;
            }
        }

        if (hasParam("PlayerTurn")) {
            List<Player> players = AbilityUtils.getDefinedPlayers(hostCard, getParam("PlayerTurn"), this);
            if (!players.contains(ph.getPlayerTurn())) {
                return false;
            }
        }

        if (hasParam("TopCardOfLibraryIs")) {
            if (controller.getCardsIn(ZoneType.Library).isEmpty()) {
                return false;
            }
            final Card topCard = controller.getCardsIn(ZoneType.Library).get(0);
            if (!topCard.isValid(getParam("TopCardOfLibraryIs").split(","), controller, this.hostCard, this)) {
                return false;
            }
        }

        if (hasParam("IsPresent")) {
            final ZoneType zone = hasParam("PresentZone") ? ZoneType.valueOf(getParam("PresentZone")) : ZoneType.Battlefield;
            final String compare = getParamOrDefault("PresentCompare", "GE1");
            CardCollectionView list = game.getCardsIn(zone);
            final String present = getParam("IsPresent");

            list = CardLists.getValidCards(list, present, controller, hostCard, this);

            int right = 1;
            final String rightString = compare.substring(2);
            right = AbilityUtils.calculateAmount(hostCard, rightString, this);
            final int left = list.size();

            if (!Expressions.compare(left, compare, right)) {
                return false;
            }
        }

        if (hasParam("GameStage")) {
            String[] stageDefs = TextUtil.split(getParam("GameStage"), ',');
            boolean isRelevantStage = false;
            for (String stage : stageDefs) {
                isRelevantStage |= (game.getAge() == GameStage.valueOf(stage));
            }
            return isRelevantStage;
        }

        if (hasParam("ClassLevel")) {
            final int level = this.hostCard.getClassLevel();
            final int levelMin = Integer.parseInt(getParam("ClassLevel"));
            if (level < levelMin) {
                return false;
            }
        }

        if (hasParam("CheckSVar")) {
            final int sVar = AbilityUtils.calculateAmount(this.hostCard, getParam("CheckSVar"), this);
            final String comparator = getParamOrDefault("SVarCompare", "GE1");
            final String svarOperator = comparator.substring(0, 2);
            final String svarOperand = comparator.substring(2);
            final int operandValue = AbilityUtils.calculateAmount(this.hostCard, svarOperand, this);
            if (!Expressions.compare(sVar, svarOperator, operandValue)) {
                return false;
            }
        } else { //no need to check the others
            return true;
        }

        if (hasParam("CheckSecondSVar")) {
            final int sVar = AbilityUtils.calculateAmount(this.hostCard, getParam("CheckSecondSVar"), this);
            final String comparator = getParamOrDefault("SecondSVarCompare", "GE1");
            final String svarOperator = comparator.substring(0, 2);
            final String svarOperand = comparator.substring(2);
            final int operandValue = AbilityUtils.calculateAmount(this.hostCard, svarOperand, this);
            if (!Expressions.compare(sVar, svarOperator, operandValue)) {
                return false;
            }
        } else { //no need to check the others
            return true;
        }

        if (hasParam("CheckThirdSVar")) {
            final int sVar = AbilityUtils.calculateAmount(this.hostCard, getParam("CheckThirdSVar"), this);
            final String comparator = getParamOrDefault("ThirdSVarCompare", "GE1");
            final String svarOperator = comparator.substring(0, 2);
            final String svarOperand = comparator.substring(2);
            final int operandValue = AbilityUtils.calculateAmount(this.hostCard, svarOperand, this);
            if (!Expressions.compare(sVar, svarOperator, operandValue)) {
                return false;
            }
        } else { //no need to check the others
            return true;
        }

        if (hasParam("CheckFourthSVar")) {
            final int sVar = AbilityUtils.calculateAmount(this.hostCard, getParam("CheckFourthSVar"), this);
            final String comparator = getParamOrDefault("FourthSVarCompare", "GE1");
            final String svarOperator = comparator.substring(0, 2);
            final String svarOperand = comparator.substring(2);
            final int operandValue = AbilityUtils.calculateAmount(this.hostCard, svarOperand, this);
            if (!Expressions.compare(sVar, svarOperator, operandValue)) {
                return false;
            }
        }

        return true;
    }

    /**
     * @return the ignoreEffectCards
     */
    public CardCollectionView getIgnoreEffectCards() {
        return ignoreEffectCards;
    }

    /**
     * @param cards the ignoreEffectCards to set
     */
    public void setIgnoreEffectCards(final CardCollectionView cards) {
        ignoreEffectCards = cards;
    }

    /**
     * @return the ignoreEffectPlayers
     */
    public List<Player> getIgnoreEffectPlayers() {
        return ignoreEffectPlayers;
    }

    /**
     * @param p the ignoreEffectPlayers to add
     */
    public void addIgnoreEffectPlayers(final Player p) {
        ignoreEffectPlayers.add(p);
    }

    public void clearIgnoreEffects() {
        ignoreEffectPlayers.clear();
        ignoreEffectCards = new CardCollection();
    }

    /**
     * @return the layer
     */
    public Set<StaticAbilityLayer> getLayers() {
        return layers;
    }

    public int getMayPlayTurn() {
        return mayPlayTurn;
    }

    public void incMayPlayTurn() {
        this.mayPlayTurn++;
    }

    public void resetMayPlayTurn() {
        this.mayPlayTurn = 0;
    }

    @Override
    public int compareTo(StaticAbility arg0) {
        return ComparisonChain.start()
        .compare(getHostCard(),arg0.getHostCard())
        .compare(getId(), arg0.getId())
        .result();
    }

    public long getTimestamp() {
        if (hasParam("Timestamp")) {
            return Long.valueOf(getParam("Timestamp"));
        }
        return getHostCard().getLayerTimestamp();
    }

    @Override
    public void setHostCard(Card host) {
        super.setHostCard(host);
        if (payingTrigSA != null) {
            payingTrigSA.setHostCard(host);
        }
    }

    public final StaticAbility copy(Card newHost, boolean lki) {
        return copy(newHost, lki, false);
    }
    public StaticAbility copy(Card host, final boolean lki, boolean keepTextChanges) {
        StaticAbility clone = null;
        try {
            clone = (StaticAbility) clone();
            clone.id = lki ? id : nextId();

            copyHelper(clone, host, lki || keepTextChanges);

            // reset to force refresh if needed
            clone.payingTrigSA = null;

            if (!lki) {
                clone.mayPlayTurn = 0;
            }

            clone.layers = this.generateLayer();
            if (validHostZones != null) {
                clone.setActiveZone(EnumSet.copyOf(validHostZones));
            }
            if (modes != null) {
                clone.setMode(EnumSet.copyOf(modes));
            }
        } catch (final CloneNotSupportedException e) {
            System.err.println(e);
        }
        return clone;
    }

}
