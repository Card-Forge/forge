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
package forge.card.replacement;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import forge.Card;
import forge.GameLogEntryType;
import forge.card.ability.AbilityFactory;
import forge.card.ability.AbilityUtils;
import forge.card.spellability.SpellAbility;
import forge.game.Game;
import forge.game.ai.ComputerUtil;
import forge.game.player.HumanPlay;
import forge.game.player.Player;
import forge.game.zone.ZoneType;
import forge.gui.GuiChoose;
import forge.util.FileSection;

/**
 * TODO: Write javadoc for this type.
 * 
 */
public class ReplacementHandler {
    private final Game game;
    /**
     * ReplacementHandler.
     * @param gameState
     */
    public ReplacementHandler(Game gameState) {
        game = gameState;
    }

    //private final List<ReplacementEffect> tmpEffects = new ArrayList<ReplacementEffect>();

    public ReplacementResult run(final HashMap<String, Object> runParams) {
        final Object affected = runParams.get("Affected");
        Player decider = null;

        
        // Figure out who decides which of multiple replacements to apply
        // as well as whether or not to apply optional replacements.
        if (affected instanceof Player) {
            decider = (Player) affected;
        } else {
            decider = ((Card) affected).getController();
        }
        final Game game = decider.getGame(); 

        if (runParams.get("Event").equals("Moved")) {
            ReplacementResult res = run(runParams, ReplacementLayer.Control, decider, game);
            if (res != ReplacementResult.NotReplaced) {
                return res;
            }
            res = run(runParams, ReplacementLayer.Copy, decider, game);
            if (res != ReplacementResult.NotReplaced) {
                return res;
            }
            res = run(runParams, ReplacementLayer.Other, decider, game);
            if (res != ReplacementResult.NotReplaced) {
                return res;
            }
            res = run(runParams, ReplacementLayer.None, decider, game);
            if (res != ReplacementResult.NotReplaced) {
                return res;
            }
        }
        else {
            ReplacementResult res = run(runParams, ReplacementLayer.None, decider, game);
            if (res != ReplacementResult.NotReplaced) {
                return res;
            }
        }

        return ReplacementResult.NotReplaced;

    }

    /**
     * 
     * Runs any applicable replacement effects.
     * 
     * @param runParams
     *            the run params,same as for triggers.
     * @return true if the event was replaced.
     */
    public ReplacementResult run(final HashMap<String, Object> runParams, final ReplacementLayer layer, final Player decider, final Game game) {

        final List<ReplacementEffect> possibleReplacers = new ArrayList<ReplacementEffect>();
        // Round up Non-static replacement effects ("Until EOT," or
        // "The next time you would..." etc)
        /*for (final ReplacementEffect replacementEffect : this.tmpEffects) {
            if (!replacementEffect.hasRun() && replacementEffect.canReplace(runParams) && replacementEffect.getLayer() == layer) {
                possibleReplacers.add(replacementEffect);
            }
        }*/

        // Round up Static replacement effects
        for (final Player p : game.getPlayers()) {
            for (final Card crd : p.getAllCards()) {
                for (final ReplacementEffect replacementEffect : crd.getReplacementEffects()) {
                    if (!replacementEffect.hasRun()
                            && replacementEffect.getLayer() == layer
                            && replacementEffect.requirementsCheck(game)
                            && replacementEffect.canReplace(runParams)
                            && !possibleReplacers.contains(replacementEffect)
                            && replacementEffect.zonesCheck(game.getZoneOf(crd))) {
                        possibleReplacers.add(replacementEffect);
                    }
                }
            }
        }

        if (possibleReplacers.isEmpty()) {
            return ReplacementResult.NotReplaced;
        }

        ReplacementEffect chosenRE = null;

        if (possibleReplacers.size() == 1) {
            chosenRE = possibleReplacers.get(0);
        }

        if (possibleReplacers.size() > 1) {
            if (decider.isHuman()) {
                chosenRE = GuiChoose.one("Choose which replacement effect to apply.",
                        possibleReplacers);
            } else {
                // AI logic for choosing which replacement effect to apply
                // happens here.
                chosenRE = possibleReplacers.get(0);
            }
        }

        possibleReplacers.remove(chosenRE);

        if (chosenRE != null) {
            chosenRE.setHasRun(true);
            ReplacementResult res = this.executeReplacement(runParams, chosenRE, decider, game);
            if (res != ReplacementResult.NotReplaced) {
                chosenRE.setHasRun(false);
                String message = chosenRE.toString();
                if ( !StringUtils.isEmpty(message))
                    game.getGameLog().add(GameLogEntryType.EFFECT_REPLACED, chosenRE.toString());
                return res;
            } else {
                if (possibleReplacers.size() == 0) {
                    chosenRE.setHasRun(false);
                    return res;
                }
                else {
                    ReplacementResult ret = run(runParams);
                    chosenRE.setHasRun(false);
                    return ret;
                }
            }
        } else {
            return ReplacementResult.NotReplaced;
        }

    }

    /**
     * 
     * Runs a single replacement effect.
     * 
     * @param replacementEffect
     *            the replacement effect to run
     */
    private ReplacementResult executeReplacement(final Map<String, Object> runParams,
        final ReplacementEffect replacementEffect, final Player decider, final Game game) {
        final Map<String, String> mapParams = replacementEffect.getMapParams();

        SpellAbility effectSA = null;

        if (mapParams.containsKey("ReplaceWith")) {
            final String effectSVar = mapParams.get("ReplaceWith");
            final String effectAbString = replacementEffect.getHostCard().getSVar(effectSVar);

            effectSA = AbilityFactory.getAbility(effectAbString, replacementEffect.getHostCard());
            effectSA.setTrigger(true);

            SpellAbility tailend = effectSA;
            do {
                replacementEffect.setReplacingObjects(runParams, tailend);
                tailend = tailend.getSubAbility();
            } while(tailend != null);
        }
        else if (replacementEffect.getOverridingAbility() != null) {
            effectSA = replacementEffect.getOverridingAbility();
            SpellAbility tailend = effectSA;
            do {
                replacementEffect.setReplacingObjects(runParams, tailend);
                tailend = tailend.getSubAbility();
            } while(tailend != null);
        }

        // Decider gets to choose whether or not to apply the replacement.
        if (replacementEffect.getMapParams().containsKey("Optional")) {
            Player optDecider = decider;
            if (mapParams.containsKey("OptionalDecider") && (effectSA != null)) {
                effectSA.setActivatingPlayer(replacementEffect.getHostCard().getController());
                optDecider = AbilityUtils.getDefinedPlayers(replacementEffect.getHostCard(),
                        mapParams.get("OptionalDecider"), effectSA).get(0);
            }

            String effectDesc = replacementEffect.toString().replace("CARDNAME", replacementEffect.getHostCard().getName());
            final String question = String.format("Apply replacement effect of %s?\r\n(%s)", replacementEffect.getHostCard(), effectDesc);
            boolean confirmed = optDecider.getController().confirmReplacementEffect(replacementEffect, effectSA, question);
            if (!confirmed) {
                return ReplacementResult.NotReplaced;
            }
        }

        if (mapParams.containsKey("Prevent")) {
            if (mapParams.get("Prevent").equals("True")) {
                return ReplacementResult.Prevented; // Nothing should replace the event.
            }
        }

        Player player = replacementEffect.getHostCard().getController();
        player.getController().playSpellAbilityNoStack(player, effectSA);

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
        
        final Map<String, String> mapParams = FileSection.parseToMap(repParse, "$", "|");
        return ReplacementHandler.parseReplacement(mapParams, host, intrinsic);
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
    private static ReplacementEffect parseReplacement(final Map<String, String> mapParams, final Card host, final boolean intrinsic) {
        final ReplacementType rt = ReplacementType.smartValueOf(mapParams.get("Event"));
        ReplacementEffect ret = rt.createReplacement(mapParams, host, intrinsic);

        String activeZones = mapParams.get("ActiveZones");
        if (null != activeZones) {
            ret.setActiveZone(EnumSet.copyOf(ZoneType.listValueOf(activeZones)));
        }

        return ret;
    }

    /**
     * TODO: Write javadoc for this method.
     */
    public void cleanUpTemporaryReplacements() {
         final List<Card> absolutelyAllCards = game.getCardsInGame();
         for (final Card c : absolutelyAllCards) {
             for (int i = 0; i < c.getReplacementEffects().size(); i++) {
                if (c.getReplacementEffects().get(i).isTemporary()) {
                     c.getReplacementEffects().remove(i);
                     i--;
                }
             }
        }
        for (final Card c : absolutelyAllCards) {
             for (int i = 0; i < c.getReplacementEffects().size(); i++) {
                 c.getReplacementEffects().get(i).setTemporarilySuppressed(false);
             }
        }
    }
}
