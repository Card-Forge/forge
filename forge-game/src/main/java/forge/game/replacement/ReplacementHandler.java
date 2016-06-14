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

import forge.card.MagicColor;
import forge.game.Game;
import forge.game.GameLogEntryType;
import forge.game.ability.AbilityFactory;
import forge.game.ability.AbilityUtils;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;
import forge.util.FileSection;
import forge.util.Visitor;

import org.apache.commons.lang3.StringUtils;

import java.util.*;

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

        ReplacementEffect chosenRE = decider.getController().chooseSingleReplacementEffect("Choose a replacement effect to apply first.", possibleReplacers, runParams);

        possibleReplacers.remove(chosenRE);

        chosenRE.setHasRun(true);
        ReplacementResult res = this.executeReplacement(runParams, chosenRE, decider, game);
        if (res == ReplacementResult.NotReplaced) {
            if (!possibleReplacers.isEmpty()) {
                res = run(runParams);
            }
            chosenRE.setHasRun(false);
            return res;
        }
        chosenRE.setHasRun(false);
        String message = chosenRE.toString();
        if ( !StringUtils.isEmpty(message))
        	if (chosenRE.getHostCard() != null) {
        		message = message.replaceAll("CARDNAME", chosenRE.getHostCard().getName());
        	}
            game.getGameLog().add(GameLogEntryType.EFFECT_REPLACED, message);
        return res;
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

        Card host = replacementEffect.getHostCard();
        // AlternateState for OriginsPlaneswalker
        if (host.hasAlternateState()) {
            host = game.getCardState(host);
        }

        if (mapParams.containsKey("ReplaceWith")) {
            final String effectSVar = mapParams.get("ReplaceWith");
            final String effectAbString = host.getSVar(effectSVar);
            // TODO: the source of replacement effect should be the source of the original effect 
            effectSA = AbilityFactory.getAbility(effectAbString, host);
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

        if (effectSA != null && replacementEffect.isIntrinsic()) {
            effectSA.setIntrinsic(true);
            effectSA.changeText();
            effectSA.setReplacementAbility(true);
        }

        // Decider gets to choose whether or not to apply the replacement.
        if (replacementEffect.getMapParams().containsKey("Optional")) {
            Player optDecider = decider;
            if (mapParams.containsKey("OptionalDecider") && (effectSA != null)) {
                effectSA.setActivatingPlayer(host.getController());
                optDecider = AbilityUtils.getDefinedPlayers(host,
                        mapParams.get("OptionalDecider"), effectSA).get(0);
            }

            Card cardForUi = host.getCardForUi();
            String effectDesc = replacementEffect.toString().replace("CARDNAME", cardForUi.getName());
            final String question = replacementEffect instanceof ReplaceDiscard
                ? String.format("Apply replacement effect of %s to %s?\r\n(%s)", cardForUi, runParams.get("Card").toString(), effectDesc)
                : String.format("Apply replacement effect of %s?\r\n(%s)", cardForUi, effectDesc);
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

        Player player = host.getController();

        if (mapParams.containsKey("ManaReplacement")) {
            final SpellAbility manaAb = (SpellAbility) runParams.get("AbilityMana");
            final Player player1 = (Player) runParams.get("Player");
            final String rep = (String) runParams.get("Mana");
            // Replaced mana type
            final Card repHost = host;
            String repType = repHost.getSVar(mapParams.get("ManaReplacement"));
            if (repType.contains("Chosen") && repHost.hasChosenColor()) {
                repType = repType.replace("Chosen", MagicColor.toShortString(repHost.getChosenColor()));
            }
            manaAb.getManaPart().setManaReplaceType(repType);
            manaAb.getManaPart().produceMana(rep, player1, manaAb);
        } else {
            player.getController().playSpellAbilityNoStack(effectSA, true);
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

    public void cleanUpTemporaryReplacements() {
        game.forEachCardInGame(new Visitor<Card>() {
            @Override
            public void visit(Card c) {
                for (int i = 0; i < c.getReplacementEffects().size(); i++) {
                    ReplacementEffect rep = c.getReplacementEffects().get(i);
                    if (rep.isTemporary()) {
                        c.removeReplacementEffect(rep);
                        i--;
                    }
                }
            }
        });
        game.forEachCardInGame(new Visitor<Card>() {
            @Override
            public void visit(Card c) {
                for (int i = 0; i < c.getReplacementEffects().size(); i++) {
                    c.getReplacementEffects().get(i).setTemporarilySuppressed(false);
                }
            }
        });
    }
}
