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

import com.google.common.collect.Lists;
import forge.GameCommand;
import forge.card.CardType;
import forge.card.ColorSet;
import forge.card.RemoveType;
import forge.card.mana.ManaCost;
import forge.game.CardTraitBase;
import forge.game.Game;
import forge.game.ability.AbilityFactory;
import forge.game.ability.AbilityUtils;
import forge.game.ability.SpellAbilityEffect;
import forge.game.card.Card;
import forge.game.card.CardTraitChanges;
import forge.game.card.ICardTraitChanges;
import forge.game.card.perpetual.*;
import forge.game.event.GameEventCardStatsChanged;
import forge.game.keyword.Keyword;
import forge.game.replacement.ReplacementEffect;
import forge.game.replacement.ReplacementHandler;
import forge.game.spellability.AbilityStatic;
import forge.game.spellability.SpellAbility;
import forge.game.staticability.StaticAbility;
import forge.game.trigger.Trigger;
import forge.game.trigger.TriggerHandler;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

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

        Predicate<CardTraitBase> removeAbilities = null;
        if (sa.hasParam("RemoveAllAbilities")) {
            removeAbilities = e -> true;
        } else if (sa.hasParam("RemoveNonManaAbilities")) {
            removeAbilities = Predicate.not(CardTraitBase::isManaAbility);
        }

        if (sa.hasParam("RememberAnimated")) {
            source.addRemembered(c);
        }

        final boolean wasCreature = c.isCreature();

        // Alchemy "incorporate" cost
        if (sa.hasParam("Incorporate")) {
            final ManaCost incMCost = new ManaCost(sa.getParam("Incorporate"));
            PerpetualIncorporate p = new PerpetualIncorporate(timestamp, incMCost);
            c.addPerpetual(p);
            p.applyEffect(c);
        }
        if (sa.hasParam("ManaCost")) {
            final ManaCost manaCost = new ManaCost(sa.getParam("ManaCost"));
            if (perpetual) {
                PerpetualManaCost p = new PerpetualManaCost(timestamp, manaCost);
                c.addPerpetual(p);
                p.applyEffect(c);
            }
        }
        
        if (!addType.isEmpty() || !removeType.isEmpty() || addAllCreatureTypes || !remove.isEmpty()) {
            if (perpetual) {
                c.addPerpetual(new PerpetualTypes(timestamp, addType, removeType, remove));
            }
            c.addChangedCardTypes(addType, removeType, addAllCreatureTypes, remove, timestamp, 0, true, false);
        }

        if (!keywords.isEmpty() || !removeKeywords.isEmpty() || removeAbilities != null) {
            if (perpetual) {
                c.addPerpetual(new PerpetualKeywords(timestamp, keywords, removeKeywords, removeAbilities != null));
            }
            c.addChangedCardKeywords(keywords, removeKeywords, removeAbilities != null, timestamp, null);
        }

        // do this after changing types in case it wasn't a creature before
        if (power != null || toughness != null) {
            if (perpetual) {
                c.addPerpetual(new PerpetualNewPT(timestamp, power, toughness));
            }
            c.addNewPT(power, toughness, timestamp, 0);
        } else if (!wasCreature && c.isCreature()) {
            c.updatePTforView();
        }

        if (sa.hasParam("CantHaveKeyword")) {
            c.addCantHaveKeyword(timestamp, Keyword.setValueOf(sa.getParam("CantHaveKeyword")));
        }

        if (!hiddenKeywords.isEmpty()) {
            c.addHiddenExtrinsicKeywords(timestamp, 0, hiddenKeywords);
        }

        if (colors != null) {
            final boolean overwrite = sa.hasParam("OverwriteColors");
            if (perpetual) {
                c.addPerpetual(new PerpetualColors(timestamp, colors, overwrite));
            }
            c.addColor(colors, !overwrite, timestamp, null);
        }

        if (sa.hasParam("LeaveBattlefield")) {
            addLeaveBattlefieldReplacement(c, sa, sa.getParam("LeaveBattlefield"));
        }

        // remove abilities
        final List<SpellAbility> removedAbilities = Lists.newArrayList();
        if (sa.hasParam("RemoveThisAbility")) {
            removedAbilities.add(sa.getOriginalAbility());
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
                c.updatePTforView();

                game.fireEvent(new GameEventCardStatsChanged(c));
            }
        };

        if (sa.hasParam("RevertCost")) {
            final ManaCost cost = new ManaCost(sa.getParam("RevertCost"));
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
        if (removeAbilities != null
                || !addedAbilities.isEmpty() || !removedAbilities.isEmpty() || !addedTriggers.isEmpty()
                || !addedReplacements.isEmpty() || !addedStaticAbilities.isEmpty()) {
            ICardTraitChanges changes = c.addChangedCardTraits(addedAbilities, removedAbilities, addedTriggers, addedReplacements,
                addedStaticAbilities, removeAbilities, timestamp, 0);
            if (perpetual) {
                c.addPerpetual(new PerpetualAbilities(timestamp, changes));
                if (changes instanceof  CardTraitChanges && ((CardTraitChanges) changes).containsCostChange()) {
                    c.calculatePerpetualAdjustedManaCost();
                }
            }
        }

        if (!"Permanent".equals(duration) && !perpetual) {
            if ("UntilAnimatedFaceup".equals(duration)) {
                c.addFaceupCommand(unanimate);
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
}
