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
package forge.game.ability.effects;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.Lists;

import forge.GameCommand;
import forge.card.CardType;
import forge.card.ColorSet;
import forge.card.RemoveType;
import forge.card.mana.ManaCost;
import forge.card.mana.ManaCostParser;
import forge.game.cost.Cost;
import forge.game.Game;
import forge.game.ability.AbilityFactory;
import forge.game.ability.AbilityUtils;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.event.GameEventCardStatsChanged;
import forge.game.keyword.Keyword;
import forge.game.replacement.ReplacementEffect;
import forge.game.replacement.ReplacementHandler;
import forge.game.spellability.AbilityStatic;
import forge.game.spellability.SpellAbility;
import forge.game.staticability.StaticAbility;
import forge.game.trigger.Trigger;
import forge.game.trigger.TriggerHandler;

public abstract class AnimateEffectBase extends SpellAbilityEffect {
    public static void doAnimate(final Card c, final SpellAbility sa, final Integer power, final Integer toughness,
            final CardType addType, final CardType removeType, final ColorSet colors,
            final List<String> keywords, final List<String> removeKeywords, final List<String> hiddenKeywords,
            List<String> abilities, final List<String> triggers, final List<String> replacements, final List<String> stAbs,
            final long timestamp, final String duration) {
        final Card source = sa.getHostCard();
        final Game game = source.getGame();
        final boolean perpetual = "Perpetual".equals(duration);

        boolean addAllCreatureTypes = sa.hasParam("AddAllCreatureTypes");

        Set<RemoveType> remove = EnumSet.noneOf(RemoveType.class);
        if (sa.hasParam("RemoveSuperTypes"))
            remove.add(RemoveType.SuperTypes);
        if (sa.hasParam("RemoveCardTypes"))
            remove.add(RemoveType.CardTypes);
        if (sa.hasParam("RemoveSubTypes"))
            remove.add(RemoveType.SubTypes);
        if (sa.hasParam("RemoveLandTypes"))
            remove.add(RemoveType.LandTypes);
        if (sa.hasParam("RemoveCreatureTypes"))
            remove.add(RemoveType.CreatureTypes);
        if (sa.hasParam("RemoveArtifactTypes"))
            remove.add(RemoveType.ArtifactTypes);
        if (sa.hasParam("RemoveEnchantmentTypes"))
            remove.add(RemoveType.EnchantmentTypes);

        boolean removeNonManaAbilities = sa.hasParam("RemoveNonManaAbilities");
        boolean removeAll = sa.hasParam("RemoveAllAbilities");

        if (sa.hasParam("RememberAnimated")) {
            source.addRemembered(c);
        }

        // Alchemy "incorporate" cost
        ColorSet incColors = null;
        if (sa.hasParam("Incorporate")) {
            final String incorporate = sa.getParam("Incorporate");

            Map <String, Object> params = new HashMap<>();
            params.put("Incorporate", incorporate);
            params.put("Timestamp", timestamp);
            params.put("Category", "Incorporate");
            c.addPerpetual(params);

            final ManaCost incMCost = new ManaCost(new ManaCostParser(incorporate));
            incColors = ColorSet.fromMask(incMCost.getColorProfile());
            final ManaCost newCost = ManaCost.combine(c.getManaCost(), incMCost);
            c.addChangedManaCost(newCost, timestamp, 0);
            c.updateManaCostForView();

            if (c.getFirstSpellAbility() != null) {
                c.getFirstSpellAbility().getPayCosts().add(new Cost(incorporate, false));
            }
        }
        
        if (!addType.isEmpty() || !removeType.isEmpty() || addAllCreatureTypes || !remove.isEmpty()) {
            if (perpetual) {
                Map <String, Object> params = new HashMap<>();
                params.put("AddTypes", addType);
                params.put("RemoveTypes", removeType);
                params.put("RemoveXTypes", remove);
                params.put("Timestamp", timestamp);
                params.put("Category", "Types");
                c.addPerpetual(params);
            }
            c.addChangedCardTypes(addType, removeType, addAllCreatureTypes, remove, timestamp, 0, true, false);
        }

        if (!keywords.isEmpty() || !removeKeywords.isEmpty() || removeAll) {
            if (perpetual) {
                Map <String, Object> params = new HashMap<>();
                params.put("AddKeywords", keywords);
                params.put("RemoveAll", removeAll);
                params.put("Timestamp", timestamp);
                params.put("Category", "Keywords");
                c.addPerpetual(params);
            }
            c.addChangedCardKeywords(keywords, removeKeywords, removeAll, timestamp, 0);
        }

        // do this after changing types in case it wasn't a creature before
        if (power != null || toughness != null) {
            if (perpetual) {
                Map <String, Object> params = new HashMap<>();
                params.put("Power", power);
                params.put("Toughness", toughness);
                params.put("Timestamp", timestamp);
                params.put("Category", "NewPT");
                c.addPerpetual(params);
            }
            c.addNewPT(power, toughness, timestamp, 0);
        }

        if (sa.hasParam("CantHaveKeyword")) {
            c.addCantHaveKeyword(timestamp, Keyword.setValueOf(sa.getParam("CantHaveKeyword")));
        }

        if (!hiddenKeywords.isEmpty()) {
            c.addHiddenExtrinsicKeywords(timestamp, 0, hiddenKeywords);
        }

        if (colors != null) {
            final boolean overwrite = sa.hasParam("OverwriteColors");
            handleColors(c, colors, timestamp, overwrite, perpetual);
        }
        if (incColors != null) {
            handleColors(c, incColors, timestamp, false, perpetual);
        }

        if (sa.hasParam("LeaveBattlefield")) {
            addLeaveBattlefieldReplacement(c, sa, sa.getParam("LeaveBattlefield"));
        }

        // remove abilities
        final List<SpellAbility> removedAbilities = Lists.newArrayList();
        boolean clearSpells = sa.hasParam("OverwriteSpells");

        if (clearSpells) {
            removedAbilities.addAll(Lists.newArrayList(c.getSpells()));
        }

        if (sa.hasParam("RemoveThisAbility") && !removedAbilities.contains(sa)) {
            removedAbilities.add(sa);
        }

        // give abilities
        final List<SpellAbility> addedAbilities = Lists.newArrayList();
        for (final String s : abilities) {
            SpellAbility sSA = AbilityFactory.getAbility(c, s, sa);
            addedAbilities.add(sSA);

            if (sa.hasParam("TransferActivator")) {
                sSA.getRestrictions().setActivator("Player.PlayerUID_" + sa.getActivatingPlayer().getId());
            }
        }

        // Grant triggers
        final List<Trigger> addedTriggers = Lists.newArrayList();
        for (final String s : triggers) {
            final Trigger parsedTrigger = TriggerHandler.parseTrigger(AbilityUtils.getSVar(sa, s), c, false, sa);
            addedTriggers.add(parsedTrigger);
        }

        // give replacement effects
        final List<ReplacementEffect> addedReplacements = Lists.newArrayList();
        for (final String s : replacements) {
            addedReplacements.add(ReplacementHandler.parseReplacement(AbilityUtils.getSVar(sa, s), c, false, sa));
        }

        // give static abilities (should only be used by cards to give
        // itself a static ability)
        final List<StaticAbility> addedStaticAbilities = Lists.newArrayList();
        for (final String s : stAbs) {
            addedStaticAbilities.add(StaticAbility.create(AbilityUtils.getSVar(sa, s), c, sa.getCardState(), false));
        }

        final GameCommand unanimate = new GameCommand() {
            private static final long serialVersionUID = -5861759814760561373L;

            @Override
            public void run() {
                doUnanimate(c, timestamp);

                c.removeChangedSVars(timestamp, 0);
                c.removeChangedName(timestamp, 0);
                c.updateStateForView();

                game.fireEvent(new GameEventCardStatsChanged(c));
            }
        };

        if (sa.hasParam("RevertCost")) {
            final ManaCost cost = new ManaCost(new ManaCostParser(sa.getParam("RevertCost")));
            final String desc = sa.getStackDescription();
            final SpellAbility revertSA = new AbilityStatic(c, cost) {
                @Override
                public void resolve() {
                    unanimate.run();
                }
                @Override
                public String getDescription() {
                    return cost + ": End Effect: " + desc;
                }
            };
            addedAbilities.add(revertSA);
        }

        // after unanimate to add RevertCost
        if (removeAll || removeNonManaAbilities
                || !addedAbilities.isEmpty() || !removedAbilities.isEmpty() || !addedTriggers.isEmpty()
                || !addedReplacements.isEmpty() || !addedStaticAbilities.isEmpty()) {
            c.addChangedCardTraits(addedAbilities, removedAbilities, addedTriggers, addedReplacements,
                addedStaticAbilities, removeAll, removeNonManaAbilities, timestamp, 0);
            if (perpetual) {
                Map <String, Object> params = new HashMap<>();
                params.put("Timestamp", timestamp);
                params.put("Category", "Abilities");
                c.addPerpetual(params);
            }
        }

        if (!"Permanent".equals(duration) && !perpetual) {
            if ("UntilControllerNextUntap".equals(duration)) {
                game.getUntap().addUntil(c.getController(), unanimate);
            } else {
                addUntilCommand(sa, unanimate);
            }
        }
    }

    /**
     * <p>
     * doUnanimate.
     * </p>
     *
     * @param c
     *            a {@link forge.game.card.Card} object.
     *            a {@link java.util.ArrayList} object.
     * @param colorDesc
     *            a {@link java.lang.String} object.
     * @param addedAbilities
     *            a {@link java.util.ArrayList} object.
     * @param addedTriggers
     *            a {@link java.util.ArrayList} object.
     * @param timestamp
     *            a long.
     */
    static void doUnanimate(final Card c, final long timestamp) {
        c.removeNewPT(timestamp, 0);

        c.removeChangedCardKeywords(timestamp, 0);

        c.removeChangedCardTypes(timestamp, 0);
        c.removeColor(timestamp, 0);

        c.removeChangedCardTraits(timestamp, 0);

        c.removeCantHaveKeyword(timestamp);

        c.removeHiddenExtrinsicKeywords(timestamp, 0);
    }

    static void handleColors(final Card c, final ColorSet colors, final long timestamp, final boolean overwrite, 
                                final boolean perpetual) {
        if (perpetual) {
            Map <String, Object> params = new HashMap<>();
            params.put("Colors", colors);
            params.put("Overwrite", overwrite);
            params.put("Timestamp", timestamp);
            params.put("Category", "Colors");
            c.addPerpetual(params);
        }
        c.addColor(colors, !overwrite, timestamp, 0, false);
    }

}
