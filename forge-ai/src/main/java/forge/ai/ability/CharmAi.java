package forge.ai.ability;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.google.common.collect.Lists;

import forge.ai.AiController;
import forge.ai.AiPlayDecision;
import forge.ai.ComputerUtilAbility;
import forge.ai.PlayerControllerAi;
import forge.ai.SpellAbilityAi;
import forge.game.ability.AbilityUtils;
import forge.game.ability.effects.CharmEffect;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.spellability.AbilitySub;
import forge.game.spellability.SpellAbility;
import forge.util.Aggregates;
import forge.util.MyRandom;
import forge.util.collect.FCollection;

public class CharmAi extends SpellAbilityAi {
    @Override
    protected boolean checkApiLogic(Player ai, SpellAbility sa) {
        final Card source = sa.getHostCard();
        List<AbilitySub> choices = CharmEffect.makePossibleOptions(sa);

        final int num;
        final int min;
        if (sa.isEntwine()) {
            num = min = choices.size();
        } else {
            num = AbilityUtils.calculateAmount(source, sa.getParamOrDefault("CharmNum", "1"), sa);
            min = sa.hasParam("MinCharmNum") ? AbilityUtils.calculateAmount(source, sa.getParam("MinCharmNum"), sa) : num;
        }

        boolean timingRight = sa.isTrigger(); //is there a reason to play the charm now?

        // Reset the chosen list otherwise it will be locked in forever by earlier calls
        sa.setChosenList(null);
        sa.setSubAbility(null);
        List<AbilitySub> chosenList;
        
        if (!ai.equals(sa.getActivatingPlayer())) {
            // This branch is for "An Opponent chooses" Charm spells from Alliances
            // Current just choose the first available spell, which seem generally less disastrous for the AI.
            chosenList = choices.subList(1, choices.size());
        } else if ("Triskaidekaphobia".equals(ComputerUtilAbility.getAbilitySourceName(sa))) {
            chosenList = chooseTriskaidekaphobia(choices, ai);
        } else {
            // only randomize if not all possible together
            if (num < choices.size()) {
                Collections.shuffle(choices);
            }

            /*
             * The generic chooseOptionsAi uses canPlayAi() to determine good choices
             * which means most "bonus" effects like life-gain and random pumps will
             * usually not be chosen. This is designed to force the AI to only select
             * the best choice(s) since it does not actually know if it can pay for
             * "bonus" choices (eg. Entwine/Escalate).
             * chooseMultipleOptionsAi() uses "AILogic$Good" tags to manually identify
             * bonus choice(s) for the AI otherwise it might be too hard to ever fulfil
             * minimum choice requirements with canPlayAi() alone.
             */
            chosenList = min > 1 ? chooseMultipleOptionsAi(choices, ai, min)
                    : chooseOptionsAi(choices, ai, timingRight, num, min, sa.hasParam("CanRepeatModes"));
        }

        if (chosenList.isEmpty()) {
            if (timingRight) {
                // Set minimum choices for triggers where chooseMultipleOptionsAi() returns null
                chosenList = chooseOptionsAi(choices, ai, true, num, min, sa.hasParam("CanRepeatModes"));
                if (chosenList.isEmpty() && min != 0) {
                    return false;
                }
            } else {
                return false;
            }
        }

        // store the choices so they'll get reused
        sa.setChosenList(chosenList);
        if (sa.isSpell()) {
            // prebuild chain to improve cost calculation accuracy
            CharmEffect.chainAbilities(sa, chosenList);
        }

        // prevent run-away activations - first time will always return true
        return MyRandom.getRandom().nextFloat() <= Math.pow(.6667, sa.getActivationsThisTurn());
    }

    private List<AbilitySub> chooseOptionsAi(List<AbilitySub> choices, final Player ai, boolean isTrigger, int num,
            int min, boolean allowRepeat) {
        List<AbilitySub> chosenList = Lists.newArrayList();
        AiController aic = ((PlayerControllerAi) ai.getController()).getAi();
        // First pass using standard canPlayAi() for good choices
        for (AbilitySub sub : choices) {
            sub.setActivatingPlayer(ai, true);
            if (AiPlayDecision.WillPlay == aic.canPlaySa(sub)) {
                chosenList.add(sub);
                if (chosenList.size() == num) {
                    return chosenList; // maximum choices reached
                }
            }
        }
        if (isTrigger && chosenList.size() < min) {
            // Second pass using doTrigger(false) to fulfill minimum choice
            choices.removeAll(chosenList);
            for (AbilitySub sub : choices) {
                if (aic.doTrigger(sub, false)) {
                    chosenList.add(sub);
                    if (chosenList.size() == min) {
                        return chosenList;
                    }
                }
            }
            // Third pass using doTrigger(true) to force fill minimum choices
            if (chosenList.size() < min) {
                choices.removeAll(chosenList);
                for (AbilitySub sub : choices) {
                    if (aic.doTrigger(sub, true)) {
                        chosenList.add(sub);
                        if (chosenList.size() == min) {
                            break;
                        }
                    }
                }
            }
        }
        if (chosenList.size() < min) {
            chosenList.clear(); // not enough choices
        }
        return chosenList;
    }

    private List<AbilitySub> chooseTriskaidekaphobia(List<AbilitySub> choices, final Player ai) {
        List<AbilitySub> chosenList = Lists.newArrayList();
        if (choices == null || choices.isEmpty()) { return chosenList; }

        AbilitySub gain = choices.get(0);
        AbilitySub lose = choices.get(1);
        FCollection<Player> opponents = ai.getOpponents();

        boolean oppTainted = false;
        boolean allyTainted = ai.isCardInPlay("Tainted Remedy");
        final int aiLife = ai.getLife(); 

        //Check if Opponent controls Tainted Remedy
        for (Player p : opponents) {
            if (p.isCardInPlay("Tainted Remedy")) {
                oppTainted = true;
                break;
            }
        }
        // if ai or ally of ai does control Tainted Remedy, prefer gain life instead of lose
        if (!allyTainted) {
            for (Player p : ai.getAllies()) {
                if (p.isCardInPlay("Tainted Remedy")) {
                    allyTainted = true;
                    break;
                }
            }
        }
        
        if (!ai.canLoseLife() || ai.cantLose()) {
            // ai cant lose life, or cant lose the game, don't think about others
            chosenList.add(allyTainted ? gain : lose);
        } else if (oppTainted || ai.getGame().isCardInPlay("Rain of Gore")) {
            // Rain of Gore does negate lifegain, so don't benefit the others
            // same for if a opponent does control Tainted Remedy
            // but if ai cant gain life, the effects are negated
            chosenList.add(ai.canGainLife() ? lose : gain);
        } else if (ai.getGame().isCardInPlay("Sulfuric Vortex")) {
            // no life gain, but extra life loss.
            if (aiLife >= 17)
                chosenList.add(lose);
            // try to prevent to get to 13 with extra lose
            else if (aiLife < 13 || ((aiLife - 13) % 2) == 1) {
                chosenList.add(gain);
            } else {
                chosenList.add(lose);
            }
        } else if (ai.canGainLife() && aiLife <= 5) {
            // critical Life try to gain more
            chosenList.add(gain);
        } else if (!ai.canGainLife() && aiLife == 14) {
            // ai cant gain life, but try to avoid falling to 13
            // but if a opponent does control Tainted Remedy its irrelevant
            chosenList.add(oppTainted ? lose : gain);
        } else if (allyTainted) {
            // Tainted Remedy negation logic, try gain instead of lose
            // because negation does turn it into lose for opponents
            boolean oppCritical = false;
            // an opponent is Critical = 14, and can't gain life, try to lose life instead
            // but only if ai doesn't kill itself with that.
            if (aiLife != 14) {
                for (Player p : opponents) {
                    if (p.getLife() == 14 && !p.canGainLife() && p.canLoseLife()) {
                        oppCritical = true;
                        break;
                    }
                }
            }
            chosenList.add(aiLife == 12 || oppCritical ? lose : gain);
        } else {
            // normal logic, try to gain life if its critical
            boolean oppCritical = false;
            // an opponent is Critical = 12, and can gain life, try to gain life instead
            // but only if ai doesn't kill itself with that.
            if (aiLife != 12) {
                for (Player p : opponents) {
                    if (p.getLife() == 12 && p.canGainLife()) {
                        oppCritical = true;
                        break;
                    }
                }
            }
            chosenList.add(aiLife == 14 || aiLife <= 10 || oppCritical ? gain : lose);
        }
        return chosenList;
    }

    // Choice selection for charms that require multiple choices (eg. Cryptic Command, DTK commands)
    private List<AbilitySub> chooseMultipleOptionsAi(List<AbilitySub> choices, final Player ai, int min) {
        AbilitySub goodChoice = null;
        List<AbilitySub> chosenList = Lists.newArrayList();
        AiController aic = ((PlayerControllerAi) ai.getController()).getAi();
        for (AbilitySub sub : choices) {
            sub.setActivatingPlayer(ai, true);
            // Assign generic good choice to fill up choices if necessary 
            if ("Good".equals(sub.getParam("AILogic")) && aic.doTrigger(sub, false)) {
                goodChoice = sub;
            } else {
                // Standard canPlayAi()
                sub.setActivatingPlayer(ai, true);
                if (AiPlayDecision.WillPlay == aic.canPlaySa(sub)) {
                    chosenList.add(sub);
                    if (chosenList.size() == min) {
                        break; // enough choices
                    }
                }
            }
        }
        // Add generic good choice if one more choice is needed
        if (chosenList.size() == min - 1 && goodChoice != null) {
            chosenList.add(0, goodChoice);  // hack to make Dromoka's Command fight targets work
        }
        if (chosenList.size() != min) {
            chosenList.clear();
        }
        return chosenList;
    } 

    @Override
    public Player chooseSinglePlayer(Player ai, SpellAbility sa, Iterable<Player> opponents, Map<String, Object> params) {
        return Aggregates.random(opponents);
    }

    @Override
    public boolean chkDrawbackWithSubs(Player aiPlayer, AbilitySub ab) {
        // choices were already targeted
        if (ab.getRootAbility().getChosenList() != null) {
            return true;
        }
        return super.chkDrawbackWithSubs(aiPlayer, ab);
    }

}
