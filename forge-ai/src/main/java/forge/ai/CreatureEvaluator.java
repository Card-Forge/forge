package forge.ai;

import forge.game.GameEntity;
import forge.game.ability.AbilityUtils;
import forge.game.ability.ApiType;
import forge.game.card.Card;
import forge.game.card.CounterEnumType;
import forge.game.cost.CostPayEnergy;
import forge.game.keyword.Keyword;
import forge.game.spellability.SpellAbility;
import forge.game.staticability.StaticAbilityAssignCombatDamageAsUnblocked;
import forge.game.staticability.StaticAbilityCantAttackBlock;
import forge.game.staticability.StaticAbilityMustAttack;
import forge.game.trigger.Trigger;
import forge.game.trigger.TriggerType;

import java.util.List;
import java.util.function.Function;

public class CreatureEvaluator implements Function<Card, Integer> {
    @Override
    public Integer apply(Card c) {
        return evaluateCreature(c);
    }

    public int evaluateCreature(final Card c) {
        return evaluateCreature(c, true, true);
    }
    public int evaluateCreature(final Card c, final boolean considerPT, final boolean considerCMC) {
        //Card shouldn't be null and AI shouldn't crash since this is just score
        if (c == null)
            return 0;
        int value = 80;
        if (!c.isToken()) {
            value += addValue(20, "non-token"); // tokens should be worth less than actual cards
        }
        int power = c.getNetCombatDamage();
        final int toughness = c.getNetToughness();

        // TODO replace with ReplacementEffect checks
        if (c.hasKeyword("Prevent all combat damage that would be dealt by CARDNAME.")
                || c.hasKeyword("Prevent all damage that would be dealt by CARDNAME.")
                || c.hasKeyword("Prevent all combat damage that would be dealt to and dealt by CARDNAME.")
                || c.hasKeyword("Prevent all damage that would be dealt to and dealt by CARDNAME.")) {
            power = 0;
        }

        if (considerPT) {
            value += addValue(power * 15, "power");
            value += addValue(toughness * 10, "toughness: " + toughness);

            // because backside is always stronger the potential makes it better than a single faced card
            if (c.hasKeyword(Keyword.DAYBOUND) && c.isDoubleFaced()) {
                value += addValue(power * 10, "transforming");
            }
        }
        if (considerCMC) {
            value += addValue(c.getCMC() * 5, "cmc");
        }

        // Evasion keywords
        if (c.hasKeyword(Keyword.FLYING)) {
            value += addValue(power * 10, "flying");
        }
        if (c.hasKeyword(Keyword.HORSEMANSHIP)) {
            value += addValue(power * 10, "horses");
        }

        if (StaticAbilityCantAttackBlock.cantBlockBy(c, null)) {
            value += addValue(power * 10, "unblockable");
        } else {
            if (StaticAbilityAssignCombatDamageAsUnblocked.assignCombatDamageAsUnblocked(c)
                    || StaticAbilityAssignCombatDamageAsUnblocked.assignCombatDamageAsUnblocked(c, false)) {
                value += addValue(power * 6, "thorns");
            }
            if (c.hasKeyword(Keyword.FEAR)) {
                value += addValue(power * 6, "fear");
            }
            if (c.hasKeyword(Keyword.INTIMIDATE)) {
                value += addValue(power * 6, "intimidate");
            }
            if (c.hasKeyword(Keyword.MENACE)) {
                value += addValue(power * 4, "menace");
            }
            if (c.hasKeyword(Keyword.SKULK)) {
                value += addValue(power * 3, "skulk");
            }
        }

        // Other good keywords
        if (power > 0) {
            if (c.hasKeyword(Keyword.DOUBLE_STRIKE)) {
                value += addValue(10 + (power * 15), "ds");
            } else if (c.hasKeyword(Keyword.FIRST_STRIKE)) {
                value += addValue(10 + (power * 5), "fs");
            }
            if (c.hasKeyword(Keyword.DEATHTOUCH)) {
                value += addValue(25, "dt");
            }
            if (c.hasKeyword(Keyword.LIFELINK)) {
                value += addValue(power * 10, "lifelink");
            }
            if (power > 1 && c.hasKeyword(Keyword.TRAMPLE)) {
                value += addValue((power - 1) * 5, "trample");
            }
            if (c.hasKeyword(Keyword.VIGILANCE)) {
                value += addValue((power * 5) + (toughness * 5), "vigilance");
            }
            if (c.hasKeyword(Keyword.INFECT)) {
                value += addValue(power * 15, "infect");
            }
            else if (c.hasKeyword(Keyword.WITHER)) {
                value += addValue(power * 10, "wither");
            }
            value += addValue(c.getKeywordMagnitude(Keyword.TOXIC) * 5, "toxic");
            value += addValue(c.getKeywordMagnitude(Keyword.AFFLICT) * 5, "afflict");
            value += addValue(c.getKeywordMagnitude(Keyword.RAMPAGE), "rampage");
        }

        value += addValue(c.getKeywordMagnitude(Keyword.ANNIHILATOR) * 50, "eldrazi");
        value += addValue(c.getKeywordMagnitude(Keyword.ABSORB) * 11, "absorb");

        // Keywords that may produce temporary or permanent buffs over time
        if (c.hasKeyword(Keyword.OUTLAST)) {
            value += addValue(10, "outlast");
        }
        value += addValue(c.getKeywordMagnitude(Keyword.BUSHIDO) * 16, "bushido");
        value += addValue(c.getAmountOfKeyword(Keyword.FLANKING) * 15, "flanking");
        value += addValue(c.getAmountOfKeyword(Keyword.EXALTED) * 15, "exalted");
        value += addValue(c.getAmountOfKeyword(Keyword.MELEE) * 18, "melee");
        value += addValue(c.getAmountOfKeyword(Keyword.PROWESS) * 5, "prowess");

        // Defensive Keywords
        if (c.hasKeyword(Keyword.REACH) && !c.hasKeyword(Keyword.FLYING)) {
            value += addValue(5, "reach");
        }
        if (c.hasKeyword("CARDNAME can block creatures with shadow as though they didn't have shadow.")) {
            value += addValue(3, "shadow-block");
        }

        // Protection
        if (c.hasKeyword(Keyword.INDESTRUCTIBLE)) {
            value += addValue(70, "darksteel");
        } else {
            value += addValue(20 * c.getCounters(CounterEnumType.SHIELD), "shielded");
        }
        if (c.hasKeyword("Prevent all damage that would be dealt to CARDNAME.")) {
            value += addValue(60, "cho-manno");
        } else if (c.hasKeyword("Prevent all combat damage that would be dealt to CARDNAME.")) {
            value += addValue(50, "fogbank");
        }
        if (c.hasKeyword(Keyword.HEXPROOF)) {
            value += addValue(35, "hexproof");
        } else if (c.hasKeyword(Keyword.SHROUD)) {
            value += addValue(30, "shroud");
        } else if (c.hasKeyword(Keyword.WARD)) {
            value += addValue(10, "ward");
        }
        if (c.hasKeyword(Keyword.PROTECTION)) {
            value += addValue(20, "protection");
        }

        // paired creatures are more valuable because they grant a bonus to the other creature
        if (c.isPaired()) {
            value += addValue(14, "paired");
        }

        if (c.hasEncodedCard()) {
            value += addValue(24, "encoded");
        }

        if (ComputerUtilCard.hasActiveUndyingOrPersist(c)) {
            value += addValue(30, "revive");
        }

        // Bad keywords
        if (c.hasKeyword(Keyword.DEFENDER) || c.hasKeyword("CARDNAME can't attack.")) {
            value -= subValue((power * 9) + 40, "defender");
        } else if (c.getSVar("SacrificeEndCombat").equals("True")) {
            value -= subValue(40, "sac-end");
        }
        if (c.isDetained()) {
            value = addValue(50 + (c.getCMC() * 5), "detained"); // reset everything - useless
        } else if (c.hasKeyword("CARDNAME can't attack or block.")) {
            value = addValue(50 + (c.getCMC() * 5), "useless"); // reset everything - useless
        } else if (c.hasKeyword("CARDNAME can't block.")) {
            value -= subValue(10, "cant-block");
        } else if (c.isGoaded()) {
            value -= subValue(5, "goaded");
        } else {
            // TODO lower the values slightly if they're only temporary
            List<GameEntity> mAEnt = StaticAbilityMustAttack.entitiesMustAttack(c);
            if (mAEnt.contains(c)) {
                value -= subValue(10, "must-attack");
            } else if (!mAEnt.isEmpty()) {
                value -= subValue(10, "must-attack-player");
            }/* else if (c.hasKeyword("CARDNAME can block only creatures with flying.")) {
            value -= subValue(toughness * 5, "reverse-reach");
        }//*/
        }

        if (c.hasSVar("DestroyWhenDamaged")) {
            value -= subValue((toughness - 1) * 9, "dies-to-dmg");
        }
        if (c.getSVar("Targeting").equals("Dies")) {
            value -= subValue(25, "dies");
        }

        if (c.isUntapped()) {
            value += addValue(1, "untapped");
        }

        if (!c.canUntap(c.getController(), true)) {
            if (c.isTapped()) {
                value = addValue(50 + (c.getCMC() * 5), "tapped-useless"); // reset everything - useless
            } else {
                value -= subValue(50, "doesnt-untap");
            }
        } else {
            value -= subValue(10 * c.getCounters(CounterEnumType.STUN), "stunned");
        }

        for (final SpellAbility sa : c.getSpellAbilities()) {
            if (sa.isAbility()) {
                value += addValue(evaluateSpellAbility(sa), "sa: " + sa);
            }
        }

        if (!c.getManaAbilities().isEmpty()) {
            value += addValue(10, "manadork");
        }

        // use scaling because the creature is only available halfway
        if (c.hasKeyword(Keyword.PHASING)) {
            value -= subValue(Math.max(20, value / 2), "phasing");
        }

        if (c.hasSVar("EndOfTurnLeavePlay")) {
            value -= subValue(50, "eot-leaves");
        } else {
            for (Trigger t : c.getTriggers()) {
                if (!TriggerType.Phase.equals(t.getMode())) {
                    continue;
                }
                if (!"Upkeep".equals(t.getParam("Phase"))) {
                    continue;
                }

                if (t.isKeyword(Keyword.CUMULATIVE_UPKEEP)) {
                    value -= subValue(30, "cupkeep");
                } else if (t.isKeyword(Keyword.ECHO) && c.cameUnderControlSinceLastUpkeep()) {
                    value -= subValue(10, "echo-unpaid");
                }
                if (t.isKeyword(Keyword.FADING)) {
                    value -= subValue(20 / (Math.max(1, c.isInPlay() ? c.getCounters(CounterEnumType.FADE) : c.getKeywordMagnitude(Keyword.FADING))), "fading");
                }
                if (t.isKeyword(Keyword.VANISHING)) {
                    value -= subValue(20 / (Math.max(1, c.isInPlay() ? c.getCounters(CounterEnumType.TIME) : c.getKeywordMagnitude(Keyword.VANISHING))), "vanishing");
                }

                SpellAbility ab = t.ensureAbility();
                if (ab == null) {
                    continue;
                }
                if (ApiType.DealDamage.equals(ab.getApi())) {
                    if (!"You".equals(ab.getParamOrDefault("Defined", "You"))) {
                        continue;
                    }
                    if (c.getController().canLoseLife()) {
                        value -= subValue(20, "upkeep-dmg");
                    }
                } else if (ApiType.Sacrifice.equals(ab.getApi())) {
                    if (!ab.hasParam("UnlessCost")) {
                        continue;
                    }
                    value -= subValue(20, "sac-unless");
                }
            }
        }

        // card-specific evaluation modifier
        if (c.hasSVar("AIEvaluationModifier")) {
            value += AbilityUtils.calculateAmount(c, c.getSVar("AIEvaluationModifier"), null);
        }

        return value;
    }

    private int evaluateSpellAbility(SpellAbility sa) {
        // Pump abilities
        if (sa.getApi() == ApiType.Pump) {
            // Pump abilities that grant +X/+X to the card
            if ("+X".equals(sa.getParam("NumAtt"))
                    && "+X".equals(sa.getParam("NumDef"))
                    && !sa.usesTargeting()
                    && (!sa.hasParam("Defined") || "Self".equals(sa.getParam("Defined")))) {
                if (sa.getPayCosts().hasOnlySpecificCostType(CostPayEnergy.class)) {
                    // Electrostatic Pummeler, can be expanded for similar cards
                    int initPower = sa.getHostCard().getNetPower();
                    int pumpedPower = initPower;
                    int energy = sa.getHostCard().getController().getCounters(CounterEnumType.ENERGY);
                    if (energy > 0) {
                        int numActivations = energy / 3;
                        for (int i = 0; i < numActivations; i++) {
                            pumpedPower *= 2;
                        }
                        return (pumpedPower - initPower) * 15;
                    }
                }
            }
        } else if (ComputerUtilCost.isSacrificeSelfCost(sa.getPayCosts())) {
            return -10; // can be sacrificed in response to ability or spell, thus, less prioritable
        }
        // default value
        return 10;
    }

    protected int addValue(int value, String text) {
        return value;
    }
    protected int subValue(int value, String text) {
        return -addValue(-value, text);
    }
}
