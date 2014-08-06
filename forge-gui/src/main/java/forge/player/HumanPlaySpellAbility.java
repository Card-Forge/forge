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
package forge.player;

import com.google.common.collect.Iterables;

import forge.card.CardType;
import forge.card.MagicColor;
import forge.game.Game;
import forge.game.GameObject;
import forge.game.ability.AbilityUtils;
import forge.game.card.Card;
import forge.game.cost.CostPartMana;
import forge.game.cost.CostPayment;
import forge.game.mana.ManaPool;
import forge.game.player.Player;
import forge.game.player.PlayerController;
import forge.game.spellability.AbilitySub;
import forge.game.spellability.Spell;
import forge.game.spellability.SpellAbility;
import forge.game.spellability.TargetRestrictions;
import forge.game.zone.Zone;

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * <p>
 * SpellAbility_Requirements class.
 * </p>
 * 
 * @author Forge
 * @version $Id: HumanPlaySpellAbility.java 24317 2014-01-17 08:32:39Z Max mtg $
 */
public class HumanPlaySpellAbility {
    private final SpellAbility ability;
    private final CostPayment payment;

    public HumanPlaySpellAbility(final SpellAbility sa, final CostPayment cp) {
        this.ability = sa;
        this.payment = cp;
    }

    public final void playAbility(boolean mayChooseTargets, boolean isFree, boolean skipStack) {
        final Player human = ability.getActivatingPlayer();
        final Game game = ability.getActivatingPlayer().getGame();
        
        // used to rollback
        Zone fromZone = null;
        int zonePosition = 0;
        final ManaPool manapool = human.getManaPool();

        final Card c = this.ability.getHostCard();
        boolean manaConversion = (ability.isSpell() && c.hasKeyword("May spend mana as though it were mana of any color to cast CARDNAME"));
        boolean playerManaConversion = human.hasManaConversion()
                && human.getController().confirmAction(ability, null, "Do you want to spend mana as though it were mana of any color to pay the cost?");
        if (this.ability instanceof Spell && !c.isCopiedSpell()) {
            fromZone = game.getZoneOf(c);
            if (fromZone != null) {
            	zonePosition = fromZone.getCards().indexOf(c);
            }
            this.ability.setHostCard(game.getAction().moveToStack(c));
        }

        // freeze Stack. No abilities should go onto the stack while I'm filling requirements.
        game.getStack().freezeStack();

        if (manaConversion) {
            AbilityUtils.applyManaColorConversion(human, MagicColor.Constant.ANY_MANA_CONVERSION);
        }
        if (playerManaConversion) {
            AbilityUtils.applyManaColorConversion(human, MagicColor.Constant.ANY_MANA_CONVERSION);
            human.incNumManaConversion();
        }
        // This line makes use of short-circuit evaluation of boolean values, that is each subsequent argument
        // is only executed or evaluated if the first argument does not suffice to determine the value of the expression
        boolean prerequisitesMet = this.announceValuesLikeX()
                && this.announceType()
                && (!mayChooseTargets || setupTargets()) // if you can choose targets, then do choose them.
                && (isFree || this.payment.payCost(new HumanCostDecision(human, ability, ability.getHostCard())));

        if (!prerequisitesMet) {
            if (!ability.isTrigger()) {
                rollbackAbility(fromZone, zonePosition);
                if (ability.getHostCard().isMadness()) {
                    // if a player failed to play madness cost, move the card to graveyard
                    game.getAction().moveToGraveyard(c);
                    ability.getHostCard().setMadness(false);
                } else if (ability.getHostCard().isBestowed()) {
                    ability.getHostCard().unanimateBestow();
                }
            }
            if (manaConversion) {
                manapool.restoreColorReplacements();
            }
            if (playerManaConversion) {
                manapool.restoreColorReplacements();
                human.decNumManaConversion();
            }
            return;
        }

        if (isFree || this.payment.isFullyPaid()) {
            if (skipStack) {
                AbilityUtils.resolve(this.ability);
            }
            else {
                this.enusureAbilityHasDescription(this.ability);
                game.getStack().addAndUnfreeze(this.ability);
            }

            // no worries here. The same thread must resolve, and by this moment ability will have been resolved already
            // Triggers haven't resolved yet ??
            if (mayChooseTargets) {
                clearTargets(ability);
            }
            if (manaConversion) {
                manapool.restoreColorReplacements();
            }
        }
    }

    private final boolean setupTargets() {
        // Skip to paying if parent ability doesn't target and has no subAbilities.
        // (or trigger case where its already targeted)
        SpellAbility currentAbility = ability;
        final Card source = ability.getHostCard();
        do {
            TargetRestrictions tgt = currentAbility.getTargetRestrictions();
            if (tgt != null && tgt.doesTarget()) {
                clearTargets(currentAbility);
                Player targetingPlayer;
                if (currentAbility.hasParam("TargetingPlayer")) {
                    List<Player> candidates = AbilityUtils.getDefinedPlayers(source, currentAbility.getParam("TargetingPlayer"), currentAbility);
                    // activator chooses targeting player
                    targetingPlayer = ability.getActivatingPlayer().getController().chooseSingleEntityForEffect(
                            candidates, currentAbility, "Choose the targeting player");
                } else {
                    targetingPlayer = ability.getActivatingPlayer();
                }
                currentAbility.setTargetingPlayer(targetingPlayer);
                if (!targetingPlayer.getController().chooseTargetsFor(currentAbility))
                    return false;
            }
            final SpellAbility subAbility = currentAbility.getSubAbility();
            if (subAbility != null) {
                // This is necessary for "TargetsWithDefinedController$ ParentTarget"
                ((AbilitySub) subAbility).setParent(currentAbility);
            }
            currentAbility = subAbility;
        } while (currentAbility != null);
        return true;
    }

    public final void clearTargets(SpellAbility ability) {
        TargetRestrictions tg = ability.getTargetRestrictions();
        if (tg != null) {
            ability.resetTargets();
            tg.calculateStillToDivide(ability.getParam("DividedAsYouChoose"), ability.getHostCard(), ability);
        }
    }

    private void rollbackAbility(Zone fromZone, int zonePosition) { 
        // cancel ability during target choosing
        final Game game = ability.getActivatingPlayer().getGame();

        if (fromZone != null) { // and not a copy
            // add back to where it came from
            game.getAction().moveTo(fromZone, ability.getHostCard(), zonePosition >= 0 ? Integer.valueOf(zonePosition) : null);
        }

        clearTargets(ability);

        this.ability.resetOnceResolved();
        this.payment.refundPayment();
        game.getStack().clearFrozen();
    }

    private boolean announceValuesLikeX() {
        // Announcing Requirements like Choosing X or Multikicker
        // SA Params as comma delimited list
        String announce = ability.getParam("Announce");
        if (announce != null) {
            for(String aVar : announce.split(",")) {
                String varName = aVar.trim();

                boolean isX = "X".equalsIgnoreCase(varName);
                CostPartMana manaCost = ability.getPayCosts().getCostMana();
                boolean allowZero = !ability.hasParam("XCantBe0") && (!isX || manaCost == null || manaCost.canXbe0());

                Integer value = ability.getActivatingPlayer().getController().announceRequirements(ability, varName, allowZero);
                if (value == null) {
                    return false;
                }

                ability.setSVar(varName, value.toString());
                if ("Multikicker".equals(varName)) {
                    ability.getHostCard().setKickerMagnitude(value);
                }
                else {
                    ability.getHostCard().setSVar(varName, value.toString());
                }
            }
        }
        return true;
    }

    private boolean announceType() {
     // Announcing Requirements like choosing creature type or number
        String announce = ability.getParam("AnnounceType");
        PlayerController pc = ability.getActivatingPlayer().getController();
        if (announce != null) {
            for(String aVar : announce.split(",")) {
                String varName = aVar.trim();
                if ("CreatureType".equals(varName)) {
                    String choice = pc.chooseSomeType("Creature", ability, CardType.getCreatureTypes(), new ArrayList<String>());
                    ability.getHostCard().setChosenType(choice);
                }
                if ("ChooseNumber".equals(varName)) {
                    int min = Integer.parseInt(ability.getParam("Min"));
                    int max = Integer.parseInt(ability.getParam("Max"));
                    int i = ability.getActivatingPlayer().getController().chooseNumber(ability,
                            "Choose a number", min, max);
                    ability.getHostCard().setChosenNumber(i);
                }
            }
        }
        return true;
    }

    private void enusureAbilityHasDescription(SpellAbility ability) {
        if (!StringUtils.isBlank(ability.getStackDescription())) {
            return;
        }

        // For older abilities that don't setStackDescription set it here
        final StringBuilder sb = new StringBuilder();
        sb.append(ability.getHostCard().getName());
        if (ability.getTargetRestrictions() != null) {
            final Iterable<GameObject> targets = ability.getTargets().getTargets();
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
