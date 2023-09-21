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
package forge.game.replacement;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import forge.game.CardTraitBase;
import forge.game.Game;
import forge.game.GameEntity;
import forge.game.GameEntityCounterTable;
import forge.game.GameLogEntryType;
import forge.game.IHasSVars;
import forge.game.ability.AbilityFactory;
import forge.game.ability.AbilityKey;
import forge.game.ability.AbilityUtils;
import forge.game.ability.ApiType;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CardCollectionView;
import forge.game.card.CardDamageMap;
import forge.game.card.CardState;
import forge.game.card.CardUtil;
import forge.game.player.Player;
import forge.game.player.PlayerCollection;
import forge.game.spellability.AbilitySub;
import forge.game.spellability.SpellAbility;
import forge.game.zone.Zone;
import forge.game.zone.ZoneType;
import forge.util.CardTranslation;
import forge.util.Localizer;
import forge.util.TextUtil;
import forge.util.Visitor;

public class ReplacementHandler {
    private final Game game;

    private Set<ReplacementEffect> hasRun = Sets.newHashSet();

    // List of all replacement effect candidates for DamageDone event, in APNAP order
    private final List<Map<ReplacementEffect, List<Map<AbilityKey, Object>>>> replaceDamageList = new ArrayList<>();

    /**
     * ReplacementHandler.
     * @param gameState
     */
    public ReplacementHandler(Game gameState) {
        game = gameState;
    }

    //private final List<ReplacementEffect> tmpEffects = new ArrayList<ReplacementEffect>();

    public List<ReplacementEffect> getReplacementList(final ReplacementType event, final Map<AbilityKey, Object> runParams, final ReplacementLayer layer) {
        final CardCollection preList = new CardCollection();
        boolean checkAgain = false;
        Card affectedLKI = null;
        Card affectedCard = null;

        if (ReplacementType.Moved.equals(event) && ZoneType.Battlefield.equals(runParams.get(AbilityKey.Destination))) {
            // if it was caused by an replacement effect, use the already calculated RE list
            // otherwise the RIOT card would cause a StackError
            final ReplacementEffect causeRE = (ReplacementEffect) runParams.get(AbilityKey.ReplacementEffect);
            if (causeRE != null) {
                // only return for same layer
                if (ReplacementType.Moved.equals(causeRE.getMode()) && layer.equals(causeRE.getLayer())) {
                    if (!causeRE.getOtherChoices().isEmpty())
                        return causeRE.getOtherChoices();
                }
            }

            // Rule 614.12 Enter the Battlefield Replacement Effects look at what the card would be on the battlefield
            affectedCard = (Card) runParams.get(AbilityKey.Affected);
            affectedLKI = CardUtil.getLKICopy(affectedCard);
            affectedLKI.setLastKnownZone(affectedCard.getController().getZone(ZoneType.Battlefield));

            // need to apply Counters to check its future state on the battlefield
            affectedLKI.putEtbCounters(null);
            preList.add(affectedLKI);
            game.getAction().checkStaticAbilities(false, Sets.newHashSet(affectedLKI), preList);
            checkAgain = true;

            runParams.put(AbilityKey.Affected, affectedLKI);
        }

        final List<ReplacementEffect> possibleReplacers = Lists.newArrayList();
        // Round up Non-static replacement effects ("Until EOT," or
        // "The next time you would..." etc)
        /*for (final ReplacementEffect replacementEffect : this.tmpEffects) {
            if (!replacementEffect.hasRun() && replacementEffect.canReplace(runParams) && replacementEffect.getLayer() == layer) {
                possibleReplacers.add(replacementEffect);
            }
        }*/

        // Round up Static replacement effects
        game.forEachCardInGame(new Visitor<Card>() {
            @Override
            public boolean visit(Card crd) {
                Card c = preList.get(crd);
                Zone cardZone = game.getZoneOf(c);
                Zone lkiZone = game.getChangeZoneLKIInfo(c).getLastKnownZone();

                // only when not prelist
                boolean noLKIstate = c != crd || event != ReplacementType.Moved;
                // might be inbound token
                noLKIstate |= lkiZone == null || !lkiZone.is(ZoneType.Battlefield);
                noLKIstate |= !runParams.containsKey(AbilityKey.LastStateBattlefield) || runParams.get(AbilityKey.LastStateBattlefield) == null;
                if (!noLKIstate) {
                    Card lastState = ((CardCollectionView) runParams.get(AbilityKey.LastStateBattlefield)).get(crd);
                    // no LKI found for this card so it shouldn't apply, this can happen during simultaneous zone changes
                    if (lastState == crd) {
                        return true;
                    }
                    // use the LKI because it has the right RE from the state before the effect started
                    c = lastState;
                    cardZone = lkiZone;
                }

                for (final ReplacementEffect replacementEffect : c.getReplacementEffects()) {
                    if (!replacementEffect.hasRun() && !hasRun.contains(replacementEffect)
                            && (layer == null || replacementEffect.getLayer() == layer)
                            && event.equals(replacementEffect.getMode())
                            && !possibleReplacers.contains(replacementEffect)
                            && replacementEffect.zonesCheck(cardZone)
                            && replacementEffect.requirementsCheck(game)
                            && replacementEffect.canReplace(runParams)) {
                        possibleReplacers.add(replacementEffect);
                    }
                }
                return true;
            }

        });

        if (checkAgain) {
            if (affectedLKI != null && affectedCard != null) {
                // need to set the Host Card there so it is not connected to LKI anymore?
                // need to be done after canReplace check
                for (final ReplacementEffect re : affectedLKI.getReplacementEffects()) {
                    re.setHostCard(affectedCard);
                }
                // need to copy stored keywords from lki into real object to prevent the replacement effect from making new ones
                affectedCard.setStoredKeywords(affectedLKI.getStoredKeywords(), true);
                affectedCard.setStoredReplacements(affectedLKI.getStoredReplacements());
                if (affectedCard.getCastSA() != null && affectedCard.getCastSA().getKeyword() != null) {
                   // need to readd the CastSA Keyword into the Card
                   affectedCard.addKeywordForStaticAbility(affectedCard.getCastSA().getKeyword());
                }
                runParams.put(AbilityKey.Affected, affectedCard);
                runParams.put(AbilityKey.NewCard, CardUtil.getLKICopy(affectedLKI));
            }
            game.getAction().checkStaticAbilities(false);
        }

        return possibleReplacers;
    }

    /**
     *
     * Runs any applicable replacement effects.
     *
     * @param runParams
     *            the run params,same as for triggers.
     * @return ReplacementResult, an enum that represents what happened to the replacement effect.
     */
    public ReplacementResult run(ReplacementType event, final Map<AbilityKey, Object> runParams) {
        final Object affected = runParams.get(AbilityKey.Affected);
        Player decider = null;

        // Figure out who decides which of multiple replacements to apply
        // as well as whether or not to apply optional replacements.
        if (affected instanceof Player) {
            decider = (Player) affected;
        } else {
            decider = ((Card) affected).getController();
        }

        // try out all layer
        for (ReplacementLayer layer : ReplacementLayer.values()) {
            ReplacementResult res = run(event, runParams, layer, decider);
            if (res != ReplacementResult.NotReplaced) {
                return res;
            }
        }

        return ReplacementResult.NotReplaced;
    }

    private ReplacementResult run(final ReplacementType event, final Map<AbilityKey, Object> runParams, final ReplacementLayer layer, final Player decider) {
        final List<ReplacementEffect> possibleReplacers = getReplacementList(event, runParams, layer);

        if (possibleReplacers.isEmpty()) {
            return ReplacementResult.NotReplaced;
        }

        ReplacementEffect chosenRE;
        // "can't" is never a choice
        if (layer == ReplacementLayer.CantHappen) {
            chosenRE = possibleReplacers.get(0);
        } else {
            chosenRE = decider.getController().chooseSingleReplacementEffect(Localizer.getInstance().getMessage("lblChooseFirstApplyReplacementEffect"), possibleReplacers);
        }

        possibleReplacers.remove(chosenRE);

        chosenRE.setHasRun(true);
        hasRun.add(chosenRE);
        chosenRE.setOtherChoices(possibleReplacers);
        ReplacementResult res = executeReplacement(runParams, chosenRE, decider);
        if (res == ReplacementResult.NotReplaced) {
            if (!possibleReplacers.isEmpty()) {
                res = run(event, runParams);
            }
            chosenRE.setHasRun(false);
            hasRun.remove(chosenRE);
            chosenRE.setOtherChoices(null);
            return res;
        }

        // Log there
        String message = chosenRE.getDescription();
        if (!StringUtils.isEmpty(message)) {
            game.getGameLog().add(GameLogEntryType.EFFECT_REPLACED, message);
        }

        // if its updated, try to call event again
        if (res == ReplacementResult.Updated) {
            Map<AbilityKey, Object> params = AbilityKey.newMap(runParams);

            if (params.containsKey(AbilityKey.EffectOnly)) {
                params.put(AbilityKey.EffectOnly, true);
            }
            ReplacementResult result = run(event, params);
            switch (result) {
            case NotReplaced:
            case Updated: {
                runParams.putAll(params);
                // effect was updated
                runParams.put(AbilityKey.ReplacementResult, ReplacementResult.Updated);
                break;
            }
            default:
                // effect was replaced with something else
                res = result;
                runParams.put(AbilityKey.ReplacementResult, result);
                break;
            }
        }

        chosenRE.setHasRun(false);
        hasRun.remove(chosenRE);
        chosenRE.setOtherChoices(null);

        return res;
    }

    /**
     *
     * Runs a single replacement effect.
     *
     * @param replacementEffect
     *            the replacement effect to run
     */
    private ReplacementResult executeReplacement(final Map<AbilityKey, Object> runParams,
        final ReplacementEffect replacementEffect, final Player decider) {
        SpellAbility effectSA = null;

        Card host = replacementEffect.getHostCard();
        // AlternateState for OriginsPlaneswalker
        // FaceDown for cards like Necropotence
        if (host.hasAlternateState() || host.isFaceDown()) {
            host = game.getCardState(host);
        }

        if (replacementEffect.getOverridingAbility() == null && replacementEffect.hasParam("ReplaceWith")) {
            // TODO: the source of replacement effect should be the source of the original effect
            effectSA = AbilityFactory.getAbility(host, replacementEffect.getParam("ReplaceWith"), replacementEffect);
            //replacementEffect.setOverridingAbility(effectSA);
            //effectSA.setTrigger(true);
        } else if (replacementEffect.getOverridingAbility() != null) {
            effectSA = replacementEffect.getOverridingAbility();
        }

        if (effectSA != null) {
            SpellAbility tailend = effectSA;
            do {
                replacementEffect.setReplacingObjects(runParams, tailend);
                //set original Params to update them later
                tailend.setReplacingObject(AbilityKey.OriginalParams, runParams);
                tailend.setReplacingObjectsFrom(runParams, AbilityKey.SimultaneousETB);
                tailend = tailend.getSubAbility();
            } while(tailend != null);

            effectSA.setLastStateBattlefield((CardCollectionView) ObjectUtils.firstNonNull(runParams.get(AbilityKey.LastStateBattlefield), game.getLastStateBattlefield()));
            effectSA.setLastStateGraveyard((CardCollectionView) ObjectUtils.firstNonNull(runParams.get(AbilityKey.LastStateGraveyard), game.getLastStateGraveyard()));
            if (replacementEffect.isIntrinsic()) {
                effectSA.setIntrinsic(true);
                effectSA.changeText();
            }
            effectSA.setReplacementEffect(replacementEffect);
        }

        // Decider gets to choose whether or not to apply the replacement.
        if (replacementEffect.hasParam("Optional")) {
            Player optDecider = decider;
            if (replacementEffect.hasParam("OptionalDecider") && effectSA != null) {
                effectSA.setActivatingPlayer(host.getController());
                optDecider = AbilityUtils.getDefinedPlayers(host,
                        replacementEffect.getParam("OptionalDecider"), effectSA).get(0);
            }

            String name = CardTranslation.getTranslatedName(host.getCardForUi().getName());
            String effectDesc = TextUtil.fastReplace(replacementEffect.getDescription(), "CARDNAME", name);
            final String question = replacementEffect instanceof ReplaceDiscard
                ? Localizer.getInstance().getMessage("lblApplyCardReplacementEffectToCardConfirm", name, runParams.get(AbilityKey.Card).toString(), effectDesc)
                : Localizer.getInstance().getMessage("lblApplyReplacementEffectOfCardConfirm", name, effectDesc);
            GameEntity affected = (GameEntity) runParams.get(AbilityKey.Affected);
            boolean confirmed = optDecider.getController().confirmReplacementEffect(replacementEffect, effectSA, affected, question);
            if (!confirmed) {
                return ReplacementResult.NotReplaced;
            }
        }

        boolean isPrevent = "True".equals(replacementEffect.getParam("Prevent"));
        if (isPrevent || replacementEffect.hasParam("PreventionEffect")) {
            if (Boolean.TRUE.equals(runParams.get(AbilityKey.NoPreventDamage))) {
                // If can't prevent damage, result is not replaced
                // But still put "prevented" amount for buffered SA
                if (replacementEffect.hasParam("AlwaysReplace")) {
                    runParams.put(AbilityKey.PreventedAmount, runParams.get(AbilityKey.DamageAmount));
                } else {
                    runParams.put(AbilityKey.PreventedAmount, 0);
                }
                return ReplacementResult.NotReplaced;
            }
            if (isPrevent) {
                return ReplacementResult.Prevented; // Nothing should replace the event.
            }
        }

        if ("True".equals(replacementEffect.getParam("Skip"))) {
            return ReplacementResult.Skipped; // Event is skipped.
        }
        Player player = host.getController();

        if (effectSA != null) {
            ApiType apiType = effectSA.getApi();
            if (replacementEffect.getMode() != ReplacementType.DamageDone ||
                (apiType == ApiType.ReplaceDamage || apiType == ApiType.ReplaceSplitDamage || apiType == ApiType.ReplaceEffect)) {
                player.getController().playSpellAbilityNoStack(effectSA, true);
            } else {
                // The SA if buffered, but replacement result should be set to Replaced
                runParams.put(AbilityKey.ReplacementResult, ReplacementResult.Replaced);
            }

            // these ones are special for updating
            if (apiType == ApiType.ReplaceToken || apiType == ApiType.ReplaceEffect || apiType == ApiType.ReplaceMana) {
                runParams.put(AbilityKey.ReplacementResult, ReplacementResult.Updated);
            }
        }

        if (replacementEffect.hasParam("ReplacementResult")) {
            return ReplacementResult.valueOf(replacementEffect.getParam("ReplacementResult")); // Event is replaced without SA.
        }

        // if the spellability is a replace effect then its some new logic
        // if ReplacementResult is set in run params use that instead
        if (runParams.containsKey(AbilityKey.ReplacementResult)) {
            return (ReplacementResult) runParams.get(AbilityKey.ReplacementResult);
        }

        return ReplacementResult.Replaced;
    }

    private void getPossibleReplaceDamageList(PlayerCollection players, final boolean isCombat, final CardDamageMap damageMap, final SpellAbility cause) {
        for (Map.Entry<GameEntity, Map<Card, Integer>> et : damageMap.columnMap().entrySet()) {
            final GameEntity target = et.getKey();
            int playerIndex = target instanceof Player ? players.indexOf(((Player) target)) :
                                players.indexOf(((Card) target).getController());
            if (playerIndex == -1) continue;
            Map<ReplacementEffect, List<Map<AbilityKey, Object>>> replaceCandidateMap = replaceDamageList.get(playerIndex);
            for (Map.Entry<Card, Integer> e : et.getValue().entrySet()) {
                Card source = e.getKey();
                Integer damage = e.getValue();
                if (damage > 0) {
                    boolean prevention = source.canDamagePrevented(isCombat) &&
                                            (cause == null || !cause.hasParam("NoPrevention"));
                    final Map<AbilityKey, Object> repParams = AbilityKey.mapFromAffected(target);
                    repParams.put(AbilityKey.DamageSource, source);
                    repParams.put(AbilityKey.DamageAmount, damage);
                    repParams.put(AbilityKey.IsCombat, isCombat);
                    repParams.put(AbilityKey.NoPreventDamage, !prevention);
                    if (cause != null) {
                        repParams.put(AbilityKey.Cause, cause);
                    }

                    List<ReplacementEffect> reList = getReplacementList(ReplacementType.DamageDone, repParams, ReplacementLayer.Other);
                    for (ReplacementEffect re : reList) {
                        if (!replaceCandidateMap.containsKey(re)) {
                            replaceCandidateMap.put(re, new ArrayList<>());
                        }
                        List<Map<AbilityKey, Object>> runParamList = replaceCandidateMap.get(re);
                        runParamList.add(repParams);
                    }
                }
            }
        }
    }

    private void runSingleReplaceDamageEffect(ReplacementEffect re, Map<AbilityKey, Object> runParams, Map<ReplacementEffect, List<Map<AbilityKey, Object>>> replaceCandidateMap,
            Map<ReplacementEffect, List<Map<AbilityKey, Object>>> executedDamageMap, Player decider, final CardDamageMap damageMap, final CardDamageMap preventMap) {
        List<Map<AbilityKey, Object>> executedParamList = executedDamageMap.get(re);
        ApiType apiType = re.getOverridingAbility() != null ? re.getOverridingAbility().getApi() : null;
        Card source = (Card) runParams.get(AbilityKey.DamageSource);
        GameEntity target = (GameEntity) runParams.get(AbilityKey.Affected);
        int damage = (int) runParams.get(AbilityKey.DamageAmount);
        Map<String, String> mapParams = re.getMapParams();

        ReplacementResult res = executeReplacement(runParams, re, decider);
        GameEntity newTarget = (GameEntity) runParams.get(AbilityKey.Affected);
        int newDamage = (int) runParams.get(AbilityKey.DamageAmount);

        // ReplaceSplitDamage will split the damage event into two event, so need to create run params for old event
        // (original run params is changed for new event)
        Map<AbilityKey, Object> oldParams = null;

        if (res != ReplacementResult.NotReplaced) {
            // Remove this event from other possible replacers
            Iterator<Map.Entry<ReplacementEffect, List<Map<AbilityKey, Object>>>> itr = replaceCandidateMap.entrySet().iterator();
            while (itr.hasNext()) {
                Map.Entry<ReplacementEffect, List<Map<AbilityKey, Object>>> entry = itr.next();
                if (entry.getKey() == re) continue;
                if (entry.getValue().contains(runParams)) {
                    entry.getValue().remove(runParams);
                    if (entry.getValue().isEmpty()) {
                        itr.remove();
                    }
                }
            }
            // Add updated event to possible replacers
            if (res == ReplacementResult.Updated || apiType == ApiType.ReplaceSplitDamage) {
                Map<ReplacementEffect, List<Map<AbilityKey, Object>>> newReplaceCandidateMap = replaceCandidateMap;
                if (!target.equals(newTarget)) {
                    PlayerCollection players = game.getPlayersInTurnOrder();
                    int playerIndex = newTarget instanceof Player ? players.indexOf(((Player) newTarget)) :
                                       players.indexOf(((Card) newTarget).getController());
                    newReplaceCandidateMap = replaceDamageList.get(playerIndex);
                }

                List<ReplacementEffect> reList = getReplacementList(ReplacementType.DamageDone, runParams, ReplacementLayer.Other);
                for (ReplacementEffect newRE : reList) {
                    // Skip if this has already been executed by given replacement effect
                    if (executedDamageMap.containsKey(newRE) && executedDamageMap.get(newRE).contains(runParams)) {
                        continue;
                    }
                    if (!newReplaceCandidateMap.containsKey(newRE)) {
                        newReplaceCandidateMap.put(newRE, new ArrayList<>());
                    }
                    List<Map<AbilityKey, Object>> runParamList = newReplaceCandidateMap.get(newRE);
                    runParamList.add(runParams);
                }
            }
            // Add old updated event too for ReplaceSplitDamage
            if (apiType == ApiType.ReplaceSplitDamage && res == ReplacementResult.Updated) {
                oldParams = AbilityKey.newMap(runParams);
                oldParams.put(AbilityKey.Affected, target);
                oldParams.put(AbilityKey.DamageAmount, damage - newDamage);
                List<ReplacementEffect> reList = getReplacementList(ReplacementType.DamageDone, oldParams, ReplacementLayer.Other);
                for (ReplacementEffect newRE : reList) {
                    if (!replaceCandidateMap.containsKey(newRE)) {
                        replaceCandidateMap.put(newRE, new ArrayList<>());
                    }
                    List<Map<AbilityKey, Object>> runParamList = replaceCandidateMap.get(newRE);
                    runParamList.add(oldParams);
                }
            }
        }

        @SuppressWarnings("unchecked")
        Map<ReplacementEffect, ReplacementResult> resultMap = (Map<ReplacementEffect, ReplacementResult>) runParams.get(AbilityKey.ReplacementResultMap);
        resultMap.put(re, res);

        // Update damage map and prevent map
        switch (res) {
        case NotReplaced:
            break;
        case Updated:
            // check if this is still the affected card or player
            if (target.equals(newTarget)) {
                damageMap.put(source, target, newDamage - damage);
            } else if (apiType == ApiType.ReplaceSplitDamage) {
                damageMap.put(source, target, -newDamage);
            }
            if (!target.equals(newTarget)) {
                if (apiType != ApiType.ReplaceSplitDamage) {
                    damageMap.remove(source, target);
                }
                damageMap.put(source, newTarget, newDamage);
            }
            if (apiType == ApiType.ReplaceDamage) {
                preventMap.put(source, target, damage - newDamage);
                // Record prevented amount
                runParams.put(AbilityKey.PreventedAmount, damage - newDamage);
            }
            break;
        default:
            damageMap.remove(source, target);
            if (apiType == ApiType.ReplaceDamage ||
                    (mapParams.containsKey("Prevent") && mapParams.get("Prevent").equals("True")) ||
                    mapParams.containsKey("PreventionEffect")) {
                preventMap.put(source, target, damage);
                // Record prevented amount
                runParams.put(AbilityKey.PreventedAmount, damage);
            }
            if (apiType == ApiType.ReplaceSplitDamage) {
                damageMap.put(source, newTarget, newDamage);
            }
        }

        // Put run params into executed param list so this replacement effect won't handle them again
        // (For example, if the damage is redirected back)
        executedParamList.add(runParams);
        if (apiType == ApiType.ReplaceSplitDamage) {
            executedParamList.add(oldParams);
        }

        // Log the replacement effect
        if (res != ReplacementResult.NotReplaced) {
            String message = re.getDescription();
            if ( !StringUtils.isEmpty(message)) {
                if (re.getHostCard() != null) {
                    message = TextUtil.fastReplace(message, "CARDNAME", re.getHostCard().getName());
                }
                game.getGameLog().add(GameLogEntryType.EFFECT_REPLACED, message);
            }
        }
    }

    private void executeReplaceDamageBufferedSA(Map<ReplacementEffect, List<Map<AbilityKey, Object>>> executedDamageMap) {
        for (Map.Entry<ReplacementEffect, List<Map<AbilityKey, Object>>> entry : executedDamageMap.entrySet()) {
            ReplacementEffect re = entry.getKey();
            if (re.getOverridingAbility() == null) {
                continue;
            }
            SpellAbility bufferedSA = re.getOverridingAbility();
            ApiType apiType = bufferedSA.getApi();
            if (apiType == ApiType.ReplaceDamage || apiType == ApiType.ReplaceSplitDamage || apiType == ApiType.ReplaceEffect) {
                bufferedSA = bufferedSA.getSubAbility();
                if (bufferedSA == null) {
                    continue;
                }
            }

            List<Map<AbilityKey, Object>> executedParamList = entry.getValue();
            if (executedParamList.isEmpty()) {
                continue;
            }

            Map<String, String> mapParams = re.getMapParams();
            boolean isPrevention = (mapParams.containsKey("Prevent") && mapParams.get("Prevent").equals("True")) || mapParams.containsKey("PreventionEffect");
            boolean executePerSource = mapParams.containsKey("ExecuteMode") && mapParams.get("ExecuteMode").equals("PerSource");
            boolean executePerTarget = mapParams.containsKey("ExecuteMode") && mapParams.get("ExecuteMode").equals("PerTarget");

            while (!executedParamList.isEmpty()) {
                Map<AbilityKey, Object> runParams = AbilityKey.newMap();
                List<Card> damageSourceList = new ArrayList<>();
                List<GameEntity> affectedList = new ArrayList<>();
                int damageSum = 0;

                Iterator<Map<AbilityKey, Object>> itr = executedParamList.iterator();
                while (itr.hasNext()) {
                    Map<AbilityKey, Object> executedParams = itr.next();

                    @SuppressWarnings("unchecked")
                    Map<ReplacementEffect, ReplacementResult> resultMap = (Map<ReplacementEffect, ReplacementResult>) executedParams.get(AbilityKey.ReplacementResultMap);
                    ReplacementResult res = resultMap.get(re);
                    if (res == ReplacementResult.NotReplaced && (!isPrevention || Boolean.FALSE.equals(executedParams.get(AbilityKey.NoPreventDamage)))) {
                        itr.remove();
                        continue;
                    }

                    Card source = (Card) executedParams.get(AbilityKey.DamageSource);
                    if (executePerSource && !damageSourceList.isEmpty() && !damageSourceList.contains(source)) {
                        continue;
                    }

                    GameEntity target = (GameEntity) executedParams.get(AbilityKey.Affected);
                    if (executePerTarget && !affectedList.isEmpty() && !affectedList.contains(target)) {
                        continue;
                    }

                    itr.remove();
                    int damage = (int) executedParams.get(isPrevention ? AbilityKey.PreventedAmount : AbilityKey.DamageAmount);
                    if (!damageSourceList.contains(source)) {
                        damageSourceList.add(source);
                    }
                    if (!affectedList.contains(target)) {
                        affectedList.add(target);
                    }
                    damageSum += damage;
                }

                if (damageSum > 0) {
                    runParams.put(AbilityKey.DamageSource, damageSourceList.size() > 1 ? damageSourceList : damageSourceList.get(0));
                    runParams.put(AbilityKey.Affected, affectedList.size() > 1 ? affectedList : affectedList.get(0));
                    runParams.put(AbilityKey.DamageAmount, damageSum);

                    re.setReplacingObjects(runParams, re.getOverridingAbility());
                    bufferedSA.setActivatingPlayer(re.getHostCard().getController());
                    AbilityUtils.resolve(bufferedSA);
                }
            }
        }
    }

    public void runReplaceDamage(final boolean isCombat, final CardDamageMap damageMap, final CardDamageMap preventMap,
            final GameEntityCounterTable counterTable, final SpellAbility cause) {
        PlayerCollection players = game.getPlayersInTurnOrder();
        for (int i = 0; i < players.size(); i++) {
            replaceDamageList.add(new HashMap<>());
        }

        // Map of all executed replacement effect for DamageDone event, including run params
        Map<ReplacementEffect, List<Map<AbilityKey, Object>>> executedDamageMap = new HashMap<>();

        // First, gather all possible replacement effects
        getPossibleReplaceDamageList(players, isCombat, damageMap, cause);

        // Next, handle replacement effects in APNAP order
        // Handle "Prevented this way" and abilities like "Phantom Nomad", by buffer the replaced SA
        // and only run them after all prevention and redirection effects are processed.
        while (true) {
            Player decider = null;
            Map<ReplacementEffect, List<Map<AbilityKey, Object>>> replaceCandidateMap = null;
            for (int i = 0; i < players.size(); i++) {
                if (replaceDamageList.get(i).isEmpty()) continue;
                decider = players.get(i);
                replaceCandidateMap = replaceDamageList.get(i);
                break;
            }
            if (replaceCandidateMap == null) {
                break;
            }

            List<ReplacementEffect> possibleReplacers = new ArrayList<>(replaceCandidateMap.keySet());
            ReplacementEffect chosenRE = decider.getController().chooseSingleReplacementEffect(Localizer.getInstance().getMessage("lblChooseFirstApplyReplacementEffect"), possibleReplacers);
            List<Map<AbilityKey, Object>> runParamList = replaceCandidateMap.get(chosenRE);

            if (!executedDamageMap.containsKey(chosenRE)) {
                executedDamageMap.put(chosenRE, new ArrayList<>());
            }

            // Run all possible events for chosen replacement effect
            chosenRE.setHasRun(true);
            SpellAbility effectSA = chosenRE.getOverridingAbility();
            ApiType apiType = null;
            SpellAbility bufferedSA = effectSA;
            boolean needRestoreSubSA = false;
            boolean needDivideShield = false;
            boolean needChooseSource = false;
            int shieldAmount = 0;
            if (effectSA != null) {
                apiType = effectSA.getApi();
                // Temporary remove sub ability from ReplaceDamage, ReplaceSplitDamage and ReplaceEffect API so they could be run later
                if (apiType == ApiType.ReplaceDamage || apiType == ApiType.ReplaceSplitDamage || apiType == ApiType.ReplaceEffect) {
                    bufferedSA = effectSA.getSubAbility();
                    if (bufferedSA != null) {
                        needRestoreSubSA = true;
                        effectSA.setSubAbility(null);
                    }
                }

                // Determine if need to divide shield among affected entity and
                // determine if the prevent next N damage shield is large enough to replace all damage
                Map<String, String> mapParams = chosenRE.getMapParams();
                if ((mapParams.containsKey("PreventionEffect") && mapParams.get("PreventionEffect").equals("NextN"))
                        || apiType == ApiType.ReplaceSplitDamage) {
                    if (apiType == ApiType.ReplaceDamage) {
                        shieldAmount = AbilityUtils.calculateAmount(effectSA.getHostCard(), effectSA.getParamOrDefault("Amount", "1"), effectSA);
                    } else if (apiType == ApiType.ReplaceSplitDamage) {
                        shieldAmount = AbilityUtils.calculateAmount(effectSA.getHostCard(), effectSA.getParamOrDefault("VarName", "1"), effectSA);
                    }
                    int damageAmount = 0;
                    boolean hasMultipleSource = false;
                    boolean hasMultipleTarget = false;
                    Card firstSource = null;
                    GameEntity firstTarget = null;
                    for (Map<AbilityKey, Object> runParams : runParamList) {
                        // Only count damage that can be prevented
                        if (apiType == ApiType.ReplaceDamage && Boolean.TRUE.equals(runParams.get(AbilityKey.NoPreventDamage))) continue;
                        damageAmount += (int) runParams.get(AbilityKey.DamageAmount);
                        if (firstSource == null) {
                            firstSource = (Card) runParams.get(AbilityKey.DamageSource);
                        } else if (!firstSource.equals(runParams.get(AbilityKey.DamageSource))) {
                            hasMultipleSource = true;
                        }
                        if (firstTarget == null) {
                            firstTarget = (GameEntity) runParams.get(AbilityKey.Affected);
                        } else if (!firstTarget.equals(runParams.get(AbilityKey.Affected))) {
                            hasMultipleTarget = true;
                        }
                    }
                    if (damageAmount > shieldAmount && runParamList.size() > 1) {
                        if (hasMultipleSource)
                            needChooseSource = true;
                        if (effectSA.hasParam("DivideShield") && hasMultipleTarget)
                            needDivideShield = true;
                    }
                }
            }

            // Ask the decider to divide shield among affected damage target
            Map<GameEntity, Integer> shieldMap = null;
            if (needDivideShield) {
                Map<GameEntity, Integer> affected = new HashMap<>();
                for (Map<AbilityKey, Object> runParams : runParamList) {
                    GameEntity target = (GameEntity) runParams.get(AbilityKey.Affected);
                    Integer damage = (Integer) runParams.get(AbilityKey.DamageAmount);
                    if (!affected.containsKey(target)) {
                        affected.put(target, damage);
                    } else {
                        affected.put(target, damage + affected.get(target));
                    }
                }
                shieldMap = decider.getController().divideShield(chosenRE.getHostCard(), affected, shieldAmount);
            }

            // CR 615.7
            // If damage would be dealt to the shielded permanent or player by two or more applicable sources at the same time,
            // the player or the controller of the permanent chooses which damage the shield prevents.
            if (needChooseSource) {
                CardCollection sourcesToChooseFrom = new CardCollection();
                for (Map<AbilityKey, Object> runParams : runParamList) {
                    if (apiType == ApiType.ReplaceDamage && Boolean.TRUE.equals(runParams.get(AbilityKey.NoPreventDamage))) continue;
                    sourcesToChooseFrom.add((Card) runParams.get(AbilityKey.DamageSource));
                }
                final String choiceTitle = Localizer.getInstance().getMessage("lblChooseSource") + " ";
                while (shieldAmount > 0 && !sourcesToChooseFrom.isEmpty()) {
                    Card source = decider.getController().chooseSingleEntityForEffect(sourcesToChooseFrom, effectSA, choiceTitle, null);
                    sourcesToChooseFrom.remove(source);
                    Iterator<Map<AbilityKey, Object>> itr = runParamList.iterator();
                    while (itr.hasNext()) {
                        Map<AbilityKey, Object> runParams = itr.next();
                        if (source.equals(runParams.get(AbilityKey.DamageSource))) {
                            itr.remove();
                            if (shieldMap != null) {
                                GameEntity target = (GameEntity) runParams.get(AbilityKey.Affected);
                                if (shieldMap.containsKey(target) && shieldMap.get(target) > 0) {
                                    Integer dividedShieldAmount = shieldMap.get(target);
                                    runParams.put(AbilityKey.DividedShieldAmount, dividedShieldAmount);
                                    shieldAmount -= (int) dividedShieldAmount;
                                } else {
                                    continue;
                                }
                            } else {
                                shieldAmount -= (int) runParams.get(AbilityKey.DamageAmount);
                            }
                            if (!runParams.containsKey(AbilityKey.ReplacementResultMap)) {
                                Map<ReplacementEffect, ReplacementResult> resultMap = new HashMap<>();
                                runParams.put(AbilityKey.ReplacementResultMap, resultMap);
                            }
                            runSingleReplaceDamageEffect(chosenRE, runParams, replaceCandidateMap, executedDamageMap, decider, damageMap, preventMap);
                        }
                    }
                }
            } else {
                for (Map<AbilityKey, Object> runParams : runParamList) {
                    if (shieldMap != null) {
                        GameEntity target = (GameEntity) runParams.get(AbilityKey.Affected);
                        if (shieldMap.containsKey(target) && shieldMap.get(target) > 0) {
                            Integer dividedShieldAmount = shieldMap.get(target);
                            runParams.put(AbilityKey.DividedShieldAmount, dividedShieldAmount);
                        } else {
                            continue;
                        }
                    }
                    if (!runParams.containsKey(AbilityKey.ReplacementResultMap)) {
                        Map<ReplacementEffect, ReplacementResult> resultMap = new HashMap<>();
                        runParams.put(AbilityKey.ReplacementResultMap, resultMap);
                    }
                    runSingleReplaceDamageEffect(chosenRE, runParams, replaceCandidateMap, executedDamageMap, decider, damageMap, preventMap);
                }
            }

            // Restore temporary removed SA
            if (needRestoreSubSA) {
                effectSA.setSubAbility((AbilitySub)bufferedSA);
            }
            chosenRE.setHasRun(false);
            replaceCandidateMap.remove(chosenRE);
        }

        replaceDamageList.clear();

        // Finally, run all buffered SA to finish the replacement processing
        executeReplaceDamageBufferedSA(executedDamageMap);
    }

    /**
     *
     * Creates an instance of the proper replacement effect object based on raw
     * script.
     *
     * @param repParse
     *            A raw line of script
     * @param host
     *            The cards that hosts the replacement effect.
     * @return A finished instance
     */
    public static ReplacementEffect parseReplacement(final String repParse, final Card host, final boolean intrinsic) {
        return parseReplacement(repParse, host, intrinsic, host);
    }
    public static ReplacementEffect parseReplacement(final String repParse, final Card host, final boolean intrinsic, final IHasSVars sVarHolder) {
        return ReplacementHandler.parseReplacement(AbilityFactory.getMapParams(repParse), host, intrinsic, sVarHolder);
    }

    /**
     *
     * Creates an instance of the proper replacement effect object based on a
     * parsed script.
     *
     * @param mapParams
     *            The parsed script
     * @param host
     *            The card that hosts the replacement effect
     * @return The finished instance
     */
    private static ReplacementEffect parseReplacement(final Map<String, String> mapParams, final Card host, final boolean intrinsic, final IHasSVars sVarHolder) {
        final ReplacementType rt = ReplacementType.smartValueOf(mapParams.get("Event"));
        ReplacementEffect ret = rt.createReplacement(mapParams, host, intrinsic);

        String activeZones = mapParams.get("ActiveZones");
        if (null != activeZones) {
            ret.setActiveZone(EnumSet.copyOf(ZoneType.listValueOf(activeZones)));
        }

        if (mapParams.containsKey("ReplaceWith") && sVarHolder != null) {
            ret.setOverridingAbility(AbilityFactory.getAbility(host, mapParams.get("ReplaceWith"), sVarHolder));
        }

        if (sVarHolder instanceof CardState) {
            ret.setCardState((CardState)sVarHolder);
        } else if (sVarHolder instanceof CardTraitBase) {
            ret.setCardState(((CardTraitBase)sVarHolder).getCardState());
        }
        return ret;
    }

    /**
     * Helper function to check if a phase would be skipped for AI.
     */
    public boolean wouldPhaseBeSkipped(final Player player, final String phase) {
        final Map<AbilityKey, Object> repParams = AbilityKey.mapFromAffected(player);
        repParams.put(AbilityKey.Phase, phase);
        List<ReplacementEffect> list = getReplacementList(ReplacementType.BeginPhase, repParams, ReplacementLayer.Control);
        if (list.isEmpty()) {
            return false;
        }
        return true;
    }
    /**
     * Helper function to check if an extra turn would be skipped for AI.
     */
    public boolean wouldExtraTurnBeSkipped(final Player player) {
        final Map<AbilityKey, Object> repParams = AbilityKey.mapFromAffected(player);
        repParams.put(AbilityKey.ExtraTurn, true);
        List<ReplacementEffect> list = getReplacementList(ReplacementType.BeginTurn, repParams, ReplacementLayer.Other);
        if (list.isEmpty()) {
            return false;
        }
        return true;
    }

    /**
     * Helper function to get total prevention shield amount (limited to "prevent next N damage effects")
     * @param o Affected game entity object
     * @return total shield amount
     */
    public int getTotalPreventionShieldAmount(GameEntity o) {
        final List<ReplacementEffect> list = Lists.newArrayList();
        game.forEachCardInGame(new Visitor<Card>() {
            @Override
            public boolean visit(Card c) {
                for (final ReplacementEffect re : c.getReplacementEffects()) {
                    if (re.getMode() == ReplacementType.DamageDone
                            && re.getLayer() == ReplacementLayer.Other
                            && re.hasParam("PreventionEffect")
                            && re.zonesCheck(game.getZoneOf(c))
                            && re.getOverridingAbility() != null
                            && re.getOverridingAbility().getApi() == ApiType.ReplaceDamage
                            && re.matchesValidParam("ValidTarget", o)) {
                        list.add(re);
                    }
                }
                return true;
            }

        });

        int totalAmount = 0;
        for (ReplacementEffect re : list) {
            SpellAbility sa = re.getOverridingAbility();
            if (sa.hasParam("Amount")) {
                String varValue = sa.getParam("Amount");
                if (StringUtils.isNumeric(varValue)) {
                    totalAmount += Integer.parseInt(varValue);
                } else {
                    varValue = sa.getSVar(varValue);
                    if (varValue.startsWith("Number$")) {
                        totalAmount += Integer.parseInt(varValue.substring(7));
                    }
                }
            }
        }
        return totalAmount;
    }

    /**
     * Helper function to check if combat damage is prevented this turn (fog effect)
     * @return true if there is some resolved fog effect
     */
    public final boolean isPreventCombatDamageThisTurn() {
        final List<ReplacementEffect> list = Lists.newArrayList();
        game.forEachCardInGame(new Visitor<Card>() {
            @Override
            public boolean visit(Card c) {
                for (final ReplacementEffect re : c.getReplacementEffects()) {
                    if (re.getMode() == ReplacementType.DamageDone
                            && re.getLayer() == ReplacementLayer.Other
                            && re.hasParam("Prevent") && re.getParam("Prevent").equals("True")
                            && re.hasParam("IsCombat") && re.getParam("IsCombat").equals("True")
                            && !re.hasParam("ValidSource") && !re.hasParam("ValidTarget")
                            && re.zonesCheck(game.getZoneOf(c))) {
                        list.add(re);
                    }
                }
                return true;
            }
        });
        return !list.isEmpty();
    }

    public boolean isReplacing() {
        return !hasRun.isEmpty();
    }
}
