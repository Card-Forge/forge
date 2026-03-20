package forge.ai.ability;

import forge.ai.*;
import forge.card.MagicColor;
import forge.game.Game;
import forge.game.card.*;
import forge.game.combat.Combat;
import forge.game.combat.CombatUtil;
import forge.game.keyword.Keyword;
import forge.game.phase.PhaseHandler;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;

import java.util.List;
import java.util.function.Predicate;

public abstract class PumpAiBase extends SpellAbilityAi {

    public boolean containsUsefulKeyword(final Player ai, final List<String> keywords, final Card card, final SpellAbility sa, final int attack) {
        for (final String keyword : keywords) {
            if (!sa.isCurse() && isUsefulPumpKeyword(ai, keyword, card, sa, attack)) {
                return true;
            }
            if (sa.isCurse() && isUsefulCurseKeyword(ai, keyword, card, sa)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks if is useful keyword.
     * 
     * @param keyword
     *            the keyword
     * @param card
     *            the card
     * @param sa SpellAbility
     * @return true, if is useful keyword
     */
    public boolean isUsefulCurseKeyword(final Player ai, final String keyword, final Card card, final SpellAbility sa) {
        final Game game = ai.getGame();
        final Combat combat = game.getCombat();
        final PhaseHandler ph = game.getPhaseHandler();
        //int attack = getNumAttack(sa);
        //int defense = getNumDefense(sa);
        if (!CardUtil.isStackingKeyword(keyword) && card.hasKeyword(keyword)) {
            return false;
        } else if (keyword.equals("Defender") || keyword.endsWith("CARDNAME can't attack.")) {
            return ph.isPlayerTurn(card.getController()) && CombatUtil.canAttack(card, ai)
                    && (card.getNetCombatDamage() > 0)
                    && !ph.getPhase().isAfter(PhaseType.COMBAT_DECLARE_ATTACKERS);
        } else if (keyword.endsWith("CARDNAME can't attack or block.")) {
            if ("UntilYourNextTurn".equals(sa.getParam("Duration"))) {
                return CombatUtil.canAttack(card, ai) || CombatUtil.canBlock(card, true);
            }
            if (!ph.isPlayerTurn(ai)) {
                return card.getNetCombatDamage() > 0
                        && !ph.getPhase().isAfter(PhaseType.COMBAT_DECLARE_ATTACKERS)
                        && CombatUtil.canAttack(card, ai);
            } else {
                if (ph.getPhase().isAfter(PhaseType.COMBAT_DECLARE_BLOCKERS)
                        || ph.getPhase().isBefore(PhaseType.MAIN1)) {
                    return false;
                }

                List<Card> attackers = CardLists.filter(ai.getCreaturesInPlay(), c -> {
                    if (c.equals(sa.getHostCard()) && sa.getPayCosts().hasTapCost()
                            && (combat == null || !combat.isAttacking(c))) {
                        return false;
                    }
                    return (combat != null && combat.isAttacking(c)) || CombatUtil.canAttack(c, card.getController());
                });
                return CombatUtil.canBlockAtLeastOne(card, attackers);
            }
        } else if (keyword.endsWith("CARDNAME can't block.")) {
            if (!ph.isPlayerTurn(ai) || ph.getPhase().isAfter(PhaseType.COMBAT_DECLARE_BLOCKERS)
                    || ph.getPhase().isBefore(PhaseType.MAIN1)) {
                return false;
            }

            List<Card> attackers = CardLists.filter(ai.getCreaturesInPlay(), c -> {
                if (c.equals(sa.getHostCard()) && sa.getPayCosts().hasTapCost()
                        && (combat == null || !combat.isAttacking(c))) {
                    return false;
                }
                // the cards controller needs to be the one attacked
                return (combat != null && combat.isAttacking(c) && card.getController().equals(combat.getDefenderPlayerByAttacker(c))) ||
                        CombatUtil.canAttack(c, card.getController());
            });
            return CombatUtil.canBlockAtLeastOne(card, attackers);
        } else if (keyword.endsWith("This card doesn't untap during your next untap step.")) {
            return !ph.getPhase().isBefore(PhaseType.MAIN2) && !card.isUntapped() && ph.isPlayerTurn(ai)
                    && card.canUntap(card.getController(), true);
        } else if (keyword.endsWith("Prevent all combat damage that would be dealt by CARDNAME.")
                || keyword.endsWith("Prevent all damage that would be dealt by CARDNAME.")) {
            if (ph.isPlayerTurn(ai) && (!(CombatUtil.canBlock(card) || combat != null && combat.isBlocking(card))
                    || card.getNetCombatDamage() <= 0
                    || ph.getPhase().isAfter(PhaseType.COMBAT_DECLARE_BLOCKERS)
                    || ph.getPhase().isBefore(PhaseType.MAIN1)
                    || CardLists.getNotKeyword(ai.getCreaturesInPlay(), Keyword.DEFENDER).isEmpty())) {
                return false;
            }
            return ph.isPlayerTurn(ai) || (combat != null && combat.isAttacking(card) && card.getNetCombatDamage() > 0);
        } else return true;
    }

    /**
     * Checks if is useful keyword.
     * 
     * @param keyword
     *            the keyword
     * @param card
     *            the card
     * @param sa SpellAbility
     * @return true, if is useful keyword
     */
    public boolean isUsefulPumpKeyword(final Player ai, final String keyword, final Card card, final SpellAbility sa, final int attack) {
        final Game game = ai.getGame();
        final Combat combat = game.getCombat();
        final PhaseHandler ph = game.getPhaseHandler();
        final Player opp = AiAttackController.choosePreferredDefenderPlayer(ai);
        final int newPower = card.getNetCombatDamage() + attack;
        //int defense = getNumDefense(sa);
        if (!CardUtil.isStackingKeyword(keyword) && card.hasKeyword(keyword)) {
            return false;
        }

        final boolean evasive = keyword.endsWith("Shadow");
        // give evasive keywords to creatures that can or do attack
        if (evasive) {
            return !ph.isPlayerTurn(opp) && ((combat != null && combat.isAttacking(card)) || CombatUtil.canAttack(card, opp))
                    && !ph.getPhase().isAfter(PhaseType.COMBAT_DECLARE_ATTACKERS)
                    && newPower > 0
                    && opp.getCreaturesInPlay().anyMatch(CardPredicates.possibleBlockers(card));
        } else if (keyword.endsWith("Flying")) {
            CardCollectionView attackingFlyer = CardCollection.EMPTY;
            if (combat != null) {
                attackingFlyer = CardLists.getKeyword(combat.getAttackers(), Keyword.FLYING);
            }

            if (ph.isPlayerTurn(opp)
                    && ph.getPhase() == PhaseType.COMBAT_DECLARE_ATTACKERS
                    && !attackingFlyer.isEmpty()
                    && !card.hasKeyword(Keyword.REACH)
                    && CombatUtil.canBlock(card)
                    && ComputerUtilCombat.lifeInDanger(ai, game.getCombat())) {
                return true;
            }
            Predicate<Card> flyingOrReach = CardPredicates.hasKeyword(Keyword.FLYING).or(CardPredicates.hasKeyword(Keyword.REACH));
            if (ph.isPlayerTurn(opp) && combat != null
                    && !attackingFlyer.isEmpty()
                    && CombatUtil.canBlock(card)) {
                // Use defensively to destroy the opposing Flying creature when possible, or to block with an indestructible
                // creature buffed with Flying
                for (Card c : attackingFlyer) {
                    if (!ComputerUtilCombat.combatantCantBeDestroyed(c.getController(), c)
                            && (card.getNetPower() >= c.getNetToughness() && card.getNetToughness() > c.getNetPower()
                            || ComputerUtilCombat.combatantCantBeDestroyed(ai, card))) {
                        return true;
                    }
                }
            }
            return !ph.isPlayerTurn(opp) && ((combat != null && combat.isAttacking(card)) || CombatUtil.canAttack(card, opp))
                    && !ph.getPhase().isAfter(PhaseType.COMBAT_DECLARE_ATTACKERS)
                    && newPower > 0
                    && CardLists.filter(opp.getCreaturesInPlay(), CardPredicates.possibleBlockers(card))
                        .anyMatch(flyingOrReach.negate());
        } else if (keyword.endsWith("Horsemanship")) {
            if (ph.isPlayerTurn(opp)
                    && ph.getPhase().equals(PhaseType.COMBAT_DECLARE_ATTACKERS)
                    && !CardLists.getKeyword(game.getCombat().getAttackers(), Keyword.HORSEMANSHIP).isEmpty()
                    && CombatUtil.canBlock(card)
                    && ComputerUtilCombat.lifeInDanger(ai, game.getCombat())) {
                return true;
            }
            return !ph.isPlayerTurn(opp) && ((combat != null && combat.isAttacking(card)) || CombatUtil.canAttack(card, opp))
                    && !ph.getPhase().isAfter(PhaseType.COMBAT_DECLARE_ATTACKERS)
                    && newPower > 0
                    && !CardLists.getNotKeyword(CardLists.filter(opp.getCreaturesInPlay(), CardPredicates.possibleBlockers(card)),
                    Keyword.HORSEMANSHIP).isEmpty();
        } else if (keyword.endsWith("Intimidate")) {
            return !ph.isPlayerTurn(opp) && ((combat != null && combat.isAttacking(card)) || CombatUtil.canAttack(card, opp))
                    && !ph.getPhase().isAfter(PhaseType.COMBAT_DECLARE_ATTACKERS)
                    && newPower > 0
                    && !CardLists.getNotType(CardLists.filter(
                    opp.getCreaturesInPlay(), CardPredicates.possibleBlockers(card)), "Artifact").isEmpty();
        } else if (keyword.endsWith("Fear")) {
            return !ph.isPlayerTurn(opp) && ((combat != null && combat.isAttacking(card)) || CombatUtil.canAttack(card, opp))
                    && !ph.getPhase().isAfter(PhaseType.COMBAT_DECLARE_ATTACKERS)
                    && newPower > 0
                    && !CardLists.getNotColor(CardLists.getNotType(CardLists.filter(
                    opp.getCreaturesInPlay(), CardPredicates.possibleBlockers(card)), "Artifact"), MagicColor.BLACK).isEmpty();
        } else if (keyword.endsWith("Haste")) {
            return CombatUtil.isAttackerSick(card, opp) && !ph.isPlayerTurn(opp) && !card.isTapped()
                    && newPower > 0
                    && !ph.getPhase().isAfter(PhaseType.COMBAT_DECLARE_ATTACKERS)
                    && ComputerUtilCombat.canAttackNextTurn(card);
        } else if (keyword.endsWith("Indestructible")) {
            // Predicting threatened objects in relevant non-combat situations happens elsewhere,
            // so we are only worrying about combat relevance of Indestructible at this point.
            return combat != null
                    && ((combat.isBlocked(card) || combat.isBlocking(card))
                    && ComputerUtilCombat.combatantWouldBeDestroyed(ai, card, combat));
        } else if (keyword.endsWith("Deathtouch")) {
            if (ph.isPlayerTurn(opp) && ph.getPhase().equals(PhaseType.COMBAT_DECLARE_ATTACKERS)) {
                List<Card> attackers = combat.getAttackers();
                for (Card attacker : attackers) {
                    if (CombatUtil.canBlock(attacker, card, combat)
                            && !ComputerUtilCombat.canDestroyAttacker(ai, attacker, card, combat, false)) {
                        return true;
                    }
                }
            } else if (ph.isPlayerTurn(ai) && ph.getPhase().isBefore(PhaseType.COMBAT_DECLARE_ATTACKERS)
                    && CombatUtil.canAttack(card, opp)) {
                List<Card> blockers = opp.getCreaturesInPlay();
                for (Card blocker : blockers) {
                    if (CombatUtil.canBlock(card, blocker, combat)
                            && !ComputerUtilCombat.canDestroyBlocker(ai, blocker, card, combat, false)) {
                        return true;
                    }
                }
            }
            return false;
        } else if (keyword.startsWith("Bushido")) {
            return !ph.isPlayerTurn(opp) && ((combat != null && combat.isAttacking(card)) || CombatUtil.canAttack(card, opp))
                    && !ph.getPhase().isAfter(PhaseType.COMBAT_DECLARE_BLOCKERS)
                    && !opp.getCreaturesInPlay().isEmpty()
                    && opp.getCreaturesInPlay().anyMatch(CardPredicates.possibleBlockers(card));
        } else if (keyword.equals("First Strike")) {
            if (card.hasDoubleStrike()) {
                return false;
            }
            if (combat != null && combat.isBlocked(card) && !combat.getBlockers(card).isEmpty()) {
                Card blocker = combat.getBlockers(card).get(0);
                if (ComputerUtilCombat.canDestroyAttacker(ai, card, blocker, combat, true) 
                        && !ComputerUtilCombat.canDestroyAttacker(ai, card, blocker, combat, false))
                	return true;
                if (!ComputerUtilCombat.canDestroyBlocker(ai, blocker, card, combat, true) 
                        && ComputerUtilCombat.canDestroyBlocker(ai, blocker, card, combat, false))
                	return true;
            }
            if (combat != null && combat.isBlocking(card) && !combat.getAttackersBlockedBy(card).isEmpty()) {
                Card attacker = combat.getAttackersBlockedBy(card).get(0);
                if (!ComputerUtilCombat.canDestroyAttacker(ai, attacker, card, combat, true) 
                        && ComputerUtilCombat.canDestroyAttacker(ai, attacker, card, combat, false))
                	return true;
                return ComputerUtilCombat.canDestroyBlocker(ai, card, attacker, combat, true)
                        && !ComputerUtilCombat.canDestroyBlocker(ai, card, attacker, combat, false);
            }
            return false;
        } else if (keyword.equals("Double Strike")) {
            return !ph.isPlayerTurn(opp) && ((combat != null && combat.isAttacking(card)) || CombatUtil.canAttack(card, opp))
                    && newPower > 0
                    && !ph.getPhase().isAfter(PhaseType.COMBAT_DECLARE_BLOCKERS);
        } else if (keyword.startsWith("Rampage")) {
            return !ph.isPlayerTurn(opp) && ((combat != null && combat.isAttacking(card)) || CombatUtil.canAttack(card, opp))
                    && newPower > 0
                    && !ph.getPhase().isAfter(PhaseType.COMBAT_DECLARE_ATTACKERS)
                    && CardLists.filter(opp.getCreaturesInPlay(), CardPredicates.possibleBlockers(card)).size() >= 2;
        } else if (keyword.startsWith("Flanking")) {
            return !ph.isPlayerTurn(opp) && ((combat != null && combat.isAttacking(card)) || CombatUtil.canAttack(card, opp))
                    && newPower > 0
                    && !ph.getPhase().isAfter(PhaseType.COMBAT_DECLARE_ATTACKERS)
                    && !CardLists.getNotKeyword(CardLists.filter(opp.getCreaturesInPlay(), CardPredicates.possibleBlockers(card)),
                    Keyword.FLANKING).isEmpty();
        } else if (keyword.startsWith("Trample")) {
            return !ph.isPlayerTurn(opp) && ((combat != null && combat.isAttacking(card)) || CombatUtil.canAttack(card, opp))
                    && CombatUtil.canBeBlocked(card, null, opp)
                    && !ph.getPhase().isAfter(PhaseType.COMBAT_DECLARE_ATTACKERS)
                    && newPower > 1
                    && opp.getCreaturesInPlay().anyMatch(CardPredicates.possibleBlockers(card));
        } else if (keyword.equals("Infect")) {
            if (newPower <= 0) {
                return false;
            }
            if (combat != null && combat.isBlocking(card) && !card.hasKeyword(Keyword.WITHER)) {
                return true;
            }
            return !ph.isPlayerTurn(opp) && ((combat != null && combat.isAttacking(card)) || CombatUtil.canAttack(card, opp))
                    && !ph.getPhase().isAfter(PhaseType.COMBAT_DECLARE_BLOCKERS);
        } else if (keyword.endsWith("Wither")) {
            if (newPower <= 0 || card.isWitherDamage()) {
                return false;
            }
            return combat != null && (combat.isBlocking(card) || (combat.isAttacking(card) && combat.isBlocked(card)));
        } else if (keyword.equals("Lifelink")) {
            if (newPower <= 0 || ai.canGainLife()) {
                return false;
            }
            return combat != null && (combat.isAttacking(card) || combat.isBlocking(card));
        } else if (keyword.equals("Vigilance")) {
            return !ph.isPlayerTurn(opp) && CombatUtil.canAttack(card, opp)
                    && newPower > 0
                    && !ph.getPhase().isAfter(PhaseType.COMBAT_DECLARE_ATTACKERS)
                    && !CardLists.getNotKeyword(opp.getCreaturesInPlay(), Keyword.DEFENDER).isEmpty();
        } else if (keyword.equals("Reach")) {
            return !ph.isPlayerTurn(ai)
                    && ph.getPhase().equals(PhaseType.COMBAT_DECLARE_ATTACKERS)
                    && !CardLists.getKeyword(game.getCombat().getAttackers(), Keyword.FLYING).isEmpty()
                    && !card.hasKeyword(Keyword.FLYING)
                    && CombatUtil.canBlock(card);
        } else if (keyword.equals("Shroud") || keyword.equals("Hexproof")) {
            return ComputerUtil.predictThreatenedObjects(sa.getActivatingPlayer(), sa).contains(card);
        } else if (keyword.equals("Persist")) {
            return card.getBaseToughness() > 1 && !card.hasKeyword(Keyword.UNDYING);
        } else if (keyword.startsWith("Landwalk:")) {
            return !ph.isPlayerTurn(opp) && ((combat != null && combat.isAttacking(card)) || CombatUtil.canAttack(card, opp))
                    && !ph.getPhase().isAfter(PhaseType.COMBAT_DECLARE_ATTACKERS)
                    && newPower > 0
                    && !CardLists.getType(opp.getLandsInPlay(), keyword.split(":")[1]).isEmpty()
                    && opp.getCreaturesInPlay().anyMatch(CardPredicates.possibleBlockers(card));
        } else if (keyword.equals("Prevent all combat damage that would be dealt to CARDNAME.")) {
            return combat != null && (combat.isBlocking(card) || combat.isBlocked(card));
        } else if (keyword.equals("Menace")) {
            return combat != null && combat.isAttacking(card);
        }
        return true;
    }

    /**
     * <p>
     * getPumpCreatures.
     * </p>
     * 
     * @return a {@link forge.CardList} object.
     */
    protected CardCollection getPumpCreatures(final Player ai, final SpellAbility sa, final int defense, final int attack,
            final List<String> keywords, final boolean immediately) {
        CardCollection list = CardLists.getTargetableCards(ai.getCreaturesInPlay(), sa);
        list = CardLists.filter(list, c -> ComputerUtilCard.shouldPumpCard(ai, sa, c, defense, attack, keywords, immediately));
        return list;
    }

    /**
     * <p>
     * getCurseCreatures.
     * </p>
     * 
     * @param sa
     *            a {@link forge.game.spellability.SpellAbility} object.
     * @param defense
     *            a int.
     * @param attack
     *            a int.
     * @return a {@link forge.CardList} object.
     */
    protected CardCollection getCurseCreatures(final Player ai, final SpellAbility sa, final int defense, final int attack, final List<String> keywords) {
        CardCollection list = ai.getOpponents().getCardsIn(ZoneType.Battlefield);
        final Game game = ai.getGame();
        final Combat combat = game.getCombat();
        list = CardLists.getTargetableCards(list, sa);

        if (list.isEmpty()) {
            return list;
        }

        if (defense < 0) { // with spells that give -X/-X, compi will try to destroy a creature
            list = CardLists.filter(list, c -> {
                if (c.getSVar("Targeting").equals("Dies") || c.getNetToughness() <= -defense) {
                    return true; // can kill indestructible creatures
                }
                return ComputerUtilCombat.getDamageToKill(c, false) <= -defense && !c.hasKeyword(Keyword.INDESTRUCTIBLE);
            }); // leaves all creatures that will be destroyed
        } // -X/-X end
        else if (attack < 0 && !game.getReplacementHandler().isPreventCombatDamageThisTurn()) {
            // spells that give -X/0
            if (game.getPhaseHandler().isPlayerTurn(ai)) {
                if (game.getPhaseHandler().getPhase().isBefore(PhaseType.COMBAT_BEGIN)) {
                    // TODO: Curse creatures that will block AI's creatures, if AI is going to attack.
                    list = new CardCollection();
                } else {
                    list = new CardCollection();
                }
            } else if (game.getPhaseHandler().getPhase().isBefore(PhaseType.COMBAT_DECLARE_BLOCKERS)) {
                // Human active, only curse attacking creatures
                list = CardLists.filter(list, c -> {
                    if (combat == null || !combat.isAttacking(c)) {
                        return false;
                    }
                    if (c.getNetPower() > 0 && ai.getLife() < 5) {
                        return true;
                    }
                    //Don't waste a -7/-0 spell on a 1/1 creature
                    return c.getNetPower() + attack > -2 || c.getNetPower() > 3;
                });
            } else {
                list = new CardCollection();
            }
        } // -X/0 end
        else if (!keywords.isEmpty()) {
            // If the keyword can prevent a creature from attacking, see if there's some kind of viable prioritization
            if (keywords.contains("CARDNAME can't attack.") || keywords.contains("CARDNAME can't attack or block.")) {
                if (CardLists.getNotType(list, "Creature").isEmpty()) {
                    list = ComputerUtilCard.prioritizeCreaturesWorthRemovingNow(ai, list, true);
                }
            }

            list = CardLists.filter(list, c -> containsUsefulKeyword(ai, keywords, c, sa, attack));
        } else if (sa.hasParam("NumAtt") || sa.hasParam("NumDef")) {
            // X is zero
            list = new CardCollection();
        }

        return list;
    }

    protected boolean containsNonCombatKeyword(final List<String> keywords) {
        for (final String keyword : keywords) {
            // since most keywords are combat relevant check for those that are not
            if (keyword.endsWith("This card doesn't untap during your next untap step.")
                    || keyword.endsWith("Shroud") || keyword.endsWith("Hexproof")) {
                return true;
            }
        }
        return false;
    }
}
