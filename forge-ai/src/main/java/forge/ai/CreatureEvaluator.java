package forge.ai;

import com.google.common.base.Function;

import forge.game.card.Card;
import forge.game.spellability.SpellAbility;

public class CreatureEvaluator implements Function<Card, Integer> {
    protected int getEffectivePower(final Card c) {
        return c.getNetCombatDamage();
    }
    protected int getEffectiveToughness(final Card c) {
        return c.getNetToughness();
    }

    @Override
    public Integer apply(Card c) {
        return evaluateCreature(c);
    }

    public int evaluateCreature(final Card c) {
        int value = 80;
        if (!c.isToken()) {
            value += addValue(20, "non-token"); // tokens should be worth less than actual cards
        }
        int power = getEffectivePower(c);
        final int toughness = getEffectiveToughness(c);
        for (String keyword : c.getKeywords()) {
            if (keyword.equals("Prevent all combat damage that would be dealt by CARDNAME.")
                    || keyword.equals("Prevent all damage that would be dealt by CARDNAME.")
                    || keyword.equals("Prevent all combat damage that would be dealt to and dealt by CARDNAME.")
                    || keyword.equals("Prevent all damage that would be dealt to and dealt by CARDNAME.")) {
                power = 0;
                break;
            }
        }
        value += addValue(power * 15, "power");
        value += addValue(toughness * 10, "toughness");
        value += addValue(c.getCMC() * 5, "cmc");
    
        // Evasion keywords
        if (c.hasKeyword("Flying")) {
            value += addValue(power * 10, "flying");
        }
        if (c.hasKeyword("Horsemanship")) {
            value += addValue(power * 10, "horses");
        }
        if (c.hasKeyword("Unblockable")) {
            value += addValue(power * 10, "unblockable");
        } else {
            if (c.hasKeyword("You may have CARDNAME assign its combat damage as though it weren't blocked.")) {
                value += addValue(power * 6, "thorns");
            }
            if (c.hasKeyword("Fear")) {
                value += addValue(power * 6, "fear");
            }
            if (c.hasKeyword("Intimidate")) {
                value += addValue(power * 6, "intimidate");
            }
            if (c.hasStartOfKeyword("CantBeBlockedBy")) {
                value += addValue(power * 3, "block-restrict");
            }
        }
    
        // Other good keywords
        if (power > 0) {
            if (c.hasKeyword("Double Strike")) {
                value += addValue(10 + (power * 15), "ds");
            } else if (c.hasKeyword("First Strike")) {
                value += addValue(10 + (power * 5), "fs");
            }
            if (c.hasKeyword("Deathtouch")) {
                value += addValue(25, "dt");
            }
            if (c.hasKeyword("Lifelink")) {
                value += addValue(power * 10, "lifelink");
            }
            if (power > 1 && c.hasKeyword("Trample")) {
                value += addValue((power - 1) * 5, "trample");
            }
            if (c.hasKeyword("Vigilance")) {
                value += addValue((power * 5) + (toughness * 5), "vigilance");
            }
            if (c.hasKeyword("Wither")) {
                value += addValue(power * 10, "Wither");
            }
            if (c.hasKeyword("Infect")) {
                value += addValue(power * 15, "infect");
            }
            value += addValue(c.getKeywordMagnitude("Rampage"), "rampage");
        }
    
        value += addValue(c.getKeywordMagnitude("Bushido") * 16, "bushido");
        value += addValue(c.getAmountOfKeyword("Flanking") * 15, "flanking");
        value += addValue(c.getAmountOfKeyword("Exalted") * 15, "exalted");
        value += addValue(c.getKeywordMagnitude("Annihilator") * 50, "eldrazi");
        value += addValue(c.getKeywordMagnitude("Absorb") * 11, "absorb");

        // Defensive Keywords
        if (c.hasKeyword("Reach") && !c.hasKeyword("Flying")) {
            value += addValue(5, "reach");
        }
        if (c.hasKeyword("CARDNAME can block creatures with shadow as though they didn't have shadow.")) {
            value += addValue(3, "shadow-block");
        }
    
        // Protection
        if (c.hasKeyword("Indestructible")) {
            value += addValue(70, "darksteel");
        }
        if (c.hasKeyword("Prevent all damage that would be dealt to CARDNAME.")) {
            value += addValue(60, "cho-manno");
        } else if (c.hasKeyword("Prevent all combat damage that would be dealt to CARDNAME.")) {
            value += addValue(50, "fogbank");
        }
        if (c.hasKeyword("Hexproof")) {
            value += addValue(35, "hexproof");
        } else if (c.hasKeyword("Shroud")) {
            value += addValue(30, "shroud");
        }
        if (c.hasStartOfKeyword("Protection")) {
            value += addValue(20, "protection");
        }
        if (c.hasStartOfKeyword("PreventAllDamageBy")) {
            value += addValue(10, "prevent-dmg");
        }
    
        // Bad keywords
        if (c.hasKeyword("Defender") || c.hasKeyword("CARDNAME can't attack.")) {
            value -= subValue((power * 9) + 40, "defender");
        } else if (c.getSVar("SacrificeEndCombat").equals("True")) {
            value -= subValue(40, "sac-end");
        }
        if (c.hasKeyword("CARDNAME can't block.")) {
            value -= subValue(10, "cant-block");
        } else if (c.hasKeyword("CARDNAME attacks each turn if able.")
                || c.hasKeyword("CARDNAME attacks each combat if able.")) {
            value -= subValue(10, "must-attack");
        } else if (c.hasStartOfKeyword("CARDNAME attacks specific player each combat if able")) {
            value -= subValue(10, "must-attack-player");
        } else if (c.hasKeyword("CARDNAME can block only creatures with flying.")) {
            value -= subValue(toughness * 5, "reverse-reach");
        }
    
        if (c.hasSVar("DestroyWhenDamaged")) {
            value -= subValue((toughness - 1) * 9, "dies-to-dmg");
        }
    
        if (c.hasKeyword("CARDNAME can't attack or block.")) {
            value = addValue(50 + (c.getCMC() * 5), "useless"); // reset everything - useless
        }
        if (c.hasKeyword("CARDNAME doesn't untap during your untap step.")) {
            if (c.isTapped()) {
                value = addValue(50 + (c.getCMC() * 5), "tapped-useless"); // reset everything - useless
            } else {
                value -= subValue(50, "doesnt-untap");
            }
        }
        if (c.hasSVar("EndOfTurnLeavePlay")) {
            value -= subValue(50, "eot-leaves");
        } else if (c.hasStartOfKeyword("Cumulative upkeep")) {
            value -= subValue(30, "cupkeep");
        } else if (c.hasStartOfKeyword("At the beginning of your upkeep, sacrifice CARDNAME unless you pay")) {
            value -= subValue(20, "sac-unless");
        } else if (c.hasStartOfKeyword("(Echo unpaid)")) {
            value -= subValue(10, "echo-unpaid");
        }
    
        if (c.hasStartOfKeyword("At the beginning of your upkeep, CARDNAME deals")) {
            value -= subValue(20, "upkeep-dmg");
        } 
        if (c.hasStartOfKeyword("Fading")) {
            value -= subValue(20, "fading");
        }
        if (c.hasStartOfKeyword("Vanishing")) {
            value -= subValue(20, "vanishing");
        }
        if (c.getSVar("Targeting").equals("Dies")) {
            value -= subValue(25, "dies");
        }
    
        for (final SpellAbility sa : c.getSpellAbilities()) {
            if (sa.isAbility()) {
                value += addValue(10, "sa: " + sa);
            }
        }
        if (!c.getManaAbilities().isEmpty()) {
            value += addValue(10, "manadork");
        }
    
        if (c.isUntapped()) {
            value += addValue(1, "untapped");
        }
    
        // paired creatures are more valuable because they grant a bonus to the other creature
        if (c.isPaired()) {
            value += addValue(14, "paired");
        }

        if (!c.getEncodedCards().isEmpty()) {
            value += addValue(24, "encoded");
        }
        return value;
    }
    
    protected int addValue(int value, String text) {
        return value;
    }
    protected int subValue(int value, String text) {
        return -addValue(-value, text);
    }
}
