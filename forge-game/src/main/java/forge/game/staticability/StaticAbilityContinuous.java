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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import forge.GameCommand;
import forge.card.CardStateName;
import forge.card.CardType;
import forge.card.ColorSet;
import forge.card.MagicColor;
import forge.card.RemoveType;
import forge.game.Game;
import forge.game.GlobalRuleChange;
import forge.game.StaticEffect;
import forge.game.StaticEffects;
import forge.game.ability.AbilityUtils;
import forge.game.ability.ApiType;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CardCollectionView;
import forge.game.card.CardFactoryUtil;
import forge.game.card.CardLists;
import forge.game.card.CardPredicates;
import forge.game.card.CardState;
import forge.game.card.CardUtil;
import forge.game.cost.Cost;
import forge.game.keyword.Keyword;
import forge.game.keyword.KeywordInterface;
import forge.game.player.Player;
import forge.game.player.PlayerCollection;
import forge.game.replacement.ReplacementEffect;
import forge.game.spellability.AbilityStatic;
import forge.game.spellability.SpellAbility;
import forge.game.trigger.Trigger;
import forge.game.zone.ZoneType;
import forge.util.TextUtil;

/**
 * The Class StaticAbility_Continuous.
 */
public final class StaticAbilityContinuous {

    // Private constructor to prevent instantiation
    private StaticAbilityContinuous() {
    }

    /**
     * Apply the effects of a static ability that apply in a particular layer.
     * The cards to which the effects are applied are dynamically determined.
     *
     * @param stAb
     *            a {@link StaticAbility}.
     * @param layer
     *            the {@link StaticAbilityLayer} of effects to apply.
     * @return a {@link CardCollectionView} of cards that have been affected.
     * @see #getAffectedCards(StaticAbility)
     * @see #applyContinuousAbility(StaticAbility, CardCollectionView,
     *      StaticAbilityLayer)
     */
	public static CardCollectionView applyContinuousAbility(final StaticAbility stAb, final StaticAbilityLayer layer, final CardCollectionView preList) {
        final CardCollectionView affectedCards = getAffectedCards(stAb, preList);
        return applyContinuousAbility(stAb, affectedCards, layer);
    }

    /**
     * Apply the effects of a static ability that apply in a particular layer to
     * a predefined set of cards.
     *
     * @param stAb
     *            a {@link StaticAbility}.
     * @param affectedCards
     *            a {@link CardCollectionView} of cards that are to be affected.
     * @param layer
     *            the {@link StaticAbilityLayer} of effects to apply.
     * @return a {@link CardCollectionView} of cards that have been affected,
     *         identical to {@code affectedCards}.
     */
    public static CardCollectionView applyContinuousAbility(final StaticAbility stAb, final CardCollectionView affectedCards, final StaticAbilityLayer layer) {
        final Map<String, String> params = stAb.getMapParams();
        final Card hostCard = stAb.getHostCard();
        final Player controller = hostCard.getController();

        final List<Player> affectedPlayers = StaticAbilityContinuous.getAffectedPlayers(stAb);
        final Game game = hostCard.getGame();

        final StaticEffects effects = game.getStaticEffects();
        final StaticEffect se = effects.getStaticEffect(stAb);
        se.setAffectedCards(affectedCards);
        se.setAffectedPlayers(affectedPlayers);
        se.setParams(params);
        se.setTimestamp(hostCard.getTimestamp());

        // nothing more to do
        if (stAb.hasParam("Affected") && affectedPlayers.isEmpty() && affectedCards.isEmpty()) {
            return affectedCards;
        }

        String addP = "";
        int powerBonus = 0;
        String addT = "";
        int toughnessBonus = 0;
        String setP = "";
        Integer setPower = null;
        String setT = "";
        Integer setToughness = null;

        List<String> addKeywords = null;
        List<String> addHiddenKeywords = Lists.newArrayList();
        List<String> removeKeywords = null;
        String[] addAbilities = null;
        String[] addReplacements = null;
        String[] addSVars = null;
        List<String> addTypes = null;
        List<String> removeTypes = null;
        ColorSet addColors = null;
        String[] addTriggers = null;
        String[] addStatics = null;
        boolean removeAllAbilities = false;
        boolean removeNonMana = false;
        boolean addAllCreatureTypes = false;
        Set<RemoveType> remove = EnumSet.noneOf(RemoveType.class);

        boolean overwriteColors = false;

        Set<Keyword> cantHaveKeyword = null;

        List<Player> mayLookAt = null;

        boolean controllerMayPlay = false, mayPlayWithoutManaCost = false, mayPlayWithFlash = false;
        String mayPlayAltManaCost = null;
        boolean mayPlayGrantZonePermissions = true;
        Integer mayPlayLimit = null;

        //Global rules changes
        if (layer == StaticAbilityLayer.RULES && params.containsKey("GlobalRule")) {
            effects.setGlobalRuleChange(GlobalRuleChange.fromString(params.get("GlobalRule")));
        }

        if (layer == StaticAbilityLayer.SETPT || layer == StaticAbilityLayer.CHARACTERISTIC) {
            if (params.containsKey("SetPower")) {
                setP = params.get("SetPower");
                setPower = AbilityUtils.calculateAmount(hostCard, setP, stAb);
            }

            if (params.containsKey("SetToughness")) {
                setT = params.get("SetToughness");
                setToughness = AbilityUtils.calculateAmount(hostCard, setT, stAb);
            }
        }

        if (layer == StaticAbilityLayer.MODIFYPT) {
            if (params.containsKey("AddPower")) {
                addP = params.get("AddPower");
                powerBonus = AbilityUtils.calculateAmount(hostCard, addP, stAb, true);
            }

            if (params.containsKey("AddToughness")) {
                addT = params.get("AddToughness");
                toughnessBonus = AbilityUtils.calculateAmount(hostCard, addT, stAb, true);
            }
        }

        if (layer == StaticAbilityLayer.ABILITIES) {
            if (params.containsKey("AddKeyword")) {
                addKeywords = Lists.newArrayList(Arrays.asList(params.get("AddKeyword").split(" & ")));
                final List<String> newKeywords = Lists.newArrayList();

                // update keywords with Chosen parts
                final String hostCardUID = Integer.toString(hostCard.getId()); // Protection with "doesn't remove" effect

                Iterables.removeIf(addKeywords, new Predicate<String>() {
                    @Override
                    public boolean apply(String input) {
                        if (!hostCard.hasChosenColor() && input.contains("ChosenColor")) {
                            return true;
                        }
                        if (!hostCard.hasChosenType() && input.contains("ChosenType")) {
                            return true;
                        }
                        if (!hostCard.hasChosenNumber() && input.contains("ChosenNumber")) {
                            return true;
                        }
                        if (!hostCard.hasChosenPlayer() && input.contains("ChosenPlayer")) {
                            return true;
                        }
                        if (!hostCard.hasChosenName() && input.contains("ChosenName")) {
                            return true;
                        }
                        if (!hostCard.hasChosenEvenOdd() && (input.contains("ChosenEvenOdd") || input.contains("chosenEvenOdd"))) {
                            return true;
                        }

                        if (input.contains("AllColors") || input.contains("allColors")) {
                            for (byte color : MagicColor.WUBRG) {
                                final String colorWord = MagicColor.toLongString(color);
                                String y = input.replaceAll("AllColors", StringUtils.capitalize(colorWord));
                                y = y.replaceAll("allColors", colorWord);
                                newKeywords.add(y);
                            }
                            return true;
                        }
                        if (input.contains("CommanderColorID")) {
                            if (!hostCard.getController().getCommanders().isEmpty()) {
                                if (input.contains("NotCommanderColorID")) {
                                    for (Byte color : hostCard.getController().getNotCommanderColorID()) {
                                        newKeywords.add(input.replace("NotCommanderColorID", MagicColor.toLongString(color)));
                                    }
                                    return true;
                                } else for (Byte color : hostCard.getController().getCommanderColorID()) {
                                    newKeywords.add(input.replace("CommanderColorID", MagicColor.toLongString(color)));
                                }
                                return true;
                            }
                            return true;
                        }
                        // two variants for Red vs. red in keyword
                        if (input.contains("ColorsYouCtrl") || input.contains("colorsYouCtrl")) {
                            final ColorSet colorsYouCtrl = CardUtil.getColorsFromCards(controller.getCardsIn(ZoneType.Battlefield));

                            for (byte color : colorsYouCtrl) {
                                final String colorWord = MagicColor.toLongString(color);
                                String y = input.replaceAll("ColorsYouCtrl", StringUtils.capitalize(colorWord));
                                y = y.replaceAll("colorsYouCtrl", colorWord);
                                newKeywords.add(y);
                            }
                            return true;
                        }
                        if (input.contains("EachCMCAmongDefined")) {
                            String keywordDefined = params.get("KeywordDefined");
                            CardCollectionView definedCards = game.getCardsIn(ZoneType.Battlefield);
                            definedCards = CardLists.getValidCards(definedCards, keywordDefined, hostCard.getController(),
                                    hostCard, stAb);
                            for (Card c : definedCards) {
                                final int cmc = c.getCMC();
                                String y = (input.replace(" from EachCMCAmongDefined", ":Card.cmcEQ"
                                        + (cmc) + ":Protection from mana value " + (cmc)));
                                if (!newKeywords.contains(y)) {
                                    newKeywords.add(y);
                                }
                            }
                            return true;
                        }

                        return false;
                    }

                });

                addKeywords.addAll(newKeywords);

                addKeywords = Lists.transform(addKeywords, new Function<String, String>() {

                    @Override
                    public String apply(String input) {
                        if (hostCard.hasChosenColor()) {
                            input = input.replaceAll("ChosenColor", StringUtils.capitalize(hostCard.getChosenColor()));
                            input = input.replaceAll("chosenColor", hostCard.getChosenColor().toLowerCase());
                        }
                        if (hostCard.hasChosenType()) {
                            input = input.replaceAll("ChosenType", hostCard.getChosenType());
                        }
                        if (hostCard.hasChosenNumber()) {
                            input = input.replaceAll("ChosenNumber", String.valueOf(hostCard.getChosenNumber()));
                        }
                        if (hostCard.hasChosenPlayer()) {
                            Player cp = hostCard.getChosenPlayer();
                            input = input.replaceAll("ChosenPlayerUID", String.valueOf(cp.getId()));
                            input = input.replaceAll("ChosenPlayerName", cp.getName());
                        }
                        if (hostCard.hasChosenName()) {
                            final String chosenName = hostCard.getChosenName().replace(",", ";");
                            input = input.replaceAll("ChosenName", "Card.named" + chosenName);
                        }
                        if (hostCard.hasChosenEvenOdd()) {
                            input = input.replaceAll("ChosenEvenOdd", hostCard.getChosenEvenOdd().toString());
                            input = input.replaceAll("chosenEvenOdd", hostCard.getChosenEvenOdd().toString().toLowerCase());
                        }
                        input = input.replace("HostCardUID", hostCardUID);
                        if (params.containsKey("CalcKeywordN")) {
                            input = input.replace("N", String.valueOf(AbilityUtils.calculateAmount(hostCard, params.get("CalcKeywordN"), stAb)));
                        }
                        return input;
                    }

                });

                if (params.containsKey("SharedKeywordsZone")) {
                    List<ZoneType> zones = ZoneType.listValueOf(params.get("SharedKeywordsZone"));
                    String[] restrictions = params.containsKey("SharedRestrictions") ? params.get("SharedRestrictions").split(",") : new String[] {"Card"};
                    addKeywords = CardFactoryUtil.sharedKeywords(addKeywords, restrictions, zones, hostCard, stAb);
                }
            } else if (params.containsKey("ShareRememberedKeywords")) {
                List<String> kwToShare = Lists.newArrayList();
                for (final Object o : hostCard.getRemembered()) {
                    final String k = (String) o;
                    kwToShare.add(k);
                }
                if (!kwToShare.isEmpty()) {
                    addKeywords = kwToShare;
                }
            }

            if (params.containsKey("CantHaveKeyword")) {
                cantHaveKeyword = Keyword.setValueOf(params.get("CantHaveKeyword"));
            }

            if (params.containsKey("RemoveKeyword")) {
                removeKeywords = Arrays.asList(params.get("RemoveKeyword").split(" & "));
            }
        }

        if (layer == StaticAbilityLayer.RULES && params.containsKey("AddHiddenKeyword")) {
            addHiddenKeywords.addAll(Arrays.asList(params.get("AddHiddenKeyword").split(" & ")));
        }

        if (layer == StaticAbilityLayer.ABILITIES) {
            if (params.containsKey("RemoveAllAbilities")) {
                removeAllAbilities = true;
                if (params.containsKey("ExceptManaAbilities")) {
                    removeNonMana = true;
                }
            }

            if (params.containsKey("AddAbility")) {
                final String[] sVars = params.get("AddAbility").split(" & ");
                for (int i = 0; i < sVars.length; i++) {
                    sVars[i] = AbilityUtils.getSVar(stAb, sVars[i]);
                }
                addAbilities = sVars;
            }

            if (params.containsKey("AddReplacementEffects")) {
                final String[] sVars = params.get("AddReplacementEffects").split(" & ");
                for (int i = 0; i < sVars.length; i++) {
                    sVars[i] = AbilityUtils.getSVar(stAb, sVars[i]);
                }
                addReplacements = sVars;
            }

            if (params.containsKey("AddTrigger")) {
                final String[] sVars = params.get("AddTrigger").split(" & ");
                for (int i = 0; i < sVars.length; i++) {
                    sVars[i] = AbilityUtils.getSVar(stAb, sVars[i]);
                }
                addTriggers = sVars;
            }

            if (params.containsKey("AddStaticAbility")) {
                final String[] sVars = params.get("AddStaticAbility").split(" & ");
                for (int i = 0; i < sVars.length; i++) {
                    sVars[i] = AbilityUtils.getSVar(stAb, sVars[i]);
                }
                addStatics = sVars;
            }

            if (params.containsKey("AddSVar")) {
                addSVars = params.get("AddSVar").split(" & ");
            }
        }

        if (layer == StaticAbilityLayer.TYPE) {
            if (params.containsKey("AddType")) {
                addTypes = Lists.newArrayList(Arrays.asList(params.get("AddType").split(" & ")));
                List<String> newTypes = Lists.newArrayList();

                Iterables.removeIf(addTypes, new Predicate<String>() {
                    @Override
                    public boolean apply(String input) {
                        if (input.equals("ChosenType") && !hostCard.hasChosenType()) {
                            return true;
                        }
                        if (input.equals("ChosenType2") && !hostCard.hasChosenType2()) {
                            return true;
                        }
                        if (input.equals("ImprintedCreatureType")) {
                            if (hostCard.hasImprintedCard()) {
                                newTypes.addAll(hostCard.getImprintedCards().getLast().getType().getCreatureTypes());
                            }
                            return true;
                        }
                        if (input.equals("AllBasicLandType")) {
                            newTypes.addAll(CardType.getBasicTypes());
                            return true;
                        }
                        return false;
                    }
                });
                addTypes.addAll(newTypes);

                addTypes = Lists.transform(addTypes, new Function<String, String>() {
                    @Override
                    public String apply(String input) {
                        if (hostCard.hasChosenType2()) {
                            input = input.replaceAll("ChosenType2", hostCard.getChosenType2());
                        }
                        if (hostCard.hasChosenType()) {
                            input = input.replaceAll("ChosenType", hostCard.getChosenType());
                        }
                        return input;
                    }

                });
            }

            if (params.containsKey("RemoveType")) {
                removeTypes = Lists.newArrayList(Arrays.asList(params.get("RemoveType").split(" & ")));

                Iterables.removeIf(removeTypes, new Predicate<String>() {
                    @Override
                    public boolean apply(String input) {
                        if (input.equals("ChosenType") && !hostCard.hasChosenType()) {
                            return true;
                        }
                        return false;
                    }
                });
            }
            if (params.containsKey("AddAllCreatureTypes")) {
                addAllCreatureTypes = true;
            }
            if (params.containsKey("RemoveSuperTypes")) {
                remove.add(RemoveType.SuperTypes);
            }
            if (params.containsKey("RemoveCardTypes")) {
                remove.add(RemoveType.CardTypes);
            }
            if (params.containsKey("RemoveSubTypes")) {
                remove.add(RemoveType.SubTypes);
            }
            if (params.containsKey("RemoveLandTypes")) {
                remove.add(RemoveType.LandTypes);
            }
            if (params.containsKey("RemoveCreatureTypes")) {
                remove.add(RemoveType.CreatureTypes);
            }
            if (params.containsKey("RemoveArtifactTypes")) {
                remove.add(RemoveType.ArtifactTypes);
            }
            if (params.containsKey("RemoveEnchantmentTypes")) {
                remove.add(RemoveType.EnchantmentTypes);
            }
        }

        if (layer == StaticAbilityLayer.COLOR) {
            if (params.containsKey("AddColor")) {
                final String colors = params.get("AddColor");
                if (colors.equals("ChosenColor")) {
                    if (hostCard.hasChosenColor()) {
                        addColors = ColorSet.fromNames(hostCard.getChosenColors());
                    }
                } else if (colors.equals("All")) {
                    addColors = ColorSet.ALL_COLORS;
                } else {
                    addColors = ColorSet.fromNames(colors.split(" & "));
                }
            }

            if (params.containsKey("SetColor")) {
                final String colors = params.get("SetColor");
                if (colors.equals("ChosenColor")) {
                    if (hostCard.hasChosenColor()) {
                        addColors = ColorSet.fromNames(hostCard.getChosenColors());
                    }
                } else if (colors.equals("All")) {
                    addColors = ColorSet.ALL_COLORS;
                } else {
                    addColors = ColorSet.fromNames(colors.split(" & "));
                }
                overwriteColors = true;
            }
        }

        if (layer == StaticAbilityLayer.RULES) {
            // These fall under Rule changes, as they don't fit any other category
            if (params.containsKey("MayLookAt")) {
                String look = params.get("MayLookAt");
                if ("True".equals(look)) {
                    look = "You";
                }
                mayLookAt = AbilityUtils.getDefinedPlayers(hostCard, look, stAb);
            }
            if (params.containsKey("MayPlay")) {
                controllerMayPlay = true;
                if (params.containsKey("MayPlayWithoutManaCost")) {
                    mayPlayWithoutManaCost = true;
                } else if (params.containsKey("MayPlayAltManaCost")) {
                    mayPlayAltManaCost = params.get("MayPlayAltManaCost");
                }
                if (params.containsKey("MayPlayWithFlash")) {
                    mayPlayWithFlash = true;
                }
                if (params.containsKey("MayPlayLimit")) {
                    mayPlayLimit = Integer.parseInt(params.get("MayPlayLimit"));
                }
                if (params.containsKey("MayPlayDontGrantZonePermissions")) {
                    mayPlayGrantZonePermissions = false;
                }
            }

            if (params.containsKey("IgnoreEffectCost")) {
                String cost = params.get("IgnoreEffectCost");
                buildIgnorEffectAbility(stAb, cost, affectedPlayers, affectedCards);
            }
        }

        // modify players
        for (final Player p : affectedPlayers) {
            // add keywords
            if (addKeywords != null) {
                p.addChangedKeywords(addKeywords, removeKeywords, se.getTimestamp(), stAb.getId());
            }

            // add static abilities
            if (addStatics != null) {
                for (String s : addStatics) {
                    StaticAbility stat = p.addStaticAbility(hostCard, s);
                    stat.setIntrinsic(false);
                }
            }

            if (layer == StaticAbilityLayer.RULES) {
                if (params.containsKey("SetMaxHandSize")) {
                    String mhs = params.get("SetMaxHandSize");
                    if (mhs.equals("Unlimited")) {
                        p.setUnlimitedHandSize(true);
                    } else {
                        p.setUnlimitedHandSize(false);
                        int max = AbilityUtils.calculateAmount(hostCard, mhs, stAb);
                        p.setMaxHandSize(max);
                    }
                }
                if (params.containsKey("RaiseMaxHandSize")) {
                    String rmhs = params.get("RaiseMaxHandSize");
                    int rmax = AbilityUtils.calculateAmount(hostCard, rmhs, stAb);
                    p.setMaxHandSize(p.getMaxHandSize() + rmax);
                }

                if (params.containsKey("AdjustLandPlays")) {
                    String mhs = params.get("AdjustLandPlays");
                    if (mhs.equals("Unlimited")) {
                        p.addMaxLandPlaysInfinite(se.getTimestamp());
                    } else {
                        int add = AbilityUtils.calculateAmount(hostCard, mhs, stAb);
                        p.addMaxLandPlays(se.getTimestamp(), add);
                    }
                }

                if (params.containsKey("ControlOpponentsSearchingLibrary")) {
                    Player cntl = Iterables.getFirst(AbilityUtils.getDefinedPlayers(hostCard, params.get("ControlOpponentsSearchingLibrary"), stAb), null);
                    p.addControlledWhileSearching(se.getTimestamp(), cntl);
                }

                if (params.containsKey("ControlVote")) {
                    p.addControlVote(se.getTimestamp());
                }
                if (params.containsKey("AdditionalVote")) {
                    String mhs = params.get("AdditionalVote");
                    int add = AbilityUtils.calculateAmount(hostCard, mhs, stAb);
                    p.addAdditionalVote(se.getTimestamp(), add);
                }
                if (params.containsKey("AdditionalOptionalVote")) {
                    String mhs = params.get("AdditionalOptionalVote");
                    int add = AbilityUtils.calculateAmount(hostCard, mhs, stAb);
                    p.addAdditionalOptionalVote(se.getTimestamp(), add);
                }
            }
        }

        // start modifying the cards
        for (int i = 0; i < affectedCards.size(); i++) {
            final Card affectedCard = affectedCards.get(i);

            // Gain control
            if (layer == StaticAbilityLayer.CONTROL && params.containsKey("GainControl")) {
                final PlayerCollection gain = AbilityUtils.getDefinedPlayers(hostCard, params.get("GainControl"), stAb);
                if (!gain.isEmpty()) {
                    affectedCard.addTempController(gain.get(0), hostCard.getTimestamp());
                }
            }

            // Gain text from another card
            if (layer == StaticAbilityLayer.TEXT) {
                if (params.containsKey("GainTextOf")) {
                    CardCollection allValid = AbilityUtils.getDefinedCards(hostCard, params.get("GainTextOf"), stAb);
                    if (!allValid.isEmpty()) {
                        Card first = allValid.getFirst();

                        // for Volrath’s Shapeshifter, respect flipped state if able?
                        CardState state = first.getState(affectedCard.isFlipped() && first.isFlipCard() ? CardStateName.Flipped : first.getCurrentStateName());

                        List<SpellAbility> spellAbilities = Lists.newArrayList();
                        List<Trigger> trigger = Lists.newArrayList();
                        List<ReplacementEffect> replacementEffects = Lists.newArrayList();
                        List<StaticAbility> staticAbilities = Lists.newArrayList();
                        List<KeywordInterface> keywords = Lists.newArrayList();

                        for (SpellAbility sa : state.getSpellAbilities()) {
                            spellAbilities.add(affectedCard.getSpellAbilityForStaticAbilityByText(sa, stAb));
                        }
                        if (params.containsKey("GainTextAbilities")) {
                            for (String ability : params.get("GainTextAbilities").split(" & ")) {
                                spellAbilities.add(affectedCard.getSpellAbilityForStaticAbilityGainedByText(AbilityUtils.getSVar(stAb, ability), stAb));
                            }
                        }
                        for (Trigger tr : state.getTriggers()) {
                            trigger.add(affectedCard.getTriggerForStaticAbilityByText(tr, stAb));
                        }
                        for (ReplacementEffect re : state.getReplacementEffects()) {
                            replacementEffects.add(affectedCard.getReplacementEffectForStaticAbilityByText(re, stAb));
                        }
                        for (StaticAbility st : state.getStaticAbilities()) {
                            staticAbilities.add(affectedCard.getStaticAbilityForStaticAbilityByText(st, stAb));
                        }
                        long kwIdx = 1;
                        for (KeywordInterface ki : state.getIntrinsicKeywords()) {
                            keywords.add(affectedCard.getKeywordForStaticAbilityByText(ki, stAb, kwIdx));
                            kwIdx++;
                        }

                        // Volrath’s Shapeshifter has that card’s name, mana cost, color, types, abilities, power, and toughness.

                        // name
                        affectedCard.addChangedName(state.getName(), false, se.getTimestamp(), stAb.getId());
                        // Mana cost
                        affectedCard.addChangedManaCost(state.getManaCost(), se.getTimestamp(), stAb.getId());
                        // color
                        affectedCard.addColorByText(ColorSet.fromMask(state.getColor()), se.getTimestamp(), stAb.getId());
                        // type
                        affectedCard.addChangedCardTypesByText(new CardType(state.getType()), se.getTimestamp(), stAb.getId());
                        // abilities
                        affectedCard.addChangedCardTraitsByText(spellAbilities, trigger, replacementEffects, staticAbilities, se.getTimestamp(), stAb.getId());
                        affectedCard.addChangedCardKeywordsByText(keywords, se.getTimestamp(), stAb.getId(), false);

                        // power and toughness
                        affectedCard.addNewPTByText(state.getBasePower(), state.getBaseToughness(), se.getTimestamp(), stAb.getId());
                    }
                }

                if (stAb.hasParam("AddNames")) { // currently only for AllNonLegendaryCreatureNames
                    affectedCard.addChangedName(null, true, se.getTimestamp(), stAb.getId());
                }
                if (stAb.hasParam("SetName")) {
                    affectedCard.addChangedName(stAb.getParam("SetName"), false,
                            se.getTimestamp(), stAb.getId());
                }

                // Change color words
                if (params.containsKey("ChangeColorWordsTo")) {
                    final byte color;
                    String changeColorWordsTo = params.get("ChangeColorWordsTo");
                    if (changeColorWordsTo.equals("ChosenColor")) {
                        if (hostCard.hasChosenColor()) {
                            color = MagicColor.fromName(Iterables.getFirst(hostCard.getChosenColors(), null));
                        } else {
                            color = 0;
                        }
                    } else {
                        color = MagicColor.fromName(changeColorWordsTo);
                    }

                    if (color != 0) {
                        final String colorName = MagicColor.toLongString(color);
                        affectedCard.addChangedTextColorWord(stAb.getParamOrDefault("ChangeColorWordsFrom", "Any"), colorName, se.getTimestamp(), stAb.getId());
                    }
                }
            }

            // set P/T
            if (layer == StaticAbilityLayer.SETPT || layer == StaticAbilityLayer.CHARACTERISTIC) {
                if (setPower != null || setToughness != null) {
                    // non CharacteristicDefining
                    if (setP.contains("Affected")) {
                        setPower = AbilityUtils.calculateAmount(affectedCard, setP, stAb, true);
                    }
                    if (setT.contains("Affected")) {
                        setToughness = AbilityUtils.calculateAmount(affectedCard, setT, stAb, true);
                    }
                    affectedCard.addNewPT(setPower, setToughness,
                        hostCard.getTimestamp(), stAb.getId(), layer == StaticAbilityLayer.CHARACTERISTIC);
                }
            }

            // add P/T bonus
            if (layer == StaticAbilityLayer.MODIFYPT) {
                if (addP.contains("Affected")) {
                    // TODO don't calculate these above if this gets used instead
                    powerBonus = AbilityUtils.calculateAmount(affectedCard, addP, stAb, true);
                }
                if (addT.contains("Affected")) {
                    toughnessBonus = AbilityUtils.calculateAmount(affectedCard, addT, stAb, true);
                }
                affectedCard.addPTBoost(powerBonus, toughnessBonus, se.getTimestamp(), stAb.getId());
            }

            // add keywords
            if (addKeywords != null || removeKeywords != null || removeAllAbilities) {
                List<String> newKeywords = null;
                if (addKeywords != null) {
                    newKeywords = Lists.newArrayList(addKeywords);
                    final List<String> extraKeywords = Lists.newArrayList();

                    Iterables.removeIf(newKeywords, new Predicate<String>() {
                        @Override
                        public boolean apply(String input) {
                            if (input.contains("CardManaCost")) {
                                if (affectedCard.getManaCost().isNoCost()) {
                                    return true;
                                }
                            }
                            // replace one Keyword with list of keywords
                            if (input.startsWith("Protection") && input.contains("CardColors")) {
                                for (Byte color : affectedCard.getColor()) {
                                    extraKeywords.add(input.replace("CardColors", MagicColor.toLongString(color)));
                                }
                                return true;
                            }

                            return false;
                        }
                    });
                    newKeywords.addAll(extraKeywords);

                    newKeywords = Lists.transform(newKeywords, new Function<String, String>() {

                        @Override
                        public String apply(String input) {
                            if (input.contains("CardManaCost")) {
                                input = input.replace("CardManaCost", affectedCard.getManaCost().getShortString());
                            } else if (input.contains("ConvertedManaCost")) {
                                final String costcmc = Integer.toString(affectedCard.getCMC());
                                input = input.replace("ConvertedManaCost", costcmc);
                            }
                            return input;
                        }
                    });
                }

                affectedCard.addChangedCardKeywords(newKeywords, removeKeywords,
                        removeAllAbilities, hostCard.getTimestamp(), stAb.getId(), false);
            }

            // add HIDDEN keywords
            if (!addHiddenKeywords.isEmpty()) {
                affectedCard.addHiddenExtrinsicKeywords(hostCard.getTimestamp(), stAb.getId(), addHiddenKeywords);
            }

            // add SVars
            if (addSVars != null) {
                Map<String, String> map = Maps.newHashMap();
                for (final String sVar : addSVars) {
                    String actualSVar = AbilityUtils.getSVar(stAb, sVar);
                    String name = sVar;
                    if (actualSVar.startsWith("SVar:")) {
                        actualSVar = actualSVar.split("SVar:")[1];
                        name = actualSVar.split(":")[0];
                        actualSVar = actualSVar.split(":")[1];
                    }
                    map.put(name, actualSVar);
                }
                affectedCard.addChangedSVars(map, se.getTimestamp(), stAb.getId());
            }

            if (layer == StaticAbilityLayer.ABILITIES) {
                List<SpellAbility> addedAbilities = Lists.newArrayList();
                List<ReplacementEffect> addedReplacementEffects = Lists.newArrayList();
                List<Trigger> addedTrigger = Lists.newArrayList();
                List<StaticAbility> addedStaticAbility = Lists.newArrayList();
                // add abilities
                if (addAbilities != null) {
                    for (String abilty : addAbilities) {
                        if (abilty.contains("CardManaCost")) {
                            abilty = TextUtil.fastReplace(abilty, "CardManaCost", affectedCard.getManaCost().getShortString());
                        } else if (abilty.contains("ConvertedManaCost")) {
                            final String costcmc = Integer.toString(affectedCard.getCMC());
                            abilty = TextUtil.fastReplace(abilty, "ConvertedManaCost", costcmc);
                        }
                        if (abilty.startsWith("AB") || abilty.startsWith("ST")) { // grant the ability
                            addedAbilities.add(affectedCard.getSpellAbilityForStaticAbility(abilty, stAb));
                        }
                    }
                }

                if (params.containsKey("GainsAbilitiesOf") || params.containsKey("GainsAbilitiesOfDefined")) {
                    CardCollection cardsIGainedAbilitiesFrom = new CardCollection();

                    if (params.containsKey("GainsAbilitiesOf")) {
                        final String[] valids = params.get("GainsAbilitiesOf").split(",");
                        List<ZoneType> validZones;
                        if (params.containsKey("GainsAbilitiesOfZones")) {
                            validZones = ZoneType.listValueOf(params.get("GainsAbilitiesOfZones"));
                        } else {
                            validZones = ImmutableList.of(ZoneType.Battlefield);
                        }
                        cardsIGainedAbilitiesFrom.addAll(CardLists.getValidCards(game.getCardsIn(validZones), valids, hostCard.getController(), hostCard, stAb));
                    }
                    if (params.containsKey("GainsAbilitiesOfDefined")) {
                        cardsIGainedAbilitiesFrom.addAll(AbilityUtils.getDefinedCards(hostCard, params.get("GainsAbilitiesOfDefined"), stAb));
                    }

                    for (Card c : cardsIGainedAbilitiesFrom) {
                        for (SpellAbility sa : c.getSpellAbilities()) {
                            if (sa.isActivatedAbility()) {
                                if (!stAb.matchesValidParam("GainsValidAbilities", sa)) {
                                    continue;
                                }
                                SpellAbility newSA = sa.copy(affectedCard, false);
                                if (params.containsKey("GainsAbilitiesLimitPerTurn")) {
                                    newSA.setRestrictions(sa.getRestrictions());
                                    newSA.getRestrictions().setLimitToCheck(params.get("GainsAbilitiesLimitPerTurn"));
                                }
                                newSA.setOriginalAbility(sa); // need to be set to get the Once Per turn Clause correct
                                newSA.setGrantorStatic(stAb);
                                newSA.setIntrinsic(false);
                                addedAbilities.add(newSA);
                            }
                        }
                    }
                }

                // add Replacement effects
                if (addReplacements != null) {
                    for (String rep : addReplacements) {
                        addedReplacementEffects.add(affectedCard.getReplacementEffectForStaticAbility(rep, stAb));
                    }
                }

                // add triggers
                if (addTriggers != null) {
                    for (final String trigger : addTriggers) {
                        final Trigger actualTrigger = affectedCard.getTriggerForStaticAbility(trigger, stAb);
                        // if the trigger has Execute param, which most trigger gained by Static Abilties should have
                        // turn them into SpellAbility object before adding to card
                        // with that the TargetedCard does not need the Svars added to them anymore
                        // but only do it if the trigger doesn't already have a overriding ability
                        addedTrigger.add(actualTrigger);
                    }
                }

                // add static abilities
                if (addStatics != null) {
                    for (String s : addStatics) {
                        if (s.contains("ConvertedManaCost")) {
                            final String costcmc = Integer.toString(affectedCard.getCMC());
                            s = TextUtil.fastReplace(s, "ConvertedManaCost", costcmc);
                        }

                        addedStaticAbility.add(affectedCard.getStaticAbilityForStaticAbility(s, stAb));
                    }
                }

                if (!addedAbilities.isEmpty() || addReplacements != null || addTriggers != null || addStatics != null
                    || removeAllAbilities) {
                    affectedCard.addChangedCardTraits(
                        addedAbilities, null, addedTrigger, addedReplacementEffects, addedStaticAbility, removeAllAbilities, removeNonMana,
                        hostCard.getTimestamp(), stAb.getId()
                    );
                }

                if (cantHaveKeyword != null) {
                    affectedCard.addCantHaveKeyword(hostCard.getTimestamp(), cantHaveKeyword);
                }
            }

            // add Types
            if (addTypes != null || removeTypes != null || addAllCreatureTypes
                    || !remove.isEmpty()) {
                affectedCard.addChangedCardTypes(addTypes, removeTypes, addAllCreatureTypes, remove,
                        hostCard.getTimestamp(), stAb.getId(), false, stAb.hasParam("CharacteristicDefining"));
            }

            // add colors
            if (addColors != null) {
                affectedCard.addColor(addColors, !overwriteColors, hostCard.getTimestamp(), stAb.getId(), stAb.hasParam("CharacteristicDefining"));
            }

            if (layer == StaticAbilityLayer.RULES) {
                if (params.containsKey("Goad")) {
                    affectedCard.addGoad(se.getTimestamp(), hostCard.getController());
                }
                if (params.containsKey("CanBlockAny")) {
                    affectedCard.addCanBlockAny(se.getTimestamp());
                }
                if (params.containsKey("CanBlockAmount")) {
                    int v = AbilityUtils.calculateAmount(hostCard, params.get("CanBlockAmount"), stAb, true);
                    affectedCard.addCanBlockAdditional(v, se.getTimestamp());
                }
            }

            if (mayLookAt != null && (!affectedCard.getOwner().getTopXCardsFromLibrary(1).contains(affectedCard) || game.getTopLibForPlayer(affectedCard.getOwner()) == null || game.getTopLibForPlayer(affectedCard.getOwner()) == affectedCard)) {
                affectedCard.addMayLookAt(se.getTimestamp(), mayLookAt);
            }

            if (controllerMayPlay && (mayPlayLimit == null || stAb.getMayPlayTurn() < mayPlayLimit)) {
                String mayPlayAltCost = mayPlayAltManaCost;
                boolean additional = mayPlayAltCost != null && mayPlayAltCost.contains("RegularCost");

                if (mayPlayAltCost != null) {
                    if (mayPlayAltCost.contains("ConvertedManaCost")) {
                        final String costcmc = Integer.toString(affectedCard.getCMC());
                        mayPlayAltCost = mayPlayAltCost.replace("ConvertedManaCost", costcmc);
                    } else if (additional) {
                        final String regCost = affectedCard.getManaCost().getShortString();
                        mayPlayAltCost = mayPlayAltManaCost.replace("RegularCost", regCost);
                    }
                }

                Player mayPlayController = params.containsKey("MayPlayPlayer") ?
                    AbilityUtils.getDefinedPlayers(affectedCard, params.get("MayPlayPlayer"), stAb).get(0) :
                    controller;
                affectedCard.setMayPlay(mayPlayController, mayPlayWithoutManaCost,
                        mayPlayAltCost != null ? new Cost(mayPlayAltCost, false, affectedCard.equals(hostCard)) : null, additional, mayPlayWithFlash,
                        mayPlayGrantZonePermissions, stAb);

                // If the MayPlay effect only affected itself, check if it is in graveyard and give other player who cast Shaman's Trance MayPlay
                if (stAb.hasParam("Affected") && stAb.getParam("Affected").equals("Card.Self") && affectedCard.isInZone(ZoneType.Graveyard)) {
                    for (final Player p : game.getPlayers()) {
                        if (p.hasKeyword("Shaman's Trance") && mayPlayController != p) {
                            affectedCard.setMayPlay(p, mayPlayWithoutManaCost,
                                    mayPlayAltCost != null ? new Cost(mayPlayAltCost, false) : null, additional,
                                    mayPlayWithFlash, mayPlayGrantZonePermissions, stAb);
                        }
                    }
                }
            }
        }

        return affectedCards;
    }

    private static void buildIgnorEffectAbility(final StaticAbility stAb, final String costString, final List<Player> players, final CardCollectionView cards) {
        final List<Player> validActivator = new ArrayList<>(players);
        for (final Card c : cards) {
            validActivator.add(c.getController());
        }
        final Card sourceCard = stAb.getHostCard();
        Cost cost = new Cost(costString, true);
        final AbilityStatic addIgnore = new AbilityStatic(sourceCard, cost, null) {

            @Override
            public void resolve() {
                stAb.addIgnoreEffectPlayers(this.getActivatingPlayer());
                stAb.setIgnoreEffectCards(cards);
            }

            @Override
            public boolean canPlay() {
                return validActivator.contains(this.getActivatingPlayer())
                        && sourceCard.isInPlay();
            }

        };

        addIgnore.setIntrinsic(false);
        addIgnore.setApi(ApiType.InternalIgnoreEffect);
        addIgnore.setDescription(cost + " Ignore the effect until end of turn.");
        sourceCard.addChangedCardTraits(ImmutableList.of(addIgnore), null, null, null, null, false, false, sourceCard.getTimestamp(), stAb.getId());

        final GameCommand removeIgnore = new GameCommand() {
            private static final long serialVersionUID = -5415775215053216360L;
            @Override
            public void run() {
                stAb.clearIgnoreEffects();
            }
        };
        sourceCard.getGame().getEndOfTurn().addUntil(removeIgnore);
        sourceCard.addLeavesPlayCommand(removeIgnore);
    }

    private static List<Player> getAffectedPlayers(final StaticAbility stAb) {
        final Map<String, String> params = stAb.getMapParams();
        final Card hostCard = stAb.getHostCard();
        final Player controller = hostCard.getController();

        final List<Player> players = new ArrayList<>();

        if (!params.containsKey("Affected")) {
            return players;
        }

        final String[] strngs = params.get("Affected").split(",");

        for (Player p : controller.getGame().getPlayersInTurnOrder()) {
            if (p.isValid(strngs, controller, hostCard, stAb)) {
                players.add(p);
            }
        }
        players.removeAll(stAb.getIgnoreEffectPlayers());

        return players;
    }

    private static CardCollectionView getAffectedCards(final StaticAbility stAb, final CardCollectionView preList) {
        final Card hostCard = stAb.getHostCard();
        final Game game = hostCard.getGame();
        final Player controller = hostCard.getController();

        if (stAb.hasParam("CharacteristicDefining")) {
            if (stAb.hasParam("ExcludeZone")) {
                for (ZoneType zt : ZoneType.listValueOf(stAb.getParam("ExcludeZone"))) {
                    if (hostCard.isInZone(zt)) {
                        return CardCollection.EMPTY;
                    }
                }
            }
            return new CardCollection(hostCard); // will always be the card itself
        }

        // non - CharacteristicDefining
        CardCollection affectedCards = new CardCollection();

        // add preList in addition to the normal affected cards
        // need to add before game cards to have preference over them
        if (!preList.isEmpty()) {
            if (stAb.hasParam("AffectedZone")) {
                affectedCards.addAll(CardLists.filter(preList, CardPredicates.inZone(
                        ZoneType.listValueOf(stAb.getParam("AffectedZone")))));
            } else {
                affectedCards.addAll(CardLists.filter(preList, CardPredicates.inZone(ZoneType.Battlefield)));
            }
        }

        if (stAb.hasParam("AffectedZone")) {
            affectedCards.addAll(game.getCardsIn(ZoneType.listValueOf(stAb.getParam("AffectedZone"))));
        } else {
            affectedCards.addAll(game.getCardsIn(ZoneType.Battlefield));
        }
        if (stAb.hasParam("Affected")) {
            // Handle Shaman's Trance
            CardCollection affectedCardsOriginal = null;
            if (controller.hasKeyword("Shaman's Trance") && stAb.hasParam("MayPlay")) {
                affectedCardsOriginal = new CardCollection(affectedCards);
            }

            affectedCards = CardLists.getValidCards(affectedCards, stAb.getParam("Affected"), controller, hostCard, stAb);

            // Add back all cards that are in other player's graveyard, and meet the restrictions without YouOwn/YouCtrl (treat it as in your graveyard)
            if (affectedCardsOriginal != null) {
                String affectedParam = stAb.getParam("Affected");
                affectedParam = affectedParam.replaceAll("[\\.\\+]YouOwn", "");
                affectedParam = affectedParam.replaceAll("[\\.\\+]YouCtrl", "");
                String[] restrictions = affectedParam.split(",");
                for (final Card card : affectedCardsOriginal) {
                    if (card.isInZone(ZoneType.Graveyard) && card.getController() != controller && card.isValid(restrictions, controller, hostCard, stAb)) {
                        affectedCards.add(card);
                    }
                }
            }
        }

        affectedCards.removeAll(stAb.getIgnoreEffectCards());
        return affectedCards;
    }
}
