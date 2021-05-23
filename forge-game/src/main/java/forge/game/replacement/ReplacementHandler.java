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

import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import forge.game.CardTraitBase;
import forge.game.Game;
import forge.game.GameEntity;
import forge.game.GameLogEntryType;
import forge.game.IHasSVars;
import forge.game.ability.AbilityFactory;
import forge.game.ability.AbilityKey;
import forge.game.ability.AbilityUtils;
import forge.game.ability.ApiType;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CardDamageMap;
import forge.game.card.CardState;
import forge.game.card.CardTraitChanges;
import forge.game.card.CardUtil;
import forge.game.keyword.KeywordInterface;
import forge.game.keyword.KeywordsChange;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.Zone;
import forge.game.zone.ZoneType;
import forge.util.CardTranslation;
import forge.util.FileSection;
import forge.util.Localizer;
import forge.util.TextUtil;
import forge.util.Visitor;

public class ReplacementHandler {
    private final Game game;

    private Set<ReplacementEffect> hasRun = Sets.newHashSet();
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

            // need to check if Intrinsic has run
            for (ReplacementEffect re : affectedLKI.getReplacementEffects()) {
                if (re.isIntrinsic() && this.hasRun.contains(re)) {
                    re.setHasRun(true);
                }
            }

            // need to check non Intrinsic
            for (Map.Entry<Long, CardTraitChanges> e : affectedLKI.getChangedCardTraits().entrySet()) {
                boolean hasRunRE = false;
                String skey = String.valueOf(e.getKey());

                for (ReplacementEffect re : this.hasRun) {
                    if (!re.isIntrinsic() && skey.equals(re.getSVar("_ReplacedTimestamp"))) {
                        hasRunRE = true;
                        break;
                    }
                }

                for (ReplacementEffect re : e.getValue().getReplacements()) {
                    re.setSVar("_ReplacedTimestamp", skey);
                    if (hasRunRE) {
                        re.setHasRun(true);
                    }
                }
            }
            for (Map.Entry<Long, KeywordsChange> e : affectedLKI.getChangedCardKeywords().entrySet()) {
                boolean hasRunRE = false;
                String skey = String.valueOf(e.getKey());

                for (ReplacementEffect re : this.hasRun) {
                    if (!re.isIntrinsic() && skey.equals(re.getSVar("_ReplacedTimestamp"))) {
                        hasRunRE = true;
                        break;
                    }
                }

                for (KeywordInterface k : e.getValue().getKeywords()) {
                    for (ReplacementEffect re : k.getReplacements()) {
                        re.setSVar("_ReplacedTimestamp", skey);
                        if (hasRunRE) {
                            re.setHasRun(true);
                        }
                    }
                }
            }

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
                final Card c = preList.get(crd);

                for (final ReplacementEffect replacementEffect : c.getReplacementEffects()) {

                    // Use "CheckLKIZone" parameter to test for effects that care abut where the card was last (e.g. Kalitas, Traitor of Ghet
                    // getting hit by mass removal should still produce tokens).
                    Zone cardZone = "True".equals(replacementEffect.getParam("CheckSelfLKIZone")) ? game.getChangeZoneLKIInfo(c).getLastKnownZone() : game.getZoneOf(c);

                    // Replacement effects that are tied to keywords (e.g. damage prevention effects - if the keyword is removed, the replacement
                    // effect should be inactive)
                    if (replacementEffect.hasParam("TiedToKeyword")) {
                        String kw = replacementEffect.getParam("TiedToKeyword");
                        if (!c.hasKeyword(kw)) {
                            continue;
                        }
                    }

                    if (!replacementEffect.hasRun()
                            && (layer == null || replacementEffect.getLayer() == layer)
                            && event.equals(replacementEffect.getMode())
                            && replacementEffect.requirementsCheck(game)
                            && replacementEffect.canReplace(runParams)
                            && !possibleReplacers.contains(replacementEffect)
                            && replacementEffect.zonesCheck(cardZone)) {
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
                runParams.put(AbilityKey.Affected, affectedCard);
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

        ReplacementEffect chosenRE = decider.getController().chooseSingleReplacementEffect(Localizer.getInstance().getMessage("lblChooseFirstApplyReplacementEffect"), possibleReplacers);

        possibleReplacers.remove(chosenRE);

        chosenRE.setHasRun(true);
        hasRun.add(chosenRE);
        chosenRE.setOtherChoices(possibleReplacers);
        ReplacementResult res = executeReplacement(runParams, chosenRE, decider, game);
        if (res == ReplacementResult.NotReplaced) {
            if (!possibleReplacers.isEmpty()) {
                res = run(event, runParams);
            }
            chosenRE.setHasRun(false);
            hasRun.remove(chosenRE);
            chosenRE.setOtherChoices(null);
            return res;
        }
        chosenRE.setHasRun(false);
        hasRun.remove(chosenRE);
        chosenRE.setOtherChoices(null);

        // Updated Replacements need to be logged elsewhere because its otherwise in the wrong order
        if (res != ReplacementResult.Updated) {
            String message = chosenRE.getDescription();
            if ( !StringUtils.isEmpty(message))
                if (chosenRE.getHostCard() != null) {
                    message = TextUtil.fastReplace(message, "CARDNAME", chosenRE.getHostCard().getName());
                }
                game.getGameLog().add(GameLogEntryType.EFFECT_REPLACED, message);
        }

        return res;
    }

    private void putPreventMapEntry(final Map<AbilityKey, Object> runParams) {
        Card sourceLKI = (Card) runParams.get(AbilityKey.DamageSource);
        GameEntity target = (GameEntity) runParams.get(AbilityKey.Affected);
        Integer damage = (Integer) runParams.get(AbilityKey.DamageAmount);

        // Set prevent map entry
        CardDamageMap preventMap = (CardDamageMap) runParams.get(AbilityKey.PreventMap);
        preventMap.put(sourceLKI, target, damage);

        // Following codes are commented out since DamagePrevented trigger is currently not used by any Card.
        // final Map<AbilityKey, Object> trigParams = AbilityKey.newMap();
        // trigParams.put(AbilityKey.DamageTarget, target);
        // trigParams.put(AbilityKey.DamageAmount, damage);
        // trigParams.put(AbilityKey.DamageSource, sourceLKI);
        // trigParams.put(AbilityKey.IsCombatDamage, runParams.get(AbilityKey.IsCombat));
        // game.getTriggerHandler().runTrigger(TriggerType.DamagePrevented, trigParams, false);
    }

    /**
     *
     * Runs a single replacement effect.
     *
     * @param replacementEffect
     *            the replacement effect to run
     */
    private ReplacementResult executeReplacement(final Map<AbilityKey, Object> runParams,
        final ReplacementEffect replacementEffect, final Player decider, final Game game) {
        final Map<String, String> mapParams = replacementEffect.getMapParams();

        SpellAbility effectSA = null;

        Card host = replacementEffect.getHostCard();
        // AlternateState for OriginsPlaneswalker
        // FaceDown for cards like Necropotence
        if (host.hasAlternateState() || host.isFaceDown()) {
            host = game.getCardState(host);
        }

        if (replacementEffect.getOverridingAbility() == null && mapParams.containsKey("ReplaceWith")) {
            final String effectSVar = mapParams.get("ReplaceWith");
            // TODO: the source of replacement effect should be the source of the original effect
            effectSA = AbilityFactory.getAbility(host, effectSVar, replacementEffect);
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
                tailend = tailend.getSubAbility();
            } while(tailend != null);

            effectSA.setLastStateBattlefield(game.getLastStateBattlefield());
            effectSA.setLastStateGraveyard(game.getLastStateGraveyard());
            if (replacementEffect.isIntrinsic()) {
                effectSA.setIntrinsic(true);
                effectSA.changeText();
            }
            effectSA.setReplacementEffect(replacementEffect);
        }

        // Decider gets to choose whether or not to apply the replacement.
        if (replacementEffect.hasParam("Optional")) {
            Player optDecider = decider;
            if (mapParams.containsKey("OptionalDecider") && (effectSA != null)) {
                effectSA.setActivatingPlayer(host.getController());
                optDecider = AbilityUtils.getDefinedPlayers(host,
                        mapParams.get("OptionalDecider"), effectSA).get(0);
            }

            Card cardForUi = host.getCardForUi();
            String effectDesc = TextUtil.fastReplace(replacementEffect.getDescription(), "CARDNAME", CardTranslation.getTranslatedName(cardForUi.getName()));
            final String question = replacementEffect instanceof ReplaceDiscard
                ? Localizer.getInstance().getMessage("lblApplyCardReplacementEffectToCardConfirm", CardTranslation.getTranslatedName(cardForUi.getName()), runParams.get(AbilityKey.Card).toString(), effectDesc)
                : Localizer.getInstance().getMessage("lblApplyReplacementEffectOfCardConfirm", CardTranslation.getTranslatedName(cardForUi.getName()), effectDesc);
            GameEntity affected = (GameEntity) runParams.get(AbilityKey.Affected);
            boolean confirmed = optDecider.getController().confirmReplacementEffect(replacementEffect, effectSA, affected, question);
            if (!confirmed) {
                return ReplacementResult.NotReplaced;
            }
        }

        if (mapParams.containsKey("Prevent") && mapParams.get("Prevent").equals("True")) {
            if (replacementEffect.getMode() == ReplacementType.DamageDone) {
                if (Boolean.TRUE.equals(runParams.get(AbilityKey.NoPreventDamage))) {
                    return ReplacementResult.NotReplaced;
                }
                putPreventMapEntry(runParams);
            }
            return ReplacementResult.Prevented; // Nothing should replace the event.
        }

        if (mapParams.containsKey("Skip")) {
            if (mapParams.get("Skip").equals("True")) {
                return ReplacementResult.Skipped; // Event is skipped.
            }
        }

        boolean cantPreventDamage = (replacementEffect.getMode() == ReplacementType.DamageDone
            && mapParams.containsKey("PreventionEffect")
            && Boolean.TRUE.equals(runParams.get(AbilityKey.NoPreventDamage)));

        Player player = host.getController();

        if (!cantPreventDamage || mapParams.containsKey("AlwaysReplace")) {
            player.getController().playSpellAbilityNoStack(effectSA, true);
            if (replacementEffect.getMode() == ReplacementType.DamageDone
                    && effectSA.getApi() != ApiType.ReplaceDamage && !cantPreventDamage) {
                putPreventMapEntry(runParams);
            }
        }

        // If can't prevent damage, result is not replaced
        if (cantPreventDamage) {
            return ReplacementResult.NotReplaced;
        }

        // if the spellability is a replace effect then its some new logic
        // if ReplacementResult is set in run params use that instead
        if (runParams.containsKey(AbilityKey.ReplacementResult)) {
            return (ReplacementResult) runParams.get(AbilityKey.ReplacementResult);
        }

        return ReplacementResult.Replaced;
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
        return ReplacementHandler.parseReplacement(parseParams(repParse), host, intrinsic, sVarHolder);
    }

    public static Map<String, String> parseParams(final String repParse) {
        return FileSection.parseToMap(repParse, FileSection.DOLLAR_SIGN_KV_SEPARATOR);
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
                            && re.getOverridingAbility().getApi() == ApiType.ReplaceDamage) {
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
}
