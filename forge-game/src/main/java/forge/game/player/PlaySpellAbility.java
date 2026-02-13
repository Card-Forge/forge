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
package forge.game.player;

import com.google.common.collect.Iterables;
import forge.card.CardStateName;
import forge.card.CardType;
import forge.game.Game;
import forge.game.GameActionUtil;
import forge.game.GameObject;
import forge.game.ability.AbilityUtils;
import forge.game.ability.ApiType;
import forge.game.ability.effects.CharmEffect;
import forge.game.card.Card;
import forge.game.card.CardPlayOption;
import forge.game.cost.Cost;
import forge.game.cost.CostPayment;
import forge.game.mana.ManaPool;
import forge.game.spellability.OptionalCostValue;
import forge.game.spellability.SpellAbility;
import forge.game.staticability.StaticAbilityManaConvert;
import forge.game.zone.Zone;
import forge.game.zone.ZoneType;
import forge.util.Localizer;
import org.apache.commons.lang3.Range;
import org.apache.commons.lang3.StringUtils;

import java.util.List;

/**
 * <p>
 * SpellAbility_Requirements class.
 * </p>
 *
 * @author Forge
 * @version $Id: HumanPlaySpellAbility.java 24317 2014-01-17 08:32:39Z Max mtg $
 */
public class PlaySpellAbility {
    private final PlayerController controller;
    private SpellAbility ability;
    private boolean needX = true;

    public PlaySpellAbility(final PlayerController controller, final SpellAbility ability) {
        this.controller = controller;
        this.ability = ability;
    }

    /**
     * <p>
     * playSpellAbility.
     * </p>
     *
     * @param sa
     *            a {@link SpellAbility} object.
     */
    public static boolean playSpellAbility(final PlayerController controller, final Player p, SpellAbility sa) {
        //FThreads.assertExecutedByEdt(false); //TODO: Find a new home for this.

        // Should I be storing state here? It should be the same as last stored state though?

        Card source = sa.getHostCard();
        sa.setActivatingPlayer(p);

        if (sa.isLandAbility()) {
            if (sa.canPlay()) {
                sa.resolve();
            }
            return true;
        }

        boolean castFaceDown = sa.isCastFaceDown();
        boolean flippedToCast = sa.isSpell() && source.isFaceDown();

        sa = chooseOptionalAdditionalCosts(p, sa);
        if (sa == null) {
            return false;
        }

        final CardStateName oldState = source.getCurrentStateName();
        source.setSplitStateToPlayAbility(sa);

        // extra play check
        if (sa.isSpell() && !sa.canPlay()) {
            // in case human won't pay optional cost
            if (source.getCurrentStateName() != oldState) {
                source.setState(oldState, true);
            }
            return false;
        }

        if (flippedToCast && !castFaceDown) {
            source.forceTurnFaceUp();
        }

        final PlaySpellAbility req = new PlaySpellAbility(controller, sa);
        if (!req.playAbility(true, false, false)) {
            if (!controller.getGame().EXPERIMENTAL_RESTORE_SNAPSHOT) {
                Card rollback = p.getGame().getCardState(source);
                if (castFaceDown) {
                    rollback.setFaceDown(false);
                    rollback.updateStateForView();
                } else if (flippedToCast) {
                    // need to get the changed card if able
                    rollback.turnFaceDown(true);
                    if (rollback.isInZone(ZoneType.Exile)) {
                        rollback.addMayLookFaceDownExile(p);
                    }
                }
            }

            return false;
        }
        return true;
    }

    /**
     * choose optional additional costs. For HUMAN only
     * @param p
     *
     * @param original
     *            the original sa
     * @return an ArrayList<SpellAbility>.
     */
    static SpellAbility chooseOptionalAdditionalCosts(Player p, final SpellAbility original) {
        PlayerController c = p.getController();

        // choose alternative additional cost
        final List<SpellAbility> abilities = GameActionUtil.getAdditionalCostSpell(original);

        final SpellAbility choosen = c.getAbilityToPlay(original.getHostCard(), abilities);

        List<OptionalCostValue> list = GameActionUtil.getOptionalCostValues(choosen);
        if (!list.isEmpty()) {
            list = c.chooseOptionalCosts(choosen, list);
        }

        return GameActionUtil.addOptionalCosts(choosen, list);
    }

    public final boolean playAbility(final boolean mayChooseTargets, final boolean isFree, final boolean skipStack) {
        final Player player = ability.getActivatingPlayer();
        final Game game = player.getGame();
        boolean refreeze = game.getStack().isFrozen();

        if (!skipStack) {
            if (!refreeze) {
                // CR 401.5: freeze top library cards until cast/activated so player can't cheat and see the next
                game.setTopLibsCast();
            }

            if (ability.getApi() == ApiType.Charm) {
                if (ability.isAnnouncing("X")) {
                    needX = ability.getPayCosts().hasXInAnyCostPart();
                    // CR 601.4
                    if (!announceValuesLikeX()) {
                        game.clearTopLibsCast(ability);
                        return false;
                    }
                }
                if (!CharmEffect.makeChoices(ability)) {
                    game.clearTopLibsCast(ability);
                    // CR 603.3c If no mode is chosen, the ability is removed from the stack.
                    return false;
                }
            }

            ability = AbilityUtils.addSpliceEffects(ability);
        }

        // used to rollback
        Zone fromZone = null;
        int zonePosition = 0;
        final ManaPool manapool = player.getManaPool();

        final Card c = ability.getHostCard();
        final CardPlayOption option = c.mayPlay(ability.getMayPlay());

        if (ability.isSpell() && !c.isCopiedSpell()) {
            fromZone = game.getZoneOf(c);
            if (fromZone != null) {
                zonePosition = fromZone.getCards().indexOf(c);
            }
            ability.setHostCard(game.getAction().moveToStack(c, ability));
            ability.changeText();
        }

        if (!ability.isCopied()) {
            ability.resetPaidHash();
            ability.setPaidLife(0);
        }

        if (ability.isSpell() && !c.isCopiedSpell()) {
            ability = GameActionUtil.addExtraKeywordCost(ability);
        }

        Cost abCost = ability.getPayCosts();
        CostPayment payment = new CostPayment(abCost, ability);

        boolean manaColorConversion = false;

        if (!ability.isCopied()) {
            if (ability.isSpell()) { // Apply by Option
                if (option != null && option.applyManaConvert(payment)) {
                    manaColorConversion = true;
                }

                if (option != null && option.isIgnoreSnowSourceManaCostColor()) {
                    payment.setSnowForColor(true);
                }
            }

            if (ability.isActivatedAbility() && ability.getGrantorStatic() != null && ability.getGrantorStatic().hasParam("ManaConversion")) {
                AbilityUtils.applyManaColorConversion(payment, ability.getGrantorStatic().getParam("ManaConversion"));
                manaColorConversion = true;
            }

            if (StaticAbilityManaConvert.manaConvert(payment, player, ability.getHostCard(), ability)) {
                manaColorConversion = true;
            }

            if (ability.hasParam("ManaConversion")) {
                AbilityUtils.applyManaColorConversion(payment, ability.getParam("ManaConversion"));
                manaColorConversion = true;
            }
        }

        // reset is also done early here, because if an ability is canceled from targeting it might otherwise lead to refunding mana from earlier cast
        ability.clearManaPaid();
        ability.getPayingManaAbilities().clear();

        // This line makes use of short-circuit evaluation of boolean values, that is each subsequent argument
        // is only executed or evaluated if the first argument does not suffice to determine the value of the expression
        // because of Selective Snare do announceType first

        boolean preCostRequisites = announceType() && announceValuesLikeX() &&
            ability.checkRestrictions(player) &&
            (!mayChooseTargets || ability.setupTargets()) &&
            ability.canCastTiming(player) &&
            ability.isLegalAfterStack();

        // Freeze the stack just before we start paying costs but after the ability is fully set up
        game.getStack().freezeStack(ability);
        final boolean prerequisitesMet = preCostRequisites && (isFree || payment.payCost(controller.getCostDecisionMaker(player, ability, ability.isTrigger())));

        game.clearTopLibsCast(ability);

        if (!prerequisitesMet) {
            // Would love to restore game state when undoing a trigger rather than just declining all costs.
            // Is there a way to tell the difference?

            if (ability.isTrigger()) {
                // Only roll back triggers if they were not paid for
                if (game.EXPERIMENTAL_RESTORE_SNAPSHOT && preCostRequisites) {
                    GameActionUtil.rollbackAbility(ability, fromZone, zonePosition, payment, c);
                } else {
                    // If precost requsities failed, then there probably isn't anything to refund during experimental
                    payment.refundPayment();
                }
            } else {
                GameActionUtil.rollbackAbility(ability, fromZone, zonePosition, payment, c);
            }

            if (!refreeze) {
                game.getStack().unfreezeStack();
            }

            // These restores may not need to happen if we're restoring from snapshot
            if (manaColorConversion) {
                manapool.restoreColorReplacements();
            }

            return false;
        }

        if (isFree || payment.isFullyPaid()) {
            //track when planeswalker ultimates are activated
            player.getAchievementTracker().onSpellAbilityPlayed(ability);

            if (skipStack) {
                AbilityUtils.resolve(ability);
                // Should unfreeze stack (but if it was a RE with a cause better to let it be handled by that)
                if (!ability.isReplacementAbility()) {
                    game.getStack().unfreezeStack();
                }
            } else {
                ensureAbilityHasDescription(ability);
                game.getStack().addAndUnfreeze(ability);
            }

            if (manaColorConversion) {
                manapool.restoreColorReplacements();
            }
        }
        return true;
    }

    private boolean announceValuesLikeX() {
        if (ability.isCopied() || ability.isWrapper()) { return true; } //don't re-announce for spell copies

        final Cost cost = ability.getPayCosts();
        final Card card = ability.getHostCard();

        // Announcing Requirements like Choosing X or Multikicker
        // SA Params as comma delimited list
        final String announce = ability.getParam("Announce");
        if (announce != null && needX) {
            for (final String aVar : announce.split(",")) {
                final String varName = aVar.trim();

                final Integer value = controller.announceRequirements(ability, varName);
                if (value == null) {
                    return false;
                }

                if ("X".equalsIgnoreCase(varName)) {
                    needX = false;
                    ability.setXManaCostPaid(value);
                } else {
                    ability.setSVar(varName, value.toString());
                    card.setSVar(varName, value.toString());
                }
            }
        }

        if (needX) {
            if (cost.hasXInAnyCostPart()) {
                final String sVar = ability.getParamOrDefault("XAlternative", ability.getSVar("X")); //only prompt for new X value if card doesn't determine it another way
                // check if X != 0 is even allowed or the X shard got removed
                boolean replacedXshard = ability.isSpell() && ability.getHostCard().getManaCost().countX() > 0 && !cost.hasXInAnyCostPart();
                if (("Count$xPaid".equals(sVar) && !replacedXshard) || sVar.isEmpty()) {
                    final Integer value = controller.announceRequirements(ability, "X");
                    if (value == null) {
                        return false;
                    }
                    ability.setXManaCostPaid(value);
                }
            } else {
                ability.setXManaCostPaid(null);
            }
        }
        return true;
    }

    // Announcing Requirements like choosing creature type or number
    private boolean announceType() {
        if (ability.isCopied()) { return true; } //don't re-announce for spell copies

        final String announce = ability.getParam("AnnounceType");
        final PlayerController pc = ability.getActivatingPlayer().getController();
        if (announce != null) {
            for (final String aVar : announce.split(",")) {
                final String varName = aVar.trim();
                if ("CreatureType".equals(varName)) {
                    final String choice = pc.chooseSomeType("Creature", ability, CardType.getAllCreatureTypes());
                    ability.getHostCard().setChosenType(choice);
                }
                if ("ChooseNumber".equals(varName)) {
                    final int min = Integer.parseInt(ability.getParam("Min"));
                    final int max = Integer.parseInt(ability.getParam("Max"));
                    final int i = ability.getActivatingPlayer().getController().chooseNumber(ability,
                            Localizer.getInstance().getMessage("lblChooseNumber") , min, max);
                    ability.getHostCard().setChosenNumber(i);
                }
                if ("Opponent".equals(varName)) {
                    Player opp = ability.getActivatingPlayer().getController().chooseSingleEntityForEffect(ability.getActivatingPlayer().getOpponents(), ability, Localizer.getInstance().getMessage("lblChooseAnOpponent"), null);
                    ability.getHostCard().setChosenPlayer(opp);
                }
            }
        }
        return true;
    }

    private static void ensureAbilityHasDescription(final SpellAbility ability) {
        if (!StringUtils.isBlank(ability.getStackDescription())) {
            return;
        }

        // For older abilities that don't setStackDescription set it here
        final StringBuilder sb = new StringBuilder();
        sb.append(ability.getHostCard().getDisplayName());
        if (ability.usesTargeting()) {
            final Iterable<GameObject> targets = ability.getTargets();
            if (!Iterables.isEmpty(targets)) {
                sb.append(" - Targeting ");
                for (final GameObject o : targets) {
                    sb.append(o.toString()).append(" ");
                }
            }
        }

        ability.setStackDescription(sb.toString());
    }
}
