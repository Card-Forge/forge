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

import forge.AllZone;
import forge.Card;
import forge.GameActionUtil;
import forge.Singletons;
import forge.card.abilityfactory.AbilityFactory;
import forge.card.spellability.SpellAbility;
import forge.game.player.ComputerUtil;
import forge.game.player.Player;
import forge.game.zone.ZoneType;
import forge.gui.GuiChoose;

/**
 * TODO: Write javadoc for this type.
 * 
 */
public class ReplacementHandler {

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
        
        
        if(runParams.get("Event").equals("Moved")) {
            ReplacementResult res = run(runParams,ReplacementLayer.Control,decider);
            if(res != ReplacementResult.NotReplaced) {
                return res;
            }
            res = run(runParams,ReplacementLayer.Copy,decider);
            if(res != ReplacementResult.NotReplaced) {
                return res;
            }
            res = run(runParams,ReplacementLayer.Other,decider);
            if(res != ReplacementResult.NotReplaced) {
                return res;
            }
            res = run(runParams,ReplacementLayer.None,decider);
            if(res != ReplacementResult.NotReplaced) {
                return res;
            }
        }
        else {
            ReplacementResult res = run(runParams,ReplacementLayer.None,decider);
            if(res != ReplacementResult.NotReplaced) {
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
    public ReplacementResult run(final HashMap<String, Object> runParams, final ReplacementLayer layer,final Player decider) {
        
        final List<ReplacementEffect> possibleReplacers = new ArrayList<ReplacementEffect>();
        // Round up Non-static replacement effects ("Until EOT," or
        // "The next time you would..." etc)
        /*for (final ReplacementEffect replacementEffect : this.tmpEffects) {
            if (!replacementEffect.hasRun() && replacementEffect.canReplace(runParams) && replacementEffect.getLayer() == layer) {
                possibleReplacers.add(replacementEffect);
            }
        }*/

        // Round up Static replacement effects
        for (final Player p : Singletons.getModel().getGameState().getPlayers()) {
            for (final Card crd : p.getAllCards()) {
                for (final ReplacementEffect replacementEffect : crd.getReplacementEffects()) {
                    if (!replacementEffect.hasRun()
                            && replacementEffect.getLayer() == layer
                            && replacementEffect.requirementsCheck()
                            && replacementEffect.canReplace(runParams) 
                            && !possibleReplacers.contains(replacementEffect)
                            && replacementEffect.zonesCheck(AllZone.getZoneOf(crd))) {
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
            ReplacementResult res = this.executeReplacement(runParams, chosenRE, decider);
            if (res != ReplacementResult.NotReplaced) {
                chosenRE.setHasRun(false);
                AllZone.getGameLog().add("ReplacementEffect", chosenRE.toString(), 2);
                return res;
            } else {
                if (possibleReplacers.size() == 0) {
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
    private ReplacementResult executeReplacement(final HashMap<String, Object> runParams,
        final ReplacementEffect replacementEffect, final Player decider) {
        final HashMap<String, String> mapParams = replacementEffect.getMapParams();

        SpellAbility effectSA = null;

        if (mapParams.containsKey("ReplaceWith")) {
            final String effectSVar = mapParams.get("ReplaceWith");
            final String effectAbString = replacementEffect.getHostCard().getSVar(effectSVar);

            final AbilityFactory abilityFactory = new AbilityFactory();

            effectSA = abilityFactory.getAbility(effectAbString, replacementEffect.getHostCard());

            SpellAbility tailend = effectSA;
            do
            {
                replacementEffect.setReplacingObjects(runParams, tailend);
                tailend = tailend.getSubAbility();
            } while(tailend != null);
        }
        else if (replacementEffect.getOverridingAbility() != null) {
            effectSA = replacementEffect.getOverridingAbility();
            SpellAbility tailend = effectSA;
            do
            {
                replacementEffect.setReplacingObjects(runParams, tailend);
                tailend = tailend.getSubAbility();
            } while(tailend != null);
        }

        // Decider gets to choose wether or not to apply the replacement.
        if (replacementEffect.getMapParams().containsKey("Optional")) {
            Player optDecider = decider;
            if (mapParams.containsKey("OptionalDecider") && (effectSA != null)) {
                effectSA.setActivatingPlayer(replacementEffect.getHostCard().getController());
                optDecider = AbilityFactory.getDefinedPlayers(replacementEffect.getHostCard(),
                        mapParams.get("OptionalDecider"), effectSA).get(0);
            }

            if (optDecider.isHuman()) {
                final StringBuilder buildQuestion = new StringBuilder("Apply replacement effect of ");
                buildQuestion.append(replacementEffect.getHostCard());
                buildQuestion.append("?\r\n(");
                buildQuestion.append(replacementEffect.toString());
                buildQuestion.append(")");
                if (!GameActionUtil.showYesNoDialog(replacementEffect.getHostCard(), buildQuestion.toString())) {
                    return ReplacementResult.NotReplaced;
                }
            } else {
                // AI-logic
                if (!replacementEffect.aiShouldRun(effectSA)) {
                    return ReplacementResult.NotReplaced;
                }
            }
        }

        if (mapParams.containsKey("Prevent")) {
            if (mapParams.get("Prevent").equals("True")) {
                return ReplacementResult.Prevented; // Nothing should replace the event.
            }
        }

        Player player = replacementEffect.getHostCard().getController(); 
        if (player.isHuman()) {
            Singletons.getModel().getGameAction().playSpellAbilityNoStack(effectSA, false);
        } else {
            ComputerUtil.playNoStack(player, effectSA);
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
    public static ReplacementEffect parseReplacement(final String repParse, final Card host) {
        final HashMap<String, String> mapParams = ReplacementHandler.parseParams(repParse);
        return ReplacementHandler.parseReplacement(mapParams, host);
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
    public static ReplacementEffect parseReplacement(final HashMap<String, String> mapParams, final Card host) {
        ReplacementEffect ret = null;

        final String eventToReplace = mapParams.get("Event");
        if (eventToReplace.equals("Draw")) {
            ret = new ReplaceDraw(mapParams, host);
        } else if (eventToReplace.equals("GainLife")) {
            ret = new ReplaceGainLife(mapParams, host);
        } else if (eventToReplace.equals("DamageDone")) {
            ret = new ReplaceDamage(mapParams, host);
        } else if (eventToReplace.equals("GameLoss")) {
            ret = new ReplaceGameLoss(mapParams, host);
        } else if (eventToReplace.equals("Moved")) {
            ret = new ReplaceMoved(mapParams, host);
        }
        
        String activeZones = mapParams.get("ActiveZones");
        if (null != activeZones) {
            ret.setActiveZone(EnumSet.copyOf(ZoneType.listValueOf(activeZones)));
        }

        return ret;
    }

    /**
     * <p>
     * parseParams.
     * </p>
     * 
     * @param repParse
     *            a {@link java.lang.String} object.
     * @return a {@link java.util.HashMap} object.
     */
    private static HashMap<String, String> parseParams(final String repParse) {
        final HashMap<String, String> mapParams = new HashMap<String, String>();

        if (repParse.length() == 0) {
            throw new RuntimeException("ReplacementFactory : registerTrigger -- trigParse too short");
        }

        final String[] params = repParse.split("\\|");

        for (int i = 0; i < params.length; i++) {
            params[i] = params[i].trim();
        }

        for (final String param : params) {
            final String[] splitParam = param.split("\\$");
            for (int i = 0; i < splitParam.length; i++) {
                splitParam[i] = splitParam[i].trim();
            }

            if (splitParam.length != 2) {
                final StringBuilder sb = new StringBuilder();
                sb.append("ReplacementFactory Parsing Error in registerTrigger() : Split length of ");
                sb.append(param).append(" is not 2.");
                throw new RuntimeException(sb.toString());
            }

            mapParams.put(splitParam[0], splitParam[1]);
        }

        return mapParams;
    }
}
