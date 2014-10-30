package forge.ai.ability;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.Iterables;

import forge.ai.ComputerUtil;
import forge.ai.ComputerUtilCard;
import forge.ai.ComputerUtilCombat;
import forge.ai.SpellAbilityAi;
import forge.card.MagicColor;
import forge.game.Game;
import forge.game.card.Card;
import forge.game.card.CardCollection;
import forge.game.card.CardFactory;
import forge.game.card.CardLists;
import forge.game.card.CardPredicates;
import forge.game.card.CardUtil;
import forge.game.card.CounterType;
import forge.game.combat.Combat;
import forge.game.combat.CombatUtil;
import forge.game.phase.PhaseHandler;
import forge.game.phase.PhaseType;
import forge.game.phase.Untap;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;
import forge.game.zone.ZoneType;
import forge.util.MyRandom;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

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
        final Player human = ai.getOpponent();
        //int attack = getNumAttack(sa);
        //int defense = getNumDefense(sa);
        if (!CardUtil.isStackingKeyword(keyword) && card.hasKeyword(keyword)) {
            return false;
        } else if (keyword.equals("Defender") || keyword.endsWith("CARDNAME can't attack.")) {
            if (ph.isPlayerTurn(ai) || !CombatUtil.canAttack(card, human)
                    || (card.getNetCombatDamage() <= 0)
                    || ph.getPhase().isAfter(PhaseType.COMBAT_DECLARE_ATTACKERS)) {
                return false;
            }
        } else if (keyword.endsWith("CARDNAME can't attack or block.")) {
            if (sa.hasParam("UntilYourNextTurn")) {
                if (CombatUtil.canAttack(card, human) || CombatUtil.canBlock(card, true)) {
                    return true;
                }
                return false;
            }
            if (ph.isPlayerTurn(human)) {
                if (!CombatUtil.canAttack(card, human)
                        || (card.getNetCombatDamage() <= 0)
                        || ph.getPhase().isAfter(PhaseType.COMBAT_DECLARE_ATTACKERS)) {
                    return false;
                }
            } else {
                if (ph.getPhase().isAfter(PhaseType.COMBAT_DECLARE_BLOCKERS)
                        || ph.getPhase().isBefore(PhaseType.MAIN1)) {
                    return false;
                }

                List<Card> attackers = CardLists.filter(ai.getCardsIn(ZoneType.Battlefield), new Predicate<Card>() {
                    @Override
                    public boolean apply(final Card c) {
                        return (c.isCreature() && CombatUtil.canAttack(c, human));
                    }
                });
                if (!CombatUtil.canBlockAtLeastOne(card, attackers)) {
                    return false;
                }
            }
        } else if (keyword.endsWith("CARDNAME can't block.")) {
            if (ph.isPlayerTurn(human) || ph.getPhase().isAfter(PhaseType.COMBAT_DECLARE_BLOCKERS)
                    || ph.getPhase().isBefore(PhaseType.MAIN1)) {
                return false;
            }

            List<Card> attackers = CardLists.filter(ai.getCardsIn(ZoneType.Battlefield), new Predicate<Card>() {
                @Override
                public boolean apply(final Card c) {
                    return (c.isCreature() && CombatUtil.canAttack(c, human));
                }
            });
            if (!CombatUtil.canBlockAtLeastOne(card, attackers)) {
                return false;
            }
        } else if (keyword.endsWith("This card doesn't untap during your next untap step.")) {
            if (ph.getPhase().isBefore(PhaseType.MAIN2) || card.isUntapped() || ph.isPlayerTurn(human)
                    || !Untap.canUntap(card)) {
                return false;
            }
        } else if (keyword.endsWith("Prevent all combat damage that would be dealt by CARDNAME.")
                || keyword.endsWith("Prevent all damage that would be dealt by CARDNAME.")) {
            if (ph.isPlayerTurn(ai) && (!(CombatUtil.canBlock(card) || combat != null && combat.isBlocking(card))
                    || card.getNetCombatDamage() <= 0
                    || ph.getPhase().isAfter(PhaseType.COMBAT_DECLARE_BLOCKERS)
                    || ph.getPhase().isBefore(PhaseType.MAIN1)
                    || CardLists.getNotKeyword(ai.getCreaturesInPlay(), "Defender").isEmpty())) {
                return false;
            }
            if (ph.isPlayerTurn(human) && (combat == null || !combat.isAttacking(card) || card.getNetCombatDamage() <= 0)) {
                return false;
            }
        } else if (keyword.endsWith("CARDNAME attacks each turn if able.")
                || keyword.endsWith("CARDNAME attacks each combat if able.")) {
            if (ph.isPlayerTurn(ai) || !CombatUtil.canAttack(card, human) || !CombatUtil.canBeBlocked(card, ai.getOpponent())
                    || ph.getPhase().isAfter(PhaseType.COMBAT_DECLARE_ATTACKERS)) {
                return false;
            }
        } else if (keyword.endsWith("CARDNAME can't be regenerated.")) {
            if (card.getShieldCount() > 0) {
                return true;
            }
            if (card.hasKeyword("If CARDNAME would be destroyed, regenerate it.") && combat != null
                    && (combat.isBlocked(card) || combat.isBlocking(card))) {
                return true;
            }
            return false;
        } else if (keyword.endsWith("CARDNAME's activated abilities can't be activated.")) {
            return false; //too complex
        }
        return true;
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
        final Player opp = ai.getOpponent();
        final int newPower = card.getNetCombatDamage() + attack;
        //int defense = getNumDefense(sa);
        if (!CardUtil.isStackingKeyword(keyword) && card.hasKeyword(keyword)) {
            return false;
        }

        final boolean evasive = (keyword.endsWith("Unblockable") || keyword.endsWith("Shadow") || keyword.startsWith("CantBeBlockedBy"));
        // give evasive keywords to creatures that can or do attack
        if (evasive) {
            if (ph.isPlayerTurn(opp) || !(CombatUtil.canAttack(card, opp) || (combat != null && combat.isAttacking(card)))
                    || ph.getPhase().isAfter(PhaseType.COMBAT_DECLARE_ATTACKERS)
                    || newPower <= 0
                    || CardLists.filter(opp.getCreaturesInPlay(), CardPredicates.possibleBlockers(card)).isEmpty()) {
                return false;
            }
        } else if (keyword.endsWith("Flying")) {
            if (ph.isPlayerTurn(opp)
                    && ph.getPhase() == PhaseType.COMBAT_DECLARE_ATTACKERS
                    && !CardLists.getKeyword(game.getCombat().getAttackers(), "Flying").isEmpty()
                    && !card.hasKeyword("Reach")
                    && CombatUtil.canBlock(card)
                    && ComputerUtilCombat.lifeInDanger(ai, game.getCombat())) {
                return true;
            }
            Predicate<Card> flyingOrReach = Predicates.or(CardPredicates.hasKeyword("Flying"), CardPredicates.hasKeyword("Reach"));
            if (ph.isPlayerTurn(opp) || !(CombatUtil.canAttack(card, opp) || (combat != null && combat.isAttacking(card)))
                    || ph.getPhase().isAfter(PhaseType.COMBAT_DECLARE_ATTACKERS)
                    || newPower <= 0
                    || !Iterables.any(CardLists.filter(opp.getCreaturesInPlay(), CardPredicates.possibleBlockers(card)),
                            Predicates.not(flyingOrReach))) {
                return false;
            }
        } else if (keyword.endsWith("Horsemanship")) {
            if (ph.isPlayerTurn(opp)
                    && ph.getPhase().equals(PhaseType.COMBAT_DECLARE_ATTACKERS)
                    && !CardLists.getKeyword(game.getCombat().getAttackers(), "Horsemanship").isEmpty()
                    && CombatUtil.canBlock(card)
                    && ComputerUtilCombat.lifeInDanger(ai, game.getCombat())) {
                return true;
            }
            if (ph.isPlayerTurn(opp) || !(CombatUtil.canAttack(card, opp) || (combat != null && combat.isAttacking(card)))
                    || ph.getPhase().isAfter(PhaseType.COMBAT_DECLARE_ATTACKERS)
                    || newPower <= 0
                    || CardLists.getNotKeyword(CardLists.filter(opp.getCreaturesInPlay(), CardPredicates.possibleBlockers(card)),
                            "Horsemanship").isEmpty()) {
                return false;
            }
        } else if (keyword.endsWith("Intimidate")) {
        	if (ph.isPlayerTurn(opp) || !(CombatUtil.canAttack(card, opp) || (combat != null && combat.isAttacking(card)))
                    || ph.getPhase().isAfter(PhaseType.COMBAT_DECLARE_ATTACKERS)
                    || newPower <= 0
                    || CardLists.getNotType(CardLists.filter(
                    		opp.getCreaturesInPlay(), CardPredicates.possibleBlockers(card)), "Artifact").isEmpty()) {
                return false;
            }
        } else if (keyword.endsWith("Fear")) {
        	if (ph.isPlayerTurn(opp) || !(CombatUtil.canAttack(card, opp) || (combat != null && combat.isAttacking(card)))
                    || ph.getPhase().isAfter(PhaseType.COMBAT_DECLARE_ATTACKERS)
                    || newPower <= 0
                    || CardLists.getNotColor(CardLists.getNotType(CardLists.filter(
                    		opp.getCreaturesInPlay(), CardPredicates.possibleBlockers(card)), "Artifact"), MagicColor.BLACK).isEmpty()) {
                return false;
            }
        } else if (keyword.endsWith("Haste")) {
            if (!card.hasSickness() || ph.isPlayerTurn(opp) || card.isTapped()
                    || newPower <= 0
                    || card.hasKeyword("CARDNAME can attack as though it had haste.")
                    || ph.getPhase().isAfter(PhaseType.COMBAT_DECLARE_ATTACKERS)
                    || !ComputerUtilCombat.canAttackNextTurn(card)) {
                return false;
            }
        } else if (keyword.endsWith("Indestructible")) {
            return true;
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
        } else if (keyword.equals("Bushido")) {
            if (ph.isPlayerTurn(opp) || !(CombatUtil.canAttack(card, opp) || (combat != null && combat.isAttacking(card)))
                    || ph.getPhase().isAfter(PhaseType.COMBAT_DECLARE_BLOCKERS)
                    || opp.getCreaturesInPlay().isEmpty()
                    || CardLists.filter(opp.getCreaturesInPlay(), CardPredicates.possibleBlockers(card)).isEmpty()) {
                return false;
            }
        } else if (keyword.equals("First Strike")) {
        	if (card.hasKeyword("Double Strike")) {
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
                if (ComputerUtilCombat.canDestroyBlocker(ai, card, attacker, combat, true) 
                        && !ComputerUtilCombat.canDestroyBlocker(ai, card, attacker, combat, false))
                	return true;
            }
            return false;
        } else if (keyword.equals("Double Strike")) {
            if (ph.isPlayerTurn(opp) || !(CombatUtil.canAttack(card, opp) || (combat != null && combat.isAttacking(card)))
                    || newPower <= 0
                    || ph.getPhase().isAfter(PhaseType.COMBAT_DECLARE_BLOCKERS)) {
                return false;
            }
        } else if (keyword.startsWith("Rampage")) {
            if (ph.isPlayerTurn(opp) || !(CombatUtil.canAttack(card, opp) || (combat != null && combat.isAttacking(card)))
            		|| newPower <= 0
                    || ph.getPhase().isAfter(PhaseType.COMBAT_DECLARE_ATTACKERS)
                    || CardLists.filter(opp.getCreaturesInPlay(), CardPredicates.possibleBlockers(card)).size() < 2) {
                return false;
            }
        } else if (keyword.startsWith("Flanking")) {
            if (ph.isPlayerTurn(opp) || !(CombatUtil.canAttack(card, opp) || (combat != null && combat.isAttacking(card)))
            		|| newPower <= 0
                    || ph.getPhase().isAfter(PhaseType.COMBAT_DECLARE_ATTACKERS)
                    || CardLists.getNotKeyword(CardLists.filter(opp.getCreaturesInPlay(), CardPredicates.possibleBlockers(card)),
                            "Flanking").isEmpty()) {
                return false;
            }
        } else if (keyword.startsWith("Trample")) {
            if (ph.isPlayerTurn(opp) || !(CombatUtil.canAttack(card, opp) || (combat != null && combat.isAttacking(card)))
                    || !CombatUtil.canBeBlocked(card, opp)
                    || ph.getPhase().isAfter(PhaseType.COMBAT_DECLARE_ATTACKERS)
                    || newPower <= 1
                    || CardLists.filter(opp.getCreaturesInPlay(), CardPredicates.possibleBlockers(card)).isEmpty()) {
                return false;
            }
        } else if (keyword.equals("Infect")) {
            if (newPower <= 0) {
                return false;
            }
            if (combat != null && combat.isBlocking(card) && !card.hasKeyword("Wither")) {
                return true;
            }
            if ((ph.isPlayerTurn(opp))
                    || !(CombatUtil.canAttack(card, opp) || (combat != null && combat.isAttacking(card)))
                    || ph.getPhase().isAfter(PhaseType.COMBAT_DECLARE_BLOCKERS)) {
                return false;
            }
        } else if (keyword.endsWith("Wither")) {
            if (newPower <= 0 || card.hasKeyword("Infect")) {
                return false;
            }
            return combat != null && ( combat.isBlocking(card) || (combat.isAttacking(card) && combat.isBlocked(card)) );
        } else if (keyword.equals("Lifelink")) {
            if (newPower <= 0 || ai.canGainLife()) {
                return false;
            }
            return combat != null && ( combat.isAttacking(card) || combat.isBlocking(card) );
        } else if (keyword.equals("Vigilance")) {
            if (ph.isPlayerTurn(opp) || !CombatUtil.canAttack(card, opp)
            		|| newPower <= 0
                    || ph.getPhase().isAfter(PhaseType.COMBAT_DECLARE_ATTACKERS)
                    || CardLists.getNotKeyword(opp.getCreaturesInPlay(), "Defender").isEmpty()) {
                return false;
            }
        } else if (keyword.equals("Reach")) {
            if (ph.isPlayerTurn(ai)
                    || !ph.getPhase().equals(PhaseType.COMBAT_DECLARE_ATTACKERS)
                    || CardLists.getKeyword(game.getCombat().getAttackers(), "Flying").isEmpty()
                    || card.hasKeyword("Flying")
                    || !CombatUtil.canBlock(card)) {
                return false;
            }
        } else if (keyword.endsWith("CARDNAME can block an additional creature.")) {
            if (ph.isPlayerTurn(ai)
                    || !ph.getPhase().equals(PhaseType.COMBAT_DECLARE_ATTACKERS)) {
                return false;
            }
            int canBlockNum = 1 + card.getKeywordAmount("CARDNAME can block an additional creature.") +
                    card.getKeywordAmount("CARDNAME can block an additional ninety-nine creatures.") * 99;
            int possibleBlockNum = 0;
            for (Card attacker : game.getCombat().getAttackers()) {
                if (CombatUtil.canBlock(attacker, card)) {
                    possibleBlockNum++;
                    if (possibleBlockNum > canBlockNum) {
                        break;
                    }
                }
            }
            if (possibleBlockNum <= canBlockNum) {
                return false;
            }
        } else if (keyword.equals("Shroud") || keyword.equals("Hexproof")) {
            if (!ComputerUtil.predictThreatenedObjects(sa.getActivatingPlayer(), sa).contains(card)) {
                return false;
            }
        } else if (keyword.equals("Persist")) {
            if (card.getBaseToughness() <= 1 || card.hasKeyword("Undying")) {
                return false;
            }
        } else if (keyword.equals("Islandwalk")) {
            if (ph.isPlayerTurn(opp) || !(CombatUtil.canAttack(card, opp) || (combat != null && combat.isAttacking(card)))
                    || ph.getPhase().isAfter(PhaseType.COMBAT_DECLARE_ATTACKERS)
                    || newPower <= 0
                    || CardLists.getType(opp.getLandsInPlay(), "Island").isEmpty()
                    || CardLists.filter(opp.getCreaturesInPlay(), CardPredicates.possibleBlockers(card)).isEmpty()) {
                return false;
            }
        } else if (keyword.equals("Swampwalk")) {
            if (ph.isPlayerTurn(opp) || !(CombatUtil.canAttack(card, opp) || (combat != null && combat.isAttacking(card)))
                    || ph.getPhase().isAfter(PhaseType.COMBAT_DECLARE_ATTACKERS)
                    || newPower <= 0
                    || CardLists.getType(opp.getLandsInPlay(), "Swamp").isEmpty()
                    || CardLists.filter(opp.getCreaturesInPlay(), CardPredicates.possibleBlockers(card)).isEmpty()) {
                return false;
            }
        } else if (keyword.equals("Mountainwalk")) {
            if (ph.isPlayerTurn(opp) || !(CombatUtil.canAttack(card, opp) || (combat != null && combat.isAttacking(card)))
                    || ph.getPhase().isAfter(PhaseType.COMBAT_DECLARE_ATTACKERS)
                    || newPower <= 0
                    || CardLists.getType(opp.getLandsInPlay(), "Mountain").isEmpty()
                    || CardLists.filter(opp.getCreaturesInPlay(), CardPredicates.possibleBlockers(card)).isEmpty()) {
                return false;
            }
        } else if (keyword.equals("Forestwalk")) {
            if (ph.isPlayerTurn(opp) || !(CombatUtil.canAttack(card, opp) || (combat != null && combat.isAttacking(card)))
                    || ph.getPhase().isAfter(PhaseType.COMBAT_DECLARE_ATTACKERS)
                    || newPower <= 0
                    || CardLists.getType(opp.getLandsInPlay(), "Forest").isEmpty()
                    || CardLists.filter(opp.getCreaturesInPlay(), CardPredicates.possibleBlockers(card)).isEmpty()) {
                return false;
            }
        } else if (keyword.endsWith("CARDNAME can attack as though it didn't have defender.")) {
            if (!ph.isPlayerTurn(ai) || !card.hasKeyword("Defender")
                    || ph.getPhase().isAfter(PhaseType.COMBAT_BEGIN)
                    || card.isTapped() || newPower <= 0) {
                return false;
            }
        }
        return true;
    }

    protected boolean shouldPumpCard(final Player ai, final SpellAbility sa, final Card c, final int defense, final int attack,
            final List<String> keywords) {
        final Game game = ai.getGame();
        final PhaseHandler phase = game.getPhaseHandler();
        final Combat combat = phase.getCombat();
        
        if (!c.canBeTargetedBy(sa)) {
            return false;
        }

        if ((c.getNetToughness() + defense) <= 0) {
            return false;
        }

        /* -- currently disabled until better conditions are devised and the spell prediction is made smarter --
        // Determine if some mana sources need to be held for the future spell to cast in Main 2 before determining whether to pump.
        AiController aic = ((PlayerControllerAi)ai.getController()).getAi();
        if (aic.getCardMemory().isMemorySetEmpty(AiCardMemory.MemorySet.HELD_MANA_SOURCES)) {
            // only hold mana sources once
            SpellAbility futureSpell = aic.predictSpellToCastInMain2(ApiType.Pump);
            if (futureSpell != null && futureSpell.getHostCard() != null) {
                aic.reserveManaSourcesForMain2(futureSpell);
            }
        }
        */

        // will the creature attack (only relevant for sorcery speed)?
        if (phase.getPhase().isBefore(PhaseType.COMBAT_DECLARE_ATTACKERS)
                && phase.isPlayerTurn(ai)
                && SpellAbilityAi.isSorcerySpeed(sa)
                && attack > 0
                && ComputerUtilCard.doesCreatureAttackAI(ai, c)) {
            return true;
        }

        // buff attacker/blocker using triggered pump
        if (sa.isTrigger() && phase.getPhase().isBefore(PhaseType.COMBAT_DECLARE_ATTACKERS)) {
            if (phase.isPlayerTurn(ai)) {
                if (CombatUtil.canAttack(c)) {
                    return true;
                }
            } else {
                if (CombatUtil.canBlock(c)) {
                    return true;
                }
            }
        }

        final Player opp = ai.getOpponent();
        Card pumped = pumpedCreature(ai, sa, c, defense, attack, keywords);
        List<Card> oppCreatures = opp.getCreaturesInPlay();
        float chance = 0;
        
        //create and buff attackers
        if (phase.getPhase().isBefore(PhaseType.COMBAT_DECLARE_ATTACKERS) && phase.isPlayerTurn(ai)) {
            //1. become attacker for whatever reason
            if (!ComputerUtilCard.doesCreatureAttackAI(ai, c) && ComputerUtilCard.doesSpecifiedCreatureAttackAI(ai, pumped)) {
                float threat = 1.0f * ComputerUtilCombat.damageIfUnblocked(pumped, opp, combat, true) / opp.getLife();
                if (CardLists.filter(oppCreatures, CardPredicates.possibleBlockers(pumped)).isEmpty()) {
                    threat *= 2;
                }
                if (c.getNetPower() == 0 && c == sa.getHostCard() && attack > 0 ) {
                    threat *= 4;    //over-value self +attack for 0 power creatures which may be pumped further after attacking 
                }
                chance += threat;
            }
            
            //2. grant haste
            if (keywords.contains("Haste") && c.hasSickness() && !c.isTapped()) {
                chance += 0.5f;
                if (ComputerUtilCard.doesSpecifiedCreatureAttackAI(ai, pumped)) {
                    chance += 0.5f * ComputerUtilCombat.damageIfUnblocked(pumped, opp, combat, true) / opp.getLife();
                }
            }
            
            //3. grant evasive
            if (!CardLists.filter(oppCreatures, CardPredicates.possibleBlockers(c)).isEmpty()) {
                if (CardLists.filter(oppCreatures, CardPredicates.possibleBlockers(pumped)).isEmpty() 
                        && ComputerUtilCard.doesSpecifiedCreatureAttackAI(ai, pumped)) {
                    chance += 0.5f * ComputerUtilCombat.damageIfUnblocked(pumped, opp, combat, true) / opp.getLife();
                }
            }
        }
        
        //combat trickery
        if (phase.is(PhaseType.COMBAT_DECLARE_BLOCKERS)) {
            //clunky code because ComputerUtilCombat.combatantWouldBeDestroyed() does not work for this sort of artificial combat
            Combat pumpedCombat = new Combat(phase.isPlayerTurn(ai) ? ai : opp);
            List<Card> opposing = null;
            boolean pumpedWillDie = false;
            final boolean isAttacking = combat.isAttacking(c);
            if (isAttacking) {
                pumpedCombat.addAttacker(pumped, opp);
                opposing = combat.getBlockers(c);
                for (Card b : opposing) {
                    pumpedCombat.addBlocker(pumped, b);
                }
                if (ComputerUtilCombat.attackerWouldBeDestroyed(ai, pumped, pumpedCombat)) {
                    pumpedWillDie = true;
                }
            } else {
                opposing = combat.getAttackersBlockedBy(c);
                for (Card a : opposing) {
                    pumpedCombat.addAttacker(a, ai);
                    pumpedCombat.addBlocker(a, pumped);
                }
                if (ComputerUtilCombat.blockerWouldBeDestroyed(ai, pumped, pumpedCombat)) {
                    pumpedWillDie = true;
                }
            }
            
            //1. save combatant
            if (ComputerUtilCombat.combatantWouldBeDestroyed(ai, c, combat) && !pumpedWillDie) {
                return true;
            }
            
            //2. kill combatant
            boolean survivor = false;
            for (Card o : opposing) {
                if (!ComputerUtilCombat.combatantWouldBeDestroyed(opp, o, combat)) {
                    survivor = true;
                    break;
                }
            }
            if (survivor) {
                for (Card o : opposing) {
                    if (!ComputerUtilCombat.combatantWouldBeDestroyed(opp, o, combat)) {
                        if (isAttacking) {
                            if (ComputerUtilCombat.blockerWouldBeDestroyed(opp, o, pumpedCombat)) {
                                return true;
                            }
                        } else {
                            if (ComputerUtilCombat.attackerWouldBeDestroyed(opp, o, pumpedCombat)) {
                                return true;
                            }
                        }
                    }
                }
            }
            
            //3. buff attacker
            if (combat.isAttacking(c)) {
                int dmg = ComputerUtilCombat.damageIfUnblocked(c, opp, combat, true);
                int pumpedDmg = ComputerUtilCombat.damageIfUnblocked(pumped, opp, pumpedCombat, true);
                if (combat.isBlocked(c)) {
                    if (!c.hasKeyword("Trample")) {
                        dmg = 0;
                    }
                    if (c.hasKeyword("Trample") || keywords.contains("Trample")) {
                       for (Card b : combat.getBlockers(c)) {
                           pumpedDmg -= ComputerUtilCombat.getDamageToKill(b);
                       }
                    } else {
                        pumpedDmg = 0;
                    }
                }
                if (pumpedDmg >= opp.getLife()) {
                    return true;
                }
                float value = 1.0f * (pumpedDmg - dmg);
                if (c == sa.getHostCard() && attack > 0) {
                    int divisor = sa.getPayCosts().getTotalMana().getCMC();
                    if (divisor <= 0) {
                        divisor = 1;
                    }
                    value *= attack / divisor;
                } else {
                    value /= opp.getLife();
                }
                chance += value;
            }
            
            //4. lifelink
            if (ai.canGainLife() && !c.hasKeyword("Lifelink") && keywords.contains("Lifelink")
                    && (combat.isAttacking(c) || combat.isBlocking(c))) {
                int dmg = pumped.getNetCombatDamage();
                //The actual dmg inflicted should be the sum of ComputerUtilCombat.predictDamageTo() for opposing creature
                //and trample damage (if any)
                chance += 1.0f * dmg / ai.getLife();
            }
            
            //5. if the life of the computer is in danger, try to pump blockers blocking Tramplers
            if (combat.isBlocking(c) && defense > 0 ) {
                List<Card> blockedBy = combat.getAttackersBlockedBy(c);
                boolean attackerHasTrample = false;
                for (Card b : blockedBy) {
                    attackerHasTrample |= b.hasKeyword("Trample");
                }
                if (attackerHasTrample && (sa.isAbility() || ComputerUtilCombat.lifeInDanger(ai, combat))) {
                    return true;
                }
            }
        }
        //TODO:how to consider repeatable pump abilities? This probably requires the AI to keep track of future decisions and/or
        //plan a sequence of decisions.
        return MyRandom.getRandom().nextFloat() < chance;
    }

    /**
     * <p>
     * getPumpCreatures.
     * </p>
     * 
     * @return a {@link forge.CardList} object.
     */
    protected CardCollection getPumpCreatures(final Player ai, final SpellAbility sa, final int defense, final int attack, final List<String> keywords) {
        CardCollection list = ai.getCreaturesInPlay();
        list = CardLists.filter(list, new Predicate<Card>() {
            @Override
            public boolean apply(final Card c) {
                return shouldPumpCard(ai, sa, c, defense, attack, keywords);
            }
        });
        return list;
    } // getPumpCreatures()

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
        CardCollection list = ai.getOpponent().getCreaturesInPlay();
        final Game game = ai.getGame();
        final Combat combat = game.getCombat();
        list = CardLists.getTargetableCards(list, sa);
        
        if (list.isEmpty()) {
            return list;
        }

        if (defense < 0) { // with spells that give -X/-X, compi will try to destroy a creature
            list = CardLists.filter(list, new Predicate<Card>() {
                @Override
                public boolean apply(final Card c) {
                    if (c.getNetToughness() <= -defense) {
                        return true; // can kill indestructible creatures
                    }
                    return ((ComputerUtilCombat.getDamageToKill(c) <= -defense) && !c.hasKeyword("Indestructible"));
                }
            }); // leaves all creatures that will be destroyed
        } // -X/-X end
        else if (attack < 0 && !game.getPhaseHandler().isPreventCombatDamageThisTurn()) {
            // spells that give -X/0
            boolean isMyTurn = game.getPhaseHandler().isPlayerTurn(ai);
            if (isMyTurn) {
                if (game.getPhaseHandler().getPhase().isBefore(PhaseType.COMBAT_BEGIN)) {
                    // TODO: Curse creatures that will block AI's creatures, if AI is going to attack.
                    list = new CardCollection();
                } else {
                    list = new CardCollection();
                }
            } else {
                // Human active, only curse attacking creatures
                if (game.getPhaseHandler().getPhase().isBefore(PhaseType.COMBAT_DECLARE_BLOCKERS)) {
                    list = CardLists.filter(list, new Predicate<Card>() {
                        @Override
                        public boolean apply(final Card c) {
                            if (combat == null || !combat.isAttacking(c)) {
                                return false;
                            }
                            if (c.getNetPower() > 0 && ai.getLife() < 5) {
                                return true;
                            }
                            //Don't waste a -7/-0 spell on a 1/1 creature
                            if (c.getNetPower() + attack > -2 || c.getNetPower() > 3) {
                                return true;
                            }
                            return false;
                        }
                    });
                } else {
                    list = new CardCollection();
                }
            }
        } // -X/0 end
        else {
            final boolean addsKeywords = keywords.size() > 0;
            if (addsKeywords) {
                list = CardLists.filter(list, new Predicate<Card>() {
                    @Override
                    public boolean apply(final Card c) {
                        return containsUsefulKeyword(ai, keywords, c, sa, attack);
                    }
                });
            } else if (sa.hasParam("NumAtt") || sa.hasParam("NumDef")) { 
                // X is zero
                list = new CardCollection();
            }
        }

        return list;
    } // getCurseCreatures()

    protected boolean containsNonCombatKeyword(final List<String> keywords) {
        for (final String keyword : keywords) {
            // since most keywords are combat relevant check for those that are
            // not
            if (keyword.endsWith("This card doesn't untap during your next untap step.")
                    || keyword.endsWith("Shroud") || keyword.endsWith("Hexproof")) {
                return true;
            }
        }
        return false;
    }

    public Card pumpedCreature(final Player ai, final SpellAbility sa, final Card c, final int d, final int a,
            final List<String> keywords) {
        Card pumped = CardFactory.copyCard(c, true);
        pumped.setSickness(c.hasSickness());
        final long timestamp = c.getGame().getNextTimestamp();
        final ArrayList<String> kws = new ArrayList<String>();
        for (String kw : keywords) {
            if (kw.startsWith("HIDDEN")) {
                pumped.addHiddenExtrinsicKeyword(kw);
            } else {
                kws.add(kw);
            }
        }
        pumped.addTempPowerBoost(c.getTempPowerBoost() + a);
        pumped.addTempToughnessBoost(c.getTempToughnessBoost() + d);
        pumped.addChangedCardKeywords(kws, new ArrayList<String>(), false, timestamp);
        Set<CounterType> types = c.getCounters().keySet();
        for(CounterType ct : types) {
            pumped.addCounterFireNoEvents(ct, c.getCounters(ct), true);
        }
        //Copies tap-state and extra keywords (auras, equipment, etc.) 
        if (c.isTapped()) {
            pumped.setTapped(true);
        }
        final List<String> copiedKeywords = pumped.getKeywords();
        List<String> toCopy = new ArrayList<String>();
        for (String kw : c.getKeywords()) {
            if (!copiedKeywords.contains(kw)) {
                if (kw.startsWith("HIDDEN")) {
                    pumped.addHiddenExtrinsicKeyword(kw);
                } else {
                    toCopy.add(kw);
                }
            }
        }
        final long timestamp2 = c.getGame().getNextTimestamp(); //is this necessary or can the timestamp be re-used?
        pumped.addChangedCardKeywords(toCopy, new ArrayList<String>(), false, timestamp2);
        ComputerUtilCard.applyStaticContPT(ai.getGame(), pumped, new CardCollection(c));
        return pumped;
    }
}
