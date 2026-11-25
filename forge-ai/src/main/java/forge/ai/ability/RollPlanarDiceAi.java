package forge.ai.ability;


import forge.ai.*;
import forge.card.MagicColor;
import forge.game.GameType;
import forge.game.ability.AbilityFactory;
import forge.game.ability.AbilityUtils;
import forge.game.ability.ApiType;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CardLists;
import forge.game.combat.CombatUtil;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;
import forge.util.TextUtil;

import java.util.List;
import java.util.Locale;

public class RollPlanarDiceAi extends SpellAbilityAi {
    /* (non-Javadoc)
     * @see forge.card.abilityfactory.SpellAiLogic#canPlayAI(forge.game.player.Player, java.util.Map, forge.card.spellability.SpellAbility)
     */
    @Override
    protected AiAbilityDecision canPlay(Player ai, SpellAbility sa) {
        if (ai.getGame().getActivePlanes() == null) {
            return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
        }
        
        for (Card c : ai.getGame().getActivePlanes()) {
            if (willRollOnPlane(ai, sa, c)) {
                return new AiAbilityDecision(100, AiPlayDecision.WillPlay);
            }
        }
        return new AiAbilityDecision(0, AiPlayDecision.CantPlayAi);
    }

    boolean willRollOnPlane(Player ai, SpellAbility sa, Card plane) {
        boolean decideToRoll = false;
        boolean rollInMain1 = false;
        // Rogue Commander can override the normal hints for a plane.
        final String hintSVar = ai.getGame().getRules().getGameType() == GameType.RogueCommander
                && plane.hasSVar("AIRollPlanarDieParamsRogueCommander")
                ? "AIRollPlanarDieParamsRogueCommander" : "AIRollPlanarDieParams";
        final boolean hinted = plane.hasSVar(hintSVar);
        // Hints without Mode disable rolling; planes without hints use profile-based rolling.
        String modeName = hinted ? "never" : "random";
        int maxActivations = AiProfileUtil.getIntProperty(ai, AiProps.DEFAULT_MAX_PLANAR_DIE_ROLLS_PER_TURN);
        int chance = AiProfileUtil.getIntProperty(ai, AiProps.DEFAULT_PLANAR_DIE_ROLL_CHANCE);
        int hesitationChance = AiProfileUtil.getIntProperty(ai, AiProps.PLANAR_DIE_ROLL_HESITATION_CHANCE);
        int minTurnToRoll = AiProfileUtil.getIntProperty(ai, AiProps.DEFAULT_MIN_TURN_TO_ROLL_PLANAR_DIE);
        
        if (hinted) {
            String[] params = plane.getSVar(hintSVar).trim().split("\\|");
            for (String param : params) {
                String[] paramData = param.toLowerCase(Locale.ROOT).split("\\$", 2);
                if (paramData.length < 2) {
                    // a malformed hint must not throw: an exception here makes the AI pass its whole priority
                    System.out.println(TextUtil.concatNoSpace("Malformed AI hint parameter in card ", plane.getName(), " in RollPlanarDiceAi: ", param, "."));
                    continue;
                }
                String paramName = paramData[0].trim();
                String paramValue = paramData[1].trim();

                switch (paramName) {
                    case "mode":
                        modeName = paramValue;
                        break;
                    case "chance":
                        chance = Integer.parseInt(paramValue);
                        break;
                    case "minturn":
                        minTurnToRoll = Integer.parseInt(paramValue);
                        break;
                    case "maxrollsperturn":
                        maxActivations = Integer.parseInt(paramValue);
                        break;
                    case "rollinmain1":
                        if (paramValue.equals("true")) {
                            rollInMain1 = true;
                        }
                        break;
                    case "lowpriority":
                        // this is handled in ComputerUtilAbility.SortKey
                        break;
                    case "cardsinhandle": // num of cards in hand less than or equal to N
                        if (ai.getCardsIn(ZoneType.Hand).size() > Integer.parseInt(paramValue)) {
                            return false;
                        }
                        break;
                    case "cardsinhandge": // num of cards in hand greater than or equal to N
                        if (ai.getCardsIn(ZoneType.Hand).size() < Integer.parseInt(paramValue)) {
                            return false;
                        }
                        break;
                    case "cardsingraveyardle":
                        if (ai.getCardsIn(ZoneType.Graveyard).size() > Integer.parseInt(paramValue)) {
                            return false;
                        }
                        break;
                    case "cardsingraveyardge":
                        if (ai.getCardsIn(ZoneType.Graveyard).size() < Integer.parseInt(paramValue)) {
                            return false;
                        }
                        break;
                    case "hascreatureinplay": // TODO: All abilities below only test the presence of the option. The value (true/false) is not yet tested.
                        if (!detectCreatureInZone(ai, ZoneType.Battlefield)) {
                            return false;
                        }
                        break;
                    case "opphascreatureinplay":
                        boolean oppHasCreature = false;
                        for (Player op : ai.getOpponents()) {
                            oppHasCreature |= detectCreatureInZone(op, ZoneType.Battlefield);
                        }
                        if (!oppHasCreature) {
                            return false;
                        }
                        break;
                    case "hascolorcreatureinplay":
                        if (!detectColorInZone(ai, paramValue, ZoneType.Battlefield, true)) {
                            return false;
                        }
                        break;
                    case "hascolorinplay":
                        if (!detectColorInZone(ai, paramValue, ZoneType.Battlefield, false)) {
                            return false;
                        }
                        break;
                    case "hascoloringraveyard":
                        if (!detectColorInZone(ai, paramValue, ZoneType.Graveyard, false)) {
                            return false;
                        }
                        break;
                    case "hasvalidcardinzone":
                        // Format: Zone:Validity[:MinimumCount], defaulting to at least one card.
                        String[] zoneAndValidity = param.substring(param.indexOf('$') + 1).trim().split(":", 3);
                        ZoneType zone = ZoneType.smartValueOf(zoneAndValidity[0].trim());
                        int minimumCount = zoneAndValidity.length > 2 ? Integer.parseInt(zoneAndValidity[2].trim()) : 1;
                        if ((zone != ZoneType.Battlefield && zone != ZoneType.Graveyard)
                                || CardLists.getValidCards(ai.getGame().getCardsIn(zone), zoneAndValidity[1].trim(), ai, plane, sa).size() < minimumCount) {
                            return false;
                        }
                        break;
                    case "hasvalidopponenttarget":
                        String abilityName = param.substring(param.indexOf('$') + 1).trim();
                        SpellAbility targetAbility = AbilityFactory.getAbility(plane, abilityName);
                        targetAbility.setActivatingPlayer(ai);
                        if (!targetAbility.usesTargeting() || targetAbility.getTargetRestrictions().getAllCandidates(targetAbility).stream()
                                .noneMatch(target -> target instanceof Player && ((Player) target).isOpponentOf(ai)
                                        || target instanceof Card && ((Card) target).getController().isOpponentOf(ai))) {
                            return false;
                        }
                        break;
                    case "hasavailableinstantsorcery":
                        if (paramValue.equals("true") && ComputerUtilAbility.getSpellAbilities(
                                ComputerUtilAbility.getAvailableCards(ai.getGame(), ai), ai).stream().noneMatch(ability ->
                                ability.isSpell() && (ability.getCardState().getType().isInstant()
                                || ability.getCardState().getType().isSorcery())
                                && !ComputerUtilCard.isCardRemAIDeck(ability.getHostCard())
                                && ability.canCastTiming(ai) && ComputerUtilAbility.isFullyTargetable(ability))) {
                            return false;
                        }
                        break;
                    case "hasattackablecreature":
                        if (paramValue.equals("true") && !CombatUtil.canAttack(ai)) {
                            return false;
                        }
                        break;
                    case "devotionexceedsrollcost":
                        if (paramValue.equals("true")) {
                            String color = ComputerUtilCard.getMostProminentColor(
                                    ai.getCardsIn(ZoneType.Battlefield), MagicColor.Constant.ONLY_COLORS);
                            int devotion = AbilityUtils.calculateAmount(plane, "Count$Devotion." + color, sa);
                            int rollCost = ComputerUtilMana.calculateManaCost(sa.getPayCosts(), sa, ai, true, 0, false)
                                    .getConvertedManaCost();
                            if (devotion <= rollCost) {
                                return false;
                            }
                        }
                        break;
                    case "stopifunlimitedhandsize":
                        if (ai.isUnlimitedHandSize()) {
                            return false;
                        }
                        break;
                    default:
                        System.out.println(TextUtil.concatNoSpace("Unexpected AI hint parameter in card ", plane.getName(), " in RollPlanarDiceAi: ", paramName, "."));
                        break;
                }
            }
        }

        switch (modeName) {
            case "always":
                decideToRoll = true;
                break;
            case "random":
                if (stablePercent(ai, plane, 1, chance)) {
                    decideToRoll = true;
                }
                break;
            case "never":
                return false;
            default:
                return false;
        }

        if (ai.getGame().getPhaseHandler().getTurn() < minTurnToRoll) {
            decideToRoll = false;
        } else if (!rollInMain1 && ai.getGame().getPhaseHandler().getPhase().isBefore(PhaseType.MAIN2)) {
            decideToRoll = false;
        }

        final int rollsThisTurn = ai.getGame().getPhaseHandler().getPlanarDiceSpecialActionThisTurn();
        if (decideToRoll && rollsThisTurn >= maxActivations) {
            boolean extraRollWindow = sa.hasParam("SpecialAction")
                    && (ai.getGame().getPhaseHandler().is(PhaseType.MAIN2, ai)
                    || (rollInMain1 && ai.getGame().getPhaseHandler().is(PhaseType.MAIN1, ai)));
            if (!extraRollWindow) {
                decideToRoll = false;
            }
        }
        if (decideToRoll && rollsThisTurn > 0 && !canSpendSurplusMana(ai, sa)) {
            decideToRoll = false;
        }

        // check if the AI hesitates
        if (stablePercent(ai, plane, 2, hesitationChance)) {
            decideToRoll = false; // hesitate
        }

        return decideToRoll;
    }

    /**
     * A chance that is decided once per turn and roll count, not again at every priority pass: the AI asks several
     * times per phase (and the Monte Carlo layer asks again to confirm a play), so a fresh die each time would make
     * even a low chance come true almost surely. Every ask in the same situation gets the same answer.
     * Package-private for RollPlanarDiceAiTest.
     */
    static boolean stablePercent(Player ai, Card plane, int salt, int percent) {
        if (percent <= 0) {
            return false;
        }
        if (percent >= 100) {
            return true;
        }
        final int turn = ai.getGame().getPhaseHandler().getTurn();
        final int rolls = ai.getGame().getPhaseHandler().getPlanarDiceSpecialActionThisTurn();
        long z = (((ai.getId() * 31L + plane.getId()) * 31L + turn) * 31L + rolls) * 31L + salt;
        z *= 0x9E3779B97F4A7C15L;
        z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
        z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
        z ^= z >>> 31;
        return Math.floorMod(z, 100L) < percent;
    }

    boolean canSpendSurplusMana(Player ai, SpellAbility roll) {
        final int rollMana = ComputerUtilMana.calculateManaCost(roll.getPayCosts(), roll, ai, true, 0, false)
                .getConvertedManaCost();
        final boolean main1 = ai.getGame().getPhaseHandler().is(PhaseType.MAIN1, ai);
        final AiController aic = ((PlayerControllerAi) ai.getController()).getAi();
        CardCollection cards = ComputerUtilAbility.getAvailableCards(ai.getGame(), ai);
        cards = ComputerUtilCard.dedupeCards(cards);
        final List<SpellAbility> possible = ComputerUtilAbility.getSpellAbilities(cards, ai);
        // Current-play filtering can omit future-phase abilities and counters with no target yet.
        for (Card card : cards) {
            if (card.getController() != ai) {
                continue;
            }
            for (SpellAbility ability : card.getSpellAbilities()) {
                if ((ability.getApi() == ApiType.Counter || ability.getRestrictions().isOpponentTurn()
                        || ability.getRestrictions().getPhases().contains(PhaseType.MAIN2))
                        && !possible.contains(ability)) {
                    possible.add(ability);
                }
            }
        }
        final List<SpellAbility> abilities = ComputerUtilAbility.getOriginalAndAltCostAbilities(possible, ai);

        for (SpellAbility candidate : abilities) {
            if (candidate.getApi() == ApiType.RollPlanarDice || candidate.isManaAbility()
                    || (!candidate.isSpell() && !candidate.isActivatedAbility())
                    || candidate.getPayCosts() == null || !candidate.getPayCosts().hasManaCost()
                    || ComputerUtilCard.isCardRemAIDeck(candidate.getHostCard())) {
                continue;
            }
            if (ComputerUtilMana.calculateManaCost(candidate.getPayCosts(), candidate, ai, true, 0, false)
                    .getConvertedManaCost() == 0 || !ComputerUtilCost.canPayCost(candidate, ai, false)) {
                continue;
            }

            boolean main2Only = candidate.getRestrictions().getPhases().contains(PhaseType.MAIN2)
                    && !candidate.getRestrictions().getPhases().contains(PhaseType.MAIN1);
            if (!SpellAbilityAi.isSorcerySpeed(candidate, ai) && !main2Only) {
                if (!candidate.getRestrictions().isPlayerTurn() && candidate.canCastTiming(ai)
                        && (candidate.getApi() == ApiType.Counter || candidate.getRestrictions().isOpponentTurn()
                        || (candidate.canPlay() && ComputerUtilAbility.isFullyTargetable(candidate)))
                        && !ComputerUtilMana.canPayManaCost(candidate, ai, rollMana, false)) {
                    return false;
                }
                continue;
            }
            if (!main1 || candidate.getRestrictions().isOpponentTurn()
                    || (!candidate.getRestrictions().getPhases().isEmpty()
                    && !candidate.getRestrictions().getPhases().contains(PhaseType.MAIN2))) {
                continue;
            }

            if (!main2Only) {
                SpellAbility preview = candidate.copy(ai);
                if (candidate.isSpell() && candidate.getHostCard().isPermanent()) {
                    preview.putParam("AIIgnoreMain2Preference", "True");
                }
                AiPlayDecision decision = aic.canPlaySa(preview);
                if (decision != AiPlayDecision.WillPlay && decision != AiPlayDecision.WaitForMain2
                        && decision != AiPlayDecision.MissingPhaseRestrictions) {
                    continue;
                }
            }

            // Paying for the future play plus this roll preserves colored mana needs.
            if ((rollMana > 0 && candidate.getPayCosts().getTotalMana().countX() > 0)
                    || !ComputerUtilMana.canPayManaCost(candidate, ai, rollMana, false)) {
                return false;
            }
        }
        return true;
    }

    /* (non-Javadoc)
     * @see forge.card.abilityfactory.SpellAiLogic#chkAIDrawback(java.util.Map, forge.card.spellability.SpellAbility, forge.game.player.Player)
     */
    @Override
    public AiAbilityDecision chkDrawback(Player aiPlayer, SpellAbility sa) {
        // for potential implementation of drawback checks?
        return canPlay(aiPlayer, sa);
    }

    private boolean detectColorInZone(Player p, String paramValue, ZoneType zone, boolean creaturesOnly) {
        boolean hasColorInPlay = false;
        for (Card c : p.getCardsIn(zone)) {
            if (!creaturesOnly || c.isCreature()) {
                if (paramValue.contains("u") && c.isBlue()) {
                    hasColorInPlay = true;
                    break;
                }
                if (paramValue.contains("g") && c.isGreen()) {
                    hasColorInPlay = true;
                    break;
                }
                if (paramValue.contains("r") && c.isRed()) {
                    hasColorInPlay = true;
                    break;
                }
                if (paramValue.contains("w") && c.isWhite()) {
                    hasColorInPlay = true;
                    break;
                }
                if (paramValue.contains("b") && c.isBlack()) {
                    hasColorInPlay = true;
                    break;
                }
            }
        }
        return hasColorInPlay;
    }

    private boolean detectCreatureInZone(Player p, ZoneType zone) {
        boolean hasCreatureInPlay = false;
        for (Card c : p.getCardsIn(zone)) {
            if (c.isCreature()) {
                hasCreatureInPlay = true;
                break;
            }
        }
        return hasCreatureInPlay;
    }
}
